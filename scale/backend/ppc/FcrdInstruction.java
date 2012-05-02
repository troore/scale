package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a destination register,
 * a source register, and a displacement.
 * <p>
 * $Id: FcrdInstruction.java,v 1.8 2006-11-09 00:56:05 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FcrdInstruction extends FrInstruction
{
  /**
   * The constant value.
   */
  protected int cv1;

  /**
   * The constant value.
   */
  protected Displacement disp;

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
   * @param cv1 is ??
   * @param ra is the source register
   * @param disp is the displacement
   * @param dftn specifies the loader operation on the displacement
   */
  public FcrdInstruction(int opcode, int cv1, int ra, Displacement disp, int dftn, boolean macosx)
  {
    super(opcode, ra);
    this.cv1 = cv1;
    this.disp = disp;
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
    emit.emit(cv1);
    emit.emit(',');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');
    emit.emit(assembleDisp(asm, disp, dftn, macosx));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append(cv1);
    buf.append(',');
    buf.append(ra);
    buf.append(',');
    buf.append(PPCGenerator.displayDisp(disp, dftn, macosx));
    return buf.toString();
  }
}
