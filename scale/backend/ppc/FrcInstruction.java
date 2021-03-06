package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a source register
 * and a constant value.
 * <p>
 * $Id: FrcInstruction.java,v 1.6 2006-11-09 00:56:05 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FrcInstruction extends FrInstruction
{
  /**
   * The constant value.
   */
  protected long cv1;

  /**
   * @param opcode is the instruction's opcode
   * @param ra is the source register
   * @param cv1 is the constant value
   */
  public FrcInstruction(int opcode, int ra, long cv1)
  {
    super(opcode, ra);
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
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');
    emit.emit("0x" + Long.toHexString(cv1));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append(ra);
    buf.append(',');
    buf.append("0x" + Long.toHexString(cv1));
    return buf.toString();
  }
}
