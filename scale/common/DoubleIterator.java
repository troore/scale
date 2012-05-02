package scale.common;

import java.util.Iterator;

/**
 * This class generates an iterator for two elements.
 * <p>
 * $Id: DoubleIterator.java,v 1.6 2005-02-07 21:28:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class DoubleIterator<T> implements Iterator<T>
{
  private T value1; // The first object returned by the iterator 
  private T value2; // The second object returned by the iterator 

  /**
   * @param element1 the first object returned by the enumeration
   * @param element2 the second object returned by the enumeration
   */
  public DoubleIterator(T element1, T element2)
  {
    value1 = element1;
    value2 = element2;
  }

  public boolean hasNext()
  {
    return (value1 != null);
  }

  public T next()
  {
    T val = value1;
    value1 = value2;
    value2 = null;
    return val;      
  }

  public void remove()
  {
  }
}

