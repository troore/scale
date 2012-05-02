package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents <code> x <<= y</code> and <code>x >>= y</code>.
 * <p>
 * $Id: BitShiftAssignmentOp.java,v 1.13 2006-12-18 21:36:48 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Note that <code>x <<= y</code> is not equivalent to <code>x = x <<
 * y</code> when <code>x</code> is a an expression with side effects.
 * @see BitShiftOp
 */

public class BitShiftAssignmentOp extends CompoundAssignmentOp 
{
  /**
   * The type of the shift.
   */
  private ShiftMode mode;

  /**
   * @param type is the type of the expression
   * @param calcType is the type required for the right-hand-side computation
   * @param lhs is the left-hand-side expression of the assignment
   * @param ra is right argument to the right-hand-side expression of the assignment
   * @param mode is the shift operation - right, left, etc.
   * @see ShiftMode
   */
  public BitShiftAssignmentOp(Type       type,
                              Type       calcType,
                              Expression lhs,
                              Expression ra,
                              ShiftMode  mode)
  {
    super(type, calcType, lhs, ra);
    setShiftMode(mode);
  }

  /**
   * @param mode is the shift operation - right, left, etc.
   * @see BitShiftOp
   */
  public BitShiftAssignmentOp(Type       type,
                              Expression lhs,
                              Expression ra,
                              ShiftMode  mode)
  {
    this(type, type, lhs, ra, mode);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    BitShiftAssignmentOp op = (BitShiftAssignmentOp) exp;
    return (mode != op.mode);
  }

  public void visit(Predicate p)
  {
    p.visitBitShiftAssignmentOp(this);
  }

  /**
   * Set the shift mode to C or Java. Default is C.
   * @see BitShiftOp
   */
  public void setShiftMode(ShiftMode mode)
  {
    this.mode = mode;
  }

  /**
   * Return the shift mode.
   * @see BitShiftOp
   */
  public ShiftMode getShiftMode()
  {
    return mode;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(mode);
    return buf.toString();
  }
}
