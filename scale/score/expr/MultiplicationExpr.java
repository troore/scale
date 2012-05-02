package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.VariableDecl;
import scale.score.*;
import scale.score.dependence.AffineExpr;


/**
 * This class represents the multiplication operation.
 * <p>
 * $Id: MultiplicationExpr.java,v 1.53 2007-10-04 19:58:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class MultiplicationExpr extends BinaryExpr
{
  public MultiplicationExpr(Type t, Expr la, Expr ra)
  {
    super(t, la, ra);
  }

  /**
   * This method of creating a MultiplicationExpr instance will return a
   * simpler expression if possible.
   */
  public static Expr create(Type type, Expr la, Expr ra)
  {
    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
    }

    if (ra.isLiteralExpr())
      return ((LiteralExpr) ra).multiply(type, la);

    return new MultiplicationExpr(type, la, ra);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | SE_OVERFLOW;
  }

  public Expr copy()
  {
    return new MultiplicationExpr(getType(), getLeftArg().copy(), getRightArg().copy());
  }

  public void visit(Predicate p)
  {
    p.visitMultiplicationExpr(this);
  }

  public String getDisplayLabel()
  {
    return "*";
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

    associativeSwapOperands();

    Literal la = getLeftArg().getConstantValue(cvMap);
    Literal ra = getRightArg().getConstantValue(cvMap);

    r = Lattice.multiply(getCoreType(), la, ra);

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
    associativeSwapOperands();

    Literal la = getLeftArg().getConstantValue();
    Literal ra = getRightArg().getConstantValue();

    return Lattice.multiply(getCoreType(), la, ra);
  }

  /**
   * Return true if this expression is commutative.
   */
  public boolean isCommutative()
  {
    return true;
  }
  
  /**
   * Return true becuase multiplication is associative.
   */
  public boolean isAssociative()
  {
    return true;
  }

  public boolean isLeftDistributive()
  {
    Expr la = getLeftArg();
    return ((la instanceof AdditionExpr) || (la instanceof SubtractionExpr));   
  }
  
  public boolean isRightDistributive()
  {
    Expr ra = getRightArg();
    return ((ra instanceof AdditionExpr) || (ra instanceof SubtractionExpr));   
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return getLeftArg().isSimpleExpr() && getRightArg().isSimpleExpr();
  }

  /**
   * Return the coefficient of the mulitiplication operator. 
   * @param indexVar the index variable associated with the coefficient.
   * @return the coefficient value.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {
    Type t = getCoreType();
    if (!t.isIntegerType())
      return 0;

    return (getOperand(0).findLinearCoefficient(indexVar, thisLoop) *
            getOperand(1).findLinearCoefficient(indexVar, thisLoop));
  }

  protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>          affines,
                                               scale.score.chords.LoopHeaderChord thisLoop)
  {
    AffineExpr lae = getLeftArg().getAffineExpr(affines, thisLoop);
    if (lae == null)
      return AffineExpr.notAffine;

    AffineExpr rae = getRightArg().getAffineExpr(affines, thisLoop);
    if (lae == null)
      return AffineExpr.notAffine;

    return AffineExpr.multiply(lae, rae);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 2 + super.executionCostEstimate();
  }
}
