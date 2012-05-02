package scale.clef.type;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.TypeDecl;
import scale.clef.decl.TypeName;
import scale.clef.decl.Declaration;

/**
 * This class is the root class for type nodes.
 * <p>
 * $Id: Type.java,v 1.104 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A type is represented by a graph (DAG) of type nodes.
 * <p>
 * The current list of special case nodes is as follows:
 *   <UL>
 *   <LI> IncompleteType
 *   <LI> RefType
 *   </UL>
 * <b>Note: types are unique!</b> There is only one instance of any
 * specific type.  Types are created using a <tt>static create</tt>
 * method.  The <tt>new</tt> operator is not used. However, the
 * IncompleteType is not unique and is created using <tt>new</tt>
 * because it must be modified after it is created.
 * <p>
 * Types should not have annotations unless the annotation applies to
 * every instance where the type may be used.
 */

public abstract class Type extends Node 
{
  private static int nextColor = 0; // A unique value to use in CFG spanning algorithms.

  /**
   * Set up for a new traversal - that is, use the next color value.
   * This color is used to mark types in spanning algorithms.
   * @see #setVisited
   * @see #visited
   * @see #nextVisit
   */
  public static void nextVisit()
  {
    nextColor++;
  }

  /**
   * An integer value associated with the type that various algorithms
   * may use.
   */
  private int tag;     
  /**
   * The color value - used in traversing the CFG.
   */
  private int color;
  /**
   * True if values of this type can be in a register.
   */
  private boolean canBeInRegister = false;

  public Type()
  {
    super();
    this.color = 0;
    this.tag = 0;
  }

  /**
   * Associate the current color value with this Type.
   * The color is for the use of spanning algorithms.
   * @see #nextVisit
   * @see #visited
   */
  public void setVisited()
  {
    this.color = nextColor;
  }

  /**
   * Return true if this Type has been visited during the current
   * visit (i.e., is the current color).
   * @see #nextVisit
   * @see #setVisited
   */
  public final boolean visited()
  {
    return (color == nextColor);
  }
  
  /**
   * Set the tag for this variable.  The tag can be used by various
   * algorithms.
   */
  public final void setTag(int tag)
  {
    this.tag = tag;
  }

  /**
   * Return the tag associated with this variable.  The tag can be
   * used by various algorithms.
   */
  public final int getTag()
  {
    return tag;
  }

  /**
   * Specify that values of this type can be placed in a register.
   */
  public final void specifyCanBeInRegister(boolean canBeInRegister)
  {
    this.canBeInRegister = canBeInRegister;
  }

  /**
   * True if values of this type can be placed in a register.  This
   * will return false unless the {@link #specifyCanBeInRegister
   * specifyCanBeInRegister()} method has been called for the type
   * instance.  (See the code in
   * scale.backend.Generator#processCoreType().)
   */
  public final boolean canBeInRegister()
  {
    return canBeInRegister;
  }

  public String toString()
  {
    return toString("<", ">");
  }

  public String toStringShort()
  {
    return toString("<", ">");
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.  This method should be over-ridden as it simplay returns
   * the class name.
   */
  public String getDisplayLabel()
  {
    String mnemonic = toStringClass();
    if (mnemonic.endsWith("Type"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 4);
    return mnemonic;
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.  This method should be over-ridden
   * as it simplay returns the color red.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.GREEN;
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.  This method should be over-ridden as it
   * simplay returns the shape "ellipse".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.ELLIPSE;
  }

  /**
   * Return if <code>this</code> is a {@link AggregateType
   * AggregateType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public AggregateType returnAggregateType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link AllocArrayType
   * AllocArrayType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public AllocArrayType returnAllocArrayType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link ArrayType ArrayType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public ArrayType returnArrayType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link AtomicType AtomicType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public AtomicType returnAtomicType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link BooleanType BooleanType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public BooleanType returnBooleanType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link CharacterType
   * CharacterType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public CharacterType returnCharacterType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link ComplexType ComplexType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public ComplexType returnComplexType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link CompositeType
   * CompositeType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public CompositeType returnCompositeType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link EnumerationType
   * EnumerationType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public EnumerationType returnEnumerationType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link FixedArrayType
   * FixedArrayType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public FixedArrayType returnFixedArrayType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link FloatType FloatType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public FloatType returnFloatType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link FortranCharType
   * FortranCharType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public FortranCharType returnFortranCharType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link IncompleteType
   * IncompleteType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public IncompleteType returnIncompleteType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link IntegerType IntegerType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public IntegerType returnIntegerType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link NumericType NumericType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public NumericType returnNumericType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link PointerType PointerType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public PointerType returnPointerType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link ProcedureType
   * ProcedureType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public ProcedureType returnProcedureType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link RealType RealType},
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public RealType returnRealType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link RecordType RecordType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public RecordType returnRecordType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link RefType RefType}, return
   * <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public RefType returnRefType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link SignedIntegerType
   * SignedIntegerType}, return <code>this</code>.  Otherwise, return
   * <code>null</code>.
   */
  public SignedIntegerType returnSignedIntegerType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link UnionType UnionType}
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public UnionType returnUnionType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link UnsignedIntegerType
   * UnsignedIntegerType}, return <code>this</code>.  Otherwise,
   * return <code>null</code>.
   */
  public UnsignedIntegerType returnUnsignedIntegerType()
  {
    return null;
  }

