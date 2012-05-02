package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/**
 * This class can determine if a block is a legal TRIPS block and can
 * cut a block so that it meets the TRIPS block constraints.
 * <p>
 * $Id: BlockSplitter.java,v 1.36 2007-10-04 19:57:58 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * BLOCK CUTTING HEURISTIC
 * <p>
 * The following heuristic is used to cut blocks:
 * <p>
 * Blocks are cut so that the number of instructions in the block does
 * not exceed the maximum TRIPS block size.  Block size is determined
 * by,
 * <p>
 *   total block size = # instructions + estimated fanout
 * <p>
 * The block is cut into N parts with,
 * <p>
 *   N = total block size / maximum TRIPS block size + 1
 * <p>
 * Blocks are also cut based on the number of loads and stores. The
 * first instruction in the new block, is the load/store that exceeded
 * the maximum number of loads and stores in the original block or the
 * instruction which would make the block exceed 128 instructions.
 * The original block will be cut as many times as necessary to
 * enforce these limits.
 * <p>
 * FAN OUT
 * <p>
 * We base our estimate of fanout on the number of targets an
 * instruction can have.  If an instruction uses a register and the
 * defining instruction (the instruction which defines the register)
 * has no more available targets, we need to insert fanout.  Note, we
 * don't actually insert anything into the codestream.  We just track
 * the number of instructions that we would insert.
 * <p>
 * For example,
 * <pre>
 *   addi $t1, $t2, 7     // $t1 defined
 *   sub  $t4, $t1, $t5   // $t1 used
 *   sub  $t6, $t1, $t7   // $t1 used
 * </pre>
 * Here $t1 is defined by the "addi" instruction, which can only
 * target one other instruction. There are two uses of $t1, so a "mov"
 * must be inserted to forward the value of $t1 to the second "sub".
 * The "mov" is inserted by the TRIPS scheduler.
 * <p>
 * Our algorithm (derived from the scheduler's) computes fanout as,
 * <p>
 *   fanout = total # targets - # targets for instruction
 * <p>
 * The difference between the algorithm here and the one in the
 * scheduler is that this algorithm does not use mov3 or mov4
 * instructions in the approximation.
 * <p>
 * REVERSE IF-CONVERSION
 * <p>
 * TODO
 * <p>
 * SPILL CODE
 * <p>
 * During register allocation, spill code may be inserted.  The spills
 * can cause the number of instructions in a block to exceed the
 * maximum block size.  They may also cause the number of load/store
 * queue ids to exceed their limit.  To handle this we re-run the
 * block splitter after register allocation on the set of hyperblocks
 * that contain spills.
 * <p>
 * The block cutting algorithm for spills is different than the normal
 * algorithm.  The main differences are that we do not go into SSA
 * form and that the block cutter removes all spill code from the
 * hyperblocks.  The reason we remove the spill code is that the
 * register allocator is going to re-run and it will re-insert any
 * required spill code at that time.
 * <p>
 * READ AND WRITE INSTRUCTIONS
 * <p>
 * The block cutter does not handle read and write instructions at
 * this time.  We use liveness information during the analysis phase
 * to determine what the read and write instructions would be.
 */

public class BlockSplitter
{
  private static int blocksReverseIfConvertedCount = 0; // A count of the number of blocks which were reverse if-converted.

  private static final String[] stats = { "blocksReverseIfConverted" };

  static
  {
    Statistics.register("scale.backend.trips2.BlockSplitter", stats);
  }

  /**
   * Return the number of blocks reverse if-converted.
   */
  public static int blocksReverseIfConverted()
  {
    return blocksReverseIfConvertedCount;
  }

  private Trips2Generator    gen;
  private Trips2RegisterSet  regs;
  private Vector<Hyperblock> workingSet; // The set of hyperblocks being processed.

  private final static int    WARN_SPLIT_ATTEMPTS = 25;   // Number of split attempts before the block splitter issues a warning.
  private final static double MAXFILL             = 1.0;  // Max % of a block to fill when splitting  

  /**
   * The constructor.
   */
  public BlockSplitter(Trips2Generator gen)
  {
    this.gen        = gen;
    this.regs       = (Trips2RegisterSet) gen.getRegisterSet();
    this.workingSet = new Vector<Hyperblock>();
  }

