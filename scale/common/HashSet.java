package scale.common;

import java.util.Iterator;
import java.util.Enumeration;

/**
 * A Scale cover class for a {@link java.util.HashSet java.util.HashSet}.
 * <p>
 * $Id: HashSet.java,v 1.25 2007-10-04 19:58:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class HashSet<T> extends java.util.HashSet<T> implements java.lang.Cloneable 
{
  private static final long serialVersionUID = 42L;

  public HashSet()
  {
    this(203, 0.75f);
  }

  public HashSet(int capacity)
  {
    this(capacity, 0.75f);
  }

  public HashSet(int capacity, float loadRatio)
  {
    super(capacity, loadRatio);
  }

  public HashSet(HashSet<T> set)
  {
    super(set);
  }

  public HashSet(java.util.HashSet<T> set)
  {
    super(set);
  }

  public HashSet(java.util.Set<T> set)
  {
    super(set);
  }

  public HashSet(java.util.List<T> list)
  {
    super(list);
  }

  public synchronized String toString()
  {
    StringBuffer tmp = new StringBuffer("{");

    if (!isEmpty()) {
      Iterator<T> e = iterator();
      while (e.hasNext()) {
        T o = e.next();
        tmp.append(o.toString());
        if (e.hasNext())
          tmp.append(", ");
      }
    }
    tmp.append('}');
    return tmp.toString();
  }

  /**
   * Remove an object at random from the HashSet
   */
  public T remove()
  {
    Iterator<T> e = iterator();
    T           r = null;
    if (e.hasNext()) {
      r = e.next();
      remove(r);
    }
    return r;
  }

  public HashSet<T> union(HashSet<T> u)
  {
    HashSet<T>  n = new HashSet<T>(this);
    Iterator<T> e = u.iterator();
    while (e.hasNext())
      n.add(e.next());
    return n;
  }

  /**
   * Add the elements of an iteration to this HashSet.
   * @param e is the iterator from which the new elements are obtained
   */
  public void add(Iterator<T> e)
  {
    while (e.hasNext())
      super.add(e.next());
  }

  /**
   * Add the elements of an enumeration to this HashSet.
   * @param e is the enumeration from which the new elements are obtained
   */
  public void add(Enumeration<T> e)
  {
    while (e.hasMoreElements())
      super.add(e.nextElement());
  }

  /**
   * Add the elements of another HashSet to this HashSet.
   * @param hs the HashSet from which the new elements are obtained
   */
  public void add(HashSet<T> hs)
  {
    Iterator<T> e = hs.iterator();
    while (e.hasNext())
      this.add(e.next());
  }

  @SuppressWarnings("unchecked")
  public final HashSet<T> clone()
  {
    return (HashSet<T>) super.clone();
  }
}
