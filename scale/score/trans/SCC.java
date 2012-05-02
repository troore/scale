package scale.score.trans;

import java.math.*;

import java.util.Iterator;
import java.util.Enumeration;

import scale.common.*;

import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.score.pred.References;
import scale.score.analyses.VirtualVar;

import scale.clef.LiteralMap;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.type.*;
import scale.clef.expr.Literal;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.BooleanLiteral;

/**
 * This class performs the <i>sparse conditional constant</i> propagation.
 * <p>
 * $Id: SCC.java,v 1.111 2007-10-04 19:58:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * See <cite>Constant Propagation with Conditional Branches</cite> by Mark N. 
 * Wegman and F. Kenneth Zadeck in ACM Transactions on Programming Languages 
 * and Systems, Vol 13, No. 2, April 1991, Pages 181-210.
 * <p>
 * This optimization attempts to do two things:
 * <ul>
 * <li>replace expressions with constants, and
 * <li>eliminate CFG edges that will never be executed.
 * </ul>
 * It it can be proved that an expression's value is always the same,
 * then that expression may be replaced by \a literal of that value.
 * If the predicate of a {@link scale.score.chords.DecisionChord
 * decision node} is a constant, then un-selected edges out of that
 * node may be eliminated.  If only one edge remains, the the decision
 * node may also be eliminated.
 * <p>
 * Unlike most optimizations this optimization never increases
 * register pressure.  Consequently, there is never a case where a
 * {@link scale.score.trans#legal legal} transformation is not also
 * {@link scale.score.trans#legal beneficial}.
 */
