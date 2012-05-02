package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class representes the unary plus operator.
 * <p>
 * $Id: PositiveOp.java,v 1.25 2005-02-07 21:28:01 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The unary plus operator is basically a no-op (does not do anything).
 * However, we retain the knowledge that the programmer specified it.
 */

public class PositiveOp extends MonadicOp 
{
  public PositiveOp(Type type, Expression e)
  {
    super(type, e);
  }

  public void visit(Predicate p)
  {
    p.visitPositiveOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return getExpr().getConstantValue();
  }
}
