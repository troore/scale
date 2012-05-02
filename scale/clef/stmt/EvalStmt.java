package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents statements with a single expression.
 * <p>
 * $Id: EvalStmt.java,v 1.28 2007-10-04 19:58:08 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class EvalStmt extends Statement
{
  /**
   * The expression to be evaluated.
   */
  private Expression expr;

  public EvalStmt(Expression exp)
  {
    assert (exp != null) : "No expression to eval.";
    setExpr(exp);
  }

  public void visit(Predicate p)
  {
    p.visitEvalStmt(this);
  }

  /**
   * Return the expression to be evaluated.
   */
  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Specify the expression to be evaluated.
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

  /**
   * Return true if this statement is, or contains, a return statement
   * or a call to <code>exit()</code>.
   */
  public boolean hasReturnStmt()
  {
    if (expr instanceof CallOp) {
      Expression ftn = ((CallOp) expr).getRoutine();
      if ((ftn instanceof IdReferenceOp) &&
          (((IdReferenceOp) ftn).getDecl().getName().equals("exit")))
        return true;
    }

    return false;
  }
}
