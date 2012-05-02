package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.score.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;
import scale.score.pred.References;

/**
 * A class which represents the C variable argument processing.
 * <p>
 * $Id: VarArgExpr.java,v 1.50 2007-10-17 13:40:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class VarArgExpr extends Expr
{
  /**
   * vaList
   */
  private Expr vaList;

  public VarArgExpr(Type t, Expr vaList)
  {
    super(t);
    this.vaList = vaList;
    vaList.setOutDataEdge(this); // Add other side of data edges.

    assert vaList.getCoreType().isPointerType() : "Must be address type " + t;

    Vector<LoadExpr> v = new Vector<LoadExpr>(2);
    vaList.getLoadExprList(v);
    int l = v.size();
    for (int i = 0; i < l; i++) {
      LoadExpr le = v.elementAt(i);
      if (le instanceof LoadDeclAddressExpr)
        le.getDecl().setAddressTaken();
    }
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return SE_STATE;
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
    VarArgExpr o = (VarArgExpr) exp;
    return vaList.equivalent(o.vaList);
  }

  protected Expr setOperand(Expr operand, int position)
  {
    assert (position == 0) : "Invalid operand position - " + position;

    Expr old = vaList;
    if (old != null)
      old.deleteOutDataEdge(this);

    vaList = operand;
    vaList.setOutDataEdge(this); // Add other side of data edges.

    return old;
  }

  /**
   * Return the nth operand.
   * @param position the index of the operand
   */
  public final Expr getOperand(int position)
  {
    assert (position == 0) : "Invalid operand position - " + position;

    return vaList;
  }

  /**
   * Return an array of the operands to the expression.
   */
  public final Expr[] getOperandArray()
  {
    Expr[] array = new Expr[1];
    array[0] = vaList;
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
   * Return the expression representing the va_list argument.
   */
  public final Expr getVaList()
  {
    return vaList;
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();
    vaList.loopClean();
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    super.unlinkExpression();
    vaList.unlinkExpression();
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
   * Return true if this expression contains a reference to the variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return vaList.containsDeclaration(decl);
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    return vaList.dependsOnDeclaration(decl);
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
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    vaList.getDeclList(varList);
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    vaList.getLoadExprList(expList);
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
    vaList.getExprList(expList);
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
    wl.push(vaList);
  }

  /**
   * Replace all occurrances of a Declaration with another Declaration.
   * Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return vaList.replaceDecl(oldDecl, newDecl);
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    vaList.removeUseDef();
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    if (vaList.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + vaList);

    vaList.validate();
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    vaList.recordRefs(stmt, refs);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    vaList.removeRefs(stmt, refs);
  }
}
