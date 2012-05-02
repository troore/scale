package scale.clef.stmt;

import scale.common.*;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * Unused.
 * <p>
 * $Id: AltCase.java,v 1.30 2006-06-28 16:39:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class AltCase extends Node
{
  private Statement          stmt; // The statement.
  private Vector<Expression> keys; // A vector of Keys.

  public AltCase(Statement s)
  {
    this(s, new Vector<Expression>());
  }

  public AltCase(Statement s, Vector<Expression> k)
  {
    setStmt(s);
    setKeys(k);
  }

  public void visit(Predicate p)
  {
    p.visitAltCase(this);
  }

  public final Statement getStmt()
  {
    return stmt;
  }

  protected final void setStmt(Statement stmt)
  {
    this.stmt = stmt;
  }

  protected final void setKeys(Vector<Expression> keys)
  {
    this.keys = keys;
  }

  /**
   * Return the number of AltCase objects.
   */
  public final int numKeys()
  {
    if (keys == null)
      return 0;
    return keys.size();
  }

  /**
   * Return the AltCase.
   */
  public Expression getKey(int i)
  {
    return keys.elementAt(i);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return stmt;
    return (Node) keys.elementAt(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1 + keys.size();
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
    return stmt.numTotalStmts();
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
