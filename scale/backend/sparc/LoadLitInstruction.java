package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc load instructions.
 * <p>
 * $Id: LoadLitInstruction.java,v 1.24 2006-11-09 00:56:10 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=06,10 Op=1x
 */

public class LoadLitInstruction extends SparcInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.LoadLitInstruction", stats);
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
   * the literal value
   */
  protected Displacement value;

  /**
   * The function applied to the displacement (e.g., %hi, %lo, etc).
   */
  protected int dftn;

  /**
   * the rs1 register
   */
  protected int rs1;

  protected LoadLitInstruction(int opcode, int rs1, Displacement value, int dftn, int rd)
  {
    super(opcode);
    this.rd    = rd;
    this.rs1   = rs1;
    this.value = value;
    this.dftn  = dftn;

    createdCount++;
  }

  /**
   * Return true if this is a load-from-memory instruction.
   */
  public boolean isLoad()
  {
    return true;
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
    src[0] = rs1;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    rd = map[rd];
    rs1 = map[rs1];
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
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    if (rd == oldReg)
      rd = newReg;
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
    rs.useRegister(index, rs1, strength);
    rs.defRegister(index, rd);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    int ars1 = registers.actualRegister(rs1);
    int areg = registers.actualRegister(register);
    return (areg == ars1);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    int ard  = registers.actualRegister(rd);
    int areg = registers.actualRegister(register);
    return Opcodes.defs(opcode, ard, areg);
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
    if (inst.uses(rd, registers))
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

    int ard = registers.actualRegister(rd);
    return (ard == SparcRegisterSet.G0_REG);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit("\t[");
    emit.emit(asm.assembleRegister(rs1));
    if (!value.isZero()) {
      emit.emit('+');
      emit.emit(assembleDisp(asm, value, dftn));
    }
    emit.emit("],");
    emit.emit(asm.assembleRegister(rd));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t[%");
    buf.append(rs1);
    buf.append('+');
    buf.append(SparcGenerator.displayDisp(value, dftn));
    buf.append("],%");
    buf.append(rd);

    return buf.toString();
  }
}


