package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.BooleanType;
import scale.score.Predicate;

/**
 * This class represents the boolean and function.
 * <p>
 * $Id: AndExpr.java,v 1.24 2007-04-27 18:04:35 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class AndExpr extends BinaryExpr
{
  public AndExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  public Expr copy()
  {
    return new AndExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitAndExpr(this);
  }

  public String getDisplayLabel()
  {
    return "&&";
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public scale.clef.expr.Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return  r;

    Literal la = getLeftArg().getConstantValue(cvMap);
    Literal ra = getRightArg().getConstantValue(cvMap);
    r = Lattice.andCond(getCoreType(), la, ra);

    cvMap.put(this, r);
    return r;
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   * Do not follow use-def links.
   */
  public scale.clef.expr.Literal getConstantValue()
  {
    Literal la = getLeftArg().getConstantValue();
    Literal ra = getRightArg().getConstantValue();
    return Lattice.andCond(getCoreType(), la, ra);
  }
}
