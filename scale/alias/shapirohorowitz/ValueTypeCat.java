package scale.alias.shapirohorowitz;

import scale.common.*;
import scale.alias.steensgaard.*;

/**
 * A class which implements the non-standard type describing values.
 * <p>
 * $Id: ValueTypeCat.java,v 1.10 2005-06-15 04:17:05 asmith Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * In Steensgaard's paper, he refers to this as <i>alpha</i> types.
 * A ValueType contains a location (<i>tau</i>) and function (<i>lamda</i>)
 * type.
 * 
 * @see LocationTypeCat
 * @see FunctionTypeCat
 */
public class ValueTypeCat extends AliasType
{
  /**
   * The type decribing a location (or a pointer to a location).
   */
  private ECR[] location;
  /**
   * The type describing a function (or a pointer to a function).
   */
  private ECR[] function;

  /**
   * Create a new value type.  The values of the location and function
   * types are ECRs that represent the BOTTOM type.
   */
  public ValueTypeCat(int categories)
  {
    location = new ECR[categories];
    for (int i = 0; i < categories; i++)
      location[i] = new ECR();
      
    function = new ECR[categories];
    for (int i = 0; i < categories; i++)
      function[i] = new ECR();
  }

  /**
   * Create a value type with a given location and function type.
   * @param l the ECR representing the location type.
   * @param f the ECR representing the function type.
   */
  public ValueTypeCat(Vector<ECR> l, Vector<ECR> f)
  {
    int ll = l.size();
    location = new ECR[ll];
    for (int i = 0; i < ll; i++)
      location[i] = l.elementAt(i);
      
    int lf = f.size();
    function = new ECR[lf];
    for (int i = 0; i < lf; i++)
      function[i] = f.elementAt(i);
  }
  
  /**
   * Return the location type of the value.
   * @return the location type of the value.
   */
  public final ECR getLocation(int index)
  {
    return (ECR) location[index].find();
  }

  /**
   * Return the function type of the value.
   * @return the function type of the value.
   */
  public final ECR getFunction(int index)
  {
    return (ECR) function[index].find();
  }

  /**
   * return the location vector
   */
  public Vector<ECR> getLocations()
  {
    int         l = location.length;
    Vector<ECR> v = new Vector<ECR>(l);
    for (int i = 0; i < l; i++)
      v.addElement((ECR) location[i].find());
    return v;
  }

  /**
   * return the function vector
   */
  public Vector<ECR> getFunctions()
  {
    int         l = function.length;
    Vector<ECR> v = new Vector<ECR>(l);
    for (int i = 0; i < l; i++)
      v.addElement((ECR) function[i].find());
    return v;
  }

  /**
   * Return a string representing of a value type.
   * @return a string representing of a value type.
   */
  public String toStringSpecial() 
  {
    StringBuffer buf = new StringBuffer("(ref (");
    int    ll = location.length;
    for (int i = 0; i < ll; i++) {
      ECR el = location[i];
      buf.append(el.toStringShort());
    }
    buf.append(") x (");
    int    lf = function.length;
    for (int i = 0; i < lf; i++) {
      ECR ef = function[i];
      buf.append(ef.toStringShort());
    }
    buf.append("))");

    return buf.toString();
  }

  /**
   * Remove any un-needed stuff after analysis has been performed.
   */
  public void cleanup()
  {
    if (location != null) {
      int ll = location.length;
      for (int i = 0; i < ll; i++) {
        ECR el = location[i];
        el.cleanup();
      }
    }
      
    if (function != null) {
      int lf = function.length;
      for (int i = 0; i < lf; i++) {
        ECR ef = function[i];
        ef.cleanup();
      }
    }
  }
}
