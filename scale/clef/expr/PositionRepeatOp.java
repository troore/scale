package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * The PositionRepeatOp class specifies a repeat count for the next
 * constant in an aggregation.
 * <p>
 * $Id: PositionRepeatOp.java,v 1.1 2005-03-14 22:28:57 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class PositionRepeatOp extends PositionOp
{
  private int count;

  public PositionRepeatOp(int count)
  {
    super();
    this.count = count;
  }

  /**
   * Return the repeat count.
   */
  public final int getCount()
  {
    return count;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;

    PositionRepeatOp op = (PositionRepeatOp) exp;
    return (count == op.count);
  }

  public String toString()
  {
    return "(pr " + count + ")";
  }
}
