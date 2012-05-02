package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;

/**
 * This class represents array types with bounds that are determined
 * at run time.
 * <p>
 * $Id: AllocArrayType.java,v 1.3 2007-06-15 18:21:56 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Allocatable arrays are allocated at run time.
 * <p>
 * Regardless of whether the source language uses row major or column
 * major array ordering, the dimensions represented by this type are
 * always in row major (C style) ordering.
 */

public class AllocArrayType extends ArrayType 
{
  private static Vector<AllocArrayType> types; /* A list of all the unique fixed array types */

  private Type struct; // Structure used to represent array at run time.
  private int  rank;   // How many dimensions.

  /**
   * Re-use an existing instance of a particular fixed array type.
   * If no equivalent fixed array type exists, create a new one.
   * @param rank specifies the number of array dimensions
   * @param struct specifies the structure used to represent array at run time.
   * @param elementType the type of elements in the array.
   */
  public static AllocArrayType create(int rank, Type struct, Type elementType)
  {
    assert struct.getCoreType().returnRecordType() != null;
    if (elementType != null) {
      Type et = elementType.getCoreType();
      if (et != null) {
        ArrayType at = et.getCoreType().returnArrayType();
        if (at != null) {
          // An array whose elements are arrays is just an array of
          // greater rank.

          rank += at.getRank();

          elementType = at.getElementType();
        }
      }
    }

    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        AllocArrayType ta = types.elementAt(i);

        if (ta.getElementType() != elementType)
          continue;

        if (ta.getRank() != rank)
          continue;

        if (!ta.getStruct().equivalent(struct))
          continue;

        return ta;
      }
    }
    AllocArrayType a = new AllocArrayType(rank, struct, elementType);
    return a;
  }

  private AllocArrayType(int rank, Type struct, Type elementType)
  {
    super(elementType);
    this.rank   = rank;
    this.struct = struct;

    if (types == null)
      types = new Vector<AllocArrayType>(2);
    types.addElement(this);
  }

  /**
   * Creates an array type with the same dimensions but a different
   * element type.
   * @param elementType the new array element type
   * @return the new type
   */
  public ArrayType copy(Type elementType)
  {
    return AllocArrayType.create(rank, struct, elementType);
  }

  /**
   * Return a vector of the array indexes.  Regardless of whether the
   * source language uses row major or column major array ordering,
   * the dimensions represented by this type are always in row major
   * (C style) ordering.
   */
  protected final Vector<Bound> getIndexes()
  {
    throw new scale.common.InternalError("allocatable array");
  }

  /**
   * Return the specified array index.  Regardless of whether the
   * source language uses row major or column major array ordering,
   * the dimensions represented by this type are always in row major
   * (C style) ordering.
   */
  public final Bound getIndex(int i)
  {
    return Bound.noBound;
  }

  /**
   * Return the rank of the type.  For a scalar, the rank is 0.  For
   * an array, the rank is the number of dimensions.
   */
  public final int getRank()
  {
    return rank;
  }

  /**
   * Return the structure used to represent the array at run time.
   */
  public final Type getStruct()
  {
    return struct;
  }

  /**
   * Return false - the size is not known at compile time.
   */
  public final boolean isSizeKnown()
  {
    return false;
  }

  public final AllocArrayType returnAllocArrayType()
  {
    return this;
  }

  /**
   * Return true.
   */
  public final boolean isAllocArrayType()
  {
    return true;
  }

  /**
   * Calculate how many elements are in the array.
   * @return the number of elements
   */
  public final long numberOfElements()
  {
    return -1;
  }

  /**
   * Return the type of the first index of the array.
   */
  public final Bound getFirstIndex()
  {
    return Bound.noBound;
  }

  public void visit(Predicate p)
  {
    p.visitAllocArrayType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitAllocArrayType(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    return getElementType();
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

    AllocArrayType t2 = (AllocArrayType) tc;
    if (!t2.getElementType().equivalent(getElementType()))
      return false;

    return (t2.rank == rank);
  }

  /**
   * Return true if the array has definite bounds.
   */
  public boolean isBounded()
  {
    return false;
  }

  /**
   * Return the type of a subscript expression with the specified
   * number of subscripts or <code>null</code> if there are too many
   * subscripts.
   */
  public Type getArraySubtype(int n)
  {
    if (n > rank)
      return null;

    Type et = getElementType();
    if (n == rank)
      return et;

    return AllocArrayType.create(rank - n, struct, et);
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<AllocArrayType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<AllocArrayType>();
    return types.elements();
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }

  /**
   *  Return a new array type with the additional dimension.
   */
  public final ArrayType addIndex(Bound index)
  {
    throw new scale.common.InternalError("allocatable array");
  }
}
