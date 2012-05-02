package scale.alias.steensgaard;

import scale.common.*;
import scale.alias.*;

/**
 * A class which represents a type variable in Steensgaard's alias
 * analysis algorithm.
 * <p>
 * $Id: TypeVar.java,v 1.30 2006-02-28 16:37:02 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A class which represents a type variable.  The type variable is
 * the interface between the user variables and the variables maintained
 * by Steensgaard's algorithm.  In the algorithm, each variable is
 * associated with a type which is a description of the abstract location
 * for the variable.   The type is represented by an <tt>ECR</tt> which
 * is an element in the fast union/find data structure.
 * 
 * @see ECR
 */
public class TypeVar extends AliasVar
{
  /**
   * The ECR that represents the type of the variable.
   */
  protected ECR ecr;

  public TypeVar(scale.clef.decl.Declaration n, AliasType t)
  {
    super(n);
    ecr = new ECR(t, this);
  }
  
  /**
   * Create a new type variable.  The initial type is <tt>ref(BOT, BOT)</tt>.
   */
  public TypeVar(scale.clef.decl.Declaration decl)
  {
    this(decl, new LocationType());
  }

  /**
   * Create a new type variable which is equivalent to the given type variable.
   * That is, they belong to the same ECR.
   * @param decl is the name of the variable.
   * @param v is the type variable with the type information.
   */
  public TypeVar(scale.clef.decl.Declaration decl, TypeVar v)
  {
    super(decl);
    ecr = new ECR(v.getECR().getType(), this);
    // the type variables belong to the same alias group
    ecr.union(v.getECR());
  }

  /**
   * Return the representative ECR associated with the type variable.
   */
  public final ECR getECR()
  {
    return (ECR) ecr.find();
  }
  
  /**
   * Return the original ECR associated with the type variable.  The
   * ECR may not be the representative ECR.
   */
  public final ECR getOriginalECR()
  {
    return ecr;
  }

  /**
   * Return true if the alias variable is involved in an alias relationship.
   */
  public boolean isAlias() 
  {
    // Check if the ECR represents more than one variable.
    return (ecr.size() > 1);
  }

  /**
   * Return the points-to relation for this alias variable.  In Steensgaard's
   * algorithm, each variable only points to one other alias variable.  However, each
   * alias variable represents more than one actual variable.
   */
  public Vector<ECR> pointsTo()
  {
    Vector<ECR> v = ecr.getType().pointsTo();
    Vector<ECR> p = new Vector<ECR>(4);
    int    l = v.size();

    for (int i = 0; i < l; i++) {
      ECR ecr1 = v.elementAt(i);
      ecr1.addECRs(p); // only add the points-to relations for user variables.
    }

    return p;
  }

  /**
   * Return the points-to size for this alias variable.  In
   * Steensgaard's algorithm, each variable only points to one other
   * alias variable.  However, each alias variable represents more
   * than one actual variable.
   */
  public int pointsToSize()
  {
    return ecr.getType().pointsToSize();
  }

  /**
   * Return a string representation of a type variable.
   */
  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(" alias = ");
    buf.append(isAlias());
    buf.append(' ');
    buf.append(ecr);
    return buf.toString();
  }

  /**
   * Return a String suitable for labeling this node in a graphical display.
   * This method should be over-ridden as it simplay returns the class name.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer(super.getDisplayLabel());
    buf.append(' ');
    buf.append(isAlias());
    buf.append(' ');
    buf.append(" ECR ");
    buf.append(ecr.getID());
    return buf.toString();
  }

  /**
   * Remove any un-needed stuff after analysis has been performed.
   */
  public void cleanup()
  {
    if (ecr != null)
      ecr.cleanup();
  }

 /**
  * return all points-to relations from this type variable
  */
  public void allPointsTo(Vector<ECR> tv)
  {
    if (ecr.getType() == AliasType.BOT) // If the type is bottom, return.
      return;

    ECR e = ((LocationType) ecr.getType()).getLocation();

    if (e.visited())
      return;

    if (e.getType() == AliasType.BOT)
      return;

    tv.addElement(e);

    e.setVisited();
    if (e.getTypeVar() == null)
      return;

    e.getTypeVar().allPointsTo(tv);
  }
}
