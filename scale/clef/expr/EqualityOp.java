package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the test for equality.
 * <p>
 * $Id: EqualityOp.java,v 1.28 2005-02-07 21:27:56 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class EqualityOp extends DyadicOp 
{
  public EqualityOp(Type type, Expression e1, Expression e2)
  {
    super(type, e1, e2);
  }

  public EqualityOp(Expression e1, Expression e2)
  {
    this(BooleanType.type, e1, e2);
  }

  public void visit(Predicate p)
  {
    p.visitEqualityOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr1().getConstantValue();
    Literal ra = getExpr2().getConstantValue();
    return scale.common.Lattice.equal(getCoreType(), la, ra);
  }

  /**
   *  Return true if the result of the expression is either true (1) or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return true;
  }
}
