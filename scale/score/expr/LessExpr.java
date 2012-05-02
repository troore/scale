package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.BooleanType;
import scale.score.Predicate;

/**
 * This class represents the test for less than.
 * <p>
 * $Id: LessExpr.java,v 1.28 2007-10-17 13:46:37 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public final class LessExpr extends MatchExpr
{
  public LessExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * This method of creating a LessExpr instance will return a
   * reduced expression if possible.
   */
  public Expr create(Type t, Expr e1, Expr e2)
  {
    return new LessExpr(t, e1, e2);
  }

  public Expr copy()
  {
    return new LessExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  /**
   * Return the complement expression.
   * The complement of "&lt;" is "&gt;=".
   */
  public MatchExpr complement()
  {
    return new GreaterEqualExpr(getType(),
                                getLeftArg().copy(),
                                getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitLessExpr(this);
  }

  public String getDisplayLabel()
  {
    return "LT";
  }

  /**
   * Return an integer representing the type of comparison.
   */
  public CompareMode getMatchOp()
  {
    return CompareMode.LT;
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

    Expr la = getLeftArg();
    Expr ra = getRightArg();

    Literal lac = getLeftArg().getConstantValue(cvMap);
    Literal rac = getRightArg().getConstantValue(cvMap);
    if (la.getType().isSigned())
      r = Lattice.less(getCoreType(), lac, rac);
    else
      r = Lattice.lessUnsigned(getCoreType(), lac, rac);

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
    Expr la = getLeftArg();
    Expr ra = getRightArg();

    Literal lac = getLeftArg().getConstantValue();
    Literal rac = getRightArg().getConstantValue();
    if (la.getType().isSigned())
      return Lattice.less(getCoreType(), lac, rac);

    return Lattice.lessUnsigned(getCoreType(), lac, rac);
  }
}
