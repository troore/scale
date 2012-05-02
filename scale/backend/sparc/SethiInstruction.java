package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc SETHI and NOP instructions.
 * <p>
 * $Id: SethiInstruction.java,v 1.22 2006-10-04 13:59:16 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=01, Op=00, op2=100
 */

public class SethiInstruction extends SparcInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.SethiInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * The function applied to the displacement (e.g., %hi, %lo, etc).
   */
  protected int dftn;

  /**
   * the rd register.
   */
  protected int rd;
  /**
   * A symbolic representation of the displacement
   */
  private Displacement displacement;

  public SethiInstruction(int opcode, int rd, Displacement displacement)
  {
    this(opcode, rd, displacement, SparcGenerator.FT_HI);
  }

  public SethiInstruction(int opcode, int rd, Displacement displacement, int dftn)
  {
    super(opcode);
    this.rd           = rd;
    this.displacement = displacement;
    this.dftn         = dftn;

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
    return null;
  }

  public void remapRegisters(int[] map)
  {
    rd = map[rd];
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
    if (rd == oldReg)
      rd = newReg;
  }

  public void assembler(Assembler asm, Emit emit)
  {
    if (rd == SparcRegisterSet.G0_REG) {
      emit.emit("nop");
      return;
    }
    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(assembleDisp(asm, displacement, dftn));
    emit.emit(',');
    emit.emit(asm.assembleRegister(rd));
  }

  public String toString()
  {
    if (rd == SparcRegisterSet.G0_REG)
      return "nop";

    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append(SparcGenerator.displayDisp(displacement, dftn));
    buf.append(",%");
    buf.append(rd);
    return buf.toString();
  }

  /**
   * Mark the instruction so that it is not used.
   */
  public void nullify(RegisterSet rs)
  {
    if (rd == SparcRegisterSet.G0_REG)
      return; // Don't nullify NOPS
    super.nullify(rs);
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
    if (rd == SparcRegisterSet.G0_REG)
      return false; // NOP instruction
    if (inst.uses(rd, registers))
      return false;
    if (!(inst instanceof SparcInstruction))
      return true;

    SparcInstruction si = (SparcInstruction) inst;
    return independentCC(si);
  }
}
