package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.score.pred.References;

/**
 * This class is the superclass of all operators with variable arity.
 * <p>
 * $Id: NaryExpr.java,v 1.52 2007-10-17 13:40:00 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Operands of instances of this class can be null (see {@link
 * scale.score.trans.PRE PRE}).
 */
public abstract class NaryExpr extends Expr
{
  /**
   * A vector of expressions.
   */
  private Vector<Expr> operands;

  public NaryExpr(Type t, Vector<Expr> operands)
  {
    super(t);
    this.operands = operands;

    // Add other side of data edges.

    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.setOutDataEdge(this);
    }
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

    NaryExpr o = (NaryExpr) exp;

    int n = operands.size();
    if (n != o.operands.size())
      return false;

    for (int i = 0; i < n; i++) {
      Expr op1 = operands.elementAt(i);
      Expr op2 = o.operands.elementAt(i);
      if (op1 == null) {
        if (op2 != null)
	  return false;
	continue;
      }
      if (!op1.equivalent(op2))
        return false;
    }

    return true;
  }

  /**
   * Add an additional operand to the expression.
   * The new operand becomes the new last operand.
   */
  public void addOperand(Expr operand)
  {
    operands.addElement(operand);

    if (operand != null)
      operand.setOutDataEdge(this); // Add other side of data edges.
  }

  /**
   * Set the specified operand and return the previous operand that was
   * at that location.  The data edges are set properly.
   * The operand may be <code>null</code>,
   */
  public Expr setOperand(Expr operand, int position)
  {
    Expr op = operands.elementAt(position);

    if (op != null)
      op.deleteOutDataEdge(this);

    operands.setElementAt(operand, position);

    if (operand != null)
      operand.setOutDataEdge(this); // Add other side of data edges.

    return op;
  }

  /**
   * Return the nth operand.
   * @param position the index of the operand
   */
  public Expr getOperand(int position)
  {
    return operands.elementAt(position);
  }

  /** 
   * Allow some expressions such as VectorExpr to remove an operand.
   */
  protected void removeOperand(int i)
  {
    if (operands != null) {
      Expr op = operands.elementAt(i);
      operands.removeElementAt(i);
      if (op != null) {
        op.deleteOutDataEdge(this);
        op.unlinkExpression();
      }
    }
  }

  /**
   * Return the number of operands to this expression.
   */
  public int numOperands()
  {
    if (operands == null)
      return 0;
    return operands.size();
  }

  /**
   * Return an array of the operands to the expression.
   */
  public Expr[] getOperandArray()
  {
    int l = operands.size();
    int k = 0;
    Expr[] array = new Expr[numInDataEdges()];
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      array[k++] = op;
    }

    return array;
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();

    int len = operands.size();
    for (int i = 0; i < len; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.loopClean();
    }
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    int len = operands.size();
    for (int i = 0; i < len; i++) {
      Expr exp = operands.elementAt(i);
      if (exp == null)
        continue;
      exp.unlinkExpression();
    }
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if ((op != null) && op.containsDeclaration(decl))
        return true;
    }
    return false;
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if ((op != null) && op.dependsOnDeclaration(decl))
        return true;
    }
    return false;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if ((op != null) && !op.optimizationCandidate())
        return false;
    }
    return true;
  }

  /**
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.getDeclList(varList);
    }
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.getLoadExprList(expList);
    }
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.getExprList(expList);
    }
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
	wl.push(op);
    }
  }

  /**
   * Replace all occurrances of a Declaration with another Declaration.
   * Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    boolean changed = false;
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        changed |= op.replaceDecl(oldDecl, newDecl);
    }
    return changed;
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.removeUseDef();
    }
  }

  /**
   * Check this node for validity.
   * This method throws an exception if the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op == null)
	continue;
      if (op.getOutDataEdge() != this)
        throw new scale.common.InternalError("One way " + this + " -> " + op);
      op.validate();
    }
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.recordRefs(stmt, refs);
    }
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    int l = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        op.removeRefs(stmt, refs);
    }
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    int cost = 0;
    int l    = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        cost += op.executionCostEstimate();
    }
    return cost;
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int se = SE_NONE;
    int l    = operands.size();
    for (int i = 0; i < l; i++) {
      Expr op = operands.elementAt(i);
      if (op != null)
        se |= op.sideEffects();
    }
    if (getCoreType().isRealType())
      se |= SE_DOMAIN; // If an arg is Nan a domain error will occur.
    return se;
  }
}
