package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 instructions except branches
 * that reference two registers.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86RRInstruction extends X86RInstruction
{
  protected int reg2;

  public X86RRInstruction(int opcode, int reg, int reg2)
  {
    super(opcode, reg);
    this.reg2 = reg2;
  }

  protected boolean checkForm(int opcode)
  {
    return (0 != (opcode & Opcodes.F_RR)) && (0 == (opcode & Opcodes.F_BRANCH));
  }

  public int getReg2()
  {
    return reg2;
  }

  public void setReg2(int reg2)
  {
    this.reg2 = reg2;
  }

  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);

    if (reg2 != -1)
      reg2 = map[reg2];
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (reg2 == oldReg)
      reg2 = newReg;
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == reg2);
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

    emit.emit(asm.assembleRegister(reg2));
    emit.emit(',');
    emit.emit(asm.assembleRegister(getReg()));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(getOperandSizeLabel());
    buf.append('\t');
    buf.append(getReg2());
    buf.append(',');
    buf.append(reg);
    return buf.toString();
  }
}
