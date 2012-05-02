package scale.score.trans;

import java.util.Iterator;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.pred.*;
import scale.score.dependence.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.*;

/**
 * This class replaces array element address calculations in a loop
 * with simple additions.
 * <p>
 * $Id: AASR.java,v 1.57 2007-10-04 19:58:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <p>
 * The array address strength reduction optimization attempts to
 * replace the complex array element address calculation with a simple
 * addition.  For example, consider the loop
 * <pre>
 *   for (int i = 0; i < n; i++)
 *     a(i) =
 * </pre>
 * AASR transforms this to
 * <pre>
 *   int *add = &a[0];
 *   for (int i = 0; i < n; i++) {
 *     *add =
 *     add = ((char *) add) + sizeof(int);
 *   }
 * </pre>
 * which eliminates the multiplication, of the loop induction variable
 * by the size of the array element, which would normally occur on each
 * iteration.  Arrays of higher rank result in even more savings
 * because their array element address calculation requires more
 * multiplies and additions.
 * <p>
 * Note that if the array is referenced in the loop in some manner
 * that can't be transformed, then register pressure has been increased
 * by one register (e.g., <code>add</code>).  And, even though the
 * induction variable is no longer referenced for each array access,
 * it is still needed for the loop termination test.
 * <p>
 * A transformation is {@link scale.score.trans#legal legal} if:
 * <ul>
 * <li>the loop has an induction variable with loop independent
 * initial value and increment,
 * <li>the subscript expression depends on the loop induction
 * variable,
 * <li>all other elements of the subscript expression are loop
 * independent, and
 * <li>the subscript expression is affine.
 * </ul>
 * It is {@link scale.score.trans#legal beneficial} if:
 * <ul>
 * <li>the loop is an inner-most loop,
 * <li>the loop is smaller than <code>maxLoopSize</code>, and
 * <li>the loop does not contain a function call.
 * </ul>
 */

