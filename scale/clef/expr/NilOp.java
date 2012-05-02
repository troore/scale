package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents the nil pointer value.
 * <p>
 * $Id: NilOp.java,v 1.29 2005-02-07 21:27:59 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class NilOp extends Literal
{
  public NilOp(Type type)
  {
    super(type);
  }

  public NilOp()
  {
    this(PointerType.create(VoidType.type));
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    return super.equivalent(exp);
  }

  public void visit(Predicate p)
  {
    p.visitNilOp(this);
  }

  public String getGenericValue()
  {
    return "((void *) 0)";
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return LiteralMap.put(0, getType());
  }
}
