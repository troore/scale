package scale.clef.decl;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.expr.*;

/**
 * This class represents a C case label in a switch statement.
 * <p>
 * $Id: CaseLabelDecl.java,v 1.25 2007-03-21 13:31:50 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class CaseLabelDecl extends LabelDecl
{
  /**
   * Should evaluate to a constant - may be null.
   */
  private Expression value; 

  /**
   * @param name - the label name
   * @param value - the case value
   */
  public CaseLabelDecl(String name, Expression value)
  {
    super(name);
    setExpr(value);
  }

  public void visit(Predicate p)
  {
    p.visitLabelDecl(this);
  }

  /**
   * Return the case label value.
   */
  public Expression getExpr()
  {
    return value;
  }

  /**
   * Set the value of the case label.
   */
  public void setExpr(Expression value)
  {
    this.value = value;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return value;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name)
  {
    return new CaseLabelDecl(name, value);
  }

  public final boolean isCaseLabelDecl()
  {
    return true;
  }

  public final CaseLabelDecl returnCaseLabelDecl()
  {
    return this;
  }
}
