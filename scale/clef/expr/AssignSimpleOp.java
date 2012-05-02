package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the assignment operation for atomic data.
 * <p>
 * $Id: AssignSimpleOp.java,v 1.22 2007-06-04 00:30:13 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class AssignSimpleOp extends AssignmentOp 
{
  public AssignSimpleOp(Type type, Expression lhs, Expression rhs)
  {
    super(type, lhs, rhs);
  }

  public AssignSimpleOp(Expression lhs, Expression rhs)
  {
    this(lhs.getCoreType().getPointedTo(), lhs, rhs);
  }

  public void visit(Predicate p)
  {
    p.visitAssignSimpleOp(this);
  }
}
