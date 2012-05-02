package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.score.*;
import scale.score.dependence.*;
import scale.clef.type.Type;

/**
 * This class represents the operation max(a,b).
 * <p>
 * $Id: MaxExpr.java,v 1.33 2007-04-27 18:04:37 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class MaxExpr extends BinaryExpr
{
  public MaxExpr(Type type, Expr e1, Expr e2)
  {
    super(type,  e1, e2);
  }

  public Expr copy()
  {
    return new MaxExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitMaxExpr(this);
  }

  public String getDisplayLabel()
  {
    return "Max";
  }

  /**
   * Return the constant value of the expression
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

    r = Lattice.maximum(getCoreType(), la, ra);

    cvMap.put(this, r);
    return r;
  }

  /**
   * Return the constant value of the expression
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getLeftArg().getConstantValue();
    Literal ra = getRightArg().getConstantValue();

    return Lattice.maximum(getCoreType(), la, ra);
  }
  
  /**
   * Return true if this expression is commutative.
   */
  public boolean isCommutativen()
  {
    return true;
  }
}
