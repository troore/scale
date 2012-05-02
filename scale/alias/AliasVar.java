package scale.alias;

import scale.common.*;
import scale.clef.decl.Declaration;
import scale.alias.steensgaard.ECR;

/**
 * A class which maintains information about an expression (variable or 
 * access path) that may be involved in an alias.
 * <p>
 * $Id: AliasVar.java,v 1.25 2007-08-13 12:32:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class AliasVar
{
  private String declName;

  /**
   * Create an alias variable object.
   */
  public AliasVar(Declaration decl)
  {
    this.declName = decl.getName();
  }

  /**
   * Return the name of the alias variable.
   */
  public String getDeclName()
  {
    return declName;
  }

  /**
   * Return true if the alias variable is involved in an alias relationship.
   */
  public abstract boolean isAlias();

  /**
   * Return the points-to relation for this alias variable.  The points-to
   * relation is the list of alias variables that this one points to.
   * @return the points-to relation for this alias variable.
   */
  public abstract Vector<ECR> pointsTo();
  /**
   * Return the representative ECR associated with the type variable.
   */
  public abstract ECR getECR();
  /**
   * Return the points-to size for this alias variable.  The points-to
   * relation is the list of alias variables that this one points to.
   */
  public abstract int pointsToSize();

  /**
   * Convert the class name of this node to a string representation.
   */
  public final String toStringClass()
  {
    String c = getClass().toString();
    return c.substring(c.lastIndexOf('.') + 1);
  }

  /**
   * Return a string represention of an alias variable.
   */
  public final String toString() 
  {
    StringBuffer buf = new StringBuffer("(");
    buf.append(toStringClass());
    buf.append(" \"");
    buf.append(declName);
    buf.append("\" ");
    buf.append(toStringSpecial());
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return a String suitable for labeling this node in a graphical display.
   * This method should be over-ridden as it simplay returns the class name.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer(toStringClass());
    buf.append(' ');
    buf.append(declName);
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
   * Remove any un-needed stuff after analysis has been performed.
   */
  public abstract void cleanup();

  /**
   * return all points-to relations from this type variable
   */
  public abstract void allPointsTo(Vector<ECR> v);
}
