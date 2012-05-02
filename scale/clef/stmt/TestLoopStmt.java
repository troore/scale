package scale.clef.stmt;

import scale.common.Vector;

import scale.clef.*;
import scale.clef.expr.*;

/**
 * The TestLoopStmt class is the abstract class for loop statements that have a
 * termination expression.
 * <p>
 * $Id: TestLoopStmt.java,v 1.26 2005-03-24 13:57:08 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Examples include C-style for and while loops but not
 * Fortran-style do loops.
 */

public abstract class TestLoopStmt extends LoopStmt 
{
  /**
   * The test expression.
   */
  private Expression expr;

  public TestLoopStmt(Statement s, Expression exp)
  {
    super(s);
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
    p.visitTestLoopStmt(this);
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
}
