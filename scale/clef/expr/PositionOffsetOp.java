package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * The PositionOffsetOp class represents a position in an aggregation
 * as an offset, in bytes, from the start of the aggregation.
 * <p>
 * $Id: PositionOffsetOp.java,v 1.1 2005-03-14 22:28:57 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class PositionOffsetOp extends PositionOp
{
  private long offset;

  public PositionOffsetOp(long offset)
  {
    super();
    this.offset = offset;
  }

  /**
   * Return the offset, in bytes, from the start of the aggregation.
   */
  public final long getOffset()
  {
    return offset;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;

    PositionOffsetOp op = (PositionOffsetOp) exp;
    return (offset == op.offset);
  }

  public String toString()
  {
    return "(po " + offset + ")";
  }
}
