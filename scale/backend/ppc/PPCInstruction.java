package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions except branches.
 * <p>
 * $Id: PPCInstruction.java,v 1.7 2006-11-09 00:56:06 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class PPCInstruction extends Instruction
{
  /**
   * the instruction opcode
   */
  protected int opcode;

  /**
   * @param opcode is the instruction's opcode
   */
  public PPCInstruction(int opcode)
  {
    this.opcode = opcode;
  }

  public final int getOpcode()
  {
    return opcode;
  }

  public final void setOpcode(int opcode)
  {
    this.opcode = opcode;
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

  /**
   * Return the number of bytes required for the BranchRegInstruction.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    return true;
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
  }

  /**
   * Return true if the instruction is a load.
   */
  public final boolean isLoad()
  {
    return Opcodes.LOAD == Opcodes.instMode[opcode];
  }

  /**
   * Return true if the instruction is a load.
   */
  public final boolean isStore()
  {
    return Opcodes.STORE == Opcodes.instMode[opcode];
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

  public void remapRegisters(int[] map)
  {
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    return nullified();
  }

  /**
   * Generate a String representation of a Displacement that can be used by the
   * assembly code generater.
   */
  public String assembleDisp(Assembler asm, Displacement disp, int ftn, boolean macosx)
  {
    if (ftn == PPCGenerator.FT_NONE) {
          String dispString = disp.assembler(asm);
          if (dispString == "")
            return "0";
          else
            return dispString;
        }
        if (macosx) {
          StringBuffer buf = new StringBuffer("");
          buf.append(PPCGenerator.ftnsMacosx[ftn]);
          buf.append('(');
          buf.append(disp.assembler(asm));
          buf.append(')');
          return buf.toString();
        }
    StringBuffer buf = new StringBuffer("");
    buf.append(disp.assembler(asm));
        buf.append('@');
        buf.append(PPCGenerator.ftnsLinux[ftn]);
    return buf.toString();
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
