package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all Mips instructions except branches.
 * <p>
 * $Id: MipsInstruction.java,v 1.9 2006-11-16 17:49:37 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * <pre>
 */

public abstract class MipsInstruction extends Instruction
{
  /**
   * the instruction opcode
   */
  protected int opcode;

  /**
   * @param opcode is the instruction's opcode
   */
  public MipsInstruction(int opcode)
  {
    this.opcode = opcode;
  }

  public int getOpcode()
  {
    return opcode;
  }

  protected void setOpcode(int opcode)
  {
    this.opcode = opcode;
  }

  /**
   * Return the number of bytes required for all Mips instructions.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Specify the registers used and defined by this instruction.
   * Uses must be specified before definitions.
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
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return false;
  }

  public void remapRegisters(int[] map)
  {
  }

  /**
   * Generate a String representation of a Displacement that can be used by the
   * assembly code generater.
   */
  public String assembleDisp(Assembler asm, Displacement disp, int ftn)
  {
    return MipsGenerator.assembleDisp(asm, disp, ftn);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
  }

  public String toString()
  {
    return Opcodes.getOp(this);
  }
}
