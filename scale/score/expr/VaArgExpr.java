package scale.score.expr;

import scale.score.*;
import scale.clef.type.*;

/**
 * A class which represents the va_arg(va_list, type) C construct.
 * <p>
 * $Id: VaArgExpr.java,v 1.19 2005-02-07 21:28:49 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class VaArgExpr extends VarArgExpr 
{
  public VaArgExpr(Type t, Expr va_list)
  {
    super(t, va_list);
  }

  public Expr copy()
  {
    return new VaArgExpr(getType(), getVaList().copy());
  }

  public void visit(Predicate p)
  {
    p.visitVaArgExpr(this);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5;
  }
}
