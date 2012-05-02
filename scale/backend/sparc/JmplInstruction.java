package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents the Sparc jump & link instruction.
 * <p>
 * $Id: JmplInstruction.java,v 1.23 2006-11-16 17:49:38 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=05, Op=1x, op3=111000
 */

public class JmplInstruction extends SparcBranch
{
  /**
   * the rd register.
   */
  protected int rd;

  /**
   * the rs1 register.
   */
  protected int rs1;

  /**
   * the rs2 register
   */
  protected int rs2;

  private int returnedStructSize; // The size of the struct returned by this call.

  /**
   * @param numTargets      is the number of possible next instructions
   * @param delaySlot       is the delay slot instruction or null
   */
  public JmplInstruction(int rs1, int rs2, int rd, int numTargets, SparcInstruction delaySlot)
  {
    super(Opcodes.JMPL, false, true, numTargets, delaySlot);
    this.rs1          = rs1;
    this.rs2          = rs2;
    this.rd           = rd;
    this.returnedStructSize = 0;
  }

  /**
   * Specifiy the size of the struct returned by the call or 0 if none.
   */
  protected void setReturnedStructSize(int size)
  {
    returnedStructSize = size;
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
    int[] src = new int[2];
    src[0] = rs1;
    src[1] = rs2;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    rs1 = map[rs1];
    rs2 = map[rs2];
    rd  = map[rd];
    super.remapRegisters(map);
  }

  public int getRs1()
  {
    return rs1;
  }

  public int getRs2()
  {
    return rs2;
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
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    int ard  = registers.actualRegister(rd);
    int areg = registers.actualRegister(register);
    return (areg == ard) || super.defs(register, registers);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(rs1, registers))
      return false;
    if (inst.defs(rs2, registers))
      return false;
    if (inst.uses(rd, registers))
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
    rs.useRegister(index, rs2, strength);
    rs.useRegister(index, rs1, strength);
    rs.defRegister(index, rd);
    super.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit("jmpl");
    emit.emit("\t");
    emit.emit(asm.assembleRegister(rs1));
    emit.emit('+');
    emit.emit(asm.assembleRegister(rs2));
    emit.emit(',');
    emit.emit(asm.assembleRegister(rd));
    assembleDelay(asm, emit);
    if (returnedStructSize > 0) {
      emit.endLine();
      emit.emit("\t.word\t");
      emit.emit(returnedStructSize);
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("jmpl");
    buf.append("\t%");
    buf.append(rs1);
    buf.append("+%");
    buf.append(rs2);
    buf.append(",%");
    buf.append(rd);
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }
}
