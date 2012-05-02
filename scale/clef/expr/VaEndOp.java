package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents the va_end(va_list) C construct in the Clef AST.
 * <p>
 * $Id: VaEndOp.java,v 1.21 2005-02-07 21:28:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class VaEndOp extends VarArgOp 
{
  public VaEndOp(Type type, Expression va_list)
  {
    super(type, va_list);
  }

  public void visit(Predicate p)
  {
    p.visitVaEndOp(this);
  }
}
