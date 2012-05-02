package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.score.pred.References;

/**
 * This is the base class for operations with three arguments.
 * <p>
 * $Id: TernaryExpr.java,v 1.49 2007-10-17 13:40:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class TernaryExpr extends Expr
{
  /**
   * Left argument.
   */
  private Expr la; 
  /**
   * Middle argument.
   */
  private Expr ma; 
  /**
   * Right argument.
   */
  private Expr ra; 

  public TernaryExpr(Type t, Expr la, Expr ma, Expr ra)
  {
    super(t);
    this.la = la;
    la.setOutDataEdge(this); // Add other side of data edges.
    this.ma = ma;
    ma.setOutDataEdge(this); // Add other side of data edges.
    this.ra = ra;
    ra.setOutDataEdge(this); // Add other side of data edges.
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
    TernaryExpr o = (TernaryExpr) exp;
    return (la.equivalent(o.la) && ma.equivalent(o.ma) && ra.equivalent(o.ra));
  }

  /**
   * Return the left argument.
   */
  public final Expr getLA()
  {
    return la;
  }

  /**
   * Return the middle argument.
   */
  public final Expr getMA()
  {
    return ma;
  }

  /**
   * Return the right argument.
   */
  public final Expr getRA()
  {
    return ra;
  }

  /**
   * Replace the left operand.  This method eliminates the data edge
   * between the previous left argument expression and this
   * expression.  The <code>la</code> argument may be <code>null</code>.
   * @param la is the new left argument
   */
  public final void setLA(Expr la)
  {
    Expr old = this.la;
    if (old != null)
      old.deleteOutDataEdge(this);

    if (la != null)
      la.setOutDataEdge(this); // Add other side of data edges.
    this.la = la;
  }

  /**
   * Replace the middle operand.  This method eliminates the data edge
   * between the previous middle argument expression and this
   * expression.  The <code>ma</code> argument may be <code>null</code>.
   * @param ma is the new middle argument
   */
  public final void setMA(Expr ma)
  {
    Expr old = this.ma;
    if (old != null)
      old.deleteOutDataEdge(this);

    if (ma != null)
      ma.setOutDataEdge(this); // Add other side of data edges.
    this.ma = ma;
  }

  /**
   * Replace the right operand.  This method eliminates the data edge
   * between the previous right argument expression and this
   * expression.  The <code>ra</code> argument may be <code>null</code>.
   * @param ra is the new right argument
   */
  public final void setRA(Expr ra)
  {
    Expr old = this.ra;
    if (old != null)
      old.deleteOutDataEdge(this);

    if (ra != null)
      ra.setOutDataEdge(this); // Add other side of data edges.
    this.ra = ra;
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
    assert (operand != null) : "An expression operand can not be null.";

    Expr old = null;

    if (position == 0) {
      old = la;
      la = operand;
    } else if (position == 1) {
      old = ma;
      ma = operand;
    } else if (position == 2) {
      old = ra;
      ra = operand;
    } else
      throw new scale.common.InternalError("Invalid operand position - " + position);

    if (old != null)
      old.deleteOutDataEdge(this);

    operand.setOutDataEdge(this); // Add other side of data edges.
    return old;
  }

  /**
   * Return the nth operand.
   * @param position the index of the operand
   */
  public final Expr getOperand(int position)
  {
    if (position == 0)
      return la;
    else if (position == 1)
      return ma;
    else if (position == 2)
      return ra;
    else
      throw new scale.common.InternalError("Invalid operand position - " + position);
  }

  /**
   * Return an array of the operands to the expression.
   */
  public final Expr[] getOperandArray()
  {
    Expr[] array = new Expr[3];
    array[0] = la;
    array[1] = ma;
    array[2] = ra;
    return array;
  }

  /**
   * Return the number of operands to this expression.
   */
  public int numOperands()
  {
    return 3;
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();
    la.loopClean();
    ma.loopClean();
    ra.loopClean();
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    if (la != null)
      la.unlinkExpression();
    if (ma != null)
      ma.unlinkExpression();
    if (ra != null)
      ra.unlinkExpression();
  }

  /**
   * Return true if this expression contains a reference to the variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return (la.containsDeclaration(decl) ||
            ra.containsDeclaration(decl) ||
            ma.containsDeclaration(decl));
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    return (la.dependsOnDeclaration(decl) ||
            ra.dependsOnDeclaration(decl) ||
            ma.dependsOnDeclaration(decl));
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic variables, etc
   * are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return (la.optimizationCandidate() &&
            ra.optimizationCandidate() &&
            ma.optimizationCandidate());
  }

  /**
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    la.getDeclList(varList);
    ma.getDeclList(varList);
    ra.getDeclList(varList);
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    la.getLoadExprList(expList);
    ma.getLoadExprList(expList);
    ra.getLoadExprList(expList);
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
    la.getExprList(expList);
    ma.getExprList(expList);
    ra.getExprList(expList);
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
    wl.push(la);
    wl.push(ma);
    wl.push(ra);
  }

  /**
   * Replace all occurrances of a Declaration with another Declaration.
   * Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return (ra.replaceDecl(oldDecl, newDecl) |
            la.replaceDecl(oldDecl, newDecl) |
            ma.replaceDecl(oldDecl, newDecl));
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    ra.removeUseDef();
    ma.removeUseDef();
    la.removeUseDef();
  }

  /**
   * Check this node for validity.
   * This method throws an exception if the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    if (la.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + la);
    if (ma.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + ma);
    if (ra.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + ra);

    la.validate();
    ma.validate();
    ra.validate();
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    la.recordRefs(stmt, refs);
    ma.recordRefs(stmt, refs);
    ra.recordRefs(stmt, refs);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    la.removeRefs(stmt, refs);
    ma.removeRefs(stmt, refs);
    ra.removeRefs(stmt, refs);
  }

  /**
   * Return an indication of the side effects execution of this expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int se = la.sideEffects() | ma.sideEffects() | ra.sideEffects();
    if (getCoreType().isRealType())
      se |= SE_DOMAIN; // If an arg is Nan a domain error will occur.
    return se;
  }
}
