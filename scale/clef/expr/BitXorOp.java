package scale.clef.expr;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the bit-exclusive or operation.
 * <p>
 * $Id: BitXorOp.java,v 1.24 2005-08-31 18:19:10 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class BitXorOp extends DyadicOp
{
  public BitXorOp(Type type, Expression e1, Expression e2)
  {
    super(type, e1, e2);
    setExpr2(e2);
  }

  public void visit(Predicate p)
  {
    p.visitBitXorOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr1().getConstantValue();
    Literal ra = getExpr2().getConstantValue();
    return scale.common.Lattice.bitXor(getCoreType(), la, ra);
  }
}
