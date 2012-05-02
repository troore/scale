package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the abstract class for all machine SparcBranch instructions.
 * <p>
 * $Id: SparcBranch.java,v 1.25 2006-11-16 17:49:38 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class SparcBranch extends Branch
{
  /**
   * the instruction opcode
   */
  protected int opcode;
  /**
   * Annulled?
   */
  protected boolean annulled;
  /**
   * True branch predicted?
   */
  protected boolean pt;
  /**
   * The delay slot instruction is kept as part of the branch.
   */
  protected SparcInstruction delaySlot;

  /**
   * @param numTargets is the number of successors of this instruction.
   * @param annulled is true if the next instruction is annulled
   * @param pt is true if the true condition is predicted
   * @param delaySlot is the delay slot instruction
   * For routine calls, it does not include the routine called.
   */
  protected SparcBranch(int opcode, boolean annulled, boolean pt, int numTargets, SparcInstruction delaySlot)
  {
    super(numTargets);
    this.opcode    = opcode;
    this.annulled  = annulled;
    this.pt        = pt;
    this.delaySlot = delaySlot;
  }

  /**
   * Specifiy the size of the struct returned by the call or 0 if none.
   */
  protected void setReturnedStructSize(int size)
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
    super.specifyRegisterUsage(rs, index, strength);

    if (annulled)
      return; // Annulled instructions are processed as part of an AnnulMarker.

    if (delaySlot != null)
      delaySlot.specifyRegisterUsage(rs, index, strength);
  }

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
  public String assembleDisp(Assembler asm, Displacement disp, int ftn)
  {
    if (ftn == SparcGenerator.FT_NONE)
      return disp.assembler(asm);

    StringBuffer buf = new StringBuffer(SparcGenerator.ftns[ftn]);
    buf.append('(');
    buf.append(disp.assembler(asm));
    buf.append(')');
    return buf.toString();
  }

  public int getOpcode()
  {
    return opcode;
  }

  protected void setOpcode(int opcode)
  {
    this.opcode = opcode;
  }

  public SparcInstruction getDelaySlot()
  {
    return delaySlot;
  }

  protected void setDelaySlot(SparcInstruction delaySlot)
  {
    this.delaySlot = delaySlot;
  }

  /**
   * @return true if the following instruction is annulled
   */
  public boolean getAnnulled()
  {
    return annulled;
  }

  /**
   * Specify if the following instruction is annulled.
   */
  protected void setAnnulled(boolean annulled)
  {
    this.annulled = annulled;
  }

  /**
   * @return true if the branch is predicited to occur
   */
  public boolean getPt()
  {
    return pt;
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    return nullified();
  }

  /**
   * @return the number of bytes required for the Branch Instruction
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
   * Return true if the branch can be annulled.
   */
  public boolean canBeAnnulled()
  {
    return false;
  }

  /**
   * Return true if the branch is an unconditional transfer of control to a new address.
   */
  public boolean isUnconditional()
  {
    return true;
  }
}
