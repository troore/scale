package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This is the base class for all if statements.
 * <p>
 * $Id: IfStmt.java,v 1.25 2005-03-24 13:57:07 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class IfStmt extends Statement
{
  /**
   * The expression that determines the branch taken.
   */
  private Expression expr;

  public IfStmt(Expression exp)
  {
    setExpr(exp);
  }

  public void visit(Predicate p)
  {
    p.visitIfStmt(this);
  }

  /**
   * Return the test expression.
   */
  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Specify the test expression.
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
}
