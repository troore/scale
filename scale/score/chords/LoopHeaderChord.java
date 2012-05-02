package scale.score.chords;

import java.util.Enumeration;
import java.util.Iterator;

import scale.frontend.SourceLanguage;
import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.pred.*;
import scale.score.dependence.*;

import scale.annot.*;
import scale.clef.decl.*;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.type.*; 

/** 
 * This class is used to mark the start of a loop.
 * <p>
 * $Id: LoopHeaderChord.java,v 1.136 2007-10-29 13:39:26 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An instance of this class always has two and only two in-coming CFG
 * edges.  One is from a {@link LoopPreHeaderChord LoopPreHeaderChord}
 * and the other is from a {@link LoopTailChord LoopTailChord}.  No
 * code is generated for a LoopHeaderChord instance.
 * <p>
 * Don't assume that a LoopHeaderChord instance will always start a
 * basic block.  For example, a "do { } while (false);" will not have
 * a {@link LoopTailChord LoopTailChord} after SCC.
 * <p>
 * There may be one, none, or several {@link LoopExitChord
 * LoopExitChord} instances associated with a loop.
 * <p>
 * Below are two images of the loop structure used by Scale.  They
 * show the CFG before going into {@link scale.score.SSA SSA} mode.
 * Both were generated from:

 * <pre>
 * int sumfor(int n)
 * {
 *   int sum, i;
 *   sum = 0;
 *   for (i = 0; i < n; i++)
 *     sum += i;
 *   return sum;
 * }
 * </pre>
 * <table cols=2><thead><tr><th>Regular Loop</th><th>Loop Test at End</th></thead>
 * <tbody>
 * <tr><td><img src="../../../loop.jpg" alt="Regular Loop"></td>
 *     <td><img src="../../../looplte.jpg" alt="Loop Test at End"></td>
 * </tbody></table>
 * @see LoopPreHeaderChord
 * @see LoopTailChord
 * @see LoopExitChord
 * @see LoopInitChord
 */

