package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents the Mips jump, and jump & link instructions.
 * <p>
 * $Id: JumpLabelInstruction.java,v 1.9 2005-03-24 13:56:47 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class JumpLabelInstruction extends MipsBranch
{
  /**
   * A symbolic representation of the displacement
   */
  protected Displacement value;

  public JumpLabelInstruction(int opcode, Displacement displacement, int numTargets, MipsInstruction delaySlot, boolean link)
  {
    super(opcode, numTargets, delaySlot);
    this.value = displacement;
  }

  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (register == MipsRegisterSet.RA_REG) || super.defs(register, registers);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.uses(MipsRegisterSet.RA_REG, registers))
      return false;
    return !inst.isBranch();
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
    rs.defRegister(index, MipsRegisterSet.RA_REG);
    super.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit("\t");

    emit.emit(assembleDisp(asm, value));

    assembleDelay(asm, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    //   buf.append(MipsGenerator.displayDisp(value));
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }
}


