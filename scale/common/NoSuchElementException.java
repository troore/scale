package scale.common;

/** 
 * A search for an element of a container failed to find the element.
 * <p>
 * $Id: NoSuchElementException.java,v 1.14 2005-02-07 21:28:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class NoSuchElementException extends scale.common.RuntimeException 
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info - information about the exception
   */
  public NoSuchElementException(String info)
  {
    super("No such element - " + info);
  }
}

