package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Alpha load instructions.
 * <p>
 * $Id: LoadInstruction.java,v 1.27 2006-11-16 17:49:36 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class LoadInstruction extends MemoryInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.LoadInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  public LoadInstruction(int opcode, int ra, int rb)
  {
    this(opcode, ra, rb, null);
  }

  public LoadInstruction(int opcode, int ra, int rb, Displacement displacement)
  {
    this(opcode, ra, rb, displacement, AlphaGenerator.RT_NONE);
  }

  public LoadInstruction(int opcode, int ra, int rb, Displacement displacement, int relocType)
  {
    super(opcode, ra, rb, displacement, relocType);
    createdCount++;
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return ra;
  }
  
  /**
   * Return true if this is a load-from-memory instruction.
   */
  public boolean isLoad()
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
    rs.defRegister(index, ra);
    rs.useRegister(index, rb, strength);
    if ((rb == AlphaRegisterSet.SP_REG) && mods(AlphaRegisterSet.AT_REG))
      rs.modRegister(index, AlphaRegisterSet.AT_REG);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == rb);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (register == ra);
  }

  /**
   * Return true if the instruction modifies the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return (rb == AlphaRegisterSet.SP_REG) && mods(register);
  }

  private boolean mods(int register)
  {
    if (register != AlphaRegisterSet.AT_REG)
      return false;

    Displacement disp = getDisplacement();
    if (!(disp instanceof StackDisplacement))
      return false;

    // We don't use 32768 here because the displacement may not be adjusted yet.

    long d = disp.getDisplacement();
    return (d < -32000) || (d > 32000);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.uses(ra, registers))
      return false;
    if (inst.defs(rb, registers))
      return false;    
    return !(inst.isLoad() || inst.isStore());
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    if (nullified())
      return true;
    return (ra == AlphaRegisterSet.I0_REG) || (ra == AlphaRegisterSet.F0_REG);
  }
}
