package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * A class which represents the C variable argument processing.
 * <p>
 * $Id: VarArgOp.java,v 1.29 2007-08-28 17:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class VarArgOp extends Expression
{
  /**
   * vaList
   */
  private Expression vaList;

  public VarArgOp(Type type, Expression val)
  {
    super(type);
    setVaList(val);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    VarArgOp op = (VarArgOp) exp;
    return vaList.equivalent(op.vaList);
  }

  public void visit(Predicate p)
  {
    p.visitVarArgOp(this);
  }

  /**
   * Return the expression representing the va_list argument.
   */
  public final Expression getVaList()
  {
    return vaList;
  }

  /**
   * Specify the expression representing the va_list argument.
   */
  protected final void setVaList(Expression vaList)
  {
    this.vaList = vaList;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return vaList;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return vaList.containsDeclaration(decl);
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    vaList.getDeclList(varList);
  }
}
