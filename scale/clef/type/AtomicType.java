package scale.clef.type;

import scale.common.*;
import scale.clef.*;

/**
 * This class represents types directly supported by (most) hardware
 * (e.g., integers, reals, and pointers).
 * <p>
 * $Id: AtomicType.java,v 1.30 2007-03-21 13:31:55 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * It is assumed that a variable of atomic type can be placed in one
 * or two registers.
 */

public abstract class AtomicType extends Type
{
  public void visit(Predicate p)
  {
    p.visitAtomicType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitAtomicType(this);
  }

  /**
   * Return true if type represents a scaler value.
   */
  public boolean isAtomicType()
  {
    return true;
  }

  public final AtomicType returnAtomicType()
  {
    return this;
  }

  /**
   * Calculate how many addressable memory units are needed to represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of addressable memory units required to represent this type
   */
  public long memorySize(Machine machine)
  {
    return machine.addressableMemoryUnits(bitSize());
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    return machine.alignData(machine.addressableMemoryUnits(bitSize()));
  }

  /**
   * Return the number of bits required to represent the type.
   */
  public abstract int bitSize();
}
