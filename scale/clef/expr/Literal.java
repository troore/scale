package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * The base class for classes which represent a literal, or constant,
 * value.
 * <p>
 * $Id: Literal.java,v 1.61 2007-08-28 17:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class is also used to provide special literal values such
 * as needed by the {@link scale.common.Lattice Lattice} class.
 */
public class Literal extends Expression
{
  public Literal(Type type)
  {
    super(type);
  }

  public void visit(Predicate p)
  {
    p.visitLiteral(this);
  }

  /**
   * Get the string version of the literal using C syntax.  For
   * another language, use getValue which returns an object that can
   * then be displayed using logic specific to the target language.
   * @return a String representing the literal value and uses C syntax
   */
  public String getGenericValue()
  {
    return "??";
  }

  /**
   * Return the number of elements in the Literal.
   */
  public int getCount()
  {
    return 1;
  }

  /**
   * Return the specified element of the constant.
   */
  public Literal getElement(long index) throws InvalidException
  {
    if (index == 0)
      return this;
    throw new InvalidException("No such element");
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return this;
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    return getDisplayString(getGenericValue());
  }

  /**
   * Return linearity of literal.
   * Integer literals have linearity of 0.
   * All other literals have linearity of 2.
   */
  public int linearity()
  {
    return 2;
  }

  /**
   * Return the coefficient value.
   */
  public int findCoefficient()
  {
    return 0;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5;
  }

  /**
   * Return true if the value of this literal is known to be zero.
   */
  public boolean isZero()
  {
    return false;
  }

  /**
   * Return true if the value of this literal is known to be one.
   */
  public boolean isOne()
  {
    return false;
  }

  /**
   * Return a string representing the floating point value.
   */
  public static String formatRealValue(double value)
  {
    String s = Double.toString(value);
    int    p = s.indexOf('.');

    if (p >= 0)
      return s;

    if (s.equals("NaN"))
      return "(0.0/0.0)";

    if (s.equals("Infinity"))
      return "(1.0/0.0)";

    if (s.equals("-Infinity"))
      return "(-1.0/0.0)";

    return s + "e0"; // Insure that it is in floating point form      
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
  }
}
