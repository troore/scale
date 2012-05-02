package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.score.Predicate;

/**
 * This class represents the absolute value function.
 * <p>
 * $Id: AbsoluteValueExpr.java,v 1.24 2007-04-27 18:04:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class AbsoluteValueExpr extends UnaryExpr
{
  public AbsoluteValueExpr(Type t, Expr e1)
  {
    super(t, e1);
  }

  /**
   * The expression type is the same as the argument type.
   */
  public AbsoluteValueExpr(Expr e1)
  {
    this(e1.getType(), e1);
  }

  public Expr copy()
  {
    return new AbsoluteValueExpr(getType(), getArg().copy());
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return getCoreType().isRealType() ? SE_NONE : SE_OVERFLOW;
  }

  public void visit(Predicate p)
  {
    p.visitAbsoluteValueExpr(this);
  }

  public String getDisplayLabel()
  {
    return "abs";
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

    Literal la = getArg().getConstantValue(cvMap);
    r = Lattice.abs(getCoreType(), la);

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
    Literal la = getArg().getConstantValue();
    return Lattice.abs(getCoreType(), la);
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return getArg().isSimpleExpr();
  }
}
