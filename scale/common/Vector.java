package scale.common;

import java.util.Enumeration;
import java.util.Collection;

/**
 * Implement our own Vector class that is un-synchronized and allows
 * us to collect statictics on the number of Vectors in use.
 * <p>
 * $Id: Vector.java,v 1.19 2007-10-04 19:58:11 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class Vector<T> extends java.util.ArrayList<T> implements Cloneable
{
  private static final long serialVersionUID = 42L;

  /**
   * Constructs an empty vector with the specified initial capacity.
   *
   * @param   initialCapacity   the initial capacity of the vector.
   */
  public Vector(int initialCapacity)
  {
    super(initialCapacity);
  }

  /**
   * Constructs an empty vector with the specified initial capacity.
   *
   * @param   initialCapacity   the initial capacity of the vector.
   */
  public Vector(int initialCapacity, int dummy)
  {
    super(initialCapacity);
  }

  /**
   * Constructs an empty vector. 
   */
  public Vector()
  {
    this(10);
  }

  /**
   * Constructs an empty vector. 
   */
  public Vector(Collection<T> set)
  {
    super(set);
  }

  /**
   * Returns a clone of this vector.
   */
  public Vector<T> clone()
  {
    return new Vector<T>(this);
  }

  /**
   * Sets the size of this vector. If the new size is greater than the
   * current size, new null items are added to the end of the
   * vector. If the new size is less than the current size, all
   * components at index newSize and greater are discarded.
   */
  public final void setSize(int newSize)
  {
    int size = size();
    if (size == newSize)
      return;

    if (size > newSize) {
      removeRange(newSize, size);
      return;
    }

    for (int i = 0; i < newSize - size; i++)
      add(null);
  }

  /**
   * Return the specified element.
   */
  public final T elementAt(int i)
  {
    return get(i);
  }

  /**
   * Add the element to the end of the vector.
   */
  public final void addElement(T element)
  {
    add(element);
  }

  /**
   * Inserts the specified object as a component in this vector at the
   * specified index. Each component in this vector with an index
   * greater or equal to the specified index is shifted upward to have
   * an index one greater than the value it had previously.
   */
  public final void insertElementAt(T element, int i)
  {
    add(i, element);
  }

  /**
   * Sets the component at the specified index of this vector to be
   * the specified object. The previous component at that position is
   * discarded.
   */
  public final void setElementAt(T element, int i)
  {
    set(i, element);
  }

  /**
   * Deletes the component at the specified index. Each component in
   * this vector with an index greater or equal to the specified index
   * is shifted downward to have an index one smaller than the value
   * it had previously. The size of this vector is decreased by 1.
   * @return the deleted component.
   */
  public final T removeElementAt(int i)
  {
    return remove(i);
  }

  /**
   * Removes all components from this vector and sets its size to zero.
   */
  public final void removeAllElements()
  {
    clear();
  }

  /**
   * Removes the first (lowest-indexed) occurrence of the argument
   * from this vector. If the object is found in this vector, each
   * component in the vector with an index greater or equal to the
   * object's index is shifted downward to have an index one smaller
   * than the value it had previously.
   * @return true if the eelement was removed
   */
  public final boolean removeElement(Object element)
  {
    int i = indexOf(element);
    if (i < 0)
      return false;
    remove(i);
    return true;
  }

  /**
   * Returns the first component (the item at index 0) of this vector.
   */
  public final T firstElement()
  {
    return get(0);
  }

 /**
   * Returns the last component of the vector.
   */
  public final T lastElement()
  {
    return get(size() - 1);
  }

  /**
   * Returns an enumeration of the components of this vector. The
   * returned enumeration object will generate all items in this
   * vector. The first item generated is the item at index 0, then the
   * item at index 1, and so on.
   */
  public final Enumeration<T> elements()
  {
    return new VEnumeration<T>(this);
  }

  private class VEnumeration<T> implements Enumeration<T>
  {
    int       i;
    Vector<T> v;

    public VEnumeration(Vector<T> v)
    {
      this.v = v;
      this.i = 0;
    }

    public boolean hasMoreElements()
    {
      return (i < v.size());
    }

    public T nextElement()
    {
      return v.get(i++);
    }
  }

  /**
   * Add the elements of an array to this Vector.
   */
  public final void addVectors(T[] c) 
  {
    if (c == null)
      return;

    for (int i = 0; i < c.length; i++)
      add(c[i]);
  }

  /**
   * Add the elements of a Vector to this vector.
   */
  public final void addVectors(Vector<T> c) 
  {
    if (c == null)
      return;

    int n = c.size();
    for (int i = 0; i < n; i++)
      add(c.get(i));
  }

  /**
   * Add the elements of an enumeration to this Vector.
   */
  public final void addVectors(Enumeration<T> e) 
  {
    while (e.hasMoreElements())
      add(e.nextElement());
  }

  /**
   * Revrese the order of the elements in the Vector.
   */
  public final void reverse()
  {
    for (int i = 0, j = size() - 1; i < j; i++, j--) {
      T x = get(i);
      T y = get(j);
      set(j, x);
      set(i, y);
    }
  }
}
