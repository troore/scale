package scale.clef.decl;

import scale.common.*;

import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents a component of an aggregate data structure.
 * <p>
 * $Id: FieldDecl.java,v 1.40 2007-10-04 19:58:04 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class FieldDecl extends ValueDecl
{
  private AggregateType myStruct; // The aggregate structure (record or class) to which I belong.

  private long fieldOffset;    // The offset of the field from the start of the structure.
  private Accessibility accessibility; // The accessibility of the field.
  private byte bits;           // The number of bits specified for the field - 0 means none specified.
  private byte bitOffset;      // The bit offset from the appropriate (word) boundary in the structure.
  private byte fieldAlignment; // The alignment required for the field.

  /**
   * Create a declaration of a field of a structure.
   * @param name is the name of the field
   * @param type is the type of the field
   * @param fieldOffset is the offset in bytes of the field in the
   * structure
   * @param fieldAlignment is the required alignment of the field in
   * the structure
   * @param initialValue is the initial value for the field
   * @param bits specifies the size of a C bit field
   */
  public FieldDecl(String     name,
                   Type       type,
                   long       fieldOffset,
                   int        fieldAlignment,
                   Expression initialValue,
                   int        bits)
  {
    super(name, type, initialValue);
    this.bits           = (byte) bits;
    this.fieldOffset    = fieldOffset;
    this.fieldAlignment = (byte) fieldAlignment;
    this.accessibility  = Accessibility.PUBLIC;
  }

  /**
   * Create a declaration of a field of a structure.
   * @param name is the name of the field
   * @param type is the type of the field
   * @param fieldOffset is the offset in bytes of the field in the
   * structure
   * @param fieldAlignment is the required alignment of the field in
   * the structure
   * @param initialValue is the initial value for the field
   */
  public FieldDecl(String     name,
                   Type       type,
                   long       fieldOffset,
                   int        fieldAlignment,
                   Expression initialValue)
  {
    this(name, type, fieldOffset, fieldAlignment, initialValue, 0);
  }

  public FieldDecl(String name, Type type, long fieldOffset, int fieldAlignment)
  {
    this(name, type, fieldOffset, fieldAlignment, null, 0);
  }

  public FieldDecl(String name, Type type, long fieldOffset)
  {
    this(name, type, fieldOffset, 0, null, 0);
  }

  public FieldDecl(String name, Type type)
  {
    this(name, type, 0, 0, null, 0);
  }

  /**
   * Specify the accessibility of the declaration.
   * @param accessibility is the accessibility of the declaration
   * @see Accessibility
   */
  public final void setAccessibility(Accessibility accessibility)
  {
      this.accessibility = accessibility;
  }

  /**
   * Return the declaration accessibility.
   * @see Accessibility
   */
  public final Accessibility accessibility()
  {
    return accessibility;
  }

  /**
   * Return the number of bits specified for the field - 0 means none
   * specified.
   */
  public final int getBits()
  {
    return bits;
  }

  /**
   * Set the number of bits specified for the field - 0 means none
   * specified.
   */
  public final void setBits(int bits, int bitOffset)
  {
    assert ((bits >= 0) && (bits <= 127)) : "Invalid bit field size: " + bits;
    this.bits = (byte) bits;
    this.bitOffset = (byte) bitOffset;
  }

  /**
   * Return the bit offset from the appropriate (word) boundary in the
   * structure.
   */
  public final int getBitOffset()
  {
    return bitOffset;
  }

  /**
   * Set the bit offset from the appropriate (word) boundary in the
   * structure.
   */
  public final void setBitOffset(int bitOffset)
  {
    assert ((bits >= 0) && (bits <= 127)) :
      "Invalid bit field offset: " + bitOffset;
    this.bitOffset = (byte) bitOffset;
  }

  /**
   * Return the required field offset for the field.
   */
  public final long getFieldOffset()
  {
    return fieldOffset;
  }

  /**
   * Set the required field offset for the field.
   */
  public final void setFieldOffset(long fieldOffset)
  {
    if ((bits == 0) && (this.fieldOffset > 0))
      return;
    assert (fieldOffset >= 0) : "Invalid field offset: " + fieldOffset;
//     assert ((this.fieldOffset > 0) ? (this.fieldOffset == fieldOffset) : true) :
//       "Invalid field offset: " + myStruct + " " + getName() + " " + this.fieldOffset + " " + fieldOffset;
    this.fieldOffset = fieldOffset;
  }

  /**
   * Return the required field alignment for the field.
   */
  public final int getFieldAlignment()
  {
    return fieldAlignment;
  }

  /**
   * Set the required field alignment for the field.
   */
  public final void setFieldAlignment(int fieldAlignment)
  {
    assert ((fieldAlignment >= 0) && (fieldAlignment <= 127)) :
      "Invalid field alignment: " + fieldAlignment;
    this.fieldAlignment = (byte) fieldAlignment;
  }

  /**
   * Set the target attributes of the field.
   */
  public final void setFieldTargetAttributes(long fieldOffset,
                                             int  fieldAlignment,
                                             int  bitOffset)
  {
    setFieldOffset(fieldOffset);
    setFieldAlignment(fieldAlignment);
    setBitOffset(bitOffset);
  }

  public void visit(Predicate p)
  {
    p.visitFieldDecl(this);
  }

  /**
   * Return the initializer for the field.
   * For C programs this is always null.
   */
  public final Expression getInitialValue()
  {
    return getValue();
  }

  /**
   * Specify the initializer for the field.
   * For C programs this is always null.
   */
  protected final void setInitialValue(Expression i)
  {
    setValue(i);
  }

  /**
   * Return the class that this field belong to, if applicable.
   */
  public final AggregateType getMyStruct()
  {
    return myStruct;
  }

  /**
   * Specify the structure to which this FieldDecl belongs.
   * @param n the aggregate structure (record or class) to which I belong
   */
  public final void setMyStruct(AggregateType n)
  {
    myStruct = n;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer("\"");
    buf.append(getName());
    buf.append("\" ");
    buf.append(getType().toStringShort());
    buf.append(' ');
    buf.append(fieldOffset);
    if (bits > 0) {
      buf.append(" :");
      buf.append(bits);
      buf.append(' ');
      buf.append(bitOffset);
    }
    return buf.toString();
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name)
  {
    FieldDecl fd = new FieldDecl(name,
                                 getType(),
                                 fieldOffset,
                                 fieldAlignment,
                                 getValue(),
                                 bits);
    fd.myStruct      = myStruct;
    fd.accessibility = accessibility;
    return fd;
  }

  /**
   * Return true if the field is not aligned on a natural boundary for
   * its type.
   */
  public boolean isPackedField(Machine machine)
  {
    return (0 != (fieldOffset % getCoreType().alignment(machine)));
  }

  public final boolean isFieldDecl()
  {
    return true;
  }

  public final FieldDecl returnFieldDecl()
  {
    return this;
  }
}
