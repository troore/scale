package scale.common;

/**
 * This error indicates that some feature is not yet implemented.
 * <p>
 * $Id: NotImplementedError.java,v 1.15 2005-02-07 21:28:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class NotImplementedError extends scale.common.Error 
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info - information about the exception
   */
  public NotImplementedError(String info)
  {
    super("Not implemented - " + info);
  }
}