  /**
   * Return if <code>this</code> is a {@link VoidType VoidType},
   * return <code>this</code>.  Otherwise, return <code>null</code>.
   */
  public VoidType returnVoidType()
  {
    return null;
  }

  /**
   * Return true if type represents an array.
   */
  public boolean isArrayType()
  {
    return false;
  }

  /**
   * Return true if type represents an array whose dimensions are
   * not known at compile time.
   */
  public boolean isAllocArrayType()
  {
    return false;
  }

  /**
   * Return true if type represents an array whose dimensions are
   * known at compile time.
   */
  public boolean isFixedArrayType()
  {
    return false;
  }

  /**
   * Return true if type represents a class type.
   */
  public boolean isClassType()
  {
    return false;
  }

  /**
   * Return true if type represents a character type.
   */
  public boolean isFloatType()
  {
    return false;
  }

  /**
   * Return true if type represents a reference type.
   */
  public boolean isRefType()
  {
    return false;
  }

  /**
   * Return true if type represents a character type.
   */
  public boolean isCharacterType()
  {
    return false;
  }

  /**
   * Return true if type represents an incomplete type.
   */
  public boolean isIncompleteType()
  {
    return false;
  }

  /**
   * Return true if type represents an address.
   */
  public boolean isPointerType()
  {
    return false;
  }

  /**
   * Return true if type represents no type.
   */
  public boolean isVoidType()
  {
    return false;
  }

  /**
   * Return true if type represents an aggregate object.
   */
  public boolean isAggregateType()
  {
    return false;
  }

  /**
   * Return true if type represents a union.
   */
  public boolean isUnionType()
  {
    return false;
  }

  /**
   * Return true if type represents a composite type.
   */
  public boolean isCompositeType()
  {
    return false;
  }

  /**
   * Return true if type represents a floating point value.
   */
  public boolean isRealType()
  {
    return false;
  }

  /**
   * Return true if type represents an integer value.
   */
  public boolean isIntegerType()
  {
    return false;
  }

  /**
   * Return true if type represents an enumeration value.
   */
  public boolean isEnumerationType()
  {
    return false;
  }

  /**
   * Return true if type represents a complex value.
   */
  public boolean isComplexType()
  {
    return false;
  }

  /**
   * Return true if type represents an atomic type.
   */
  public boolean isAtomicType()
  {
    return false;
  }

  /**
   * Return true if type represents a boolean type.
   */
  public boolean isBooleanType()
  {
    return false;
  }

  /**
   * Return true if type represents a Fortran CHARACTER type.
   */
  public boolean isFortranCharType()
  {
    return false;
  }

  /**
   * Return true if type represents a numeric type.
   */
  public boolean isNumericType()
  {
    return false;
  }

  /**
   * Return true if this type has an associated TypeDecl or TypeName.
   */
  public boolean isNamedType()
  {
    return false;
  }

  /**
   * Return true if this type represents a procedure.
   */
  public boolean isProcedureType()
  {
    return false;
  }

  /**
   * Return true if the type is signed.
   */
  public boolean isSigned()
  {
    return false;
  }

  /**
   * Return the equivalent signed type of the <b>core</b> type.
   */
  public Type getSignedType()
  {
    return this;
  }

  /**
   * Return the number of elements represented by this type.
   */
  public long numberOfElements()
  {
    return 1;
  }

  /**
   * Return true if the attribute is associated with this type.
   * @see scale.clef.type.RefType
   */
  public boolean isAttributeSet(RefAttr attribute)
  {
    return false;
  }

  public void visit(Predicate p)
  {
    p.visitType(this);
  }

  /**
   * Process a node by calling its associated routine.
   * See the "visitor" design pattern in <cite>Design Patterns:
   * Elements of Reusable Object-Oriented Software</cite> by E. Gamma,
   * et al, Addison Wesley, ISBN 0-201-63361-2.
   * <p>
   * Each type class has a <code>visit(TypePredicate p)</code> method.
   * For example, in <code>class ABC</code>:
   * <pre>
   *   public void visit(Predicate p)
   *   {
   *     p.visitABC(this);
   *   }
   * </pre>
   * and the class that implements <code>Predicate</code> has a method
   * <pre>
   *   public void visitABC(Node n)
   *   {
   *     ABC a = (ABC) n;
   *     ...
   *   }
   * </pre>
   * Thus, the class that implements <code>TypePredicate</code> can call
   * <pre>
   *   n.visit(this);
   * </pre>
   * where <code>n</code> is a <code>Node</code> sub-class without
   * determining which specific sub-class <code>n</code> is.
   * The visit pattern basically avoids implementing a large
   * <code>switch</code> statement or defining different methods
   * in each class for some purpose.
   * @see TypePredicate
   */
  public void visit(TypePredicate p)
  {
    p.visitType(this);
  }

