package scale.backend;

/** 
 * This class represents a displacement field in an instruction that 
 * is the difference between two displacements.
 * <p>
 * $Id: DiffDisplacement.java,v 1.19 2007-09-20 18:57:39 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class DiffDisplacement extends Displacement
{
  private Displacement lDisplacement;  // The left argument.
  private Displacement rDisplacement;  // The right argument.

  /**
   * Obtain a Displacement which is a difference between two other
   * Displacements.
   */
  public DiffDisplacement(Displacement lDisplacement, Displacement rDisplacement)
  {
    super();
    this.lDisplacement = lDisplacement;
    this.rDisplacement = rDisplacement;
  }

  /**
   * Return a unique displacement.  For the Alpha, each symbol
   * reference must have a unique identification number.
   * @see SymbolDisplacement
   */
  public Displacement unique()
  {
    Displacement l = lDisplacement.unique();
    Displacement r = rDisplacement.unique();

    if ((l == lDisplacement) && (r == rDisplacement))
      return this;

    return new DiffDisplacement(l, r);
  }

  /**
   * Return true if the displacement is zero.
   */
  public boolean isZero()
  {
    if (!lDisplacement.isNumeric())
      return false;
    if (!rDisplacement.isNumeric())
      return false;

    long dl = lDisplacement.getDisplacement();
    long dr = rDisplacement.getDisplacement();
    return ((dl - dr) == 0);
  }

  /**
   * Return true if the displacement can be represented as a number.
   */
  public boolean isNumeric()
  {
    return lDisplacement.isNumeric() && rDisplacement.isNumeric();
  }

  /**
   * Return the displacement.
   */
  public long getDisplacement()
  {
    return lDisplacement.getDisplacement() - rDisplacement.getDisplacement();
  }

  /**
   * Return true if the displacement is from the stack pointer.
   */
  public boolean isStack()
  {
    return lDisplacement.isStack() || rDisplacement.isStack();
  }

  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public String assembler(Assembler asm)
  {
    if (isNumeric())
      return Long.toString(getDisplacement());

    StringBuffer buf = new StringBuffer(lDisplacement.assembler(asm));
    buf.append('-');
    buf.append(rDisplacement.assembler(asm));
    return buf.toString();
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(diff ");
    buf.append(lDisplacement);
    buf.append('-');
    buf.append(rDisplacement);
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

    DiffDisplacement d = (DiffDisplacement) o;
    return (lDisplacement.equivalent(d.lDisplacement) &&
            rDisplacement.equivalent(d.rDisplacement));
  }
}
