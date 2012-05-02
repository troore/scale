package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc floating point move instructions.
 * <p>
 * $Id: FmoveInstruction.java,v 1.19 2006-10-04 13:59:15 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class FmoveInstruction extends FltOpInstruction
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.sparc.FmoveInstruction", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  private int cc;

  public FmoveInstruction(int opcode, int cc, int rs2, int rd)
  {
    super(opcode, rs2, rd);
    this.cc = cc;

    createdCount++;
  }

  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    int[] src = new int[2];
    src[0] = rs2;
    src[0] = rd;
    return src;
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
    rs.useRegister(index, rd, strength);
    rs.useRegister(index, rs2, strength);
    rs.defRegister(index, rd);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    int ars2 = registers.actualRegister(rs2);
    int ard  = registers.actualRegister(rd);
    int areg = registers.actualRegister(register);
    return Opcodes.uses(opcode, ars2, areg) || Opcodes.uses(opcode, ard, areg);
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.defs(rs2, registers))
      return false;
    if (inst.defs(rd, registers))
      return false;
    if (inst.uses(rd, registers))
      return false;
    if (!(inst instanceof SparcInstruction))
      return true;

    return independentCC((SparcInstruction) inst);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit("\t%");
    emit.emit(SparcGenerator.ccTab[cc]);
    emit.emit(',');
    emit.emit(gen.assembleRegister(rs2));
    emit.emit(',');
    emit.emit(gen.assembleRegister(rd));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append(" %");
    buf.append(SparcGenerator.ccTab[cc]);
    buf.append(",%");
    buf.append(rs2);
    buf.append(",%");
    buf.append(rd);

    return buf.toString();
  }
}


