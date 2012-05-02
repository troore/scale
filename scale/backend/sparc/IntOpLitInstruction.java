package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc integer arithmetic instructions that use an immediate value.
 * <p>
 * $Id: IntOpLitInstruction.java,v 1.29 2006-11-09 00:56:10 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=06,10 Op=1x
 */

public class IntOpLitInstruction extends SparcInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.IntOpLitInstruction", stats);
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

  protected IntOpLitInstruction(int opcode, int rs1, Displacement value, int dftn, int rd)
  {
    super(opcode);
    this.rd    = rd;
    this.rs1   = rs1;
    this.value = value;
    this.dftn  = dftn;

    if (Opcodes.setsCC(SparcGenerator.ICC, opcode))
      setSetCC(SparcGenerator.ICC);
    if (Opcodes.usesCC(SparcGenerator.ICC, opcode))
      setUseCC(SparcGenerator.ICC);

    createdCount++;
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
    return (areg == ard);
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
    if (!(inst instanceof SparcInstruction))
      return true;

    return independentCC((SparcInstruction) inst);
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
    if (Opcodes.setsCC(SparcGenerator.ICC, opcode))
      return;
    super.nullify(rs);
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    if (nullified())
      return true;

    if ((opcode != Opcodes.OR) && (opcode != Opcodes.ADD))
      return false;

    if (!value.isZero())
      return false;

    int ars1 = registers.actualRegister(rs1);
    int ard  = registers.actualRegister(rd);
    return (ars1 == ard);
  }

  public String assembleDisp(Assembler asm, Displacement value, int dftn)
  {
    if (value.isZero())
      return "0";

    return super.assembleDisp(asm, value, dftn);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (nullified())
      emit.emit("nop ! ");

    if ((opcode == Opcodes.SUBCC) && (rd == SparcRegisterSet.G0_REG)) {
      if (value.isZero()) {
        emit.emit("tst\t");
        emit.emit(asm.assembleRegister(rs1));
        return;
      }
      emit.emit("cmp\t");
      emit.emit(asm.assembleRegister(rs1));
      emit.emit(',');
      emit.emit(assembleDisp(asm, value, dftn));
      return;
    }

    if ((opcode == Opcodes.OR) || (opcode == Opcodes.ADD)) {
      if (rs1 == SparcRegisterSet.G0_REG) {
        emit.emit("mov\t");
        emit.emit(assembleDisp(asm, value, dftn));
        emit.emit(',');
        emit.emit(asm.assembleRegister(rd));
        return;
      }
      if (value.isZero()) {
        emit.emit("mov\t");
        emit.emit(asm.assembleRegister(rs1));
        emit.emit(',');
        emit.emit(asm.assembleRegister(rd));
        return;
      }
    }

    emit.emit(Opcodes.getOp(opcode));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(rs1));
    emit.emit(',');
    emit.emit(assembleDisp(asm, value, dftn));
    emit.emit(',');
    emit.emit(asm.assembleRegister(rd));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t%");
    buf.append(rs1);
    buf.append(',');
    buf.append(SparcGenerator.displayDisp(value, dftn));
    buf.append(",%");
    buf.append(rd);

    return buf.toString();
  }
}
