package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents pseudo instruction Phi for building SSA form.
 * <p>
 * $Id: PhiInstruction.java,v 1.16 2007-05-01 20:14:42 asmith Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class PhiInstruction extends TripsInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = { "created" };

  static 
  {
    Statistics.register("scale.backend.trips2.PhiInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the destination register
   */
  protected int ra;

  /**
   * the operands
   */
  protected int[] operands;
  /**
   * the number of operands
   */
  protected int numOperands;
  
  /**
   * Create a new Phi instruction.
   * <p>
   * @param ra specifies the destination register
   * @param numOperands specifies the number of operands
   */
  public PhiInstruction(int ra, int numOperands)
  {
    this.ra       = ra;
    this.operands = new int[numOperands];
    for (int i = 0; i < numOperands; i++)
      operands[i] = ra;
    this.numOperands = numOperands;
    createdCount++;
  }

  /**
   * Map the registers used in the instruction to the specified registers.
   */
  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
    ra = map[ra];
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
    for (int i = 0; i < numOperands; i++)
      if (operands[i] == oldReg)
        operands[i] = newReg;
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

  /**
   * Return the instruction opcode.
   */
  public int getOpcode()
  {
    return Opcodes._PHI;
  }

  /**
   * Return the ra field.
   */
  public int getRa()
  {
    return ra;
  }
  
  /**
   * Return the destination register.
   */
  public int getDestRegister()
  {
    return ra;
  }

  /**
   * Change the specified operand.
   */
  protected void setOperand(int newOperand, int position)
  {
    assert (position < numOperands) : "Invalid operand position.";
    operands[position] = newOperand;
  }

  /**
   * This routine returns the source registers for an instruction.
   * Null is returned if there are no source registers.
   */
  public int[] getSrcRegisters()
  {
    int[] sups = super.getSrcRegisters();
    int   len  = sups == null ? 0 : sups.length;
    int[] srcs = new int[numOperands + len];

    System.arraycopy(operands, 0, srcs, 0, numOperands);
    
    if (len > 0)
      System.arraycopy(sups, 0, srcs, numOperands, len);

    return srcs;
  }

  /**
   * Return the operands.
   */
  public int[] getOperands()
  {
    int[] r = new int[numOperands];
    System.arraycopy(operands, 0, r, 0, numOperands);
    return r;
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
    rs.defRegister(index, ra);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return super.uses(register, registers);
  }

  /**
   * Return true if the instruction sets the register
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
    if (inst.uses(ra, registers))
      return false;
    
    return super.independent(inst, registers);
  }

  /**
   * Return the number of bytes required for the EnterInstruction.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    if (nullified())
      return true;
    return false;
  }

  /**
   * Mark the instruction so that it is not used.
   */
  public void nullify(RegisterSet rs)
  {
  }

  /**
   * Return true if the instruction copies a value from one register 
   * to another without modification.  Transfers between integer and
   * floating point registers are not considered to be copy instructions.
   */
  public boolean isCopy()
  {
    return false;
  }

  /**
   * Return the source register of a copy instruction.
   */
  public int getCopySrc()
  {
    return super.getCopySrc();
  }

  /**
   * Return the destination register of a copy instruction.
   */
  public int getCopyDest()
  {
    return ra;
  }

  /**
   * Return true if this is a phi instruction.
   */
  public boolean isPhi()
  {
    return true;
  }
  
  /**
   * Return a hash code that can be used to determine equivalence.
   * <br>
   * The hash code does not include predicates or the destination register.
   */
  public int ehash()
  {
    String       str = Opcodes.getOp(Opcodes._PHI);
    StringBuffer buf = new StringBuffer(str);
   
    for (int i = 0; i < numOperands; i++)
      buf.append(operands[i]);
    
    return buf.toString().hashCode();
  }
  
  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(formatOpcode("phi"));
    emit.emit('\t');
    emit.emit(formatRa(asm, ra));

    for (int i = 0; i < numOperands; i++) {
      emit.emit(", ");
      emit.emit(asm.assembleRegister(operands[i]));
    }
  }

  /**
   * Return a string representation of the instruction.
   */
  public String toString()
  {
    StringBuffer buf = new StringBuffer(formatOpcode(Opcodes._PHI));
    
    buf.append('\t');
    buf.append(ra);
    
    for (int i = 0; i < numOperands; i++) {
      buf.append(' ');
      buf.append(operands[i]);
    }
    
    return buf.toString();
  }
}
