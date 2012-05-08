package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents a C-style for loop statement, different with normal 
 * for loop statement, it is a future-CLONED for loop statement, and has a 'thread'
 * number member.
 */

public class CloneForLoopStmt extends TestLoopStmt
{
  /**
   * The initialization expression.
   */
  private Expression exprInit;
  /**
   * The increment expression.
   */
  private Expression exprInc;

  /**
   * Number of threads
   */
  private int clnNum;

  public CloneForLoopStmt(Statement s, Expression eInit, Expression eTest, Expression eInc, int clnNum)
  {
    super(s, eTest);
    setExprInit(eInit);
    setExprInc(eInc);
	this.clnNum = clnNum;
  }

  public void visit(Predicate p)
  {
    p.visitCloneForLoopStmt(this);
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
   * Return the clone number
   */
  public final int getClnNum()
  {
	return clnNum;  
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
