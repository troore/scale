package scale.score.expr;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.expr.Literal;
import scale.score.Predicate;

/**
 * This class represents the bit complement operation.
 * <p>
 * $Id: BitComplementExpr.java,v 1.22 2007-04-27 18:04:35 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class BitComplementExpr extends UnaryExpr
{
  public BitComplementExpr(Type t,  Expr e1)
  {
    super(t, e1);
  }

  /**
   * The expression type is the same as the type of expression e1.
   */
  public BitComplementExpr(Expr e1)
  {
    this(e1.getType(), e1);
  }

  public Expr copy()
  {
    return new BitComplementExpr(getType(), getArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitBitComplementExpr(this);
  }

  public String getDisplayLabel()
  {
    return "~";
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
    r = Lattice.bitComplement(getCoreType(), la);

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
    return Lattice.bitComplement(getCoreType(), la);
  }
}
