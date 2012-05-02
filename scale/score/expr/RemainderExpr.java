package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.score.Predicate;

/**
 * This class represents the remainder operation.
 * <p>
 * $Id: RemainderExpr.java,v 1.26 2007-04-27 18:04:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class RemainderExpr extends BinaryExpr
{
  public RemainderExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  public RemainderExpr(Expr e1, Expr e2)
  {
    this(e1.getType(), e1, e2);
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

  public Expr copy()
  {
    return new RemainderExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitRemainderExpr(this);
  }

  public String getDisplayLabel()
  {
    return "%";
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
    r = Lattice.remainder(getCoreType(), la, ra);

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
    return Lattice.remainder(getCoreType(), la, ra);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5 + super.executionCostEstimate();
  }
}
