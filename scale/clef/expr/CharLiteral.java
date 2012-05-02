package scale.clef.expr;

import java.lang.Character;
import scale.common.*;

import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents a <tt>char<tt> literal value.
 * <p>
 * $Id: CharLiteral.java,v 1.28 2005-06-16 20:56:28 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class CharLiteral extends Literal
{
  /**
   * The value.
   */
  private char value;

  public CharLiteral(Type type, char value)
  {
    super(type);
    this.value = value;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    CharLiteral cl = (CharLiteral) exp;
    return (value == cl.value);
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
    p.visitCharLiteral(this);
  }

  /**
   * Get the string version of the literal using C syntax.
   * @return a String already converted for display using C syntax.
   */
  public final String getGenericValue() 
  {
    return "'"  + getDisplayString(String.valueOf(value)) + "'";
  }

  /**
   * Return the value of the literal.
   */
  public char getCharacterValue()
  {
    return value;
  }

  /**
   * Set the value of the literal.
   */
  protected final void setValue(char value)
  {
    this.value = value;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return (long) value;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return Machine.currentMachine.executionCostEstimate(value);
  }
}
