package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * This class is the abstract class for operations with two arguments.
 * <p>
 * $Id: DyadicOp.java,v 1.27 2007-08-28 17:58:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class DyadicOp extends Expression
{
  /**
   * The left argument.
   */
  private Expression expr1;

  /**
   * The right argument.
   */
  private Expression expr2;

  public DyadicOp(Type type, Expression left, Expression right)
  {
    super(type);
    setExpr1(left);
    setExpr2(right);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    DyadicOp op = (DyadicOp) exp;
    return expr2.equivalent(op.expr2) && expr1.equivalent(op.expr1);
  }

  public void visit(Predicate p)
  {
    p.visitDyadicOp(this);
  }

  /**
   * Return the left argument of the operator.
   */
  public final Expression getExpr1()
  {
    return expr1;
  }

  /**
   * Return the left argument of the operator.
   */
  public final Expression getLhs()
  {
    return expr1;
  }

  /**
   * Return the right argument of the operator.
   */
  public final Expression getRhs()
  {
    return expr2;
  }

  /**
   * Specify the left argument of the operator.
   */
  protected final void setExpr1(Expression expr1)
  {
    this.expr1 = expr1;
  }

  /**
   * Specify the left argument of the operator.
   */
  protected final void setLhs(Expression e)
  {
    setExpr1(e);
  }

  /**
   * Specify the right argument of the operator.
   */
  protected final void setRhs(Expression e)
  {
    setExpr2(e);
  }

  /**
   * Return the right argument of the operator.
   */
  public final Expression getExpr2()
  {
    return expr2;
  }
  
  /**
   * Specify the right argument of the operator.
   */
  protected final void setExpr2(Expression expr2)
  {
    this.expr2 = expr2;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return expr1;
    assert (i == 1) : "No such child " + i;
    return expr2;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }

  public boolean isSimpleOp()
  {
    return expr1.isSimpleOp() && expr2.isSimpleOp();
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return expr1.containsDeclaration(decl) || expr2.containsDeclaration(decl);
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    expr1.getDeclList(varList);
    expr2.getDeclList(varList);
  }
}
