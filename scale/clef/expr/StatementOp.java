package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.stmt.Statement;
import scale.clef.stmt.EvalStmt;
import scale.clef.stmt.BlockStmt;

/**
 * A class which allows a statement to be included as part of an expression.
 * <p>
 * $Id: StatementOp.java,v 1.8 2007-01-31 18:36:56 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instances of this class are used to represent the GNU expression
 * statement.
 */

public class StatementOp extends MonadicOp
{
  /**
   * The statement associated with the StatementOp.
   */
  private Statement statement;

  public StatementOp(Type type, Expression exp, Statement statement)
  {
    super(type, exp);
    this.statement = statement;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    StatementOp op = (StatementOp) exp;
    return (statement == op.statement);
  }

  public void visit(Predicate p)
  {
    p.visitStatementOp(this);
  }

  /**
   * Return the statement associated with the StatementOp.
   */
  public final Statement getStatement()
  {
    return statement;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getExpr();
    assert (i == 1) : "No such child " + i;
    return statement;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }

  public boolean isSimpleOp()
  {
    return (statement == null) && super.isSimpleOp();
  }
}
