package scale.alias.shapirohorowitz;

import scale.common.*;
import scale.alias.steensgaard.*;

/**
 * A class which implements the non-standard type describing functions
 * (or pointers to functions).
 * <p>
 * $Id: FunctionTypeCat.java,v 1.12 2005-02-07 21:27:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * In Steensgaard's paper, he refers to
 * this as <i>lambda</i> types.  A function type contains a list
 * of parameters (represented as <tt>ValueType</tt> objects) and
 * a list of function return values (also represented as 
 * {@link scale.alias.steensgaard.ValueType ValueType}
 * objects).  Although Steensgaard's algorithm allows multiple return
 * values, we just use one.
 */
public class FunctionTypeCat extends AliasType
{
  /**
   * The list of types representing the arguments to the function.
   */
  private Vector<AliasType> arguments;
  /**
   * The type representing the return value of the function.
   */
  private ValueTypeCat retval;

  /**
   * private variable that stores the number of categories being implemented in algorithm
   */
  private int categories;

  /**
   * Create a function type with an empty list of arguments and 
   * a return type with the value BOTTOM (upside down T).
   */
  public FunctionTypeCat(int numberCategories)
  {
    arguments = new Vector<AliasType>(1);
    retval = null;
    categories = numberCategories;
  }

  /**
   * Return the number of arguments.
   */
  public int numArguments()
  {
    return arguments.size();
  }

  /**
   * Return the specified argument.
   */
  public AliasType getArgument(int i)
  {
    return arguments.elementAt(i);
  }

  /**
   * Add an argument to the function type.
   * @param a the argument to add to the function type
   */
  public final void addArgument(ValueTypeCat a)
  {
    arguments.addElement(a);
  }

  /**
   * Return the type representing the function return value.
   * @return the type representing the function return value.
   */
  public final ValueTypeCat getRetval()
  {
    return retval;
  }

  /**
   * Set function's return value type.
   * @param r the ECR representing the return value.
   */
  public final void setRetval(ValueTypeCat r)
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
    FunctionTypeCat f = (FunctionTypeCat) t;
    int             l1 = arguments.size();
    int             l2 = f.arguments.size();

    // First process the arguments.

    Vector<AliasType> newarg1 = new Vector<AliasType>();
    Vector<AliasType> newarg2 = new Vector<AliasType>();

    int i1 = 0;
    int i2 = 0;
    while ((i1 < l1) || (i2 < l2)) {
      ValueTypeCat alpha1;
      ValueTypeCat alpha2;

      // Get the next argument for this function - if there isn't any, then create one.

      if (i1 >= l1) {
        alpha1 = new ValueTypeCat(categories);
        newarg1.addElement(alpha1);
      } else {
        alpha1 = (ValueTypeCat) arguments.elementAt(i1);
        i1++;
      }

      // Get the next argument for the given function - if there isn't any then create one.

      if (i2 >= l2) {
        alpha2 = new ValueTypeCat(categories);
        newarg2.addElement(alpha2);
      } else {
        alpha2 = (ValueTypeCat) f.arguments.elementAt(i2);
        i2++;
      }

      for (int i = 0; i < categories; i++) {
        ECR tau1 = alpha1.getLocation(i);
        ECR tau2 = alpha2.getLocation(i);
        if (tau1 != tau2)
          tau1.join(tau2);

        ECR lamda1 = alpha1.getFunction(i);
        ECR lamda2 = alpha2.getFunction(i);
        if (lamda1 != lamda2)
          lamda1.join(lamda2);
      }

    }

    // If we've created any new arguments, add them.

    int ln1 = newarg1.size();
    for (int i = 0; i < ln1; i++)
      arguments.addElement(newarg1.elementAt(i));

    int ln2 = newarg2.size();
    for (int i = 0; i < ln2; i++)
      arguments.addElement(newarg2.elementAt(i));

    // Now - process the return type.

    ValueTypeCat alpha1 = getRetval();
    ValueTypeCat alpha2 = ((FunctionTypeCat) t).getRetval();

    for (int i = 0; i < categories; i++) {
       ECR tau1 = alpha1.getLocation(i);
       ECR tau2 = alpha2.getLocation(i);
       if (tau1 != tau2)
         tau1.join(tau2);

       ECR lamda1 = alpha1.getFunction(i);
       ECR lamda2 = alpha2.getFunction(i);
       if (lamda1 != lamda2)
         lamda1.join(lamda2);
    }
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
   * Return a string representing of a function type.
   * @return a string representing of a function type.
   */
  public String toStringSpecial() 
  {
    StringBuffer lam = new StringBuffer("(lam ");

    int l = arguments.size();
    for (int i = 0; i < l; i++) {
      if (i > 0)
        lam.append(", ");
      lam.append(((ValueTypeCat) arguments.elementAt(i)).toStringShort());
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
