package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * Un-used.
 * <p>
 * $Id: ModulusOp.java,v 1.23 2005-02-07 21:27:59 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
*/

public class ModulusOp extends DyadicOp
{
  public ModulusOp(Type type, Expression e1, Expression e2)
  {
    super(type, e1, e2);
  }

  public void visit(Predicate p)
  {
    p.visitModulusOp(this);
  }
}
