package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Alpha floating point conversion instructions.
 * <p>
 * $Id: FltCvtInstruction.java,v 1.15 2006-05-15 01:31:40 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class FltCvtInstruction extends FltOpInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.FltCvtInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  public FltCvtInstruction(int opcode, int rb, int rc)
  {
    super(opcode, AlphaRegisterSet.F0_REG, rb, rc);
    createdCount++;
  }

  /**
   * Return the number of cycles that this instruction requires.
   */
  public int getExecutionCycles()
  {
    return Opcodes.getExecutionCycles(opcode);
  }

  /**
   * Return the number of the functional unit required to execute this
   * instruction.
   */
  public int getFunctionalUnit()
  {
    return AlphaMachine.FU_FPALU;
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(gen.assembleRegister(rb));
    emit.emit(',');
    emit.emit(gen.assembleRegister(rc));
  }
}
