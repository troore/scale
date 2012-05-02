package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * The SeriesOp class represents the C-style comma operator.
 * <p>
 * $Id: SeriesOp.java,v 1.27 2005-02-07 21:28:02 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class SeriesOp extends DyadicOp
{
  public SeriesOp(Type type, Expression e1, Expression e2)
  {
    super(type, e1, e2);
  }

  public SeriesOp(Expression e1, Expression e2)
  {
    this(e2.getType(), e1, e2);
  }

  public void visit(Predicate p)
  {
    p.visitSeriesOp(this);
  }
}
