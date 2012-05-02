package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.expr.ShiftMode;
import scale.clef.type.Type;
import scale.score.Predicate;

/**
 * This class represents the bit shift operations.
 * <p>
 * $Id: BitShiftExpr.java,v 1.26 2007-10-04 19:58:28 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see scale.clef.expr.ShiftMode
 */
public class BitShiftExpr extends BinaryExpr
{
  /**
   * Specify C or Java style shifting..
   */
  private ShiftMode mode; 

  /**
   * @param mode is the shift mode
   * @see scale.clef.expr.BitShiftOp
   */
  public BitShiftExpr(Type t, Expr e1, Expr e2, ShiftMode mode)
  {
    super(t, e1, e2);
    this.mode = mode;
  }

  /**
   * The expression type is the same as the type of expression e1.
   */
  public BitShiftExpr(Expr e1, Expr e2, ShiftMode mode)
  {
    this(e1.getType(), e1, e2, mode);
  }

  /**
   * Set the shift mode.
   * @see scale.clef.expr.BitShiftOp
   */
  public void setShiftMode(ShiftMode mode)
  {
    this.mode = mode;
  }

  /**
   * Return the shift mode.
   */
  public ShiftMode getShiftMode()
  {
    return mode;
  }

  public void visit(Predicate p)
  {
    p.visitBitShiftExpr(this);
  }

  public Expr copy()
  {
    return new BitShiftExpr(getType(), getLeftArg().copy(), getRightArg().copy(), mode);
  }

  public String getDisplayLabel()
  {
    return mode.toString();
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal la = getLeftArg().getConstantValue(cvMap);
    if (Lattice.isZero(la))
      return scale.clef.LiteralMap.put(0, getCoreType());
    Literal ra = getRightArg().getConstantValue(cvMap);
    switch (mode) {
    case Left:
      return scale.common.Lattice.shiftLeft(getCoreType(), la, ra);
    case SignedRight:
      return scale.common.Lattice.shiftSignedRight(getCoreType(), la, ra);
    case UnsignedRight:
      return scale.common.Lattice.shiftUnsignedRight(getCoreType(), la, ra);
    case LeftRotate:
    case RightRotate:
    default: return Lattice.Bot;
    }
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getLeftArg().getConstantValue();
    if (Lattice.isZero(la))
      return scale.clef.LiteralMap.put(0, getCoreType());
    Literal ra = getRightArg().getConstantValue();
    switch (mode) {
    case Left:
      return scale.common.Lattice.shiftLeft(getCoreType(), la, ra);
    case SignedRight:
      return scale.common.Lattice.shiftSignedRight(getCoreType(), la, ra);
    case UnsignedRight:
      return scale.common.Lattice.shiftUnsignedRight(getCoreType(), la, ra);
    case LeftRotate:
    case RightRotate:
    default: return Lattice.Bot;
    }
  }

  /**
   * Return true if the expressions are equivalent.  This operation is
   * aware of commutative functions.  This method should be called by
   * the equivalent() method of the derived classes.
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    BitShiftExpr bse = (BitShiftExpr) exp;
    return (mode == bse.mode);
  }
}
