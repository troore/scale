package scale.alias.shapirohorowitz;

import scale.common.*;
import scale.alias.steensgaard.*;

/**
 * A class that extends TypeVar class.
 * <p>
 * $Id: TypeVarCat.java,v 1.12 2006-02-28 16:37:02 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class is very similar to the {@link scale.alias.steensgaard.TypeVar TypeVar} class.
 * The principal difference is we have a new field that the type points off to - a category
 * object that the type variable belongs to
 * Also, when setting up the {@link scale.alias.steensgaard.ECR ECR},
 * we reference new classes: {@link scale.alias.steensgaard.ECR ECR}
 * and {@link scale.alias.shapirohorowitz.LocationTypeCat LocationTypeCat}.
 */
public class TypeVarCat extends TypeVar
{
  /**
   * The category to which this Type variable belongs.
   */
  private int category;

  /**
   * Create a new type variable.  The initial type is <tt>ref(BOT, BOT)</tt>.
   */
  public TypeVarCat(scale.clef.decl.Declaration n, int categories)
  {
    super(n, new LocationTypeCat(categories));
  }

  /**
   * Create a new type variable which is equivalent to the given type variable.
   * That is, they belong to the same ECR.
   * @param n the name of the variable.
   * @param v the type variable with the type information.
   **/
  public TypeVarCat(scale.clef.decl.Declaration n, TypeVarCat v)
  {
    super(n, v);
    category = v.getCategory();
  }

  /**
   * Set the category of the type variable.
   */
  public void setCategory(int category)
  {
    this.category = category;
  }

  /**
   * Return the category.
   */
  public int getCategory()
  {
    return category;
  }

 /**
  * return all points-to relations from this type variable
  */
  public void allPointsTo(Vector<ECR> tv)
  {
    if (ecr.getType() == AliasType.BOT)
      return;

    // Get the vector of locations pointed to by this type var.

    Vector<ECR> v = ((LocationTypeCat) ecr.getType()).getLocations();
    int         l = v.size();

    for (int i = 0; i < l; i++) {
      ECR e = v.elementAt(i);
      // If have not seen this ecr yet.
      if (!(e.visited() || (e.getType() == AliasType.BOT))) {
        tv.addElement(e);
        e.setVisited();
        // Get the points-to relations from this ecr.
        if (e.getTypeVar() != null)
          ((TypeVarCat) e.getTypeVar()).allPointsTo(tv);
      }
    }
  }

  /**
   * Return a string representation of a type variable.
   * @return a string representation of a type variable.
   */
  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(" cat-");
    buf.append(category);
    return buf.toString();
  }

  /**
   * @return a String suitable for labeling this node in a graphical display.
   * This method should be over-ridden as it simplay returns the class name.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer(super.getDisplayLabel());
    buf.append(" cat-");
    buf.append(category);
    return buf.toString();
  }
}
