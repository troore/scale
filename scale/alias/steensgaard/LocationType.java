package scale.alias.steensgaard;

import scale.common.*;

/**
 * A class which implements the non-standard type describing locations
 * (or pointers to locations).  In Steensgaard's paper, he refers to
 * <p>
 * $Id: LocationType.java,v 1.22 2005-02-07 21:27:11 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * In Steensgaard's paper, he refers to
 * this as <i>tau</i> types.  A location is either bottom (upside down
 * T) or is a <i>value</i>.
 * This type represents a pointer to either another
 * <tt>LocationType</tt> or to a <tt>FunctionType</tt>.  
 * <p> 
 * In theory, this class contains an ECR representing a
 * <tt>ValueType</tt>.  In practice, instead of maintaining a field
 * with the <tt>ValueType</tt>, we expose the location and function
 * fields of the <tt>ValueType</tt> in this class in order to improve
 * efficiency.
 *
 * @see ValueType
 * @see FunctionType
 */
public class LocationType extends AliasType
{
  /**
   * The ECR representing a pointed-to location.
   */
  private ECR location;
  /**
   * The ECR representing a pointed-to function.
   */
  private ECR function;

  /**
   * Create a new location type.  The initial values of the location
   * and function types are ECRs that represent the BOTTOM type.
   */
  public LocationType()
  {
    this(new ECR(), new ECR());
  }

  /**
   * Create a location type with an initial value.
   *
   * @param location the ECR representing the location.
   * @param function the ECR representing the function.
   */
  public LocationType(ECR location, ECR function) 
  {
    this.location = location;
    this.function = function;
  }

  /**
   * Return the type that represents the pointed-to location.
   */
  public final ECR getLocation()
  {
    return (ECR) location.find();
  }

  /**
   * Return the type that represents the pointed-to function.
   */
  public final ECR getFunction()
  {
    return (ECR) function.find();
  }

  /**
   * Recursively join two location types.
   *
   * @param t a location type.
   */
  public final void unify(AliasType t)
  {
    ECR tau1 = getLocation();
    ECR tau2 = ((LocationType) t).getLocation();

    if (!tau1.equivalent(tau2))
      tau1.join(tau2);

    ECR lamda1 = getFunction();
    ECR lamda2 = ((LocationType) t).getFunction();
    
    if (!lamda1.equivalent(lamda2))
      lamda1.join(lamda2);
  }

  /**
   * Return the list of ECRs that this location type represents.
   */
  public Vector<ECR> pointsTo()
  {
    Vector<ECR> v = new Vector<ECR>(2);
    if (location.getType() != AliasType.BOT)
      v.addElement(location);
    if (function.getType() != AliasType.BOT)
      v.addElement(function);
    return v;
  }

  /**
   * Return the number of ECRs that this location type represents.
   */
  public int pointsToSize()
  {
    int x = 0;
    if (location.getType() != AliasType.BOT)
      x += location.size();
    if (function.getType() != AliasType.BOT)
      x += function.size();
    return x;
  }

  /**
   * Return a string representing of a location type.
   */
  public String toStringSpecial() 
  {
    StringBuffer buf = new StringBuffer("(ref ");
    buf.append(location.toStringShort());
    buf.append(" x ");
    buf.append(function.toStringShort());
    buf.append(')');
    return buf.toString();
  }

  /**
   * Remove any un-needed stuff after analysis has been performed.
   */
  public void cleanup()
  {
    if (location != null)
      location.cleanup();
    if (function != null)
      function.cleanup();
  }
}
