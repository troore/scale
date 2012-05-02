package scale.alias.steensgaard;

import scale.common.*;

/**
 * A class which implements the non-standard set of types used in
 * Steensgaard's algorithm to represent abstract locations.  
 * <p>
 * $Id: AliasType.java,v 1.12 2005-05-09 17:10:33 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * We never create objects of class <tt>AliasType</tt>.  We only
 * create objects that extend this class.
 *
 * @see ValueType
 * @see FunctionType
 * @see LocationType
 */
public class AliasType
{
  private int id; // A unique value the identifies the type.
  private static int num = 0; // Maintain a list of unique identifier values.

  /**
   * A special type that represents <tt>bottom</tt>.
   */
  public static final AliasType BOT = new AliasType();

  /**
   * Only allow subtypes to create these objects.
   */
  protected AliasType() 
  {
    id = num++;
  }

  public int getNodeID()
  {
    return id;
  }

  /**
   * Return the list of ECRs that this type points-to.   This method should be
   * redefined by a subclass.
   */
  public Vector<ECR> pointsTo() 
  {
    return null;
  }

  /**
   * Return the number of ECRs that this type points-to.   This method should be
   * redefined by a subclass.
   */
  public int pointsToSize() 
  {
    return 0;
  }

  /**
   * Recursively join two types.  This method should be redefined
   * by a subclass.
   * @param t the specifed type.
   */
  public void unify(AliasType t)
  {
  }

  /**
   * Convert the class name of this node to a string representation.
   */
  public final String toStringClass()
  {
    String c = getClass().toString();
    return c.substring(c.lastIndexOf('.') + 1);
  }

  /**
   * Return a string representing the type id.
   */
  public final String toString()
  {
    StringBuffer buf = new StringBuffer("(");
    buf.append(toStringClass());
    buf.append('-');
    buf.append(id);
    buf.append(' ');
    if (this == BOT)
      buf.append("BOT ");
    buf.append(toStringSpecial());
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return information specific to the derived class.
   */
  public String toStringSpecial()
  {
    return "";
  }

  /**
   * Return a string represnting the type id.  This routine is not overidden
   * by a subclass (and <tt>toString</tt> is typically overidden).
   */
  public String toStringShort()
  {
    if (this == BOT)
      return "BOT";
    return String.valueOf(id);
  }

  /**
   * Remove any un-needed stuff after analysis has been performed.
   */
  public void cleanup()
  {
  }
}
