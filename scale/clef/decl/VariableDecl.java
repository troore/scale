package scale.clef.decl;

import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;
import scale.backend.ResultMode;
import scale.backend.Displacement;

/**
 * This class declares a variable.
 * <p>
 * $Id: VariableDecl.java,v 1.103 2007-08-28 13:34:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If you want to name your generated variables <code>_inxxx</code>
 * where <code>xxx</code> is a number you would do
 * <pre>
 *   UniqueName un = new UniqueName("_in");
 *   ...
 *   VariableDecl vd = new VariableDecl(un.genName(), type);
 *   vd.setTemporary();
 *   ...
 * </pre>
 * If you don't care about the name just do
 * <pre>
 *   VariableDecl vd = new VariableDecl(type);
 * </pre>
 * The naming scheme is basically <code>_oonnn</code> where
 * <code>oo</code> identifies the optimization that created the
 * variable.  Compiler created variables should be temporary variables
 * unless they must reside in memory.  Some values in use for
 * <code>oo</code> are
 * <table cols=3 border=1><thead>
 * <tr><th><code>oo</code></th><th>creater</th><th>use</th>
 * </thead><tbody>
 * <tr><td><code> A</code></td>
 *     <td><code>parser</code></td><td>Function prototype argument names</td>
 * <tr><td><code> V</code></td>
 *     <td><code>parser</code></td><td>Compiler created variables</td>
 * <tr><td><code>_t</code></td>
 *     <td><code>FindAliases</code></td><td>Compiler created temporary
 *     alias variables</td>
 * <tr><td><code>vv</code></td>
 *     <td><code>Aliases</code></td><td>Compiler created temporary
 *     alias virtual variables</td>
 * <tr><td><code>rv_</code></td>
 *     <td><code>Aliases</code></td><td>Compiler created temporary
 *     alias variables</td>
 * <tr><td><code>addrtaken_</code></td>
 *     <td><code>Aliases</code></td><td>Compiler created temporary
 *     alias variables</td>
 * <tr><td><code> s</code></td>
 *     <td><code>Scribble</code></td>
 *     <td>Compiler created temporary variables</td>
 * <tr><td><code>vn</code></td>
 *     <td><code>ValNum</code></td><td>Compiler created temporary variables</td>
 * <tr><td><code>li</code></td>
 *     <td><code>LICM</code></td><td>Compiler created temporary variables</td>
 * <tr><td><code>sr</code></td>
 *     <td><code>ScalarReplacement</code></td>
 *     <td>Compiler created temporary variables</td>
 * <tr><td><code>np</code></td>
 *     <td><code>frontend</code></td><td>Compiler created temporary variables</td>
 * <tr><td><code>cs</code></td>
 *     <td><code>Clef2Scribble</code></td><td>Compiler created
 *     temporary variables</td>
 * </tbody></table>
 *
 * @see scale.common.UniqueName
 */
public class VariableDecl extends ValueDecl
{
  /**
   * Set this flag true to study the effect of the address-taken
   * attribute.  Note, when this flag is set true programs will not be
   * compiled correctly.
   */
  public static boolean ignoreAddressTaken = false;

  private static final int IS_REF                   = 0x001;
  private static final int IS_TEMP                  = 0x002;
  private static final int IS_FTN_RESULT            = 0x004;
  private static final int ADDRESS_TAKEN            = 0x008;
  private static final int INVALID_ARRAY_REFERENCES = 0x010;
  private static final int COMMON_BASE_VARIABLE     = 0x020;
  private static final int HIDDEN_ALIASES           = 0x040;
  private static final int HIDDEN_PTR_ALIASES       = 0x080;
  private static final int OPTIMIZATION_CANDIDATE   = 0x100;
  private static final int SHOULD_BE_IN_REGISTER    = 0x200;
  private static final int SSA_FORM_ALLOWED         = 0x400;
  private static final int ALIASES_ALLOWED          = 0x800;

