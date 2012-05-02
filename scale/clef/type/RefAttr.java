package scale.clef.type;

/**
 * This enum specifies the attributes of a reference type - const,
 * aligned, etc.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This enum describes what attributes are associated with a
 * type.  Attributes are non-type information that is associated
 * with a type because they can be associated with just a part of a
 * type declaration.
 * @see RefType
 */
public enum RefAttr
{
  None(),
  /**
   * Indicates that instances of this type have a constant
   * value.  By default, instances of a type are mutable.
   * <p>
   * One could reasonably argue that the immutability of a value is
   * not a property (or attribute) of a type.  However, C++'s typedef
   * construct permits immutability to be included with the type.
   */
  Const(),
  /**
   * Marks a value which may be changed by something which
   * a compiler cannot detect.
   */
  Volatile(),
  /**
   * For types whose alignment was specified.
   */
  Aligned(),
  /**
   * For the type that represents the builtin type va_list.
   */
  VaList(),
  /**
   * Marks a value which may not be changed by only by restricted
   * means.
   */
  Restrict(),
  /**
   * For types with substructures, this attribute indicates if the
   * source language requires the data layout to preserve the order of
   * substructures (declaration order is assumed).
   */
  Ordered(),
  /**
   * Supports Modula-3's traced data types.
   */
  Traced();
}
