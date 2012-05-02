package scale.score.pred;

import java.util.Iterator;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.clef.decl.*;
import scale.clef.expr.Literal;
import scale.clef.expr.AddressLiteral;
import scale.score.chords.Chord;
import scale.score.chords.ExprChord;

/**
 * This class scans a Scribble CFG looking for {@link
 * scale.clef.decl.Declaration declaration} references.
 * <p>
 * $Id: References.java,v 1.75 2007-10-04 19:58:34 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * For this class, a definition of a variable is a place where it
 * is (potentially) given a new value.  A use site is where a
 * variable's value is used.  A reference site is any place that a
 * variable is used or defined.
 */

public class References
{
  private static int computedCount  = 0; // A count of times the use and def information was computed.
  private static int createdCount   = 0; // A count of all the current instances of this class.

  static
  {
    Statistics.register("scale.score.pred.References", "created");
    Statistics.register("scale.score.pred.References", "computed");
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * Return the number of times the dominance frontier was computed.
   */
  public static int computed()
  {
    return computedCount;
  }

  private Table<Declaration, Chord> defDecls; // Map from variables to the Chords where they are defined.
  private Table<Declaration, Chord> useDecls; // Map from variables to the Chords where they are used.

  private HashSet<Declaration> globalVars; // A set of all the global
                                           // variables that are not
                                           // const referenced in the
                                           // Scribble graph.
  private HashSet<Declaration> staticVars; // A set of all the local
                                           // static variables that
                                           // are not const referenced
                                           // in the Scribble graph.

  private boolean valid;

  /**
   * Construct a predicate to visit all the expressions in a CFG to
   * determine all the places declarations are referenced.
   */
  public References()
  {
    createdCount++;
    valid = false;

    defDecls = new Table<Declaration, Chord>();
    useDecls = new Table<Declaration, Chord>();

    globalVars = new HashSet<Declaration>(23);
    staticVars = new HashSet<Declaration>(13);
  }

  /**
   * Determine the actual declaration references.
   */
  public void compute(Scribble scribble)
  {
    if (valid)
      return;

    computedCount++;

    Stack<Chord>   wl   = WorkArea.<Chord>getStack("compute"); // Use a stack - not a queue!
    HashSet<Chord> done = WorkArea.<Chord>getSet("compute");

    Chord begin = scribble.getBegin();
    wl.push(begin);
    done.add(begin);

    Chord first = begin; // First Chord in the basic block.
    while (!wl.empty()) {
      Chord start = wl.pop();

      start.recordRefs(this);
      start.pushOutCfgEdges(wl, done);
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(done);

    valid = true;
  }

  /**
   * Return true if the reference information is valid.
   */
  public final boolean isValid()
  {
    return valid;
  }
  
  /**
   * Specify that the reference information is invalid.
   * This also removes the reference information.
   */
  public void setInvalid()
  {
    valid = false;
    defDecls.clear();
    useDecls.clear();
    globalVars.clear();
    staticVars.clear();
  }

  /**
   * Return an iteration of the global variables, that are not const,
   * which are referenced in this procedure.
   */
  public final Iterator<Declaration> getGlobalVars()
  { 
    return globalVars.iterator(); 
  }

  /**
   * Return an iteration of the local static variables, that are not
   * const, which are referenced in this procedure.
   */
  public final Iterator<Declaration> getStaticVars()
  { 
    return staticVars.iterator(); 
  }

  /**
   * Return a set of statements with definition sites for a given
   * variable.
   */
  public final HashSet<Chord> getDefChordSet(Declaration var)
  { 
    return defDecls.getRowSet(var);
  }
  
  /**
   * Return a set of statements with use sites for a given variable.
   */
  public final HashSet<Chord> getUseChordSet(Declaration var)
  { 
    return useDecls.getRowSet(var);
  }
  
  /**
   * Return an iteration of the statements with definition sites for a
   * given variable.
   */
  public final Iterator<Chord> getDefChords(Declaration var)
  { 
    return defDecls.getRowEnumeration(var);
  }

  /**
   * Return the numberof definition sites.
   */
  public final int numDefChords(Declaration var)
  {
    return defDecls.rowSize(var);
  }

  /**
   * Return an iteration of basic blocks with use sites for a given
   * variable.
   */
  public final Iterator<Chord> getUseChords(Declaration var) 
  {
    return useDecls.getRowEnumeration(var);
  }

  /**
   * Return true if there are any uses of a variable.
   */
  public final boolean anyUseChords(Declaration var) 
  {
    return !useDecls.isRowEmpty(var);
  }

  /**
   * Return true if there are any defs of a variable.
   */
  public final boolean anyDefChords(Declaration var) 
  {
    return !defDecls.isRowEmpty(var);
  }

  /**
   * Record the use of a variable.
   * @param expr is the expression where the variable is used.
   * @param decl is the declaration of the variable.
   */
  public void recordUse(Chord stmt, Expr expr, Declaration decl)
  {
    VariableDecl vd = decl.returnVariableDecl();
    if (vd == null)
      return;

    useDecls.add(vd, stmt);
    otherLists(vd);

    EquivalenceDecl evd = vd.returnEquivalenceDecl();
    if (evd != null) {
      VariableDecl decl2 = evd.getBaseVariable();
      useDecls.add(decl2, stmt);
      otherLists(decl2);
        
    }
  }

  private void otherLists(VariableDecl a)
  {
    if (!a.isConst()) {
      if (a.isGlobal())
        globalVars.add(a);
      else if (a.isStatic())
        staticVars.add(a);
    }
  }

  /**
   * Remove a VariableDecl from the 
   * <ul>
   * <li> list of global variables
   * <li> list of local static variables
   * </ul>
   * Note - the use & def information is not modified.
   * @param v is the variable
   */
  public final void removeVars(VariableDecl v)
  {
    globalVars.remove(v);
    staticVars.remove(v);
  }

  /**
   * Remove all mappings from the specified variable to the specified
   * CFG node.
   */
  public final void remove(Chord stmt, Declaration v)
  {
    defDecls.remove(v, stmt);
    useDecls.remove(v, stmt);
  }

  /**
   * Record the definition (kill) of a variable.
   * @param expr the expression where the variable is defined (killed)
   * @param decl the declaration of the variable.
   */
  public void recordDef(Chord stmt, Expr expr, Declaration decl)
  {
    VariableDecl vd = decl.returnVariableDecl();
    if (vd == null)
      return;

    defDecls.add(decl, stmt);
    otherLists(vd);
  }
}
