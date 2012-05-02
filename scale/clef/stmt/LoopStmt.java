package scale.clef.stmt;

import scale.common.Vector;

import scale.clef.*;

/**
 * This class is the abstract class for all loop statements.
 * <p>
 * $Id: LoopStmt.java,v 1.27 2006-06-28 16:39:03 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class LoopStmt extends Statement
{
  /**
   * The statement that is iterated.
   */
  private Statement stmt;

  public LoopStmt(Statement stmt)
  {
    setStmt(stmt);
  }

  public void visit(Predicate p)
  {
    p.visitLoopStmt(this);
  }

  /**
   * Return the statement that is iterated.
   */
  public final Statement getStmt()
  {
    return stmt;
  }

  /**
   * Specify the statement that is iterated.
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
    assert (i == 0) : "No such child " + i;
    return stmt;
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
    return true;
  }
}
