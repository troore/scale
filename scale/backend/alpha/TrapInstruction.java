package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents an  Alpha Trap Barrior instruction.
 * <p>
 * $Id: TrapInstruction.java,v 1.15 2006-11-09 14:40:26 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class TrapInstruction extends MemoryInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.TrapInstruction", stats);
  }

  public TrapInstruction()
  {
    super(Opcodes.TRAPB, 0, 0, null);
    createdCount++;
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
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
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit("TRAPB");
  }

  public String toString()
  {
    return "TRAPB";
  }
}
