package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a constant value.
 * <p>
 * $Id: FcccInstruction.java,v 1.5 2006-11-09 00:56:05 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FcccInstruction extends FccInstruction
{
  /**
   * The third constant value.
   */
  protected int cv3;

  /**
   * @param opcode is the instruction's opcode
   * @param cv1 is the first constant value
   * @param cv2 is the second constant value
   * @param cv3 is the third constant value
   */
  public FcccInstruction(int opcode, int cv1, int cv2, int cv3)
  {
    super(opcode, cv1, cv2);
    this.cv3 = cv3;
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
    emit.emit("0x" + Integer.toHexString(cv1));
    emit.emit(',');
    emit.emit("0x" + Integer.toHexString(cv2));
    emit.emit(',');
    emit.emit("0x" + Integer.toHexString(cv3));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append("0x" + Integer.toHexString(cv1));
    buf.append(',');
    buf.append("0x" + Integer.toHexString(cv2));
    buf.append(',');
    buf.append("0x" + Integer.toHexString(cv3));
    return buf.toString();
  }
}
