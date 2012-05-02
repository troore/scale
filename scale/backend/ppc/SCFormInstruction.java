package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents PowerPC SC-form instructions.
 * <p>
 * $Id: SCFormInstruction.java,v 1.5 2007-10-04 19:57:56 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class SCFormInstruction extends PPCBranch
{
  /**
   * Create a branch relative that does not set the link register.
   * @param numTargets specifies where the number of branch targets
   */
  public SCFormInstruction(int numTargets)
  {
    super(Opcodes.SC, false, numTargets);
    assert (Opcodes.instForm[opcode] == Opcodes.SC_FORM) :
      "SC-form incorrect form for " + Opcodes.opcodes[opcode] + " instruction";
  }

 /**
   * Return true if this instruction is independent of the specified
   * instruction.  If instructions are independent, than one
   * instruction can be moved before or after the other instruction
   * without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    return !inst.isBranch();
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
  }

  public String toString()
  {
    return Opcodes.getOp(this);
  }
}
