package scale.score.expr;

import java.util.BitSet;

import scale.common.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.type.Type;
import scale.clef.expr.Literal;
import scale.score.*;
import scale.score.chords.Chord;

import scale.score.dependence.AffineExpr;


/**
 * This operator represents a phi operation in static single
 * assignment form.
 * <p>
 * $Id: PhiExpr.java,v 1.71 2007-10-17 13:45:45 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see scale.score.SSA
 */
public class PhiExpr extends NaryExpr
{
  /**
   * Utility markers for various algorithms.
   */
  private BitSet markers; 

  /**
   * Build a phi function instance.
   * <p>
   * All incoming values should be of the same type.  The result
   * type is the same as that of the incoming data.
   * @param type of the result and each argument
   * @param operands a vector of arguments
   * @see scale.score.SSA
   */
  public PhiExpr(Type type, Vector<Expr> operands)
  {
    super(type, operands);

    assert (operands != null) : "Must have some operands to the Phi function.";

    markers = new BitSet(operands.size());
  }

  public Expr copy()
  {
    int          n    = numOperands();
    Vector<Expr> args = new Vector<Expr>(n);
    for (int i = 0; i < n; i++)
      args.addElement(getOperand(i).copy());

    return new PhiExpr(getType(), args);
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

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | SE_STATE;
  }

  public void visit(Predicate p)
  {
    p.visitPhiExpr(this);
  }

  public String getDisplayLabel()
  {
    return "phi";
  }

  /** 
   * Allow some expressions such as VectorExpr to remove an operand.
   */
  public void removeOperand(int i)
  {
    super.removeOperand(i);
    int l = markers.size();
    for (int j = i + 1; j < l; j++) {
      if (markers.get(j))
        markers.set(j - 1);
      else
        markers.clear(j - 1);
    }
  }

  /**
   * Clear all the markers.  Each marker is associated with one
   * operand of the Phi function.
   */
  public void clearEdgeMarkers()
  {
    int s = markers.size();
    for (int i = 0; i < s; i++)
      markers.clear(i);
  }

  /**
   * Return the i-th marker.
   * @param i the index of the marker
   */
  public boolean edgeMarked(int i)
  {
    return markers.get(i);
  }

  /**
   * Set the i-th marker.
   * @param i the index of the marker
   */
  public void markEdge(int i)
  {
    markers.set(i);
  }

  /**
   * Clear the i-th marker.
   * @param i the index of the marker
   */
  public void clearEdge(int i)
  {
    markers.clear(i);
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    if (numOperands() > 1)
      return Lattice.Bot;
    return getOperand(0).getConstantValue();
  }

  /**
   * Determine the coefficent of a linear expression. 
   * @param indexVar the index variable associated with the
   * coefficient.
   * @return the coefficient value.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {
    int n = numOperands();
    for (int i = 0; i < n; i++) {
      Expr exp = getOperand(i);

      if (!(exp instanceof LoadDeclValueExpr))
        continue;

      LoadDeclValueExpr ldve = (LoadDeclValueExpr) exp;
      VariableDecl      vd   = (VariableDecl) ldve.getDecl();
      if (vd.getOriginal() == indexVar)
        return 1;
    }

    return 0;
  }

  protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>          affines,
                                               scale.score.chords.LoopHeaderChord thisLoop)
  {
    // It can't be affine if the induction variable depends on an if
    // statememt in the loop.

    if (!(getChord().firstInBasicBlock() instanceof scale.score.chords.LoopHeaderChord))
      return AffineExpr.notAffine;

    // Prevent infinite loops.  Use a bit of trickery based on our
    // knownledge of the getAffineExpr() method.  What we place in
    // the hash map will be replaced later.

    affines.put(this, AffineExpr.marker);

    AffineExpr ae    = getOperand(0).getAffineExpr(affines, thisLoop);
    int        start = 1;

    if (ae == null)
      return AffineExpr.notAffine;

    ae = ae.copy();

    // Only one operand can return an AffineExpr.marker and it's
    // normally the second operand to the phi function.

    if (ae == AffineExpr.marker) {
      ae = getOperand(1).getAffineExpr(affines, thisLoop).copy();
      start = 2;
    }

    int n = numOperands();
    for (int i = start; i < n; i++) {
      Expr       child = getOperand(i);
      AffineExpr ae2   = child.getAffineExpr(affines, thisLoop);
      if (ae2 == null)
        return AffineExpr.notAffine;

      if (ae2 == AffineExpr.marker)
        continue;

      ae.merge(ae2);
    }

    return ae;
  }

  private boolean invFlag = false;

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    if (!invFlag)
      try {
        invFlag = true;
        super.isLoopInvariant(loop);
      } finally {
        invFlag = false;
      }

    return false;
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

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    return containsDeclaration(decl); // Avoid stack overflow.
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 0;
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    Chord fibb = getChord().firstInBasicBlock();
    int   n    = fibb.numInCfgEdges();
    if (n != numOperands())
      throw new scale.common.InternalError("Phi function " +
                                           this +
                                           " does not conform - ops:" +
                                           numOperands() +
                                           " edges:" +
                                           n);
  }
}
