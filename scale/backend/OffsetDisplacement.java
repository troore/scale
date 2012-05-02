package scale.backend;

/** 
 * This class represents a displacement field in an instruction that 
 * is offset from another displacement.
 * <p>
 * $Id: OffsetDisplacement.java,v 1.23 2007-09-20 18:57:40 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This displacement is offset from another displacement.  When the
 * base displacement is adjusted, this displacement is also adjusted.
 */

public class OffsetDisplacement extends Displacement
{
  private Displacement displacement; // The base displacement.
  private long         offset;       // The offset from the base.

  /**
   * Obtain a Displacement with an offset from the specified Displacement.
   */
  public OffsetDisplacement(Displacement displacement, long offset)
  {
    super();
    this.displacement = displacement;
    this.offset = offset;
  }

  /**
   * Return the base of the offset displacement.
   */
  public Displacement getBase()
  {
    return displacement;
  }

  /**
   * Return true if the displacement is zero.
   */
  public boolean isZero()
  {
    return ((offset == 0) && displacement.isZero());
  }

  /**
   * Return a unique displacement.  For the Alpha, each symbol
   * reference must have a unique identification number.
   * @see SymbolDisplacement
   */
  public Displacement unique()
  {
    Displacement d = displacement.unique();
    if (d == displacement)
      return this;
    return new OffsetDisplacement(d, offset);
  }

  /**
   * Return true if the displacement can be represented as an integer.
   */
  public boolean isNumeric()
  {
    return displacement.isNumeric();
  }

  /**
   * Return the displacement.
   */
  public long getDisplacement()
  {
    return displacement.getDisplacement() + offset;
  }

  /**
   * Return true if the displacement is from the stack pointer.
   */
  public boolean isStack()
  {
    return displacement.isStack();
  }

  /**
   * Adjust the displacement by the specified value.
   */
  public void adjust(int adjustment)
  {
  }

  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public String assembler(Assembler asm)
  {
    if (displacement.isZero())
      return Long.toString(offset);
    if (offset == 0)
      return displacement.assembler(asm);

    StringBuffer buf = new StringBuffer(displacement.assembler(asm));
    buf.append('+');
    buf.append(offset);
    return buf.toString();
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(offset ");
    buf.append(displacement);
    buf.append('+');
    buf.append(offset);
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return true if the displacements are equivalent.
   */
  public boolean equivalent(Object o)
  {
    if (!super.equivalent(o))
      return false;

    OffsetDisplacement d = (OffsetDisplacement) o;
    if (offset != d.offset)
      return false;

    return displacement.equivalent(d.displacement);
  }
}
