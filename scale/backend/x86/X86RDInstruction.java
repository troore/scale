package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 instructions except branches
 * that reference a register and a displacement.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86RDInstruction extends X86RInstruction
{
  protected Displacement disp;

  public X86RDInstruction(int opcode, int reg, Displacement disp)
  {
    super(opcode, reg);
    this.disp = disp;
  }

  public X86RDInstruction(int opcode, Displacement disp, int reg)
  {
    super(opcode + Opcodes.F_REV, reg);
    this.disp = disp;
  }

  protected boolean checkForm(int opcode)
  {
    return (0 != (opcode & Opcodes.F_RD)) && (0 == (opcode & Opcodes.F_BRANCH));
  }

  public Displacement getDisplacement()
  {
    return disp;
  }

  public void setDisplacement(Displacement disp)
  {
    this.disp = disp;
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

    if (isReversed()) {
      emit.emit(asm.assembleRegister(getReg()));
      emit.emit(',');
      ((X86Assembler) asm).buildAddress(emit, -1, -1, disp, 0);
    } else {
      ((X86Assembler) asm).buildAddress(emit, -1, -1, disp, 0);
      emit.emit(',');
      emit.emit(asm.assembleRegister(getReg()));
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(getOperandSizeLabel());
    buf.append('\t');
    if (isReversed()) {
      buf.append(getReg());
      buf.append(',');
      buildAddress(buf, -1, -1, null, 0);
    } else {
      buildAddress(buf, -1, -1, null, 0);
      buf.append(',');
      buf.append(getReg());
    }
    return buf.toString();
  }
}
