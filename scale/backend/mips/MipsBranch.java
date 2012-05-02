package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the abstract class for all machine MipsBranch instructions.
 * <p>
 * $Id: MipsBranch.java,v 1.15 2006-11-16 17:49:37 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class MipsBranch extends Branch
{
  /**
   * the instruction opcode
   */
  protected int opcode;
  /**
   * The delay slot instruction is kept as part of the branch.
   */
  protected MipsInstruction delaySlot;

  /**
   * @param opcode specifies the instruction opcode
   * @param numTargets is the number of successors of this instruction.
   * @param delaySlot is the delay slot instruction
   * For routine calls, it does not include the routine called.
   */
  protected MipsBranch(int opcode, int numTargets, MipsInstruction delaySlot)
  {
    super(numTargets);
    this.opcode    = opcode;
    this.delaySlot = delaySlot;
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
    if (delaySlot != null)
      delaySlot.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Map the virtual registers referenced in the instruction to the specified real registers.
   * The mapping is specified using an array that is indexed by the virtual register to return the real register.
   * @param map maps from the virtual register to real register
   */
  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
    if (delaySlot != null)
      delaySlot.remapRegisters(map);
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    super.remapSrcRegister(oldReg, newReg);
    if (delaySlot != null)
      delaySlot.remapSrcRegister(oldReg, newReg);
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    super.remapDestRegister(oldReg, newReg);
    if (delaySlot != null)
      delaySlot.remapDestRegister(oldReg, newReg);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (super.uses(register, registers))
      return true;
    if (delaySlot != null)
      return delaySlot.uses(register, registers);
    return false;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    if (super.defs(register, registers))
      return true;
    if (delaySlot != null)
      return delaySlot.defs(register, registers);
    return false;
  }

  /**
   * Return true if the instruction clobbers the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return super.mods(register, registers);
  }

  /**
   * Generate a String representation of a Displacement that can be used by the
   * assembly code generater.
   */
  public String assembleDisp(Assembler asm, Displacement disp)
  {
    // MIGHT NEED TO BE CHANGED -JEFF
    return disp.assembler(asm);
  }

  public int getOpcode()
  {
    return opcode;
  }

  protected void setOpcode(int opcode)
  {
    this.opcode = opcode;
  }

  public MipsInstruction getDelaySlot()
  {
    return delaySlot;
  }

  protected void setDelaySlot(MipsInstruction delaySlot)
  {
    this.delaySlot = delaySlot;
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    return nullified();
  }

  /**
   * Return the number of bytes required for the Branch Instruction.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Assemble the delay slot instruction.
   */
  protected final void assembleDelay(Assembler asm, Emit emit)
  {
    emit.endLine();
    emit.emit('\t');
    if (delaySlot != null)
      delaySlot.assembler(asm, emit);
    else
      emit.emit("nop");
  }

  /**
   * toString() helper method.
   */
  protected final void delayToStringBuf(StringBuffer buf)
  {
    if (delaySlot != null) {
      buf.append("; ");
      buf.append(delaySlot.toString());
    }
  }

  /**
   * Return true if the branch is an unconditional transfer of control to a new address.
   */
  public boolean isUnconditional()
  {
    return true;
  }
}
