package scale.backend.trips2;

import scale.backend.*;
import scale.common.*;

/**
 * This class represents Trips non-branch three operand instructions.
 * <p>
 * $Id: GeneralInstruction.java,v 1.34 2007-10-04 19:57:58 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class GeneralInstruction extends TripsInstruction 
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.trips2.GeneralInstruction", stats);
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
   * the rc register.
   */
  protected int rc;
  
  /**
   * Create a new General Trips instruction in the G:2 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   * @param rc specifies the right argument register
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue speficies the condition to predicate on
   * (ignored if rp is -1)
   */
  public GeneralInstruction(int     opcode,
                            int     ra,
                            int     rb,
                            int     rc,
                            int     rp,
                            boolean predicatedOnTrue)
  {
    super(rp, predicatedOnTrue);

    this.opcode = opcode;
    this.ra     = ra;
    this.rb     = rb;
    this.rc     = rc;
    createdCount++;
  }

  /**
   * Create a new non-predicated General Trips instruction in the G:2 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   * @param rc specifies the right argument register
   */
  public GeneralInstruction(int opcode, int ra, int rb, int rc)
  {
    this(opcode, ra, rb, rc, -1, false);
  }

  /**
   * Create a new General Trips instruction in the G:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue speficies the condition to predicate on
   * (ignored if rp is -1)
   */
  public GeneralInstruction(int opcode, int ra, int rb, int rp, boolean predicatedOnTrue)
  {
    this(opcode, ra, rb, -1, rp, predicatedOnTrue);
    
    byte format = Opcodes.getFormat(opcode);
    if (format != Opcodes.G1)
      throw new scale.common.InternalError("Wrong form for G:1 instruction format.");
  }

  /**
   * Create a new non-predicated General Trips instruction in the G:0 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue speficies the condition to predicate on (ignored if rp is -1)
   */
  public GeneralInstruction(int opcode, int ra, int rp, boolean predicatedOnTrue)
  {
    this(opcode, ra, -1, -1, rp, predicatedOnTrue);
    
    byte format = Opcodes.getFormat(opcode);
    if (format != Opcodes.G0)
      throw new scale.common.InternalError("Wrong form for G:0 instruction format.");
  }
  
  /**
   * Create a new non-predicated General Trips instruction in the G:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param ra specifies the destination register
   * @param rb specifies the left argument register
   */
  public GeneralInstruction(int opcode, int ra, int rb)
  {
    this(opcode, ra, rb, -1, -1, false);
  }

  /**
   * Map the registers used in the instruction to the specified registers.
   */
  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
    ra = map[ra];
    if ((rb > -1) && (opcode != Opcodes.NULL))
      rb = map[rb];
    if (rc > -1)
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
    if ((rb == oldReg) && (opcode != Opcodes.NULL))
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

  /**
   * Return the instruction opcode.
   */
  public int getOpcode()
  {
    return opcode & 0xff;
  }

  /**
   * Return the instruction format.
   */
  public byte getFormat()
  {
    return Opcodes.getFormat(opcode);
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
    int   len  = sups == null ? 0 : sups.length;
    int   slen = 0;
    
    if ((rb > -1) && (opcode != Opcodes.NULL))
      slen++;
    if (rc > -1)
      slen++;
   
    if (slen > 0) {
      int[] srcs = new int[slen + len];
      int   n    = 0;
      
      if ((rb > -1) && (opcode != Opcodes.NULL))
        srcs[n++] = rb;
      if (rc > -1)
        srcs[n] = rc;
      if (len > 0)
        System.arraycopy(sups, 0, srcs, slen, len);
      
      return srcs;
    }
    
    return sups;
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
   * Return the rc field.
   */
  public int getRc()
  {
    return rc;
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
    if ((rb > -1) && (opcode != Opcodes.NULL))
      rs.useRegister(index, rb, strength);
    if (rc > -1)
      rs.useRegister(index, rc, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (((rb == register) && (opcode != Opcodes.NULL)) ||
            (rc == register) ||
            super.uses(register, registers));
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

    if (format != Opcodes.G0 && inst.defs(rb, registers))
      return false;

    if (format == Opcodes.G2 && inst.defs(rc, registers))
      return false;

    return super.independent(inst, registers);
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
  }

  /**
   * Return true if the instruction copies a value from one register
   * to another without modification.  Transfers between integer and
   * floating point registers are not considered to be copy instructions.
   */
  public boolean isCopy()
  {
    if (opcode == Opcodes.MOV)
      return true;
    return false;
  }

  /**
   * Return the source register of a copy instruction.
   */
  public int getCopySrc()
  {
    if (opcode == Opcodes.MOV)
      return rb;
    return super.getCopySrc();
  }

  /**
   * Return the dest register of a copy instruction.
   */
  public int getCopyDest()
  {
    if (opcode == Opcodes.MOV)
      return ra;
    return super.getCopyDest();
  }

  /**
   * Return a hash code that can be used to determine equivalence.
   * <br>
   * The hash code does not include predicates or the destination register.
   */
  public int ehash()
  {
    String       str = Opcodes.getOp(opcode);
    StringBuffer buf = new StringBuffer(str);
   
    byte format = Opcodes.getFormat(opcode);
    if (format != Opcodes.G2) {
      if (rb > -1)
        buf.append(rb);
      return buf.toString().hashCode();
    }
   
    // Sort commutative instructions with smaller operand first.
    
    if ((rb > rc) && ((opcode == Opcodes.ADD) || 
        (opcode == Opcodes.MUL) || (opcode == Opcodes.OR) || 
        (opcode == Opcodes.AND) || (opcode == Opcodes.XOR) || 
        (opcode == Opcodes.FADD) || (opcode == Opcodes.FMUL))) {
       buf.append(rc);
       buf.append(rb);
     } else {
       buf.append(rb);
       buf.append(rc);
     }
    
    return buf.toString().hashCode();
  }
  
  /**
   * Insert the assembler representation of the instruction into the output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(formatOpcode(asm, opcode));
    emit.emit('\t');
    emit.emit(formatRa(asm, ra));

    if ((rb > -1) && (opcode != Opcodes.NULL)) {
      emit.emit(", ");
      emit.emit(asm.assembleRegister(rb));
    }
    
    byte format = Opcodes.getFormat(opcode);
    if (format == Opcodes.G2) { 
      emit.emit(", ");
      emit.emit(asm.assembleRegister(rc));
    }
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

    if (rb > -1) {
      buf.append(' ');
      buf.append(rb);
    }
    
    byte format = Opcodes.getFormat(opcode);
    if (format == Opcodes.G2) {
      buf.append(' ');
      buf.append(rc);
    }
    return buf.toString();
  }
}
