package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.IntegerType;
import scale.score.Predicate;

/**
 * This class represents three-valued comparison functions. 
 * <p>
 * $Id: CompareExpr.java,v 1.26 2007-04-27 18:04:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If the left argument is less than the right argument, a -1 is
 * returned.  If the left argument is greater than the right argument,
 * a 1 is returned.  Otherwise, a 0 is returned.  If the arguments are
 * floating point and either is Nan, then either a -1 or a 1 is
 * returned depending on the mode setting.
 * <p>
 * We use instances of this class instead of generating comparisons
 * and IfThenElseChords directly in the CFG.  There is no benefit to
 * generating the decision points in the CFG and doing so makes the
 * CFG unnecessarily complex.  Also, there is no easy way to test for
 * a NaN in C code so a function call would have to be made to do that
 * test anyway.
 * <p>
 * By using this class, Scribble2C can generate a function call for
 * floating point arguments and use the C conditional operator inline
 * for integer arguments.
 * <p>
 * When going direct to machine code, an instance of this class can be
 * converted directly into machine instructions that test for NaN.
 */
public class CompareExpr extends BinaryExpr 
{
  /**
   * Use normal compare.
   */
  public static final int Normal = 0;
  /**
   * If either argument is Nan, return -1.
   */
  public static final int FloatL = 1;
  /**
   * If either argument is Nan, return 1.
   */
  public static final int FloatG = 2;
  /**
   * String representation of the compare mode.
   */
  public static final String[] modes = {"Normal", "FloatL", "FloatG"};

  /**
   * The compare mode.
   */
  private int mode;

  /**
   * @param type is the expression type
   * @param la is the left operand
   * @param ra is the right operand
   * @param mode determines the type of comparison
   */
  public CompareExpr(Type type, Expr la, Expr ra, int mode)
  {
    super(type, la, ra);
    assert ((mode >= 0) && (mode <= FloatG)) :
           "Improper mode " + mode;
    this.mode = mode;
  }

  /**
   * The expression type is integer two's complement.
   * @param la is the left operand
   * @param ra is the right operand
   * @param mode determines the type of comparison
   */
  public CompareExpr(Expr la, Expr ra, int mode)
  {
    this(Machine.currentMachine.getIntegerCalcType(), la, ra, mode);
  }

  public Expr copy()
  {
    return new CompareExpr(getType(), getLeftArg().copy(), getRightArg().copy(), mode);
  }

  /**
   * Return the mode of the comparison operation.
   */
  public int getMode()
  {
    return mode;
  }

  public void visit(Predicate p)
  {
    p.visitCompareExpr(this);
  }

  public String getDisplayLabel()
  {
    return "cmp " + modes[mode];
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return r;

    if (mode == Normal) {
      Literal la = getLeftArg().getConstantValue(cvMap);
      Literal ra = getRightArg().getConstantValue(cvMap);
      r = Lattice.compare(getCoreType(), la, ra);
    } else
      r = Lattice.Bot;

    cvMap.put(this, r);
    return r;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    if (mode == Normal) {
      Literal la = getLeftArg().getConstantValue();
      Literal ra = getRightArg().getConstantValue();
      return Lattice.compare(getCoreType(), la, ra);
    }

    return Lattice.Bot;
  }
}
