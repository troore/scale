package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents a heap allocation operation that has a default
 * value and a placement parameter.  
 * <p>
 * $Id: AllocatePlacementOp.java,v 1.33 2007-08-28 17:58:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result type of the operation should 
 * be a pointer to the object type (ie., a pointer to <tt>type</tt>). 
 */
public class AllocatePlacementOp extends HeapOp
{
  /**
   * The expression.
   */
  private Expression expr;
  /**
   * The placement expression.
   */
  private Expression placement;

  /**
   * @param rt is the type of the result
   * @param alloctype is the type of the allocated object, not the type of the result
   * @param defv is the default value
   * @param placement is the placement expression
   */
  public AllocatePlacementOp(Type rt, Type alloctype, Expression defv, Expression placement)
  {
    super(rt, alloctype);
    setExpr(defv);
    setPlacement(placement);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    AllocatePlacementOp op = (AllocatePlacementOp) exp;
    return expr.equivalent(op.expr) && placement.equivalent(op.placement);
  }

  public void visit(Predicate p)
  {
    p.visitAllocatePlacementOp(this);
  }

  /**
   * Return the default value.
   */
  public final Expression getExpr()
  {
    return expr;
  }

  /**
   * Return the placement expression.
   */
  public final Expression getPlacement()
  {
    return placement;
  }

  /**
   * Specify the default value.
   */
  protected final void setExpr(Expression expr)
  {
    this.expr = expr;
  }

  /**
   * Specify the placement expression.
   */
  protected final void setPlacement(Expression placement)
  {
    this.placement = placement;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getAllocType();
    if (i == 1)
      return expr;
    assert (i == 2) : "No such child " + i;
    return placement;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 3;
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    expr.getDeclList(varList);
    placement.getDeclList(varList);
  }
}
