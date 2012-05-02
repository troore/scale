package scale.clef.expr;

import java.math.*;
import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents a floating point literal value.
 * <p>
 * $Id: ComplexLiteral.java,v 1.4 2007-10-04 19:58:05 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ComplexLiteral extends Literal
{
  /**
   * The value.
   */
  private double real;
  private double imag;

  public ComplexLiteral(Type type, double real, double imag)
  {
    super(type);
    this.real = real;
    this.imag = imag;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    ComplexLiteral fl = (ComplexLiteral) exp;
    return (real == fl.real) && (imag == fl.imag);
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(getGenericValue());
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitComplexLiteral(this);
  }

  /**
   * Get the string version of the literal using C syntax.
   * @return a String already converted for display using C syntax.
   */
  public final String getGenericValue() 
  {
    StringBuffer buf = new StringBuffer(getCoreType().mapTypeToCString());
    buf.append('(');
    buf.append(formatRealValue(real));
    buf.append(',');
    buf.append(formatRealValue(imag));
    buf.append(')');
    return buf.toString();
  }

  protected final void setValue(double real, double imag)
  {
    this.real = real;
    this.imag = imag;
  }

  /**
   * Return the real part.
   */
  public double getReal()
  {
    return real;
  }

  /**
   * Return the imaginary part as a {@link Literal Literal}.
   */
  public Literal getRealPart()
  {
    return LiteralMap.put(real, ((ComplexType) getCoreType()).getRealType());
  }

  /**
   * Return the imaginary part.
   */
  public double getImaginary()
  {
    return imag;
  }

  /**
   * Return the imaginary part as a {@link Literal Literal}.
   */
  public Literal getImaginaryPart()
  {
    return LiteralMap.put(imag, ((ComplexType) getCoreType()).getImaginaryType());
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return Double.doubleToLongBits(real) ^ Double.doubleToLongBits(imag);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return (Machine.currentMachine.executionCostEstimate(real) +
            Machine.currentMachine.executionCostEstimate(imag));
  }

  /**
   * Return true if the value of this literal is known to be zero.
   */
  public boolean isZero()
  {
    return (real == 0.0) && (imag == 0.0);
  }

  /**
   * Return true if the value of this literal is known to be one.
   */
  public boolean isOne()
  {
    return (real == 1.0) && (imag == 0.0);
  }
}
