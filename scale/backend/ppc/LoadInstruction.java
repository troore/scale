package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC Load instructions.
 * <p>
 * $Id: LoadInstruction.java,v 1.4 2006-10-04 13:59:21 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class LoadInstruction extends MemoryInstruction
{
  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   */
  public LoadInstruction(int opcode, int rd, int ra)
  {
    super(opcode, rd, ra);
  }

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param disp is the displacement
   */
  public LoadInstruction(int opcode, int rd, int ra, Displacement disp)
  {
    super(opcode, rd, ra, disp);
  }
  
  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the source register
   * @param disp is the displacement
   */
  public LoadInstruction(int opcode, int rd, int ra, Displacement disp, int dftn, boolean macosx)
  {
    super(opcode, rd, ra, disp, dftn, macosx);
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return rd;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    int[] src = new int[1];
    src[0] = ra;
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
    rs.defRegister(index, rd);
    super.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return (register == rd);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (register == rd);
  }
}
