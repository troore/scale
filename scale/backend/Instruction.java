package scale.backend;

import scale.common.*;

/** 
 * This is the abstract class for all machine instructions including
 * Markers.
 * <p>
 * $Id: Instruction.java,v 1.57 2007-10-17 23:21:34 bmaher Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 *<p>
 *<h3>Flags</h3>
 * Various flags are used to mark instructions:
 * <dl>
 * <dt>END_INST<dd>
 * This instruction is the last in a sequence.  In particular, an
 * instruction that ends a sequence can be used as a place to insert
 * spill stores.  Most instructions do not end a sequence.  There
 * may be several <i>start</i>s between every <i>end</i>.
 * <dt>START_INST<dd>
 * This instruction starts a sequence of instructions.  In
 * particular, an instruction that starts a sequence can be used as
 * a place to insert spill loads.  Most instructions can start a
 * sequence.  However, if there is a sequence of instructions that
 * can not be split by a spill load, all but the first must be
 * marked as not starting a sequence.  There may be several
 * <i>start</i>s between every <i>end</i>.
 * <dt>NULLIFIED_INST<dd>
 * This instruction is no longer used.
 * <dt>MANDATORY_INST<dd>
 * This instruction should never be eliminated.  An example of a
 * mandatory instruction would be a load of a "volatile" variable.
 * Another example would be an instruction with no outputs such as a
 * status setting instruction or a nop inserted for timing purposes.
 * <dt>SPILL_INST<dd>
 * This instruction was inserted as part of a spill sequence.
 * </dl>
 */

public abstract class Instruction implements Cloneable
{
  private static final int START_INST     = 0x01;
  private static final int END_INST       = 0x02;
  private static final int NULLIFIED_INST = 0x04;
  private static final int MANDATORY_INST = 0x08;
  private static final int SPILL_INST     = 0x10;

  private Instruction next;  // The next lexical instruction.
  private int         flags;
  private int         tag;   // For use by various algorithmns.

  protected Instruction()
  {
    this.flags = START_INST;
  }

  /**
   * Make a copy of this instruction.
   * @return the copy of the instruction
   */
  public Instruction copy()
  {
    try {
      return (Instruction) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new scale.common.InternalError("Could not clone instruction " + this);
    }
  }

  /**
   * Map the virtual registers referenced in the instruction to the
   * specified real registers.  The mapping is specified using an
   * array that is indexed by the virtual register to return the real
   * register.
   * @param map maps from the virtual register to real register
   */
  public abstract void remapRegisters(int[] map);

  /**
   * Map the registers used in the instruction as sources to the
   * specified register.  If the register is not used as a source
   * register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public abstract void remapSrcRegister(int oldReg, int newReg);

  /**
   * Map the registers defined in the instruction as destinations to
   * the specified register.  If the register is not used as a
   * destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public abstract void remapDestRegister(int oldReg, int newReg);

  /**
   * Return the numeric opcode of the instruction.
   * This opcode may be an encoding of the actual instruction opcode.
   */
  public abstract int getOpcode();

  /**
   * Return the destination register or -1 if none.
   */
  public int getDestRegister()
  {
    return -1;
  }
  
  /**
   * Return the source registers or <code>null</code> if none.
   */
  public int[] getSrcRegisters()
  {
    return null;
  }

  /**
   * Mark the instruction so that it is not used.
   */
  public void nullify(RegisterSet rs)
  {
    if ((flags & MANDATORY_INST) == 0)
      flags |= NULLIFIED_INST;
  }

  /**
   * Return true if the instruction is nullified.
   */
  public final boolean nullified()
  {
    return (flags & NULLIFIED_INST) != 0 ;
  }

  /**
   * Specify that this instruction should never be eliminated.  An
   * example of a mandatory instruction would be a load of a
   * "volatile" variable.  Another example would be an instruction
   * with no outputs such as a status setting instruction or a nop
   * inserted for timing purposes.
   *
   */
  public final void setMandatory()
  {
    flags |= MANDATORY_INST;
  }

  /**
   * Return true if this instruction is mandatory.  An example of a
   * mandatory instruction would be a load of a "volatile" variable.
   * Another example would be an instruction with no outputs such as a
   * status setting instruction or a nop inserted for timing purposes.
   */
  public final boolean isMandatory()
  {
    return ((flags & MANDATORY_INST) != 0);
  }

