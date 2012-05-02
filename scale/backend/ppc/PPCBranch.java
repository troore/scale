package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the abstract class for all machine PPCBranch instructions.
 * <p>
 * $Id: PPCBranch.java,v 1.4 2006-04-26 18:07:17 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class PPCBranch extends Branch
{
  /**
   * the instruction opcode
   */
  protected int opcode;
 /**
   * True branch predicted?
   */
  protected boolean pt;

  /**
   * @param numTargets is the number of successors of this instruction.
   * @param pt is true if the true condition is predicted
   * For routine calls, it does not include the routine called.
   */
  protected PPCBranch(int opcode, boolean pt, int numTargets)
  {
    super(numTargets);
    this.opcode = opcode;
    this.pt     = pt;
  }

  /**
   * Generate a String representation of a Displacement that can be used by the
   * assembly code generater.
   */
  public String assembleDisp(Assembler asm, Displacement disp, int ftn)
  {
    StringBuffer buf = new StringBuffer("");
    buf.append('(');
    buf.append(disp.assembler(asm));
    buf.append(')');
    return buf.toString();
  }

  public final int getOpcode()
  {
    return opcode;
  }

  public final void setOpcode(int opcode)
  {
    this.opcode = opcode;
  }

 /**
   * @return true if the branch is predicited to occur
   */
  public boolean getPt()
  {
    return pt;
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    return nullified();
  }

  /**
   * @return the number of bytes required for the Branch Instruction
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Return true if the branch can be annulled.
   */
  public boolean canBeAnnulled()
  {
    return false;
  }

  /**
   * Return true if the branch is an unconditional transfer of control to a new address.
   */
  public boolean isUnconditional()
  {
    return true;
  }
}
