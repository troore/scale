package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

import java.text.DecimalFormat;

/** 
 * This class implements a quick and dirty register allocator for the
 * Trips TIL.
 * <p>
 * $Id: Trips2Allocator.java,v 1.76 2007-10-23 17:18:56 beroy Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This allocator is based upon {@link scale.backend.QDRA QDRA}.
 * It is different in that it only allocates registers for the virtual registers
 * that are alive across basic block boundaries.  The virtual registers that are
 * used only within a single basic block are not allocated.
 * </ol>
 */
public class Trips2Allocator 
{ 
  protected static int redoCount       = 0;
  protected static int maxVRCount      = 0;
  protected static int finalSpillCount = 0;
  protected static int finalSpillStCnt = 0;
  protected static int finalSpillLdCnt = 0;

  private static final String[] stats = {
    "redo", "maxVirtualRegs", "spills", "spillLoads", "spillStores"};
  
  static
  {
    Statistics.register("scale.backend.trips2.Trips2Allocator", stats);
  }

  /**
   * Return the number of spills required.
   */
  public static int spills()
  {
    return finalSpillCount;
  }

  /**
   * Return the number of loads inserted by spilling.
   */
  public static int spillLoads()
  {
    return finalSpillLdCnt;
  }

  /**
   * Return the number of stores inserted by spilling.
   */
  public static int spillStores()
  {
    return finalSpillStCnt;
  }

  /**
   * Return the number of times register allocation was re-done.
   */
  public static int redo()
  {
    return redoCount;
  }

  /**
   * Return the maximum number of virtual registers encountered.
   */
  public static int maxVirtualRegs()
  {
    return maxVRCount;
  }
  
  /**
   * Try to avoid spilling in a hyperblock if the spills would cause the hyperblock 
   * to violate a block constraint.
   */
  public static boolean LBSA2 = true; 
  
  protected int               spillCount;
  private   int               spillStCnt;
  private   int               spillLdCnt;
  protected int[]             unallocated;      // List of un-allocated virtual registers.
  protected int[]             sorted;           // Virtual registers sorted in the order to be allocated.
  protected double[]          strengths;        // Strength of each register.
  protected boolean[]         spilled;          // True if register has been spilled.
  protected int[]             map;              // Map from virtual register to real register.
  protected Hyperblock[]      hbs;              // Linearized hyberblocks.
  protected BitVect[]         tLiveUse;         // The transposed use set for each hyperblock (register -> hyperblock).
  protected BitVect[]         tLiveIn;          // The transposed in set for each hyperblock (register -> hyperblock).
  protected BitVect[]         tLiveOut;         // The transposed out set for each hyperblock (register -> hyperblock).
  private   BitVect[]         tMod;             // The transposed clobbered set for each hyperblock (register -> hyperblock).
  protected BitVect[]         blocksUsedIn;     // True if a register is used in a hyperblock (hyperblock -> register).
  protected BitVect[]         blocksDefdIn;     // True if a register is defined in a hyperblock (hyperblock -> register).
  protected boolean           trace;
  private   Trips2RegisterSet registers;
  private   Trips2Generator   generator;
  private   Hyperblock        hbStart;
  private   int[][]           readBanks; 
  private   int[][]           writeBanks;
  private   int[]             usedIn;
  private   int[]             defdIn; 
  
  /**
   * Setup a quick & dirty register allocation.
   * @param gen is the instruction generator in use
   * @param hbStart is the entry to the hyperblock flow graph
   * @param trace is true if the register allocation should be traced
   */
  public Trips2Allocator(Generator gen, Hyperblock hbStart, boolean trace)
  {
    this.generator = (Trips2Generator) gen;
    this.registers = (Trips2RegisterSet) generator.getRegisterSet();
    this.hbStart   = hbStart;
  }

