package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.expr.Literal;
import scale.clef.decl.Declaration;
import scale.score.Predicate;
import scale.score.Note;
import scale.score.analyses.AliasAnnote;
import scale.score.pred.References;

/**
 * A dual expression is used when there is both a "high-level" and a "low-level"
 * representation of an expression.
 * <p>
 * $Id: DualExpr.java,v 1.67 2007-10-17 13:39:59 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Both the high-level and the low-level expressions are in-coming
 * data edges.  For example, for the subscript operation A[i], the
 * DualExpr expression would have a SubscriptExpr in-coming data edge
 * representing A[I].  It would also have a LoadDeclValueExpr
 * in-coming data edge representing the load of the address of the
 * temporary in which the address A+I*sizeof(A[i]) was placed by the
 * low-level expression.
 */
public class DualExpr extends Expr 
{
  /**
   * The high level expression..
   */
  private Expr high; 
  /**
   * The low level expression..
   */
  private Expr low;  

  /**
   * Create a dual expression that has both a high- and low-level
   * representation.
   * @param high the high-level representation
   * @param low the low-level representation
   */
  public DualExpr(Expr high, Expr low)
  {
    super(high.getType());
    this.high = high;
    high.setOutDataEdge(this); // Add other side of data edges.
    this.low = low;
    low.setOutDataEdge(this); // Add other side of data edges.
  }

  /**
   * Return true if the expressions are equivalent.
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    return low.equivalent(((DualExpr) exp).low);
  }

  protected Expr setOperand(Expr operand, int position)
  {
    Expr old = null;

    if (position == 0) {
      old = high;
      high = operand;
    } else if (position == 1) {
      old = low;
      low = operand;
    } else
      throw new scale.common.InternalError("Invalid operand position - " + position);

    if (old != null)
      old.deleteOutDataEdge(this);

    operand.setOutDataEdge(this); // Add other side of data edges.
    return old;
  }

  /**
   * Return the nth operand.
   * @param position the index of the operand
   */
  public final Expr getOperand(int position)
  {
    if (position == 0)
      return high;
    else if (position == 1)
      return low;
    else
      throw new scale.common.InternalError("Invalid operand position - " + position);
  }

  /**
   * Return an array of the operands to the expression.
   */
  public final Expr[] getOperandArray()
  {
    Expr[] array = new Expr[2];
    array[0] = high;
    array[1] = low;
    return array;
  }

  /**
   * Return the number of operands to this expression.
   */
  public int numOperands()
  {
    return 2;
  }

  public Expr copy()
  {
    return new DualExpr(high.copy(), low.copy());
  }

  /**
   * Return the high-level representation.
   */
  public final Expr getHigh()
  {
    return high;
  }

  /**
   * Return the low-level representation.
   */
  public final Expr getLow()
  {
    return low;
  }

  /**
   * Return the array associated with the subscript expression. We
   * use the array name to represent the access of the array
   * - instead of the array index value.
   */
  public Expr getReference()
  {
    return low.getReference();
  }

  public String getDisplayLabel()
  {
    return "dual";
  }

  /**
   * Return an integer specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.MAGENTA;
  }

  /**
   * Return a integer specifying a shape to use when drawing this node
   * in a graphical display.
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.CIRCLE;
  }

  /**
   * The given expression is defined if the dual expression is defined
   * and the given expression is either the low or high level expression.
   * @param lv the low or high level expression
   * @return true if the low expression is defined
   */
  public boolean isDefined(Expr lv)
  {
    return this.isDefined() && ((getLow() == lv) || (getHigh() == lv));
  }

  public void visit(Predicate p)
  {
    p.visitDualExpr(this);
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    return getLow().getConstantValue(cvMap);
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return getLow().getConstantValue();
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return getLow().canonical();
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();
    high.loopClean();
    low.loopClean();
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    high.unlinkExpression();
    low.unlinkExpression();
  }

  /**
   * Replace this DualExpr with its lowered form.
   */
  public void lowerPermanently()
  {
    Note x    = getOutDataEdge();
    Expr lowe = low;
    low = null;
    lowe.deleteOutDataEdge(this);
    x.changeInDataEdge(this, lowe);
    high.unlinkExpression();
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.  Note - only the low expression is checked.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return low.containsDeclaration(decl);
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.  Note - only the low expression
   * is checked.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    return low.dependsOnDeclaration(decl);
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return low.optimizationCandidate();
  }

  /**
   * Return the {@link SubscriptExpr SubscriptExpr} that this load
   * uses or <code>null</code> if none is found.  This method uses the
   * use-def link to find an existing <code>SubscriptExpr</code
   * instance.
   */
  public SubscriptExpr findSubscriptExpr()
  {
    return high.findSubscriptExpr();
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
    return low.getAliasAnnote();
  }

  /**
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    high.getDeclList(varList);
    low.getDeclList(varList);
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    high.getLoadExprList(expList);
    low.getLoadExprList(expList);
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
    high.getExprList(expList);
    low.getExprList(expList);
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
    wl.push(high);
    wl.push(low);
  }

  /**
   * Replace all occurrances of a Declaration with another
   * Declaration.  Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return high.replaceDecl(oldDecl, newDecl) | low.replaceDecl(oldDecl, newDecl);
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    high.removeUseDef();
    low.removeUseDef();
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    if (high.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + high);
    if (low.getOutDataEdge() != this)
      throw new scale.common.InternalError("One way " + this + " -> " + low);

    high.validate();
    low.validate();
  }

  /**
   * Record any variable references in this expression in the table
   * of reference.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    high.recordRefs(stmt, refs);
    low.recordRefs(stmt, refs);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    high.removeRefs(stmt, refs);
    low.removeRefs(stmt, refs);
  }

  /**
   * Return this Note unless it is a non-essential expression.  For
   * {@link scale.score.chords.Chord Chord} this method returns
   * <code>this</code>.  For a {@link scale.score.expr.DualExpr
   * DualExpr} or an address cast (e.g., {@link
   * scale.score.expr.ConversionExpr ConversionExpr}) this method
   * returns the out data edge.
   */
  public Note getEssentialUse()
  {
    return getOutDataEdge().getEssentialUse();
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return low.executionCostEstimate();
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return low.sideEffects();
  }
}
