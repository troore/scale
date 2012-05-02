package scale.common;

import java.util.Enumeration;

/**
 * This class generates an enumeration for a single element.
 * <p>
 * $Id: SingleEnumeration.java,v 1.10 2005-02-07 21:28:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class SingleEnumeration<T> implements Enumeration<T>
{
  private T  value; // The single object returned by the enumeration.

  /**
   * @param element the single object returned by the enumeration
   */
  public SingleEnumeration(T element)
  {
    value = element;
  }
    
  public boolean hasMoreElements()
  {
    return (value != null);
  }

  public T nextElement()
  {
    T val = value;
    value = null;
    return val;      
  }
}

