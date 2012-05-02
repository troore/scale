package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the Fortran 77 arithmetic if statement.
 * <p>
 * $Id: ArithmeticIfStmt.java,v 1.22 2005-03-24 13:57:06 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ArithmeticIfStmt extends IfStmt
{
  /**
   * The label of the negative branch statement.
   */
  private LabelDecl lessLabel;
  /**
   * The label of the zero branch statement.
   */
  private LabelDecl equalLabel;
  /**
   * The label of the positive branch statement.
   */
  private LabelDecl moreLabel;

  public ArithmeticIfStmt(Expression e,  LabelDecl less, LabelDecl equal, LabelDecl more)
  {
    super(e);
    lessLabel = less;
    equalLabel = equal;
    moreLabel = more;
  }

  public void visit(Predicate p)
  {
    p.visitArithmeticIfStmt(this);
  }

  /**
   * Return the label of the negative branch statement.
   */
  public final LabelDecl getLessLabel()
  {
    return lessLabel;
  }

  /**
   * Return the label of the zero branch statement.
   */
  public final LabelDecl getEqualLabel()
  {
    return equalLabel;
  }

  /**
   * Return the label of the positive branch statement.
   */
  public final LabelDecl getMoreLabel()
  {
    return moreLabel;
  }

  /**
   * Specify the label of the negative branch statement.
   */
  protected final void setLessLabel(LabelDecl lessLabel)
  {
    this.lessLabel = lessLabel;
  }

  /**
   * Specify the label of the zero branch statement.
   */
  protected final void setEqualLabel(LabelDecl equalLabel)
  {
    this.equalLabel = equalLabel;
  }

  /**
   * Specify the label of the positive branch statement.
   */
  protected final void setMoreLabel(LabelDecl moreLabel)
  {
    this.moreLabel = moreLabel;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getExpr();
    if (i == 1)
      return lessLabel;
    if (i == 2)
      return equalLabel;
    assert (i == 3) : "No such child " + i;
    return moreLabel;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 4;
  }
}
