package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents pseudo instruction ENTER.
 * <p>
 * $Id: EnterInstruction.java,v 1.37 2006-11-16 17:49:39 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The Enter instruction is used to represent all constant values (integers, addresses, etc)
 * prior to the conversion of instructions from operand form to target form.
 */
public class EnterInstruction extends TripsInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = { "created" };

  static 
  {
    Statistics.register("scale.backend.trips2.EnterInstruction", stats);
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
   * the constant value
   */
  protected Displacement disp;

  /**
   * the destination register
   */
  protected int ra;
  
  /**
   * Create a new Enter instruction.
   * @param ra specifies the destination register
   * @param disp specifies the constant value
   */
  public EnterInstruction(int opcode, int ra, Displacement disp)
  {
    this(opcode, ra, disp, -1, false);
  }

  /**
   * Create a new predicated Enter instruction.
   */
  public EnterInstruction(int opcode, int ra, Displacement disp, int rp, boolean predicatedOnTrue)
  {
    super(rp, predicatedOnTrue);
    
    this.opcode = opcode;
    this.ra     = ra;
    this.disp   = disp;
    
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
    return opcode;
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
   * This routine returns the source registers for an instruction.
   * Null is returned if there are no source registers.
   */
  public int[] getSrcRegisters()
  {
    return super.getSrcRegisters();
  }

  /**
   * Return the constant value.
   */
  public Displacement getDisp()
  {
    return disp;
  }
  
  /**
   * Set the displacement value.
   */
  protected void setDisp(Displacement d)
  {
    this.disp = d;
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
   * Return the size of an ENTER instruction.
   */
  public int getSize()
  {
    if (opcode == Opcodes._ENTER)
      return Trips2Machine.enterSizes[Trips2Machine.ENTER];
    
    if (opcode == Opcodes._ENTERA)
      return Trips2Machine.enterSizes[Trips2Machine.ENTERA];

    if (opcode == Opcodes._ENTERB)
      return Trips2Machine.enterSizes[Trips2Machine.ENTERB];
   
    return 4;
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
    
    if (disp.isSymbol())
      buf.append(((SymbolDisplacement) disp).getName());
    else
      buf.append(disp);
    
    return buf.toString().hashCode();
  }
  
  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(formatOpcode(opcode));
    emit.emit('\t');
    emit.emit(formatRa(asm, ra));
    emit.emit(", ");

    String str;
    if (disp instanceof OffsetDisplacement) {
      OffsetDisplacement od = (OffsetDisplacement) disp;
      str = (new Long(od.getDisplacement())).toString();
    } else
      str = disp.assembler(asm);

    emit.emit(str.equals("") ? "0" : str);
    if ((opcode == Opcodes._ENTER) && (disp instanceof FloatDisplacement)) {
      emit.emit("\t; ");
      emit.emit(Double.toString(((FloatDisplacement) disp).getDoubleDisplacement()));
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
    buf.append(' ');
    buf.append(disp);
    
    return buf.toString();
  }
}
