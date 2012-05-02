package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Mips MFLO and MFHI instructions.
 * <p>
 * $Id: MFSpecialInstruction.java,v 1.14 2006-11-16 17:49:37 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class MFSpecialInstruction extends MipsInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.mips.MFSpecialInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the instruction opcode
   */
  private int opcode;

  /**
   * the ra register.
   */
  protected int ra;


  private boolean hi;
  private boolean lo;

  /**
   * The delay slot instructions are kept as part of the MFLO/MFHI instructions.
   */
  protected MipsInstruction delaySlot1;
  protected MipsInstruction delaySlot2;

  public MFSpecialInstruction(int opcode, int ra, MipsInstruction delaySlot1, MipsInstruction delaySlot2)
  {
    super(opcode);

    createdCount++;
    this.opcode     = opcode;
    this.ra         = ra;
    this.delaySlot1 = delaySlot1;
    this.delaySlot2 = delaySlot2;

    lo = (opcode == Opcodes.MFLO);
    hi = (opcode == Opcodes.MFHI);
  }

  public MFSpecialInstruction(int opcode, int ra)
  {
    this(opcode, ra, null, null);
  }

  public void remapRegisters(int[] map)
  {
    ra = map[ra];
    if (delaySlot1 != null)
      delaySlot1.remapRegisters(map);
    if (delaySlot2 != null)
      delaySlot2.remapRegisters(map);
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return ra;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    return null;
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (delaySlot1 != null)
      delaySlot1.remapSrcRegister(oldReg, newReg);
    if (delaySlot2 != null)
      delaySlot2.remapSrcRegister(oldReg, newReg);
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    if (ra == oldReg)
      ra = newReg;
    if (delaySlot1 != null)
      delaySlot1.remapDestRegister(oldReg, newReg);
    if (delaySlot2 != null)
      delaySlot2.remapDestRegister(oldReg, newReg);
  }

  public int getOpcode()
  {
    return opcode;
  }

  public MipsInstruction getDelaySlot1()
  {
    return delaySlot1;
  }

  protected void setDelaySlot1(MipsInstruction delaySlot)
  {
    this.delaySlot1 = delaySlot;
  }

  public MipsInstruction getDelaySlot2()
  {
    return delaySlot2;
  }

  protected void setDelaySlot2(MipsInstruction delaySlot)
  {
    this.delaySlot2 = delaySlot;
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
    rs.defRegister(index, ra);
    if (lo)
      rs.useRegister(index, MipsRegisterSet.LO_REG, strength);
    if (hi)
      rs.useRegister(index, MipsRegisterSet.HI_REG, strength);

    if (delaySlot1 != null)
      delaySlot1.specifyRegisterUsage(rs, index, strength);
    if (delaySlot2 != null)
      delaySlot2.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (lo && register == MipsRegisterSet.LO_REG)
      return true;
    if (hi && register == MipsRegisterSet.HI_REG)
      return true;
    if (delaySlot1 != null && delaySlot1.uses(register, registers))
      return true;
    if (delaySlot2 != null && delaySlot2.uses(register, registers))
      return true;
    return false;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    if (register == ra)
      return true;
    if (delaySlot1 != null && delaySlot1.defs(register, registers))
      return true;
    if (delaySlot2 != null && delaySlot2.defs(register, registers))
      return true;
    return false;
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.uses(ra, registers))
      return false;
    if (lo && inst.defs(MipsRegisterSet.LO_REG, registers))
      return false;
    if (hi && inst.defs(MipsRegisterSet.HI_REG, registers))
      return false;
    return true;    
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    if (nullified())
      return true;
    return false;
  }

  /**
   * Assemble the delay slot instructions.
   */
  protected final void assembleDelay(Assembler asm, Emit emit)
  {
    emit.endLine();
    emit.emit('\t');
    if (delaySlot1 != null)
      delaySlot1.assembler(asm, emit);
    else
      emit.emit("nop");

    emit.endLine();
    emit.emit('\t');
    if (delaySlot2 != null)
      delaySlot2.assembler(asm, emit);
    else
      emit.emit("nop");
  }

  /**
   * toString() helper method.
   */
  protected final void delayToStringBuf(StringBuffer buf)
  {
    if (delaySlot1 != null) {
      buf.append("; ");
      buf.append(delaySlot1.toString());
    }
    if (delaySlot2 != null) {
      buf.append("; ");
      buf.append(delaySlot2.toString());
    }
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    if (nullified())
      emit.emit("nop # ");

    emit.emit(Opcodes.getOp(opcode));
    emit.emit('\t');
    emit.emit(gen.assembleRegister(ra));

    assembleDelay(gen, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t$");
    buf.append(ra);

    delayToStringBuf(buf);
    
    return buf.toString();
  }
}


