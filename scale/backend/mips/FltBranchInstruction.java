package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents the Mips brach on float cc instructions.
 * <p>
 * $Id: FltBranchInstruction.java,v 1.11 2006-10-04 13:59:18 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FltBranchInstruction extends MipsBranch
{
  /**
   * A symbolic representation of the displacement
   */
  protected Displacement value;

  protected int cc;


  public FltBranchInstruction(int opcode, int cc, Displacement displacement, int numTargets, MipsInstruction delaySlot)
  {
    super(opcode, numTargets, delaySlot);
    this.value = displacement;
    this.cc    = cc;
  }

  public FltBranchInstruction(int opcode, Displacement displacement, int numTargets, MipsInstruction delaySlot)
  {
    this (opcode, MipsRegisterSet.FCC0, displacement, numTargets, delaySlot);
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return -1;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    int[] src = new int[1];
    src[0] = cc;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    cc = map[cc];
    super.remapRegisters(map);
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (cc == oldReg)
      cc = newReg;
    super.remapSrcRegister(oldReg, newReg);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (cc == register) || super.uses(register, registers);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    return (!inst.defs(cc, registers)) && (!inst.isBranch());
  }

  /**
   * Specify the registers used by this instruction.
   * @param ra is the register allocator in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator ra, int index, int strength)
  {
    ra.useRegister(index, cc, strength);
    super.specifyRegisterUsage(ra, index, strength);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit("\t");

    if (cc != MipsRegisterSet.FCC0) {
      emit.emit(asm.assembleRegister(cc));
      emit.emit(',');
    }

    emit.emit(assembleDisp(asm, value));

    assembleDelay(asm, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(",$");
    buf.append(cc);
    buf.append(',');
    buf.append(MipsGenerator.displayDisp(value, MipsGenerator.FT_NONE));
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }
}


