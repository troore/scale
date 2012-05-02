package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * The BitShiftOp class is the class for all bit shifting operations.
 * <p>
 * $Id: BitShiftOp.java,v 1.18 2007-10-04 19:58:05 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class BitShiftOp extends DyadicOp
{
  /**
   * The type of the shift.
   */
  private ShiftMode  mode;

  /**
   * @param mode is the shift operation - right, left, etc.
   */
  public BitShiftOp(Type type, Expression e1, Expression e2, ShiftMode mode)
  {
    super(type, e1, e2);
    this.mode = mode;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    BitShiftOp op = (BitShiftOp) exp;
    return mode != op.mode;
  }

  public void visit(Predicate p)
  {
    p.visitBitShiftOp(this);
  }

  /**
   * Set the shift mode to C or Java. Default is C.
   */
  public void setShiftMode(ShiftMode mode)
  {
    this.mode = mode;
  }

  /**
   * Return the shift mode.
   * @see ShiftMode
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

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr1().getConstantValue();

    if (Lattice.isZero(la))
      return scale.clef.LiteralMap.put(0, getCoreType());

    Literal ra = getExpr2().getConstantValue();
    switch (mode) {
    case Left:          return scale.common.Lattice.shiftLeft(getCoreType(), la, ra);
    case SignedRight:   return scale.common.Lattice.shiftSignedRight(getCoreType(), la, ra);
    case UnsignedRight: return scale.common.Lattice.shiftUnsignedRight(getCoreType(), la, ra);
    case LeftRotate:
    case RightRotate:
    default: return Lattice.Bot;
    }
  }
}