  private short        flags; // Various status flags. Do not reference directly.
  private ResultMode   vr;
  private Residency    res;
  private Assigned     assigned;
  private Displacement disp; // Displacement used by the code generator.
  /**
   * Used by the code generator to specify the register containing the
   * value or address of the variable when the variable is normally in
   * memory, it may be temporarily assigned to a register or its
   * address may be temporarily assigned to a register.
   * @see scale.backend.Generator
   */
  private int valReg;
  /**
   * Used by the code generator to specify the register containing the
   * address of the variable when the address is wanted.
   * @see scale.backend.Generator
   */
  private int adrReg;

  public VariableDecl(String name, Type type, Expression initialValue)
  {
    super(name, type, initialValue);
    this.res      = Residency.AUTO;
    this.assigned = Assigned.NOT_ALLOCATED;
    computeAttributes();
  }

  /**
   * Create a variable declaration which doesn't have a default value.
   */
  public VariableDecl(String name, Type type)
  {
    this(name, type, null);
  }

  public void setType(Type type)
  {
    super.setType(type);
    computeAttributes();
  }

  /**
   * Specify the visibility of the declaration.
   * @param visibility is the visibility of the declaration
   * @see Visibility
   */
  public void setVisibility(Visibility visibility)
  {
    super.setVisibility(visibility);
    if (visibility == Visibility.EXTERN)
      setResidency(Residency.MEMORY);
    computeAttributes();
  }

  /**
   * Specify the residency of the declaration.
   * @param residency is the residency of the declaration
   * @see Residency
   */
  public void setResidency(Residency residency)
  {
    this.res = residency;
    computeAttributes();
  }

  /**
   * Return the declaration residency.
   * @see Residency
   */
  public Residency residency()
  {
    return res;
  }

  /**
   * Return true if the location type used for this declaration is
   * known.
   */
  protected final boolean isStorageLocSet()
  {
    return assigned != Assigned.NOT_ALLOCATED;
  }

  /**
   * Return the location type used for this declaration.  If the
   * location type has not yet been decided, this routine determines
   * where the variable should be located.  Variables whose residency
   * is memory, whose visibility is not local, or that have an initial
   * value are assigned to memory.  Variables whose address is taken
   * or that can not be assigned to a register, are allocated to the
   * stack.
   * @see #setStorageLoc
   * @see scale.common.Machine#keepTypeInRegister
   */
  public Assigned getStorageLoc()
  {
    Assigned alloc = assigned;
    if (alloc == Assigned.NOT_ALLOCATED) {
      if (residency() == Residency.MEMORY)
        alloc = Assigned.IN_MEMORY;
      else if (visibility() != Visibility.LOCAL)
        alloc = Assigned.IN_MEMORY;
      else if (getInitialValue() != null)
        alloc = Assigned.IN_MEMORY;
      else if (addressTaken())
        alloc = Assigned.ON_STACK;
      else if (Machine.currentMachine.keepTypeInRegister(getType(), isTemporary()))
        alloc = Assigned.IN_REGISTER;
      else
        alloc =  Assigned.ON_STACK;

      setStorageLoc(alloc);
    }
    return alloc;
  }

  /**
   * Set the location type used for this declaration.
   */
  public final void setStorageLoc(Assigned loc)
  {
    this.assigned = loc;
  }

  /**
   * Return true if this declaration is to be allocated to memory.
   */
  public boolean inMemory()
  {
    return (res == Residency.MEMORY);
  }

  /**
   * Return the displacement the code generator assigned to this variable.
   */
  public final scale.backend.Displacement getDisplacement()
  {
    assert (getStorageLoc() != Assigned.IN_REGISTER) :
      "Storage location " + getStorageLoc();
    return disp;
  }

  /**
   * Specify the displacement the code generator assigned to this variable.
   */
  public final void setDisplacement(scale.backend.Displacement disp)
  {
    assert (getStorageLoc() != Assigned.IN_REGISTER) :
      "Storage location " + getStorageLoc();
    this.disp = disp;
    if (addressTaken() && disp.isSymbol())
      ((scale.backend.SymbolDisplacement) disp).setAddressTaken(true);
  }

