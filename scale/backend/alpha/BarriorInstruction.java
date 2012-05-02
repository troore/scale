package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents an Alpha Memory Barrior instruction.
 * <p>
 * $Id: BarriorInstruction.java,v 1.16 2006-11-09 14:40:25 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class BarriorInstruction extends MemoryInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */
  private static final IntegerDisplacement disp = new IntegerDisplacement(0x4000);

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.BarriorInstruction", stats);
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  public BarriorInstruction()
  {
    super(Opcodes.MB, 0, 0, disp);
    createdCount++;
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit("MB");
  }

  public String toString()
  {
    return "MB";
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
}
