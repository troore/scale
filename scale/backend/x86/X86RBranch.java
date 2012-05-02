package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 branches that reference just a
 * register.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86RBranch extends X86Branch
{
  protected int reg;

  public X86RBranch(int     opcode,
                    boolean pt,
                    int     numTargets,
                    int     reg)
  {
    super(opcode, pt, numTargets);
    this.reg = reg;
  }

  protected boolean checkForm(int opcode)
  {
    return (0 != (opcode & Opcodes.F_R)) && (0 != (opcode & Opcodes.F_BRANCH));
  }

  public int getReg()
  {
    return reg;
  }

  public void setReg(int reg)
  {
    this.reg = reg;
  }

  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);

    if (reg != -1)
      reg = map[reg];
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
    if (reg == oldReg)
      reg = newReg;
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (super.uses(register, registers))
      return true;
    return (register == reg);
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