  /**
   * Return the register the code generator assigned to this variable.
   */
  public final int getValueRegister()
  {
    return valReg;
  }

  /**
   * Return the register mode that the code generator temporarily
   * assigned to this variable.
   * @see scale.backend.Generator
   */
  public final ResultMode valueRegMode()
  {
    return vr;
  }

  /**
   * Specify the register the code generator temporarily assigned to
   * this variable.
   * @see scale.backend.Generator
   */
  public final void setValueRegister(int reg, ResultMode mode)
  {
    this.valReg = reg;
    this.vr     = mode;
  }

  /**
   * Return the register the code generator assigned to the address of
   * this variable.
   */
  public final int getAddressRegister()
  {
    return adrReg;
  }

  /**
   * Specify the register the code generator assigned to the address
   * of this variable.
   */
  public final void setAddressRegister(int reg)
  {
    this.adrReg = reg;
  }

  /**
   * Return true if the declaration is referenced somewhere in the
   * Clef AST.
   */
  public boolean isReferenced()
  {
    return (flags & IS_REF) != 0;
  }

  /**
   * Specify that the variable is referenced somewhere in the Clef
   * AST.
   */
  public final void setReferenced()
  {
    flags |= IS_REF;
  }

  /**
   * Return true if the variable is a const variable.
   */
  public boolean isConst()
  {
    return getType().isConst();
  }

  /**
   * Return true if the declaration is a temporary variable.
   */
  public final boolean isTemporary()
  {
    return (flags & IS_TEMP) != 0;
  }

  /**
   * Specify that the variable is a temporary variable.
   */
  public final void setTemporary()
  {
    flags |= IS_TEMP;
  }

  /**
   * Return true if the variable may be modified in an unknown way.
   */
  public boolean hasHiddenAliases()
  {
    return (flags & HIDDEN_ALIASES) != 0;
  }

  /**
   * Specify that the variable may be modified in an unknown way.
   */
  public final void setHiddenAliases()
  {
    flags |= HIDDEN_ALIASES;
    computeAttributes();
  }

  /**
   * Return true if the memory, to which this pointer variable refers,
   * may be modified in an unknown way.
   */
  public boolean hasHiddenPtrAliases()
  {
    return (flags & HIDDEN_PTR_ALIASES) != 0;
  }

  /**
   * Specify that the memory, to which this pointer variable refers, may
   * be modified in an unknown way.
   */
  public final void setHiddenPtrAliases()
  {
    assert getCoreType().isPointerType() : getCoreType();
    flags |= HIDDEN_PTR_ALIASES;
    computeAttributes();
  }

  /**
   * Return true if the declaration is declared as the result variable
   * of a function.
   */
  public final boolean isFtnResultVar()
  {
    return (flags & IS_FTN_RESULT) != 0;
  }

  /**
   * Specify that the variable is declared as the result variable of a
   * function.
   */
  public final void declareFtnResultVar()
  {
    flags |= IS_FTN_RESULT;
  }

  /**
   * Record that the address is used.
   */
  public final void setAddressTaken()
  {
    flags |= ADDRESS_TAKEN;
    computeAttributes();
  }

  /**
   * Return true if the address of this Declaration has been taken.
   */
  public boolean addressTaken()
  {
    if (ignoreAddressTaken)
      return false;
    return (flags & ADDRESS_TAKEN) != 0;
  }

  /**
   * Specify that this variable is the base variable for a Fortram
   * COMMON.
   */
  public final void specifyCommonBaseVariable()
  {
    flags |= COMMON_BASE_VARIABLE;
  }

  /**
   * Return true if this is the base variable of a Fortran COMMON
   * area.
   */
  public final boolean isCommonBaseVariable()
  {
    return (flags & COMMON_BASE_VARIABLE) != 0;
  }

