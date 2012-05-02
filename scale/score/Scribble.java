package scale.score;

import java.util.Iterator;
import java.util.AbstractCollection;
import java.util.Enumeration;
import java.util.LinkedList;

import scale.common.*;
import scale.annot.*;
import scale.frontend.SourceLanguage;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.expr.Expression;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.IntArrayLiteral;
import scale.clef.expr.StringLiteral;
import scale.clef.expr.AddressLiteral;
import scale.clef.expr.AggregationElements;
import scale.clef.expr.NilOp;
import scale.clef.LiteralMap;

import scale.callGraph.*;

import scale.score.chords.*;
import scale.score.analyses.*;
import scale.score.expr.*;
import scale.score.pred.References;
import scale.score.dependence.*;
import scale.score.trans.Optimization;
import scale.score.pp.PPCfg;

/**
 * This class represents a Scribble graph (CFG).
 * <p>
 * $Id: Scribble.java,v 1.323 2007-10-29 13:41:44 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Scribble graphs are built for one procedure at a time, so this
 * class represents a single procedure.
 * <p>
 * Scale converts irreducible graphs into reducible graphs using
 * node-splitting.  This can create a memory & time problem when going
 * into and out of SSA form.  The problem results from generating
 * multiple definitions of temporary, compiler-created variables.
 * There may be thousands of them.  Each one then becomes a non-local
 * variable and requires phi-functions and re-naming.  The solution
 * adopted here is to bypass SSA form (and optimizations) for those
 * routines that had irreducible graphs.  Another solution is to
 * change Clef2Scribble to create more complex expression trees
 * instead of placing one expression per CFG node.  This solution
 * should provide a significant speed-up because:
 * <ul>
 * <li>there will be vastly fewer varaibles, and
 * <li>the scans over the CFG will be faster (fewer nodes).
 * </ul>
 * The downside is that many optimizations currently depend on having
 * simple expression trees and will need re-writting.
 * <p>
 * Scalar variables have {@link scale.score.analyses.Aliases alias}
 * information.  Data dependence for scalar variables is represented
 * by use-def links in {@link scale.score.SSA SSA form}.  Array
 * references have data dependence information represented via a
 * {@link scale.score.dependence.DDGraph DDGraph} instance and no
 * alias information.
 */

public final class Scribble extends Root 
{
  /**
   * The CFG is not in SSA form.
   */
  public static final int notSSA = 0;
  /**
   * The CFG is in SSA form.
   */
  public static final int validSSA = 1;
  /**
   * The CFG is in an invalid SSA form.
   */
  public static final int invalidSSA = 2;

  /**
   * Set true to combine if-then-elses where possible.
   */
  public static boolean doIfCombine = true;
  /**
   * Set true to convert if-then-else to conditional expressions where
   * possible.
   */
  public static boolean doIfConversion = true;
  /**
   * Set true to report elapsed times for each optimization.
   */
  public static boolean reportOptTimes = false;
  /**
   * If the count of possible implicit loops exceeds this value, no
   * conversion of implicit loops to explicit loops is performed
   * and no optimizations that require SSA form are performed.
   */
  public static int maxImplicitLoopFactor = 5;
  /**
   * Profile block execution.
   */
  public static final int PROFILE_BLOCKS = 0x001;
  /**
   * Profile if-then-else choices.
   */
  public static final int PROFILE_EDGES  = 0x002;
  /**
   * Do path profiling.
   */
  public static final int PROFILE_PATHS  = 0x004;
  /**
   * Do loop iteration count profiling.
   */
  public static final int PROFILE_LOOPS  = 0x008;
  /**
   * Do loop instruction count profiling.
   */
  public static final int PROFILE_LICNT  = 0x010;

  private static int implicitLoopCount   = 0; // A count of implicit loops processed.
  private static int deadVarCFGNodeCount = 0; // A count of nodes removed because they set dead variables.
  private static int newCFGNodeCount     = 0; // A count of new nodes created because of implicit loops.
  private static int deadVariableCount   = 0; // A count of variables removed because they were unused.
  private static int irreducibleCount    = 0; // A count of routines that were irreducible.
  private static int ifsReducedCount     = 0; // A count of if-then-else nodes replaced by "?:".
  private static int ifsCombinedCount    = 0; // A count of if-then-else nodes combined with another if-then-else node.

  private static final String[]  stats = {"implicitLoops",
                                          "deadVarCFGNodes",
                                          "newCFGNodes",
                                          "deadVariables",
                                          "irreducible",
                                          "ifsReduced",
                                          "ifsCombined"};

  private static final CreatorSource creator = new CreatorSource("Scribble");

  static
  {
    Statistics.register("scale.score.Scribble", stats);
  }

  /**
   * Return the current number of implicit loops found.
   */
  public static int implicitLoops()
  {
    return implicitLoopCount;
  }

