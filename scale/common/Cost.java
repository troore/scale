package scale.common;

/**
 * This class implements a cost model based on a power expansion.
 * <p>
 * $Id: Cost.java,v 1.15 2007-05-15 17:36:06 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The cost model is the sum of a set of terms.  Each term is of the form
 * <pre>
 * c*x**n
 * </pre>
 */
public class Cost
{
  private double[] coefficients;
  private int      numTerms;

  /**
   * Create a cost with one term with a zero coefficient, power of 0.
   */
  public Cost()
  {
    this(0.0, 0);
  }

  /**
   * Create a cost with one term with the given coefficient, etc.
   * @param coefficient of the cost term
   * @param power of the cost term
   */
  public Cost(double coefficient, int power)
  {
    this.coefficients = new double[power + 1];
    this.numTerms     = power + 1;

    for (int i = 0; i < power; i++)
      this.coefficients[i] = 0.0;

    this.coefficients[power] = coefficient;
  }

  private Cost(double[] coefficients, int numTerms)
  {
    this.coefficients = coefficients;
    this.numTerms = numTerms;

    assert (coefficients.length <= numTerms) : "Invalid cost construction.";
  }

  /**
   * Return the cost based on value x.
   */
  public double calcCost(double x)
  {
    double cost = 0.0;
    double xpow = 1.0;
    for (int i = 0; i < numTerms; i++) {
      cost += coefficients[i] * xpow;
      xpow *= x;
    }
    return cost;
  }

  /**
   *  Set all terms to 0.
   */
  public void reset()
  {
    for (int i = 0; i < numTerms; i++)
      coefficients[i] = 0.0;
  }

  /**
   *  Increment cost expression by constant:
   */
  public void add(int c)
  {
    coefficients[0] += c;
  }

  private void increase(int power)
  {
    if (power < numTerms)
      return;

    double[] nc = new double[power + 1];
    System.arraycopy(coefficients, 0, nc, 0, numTerms);
    coefficients = nc;
    numTerms = power + 1;
  }

  /**
   *  Add new term to cost expression:
   */
  public void add(double coefficient, int power)
  {
    increase(power);
    coefficients[power] += coefficient;
  }

  /**
   *  Add all the terms of another Cost to this one.
   */
  public void add(Cost cost)
  {
    if (cost == null)
      return;

    increase(cost.numTerms);

    for (int i = 0; i < cost.numTerms; i++)
      coefficients[i] += cost.coefficients[i];
  }

  /**
   *  Add new term to cost expression:
   */
  public void subtract(double coefficient, int power)
  {
    increase(power);
    coefficients[power] -= coefficient;
  }

  /**
   *  Multiply cost expression by constant:
   */
  public void multiply(double mfactor)
  {
    for (int i = 0; i < numTerms; i++)
      coefficients[i] *= mfactor;
  }

  /**
   *  Multiply cost expression by cost term:
   */
  public void multiply(double coefficient, int power)
  {
    increase(power);
    coefficients[power] *= coefficient;
  }

  /**
   *  Multiply all the terms of this loop with the terms of the argument
   *  cost.
   */
  public void multiply(Cost cost)
  {
    if (cost == null)
      return;

    int n1 = cost.numTerms;
    if (n1 == 1) {
      multiply(cost.coefficients[0]);
      return;
    }

    double[] nc = new double[numTerms + cost.numTerms - 1];
    for (int i = 0; i < numTerms; i++) {
      double ci = coefficients[i];
      for (int j = 0; j < n1; j++) {
        int    power = i + j;
        double cj    = cost.coefficients[j];
        nc[power] += ci * cj;
      }
    }

    coefficients = nc;
    numTerms = nc.length;
  }

  /**
   *  Divide cost expression by constant:
   */
  public void divide(double c)
  {
    for (int i = 0; i < numTerms; i++)
      coefficients[i] /= c;
  }

  /**
   *  Return the order of the polynomial: the power of the largest
   *  non-zero term.
   */
  public int order()
  {
    for (int i = numTerms - 1; i >= 0; i--) {
      if (coefficients[i] != 0.0)
        return i;
    }
    return 0;
  }

  /**
   * Return the term with the highest exponent.
   */
  public Cost getHighestOrderTerm()
  {
    int ord = order();
    return new Cost(coefficients[ord], ord);
  }

  public String toString()
  {
    StringBuffer sb    = new StringBuffer();
    boolean      first = true;

    for (int i = 0; i < numTerms; i++) {
      if (coefficients[i] == 0.0)
        continue;

      if (!first)
        sb.append(" + ");

      sb.append(coefficients[i]);

      if (i > 0) {
        sb.append("*n^");
        sb.append(i);
      }

      first = false;
    }

    if (first)
      sb.append('0');

    return sb.toString();
  }

  /**
   * Return true if this cost is less than the specified cost.
   */
  public boolean lessThan(Cost cost)
  {
    int l = numTerms;
    for (int i = l - 1; i >= 0; i--) {
      double c1 = coefficients[i];
      double c2 = 0.0;
      if (i < cost.numTerms)
        c2 = cost.coefficients[i];

      if (c1 < c2)
        return true;
      if (c1 > c2)
        break;
    }

    return false;
  }

  /**
   * Return a deep copy of this cost.
   */
  public Cost copy()
  {
    return new Cost(coefficients.clone(), numTerms);
  }
}
