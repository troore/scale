package scale.clef.expr;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents the C++ <tt>delete</tt> operator for
 * heap allocated structures.
 * <p>
 * $Id: DeleteOp.java,v 1.29 2005-03-24 13:57:03 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result type is <tt>void</tt>.
 */

public class DeleteOp extends MonadicOp
{
  /**
   * The destructor.
   */
  private Expression destructor;

  public DeleteOp(Type type, Expression destructor, Expression e)
  {
    super(type, e);
    setDestructor(destructor);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    DeleteOp op = (DeleteOp) exp;
    return destructor.equivalent(op.destructor);
  }

  public void visit(Predicate p)
  {
    p.visitDeleteOp(this);
  }

  /**
   * Return the destructor expression that is used.
   */
  public final Expression getDestructor()
  {
    return destructor;
  }

  protected final void setDestructor(Expression destructor)
  {
    this.destructor = destructor;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getExpr();

    return destructor;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }
}
