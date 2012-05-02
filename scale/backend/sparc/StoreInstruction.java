package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc store instructions.
 * <p>
 * $Id: StoreInstruction.java,v 1.22 2006-10-04 13:59:16 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=05,09 Op=1x
 */

public class StoreInstruction extends SparcInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.StoreInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the rd register.
   */
  protected int rd;

  /**
   * the rs1 register
   */
  protected int rs1;

  /**
   * the rs2 register
   */
  protected int rs2;

  public StoreInstruction(int opcode, int rs1, int rs2, int rd)
  {
    super(opcode);
    this.rd   = rd;
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
    int[] src = new int[3];
    src[0] = rs1;
    src[1] = rs2;
    src[2] = rd;
    return src;
  }

  /**
   * Return true if this is a store-into-memory instruction.
   */
  public boolean isStore()
  {
    return true;
  }

  public void remapRegisters(int[] map)
  {
    rd = map[rd];
    rs1 = map[rs1];
    rs2 = map[rs2];
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (rs1 == oldReg)
      rs1 = newReg;
    if (rs2 == oldReg)
      rs2 = newReg;
    if (rd == oldReg)
      rd = newReg;
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
    rs.useRegister(index, rd, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    int ars1 = registers.actualRegister(rs1);
    int ars2 = registers.actualRegister(rs2);
    int ard  = registers.actualRegister(rd);
    int areg = registers.actualRegister(register);
    return (areg == ars1) || (areg == ars2) || Opcodes.uses(opcode, ard, areg);
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
    if (inst.defs(rd, registers))
      return false;
    return !(inst.isLoad() || inst.isStore());
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(gen.assembleRegister(rd));
    emit.emit(",[");
    emit.emit(gen.assembleRegister(rs1));
    emit.emit('+');
    emit.emit(gen.assembleRegister(rs2));
    emit.emit(']');
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t%");
    buf.append(rd);
    buf.append(",[%");
    buf.append(rs1);
    buf.append("+%");
    buf.append(rs2);
    buf.append("]");

    return buf.toString();
  }
}
