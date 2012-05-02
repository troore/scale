package scale.score.expr;

import scale.common.*;
import scale.clef.expr.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.VariableDecl;
import scale.clef.expr.CastMode;
import scale.score.*;

import scale.score.dependence.AffineExpr;


/**
 * This class represents the subtraction operation.
 * <p>
 * $Id: SubtractionExpr.java,v 1.62 2007-10-04 19:58:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class SubtractionExpr extends BinaryExpr
{
  public SubtractionExpr(Type type, Expr la, Expr ra)
  {
    super(type, la, ra);
  }

  /**
   * The expression type is the same as the type of expression la.
   */
  public SubtractionExpr(Expr la, Expr ra)
  {
    this(la.getType(), la, ra);
  }

  /**
   * This method of creating a SubtractionExpr instance will return a
   * LiteralExpr instance if the two arguments are equivalent.
   */
  public static Expr create(Type type, Expr la, Expr ra)
  {
    if (la.isLiteralExpr() && ra.isLiteralExpr())
      return ((LiteralExpr) la).subtract(type, ra);

    Type tat = type.getCoreType();
    if (!fpReorder && tat.isRealType())
      return new SubtractionExpr(type, la, ra);

    if (tat.isAtomicType()) {
      if (la.equivalent(ra) && (la.sideEffects() < SE_STATE)) {
        la.unlinkExpression();
        ra.unlinkExpression();
        if (type.isRealType())
          return new LiteralExpr(LiteralMap.put(0.0, type));

        return new LiteralExpr(LiteralMap.put(0, type));
      }
      if (ra instanceof SubtractionExpr) {
        SubtractionExpr se = (SubtractionExpr) ra;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (la.equivalent(sla) && (la.sideEffects() < SE_STATE)) {
          se.setRightArg(null);
          la.unlinkExpression();
          sla.unlinkExpression();
          return sra;
        }
      } else if (ra instanceof AdditionExpr) {
        AdditionExpr se = (AdditionExpr) ra;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (la.equivalent(sla) && (la.sideEffects() < SE_STATE)) {
          se.setRightArg(null);
          la.unlinkExpression();
          sla.unlinkExpression();
          return NegativeExpr.create(sra.getType(), sra);
        }
        if (sra.isLiteralExpr()) {
          se.setRightArg(null);
          se.setLeftArg(null);
          se.unlinkExpression();
          return new SubtractionExpr(type, new SubtractionExpr(type, la, sla), sra);
        }
      }

      if (la instanceof AdditionExpr) {
        AdditionExpr se = (AdditionExpr) la;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (la.sideEffects() < SE_STATE) {
          if (sla.equivalent(ra)) {
            se.setRightArg(null);
            se.setLeftArg(null);
            la.unlinkExpression();
            ra.unlinkExpression();
            sla.unlinkExpression();
            return sra;
          } else if (sra.equivalent(ra)) {
            se.setRightArg(null);
            se.setLeftArg(null);
            la.unlinkExpression();
            ra.unlinkExpression();
            sra.unlinkExpression();
            return sla;
          }
        }
        if (sra.isLiteralExpr()) {
          se.setRightArg(null);
          se.setLeftArg(null);
          se.unlinkExpression();
          if (ra.isLiteralExpr())
            return new AdditionExpr(type, sla, ((LiteralExpr) sra).subtract(type, ra));

          return new AdditionExpr(type, new SubtractionExpr(type, sla, ra), sra);
        }
      }

      if (la instanceof SubtractionExpr) {
        SubtractionExpr se = (SubtractionExpr) la;
        Expr sla = se.getLeftArg();
        Expr sra = se.getRightArg();
        if (sra.isLiteralExpr()) {
          se.setRightArg(null);
          se.setLeftArg(null);
          se.unlinkExpression();
          if (ra.isLiteralExpr())
            return new SubtractionExpr(type, sla, ((LiteralExpr) sra).add(type, ra));

          return new SubtractionExpr(type, new SubtractionExpr(type, sla, ra), sra);
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
            return AdditionExpr.create(type, la, ra2);
          }
        }
      }

      if (!type.isPointerType()) {
        Type lat = la.getCoreType();
        Type rat = ra.getCoreType();
        if (tat != lat)
          la = ConversionExpr.create(type,
                                     la,
                                     (tat.isRealType() ?
                                      CastMode.REAL :
                                      CastMode.TRUNCATE));
        if (tat != rat)
          ra = ConversionExpr.create(type,
                                     ra,
                                     (tat.isRealType() ?
                                      CastMode.REAL :
                                      CastMode.TRUNCATE));
      }
    }

    return new SubtractionExpr(type, la, ra);
  }

  public Expr copy()
  {
    return new SubtractionExpr(getType(), getLeftArg().copy(), getRightArg().copy());
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
    p.visitSubtractionExpr(this);
  }

  public String getDisplayLabel()
  {
    return "-";
  }

  /**
   * Return the coefficient of the subtraction operator. 
   * @param indexVar the index variable associated with the coefficient.
   * @return the coefficient value.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {
    Type t = getCoreType();
    if (!t.isIntegerType())
      return 0;

    return (getLeftArg().findLinearCoefficient(indexVar, thisLoop) -
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

    return AffineExpr.subtract(lae, rae);
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

    Type t  = getCoreType();
    Expr la = getLeftArg();
    Expr ra = getRightArg();
    if (t.isAtomicType() && la.equivalent(ra)) {
      if (t.isRealType())
        return LiteralMap.put(0.0, getType());
      return LiteralMap.put(0, getType());
    }

    if (t.isSigned()) {
      while ((la instanceof SubtractionExpr) && ra.isLiteralExpr()) {
        SubtractionExpr se  = (SubtractionExpr) la;
        Expr            la2 = se.getLeftArg();
        Expr            ra2 = se.getRightArg();

        if (!ra2.isLiteralExpr())
          break;

        la2.deleteOutDataEdge(se);
        ra2.deleteOutDataEdge(se);
        setRightArg(null);
        ra = ((LiteralExpr) ra2).add(getType(), ra);
        setLeftArg(la2);
        setRightArg(ra);
        la = la2;
      }

      Literal lao = la.getConstantValue(cvMap);
      Literal rao = ra.getConstantValue(cvMap);
      r = Lattice.subtract(getCoreType(), lao, rao);

      cvMap.put(this, r);
    } else
      r = Lattice.Bot; // The coward's way out!

    return r;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Type t  = getCoreType();
    Expr la = getLeftArg();
    Expr ra = getRightArg();
    if (t.isAtomicType() && la.equivalent(ra)) {
      if (t.isRealType())
        return LiteralMap.put(0.0, getType());
      return LiteralMap.put(0, getType());
    }

    if (t.isSigned()) {
      while ((la instanceof SubtractionExpr) && ra.isLiteralExpr()) {
        SubtractionExpr se  = (SubtractionExpr) la;
        Expr            la2 = se.getLeftArg();
        Expr            ra2 = se.getRightArg();

        if (!ra2.isLiteralExpr())
          break;

        la2.deleteOutDataEdge(se);
        ra2.deleteOutDataEdge(se);
        setRightArg(null);
        ra = ((LiteralExpr) ra2).add(getType(), ra);
        setLeftArg(la2);
        setRightArg(ra);
        la = la2;
      }

      Literal lao = la.getConstantValue();
      Literal rao = ra.getConstantValue();
      return Lattice.subtract(getCoreType(), lao, rao);
    }

    return Lattice.Bot; // The coward's way out!
  }

  /**
   * Return a simplied equivalent expression.
   * For example, for
   * <pre>
   *   ((a + c1) - c2)
   * <pre>
   * where <code>c1</code> and <code>c2</code> are constants,
   * the result is
   * <pre>
   *   (a + k)
   * </pre>
   * where <code>k</code> is <code>c1-c2</code>.
   * This method may modify the original expression.
   * This is used in the lowering of subscript expressions.
   * <b>Note</b> - this method is not safe for use when the CFG is in
   * SSA form as it may leave dangling def-use links.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr reduce()
  {
    Expr ra = getRightArg();
    if (!ra.isLiteralExpr())
      return this;

    Expr la = getLeftArg();
    if (la instanceof SubtractionExpr) {
      SubtractionExpr sub2 = (SubtractionExpr) la;
      Expr            ra2  = sub2.getRightArg();
      if (ra2.isLiteralExpr()) {
        setLeftArg(null);
        setRightArg(null);
        sub2.setRightArg(null);
        sub2.setRightArg(((LiteralExpr) ra).add(ra.getType(), ra2));
        return sub2;
      }
    } else if (la instanceof AdditionExpr) {
      AdditionExpr add2 = (AdditionExpr) la;
      Expr         ra2  = add2.getRightArg();
      if (ra2.isLiteralExpr()) {
        setLeftArg(null);
        setRightArg(null);
        add2.setRightArg(null);
        add2.setRightArg(((LiteralExpr) ra2).subtract(ra.getType(), ra));
        return add2;
      }
    }

    if (la.equivalent(ra))
      return la.getCoreType().isRealType() ?
               new LiteralExpr(LiteralMap.put(0.0, this.getType())) :
               new LiteralExpr(LiteralMap.put(0, this.getType()));

    return this;
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
}
