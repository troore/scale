package scale.common;

import java.util.Enumeration;

/**
 * A class which implements the data structure for Disjoint Sets.
 * <p>
 * $Id: DisjointSet.java,v 1.22 2005-03-24 13:57:13 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The disjoint-set data structure was introduced by Tarjan for performing
 * very quick <b>find-union</b> operations.  A disjoint-set data structure
 * maintains a collection <tt>S = {S1,S2,...,Sk}</tt> of disjoint dynamic
 * sets.  Each set is identified by a <b>representative</b>, which is some member
 * of the set. 
 * <p>
 * We implement the fast version which represents the sets as trees
 * and uses <i>path compression</i> and <i>union by rank</i> to
 * achieve good performance.  The worst case running time is
 * essentially linear w.r.t the total number of operations (actually
 * it is the total no. of operations times the inverse of Ackermann's
 * function).
 * <p>
 * Disjoint-sets are discussed in the book <i>Data Structures and
 * Network Algorithms</i> by Tarjan.  They are also described in
 * Chapter 22 of <i>Introduction to Algorithms</i> by Cormen
 * et. al. (aka, CLR).
 * <p>
 * Note that the list of elements in the disjoint set is only valid
 * for the representative element.  We only maintain the list of elements
 * when the set represents more than one value.  We do this to save
 * space because many sets will only represent a single element.  Thus,
 * the code is a little difficult to read because we try to only create
 * the vector structure when absolutly necessary.
 */
public class DisjointSet
{
  /**
   * A pointer to the parent.
   */
  private DisjointSet parent;
  /**
   * The 'rank' of the element.  The rank approximates the logarithm of the
   * subtree size.  The value is used to improve the efficiency of the
   * union.
   */
  private int rank;
  /**
   * The list of elements in the disjoint set.
   * Only the representative element contains the list of elements.
   * All other elements are null (we do this to save space).
   */
  private Vector<Object> elements;

  /**
   * Create a new disjoint set whose only member is this element.
   */
  public DisjointSet()
  {
    this.rank     = 0;
    this.parent   = null;
    this.elements = null;
  }

  /**
   * Combine this dynamic set with another.  We return the element that
   * is the representative element in the combined set.
   *
   * @param y a member of (another) disjoint set. 
   * @return the representative object of the unioned set.
   */
  public DisjointSet union(DisjointSet y)
  {
    // Get the representative elements of each set.
    DisjointSet elemX = find();
    DisjointSet elemY = y.find();

    // If the representatives are the same set, then they represent
    // the same set already so don't do a union.
    if (elemX == elemY)
      return elemX;

    // Intersect the sets based upon rank - the larger tree becomes the
    // the parent of the smaller tree.

    if (elemX.rank > elemY.rank) {
      elemY.parent = elemX;
      elemX.mergeElements(elemY);
      return elemX;
    }

    elemX.parent = elemY;
    if (elemX.rank == elemY.rank)
      elemY.rank += 1;
    elemY.mergeElements(elemX);

    return elemY;
  }
  
  /**
   * Return a pointer to the representative of this set.
   * @return the representative of this set.
   */
  public DisjointSet find()
  {
    if (parent == null)
      return this;

    return parent.find();
  }

  /**
   * Returns true if this is the element that is the representative
   * of the set (i.e., the parent of the tree that maintains all
   * the elements of this set).
   * @return true if this is the representative.
   */
  protected boolean isRepresentative()
  {
    return (parent == null);
  }

  /**
   * Return the number of elements represented by this disjoint set.
   * We look at the representative element to find the total.
   */
  public int size()
  {
    DisjointSet rep = find();
    if (rep.elements == null)
      return 1;

    return rep.elements.size();
  }

  /**
   * Return the i-th element of the set.
   */
  public Object getElement(int i)
  {
    DisjointSet rep = find();
    if (rep.elements == null) {
      assert (i == 0) : "index > size";
      return this;
    }

    return rep.elements.elementAt(i);
  }

  /**
   * Return the list of elements reprensented by this disjoint set.
   * We look at the representative element to get the list.
   */
  public Enumeration<Object> getElements()
  {
    DisjointSet rep = find();
    if (rep.elements == null)
      return new SingleEnumeration<Object>(this);

    return rep.elements.elements();
  }

  /**
   * Add the list of elements reprensented by this disjoint set to the vector.
   * We look at the representative element to get the list.
   */
  public void addElements(Vector<Object> v)
  {
    DisjointSet rep = find();
    if (rep.elements == null) {
      v.addElement((Object) this);
      return;
    }

    v.addVectors(rep.elements);
  }

  /**
   * Two disjoint sets are equivalent if they have the same
   * representative element.
   *
   * @param obj the object we check equality with
   * @return true if the objects are in the same disjoint set.
   */
  public boolean equivalent(DisjointSet obj)
  {
    if (this == obj)
      return true;
    if (this.find() == obj.find())
      return true;
    return false;
  }

  /**
   * Merge the elements from the two disjoint sets.  We add the
   * elements from the given disjoint set to this disjoint set.
   **/
  private final void mergeElements(DisjointSet set)
  {
    if (set == this)
      return;

    if (elements == null) {
      elements = new Vector<Object>(10, 10);
      elements.addElement(this);
    }

    if (set.elements == null) {
      elements.addElement(set);
      return;
    }

    int l = set.elements.size();
    for (int i = 0; i < l; i++)
      elements.addElement(set.elements.elementAt(i));
  }
}
