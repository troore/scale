package scale.score.trans;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.dependence.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.IntLiteral;

/**
 * This class performs no optimization.
 * <p>
 * $Id: Noopt.java,v 1.13 2007-10-04 19:58:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <P>
 * This class is used to force entry into SSA form without performing
 * an optimization.  This is useful for debugging the compiler.
 */

public class Noopt extends Optimization
{
  /**
   * Set true to obtain some statistics for a study of alias analysis.
   * The statistics are obtained in two different sets.  The first set
   * is obtained the first time the 's' optimization is invoked on a
   * CFG and includes:
   * <dl>
   * <dt>numberOfCfgNodesBefore<dd>Number of CFG nodes.
   * <dt>numberOfDirectStoresBefore<dd>Number of simple assignments.
   * <dt>numberOfDuplicateStoresBefore<dd>Number of duplicate indirect
   * assignments in the same basic block.
   * <dt>numberOfIndirectStoresBefore<dd>Number of indirect assignments.
   * <dt>numberOfStoresBefore<dd>Number of assignments.
   * <dt>numberOfMayDefsBefore<dd>Number of assignments with may-def
   * information.
   * <dt>numberOfExprBefore<dd>Number of expression nodes on the
   * right-hand-side.
   * <dt>numberOfDirectMayUseBefore<dd>Number of variable loads with
   * may-use information.
   * <dt>numberOfDirectLoadsBefore<dd>Number of variable loads.
   * <dt>numberOfIndirectMayUseBefore<dd>Number of indirect loads with
   * may-use information.
   * <dt>numberOfIndirectLoadsBefore<dd>Number of indirect loads.
   * <dt>numberOfATNoMayUseBefore<dd>Number of direct loads of
   * variables whose address is taken but for which there is no may-use
   * information.
   * <dt>numberOfNoATMayUseBefore<dd>Number of direct loads of
   * variables whose address is not taken but for which there is
   * may-use information.
   * </dl>
   * The second set is obtained for all subsequent times the 's'
   * optimization is invoked on a CFG and includes:
   * <dl>
   * <dt>numberOfCfgNodesAfter<dd>Number of CFG nodes.
   * <dt>numberOfDirectStoresAfter<dd>Number of simple assignments.
   * <dt>numberOfDuplicateStoresAfter<dd>Number of duplicate indirect
   * assignments in the same basic block.
   * <dt>numberOfIndirectStoresAfter<dd>Number of indirect assignments.
   * <dt>numberOfStoresAfter<dd>Number of assignments.
   * <dt>numberOfMayDefsAfter<dd>Number of assignments with may-def information.
   * <dt>numberOfExprAfter<dd>Number of expression nodes on the right-hand-side.
   * <dt>numberOfDirectMayUseAfter<dd>Number of variable loads with may-use information.
   * <dt>numberOfDirectLoadsAfter<dd>Number of variable loads.
   * <dt>numberOfIndirectMayUseAfter<dd>Number of indirect loads with may-use information.
   * <dt>numberOfIndirectLoadsAfter<dd>Number of indirect loads.
   * <dt>numberOfATNoMayUseAfter<dd>Number of direct loads of variables
   * whose address is taken but for which there is no may-use
   * information.
   * <dt>numberOfNoATMayUse<dd>Number of direct loads of variables
   * whose address is not taken but for which there is may-use
   * information.
   * </dl>
   * To obtain these statistics use the <tt>-f
   * scale.score.trans.Noopt.obtainAliasAnalysisStats=1</tt> command
   * line switch and use the <tt>s</tt> optimization.  For example,
   * <pre>
   *   java scale.test.Scale module.c -O snsud -f scale.score.trans.Noopt.obtainAliasAnalysisStats=1
   * </pre>
   * would compile <tt>module.c</tt> and obtain the alias analysis
   * statistics for the global value numbering optimization.  Use of
   * the <tt>s</tt> optimization more than twice will result in
   * erroneous statistics.  Also, note that this optimization forces
   * the CFG into SSA form.
   */
  public static boolean obtainAliasAnalysisStats = false;

  private static final int BEFORE = 0; /* The first time for a CFG. */
  private static final int AFTER  = 1; /* All subsequent times. */

