package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the C-style switch statement.
 * <p>
 * $Id: SwitchStmt.java,v 1.26 2006-06-28 16:39:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class SwitchStmt extends Statement
{
  /**
   * The selection expression
   */
  private Expression expr;
  /**
   * The statement.
   */
  private Statement  stmt;

  public SwitchStmt(Expression exp, Statement stmt)
  {
    setExpr(exp);
    setStmt(stmt);
  }

  public void visit(Predicate p)
  {
    p.visitSwitchStmt(this);
  }

  /**
   * Return the switch expression.
   */
  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Return the switch statement body.
   */
  public final Statement getStmt()
  {
    return stmt;
  }

  /**
   * Specify the switch expression.
   */
  protected final void setExpr(Expression expr)
  {
    this.expr = expr;
  }

  /**
   * Specify the switch statement body.
   */
  protected final void setStmt(Statement stmt)
  {
    this.stmt = stmt;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return expr;
    assert (i == 1) : "No such child " + i;
    return stmt;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }

  /**
   * Return true if this statement is, or contains, a return statement
   * or a call to <code>exit()</code>.
   */
  public boolean hasReturnStmt()
  {
    return ((stmt != null) && stmt.hasReturnStmt());
  }

  /**
   * Return the number of statements represented by this statement.
   * Most statements return 1.  A block statement returns sum of the
   * number of statements in each statement in the block.
   */
  public int numTotalStmts()
  {
    return 1 + stmt.numTotalStmts();
  }

  /**
   * Return true if this statement is a loop statement or contains a
   * loop statement.
   */
  public boolean containsLoopStmt()
  {
    return stmt.containsLoopStmt();
  }
}
