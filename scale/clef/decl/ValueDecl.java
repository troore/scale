package scale.clef.decl;

import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents a declaration that has an initial value.
 * <p>
 * $Id: ValueDecl.java,v 1.42 2007-03-21 13:31:52 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class ValueDecl extends Declaration
{
  /**
   * The value of the declaration.
   */
  private Expression value;

  /**
   * Create a ValueDecl with the specified name, type, and value.
   */
  protected ValueDecl(String name, Type type, Expression initialValue)
  {
    super(name, type);

    setValue(initialValue);
  }

  public void visit(Predicate p)
  {
    p.visitValueDecl(this);
  }

  /**
   * Return the value associated with this declaration.
   */
  public final Expression getValue()
  {
    return value;
  }

  /**
   * Specify the value associated with this declaration.
   */
  public void setValue(Expression value)
  {
    this.value = value;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return value;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public scale.clef.expr.Literal getConstantValue()
  {
    if (value == null) 
      return Lattice.Top;
    return value.getConstantValue();
  }

  /**
   * Return true if the declaration is constant.
   */
  public boolean isConst()
  {
    Type t = getType();
    if (t == null)
      return false;
    return t.isConst();
  }

  public final boolean isValueDecl()
  {
    return true;
  }

  public final ValueDecl returnValueDecl()
  {
    return this;
  }
}