  /**
   * Determine a mapping from virtual registers to real registers.
   * This may involve the insertion of spill code to load and store
   * the value of a virtual register.
   * @return the mapping from virtual register to real register.
   */
  public int[] allocate()
  { 
    initialize();

    BitVect fliv    = computeLiveness();
    int     unalloc = allocateRealRegisters(fliv);
    
    if (unalloc > 0)
      meekSpill(unalloc);
    
    if (Debug.debug(1))
      System.out.println("  spills " + spillCount);
        
    return map;
  }

  /**
   * Compute the statistics after register allocation.
   */
  protected void computeStats(String msg, int retries)
  {
    if (trace && spillCount > 0)
      System.out.println("** spill " + msg + " " + spillCount);

    finalSpillCount += spillCount;
    finalSpillLdCnt += spillLdCnt;
    finalSpillStCnt += spillStCnt;
    redoCount       += retries;
  }

  /**
   * Specify that a hyperblock uses a register.
   * @param hb is the hyperblock index
   * @param reg is the register
   * @param strength is the importance of the hyperblock
   * @see #defRegister(int,int)
   */
  private void useRegister(int hb, int reg)
  {
//     // If only the second register in a set is referenced we must
//     // consider the first register referenced too or the register
//     // allocator won't see it at all.
//     while (registers.continueRegister(reg))
//       reg--;

    int fr = reg;
    int lr = fr + registers.numContiguousRegisters(reg);
    
    for (int r = fr; r < lr; r++) {
      blocksUsedIn[r].set(hb);
    }
    
    fr = registers.rangeBegin(reg);
    if (fr == reg)
      return;
    
    lr = registers.rangeEnd(reg);
    for (int r = fr; r <= lr; r++) {
      blocksUsedIn[r].set(hb);
    }
  }

  /**
   * Specify that a hyperblock defines a register.
   * Uses must be specified before definitions.
   * @param hb is the instruction index
   * @param reg is the register
   * @see #useRegister(int,int,int)
   * @see scale.backend.Instruction#specifyRegisterUsage
   */
  private void defRegister(int hb, int reg)
  {
    int fr = reg;
    int lr = fr + registers.numContiguousRegisters(reg);
    
    for (int r = fr; r < lr; r++)
      blocksDefdIn[r].set(hb);

    fr = registers.rangeBegin(reg);
    if (fr == reg)
      return;
    
    lr = registers.rangeEnd(reg);
    for (int r = fr; r <= lr; r++)
      blocksDefdIn[r].set(hb);
  }
  
  /**
   * Initialize the register allocator.
   */
  protected void initialize()
  {
    int         n   = 0;
    int         nr  = registers.numRegisters();
    Stack<Node> hwl = WorkArea.<Node>getStack("linearizeHyperblocks");
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);
    
    while (!hwl.isEmpty()) {
      Hyperblock hb = (Hyperblock) hwl.pop();
      hb.pushOutEdges(hwl);
      hb.setTag(n);
      n++;
    }
    
    // Initialize for hardware register allocation.
    // New mod, use and def bit vectors for each hyperblock are created.
    
    hbs          = new Hyperblock[n];
    blocksUsedIn = new BitVect[nr];
    blocksDefdIn = new BitVect[nr];

    for (int i = 0; i < nr; i++) {
      blocksUsedIn[i] = new BitVect();
      blocksDefdIn[i] = new BitVect();
    }
      
    WorkArea.<Node>returnStack(hwl);
    
    int nrr        = registers.numRealRegisters();
    int numVirtual = nr - nrr;

