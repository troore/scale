package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC Store instructions.
 * <p>
 * $Id: StoreInstruction.java,v 1.4 2006-10-04 13:59:23 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class StoreInstruction extends MemoryInstruction {
  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   */
  public StoreInstruction(int opcode, int rd, int ra)
  {
    super(opcode, rd, ra);
  }

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param disp is the displacement
   */
  public StoreInstruction(int opcode, int rd, int ra, Displacement disp)
  {
    super(opcode, rd, ra, disp);
  }
  
  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param disp is the displacement
   */
  public StoreInstruction(int opcode, int rd, int ra, Displacement disp, int dftn, boolean macosx)
  {
    super(opcode, rd, ra, disp, dftn, macosx);
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
    int[] src = new int[2];
    src[0] = ra;
    src[0] = rd;
    return src;
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
    rs.useRegister(index, rd, strength);
    super.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
   if (register == rd)
     return true;
   return super.uses(register, registers);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(rd, registers))
      return false;    
    return super.independent(inst, registers);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
  }
  
  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (rd == oldReg)
      rd = newReg;
    super.remapSrcRegister(oldReg, newReg);
  }
}
