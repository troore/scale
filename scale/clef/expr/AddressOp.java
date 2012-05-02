package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the operation of obtaining the address of some thing.
 * <p>
 * $Id: AddressOp.java,v 1.32 2005-09-09 15:13:53 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * We take the address of an expression.
 */
public class AddressOp extends MonadicOp
{
  public AddressOp(Type type, Expression e)
  {
    super(type, e);
  }

  public void visit(Predicate p)
  {
    p.visitAddressOp(this);
  }
}
