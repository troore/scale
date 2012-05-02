package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the <tt>x--</t> operator.
 * <p>
 * $Id: PostDecrementOp.java,v 1.25 2005-02-07 21:28:01 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class PostDecrementOp extends IncrementOp 
{
  public PostDecrementOp(Type type, Expression e)
  {
    super(type, e);
  }

  public void visit(Predicate p)
  {
    p.visitPostDecrementOp(this);
  }
}
