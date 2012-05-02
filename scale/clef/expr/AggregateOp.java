package scale.clef.expr;

import scale.clef.*;
import scale.clef.decl.FieldDecl;
import scale.clef.type.*;

/**
 * A class which represents a field/member selection, or aggregate, 
 * operator.  For example, "." or "->".
 * <p>
 * $Id: AggregateOp.java,v 1.28 2006-12-19 16:09:27 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class AggregateOp extends MonadicOp
{
  private FieldDecl fd;

  protected AggregateOp(Type type, Expression struct, FieldDecl fd)
  {
    super(type, struct);
    this.fd = fd;
  }

  /**
   * Return the structure that is referenced.
   */
  public final Expression getStruct()
  {
    return getExpr();
  }

  /**
   * Return the field of the structure that is referenced.
   */
  public final FieldDecl getField()
  {
    return fd;
  }

  public void visit(Predicate p)
  {
    p.visitAggregateOp(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getStruct();
    assert (i == 1) : "No such child " + i;
    return fd;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }
}
