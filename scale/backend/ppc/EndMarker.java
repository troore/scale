package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;
import scale.clef.decl.RoutineDecl;

/** 
 * This class marks the last position in a routine.
 * <p>
 * $Id: EndMarker.java,v 1.3 2006-12-05 21:02:02 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class EndMarker extends Marker
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */
  private boolean macosx; /* True of OS is mac os x. */


  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.ppc.EndMarker", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  private RoutineDecl rd;

  public EndMarker(RoutineDecl rd, boolean macosx)
  {
    super();
    this.rd = rd;
    this.macosx = macosx;
    createdCount++;
  }

  /**
   * Specify the registers used by this instruction.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator rs, int index, int strength)
  {
    rs.useRegister(index, PPCRegisterSet.SP_REG, strength); // Make sure these registers are always "live".
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
  }
}
