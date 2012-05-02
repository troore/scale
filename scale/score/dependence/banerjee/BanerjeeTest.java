package scale.score.dependence.banerjee;

import scale.common.*;
import scale.score.*;
import scale.score.chords.LoopHeaderChord;
import scale.score.expr.*;
import scale.score.dependence.*;
import scale.clef.decl.VariableDecl;

/**
 * A class which implements Banerjee's data dependence test.
 * <p>
 * $Id: BanerjeeTest.java,v 1.29 2007-05-03 18:02:43 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Currently, we have only implemented the Single Loop Index test.
 * The Single Loop Index test is an exact algorithm that works only
 * with subscripts that have a single loop index variable.  The SLI
 * test only produces direction information (that is, no distance
 * information).  Also, the test only works when the bounds of the
 * loops are constants.
 * <p>
 * Note that only dependence direction information is computed.
 * Distance values are not computed.
 * @see scale.score.dependence.omega.OmegaTest
 */
public class BanerjeeTest extends DataDependence 
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  private int     tLowerBound; // Lower bound value used in single loop index test.
  private int     tUpperBound; // Upper bound value used in single loop index test.
  private long    info;        // Used to return a DDInfo value.
  private boolean trace;

  /**
   * Create an object for dependence testing.
   */
  public BanerjeeTest(Scribble scribble)
  {
    super(scribble);
    assert setTrace();
  }

  private boolean setTrace()
  {
    trace = Debug.trace(scribble.getRoutineDecl().getName(), classTrace, 3);
    return true;
  }

  /**
   * Implements Banerjee's dependence test.  Currently, we just
   * implement the simple case when the array index expressions are
   * either:
   * <ul>
   * <li>Constant values.
   * <li>Single loop index expresssions.
   * </ul>
   * Note that only dependence direction information is computed.
   * Distance values are not computed.  Also, the loop bounds must be
   * constants.
   * @param onode is a node access 
   * @param inode is a node access
   * @param oloop is the loop containing <code>onode</code>
   * @param iloop is the loop containing <code>inode</code>
   * @param precedes specifies the execution order.
   *  It is 1 if inode is execute before onode,
   * -1 if onode is executed befoe inode,
   * and 0 if they are "executed in parallel".
   * @return the data dependence test result
   */
  public int ddTest(SubscriptExpr   onode,
                    SubscriptExpr   inode,
                    LoopHeaderChord oloop,
                    LoopHeaderChord iloop,
                    int             precedes)
  {
    super.initialize(onode, inode, oloop, iloop, precedes);

    this.tLowerBound = Integer.MIN_VALUE;
    this.tUpperBound = Integer.MAX_VALUE;

    SubscriptExpr e1  = onode;
    SubscriptExpr e2  = inode;
    int           e1l = e1.numSubscripts();
    int           e2l = e2.numSubscripts();

    LoopHeaderChord tlp = cloop.getTopLoop();

    if (e1l != e2l)
      return cFailed;

    for (int i = 0; i <= bnest; i++)
      ddinfo[i] = DDInfo.cDist0;

    int ddResult = cIndependent;
    for (int i = 0; i < e1l; i++) {
      Expr oindex = e1.getSubscript(i);
      Expr iindex = e2.getSubscript(i);

      AffineExpr affn1 = tlp.isAffine(oindex);
      AffineExpr affn2 = tlp.isAffine(iindex);

      if ((affn1 == null) || (affn2 == null))
        return cFailed;

      // Check for special cases - zero index variables, single loop
      // index.

      int af1nt = affn1.numTerms();
      int af2nt = affn2.numTerms();
      int a1c   = (int) affn1.getConstant();
      int a2c   = (int) affn2.getConstant();

      if ((af1nt == 0) && (af2nt == 0)) {
        if (a1c != a2c) // constant independent
          return cIndependent;

        if (e1l == 1)
          return cLCDependent;

        // Result depends on other indicies.

        continue;
      }

      if ((af1nt != 1) || (af2nt != 1))
        return cFailed;

      VariableDecl ai1 = affn1.getVariable(0);
      VariableDecl ai2 = affn2.getVariable(0);

      if (ai1 != ai2)
        return cFailed;

      LoopHeaderChord loop = null;
      int k;
      for (k = bnest; k > 0; k--) {
        LoopHeaderChord lp = loopInfo[k];
        if (lp.isLoopIndex(ai1)) {
          loop = lp;
          break;
        }
      }

      if (loop == null)
        return cFailed;

      // Try single loop index test (an exact test).

      int c1  = (int) affn1.getCoefficient(0);
      int c2  = (int) affn2.getCoefficient(0);
      int res = testSingleLoopIndex(c1, c2, a1c, a2c, loop);

      switch (res) {
      case cIndependent:
        ddinfo[k] = 0;
        continue;
      case cUnkDependent:
        ddinfo[k] = 0;
        ddResult = cUnkDependent;
        continue;
      case cEqDependent:
        if (ddResult == cIndependent)
          ddResult = cEqDependent;
        ddinfo[k] = info;
        continue;
      case cLCDependent:
        if (ddResult == cIndependent)
          ddResult = cLCDependent;
        else if (ddResult == cEqDependent)
          ddResult = cLCDependent;
        ddinfo[k] = info;
        continue;
      case cFailed:
        ddinfo[k] = 0;
        return cFailed;
      }
    }

    return ddResult;
  }

  /**
   * Compute data dependences in one direction.
   * @param forward is true if we want forward dependence information.
   * @return the dependence information (in one direction).
   */
  public Vector<long[]> getDependenceInfo(boolean forward)
  {
    Vector<long[]> ddvect = new Vector<long[]>(1);
    ddvect.addElement(ddinfo);
    return ddvect;
  }

  /**
   * A special dependence test created by Banerjee that handles
   * subscripts that involve only a single loop index variable.  See
   * "Optimizing Supercompilers for Supercomputers", page 28.  The
   * test computes dependence direction information, but not distance
   * information.  The loop bounds must be constant.
   *
   * @param am is the coefficient for the first loop index expr.
   * @param bm is the coefficient for the second loop index expr.
   * @param a1c is the constant for the first loop index expr.
   * @param a2c is the constant for the second loop index expr.
   * @return the data dependence result
   */
  private int testSingleLoopIndex(int am, int bm, int a1c, int a2c, LoopHeaderChord loop)
  {
    int[] xy = new int[2];
    int   g  = kirchGCD(am, bm, xy);

    if (g == 0)
      return cFailed;

    int dif  = a2c - a1c;
    int gg   = dif / g;

    if ((gg * g) != dif) // There is no data dependence.
      return cIndependent;

    int        x  = xy[0] * gg;
    int        y  = xy[1] * gg;
    AffineExpr lb = cloop.isAffine(loop.getLowerBound());

    // Check that it's not null and it's a constant value.

    if ((lb == null) || (lb.numTerms() != 0))
      return cFailed;

    int lm = (int) lb.getConstant();
    if (relationTest(bm / g, lm - x, true) || relationTest(am / g, lm - y, true))
      return cFailed;

    AffineExpr ub = cloop.isAffine(loop.getUpperBound());

    // Check that it's not null and it's a constant value.

    if ((ub == null) || (ub.numTerms() != 0))
      return cFailed;

    int um = (int) ub.getConstant();
    if (relationTest(-bm / g, x - um, true) || relationTest(-am / g, y - um, true))
      return cFailed;

    int direction = DDInfo.ddAll;

    // now, check the direction vectors
    if (relationTest(am - bm, x - y + 1, false))
      direction &= ~DDInfo.ddForward;

    if (relationTest(am - bm, x - y, false) || relationTest(bm - am, y - x, false))
      direction &= ~DDInfo.ddEqual;

    if (relationTest(bm - am, y - x + 1, false))
      direction &= ~DDInfo.ddBackward;

    // If no dependence direction values are set then we're
    // independent.

    if (direction == 0)
      return cIndependent;

    boolean exact = (direction == DDInfo.ddEqual);
    info = DDInfo.create(0, exact, direction);

    return exact ? cEqDependent : cLCDependent;
  }

  /** 
   * Test a relation for the single loop index exact algorithm.  The
   * test performs step 4 from pg. 30 in <i>Optimizing Supercompilers
   * for Supercomputers</i>.  The inequality is <tt>t*tmul >=
   * rhs</tt>.  Note that this code is different from Program 2.3 on
   * pg. 33 (instead it is a copy of the code in Tiny).  The values of
   * <tt>tUpperBound</tt> and <tt>tLowerBound</tt> should only be
   * updated when checking for dependence and not when we are just
   * checking for direction.
   *
   * @param tmul the index variable coefficient / GCD value.
   * @param rhs the 'adjusted' lower or upper bound.
   * @param update if true if we update <tt>tUpperBound</tt> and
   * <tt>tLowerBound</tt> when they change.
   * @return true if the relation is not satisfied (i.e., no dependence).
   */
  private boolean relationTest(int tmul, int rhs, boolean update)
  {
    int lb = tLowerBound;
    int ub = tUpperBound;

    if (tmul == 0) {
      if (rhs > 0) // independent
        return true;
    } else if (tmul > 0) {
      int r;

      if (rhs > 0) {
        r = (rhs + tmul - 1) / tmul;
      } else {
        r = rhs / tmul;
      }

      if (lb < r)
        lb = r;

      if (update)
        tLowerBound = lb;

      // check for independence 

      if (ub < lb)
        return true;
    } else { // tmul < 0
      int r;
      if (rhs < 0) {
        r = rhs / tmul;
      } else {
        r = (rhs - tmul + 1) / tmul;
      }
      if (ub > r)
        ub = r;
      
      if (update)
        tUpperBound = ub;

      // check for independence

      if (ub < lb)
        return true;
    }
    return false;
  }

  /**
   * Kirch's algorithm to efficiently compute the greatest common
   * divisor.  This algorithm computes <tt>g = GCD(a,b)</tt> and also
   * provides one solution to the equation <tt>ax - by =
   * GCD(a,b)</tt>.  See "Optimizing Supercompilers for
   * Supercomputers", page 29.
   * 
   * @param a input to GCD algorithm.
   * @param b input to GCD algorithm.
   * @param x output parameter which is part of the solution <tt>ax - by = GCD(a,b)</tt>.
   * @param y output parameter which is part of the solution <tt>ax - by = GCD(a,b)</tt>.
   * @return the greatest common divisor of the two inputs.
   */
  private int kirchGCD(int a, int b, int[] xy)
  {
    int g;
    int x;
    int y;
    if ((a == 0) && (b == 0)) {
      g = 0;
      x = 0;
      y = 0;
    } else if (a == 0) {
      g = Math.abs(b);
      x = 0;
      y = -b / g;
    } else if (b == 0) {
      g = Math.abs(a);
      x = a / g;
      y = 0;
    } else {
      int a0 = 1;
      int a1 = 0;
      int b0 = 0;
      int b1 = 1;
      int g0 = Math.abs(a);
      int g1 = Math.abs(b);
      int q  = g0 / g1;
      int r  = g0 % g1;

      while (r != 0) {
        int a2 = a0 - q * a1;
        a0 = a1;
        a1 = a2;
        int b2 = b0 - q * b1;
        b0 = b1;
        b1 = b2;
        g0 = g1;
        g1 = r;
        q = g0 / g1;
        r = g0 % g1;
      }

      g = g1;
      x = a1;
      y = -b1;

      if (a < 0)
        x = -x;

      if (b < 0)
        y = -y;
    }

    xy[0] = x;
    xy[1] = y;
    return g;
  }

  public void spatialTest(SubscriptExpr   onode,
                          SubscriptExpr   inode,
                          LoopHeaderChord oloop,
                          LoopHeaderChord iloop,
                          int             precedes) throws java.lang.Exception
  {
    throw new scale.common.NotImplementedError("spatialTest in Banerjeetest");
  }
}
