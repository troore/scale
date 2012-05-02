package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.*;
import scale.clef.expr.Transcendental2Op;
import scale.score.Predicate;

import scale.score.dependence.AffineExpr;
import scale.score.Note;

/**
 * This class represents two-argument inrinsic functions (e.g., atan2).
 * <p>
 * $Id: Transcendental2Expr.java,v 1.19 2007-04-27 18:04:38 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If would have been better to name this class the BuiltInOperation
 * class.  It represents those complex operations which it may be
 * possible to implement on the target architecture with a simple
 * sequence of instructions.  An example is the square root function
 * which some architectures provide as a single instruction.
 * @see scale.clef.expr.Transcendental2Op
 */
public class Transcendental2Expr extends BinaryExpr 
{
  /**
   * ftn is the transcendental function.
   */
  private int ftn;

  public Transcendental2Expr(Type t, Expr e1, Expr e2, int ftn)
  {
    super(t, e1, e2);
    this.ftn = ftn;
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the <code>equivalent()</code> method of the
   * derived classes.
   * @return true if the expressions are equivalent
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    Transcendental2Expr o = (Transcendental2Expr) exp;
    return (o.ftn == ftn);
  }

  /**
   * Return a value indicating the transcendental function
   * represented.
   */
  public int getFtn()
  {
    return ftn;
  }

  public Expr copy()
  {
    return new Transcendental2Expr(getType(), getLeftArg().copy(), getRightArg().copy(), ftn);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | SE_OVERFLOW | SE_DOMAIN;
  }

  public void visit(Predicate p)
  {
    p.visitTranscendental2Expr(this);
  }

  public String getDisplayLabel()
  {
    return Transcendental2Op.trans2Ftn[ftn];
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return r;

    if (ftn == Transcendental2Op.cAtan2) {
      Literal la = getLeftArg().getConstantValue(cvMap);
      Literal ra = getRightArg().getConstantValue(cvMap);
      r = Lattice.atan2(getCoreType(), la, ra);
    } else if (ftn == Transcendental2Op.cSign) {
      Literal la = getLeftArg().getConstantValue(cvMap);
      Literal ra = getRightArg().getConstantValue(cvMap);
      r = Lattice.sign(getCoreType(), la, ra);
    } else if (ftn == Transcendental2Op.cDim) {
      Literal la = getLeftArg().getConstantValue(cvMap);
      Literal ra = getRightArg().getConstantValue(cvMap);
      r = Lattice.dim(getCoreType(), la, ra);
    } else
      r = Lattice.Bot;

    cvMap.put(this, r);
    return r;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal r = null;
    if (ftn == Transcendental2Op.cAtan2) {
      Literal la = getLeftArg().getConstantValue();
      Literal ra = getRightArg().getConstantValue();
      r = Lattice.atan2(getCoreType(), la, ra);
    } else if (ftn == Transcendental2Op.cSign) {
      Literal la = getLeftArg().getConstantValue();
      Literal ra = getRightArg().getConstantValue();
      r = Lattice.sign(getCoreType(), la, ra);
    } else if (ftn == Transcendental2Op.cDim) {
      Literal la = getLeftArg().getConstantValue();
      Literal ra = getRightArg().getConstantValue();
      r = Lattice.dim(getCoreType(), la, ra);
    } else
      r = Lattice.Bot;

    return r;
  }

  /**
   * Return true if this expression may result in the generation of a
   * call to a subroutine.
   */
  public boolean mayGenerateCall()
  {
    return true;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 25 + super.executionCostEstimate();
  }
}
