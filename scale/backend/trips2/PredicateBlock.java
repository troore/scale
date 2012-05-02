package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/**
 * This class represents a predicated basic block.
 * <p>
 * $Id: PredicateBlock.java,v 1.53 2007-10-04 19:57:59 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A predicated block is similar to a traditional basic block, except
 * that a predicate can be associated with the block.  All
 * instructions in the block are predicated on that predicate.  If no
 * predicate is specified, all instructions are considered to be
 * unpredicated.
 * <p>
 * A hyperblock is made up of a collection of predicated blocks.
 */
public class PredicateBlock extends Node
{
  private static int nullStoresInsertedCount = 0; // The number of nulls inserted for store nullification.
  private static int blocksSplitCount        = 0; // A count of the number of blocks which were split.
  private static int dummyStoresCount        = 0; // The number of dummy stores inserted.

  private static final String[] stats = { "blocksSplit", "nullStoresInserted", "dummyStores" };

  static
  {
    Statistics.register("scale.backend.trips2.PredicateBlock", stats);
  }

  /**
   * Return the number of blocks split.
   */
  public static int blocksSplit()
  {
    return blocksSplitCount;
  }

  /**
   * Return the number of nulls inserted.
   */
  public static int nullStoresInserted()
  {
    return nullStoresInsertedCount;
  }

  /**
   * Return the number of dummy stores inserted.
   */
  public static int dummyStores()
  {
    return dummyStoresCount;
  }

  private static int nextColor = 0;     // The next available unique color (for graph traversals).
  private static int nextLabel = 0;     // The next available unique label.

  private Instruction first;            // The first instruction in the block.
  private Instruction last;             // The last instruction in the block.
  private int[]       predicates;       // The predicate this block is predicated on.
  private int         blockSize;        // The size of the block (not including fanout and spills).
  private int         fanout;           // The estimated fanout for the block.
  private int         spillSize;        // The size of spill code in the block.
  private int         numSpills;        // The number of load/stores in the block which are spills.
  private int         maxLSID;          // The highest load/store ID assigned in this hyperblock.
  private boolean     hasBranch;        // True if the block contains a branch.
  private boolean     hasCall;          // True if the block contains a function call.
  private boolean     hasSwitch;        // True if the block contains a switch statement.
  private boolean     hasDummyStores;   // True if the block contains a dummy store for store nullification.
  private boolean     predicatedOnTrue; // The "sense" the block is predicated on.
  private boolean     isSplitPoint;     // True if this block is a good candidate for reverse if-conversion.
  
  /**
   * Construct an unpredicated block.
   */
  public PredicateBlock()
  {
    super(nextLabel++);
  }

  /**
   * Construct an unpredicated block beginning with first.
   */
  public PredicateBlock(Instruction first, Instruction last)
  {
    this();
    
    this.first = first;
    this.last  = last;
  }

  /**
   * Construct a predicated block.
   */
  public PredicateBlock(int predicate, boolean predicatedOnTrue)
  {
    this();

    setPredicate(predicate, predicatedOnTrue);
  }
  
  /**
   * Construct a predicated block.
   */
  public PredicateBlock(int[] predicates, boolean predicatedOnTrue)
  {
    this();

    setPredicates(predicates);
    
    this.predicatedOnTrue = predicatedOnTrue;
  }

  /**
   * Mark that the block has been visited.
   */
  public final void setVisited()
  {
    color = nextColor;
  }

  /**
   * Return true if the block has been visited.
   */
  public final boolean visited()
  {
    return color == nextColor;
  } 
  
  /**
   * The next unique color for traversing the graph.
   */
  public final void nextVisit()
  {
    nextColor++;
  }
  
  /**
   * Return the size of the instructions in the block (not including
   * fanout).
   */
  public final int getBlockSize()
  {
    return blockSize;
  }

  /**
   * Return the estimated fanout for the block.
   */
  public final int getFanout()
  {
    return fanout;
  }
  
  /**
   * Return the highest assigned load/store ID in the block.
   */
  public final int maxLSID()
  {
    return maxLSID;
  }
  
