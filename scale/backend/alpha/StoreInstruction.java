package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Alpha store instructions.
 * <p>
 * $Id: StoreInstruction.java,v 1.23 2005-02-07 21:27:21 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class StoreInstruction extends MemoryInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.StoreInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  public StoreInstruction(int opcode, int ra, int rb)
  {
    this(opcode, ra, rb, null);
  }

  public StoreInstruction(int opcode, int ra, int rb, Displacement displacement)
  {
    this(opcode, ra, rb, displacement, AlphaGenerator.RT_NONE);
  }

  public StoreInstruction(int opcode, int ra, int rb, Displacement displacement, int relocType)
  {
    super(opcode, ra, rb, displacement, relocType);
    createdCount++;
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (ra == oldReg)
      ra = newReg;
    if (rb == oldReg)
      rb = newReg;
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
  }

  /**
   * Return true if this is a store-into- memory instruction.
   */
  public boolean isStore()
  {
    return true;
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
    rs.useRegister(index, ra, strength);
    rs.useRegister(index, rb, strength);
    if (rb == AlphaRegisterSet.SP_REG)
      rs.modRegister(index, AlphaRegisterSet.AT_REG);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == rb) || (register == ra);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return (register == AlphaRegisterSet.AT_REG) && (rb == AlphaRegisterSet.SP_REG);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(ra, registers))
      return false;
    if (inst.defs(rb, registers))
      return false;    
    return !(inst.isLoad() || inst.isStore());
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
}
