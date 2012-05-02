package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents the operation using two values to create a complex value.
 * <p>
 * $Id: ComplexOp.java,v 1.8 2006-09-10 19:35:05 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class ComplexOp extends DyadicOp
{
  /**
   * Create an instance of a complex value from two expressions.
   * @param resultType is the type of the operation
   * @param real specifies the real part
   * @param imag specifies the imaginary part
   */
  public ComplexOp(ComplexType resultType, Expression real, Expression imag)
  {
    super(resultType, real, imag);
  }

  public void visit(Predicate p)
  {
    p.visitComplexOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr1().getConstantValue();
    Literal ra = getExpr2().getConstantValue();
    try {
      double realv = Lattice.convertToDoubleValue(la);
      double imagv = Lattice.convertToDoubleValue(ra);
      return new ComplexLiteral(getType(), realv, imagv);
    } catch (scale.common.InvalidException ex) {
      return Lattice.Bot;
    }
  }
}