public class LoopHeaderChord extends SequentialChord
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;


  // The primary induction variable for this loop. Usually, this is
  // the index variable that appears in the for/do loop header. If
  // this is not a for loop, this is the most-complete index detected
  // that is part of a conditional expression that may cause a jump
  // outside the loop.
  private InductionVar    primaryInductionVar; 

  private LoopHeaderChord parent;   // The outer loop containing this loop.
  private LoopTailChord   loopTail; // The LoopTailChord for the loop if known.
  private LoopInitChord   loopInit; // The LoopInitChord for the loop if known.
  private IfThenElseChord loopTest; // The loop test for the loop if known.

  private Object   children;  // Loops contained in this loop.
  private Object   loopExits; // The LoopExitChord or a vector of LoopExitChords for the loop if known.
  private DDGraph  ddGraph;   // The graph of dependence information.
  private Scribble scribble;

  private Vector<InductionVar>      inductionVars;    // LoopHeaderChord induction variables.
  private HashSet<Expr>             loopVariantExprs; // Set of expressions that are not loop invariant expressions.
  private HashMap<Expr, AffineExpr> isAffineMap;      // A mapping from the Expr to the AffineExpr.

  private int numChordsInLoop;  // The number of nodes in this loop including the subloops.
  private int profEntryCnt;     // The number of times the loop was entered - supplied by profiling.
  private int profIterationCnt; // The number of times the loop was iterated - supplied by profiling.
  private int loopNumber;       // The number of the loop in this CFG.
  private int unrollFactor;     // Programmer specified unroll factor.

  private boolean liComplete;            // True if all loop information for this loop nest was found.
  private boolean ddComplete;            // True if all data dependence information for this loop nest was found.
  private boolean numChordsInLoopValid;  // True if the number of Chords in the loop is known.
  private boolean boundsAndStepValid;    // True if the bounds and step have been determined for this loop.
  private boolean loopContainsCallValid; // True if loopContainsCall is valid.
  private boolean loopContainsCall;      // True if some expression in the loop may result in a call to a subroutine.
  private boolean forwardSubstituteDone;
  private boolean inhibitLoopPermute;    // True if the loop should not be interchanged with other loops.
  private boolean trace;

  /**
   * @param parent the parent loop of this loop or <code>null</code>
   * @param next is the successor CFG node
   */
  public LoopHeaderChord(Scribble scribble, LoopHeaderChord parent, Chord next)
  {
    super(next);

    this.scribble         = scribble;
    this.loopNumber       = scribble.getNextLoopNumber();
    this.profEntryCnt     = -1;
    this.profIterationCnt = -1;

    setParent(parent);
    assert setTrace();
  }

  private boolean setTrace()
  {
    trace = Debug.trace(scribble.getRoutineDecl().getName(), classTrace, 3);
    return true;
  }

  /**
   * @param parent the parent loop of this loop or <code>null</code>
   */
  public LoopHeaderChord(Scribble scribble, LoopHeaderChord parent)
  {
    this(scribble, parent, null);
  }

  /**
   * Make a copy of this loop header sans any data dependence information.
   */
  public Chord copy()
  {
    LoopHeaderChord lh = new LoopHeaderChord(scribble, null, getNextChord());

    lh.copySourceLine(this);

    lh.parent   = parent;
    lh.loopTail = loopTail;
    lh.loopInit = loopInit;
    lh.loopTest = loopTest;
    lh.loopNumber = scribble.getNextLoopNumber();

    if (loopExits == null)
      return lh;

    if (loopExits instanceof Vector)
      lh.loopExits = ((Vector) loopExits).clone();
    else
      lh.loopExits = loopExits;

    return lh;   
  }

  /**
   * Eliminate associated loop information.
   * All of the removed information is generated.
   */
  public void loopClean()
  {
    this.loopVariantExprs    = null;
    this.isAffineMap         = null;
    this.ddGraph             = null;

    this.ddComplete          = false;
    this.boundsAndStepValid  = false;
    this.forwardSubstituteDone = false;
  }

  /**
   * Return the parent loop of this loop.
   */
  public final LoopHeaderChord getParent()
  {
    return parent;
  }

  /**
   * Specify that <code>child</code> is a loop contained in this loop.
   */
  public final void addChildLoop(LoopHeaderChord child)
  {
    if (child == null)
      return;

    if (children == null) {
      children = child;
      return;
    }

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> v = (Vector<LoopHeaderChord>) children;
      v.addElement(child);
      return;
    }

    Vector<LoopHeaderChord> nc = new Vector<LoopHeaderChord>();
    nc.addElement((LoopHeaderChord) children);
    nc.addElement(child);
    children = nc;
  }

  /**
   * Specify that <code>child</code> is no longer a loop contained in
   * this loop.
   */
  public final void removeChildLoop(LoopHeaderChord child)
  {
    if (child == null)
      return;

    if (children == child) {
      children = null;
      return;
    }

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> c = (Vector<LoopHeaderChord>) children;
      if (c.removeElement(child)) {
        int l = c.size();
        if (l == 0)
          children = null;
        else if (l == 1)
          children = c.elementAt(0);
        return;
      }
    }

    throw new scale.common.InternalError("Not a child loop " + child);
  }

  /**
   * Return true if the specified loop is a child of this loop.
   */
  public boolean hasInnerLoop(LoopHeaderChord child)
  {
    if (children == child)
      return true;

    if (!(children instanceof Vector))
      return false;

    return ((Vector) children).contains(child);
  }

 /**
   * Return true if this loop is an inner-most loop.
   * It is an inner-most loop if it has no children loops.
   */
  public final boolean isInnerMostLoop()
  {
    return (children == null);
  }

  /**
   * Return the first child loop.
   */
  public final LoopHeaderChord getFirstChild()
  {
    if (children == null)
      return null;

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) children;
      return nc.elementAt(0);
    }

    return (LoopHeaderChord) children;
  }

  /**
   * Return a <code>Vector</code> of all of the loops
   * (<code>LoopHeaderChord</code> instances) contained in this loop.
   */
  public Vector<LoopHeaderChord> getInnerLoops()
  {
    if (children == null)
      return new Vector<LoopHeaderChord>(0);

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) children;
      int                     l  = nc.size();
      Vector<LoopHeaderChord> vl = new Vector<LoopHeaderChord>(l);
      for (int i = 0; i < l; i++) {
        LoopHeaderChord lh = nc.elementAt(i);
        vl.addElement(lh);
      }
      return vl;
    }

    Vector<LoopHeaderChord> vl = new Vector<LoopHeaderChord>(1);
    vl.addElement((LoopHeaderChord) children);

    return vl;
  }

  /**
   * Return the specified inner loop.
   */
  public LoopHeaderChord getInnerLoop(int i)
  {
    assert (children != null) : "No such inner loop - " + i;

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) children;
      return nc.elementAt(i);
    }

    assert (i == 0) : "No such inner loop - " + i;

    return (LoopHeaderChord) children;
  }

  /**
   * Add all innermost loops in this loop tree to the vector.
   * The {@link BeginChord BeginChord} instance is never added.
   */
  public final void getInnermostLoops(Vector<LoopHeaderChord> loops)
  {
    if (children == null) {
      if (isTrueLoop())
        loops.add(this);
      return;
    }

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) children;
      int                    l  = nc.size();
      for (int i = 0; i < l; i++) {
        LoopHeaderChord lh = nc.elementAt(i);
        lh.getInnermostLoops(loops);
      }
      return;
    }

    ((LoopHeaderChord) children).getInnermostLoops(loops);
  }

  /**
   * Return the number of the loops contained in this loop
   * (i.e., the number of immediate sub-loops).
   */
  public final int numInnerLoops()
  {
    if (children == null)
      return 0;

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) children;
      return nc.size();
    }

    return 1;
  }

  /**
   * Break any un-needed links from this <code>LoopHeaderChord</code>
   * instance that has been deleted.
   */
  public void unlinkChord()
  {
    if (parent != null)
      parent.removeChildLoop(this);

    if (loopExits == null)
      return;

    if (loopExits instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopExitChord> les = (Vector<LoopExitChord>) loopExits;
      int                   l   = les.size();
      for (int i = l - 1; i >= 0; i--)
        les.elementAt(i).setLoopHeader(null);
      loopExits = null;
      return;
    }

    LoopExitChord lec = (LoopExitChord) loopExits;
    lec.setLoopHeader(null);
    loopExits = null;
  }
  
  /**
   * Always return true, since we ignore the back edge when performing
   * a topological sort.
   */
  public final boolean parentsVisited()
  {
    assert(numInCfgEdges() <= 2);
    return true;
  }
  
  /**
   * Always return true, since we ignore the back edge when performing
   * a topological sort.
   */
  public final boolean parentsFinished(HashSet<Chord> finished)
  {
    assert(numInCfgEdges() <= 2);
    return true;
  }

  /**
   * Specify the enclosing loop of this loop.
   * @param parent new parent loop of this loop
   */
  public final void setParent(LoopHeaderChord parent)
  {
    if (this.parent != null)
      this.parent.removeChildLoop(this);

    this.parent = parent;
    if (parent != null)
      parent.addChildLoop(this);
  }

  /**
   * Specify a {@link scale.score.chords.LoopExitChord LoopExitChord}
   * instance associated with this loop.
   * @param loopExit is the {@link scale.score.chords.LoopExitChord
   * LoopExitChord} instance
   */
  public void addLoopExit(LoopExitChord loopExit)
  {
    if (loopExits == null) {
      loopExits = loopExit;
      return;
    }

    if (loopExits == loopExit)
      return;

    if (loopExits instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopExitChord> v = (Vector<LoopExitChord>) loopExits;
      if (!v.contains(loopExit))
        v.addElement(loopExit);
      return;
    }

    Vector<LoopExitChord> exits = new Vector<LoopExitChord>(2);
    exits.addElement((LoopExitChord) loopExits);
    exits.addElement(loopExit);
    loopExits = exits;
  }

  /**
   * Specify that {@link scale.score.chords.LoopExitChord
   * LoopExitChord} instance is no longer a loop exit for this loop.
   */
  public final void removeLoopExit(LoopExitChord loopExit)
  {
    if (loopExit == null)
      return;

    if (loopExits == loopExit) {
      loopExits = null;
      return;
    }

    if (loopExits instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopExitChord> c = (Vector<LoopExitChord>) loopExits;
      if (c.removeElement(loopExit)) {
        int l = c.size();
        if (l == 0)
          loopExits = null;
        else if (l == 1)
          loopExits = c.elementAt(0);
        return;
      }
    }

    throw new scale.common.InternalError("Not a loop exit " + loopExit + " of " + this);
  }

  /**
   * Return the first loop exit.
   */
  public final LoopExitChord getFirstExit()
  {
    if (loopExits == null)
      return null;

    if (loopExits instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopExitChord> v = (Vector<LoopExitChord>) loopExits;
      return v.elementAt(0);
    }

    return (LoopExitChord) loopExits;
  }

  /**
   * Return the specified loop exit.
   */
  public final LoopExitChord getLoopExit(int i)
  {
    assert (loopExits != null) : "No such exit " + i;

    if (loopExits instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopExitChord> v = (Vector<LoopExitChord>) loopExits;
      return v.elementAt(i);
    }

    assert (i == 0) : "No such exit " + i;

    return (LoopExitChord) loopExits;
  }

  /**
   * Return the number of loop exits.
   */
  public final int numLoopExits()
  {
    if (loopExits == null)
      return 0;

    if (loopExits instanceof Vector)
      return ((Vector) loopExits).size();

    return 1;
  }

  /**
   * Add the loop's exits to the set.
   */
  private void addLoopExits(HashSet<Chord> set)
  {
    if (loopExits == null)
      return;

    if (loopExits instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopExitChord> v = (Vector<LoopExitChord>) loopExits;
      int                   l = v.size();
      for (int i = 0; i < l; i++)
        set.add(v.elementAt(i));
      return;
    }

    set.add((Chord) loopExits);
  }

  /**
   * Return true if the specified loop exit is for this loop.
   */
  public boolean isLoopExit(LoopExitChord le)
  {
    if (le == loopExits)
      return true;

    if (loopExits instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopExitChord> v = (Vector<LoopExitChord>) loopExits;
      int                   l = v.size();
      for (int i = 0; i < l; i++)
        if (le == v.get(i))
          return true;
    }

    return false;
  }

  /**
   * Return the {@link scale.score.chords.LoopPreHeaderChord
   * LoopPreHeaderChord} instance for this loop.
   */
  public LoopPreHeaderChord getPreHeader()
  {
    int l = numInCfgEdges();
    if (l > 0) {
      Chord c = getInCfgEdge(0);
      if (c.isLoopPreHeader())
        return (LoopPreHeaderChord) c;
      if (l > 1) {
        c = getInCfgEdge(1);
        if (c.isLoopPreHeader())
          return (LoopPreHeaderChord) c;
      }
    }
    throw new scale.common.InternalError("Funny loop " + this);
  }

  public final void usePragma(PragmaStk.Pragma  pragma)
  {
    if (pragma == null)
      return;

    this.unrollFactor       = pragma.getValue(PragmaStk.UNROLL);
    this.inhibitLoopPermute = !pragma.isSet(PragmaStk.LOOP_PERMUTE);
  }

  /**
   * Return the requested unroll factor.  This value may be set via a
   * pragma or via profiling.
   */
  public final int getUnrollFactor()
  {
    return unrollFactor;
  }

  /**
   * Set the requested unroll factor.  This value may be set via a
   * pragma or via profiling.
   */
  public final void setUnrollFactor(int unrollFactor)
  {
    this.unrollFactor = unrollFactor;
  }

  public final boolean inhibitLoopPermute()
  {
    return inhibitLoopPermute;
  }

  /**
   * Specify the {@link scale.score.chords.LoopTailChord
   * LoopTailChord} instance associated with this loop.
   * @param loopTail the LoopTailChord
   */
  public void setLoopTail(LoopTailChord loopTail)
  {
    this.loopTail = loopTail;
  }

  /**
   * Return the {@link scale.score.chords.LoopTailChord LoopTailChord}
   * instance for this loop if known.
   */
  public LoopTailChord getLoopTail()
  {
    return loopTail;
  }

  /**
   * Specify the {@link scale.score.chords.LoopInitChord
   * LoopInitChord} instance associated with this loop.
   * @param loopInit the LoopInitChord
   */
  public void setLoopInit(LoopInitChord loopInit)
  {
    this.loopInit = loopInit;
  }

  /**
   * Return the {@link scale.score.chords.LoopInitChord LoopInitChord}
   * instance for this loop.
   */
  public LoopInitChord getLoopInit()
  {
    return loopInit;
  }

  /**
   * Specify the loop exit test associated with this loop.
   * @param loopTest the loop test Chord
   */
  public void setLoopTest(IfThenElseChord loopTest)
  {
    this.loopTest = loopTest;
  }

  /**
   * Return the loop test Chord for this loop or <code>null</code> if
   * not known.  There may be multiple loop exit tests.  At most one
   * of these tests will be known.
   */
  public IfThenElseChord getLoopTest()
  {
    return loopTest;
  }

  /**
   * Return the <code>LoopHeaderChord</code> instance associated with
   * this loop exit.
   */
  public LoopHeaderChord getLoopHeader()
  {
    return this;
  }

  /**
   * Return true if this is CFG node was added for the convenience of
   * the compiler and does not correspond to actual source code in the
   * user program.
   */
  public boolean isSpecial()
  {
    return true;
  }

  /**
   * Return the number of in-coming data edges.
   */
  public int numInDataEdges()
  {
   return 0;
  }

  public void visit(Predicate p)
  {
    p.visitLoopHeaderChord(this);
  }

  /**
   * Return the nest level for a loop.  A value of 0 indicates an
   * entry node.  A value of 1 means the outermost loop. The nest
   * level increases for inner loops.
   */
   public final int getNestedLevel()
  {
    int             depth = 0;
    LoopHeaderChord cur   = parent;
    while (cur != null) {
      cur = cur.parent;
      depth++;
    }
    return depth;
  }

  /**
   * Return true if this is a perfectly nested loop.  Assuming the
   * loop exit test is at the beginning of the loop, a perfectly
   * nested loop is a tightly nested loop that has no nodes, other
   * than Phi functions, between the LoopHeaderChord &amp; the loop
   * exit test, between the loop exit test and the inner loop, and
   * between the inner loop exit and the loop tail.  This method is
   * correct for loops where the loop exit test is at the end of the
   * loop.
   * @see #isTightlyNested
   */
  public boolean isPerfectlyNested()
  {
    if (parent == null)
      return false;

    LoopHeaderChord lh = this;
    while (lh != null) {
      if (lh.numLoopExits() != 1)
        return false;

      IfThenElseChord test = lh.loopTest;
      if (test == null)
        return false;

      Chord s = lh.getNextChord();
      while (s.isPhiExpr())
        s = s.getNextChord();

      // Either the loop exit test goes directly to the LoopTailChord
      // or it is preceded by only PhiExprChords and the
      // LoopHeaderChord.

      LoopTailChord tail = lh.loopTail;
      boolean       tas  = false;
      if ((tail != test.getTrueCfgEdge()) && (tail != test.getFalseCfgEdge())) {

        if (s != test) // Check for test at start of loop.
          return false;

        tas = true; // Loop test at start of loop.
      }

      LoopHeaderChord child = null;
      if (lh.children != null) { // Check for only one or no child.
        if (lh.children instanceof Vector) {
          @SuppressWarnings("unchecked")
          Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) lh.children;
          int l = nc.size();
          if (l > 1)
            return false;
          if (l != 0)
            child = nc.elementAt(0);
        } else
          child = (LoopHeaderChord) lh.children;
      }

      if (child != null) {
        if (child.numLoopExits() != 1)
          return false;

        // Check for statements between the child and the end of the loop.

        Chord n = child.getLoopExit(0).getNextChord();
        while (n.isPhiExpr())
          n = n.getNextChord();

        if (!n.isAssignChord())
          return false;

        Expr lhs = ((ExprChord) n).getLValue();
        if (!(lhs instanceof LoadDeclAddressExpr))
          return false;

        if (!lh.isLoopIndex(((LoadDeclAddressExpr) lhs)))
          return false;

        if (tas) { // Test at start of loop.
          if (test != child.loopInit.getFirstInCfgEdge())
            return false;
        } else { // Assume test at end of loop.
          // Check for statements between the loop start and the child.

          if (s != child.loopInit) // Check for test at start of loop.
            return false;
        }
      }

      lh = child;
    }

    return true;
  }

  /**
   * Return true if this loop is tightly nested.  A tighly nested loop
   * has only one inner loop and that inner loop is a tightly nested
   * loop.
   */
  public final boolean isTightlyNested()
  { 
    if (parent == null)
      return false;

    LoopHeaderChord lh = this;
    while (true) {
      if (lh.children == null)
        return true;

      if (lh.children instanceof Vector) {
        @SuppressWarnings("unchecked")
        Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) lh.children;
        int l = nc.size();
        if (l > 1)
          return false;
        if (l == 0)
          return true;
        lh = nc.elementAt(0);
        continue;
      }

      lh = (LoopHeaderChord) lh.children;
    }
  }

  /**
   * Return a vector of the tightly nested loops or <code>null</code>.
   * A tighly nested loop has only one inner loop and that inner loop
   * is a tightly nested loop.
   */
  public final Vector<LoopHeaderChord> getTightlyNestedLoops()
  { 
    if (parent == null)
      return null; // The function itself is not a tightly nested loop.

    Vector<LoopHeaderChord> loops = new Vector<LoopHeaderChord>(3);
    LoopHeaderChord         lh    = this;
    while (true) {
      loops.addElement(lh);

      if (lh.children == null)
        return loops;

      if (lh.children instanceof Vector) {
        @SuppressWarnings("unchecked")
        Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) children;
        int    l  = nc.size();
        if (l > 1)
          return null;
        if (l == 0)
          return loops;
        lh = nc.elementAt(0);
        continue;
      }

      lh = (LoopHeaderChord) lh.children;
    }
  }

  /**
   * Return the nesting depth of the inner-most loop relative to this
   * loop.  This method is only valid for tightly nested looops.
   */
  public final int nestedLevel()
  {
    int             level = 1;
    LoopHeaderChord lh    = this;

    while (lh.children != null) {
      if (lh.children instanceof Vector)
        break;

      lh = (LoopHeaderChord) lh.children;
      level++;
    }

    return level;
  }

  /**
   * Define the primary induction variable for this loop.  This method
   * is used when we know syntactically what the induction variable
   * is.
   */
  public void defPrimaryInductionVariable(InductionVar var)
  {
    assert (primaryInductionVar == null) : "Primary induction variable already defined.";
    primaryInductionVar = var;
    if (inductionVars == null)
      inductionVars = new Vector<InductionVar>();
    inductionVars.add(var);
  }

  /**
   * Return the best primary induction variable.
   */
  public InductionVar getPrimaryInductionVar()
  {
    if ((primaryInductionVar == null) && (scribble.inSSA() != Scribble.notSSA)) {
      if (!boundsAndStepValid)
        doAll();
      if (inductionVars == null)
        findInductionVariables();
    }

    return primaryInductionVar;
  }

  /**
   * Return the name of the primary induction variable.
   */
  public String inductionVarName()
  {
    if (primaryInductionVar == null)
      return "?";
    VariableDecl iv = primaryInductionVar.getVar();
    return iv.getName();
  }

  /**
   * Return the primary loop index variable or <code>null</code> if
   * none known.
   */
  public VariableDecl getLoopIndexVar()
  {
    if ((primaryInductionVar == null)  && !boundsAndStepValid) {
      doAll();
      if (primaryInductionVar == null)
        return null;
    }

    if (primaryInductionVar == null)
      return null;

    return primaryInductionVar.getVar();
  }

  /**
   * Return the indicated primary induction variable.
   */
  public InductionVar getPrimaryInductionVar(int index)
  {
    if ((inductionVars == null) && !boundsAndStepValid)
      doAll();

    if (inductionVars != null) {
      int l   = inductionVars.size();
      int cnt = 0;
      for (int i = 0; i < l; i++) {  // For each induction var.
        InductionVar ivar = inductionVars.elementAt(i);
        if (ivar.getTermExpr() != null) { // Pick the one with the most information.
          if (index == cnt)
            return ivar;
          cnt++;
        }
      }
    }

    return null;
  }

  /**
   * Return the number of induction variables with termination tests.
   */
  public int numPrimaryInductionVars()
  {
    if ((inductionVars == null) && !boundsAndStepValid)
      doAll();

    int cnt = 0;
    if (inductionVars != null) {
      int l   = inductionVars.size();
      for (int i = 0; i < l; i++) {  // For each induction var.
        InductionVar ivar = inductionVars.elementAt(i);
        if (ivar.getTermExpr() != null) // Pick the one with the most information.
          cnt++;
      }
    }

    return cnt;
  }

  /**
   * Return the expression that initializes the primary induction
   * variable or <code>null</code> if it is not known.
   */
  public Expr getInductionVarInitExpr()
  {
    if (primaryInductionVar == null) {
      if (!boundsAndStepValid)
        doAll();
      if (primaryInductionVar == null)
        return null;
    }

    return primaryInductionVar.getInitExpr();
  }

  /**
   * Return the step value of the primary induction variable or 
   * <code>0</code> if it is not known.
   */
  public long getStepValue()
  {
    if (primaryInductionVar == null) {
      if (!boundsAndStepValid)
        doAll();
      if (primaryInductionVar == null)
        return 0;
    }

    return primaryInductionVar.getStepValue();
  }

  /**
   * Return the node representing the loop's lower bound.
   */
  public Expr getLowerBound()
  {
    if (primaryInductionVar == null) {
      if (!boundsAndStepValid)
        doAll();
      if (primaryInductionVar == null)
        return null;
    }

    return primaryInductionVar.getInitExpr(); 
  }

  /**
   * Return the node representing the loop's upper bound.
   */
  public Expr getUpperBound()
  { 
    if (primaryInductionVar == null) {
      if (!boundsAndStepValid)
        doAll();
      if (primaryInductionVar == null)
        return null;
    }

    return primaryInductionVar.getEndExpr(); 
  }

  private void computeLoopInfoComplete()
  {
    if (liComplete)
      return;

    liComplete = false;
    if (primaryInductionVar == null) {
      if (this instanceof BeginChord)
        liComplete = true;
      return;
    }

    if (primaryInductionVar.getStepExpr() == null)
      return;

    if (primaryInductionVar.getInitExpr() == null)
      return;

    liComplete = true;
  }

  /**
   * Return true if the expression is invariant in the loop.
   * The expression must be connected to the CFG.
   */
  public boolean isInvariant(Expr exp)
  {
    if (scribble.inSSA() == Scribble.notSSA)
      return false; // Can't tell.
 
    if (loopVariantExprs == null) 
      loopVariantExprs = new HashSet<Expr>(203);

    if (loopVariantExprs.contains(exp))
      return false;

    boolean inv = exp.isLoopInvariant(this);
    if (inv)
      return true;

    loopVariantExprs.add(exp);
    return false;
  }

  /**
   * Return true if the specified variable is the primary loop
   * induction variable.
   */
  public boolean isPrimaryLoopIndex(VariableDecl vd)
  {
    if (primaryInductionVar == null)
      return false;

    Declaration decl = vd.getOriginal();
    return (primaryInductionVar.getVar() == decl);
  }

  /**
   * Return true if an expression in the loop may result in a call to
   * a subroutine.
   */
  public boolean loopContainsCall()
  {
    if (loopContainsCallValid) 
      return loopContainsCall;

    HashSet<Chord> chordsInLoop = WorkArea.<Chord>getSet("loopContainsCall");

    getLoopChordsRecursive(chordsInLoop);

    // Go through all chords looking for calls.

    loopContainsCall = false;

    Iterator<Chord> e = chordsInLoop.iterator();
    outer:
    while (e.hasNext()) {
      Chord c = e.next();
      int   l = c.numInDataEdges();
      if (l == 0)
        continue;

      for (int i = 0; i < l; i++) {
        Expr in = c.getInDataEdge(i);
        loopContainsCall |= in.mayGenerateCall();
        if (loopContainsCall)
          break outer;
      }
    }

    WorkArea.<Chord>returnSet(chordsInLoop);

    loopContainsCallValid = true;

    return loopContainsCall;
  }

  /**
   * Return the number of induction variables found for the loop.
   */
  public int numInductionVars()
  {
    if (inductionVars == null)
      doAll();
    return inductionVars.size();
  }

 /**
   * Return the indicated induction variable.
   */
  public InductionVar getInductionVar(int index)
  {
    if (inductionVars == null)
      doAll();

    return inductionVars.elementAt(index);
  }

  /**
   * Return a vector of this loop's induction variables.
   */
  public Vector<InductionVar> getInductionVars()
  {
    if (inductionVars == null)
      doAll();
    return inductionVars;
  }

  /**
   * Returns the top most "real" loop containing this loop.  The
   * result may be <code>this</code> if <code>this</code> is the top
   * loop or <code>this</code> is the {@link
   * scale.score.chords.BeginChord BeginChord}.
   */
  public final LoopHeaderChord getTopLoop()
  {
    LoopHeaderChord c = this;
    LoopHeaderChord t = this;
    while (true) {
      LoopHeaderChord p = c.getParent();
      if (p == null)
        return t;
      t = c;
      c = p;
    }
  }

  /**
   * Return the {@link scale.score.Scribble Scribble} instance for
   * this loop header.
   */
  public final Scribble getScribble()
  {
    return scribble;
  }

  /**
   * Specify the {@link scale.score.Scribble Scribble} instance for
   * this loop header.
   */
  public final void setScribble(Scribble scribble)
  {
    this.scribble = scribble;
  }

  /**
   * Return the data dependency graph for this CFG.  It's value will
   * be null if there are no array references in loops.
   * @param createSpatialDependencies is true if spatial edges should
   * be created
   */
  public DDGraph getDDGraph(boolean createSpatialDependencies)
  {
    if (ddGraph != null)
      return ddGraph;

    // Label the entire graph because some edges from one loop will be
    // to references in another loop.

    scribble.getLoopTree().labelCFGLoopOrder();

    // Contruct the data dependency graph for the program.

    computeAllRecursive();
    ddGraph = new DDGraph(scribble, createSpatialDependencies);

    ddGraph.computeArrayDependences(this);

    return ddGraph;
  }

  /**
   * Give unique labels to all CFG nodes.  For any given node n1, node
   * n1's label is greater than the labels of all its immediate
   * predecessors.  In addition, for any {@link LoopExitChord
   * LoopExitChord}, its label is greater than the label of any other
   * node in the loop.  Note - if node n2's label is greater than node
   * n1's label, this does not necessarily mean that n2 is always
   * executed after n1; they could be in parallel branches of the CFG.
   * So, one can not compare the labels of two nodes and declare that
   * one is executed before or after the other.  But, the label values
   * do provide a useful partial ordering.
   * <p>
   * For a complete ordering of a CFG do
   * <pre>
   *    scribble.getLoopTree().labelCFGLoopOrder();
   * </pre>
   * <p>
   * Note - other algorithms may use the label field and destroy this
   * value.  An example is the computation of dominators.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @see scale.score.trans.AASR
   * @see scale.score.Scribble#linearize
   * @see scale.score.chords.Chord#pushChordWhenReady(Stack, int)
   */
  public void labelCFGLoopOrder()
  {
    int                   label = 0;
    Stack<Chord>          wl    = WorkArea.<Chord>getStack("labelCFGLoopOrder");
    Vector<LoopExitChord> x     = new Vector<LoopExitChord>();
    int                   leCnt = 0;

    Chord.nextVisit();
    wl.push(this);
    this.setLabel(label++);

    do {
      do {
        Chord c = wl.pop();
        c.setVisited();

        int l = c.numOutCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord cs = c.getOutCfgEdge(i);
          if (cs.isLoopExit()) {
            LoopExitChord le   = (LoopExitChord) cs;
            LoopTailChord tail = le.getLoopHeader().getLoopTail();

            // We can actually have a loop tail with no in-coming CFG edges.
            // For example
            //  do {
            //     ...
            // } while (0);

            if ((tail != null) && (tail.numInCfgEdges() > 0) && !tail.visited()) {
              x.addElement(le);
              leCnt++;
              continue;
            } else if (isLoopExit(le)) {
              le.setLabel(label++);
              continue;
            }
          }

          label = cs.pushChordWhenReady(wl, label);
        }
      } while (!wl.empty());

      int l = x.size();
      for (int i = l - 1; i >= 0; i--) {
        LoopExitChord le = x.elementAt(i);
        if (le == null)
          continue;

        LoopTailChord tail = le.getLoopHeader().getLoopTail();

        if (!tail.visited())
          continue;

        le.setLabel(label++);
        if (!isLoopExit(le))
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
        System.out.println("Loop exit un-processed " + xc.getSourceLineNumber() + " " + xc);
        return false;
      }
    }
    return true;
  }

  /**
   * Create the mapping from the array names to the {@link
   * scale.score.expr.SubscriptExpr SubscriptExpr} instances in this
   * loop.  Only "valid" subscript expressions are added.  A valid
   * subscript expression is one that dependence testing can utilize.
   * Examples of invalid subscript expressions include
   * <pre>
   *   (func(a,b))[i]
   *   strct.arr[i]
   * </pre>
   * @return true if all subscripts are valid
   */
  public boolean getSubscripts(Table<Declaration, SubscriptExpr> subs)
  {
    HashSet<Chord> done = WorkArea.<Chord>getSet("getSubscripts");
    Stack<Chord>   wl   = WorkArea.<Chord>getStack("getSubscripts");

    addLoopExits(done);

    Chord nxt = getNextChord();
    done.add(nxt);
    wl.push(nxt);

    boolean allValid = true;
    while (!wl.empty()) {
      Chord c = wl.pop();

      if (c.isLoopPreHeader()) {
        LoopHeaderChord lh   = (LoopHeaderChord) c.getNextChord();
        int             l2 = lh.numLoopExits();
        for (int i = 0; i < l2; i++) {
          Chord ex = lh.getLoopExit(i);
          Chord n = ex.getNextChord();
          if (done.add(n))
            wl.push(n);
        }
        continue;
      }

      allValid &= findSubscripts(c, subs);

      c.pushOutCfgEdges(wl, done);
    }

    WorkArea.<Chord>returnSet(done);
    WorkArea.<Chord>returnStack(wl);

    return allValid;
  }

  /**
   * Create a {@link scale.common.Table Table} mapping from array name
   * to {@link scale.score.expr.SubscriptExpr SubscriptExpr} instances
   * in this loop nest.  Only "valid" subscript expressions are added.
   * A valid subscript expression is one that dependence testing can
   * utilize.  Examples of invalid subscript expressions include
   * <pre>
   *   (func(a,b))[i]
   *   strct.arr[i]
   * </pre>
   * @return true if all subscripts are valid
   */
  public boolean getSubscriptsRecursive(Table<Declaration, SubscriptExpr> subs)
  {
    HashSet<Chord> done = WorkArea.<Chord>getSet("getSubscriptsRecursive");
    Stack<Chord>   wl   = WorkArea.<Chord>getStack("getSubscriptsRecursive");

    addLoopExits(done);

    done.add(this);
    wl.push(this.getNextChord());

    boolean allValid = true;
    while (!wl.empty()) {
      Chord c = wl.pop();

      if (c.isSpecial()) {
        if (c.isLoopHeader())
          continue;

        if (c.isLoopPreHeader())
          c = c.getNextChord();

        c.pushOutCfgEdges(wl, done);
        continue;
      }

      allValid &= findSubscripts(c, subs);
      c.pushOutCfgEdges(wl, done);
    }

    WorkArea.<Chord>returnSet(done);
    WorkArea.<Chord>returnStack(wl);

    return allValid;
  }

  /**
   * Return the affine expression defined by the specified expression
   * or <code>null</code> if the expression is not affine.
   * @param n is the expression.
   */
  public AffineExpr isAffine(Expr n)
  {
    if (n == null)
      return null;

    if (isAffineMap == null)
      isAffineMap = new HashMap<Expr, AffineExpr>(203);

    return n.getAffineExpr(isAffineMap, this);
  }

  /**
   * Specify that the loop information is no longer valid for this
   * loop and all its sub-loops.
   */
  public void recomputeLoops()
  {
    recomputeLoop();

    if (children == null)
      return;

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> v = (Vector<LoopHeaderChord>) children;
      int                     l = v.size();
      for (int i = 0; i < l; i++)
        v.get(i).recomputeLoops();
      return;
    }

    ((LoopHeaderChord) children).recomputeLoops();
  }

  /**
   * Specify that the loop information is no longer valid.
   */
  public void recomputeLoop()
  {
    loopVariantExprs      = null;
    boundsAndStepValid    = false;
    loopContainsCallValid = false;
    numChordsInLoopValid  = false;
  }

  private void doAll()
  {
    findInductionVariables();
    computeLoopInfoComplete();
    forwardSubstituteInductionVars();
    boundsAndStepValid = true;
  }

  /**
   * Compute everyting needed for dependence testing.
   */
  private void computeAllRecursive()
  {
    doAll();

    assert printTrace(this.getNestedLevel() * 2);

    if (children == null)
      return;

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> innerLoops = (Vector<LoopHeaderChord>) children;
      int    len        = innerLoops.size();
      for (int i = 0; i < len; i++) {
        LoopHeaderChord curLoop = innerLoops.elementAt(i);
        curLoop.computeAllRecursive();
      }
      return;
    }

    ((LoopHeaderChord) children).computeAllRecursive();
  }

  /**
   * Remove the induction variable information from this loop and all
   * child loops.
   */
  public void newSSAForm()
  {
    liComplete            = false;
    ddComplete            = false;
    forwardSubstituteDone = false;
    primaryInductionVar   = null;
    ddGraph               = null;
    isAffineMap           = null;
    loopVariantExprs      = null;

    if (inductionVars != null) {
      int l = inductionVars.size();
      for (int i = 0; i < l; i++) {
        InductionVar iv = inductionVars.get(i);
        iv.clean();
      }
      inductionVars = null;
    }

    if (children == null)
      return;

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> innerLoops = (Vector<LoopHeaderChord>) children;
      int                     len        = innerLoops.size();
      for (int i = 0; i < len; i++) {
        LoopHeaderChord curLoop = innerLoops.elementAt(i);
        curLoop.newSSAForm();
      }
      return;
    }

    ((LoopHeaderChord) children).newSSAForm();
  }

  /**
   * Specify the number of times the loop was entered during
   * execution.
   */
  public final void setProfEntryCnt(int count)
  {
    this.profEntryCnt = count;
  }

  /**
   * Return the number of times the loop was entered during execution.
   */
  public final int getProfEntryCnt()
  {
    return profEntryCnt;
  }

  /**
   * Specify the number of times the loop was iterated during
   * execution.
   */
  public final void setProfIterationCnt(int count)
  {
    this.profIterationCnt = count;
  }

  /**
   * Return the number of times the loop was iterated during
   * execution as determined by profiling.
   */
  public final int getProfIterationCnt()
  {
    return profIterationCnt;
  }

  /**
   * Return true if this chord is a LoopHeaderChord.
   */
  public final boolean isLoopHeader()
  {
    return true;
  }

  /**
   * Return true if the data dependence information for this loop nest
   * is complete.
   */
  public boolean isDDComplete()
  {
    return ddComplete;
  }

  /**
   * Return true if the loop information for this loop is complete.
   */
  public boolean isLoopInfoComplete()
  {
    if (!boundsAndStepValid)
      doAll();
    return liComplete;
  }

  /**
   * Specify that the data dependence information for this loop is not
   * complete because data dependence testing failed.
   */
  public void setDDIncomplete()
  {
    if (!this.ddComplete)
      return;

    this.ddComplete = false;

    LoopHeaderChord p = getParent();
    while (p != null) {
      if (!p.ddComplete)
        return;
      p.ddComplete = false;
      p = p.getParent();
    }
  }

  /**
   * Specify that the data dependence information for this loop is
   * complete to initialize for data dependence testing.
   */
  public void setDDComplete()
  {
    ddComplete = true;
    if (children == null)
      return;

    if (children instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoopHeaderChord> nc = (Vector<LoopHeaderChord>) children;
      int    l  = nc.size();
      for (int i = 0; i < l; i++) {
        LoopHeaderChord lh = nc.elementAt(i);
        lh.setDDComplete();
      }
      return;
    }

    ((LoopHeaderChord) children).setDDComplete();
    return;
  }

  /**
   * Find all the induction variables for this loop and determine
   * their attributes (initial value, limit, and step.
   * <p>
   * We start with the phi functions at the top of the loop.  All
   * induction variables must be defined by a one of these phi
   * functions.
   */
  private void findInductionVariables()
  {
    if (inductionVars != null)
      return; // Already computed.

    inductionVars = new Vector<InductionVar>();

    Chord cur = this.getNextChord();
    while (cur != null) {
      if (cur.isPhiExpr())
        findInductionVariable((PhiExprChord) cur, inductionVars);
      if (cur.isLastInBasicBlock())
        break;
      cur = cur.getNextChord();
    }

    // The bound and step expressions are obtained for each induction
    // variable. If it is the primary induction variable, then set
    // loop variables.

    primaryInductionVar = null;
    int l = inductionVars.size();
    for (int i = 0; i < l; i++) {  // For each induction var.
      InductionVar ivar = inductionVars.elementAt(i);
      if (ivar.moreCompleteThan(primaryInductionVar)) // Pick the one with the most information.
        primaryInductionVar = ivar;
    }
  }

  /**
   * Determine if the variable defined by the PhiExprChord is an
   * induction variable.  If so, add it to the vector.
   * <p>
   * In order to be an induction variable, it must be affine.  We
   * determine this by following the def-use links from the phi
   * function back to the phi function and building the affine
   * expression at each step.
   */
  private void findInductionVariable(PhiExprChord se, Vector<InductionVar> iVars)
  {
    PhiExpr phi = (PhiExpr) se.getRValue();

    assert (phi.numOperands() == 2) : "Loop structure funny." + this;

    LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) se.getLValue();
    VariableDecl        iv   = (VariableDecl) ldae.getDecl();  // Candidate induction variable.
    if (iv.isVirtual() || iv.addressTaken() || iv.inMemory())
      return;

    Stack<Expr>    wl   = WorkArea.<Expr>getStack("findInductionVariable");
    HashSet<Chord> done = WorkArea.<Chord>getSet("findInductionVariable");

    int n = se.numDefUseLinks();
    ml:
    for (int i = 0; i < n; i++) {
      LoadExpr le = se.getDefUse(i);
      Expr     op = findPath(done, le, phi, wl);
      done.clear();

      if (op == null)
        continue;

      // If a path is found from the phi function back to the phi
      // function, this is an induction variable.  Because this phi is
      // at the start of the loop, there has to be a path.  So, we are
      // really only interested in the computations performed along
      // the path (and which phi operand it is).

      // Generate the step expression.

      if (wl.isEmpty())
        continue;

      Expr step      = wl.pop().copy();
      Expr matchExpr = wl.pop();
      while (!wl.empty()) {
        Expr original = wl.pop();

        // Replace all occurrences of matchExpr in a copy of original
        // with the step expression.  This logic should be pushed into
        // the expression classes so that a complete tree span is
        // performed.

        if (original != matchExpr) {
          int     on   = original.numInDataEdges();
          boolean nviv = false;
          for (int j = 0; j < on; j++) {
            if (original.getInDataEdge(j) == matchExpr) {
              Expr copy = original.copy();
              Expr exp  = copy.getInDataEdge(j);
              copy.changeInDataEdge(exp, step);
              exp.unlinkExpression();
              step = copy;
              nviv = true;
              break;
            }
          }

          if (!nviv) { // Not a "valid" induction variable.
            step.unlinkExpression();
            wl.clear();
            continue ml;
          }
        }

        matchExpr = wl.pop();  // Use-def link that we used from step2.
      }

      // For now we will only deal with +/- constant step expressions.

      InductionVar ivar = new InductionVar(this, iv);
      iVars.addElement(ivar);

      ivar.setStepExpr(step);

      // Find the initialization expression for the induction variable.

      Expr iop = phi.getOperand(0);
      if (op == iop)
        iop = phi.getOperand(1);

      while (iop instanceof LoadExpr) {
        ExprChord ise = ((LoadExpr) iop).getUseDef();
        if (ise == null)
          break;
        Expr rhs = ise.getRValue();
        if (rhs instanceof PhiExpr)
          break;
        iop = rhs;
      }

      ivar.setInitExpr(iop);

      // Find a termination test if any.

      for (int j = 0; j < n; j++) {
        if (findMatch(new HashSet<Object>(203), se.getDefUse(j), phi, ivar))
          break;
      }

      break;
    }

    WorkArea.<Chord>returnSet(done);
    WorkArea.<Expr>returnStack(wl);
  }

  /**
   * Find the path back to the phi function and record the
   * steps on the <code>path</code>.
   * @return the phi operand or null
   */
  private Expr findPath(HashSet<Chord> done, Expr exp, PhiExpr phi, Stack<Expr> path)
  {
    while (true) {
      Note out = exp.getOutDataEdge();
      if (out == null)
        return null;

      if (out == phi)
        return exp;

      if (out instanceof PhiExpr)
        return null;

      if (out instanceof ExprChord) {
	ExprChord se = (ExprChord) out;

        if (!done.add(se))
          return null;

        if (!se.getLoopHeader().isSubloop(this))
          return null;

        int n = se.numDefUseLinks();
        for (int i = 0; i < n; i++) {
          LoadExpr le   = se.getDefUse(i);
          Expr     last = findPath(done, le, phi, path);
	  if (last != null) {
            path.push(le);
            path.push(se.getRValue());
            return last;
          }
        }
        return null;
      }

      if (out instanceof Chord)
        return null;

      exp = (Expr) out;
    }
  }

  /**
   * Find the match expression that is used to terminate the loop.
   * Note - the termination expression will depend on the induction
   * variable and yet be non-affine.
   * @return true if the match expression is found
   */
  private boolean findMatch(HashSet<Object> done, Expr exp, PhiExpr phi, InductionVar ivar)
  {
    while (true) {
      // If it is not simple, it won't be affine.  For an example,
      // look at 101.tomcatv and the ITER variable.  ITER is used in a
      // normal loop termination test and in a complex one.  We need a
      // way of picking one of them.  Since the data dependence
      // testing requires loops with affine loop-termination
      // expressions, we should pick the simplest one.  We really
      // ought to pick the best one - not just the first one
      // encountered.  Oh well, someone else can fight that battle.

      if (!exp.isSimpleExpr())
        return false;

      Note out = exp.getOutDataEdge();
      if (out == null)
        return false;

      if (out == phi)
        return false;

      if (out instanceof PhiExpr)
        return false;

      if (out instanceof MatchExpr) {
        MatchExpr me  = (MatchExpr) out;
        Chord     xit = me.getChord();
        if (xit instanceof IfThenElseChord) {
          IfThenElseChord ifc = (IfThenElseChord) xit;
          Chord           toe = ifc.getTrueCfgEdge();
          Chord           foe = ifc.getFalseCfgEdge();
          LoopExitChord   tle = toe.findLoopExit(this);
          LoopExitChord   fle = foe.findLoopExit(this);

          if (tle != null) {
            if (fle == null) {
              ivar.setTermExpr(me, true);
              return true;
            }
          } else if (fle != null) {
            ivar.setTermExpr(me, false);
            return true;
          }
        }
      } else if (out instanceof ExprChord) {
	ExprChord se = (ExprChord) out;
        if (!done.add(se))
          return false;

        if (!se.getLoopHeader().isSubloop(this))
          return false;

        int n = se.numDefUseLinks();
        for (int i = 0; i < n; i++) {
          if (findMatch(done, se.getDefUse(i), phi, ivar))
            return true;
        }
        return false;
      } else if (out instanceof Chord)
        return false;

      exp = (Expr) out;
    }
  }

  /**
   * For each induction var j:
   * <pre>
   *  j = jstart;
   *  for i = istart to iend by istep {
   *    ...
   *    j = j + jstep;
   *  }
   * </pre>
   * create a forward expression of the following form when it is
   * possible to do so.
   * <pre>
   *   jforwardExpr := jstart + (i - istart) * jstep/istep 
   * </pre>
   */
  private void forwardSubstituteInductionVars()
  {
    if (forwardSubstituteDone)
      return;

    if (this.getNestedLevel() <= 0)
      return;

    if (primaryInductionVar == null)
      return;

    forwardSubstituteDone = true;

    // If primary induction variable, use it as its forward
    // expression.

    VariableDecl vd  = primaryInductionVar.getVar();
    AffineExpr   pae = new AffineExpr(vd, this);

    primaryInductionVar.setForwardExpr(pae);

    Expr istart = primaryInductionVar.getInitExpr();
    if (istart == null) // If forward substitution is not possible.
      return;

    int          lin   = istart.findLinearCoefficient(vd, this);
    InductionVar isv   = getInductionVar(istart);
    AffineExpr   ivfe2 = null;
    if (isv != null)
      ivfe2 = isv.getForwardExpr();

    VariableDecl    iinv  = null;
    LoopHeaderChord iloop = null;
    if (istart instanceof LoadDeclValueExpr) {
      LoadDeclValueExpr ldve = (LoadDeclValueExpr) istart;
      iinv = ((VariableDecl) ldve.getDecl()).getOriginal();
      ExprChord ud = ldve.getUseDef();
      if (ud != null)
        iloop = ud.getChord().getLoopHeader();
    }

    long istep = 1;
    if (primaryInductionVar != null) {
      istep = primaryInductionVar.getStepValue();
      if (istep == 0)
        istep = 1;
      else if (istep < 0)
        istep = - istep;
    }

    int len = inductionVars.size();
    for (int i = 0; i < len; i++) {
      InductionVar ivar = inductionVars.elementAt(i);

      // If primary induction variable, use it(its name) as its
      // forward expression.

      if (ivar.isPrimary())
        continue;

      Expr jstart = ivar.getInitExpr();
      if (jstart == null) // If forward substitution is not possible.
        continue;

      long jstep = ivar.getStepValue();
      if (jstep == 0) // Induction variable step is not constant; skip forward substitution.
        continue; // If forward substitution is not possible.

      if (jstep < 0)
        jstep = - jstep;

      if ((istep % jstep) != 0) // LoopHeaderChord step does not divide induction variable; skip forward substitution.
        continue; // If forward substitution is not possible.

      long         mult = jstep / istep;
      InductionVar iv   = getInductionVar(jstart);
      AffineExpr   ivfe = null;
      if (iv != null)
        ivfe = iv.getForwardExpr();

      AffineExpr ae = new AffineExpr(vd, mult, this);
      if (ivfe != null)
        ae.merge(ivfe);
      else
        ae.addTerm(jstep);

      if (ivfe2 != null) {
        ivfe = ivfe2.copy();
        ivfe.multConst(-mult);
        ae.merge(ivfe);
        ivar.setForwardExpr(ae);
        continue;
      }

      long coeff = -mult * lin;
      if (iinv == null)
        ae.addTerm(coeff);
      else
        ae.addTerm(iinv, coeff, iloop);

      ivar.setForwardExpr(ae);
    }
  }

  /**
   * Find the subscript expressions in the CFG node and add them to
   * the table.  Only "valid" subscript expressions are found.  A
   * valid subscript expression is one that dependence testing can
   * utilize.  Examples of invalid subscript expressions include
   * <pre>
   *   (func(a,b))[i]
   *   strct.arr[i]
   * </pre>
   * @return true if all subscripts are valid
   */
  private boolean findSubscripts(Chord                             c,
                                 Table<Declaration, SubscriptExpr> subscripts)
  {
    Vector<Expr> exps = c.getExprList();
    if (exps == null)
      return true;

    boolean allValid = true;
    int     l        = exps.size();
    for (int k = 0; k < l; k++) {
      Expr curExpr = exps.elementAt(k);

      if (!(curExpr instanceof SubscriptExpr))
        continue;

      SubscriptExpr sub = (SubscriptExpr) curExpr;

      allValid &= sub.addUse(subscripts);
    }

    return allValid;
  }

  /**
   * Print the (sometimes very large) list of subscript expressions in
   * this loop.
   */
  public void printSubscripts(Table<Declaration, SubscriptExpr> arraySubscripts)
  {
    System.out.println("\n-------------------------------------");
    System.out.println(arraySubscripts);
    
    Enumeration<Declaration> e1 = arraySubscripts.keys();
    while (e1.hasMoreElements()) {
      Declaration key = e1.nextElement();
      System.out.print("  ");
      System.out.println(key);
      Iterator<SubscriptExpr> e2 = arraySubscripts.getRowEnumeration(key);
      while (e2.hasNext()) {
        System.out.print("    ");
        System.out.println(e2.next());
      }
    }
    System.out.println("-------------------------------------\n");
  }

  /**
   * Return true if <code>this</code> loop is a subloop of
   * <code>loop</code> or if <code>this == loop</code>.
   */
  public boolean isSubloop(LoopHeaderChord loop)
  {
    LoopHeaderChord n = this;
    while (n != null) {
      if (n == loop)
        return true;
      n = n.getParent();
    }
    return false;  
  }

  /**
   *
   */
  public Cost getTripCount()
  {
    if (primaryInductionVar == null)
      return new Cost(1.0, 1);

    long ic = primaryInductionVar.getIterationCount();
    if (ic < 0)
      return new Cost(1.0, 1);

    return new Cost(ic, 0);
  }

  /**
   * Construct a String that contains the loop header chord of this loop.
   * @return String object that contains a printable description of the 
   * loop header chord of this loop.
   */
  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(" lv:");
    buf.append(getNestedLevel());
    buf.append(" dd:");
    buf.append(ddComplete);
    buf.append(" li:");
    buf.append(liComplete);
    buf.append(" iv:");
    buf.append(inductionVarName());
    return buf.toString();
  }

  /**
   * Return true if this loop is an actual loop in the program.
   */
  public boolean isTrueLoop()
  { 
    return true;
  }

  /**
   * Return all the CFG nodes in this loop and all of its inner loops.
   */
  public void getLoopChordsRecursive(HashSet<Chord> chords)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("getLoopChordsRecursive");

    addLoopExits(chords);

    chords.add(this);
    wl.push(this);

    numChordsInLoop = 0;
    while (!wl.empty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl, chords);
      numChordsInLoop++;
    }

    numChordsInLoopValid = true;
    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Return the number of nodes in this loop including its subloops.
   * The CFG is not necessarily spanned to calculate the value.
   */
  public int numChordsInLoop()
  {
    if (!numChordsInLoopValid) {
      HashSet<Chord> chords = WorkArea.<Chord>getSet("numChordsInLoop");
      getLoopChordsRecursive(chords);
      WorkArea.<Chord>returnSet(chords);
    }

    return numChordsInLoop;
  }

  /**
   * Return the number of nodes in the loop, not including the loop
   * header or the {@link LoopExitChord LoopExitChord} nodes.  The CFG
   * is spanned to calculate the value.
   * @see Chord#nextVisit
   * @see #numChordsInLoop
   */
  public int countNodesInLoop()
  {
    int          count = 0;
    Stack<Chord> wl    = WorkArea.<Chord>getStack("loopSize");
    Chord        first = getNextChord();

    Chord.nextVisit();
    wl.push(first);
    this.setVisited();
    first.setVisited();

    int l = numLoopExits();
    for (int i = 0; i < l; i++)
      getLoopExit(i).setVisited();

    while (!wl.empty()) {
      count++;
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);

    return count;
  }

  /**
   * Return all the CFG nodes in this loop but not in its inner loops.
   */
  private void getLoopChords(HashSet<Chord> chords)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("getLoopChords");

    addLoopExits(chords);

    chords.add(this);
    wl.push(this);

    while (!wl.empty()) {
      Chord c = wl.pop();

      if (c.isLoopPreHeader()) {
        LoopHeaderChord lh   = (LoopHeaderChord) c.getNextChord();
        int             l    = lh.numLoopExits();
        for (int i = 0; i < l; i++) {
          Chord ex = lh.getLoopExit(i);
          Chord n  = ex.getNextChord();
          if (chords.add(n))
            wl.push(n);
        }
        continue;
      }

      c.pushOutCfgEdges(wl, chords);
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Return the common ancestor of this loop and the specified loop.
   * The common ancestor is the deepest loop that contains both loops.
   * The result may be this loop, the specified loop or some other
   * loop.  Note that the {@link scale.score.chords.BeginChord
   * BeginChord} node is a loop header.
   */
  public LoopHeaderChord commonAncestor(LoopHeaderChord l2)
  {
    LoopHeaderChord l1 = this;

    if (l1 == l2) // Same loop: most common case.
      return l1;

    LoopHeaderChord l2p = l2.getParent();
    if (l1 == l2p) // l2 is an immediate child of l1.
      return l1;

    LoopHeaderChord l1p = l1.getParent();
    if (l2 == l1p) // l1 is an immediate child of l2.
      return l2;

    if (l1p == l2p) // l1 & l2 are co-loops.
      return l1p;

    // Compute the common shared loop.

    do {
      if (l1.getNestedLevel() > l2.getNestedLevel())
        l1 = l1.getParent();
      else
        l2 = l2.getParent();
    } while (l1 != l2);

    return l2;
  }

  /**
   * Print a display of the information about the loop.
   */
  public boolean print(int indent)
  {
    Debug.printMessage("", this, indent);
    Debug.printMessage("  Indvar ", primaryInductionVar, indent);

    if (primaryInductionVar != null) {
      Debug.printMessage("  Init   ", primaryInductionVar.getInitExpr(), indent);
      Debug.printMessage("  Term   ", primaryInductionVar.getTermExpr(), indent);
      Debug.printMessage("  Step   ", primaryInductionVar.getStepExpr(), indent);
    }

    return true;
  }

  private boolean printTrace(int indent)
  {
    if (trace) {
      print(indent);
      printVector(inductionVars, "Induction Vars", 1 + this.getNestedLevel());
    }

    return true;
  }

  private void printVector(Vector<? extends Object> vec, String msg, int indent)
  {
    if (vec == null)
      return;

    Debug.printMessage(msg, null, indent);

    int l = vec.size();
    for (int i = 0; i < l; i++)
      Debug.printMessage("  ", vec.elementAt(i), indent);
  }

  private void printVectorOfVectors(Vector<Vector<? extends Object>> vec, String msg1, String msg2, int indent)
  {
    Debug.printMessage(msg1, null, indent);

    int isl = vec.size();
    for (int i = 0; i < isl; i++) {
      printVector(vec.elementAt(i), msg2 + " " + i, indent + 1);
    }
  }

  @SuppressWarnings("unchecked")
  private void printArrayOfVectors(Vector[] vec, String msg1, String msg2, int indent)
  {
    Debug.printMessage(msg1, null, indent);

    int    isl = vec.length;
    for (int i = 0; i < isl; i++) {
      printVector(vec[i], msg2 + " " + i, indent + 1);
    }
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTGREY;
  }

  /**
   * Return the {@link scale.score.InductionVar InductionVar} instance
   * associated with this {@link scale.score.expr.LoadExpr LoadExpr}
   * or <code>null</code>.
   */
  public InductionVar getLoopIndex(LoadExpr le)
  {
    Declaration  decl = le.getDecl();
    InductionVar iv   = getInductionVar((VariableDecl) decl);
    if (iv != null)
      return iv;

    // Maybe it's a temporary resulting from
    // _s12 = i3
    // i4 = i3+1

    Note x = le.getOutDataEdge();
    if (x instanceof ExprChord) {
      ExprChord se = (ExprChord) x;
      if (le == se.getLValue()) {
        Expr rhs = se.getRValue();
        if (rhs instanceof LoadDeclValueExpr) {
          iv = getLoopIndex((LoadDeclValueExpr) rhs);
          if (iv != null)
            return iv;
        }
      }
    }

    ExprChord se = le.getUseDef();
    if (se == null)
      return null;

    Expr rhs = se.getRValue();
    if (isInvariant(rhs))
      return null;

    if (rhs instanceof LoadDeclValueExpr)
      iv = getLoopIndex((LoadDeclValueExpr) rhs);

    return iv;
  }

  /**
   * Return true if the specified expression is a {@link
   * scale.score.expr.LoadExpr LoadExpr} instance that references a
   * loop induction variable.
   */
  public boolean isLoopIndex(Expr exp)
  {
    if (!(exp instanceof LoadExpr))
      return false;

    LoadExpr le = (LoadExpr) exp;
    return (getLoopIndex(le) != null);
  }

  /**
   * Return true if the specified variable is a loop induction variable.
   */
  public boolean isLoopIndex(VariableDecl vd)
  {
    return (getInductionVar(vd) != null);
  }

  /**
   * Return the {@link scale.score.InductionVar InductionVar} instance
   * referenced by this {@link scale.score.expr.LoadExpr LoadExpr}
   * expression or <code>null</code>.
   */
  public InductionVar getInductionVar(Expr exp)
  {
    if (!(exp instanceof LoadExpr))
      return null;

    LoadExpr    le   = (LoadExpr) exp;
    Declaration decl = le.getDecl();
    return getInductionVar((VariableDecl) decl);
  }

  /**
   * Return the {@link scale.score.InductionVar InductionVar} instance
   * associated with this variable or <code>null</code>.
   */
  public InductionVar getInductionVar(VariableDecl vd)
  {
    if (inductionVars == null)
      return null;

    Declaration decl = vd.getOriginal();
    int         l    = inductionVars.size();
    for (int i = 0; i < l; i++) {
      InductionVar ivar = inductionVars.elementAt(i);
      if (ivar.getVar() == decl)
        return ivar;
    }
    return null;
  }

  /**
   * Return true if the specified {@link scale.score.expr.LoadExpr
   * LoadExpr} instance references the primary loop induction
   * variable.
   */
  public boolean isPrimaryLoopIndex(Expr exp)
  {
    if (!(exp instanceof LoadExpr))
      return false;

    LoadExpr    le   = (LoadExpr) exp;
    Declaration decl = le.getDecl();
    return isPrimaryLoopIndex((VariableDecl) decl);
  }

  /**
   * Link a new CFG node that contains old links.  When a
   * LoopHeaderChord is copied, the copy has the same links as the
   * original loop header.  This method updates those links.
   * @param nm is a map from the old nodes to the new nodes.
   * @see scale.score.Scribble#linkSubgraph
   */
  public void linkSubgraph(HashMap<Chord, Chord> nm)
  {
    super.linkSubgraph(nm);

    LoopHeaderChord nlh = this;
    LoopHeaderChord op  = parent;

    if (op != null) {
      LoopHeaderChord np  = (LoopHeaderChord) nm.get(op);
      if (np == null)
        np = op;

      parent = np;
      np.addChildLoop(this);
    }

    IfThenElseChord olt = loopTest;
    if (olt != null) {
      IfThenElseChord nlt = (IfThenElseChord) nm.get(olt);
      if (nlt == null)
        nlt = olt;

      loopTest = nlt;
    }

    LoopTailChord oltl = loopTail;
    if (oltl != null) {
      Chord x = nm.get(oltl);

      LoopTailChord nltl = (LoopTailChord) nm.get(oltl);
      if (nltl == null)
        nltl = oltl;

      loopTail = nltl;
    }

    LoopInitChord oli = loopInit;
    if (oli != null) {
      LoopInitChord nli = (LoopInitChord) nm.get(oli);
      if (nli == null)
        nli = oli;

      loopInit = nli;
    }

    int    l = numLoopExits();
    Vector<LoopExitChord> x = new Vector<LoopExitChord>(l);
    for (int i = 0; i < l; i++) {
      LoopExitChord ole = getLoopExit(i);
      if (ole != null) {
        LoopExitChord nle = (LoopExitChord) nm.get(ole);
        if (nle == null)
          nle = ole;

        x.addElement(nle);
        nle.setLoopHeader(nlh);
      }
    }

    int l2 = x.size();
    if (l2 == 0)
      loopExits = null;
    else if (l2 == 1)
      loopExits = x.elementAt(0);
    else
      loopExits = x;
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    int ll = numLoopExits();
    for (int i = 0; i < ll; i++) {
      LoopExitChord le = getLoopExit(i);
      if (le.getLoopHeader() != this)
        throw new scale.common.InternalError("loop exit " + this + " -> " + le);
    }

    if (parent != null) {
      if (!parent.hasInnerLoop(this))
        throw new scale.common.InternalError("loop " + parent + " -> " + this);
    } else if (!(this instanceof BeginChord))
      throw new scale.common.InternalError("no parent loop " + this);

    Vector<LoopHeaderChord> children = getInnerLoops();
    int l = children.size();
    for (int i = 0; i < l; i++) {
      LoopHeaderChord child = children.elementAt(i);
      if (child.getParent() != this)
        throw new scale.common.InternalError("loop " + this + " -> " + child);
    }
  }

  /**
   * Return the integer value associated with a loop.
   * The value 0 indicates no loop.
   * @see scale.score.trans.URJ
   */
  public final int getLoopNumber()
  {
    return loopNumber;
  }
}
