package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/** 
 * This class implements a hybrid version of the trips register allocator.
 * <p>
 * $Id: Trips2AllocatorHybrid.java,v 1.3 2007-07-27 16:09:49 beroy Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This allocator is based upon {@link scale.backend.QDRA QDRA}.
 * It is different in that it only allocates registers for the virtual registers
 * that are alive across basic block boundaries.  The virtual registers that are
 * used only within a single basic block are not allocated.
 * <br>
 * This allocator first allocates virtual registers from shortest to longest
 * liveranges.  If there are any spills, it redoes the allocation in reverse
 * order, from longest to shortest liveranges and then picks the allocation 
 * with the fewest spills.
 * </ol>
 */
public class Trips2AllocatorHybrid extends scale.backend.trips2.Trips2Allocator 
{ 
  /**
   * True: use the hybrid register allocator.
   */
  public static boolean enabled = false;
  
  private boolean reverseVirtualRegisterOrder;    // Assign virtual registers in reverse order
  
  /**
   * Setup a quick & dirty register allocation.
   * @param gen is the instruction generator in use
   * @param hbStart is the entry to the hyperblock flow graph
   * @param trace is true if the register allocation should be traced
   */
  public Trips2AllocatorHybrid(Generator gen, Hyperblock hbStart, boolean trace)
  {
  	super(gen, hbStart, trace);
  }
  
  /**
   * Determine a mapping from virtual registers to real registers.
   * This may involve the insertion of spill code to load and store the value of a virtual register.
   * @return the mapping from virtual register to real register.
   */
  public int[] allocate()
  { 
    initialize();	
    reverseVirtualRegisterOrder = false;
    
    BitVect fliv    = computeLiveness();
    int     unalloc = allocateRealRegisters(fliv);
    
    if (unalloc > 0) {  
      int       spills           = computeSpills(unalloc);
      int       savedUnalloc     = unalloc;
      int[]     savedMap         = map.clone();
      int[]     savedUnallocated = unallocated.clone();
      boolean[] savedSpilled     = spilled.clone();

      // Redo in reverse
      
      initialize();	
      reverseVirtualRegisterOrder = true;
      
      fliv    = computeLiveness();
      unalloc = allocateRealRegisters(fliv);
      int rspills = computeSpills(unalloc);
      
      if (trace)
        System.out.println("*** # spill ld/sd normal: " + spills + "  reverse: " + rspills);

      // Choose the allocation that resulted in the fewest spills.
      
      if (rspills < spills) {
        map         = savedMap;
        unallocated = savedUnallocated;
        spilled     = savedSpilled;
        unalloc     = savedUnalloc;
      }    	
    }

    if (unalloc > 0)   
      meekSpill(unalloc);
    
    if (Debug.debug(1))
      System.out.println("  spills " + spillCount);
    
    return map;
  }
  
  /**
   * Return the number of instructions inserted for spills with the current allocation.
   */
  private int computeSpills(int numNotAllocated)
  {
    int[] blks   = new int[hbs.length];
    int   spills = 0;
    
    for (int un = 0; un < numNotAllocated; un++) {
      int vr = unallocated[un];
      if (vr == 0)
        continue;

      if (spilled[vr])
        continue;
      
      int numUsedIn = blocksUsedIn[vr].getSetBits(blks);  
      for (int i = 0; i < numUsedIn; i++)
        spills++;
    
      int numDefdIn = blocksDefdIn[vr].getSetBits(blks);
      for (int i = 0; i < numDefdIn; i++)
        spills++;
    }
    
    return spills;
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
      
      if (reverseVirtualRegisterOrder) {
        // Allocate registers with the longest live ranges first.
        strengths[i] = liveness;
      } else {
        // Allocate the registers with the shortest live ranges first.
        // Spilling them is less likely to be effective.
        strengths[i] = 1.0 / liveness;
      }
      
//      if (LBSA2)
  //      strengths[i] += computeStrengthBlockSize(virtualReg); 
    }
  }
}

