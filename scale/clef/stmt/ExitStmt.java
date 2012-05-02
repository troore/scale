package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents a Unix like exit() routine call.
 * <p>
 * $Id: ExitStmt.java,v 1.25 2006-06-06 14:49:19 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ExitStmt extends Statement
{
  /**
   * the argument to the exit.
   */
  private Expression expr;

  public ExitStmt(Expression expr)
  {
    setExpr(expr);
  }

  public void visit(Predicate p)
  {
    p.visitExitStmt(this);
  }

  /**
   * Return the exit value.
   */
  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Specify the exit value.
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
    return true;
  }
}
