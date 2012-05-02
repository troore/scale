package scale.clef.stmt;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the C-style if statement.
 * <p>
 * $Id: IfThenElseStmt.java,v 1.27 2006-09-22 19:48:33 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class IfThenElseStmt extends IfStmt
{
  /**
   * The then statement.
   */
  private Statement  thenStmt;
  /**
   * The else statement.
   */
  private Statement  elseStmt;

  public IfThenElseStmt(Expression e, Statement thenStmt, Statement elseStmt)
  {
    super(e);
    setThenStmt(thenStmt);
    setElseStmt(elseStmt);
  }

  public void visit(Predicate p)
  {
    p.visitIfThenElseStmt(this);
  }

  /**
   * Return the "then" clause.
   */
  public final Statement getThenStmt()
  {
    return thenStmt;

  }

  /**
   * Return the "else" clause.
   */
  public final Statement getElseStmt()
  {
    return elseStmt;
  }

  /**
   * Specify the "then" clause.
   */
  protected final void setThenStmt(Statement thenStmt)
  {
    this.thenStmt = thenStmt;
  }

  /**
   * Specify the "else" clause.
   */
  protected final void setElseStmt(Statement elseStmt)
  {
    this.elseStmt = elseStmt;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getExpr();
    if (i == 1)
      return thenStmt;
    assert (i == 2) : "No such child " + i;
    return elseStmt;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 3;
  }

  /**
   * Return true if this statement is, or contains, a return statement
   * or a call to <code>exit()</code>.
   */
  public boolean hasReturnStmt()
  {
    if ((thenStmt != null) && thenStmt.hasReturnStmt())
      return true;
    if ((elseStmt != null) && elseStmt.hasReturnStmt())
      return true;
    return false;
  }

  /**
   * Return the number of statements represented by this statement.
   * Most statements return 1.  A block statement returns sum of the
   * number of statements in each statement in the block.
   */
  public int numTotalStmts()
  {
    int num = 1;
    if (thenStmt != null)
      num += thenStmt.numTotalStmts();
    if (elseStmt != null)
      num += elseStmt.numTotalStmts();
    return num;
  }

  /**
   * Return true if this statement is a loop statement or contains a
   * loop statement.
   */
  public boolean containsLoopStmt()
  {
    if ((thenStmt != null) && thenStmt.containsLoopStmt())
        return true;
    if ((elseStmt != null) && elseStmt.containsLoopStmt())
        return true;
    return false;
  }

  public void dump(String indents, java.io.PrintStream out)
  {
    out.print(Debug.formatInt(getSourceLineNumber(), 4));
    out.print(indents);
    out.print("if (");
    out.print(getExpr());
    out.println(") then");
    if (thenStmt != null)
      thenStmt.dump(indents + "  ", out);
    if (elseStmt != null) {
      out.print(Debug.formatInt(getSourceLineNumber(), 4));
      out.print(indents);
      out.println("else");
      elseStmt.dump(indents + "  ", out);
    }
  }
}
