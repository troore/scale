package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.score.analyses.AliasAnnote;
import scale.clef.decl.Declaration;
import scale.score.pred.References;

/**
 * This class is the superclass of all unary operators.
 * <p>
 * $Id: UnaryExpr.java,v 1.48 2007-10-17 13:40:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class UnaryExpr extends Expr
{
  /**
   * The argument of the operator..
   */
  private Expr arg; 

  public UnaryExpr(Type t, Expr arg)
  {
    super(t);
    this.arg = arg;
    arg.setOutDataEdge(this); // Add other side of data edges.
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the equivalent() method of the derived
   * classes.
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    UnaryExpr o = (UnaryExpr) exp;
    return arg.equivalent(o.arg);
  }

  /**
   * Set the nth operand of an expression.  This method eliminates the
   * data edge between the previous operand expression and this
   * expression.  The new <code>operand</code> must not be
   * <code>null</code>.
   * @param operand - the new operand
   * @param position - indicates which operand
   * @return the previous operand
   */
  protected Expr setOperand(Expr operand, int position)
  {
    Expr old = null;

    assert (position == 0) : "Invalid operand position - " + position;

    old = arg;
    if (old != null)
      old.deleteOutDataEdge(this);

    arg = operand;
    arg.setOutDataEdge(this); // Add other side of data edges.

    return old;
  }

  /**
   * Replace the operand.  This method eliminates the data edge
   * between the previous argument expression and this expression.
   * The <code>arg</code> argument may be <code>null</code>.
   * @param arg is the new argument
   */
  public final void setArg(Expr arg)
  {
    Expr old = this.arg;
    if (old != null)
      old.deleteOutDataEdge(this);

    if (arg != null)
      arg.setOutDataEdge(this); // Add other side of data edges.
    this.arg = arg;
  }

  /**
   * Return the nth operand.
   * @param position the index of the operand
   */
  public final Expr getOperand(int position)
  {
    assert (position == 0) : "Invalid operand position - " + position;
    return arg;
  }

  /**
   * Return an array of the operands to the expression.
   */
  public final Expr[] getOperandArray()
  {
    Expr[] array = new Expr[1];
    array[0] = arg;
    return array;
  }

  /**
   * Return the number of operands to this expression.
   */
  public int numOperands()
  {
    return 1;
  }

  /**
   * Return the operator's argument.
   */
  public final Expr getArg()
  {
    return arg;
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();
    arg.loopClean();
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    super.unlinkExpression();
    if (arg != null)
      arg.unlinkExpression();
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return arg.containsDeclaration(decl);
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    return arg.dependsOnDeclaration(decl);
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return arg.optimizationCandidate();
  }

  /**
   * Get the alias annotation associated with a Scribble operator.
   * Most Scribble operators do not have alias variables so this
   * routine may return null.  Typically, the alias variable
   * information is attached to the declaration node associated with
   * the load operations.  However, we sometimes need to create alias
   * variables to hold alias information that is not directly assigned
   * to a user variable (e.g., <tt>**x</tt>).
   * @return the alias annotation associated with the expression
   */
  public AliasAnnote getAliasAnnote()
  {
    AliasAnnote aa = super.getAliasAnnote();
    if (aa != null)
      return aa;
    return arg.getAliasAnnote();
  }

  /**
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    arg.getDeclList(varList);
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    arg.getLoadExprList(expList);
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
    arg.getExprList(expList);
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
    wl.push(arg);
  }

  /**
   * Replace all occurrances of a Declaration with another
   * Declaration.  Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return arg.replaceDecl(oldDecl, newDecl);
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    arg.removeUseDef();
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    if (arg.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + arg);

    arg.validate();
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    arg.recordRefs(stmt, refs);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    arg.removeRefs(stmt, refs);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 1 + arg.executionCostEstimate();
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int se = arg.sideEffects();
    if (getCoreType().isRealType())
      se |= SE_DOMAIN; // If an arg is Nan a domain error will occur.
    return se;
  }
}
