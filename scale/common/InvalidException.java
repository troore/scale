package scale.common;

/**
 * This exception may be used whenever a parameter has an invalid value
 * such as an unexpected NULL pointer or a value out of range.  
 * <p>
 * $Id: InvalidException.java,v 1.15 2005-02-07 21:28:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class InvalidException extends scale.common.Exception 
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info - information about the exception
   */
  public InvalidException(String info)
  {
    super("Invalid - " + info);
  }
}

