package scale.alias.steensgaard;

import java.util.Iterator;

import scale.common.*;

/**
 * A class which represents an Equivalence Class Representative (ECR) 
 * with associated type information.
 * <p>
 * $Id: ECR.java,v 1.43 2005-06-15 04:17:05 asmith Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The ECR is an extension of a fast union/find data structure.
 */
public class ECR extends DisjointSet
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  private static int createdCount = 0; // A count of all the created instances of this class.
  private static int num          = 0; // Maintain a list of unique identifier values.

  static
  {
    Statistics.register("scale.alias.steensgaard.ECR", "created");
  }

  /**
   * Return the current number of instances of this class.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * A unique value to use in CFG spanning algorithms.
   */
   private static int nextColor = 0; 

  /**
   * The (non-standard) inference type of the variable.  The type of
   * an ECR is actually the type of the reprsentative element.  If
   * this ECR is merged and is no longer the representative, then the
   * type field is invalid.
   */
  private AliasType type;
  /**
   * A list of 'pending' joins for this ECR.  We make conditional joins,
   * if the type for this variable is BOTTOM.  Then, if the type for
   * this variable changes, we need to update the types of all the 
   * ECRs on the pending list.
   */
  private HashSet<ECR> pending;
  /**
   * A unique identifier for the ECR.
   */
  private int id;

  /**
   * the color of the ECR in the current visit
   */
  private int color;

  /**
   * The (original) alias variable that this ECR represents.
   */
  private TypeVar var;

  /**
   * Create an Equivalence Class Representative (ECR) that is associated
   * with the BOTTOM type (upside down T).
   */
  public ECR()
  {
    this(AliasType.BOT, null);
  }
  
  /**
   * Create a new Equivalence Class Representative (ECR) with a given type.
   * @param type the initial type of the ECR.
   * @param var the variable that this ECR represents.
   */
  public ECR(AliasType type, TypeVar var)
  {
    this.type    = type;
    this.pending = null;
    this.id      = num++;
    this.var     = var;
    this.color   = 0;

    createdCount++;
  }

  /**
   * Return the type associated with the ECR.
   */
  public final AliasType getType() 
  { 
    ECR rep = (ECR) find();
    return rep.type;
  }

  /**
   * Return the unique identifier number representing the ECR.
   */
  public final int getID()
  {
    return id;
  }

  /**
   * Return the set representative identifier number for the ECR.
   */
  public final int getsetID()
  {
    ECR rep = (ECR) find();
    return rep.id;
  }
  
  /**
   * Return the type variable that is represented by this ECR.
   */
  public final TypeVar getTypeVar()
  {
    return var;
  }

  /**
   * Add the list of elements represented by this disjoint set to the
   * vector.  Only add the points-to relations for user variables.  We
   * look at the representative element to get the list.
   */
  public void addECRs(Vector<ECR> v)
  {
    int l = size();
    for (int i = 0; i < l; i++) {
      ECR ecr = (ECR) getElement(i);
      if (ecr.getTypeVar() != null)
        v.addElement(ecr);
    }
  }

  /**
   * Add an ECR to the pending list of this ECR.  This occurs when
   * the type of this ECR is null - once a type is assigned, we
   * chane the type of the ECRs on the pending list.  The pending ECRs
   * are maintained by the representative element.
   *
   * @param e the ECR added to the pending list.
   */
  private final void addPending(ECR e) 
  {
    ECR rep = (ECR) find();
    if (rep.pending == null) 
      rep.pending = new HashSet<ECR>(5);
    rep.pending.add(e);
  }

  /**
   * Create a new pending set which is the union of the pending sets from
   * two other ECRs.
   * 
   * @param e1 an ECR containing a pending set to be unioned
   * @param e2 an ECR containing a pending set to be unioned
   */
  public final void unionPendingSets(ECR e1, ECR e2)
  {
    assert isRepresentative() : "unionPendingSets only legal on representative element " + this;

    if ((e1.pending != null) && (e2.pending != null))
      pending = e1.pending.union(e2.pending);
    else if (e2.pending != null)
      pending = new HashSet<ECR>(e2.pending);
    else if (e1.pending != null)
      pending = new HashSet<ECR>(e1.pending);

    if (this != e1)
      e1.pending = null;
    if (this != e2)
      e2.pending = null;
  }

  /**
   * A conditional join of two ECRs.  A conditional join places this
   * ECR on the <i>pending</i> list of the given ECR, if the type of
   * the given ECR is BOTTOM.  We do this in case the type of the
   * given ECR changes at some point.  If it does, then we update the
   * type of the first ECR.
   *
   * @param e the given ECR.
   */
  public final void cjoin(ECR e)
  {
    if (classTrace || Debug.debug(3))
      System.out.println("\tECR: cjoin - " + this + " and " + e);

    if (e.getType() == AliasType.BOT) {
      e.addPending(this);
    } else {
      join(e);
    }
  }

  /**
   * Join the types represented by this ECR and the specified ECR.
   *
   * @param e the specified ECR.
   */
  public final void join(ECR e)
  {
    if (classTrace || Debug.debug(3))
      System.out.println("\tECR: join - " + this + " and " + e);

    AliasType t1 = getType();
    AliasType t2 = e.getType();

    // combine the ECRs
    ECR u = (ECR) union(e);
  
    if (t1 == AliasType.BOT) {
      u.type = t2;
      if (t2 == AliasType.BOT) {
        u.unionPendingSets(this, e);
      } else {
        if (pending != null) {
          Iterator<ECR> iter = pending.iterator();
          while (iter.hasNext())
            u.join(iter.next());
          pending = null;
        }
      }
    } else {
      u.type = t1;
      if (t2 == AliasType.BOT) {
        if (e.pending != null) {
          Iterator<ECR> iter = e.pending.iterator();
          while (iter.hasNext())
            u.join(iter.next());
          e.pending = null;
        }
      } else {
        t1.unify(t2);
      }
    }
  }

  /**
   * Set the type of the ECR.  We also check if there are any pending ECRs -
   * if so then we need to call <tt>join</tt> on them.
   *
   * @param t the type.
   */
  public final void setType(AliasType t)
  {
    ECR rep = (ECR) find();
    rep.type = t;
    if (pending != null) {
      Iterator<ECR> iter = pending.iterator();
      while (iter.hasNext())
        join(iter.next());
      pending = null;
    }
  }

  /**
   * Return a string representation of an ECR.
   */
  public String toString()
  {
    StringBuffer buf = new StringBuffer("(ECR ");
    buf.append(id);
    buf.append(' ');
    buf.append(getType());

    if ((pending != null) && (pending.size() > 0)) {
      buf.append(" (pending");
      Iterator<ECR> iter = pending.iterator();
      while (iter.hasNext()) {
        ECR ecr = iter.next();
        buf.append(' ');
        buf.append(ecr.toStringShort());
      }
      buf.append(')');
    }
    buf.append(')');

    return buf.toString();
  }

  /**
   * Return a string representation of an ECR.  In this routine, we
   * just return a short version of the ECR. Specifically, we return
   * only the id value of the type.
   */
  public String toStringShort()
  {
    StringBuffer buf = new StringBuffer("(");
    buf.append(id);
    buf.append(' ');
    buf.append(getType().toStringShort());
    buf.append(')');
    return buf.toString();
  }

 /**
  * Set up for a new points-to graph traversal - that is, use the next color va  * lue.
  * This color is used to mark ECRs in points-to set traversals
  */
  public static void nextVisit()
  {
    nextColor++;
  }

  /**
   * Associate the current color value with an ECR.
   * The color is for the use of points-to set traversal algorithms.
  */
  public void setVisited()
  {
    this.color = nextColor;
  }

  /**
   * Return true if this Chord has been visited during the current visit.
   */
  public boolean visited()
  {
    return (color == nextColor);
  }   

  /**
   * Remove any un-needed stuff after analysis has been performed.
   */
  public void cleanup()
  {
    if (pending == null)
      return;

    pending = null;
    if (type != null)
      type.cleanup();
  }
}
