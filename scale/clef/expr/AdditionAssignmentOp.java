package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents <code> x += y</code>.
 * <p>
 * $Id: AdditionAssignmentOp.java,v 1.27 2006-12-18 21:36:48 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Note that <code>x += y</code> is not equivalent to <code>x = x +
 * y</code> when <code>x</code> is a an expression with side effects.
 */

public class AdditionAssignmentOp extends CompoundAssignmentOp 
{
  /**
   * @param type is the type of the expression
   * @param calcType is the type required for the right-hand-side computation
   * @param lhs is the left-hand-side expression of the assignment
   * @param ra is right argument to the right-hand-side expression of the assignment
   */
  public AdditionAssignmentOp(Type type, Type calcType, Expression lhs, Expression ra)
  { 
    super(type, calcType, lhs, ra);
  }

  public AdditionAssignmentOp(Type type, Expression lhs, Expression ra)
  { 
    this(type, type, lhs, ra);
  }

  public void visit(Predicate p)
  {
    p.visitAdditionAssignmentOp(this);
  }
}
