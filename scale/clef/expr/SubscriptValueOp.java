package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class representing an array subscript operation that returns a value.
 * <p>
 * $Id: SubscriptValueOp.java,v 1.12 2007-10-04 19:58:06 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result is the value of the array element.
 */

public class SubscriptValueOp extends SubscriptOp
{
  public SubscriptValueOp(Type type, Expression array, Vector<Expression> subscripts)
  {
    super(type, array, subscripts);
  }

  public void visit(Predicate p)
  {
    p.visitSubscriptValueOp(this);
  }

  /**
   * Create a <code>SubscriptAddressOp</code> instance from this.
   */
  public Expression makeLValue()
  {
    Type               type = PointerType.create(getType());
    SubscriptAddressOp saop = new SubscriptAddressOp(type, getArray(),
                                                     getSubscripts());
    if (isFortranArray())
      saop.setFortranArray();
    return saop;
  }
}
