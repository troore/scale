package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC Memory-format instructions.
 * <p>
 * $Id: MemoryInstruction.java,v 1.6 2006-11-09 00:56:06 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class MemoryInstruction extends FrInstruction
{
  /**
   * The destination/source register
   */
  protected int rd;
  /**
   * The constant value.
   */
  protected Displacement displacement;
  /**
   *
   */
  protected int dftn;
  /**
   *
   */
  protected boolean macosx;

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   */
  public MemoryInstruction(int opcode, int rd, int ra)
  {
    super(opcode, ra);
    this.rd = rd;
    this.dftn = PPCGenerator.FT_NONE;
  }

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param disp is the displacement
   */
  public MemoryInstruction(int opcode, int rd, int ra, Displacement disp)
  {
    super(opcode, ra);
    this.rd = rd;
    displacement = disp;
    this.dftn = PPCGenerator.FT_NONE;
  }
  
  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param disp is the displacement
   */
  public MemoryInstruction(int opcode, int rd, int ra, Displacement disp, int dftn, boolean macosx)
  {
    super(opcode, ra);
    this.rd = rd;
    displacement = disp;
    this.dftn = dftn;
    this.macosx = macosx;
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (nullified())
      emit.emit("nop # ");

    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(rd));
    emit.emit(',');

    if (displacement != null) {
      emit.emit(assembleDisp(asm, displacement, dftn, macosx));
    }

    emit.emit('(');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(')');
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t$");
    buf.append(rd);
    buf.append(',');
    buf.append(displacement);
    buf.append("($");
    buf.append(ra);
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst instanceof MemoryInstruction)
      return false;
    return super.independent(inst, registers);
  }
  
  public void remapRegisters(int[] map)
  {
    rd = map[rd];
    super.remapRegisters(map);
  }
}
