package scale.score.expr;

import scale.score.*;
import scale.clef.decl.*;
import scale.clef.type.*;

/**
 * A class which represents the va_start(va_list, parmN) C construct
 * in Score.
 * <p>
 * $Id: VaStartExpr.java,v 1.25 2007-01-05 19:06:46 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class VaStartExpr extends VarArgExpr
{
  /**
   * The reference parameter of the calling function.
   */
  private FormalDecl parmN; 

  public VaStartExpr(Expr va_list, FormalDecl pn)
  {
    super(VoidType.type, va_list);
    parmN = pn;
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the equivalent() method of the derived
   * classes.
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    VaStartExpr o = (VaStartExpr) exp;
    return (parmN == o.parmN);
  }

  public Expr copy()
  {
    return new VaStartExpr(getVaList().copy(), parmN);
  }

  public void visit(Predicate p)
  {
    p.visitVaStartExpr(this);
  }

  /**
   * Return the reference parameter of the calling function.
   */
  public final FormalDecl getParmN()
  {
    return parmN;
  }

  /**
   * Specify the reference parameter of the calling function.
   */
  protected final void setParmN(FormalDecl pn)
  {
    parmN = pn;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 2;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(parmN);
    return buf.toString();
  }
}
