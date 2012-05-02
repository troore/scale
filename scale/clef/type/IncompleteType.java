package scale.clef.type;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.Declaration;

/**
 * An IncompleteType is used to represent a type before the complete
 * type is known.
 * <p>
 * $Id: IncompleteType.java,v 1.63 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * For example, when creating a struct type in C, a field of the
 * struct may reference itself.
 */

public class IncompleteType extends Type 
{
  /**
   * The completed type.
   */
  private Type completeType;

  public IncompleteType()
  {
    completeType = null;
  }

  /**
   * Return the completed type or null if it is not complete.
   */
  public Type getCompleteType()
  {
    return completeType;
  }

  /**
   * Specify the completed type.
   */
  public final void setCompleteType(Type type)
  {
    completeType = type;
  }

  /**
   * Return the type without any "const" attributes.
   * @see RefType
   */
  public Type getNonConstType()
  {
    if (completeType != null)
      return completeType.getNonConstType();
    return null;
  }

  /**
   * Return true if the attribute is associated with this type.
   * @see scale.clef.type.RefType
   */
  public boolean isAttributeSet(RefAttr attribute)
  {
    if (completeType != null)
      return completeType.isAttributeSet(attribute);
    return false;
  }

  /**
   * Return true if type represents an incomplete type.
   */
  public final boolean isIncompleteType()
  {
    return true;
  }

  public final IncompleteType returnIncompleteType()
  {
    return this;
  }

  /**
   * Return true if type represents an array.
   */
  public boolean isArrayType()
  {
    if (completeType != null)
      return completeType.isArrayType();
    return false;
  }

  /**
   * Return true if type represents an address.
   */
  public boolean isPointerType()
  {
    if (completeType != null)
      return completeType.isPointerType();
    return false;
  }

  /**
   * Return true if type represents no type.
   */
  public boolean isVoidType()
  {
    if (completeType != null)
      return completeType.isVoidType();
    return false;
  }

  /**
   * Return true if type represents an aggregate object.
   */
  public boolean isAggregateType()
  {
    if (completeType != null)
      return completeType.isAggregateType();
    return false;
  }

  /**
   * Return true if type represents a union object.
   */
  public boolean isUnionType()
  {
    if (completeType != null)
      return completeType.isUnionType();
    return false;
  }

  /**
   * Return true if type represents a composite type.
   */
  public boolean isCompositeType()
  {
    if (completeType != null)
      return completeType.isCompositeType();
    return false;
  }

  /**
   * Return true if type represents a floating point value.
   */
  public boolean isRealType()
  {
    if (completeType != null)
      return completeType.isRealType();
    return false;
  }

  /**
   * Return true if type represents an integer value.
   */
  public boolean isIntegerType()
  {
    if (completeType != null)
      return completeType.isIntegerType();
    return false;
  }

  /**
   * Return true if type represents a complex value.
   */
  public boolean isComplexType()
  {
    if (completeType != null)
      return completeType.isComplexType();
    return false;
  }

  /**
   * Return true if type represents a scaler value.
   */
  public boolean isAtomicType()
  {
    if (completeType != null)
      return completeType.isAtomicType();
    return false;
  }

  /**
   * Return true if type represents a numeric value.
   */
  public boolean isNumericType()
  {
    if (completeType != null)
      return completeType.isNumericType();
    return false;
  }

  /**
   * Return true if this type has an associated TypeName or TypeDecl.
   */
  public boolean isNamedType()
  {
    if (completeType != null)
      return completeType.isNamedType();
    return false;
  }

  /**
   * Return true if this type represents a procedure.
   */
  public boolean isProcedureType()
  {
    if (completeType != null)
      return completeType.isProcedureType();
    return false;
  }

  /**
   * Return true is the type is signed.
   */
  public boolean isSigned()
  {
    if (completeType != null)
      return completeType.isSigned();
    return false;
  }

  /**
   * Return the equivalent signed type.
   */
  public Type getSignedType()
  {
    return completeType.getSignedType();
  }

  /**
   * Return the TypeDecl or TypeName of this type reference.
   */
  public Declaration getDecl()
  {
    if (completeType != null)
      return completeType.getDecl();
    return null;
  }

  /**
   * Return the type to use if a variable of this type is in a register.
   */
  public Type registerType()
  {
    if (completeType == null)
    return null;

    Type t = completeType.registerType();
    if (t == completeType)
      return this;

    return t;
  }

  public void visit(Predicate p)
  {
    p.visitIncompleteType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitIncompleteType(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return completeType;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  /**
   * This method filters out some the special case type nodes from a
   * type DAG.
   * @see Type#getCoreType
   */
  public Type getCoreType()
  {
    if (completeType != null)
      return completeType.getCoreType();
    return null;
  }

  /**
   * Return the rank of the type.  For a scalar, the rank is 0.  For
   * an array, the rank is the number of dimensions.
   */
  public int getRank()
  {
    if (completeType != null)
      return completeType.getRank();
    return 0;
  }

  /**
   * Return the number of addressable memory units are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   */
  public long memorySize(Machine machine)
  {
    assert (completeType != null) : "Incomplete type " + this;
    return completeType.memorySize(machine);
  }

  /**
   * Return the number of addressable memory units are needed to
   * represent an element of the type.  Except for arrays this is
   * identical to the value return by {@link #memorySize
   * memorySize()}.
   * @param machine is the machine-specific data machine
   */
  public long elementSize(Machine machine)
  {
    assert (completeType != null) : "No reference type " + this;
    return completeType.elementSize(machine);
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    assert (completeType != null) : "Incomplete type " + this;
    return completeType.alignment(machine);
  }

  /**
   * Return true if the type specifies const.
   */
  public boolean isConst()
  {
    if (completeType != null)
      return completeType.isConst();
    return false;
  }

  /**
   * Return true if the type specifies volatile.
   */
  public boolean isVolatile()
  {
    if (completeType != null)
      return completeType.isVolatile();
    return false;
  }

  /**
   * Return true if the type specifies restricted.
   */
  public boolean isRestricted()
  {
    if (completeType != null)
      return completeType.isRestricted();
    return false;
  }

  public String toStringSpecial()
  {
    return "";
  }

  /**
   * Return true if the types are equivalent.
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    if (tc == null)
      return false;

    if (completeType == null)
      return false;

    return completeType.equivalent(tc);
  }

  /**
   * Return the type to be used by the equivalence method.
   */
  protected Type getEquivalentType()
  {
    if (completeType == null)
      return null;
    return completeType.getEquivalentType();
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
    return completeType.precedence();
  }
}
