package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents <code> x ^= y</code>.
 * <p>
 * $Id: BitXorAssignmentOp.java,v 1.24 2006-12-18 21:36:48 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Note that <code>x ^= y</code> is not equivalent to <code>x = x ^
 * y</code> when <code>x</code> is a an expression with side effects.
 */

public class BitXorAssignmentOp extends CompoundAssignmentOp 
{
  public BitXorAssignmentOp(Type type, Type calcType, Expression lhs, Expression ra)
  {
    super(type, calcType, lhs, ra);
  }

  public BitXorAssignmentOp(Type type, Expression lhs, Expression ra)
  {
    this(type, type, lhs, ra);
  }

  public void visit(Predicate p)
  {
    p.visitBitXorAssignmentOp(this);
  }
}