  public final void setMaxLSID(int maxLSID)
  {
    this.maxLSID = maxLSID;
  }
  
  /**
   * Return true if this block is a good candidate for reverse
   * if-conversion.
   */
  public final boolean isSplitPoint()
  {
    return isSplitPoint;
  }
  
  /**
   * Set that the block is a good candidate for reverse if-conversion.
   */
  public final void setSplitPoint()
  {
    isSplitPoint = true;
  }
  
  /**
   * Return true if this block has a branch.
   */
  public final boolean hasBranch()
  {
    return hasBranch;
  }
  
  /**
   * Return true if this block has a function call.
   */
  public final boolean hasCall()
  {
    return hasCall;
  }
  
  /**
   * Return true if this block has a switch.
   */
  public final boolean hasSwitch()
  {
    return hasSwitch;
  }
  
  /**
   * Set that the block has a branch.
   */
  public final void setHasBranch()
  {
    hasBranch = true;
  }

  /**
   * Return the number of spills in the block.
   */
  public final int numSpills()
  {
    return numSpills;
  }
  
  /**
   * Return the size of the spills in the block.
   */
  public final int getSpillSize()
  {
    return spillSize;
  }

  /**
   * Add a spill to the predicate block.
   * <br>
   * This method is used during register allocation to adjust 
   * the stats for the block during spilling.
   * <br>
   * Do not change the fanout in this routine. Add any fanout
   * to the spillSize for the block instead.
   */
  public final void addSpill(int size)
  {
    numSpills++;
    maxLSID++;
    spillSize += size;

    // the block splitter removes spills before computing the location
    // to split the block. without the actual spill instructions the 
    // fanout cannot be computed and the block splitter won't find a
    // place to split.

    spillSize++;  // for fanout of SP
  }
  
  /**
   * Return true if this block has any "dummy" stores inserted for
   * store nullification.
   */
  public final boolean hasDummyStores()
  {
    return hasDummyStores;
  }

  /**
   * Append the instruction to the end of the predicate block.
   */
  public final void appendInstruction(Instruction inst)
  {
    if (first == null)
      first = inst;
    else
      last.setNext(inst);

    last = inst;
  }
  
  /**
   * Append the instruction to the end of the predicate block and maintain SSA form.
   */
  public final void appendInstruction(Instruction inst, SSA ssa)
  {
    appendInstruction(inst);
    
    if (ssa != null) {
      int ra = inst.getDestRegister();
      if (ra > -1)
        ssa.setDef(inst, ra);
     
      int[] srcs = inst.getSrcRegisters();
      if (srcs != null) {
        for (int i = 0; i < srcs.length; i++) {
          ssa.addUse(inst, srcs[i]);
        }
      }
    }
  }
  
  /**
	 * Delete the specified instruction. Since this routine scans the list to
	 * find the previous instruction, it is O(n). Therefore, use the other form
	 * if possible.
	 */
  public final void removeInstruction(Instruction inst)
  {
	  Instruction i = this.first;
	  Instruction prev = null;
	  
	  while (i != inst) {
		  prev = i;
		  i = i.getNext();
	  }

	  removeInstruction(prev, i);
  }
  
  /**
   * Delete the specified instruction.
   */
  public final void removeInstruction(Instruction prev, Instruction inst)
  {
    if (prev != null)
      prev.setNext(inst.getNext());
    else if (inst == first)
      first = inst.getNext();

    if (inst == last)
      last = prev;
  }

  /**
   * Insert an instruction after the specified instruction.
   */
  public final Instruction insertInstructionAfter(Instruction after, Instruction inst)
  {
    inst.setNext(after.getNext());
    after.setNext(inst);

    if (after == last)
      last = inst;

    return inst;
  }

  /**
   * Insert an instruction at the beginning of the block.
   * <br>
   * Do not use this method in SSA form unless you understand what you are doing.
   * There may be a phi instruction in the predicate block you are inserting into
   * that defines the predicate for the predicate block. Calling this method will
   * insert the instruction before the phi!  For example,
   * <br>
   *   null_t<217> ...           // trying to insert a null in the head
   *   phi_t<200> 217 ...        // but the predicate is defined here by the phi!
   * <br>
   */
  public final void insertInstructionAtHead(Instruction inst)
  {
    if (first != null)
      inst.setNext(first);
    else
      last = inst;

    first = inst;
  }

