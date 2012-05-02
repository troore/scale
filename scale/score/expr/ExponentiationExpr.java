package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.score.Predicate;
import scale.score.Note;

/**
 * This class represents the operation A to the power B.
 * <p>
 * $Id: ExponentiationExpr.java,v 1.28 2007-04-27 18:04:36 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ExponentiationExpr extends BinaryExpr
{
  public ExponentiationExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * The expression type is the same as the type of expression e1.
   */
  public ExponentiationExpr(Expr e1, Expr e2)
  {
    this(e1.getType(), e1, e2);
  }

  public Expr copy()
  {
    return new ExponentiationExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | SE_OVERFLOW;
  }

  public void visit(Predicate p)
  {
    p.visitExponentiationExpr(this);
  }

  public String getDisplayLabel()
  {
    return "**";
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

    r = Lattice.power(getCoreType(), la, ra);

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

    return Lattice.power(getCoreType(), la, ra);
  }
}
