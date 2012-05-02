package scale.common;

/**
 * This error indicates that the system has detected an
 * improper useage of a table.
 * <p>
 * $Id: InvalidTableError.java,v 1.15 2005-02-07 21:28:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This includes conditions such as table underflow or overflow.
 */
public class InvalidTableError extends scale.common.Error 
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info - information about the exception
   */
  public InvalidTableError(String info)
  {
    super("Invalid table - " + info);
  }
}

