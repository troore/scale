package scale.clef.expr;

import java.math.*;
import scale.common.*;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * A class which represents a integer literal array.
 * <p>
 * $Id: IntArrayLiteral.java,v 1.37 2007-10-04 19:58:05 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class IntArrayLiteral extends Literal
{
  /**
   * The array of values.
   */
  private long[] value;
  /**
   * The number of values.
   */
  private int  vcount;
  /**
   * The mask to use when adding values.
   */
  private long mask;

  public IntArrayLiteral(Type type, int size)
  {
    super(type);
    type = type.getCoreType();
    value = new long[size];
    vcount = 0;

    FixedArrayType ft = type.returnFixedArrayType();

    assert (ft != null) : "Invalid type for integer array literal " + type;

    Type et = basicElementType(ft);

    mask = 0xffffffffffffffffL;
    if (et.isSigned())
      return;

    IntegerType it  = et.getCoreType().returnIntegerType();
    if (it != null) {
      int mbs = it.bitSize();
      if (mbs < 64)
        mask = ((1L << mbs) - 1);
    } else if (!et.isEnumerationType())
      throw new scale.common.InternalError("Invalid type for integer array literal " + type);
  }

  private Type basicElementType(FixedArrayType type)
  {
    Type et = type.getElementType().getCoreType();
    while (true) {
      ArrayType at = et.getCoreType().returnArrayType();
      if (at == null)
        return et;
      et = at.getElementType().getCoreType();
    }
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;

    IntArrayLiteral al = (IntArrayLiteral) exp;

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
    p.visitIntArrayLiteral(this);
  }

  /**
   * Get the string version of the literal using C syntax.
   * @return a String already converted for display using C syntax.
   */
  public final String getGenericValue() 
  {
    Type    t      =  getCoreType();
    boolean signed = false;
    boolean isLong = false;

    ArrayType at = t.getCoreType().returnArrayType();
    if (at != null) {
      t = at.getElementType().getCoreType();
      signed = t.isSigned();
      IntegerType it = t.returnIntegerType();
      if (it != null) {
        int bs  = it.bitSize();
        if (bs > 32)
          isLong = true;
      }
    }

    StringBuffer buf = new StringBuffer("");
    int          num = (vcount < 3) ? vcount : 3;

    for (int i = 0; i < num; i++) {
      if (i > 0)
        buf.append(",");
      if (signed)
        buf.append(Long.toString(value[i]));
      else {
        buf.append("0x");
        buf.append(Long.toHexString(value[i]));
        buf.append('U');
      }
      if (isLong)
        buf.append('L');
    }
    if (num < vcount)
      buf.append(",...");

    return buf.toString();
  }

  /**
   * Return the array of values of the literal.
   */
  public long[] getArrayValue()
  {
    return value;
  }

  /**
   * Return the i-th value.
   */
  public long getValue(int i)
  {
    return value[i];
  }

  /**
   * Set the i-th value.
   */
  public void setValue(int i, long value)
  {
    this.value[i] = value & mask;
    if (i >= vcount)
      vcount = i + 1;
  }

  /**
   * Add a new value to the end of the array.
   */
  public final void addElement(long v)
  {
    value[vcount++] = v & mask;
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
