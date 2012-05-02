package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.LiteralMap;
import scale.clef.type.Type;
import scale.score.Predicate;
import scale.score.dependence.AffineExpr;

/**
 * This class represents the division operation.
 * <p>
 * $Id: DivisionExpr.java,v 1.44 2007-10-04 19:58:29 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class DivisionExpr extends BinaryExpr
{
  public DivisionExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * This method of creating a DivisionExpr instance will return a
   * reduced expression if possible.
   */
  public static Expr create(Type type, Expr la, Expr ra)
  {
    Type tat = type.getCoreType();
    if (tat.isAtomicType() && ra.isLiteralExpr()) {
      Object litr = ((LiteralExpr) ra).getLiteral().getConstantValue();
      if (litr instanceof IntLiteral) {
        long value = ((IntLiteral) litr).getLongValue();
        if (value == 1)
          return la;
        if ((value != 0) && la.isLiteralExpr()) {
          Object litl = ((LiteralExpr) la).getLiteral().getConstantValue();
          if (litl instanceof IntLiteral) {
            value = ((IntLiteral) litl).getLongValue() / value;
            return new LiteralExpr(LiteralMap.put(value, type));
          }
        }
      } else if (litr instanceof FloatLiteral) {
        double value = ((FloatLiteral) litr).getDoubleValue();
        if (value == 1.0)
          return la;
        if (value != 0.0) {
          if (la.isLiteralExpr()) {
            Object litl = ((LiteralExpr) la).getLiteral().getConstantValue();
            if (litl instanceof FloatLiteral) {
              value = ((FloatLiteral) litl).getDoubleValue() / value;
              return new LiteralExpr(LiteralMap.put(value, type));
            }
          } else {
            // If the divisor is a power of 2, we can safely do the
            // conversion to a multiply.
            long x = Double.doubleToRawLongBits(value);
            if (fpReorder || // It's a power of 2.
                ((0L == (x & 0x000fffffffffffffL)) &&
                 (x != 0x7ff0000000000000L) && // And not infinity 
                 (x != 0xfff000000000000L))) { // or negative infinity.
              return MultiplicationExpr.create(type, la, calcRecip(value, type));
            }
          }
        }
      }
    }
    return new DivisionExpr(type, la, ra);
  }

  private static LiteralExpr calcRecip(double value, Type type)
  {
    double recip = 1.0 / value;
    return new LiteralExpr(LiteralMap.put(recip, type));
  }

  public Expr copy()
  {
    return new DivisionExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int  se  = super.sideEffects() | SE_OVERFLOW;
    Expr arg = getRightArg();
    if (arg.isLiteralExpr()) {
      if (!((LiteralExpr) arg).isZero())
        return se;
    }
    return se | SE_DOMAIN;
  }

  public void visit(Predicate p)
  {
    p.visitDivisionExpr(this);
  }

  public String getDisplayLabel()
  {
    return "/";
  }

protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>          affines,
                                             scale.score.chords.LoopHeaderChord thisLoop)
  {
    AffineExpr lae = getLeftArg().getAffineExpr(affines, thisLoop);
    if (lae == null)
      return AffineExpr.notAffine;

    AffineExpr rae = getRightArg().getAffineExpr(affines, thisLoop);
    if (lae == null)
      return AffineExpr.notAffine;

    return AffineExpr.divide(lae, rae);
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

    Literal la = getLeftArg().getConstantValue(cvMap);
    Literal ra = getRightArg().getConstantValue(cvMap);

    r = Lattice.divide(getCoreType(), la, ra);

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
    Literal la = getLeftArg().getConstantValue();
    Literal ra = getRightArg().getConstantValue();

    return Lattice.divide(getCoreType(), la, ra);
  }
  
  public boolean isLeftDistributive()
  {
    Expr la = getLeftArg();
    return ((la instanceof AdditionExpr) ||
            (la instanceof SubtractionExpr));           
  }   

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5 + super.executionCostEstimate();
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return getLeftArg().isSimpleExpr() && getRightArg().isSimpleExpr();
  }

  /**
   * Return true if this expression may result in the generation of a
   * call to a subroutine.
   */
  public boolean mayGenerateCall()
  {
    return Machine.currentMachine.hasCapability(getCoreType().isRealType() ?
                                                Machine.HAS_NO_FP_DIVIDE :
                                                Machine.HAS_NO_INT_DIVIDE);
  }

  /**
   * Return a simplied equivalent expression.  If both the divisor and
   * the quotient are constants, we replace this expression with a
   * constant.  If just the divisor is a floating point constant and
   * floating point re-ordering is allowed, we replace this expression
   * with a multiply.  This method may modify the original expression.
   * This is used in the lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr reduce()
  {
    Expr la = getLeftArg();
    Expr ra = getRightArg();

    if (la.isLiteralExpr() &&
        ra.isLiteralExpr() &&
        getType().isSigned()) {
      setLeftArg(null);
      setRightArg(null);
      return ((LiteralExpr) la).divide(getType(), ra);
    }

    if (fpReorder && ra.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) ra).getLiteral();
      if (lit instanceof FloatLiteral) {
        setLeftArg(null);
        double v = ((FloatLiteral) lit).getDoubleValue();
        return MultiplicationExpr.create(getType(),
                                         la,
                                         calcRecip(v, lit.getType()));
      }
    }

    return this;
  }
}
