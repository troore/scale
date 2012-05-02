package scale.backend;

import scale.common.*;
import java.text.DecimalFormat;

/** 
 * This class implements a quick and dirty register allocator.
 * <p>
 * $Id: QDRA.java,v 1.75 2007-10-04 19:57:49 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * It's quick not because it runs quickly but because it was written
 * quickly.  It's dirty because it's not been proven to terminate in
 * all cases.
 * <p>
 * <h2>Algorithm</h2>
 * <pre>
 *   FOR trip = 1 to 30 DO
 *      compute the importance of each instruction (Note 1)
 *      FOR each instruction
 *        obtain the registers used by the instruction
 *        obtain the registers defined by the instruction
 *        obtain the registers clobbered by the instruction
 *      FOREND
 *      compute register liveness across instructions
 *      FOR each virtual register (VA)
 *        compute VA's importance (Note 2)
 *      FOREND
 *      sort virtual registers by importance 
 *      FOR each un-allocated virtual register (VA) in order of its importance
 *        FOR each real register (RA) (Note 3)
 *          IF the liveness of VA does not conflict with the liveness of RA THEN
 *            allocate VA to RA
 *            update RA's liveness
 *            BREAK
 *        FOREND
 *      FOREND
 *      IF no un-allocated virtual registers remain THEN
 *        RETURN allocation map
 *      spill
 *   FOREND
 *   ABORT(Allocation failure)
 * </pre>
 * <b>Notes</b>
 * <ol>
 * <li> The importance of an instruction is basically its loop_depth * 10.
 * <li> The importance of a register is a function of the sum of the
 * importance of the instructions that use the register and the number
 * of instructions over which the register is live.
 * <li> Real registers are checked in the order specified by the
 * register set definition.
 * </ol>
 * <p>
 * Our "bin-packing" register allocator is based on the bin-packing
 * register allocator as described in [Truab "Quality and Speed in
 * Linear-scan Register Allocation"].
 * <p>
 * <h3>Spilling</h3>
 * <p>
 * We use two different methods to select virtual registers to be
 * spilled.  The method chosen is determined by a heuristic.  The
 * first spill method (meek) simply spills those virtual registers
 * that were not allocated.  The second spill method (aggressive)
 * attempts to spill those virtual registers with the longest live
 * sequence. Since that information is not readily available, it uses
 * the number of instructions over which the register is live as an
 * approximation.  The number spilled by the second method is the
 * number of virtual registers that were not allocated.
 * <p>
 * The heuristic for selecting the method is simply to choose the
 * aggressive method if there are more un-allocated virtual registers
 * than real registers or if the meek method has already been used in
 * a previous attempt.
 * <p>
 * When spilling, register saves are inserted immediately after any
 * code sequence that defines the virtual register.  Code sequences
 * basically consist of those instructions generated for one CFG node.
 * Code sequences are defined by the backend code generator which
 * marks the last instruction in a sequence.
 * <p>
 * The point at which a spilled virtual register is loaded is also
 * determined by heuristic.  If the virtual register is defined only
 * once, it is reloaded immediately before every use.  Otherwise, it
 * is reloaded immediately before the first use in a basic block.
 */
public class QDRA extends RegisterAllocator
{
  private static int spillCount = 0;
  private static int redoCount  = 0;
  private static int maxVRCount = 0;

  private static final String[] stats = {"spills", "redo", "maxVirtualRegs"};

  static
  {
    Statistics.register("scale.backend.QDRA", stats);
  }

