package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents a logical <b>or</b> operation with conditional
 * evaluation of its second operand.
 * <p>
 * $Id: OrConditionalOp.java,v 1.31 2005-03-17 14:11:32 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class OrConditionalOp extends DyadicOp 
{
  public OrConditionalOp(Type type, Expression e1, Expression e2)
  {
    super(type, e1, e2);
  }

  public void visit(Predicate p)
  {
    p.visitOrConditionalOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr1().getConstantValue();
    Literal ra = getExpr2().getConstantValue();
    return scale.common.Lattice.orCond(getCoreType(), la, ra);
  }

  public boolean isSimpleOp()
  {
    return false;
  }

  /**
   *  Return true if the result of the expression is either true (1) or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return true;
  }
}
