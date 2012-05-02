package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * The PositionIndexOp class represents a position in an aggregation
 * as an single dimension array index from the start of the aggregation.
 * <p>
 * $Id: PositionIndexOp.java,v 1.2 2006-09-10 19:35:58 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class PositionIndexOp extends PositionOp
{
  private long index;

  public PositionIndexOp(long index)
  {
    super();
    this.index = index;
  }

  /**
   * Return the single dimension array index from the start of the aggregation.
   */
  public final long getIndex()
  {
    return index;
  }

  /**
   * Set the single dimension array index from the start of the aggregation.
   */
  public final void setIndex(long index)
  {
    this.index = index;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;

    PositionIndexOp op = (PositionIndexOp) exp;
    return (index == op.index);
  }

  public String toString()
  {
    return "(pi " + index + ")";
  }
}
