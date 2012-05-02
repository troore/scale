package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 instructions except branches
 * that def a register.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86RInstruction extends X86Instruction
{
  /**
   * If the instruction modifies a register, it is specified by this
   * member.  If there is no destination register, the value is -1.
   */
  protected int reg;

  public X86RInstruction(int opcode, int reg)
  {
    super(opcode);
    this.reg = reg;
  }

  protected boolean checkForm(int opcode)
  {
    return ((0 != (opcode & Opcodes.F_R)) &&
            (0 == (opcode & Opcodes.F_BRANCH)));
  }

  public int getReg()
  {
    return reg;
  }

  public void setReg(int reg)
  {
    this.reg = reg;
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return isReversed() ? -1 : reg;
  }
  
  public void remapRegisters(int[] map)
  {
    if (reg != -1)
      reg = map[reg];
  }

  /**
   * Map the registers defined in the instruction as destinations to
   * the specified register.  If the register is not used as a
   * destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    if (!isReversed() && (reg == oldReg))
      reg = newReg;
  }

  /**
   * Map the registers used in the instruction as sources to the
   * specified register.  If the register is not used as a source
   * register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (isReversed() && (reg == oldReg))
      reg = newReg;
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
    if (reg != -1)
      if (isReversed())
        rs.useRegister(index, reg, strength);
      else
        rs.defRegister(index, reg);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return isReversed() ? (register == reg) : false;
  }

  /**
   * Return true if the instruction sets the register
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return isReversed() ? false : (register == reg);
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (nullified())
      emit.emit("nop ! ");

    emit.emit(Opcodes.getOp(this));
    emit.emit(getOperandSizeLabel());
    emit.emit('\t');

    emit.emit(asm.assembleRegister(reg));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(getOperandSizeLabel());
    buf.append('\t');
    buf.append(reg);
    return buf.toString();
  }
}
