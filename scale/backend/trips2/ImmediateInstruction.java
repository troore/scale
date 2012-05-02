package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/**
 * This class represents Trips non-branch instructions with an
 * immediate operand.
 * <p>
 * $Id: ImmediateInstruction.java,v 1.38 2007-10-04 19:57:59 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class ImmediateInstruction extends TripsInstruction
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.trips2.ImmediateInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * the instruction opcode.
   */
  private int opcode;

  /**
   * the ra register.
   */
  protected int ra;

  /**
   * the rb register.
   */
  protected int rb;

  /**
   * the immediate value.
   */
  protected long imm;

  /**
   * used for stack displacements.
   */
  protected Displacement disp;
  
  /**
   * Create a new Immediate Trips instruction in the I:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   * @param imm specifies the right immediate argument
   * @see scale.backend.trips2.Trips2Machine#minImmediate
   * @see scale.backend.trips2.Trips2Machine#maxImmediate
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue speficies the condition to predicate on
   * (ignored if rp is -1)
   */
  public ImmediateInstruction(int     opcode,
                              int     ra,
                              int     rb,
                              long    imm,
                              int     rp,
                              boolean predicatedOnTrue)
  {
    super(rp, predicatedOnTrue);
    
    assert (Opcodes.getFormat(opcode) == Opcodes.I1) :
      "Wrong form for I:1 instruction format.";

    this.opcode = opcode;
    this.ra     = ra;
    this.rb     = rb;
    this.imm    = imm;

    createdCount++;
  }
  
  /**
   * Create a new Immediate Trips instruction in the I:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   * @param disp specifies the right immediate argument
   * @see scale.backend.trips2.Trips2Machine#minImmediate
   * @see scale.backend.trips2.Trips2Machine#maxImmediate
   */
  public ImmediateInstruction(int opcode, int ra, int rb, Displacement disp)
  {
    assert ((Opcodes.getFormat(opcode) == Opcodes.I1) && disp.isNumeric()) :
      "Wrong form for I:1 instruction format.";

    this.opcode = opcode;
    this.ra     = ra;
    this.rb     = rb;
    this.disp   = disp;

    createdCount++;
  }

  /**
   * Create a new non-predicated Immediate Trips instruction in the
   * I:1 format.
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   * @param imm specifies the right immediate argument
   * @see scale.backend.trips2.Trips2Machine#minImmediate
   * @see scale.backend.trips2.Trips2Machine#maxImmediate
   */
  public ImmediateInstruction(int opcode, int ra, int rb, long imm)
  {
    this(opcode, ra, rb, imm, -1, false);
  }

  /**
   * Create a new Immediate Trips instruction in the I:0 format.
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param imm specifies the immediate argument
   * @see scale.backend.trips2.Trips2Machine#minImmediate
   * @see scale.backend.trips2.Trips2Machine#maxImmediate
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue speficies the condition to predicate on
   * (ignored if rp is -1)
   */
  public ImmediateInstruction(int opcode, int ra, long imm, int rp, boolean predicatedOnTrue)
  {
    super(rp, predicatedOnTrue);
    
    byte format = Opcodes.getFormat(opcode);
    if (format != Opcodes.I0)
      throw new scale.common.InternalError("Wrong form for I:0 instruction format.");

    this.opcode = opcode;
    this.ra     = ra;
    this.rb     = -1;
    this.imm    = imm;

    createdCount++;
  }

  /**
   * Create a new non-predicated Immediate Trips instruction in the I:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param imm specifies the right immediate argument
   * @see scale.backend.trips2.Trips2Machine#minImmediate
   * @see scale.backend.trips2.Trips2Machine#maxImmediate
   */
  public ImmediateInstruction(int opcode, int ra, long imm)
  {
    this(opcode, ra, imm, -1, false);
  }

  /**
   * Map the registers used in the instruction to the specified registers.
   */
  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
    ra = map[ra];
    if (rb > -1)
      rb = map[rb];
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
    if (rb == oldReg)
      rb = newReg;
  }

  /**
   * Map the registers defined in the instruction as destinations to
   * the specified register.  If the register is not used as a
   * destination register, no change is made.
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
    return opcode & 0xff;
  }

  /**
   * Return the destination register.
   */
  public int getDestRegister()
  {
    return ra;
  }
  
  /**
   * This routine returns the source registers for an instruction.
   * Null is returned if there are no source registers.
   */
  public int[] getSrcRegisters()
  {
    int[] sups = super.getSrcRegisters();
    
    if (rb > -1) {
      int   len  = sups == null ? 0 : sups.length;
      int[] srcs = new int[1 + len];
  
      srcs[0] = rb;
    
      if (len > 0)
        System.arraycopy(sups, 0, srcs, 1, len);

      return srcs;
    } 
    
    return sups;
  }
  
  /**
   * Return the instruction format.
   */
  public byte getFormat()
  {
    return Opcodes.getFormat(opcode);
  }

  /**
   * Return the ra field.
   */
  public int getRa()
  {
    return ra;
  }

  /**
   * Return the rb field.
   */
  public int getRb()
  {
    return rb;
  }

  /**
   * Return the immediate field.
   */
  public long getImm()
  {
    return imm;
  }
  
  /**
   * Return the displacement field.
   */
  public Displacement getDisp()
  {
    return disp;
  }
  
  /**
   * Set the immediate field.
   */
  protected void setImm(long imm)
  {
    this.imm = imm;
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
    if (rb > -1)
      rs.useRegister(index, rb, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (rb == register || super.uses(register, registers));
  }

  /**
   * Return true if the instruction sets the register
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (register == ra);
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
    byte format = Opcodes.getFormat(opcode);

    if (inst.uses(ra, registers))
      return false;

    if (format == Opcodes.I1 && inst.defs(rb, registers))
      return false;

    return super.independent(inst, registers);
  }

  /**
   * Return the number of bytes required for the Trips instruction.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Return true if the instruction can be deleted without changing
   * program semantics.
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
   * Return the source register of a copy instruction.
   */
  public int getCopyDest()
  {
    return ra;
  }
  
  /**
   * Return a hash code that can be used to determine equivalence.
   * The hash code does not include predicates or the destination
   * register.
   */
  public int ehash()
  {
    String       str = Opcodes.getOp(opcode);
    StringBuffer buf = new StringBuffer(str);
    
    byte format = Opcodes.getFormat(opcode);
    if (format == Opcodes.I1)
      buf.append(rb);

    if (disp != null) {
      if (disp.isStack())
        buf.append(disp.hashCode());
      else
        buf.append(disp);
    } else
      buf.append(imm);
    
    return buf.toString().hashCode();
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(formatOpcode(asm, opcode));
    emit.emit('\t');
    emit.emit(formatRa(asm, ra));

    byte format = Opcodes.getFormat(opcode);
    if (format == Opcodes.I1) { 
      emit.emit(", ");
      emit.emit(asm.assembleRegister(rb));
    }
    
    emit.emit(", ");
    if (disp != null) {
      if (disp.isNumeric())
         emit.emit(disp.getDisplacement());
      else
        emit.emit(disp.assembler(asm));
    } else
      emit.emit(imm);
  }

  /**
   * Return a string representation of the instruction.
   */
  public String toString()
  {
    StringBuffer buf = new StringBuffer("");

    buf.append(formatOpcode(opcode));
    buf.append('\t');
    buf.append(ra);

    byte format = Opcodes.getFormat(opcode);
    if (format == Opcodes.I1) {
      buf.append(' ');
      buf.append(rb);
    }

    buf.append(" #");
    if (disp != null)
      buf.append(disp);
    else
      buf.append(imm);

    return buf.toString();
  }
}
