package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * A class which represents ternary operators.
 * <p>
 * $Id: TernaryOp.java,v 1.11 2007-08-28 17:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class TernaryOp extends Expression
{
  /**
   * The first expression.
   */
  private Expression expr1;
  /**
   * The second expression.
   */
  private Expression expr2;
  /**
   * The third expression.
   */
  private Expression expr3;

  public TernaryOp(Type type, Expression e1, Expression e2, Expression e3)
  {
    super(type);
    setExpr1(e1);
    setExpr2(e2);
    setExpr3(e3);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    TernaryOp op = (TernaryOp) exp;
    return (expr1.equivalent(op.expr1) &&
            expr2.equivalent(op.expr2) &&
            expr3.equivalent(op.expr3));
  }

  public void visit(Predicate p)
  {
    p.visitTernaryOp(this);
  }

  /**
   * Return the left argument.
   */
  public final Expression getExpr1()
  {
    return expr1;
  }

  /**
   * Return the middle argument.
   */
  public final Expression getExpr2()
  {
    return expr2;
  }

  /**
   * Return the right argument.
   */
  public final Expression getExpr3()
  {
    return expr3;
  }

  /**
   * Specify the left argument.
   */
  protected final void setExpr1(Expression expr1)
  {
    this.expr1 = expr1;
  }

  /**
   * Specify the middle argument.
   */
  protected final void setExpr2(Expression expr2)
  {
    this.expr2 = expr2;
  }

  /**
   * Specify the right argument.
   */
  protected final void setExpr3(Expression expr3)
  {
    this.expr3 = expr3;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return expr1;
    if (i == 1)
      return expr2;
    assert (i == 2) : "No such child " + i;
    return expr3;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 3;
  }

  public boolean isSimpleOp()
  {
    return expr1.isSimpleOp() && expr2.isSimpleOp() && expr3.isSimpleOp();
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return (expr1.containsDeclaration(decl) ||
            expr2.containsDeclaration(decl) ||
            expr3.containsDeclaration(decl));
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    expr1.getDeclList(varList);
    expr2.getDeclList(varList);
    expr3.getDeclList(varList);
  }
}
