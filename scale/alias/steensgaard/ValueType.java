package scale.alias.steensgaard;

/**
 * A class which implements the non-standard type describing values.
 * <p>
 * $Id: ValueType.java,v 1.16 2005-02-07 21:27:11 burrill Exp $
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * In Steensgaard's paper, he refers to this as <i>alpha</i> types.
 * A ValueType contains a location (<i>tau</i>) and function (<i>lamda</i>)
 * type.
 * 
 * @see LocationType
 * @see FunctionType
 */
public class ValueType extends AliasType
{
  /**
   * The type decribing a location (or a pointer to a location).
   */
  private  ECR location;
  /**
   * The type describing a function (or a pointer to a function).
   */
  private ECR function;

  /**
   * Create a new value type.  The values of the location and function
   * types are ECRs that represent the BOTTOM type.
   */
  public ValueType()
  {
    this(new ECR(), new ECR());
  }

  /**
   * Create a value type with a given location and function type.
   * @param location the ECR representing the location type.
   * @param function the ECR representing the function type.
   */
  public ValueType(ECR location, ECR function)
  {
    this.location = location;
    this.function = function;
  }
  
  /**
   * Return the location type of the value.
   * @return the location type of the value.
   */
  public final ECR getLocation()
  {
    return (ECR) location.find();
  }

  /**
   * Return the function type of the value.
   * @return the function type of the value.
   */
  public final ECR getFunction()
  {
    return (ECR) function.find();
  }

  /**
   * Return a string representing of a value type.
   * @return a string representing of a value type.
   */
  public String toStringSpecial() 
  {
    StringBuffer buf = new StringBuffer("( ");
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