  /**
   * Record that the array specified by this variable has invalid
   * array references.  An example of an invalid array reference is
   * something like
   * <pre>
   *    ((int) ((char *) a + 14))[5]
   * </pre>
   */
  public final void setInvalidArrayReferences()
  {
    flags |= INVALID_ARRAY_REFERENCES;
  }

  /**
   * Return true if the array specified by this variable has invalid
   * array references.  An example of an invalid array reference is
   * something like
   * <pre>
   *    ((int) ((char *) a + 14))[5]
   * </pre>
   */
  public final boolean hasInvalidArrayReferences()
  {
    return (flags & INVALID_ARRAY_REFERENCES) != 0;
  }

  /**
   * Determine how the variable must be handled.  There seems to be
   * some redundancy involved but we will leave the resolution to some
   * future date.  It is true that if the variable is an optimization
   * candidate, then it must be able to be put in SSA form.
   */
  protected final void computeAttributes()
  {
    int aliasesAllowed        = ALIASES_ALLOWED;
    int ssaForm               = SSA_FORM_ALLOWED;
    int shouldBeInRegister    = SHOULD_BE_IN_REGISTER;
    int optimizationCandidate = OPTIMIZATION_CANDIDATE;

    flags &= ~(ALIASES_ALLOWED |
               SSA_FORM_ALLOWED |
               SHOULD_BE_IN_REGISTER |
               OPTIMIZATION_CANDIDATE);

    if (isGlobal()) {
      optimizationCandidate = 0;
      shouldBeInRegister    = 0;
      ssaForm               = 0;
      aliasesAllowed        = 0;
      return; // Remove return if item above removed.
    }

    Type type = getType();
    if (!type.getCoreType().isAtomicType()) {
      optimizationCandidate = 0;
      shouldBeInRegister    = 0;
      ssaForm               = 0;
      aliasesAllowed        = 0;
      return; // Remove return if item above removed.
    }

    if (addressTaken()) {
      optimizationCandidate = 0;
      shouldBeInRegister    = 0;
      ssaForm               = 0;
      aliasesAllowed        = 0;
      return; // Remove return if item above removed.
    }

    if (type.isVolatile()) {
      optimizationCandidate = 0;
      shouldBeInRegister    = 0;
      ssaForm               = 0;
      aliasesAllowed        = 0;
      return; // Remove return if item above removed.
    }

    if (type.isConst()) {
      optimizationCandidate = 0;
      ssaForm               = 0;
    }

    if (inMemory()) {
      shouldBeInRegister    = 0;
      aliasesAllowed        = 0;
    }

    if (hasHiddenAliases()) {
      ssaForm               = 0;
      optimizationCandidate = 0;
      aliasesAllowed        = 0;
    }

    if (getValue() != null) {
      optimizationCandidate = 0;
      shouldBeInRegister    = 0;
    }

    flags |= (aliasesAllowed |
              ssaForm |
              shouldBeInRegister |
              optimizationCandidate);
  }

  /**
   * Return true if this variable can not be in SSA form.  A variable
   * cannot be put in SSA form if
   * <ul>
   * <li>it is not an atomic type (e.g., an array or <code>struct</code>),
   * <li>its address is taken,
   * <li>it is <code>volatile</code>,
   * <li>it is a global variable, or
   * <li>it has hidden aliases.
   * </ul>
   * Also, there is no need to place <code>const</code> variables in
   * SSA form.
   */
  public final boolean isNotSSACandidate()
  {
    return (flags & SSA_FORM_ALLOWED) == 0;
  }

  /**
   * Return true if no aliases should be created for this variable.
   * An aliases should not be created if this variable
   * <ul>
   * <li>resides in memory,
   * <li>is global,
   * <li>has its address taken,
   * <li>has hidden aliases,
   * <li>is referenced indirectly, or
   * <li>is not an atomic type (e.g., an array or <code>struct</code>).
   * </ul>
   * An "alias" is usually created by using a copy of the variable
   * that is maintained in a register.
   */
  public final boolean isNotAliasCandidate()
  {
    return (flags & ALIASES_ALLOWED) == 0;
  }

