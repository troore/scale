package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents the Mips brach on comparison instructions.
 * <p>
 * $Id: CmpBranchInstruction.java,v 1.12 2007-10-04 19:57:53 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class CmpBranchInstruction extends MipsBranch
{
  /**
   * A symbolic representation of the displacement
   */
  protected Displacement value;

  /**
   * the rs register
   */
  protected int rs;

  /**
   * whether or not the instruction uses the rt register
   */  
  protected boolean uses_rt;

  /**
   * the rt register
   */    
  protected int rt;

  /**
   * whether or not the branch links
   */  
  protected boolean link;

  public CmpBranchInstruction(int             opcode,
                              int             rs,
                              int             rt,
                              Displacement    displacement,
                              int             numTargets,
                              MipsInstruction delaySlot,
                              boolean         link)
  {
    super(opcode, numTargets, delaySlot);
    this.value = displacement;
    this.rs    = rs;
    this.rt    = rt;
    uses_rt    = true;
    this.link  = link;
  }

  public CmpBranchInstruction(int             opcode,
                              int             rs,
                              Displacement    displacement,
                              int             numTargets,
                              MipsInstruction delaySlot,
                              boolean         link)
  {
    super(opcode, numTargets, delaySlot);
    this.value = displacement;
    this.rs    = rs;
    uses_rt    = false;
    this.link  = link;
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
    src[0] = rs;
    src[1] = rt;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    rs = map[rs];
    if (uses_rt)
      rt = map[rt];
    super.remapRegisters(map);
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (rs == oldReg)
      rs = newReg;
    if (uses_rt && (rt == oldReg))
      rt = newReg;
    super.remapSrcRegister(oldReg, newReg);
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    super.remapDestRegister(oldReg, newReg);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == rs) || (uses_rt && (register == rt)) || super.uses(register, registers);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (link && register == MipsRegisterSet.RA_REG) || super.defs(register, registers);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(rs, registers))
      return false;
    if (uses_rt && inst.defs(rt, registers))
      return false;
    if (link && inst.uses(MipsRegisterSet.RA_REG, registers))
      return false;
    return !inst.isBranch();
  }

  /**
   * Specify the registers used by this instruction.
   * @param ra is the register allocator in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator ra, int index, int strength)
  {
    if (link)
      ra.defRegister(index, MipsRegisterSet.RA_REG);
    ra.useRegister(index, rs, strength);
    if (uses_rt)
      ra.useRegister(index, rt, strength);
    super.specifyRegisterUsage(ra, index, strength);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit("\t");

    emit.emit(asm.assembleRegister(rs));
    emit.emit(',');

    if (uses_rt) {
      emit.emit(asm.assembleRegister(rt));
      emit.emit(',');
    }

    emit.emit(assembleDisp(asm, value));

    assembleDelay(asm, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(",$");
    buf.append(rs);
    if (uses_rt) {
      buf.append(",$");
      buf.append(rt);
    }
    buf.append(',');
    buf.append(MipsGenerator.displayDisp(value, MipsGenerator.FT_NONE));
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }
}


