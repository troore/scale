package scale.alias.shapirohorowitz;

import scale.common.*;
import scale.alias.steensgaard.*;

/**
 * This class extends the superclass AliasType.
 * <p>
 * $Id: LocationTypeCat.java,v 1.11 2005-06-15 04:17:05 asmith Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * It is very similar to the class {@link scale.alias.steensgaard.LocationType LocationType}.
 * The principal difference between the two classes is the vector of ECRs that it points 
 * off to , as opposed to a single location/function type.
 * In theory, this class contains a vector of {@link scale.alias.steensgaard.ECR ECRs}
 * representing {@link scale.alias.steensgaard.ValueType ValueTypes}.
 * In practice, instead of maintaining a field
 * with the {@link scale.alias.steensgaard.ValueType ValueType}, we expose the
 * location and function fields of the {@link scale.alias.steensgaard.ValueType ValueType}
 * in this class in order to improve efficiency.
 *
 * @see LocationType
 * @see ValueTypeCat
 * @see FunctionTypeCat
 */
public class LocationTypeCat extends AliasType
{
  /**
   * The vector (of ECRs) representing pointed-to locations.
   */
  private ECR[] location;
  /**
   * The vector of (ECRs) representing pointed-to functions.
   */
  private ECR[] function;

  /**
   * Create a new location type.  The initial values of the location
   * and function types are ECRs that represent the BOTTOM type.
   */
  public LocationTypeCat(int categories)
  {
    location = new ECR[categories];
    for (int i = 0; i < categories; i++)
      location[i] = new ECR();
      
    function = new ECR[categories];
    for (int i = 0; i < categories; i++)
      function[i] = new ECR();
  }

  /**
   * Create a location type with an initial value.
   *
   * @param l the vector of ECRs representing locations.
   * @param f the vector of ECRs representing functions.
   */
  public LocationTypeCat(Vector<ECR> l, Vector<ECR> f) 
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
   * Return the ECR that represents the pointed-to location at specified index.
   * @return the ECR that represents the pointed-to location at specified index.
   */
  public final ECR getLocation(int index)
  {
    return (ECR) location[index].find();
  }

  /**
   * Return the ECR that represents the pointed-to function at specified index
   * @return the ECR that represents the pointed-to function at specified index
   */
  public final ECR getFunction(int index)
  {
    return (ECR) function[index].find();
  }

  /**
   * Recursively join two location types.
   *
   * @param t a location type.
   */
  public final void unify(AliasType t)
  {
    for (int i = 0; i < location.length; i++) {
      ECR tau1 = getLocation(i);
      ECR tau2 = ((LocationTypeCat) t).getLocation(i);

      if (!tau1.equivalent(tau2))
        tau1.join(tau2);
    }

    for (int i = 0; i < function.length; i++) {
      ECR lamda1 = getFunction(i);
      ECR lamda2 = ((LocationTypeCat) t).getFunction(i);
    
      if (!lamda1.equivalent(lamda2))
        lamda1.join(lamda2);
    }
  }

  /**
   * Return the list of ECRs that this location type represents.
   * @return the list of ECRs that this location type represents.
   */
  public Vector<ECR> pointsTo()
  {
    Vector<ECR> v = new Vector<ECR>();
    for (int i = 0; i < location.length; i++) {
      ECR ecr = location[i];
      if (ecr.getType() != AliasType.BOT)
        v.addElement(ecr);
    }
    
    for (int i = 0; i < function.length; i++) {
      ECR ecr = function[i];
      if (ecr.getType() != AliasType.BOT)
        v.addElement(ecr);
    }
    
    return v;
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
   * Return a string representing of a location type.
   * @return a string representing of a location type.
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
