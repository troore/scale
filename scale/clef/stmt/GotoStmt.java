package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the C-style goto statement.
 * <p>
 * $Id: GotoStmt.java,v 1.22 2005-03-24 13:57:07 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class GotoStmt extends Statement
{
  /**
   * The label of the statement to branch to.
   */
  private LabelDecl label;

  public GotoStmt(LabelDecl label)
  {
    this.label = label;
  }

  public void visit(Predicate p)
  {
    p.visitGotoStmt(this);
  }

  /**
   * Return the label of the statement to branch to.
   */
  public final LabelDecl getLabel()
  {
    return label;
  }

  /**
   * Specify the label of the statement to branch to.
   */
  protected final void setLabel(LabelDecl label)
  {
    this.label = label;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return label;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }
}
