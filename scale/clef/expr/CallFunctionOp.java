package scale.clef.expr;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * A class which represents a call to a function.
 * <p>
 * $Id: CallFunctionOp.java,v 1.27 2005-02-07 21:27:55 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result type should be the the return type of the called function.
*/

public class CallFunctionOp extends CallOp
{
  public CallFunctionOp(Type returnType, Expression routine, Vector<Expression> argList)
  {
    super(returnType, routine, argList);
  }

  public void visit(Predicate p)
  {
    p.visitCallFunctionOp(this);
  }
}
