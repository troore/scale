package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a destination register and two constants.
 * <p>
 * $Id: FDccInstruction.java,v 1.6 2006-11-09 00:56:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FDccInstruction extends FDcInstruction
{
  /**
   * The second constant value.
   */
  protected int cv2;

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param cv is the first constant value
   * @param cv2 is the second constant value
   */
  public FDccInstruction(int opcode, int rd, int cv, int cv2)
  {
    super(opcode, rd, cv);
          this.cv2 = cv2;
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
    emit.emit("0x" + Integer.toHexString(cv));
          emit.emit(',');
          emit.emit("0x" + Integer.toHexString(cv2));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append(rd);
    buf.append(',');
    buf.append("0x" + Integer.toHexString(cv));
          buf.append(',');
          buf.append("0x" + Integer.toHexString(cv2));
    return buf.toString();
  }
}
