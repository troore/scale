package scale.clef.expr;

import scale.clef.*;
import scale.clef.decl.FieldDecl;
import scale.clef.type.Type;

/**
 * A class which represents value of the "." and "<code>-&gt;</code>"
 * operators in C.
 * <p>
 * $Id: SelectOp.java,v 1.29 2007-06-05 14:15:24 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The selection operation represents the value of the operation.  The
 * right argument is the address of the structure and the left
 * argument is the field.
 */
public class SelectOp extends AggregateOp 
{
  public SelectOp(Expression struct, FieldDecl fd)
  {
    super(fd.getType(), struct, fd);
    assert struct.getType().isPointerType() : "Not pointer type - " + struct.getCoreType();
  }

  public void visit(Predicate p)
  {
    p.visitSelectOp(this);
  }
}
