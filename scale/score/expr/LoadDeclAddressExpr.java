package scale.score.expr;

import scale.common.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.type.*;

import scale.score.*;
import scale.score.analyses.*;
import scale.score.dependence.AffineExpr;
import scale.score.pred.References;

/**
 * This class represents the address of a declaration. 
 * <p>
 * $Id: LoadDeclAddressExpr.java,v 1.74 2007-10-04 19:58:31 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The type of a LoadDeclAddressExpr is always of type
 * pointer-to-declaration-type.
 */
public final class LoadDeclAddressExpr extends LoadExpr
{
  /**
   * This method builds an expression node with this operation as the
   * operator.
   */
  public LoadDeclAddressExpr(Declaration decl)
  {
    super(PointerType.create(decl.getType()), decl);
    ArrayType at = decl.getCoreType().returnFixedArrayType();
    if (at != null)
      setType(PointerType.create(at.getArraySubtype()));
  }

  /**
   * Make a copy of this load expression.
   * The use - def information is copied too.
   */
  public Expr copy()
  {
    LoadDeclAddressExpr ld     = new LoadDeclAddressExpr(getDecl());
    MayUse              mayUse = getMayUse();
    if (mayUse != null)
      mayUse.copy(ld);
    ld.setUseDef(getUseDef());
    return ld;
  }

  /**
   * Make a copy of this load expression without the use - def information.
   */
  public Expr copyNoUD()
  {
    LoadDeclAddressExpr ld = new LoadDeclAddressExpr(getDecl());
    return ld;
  }

  public void visit(Predicate p)
  {
    p.visitLoadDeclAddressExpr(this);
  }

  public String getDisplayLabel()
  {
    return "&" + getName();
  }

  /**
   * Return linearity of an address.
   */
  public int linearity()
  {
    return 0;
  }

  /**
   * Return the coefficient value.
   */
  public int findCoefficient()
  {
    return 0;
  }

  /**
   * Return the label of the Chord with the highest label value from
   * the set of Chords that must be executed before this expression.
   * @param lMap is used to memoize the expression to critical Chord
   * information
   * @param independent is returned if the expression is not dependent
   * on anything
   */
  protected scale.score.chords.Chord findCriticalChord(HashMap<Expr, scale.score.chords.Chord> lMap,
                                                       scale.score.chords.Chord                independent)
  {
    return independent;
  }

  /**
   * Return true if the expression can be moved without problems.  If
   * the address is of a variable in memory whose address is not used
   * as an argument, then the LoadDeclAddressEXpr instance can be
   * moved.
   */
  public boolean optimizationCandidate()
  {
    Declaration vd = getDecl();

    if (!vd.inMemory())
      return false;

    if (!vd.isVariableDecl())
      return false;

    if (vd.addressTaken())
      return false;

    return true;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 1;
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    Declaration decl = getDecl();

    // This records a reference to a variable.  The method
    // <tt>getObject</tt> returns the variable reference.  We use this
    // to return the appropriate object for an array or structure
    // reference.

    refs.recordUse(stmt, getObject(), decl);

    Note s = getOutDataEdge();
    while ((s instanceof Expr) &&  ((Expr) s).isCast())
      s = ((Expr) s).getOutDataEdge();

    if (s instanceof CallExpr)
      refs.recordDef(stmt, this, decl);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    Declaration decl = getDecl();

    // This removes a reference to a variable.  The method
    // <tt>getObject</tt> returns the variable reference.  We use this
    // to return the appropriate object for an array or structure
    // reference.

    refs.remove(stmt, decl);
  }

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    return true;
  }
}
