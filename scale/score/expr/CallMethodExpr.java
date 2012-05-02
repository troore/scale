package scale.score.expr;

import scale.common.*;
import scale.clef.type.Type;
import scale.score.Predicate;

/**
 * This class represents a call to a class method.
 * <p>
 * $Id: CallMethodExpr.java,v 1.28 2007-10-04 19:58:28 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class CallMethodExpr extends CallExpr
{
  /**
   * @param type result type of the routine CallMethod.  Void for procedures.
   * @param method is the method
   * @param arguments is a vector of expressions which are the
   * arguments - the first of which is the <i>this</i> argument
   */
  public CallMethodExpr(Type type, Expr method, Vector<Expr> arguments)
  {
    super(type, method, arguments);
  }

  public Expr copy()
  {
    int    n    = numArguments();
    Vector<Expr> args = new Vector<Expr>(n);
    for (int i = 0; i < n; i++)
      args.addElement(getArgument(i).copy());

    return new CallMethodExpr(getType(), getFunction().copy(), args);
  }

  public Expr getObjectClass() 
  {
    return getOperand(1);
  }

  public Expr getMethod() 
  {
    return getFunction();
  }

  public void visit(Predicate p)
  {
    p.visitCallMethodExpr(this);
  }

  public String getDisplayLabel()
  {
    return "CallM";
  }
}