  /**
   * The main routine for block splitting.
   */
  public final void split(Hyperblock hbStart)
  {
    Stack<Node>      wl    = WorkArea.<Node>getStack("split");
    DataflowAnalysis df    = new DataflowAnalysis(hbStart, regs);
    int              trips = 0;

    // Add all the hyperblocks to the working set.
    
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);      
      workingSet.add(hb);
    }
    
    // Split the hyperblocks.
    
    while (!workingSet.isEmpty()) {
      df.computeLiveness3();
      wl.addAll(workingSet);

      while (!wl.isEmpty()) {
        Hyperblock hb = (Hyperblock) wl.pop();
        hb.enterSSA();
        hb.analyzeLeaveSSA();
        
        if (!hb.isLegalBlock(true))
          splitHyperblock(hb);
        else
          workingSet.remove(hb);
      }

      if ((++trips % WARN_SPLIT_ATTEMPTS) == 0)
        System.err.println("** Warning: the block splitter has run " + trips
            + " times for " + gen.getCurrentRoutine().getName() + "().");
    }

    WorkArea.<Node>returnStack(wl);
  }
   
  /**
   * This routine is called to split blocks during register allocation.
   * At this point we know that the block was legal before the spill 
   * stores and loads were inserted.  For every hyperblock with spills
   * that is no longer legal, we pick its largest predicate block and
   * split that in half. One half is then placed into a new hyperblock.
   * <p>
   * The case that was causing trouble results from the classic
   * diamond-shaped predicate flow graph:
   * <pre>
   *     a
   *    / \
   *   b   c
   *    \ /
   *     d
   * </pre>
   * where predicate block <code>a</code> contains most of the
   * instructions, blocks <code>b</code> and <code>c</code> each
   * contain a predicated branch, and block <code>d</code> is empty.
   * Because of spilling, the hyperblock was too big but all of the
   * predicate blocks were legal.  As a result we attempted to split
   * block <code>d</code> which did no good.
   * @param blocks is a list of hyperblocks that have had spills inserted
   * @return true if a block was split
   */
  public final boolean splitBlocksWithSpills(Hyperblock hbStart, Vector<Hyperblock> blocks)
  {
    // If all the blocks with spills are legal we are done.
    
    boolean illegal = false;
    int     bs      = blocks.size();
    for (int i = 0; i < bs; i++) {
      Hyperblock hb = blocks.get(i);
      if (!hb.isLegalBlock(true))
        illegal = true;
    }
    
    if (!illegal)
      return false;
    
    // Remove any spill code because the register allocator will run again.
    // And remove any blocks which are legal from the set to split.
  
    for (int i = bs - 1; i > -1; i--) {
      Hyperblock hb = blocks.get(i);
      if (hb.isLegalBlock(true))
        blocks.remove(i);
      hb.removeSpillCode();
      workingSet.add(hb);
    }
     
    // Split hyperblocks that have violations.
   
    int bl = blocks.size();
    for (int i = bl-1; i > -1; i--) {
      Hyperblock hb = blocks.remove(i);  
      splitHyperblock(hb);
    }

    // The register allocator and block splitter are going to run again.  
    // Analyze all the hyperblocks that were created or changed.
    
    DataflowAnalysis df = new DataflowAnalysis(hbStart, regs);
    df.computeLiveness3();
    
    int sl = workingSet.size();
    for (int i = sl-1; i > -1; i--) {
      Hyperblock hb = workingSet.remove(i);
      hb.enterSSA();
      hb.analyzeLeaveSSA();
    }
 
    return true;
  }

  /**
   * Find a split point for a block with spills.
   * <br>
   * We know the hyperblock was legal before the insertion of spill code. 
   * Return the "deepest" split point in the predicate flow graph, or if 
   * there are no split points, the block with the most instructions.
   */
  private PredicateBlock findSplitPointSpills(Hyperblock hb)
  {
    Vector<PredicateBlock> wl = new Vector<PredicateBlock>();

    PredicateBlock start       = hb.getFirstBlock();
    PredicateBlock last        = hb.getLastBlock();
    PredicateBlock biggest     = null;
    PredicateBlock splitPoint  = null;
    int            biggestSize = 0;   
    
    if (start.numOutEdges() == 0)
      return start;
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);

    // Find the block with the most "real" instructions.  
    // We do not include fanout, nulls, or spill code.  
    
    while (!wl.isEmpty()) {
      int sl = wl.size();
      for (int i = 0; i < sl; i++) {
        PredicateBlock block = wl.get(i);
        int            size  = 0;
        
        for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext())
          size++;  // TODO?  Should we use the real size of the instruction?
        
        if (size >= biggestSize) {
          if (block != last) {
            biggestSize = size;
            biggest = block;
          }
        }

        if (block.isSplitPoint() && (block != start))
          splitPoint = block;
      }
      
      wl = hb.getNextPFGLevel(wl);
    }
    
    return (splitPoint != null) ? splitPoint : biggest;
  }
  
  /**
   * Find the predicate block in a hyperblock to split.
   */
  private PredicateBlock findSplitPoint(Hyperblock hb)
  {
    Vector<PredicateBlock> wl = new Vector<PredicateBlock>();

    int            totalSize              = hb.getFanout() + hb.getBlockSize();
    int            splits                 = (totalSize / Trips2Machine.maxBlockSize) + 1;
    int            splitSize              = totalSize / splits;
    int            hbSize                 = 0; 
    PredicateBlock start                  = hb.getFirstBlock();
    PredicateBlock lastUnpredicated       = null;
    int            lastUnpredicatedHBSize = 0;
   
    assert (hb.numSpills() == 0) : "This method should not be called for blocks with spills.";
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);

    while (!wl.isEmpty()) {
      int l         = wl.size();
      int levelSize = 0;
      int levelLSID = 0;
      
      // Compute the statistics for this level of the PFG.
      
      for (int i = 0; i < l; i++) {
        PredicateBlock block     = wl.get(i);
        int            blockSize = block.getBlockSize() + block.getFanout();
        int            id        = block.maxLSID();
        
        levelSize += blockSize;
        if (id > levelLSID)
          levelLSID = id;

        // Remember the block and the hyperblock size if this block is unpredicated
        // and not the special exit block.
        // TODO - Can we remove the restriction on being the last block now?
        
        if (!block.isPredicated()) {
          if (block.numOutEdges() > 0) {
            if (lastUnpredicatedHBSize < (blockSize + hbSize)) {
              lastUnpredicatedHBSize = blockSize + hbSize;
              lastUnpredicated       = block;
            } 
          }
        }
      }
   
      // Determine if all the blocks can be added to the hyperblock.
      
      int size = hbSize + levelSize;
      if ((size > Trips2Machine.maxBlockSize) ||
          (levelLSID >= Trips2Machine.maxLSQEntries))
        break;
      
      hbSize = size;
      wl = hb.getNextPFGLevel(wl);
    }
     
    assert (!wl.isEmpty()) : "This block does not need to be split?";
    
    // If there is only one unpredicated block in the level and it is
    // not the special exit block use it.  Or if this is the only
    // block in the PFG.
    
    int l = wl.size();  
    if (l == 1) {
      PredicateBlock block = wl.get(0);
      if (!block.isPredicated()) {
        if ((start == block) || (block.numOutEdges() > 0)) {
          //System.out.println("block");
          return block;
        }
      }
    }
    
    // Is there a last known unpredicated block of adequate size use it.
      
    if (lastUnpredicated != null) {
      if (lastUnpredicatedHBSize >= splitSize) {
        //System.out.println("*** last unpred is greater than split sz " + splitSize);
        return lastUnpredicated;
      }
    }
     
    // Try to find a parent that's unpredicated unless the parent is
    // the first block.
      
    for (int i = 0; i < l; i++) {
      PredicateBlock block = wl.get(i);
      int            pl    = block.numInEdges();
      for (int j = 0; j < pl; j++) {
        PredicateBlock pred = (PredicateBlock) block.getInEdge(j);
        if (!pred.isPredicated() && pred.numInEdges() > 1) {
          //System.out.println("unpred parent not start");
          return pred;
        }
      }
    }
      
    // Reverse if-convert the largest block in this level which is not
    // an exit.  Although this seems like a good idea, there is not
    // always enough room in the hyperblock to fanout the live-outs to
    // the write instructions.  Don't do this for now. -- Aaron
    
    PredicateBlock candidate = null;
    int            largest      = 0;
