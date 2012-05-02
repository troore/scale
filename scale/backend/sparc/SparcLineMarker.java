package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class is used to associate source line numbers with instructions.
 * <p>
 * $Id: SparcLineMarker.java,v 1.8 2005-02-07 21:27:39 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class SparcLineMarker extends LineMarker
{
  public SparcLineMarker(Object marker, int lineNumber)
  {
    super(marker, lineNumber);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit("\t!\tline ");
    emit.emit(lineNumber());
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public String toString()
  {
    return "!\tline " + lineNumber();
  }
}
