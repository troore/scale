package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.BooleanType;
import scale.score.Predicate;

/**
 * This class represents the test for greater than.
 * <p>
 * $Id: GreaterExpr.java,v 1.28 2007-10-17 13:46:37 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public final class GreaterExpr extends MatchExpr
{
  public GreaterExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * This method of creating a GreaterExpr instance will return a
   * reduced expression if possible.
   */
  public Expr create(Type t, Expr e1, Expr e2)
  {
    return new GreaterExpr(t, e1, e2);
  }

  public Expr copy()
  {
    return new GreaterExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  /**
   * Return the complement expression.
   * The complement of "&gt;" is "&lt;=""&gt;".
   */
  public MatchExpr complement()
  {
    return new LessEqualExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitGreaterExpr(this);
  }

  public String getDisplayLabel()
  {
    return "GT";
  }

  /**
   * Return an integer representing the type of comparison.
   */
  public CompareMode getMatchOp()
  {
    return CompareMode.GT;
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
      r = Lattice.greater(getCoreType(), lac, rac);
    else
      r = Lattice.greaterUnsigned(getCoreType(), lac, rac);

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
      return Lattice.greater(getCoreType(), lac, rac);

    return Lattice.greaterUnsigned(getCoreType(), lac, rac);
  }
}
