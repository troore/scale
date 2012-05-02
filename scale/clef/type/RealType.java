package scale.clef.type;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * This is the base class for all scaled types such a C's float and double types.
 * <p>
 * $Id: RealType.java,v 1.32 2007-03-21 13:32:00 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The size of the type is specified as the minimum number of bits that are required
 * to represent a value of that type. 
 */

public abstract class RealType extends NumericType 
{
  /**
   * The minimum number of bits required to represent a value of this type.
   */
  private int minbitSize;

  /**
   * @param minbitSize is the minimum number of bits required to represent a value of this type
   */
  public RealType(int minbitSize)
  {
    this.minbitSize = minbitSize;
  }

  public int bitSize()
  {
    return minbitSize;
  }

  /**
   * Return true if type represents a floating point value.
   */
  public boolean isRealType()
  {
    return true;
  }

  public final RealType returnRealType()
  {
    return this;
  }

  public void visit(Predicate p)
  {
    p.visitRealType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitRealType(this);
  }

  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToCString()
  {
    switch (bitSize()) {
    case 32:  return "float";
    case 64:  return "double";
    case 128: return "long double";
    default:
      throw new scale.common.InternalError("Incorrect real type " + this);
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
    case 32:  return "real*4";
    case 64:  return "real*8";
    case 128: return "real*16";
    default:
      throw new scale.common.InternalError("Incorrect real type " + this);
    }
  }
}
