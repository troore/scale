package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.score.pred.References;

/**
 * This class is the superclass of all value expressions.  
 * <p>
 * $Id: ValueExpr.java,v 1.34 2007-10-17 13:40:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>

 * A value operator is an operator with no operands.  Value operators
 * may obtain their value in one of two ways:
 * <ol>
 * <li> The value may be intrinsic to the operator (e.g., <I>true</I>,
 *   <I>false</I>, and <I>this</I>).
 * <li> The value may originate from a non-graph edge (e.g., type or
 *   declaration nodes).
 * </ol>
 */
public abstract class ValueExpr extends Expr
{
  public ValueExpr(Type t)
  {
    super(t);
  }

  /**
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
  }

  /**
   * Replace all occurrances of a Declaration with another Declaration.
   * Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return false;
  }

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    return true;
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
  }

  /**
   * Return an array of the operands to the expression.
   */
  public final Expr[] getOperandArray()
  {
    return new Expr[0];
  }

  /**
   * Return the number of operands to this expression.
   */
  public final int numOperands()
  {
    return 0;
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return SE_NONE;
  }
}
