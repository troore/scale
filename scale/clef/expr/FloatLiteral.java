package scale.clef.expr;

import java.math.*;
import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents a floating point literal value.
 * <p>
 * $Id: FloatLiteral.java,v 1.39 2007-08-03 15:15:36 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class FloatLiteral extends Literal
{
  /**
   * The value.
   */
  private double value;

  public FloatLiteral(Type type, double value)
  {
    super(type);

    FloatType ft = type.getCoreType().returnFloatType();
    if (ft != null)
      value = ft.putValueInRange(value);

    this.value = value;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    FloatLiteral fl = (FloatLiteral) exp;
    if (value == 0.0)
      return (Double.doubleToLongBits(value) == Double.doubleToLongBits(fl.value));
    return (value == fl.value);
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
    p.visitFloatLiteral(this);
  }

  /**
   * Get the string version of the literal using C syntax.
   * @return a String already converted for display using C syntax.
   */
  public final String getGenericValue() 
  {
    return formatRealValue(value);
  }

  protected final void setValue(double value)
  {
    this.value = value;
  }

  /**
   * Return the value of the double literal.
   */
  public double getDoubleValue()
  {
    return value;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return Double.doubleToLongBits(value);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return Machine.currentMachine.executionCostEstimate(value);
  }

  /**
   * Return true if the value of this literal is known to be zero.
   */
  public boolean isZero()
  {
    return (value == 0.0);
  }

  /**
   * Return true if the value of this literal is known to be one.
   */
  public boolean isOne()
  {
    return value == 1.0;
  }
}