  private static int[] numberOfCfgNodesCount        = {0, 0}; // Number of CFG nodes.
  private static int[] numberOfDirectStoresCount    = {0, 0}; // Number of simple assignments.
  private static int[] numberOfDuplicateStoresCount = {0, 0}; // Number of duplicate indirect assignments in the same basic block.
  private static int[] numberOfIndirectStoresCount  = {0, 0}; // Number of indirect assignments.
  private static int[] numberOfStoresCount          = {0, 0}; // Number of assignments.
  private static int[] numberOfMayDefsCount         = {0, 0}; // Number of assignments with may-def information.
  private static int[] numberOfExprCount            = {0, 0}; // Number of expression nodes on the right-hand-side.
  private static int[] numberOfDirectMayUseCount    = {0, 0}; // Number of variable loads with may-use information.
  private static int[] numberOfDirectLoadsCount     = {0, 0}; // Number of variable loads.
  private static int[] numberOfIndirectMayUseCount  = {0, 0}; // Number of indirect loads with may-use information.
  private static int[] numberOfIndirectLoadsCount   = {0, 0}; // Number of indirect loads.
  private static int[] numberOfATNoMayUseCount      = {0, 0}; // Number of direct loads to variables whose address is taken but for which there is no may-use information.
  private static int[] numberOfNoATMayUseCount      = {0, 0}; // Number of direct loads to variables whose address is not taken but for which there is may-use information.

  private static final String[] stats = {
    "numberOfCfgNodesBefore",
    "numberOfDirectStoresBefore",
    "numberOfDuplicateStoresBefore",
    "numberOfIndirectStoresBefore",
    "numberOfStoresBefore",
    "numberOfMayDefsBefore",
    "numberOfExprBefore",
    "numberOfDirectMayUseBefore",
    "numberOfDirectLoadsBefore",
    "numberOfIndirectMayUseBefore",
    "numberOfIndirectLoadsBefore",
    "numberOfATNoMayUseBefore",
    "numberOfNoATMayUseBefore",
    "numberOfCfgNodesAfter",
    "numberOfDirectStoresAfter",
    "numberOfDuplicateStoresAfter",
    "numberOfIndirectStoresAfter",
    "numberOfStoresAfter",
    "numberOfMayDefsAfter",
    "numberOfExprAfter",
    "numberOfDirectMayUseAfter",
    "numberOfDirectLoadsAfter",
    "numberOfIndirectMayUseAfter",
    "numberOfIndirectLoadsAfter",
    "numberOfATNoMayUseAfter",
    "numberOfNoATMayUse",
  };

  static
  {
    Statistics.register("scale.score.trans.Noopt", stats);
  }

  public static int numberOfCfgNodesBefore()
  {
    return numberOfCfgNodesCount[BEFORE];
  }

  public static int numberOfDirectStoresBefore()
  {
    return numberOfDirectStoresCount[BEFORE];
  }

  public static int numberOfDuplicateStoresBefore()
  {
    return numberOfDuplicateStoresCount[BEFORE];
  }

  public static int numberOfIndirectStoresBefore()
  {
    return numberOfIndirectStoresCount[BEFORE];
  }

  public static int numberOfStoresBefore()
  {
    return numberOfStoresCount[BEFORE];
  }

  public static int numberOfMayDefsBefore()
  {
    return numberOfMayDefsCount[BEFORE];
  }

  public static int numberOfExprBefore()
  {
    return numberOfExprCount[BEFORE];
  }

  public static int numberOfDirectMayUseBefore()
  {
    return numberOfDirectMayUseCount[BEFORE];
  }

  public static int numberOfDirectLoadsBefore()
  {
    return numberOfDirectLoadsCount[BEFORE];
  }

  public static int numberOfIndirectMayUseBefore()
  {
    return numberOfIndirectMayUseCount[BEFORE];
  }

  public static int numberOfIndirectLoadsBefore()
  {
    return numberOfIndirectLoadsCount[BEFORE];
  }

  public static int numberOfATNoMayUseBefore()
  {
    return numberOfATNoMayUseCount[BEFORE];
  }

  public static int numberOfNoATMayUseBefore()
  {
    return numberOfNoATMayUseCount[BEFORE];
  }

  public static int numberOfCfgNodesAfter()
  {
    return numberOfCfgNodesCount[AFTER];
  }

  public static int numberOfDirectStoresAfter()
  {
    return numberOfDirectStoresCount[AFTER];
  }

  public static int numberOfDuplicateStoresAfter()
  {
    return numberOfDuplicateStoresCount[AFTER];
  }

