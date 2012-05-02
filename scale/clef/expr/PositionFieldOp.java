package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.FieldDecl;

/**
 * The PositionFieldOp class represents a position in an aggregation
 * by specifying the <code>struct</code> field to be initialized.
 * <p>
 * $Id: PositionFieldOp.java,v 1.1 2005-03-14 22:28:56 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class PositionFieldOp extends PositionOp
{
  private FieldDecl field;

  public PositionFieldOp(FieldDecl field)
  {
    super();
    this.field = field;
  }

  /**
   * Return the field.
   */
  public final FieldDecl getField()
  {
    return field;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;

    PositionFieldOp op = (PositionFieldOp) exp;
    return (field == op.field);
  }

  public String toString()
  {
    return "(pf " + field.getName() + ")";
  }
}