  /**
   * Return true if this variable should be allocated to a register.
   * A variable can not be allocated to a register if
   * <ul>
   * <li>it is global,
   * <li>it is not an atomic type,
   * <li>its address is taken,
   * <li>it is <code>volatile</code>,
   * <li>it is allocted to memory, or
   * <li>it has an initial value.
   * </ul>
   */
  public final boolean shouldBeInRegister()
  {
    return (flags & SHOULD_BE_IN_REGISTER) != 0;
  }

  /**
   * Return true if this variable
   * <ul>
   * <li>is not a global variable,
   * <li>is not an atomic type,
   * <li>is not <code>volatile</code>,
   * <li>is not <code>const</code>,
   * <li>has no hidden alias,
   * <li>does not have its address taken, and
   * <li>does not have an initial value.
   * </ul>
   */
  public final boolean optimizationCandidate()
  {
    return (flags & OPTIMIZATION_CANDIDATE) != 0;
  }

  /**
   * Return true if the type specifies volatile.
   */
  public final boolean isVolatile()
  {
    return getType().isVolatile();
  }

  /**
   * Return true if the type specifies restricted.
   */
  public final boolean isRestricted()
  {
    return getType().isRestricted();
  }

  public void visit(Predicate p)
  {
    p.visitVariableDecl(this);
  }

  /**
   * Return the initializer for this variable or null.
   */
  public final Expression getInitialValue()
  {
    return getValue();
  }

  /**
   * Specify the initial value for a variable.
   * This method is used in those cases where the variable 
   * must be defined before its initial value is known.
   */
  public final void setInitialValue(Expression value)
  {
    super.setValue(value);
    computeAttributes();
  }

  /**
   * Specify the initial value for a variable.
   * This method is used in those cases where the variable 
   * must be defined before its initial value is known.
   */
  public final void setValue(Expression value)
  {
    super.setValue(value);
    computeAttributes();
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public scale.clef.expr.Literal getConstantValue()
  {
    if (isConst())
      return super.getConstantValue();
    return (isGlobal() ? Lattice.Bot : Lattice.Top);
  }

  /**
   * Return true if this declaration is static.
   */
  public final boolean isStatic()
  {
    return (residency() == Residency.MEMORY);
  }

  /**
   * Return the variable that this variable was renamed from.
   */
  public VariableDecl getOriginal()
  {
    return this;
  }

  public String toStringSpecial()
  {
    String    s            = super.toStringSpecial();
    Residency residency    = residency();
    Assigned  allocated    = assigned;
    boolean   addressTaken = (flags & ADDRESS_TAKEN) != 0;

    StringBuffer buf = new StringBuffer(s);

    buf.append(' ');
    buf.append(res);
    buf.append(' ');
    buf.append(allocated);

    buf.append(addressTaken            ? " address-taken"  : "");
    buf.append(isTemporary()           ? " temp"           : "");
    buf.append(isCommonBaseVariable()  ? " cvb"            : "");
    buf.append(hasHiddenAliases()      ? " hidden-aliases" : "");
    buf.append(isReferenced()          ? " refed"          : "");
    buf.append(!isNotSSACandidate()    ? " ssa-able"       : "");
    buf.append(!isNotAliasCandidate()  ? " alias-able"     : "");
    buf.append(optimizationCandidate() ? " opt-able"       : "");
    buf.append(shouldBeInRegister()    ? " reg-able"       : "");

    return buf.toString();
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name)
  {
    VariableDecl vd = new VariableDecl(name, getType(), getValue());
    vd.flags = (short) (flags & ~IS_FTN_RESULT);
    return vd;
  }

  /**
   * Return true if the variable is the base variable for Fortran
   * COMMON.
   */
  public final boolean isCommonBaseVar()
  {
    return (flags & COMMON_BASE_VARIABLE) != 0;
  }

  public final boolean isVariableDecl()
  {
    return true;
  }

  public final VariableDecl returnVariableDecl()
  {
    return this;
  }
}
