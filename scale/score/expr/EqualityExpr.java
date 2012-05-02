package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.BooleanType;
import scale.score.Predicate;

/**
 * This class represents the test for equality.
 * <p>
 * $Id: EqualityExpr.java,v 1.29 2007-10-18 15:56:37 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public final class EqualityExpr extends MatchExpr
{
  public EqualityExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * This method of creating a EqualityExpr instance will return a
   * reduced expression if possible.
   */
  public Expr create(Type t, Expr e1, Expr e2)
  {
    return new EqualityExpr(t, e1, e2);
  }

  public Expr copy()
  {
    return new EqualityExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  /**
   * Return the complement expression.
   * The complement of "==" is "!=".
   */
  public MatchExpr complement()
  {
    return new NotEqualExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitEqualityExpr(this);
  }

  public String getDisplayLabel()
  {
    return "==";
  }

  /**
   * Return an integer representing the type of comparison.
   */
  public CompareMode getMatchOp()
  {
    return CompareMode.EQ;
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

    r = Lattice.equal(getCoreType(), la, ra);

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

    return Lattice.equal(getCoreType(), la, ra);
  }
  
  /**
   * Return true if this expression is commutative.
   */
  public boolean isCommutative()
  {
    return true;
  }
}
