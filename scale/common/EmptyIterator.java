package scale.common;

import java.util.Iterator;

/**
 * Define an empty iterator to return when some complex container structure
 * has not yet been allocated but an iterator of it is requested.
 * <p>
 * $Id: EmptyIterator.java,v 1.6 2005-02-07 21:28:21 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public final class EmptyIterator<T> implements Iterator<T> 
{
  public EmptyIterator()
  {
  }

  public boolean hasNext()
  {
    return false;
  }

  public T next()
  {
    return null;
  }

  public void remove()
  {
  }
}
