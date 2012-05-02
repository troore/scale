package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Mips integer arithmetic instructions.
 * <p>
 * $Id: IntOpInstruction.java,v 1.14 2006-11-09 00:56:07 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class IntOpInstruction extends MipsInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.mips.IntOpInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the instruction opcode
   */
  private int opcode;

  /**
   * the ra register.
   */
  protected int ra;

  /**
   * the rb register
   */
  protected int rb;

  /**
   * the rc register
   */
  protected int rc;

  public IntOpInstruction(int opcode, int ra, int rb, int rc)
  {
    super(opcode);
    createdCount++;
    this.opcode   = opcode;
    this.ra       = ra;
    this.rb       = rb;
    this.rc       = rc;
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return ra;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    int[] src = new int[2];
    src[0] = rb;
    src[1] = rc;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    ra = map[ra];
    rb = map[rb];
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
    if (rb == oldReg)
      rb = newReg;
    if (rc == oldReg)
      rc = newReg;
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    if (ra == oldReg)
      ra = newReg;
  }

  public int getOpcode()
  {
    return opcode;
  }

  public boolean match(int ra, int rb, int rc)
  {
    return (this.ra == ra) && (this.rb == rb) && (this.rc == rc);
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
    rs.useRegister(index, rb, strength);
    rs.useRegister(index, rc, strength);
    rs.defRegister(index, ra);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == rb) || (register == rc);
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (register == ra);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(rb, registers))
      return false;
    if (inst.defs(rc, registers))
      return false;
    if (inst.uses(ra, registers))
      return false;
    return true;    
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    if (nullified())
      return true;
    return (ra == MipsRegisterSet.ZERO_REG) ||
           ((opcode == Opcodes.OR) && (ra == rc) && (ra == rb || rb == MipsRegisterSet.ZERO_REG)) ||
        ((opcode == Opcodes.ADD || opcode == Opcodes.ADDU || opcode == Opcodes.DADD || opcode == Opcodes.DADDU) &&
         ((ra == rc && rb == MipsRegisterSet.ZERO_REG) || (ra == rb && rc == MipsRegisterSet.ZERO_REG))); // Should clean up code -Jeff
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    if (nullified())
      emit.emit("nop # ");

    emit.emit(Opcodes.getOp(opcode));
    emit.emit('\t');
    emit.emit(gen.assembleRegister(ra));
    emit.emit(',');
    emit.emit(gen.assembleRegister(rb));
    emit.emit(',');
    emit.emit(gen.assembleRegister(rc));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t$");
    buf.append(ra);
    buf.append(",$");
    buf.append(rb);
    buf.append(",$");
    buf.append(rc);

    return buf.toString();
  }
}


