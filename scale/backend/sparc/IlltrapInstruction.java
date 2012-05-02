package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc ILLTRAP & IMPDEP instructions.
 * <p>
 * $Id: IlltrapInstruction.java,v 1.21 2005-04-05 20:22:37 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=01, Op=00, op2=000
 */

public class IlltrapInstruction extends SparcInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.IlltrapInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  private int const22;

  public IlltrapInstruction(int opcode, int const22)
  {
    super(opcode);
    this.const22 = const22;
    createdCount++;
  }

  public void remapRegisters(int[] map)
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
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    return false;
  }

  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit("\t");
    emit.emit(const22);
    return;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t");
    buf.append(const22);
    return buf.toString();
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
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
  }
}
