package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc store instructions.
 * <p>
 * $Id: TrapInstruction.java,v 1.20 2005-03-24 13:56:56 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=05,09 Op=1x
 */

public class TrapInstruction extends SparcInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.TrapInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the cc register.
   */
  protected int cc;

  /**
   * the rs1 register
   */
  protected int rs1;

  /**
   * the rs2 register
   */
  protected int rs2;

  public TrapInstruction(int opcode, int rs1, int rs2, int cc)
  {
    super(opcode);
    this.cc   = cc;
    this.rs1  = rs1;
    this.rs2  = rs2;

    setUseCC(cc);

    createdCount++;
  }

  public void remapRegisters(int[] map)
  {
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
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    int ars1 = registers.actualRegister(rs1);
    int ars2 = registers.actualRegister(rs2);
    int areg = registers.actualRegister(register);
    return (areg == ars1) || (areg == ars2);
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
    if (!(inst instanceof SparcInstruction))
      return true;

    return independentCC((SparcInstruction) inst);
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
    emit.emit("\t%");
    emit.emit(SparcGenerator.ccTab[cc]);
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
    buf.append(SparcGenerator.ccTab[cc]);
    buf.append(",[%");
    buf.append(rs1);
    buf.append("+%");
    buf.append(rs2);
    buf.append(']');

    return buf.toString();
  }
}