  /**
   * This method filters out some of the special case type nodes from
   * a type DAG.
   * <p>
   * We define <IT>core</IT> type as being a primitive type, or a type
   * built from type constructors.  Starting at the root of a type
   * DAG, this method skips over select speical case type nodes and
   * returns the first core type node.  Well, actually, it returns the
   * first anything that is not one of the select special case nodes.
   * The select special case nodes are (currently) as follows:
   *   <UL>
   *   <LI> IncompleteType
   *   <LI> RefType
   *   </UL>
   * <p>
   * The remaining two special case type nodes are simply irrelevant
   * for my current application, so I haven't bothered to determine
   * what happens to them.  This nondecision may be changed as
   * appropriate in the future.  Null implies the absense of a type,
   * so no core type would exist.
   * <p>
   * This method instance defines the behavior for all core types,
   * which is to simply return the current type.  This method needs to
   * be overridden in the select special case type classes.
   */
  public Type getCoreType()
  {
    return this;
  }

  public Type getType()
  {
    return this;
  }

  /**
   * Return the type to use if a variable of this type is in a
   * register.
   */
  public Type registerType()
  {
    return this;
  }

  /**
   * Return the completed type or null if it is not complete.
   * @see IncompleteType
   */
  public Type getCompleteType()
  {
    return this;
  }

  /**
   * Return the type without any attributes such as "const".
   * @see RefType
   */
  public Type getNonAttributeType()
  {
    Declaration decl  = getDecl();
    Type        type2 = getCoreType();

    if (type2 == null)
      System.out.println("gNAT " + this);

    if (decl != null)
      type2 = RefType.create(type2, decl);

    return type2;
  }

  /**
   * Return the type without any "const" attributes.
   * @see RefType#getNonConstType
   */
  public Type getNonConstType()
  {
    return this;
  }

  /**
   * Return true if the types are equivalent.  Types are equivalent if
   * they are the same except for names.  For example,
   * <pre>
   *    struct x { int a; int b;}
   * </pre>
   * is equivalent to
   * <pre>
   *    struct x { int c; int d;}
   * </pre>
   */
  public abstract boolean equivalent(Type t);

  /**
   * Return the type to be used by the equivalence method.
   */
  protected Type getEquivalentType()
  {
    return this;
  }

  /**
   * Return the type of the thing pointed to.
   */
  public Type getPointedTo()
  {
    return this;
  }

  /**
   * Return the type of the thing pointed to.
   * Equivalent to
   * <pre>
   *   getCoreType().getPointedTo().getCoreType()
   * </pre>
   */
  public final Type getPointedToCore()
  {
    return getCoreType().getPointedTo().getCoreType();
  }

  /**
   * Return the rank of the type.  For a scalar, the rank is 0.  For
   * an array, the rank is the number of dimensions.
   */
  public int getRank()
  {
    return 0;
  }

  /**
   * Return true if the type specifies const.
   */
  public boolean isConst()
  {
    return false;
  }

  /**
   * Return true if the type specifies volatile.
   */
  public boolean isVolatile()
  {
    return false;
  }

  /**
   * Return true if the type specifies restricted.
   */
  public boolean isRestricted()
  {
    return false;
  }

  /**
   * Return the number of addressable memory units that are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   */
  public abstract long memorySize(Machine machine);

  /**
   * Return the number of addressable memory units are needed to
   * represent an element of the type.  Except for arrays this is
   * identical to the value return by {@link #memorySize
   * memorySize()}.
   * @param machine is the machine-specific data machine
   */
  public long elementSize(Machine machine)
  {
    return memorySize(machine);
  }

  /**
   * Calculate how many addressable memory units that are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of addressable memory units required to
   * represent this type
   */
  public final int memorySizeAsInt(Machine machine)
  {
    long size = memorySize(machine);
    assert (size <= 0x7fffffff) : "memory size too large " + size;
    return (int) size;
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public abstract int alignment(Machine machine);

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
    return 1;
  }

  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToCString()
  {
    throw new scale.common.InternalError("Incorrect type " + this);
  }

  /**
   * Map a type to a Fortran string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToF77String()
  {
    throw new scale.common.InternalError("Incorrect type " + this);
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    scale.clef.type.Bound.cleanup();
    scale.clef.type.CharacterType.cleanup();
    scale.clef.type.ComplexType.cleanup();
    scale.clef.type.EnumerationType.cleanup();
    scale.clef.type.FixedArrayType.cleanup();
    scale.clef.type.AllocArrayType.cleanup();
    scale.clef.type.FortranCharType.cleanup();
    scale.clef.type.FloatType.cleanup();
    scale.clef.type.SignedIntegerType.cleanup();
    scale.clef.type.UnsignedIntegerType.cleanup();
    scale.clef.type.PointerType.cleanup();
    scale.clef.type.ProcedureType.cleanup();
    scale.clef.type.RecordType.cleanup();
    scale.clef.type.RefType.cleanup();
    scale.clef.type.UnionType.cleanup();
  }
}
