package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * This class repsents floating point types such as C's float and
 * double types.
 * <p>
 * $Id: FloatType.java,v 1.54 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class FloatType extends RealType 
{
  private static Vector<FloatType> types; // A list of all the unique float types.

  /**
   * Re-use an existing instance of a particular float type.
   * If no equivalent float type exists, create a new one.
   * @param minbitSize is the minimum number of bits required for the
   * representation
   */

  public static FloatType create(int minbitSize)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        FloatType ta = types.elementAt(i);
        if (ta.bitSize() == minbitSize) {
          return ta;
        }
      }
    }
    FloatType a = new FloatType(minbitSize);
    return a;
  }

  private FloatType(int minbitSize)
  {
    super(minbitSize);
    if (types == null)
      types = new Vector<FloatType>(2);
    types.addElement(this);
  }

  public final boolean isSigned()
  {
    return true;
  }

  /**
   * Return true if type represents a character type.
   */
  public final boolean isFloatType()
  {
    return true;
  }

  public final FloatType returnFloatType()
  {
    return this;
  }

  /**
   * Transform a value to be in a suitable range.  This method returns
   * the original value unmodified.
   */
  public double putValueInRange(double value)
  {
    return value;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("<");
    int minbitSize = bitSize();
    if (minbitSize == 32)
      buf.append("float");
    else if (minbitSize == 64)
      buf.append("double");
    else if (minbitSize == 128)
      buf.append("long double");
    else {
      buf.append('r');
      buf.append(String.valueOf(minbitSize));
    }
    buf.append('>');
    return buf.toString();
  }

  public String toStringShort()
  {
    return toString();
  }

  public void visit(Predicate p)
  {
    p.visitFloatType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitFloatType(this);
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

    FloatType t2 = (FloatType) tc;
    return (bitSize() == t2.bitSize());
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<FloatType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<FloatType>();
    return types.elements();
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
