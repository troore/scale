package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Alpha jmp instructions.
 * <p>
 * $Id: JmpInstruction.java,v 1.28 2006-02-06 21:07:11 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public final class JmpInstruction extends Branch
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.JmpInstruction", stats);
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
   * the displacement
   */
  protected Displacement displacement;

  public JmpInstruction(int opcode, int ra, int rb, int numTargets)
  {
    this(opcode, ra, rb, null, numTargets);
  }

  public JmpInstruction(int opcode, int ra, int rb, Displacement displacement, int numTargets)
  {
    super(numTargets);
    this.opcode = opcode;
    this.ra = ra;
    this.rb = rb;
    this.displacement = displacement;
    createdCount++;
  }

  public JmpInstruction(int opcode, int rb, int numTargets)
  {
    this(opcode, AlphaRegisterSet.I0_REG, rb, numTargets);
  }

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return ra;
  }
  
  /**
   * Return the number of cycles that this instruction requires.
   */
  public int getExecutionCycles()
  {
    return Opcodes.getExecutionCycles(opcode);
  }

  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
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
    super.remapSrcRegister(oldReg, newReg);
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
    super.remapDestRegister(oldReg, newReg);
    if (ra == oldReg)
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
    super.specifyRegisterUsage(rs, index, strength);

    rs.defRegister(index, ra);
    rs.useRegister(index, rb, strength);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return (register == rb) || super.uses(register, registers);
  }

  /**
   * Return true if the instruction sets the register
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return (register == ra) || super.defs(register, registers);
  }

  /**
   * Return true if the instruction clobbers the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return super.mods(register, registers);
  }

  /**
   * Return the number of bytes required for the instruction.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');
    emit.emit('(');
    emit.emit(asm.assembleRegister(rb));
    emit.emit(')');

    if (displacement instanceof SymbolDisplacement) {
      emit.emit(',');
      emit.emit(displacement.assembler(asm));
      emit.emit(((AlphaAssembler) asm).relocationInfo(displacement, AlphaGenerator.RT_LITUSE_JSR));
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t$");
    buf.append(ra);
    buf.append(',');
    buf.append("($");
    buf.append(rb);
    buf.append(')');
    buf.append(super.toString());
    return buf.toString();
  }
}

