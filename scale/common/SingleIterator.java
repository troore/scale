package scale.common;

import java.util.Iterator;

/**
 * This class generates an iterator for a single element.
 * <p>
 * $Id: SingleIterator.java,v 1.5 2005-02-07 21:28:23 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class SingleIterator<T> implements Iterator<T>
{
  private T value; // The single object returned by the iterator 

  /**
   * @param element the single object returned by the iterator
   */
  public SingleIterator(T element)
  {
    value = element;
  }
    
  public boolean hasNext()
  {
    return (value != null);
  }

  public T next()
  {
    T val = value;
    value = null;
    return val;      
  }

  public void remove()
  {
  }
}

