package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc calPowerPC I-form instructions.
 * <p>
 * $Id: IFormInstruction.java,v 1.6 2007-10-04 19:57:55 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class IFormInstruction extends PPCBranch
{
  /**
   * the displacement
   */
  protected Displacement displacement;

  private boolean absolute; // True is displacement is absolute address.
  private boolean link; // True is link register will be set.

  public IFormInstruction(int opcode, int numTargets)
  {
    this(opcode, null, numTargets, false, false);
  }

  /**
   * Create a branch relative that does not set the link register.
   * @param displacement is the branch address
   * @param numTargets specifies where the number of branch targets
   */
  public IFormInstruction(int opcode, Displacement displacement, int numTargets)
  {
    this(opcode, displacement, numTargets, false, false);
  }

  /**
   * @param displacement is the branch address
   * @param numTargets specifies where the number of branch targets
   * @param absolute is true if the displacement represents an absolute address
   * @param link is true if this instruction sets the link register
   */
  public IFormInstruction(int          opcode,
                          Displacement displacement,
                          int          numTargets,
                          boolean      absolute,
                          boolean      link)
  {
    super(opcode, false, numTargets);
    this.displacement = displacement;
    this.absolute     = absolute;
    this.link         = link;
    assert Opcodes.instForm[opcode] == Opcodes.I_FORM :
      "I-form incorrect form for " + Opcodes.opcodes[opcode] + " instruction";
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
    return !inst.isBranch();
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
    emit.emit('\t');
    emit.emit(displacement.assembler(asm));
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t");
    buf.append(displacement.toString());
    buf.append(super.toString());
    return buf.toString();
  }
}
