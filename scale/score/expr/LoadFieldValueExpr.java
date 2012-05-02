package scale.score.expr;

import scale.common.*;
import scale.clef.decl.FieldDecl;
import scale.clef.type.*;

import scale.score.Predicate;

/**
 * This class represents the value of a field of a structure.
 * <p>
 * $Id: LoadFieldValueExpr.java,v 1.21 2007-10-04 19:58:31 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class LoadFieldValueExpr extends FieldExpr
{
  /**
   * This method builds an expression node with this operation as the
   * operator.
   * @param structure is the address of the structure accessed
   * @param field specifies the field of the structure
   */
  public LoadFieldValueExpr(Expr structure, FieldDecl field)
  {
    super(field.getType(), structure, field);
  }

  /**
   * Make a copy of this load expression.
   * The use - def information is copied too.
   */
  public Expr copy()
  {
    LoadFieldValueExpr ld = new LoadFieldValueExpr(getStructure().copy(), getField());
    return ld;
  }

  /**
   * Make a copy of this load expression without the use - def information.
   */
  public Expr copyNoUD()
  {
    LoadFieldValueExpr ld = new LoadFieldValueExpr(getStructure().copy(), getField());
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
    return getChord();
  }

  public void visit(Predicate p)
  {
    p.visitLoadFieldValueExpr(this);
  }

  /**
   * Return true if this expression is valid on the left side of an
   * assignment.
   */
  public boolean validLValue()
  {
    return false;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5 + getStructure().executionCostEstimate();
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int se = super.sideEffects();
    if (getStructure() instanceof LoadDeclAddressExpr)
      return se;
    return se | SE_DOMAIN;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return false;
  }
}
