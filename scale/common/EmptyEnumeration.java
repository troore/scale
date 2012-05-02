package scale.common;

import java.util.Enumeration;

/**
 * Define an empty enumeration to return when some complex container
 * structure has not yet been allocated but an enumeration of it is
 * requested.
 * <p>
 * $Id: EmptyEnumeration.java,v 1.14 2005-02-07 21:28:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public final class EmptyEnumeration<T> implements Enumeration<T>
{
  public EmptyEnumeration()
  {
  }

  public boolean hasMoreElements()
  {
    return false;
  }

  public T nextElement()
  {
    return null;
  }
}
