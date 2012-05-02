package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This is the base class for all compound assignments such as
 * <code>+=</code>.
 * <p>
 * $Id: CompoundAssignmentOp.java,v 1.23 2006-12-18 21:36:49 burrill Exp $
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

public abstract class CompoundAssignmentOp extends AssignmentOp 
{
  private Type calcType; // Type required for computing right-hand-side value.

  /**
   * @param type is the type of the expression
   * @param calcType is the type required for the right-hand-side computation
   * @param lhs is the left-hand-side expression of the assignment
   * @param ra is right argument to the right-hand-side expression of the assignment
   */
  public CompoundAssignmentOp(Type type, Type calcType, Expression lhs, Expression ra)
  {
    super(type, lhs, ra);
    this.calcType = calcType;
  }

  /**
   * Return the type required to perform the computation prior to the
   * assignment.  For example, in
   * <pre>
   *   int x;
   *   x = *= 0.5;
   * </pre>
   * the computation <code>x * 0.5</code> must be performed as
   * <code>(int) (((double) x) * 0.5)</code>.  So, even though the
   * type of the expression is <code>int</code>, the computational
   * type is <code>double</code>.
   */
  public final Type getCalcType()
  {
    return calcType;
  }

  public void visit(Predicate p)
  {
    p.visitCompoundAssignmentOp(this);
  }
}
