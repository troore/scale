package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/**
 * Backend hyperblock formation.
 * <p>
 * $Id: HyperblockFormation.java,v 1.57 2007-10-31 16:39:16 bmaher Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class HyperblockFormation
{
  /**
   * True: perform hyperblock formation.
   */
  public static boolean enableHyperblockFormation = false;
  
  /**
   * True: print debugging information.
   */
  public static boolean debug = false;
  
  /**
   * True: print analysis of merging attempts.
   */
  public static boolean analyze = false;
  
  /**
   * True: produces dot files, used for producing a graphical HFG.
   */
  public static boolean makeDotFiles = false;
  
  /**
   * True: use profiling information (if available) to decide which
   * regions to place into a hyperblock.
   */
  public static boolean useProfile = false;

  /**
   * Threshold for excluding basic blocks from a hyperblock.
   **/
  public static double  threshold  = 0.03;
  
  /**
   * True: perform loop peeling during hyperblock formation.
   */
  public static boolean peelLoops = false;
  
  /**
   * True: perform loop unrolling during hyperblock formation.
   */
  public static boolean unrollLoops = false;
  
  /**
   * True: perform for loop unrolling during hyperblock formation.
   */
  public static boolean unrollForLoops = false;
  
  /**
   * True: perform tail duplication during hyperblock formation.
   */
  public static boolean tailDuplicate = false;
  
  /**
   * True: allow function calls to be predicated.
   */
  public static boolean includeCalls = false;
  
  /**
   * True: allow the return block to be tail duplicated.
   */
  public static boolean duplicateReturn = false;

  private Trips2Generator  gen;
  private Domination       dom;
  private Hyperblock       returnBlock;   // The hyperblock with the return instruction.
  private HashMap<Instruction, Instruction> loopHeaders;   // Maps hyperblocks to the innermost loop containing them.
  private Hyperblock       hbStart;
  private boolean          beforeRegisterAllocation;
  private DataflowAnalysis df;
  
  /**
   * The default constructor.
   */
  public HyperblockFormation(Trips2Generator gen,
                             Hyperblock      hbStart,
                             boolean         beforeRegisterAllocation)
  {
    this.gen                      = gen;
    this.returnBlock              = gen.getReturnBlock();
    this.hbStart                  = hbStart;
    this.beforeRegisterAllocation = beforeRegisterAllocation;
  }

  /**
   * The main routine.
   */
  public Hyperblock createHyperblocks()
  {
    boolean doDot = makeDotFiles &&
      ((Debug.getReportName() == null) ||
        Debug.getReportName().equals(gen.getCurrentRoutine().getName()));

    String rname = gen.getCurrentRoutine().getName();
    if (doDot)
      Hyperblock.writeDotFlowGraph(hbStart, rname + "_before.dot");
    
    df = new DataflowAnalysis(hbStart, gen.getRegisterSet());
    df.computeLiveness3();
    findLoopHeaders();
    //assert(checkLoopHeaders())
    //  : "iterative post-order traversal does not match recursive traversal";

    combineNonLoopRegions();
    combineLoopRegions();
    
    if (doDot)
      Hyperblock.writeDotFlowGraph(hbStart, rname + "_after.dot");
     
    return hbStart;
  }

  /**
   * Used to merge the prologue and epilogue during stack frame generation.
   */
  public Hyperblock mergePrologueEpilogue(Hyperblock block)
  {
    if (block.numOutEdges() == 0)
      return hbStart;
    
    df = new DataflowAnalysis(hbStart, gen.getRegisterSet());
    df.computeLiveness3();
    
    Hyperblock nxt   = (Hyperblock) block.getOutEdge(0);
    Hyperblock merge = mergeHyperblocks(block, nxt);
    
    if ((merge != null) && (hbStart == block))
      hbStart = merge;
    
    return hbStart;
  }
  
  /**
   * Merges the epilogue after stack frame generation.  Can perform tail duplication if necessary.
   */
  public Hyperblock mergeEpilogue(Hyperblock block)
  {
    if (block.numOutEdges() == 0)
      return hbStart;
    
    df = new DataflowAnalysis(hbStart, gen.getRegisterSet()); 
    df.computeLiveness3();
    
    // First merge the epilogue into the last block.
    
    Hyperblock next  = (Hyperblock) block.getOutEdge(0);
    Hyperblock merge = mergeHyperblocks(block, next);
    
    if (merge == null)
      return hbStart;
    if (hbStart == block)
      return merge;
    
    if (!duplicateReturn)
      return hbStart;
    
    // Now, merge the epilogue into its predecessors.
    
    Vector<Node> preds = merge.getInEdges();
    
    for (int i = 0; i < preds.size(); i++) {
      Hyperblock pred   = (Hyperblock) preds.get(i);
      Hyperblock merge2 = null;

      if (pred.numSpills() > 0)
        continue;  // Don't bother if there are spills.
      
      pred.enterSSA();
      pred.analyzeLeaveSSA();
      
      if (!checkReturn(pred, merge))
        continue;

      if ((merge.numInEdges() == 1) && !pred.hasCallTo(merge))
        merge2 = mergeHyperblocks(pred, merge);
      else
        merge2 = tailDuplicate(pred, merge);

      if ((merge2 != null) && (pred == hbStart))
        hbStart = merge2;
    }
    
    return hbStart;
  }
  
  /**
   * Combine all regions into hyperblocks.
   */
  private void combineNonLoopRegions()
  {
    Stack<Node> wl = WorkArea.<Node>getStack("combineNonLoopRegions");
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hbStart = mergeNonLoopBlocks(hbStart);
    hbStart.pushOutEdges(wl);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb = mergeNonLoopBlocks(hb);
      hb.pushOutEdges(wl);
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Combine all regions into hyperblocks.
   */
  private void combineLoopRegions()
  {
    Stack<Node> wl = WorkArea.<Node>getStack("combineLoopRegions");
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hbStart = mergeLoopBlocks(hbStart);
    hbStart.pushOutEdges(wl);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb = mergeLoopBlocks(hb);
      hb.pushOutEdges(wl);
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Try to merge all the successors with the hyperblock.
   */
  private Hyperblock mergeNonLoopBlocks(Hyperblock hb)
  { 
    HashSet<Object> finished = new HashSet<Object>(11);
    
    while (true) {
      Hyperblock s = selectBestNonLoop(hb, finished);
      if (s == null)
        return hb;
      
      finished.add(getLabel(s));
      Hyperblock merged = null;
      
      if (!check(hb, s))
        continue;
      
      if (loopHeaders.get(getLabel(hb)) != loopHeaders.get(getLabel(s))) {
        analyzeEdge(hb, s, "blocks are in different loops");
        continue;
      }
      
      if (s.numInEdges() == 1 && !hb.hasCallTo(s)) {
        merged = mergeHyperblocks(hb, s);
      } else if (!isLoopHeader(s)) {
        merged = tailDuplicate(hb, s);
      } else {
        analyzeEdge(hb, s, "successor is a loop header");
        continue;
      }
      
      if (merged != null) {
        hb = merged;
      } else {
        analyzeEdge(hb, s, "merging violates a block constraint");
      }
    }
  }
  
  /**
   * Try to merge all the successors with the hyperblock.
   */
  private Hyperblock mergeLoopBlocks(Hyperblock hb)
  { 
    HashSet<Object> finished = new HashSet<Object>(11);
    
    while (true) {
      Hyperblock s = selectBestLoop(hb, finished);
      if (s == null)
        return hb;
      
      finished.add(getLabel(s));
      Hyperblock merged = null;
      
      if (!check(hb, s))
        continue;
      
      if (s.numInEdges() == 1 && !hb.hasCallTo(s)) {
        merged = mergeHyperblocks(hb, s);
      } else if (hb == s) {
        merged = unrollLoop(hb);
      } else if (s.indexOfOutEdge(s) != -1) {
        merged = peelLoop(hb, s);
      } else if (!isLoopHeader(s)) {
        merged = tailDuplicate(hb, s);
      } else {
        analyzeEdge(hb, s, "successor is the header of a multi-block loop");
        continue;
      }
      
      if (merged != null) {
        hb = merged;
      } else {
        analyzeEdge(hb, s, "merging violates a block constraint");
      }
    }
  }

  /**
   * Select topologically first successor.  Allow merging blocks from
   * different loops.
   */
  private Hyperblock selectBestNonLoop(Hyperblock hb, HashSet<Object> finished)
  {
    Hyperblock best = null;
    int        bp   = -1;

    // If hb has a successor that is a loop at the next deeper level
    // of nesting, then stop merging for now.  We want to keep the
    // opportunity to peel in the next pass.
    
    for (int i = 0; i < hb.numOutEdges(); i++) {
      Hyperblock s = (Hyperblock) hb.getOutEdge(i);
      if (isLoopHeader(s)) {
        int tb = hb.getTag();
        int ts = s.getTag();
        if (tb > ts) {
          // Use topo ordering to determine nesting.
          return null;
        }
      }
    }

    // Now select according to a topological sort.
    
    for (int i = 0; i < hb.numOutEdges(); i++) {
      Hyperblock s = (Hyperblock) hb.getOutEdge(i);
      if (finished.contains(getLabel(s)))
        continue;
      if (useProfile) {
        if (hb.getBranchProbability(s) < threshold)
          continue;
      }
      int p = s.getTag();
      if (p > bp) {
        bp = p;
        best = s;
      }
    }
    
    return best;
  }

  /**
   * Select topologically first successor.  Allow merging blocks from
   * different loops.
   */
  private Hyperblock selectBestLoop(Hyperblock hb, HashSet<Object> finished)
  {
    Hyperblock best = null;
    int        bp   = -1;
    
    // Now select according to a topological sort.
    
    for (int i = 0; i < hb.numOutEdges(); i++) {
      Hyperblock s = (Hyperblock) hb.getOutEdge(i);
      if (finished.contains(getLabel(s)))
        continue;
      if (useProfile) {
        if (hb.getBranchProbability(s) < threshold)
          continue;
      }
      int p = s.getTag();
      if (p > bp) {
        bp = p;
        best = s;
      }
    }
    
    return best;
  }

  /**
   * Return true if the block is a loop header.
   */
  private boolean isLoopHeader(Hyperblock hb)
  {
    return getLabel(hb) == loopHeaders.get(getLabel(hb));
  }
  
  /**
   * Return true if the successor hyperblock can be added to the predecessor.
   */
  private boolean check(Hyperblock pred, Hyperblock succ)
  {
    debug(pred, succ, "check");
    
    if (includeCalls) {
      if (pred.hasCall() && !pred.hasBranchTo(succ)) {  // Cannot return into the middle of this hyperblock.
        analyzeEdge(pred, succ, "predecessor has call");
        return false;
      }
    } else {
      if (pred.hasCall()) {
        return false;
      }
    }
    
    if (pred.hasSwitch()) { 
      analyzeEdge(pred, succ, "predecessor has switch");
      return false;
    }
    
    // Don't tail duplicate the return block because the stack frame
    // has not be generated yet.  We don't want to generate the
    // epilogue in multiple blocks. Also don't allow the return block
    // to be predicated.
    
    if ((succ == returnBlock) &&
        ((succ.numInEdges() > 1 || pred.hasCallTo(succ)) || (pred.numOutEdges() > 1))) {
      analyzeEdge(pred, succ, "successor is the return block and has >1 in-edges");
      return false;
    }
    
    if (!includeCalls) {
      if (succ.hasCall()) {
        analyzeEdge(pred, succ, "successor has call");
        return false;
      }
    }
    
    if (succ.indexOfOutEdge(succ) != -1) {
      if (succ.hasCall()) {
        analyzeEdge(pred, succ, "single-block loop has call");
        return false;
      }
      if (succ.hasSwitch()) {
        analyzeEdge(pred, succ, "single-block loop has switch");
        return false;
      }
    }
    
    // If the total number of combined real instructions is obviously
    // too large then don't try to combine the blocks.
    
    if ((pred.getBlockSize() + succ.getBlockSize()) > Trips2Machine.maxBlockSize + 20)
      return false; 
    
    return true;
  }
  
  /**
   * Return true if the successor hyperblock can be added to the predecessor.
   */
  private boolean checkReturn(Hyperblock pred, Hyperblock succ)
  {
    debug(pred, succ, "check");
    
    if (includeCalls) {
      if (pred.hasCall() && !pred.hasBranchTo(succ)) {  // Cannot return into the middle of this hyperblock.
        analyzeEdge(pred, succ, "predecessor has call");
        return false;
      }
    } else {
      if (pred.hasCall()) {
        return false;
      }
    }
    
    if (pred.hasSwitch()) {  
      analyzeEdge(pred, succ, "predecessor has switch");
      return false;
    }
    
    // If the predecessor has spills, don't merge.  
    // It probably wouldn't have worked out anyway.
    
    if (pred.numSpills() > 0) {
      return false;
    }

    if (!includeCalls) {
      if (succ.hasCall()) {
        analyzeEdge(pred, succ, "successor has call");
        return false;
      }
    }
    
    if (succ.indexOfOutEdge(succ) != -1) {
      if (succ.hasCall()) {
        analyzeEdge(pred, succ, "single-block loop has call");
        return false;
      }
      if (succ.hasSwitch()) {
        analyzeEdge(pred, succ, "single-block loop has switch");
        return false;
      }
    }
    
    // If the total number of combined real instructions is obviously
    // too large then don't try to combine the blocks.
    
    if (pred.getBlockSize() + succ.getBlockSize() > Trips2Machine.maxBlockSize + 20)
      return false; 
    
    return true;
  }
  
  /**
   * Add the successor hyperblock to the predecessor.
   */
  private Hyperblock mergeHyperblocks(Hyperblock pred, Hyperblock succ)
  {
    debug(pred, succ, "merge");
    
    // Copy the blocks to scratch space for merging.
    
    Hyperblock pcopy = combineHyperblocks(pred, succ);
    if (pcopy == null)
      return null;
    
    // Update profiling information.
    
    if (useProfile)
      updateProfile(pred, succ, pcopy);
    
    // Swap the new block into the HFG.

    PredicateBlock ps   = pcopy.getFirstBlock();
    Instruction    olab = pred.getFirstBlock().getFirstInstruction();
    
    ps.removeInstruction(null, ps.getFirstInstruction());
    ps.insertInstructionAtHead(olab); 
    removeHyperblock(pred, succ);

    // In-edges of pred now point to pcopy.
    
    for (int i = 0; i < pred.numInEdges(); i++) {
      Hyperblock hb = (Hyperblock) pred.getInEdge(i);
      hb.replaceOutEdge(pred, pcopy);
      pcopy.addInEdge(hb);
    }
    
    // Out-edges of pred now point to pcopy.
    
    for (int i = 0; i < pred.numOutEdges(); i++) {
      Hyperblock hb = (Hyperblock) pred.getOutEdge(i);
      hb.replaceInEdge(pred, pcopy);
      pcopy.addOutEdge(hb);
    }
    
    // Update the return block.
    
    if (succ == returnBlock) {
      returnBlock = pcopy;
      gen.setReturnBlock(pcopy);
    }
    
    return pcopy;
  }
  
  /**
   * Tail duplicate a hyperblock and insert it into the HFG.
   */
  private Hyperblock tailDuplicate(Hyperblock pred, Hyperblock succ)
  {
    if (!tailDuplicate)
      return null;
    
    debug(pred, succ, "tail duplicate");
    
    Hyperblock pcopy = combineHyperblocks(pred, succ);
    if (pcopy == null)
      return null;
    
    if (useProfile)
      updateProfile(pred, succ, pcopy);
    
    Instruction    olab = pred.getFirstBlock().getFirstInstruction();
    PredicateBlock ps   = pcopy.getFirstBlock();
    ps.removeInstruction(null, ps.getFirstInstruction());
    ps.insertInstructionAtHead(olab);

    // Change predecessors of pred to go before pcopy.
    
    for (int i = 0; i < pred.numInEdges(); i++) {
      Hyperblock hb = (Hyperblock) pred.getInEdge(i);
      hb.replaceOutEdge(pred, pcopy);
      pcopy.addInEdge(hb);
    }
    
    // Change successors of pred to follow pcopy, except for succ.
    
    for (int i = 0; i < pred.numOutEdges(); i++) {
      Hyperblock hb = (Hyperblock) pred.getOutEdge(i);
      hb.replaceInEdge(pred, pcopy);
      pcopy.addOutEdge(hb);
    }

    if (!pred.hasCallTo(succ)) {
      pcopy.deleteOutEdge(succ);  
      succ.deleteInEdge(pcopy); 
    }
    
    // Add pcopy to predecessors of succ's successors.

    for (int i = 0; i < succ.numOutEdges(); i++) {
      Hyperblock hb = (Hyperblock) succ.getOutEdge(i);
      hb.addInEdge(pcopy);
      pcopy.addOutEdge(hb);
    }

    return pcopy;
  }
  
  /**
   * Unroll a loop to fill the block.
   */
  private Hyperblock unrollLoop(Hyperblock hb)
  {
    if (!unrollLoops)
      return null;
    
    debug(hb, null, "unrolling");
    
    // Ignore empty infinite loops.
    
    if (hb.getBlockSize() < 2)
      return null;
    
    // Unroll the loop.
    
    Hyperblock body     = hb.copy();
    Hyperblock unrolled = hb;
    
    while (true) {
      Hyperblock lcopy = combineHyperblocks(unrolled, body);
      if (lcopy == null)
        break;
      unrolled = lcopy;
      if (useProfile)
        updateProfile(unrolled, hb, unrolled);
    }
    
    if (unrolled == hb)
      return null;
    
    Instruction    olab  = hb.getFirstBlock().getFirstInstruction();
    PredicateBlock start = unrolled.getFirstBlock();
    
    start.removeInstruction(null, start.getFirstInstruction());
    start.insertInstructionAtHead(olab);
    
    // In-edges of old loop point to new loop.
    
    for (int i = 0; i < hb.numInEdges(); i++) {
      Hyperblock in = (Hyperblock) hb.getInEdge(i);
      in.replaceOutEdge(hb, unrolled);
      unrolled.addInEdge(in);
    }
    
    // Out-edges of old loop point to new loop.
    
    for (int i = 0; i < hb.numOutEdges(); i++) {
      Hyperblock out = (Hyperblock) hb.getOutEdge(i);
      out.replaceInEdge(hb, unrolled);
      unrolled.addOutEdge(out);
    }
    
    return unrolled;
  }
  
  /**
   * Peel as many iterations out of the successor as will fit in the
   * predecessor.
   */
  private Hyperblock peelLoop(Hyperblock pred, Hyperblock succ)
  {
    if (!peelLoops)
      return null;
    
    debug(pred, succ, "peeling");
    
    // Ignore empty infinite loops.
    
    if (succ.getBlockSize() < 2)
      return null;
    
    // Merge in a loop iteration.
    
    Hyperblock peeled = pred;
    while (true) {
      Hyperblock pcopy = combineHyperblocks(peeled, succ);
      if (pcopy == null)
        break;
      peeled = pcopy;
      if (useProfile)
        updateProfile(peeled, succ, peeled);
    }
    
    if (peeled == pred)
      return null;
    
    Instruction    olab  = pred.getFirstBlock().getFirstInstruction();
    PredicateBlock start = peeled.getFirstBlock();
    
    start.removeInstruction(null, start.getFirstInstruction());
    start.insertInstructionAtHead(olab);
    
    // Predecessors of pred now point to pcopy.
    
    for (int i = 0; i < pred.numInEdges(); i++) {
      Hyperblock hb = (Hyperblock) pred.getInEdge(i);
      hb.replaceOutEdge(pred, peeled);
      peeled.addInEdge(hb);
    }
    
    // Successors of pred now point to pcopy.
    
    for (int i = 0; i < pred.numOutEdges(); i++) {
      Hyperblock hb = (Hyperblock) pred.getOutEdge(i);
      hb.replaceInEdge(pred, peeled);
      peeled.addOutEdge(hb);
    }
    
    // pcopy leads to successors of succ.
    
    for (int i = 0; i < succ.numOutEdges(); i++) {
      Hyperblock hb = (Hyperblock) succ.getOutEdge(i);
      hb.addInEdge(peeled);  
      peeled.addOutEdge(hb);
    }

    return peeled;
  }

  /**
   * Move the "target" block so it is a successor of all the predicate
   * blocks in "exits".
   */
  private void moveRegionSafe(Hyperblock     hb,
                              PredicateBlock target, 
                              Vector<PredicateBlock> exits,
                              int            totalExits)
  {
    assert (!target.isPredicated());
        
    removeLabel(target);      // Remove the label that starts the target block.
    target.setSplitPoint();   // Mark this is a split point.
    
    // Determine if the target block needs to be predicated if it is
    // added to the hyperblock.
    
    if (totalExits > exits.size()) { // Not all exits go to the target block so it needs a predicate.
      int rp = gen.getRegisterSet().newTempRegister(RegisterSet.AIREG);
      
      hb.setPredicate(rp);
      
      // Add a predicate-or to each exit so the target block will
      // execute when it fires.
    
      int el = exits.size();
      for (int i = 0; i < el; i++) {
        PredicateBlock exit = exits.get(i);
        removeBranchInstruction(exit);
        
        Instruction por = new ImmediateInstruction(Opcodes.MOVI, rp, 1);
        por.setPredicate(exit.getPredicate(), exit.isPredicatedOnTrue());
        ((TripsInstruction) por).setDefinesPredicate();
        exit.appendInstruction(por);
      }
    
      // Predicate all unpredicated instructions in the target on the
      // predicate-or.
      
      Stack<Node> wl = new Stack<Node>();
      
      target.nextVisit();
      target.setVisited();
      wl.add(target);
      
      while (!wl.isEmpty()) {
        PredicateBlock block = (PredicateBlock) wl.pop();
        block.pushOutEdges(wl);
        
        if (!block.isPredicated()) {
          block.setPredicate(rp, true);  
          for (Instruction inst = block.getFirstInstruction();
               inst != null;
               inst = inst.getNext())
            inst.setPredicate(rp, true);
        }
      }
    } else { 
      // The target block is unconditionally executed.
      // After we remove the branch instructions the predicate blocks
      // might become dead. Dead predicate block removal will run and 
      // during the optimization phase and clean up the PFG.

      int el = exits.size();
      for (int i = 0; i < el; i++) {
        PredicateBlock exit = exits.get(i);
        removeBranchInstruction(exit);
      }
    }
    
    // Make the target block a successor of all the exits.
 
    int bl = exits.size();
    for (int i = 0; i < bl; i++) {
      PredicateBlock block = exits.get(i);
      block.addOutEdge(target);
      target.addInEdge(block);
    }
  }
  
  /**
   * Move the "target" block so it is a successor of all the predicate
   * blocks in "exits".  This method duplicates the instructions in
   * the target block and adds them to each exit block.  It is not
   * safe to call this method if the target block has multiple
   * out-going edges and there is more than one exit because we must
   * guarantee that if a predicate is defined by a test instruction,
   * there is only one such test instruction in the entire hyperblock.
   */
  private void moveRegionByCodeDuplication(PredicateBlock target, Vector<PredicateBlock> exits)
  {   
    int el = exits.size();
    for (int i = 0; i < el; i++) {
      PredicateBlock exit = exits.get(i);     
      copyInstructions(target, exit);
      removeBranchInstruction(exit);
    }
    
    if (target.numOutEdges() == 0)
      return;
    
    assert (exits.size() == 1) : "Cannot duplicate a target with multiple out-edges.";
    
    // Make the successors of "target", successors of the exit.
    
    PredicateBlock exit = exits.get(0);
    int            ol   = target.numOutEdges();
    for (int i = 0; i < ol; i++) {
      PredicateBlock out = (PredicateBlock) target.getOutEdge(i);
      exit.addOutEdge(out);
      out.addInEdge(exit);
    }
    
    exit.setSplitPoint();  // The exit is now a good split point.
    target.unlink();
    
    // Any predicate block, that was just added to the hyperblock and
    // is not predicated, must be predicated on the same predicate
    // as the original exit block.
    
    if (!exit.isPredicated())
      return;
    
    int         rp     = exit.getPredicate();
    boolean     onTrue = exit.isPredicatedOnTrue();
    Stack<Node> wl     = new Stack<Node>();
    
    exit.nextVisit();
    exit.setVisited();
    wl.add(exit);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      if (block.isPredicated()) 
        continue;
      
      block.setPredicate(rp, onTrue);
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext())
        inst.setPredicate(rp, onTrue);
    }     
  }
  
  /**
   * Return all the predicate blocks in the hyperblock with a branch
   * to the target predicate block.
   */
  private Vector<PredicateBlock> getExitBlocks(Hyperblock hb, PredicateBlock target)
  {
    Stack<Node>            wl     = new Stack<Node>();
    PredicateBlock         start  = hb.getFirstBlock();
    Label                  lab    = (Label) target.getFirstInstruction(); 
    Vector<PredicateBlock> blocks = new Vector<PredicateBlock>();
     
    start.nextVisit();
    start.setVisited();
    wl.add(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.isBranch()) {
          TripsBranch br = (TripsBranch) inst;
          if (br.numTargets() == 1 && !br.isCall()) {
            if (br.getTarget().getLabelIndex() == lab.getLabelIndex())
              blocks.add(block);
          }
          break;
        }
      }
    }

    return blocks;  
  }
  
  /**
   * Return true if n1 dominates n2.
   */
  private boolean dominates(Domination dom, Hyperblock n1, Hyperblock n2)
  {
    for (Node d = n2; d != null; ) {
      if (d == n1)
        return true;
      d = dom.getDominatorOf(d);
    }
    return false;
  }
  
  /**
   * Return the label of a given hyperblock.
   */
  private Marker getLabel(Hyperblock hb)  
  {
    return (Marker) hb.getFirstBlock().getFirstInstruction();
  }
  
  /**
   * Find the loop body associated with header hb.
   */
  private void findLoop(Hyperblock hb)
  {
    HashSet<Object>   done = WorkArea.<Object>getSet("findLoop");
    Stack<Hyperblock> wl   = WorkArea.<Hyperblock>getStack("findLoop");
    
    // Find the back edge.
    
    int il = hb.numInEdges();
    for (int i = 0; i < il; i++) {
      Hyperblock pred = (Hyperblock) hb.getInEdge(i);
      if (dominates(dom, hb, pred)) {
        loopHeaders.put(getLabel(hb), getLabel(hb));
        wl.push(pred);
      }
    }
    
    // Search upwards until reaching the header.
    
    while (!wl.empty()) {
      Hyperblock lb = wl.pop();
      if (lb == hb)
        continue;
      if (done.contains(lb))
        continue;
      
      done.add(lb);
      if (!loopHeaders.containsKey(getLabel(lb)))
        loopHeaders.put(getLabel(lb), getLabel(hb));
      
      for (int i = 0; i < lb.numInEdges(); i++)
        wl.push((Hyperblock) lb.getInEdge(i));
    }
    
    WorkArea.<Hyperblock>returnStack(wl);
    WorkArea.<Object>returnSet(done);
  }
  
  /**
   * Find the loop headers using a post-order traversal of the HFG.
   */
  private void findLoopHeaders()
  {
  	Stack<Hyperblock> wl        = WorkArea.<Hyperblock>getStack("findLoopHeaders");
  	int               postOrder = 0;
  	
  	loopHeaders = new HashMap<Instruction, Instruction>(11);
  	dom         = new Domination(false, hbStart);
  	
  	hbStart.nextVisit();
  	hbStart.setVisited();
  	wl.push(hbStart);
  	
  	while (!wl.isEmpty()) {
  		Hyperblock hb   = wl.peek();
  		boolean    done = true;
  		int        ol   = hb.numOutEdges();
  		for (int i = 0; i < ol; i++) {
  			Hyperblock s = (Hyperblock) hb.getOutEdge(i);
  			if (!s.visited()) {
  				s.setVisited();
  				wl.push(s);
  				done = false;
  				break;
  			}
  		}
  		if (done) {
  			hb.setTag(postOrder++);
  			findLoop(hb);
  			wl.pop();
  		}
  	}
  }
  
  /**
   * Do a recursive post-order traversal of the HFG to check the correctness
   * of the faster, iterative post-order traversal.
   */
  private int recursiveCheckLoopHeaders(Hyperblock hb, int postOrder)
  {
    int ol = hb.numOutEdges();
    for (int i = 0; i < ol; i++) {
      Hyperblock s = (Hyperblock) hb.getOutEdge(i);
      if (!s.visited()) {
        s.setVisited();
        postOrder = recursiveCheckLoopHeaders(s, postOrder);
        if (s.getTag() != postOrder) {
        	return -1;
        }
        postOrder++;
      }
    }
    return postOrder;
  }
 
  /**
   * Verify that the iterative post-order traversal is correct.
   */
  private boolean checkLoopHeaders()
  {
    loopHeaders = new HashMap<Instruction, Instruction>(11);
    dom         = new Domination(false, hbStart);
    hbStart.nextVisit();
    int postOrder = recursiveCheckLoopHeaders(hbStart, 0);
    boolean result = (hbStart.getTag() == postOrder);
    return result;
  }
  
  /**
   * Remove the successor hyperblock from the HFG.
   */
  private void removeHyperblock(Hyperblock pred, Hyperblock succ)
  { 
    // All the out-edges of the "succ" hyperblock 
    // are now in-edges of the "pred" hyperblock.
    
    int sl = succ.numOutEdges();
    for (int i = 0; i < sl; i++) {
      Hyperblock hb = (Hyperblock) succ.getOutEdge(i);
      hb.replaceInEdge(succ, pred);
      pred.addOutEdge(hb);
    } 
    
    succ.unlink();
  }
  
  /**
   * Update the last block for the new hyperblock.
   */
  private void updateLastBlock(Hyperblock hb)
  { 
    Stack<Node>    wl    = new Stack<Node>();
    PredicateBlock start = hb.getFirstBlock();
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      
      if (block.numOutEdges() > 0) 
        block.pushOutEdges(wl);
      else if (block.getFirstInstruction() == null)
        block.unlink();
    }
      
    hb.updateLastBlock();
  }
  
  /**
   * Copy the instructions from predicate block "b1" to predicate
   * block "b2" skipping any labels.
   */
  private void copyInstructions(PredicateBlock b1, PredicateBlock b2)
  {   
    int     rp     = b2.getPredicate();
    boolean onTrue = b2.isPredicatedOnTrue();
    
    for (Instruction inst = b1.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.isLabel())
        continue;
      
      assert (!inst.isPredicated()) : "Predicated copy not implemented.";
      
      if (!inst.isPredicated()) {
        Instruction copy = (Instruction) inst.clone();
        if (b2.isPredicated())
          copy.setPredicate(rp, onTrue);
        b2.appendInstruction(copy);
      }
    }
  }
  
  /**
   * Remove the branch instruction from a block.
   */
  private void removeBranchInstruction(PredicateBlock block)
  {
    Instruction prev = null;
    for (Instruction inst = block.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.isBranch()) {
        block.removeInstruction(prev, inst);
        break;
      }
      prev = inst;
    }
  }
  
  /**
   * Remove the label from a block.
   */
  private void removeLabel(PredicateBlock block)
  {
    Instruction prev = null;
    for (Instruction inst = block.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.isLabel()) {
        block.removeInstruction(prev, inst);
        break;
      }
      prev = inst;
    }
  }
  
  /**
   * Combines two hyperblocks.
   */
  private Hyperblock combineHyperblocks(Hyperblock pred, Hyperblock succ)
  {
    Hyperblock pcopy = pred.copy();
    Hyperblock scopy = succ.copy();
    
    // Rename the predicates so they are unique.
    
    scopy.enterSSA();
    scopy.leaveSSA(true);
    
    // If the successor hyperblock is simple enough, we will use code
    // duplication to merge the blocks.  Otherwise we will create a
    // predicate-or and move all the predicate blocks from the
    // successor to the predecessor.
    
    PredicateBlock target = scopy.getFirstBlock();
    Vector<PredicateBlock>         exits  = getExitBlocks(pcopy, target);
    
    assert (exits.size() > 0) : "No exits to specified block";
    
    if (exits.size() == 1) 
      moveRegionByCodeDuplication(target, exits);
    else
      moveRegionSafe(pcopy, target, exits, pred.numBranches());
    
    // Update the new hyperblock.
    
    updateLastBlock(pcopy);
    updateLive(pcopy, scopy);
    pcopy.invalidateDomination();
    pcopy.getPredicates().or(scopy.getPredicates());

    // Apply optimizations to reduce the block size.
    pcopy.optimize(true, df);

    if (pcopy.isLegalBlock(beforeRegisterAllocation)) {
      return pcopy;
    }

    return null;  
  }
  
  /**
   * Update the live-in's/out's.  This routine does not preserve the
   * live-in/out of the successor hyperblock.
   */
  private void updateLive(Hyperblock pred, Hyperblock succ)
  {
    // Add the live-out's of the successor to the predecessor.
    
    BitVect sout = succ.getLiveOut();
    BitVect pout = pred.getLiveOut();
    
    pout.or(sout);
    
    // Add the live-in's of the successor to the predecessor.
    
    BitVect sin = succ.getLiveIn();
    BitVect pin = pred.getLiveIn();
    
    pin.or(sin);
  }
  
  /**
   * Sets the branch probabilities of copy created by merging pred and
   * succ.
   */
  private void updateProfile(Hyperblock pred, Hyperblock succ, Hyperblock copy)
  {
    int    nouts = succ.numOutEdges();
    double psucc = pred.getBranchProbability(succ);
    
    for (int i = 0; i < nouts; i++) {
      Hyperblock hb = (Hyperblock) succ.getOutEdge(i);
      double     pr = 0.0;
      
      if (succ != hb)
        pr = pred.getBranchProbability(hb);
      copy.setBranchProbability(hb, pr + psucc * succ.getBranchProbability(hb));
    }
  }
  
  /**
   * Output debugging information.
   */
  private void debug(Hyperblock hb1, Hyperblock hb2, String phase)
  {
    if (!debug)
      return;

    System.out.print("*** " + phase + "\t");
    if (hb2 == null)
      System.out.println(hb1.getBlockName());
    else
      System.out.println(hb1.getBlockName() + "\t" + hb2.getBlockName());     
  }
  
  private void analyzeEdge(Hyperblock hb1, Hyperblock hb2, String analysis)
  {
    if (!analyze)
      return;

    String hb1name = hb1.getBlockName();
    String hb2name = hb2.getBlockName();
    int    hb1size = hb1.getBlockSize() + hb1.getFanout() + hb1.getSpillSize();
    int    hb2size = hb2.getBlockSize() + hb2.getFanout() + hb2.getSpillSize();
    
    System.out.print(hb1name);
    System.out.print(",");
    System.out.print(hb1size);
    System.out.print(",");
    System.out.print(hb2name);
    System.out.print(",");
    System.out.print(hb2size);
    System.out.print(",");
    System.out.println(analysis);
  }
}
