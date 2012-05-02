package scale.clef.expr;

import java.math.*;
import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents a integer literal value including address constants.
 * <p>
 * $Id: IntLiteral.java,v 1.50 2007-03-21 13:31:53 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Integer literals include address constants.
 */
public class IntLiteral extends Literal
{
  /**
   * If true, {@link #getGenericValue getGenericValue} will return
   * unsigned values, between 0 and 1000, in decimal instead of hex
   * notation.  This switch is used for debugging.
   */
  public static boolean useDecimal = false;
  /**
   * The value.
   */
  private long value;

  public IntLiteral(Type type, long value)
  {
    super(type);

    // Don't sign extend unsigned values.

    IntegerType it = type.getCoreType().returnIntegerType();
    if (it != null)
      value = it.putValueInRange(value);

    this.value = value;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    IntLiteral il = (IntLiteral) exp;
    return (value == il.value);
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
    p.visitIntLiteral(this);
  }

  /**
   * Return a String already converted for display using C syntax.
   * Get the string version of the literal using C syntax.
   */
  public final String getGenericValue() 
  {
    if (value == 0)
      return "0";

    StringBuffer buf = new StringBuffer("");
    Type         t   = getCoreType();

    if (t.isSigned() || (useDecimal && (value >= 0) && (value <= 1000)))
      buf.append(Long.toString(value));
    else {
      buf.append("0x");
      buf.append(Long.toHexString(value));
      buf.append('U');
    }

    IntegerType it = t.returnIntegerType();
    if (it != null) {
      int bs  = it.bitSize();
      if (bs > 32) {
        if (Machine.currentMachine.getIntegerCalcType().bitSize() < 64)
          buf.append("LL");
        else
          buf.append('L');
      }
    }

    return buf.toString();
  }

  /**
   * Specify the value of this literal. <b>This method must be used
   * with care.  If the literal has been memoized (see {@link
   * scale.clef.LiteralMap LiteralMap}) using this method will cause
   * hard to find problems.</b>
   */
  public final void setValue(long value)
  {
    this.value = value;
  }

  /**
   * Return the value of the integer literal.
   */
  public long getLongValue()
  {
    return value;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return value;
  }

  /**
   * Return linearity of literal.
   * Integer literals have linearity of 0.
   * All other literals have linearity of 2.
   */
  public int linearity()
  {
    return 0;
  }

  /**
   * Return the coefficient value.
   */
  public int findCoefficient()
  {
      return (int) value;
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
    return value == 0;
  }

  /**
   * Return true if the value of this literal is known to be one.
   */
  public boolean isOne()
  {
    return value == 1;
  }

  /**
   *  Return true if the result of the expression is either true (1) or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return (value == 1) || (value == 0);
  }

  /**
   * Return the value of the literal as a double value.
   * Care must be taken when converting unsigned long values.
   */
  public double convertToDouble()
  {
    if (getCoreType().isSigned())
      return (double) value;

    return ((double) (value & 0xffffffffL)) +
           (((double) ((value >> 32) & 0xffffffffL)) * 4294967296.0);
  }

  /**
   * Return the value of the literal as a double value.
   * Care must be taken when converting unsigned long values.
   */
  public float convertToFloat()
  {
    if (getCoreType().isSigned())
      return (float) value;

    return (float) (((double) (value & 0xffffffffL)) +
                   (((double) ((value >> 32) & 0xffffffffL)) * 4294967296.0));
  }
}
