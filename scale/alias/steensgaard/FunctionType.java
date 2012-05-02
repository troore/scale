package scale.alias.steensgaard;

import scale.common.*;

/**
 * A class which implements the non-standard type describing functions
 * (or pointers to functions).
 * <p>
 * $Id: FunctionType.java,v 1.24 2005-02-07 21:27:11 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * In Steensgaard's paper, he refers to
 * this as <i>lambda</i> types.  A function type contains a list
 * of parameters (represented as <tt>ValueType</tt> objects) and
 * a list of function return values (also represented as <tt>ValueType</tt>
 * objects).  Although Steensgaard's algorithm allows multiple return
 * values, we just use one.
 */
public class FunctionType extends AliasType
{
  /**
   * The list of types representing the arguments to the function.
   */
  private Vector<ValueType> arguments;
  /**
   * The type representing the return value of the function.
   */
  private ValueType retval;

  /**
   * Create a function type with an empty list of arguments and 
   * a return type with the value BOTTOM (upside down T).
   */
  public FunctionType()
  {
    arguments = new Vector<ValueType>(1);
    retval = null;
  }

  /**
   * Return the list of arguments.
   */
  public final Vector<ValueType> getArguments()
  {
    return arguments;
  } 
  
  /**
   * Add an argument to the function type.
   * @param a the argument to add to the function type
   **/
  public final void addArgument(ValueType a)
  {
    arguments.addElement(a);
  }

  /**
   * Create new arguments.
   * @param n is the number of new arguments to create
   */
  public void addNewArguments(int n)
  {
    for (int i = 0; i < n; i++) {
      arguments.addElement(new ValueType());
    }
  }

  /**
   * Return the type representing the function return value.
   */
  public final ValueType getRetval()
  {
    return retval;
  }

  /**
   * Set function's return value type.
   * @param r the ECR representing the return value.
   */
  public final void setRetval(ValueType r)
  {
    retval = r;
  }
  
  /**
   * Recursively join two function types.
   *
   * @param t a function type.
   */
  public final void unify(AliasType t)
  {
    Vector<ValueType> args2 = ((FunctionType) t).arguments;
    int    l1    = arguments.size();
    int    l2    = args2.size();
    int    l     = l1;

    if (l2 > l)
      l = l2;

    // First process the arguments.

    ValueType alpha2i = null;
    for (int i = 0; i < l; i++) {
      ValueType alpha1;
      ValueType alpha2;

      // Get the next argument for this function - if there isn't any, then create one.

      if (i >= l1) {
        alpha1 = new ValueType();
        addArgument(alpha1);
      } else {
        alpha1 = arguments.elementAt(i);
      }

      // Get the next argument for the given function - if there isn't any then create one.

      if (i >= l2) {
        if (alpha2i == null)
          alpha2i = new ValueType();
        alpha2 = alpha2i;
      } else {
        alpha2 = args2.elementAt(i);
      }

      ECR tau1 = alpha1.getLocation();
      ECR tau2 = alpha2.getLocation();
      if (tau1 != tau2)
        tau1.join(tau2);

      ECR lamda1 = alpha1.getFunction();
      ECR lamda2 = alpha2.getFunction();
      if (lamda1 != lamda2)
        lamda1.join(lamda2);
    }

    // Now - process the return type.

    ValueType alpha1 = getRetval();
    ValueType alpha2 = ((FunctionType) t).getRetval();

    ECR tau1 = alpha1.getLocation();
    ECR tau2 = alpha2.getLocation();
    if (tau1 != tau2)
      tau1.join(tau2);

    ECR lamda1 = alpha1.getFunction();
    ECR lamda2 = alpha2.getFunction();
    if (lamda1 != lamda2)
      lamda1.join(lamda2);
  }

  /**
   * Return the points-to relation for this type.  In this representation,
   * function types do not point to anything.
   * @return an empty vector.
   */
  public Vector<ECR> pointsTo() 
  {
    return new Vector<ECR>(1);
  }

  /**
   * Return the points-to size for this type.  In this representation,
   * function types do not point to anything.
   */
  public int pointsToSize() 
  {
    return 0;
  }

  /**
   * Return a string representing of a function type.
   */
  public String toStringSpecial() 
  {
    StringBuffer lam = new StringBuffer("lam");

    int l = arguments.size();
    for (int i = 0; i < l; i++) {
      if (i > 0)
        lam.append(", ");
      lam.append(arguments.elementAt(i).toStringShort());
    }

    lam.append(") (");
    lam.append(retval);
    lam.append("))");
    return lam.toString();
  }

  /**
   * Remove any un-needed stuff after analysis has been performed.
   */
  public void cleanup()
  {
    if (retval != null)
      retval.cleanup();
  }
}
