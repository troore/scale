package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This is the base class for all modify and replace operations such as <tt>x++</tt>.
 * <p>
 * $Id: IncrementOp.java,v 1.23 2005-03-17 14:11:32 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class IncrementOp extends MonadicOp 
{
  public IncrementOp(Type type, Expression e)
  {
    super(type, e);
  }

  public boolean isSimpleOp()
  {
    return false;
  }

  public void visit(Predicate p)
  {
    p.visitIncrementOp(this);
  }
}
