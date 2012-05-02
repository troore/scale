package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * This is the abstract class which represents an operation with one argument.
 * <p> 
 * $Id: MonadicOp.java,v 1.11 2007-08-28 17:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The type is promoted from the type of the expression.
 */

public abstract class MonadicOp extends Expression
{
  /**
   * The argument.
   */
  private Expression expr;

  public MonadicOp(Type type, Expression arg)
  {
    super(type);
    expr = arg;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    MonadicOp op = (MonadicOp) exp;
    return expr.equivalent(op.expr);
  }

  public void visit(Predicate p)
  {
    p.visitMonadicOp(this);
  }

  /**
   * Return the operator's argument expression.
   */
  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Specify the operator's argument expression.
   */
  protected final void setExpr(Expression expr)
  {
    this.expr = expr;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return expr;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  public boolean isSimpleOp()
  {
    return expr.isSimpleOp();
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return expr.containsDeclaration(decl);
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    expr.getDeclList(varList);
  }
}
