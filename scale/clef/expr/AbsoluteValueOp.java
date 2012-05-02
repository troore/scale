package scale.clef.expr;

import scale.common.Vector;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * The AbsoluteValueOp class represents the absolute value operation.
 * <p>
 * $Id: AbsoluteValueOp.java,v 1.28 2005-02-07 21:27:53 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class AbsoluteValueOp extends MonadicOp 
{
  public AbsoluteValueOp(Type type, Expression arg)
  {
    super(type, arg);
  }

  public void visit(Predicate p)
  {
    p.visitAbsoluteValueOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr().getConstantValue();
    return scale.common.Lattice.abs(getCoreType(), la);
  }
}
