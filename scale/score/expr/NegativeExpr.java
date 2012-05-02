package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.type.Type;
import scale.clef.decl.VariableDecl;
import scale.clef.LiteralMap;
import scale.score.*;

import scale.score.dependence.AffineExpr;


/**
 * This class represents the nagate operation (e.g., two's complement).
 * <p>
 * $Id: NegativeExpr.java,v 1.35 2007-10-04 19:58:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class NegativeExpr extends UnaryExpr
{
  public NegativeExpr(Type t, Expr e1)
  {
    super(t, e1);
  }

  /**
   * The expression type is the same as the type of expression e1.
   */
  public NegativeExpr(Expr e1)
  {
    this(e1.getType(), e1);
  }

  public static Expr create(Type type, Expr exp)
  {
    Type tat = type.getCoreType();
    if (tat.isAtomicType()) {
      if (exp instanceof SubtractionExpr) {
        SubtractionExpr se = (SubtractionExpr) exp;
        se.swapOperands();
        return se;
      }

      if (exp instanceof NegativeExpr) {
        NegativeExpr ne  = (NegativeExpr) exp;
        Expr         ra2 = ne.getArg();
        ne.setArg(null);
        ne.unlinkExpression();
        return ra2;
      }

      if (exp.isLiteralExpr()) {
        Literal lit = ((LiteralExpr) exp).getLiteral().getConstantValue();
        if (lit instanceof IntLiteral)
          return new LiteralExpr(LiteralMap.put(-((IntLiteral) lit).getLongValue(), type));
        if (lit instanceof FloatLiteral)
          return new LiteralExpr(LiteralMap.put(-((FloatLiteral) lit).getDoubleValue(), type));
      }
    }

    return new NegativeExpr(type, exp);
  }

  public Expr copy()
  {
    return new NegativeExpr(getType(), getArg().copy());
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | (getCoreType().isRealType() ? SE_NONE : SE_OVERFLOW);
  }

  public void visit(Predicate p)
  {
    p.visitNegativeExpr(this);
  }

  public String getDisplayLabel()
  {
    return "-";
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

    Literal la = getArg().getConstantValue(cvMap);
    r = Lattice.negate(getCoreType(), la);

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
    Literal la = getArg().getConstantValue();
    return Lattice.negate(getCoreType(), la);
  }

  /**
   * Determine the coefficent of a linear expression. 
   * @param indexVar the index variable associated with the coefficient.
   * @return the coefficient value.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {   
    return -getArg().findLinearCoefficient(indexVar, thisLoop);
  }

  protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>          affines,
                                               scale.score.chords.LoopHeaderChord thisLoop)
  {
    AffineExpr lae = getArg().getAffineExpr(affines, thisLoop);
    if (lae == null)
      return AffineExpr.notAffine;

    return AffineExpr.negate(lae);
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return getArg().isSimpleExpr();
  }
}










