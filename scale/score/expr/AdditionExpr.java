package scale.score.expr;

import scale.common.*;
import scale.clef.expr.*;
import scale.clef.LiteralMap;
import scale.clef.type.*; 
import scale.clef.decl.VariableDecl;
import scale.score.*;

import scale.score.dependence.AffineExpr;


/**
 * This class represents the addition function.
 * <p>
 * $Id: AdditionExpr.java,v 1.72 2007-10-04 19:58:27 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class AdditionExpr extends BinaryExpr 
{

  public AdditionExpr(Type type, Expr la, Expr ra)
  {
    super(type, la, ra);
  }

  /**
   * The expression type is the same as the type of expression
   * <code>la</code>.
   */
  public AdditionExpr(Expr la, Expr ra)
  {
    this(la.getType(), la, ra);
  }

  /**
   * This method of creating a AdditionExpr instance will return a
   * reduced expression if possible.  It will also make sure that the
   * arguments of any add generated will have the same type.
   */
  public static Expr create(Type type, Expr la, Expr ra)
  {
    if (la.isLiteralExpr()) {
      if (ra.isLiteralExpr())
        return ((LiteralExpr) la).add(type, ra);

      Expr t = la;
      la = ra;
      ra = t;
    }

    Type tat = type.getCoreType();
    if (!fpReorder && tat.isRealType())
      return new AdditionExpr(type, la, ra);

    if (tat.isAtomicType()) {
      if (la instanceof SubtractionExpr) {
        SubtractionExpr se = (SubtractionExpr) la;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (sra.equivalent(ra) && (ra.sideEffects() < SE_STATE)) {
          se.setLeftArg(null);
          se.unlinkExpression();
          ra.unlinkExpression();
          return sla;
        }
        if (sra.isLiteralExpr()) {
          se.setLeftArg(null);
          se.setRightArg(null);
          se.unlinkExpression();
          return new SubtractionExpr(type, new AdditionExpr(type, sla, ra), sra);
        }
      } else if (la instanceof NegativeExpr) {
        NegativeExpr ne  = (NegativeExpr) la;
        Expr         ra2 = ne.getArg();
        ne.setArg(null);
        ne.unlinkExpression();
        return SubtractionExpr.create(type, ra, ra2);
      } else if (la instanceof AdditionExpr) {
        AdditionExpr se = (AdditionExpr) la;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (sra.isLiteralExpr()) {
          se.setLeftArg(null);
          se.setRightArg(null);
          se.unlinkExpression();
          return new AdditionExpr(type, new AdditionExpr(type, sla, ra), sra);
        }
      }

      if (ra.isLiteralExpr()) {
        LiteralExpr ra2 = (LiteralExpr) ra;
        if (ra2.isZero() && (la.getCoreType() == type))
          return la;

        Literal lit = ra2.getLiteral().getConstantValue();
        if (lit instanceof IntLiteral) {
          long value = ((IntLiteral) lit).getLongValue();
          if (value < 0) {
            ra2.setLiteral(LiteralMap.put(-value, ra2.getType()));
            return SubtractionExpr.create(type, la, ra2);
          }
        }

        return ((LiteralExpr) ra).add(type, la);
      }

      if (ra instanceof AdditionExpr) {
        AdditionExpr se = (AdditionExpr) ra;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (sra.isLiteralExpr()) {
          se.setLeftArg(null);
          se.setRightArg(null);
          se.unlinkExpression();
          return new AdditionExpr(type, new AdditionExpr(type, la, sla), sra);
        }
      } else if (ra instanceof SubtractionExpr) {
        SubtractionExpr se = (SubtractionExpr) ra;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (sra.isLiteralExpr()) {
          se.setLeftArg(null);
          se.setRightArg(null);
          se.unlinkExpression();
          return new SubtractionExpr(type, new AdditionExpr(type, la, sla), sra);
        }
      }

      if (!type.isPointerType()) {
        Type lat = la.getCoreType();
        Type rat = ra.getCoreType();
        if (tat != lat)
          la = ConversionExpr.create(type, la, (tat.isRealType() ?
                                                CastMode.REAL :
                                                CastMode.TRUNCATE));
        if (tat != rat)
          ra = ConversionExpr.create(type, ra, (tat.isRealType() ?
                                                CastMode.REAL :
                                                CastMode.TRUNCATE));
      }
    }

    return new AdditionExpr(type, la, ra);
  }

  public Expr copy()
  {
    return new AdditionExpr(getType(), getLeftArg().copy(), getRightArg().copy());
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

  public void visit(Predicate p)
  {
    p.visitAdditionExpr(this);
  }

  public String getDisplayLabel()
  {
    return "+";
  }

  /**
   * Return the coefficient of the addition operator. 
   * @param indexVar the index variable associated with the coefficient.
   * @return the coefficient value.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {
    Type t = getCoreType();
    if (!t.isIntegerType())
      return 0;

    return (getLeftArg().findLinearCoefficient(indexVar, thisLoop) +
            getRightArg().findLinearCoefficient(indexVar, thisLoop));
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

    return AffineExpr.add(lae, rae);
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

    r = Lattice.add(getCoreType(), la, ra);

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

    return Lattice.add(getCoreType(), la, ra);
  }
  
  /**
   * Return true becuase addition is commutative.
   */
  public boolean isCommutative()
  {
    return true;
  }
  
  /**
   * Return true becuase addition is associative.
   */
  public boolean isAssociative()
  {
    return true;
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
   * Return a simplied equivalent expression.
   * For example, for
   * <pre>
   *   ((a + c1) + c2)
   * <pre>
   * where <code>c1</code> and <code>c2</code> are constants,
   * the result is
   * <pre>
   *   (a + k)
   * </pre>
   * where <code>k</code> is the sum of <code>c1</code> and <code>c2</code>.
   * This method may modify the original expression.
   * This is used in the lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr reduce()
  {
    Expr la = getLeftArg();
    Expr ra = getRightArg();
    if ((la.isLiteralExpr()) && !ra.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
    }

    if (ra.isLiteralExpr()) {
      if (((LiteralExpr) ra).isZero()) {
        setLeftArg(null);
        setRightArg(null);
        return la;
      }
      if  (la instanceof SubtractionExpr) {
        SubtractionExpr sub2 = (SubtractionExpr) la;
        Expr            ra2  = sub2.getRightArg();
        if (ra2.isLiteralExpr()) {
          Type type = getType();
          setLeftArg(null);
          setRightArg(null);
          sub2.setRightArg(null);
          sub2.setRightArg(SubtractionExpr.create(ra.getType(), ra2, ra));
          return sub2;
        }
      } else if (la instanceof AdditionExpr) {
        AdditionExpr add2 = (AdditionExpr) la;
        Expr         ra2  = add2.getRightArg();
        if (ra2.isLiteralExpr()) {
          setLeftArg(null);
          setRightArg(null);
          add2.setRightArg(null);
          add2.setRightArg(AdditionExpr.create(ra.getType(), ra, ra2));
          return add2;
        }
      }
    } else if (ra instanceof AdditionExpr) {
      AdditionExpr add2 = (AdditionExpr) ra;
      Expr         ra2  = add2.getRightArg();
      Expr         la2  = add2.getLeftArg();
      if (ra2.isLiteralExpr()) {
        setLeftArg(null);
        setRightArg(null);
        add2.setLeftArg(null);
        add2.setRightArg(null);
        setLeftArg(AdditionExpr.create(getType(), la, la2));
        setRightArg(ra2);
        return this;
      }
      if (la2.isLiteralExpr()) {
        setLeftArg(null);
        setRightArg(null);
        add2.setLeftArg(null);
        add2.setRightArg(null);
        setLeftArg(AdditionExpr.create(getType(), la, ra2));
        setRightArg(la2);
        return this;
      }
    } else if (ra instanceof SubtractionExpr) {
      SubtractionExpr sub2 = (SubtractionExpr) ra;
      Expr            ra2  = sub2.getRightArg();
      Expr            la2  = sub2.getLeftArg();
      if (ra2.isLiteralExpr()) {
        setLeftArg(null);
        setRightArg(null);
        sub2.setLeftArg(null);
        sub2.setRightArg(null);
        Expr nra = AdditionExpr.create(getType(), la, la2);

        return SubtractionExpr.create(getType(), nra, ra2);
      }
    }

    return this;
  }
}
