package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a destination register,
 * a source register, and a constant value.
 * <p>
 * $Id: FDrcInstruction.java,v 1.7 2006-11-09 00:56:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FDrcInstruction extends FDrInstruction
{
  /**
   * The constant value.
   */
  protected int cv1;
  protected boolean macosx;

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param cv1 is the constant value
   * @param macosx is true for OS X and false for Linux
   */
  public FDrcInstruction(int opcode, int rd, int ra, int cv1, boolean macosx)
  {
    super(opcode, rd, ra);
    this.cv1 = cv1;
    this.macosx = macosx;
  }

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param cv1 is the constant value
   */
  public FDrcInstruction(int opcode, int rd, int ra, int cv1)
  {
    super(opcode, rd, ra);
    this.cv1 = cv1;
    this.macosx = false;
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
    // go back to here and make this far more general
    if ((ra == 0) && (opcode == Opcodes.ADDI ||
                      opcode == Opcodes.ADDIS)) {
      emit.emit('0');
    } else {
      emit.emit(asm.assembleRegister(ra));
    }
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
    buf.append("0x" + Integer.toHexString(cv1));
    return buf.toString();
  }
}
