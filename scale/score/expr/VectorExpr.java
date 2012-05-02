package scale.score.expr;

import scale.common.*;
import scale.clef.type.Type;
import scale.score.Note;
import scale.score.Predicate;

/**
 * This class represents a Vector expression.
 * <p>
 * $Id: VectorExpr.java,v 1.27 2006-02-28 16:37:14 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class VectorExpr extends NaryExpr
{
  /**
   * Create an expression that represents construction of a vector.
   * <p>
   * All incoming values should be of the same type.  The result
   * type is the same as that of the incoming data.
   * @param type of each vector element
   * @param operands each vector element
   */
  public VectorExpr(Type type, Vector<Expr> operands)
  {
    super(type, operands);
  }

  public Expr copy()
  {
    int          n    = numOperands();
    Vector<Expr> args = new Vector<Expr>(n);
    for (int i = 0; i < n; i++)
      args.addElement(getOperand(i).copy());

    return new VectorExpr(getType(), args);
  }

  public void visit(Predicate p)
  {
    p.visitVectorExpr(this);
  }

  public String getDisplayLabel()
  {
    return "Vector";
  }
}
