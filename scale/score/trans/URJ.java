package scale.score.trans;

import java.util.Map;
import java.util.Iterator;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.pp.PPCfg;
import scale.score.chords.*;
import scale.score.dependence.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.TypeConversionOp;
 
import scale.backend.ICEstimator;

/**
 * This class performs unroll &amp; jam.
 * <p>
 * $Id: URJ.java,v 1.123 2007-10-29 13:42:27 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <p>
 * For each loop, we first try to flatten it.  If that fails, we next
 * try to unroll it as a for-loop.  If that fails, we try to unroll it
 * as a while-loop.  If that fails, we give up.  The following is the
 * decision tree for this process.  By flattening, we mean unrolling
 * the loop by its known iteration count, removing the exit test,
 * and removing the back edge of the loop.
 * <p>
 * For target architectures that have instruction blocks of fixed size
 * (e.g., Trips), an attempt is made to unroll or flatten so that the
 * blocks are filled.
 */

public class URJ extends Optimization
{
  /**
   * Do back propagation of induction variables after unrolling.
   */
  public static boolean doPropagation = true;
  /**
   * The maximum size, in CFG nodes, of an unrolled or flattened loop.
   * Default is 256.
   */
  public static int maxLoopCFGNodes = 256;
  /**
   * The maximum size, in "blocks", of an unrolled or flattened loop
   * when using instruction estimates.  Default is 4.
   */
  public static int maxLoopBlocks = 4;
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * If true, use heuristics that prune the cases where the
   * optimization is applied.
   */
  public static boolean useHeuristics = true;
  /**
   * If true, use estimated instruction counts instead of node counts.
   * Instruction count estimates are not accurate for all target
   * architectures.  Instead of using <code>maxLoopCFGNodes</code>, the
   * value of <code>maxLoopBlocks</code>&times;{@link
   * scale.common.Machine#getMaxBlockSize Machine.getMaxBlockSize()}
   * is used determine the maximum loop size.
   */
  public static boolean useInstCountEstimates = false;
  /**
   * True if loop peeling is to be performed.
   */
  public static boolean peel = false;
  /**
   * Controls the number of times a loop is unrolled.  This is the
   * default number of times to unroll a loop when the loop iteration
   * count is unknown.  This value must be greater than 1.
   */
  public static int defaultMinUnrollFactor = 3;
    /**
   * If true, inhibits while-loop unrolling.
   */
  public static boolean inhibitWhileLoops = false;
  /**
   * If true, inhibits for-loop unrolling.
   */
  public static boolean inhibitForLoops = false;
  /**
   * If true, inhibits loop flattening.
   */
  public static boolean inhibitFlattenLoops = false;
  /**
   * If path profiling is used, this is the minimum execution ratio
   * that a loop header needs in order for its loop to be unrolled.
   * The execution ratio is the ratio of the execution frequency of a
   * loop header to the flow (using the unit metric) of the entire
   * program.
   */ 
  public static double minLoopHeaderExecRatio = 0.0001; // 0.01%
  /**
   * If path profiling is used and the exact loop iteration is
   * <b>not</b> known, this is the minimum allowed trip count of the
   * <b>unrolled</b> loop.
   */
  public static int minNewAvgTripCount = 2; // Too low?  Too high?

  private static final int UNROLL_ERROR      = 0;
  private static final int NOT_UNROLLABLE    = 1;
  private static final int TOO_BIG_TO_UNROLL = 2;
  private static final int FLATTENED         = 3;
  private static final int FOR_UNROLLED      = 4;
  private static final int WHILE_UNROLLED    = 5;
  private static final int NO_LOOP_TAIL      = 6;

  private static final String[] unrollStatus = {
    "error",    "not unrollable", "too big", "flattened",
    "for loop", "while loop",     "no loop tail"};

  // Values indicating what kind of loop flattening/unrolling to try next.

  private static final int tryFlattening      = 0;
  private static final int tryForUnrolling    = 1;
  private static final int tryWhileUnrolling  = 2;
  private static final int finishedProcessing = 3;
  private static final int stopNow            = 4;
  private static final int stopNowTooBig      = 5;

  private static final String[] actions = {
    "tryFlattening",      "tryForUnrolling", "tryWhileUnrolling",
    "finishedProcessing", "stopNow",         "stopNowTooBig"};

  private static int unrollForCount   = 0; // A count of the for loops unrolled.
  private static int unrollWhileCount = 0; // A count of the while loops unrolled.
  private static int flattenedCount   = 0; // A count of the loops completely unrolled.
  private static int newCFGNodeCount  = 0; // A count of new nodes created.

  private static final String[] stats = {
    "unrolled",
    "unrolledWhile",
    "flattened",
    "newCFGNodes"};

  static
  {
    Statistics.register("scale.score.trans.URJ", stats);
  }

  /**
   * Return the number of for loops that were unrolled.
   */
  public static int unrolled()
  {
    return unrollForCount;
  }

  /**
   * Return the number of while loops that were unrolled.
   */
  public static int unrolledWhile()
  {
    return unrollWhileCount;
  }

