package scale.score.chords;

import scale.common.*;
import scale.score.Note;
import scale.score.expr.*;
import scale.score.Predicate;
import scale.score.pred.References;
import scale.clef.decl.Declaration;

/** 
 * This class is a parent class for routine terminating statements.
 * <p>
 * $Id: LeaveChord.java,v 1.43 2007-10-17 13:39:59 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 *Though not an inherent property of routine terminating
 * statements, the ones that exist (<code>return</code> and
 * <code>exit</code>) potentially carry a single data value.
 */

public abstract class LeaveChord extends EndChord
{
  /**
   * This field holds the node's incoming data edge.  
   * <p>
   * This field may have the null value, so all routines which
   * access this field must check its value first.
   */
  private Expr resultValue = null;
  
  /**
   * @param resultValue is the expression that defines the result of
   * the routine or null
   */
  public LeaveChord(Expr resultValue)
  {
    super();
    setResultValue(resultValue);
  }

  public LeaveChord()
  {
    this(null);
  }

  /**
   * Use this method when you may be modifying an in-coming data edge
   * to this expression while iterating over the in-coming edges.
   * @return an array of in-coming data edges.  
   */
  public final Expr[] getInDataEdgeArray()
  {
    if (resultValue == null)
      return new Expr[0];

    Expr[] array = new Expr[1];
    array[0] = resultValue;
    return array;
  }

  /**
   * Return the specified in-coming data edge.
   */
  public final scale.score.expr.Expr getInDataEdge(int i)
  {
    assert (i == 0) : "No incoming data edge (" + i + ") " + this;
    return resultValue;
  }

  /**
   * Return the number of in-coming data edges.
   */
  public final int numInDataEdges()
  {
    return (resultValue == null) ? 0 : 1;
  }

  /**
   * Push all incoming data edges on the stack.
   */
  public void pushInDataEdges(Stack<Expr> wl)
  {
    if (resultValue != null)
      wl.push(resultValue);
  }

  /**
   * Return the expression that defines the result of the routine.
   */
  public Expr getResultValue()
  {
    return resultValue;
  }

  /**
   * Set the expression that defines the result of the routine.
   */
  public void setResultValue(Expr resultValue)
  {
    assert (this.resultValue == null) :
      "Attempt to redefine function result expresion for " + this;
    this.resultValue = resultValue;
    if (resultValue != null)
      resultValue.setOutDataEdge(this);
  }

  /** 
   * This method changes an incoming data edge to point to a new
   * expression.
   * <p>
   * This method ensures that the node previously pointing to
   * this one is updated properly, as well as, the node which will now
   * point to this node.
   * <p>
   * Expr and Chord nodes have a fixed number of incoming
   * edges with specific meaning applied to each.  Hence, the edge
   * being replaced is indicated by position.
   * @param oldExpr is the expression to be replaced
   * @param newExpr is the new expression
   */
  public void changeInDataEdge(scale.score.expr.Expr oldExpr,
                               scale.score.expr.Expr newExpr)
  {
    assert (newExpr != null)        : "LeaveChord requires an incoming data edge.";
    assert (oldExpr == resultValue) : "Old incoming data edge not found." + oldExpr;

    if (resultValue != null) 
      resultValue.deleteOutDataEdge(this);
    resultValue = newExpr;
    resultValue.setOutDataEdge(this);
  }

  /**
   * Remove all the in-coming adat edges.
   */
  public void deleteInDataEdges()
  {
    if (resultValue == null)
      return;

    resultValue.unlinkExpression();
    resultValue = null;
  }

  /**
   * Return a string containing additional information about this
   * Chord.
   */
  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    if (resultValue != null)
      buf.append(resultValue);
    return buf.toString();
  }

  /**
   * Return a vector of all {@link scale.clef.decl.Declaration
   * declarations} referenced in this CFG node or <code>null</code>.
   */
  public Vector<Declaration> getDeclList()
  {
    if (resultValue == null)
      return null;

    Vector<Declaration> varList = new Vector<Declaration>();
    resultValue.getDeclList(varList);
    return varList;
  }

  /**
   * Return a vector of all {@link scale.score.expr.LoadExpr LoadExpr}
   * instances in this CFG node or <code>null</code>.
   */
  public Vector<LoadExpr> getLoadExprList()
  {
    if (resultValue == null)
      return null;

    Vector<LoadExpr> expList = new Vector<LoadExpr>();
    resultValue.getLoadExprList(expList);
    return expList;
  }

  /**
   * Return a vector of all {@link scale.score.expr.LoadExpr LoadExpr}
   * instances in this CFG node or <code>null</code>.
   */
  public Vector<Expr> getExprList()
  {
    if (resultValue == null)
      return null;

    Vector<Expr> expList = new Vector<Expr>();
    resultValue.getExprList(expList);
    return expList;
  }

  /**
   * Replace all occurrances of a {@link scale.clef.decl.Declaration
   * declaration} with another Declaration.  Return true if a replace
   * occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return resultValue.replaceDecl(oldDecl, newDecl);
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    if (resultValue != null)
      resultValue.removeUseDef();
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();
    if (resultValue == null)
      return;

    resultValue.validate();
    if (resultValue.getOutDataEdge() != this)
      throw new scale.common.InternalError(" expr " + this + " -> " + resultValue);
  }

  /**
   * Record any variable references in this CFG node in the table
   * of references.
   */
  public void recordRefs(References refs)
  {
    if (resultValue == null)
      return;

    resultValue.recordRefs(this, refs);
  }

  /**
   * Remove any variable references in this CFG node from the table
   * of references.
   */
  public void removeRefs(References refs)
  {
    if (resultValue == null)
      return;

    resultValue.removeRefs(this, refs);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    if (resultValue == null)
      return 0;
    return resultValue.executionCostEstimate();
  }

  /**
   * Remove all DualExpr instances from the CFG.  Use the lower form.
   * This eliminates references to variables that may no longer be
   * needed.
   */
  public boolean removeDualExprs()
  {
    boolean found = false;
    if (resultValue != null)
      found |= resultValue.removeDualExprs();
    return found;
  }
}