  /**
   * Mark this instruction as part of a spill load or spill store
   * sequence.
   *
   */
  public final void markSpillInstruction()
  {
    flags |= SPILL_INST;
  }

  /**
   * Return true if this instruction was inserted as part of a spill
   * load or spill store sequence.
   */
  public boolean isSpillInstruction()
  {
    return ((flags & SPILL_INST) != 0);
  }

  /**
   * Set the tag value.  The tag is used by various algorithmns.
   */
  public final void setTag(int tag)
  {
    this.tag = tag;
  }

  /**
   * Get the tag value.  The tag is used by various algorithmns.
   */
  public final int getTag()
  {
    return tag;
  }

  /**
   * Return the number of cycles that this instruction requires.
   */
  public int getExecutionCycles()
  {
    return (isLoad() || isStore()) ? 7 : 1;
  }

  /**
   * Return the number of the functional unit required to execute this
   * instruction.
   */
  public int getFunctionalUnit()
  {
    return 0;
  }

  /**
   * Specify that this instruction is the last in a sequence of
   * instructions and that a spill store can be inserted after it.
   * Some sequences of instructions may define a register and then go
   * on to modify that register.  Sometimes these sequences utilize a
   * branch.  An example is the sequence generated for a divide on
   * some architectures.  If a spill store is needed for the result
   * register, it is not valid to do the store before the sequence is
   * complete.  This flag is currently set when a {@link
   * scale.score.chords.ExprChord ExprChord} instance is translated or
   * after the predicate of an if-then-else is evaluated.
   * @see Instruction#isSpillStorePoint
   * @see Instruction#specifyNotSpillLoadPoint
   * @see Instruction#isSpillLoadPoint
   */
  public final void specifySpillStorePoint()
  {
    flags |= END_INST;
  }

  /**
   * Return true if this instruction is the last in a sequence of
   * instructions.  A sequence is defined to be the sequence of
   * instructions that define a value.  In particular, an instruction
   * that ends a sequence can be used as a place to insert spill
   * stores.  Most instructions do not end a sequence.  There may be
   * several <i>start</i>s between every <i>end</i>.  This flag is
   * currently set when a {@link scale.score.chords.ExprChord
   * ExprChord} instance is translated.
   * @see Instruction#specifySpillStorePoint
   * @see Instruction#specifyNotSpillLoadPoint
   * @see Instruction#isSpillLoadPoint
   */
  public final boolean isSpillStorePoint()
  {
    return ((flags & END_INST) != 0);
  }

  /**
   * Specify that this instruction is not the first in a sequence of
   * instructions.  A sequence is defined to be the sequence of
   * instructions that define a value.
   * @see Instruction#specifySpillStorePoint
   * @see Instruction#isSpillStorePoint
   * @see Instruction#isSpillLoadPoint
   */
  public final void specifyNotSpillLoadPoint()
  {
    flags &= ~START_INST;
  }

  /**
   * Return true if this instruction is the first in a sequence of
   * instructions.  A sequence is defined to be the sequence of
   * instructions that define a value.  In particular, an instruction
   * that starts a sequence can be used as a place to insert spill
   * loads.  Most instructions can start a sequence.  However, if
   * there is a sequence of instructions that can not be split by a
   * spill load, all but the first must be marked as not starting a
   * sequence.  There may be several <i>start</i>s between every
   * <i>end</i>.
   * @see Instruction#specifySpillStorePoint
   * @see Instruction#isSpillStorePoint
   * @see Instruction#specifyNotSpillLoadPoint
   */
  public final boolean isSpillLoadPoint()
  {
    return ((flags & START_INST) != 0);
  }

  /**
   * Return true if this is a load-from-memory instruction.
   */
  public boolean isLoad()
  {
    return false;
  }

  /**
   * Return true if this is a store-into-memory instruction.
   */
  public boolean isStore()
  {
    return false;
  }

  /**
   * Return true if this is a branch instruction.
   */
  public boolean isBranch()
  {
    return false;
  }

