package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents an expression inside a set of parens.
 * <p>
 * $Id: ParenthesesOp.java,v 1.29 2005-02-07 21:28:00 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class ParenthesesOp extends MonadicOp
{
  public ParenthesesOp(Type type, Expression e)
  {
    super(type, e);
  }

  public void visit(Predicate p)
  {
    p.visitParenthesesOp(this);
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
