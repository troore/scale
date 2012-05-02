package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/**
 * This class represents Trips non-branch instructions for generating large constants.
 * <p>
 * $Id: ConstantInstruction.java,v 1.17 2006-11-09 00:56:11 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class ConstantInstruction extends Instruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.trips2.ConstantInstruction", stats);
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
   * the contant value.
   */
  protected int cons;

  /**
   * Create a new Contant Trips instruction in the C:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   * @param cons specifies the constant argument
   * @see scale.backend.trips2.Trips2Machine#minSignedConst
   * @see scale.backend.trips2.Trips2Machine#maxSignedConst
   * @see scale.backend.trips2.Trips2Machine#maxUnsignedConst
   */
  public ConstantInstruction(int opcode, int ra, int rb, int cons)
  {
    super();
    byte format = Opcodes.getFormat(opcode);
    if (format != Opcodes.C1)
      throw new scale.common.InternalError("Wrong form for C:1 instruction format.");
    if (cons < Trips2Machine.minSignedConst || cons > Trips2Machine.maxSignedConst)
      throw new scale.common.InternalError("Immediate value out of range for C:1 format.");

    this.opcode = opcode;
    this.ra     = ra;
    this.rb    = rb;
    this.cons   = cons;

    createdCount++;
  }

  /**
   * Create a new Contant Trips instruction in the C:0 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param cons specifies the constant argument
   * @see scale.backend.trips2.Trips2Machine#minSignedConst
   * @see scale.backend.trips2.Trips2Machine#maxSignedConst
   * @see scale.backend.trips2.Trips2Machine#maxUnsignedConst
   */
  public ConstantInstruction(int opcode, int ra, int cons)
  {
    super();
    byte format = Opcodes.getFormat(opcode);
    if (format != Opcodes.C0)
      throw new scale.common.InternalError("Wrong form for I:0 instruction format.");
    if (cons < Trips2Machine.minSignedConst || cons > Trips2Machine.maxSignedConst)
      throw new scale.common.InternalError("Immediate value out of range for C:0 format.");

    this.opcode = opcode;
    this.ra     = ra;
    this.rb     = 0;
    this.cons   = cons;
    
    createdCount++;
  }

  /**
   *  Create a new NOP instruction in the C:0 format.
   */
  public ConstantInstruction(int opcode)
  {
    super();
    if (opcode != Opcodes._NOP)
      throw new scale.common.InternalError("Wrong form for NOP instruction");

    this.opcode = opcode;
    this.ra     = 0;
    this.rb     = 0;
    this.cons   = 0;
    
    createdCount++;
  }

  public void remapRegisters(int[] map)
  {
    ra = map[ra];
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
    if (rb == oldReg)
      rb = newReg;
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
    int[] srcs = new int[1];
    srcs[0] = rb;
    return srcs;
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
   * Return the constant field.
   */
  public int getConst()
  {
    return cons;
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
    rs.defRegister(index, ra);
    rs.useRegister(index, rb, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (rb == register);
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
    byte format = Opcodes.getFormat(opcode);

    if (inst.uses(ra, registers))
      return false;

    if (format == Opcodes.C1 && inst.defs(rb, registers))
      return false;

    return true;
  }

  /**
   * Return the number of bytes required for the TripsInstruction.
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
    if (isPredicated())
      return;
    super.nullify(rs);
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
   * Insert the assembler representation of the instruction into the output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    byte format = Opcodes.getFormat(opcode);

    emit.emit(Opcodes.getOp(opcode));
    if (opcode == Opcodes._NOP)
      return;
    // emit target
    emit.emit('\t');
    emit.emit(asm.assembleRegister(ra));

    if (format == Opcodes.C1) { // emit operand 1
      emit.emit(", ");
      emit.emit(asm.assembleRegister(rb));
    }
    // emit immediate
    emit.emit(", ");
    emit.emit(cons);

    return;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("");
    byte format = Opcodes.getFormat(opcode);

    buf.append(Opcodes.getOp(opcode));
    if (opcode == Opcodes._NOP)
      return buf.toString();

    buf.append('\t');
    buf.append(ra);

    if (format != Opcodes.C1) {
      buf.append(' ');
      buf.append(rb);
    }

    buf.append(' ');
    buf.append(cons);

    return buf.toString();
  }}
