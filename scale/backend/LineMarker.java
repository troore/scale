package scale.backend;

import scale.common.*;

/** 
 * This class is used to associate source line numbers with instructions.
 * <p>
 * $Id: LineMarker.java,v 1.8 2006-09-10 19:56:26 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class LineMarker extends Marker
{
  private int lineNumber; /* The line number of the source program. */
  private Object marker;  /* Other associated information. */

  /**
   * Mark the position corresponding to a source program line.
   * @param marker is other associated information
   * @param lineNumber is the source line number
   */
  public LineMarker(Object marker, int lineNumber)
  {
    super();
    this.marker = marker;
    this.lineNumber = lineNumber;
  }

  /**
   * Return the line number.
   */
  public int lineNumber()
  {
    return lineNumber;
  }

  /**
   * Return the other associated information.
   */
  public Object getMarker()
  {
    return marker;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("LineMarker: ");
    buf.append(marker);
    buf.append(' ');
    buf.append(lineNumber);
    return buf.toString();
  }
}


