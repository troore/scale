package scale.clef.type;

import scale.common.*;
import scale.clef.*;

/**
 * This class represents the void type in C and is used to represent the absence of a type.
 * <p>
 * $Id: VoidType.java,v 1.39 2007-03-21 13:32:01 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class VoidType extends Type 
{
  /**
   * There is only one void type.
   */
  public static final VoidType type = new VoidType();

  private VoidType()
  {
  }

  /**
   * Return true if type represents no type.
   */
  public boolean isVoidType()
  {
    return true;
  }

  public final VoidType returnVoidType()
  {
    return this;
  }

  public String toString()
  {
    return "<void>";
  }

  public String toStringSpecial()
  {
    return "";
  }

  public void visit(Predicate p)
  {
    p.visitVoidType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitVoidType(this);
  }

  /**
   * Return true because all void types are equivalent.
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    return (tc == type);
  }

  /**
   * Return the number of elements represented by this type
   * which is 0 for void.
   */
  public long numberOfElements()
  {
    return 0;
  }

  /**
   * Calculate how many addressable memory units are needed to represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of addressable memory units required to represent this type
   */
  public long memorySize(Machine machine)
  {
    return 1;
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    return 1;
  }
}
