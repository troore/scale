package scale.backend;

/** 
 * This class represents a displacement field in an instruction when
 * the displacement refers to an offset on the stack.
 * <p>
 * $Id: StackDisplacement.java,v 1.18 2007-09-20 18:57:40 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A separate class is used for this so that instanceof may be used to
 * distinguish between a normal displacement and a stack displacement.
 * Also, stack displacements must often be changed after the code for
 * a routine is generated to reflect additional stack entries for
 * spills, etc.
 */

public class StackDisplacement extends Displacement
{
  private long offset; // The stack offset.

  public StackDisplacement(long offset)
  {
    super();
    this.offset = offset;
  }

  /**
   * Return true if the displacement can be represented as a number.
   */
  public boolean isNumeric()
  {
    return true;
  }

  /**
   * Return the displacement.
   */
  public long getDisplacement()
  {
    return offset;
  }

  /**
   * Return true if the displacement is from the stack pointer.
   */
  public boolean isStack()
  {
    return true;
  }

  /**
   * Return true if the displacement is zero.
   */
  public boolean isZero()
  {
    return (offset == 0);
  }

  /**
   * Adjust the displacement by the specified value.
   */
  public void adjust(int adjustment)
  {
    offset += adjustment;
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
    if (offset == 0)
      return "";
    return Long.toString(offset);
  }

  public String toString()
  {
    return "(Stk " + offset + ")";
  }

  /**
   * Return true if the displacements are equivalent.
   */
  public boolean equivalent(Object o)
  {
    if (!super.equivalent(o))
      return false;

    StackDisplacement d = (StackDisplacement) o;
    return (offset == d.offset);
  }
}


