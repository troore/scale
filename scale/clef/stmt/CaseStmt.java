package scale.clef.stmt;

import scale.common.Vector;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents a M3 case statement.
 * <p>
 * $Id: CaseStmt.java,v 1.29 2006-06-28 16:39:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class CaseStmt extends Statement
{
  private Expression      expr; // The selection expression.
  private Vector<AltCase> alts; // A vector of the cases.

  public CaseStmt(Expression exp)
  {
    this(exp, new Vector<AltCase>());
  }

  public CaseStmt(Expression exp, Vector<AltCase> a)
  {
    setExpr(exp);
    setAlts(a);
  }

  public void visit(Predicate p)
  {
    p.visitCaseStmt(this);
  }

  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Return the number of AltCase objects.
   */
  public final int numAlts()
  {
    if (alts == null)
      return 0;
    return alts.size();
  }

  /**
   * Return the AltCase.
   */
  public AltCase getAlt(int i)
  {
    return alts.elementAt(i);
  }

  protected final void setExpr(Expression expr)
  {
    this.expr = expr;
  }

  protected final void setAlts(Vector<AltCase> alts)
  {
    this.alts = alts;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return expr;
    return (Node) alts.elementAt(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    if (alts == null)
      return 1;
    return 1 + alts.size();
  }

  /**
   * Return true if this statement is, or contains, a return statement
   * or a call to <code>exit()</code>.
   */
  public boolean hasReturnStmt()
  {
    if (alts == null)
      return false;

    int l = alts.size();
    for (int i = l - 1; i >= 0; i--) {
      AltCase stmt = alts.get(i);
      if (stmt.hasReturnStmt())
        return true;
    }

    return false;
  }

  /**
   * Return the number of statements represented by this statement.
   * Most statements return 1.  A block statement returns sum of the
   * number of statements in each statement in the block.
   */
  public int numTotalStmts()
  {
    int num = 0;
    int l   = alts.size();
    for (int i = 0; i < l; i++)
      num += alts.get(i).numTotalStmts();
    return num;
  }

  /**
   * Return true if this statement is a loop statement or contains a
   * loop statement.
   */
  public boolean containsLoopStmt()
  {
    int l = alts.size();
    for (int i = 0; i < l; i++)
      if (alts.get(i).containsLoopStmt())
        return true;
    return false;
  }
}
