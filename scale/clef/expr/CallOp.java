package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * A class which represents a generic routine call expression.
 * <p>
 * $Id: CallOp.java,v 1.44 2007-08-28 17:58:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result type is specified via the constructor and should be
 * the result of the procedure call.
 */

public abstract class CallOp extends Expression
{
  private Vector<Expression> argList; //  List of arguments to the function.
  private Expression         routine; // The expression specifying the function (e.g., an IdReferenceOp).

  public CallOp(Type returnType, Expression routine, Vector<Expression> argList)
  {
    super(returnType);
    setArgList(argList);
    setRoutine(routine);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    CallOp op = (CallOp) exp;
    int n = argList.size();
    if (n != op.argList.size())
      return false;
    for (int i = 0; i < n; i++) {
      Expression arg1 = argList.elementAt(i);
      Expression arg2 = op.argList.elementAt(i);
      if (!arg1.equivalent(arg2))
        return false;
    }
    return routine.equivalent(op.routine);
  }

  public void visit(Predicate p)
  {
    p.visitCallOp(this);
  }

  /**
   * Return the expression that specifies what is called.
   */
  public final Expression getRoutine()
  {
    return routine;
  }

  /**
   * Return the <code>i</code>-th argument to the call.
   */
  public final Expression getArg(int i)
  {
    return argList.elementAt(i);
  }

  /**
   * set the <code>i</code>-th argument to the call.
   */
  public final void setArg(Expression arg, int i)
  {
    argList.setElementAt(arg, i);
  }

  /**
   * Return the number of arguments to the call.
   */
  public final int getNumArgs()
  {
    if (argList == null)
      return 0;
    return argList.size();
  }

  /**
   * Specify the expression that specifies what is called.
   */
  protected final void setRoutine(Expression routine)
  {
    this.routine = routine;
  }

  /**
   * Specify the arguments of the call.
   */
  protected final void setArgList(Vector<Expression> argList)
  {
    this.argList = argList;
  }

  /**
   * Return the procedure type node of the callee.  The expression
   * representing the routine should either (1) be an expression
   * of type ProcedureType or (2) be a pointer to a ProcedureType.
   */
  public final ProcedureType getProcedureInfo() 
  {
    return (ProcedureType) routine.getPointedToCore();
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getRoutine();

    return getArg(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1 + getNumArgs();
  }

  public boolean isSimpleOp()
  {
    return false;
  }

  /**
   * Return true if this expression contains a reference to the variable.
   */
  public boolean containsDeclaration(Declaration decl)
  {
    if (routine.containsDeclaration(decl))
      return true;

    if (argList == null)
      return false;

    int l = argList.size();
    for (int i = 0; i < l; i++) {
      Expression op = argList.elementAt(i);
      if ((op != null) && op.containsDeclaration(decl))
        return true;
    }
    return false;
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    routine.getDeclList(varList);
    int l = argList.size();
    for (int i = 0; i < l; i++) {
      Expression op = argList.elementAt(i);
      op.getDeclList(varList);
    }
  }
}