//     for (int i = 0; i < l; i++) {
//       PredicateBlock block = (PredicateBlock) wl.get(i);
//       int            bsize = block.getBlockSize() + block.getFanout() + block.getSpillSize();
//       if ((bsize > largest) && (block.numBranches() == 0)) {
//         largest   = bsize;
//         candidate = block;
//       }
//     }
    
//     if (candidate != null) {
//       //System.out.println("level no exit");
//       return candidate;
//     }
    
    // Reverse if-convert a parent which is not start.  
    // Prefer parents that are split points.
    
    for (int i = 0; i < l; i++) {
      PredicateBlock block = wl.get(i);
      int            pl    = block.numInEdges();
      for (int j = 0; j < pl; j++) {
        PredicateBlock pred = (PredicateBlock) block.getInEdge(j);
        if (pred != start) {
          if (pred.isSplitPoint()) {
            //System.out.println("pred out isSplit not start");
            return pred;
          }
          candidate = pred;
        }
      }
    }
      
    if (candidate != null) {
      //System.out.println("pred out not start");  
      return candidate;
    }
    
    // Reverse if-convert the largest successor of start without an exit.
    
    largest = 0;
    for (int i = 0; i < start.numOutEdges(); i++) {
      PredicateBlock block = (PredicateBlock) start.getOutEdge(i);
      int            bsize = block.getBlockSize() + block.getFanout() + block.getSpillSize();
      if ((bsize > largest) && !block.hasBranch()) {
        largest   = bsize;
        candidate = block;
      }
    }
    
    if (candidate != null) {
      //System.out.println("start successor no exit");
      return candidate;
    }
    
    //System.out.println("1st start successor ?");
    return (PredicateBlock) start.getOutEdge(0);  
  }
  
  /**
   * Split a hyperblock.
   */
  private void splitHyperblock(Hyperblock hb)
  {
    PredicateBlock block = (hb.numSpills() > 0) ? findSplitPointSpills(hb) : findSplitPoint(hb);
    
    if (block.isPredicated()) {
      reverseIfConvert(hb, block);
      return;
    }

    int bsize = block.getBlockSize() + block.getFanout() + block.getSpillSize();
    if (bsize > Trips2Machine.maxBlockSize) {
      splitBlock(hb, block);
      return;
    }

    if (block.maxLSID() >= Trips2Machine.maxLSQEntries) {
      splitBlock(hb, block);
      return;
    }

    if (block.numInEdges() == 0) {
      splitBlock(hb, block);
      return;
    }

    reverseIfConvert(hb, block);
  }

  /**
   * Reverse if-convert a block.  Return a new hyperblock starting
   * with the reverse if-converted block or null.
   */
  private Hyperblock reverseIfConvertBlock(PredicateBlock block)
  {
    int     rp    = block.getPredicate();
    boolean sense = block.isPredicatedOnTrue();
    
    // Insert a label at the beginning of the block.

    TripsLabel lab = (TripsLabel) gen.createLabel();
    gen.updateLabelIndex(lab);
    block.insertInstructionAtHead(lab);

    // Insert a branch to this block in its predecessors.

    for (int i = block.numInEdges()-1; i > -1; i--) {
      PredicateBlock pred      = (PredicateBlock) block.getInEdge(i);
      int            rpPred    = pred.getPredicate();
      boolean        sensePred = pred.isPredicatedOnTrue();
      boolean        needThunk = true;

      // Check if we need a thunk.

      if (!block.isPredicated())
        needThunk = false;
      else if (pred.numOutEdges() == 1)
        needThunk = false;
      else if ((rp == rpPred) && (sense == sensePred))
        needThunk = false;

      // Create a branch to the label and insert it.

      if (needThunk) {
        TripsBranch    br    = new TripsBranch(Opcodes.BRO, lab, 1, rp, sense);
        PredicateBlock thunk = new PredicateBlock(rp, sense);

        br.addTarget(lab, 0);
        thunk.appendInstruction(br);
        pred.addOutEdge(thunk);
        thunk.addInEdge(pred);
      } else {
        TripsBranch br = new TripsBranch(Opcodes.BRO, lab, 1, rpPred, sensePred);

        br.addTarget(lab, 0);
        pred.appendInstruction(br);
      }

      pred.deleteOutEdge(block);
      block.deleteInEdge(pred);
    }
    
    blocksReverseIfConvertedCount++;
    
    // Create the new hyperblock.
    
    BitVect    predicates = removePredicates(block);  
    Hyperblock hnew       = new Hyperblock(block, predicates, regs);
    
    hnew.updateLastBlock();
    
    return hnew;
  }

  /**
   * Remove all the predicates which are not defined in the hyperblock.
   */
  private BitVect removePredicates(PredicateBlock start)
  {
    BitVect     predicates = new BitVect();
    Stack<Node> wl         = new Stack<Node>();

    // Determine all the predicates defined outside this hyperblock.

    start.nextVisit();
    start.setVisited();
    wl.add(start);

    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);

      for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        if (inst.isMarker())
          continue;

        if (inst.isBranch())
          continue;

        if (((TripsInstruction) inst).definesPredicate())
          predicates.set(inst.getDestRegister());
      }
    }
    
    // Remove the predicates.

    start.nextVisit();
    start.setVisited();
    wl.add(start);

    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);

      if (!block.isPredicated())
        continue;

      int rp = block.getPredicate();
      if (predicates.get(rp))
        continue;

      block.removePredicates();

      for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext())
        inst.removePredicates();
    }
    
    return predicates;
  }

  /**
   * Reverse if-convert the given predicate block from the hyperblock.
   */
  private void reverseIfConvert(Hyperblock hb, PredicateBlock start)
  {
    Stack<Node>            wl      = WorkArea.<Node>getStack("reverseIfConvert");
    Stack<PredicateBlock>  reverse = WorkArea.<PredicateBlock>getStack("reverseIfConvert");
    Vector<PredicateBlock> blocks  = new Vector<PredicateBlock>();
    Vector<Hyperblock>     hbs     = new Vector<Hyperblock>();
    
    // Find the blocks which need to be reverse if-converted.
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);

    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);

      for (int i = 0; i < block.numInEdges(); i++) {
        PredicateBlock pred = (PredicateBlock) block.getInEdge(i);
        if (!pred.visited()) {
          blocks.add(block);
          break;
        } else if (blocks.contains(pred) && block.numInEdges() > 1) {
          blocks.add(block);
          break;
        }
      }
    }
    
    // Order the blocks to reverse if-convert based on their depth from the root.

    PredicateBlock         head = hb.getFirstBlock();
    Vector<PredicateBlock> wl2  = new Vector<PredicateBlock>();

    head.nextVisit();
    head.setVisited();
    wl2.add(head);

    while (!wl2.isEmpty()) {
      int l = wl2.size();

      for (int i = 0; i < l; i++) {
        PredicateBlock block = wl2.get(i);
        if (blocks.contains(block)) {
          blocks.remove(block);
          reverse.push(block);
        }
      }

      wl2 = hb.getNextPFGLevel(wl2);
    }

    // Remove the special "dummy" last block from the PFG.

    PredicateBlock last = hb.getLastBlock();
    assert (last.numOutEdges() == 0 && !last.isPredicated());
    
    if (last.getFirstInstruction() == null) {
      for (int i = last.numInEdges()-1; i > -1; i--) {
        PredicateBlock pred = (PredicateBlock) last.getInEdge(i);
        pred.deleteOutEdge(last);
        last.deleteInEdge(pred);
      }
      reverse.remove(last);
    }
    
    // Reverse if-convert.

    while (!reverse.isEmpty()) {
      PredicateBlock block = reverse.pop();
      Hyperblock     hbn   = reverseIfConvertBlock(block);
  
      hbs.add(hbn);
      workingSet.add(hbn);
    }

    // Update the PFG.

    hb.updateLastBlock();
    hb.invalidateDomination();     // The dominators are now invalid.
    
    // Insert the new hyperblocks in the HFG.

    HashMap<Instruction, Hyperblock> entries = computeEntries(hb, hbs);
    hbs.add(hb);
    Hyperblock.computeHyperblockFlowGraph(hbs, entries);

    // Update the return block.  Since 'hbs' is an ordered list, the
    // first element in the list is the hyperblock with the return
    // because this was the original tail of the PFG which was reverse
    // if-converted.

    if (hb == gen.getReturnBlock())
      gen.setReturnBlock(hbs.firstElement());

    WorkArea.<Node>returnStack(wl);
    WorkArea.<PredicateBlock>returnStack(reverse);
  }

  /**
   * Compute the entry points for a list of hyperblocks to be inserted
   * into the HFG.  This method also removes the link between hbStart
   * and all its successors.
   */
  public static HashMap<Instruction, Hyperblock> computeEntries(Hyperblock hbStart, Vector<Hyperblock> hbs)
  {
    HashMap<Instruction, Hyperblock> entries = new HashMap<Instruction, Hyperblock>(203);

    // Find the entry points in the new hyperblocks.

    int l = hbs.size();
    for (int i = 0; i < l; i++) {
      Hyperblock     hb    = hbs.elementAt(i);
      PredicateBlock start = hb.getFirstBlock();
      Instruction    first = start.getFirstInstruction();

      entries.put(first, hb);
    }

    // The successors of the original hyperblock are also entry points.

    int sl = hbStart.numOutEdges();
    for (int i = 0; i < sl; i++) {
      Hyperblock     succ  = (Hyperblock) hbStart.getOutEdge(0);  
      PredicateBlock start = succ.getFirstBlock();
      Instruction    first = start.getFirstInstruction();
     
      hbStart.deleteOutEdge(succ);  
      succ.deleteInEdge(hbStart); 
      entries.put(first, succ);
    }

    return entries;
  }

  /**
   * Split an unpredicated predicate block.
   */
  private void splitBlock(Hyperblock hb, PredicateBlock block)
  { 
    int chunkSize = (block.getBlockSize() + block.getFanout()) / 2;    
    int maxChunk  = (int)(Trips2Machine.maxBlockSize * MAXFILL);

    if (chunkSize > maxChunk)
      chunkSize = maxChunk;
    
    Instruction    splitLocation = findSplitLocation(hb, block, chunkSize);
    PredicateBlock start         = block.cut(splitLocation, gen);  
    Hyperblock     nhb           = new Hyperblock(start, regs);
    
    // Insert the new hyperblock into the HFG.
      
    for (int i = hb.numOutEdges()-1; i > -1; i--) {
      Hyperblock out = (Hyperblock) hb.getOutEdge(i);
      out.replaceInEdge(hb, nhb);
      hb.deleteOutEdge(out);
      nhb.addOutEdge(out);
    }
      
    hb.addOutEdge(nhb);
    nhb.addInEdge(hb);
    workingSet.add(nhb);
       
    hb.invalidateDomination();    
    hb.findLastBlock();            
    hb.determinePredicatesBranches();
    nhb.findLastBlock();
    nhb.determinePredicatesBranches();
    
    // Update the return block if it has changed.
    
    if (gen.getReturnBlock() == hb)
      gen.setReturnBlock(nhb);
  }
  
  /**
   * Return the location in the block to split.  Use the last spill
   * store point found as the location to split.  If no spill store is
   * found, use the last instruction that does not cause a violation.
   * This method will throw an exception if not split location is
   * found.
   */
  private final Instruction findSplitLocation(Hyperblock hb, PredicateBlock block, int chunkSize)
  {
    int             cLSID           = 0;
    int             cSize           = 0;
    Instruction     splitLocation   = null;
    boolean         foundSpillPoint = false; 
    BitVect         mov3            = new BitVect();
    Stack<Node>     wl              = new Stack<Node>();
    IntMap<Integer> uses            = new IntMap<Integer>(37);
    IntMap<Vector<Integer>> targets = hb.computeTargets(wl, mov3);
    
    for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext()) {
      if (inst.isMarker()) 
        continue;
        
      if (inst.isLoad() || inst.isStore()) {
        int id = (inst.isLoad() ?
                  ((LoadInstruction) inst).getLSQid() :
                  ((StoreInstruction) inst).getLSQid());
        if (id > cLSID)
          cLSID = id;
      }
      
      cSize += block.estimateNumInstructions(inst);
      cSize += block.estimateFanout(inst, targets, uses, mov3);
      
      if ((cLSID >= Trips2Machine.maxLSQEntries) || (cSize >= chunkSize)) {
        if (splitLocation == null)
          splitLocation = inst;  // Possible when splitting because of spills.
        assert (splitLocation != block.getLastInstruction()) :
          "How is the last instruction the split location?";
        return splitLocation;
      }
      
      if (inst.isSpillStorePoint()) {
        splitLocation = inst;
        foundSpillPoint = true;
      }
      
      if (!foundSpillPoint)
        splitLocation = inst;
    }

    throw new scale.common.InternalError("Could not find a location to split the predicate block.");
  }
}