  /**
   * Insert an instruction at the end of a block before the branch (if it exists).
   * <br>
   * TODO This is no longer needed
   */
  public final void insertInstructionBeforeBranch(Instruction inst)
  {
    if (last == null) {
      appendInstruction(inst);
      return;
    }
  
    if (!last.isBranch() && !last.isMarker()) {
      appendInstruction(inst);
      return;
    }
    
    // Find the branch and insert the instruction before it.

    Instruction prev = null;
    for (Instruction pinst = first;
         pinst != null;
         prev = pinst, pinst = pinst.getNext()) {
      if (pinst.isBranch()) {
        if (prev == null)
          insertInstructionAtHead(inst);
        else
          insertInstructionAfter(prev, inst);
        return;
      }
    }
    
    // No branch instruction and the last instruction is a marker.
  
    appendInstruction(inst);
  }
  
  /**
   * Return the first instruction in this block.
   */
  public final Instruction getFirstInstruction()
  {
    return first;
  }

  /**
   * Set the first instruction in this block.
   */
  public final void setFirstInstruction(Instruction first)
  {
    this.first = first;
  }

  /**
   * Return the last instruction in this block.
   */
  public final Instruction getLastInstruction()
  {
    return last;
  }

  /**
   * Set the last instruction in this block.
   */
  public final void setLastInstruction(Instruction last)
  {
    this.last = last;
  }

  /**
   * Return the predicate for this block.
   */
  public final int getPredicate()
  {
    if (predicates == null)
      return -1;
    
    assert (predicates.length == 1) : "Multiple predicates.";
    
    return predicates[0];
  }
  
  /**
   * Return the predicates for this block.
   */
  public final int[] getPredicates()
  {
    assert (predicates != null);
    
    return predicates;
  }

  /**
   * Remove the predicates from a block.
   */
  public final void removePredicates()
  {
    predicates = null;
  }
  
  /**
   * Set the predicate for this block.
   */
  public final void setPredicate(int predicate)
  {
    assert (predicate > -1);
    
    if (predicates == null)
      predicates = new int[1];
    
    predicates[0] = predicate;
  }
  
  /**
   * Set the predicates for this block.
   */
  public final void setPredicates(int[] predicates)
  {
    if (predicates == null)
      return;
    
    this.predicates = predicates.clone();
  }
  
  /**
   * Set the predicate for this block.
   */
  public final void setPredicate(int predicate, boolean onTrue)
  {
    assert (predicate > -1);
    
    if (predicates == null)
      predicates = new int[1];
    
    predicates[0]    = predicate;
    predicatedOnTrue = onTrue;
  }

  /**
   * Return true if this block is predicated on the predicate
   * evaluating to true.
   */
  public final boolean isPredicatedOnTrue()
  {
    return predicatedOnTrue;
  }

  /**
   * Return true if the instruction is predicated.
   */
  public final boolean isPredicated()
  {
    return predicates != null;
  }

  /**
   * Return a description of the in/out blocks.
   */
  private String getEdges(Vector<Node> wl)
  {
    if (wl.isEmpty())
      return "";

    StringBuffer buf   = new StringBuffer("");
    boolean      start = true;

    int l = wl.size();
    for (int i = 0; i < l; i++) {
      PredicateBlock block = (PredicateBlock) wl.elementAt(i);
      if (!start)
        buf.append(' ');
      buf.append('B');
      buf.append(block.getLabel());
      start = false;
    }

    return buf.toString();
  }