  public static int numberOfIndirectStoresAfter()
  {
    return numberOfIndirectStoresCount[AFTER];
  }

  public static int numberOfStoresAfter()
  {
    return numberOfStoresCount[AFTER];
  }

  public static int numberOfMayDefsAfter()
  {
    return numberOfMayDefsCount[AFTER];
  }

  public static int numberOfExprAfter()
  {
    return numberOfExprCount[AFTER];
  }

  public static int numberOfDirectMayUseAfter()
  {
    return numberOfDirectMayUseCount[AFTER];
  }

  public static int numberOfDirectLoadsAfter()
  {
    return numberOfDirectLoadsCount[AFTER];
  }

  public static int numberOfIndirectMayUseAfter()
  {
    return numberOfIndirectMayUseCount[AFTER];
  }

  public static int numberOfIndirectLoadsAfter()
  {
    return numberOfIndirectLoadsCount[AFTER];
  }

  public static int numberOfATNoMayUseAfter()
  {
    return numberOfATNoMayUseCount[AFTER];
  }

  public static int numberOfNoATMayUseAfter()
  {
    return numberOfNoATMayUseCount[AFTER];
  }

  private static HashSet<Scribble> modules;

  public Noopt(Scribble scribble)
  {
    super(scribble, "_no");
  }

  public void perform()
  {
    if (obtainAliasAnalysisStats)
      obtainAliasAnalysisStats();
  }

  /**
   * Obtain some statistics for a study of alias analysis.
   */
  public void obtainAliasAnalysisStats()
  {
    Stack<Chord>         wl     = WorkArea.<Chord>getStack("obtainAliasAnalysisStats");
    Vector<Expr>         v      = new Vector<Expr>(20);
    HashSet<Declaration> exists = WorkArea.<Declaration>getSet("obtainAliasAnalysisStats");

    int ba = AFTER;
    if (modules == null)
      modules = new HashSet<Scribble>(23);

    if (modules.add(scribble))
      ba = BEFORE;

    Chord.nextVisit();
    wl.push(scribble.getBegin());

    while (!wl.empty()) {
      Chord c = wl.pop();

      numberOfCfgNodesCount[ba]++;
      c.pushOutCfgEdges(wl);

      Expr rhs = null;
      if (c instanceof ExprChord) {
	ExprChord se  = (ExprChord) c;
	Expr      lhs = se.getLValue();
	rhs = se.getRValue();
	if (lhs instanceof LoadDeclAddressExpr)
	  numberOfDirectStoresCount[ba]++;
	else if (lhs instanceof LoadDeclValueExpr) {
	  LoadDeclValueExpr ldve = (LoadDeclValueExpr) lhs;
	  Declaration       decl = ldve.getDecl();
	  if (!exists.add(decl))
	    numberOfDuplicateStoresCount[ba]++;
	  numberOfIndirectStoresCount[ba]++;
	}
	numberOfStoresCount[ba]++;
	if (se.getMayDef() != null)
	  numberOfMayDefsCount[ba]++;
      }

      v.clear();
      if (rhs != null)
        rhs.getExprList(v);
      int l = v.size();
      for (int i = 0;i < l; i++) {
	Expr exp = v.elementAt(i);
	numberOfExprCount[ba]++;
	if (exp instanceof LoadDeclValueExpr) {
	  LoadDeclValueExpr ldve = (LoadDeclValueExpr) exp;
	  Declaration       decl = ldve.getDecl();

	  if (decl.addressTaken()) {
	    if (ldve.getMayUse() == null)
	      numberOfATNoMayUseCount[ba]++;
	  } else if (ldve.getMayUse() != null)
	    numberOfNoATMayUseCount[ba]++;

	  if (ldve.getMayUse() != null)
	    numberOfDirectMayUseCount[ba]++;
	  numberOfDirectLoadsCount[ba]++;
	} else if (exp instanceof LoadValueIndirectExpr) {
	  LoadValueIndirectExpr lvie = (LoadValueIndirectExpr) exp;
	  if (lvie.getMayUse() != null)
	    numberOfIndirectMayUseCount[ba]++;
	  numberOfIndirectLoadsCount[ba]++;
	}
      }

      if (c.isLastInBasicBlock())
	exists.clear();
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Declaration>returnSet(exists);
  }

  /**
   * Clean up for profiling statistics.
   */
  public static void cleanup()
  {
    modules = null;
  }
}
