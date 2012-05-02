package scale.clef.expr;

import scale.common.Vector;
import java.math.*;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents the exponentiation operator.
 * <p>
 * $Id: ExponentiationOp.java,v 1.29 2005-02-07 21:27:57 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ExponentiationOp extends DyadicOp 
{
  public ExponentiationOp(Type type, Expression e1, Expression e2)
  {
    super(type, e1, e2);
  }

  public void visit(Predicate p)
  {
    p.visitExponentiationOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr1().getConstantValue();
    Literal ra = getExpr2().getConstantValue();
    return scale.common.Lattice.power(getCoreType(), la, ra);
  }
}
