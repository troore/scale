package scale.score.expr;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.score.Predicate;

import scale.score.*;
import scale.score.dependence.AffineExpr;

/**
 * This class represents a call to a function.
 * <p>
 * $Id: CallFunctionExpr.java,v 1.31 2006-02-28 16:37:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class CallFunctionExpr extends CallExpr
{
  /**
   * An instance of this class represents a call to a function.
   * @param type result type of the routine call.  Void for procedures.
   * @param routine expression node holding address of routine to call
   * @param arguments vector of expressions representing arguments
   */
  public  CallFunctionExpr(Type type, Expr routine, Vector<Expr> arguments)
  {
    super(type, routine, arguments);
  }

  public Expr copy()
  {
    int          n    = numArguments();
    Vector<Expr> args = new Vector<Expr>(n);
    for (int i = 0; i < n; i++)
      args.addElement(getArgument(i).copy());

    return new CallFunctionExpr(getType(), getFunction().copy(), args);
  }

  public void visit(Predicate p)
  {
    p.visitCallFunctionExpr(this);
  }

  public String getDisplayLabel()
  {
    return "Call";
  }
}
