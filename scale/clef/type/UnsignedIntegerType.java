package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * The <tt>UnsignedIntegerType</tt> class represents a primitive
 * unsigned integer type.
 * <p>
 * $Id: UnsignedIntegerType.java,v 1.4 2007-10-04 19:58:09 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Examples of unsigned integer types are C's <tt>unsigned short</tt>,
 * <tt>unsigned int</tt>, and <tt>unsigned long</tt> types.  The size
 * of the type is specified as the minimum number of bits that are
 * required to represent a value of that type.
 */
public class UnsignedIntegerType extends IntegerType 
{
  private static Vector<UnsignedIntegerType> types; // A list of all the unique unsigned integer types

  /**
   * Re-use an existing instance of a particular integer type.
   * If no equivalent integer type exists, create a new one.
   */

  public static UnsignedIntegerType create(int bits)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        UnsignedIntegerType ta = types.elementAt(i);
        if (ta.bitSize() == bits)
          return ta;
      }
    }
    UnsignedIntegerType a = new UnsignedIntegerType(bits);
    return a;
  }

  private UnsignedIntegerType(int bits)
  {
    super(bits);
    if (types == null)
      types = new Vector<UnsignedIntegerType>(2);
    types.addElement(this);
  }

  /**
   * Return the equivalent signed type.
   */
  public Type getSignedType()
  {
    return SignedIntegerType.create(bitSize());
  }

  public final boolean isSigned()
  {
    return false;
  }

  public final UnsignedIntegerType returnUnsignedIntegerType()
  {
    return this;
  }

  public String toString()
  {
    String       str = "";
    StringBuffer buf = new StringBuffer("<");
    buf.append("ui");
    buf.append(bitSize());
    buf.append('>');
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitUnsignedIntegerType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitUnsignedIntegerType(this);
  }

  /**
   * Transform an integer value to be in a suitable range.  The value
   * is truncated according the the number of bits in the integer.
   */
  public long putValueInRange(long value)
  {
    int bs = bitSize();
    if (bs >= 64)
      return value;

    long mask = (1L << bs) - 1;
    return mask & value;
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<UnsignedIntegerType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<UnsignedIntegerType>();
    return types.elements();
  }


  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToCString()
  {
    switch (bitSize()) {
    case 8:  return "unsigned char";
    case 16: return "unsigned short";
    case 32: return "unsigned int";
    case 64: return ((Machine.currentMachine.getIntegerCalcType().bitSize() < 64) ?
                     "unsigned long long" :
                     "unsigned long");
    default:
      throw new scale.common.InternalError("Incorrect integer type " + this);
    }
  }

  /**
   * Map a type to a Fortran string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToF77String()
  {
    switch (bitSize()) {
    case 8:  return "logical";
    case 16: return "integer*2";
    case 32: return "integer*4";
    case 64: return "integer*8";
    default:
      throw new scale.common.InternalError("Incorrect integer type " + this);
    }
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
