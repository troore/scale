package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class is used to represents the C <i>conditional</i> operator.
 * <p>
 * $Id: ExpressionIfOp.java,v 1.31 2005-09-09 15:04:44 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents the C <code>?:</code> operator.
 * The result type is the type of the second expression (the types of 
 * the second and third expressions must be the same).
 */

public class ExpressionIfOp extends TernaryOp
{
  public ExpressionIfOp(Type type, Expression test, Expression trueExpr, Expression falseExpr)
  {
    super(type, test, trueExpr, falseExpr);
  }

  public void visit(Predicate p)
  {
    p.visitExpressionIfOp(this);
  }

  public boolean isSimpleOp()
  {
    return false;
  }

  /**
   *  Return true if the result of the expression is either true (1) or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    Expression te = getExpr2();
    if (!(te instanceof Literal))
      return false;
    Literal tl = ((Literal) te).getConstantValue();

    Expression fe = getExpr3();
    if (!(fe instanceof Literal))
      return false;
    Literal fl = ((Literal) fe).getConstantValue();

    return ((fl.isZero() && tl.isOne()) || (tl.isZero() && fl.isOne()));
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getExpr1().getConstantValue();
    if (la.isZero())
      return getExpr3().getConstantValue();
    if ((la == Lattice.Bot) || (la == Lattice.Top))
      return Lattice.Bot;
    return getExpr2().getConstantValue();
  }
}
