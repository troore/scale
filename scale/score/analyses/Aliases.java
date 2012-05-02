package scale.score.analyses;

import java.util.Iterator;
import java.util.Enumeration;

import scale.common.*;
import scale.alias.*;
import scale.alias.steensgaard.*;
import scale.annot.*;
import scale.callGraph.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.score.*;
import scale.score.chords.Chord;
import scale.score.expr.LoadDeclAddressExpr;
import scale.score.pred.TraceChords;

/**
 * This class computes aliases for a Suite of routines.
 * <p>
 * $Id: Aliases.java,v 1.78 2007-10-04 19:58:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If simple analysis is selected, we assume that all variables whose
 * addresses have been taken belong to the same alias variable.  That
 * is, a variable that points to another variable actually points to
 * any variable that has its address taken.  We add an extra pass
 * which finds the variables whose addresses have been taken and
 * creates a special alias variable for them.
 * <p>
 * If this is not simple analysis, we first create alias
 * variables for each variable in the program.  Then, we find the
 * aliases.  The alias variables are simply the interface to the alias
 * analysis code.  The type of alias analysis must be specified.
 * <p>
 * Note that we really don't need to create the alias variables during
 * a separate pass.  But, it is easier if we create them separately -
 * that way we are able to keep track of them more easily.
 *
 * @see scale.callGraph.Suite
 * @see scale.alias.AliasAnalysis 
 */
