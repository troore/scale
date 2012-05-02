package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.Declaration;

/**
 * A RefType node is used to represent an exisiting type when
 * attributes must be set on a new equivalent type.
 * <P>
 * $Id: RefType.java,v 1.76 2007-10-04 19:58:08 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * For example: 
 * <PRE>
 *   char *s;
 *   typedef const char *Identifier;
 *   Identifier id;
 * </PRE>
 * would be represented in clef as:
 *
 * <PRE>
 *      VariableDecl(s)  VariableDecl(id)  TypeDecl(Identifier)
 *             \                 |          |
 *              \                |          |
 *               \               v          v
 *                \             RefType{const}
 *                 \            /
 *                  \          /
 *                   |         |
 *                   v         v
 *                   PointerType
 *                        |
 *                        v
 *                   CharacterType
 * </PRE>
 */

public class RefType extends Type 
{
  private static Vector<RefType> types; // A list of all the unique reference types.

  private Type        refTo;     // The type referenced.
  private RefAttr     attribute; // The attribute of this reference.
  private Declaration myDecl;    // The TypeDecl for this type if any.
  private int         alignment; // The user-specified alignment for this type if any.

  /**
   * Re-use an existing instance of a particular reference type.
   * If no equivalent reference type exists, create a new one.
   * @param refTo the actual type
   * @param attribute an attribute of the type
   * @param myDecl null or a TypeName or TypeDecl declaration
   * @param alignment is the user-specified alignment
   */
  private static RefType create(Type        refTo,
                                RefAttr     attribute,
                                Declaration myDecl,
                                int         alignment)
  {
    // refTo.getCoreType() may be null if a struct is being defined.

    if (refTo instanceof RefType) {
      RefType to = (RefType) refTo;
      if ((to.alignment == alignment) && (to.attribute == attribute) && (to.myDecl == myDecl))
        return to;
      if ((alignment != 0) && (to.attribute == attribute) && (to.myDecl == myDecl))
        refTo = to.getRefTo();
    }

    if ((alignment != 0) && (refTo.getCoreType() instanceof FixedArrayType)) {
      FixedArrayType art = (FixedArrayType) refTo.getCoreType();
      refTo = art.copy(RefType.createAligned(art.getElementType(), alignment));
    }

    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        RefType ta = types.elementAt(i);
        if (ta.refTo != refTo)
          continue;
        if (ta.myDecl != myDecl)
          continue;
        if (ta.attribute == attribute) {
	  if ((attribute != RefAttr.Aligned) ||
              (ta.alignment == alignment))
            return ta;
        }
      }
    }

    RefType a = new RefType(refTo, attribute, myDecl, alignment);
    return a;
  }

  /**
   * Re-use an existing instance of a particular reference type.
   * If no equivalent reference type exists, create a new one.
   * @param refTo the actual type
   * @param myDecl null or a TypeName or TypeDecl declaration
   */
  public static RefType create(Type refTo, Declaration myDecl)
  {
    return RefType.create(refTo, RefAttr.None, myDecl, 0);
  }

  /**
   * Re-use an existing instance of a particular reference type.
   * If no equivalent reference type exists, create a new one.
   * @param refTo the actual type
   * @param attribute an attribute of the type
   */
  public static RefType create(Type refTo, RefAttr attribute)
  {
    return RefType.create(refTo, attribute, null, 0);
  }

  /**
   * Re-use an existing instance of a particular reference type.
   * If no equivalent reference type exists, create a new one.
   * Do not specify an alignment of zero.
   * @param refTo the actual type
   * @param alignment is the user-specified alignment
   */
  public static RefType createAligned(Type refTo, int alignment)
  {
    if (refTo.getCoreType() instanceof FixedArrayType) {
      FixedArrayType art = (FixedArrayType) refTo.getCoreType();
      refTo = art.copy(RefType.createAligned(art.getElementType(), alignment));
    }

    return RefType.create(refTo, RefAttr.Aligned, null, alignment);
  }

  private RefType(Type refTo, RefAttr attribute, Declaration myDecl, int alignment)
  {
    this.refTo     = refTo;
    this.attribute = attribute;
    this.myDecl    = myDecl;
    this.alignment = alignment;
    if (types == null)
      types = new Vector<RefType>(2);
    types.addElement(this);
  }

  /**
   * Return the type without any "const" attributes.
   * @see RefType
   */
  public Type getNonConstType()
  {
    if (!isConst())
      return this;

    if (attribute == RefAttr.Const)
      return refTo;

    return RefType.create(refTo.getNonConstType(), attribute, myDecl, alignment);
  }

  /**
   * Return the type to use if a variable of this type is in a register.
   */
  public Type registerType()
  {
    if (refTo == null)
      return null;

    Type t = refTo.registerType();
    if (t == refTo)
      return this;

    return create(t, attribute, myDecl, alignment);
  }

  /**
   * Return the type referenced.
   */
  public final Type getRefTo()
  {
    return refTo;
  }

  /**
   * Set the type referenced.
   */
  public final void setRefTo(Type refTo)
  {
    this.refTo = refTo;
  }

  /**
   * Return true if the attribute is associated with this type.
   */
  public boolean isAttributeSet(RefAttr attribute)
  {
    if (attribute == this.attribute)
      return true;
    return refTo.isAttributeSet(attribute);
  }

  public final RefType returnRefType()
  {
    return this;
  }

  /**
   * Return the attribute of this type reference.
   */
  public RefAttr getAttribute()
  {
    return attribute;
  }

  /**
   * Return true if the type specifies const.
   */
  public boolean isConst()
  {
    if (attribute == RefAttr.Const)
      return true;
    return refTo.isConst();
  }

  /**
   * Return true if the type specifies volatile.
   */
  public boolean isVolatile()
  {
    if (attribute == RefAttr.Volatile)
      return true;
    return refTo.isVolatile();
  }

  /**
   * Return true if the type specifies restricted.
   */
  public boolean isRestricted()
  {
    if (attribute == RefAttr.Restrict)
      return true;
    return refTo.isRestricted();
  }

  /**
   * Return the TypeDecl or TypeName of this type reference.
   */
  public Declaration getDecl()
  {
    if (myDecl != null)
      return myDecl;
    return refTo.getDecl();
  }

  public void visit(Predicate p)
  {
    p.visitRefType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitRefType(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return refTo;
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
    if (refTo != null)
      return refTo.getCoreType();
    return null;
  }

  /**
   * Return the rank of the type.  For a scalar, the rank is 0.  For
   * an array, the rank is the number of dimensions.
   */
  public int getRank()
  {
    if (refTo != null)
      return refTo.getRank();
    return 0;
  }

  /**
   * Return true if type represents an array.
   */
  public boolean isArrayType()
  {
    return refTo.isArrayType();
  }

  /**
   * Return true if type represents an address.
   */
  public boolean isPointerType()
  {
    return refTo.isPointerType();
  }

  /**
   * Return true if type represents no type.
   */
  public boolean isVoidType()
  {
    return refTo.isVoidType();
  }

  /**
   * Return true if type represents an aggregate object.
   */
  public boolean isAggregateType()
  {
    return refTo.isAggregateType();
  }

  /**
   * Return true if type represents a union object.
   */
  public boolean isUnionType()
  {
    return refTo.isUnionType();
  }

  /**
   * Return true if type represents a composite type.
   */
  public boolean isCompositeType()
  {
    return refTo.isCompositeType();
  }

  /**
   * Return true if type represents a floating point value.
   */
  public boolean isRealType()
  {
    return refTo.isRealType();
  }

  /**
   * Return true if type represents an integer value.
   */
  public boolean isIntegerType()
  {
    return refTo.isIntegerType();
  }

  /**
   * Return true if type represents a complex value.
   */
  public boolean isComplexType()
  {
    return refTo.isComplexType();
  }

  /**
   * Return true if type represents a scaler value.
   */
  public boolean isAtomicType()
  {
    return refTo.isAtomicType();
  }

  /**
   * Return true if type represents a numeric value.
   */
  public boolean isNumericType()
  {
    return refTo.isNumericType();
  }

  /**
   * Return true if type represents a reference type.
   */
  public final boolean isRefType()
  {
    return true;
  }

  /**
   * Return true if this type has an associated TypeName or TypeDecl.
   */
  public boolean isNamedType()
  {
    if (myDecl != null)
      return true;
    return refTo.isNamedType();
  }

  /**
   * Return true if this type represents a procedure.
   */
  public boolean isProcedureType()
  {
    return refTo.isProcedureType();
  }

  /**
   * Return true is the type is signed.
   */
  public boolean isSigned()
  {
    return refTo.isSigned();
  }

  /**
   * Return the equivalent signed type.
   */
  public Type getSignedType()
  {
    Type st = refTo.getSignedType();
    if (st == refTo)
      return this;
    return create(st, attribute, myDecl, alignment);
  }

  /**
   * Return the number of addressable memory units are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   */
  public long memorySize(Machine machine)
  {
    assert (refTo != null) : "No reference type " + this;
    return refTo.memorySize(machine);
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
    assert (refTo != null) : "No reference type " + this;
    return refTo.elementSize(machine);
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    if (attribute == RefAttr.Aligned)
      return alignment;

    assert (refTo != null) : "No reference type " + this;

    return refTo.alignment(machine);
  }

  /**
   * Return true if this instance references a {@link
   * scale.clef.decl.Declaration Declaration}. Note that
   * <code>hasDecl()</code> may return <code>false</code> even when
   * {@link #getDecl getDecl()} returns a non-null result.
   */
  public final boolean hasDecl()
  {
    return (myDecl != null);
  }

  /**
   * Return true if the types are equivalent.  Types are equivalent if
   * they are the same except for names.  For example, if
   * <pre>
   *    typedef int X;
   * </pre>
   * then types <code>X</code> and <code>int</code> are equivalent.
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    if (tc == null)
      return false;

    if (myDecl != null)
      return refTo.equivalent(tc);

    if (tc.getClass() != getClass())
      return false;

    RefType rt = (RefType) tc;
    return (refTo.equivalent(rt.refTo) &&
            (attribute == rt.attribute) && (myDecl == rt.myDecl));
  }

  /**
   * Return the type to be used by the equivalence method.
   */
  protected Type getEquivalentType()
  {
    if (myDecl != null)
      return refTo.getEquivalentType();
    return this;
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<RefType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<RefType>();
    return types.elements();
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.  This method should be over-ridden as it simplay returns
   * the class name.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer("Ref");
    if (attribute != RefAttr.None) {
      buf.append(' ');
      buf.append(attribute);
      if (attribute == RefAttr.Aligned) {
	buf.append(':');
	buf.append(alignment);
      }
    }
    if (myDecl != null) {
      buf.append(' ');
      buf.append(myDecl.getName());
    }
    return buf.toString();
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("<");
    buf.append(toStringClass());
    buf.append('-');
    buf.append(getNodeID());
    if (attribute != RefAttr.None) {
      buf.append(' ');
      buf.append(attribute);
      if (attribute == RefAttr.Aligned) {
	buf.append(':');
	buf.append(alignment);
      }
    }
    if (myDecl != null) {
      buf.append(" \"");
      buf.append(myDecl.getName());
      buf.append("\"");
    } else {
      buf.append(' ');
      buf.append(refTo);
    }
    buf.append('>');
    return buf.toString();
  }

  public String toStringShort()
  {
    StringBuffer buf = new StringBuffer("<");
    buf.append(toStringClass());
    buf.append('-');
    buf.append(getNodeID());
    if (attribute != RefAttr.None) {
      buf.append(' ');
      buf.append(attribute);
      if (attribute == RefAttr.Aligned) {
	buf.append(':');
	buf.append(alignment);
      }
    }
    if (myDecl != null) {
      buf.append(" \"");
      buf.append(myDecl.getName());
      buf.append('\"');
    }
    buf.append('>');
    return buf.toString();
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
    return refTo.precedence();
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