    map         = new int[nr];
    spilled     = new boolean[nr];
    unallocated = new int[100];
    sorted      = new int[numVirtual];
    strengths   = new double[numVirtual];
    spillCount  = 0;
    spillLdCnt  = 0;
    spillStCnt  = 0;
  }
  
  /**
   * Compute liveness.
   */
  protected BitVect computeLiveness()
  {
    DataflowAnalysis df  = new DataflowAnalysis(hbStart, registers);     
    df.computeLiveness();
    
    BitVect[] regMod  = df.getMods();
    BitVect[] liveUse = df.getUses();
    BitVect[] liveIn  = df.getIns();
    BitVect[] liveOut = df.getOuts();
    
    Stack<Node> wl = WorkArea.<Node>getStack("computeLiveness");

    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
    
    int[] regs = new int[Trips2Machine.maxBlockSize * 2];
    while (!wl.isEmpty()) {
      Hyperblock hb   = (Hyperblock) wl.pop();
      int        htag = hb.getTag();
      
      hb.pushOutEdges(wl);
      hbs[htag] = hb;
      
      // Remove local variables from def and use sets.
    
      BitVect hbUse = df.getUse(htag).clone();
      BitVect hbDef = df.getDef(htag).clone();
      
      hbUse.and(liveIn[htag]);
      hbDef.and(liveOut[htag]);
      
      // Specify which registers are used and defined.   
    
      int numUses = hbUse.getSetBits(regs);
      for (int i = 0; i < numUses; i++) {
        int reg = regs[i];
        useRegister(htag, reg);
      }
    
      int numDefs = hbDef.getSetBits(regs);
      for (int i = 0; i < numDefs; i++) {
        int reg = regs[i];
        defRegister(htag, reg);
      }
    }
    
    WorkArea.<Node>returnStack(wl);
    
    // Convert bit vectors from hyperblock->register to register->hyperblock
    
    int numRegisters = registers.numRegisters();
    
    tLiveUse = new BitVect[numRegisters];
    tLiveIn  = new BitVect[numRegisters];
    tLiveOut = new BitVect[numRegisters];
    tMod     = new BitVect[numRegisters];
    
    BitVect.transpose(tLiveUse, liveUse);
    BitVect.transpose(tLiveIn,  liveIn);
    BitVect.transpose(tLiveOut, liveOut);
    BitVect.transpose(tMod,     regMod); 

    // All the virtual registers that need to be assigned to global registers.
    
    BitVect fliv = new BitVect();
    for (int i = 0; i < hbs.length; i++)
      fliv.or(liveIn[i]);

    int flivCount = fliv.count();
    if (flivCount > maxVRCount)
      maxVRCount = flivCount;
    
    return fliv;
  }

  /**
   * Spill the registers that were not allocated.  If the register to
   * be spilled is used in the hyperblock, a load is inserted at the
   * beginning of the block. Likewise if the register is defined in
   * the hyperblock, a store is inserted at the end of the block.
   * @param numNotAllocated is the number of un-allocated virtual registers
   */
  protected void meekSpill(int numNotAllocated)
  {
    if (Debug.debug(1)) {
      int nr  = registers.numRegisters();
      int nrr = registers.numRealRegisters();
      System.out.print("** Trips2Allocator " + nr + " " + nrr + ":");
      for (int i = 0; i < numNotAllocated; i++) {
        if (unallocated[i] > 0) {
          System.out.print(" ");
          System.out.print(unallocated[i]);
        }
      }
      System.out.println("");
    }
    
    boolean changed = false;
    int[]   blks    = new int[hbs.length];

    for (int un = 0; un < numNotAllocated; un++) {
      int vr = unallocated[un];
      if (vr == 0)
        continue;

      if (spilled[vr])
        continue;
      
      Object loc       = generator.getSpillLocation(vr);
      int    numUsedIn = blocksUsedIn[vr].getSetBits(blks);  
      for (int i = 0; i < numUsedIn; i++) {
        int        block = blks[i];
        Hyperblock hb    = hbs[block];
   
        generator.insertSpillLoad(vr, loc, hb);
        spillLdCnt++;
      }
    
      int numDefdIn = blocksDefdIn[vr].getSetBits(blks);
      for (int i = 0; i < numDefdIn; i++) {
        int        block = blks[i];
        Hyperblock hb    = hbs[block];
      
        generator.insertSpillStore(vr, loc, hb);
        spillStCnt++;
      }
    
      spillCount++;
      spilled[vr] = true;
      changed = true;
    }
    
    if (!changed)
      throw new scale.common.InternalError("Register allocation failed - no change from spilling.");
  }

  private String formatInt(long v, int f)
  {
    String s = Long.toString(v);
    int l = s.length();
    if (l == f)
      return s;
    if (l > f)
      return "*********************************".substring(0, f);
    return ("             ".substring(0, f - l) + s);
  }
 
  /**
   * Reserve register banks for all global registers used and defined
   * in each block.
   */
  private void reserveRegisterBanks()
  {
    int   spReg  = generator.getStackPtr();
    int   spBank = registers.getBank(spReg);
    int[] blks   = new int[hbs.length];

    readBanks  = new int[Trips2RegisterSet.numBanks][hbs.length];
    writeBanks = new int[Trips2RegisterSet.numBanks][hbs.length];
    
    // Reserve the stack pointer in all blocks because we may insert
    // spill code.
    
    for (int i = 0; i < hbs.length; i++)
      readBanks[spBank][i]++;
                   
    // Reserve all real registers.
    
    int nr = registers.numRealRegisters();
    for (int i = 0; i < nr; i++) {
      int bank = registers.getBank(i);
      
      if (i != spReg) { // The stack pointer was already reserved.
        int numUses = blocksUsedIn[i].getSetBits(blks);
        for (int j = 0; j < numUses; j++) {
          int block = blks[j];
          readBanks[bank][block]++;
        }
      }
      
      int numDefs = blocksDefdIn[i].getSetBits(blks);
      for (int j = 0; j < numDefs; j++) {
        int block = blks[j];
        writeBanks[bank][block]++;
      }
    }
  }
  
  /**
   * Compute the strength of a virtual register based on the size of
   * the hyperblocks the virtual register spans.  The strength will be
   * higher for virtual registers that span "full" hyperblocks so the
   * allocator will assign those first in an attempt to avoid spilling
   * into full hyperblocks to minimize block splits.
   */
  protected double computeStrengthBlockSize(int vr)
  {
    double strength = 0.0;
    int[]  blks     = new int[hbs.length]; 
   
    int numUsedIn = blocksUsedIn[vr].getSetBits(blks);  
    for (int i = 0; i < numUsedIn; i++) {
      int        block = blks[i];
      Hyperblock hb    = hbs[block];
      int        bs    = hb.getFanout() + hb.getBlockSize(); 
    
      if (bs >= Trips2Machine.maxBlockSize) 
        strength += 1.0;
      else
        strength += 1.0 / ((double) (Trips2Machine.maxBlockSize - bs));
    }
  
    int numDefdIn = blocksDefdIn[vr].getSetBits(blks);
    for (int i = 0; i < numDefdIn; i++) {
      int        block = blks[i];
      Hyperblock hb    = hbs[block];
      int        bs    = hb.getFanout() + hb.getBlockSize();
    
      if (bs >= Trips2Machine.maxBlockSize) 
        strength += 1.0;
      else
        strength += 1.0 / ((double) (Trips2Machine.maxBlockSize - bs));
    }
    
    return strength;
  }
  
  /**
   * Determine the importance of each register.  Importance is the measure
   * of how important it is to allocate it before other registers.
   */
  protected void computeStrength(int numVirtual, int numReal)
  {    
    for (int i = 0; i < numVirtual; i++) {
      int     virtualReg = numReal + i;
      BitVect liverange  = tLiveIn[virtualReg];
      
      liverange.or(tLiveOut[virtualReg]);
      liverange.or(tLiveUse[virtualReg]);
      
      int liveness = liverange.count();
      if (liveness <= 1) {
        liveness = 1;
        spilled[virtualReg] = true;
      }
      sorted[i] = i;
      
      // Allocate the registers with the shortest live ranges first.
      // Spilling them is less likely to be effective.
      strengths[i] = 1.0 / liveness; 
      
      // Account for the size of the blocks we would spill in to.
      // We don't want to spill into full blocks because we will
      // have to spill them.
      if (LBSA2)
        strengths[i] += computeStrengthBlockSize(virtualReg); 
    }
  }
  
  /**
   * Determine the order in which virtual registers should be assigned.
   */
  private void determineAssignmentOrder()
  {
    int numRegisters = registers.numRegisters();
    int numReal      = registers.numRealRegisters();
    int numVirtual   = numRegisters - numReal;      
    
    for (int i = 0; i < numRegisters; i++)
      map[i] = i;

    computeStrength(numVirtual, numReal);

    // Sort virtual registers in descending order of importance using
    // a combination sort.
    
    int     jumpSize = numVirtual;
    boolean flag;
    do {
      flag = false;
      jumpSize = (10 * jumpSize + 3) / 13;
      int ul = numVirtual - jumpSize;
      for (int i = 0; i < ul; i++) {
        int k  = i + jumpSize;
        int i1 = sorted[i];
        int i2 = sorted[k];
        boolean swap = ((strengths[i2] > strengths[i1]) ||
                        ((strengths[i2] > strengths[i1]) && (i2 < i1)));
        if (swap) {
          sorted[i] = i2;
          sorted[k] = i1;
          flag = true;
        }
      }
    } while (flag || (jumpSize > 1));

    if (trace) {
      int           k      = 0;
      DecimalFormat format = new DecimalFormat("###00.00");
      
      for (int j = 0; j < numReal; j++) {
        BitVect liverange = tLiveIn[j].clone();
        liverange.or(tLiveOut[j]);
        
        int liveness = liverange.count();
        if (liveness == 0)
          continue;
        
        System.out.print(registers.display(j));
        System.out.println("   " + liverange);
      }
      
      for (int j = 0; j < numVirtual; j++) {
        int i = sorted[j];
        int virtualReg = numReal + i;
        
        if (registers.continueRegister(virtualReg))
          continue; // Not allocatable.
        
        BitVect liverange = tLiveIn[virtualReg].clone();
        liverange.or(tLiveOut[virtualReg]);
        
        int liveness = liverange.count();
        
        System.out.print(formatInt(k++, 4));
        System.out.print(" ");
        System.out.print(registers.display(virtualReg));
        System.out.print(" ");
        System.out.print(format.format(strengths[i]));
        System.out.print(" = ");
        System.out.print(formatInt(liveness, 3));
        System.out.println("   " + liverange);
      }
    }
  }

  /**
   * Return true if this assignment violates a banking constraint.
   */
  private boolean hasBankViolation(int realReg, int virtualReg)
  {
    int bank = registers.getBank(realReg);
    
    // Check for read bank violation.

    int numUsedIn = blocksUsedIn[virtualReg].getSetBits(usedIn);
    for (int k = 0; k < numUsedIn; k++) {
      int block = usedIn[k];
      if (readBanks[bank][block] >= Trips2RegisterSet.bankAccesses)
        return true;
    }
    
    // Check for write bank violation.
    
    int numDefedIn = blocksDefdIn[virtualReg].getSetBits(defdIn);
    for (int k = 0; k < numDefedIn; k++) {
      int block = defdIn[k];
      if (writeBanks[bank][block] >= Trips2RegisterSet.bankAccesses)
        return true;

      // If we aren't nullifying writes we have to read in the
      // register. We only need the read though if there is a path 
      // through the block that doesnt def the write. I'm going to
      // assume we always need the read to get this working. TODO
      
      if (!Trips2Machine.nullifyWrites) {
        if (readBanks[bank][block] >= Trips2RegisterSet.bankAccesses)
          return true;
      }
    }

    // Update banking information for this assignment.
    
    for (int k = 0; k < numUsedIn; k++) {
      int block = usedIn[k];
      readBanks[bank][block]++;
    }
    
    for (int k = 0; k < numDefedIn; k++) {
      int block = defdIn[k];
      writeBanks[bank][block]++;

      if (!Trips2Machine.nullifyWrites) {
        readBanks[bank][block]++;
      }
    }
    
    return false;
  }
  
  /**
   * Generate a virtual register to real register mapping.
   * @param allocReg is true if the register should be allocated
   * @return the number of virtual registers that were not allocated.
   */
  protected int allocateRealRegisters(BitVect allocReg)
  {
    determineAssignmentOrder();
    reserveRegisterBanks();
        
    // Multiple virtual registers may be mapped to the same real register.

    int     numRegisters    = registers.numRegisters();
    int     numReal         = registers.numRealRegisters();
    int     numVirtual      = numRegisters - numReal; 
    short[] preferredOrder  = registers.getPreferredOrder(); // Order real registers should be allocated.
    int     notAllocatedCnt = 0;

    usedIn = new int[hbs.length];
    defdIn = new int[hbs.length];
    
    for (int i = 0; i < numVirtual; i++) {
      int virtualReg = numReal + sorted[i];
      
      if (!allocReg.get(virtualReg)) // Don't allocate "local" virtual registers.
        continue;
      
      if (map[virtualReg] < Trips2RegisterSet.regSetSize) // Already allocated.
        continue;

      for (int j = 0; j < preferredOrder.length; j++) {
        int realReg = preferredOrder[j];

        // Overlapping live ranges or a register clobbered by a call.
        
        if ((tLiveIn[realReg].intersect(tLiveIn[virtualReg])
            || tLiveOut[realReg].intersect(tLiveOut[virtualReg])
            || tMod[realReg].intersect(tLiveOut[virtualReg]))
            || tLiveIn[realReg].intersect(tLiveOut[virtualReg])
            || tLiveOut[realReg].intersect(tLiveIn[virtualReg]))
          continue;
        
        if (hasBankViolation(realReg, virtualReg))
          continue;
        
        // Found a virtual register to allocate to the real register.

        tLiveIn[realReg].or(tLiveIn[virtualReg]);
        tLiveOut[realReg].or(tLiveOut[virtualReg]);
        map[virtualReg] = realReg;
        break;
      }

      if (map[virtualReg] >= numReal) {
        if (notAllocatedCnt >= unallocated.length) {
          int[] nua = new int[notAllocatedCnt * 2];
          System.arraycopy(unallocated, 0, nua, 0, notAllocatedCnt);
          unallocated = nua;
        }
        unallocated[notAllocatedCnt++] = virtualReg;
      }
    }
    
    readBanks  = null;
    writeBanks = null;
    usedIn     = null;
    defdIn     = null;
    
    // Return if all virtual registers allocated.
    
    if (notAllocatedCnt == 0) 
      return 0;

    if (trace) {
      StringBuffer buf = new StringBuffer("");
      
      for (int i = 0; i < numVirtual; i++) {
        int virtualReg = numReal + i;

        if (registers.continueRegister(virtualReg))
          continue;

        if (!allocReg.get(virtualReg)) // Don't allocate "local" virtual registers.
          continue;

        buf.setLength(0);

        int realReg = map[virtualReg];

        if (realReg >= numReal)
          buf.append("Nomap ");
        else
          buf.append("Remap ");

        buf.append(registers.display(virtualReg));
        buf.append("(");
        buf.append(strengths[virtualReg - numReal]);
        buf.append(") ");
        int lv = buf.length();

        buf.append(tLiveIn[virtualReg]);
        buf.append(" / ");
        buf.append(tLiveOut[virtualReg]);

        if (realReg < numReal) {
          System.out.println(buf.toString());
          buf.setLength(0);
          buf.append("      ");
          buf.append(registers.display(realReg));
          int lr = buf.length();
          if (lv > lr)
            buf.append("                                   ".substring(0, lv - lr));
          buf.append(tLiveIn[realReg]);
          buf.append(" / ");
          buf.append(tLiveOut[realReg]);
        }
        System.out.println(buf.toString());
      }
    }

    return notAllocatedCnt;
  }
}