  /**
   * Return true if this is a Marker.
   * @see scale.backend.Marker
   */
  public boolean isMarker()
  {
    return false;
  }

  /**
   * Return true if this is a prefetch instruction.
   */
  public boolean isPrefetch()
  {
    return false;
  }
  
  /**
   * Return true if this is a LabelMarker.
   */
  public boolean isLabel()
  {
    return false;
  }
  
  /**
   * Return true if this is a phi instruction.
   */
  public boolean isPhi()
  {
    return false;
  }

  /**
   * Set the next lexical instruction.
   */
  public final void setNext(Instruction next)
  {
    this.next = next;
  }
  
  /**
   * Return the next lexical instruction.
   */
  public final Instruction getNext()
  {
    return next;
  }
  
  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public abstract void assembler(Assembler asm, Emit emit);

  /**
   * Return the number of bytes required for the instruction.
   */
  public abstract int instructionSize();

  /**
   * Specify the registers used and defined by this instruction.
   * Uses must be specified before definitions.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public abstract void specifyRegisterUsage(RegisterAllocator rs,
                                            int               index,
                                            int               strength);

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return true if the instruction clobbers the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return true if the instruction can be deleted without changing
   * the program's semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    return false;
  }

  /**
   * Return true if this instruction is independent of the specified
   * instruction.  If instructions are independent, than one
   * instruction can be moved before or after the other instruction
   * without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  public abstract boolean independent(Instruction inst, RegisterSet registers);

  /**
   * Return true if this instruction has a side effect of changing a
   * special register.  An example would be a condition code register
   * for architectures that set the condition and tehn branch on it
   * such as the Sparc.
   */
  public boolean setsSpecialReg()
  {
    return false;
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
   * Return true if the instruction is predicated.
   */
  public boolean isPredicated()
  {
    return false;
  }
  
  /**
   * Returns true if the instruction is predicated on true.
   */
  public boolean isPredicatedOnTrue()
  {
    return false;
  }
  
  /**
   * Set the predicate for the instruction.
   */
  public void setPredicate(int rp)
  {
  }
  
  /**
   * Set the predicate for the instruction. 
   */
  public void setPredicate(int rp, boolean predicatedOnTrue)
  {
  }
  
  /**
   * Set the condition for the predicate.
   */
  public void setPredicatedOnTrue(boolean predicatedOnTrue)
  {
  }

  /**
   * Remove the predicates from this instruction.
   */
  public void removePredicates()
  {
  }

  /**
   * Return the predicates for the instruction.
   */
  public int[] getPredicates()
  {
    return null;
  }

  /**
   * Return the number of predicates.
   */
  public int numPredicates()
  {
    return 0;
  }

  /**
   * Return the specified predicate.
   */
  public int getPredicate(int i)
  {
    throw new scale.common.InternalError("Predicates not defined for this architecture.");
  }

  /**
   * Specify the predicates for the instruction.
   */
  public void setPredicates(int[] predicates)
  {
  }

  /**
   * Return a hash code that can be used to determine equivalence.
   */
  public int ehash()
  {
    return -1;
  }
  
  /**
   * Return the source register of a copy instruction.
   */
  public int getCopySrc()
  {
    throw new scale.common.InternalError("Not a copy instruction " + this);
  }

  /**
   * Return the source register of a copy instruction.
   */
  public int getCopyDest()
  {
    throw new scale.common.InternalError("Not a copy instruction " + this);
  }
  
  /**
   * Clone the instruction.  Performs a shallow copy.
   */
  public Object clone()
  {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new scale.common.InternalError("Could not clone instruction " + this);
    }
  }

  /**
   * Return the loop number of the instruction.
   * This is Trips specific.  For all other backends,
   * it returns 0.
   */
  public int getLoopNumber()
  {
    return 0;
  }

  /**
   * Set the loop number of the instruction.
   * This is Trips specific.  For all other backends,
   * it does nothing.
   */
  protected void setLoopNumber(int loopNumber)
  {
  }
  
  /**
   * Get the basic block number of the instruction.
   * TRIPS-specific.
   */
  public int getBBID()
  {
    return 0;
  }
  
  /**
   * Set the basic block number of the instruction.
   * TRIPS-specific.
   */
  public void setBBID(int bbid)
  {
  }
}