public class Aliases 
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * Prefix for virtual variable names.
   */
  public static final String vvName = "_vv";

  private static final CreatorSource creator = new CreatorSource("Aliases");  // Used for annotations.
  private static       int sizePointsToCount = 0;

  static
  {
    Statistics.register("scale.score.analyses.Aliases", "sizePointsTo");
  }

  /**
   * Return the size-points-to statistic.
   */
  public static int sizePointsTo()
  {
    return sizePointsToCount;
  }

  private Suite      suite;              // The suite containing the procedures in the program.
  private UniqueName un;                 // A unique name for unnamed formals.  Reset for each routine.
  private boolean    simple;             // If this is simple analysis.
  private int        varCount   = 0;     // A count of the number of virtual variables created

  private HashSet<VariableDecl> variables  = null;  // The list of variables whose addresses are taken.
  private HashMap<AliasVar, HashSet<EquivalenceDecl>> commonVars = null; // A map from the common variable AliasVar to the set of its equivalenced variables.
  private HashMap<ECR, VirtualVar> varMap;          // A mapping between ECRs and virtual variables

  /**
   * The object representing the actual alias analyzer.
   */
  protected AliasAnalysis analyzer;
  /**
   * A list of the alias variables created.
   */
  protected Vector<AliasVar> aliasVars;

  /**
   * Create an object for computing aliases in a Suite.
   * @param analyzer the alias analysis code
   * @param suite a collection of modules to process (hopefully, a whole program)
   * @param simple true if simple analysis is requested
   */
  public Aliases(AliasAnalysis analyzer, Suite suite, boolean simple) 
  {
    this.analyzer  = analyzer;
    this.suite     = suite;
    this.simple    = simple;
    this.varMap    = new HashMap<ECR, VirtualVar>(29);
    this.aliasVars = new Vector<AliasVar>();

    if (simple) {
      variables = new HashSet<VariableDecl>(31);
    }
  }

  /**
   * Generate the next virtual variable name.
   */
  protected String nextVirtualName()
  {
    return vvName + varCount++;
  }

  /**
   * Return the virtual variable associated with the ECR or create a new
   * virtual var if there isn't one already.
   * @param ecr an ECR that represents a virtual variable.
   * @return the virtual variable associated with the ECR.
   */
  public VirtualVar getVirtualVar(ECR ecr)
  {
    VirtualVar var = varMap.get(ecr);
    if (var == null) {
      var = new VirtualVar(vvName + ecr.getsetID(), ecr);
      varMap.put(ecr, var);
    }

    return var;
  }

  /**
   * Return the virtual variable associated with the ECR or create a new
   * virtual var if there isn't one already.
   * @param ecr an ECR that represents a virtual variable.
   * @return the virtual variable associated with the ECR.
   */
  public VirtualVar getVirtualVariable(ECR ecr)
  {
    return varMap.get(ecr);
  }

  /**
   * Associate a virtual variable with an ECR.
   */
  public void addVirtualVariable(ECR ecr, VirtualVar vv)
  {
    varMap.put(ecr, vv);
  }

  /**
   * The main routine for computing aliases.  For simple analysis, we
   * first find all variables that have their address taken and create
   * a special alias variable for them.  Then, we create alias
   * variables for the rest of the user variables.  Finally, we
   * analyze the program to find the alias relationships among the
   * variables.
   */
  public void computeAliases()
  {
    if (simple)
      findAddrTaken();

    createAliasVariables();
    findAliases();
  }

  /**
   * The first pass of the alias analysis.  We need to create
   * alias variables which the alias analyzer uses to represent the
   * relationship between the actual variables in program.
   */
  protected void createAliasVariables()
  {
    // For each top level declaration.

    Iterator<Declaration> et = suite.topLevelDefDecls();
    while (et.hasNext()) {
      Declaration d = et.next();
      if (d instanceof ValueDecl)
        createAliasVariable(d);
    }

    // For each function in the call graph - we first process defining
    // declarations.

    Iterator<RoutineDecl> er = suite.allDefRoutines();
    while (er.hasNext()) {
      RoutineDecl rd = er.next();
      createFunctionAliasVariable(rd, rd.getSignature(), rd.getScribbleCFG());
    }

    // In case we don't have a complete program, make sure that we've created
    // alias variables for all the external declarations.

    Iterator<Declaration> ed = suite.topLevelExternDecls();
    while (ed.hasNext()) {
      Declaration d = ed.next();
      if (d instanceof ValueDecl)
        createAliasVariable(d);
    }

    // Now do the same for all external routines.

    Iterator<RoutineDecl> ex = suite.allExternRoutines();
    while (ex.hasNext()) {
      RoutineDecl rd = ex.next();
      createFunctionAliasVariable(rd, rd.getSignature(), rd.getScribbleCFG());
    }
  }

  /**
   * Remove all the un-needed stuff.
   */
  public void cleanup()
  {
    int n = aliasVars.size();
    for (int i = 0; i < n; i++) {
      AliasVar avar = aliasVars.elementAt(i);
      avar.cleanup();
    }
  }

  /**
   * The second pass of the alias analysis.  We find the alias
   * relationships for an entire program.
   */
  protected void findAliases()
  {
    Stack<Chord>          wl = WorkArea.<Chord>getStack("findAliases");
    FindAliases           fa = new FindAliases(analyzer, suite, this);
    Iterator<RoutineDecl> it = suite.allDefRoutines();

    while (it.hasNext())
      fa.findAliases(it.next(), wl);

    WorkArea.<Chord>returnStack(wl);

    computeSizeOfPointsToSet();
  }

  /**
   * Obtain the alias variable for a declaration.  If none, create one
   * and add an annotation to the declaration.  We process function
   * variables differently than regular variables.
   *
   * @param d the declaration for the variable.
   * @return the alias variable created for the declaration
   */
  private AliasVar createAliasVariable(Declaration d)
  {
    Annotation annote = d.getAnnotation(AliasAnnote.annotationKey());

    if (annote != null) // If we've already created an alias variable, don't make another one.
      return ((AliasAnnote) annote).getAliasVar();

    Type dt = d.getCoreType();
    if (dt.isPointerType()) {
      ProcedureType pt = dt.getPointedTo().getCoreType().returnProcedureType();
      if (pt != null)
        return createFunctionAliasVariable(d, pt, null); // Create a alias variable for a function.
    }

    if (d.isEquivalenceDecl())
      return processCommonEquivalence((EquivalenceDecl) d);

    return newAliasVariable(d);
  }

  /**
   * Create an alias variable for a declaration and add an annotation
   * to the declaration.
   *
   * @param d the declaration for the variable.
   * @return the alias variable created for the declaration
   */
  public AliasVar newAliasVariable(Declaration d)
  {
    AliasVar avar = analyzer.addVariable(d);

    aliasVars.addElement(avar);
   
    Annotation annote = AliasAnnote.create(d, creator, Support.systemTrue, avar); // Create an annotation which is used later.

    // Add the annotation to all extern definitions of this variable.

    addAnnoteToAll(d, annote);

    return avar;
  }

  /**
   * Add the annotation to all extern definitions of this declaration.
   */
  private void addAnnoteToAll(Declaration d, Annotation annote)
  {
    if (!d.isGlobal())
      return;

    if (d instanceof ValueDecl) {
      Enumeration<Declaration> e   = suite.externDecls(d);
      while (e.hasMoreElements()) {
        Declaration extern = e.nextElement();
        if (!(extern instanceof ValueDecl))
          continue;

        if (!extern.hasAnnotation(AliasAnnote.annotationKey())) {
          extern.addAnnotation(annote);
        }
      }
      return;
    }

    if (d.isRoutineDecl()) {
      Enumeration<Declaration> e   = suite.externDecls(d);
      while (e.hasMoreElements()) {
        RoutineDecl extern = (RoutineDecl) e.nextElement();
        if (!extern.hasAnnotation(AliasAnnote.annotationKey())) {
          extern.addAnnotation(annote);
        }
      }
    }
  }

  /**
   * Find or create the AliasVar for a variable EQUIVALENCEd to either COMMON 
   * or another variable.
   */
  private AliasVar processCommonEquivalence(EquivalenceDecl d)
  {
    VariableDecl bvd  = d.getBaseVariable();
    AliasVar     avar = createAliasVariable(bvd); // Use the base variable's alias variable.

    if (!simple) {
      if (commonVars == null)
        commonVars = new HashMap<AliasVar, HashSet<EquivalenceDecl>>(11);

      // Record which variable goes with what COMMON.

      HashSet<EquivalenceDecl> set = commonVars.get(avar);
      if (set == null) {
        set = new HashSet<EquivalenceDecl>(23);
        commonVars.put(avar, set);
      }
      set.add(d);

      // Find the first variable in COMMON that overlaps this variable.

      Declaration link    = null;
      long        offset1 = d.getBaseOffset();
      long        size1   = d.getCoreType().memorySize(Machine.currentMachine);

      Iterator<EquivalenceDecl> e = set.iterator();
      while (e.hasNext()) {
        EquivalenceDecl ed      = e.next();
        long            offset2 = ed.getBaseOffset();
        long            size2   = ed.getCoreType().memorySize(Machine.currentMachine);

        if ((offset1 <= offset2) && ((offset1 + size1) > offset2)) {
          link = ed;
          break;
        } else if ((offset1 > offset2) && ((offset2 + size2) > offset1)) {
          link = ed;
          break;
        }
      }

      if (link == null)
        return newAliasVariable(d); // Create a new AliasVar for this variable.

      // If an overlapping variable is found, use the same AliasVar for both variables.

      Annotation annote = link.getAnnotation(AliasAnnote.annotationKey());

      if (annote != null) // If we've already created an alias variable, don't make another one.
        avar = ((AliasAnnote) annote).getAliasVar();
      else
        avar = newAliasVariable(link); // Create a new one for both.
    }

    // Attach the AliasVar to the variable with an annotation.

    AliasAnnote.create(d, creator, Support.systemTrue, avar);

    return avar;
  }

  /**
   * Create alias variables for functions.  We need to create
   * alias variables for the following declarations:
   * <ul>
   * <li> Local variables (for a defining declaration)
   * <li> Formal parameters
   * <li> Return value
   * <li> Parameter name
   * </ul>
   * We create a dummay alias variable for functions with no
   * arguments.  We do this because, in C, these functions may
   * actually be called with arguments.  The dummy alias variable
   * is used for all the actual arguments (which means that all
   * actuals may become potential aliases with each other).
   * 
   * @param rd the function declaration node (may be a function pointer)
   * @param sig the signature of the function
   * @param fn the scribble tree for the function (may be null if there isn't
   * a Scribble CFG
   * @return the alias variable created for the function
   */
  private AliasVar createFunctionAliasVariable(Declaration rd, ProcedureType sig, Scribble fn)
  {
    Annotation annote = rd.getAnnotation(AliasAnnote.annotationKey());

    if (annote != null) // If we've already created an alias variable, don't make another one.
      return ((AliasAnnote) annote).getAliasVar();

    String   name      = rd.getName();
    AliasVar retvalVar = null;

    // In a fully defined function - for each declaration.

    if (fn != null) {
      int l = fn.numDecls();
      for (int i = 0; i < l; i++) {
        Declaration d = fn.getDecl(i);
        if (d instanceof ValueDecl) {
          AliasVar avar = createAliasVariable(d);
          if (d.isFtnResultVar())
            retvalVar = avar; // the special return value
        }
      }
    }

    // For each formal parameter (get the Clef declaration)
    // create a list of parameters which is used below (in functionDef).

    Vector<AliasVar> params = new Vector<AliasVar>();
    un = new UniqueName(name + "arg");
    if (sig.numFormals() == 0) {
      // Add a dummy alias variable to routines with no arguments.  In C,
      // these routines may be called with arguments, so we need an alias
      // variable to represent the alias for all the arguments.

      Declaration tdecl = new FormalDecl(un.genName(), VoidType.type);
      AliasVar    avar  = analyzer.addVariable(tdecl);
      aliasVars.addElement(avar);

      params.addElement(avar);

    } else {
      int l = sig.numFormals();
      for (int i = 0; i < l; i++) {
        FormalDecl d    = sig.getFormal(i);
        AliasVar   avar = createAliasVariable(d);
        params.addElement(avar);
      }
    }

    // Create an alias variable for the function name.

    AliasVar afun = analyzer.addVariable(rd);
    aliasVars.addElement(afun);

    if (retvalVar == null) {
      Declaration rv = new VariableDecl("_rv_" + name, sig.getReturnType());
                                        
      retvalVar = analyzer.addVariable(rv);
      aliasVars.addElement(retvalVar);
    }

    // Call the analysis routine for a function definition.

    analyzer.functionDef(afun, params, retvalVar);

    // Add the annotation.

    annote = AliasAnnote.create(rd, creator, Support.systemTrue, afun);

    // Also, if this is a defining declaration then add the annotation
    // to all extern definitions of the function.

    if (fn != null)
      addAnnoteToAll(rd, annote);

    return afun;

  }

  /**
   * Add alias variable created on the fly in FindAliases to the vector.
   */
  public void addAliasVarToVector(AliasVar av)
  {
    aliasVars.addElement(av);
  }

  /**
   * For each routine in the Suite, find the variables whose addresses
   * are taken.  We create a special alias variable to represent them.
   * However, we still create an alias variable for each distintct variable,
   * but the internal data maintained by each of the alias variables is
   * the same.
   */
  private void findAddrTaken()
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("findAddrTaken");

    Iterator<RoutineDecl> it = suite.allDefRoutines();
    while (it.hasNext()) {
      RoutineDecl cn = it.next();
      Scribble    fn = cn.getScribbleCFG();

      if (fn == null)
        throw new scale.common.InternalError("No Scribble CFG for " + cn.getRoutineName());

      Chord start = fn.getBegin();

      variables.clear();

      // Define the visit class that checks for scribble nodes that represent
      // the address (&) operation.

      TraceChords fa = new TraceChords() {
        // Look for LoadDeclAddressExpr nodes that appear on rhs.
        public void visitLoadDeclAddressExpr(LoadDeclAddressExpr e)
        {
          if (!e.isDefined()) {
            Declaration decl = e.getDecl();
            if (decl.isVariableDecl())
              variables.add((VariableDecl) decl);
          }
        } 
      };

      wl.push(start);
      Chord.nextVisit();
      start.setVisited();

      while (!wl.empty()) {  // Find the variables whose addresses are taken.
        Chord s = wl.pop();
        s.pushOutCfgEdges(wl);
        s.visit(fa);
      }

      Declaration            at      = new VariableDecl("_addrtaken_", VoidType.type);
      AliasVar               addrVar = analyzer.addVariable(at);
      Iterator<VariableDecl> ev      = variables.iterator();
      while (ev.hasNext()) {
        ValueDecl d    = ev.next();
        AliasVar  avar = analyzer.addVariable(d, addrVar);

        aliasVars.addElement(avar);

        AliasAnnote.create(d, creator, Support.systemTrue, avar);
      }
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Compute the size-points-to statistic.
   */
  protected void computeSizeOfPointsToSet()
  {
    int l = aliasVars.size();
    for (int i = 0; i < l; i++) {
      AliasVar av = aliasVars.elementAt(i);
      sizePointsToCount += av.pointsToSize();
    }
  }

  /**
   * Print the aliasing information.
   */
  public void printAliasInfo()
  {
    System.out.println("Alias information:");

    int l = aliasVars.size();
    for (int i = 0; i < l; i++) {
      System.out.print("  ");
      System.out.println(aliasVars.elementAt(i));
    }

    System.out.println("Alias points-to relations:");

    for (int i = 0; i < l; i++) {
      AliasVar    av = aliasVars.elementAt(i);
      Vector<ECR> pt = av.pointsTo();
      int         n  = pt.size();

      if (n <= 0)
        continue;

      System.out.print("  ");
      System.out.print(av);
      System.out.println(" => {");
      System.out.print("    ");
      System.out.println(pt.elementAt(0));
      
      for (int j = 1; j < n; j++) {
        AliasVar pv = (AliasVar) pt.elementAt(j).getTypeVar();
        String   d  = pv.getDeclName();

        System.out.print("    ");
        System.out.println(d);
      }
      System.out.println("}");
    }
  }
}
