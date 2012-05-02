package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Alpha load address instructions.
 * <p>
 * $Id: LoadAddressInstruction.java,v 1.34 2006-11-09 00:56:09 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class LoadAddressInstruction extends MemoryInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.alpha.LoadAddressInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  public LoadAddressInstruction(int opcode, int ra, int rb)
  {
    this(opcode, ra, rb, null);
  }

  public LoadAddressInstruction(int opcode, int ra, int rb, Displacement displacement)
  {
    this(opcode, ra, rb, displacement, AlphaGenerator.RT_NONE);
  }

  public LoadAddressInstruction(int opcode, int ra, int rb, Displacement displacement, int relocType)
  {
    super(opcode, ra, rb, displacement, relocType);
    createdCount++;
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

  /**
   * Return the number of the functional unit required to execute this
   * instruction.
   */
  public int getFunctionalUnit()
  {
    return AlphaMachine.FU_INTALU;
  }

  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(asm.assembleRegister(ra));
    emit.emit(',');

    if (displacement != null) {
      Displacement d = displacement;
      if (d instanceof OffsetDisplacement)
        d = ((OffsetDisplacement) d).getBase();
      if (d instanceof StackDisplacement) {
        long disp = displacement.getDisplacement();
        if (opcode == Opcodes.LDA) {
          emit.emit("0x");
          emit.emit(Long.toHexString(disp & 0xffff));
        } else {
          emit.emit("0x");
          emit.emit(Long.toHexString((disp >> 16) & 0xffff));
        }
      } else if (displacement.isNumeric()) {
        long disp = displacement.getDisplacement();
        emit.emit((disp << 48) >> 48);
      } else
        emit.emit(displacement.assembler(asm));
    }
    emit.emit('(');
    emit.emit(asm.assembleRegister(rb));
    emit.emit(')');

    if (displacement != null)
      emit.emit(((AlphaAssembler) asm).relocationInfo(displacement, relocType));
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
    if (rs.virtualRegister(ra) && !isMandatory())
      super.nullify(rs);
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    if (nullified())
      return true;

    if (ra == AlphaRegisterSet.I0_REG)
      return true;

    if (displacement == null)
      return true;

    if (!displacement.isNumeric() || (ra != rb))
      return false;

    Displacement d = displacement;
    if (d instanceof OffsetDisplacement)
      d = ((OffsetDisplacement) d).getBase();
    long disp = displacement.getDisplacement();

    if (d instanceof StackDisplacement) {
      if (opcode == Opcodes.LDA)
        disp = disp & 0xffff;
      else
        disp = (disp >> 16) & 0xffff;
    }

    return (disp == 0);
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
    return (register == rb);
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
    return !inst.defs(rb, registers);    
  }
}
