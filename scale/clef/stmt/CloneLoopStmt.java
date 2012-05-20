package scale.clef.stmt;

import scale.common.Vector;

import scale.clef.*;
import scale.clef.symtab.*;

/**
 * This class is the abstract class for all clone loop statements.
 * $Id: CloneLoopStmt.java, 2012-05-18 16:39:03 troore $
 */

public class CloneLoopStmt extends Statement
{
  /**
   * The statement that is iterated.
   */
  private Statement stmt;

  /**
   * Number of threads
   */
  private int clnNum;

  /**
   * Number of slices of per thread
   */
  private int slcNum;


  public CloneLoopStmt(Statement stmt, int clnNum, int slcNum)
  {
    setStmt(stmt);
	this.clnNum = clnNum;
	this.slcNum = slcNum;
  }

  public void visit(Predicate p)
  {
    p.visitCloneLoopStmt(this);
  }

  /**
   * Return the statement that is iterated.
   */
  public final Statement getStmt()
  {
    return stmt;
  }

  public final SymtabScope getScope ()
  {
	  if (stmt instanceof BlockStmt)
	  {
		  return ((BlockStmt)stmt).getScope ();
	  }

	  return null;
  }

  /**
   * Return the clone number
   */
  public final int getClnNum()
  {
	return clnNum;  
  }

  /**
   * Return the slice number
   */
  public final int getSlcNum()
  {
	return slcNum;  
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
