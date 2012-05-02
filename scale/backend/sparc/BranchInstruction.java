package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc Branch on condition code instructions.
 * <p>
 * $Id: BranchInstruction.java,v 1.18 2005-03-24 13:56:52 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Instance=02, Op=00, op2=x10
 * <p>
 * It is important that the branch target always be target 0 and the fall through target be target 1.
 * @see scale.backend.Branch#addTarget
 */

public class BranchInstruction extends SparcBranch
{
  /**
   * the displacement.
   */
  private Displacement displacement;

  public BranchInstruction(int opcode, Displacement displacement, boolean annulled, int numTargets, SparcInstruction delaySlot)
  {
    super(opcode, annulled, false, numTargets, delaySlot);
    this.displacement = displacement;
  }

  /**
   * After all of the instructions have been created, the Sparc Branch displacements must be calculated.
   * This must be done prior to generating the assembly file.
   */
  public void setDisplacement(Displacement displacement)
  {
    this.displacement = displacement;
  }
  
  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    if (inst.isBranch())
      return false;   
    if (!(inst instanceof SparcInstruction))
      return true;
    SparcInstruction si = (SparcInstruction) inst;
    return !si.setsCC(SparcGenerator.ICC);
  }

  /**
   * Insert the assembler representation of the instruction into the output stream. 
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    if (annulled)
      emit.emit(",a");
    emit.emit('\t');
    emit.emit(displacement.assembler(asm));
    assembleDelay(asm, emit);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    if (annulled)
      buf.append(",a");
    buf.append("\t");
    buf.append(displacement);
    buf.append(super.toString());
    delayToStringBuf(buf);
    return buf.toString();
  }

  /**
   * Return true if the branch can be annulled.
   */
  public boolean canBeAnnulled()
  {
    return true;
  }

  /**
   * Return true if the branch is an unconditional transfer of control to a new address.
   */
  public boolean isUnconditional()
  {
    return (opcode == Opcodes.BA)|| (opcode == Opcodes.FBA);
  }
}
