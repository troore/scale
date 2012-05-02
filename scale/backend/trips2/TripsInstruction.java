package scale.backend.trips2;

import scale.backend.*;
import scale.common.*;

/**
 * This class represents a Trips instruction.
 * <p>
 * $Id: TripsInstruction.java,v 1.22 2007-10-31 23:47:51 bmaher Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public abstract class TripsInstruction extends Instruction 
{
  /**
   * Number of the loop this instruction is in, or 0 if none.  A
   * value of zero indicates that the instruction is not in a loop.
   */
  private int loopNumber;
  
  /**
   * Indicates the basic block this instruction was originally in.
   */
  private int bbid = -1;

  /**
   * The registers the instruction is predicated on.
   */
  protected int[] predicates;
  
  /**
   * The number of registers the instruction is predicated on.
   */
  protected int numPredicates;

  /**
   * Whether the instruction is predicated on true or false.
   */
  protected boolean predicatedOnTrue;
  
  /**
   * True if the instruction defines a predicate.
   */
  protected boolean definesPredicate;
  
  /**
   * Create a new unpredicated Trips instruction.
   */
  public TripsInstruction()
  {
    this(-1, false);
  }
  
  /**
   * Create a new predicated Trips instruction.
   */
  public TripsInstruction(int rp, boolean predicatedOnTrue)
  {
    super();
    
    this.definesPredicate = false;
    
    if (rp > -1)
      setPredicate(rp, predicatedOnTrue);
  }

  /**
   * Return the number of predicates for the instruction.
   */
  public final int numPredicates()
  {
    return numPredicates;
  }

  /**
   * Return the i-th predicate for the instruction.
   */
  public final int getPredicate(int i)
  {
    return predicates[i];
  }

  /**
   * Return the predicates for the instruction.
   */
  public int[] getPredicates()
  {
    if (numPredicates <= 0)
      return null;

    int[] r = new int[numPredicates];
    System.arraycopy(predicates, 0, r, 0, numPredicates);
    return r;
  }

  /**
   * Set the predicate for the instruction. 
   */
  public void setPredicate(int rp)
  {
    assert (rp > -1) : "Illegal predicate.";
    
    if (predicates == null)
      predicates = new int[1];

    predicates[0] = rp;
    numPredicates = 1;
  }
  
  /**
   * Set the predicate for the instruction. 
   */
  public void setPredicate(int rp, boolean predicatedOnTrue)
  {
    assert (rp > -1) : "Illegal predicate.";
    
    setPredicate(rp);
    setPredicatedOnTrue(predicatedOnTrue);
  }
  
  /**
   * Set the predicates for the instruction. 
   */
  public void setPredicates(int[] predicates)
  {
    assert (predicates != null) : "Predicates cannot be null.";

    this.predicates    = predicates.clone();
    this.numPredicates = this.predicates.length;
  }
  
  /**
   * Set the predicates for the instruction.  This clears any previous predicates.
   */
  public void setPredicates(int[] predicates, boolean predicatedOnTrue)
  {
    setPredicates(predicates);
    setPredicatedOnTrue(predicatedOnTrue);
  }
  
  /**
   * Remove the predicates from this instruction.
   */
  public void removePredicates()
  {
    numPredicates = 0;
    predicates    = null;
  }
  
  /**
   * Set the condition for the predicate.
   */
  public void setPredicatedOnTrue(boolean predicatedOnTrue)
  {
    this.predicatedOnTrue = predicatedOnTrue;
  }
  
  /**
   * Return true if the instruction is predicated on true.
   */
  public boolean isPredicatedOnTrue()
  {
    return predicatedOnTrue;  
  }
  
  /**
   * Return true if the instruction is predicated.
   */
  public boolean isPredicated() 
  {
    return numPredicates > 0;
  }
  
  /**
   * Return true if the instruction defines a predicated.
   */
  public boolean definesPredicate()
  {
    return definesPredicate;
  }
  
  /**
   * Set if the instruction defines a predicate.
   */
  protected void setDefinesPredicate()
  {
    this.definesPredicate = true;
  }
  
  /**
   * Map the registers used in the instruction to the specified registers.
   */
  public void remapRegisters(int[] map)
  {
    for (int i = 0; i < numPredicates; i++) {
      int rp = predicates[i];      
      rp = map[rp];
      predicates[i] = rp;
    }
  }
  
  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    for (int i = 0; i < numPredicates; i++) {
      int rp = predicates[i];      
      if (rp == oldReg) {
        rp = newReg;
        predicates[i] = rp;
      }
    }
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
    for (int i = 0; i < numPredicates; i++) {
      int rp = predicates[i];
      rs.useRegister(index, rp, strength);
    }
  }
  
  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    for (int i = 0; i < numPredicates; i++) {
      int rp = predicates[i];
      if (rp == register)
        return true;
    }

    return false;
  }
  
  /**
   * Mark the instruction so that it is not used.
   */
  public void nullify(RegisterSet rs)
  {
    super.nullify(rs);
  }
  
  /**
   * This routine returns the source registers for an instruction.
   * Null is returned if there are no source registers.
   */
  public int[] getSrcRegisters()
  {
    if (isPredicated()) {
      int[] srcs = new int[numPredicates];
      System.arraycopy(predicates, 0, srcs, 0, numPredicates);
      return srcs;
    }
    return null;
  }
  
  /**
   * Return true if this instruction is independent of the specified instruction.
   * If instructions are independent, than one instruction can be moved before
   * or after the other instruction without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public boolean independent(Instruction inst, RegisterSet registers)
  {
    for (int i = 0; i < numPredicates; i++) {
      int rp = predicates[i];
      if (inst.defs(rp, registers))
        return false;
    }

    return true;
  }
  
  /**
   * Return a string representation of the ra register for the assembler.
   */
  protected String formatRa(Assembler asm, int ra)
  {
    if (definesPredicate())
      return ((Trips2Assembler) asm).assemblePredicateRegister(ra);
    else
      return asm.assembleRegister(ra);
  }
  
  /**
   * Return a string representation of the opcode for the assembler.
   */
  protected String formatOpcode(Assembler asm, int opcode)
  {
    String str = Opcodes.getOp(opcode);
    if (!isPredicated())
      return str;

    StringBuffer buf = new StringBuffer(str);
    buf.append(predicatedOnTrue ? "_t" : "_f");
    buf.append('<');
    for (int i = 0; i < numPredicates; i++) {
      if (i > 0)
        buf.append(", ");
      buf.append(((Trips2Assembler) asm).assemblePredicateRegister(predicates[i]));
    }
    buf.append('>');

    return buf.toString();
  }
  
  /**
   * Return a string representation of the opcode.
   */
  protected String formatOpcode(int opcode)
  {
    String str = Opcodes.getOp(opcode);
    if (!isPredicated())
      return str;

    StringBuffer buf = new StringBuffer(str);
    buf.append(predicatedOnTrue ? "_t" : "_f");
    buf.append('<');
    for (int i = 0; i < numPredicates; i++) {
      if (i > 0)
        buf.append(", ");
      buf.append(predicates[i]);
    }
    buf.append('>');

    return buf.toString();
  }
  
  /**
   * Return a string representation of the opcode.
   */
  protected String formatOpcode(String opcode)
  {
    if (!isPredicated())
      return opcode;

    StringBuffer buf = new StringBuffer(opcode);
    buf.append(predicatedOnTrue ? "_t" : "_f");
    buf.append('<');
    for (int i = 0; i < numPredicates; i++) {
      if (i > 0)
        buf.append(", ");
      buf.append(predicates[i]);
    }
    buf.append('>');

    return buf.toString();
  }

  /**
   * Return the loop number of the instruction.
   * This is Trips specific.  For all other backends,
   * it returns 0.
   */
  public int getLoopNumber()
  {
    return loopNumber;
  }

  /**
   * Set the loop number of the instruction.
   * This is Trips specific.  For all other backends,
   * it does nothing.
   */
  protected void setLoopNumber(int loopNumber)
  {
    this.loopNumber = loopNumber;
  }
  
  /**
   * Get the basic block number of the instruction.
   * TRIPS-specific.
   */
  public int getBBID()
  {
    return bbid;
  }
  
  /**
   * Set the basic block number of the instruction.
   * TRIPS-specific.
   */
  public void setBBID(int bbid)
  {
    this.bbid = bbid;
  }
}
