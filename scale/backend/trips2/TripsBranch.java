package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/**
 * This class represents Trips branch instructions.
 * <p>
 * $Id: TripsBranch.java,v 1.43 2007-10-31 23:47:51 bmaher Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The TripsInstruction class represents operations in both operand
 * and target form.  When in operand form, ithe operation is
 * considered a PSEUDO-op.  If a PSEUDO operation is represented, the
 * ra, rb, and rc fields represent an input virtual register.

 * <p>
 * For operations in target form (non-PSEUDO operations), the ra, rb,
 * and rc fields represent the targets of the operation and specifies
 * either an instruction ID, immediate value, or a register depending
 * on the instruction format.
 * <p>
 * The opcode encodes the instruction format:
 * <table cols="3"><thead>
 * <tr><th>Bits</th><th>Use</th><th>description</th>
 * </thead><tbody>
 * <tr><td>0..19</td><td>0</td><td>should be zero</td>
 * <tr><td>20..23</td><td>format</td><td>The format of the Trips instruction</td>
 * <tr><td>24..31</td><td>opcode</td><td>The opcode of the Trips instruction</td>
 * </tbody></table>
 * <p>
 * If there is only one output required from an instruction, the B1 or T1 form is used
 * regardless of the register number or the immediate value size.
 * <p>
 * Each instruction is labeled with an integer ID which is unique
 * within the basic block in which the instruction resides.
 * <p>
 * When the instruction is a Trips instruction containing an immediate
 * value, the immediate value is coded as follows:
 * <table cols="3"><thead>
 * <tr><th>Bits</th><th>Use</th><th>description</th>
 * </thead><tbody>
 * <tr><td>0..27</td><td>value</td><td>an index into a table of displacements</td>
 * <tr><td>28..31</td><td>ftn</td><td>the function to apply to the value</td>
 * </tbody></table>
 * Allowed values for the function are
 * <table cols="2"><thead>
 * </tbody></table>
 */
