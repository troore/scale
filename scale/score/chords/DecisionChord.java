package scale.score.chords;

import scale.common.*;
import scale.score.expr.*;
import scale.score.Predicate;
import scale.score.pred.References;
import scale.clef.decl.Declaration;

/** 
 * This class represents a CFG node that has multiple out-going CFG
 * edges.
 * <p>
 * $Id: DecisionChord.java,v 1.62 2007-10-17 13:44:05 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This is the base class for all classes that have more than one
 * out-going CFG edge.
 */

public abstract class DecisionChord extends Chord
{
  private Expr predicate = null; // Data edge for test expression.

  /**
   * Create a Chord that has more than one out-going CFG edge where
   * the edge is selected by some computation.
   * @param predicate the expression which is evaluated to select the
   * out-going edge
   */
  public DecisionChord(Expr predicate)
  {
    super();

    this.predicate = predicate;
    if (predicate != null)
      predicate.setOutDataEdge(this);
  }

  /**
   * Use this method when you may be modifying an in-coming data edge
   * to this expression while iterating over the in-coming edges.
   * @return an array of in-coming data edges.  
   */
  public final Expr[] getInDataEdgeArray()
  {
    if (predicate == null)
      return new Expr[0];

    Expr[] array = new Expr[1];
    array[0] = predicate;
    return array;
  }

  /**
   * Return the specified in-coming data edge.
   */
  public scale.score.expr.Expr getInDataEdge(int i)
  {
    assert (i == 0) : "No incoming data edge (" + i + ") " + this;
    return predicate;
  }

  /**
   * Return the number of in-coming data edges.
   */
  public int numInDataEdges()
  {
    return (predicate == null) ? 0 : 1;
  }

  /**
   * Push all incoming data edges on the stack.
   */
  public void pushInDataEdges(Stack<Expr> wl)
  {
    if (predicate != null)
      wl.push(predicate);
  }

  /**
   * Return a String containing additional information about this CFG node..
   */
  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    if (predicate != null)
      buf.append(predicate);
    return buf.toString();
  }

  /**
   * Return the expression used to select an out-going CFG edge.
   */
  public final Expr getPredicateExpr()
  {
    return predicate;
  }

  /**
   * Specify the probability that the specified edge will be executed
   * next.  The stored value for the probability is quantized for
   * reasons of space utilization.  The probability of both edges is
   * set.  The sum of the probabilities of both edges is always
   * (approximately) 1.0.
   */
  public abstract void specifyBranchProbability(Chord edge, double probability);

  /**
   * Return the probability that the specified edge will be executed
   * next.  The stored value for the probability is quantized for
   * reasons of space utilization.  The sum of the probabilities of
   * both edges is always (approximately) 1.0.
   */
  public abstract double getBranchProbability(Chord edge);

  /**
  * Return null because there is no ONE next Chord in the CFG.
  */
  public final Chord getNextChord()
  {
    return null;
  }

  /**
   * Return true if this is the last Chord in this Basic Block.
   */
  public final boolean isLastInBasicBlock()
  {
    return true;
  }

  /**
   * Return true if this chord may have multiple out-going CFG edges.
   */
  public final boolean isBranch()
  {
    return true;
  }

  /**
   * Return the index of the selected out-going CFG edge.
   * @param key specifies the out-going CFG edge
   */
  public abstract int getBranchEdgeIndex(Object key);

  /** 
   * This method changes an incoming data edge to point to a new expression.
   * <p>
   * This method ensures that the node previously pointing to
   * this one is updated properly, as well as, the node which will now
   * point to this node.
   * <p>
   * Expr and Chord nodes have a fixed number of incoming
   * edges with specific meaning applied to each.
   * @param oldExpr is the expression to be replaced
   * @param newExpr is the new expression
   */
  public void changeInDataEdge(scale.score.expr.Expr oldExpr,
                               scale.score.expr.Expr newExpr)
  {
    assert (newExpr != null) : "DecisionChord requires an incoming data edge.";

    if (oldExpr == predicate) {
      if (predicate != null) 
        predicate.deleteOutDataEdge(this);
      predicate = newExpr;
      predicate.setOutDataEdge(this);
    } else
      throw new scale.common.InternalError("Old incoming data edge not found." + oldExpr);
  }

  /**
   * Remove all the in-coming data edges.
   */
  public void deleteInDataEdges()
  {
    if (predicate == null)
      return;

    predicate.deleteOutDataEdge(this);
    predicate.unlinkExpression();
    predicate = null;
  }

  /**
   * Return a string specifying the color to use for coloring this
   * node in a graphical display.  This method should be over-ridden
   * as it simplay returns the color red.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.YELLOW;
  }

  /**
   * Return a string specifying a shape to use when drawing this node
   * in a graphical display.  This method should be over-ridden as it
   * simplay returns the shape "box".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.RHOMBUS;
  }

  /**
   * Return a vector of all declarations referenced in this CFG node
   * or <code>null</code>.
   */
  public Vector<Declaration> getDeclList()
  {
    if (predicate == null)
      return null;

    Vector<Declaration> varList = new Vector<Declaration>(20);
    predicate.getDeclList(varList);
    return varList;
  }

  /**
   * Return a vector of all {@link scale.score.expr.LoadExpr LoadExpr}
   * instances in this CFG node or <code>null</code>.
   */
  public Vector<LoadExpr> getLoadExprList()
  {
    if (predicate == null)
      return null;

    Vector<LoadExpr> expList = new Vector<LoadExpr>(20);
    predicate.getLoadExprList(expList);
    return expList;
  }

  /**
   * Return a vector of all {@link scale.score.expr.LoadExpr LoadExpr}
   * instances in this CFG node or <code>null</code>.
   */
  public Vector<Expr> getExprList()
  {
    if (predicate == null)
      return null;

    Vector<Expr> expList = new Vector<Expr>(20);
    predicate.getExprList(expList);
    return expList;
  }

  /**
   * Replace all occurrances of a declaration with another
   * declaration.  Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return predicate.replaceDecl(oldDecl, newDecl);
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    predicate.removeUseDef();
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();
    predicate.validate();
    if (predicate.getOutDataEdge() != this)
      throw new scale.common.InternalError(" expr " + this + " -> " + predicate);
  }

  /**
   * Record any variable references in this CFG node in the table of
   * references.
   */
  public void recordRefs(References refs)
  {
    if (predicate != null)
      predicate.recordRefs(this, refs);
  }

  /**
   *  Record any variable references in this CFG node from the table of
   * references.
   */
  public void removeRefs(References refs)
  {
    if (predicate != null)
      predicate.removeRefs(this, refs);
  }

  /**
   * Remove all {@link scale.score.expr.DualExpr DualExpr} instances
   * from the CFG.  Use the lower form.  This eliminates references to
   * variables that may no longer be needed.
   */
  public boolean removeDualExprs()
  {
    boolean found = false;
    if (predicate != null)
      found |= predicate.removeDualExprs();
    return found;
  }
}
