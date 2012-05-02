package scale.score.trans;

import java.util.Iterator;

import scale.common.*;
import scale.clef.decl.*;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.type.*;
import scale.clef.LiteralMap;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.score.pred.References;

/**
 * This transformation moves CFG nodes to the outer-most basic block in which
 * they may be executed correctly.
 * <p>
 * $Id: LICM.java,v 1.79 2007-02-28 18:00:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Using the SSA use-def links, CFG nodes are moved into the
 * outer-most basic block in which their dependencies are available.
 * In order to preserve the program semantics, only CFG nodes
 * containing stores into temporary variables are moved.
 * <p>
 * The algorithm does a depth first visit to all the CFG nodes.  The
 * nodes in each basic block are numbered with the basic blocks
 * number.  The basic block number starts at 0 and is incremented each
 * time a new basic block is encountered.  Because the CFG is in SSA
 * form, we know that the definition for every dependency has already
 * been encountered when we reach any particular CFG node.  (We
 * exclude Phi functions.)  Consequently, we can simply compare the
 * current basic block number to the basic block number of the
 * defining CFG node for every dependency.  If it is different, then
 * the CFG node is moved to the end of the basic block with the
 * highest number containing the definition of a dependency that is at
 * the same or higher loop nesting depth.  This results in loop
 * invariant code being moved out of loops and still preserves the SSA
 * form of the CFG.
 * <p>
 * This optimization will also convert a divide to a multiply by the
 * reciprocal if the divisor is loop invariant, the operands are
 * floating point, and the user has allowed operations with floating
 * point operands to be re-arranged.  The calculation of the
 * reciprocal is moved out of the loop.
 * <p>
 * It is possible, though unlikely, for a transformation to reduce
 * register pressure.  If the expression references more than one
 * variable that are referenced nowhere else in the scope of the moved
 * expression, register pressure will be reduced.  If there is more
 * than one variable that is referenced in other expressions, register
 * pressure will increase.
 * <p>
 * A transformation is {@link scale.score.trans#legal legal} if:
 * <ul>
 * <li>the expression is an {@link
 * scale.score.expr.Expr#optimizationCandidate optimization
 * candidate},
 * <li>the expression does not contain a function call,
 * <li>the expression does not reference a global variable,
 * <li>the expression is not <code>va_arg()</code>, and
 * <li>the expression is loop invariant.
 * </ul>
 * It is {@link scale.score.trans#legal beneficial} if:
 * <ul>
 * <li>the cost of evaluating the expression is greater than
 * <code>minimumExecutionCost</code>,
 * <li>the expression is not the add of an address and a constant, and
 * <li>the size of the loop is less than a heuristic based upon the
 * number of uses of the expression and <code>maxLoopSize</code>.
 * </ul>
 */

public class LICM extends Optimization
{
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
   * Maximum loop size allowed for a single use of a loop invariant
   * expression.
   */
  public static int maxLoopSize = 40;

  private static int newCFGNodeCount   = 0; // A count of new nodes created.
  private static int movedExprsCount   = 0; // A count of expressions moved.
  private static int movedNAExprsCount = 0; // A count of expressions moved.
  private static int funnyLoopCount    = 0; // A count of the number of non-standard loop structures.
  private static int reuseCount        = 0; // A count of expressions re-used.

  private static final String[] stats  = {
    "newCFGNodes",
    "movedExprs",
    "movedNAExprs",
    "funnyLoops",
    "reusedExpr"
  };

  static
  {
    Statistics.register("scale.score.trans.LICM", stats);
  }

  /**
   * Return the number of expressions moved.
   */
  public static int movedExprs()
  {
    return movedExprsCount;
  }

  /**
   * Return the number of expressions moved.
   */
  public static int movedNAExprs()
  {
    return movedNAExprsCount;
  }

  /**
   * Return the number of non-standard loop structures encountered.
   */
  public static int funnyLoops()
  {
    return funnyLoopCount;
  }