public final class TripsBranch extends Branch
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.trips2.TripsBranch", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * The instruction's opcode.
   */
  private int opcode;

  /**
   * The rb register.
   */
  protected int rb;
  
  /**
   * The target symbol displacement for a CALLO instruction.
   */
  protected SymbolDisplacement disp;

  /**
   * Number of the loop this instruction is in, or -1 if none.  A
   * value of zero indicates that the instruction is not in a loop.
   */
  private int loopNumber;

  /**
   * Indicates the basic block this instruction was originally in.
   */
  private int bbid = -1;
  
  /**
   * The branch id.
   */
  private int branchId = 0;
  
  /**
   * The registers the instruction is predicated on.
   */
  protected int[] predicates;
  
  /**
   * The number of registers the instruction is predicated on.
   */
  protected int numPredicates;
  
  /**
   * The condition on which to predicate.
   */
  protected boolean predicatedOnTrue;
  
  /**
   * Create a new Trips branch instruction in the B:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param rb specifies the right argument register
   * @param numTargets specifies the number of targets of the branch
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue specifies the condition to predicate on
   * (ignored if rp is -1)
   */
  public TripsBranch(int     opcode,
                     int     rb,
                     int     numTargets,
                     int     rp,
                     boolean predicatedOnTrue)
  {
    this(opcode, rb, numTargets, null, null, rp, predicatedOnTrue);
    
    byte format = Opcodes.getFormat(opcode);
    assert (format == Opcodes.B1) : "Wrong form for B:1 instruction format.";
  }

  /**
   * Create a new Trips branch instruction in the B:1 format.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param rb specifies the right argument register
   * @param numTargets specifies the number of targets of the branch
   */
  public TripsBranch(int opcode, int rb, int numTargets) 
  {
    this(opcode, rb, numTargets, -1, false);
  }

  /**
   * Create a new Trips branch instruction in the B:0 format (except SCALL).
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param lab specifies the target label (ignored if instruction opcode = SCALL)
   * @param numTargets specifies the number of targets of the branch
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue specifies the condition to predicate on
   * (ignored if rp is -1)
   */
  public TripsBranch(int     opcode,
                     Label   lab,
                     int     numTargets,
                     int     rp,
                     boolean predicatedOnTrue)
  {
    this(opcode, -1, numTargets, lab, null, rp, predicatedOnTrue);
    
    byte format = Opcodes.getFormat(opcode);
    assert (format == Opcodes.B0) : "Wrong form for B:0 instruction format.";
    assert (opcode != Opcodes.SCALL) : "Wrong form for SCALL instruction.";
  }
  
  /**
   * Create a new Trips branch with specified taken probability.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param lab specifies the target label (ignored if instruction
   * opcode = SCALL)
   * @param numTargets specifies the number of targets of the branch
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue specifies the condition to predicate on
   * (ignored if rp is -1)
   * @param brpred is the probability that this branch is taken
   */
  public TripsBranch(int     opcode,
                     Label   lab,
                     int     numTargets,
                     int     rp,
                     boolean predicatedOnTrue,
                     double  brpred)
  {
    this(opcode, lab, numTargets, rp, predicatedOnTrue);
    setBranchProbability(brpred);
  }
  
  /**
   * Create a new Trips branch instruction in the B:0 format (except SCALL).
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param lab specifies the target label (ignored if instruction opcode = SCALL)
   * @param numTargets specifies the number of targets of the branch
   */
  public TripsBranch(int opcode, Label lab, int numTargets) 
  {
    this(opcode, lab, numTargets, -1, false);
  }
  
  /**
   * Create a new Trips CALLO Instruction.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param disp specifies the target symbol displacement (ignored if
   * instruction opcode = SCALL)
   * @param numTargets specifies the number of targets of the branch
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue specifies the condition to predicate on
   * (ignored if rp is -1)
   */
  public TripsBranch(int                opcode,
                     SymbolDisplacement disp,
                     int                numTargets,
                     int                rp,
                     boolean            predicatedOnTrue)
  {
    this(opcode, -1, numTargets, null, disp, rp, predicatedOnTrue);
          
    assert (opcode == Opcodes.CALLO) :
      "Wrong form: this form only for CALLO instruction.";
  }
  
  /**
   * Create a new Trips CALLO Instruction.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param disp specifies the target symbol displacement (ignored if
   * instruction opcode = SCALL)
   * @param numTargets specifies the number of targets of the branch
   */
  public TripsBranch(int opcode, SymbolDisplacement disp, int numTargets) 
  {
    this(opcode, disp, numTargets, -1, false);
  }

  /**
   * Create a new Trips SCALL Instruction.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param numTargets specifies the number of targets of the branch
   * @param rp specifies the register to predicate on (-1 if unused)
   * @param predicatedOnTrue specifies the condition to predicate on
   * (ignored if rp is -1)
   */
  public TripsBranch(int     opcode,
                     int     numTargets,
                     int     rp,
                     boolean predicatedOnTrue)
  {
    this(opcode, -1, numTargets, null, null, rp, predicatedOnTrue);
    
    assert (opcode == Opcodes.SCALL) : "Wrong form: this form only for SCALL.";
  }
  
  /**
   * Create a new Trips SCALL instruction.
   * <p>
   * @param opcode specifies the pseudo-operation
   * @param numTargets specifies the number of targets of the branch
   */
  public TripsBranch(int opcode, int numTargets) 
  {
    this(opcode, numTargets, -1, false);
  }
  
  /**
   * Create a new Trips branch instruction.
   */ 
  private TripsBranch(int                opcode,
                      int                rb,
                      int                numTargets,
                      Label              lab,
                      SymbolDisplacement disp,
                      int                rp,
                      boolean            predicatedOnTrue)
  {
    super(numTargets);

    this.opcode = opcode;
    this.rb     = rb;
    this.disp   = disp;
    
    if (numTargets > 0)
      addTarget(lab, 0);
    
    setBranchProbability(1.0);
    
    if (rp > -1)
      setPredicate(rp, predicatedOnTrue);
    
    createdCount++;
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
    super.remapRegisters(map);
    if (Opcodes.getFormat(opcode) == Opcodes.B1)
      rb = map[rb];
    
    for (int i = 0; i < numPredicates; i++)
      predicates[i] = map[predicates[i]];
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
    super.remapSrcRegister(oldReg, newReg);
    if (rb == oldReg && (Opcodes.getFormat(opcode) == Opcodes.B1))
      rb = newReg;
    
    for (int i = 0; i < numPredicates; i++) {
      if (predicates[i] == oldReg)
        predicates[i] = newReg;
    }
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
    super.remapDestRegister(oldReg, newReg);
  }

  /**
   * Return the instruction opcode.
   */
  public int getOpcode()
  {
    return opcode & 0xff;
  }

  /**
   * Return the target of the branch.
   */
  public Label getTarget()
  {
    return getTarget(0);
  }

  /**
   * Return the instruction format.
   */
  public int getFormat()
  {
    return Opcodes.getFormat(opcode);
  }
  
  /**
   * This routine returns the source registers for an instruction.
   * Null is returned if there are no source registers.
   */
  public int[] getSrcRegisters()
  {
    if (rb > -1) {
      int[] srcs = new int[1 + numPredicates];
    
      srcs[0] = rb;
      if (numPredicates > 0)
        System.arraycopy(predicates, 0, srcs, 1, numPredicates);
    
      return srcs;
    } 
    
    return predicates;
  }
  
  /**
   * Return the rb field.
   */
  public int getRb()
  {
    return rb;
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
    
    numPredicates = 1;
    predicates[0] = rp;
  }
  
  /**
   * Set the predicate for the instruction.  
   */
  public void setPredicate(int rp, boolean predicatedOnTrue)
  {
    assert (rp > -1) : "Illegal predicate.";
    
    setPredicate(rp);
    this.predicatedOnTrue = predicatedOnTrue;
  }
  
  /**
   * Set the predicates for the instruction.  
   */
  public void setPredicates(int[] predicates)
  {
    assert (predicates != null) : "Predicates cannot be null.";

    this.predicates = predicates.clone();
    numPredicates = this.predicates.length;
  }
  
  /**
   * Set the predicates for the instruction.  This clears any previous
   * predicates.
   */
  public void setPredicates(int[] predicates, boolean predicatedOnTrue)
  {
    setPredicates(predicates);
    
    this.predicatedOnTrue = predicatedOnTrue;
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
   * Returns true if the instruction in predicated
   */
  public boolean isPredicated() 
  {
    return numPredicates > 0;
  }
  
  /**
   * Returns true if the instruction is predicated on true.
   */
  public boolean isPredicatedOnTrue()
  {
    return predicatedOnTrue;
  }
  
  /**
   * Set the condition for the predicate.
   */
  public void setPredicatedOnTrue(boolean predicatedOnTrue)
  {
    this.predicatedOnTrue = predicatedOnTrue;
  }
  
  /**
   * Specify the registers used by this instruction.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator rs,
                                   int               index,
                                   int               strength)
  {
    super.specifyRegisterUsage(rs, index, strength);

    if (rb > -1)
      rs.useRegister(index, rb, strength);
    
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
    return (rb == register) || super.uses(register, registers);
  }

  /**
   * Return true if the instruction sets the register
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return super.defs(register, registers);
  }

  /**
   * Return true if the instruction clobbers the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return super.mods(register, registers);
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
    if (inst.defs(rb, registers))
      return false;

    for (int i = 0; i < numPredicates; i++) {
      int rp = predicates[i];
      if (inst.defs(rp, registers))
        return false;
    }

    return true;
  }

  /**
   * Return the number of bytes required for the TripsBranch.
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Return true if the instruction can be deleted without changing
   * program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
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
   * Return the source register of a copy instruction.
   */
  public int getCopySrc()
  {
    return super.getCopySrc();
  }

  /**
   * Return the source register of a copy instruction.
   */
  public int getCopyDest()
  {
    return super.getCopyDest();
  }

  /**
   * Return a hash code that can be used to determine equivalence.
   * The hash code does not include predicates or the destination
   * register.
   */
  public int ehash()
  {
    String       str = Opcodes.getOp(opcode);
    StringBuffer buf = new StringBuffer(str);
    
    byte format = Opcodes.getFormat(opcode);
    if (format == Opcodes.B1)
      buf.append(rb);

    if ((format == Opcodes.B0) && (opcode != Opcodes.SCALL)) {
      if (opcode == Opcodes.CALLO)
        buf.append(disp.getName());
      else
        buf.append(((TripsLabel) getTarget(0)).getLabelString());
    }
    
    return buf.toString().hashCode();
  }
  
  /**
   * Return a string representation of the opcode for the assembler.
   */
  private String formatOpcode(Assembler asm)
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
  private String formatOpcode()
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
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    byte format = Opcodes.getFormat(opcode);

    emit.emit(formatOpcode(asm));
    
    if (format == Opcodes.B1) {
      emit.emit('\t');
      emit.emit(asm.assembleRegister(rb));
    }

    if (format == Opcodes.B0 && opcode != Opcodes.SCALL) {
      emit.emit('\t');
      if (opcode == Opcodes.CALLO)
        emit.emit(disp.getName());
      else
        asm.assembleLabel(getTarget(0), emit);
    }
    
    emit.emit("\tB[");
    emit.emit(branchId);
    emit.emit(']');

    emit.emit("\t; prob ");
    emit.emit(Double.toString(getBranchProbability()));
  }

  /**
   * Return a string representation of the instruction.
   */
  public String toString()
  {
    StringBuffer buf    = new StringBuffer("");
    byte         format = Opcodes.getFormat(opcode);

    buf.append(formatOpcode());

    if (format == Opcodes.B1) {
      buf.append('\t');
      buf.append(rb);
    }

    if (format == Opcodes.B0 && opcode != Opcodes.SCALL) {
      buf.append('\t');
      if (opcode == Opcodes.CALLO)
        buf.append(disp.getName());
      else
        buf.append(((TripsLabel) getTarget(0)).getLabelString());
    }
    
    buf.append("\tB[");
    buf.append(branchId);
    buf.append(']');

    buf.append("\t prob ");
    buf.append(Double.toString(getBranchProbability()));
    
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

  /**
   * Return the branch id.
   */
  public int getBranchId()
  {
    return branchId;
  }
  
  /**
   * Set the branch id.
   */
  public void setBranchId(int branchId)
  {
    this.branchId = branchId;
  }
}

