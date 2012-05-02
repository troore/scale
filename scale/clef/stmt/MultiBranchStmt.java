package scale.clef.stmt;

import scale.common.Vector;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This is the base class for all multi-way branch statements.
 * <p>
 * $Id: MultiBranchStmt.java,v 1.29 2006-03-31 23:29:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class MultiBranchStmt extends Statement
{
  private Expression        expr;   // The selection expression.
  private Vector<LabelDecl> labels; // A vector of statement labels.

  /**
   * @param expr is the selection value
   * @param labels is the set of statement labels.
   */
  public MultiBranchStmt(Expression expr, Vector<LabelDecl> labels)
  {
    setExpr(expr);
    this.labels = labels;
  }

  public void visit(Predicate p)
  {
    p.visitMultiBranchStmt(this);
  }

  public final Expression getExpr()
  {
    return expr;
  }

  protected final void setExpr(Expression expr)
  {
    this.expr = expr;
  }

  protected final void setLabels(Vector<LabelDecl> labels)
  {
    this.labels = labels;
  }

  /**
   *
   */
  public final int numLabels()
  {
    return labels.size();
  }

  /**
   *
   */
  public final LabelDecl getLabel(int i)
  {
    return labels.elementAt(i);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return expr;

    return (Node) labels.elementAt(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1 + labels.size();
  }
}
