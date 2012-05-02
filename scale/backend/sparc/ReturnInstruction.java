package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc integer arithmetic instructions 
 * with two register arguments and a function code in place of a destination register..
 * <p>
 * $Id: ReturnInstruction.java,v 1.23 2006-10-04 13:59:16 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=07, Op=1x, Op3=111001
 */

public class ReturnInstruction extends SparcBranch
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.ReturnInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the rs1 register
   */
  protected int rs1;

  /**
   * the rs2 register
   */
  protected int rs2;

  public ReturnInstruction(int rs1, int rs2, SparcInstruction delaySlot)
  {
    super(Opcodes.RETURN, false, true, 1, delaySlot);
    this.rs1  = rs1;
    this.rs2  = rs2;

    createdCount++;
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
    src[0] = rs1;
    src[1] = rs2;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    rs1 = map[rs1];
    rs2 = map[rs2];
    super.remapRegisters(map);
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
    rs.useRegister(index, rs2, strength);
    rs.useRegister(index, rs1, strength);
    super.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    int ars1 = registers.actualRegister(rs1);
    int ars2 = registers.actualRegister(rs2);
    int areg = registers.actualRegister(register);
    return (areg == ars1) || (areg == ars2) || super.uses(register, registers);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit("return");
    emit.emit('\t');
    emit.emit(asm.assembleRegister(rs1));
    emit.emit('+');
    emit.emit(asm.assembleRegister(rs2));
    assembleDelay(asm, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("return");
    buf.append("\t%");
    buf.append(rs1);
    buf.append("+%");
    buf.append(rs2);
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }
}
