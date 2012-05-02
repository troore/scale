package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents the va_copy(va_list src, va_list dest) C construct in the Clef AST.
 * <p>
 * $Id: VaCopyOp.java,v 1.4 2005-03-24 13:57:05 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class VaCopyOp extends AssignmentOp
{
  public VaCopyOp(Type type, Expression dst, Expression src)
  {
    super(type, dst, src);
  }

  public void visit(Predicate p)
  {
    p.visitVaCopyOp(this);
  }
}
