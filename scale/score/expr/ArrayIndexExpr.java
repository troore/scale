package scale.score.expr;

import scale.common.*;
import scale.score.*;
import scale.clef.type.Type;
import scale.clef.LiteralMap;
import scale.score.analyses.AliasAnnote;

/**
 * This class represents the generation of an array element address.
 * <p>
 * $Id: ArrayIndexExpr.java,v 1.22 2007-01-05 19:06:43 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ArrayIndexExpr extends TernaryExpr
{
  private int reuseLevel = 0;

  public ArrayIndexExpr(Type t, Expr array, Expr index, Expr offset)
  {
    super(t, array, index, offset);
  }

  public Expr copy()
  {
    return new ArrayIndexExpr(getType(), getLA().copy(), getMA().copy(), getRA().copy());
  }

  public void visit(Predicate p)
  {
    p.visitArrayIndexExpr(this);
  }

  public String getDisplayLabel()
  {
    return "[I]";
  }

  /**
   * Return the expression specifying the array.
   */
  public final Expr getArray() 
  {
    return getLA();
  }

  private void setArray(Expr array)
  {
    setLA(array);
  }

  /**
   * Return the expression specifying the index.
   */
  public final Expr getIndex() 
  {
    return getMA();
  }

  private void setIndex(Expr index)
  {
    setMA(index);
  }

  /**
   * Return the expression specifying the offset.
   */
  public final Expr getOffset() 
  {
    return getRA();
  }

  private void setOffset(Expr offset)
  {
    setRA(offset);
  }

  /**
   * The given expression is defined if the ArrayIndexExpr expression
   * is defined and the given expression is the array.
   * @param lv the expression representing the array name
   * @return true if the array is defined.
   */
  public boolean isDefined(Expr lv)
  {
    boolean d = this.isDefined();
    boolean l = (getArray() == lv);
    return d && l;
  }

  /**
   * Return the array associated with the subscript expression. We use
   * the array name to represent the access of the array - instead of
   * the array index value.
   * @return the array name
   */
  public Expr getReference()
  {
    return getArray().getReference();
  }

  /**
   * Return true if this expression is valid on the left side of an
   * assignment.
   */
  public boolean validLValue()
  {
    return true;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    Expr arr = getArray();
    return (((arr instanceof LoadDeclAddressExpr) ||
             arr.optimizationCandidate()) &&
            getIndex().optimizationCandidate() &&
            getOffset().optimizationCandidate());
  }

  public void setReuseLevel(int r)
  {
    reuseLevel = r;
  }

  public int getReuseLevel()
  {
    return reuseLevel;
  }

  /**
   * Get the alias annotation associated with a Scribble operator.
   * Most Scribble operators do not have alias variables so this
   * routine may return null.  Typically, the alias variable
   * information is attached to the declaration node associated with
   * the load operations.  However, we sometimes need to create alias
   * variables to hold alias information that is not directly assigned
   * to a user variable (e.g., <tt>**x</tt>).
   * @return the alias annotation associated with the expression
   */
  public AliasAnnote getAliasAnnote()
  {
    AliasAnnote aa = super.getAliasAnnote();
    if (aa != null)
      return aa;
    return getArray().getAliasAnnote();
  }

  /**
   * Return a simplied equivalent expression.
   * This method may modify the original expression.
   * This is used in the lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr reduce()
  {
    Expr index  = getIndex();
    Expr offset = getOffset();

    if (offset.getType().isSigned() ^ index.getType().isSigned())
      return this;

    if (index instanceof AdditionExpr) {
      AdditionExpr ae = (AdditionExpr) index;
      Expr         la = ae.getLeftArg();
      Expr         ra = ae.getRightArg();
      if (la.isLiteralExpr()) {
        Expr t = la;
        la = ra;
        ra = t;
      }
      if (ra.isLiteralExpr()) {
        ae.setLeftArg(null);
        ae.setRightArg(null);
        setOffset(null);
        offset = ((LiteralExpr) ra).add(offset.getCoreType(), offset);
        setIndex(la);
        setOffset(offset);
        return this;
      } else if (ra instanceof AdditionExpr) {
        AdditionExpr ae2 = (AdditionExpr) ra;
        Expr         la2 = ae2.getLeftArg();
        Expr         ra2 = ae2.getRightArg();
        if (la2.isLiteralExpr()) {
          Expr t = la2;
          la2 = ra2;
          ra2 = t;
        }
        if (ra2.isLiteralExpr()) {
          ae.setLeftArg(null);
          ae.setRightArg(null);
          ae2.setLeftArg(null);
          ae2.setRightArg(null);
          setOffset(null);
          offset = ((LiteralExpr) ra2).add(offset.getCoreType(), offset);
          setIndex(new AdditionExpr(ae.getType(), la, la2));
          setOffset(offset);
          return this;
        }
      }
    } else if (index instanceof SubtractionExpr) {
      SubtractionExpr ae = (SubtractionExpr) index;
      Expr            la = ae.getLeftArg();
      Expr            ra = ae.getRightArg();
      if (ra.isLiteralExpr()) {
        ae.setLeftArg(null);
        ae.setRightArg(null);
        setOffset(null);
        offset = SubtractionExpr.create(offset.getCoreType(), offset, ra);
        setIndex(la);
        setOffset(offset);
        return this;
      }
    }

    return this;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return (getArray().executionCostEstimate() +
            getOffset().executionCostEstimate() +
            getArray().executionCostEstimate());
  }
}
