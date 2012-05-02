package scale.clef.type;

import scale.common.*;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;

/**
 * The abstract class for all array types.
 * <p>
 * $Id: ArrayType.java,v 1.77 2007-03-21 13:31:55 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * All arrays have this type.  Note, this is not an address type.
 * <p>
 * Regardless of whether the source language uses row major or column
 * major array ordering, the dimensions represented by this type are
 * always in row major (C style) ordering.
 */

public abstract class ArrayType extends CompositeType 
{
  /**
   * The type of elements in the array.
   */
  private Type elementType; 

  public ArrayType(Type elementType)
  {
    this.elementType    = elementType;
  }

  /**
   * Creates an array type with the same dimensions but a different element type.
   * @param elementType the new array element type
   * @return the new type
   */
  public abstract ArrayType copy(Type elementType);

  /**
   * Return a vector of the array indexes.  Regardless of whether the
   * source language uses row major or column major array ordering,
   * the dimensions represented by this type are always in row major
   * (C style) ordering.
   */
  protected abstract Vector<Bound> getIndexes();

  /**
   * Return the specified array index.  Regardless of whether the
   * source language uses row major or column major array ordering,
   * the dimensions represented by this type are always in row major
   * (C style) ordering.
   */
  public abstract Bound getIndex(int i);

  /**
   * Return the rank of the type.  For a scalar, the rank is 0.  For
   * an array, the rank is the number of dimensions.
   */
  public abstract int getRank();

  /**
   * Return the number of addressable memory units that are needed to
   * represent a single element of the array.
   * @param machine is the machine-specific data machine
   */
  public long elementSize(Machine machine)
  {
    return elementType.memorySize(machine);
  }

  /**
   * Calculate how many addressable memory units are needed to represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of addressable memory units required to represent this type
   */
  public long memorySize(Machine machine)
  {
    long elementSize = elementType.memorySize(machine);

    return  numberOfElements() * elementSize;
  }

  /**
   * Return true if the size of the array is known.
   */
  public abstract boolean isSizeKnown();
  /**
   * Calculate how many elements are in the array.
   * @return the number of elements
   */
  public abstract long numberOfElements();
  /**
   * Return the type of the first index of the array.
   */
  public abstract Bound getFirstIndex();

  /**
   * Return the type of the array elements.
   */
  public final Type getElementType()
  {
    return elementType;
  }

  public final ArrayType returnArrayType()
  {
    return this;
  }

  public void visit(Predicate p)
  {
    p.visitArrayType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitArrayType(this);
  }

  /**
   * Return true if the array has definite bounds known at compile
   * time.
   */
  public abstract boolean isBounded();

  /**
   * Return true if type represents an array.
   */
  public final boolean isArrayType()
  {
    return true;
  }

  /**
   * Return the type of a subscript expression with one subscript.
   * For a rank 1 array, this is the element type.
   * For a rank 2 array, this is a rank 1 array of the element type, etc.
   */
  public Type getArraySubtype()
  {
    return getArraySubtype(1);
  }

  /**
   * Return the type of a subscript expression with the specified
   * number of subscripts or <code>null</code> if there are too many
   * subscripts.
   */
  public abstract Type getArraySubtype(int n);

  /**
   * Calculate the alignment needed for this data type.
   */
  public final int alignment(Machine machine)
  {
    return elementType.alignment(machine);
  }

  /**
   * Return a precedence value for types. We assign precendence values
   * to declarators that use operators (e.g., arrays, functions, pointers).
   * The precendence values are relative to the other operators - larger
   * values mean higher precedence. 
   * @return an integer representing the precendence value for the type
   * relative to the other values.
   */
  public final int precedence() 
  {
    return 3;
  }
}
