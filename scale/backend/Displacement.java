package scale.backend;

import scale.common.*;

/** 
 * This class represents a displacement field in an instruction.
 * <p>
 * $Id: Displacement.java,v 1.22 2007-09-20 18:57:39 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * It is the base class for displacements that are represented symbolicly
 * or as an expression.
 */

public abstract class Displacement
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.Displacement", stats);
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  public Displacement()
  {
    createdCount++;
  }

  /**
   * Adjust the displacement by the specified value.
   */
  public void adjust(int adjustment)
  {
    throw new scale.common.InternalError("Not a numeric displacement - " + this);
  }

  /**
   * Obtain a Displacement with an offset from this Displacement.
   */
  public OffsetDisplacement offset(long offset)
  {
    return new OffsetDisplacement(this, offset);
  }

  /**
   * Return the base of the displacement.
   */
  public Displacement getBase()
  {
    return this;
  }

  /**
   * Return the displacement.
   */
  public long getDisplacement()
  {
    throw new scale.common.InternalError("Not a numeric displacement - " + this);
  }

  /**
   * Return true if the displacement is from the stack pointer.
   */
  public boolean isStack()
  {
    return false;
  }

  /**
   * Return true if the displacement is zero.
   */
  public boolean isZero()
  {
    return false;
  }

  /**
   * Return true if the displacement can be represented as an integer.
   */
  public boolean isNumeric()
  {
    return false;
  }
  
  /**
   * Return true if the displacement is from a symbol.
   */
  public boolean isSymbol()
  {
    return false;
  }

  /**
   * Return a unique displacement.  For the Alpha, each symbol
   * reference must have a unique identification number.
   * @see SymbolDisplacement
   */
  public abstract Displacement unique();

  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public abstract String assembler(Assembler asm);

  /**
   * Return true if the displacements are equivalent.
   */
  public boolean equivalent(Object o)
  {
    return (o.getClass() == this.getClass());
  }
}
