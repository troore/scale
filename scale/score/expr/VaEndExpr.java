package scale.score.expr;

import scale.score.*;
import scale.clef.type.*;

/**
 * A class which represents the va_end(va_list) C construct in Score.
 * <p>
 * $Id: VaEndExpr.java,v 1.19 2005-02-07 21:28:49 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class VaEndExpr extends VarArgExpr 
{
  public VaEndExpr(Expr va_list)
  {
    super(VoidType.type, va_list);
  }

  public Expr copy()
  {
    return new VaEndExpr(getVaList().copy());
  }

  public void visit(Predicate p)
  {
    p.visitVaEndExpr(this);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 0;
  }
}
