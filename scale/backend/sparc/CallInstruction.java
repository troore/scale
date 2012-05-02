package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc call instruction.
 * <p>
 * $Id: CallInstruction.java,v 1.19 2006-11-16 17:49:38 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class CallInstruction extends SparcBranch
{
  /**
   * the displacement
   */
  protected Displacement displacement;

  private int returnedStructSize; // The size of the struct returned by this call.

  public CallInstruction(int numTargets, SparcInstruction delaySlot)
  {
    this(null, numTargets, delaySlot);
  }

  public CallInstruction(Displacement displacement, int numTargets, SparcInstruction delaySlot)
  {
    super(Opcodes.CALL, false, true, numTargets, delaySlot);
    this.displacement    = displacement;
    this.returnedStructSize = 0;
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return SparcRegisterSet.O7_REG;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    return null;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    int areg = registers.actualRegister(register);
    return (areg == SparcRegisterSet.O7_REG) || super.defs(register, registers);
  }

  /**
   * Specifiy the size of the struct returned by the call or 0 if none.
   */
  protected void setReturnedStructSize(int size)
  {
    returnedStructSize = size;
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
    rs.defRegister(index, SparcRegisterSet.O7_REG);
    super.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    return !inst.isBranch();
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(displacement.assembler(asm));
    assembleDelay(asm, emit);
    if (returnedStructSize > 0) {
      emit.endLine();
      emit.emit("\t.word\t");
      emit.emit(returnedStructSize);
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t");
    buf.append(displacement.toString());
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }
}
