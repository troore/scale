package scale.score.expr;

import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.BooleanType;
import scale.score.Predicate;
import scale.common.*;

/**
 * This class represents the boolean complement operation.
 * <p>
 * $Id: NotExpr.java,v 1.24 2007-04-27 18:04:37 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class NotExpr extends UnaryExpr
{
  public NotExpr(Type t, Expr e1)
  {
    super(t, e1);
  }

  public Expr copy()
  {
    return new  NotExpr(getType(), getArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitNotExpr(this);
  }

  public String getDisplayLabel()
  {
    return "!";
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
    r = Lattice.not(getCoreType(), la);

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
    return Lattice.not(getCoreType(), la);
  }

  /**
   * Return true if the result of the expression is either true (1) or
   * false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return true;
  }
}
