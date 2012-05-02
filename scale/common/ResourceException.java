package scale.common;

/** 
 * This exception signals a lack of a resource needed.
 * <p>
 * $Id: ResourceException.java,v 1.14 2005-02-07 21:28:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ResourceException extends scale.common.RuntimeException 
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info - information about the exception
   */
  public ResourceException(String info)
  {
   super(info);
  }
}

