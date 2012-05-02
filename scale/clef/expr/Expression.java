package scale.clef.expr;

import java.math.*;
import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * This is the base class for expressions such as add, subscript, etc.
 * <p>
 * $Id: Expression.java,v 1.74 2007-08-28 21:10:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class Expression extends Node 
{
  /**
   * The expression type (e.g., int, etc.).
   */
  private Type type; 

  public Expression(Type type)
  {
    assert (type != null) : "Expresion has no type!";
    this.type = type;
  }

  /**
   * Add all declarations referenced in this expression to the
   * collection.
   */
  public abstract void getDeclList(AbstractCollection<Declaration> varList);

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (getClass() != exp.getClass())
      return false;
    Expression expr = (Expression) exp;
    return type.getCoreType() == expr.getCoreType();
  }

  public String toStringSpecial()
  {
    return type.toString();
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.  This method should be over-ridden as it simplay returns
   * the class name.
   */
  public String getDisplayLabel()
  {
    String mnemonic = toStringClass();
    if (mnemonic.endsWith("Op"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 2);
    return mnemonic;
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.  This method should be over-ridden
   * as it simplay returns the color red.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTBLUE;
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.  This method should be over-ridden as it
   * simplay returns the shape "box".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.BOX;
  }

  public void visit(Predicate p)
  {
    p.visitExpression(this);
  }

  public final Type getType()
  {
    return type;
  }

  /**
   * Specify the type associated with this expression.
   */
  public void setType(Type type) 
  {
    this.type = type;
  }

  /**
   * Get the actual Type in cases where getting a RefType will
   * be a problem.  For example, if the expression should be of
   * type PointerType, a cast of a RefType to PointerType will
   * cause an exception eventhough the RefType contains the 
   * PointerType.
   *
   * Note - getType() should always be used when the Type
   * is not examined or when the attributes are needed.
   *
   * @see #getType
   * @see scale.clef.type.RefType
   */
  public final Type getCoreType()
  {
    if (type == null)
      return null;
    return type.getCoreType();
  }

  /**
   * Return the type of the thing pointed to by the type of the expression.
   * Equivalent to
   * <pre>
   *   getCoreType().getPointedTo().getCoreType()
   * </pre>
   */
  public final Type getPointedToCore()
  {
    return type.getCoreType().getPointedTo().getCoreType();
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return Lattice.Bot;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return hashCode();
  }

  /**
   * Return true if compilation of this expression will not result in
   * the generation of a {@link scale.score.chords.Chord CFG node}.
   * For example, <code>(a && b)</code> may require a {@link
   * scale.score.chords.IfThenElseChord branch} to represent and
   * <code>i++</code> requires a {@link scale.score.chords.ExprChord
   * store}.
   */
  public boolean isSimpleOp()
  {
    return true;
  }

  /**
   *  Return true if the result of the expression is either true (1)
   *  or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return false;
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return false;
  }
}
