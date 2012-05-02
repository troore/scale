package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents a literal string value.
 * <p>
 * $Id: StringLiteral.java,v 1.44 2007-10-04 19:58:06 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result type is the type of the literal.
 */
public class StringLiteral extends Literal
{
  /**
   * The value of the string literal.
   */
  private String value;

  public StringLiteral(Type type, String v)
  {
    super(type);
    Type t = type.getCoreType();
    assert (t.isArrayType() || (t instanceof FortranCharType)):
      "Invalid type " + t + " for string literal.";
    value = v;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    StringLiteral sl = (StringLiteral) exp;
    return value.equals(sl.value);
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
    p.visitStringLiteral(this);
  }

  /**
   * Get the string version of the literal using C syntax.
   * @return a String already converted for display using C syntax.
   */
  public final String getGenericValue() 
  {
    String v = value;
    int l = v.length();
    if ((l > 0) && (v.charAt(l - 1) == 0))
      v = v.substring(0, l - 1);
    StringBuffer buf = new StringBuffer("\"");
    buf.append(getDisplayString(v));
    buf.append('"');
    return buf.toString();
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer("\"");
    int          len = value.length();

    if (len > 10)
      len = 10;

    buf.append(value.substring(0, len));

    if (len != value.length())
      buf.append("...");

    buf.append('"');
    return getDisplayString(buf.toString());
  }

  /**
   * Return the value of the literal.
   */
  public String getStringValue()
  {
    return value;
  }

  /**
   * Return the String value.
   */
  public String getString()
  {
    return value;
  }

  /**
   * Specify the String value.
   */
  protected final void setValue(String value)
  {
    this.value = value;
  }

  /**
   * Return the number of elements in the Literal.
   */
  public int getCount()
  {
    return value.length();
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return value.hashCode();
  }
}
