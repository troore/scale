package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the bit-complement operation.
 * <p>
 * $Id: BitComplementOp.java,v 1.23 2005-02-07 21:27:55 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class BitComplementOp extends MonadicOp 
{
  public BitComplementOp(Type type, Expression arg)
  {
    super(type, arg);
  }

  public void visit(Predicate p)
  {
    p.visitBitComplementOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr().getConstantValue();
    return scale.common.Lattice.bitComplement(getCoreType(), la);
  }
}
