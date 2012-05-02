package scale.clef.decl;

import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class declares a variable that is equivalenced to an offset in
 * another variable.
 * <p>
 * $Id: EquivalenceDecl.java,v 1.39 2007-10-04 19:58:04 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class declares a variable that is equivalenced to an offset in
 * another variable.  An equivalence relationship is generated for
 * Fortran EQUIVALENCE statements and COMMON statements.  For the
 * statement
 * <pre>
 * EQUIVALENCE A, B
 * </pre>
 * the base variable would be A with a offset of zero.
 * For the statement
 * <pre>
 * EQUIVALENCE A(20), B
 * </pre>
 * the base variable would be A with a offset of 20*sizeof(double).
 * <p>
 * Note - an EquivalenceDecl specifies an implied equivalence for
 * COMMON variables.
 */
public final class EquivalenceDecl extends VariableDecl
{
  private VariableDecl base;   // VariableDecl of the base variable that this variable is equivalenced to.
  private long         offset; // Offset of this variable in bytes into the base variable.

  /**
   * @param name - name of the variable
   * @param type - type of the variable
   * @param base - VariableDecl representing the base variable
   * (COMMONM area) in the equivalence relationship
   * @param offset - offset of this variable in bytes into the base
   * variable
   */
  public EquivalenceDecl(String name, Type type, VariableDecl base, long offset)
  {
    super(name, type);
    setBaseVariable(base);
    setBaseOffset(offset);
    base.specifyCommonBaseVariable();
  }

  /**
   * Make a copy of this VariableDecl using a new name.
   */
  public Declaration copy(String name)
  {
    return new EquivalenceDecl(name, getType(), base, offset);
  }

  public void visit(Predicate p)
  {
    p.visitEquivalenceDecl(this);
  }

  /**
   * Return the base variable (i.e. COMMON area).
   */
  public VariableDecl getBaseVariable()
  {
    return base;
  }

  /**
   * Specify the base variable that this variable is equivalenced to.
   */
  public void setBaseVariable(VariableDecl base)
  {
    this.base = base;
    computeAttributes();
  }

  /**
   * Return the offset from the base (i.e. COMMON area).
   */
  public long getBaseOffset()
  {
    return offset;
  }
  
  /**
   * Specify the offset from the base variable that this variable is
   * equivalenced to.
   */
  public void setBaseOffset(long offset)
  {
    this.offset = offset;
  }

  /**
   * Generate an error - residency is specified only for the base
   * VariableDecl.
   * @param residency is the residency of the declaration
   * @see Residency
   */
  public void setResidency(Residency residency)
  {
    if (base == null)
      return;
    base.setResidency(residency);
  }

  /**
   * Return the declaration residency.  The residency of an
   * EquivalenceDecl is the residency of the base VariableDecl.
   * @see Residency
   */
  public Residency residency()
  {
    if (base == null)
      return Residency.AUTO;
    return base.residency();
  }

  /**
   * Return true if this declaration is to be allocated to memory.
   */
  public boolean inMemory()
  {
    return (base != null) && (base.residency() == Residency.MEMORY);
  }

  /**
   * Generate an error - visibility is specified only for the base
   * {@link VariableDecl variable}.
   * @param visibility is the visibility of the declaration
   * @see Visibility
   */
  public void setVisibility(int visibility)
  {
    throw new scale.common.InternalError("Can't set visibility of an EquivalenceDecl " + this);
  }

  /**
   * Return the declaration visibility.  The visibility of an
   * EquivalenceDecl is the visibility of the base {@link VariableDecl
   * variable}.
   * @see Visibility
   */
  public Visibility visibility()
  {
    if (base == null)
      return Visibility.LOCAL;
    return base.visibility();
  }

  /**
   * Return true if this declaration is globally visible.
   */
  public boolean isGlobal()
  {
    if (base == null)
      return false;
    return base.isGlobal();
  }

  /**
   * Return true if the declaration may be modified in an unknown way.
   */
  public boolean hasHiddenAliases()
  {
    return (super.hasHiddenAliases() ||
            ((base == null) ? false : base.hasHiddenAliases()));
  }

  /**
   * Return the location type used for this declaration.
   */
  public Assigned getStorageLoc()
  {
    if (!isStorageLocSet()) {
      VariableDecl base  = getBaseVariable();
      Assigned     btype = base.getStorageLoc();

      if (btype == Assigned.IN_MEMORY)
        btype = Assigned.IN_COMMON;

      setStorageLoc(btype);
    }
    return super.getStorageLoc();
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
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getValue();

    assert (i == 1) : "No such child " + i;

    return base;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }

  public final boolean isEquivalenceDecl()
  {
    return true;
  }

  public final EquivalenceDecl returnEquivalenceDecl()
  {
    return this;
  }
}
