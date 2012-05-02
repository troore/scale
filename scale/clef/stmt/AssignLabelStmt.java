package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the Fortran construct for assigning labels to variables.
 * <p>
 * $Id: AssignLabelStmt.java,v 1.28 2006-06-28 16:39:01 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class AssignLabelStmt extends Statement
{
  /**
   * The label.
   */
  private Declaration label;
  /**
   * The value.
   */
  private Declaration value;

  /**
   * @param label specifies the label
   * @param value specifies the value assigned to the label
   */
  public AssignLabelStmt(Declaration label, Declaration value)
  {
    this.label = label;
    this.value = value;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(label);
    buf.append(' ');
    buf.append(value);
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitAssignLabelStmt(this);
  }

  /**
   * Return the label.
   */
  public final Declaration getLabel()
  {
    return label;
  }

  /**
   * Return the assigned value.
   */
  public final Declaration getValue()
  {
    return value;
  }

  /**
   * Specify the label.
   */
  protected final void setLabel(Declaration l)
  {
    label = l;
  }

  /**
   * Specify the assigned value.
   */
  protected final void setValue(Declaration v)
  {
    value = v;
  }
}