public class AASR extends Optimization
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
   * Set true to use the <code>sizeof()</code> instead of the machine
   * specific array element size in addressable units.
   */
  public static boolean useSizeof = false;
  /**
   * Maximum loop size in CFG nodes.
   */
  public static int maxLoopSize = 400;

  private static int replacedCount   = 0;
  private static int newCFGNodeCount = 0;

  private static final String[] stats = {"replaced", "newCFGNodes"};

  static
  {
    Statistics.register("scale.score.trans.AASR", stats);
  }

  /**
   * Return the current number of array loads replaced.
   */
  public static int replaced()
  {
    return replacedCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * This class is used to record the information about the array
   * address variable that is created.  The information is for a
   * specific element of the array.
   */
  private final class Record
  {
    private Type              eType;  // The array element type.
    private Expr              mult;   // The loop independent multiplier of the index.
    private Expr              array;  // The loop independent address of the array in memory from the ArrayIndexExpr instance.
    public  Expr              offset; // The loop independent offset operand from the ArrayIndexExpr instance.
    private InductionVar      ivar;   // The induction variable for this set of array accesses.
    public  LoadDeclValueExpr access; // The first access to the new array element address variable.
    private EquivalenceDecl   ao;     // An alternate array address for EquivalenceDecl arrays.
    public  int               dist;   // The difference in the value of the induction variable from its initial value
                                      // when the array address variable initial value was determined.

    /**
     * Create a record for the new array address variable.
     * For some array access such as
     * <pre>
     *     DO 10 I = 10, 2, -1
     * 10   A[K - I]
     * </pre>
     * where <code>I</code> is the loop induction variable, then the
     * arguments to the constructor will be:
     * <dl>
     * <dt><code>mult</code><dd><code>-1</code>
     * <dt><code>array</code><dd><code>A</code>
     * <dt><code>index</code><dd>some temporary variable set from <code>I</code>
     * <dt><code>ivar</code><dd><code>I</code>
     * <dt><code>offset</code><dd>K-1
     * </dl>
     * The <code>array</code> and <code>offset</code> arguments must
     * be loop invariants.  The <code>access</code> argument is a
     * {@link scale.score.expr.LoadDeclValueExpr LoadDeclValueExpr}
     * instead of a {@link scale.clef.decl.VariableDecl VariableDecl}
     * so that the use-def information is easily copied to new
     * instances.
     * @param eType is the array element type
     * @param array is the expression specifying the base of the array
     * @param index is the expression based on the loop induction
     * variable used to access the array element
     * @param mult is the function used to modify the induction variable
     * @param ivar is the actual loop induction variable
     * @param offset specifies an offset from the base of the array
     * for the array element
     * @param access is an instance of loading the array address variable
     */
    public Record(Type              eType,
                  Expr              array,
                  int               dist,
                  Expr              mult,
                  InductionVar      ivar,
                  Expr              offset,
                  LoadDeclValueExpr access)
    {
      this.eType  = eType;
      this.dist   = dist;
      this.mult   = mult;
      this.array  = array;
      if (array instanceof LoadDeclAddressExpr) {
        VariableDecl vd = (VariableDecl) ((LoadDeclAddressExpr) array).getDecl();
        if (vd.isEquivalenceDecl()) {
          this.ao = (EquivalenceDecl) vd;
        }
      }
      this.ivar   = ivar;
      this.offset = offset;
      this.access = access;
    }

    /**
     * Return true if this array access can be mapped to the recorded
     * one and use the same array address variable.
     * @param eType is the array element type
     * @param array is the expression specifying the base of the array
     * @param mult is the function used to modify the induction variable
     * @param ivar is the actual loop induction variable
     */
    public boolean matches(Type eType, Expr array, Expr mult, InductionVar ivar)
    {
      // When the array address variable is incremented, it is not the
      // type that matters, it is only the number of bytes per array
      // element that is important!  But, we don't want to be
      // machine-specific at this point in the compilation.  The types
      // may differ when processing array accesses to Fortran COMMON
      // variables.

      if (eType != this.eType)
        return false;

      if (mult == null) {
        if (this.mult != null)
          return false;
      } else if (!mult.equivalent(this.mult))
        return false;

      if (ivar != this.ivar)
        return false;

      if ((this.ao != null) && (array instanceof LoadDeclAddressExpr)) {
        VariableDecl vd = (VariableDecl) ((LoadDeclAddressExpr) array).getDecl();
        if (vd.isEquivalenceDecl()) {
          EquivalenceDecl ao = (EquivalenceDecl) vd;
          return (ao.getBaseVariable() == this.ao.getBaseVariable());
        }
      }

      return array.equivalent(this.array);
    }

    /**
     * Return the address required to load the array element for this
     * instance using the array address variable specified by this
     * Record instance.
     * @param type is the Type for the address to be computed
     * @param offset specifies an offset from the base of the array
     * for the array element
     * @param index is the expression based on the loop induction
     * variable used to access the array element
     * @param array is the expression specifying the base of the array
     */
    public Expr computeLoad(Type              type,
                            Expr              offset,
                            LoadDeclValueExpr index,
                            Expr              array)
    {
      long eoff = 0;
      if ((this.ao != null) && (array instanceof LoadDeclAddressExpr)) {
        VariableDecl vd = (VariableDecl) ((LoadDeclAddressExpr) array).getDecl();
        if (vd.isEquivalenceDecl()) {
          EquivalenceDecl ao = (EquivalenceDecl) vd;
          eoff = ao.getBaseOffset() - this.ao.getBaseOffset();
        }
      }

      int dist = 0;
      try {
        dist = calcDist(index) - this.dist;
      } catch (java.lang.Exception ex) {
        return null;
      }

      Expr off  = null;

      if (dist != 0)
        off = new LiteralExpr(LiteralMap.put(dist, longType));

      if ((off != null) && (this.mult != null))
        off = MultiplicationExpr.create(longType, off, mult.copy());

      if (!this.offset.equivalent(offset)) {
        Expr aoff = offset.conditionalCopy();
        if (!this.offset.isLiteralExpr() || !((LiteralExpr) this.offset).isZero())
          aoff  = SubtractionExpr.create(longType, aoff, this.offset.copy());
        if (off != null)
          off = AdditionExpr.create(longType, off, aoff);
        else
          off = aoff;
      }

      if (off != null) {
        LiteralExpr sizeof = null;
        if (useSizeof)
          sizeof = new LiteralExpr(new SizeofLiteral(longType, eType));
        else {
          long bs = eType.memorySize(Machine.currentMachine);
          sizeof = new LiteralExpr(LiteralMap.put(bs, longType));
        }

        off = MultiplicationExpr.create(longType, off, sizeof);
      }

      if (eoff != 0) {
        Expr off2 = new LiteralExpr(LiteralMap.put(eoff, longType));
        if (off != null)
          off = AdditionExpr.create(longType, off, off2);
        else
          off = off2;
      }

      if (off == null)
        return this.access.conditionalCopy();

      return AdditionExpr.create(type, this.access.conditionalCopy(), off);
    }

    public void unlinkExpressions()
    {
      if (mult != null)
        mult.conditionalUnlinkExpression();
      if (array != null)
        array.conditionalUnlinkExpression();
      if (offset != null)
        offset.conditionalUnlinkExpression();
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(R ");
      buf.append(array);
      buf.append('[');
      if (mult != null) {
        buf.append(mult);
        buf.append(" * ");
      }
      buf.append(ivar.getVar());
      if (offset != null) {
        buf.append(" + ");
        buf.append(offset);
      }
      buf.append("])");
      return buf.toString();
    }
  }

  private Type    longType; /* 32-bit signed integer */
  private Type    psaut;
  private Chord   start;
  private SSA     ssa;
  private Expr    multiplier;

  private HashMap<Expr, Chord> emap; // Map from an expression to its critical CFG node.
  private Vector<Record>       amap; // Record the information about each new array address variable.
  private Vector<Expr>         adds;
  private Vector<Expr>         subs;
  private Vector<Expr>         obs;

  public AASR(Scribble scribble)
  {
    super(scribble, "_ar");
    
    this.longType = Machine.currentMachine.getIntegerCalcType();
    this.psaut    = PointerType.create(Machine.currentMachine.getSmallestAddressableUnitType());
    this.start    = scribble.getBegin();

    assert setTrace(classTrace);
  }

  /**
   * Do strength reduction of array accesses.
   */
  public void perform()
  {
    ssa  = scribble.getSSA();
    emap = new HashMap<Expr, Chord>(203);
    amap = new Vector<Record>();
    adds = new Vector<Expr>();
    subs = new Vector<Expr>();
    obs  = new Vector<Expr>();

    multiplier = null;

    scribble.getLoopTree().labelCFGLoopOrder();

    processLoop(scribble.getLoopTree());

    if (dChanged) {
      scribble.recomputeDominators();
      scribble.recomputeRefs();
    }
  }

  /**
   * Find and process only the inner-most loops.
   */
  private void processLoop(LoopHeaderChord loop)
  {
    Vector<LoopHeaderChord> innerLoops = loop.getInnerLoops();
    int                     ilcnt      = innerLoops.size();

    if (ilcnt == 0) { // If it is not an inner loop - don't bother.
      doLoop(loop);
      return;
    }

    for (int i = 0; i < ilcnt; i++)
      processLoop(innerLoops.elementAt(i));

    if (!useHeuristics)
      doLoop(loop);
  }

  /**
   *  Span the nodes in the loop looking for array accesses.
   */
  private void doLoop(LoopHeaderChord loop)
  {
    if (!loop.isLoopInfoComplete() || (loop.getLoopTail() == null))
      return;

    if (useHeuristics) {
      if ((loop.numChordsInLoop() > maxLoopSize) ||
          loop.loopContainsCall())
        return;
    }

    assert assertTrace(trace, "*** AASR loop ", loop);

    int minLabel = loop.getLabel();

    Stack<Chord> wl = WorkArea.<Chord>getStack("doLoop");
    Chord.nextVisit();
    wl.push(loop);
    loop.setVisited();

    int nle = loop.numLoopExits();
    for (int i = 0; i < nle; i++)
      loop.getLoopExit(i).setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);

      int n = c.numInDataEdges();
      for (int i = 0; i < n; i++)
        processExpr(c.getInDataEdge(i), loop, minLabel);
    }

    // Don't leave any dangling use-def links around.

    int al = amap.size();
    for (int i = 0; i < al; i++)
      amap.elementAt(i).unlinkExpressions();

    int ol = obs.size();
    for (int i = 0; i < ol; i++)
      obs.elementAt(i).conditionalUnlinkExpression();

    amap.clear();

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Find each array access in the expression and, if possible, convert it
   * to a reference to an array address variable.
   */
  private void processExpr(Expr exp, LoopHeaderChord loop, int minLabel)
  {
    // Do the sub-expressions first.

    int n = exp.numInDataEdges();
    for (int i = 0; i < n; i++)
      processExpr(exp.getInDataEdge(i), loop, minLabel);

    if (!(exp instanceof ArrayIndexExpr))
      return;

    processArrayExpr((ArrayIndexExpr) exp, loop, minLabel);

    // Don't leave any dangling use-def links around.

    if (multiplier != null)
      multiplier.conditionalUnlinkExpression();

    adds.clear();
    subs.clear();

    multiplier = null;
  }

  /**
   * If possible, convert the array access
   * to a reference to an array address variable.
   */
  private void processArrayExpr(ArrayIndexExpr  aie,
                                LoopHeaderChord loop,
                                int             minLabel)
  { 
    assert assertTrace(trace, "**  aie " + minLabel + " ", aie);

    // Found an array access - see if it can be converted.

    Expr access = aie.getArray();

    assert assertTrace(trace, "  access     ", access);
    assert assertTrace(trace, "  crit lab   ", access.getCriticalChord(emap, start).getLabel());
    assert assertTrace(trace, "  access lab ", access.getChord().getLabel());
    assert assertTrace(trace, "  crit       ", access.getCriticalChord(emap, start));
    assert assertTrace(trace, "  line lab   ", aie.getChord().getLabel());
    assert assertTrace(trace, "  line num   ", aie.getChord().getSourceLineNumber());

    if (access.getCriticalChord(emap, start).getLabel() >= minLabel)
      return; // The array address is loop dependent.

    // Get the actual address of the array.

    Expr array = access;
    while (array instanceof ConversionExpr) {
      ConversionExpr ce = (ConversionExpr) array;
      if (!ce.isCast())
        break;
      array = ce.getArg();
    }

    Expr offset = aie.getOffset();
    if (minLabel <= offset.getCriticalChord(emap, start).getLabel())
      return; // The offset is not loop independent.

    // Find the loop induction variable and put it into the *index* variable.

    assert assertTrace(trace, "  offset ", offset);

    Expr term = aie.getIndex();
    if (term == null)
      return;

    assert assertTrace(trace, "  term ", term);

    // Parse the index expression to make it in the form of c*I + k1 -
    // k2 where c, k1 & k2 are loop independent terms and I is the
    // loop induction variable.

    boolean multIsNegOne = false;
    while (true) {
      assert assertTrace(trace, "    term n ", term);

      if (term instanceof LoadDeclValueExpr) {
        LoadDeclValueExpr index = (LoadDeclValueExpr) term;
        if (loop.isLoopIndex(index))
          break;
        ExprChord se = index.getUseDef();
        if ((se == null) || (se.getLoopHeader() != loop))
          return;
        term = se.getRValue();
        continue;
      }

      if (term instanceof AdditionExpr) {
        AdditionExpr ae = (AdditionExpr) term;
        Expr         la = ae.getLeftArg();
        Expr         ra = ae.getRightArg();
        if (minLabel > la.getCriticalChord(emap, start).getLabel()) { // Left is loop independent.
          if (minLabel > ra.getCriticalChord(emap, start).getLabel()) // Right is loop independent.
            return; // Both are loop independent.

          term = ra;

          Expr l = la;
          if (multiplier != null) {
            if (multIsNegOne) {
              subs.addElement(l);
              obs.addElement(l);
              continue;
            }
            l = MultiplicationExpr.create(longType, multiplier.copy(), l.conditionalCopy());
          }
          adds.addElement(l);
          obs.addElement(l);
          continue;
        } 
        if (minLabel > ra.getCriticalChord(emap, start).getLabel()) { // Right is loop independent.
          term = la;
          Expr l = ra;
          if (multiplier != null) {
            if (multIsNegOne) {
              subs.addElement(l);
              obs.addElement(l);
              continue;
            }
            l = MultiplicationExpr.create(longType, multiplier.copy(), l.conditionalCopy());
          }
          adds.addElement(l);
          obs.addElement(l);
          continue;
        }
        return; // Both are loop dependent.
      }

      if (term instanceof SubtractionExpr) {
        SubtractionExpr ae = (SubtractionExpr) term;
        Expr            la = ae.getLeftArg();
        Expr            ra = ae.getRightArg();
        if (minLabel > la.getCriticalChord(emap, start).getLabel()) { // Left is loop independent.
          if (minLabel > ra.getCriticalChord(emap, start).getLabel()) // Right is loop independent.
            return; // Both are loop independent.
          term = ra;
          Expr l = la;
          if (multiplier == null) {
            multiplier = new LiteralExpr(LiteralMap.put(-1, longType));;
            multIsNegOne = true;
          } else {
            if (multIsNegOne) {
              multiplier.unlinkExpression();
              multiplier = null; // -1 * -1
              multIsNegOne = false;
              subs.addElement(l);
              obs.addElement(l);
              continue;
            } else {
              l = MultiplicationExpr.create(longType, multiplier.copy(), l.conditionalCopy());
              LiteralExpr le = new LiteralExpr(LiteralMap.put(-1, longType));
              multiplier = MultiplicationExpr.create(longType, multiplier, le);
            }
            multIsNegOne = false;
          }
          adds.addElement(l);
          obs.addElement(l);
          continue;
        } 
        if (minLabel > ra.getCriticalChord(emap, start).getLabel()) { // Right is loop independent.
          term = la;
          Expr l = ra;
          if (multiplier != null) {
            if (multIsNegOne) {
              adds.addElement(l);
              obs.addElement(l);
              continue;
            }
            l = MultiplicationExpr.create(longType, multiplier.copy(), l.conditionalCopy());
          }
          subs.addElement(l);
          obs.addElement(l);
          continue;
        }
        return; // Both are loop dependent.
      }

      if (term instanceof MultiplicationExpr) {
        MultiplicationExpr ae = (MultiplicationExpr) term;
        Expr               la = ae.getLeftArg();
        Expr               ra = ae.getRightArg();

        multIsNegOne = false;

        if (minLabel > la.getCriticalChord(emap, start).getLabel()) { // Left is loop independent.
          if (minLabel > ra.getCriticalChord(emap, start).getLabel()) // Right is loop independent.
            return; // Both are loop independent.
          term = ra;
          if (multiplier == null)
            multiplier = la.copy();
          else
            multiplier = MultiplicationExpr.create(longType, multiplier, la.conditionalCopy());
          continue;
        } 
        if (minLabel > ra.getCriticalChord(emap, start).getLabel()) { // Right is loop independent.
          term = la;
          if (multiplier == null)
            multiplier = ra.copy();
          else
            multiplier = MultiplicationExpr.create(longType, multiplier, ra.conditionalCopy());
          continue;
        }
        return; // Both are loop dependent.
      }

      if (term instanceof ConversionExpr) {
        ConversionExpr ce = (ConversionExpr) term;
        if (ce.getConversion() == CastMode.TRUNCATE) {
          term = ce.getArg();
          continue;
        }
      }

      return; // The subscript is not something we can handle.
    }

    LoadDeclValueExpr index = (LoadDeclValueExpr) term;
    assert assertTrace(trace, "  index ", index);

    int dist = 0;
    try {
      dist = calcDist(index);
    } catch (java.lang.Exception ex) {
      return;
    }

    assert assertTrace(trace, "  dist ", dist);

    // Find the induction step in the loop.

    InductionVar iv   = loop.getLoopIndex((LoadExpr) index);
    Expr         step = iv.getStepExpr();

    if (step == null)
      return; // This induction variable has no step value.

    assert assertTrace(trace, "  iv ", iv);
    assert assertTrace(trace, "  adds ", adds.size());
    assert assertTrace(trace, "       ", adds);
    assert assertTrace(trace, "  subs ", subs.size());
    assert assertTrace(trace, "       ", subs);

    int  ladds   = adds.size();
    Expr addTerm = null;
    if (ladds > 0)
      addTerm = addConversion(adds.elementAt(0).conditionalCopy());
    for (int i = 1; i < ladds; i++) {
      Expr conv = addConversion(adds.elementAt(i).conditionalCopy());
      addTerm = AdditionExpr.create(longType, addTerm, conv);
    }

    assert assertTrace(trace, "  addterm ", addTerm);

    int  lsubs   = subs.size();
    Expr subTerm = null;
    if (lsubs > 0)
      subTerm = addConversion(subs.elementAt(0).conditionalCopy());
    for (int i = 1; i < lsubs; i++) {
      Expr conv = addConversion(subs.elementAt(i).conditionalCopy());
      subTerm = AdditionExpr.create(longType, subTerm, conv);
    }

    assert assertTrace(trace, "  subterm ", subTerm);

    if (addTerm != null)
      offset = AdditionExpr.create(longType, addConversion(offset.conditionalCopy()), addTerm);

    if (subTerm != null) {
      if (offset == null)
        offset = NegativeExpr.create(longType, subTerm);
      else
        offset = SubtractionExpr.create(longType, offset.conditionalCopy(), subTerm);
    }

    assert assertTrace(trace, "  offset ", offset);
    assert assertTrace(trace, "  array  ", array);
    assert assertTrace(trace, "  index  ", aie.getIndex());
    assert assertTrace(trace, "  term   ", term);
    assert assertTrace(trace, "  mult   ", multiplier);

    Type type  = aie.getType().getNonAttributeType(); // Type of address.
    Type eType = type.getCoreType().getPointedTo();

    // See if we can reuse an address variable.

    int al = amap.size();
    for (int i = 0; i < al; i++) {
      Record r = amap.elementAt(i);
      if (r.matches(eType, array, multiplier, iv)) {
        Expr load = r.computeLoad(type, offset, index, array);

        if (load == null)
          return;

        Note x = aie.getOutDataEdge();
        x.changeInDataEdge(aie, load);
        aie.unlinkExpression();
        replacedCount++;
        offset.conditionalUnlinkExpression();
        return;
      }
    }

    // Find the initial value for the loop induction variable.

    Expr init = null;
    for (Chord first = loop; first != null; first = first.getNextChord()) {
      if (!(first.isPhiExpr()))
        continue;

      ExprChord           pse  = (ExprChord) first;
      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) pse.getLValue();
      VariableDecl        pvd  = (VariableDecl) ldae.getDecl();

      if (pvd.getOriginal() != iv.getVar())
        continue;

      PhiExpr phi = (PhiExpr) pse.getRValue();
      int     l   = phi.numOperands();
      for (int k = 0; k < l; k++) {
        Expr op = phi.getOperand(k);
        if (op.isLiteralExpr()) {
          init = op.conditionalCopy();
          break;
        }
        if (op instanceof LoadDeclValueExpr) {
          Chord bc = ((LoadDeclValueExpr) op).getUseDef();
          if (bc.getLabel() < minLabel) {
            init = op.conditionalCopy();
            break;
          }
          continue;
        }
      }
      break;
    }

    if (init == null)
      return;
    if (dist != 0)
      init = AdditionExpr.create(longType,
                                 init,
                                 new LiteralExpr(LiteralMap.put(dist, longType)));

    // We can generate cleaner code if we know that the offset is zero.

    boolean offsetIsZero = offset.isLiteralExpr() && ((LiteralExpr) offset).isZero();
    boolean initIsZero   = init.isLiteralExpr() && ((LiteralExpr) init).isZero();

    LoopPreHeaderChord lph = loop.getPreHeader();

    // Create the initial address value.

    Expr additional = null;
    if (!initIsZero) {
      if (multiplier != null)
        init = MultiplicationExpr.create(longType, init, multiplier.copy());

      additional = init;
    }

    Expr sizeof = null;
    if (useSizeof)
      sizeof = new LiteralExpr(new SizeofLiteral(longType, eType));
    else {
      long bs = eType.memorySize(Machine.currentMachine);
      sizeof = new LiteralExpr(LiteralMap.put(bs, longType));
    }

    Expr iaddr  = ConversionExpr.create(psaut,
                                        aie.getArray().conditionalCopy(),
                                        CastMode.CAST);
    if (offsetIsZero) {
      if (additional != null) {
        Expr mval = MultiplicationExpr.create(longType, additional, sizeof.conditionalCopy());
        iaddr = AdditionExpr.create(type, iaddr, mval);
      }
    } else if (additional != null) {
      Expr aval = AdditionExpr.create(longType, additional, offset.copy());
      Expr mval = MultiplicationExpr.create(longType, aval, sizeof.conditionalCopy());
      iaddr = AdditionExpr.create(type, iaddr, mval);
    } else {
      Expr mval = MultiplicationExpr.create(longType, offset.copy(), sizeof.conditionalCopy());
      iaddr = AdditionExpr.create(type, iaddr, mval);
    }

    if (iaddr instanceof AdditionExpr) {
      AdditionExpr ia = (AdditionExpr) iaddr;
      Expr         la = ia.getLeftArg();
      Expr         ra = ia.getRightArg();
      if (ra.isLiteralExpr() && (((LiteralExpr) ra).isZero())) {
        ia.setLeftArg(null);
        ia.unlinkExpression();
        iaddr = la;
      }
    }

    assert assertTrace(trace, "   iaddr  ", iaddr);

    iaddr = iaddr.addCast(type);


    // Create the variable to hold the current array element address.
    // As it will be updated in the loop, we need three versions:
    //
    //  vd = initial address
    //  ...
    //  vd1 = phi(vd, vd2)
    //  ...
    //  vd2 = vd1 + increment

    Type         pet = PointerType.create(eType);
    VariableDecl vd  = genTemp(pet);
    VariableDecl vd1 = ssa.createRenamedVariable(vd, true);
    VariableDecl vd2 = ssa.createRenamedVariable(vd, true);

    assert assertTrace(trace, "   offset ", offset);
    assert assertTrace(trace, "   iaddr  ", iaddr);

    // Generate the store of the initial address value into the
    // address variable.

    Expr      lhs = new LoadDeclAddressExpr(vd);
    Expr      rhs = ConversionExpr.create(pet, iaddr, CastMode.CAST);
    ExprChord ic  = new ExprChord(lhs, rhs);
    assert assertTrace(trace, "   ic     ", ic);

    // And insert the initialization before the loop.

    lph.insertBeforeInCfg(ic);
    ic.copySourceLine(loop);
    newCFGNodeCount++;
    ssa.addNewNode(ic);

    // Replace this instance of the array access with a reference to
    // the address variable.

    Note              x   = aie.getOutDataEdge();
    LoadDeclValueExpr ild = new LoadDeclValueExpr(vd1);

    x.changeInDataEdge(aie, ild);
    aie.unlinkExpression();
    replacedCount++;

    // Create the update value for the address variable.

    Expr umult = sizeof.conditionalCopy();
    if (multiplier != null)
      umult = MultiplicationExpr.create(longType, umult, multiplier.copy());

    umult = MultiplicationExpr.create(longType, umult, addUseDef(step.copy()));

    // Generate the store of the updated address value into the address variable.

    LoadDeclValueExpr uld   = new LoadDeclValueExpr(vd1);
    Expr              uaddr = AdditionExpr.create(type, uld, umult);
    ExprChord         uc    = new ExprChord(new LoadDeclAddressExpr(vd2), uaddr);

    // Determine where to place the address update instructions.

    Chord sc = loop.getLoopTail();
    if (sc.numInCfgEdges() == 1) {
      Chord prior = sc.getInCfgEdge(0);
      if (prior instanceof IfThenElseChord) // Don't add another branch in the generated code.
        sc = prior;
    }

    // And insert the update in the loop.

    sc.insertBeforeInCfg(uc);
    uc.copySourceLine(loop);
    newCFGNodeCount++;
    ssa.addNewNode(uc);

    // Record this information so we can use it again.

    Record r = new Record(eType, array, dist, multiplier, iv, offset, uld);
    amap.addElement(r);

    // Create the phi function and insert in at the top of the loop.

    Vector<Expr>      ops = new Vector<Expr>(2);
    LoadDeclValueExpr op0 = new LoadDeclValueExpr(vd);
    LoadDeclValueExpr op1 = new LoadDeclValueExpr(vd2);

    op0.setUseDef(ic);
    op1.setUseDef(uc);

    boolean isOp0 = (0 == loop.indexOfInCfgEdge(lph));
    if (isOp0) {
      ops.addElement(op0);
      ops.addElement(op1);
    } else {
      ops.addElement(op1);
      ops.addElement(op0);
    }

    PhiExpr   phi = new PhiExpr(type, ops);
    ExprChord pc  = new PhiExprChord(new LoadDeclAddressExpr(vd1), phi);

    uld.setUseDef(pc);
    ild.setUseDef(pc);
    loop.getNextChord().insertBeforeInCfg(pc);
    pc.copySourceLine(loop);
    ssa.addNewNode(pc);
    newCFGNodeCount++;

    dChanged = true;
  }

  /**
   * Return the difference in the value of the induction variable
   * between this use and its value at the beginning point of the loop
   * body.  If the form of the induction variable induction steps is
   * not a simple plus or minus a constant, throw an exception.
   */
  private static int calcDist(LoadDeclValueExpr ldve) throws java.lang.Exception
  {
    int dist = 0;
    while (true) {
      ExprChord ud = ldve.getUseDef();
      if (ud == null)
        break;

      Expr rhs = ud.getRValue();
      if (rhs instanceof LoadDeclValueExpr) {
        ldve = (LoadDeclValueExpr) rhs;
        continue;
      }

      if (rhs instanceof PhiExpr)
        break;

      if (!(rhs instanceof BinaryExpr))
        throw new java.lang.Exception("");

      BinaryExpr be = (BinaryExpr) rhs;
      Expr       la = be.getLeftArg();
      Expr       ra = be.getRightArg();

      if (rhs instanceof AdditionExpr) {
        if (la.isLiteralExpr()) {
          Expr t = la;
          la = ra;
          ra = t;
        }

        if (!(la instanceof LoadDeclValueExpr))
          throw new java.lang.Exception("");
        ldve = (LoadDeclValueExpr) la;
        if (!ra.isLiteralExpr())
          throw new java.lang.Exception("");
        Object lit = ((LiteralExpr) ra).getLiteral().getConstantValue();
        if (!(lit instanceof IntLiteral))
          throw new java.lang.Exception("");
        dist += (int) ((IntLiteral) lit).getLongValue();
        continue;
      } 
      if (rhs instanceof SubtractionExpr) {
        if (!(la instanceof LoadDeclValueExpr))
          throw new java.lang.Exception("");
        ldve = (LoadDeclValueExpr) la;
        if (!ra.isLiteralExpr())
          throw new java.lang.Exception("");
        Object lit = ((LiteralExpr) ra).getLiteral().getConstantValue();
        if (!(lit instanceof IntLiteral))
          throw new java.lang.Exception("");
        dist -= (int) ((IntLiteral) lit).getLongValue();
        continue;
      }
      throw new java.lang.Exception("");
    }
    return dist;
  }

  /**
   * Ensure that the expression is of the signed "integer-calc" type.
   */
  private Expr addConversion(Expr term)
  {
    Type aType = term.getCoreType();
    if (!aType.isSigned()) {
      // If the offset is unsigned, we need to sign extend it to get
      // the proper value to add to the address variable.
      aType  = SignedIntegerType.create(((IntegerType) aType).bitSize());
      term = ConversionExpr.create(aType, term.conditionalCopy(), CastMode.TRUNCATE);
    }
    return ConversionExpr.create(longType, term.conditionalCopy(), CastMode.TRUNCATE);
  }

  /**
   * Add the use-def links to the expression.
   */
  private Expr addUseDef(Expr step)
  {
    if (step.isLiteralExpr())
      return step;

    References       refs = scribble.getRefs();
    Vector<LoadExpr> v    = new Vector<LoadExpr>();

    step.getLoadExprList(v);

    int l = v.size();
    for (int i = 0; i < l; i++) {
      LoadExpr     le = v.get(i);
      VariableDecl vd = (VariableDecl) le.getDecl();

      if (vd.isNotSSACandidate())
        continue;

      assert (refs.numDefChords(vd) == 1) :
        "One & only one def(" +
        refs.numDefChords(vd) +
        ") allowed of " +
        vd.getName() +
        " in " +
        scribble.getRoutineDecl().getName();

      Iterator<Chord> it = refs.getDefChords(vd);
      ExprChord       se = (ExprChord) it.next();
      le.setUseDef(se);
    }

    return step;
  }
}
