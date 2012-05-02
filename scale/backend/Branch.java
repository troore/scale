package scale.backend;

/** 
 * This is the abstract class for all machine branch instructions.
 * <p>
 * $Id: Branch.java,v 1.26 2007-10-04 19:57:48 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class Branch extends Instruction
{
  /**
   * The labels of the possible next instruction executed.
   * For routine calls, it does not include the routine called.
   */
  private Label[] successors;
  /**
   * regsUsed is the list of registers that should be marked as used.
   */
  private short[] regsUsed;
  /**
   * regsKilled is the list of registers that should be marked as killed.
   */
  private short[] regsKilled;
  /**
   * regsSet is the list of registers that should be marked as defined.
   */
  private short[] regsSet;
  /**
   * isCall is true if this branch is a call to a subroutine.
   */
  private boolean isCall;
  /**
   * branchProbability is the probability of this branch being taken.
   */
  private double branchProbability;

  /**
   * @param numTargets is the number of successors of this instruction.
   * For routine calls, it does not include the routine called.
   */
  protected Branch(int numTargets)
  {
    super();
    if (numTargets > 0)
      this.successors = new Label[numTargets];
  }
  
  /**
   * Add a successor label for this branch.
   * Generally, the branch target should be target number 0.
   */
  public final void addTarget(Label lab, int targetNumber)
  {
    successors[targetNumber] = lab;
  }

  /**
   * Specify additional registers that may be used by a subroutine call.
   */
  public final void additionalRegsUsed(short[] regsUsed)
  {
    this.regsUsed = regsUsed;
  }

  /**
   * Specify additional registers that may be killed by a subroutine call.
   */
  public final void additionalRegsKilled(short[] regsKilled)
  {
    this.regsKilled = regsKilled;
  }

  /**
   * Specify additional registers that may be killed by a subroutine call.
   */
  public final short[] getRegsKilled()
  {
    return regsKilled;
  }
  
  /**
   * Specify additional registers that may be set by a subroutine call.
   */
  public final void additionalRegsSet(short[] regsSet)
  {
    this.regsSet = regsSet;
  }

  /**
   * Mark the instruction as no longer needed.
   */
  public void nullify(RegisterSet rs)
  {
  }

  /**
   * Return true if this is a branch instruction.
   */
  public final boolean isBranch()
  {
    return true;
  }

  /**
   * Specify that this branch is a call to a subroutine.
   */
  public final void markAsCall()
  {
    isCall = true;
  }

  /**
   *  Return true if this branch is a call to a subroutine.
   */
  public final boolean isCall()
  {
    return isCall;
  }
  
  /**
   * Return the probability that this branch is taken.
   */
  public final double getBranchProbability()
  {
    return branchProbability;
  }

  /**
   * Set the probability that this branch is taken.
   */
  public final void setBranchProbability(double brProb)
  {
    branchProbability = brProb;
  }

  /**
   * Return the place branched to.
   */
  public final Label getTarget(int targetNumber)
  {
    return successors[targetNumber];
  }

  /**
   * Return the number of successors of this branch.
   */
  public final int numTargets()
  {
    if (successors == null)
      return 0;
    return successors.length;
  }

  /**
   * Return the number of cycles that this instruction requires.
   */
  public int getExecutionCycles()
  {
    return 4;
  }

  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    return false;    
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
    if (regsUsed != null)
      for (int i = 0; i < regsUsed.length; i++)
        rs.useRegister(index, regsUsed[i], strength);
    if (regsKilled != null)
      for (int i = 0; i < regsKilled.length; i++)
        rs.modRegister(index, regsKilled[i]);
    if (regsSet != null)
      for (int i = 0; i < regsSet.length; i++) {
        rs.defRegister(index, regsSet[i]);
      }
  }

  /**
   * Return true if the instruction defines the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    if (regsSet == null)
      return false;

    for (int i = 0; i < regsSet.length; i++)
      if (regsSet[i] == register)
        return true;

    return false;
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (regsUsed == null)
      return false;

    for (int i = 0; i < regsUsed.length; i++)
      if (regsUsed[i] == register)
        return true;

    return false;
  }
  
  /**
   * Returns array of registers used.
   */
  public short[] uses() 
  {
    return regsUsed;
  }

  /**
   * Return true if the instruction clobbers the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    if (regsKilled == null)
      return false;

    for (int i = 0; i < regsKilled.length; i++)
      if (regsKilled[i] == register)
        return true;

    return false;
  }

  /**
   * Map the virtual registers referenced in the instruction to the
   * specified real registers.  The mapping is specified using an
   * array that is indexed by the virtual register to return the real
   * register.
   * @param map maps from the virtual register to real register
   */
  public void remapRegisters(int[] map)
  {
    if (regsUsed != null)
      for (int i = 0; i < regsUsed.length; i++)
        regsUsed[i] = (short) map[regsUsed[i]];

    if (regsKilled != null)
      for (int i = 0; i < regsKilled.length; i++)
        regsKilled[i] = (short) map[regsKilled[i]];

    if (regsSet != null)
      for (int i = 0; i < regsSet.length; i++)
        regsSet[i] = (short) map[regsSet[i]];
  }

  /**
   * Map the registers used in the instruction as sources to the
   * specified register.  If the register is not used as a source
   * register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    if (regsUsed == null)
      return;

    for (int i = 0; i < regsUsed.length; i++)
      if (regsUsed[i] == oldReg)
        regsUsed[i] = (short) newReg;
  }

  /**
   * Map the registers defined in the instruction as destinations to
   * the specified register.  If the register is not used as a
   * destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    if (regsKilled != null)
      for (int i = 0; i < regsKilled.length; i++)
        if (regsKilled[i] == oldReg)
          regsKilled[i] = (short) newReg;

    if (regsSet != null)
      for (int i = 0; i < regsSet.length; i++)
        if (regsSet[i] == oldReg)
          regsSet[i] = (short) newReg;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer(isCall ? " call" : " ");
    if (regsUsed != null) {
      buf.append(" (Uses ");
      for (int i = 0; i < regsUsed.length; i++) {
        buf.append(' ');
        buf.append(regsUsed[i]);
      }
      buf.append(')');
    }
    if (regsKilled != null) {
      buf.append(" (kills");
      for (int i = 0; i < regsKilled.length; i++) {
        buf.append(' ');
        buf.append(regsKilled[i]);
      }
      buf.append(')');
    }
    if (regsSet != null) {
      buf.append(" (defs");
      for (int i = 0; i < regsSet.length; i++) {
        buf.append(' ');
        buf.append(regsSet[i]);
      }
      buf.append(')');
    }
    return buf.toString();    
  }
  
  public Instruction copy()
  {
    Branch copy = (Branch) super.clone();

    if (regsKilled != null) {
      int rkl = regsKilled.length;
      copy.regsKilled = new short[rkl];
      System.arraycopy(regsKilled, 0, copy.regsKilled, 0, rkl);
    }
    
    if (regsSet != null) {
      int rsl = regsSet.length;
      copy.regsSet    = new short[rsl];
      System.arraycopy(regsSet, 0, copy.regsSet, 0, rsl);
    }
    
    if (regsUsed != null) {
      int rul = regsUsed.length;
      copy.regsUsed   = new short[rul];
      System.arraycopy(regsUsed, 0, copy.regsUsed, 0, rul);
    }

    if (successors != null) {
      int ssl = successors.length;
      copy.successors = new Label[ssl];
      System.arraycopy(successors, 0, copy.successors, 0, ssl);
    }
    
    return copy;
  }

}