public class SCC extends Optimization
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

  private static int deadCFGNodeCount = 0; // A count of nodes removed because they were un-reachable.
  private static int propagationCount = 0; // The number of times constant propagation was performed.
  private static int newCFGNodeCount  = 0; // A count of new nodes created.
  private static int uselessDecisions = 0; // A count of the DecisionChords deleted.
  private static final String[] stats = {
    "deadCFGNodes",
    "propagations",
    "newCFGNodes",
    "uselessDecisions"};

  static
  {
    Statistics.register("scale.score.trans.SCC", stats);
  }

  /**
   * Return the number of dead nodes removed.
   */
  public static int deadCFGNodes()
  {
    return deadCFGNodeCount;
  }

  /**
   * Return the number of times copy propagation was performed.
   */
  public static int propagations()
  {
    return propagationCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * Return the number of DecisionChords deleted.
   */
  public static int uselessDecisions()
  {
    return uselessDecisions;
  }

  private Chord[] flowWorkListTo;
  private Chord[] flowWorkListFrom;
  private int[]   flowWorkListEdge;
  private int     fwlIndex;

  private Chord[]    ssaWorkListNode;
  private Expr[]     ssaWorkListExpr;
  private int        swlIndex;
  private int        chordCount; // How many nodes in the CFG.

  private HashSet<Chord>         phiChords; // Modified PhiExprChords
  private HashMap<Expr, Literal> lattices;
  private HashMap<Expr, Literal> cvMap;
  private Domination             dom;       // Domination information.

  /**
   * Perform <i>sparse conditional constant</i> propagation.
   * @param scribble the Scribble graph to be transformed
   */
  public SCC(Scribble scribble)
  {
    super(scribble, "_sc");
    assert setTrace(classTrace);
  }

  /**
   * Propagate constants through the Scribble graph.  Eliminate any
   * CFG nodes that are not executable because of the constant values
   * deterining the control flow.
   */
  public void perform()
  {
    phiChords = WorkArea.<Chord>getSet("perform SCC"); // Modified PhiExprChords

    BeginChord start = scribble.getBegin();

    resetEdges(start);

    cvMap     = new HashMap<Expr, Literal>(chordCount);
    lattices  = new HashMap<Expr, Literal>(chordCount);
    dom       = scribble.getDomination();

    doSCC(start);
    insertConstants();

    lattices  = null;
    cvMap     = null;

    Stack<Chord> dead = WorkArea.<Chord>getStack("perform SCC");
    findDeadCode(start, dead);
    removeUselessDecisions(start, dead);
    boolean modified = removeDeadCode(start, dead);
    WorkArea.<Chord>returnStack(dead);

    if (modified)
      scribble.recomputeDominators();

    if (rChanged)
      scribble.recomputeRefs();

    WorkArea.<Chord>returnSet(phiChords);
    phiChords = null;
  }

  /**
   * Mark all the CFG edges as being non-executable.  Generate the
   * initial Lattice value for each expression.  CFG edges are marked
   * true if they are used.  Phi expression edges are marked true if
   * they are no longer used.
   */
  private void resetEdges(Chord first)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("resetEdges");
    Chord.nextVisit();
    first.setVisited();
    wl.push(first);

    int cnt = 0;

    while (!wl.empty()) {
      Chord c = wl.pop();
      c.clearEdgeMarkers();
      c.pushOutCfgEdges(wl); // populate CFG edges
      cnt++;

      if (c.isPhiExpr()) {
        PhiExprChord pchd  = (PhiExprChord) c;
        PhiExpr      phi   = pchd.getPhiFunction();
        phi.clearEdgeMarkers();
      }
    }

    WorkArea.<Chord>returnStack(wl);

    if (cnt < 10)
      cnt = 10;
    chordCount = cnt;
  }

  /**
   * Add an edge (x -> y) to the flow work list.
   * @param x one end of the edge
   * @param y the other end of the edge
   */
  private void addToFWL(Chord x, Chord y, int edge)
  {
    assert assertTrace(trace, "  FWL push x ", x.getNodeID());
    assert assertTrace(trace, "           y ", y.getNodeID());
    assert ((x != null) && (y != null)) : "Invalid flow work list item.";

    if (fwlIndex >= flowWorkListFrom.length) {
      Chord[] nf = new Chord[fwlIndex + chordCount];
      Chord[] nt = new Chord[fwlIndex + chordCount];
      int[]   ne = new int[fwlIndex + chordCount];
      System.arraycopy(flowWorkListFrom, 0, nf, 0, flowWorkListFrom.length);
      System.arraycopy(flowWorkListTo,   0, nt, 0, flowWorkListFrom.length);
      System.arraycopy(flowWorkListEdge, 0, ne, 0, flowWorkListFrom.length);
      flowWorkListFrom = nf;
      flowWorkListTo   = nt;
      flowWorkListEdge = ne;
    }
    flowWorkListFrom[fwlIndex] = x;
    flowWorkListTo[fwlIndex]   = y; // Initialize work list to CFG edges from start node
    flowWorkListEdge[fwlIndex] = edge;
    fwlIndex++;
  }

  /**
   * Add an edge (x -> y) to the SSA work list.
   * @param x one end of the edge
   * @param y the other end of the edge
   */
  private void addToSWL(Chord x, Expr y)
  {
    assert assertTrace(trace, "  SWL push x ", x.getNodeID());
    assert assertTrace(trace, "           y ", y.getNodeID());

    assert ((x != null) && (y != null)) : "Invalid SSA work list item.";

    if (swlIndex >= ssaWorkListNode.length) {
      Chord[] nn = new Chord[swlIndex * 2];
      Expr[]  ne = new Expr[swlIndex  * 2];
      System.arraycopy(ssaWorkListNode, 0, nn, 0, ssaWorkListNode.length);
      System.arraycopy(ssaWorkListExpr, 0, ne, 0, ssaWorkListNode.length);
      ssaWorkListNode = nn;
      ssaWorkListExpr = ne;
    }
    ssaWorkListNode[swlIndex] = x;
    ssaWorkListExpr[swlIndex] = y;
    swlIndex++;
  }

  private Literal makeLiteral(Literal ra, Type t)
  {
    if ((ra == Lattice.Bot) || (ra == Lattice.Top))
      return null;

    if (ra.getCoreType() != t.getCoreType()) {
      if (ra instanceof IntLiteral)
        ra = LiteralMap.put(((IntLiteral) ra).getLongValue(), t);
      else if (ra instanceof BooleanLiteral)
        ra = LiteralMap.put(((BooleanLiteral) ra).isZero() ? 0 : 1, t);
      else if (ra instanceof FloatLiteral)
        ra = LiteralMap.put(((FloatLiteral) ra).getDoubleValue(), t);
      else
        return null;
    }

    return ra;
  }

  private void doSCC(Chord start)
  {
    boolean flg = true;

    flowWorkListTo   = new Chord[chordCount];
    flowWorkListFrom = new Chord[chordCount];
    flowWorkListEdge = new int[chordCount];
    fwlIndex         = 0;
    ssaWorkListNode  = new Chord[chordCount * 4];
    ssaWorkListExpr  = new Expr[chordCount * 4];
    swlIndex         = 0;

    addToFWL(start, start.getNextChord(), 0); // Initialize work list to CFG edges from start node

    Chord.nextVisit();

    while (flg) {
      flg = false;

      if (fwlIndex > 0) { // process an item from the work flow list
        fwlIndex--;
        Chord y    = flowWorkListTo[fwlIndex];
        Chord from = flowWorkListFrom[fwlIndex];
        int   edge = flowWorkListEdge[fwlIndex];

        flg = true;

        if (!from.edgeMarked(edge)) {
          from.markEdge(edge);
          if (y.isPhiExpr()) {
            PhiExprChord sy = (PhiExprChord) y;
            PhiExpr      so = sy.getPhiFunction();
            processPhi(so, y);
          }
          if (!y.visited()) {
            y.setVisited();
            processChord(y);
          }
          if (y.numOutCfgEdges() == 1) {
            Chord nxt = y.getNextChord();
            if (nxt != null)
              addToFWL(y, nxt, 0);
          }
        }
      }

      if (swlIndex <= 0)
        continue;

      // process an item from the SSA flow list

      swlIndex--;
      Expr  use = ssaWorkListExpr[swlIndex];
      Chord x   = ssaWorkListNode[swlIndex];

      flg = true;
      Chord n = use.getChord();

      assert assertTrace(trace, "SWL from ", x);
      assert assertTrace(trace, "    to   ", use);
      assert assertTrace(trace, "         ", n);

      if (n == null)
        continue;

      if (use instanceof PhiExpr) {
        processPhi((PhiExpr) use, n);
        continue;
      }

      int l = n.numInCfgEdges();
      for (int i = 0; i < l; i++) {
        Chord p    = n.getInCfgEdge(i);
        int   edge = p.indexOfOutCfgEdge(n, countOccurrances(n, p, i));
        if (p.edgeMarked(edge)) {
          processChord(n);
          break;
        }
      }
    }

    flowWorkListTo   = null;
    flowWorkListFrom = null;
    flowWorkListEdge = null;
    ssaWorkListNode  = null;
    ssaWorkListExpr  = null;
  }

  private int countOccurrances(Chord n, Chord to, int edge)
  {
    int skip = 0;
    for (int j = 0; j < edge; j++) {
      Chord from = n.getInCfgEdge(j);
      if (from == to)
        skip++;
    }
    return skip;
  }

  private void insertConstants()
  {
    Stack<Note> wl = WorkArea.<Note>getStack("insertConstants");

    // Actually propagate the constants that were found.

    Enumeration<Expr> el = lattices.keys();
    while (el.hasMoreElements()) {
      Expr exp = el.nextElement();

      if (exp instanceof PhiExpr)
        continue; // else the resultant program is wrong.

      if (exp.isLiteralExpr())
        continue;

      Literal val = getLatticeValue(exp);
      if (val == Lattice.Top)
        continue;

      Literal lit = makeLiteral(val, exp.getCoreType());
      if (lit == null)
        continue; // The expression is not constant.

      if (exp instanceof LoadExpr) {
        // Free up some memory once we are sure exp will be removed.
        LoadExpr le = (LoadExpr) exp;
        le.removeUseDef();
      }

      Chord s = exp.getChord();
      if (s == null) // Avoid doing un-necessary replacement and update.
        continue;

      Note n = exp.getOutDataEdge();

      rChanged = true;
      propagationCount++;

      if (n instanceof ConditionalExpr) {
        ConditionalExpr ce = (ConditionalExpr) n;
        if (exp == ce.getTest()) {
          n = ce.getOutDataEdge();
          exp = ce;

          Expr texp = ce.getTrueExpr();
          Expr fexp = ce.getFalseExpr();
          if (lit.isZero()) {
            ce.setRA(null);
            n.changeInDataEdge(exp, fexp);
          } else {
            ce.setMA(null);
            n.changeInDataEdge(exp, texp);
          }

          wl.push(n);
          exp.unlinkExpression();
          continue;
        }
      }

      n.changeInDataEdge(exp, new LiteralExpr(lit));
      wl.push(n);

      exp.unlinkExpression();
    }

    // Replacing expressions by constants may allow further reduction
    // of the expression.  This can't be done earlier because it might
    // invalidate a mapping in lattices.

    while (!wl.empty()) {
      Note n = wl.pop();
      if (n.getChord() == null)
        continue;

      while (n instanceof Expr) {
        Expr oe = (Expr) n;
        Expr ne = oe.reduce();
        n = oe.getOutDataEdge();
        if (ne != oe) {
          n.changeInDataEdge(oe, ne);
          oe.unlinkExpression();
        }
      }
    }

    WorkArea.<Note>returnStack(wl);
  }

  /**
   * When a branch of a CFG will be removed, all the corresponding
   * arguments in the phi functions have to be removed.  This routine
   * marks those arguments.
   * @param start is the last node in the dead CFG branch
   * @param fibb is the first node in the basic block that is the
   * successor of start
   * @param ots is a one time switch set to true to ignore the first
   * edge that goes from start to fibb
   */
  private void markPhiEdgeNotUsed(Chord start, Chord fibb, boolean ots)
  {
    int     ne  = fibb.numInCfgEdges();
    boolean flg = true;

    for (int edge = 0; edge < ne; edge++) {
      Chord in = fibb.getInCfgEdge(edge);

      if (in != start) 
        continue;

      flg = false;

      if (ots) {
        ots = false;
        continue;
      }

      Chord out = fibb;
      do {
        Chord next = null;
        if (!out.isLastInBasicBlock())
          next = out.getNextChord();

        if (out.isPhiExpr()) {
          PhiExprChord pchd = (PhiExprChord) out;
          PhiExpr      phi  = pchd.getPhiFunction();
          phi.markEdge(edge); // Mark the appropriate operand of the Phi function.
          phiChords.add(pchd);
        }
        out = next;
      } while (out != null);
    }

    if (flg)
      throw new scale.common.InternalError("Not in-coming edge " + start + " of " + fibb);
  }

  /**
   * When a branch of a CFG will not be removed, all the corresponding
   * arguments in the phi functions have to be mark used.  This
   * routine marks those arguments.
   * @param start is the last node in the dead CFG branch
   * @param fibb is the first node in the basic block that is the
   * successor of start
   * @param ots is a one time switch set to true to ignore the first
   * edge that goes from start to fibb
   */
  private void markPhiEdgeUsed(Chord start, Chord fibb, boolean ots)
  {
    int     ne  = fibb.numInCfgEdges();
    boolean flg = true;

    for (int edge = 0; edge < ne; edge++) {
      Chord in = fibb.getInCfgEdge(edge);

      if (in != start) 
        continue;

      flg = false;

      if (ots) {
        ots = false;
        continue;
      }

      Chord out = fibb;
      do {
        Chord next = null;
        if (!out.isLastInBasicBlock())
          next = out.getNextChord();

        if (out.isPhiExpr()) {
          PhiExprChord pchd = (PhiExprChord) out;
          PhiExpr      phi  = pchd.getPhiFunction();
          phi.clearEdge(edge); // Mark the appropriate operand of the Phi function.
          phiChords.add(pchd);
        }
        out = next;
      } while (out != null);
    }

    if (flg)
      throw new scale.common.InternalError("Not in-coming edge " + start + " of " + fibb);
  }

  /**
   * Find the CFG nodes that are known to be dead.
   * @param first CFG node
   */
  private void findDeadCode(Chord first, Stack<Chord> dead)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("findDeadCode");

    Chord.nextVisit();
    first.setVisited();
    wl.push(first);

    while (!wl.empty()) {
      Chord   c     = wl.pop();
      int     n     = c.numInCfgEdges();
      boolean alive = (c instanceof BeginChord); // The BeginChord is always alive

      c.pushOutCfgEdges(wl); // Check my children

      if (!alive) {
        int l = c.numInCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord x    = c.getInCfgEdge(i);
          int   edge = x.indexOfOutCfgEdge(c, countOccurrances(c, x, i));

          if (x.edgeMarked(edge)) {
            alive = true;
            break;
          }
        }

        if (!alive) { // None of this nodes in-coming CFG edges were live.
          if (c.isLastInBasicBlock()) {
            int ll = c.numOutCfgEdges();
            for (int i = 0; i < ll; i++)
              markPhiEdgeNotUsed(c, c.getOutCfgEdge(i), false);
          }
          dead.push(c);
        }
      }

      c.setLabel(alive ? 1 : 0);
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Find and remove DecisionChords that have only one live out-going
   * CFG edge.
   * @param first is the first CFG node
   */
  private void removeUselessDecisions(Chord first, Stack<Chord> dead)
  {
    Stack<Chord> wl   = WorkArea.<Chord>getStack("removeUselessDecisions");
    Chord[]       outs = new Chord[10];

    Chord.nextVisit();
    first.setVisited();
    wl.push(first);

    while (!wl.empty()) {
      Chord start = wl.pop();

      start.pushOutCfgEdges(wl);

      if (!start.isBranch())
        continue;

      if (0 == start.getLabel()) // This Chord is dead
        continue;

      DecisionChord dc = (DecisionChord) start;
      int           ks = dc.numOutCfgEdges();
      int           k  = 0;

      if (ks > outs.length)
        outs = new Chord[ks];

      // Find all the live out-going edges.

      for (int i = 0; i < ks; i++) { // Collect all live edges
        Chord ce = dc.getOutCfgEdge(i);

        if (dc.edgeMarked(i)) {
          outs[k++] = ce;
          continue;
        }

        markPhiEdgeNotUsed(dc, ce, false);
      }

      if (k < 1) // Chord is dead
        continue;

      Chord edge = outs[0];
      int   j;
      for (j = 1; j < k; j++) {
        Chord n = outs[j];
        if (n != edge) // Live edges are different
          break;
      }

      if (j < k) // If the live edges are different.
        continue;

      if ((j > 1) && edge.isPhiExpr())
        // After phi-removal, the edges may be different again.
        continue;

      // Fix up Phi functions in the following basic block

      markPhiEdgeNotUsed(dc, edge, true);

      // Replace the DecisionChord with a NullChord.  We can't use
      // removeFromCfg because there may be more than one out edge.
      // We can't use expungeFromCfg because it doesn't maintain the
      // CFG.  However, we do know that all "valid" out-going CFG
      // edges are the same.

      // Insert NullChord because the DecisionChord may have more than
      // one in-coming CFG edge.  This preserves the number of
      // in-coming CFG edges to the children of the DecisionChord.

      NullChord dummy = new NullChord();
      dummy.setTargetUnsafe(edge);
      boolean ots = true;
      for (int i = ks - 1; i >= 0; i--) {
        Chord fibb = dc.getOutCfgEdge(i);
        if (fibb == edge) {
          dc.replaceOutCfgEdge(fibb, null);
          if (ots)
            edge.replaceInCfgEdge(dc, dummy);
          else
            edge.deleteInCfgEdge(dc);
          ots = false;
        }
      }

      // Get the indexes first and put them in an array before
      // making any changes that might affect the dc's in-edges

      int   l       = dc.numInCfgEdges();
      int[] indexes = new int[l];
      for (int i = 0; i < l; i++) {
        Chord to = dc.getInCfgEdge(i);
        indexes[i] = to.indexOfOutCfgEdge(dc, countOccurrances(dc, to, i));
      }

      for (int i = l - 1; i >= 0; i--) {
        Chord   in = dc.getInCfgEdge(i);
        int     ie = indexes[i];
        boolean mk = in.edgeMarked(ie);

        in.changeOutCfgEdge(dc, dummy);
        if (!mk)
          in.clearEdge(ie);
        else
          in.markEdge(ie);
      }

      uselessDecisions++;
      dead.push(dc);
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Remove all of the dead CFG edges and all DecisionChords with only
   * one live out-going CFG edge.
   * @param start the first CFG node
   */
  private boolean removeDeadCode(Chord start, Stack<Chord> dead)
  {
    boolean  modified = false;
    Iterator<Chord> ep = phiChords.iterator();
    while (ep.hasNext()) {
      PhiExprChord pchd   = (PhiExprChord) ep.next();
      PhiExpr      phi    = pchd.getPhiFunction();
      int          na     = phi.numOperands();
      int          index  = -1;
      int          cnt    = 0; // count of active operands;

      for (int j = na - 1; j >= 0; j--) { // Count the number of alive operands.
        if (phi.edgeMarked(j)) {
          phi.removeOperand(j);
        } else
          cnt++;
      }

      if (cnt == 1) { // If only one, replace the Phi function.
        Expr    arg  = phi.getOperand(0);
        boolean doit = true;

        if (arg instanceof LoadDeclValueExpr) {
          // Avoid simple copies by doing the copy propagation now.
          LoadDeclValueExpr ldve = (LoadDeclValueExpr) arg;
          Declaration       d    = ldve.getDecl();
          if ((d != null) && !d.isVirtual()) {
            doit = false;
            ExprChord  s    = ldve.getUseDef();
            LoadExpr[] uses = pchd.getDefUseArray();
            for (int i = 0; i < uses.length; i++) {
              LoadExpr use = uses[i];
              use.setDecl(d);
              use.setUseDef(s);
            }
          }
        }

        if (doit) {
          phi.setOperand(null, 0);

          ExprChord  copyChord = new ExprChord(pchd.getLValue().copy(), arg);  // Replace PhiExprChord with ExprChord.
          LoadExpr[] uses      = pchd.getDefUseArray();
          for (int j = 0; j < uses.length; j++) {
            LoadExpr op = uses[j];
            op.setUseDef(copyChord);
          }
          newCFGNodeCount++;
          pchd.insertBeforeInCfg(copyChord);
          rChanged = true;
          newCFGNodeCount++;
        }
      }

      if (cnt <= 1) { // The Phi function is no longer needed.
        pchd.removeFromCfg();
        deadCFGNodeCount++;
        modified = true;
        rChanged = true;
      }
    }

    while (!dead.empty()) {
      Chord x = dead.pop();

      assert assertTrace(trace, "  Expunging ", x);

      if (x.isLoopHeader()) {
        // For various reasons, the exits may not have been marked for removal -
        // probably due to incorrect placement by the implicit loop logic.
        LoopHeaderChord lh = (LoopHeaderChord) x;
        int l = lh.numLoopExits();
        for (int i = 0; i < l; i++) {
          lh.getLoopExit(i).removeFromCfg();
        }
      }

      rChanged = true;
      x.expungeFromCfg();
      deadCFGNodeCount++;
      modified = true;
    }

    return modified;
  }

  /**
   * @param phi is a Phi function
   */
  private void processPhi(PhiExpr phi, Chord yPhi)
  {
    Literal v  = Lattice.Top;
    Chord   x  = yPhi.firstInBasicBlock();
    int     no = x.numInCfgEdges();

    assert (no == phi.numOperands()) : "Phi function does not conform to edges.";

    for (int i = 0; i < no; i++) {
      Expr  exp  = phi.getOperand(i);
      Chord c    = x.getInCfgEdge(i);
      int   edge = c.indexOfOutCfgEdge(x, countOccurrances(x, c, i));
      if (c.edgeMarked(edge))
        v = Lattice.meet(v, exp.getConstantValue(cvMap));
    }
    lattices.put(phi, v);
  }

  private void putLatticeValue(Expr exp, Literal oldVal, Literal newVal)
  {
    if (oldVal == newVal)
      return;
    lattices.put(exp, newVal);
  }

  private Literal getLatticeValue(Expr exp)
  {
    Literal o = lattices.get(exp);
    if (o == null)
      o = Lattice.Top;
    return o;
  }

  private void processExpr(Expr exp)
  {
    Literal v = exp.getConstantValue(cvMap);
    Literal o = getLatticeValue(exp);

    if ((v == Lattice.Top) || (v == Lattice.Bot)) {
      int l = exp.numInDataEdges();
      for (int i = 0; i < l; i++)
        processExpr(exp.getInDataEdge(i));
    }

    if (v != o) {
      Literal meet = Lattice.meet(o, v);
      putLatticeValue(exp, o, meet);
    }
  }

  private Literal processExpr2(Expr exp)
  {
    Literal v = exp.getConstantValue(cvMap);

    if ((v == Lattice.Top) || (v == Lattice.Bot)) {
      int l = exp.numInDataEdges();
      for (int i = 0; i < l; i++)
        processExpr(exp.getInDataEdge(i));
    }

    return v;
  }

  /**
   * Find the constant values in a CFG node and determine
   * what other nodes need to be checked again.
   * @param c is the Chord from which the expressions are obtained
   */
  private void processChord(Chord c)
  {
    if (c.isExprChord()) {
      ExprChord sY  = (ExprChord) c;
      Expr      exp = sY.getRValue();

      if (exp == null)
        return;

      Literal o = getLatticeValue(exp);
      Literal v = processExpr2(exp);

      if (v != Lattice.Top) {
        if (v == o)
          return;
        if (v.equals(o))
          return;
      }

      putLatticeValue(exp, o, v);

      if (sY.isAssignChord()) {
        processExpr(sY.getLValue());

        int ndu = sY.numDefUseLinks();
        for (int id = 0; id < ndu; id++) {
          LoadExpr use = sY.getDefUse(id);
          addToSWL(sY, use);
        }
      }
      return;
    }

    if (c.isBranch()) {
      DecisionChord dY  = (DecisionChord) c;
      Expr          exp = dY.getPredicateExpr();
      Literal       o   = getLatticeValue(exp);
      Literal       v   = processExpr2(exp);

      if (v != Lattice.Top) {
        if (v == o)
          return;
        if (v.equals(o))
          return;
      }

      putLatticeValue(exp, o, v);

      if ((v == Lattice.Bot) || (v == Lattice.Top)) {
        propagateEqualities(dY);
        int len = dY.numOutCfgEdges();
        for (int i = 0; i < len; i++) {
          Chord child = dY.getOutCfgEdge(i);
          addToFWL(dY, child, i);
        }
        return;
      }

      int   edge = dY.getBranchEdgeIndex(v);
      Chord n    = dY.getOutCfgEdge(edge);
      int   l    = dY.numOutCfgEdges();

      for (int i = 0; i < l; i++) {
        Chord ce = dY.getOutCfgEdge(i);
        if (ce != n) { // Edge is dead.
          Chord last = ce;

          while (!last.isLoopExit() && !last.isLastInBasicBlock())
            last = last.getNextChord();

          // Don't remove the only path to a loop exit.

          if (!last.isLoopExit() || (last.numInCfgEdges() > 1)) {
            boolean reinstate = false;

            // Don't remove the only path to the EndChord.

            while (last.getNextChord() != null)
              last = last.getNextChord().lastInBasicBlock();

            if (last == scribble.getEnd())
              reinstate = true;

            if (!reinstate) // Edge is dead and unneeded.
              continue;
          }
        }

        // Mark the path as used.

        addToFWL(dY, ce, i);
      }

      return;
    }

    int l = c.numInDataEdges();
    for (int i = 0; i < l; i++)
      processExpr(c.getInDataEdge(i));
  }

  /**
   * Try to propagate more constants through equalities.
   * For example, in
   * <pre>
   *   if (var == 6) { block }
   * </pre>
   * we know that var is 6 in <code>block</code>.  This idea came from
   * <cite>Using Conditional Branches to Improve Constant
   * Propagaation</cite> by Briggs, et al, Center for Research on
   * Parallel Computation, Rice University, Report CRPC-TR95533.
   */
  private void propagateEqualities(DecisionChord dc)
  {
    Expr exp  = dc.getPredicateExpr();
    Type type = exp.getType();

    if (!type.isAtomicType())
      return;

    if (dc instanceof IfThenElseChord) {
      doIfThenElse((IfThenElseChord) dc, exp, true);
      return;
    }

    if (!(dc instanceof SwitchChord))
      return;

    if (!(exp instanceof LoadDeclValueExpr))
      return;

    LoadDeclValueExpr ldve     = (LoadDeclValueExpr) exp;
    SwitchChord       sw       = (SwitchChord) dc;
    long[]            keys     = sw.getBranchEdgeKeyArray();
    Chord[]           outEdges = sw.getOutCfgEdgeArray(); 
    int               def      = sw.getDefaultIndex();
    for (int i = 0; i < outEdges.length; i++) {
      if (i == def)
        continue;

      doEdge(ldve, LiteralMap.put(keys[i], type), outEdges[i]);
    }
  }

  private void doIfThenElse(IfThenElseChord ifc, Expr pred, boolean neAllowed)
  {
    if (pred instanceof EqualityExpr)
      doMatchExpr((EqualityExpr) pred, ifc.getTrueCfgEdge());
    else if (neAllowed && (pred instanceof NotEqualExpr))
      doMatchExpr((NotEqualExpr) pred, ifc.getFalseCfgEdge());
    else if (pred instanceof AndExpr) {
      AndExpr ac = (AndExpr) pred;
      Expr    la = ac.getLeftArg();
      Expr    ra = ac.getRightArg();
      doIfThenElse(ifc, la, false);
      doIfThenElse(ifc, ra, false);
    }
  }

  private void doMatchExpr(MatchExpr pred, Chord edge)
  {
    Expr la = pred.getLeftArg();
    Expr ra = pred.getRightArg();
    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
    }

    if (!(la instanceof LoadDeclValueExpr))
      return;

    if (!ra.isLiteralExpr())
      return;

    Literal lit = ((LiteralExpr) ra).getLiteral();
    Type    lat = la.getCoreType();
    if (lit.isZero() && lit.getCoreType().isRealType() && !fpReorder)
      return; // -0.0 and 0.0 are not the same but they are equal.

    if (lit.getCoreType() != lat) {
      if (lit instanceof IntLiteral)
        lit = LiteralMap.put(((IntLiteral) lit).getLongValue(), lat);
      else if (lit instanceof FloatLiteral)
        lit = LiteralMap.put(((FloatLiteral) lit).getDoubleValue(), lat);
      else
        return;
    }

    doEdge((LoadDeclValueExpr) la, lit, edge);
  }

  /**
   * Replace all occurances, of the variable, that are specified by
   * <code>ldve</code> instance and are dominated by the
   * <code>edge</code> CFG node, with the specified
   * <code>value</code>.  We follow the def-use links to make sure we
   * only change those references to the variable that have to have
   * the same value as the specified reference (<code>ldve</code>).
   */
  private void doEdge(LoadDeclValueExpr ldve, Literal value, Chord edge)
  {
    if (edge.numInCfgEdges() > 1) // One edge uses the value, the other doesn't.
      return;

    ExprChord se = ldve.getUseDef();
    if (se == null)
      return;

    Vector<Chord> d = dom.getIterativeDomination(edge);
    d.add(edge);

    // We can safely change every use that is dominated by this edge.

    int n = se.numDefUseLinks();
    for (int i = n - 1; i >= 0; i--) {
      LoadExpr use = se.getDefUse(i);
      if (!(use instanceof LoadDeclValueExpr))
        continue;

      Chord uc = use.getChord();
      if (uc == null)
        continue;

      if (!d.contains(uc))
        continue;

      Note out = use.getOutDataEdge();
      out.changeInDataEdge(use, new LiteralExpr(value));
      use.unlinkExpression();
      propagationCount++;
      rChanged = true;
    }

    return;
  }
}
