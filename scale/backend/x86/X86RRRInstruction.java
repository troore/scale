package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 instructions except branches
 * that reference three registers.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86RRRInstruction extends X86RRInstruction
{
  protected int reg3;

  public X86RRRInstruction(int opcode, int reg, int reg2, int reg3)
  {
    super(opcode, reg, reg2);
    this.reg3 = reg3;
  }

  protected boolean checkForm(int opcode)
  {
    return ((0 != (opcode & Opcodes.F_RRR)) &&
            (0 == (opcode & Opcodes.F_BRANCH)));
  }

  public int getReg3()
  {
    return reg3;
  }

  public void setReg3(int reg3)
  {
    this.reg3 = reg3;
  }

  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);

    if (reg3 != -1)
      reg3 = map[reg3];
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
    if (reg3 == oldReg)
      reg3 = newReg;
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (super.uses(register, registers))
      return true;
    return (register == reg3);
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

    ((X86Assembler) asm).buildAddress(emit, getReg2(), reg3, null, getScale());
    emit.emit(',');
    emit.emit(asm.assembleRegister(getReg()));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(getOperandSizeLabel());
    buf.append('\t');
    buildAddress(buf, getReg2(), reg3, null, getScale());
    buf.append(',');
    buf.append(getReg());
    return buf.toString();
  }
}