  /**
   * Return the number of dead nodes removed.
   */
  public static int deadVarCFGNodes()
  {
    return deadVarCFGNodeCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * Return the number of dead variables removed.
   */
  public static int deadVariables()
  {
    return deadVariableCount;
  }

  /**
   * Return the number of irreducible routines.
   */
  public static int irreducible()
  {
    return irreducibleCount;
  }

  /**
   * Return the number of if-then-else nodes changed to conditional
   * expressions.
   */
  public static int ifsReduced()
  {
    return ifsReducedCount;
  }

  /**
   * Return the number of if-then-else nodes eliminated by combining
   * them with another if-then-else.
   */
  public static int ifsCombined()
  {
    return ifsCombinedCount;
  }

  /**
   * The number of slots to use in the loop trip count table for each
   * loop.
   */
  public static final int LTC_TABLE_SIZE = 100;

  private static UniqueName un = new UniqueName("_s"); // Generate names for temporaries.

  // The following are used for profiling.

  private static RecordType  pfStruct = null; // Runtime profiling data structure.
  private static Type        pfct     = null;
  private static Type        pfpct    = null;
  private static IntegerType pfit     = null;
  private static Type        pfpit    = null;
  private static IntegerType pfit64   = null;
  private static Type        pfpit64  = null;
  private static Literal     pflit1   = null;
  private static Literal     pflitl0  = null;
  private static Literal     pflit0   = null;
  private static RoutineDecl ltcRDecl = null; // Runtime loop profiling routine.
  private static RoutineDecl hashFtn  = null;

  // Finite state machine for putting CFG in the proper state before
  // applying an optimization.

  private static final int NOPT_SSA  = 0; // No action required.
  private static final int ENTER_SSA = 1; // Enter SSA form.
  private static final int LEAVE_SSA = 2; // Leave SSA form.
  private static final int REDO_SSA  = 3; // Leave SSA form and re-enter.
  private static final int ERROR_SSA = 4; // Leave SSA form and re-enter.

  private static final int[] actions = {
    //                             SSA status   |  SSA request
    // no SSA  valid SSA  invalid SSA  ???      |
    NOPT_SSA,  LEAVE_SSA, LEAVE_SSA, ERROR_SSA, // Optimization.NO_SSA
    ENTER_SSA, NOPT_SSA,  NOPT_SSA,  ERROR_SSA, // Optimization.IN_SSA
    ENTER_SSA, NOPT_SSA,  REDO_SSA,  ERROR_SSA, // Optimization.VALID_SSA
    NOPT_SSA,  NOPT_SSA,  NOPT_SSA,  ERROR_SSA, // Optimization.NA_SSA
  };

  private BeginChord        begin;          //  First node in the CFG.
  private EndChord          end;            //  Last node in the CFG.
  private Vector<Declaration> declarations;   //  A list of declarations that go with the graph.
  private References        references;     //  Def and use lists.
  private DominanceFrontier df;             //  The dominance frontier.
  private DominanceFrontier pdf;            //  Dominance frontier based on post dominators.
  private Domination        dom;            //  The dominance information..
  private Domination        pDom;           //  The post dominance information..
  private SourceLanguage    sourceLang;     //  source lang. used to generate Scribble.
  private SSA               ssa;            //  The SSA instance if it is in SSA form.
  private Vector<String>    warnings;       //  A vector of messages to be generated in the generated .s or .c file.
  private RoutineDecl       cn;             //  The routine to which this CFG is attached.
  private CallGraph         cg;             //  The call graph to which this CFG is attached.
  private int[]             ucArray;        //  A map from loop number to the unroll count of the loop body, as read from a static profile.
  private int[]             icArray;        //  A map from loop number to the instruction count of the loop body, as read from a static profile.
  private int[]             ltcArray;       //  A map from loop number to the unroll count determined by loop histograms, as read from a static profile.
  private int               nxtLoopNumber;  //  Provide a unique loop ID to each loop in the CFG.
  private int               ssaForm;        //  True if the CFG is in valid SSA form.
  private boolean           ssaDone;        //  True if the CFG has ever been in SSA form.
  private boolean           srDone;         //  True if scalar replacement has been performed on the CFG.
  private boolean           containedGotos; //  Set true if the original graph may be irreducible.
  private boolean           irreducible;    //  Set true if the original graph was irreducible.

  /**
   * The variable that points to profiling information.  The first
   * time addProfiling() is called, this variable is computed and
   * returned.  On later calls, the variable is just returned.
   */
  private VariableDecl    pfvd;  // Runtime profiling variable.
  private IntArrayLiteral icAr;  // Loop instruction count array.
  private PPCfg           ppcfg; // The path profiling representation of this CFG.

  /**
   * Creates a Scribble object to collect information about a Scribble
   * CFG.
   * @param sourceLang the source language
   */
  public Scribble(RoutineDecl cn, SourceLanguage sourceLang, CallGraph cg)
  {
    super();
    this.cn            = cn;
    this.cg            = cg;
    this.sourceLang    = sourceLang;
    this.nxtLoopNumber = 0;
  }

  /**
   * Attach the CFG information to the Scribble node.  As part of
   * this, the dominators of the CFG nodes are determined.
   * @param begin the starting node in the graph
   * @param end the ending node in the graph
   * @param declarations a vector of the Clef Declarations associated
   * with this graph
   * @param containedGotos true if the CFG may be irreducible
   */
  public void instantiate(BeginChord begin,
                          EndChord   end,
                          Vector<Declaration> declarations,
                          boolean    containedGotos)
  {
    this.begin           = begin;
    this.end             = end;
    this.declarations    = declarations;
    this.containedGotos  = containedGotos;
    this.ssaForm         = notSSA;
    this.srDone          = false;
    this.ssaDone         = false;

    this.references    = null;
    this.df            = null;
    this.pdf           = null;
    this.dom           = null;
    this.pDom          = null;

    inspectVariables();

    simplifyIfs();

    // Compute the dominators and dominance frontiers.  We must do
    // this now to insure that every CFG graph is reducible.  We don't
    // want to repeat this loop again as it won't be necessary.

    boolean flg = false;
    irreducible = false;

    dom = new Domination(false, begin); /* Do dominators */
    dom.removeDeadCode();

    // Validity checking can not be performed until dead code is
    // eliminated.

    if (Debug.debug(1))
      validateCFG();

    try {
      do {
        df = new DominanceFrontier(begin, getDomination());
        if (!containedGotos)
          break;

        flg = df.makeGraphReducible(maxImplicitLoopFactor);

        if (flg)
          dom = new Domination(false, begin); /* Do dominators */

      } while (flg);
    } catch (scale.common.ResourceException ex) {
      Msg.reportInfo(Msg.MSG_Routine_s_can_not_be_optimized, cg.getName(), 0, 0, cn.getName());
      irreducible = true; // Too many implicit loops.
      irreducibleCount++;
    }

    // Make sure that all loops are in standard form.

    if (Debug.debug(1))
      validateCFG();

    normalizeLoopStructure();

    if (Debug.debug(1))
      validateCFG();
  }

  private void inspectVariables()
  {
    // Mark equivalenced variables that overlap another variable.
    // Variables in COMMON are equivalenced but do not necessarily
    // overlap.

    Table<VariableDecl, EquivalenceDecl> table = new Table<VariableDecl, EquivalenceDecl>();
    int   l     = declarations.size();
    for (int i = 0; i < l; i++) {
      Declaration decl = declarations.get(i);
      if (!decl.isEquivalenceDecl())
        continue;

      EquivalenceDecl ed   = (EquivalenceDecl) decl;
      VariableDecl    base = ed.getBaseVariable();
      table.put(base, ed);
    }

    Enumeration<VariableDecl> be = table.keys();
    while (be.hasMoreElements()) {
      VariableDecl base = be.nextElement();
      Object[]     vds  = table.getRowArray(base);
      for (int i = 0; i < vds.length - 1; i++) {
        EquivalenceDecl edvi = (EquivalenceDecl) vds[i];
        long            offi = edvi.getBaseOffset();
        long            endi = offi + edvi.getCoreType().memorySize(Machine.currentMachine);

        for (int j = i + 1; j < vds.length; j++) {
          EquivalenceDecl edvj  = (EquivalenceDecl) vds[j];
          long            offj = edvj.getBaseOffset();
          if ((offi < offj) && (endi <= offj))
            continue;

          long endj = offj + edvj.getCoreType().memorySize(Machine.currentMachine);
          if ((offj < offi) && (endj <= offi))
            continue;

          edvi.setHiddenAliases();
          edvj.setHiddenAliases();
        }
      }
    }
  }


  /**
   * Create a new temporary variable for use by the optimized code.
   * The variable definition is added to the set of variables for this
   * CFG.
   */
  protected VariableDecl genTemp(Type t)
  {
    VariableDecl vd = new VariableDecl(un.genName(), t);
    vd.setTemporary();
    addDeclaration(vd);
    return vd;
  }

  /**
   * Provide unique loop IDs for each loop in the CFG.
   * The BeginChord instance is loop 0.
   */
  public final int getNextLoopNumber()
  {
    return nxtLoopNumber++;
  }

  private static class P
  {
    public VariableDecl vd;
    public Expr l;
    public Expr r;

    public P(VariableDecl vd, Expr l, Expr r)
    {
      this.vd = vd;
      this.l = l;
      this.r = r;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(P");
      buf.append(hashCode());
      buf.append(" vd:");
      buf.append(vd);
      buf.append(" l:");
      buf.append(l);
      buf.append(" r:");
      buf.append(r);
      return buf.toString();
    }
  }

  /**
   * Scan the CFG looking for if-then-else nodes that might be
   * replacable by the conditional expression.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   */
  private void simplifyIfs()
  {
    if (!doIfConversion && !doIfCombine)
      return;

    Stack<Chord>           wl  = WorkArea.<Chord>getStack("reduceGraph");
    Stack<IfThenElseChord> ifs = WorkArea.<IfThenElseChord>getStack("reduceGraph");

    boolean doIfC = Machine.currentMachine.hasCapability(Machine.ALL_CONDITIONAL_MOVES);

    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();

    // Get a list of all of the if-then-else nodes in the CFG.

    while (!wl.isEmpty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);
      if (c instanceof IfThenElseChord)
        ifs.push((IfThenElseChord) c);
    }

    // Try to reduce every if-then-else as long as one has been reduced.

    boolean flag = true;
    int     l    = ifs.size();
    while (flag) {
      flag = false;
      for (int i = 0; i < l; i++) {
        IfThenElseChord ifc = ifs.elementAt(i);
        if (ifc == null)
          continue;
        if (doIfCombine && combineIfs(ifc)) {
          flag = true;
          ifs.setElementAt(null, i);
          continue;
        }
        if (doIfC && doIfConversion && reduceToConditionalExpr(ifc)) {
          flag = true;
          ifs.setElementAt(null, i);
        }
      }
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<IfThenElseChord>returnStack(ifs);
  }

  /**
   * Try to combine two ifs into one if possible.
   * Two ifs can be combined if one if is the immediate predecessor of
   * the other if and the other out-going CFG edge of the predecessor
   * goes to the same node as one of the out-going edges of the other if.
   * @return true if this if-then-else was eliminated by combining it
   * with another if-then-else
   */
  private boolean combineIfs(IfThenElseChord child)
  {
    if (child.numInCfgEdges() != 1)
      return false;

    Chord pre = child.getFirstInCfgEdge();
    if (!(pre instanceof IfThenElseChord))
      return false;

    Expr cpred = child.getPredicateExpr();
    if (cpred.sideEffects() > Expr.SE_OVERFLOW)
      return false;

    if (cpred.isMatchExpr() &&
        cpred.getOperand(0).getCoreType().isRealType() &&
        !Machine.currentMachine.hasCapability(Machine.HAS_INT_FROM_FP_CMP))
      return false;

    IfThenElseChord parent = (IfThenElseChord) pre;
    Expr            ppred  = parent.getPredicateExpr();
    if (ppred.sideEffects() > Expr.SE_OVERFLOW)
      return false;

    if (ppred.isMatchExpr() &&
        ppred.getOperand(0).getCoreType().isRealType() &&
        !Machine.currentMachine.hasCapability(Machine.HAS_INT_FROM_FP_CMP))
      return false;

    Chord ct = child.getTrueCfgEdge();
    Chord cf = child.getFalseCfgEdge();
    Chord pt = parent.getTrueCfgEdge();
    Chord pf = parent.getFalseCfgEdge();

    Expr cond = null;
    if (ct == pt) {
      cpred.deleteOutDataEdge(child);
      cond = new OrExpr(BooleanType.type, ppred.copy(), cpred);
    } else if (ct == pf) {
      cpred.deleteOutDataEdge(child);
      Expr la = ppred.isMatchExpr() ? (Expr) ((MatchExpr) ppred).complement() :
                                      (Expr) new NotExpr(BooleanType.type, ppred.copy());
      cond = new OrExpr(BooleanType.type, la, cpred);
    } else if (cf == pt) {
      cpred.deleteOutDataEdge(child);
      Expr la = ppred.isMatchExpr() ? (Expr) ((MatchExpr) ppred).complement() :
                                      (Expr) new NotExpr(BooleanType.type, ppred.copy());
      cond = new AndExpr(BooleanType.type, la, cpred);
    } else if (cf == pf) {
      cpred.deleteOutDataEdge(child);
      cond = new AndExpr(BooleanType.type, ppred.copy(), cpred);
    } else
      return false;

    ct.deleteInCfgEdge(child);
    cf.deleteInCfgEdge(child);

    parent.changeInDataEdge(ppred, cond);
    parent.setTrueEdge(ct);
    parent.setFalseEdge(cf);

    ifsCombinedCount++;

    return true;
  }

  /**
   * Attempt to convert an if-then-else to a "?:".
   * @return true if if-then-else was converted
   */
  private boolean  reduceToConditionalExpr(IfThenElseChord c)
  {
    Chord outt   = c.getTrueCfgEdge();
    Chord jt     = outt;
    Chord firstt = null;  // First node in true branch.
    Chord lastt  = c;     // Last node in true branch.
    if (outt.numInCfgEdges() == 1) {
      lastt = outt.lastInBasicBlock();
      if (lastt.numOutCfgEdges() != 1)
        return false;
      jt = lastt.getOutCfgEdge(0);
      firstt = outt;
    }

    Chord outf   = c.getFalseCfgEdge();
    Chord jf     = outf;
    Chord firstf = null;  // First node in false branch.
    Chord lastf  = c;     // Last node in false branch.
    if (outf.numInCfgEdges() == 1) {
      lastf = outf.lastInBasicBlock();
      if (lastf.numOutCfgEdges() != 1)
        return false;
      jf = lastf.getOutCfgEdge(0);
      firstf = outf;
    }

    if (jt != jf)
      return false; // The true and false branches do not go to the same place.

    HashSet<VariableDecl> var = WorkArea.<VariableDecl>getSet("reduceToConditionalExpr");  // The set of variables and the expressions that set them.
    Stack<P>              v   = WorkArea.<P>getStack("reduceToConditionalExpr"); // Preserve the order that the variables are defined. 

    // Check the CFG nodes along the true branch.
    // Don't allow any side effects because the expressions will
    // always be executed if the if-then-else is converted.
    // Every node must define a variable.

    boolean allFloating = true;  // True if all are floating point assignment.
    boolean allInteger  = true;  // True if all are integer assignment.
    boolean failed      = false;

    for (Chord st = firstt; ((st != null) && (st != jt)); st = st.getNextChord()) {
      if (st instanceof NullChord)
        continue;

      if (!st.isAssignChord()) {
        failed = true;
        break;
      }

      ExprChord se  = (ExprChord) st;
      Expr      lhs = se.getLValue();

      if (!(lhs instanceof LoadDeclAddressExpr)) {
        failed = true;
        break;
      }

      Expr rhs = se.getRValue();
      if ((rhs.sideEffects() > Expr.SE_OVERFLOW) ||
          !rhs.getCoreType().isAtomicType() ||
          !rhs.isSimpleExpr()) {
        failed = true;
        break;
      }

      VariableDecl od = (VariableDecl) ((LoadDeclAddressExpr) lhs).getDecl();
      if (od.isTemporary()) {
        // This may be the only def of the temporary.  If we convert
        //   if (pred) { temp = exp; var = temp; }
        // to
        //   temp = pred ? exp : temp;  var = pred ? temp : var;
        // then the live range for the register used for temp will
        // start at the first instruction of the function.  This may
        // cause excess register spilling.  The same will happen for
        // user variables that are not defined before they are used -
        // but that's the user's fault :-)
        failed = true;
        break;
      }

      if (!var.add(od)) {
        failed = true;
        break;
      }

      if (od.getCoreType().isRealType())
        allInteger = false;
      else
        allFloating = false;

      v.addElement(new P(od, rhs, null));
    }

    if (failed) {
      WorkArea.<VariableDecl>returnSet(var);
      WorkArea.<P>returnStack(v);
      return false;
    }

    int lend = v.size();
    var.clear();

    // Check the CFG nodes along the false branch.
    // Don't allow any side effects because the expressions will
    // always be executed if the if-then-else is converted.
    // Every node must define a variable.

    for (Chord sf = firstf; ((sf != null) && (sf != jf)); sf = sf.getNextChord()) {
      if (sf instanceof NullChord)
        continue;

      if (!sf.isAssignChord()) {
        failed = true;
        break;
      }

      ExprChord se  = (ExprChord) sf;
      Expr      lhs = se.getLValue();

      if (!(lhs instanceof LoadDeclAddressExpr)) {
        failed = true;
        break;
      }

      Expr rhs = se.getRValue();
      if ((rhs.sideEffects() > Expr.SE_OVERFLOW) ||
          !rhs.getCoreType().isAtomicType() ||
          !rhs.isSimpleExpr()) {
        failed = true;
        break;
      }

      VariableDecl od = (VariableDecl) ((LoadDeclAddressExpr) lhs).getDecl();
      if (od.isTemporary()) {
        // This may be the only def of the temporary.  If we convert
        //   if (pred) { temp = exp; var = temp; }
        // to
        //   temp = pred ? exp : temp;  var = pred ? temp : var;
        // then the live range for the register used for temp will
        // start at the first instruction of the function.  This may
        // cause excess register spilling.  The same will happen for
        // user variables that are not defined before they are used -
        // but that's the user's fault :-)
        failed = true;
        break;
      }

      if (!var.add(od)) {
        failed = true;
        break;
      }

      if (od.getCoreType().isRealType())
        allInteger = false;
      else
        allFloating = false;

      v.addElement(new P(od, null, rhs));
    }

    WorkArea.<VariableDecl>returnSet(var);
    if (failed) {
      WorkArea.<P>returnStack(v);
      return false;
    }

    int      rend = v.size();
    Stack<P> expl = WorkArea.<P>getStack("reduceToConditionalExpr");

    // Now merge the left and right expressions while preserving the
    // execution order.  If the order can't be preserved, we can't do
    // the conversion.

    // For each true clause, find the false clause.

    for (int i = 0; i < lend; i++) {
      P lp = v.elementAt(i);

      // Find the expression for the false clause of the conditional
      // expression.

      for (int j = lend; j < rend; j++) {
        P rp = v.elementAt(j);
        if (rp == null)
          continue;

        if (rp.vd != lp.vd)
          continue;

        // We have a candidate for the false clause.

        lp.r = rp.r;
        v.setElementAt(null, j);

        // See if the order of execution matters.
        // Check the remainder of the possible false clauses to see if
        // the variable is used prior to this false clause.

        for (int k = lend; k < j; k++) {
          P rp2 = v.elementAt(k);
          if (rp2 == null)
            continue;

          if (rp2.r.containsDeclaration(rp.vd) ||
              rp.r.containsDeclaration(rp2.vd)) { // Order of execution conflict.
            WorkArea.<P>returnStack(v);
            WorkArea.<P>returnStack(expl);
            return false;
          }
        }

        break;
      }

      expl.push(lp);
    }

    // Process the false clauses that have no corresponding true clause.

    for (int i = lend; i < rend; i++) {
      P rp = v.elementAt(i);
      if (rp == null)
        continue;
      expl.push(rp);
    }

    WorkArea.<P>returnStack(v);

    Expr predicate    = c.getPredicateExpr();
    int  capabilities = 0;
    if (predicate.getCoreType().isRealType()) {
      capabilities = !allInteger ? Machine.HAS_FF_CONDITIONAL_MOVE : 0;
      capabilities |= !allFloating ? Machine.HAS_FI_CONDITIONAL_MOVE : 0;
    } else {
      capabilities = !allInteger ? Machine.HAS_IF_CONDITIONAL_MOVE : 0;
      capabilities |= !allFloating ? Machine.HAS_II_CONDITIONAL_MOVE : 0;
    }

    if (!Machine.currentMachine.hasCapabilities(capabilities)) {
      WorkArea.<P>returnStack(expl);
      return false;
    }

    int l = expl.size();
    if (!allInteger) {
      // Must have both a true and false clause because garbage may
      // cause a floating point exception (e.g., NaN). Remember that
      // both the true and false clauses are evaluated for a
      // ConditionalExpr instance.

      for (int i = 0; i < l; i++) {
        P p = expl.elementAt(i);
        if (p.vd.getCoreType().isRealType() && ((p.r == null) || (p.l == null))) {
          WorkArea.<P>returnStack(expl);
          return false;
        }
      }
    }

    // At this point we can do the changes to the CFG.

    // Remove all the nodes from c to jt.  Replace with a begin-end
    // pair of NullChord instances so that the graph is maintained
    // properly.

    NullChord ne = new NullChord();
    NullChord ns = new NullChord();

    c.insertBeforeInCfg(ns);
    ns.setTarget(ne);
    ne.setTargetUnsafe(jt);
    jt.replaceInCfgEdge(lastt, ne);
    jt.replaceInCfgEdge(lastf, null);

    // The predicate to the if-then-else is the predicate for all the
    // "?:" expressions.

    predicate.deleteOutDataEdge(predicate.getOutDataEdge());

    if (l > 1) {
    // Save the predicate value in case it is dependent on something
    // that will be changed.

      VariableDecl pv = genTemp(predicate.getType());
      ExprChord    ec = new ExprChord(new LoadDeclAddressExpr(pv), predicate);
      ec.copySourceLine(c);

      ne.insertBeforeInCfg(ec);
      predicate = new LoadDeclValueExpr(pv);
    }

    // Insert the computations from both branches of the if-then-else
    // using new ConditionalExpr instances.

    for (int i = 0; i < l; i++) {
      P p = expl.elementAt(i);

      VariableDecl vd = p.vd;

      Expr rhst = p.l;
      if (rhst == null)
        rhst = new LoadDeclValueExpr(vd);
      else
        rhst.deleteOutDataEdge(rhst.getOutDataEdge());

      Expr rhsf = p.r;
      if (rhsf == null)
        rhsf = new LoadDeclValueExpr(vd);
      else
        rhsf.deleteOutDataEdge(rhsf.getOutDataEdge());

      Expr pd = predicate;
      if (rhst instanceof ConditionalExpr) {
        ConditionalExpr cd = (ConditionalExpr) rhst;
        if (cd.getFalseExpr().equivalent(rhsf)) {
          rhst = cd.getTrueExpr().copy();
          pd = new AndExpr(BooleanType.type, pd.conditionalCopy(), cd.getTest().copy());
        } else if (cd.getTrueExpr().equivalent(rhsf)) {
          rhst = cd.getFalseExpr().copy();
          Expr cpred = cd.getTest();
          Expr me    = null;
          if (cpred.isMatchExpr())
            me = ((MatchExpr) cpred).complement();
          else
            me = new NotExpr(BooleanType.type, cpred.copy());
          pd = new AndExpr(BooleanType.type, pd.conditionalCopy(), me);
        }
      } else if (rhsf instanceof ConditionalExpr) {
        ConditionalExpr cd = (ConditionalExpr) rhsf;
        if (cd.getTrueExpr().equivalent(rhst)) {
          rhsf = cd.getFalseExpr().copy();
          pd = new OrExpr(BooleanType.type, pd.conditionalCopy(), cd.getTest().copy());
        } else if (cd.getFalseExpr().equivalent(rhst)) {
          rhsf = cd.getTrueExpr().copy();
          Expr cpred = cd.getTest();
          Expr me    = null;
          if (cpred.isMatchExpr())
            me = ((MatchExpr) cpred).complement();
          else
            me = new NotExpr(BooleanType.type, cpred.copy());
          pd = new OrExpr(BooleanType.type, pd.conditionalCopy(), me);
        }
      }

      Expr ce;
      if (pd.isLiteralExpr()) {
        LiteralExpr litp = (LiteralExpr) pd;
        if (litp.isZero())
          ce = rhsf;
        else
          ce = rhst;
      } else {
        ce = ConditionalExpr.create(vd.getType(), pd.conditionalCopy(), rhst, rhsf);
      }

      ExprChord sce = new ExprChord(new LoadDeclAddressExpr(vd), ce);
      ne.insertBeforeInCfg(sce);
      sce.copySourceLine(c);
    }

    WorkArea.<P>returnStack(expl);

    // Remove the begin-end markers.

    ns.removeFromCfg();
    ne.removeFromCfg();

    ifsReducedCount++;

    return true;
  }

  /**
   * Specify that the CFG is in valid SSA form.
   */
  public void inSSAForm()
  {
    ssaForm = validSSA;
  }

  /**
   * Specify that the CFG is in invalid SSA form.
   */
  public void invalidSSAForm()
  {
    ssaForm = invalidSSA;
  }

  /**
   * Specify that the CFG is not in SSA form.
   */
  public void notSSAForm()
  {
    ssaForm = notSSA;
  }

  /**
   * Return an indication of the state of the SSA form of the CFG.
   */
  public int inSSA()
  {
    return ssaForm;
  }

  /**
   * Return the SSA instance for this CFG.
   */
  public final SSA getSSA()
  {
    return ssa;
  }

  /**
   * Specify that Scalar Replacement was performed on the CFG.
   */
  public void performedScalarReplacement()
  {
    srDone = true;
  }

  /**
   * Return true if scalar replacement has been performed.
   */
  public boolean scalarReplacementPerformed()
  {
    return srDone;
  }
  
  /**
   * Throw an error if the CFG has an invalid link.
   */
  public void validateCFG()
  {
    Stack<Chord>   wl        = WorkArea.<Chord>getStack("validateCFG");
    HashSet<Chord> reachable = WorkArea.<Chord>getSet("validateCFG");

    wl.push(begin);
    while (!wl.empty()) {
      Chord c = wl.pop();

      c.pushOutCfgEdges(wl, reachable);
      c.validate();
    }

    Iterator<Chord> er = reachable.iterator();
    while (er.hasNext()) {
      Chord node   = er.next();
      int   length = node.numInCfgEdges();
      for (int i = 0; i < length; i++) {
        Chord ie = node.getInCfgEdge(i);
        if (ie == null)
          throw new scale.common.InternalError("Null in edge in " + node);
        if (!reachable.contains(ie) && !(ie instanceof BeginChord))
          throw new scale.common.InternalError("Unreachable chord " +
                                               ie +
                                               " connected to " +
                                               node);
      }
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(reachable);
  }

  /**
   * Reduce the amount of memory used by this instance of a Scribble
   * graph.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   */
  public void reduceMemory()
  {
    df           = null;
    pdf          = null;
    dom          = null;
    pDom         = null;
    references   = null;
    ssa          = null;

    loopClean();
  }

  /**
   * Remove the loop information from the CFG.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   */
  private void loopClean()
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("loopClean");
    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();
    while (!wl.empty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);
      c.loopClean();
    }
    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Return true if the function is simple.
   * A simple function has no more than one loop.
   */
  public boolean isSimpleFunction()
  {
    LoopHeaderChord begin    = getBegin();
    int             numLoops = begin.numInnerLoops();

    if (numLoops > 1)
      return false;

    if (numLoops == 1) {
      LoopHeaderChord in = begin.getInnerLoop(0);
      if (!in.isInnerMostLoop())
        return false;
      if (in.numLoopExits() > 1)
        return false;
    }

    return true;
  }

  /**
   * Return the source language of the Scribble tree.
   */
  public final SourceLanguage getSourceLanguage()
  {
    return sourceLang;
  }

  /**
   * Return the dominator information for the CFG.
   */
  public Domination getDomination()
  {
    if (dom == null) {
      dom = new Domination(false, begin); /* Do dominators */
    }
    return dom;
  }

  /**
   * Return the dominator information for the CFG.
   */
  public Domination getPostDomination()
  {
    if (pDom == null)
      pDom = new Domination(true, end); /* Do dominators */

    return pDom;
  }

  /**
   * Removes statements that set the value of a variable that is not
   * used.  See Algorithm 19.12 in "Modern Compiler Implementation in
   * Java" by Appel.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param ssa true if SSA variable coalescing has not yet been
   * performed
   * @see scale.score.SSA#coalesceVariables
   */
  public void removeDeadVariables(boolean ssa)
  {
    References refs = getRefs();

    Stack<Chord>   wl   = WorkArea.<Chord>getStack("removeDeadVariables");
    HashSet<Chord> dead = WorkArea.<Chord>getSet("removeDeadVariables");
    int             l    = declarations.size();

    // BEGIN dead definition removal.

    HashMap<Chord, BitVect> inSets = new HashMap<Chord, BitVect>(203);
    
    Chord.nextVisit();
    
    // Set a unique tag for each declaration.

    for (int i = 0; i < l; i++) {
      Declaration d = declarations.elementAt(i);
      d.setTag(i);
    }

    // Begin at bottom since this is a backwards analysis.

    wl.push(end);
    inSets.put(end, new BitVect());
    end.setVisited();

    // Eliminate stores to variables whose value is not used.  The
    // outer loop is needed since removal of defs might have changed
    // liveness.

    while (!wl.empty()) {

      // The inner loop cycles through the CFG detecting possibly dead
      // defs.

      while (!wl.empty()) {
        Chord       s       = wl.pop();
        BitVect     inPrime = inSets.get(s);
        BitVect     out     = new BitVect();
        BitVect     uses    = null;
        Declaration kills   = null;

        // Compute live-out (union of successor's live-ins)

        Chord[] succ = s.getOutCfgEdgeArray();
        int     sl   = succ.length;
        for (int i = 0; i< sl; i++) {
          BitVect sin = inSets.get(succ[i]);
          // The successors might not have computed live-ins if they
          // are in a loop.  They will be caught when we circle back
          // through the CFG.
          if (sin != null)
            out.or(sin);
        }
        
        // There is no source code in a special chord so just pass set
        // through.

        if (!s.isSpecial()) {
          Vector<Declaration> varList = null; // list of declarations used by this chord
          if (s.isAssignChord()) {
            Expr lhs = ((ExprChord) s).getLValue();
            if (lhs instanceof LoadDeclAddressExpr) {
              Expr         rhs   = ((ExprChord) s).getRValue();
              VariableDecl d     = (VariableDecl) ((LoadDeclAddressExpr) lhs).getDecl();
              int          varId = d.getTag();
              
              varList = new Vector<Declaration>();
              lhs.getDeclList(varList);
              varList.remove(d);
              rhs.getDeclList(varList);
              
              if (!d.isGlobal() && !d.isFormalDecl() && !d.isEquivalenceDecl())
                kills = d;

              if (out.get(varId)) // What we are defining is live so remove it from dead set.
                dead.remove(s);
              else if (!d.isGlobal() &&
                       !d.addressTaken() &&
                       !d.isEquivalenceDecl() &&
                       !d.isStatic() &&
                       !d.isFormalDecl() &&
                       (lhs.sideEffects() == Expr.SE_NONE) && 
                       (rhs.sideEffects() == Expr.SE_NONE))
                // It cannot be removed if its scope is larger than this function
                // or it has side effects.
                dead.add(s);
            } else
              // The assignment is not simple - e.g. field of struct,
              // array element.  So, we do not want to kill anything,
              // but note all uses.
              varList = s.getDeclList();
          } else
            varList = s.getDeclList();

          if (varList != null) {
            uses = new BitVect();
            int vl= varList.size();
            for (int i = 0; i < vl; i++) {
              Declaration var = varList.elementAt(i);
              if (!var.isVariableDecl())
                continue;
              if (!var.isGlobal() &&
                  !((VariableDecl) var).isFormalDecl() &&
                  !var.isEquivalenceDecl())
                uses.set(var.getTag());
            }
          }
        }

        // Compute live-ins

        BitVect in = out.clone(); // live-outs
        if (kills != null)
          in.clear(kills.getTag()); // (live-outs - kills)
        if (uses != null)
          in.or(uses); // uses + (live-outs - kills)

        // Update the worklist.

        Chord[] preds = s.getInCfgEdgeArray();
        int     pl    = preds.length;
        // Check to see if the live-ins has changed.
        if (in.equivalent(inPrime))
          // Place only those nodes on the worklist that have not yet
          // been visited.
          s.pushInCfgEdges(wl);
        else {
          // The live-ins have changed so update it and place all
          // predecessors on the worklist.
          inSets.put(s, in);
          for (int j = 0; j < pl; j++) {
            Chord pred = preds[j];
            if (wl.search(pred) == -1) { // Add CFG node to the worklist if it is not already there.
              wl.add(pred);
              pred.setVisited();
            }
          }
        }
      }
      if (!dead.isEmpty()) {
        // These chords are dead.
        Iterator<Chord> e = dead.iterator();
        while (e.hasNext()) {
          Chord d = e.next();
          // Add the chords predecessors back to worklist since they have
          // a new successor and liveness might have changed.
          d.pushAllInCfgEdges(wl);
          d.removeFromCfg();
          // In case it was the predecessor of a previously removed chord.
          wl.remove(d);
        }
      }
    }

    dead.clear();

    // END dead definition removal
    
    if (ssa) {
      // Mark original variables of renamed variables.  If the
      // variable has been renamed, then the original variable can not
      // be deleted even if it has no uses.  This is to allow this
      // method to be called before SSA variable coalescing.

      for (int i = 0; i < l; i++) {
        Declaration d = declarations.elementAt(i);
        d.setTag(0);
      }
      for (int i = 0; i < l; i++) {
        Declaration d = declarations.elementAt(i);
        if (d.isRenamed()) {
          // Mark original variables as not being candidates for removal.
          Declaration od = ((RenamedVariableDecl) d).getOriginal();
          od.setTag(1);
        }
      }
    }

    // Obtain the set of all variables referenced in initializers.

    HashSet<Declaration> used =  WorkArea.<Declaration>getSet("removeDeadVariables");
    for (int i = 0; i < l; i++) {
      Declaration decl = declarations.elementAt(i);
      if (!decl.isVariableDecl())
        continue;
      Expression value = ((VariableDecl) decl).getValue();
      if (value != null)
        value.getDeclList(used);
    }

    WorkArea.<Chord>returnStack(wl);

    Stack<VariableDecl> wld = WorkArea.<VariableDecl>getStack("removeDeadVariables");

    // Determine which local variables are candidates for elimination.
    // We can't remove any variable that is accessible outside of this
    // function (CFG).  If it's not in our list of local
    // declarations or if it is but its scope is greater, than it is
    // not a candidate for deletion.

    for (int i = 0; i < l; i++) {
      Declaration decl = declarations.elementAt(i);

      if (ssa && (decl.getTag() != 0))
        continue;

      if (!decl.isVariableDecl())
        continue;

      if (decl.isGlobal())
        continue;

      if (decl.isEquivalenceDecl())
        continue;

      if (decl.addressTaken() && refs.anyUseChords(decl))
        continue;

      if (used.contains(decl))
        continue;

      // This one is a candidate for elimination.

      wld.push((VariableDecl) decl);
    }

    WorkArea.<Declaration>returnSet(used);

    while (!wld.empty()) {
      VariableDecl    decl = wld.pop();
      Iterator<Chord> ec   = refs.getUseChords(decl);

      if (ec.hasNext()) { // If it has some uses we can't get rid of the store.
        boolean allDead = true;

        while (ec.hasNext()) {
          Object o = ec.next();
          if (!dead.contains(o)) {
            allDead = false;
            break;
          }
        }
        if (!allDead)
          continue;
      }

      boolean removeVar = true;

      Iterator<Chord> ed = refs.getDefChords(decl);
      while (ed.hasNext()) { // for every definition
        ExprChord s   = (ExprChord) ed.next();
        Expr      rhs = s.getRValue();

        if ((rhs != null) && (rhs.sideEffects() >= Expr.SE_STATE)) { // E.g., va_arg()
          Expr exp = s.getLValue();
          s.setLValue(null);
          if (exp != null)
            exp.unlinkExpression();
          continue;
        }

        CallExpr call = s.getCall(true);

        if (call != null) {
          Expr exp = s.getLValue();
          if (exp != null) { // Probably a call to printf.
            s.setLValue(null);
            exp.unlinkExpression();
            continue;
          } else {
            removeVar = false;
            continue; // May have side effects
          }
        }

        dead.add(s);

        Vector<Declaration> varList = s.getDeclList();
        if (varList == null)
          continue;

        // If this one is dead, variables it used may also be dead.
        // They need to be checked again.

        int len = varList.size();
        for (int i = 0; i < len; i++) {
          Declaration d = varList.elementAt(i);
          if (d == decl)
            continue;

          if (!d.isVariableDecl())
            continue;

          if (d.isVirtual())
            continue;

          wld.push((VariableDecl) d);
        }
      }

      if (removeVar) {
        removeDeclaration(decl);
        deadVariableCount++;
      }
    }

    Iterator<Chord> es = dead.iterator();
    if (es.hasNext()) {
      while (es.hasNext()) {
        Chord s = es.next();
        s.removeRefs(refs);
        s.removeFromCfg();
        deadVarCFGNodeCount++;
      }

      recomputeDominators();
      recomputeRefs();
    }

    WorkArea.<Chord>returnSet(dead);
    WorkArea.<VariableDecl>returnStack(wld);
  }

  /**
   * Remove all DualExpr instances from the CFG.  Use the lower form.
   * This eliminates references to variables that may no longer be
   * needed.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   */
  public void removeDualExprs()
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("removeDualExprs");
    wl.push(begin);
    Chord.nextVisit();
    begin.setVisited();

    while (!wl.empty()) {
      Chord s = wl.pop();
      s.pushOutCfgEdges(wl);

      if (!s.removeDualExprs())
        continue;

      if (references != null) {
        s.removeRefs(references);
        s.recordRefs(references);
      }

      // The following logic does a copy propagation so that the
      // backends can avoid generating an address in a register and
      // then using it.  Instead of
      // <pre>
      //    r10 = r9 + 8
      //    load (r10)
      // </pre>
      // we can generate
      // <pre>
      //    load 8(r9)
      // </pre>

      if (!s.isAssignChord())
        continue;

      doCopyPropagate((ExprChord) s);
    }


    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Do a copy propagate if the def-use chain of the assignment has
   * only one link and there are no variables referenced whose address
   * is taken.
   */
  private void doCopyPropagate(ExprChord ec)
  {
    Expr rhs = ec.getRValue();

    if (!(rhs instanceof ArrayIndexExpr) &&
        !(rhs instanceof AdditionExpr) &&
        !(rhs instanceof SubtractionExpr))
      return;

    if (!rhs.optimizationCandidate())
      return;

    if (ssaForm != validSSA)
      return;

    if (ec.numDefUseLinks() != 1)
      return;

    Vector<Declaration> decls = ec.getDeclList();
    int    l     = decls.size();
    for (int i = 0; i < l; i++) {
      Declaration d = decls.elementAt(i);
      if (d.addressTaken()) 
        return;
    }

    LoadExpr le = ec.getDefUse(0);
    Chord    ss = le.getChord();

    le.getOutDataEdge().changeInDataEdge(le, rhs.copy());
    le.unlinkExpression();

    if (references != null) {
      ss.removeRefs(references);
      ss.recordRefs(references);
    }
  }

  /**
   * Removes a declaration from any list it might be on.
   * @param a is the variable
   */
  public void removeDeclaration(Declaration a)
  {
    declarations.removeElement(a);

    if (a.isVariableDecl()) {
      VariableDecl v = (VariableDecl) a;
      if (references != null)
        references.removeVars(v);
    }
  }

  /**
   * Return the reference information object.
   */
  public References getRefs()
  {
    if (references == null)
      references = new References();

    if (!references.isValid())
      references.compute(this);

    return references;
  }

  /**
   * Return a list of variables used in more than one basic block.
   * Create a list of non-local variables for use in creating a
   * pruned-SSA form.  This is the list of variables for which Phi
   * functions must be inserted.  The algorithm comes from
   * <cite>Practical Improvements to the Construction and Destruction
   * of Static Single Assignment Form</cite> by Briggs, et al, in
   * Software - Practice and Experience, Vol 1(1), 1-28 (January 1988.
   * This algorithm requires that multiple statements be placed into a
   * single basic block in order to determine whether a variable
   * reference is "non-local".  Since Scribble places each statement
   * into its own basic block, we must determine what "normally" would
   * be in a traditional basic-block.  We use an iterative algorithm
   * because the recursive algorithm proved too costly for Fortran
   * programs that have large basic blocks.
   */
  public Iterator<Declaration> getNonLocalVars()
  {
    Stack<Object>        wl           = WorkArea.<Object>getStack("getNonLocalVars");
    HashSet<Declaration> killed       = WorkArea.<Declaration>getSet("getNonLocalVars");
    HashSet<Chord>       done         = WorkArea.<Chord>getSet("getNonLocalVars");
    HashSet<Declaration> nonLocalVars = new HashSet<Declaration>(203); // The set of variables in this basic block that do not need a Phi function.
    HashMap<Declaration, Chord> defMap = new HashMap<Declaration, Chord>(203); // Map from defined variable to basic block it is defined in.
    boolean dChanged     = false;

    wl.push(begin);
    done.add(begin);

    Chord first = begin; // First Chord in the basic block.
    while (!wl.empty()) {
      Object wli = wl.pop();
      if (wli == this) { // Start of new basic block.
        killed.clear();
        first = null;
        continue;
      }

      Chord start = (Chord) wli;

      // Remove useless copies so that the result of this method is
      // more valid.  This speeds up the compiler by reducing the
      // amount of work required to go into SSA form.  The main source
      // of useless copies is the transition from SSA form to non-SSA
      // form.  So, this logic is of importance mainly when we go into
      // SSA form a second time.

      if (start.isAssignChord()) {
        ExprChord ec  = (ExprChord) start;
        Expr      lhs = ec.getLValue();
        if (lhs instanceof LoadDeclAddressExpr) {
          Expr rhs = ec.getRValue();
          if (rhs instanceof LoadDeclValueExpr) {
            if (((LoadExpr) lhs).getDecl() == ((LoadExpr) rhs).getDecl()) {
              wl.push(ec.getNextChord());
              ec.removeFromCfg(); // Useless copy.
              dChanged = true;
              continue;
            }
          }
        }
      }

      if (first == null)
        first = start;

      Expr def = start.getDefExpr();
      if (def instanceof LoadDeclValueExpr)
        def = null;

      Vector<LoadExpr> varList = start.getLoadExprList();
      if (varList != null) {
        int l= varList.size();
        for (int i = 0; i < l; i++) {
          LoadExpr le = varList.elementAt(i);
          if (le == def)
            continue;

          Declaration d = le.getDecl();
          if (!d.isVariableDecl())
            continue;

          Type dt = d.getCoreType();

          if (killed.contains(d) && !dt.isCompositeType() && !d.isGlobal())
            continue;

          if (d.isVirtual())
            d = ((VirtualVar) d).getSuperset();

          nonLocalVars.add(d);
        }
      }

      if (def instanceof LoadDeclAddressExpr) {
        Declaration d = ((LoadDeclAddressExpr) def).getDecl();

        killed.add(d);

        if (d.isVirtual())
          d = ((VirtualVar) d).getSuperset();

        // All this logic is just to make sure that variables that are
        // defined in multiple blocks, and may have no uses, are added
        // to the list of non-local variables so that phi-functions
        // are created when going into SSA mode.

        Chord chk = defMap.get(d);
        if (chk == null)
          defMap.put(d, first);
        else if (chk != first)
          nonLocalVars.add(d); // Defined in more than one basic block.
      }

      CallExpr call = start.getCall(true);
      if (call != null) {
        int l = call.numArguments();
        if (l > 0) {
          for (int i = 0; i < l; i++) {
            Expr arg = call.getArgument(i).getReference();
            if (!(arg instanceof LoadDeclAddressExpr))
              continue;

            Declaration d = ((LoadDeclAddressExpr) arg).getDecl();
            if (!d.isVariableDecl())
              continue;

            killed.add(d);

            if (d.isVirtual())
              d = ((VirtualVar) d).getSuperset();

            // All this logic is just to make sure that variables that
            // are defined in multiple blocks, and may have no uses,
            // are added to the list of non-local variables so that
            // phi-functions are created when going into SSA mode.

            Chord chk = defMap.get(d);
            if (chk == null)
              defMap.put(d, first);
            else if (chk != first)
              nonLocalVars.add(d); // Defined in more than one basic block.
          }
        }
      }

      int     l                = start.numOutCfgEdges();
      boolean childrenStartNew = (l > 1);
      for (int i = 0; i < l; i++) {
        Chord c = start.getOutCfgEdge(i);
        if (done.add(c)) {
          wl.push(c);
          // If child has more than one in-edge, it starts a new basic block.
          if (childrenStartNew || (c.numInCfgEdges() > 1))
            // Push marker indicating that the Chord begins a basic
            // block.  Else, re-use the killed from my parent who is
            // part of the same basic block.
            wl.push(this);
        }
      }
    }

    if (dChanged) {
      recomputeRefs();
      recomputeDominators();
    }

    WorkArea.<Object>returnStack(wl);
    WorkArea.<Declaration>returnSet(killed);
    WorkArea.<Chord>returnSet(done);

    return nonLocalVars.iterator();
  }

  private void nonLocalStats(HashSet<Declaration> nonLocalVars)
  {
    int dCount = 0;
    int vCount = 0;
    int gCount = 0;
    int tCount = 0;
    int rCount = 0;

    Iterator<Declaration> it = nonLocalVars.iterator();
    while (it.hasNext()) {
      Declaration d = it.next();
      dCount++;
      if (!d.isVariableDecl())
        continue;
      vCount++;
      if (d.isGlobal()) {
        gCount++;
        continue;
      }
      VariableDecl vd = (VariableDecl) d;
      if (vd.isTemporary()) {
        tCount++;
        continue;
      }
      rCount++;
    }

    System.out.print("** Non-local stats  d:" + dCount);
    System.out.print(" v:" + vCount);
    System.out.print(" g:" + gCount);
    System.out.print(" t:" + tCount);
    System.out.println(" r:" + rCount);
  }

  /**
   * Return the dominance frontier information.
   */
  public DominanceFrontier getDominanceFrontier()
  {
    if (df == null) {
      if (dom == null)
        dom = new Domination(false, begin); /* Do dominators */
      df = new DominanceFrontier(begin, dom);
    }
    return df;
  }

  /**
   * Return the post dominance frontier information.
   */
  public DominanceFrontier getPostDominanceFrontier()
  {
    if (pdf == null) {
      if (pDom == null)
        pDom = new Domination(true, end); /* Do dominators */
      pdf = new DominanceFrontier(end, pDom);
    }
    return pdf;
  }

  /**
   * Return true if there is an edge from <code>start</code> to 
   * <code>in</code> that goes not go through <code>term</code> .
   */
  private boolean pathExists(Chord start, Chord in, Chord term)
  {
    if (in == start)
      return true;

    Stack<Chord>   wl   = WorkArea.<Chord>getStack("pathExits");
    HashSet<Chord> done = WorkArea.<Chord>getSet("pathExits");

    wl.push(start);
    done.add(start);

    while (!wl.empty()) {
      Chord c = wl.pop();
      int   l = c.numOutCfgEdges();
      for (int i = 0; i < l; i++) {
        Chord s = c.getOutCfgEdge(i);
        if (s == in) {
          WorkArea.<Chord>returnStack(wl);
          WorkArea.<Chord>returnSet(done);
          return true;
        }
        if (s == term)
          continue;
        if (done.add(s))
          wl.push(s);
      }
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(done);

    return false;
  }
    
  /**
   * Return true if the source program was unstructured.
   */
  public boolean containedGotos()
  {
    return containedGotos;
  }

  /**
   * This method detects implicit loops in a CFG and converts them to
   * explicit loops using {@link scale.score.chords.LoopHeaderChord
   * LoopHeaderChord} and {@link scale.score.chords.LoopPreHeaderChord
   * LoopPreHeaderChord} classes.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @see scale.score.chords.LoopHeaderChord
   * @see scale.score.chords.LoopPreHeaderChord
   */
  private void normalizeLoopStructure()
  {
    if (containedGotos) {
      if (Debug.debug(1))
        Msg.reportInfo(Msg.MSG_s_contains_gotos, null, 0, 0, cn.getName());

      insertLoopHeaders();
    }

    buildLoopTree();
    insertLoopExits();

    if (Debug.debug(1))
      validateCFG();
  }

  private void insertLoopHeaders()
  {
    Chord start = begin.getNextChord();

    // Find the implicit loops and add loop headers.

    Stack<Chord>   wl   = WorkArea.<Chord>getStack("insertLoopHeaders");
    HashSet<Chord> done = WorkArea.<Chord>getSet("insertLoopHeaders");

    wl.push(start);
    wl.push(begin);
    done.add(start);

  main:
    while (!wl.empty()) {
      LoopHeaderChord loopHeader = (LoopHeaderChord) wl.pop();
      Chord           n          = wl.pop();

      while (n != null) {
        if (n.isLoopHeader()) {
          LoopHeaderChord me = (LoopHeaderChord) n;
          if (me.getParent() == null) // Assume Clef2Scribble was right.
            me.setParent(loopHeader);
          loopHeader = me;
          n = n.getNextChord();
          done.add(n); 
          continue;
        }

        if (n.isLoopExit()) {
          LoopExitChord le = (LoopExitChord) n;
          loopHeader = le.getLoopHeader();
          break;
        }

        if ((n.numInCfgEdges() > 1) &&
            df.inDominanceFrontier(n, n)) { // Found implicit loop.
          LoopHeaderChord    lh  = new LoopHeaderChord(this, loopHeader);
          LoopPreHeaderChord lph = new LoopPreHeaderChord(lh);
          LoopInitChord      lin = new LoopInitChord(lph);
          LoopTailChord      lt  = new LoopTailChord(lh);

          lh.copySourceLine(n);

          loopHeader = lh;

          lh.setLoopTail(lt);
          lh.setLoopInit(lin);

          implicitLoopCount++;
          newCFGNodeCount += 3;

          Chord[] inEdges = n.getInCfgEdgeArray();
          for (int i = 0; i < inEdges.length; i++) {
            Chord s = inEdges[i];
            if (pathExists(begin, s, n)) { // Change non-loop edges to point to the loop init.
              s.changeOutCfgEdge(n, lin);
            } else { // Change loop edges to point to the loop tail.
              s.changeOutCfgEdge(n, lt);
            }
          }

          done.add(lh); 
          lh.setTarget(n); // header -> node
          recomputeDominators();
          getDominanceFrontier();
          break;
        }

        Chord nxt = n.getNextChord();
        if (nxt == null)
          break;

        if (!done.add(nxt))
          continue main;
        n = nxt;
      }

      if (n == null)
        continue;

      int l = n.numOutCfgEdges();
      for (int i = 0; i < l; i++) {
        Chord c = n.getOutCfgEdge(i);

        if (!done.add(c))
          continue;

        wl.push(c);
        wl.push(loopHeader);
      }
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(done);
  }

  /**
   * This method determines the parent loop of every loop.
   * To build the correct loop structure the algorithm is basically
   * <pre>
   *    for all loop headers A
   *      let B be the immediate predecessor to A that is not the loop tail marker
   *      while true do
   *        while B is not null and B is not a loop header
   *          let B be the first predecessor to B
   *        while end
   *        if B is null, then A has no parent loop - next for all
   *        if a path exists from A to B then B is the parent of A - next for all
   *        let B be the immediate predecessor of B that is not the loop tail marker
   *      while end
   *    for all end
   * </pre>
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   */
  private void buildLoopTree()
  {
    Stack<Chord> wl    = WorkArea.<Chord>getStack("buildLoopTree");
    Stack<Chord> hdrs  = WorkArea.<Chord>getStack("buildLoopTree");
    Chord        start = begin.getNextChord();

    // Find all the loop headers.

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();
      if (c.isLoopHeader())
        hdrs.push(c);
      c.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);

    // Determine the loop tree.

    while (!hdrs.empty()) { // For all loops.
      LoopHeaderChord lh   = (LoopHeaderChord) hdrs.pop();
      Chord           pred = lh.getPreHeader();

      while (true) {

        // Find the predecessor loop of this loop.

        while ((pred != null) && !pred.isLoopHeader())
          pred = pred.getFirstInCfgEdge();

        if (pred == null) { // If none, this is a top level loop.
          lh.setParent(begin);
          break;
        }

        LoopHeaderChord plh   = (LoopHeaderChord) pred;
        Chord           pred2 = plh.getPreHeader();
        while ((pred2 != null) && !pred2.isLoopHeader())
          pred2 = pred2.getFirstInCfgEdge();

        // If you can get from this loop to the predecessor loop
        // without going through the predecessor's predecessor loop
        // then this loop must be a sub-loop of the predecessor loop.

        if (pathExists(lh, plh, pred2)) {
          lh.setParent(plh);
          break;
        }

        // The predecessor loop must be on the same level.

        pred = plh.getPreHeader();
      }
    }

    WorkArea.<Chord>returnStack(hdrs);
  }

  /**
   * Insert the {@link scale.score.chords.LoopExitChord  LoopExitChord}
   * instances.
   */
  private void insertLoopExits()
  {
    Stack<Chord> wl      = WorkArea.<Chord>getStack("insertLoopExits");
    Stack<Chord> wl2     = WorkArea.<Chord>getStack("insertLoopExits");
    boolean      changed = false;

    int l = begin.numInnerLoops();
    for (int i = 0; i < l; i++) {
      LoopHeaderChord lh = begin.getInnerLoop(i);
      changed |= insertLoopExits(lh, wl, wl2);
    }

    if (changed)
      recomputeDominators();

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnStack(wl2);
  }

  /**
   * Insert the {@link scale.score.chords.LoopExitChord  LoopExitChord}
   * instances.
   * <p>
   * Span the CFG from the loop tail marker back following the
   * in-coming CFG edges.  Mark every node encountered.  Record every
   * node with more than one out-going CFG edge.  The spanning
   * terminates when the loop header is encountered.
   * <p>
   * For every node A with more than one out-going edge, if it has an
   * out-going edge to node B and B is not marked then insert a loop
   * exit between A and B.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   */
  private boolean insertLoopExits(LoopHeaderChord lh, Stack<Chord> wl, Stack<Chord> wl2)
  {
    boolean changed = false;

    int l = lh.numInnerLoops();
    for (int i = 0; i < l; i++) {
      LoopHeaderChord clh = lh.getInnerLoop(i);
      changed |= insertLoopExits(clh, wl, wl2);
    }

    Chord.nextVisit();
    Chord start = lh.getLoopTail();
    if (start == null)
      return false;

    if (!dom.inIterativeDominatees(lh, start)) {
      // The loop header does not dominate the loop tail.
      // Example:
      //   switch (Mc) {
      //   case 3: *ep++ = 0;
      //   case 2:  do {
      //           *ep++ = 0;
      //   case 1: *ep++ = 0;
      //   case 0: *ep++ = *xMp++;
      //            } while (--i);
      //   }

      Msg.reportInfo(Msg.MSG_Routine_s_can_not_be_optimized,
                     cg.getName(),
                     0,
                     0,
                     cn.getName());
      irreducible = true; // Inhibit optimizations.
      irreducibleCount++;
      return false;
    }

    wl.push(start);
    start.setVisited();

    // Propagate back from the loop tail to every CFG node reachable
    // from it.  Record every branch node.

    while (!wl.empty()) {
      Chord c = wl.pop();
      if (c == lh)
        continue;
      int li = c.numInCfgEdges();
      for (int i = 0; i < li; i++) {
        Chord in = c.getInCfgEdge(i);
        if (in.visited())
          continue;
        if (in.numOutCfgEdges() > 1)
          wl2.push(in);
        wl.push(in);
        in.setVisited();
      }
    }

    // If a branch node exists with an out-going edge that has not
    // been visited, then that out-going edge is an exit from the
    // loop.

    while (!wl2.empty()) {
      Chord c  = wl2.pop();
      int   lo = c.numOutCfgEdges();
      for (int i = 0; i < lo; i++) {
        Chord out = c.getOutCfgEdge(i);
        if (out.visited())
          continue;
        insertLoopExit(lh, c, out);
        changed = true;
      }
    }

    if (lh.numLoopExits() == 0) {
      // Insert an impotent loop exit test to maintain the validity of
      // the CFG graph.  This makes sure the graph end node is attached.
      Expr            pred = new LiteralExpr(LiteralMap.put(1, BooleanType.type));
      LoopExitChord   le   = new LoopExitChord(lh, end);
      IfThenElseChord ifc  = new IfThenElseChord(pred, lh.getNextChord(), le);
      lh.setLoopTest(ifc);
      lh.setTarget(ifc);
      le.copySourceLine(end);
      ifc.copySourceLine(lh);
    }

    return changed;
  }

  /**
   * Return true if this routine has an irreducible CFG.
   */
  public final boolean isIrreducible()
  {
    return irreducible;
  }

  /**
   * Insert a loop exit for loop <code>lh</code> between the
   * <code>pred</code> node and the <code>succ</code> node in the CFG.
   */
  private void insertLoopExit(LoopHeaderChord lh, Chord pred, Chord succ)
  {
    // The loop exits should be in the proper order in the case where
    // a sub-loop exits directly out of its parent loop.

    while (succ.isLoopExit()) {
      LoopHeaderChord slh = succ.getLoopHeader();
      if (slh == lh)
        return;
      if (!slh.isSubloop(lh))
        break;
      pred = succ;
      succ = succ.getNextChord();
    }

    // Don't create a new loop exit if there is already one we can use.

    int l = succ.numInCfgEdges();
    if (l > 1) {
      for (int i = 0; i < l; i++) {
        Chord in = succ.getInCfgEdge(i);
        if (in.isLoopExit()) {
          LoopExitChord le = (LoopExitChord) in;
          if (le.getLoopHeader() == lh) { // Re-use an exisiting exit for this loop.
            pred.replaceOutCfgEdge(succ, le);
            le.addInCfgEdge(pred);
            succ.deleteInCfgEdge(pred);
            return;
          }
        }
      }
    }

    // Create a new loop exit.

    LoopExitChord lec = new LoopExitChord(lh);
    pred.insertAfterOutCfg(lec, succ);
    lec.copySourceLine(succ);
  }

  private LoopHeaderChord findLoopHeader(Chord c, LoopHeaderChord lh)
  {
    while (lh != null) {
      if (lh instanceof BeginChord)
        return lh; // It must be the loop header for c.
      if (inIteratedDominanceFrontier(c, lh))
        return lh;
      Object old = lh;
      lh = lh.getParent();
      assert (old != lh) : "Old == new";
    }
    throw new scale.common.InternalError("Not in any loop - " + c);
  }

  /**
   * Return true if b is in the iterated dominance frontier of c.
   */
  public boolean inIteratedDominanceFrontier(Chord c, Chord b)
  {
    Vector<Chord>   v  = new Vector<Chord>(10);
    Iterator<Chord> ed = df.getDominanceFrontier(c);
  
    while (ed.hasNext()) {
      Chord o = ed.next();
      if (o == b)
        return true;
      v.addElement(o);
    }

    int lastSize = 0;
    do {
      int oldSize = v.size();

      for (int i = lastSize; i < oldSize; i++) {
        Iterator<Chord> e = df.getDominanceFrontier(v.elementAt(i));

        while (e.hasNext()) {
          Chord o = e.next();
          if (o == b)
            return true;
          if (!v.contains(o))
            v.addElement(o);
        }
      }

      lastSize = oldSize;
    } while (v.size() != lastSize); 

    return false;
  }

  /**
   * This method is called if an optimization makes any change to the
   * CFG that would result in making the use - def information
   * invalid.
   */
  public void recomputeRefs()
  {
    if (references != null)
      references.setInvalid();
  }

  /**
   * This method is called if an optimization makes any change to the
   * CFG that would result in making the dominator tree invalid.
   */
  public void recomputeDominators()
  {
    dom  = null;
    pDom = null;
    df   = null;
    pdf  = null;
  }

  /**
   * Specify that all the loop information is no longer valid for this
   * CFG.
   */
  public void recomputeLoops()
  {
    begin.recomputeLoops();
  }

  /**
   * Return the head of this CFG.
   */
  public BeginChord getBegin()
  {
    return begin;
  }

  /**
   * Return the tail of the CFG.
   */
  public EndChord getEnd()
  {
    return end;
  }

  /**
   * Return the specified {@link scale.clef.decl.Declaration
   * declaration} associated with this CFG.
   */
  public Declaration getDecl(int i)
  {
    return declarations.elementAt(i);
  }
  
  /**
   * Return the count of {@link scale.clef.decl.Declaration
   * declarations} associated with this CFG.
   */
  public int numDecls()
  {
    return declarations.size();
  }

  /**
   * Add all {@link scale.clef.decl.Declaration declarations},
   * associated with this CFG, to the specified collection.
   */
  public void getAllDeclarations(AbstractCollection<Declaration> collection)
  {
    collection.addAll(declarations);
  }

  /**
   * Add a new {@link scale.clef.decl.Declaration declaration} to the
   * set of declarations for this CFG if it is not already there.
   * This method is designed for optimizations that may try to define
   * something twice such as Inlining.
   */
  public void addDeclaration(Declaration decl)
  {
    if (decl == null)
      return;

    if (declarations.contains(decl))
      return;

    declarations.addElement(decl);
  }

  /**
   * Return the {@link scale.clef.decl.RoutineDecl routine} associated
   * with this CFG.
   */
  public final RoutineDecl getRoutineDecl()
  {
    return cn;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(Scribble ");
    buf.append(cn);
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return the top loop which is the {@link
   * scale.score.chords.BeginChord BeginChord}.  The outermost code
   * containing the loop(s) is considered to be the level 0 loop.
   * Although technically it is not a loop, the root (level 0) is
   * encapsulated in a {@link scale.score.chords.LoopHeaderChord
   * LoopHeaderChord} instance for consistency of representation.
   * Nesting info is available thru the {@link
   * scale.score.chords.LoopHeaderChord LoopHeaderChord} instance.
   */
  public LoopHeaderChord getLoopTree()
  {
    return begin;
  }

  /**
   * This method is called if an optimization makes any change to the
   * CFG that would result in making the data dependence information
   * invalid.
   */
  public void recomputeDataDependence()
  {
    loopClean();
  }

  /**
   * Give unique labels to all CFG nodes.  For any given node n1,
   * node n1's label is greater than the labels of all its 
   * immediate predecessors.
   * <p>
   * Note - other algorithms may use the label field and destroy this
   * value.  An example is the computation of dominators.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @see scale.score.Domination
   */
  public void labelCFG()
  {
    int          label = 0;
    Stack<Chord> wl    = WorkArea.<Chord>getStack("labelCFG");

    Chord.nextVisit();
    wl.push(begin);
    begin.setLabel(label++);

    while (!wl.empty()) {
      Chord c = wl.pop();
      c.setVisited();

      int l = c.numOutCfgEdges();
      for (int i = 0; i < l; i++)
        label = c.getOutCfgEdge(i).pushChordWhenReady(wl, label);
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Label the CFG nodes for the backend code generator.  Only nodes
   * that begin a basic block are given non-zero label values.  This
   * method is intended to help code generators decide when to
   * generate labels in the output text.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param labelIndex is the first label index value to use
   * @return the next label index value to use
   */
  public int labelCfgForBackend(int labelIndex)
  {
    Stack<Chord> wl    = WorkArea.<Chord>getStack("labelCfgForBackend");
    Chord        start = begin.getNextChord();

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();
    begin.setLabel(0);

    while (!wl.empty()) {
      Chord   s      = wl.pop();
      boolean addLab = s.isFirstInBasicBlock();

      s.pushOutCfgEdges(wl);

      int cl = 0;
      if (addLab)
        cl = labelIndex++;

      s.setLabel(cl);
    }

    WorkArea.<Chord>returnStack(wl);
    return labelIndex;
  }

  /**
   * Append the CFG nodes to the specified vector in execution order.
   * All the nodes of a loop are guaranteed to be before the nodes
   * after the loop exits.
   * @see scale.score.trans.GlobalVarReplacement
   * @see scale.score.chords.Chord#pushChordWhenReady(Stack)
   * @see scale.score.chords.LoopHeaderChord#labelCFGLoopOrder
   */
  public void linearize(Vector<Chord> linear)
  {
    Stack<Chord>          wl    = WorkArea.<Chord>getStack("linearize");
    Vector<LoopExitChord> x     = new Vector<LoopExitChord>();
    int                   leCnt = 0;

    Chord.nextVisit();
    wl.push(begin);

    do {
      do {
        Chord c = wl.pop();
        c.setVisited();
        linear.addElement(c);

        int l = c.numOutCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord cs = c.getOutCfgEdge(i);
          if (cs.isLoopExit()) {
            LoopExitChord   le   = (LoopExitChord) cs;
            LoopHeaderChord lh   = le.getLoopHeader();
            LoopTailChord   tail = lh.getLoopTail();

            // We can actually have a loop tail with no in-coming CFG edges.
            // For example
            //  do {
            //     ...
            // } while (0);

            if ((tail != null) &&
                (tail.numInCfgEdges() > 0) &&
                !tail.visited()) {
              x.addElement(le);
              leCnt++;
              continue;
            }
          }

          cs.pushChordWhenReady(wl);
        }
      } while (!wl.empty());

      int l = x.size();
      for (int i = l - 1; i >= 0; i--) {
        LoopExitChord le = x.elementAt(i);
        if (le == null)
          continue;

        LoopHeaderChord lh   = le.getLoopHeader();
        LoopTailChord   tail = lh.getLoopTail();
        if (!tail.visited())
          continue;

        wl.push(le);
        x.setElementAt(null, i);
        leCnt--;
      }

    } while (!wl.empty());

    assert (leCnt == 0) || checkLoopExits(x) : "Loop exit un-processed.";

    WorkArea.<Chord>returnStack(wl);
  }

  private boolean checkLoopExits(Vector<LoopExitChord> x)
  {
    int l = x.size();
    for (int i = 0; i < l; i++) {
      LoopExitChord xc = x.elementAt(i);
      if (xc != null) {
        System.out.print("** Loop exit un-processed ");
        System.out.print(xc.getLoopHeader().getSourceLineNumber());
        System.out.print(" ");
        System.out.println(xc);
        return false;
      }
    }
    return true;
  }

  private long reportOptTime(String msg, long ct)
  {
    if (!reportOptTimes)
      return 0;

    long t = System.currentTimeMillis();
    System.out.print(msg);
    System.out.print(" ");
    System.out.println(t - ct);
    return t;
  }

  /**
   * Apply the specified optimization to the CFG.  Insure that the CFG
   * is in the proper SSA form for the optimization first.
   * @param opt is the optimization
   * @param pio is the proper class instance for the alias analysis
   * virtual variables
   * @return true if the optimization was performed
   */
  public boolean applyOptimization(Optimization opt, PlaceIndirectOps pio)
  {
    long ct     = System.currentTimeMillis();
    int  reqSSA = opt.requiresSSA();
    int  action = actions[ssaForm + (reqSSA << 2)];

    switch (action) {
    case NOPT_SSA: // No transition needed.
      break;
    case REDO_SSA: // Leave, then enter SSA form.
      ssa.removePhis();
      ct = reportOptTime("  scale.score.SSA.removePhis ", ct);
      ssa.coalesceVariables();
      recomputeRefs();
      ct = reportOptTime("  scale.score.SSA.coalesceVariables ", ct);
      ssa = new SSA(this, pio);
      ssa.buildSSA();
      ct = reportOptTime("  scale.score.SSA.buildSSA ", ct);
      break;
    case ENTER_SSA: // Enter SSA form.
      if (irreducible || (pio == null))
        return false;
      ssa = new SSA(this, pio);
      ssaDone = true;
      ssa.buildSSA();
      ct = reportOptTime("  scale.score.SSA.buildSSA ", ct);
      break;
    case LEAVE_SSA: // Leave SSA form.
      ssa.removePhis();
      ct = reportOptTime("  scale.score.SSA.removePhis ", ct);
      ssa.coalesceVariables();
      recomputeRefs();
      ct = reportOptTime("  scale.score.SSA.coalesceVariables ", ct);
      ssa = null;
      break;
    default:
      throw new scale.common.InternalError("Invalid SSA trannsition requested " +
                                           ssaForm +
                                           " " +
                                           reqSSA);
    }

    if ((action != NOPT_SSA) && Debug.debug(1))
      try {
        validateCFG();
      } catch (scale.common.InternalError ex) {
        Msg.reportError(Msg.MSG_Invalid_CFG_before_optimization_s,
                        cg.getName(),
                        0,
                        0,
                        opt.getClass().getName());
        throw ex;
      }

    opt.perform();

    ct = reportOptTime("  " + opt.getClass().getName(), ct);

    if (Debug.debug(1))
      try {
        validateCFG();
      } catch (scale.common.InternalError ex) {
        Msg.reportError(Msg.MSG_Invalid_CFG_after_optimization_s,
                        cg.getName(),
                        0,
                        0,
                        opt.getClass().getName());
        throw ex;
      }

    return true;
  }

  /**
   * If the CFG is in SSA form, exit SSA form.
   */
  public void exitSSA()
  {
    if (ssaForm == notSSA)
      return;

    ssa.removePhis();
    ssa.coalesceVariables();
    ssa = null;

    if (Debug.debug(1))
      validateCFG();
  }

  /**
   * Add a warning message to be displayed for this routine.
   */
  public final void addWarning(int msg, String text1, String text2)
  {
    if (warnings == null)
      warnings = new Vector<String>(3);
    warnings.add(Msg.insertText(msg, text1, text2));
  }

  /**
   * Return an enumeration of the warnings.
   */
  public final Enumeration<String> getWarnings()
  {
    if (warnings == null)
      return new EmptyEnumeration<String>();
    return warnings.elements();
  }

  /**
   * Make a copy of the CFG specified.  The new CFG is not linked (see
   * {@link #linkSubgraph linkSubgraph}.  The spanning of the CFG
   * halts when a node is encountered that is already in the map.
   * @param first specifies the first node to copy
   * @param nm maps from old node to new node
   * @param newn is a vector of the new nodes that are created
   * @param wl is a work stack - it is empty on exit
   * @return a vector of the special nodes (e.g., Phi CFG nodes &amp;
   * LoopHeaderChords) in the original graph that require special
   * treatment or null
   * @see #linkSubgraph
   */
  public static Vector<Chord> grabSubgraph(Chord first, HashMap<Chord, Chord> nm, Vector<Chord> newn, Stack<Chord> wl)
  {
    Vector<Chord> phis = null;
    
    wl.push(first);
    while (!wl.empty()) {
      Chord c = wl.pop();
      if (nm.get(c) != null)
        continue;

      if (c.isSpecial()) {
        if (phis == null)
          phis = new Vector<Chord>(10);
        phis.addElement(c);
      }

      Chord nn = c.copy();

      newCFGNodeCount++;

      nm.put(c, nn);
      newn.addElement(nn);

      c.pushAllOutCfgEdges(wl);
    }

    return phis;
  }

  /**
   * Given a mapping from old CFG nodes to new ones, create the
   * subgraph that is the duplicate of the old graph by replacing, in
   * the new nodes, edges to the old nodes with edges to the new
   * nodes.  The actual nodes in the subgraph, that are specified in
   * the mapping, may be determined in many ways:
   * <ol>
   * <li>from the dominance tree as is the case for irreducible graphs
   * <li>from the loop as is done for unroll
   * <li>from a complete routine as is done for inlining.
   * </ol>
   * @param newn is the list of the new nodes
   * @param nm is the mapping from old nodes to new ones
   * @param special is the set or original nodes that require special
   * treatment
   * @see #grabSubgraph
   */
  public static void linkSubgraph(Vector<Chord> newn, HashMap<Chord, Chord> nm, Vector<Chord> special)
  {
    int n = newn.size();
    for (int i = 0; i < n; i++) {
      Chord nn = newn.elementAt(i);
      nn.linkSubgraph(nm);
    }
    
    // Fixup phi functions so that their arguments match their
    // in-coming CFG edges.

    if (special == null)
      return;

    int l = special.size();
    for (int i = 0; i < l; i++) {
      Chord c = special.elementAt(i);
      c.reorderInCfgEdgesOfCopy(nm);
    }
  }

  /**
   * Add the profiling information to this CFG.  A variable is defined
   * that holds the profiling information.  It is an struct of ten
   * fields.  The first entry points to a C string containing the name
   * of the function.
   * <p>
   * The second entry is the size of the next two arrays.  The third
   * entry points to an array of C <tt>int</tt>s containing line
   * numbers from the source code or is null.  The fourth entry points
   * to the array of C <tt>int</tt>s of block frequencies or is null.
   * There is a one-to-one correspondence between the entries in the
   * line number array and the entries in the frequency array.
   * <p>
   * The fifth entry is the size of the next two arrays.  The sixth
   * entry points to an array of C <tt>int</tt>s containing edges from
   * the source code or is null.  The seventh entry points to the
   * array of C <tt>int</tt>s of edge frequencies or is null.  There
   * is a one-to-one correspondence between the entries in the line
   * number array and the entries in the frequency array.  The edges
   * in the line number array are encoded as a pair (src, dst) of line
   * numbers.
   * <p>
   * The eighth entry is the size of the next array.  The ninth entry
   * points to an array of C <tt>int</tt>s of path numbers.  The tenth
   * entry points to an array of C <tt>int</tt>s of path frequencies
   * or is null.
   * <p>
   * The eleventh entry is the number of loops.  The twelfth entry is
   * the loop trip count histogram (LTCH) table, which has size
   * numLoops * LTC_TABLE_SIZE.
   * <p>
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param profileOptions specifies which profiling instrumentation to insert
   * @param isMain - is true if this is the "main" entry node
   * @return the variable definition for the profiling information
   */
  public VariableDecl addProfiling(int profileOptions, boolean isMain)
  {
    boolean doBlocks = (profileOptions & PROFILE_BLOCKS) != 0;
    boolean doEdges  = (profileOptions & PROFILE_EDGES)  != 0;
    boolean doPaths  = (profileOptions & PROFILE_PATHS)  != 0;

    // Check if we've already been here and computed the profile
    // variable.

    if (pfvd != null)
      return pfvd;

    // Check if this routine should not be instrumented.

    if (PPCfg.doNotInstrument(this)) {
      
      // If this routine is main(), we still want it to call __pf_profile_dump().
      if (isMain) {
        addProfileDumpToMain(cg);
        
        // We modified the CFG, so invalidate references, data
        // dependences, and dominators.
        recomputeRefs();
        recomputeDataDependence();
        recomputeDominators();
      }
        
      return null;
    }

    // Create the profiling struct type.

    if (pfStruct == null) {
      pfct      = SignedIntegerType.create(8);
      pfpct     = PointerType.create(pfct);
      pfit      = SignedIntegerType.create(32);
      pfpit     = PointerType.create(pfit);
      pfit64    = SignedIntegerType.create(64);
      pfpit64   = PointerType.create(pfit64);
      pflit1    = LiteralMap.put(1, pfit);
      pflit0    = LiteralMap.put(0, pfit);
      pflitl0   = LiteralMap.put(0, pfit64);

      Vector<FieldDecl>      fields    = new Vector<FieldDecl>(16);
      FieldDecl   fnamef    = new FieldDecl("fname",     pfpct);   // Name of the function.
      FieldDecl   hash      = new FieldDecl("hash",      pfit);    // CFG hash number.
      FieldDecl   numbf     = new FieldDecl("numblks",   pfit);    // Number of basic blocks in the function.
      FieldDecl   lblkf     = new FieldDecl("lblk",      pfpit);   // Source file line number for each block.
      FieldDecl   blkcntf   = new FieldDecl("blkcnt",    pfpit);   // Profile block count.
      FieldDecl   numef     = new FieldDecl("numedges",  pfit);    // Number of edges in the function.
      FieldDecl   lsrcf     = new FieldDecl("lsrc",      pfpit);   // Source file line number of edge start.
      FieldDecl   ldstf     = new FieldDecl("ldst",      pfpit);   // Source file line number of edge destination.
      FieldDecl   edgecntf  = new FieldDecl("edgecnt",   pfpit);   // Profile edge count.
      FieldDecl   ptblsizef = new FieldDecl("ptblsize",  pfit);    // Number of paths in the function.
      FieldDecl   pathnumsf = new FieldDecl("pathnums",  pfpit64); // Path numbers.
      FieldDecl   pathcntf  = new FieldDecl("pathcnt",   pfpit64); // Profile path count.
      FieldDecl   loopcntf  = new FieldDecl("loopcnt",   pfit);    // Number of loops in the function.
      FieldDecl   lloop     = new FieldDecl("lloop",     pfpit);   // Source file line number for each loop.
      FieldDecl   ltchtable = new FieldDecl("ltchtable", pfpit64); // Loop trip count histogram table.
      FieldDecl   liccntf   = new FieldDecl("licnt",     pfpit);   // Loop instruction counts.

      fields.addElement(fnamef);
      fields.addElement(hash);
      fields.addElement(numbf);
      fields.addElement(lblkf);
      fields.addElement(blkcntf);
      fields.addElement(numef);
      fields.addElement(lsrcf);
      fields.addElement(ldstf);
      fields.addElement(edgecntf);
      fields.addElement(ptblsizef);
      fields.addElement(pathnumsf);
      fields.addElement(pathcntf);
      fields.addElement(loopcntf);
      fields.addElement(lloop);
      fields.addElement(ltchtable);
      fields.addElement(liccntf);

      pfStruct = RecordType.create(fields);
    }

    String fname = cn.getName();

    // Create profiling struct in code generated for the function.

    Type vt =  RefType.create(RefType.createAligned(pfStruct, 8), RefAttr.Const);
    pfvd = new VariableDecl("__pf_t_" + fname, vt); // Member variable.
    pfvd.setVisibility(Visibility.FILE);
    pfvd.setResidency(Residency.MEMORY);
    pfvd.setReferenced();
    cg.addTopLevelDecl(pfvd);

    FixedArrayType fnamet  = FixedArrayType.create(0, fname.length(), pfct);
    StringLiteral  fnamesl = new StringLiteral(fnamet, fname + "\0");
    VariableDecl   fnamev  = new VariableDecl("__pf_fname_" + fname, fnamet, fnamesl);
    AddressLiteral fnameal = new AddressLiteral(pfpct, fnamev);

    // Count the number of blocks and edges, gather loop headers, and
    // compute the hash.  Don't count paths yet.

    Vector<LoopHeaderChord> loops = new Vector<LoopHeaderChord>();

    int   numBlocks = 0;
    int   numEdges  = 0;
    int   numLoops  = 0;

    Stack<Chord> wl = WorkArea.<Chord>getStack("addProfiling");
    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();
                
    while (!wl.empty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);

      if (c.isFirstInBasicBlock())
        numBlocks++;

      if (c.isBranch())
        numEdges += c.numOutCfgEdges();

      if (c.isLoopHeader()) {
        numEdges += 2; // For loop entry and loop iteration counts.
        numLoops++;

        // Record LoopHeaderChords if 'loops' is non-null.

        if ((loops != null) && !(c instanceof BeginChord))
          loops.add((LoopHeaderChord) c);
      }
    }

    WorkArea.<Chord>returnStack(wl);

    int hashv = (numEdges << 1) ^ numLoops; 

    fnamev.setReferenced();
    fnamev.setVisibility(Visibility.FILE);
    fnamev.setResidency(Residency.MEMORY);
    cg.addTopLevelDecl(fnamev);

    Vector<Object> init  = new Vector<Object>(16);

    init.addElement(fnameal); //  0 - Function name
    init.addElement(LiteralMap.put(hashv, pfit)); //  1 - Hash value
    init.addElement(LiteralMap.put(numBlocks, pfit)); //  2 - Number of blocks
    init.addElement(pflit0); //  3 - Block line number table
    init.addElement(pflit0); //  4 - Block count table
    init.addElement(pflit0); //  5 - Number of edges
    init.addElement(pflit0); //  6 - Edge source table
    init.addElement(pflit0); //  7 - Edge target table
    init.addElement(pflit0); //  8 - Edge count table
    init.addElement(pflit0); //  9 - Number of paths
    init.addElement(pflit0); // 10 - Path number table
    init.addElement(pflit0); // 11 - Path count table
    init.addElement(pflit0); // 12 - Number of loops
    init.addElement(pflit0); // 13 - Loop line number table
    init.addElement(pflit0); // 14 - Loop trip count table
    init.addElement(pflit0); // 15 - Loop instruction count table

    // Path profiling. We must do path profiling before edge profiling
    // because the edge profiling instrumentation adds edges which may
    // not be there when the profiling information is used.

    doPathProfiling(doPaths, init);

    // Loop profiling.

    doLoopProfiling(profileOptions, loops, init);

    // Block profiling.

    doBlockProfiling(doBlocks, numBlocks, init);

    // Edge profiling.

    doEdgeProfiling(doEdges, numEdges, init);

    AggregationElements ae = new AggregationElements(pfStruct, init);
    pfvd.setValue(ae);

    if (isMain) // Add call to profile utility function.
      addProfileDumpToMain(cg);

    // We modified the CFG, so invalidate references, data
    // dependences, and dominators.

    recomputeRefs();
    recomputeDataDependence();
    recomputeDominators();

    return pfvd;
  }

  private void doBlockProfiling(boolean doBlocks, int numBlocks, Vector<Object> init)
  {
    if (!doBlocks || (numBlocks <= 0))
      return;

    String         fname    = cn.getName();
    Stack<Chord>   wl       = WorkArea.<Chord>getStack("doBlockProfiling");
    FixedArrayType blkcntt  = FixedArrayType.create(0, numBlocks - 1, pfit);
    VariableDecl   blkcntv  = new VariableDecl("__pf_blkcnt_" + fname, blkcntt); // Array variable to hold block counts.
    int[]          blines   = new int[100];
    int            bindex   = 0;
    VariableDecl   blkcnttv = genTemp(pfpit); // Temp to hold block-count-array element address.

    blkcntv.setVisibility(Visibility.FILE);
    blkcntv.setResidency(Residency.MEMORY);
    blkcntv.setReferenced();
    cg.addTopLevelDecl(blkcntv);

    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();

    // Add instructions to do block profiling at the beginning of
    // every basic block.

    while (!wl.empty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);
      if (!c.isFirstInBasicBlock())
        continue;

      Chord ins = c;
      if (c.isSpecial()) {
        if (c.isLoopHeader())
          ins = c.getNextChord();
        else if ((c.isLoopTail()) && (c.numInCfgEdges() == 1)) {
          Chord itc = c.getInCfgEdge(0);
          if (itc instanceof IfThenElseChord)
            ins = itc; // Loop test at end.
        }
      }

      if (bindex >= blines.length) {
        int[] na = new int[bindex * 2];
        System.arraycopy(blines, 0, na, 0, bindex);
        blines = na;
      }
      int index = bindex++;
      blines[index] = findSourceLineNumber(c);

      insertIncrementCode(blkcntv, blkcnttv, index, ins, null, true);
    }

    IntArrayLiteral lblkial   = new IntArrayLiteral(blkcntt, numBlocks); // Array to hold source code line numbers.
    IntArrayLiteral blkcntial = new IntArrayLiteral(blkcntt, numBlocks); // Array to hold array block counts.
    for (int i = 0; i < numBlocks; i++) {
      lblkial.addElement(blines[i]);
      blkcntial.addElement(0);
    }

    Vector<Object> elblkial   = new Vector<Object>(1);
    Vector<Object> eblkcntial = new Vector<Object>(1);

    elblkial.addElement(lblkial);
    eblkcntial.addElement(blkcntial);

    // Create array variable to hold source line numbers.

    FixedArrayType lblkt = FixedArrayType.create(0, numBlocks - 1, pfit);
    Expression     lblkd = buildVarAndRef("__pf_lblk_" + fname, elblkial, lblkt);

    blkcntv.setInitialValue(new AggregationElements(blkcntt, eblkcntial));
    Expression blkcntd = new AddressLiteral(pfpit, blkcntv);

    init.setElementAt(lblkd, 3);
    init.setElementAt(blkcntd, 4);

    WorkArea.<Chord>returnStack(wl);
  }

  private void insertIncrementCode(VariableDecl cnt,
                                   VariableDecl x,
                                   int          index,
                                   Chord        predecessor,
                                   Chord        successor,
                                   boolean      before)
  {
    // x = &cnt[index];

    Expr      arar = new LoadDeclAddressExpr(cnt);
    Expr      arin = new LiteralExpr(LiteralMap.put(index, pfit));
    Expr      arof = new LiteralExpr(pflit0);
    Expr      arhs = new ArrayIndexExpr(pfpit, arar, arin, arof);
    Expr      alhs = new LoadDeclAddressExpr(x);
    ExprChord aec  = new ExprChord(alhs, arhs);

    // *x = *x + 1;

    Expr      srad = new LoadDeclValueExpr(x);
    Expr      srla = new LoadValueIndirectExpr(srad);
    Expr      srra = new LiteralExpr(pflit1);
    Expr      srhs = new AdditionExpr(pfit, srla, srra);
    Expr      slhs = new LoadDeclValueExpr(x);
    ExprChord sec  = new ExprChord(slhs, srhs);

    if (before) {
      predecessor.insertBeforeInCfg(aec);
      predecessor.insertBeforeInCfg(sec);
    } else {
      predecessor.insertAfterOutCfg(aec, successor);
      aec.insertAfterOutCfg(sec, successor);
    }

    aec.setVisited();
    sec.setVisited();
    aec.copySourceLine(predecessor);
    sec.copySourceLine(predecessor);
  }

  private Literal buildVarAndRef(String name, Vector<Object> v, Type type)
  {
    Literal      lit = new AggregationElements(type, v);
    VariableDecl vd  = new VariableDecl(name, type, lit);
    vd.setVisibility(Visibility.FILE);
    vd.setResidency(Residency.MEMORY);
    vd.setReferenced();
    cg.addTopLevelDecl(vd);
    return new AddressLiteral(pfpit, vd);
  }

  private void doEdgeProfiling(boolean doEdges, int numEdges, Vector<Object> init)
  {
    if (!doEdges || (numEdges <= 0))
      return;

    String          fname      = cn.getName();
    Stack<Chord>    wl         = WorkArea.<Chord>getStack("doEdgeProfiling");
    FixedArrayType  edgecntt   = FixedArrayType.create(0, numEdges - 1, pfit);
    IntArrayLiteral lsrcial    = new IntArrayLiteral(edgecntt, numEdges); // Array to hold starting line.
    IntArrayLiteral ldstial    = new IntArrayLiteral(edgecntt, numEdges); // Array to hold ending line.
    IntArrayLiteral edgecntial = new IntArrayLiteral(edgecntt, numEdges); // Array to hold edge count.
    VariableDecl    edgecnttv  = genTemp(pfpit); // Temp variable to hold array element address.
    VariableDecl    edgecntv   = new VariableDecl("__pf_edgecnt_" + fname, edgecntt); // Variable to hold edge counts.
    int             eindex     = 0;

    edgecntv.setVisibility(Visibility.FILE);
    edgecntv.setResidency(Residency.MEMORY);
    edgecntv.setReferenced();
    cg.addTopLevelDecl(edgecntv);

    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();

    // Add instructions to do edge counts.

    while (!wl.empty()) {
      Chord cc = wl.pop();
      cc.pushOutCfgEdges(wl);

      boolean doit   = cc.isBranch();
      boolean before = false;
      if (!doit && cc.isSpecial()) { // We count loop entries and iterations too.
        doit = cc.isLoopHeader();
        if (!doit && cc.isLoopPreHeader()) {
          before = true;
          doit = true;
        }
      }
      if (!doit)
        continue;

      int srcln  = findSourceLineNumber(cc.firstInBasicBlock());
      int numOut = cc.numOutCfgEdges();
      for (int i = 0; i < numOut; i++) {
        Chord c     = cc.getOutCfgEdge(i);
        int   dstln = findSourceLineNumber(c);
        lsrcial.addElement(srcln);
        ldstial.addElement(dstln);
        edgecntial.addElement(0);

        insertIncrementCode(edgecntv, edgecnttv, eindex, cc, c, before);

        eindex++;
      }
    }

    Vector<Object> elsrcial    = new Vector<Object>(1);
    Vector<Object> eldstial    = new Vector<Object>(1);
    Vector<Object> eedgecntial = new Vector<Object>(1);

    elsrcial.addElement(lsrcial);
    eldstial.addElement(ldstial);
    eedgecntial.addElement(edgecntial);

    // Create variable to hold starting source line number.

    Expression lsrcd = buildVarAndRef("__pf_lsrc_" + fname, elsrcial, edgecntt);

    // Create variable to hold destination source line number.

    Expression ldstd = buildVarAndRef("__pf_ldst_" + fname, eldstial, edgecntt);

    edgecntv.setInitialValue(new AggregationElements(edgecntt, eedgecntial));
    Expression edgecntd  = new AddressLiteral(pfpit, edgecntv);

    init.setElementAt(LiteralMap.put(numEdges, pfit), 5);
    init.setElementAt(lsrcd, 6);
    init.setElementAt(ldstd, 7);
    init.setElementAt(edgecntd, 8);

    WorkArea.<Chord>returnStack(wl);
  }

  private void doPathProfiling(boolean doPaths, Vector<Object> init)
  {
    if (!doPaths)
      return;

    // This is the first step of path profiling; it creates a
    // separate CFG representation (see scale.score.pp.PPCfg), makes
    // this CFG representation acyclic, and then computes edge
    // increment values.  Note that ppcfg will already be
    // initialized if we read in profile information.

    boolean doPgp = PPCfg.getPgp() && (ppcfg != null);

    if (doPgp) {
      ppcfg = PPCfg.getPgpCfg(ppcfg);
    } else {
      ppcfg = new PPCfg(this, null);
    }

    // The number of static paths in the routine.

    long numPaths = ppcfg.beginBlock().getNumPaths();

    // True if and only if we will use hashing to count paths.

    boolean usePathHashing = ppcfg.useHashing();

    // Get the size of the table to use; if we're not using hashing
    // or doing profile-guided profiling, this is the same as
    // numPaths.

    int pathTableSize = ppcfg.getPathTableSize();

    FixedArrayType  pathnumst = FixedArrayType.create(0, pathTableSize - 1, pfit64);
    String          fname     = cn.getName();

    // A variable that holds the address of the array of path numbers.
    VariableDecl pathnumsv = new VariableDecl("__pf_pathnums_" + fname, pathnumst);
    // A variable that holds the address of the array of path counters.
    VariableDecl pathcntv  = new VariableDecl("__pf_pathcnt_" + fname, pathnumst);

    // Make pathnumsv (the address of the array of path numbers) and
    // pathcntv (the address of the array of path counters) visible
    // and resident in memory and so forth.

    pathnumsv.setVisibility(Visibility.FILE);
    pathnumsv.setResidency(Residency.MEMORY);
    pathnumsv.setReferenced();
    cg.addTopLevelDecl(pathnumsv);
    pathcntv.setVisibility(Visibility.FILE);
    pathcntv.setResidency(Residency.MEMORY);
    pathcntv.setReferenced();
    cg.addTopLevelDecl(pathcntv);

    // Populate the arrays.

    IntArrayLiteral pathnumsial = new IntArrayLiteral(pathnumst, pathTableSize);
    IntArrayLiteral pathcntial  = new IntArrayLiteral(pathnumst, pathTableSize);

    for (int i = 0; i < pathTableSize; i++) {
      // If we're hashing, number all path number entries -1 (which
      // means "invalid");

      // If we're not hashing, number all path numbers 0 through
      // pathTableSize - 1

      int k = usePathHashing ? -1 : i;
      pathnumsial.addElement(k);

      // set all path frequencies to 0
      pathcntial.addElement(0);
    }

    Vector<Object> epathnumsial = new Vector<Object>(1);
    Vector<Object> epathcntial  = new Vector<Object>(1);

    epathnumsial.addElement(pathnumsial);
    epathcntial.addElement(pathcntial);

    pathnumsv.setInitialValue(new AggregationElements(pathnumst, epathnumsial));
    pathcntv.setInitialValue(new AggregationElements(pathnumst, epathcntial));

    Expression pathnumsd = new AddressLiteral(pfpit64, pathnumsv);
    Expression pathcntd  = new AddressLiteral(pfpit64, pathcntv);

    // Now do path profiling instrumentation.  We pass the variable
    // pfvd, which is a pointer to profile information, to the
    // function that hashes paths.

    // If we're using a hashing, create a declaration for __pf_hash_path().
    // First check if the declaration already exists.

    RoutineDecl hashRoutineDecl = null;
    if (ppcfg.useHashing()) {
      // If the declaration does not exist, create a declaration.

      if (hashFtn == null) {
        Vector<FormalDecl> formals = new Vector<FormalDecl>(2);

        formals.add(new FormalDecl("ftab", PointerType.create(VoidType.type)));
        formals.add(new FormalDecl("pathNum", pfit64));

        ProcedureType pt = ProcedureType.create(VoidType.type, formals, null);

        hashFtn = new ProcedureDecl("__pf_hash_path", pt);
        hashFtn.setVisibility(Visibility.EXTERN);
        hashFtn.setReferenced();

        cg.addTopLevelDecl(hashFtn);
      }

      hashRoutineDecl = hashFtn;
    }

    // An integer pointer that is useful for temporary operations.

    VariableDecl tempVar = genTemp(pfpit64);

    // An integer variable that is used as the path register.

    VariableDecl pathRegister = genTemp(pfit64);

    // Do path profiling instrumentation.  This makes the (PPCfg)
    // CFG cyclic again and puts all the necessary instrumentation
    // on the edges.  The final step of doInstrumentation() is
    // destructive to ppcfg.

    ppcfg.doInstrumentation(pathRegister,
                            pathcntv,
                            tempVar,
                            pfvd,
                            pfit64,
                            hashRoutineDecl);


    init.setElementAt(LiteralMap.put(pathTableSize, pfit), 9);
    init.setElementAt(pathnumsd, 10);
    init.setElementAt(pathcntd, 11);
  }

  private void doLoopProfiling(int profileOptions, Vector<LoopHeaderChord> loops, Vector<Object> init)
  {
    int     numLoops = loops.size();
    boolean doLoops  = (profileOptions & PROFILE_LOOPS) != 0;
    boolean doic     = (profileOptions & PROFILE_LICNT) != 0;
    if ((numLoops == 0) || (!doLoops && !doic))
      return;

    init.setElementAt(LiteralMap.put(numLoops, pfit), 12);

    // Create a pointer to the loop line number table.

    String         fname     = cn.getName();
    FixedArrayType lloopst   = FixedArrayType.create(0, numLoops - 1, pfit);
    VariableDecl   lloopsVar = new VariableDecl("__pf_lloops_" + fname, lloopst);
    lloopsVar.setVisibility(Visibility.FILE);
    lloopsVar.setResidency(Residency.MEMORY);
    lloopsVar.setReferenced();
    cg.addTopLevelDecl(lloopsVar);

    // Now initialize the loop trip count histogram (LTCH) table.
    // Plus other stuff I don't fully understand.

    IntArrayLiteral lloopsial = new IntArrayLiteral(lloopst, numLoops);
    for (int i = 0; i < numLoops; i++) {
      LoopHeaderChord lh = loops.get(i);
      lloopsial.setValue(lh.getLoopNumber() - 1, lh.getSourceLineNumber());
    }

    lloopsVar.setInitialValue(lloopsial);
    Expression lloopsd = new AddressLiteral(pfpit64, lloopsVar);
    init.setElementAt(lloopsd, 13);

    if (doLoops) {
      // Create a globally visible pointer to the loop trip count
      // histogram table.

      int            ub           = numLoops * LTC_TABLE_SIZE;
      FixedArrayType ltchtablet   = FixedArrayType.create(0, ub - 1, pfit64);
      VariableDecl   ltchTableVar = new VariableDecl("__pf_ltchtable_" + fname, ltchtablet);
      ltchTableVar.setVisibility(Visibility.FILE);
      ltchTableVar.setResidency(Residency.MEMORY);
      ltchTableVar.setReferenced();
      cg.addTopLevelDecl(ltchTableVar);

      // Now initialize the loop trip count histogram table.

      IntArrayLiteral ltchtableial = new IntArrayLiteral(ltchtablet, ub);
      for (int i = 0; i < ub; i++)
        ltchtableial.addElement(0);

      ltchTableVar.setInitialValue(ltchtableial);
      Expression ltchtabled = new AddressLiteral(pfpit64, ltchTableVar);
      init.setElementAt(ltchtabled, 14);


      // Now we do loop trip count profiling instrumentation.

      // Create a declaration for __pf_record_ltc().
      // If the declaration does not exist, create a declaration.

      if (ltcRDecl == null) {
        Vector<FormalDecl> ltcFormals = new Vector<FormalDecl>(3);
        Type   ty         = RefType.create(VoidType.type, RefAttr.Const);
        Type   pty        = PointerType.create(ty);

        ltcFormals.add(new FormalDecl("ftab",      pty));
        ltcFormals.add(new FormalDecl("loopNum",   pfit));
        ltcFormals.add(new FormalDecl("tripCount", pfit64));

        ProcedureType ltcProcType = ProcedureType.create(VoidType.type,
                                                         ltcFormals,
                                                         null);

        ltcRDecl = new ProcedureDecl("__pf_record_ltc", ltcProcType);
        ltcRDecl.setVisibility(Visibility.EXTERN);
        ltcRDecl.setReferenced();
        cg.addTopLevelDecl(ltcRDecl);
      }

      // Add instrumentation to each loop.

      for (int loopNum = 0; loopNum < numLoops; loopNum++) {
        // Create a variable that will be used to count the number of
        // loop iterations (i.e., the trip count).

        VariableDecl       loopIterVar   = genTemp(pfit64);
        LoopHeaderChord    loopHeader    = loops.get(loopNum);
        LoopPreHeaderChord loopPreHeader = loopHeader.getPreHeader();
        LoopTailChord      loopTail      = loopHeader.getLoopTail();

        if (loopTail == null)
          continue;

        // We need to do three things:
        // (1) Initialize the loop iteration var in the preheader.

        Expr      initLhs   = new LoadDeclAddressExpr(loopIterVar);
        Expr      initRhs   = new LiteralExpr(LiteralMap.put(0, pfit64));
        ExprChord initChord = new ExprChord(initLhs, initRhs);

        loopPreHeader.insertBeforeInCfg(initChord);
        initChord.copySourceLine(loopHeader);

        // (2) Increment the loop iteration var in the header.

        Expr      incLhs   = new LoadDeclAddressExpr(loopIterVar);
        Expr      incLa    = new LoadDeclValueExpr(loopIterVar);
        Expr      incRa    = new LiteralExpr(LiteralMap.put(1, pfit64));
        Expr      incRhs   = new AdditionExpr(pfit64, incLa, incRa);
        ExprChord incChord = new ExprChord(incLhs, incRhs);

        Chord ins = loopTail;
        if (ins.numInCfgEdges() == 1) {
          Chord itc = ins.getInCfgEdge(0);
          if (itc instanceof IfThenElseChord)
            ins = itc; // Loop test at end.
        }

        ins.insertBeforeInCfg(incChord);
        incChord.copySourceLine(loopHeader);

        // (3) Record the loop iteration count (LTC) in all loop exits.

        for (int i = 0; i < loopHeader.numLoopExits(); i++) {
          LoopExitChord loopExitChord = loopHeader.getLoopExit(i);

          // Generate the call expression chord.

          Vector<Expr> args = new Vector<Expr>(3);

          args.add(new LoadDeclAddressExpr(pfvd));
          args.add(new LiteralExpr(LiteralMap.put(loopHeader.getLoopNumber(), pfit)));
          args.add(new LoadDeclValueExpr(loopIterVar));

          Expr  ftn         = new LoadDeclAddressExpr(ltcRDecl);
          Expr  callExpr    = new CallFunctionExpr(VoidType.type, ftn, args);
          Chord recordChord = new ExprChord(callExpr);

          // Insert the new chord.

          loopExitChord.insertAfterOutCfg(recordChord, loopExitChord.getTarget());
          recordChord.copySourceLine(loopExitChord.getTarget());
        }
      }
    }

    if (doic) {
      // Create a pointer to the loop instruction count table.

      int            ub      = 4 * numLoops;
      FixedArrayType licst   = FixedArrayType.create(0, ub - 1, pfit);
      VariableDecl   licsVar = new VariableDecl("__pf_lics_" + fname, licst);
      licsVar.setVisibility(Visibility.FILE);
      licsVar.setResidency(Residency.MEMORY);
      licsVar.setReferenced();
      cg.addTopLevelDecl(licsVar);

      // Now initialize the loop instruction count table.  This will
      // be update at the end of compilation.

      IntArrayLiteral licsial = new IntArrayLiteral(licst, ub);
      for (int i = 0; i < ub; i++)
        licsial.addElement(0);

      icAr = licsial;
      licsVar.setInitialValue(licsial);
      Expression licsd = new AddressLiteral(pfpit64, licsVar);
      init.setElementAt(licsd, 15);
    }
  }

  /**
   * Add a call to <code>atexit(__pf_profile_dump)</code> at the
   * beginning of this routine.  This causes
   * <code>__pf_profile_dump</code> to be called when the program
   * exits, whether its exits by returning from main() or by calling
   * <code>exit()</code>.  This should only be called if this routine
   * is <code>main()</code>.
   * @param cg is the call graph that the routine is part of.
   */
  private void addProfileDumpToMain(CallGraph cg)
  {
    if (!cn.equals(cg.getMain()))
      CallGraph.reportProfileProblem("This method should only be called for main()");
    
    ProcedureType pft   = ProcedureType.create(VoidType.type, new Vector<FormalDecl>(0), null);
    RoutineDecl   pfrd  = new ProcedureDecl("__pf_profile_dump", pft);

    pfrd.setVisibility(Visibility.EXTERN);
    pfrd.setReferenced();
    cg.addTopLevelDecl(pfrd);

    // For C or Fortran: Call atexit(__pf_profile_dump) at the
    // beginning of main.  If the program exits normally or calls
    // exit(), __pf_profile_dump() will be called automatically.  If
    // the program calls the built-in Fortran function stop(),
    // __pf_profile_dump() will be called because Scale implements
    // stop() by calling exit().

    Vector<FormalDecl> formals = new Vector<FormalDecl>(1);

    formals.add(new FormalDecl("func", PointerType.create(pft)));

    Type          rt   = SignedIntegerType.create(32);
    ProcedureType pt   = ProcedureType.create(rt, formals, null);
    RoutineDecl   rd   = new ProcedureDecl("atexit", pt);
    Vector<Expr>  args = new Vector<Expr>(1);

    args.add(new LoadDeclAddressExpr(pfrd));

    LoadDeclAddressExpr ldae = new LoadDeclAddressExpr(rd);
    CallExpr            expr = new CallFunctionExpr(rt, ldae, args);
    ExprChord           ec   = new ExprChord(expr);

    begin.insertAfterOutCfg(ec, begin.getNextChord());
    ec.copySourceLine(begin.getNextChord());

    rd.setVisibility(Visibility.EXTERN);
    rd.setReferenced();
    cg.addTopLevelDecl(rd);
  }

  /**
   * Obtain the profile information from a previous execution of the
   * program and annotate the CFG with the information.
   *<p>
   * The user's program is first modified to produce the profiling
   * information by inserting operations into the CFG.  This insertion
   * occurs just prior to code generation.  The user requests this via
   * a command line switch.
   * <p>
   * When the program is executed it produces a <code>.pft</code> file
   * for every source module in the program.  This file contains the
   * profile information on the number of times a block, edge, or path
   * in the program was executed.
   * <p>
   * On a subsequent compilation an optimization can request that this
   * profile information be read and the CFG annotated with it.  For
   * edge profiling, the out-going CFG edges of a {@link
   * scale.score.chords.DecisionChord DecisionChord} are given a
   * probability of that edge being used.
   * <p>
   * <b>In order for this to work</b>, the CFG must be equivalent, in
   * the structure of the graph edges when the profile information is
   * read, to the CCFG when the profiling operations were inserted.
   */
  public void applyProfInfo(ProfileInfo pf, int profileOptions)
  {
    Vector<LoopHeaderChord> loops = new Vector<LoopHeaderChord>();
    int numBlocks = 0;
    int numEdges  = 0;
    int numLoops  = 0;

    Stack<Chord> wl = WorkArea.<Chord>getStack("applyProfInfo");
    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();
                
    while (!wl.empty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);

      if (c.isFirstInBasicBlock())
        numBlocks++;

      if (c.isBranch())
        numEdges += c.numOutCfgEdges();

      if (c.isLoopHeader()) {
        numEdges += 2; // For loop entry and loop iteration counts.
        numLoops++;

        // Record LoopHeaderChords if 'loops' is non-null.

        if ((loops != null) && !(c instanceof BeginChord))
          loops.add((LoopHeaderChord) c);
      }
    }

    WorkArea.<Chord>returnStack(wl);

    int hash = (numEdges << 1) ^ numLoops;
    if (hash != pf.hash) {
      Msg.reportWarning(Msg.MSG_Profile_CFG_characteristic_value_differs_for_s,
                        cg.getName(),
                        0,
                        0,
                        cn.getName());
      return;
    }

    if ((profileOptions & PROFILE_PATHS) != 0)
      applyProfPathInfo  (pf);

    if ((profileOptions & PROFILE_BLOCKS) != 0)
      applyProfBlockInfo  (pf, numBlocks);

    if ((profileOptions & PROFILE_EDGES) != 0)
      applyProfEdgeInfo  (pf, numEdges);

    if ((profileOptions & PROFILE_LOOPS) != 0)
      applyProfLoopInfo  (pf, loops);

    if ((profileOptions & PROFILE_LICNT) != 0) {
      icArray = pf.icArray;
      ucArray = pf.ucArray;
    }

    if (Debug.debug(1))
      validateCFG();
  }

  /**
   * Apply the profiling information to the function CFG edges.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param pf is the profile info that has been read in for
   * the CFG
   * @param numEdges is the number of CFG edges
   */
  private void applyProfEdgeInfo(ProfileInfo pf, int numEdges)
  {
    int[] pfeArray = pf.edgeArray;
    if (pfeArray == null)
      return;

    int          eindex   = 0;
    Stack<Chord> wl       = WorkArea.<Chord>getStack("applyProfEdgeInfo");

    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();

    while (!wl.empty()) {
      Chord cc = wl.pop();
      cc.pushOutCfgEdges(wl);

      boolean doit = cc.isBranch();
      boolean islh = false;
      if (!doit && cc.isSpecial()) { // We count loop entries and iterations too.
        islh = cc.isLoopHeader();
        doit = islh;
        if (!doit && cc.isLoopPreHeader())
          doit = true;
      }

      if (!doit)
        continue;

      int numOut = cc.numOutCfgEdges();
      if (numOut < 2) {
        if (islh && (eindex > 0)) {
          LoopHeaderChord lh = (LoopHeaderChord) cc;
          lh.setProfEntryCnt(pfeArray[eindex - 1]);
          lh.setProfIterationCnt(pfeArray[eindex + 0]);
        }
        eindex++;
        continue;
      }

      DecisionChord dc    = (DecisionChord) cc;
      double        total = 0.0;
      for (int i = 0; i < numOut; i++)
        total += Math.abs(pfeArray[eindex + i]);

      if (total == 0.0) // Avoid generating a Nan
        total = 1.0;

      for (int i = 0; i < numOut; i++) {
        Chord c     = cc.getOutCfgEdge(i);
        int   index = eindex++;
        dc.specifyBranchProbability(c, Math.abs(pfeArray[index]) / total);
      }
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Apply the profiling information to the function blocks.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param pf is the profile info that has been read in for
   * the CFG
   * @param numBlocks is the number of CFG basic blocks
   */
  private void applyProfBlockInfo(ProfileInfo pf, int numBlocks)
  {
    int[] pfbArray = pf.blockArray;
    if (pfbArray == null)
      return;

    int          eindex = 0;
    Stack<Chord> wl     = WorkArea.<Chord>getStack("applyProfEdgeInfo");

    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();

    int currentCnt = 0;
    while (!wl.empty()) {
      Chord cc = wl.pop();
      cc.pushOutCfgEdges(wl);

      if (cc.isFirstInBasicBlock())
        currentCnt = pfbArray[eindex++];

      if (cc instanceof BeginChord)
        cn.setProfCallCnt(currentCnt);

      if (cc.isLoopPreHeader())
        ((LoopPreHeaderChord) cc).getLoopHeader().setProfEntryCnt(currentCnt);

      CallExpr call = cc.getCall(false);
      if (call != null)
        call.setProfCallCnt(currentCnt);
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Apply the profiling information to the function CFG edges.
   * @param pf is the profile information that has been read
   * in for the call graph
   */
  private void applyProfPathInfo(ProfileInfo pf)
  {
    HashMap<Long, Long> pfpMap = pf.pathMap;
    if (pfpMap == null)
      return;

    // Now apply the path profiling information to the CFG.

    if (ppcfg != null)
      CallGraph.reportProfileProblem("Expected path profiling CFG to be uninitialized");

    boolean doPgp = PPCfg.getPgp() && (ppcfg != null);
    if (doPgp)
      CallGraph.reportProfileProblem("Reading PGP profile information is not supported");

    PPCfg cfg = new PPCfg(this, null);
    ppcfg = cfg;
    cfg.doAnalysis(true, false);
    cfg.setPathFreqMap(pfpMap);
  }

  /**
   * Apply loop trip count profiling information to the CFG.
   * @param pf is the profile info that has been read in for
   * the CFG
   * @param loops is the set of loops in the program
   */
  public void applyProfLoopInfo(ProfileInfo pf, Vector<LoopHeaderChord> loops)
  {
    IntMap<long[]> pflMap = pf.loopHistMap;
    if (pflMap == null)
      return;

    // Now apply the loop trip count profiling information to compute
    // an unroll factor for each loop.

    int numLoops = loops.size();

    ltcArray = new int[numLoops];

    for (int loopNum = 0; loopNum < numLoops; loopNum++) {
      LoopHeaderChord lh = loops.get(loopNum);
      int             ln = lh.getLoopNumber() - 1;

      if (lh.getUnrollFactor() > 0)
        continue; // Programmer specified it using a pragma.

      long[] hist = pflMap.get(ln);
      if (hist == null)
        continue;

      long sum  = 0;
      int  min  = -1;
      int  max  = -1;
      for (int i = 0; i < hist.length; i++) {
        long x = hist[i];
        if (x > 0) {
          if (min < 0)
            min = i;
          max = i;
          sum += x;
        }
      }

      if (min < 0)
        continue;

      long half = sum / 2;
      int  mean = -1;
      sum = 0;
      for (int i = 0; i < hist.length; i++) {
        long x = hist[i];
        sum += x;
        if (sum > half) {
          if (x == sum)
            mean = i;
          break;
        }
        mean = i;
      }

      if (mean <= 0)
        mean = min;

      int urf = 0;

      if (min >= 3)
        urf = min;
      else
        urf = mean;

      ltcArray[ln] = urf;
    }
  }

  private int findSourceLineNumber(Chord c)
  {
    int lineno = -1;
    while (c != null) {
      lineno = c.getSourceLineNumber();
      if (lineno >= 0)
        return lineno;
      c = c.getNextChord();
    }
    return lineno;
  }
  
  /**
   * Return the path profiling-specific representation of this
   * Scribble CFG.
   */
  public PPCfg getPPCfg()
  {
    return ppcfg;
  }
  
  /**
   * Return the loop body unroll count for a particular loop.  Returns
   * -1 if not found.
   */
  public int getLoopUcount(LoopHeaderChord loopHeader)
  {
    if (ucArray == null)
      return -1;

    int loopIndex = loopHeader.getLoopNumber() * 4;
    if (loopIndex >= icArray.length)
      return -1; // Loop added by unrolling, etc.

    return icArray[loopIndex + 2];
  }

  /**
   * Return the loop body instruction count for a particular loop.
   * Returns -1 if not found.
   */
  public int getLoopIcount(LoopHeaderChord loop)
  {
    if (icArray == null)
      return -1;

    int loopIndex = (loop.getLoopNumber() - 1) * 4;
    if (loopIndex >= ucArray.length)
      return -1; // Loop added by unrolling, etc.

    return ucArray[loopIndex + 0];
  }

  /**
   * Return the unroll count determined from loop histogram profiling.
   * Returns -1 if not found.
   */
  public int getLoopLtcUnrollCount(LoopHeaderChord loop)
  {
    if (ltcArray == null)
      return -1;

    int loopIndex = loop.getLoopNumber() - 1;
    if (loopIndex >= ltcArray.length)
      return -1; // Loop added by unrolling, etc.

    return ltcArray[loopIndex];
  }

  private long[] getLoopStats()
  {
    if ((pfvd == null) || (icAr == null))
      return null;

    return icAr.getArrayValue();
  }

  public void incrementLoopInstCount(int loopNumber, int instructionCount)
  {
    long[] array = getLoopStats();
    if (array == null)
      return;

    int loopIndex = (loopNumber - 1) * 4;
    if (loopIndex >= array.length)
      return; // Loop added by unrolling, etc.

    array[loopIndex + 0] += instructionCount;
  }


  public void setLoopICEst(LoopHeaderChord loop, int instCountEstimate)
  {
    long[] array = getLoopStats();
    if (array == null)
      return;

    int loopIndex = (loop.getLoopNumber() - 1) * 4;
    if (loopIndex >= array.length)
      return; // Loop added by unrolling, etc.

    array[loopIndex + 1] = instCountEstimate;
  }

  public void setLoopUC(LoopHeaderChord loop, int unrollCount)
  {
    long[] array = getLoopStats();
    if (array == null)
      return;

    int loopIndex = (loop.getLoopNumber() - 1) * 4;
    if (loopIndex >= array.length)
      return; // Loop added by unrolling, etc.

    array[loopIndex + 2] = unrollCount;
  }

  public void setLoopUCEst(LoopHeaderChord loop, int unrollCountEst)
  {
    long[] array = getLoopStats();
    if (array == null)
      return;

    int loopIndex = (loop.getLoopNumber() - 1) * 4;
    if (loopIndex >= array.length)
      return; // Loop added by unrolling, etc.

    array[loopIndex + 3] = unrollCountEst;
  }

  /**
   * Clean up for profiling statistics.
   */
  public static void cleanup()
  {
    pfStruct = null;
    pfct     = null;
    pfpct    = null;
    pfit     = null;
    pfpit    = null;
    pfit64   = null;
    pfpit64  = null;
    pflit1   = null;
    pflit0   = null;
    ltcRDecl = null;
  }
}
