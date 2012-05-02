package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.VariableDecl;
import scale.clef.type.*;

/**
 * A class which represents a literal value that is the size of some type or variable.
 * <p>
 * $Id: SizeofLiteral.java,v 1.32 2005-06-16 20:56:29 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class SizeofLiteral extends Literal
{
  /**
   * sizeof(type).
   */
  private Type value;

  /**
   * Construct a representation of the C sizeof() function
   * @param type is the type of the expression - usually an integer type
   * @param v is either a type or a declaration
   */
  public SizeofLiteral(Type type, Node v)
  {
    super(type);
    setValue(v);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    SizeofLiteral sl = (SizeofLiteral) exp;
    return (value == sl.value);
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(getGenericValue());
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitSizeofLiteral(this);
  }

  /**
   * Get the string version of the literal using pseudo-C syntax.
   * The type of Declaraction is displayed using Clef node display syntax.
   * @return a String already converted for display.
   */
  public final String getGenericValue() 
  {
    StringBuffer buf = new StringBuffer("sizeof(");
    buf.append(getDisplayString(value.toString()));
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return the type whose size is represented.
   */
  public Type getSizeofType()
  {
    return value;
  }

  /**
   * Specify the item whose size is to be represented.
   */
  protected final void setValue(Node n)
  {
    if (n instanceof Type) {
      value = (Type) n;
      return;
    }

    assert (n instanceof VariableDecl) : "Invalid object " + n + " for sizeof literal.";

    value = ((VariableDecl) n).getType();
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    if (Machine.currentMachine != null) {
      Type t = value.getCoreType();
      return LiteralMap.put(t.memorySize(Machine.currentMachine), getCoreType());
    }
    return Lattice.Bot;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return value.hashCode();
  }

  /**
   * Return linearity of literal.
   * Integer literals have linearity of 0.
   * All other literals have linearity of 2.
   */
  public int linearity()
  {
    return 0;
  }

  /**
   * Return the coefficient value.
   */
  public int findCoefficient()
  {
    if (Machine.currentMachine != null) {
      Type t = value.getCoreType();
      return t.memorySizeAsInt(Machine.currentMachine);
    }
    return 0;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    if (Machine.currentMachine != null) {
      Type t = value.getCoreType();
      Machine.currentMachine.executionCostEstimate(t.memorySize(Machine.currentMachine));
    }
    return 5;
  }

  /**
   * Return true if the value of this literal is known to be one.
   */
  public boolean isOne()
  {
    return (1 == value.getCoreType().memorySize(Machine.currentMachine));
  }
}
