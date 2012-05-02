package scale.common;

/**
 * This error indicates that an attempt was made to change a graph,
 * and it was not possible to complete the operation. 
 * <p>
 * $Id: InvalidMutationError.java,v 1.8 2005-02-07 21:28:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class InvalidMutationError extends scale.common.InternalError
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info informative error description
   */
  public InvalidMutationError(String info)
  {
    super("Mutation - " + info);
  }
}
