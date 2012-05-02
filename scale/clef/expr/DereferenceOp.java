package scale.clef.expr;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.type.*;

/**
 * The DereferenceOp class represents the derefernce (*) operator.
 * <p>
 * $Id: DereferenceOp.java,v 1.29 2007-05-10 16:48:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class DereferenceOp extends MonadicOp
{
  public DereferenceOp(Expression arg)
  {
    super(VoidType.type, arg);
    Type ty = arg.getCoreType();
    assert ty.isPointerType();
    setType(ty.getPointedTo());
  }

  public void visit(Predicate p)
  {
    p.visitDereferenceOp(this);
  }
}
