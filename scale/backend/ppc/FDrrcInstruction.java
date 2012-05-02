package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a destination register,
 * two source registers, and a constant.
 * <p>
 * $Id: FDrrcInstruction.java,v 1.5 2006-11-09 00:56:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FDrrcInstruction extends FDrrInstruction
{
  /**
   * The constant value.
   */
  protected int cv1;

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the left source register
   * @param rb is the right source register
   * @param cv1 is the constant value.
   */
  public FDrrcInstruction(int opcode, int rd, int ra, int rb, int cv1)
  {
    super(opcode, rd, ra, rb);
    this.cv1 = cv1;
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (nullified())
      emit.emit("nop ! ");

    emit.emit(Opcodes.getOp(opcode));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(rd));
    emit.emit(',');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');
    emit.emit(asm.assembleRegister(rb));
    emit.emit(',');
    emit.emit("0x" + Integer.toHexString(cv1));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append(rd);
    buf.append(',');
    buf.append(ra);
    buf.append(',');
    buf.append(rb);
    buf.append(',');
    buf.append("0x" + Integer.toHexString(cv1));
    return buf.toString();
  }
}
