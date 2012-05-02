package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;
import java.util.Enumeration;

/**
 * This class represents a hyperblock which represents a predicate flow graph.
 * <p>
 * $Id: Hyperblock.java,v 1.117 2007-10-31 16:39:16 bmaher Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A hyperblock is a directed graph of "predicate" blocks called a
 * predicate flow graph.
 */
public class Hyperblock extends Node
{
  private static int predicatesCombinedCount = 0; // The number of predicates combined.
  private static int nullWritesInsertedCount = 0; // The number of nulls inserted for write nullification.
  private static int loadsRemovedCount       = 0; // The number of loads which were removed.
  private static int instructionsMergedCount = 0; // The number of instructions that were merged.
  private static int valueNumberCount        = 0; // The number of instructions value numbering removed.
  
  private static final String[] stats = {
    "nullWritesInserted", "predicatesCombined", "loadsRemoved",
    "instructionsMerged", "valueNumberCount" };

  static
  {
    Statistics.register("scale.backend.trips2.Hyperblock", stats);
  }

  /**
   * Return the number of nulls inserted.
   */
  public static int nullWritesInserted()
  {
    return nullWritesInsertedCount;
  }

  /**
   * Return the number of predicates combined.
   */
  public static int predicatesCombined()
  {
    return predicatesCombinedCount;
  }
  
  /**
   * Return the number of loads which were removed.
   */
  public static int loadsRemoved()
  {
    return loadsRemovedCount;
  }
  
  /**
   * Return the number of instructions merged.
   */
  public static int instructionsMerged()
  {
    return instructionsMergedCount;
  }
  
  /**
   * Return the number of instructions value numbering removed.
   */
  public static int valueNumberCount()
  {
    return valueNumberCount;
  }

  /**
   * If inter-block predicate minimization should be performed.
   */
  public static boolean enableInterBlockPredicateMinimization = false;
  /**
   * If intra-block predicate minimization should be performed.
   */
  public static boolean enableIntraBlockPredicateMinimization = true;
  /**
   * If redundant loads should be removed.
   */
  public static boolean enableRedundantLoadRemoval = true;
  /**
   * There are two ways to nullify store instructions:
   * <br>
   * (1) null t100                 (2) null_t<p1> t100
   *     sd_t<p1> t100, t100 [1]       mov t101, t100
   *     null t101                     mov t102, t100
   *     sd_t<p1> t101, t101 [2]       sd_t t101, t101 [1]
   *                                   sd_t t102, t102 [2]
   * <br>         
   * If this is set to true we do (1) which inserts a null for every store.
   * This is "fast" because it has the shortest dependence height. It may have 
   * more fanout because the predicate has to be sent to every store but the
   * store can issue as soon as the predicate resolves.
   * <br>
   * If this is set to false we do (2) which uses a single null for multiple
   * stores. This is "slow" because it has the longest dependence height
   * and requires fanout when there is more than one store to be nullified.
   */
  public static boolean doFastStoreNullification = false;
  /**
   * If true, loads are given a unique LSID and stores are given the minimal
   * number of LSIDs. This allows all loads to be unpredicated and requires
   * the fewest instructions for store nullification. Otherwise, all memory
   * instructions are given unique ids.
   */
  public static boolean useMinMaxLoadStoreAssignment = true;
  /**
   * To unpredicate a load it cannot share a LSID with another load. 
   * Therefore, loads will only be unpredicated when using an LSID 
   * assignment policy that maximizes the identifiers assigned to loads.
   */
  public static boolean unpredicateLoads = true;
  /**
   * True if load and store instructions do not have a predicate field.
   * This should always be false when compiling to the TRIPS ISA.
   */
  public static boolean noLoadStorePredicateField = false;
  /**
   * Predicate only the top of dependence chains.
   */
  public static final int PREDICATE_TOP    = 0x1;
  /**
   * Predicate only the bottom of dependence chains.
   */
  public static final int PREDICATE_BOTTOM = 0x2;
  /**
   * The default intra-block predicate minimization.
   */
  public static int intraBlockDefault = PREDICATE_BOTTOM;

  private static final Integer INT1 = new Integer(1);
  private static final Integer INT2 = new Integer(2);
  private static final Integer INT3 = new Integer(3);
  private static final Integer INT4 = new Integer(4);
  
  private static int nextColor = 0;              // The next available unique color (for graph traversals).
  private static int nextLabel = 0;              // The next available unique label.

  private PredicateBlock    head;                // The first block in the predicate flow graph.
  private PredicateBlock    tail;                // The last block in the predicate flow graph.
  private DominanceFrontier domf;                // The dominance frontier for the predicate flow graph.
  private Domination        dom;                 // The dominators for the predicate flow graph.
  private Trips2RegisterSet regs;                // The register set to use.
  private SSA               ssa;                 // If the hyperblock is in SSA form this is non-null.
  private int               blockSize;           // The size of the hyperblock (not including fanout and spills).
  private int               fanout;              // The estimated fanout for the hyperblock.
  private int               spillSize;           // The size of spill code in the hyperblock.
  private int               numSpills;           // The number of load/stores in the hyperblock which are spills.
  private int               numBranches;         // The number of branches in the hyperblock.
  private int               numReads;            // The number of reads in the hyperblock.
  private int               numWrites;           // The number of writes in the hyperblock.
  private int[]             readBanks;           // The register banks assigned to reads in the hyperblock.
  private int[]             writeBanks;          // The register banks assigned to writes in the hyperblock.
  private int               maxLSID;             // The highest load/store ID assigned in this hyperblock.
  private boolean           hasDummyStores;      // True if the hyperblock contains a dummy store for store nullification.
  private boolean           hasCall;             // True if the hyperblock has a function call.
  private boolean           hasSwitch;           // True if the hyperblock has a switch statement.
  private int               loopNumber;          // Which loop this HB is in, -1 if none
  private BitVect           livein;              // The registers live-in to the hyperblock.
  private BitVect           liveout;             // The registers live-out of the hyperblock.
  private BitVect           predicates;          // The predicates used in the hyperblock.
  
  private HashMap<Instruction, Double> probForBlock;        // The branch probability for an out edge.
  
  private IntMap<PredicateBlock> blocksFalse;    // For creating the PFG.
  private IntMap<PredicateBlock> blocksTrue;     // For creating the PFG.
  
  /**
   * Construct a new hyperblock starting with the given instruction.
   * <p>
   * @param first the first instruction in the hyperblock.
   * @param registers the RegisterSet to use.
   */
  public Hyperblock(Instruction first, Trips2RegisterSet registers)
  {
    this(registers);
    
    determinePredicatesBranches(first); 
    createPredicateFlowGraph(first);
  }

  /**
   * Construct a new hyperblock from a PredicateBlock.
   * <p>
   * @param start the block which starts the hyperblock.
   * @param registers the RegisterSet to use.
   */
  public Hyperblock(PredicateBlock start, Trips2RegisterSet registers)
  {
    this(registers);
    
    this.predicates = new BitVect();
    this.head       = start;
  }
  
  /**
   * Construct a new hyperblock from a PredicateBlock.
   * <p>
   * @param start the block which starts the hyperblock.
   * @param end the block which ends the hyperblock.
   * @param registers the RegisterSet to use.
   */
  public Hyperblock(PredicateBlock start, BitVect predicates, Trips2RegisterSet registers)
  {
    this(registers);

    this.predicates = predicates;
    this.head       = start; 
  }
  
  /**
   * Construct a new hyperblock from a Predicate Flow Graph.
   * <p>
   * @param registers the RegisterSet to use.
   */
  private Hyperblock(Trips2RegisterSet registers)
  {
    super(nextLabel++);
    
    this.regs       = registers;
    this.readBanks  = new int[Trips2RegisterSet.numBanks];
    this.writeBanks = new int[Trips2RegisterSet.numBanks];
    this.loopNumber = 0;   
  }
  
  /**
   * Compute the set of predicates used in this region and the number of branches.
   */
  private void determinePredicatesBranches(Instruction first)
  {
    predicates  = new BitVect();
    numBranches = 0;
    
    if (first.isLabel() || first.isMarker())
      first = first.getNext();

    for (Instruction inst = first; inst != null; inst = inst.getNext()) {
      if (inst.isLabel())
        break;

      if (inst.isMarker())
        continue;
      
      if (inst.isBranch())
        numBranches++;

      if (inst.isPredicated())
        predicates.set(inst.getPredicate(0));
    }
  }
  
  /**
   * Build the PFG.
   * <br>
   * This routine destroys the original list of instructions!
   */
  private void createPredicateFlowGraph(Instruction first)
  {
    Instruction prev = first;
    
    head        = new PredicateBlock(); 
    tail        = head;
    blocksTrue  = new IntMap<PredicateBlock>(11);
    blocksFalse = new IntMap<PredicateBlock>(11);
  
    tail.appendInstruction(first);
    
    for (Instruction inst = first.getNext(); inst != null; prev = inst, inst = inst.getNext()) {
      prev.setNext(null);
      
      if (inst.isLabel())
        break;
      
      // Add any markers to the last block.
      if (inst.isMarker()) {
        tail.appendInstruction(inst);
        continue;
      }
      
      // Add the instruction to its block.
      tail = getBlock(inst);
      tail.appendInstruction(inst);
      
      // Instructions that define predicates need to be linked to their successor predicate blocks.
      int ra = inst.getDestRegister();
      if (predicates.get(ra)) {
        ((TripsInstruction) inst).setDefinesPredicate();  // We could do this in the code generator.
        addEdges(tail, (TripsInstruction) inst);
      } 
    }
    
    // End the graph in an unpredicated block for write instructions.
    if (tail.isPredicated())
      tail = insertMerge();
    
    // Free memory.
    blocksTrue  = null;
    blocksFalse = null;
  }
  
  /**
   * Link a block to the blocks reached by the instruction.
   * This creates any missing blocks.
   */
  private void addEdges(PredicateBlock b, TripsInstruction inst)
  {
    boolean needTrue  = true;
    boolean needFalse = true;
    int     rp        = inst.getDestRegister();
    
    // How many predicate paths do we have to create?
    if (!isSplit(inst)) {
      needTrue  = ((ImmediateInstruction) inst).getImm() > 0 ? true : false;
      needFalse = !needTrue;
    }
      
    // Create the true path.
    if (needTrue) {
      PredicateBlock bt = blocksTrue.get(rp);
      if (bt == null) {
        bt = new PredicateBlock(rp, true);
        blocksTrue.put(rp, bt);
      }
      bt.addInEdge(b);
      b.addOutEdge(bt);
    } 
      
    // Create the false path.
    if (needFalse) {
      PredicateBlock bf = blocksFalse.get(rp);
      if (bf == null) {
        bf = new PredicateBlock(rp, false);
        blocksFalse.put(rp, bf);
      }
      bf.addInEdge(b);
      b.addOutEdge(bf);
    } 
  }
  
  /**
   * Return true if the instruction causes a split in the PFG.
   * <br>
   * Test instructions and movs that define predicates create splits.
   * Immediate instructions that define predicates only create a single path.
   */
  private static boolean isSplit(Instruction inst)
  {
    int opcode = inst.getOpcode();
    if ((opcode >= Opcodes.TEQ && opcode <= Opcodes.TGTU) ||
        (opcode >= Opcodes.TEQI && opcode <= Opcodes.TGTUI) ||
        (opcode >= Opcodes.FEQ && opcode <= Opcodes.FGT) ||
        (opcode == Opcodes.MOV))
      return true;
    
    return false;
  }
  
  /**
   * Returns the block for the given instruction.
   */
  private PredicateBlock getBlock(Instruction inst)
  {    
    // Handle unpredicated instructions.
    if (!inst.isPredicated()) {
      if (!tail.isPredicated())
        return tail;
      return insertMerge();
    }
    
    boolean sense = inst.isPredicatedOnTrue();
    int     rp    = inst.getPredicate(0);
    
    // We check the last block because its often the one we want.
    if ((tail.getPredicate() == rp) &&
        (tail.isPredicatedOnTrue() == sense))
      return tail;
    
    // Return the block that matches the predicate.
    return sense ? blocksTrue.get(rp) : blocksFalse.get(rp);
  }
  
  /**
   * Insert a merge point between all leaf nodes and a new unpredicated block.
   * Nothing is inserted if the PFG already ends in an unpredicated block.
   * <br>
   * Returns the unpredicated block that is the final merge point in the graph.
   */
  private PredicateBlock insertMerge()
  {
    if (head.numOutEdges() == 0)
      return head;
    
    Stack<Node>            wl    = WorkArea.<Node>getStack("insertMerge");
    Vector<PredicateBlock> leafs = new Vector<PredicateBlock>();

    // Find the leaf nodes.

    head.nextVisit();
    head.setVisited();
    wl.add(head);

    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      if (b.numOutEdges() > 0)
        b.pushOutEdges(wl);
      else
        leafs.add(b);
    }

    WorkArea.<Node>returnStack(wl);

    // If there is only one leaf node then it must be unpredicated
    // and we are done.
    
    int l = leafs.size();
    if (l == 1)
      return leafs.elementAt(0);
    
    // Otherwise, insert an unpredicated block as a merge point
    // and return it.
    
    PredicateBlock merge = new PredicateBlock();
    for (int i = 0; i < l; i++) {
      PredicateBlock b = leafs.elementAt(i);
      b.addOutEdge(merge);
      merge.addInEdge(b);
    }
    
