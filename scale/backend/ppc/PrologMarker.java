package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
* This class marks the position for the routine prolog.
* <p>
* $Id: PrologMarker.java,v 1.3 2005-03-24 13:56:51 burrill Exp $
* <p>
* Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
* <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
* <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
* Amherst MA. 01003, USA<br>
* All Rights Reserved.<br>
* <p>
*/

public class PrologMarker extends Marker
{
  public PrologMarker()
  {
    super();
  }

  /**
   * Insert the assembler directive for the prolog.
   */
  public void assembler(Assembler asm, Emit emit)
  {
  }
}
