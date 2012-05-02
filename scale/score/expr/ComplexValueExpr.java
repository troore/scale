package scale.score.expr;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.expr.Literal;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.ComplexLiteral;
import scale.score.Predicate;

/**
 * This class represents the combining of two real values into one
 * complex value.
 * <p>
 * $Id: ComplexValueExpr.java,v 1.21 2007-04-27 18:04:35 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ComplexValueExpr extends BinaryExpr 
{

  public ComplexValueExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * The expression type is the same as the type of expression e1.
   */
  public ComplexValueExpr(Expr e1, Expr e2)
  {
    this(e1.getType(), e1, e2);
  }

  public Expr copy()
  {
    return new ComplexValueExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  /**
   * @return the real part of a complex value
   */
  public Expr getReal()
  {
    return getOperand(0);
  }

  /**
   * Return the imaginary part of a complex value.
   */
  public Expr getImaginary()
  {
    return getOperand(1);
  }

  public void visit(Predicate p)
  {
    p.visitComplexValueExpr(this);
  }

  public String getDisplayLabel()
  {
    return "cmplx";
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

    double realv = 0.0;
    double imagv = 0.0;

    Literal la = getLeftArg().getConstantValue(cvMap);
    if (la instanceof FloatLiteral)
      realv = ((FloatLiteral) la).getDoubleValue();
    else
      return Lattice.Bot;

    Literal ra = getRightArg().getConstantValue(cvMap);
    if (ra instanceof FloatLiteral)
      imagv = ((FloatLiteral) ra).getDoubleValue();
    else
      return Lattice.Bot;


    r = new ComplexLiteral(getType(), realv, imagv);

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
    double realv = 0.0;
    double imagv = 0.0;

    Literal la = getLeftArg().getConstantValue();
    if (la instanceof FloatLiteral)
      realv = ((FloatLiteral) la).getDoubleValue();
    else
      return Lattice.Bot;

    Literal ra = getRightArg().getConstantValue();
    if (ra instanceof FloatLiteral)
      imagv = ((FloatLiteral) ra).getDoubleValue();
    else
      return Lattice.Bot;

    return new ComplexLiteral(getType(), realv, imagv);
  }
}
