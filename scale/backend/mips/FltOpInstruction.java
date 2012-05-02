package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Mips floating point instructions.
 * <p>
 * $Id: FltOpInstruction.java,v 1.14 2006-11-09 00:56:07 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class FltOpInstruction extends MipsInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.mips.FltOpInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the ra register.
   */
  protected int ra;

  /**
   * the rb register.
   */
  protected int rb;

  /**
   * the rc register.
   */
  protected int rc;

  /**
   * the rd register.
   */
  protected int rd;

  protected boolean uses_rc;
  protected boolean uses_rd;

  public FltOpInstruction(int opcode, int ra, int rb, int rc, int rd)
  {
    super(opcode);
    createdCount++;
    this.ra     = ra;
    this.rb     = rb;
    this.rc     = rc;
    uses_rc     = true;
    this.rd     = rd;
    uses_rd     = true;
  }

  public FltOpInstruction(int opcode, int ra, int rb, int rc)
  {
    super(opcode);
    createdCount++;

    this.ra     = ra;
    this.rb     = rb;
    this.rc     = rc;
    uses_rc     = true;
    uses_rd     = false;
  }

  public FltOpInstruction(int opcode, int ra, int rb)
  {
    super(opcode);
    createdCount++;
    this.ra     = ra;
    this.rb     = rb;
    uses_rc     = false;
    uses_rd     = false;
  }


  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return Opcodes.reversedOperands(opcode) ? rb : ra;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    int[] src = new int[1 + (uses_rc ? 1 : 0) + (uses_rd ? 1 : 0)];
    src[0] = Opcodes.reversedOperands(opcode) ? ra : rb;
    int k = 1;
    if (uses_rc)
      src[k++] = rc;
    if (uses_rd)
      src[k++] = rd;
    return src;
  }

  public void remapRegisters(int[] map)
  {
    ra = map[ra];
    rb = map[rb];
    if (uses_rc)
      rc = map[rc];
    if (uses_rd)
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
    if (Opcodes.reversedOperands(opcode)) {
      if (ra == oldReg)
        ra = newReg;
    } else if (rb == oldReg)
      rb = newReg;
    if (uses_rc && (rc == oldReg))
      rc = newReg;
    if (uses_rd && (rd == oldReg))
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
    if (Opcodes.reversedOperands(opcode)) {
      if (rb == oldReg)
        rb = newReg;
    } else if (ra == oldReg)
      ra = newReg;
  }

  public int getOpcode()
  {
    return opcode;
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
    if (Opcodes.reversedOperands(opcode)) {
      rs.useRegister(index, ra, strength);
      rs.defRegister(index, rb);
    } else {
      rs.defRegister(index, ra);
      rs.useRegister(index, rb, strength);
    }
    if (uses_rc)
      rs.useRegister(index, rc, strength);
    if (uses_rd)
      rs.useRegister(index, rd, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (Opcodes.reversedOperands(opcode)) {
      if (register == ra)
        return true;
    } else {
      if (register == rb)
        return true;
    }
    if (register == rc && uses_rc)
      return true;
    if (register == rd && uses_rd)
      return true;
    return false;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    if (Opcodes.reversedOperands(opcode))
      return (register == rb);

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
    if (Opcodes.reversedOperands(opcode)) {
      if (inst.uses(rb, registers))
        return false;
      if (inst.defs(ra, registers))
        return false;
    } else {      
      if (inst.uses(ra, registers))
        return false;
      if (inst.defs(rb, registers))
        return false;
    }
    if (inst.defs(rc, registers) && uses_rc)
      return false;
    if (inst.defs(rd, registers) && uses_rd)
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
    if ((opcode == Opcodes.MOV_D || opcode == Opcodes.MOV_S) && (ra == rb))
      return true;
    return false;
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
    if (uses_rc) {
      emit.emit(',');
      emit.emit(gen.assembleRegister(rc));
    }
    if (uses_rd) {
      emit.emit(',');
      emit.emit(gen.assembleRegister(rd));
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(" $");
    buf.append(ra);
    buf.append(",$");
    buf.append(rb);
    if (uses_rc) {
      buf.append(",$");
      buf.append(rc);
    }
    if (uses_rd) {
      buf.append(",$");
      buf.append(rd);
    }

    return buf.toString();
  }
}


