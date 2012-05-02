package scale.clef.expr;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.type.*;

/**
 * The AssignmentOp class is the abstract class for all assignment operations.
 * <p>
 * $Id: AssignmentOp.java,v 1.26 2005-02-07 21:27:54 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class AssignmentOp extends DyadicOp
{
  public AssignmentOp(Type type, Expression lhs, Expression rhs)
  {
    super(type, lhs, rhs);
  }

  public void visit(Predicate p)
  {
    p.visitAssignmentOp(this);
  }
}
