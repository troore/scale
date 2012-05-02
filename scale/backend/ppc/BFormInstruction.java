package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class represents Sparc calPowerPC I-form instructions.
 * <p>
 * $Id: BFormInstruction.java,v 1.8 2007-10-04 19:57:54 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class BFormInstruction extends PPCBranch
{
  /**
   * the displacement
   */
  private Displacement displacement;
  private int bo; // The bo field.
  private int bi; // The bi field.
  private boolean absolute; // True is displacement is absolute address.
  private boolean link; // True is link register will be set.

  public BFormInstruction(int opcode, int bo, int bi, int numTargets)
  {
    this(opcode, bo, bi, null, numTargets, false, false);
  }

  /**
   * Create a branch relative that does not set the link register.
   * @param displacement is the branch address
   * @param numTargets specifies where the number of branch targets
   */
  public BFormInstruction(int          opcode,
                          int          bo,
                          int          bi,
                          Displacement displacement,
                          int          numTargets)
  {
    this(opcode, bo, bi, displacement, numTargets, false, false);
  }

  /**
   * @param bo specifies the <code>bo</code> field of the instruction
   * @param bi specifies the <code>bi</code> field of the instruction
   * @param displacement is the branch address
   * @param numTargets specifies where the number of branch targets
   * @param absolute is true if the displacement represents an absolute address
   * @param link is true if this instruction sets the link register
   */
  public BFormInstruction(int          opcode,
                          int          bo,
                          int          bi,
                          Displacement displacement,
                          int          numTargets,
                          boolean      absolute,
                          boolean      link)
  {
    super(opcode, false, numTargets);
    this.displacement = displacement;
    this.absolute     = absolute;
    this.link         = link;
    this.bo           = bo;
    this.bi           = bi;
    this.link         = link;
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
    emit.emit(bo);
    emit.emit(",");
    emit.emit(bi);
    if (displacement != null) {
      emit.emit(",");
      emit.emit(displacement.assembler(asm));
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(Opcodes.getOp(this));
    buf.append("\t");
    buf.append(bo);
    buf.append(",");
    buf.append(bi);
    if (displacement != null) {
      buf.append(",");
      buf.append(displacement.toString());
    }
    buf.append(super.toString());
    return buf.toString();
  }
}
