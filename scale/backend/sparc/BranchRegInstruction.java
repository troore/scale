package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc Branch on register instructions.
 * <p>
 * $Id: BranchRegInstruction.java,v 1.23 2007-10-04 19:57:56 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=04, Op=00, op2=011
 * <p>
 * It is important that the branch target always be target 0 and the
 * fall through target be target 1.
 * @see scale.backend.Branch#addTarget
 */

public class BranchRegInstruction extends SparcBranch
{

  /**
   * the rs1 register.
   */
  private int rs1;

  /**
   * the displacement.
   */
  private Displacement displacement;

  public BranchRegInstruction(int              opcode,
                              int              rs1,
                              Displacement     displacement,
                              boolean          annulled,
                              int              numTargets,
                              SparcInstruction delaySlot)
  {
    super(opcode, annulled, false, numTargets, delaySlot);
    this.rs1          = rs1;
    this.displacement = displacement;
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
    int[] src = new int[1];
    src[0] = rs1;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    rs1 = map[rs1];
    super.remapRegisters(map);
  }

  /**
   * Map the registers used in the instruction as sources to the
   * specified register.  If the register is not used as a source
   * register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    super.remapSrcRegister(oldReg, newReg);
    if (rs1 == oldReg)
      rs1 = newReg;
    super.remapSrcRegister(oldReg, newReg);
  }

  public int getTestRegister()
  {
    return rs1;
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
    super.specifyRegisterUsage(rs, index, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == rs1) || super.uses(register, registers);
  }

  /**
   * After all of the instructions have been created, the Sparc Branch
   * displacements must be calculated.  This must be done prior to
   * generating the assembly file.
   */
  public void setDisplacement(Displacement displacement)
  {
    this.displacement = displacement;
  }
  
  /**
   * Return true if this instruction is independent of the specified
   * instruction.  If instructions are independent, than one
   * instruction can be moved before or after the other instruction
   * without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.isBranch())
      return false;   
    if (!(inst instanceof SparcInstruction))
      return true;
    SparcInstruction si = (SparcInstruction) inst;
    return !si.defs(rs1, registers);
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    if (annulled)
      emit.emit(",a");
    emit.emit('\t');
    emit.emit(asm.assembleRegister(rs1));
    emit.emit(',');
    emit.emit(displacement.assembler(asm));
    assembleDelay(asm, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    if (annulled)
      buf.append(",a");
    buf.append("\t%");
    buf.append(rs1);
    buf.append(',');
    buf.append(displacement);
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }

  /**
   * Return true if the branch can be annulled.
   */
  public boolean canBeAnnulled()
  {
    return true;
  }

  /**
   * Return true if the branch is an unconditional transfer of control
   * to a new address.
   */
  public boolean isUnconditional()
  {
    return false;
  }
}
