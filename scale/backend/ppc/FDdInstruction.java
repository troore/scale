package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a destination register and a displacement.
 * <p>
 * $Id: FDdInstruction.java,v 1.5 2006-11-09 00:56:04 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FDdInstruction extends FDInstruction
{
  /**
   * The constant value.
   */
  protected Displacement displacement;
  /**
   * 
   */
  private int dftn;
  /**
   *
   */
  private boolean macosx;

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param displacement is the displacement
   * @param dftn specifies the loader operation on the displacement
   * @param macosx boolean specifies macosx if true, linux if false
   */
  public FDdInstruction(int opcode, int rd, Displacement displacement, int dftn, boolean macosx)
  {
    super(opcode, rd);
    this.displacement = displacement;
        this.dftn = dftn;
        this.macosx = macosx;
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
    emit.emit(assembleDisp(asm, displacement, dftn, macosx));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append(rd);
    buf.append(',');
    buf.append(PPCGenerator.displayDisp(displacement, dftn, macosx));
    return buf.toString();
  }
}