  /**
   * Return the number of loops that were flattened.
   */
  public static int flattened()
  {
    return flattenedCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * Estimates how big loop bodies are (in instructions), and thus how
   * many times they should be unrolled.
   */
  private scale.backend.ICEstimator ice;

  private SSA          ssa;
  private InductionVar ivar;      // The known induction variable or null.

  /**
   * How big we should try to make our loops for an architecture
   * with instruction blocks.  Aim for a totally full block because
   * the code always(?) shrinks after the measurement is made during
   * block splitting.
   */
  private int  unrollICLimit;
  private int  unrollCount;       // The number of times to unroll.
  private int  instCountEstimate; // The estimated number of instructions in the loop.
  private long knownItCnt;        // The known iteration count or -1.

  private boolean pathDebug;      // True if path debugging desired.

  public URJ(Scribble scribble)
  {
    super(scribble, "_ur");
    
    this.unrollICLimit  = maxLoopBlocks * Machine.currentMachine.getMaxBlockSize();

    assert setTrace(classTrace);
  }

  public void perform()
  {
    pathDebug = PPCfg.getDebuggingOutput();

    this.ssa = scribble.getSSA();

    // Get the loops that we may want to unroll.

    Vector<LoopHeaderChord> loops = new Vector<LoopHeaderChord>();
    scribble.getLoopTree().getInnermostLoops(loops);
    int nLoops = loops.size();

    // Initialize the array which will hold the measured loop ICs for the
    // static profile.

    assert assertTrace(trace, "** URJ      ", scribble.getRoutineDecl().getName());
    assert assertTrace(trace, "   numLoops ", nLoops);

    // Process the innermost loops. The logic is roughly:

    // Legend:
    // - 'q' means "goto q", ie. "give up trying to unroll or flatten"
    // - 'f' means "goto f", ie. "try for-unrolling instead"
    // - 'w' means "goto w", ie. "try while-unrolling instead"
    // 
    // start: try flatten (14 tests, 5q, 5w, 4f)
    // 1)  q if numLoopExits != 1                       [asserted in for, while]
    // 2)  q if numChordsInLoop > maxLoopCFGNodes           [asserted in for, while]
    // 3)  q if [loop contains call and unrolling it is bad for this machine]
    // 4)  q if fibb.isPhiExpr()                        [asserted in for, while]
    // 5)  w if ivar == null                            [asserted in for]
    // 6)  w if numPrimaryInductionVariables != 1       [asserted in for]
    // 7)  w if getTermExpr == null || !isMatchExpr()   [asserted in for]
    // 8)  w if findExitTest(test) == null              [asserted in for]
    // 9)  f if unrollCnt < 0 (ie. unknown)
    // 10) q if unrollCnt == 0
    // 11) f if [loop too big to flatten]
    // 12) f if [loop exit test not the sole determinant of the loop exit]
    // 13) f if [big loop fails to find what it is looking for...(?)]
    // 14) w if [neither edge of the loop-exit-test is first in the BB]
    // flatten, q
    // 
    // f: try to for-unroll (23 tests, 7q, 16w)
    // 1) w if [we want predicate-based unrolling]
    // 2) w if [core type is a non-integer]
    // 3) w if (ivar.getStepValue == 0)
    // 4) w if (ivar.getInitExpr() == null)
    // 5) w if (tlec!=le and flec!=le)
    // 6) q if unrollCnt <= 1
    // 7) q if [!exact, !lte, and !cur.isAssignChord in big loop]
    // 8) w if [!exact, !lte, and lv.isRenamed() in big loop]
    // 9) w if [!exact, !lte, and big loop always continues...]
    // 10)w if [se.getLoopHeader().isSubloop() for any load expr]
    // 11)w if (ini = null)
    // 12)w if [incrementing and a signed indvar...?]
    // 13)w if [something else to do with load exprs, and isSubloop()]
    // 14)w if [not GE/GT/LE/LT for the pre-loop test...?]
    // for-unroll, q
    // 
    // w: try to while-unroll (4 tests, 4q)
    // 1) q if (!Machine.HAS_WHILE_LOOP_UNROLL_BENEFIT)
    // 2) q if (lh == BeginChord)
    // 3) q if getLoopTail() == null
    // 4) q if unrollCnt <= 1
    // while-unroll, q
    // 
    // q: done

    for (int i = 0; i < nLoops; i++) {
      LoopHeaderChord lh         = loops.elementAt(i);

      assert assertTrace(trace, "** loop# ", lh.getLoopNumber());
      assert assertTrace(trace, "   lh    ", lh);
      assert assertTrace(trace, "   exits ", lh.numLoopExits());

      if (lh instanceof BeginChord)
        continue;

      // Give up if there are multiple LoopExitChords in the loop.

      if (lh.numLoopExits() != 1) {
        assert assertTrace(trace, "  loop exits!", null);
        continue;
      }

      LoopTailChord lt = lh.getLoopTail();
      if (lt == null) { // No loop.
        assert assertTrace(trace, "  no loop!", null);
        continue;
      }

      // Give up if loop contains a function call and the architecture
      // doesn't benefit from flattening/unrolling such loops.  (For
      // TRIPS, we may revisit this later with a more subtle policy, eg.
      // allow it if we know the call is on a cold path and it can be
      // put in a separate hyperblock from the hot path of the loop.)

      if (// Machine.currentMachine.hasCapability(Machine.HAS_EXPENSIVE_SUBROUTINE_CALLS) &&
          lh.loopContainsCall()) {
        assert assertTrace(trace, "  function call!", null);
        continue;
      }

      // Give up if multiple incoming CFG edges to a LoopExitChord.

      LoopExitChord le   = lh.getFirstExit();
      Chord         fibb = le.firstInBasicBlock();
      if (le.numInCfgEdges() > 1) {
        assert assertTrace(trace, "  multiple in edges!", null);
        continue;
      }

      if (fibb.isPhiExpr()) {
        assert assertTrace(trace, "  in edges!", null);
        continue;
      }

      instCountEstimate = useInstCountEstimates ? estimateLoopIC(lh) : -1;
      scribble.setLoopICEst(lh, instCountEstimate);

      ivar       = lh.getPrimaryInductionVar();
      knownItCnt = (ivar == null) ? -1 : ivar.getIterationCount();

      assert assertTrace(trace, "   ivar  ", ivar);
      assert assertTrace(trace, "   itcnt ", knownItCnt);

      unrollCount = determineUnrollCnt(lh);
      scribble.setLoopUC(lh, unrollCount);

      assert assertTrace(trace, "   urc   ", unrollCount);

      if (unrollCount < 0)
        continue;

      int unrollKind = UNROLL_ERROR;
      int action     = tryFlattening;

      while (action != finishedProcessing) {
        assert assertTrace(trace, "   action ", actions[action]);

        switch (action) {
        case tryFlattening:
          action = flatten(lh, i);
          unrollKind = FLATTENED;
          break;
        case tryForUnrolling:
          action = unrollFor(lh, i);
          unrollKind = FOR_UNROLLED;
          break;
        case tryWhileUnrolling:
          action = unrollWhile(lh, i); 
          unrollKind = WHILE_UNROLLED;
          break;
        case stopNow:
          unrollKind = NOT_UNROLLABLE;
          action = finishedProcessing;
          break;
        case stopNowTooBig:
          unrollKind = TOO_BIG_TO_UNROLL;
          action = finishedProcessing;
          break;
        default:
          throw new scale.common.InternalError("Unknown action: " +
                                               actions[action]);
        }
      }

      assert assertTrace(trace, "   status ", unrollStatus[unrollKind]);
    }

    if (dChanged) {
      scribble.recomputeDominators();
      scribble.recomputeRefs();
    }
  }

  /**
   * Decide how many times to unroll a loop.
   * @returns the unroll count to use or -1 if the loop should not be
   * unrolled
   */
  private int calcUnrollCnt(LoopHeaderChord lh)
  {
    // Use the unroll factor from the command line, if present. If
    // it's greater than the known trip count, use the known trip count.

    int requestedUnrollFactor = lh.getUnrollFactor();
    if (requestedUnrollFactor > 0)
        return requestedUnrollFactor;

    // If profiling shows that the loop was not executed, do not
    // unroll it.

    int eCnt = lh.getProfEntryCnt();
    if (eCnt == 0)
      return -1;

    int ltcUnrollFactor = scribble.getLoopLtcUnrollCount(lh);
    if (ltcUnrollFactor == 0)
      return -1;

    PPCfg ppcfg = lh.getScribble().getPPCfg();
    if (ppcfg != null) {
      // Convert the graph to a cyclic one if it is not already.

      if (!ppcfg.isCyclic())
        ppcfg.makeCyclicPreservingEdgeProfile();

      long loopHeaderFreq = ppcfg.getBlockFreq(lh);
      if (loopHeaderFreq <= 0)
        return -1;
    }

    // If the loop iteration count is known, use it.

    if (knownItCnt >= 0)
      return (int) knownItCnt;

    // Use the unroll factor as determined by loop histogram
    // profiling, if present, unless it's greater than the (known)
    // trip count, in which case we can't unroll at all.

    if (ltcUnrollFactor > 0)
      return ltcUnrollFactor;

    // If static profile info is present, use it.

    int staticIC = lh.getScribble().getLoopIcount(lh);
    if (staticIC >= 0) {
      int staticUC = lh.getScribble().getLoopUcount(lh);
      if (staticUC > 0) {

        // Just guess that the constIC is 4 (an increment, a test,
        // one true branch, one false branch)... not very accurate.

        int estConstIC = 4;
        if (staticIC < estConstIC)
          estConstIC = 0;  // reduce if tiny loop

        // Adding (staticUC - 1) ensures that we are conservative
        // and round up any fractions rather than rounding them
        // down.

        int estPerBodyIC = (staticIC - estConstIC + (staticUC - 1)) / staticUC;

        // Avoid a possible divide-by-zero caused by an empty loop body.

        if (0 == estPerBodyIC)
          estPerBodyIC = 1;

        // Using our loop body estimate/measurement, pick an unroll factor
        // that will hopefully fill the block up nicely.
      
        int unrollCount = (unrollICLimit - estConstIC) / estPerBodyIC;
        if (unrollCount < 1) 
          unrollCount = 1;

        // Perhaps unroll to fit 3 loop bodies into two blocks
        //      if (1 == unrollCount) {
        //        if (3*estPerBodyIC + estConstIC < 2*unrollICLimit)
        //          unrollCount = 3;
        //      }

        assert assertTrace(trace, "  Considering static IC profiling", null);
        assert assertTrace(trace, "  estPerBodyIC      ", estPerBodyIC);
        assert assertTrace(trace, "  estConstIC        ", estConstIC);
        assert assertTrace(trace, "  unrollICLimit     ", unrollICLimit);
        assert assertTrace(trace, "  unroll Count      ", unrollCount);
        assert assertTrace(trace, "  estPerBodyIC      ", estPerBodyIC);
        assert assertTrace(trace, "  estConstIC        ", estConstIC);

        return unrollCount;
      }
    }

    // If path profile info is present, use it.

    if (ppcfg != null) {
      double loopHeaderFreq      = (double) ppcfg.getBlockFreq(lh);
      double loopHeaderExecRatio = loopHeaderFreq / PPCfg.getTotalProgFlow();
      double avgLoopTripCount    = ppcfg.getAvgTripCount(lh);

      assert assertTrace(pathDebug, "   Considering path profiling", null);
      assert assertTrace(pathDebug, "     Loop hotness   ", loopHeaderFreq);
      assert assertTrace(pathDebug, "     Ratio          ", loopHeaderExecRatio);
      assert assertTrace(pathDebug, "     Avg trip count ", avgLoopTripCount);

      if (loopHeaderExecRatio < minLoopHeaderExecRatio) {
        // Don't bother if the loop executes relatively infrequently.
        assert assertTrace(pathDebug, "   Loop is not hot enough!", null);
        return -1;
      }

      // Subject the unroll factor to several constraints.

      return (int) (avgLoopTripCount / minNewAvgTripCount); // Avg. LTC of new loop must be >= 2
    }

    // If edge profiling information is available, use it;

    int iCnt = lh.getProfIterationCnt();
    if (iCnt > 0)
      return iCnt / eCnt;

    // If the estimated loop instruction count is known, use it.

    if (instCountEstimate > 0)
      return ((maxLoopBlocks * Machine.currentMachine.getMaxBlockSize()) /
              instCountEstimate);

    // Unknown trip count, no profiling.  Use the default.

    return defaultMinUnrollFactor;
  }

  private int determineUnrollCnt(LoopHeaderChord lh)
  {
    int unrollCount = calcUnrollCnt(lh);

    // If the unroll count is greater than the known iteration count,
    // which should not happen, use the known iteration count.

    if ((unrollCount > knownItCnt) && (knownItCnt > 0))
      unrollCount = (int) knownItCnt;

    if (unrollCount < 2)
      return unrollCount;

    if (!useHeuristics && (knownItCnt < 0))
      return unrollCount;

    long max  = 0;
    long mult = 0;

    if (instCountEstimate > 0) { // Use instruction estimates.
      max  = unrollICLimit;
      mult = instCountEstimate;
    } else { // Use CFG node counts.
      max  = maxLoopCFGNodes;
      mult = lh.numChordsInLoop();
    }

    long total = (mult * unrollCount);

    if (total > max) // Unrolled loop would be too big.
      unrollCount = (int) (max / mult);

    if (knownItCnt > 0) { // Loop has a known trip count.
      // If there are any values in the range [unrollCount,
      // defaultMinUnrollFactor], that evenly divide the loop
      // iteration count, we use the highest such value.  This allows
      // us to avoid having a cleanup loop for left-over iterations.
      // We let Sparse Condition Constant propagation (SCC) actually
      // eliminate the if-then-else nodes and the other dead nodes
      // (i.e., the original loop).

      for (int i = unrollCount; i >= defaultMinUnrollFactor; i--) {
        if ((knownItCnt % i) == 0) {
          unrollCount = i;
          break;
        }
      }
    }
 
    if (unrollCount == 0)
      return -1;

    if ((unrollCount < defaultMinUnrollFactor) && (knownItCnt <= 0))
      return 0; // Too big to unroll.

    return unrollCount;
  }

  /**
   * Try to unroll the loop as a for-loop.  If that fails, in some
   * cases we then try to unroll as a while-loop.
   */
  private int unrollFor(LoopHeaderChord lh, int loopN)
  {
    if (inhibitForLoops)
      return tryWhileUnrolling;

    assert assertTrace(trace, "   for   ", null);

    // Should be only one LoopExitChord in the loop by now.

    assert (lh.numLoopExits() == 1) : "should be one exit by now";

    // Should have an induction variable by now.

    if (ivar == null)
      return tryWhileUnrolling;

    assert assertTrace(trace, "   ivar ", ivar);

    // Should have exactly one primary induction variable by now.
    // (Nb: in theory, for loops can be unrolled if they have multiple
    // induction variables, but I got a null pointer exception when I
    // tried it.)

    assert (lh.numPrimaryInductionVars() == 1) :
      "should have just one p.i.v. by now";

      // Can't multiply anything but integers.

    VariableDecl ivd = ivar.getVar();
    if (ivd.isNotSSACandidate())
      return stopNow;

    Type ity = ivd.getCoreType();
    if (!ity.isIntegerType() && !ity.isPointerType())
      return tryWhileUnrolling;

    // Can only handle increments and decrements of the induction
    // variable.

    long step = ivar.getStepValue();
    assert assertTrace(trace, "   step ", step);

    if (step == 0)
      return tryWhileUnrolling;

    boolean isNeg = (step < 0);
    if (isNeg)
      step = - step;

    // We need an initial value for the induction variable.

    Expr init = ivar.getInitExpr();
    assert assertTrace(trace, "   init ", init);

    if (init == null)
      return tryWhileUnrolling;

    // Should know we don't have complex forms by now.

    Expr test = ivar.getTermExpr();

    assert ((test != null) && test.isMatchExpr()) : "for loop: unexpected complex form";

    // Loop should have a loop exit test by now.

    MatchExpr       mex = (MatchExpr) test;
    IfThenElseChord ltc = findExitTest(mex);
    boolean         lte = isLoopTestAtEnd(ltc);

    assert (ltc != null) : "For loop should have a loop exit test by now";

    assert assertTrace(trace, "   mex  ", mex);
    assert assertTrace(trace, "   ltc  " + (lte ? "lte " : " "), ltc);

    // Should not be multiple incoming CFG edges to a LoopExitChord by
    // now.

    LoopExitChord le   = lh.getFirstExit();
    Chord         fibb = le.firstInBasicBlock();

    assert (!fibb.isPhiExpr()) : "for loop should pass isPhiExpr test by now";

    // Fall back on while loop unrolling if we can't find the loop exit.

    LoopExitChord tlec    = ltc.getTrueCfgEdge().findLoopExit(lh);
    boolean       useTrue = false;
    if (tlec == le)
      useTrue = true;
    else {
      LoopExitChord flec = ltc.getFalseCfgEdge().findLoopExit(lh);
      if (flec != le)
        return tryWhileUnrolling;
    }

    // Determine the unroll count.  Give up if the result would be too
    // big?

    if (unrollCount <= 1)
      return stopNowTooBig;

    boolean exact = (knownItCnt > 0) && ((knownItCnt % unrollCount) == 0);

    assert assertTrace(trace, "   exact ", exact);

    if (!exact && !lte) {
      // We can allow only phi functions between the loop header and
      // the loop exit test, when the loop test is at the beginning
      // and the loop will not be unrolled exactly, because the
      // unrolled loop exit test branches back to the original loop
      // header.  Thus, any defs between the loop header and the loop
      // exit test will be done one extra time.  For example
      //   while(--n) body;
      // is a problem while
      //   while (n > 0) {body; --n; }
      // is not. (Note - these two loops are not equivalent.)  Note
      // for future work: if the unrolled loop's exit test were always
      // placed at the end of the loop, this problem would not occur.

      for (Chord cur = lh.getNextChord(); cur != null; cur = cur.getNextChord()) {
        if (cur == ltc)
          break;

        if (cur instanceof PhiExprChord)
          continue;

        if (!cur.isAssignChord())
          return stopNow;  // Can't allow a function call because it will be called too many times.

        ExprChord se  = (ExprChord) cur;
        Expr      lhs = se.getLValue();
        if (lhs instanceof LoadDeclAddressExpr) {
          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
          VariableDecl        lv   = (VariableDecl) ldae.getDecl();
          if (lv.isRenamed()) // Can't allow any induction variable.
            return tryWhileUnrolling;

          if (lv.isTemporary() && mex.dependsOnDeclaration(lv))
            continue; // Only a temporary variable can be defined between the loop header and the loop exit test.
        }

        return tryWhileUnrolling;
      }
    }

    // Make sure that the loop exit test is not dependent on anything
    // in the loop other than the induction variable.

    Vector<LoadExpr> loadExprs = new Vector<LoadExpr>();
    mex.getLoadExprList(loadExprs);
    int lel = loadExprs.size();
    for (int i = 0; i < lel; i++) {
      LoadExpr loadExpr = loadExprs.elementAt(i);
      if (loadExpr instanceof LoadDeclAddressExpr)
        continue;

      VariableDecl vd = (VariableDecl) loadExpr.getDecl();
      if (vd.getOriginal() == ivd)
        continue;

      ExprChord se = loadExpr.getUseDef();
      if (se == null)
        continue;

      if (se.getLoopHeader().isSubloop(lh))
        return tryWhileUnrolling;
    }

    // Create the test for skipping the unrolled loop.
    // Create the test for exiting the unrolled loop.

    CompareMode mop = mex.getMatchOp();
    Expr        la  = mex.getLeftArg();
    Expr        ra  = mex.getRightArg();
    Expr        val = null;
    Expr        ini = null;

    // Find which side of the test references the induction variable.

    if (validUse(ivd, la)) {
      val = ra;
      ini = la;
    } else if (validUse(ivd, ra)) {
      val = la;
      ini = ra;
      mop = mop.argswap();
    }

    if (ini == null)
      return tryWhileUnrolling;

    if (!useTrue)
      mop = mop.reverse();

    boolean doPeeling = !exact & peel;

    // Calculate the value of the step times the unroll factor.

    Type it  = ivar.getStepExpr().getCoreType();
    Type vt  = val.getCoreType();
    Expr mu  = new LiteralExpr(LiteralMap.put((unrollCount - 1) * step, it));
    Expr umu = null;
    if (doPeeling) {
      umu = mu;
      mu = new LiteralExpr(LiteralMap.put(unrollCount * step, it));
    } else
      umu = mu.copy();

    // The initial value must be "less" than this value in order to
    // use the unrolled loop.

    boolean chkDiff = false;
    Expr    cv      = null;
    Expr    cvu     = null;

    if (isNeg) {
      cv  = AdditionExpr.create(val.getCoreType(), val.copy(), mu);
      cvu = AdditionExpr.create(val.getCoreType(), val.copy(), umu);
    } else {

      // If the induction variable is unsigned, then the subtract we need may
      // result in a non-valid unsigned value.

      chkDiff = !vt.isSigned();
      if (chkDiff && val.isLiteralExpr() && mu.isLiteralExpr()) {
        Object vlit = ((LiteralExpr) val).getLiteral().getConstantValue();
        Object mlit = ((LiteralExpr) mu).getLiteral().getConstantValue();
        if ((vlit instanceof IntLiteral) && (mlit instanceof IntLiteral)) {
          if (((IntLiteral) vlit).getLongValue() <= ((IntLiteral) mlit).getLongValue())
            // No point in unrolling this loop - we already know we can't do any unrolled iterations.
            return tryWhileUnrolling;

          chkDiff = false;
        }
      }

      cv  = SubtractionExpr.create(vt, val.copy(), mu);
      cvu = SubtractionExpr.create(vt, val.copy(), umu);
    }

    assert assertTrace(trace, "   cv   ", cv);

    // Check to see that the exit test is simple enough that only the
    // CFG node for the loop exit test needs to be copied for the "use
    // unrolled loop" test.  If more than one node needs to be copied,
    // we can't be bothered.

    Vector<LoadExpr> loads = new Vector<LoadExpr>();
    cv.getLoadExprList(loads);
    int lloads = loads.size();
    for (int i = 0; i < lloads; i++) {
      LoadExpr  ld = loads.elementAt(i);
      ExprChord ud = ld.getUseDef();

      if (ud == null)
        continue;

      if (ud.getLoopHeader().isSubloop(lh)) {
        cv.unlinkExpression();
        return tryWhileUnrolling;
      }
    }

    // Create the test used to determine if the unrolled loop should be used.

    Expr ichk = null;
    Expr uchk = null;
    Type mt   = mex.getType();
    switch (mop) {
    case LT:
      ichk = new LessExpr(mt, ini.copy(), cv);
      if (!useTrue)
        uchk = new GreaterEqualExpr(mt, ini.copy(), cvu);
      else
        uchk = new LessExpr(mt, ini.copy(), cvu);
      break;
    case LE:
      ichk = new LessEqualExpr(mt, ini.copy(), cv);
      if (!useTrue)
        uchk = new GreaterExpr(mt, ini.copy(), cvu);
      else
        uchk = new LessEqualExpr(mt, ini.copy(), cvu);
      break;
    case GT:
      ichk = new GreaterExpr(mt, ini.copy(), cv);
      if (!useTrue)
        uchk = new LessEqualExpr(mt, ini.copy(), cvu);
      else
        uchk = new GreaterExpr(mt, ini.copy(), cvu);
      break;
    case GE:
      ichk = new GreaterEqualExpr(mt, ini.copy(), cv);
      if (!useTrue)
        uchk = new LessExpr(mt, ini.copy(), cvu);
      else
        uchk = new GreaterEqualExpr(mt, ini.copy(), cvu);
      break;
    default: // The loop exit test is not in understandable form.
      cv.unlinkExpression();    // not understandable
      cvu.unlinkExpression();    // not understandable
      return tryWhileUnrolling;
    }

    // At this point we know we can unroll it as a for-loop.
    // For a loop-test-at-beginning loop, convert
    // <pre>
    //   for (int i = 0; i < n; i += s) {
    //     body;
    //   }
    // </pre>
    // to
    // <pre>
    //   int i_0 = i;
    //   if (i_0 = 0 < (n - s * uc)) { // Use unrolled loop.
    //     for (i_0 = 0; i_0 < n; i_0 += s * uc) {
    //        body;
    //        body;
    //        body;
    //     }
    //   }
    //   for (; i_0 < n; i_0 += s) {
    //      body;
    //   }
    // </pre>
    //
    // By the time this method is called, we know that we can do so.
    //
    // If the loop test is at the end of the loop, the resultant
    // code can not be converted back into structured C code.
    // But, since we really want to generate assembly code,
    // who cares?

    assert assertTrace(trace, "   for unrolling loop #", loopN);
    
    if (exact) {
      ichk.unlinkExpression();
      ichk = new LiteralExpr(LiteralMap.put(0, it));
    }

    assert assertTrace(trace, "   ichk ", ichk);
    assert assertTrace(trace, "   uchk ", uchk);
    assert assertTrace(trace, "   *****", null);

    // In the "use loop unroll test", replace the version of the
    // induction variable used in the loop with the one set just
    // before the loop.

    LoopPreHeaderChord lph = lh.getPreHeader();
    ExprChord          fse = findVarDef(lph, ivd);
    Declaration        fvd = ((LoadDeclAddressExpr) fse.getLValue()).getDecl();

    Vector<LoadExpr> lds = new Vector<LoadExpr>();
    ichk.getLoadExprList(lds);
    int llds = lds.size();
    for (int i = 0; i < llds; i++) {
      LoadExpr     ld  = lds.elementAt(i);
      VariableDecl vdx = (VariableDecl) ld.getDecl();
      if (vdx.getOriginal() == ivd.getOriginal()) {
        ld.setDecl(fvd);
        ld.setUseDef(fse);
      }
    }

    if (chkDiff) {
      // If the induction variable is unsigned, we can't depend on the
      // subtract above to give a valid value for the test.

      int     ci  = useTrue ? 0 : 1;
      Literal lit = LiteralMap.put(ci, it);
      Expr    cmp = new GreaterEqualExpr(mt, val.copy(), mu.copy());
      uchk = ConditionalExpr.create(it, cmp,        uchk, new LiteralExpr(lit));
      ichk = ConditionalExpr.create(it, cmp.copy(), ichk, new LiteralExpr(lit));

      assert assertTrace(trace, "   ichk ", ichk);
      assert assertTrace(trace, "   uchk ", uchk);
    }

    LoopTailChord lt   = lh.getLoopTail();
    Chord         exit = le.getNextChord();
    LoopExitChord nle  = (LoopExitChord) le.copy();
    NullChord     nc   = new NullChord();

    Stack<Chord>          wl   = WorkArea.<Chord>getStack("unrollFor");
    HashMap<Chord, Chord> nm   = new HashMap<Chord, Chord>(23); // Mapping from old nodes to their new copies.
    Vector<Chord>         newn = new Vector<Chord>(128); // New CFG nodes created.

    // Unroll the loop the first time.
    // Copy the LoopTailChord, etc in this case.

    lph.insertBeforeInCfg(nc); // Insure that the pre-header has only one in-coming edge.
    nm.put(le, nle);           // Terminate new subgraph at the LoopExitChord.
    nm.put(exit, lph);
    newn.addElement(nc);
    newn.addElement(nle);

    // Note that we currently don't maintain the profile during
    // unrolling (too complicated!)

    Vector<Chord> phis = Scribble.grabSubgraph(lph, nm, newn, wl);
    Scribble.linkSubgraph(newn, nm, phis);

    LoopHeaderChord    nlh     = (LoopHeaderChord) nm.get(lh);
    LoopPreHeaderChord nlph    = (LoopPreHeaderChord) nm.get(lph);
    LoopTailChord      loopEnd = (LoopTailChord) nm.get(lt);
    Chord              nlt     = loopEnd;

    nle.setLoopHeader(nlh);

    // Create the "use unrolled loop" test.

    IfThenElseChord ifc = new IfThenElseChord(ichk, lph, nlph);
    nc.changeOutCfgEdge(nlph, ifc);
    ifc.copySourceLine(lh);
    ssa.addNewNode(ifc);

    // Create the unrolled loop's exit test.

    IfThenElseChord nltc = (IfThenElseChord) nm.get(ltc);
    Expr            oldp = nltc.getPredicateExpr();
    nltc.changeInDataEdge(oldp, uchk);
    oldp.unlinkExpression();

    assert assertTrace(trace, "   ltc  ", ltc);
    assert assertTrace(trace, "     t  ", ltc.getTrueCfgEdge());
    assert assertTrace(trace, "     f  ", ltc.getFalseCfgEdge());
    assert assertTrace(trace, "   nltc ", nltc);
    assert assertTrace(trace, "     t  ", nltc.getTrueCfgEdge());
    assert assertTrace(trace, "     f  ", nltc.getFalseCfgEdge());

    // Don't copy Phi functions.

    boolean isOp0  = (0 == lh.indexOfInCfgEdge(lph));
    Chord   ins    = nlh;
    Chord   first  = nlh.getNextChord();
    Chord   sf     = first;
    Chord   psf    = first;
    Vector<ExprChord>  copies = new Vector<ExprChord>();
    Vector<PhiExprChord>  nphis  = new Vector<PhiExprChord>();
    while (first.isPhiExpr()) {
      PhiExprChord pc   = (PhiExprChord) first;
      PhiExpr      phi  = pc.getPhiFunction();
      Expr         op   = phi.getOperand(isOp0 ? 1 : 0);
      Expr         lhs  = pc.getLValue();
      boolean      doit = true;

      if (lhs instanceof LoadExpr)
        doit = !((LoadExpr) lhs).getDecl().isVirtual();

      if (doit) {
        ExprChord se = new ExprChord(lhs.copy(), op.copy());
        copies.addElement(se);
        nphis.addElement(pc);
      }

      ins = first;
      first = first.getNextChord();
    }

    HashSet<Chord> stops = WorkArea.<Chord>getSet("unrollFor");
    int            cl    = copies.size();
    Vector<Expr>   sop1  = null;
    if (doPeeling) {
      // Peel the loop: insert a copy of the loop body before the
      // unrolled loop.

      LoopExitChord         nlep  = new LoopExitChord(lh);
      HashMap<Chord, Chord> nmp   = new HashMap<Chord, Chord>(23); // Mapping from old nodes to their new copies.
      Vector<Chord>        newnp = new Vector<Chord>(128); // New CFG nodes created.
      NullChord             ncp   = new NullChord();

      nlph.insertBeforeInCfg(ncp); // Insure that the pre-header has only one in-coming edge.
      newnp.addElement(ncp);
      nmp.put(lt, nlph); // Terminate new subgraph at the LoopTailChord
      nmp.put(le, nlep); // and loop exit.

      // Don't peel the phi functions as they are not required at the
      // start of the peeled loop body.

      Chord  firstp = lh.getNextChord();
      while (firstp.isPhiExpr())
        firstp = firstp.getNextChord();

      // Copy the loop body and link it into the CFG just before the
      // unrolled loop.

      Vector<Chord> phisp = Scribble.grabSubgraph(firstp, nmp, newnp, wl);
      Scribble.linkSubgraph(newnp, nmp, phisp);
      Chord insp = nmp.get(firstp);
      ncp.setTarget(insp);
      IfThenElseChord dc = (IfThenElseChord) nmp.get(ltc);
      deleteLoopExitTest(dc, lh, nlep);

      // Insert copies of the SSA variables so that the use-def
      // renaming logic works properly.

      sop1 = new Vector<Expr>(cl);
      for (int i = 0; i < cl; i++) {
        PhiExprChord pc  = nphis.get(i);
        PhiExpr      phi = pc.getPhiFunction();
        ExprChord    cc  = copies.get(i);
        Expr         op0 = phi.getOperand(isOp0 ? 0 : 1);

        if (op0 instanceof LoadDeclValueExpr) {
          LoadDeclValueExpr ldve = (LoadDeclValueExpr) op0;
          Expr              lhs2 = new LoadDeclAddressExpr(ldve.getDecl());
          ExprChord         ncca = new ExprChord(lhs2, cc.getRValue().copy());
          ssa.addNewNode(ncca);
          nlph.insertBeforeInCfg(ncca);
          ncca.copySourceLine(nlph);

          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) pc.getLValue();
          Expr                lhs  = new LoadDeclAddressExpr(ldae.getDecl());
          Expr                rhs  = new LoadDeclValueExpr(ldve.getDecl());
          ExprChord           nccb = new ExprChord(lhs, rhs);
          ssa.addNewNode(nccb);
          insp.insertBeforeInCfg(nccb);
          nccb.copySourceLine(nlph);
        }

        Expr op1 = phi.getOperand(isOp0 ? 1 : 0);
        sop1.add(op1);
        phi.changeInDataEdge(op1, new LiteralExpr(Lattice.Top));
      }

      stops.add(sf);
      sf = ncp;
    }

    // Unroll the loop the additional times.

    for (int j = 0; j < unrollCount - 1; j++) {
      newn.clear();

      nm.put(nlt, first); // Terminate new subgraph before the LoopTailChord.
      nm.put(nle, nle);   // Terminate new subgraph before the LoopExitChord.
      newn.addElement(ins);

      // Note that currently we don't maintain profile information
      // during unrolling (too complicated!).

      Vector<Chord> ophis = Scribble.grabSubgraph(first, nm, newn, wl);
      Scribble.linkSubgraph(newn, nm, ophis);

      Chord pos = first;
      nlt = first;
      first = nm.get(first);

      for (int i = 0; i < cl; i++) {
        ExprChord cc = copies.elementAt(i);
        if (j != 0)
          cc = new ExprChord(cc.getLValue().copy(), cc.getRValue().copy());
        ssa.addNewNode(cc);
        pos.insertBeforeInCfg(cc);
        cc.copySourceLine(pos);
        if (i == 0) {
          nlt = cc;
          stops.add(cc);
        }
      }

      if (lte) {
        if (j == 0) // Leave the first (nltc) - remove the second, etc.
          deleteLoopExitTest((IfThenElseChord) nm.get(nltc), nlh, nle);
      } else {
        if (j != (unrollCount - 2)) { // Remove all but the last one generated.
          Chord nxt = deleteLoopExitTest(nltc, nlh, nle);
          if ((nxt != null) && (nltc == nlt))
            nlt = nxt;
          nltc = (IfThenElseChord) nm.get(nltc);
        }
      }
    }

    if (!lte)
      deleteLoopExitTest(nltc, nlh, nle);

    // Get the use-defs right.

    HashMap<Declaration, ExprChord> remap = new HashMap<Declaration, ExprChord>(203);

    Chord.nextVisit();
    nle.setVisited();
    if (!doPeeling)
      nlh.setVisited();

    fixupNewCode(sf, remap, stops, wl);

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(stops);

    // Fix up the operands to the Phi functions at the start of the
    // loop.  They need to use the variables created at the end of the
    // loop.


    for (int i = 0; i < cl; i++) {
      PhiExprChord pc  = nphis.get(i);
      if (doPeeling) {
        PhiExpr      phi = pc.getPhiFunction();
        Expr         op1 = phi.getOperand(isOp0 ? 1 : 0);
        phi.changeInDataEdge(op1, sop1.get(i));
      }
      fixupUseDefs(pc, remap, false);
    }

    IfThenElseChord      jt     = null;
    Vector<PhiExprChord> lhphis = lh.findPhiChords();
    int                  lp     = lhphis.size();
    if (lte) {
      // Insert the branch after the unrolled loop that goes to the
      // old loop or the old loop's exit.

      Chord oldlp = nle.getNextChord();
      Expr  nt    = null;
      if (exact)
        nt = ichk.copy();
      else
        nt = mex.copy();

      if (useTrue)
        jt = new IfThenElseChord(nt, exit, oldlp);
      else
        jt = new IfThenElseChord(nt, oldlp, exit);

      ssa.addNewNode(jt);
      nle.changeOutCfgEdge(oldlp, jt);
      jt.copySourceLine(oldlp);

      // Add an operand for this branch point to the phi functions at
      // the exit.  We assume that the new in-coming edge at the exit
      // is last.  If there were not any phi functions there before,
      // we must add ones in case the loop variables are referenced
      // after the original loop.

      Vector<PhiExprChord> ph  = exit.findPhiChords();
      int                  phl = ph.size();
      if (phl > 0) {
        for (int i = 0; i < phl; i++) {
          PhiExprChord        se   = ph.elementAt(i);
          PhiExpr             phi  = se.getPhiFunction();
          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) se.getLValue();
          VariableDecl        vd   = (VariableDecl) ldae.getDecl();
          VariableDecl        var  = vd.getOriginal();
          phi.addOperand(findVar(nle, var));
        }
      } else { // Add new phis as needed.
        for (int i = 0; i < lp; i++) {
          PhiExprChord        se   = lhphis.elementAt(i);
          PhiExpr             phi  = se.getPhiFunction();
          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) se.getLValue();
          VariableDecl        vd   = (VariableDecl) ldae.getDecl();
          VariableDecl        var  = vd.getOriginal();
          Vector<Expr>        nops = new Vector<Expr>(2);
          Expr                op   = se.getLValue();
          if (lte)
            op = phi.getOperand(isOp0 ? 1 : 0);

          VariableDecl        nvd  = ssa.createRenamedVariable(var, true);
          LoadDeclValueExpr   nld  = new LoadDeclValueExpr(nvd);
          LoadDeclAddressExpr nl   = new LoadDeclAddressExpr(nvd);

          nops.addElement(op.copy());
          nops.addElement(findVar(nle, var));

          PhiExpr             phi2  = new PhiExpr(phi.getType(), nops);
          PhiExprChord        pc2   = new PhiExprChord(nl, phi2);

          ssa.addNewNode(pc2);

          exit.insertBeforeInCfg(pc2);

          // Fix up uses of the variable that are after the loop.

          ExprChord se3 = se;
          if (lte)
            se3 = op.getUseDef();

          LoadExpr[] se3l = se3.getDefUseArray();
          for (int k = 0; k < se3l.length; k++) {
            LoadExpr ld3  = se3l[k];
            Chord    ld3c = ld3.getChord();
            if ((ld3c.getLoopHeader() != lh) && (ld3c != pc2)) {
              ld3.setDecl(nvd);
              ld3.setUseDef(pc2);
            }
          }
        }
      }
    }

    // Insert phi functions before the old loop to join in the return
    // from the unrolled loop.

    Chord[] exs  = nle.getInCfgEdgeArray();
    Chord   posa = nle;
    Chord   posb = lph;
    boolean in0  = (0 == posb.indexOfInCfgEdge(ifc));
    for (int i = 0; i < lp; i++) {
      PhiExprChord        se   = lhphis.elementAt(i);
      PhiExpr             phi  = se.getPhiFunction();
      Expr                op0  = phi.getOperand(isOp0 ? 0 : 1);
      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) se.getLValue();
      VariableDecl        vd   = (VariableDecl) ldae.getDecl();
      VariableDecl        var  = vd.getOriginal();
      VariableDecl        nvd  = ssa.createRenamedVariable(var, true);
      LoadDeclValueExpr   nld  = new LoadDeclValueExpr(nvd);
      LoadDeclAddressExpr nl   = new LoadDeclAddressExpr(nvd);

      ssa.addNewLoad(nld);

      if (exs.length > 1) {
        Vector<Expr> ops2  = new Vector<Expr>(2);
        for (int k = 0; k < exs.length; k++) {
          Expr lex = findVar(exs[k], var);
          ops2.addElement(lex);
        }

        VariableDecl nvd2 = ssa.createRenamedVariable(var, true);
        PhiExpr      phi2 = new PhiExpr(var.getType(), ops2);
        ExprChord    cc2  = new PhiExprChord(new LoadDeclAddressExpr(nvd2), phi2);

        ssa.addNewNode(cc2);

        posa.insertBeforeInCfg(cc2);
        posa = cc2;
      }

      phi.changeInDataEdge(op0, nld);

      // Create a new phi function.

      Vector<Expr> ops = new Vector<Expr>(2);
      if (in0) {
        ops.addElement(op0);
        ops.addElement(findVar(nle, var));
      } else {
        ops.addElement(findVar(nle, var));
        ops.addElement(op0);
      }

      PhiExpr   phi2 = new PhiExpr(var.getType(), ops);
      ExprChord cc   = new PhiExprChord(nl, phi2);

      nld.setUseDef(cc);

      ssa.addNewNode(cc);

      posb.insertBeforeInCfg(cc);

      posb = cc;
      newCFGNodeCount++;
    }

    // Fix up the test that joins the two loops when the loop exit
    // test is at the end of the loop.

    if (jt != null) {
      Expr             jtp = jt.getPredicateExpr();
      Vector<LoadExpr> v   = new Vector<LoadExpr>();

      jtp.getLoadExprList(v);

      int l = v.size();
      for (int i = 0; i < l; i++) {
        LoadExpr     jtle = v.elementAt(i);
        VariableDecl vd   = (VariableDecl) jtle.getDecl();
        VariableDecl var  = vd.getOriginal();

        if (vd == var)
          continue;

        ExprChord    jtse = findVarDef(jt, var);
        Declaration  jtvd = ((LoadDeclAddressExpr) jtse.getLValue()).getDecl();

        jtle.setDecl(jtvd);
        jtle.setUseDef(jtse);
      }
    }

    // Propagate the induction variables.

    propagate(nphis, loopEnd);

    // Now, wasn't that painful?  Three cheers for SSA form.
    // Yuk! Yuk! Yuk!

    unrollForCount++;

    dChanged = true;

    return finishedProcessing;
  }

  /**
   * Try to unroll the loop as a while-loop.  If that fails, we give
   * up trying to transform the loop.
   */
  private int unrollWhile(LoopHeaderChord lh, int loopN)
  {
    // Unrolling loops that have no induction variable has no benefit
    // on normal machines because the only thing that can be
    // eliminated is the back branch.  Some systems (e.g., Trips) have
    // predication and benefit from having larger loop bodies even
    // when not all the instructions in the loop are executed.

    if (inhibitWhileLoops ||
        !Machine.currentMachine.hasCapability(Machine.HAS_WHILE_LOOP_UNROLL_BENEFIT))
      return stopNow;

    assert assertTrace(trace, "   while ", lh);

    // We can't unroll a loop with more than one loop exit because
    // doing the renaming of the variables used outside of the loop,
    // that are set in the loop, is non-determinable.  The use outside
    // of the loop may not be in the iterative dominator set of any of
    // the loop exits.  Thus, the only valid test that can be used to
    // determine if the use of a variable must be renamed, that is
    // used after the phi functions created at the loop exits, is
    // whether the use is inside of the loop or outside of the loop.
    // (But there should be only one LoopExitChords in the loop by
    // now.)

    assert (lh.numLoopExits() == 1) : "should be one exit by now";

    // Should not be multiple incoming CFG edges to a LoopExitChord by
    // now.

    LoopExitChord le   = lh.getFirstExit();
    Chord         fibb = le.firstInBasicBlock();

    assert (!fibb.isPhiExpr()) : "while loop should pass isPhiExpr test by now";

    if (unrollCount <= 1)
      return stopNowTooBig;

    // At this point we know we can unroll it as a while-loop.

    LoopPreHeaderChord   lph    = lh.getPreHeader();
    boolean              isOp0  = (0 == lh.indexOfInCfgEdge(lph));
    Chord                first  = lh.getNextChord();
    Vector<ExprChord>    copies = new Vector<ExprChord>();
    Vector<PhiExprChord> nphis  = new Vector<PhiExprChord>();
    SequentialChord      lphi   = lh;
    HashSet<Chord>       cill   = WorkArea.<Chord>getSet("unrollWhile");

    lh.getLoopChordsRecursive(cill);

    while (first.isPhiExpr()) {
      PhiExprChord pc   = (PhiExprChord) first;
      PhiExpr      phi  = pc.getPhiFunction();
      Expr         op   = phi.getOperand(isOp0 ? 1 : 0);
      Expr         lhs  = pc.getLValue();
      boolean      doit = true;

      if (lhs instanceof LoadExpr) {
        doit = !((LoadExpr) lhs).getDecl().isVirtual();

        // Remove these use-def links - they won't be valid after the
        // loop body is replicated.  We'll add back valid use-def
        // links later.

        int ndul = pc.numDefUseLinks();
        for (int i = ndul - 1; i >= 0; i--) {
          LoadExpr use = pc.getDefUse(i);
          if (cill.contains(use.getChord()))
            use.removeUseDef();
        }
      }

      if (doit) {
        ExprChord se = new ExprChord(lhs.copy(), op.copy());
        copies.addElement(se);
        nphis.addElement(pc);
      }

      lphi = pc;
      first = first.getNextChord();
    }

    WorkArea.<Chord>returnSet(cill);

    Vector<Chord>  newn  = new Vector<Chord>(128); // New CFG nodes created.
    Chord          nlt   = lh.getLoopTail();
    Stack<Chord>   wl    = WorkArea.<Chord>getStack("unrollWhile");
    HashSet<Chord> stops = WorkArea.<Chord>getSet("unrollWhile");
    int            cl    = copies.size();
    HashMap<Chord, Chord> nm = new HashMap<Chord, Chord>(23); // Mapping from old nodes to their new copies.

    nm.put(le, le);

    // Unroll the loop.

    for (int j = 0; j < unrollCount - 1; j++) {
      newn.clear();
      nm.put(nlt, first); // Terminate new subgraph before the LoopTailChord.

      Vector<Chord> ophis = Scribble.grabSubgraph(first, nm, newn, wl);
      Scribble.linkSubgraph(newn, nm, ophis);

      Chord pos = first;
      nlt = first;
      first = nm.get(first);

      // Insert copies in place of the original phis to join up the
      // new copies.

      for (int i = 0; i < cl; i++) {
        ExprChord cc = copies.elementAt(i);
        if (j != 0)
          cc = new ExprChord(cc.getLValue().copy(), cc.getRValue().copy());
        ssa.addNewNode(cc);
        pos.insertBeforeInCfg(cc);
        cc.copySourceLine(pos);
        newn.add(cc);
        if (i == 0) {
          nlt = cc;
          stops.add(cc);
        }
      }

      // Remove the use-def links from the copied CFG nodes.  Do not
      // remove the may-use links as they are still valid and get used
      // by other optimizations.  New use-def links will be added
      // after the new loop body is constructed.

      int ll = newn.size();
      for (int k = 0; k < ll; k++) {
        Chord            c = newn.get(k);
        Vector<LoadExpr> v = c.getLoadExprList();
        if (v == null)
          continue;
        int    n = v.size();
        for (int kk = 0; kk < n; kk++) {
          LoadExpr  lde = v.get(kk);
          ExprChord ud  = lde.getUseDef();
          if (cill.contains(ud))
            lde.removeUseDef();
        }
      }
    }

    lphi.setTarget(first);

    HashMap<Declaration, ExprChord> remap = new HashMap<Declaration, ExprChord>(203);

    Chord.nextVisit();
    lh.setVisited();
    le.setVisited();

    fixupNewCode(lh, remap, stops, wl);

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(stops);

    // Fix up the operands to the Phi functions at the start of the
    // loop.  They need to use the variables created at the end of the
    // loop.

    Chord sf = lh;
    while (sf != null) {
      if (sf.isExprChord())
        fixupUseDefs((ExprChord) sf, remap, false);
      sf = sf.getNextChord();
    }

    // This is probably an expensive way to determine if a CFG node is
    // in the loop.

    HashSet<Chord> cil = WorkArea.<Chord>getSet("unrollWhile");
    lh.getLoopChordsRecursive(cil);

    // Add required phi functions at the loop exits since they are now
    // referenced from multiple places.

    Vector<PhiExprChord>  lhphis = lh.findPhiChords();
    int                   lp     = lhphis.size();
    Chord[]               exs    = le.getInCfgEdgeArray();

    for (int j = 0; j < lp; j++) {
      PhiExprChord        se   = lhphis.elementAt(j);
      PhiExpr             phi  = se.getPhiFunction();
      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) se.getLValue();
      VariableDecl        vd   = (VariableDecl) ldae.getDecl();
      VariableDecl        var  = vd.getOriginal();
      Vector<Expr>        nops = new Vector<Expr>(exs.length);
      VariableDecl        nvd  = ssa.createRenamedVariable(var, true);
      LoadDeclValueExpr   nld  = new LoadDeclValueExpr(nvd);
      LoadDeclAddressExpr nl   = new LoadDeclAddressExpr(nvd);
      PhiExpr             phi2 = new PhiExpr(phi.getType(), nops);
      PhiExprChord        pc2  = new PhiExprChord(nl, phi2);

      cil.add(pc2);

      for (int k = 0; k < exs.length; k++) {
        LoadDeclValueExpr op = findVar(exs[k], var);

        nops.addElement(op);
        op.setOutDataEdge(phi2);

        // Fix up uses of the variable that are after the exit.

        ExprChord se3    = op.getUseDef();
        LoadExpr[]  se3l = se3.getDefUseArray();
        for (int kk = 0; kk < se3l.length; kk++) {
          LoadExpr ld3  = se3l[kk];
          Chord    ld3c = ld3.getChord();
          if (!cil.contains(ld3c)) {
            ld3.setDecl(nvd);
            ld3.setUseDef(pc2);
          }
        }
      }

      ssa.addNewNode(pc2);

      le.insertBeforeInCfg(pc2);
    }

    WorkArea.<Chord>returnSet(cil);

    // Propagate the induction variables.

    propagate(nphis, lh.getLoopTail());

    lh.recomputeLoop();
    unrollWhileCount++;

    dChanged = true;

    return finishedProcessing;
  }

  /**
   * Change the names of the variables with multiple definitions
   * created by duplicating the code.  Make the use-def links point to
   * the right def.  There should only be one def per variable as a
   * result.
   */
  private void fixupNewCode(Chord                           start,
                            HashMap<Declaration, ExprChord> remap,
                            HashSet<Chord>                  stops,
                            Stack<Chord>                    wl)
  {
    // Must be processed in breath-first order.  And, each copy of the
    // loop body must be completely processed before the next copy of
    // the loop body.

    int label = 0;
    while (start != null) {
      wl.push(start);
      start.setLabel(label++);
      start = null;

      for (int ii = 0; ii < wl.size(); ii++) {
        Chord c = wl.elementAt(ii);
        if (c == null)
          continue;

        wl.setElementAt(null, ii);

        if ((stops != null) && stops.contains(c)) {
          stops.remove(c);
          start = c;
          continue;
        }

        c.setVisited();

        fixupUseDefs(c, remap, true);

        int len = c.numOutCfgEdges();
        for (int i = 0; i < len; i++)
          label = c.getOutCfgEdge(i).pushChordWhenReady(wl, label);
      }

      wl.clear();
    }
  }

  private void fixupUseDefs(Note exp, HashMap<Declaration, ExprChord> remap, boolean doStores)
  {
    if (exp instanceof LoadExpr) {
      LoadExpr    le   = (LoadExpr) exp;
      Declaration decl = le.getDecl();
      ExprChord   se   = remap.get(decl);
      if (se != null) {
        Expr                lhs  = se.getLValue();
        LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
        le.setDecl(ldae.getDecl());
        le.setUseDef(se);
      }
      return;
    }

    if (doStores && (exp instanceof ExprChord)) {
      ExprChord se  = (ExprChord) exp;
      Expr      lhs = se.getLValue();
      fixupUseDefs(se.getRValue(), remap, doStores);
      if (lhs instanceof LoadDeclAddressExpr) {
        LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
        VariableDecl        vd   = (VariableDecl) ldae.getDecl();
        if (!vd.isNotSSACandidate()) {
          VariableDecl nvd = ssa.createRenamedVariable(vd.getOriginal(), true);
          ldae.setDecl(nvd);
          remap.put(vd, se);
          LoadExpr[] uses = se.getDefUseArray();
          for (int jj = 0; jj < uses.length; jj++) {
            uses[jj].setDecl(nvd);
          }
          return;
        }
      } else
        fixupUseDefs(lhs, remap, doStores);
      return;
    }

    if (exp == null)
      return;

    int l = exp.numInDataEdges();
    for (int i = 0; i < l; i++)
      fixupUseDefs(exp.getInDataEdge(i), remap, doStores);
  }

  private boolean validUse(VariableDecl var, Expr exp)
  {
    if (exp instanceof LoadDeclValueExpr) {
      Declaration decl = ((LoadDeclValueExpr) exp).getDecl();
      return decl.isVariableDecl() && var.getOriginal() == ((VariableDecl) decl).getOriginal();
    }

    if ((exp instanceof AdditionExpr) ||
        (exp instanceof SubtractionExpr) ||
        (exp instanceof MultiplicationExpr)) {
      BinaryExpr be = (BinaryExpr) exp;
      return validUse(var, be.getLeftArg()) || validUse(var, be.getRightArg());
    }

    if (exp instanceof NegativeExpr)
      return validUse(var, ((UnaryExpr) exp).getArg());

    if (exp instanceof ConversionExpr)
      return validUse(var, ((UnaryExpr) exp).getArg());

    if (exp instanceof AbsoluteValueExpr)
      return validUse(var, ((UnaryExpr) exp).getArg());

    return false; 
  }

  private IfThenElseChord findExitTest(Expr test)
  {
    Note n = test.getOutDataEdge();
    if (!(n instanceof Chord))
      return null;

    Chord lec = (Chord) n;

    if (lec.isAssignChord()) {
      ExprChord se  = (ExprChord) lec;
      int       ndu = se.numDefUseLinks();
      for (int id = 0; id < ndu; id++) {
        LoadExpr use = se.getDefUse(id);
        Note     uc  = use.getOutDataEdge();
        if (uc instanceof IfThenElseChord)
          return (IfThenElseChord) uc;
      }
      return null;
    } else if (lec instanceof IfThenElseChord)
      return (IfThenElseChord) lec;

    return null;
  }

  /**
   * Delete the loop exit test from the unrolled code - only the first
   * (last) one is needed.  Return the alive edge from the loop exit
   * test.
   */
  private Chord deleteLoopExitTest(IfThenElseChord dc, LoopHeaderChord lh, LoopExitChord le)
  {
    Chord tEdge = dc.getTrueCfgEdge();
    Chord fEdge = dc.getFalseCfgEdge();
    Chord sEdge = tEdge;
    Chord dEdge = fEdge;

    // Determine which edge is alive.

   if (le == tEdge.findLoopExit(lh)) {
      sEdge = fEdge;
      dEdge = tEdge;
    } else if (le != fEdge.findLoopExit(lh))
      return null;

    // Remove the loop exit test.

    dc.changeParentOutCfgEdge(sEdge);
    dc.changeOutCfgEdge(sEdge, null);
    dc.unlinkChord();

    // Get rid of the entire dead edge.

    Chord parent = dc;
    Chord child  = dEdge;
    while ((child != null) && (child.numInCfgEdges() <= 1)) {
      parent = child;
      child.unlinkChord();
      child = child.getNextChord();
    }

    // Fix up any phi functions whose in-coming edge is now dead.

    if (child != null) {
      int   edge = child.indexOfInCfgEdge(parent);
      Chord pc   = child;
      while (pc.isPhiExpr()) {
        PhiExpr phi = ((PhiExprChord) pc).getPhiFunction();
        phi.removeOperand(edge);
        pc = pc.getNextChord();
      }
      parent.changeOutCfgEdge(child, null);
    }

    return sEdge;
  }

  private boolean isLoopTestAtEnd(IfThenElseChord ifc)
  {
    return (ifc.getTrueCfgEdge() .isLoopTail() ||
            ifc.getFalseCfgEdge().isLoopTail());
  }

  /**
   * Try to flatten or unroll the loop. 
   * @return the status of the attempt 
   */
  private int flatten(LoopHeaderChord lh, int loopN)
  {
    if (inhibitFlattenLoops)
      return tryForUnrolling;

    // Can't flatten if there's no induction variable.

    if (ivar == null)
      return tryWhileUnrolling;

    // Can't flatten if there are too many induction variables.

    assert assertTrace(trace, "   npiv ", lh.numPrimaryInductionVars());

    if (lh.numPrimaryInductionVars() != 1) // Too complex.
      return tryWhileUnrolling;

    // Can't handle complex forms.

    Expr test = ivar.getTermExpr();
    if ((test == null) || !test.isMatchExpr())
      return tryWhileUnrolling;

    // Must be able to find the exit test.

    IfThenElseChord ltc = findExitTest(test);
    if (ltc == null)
      return tryWhileUnrolling;

    LoopExitChord   le   = lh.getFirstExit();
    LoopTailChord   lt   = lh.getLoopTail();
    Chord           fibb = le.firstInBasicBlock();

    assert assertTrace(trace, "   mex  ", (MatchExpr) test);
    assert assertTrace(trace, "   ltc  ", ltc);

    // Get the trip count.

    long tripCount = knownItCnt; 
    if (tripCount == 0) {
      if (lt.getInCfgEdge() == ltc)
        tripCount = 1; 
    }

    if (tripCount < 0) // Unknown trip count.  Cannot flatten.
      return tryForUnrolling;

    if (unrollCount <= 0)
      return stopNowTooBig;

    assert assertTrace(trace, "  urc ", unrollCount);

    // Cannot flatten if the known iteration count is not the same as
    // the computed unroll factor.

    if (unrollCount != tripCount)
      return tryForUnrolling;

    // If the tripCount is zero, we really ought to remove the loop
    // completely.  Perhaps we ought to special case for 0 & 1.

    if (unrollCount <= 1)
      return stopNow;

    // The loop exit test must be the sole determinant of the loop exit.

    Domination    dom = scribble.getDomination();
    Vector<Chord> id  = dom.getIterativeDomination(ltc);

    if (!id.contains(lt))
      return tryForUnrolling;
    
    // Collect the phi functions at the start of the loop.

    Vector<PhiExprChord> phis = lh.findPhiChords();

    // Collect the phi function variable names that were last set
    // before loop exit.

    Vector<Declaration> dvd  = new Vector<Declaration>(); // The renamed variable last set before loop exit.
    Vector<LoadExpr[]>  svd  = new Vector<LoadExpr[]>(); // The def-use links from the set of the renamed variable.
    int    plen = phis.size();
    for (int i = 0; i < plen; i++) {
      PhiExprChord se  = phis.elementAt(i);
      PhiExpr      phi = se.getPhiFunction();

      LoadDeclAddressExpr lhs = (LoadDeclAddressExpr) se.getLValue();
      VariableDecl        var = ((VariableDecl) lhs.getDecl()).getOriginal();

      // Look up along the graph edge for the last set of the
      // variable.

      boolean found = false;
      for (Chord edge = ltc; edge != null; edge = edge.getFirstInCfgEdge()) {
        if (!edge.isAssignChord())
          continue;

        ExprChord se2  = (ExprChord) edge;
        Expr      lhs2 = se2.getLValue();
        if (!(lhs2 instanceof LoadDeclAddressExpr))
          continue;

        LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs2;
        VariableDecl        vd   = (VariableDecl) ldae.getDecl();
        if (vd.getOriginal() != var)
          continue;

        if (false && se2 != se) {
          LoadDeclValueExpr ldve = (LoadDeclValueExpr) phi.getOperand(1);
          dvd.addElement(ldve.getDecl());
          svd.addElement(ldve.getUseDef().getDefUseArray());
        } else {
          dvd.addElement(ldae.getDecl());
          svd.addElement(se2.getDefUseArray());
        }
        found = true;
        break;
      }

      if (!found)
        return tryForUnrolling;
    }

    if (fibb != ltc.getTrueCfgEdge() && fibb != ltc.getFalseCfgEdge())
      return tryWhileUnrolling;

    // Flatten the loop.  By the time this is called, we know that we
    // can do so -- the trip count is known and small enough, and
    // various other constraints are satisfied.  No loop constructs are
    // generated, we can just duplicate the loop body the number of
    // times the loop will iterate.

    // Eliminate the loop exit test.

    Chord exit = null;
    Chord ted  = ltc.getTrueCfgEdge();
    Chord fed  = ltc.getFalseCfgEdge();
    if (ted == fibb) {
      ltc.changeParentOutCfgEdge(fed);
      exit = ted;
    } else if (fed == fibb) {
      ltc.changeParentOutCfgEdge(ted);
      exit = fed;
    } else {
      assert (false) : "fibb should equal ted or fed here";
    }

    assert assertTrace(trace, "  ** flatten ", lh);
    assert assertTrace(trace, "      urc  ", unrollCount);
    assert assertTrace(trace, "      ivar ", lh.getPrimaryInductionVar());
    assert assertTrace(trace, "      exit ", exit);
    assert assertTrace(trace, "      le   ", le);
    assert assertTrace(trace, "  ******", null);

    // Eliminate the LoopTailChord.

    NullChord nc = new NullChord();

    lt.insertBeforeInCfg(nc);
    lt.changeParentOutCfgEdge(exit);
    ltc.removeFromCfg();

    // Change the phi functions into copies.

    LoopPreHeaderChord lph    = lh.getPreHeader();
    Chord              ins    = lh;
    Vector<ExprChord>  copies = new Vector<ExprChord>(); // The copy nodes.
    boolean            isOp0  = (0 == lh.indexOfInCfgEdge(lph));
    Vector<ExprChord>  props  = new Vector<ExprChord>(phis.size());
    for (int i = 0; i < plen; i++) {
      PhiExprChord        se  = phis.elementAt(i);
      LoadDeclAddressExpr lhs = (LoadDeclAddressExpr) se.getLValue();

      PhiExpr   phi = se.getPhiFunction();
      Expr      op1 = phi.getOperand(1);
      Expr      op0 = phi.getOperand(0);
      ExprChord se1 = new ExprChord(lhs.copy(), op0.copy());
      ExprChord se0 = new ExprChord(lhs.copy(), op1.copy());
      if (isOp0) {
        ExprChord t = se1;
        se1 = se0;
        se0 = t;
      }
      ExprChord cp = se0;

      props.add(se0); // Save for copy propagation.
      ssa.addNewNode(cp);
      copies.addElement(se1);

      se.insertBeforeInCfg(cp);
      se.removeFromCfg();
      ins = cp;
    }

    // Unroll the loop the additional times.

    Chord          first = ins.getNextChord();
    Stack<Chord>   wl    = WorkArea.<Chord>getStack("flatten");
    HashMap<Chord, Chord> nm    = new HashMap<Chord, Chord>(23); /* Mapping from old nodes to their new copies. */
    Chord          nxt   = nc;
    Vector<Chord>  newn  = new Vector<Chord>(); // New CFG nodes created.
    HashSet<Chord> stops = WorkArea.<Chord>getSet("flatten");

    nm.put(fibb, fibb);
    for (int j = 0; j < unrollCount - 1; j++) {
      newn.clear();

      nm.put(nxt, first); // Terminate new subgraph before the end.
      newn.addElement(ins);

      // Note that currently we don't maintain the profile during unrolling (too complicated!)
      Vector<Chord> sgp = Scribble.grabSubgraph(first, nm, newn, wl);
      Scribble.linkSubgraph(newn, nm, sgp);

      Chord pos = first;
      nxt = first;
      first = nm.get(first);

      for (int i = 0; i < copies.size(); i++) {
        ExprChord cc = copies.elementAt(i);
        if (j != 0)
          cc = new ExprChord(cc.getLValue().copy(), cc.getRValue().copy());
        newCFGNodeCount++;
        ssa.addNewNode(cc);
        pos.insertBeforeInCfg(cc);
        cc.copySourceLine(pos);
        if (i == 0) {
          nxt = cc;
          stops.add(cc);
        }
      }
    }

    int lp = copies.size();
    if (lp > 0) {

      // Insert the new phi functions after the flattened code.  Note
      // - there may be additional branches out of the loop that
      // result in multiple operands to the phi function.  Or, it may
      // have just one argument.

      SequentialChord posb = new NullChord();

      exit.insertBeforeInCfg(posb);

      Chord   pose = exit;
      Chord[] in   = posb.getInCfgEdgeArray();
      for (int i = 0; i < lp; i++) {
        ExprChord           se   = copies.elementAt(i);
        LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) se.getLValue();
        VariableDecl        vd   = (VariableDecl) ldae.getDecl();
        VariableDecl        var  = vd.getOriginal();
        VariableDecl        tvd  = (VariableDecl) dvd.elementAt(i);
        LoadDeclAddressExpr nl   = new LoadDeclAddressExpr(tvd);
        ExprChord           cc   = null;

        if (in.length > 1) { // Create a new phi function.
          Vector<Expr> ops  = new Vector<Expr>(in.length);

          for (int ii = 0; ii < in.length; ii++)
            ops.addElement(findVar(in[ii], var));

          PhiExpr phi = new PhiExpr(var.getType(), ops);
          cc  = new PhiExprChord(nl, phi);
        } else { // Only a copy is needed.
          LoadDeclValueExpr fvn = findVar(in[0], var);
          if (fvn.getDecl() == tvd) {
            fvn.unlinkExpression();
            continue;
          }
          cc  = new ExprChord(nl, fvn);
        }

        ssa.addNewNode(cc);
        posb.insertAfterOutCfg(cc, pose);
        cc.copySourceLine(posb);
        posb = cc;
        newCFGNodeCount++;

        LoadExpr[] lds = svd.elementAt(i);
        for (int j = 0; j < lds.length; j++) {
          LoadExpr ld = lds[j];
          Chord    c = ld.getChord();
          if (c == null)
            continue;
          if (ld.getDecl() != tvd)
            continue;
          ld.setUseDef(cc);
        }
      }
    }

    // Set the use-def links.

    Chord.nextVisit();
    nc.setVisited();
    le.setVisited();

    fixupNewCode(lh.getNextChord(), new HashMap<Declaration, ExprChord>(203), stops, wl);

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(stops);

    // Propagate the induction variables.

    propagate(props, nc);

    // Clean up after flattening.
    // Eliminate the loop header, pre header, etc.

    lh.getParent().recomputeLoop();
    lph.changeParentOutCfgEdge(lh.getNextChord());
    lh.getLoopInit().removeFromCfg();
    le.insertBeforeInCfg(new NullChord());
    le.removeFromCfg();
    lph.removeFromCfg();
    lh.removeFromCfg();
    lt.removeFromCfg();

    flattenedCount++;

    dChanged = true;

    return finishedProcessing;
  }

  /**
   * Find the renamed variable for the specified original variable
   * along the CFG edge specified by the edge.  
   * @param edge specifies the in-coming CFG edge
   * @param var specifies the un-renamed variable
   * @return a LoadDeclValueExpr for the renamed variable with the use-def link set
   */
  private LoadDeclValueExpr findVar(Chord edge, VariableDecl var)
  {
    while (edge != null) {
      if (edge.isAssignChord()) {
        ExprChord se  = (ExprChord) edge;
        Expr      lhs = se.getLValue();
        if (lhs instanceof LoadDeclAddressExpr) {
          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
          VariableDecl        vd   = (VariableDecl) ldae.getDecl();
          if (vd.getOriginal() == var) {
            LoadDeclValueExpr ldve = new LoadDeclValueExpr(vd);
            ldve.setUseDef(se);
            return ldve;
          }
        }
      } else if (edge.isSpecial()) {
        if (edge.isLoopHeader()) {
          edge = ((LoopHeaderChord) edge).getPreHeader();
          continue;
        }
      }

      edge = edge.getFirstInCfgEdge();
    }

    throw new scale.common.InternalError("var not found - " + var);
  }

  /**
   * Find the def of the renamed variable for the specified original
   * variable along the specified CFG edge.
   * @param edge specifies the in-coming CFG edge
   * @param var specifies the un-renamed variable
   * @return the <code>ExprChord</code> for the renamed variable
   */
  private ExprChord findVarDef(Chord edge, VariableDecl var)
  {
    while (edge != null) {
      if (edge.isAssignChord()) {
        ExprChord se  = (ExprChord) edge;
        Expr      lhs = se.getLValue();
        if (lhs instanceof LoadDeclAddressExpr) {
          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
          VariableDecl        vd   = (VariableDecl) ldae.getDecl();
          if (vd.getOriginal() == var)
            return se;
        }
      } else if (edge.isSpecial()) {
        if (edge.isLoopHeader()) {
          edge = ((LoopHeaderChord) edge).getPreHeader();
          continue;
        }
      }
      edge = edge.getFirstInCfgEdge();
    }
    throw new scale.common.InternalError("var not found - " + var);
  }

  /**
   * Estimate the loop size, in instructions.
   *
   */
  private int estimateLoopIC(LoopHeaderChord loop)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("estimateLoopIC");

    if (ice == null)
      ice = Machine.sGetInstructionCountEstimator();

    Chord.nextVisit();
    wl.push(loop);
    loop.setVisited();

    int l = loop.numLoopExits();
    for (int i = 0; i < l; i++)
      loop.getLoopExit(i).setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();
      ice.estimate(c);
      c.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);

    return ice.getEstimateAndReset();
  }

  /**
   * Push the induction variables down so that their critical path
   * can be shorter.  For example,
   * <pre>
   *         = a[i_s_1];
   *   i_s_2 = i_s_1 + 1;
   *         = a[i_s_2];
   *   i_s_3 = i_s_2 + 1;
   *         = a[i_s_3];
   *   i_s_4 = i_s_3 + 1;
   * </pre>
   * is converted to
   * <pre>
   *         = a[i_s_1];
   *   i_s_2 = i_s_1 + 1;
   *         = a[i_s_1 + 1];
   *   i_s_3 = (i_s_1 + 1) + 1;
   *         = a[(i_s_1 + 1) + 1];
   *   i_s_4 = ((i_s_1 + 1) + 1) + 1;
   * </pre>
   * Sparse Condition Constat propagation will then further reduce this to
   * <pre>
   *         = a[i_s_1];
   *   i_s_2 = i_s_1 + 1;
   *         = a[i_s_1 + 1];
   *   i_s_3 = i_s_1 + 2;
   *         = a[i_s_1 + 2];
   *   i_s_4 = i_s_1 + 3;
   * </pre>
   * Exiting SSA mode will result in
   * <pre>
   *         = a[i];
   *         = a[i + 1];
   *         = a[i + 2];
   *       i = i + 3;
   * </pre>
   * @param ise is a list of store expressions that define the induction variables
   * at the top of the loop.
   */
  private void propagate(Vector<? extends ExprChord> ise, Chord loopEnd)
  {
    if (!doPropagation)
      return;

    int l = ise.size();
    for (int i = 0; i < l; i++) {
      ExprChord se = ise.elementAt(i);

      propagate(se.getDefUseArray(), se.getRValue(), true);
    }
  }

  /**
   * Propagate the induction variable specified by the def.  Follow
   * the def-use links down.  Don't go past the last def of the
   * induction variable in the loop.
   */
  private void propagate(LoadExpr[] uses, Expr rhs, boolean replace)
  {
    for (int i = 0; i < uses.length; i++) {
      LoadExpr use = uses[i];
      if (use.getChord() == null)
        continue;

      if (use instanceof LoadDeclAddressExpr)
        continue;

      Note out = use.getOutDataEdge();
      if (out == null)
        continue;

      if (use.getChord() == null)
        continue;

      if ((out instanceof PhiExpr) && !use.isLiteralExpr())
        continue;

      if ((out instanceof ExprChord) && (use == ((ExprChord) out).getLValue()))
        continue; // Don't modify left hand sides.

      if (replace && !(rhs instanceof PhiExpr)) { // Propagate the induction variable.
        Expr exp = rhs.copy();
        out.changeInDataEdge(use, exp);
        use.unlinkExpression();
      }

      if ((out instanceof Chord)) {
        if (out instanceof ExprChord)
          propagate((ExprChord) out);
        continue;
      }

      // Propagate <code>i + 1</code>.

      Expr exp  = (Expr) out;
      Expr exp2 = exp.reduce();
      if (exp2 != exp) {
        Note out2 = exp.getOutDataEdge();
        out2.changeInDataEdge(exp, exp2);
        exp.unlinkExpression();
        exp = exp2;
      }

      if (exp instanceof BinaryExpr) {
        BinaryExpr be = (BinaryExpr) exp;
        out = be.getOutDataEdge();

        Expr la = be.getLeftArg();
        Expr ra = be.getRightArg();
        if ((la == use) && !ra.isLiteralExpr())
          continue;
        if ((ra == use) && !la.isLiteralExpr())
          continue;

        if (out instanceof ExprChord) {
          propagate((ExprChord) out);
          continue;
        }
      }
    }
  }

  private void propagate(ExprChord se)
  {
    Expr lhs = se.getLValue();
    if (!(lhs instanceof LoadDeclAddressExpr))
      return;

    VariableDecl vd = (VariableDecl) ((LoadDeclAddressExpr) lhs).getDecl();
    if (vd.addressTaken())
      return; // Can't propagate variables whose address is taken.

    if (vd.isNotSSACandidate())
      return; // Can't propagate variables that are not in SSA form,

    // Don't propagate expensive expressions.
    // Don't replace any use with a phi function.

    Expr    rhs     = se.getRValue();
    boolean replace = (!(rhs instanceof PhiExpr) &&
                       rhs.optimizationCandidate() &&
                       rhs.isSimpleExpr());
    if (replace) {
      // Use a heuristic to avoid the combinatorial explosion that can
      // result from flattened loops such as:
      //
      //    for (k = 0; k < 20; k++) {
      //        cj = aj;
      //        bj = scalar * cj;
      //        cj = aj + bj;
      //        aj = bj + scalar * cj;
      //    }

      long cost = -1;
      if (rhs instanceof BinaryExpr) {
        BinaryExpr be = (BinaryExpr) rhs;
        Expr       la = be.getLeftArg();
        Expr       ra = be.getRightArg();
        if (ra.isLiteralExpr())
          cost = 0;
        else if (la.isLiteralExpr())
          cost = 0;
      }

      if (cost < 0)
        cost = rhs.executionCostEstimate();

      if (cost > 30)
        replace = false;
    }

    propagate(se.getDefUseArray(), rhs, replace);

    if (se.numDefUseLinks() == 0) { // Remove definition if no uses left.
      VariableDecl decl = ((VariableDecl) ((LoadDeclAddressExpr) lhs).getDecl()).getOriginal();
      if (decl.inMemory())
        return;

      // We can remove the CFG node if the result is no longer used.
      // However, we cannot remove a CFG edge because that
      // invalidates the Phi functions whose number of operands must
      // match the number of in-coming CFG edges.

      if (se.isLastInBasicBlock() && se.isFirstInBasicBlock())
        se.insertBeforeInCfg(new NullChord()); // The node was the only node on this CFG edge.

      se.removeFromCfg();
      newCFGNodeCount--;
    }
  }

  private void unrollJam(LoopHeaderChord lh)
  {
  }

  /**
   * Return whether this optimization requires that the CFG be in SSA form.
   * It returns either
   * <dl>
   * <dt>NO_SSA<dd>the CFG must not be in SSA form,
   * <dt>IN_SSA<dd>the CFG must be in SSA form,
   * <dt>VALID_SSA<dd>the CFG must be in valid SSA form, or
   * <dt>NA_SSA<dd>the CFG does not need to be in valid SSA form.
   * </dl>
   */
  public int requiresSSA()
  {
    return VALID_SSA;
  }


  /**
   * Convert the unroll status to a string for display.
   */
  public String getUnrollLoopKindString(int loopKind)
  {
    switch (loopKind) {
    case TOO_BIG_TO_UNROLL: return "too big";
    case NOT_UNROLLABLE:    return "n/a";
    case FLATTENED:         return "flattened";
    case FOR_UNROLLED:      return "for";
    case WHILE_UNROLLED:    return "while";
    case NO_LOOP_TAIL:      return "no loop tail";
    default:                return "??";
    }
  }
}
