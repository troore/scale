package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 branches that reference just a
 * displacement.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86DBranch extends X86Branch
{
  protected Displacement disp;

  public X86DBranch(int opcode, boolean pt, int numTargets, Displacement disp)
  {
    super(opcode, pt, numTargets);
    this.disp = disp;
  }

  protected boolean checkForm(int opcode)
  {
    return (0 != (opcode & Opcodes.F_D)) && (0 != (opcode & Opcodes.F_BRANCH));
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
    ((X86Assembler) asm).buildAddress(emit, -1, -1, disp, 0);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(getOperandSizeLabel());
    buf.append('\t');
    buildAddress(buf, -1, -1, disp, 0);
    return buf.toString();
  }
}
