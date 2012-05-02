package scale.annot;

import scale.annot.Creator;

/**
 * This class is used to indicate that the annotation came from the
 * source program.
 * <p>
 * $Id: CreatorSource.java,v 1.14 2007-08-13 12:32:02 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class CreatorSource extends Creator
{
  /**
   * @param n name of creator of the annotation.
   * Used for debugging and display.
   */
  public CreatorSource(String n)
  {
    super(n);
  }
}
