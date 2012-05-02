package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all PPC instructions that have a destination register 
 * and three source registers.
 * <p>
 * $Id: FDrrrInstruction.java,v 1.5 2006-11-09 00:56:05 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class FDrrrInstruction extends FDrrInstruction
{
  /**
   * The left source register.
   */
  protected int rc;

  /**
   * @param opcode is the instruction's opcode
   * @param rd is the destination register
   * @param ra is the left source register
   * @param rb is the right source register
   * @param rc is the third source register
   */
  public FDrrrInstruction(int opcode, int rd, int ra, int rb, int rc)
  {
    super(opcode, rd, ra, rb);
    this.rc = rc;
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
    int[] src = new int[3];
    src[0] = ra;
    src[1] = rb;
    src[2] = rc;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
    rc = map[rc];
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    super.remapSrcRegister(oldReg, newReg);
    if (rc == oldReg)
      rc = newReg;
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
    super.specifyRegisterUsage(rs, index, strength);
    rs.useRegister(index, rc, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    int ara  = registers.actualRegister(ra);
    int arb  = registers.actualRegister(rb);
    int arc  = registers.actualRegister(rc);
    int areg = registers.actualRegister(register);
    return (ara == areg) || (arb == areg) || (arc == areg);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(ra, registers))
      return false;
    if (inst.defs(rb, registers))
      return false;
    if (inst.defs(rc, registers))
      return false;
    if (inst.uses(rd, registers))
      return false;
    if (!(inst instanceof PPCInstruction))
      return true;

    return false;
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (nullified())
      emit.emit("nop ! ");

    emit.emit(Opcodes.getOp(opcode));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(rd));
    emit.emit(',');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');
    emit.emit(asm.assembleRegister(rb));
    emit.emit(',');
    emit.emit(asm.assembleRegister(rc));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append('\t');
    buf.append(rd);
    buf.append(',');
    buf.append(ra);
    buf.append(',');
    buf.append(rb);
    buf.append(',');
    buf.append(rc);
    return buf.toString();
  }
}
