package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;
import scale.clef.decl.RoutineDecl;

/** 
 * This class marks the position of a point branched to in Trips code.
 * <p>
 * $Id: TripsLabel.java,v 1.10 2006-11-16 17:49:41 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class TripsLabel extends Label
{
  private RoutineDecl routine;
  
  /**
   * Number of the loop this instruction is in, or 0 if none.  A
   * value of zero indicates that the instruction is not in a loop.
   */
  private int loopNumber;

  public TripsLabel(boolean referenced, RoutineDecl routine)
  {  	
    super(referenced);
    this.routine = routine;
  }

  /**
   * Create a label.
   * @param routine specifies the routine containing this label
   */
  public TripsLabel(RoutineDecl routine)
  {
    this(true, routine);
  }

  public RoutineDecl getRoutine()
  {
    return routine;
  }

  /**
   * Return the String representing the label.
   */
  public String getLabelString()
  {
    StringBuffer buf = new StringBuffer(this.getRoutine().getName());
    buf.append('$');
    buf.append(this.getLabelIndex());
    return buf.toString();
  }

  /**
   * Return the loop number of the instruction.
   * This is Trips specific.  For all other backends,
   * it returns 0.
   */
  public int getLoopNumber()
  {
    return loopNumber;
  }

  /**
   * Set the loop number of the instruction.
   * This is Trips specific.  For all other backends,
   * it does nothing.
   */
  protected void setLoopNumber(int loopNumber)
  {
    this.loopNumber = loopNumber;
  }
}
