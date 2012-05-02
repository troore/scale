package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.BooleanType;
import scale.score.Predicate;

/**
 * This class represents the test for greater or equal.
 * <p>
 * $Id: GreaterEqualExpr.java,v 1.28 2007-10-17 13:46:37 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public final class GreaterEqualExpr extends MatchExpr
{
  public GreaterEqualExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * This method of creating a GreaterEqualExpr instance will return a
   * reduced expression if possible.
   */
  public Expr create(Type t, Expr e1, Expr e2)
  {
    return new GreaterEqualExpr(t, e1, e2);
  }

  public Expr copy()
  {
    return new GreaterEqualExpr(getType(),
                                getLeftArg().copy(),
                                getRightArg().copy());
  }

  /**
   * Return the complement expression.
   * The complement of "&gt;=" is "&lt;".
   */
  public MatchExpr complement()
  {
    return new LessExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitGreaterEqualExpr(this);
  }

  public String getDisplayLabel()
  {
    return "GE";
  }

  /**
   * Return an integer representing the type of comparison.
   */
  public CompareMode getMatchOp()
  {
    return CompareMode.GE;
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
      r = Lattice.greaterEqual(getCoreType(), lac, rac);
    else
      r = Lattice.greaterEqualUnsigned(getCoreType(), lac, rac);

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
      return Lattice.greaterEqual(getCoreType(), lac, rac);

    return Lattice.greaterEqualUnsigned(getCoreType(), lac, rac);
  }
}
