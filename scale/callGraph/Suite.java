package scale.callGraph;

import java.util.Enumeration;
import java.util.Iterator;

import java.io.*;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;
import scale.clef.symtab.*;
import scale.score.Scribble;
import scale.score.pp.PPCfg;

/**
 * This class is meant to facilitate separate compilations by being a
 * repository for multiple CallGraph instances.
 * <p>
 * $Id: Suite.java,v 1.66 2007-10-04 19:53:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * In Scale, each source file is converted to a Clef abstract syntax
 * tree (AST).  The ASTs are processed into {@link
 * scale.callGraph.CallGraph call graphs}.  This class is a holder for
 * multiple {@link scale.callGraph.CallGraph call graphs} and has
 * methods that perform similar to the methods on a single {@link
 * scale.callGraph.CallGraph call graph}.  In addition, this class
 * maintains a mapping from a name to a set of top level {@link
 * scale.clef.decl.Declaration declarations} and from these
 * declarations to the {@link scale.callGraph.CallGraph call graphs}
 * in which they are defined.
 * @see CallGraph
 */
public class Suite extends Root 
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * Map (file) names to call graphs.
   */
  private HashMap<String, CallGraph> callGraphs;  
  /**
   * The main procedure if one is found..
   */
  private CallGraph rootRoutine; 
  /**
   * Map from the declaration name to the CallGraph containing the
   * declaration.
   */
  private HashMap<String, HashMap<Declaration, CallGraph>> decls;       
  /**
   * True if static functions and variable should be made global for
   * inlining.
   */
  private boolean noStaticDecls = false;

  /**
   * Create a holder for related {@link scale.callGraph.CallGraph call
   * graphs} that acts like a single {@link scale.callGraph.CallGraph
   * call graph}.
   * @param noStaticDecls is true if static functions and variable
   * should be made global for inlining
   * @see CallGraph
   */
  public Suite(boolean noStaticDecls)
  {
    this.noStaticDecls = noStaticDecls;
    this.callGraphs    = new HashMap<String, CallGraph>(11);
    this.decls         = new HashMap<String, HashMap<Declaration, CallGraph>>(11);
  }

  /**
   * Return an enumeration of all the {@link scale.callGraph.CallGraph
   * call graphs}.
   */
  public Enumeration<CallGraph> getCallGraphs()
  {
    return callGraphs.elements();
  }

  /**
   * Return an enumeration of all the {@link scale.callGraph.CallGraph
   * call graph} names.
   */
  public Enumeration<String> getNames()
  {
    return callGraphs.keys();
  }

  /**
   * Add the profiling instrumentation to every {@link
   * scale.callGraph.CallGraph call graph} in the suite.
   * @param moduleNames is the list of all source modules in the program 
   * and must include the module containing "main"
   * @param profileOptions specifies which profiling instrumentation to insert
   */
  public void addProfiling(Vector<String> moduleNames,int profileOptions)
  {
    // If we're supposed to do profile-guided profiling, then it's
    // important that we process the CFGs in a particular order (since
    // we will only instrument as many as necessary until we get the
    // benefit we want.  Note that calling addProfiling() on a
    // Scribble CFG is not enough.  It is also necessary to call it on
    // a call graph to set up some other stuff.

    if (PPCfg.getPgp())
      PPCfg.addProfilingInSpecialOrder(profileOptions);

    Enumeration<CallGraph> ecg = callGraphs.elements();
    while (ecg.hasMoreElements()) {
      CallGraph cg = ecg.nextElement();
      cg.addProfiling(moduleNames, profileOptions);
    }
  }

  /**
   * Read in profiling informationforo every {@link
   * scale.callGraph.CallGraph call graph} in the suite.
   * @param profilePaths is the list of directories to search for
   * profile information and must include the module containing "main"
   * @param profileOptions specifies which profiling instrumentation to insert
   */
  public void readProfInfo(Vector<String> profilePaths, int profileOptions)
  {
    // Note that calling readProfInfo() on a Scribble CFG is not
    // enough.  It is also necessary to call it on a call graph to set
    // up some other stuff.

    if (PPCfg.getPgp())
      PPCfg.addProfilingInSpecialOrder(profileOptions);

    Enumeration<CallGraph> ecg = callGraphs.elements();
    while (ecg.hasMoreElements()) {
      CallGraph cg = ecg.nextElement();
      cg.readProfInfo(profilePaths, profileOptions);
    }
  }

  /**
   * Keep track of the {@link  scale.clef.decl.Declaration
   * declarations} by name.
   * @param decl is the declaration
   * @param cg is the CallGraph in which the declaration is found
   */
  private void addDecl(Declaration decl, CallGraph cg)
  {
    String                          name   = decl.getName();
    HashMap<Declaration, CallGraph> others = decls.get(name);
    if (others == null) {
      others = new HashMap<Declaration, CallGraph>(11);
      decls.put(name, others);
    }
    others.put(decl, cg);
  }

  /**
   * Create a new, unique global name for a static variable or function.
   */
  private String createGlobalName(String name, String fileName)
  {
    StringBuffer buf = new StringBuffer(name);
    buf.append('_');
    buf.append(Integer.toHexString(fileName.hashCode()));
    return buf.toString();
  }

  /**
   * Add a {@link scale.callGraph.CallGraph call graph} to the Suite.
   * @param cg is the call graph
   * @see CallGraph
   */
  public void addCallGraph(CallGraph cg)
  {
    RoutineDecl rRoutine = cg.getMain();   /* The main procedure if one is found. */
    if (rRoutine != null) {
      if (rootRoutine == null)
        rootRoutine = cg;
      else
        Msg.reportWarning(Msg.MSG_Main_already_defined_in_s,
                          cg.getName(),
                          1,
                          1,
                          rootRoutine.getName());
    }

    Object o = callGraphs.put(cg.getName(), cg);
    assert (o == null) : "CallGraph " + cg + " already added.";

    // Build map of names to sets of (Declaration, CallGraph) tuples.
    // First, we do it for top level decls (except routines).

    Iterator<Declaration> ecg = cg.topLevelDecls();
    while (ecg.hasNext()) {
      Declaration d = ecg.next();
      if (noStaticDecls &&
          d.isVariableDecl() &&
          (d.visibility() == Visibility.FILE)) {
        d.setName(createGlobalName(d.getName(), cg.getName()));
        d.setVisibility(Visibility.GLOBAL);
      }
      addDecl(d, cg);
    }

    // Then, we do it for routines (note - we do it for Clef routine
    // decl nodes and not call nodes).

    Iterator<RoutineDecl> it = cg.allRoutines();
    while (it.hasNext()) {
      RoutineDecl d = it.next();
      if (noStaticDecls && (d.visibility() == Visibility.FILE)) {
        d.setName(createGlobalName(d.getName(), cg.getName()));
        d.setVisibility(Visibility.GLOBAL);
      }
      addDecl(d, cg);
    }
  }

  /**
   * Return all the other top level {@link
   * scale.clef.decl.Declaration declarations} with the same name
   * (including {@link  scale.clef.decl.RoutineDecl routine}
   * declarations)
   * @param name is the name of a Declaration
   */
  public Enumeration<Declaration> otherDecls(String name)
  {
    HashMap<Declaration, CallGraph> others = decls.get(name);
    if (others == null)
      return new EmptyEnumeration<Declaration>();
    return others.keys();
  }

  /**
   * Return all the other top level {@link
   * scale.clef.decl.Declaration declarations} with the same name
   * (including routine declarations).
   * @param decl is a Declaration
   */
  public Enumeration<Declaration> otherDecls(Declaration decl)
  {
    return otherDecls(decl.getName());
  }

  /**
   * Return the CallGraph in which the {@link
   * scale.clef.decl.Declaration declaration} is defined.
   * @param decl is a Declaration (may be routine declaration)
   */
  public CallGraph getCallGraph(Declaration decl)
  {
    HashMap<Declaration, CallGraph> others = decls.get(decl.getName());
    if (others == null)
      return null;
    return others.get(decl);
  }

  /**
   * Print a listing of all symbols and the {@link
   * scale.callGraph.CallGraph call graphs} in which they are
   * referenced.
   */
  public void printXRef()
  {
    Enumeration<String> en = decls.keys();
    while (en.hasMoreElements()) {
      String                          name    = en.nextElement();
      HashMap<Declaration, CallGraph> declmap = decls.get(name);

      System.out.println("// " + name + " referenced in: ");

      Enumeration<CallGraph> ecg = declmap.elements();
      while (ecg.hasMoreElements()) {
        CallGraph cg = ecg.nextElement();
        System.out.print(cg.getName());
        if (ecg.hasMoreElements())
          System.out.print(", ");
      }
      System.out.println();
    }
  }

  /**
   * Print to out the {@link  scale.clef.decl.RoutineDecl routines}
   * in this {@link scale.callGraph.CallGraph call graph}.
   */
  public void printAllRoutines()
  {
    Iterator<RoutineDecl> er = allRoutines();
    while (er.hasNext()) {
      RoutineDecl n    = er.next();
      String      name = n.getName();

      System.out.print("// " + name + " calls: ");
      n.printCallees();

      int l = n.numCallers();
      if (l > 0) {
        System.out.print("// " + name + " called by: ");

        for (int i = 0; i < l; i++) {
          if (i > 0)
            System.out.print(", ");
          RoutineDecl caller = n.getCaller(i);
          System.out.print(caller.getName());
        }
        System.out.println();
      }
    }
  }
   
  /**
   * Return the main {@link scale.clef.decl.RoutineDecl routine} if
   * any.
   */
  public RoutineDecl getMain()
  {
    if (rootRoutine == null)
      return null;
    return rootRoutine.getMain();
  }

  // Declare constants which determine the declaration properties
  private static final int ALL     = 0;  // all declarations
  private static final int DEFS    = 1;  // defining declarations
  private static final int EXTERNS = 2;  // referencing decls (ie., extern)

  /**
   * A class which returns an iteration of declarations.  We can
   * also choose the property of the nodes we would like to return,
   * ie., all node, external declarations, or defining declarations.
   */
  private class CGDIterator implements Iterator<Declaration>
  {
    private int                    property;     // property
    private Enumeration<CallGraph> ecg;          // current call graph
    private Iterator<Declaration>  it   = null;  // current node in call graph
    private Declaration            next = null;  // the next node to return

    public CGDIterator(int property)
    {
      this.property = property;
      ecg = callGraphs.elements();
      if (ecg.hasMoreElements()) {
        CallGraph cg = ecg.nextElement();
        it = cg.topLevelDecls();
        if (it != null)
          next = getNext();
      }
    }

    /**
     * Get the next node in the list.  It depends upon the type of
     * node we want (ie., routine or decl) and the property (ie., any
     * declaration, a defining declaration, or a external
     * declaration).
     */
    private Declaration getNext()
    {
      switch (property) {
      case ALL:
        if (it.hasNext())
          return it.next();
        break;
      case DEFS:
        while (it.hasNext()) {
          Declaration decl = it.next();
          if (decl.visibility() != Visibility.EXTERN)
            return decl;
        }
        break;
      case EXTERNS:
        while (it.hasNext()) {
          Declaration decl = it.next();
          if (decl.visibility() == Visibility.EXTERN)
            return decl;
        }
        break;
      }
      return null;
    }

    public boolean hasNext()
    {
      while (it != null) {
        if (next != null)
          return true;
        else if (ecg.hasMoreElements()) {
          CallGraph cg = ecg.nextElement();
          it = cg.topLevelDecls();
          if (it != null)
            next = getNext();
        } else {
          it = null;
          break;
        }
      }
      return false;
    }

    public Declaration next()
    {
      Declaration o = next;
      next = getNext();
      return o;
    }

    public void remove()
    {
      throw new scale.common.InternalError("Remove() not available.");
    }
  }
  /**
   * A class which returns an iteration of routines.  We can also
   * choose the property of the nodes we would like to return, ie.,
   * all node, external declarations, or defining declarations.
   */
  private class CGRIterator implements Iterator<RoutineDecl>
  {
    private int                    property;     // property
    private Enumeration<CallGraph> ecg;          // current call graph
    private Iterator<RoutineDecl>  it   = null;  // current node in call graph
    private RoutineDecl            next = null;  // the next node to return

    public CGRIterator(int property)
    {
      this.property = property;
      ecg = callGraphs.elements();
      if (ecg.hasMoreElements()) {
        CallGraph cg = ecg.nextElement();
        it = cg.allRoutines();
        if (it != null)
          next = getNext();
      }
    }

    /**
     * Get the next node in the list.  It depends upon the type of
     * node we want (ie., routine or decl) and the property (ie., any
     * declaration, a defining declaration, or a external
     * declaration).
     */
    private RoutineDecl getNext()
    {
      switch (property) {
      case ALL:
        if (it.hasNext())
          return it.next();
        break;
      case DEFS:
        while (it.hasNext()) {
          RoutineDecl cn = it.next();
          if (cn.getScribbleCFG() != null)
            return cn;
        }
        break;
      case EXTERNS:
        while (it.hasNext()) {
          RoutineDecl rd = it.next();
          if (rd.visibility() == Visibility.EXTERN)
            return rd;
        }
        break;
      }
      return null;
    }

    public boolean hasNext()
    {
      while (it != null) {
        if (next != null)
          return true;
        else if (ecg.hasMoreElements()) {
          CallGraph cg = ecg.nextElement();
          it = cg.allRoutines();
          if (it != null)
            next = getNext();
        } else {
          it = null;
          break;
        }
      }
      return false;
    }

    public RoutineDecl next()
    {
      RoutineDecl o = next;
      next = getNext();
      return o;
    }

    public void remove()
    {
      throw new scale.common.InternalError("Remove() not available.");
    }
  }
      
  /**
   * Return an enumeration of all the {@link
   * scale.clef.decl.RoutineDecl routines}.
   */
  public Iterator<RoutineDecl> allRoutines()
  {
    return new CGRIterator(ALL);
  }

  /**
   * Return an enumeration of all the top level {@link
   * scale.clef.decl.Declaration declarations} except routines.
   */
  public Iterator<Declaration> topLevelDecls()
  {
    return new CGDIterator(ALL);
  }

  /**
   * Return an enumeration of all the {@link
   * scale.clef.decl.RoutineDecl routines} with bodies.  That is, the
   * routines that aren't defined external.
   */
  public Iterator<RoutineDecl> allDefRoutines()
  {
    return new CGRIterator(DEFS);
  }

  /**
   * Return an enumeration of all the top level defining {@link
   * scale.clef.decl.Declaration declarations}.  We don't return
   * routines or declarations declared with the <code>extern</code>
   * keyword (a referencing declaration).
   */
  public Iterator<Declaration> topLevelDefDecls()
  {
    return new CGDIterator(DEFS);
  }

  /**
   * Return an enumeration of all the external {@link
   * scale.clef.decl.RoutineDecl routines}.
   */
  public Iterator<RoutineDecl> allExternRoutines()
  {
    return new CGRIterator(EXTERNS);
  }

  /**
   * Return an enumeration of all the top level external {@link
   * scale.clef.decl.Declaration declarations}.
   * We don't return routines.
   */
  public Iterator<Declaration> topLevelExternDecls()
  {
    return new CGDIterator(EXTERNS);
  }

  /**
   * Return an enumeration of the external (referencing) {@link
   * scale.clef.decl.Declaration declarations} for
   * a given defining declaration.
   * @param d the defining declaration.
   */
  public Enumeration<Declaration> externDecls(Declaration d)
  {
    HashMap<Declaration, CallGraph> externs = decls.get(d.getName());
    return new ExternDeclEnumeration(externs);
  }

  /**
   * A class which returns an enumeration of external {@link
   * scale.clef.decl.Declaration declarations} for
   * a given declaration.  We maintain a map of names to declarations -
   * we just need to find the entries marked "extern".
   */
  private static class ExternDeclEnumeration implements Enumeration<Declaration>
  {
    Enumeration<Declaration> decls;
    Declaration              next = null;

    /**
     * Create an enumeration of external {@link 
     * scale.clef.decl.Declaration declarations}.
     */
    public ExternDeclEnumeration(HashMap<Declaration, CallGraph> map)
    {
      if (map != null) {
        decls = map.keys();
        next = getNextDecl();
      }
    }

    /**
     * Get the next external declaration in the list.  We ignore
     * declarations that aren't external
     */
    private Declaration getNextDecl()
    {
      while (decls.hasMoreElements()) {
        Declaration d = decls.nextElement();
        if (d.visibility() == Visibility.EXTERN)
          return d;
      }
      return null;
    }

    public boolean hasMoreElements()
    {
      return (next != null);
    }

    public Declaration nextElement()
    {
      Declaration d = next;
      next = getNextDecl();
      return d;
    }
  }
}
