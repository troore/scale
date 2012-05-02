package scale.score.expr;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.BooleanType;
import scale.score.Predicate;

/**
 * This class represents expressions that compare two values and
 * return "true" or "false".
 * <p>
 * $Id: CompareMode.java,v 1.10 2007-10-18 16:55:30 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class MatchExpr extends BinaryExpr
{
  public MatchExpr(Type t, Expr e1, Expr e2)
  {
    super(t, e1, e2);
  }

  /**
   * Return true if this is a match expression.
   */
  public final boolean isMatchExpr()
  {
    return true;
  }

  /**
   * Return the type of comparison.
   */
  public abstract CompareMode getMatchOp();

  /**
   * Return the complement expression.
   * For example, the complement of "&lt;=" is "&gt;".
   */
  public abstract MatchExpr complement();

  /**
   *  Return true if the result of the expression is either true (1)
   *  or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return true;
  }

  public abstract Expr create(Type t, Expr e1, Expr e2);

  public Expr reduce()
  {
    Expr la = getLeftArg();
    Expr ra = getRightArg();

    if (ra.isLiteralExpr() && ra.getType().isSigned()) {
      if (la instanceof BinaryExpr) {
        BinaryExpr be  = (BinaryExpr) la;
        Expr       la2 = be.getLeftArg();
        Expr       ra2 = be.getRightArg();
        if (be.isAssociative() && la2.isLiteralExpr()) {
          Expr t = la2;
          la2 = ra2;
          ra2 = t;
        }
        if (be instanceof AdditionExpr) {
          if (ra2.isLiteralExpr()) {
            be.setLeftArg(null);
            be.setRightArg(null);
            setRightArg(null);
            Expr sub = SubtractionExpr.create(ra.getType(), ra, ra2);
            return create(getType(), la2, sub);
          }
        } else if ((be instanceof SubtractionExpr) && (ra2.isLiteralExpr())) {
          be.setLeftArg(null);
          be.setRightArg(null);
          setRightArg(null);
          Expr add = AdditionExpr.create(ra.getType(), ra, ra2);
          return create(getType(), la2, add);
        }
      }
    } else if (la.isLiteralExpr() && la.getType().isSigned()) {
      if (ra instanceof BinaryExpr) {
        BinaryExpr be  = (BinaryExpr) ra;
        Expr       la2 = be.getLeftArg();
        Expr       ra2 = be.getRightArg();
        if (be.isAssociative() && la2.isLiteralExpr()) {
          Expr t = la2;
          la2 = ra2;
          ra2 = t;
        }
        if (be instanceof AdditionExpr) {
          if (ra2.isLiteralExpr()) {
            be.setLeftArg(null);
            be.setRightArg(null);
            setLeftArg(null);
            Expr sub = SubtractionExpr.create(ra.getType(), la, ra2);
            return create(getType(), sub, la2);
          }
        } else if ((be instanceof SubtractionExpr) && (ra2.isLiteralExpr())) {
          be.setLeftArg(null);
          be.setRightArg(null);
          setLeftArg(null);
          Expr add = AdditionExpr.create(ra.getType(), la, ra2);
          return create(getType(), add, la2);
        }
      }
    }

    return this;
  }
}
