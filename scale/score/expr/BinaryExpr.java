package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.score.analyses.AliasAnnote;
import scale.score.pred.References;

/**
 * This class is the superclass of all binary operators.
 * <p>
 * $Id: BinaryExpr.java,v 1.63 2007-10-17 13:39:59 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class BinaryExpr extends Expr
{
  /**
   * Left argument.
   */
  private Expr la; 
  /**
   * Right argument.
   */
  private Expr ra; 

  public BinaryExpr(Type t, Expr la, Expr ra)
  {
    super(t);
    this.la = la;
    if (la != null)
      la.setOutDataEdge(this); // Add other side of data edges.
    this.ra = ra;
    ra.setOutDataEdge(this); // Add other side of data edges.
  }

  /**
   * Return true if the expressions are equivalent.  This operation is
   * aware of commutative functions.  This method should be called by
   * the equivalent() method of the derived classes.
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    BinaryExpr o = (BinaryExpr) exp;
    return (la.equivalent(o.la) && ra.equivalent(o.ra)) ||
           (isCommutative() && la.equivalent(o.ra) && ra.equivalent(o.la));
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
  public Expr setOperand(Expr operand, int position)
  {
    assert (operand != null) : "An expression operand can not be null " + this;

    Expr old = null;

    if (position == 0) {
      old = la;
      la = operand;
    } else if (position == 1) {
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
  public Expr getOperand(int position)
  {
    if (position == 0)
      return la;
    else if (position == 1)
      return ra;
    else
      throw new scale.common.InternalError("Invalid operand position - " + position);
  }

  /**
   * Return an array of the operands to the expression.
   */
  public Expr[] getOperandArray()
  {
    Expr[] array = new Expr[2];
    array[0] = la;
    array[1] = ra;
    return array;
  }

  /**
   * Return the number of operands to this expression.
   */
  public int numOperands()
  {
    return 2;
  }

  /**
   * Return the left argument to the operator.
   */
  public final Expr getLeftArg()
  {
    return la;
  }

  /**
   * Return the right argument to the operator.
   */
  public final Expr getRightArg()
  {
    return ra;
  }

  /**
   * Replace the left operand.  This method eliminates the data edge
   * between the previous left argument expression and this
   * expression.  The <code>la</code> argument may be <code>null</code>.
   * @param la is the new left argument
   */
  public final void setLeftArg(Expr la)
  {
    Expr old = this.la;
    if (old != null)
      old.deleteOutDataEdge(this);

    if (la != null)
      la.setOutDataEdge(this); // Add other side of data edges.
    this.la = la;
  }

  /**
   * Replace the right operand.  This method eliminates the data edge
   * between the previous right argument expression and this
   * expression.  The <code>ra</code> argument may be <code>null</code>.
   * @param ra is the new right argument
   */
  public final void setRightArg(Expr ra)
  {
    Expr old = this.ra;
    if (old != null)
      old.deleteOutDataEdge(this);

    if (ra != null)
      ra.setOutDataEdge(this); // Add other side of data edges.
    this.ra = ra;
  }

  /**
   * Swap the left and right arguments.
   */
  public final void swapOperands()
  {
    Expr t = la;
    la = ra;
    ra = t;
  }

  /**
   * See if the associative property of the expression can be used to
   * make two constants the arguments of the same binary expression.
   * For example, <code>(x + c1) + c2<code> is changed to <code>(c2 +
   * c1) + x</code>.  This method should be called only by associative
   * binary expressions.
   */
  public void associativeSwapOperands()
  {
    if (!isAssociative())
      return;

    if (isCommutative() && la.isLiteralExpr())
      swapOperands();

    if (!ra.isLiteralExpr())
      return;

    if (la.getClass() != this.getClass())
      return;

    BinaryExpr ae  = (BinaryExpr) la;
    Expr       lae = ae.la;
    Expr       rae = ae.ra;
    Expr       rr  = ra;

    if (lae.isLiteralExpr()) {
      rr.deleteOutDataEdge(this);
      rr.setOutDataEdge(ae);
      ae.ra = rr;
      ae.setType(rr.getType());

      rae.deleteOutDataEdge(ae);
      rae.setOutDataEdge(this);
      ra = rae;
    } else if (rae.isLiteralExpr()) {
      rr.deleteOutDataEdge(this);
      rr.setOutDataEdge(ae);
      ae.la = rr;
      ae.setType(rr.getType());

      lae.deleteOutDataEdge(ae);
      lae.setOutDataEdge(this);
      ra = lae;
      if (isCommutative()) // Make sure the constant is on the right.
        swapOperands();
    }
  }

  /**
   * Return true if the binary operation is commutative:
   * (a &alpha; b) &equiv; (b &alpha a).
   */
  public boolean isCommutative()
  {
    return false;
  }

  /**
   * Return true if the binary operation is associative:
   * (a &alpha; b) &alpha; c &equiv; a &alpha; (b &alpha; c).
   */
  public boolean isAssociative()
  {
    return false;
  }

  /**
   * Return true if the binary operation &alpha; is left distributive
   * over &beta;: (a &beta; b) &alpha; c &equiv; (a &alpha; c) &beta;
   * (b &alpha; c).
   */ 
  public boolean isLeftDistributive()
  {
     return false;
  }     
  
  /**
   * Return true if the binary operation &alpha; is right distributive
   * over &beta;: a &alpha; (b &beta; c) &equiv; (a &alpha; b) &beta;
   * (a &alpha; c).
   */   
  public boolean isRightDistributive()
  {
     return false;
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();
    la.loopClean();
    ra.loopClean();
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    if (la != null)
      la.unlinkExpression();
    if (ra != null)
      ra.unlinkExpression();
  }

  /**
   * Return true if this expression contains a reference to the variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return la.containsDeclaration(decl) || ra.containsDeclaration(decl);
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    return la.dependsOnDeclaration(decl) || ra.dependsOnDeclaration(decl);
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic variables, etc
   * are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return la.optimizationCandidate() && ra.optimizationCandidate();
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

    AliasAnnote laa = la.getAliasAnnote();
    AliasAnnote raa = ra.getAliasAnnote();
    if (laa == null)
      return raa;
    if (raa == null)
      return laa;

    if (la.getCoreType().isPointerType())
      return laa;

    return raa;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 2 + la.executionCostEstimate() + ra.executionCostEstimate();
  }

  /**
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    la.getDeclList(varList);
    ra.getDeclList(varList);
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    la.getLoadExprList(expList);
    ra.getLoadExprList(expList);
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
    if (la != null)
      la.getExprList(expList);
    ra.getExprList(expList);
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
    wl.push(la);
    wl.push(ra);
  }

  /**
   * Replace all occurrances of a Declaration with another
   * Declaration.  Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return ra.replaceDecl(oldDecl, newDecl) | la.replaceDecl(oldDecl, newDecl);
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    ra.removeUseDef();
    la.removeUseDef();
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    if (la.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + la);
    if (ra.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + ra);

    la.validate();
    ra.validate();
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    la.recordRefs(stmt, refs);
    ra.recordRefs(stmt, refs);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    la.removeRefs(stmt, refs);
    ra.removeRefs(stmt, refs);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int se = la.sideEffects() | ra.sideEffects();
    if (getCoreType().isRealType())
      se |= SE_DOMAIN; // If an arg is Nan a domain error will occur.
    return se;
  }
}
