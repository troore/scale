package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * The <tt>IntegerType</tt> class represents a primitive integer type.
 * <p>
 * $Id: IntegerType.java,v 1.62 2007-03-21 13:31:56 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Examples of integer types are C's <tt>short</tt>, <tt>int</tt>, and
 * <tt>long</tt> types.  The size of the type is specified as the
 * minimum number of bits that are required to represent a value of
 * that type.
 */
public abstract class IntegerType extends NumericType 
{
  /**
   * If <code>true</code>, the default for the C <code>char</code>
   * type is signed.  If <code>false</code>, the default for the C
   * <code>char</code> type is unsigned.
   */
  public static boolean cCharsAreSigned = true;

  /**
   * The minimum number of bits that must be used for this type.
   */
  private int minbitSize;     

  public IntegerType(int bits)
  {
    this.minbitSize = bits; /*** This is really target dependent. **/
  }

  public String toStringShort()
  {
    return toString();
  }

  public void visit(Predicate p)
  {
    p.visitIntegerType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitIntegerType(this);
  }

  /**
   * Return the equivalent signed type.
   */
  public abstract Type getSignedType();
  /**
   * Transform an integer value to be in a suitable range.  The value
   * is truncated according the the number of bits in the integer.
   */
  public abstract long putValueInRange(long value);
  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public abstract String mapTypeToCString();
  /**
   * Map a type to a Fortran string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public abstract String mapTypeToF77String();

  /**
   * Return true if type represents an integer value.
   */
  public final boolean isIntegerType()
  {
    return true;
  }

  public final IntegerType returnIntegerType()
  {
    return this;
  }

  public final int bitSize()
  {
    return minbitSize; 
  }

  /**
   * Return true if the types are equivalent.
   */
  public final boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    if (tc == null)
      return false;

    if (tc.getClass() != getClass())
      return false;

    IntegerType t2 = (IntegerType) tc;
    return (minbitSize == t2.minbitSize);
  }
}
