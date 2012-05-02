package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Alpha Branch instructions.
 * <p>
 * $Id: BranchInstruction.java,v 1.29 2007-10-04 19:57:51 burrill Exp $
 * <p>
 * Copyright 2005 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class BranchInstruction extends Branch
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.BranchInstruction", stats);
  }

  private int          opcode;       // The instruction opcode.
  private int          ra;           // The ra register.
  private Displacement displacement; // The displacement.

  public BranchInstruction(int opcode, int ra, Displacement displacement, int numTargets)
  {
    super(numTargets);
    this.opcode       = opcode;
    this.ra           = ra;
    this.displacement = displacement;
    createdCount++;
  }

  /**
   * Map the virtual registers referenced in the instruction to the
   * specified real registers.  The mapping is specified using an
   * array that is indexed by the virtual register to return the real
   * register.
   * @param map maps from the virtual register to real register
   */
  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
    ra = map[ra];
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
    super.remapSrcRegister(oldReg, newReg);
    if ((opcode == Opcodes.BSR) || (opcode == Opcodes.BR))
      return;
    if (ra == oldReg)
      ra = newReg;
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
    super.remapDestRegister(oldReg, newReg);
    if ((opcode == Opcodes.BSR) || (opcode == Opcodes.BR)) {
      if (ra == oldReg)
        ra = newReg;
    }
  }

  public int getOpcode()
  {
    return opcode;
  }

  protected void setOpcode(int opcode)
  {
    this.opcode = opcode;
  }

  public int getTestRegister()
  {
    return ra;
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
    return AlphaMachine.FU_BRANCH;
  }

  /**
   * Specify the registers used by this instruction.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator rs, int index, int strength)
  {
    super.specifyRegisterUsage(rs, index, strength);

    if ((opcode == Opcodes.BSR) || (opcode == Opcodes.BR))
      rs.defRegister(index, ra);
    else
      rs.useRegister(index, ra, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (super.uses(register, registers))
      return true;
    if ((opcode == Opcodes.BSR) || (opcode == Opcodes.BR))
      return false;
    return (register == ra);
  }

  /**
   * Return true if the instruction sets the register
   */
  public boolean defs(int register, RegisterSet registers)
  {
    if (super.defs(register, registers))
      return true;
    if ((opcode == Opcodes.BSR) || (opcode == Opcodes.BR))
      return (register == ra);
    return false;
  }

  /**
   * Return true if the instruction clobbers the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return super.mods(register, registers);
  }

  /**
   * After all of the instructions have been created, the Alpha Branch
   * displacements must be calculated.  This must be done prior to
   * generating the assembly file.
   */
  public void setDisplacement(Displacement displacement)
  {
    this.displacement = displacement;
  }
  
  /**
   * Return the number of bytes required for the BranchInstruction.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (opcode == Opcodes.BEQ) {
      if (ra == AlphaRegisterSet.I0_REG) {
        emit.emit("br\t");
        emit.emit(displacement.assembler(asm));
        return;
      }
    } else if (opcode == Opcodes.BR) {
      emit.emit("br\t");
      emit.emit(displacement.assembler(asm));
      return;
    }

    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');
    emit.emit(displacement.assembler(asm));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t$");
    buf.append(ra);
    buf.append(',');
    buf.append(displacement);
    buf.append(super.toString());
    return buf.toString();
  }
}
