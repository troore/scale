package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;

/**
 * This class represents the complex type with a real and an imaginary
 * part.
 * <p>
 * $Id: ComplexType.java,v 1.51 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ComplexType extends NumericType 
{
  private static Vector<ComplexType> types; // A list of all the unique complex types.

  /**
   * The minimum number of bits allowed for the real part.
   */
  private int realMinbitSize;
  /**
   *  The minimum number of bits allowed for the imaginary part.
   */
  private int imaginaryMinbitSize;

  /**
   * Re-use an existing instance of a particular complex type.
   * If no equivalent complex type exists, create a new one.
   * @param realMinbitSize is the minimum number of bits for the real
   * part
   * @param imaginaryMinbitSize is the minimum number of bits for the
   * imaginary part
   */

  public static ComplexType create(int realMinbitSize, int imaginaryMinbitSize)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        ComplexType ta = types.elementAt(i);
        if ((ta.realMinbitSize == realMinbitSize) &&
            (ta.imaginaryMinbitSize == imaginaryMinbitSize)) {
          return ta;
        }
      }
    }
    ComplexType a = new ComplexType(realMinbitSize, imaginaryMinbitSize);
    return a;
  }

  private ComplexType(int r, int i)
  {
    realMinbitSize = r;
    imaginaryMinbitSize = i;
    if (types == null)
      types = new Vector<ComplexType>(2);
    types.addElement(this);
  }

  /**
   * Return the minimum number of bits allowed for the real part.
   */
  public final int getRealMinbitSize()
  {
    return realMinbitSize;
  }

  /**
   * Return the minimum number of bits allowed for the imaginary part.
   */
  public final int getImaginaryMinbitSize()
  {
    return imaginaryMinbitSize;
  }

  /**
   * Return the real type for the real part of the complex value.
   */
  public RealType getRealType()
  {
    return FloatType.create(realMinbitSize);
  }

  /**
   * Return the real type for the real part of the complex value.
   */
  public RealType getImaginaryType()
  {
    return FloatType.create(imaginaryMinbitSize);
  }

  public int bitSize()
  {
    return realMinbitSize + imaginaryMinbitSize;
  }

  /**
   * Calculate how many addressable memory units are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of bytes required to represent this type
   */
  public long memorySize(Machine machine)
  {
    return (machine.addressableMemoryUnits(imaginaryMinbitSize) +
            machine.addressableMemoryUnits(realMinbitSize));
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    int ra = machine.alignData(machine.addressableMemoryUnits(realMinbitSize));
    int ia = machine.alignData(machine.addressableMemoryUnits(imaginaryMinbitSize));
    if (ia > ra)
      ra = ia;
    return ra;
  }

  /**
   * Return true if type represents a floating point value.
   */
  public boolean isRealType()
  {
    return true;
  }

  public final ComplexType returnComplexType()
  {
    return this;
  }

  /**
   * Return true if type represents a complex value.
   */
  public boolean isComplexType()
  {
    return true;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("<complex ");
    buf.append(String.valueOf(realMinbitSize));
    buf.append(' ');
    buf.append(String.valueOf(imaginaryMinbitSize));
    buf.append('>');
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitComplexType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitComplexType(this);
  }

  /**
   * Return true if the types are equivalent.
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    if (tc == null)
      return false;

    if (tc.getClass() != getClass())
      return false;

    ComplexType t2 = (ComplexType) tc;
    return ((realMinbitSize == t2.realMinbitSize) &&
            (imaginaryMinbitSize == t2.imaginaryMinbitSize));
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<ComplexType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<ComplexType>();
    return types.elements();
  }

  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToCString()
  {
    int         trs = getRealMinbitSize();
    int         tis = getImaginaryMinbitSize();
    if ((trs <= 32) && (tis <= 32))
      return "complex";
    assert ((trs <= 64) && (tis <= 64)) : "Incorrect complex type " + this;
    return "doublecomplex";
  }

  /**
   * Map a type to a Fortran string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToF77String()
  {
    int         trs = getRealMinbitSize();
    int         tis = getImaginaryMinbitSize();
    if ((trs <= 32) && (tis <= 32))
      return "complex*8";
    if ((trs <= 64) && (tis <= 64))
      return "complex*16";
    return "complex*32";
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
