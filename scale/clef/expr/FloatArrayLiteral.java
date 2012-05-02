package scale.clef.expr;

import java.math.*;
import scale.common.*;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * A class which represents a floating point literal array.
 * <p>
 * $Id: FloatArrayLiteral.java,v 1.31 2007-10-04 19:58:05 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class FloatArrayLiteral extends Literal
{
  /**
   * The array of values.
   */
  private double[] value;
  /**
   * The count of values.
   */
  private int vcount;

  public FloatArrayLiteral(Type type, int size)
  {
    super(type);
    type = type.getCoreType();
    value = new double[size];
    vcount = 0;

    assert checkElementType(type) : "Invalid type for real array literal " + type;
  }

  private boolean checkElementType(Type type)
  {
    FixedArrayType at = type.getCoreType().returnFixedArrayType();
    if (at == null)
      return false;

    Type et = at.getElementType().getCoreType();
    while (true) {
      ArrayType at2 = et.getCoreType().returnArrayType();
      if (at2 == null)
        return et.isRealType();
      et = at2.getElementType().getCoreType();
    }
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;

    FloatArrayLiteral al = (FloatArrayLiteral) exp;

    if (vcount != al.vcount)
      return false;

    for (int i = 0; i < vcount; i++) {
      if (value[i] != al.value[i])
        return false;
    }

    return true;
  }

  /**
   * Return the size of the array.
   */
  public int size()
  {
    return vcount;
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
    p.visitFloatArrayLiteral(this);
  }

  /**
   * Return a String already converted for display using C syntax.
   */
  public final String getGenericValue() 
  {
    StringBuffer buf = new StringBuffer("");
    int          num = (vcount < 3) ? vcount : 3;
    for (int i = 0; i < num; i++) {
      if (i > 0)
        buf.append(",");
      buf.append(Double.toString(value[i]));
    }
    if (num < vcount)
      buf.append(",...");
    return buf.toString();
  }

  /**
   * Return the array of values of the literal.
   */
  public double[] getArrayValue()
  {
    return value;
  }

  /**
   * Return the i-th value.
   */
  public double getValue(int i)
  {
    return value[i];
  }

  /**
   * Set the i-th value.
   */
  public void setValue(int i, double value)
  {
    this.value[i] = value;
    if (i >= vcount)
      vcount = i + 1;
  }

  /**
   * Add a new value to the end of the array.
   */
  public final void addElement(double v)
  {
    value[vcount++] = v;
  }

  /**
   * Return the number of elements in the Literal.
   */
  public int getCount()
  {
    return value.length;
  }

  /**
   * Return the specified element of the constant.
   */
  public Literal getElement(long index) throws InvalidException
  {
    if ((index >= 0) && (index < value.length)) {
      ArrayType at = getCoreType().returnArrayType();
      if (at != null)
        return LiteralMap.put(value[(int) index], at.getElementType());
    }

    throw new InvalidException("No such element");
  }
}
