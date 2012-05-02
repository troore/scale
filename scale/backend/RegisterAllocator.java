package scale.backend;

import scale.common.*;
import scale.clef.decl.Declaration;


/** 
 * This is the base class for all register allocators.
 * <p>
 * $Id: RegisterAllocator.java,v 1.55 2007-04-12 20:09:29 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class RegisterAllocator
{
  public static int spillLoadCount  = 0;
  public static int spillStoreCount = 0;

  private static final String[] stats = {"spillLoads", "spillStores"};

  static
  {
    Statistics.register("scale.backend.RegisterAllocator", stats);
  }

  /**
   * Return the number of spill loads inserted.
   */
  public static int spillLoads()
  {
    return spillLoadCount;
  }

  /**
   * Return the number of spill stores inserted.
   */
  public static int spillStores()
  {
    return spillStoreCount;
  }

  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  /**
   * The register set definition in use for allocation.
   */
  protected RegisterSet registers;
  /**
   * The backend in use for code generation.
   */
  protected Generator generator;
  /**
   * regUse[instruction,register] is true if instruction uses the
   * register.
   */
  protected BitVect[] regUse;
  /**
   * regDef[instruction,register] is true if instruction sets the
   * register.
   */
  protected BitVect[] regDef;
  /**
   * regMod[instruction,register] is true if instruction destroys the
   * value in the register.
   */
  protected BitVect[] regMod;
  /**
   * The register strength - indexed by register number.
   */
  protected long[] regStrength;
  /**
   * A count of the number of times the register was set - indexed by
   * register number.
   */
  protected int[] regDefCnt;
  /**
    * A count of the number of times the register was referenced -
    * indexed by register number.
    */
  protected int[] regUseCnt;

  /**
   * A mapping from register to declaration.  The value may be
   * <code>null</code>.  It is set by the caller of the register
   * allocator.  If it is not null, the register allocator can use the
   * information in debugging displays.
   */
  private Declaration[] regToDecls = null;
  /**
   * Used for debugging.
   */
  private byte[] regToDeclsMode = null;
  /**
   * True if tracing requested.
   */
  protected boolean trace;

  public RegisterAllocator(Generator generator, boolean trace)
  {
    this.generator = generator;
    this.registers = generator.getRegisterSet();
    this.trace     = (trace && classTrace) || Debug.debug(3);
  }

  /**
   * Specify that instruction inst uses the value of register reg.
   * Uses must be specified before definitions.
   * @param inst is the instruction index
   * @param reg is the register
   * @param strength is the importance of the instruction
   * @see #defRegister(int,int)
   * @see scale.backend.Instruction#specifyRegisterUsage
   */
  public void useRegister(int inst, int reg, int strength)
  {
    BitVect bs = regUse[inst];
    int     fr = reg;
    int     lr = fr + registers.numContiguousRegisters(reg);
    for (int r = fr; r < lr; r++) {
      bs.set(r);
      regStrength[r] += strength;
      regUseCnt[r]++;
    }
    fr = registers.rangeBegin(reg);
    if (fr == reg)
      return;
    lr = registers.rangeEnd(reg);
    for (int r = fr; r <= lr; r++) {
      bs.set(r);
      regStrength[r] += strength;
      regUseCnt[r]++;
    }
  }

  /**
   * Specify that instruction inst defines the value of register reg.
   * Uses must be specified before definitions.
   * @param inst is the instruction index
   * @param reg is the register
   * @see #useRegister(int,int,int)
   * @see scale.backend.Instruction#specifyRegisterUsage
   */
  public void defRegister(int inst, int reg)
  {
    BitVect bs = regDef[inst];
    int     fr = reg;
    int     lr = fr + registers.numContiguousRegisters(reg);
    for (int r = fr; r < lr; r++) {
      bs.set(r);
      regDefCnt[r]++;
    }
    fr = registers.rangeBegin(reg);
    if (fr == reg)
      return;
    lr = registers.rangeEnd(reg);
    for (int r = fr; r <= lr; r++) {
      bs.set(r);
      regDefCnt[r]++;
    }
  }

  /**
   * Specify that instruction inst destroys the value in register reg.
   * @param inst is the instruction index
   * @param reg is the register
   * @see #useRegister(int,int,int)
   * @see scale.backend.Instruction#specifyRegisterUsage
   */
  public void modRegister(int inst, int reg)
  {
    BitVect bs = regMod[inst];
    int     fr = reg;
    int     lr = fr + registers.numContiguousRegisters(reg);
    for (int r = fr; r < lr; r++)
      bs.set(r);
    fr = registers.rangeBegin(reg);
    if (fr == reg)
      return;
    lr = registers.rangeEnd(reg);
    for (int r = fr; r <= lr; r++)
      bs.set(r);
  }

  /**
   * Return the importance of the register.
   * This information must be specified.
   * @see #useRegister(int,int,int)
   */
  public long getStrength(int register)
  {
    return regStrength[register];
  }

  /**
   * Determine a mapping from virtual registers to real registers.
   * This may involve the insertion of instructions to load and store
   * the value of a virtual register.
   * @param first is the first instruction in the instruction sequence
   * @return the mapping from virtual register to real register
   */
  public abstract int[] allocate(Instruction first);

  /**
   * Linearize the instructions so that an index can be used to access
   * an instruction.  The instruction's index is set as the
   * instruction's tag value.  Obtain the register usage by
   * instruction along with the importance of each instruction.
   * @param first is the first instruction
   */
  protected Instruction[] linearize(Instruction first)
  {
    Instruction inst = first;
    int         n    = 0;
    int         nr   = registers.numRegisters();

    while (inst != null) {
      inst = inst.getNext();
      n++;
    }

    Instruction[] insts = new Instruction[n];
    int         cs      = 1;
    int         k       = 0;

    // Initialize for hardware register allocation.  New mod, use and
    // def bit vectors for each instruction are created.

    regStrength = new long[nr];
    regDefCnt   = new int[nr];
    regUseCnt   = new int[nr];
    regUse      = new BitVect[n];
    regDef      = new BitVect[n];
    regMod      = new BitVect[n];

    for (int i = 0; i < n; i++) {
      regUse[i] = new BitVect();
      regDef[i] = new BitVect();
      regMod[i] = new BitVect();
    }

    inst = first;
    while (inst != null) {
      insts[k] = inst;
      inst.setTag(k);

      if (!inst.nullified())
        inst.specifyRegisterUsage(this, k, cs); // Get register usage.

      if (inst.isLabel()) {
        Label lab = (Label) inst;
        cs = lab.getStrength();
      }

      inst = inst.getNext();
      k++;
    }
 
    return insts;
  }

  /**
   * Compute the liveness for every register at every instruction.
   */
  protected BitVect[] computeLiveness(Instruction[] insts)
  {
    int ni = insts.length;
    int nu = regUse.length;
    int nr = registers.numRegisters();

    BitVect[] in  = new BitVect[nu];
    BitVect[] out = new BitVect[nu];

    for (int i = 0; i < nu; i++) {
      BitVect bi = regUse[i].clone();

      in[i]  = bi;
      out[i] = new BitVect();
    }

    // Set up "work list" for liveness algorithm.

    int wln  = (ni + 31) / 32;
    int[] wl = new int[wln];

    for (int i = 0; i < wln; i++)
      wl[i] = 0xffffffff;

    int m = ni - ((wln - 1) * 32);
    if (m < 32)
       wl[wln - 1] = (1 << m) - 1; // This is right-to-left.

    int[] newin = new int[(nr + 31) / 32];

    // Liveness for register computation.

    int tryfirst = 0; // Index of next item on the worklist.
    while (true) {
      int index = tryfirst; // Work list word index.

      tryfirst = -1;

      if (index < 0) { // Find a worklist item.
        int wli; // Worklist word index;
        for (wli = wln - 1; wli >= 0; wli--) {
          if (wl[wli] != 0)
            break;
        }
        if (wli < 0)
          break; // Work list is empty.

        // Find bit offset of first worklist item.

        int wlw = wl[wli];
        int wlb = 0; // Worklist word bit offset.
        if ((wlw & 0xffff) == 0)
          wlb += 16;
        if (((wlw >> wlb) & 0xff) == 0)
          wlb += 8;
        if (((wlw >> wlb) & 0x0f) == 0)
          wlb += 4;
        if (((wlw >> wlb) & 0x03) == 0)
          wlb += 2;
        if (((wlw >> wlb) & 0x01) == 0)
          wlb += 1;

        int bit = 1 << wlb; // This is right-to-left.
        wl[wli] = wlw & ~bit; // Remove from work list.
        index = wli * 32 + wlb;
      } else {
        int wli = index / 32;
        int wlb = index - wli * 32;
        int bit = 1 << wlb; // This is right-to-left.
        wl[wli] = wl[wli] & ~bit; // Remove from work list.
      }


      Instruction inst = insts[index];
      BitVect     bs   = out[index];

      // Look at successors.

      if (inst.isBranch()) {
        Branch br = (Branch) inst;
        int    nt = br.numTargets();
        for (int i = 0; i < nt; i++) {
          Label lab = br.getTarget(i);
          int   ins = lab.getTag();
          bs.or(in[ins]);
        }
        if (index < (insts.length - 1))
          if (!insts[index + 1].isLabel())
            bs.or(in[index + 1]);
      } else if (index < (insts.length - 1))
        bs.or(in[index + 1]);

      bs.copyTo(newin);

      regDef[index].andNotTo(newin);

      if (in[index].orTo(newin)) { // Look at predecessors.
        in[index].define(newin);
        if (inst.isLabel()) {
          Label lab = (Label) inst;
          int   l   = lab.numPredecessors();
          for (int i = 0; i < l; i++) {
            Instruction p = lab.getPredecessor(i);
            tryfirst = p.getTag();
            int wi = tryfirst / 32;
            int bi = tryfirst - wi * 32;
            wl[wi] |= 1 << bi; // This is right-to-left.
          }
        } else if (index > 0) {
          tryfirst = index - 1;
          int wi = tryfirst / 32;
          int bi = tryfirst - wi * 32;
          wl[wi] |= 1 << bi; // This is right-to-left.
        }
      }
    }

    for (int i = 0; i < nu; i++)
      in[i].or(regMod[i]);

    // If there is a definition but the definition is not live
    // immediately after, it is not used at all.  Since dead code
    // can expose other dead code, we go backwards through the
    // instructions to eliminate dead chains.
    
    int l = nu - 1;
    for (int i = l - 1; i > -1; i--) {
      BitVect d = regDef[i];
        
      if (d.empty())
        continue;
        
      // Find the next live instruction.
        
      int nl = i + 1;
      while (insts[nl].nullified()) 
        nl++;
  
      if (d.intersect(in[nl]))
        continue;
         
      // Remove un-needed defs.

      insts[i].nullify(registers);
    }
 
    return in;
  }

  /**
   * Remove un-needed bit vectors from the liveness array.  If two
   * consecutive bit vectors are equivalent, the second one is
   * removed.  This method modifies the argument.
   */
  protected BitVect[] compress(BitVect[] in)
  {
    if (in.length < 2)
      return in;

    BitVect test = in[0];
    int     k    = 1;
    for (int i = 1; i < in.length; i++) {
      BitVect x = in[i];
      if (!x.equivalent(test)) {
        in[k++] = x;
        test = x;
      }
    }

    if (k == in.length)
      return in;

    BitVect[] res = new BitVect[k];
    System.arraycopy(in, 0, res, 0, k);

    return res;
  }

  /**
   * Convert a m by n bit array to an n by m bit array.  The bit array
   * is represented by an array of bit vectors.
   * @param n is the max in columns
   */
  protected BitVect[] transpose(BitVect[] in, int n)
  {
    BitVect[] out = new BitVect[n];

    if (false) {
      for (int i = 0; i < n; i++)
        out[i] = BitVect.getSlice(i, in);
    } else {
      BitVect.transpose(out, in);
    }

    return out;
  }

  /**
   * The predecessors of labels are determined.
   * @param first is the first instruction
   */
  protected void computePredecessors(Instruction first)
  {
    Instruction inst = first; // Current instruction in the instruction list.
    Instruction last = null;  // Last instruction in the instruction list.
    Instruction lact = null;  // Last non-line marker in the instruction list.

    // Remove all predecessors.

    while (inst != null) {
      if (inst.isLabel())
        ((Label) inst).removePredecessors();
      inst = inst.getNext();
    }

    // Find the predecessors of each label.
    // Two cases:
    //   1. a branch to the label
    //   2. a fall through to the label (the label has multiple incoming edges
    //      all but one of which is from a branch)

    for (inst = first; inst != null; inst = inst.getNext()) {
      if (inst.isBranch()) { // Compute the predecessors of each label.
        Branch br = (Branch) inst;
        int    nt = br.numTargets();
        for (int i = 0; i < nt; i++) {
          Label lab = br.getTarget(i);
          lab.addPredecessor(br);
        }
      } else if (inst.isMarker()) {
        if (inst.isLabel()) {
          Label lab = (Label) inst;
          if ((lact != null) && !lact.isBranch()) // Case 2.
            lab.addPredecessor(last);
        } else if (inst instanceof LineMarker) {
          // Process line markers so that they don't mask a branch.
          last = inst;
          continue;
        }
      }
      lact = inst;
      last = inst;
    }
  }

  /**
   * The AGGRESSIVE_SPAN_LIMIT must be larger than the longest
   * sequence of instructions generated for an operation.  The
   * RELAXED_SPAN_LIMIT must be greater than the
   * AGGRESSIVE_SPAN_LIMIT.
   */
  private static final int RELAXED_SPAN_LIMIT    = 10;
  private static final int AGGRESSIVE_SPAN_LIMIT =  5;

  /**
   * Insert loads and stores to shorten the liveness ranges of a virtual register.
   * @param reg is the register to spill
   * @param first is the first instruction of the program
   * @param aggressive is true if aggressive spilling should be used
   */
  protected void spill(int reg, Instruction first, boolean aggressive)
  {
    if (regToDecls != null) { // Collect statistics on spills.
      System.out.print("** spi ");
      System.out.print(registers.display(reg));
      if ((reg < regToDecls.length) && (regToDecls[reg] != null)) {
        System.out.print(" ");
        System.out.print(regToDeclsMode[reg]);
        System.out.print(" ");
        System.out.print(regToDecls[reg].getName());
      }
      System.out.println("");
    }

    Instruction inst = first;
    Instruction last = null;
    Instruction ins  = null;
    Object      loc  = generator.getSpillLocation(reg);
    int         span = 2 * RELAXED_SPAN_LIMIT;

    // Set the limit of the span, between references to the register,
    // that triggers a spill load.  It must larger than the largest
    // sequence of instructions needed to restore the value or bad
    // things happen.  This is a kludge that can only be solved by
    // marking the begining and ending of such sequences.

    int limit = aggressive ? AGGRESSIVE_SPAN_LIMIT : RELAXED_SPAN_LIMIT; // Distance from last use of virtual register.

    generator.resetForBasicBlock();

    while (inst != null) {
      Instruction next = inst.getNext();

      if (inst.isMarker()) {
        if (inst.isLabel()) {
          Label lab = (Label) inst;
          if (lab.isFirstInBasicBlock()) {
            span = 2 * RELAXED_SPAN_LIMIT;
            generator.resetForBasicBlock();
          }
        }

        last = inst;
        inst = next;
        continue;
      }

      span++;

      if (inst.isSpillLoadPoint())
        ins = last;

      if (inst.uses(reg, registers) && !inst.nullified()) {
        if ((span > limit) && !last.defs(reg, registers)) { // Re-load spilled register.
          last = generator.insertSpillLoad(reg, loc, ins);
          spillLoadCount++;
          for (Instruction si = ins.getNext(); si != last; si = si.getNext())
            si.markSpillInstruction();
          last.markSpillInstruction();
        }
        span = 0;
      }

      if (inst.defs(reg, registers) && !inst.nullified()) {
        span = 0;
        if (loc != null) { // Insert spill (store) instruction after defining instruction.

          // Skip til end of sequence (past multi-instruction
          // sequences that define the same virtual register).

          while (next != null) {
            if (inst.isSpillStorePoint())
              break;

            inst = next;
            next = inst.getNext();
          }

          // Insert the spill.

          Instruction store = generator.insertSpillStore(reg, loc, inst);
          spillStoreCount++;
          for (Instruction si = inst.getNext(); si != store; si = si.getNext())
            si.markSpillInstruction();
          store.markSpillInstruction();

          if ((next != null) && next.isLabel())
            ((Label) next).replacePredecessor(inst, store);

          last = store;
          inst = store.getNext();
          while ((inst != null) &&
                 !inst.defs(reg, registers) &&
                 (inst.uses(reg, registers) || (inst instanceof LineMarker))) {
            last = inst;
            inst = inst.getNext();
          }
          continue;
        }
      }

      last = inst;
      inst = next;
    }
  }
}
