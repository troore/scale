package scale.clef.type;

import java.lang.Long;

import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;

/**
 * A Bound class represents a range of allowed integer values.
 * <p>
 * $Id: Bound.java,v 1.62 2007-10-04 19:58:08 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class Bound extends Node
{
  /**
   * Because the original design represented a bounds using Fortran
   * notation, the result was the inability to represent a range of no
   * values.  We have kludged up the class to add this ability.  The
   * better way of doing this would be to represent the lower bound and
   * the number of values instead of the lower and upper bound.
   */

  private static Vector<Bound> bounds;     // A list of all the unique bounds.

  private Expression min; // The minimum allowed value.
  private long       minValue;

  private Expression max; // The maximum allowd value.
  private long       maxValue;

  private long numberOfElements; // The number of values in the range.  It may be zero.

  private static final int NORMAL    =  0; // Nothing known
  private static final int INTMIN    =  1; // The min value is a known integer value.
  private static final int INTMAX    =  2; // The max value is a known integer value.
  private static final int INTNOE    =  4; // The number of elements is a known integer value.
  private static final int UNBOUNDED =  8; // The range is unbounded
  private static final int MINCALC   = 16; // The value for min value has been calculated.
  private static final int MAXCALC   = 32; // The value for max value has been calculated.
  private static final int NOECALC   = 64; // The value for number of values has been calculated.
  private static final int NOBOUNDS  = INTMIN+INTMAX+MINCALC+MAXCALC+NOECALC+UNBOUNDED;
  private static final int NOVALUES  = INTMIN+INTMAX+INTNOE+MINCALC+MAXCALC+NOECALC;

  private int flags;

  /**
   * Represents a "no bounds".  In C, this would be represented by
   * <code>[]</code>.
   */
  public static final Bound noBound = new Bound(0, 0, 0, NOBOUNDS);
  /**
   * Represents a range with no values.  In C, this would be
   * represented by <code>[0]</code>.
   */
  public static final Bound noValues = new Bound(0, 0, 0, NOVALUES);

  /**
   * Create a representation of a range from min to max.
   * @param min is the lower bound
   * @param max is the upper bound
   */
  public static Bound create(Expression min, Expression max)
  {
    if (max == null) {
      if (min == null)
        return noBound;
      max = LiteralMap.put(0x7ffffff, Machine.currentMachine.getIntegerCalcType());
    }

    if (bounds != null) {
      int n = bounds.size();
      for (int i = 0; i < n; i++) {
        Bound ta = bounds.elementAt(i);

        if (!min.equivalent(ta.min))
          continue;

        if (!max.equivalent(ta.max))
          continue;

        return ta;
      }
    }
    Bound a = new Bound(min, max);
    return a;
  }

  /**
   * Create a representation of a range from <code>min</code> to
   * <code>max</code>.  If <code>min &gt; max</code>, the bound is
   * considered to be unbounded.
   * @param min the lower bound
   * @param max the upper bound
   */
  public static Bound create(long min, long max)
  {
    if (min > max)
      return noBound;

    IntegerType intType = Machine.currentMachine.getIntegerCalcType();
    Literal     lit0    = LiteralMap.put(min, intType);
    Literal     litn    = LiteralMap.put(max, intType);
    return create(lit0, litn);
  }

  private Bound(Expression min, Expression max)
  {
    this.flags = NORMAL;
    setMin(min);
    setMax(max);
    if (bounds == null)
      bounds = new Vector<Bound>(10);
    bounds.addElement(this);
  }

  private Bound(long min, long max, long numberOfElements, int flags)
  {
    IntegerType intType   = Machine.currentMachine.getIntegerCalcType();
    this.min              = LiteralMap.put(min, intType);
    this.minValue         = min;
    this.max              = LiteralMap.put(max, intType);
    this.maxValue         = max;
    this.numberOfElements = numberOfElements;
    this.flags            = flags;
  }

  /**
   * Return the expression representing the minimum of the bounds.
   */
  public final Expression getMin()
  {
    return min;
  }

  /**
   * Return the expression representing the maximum of the bounds.
   */
  public final Expression getMax()
  {
    return max;
  }

  /**
   * Return the integer value maximum value for the bound.
   * @throws scale.common.InvalidException if the maximum value for
   * the bound is not statically known
   */
  public final long getConstMax() throws scale.common.InvalidException
  {
    if ((flags & INTMAX) != 0)
      return maxValue;

    if ((flags & MAXCALC) != 0)
      throw new scale.common.InvalidException("The number of elements is not known at compile time.");

    flags |= MAXCALC;

    maxValue = scale.common.Lattice.convertToLongValue(max.getConstantValue());
    flags |= INTMAX;
    return maxValue;
  }

  /**
   * Return the integer value minimum value for the bound.
   * @throws scale.common.InvalidException if the minimum value for
   * the bound is not statically known
   */
  public final long getConstMin() throws scale.common.InvalidException
  {
    if ((flags & INTMIN) != 0)
      return minValue;

    if ((flags & MINCALC) != 0)
      throw new scale.common.InvalidException("The number of elements is not known at compile time.");

    flags |= MINCALC;

    minValue = scale.common.Lattice.convertToLongValue(min.getConstantValue());
    flags |= INTMIN;
    return minValue;
  }

  /** 
   * Return the number of elements in a bound (range) if the
   * min and max expressions are integer literals
   * @return max - min + 1 or null if either max or min aren't literals
   * @throws scale.common.InvalidException if the number of elements
   * is not statically known
   */
  public final long numberOfElements() throws scale.common.InvalidException
  {
    if ((flags & UNBOUNDED) != 0)
      throw new scale.common.InvalidException("The number of elements is not known at compile time.");

    if ((flags & INTNOE) != 0)
      return numberOfElements;

    if ((flags & NOECALC) != 0)
      throw new scale.common.InvalidException("The number of elements is not known at compile time.");

    flags |= NOECALC;

    long min = getConstMin();
    long max = getConstMax();

    numberOfElements = max - min + 1;
    flags |= INTNOE;

    return numberOfElements;
  }

  /** 
   * Return the number of bits required to represent the maximum.
   */
  public int bitSize()
  {
    try {
      long range = numberOfElements();
      if (range <= 2)
        return 1;
      if (range <= 256)
        return 8;
      if (range <= 32678)
        return 16;
    } catch (scale.common.InvalidException ex) {
    }
    return 32;
  }

  /**
   * Check if the bounds represent integer contant expressions.  If so, then
   * return true else return false.
   * @return true is the bounds are constant expresssions, otherwise false
   */
  public final boolean isConstantBounds() 
  {
    try {
      long noe = numberOfElements();
      return true;
    } catch (scale.common.InvalidException ex) {
    }
    return false;
  }
    
  public void visit(Predicate p)
  {
    p.visitBound(this);
  }

  /**
   * Specify the expression representing the minimum of the bounds.
   */
  protected final void setMin(Expression min)
  {
    assert (min != null) : "Lower bound must be supplied.";
    this.min = min;
  }

  /**
   * Specify the expression representing the maximum of the bounds.
   */
  protected final void setMax(Expression max)
  {
    assert (max != null) : "Upper bound must be supplied.";
    this.max = max;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return min;
    assert (i == 1) : "No such child " + i;
    return max;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }

  /**
   * Return true if the types are equivalent.
   */
  public boolean equivalent(Node tc)
  {
    if (!(tc instanceof Bound))
      return false;

    Bound t2 = (Bound) tc;
    if (this == t2)
      return true;

    if ((min == t2.min) && (max == t2.max))
      return true;

    if ((min == null) || (t2.min == null) || (max == null) || (t2.max == null))
      return false;

    Literal b1 = scale.common.Lattice.equal(min.getCoreType(),
                                            min.getConstantValue(),
                                            t2.min.getConstantValue());
    Literal b2 = scale.common.Lattice.equal(max.getCoreType(),
                                            max.getConstantValue(),
                                            t2.max.getConstantValue());
    try {
      if (!scale.common.Lattice.convertToBooleanValue(b1))
        return false;
      if (!scale.common.Lattice.convertToBooleanValue(b2))
        return false;
    } catch(scale.common.InvalidException e) {
      return false;
    }
    return true;
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    return "bound";
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    bounds = null;
  }

  public String toString()
  {
    if ((flags & UNBOUNDED) != 0)
      return "[...]";

    if (((flags & INTNOE) != 0) && (numberOfElements == 0))
      return "[0]";

    StringBuffer buf = new StringBuffer("[");

    try {
      buf.append(getConstMin());
    } catch(scale.common.InvalidException ex) {
      buf.append(min);
    }

    buf.append("..");

    try {
      buf.append(getConstMax());
    } catch(scale.common.InvalidException ex) {
      buf.append(max);
    }

    buf.append(']');

    return buf.toString();
  }
}
