package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;

/**
 * A ProcedureType represents the type of a procedure.
 * <p>
 * $Id: ProcedureType.java,v 1.65 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ProcedureType extends Type 
{
  private static Vector<ProcedureType> types; // A list of all the unique procedure types.

  private Type               returnType; // The return type of the procedure.
  private Vector<FormalDecl> formals;    // A vector of FormalDecls.
  private Vector<Raise>      raises;     // A vector of the exceptions raised.
  /**
   * True if this is a K&R procedure.
   * A K&R procedure is defined without an ANSI C prototype.
   */
  private boolean oldStyle;
  /**
   * True if this is a Fortran FUNCTION that returns a CHRACTER value
   * via an address passed in as in argument..
   */
  private boolean fchar;

  /**
   * Re-use an existing instance of a particular procedure type.
   * If no equivalent procedure type exists, create a new one.
   * @param returnType the return type of the routine.
   * @param formals a vector of FormalDecls.  If the vector is empty,
   * the routine is of the form r(void).
   * @param raises the exceptions raised by the routine or null if none.
   */
  public static ProcedureType create(Type returnType, Vector<FormalDecl> formals, Vector<Raise> raises)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        ProcedureType ta = types.elementAt(i);
        if ((ta.returnType == returnType) &&
            ta.sameFormals(formals) &&
            ta.sameRaises(raises)) {
          return ta;
        }
      }
    }
    ProcedureType a = new ProcedureType(returnType, formals, raises);
    if (types == null)
      types = new Vector<ProcedureType>(2);
    types.addElement(a);
    return a;
  }

  protected ProcedureType(Type returnType, Vector<FormalDecl> formals, Vector<Raise> raises)
  {
    this.returnType = returnType;
    this.formals    = formals;
    this.raises     = raises;
    this.oldStyle   = false;
    this.fchar      = false;
  }

  /**
   * Create a procedure type with the same parameters but a different
   * return type.
   */
  public ProcedureType copy(Type returnType)
  {
    return ProcedureType.create(returnType, formals, raises);
  }

  /**
   * Return true if the formals match.
   */
  public boolean sameFormals(Vector<FormalDecl> formals)
  {
    int l1 = formals.size();
    int l2 = this.formals.size();
    if (l1 != l2)
      return false;

    for (int i = 0; i < l1; i++) {
      if (formals.elementAt(i) != this.formals.elementAt(i))
        return false;
    }
    return true;
  }

  /**
   * Return true if the formals match.
   */
  public boolean sameRaises(Vector<Raise> raises)
  {
    if (raises == null)
      return (this.raises == null);
    else if (this.raises == null)
      return false;

    int l1 = raises.size();
    int l2 = this.raises.size();
    if (l1 != l2)
      return false;

    for (int i = 0; i < l1; i++) {
      if (raises.elementAt(i) != this.raises.elementAt(i))
        return false;
    }
    return true;
  }

  /**
   * Return true if this type represents a procedure.
   */
  public boolean isProcedureType()
  {
    return true;
  }

  public final ProcedureType returnProcedureType()
  {
    return this;
  }

  /**
   * Return true if this is an old style C procedure type defined with
   * out a prototype.
   */
  public final boolean isOldStyle()
  {
    return oldStyle;
  }

  /**
   * Mark this as an old style K&R C procedure type.
   */
  public final void markAsOldStyle()
  {
    oldStyle = true;
  }

  /**
   * Return true if this is a Fortran FUNCTION that returns a
   * CHARACTER value via an address passed in as in argument.
   */
  public final boolean isFChar()
  {
    return fchar;
  }

  /**
   * Mark this as a Fortran FUNCTION that returns a CHARACTER value
   * via an address passed in as in argument.
   */
  public final void markAsFChar()
  {
    fchar = true;
  }

  /**
   * Return the return type of the procedure.
   */
  public final Type getReturnType()
  {
    return returnType;
  }

  /**
   * Return the specified formal parameter.
   */
  public final FormalDecl getFormal(int i)
  {
    return formals.elementAt(i); 
  }

  /**
   * Return the specified formal parameter.
   */
  public final FormalDecl getFormal(String name)
  {
    int l = formals.size();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = formals.elementAt(i);
      if (fd.getName().equals(name))
        return fd;
    }
    return null;
  }

  /**
   * Return the number of formal parameters.
   */
  public int numFormals()
  {
    return formals.size();
  }

  /**
   * Return a vector of exceptions raised.
   */
  public final Vector<Raise> getRaiseVector()
  {
    if (raises == null)
      return new Vector<Raise>(0);
    return raises.clone(); 
  }

  /**
   * Return the number of exceptions raised.
   */
  public int numRaises()
  {
    if (raises == null)
      return 0;
    return raises.size();
  }

  /**
   * Return the specified exception.
   */
  public final Raise getRaise(int i)
  {
    return raises.elementAt(i); 
  }

  public void visit(Predicate p)
  {
    p.visitProcedureType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitProcedureType(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return returnType;

    i--;
    int l = formals.size();
    if (i < l)
      return (Node) formals.elementAt(i);
    return (Node) raises.elementAt(i - l);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    int n = 1;
    if (formals != null)
      n += formals.size();
    if (raises != null)
      n += raises.size();
    return n;
  }

  public String toStringSpecial()
  {
    return oldStyle ? "K&R" : "";
  }

  /**
   * Return true if the specified procedure type matches this
   * procedure type.
   * <p>
   * The criterion for considering two signatures equivalent can be
   * modified by the <code>boolean</code> arguments to the method.
   * <p>
   * If <code>matchUnknowns</code> is <code>false</code> then two
   * parameter lists match if:
   * <ul>
   * <li>both have {@link scale.clef.decl.UnknownFormals
   * UnknownFormals} instances, or
   * <li>the shorter parameter list has an {@link
   * scale.clef.decl.UnknownFormals UnknownFormals} instance,
   * <li>neither parameter list has an {@link
   * scale.clef.decl.UnknownFormals UnknownFormals} instance.
   * </ul>
   * Any {@link scale.clef.decl.UnknownFormals UnknownFormals}
   * instance must always be the last parameter.
   * @param s2 is the other procedure type
   * @param matchUnknowns is true if the signatures don't match when
   * the position of any {@link scale.clef.decl.UnknownFormals
   * UnknownFormals} instances differ
   * @param matchFormalNames is true is the names of the parameters
   * should be compared
   * @param matchFormalModes is true if the parameter modes should be
   * compared
   * @param matchExceptions is true if the exceptions should be
   * compared
   * @param exactExceptionMatch is true if the exceptions must match
   * excatly
   */
  public boolean compareSignatures(ProcedureType s2,
                                   boolean matchUnknowns,
                                   boolean matchFormalNames,
                                   boolean matchFormalModes,
                                   boolean matchExceptions,
                                   boolean exactExceptionMatch)
  {
    if (returnType == null)
      return (s2.returnType == null);

    if (s2.returnType == null)
      return false;

    if (!returnType.equivalent(s2.returnType))
      return false;

    int l1 = formals.size();
    int l2 = s2.formals.size();

    // Compare the size of the argument lists.

    // Record if the parameter lists are not the same size.  Normally,
    // unequal length parameter lists imply that procedures don't
    // match.  However, if one of the procedures has a variable length
    // parameter lists, then they may still match.

    if (l1 != l2) {
      if (matchUnknowns)
        return false;

      boolean isUnknown1 = (l1 > 0) && (formals.elementAt(l1 - 1) instanceof UnknownFormals);
      boolean isUnknown2 = (l2 > 0) && (s2.formals.elementAt(l2 - 1) instanceof UnknownFormals);

      if (isUnknown1) {
        if (!isUnknown2 && (l2 < (l1 - 1)))
          return false;
      } else if (isUnknown2) {
        if (l1 < (l2 - 1))
          return false;
      } else
        return false;
    }

    int i1 = 0;
    int i2 = 0;
    while ((i1 < l1) && (i2 < l2)) {
      FormalDecl fd1 = formals.elementAt(i1);
      FormalDecl fd2 = s2.formals.elementAt(i2);
      i1++;
      i2++;

      if (fd1 == fd2) // Check for trivial equivalence.
        continue;

      // Handle Unknown Formal parameters (ie, variable length argument lists).

      boolean isUnknown1 = fd1 instanceof UnknownFormals;
      boolean isUnknown2 = fd2 instanceof UnknownFormals;
      if ((isUnknown1 && (i1 < (l1 - 1))) ||
          (isUnknown2 && (i2 < (l2 - 1))))
        throw new scale.common.InternalError("Invalid UnknownFormals");

      if (isUnknown1 || isUnknown2)
        break;

      // Compare the parameter names.

      if (matchFormalNames && !fd1.getName().equals(fd2.getName()))
        return false;

      // Compare the parameter modes.

      if (matchFormalModes && (fd1.getMode() != fd2.getMode()))
        return false;

      // Compare the parameter types.

      if (!fd1.getCoreType().equivalent(fd2.getCoreType()))
        return false;
    }

    if (!matchExceptions)
      return true;

    // Test the potentially raised exceptions.

    boolean foundRaisesAny = false;
    int     l1r            = numRaises();
    int     l2r            = s2.numRaises();

    // This first block of code checks to see if we have a RaisesAny
    // in at least one of the lists.  We only need to do this if we
    // are doing an inexact match.

    if (!exactExceptionMatch) {

      // A raise list with a RaisesAny should have no other raises
      // in the list.  Hence, it should be a list of length one.
      if (l1r == 1) { // Is this a RaisesAny?
        Raise r = raises.elementAt(0);
        if ((r instanceof RaiseWithObject) || (r instanceof RaiseWithType))
          foundRaisesAny = true;
      }
      if (l2r == 1) { // Is this a RaisesAny?
        Raise r = s2.raises.elementAt(0);
        if ((r instanceof RaiseWithObject) || (r instanceof RaiseWithType))
          foundRaisesAny = true;
      }
    }

    // If we are doing an inexact match, and at least on of the
    // raise lists contains a RaisesAny we do not have to check any
    // further.

    if (!exactExceptionMatch && foundRaisesAny)
      return true;

        
    // At this point, we must do an *exact* match because either...
    //    1. An exact match is called for (note we may have one or
    //       more RaisesAny).
    //    2. We are doing an inexact match, but have no RaisesAny.

    // In an exact match, raise lists match only if the lists
    // contain exactly the same exceptions (though in an arbitrary
    // order).   

    // Hence, raise lists of unequal length cannot match.
    // Note that for simplicity, I am assuming that there are no
    // redundant exceptions.  Otherwise, the lists could exactly
    // match and be of different lengths, but the comparison would
    // be more difficult.

    if (l1r != l2r)
      return false;

    // Here we compare the raise lists, while ignoring order.  We
    // assume that there are no duplicates.  This is a simplistic
    // O(n^2) comparison.

    for (int i1r = 0; i1r < l1r; i1r++) {
      boolean found = false;
      Raise r1 = raises.elementAt(i1r);

      for (int i2r = 0; (i2r < l2r) && !found; i2r++) {
        Raise r2 = s2.raises.elementAt(i2r);

        // If they are equal, we found a match!
        if (r1 == r2)
          found = true;
      }

      // If no match was found in e2 for this element of e1, then
      // the two raise lists do not match.

      if (!found)
        return false;
    }

    return true;
  }

  /**
   * Compares two ProcedureTypes for equivalence as defined by the
   * type equivalence rules specified in <TT>ted</TT>.
   * @return true if the types are equivalent
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    if (tc == null)
      return false;

    if (tc.getClass() != getClass())
      return false;

    ProcedureType t2 = (ProcedureType) tc;
    return compareSignatures(t2, true, false, true, true, true);
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<ProcedureType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<ProcedureType>();
    return types.elements();
  }

  /**
   * Calculate how many addressable memory units are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of addressable memory units required to
   * represent this type
   */
  public long memorySize(Machine machine)
  {
    throw new scale.common.InternalError("No memory size for this type " + this);
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    return 1;
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
    return 2;
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
