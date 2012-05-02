package scale.common;

/**
 * Implement our own Stack class that is un-synchronized and allows us
 * to collect statictics on the number of Stacks in use.
 * <p>
 * $Id: Stack.java,v 1.4 2007-10-04 19:58:11 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class Stack<T> extends Vector<T> implements Cloneable
{
  private static final long serialVersionUID = 42L;

  /**
   * Constructs an empty vector with the specified initial capacity.
   *
   * @param   initialCapacity   the initial capacity of the vector.
   */
  public Stack(int initialCapacity)
  {
    super(initialCapacity);
  }

  /**
   * Constructs an empty vector with the specified initial capacity.
   *
   * @param   initialCapacity   the initial capacity of the vector.
   */
  public Stack(int initialCapacity, int dummy)
  {
    super(initialCapacity);
  }

  /**
   * Constructs an empty vector. 
   */
  public Stack()
  {
    this(10);
  }

  /**
   * Constructs an empty vector. 
   */
  public Stack(Stack<T> stk)
  {
    super(stk.size());
    addVectors(stk);
  }

  /**
   * Returns a clone of this vector.
   */
  public Stack<T> clone()
  {
    return new Stack<T>(this);
  }

  /**
   * Removes the object at the top of this stack and returns that
   * object as the value of this function.
   */
  public T pop()
  {
    return remove(size() - 1);
  }

  /**
   * Pushes an item onto the top of this stack. 
   */
  public T push(T element)
  {
    add(element);
    return element;
  }

  /**
   * Returns the object at the top of this stack without removing it
   * from the stack.
   */
  public T peek()
  {
    return get(size() - 1);
  }

  /**
   * Returns the object next to the top of this stack without removing it
   * from the stack.
   */
  public T peekd()
  {
    return get(size() - 2);
  }

  /**
   * Returns true if and only if this stack contains no items; false
   * otherwise.
   */
  public final boolean empty()
  {
    return size() == 0;
  }

  /**
   * Returns the 1-based position where an object is on this stack. If
   * the object o occurs as an item in this stack, this method returns
   * the distance from the top of the stack of the occurrence nearest
   * the top of the stack; the topmost item on the stack is considered
   * to be at distance 1. The equals method is used to compare o to
   * the items in this stack.
   */
  public final int search(Object o)
  {
    int l = size();
    for (int i = l - 1; i >= 0; i--) {
      if (get(i) == o)
        return l - i;
    }
    return -1;
  }
}