  /**
   * Return the number of expressions re-used.
   */
  public static int reusedExpr()
  {
    return reuseCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  private Chord[]           bbMap;    // Map from basic block number to last node in the basic block.
  private int[]             bbMove;   // Map from basic block number to the basic block to move things to.
  private int[]             bbDepth;  // Map from basic block number to the loop depth for the basic block.
  private LoopHeaderChord[] bbInner;  // Map from basic block number to inner-most loop flag.
  private int               bbNumber; // Current basic block number.

  /**
   * Move the LOOP INVARIANT nodes of the CFG.
   * @param scribble the CFG
   */
  public LICM(Scribble scribble)
  {
    super(scribble, "_li");
  }

  /**
   * Perform LoopHeaderChord Invariant Code Motion.  This modifies the
   * CFG by moving loop invariant expressions out of the loop.  The
   * value of the expression is saved in a temporary and that
   * temporary is used inside of the loop.  Some expressions are only
   * moved if they are used in the inner-most loop so that register
   * pressure is not un-duly increased.
   */
  public void perform()
  {
    LoopHeaderChord start = (LoopHeaderChord) scribble.getBegin();
    if (start.numInnerLoops() <= 0)
      return; // No loops.

    bbNumber = 0;                        // Current basic block number.
    bbMap    = new Chord[100];           // Map from basic block number to last node in the basic block.
    bbMove   = new int[100];             // Map from basic block number to the basic block to move things to.
    bbDepth  = new int[100];             // Map from basic block number to the loop depth for the basic block.
    bbInner  = new LoopHeaderChord[100]; // Map from basic block number to inner-most loop flag.

    Chord.nextVisit();
    recurseLoops(start, 0);

    HashMap<Expr, Chord>    lMap     = new HashMap<Expr, Chord>(203); // Map from expression to critical CFG node.
    Table<Chord, ExprChord> prevMap  = new Table<Chord, ExprChord>(); // Map from move position to expressions moved there.
    Stack<Expr>             ewl      = WorkArea.<Expr>getStack("perform LICM"); // Expression work list.
    Stack<Chord>            wl       = WorkArea.<Chord>getStack("perform LICM");

    // The skipSubscripts flag is true if subscript address
    // expressions should not be moved.  Moving them creates synonyms
    // which causes problems with scalar replacement.

    boolean skipSubscripts = !scribble.scalarReplacementPerformed();

    Chord.nextVisit();
    start.setVisited();
    wl.push(start.getNextChord());

    // Scan for nodes and expressions that can be moved.

    while (!wl.empty()) { // Traverse the CFG
      Chord s = wl.pop();

      s.pushOutCfgEdges(wl);

      int cnum   = s.getLabel();
      int cDepth = bbDepth[cnum];
      if (cDepth <= 0)
        continue;

      boolean il = bbInner[cnum].isInnerMostLoop(); // True if this node is in an inner-most loop.
      if (!il)
        continue;

      int loopSize = bbInner[cnum].numChordsInLoop();

      // If the expression is not dependent on anything, we just move
      // it outside this loop.  If we moved it to block 0 it would
      // increase the register pressure too much.  Obtain the position
      // to use for independent expressions.

      Chord independent = bbMap[bbMove[s.getLabel()]];

      ewl.clear();

      int l = s.numInDataEdges();
      for (int i = 0; i < l; i++)
        ewl.push(s.getInDataEdge(i));

      // Attempt to move expressions out of the loop.

      while (!ewl.empty()) {
        Expr exp = ewl.pop();
        // Simplifying division expressions may result in pushing a
        // null on the stack.

        if (exp == null)
          continue;

        if (exp instanceof PhiExpr)
          continue;

        // Discard those things it is not worth it to move.

        if (exp.executionCostEstimate() < minimumExecutionCost)
          continue;

        if ((exp instanceof AdditionExpr) || (exp instanceof SubtractionExpr)) {
          BinaryExpr ae = (BinaryExpr) exp;
          Expr       ra = ae.getRightArg();
          Expr       la = ae.getLeftArg();
          if (ra.isLiteralExpr() &&
              (la.executionCostEstimate() < minimumExecutionCost))
            continue;
        }

        Type ct = exp.getCoreType();
        if (ct.isPointerType()) { // Try not to increase register pressure.
          // While the cost of the total expression may be high, just
          // computing an array element address, from adding an array
          // address and a constant, is better done in the loop.
          if (exp instanceof AdditionExpr) {
            if (((AdditionExpr) exp).getRightArg().isLiteralExpr()) {
              exp.pushOperands(ewl);
              continue;
            }
          } else if (exp instanceof ArrayIndexExpr) {
            ArrayIndexExpr aie = (ArrayIndexExpr) exp;
            if (aie.getOffset().isLiteralExpr() &&
                aie.getIndex().isLiteralExpr()) {
              exp.pushOperands(ewl);
              continue;
            }
          } else if (exp instanceof DualExpr) {
            DualExpr de   = (DualExpr) exp;
            Expr     high = de.getHigh();
            if (high instanceof SubscriptExpr) {
              Expr low = de.getLow();
              if (skipSubscripts) {
                de.getLow().pushOperands(ewl);
                continue;
              }
              ewl.push(low);
              continue;
            }
          }
        }

        // Discard those things it is too expensive to move.

        if (useHeuristics) {
          int mls = maxLoopSize;
          if ((s == exp.getOutDataEdge()) && s.isAssignChord()) {
            int ndu = ((ExprChord) s).numDefUseLinks() - 1;
            if (ndu < 0)
              ndu = 0;
            mls = (int) ((1.0 + (0.5 * ndu)) * mls);
          }
          if (loopSize > mls)
            continue;
        }

        // Discard those things it is not safe to move.

        if (!exp.optimizationCandidate()) {
          if (!fpReorder || !(exp instanceof DivisionExpr) ||
              !exp.getCoreType().isRealType()) {
            exp.pushOperands(ewl);
            continue;
          }

          // Try to move a divide outside of the loop and use a
          // multiply instead.

          DivisionExpr de = (DivisionExpr) exp;
          Expr         ra = de.getRightArg();

          int lab = getMovePoint(ra, independent, cnum, lMap);
          if (lab < 0) { // The divisor is loop variant.
            exp.pushOperands(ewl);
            continue;
          }

          // Move the divide outside of the loop and use a multiply by
          // the reciprocal instead.

          Expr la  = de.getLeftArg();
          Expr div = changeDivToMul(de);
          la.pushOperands(ewl);
          moveit(div, lab, s, prevMap);
          rChanged = true;
          continue;
        }

        if (exp instanceof LoadFieldAddressExpr) {
          if (((LoadFieldAddressExpr) exp).getField().getBits() != 0) {
            // Can't take the addrtess of a bit field.
            exp.pushOperands(ewl);
            continue;
          }
        }

        Note n = exp.getOutDataEdge();
        if (n instanceof VarArgExpr)
          continue;

        int lab = getMovePoint(exp, independent, cnum, lMap);
        if (lab < 0) { // The expression is loop variant.
          if (!fpReorder ||
              !(exp instanceof DivisionExpr) ||
              !exp.getCoreType().isRealType()) {
            exp.pushOperands(ewl);
            continue;
          }

          DivisionExpr de = (DivisionExpr) exp;
          Expr         ra = de.getRightArg();

          lab = getMovePoint(ra, independent, cnum, lMap);
          if (lab < 0) { // The divisor is loop variant.
            exp.pushOperands(ewl);
            continue;
          }

          // Move the divide outside of the loop and use a multiply by
          // the reciprocal instead.

          Expr la = de.getLeftArg();
          exp = changeDivToMul(de);
          la.pushOperands(ewl);
          rChanged = true;
        }

        // We have an expression that can be moved outside of a loop.

        moveit(exp, lab, s, prevMap);
      }
    }

    WorkArea.<Expr>returnStack(ewl);
    WorkArea.<Chord>returnStack(wl);

    if (dChanged)
      scribble.recomputeDominators();
    if (rChanged)
      scribble.recomputeRefs();
  }

  /**
   * Return the place that this expression can be moved to or -1 if none.
   * @param exp is the expression to check
   * @param independent specifies where to put it if it is loop-invariant
   * @param cnum specifies the basic block which contains the expression
   * @param lMap maps from expression to critical CFG node
   */
  private int getMovePoint(Expr exp, Chord independent, int cnum, HashMap<Expr, Chord> lMap)
  {
    int lab = exp.getCriticalChord(lMap, independent).getLabel();

    // If lab == 0, it means the expression is not dependent on anything.
    // So just move it outside this loop.  If we moved it to block 0
    // it would increase the register pressure too much.  Too bad we
    // don't know the outermost loop.

    if (lab > cnum)
      lab = cnum;

    // Check if the expression is loop independent.

    if ((lab >= cnum) ||
        (bbDepth[lab] >= bbDepth[cnum]) ||
        !exp.getCoreType().isAtomicType() ||
        (exp.sideEffects() >= Expr.SE_STATE)) {
      return -1; // Expression cannot be moved out of the loop.
    }

    // We have an expression that can be moved outside of a loop.

    return lab;
  }

  /**
   * Move an expression to the indicated location.
   * @param exp is the expression to move
   * @param lab specifies where to move it to
   * @param s is the CFG node containing the expression
   * @param prevMap is the set of moved expressions
   * @return true if the expression was moved.
   */
  private boolean moveit(Expr exp, int lab, Chord s, Table<Chord, ExprChord> prevMap)
  {
    if (exp == null)
      return false;

    Chord move = bbMap[lab];
    Note  n    = exp.getOutDataEdge();

//  if (exp.isLiteralExpr()) { // Move literals outside of this loop only.
//    if (exp.getCoreType().isRealType() && !Machine.currentMachine.hasCapability(Machine.HAS_NON_VOLATILE_FP_REGS))
//      continue;
//    move = bbMap[exp.getLoopHeader().getPreHeader().getLabel()];
//  }

    if (n instanceof ExprChord) {
      // Moving CFG nodes before doing PRE invalidates a PRE
      // assumption about SSA variable names.
      ExprChord ec = (ExprChord) n;
      if ((ec.getLValue() == null) &&    // Sets no value and
          (ec.getCall(false) == null) && // is not a function call
          !(ec instanceof ExitChord)) {  // which an ExitChord creates.
        // Move the CFG node itself.
        Expr ra = ec.getRValue();
        if (ra.isLiteralExpr()) {
          LiteralExpr le  = (LiteralExpr) ra;
          if (le.executionCostEstimate() <= minimumExecutionCost)
            return false;
        }

        if (ec.isFirstInBasicBlock()) {
          // Preserve the edges, the null will be removed later.
          ec.insertBeforeInCfg(new NullChord());
          newCFGNodeCount++;
        }

        ec.extractFromCfg();

        if (move.isSequential() && !move.isLoopPreHeader())
          move.insertAfterOutCfg(ec, move.getNextChord());
        else
          move.insertBeforeInCfg(ec);

        ec.setLabel(lab);
        movedExprsCount++;
        dChanged = true;
        return true;
      }
    }

    // Try to find an expression at the move point that is equivalent
    // and can be re-used.  This primarily happens when we move
    // LoadDeclAddressExpr instances for variables in memory.  This
    // results in fewer local variables which decreases register
    // pressure.

    ExprChord           prev = null;
    Iterator<ExprChord> ep   = prevMap.getRowEnumeration(move);
    while (ep.hasNext()) {
      ExprChord se = ep.next();
      if (se.getRValue().equivalent(exp)) {
        prev = se;
        break;
      }
    }

    if (prev == null) { // Can't reuse previous one.

      // Move an expression out of the loop.  Create a new variable
      // and store that expression's value into it.  Replace the
      // expression in the loop with a reference to that new variable.

      Type                et   = exp.getType().getNonAttributeType();
      VariableDecl        decl = genTemp(et);
      LoadDeclAddressExpr lhs  = new LoadDeclAddressExpr(decl);
      LoadDeclValueExpr   rhs  = new LoadDeclValueExpr(decl);

      n.changeInDataEdge(exp, rhs);

      ExprChord ec = new ExprChord(lhs, exp.addCast(et));

      rhs.setUseDef(ec);
      ec.setLabel(lab);
      ec.copySourceLine(s);

      if (exp instanceof LoadDeclAddressExpr) {
        VariableDecl vd = (VariableDecl) ((LoadDeclAddressExpr) exp).getDecl();
        vd.setAddressTaken();
      }

      if (move.isSequential() && !move.isLoopPreHeader())
        move.insertAfterOutCfg(ec, move.getNextChord());
      else
        move.insertBeforeInCfg(ec);

      prevMap.add(move, ec);
      dChanged = true;
    } else { // Use an existing expression created by LICM.
      // This decreases register pressure.

      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) prev.getLValue();
      VariableDecl        decl = (VariableDecl) ldae.getDecl();
      LoadDeclValueExpr   rhs  = new LoadDeclValueExpr(decl);
      Chord               ns   = n.getChord();

      n.changeInDataEdge(exp, rhs);
      rhs.setUseDef(prev);
      exp.unlinkExpression();
      reuseCount++;
    }

    rChanged = true;
    movedExprsCount++;

    if (exp instanceof LoadDeclAddressExpr)
      return true;

    if (exp instanceof ConversionExpr) {
      Expr op = exp.getOperand(0);
      if (op instanceof LoadDeclAddressExpr)
        return true;
    }

    movedNAExprsCount++;
    return true;
  }

