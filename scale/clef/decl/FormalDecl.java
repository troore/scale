package scale.clef.decl;

import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents a formal parameter to a procedure/function.
 * <p>
 * $Id: FormalDecl.java,v 1.43 2007-03-21 13:31:51 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class FormalDecl extends VariableDecl
{
  /**
   * The parameter passing mode.
   */
  private ParameterMode passByMode;

  public FormalDecl(String        name,
                    Type          type,
                    ParameterMode passByMode,
                    Expression    defaultValue)
  {
    super(name, type, defaultValue);
    this.passByMode = passByMode;
  }

  public FormalDecl(String name, Type type, ParameterMode passByMode)
  {
    this(name, type, passByMode, null);
  }

  public FormalDecl(String name, Type type)
  {
    this(name, type, ParameterMode.VALUE);
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(passByMode);
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitFormalDecl(this);
  }

  /**
   * Return the parameter passing mode.
   * @see scale.clef.decl.ParameterMode
   */
  public final ParameterMode getMode()
  {
    return passByMode;
  }

  /**
   * Return the default value for this parameter (unused).
   */
  public final Expression getDefaultValue()
  {
    return getValue();
  }

  /**
   * Specify the parameter passing mode.
   */
  public final void setMode(ParameterMode passByMode)
  {
    this.passByMode = passByMode;
  }

  /**
   * Set the default value for this parameter (unused).
   */
  protected final void setDefaultValue(Expression d)
  {
    setValue(d);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public scale.clef.expr.Literal getConstantValue()
  {
    return Lattice.Bot;
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name)
  {
    return new FormalDecl(name, getType(), passByMode);
  }

  /**
   * Return true if this variable is an argument to the routine.
   */
  public final boolean isFormalDecl()
  {
    return true;
  }

  public final FormalDecl returnFormalDecl()
  {
    return this;
  }
}
