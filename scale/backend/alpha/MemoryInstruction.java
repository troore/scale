package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Alpha memory-format instructions.
 * <p>
 * $Id: MemoryInstruction.java,v 1.28 2007-10-04 19:57:51 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class MemoryInstruction extends Instruction
{
  /**
   * The instruction opcode.
   */
  protected int opcode;

  /**
   * The ra register.
   */
  protected int ra;

  /**
   * The rb register
   */
  protected int rb;

  /**
   * A symbolic representation of the displacement
   */
  protected Displacement displacement;

  /**
   * The type of relocation
   */
  protected int relocType;

  protected MemoryInstruction(int          opcode,
                              int          ra,
                              int          rb,
                              Displacement displacement,
                              int          relocType)
  {
    super();
    this.opcode       = opcode;
    this.ra           = ra;
    this.rb           = rb;
    this.displacement = displacement;
    this.relocType    = relocType;
  }

  protected MemoryInstruction(int opcode, int ra, int rb, Displacement displacement)
  {
    this(opcode, ra, rb, displacement, AlphaGenerator.RT_NONE);
  }

  public void remapRegisters(int[] map)
  {
    ra = map[ra];
    rb = map[rb];
  }

  /**
   * Map the registers used in the instruction as sources to the
   * specified register.  If the register is not used as a source
   * register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (rb == oldReg)
      rb = newReg;
  }

  /**
   * Map the registers defined in the instruction as destinations to
   * the specified register.  If the register is not used as a
   * destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    if (ra == oldReg)
      ra = newReg;
  }

  /**
   * Return the instructions opcode.
   */
  public int getOpcode()
  {
    return opcode;
  }

  /**
   * Return the Ra register field.
   */
  public int getRa()
  {
    return ra;
  }

  /**
   * Return the Rb register field.
   */
  public int getRb()
  {
    return rb;
  }

  /**
   * Set the Rb register field.
   */
  protected void setRb(int rb)
  {
    this.rb = rb;
  }

  /**
   * Return the relocation type.
   */
  public final int getRelocType()
  {
    return relocType;
  }

  /**
   * Return the displacement.
   */
  public Displacement getDisplacement()
  {
    return displacement;
  }

  /**
   * Set the displacement.
   */
  public void setDisplacement(Displacement disp)
  {
    displacement = disp;
  }

  /**
   * Return true if this instruction is independent of the specified
   * instruction.  If instructions are independent, than one
   * instruction can be moved before or after the other instruction
   * without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return the number of bytes required for the instruction.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Return the number of cycles that this instruction requires.
   */
  public int getExecutionCycles()
  {
    return Opcodes.getExecutionCycles(opcode);
  }

  /**
   * Return the number of the functional unit required to execute this
   * instruction.
   */
  public int getFunctionalUnit()
  {
    return AlphaMachine.FU_LDST;
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (nullified())
      emit.emit("nop # ");

    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');

    if (displacement != null) {
      if (displacement.isNumeric()) {
        long disp = displacement.getDisplacement();
        if (disp != 0)
          emit.emit(disp);
      } else if ((relocType == AlphaGenerator.RT_LITERAL) &&
                 (displacement instanceof OffsetDisplacement)) {
        OffsetDisplacement od = (OffsetDisplacement) displacement;
        emit.emit(od.getBase().assembler(asm));
      } else
        emit.emit(displacement.assembler(asm));
    }

    emit.emit('(');
    emit.emit(asm.assembleRegister(rb));
    emit.emit(')');

    if (displacement != null)
      emit.emit(((AlphaAssembler) asm).relocationInfo(displacement, relocType));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t$");
    buf.append(ra);
    buf.append(',');
    buf.append(displacement);
    buf.append("($");
    buf.append(rb);
    buf.append(')');
    return buf.toString();
  }
}