  /**
   * Change a divide into a multiply by the reciprocal.
   * <pre>
   *   a / b <=> a * (1 / b)
   * </pre>
   * Return the expression that computes the reciprocal.
   */
  private Expr changeDivToMul(DivisionExpr exp)
  {
    Expr la  = exp.getLeftArg();
    Expr ra  = exp.getRightArg();
    Type rat = ra.getType();

    exp.setLeftArg(null);
    exp.setRightArg(null);

    Expr one = new LiteralExpr(LiteralMap.put(1.0, rat));
    Expr div = DivisionExpr.create(rat, one, ra);
    Expr mul = MultiplicationExpr.create(exp.getType(), la, div);

    exp.getOutDataEdge().changeInDataEdge(exp, mul);
    exp.unlinkExpression();

    // If the expression has been reduced further to the point that
    // the divide disappears, there is nothing to move outside of the
    // loop.

    return (div.getOutDataEdge() == mul) ? div : null;
  }

  /**
   * Find the basic blocks and label the CFG nodes.
   * Determine the loop depth for each basic block and where
   * its loop invariants can be moved to.
   */
  private void recurseLoops(LoopHeaderChord lh, int curDepth)
  {
    Stack<Chord> wl  = WorkArea.<Chord>getStack("recurseLoops");
    int          bbn = bbNumber - 1;

    lh.setVisited();
    lh.pushOutCfgEdges(wl);
    lh.setLabel(bbn);


    // Label the CFG nodes with their basic block numbers.
    // Determine the positions to move loop invariants to.

    while (!wl.empty()) { // Traverse the CFG
      Chord s = wl.pop();

      s.setLabel(bbNumber);

      if (s.isLoopPreHeader()) {
        LoopHeaderChord nlh = (LoopHeaderChord) s.getNextChord();
        int l = nlh.numLoopExits();
        for (int i = 0; i < l; i++) {
          LoopExitChord exit = nlh.getLoopExit(i);
          if (!exit.visited()) {
            exit.setVisited();
            wl.push(exit);
          }
        }
        newBasicBlock(s, bbn, curDepth, lh);
        recurseLoops(nlh, curDepth + 1);
        continue;
      }

      int l1 = wl.size();
      s.pushOutCfgEdges(wl);
      int l2 = wl.size();

      if (s.isLastInBasicBlock() || (l1 == l2))
        newBasicBlock(s, bbn, curDepth, lh);
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Store the information for a new basic block.
   * @param s is the last node in the basic block
   * @param move specifies the basic block to which loop invariants
   * can be moved
   * @param depth is the loop depth of this basic block
   * @param il is true if this basic block is in an inner-most loop
   */
  private void newBasicBlock(Chord s, int move, int depth, LoopHeaderChord lh)
  {
    if (bbNumber >= bbMap.length) {
      Chord[] nm = new Chord[bbNumber + 100];
      System.arraycopy(bbMap, 0, nm, 0, bbMap.length);
      bbMap = nm;
      int[] nmo = new int[bbNumber + 100];
      System.arraycopy(bbMove, 0, nmo, 0, bbMove.length);
      bbMove = nmo;
      int[] ni = new int[bbNumber + 100];
      System.arraycopy(bbDepth, 0, ni, 0, bbDepth.length);
      bbDepth = ni;
      LoopHeaderChord[] nb = new LoopHeaderChord[bbNumber + 100];
      System.arraycopy(bbInner, 0, nb, 0, bbInner.length);
      bbInner = nb;
    }
    bbMap[bbNumber]   = s;
    bbMove[bbNumber]  = move;
    bbDepth[bbNumber] = depth;

    bbInner[bbNumber] = lh;
    bbNumber++;
  }
}
