package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.expr.CastMode;
import scale.clef.expr.TypeConversionOp;
import scale.clef.type.Type;
import scale.score.*;

/**
 * This class is used to represents some cases of the C <i>conditional</i> operator.
 * <p>
 * $Id: ConditionalExpr.java,v 1.15 2007-04-27 18:04:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents the C <code>?:</code> operator.
 * Note - the semantics of the <code>ConditionalExp</code>r are not the same as the
 * {@link scale.clef.expr.ExpressionIfOp  ExpressionIfOp}.  For <code>ExpressionIfOp</code>,
 * one of the conditional expression will not be evaluated.  For <code>ConditionExpr</code>,
 * both expressions are evaluated.
 */
public class ConditionalExpr extends TernaryExpr
{
  public ConditionalExpr(Type type, Expr predicate, Expr trueExpr, Expr falseExpr)
  {
    super(type, predicate, trueExpr, falseExpr);
  }

  public static Expr create(Type type, Expr predicate, Expr trueExpr, Expr falseExpr)
  {
    if (predicate.hasTrueFalseResult() && trueExpr.isLiteralExpr() && falseExpr.isLiteralExpr()) {
      LiteralExpr la  = (LiteralExpr) trueExpr;
      LiteralExpr ra  = (LiteralExpr) falseExpr;
      Expr        res = null;

      if (la.isOne() && ra.isZero())
        res = predicate;
      else if (la.isZero() && ra.isOne()) {
        if (predicate.isMatchExpr())
          res = ((MatchExpr) predicate).complement();
        else
          res = new NotExpr(predicate.getType(), predicate);
      }

      if (res != null) {
        CastMode cr = TypeConversionOp.determineCast(type, res.getType());
        if (cr == CastMode.NONE)
          return res;
        return ConversionExpr.create(type, res, cr);
      }
    }

    return new ConditionalExpr(type, predicate, trueExpr, falseExpr);
  }

  public Expr copy()
  {
    return new ConditionalExpr(getType(), getLA().copy(), getMA().copy(), getRA().copy());
  }

  /**
   * Return the test expression.
   */
  public final Expr getTest()
  {
    return getLA();
  }

  /**
   * Return the true expression.
   */
  public final Expr getTrueExpr()
  {
    return getMA();
  }

  /**
   * Return the false expression.
   */
  public final Expr getFalseExpr()
  {
    return getRA();
  }

  public void visit(Predicate p)
  {
    p.visitConditionalExpr(this);
  }

  public String getDisplayLabel()
  {
    return "?:";
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    int costt = getTrueExpr().executionCostEstimate();
    int costf = getFalseExpr().executionCostEstimate();
    if (costf > costt)
      costt = costf;
    return getTest().executionCostEstimate() + costt;
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return r;


    Literal la = getLA().getConstantValue(cvMap);
    if (Lattice.isZero(la))
      r = getRA().getConstantValue(cvMap);
    else if (Lattice.isNonZero(la))
      r = getMA().getConstantValue(cvMap);
    else {
      Literal ma = getMA().getConstantValue(cvMap);
      Literal ra = getRA().getConstantValue(cvMap);
      if (ma.equivalent(ra) && (ma != Lattice.Bot) && (ma != Lattice.Top))
        r = ma;
      else
        r = Lattice.Bot;
    }

    cvMap.put(this, r);
    return r;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la = getLA().getConstantValue();

    if (Lattice.isZero(la))
      return getRA().getConstantValue();

    if (Lattice.isNonZero(la))
      return getMA().getConstantValue();

    Literal ma = getMA().getConstantValue();
    Literal ra = getRA().getConstantValue();
    if (ma.equivalent(ra) && (ma != Lattice.Bot) && (ma != Lattice.Top))
      return ma;
    return Lattice.Bot;
  }

  /**
   *  Return true if the result of the expression is either true (1) or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return getTrueExpr().hasTrueFalseResult() && getFalseExpr().hasTrueFalseResult();
  }
}
