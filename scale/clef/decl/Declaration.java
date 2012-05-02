package scale.clef.decl;

import java.util.Enumeration;
import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;
import scale.backend.ResultMode;

/**
 * This is the base class for declarations such as variable, routines,
 * etc.
 * <p>
 * $Id: Declaration.java,v 1.106 2007-10-04 19:58:03 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class Declaration extends Node implements Comparable
{
  private String name;  // Name of the declaration.
  private String alias; // Alias for extern declaration or null if none.
  private Type   type;  // Type of the declaration.
  private int    tag ;  // An integer value associated with the declaration that various algorithms may use.
  private boolean    weak; // 
  private Visibility vis;
  /**
   * Create a declaration with the specified name and no type.
   */
  protected Declaration(String name)
  {
    this(name, VoidType.type);
  }
  
  /**
   * Create a declaration with the specified name and type.
   */
  protected Declaration(String name, Type type)
  {
    setName(name);
    this.type = type;
    this.weak = false;
    this.vis  = Visibility.LOCAL;
  }

  /**
   * Use the name of the declaration instead of the address of the
   * instance so things are more predictable.
   */
  public int hashCode()
  {
    return name.hashCode();
  }

  public final int compareTo(Object a)
  {
    if (a == null)
      return -1;

    if (getClass() == a.getClass())
      return name.compareTo(((Declaration) a).name);

    return getClass().getName().compareTo(a.getClass().getName());
  }

  /**
   * Specify the accessibility of the declaration.
   * @param accessibility is the accessibility of the declaration
   * @see Accessibility
   */
  public void setAccessibility(Accessibility accessibility)
  {
    throw new scale.common.InternalError("Can't set the accessibility of this declaration " + this);
  }

  /**
   * Return the declaration accessibility.
   * @see Accessibility
   */
  public Accessibility accessibility()
  {
    return Accessibility.PUBLIC;
  }

  /**
   * Specify the visibility of the declaration.
   * @param visibility is the visibility of the declaration
   * @see Visibility
   */
  public void setVisibility(Visibility visibility)
  {
    this.vis = visibility;
  }

  /**
   * Return the declaration visibility.
   * @see Visibility
   */
  public Visibility visibility()
  {
    return vis;
  }

  /**
   * Return true if the declaration has the "weak" attribute.
   */
  public final boolean isWeak()
  {
    return weak;
  }
  
  /**
   * Specify that the declaration has the "weak" attribute.
   */
  public final void setWeak(boolean weak)
  {
    this.weak = weak;
  }

  /**
   * Return the alias for this declaration.
   */
  public final String getAlias()
  {
    return alias;
  }
  
  /**
   * Set the alias for this declaration.  Variable declared to be
   * <code>extern</code> may be aliased to another name.
   */
  public final void setAlias(String alias)
  {
    this.alias = alias;
  }
  
  /**
   * Specify the residency of the declaration.
   * @param residency is the residency of the declaration
   * @see Residency
   */
  public void setResidency(Residency residency)
  {
    throw new scale.common.InternalError("Can't set the residency of this type of declaration " + this);
  }

  /**
   * Return the declaration residency.
   * @see Residency
   */
  public Residency residency()
  {
    return Residency.AUTO;
  }

  /**
   * Record that the address is used.
   */
  public void setAddressTaken()
  {
    /* Do nothing for non-variables. */
  }

  /**
   * Return true if the address of this Declaration has been taken.
   */
  public boolean addressTaken()
  {
    return true;
  }

  /**
   * Return true if the declaration may be modified in an unknown way.
   */
  public boolean hasHiddenAliases()
  {
    return true;
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
   * Return the location type used for this declaration.
   */
  public Assigned getStorageLoc()
  {
    return Assigned.IN_MEMORY;
  }

  /**
   * Return the register the code generator assigned to this variable.
   */
  public int getValueRegister()
  {
    return -1;
  }

  /**
   * Return true if the register the code generator assigned to this
   * variable contains the address and not the value.
   */
  public ResultMode valueRegMode()
  {
    return ResultMode.NO_VALUE;
  }

  /**
   * Specify the register the code generator assigned to this variable.
   */
  public void setValueRegister(int reg, ResultMode mode)
  {
    throw new scale.common.InternalError("Specify value of " + this);
  }

  /**
   * Return the register the code generator assigned to the address of
   * this variable.
   */
  public int getAddressRegister()
  {
    return -1;
  }

  /**
   * Specify the register the code generator assigned to the address
   * of this variable.
   */
  public void setAddressRegister(int reg)
  {
    throw new scale.common.InternalError("Specify address of " + this);
  }

  /**
   * Set the location type used for this declaration.
   */
  public void setStorageLoc(Assigned loc)
  {
    assert (loc == Assigned.IN_MEMORY) : "Invalid storage location " + loc;
  }

  /**
   * Return the code generator displacement associated with this call
   * node.
   */
  public scale.backend.Displacement getDisplacement()
  {
    return null;
  }

  /**
   * Specify the code generator displacement associated with this call
   * node.
   */
  public void setDisplacement(scale.backend.Displacement disp)
  {
    throw new scale.common.InternalError("Displacement for " + this);
  }

  /**
   * Return any Declaration associated with this Node.
   */
  public Declaration getDecl()
  {
    return this;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(" \"");
    buf.append(name);
    buf.append("\" ");
    buf.append(type);
    if (vis != Visibility.LOCAL) {
      buf.append(' ');
      buf.append(vis);
    }
    return buf.toString();
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.  This method should be over-ridden as it simplay returns
   * the class name.
   */
  public String getDisplayLabel()
  {
    String mnemonic = toStringClass();
    if (mnemonic.endsWith("Decl"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 4);

    StringBuffer buf = new StringBuffer(mnemonic);
    buf.append(' ');
    buf.append(name);
    if (vis != Visibility.LOCAL) {
      buf.append(' ');
      buf.append(vis);
    }
    return buf.toString();
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
   * simplay returns the shape "box".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.BOX;
  }

  public void visit(Predicate p)
  {
    p.visitDeclaration(this);
  }

  /**
   * Return the name of the Declaration.
   */
  public final String getName()
  {
    return name;
  }

  /**
   * Return the type of the Declaration.  Use this method to obtain
   * the complete type; if the type is a completed incomplete type,
   * the completed type is returned.
   */
  public final Type getType()
  {
    Type           t   = type;
    IncompleteType ict = t.returnIncompleteType();
    if (ict != null) {
      t = ict.getCompleteType();
      if (t == null)
        t = type;
      else
        type = t;
    }
    return t;
  }

  /**
   * Return the type of the Declaration.
   */
  public final Type getActualType()
  {
    return type;
  }

 /**
   * Return the type of the Declaration without attributes.
   * Use this method for comparing base types.
   */
  public final Type getCoreType()
  {
    Type t = getType();
    if (t != null)
      t = t.getCoreType();

    return t;
  }

  /**
   * Return the type of the thing pointed to by the type of the expression.
   * Equivalent to
   * <pre>
   *   getCoreType().getPointedTo().getCoreType()
   * </pre>
   */
  public final Type getPointedToCore()
  {
    return type.getCoreType().getPointedTo().getCoreType();
  }

  /**
   * Change the display name of the declaration.
   * @param name is the new name for the declaration
   */
  public final void setName(String name)
  {
    this.name = name;
  }

  /**
   * Specify the type of this declaration.
   */
  public void setType(Type type)
  {
    this.type = type;
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public scale.clef.expr.Literal getConstantValue()
  {
    return Lattice.Bot;
  }

  /**
   * Return true if the declaration is referenced somewhere in the
   * Clef AST.  Must declarations are considered to be referenced
   * whether or not they actually are.  However, variables and
   * routines without bodies may not be referenced and the over-ridden
   * version of this method may return false.
   * @see VariableDecl
   * @see RoutineDecl
   */
  public boolean isReferenced()
  {
    return true;
  }

  /**
   * Specify that the Declaration is referenced somewhere in the Clef
   * AST.  This method must be over-ridden for a declataion that may
   * or may not be referenced.
   * @see VariableDecl
   * @see RoutineDecl
   */
  public void setReferenced()
  {
  }

  /**
   * Return true if this declaration is globally visible.
   */
  public boolean isGlobal()
  {
    return (visibility() != Visibility.LOCAL);
  }

  /**
   * Return true if this declaration is to be allocated to memory.
   */
  public boolean inMemory()
  {
    return true;
  }

  /**
   * Return true if the declaration is a temporary variable.
   */
  public boolean isTemporary()
  {
    return false;
  }

  /**
   * Return true if the declaration is declared as the result variable
   * of a function.
   */
  public boolean isFtnResultVar()
  {
    return false;
  }

  /**
   * Return true if CaseLabelDecl instance.
   */
  public boolean isCaseLabelDecl()
  {
    return false;
  }

  /**
   * Return a {@link CaseLabelDecl CaseLabelDecl} instance or
   * <code>null</code>.
   */
  public CaseLabelDecl returnCaseLabelDecl()
  {
    return null;
  }

  /**
   * Return true if EnumElementDecl instance.
   */
  public boolean isEnumElementDecl()
  {
    return false;
  }

  /**
   * Return a {@link EnumElementDecl EnumElementDecl} instance or
   * <code>null</code>.
   */
  public EnumElementDecl returnEnumElementDecl()
  {
    return null;
  }

  /**
   * Return true if the declaration is a variable in Fortran COMMON.
   */
  public boolean isEquivalenceDecl()
  {
    return false;
  }

  /**
   * Return a {@link EquivalenceDecl EquivalenceDecl} instance or
   * <code>null</code>.
   */
  public EquivalenceDecl returnEquivalenceDecl()
  {
    return null;
  }

  /**
   * Return true if ExceptionDecl instance.
   */
  public boolean isExceptionDecl()
  {
    return false;
  }

  /**
   * Return a {@link ExceptionDecl ExceptionDecl} instance or
   * <code>null</code>.
   */
  public ExceptionDecl returnExceptionDecl()
  {
    return null;
  }

  /**
   * Return true if FieldDecl instance.
   */
  public boolean isFieldDecl()
  {
    return false;
  }

  /**
   * R a {@link FieldDecl FieldDecl} instance or <code>null</code>.
   */
  public FieldDecl returnFieldDecl()
  {
    return null;
  }

  /**
   * Return true if FileDecl instance.
   */
  public boolean isFileDecl()
  {
    return false;
  }

  /**
   *  a {@link FileDecl FileDecl} instance or <code>null</code>.
   */
  public FileDecl returnFileDecl()
  {
    return null;
  }

  /**
   * Return true if this declaration is a formal parameter to the
   * routine.
   */
  public boolean isFormalDecl()
  {
    return false;
  }

  /**
   * Re a {@link FormalDecl FormalDecl} instance or <code>null</code>.
   */
  public FormalDecl returnFormalDecl()
  {
    return null;
  }

  /**
   * Return true if ForwardProcedureDecl instance.
   */
  public boolean isForwardProcedureDecl()
  {
    return false;
  }

  /**
   * Return a For a {@link ForwardProcedureDecl ForwardProcedureDecl}
   * instance or <code>null</code>.
   */
  public ForwardProcedureDecl returnForwardProcedureDecl()
  {
    return null;
  }

  /**
   * Return true if LabelDecl instance.
   */
  public boolean isLabelDecl()
  {
    return false;
  }

  /**
   * R a {@link LabelDecl LabelDecl} instance or <code>null</code>.
   */
  public LabelDecl returnLabelDecl()
  {
    return null;
  }

  /**
   * Return true if ProcedureDecl instance.
   */
  public boolean isProcedureDecl()
  {
    return false;
  }

  /**
   * Return a {@link ProcedureDecl ProcedureDecl} instance or
   * <code>null</code>.
   */
  public ProcedureDecl returnProcedureDecl()
  {
    return null;
  }

  /**
   * Return true if RenamedVariableDecl instance.
   */
  public boolean isRenamedVariableDecl()
  {
    return false;
  }

  /**
   * Return a Re a {@link RenamedVariableDecl RenamedVariableDecl}
   * instance or <code>null</code>.
   */
  public RenamedVariableDecl returnRenamedVariableDecl()
  {
    return null;
  }

  /**
   * Return true if RoutineDecl instance.
   */
  public boolean isRoutineDecl()
  {
    return false;
  }

  /**
   * Return a {@link RoutineDecl RoutineDecl} instance or
   * <code>null</code>.
   */
  public RoutineDecl returnRoutineDecl()
  {
    return null;
  }

  /**
   * Return true if StmtFtnDecl instance.
   */
  public boolean isStmtFtnDecl()
  {
    return false;
  }

  /**
   * Return a {@link StmtFtnDecl StmtFtnDecl} instance or
   * <code>null</code>.
   */
  public StmtFtnDecl returnStmtFtnDecl()
  {
    return null;
  }

  /**
   * Return true if TypeDecl instance.
   */
  public boolean isTypeDecl()
  {
    return false;
  }

  /**
   * Return a {@link TypeDecl TypeDecl} instance or <code>null</code>.
   */
  public TypeDecl returnTypeDecl()
  {
    return null;
  }

  /**
   * Return true if ValueDecl instance.
   */
  public boolean isValueDecl()
  {
    return false;
  }

  /**
   * Return a {@link ValueDecl ValueDecl} instance or <code>null</code>.
   */
  public ValueDecl returnValueDecl()
  {
    return null;
  }

  /**
   * Return true if VariableDecl instance.
   */
  public boolean isVariableDecl()
  {
    return false;
  }

  /**
   * Return a {@link VariableDecl VariableDecl} instance or
   * <code>null</code>.
   */
  public VariableDecl returnVariableDecl()
  {
    return null;
  }

  /**
   * Return true if TypeName instance.
   */
  public boolean isTypeName()
  {
    return false;
  }

  /**
   * Return a {@link TypeName TypeName} instance or
   * <code>null</code>.
   */
  public TypeName returnTypeName()
  {
    return null;
  }

  /**
   * Return true if UnknownFormals instance.
   */
  public boolean isUnknownFormals()
  {
    return false;
  }

  /**
   * Return a {@link UnknownFormals UnknownFormals} instance or
   * <code>null</code>.
   */
  public UnknownFormals returnUnknownFormals()
  {
    return null;
  }

  /**
   * Return true if this is the base variable of a Fortran COMMON area.
   */
  public boolean isCommonBaseVariable()
  {
    return false;
  }

  /**
   * Return true if the declaration is a virtual variable.
   */
  public boolean isVirtual()
  {
    return false;
  }

  /**
   * Return true if the declaration is a renamed variable.
   */
  public boolean isRenamed()
  {
    return false;
  }

  /**
   * Return true if this declaration has a purity level of PUREGV.
   */
  public boolean isPure()
  {
    return false;
  }

  /**
   * Return true.
   */
  public boolean isConst()
  {
    return true;
  }

  /**
   * Return the parameter passing mode of this declaration.  Unless
   * this declaration is a {@link scale.clef.decl.FormalDecl formal
   * parameter} to a routine, the value returned is
   * <code>cValue</code>.
   */
  public ParameterMode getMode()
  {
    return ParameterMode.VALUE;
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public abstract Declaration copy(String name);
}
