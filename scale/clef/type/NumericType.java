package scale.clef.type;

import scale.clef.*;

/**
 * This is the base class for all numeric types.
 * <p>
 * $Id: NumericType.java,v 1.28 2007-03-21 13:31:57 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class NumericType extends AtomicType
{
  public void visit(Predicate p)
  {
    p.visitNumericType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitNumericType(this);
  }

  /**
   * Return true if type represents a numeric value.
   */
  public boolean isNumericType()
  {
    return true;
  }

  public final NumericType returnNumericType()
  {
    return this;
  }
}
