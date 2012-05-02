package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class is used to associate source line numbers with instructions.
 * <p>
 * $Id: AlphaLineMarker.java,v 1.8 2006-09-10 19:23:54 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class AlphaLineMarker extends LineMarker
{
  public AlphaLineMarker(Object marker, int lineNumber)
  {
    super(marker, lineNumber);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit(".loc\t1 ");
    emit.emit(lineNumber());
    emit.emit("\t # line ");
    emit.emit(lineNumber());
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public String toString()
  {
    StringBuffer buf = new StringBuffer(".loc\t1 ");
    buf.append(lineNumber());
    buf.append("\t # line ");
    buf.append(lineNumber());
    return buf.toString();
  }
}


