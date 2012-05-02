package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * The <tt>FortranCharType</tt> class represents the Fortran CHARACTER type.
 * <p>
 * $Id: FortranCharType.java,v 1.5 2007-03-21 13:31:56 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class FortranCharType extends CompositeType 
{
  private static Vector<Type> types; // A list of all the unique CHARACTER types.

  private int length;

  /**
   * Re-use an existing instance of a particular CHARACTER type.
   * If no equivalent CHARACTER type exists, create a new one.
   * @param length is the number of length in characters
   */
  public static FortranCharType create(int length)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        FortranCharType ta = (FortranCharType) types.elementAt(i);
        if (ta.length == length)
          return ta;
      }
    }
    FortranCharType a = new FortranCharType(length);
    return a;
  }

  private FortranCharType(int length)
  {
    this.length = length;

    if (types == null)
      types = new Vector<Type>(2);
    types.addElement(this);
  }

  public final FortranCharType returnFortranCharType()
  {
    return this;
  }

  public final boolean isFortranCharType()
  {
    return true;
  }

  /**
   * Return the number of characters in this CHARACTER type.
   * A length of 0 indicates that the length is not known.
   */
  public int getLength()
  {
    return length;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("<");
    buf.append("fc");
    buf.append(length);
    buf.append('>');
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitFortranCharType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitFortranCharType(this);
  }

  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToCString()
  {
    return "char[" + length + "]";
  }

  /**
   * Map a type to a Fortran string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToF77String()
  {
    return "character*" + length;
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

    FortranCharType t2 = (FortranCharType) tc;
    return (length == t2.length);
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
    return length;
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    return machine.alignData(1);
  }

  /**
   * Return a precedence value for types. We assign precendence values
   * to declarators that use operators (e.g., arrays, functions,
   * pointers).  The precendence values are relative to the other
   * operators - larger values mean higher precedence.
   * @return an integer representing the precendence value for the type
   * relative to the other values.
   */
  public int precedence() 
  {
    return 3;
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
