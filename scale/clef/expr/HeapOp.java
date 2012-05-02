package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This is the base class for all allocation operators.
 * <p>
 * $Id: HeapOp.java,v 1.26 2005-03-24 13:57:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class HeapOp extends Expression
{
  /**
   * The object to allocate, not the result type.
   */
  private Type alloctype;

  public HeapOp(Type type, Type alloctype)
  {
    super(type);
    this.alloctype = alloctype;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    HeapOp op = (HeapOp) exp;
    return alloctype == op.alloctype;
  }

  public void visit(Predicate p)
  {
    p.visitHeapOp(this);
  }

  /**
   * Return the type of the allocated object.
   */
  public final Type getAllocType()
  {
    return alloctype;
  }
}
