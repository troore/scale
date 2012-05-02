package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the unary <tt>-</tt> operator.
 * <p>
 * $Id: NegativeOp.java,v 1.28 2005-10-03 21:11:41 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class NegativeOp extends MonadicOp 
{
  public NegativeOp(Type type, Expression e)
  {
    super(type, e);
  }

  public void visit(Predicate p)
  {
    p.visitNegativeOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr().getConstantValue();
    return scale.common.Lattice.negate(getCoreType(), la);
  }
}
