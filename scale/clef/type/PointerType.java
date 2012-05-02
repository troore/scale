package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;

/**
 * The PointerType represents the type address of some other type.
 * <p>
 * $Id: PointerType.java,v 1.58 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class PointerType extends AtomicType 
{
  private static Vector<PointerType> types;      // A list of all the unique pointer types.
  private static int    minBitSize; // The number of bits required for a pointer.

  /**
   * Specify the size of a pointer in bits.
   */
  public static void setMinBitSize(int bitSize)
  {
    minBitSize = bitSize;
  }

  /**
   * Specify the size of a pointer in bits.
   */
  public static int getMinBitSize()
  {
    return minBitSize;
  }

  /**
   * The type pointed to.
   */
  private Type pointedTo;

  /**
   * Re-use an existing instance of a particular pointer type.
   * If no equivalent pointer type exists, create a new one.
   * @param pointedTo the type pointed to
   */

  public static PointerType create(Type pointedTo)
  {
    assert (pointedTo != null) : "Invalid pointer type.";
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        PointerType ta = types.elementAt(i);
        if (ta.pointedTo == pointedTo) {
          return ta;
        }
      }
    }
    PointerType a = new PointerType(pointedTo);
    if (types == null)
      types = new Vector<PointerType>(2);
    types.addElement(a);
    return a;
  }

  protected PointerType(Type pointedTo)
  {
    super();
    this.pointedTo = pointedTo;
  }

  public final Type getPointedTo()
  {
    return pointedTo;
  }

  public int bitSize()
  {
    return minBitSize;
  }

  /**
   * Return the number of bytes required to represent this type.
   */
  public static int addressableMemorySize(Machine machine)
  {
    return machine.addressableMemoryUnits(minBitSize);
  }

  /**
   * Return true if type represents an address.
   */
  public boolean isPointerType()
  {
    return true;
  }

  public final PointerType returnPointerType()
  {
    return this;
  }

  public String toString()
  {
    String str = " ";
    if (pointedTo != null)
      str = pointedTo.toString();
    StringBuffer buf = new StringBuffer("<");
    if (str.charAt(0) == '<')
      buf.append(str.substring(1, str.length() - 1));
    else
      buf.append(str);
    buf.append("*>");
    return buf.toString();
  }

  public String toStringShort()
  {
    String str = " ";
    if (pointedTo != null)
      str = pointedTo.toStringShort();
    StringBuffer buf = new StringBuffer("<");
    if (str.charAt(0) == '<')
      buf.append(str.substring(1, str.length() - 1));
    else
      buf.append(str);
    buf.append("*>");
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitPointerType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitPointerType(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return pointedTo;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
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

    PointerType t2 = (PointerType) tc;
    if (pointedTo != null)
      return pointedTo.equivalent(t2.pointedTo);

    return false;
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<PointerType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<PointerType>();
    return types.elements();
  }

  /**
   * Calculate how many addressable memory units are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of addressable memory units required to
   * represent this type
   */
  public long memorySize(Machine machine)
  {
    return addressableMemorySize(machine);
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public static int pAlignment(Machine machine)
  {
    return machine.alignData(machine.addressableMemoryUnits(minBitSize));
  }

  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToCString()
  {
    return getPointedTo().mapTypeToCString() + " *";
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
