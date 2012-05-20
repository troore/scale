package scale.clef.stmt;

import scale.common.Vector;

import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.decl.*;

/**
 * The TestLoopStmt class is the abstract class for clone loop statements that have a
 * termination expression.
 * $Id: CloneTestLoopStmt.java, troore $
 */

public abstract class CloneTestLoopStmt extends CloneLoopStmt 
{
  /**
   * The test expression.
   */
  private Expression expr;

  public CloneTestLoopStmt(Statement s, Expression exp, int clnNum, int slcNum)
  {
    super(s, clnNum, slcNum);
    setExpr(exp);
  }

  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Return the test expression.
   */
  protected final void setExpr(Expression expr)
  {
    this.expr = expr;
  }

  public void visit(Predicate p)
  {
    p.visitCloneTestLoopStmt(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getStmt();
    assert (i == 1) : "No such child " + i;
    return expr;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }

  /**
   * Return loop variable name if expr is like "i < 32"
   */
  public String getLoopVarName ()
  {
	  if (expr instanceof DyadicOp)
	  {
		  Expression expr1 = ((DyadicOp)expr).getExpr1 ();
		  if (expr1 instanceof IdAddressOp)
		  {
			  Declaration decl = ((IdAddressOp)expr1).getDecl ();

			  return decl.getName ();
		  }
	  }

	  return null;
  }

}
