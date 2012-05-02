package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents a C-style for loop statement.
 * <p>
 * $Id: ForLoopStmt.java,v 1.24 2005-03-24 13:57:07 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ForLoopStmt extends TestLoopStmt
{
  /**
   * The initialization expression.
   */
  private Expression exprInit;
  /**
   * The increment expression.
   */
  private Expression exprInc;

  public ForLoopStmt(Statement s, Expression eInit, Expression eTest, Expression eInc)
  {
    super(s, eTest);
    setExprInit(eInit);
    setExprInc(eInc);
  }

  public void visit(Predicate p)
  {
    p.visitForLoopStmt(this);
  }

  /**
   * Return the initialization expression.
   */
  public final Expression getExprInit()
  {
    return exprInit;
  }

  /**
   * Return the termination expression.
   */
  public final Expression getExprTest()
  {
    return getExpr();
  }

  /**
   * Return the increment expression.
   */
  public final Expression getExprInc()
  {
    return exprInc;
  }

  /**
   * Specify the initialization expression.
   */
  protected final void setExprInit(Expression expr)
  {
    this.exprInit = expr;
  }

  /**
   * Specify the termination expression.
   */
  protected final void setExprTest(Expression expr)
  {
    setExpr(expr);
  }

  /**
   * Specify the increment expression.
   */
  protected final void setExprInc(Expression expr)
  {
    this.exprInc = expr;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getStmt();
    if (i == 1)
      return getExpr();
    if (i == 2)
      return exprInit;
    assert (i == 3) : "No such child " + i;
    return exprInc;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 4;
  }
}