  /**
   * Return the number of spills required.
   */
  public static int spills()
  {
    return spillCount;
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

  private int       ssc;         // Initial spill count.
  private int       sslc;        // Initial spill load count.
  private int       sssc;        // Initial spill store count.
  private int[]     unallocated; // List of un-allocated virtual registers.
  private int[]     sorted;      // Virtual registers sorted in the order to be allocated.
  private double[]  strengths;   // Strength of each register.
  private boolean[] spilled;     // True if register has been spilled.
  private int[]     map;         // Map from virtual register to real register.

  /**
   * Setup a quick & dirty register allocation.
   * @param gen is the instruction generator in use
   * @param trace is true if the register allocation should be traced
   */
  public QDRA(Generator gen, boolean trace)
  {
    super(gen, trace);
    ssc  = spillCount;
    sslc = spillLoadCount;
    sssc = spillStoreCount;
  }

  /**
   * Determine a mapping from virtual registers to real registers.
   * This may involve the insertion of instructions to load and store
   * the value of a virtual register.
   * @param firstInstruction is the first instruction in the
   * instruction sequence
   * @return the mapping from virtual register to real register
   */
  public int[] allocate(Instruction firstInstruction)
  {
    int nr  = registers.numRegisters();
    int nrr = registers.numRealRegisters();
    int nvr = nr - nrr;

    map         = new int[nr];
    spilled     = new boolean[nr];
    unallocated = new int[100];
    sorted      = new int[nvr];
    strengths   = new double[nvr];

    if (nr > maxVRCount)
      maxVRCount = nr;

    for (int trip = 0; trip < 30; trip++) {
      computePredecessors(firstInstruction);

      Instruction[] insts   = linearize(firstInstruction);
      BitVect[]     live    = compress(computeLiveness(insts));
      BitVect[]     turned  = transpose(live, nr);
      int           unalloc = allocateRealRegisters(registers, turned);

      if (unalloc == 0) {
        int num = spillCount - ssc;
        if ((num > 0) && (trace || Debug.debug(2))) {
          System.out.print("  spills ");
          System.out.print(generator.useMemory);
          System.out.print(" ");
          System.out.print(generator.getCurrentRoutine().getName());
          System.out.print(" r:");
          System.out.print(num);
          System.out.print(" l:");
          System.out.print(spillLoadCount - sslc);
          System.out.print(" s:");
          System.out.println(spillStoreCount - sssc);
        }
        return map; // All virtual registers are allocated.
      }

      if (trace || Debug.debug(2)) {
        System.out.print("** QDRA " + nr + " " + nrr + ":");
        for (int i = 0; i < unalloc; i++)
          if (unallocated[i] > 0) {
            System.out.print(" ");
            System.out.print(registers.display(unallocated[i]));
          }
        System.out.println("");
      }
 
      // Spilling required.

      String  allocType = "";
      boolean changed   = false;

      if (trip > 4) {
        changed = fullSpill(unalloc, turned, firstInstruction);
        allocType = "full";
      } else {
        changed = partialSpill(unalloc, turned, firstInstruction);
        allocType = "part";
      }

      int nr2 = registers.numRegisters();
      if (nr2 > nr) {
        boolean[] nspilled = new boolean[nr2];
        System.arraycopy(spilled, 0, nspilled, 0, nr);

        spilled   = nspilled;
        nr        = nr2;
        nvr       = nr - nrr;
        map       = new int[nr];
        sorted    = new int[nvr];
        strengths = new double[nvr];
      }

      if (!changed)
        throw new scale.common.InternalError("Register allocation failed - no change.");

      if (trace) {
        System.out.print("Spill ");
        System.out.print(allocType);
        System.out.print(' ');
        System.out.print(trip);
        System.out.print(' ');
        System.out.print(nrr);
        System.out.print(' ');
        System.out.print(nr);
        System.out.print(' ');
        System.out.println(unalloc);
      }

      redoCount++;
    }

    throw new scale.common.InternalError("Register allocation failed - too many attempts.");
  }

  /**
   * Spill the specified register.
   * @param reg is the register to be spilled, spilled[reg] is set true
   * @param firstInstruction is the first instruction in the program
   * @param aggressive is true for aggressive spilling
   */
  private void spillReg(int reg, Instruction firstInstruction, boolean aggressive)
  {
    if (trace)
      System.out.println("spill " + registers.display(reg));
    spill(reg, firstInstruction, aggressive || (registers.numContiguousRegisters(reg) > 1));
    spillCount++;
    spilled[reg] = true;
  }

  /**
   * Spill the registers that were not allocated and one other whose
   * live range overlapped.
   * @param numNotAllopcated is the number of un-allocated virtual registers
   * @param turned[reg] is the live range for register reg
   * @param firstInstruction is the first instruction in the program
   */
  private boolean partialSpill(int         numNotAllocated,
                               BitVect[]   turned,
                               Instruction firstInstruction)
  {
    int     nr      = registers.numRegisters();
    int     nrr     = registers.numRealRegisters();
    boolean changed = false;

    for (int un = 0; un < numNotAllocated; un++) {
      int vr = unallocated[un];
      if (vr == 0)
        continue;

      if (trace)
        System.out.println("try " + registers.display(vr));

      if (!spilled[vr]) {
        spillReg(vr, firstInstruction, true);
        changed = true;
      }

      // Find an un-spilled register, to spill also, from the set of
      // registers that were allocated last time around.

      int best      = -1;
      int bestCount = -1;

      for (int i = nrr; i < nr; i++) {
        if (map[i] >= nrr)
          continue; // Not allocated last time around.

        if (spilled[i])
          continue; // Already spilled.

        if (registers.continueRegister(i))
          continue; // Spill main register.

        if (registers.floatRegister(vr) ^ registers.floatRegister(i))
          continue; // Not the right type.

        if (regUseCnt[i] <= 1)
          continue; // Spilling likely to have no bennefit.

        // Pick the register whose live ranges overlap the live ranges of
        // the un-allocated register the most.

        int ic = turned[vr].intersectCount(turned[i]);
        if (ic <= bestCount)
          continue;

        bestCount = ic;
        best = i;
      }

      if (best < 0) // No suitable candidate found.
        continue;

      // Spill register best.

      spillReg(best, firstInstruction, true);
      changed = true;
    }

    return changed;
  }

  /**
   * Spill the registers that were not allocated and all the registers
   * whose live ranges overlapped.
   * @param numNotAllopcated is the number of un-allocated virtual registers
   * @param turned[reg] is the live range for register reg
   * @param firstInstruction is the first instruction in the program
   */
  private boolean fullSpill(int         numNotAllocated,
                            BitVect[]   turned,
                            Instruction firstInstruction)
  {
    int     nr      = registers.numRegisters();
    int     nrr     = registers.numRealRegisters();
    boolean changed = false;

    for (int un = 0; un < numNotAllocated; un++) {
      int vr = unallocated[un];
      if (vr == 0)
        continue;

      if (trace)
        System.out.println("try " + registers.display(vr));

      if (!spilled[vr]) {
        spillReg(vr, firstInstruction, true);
        changed = true;
      }

      // Find and spill each un-spilled register whose live range overlaps.

      for (int i = nrr; i < nr; i++) {
        if (map[i] >= nrr)
          continue; // Not allocated last time around.

        if (spilled[i])
          continue; // Already spilled.

        if (registers.continueRegister(i))
          continue; // Spill main register.

        if (registers.floatRegister(vr) ^ registers.floatRegister(i))
          continue; // Not the right type.

        if (regUseCnt[i] <= 1)
          continue; // Spilling likely to have no bennefit.

        int ic = turned[vr].intersectCount(turned[i]);
        if (ic <= 0)
          continue;

        // Spill the register because its live range overlaps..

        spillReg(i, firstInstruction, true);
        changed = true;
      }
    }

    return changed;
  }

  private String formatInt(long v, int f)
  {
    String s = Long.toString(v);
    int    l = s.length();
    if (l == f)
      return s;
    if (l > f)
      return "*********************************".substring(0, f);
    return ("             ".substring(0, f - l) + s);
  }

  /**
   * Generate a virtual register to real register mapping.
   * @param regs is the register set of the machine
   * @param turned is the liveness of each register
   * @return the number of virtual registers that were not allocated.
   */
  private int allocateRealRegisters(RegisterSet regs, BitVect[] turned)
  {
    int nr  = regs.numRegisters();
    int nrr = regs.numRealRegisters();
    int nvr = nr - nrr;

    for (int i = 0; i < nrr; i++)
      map[i] = i;

    for (int i = 0; i < nvr; i++) {
      int virtualReg = nrr + i;
      map[virtualReg] = virtualReg;
    }

    // Determine the importance of each register.  Importance is the
    // measure of how important it is to allocate it before other
    // registers.

    for (int i = 0; i < nvr; i++) {
      int virtualReg = nrr + i;
      int liveness   = turned[virtualReg].count();
      if (liveness <= 1) {
        liveness = 1;
        spilled[virtualReg] = true; // Inhibit spilling.
      }
      sorted[i] = i;

      // Allocate the registers with the shortest live ranges first.
      // Spilling them is less likely to be effective.

      strengths[i] = 1.0 / liveness;
    }

    // Sort virtual registers in descending order of importance using
    // a combination sort.

    int     jumpSize = nvr;
    boolean flag;
    do {
      flag = false;
      jumpSize = (10 * jumpSize + 3) / 13;
      int ul = nvr - jumpSize;
      for (int i = 0; i < ul; i++) {
        int k  = i + jumpSize;
        int i1 = sorted[i];
        int i2 = sorted[k];
        // The test below for the register number being less is not
        // strictly necessary but it helps in debugging when comparing
        // two .s files.
        boolean swap = (strengths[i2] > strengths[i1]) ||
                       ((strengths[i2] == strengths[i1]) && (i2 < i1));
        if (swap) {
          sorted[i] = i2;
          sorted[k] = i1;
          flag = true;
        }
      }
    } while (flag || (jumpSize > 1));

    assert trace0(turned, nvr, nrr);

    // Multiple virtual registers may be mapped to the same real register.

    int notAllocatedCnt = (regs.useContiguous() ?
                           allocateRealRegsContiguous(regs, turned) :
                           allocateRealRegsNonContiguous(regs, turned));

    assert (notAllocatedCnt == 0) || trace1(turned, notAllocatedCnt, nvr, nrr);

    return notAllocatedCnt;
  }

  private boolean trace0(BitVect[] turned, int nvr, int nrr)
  {
    if (!trace)
      return true;

    int           k      = 0;
    DecimalFormat format = new DecimalFormat("###0.000000");
    for (int j = 0; j < nrr; j++) {
      if (turned[j].count() == 0)
        continue;
      System.out.print(registers.display(j));
      System.out.println("   " + turned[j]);
    }

    for (int j = 0; j < nvr; j++) {
      int i = sorted[j];
      int virtualReg = nrr + i;

      if (registers.continueRegister(virtualReg))
        continue; // Not allocatable.

      System.out.print(formatInt(k++, 5));
      System.out.print(" ");
      System.out.print(registers.display(virtualReg));
      System.out.print(" ");
      System.out.print(format.format(strengths[i]));
      System.out.println("   " + turned[virtualReg]);
    }

    return true;
  }

  private boolean trace1(BitVect[] turned, int notAllocatedCnt, int nvr, int nrr)
  {
    if (!trace)
      return true;

    StringBuffer buf = new StringBuffer("");

    System.out.println("** " + notAllocatedCnt + " not allocated.");
    DecimalFormat format = new DecimalFormat("###0.000000");
    for (int i = 0; i < nvr; i++) {
      int virtualReg = nrr + i;

      if (registers.continueRegister(virtualReg))
        continue;

      buf.setLength(0);

      int realReg = map[virtualReg];

      if (realReg < nrr)
        continue;

      buf.append("Nomap ");

      buf.append(registers.display(virtualReg));
      buf.append(" ");
      buf.append(format.format(strengths[virtualReg - nrr]));
      buf.append(" ");
      int lv = buf.length();

      buf.append(turned[virtualReg]);

      if (realReg < nrr) {
        System.out.println(buf.toString());
        buf.setLength(0);
        buf.append("      ");
        buf.append(registers.display(realReg));
        int lr = buf.length();
        if (lv > lr)
          buf.append("                                   ".substring(0, lv - lr));
        buf.append(turned[realReg]);
      }
      System.out.println(buf.toString());
    }

    return true;
  }

  /**
   * Assign the virtual registers to real registers where continue
   * registers are assigned to contiguous real registers.
   * Used for the Sparc for extended precision (integer and floating point).
   */
  private int allocateRealRegsContiguous(RegisterSet regs, BitVect[] turned)
  {
    int     nr              = regs.numRegisters();
    int     nrr             = regs.numRealRegisters();
    int     nvr             = nr - nrr;
    short[] preferredOrder  = regs.getPreferredOrder(); // Order real registers should be allocated.
    int     notAllocatedCnt = 0;

    for (int i = 0; i < nvr; i++) { // For each virtual register.
      int virtualReg = nrr + sorted[i];

      if (registers.continueRegister(virtualReg))
        continue; // Not allocatable.

      for (int j = 0; j < preferredOrder.length; j++) {
        // For each real register, allocate all virtual registers that
        // do not conflict.

        int realReg = preferredOrder[j];

        if (regs.readOnlyRegister(realReg))
          continue;

        if (!registers.compatible(realReg, virtualReg))
          continue; // Not compatible.

        if (turned[realReg].intersect(turned[virtualReg]))
          continue; // Register conflict.

        int fr  = registers.rangeBegin(realReg);
        int lr  = registers.rangeEnd(realReg);
        int ncr = regs.numContiguousRegisters(virtualReg);

        assert ((fr + ncr - 1) == lr) :
          "Real register (" + realReg + ") does not match " + virtualReg;

        if (fr != realReg) { // Check all contiguous real registers needed.
          // If the first register (fr) is not the same as the real
          // register (realReg), this means that we have a "real
          // register" mapped onto a real register.  For example, on
          // the Sparc V8, a double precision real register is mapped
          // onto two single precision real registers.

          // If a 64-bit long long is mapped onto two 32-bit integers
          // registers, it is possible that a conversion of the 64-bit
          // value to 32-bits may result in referencing only the
          // "continue" register part of the virtual register.  That's
          // why there are two checks below.  It's conceivable that
          // only the second one is really needed and that better
          // register allocation may result if the first is removed.

          boolean flg = false;
          for (int k = 0; k < ncr; k++) {
            if (turned[fr + k].intersect(turned[virtualReg]) ||
                turned[fr + k].intersect(turned[virtualReg + k])) {
              flg = true;
              break; // Not all contiguous real registers, that are needed, are available.
            }
          }
          
          if (flg)
            continue; // Register conflict.

          for (int k = 0; k < ncr; k++) {
            turned[fr + k].or(turned[virtualReg]);
            turned[fr + k].or(turned[virtualReg + k]);
          }
        }

        // Found a virtual register to allocate to the real register.

        for (int k = 0; k < ncr; k++) {
          turned[realReg + k].or(turned[virtualReg]);
          map[virtualReg + k] = realReg + k;
        }

        break;
      }

      if (map[virtualReg] >= nrr) {
        if (notAllocatedCnt >= unallocated.length) {
          int[] nua = new int[notAllocatedCnt * 2];
          System.arraycopy(unallocated, 0, nua, 0, notAllocatedCnt);
          unallocated = nua;
        }
        unallocated[notAllocatedCnt++] = virtualReg;
      }
    }

    return notAllocatedCnt;
  }

  /**
   * Assign the virtual registers to real registers where continue
   * registers are not assigned to contiguous real registers.
   * Used for the Alpha for structs in registers.
   */
  private int allocateRealRegsNonContiguous(RegisterSet regs, BitVect[] turned)
  {
    int     nr              = regs.numRegisters();
    int     nrr             = regs.numRealRegisters();
    int     nvr             = nr - nrr;
    short[] preferredOrder  = regs.getPreferredOrder(); // Order real registers should be allocated.
    int     notAllocatedCnt = 0;

    for (int i = 0; i < nvr; i++) { // For each virtual register.
      int virtualReg = nrr + sorted[i];

      for (int j = 0; j < preferredOrder.length; j++) {
        // For each real register, allocate all virtual registers that
        // do not conflict.

        int realReg = preferredOrder[j];

        if (regs.readOnlyRegister(realReg))
          continue;

        if (!registers.compatibleNS(realReg, virtualReg))
          continue; // Not compatible.

        if (turned[realReg].intersect(turned[virtualReg]))
          continue; // Register conflict.

        // Found a virtual register to allocate to the real register.

        turned[realReg].or(turned[virtualReg]);
        map[virtualReg] = realReg;

        break;
      }

      if (map[virtualReg] >= nrr) {
        if (notAllocatedCnt >= unallocated.length) {
          int[] nua = new int[notAllocatedCnt * 2];
          System.arraycopy(unallocated, 0, nua, 0, notAllocatedCnt);
          unallocated = nua;
        }
        unallocated[notAllocatedCnt++] = virtualReg;
      }
    }

    return notAllocatedCnt;
  }
}
