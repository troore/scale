package scale.clef.expr;

import java.lang.Boolean;
import scale.common.*;

import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents a boolean literal value.
 * <p>
 * $Id: BooleanLiteral.java,v 1.31 2007-03-21 13:31:53 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class BooleanLiteral extends Literal
{
  /**
   * The value.
   */
  private boolean value;

  public BooleanLiteral(Type t, boolean v)
  {
    super(t);
    t = t.getCoreType();
    assert t.isBooleanType() : "Invalid type " + t + " for boolean literal.";
    value = v;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    BooleanLiteral bl = (BooleanLiteral) exp;
    return (value == bl.value);
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
    p.visitBooleanLiteral(this);
  }

  /**
   * Get the string version of the literal using C syntax with symbols 'true' and 'false'.
   * @return a String already converted for display using C syntax.
   */
  public final String getGenericValue() 
  {
    return  (value ? "1" : "0");
  }

  /**
   * Return the boolean value of the literal.
   */
  public boolean getBooleanValue()
  {
    return value;
  }

  /**
   * Specify the boolean value of the literal.
   */
  protected final void setValue(boolean value)
  {
    this.value = value;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    if (value)
      return Boolean.TRUE.hashCode();
    else
      return Boolean.FALSE.hashCode();
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return Machine.currentMachine.executionCostEstimate(1);
  }

  /**
   * Return true if the value of this literal is known to be false.
   */
  public boolean isZero()
  {
    return !value;
  }

  /**
   *  Return true if the result of the expression is either true (1) or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return true;
  }
}
