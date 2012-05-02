package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;

/**
 * This class represents array types with fixed bounds.
 * <p>
 * $Id: FixedArrayType.java,v 1.53 2007-03-21 13:31:56 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Regardless of whether the source language uses row major or column
 * major array ordering, the dimensions represented by this type are
 * always in row major (C style) ordering.
 * <p>
 * It is not necessary for the upper-bound of the outer-most bounds to
 * be known at compile time for this value is not needed to calculate
 * offsets into the array.
 */

public class FixedArrayType extends ArrayType 
{
  private static Vector<FixedArrayType> types; /* A list of all the unique fixed array types */

  /**
   * True if the size of the array has been calculated.
   */
  private boolean sizeCalculated;
  /**
   * The size of the array.
   */
  private long arraySize;
  /**
   * A Bound entry for each dimension.
   */
  private Vector<Bound> indicies;    

  /**
   * Re-use an existing instance of a particular fixed array type.
   * If no equivalent fixed array type exists, create a new one.
   * @param indicies a vector of {@link Bound Bound} entries - one for
   * each dimension.
   * @param elementType the type of elements in the array.
   */
  public static FixedArrayType create(Vector<Bound> indicies, Type elementType)
  {
    if (elementType != null) {
      Type et = elementType.getCoreType();
      if (et != null) {
        ArrayType at = et.getCoreType().returnArrayType();
        if (at != null) {
          // An array whose elements are arrays is just an array of
          // greater rank.

          int l = at.getRank();
          for (int i = 0; i < l; i++)
            indicies.add(at.getIndex(i));

          elementType = at.getElementType();
        }
      }
    }

    if (types != null) {
      int l = indicies.size();
      int n = types.size();
      for (int i = 0; i < n; i++) {
        FixedArrayType ta = types.elementAt(i);

        if (ta.getElementType() != elementType)
          continue;

        if (ta.getRank() != l)
          continue;

        int j;
        for (j = 0; j < l; j++)
          if (ta.getIndex(j) != indicies.elementAt(j))
            break;

        if (j < l)
          continue;

        return ta;
      }
    }
    FixedArrayType a = new FixedArrayType(indicies, elementType);
    return a;
  }

  /**
   * Re-use an existing instance of a one-dimensional fixed array
   * type.  If no equivalent fixed array type exists, create a new
   * one.  If <code>minIndex &gt; maxIndex</code>, the range is
   * considered to be unbounded.
   * @param minIndex is the lower bound on the array
   * @param maxIndex is the upper bound on the array
   * @param elementType the type of elements in the array.
   */
  public static FixedArrayType create(long minIndex,
                                      long maxIndex,
                                      Type elementType)
  {
    Vector<Bound> indicies = new Vector<Bound>(1);

    indicies.addElement(Bound.create(minIndex, maxIndex));

    return FixedArrayType.create(indicies, elementType);
  }

  private FixedArrayType(Vector<Bound> indicies, Type elementType)
  {
    super(elementType);
    this.indicies       = indicies;
    this.sizeCalculated = false;

    if (types == null)
      types = new Vector<FixedArrayType>(2);
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
    return FixedArrayType.create(getIndexes(), elementType);
  }

  /**
   * Return a new vector of the array indexes.  Regardless of whether
   * the source language uses row major or column major array
   * ordering, the dimensions represented by this type are always in
   * row major (C style) ordering.
   */
  protected final Vector<Bound> getIndexes()
  {
    if (indicies == null)
      return null;

    Vector<Bound> nv = new Vector<Bound>(indicies.size());
    for (Bound bd : indicies)
      nv.add(bd);
    return nv;
  }

  /**
   * Return the specified array index.  Regardless of whether the
   * source language uses row major or column major array ordering,
   * the dimensions represented by this type are always in row major
   * (C style) ordering.
   */
  public final Bound getIndex(int i)
  {
    return indicies.elementAt(i);
  }

  /**
   * Return the rank of the type.  For a scalar, the rank is 0.  For
   * an array, the rank is the number of dimensions.
   */
  public final int getRank()
  {
    if (indicies == null)
      return 0;
    return indicies.size();
  }

  /**
   * Return true if the size of the array is known.
   */
  public final boolean isSizeKnown()
  {
    if (sizeCalculated)
      return (arraySize >= 0);

    sizeCalculated = true;

    Type et          = getElementType();
    long numElements = 1;
    int  l           = getRank();

    for (int i = 0; i < l; i++) {
      Bound bd  = getIndex(i);
      try {
        numElements *= bd.numberOfElements();
      } catch (scale.common.InvalidException ex) {
        arraySize = -1;
        return false;
      }
    }

    arraySize = numElements;
    return true;
  }

  /**
   * Calculate how many elements are in the array.
   * @return the number of elements
   */
  public final long numberOfElements()
  {
    if (isSizeKnown())
      return arraySize;

    // This unnecessary logic is here because of other kludges.
    // The "catch" clause should be a clue.

    Type et          = getElementType();
    long numElements = 1;
    int  l           = getRank();

    for (int i = 0; i < l; i++) {
      Bound bd = getIndex(i);
      try {
        numElements *= bd.numberOfElements();
      } catch (scale.common.InvalidException ex) {
      }
    }

    return numElements;
  }

  /**
   * Return the type of the first index of the array.
   */
  public final Bound getFirstIndex()
  {
    if (indicies == null)
      return null;
    if (indicies.size() > 0)
      return indicies.firstElement();
    else
      return null;
  }

  public void visit(Predicate p)
  {
    p.visitFixedArrayType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitFixedArrayType(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getElementType();
    return (Node) indicies.elementAt(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1 + indicies.size();
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

    FixedArrayType t2 = (FixedArrayType) tc;
    if (!t2.getElementType().equivalent(getElementType()))
      return false;

    int l = getRank();
    if (t2.getRank() != l)
      return false;

    for (int j = 0; j < l; j++)
      if (t2.indicies.elementAt(j) != indicies.elementAt(j))
        return false;

    return true;
  }

  /**
   * Return true if the array has definite bounds.
   */
  public boolean isBounded()
  {
    int n = indicies.size();
    for (int i = 0; i < n; i++) {
      Bound bd = indicies.elementAt(i);
      if (!bd.isConstantBounds())
        return false;
    }
    return true;
  }

  /**
   * Return the type of a subscript expression with the specified
   * number of subscripts or <code>null</code> if there are too many
   * subscripts.
   */
  public Type getArraySubtype(int n)
  {
    int rank = getRank();
    if (n > rank)
      return null;

    Type et = getElementType();
    if (n == rank)
      return et;

    Vector<Bound> indicies = new Vector<Bound>(rank - n);
    for (int i = n; i < rank; i++)
      indicies.add(getIndex(i));

    return FixedArrayType.create(indicies, et);
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<FixedArrayType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<FixedArrayType>();
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
    Vector<Bound> indicies = getIndexes();
    indicies.add(index);
    return create(indicies, getElementType());
  }

  /**
   * Return true if type represents an array whose dimensions are
   * known at compile time.
   */
  public boolean isFixedArrayType()
  {
    return true;
  }

  public final FixedArrayType returnFixedArrayType()
  {
    return this;
  }
}