    return merge;
  }

  /**
   * Remove dead predicate blocks.
   * <br>
   * If a predicate block becomes empty, and it was the only use of a predicate,
   * then the predicate block needs to be removed to maintain the PFG.
   * <br>
   * For example,
   * <br>
   *   pb1<p1,true>:  mov_t<p1> p2, #1 -> pb3
   *   pb2<p1,false>: mov_f<p1> p2, #1 -> pb3
   *   pb3<p2,true>:  empty -> pb4
   * <br>
   * PB1 and PB2 both create predicate {p2,true} and their successor is PB3 which
   * has no instructions. When we go into SSA form, no phi will be inserted for p2
   * because there is no instruction that references it. When SSA tries to rename
   * the predicate for PB3 to maintain the PFG, it will fail because p2 has not
   * been seen and there is nothing on the rename stack for it. We remove PB3 from 
   * the PFG and make the successor of PB1 and PB2, PB4 to maintain the PFG.
   */
  public void removeDeadPredicateBlocks()
  {
    Stack<Node>            wl    = WorkArea.<Node>getStack("removeDeadPredicateBlocks");
    Vector<PredicateBlock> empty = new Vector<PredicateBlock>();

    assert (inSSA());
    
    // Find all the empty predicate blocks.
    
    head.nextVisit(); 
    head.setVisited();
    wl.add(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      if (!b.isPredicated())
        continue;

      if (b.getFirstInstruction() == null)
        empty.add(b);
    }

    WorkArea.<Node>returnStack(wl);

    // Lookup the instruction that defines the predicate 
    // for the predicate block. If it is null, then
    // remove the predicate block.

    IntMap<Instruction> useDef  = ssa.getUseDef();
    boolean             changed = false;

    for (int i = 0; i < empty.size(); i++) {
      PredicateBlock b   = empty.get(i);
      int            rp  = b.getPredicate();
      Instruction    def = useDef.get(rp);
 
      if (def != null)
        continue;

      // Move the incoming and outgoing edges.
      
      for (int j = 0; j < b.numInEdges(); j++) {
        PredicateBlock pred = (PredicateBlock) b.getInEdge(j);
        
        for (int k = 0; k < b.numOutEdges(); k++) {
          PredicateBlock succ = (PredicateBlock) b.getOutEdge(k);
          succ.addInEdge(pred);
          pred.addOutEdge(succ);
        }
      }
      
      b.unlink();
      changed = true;
    }

    if (changed)
      invalidateDomination();
  } 
  
  /**
   * Compute the set of predicates and number of branches.
   */
  public void determinePredicatesBranches()
  {
    Stack<Node> wl = WorkArea.<Node>getStack("determinePredicatesBranches");
    
    predicates.reset();  // clear the previous predicates
    numBranches = 0;
    
    head.nextVisit();
    head.setVisited();
    wl.add(head);

    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      if (b.isPredicated())
        predicates.set(b.getPredicate());
      if (b.hasBranch())
        numBranches++;
    }

    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Return true if this hyperblock contains a function call.
   */
  public final boolean hasCall()
  {
    return hasCall;
  }
  
  /**
   * Return true if there is a branch to the specified hyperblock.
   */
  public final boolean hasBranchTo(Hyperblock s)
  {
    Stack<Node>    wl    = WorkArea.<Node>getStack("hasBranchTo");
    PredicateBlock start = getFirstBlock();
    
    start.nextVisit();
    start.setVisited();
    wl.push(start);
    
    int     tl  = ((Label) s.getFirstBlock().getFirstInstruction()).getLabelIndex();
    boolean ret = false;
    
    while (!wl.empty()) {
      PredicateBlock pb = (PredicateBlock) wl.pop();
      pb.pushOutEdges(wl);
      
      for (Instruction i = pb.getFirstInstruction(); i != null; i = i.getNext()) {
        if (i.getOpcode() == Opcodes.BRO || i.getOpcode() == Opcodes.BR) {
          TripsBranch b = (TripsBranch) i;
          if (tl == b.getTarget().getLabelIndex()) {
            ret = true;
            break;
          }
        }
      }
    }
    
    WorkArea.<Node>returnStack(wl);
    
    return ret;
  }
  
  /**
   * Return true if there is a call to the specified hyperblock.
   */
  public final boolean hasCallTo(Hyperblock s)
  {
    Stack<Node>    wl    = WorkArea.<Node>getStack("hasCallTo");
    PredicateBlock start = getFirstBlock();
    
    start.nextVisit();
    start.setVisited();
    wl.push(start);
    
    int     tl  = ((Label) s.getFirstBlock().getFirstInstruction()).getLabelIndex();
    boolean ret = false;
    
    while (!wl.empty()) {
      PredicateBlock pb = (PredicateBlock) wl.pop();
      pb.pushOutEdges(wl);
      
      for (Instruction i = pb.getFirstInstruction(); i != null; i = i.getNext()) {
        if (i.getOpcode() == Opcodes.CALLO || i.getOpcode() == Opcodes.CALL) {
          TripsBranch b = (TripsBranch) i;
          if (tl == b.getTarget().getLabelIndex()) {
            ret = true;
            break;
          }
        }
      }
    }
    
    WorkArea.<Node>returnStack(wl);
    
    return ret;
  }
  
  /**
   * Return true if this hyperblock contains a switch statement.
   */
  public final boolean hasSwitch()
  {
    return hasSwitch;
  }

  /**
   * Return the probability of branching to the specified hyperblock.
   */
  public final double getBranchProbability(Hyperblock succ)
  {
    if (probForBlock == null)
      return 0.0;
    
    Double prob = probForBlock.get(succ.getFirstBlock().getFirstInstruction());
    return (prob == null) ? 0.0 : prob.doubleValue();
  }
  
  /**
   * Set the probability of branching to the specified hyperblock.
   */
  public final void setBranchProbability(Hyperblock succ, double prob)
  {
    if (probForBlock == null)
      probForBlock = new HashMap<Instruction, Double>(7);   
    
    probForBlock.put(succ.getFirstBlock().getFirstInstruction(), new Double(prob));
  }
  
  /**
   * Set the probability of branching to the specified hyperblock.
   */
  public final void setBranchProbability(Instruction succ, double prob)
  {
    if (probForBlock == null)
      probForBlock = new HashMap<Instruction, Double>(7);   
    
    probForBlock.put(succ, new Double(prob));
  }
  
  /**
   * Return true if this hyperblock contains a back-edge to itself.
   */
  public final boolean isSimpleLoop()
  {
    return (indexOfOutEdge(this) != -1);
  }
  
  /**
   * Return the registers live-in to (and used in) the hyperblock
   */
  public final BitVect getLiveIn()
  {
    return livein;
  }
  
  /**
   * Update the registers live-in and used by the hyperblock
   */
  public final void setLiveIn(BitVect in)
  {
    this.livein = in;
  }
  
  /**
   * Return the registers live-out of (and define by) the hyperblock
   */
  public final BitVect getLiveOut()
  {
    return liveout;
  }
  
  /**
   * Update the registers live-out and defined by the hyperblock.
   */
  public final void setLiveOut(BitVect out)
  {
    this.liveout = out;
  }
  
  /**
   * Get the predicates for the hyperblock.
   */
  public final BitVect getPredicates()
  {
    return predicates;
  }
  
  /**
   * Clear the predicates used in this hyperblock.
   */
  public final void clearPredicates()
  {
    predicates.reset();
  }
  
  /**
   * Return true if the register is used as a predicate.
   */
  public final boolean isPredicate(int reg)
  {
    if (predicates == null)
      return false;
    return predicates.get(reg);
  }

  /**
   * Set that a register is used as a predicate.
   */
  public final void setPredicate(int reg)
  {
    predicates.set(reg);
  }
  
  /**
   * Clone the Predicate Flow Graph and return the clone's root predicate block.
   */
  private PredicateBlock clonePredicateFlowGraph()
  {
    Stack<Node>                             wl     = WorkArea.<Node>getStack("clonePredicateFlowGraph");
    HashMap<PredicateBlock, PredicateBlock> copies = new HashMap<PredicateBlock, PredicateBlock>(11);
    
    // Copy each predicate block.
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      PredicateBlock copy = block.copy();
      copies.put(block, copy);
    }
    
    // Create the new graph.
    
    head.nextVisit();
    head.setVisited();
    wl.add(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      PredicateBlock copy = copies.get(block);
      int            ol   = block.numOutEdges();
      for (int i = 0; i < ol; i++) {
        PredicateBlock out   = (PredicateBlock) block.getOutEdge(i);
        PredicateBlock ocopy = copies.get(out);
        
        copy.addOutEdge(ocopy);
        ocopy.addInEdge(copy);
      }
    }
    
    WorkArea.<Node>returnStack(wl); 
    
    return copies.get(head);
  }
  
  /**
   * Copy the hyperblock.
   */
  public Hyperblock copy()
  {
    PredicateBlock start = clonePredicateFlowGraph();
    Hyperblock     copy  = new Hyperblock(regs);
    
    copy.head        = start;
    copy.hasCall     = hasCall;
    copy.hasSwitch   = hasSwitch;
    copy.color       = color;
    copy.blockSize   = blockSize;
    copy.fanout      = fanout;
    copy.maxLSID     = maxLSID;
    copy.numBranches = numBranches;
    copy.tag         = tag;
    copy.loopNumber  = loopNumber;

    if (livein != null)
      copy.livein = livein.clone();
    if (liveout != null)
      copy.liveout = liveout.clone();
    if (predicates != null)
      copy.predicates = predicates.clone();
    if (probForBlock != null)
      copy.probForBlock = probForBlock.clone();
    
    copy.findLastBlock();
    
    return copy;
  }

  /**
   * Given a set of PFG nodes that are the same depth from the root node,
   * this routine will return the PFG nodes which are one level deeper.
   */
  public final Vector<PredicateBlock> getNextPFGLevel(Vector<PredicateBlock> wl)
  {
    Vector<PredicateBlock> next = new Vector<PredicateBlock>();

    // Find the nodes in the next level of the PFG.

    int l = wl.size();
    for (int i = 0; i < l; i++) {
      PredicateBlock block = wl.elementAt(i);

      // Check that the predecessor of each successor has been done already.
      // If not, the successor is not in the next level of the PFG.

      for (int j = 0; j < block.numOutEdges(); j++) {
        PredicateBlock succ     = (PredicateBlock) block.getOutEdge(j);
        boolean        addBlock = true;

        for (int k = 0; k < succ.numInEdges(); k++) {
          PredicateBlock pred = (PredicateBlock) succ.getInEdge(k);

          if (!pred.visited()) {
            addBlock = false;
            break;
          }
        }

        if (addBlock && !next.contains(succ))
          next.add(succ);
      }
    }

    // Mark the nodes in the next level as visited.

    int nl = next.size();
    for (int i = 0; i < nl; i++) {
      PredicateBlock succ = next.elementAt(i);
      succ.setVisited();
    }

    return next;
  }

  /**
   * Return the first block in the PFG.
   */
  public final PredicateBlock getFirstBlock()
  {
    return head;
  }
  
  /**
   * Return the last block in the PFG.
   */
  public final PredicateBlock getLastBlock()
  {
    assert (tail != null);
    
    return tail;
  }
  
  /**
   * Find the last block in the PFG.
   * <br>
   * There must be a path from the root to the last block or the PFG is corrupt.
   */
  public void findLastBlock()
  { 
    tail = head;
    while (tail.numOutEdges() > 0) {
      tail = (PredicateBlock) tail.getOutEdge(0);
    }
    assert (!tail.isPredicated());
  }

  /**
   * Find the last block in the PFG or create one if its missing.
   * <br>
   * Useful when fixing the PFG as its being pulled apart.
   */
  public final void updateLastBlock()
  {
    this.tail = insertMerge();
  }
  
  /**
   * Return the dominance frontier information for the PFG.
   */
  public final DominanceFrontier getDominanceFrontier()
  {
    if (domf == null) {
      if (dom == null)
        dom = new Domination(false, head); /* Do dominators */
      domf = new DominanceFrontier(head, dom);
    }

    return domf;
  }

  /**
   * Return the dominator information for the PFG.
   */
  public final Domination getDomination()
  {
    if (dom == null)
      dom = new Domination(false, head); /* Do dominators */

    return dom;
  }

  /**
   * Clear the dominator information for the PFG.
   */
  public final void invalidateDomination()
  {
    this.domf = null;
    this.dom  = null;
  }

  /**
   * Output the predicate flow graph for debugging.
   */
  public final void dumpPredicateFlowGraph()
  {
    System.out.println("\n*** BEGIN PREDICATE FLOW GRAPH ***\n");
    
    if (livein != null) {
      System.out.print(" in: ");
      livein.outputSetBits();
      System.out.println("");
    }
    
    if (liveout != null) {
      System.out.print("out: ");
      liveout.outputSetBits();
      System.out.println("");
    }
    
    System.out.println(toString());

    Vector<PredicateBlock> wl = new Vector<PredicateBlock>();

    head.nextVisit();
    head.setVisited();
    wl.add(head);

    while (!wl.isEmpty()) {
      int l = wl.size();

      for (int i = 0; i < l; i++)
        System.out.println(wl.elementAt(i));

      wl = getNextPFGLevel(wl);
    }
  }
  
  /**
   * Write a dot file that shows the hyperblock flow graph.
   */
  public static void writeDotFlowGraph(Hyperblock hbStart, String filename)
  {
    Stack<Node>       wl       = WorkArea.<Node>getStack("writeDotFlowGraph");
    HashSet<Object>   visited  = WorkArea.<Object>getSet("writeDotFlowGraph"); 
    String            basename = filename.substring(0, filename.lastIndexOf('.'));
 
    try {
      java.io.FileWriter     fw     = new java.io.FileWriter(filename);
      java.io.PrintWriter    out    = new java.io.PrintWriter(fw, true);  
      java.text.NumberFormat format = java.text.NumberFormat.getInstance();
      
      format.setMaximumFractionDigits(2);
      
      visited.add(hbStart);
      wl.push(hbStart);
      
      out.println("digraph " + basename + " {");
      
      while (!wl.isEmpty()) {
        Hyperblock hb = (Hyperblock) wl.pop();
        hb.pushOutEdges(wl, visited);
        
        String bname  = hb.getBlockName();
        String bdname = bname.replace('$', '_');
        
        out.print(bdname);
        out.print(" [label=\"");
        out.print(bname);
        out.print("\\n");
        out.print(hb.blockSize + hb.fanout);
        out.print("(");
        out.print(hb.maxLSID);
        out.print(")");
        out.println("\"];");
        
        for (int i = 0; i < hb.numOutEdges(); i++) {
          Hyperblock succ   = (Hyperblock) hb.getOutEdge(i);
          String     ename  = succ.getBlockName();
          String     edname = ename.replace('$', '_');
          
          out.print(bdname);
          out.print(" -> ");
          out.print(edname);
          out.print(" [label=\"");
          out.print(format.format(hb.getBranchProbability(succ)));
          out.print("\"]");
          out.println(";");
        }
      }

      out.println("};");
    } catch (java.lang.Exception ex) {
      System.err.println("Error opening the dot file");
    }
    
    WorkArea.<Object>returnSet(visited);
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Returns the name of the block, as seen in the TIL.
   */
  public String getBlockName()
  {
    Instruction label = head.getFirstInstruction();
    if (label instanceof BeginMarker)
      return ((BeginMarker) label).getRoutine().getName();
    
    TripsLabel lab = (TripsLabel) label;
    return lab.getRoutine().getName() + "$" + lab.getLabelIndex();
  }
  
  /**
   * Compute the number of targets for each instruction.
   * <br>
   * Returns a map of vectors indexed by the register number. Since multiple instructions
   * may define a register, there is one entry for each instruction in the vector. The
   * entries represent the number of targets for that instruction. 
   * <br>
   * This routine also computes if a target can be fanned out with a mov3 instruction.
   * The bit vector will be true if a mov3 cannot be used.
   */
  public final IntMap<Vector<Integer>> computeTargets(Stack<Node> wl, BitVect mov3)
  {
    IntMap<Vector<Integer>> targets = new IntMap<Vector<Integer>>(151);   
    IntMap<Integer>         type    = new IntMap<Integer>(151);  // null=none, 1=op1, 2=op2, 3=predicate, 4=write
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.isMarker())
          continue;

        int dest = inst.getDestRegister();
        if (dest > -1) {
          Vector<Integer> v = targets.get(dest);
          if (v == null) {
            v = new Vector<Integer>();
            targets.put(dest, v);
          }

          int n = Opcodes.getNumTargets(inst);
          v.add(new Integer(n));
        }
        
        int opcode = inst.getOpcode();
        int format = Opcodes.getFormat(opcode);
        int rb     = -1;
        int rc     = -1;
        int rw     = -1;
        
        switch (format) {
          case Opcodes.G2:
            rc = ((GeneralInstruction) inst).getRc();
            rb = ((GeneralInstruction) inst).getRb();
            break;
          case Opcodes.G1:
            rb = ((GeneralInstruction) inst).getRb();
            break;
          case Opcodes.W1:
            rw = ((GeneralInstruction) inst).getRb();
            break;
          case Opcodes.L1:
            rb = ((LoadInstruction) inst).getRb();
            break;
          case Opcodes.S2:
            rb = ((StoreInstruction) inst).getRb();
            rc = ((StoreInstruction) inst).getRc();
            break;
          case Opcodes.I1:
            rb = ((ImmediateInstruction) inst).getRb();
            break;
          case Opcodes.B1:
            rb = ((TripsBranch) inst).getRb();
            break;
          case Opcodes.C1:
            rb = ((ConstantInstruction) inst).getRb();
            break;
          case Opcodes.ENT:
          case Opcodes.I0:
          case Opcodes.G0:
          case Opcodes.B0:
          case Opcodes.LPF:
          case Opcodes.C0:
          case Opcodes.R0:
            break;
          case Opcodes.PHI:
          case Opcodes.UNK:
            throw new scale.common.InternalError("Illegal instruction format " + inst);
        }
        
        if (rb != -1) {
          Integer os = type.get(rb);
          if (os == null) 
            type.put(rb, INT1);
          else if (os.intValue() != 1)
            mov3.set(rb);
        }

        if (rc != -1) {
          Integer os = type.get(rc);
          if (os == null)
            type.put(rc, INT2);
          else if (os.intValue() != 2)
            mov3.set(rc);
        }
        
        if (inst.isPredicated()) {
          int     rp = inst.getPredicate(0);
          Integer os = type.get(rp);
          if (os == null)
            type.put(rp, INT3);
          else if (os.intValue() != 3)
            mov3.set(rp);
        }

        if (rw != -1) {
          Integer os = type.get(rw);
          if (os == null) 
            type.put(rw, INT4);
          else if (os.intValue() != 4)
            mov3.set(rw);
        }        
      }
    }
    
    return targets;
  }


  /**
   * Calculate the statistics for a hyperblock.
   */
  private void analyze()
  {
    blockSize      = 0;     
    fanout         = 0;     
    spillSize      = 0;     
    numSpills      = 0;   
    numBranches    = 0;    
    hasCall        = false;
    hasSwitch      = false;
    hasDummyStores = false;
    maxLSID        = 0;     
    
    // To analyze a block, all the load/store ids have to be recomputed.
    // This will insert nulls and dummy stores that have to be removed 
    // after the analysis is complete.
    
    assignLoadStoreQueueIds();
    
    // Determine the number of targets for each operand and whether or
    // not a mov3 can be used for fanout.

    Stack<Node>             wl      = WorkArea.<Node>getStack("analyze");
    BitVect                 mov3    = new BitVect();
    IntMap<Vector<Integer>> targets = computeTargets(wl, mov3);
    IntMap<Integer>         uses    = new IntMap<Integer>(37);
    
    // Compute the statistics for the hyperblock.
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      block.analyze(targets, uses, mov3);
      
      blockSize      += block.getBlockSize();
      fanout         += block.getFanout();
      numBranches    += block.hasBranch() ? 1 : 0;
      numSpills      += block.numSpills();
      spillSize      += block.getSpillSize();
      hasDummyStores |= block.hasDummyStores();
      hasCall        |= block.hasCall();
      hasSwitch      |= block.hasSwitch();
      
      int id = block.maxLSID();
      if (id > maxLSID)
        maxLSID = id;
    }
      
    // Remove any dummy stores inserted by load/store id assignment.
  
    if (hasDummyStores)
      removeDummyStores();
  
    WorkArea.<Node>returnStack(wl);
  }

  /**
   * Compute the number of read/write instructions in a hyperblock and
   * the banks used.  This method can only be called after register
   * allocation.
   */
  private void computeReadWriteStatistics()
  {
    Stack<Node>    wl          = WorkArea.<Node>getStack("computeReadWriteStatistics");
    PredicateBlock start       = getFirstBlock();
    int            vr          = regs.numRealRegisters();
    boolean[]      globalsUsed = new boolean[vr]; // True if the global register was used.
    boolean[]      globalsDefd = new boolean[vr]; // True if the global register was defined.

    numReads   = 0;
    numWrites  = 0;
    readBanks  = new int[Trips2RegisterSet.numBanks];
    writeBanks = new int[Trips2RegisterSet.numBanks];
    	
    start.nextVisit();
    start.setVisited();
    wl.push(start);

    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);

      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.isMarker())
          continue;

        int[] srcs = inst.getSrcRegisters();
        if (srcs != null) {
          for (int i = 0; i < srcs.length; i++) {
            int reg = srcs[i];

            // Keep track of the global registers used.

            if ((reg < vr) &&
                (globalsUsed[reg] == false) &&
                (globalsDefd[reg] == false)) {
              globalsUsed[reg] = true;
              numReads++;
            }
          }
        }

        int dest = inst.getDestRegister();
        if (dest != -1) {
          // Keep track of global registers defined.

          if ((dest < vr) && (globalsDefd[dest] == false)) {
            globalsDefd[dest] = true;
            numWrites++;
            
            // If write instructions are not being nullified, the
            // global must also be read in so it is defined on the
            // predicate paths which do not define it.
            
            if (!Trips2Machine.nullifyWrites &&
                inst.isPredicated() &&
                (globalsUsed[dest] == false)) {
              globalsUsed[dest] = true;
              numReads++;
            }
          }
        }
      }
    }

    // Update the banking statistics for the hyperblock.

    for (int i = 0; i < globalsUsed.length; i++) {
      if (globalsUsed[i] == true) {
        int bank = regs.getBank(i);
        readBanks[bank]++;
      }
    }

    for (int i = 0; i < globalsDefd.length; i++) {
      if (globalsDefd[i] == true) {
        int bank = regs.getBank(i);
        writeBanks[bank]++;
      }
    }

    WorkArea.<Node>returnStack(wl);
  }

  /**
   * Return true if this is a well formed TRIPS block.
   */
  public boolean isLegalBlock(boolean beforeRegisterAllocation)
  {
    if ((blockSize + fanout + spillSize) > Trips2Machine.maxBlockSize)
      return false;
 
    if (maxLSID >= Trips2Machine.maxLSQEntries)
      return false;

    if (beforeRegisterAllocation)
      return true;
    
    computeReadWriteStatistics();
    
    // Check that the number of read/write instructions is in range.

    if ((numReads > Trips2RegisterSet.perBlockRegAccesses) ||
        (numWrites > Trips2RegisterSet.perBlockRegAccesses))
      return false;

    // Check that the number of read/write instructions in each bank is in range.

    for (int i = 0; i < Trips2RegisterSet.numBanks; i++) {
      if ((readBanks[i] > Trips2RegisterSet.bankAccesses) ||
          (writeBanks[i] > Trips2RegisterSet.bankAccesses))
          return false;
    }
    
    return true;
  }

  /**
   * Return the size of the instructions in the block (not including fanout).
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
   * Update the fanout.
   */
  public final void updateFanout(int fanout)
  {
    this.fanout += fanout;
  }

  /**
   * Return the number of branches in the block.
   */
  public final int numBranches()
  {
    return numBranches;
  }

  /**
   * Return the highest assigned load/store ID in the block.
   */
  public final int maxLSID()
  {
    return maxLSID;
  }
  
  /**
   * Return the number of spill instructions in the block.
   */
  public final int numSpills()
  {
    return numSpills;
  }

  /**
   * Return the estimated size of the spills in the block.
   */
  public final int getSpillSize()
  {
    return spillSize;
  }

  /**
   * Add a spill to the hyperblock.
   */
  public final void addSpill(PredicateBlock block, Instruction inst)
  {
    int num = block.estimateNumInstructions(inst);
    
    fanout++;
    numSpills++;
    maxLSID++;
    spillSize += num;
    block.addSpill(num);
  }
  
  /**
   * Return true if the block contains stores inserted for nullification.
   */
  public final boolean hasDummyStores()
  {
    return hasDummyStores;
  }

  /**
   * Assign load/store queue ids and nullify stores.
   */
  protected void assignLoadStoreQueueIds()
  {
    /* The default policy is to assign unique ids to
       loads and minimize the ids assigned to stores. */
    
    if (useMinMaxLoadStoreAssignment) {
      assignLoadStoreQueueIdsMinMax();
    } else {
      assignLoadStoreQueueIdsMax();
    }
    
    /* Nullify store ids. */
    
    nullifyStoreIds();
  }
  
  /**
   * Assign each load and store a unique identifier.
   * <br>
   * This is a baseline for comparing other policies.
   * You really don't want to use this otherwise.
   */
  private void assignLoadStoreQueueIdsMax()
  {
    Vector<PredicateBlock> wl = new Vector<PredicateBlock>();
    int nextId = 0;
    
    head.nextVisit();
    head.setVisited();
    wl.add(head);

    while (!wl.isEmpty()) {
      int sz = wl.size();
      for (int i = 0; i < sz; i++) {
        PredicateBlock b = wl.elementAt(i);
        
        for (Instruction inst = b.getFirstInstruction(); inst != null; inst = inst.getNext()) {
          if (inst.isLoad()) {
            ((LoadInstruction) inst).setLSQid(nextId);
            nextId++;
          } else if (inst.isStore()) {
            ((StoreInstruction) inst).setLSQid(nextId);
            nextId++;
          }
        }
      }
      wl = getNextPFGLevel(wl);
    }
  }
  
  /**
   * Helper routine for assignLoadStoreQueueIdsMinMax()
   * <br>
   * Once all the memory instructions in a predicate block have been assigned
   * this routine adds the successor predicate blocks to the worklist if they
   * have all been assigned to.
   */
  private void addNextBlocks(Vector v, Vector wl) 
  {     
    PredicateBlock pb = (PredicateBlock) v.get(0);
    Instruction    ni = (Instruction) v.get(1);
    
    assert (ni == null);
      
    pb.setVisited();
      
    // Check each successor to make sure that its predecessors are all done.
    // If they are all done then it the successor to the worklist.
    
    int sl = pb.numOutEdges();
    for (int i = 0; i < sl; i++) {
      PredicateBlock succ         = (PredicateBlock) pb.getOutEdge(i);
      int            pl           = succ.numInEdges();
      boolean        allPredsDone = true;
      
      for (int j = 0; j < pl; j++) {
        PredicateBlock pred = (PredicateBlock) succ.getInEdge(j);
        if (!pred.visited()) {
          allPredsDone = false;
          break;
        }
      }
 
      if (!allPredsDone)
        continue;
      
      // Add the successor to the worklist if its not already in it
      
      if (!wl.contains(succ)) {
        Vector v2 = new Vector(2);
        v2.add(0, succ);
        v2.add(1, succ.getFirstInstruction());
        wl.add(v2); 
      }
    }
    
    // Remove the original item from the worklist - its done!
    
    wl.remove(v);
  }
  
  /**
   * Assign load/store identifiers.
   * <br>
   * This algorithm assigns each load a unique identifier and minimizes the identifiers 
   * assigned to stores.
   * <br>
   * Minimizing the store ids reduces the number of instructions required for nullification
   * and results in a block with the fewest overhead instructions. Maximizing load ids gives
   * the most flexibility to the compiler since it allows all loads to be unpredicated and 
   * execute speculatively. 
   * <br>
   * The algorithm works as follows:
   * <br>
   * Starting at the root of the PFG we have a list of the next memory insts in each predicate
   * block to assign. If there is a load on the list we assign it the next id and replace
   * the assigned load with the next memory inst in the predicate block. If every inst on the
   * list is a store we assign all of them the same id. When we reach the end of a predicate
   * block we use the successor predicate block if all the predecessors of the successor have
   * been assigned. 
   */
  private void assignLoadStoreQueueIdsMinMax()
  {
    Vector wl     = new Vector();
    int    nextId = 0;
    
    head.nextVisit();   
    
    // The worklist contains pairs of <PredicateBlock, MemoryInstruction> to process
    // where MemoryInstruction is the next load or store in PredicateBlock.
    
    Vector v1 = new Vector(2);
    v1.add(0, head);
    v1.add(1, head.getFirstInstruction());
    wl.add(v1);

    while (!wl.isEmpty()) {
      boolean assignedLoad = false;
      int     l            = wl.size();
      Vector  wlcopy       = new Vector(l);
      
      wlcopy.addVectors(wl);
     
      // Take any item of the worklist and assigns ids to loads 
      // until we find a store.
      
      for (int i = 0; i < l; i++) {
        Vector      v  = (Vector) wlcopy.get(i);
        Instruction ni = (Instruction) v.get(1);
        
        while (ni != null) {
          if (ni.isStore()) {
            break;
          } else if (ni.isLoad()) {
            assignedLoad = true;
            ((LoadInstruction) ni).setLSQid(nextId);
            nextId++;
          }
          ni = ni.getNext();
        }
        
        v.set(1, ni);
        
        // If we have reached the end of the predicate block we add the
        // successors to the worklist if all the predecessors are done.
        
        if (ni == null) {
          addNextBlocks(v, wl);
          
          // There may be a load at the beginning of a successor predicate
          // block so we set assignedLoad to force us to check 
          
          assignedLoad = true;
        }
      }
      
      // If a load was assigned we can't do stores yet.
      
      if (assignedLoad)
        continue;
      
      // If we get here it means that every instruction on
      // the worklist is a store that gets the same id.
      
      for (int i = 0; i < l; i++) {
        Vector         v      = (Vector) wlcopy.get(i);
        PredicateBlock pb     = (PredicateBlock) v.get(0);
        Instruction    ni     = (Instruction) v.get(1);
        int            blabel = pb.getLabel(); 
      
        assert(ni.isStore());
        
        ((StoreInstruction) ni).setLSQid(nextId);
        ni = ni.getNext();
        
        while (ni != null) {
          if (ni.isStore() || ni.isLoad()) 
            break;
          ni = ni.getNext();
        }
        
        v.set(1, ni);
        
        if (ni == null) {
          addNextBlocks(v, wl);
        }
      }
      
      nextId++;      
    }
  }

  /**
   * Nullify store ids.
   * <br>
   * We traverse the PFG backwards to compute the store ids available at each predicate block.
   * Then we compute the nulls needed based on the splits in the graph. For example, given
   * the following PFG:
   * <p>
   *              S0 Sin{1,3,5,6,7} Sout{0,1,3,5,6,7}
   *              /\
   * Sin{7}     S1  S3 Sin{5,6,7} Sout{3,5,6,7}
   * Sout{1,7}  |    /\
   *            |   -  S5 Sin{6,7} Sout{5,6,7} and the empty node is Sin{6,7} Sout{6,7}
   *             \   \/
   *              \  S6 Sin{7} Sout{6,7}
   *               \ /  
   *                S7 Sin{} Sout{7}
   *<p>
   * We start at the tail of the graph (S7). There are no succesor predicate blocks so the set 
   * of store ids into S7 is empty. The set of ids out of S7 is {7}.
   * <br>
   * Next we evaluate the predicate blocks that contain S1 and S6. We never evaluate a block
   * until all of its children predicate blocks have been evaluated. For example, S0 cannot be 
   * evaluated until S1 and S3 are done.
   * <br>
   * After computing Sin and Sout for every node we insert nulls by examing the split points
   * in the graph. The nulls required in a child predicate block of the split are are,
   * <br>
   * Sin(split) - Sout(split's child) 
   * <br>
   * i.e. Sin(S0) - Sout(S3) = Sin{1,3,5,6,7} - Sout{3,5,6,7} = {1}
   * <br>
   * Therefore we insert a null for store id 1 in S3.
   */
  private void nullifyStoreIds()
  {
    Vector<Node>          wl          = new Vector<Node>();
    Stack<PredicateBlock> splitPoints = new Stack<PredicateBlock>();
    IntMap<BitVect>       storeIdsIn  = new IntMap<BitVect>(61);
    IntMap<BitVect>       storeIdsOut = new IntMap<BitVect>(61);
    
    tail.nextVisit();
    wl.add(tail);

    // Compute the set of store ids in and out of every predicate block
    
    while (!wl.isEmpty()) {
      PredicateBlock block     = (PredicateBlock) wl.remove(0);
      int            pl        = block.numOutEdges();
      boolean        succsDone = true;
      BitVect        in        = new BitVect();
      
      // Compute the set of store ids coming into this block.
      // If all the successor blocks aren't done we wait.
      
      for (int i = 0; i < pl; i++) {
        PredicateBlock succ = (PredicateBlock) block.getOutEdge(i);
        if (!succ.visited()) {
          succsDone = false;
          break;
        }
        
        int     slabel = succ.getLabel();
        BitVect sout   = storeIdsOut.get(slabel);
        in.or(sout);  
      }
      
      if (!succsDone) {
        wl.add(block);
        continue;
      }
      
      block.setVisited();
      
      // Add the block to the list of split points.
      
      if (block.numOutEdges() > 1) {
        assert(!splitPoints.contains(block));
        splitPoints.add(block);
      }
      
      // Compute the store ids coming out of this block
      
      BitVect out    = new BitVect();
      int     blabel = block.getLabel();
      
      for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        if (inst.isStore()) {
          int sid = ((StoreInstruction) inst).getLSQid();
          out.set(sid);
        }
      }
      
      storeIdsIn.put(blabel, in);
      out.or(in);
      storeIdsOut.put(blabel, out);
      
      // Put the in edges on the worklist
      
      int l = block.numInEdges();
      for (int i = 0; i < l; i++) {
        PredicateBlock pred = (PredicateBlock) block.getInEdge(i);
        assert(!pred.visited());
        if (!wl.contains(pred))
          wl.add(pred);
      } 
    }
    
    // Look at the split points to determine what ids are missing
    
    while (!splitPoints.isEmpty()) {
      PredicateBlock split = (PredicateBlock) splitPoints.pop();
      int            sl    = split.numOutEdges();
      BitVect        myids = storeIdsIn.get(split.getLabel());
     
      // my preds store ids out - my store ids in = missing ids on that path
      
      for (int i = 0; i < sl; i++) {
        PredicateBlock succ  = (PredicateBlock) split.getOutEdge(i);
        BitVect        pout  = storeIdsOut.get(succ.getLabel()); 
        BitVect        nulls = myids.clone();
        
        nulls.andNot(pout);
        
        if (!nulls.empty())
          succ.nullifyStores(nulls, ssa, regs);
      }
    }
  }
  
  /**
   * If load and store instructions cannot be predicated this 
   * method ensures that atleast one of the instructions in 
   * the dependence chain is predicated.
   * <br>
   * For loads we insert a predicated mov as the only use of
   * the load. For stores we search up the dependence chain
   * looking for a predicate or we predicate one of the
   * store's operands.
   */
  protected void noLoadStorePredicateField()
  {
    Vector mem = new Vector();
    Stack  wl  = new Stack();
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    // find all the load and store instructions
    // and remove any predicates
   
    while (!wl.isEmpty()) {
      PredicateBlock pb = (PredicateBlock) wl.pop();
      if (!pb.isPredicated())
        continue;
      
      for (Instruction inst = pb.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        if (inst.isLoad() || inst.isStore()) {
          mem.add(inst);
          mem.add(pb);
          ((TripsInstruction) inst).removePredicates();
        }
      }
    }
    
    // check the dependence chain to make sure it is predicated
    // correctly, adding predicates as necessary
    
    int ml = mem.size();
    for (int i = 0; i < ml; i = i+2) {
      Instruction    inst = (Instruction) mem.get(i);
      PredicateBlock pb   = (PredicateBlock) mem.get(i+1);
      
      // for loads we are going to stick in a mov and forget about it!
      
      if (inst.isLoad()) {
        int                ra  = inst.getDestRegister();
        int                tr  = regs.newTempRegister(RegisterSet.AFIREG);
        int                pr  = pb.getPredicate();
        GeneralInstruction mov = new GeneralInstruction(Opcodes.MOV, tr, ra, pr, pb.isPredicatedOnTrue());
        
        inst.remapSrcRegister(ra, tr);
        pb.insertInstructionAfter(inst, mov);
        
        if (ssa != null) {
          ssa.setDef(mov, tr);
          ssa.addUse(mov, ra);
          ssa.addUse(mov, pr);
          ssa.removeUse(inst, ra);
          ssa.addUse(inst, tr);
        }
      }
     
      // for stores we are going to look at the operands up until a phi
      // if any of the operands are predicated we are done. 
      
      if (inst.isStore()) {
        boolean             done   = false;
        IntMap<Instruction> useDef = ssa.getUseDef();
        
        wl.push(inst);
        
        while (!wl.isEmpty()) {
          Instruction use = (Instruction) wl.pop();
          if (use.isPhi()) {
            continue;
          } else if (use.isPredicated()) {
            done = true;
            break;
          } else {
            int[] srcs = use.getSrcRegisters();
            if (srcs != null) {
              for (int j = 0; i < srcs.length; j++) {
                wl.push(useDef.get(srcs[j]));
              }
            }
          }
        }
        
        if (done)
          continue;
        
        // One of the stores operands needs to be predicated
        // for now just stick in a predicated mov. 
        
        int                rb  = ((StoreInstruction) inst).getRb();
        int                tr  = regs.newTempRegister(RegisterSet.AFIREG);
        int                pr  = pb.getPredicate();
        GeneralInstruction mov = new GeneralInstruction(Opcodes.MOV, tr, rb, pr, pb.isPredicatedOnTrue());
        
        inst.remapSrcRegister(rb, tr);
        pb.insertInstructionAfter(inst, mov);
        
        if (ssa != null) {
          ssa.setDef(mov, tr);
          ssa.addUse(mov, rb);
          ssa.addUse(mov, pr);
          ssa.removeUse(inst, rb);
          ssa.addUse(inst, tr);
        }
      }
    }
  }
  
  /**
   * Merge instructions.
   */
  protected void mergeInstructions(boolean includeBranches)
  {
    Vector[] insts = partitionByOpcode();
    mergeInstructionsHoist(insts, includeBranches);

    Vector[] insts_t = new Vector[insts.length];
    Vector[] insts_f = new Vector[insts.length];
    
    partitionBySense(insts, insts_t, insts_f);
    mergeInstructionsSense(insts_t, includeBranches);
    mergeInstructionsSense(insts_f, includeBranches);
  }
  
  /**
   * Partition the instructions by opcode.  The vector contains
   * [instruction, block] pairs where block is the block the
   * instruction is defined in.
   */
  private Vector[] partitionByOpcode()
  {
    Stack<Node> wl    = WorkArea.<Node>getStack("partitionByOpcode");
    Vector[]    insts = new Vector[Opcodes._LAST];
    
    head.nextVisit();
    head.setVisited();
    wl.add(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (!inst.isPredicated())
          continue;
          
        int            opcode = inst.getOpcode(); 
        @SuppressWarnings("unchecked")
        Vector<Object> v      = (Vector<Object>) insts[opcode];
        if (v == null) {
          v = new Vector<Object>();
          insts[opcode] = v;
        }
        v.add(inst);
        v.add(block);
      }
    }
    
    WorkArea.<Node>returnStack(wl);
    
    return compactPartition(insts);
  }

  /**
   * Remove empty entries in a partition.
   */
  private Vector[] compactPartition(Vector[] insts)
  {
    int len = 0;
    
    for (int i = 0; i < insts.length; i++) {
      @SuppressWarnings("unchecked")
      Vector<Object> v = (Vector<Object>) insts[i];
      if (v != null)
        len++;
    }
    
    Vector[] packed = new Vector[len];
    
    for (int i = 0, x = 0; i < insts.length; i++) {
      @SuppressWarnings("unchecked")
      Vector<Object> v = (Vector<Object>) insts[i];
      if (v != null)
        packed[x++] = v;
    }
    
    return packed;
  }
  
  /**
   * Partition instructions by sense (predicated on true or false).
   * <br>
   * The vector contains [instruction, block] pairs where block is the block
   * the instruction is defined in.
   */
  private void partitionBySense(Vector[] insts, Vector[] insts_t, Vector[] insts_f)
  {
    for (int i = 0; i < insts.length; i++) {
      @SuppressWarnings("unchecked")
      Vector<Object> v  = (Vector<Object>) insts[i];      
      int            vl = v.size();
      
      for (int j = 0; j < vl; j += 2) {
        Instruction    inst    = (Instruction) v.get(j);
        PredicateBlock block   = (PredicateBlock) v.get(j+1);
        Vector[]       insts_x = inst.isPredicatedOnTrue() ? insts_t : insts_f;
        @SuppressWarnings("unchecked")
        Vector<Object> v1      = insts_x[i];
        
        if (v1 == null) {
          v1 = new Vector<Object>();
          insts_x[i] = v1;
        }
        
        v1.add(inst);
        v1.add(block);
      }
    }
  }
  
  /**
   * Merge two instructions by hoisting.
   */
  private void mergeInstructionsHoist(Vector[] insts, boolean includeBranches)
  {
    for (int i = 0; i < insts.length; i++) {
      @SuppressWarnings("unchecked")
      Vector<Object> v  = insts[i];
      int            vl = v.size();
      
      // Compare the last instruction to every other instruction.
      // Only two instructions can ever match for hoisting.
      
      for (int j = vl - 2; j > -1; j -= 2) {
        Instruction    inst1 = (Instruction) v.get(j);
        PredicateBlock b1    = (PredicateBlock) v.get(j+1);
        
        for (int k = 0; k < j; k += 2) {
          Instruction    inst2 = (Instruction) v.get(k);
          PredicateBlock b2    = (PredicateBlock) v.get(k+1);
          
          if (!equivalent(inst1, inst2, true, includeBranches))
            continue;
          
          //System.out.println("are equal " + inst1 + " " + inst2);
            
          PredicateBlock split = findHoistPoint(b1, b2);
          Instruction    hinst = hoist(inst1, b1, inst2, b2, split);
            
          instructionsMergedCount++;
            
          // Remove inst1 from the end.
                
          v.remove(j+1);
          v.remove(j);
                
          // Replace inst2 with the hoisted instruction.
          // There may be further matches now.
            
          v.setElementAt(hinst, k);
          v.setElementAt(split, k+1);
            
          // Start over.
          
          j = v.size(); 
          break;
        }
      }
    }
  }
    
  /**
   * Merge instructions that are predicated on the same condition
   * (i.e. on true or on false).  Merge any instructions that have the
   * same operands but non-matching predicates.  The instructions have
   * to be predicated on the same sense.
   * <pre>
   * i.e. add_t<p100> t1, t2, t3  ->   add_t<p100, p200> t1, t2, t3
   *      add_t<p200> t1, t2, t3  
   * </pre> 
   */
  private void mergeInstructionsSense(Vector[] insts, boolean includeBranches)
  {
    Vector<Object> equiv = new Vector<Object>();
    
    for (int i = 0; i < insts.length; i++) {
      @SuppressWarnings("unchecked")
      Vector<Object> v = insts[i];
      if (v == null)
        continue;
      
      int vl = v.size();
      if (vl < 4)
        continue;
      
      for (int j = vl - 2; j > -1; j -= 2) {
        PredicateBlock b1    = (PredicateBlock) v.remove(j+1);
        Instruction    inst1 = (Instruction) v.remove(j);
        
        equiv.clear();
        equiv.add(inst1);
        equiv.add(b1);
        
        // Determine the equivalent instructions.
        
        for (int k = 0; k < j; k += 2) {
          Instruction    inst2 = (Instruction) v.get(k);
          PredicateBlock b2    = (PredicateBlock) v.get(k+1);
          
          if (!equivalent(inst1, inst2, false, includeBranches))
            continue;
          
          equiv.add(inst2);
          equiv.add(b2);
            
          v.remove(k+1);
          v.remove(k);
            
          j -= 2;
          k -= 2;
        }
        
        // Merge the equivalent instructions.
        
        if (equiv.size() > 2)
          mergeEquiv(equiv);
      }
    }
  }
  
  /**
   * Merge equivalent instructions (different predicates, same condition).
   */
  private void mergeEquiv(Vector<Object> equiv)
  {
    // Create the new instruction.

    int         el     = equiv.size();
    Instruction inst   = ((Instruction) equiv.get(0)).copy();
    boolean     onTrue = inst.isPredicatedOnTrue();
    int[]       preds  = new int[el / 2];
    
    for (int i = 0, j = 0; i < el; i += 2)
      preds[j++] = ((Instruction) equiv.get(i)).getPredicate(0);
    
    inst.setPredicates(preds);
    inst.setNext(null);
    
    // Find the block for the new instruction.
    
    PredicateBlock block = mergeFindBlock(preds, onTrue);
    if (block == null)
      block = mergeCreateBlock(equiv, preds, onTrue);
    
    if (block == null)
      return;  // Give up
      
    block.appendInstruction(inst);
    
    // Remove the original instructions.

    for (int i = 0; i < el; i += 2) {
      Instruction    inst1  = (Instruction) equiv.get(i);
      PredicateBlock b1     = (PredicateBlock) equiv.get(i+1);
      Instruction    prev   = null;

      for (Instruction inst2 = b1.getFirstInstruction();
           inst2 != null;
           prev = inst2, inst2 = inst2.getNext()) {
        if (inst2 == inst1) {
          b1.removeInstruction(prev, inst1);
          break;
        }
      }
    }
    
    instructionsMergedCount += el / 2;
  }
  
  /**
   * Create and insert a new block in the PFG during instruction merging.
   */
  private PredicateBlock mergeCreateBlock(Vector<Object> equiv, int[] preds, boolean onTrue)
  {
    PredicateBlock          block = new PredicateBlock(preds, onTrue);
    Vector<PredicateBlock>  succs = new Vector<PredicateBlock>();
    int                     el    = equiv.size();
    
    // Get the out-edges for each block with an instruction to be merged.
    
    for (int i = 0; i < el; i += 2) {
      PredicateBlock b1 = (PredicateBlock) equiv.get(i+1);
      for (int j = 0; j < b1.numOutEdges(); j++) {
        PredicateBlock b1succ = (PredicateBlock) b1.getOutEdge(j);
        if (!succs.contains(b1succ)) 
          succs.add(b1succ);
      }
    }
    
    // Insert the block into the PFG.
    
    if (succs.size() == 1) { // Every block has the same successor.  
      PredicateBlock succ = succs.get(0);
      block.addOutEdge(succ);
      succ.addInEdge(block);
    
      for (int i = 0; i < el; i += 2) {
        PredicateBlock b1     = (PredicateBlock) equiv.get(i+1);
        PredicateBlock b1succ = (PredicateBlock) b1.getOutEdge(0);
      
        block.addInEdge(b1);
        b1.replaceOutEdge(b1succ, block);
        b1succ.deleteInEdge(b1);
      }
      
      return block; 
    }
    
    // Some blocks have different successors.  This happens when other
    // instruction have already been merged in these blocks.
      
    // Find the block that is the merge point for all the paths.
      
    PredicateBlock merge = findMergeEquiv(succs);
    if (merge == null)
      return null;  // Give up
      
    block.addOutEdge(merge);
    merge.addInEdge(block);
    
    // Link the path from each block to the merge point.
      
    Vector<PredicateBlock> v = new Vector<PredicateBlock>();
    for (int i = 0; i < el; i += 2) {
      PredicateBlock b1 = (PredicateBlock) equiv.get(i+1);
      v.add(b1); 
    }
      
    while (!v.isEmpty()) {
      PredicateBlock b1     = v.remove(0);
      PredicateBlock b1succ = (PredicateBlock) b1.getOutEdge(0);
      
      // The block has an out-edge to the merge point.
      // Link it up and we are done with this block.
        
      if (b1succ == merge) {
        block.addInEdge(b1);
        b1.replaceOutEdge(b1succ, block);
        b1succ.deleteInEdge(b1);
        continue;
      } 
        
      // We have to find the block to link the merge point.
        
      Stack<Node>    wl    = new Stack<Node>();
      PredicateBlock found = null;
        
      b1succ.nextVisit();
      b1succ.setVisited();
      wl.push(b1succ);
        
      while (!wl.isEmpty()) {
        PredicateBlock b2 = (PredicateBlock) wl.pop();
        if (b2.numOutEdges() == 1) {
          if (b2.getOutEdge(0) == merge) {
            found = b2;
            break;
          }
        }
        b2.pushOutEdges(wl);
      }
        
      assert (found != null) : b1;
        
      block.addInEdge(found);
      found.replaceOutEdge(merge, block);
      merge.deleteInEdge(found);
        
      for (int i = 0; i < found.numInEdges(); i++) {
        PredicateBlock in = (PredicateBlock) found.getInEdge(i);
        if (v.contains(in))
          v.remove(in);
      }
    }
    
    return block;
  }
  
  /**
   * Find a predicate block during instruction merging.
   */
  private PredicateBlock mergeFindBlock(int[] preds, boolean isPredicateOnTrue)
  {
    Stack<Node> wl = new Stack<Node>();
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      if (!block.isPredicated())
        continue;
      
      if (block.isPredicatedOnTrue() != isPredicateOnTrue)
        continue;
      
      int[] bp = block.getPredicates();
      if (bp.length != preds.length)
        continue;
      
      boolean found = true;
      for (int i = 0; i < preds.length; i++) {
        int     rp    = preds[i];
        boolean match = false;
        for (int j = 0; j < bp.length; j++) {
          if (rp == bp[i]) {
            match = true;
            break;
          }
        }
        if (!match) {
          found = false;
          break;
        }
      }
      
      if (found)
        return block;
    }
    
    return null;
  }
  
  private PredicateBlock findMergeEquiv(Vector<PredicateBlock> succs)
  {
    Vector<PredicateBlock> candidates = new Vector<PredicateBlock>();      
    
    for (int i = 0; i < succs.size(); i++) {
      PredicateBlock succ  = succs.get(i);
      Stack<Node>    wl    = new Stack<Node>();
        
      succ.nextVisit();
      succ.setVisited();
      wl.push(succ);
      
      while (!wl.isEmpty()) {
        PredicateBlock sb = (PredicateBlock) wl.pop();
        if (sb.numInEdges() > 1) {
          if (!candidates.contains(sb))
            candidates.add(sb);
          break;
        }
        sb.pushOutEdges(wl);
      }
    }

    // Find the merge point reachable from all blocks.
    
    for (int i = candidates.size() - 1; i > -1; i--) {
      PredicateBlock can = candidates.get(i);
      Stack<Node>    wl  = new Stack<Node>();
        
      can.nextVisit();
      can.setVisited();
      can.pushOutEdges(wl);
          
      while (!wl.isEmpty()) {
        PredicateBlock sb = (PredicateBlock) wl.pop();
        if (candidates.contains(sb)) {
          candidates.remove(i);
          break;
        }
        sb.pushOutEdges(wl);
      }
    }
    
    if (candidates.size() == 1)
      return candidates.get(0);
    
    // This is a hack for when the last block in the PFG is the merge point.
    
    for (int i = 0; i < succs.size(); i++) {
      PredicateBlock b = succs.get(i);
      if (b.numOutEdges() > 1) 
        return null;
      if (b.getOutEdge(0) != tail)
        return null;
    }
    
    return tail;
  }
  
  /**
   * Find the split point that dominates the two predicate blocks.
   */
  private PredicateBlock findHoistPoint(PredicateBlock b1, PredicateBlock b2)
  { 
    if ((b1.numInEdges() == 1) && (b2.numInEdges() == 1)) {
      assert (b1.getInEdge(0) == b2.getInEdge(0));
      return (PredicateBlock) b1.getInEdge(0);
    }
    
    PredicateBlock split1 = (PredicateBlock) b1.getInEdge(0);
    PredicateBlock split2 = (PredicateBlock) b2.getInEdge(0);
    
    if (b1.numInEdges() > 1) {
      while (split1 != split2) {
        assert (split1.numInEdges() > 0) : "Couldn't find split.";
        split1 = (PredicateBlock) split1.getInEdge(0);
      }
      return split1;
    }
    
    if (b2.numInEdges() > 1) {
      while (split1 != split2) {
        assert (split2.numInEdges() > 0) : "Couldn't find split.";
        split2 = (PredicateBlock) split2.getInEdge(0);
      }
      return split2;
    }
    
    return null;
  }
  
  /**
   * Hoist an instruction.
   */
  private Instruction hoist(Instruction    inst1,
                            PredicateBlock b1,
                            Instruction    inst2,
                            PredicateBlock b2,
                            PredicateBlock split)
  { 
    // Remove instruction-1 from block b1

    Instruction prev = null;
    for (Instruction inst = b1.getFirstInstruction();
         inst != null;
         prev = inst, inst = inst.getNext()) {
      if (inst == inst1) {
        b1.removeInstruction(prev, inst1);
        break;
      }
    }
    
    // Remove instruction-2 from block b2
    
    prev = null;
    for (Instruction inst = b2.getFirstInstruction();
         inst != null;
         prev = inst, inst = inst.getNext()) {
      if (inst == inst2) {
        b2.removeInstruction(prev, inst2);
        break;
      }
    }
    
    // Insert the merged instruction at the split point
    
    if (split.isPredicated()) 
      inst1.setPredicate(split.getPredicate(), split.isPredicatedOnTrue());
    else
      inst1.removePredicates();
    
    inst1.setNext(null);
    split.appendInstruction(inst1);
    
    return inst1;
  }

  /**
   * Return true if two instructions are equivalent.
   */
  private boolean equivalent(Instruction inst1,
                             Instruction inst2,
                             boolean     comparePredicates,
                             boolean     includeBranches)
  { 
    if (inst1.isBranch() && !includeBranches)
      return false;
      
    int ra1 = inst1.getDestRegister();
    int rb2 = inst2.getDestRegister();
    if (ra1 != rb2)
      return false;
      
    if (comparePredicates) {
      if (!inst1.isPredicated())  
        return false;
      if (!inst2.isPredicated())
        return false;
      int rp1 = inst1.getPredicate(0);
      int rp2 = inst2.getPredicate(0);
      if (rp1 != rp2)
        return false;
    } 
    
    int hash1 = inst1.ehash();
    int hash2 = inst2.ehash();
    if ((hash1 == -1) || (hash2 == -1) || (hash1 != hash2))
      return false;
    
    return true;
  }
  
  /**
   * Remove redundant loads within a predicate block.  If a load is
   * identified as redundant, and there is no intervening store to the
   * same memory location, we can remove the redundant load. 
   * <br>
   * This routine assumes that register allocation has been done and 
   * all displacements are known.
   */
  public void removeRedundantLoads()
  {
    if (!enableRedundantLoadRemoval)
      return;
    
    Stack<Node>                  wl     = new Stack<Node>();
    Vector<Instruction>          memops = new Vector<Instruction>();
    HashMap<Instruction, MemRef> refs   = new HashMap<Instruction, MemRef>();   

    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      memops.clear();
      
      // Find all the load and store instructions that are candidates
      // for optimization<Node>
      
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.isLoad()) {
          if (((LoadInstruction) inst).isMandatory())
            continue;
          if (((LoadInstruction) inst).getDisp() != null)
            continue;
          memops.add(inst);
        } else if (inst.isStore()) {
          StoreInstruction st = (StoreInstruction) inst;
          memops.add(inst);  
        }
      }
     
      // Need at least two memory ops
      
      int ml = memops.size();
      if (ml < 2)
        continue;
      
      // Compute the location referenced by each instruction
      
      refs.clear();
      
      for (int i = 0; i < ml; i++) {
        Instruction inst = memops.get(i);
        MemRef      ref  = computeRef(inst);
        assert ref != null;
        refs.put(inst, ref);
      }
      
      // Find loads that can be removed
      
      for (int i = ml - 1; i > 0; i--) {
        Instruction ls1 = memops.get(i);
        if (ls1.isStore())
          continue;
        
        // Find a previous instruction that could replace this load
        
        MemRef      mr1 = refs.get(ls1);  
        Instruction lsc = null; 
        int         lsx = 0;
        
        for (int j = i - 1; j >= 0; j--) {
          Instruction ls2 = memops.get(j);
          MemRef      mr2 = refs.get(ls2);
          
          if ((mr2.disp != null) 
              && (mr1.disp != null) 
              && mr2.disp.isSymbol() 
              && mr1.disp.isSymbol()) {
            SymbolDisplacement sd2 = (SymbolDisplacement) mr2.disp;
            SymbolDisplacement sd1 = (SymbolDisplacement) mr1.disp;
            if (!sd2.getName().equals(sd1.getName()))
              continue;
          } else if (mr2.disp != mr1.disp)
            continue;
          
          if ((mr2.breg == mr1.breg) 
              && (mr2.off == mr1.off)
              && (mr2.sz >= mr1.sz)) {
            lsc = ls2;
            lsx = j;
            break;
          }
        }
        
        // TODO - See if the two loads can be combined into a single load
        
        if (lsc == null)
          continue;
          
        // Found a redundant load. Check if there is an intervening store
        // that would inhibit removing the load.
          
        for (int k = lsx + 1; k < i; k++) {
          Instruction ls3 = memops.get(k);
          if (ls3.isLoad())
            continue;
            
          // If the memory locations overlap, or if we cannot determine
          // if they overlap, we cannot replace the load.
 
          MemRef mr3 = refs.get(ls3);
          if (mr3.overlaps(mr1)) {
            lsc = null;
            break;
          }
        }
         
        if (lsc != null)
          replaceLoad(ls1, lsc, block);
      }
    }
  }
  
  /*
   * Replace the load instruction 'ls1' with the load/store instruction 'lsc'. 
   */
  private void replaceLoad(Instruction ls1, Instruction lsc, PredicateBlock block)
  {
    //System.out.println("*** remove: " + ls1 + " " + lsc);
    
    int r1  = (lsc.isLoad() ?
               ((LoadInstruction) lsc).getRa() :
               ((StoreInstruction) lsc).getRc());        
    int op1 = ls1.getOpcode();
    int opc = lsc.getOpcode();

    // Extract the value and sign extend if necessary.
    Instruction after = lsc;
    
    if (opc != op1) {
      int     x1 = 0;
      int     xc = 0;
      int     oe = -1;
      boolean eu = false;  
      
      switch (opc) {
        case Opcodes.SD:
          eu = true;
          xc = 64;
          break;
        case Opcodes.LD:
          xc = 64;
          break;
        case Opcodes.LWS:
        case Opcodes.SW:
          eu = true;
          xc = 32;
          break;
        case Opcodes.LW:
          xc = 32;
          break;
        case Opcodes.LHS:
        case Opcodes.SH:
          eu = true;
          xc = 16;
          break;
        case Opcodes.LH:
          xc = 16;
          break;
        case Opcodes.LBS:
        case Opcodes.SB:
          eu = true;
          xc = 8;
          break;
        case Opcodes.LB:
          xc = 8;
          break;
      }
      
      switch (op1) {
        case Opcodes.LD:
          x1 = 64;
          break;
        case Opcodes.LWS:
          oe = Opcodes.EXTSW;
          x1 = 32;
          break;
        case Opcodes.LW:
          if (eu)
            oe = Opcodes.EXTUW;
          x1 = 32;
          break;
        case Opcodes.LHS:
          oe = Opcodes.EXTSH;
          x1 = 16;
          break;
        case Opcodes.LH:
          if (eu)
            oe = Opcodes.EXTUH;
          x1 = 16;
          break;
        case Opcodes.LBS:
          oe = Opcodes.EXTSB;
          x1 = 8;
          break;
        case Opcodes.LB:
          if (eu)
            oe = Opcodes.EXTUB;
          x1 = 8;
          break;
      }
      
      // Extract the value.
      
      int         shift = xc - x1;
      if (shift > 0) {
        int         nr   = regs.newTempRegister(RegisterSet.AFIREG);
        Instruction srli = new ImmediateInstruction(Opcodes.SRLI, nr, r1, shift);
        
        block.insertInstructionAfter(after, srli);
        ssa.setDef(srli, nr);
        ssa.addUse(srli, r1);
        r1 = nr;
        after = srli;
      }
      
      // Sign extend the value.
  
      if (oe > -1) {
        int         nr   = regs.newTempRegister(RegisterSet.AFIREG);
        Instruction sext = new GeneralInstruction(oe, nr, r1);
        
        block.insertInstructionAfter(after, sext);
        ssa.setDef(sext, nr);
        ssa.addUse(sext, r1);
        r1 = nr;
        after = sext;
      }
    }
    
    // Insert a mov. This is to avoid the problem were the redundant load
    // is the only instruction to define a register used by a write:
    //
    //   read  t1, g100        read t1, g100
    //   sd    mem, t1         sd   mem, t1
    //   ld    t2, mem   ==>   nop
    //   add   t3, t2          add t3, t1
    //   ...                   ...
    //   write g50, t2         write g50, t1
    //
    // There is no real instruction that defines t1, so we will lose the
    // write to g50 because there is no def to rename to g50.
    
    int                ra1 = ((LoadInstruction) ls1).getRa();
    GeneralInstruction mov = new GeneralInstruction(Opcodes.MOV, ra1, r1);
    
    block.insertInstructionAfter(after, mov);
    if (block.isPredicated()) {
      mov.setPredicate(block.getPredicate());
      mov.setPredicatedOnTrue(block.isPredicatedOnTrue());
    }
    
    block.removeInstruction(ls1);
    
    ssa.addUse(mov, r1);
    ssa.setDef(mov, ra1);
  }
  
  private static class MemRef 
  {
    Displacement disp;  
    long         off;
    int          sz;
    int          breg;  // The base register (if displacement is unknown) 

    boolean overlaps(MemRef ref)
    {
      if (breg != ref.breg)
        return true;  
      if (disp != ref.disp)
        return false;
      if ((off >= ref.off) && (off < ref.off + ref.sz))
        return true;
      if ((ref.off >= off) && (ref.off < off + sz))
        return true;
      return false;
    }
    
    public String toString() 
    {
      if (disp == null)
        return "breg: " + breg + " off: " + off; 
      else 
        return "breg: " + breg + " off: " + off + disp.toString();
    }
  }
  
  /**
   * Determine the location for a memory instruction.
   */
  private MemRef computeRef(Instruction inst) 
  {
    int  rb;
    long imm;

    if (inst.isLoad()) {
      LoadInstruction   ld   = (LoadInstruction) inst;
      StackDisplacement disp = (StackDisplacement) ld.getDisp();
      if (disp != null) {
    	imm = disp.getDisplacement();
      } else
        imm = ld.getImm();
      rb  = ld.getRb();
    } else {
      StoreInstruction  sd   = (StoreInstruction) inst;
      StackDisplacement disp = (StackDisplacement) sd.getDisp();
      if (disp != null) {
        imm = disp.getDisplacement();
      } else 
        imm = sd.getImm();
      rb  = sd.getRb();
    }
     
    // Determine the base address and offset
  
    MemRef              ref    = new MemRef();
    IntMap<Instruction> useDef = ssa.getUseDef();
    Instruction         def    = useDef.get(rb);
    
    // If a memory displacement and offset is not found,
    // set the memory reference to be the address register
    // and offset of the instruction.
    
    if (!computeBase(def, ref)) {
      ref.disp = null;
      ref.off  = imm;
      ref.breg = rb;
    } else 
      ref.off += imm;
    
    // Determine the size of the memory op
    
    switch (inst.getOpcode()) {
      case Opcodes.LD:
      case Opcodes.SD:
        ref.sz = 8;
        break;
      case Opcodes.LW:
      case Opcodes.LWS:
      case Opcodes.SW:
        ref.sz = 4;
        break;
      case Opcodes.LH:
      case Opcodes.LHS:
      case Opcodes.SH:
        ref.sz = 2;
        break;
      case Opcodes.LB:
      case Opcodes.LBS:
      case Opcodes.SB:
        ref.sz = 1;
        break;
    }
    
    return ref;
  }
  
  /**
   * Compute the base displacement and offset.
   */
  private boolean computeBase(Instruction inst, MemRef ref) 
  {
    IntMap<Instruction> useDef = ssa.getUseDef();
    int                 op     = inst.getOpcode();
    
    if (op == Opcodes.ADDI) {
      // StackDisplacements are always off the stack pointer so 
      // we don't need to check the source register.
      
      Displacement disp = ((ImmediateInstruction) inst).getDisp();
      if (disp != null) {
        assert disp.isStack();
        ref.disp = disp;
        return true;
      }
      
      ref.off += ((ImmediateInstruction) inst).getImm();
      
      int         r1  = ((ImmediateInstruction) inst).getRb();
      Instruction in1 = useDef.get(r1);
      return computeBase(in1, ref);
    } else if (op == Opcodes._ENTERA) {
      ref.disp = ((EnterInstruction) inst).getDisp();
      return true;
    }
  
    return false;
  }
  
  /**
   * Propogate a "value number" and return true if the redundant instruction can be removed.
   * <br>
   * We cannot remove an instruction that has a use that is:
   * <br>
   *    i. a write instruction, or
   *   ii. a phi that has a use that is a write instruction
   * <br>
   * This is because we won't be able to remove read/write instructions
   * when leaving SSA form.  See the comments in the SSA class for copy
   * folding for more information.
   */
  private boolean valueNumberPropogate(Instruction inst, int val)
  {
    boolean                      remove = true;
    IntMap<Vector<Instruction>>  defUse = ssa.getDefUse();
    int                          id     = inst.getDestRegister();
    Vector<Instruction>          uses   = defUse.get(id);
    int                          ul     = uses.size();
    
    for (int i = ul-1; i > -1; i--) {
      Instruction use    = uses.get(i);
      int         opcode = use.getOpcode();
      
      if (opcode == Opcodes.WRITE) {
        remove = false;
        continue;
      }
      
      if (use.isPhi()) {
        Vector<Instruction>  pu    = defUse.get(use.getDestRegister());
        int                  pl    = pu.size();
        boolean              write = false;
        
        for (int j = 0; j < pl; j++) {
          Instruction puse = pu.get(j);
          if (puse.getOpcode() == Opcodes.WRITE) {
            write = true;
            break;
          }
        }
        
        if (write) {
          remove = false;
          continue;
        }
      }
      
      use.remapSrcRegister(id, val);
      uses.remove(i);
      ssa.addUse(use, val);
    }
    
    return remove;
  }
  
  /**
   * Return true if an instruction can be removed after value numbering.
   */
  private boolean valueNumberInst(Instruction inst, Stack<Instruction> vlocal, IntMap<Instruction> vnum)
  { 
    int val = -1;
    
    if (inst.isPhi()) {
      int[] operands = ((PhiInstruction) inst).getOperands();
      int   op       = operands[0];    
      for (int i = 1; i < operands.length; i++) {
        if (op != operands[i])
          return false;
      }
      val = op;
    } else {
      int hash = inst.ehash();
      if (hash == -1)
        return false;
      
      Instruction expr = vnum.get(hash);
      if (expr == null) {
        vnum.put(hash, inst);
        vlocal.add(inst);
        return false;
      }
   
      val = expr.getDestRegister();
    }
    
    //System.out.println("vn: " + inst + " -> " + val);
    
    return valueNumberPropogate(inst, val);
  }
  
  /**
   * Perform copy propagation in SSA. 
   */
  public void propagateCopies()
  {
    assert(ssa != null) : "Must be in SSA to use foldCopies";
    
    Stack<Node>                 wl     = WorkArea.<Node>getStack("Hyperblock.foldCopies");
    IntMap<Vector<Instruction>> defuse = ssa.getDefUse();
    IntMap<Instruction>         usedef = ssa.getUseDef();
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.empty()) {
      PredicateBlock pb = (PredicateBlock) wl.pop();
      pb.pushOutEdges(wl);
      Instruction inst = pb.getFirstInstruction();
      
      while (inst != null) {
        boolean     fold = false;
        
        // Attempt to propagate copy instructions.
        if (inst.isCopy()) {
          int dest         = inst.getCopyDest();
          int src          = inst.getCopySrc();
          Instruction def  = usedef.get(src);

          // Don't propagate copies that define predicates,
          // or that forward reads to writes
          fold = true;
          if (((TripsInstruction)inst).definesPredicate()) {
            fold = false;
          } else if (inst.isPredicated()) {
            fold = false;
          } else if (def.getOpcode() == Opcodes.READ) {
            fold = false;
          } else {
            Vector<Instruction> uses = defuse.get(dest);
            for (int i = 0; i < uses.size(); i++) {
              Instruction use = uses.elementAt(i);
              if (use.getOpcode() == Opcodes.WRITE) {
                fold = false;
              }
            }
          }
          
          // Propagate the copy.
          if (fold) {
            Vector<Instruction> uses = defuse.get(dest).clone();
            for (int i = 0; i < uses.size(); i++) {
              Instruction use = uses.elementAt(i);
              use.remapSrcRegister(dest, src);
              ssa.removeUse(use, dest);
              ssa.addUse(use, src);
            }
          }
        }

        inst = inst.getNext();
      }
    }
  }
  
  /**
   * Dominator based value-numbering.
   * <p>
   * Our implementation of value numbering differs from L. Taylor
   * Simpson's implementation.  When we find an instruction that is
   * redundant, we propogate the "value number" to the uses of the
   * redundant instruction and update the use-def and def-use
   * information.
   * <p>
   * The benefit of our implementation is we only need to keep a hash
   * of expressions.  We don't need to maintain a table of value
   * numbers for expressions, and we don't need to seperately update
   * phi nodes in successor blocks.
   * <p>
   * Load's are handled conservately.  We traverse the dominator tree
   * in reverse post order, removing redundant loads until we find a
   * store instruction.  As soon as we encounter a store, we stop
   * value numbering loads.
   */
  @SuppressWarnings("unchecked")
  private void valueNumber()
  {
    IntMap<Instruction> vnum     = new IntMap<Instruction>(37);
    Stack<Object>       wl       = WorkArea.<Object>getStack("trips valueNumber");
    Domination          dom      = getDomination();
    boolean             mayAlias = false;   
    Stack<Instruction>  vlocal   = null;

    wl.push(head);
    
    while (!wl.isEmpty()) {
      Object o = wl.pop();
      
      // Remove the previous hashed local expressions.
      
      if (o instanceof Stack) {
        vlocal = (Stack<Instruction>) o;
        while (!vlocal.isEmpty()) {
          Instruction inst = vlocal.pop();
          vnum.put(inst.ehash(), null);
        }
        continue;  
      }
      
      // Create a stack to hold the local expressions.
    
      vlocal = new Stack<Instruction>();
      wl.push(vlocal);
      
      // Value number each instruction.
      
      PredicateBlock block = (PredicateBlock) o;
      Instruction    prev  = null;
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        int opcode = inst.getOpcode();
        
        if (inst.isMarker() 
            || inst.isBranch() 
            || ((TripsInstruction) inst).definesPredicate()
            || (opcode == Opcodes.READ) 
            || (opcode == Opcodes.WRITE)
            || (opcode == Opcodes.MOV)
            || (opcode == Opcodes.NULL)
            || (inst.isLoad() && (mayAlias || ((LoadInstruction) inst).isMandatory()))) {
          prev = inst;
          continue;
        }
        
        if (inst.isStore()) {
          mayAlias = true;
          prev = inst;
          continue;
        }
        
        if (valueNumberInst(inst, vlocal, vnum)) {
          block.removeInstruction(prev, inst);
          valueNumberCount++;
        } else
          prev = inst;
      }

      // Push all the children in the dominator tree in reverse post-order.
      
      dom.pushDominatees(block, wl);
    }
    
    WorkArea.<Object>returnStack(wl);
  }
  
  /**
   * Remove predicates so that only the top of a dependence chain is predicated.
   */
  private void predicateTopOfDependenceChains()
  {
    Stack<Node>         wl     = WorkArea.<Node>getStack("predicateTopOfDependenceChains");
    Stack<Instruction>  wl2    = WorkArea.<Instruction>getStack("predicateTopOfDependenceChains"); // Instructions to unpredicate
    IntMap<Instruction> useDef = ssa.getUseDef();
    
    // Find the instructions that can be unpredicated.
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      if (!b.isPredicated())
        continue;

      for (Instruction inst = b.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (!inst.isPredicated())
          continue;
        if (inst.isPhi())
          continue;
        int[] srcs = inst.getSrcRegisters();
        if (srcs == null)
          continue;
        
        for (int i = 0; i < srcs.length; i++) {
          int         rb  = srcs[i];
          Instruction def = useDef.get(rb);
          if (def == null)
            continue;
          if (!def.isPredicated())
              continue;
          if (((TripsInstruction) def).definesPredicate())
            continue;
          if (inst.isPredicatedOnTrue() != def.isPredicatedOnTrue())
            continue;
          if (inst.getPredicate(0) != def.getPredicate(0))
            continue;
          
          wl2.add(inst);  
          break;
        }
      }
    }
    
    // Remove the predicates.
    
    while (!wl2.isEmpty()) {
      TripsInstruction inst = (TripsInstruction) wl2.pop();
      inst.removePredicates();
    }
    
    WorkArea.<Node>returnStack(wl);
    WorkArea.<Instruction>returnStack(wl2);
  }
  
  /**
   * Remove predicates so that only the bottom of a dependence chain
   * is predicated.
   */
  private void predicateBottomOfDependenceChains()
  {
    if (head.numOutEdges() == 0)
      return;
    
    // Make sure the ids are assigned before unpredicating loads.
    
    if (unpredicateLoads)
      assignLoadStoreQueueIds();

    Stack<Node> wl = WorkArea.<Node>getStack("predicateBottomOfDependenceChains");
    int[] lids     = null;
    
    // Determine the number of loads for a lsq id.
    
    if (unpredicateLoads) {
      lids = new int[Trips2Machine.maxLSQEntries * 2];  
      
      head.nextVisit();
      head.setVisited();
      wl.push(head);
      
      while (!wl.isEmpty()) {
        PredicateBlock b = (PredicateBlock) wl.pop();
        b.pushOutEdges(wl);
        
        for (Instruction inst = b.getFirstInstruction();
             inst != null;
             inst = inst.getNext()) {
          if (!inst.isLoad())
            continue;
          int id = ((LoadInstruction) inst).getLSQid();
          lids[id]++;
        }
      }
    }
    
    // Remove predicates.
    
    IntMap<Vector<Instruction>> defUse = ssa.getDefUse();
    Stack<Instruction>          uses   = WorkArea.<Instruction>getStack("predicateBottomOfDependenceChains");
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
      
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
        
      for (Instruction inst = b.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.isMarker()
            || !inst.isPredicated()
            || inst.isBranch()
            || inst.isStore()
            || inst.isPhi()
            || ((TripsInstruction) inst).definesPredicate())
          continue;
      
        if (inst.isLoad()) {
          if (!unpredicateLoads)
            continue;
          int id = ((LoadInstruction) inst).getLSQid();
          if (lids[id] > 1)  // This lsq id is being shared.
            continue;
        }

        uses.clear();
        uses.push(inst);
        
        boolean remove = true;
        while (!uses.isEmpty() && remove) {
          Instruction         use = uses.pop();
          int                 ra  = use.getDestRegister();
          Vector<Instruction> v   = defUse.get(ra);

          if (v == null) {
            remove = false;
            break;
          }
          
          int vl = v.size();
          for (int i = 0; i < vl; i++) {
            Instruction i1 = v.elementAt(i);
            if (i1.isPhi()) {
              // Phi removal can rename the instruction and 
              // avoid inserting a mov if we leave the predicate.

              remove = false;
              break;
            } else if (!i1.isPredicated()) {
              uses.add(i1);
            }
          }
        }

        // Try to always unpredicate loads by adding a
        // predicated move to guard the output of the load.

        if (inst.isLoad() && !remove && unpredicateLoads) {
          int ra = inst.getDestRegister();
          int tr = regs.newTempRegister(RegisterSet.AFIREG);

          GeneralInstruction mov = new GeneralInstruction(Opcodes.MOV, 
            ra, tr, inst.getPredicate(0), inst.isPredicatedOnTrue());

          inst.remapDestRegister(ra, tr);
          ssa.setDef(inst, tr);
          ssa.setDef(mov, ra);
          ssa.addUse(mov, tr);

          b.insertInstructionAfter(inst, mov);          
          remove = true;
        }
      
        if (remove)
          ((TripsInstruction) inst).removePredicates();
      }
    }

    WorkArea.<Instruction>returnStack(uses);
    WorkArea.<Node>returnStack(wl);      
  }
  
  /**
   * Determine the instructions that are candidates for inter-block
   * predicate removal.  Returns the instructions that we want to
   * execute unconditionally and their write instructions.
   */
  private Vector<Instruction> interBlockFindCandidateInstructions(DataflowAnalysis df)
  {
    Stack<Node>      wl         = WorkArea.<Node>getStack("interBlockFindCandidateInstructions");
    BitVect          defs       = df.getDef(getTag());
    int              lsize      = liveout.size();
    int[]            candidates = new int[lsize];
    PredicateBlock[] exits      = new PredicateBlock[lsize];
    
    // Any register that is live-out of a single exit
    // is a candidate for optimization.
    
    head.nextVisit();
    head.setVisited();
    wl.push(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      if (block.hasBranch()) {
        BitVect out = df.getBlockOut(block.getTag());
        int[]   set = out.getSetBits();
        for (int i = 0; i < set.length; i++) {
          int reg = set[i];
          if (defs.get(reg)) {
            candidates[reg]++;
            exits[reg] = block;
          }
        }
      }
    }
    
    WorkArea.<Node>returnStack(wl);
        
    // Identify the write instructions for the candidate live-out's
    // and the phi's that define the writes source operands.  
    
    IntMap<Instruction>         useDef = ssa.getUseDef();
    IntMap<Vector<Instruction>> defUse = ssa.getDefUse();
    Vector<Instruction>         phis   = null;
    
    for (Instruction inst = tail.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      int opcode = inst.getOpcode();
      if (opcode != Opcodes.WRITE)
        continue;
      
      int ra = ((GeneralInstruction) inst).getRa();
      if (candidates[ra] != 1)
        continue;
              
      int         rb  = ((GeneralInstruction) inst).getRb();
      Instruction phi = useDef.get(rb);
      if (!phi.isPhi())
        continue;
        
      // We cannot unconditionaly execute the candidate instructions
      // if their phi's target multiple instructions.  This would
      // cause the additional targets of the phi to also execute.
      
      Vector<Instruction> uses = defUse.get(phi.getDestRegister());
      if (uses.size() > 1)  
        continue;
          
      if (phis == null)
        phis = new Vector<Instruction>();
        
      phis.add(phi);
      phis.add(inst);  // Remember the write.
      
      //System.out.println("*** candidate: " + inst);
    }
    
    if (phis == null)
      return null;
      
    // Find the instructions that we want to execute unconditionally
    // We look at the instructions that define the phi's operands.
    // If there is one, and only one instruction that is not a read
    // or a null, we may be able to execute it unconditionally.
    
    Vector<Instruction> insts  = null;
    int                 pl     = phis.size();
    
    for (int i = 0; i < pl; i += 2) {
      PhiInstruction phi      = (PhiInstruction) phis.elementAt(i);
      Instruction    inst     = null;
      int[]          operands = phi.getOperands();
      
      for (int j = 0; j < operands.length; j++) {
        int         reg    = operands[j];
        Instruction def    = useDef.get(reg);
        int         opcode = def.getOpcode();
        
        // Copies don't matter.
        
        if (opcode == Opcodes.MOV) {
          def = useDef.get(((GeneralInstruction) def).getRb());
          opcode = def.getOpcode();
        }
        
        if ((opcode == Opcodes.READ) || (opcode == Opcodes.NULL))
          continue;
         
        if (inst == null) 
          inst = def;
        else if (inst != def) {
          inst = null;
          break;
        }
      }
            
      if (inst == null)
        continue;
        
      if (insts == null)
        insts = new Vector<Instruction>();
        
      insts.add(inst);
      insts.add(phis.elementAt(i+1));  // Remember the write.
    }
    
    if (insts == null)
      return null;
      
    // The instructions to unpredicate have to dominate their exits
    // or be in the predicate block with the exit.
    
    Stack<Node> wl2 = WorkArea.<Node>getStack("interBlockFindCandidateInstructions");
    Domination  dom = getDomination();
    
    head.nextVisit();
    head.setVisited();
    wl2.push(head);
    
    while (!wl2.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl2.pop();
      block.pushOutEdges(wl2);
    
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        int opcode = inst.getOpcode();
        if (opcode == Opcodes.WRITE)
          continue;
          
        int idx = insts.indexOf(inst);
        if (idx == -1)
          continue;
          
        GeneralInstruction write = (GeneralInstruction) insts.get(idx+1);
        int                ra    = write.getRa();
        PredicateBlock     exit  = exits[ra];
        if (block == exit)
          continue;
        
        Vector<Node> idoms = dom.getIterativeDomination(block);
        if (idoms.contains(exit))
          continue;
      
        insts.remove(idx);  // The def
        insts.remove(idx);  // The write
      }
    }
    
    WorkArea.<Node>returnStack(wl2);
    
    return insts;
  }

  /** 
   * Move the instructions to execute unconditionally to the
   * hyperblock entry.
   */
  private void interBlockMoveInstructions(Vector<Instruction> insts)
  {
    // Remove any instructions that are already in the entry.
     
    for (Instruction inst = head.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (insts.contains(inst))
        insts.remove(inst);
    }
   
    // Remove the instructions from the hyperblock.
                   
    Stack<Node> wl = new Stack<Node>();
     
    head.setVisited();
    head.nextVisit();
    head.pushOutEdges(wl);
     
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);

      Instruction prev = null;
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (insts.contains(inst))
          block.removeInstruction(prev, inst);
         else
          prev = inst;
      }
    }
     
    // Insert the instructions in the entry.
         
    int il = insts.size();
    for (int i = il-1; i > -1; i--) {
      Instruction inst = insts.elementAt(i);
       
      if (inst.isPredicated()) {
        int rp = inst.getPredicate(0);
        ssa.removeUse(inst, rp);
        inst.removePredicates();
      }
       
      inst.setNext(null);
      head.appendInstruction(inst);
    }
  }

  /**
   * Execute an instruction unconditionally.
   */
  private boolean interBlockExecuteUnconditionally(Instruction        candidate,
                                                   GeneralInstruction write,
                                                   Stack<Instruction> uses)
  {
    Vector<Instruction>         insts  = new Vector<Instruction>();
    IntMap<Instruction>         useDef = ssa.getUseDef();
    IntMap<Vector<Instruction>> defUse = ssa.getDefUse();
      
    // Determine the instructions that need to be unpredicated to execute
    // the candidate instruction unconditionally.  
    
    uses.clear();
    uses.push(candidate);
    
    while (!uses.isEmpty()) {
      Instruction use = uses.pop();
      if (use.isPhi())
        return false;
      
      int opcode = use.getOpcode();
      if ((opcode == Opcodes.READ) || (opcode == Opcodes.NULL))
        continue;
      
      // This instruction can only be unpredicated if it targets:
      //    * the candidate instruction, 
      //    * a phi (phi removal will insert a predicated mov), or
      //    * a predicated instruction.
      
      if (use.isPredicated()) {
        if (use.isLoad()) {  
          //System.out.println("TODO - Handle overlapping load ids " + use);
          return false;
        }
        
        int                 ra = use.getDestRegister();
        Vector<Instruction> v  = defUse.get(ra);        
        int                 vl = v.size();
        
        if (vl > 1) {
          for (int i = 0; i < vl; i++) {
            Instruction i2 = v.elementAt(i);
            if (i2 == candidate)
              continue;
            if (i2.isPhi()) { // This could be the phi that targets the original write...
              //System.out.println("Phi will insert a mov" + candidate);
              continue;
            }
            if (!i2.isPredicated()) {
              //System.out.println("Target is not predicated giving up.");
              return false;
            }
          }
        }
      }
      
      // We will move this instruction to the hyperblock entry.
       
      if (!insts.contains(use))
        insts.add(use);

      // Push the instructions that define the source operands,
      // but ignore instructions that define the predicates.
      
      int[] srcs = use.getSrcRegisters();
      if (srcs == null)
        continue;
      
      int rp = use.isPredicated() ? use.getPredicate(0) : -1;
      for (int j = 0; j < srcs.length; j++) {
        int reg = srcs[j];
        if (reg == rp)
          continue;
          
        uses.push(useDef.get(reg));
      }
    }
    
    assert (insts != null);
    
    interBlockMoveInstructions(insts);
    
    // Update the write.  We have to insert a mov instruction so when
    // we leave ssa, the mov is renamed to the live-out and not the
    // candidate instruction that we hoisted.
     
    int tr = regs.newTempRegister(RegisterSet.AFIREG);
    int ra = candidate.getDestRegister();
    int rb = write.getRb();
    
    // Insert the mov after the def. If you put the mov in the special
    // merge for the pfg this will make the temp look live-out of the
    // block! And result in write instructions with operands that don't
    // exist.

    Instruction        def = (Instruction) useDef.get(ra); 
    GeneralInstruction mov = new GeneralInstruction(Opcodes.MOV, tr, ra);

    head.insertInstructionAfter(def, mov);
    write.remapSrcRegister(rb, tr);
     
    ssa.setDef(mov, tr);
    ssa.addUse(mov, ra);
    ssa.removeUse(write, rb);
    ssa.addUse(write, tr);
            
    return true;
  }
  
  /**
   * Perform inter-block predicate minimization.
   * <br>
   * This optimization needs the live-outs for each predicate block.
   */
  protected boolean interBlockPredicateMinimization(DataflowAnalysis df)
  {
    if (!enableInterBlockPredicateMinimization || (df == null))
      return false;
   
    Vector<Instruction> insts = interBlockFindCandidateInstructions(df);
    if (insts == null)
      return false;

    boolean            changed = false;
    int                il      = insts.size();
    Stack<Instruction> wl      = WorkArea.<Instruction>getStack("interBlockPredicateMinimization");
    
    for (int i = 0; i < il; i += 2) {
      Instruction inst  = insts.elementAt(i);
      Instruction write = insts.elementAt(i+1);
      
      changed |= interBlockExecuteUnconditionally(inst, (GeneralInstruction) write, wl);
    }
      
    if (changed)
      ssa.removeDeadCode();
    
    WorkArea.<Instruction>returnStack(wl);

    return changed;
  }
  
  /**
   * Remove predicates from a hyperblock.
   */
  public void removePredicates()
  {
    if (!enableIntraBlockPredicateMinimization)
      return;
      
    // Put the predicate back on every instruction.

    Stack<Node> wl = WorkArea.<Node>getStack("removePredicates");      
          
    head.nextVisit();
    head.setVisited();
    wl.push(head);
   
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
   
      if (!b.isPredicated())
        continue;
      
      for (Instruction inst = b.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        if (inst.isMarker())
          continue;
        if (inst.isPhi())  // Predicates on phis make no sense.
          continue;
        inst.setPredicate(b.getPredicate(), b.isPredicatedOnTrue());
      }
    }

    WorkArea.<Node>returnStack(wl);

    switch (intraBlockDefault) {
      case PREDICATE_TOP:
        predicateTopOfDependenceChains();
        break;
      case PREDICATE_BOTTOM:
        predicateBottomOfDependenceChains();
        break;
    }
  }

  /**
   * Remove spill code from a hyperblock.
   * <br>
   * A hyperblock cannot be split if it contains spill code.
   */
  protected void removeSpillCode()
  {
    Stack<Node>    wl    = WorkArea.<Node>getStack("removeSpillCode");
    PredicateBlock start = getFirstBlock();

    start.nextVisit();
    start.setVisited();
    wl.push(start);

    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      block.removeSpillCode();
    }

    WorkArea.<Node>returnStack(wl);
  }

 /**
   * Remove any dummy stores that were inserted by store nullification.
   */
  private void removeDummyStores()
  {
    PredicateBlock start = getFirstBlock();
    Stack<Node>    wl    = WorkArea.<Node>getStack("removeDummyStores");

    start.nextVisit();
    start.setVisited();
    wl.add(start);

    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      if (block.hasDummyStores())
        block.removeDummyStores(ssa);
    }

    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Nullify write instructions.  
   * <br>
   * Run dead code elimination after nullification
   * since it can make read instruction become dead!
   */
  private void nullifyWrites()
  { 
    if (!Trips2Machine.nullifyWrites)
      return;

    PredicateBlock start = getFirstBlock();
    if (start.numOutEdges() == 0)
      return;

    IntMap<Instruction>         ud = ssa.getUseDef();
    Stack<Node>                 wl = WorkArea.<Node>getStack("nullifyWrites");
    IntMap<Vector<Instruction>> du = ssa.getDefUse();
    
    start.nextVisit();
    start.setVisited();
    start.pushOutEdges(wl);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);
      
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.getOpcode() != Opcodes._PHI)
          continue;
        
        int[] operands = ((PhiInstruction) inst).getOperands();
        for (int i = 0; i < operands.length; i++) {
          Instruction def = ud.get(operands[i]);
          
          // We handle two cases for nullification. The first is when there is an 
          // upwardly exposed use. In this case, we always need to insert a null.
          // i.e.,
          //                              null      t2           
          //   add       t3, ...          add       t3, ...
          //   phi_t<p1> t1, t2, t3  -->  phi_t<p1> t1, t2, t3   
          //                            
          // The second is when there is a phi that is simply forwarding a value
          // between a read and a write and the global registers are the same.
          // This is a specialized case of a more general nullification algorithm
          // that we might implement someday.
          // i.e.,
          //
          //   read      t1, g100         null      t1          
          //   add_f<p1> t3, ...          add_f<p1> t3, ...
          //   phi_f<p2> t7, t1, t3  -->  phi_f<p2> t7, t1, t3
          //   write     g100, t7         write     g100, t7
          
          if (def != null) {
            if (def.getOpcode() != Opcodes.READ)
              continue;
            
            Vector uses = du.get(inst.getDestRegister());
            if (uses == null) // There can be dead phi's before dead code elimination. 
              continue;
            
            if (uses.size() > 1)
              continue;
            
            Instruction write = (Instruction) uses.get(0);
            if (write.getOpcode() != Opcodes.WRITE)
              continue;
            
            if (write.getDestRegister() != ((GeneralInstruction) def).getRb())
              continue;
          }
       
          // Insert a null into the predecessor block.
        
          int                r1   = regs.newTempRegister(RegisterSet.AFIREG); 
          GeneralInstruction ni   = new GeneralInstruction(Opcodes.NULL, r1, operands[i]);
          PredicateBlock     pred = (PredicateBlock) block.getInEdge(i);
          
          ni.setPredicates(pred.getPredicates());
          ni.setPredicatedOnTrue(pred.isPredicatedOnTrue());
          pred.appendInstruction(ni);  
      
          ((PhiInstruction) inst).setOperand(r1, i);
          ssa.setDef(ni, r1);
          ssa.addUse(inst, r1);
          ssa.removeUse(inst, operands[i]);
          ssa.addUse(inst, pred.getPredicate());
        }
      }
    }
    
    WorkArea.<Node>returnStack(wl);
  }
 
  /**
   * Compute the read/write instructions using liveness.
   */
  private void insertReadWriteInstructions()
  {
    Stack<Node>    wl     = WorkArea.<Node>getStack("computeReadWrite");
    PredicateBlock start  = getFirstBlock();
    BitVect        reads  = new BitVect();
    BitVect        writes = new BitVect();
    
    // Compute the reads and writes for this hyperblock.
    
    start.nextVisit();
    start.setVisited();
    wl.add(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      for (Instruction inst = b.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        if (inst.isMarker())
          continue;
        
        int[] srcs = inst.getSrcRegisters();
        if (srcs != null) {
          for (int j = 0; j < srcs.length; j++) {
            int r = srcs[j];
            reads.set(r);
          }
        }
        
        int dest = inst.getDestRegister();
        if (dest != -1) 
          writes.set(dest);
      }
    }    
    
    reads.and(getLiveIn());
    writes.and(getLiveOut());
    
    // If we are not nullifying writes then we also read 
    // in all the writes.

    if (!Trips2Machine.nullifyWrites)
      reads.or(writes);

    WorkArea.<Node>returnStack(wl); 
      
    // Insert read instructions.
    
    Instruction after = start.getFirstInstruction();
    int[]       rd    = reads.getSetBits();
    
    for (int i = 0; i < rd.length; i++) {
      int         reg  = rd[i];
      Instruction inst = new GeneralInstruction(Opcodes.READ, reg, reg);
      
      assert (!isPredicate(reg)) : "Predicate " + reg + " is live-in!";
      
      after = start.insertInstructionAfter(after, inst);
    } 
    
    // Insert write instructions.
 
    PredicateBlock last  = getLastBlock();
    int[]          wt    = writes.getSetBits();
    
    after = last.getLastInstruction();
    
    for (int i = 0; i < wt.length; i++) {
      int         reg  = wt[i];
      Instruction inst = new GeneralInstruction(Opcodes.WRITE, reg, reg);

      assert (!isPredicate(reg)) : "Predicate " + reg + " is live-out!";
      
      if (after == null)
        last.appendInstruction(inst);
      else
        last.insertInstructionAfter(after, inst); 
      
      after = inst;
    } 
  }
    
  /**
   * Remove all read and write instructions in a hyperblock.
   */
  private void removeReadWriteInstructions()
  {
    Stack<Node>    wl    = WorkArea.<Node>getStack("removeReadWriteInstructions");
    PredicateBlock start = getFirstBlock();
    PredicateBlock last  = getLastBlock();
    int[]          map   = new int[regs.numRegisters()];
    
    for (int i = 0; i < map.length; i++)
      map[i] = -1;
    
    // Remove any read instructions.

    Instruction prev0 = null;
    for (Instruction inst = start.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      int opcode = inst.getOpcode();
      if (opcode == Opcodes.READ) {
        GeneralInstruction gen = (GeneralInstruction) inst;
        int                ra  = gen.getRa();
        int                rb  = gen.getRb();
         
        map[ra] = rb;
        start.removeInstruction(prev0, gen);
      } else
        prev0 = inst;
    }    
    
    // Remove any null instructions.
   
    start.nextVisit();
    start.setVisited();
    wl.add(start);

    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop();
      block.pushOutEdges(wl);

      Instruction prev2 = null;
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        int opcode = inst.getOpcode();
        if (opcode == Opcodes.NULL) {
          GeneralInstruction gen = (GeneralInstruction) inst;
          int                ra  = gen.getRa();
          int                rb  = gen.getRb();
          map[ra] = rb;
          block.removeInstruction(prev2, gen);
        } else
          prev2 = inst;
      } 
    }
    
    // Remove any write instructions.
    
    Instruction prev3 = null;
    for (Instruction inst = last.getFirstInstruction();
         inst != null;
         inst = inst.getNext()) {
      if (inst.getOpcode() == Opcodes.WRITE) {
        GeneralInstruction write = (GeneralInstruction) inst;
        int                ra    = write.getRa();
        int                rb    = write.getRb();
       
        map[rb] = ra;
        last.removeInstruction(prev3, write);
      } else
        prev3 = inst;
    }
    
    // Rename the block.
    
    start.nextVisit();
    start.setVisited();
    wl.push(start);
    
    while (!wl.isEmpty()) {
      PredicateBlock block = (PredicateBlock) wl.pop(); 
      block.pushOutEdges(wl);  
      
      // Rename the predicate for the block.
      
      if (block.isPredicated()) {
        int pred        = block.getPredicate();
        int renamedPred = map[pred];

        if (renamedPred > -1)
          block.setPredicate(renamedPred);
      }
      
      // Rename all the instructions.
   
      Instruction prev2 = null;
      for (Instruction inst = block.getFirstInstruction();
           inst != null;
           inst = inst.getNext()) {
        int[] srcs = inst.getSrcRegisters();
        if (srcs != null) {
          int sl = srcs.length;
          for (int i = 0; i < sl; i++) {
            int rb  = srcs[i];
            int reg = map[rb];
            if (reg > -1)
              inst.remapSrcRegister(rb, reg);
          }
        }
        
        int ra = inst.getDestRegister();
        if (ra > -1) {
          int reg = map[ra];
          if (reg != -1)
            inst.remapDestRegister(ra, reg);
        }
        
        // Remove useless copies.
        
        if (inst.isCopy() && (inst.getCopySrc() == inst.getCopyDest()))
          block.removeInstruction(prev2, inst);  
        else
          prev2 = inst;
      }
    } 
    
    WorkArea.<Node>returnStack(wl); 
  }
  
  /**
   * Assign branch identifiers.
   * <br>
   * We can encode up to 8 exit using a maximum predicate tree depth of 3.
   * Any exit that exceeds this is encoded as 0.
   */
  public final void assignBranchIds(boolean verbose)
  {
    if (verbose)
      System.out.println("BLOCK " + getBlockName());
    
    if (numBranches < 2)
      return;
    
    IntMap<Instruction> useDef = ssa.getUseDef(); 
    int                 np     = tail.numInEdges();
    
    for (int i = 0; i < np; i++) {
      PredicateBlock b  = (PredicateBlock) tail.getInEdge(i);
      TripsBranch    br = null;
      
      // Find the branch
      
      for (Instruction inst = b.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        if (inst.isBranch()) {
          assert (inst.isPredicated());
          br = (TripsBranch) inst;
          break;
        }
      }
      
      assert (br != null);
      
      // Compute the branch id
      
      int         bid = 0;
      Instruction def = br;
      int         idx = 0;
      
      if (verbose)
        System.out.print("   ");
      
      do  {
        if (verbose) {
          System.out.print(" <" + def.getPredicate(0));
          System.out.print(" " + def.isPredicatedOnTrue() + ">");
        }
        
        if (def.isPredicatedOnTrue())
          bid += (1 << idx);
        
        idx++;
        
        def = useDef.get(def.getPredicate(0));
        if (def == null) {
          if (verbose)
            System.out.print(" ???");
          break;
        }
      } while (def.isPredicated());
      
      // Assign the branch id or 0 when the id exceeds the encoding space.
      
      if (bid > 7) 
        bid = 0;
      
      br.setBranchId(bid);

      if (verbose)
        System.out.println("\t: " + br);
    }
  }
  
  /**
   * Optimize a hyperblock.  
   * <br>
   * The main goal of this routine is to reduce the block 
   * size to allow additional blocks to be merged.
   */
  public final void optimize(boolean removeLoads, DataflowAnalysis df)
  {
    enterSSA();
 
    if (removeLoads)
      interBlockPredicateMinimization(df);
    
    valueNumber();
  
    removePredicates();

    propagateCopies();
    
    ssa.removeDeadCode();

    analyzeLeaveSSA();
  }
  
  /**
   * Convert the PFG into SSA form.
   */
  public final void enterSSA()
  {
    insertReadWriteInstructions();
    
    ssa = new SSA(regs, this);

    ssa.placePhis();        
    
    // Nullifying writes will insert dead nulls that
    // will be cleaned up by dead code elimination.

    nullifyWrites(); 

    // Remove dead code to eliminate phi nodes, copies, 
    // and reads killed by nullification

    ssa.removeDeadCode();
  }
  
  /**
   * Convert the hyperblock out of SSA form.
   * <p>
   * @param removeReadWrite if read and write instructions should be
   * removed when converting out of SSA form.  SSA form can only be
   * re-entered if read and write instructions are removed.
   */
  public final void leaveSSA(boolean removeReadWrite)
  {
    ssa.removePhis();    
    if (removeReadWrite)
      removeReadWriteInstructions();
    ssa = null;
  }
  
  /**
   * Update the block statistics before leaving SSA form.
   */
  public final void analyzeLeaveSSA()
  {
    ssa.removePhis();
    analyze();
    removeReadWriteInstructions();  
    ssa = null;
  }

  /**
   * Return true if the hyperblock is in SSA form.
   */
  public final boolean inSSA()
  {
    return ssa != null;
  }

  /**
   * Return the ssa instance for this hyperblock.
   */
  public final SSA getSSA()
  {
    return ssa;
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
 
  public static Hyperblock enterHyperblockFlowGraph(Instruction first, Trips2Generator gen)
  {
    HashMap<Instruction, Hyperblock> entries   = new HashMap<Instruction, Hyperblock>(203);
    Trips2RegisterSet                registers = (Trips2RegisterSet) gen.getRegisterSet();
    Hyperblock                       lasthb    = null;

    // Create the hyperblocks.

    for (Instruction inst = first; inst != null; inst = inst.getNext()) {
      if (inst.isLabel() || (inst instanceof BeginMarker)) {
        Hyperblock hb = new Hyperblock(registers);     
        entries.put(inst, hb);  
        
        int loopNumber = inst.getLoopNumber();
        if (loopNumber > 0)
          hb.setLoopNumber(loopNumber);
      } 
    }

    // Create the graph.

    for (Instruction inst = first; inst != null; inst = inst.getNext()) {
      if (inst.isLabel() || (inst instanceof BeginMarker)) {
        lasthb = entries.get(inst);
      } else if (inst.getOpcode() == Opcodes.RET) {
        gen.setReturnBlock(lasthb);
      } else if (inst.isBranch()) {
        Branch br = (Branch) inst;
        int    bt = br.numTargets();

        for (int j = 0; j < bt; j++) {
          Label      lab  = br.getTarget(j);
          Hyperblock succ = entries.get(lab);

          lasthb.addOutEdge(succ);
          succ.addInEdge(lasthb);
          lasthb.setBranchProbability(lab, br.getBranchProbability());
        }
      } 
    }
    
    // Create the PFGs.
    
    for (Enumeration<Instruction> e = entries.uniqueKeys(); e.hasMoreElements(); ) {
      Instruction lab = e.nextElement();
      Hyperblock  hb  = entries.get(lab);
      
      hb.determinePredicatesBranches(lab);       
      hb.createPredicateFlowGraph(lab);
    }
    
    Hyperblock hbStart = entries.get(first);
    removeUnreachableHyperblocks(hbStart, gen);    
    
    return hbStart;
  }
  
  /**
   * Remove unreachable hyperblocks.
   * <p>
   * In this example, "return 1" is in an unreachable hyperblock and
   * should be removed.
   * <pre>
   * int foo() {
   *   for (;;)
   *     do something
   *   return 1;
   * }  
   * </pre>
   * This routine assumes the tag field for all hyperblocks is already
   * set to 0, which is the case when this routine is called during
   * construction of the hyperblock flow graph.  If you call this
   * routine for some reason you need to clear this field yourself.
   */
  private static void removeUnreachableHyperblocks(Hyperblock hbStart, Trips2Generator gen)
  {
    Stack<Node> wl = WorkArea.<Node>getStack("removeUnreachableHyperblocks");
  
    // Tag each hyperblock reachable by a breadth first search.
    
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.setTag(1);  
    }
    
    // Remove unreachable hyperblocks.
    
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
    
    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushInEdges(wl);
      hb.pushOutEdges(wl);
      if (hb.getTag() != 1)
        hb.unlink();
    }
    
    // Don't generate the epilogue if the return block is unreachable.

    Hyperblock ret = gen.getReturnBlock();
    if ((ret != null) && (ret.getTag() != 1))
      gen.setReturnBlock(null);
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Convert a function in hyperblock form to straight-line form.
   * Returns the new firstInstruction (should be the BeginMarker).
   */
  public static Instruction leaveHyperblockFlowGraph(Hyperblock      hbStart,
                                                     Trips2Generator gen)
  {
    Instruction last = null;
    Stack<Node> hwl  = WorkArea.<Node>getStack("leaveHyperblockFlowGraph");

    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);

    // Linearize all instructions.

    while (!hwl.isEmpty()) {
      Hyperblock             hb    = (Hyperblock) hwl.pop();
      PredicateBlock         start = hb.getFirstBlock();
      Vector<PredicateBlock> wl    = new Vector<PredicateBlock>();
      
      hb.pushOutEdges(hwl);

      start.nextVisit();
      start.setVisited();
      wl.add(start);

      while (!wl.isEmpty()) {
        int l = wl.size();
        for (int i = 0; i < l; i++) {
          PredicateBlock b     = wl.elementAt(i);
          Instruction    first = b.getFirstInstruction();
          
          if (last != null)
            last.setNext(first);
         
          if (b.getLastInstruction() != null)
            last = b.getLastInstruction();
        }

        wl = hb.getNextPFGLevel(wl);
      }
    }

    // Update the return label, return instruction and last instruction.

    PredicateBlock start = hbStart.getFirstBlock();
    Instruction    first = start.getFirstInstruction();

    gen.setBeginMarker((BeginMarker) first);

    for (Instruction inst = first; inst != null; ) {
      Instruction next = inst.getNext();
      if (next == null)
        gen.setLastInstruction(inst);
      
      inst = next;
    }

    WorkArea.<Node>returnStack(hwl);

    return first;
  }

  /**
   * Compute the hyperblock flow graph.  The caller should provide the
   * set of entry points to use. 
   * <br>
   * TODO Does this belong in the block splitter?
   */
  public static void computeHyperblockFlowGraph(Vector<Hyperblock> hbs, HashMap<Instruction, Hyperblock> entries)
  {    
    Stack<Node> wl = WorkArea.<Node>getStack("computeHyperblockFlowGraph");

    int l = hbs.size();
    for (int i = 0; i < l; i++) {
      Hyperblock     hb    = hbs.elementAt(i);
      PredicateBlock start = hb.getFirstBlock();

      start.nextVisit();
      start.setVisited();
      wl.push(start);

      while (!wl.isEmpty()) {
        PredicateBlock block = (PredicateBlock) wl.pop();
        block.pushOutEdges(wl);

        // Find the branch in the block.

        Instruction inst = block.getFirstInstruction();
        while (inst != null) {
          if (inst.isBranch())
            break;
          inst = inst.getNext();
        }

        if (inst != null) {
          Branch br = (Branch) inst;
          int    bt = br.numTargets();
          
          for (int j = 0; j < bt; j++) {
            Label      lab  = br.getTarget(j);
            Hyperblock succ = entries.get(lab);

            hb.addOutEdge(succ);
            succ.addInEdge(hb);
            
            hb.setBranchProbability(succ, br.getBranchProbability());
          }
        }
      }
    }

    WorkArea.<Node>returnStack(wl);
  }

  /**
   * Output the hyperblock flow graph for debugging.
   */
  public static void dumpHyperblockFlowGraph(Hyperblock hbStart)
  {
    Stack<Node> wl = new Stack<Node>();

    System.out.println("\n*** BEGIN HYPERBLOCK FLOW GRAPH ***\n");

    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);

    while (!wl.isEmpty()) {
      Hyperblock hb  = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.dumpPredicateFlowGraph();
    }
  }

  /**
   * Return the <b>unique</b> node label.
   */
  public String getDisplayName()
  {
    return "H" + getNodeID();
  }

  /**
   * Return a String suitable for labeling this node in a graphical display.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer("H-");
    buf.append(getNodeID());
    buf.append(" size: ");
    buf.append(blockSize);
    if (!isLegalBlock(true)) {
      buf.append(' ');
      buf.append("invalid");
    }
    return buf.toString();
  }

  /**
   * Returns a description of the block.
   */
  public final String toString()
  {
    StringBuffer buf = new StringBuffer(getDisplayLabel());
    
    buf.append(" fanout: ");
    buf.append(fanout);
    buf.append(" maxLSID: ");
    buf.append(maxLSID);
    buf.append(" spills: ");
    buf.append(numSpills);
    buf.append(" spillSize: ");
    buf.append(spillSize);
    buf.append(" branches: ");
    buf.append(numBranches);
    buf.append(hasDummyStores ? " hasDummyStore" : "");
    
    return buf.toString();
  }


  public int getLoopNumber()
  {
    return loopNumber;
  }

  protected void setLoopNumber(int loopNumber)
  {
    this.loopNumber = loopNumber;
  } 

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTBLUE;
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.BOX;
  }
}