  /**
   * This routine will determine the block size (including estimated
   * fanout), the highest load/store queue id, and the number of
   * branches.
   */
  public final void analyze(IntMap<Vector<Integer>> targets, IntMap<Integer> uses, BitVect mov3)
  {
    blockSize      = 0;     
    fanout         = 0;     
    spillSize      = 0;     
    numSpills      = 0;     
    hasBranch      = false; 
    hasCall        = false;
    hasSwitch      = false;
    hasDummyStores = false; 
    maxLSID        = 0;
    
    for (Instruction inst = getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.isMarker())
        continue;
      
      int num = estimateNumInstructions(inst);  
      
      if (inst.isBranch()) {
        Branch br = (Branch) inst;
        if (br.isCall())
          hasCall = true;
        else {
          int bt = br.numTargets();
          if (bt > 1)
            hasSwitch = true;
        }
        hasBranch = true;
      } else if (inst.isLoad()) { // Keep track of the number of loads.
        LoadInstruction ld = (LoadInstruction) inst;
        if (ld.isSpill()) {
          spillSize += num;
          numSpills++;
          num = 0;
        }
        int id = ld.getLSQid();
        if (id > maxLSID)
          maxLSID = id;
      } else if (inst.isStore()) { // Keep track of the number of stores.
        StoreInstruction sd  = (StoreInstruction) inst;
        if (sd.isSpill()) {
          spillSize += num;
          numSpills++;
          num = 0;
        } else if (sd.isDummyStore())
          hasDummyStores = true;
        int id = sd.getLSQid();
        if (id > maxLSID)
          maxLSID = id;
      }
      
      blockSize += num;
      fanout    += estimateFanout(inst, targets, uses, mov3);
    }
  }

  /**
   * Compute the number of fanout instructions needed given the number of targets 
   * for an instruction, the number of uses, and whether or not mov3s can be used.
   */   
  private int fanout(int targets, int uses, boolean noMov3)
  {  
    int fanout = uses - targets;
    if (fanout <= 0)
      return 0;
    
    // If mov3 is set, it means we cannot use a mov3.
      
    if (noMov3)
      return fanout;
   
    // Use mov3's. The scheduler will only use mov3's for the bottom
    // of the fanout tree, so we have to compute the number of mov2's
    // needed to fanout the values to the mov3's.
      
    int mov = (uses - 1) / 3;
    if (mov < 1)
      return 1;
      
    int x1 = (targets == 2) ? 1 : 0;  // if the inst has 2 targets, we need one less mov2
    int x2 = (((uses - 1) % 3) == 0) ? 0 : 1;  // can we use the second target of a mov2, or do we need another
      
    return (mov * 2) - x1 + x2;
  }

  /**
   * This routine computes the fanout needed for the operands of an instruction.
   * It is a helper routine for Hyperblock.estimateFanout().
   */
  public final int estimateFanout(Instruction inst, IntMap<Vector<Integer>> targets, IntMap<Integer> uses, BitVect mov3)
  {
    // The operands for a reads do not increase fanout.
    
    if (inst.getOpcode() == Opcodes.READ)
      return 0;
      
    // If there are no source operands we are done.
    
    int srcs[] = inst.getSrcRegisters();
    if (srcs == null)
      return 0;
    
    int fanout = 0;

    for (int j = 0; j < srcs.length; j++) {
      int use = srcs[j];
  
      Vector<Integer> v = targets.get(use);
      if (v == null) {
        // If targets is null the def was a read (since we do the analysis
        // without reads) and can target two instructions.
        
        v = new Vector<Integer>(1);
        v.add(new Integer(2));  
      } 
      
      // Get the previous number of uses for the operand
 
      Integer iu = uses.get(use);
      int     nu = (iu == null) ? 0 : iu.intValue();   

      // Compute the new fanout for each target
        
      for (int k = 0; k < v.size(); k++) {  
        Integer iv   = v.get(k);
        int     tgts = iv.intValue();
        int     of   = fanout(tgts, nu, mov3.get(use));
        int     nf   = fanout(tgts, nu+1, mov3.get(use));
        
        fanout += nf-of;
      }
      
      // Update the number of uses for the operand

      uses.put(use, new Integer(nu+1));
    }
  
    return fanout; 
  }

  
  /**
   * Return the number of real instructions needed to represent this instruction.
   */
  public final int estimateNumInstructions(Instruction inst)
  {
    int num    = 1;
    int opcode = inst.getOpcode();
    
    if ((opcode == Opcodes.READ) ||
        (opcode == Opcodes.WRITE) ||
        (opcode == Opcodes._PHI))
      return 0;
    
    if (inst instanceof EnterInstruction) {
      EnterInstruction enter  = (EnterInstruction) inst;

      // If the enter is a constant determine how many instructions it
      // will take.

      if (opcode == Opcodes._ENTER) {
        Displacement disp = enter.getDisp();
        long         imm  = disp.getDisplacement();

        if (!Trips2Machine.isImmediate(imm)) {
          num = enter.getSize();
        }
      } else
        num = enter.getSize();
      
      // Enter's can't be predicted.  A predicated move will be inserted
      // by expandPseudos().
      
      if (inst.isPredicated())
        num++;  // For the move
    } else if (inst.isStore()) {
      StoreInstruction sd   = (StoreInstruction) inst;
      Displacement     disp = sd.getDisp();
      long             imm;

      if (disp != null) {
        assert (disp.isStack());

        imm = disp.getDisplacement();

        // Add a routine to Trips2Generator that returns the size of the 
        // offset for stack displacements. TODO 

        imm += Trips2Generator.ARG_SAVE_OFFSET;
        imm += Trips2Generator.MAX_ARG_REGS * Trips2Generator.SAVED_REG_SIZE;
      } else {
        imm = sd.getImm();
      }

      if (!Trips2Machine.isImmediate(imm)) {
        // ExpandPseudo's will split the immediate between the
        // load/store and one addi.  If the addi is out of range it
        // will become an add + enter.

        num++;   // For the add or addi.

        // Check that the remaining immediate is in range.

        long rimm = ((imm < 0) ?
                     (imm - Trips2Machine.minImmediate) :
                     (imm - Trips2Machine.maxImmediate));
        num += estimateImmediateNumInstructions(rimm); 
      }
    } else if (inst.isLoad()) {
      LoadInstruction ld   = (LoadInstruction) inst;
      Displacement    disp = ld.getDisp();
      long            imm;

      //                                      enter   $t19, 2329
      // ldspill 731 (Stk 2288)(1) L[0]  ==>  add     $t20, $t0, $t19
      //                                      ld      $t21, 255($t20) L[0]
      
      if (disp != null) {
        assert (disp.isStack());

        imm = disp.getDisplacement();

        // Add a routine to Trips2Generator that returns the size of the 
        // offset for stack displacements. TODO 

        imm += Trips2Generator.ARG_SAVE_OFFSET;
        imm += Trips2Generator.MAX_ARG_REGS * Trips2Generator.SAVED_REG_SIZE;
      } else {
        imm = ld.getImm();
      }

      if (!Trips2Machine.isImmediate(imm)) {
        // ExpandPseudo's will split the immediate between the
        // load/store and one addi.  If the addi is out of range it
        // will become an add + enter.

        num++;   // For the add or addi.

        // Check that the remaining immediate is in range.

        long rimm = ((imm < 0) ?
                     (imm - Trips2Machine.minImmediate) :
                     (imm - Trips2Machine.maxImmediate));
        num += estimateImmediateNumInstructions(rimm); 
      }
    } else if (inst instanceof ImmediateInstruction) {
      ImmediateInstruction ii   = (ImmediateInstruction) inst;
      Displacement         disp = ii.getDisp();

      // Stack displacements are not fixed until after register allocation.
      // We have to be conservative with the estimate.

      if (disp != null)
        num += Trips2Machine.enterSizes[Trips2Machine.ENTER];
      else {
        long imm = ii.getImm();
        num += estimateImmediateNumInstructions(imm);
      }
    } 

    return num;
  }

  /**
   * Returns the number of real instructions needed to represent an
   * immediate.
   */
  private int estimateImmediateNumInstructions(long imm)
  {
    if ((imm > -256) && (imm <= 255)) 
      return 0;
    
    // These assume a combination of GENU/GENS/APP instructions.
    
    if ((imm >= -32768) && (imm <= 65535))  // 16-bit (-signed...unsigned)
      return 1;
    
    if ((imm >= -2147483648) && (imm <= 4294967295L))  // 32-bit (-signed...unsigned)
      return 2;
      
    if ((imm >= -140737488355328L) && (imm <= 281474976710655L)) // 48-bit (-signed...unsigned)
      return 3;
    
    return 4;
  }

  /**
   * Return true if this is a well formed TRIPS block.  This method is
   * for use by the block splitter before register allocation.
   */
  public final boolean isLegalBlock()
  {
    if ((blockSize + fanout + spillSize) > Trips2Machine.maxBlockSize)
      return false;

    if (maxLSID >= Trips2Machine.maxLSQEntries)
      return false;

    return true;
  }

  /**
   * Return a new block starting with the instructions after the split
   * point.  After cutting the block estimates are not correct.  The
   * caller is responsible for updating the estimates.
   */
  public final PredicateBlock cut(Instruction split, Trips2Generator gen)
  {
    TripsLabel  lab   = (TripsLabel) gen.createLabel();
    TripsBranch br    = new TripsBranch(Opcodes.BRO, lab, 1);
    Instruction olast = getLastInstruction();

    gen.updateLabelIndex(lab);
    br.addTarget(lab, 0);

    // Insert a branch as the last instruction in the block.

    insertInstructionAfter(split, br);
    setLastInstruction(br);
    lab.setNext(br.getNext());
    br.setNext(null);

    // Create a new block which inherits the successors of the split
    // block.  Make sure the inherited successors are in the same
    // order as the original block so when we re-assign LSQ ID's
    // during stack frame generation we traverse the PFG in the same
    // order as when the LSQ ID's were originally assigned. See
    // Bugzilla #1379.

    PredicateBlock nxt = new PredicateBlock(lab, olast);
    int            lo  = numOutEdges();
    
    for (int i = 0; i < lo; i++) {
      PredicateBlock succ = (PredicateBlock) getOutEdge(0);
      succ.deleteInEdge(this);
      this.deleteOutEdge(succ);
      succ.addInEdge(nxt);
      nxt.addOutEdge(succ);
    }

    blocksSplitCount++;

    return nxt;
  }

  /**
   * Remove spill code from a block.
   */
  protected void removeSpillCode()
  {
    Instruction prev = null;
    for (Instruction inst = getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.getOpcode() == Opcodes._SDSPILL)
        removeInstruction(prev, inst);
      else if (inst.getOpcode() == Opcodes._LDSPILL)
        removeInstruction(prev, inst);
      else
        prev = inst;
    }
  }

  /**
   * Insert a null and a dummy store in the block.
   */
  protected void nullifyStores(BitVect           nulls,
                               SSA               ssa,
                               Trips2RegisterSet registers)
  {
    int     rp    = getPredicate();
    boolean sense = isPredicatedOnTrue();
    
    if (Hyperblock.doFastStoreNullification) {      
      // Insert one null instruction for every store inserted and predicate the store.
      // This has the shortest dependence height but may require more instructions because
      // the pred has to be fanned out to every store instead of a single null. However,
      // we don't have to fanout the null to the stores like in the case below.

      int[] iter = nulls.getSetBits();
      for (int i = 0; i < iter.length; i++) {
        int                id = iter[i];
        int                tr = registers.newTempRegister(RegisterSet.AFIREG);
        GeneralInstruction ni = new GeneralInstruction(Opcodes.NULL, tr, -1, false); 
        StoreInstruction   si = new StoreInstruction(Opcodes._DUMMYSD, 0, tr, tr, -1, false);   

        if (Hyperblock.noLoadStorePredicateField) {
          ni.setPredicate(rp, sense);
        } else {
          si.setPredicate(rp, sense);
        }
          
        si.setLSQid(id);
        appendInstruction(ni, ssa);
        appendInstruction(si, ssa);
        nullStoresInsertedCount++;
        dummyStoresCount++;
      }
    } else {
      // Create a single null that is predicated and fanned out to every store.
      // This has a higher dependence height but may result in fewer instructions
      // since the predicate only has to be sent to the null. However, if there
      // is more than one store that needs to be nullified we have to fanout the
      // null to the stores.
      
      int                tr = registers.newTempRegister(RegisterSet.AFIREG);
      GeneralInstruction ni = new GeneralInstruction(Opcodes.NULL, tr, rp, sense);

      appendInstruction(ni, ssa);
      nullStoresInsertedCount++;
      
      // Insert the dummy stores.

      int[] iter = nulls.getSetBits();
      for (int i = 0; i < iter.length; i++) {
        int                id = iter[i];
        StoreInstruction   si = new StoreInstruction(Opcodes._DUMMYSD, 0, tr, tr, -1, false); 

        si.setLSQid(id);
        appendInstruction(si, ssa);
        dummyStoresCount++;
      }
    }
  }

  /**
   * Remove any dummy stores that were inserted by store
   * nullification.  Also remove null instructions inserted for store
   * nullification. A null without an "rb" field is used to
   * distinguish store nullification from write nullification.
   */
  protected void removeDummyStores(SSA ssa)
  {
    Instruction prev = null;
    for (Instruction inst = getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.isStore() && ((StoreInstruction) inst).isDummyStore()) {
        removeInstruction(prev, inst);
        if (ssa != null) {
          ssa.removeUse(inst, ((StoreInstruction) inst).getRb());
          if (inst.isPredicated())
            ssa.removeUse(inst, inst.getPredicate(0));
        }
      } else if ((inst.getOpcode() == Opcodes.NULL) 
          && (((GeneralInstruction) inst).getRb() == -1)) {
        removeInstruction(prev, inst);
        if (ssa != null) {
          ssa.setDef(null, ((GeneralInstruction) inst).getRa());
          if (inst.isPredicated())
            ssa.removeUse(inst, inst.getPredicate(0));
        }
      } else
        prev = inst;
    }
  }
  
  /**
   * Return the <b>unique</b> node label.
   */
  public String getDisplayName()
  {
    return "PB" + getNodeID();
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer("[B-");
    buf.append(getNodeID());
    buf.append(':');
    buf.append(label);
    buf.append(" size:");
    buf.append(blockSize);
    buf.append("]");
    return buf.toString();
  }

  public String getStats()
  {
    StringBuffer buf = new StringBuffer("fanout:");
    buf.append(fanout);
    buf.append(" maxLSID:");
    buf.append(maxLSID);
    buf.append(" spills:");
    buf.append(numSpills);
    buf.append(" spillSize:");
    buf.append(spillSize);
    buf.append(hasBranch ? " hasBranch" : "");
    buf.append(hasDummyStores ? " hasDummyStore" : "");
    buf.append(isSplitPoint ? " isSplitPoint" : "");
    return buf.toString();
  }

  /**
   * Returns a description of the block.
   */
  public final String toString()
  {
    StringBuffer buf = new StringBuffer(getDisplayLabel());
    buf.append(" => PRED(");
    if (predicates == null)
      buf.append("-1");
    else {
      buf.append(predicates[0]);
      for (int i = 1; i < predicates.length; i++) {
        buf.append(" ");
        buf.append(predicates[i]);
      }
    }
    buf.append(", ");
    buf.append(predicatedOnTrue);
    buf.append(")  IN(");
    buf.append(getEdges(predecessors));
    buf.append(")  OUT(");
    buf.append(getEdges(successors));
    buf.append(")\n\t");
    buf.append(getStats());
    buf.append("\n");

    Instruction inst = first;
    while (inst != null) {
      buf.append(inst.nullified() ? ";\t nop - " : "\t");
      buf.append(inst);

      int loopNumber = inst.getLoopNumber();
      if (loopNumber > 0)
        buf.append("\t; loop " + loopNumber);

      buf.append('\n');

      inst = inst.getNext();
    }

    return buf.toString();
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTGREY;
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.ELLIPSE;
  }
  
  /**
   * Copy the block. 
   */
  public PredicateBlock copy()
  {
    PredicateBlock copy = new PredicateBlock(predicates, predicatedOnTrue);
    
    for (Instruction inst = first; inst != null; inst = inst.getNext()) {
      Instruction i = inst.copy();
      if (i.isPredicated())
        i.setPredicates(i.getPredicates()); 
      copy.appendInstruction(i);
    }
    
    copy.tag          = tag;
    copy.isSplitPoint = isSplitPoint;
    
    return copy;
  }
}
