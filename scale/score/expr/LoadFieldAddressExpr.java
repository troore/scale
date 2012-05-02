package scale.score.expr;

import scale.common.*;
import scale.clef.decl.FieldDecl;
import scale.clef.type.*;

import scale.score.Predicate;
/**
 * This class represents the address of a field of a structure. 
 * <p>
 * $Id: LoadFieldAddressExpr.java,v 1.29 2007-10-04 19:58:31 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class LoadFieldAddressExpr extends FieldExpr
{
  /**
   * This method builds an expression node with this operation as the operator.
   * @param structure is the address of the structure accessed
   * @param field specifies the field of the structure
   */
  public LoadFieldAddressExpr(Expr structure, FieldDecl field)
  {
    super(PointerType.create(field.getType()), structure, field);
    ArrayType at = field.getCoreType().returnArrayType();
    if (at != null)
      setType(PointerType.create(at.getArraySubtype()));
  }

  /**
   * Make a copy of this load expression.
   * The use - def information is copied too.
   */
  public Expr copy()
  {
    LoadFieldAddressExpr ld = new LoadFieldAddressExpr(getStructure().copy(), getField());
    return ld;
  }

  /**
   * Make a copy of this load expression without the use - def information.
   */
  public Expr copyNoUD()
  {
    LoadFieldAddressExpr ld = new LoadFieldAddressExpr(getStructure(), getField());
    return ld;
  }

  /**
   * Return the Chord with the highest label value from the set of
   * Chords that must be executed before this expression.
   * @param lMap is used to memoize the expression to critical Chord
   * information
   * @param independent is returned if the expression is not dependent
   * on anything
   */
  protected scale.score.chords.Chord findCriticalChord(HashMap<Expr, scale.score.chords.Chord> lMap,
                                                       scale.score.chords.Chord                independent)
  {
    scale.score.chords.Chord cc        = super.findCriticalChord(lMap, independent);
    int                      cmax      = cc.getLabel();
    Expr                     structure = getStructure();

    if (structure != null) {
      scale.score.chords.Chord nc   = structure.getCriticalChord(lMap, independent);
      int                      nmax = nc.getLabel();
      if (nmax > cmax) {
        cc = nc;
      }
    }

    return cc;
  }

  public void visit(Predicate p)
  {
    p.visitLoadFieldAddressExpr(this);
  }

  public String getDisplayLabel()
  {
    FieldDecl fd = getField();
    if (fd == null)
      return "&->???";
    return "&->" + fd.getName();
  }

  /**
   * Return true if this expression is valid on the left side of an
   * assignment.
   */
  public boolean validLValue()
  {
    return true;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return getStructure().optimizationCandidate();
  }
}
