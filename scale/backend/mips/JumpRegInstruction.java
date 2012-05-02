package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents the Mips jump, and jump & link instructions.
 * <p>
 * $Id: JumpRegInstruction.java,v 1.11 2006-10-04 13:59:19 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class JumpRegInstruction extends MipsBranch
{
  /**
   * the rs register
   */
  protected int rs;

  /**
   * whether or not the jump links
   */
  protected boolean link;

  /**
   * the rd register
   */
  protected int rd;

  public JumpRegInstruction(int opcode, int rs, int rd, int numTargets, MipsInstruction delaySlot, boolean link)
  {
    super(opcode, numTargets, delaySlot);
    this.rs = rs;
    this.rd = rd;
    this.link = link;
  }

  public JumpRegInstruction(int opcode, int rs, int numTargets, MipsInstruction delaySlot, boolean link)
  {
    this(opcode, rs, MipsRegisterSet.RA_REG, numTargets, delaySlot, link);
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return rd;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    int[] src = new int[1];
    src[0] = rs;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    rs = map[rs];
    rd = map[rd];
    super.remapRegisters(map);
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (rs == oldReg)
      rs = newReg;
    super.remapSrcRegister(oldReg, newReg);
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    if (link && (rd == oldReg))
      rd = newReg;
    super.remapDestRegister(oldReg, newReg);
  }

  public int getRs()
  {
    return rs;
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == rs) || super.uses(register, registers);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (link && (register == rd)) || super.defs(register, registers);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(rs, registers))
      return false;
    if (link && inst.uses(rd, registers))
      return false;
    return !inst.isBranch();
  }

  /**
   * Specify the registers used by this instruction.
   * @param ra is the register allocator in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator ra, int index, int strength)
  {
    if (link)
      ra.defRegister(index, rd);
    ra.useRegister(index, rs, strength);
    super.specifyRegisterUsage(ra, index, strength);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit("\t");

    if (link && rd != MipsRegisterSet.RA_REG) {
      emit.emit(asm.assembleRegister(rd));
      emit.emit(',');
    }

    emit.emit(asm.assembleRegister(rs));

    assembleDelay(asm, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t$");
    if (link && rd != MipsRegisterSet.RA_REG) {
      buf.append(rd);
      buf.append(",$");
    }
    buf.append(rs);
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }
}


