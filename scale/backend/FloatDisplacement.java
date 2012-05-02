package scale.backend;

/** 
 * This is a simple displacement where the displacement is a known
 * floating point value.
 * <p>
 * $Id: FloatDisplacement.java,v 1.3 2007-09-20 18:57:40 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class FloatDisplacement extends Displacement
{
  private double displacement; // The displacement value.

  /**
   * @param displacement is an integer value.
   */
  public FloatDisplacement(double displacement)
  {
    this.displacement = displacement;
  }

  /**
   * Return the displacement using a representation of the specified
   * floating-point value according to the IEEE 754 floating-point
   * "double format" bit layout.
   * @see java.lang.Double#doubleToLongBits
   */
  public long getDisplacement()
  {
    return Double.doubleToLongBits(displacement);
  }

  /**
   * Return the displacement as the floating point value.
   */
  public double getDoubleDisplacement()
  {
    return displacement;
  }

  /**
   * Adjust the displacement by the specified value.
   */
  public void adjust(int adjustment)
  {
    displacement += adjustment;
  }

  /**
   * Return true if the displacement is zero.
   */
  public boolean isZero()
  {
    return (displacement == 0.0);
  }

  /**
   * Return true if the displacement can be represented as an integer.
   */
  public boolean isNumeric()
  {
    return true;
  }

  /**
   * Returns <code>this</code>.
   * @see SymbolDisplacement
   */
  public Displacement unique()
  {
    return this;
  }

  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public String assembler(Assembler asm)
  {
    return Long.toString(Double.doubleToLongBits(displacement));
  }

  public String toString()
  {
    return "(float " + displacement + ")";
  }

  /**
   * Return true if the displacements are equivalent.
   */
  public boolean equivalent(Object o)
  {
    if (!super.equivalent(o))
      return false;

    FloatDisplacement d = (FloatDisplacement) o;
    return (displacement == d.displacement);
  }
}
