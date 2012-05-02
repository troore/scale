package scale.score.pp;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.Properties;

import scale.common.*;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.callGraph.CallGraph;
import scale.clef.decl.VariableDecl;
import scale.clef.decl.RoutineDecl;
import scale.clef.type.*;
import scale.clef.LiteralMap;

/**
 * A CFG representation designed specifically for Ball-Larus path profiling.
 * <p>
 * $Id: PPCfg.java,v 1.33 2007-10-29 13:38:14 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public final class PPCfg extends Root implements java.lang.Comparable<PPCfg>
{
  /**
   * The statically-estimated number of times that a loop executes on
   * average.  Used to estimate edge frequencies in order to place
   * instrumentation optimally.
   */
  private static final int ESTIMATED_LOOP_TRIP_COUNT = 10;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph without profiling information.
   */
  public static final int GRAPH_MODE_BEFORE_PROFILING = 1;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph for an acyclic CFG that has Ball-Larus number on its edges.
   */
  public static final int GRAPH_MODE_PATH_NUMBERING = 2;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that shows path profiling instrumentation on the edges.
   */
  public static final int GRAPH_MODE_ABSTRACT_INSTRUMENTATION = 3;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that shows path profiling instrumentation on the edges and
   * shows path register ranges on the blocks.
   */
  public static final int GRAPH_MODE_ABSTRACT_INSTRUMENTATION_WITH_RANGES = 4;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that shows path profiling instrumentation on the edges.
   */
  public static final int GRAPH_MODE_REAL_INSTRUMENTATION = 5;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that highlights a single path (specified with another
   * argument to generateGraph).  The CFG should be acyclic and have
   * Ball-Larus numbering on the edges (i.e., same format as
   * GRAPH_MODE_PATH_NUMBERING).
   */
  public static final int GRAPH_MODE_SHOW_PATH = 6;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that labels each edge with its execution frequency.  The
   * CFG should be acyclic and have Ball-Larus numbering on the edges
   * (i.e., same format as GRAPH_MODE_PATH_NUMBERING), and edge
   * frequencies must have been computed.
   */
  public static final int GRAPH_MODE_EDGE_FREQUENCIES = 7;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that labels each edge with its execution frequency and the
   * amount of definite flow on all paths that include the edge.  The
   * CFG should be acyclic and have Ball-Larus numbering on the edges
   * (i.e., same format as GRAPH_MODE_PATH_NUMBERING), and edge
   * frequencies must have been computed.
   */
  public static final int GRAPH_MODE_DEFINITE_FLOW_EDGES = 8;
  
  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that labels each edge with its execution frequency and a
   * definite flow pair (flow, numBranches) from that edge to the end
   * of the CFG.  The CFG should be acyclic and have Ball-Larus
   * numbering on the edges (i.e., same format as
   * GRAPH_MODE_PATH_NUMBERING), and edge frequencies must have been
   * computed.
   */
  public static final int GRAPH_MODE_DEFINITE_FLOW_PAIR = 9;

  /**
   * This option to generateGraph's mode parameter says to generate a
   * graph that labels each edge with its weight and colors edges in
   * the max spanning tree.
   */
  public static final int GRAPH_MODE_MST = 10;

  /**
   * Represents the idea that we want to deal with paths' definite flow.
   */
  private static final int ACTUAL_FLOW = 1;

  /**
   * Represents the idea that we want to deal with paths' definite flow.
   */
  private static final int DEFINITE_FLOW = 2;

  /**
   * Represents the idea that we want to deal with paths' potential flow.
   */
  private static final int POTENTIAL_FLOW = 3;

  /**
   * Represents the idea that the flow of a path is the number of
   * times it was taken (or estimated to be taken).
   */
  public static final int RAW_METRIC = 1;

  /**
   * Represents the idea that the flow of a path is the number of
   * times it was taken (or estimated to be taken) multiplied by the
   * number of branches in the path.
   */
  public static final int BRANCH_METRIC = 2;
  
  /**
   * Specifies whether or not to generate .vcg files.
   */
  public static boolean generateGraphs = false;

  /**
   * Specifies whether or not to generate an increment mapping file.
   */
  public static boolean generateIncrements = false;

  /**
   * Specifies whether or not to output debug info to the console.
   */
  public static boolean debuggingOutput = false;

  /**
   * Specifies whether or not to fail if a profile information file is
   * not found.
   */
  public static boolean failWithoutProfile = false;
  
  /**
   * Specifies whether or not to truncate loop entrances.
   */
  public static boolean truncateLoopEntrances = false;
  
  /**
   * Specifies whether the simple or complex unrolling heuristic
   * should be used for profile-guided unrolling.
   */
  public static boolean simplerUnrollingHeuristic = false; 

  /**
   * Specifies whether or not the last profile read (or instrumented
   * for) uses profile-guided profiling.
   */
  public static boolean pgp = false;

  /**
   * When doing profile-guided profiling, specifies whether to use the
   * edge profile to pick the maximum spanning tree used for Ball's
   * event counting algorithm.
   */
  public static boolean pgpEventCounting = false;

  /**
   * When doing profile-guided profiling, specifies whether to use the
   * edge profile to order the traversal of edges in the path
   * profiling algorithm that numbers paths.
   */
  public static boolean pgpEdgeOrder = false;

  /**
   * When doing profile-guided profiling, specifies whether to disable
   * the aggressive instrumentation pushing that practical path
   * profiling uses.
   */
  public static boolean pgpDisableAggressivePushing = false;

  /**
   * When doing profile-guided profiling, specifies whether to
   * instrument a routine if, even after cold and obvious edge
   * removal, it still needs hashing.
   */
  public static boolean pgpAvoidHopelessHashRoutines = false;

  /**
   * When doing profile-guided profiling, specifies whether to remove
   * cold edges in all routines or just the routines that can be made
   * to go from hashing to arrays.
   */
  public static boolean pgpAlwaysRemoveColdEdges = false;

  /**
   * The hashing threshold.  If the number of paths is less than the
   * hashing threshold, hashing is not used.  Otherwise, hashing is
   * used.
   */
  public static int hashingThreshold = 4000;

  /**
   * The hash table size to use if hashing is used.  In reality, the
   * hash table will be 1 element larger than this size because the
   * last slot is used for "lost" paths.
   */
  public static int hashTableSize = 701;

  /**
   * Specifies the profile index to perform path analysis on.
   */
  public static int pathAnalysisIndex = -1;

  /**
   * When doing profile-guided profiling, specifies the threshold (as
   * a proportion of total program flow) that a routine's flow must be
   * below to be considered cold.
   */
  public static double pgpColdRoutineThreshold = 0.0001; // 0.01%

  /**
   * When doing profile-guided profiling, the desired amount of
   * attribution of definite flow (as a proportion of total program
   * flow) for the entire program.
   */
  public static double pgpDesiredAdf = 1.0; // 100% -- this setting disables this feature

  /**
   * When doing profile-guided profiling, we only instrument routines
   * with less than this attribution of definite flow.
   */
  public static double pgpRoutineAdfThreshold = 1.0;

  /**
   * When doing profile-guided profiling, the threshold (as a
   * proportion of the flow through the edge's source) that an edge's
   * flow must be below to be considered cold.
   */
  public static double pgpLocalColdEdgeThreshold = 0.05; // 5%

  /**
   * When doing profile-guided profiling, the threshold (as a
   * proportion of total program flow) that an edge's flow must be
   * below to be considered cold.
   */
  public static double pgpGlobalColdEdgeThreshold = 0.001; // 0.1%

  /**
   * When doing profile-guided profiling, if a loop's average
   * iteration count is greater than this value, it should be
   * disconnected.
   */
  public static double pgpLoopDisconnectThreshold = 0.1; // 10%

  /**
   * When doing profile-guided profiling, specifies the amount by
   * which to multiply the global edge criterion if our routine still
   * needs hashing.
   */
  public static double pgpFlexibleColdFactor = 1.0; // Disables this feature.

  /**
   * A set of Scribble routines that should not be instrumented.  Used
   * for profile-guided profiling to avoid instrumenting routines that
   * are cold or have high attribution of definite flow.
   */
  private static HashSet<Scribble>                       routinesToNotInstrument;
  private static HashSet<Path>                           defFlowPaths;               // The paths according to definite flow.
  private static HashMap<PPCfg, PPCfg>                   pgpCfgMap;
  private static HashMap<PPCfg, HashMap<PPEdge, Instr>>  pgpAbstractInstrMap;
  private static HashMap<Object, HashMap<FBPair , Long>> definiteFlow;               // The definite flow (a map from each edge and block to a map that has definite flow info) for the program.
  private static HashMap<Object, HashMap<FBPair , Long>> tepDefiniteFlow;            // Definite flow of a two-edge profile.
  private static HashMap<Object, HashMap<FBPair , Long>> potentialFlow;              // The potential flow (a map from each edge and block to a map that has potential flow info) for the program.
  private static HashMap<Object, HashMap<FBPair , Long>> tepPotentialFlow;           // Potential flow of a two-edge profile.
  private static HashMap<PPEdge, Long>                   definiteBranchFlowForEdges; // The definite flow, using the branch metric, for each edge in the program.
  private static HashMap<PPEdge, Long>                   definiteRawFlowForEdges;    // The definite flow, using the raw/unit metric, for each edge in the program.
  private static HashMap<PPEdge, Long> actualBranchFlowForEdges;   // The actual branch flow for each edge in the program.

  private static LinkedList<PPCfg>        cfgs;        // The CFGs in the program.
  private static PPSupergraphBlock superBegin;  // The top node (root) of the supergraph.
  private static PPSupergraphBlock superEnd;    // The bottom node of the supergraph.
  private static long[]            programFlow; // The total amount of flow in the program.  Each array slot is for a different profile index.
  private static String            outputPath;  // The directory that all generated files should be written to.

  private Scribble scribble;               // The Scribble CFG for this path profiling CFG.
  private long     expandedTableSize;      // The expanded table size that we'll need because of the extra cold edges. Not applicable to hashing.
  private String   routineName;            // The name of this routine.
  private PPCfg    pgpEdgeProfileCfg;      // If this CFG is using profile-guided profiling, this value is the CFG has the edge profile that will be used.
  private PPBlock  beginBlock;             // The begin block.
  private PPBlock  endBlock;               // The end block.
  private boolean  isCyclic;               // True if the graph is cyclic (has back edges and truncated edges and no dummy edges).
  private boolean  hasUnreachableEnd;      // True if the routine has an unreachable END block.

  private HashMap<Chord, PPBlock>  blocks;      // Map from first Chord to block,
  private HashMap<Long, Long>      pathFreqMap; // A map from path numbers (Long) to path frequencies (Long).

  private HashSet<PPEdge>  backEdges;              // The back edges in the CFG.
  private HashSet<PPEdge>  dummyEdges;             // The dummy edges in the CFG.
  private HashSet<PPEdge>  truncatedEdges;         // The truncated edges in the CFG.
  private HashSet<PPEdge>  coldEdges;              // The cold edges in the CFG.  This is part of profile-guided profiling.
  private HashSet<PPBlock> coldBlocks;             // The cold blocks in the CFG.  This is part of profile-guided profiling.
  private HashSet<PPEdge>  obviousEdges;           // The obvious edges in the CFG.  This is part of profile-guided profiling.
  private HashSet<PPBlock> obviousBlocks;          // The obvious blocks in the CFG.  This is part of profile-guided profiling.
  private HashSet<PPEdge>  incomingOvercountEdges; // The set of cold edges with hot targets that, if executed, may cause an overcount of some hot path.
  private HashSet<PPEdge>  outgoingOvercountEdges; // The set of cold edges with hot sources that, if executed, may cause an overcount of some hot path.
  private HashSet<Path>    definiteFlowPaths;      // The set of paths that will have their flow estimated by definite flow.
  private HashSet<Path>    overcountedPaths;       // The set of paths that will be overcounted by profiling.
  private HashSet<Path>    measuredPaths;          // The set of paths that will be have their flow measured by profiling.
  private EdgeE[]          edgeEntries;            // All the edges.


  /**
   * Create a CFG used for path profiling from Scale's CFG representation.
   * @param scribble is Scale's CFG representation.
   * @param pgpEdgeProfileCfg is the CFG with the edge profile if
   * profile-guided profiling is being used or null .
   */
  public PPCfg(Scribble scribble, PPCfg pgpEdgeProfileCfg)
  {
    this.scribble               = scribble;
    this.expandedTableSize      = 0;
    this.routineName            = scribble.getRoutineDecl().getName();
    this.pgpEdgeProfileCfg      = pgpEdgeProfileCfg;
    this.beginBlock             = null;
    this.endBlock               = null;
    this.isCyclic               = true;
    this.hasUnreachableEnd      = false;

    this.blocks                 = new HashMap<Chord, PPBlock>();
    this.backEdges              = new HashSet<PPEdge>();
    this.dummyEdges             = new HashSet<PPEdge>();
    this.truncatedEdges         = new HashSet<PPEdge>();
    this.coldEdges              = new HashSet<PPEdge>();
    this.coldBlocks             = new HashSet<PPBlock>();
    this.obviousEdges           = new HashSet<PPEdge>();
    this.obviousBlocks          = new HashSet<PPBlock>();
    this.incomingOvercountEdges = new HashSet<PPEdge>();
    this.outgoingOvercountEdges = new HashSet<PPEdge>();
    this.edgeEntries            = new EdgeE[256];

    if (programFlow == null)
      programFlow = new long[2];

    // Step 0: validate the Scribble CFG.

    if (Debug.debug(1))
      scribble.validateCFG();

    // Step 1: create the blocks.

    Stack<Chord> wl    = WorkArea.<Chord>getStack("createBlocks");
    Chord         begin = scribble.getBegin();
    Chord         end   = scribble.getEnd();

    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();

    while (!wl.empty()) {
     // Start at the beginning of a basic block. Create the block and
     // add it to the CFG.

      Chord   firstChord = wl.pop();
      PPBlock block      = getBlock(firstChord, false);
      
      // Check if the block is BEGIN and/or END.

      if (block.firstChord() == begin)
        this.beginBlock = block;

      if (block.lastChord() == end)
        this.endBlock = block;

      // Push the unvisited successors; this marks all pushed blocks
      // as visited.

      block.lastChord().pushOutCfgEdges(wl);
    }

    // Step 2: Add the edges; also set beginBlock and endBlock;
    //         it's important to use an order that will be the same in
    //         different runs.

    Chord.nextVisit();
    wl.push(begin);
    begin.setVisited();

    while (!wl.empty()) {
      Chord   firstChord     = wl.pop();
      PPBlock block          = getBlock(firstChord, false);
      Chord   lastChord      = block.lastChord();
      int     numOutCfgEdges = lastChord.numOutCfgEdges();

      for (int n = 0; n < numOutCfgEdges; n++) {
        PPBlock succBlock = getBlock(lastChord.getOutCfgEdge(n), false);

        // Check if the new edge is already in the graph.

        if (hasEdge(block, succBlock, PPEdge.NORMAL)) {
          // This should only occur when the block ends with a SwitchChord.

          assert ((block.lastChord() instanceof SwitchChord) ||
                  (block.lastChord() instanceof IfThenElseChord)) :
            "Expected redundant edge to have SwitchChord or IfThenElseChord source.";

          // There is no need to add another identical edge; we'll
          // take care of this in instrumentEdge().

        } else {// Otherwise, the edge is not in the graph, so add it.
          PPEdge newEdge = getEdge(block, succBlock, PPEdge.NORMAL);
          addEdge(newEdge);
        }
      }

      lastChord.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);

    // Set BEGIN and END.
    // Check for unreachable END block.

    if (endBlock == null) {
      // It looks like END is unreachable (silly spec benchmark authors!)
      Msg.reportInfo(Msg.MSG_Missing_return_statement_s, routineName);

      // Create an END block anyway so that the algorithms and
      // instrumentation works out right.

      endBlock = getBlock(end.firstInBasicBlock(), false);

      // Mark the routine as having an unreachable END block.

      hasUnreachableEnd = true;
    }
  }

  private static class EdgeE
  {
    PPEdge edge;
    EdgeE  next;

    public EdgeE(PPEdge edge, EdgeE next)
    {
      this.edge = edge;
      this.next = next;
    }
  }

  /**
   * Return the edge whose attributes are given.  If the edge does not
   * exist, create it.
   */
  public PPEdge getEdge(PPBlock source, PPBlock target, int type)
  {
    int   hc    = (PPEdge.hashCode(source, target, type) % edgeEntries.length);
    EdgeE entry = edgeEntries[hc];
    EdgeE last  = null;

    while (entry != null) {
      if (entry.edge.equals(source, target, type))
        return entry.edge;
      last = entry;
      entry = entry.next;
    }

    PPEdge edge = new PPEdge(source, target, type, this);
    entry = new EdgeE(edge, null);
    if (last == null)
      edgeEntries[hc] = entry;
    else
      last.next = entry;

    return edge;
  }

  /**
   * Return the edge whose attributes are given.  If the edge does not
   * exist, return <code>null</code>.
   */
  public PPEdge findEdge(PPBlock source, PPBlock target, int type)
  {
    int   hc    = (PPEdge.hashCode(source, target, type) % edgeEntries.length);
    EdgeE entry = edgeEntries[hc];
    EdgeE last  = null;

    while (entry != null) {
      if (entry.edge.equals(source, target, type))
        return entry.edge;
      last = entry;
      entry = entry.next;
    }

    return null;
  }

  /**
   * Return true if the edge, whose attributes are given, exists.
   */
  public boolean hasEdge(PPBlock source, PPBlock target, int type)
  {
    int   hc    = (PPEdge.hashCode(source, target, type) % edgeEntries.length);
    EdgeE entry = edgeEntries[hc];

    while (entry != null) {
      if (entry.edge.equals(source, target, type))
        return true;
      entry = entry.next;
    }

    return false;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(PPCfg-");
    buf.append(getNodeID());
    buf.append(' ');
    buf.append(routineName);
    if (isCyclic)
      buf.append(" cyclic");
    if (hasUnreachableEnd)
      buf.append(" noend");
    buf.append(')');
    return buf.toString();
  }

  /**
   * Compare two CFGs by flow.
   * Sort in ascending order.
   */
  public int compareTo(PPCfg o2)
  {
    PPCfg cfg2  = o2;
    long  flow1 = getBlockFreq(beginBlock);
    long  flow2 = cfg2.getBlockFreq(cfg2.beginBlock);

    if (flow1 < flow2)
      return -1;
    if (flow1 > flow2)
      return 1;
    return 0;
  }

  /**
   * Add a block to the graph.  Note that if you add a block that is
   * BEGIN and/or END, the CFG will NOT automatically update this
   * fact.  This is because some fake blocks may appear to be END
   * initially, but at the end of transformations we don't want them
   * to be END.
   */
  private PPBlock getBlock(Chord first, boolean allowFakeBlocks)
  {
    assert (first.isFirstInBasicBlock() || allowFakeBlocks) :
      "Chord must be first in block!";

    PPBlock blk = blocks.get(first);
    if (blk != null)
      return blk;

    PPBlock block = new PPBlock(first, this);
    blocks.put(first, block); // Add the block to the set of blocks.
    return block;
  }
  
  /**
   * Add an edge to the graph.  Duplicates are not allowed.
   */
  public void addEdge(PPEdge edge)
  {
    PPBlock target = edge.target();
    PPBlock source = edge.source();

    // Add the edge to the map of incoming edges.

    if (!(target instanceof PPSupergraphBlock)) {
      if (blocks.containsKey(target.firstChord())) {
        boolean duplicate = target.addInEdge(edge);
        assert !duplicate : "The edge was already in the incoming set!";
      }
    }

    // Add the edge to the map of outgoing edges.

    if (!(source instanceof PPSupergraphBlock)) {
      if (blocks.containsKey(source.firstChord())) {
        boolean duplicate = source.addOutEdge(edge);
        assert !duplicate : "The edge was already in the outgoing set!";
      }
    }

    // If the edge is a back edge, add it to the set of back edges.

    if (edge.isBackEdge())
      backEdges.add(edge);
  }

  /**
   * Remove an edge from the graph.
   * @param edge is the edge to be removed.
   * @param isPermanent is true if the edge is being removed
   * permanently.  This value is false if the edge is only being
   * removed because the graph is being made acyclic (includes
   * truncated edges).
   */
  private void removeEdge(PPEdge edge, boolean isPermanent)
  {
    // Remove the edge from the map of incoming edges.

    PPBlock target = edge.target();
    if (!(target instanceof PPSupergraphBlock)) {
      boolean ifound = target.removeInEdge(edge);
      assert  ifound : "The edge was not in the incoming set!";
    }

    // Remove the edge from the map of outgoing edges.

    PPBlock source = edge.source();
    if (!(source instanceof PPSupergraphBlock)) {
      boolean ofound = source.removeOutEdge(edge);
      assert  ofound : "The edge was not in the outgoing set!";
    }
    
    if (isPermanent) {
      backEdges.remove(edge);
      truncatedEdges.remove(edge);
      dummyEdges.remove(edge);
      coldEdges.remove(edge);
      obviousEdges.remove(edge);
      incomingOvercountEdges.remove(edge);
      outgoingOvercountEdges.remove(edge);
    }
  }

  /**
   * Make an edge cold.  This means removing it from the graph and
   * adding it to the cold set.  Part of profile-guided profiling.
   */
  private void makeEdgeCold(PPEdge edge)
  {
    removeEdge(edge, false);
    coldEdges.add(edge);
  }

  /**
   * Make a block cold.  This means removing it from the graph and
   * adding it to the cold set.  Part of profile-guided profiling.
   */
  private void makeBlockCold(PPBlock block)
  {
    blocks.remove(block.firstChord());
    coldBlocks.add(block);
  }

  /**
   * Restore a cold edge to the CFG.
   */
  private void restoreColdEdge(PPEdge edge)
  {
    addEdge(edge);
    edge.setIncrement(0);
  }
  
  /**
   * Restore a cold block to the CFG.
   */
  private void restoreColdBlock(PPBlock block)
  {
    blocks.put(block.firstChord(), block);
    block.setNumPaths(0);
  }

  /**
   * Make an edge obvious.  Part of profile-guided profiling.
   */
  private void makeEdgeObvious(PPEdge edge)
  {
    removeEdge(edge, false);
    obviousEdges.add(edge);
  }
  
  /**
   * Make a block obvious.  Part of profile-guided profiling.
   */
  private void makeBlockObvious(PPBlock block)
  {
    blocks.remove(block.firstChord());
    obviousBlocks.add(block);
  }
  
  /**
   * Restore an obvious edge to the CFG.
   */
  private void restoreObviousEdge(PPEdge edge)
  {
    addEdge(edge);
    edge.setIncrement(0);
  }
  
  /**
   * Restore an obvious block to the CFG.
   */
  private void restoreObviousBlock(PPBlock block)
  {
    blocks.put(block.firstChord(), block);
    block.setNumPaths(0);
  }

  /**
   * Remove the old edge, and replace it with the new edge.  Also set
   * the frequency of the new edge to the frequency of the old edge.
   * @param oldEdge is the edge to be removed
   * @param newEdge is the edge to be added
   */
  private void swapEdges(PPEdge oldEdge, PPEdge newEdge)
  {
    long freq = oldEdge.getFrequency();
    removeEdge(oldEdge, true);
    addEdge(newEdge);
    newEdge.setFrequency(freq);
  }

  /**
   * Split a basic block in two.
   * @param first is the first Chord in the block
   * @param newFirstChord is the first chord of the second basic block
   * @param allowFakeSplit true if the new first chord need not be in
   * the basic block
   * @return the new block that begins with the new first chord
   */
  public PPBlock splitBlock(Chord   first,
                            Chord   newFirstChord,
                            boolean allowFakeSplit)
  {
    PPBlock block = blocks.get(first);
    if (block == null) {
      assert Debug.printMessage("** " + first);
      assert Debug.printStackTrace();
      return null;
    }
    return splitBlock(block, newFirstChord, allowFakeSplit);
  }

  /**
   * Split a basic block in two.
   * @param block the basic block to split.
   * @param newFirstChord the first chord of the second basic block
   * @param allowFakeSplit true if the new first chord need not be in
   * the basic block
   * @return the new block that begins with the new first chord
   */
  public PPBlock splitBlock(PPBlock block,
                            Chord   newFirstChord,
                            boolean allowFakeSplit)
  {
    Chord currentChord = block.firstChord();
    assert (currentChord != newFirstChord) :
      "The chord is already first in the basic block.";

    if (!allowFakeSplit) {
      Chord   lastChord  = currentChord.lastInBasicBlock();
      boolean foundChord = false;   
      do {
        currentChord = currentChord.getNextChord();
        if (currentChord == newFirstChord) {
          foundChord = true;
          break;
        }
      } while (currentChord != lastChord);

      assert foundChord : "Unable to find chord";
    }

    PPBlock newBlock = getBlock(newFirstChord, true);
    insertBlock(newBlock, block);

    return newBlock;
  }

  /**
   * Insert a new basic block immediately after another basic block.
   * @param newBlock the new basic block to be inserted
   * @param predBlock the basic block that the new basic block will be
   * inserted after
   */
  private void insertBlock(PPBlock newBlock, PPBlock predBlock)
  {
    boolean isEndBlock = predBlock.equals(endBlock);
    long    blockFreq  = getBlockFreq(predBlock);

    // What if the predecessor block is actually END?

    if (predBlock.numOutEdges() == 0) {
      assert isEndBlock : "Expected the predecessor block to be END as well";

      endBlock = newBlock;
    } else {
      // The normal case: replace all outgoing edges;
      // Create a new list to avoid concurrent modification exceptions.

      PPEdge[] lo = predBlock.outgoing();
      if (lo != null) {
        for (int i = 0; i < lo.length; i++) {
          PPEdge  oldOutgoingEdge = lo[i];
          PPBlock blk             = oldOutgoingEdge.target();
          int     type            = oldOutgoingEdge.getType();
          PPEdge  newOutgoingEdge = getEdge(newBlock, blk, type);
          swapEdges(oldOutgoingEdge, newOutgoingEdge);
        }
      }
    }

    // Add an edge from predBlock to newBlock and set its frequency.

    PPEdge newEdge = getEdge(predBlock, newBlock, PPEdge.NORMAL);
    addEdge(newEdge);
    newEdge.setFrequency(blockFreq);
  }

  public void removeAndUpdate(PPCfg calleePPCfg, Chord callStmt, HashMap<Chord, Chord> nm)
  {
    if (calleePPCfg == null)
      return;

    // Inline the call in the path profiling CFG.

    removeBlock(callStmt);

    // TODO: Try to target only the blocks that might be split.

    // Combine blocks that should not be split.

    // Combine blocks that can be combined.

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock source = eb.nextElement();
      boolean flg    = true;

      while (flg) {
        if (source.numOutEdges() != 1)
          break;

        flg = false;

        PPEdge[] edges = source.outgoing();
        for (int i =  0; i < edges.length; i++) {
          PPEdge  edge   = edges[i];
          PPBlock target = edge.target();
          if (target.numInEdges() == 1) {
            PPBlock combinedBlock = combineBlocks(source, target);

            // Make sure to try the new edges.

            flg = true;
          }
        }
      }
    }

    // Output a graph of the final inlining.

    if (generateGraphs) {
      HashSet<PPBlock>  specialBlocks = new HashSet<PPBlock>(nm.size());
      Iterator<Chord>   chordIter     = nm.values().iterator();
      while (chordIter.hasNext()) {
        Chord   c   = chordIter.next();
        PPBlock blk = new PPBlock(c, this);
        specialBlocks.add(blk);
      }

      boolean done = false;
      for (int i = 1; !done; i++) {
        String name = calleePPCfg.getRoutineName() + "_after_inlining_" + i;
        done = generateGraph(name,
                             GRAPH_MODE_EDGE_FREQUENCIES,
                             null,
                             0,
                             null,
                             specialBlocks,
                             0);
      }
    }

    try { // Make sure our changes were good.
      validateCFG();
    } catch (java.lang.Exception ex) {
      System.out.println("Validation exception in inlining (profile maintenance #1): " + ex);
      System.out.println("Caller: " + getRoutineName());
      System.out.println("Callee: " + calleePPCfg.getRoutineName());
    }

    // TODO: Should we uncomment the line below?
    // If so, we need to ensure that PPCfg.makeCyclicPreservingEdgeProfile() results in a valid edge profile.
    // And that's hard to do.
    // validateEdgeProfile();
  }

  /**
   * Combine two basic blocks into one.
   * @return the combined block 
   */
  private PPBlock combineBlocks(PPBlock first, PPBlock second)
  {
    assert (first.numOutEdges() == 1) : "The two blocks cannot be combined";

    assert ((second.numInEdges() == 1) && hasEdge(first, second, PPEdge.NORMAL)) :
      "The two blocks cannot be combined";

    removeBlock(second);
    
    return first;
  }

  public void removeBlock(Chord first)
  {
    PPBlock block = blocks.get(first);
    if (block == null)
      return;
    removeBlock(block);
  }

  public void removeBlock(PPBlock block)
  {
    // Make sure that the block has exactly one incoming edge.

    PPEdge[] li     = block.incoming();
    PPEdge[] lo     = block.outgoing();
    int      inCnt  = block.numInEdges();
    int      outCnt = block.numOutEdges();

    if (inCnt != 1) {
      // Okay, let's see if it has just one outgoing edge.
      assert (block.numOutEdges() == 1) : "This block cannot be removed";

      PPEdge  outgoingEdge = lo[0];
      PPBlock succBlock    = outgoingEdge.target();

      // Special case: the block is BEGIN.

      if (inCnt == 0) {
        assert block.equals(beginBlock) : "Expected block to be BEGIN";

        beginBlock = succBlock;
      }

      // Remove the block's outgoing edge.

      removeEdge(outgoingEdge, true);

      // Replace all incoming edges.
      // Avoid concurrent modification exceptions.

      if (li != null) {
        for (int i = 0; i < li.length; i++) {
          PPEdge  oldIncomingEdge = li[i];
          PPBlock blk             = oldIncomingEdge.target();
          int     type            = oldIncomingEdge.getType();
          PPEdge  newIncomingEdge = getEdge(succBlock, blk, type);
          swapEdges(oldIncomingEdge, newIncomingEdge);
        }
      }
    } else {
      PPEdge  incomingEdge = li[0];
      PPBlock predBlock    = incomingEdge.source();

      // Special case: the block is END.

      if (outCnt == 0) {
        assert block.equals(endBlock) : "Expected block to be END";

        endBlock = predBlock;
      }

      // Remove the block's incoming edge.

      removeEdge(incomingEdge, true);

      // Replace all outgoing edges.
      // Avoid concurrent modification exceptions.

      if (lo != null) {
        for (int i = 0; i < lo.length; i++) {
          PPEdge  oldOutgoingEdge = lo[i];
          PPBlock blk             = oldOutgoingEdge.target();
          int     type            = oldOutgoingEdge.getType();
          PPEdge  newOutgoingEdge = getEdge(predBlock, blk, type);
          swapEdges(oldOutgoingEdge, newOutgoingEdge);
        }
      }
    }

    blocks.remove(block.firstChord());
    assert !block.hasEdges() : "Expected empty incoming and outgoing sets.";
  }

  /**
   * Add the call block with the callee CFG's contents, and remove the
   * call block.  Do not call this routine if nothing was inlined by
   * the inlining code that operates on chords.
   * @param calleeCfg is the callee's CFG
   * @param callStmt is the call that was replaced
   * @param newIn is the set of new CFG nodes
   * @param nm is a map from old chords to new chords
   */
  public void inlineCall(PPCfg                 calleeCfg,
                         Chord                 callStmt,
                         Chord                 newIn,
                         HashMap<Chord, Chord> nm)
  {
    if (calleeCfg == null)
      return;

    assert isCyclic : "Handling inlining requires a cyclic graph";

    // Create a modified map from old to new chords that accounts for
    // new chords that may have been added above the inlined code.

    HashMap<Chord, Chord> modifiedChordMap = new HashMap<Chord, Chord>(nm);
    Chord                 calleeBegin      = calleeCfg.beginBlock.firstChord();

    // Associate the callee's BEGIN chord with the top chord created.

    modifiedChordMap.put(calleeBegin, newIn);

    // Compute the estimated amount of flow through the callee that
    // is from the call.

    long   callFreq   = getBlockFreq(callStmt);
    long   calleeFreq = getBlockFreq(calleeBegin);
    double factor     = (double) callFreq / calleeFreq;

    // First split up the callee's END chord into a separate basic block.

    PPBlock callBlock     = getBlock(callStmt, false);
    PPBlock calleeEndPred = null;
    if (calleeCfg.endBlock.firstChord() != calleeCfg.endBlock.lastChord()) {
      calleeEndPred = calleeCfg.endBlock;
      calleeCfg.splitBlock(calleeCfg.endBlock, calleeCfg.endBlock.lastChord(), false);
    }

    // Remove the edge from the call block to the out block.

    assert (callBlock.numOutEdges() == 1) :
      "Expected call block to have exactly one outgoing edge";

    PPEdge[] clo           = callBlock.outgoing();
    PPEdge   connectorEdge = clo[0]; 
    removeEdge(connectorEdge, true);

    // Traverse the inlined CFG.

    Iterator<PPBlock> blockIter = calleeCfg.getBlocksInForwardTopologicalOrder().iterator();
    while (blockIter.hasNext()) {
      PPBlock oldBlock = blockIter.next();
      if (!oldBlock.equals(calleeCfg.endBlock)) { // clone the block and add it
        Chord   oldChord = oldBlock.firstChord(); 
        Chord   newChord = modifiedChordMap.get(oldChord);
        PPBlock newBlock = getBlock(newChord, true);

        // Clone the outgoing edges.

        PPEdge[] lo = oldBlock.outgoing();
        if (lo != null) {
          for (int i = 0; i < lo.length; i++) {
            PPEdge  oldOutgoingEdge = lo[i];
            long    oldFreq         = oldOutgoingEdge.getFrequency();
            Chord   firsto          = oldOutgoingEdge.target().firstChord();
            Chord   newSuccChord    = modifiedChordMap.get(firsto);
            PPBlock newSuccBlock    = getBlock(newSuccChord, true);

            // We might need to add the target block.

            if (newBlock == newSuccBlock)
              continue; // Maybe an empty function was inlined.

            int    type            = oldOutgoingEdge.getType();
            PPEdge newOutgoingEdge = getEdge(newBlock, newSuccBlock, type);

            // Set its frequency.

            addEdge(newOutgoingEdge);
            newOutgoingEdge.setFrequency((long) (oldFreq * factor));
          }
        }
      }
    }

    Chord   firstb        = calleeCfg.beginBlock.firstChord();
    PPBlock newFirstBlock = getBlock(modifiedChordMap.get(firstb), true);

    // Put the first block of inlined code in place of the call block;
    // note that the first block is already part of the CFG.

    swapBlocks(callBlock, newFirstBlock);

    // Combine the callee's END block again.

    if (calleeEndPred != null)
      calleeCfg.combineBlocks(calleeEndPred, calleeCfg.endBlock);

    try { // Make sure our changes were good.
      validateCFG();
    } catch (java.lang.Throwable ex) {
      System.out.println("Validation exception in inlining (profile maintenance #2): " + ex);
      System.out.println("Caller: " + getRoutineName());
      System.out.println("Callee: " + getRoutineName());
    }

    // TODO: Should we uncomment the line below?
    // If so, we need to ensure that PPCfg.makeCyclicPreservingEdgeProfile() results in a valid edge profile.
    // And that's hard to do.
    //   validateEdgeProfile();
  }

  /**
   * Transfer all edges from the old block to the new block, and then
   * remove the old block.
   */
  private void swapBlocks(PPBlock oldBlock, PPBlock newBlock)
  {
    // Swap incoming edges.
    // Avoid concurrent modification exceptions.

    PPEdge[] li = oldBlock.incoming();
    if (li != null) {
      for (int i = 0; i < li.length; i++) {
        PPEdge  currentIncoming = li[i];
        PPBlock src             = currentIncoming.source();
        int     type            = currentIncoming.getType();
        PPEdge  newIncoming     = getEdge(src, newBlock, type);
        swapEdges(currentIncoming, newIncoming);
      }
    }

    // Swap outgoing edges.
    // Avoid concurrent modification exceptions.

    PPEdge[] lo = oldBlock.outgoing();
    if (lo != null) {
      for (int i = 0; i < lo.length; i++) {
        PPEdge  currentOutgoing = lo[i];
        PPBlock target          = currentOutgoing.target();
        int     type            = currentOutgoing.getType();
        PPEdge  newOutgoing     = getEdge(newBlock, target, type);
        swapEdges(currentOutgoing, newOutgoing);
      }
    }

    // Remove the old block.

    blocks.remove(oldBlock.firstChord());
    assert !oldBlock.hasEdges() :
      "Expected empty incoming and outgoing sets.";
  }

  private boolean containsBlock(PPBlock block)
  {
    return (blocks.get(block.firstChord()) != null);
  }

  private int numBlocks()
  {
    return blocks.size();
  }

  /**
   * Return the first block in the CFG.
   */
  public PPBlock beginBlock()
  {
    return beginBlock;
  }

  /**
   * Return the last block in the CFG.
   */
  public PPBlock endBlock()
  {
    return endBlock;
  }

  /**
   * Reset the number of paths and the increments on each edge.
   * This should really be called only by computeIncrements().
   */
  private void resetNumPathsAndIncrements()
  {
    Enumeration<PPBlock> bit = blocks.elements();
    while (bit.hasMoreElements()) {
      PPBlock block = bit.nextElement();
      block.resetNumPaths();
    }

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge edge = block.getOutEdge(i);
        edge.setIncrement(0);
      }
    }
  }

  /**
   * Truncate an edge and add in appropriate dummy edges.  This works
   * for back edges and other edges.
   */
  private void truncateEdge(PPEdge edge)
  {
    int dummyType = PPEdge.DUMMY_FOR_TRUNCATED_EDGE;
    if (edge.isBackEdge())
      dummyType = PPEdge.DUMMY_FOR_BACK_EDGE;
    
    assert edge.mayTruncate() : "Truncating this edge is not allowed";

    // Remove the edge itself from the graph.

    removeEdge(edge, false);

    // If we're truncating a non-back edge add it to the set of
    // truncated edges.

    if (dummyType == PPEdge.DUMMY_FOR_TRUNCATED_EDGE)
      truncatedEdges.add(edge);

    // Add the first dummy edge.

    PPBlock target = edge.target();
    if (!hasEdge(beginBlock, target, dummyType)) {
      PPEdge firstDummy = getEdge(beginBlock, target, dummyType);
      dummyEdges.add(firstDummy);
      addEdge(firstDummy);
    }

    // Add the second dummy edge.

    PPBlock src = edge.source();
    if (!hasEdge(src, endBlock, dummyType)) {
      PPEdge secondDummy = getEdge(src, endBlock, dummyType);
      dummyEdges.add(secondDummy);
      addEdge(secondDummy);
    }
  }

  /**
   * Return true if and only if we will use hashing to count path
   * numbers.  If the number of paths is greater than or equal to a
   * threshold, we use hashing.  Otherwise, we do not.
   */
  public final boolean useHashing()
  {
    return (beginBlock.getNumPaths() >= hashingThreshold);
  }

  /**
   * Get the size of the path table.  If we are using hashing, return
   * ONE MORE THAN a suitable hash table size (HASH_TABLE_SIZE + 1).
   * (The extra slot is not used for hashing but rather for counting
   * the "lost" paths taken.)  Otherwise, return the number of paths.
   */
  public int getPathTableSize()
  {
    if (useHashing())
      return hashTableSize + 1;

    if (isPgp()) {
      assert (expandedTableSize >= beginBlock.getNumPaths()) :
        "Did not expect expanded table size to be less than the number of paths";
      assert (expandedTableSize < 10 * beginBlock.getNumPaths()) :
        "The expanded table size is much larger than expected";
      return (int) expandedTableSize;
    }
    
    return (int) beginBlock.getNumPaths();
  }

  /**
   * Get the frequency for a path.  For the path number -1 (lost paths),
   * call getLostPathFreq().
   */
  private long getPathFreq(long pathNum)
  {
    assert (pathNum >= 0) && (pathNum < beginBlock.getNumPaths()) :
      "Not a valid path number!";

    Long pathFreqObj = pathFreqMap.get(new Long(pathNum));
    if (pathFreqObj == null)
      return 0;

    return pathFreqObj.longValue();
  }

  /**
   * Return the number of paths lost because of hashing
   * (represented by the path number -1).
   */
  private long getLostPathFreq()
  {
    Long pathFreqObj = pathFreqMap.get(new Long(-1));
    if (pathFreqObj == null)
      return 0;

    return pathFreqObj.longValue();
  }

  /**
   * Return an iterator over the set of the path numbers of the paths
   * that were taken.  If there were "lost" paths, -1 will be in the
   * set.
   */
  private Iterator<Long> getTakenPathNumbers()
  {
    return pathFreqMap.keySet().iterator();
  }

  /**
   * Set the path frequency map.  This also generates an edge profile
   * from the path profile.  If graph generation is turned on, many
   * graphs are generated.
   * @param pathFreqMap is a map from path numbers to path frequencies.
   */
  public void setPathFreqMap(HashMap<Long, Long> pathFreqMap)
  {
    this.pathFreqMap = pathFreqMap;

    // Compute the edge profile from the path profile.

    computeEdgeProfile();

    // Sanity check on the edge profile.

    assert validateEdgeProfile();

    // Generate a graph showing the edge frequencies.

    if (generateGraphs) {
      generateGraph("edge_frequencies", GRAPH_MODE_EDGE_FREQUENCIES, null);

      // Generate a graph for each taken path (or the hottest 20 paths
      // if there are more than 20).

      // Sort taken path numbers from highest to lowest frequency.
      LinkedList<Long> pathNums = new LinkedList<Long>(pathFreqMap.keySet());
      pathNums.remove(new Long(-1));
      Collections.sort(pathNums, new Comparator<Long>() {
                                   public int compare(Long o1, Long o2)
                                   {
			             long pathNum1 = o1.longValue();
                                     long pathNum2 = o2.longValue();
                                     long freq1    = getPathFreq(pathNum1);
                                     long freq2    = getPathFreq(pathNum2);
                                     if (freq1 < freq2)
                                       return 1;
                                     if (freq1 == freq2)
                                       return 0;
                                     return -1;
                                   }
                                 });

      // Iterate through the top 20 paths.

      Iterator<Long> pathNumIter = pathNums.iterator();
      for (int i = 0; pathNumIter.hasNext() && i < 20; i++) {
        Long pathNum  = pathNumIter.next();
        Long pathFreq = pathFreqMap.get(pathNum);
        generateGraph("show_path_" + pathNum + "_" + pathFreq,
                      GRAPH_MODE_SHOW_PATH,
                      null,
                      pathNum.longValue(),
                      null,
                      null,
                      0);
      }
    }

    // Give this CFG to PathAnalysis so it can do analysis on the
    // supergraph of all CFGs later.

    if (pathAnalysisIndex == 0)
      addCfg(this);
  }

  /**
   * Compute the edge profile from the path profile.  This method is
   * automatically called by setPathFreqMap().  This method only works
   * on an acyclic CFG.
   */
  private void computeEdgeProfile()
  {
    // Initialize the map from edges (PPEdge) to edge frequencies (Long).

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge edge = block.getOutEdge(i);
        assert (!edge.isBackEdge() && !truncatedEdges.contains(edge)) :
          "Did not expect to find back edge or truncated edge in graph";
        edge.setFrequency(0);
      }
    }

    // Go through each taken path.

    Iterator<Long> pathNumIter = pathFreqMap.keySet().iterator();
    while (pathNumIter.hasNext()) {
      Long pn      = pathNumIter.next();
      long pathNum = pn.longValue();

      // Ignore "lost" paths (paths with key -1).

      if (pathNum == -1)
        continue;

      long           pathFreq = pathFreqMap.get(pn).longValue();
      Vector<PPEdge> edges    = getEdgesOnPath(pathNum);
      int    l        = edges.size();
      for (int i = 0; i < l; i++) {
        PPEdge edge = edges.get(i);

        // Add the path's freqency to the edge's current frequency.

        edge.addToFrequency(pathFreq);
      }
    }

    // Compute the total flow (with the unit metric) for this routine,
    // and add it to the total flow for the program.

    programFlow[0] += getBlockFreq(beginBlock);
  }

  /**
   * Do a sanity check on the edge profile.  In particular, check that
   * every block except BEGIN and END has the same total incoming and
   * outgoing frequency.
   */
  private boolean  validateEdgeProfile()
  {
    assert checkEdgeFrequency(blocks) :
      "Invalid edge profile computed from path profile";

    // Also check that sum of outgoing for BEGIN equals sum of
    // incoming for END.

    long beginTotal = beginBlock.getOutEdgeFrequency();
    long endTotal   = endBlock.getInEdgeFrequency();

    assert (beginTotal == endTotal) :
      "Invalid edge profile computed from path profile";

    return true;
  }

  private boolean checkEdgeFrequency(HashMap<Chord, PPBlock> blocks)
  {
    Enumeration<PPBlock> blockIter = blocks.elements();
    while (blockIter.hasMoreElements()) {
      PPBlock block = blockIter.nextElement();
      if (!block.equals(beginBlock) && !block.equals(endBlock)) {
        // Sum the frequencies of the incoming edges.
        long incomingTotal = block.getInEdgeFrequency();
        long outgoingTotal = block.getOutEdgeFrequency();
        if (incomingTotal != outgoingTotal)
          return false;
      }
    }
    return true;
  }

  /**
   * Get the frequency of a basic block.
   */
  public long getBlockFreq(Chord first)
  {
    PPBlock block = blocks.get(first);
    if (block == null)
      return 0;
    return getBlockFreq(block);
  }

  /**
   * Get the frequency of a basic block.
   */
  public long getBlockFreq(PPBlock block)
  {
    if (block.equals(endBlock)) {
      if (beginBlock.equals(endBlock))
        return getPathFreq(0);
      return block.getInEdgeFrequency();
    }

    return block.getOutEdgeFrequency();
  }

  /**
   * Get the frequency of a supergraph block or a normal block.
   */
  private static long getBlockFreq2(PPBlock block)
  {
    // For SUPERBEGIN or SUPEREND, sum the cfg entrance frequencies.

    if (block instanceof PPSupergraphBlock) {
      if (cfgs == null)
        return 0;

      long     totalFreq = 0;
      Iterator<PPCfg> iter      = cfgs.iterator();
      while (iter.hasNext()) {
        PPCfg cfg = iter.next();
        totalFreq += cfg.getBlockFreq(cfg.beginBlock());
      }

      return totalFreq;
    }

    // For a normal block, ask the CFG for the frequency.

    return block.getCfg().getBlockFreq(block);
  }

  /**
   * Get the average trip count for a loop.
   * @param loopHeader is the block that is the header of a loop.
   */
  public double getAvgTripCount(LoopHeaderChord loopHeader)
  {
    PPBlock block = blocks.get(loopHeader);
    if (block == null)
      return 0;

    assert isCyclic : "Expected the graph to be cyclic.";

    return block.getAvgTripCount();
  }

  /**
   * Get the total frequency of all loop headers in the program. 
   */
  public static long getTotalProgFlow()
  {
    return programFlow[0];
  }

  /**
   * Make the graph cyclic, and preserve its edge profile.
   * Because of lost paths and truncated edges, the edge profile may not be
   * perfectly valid (i.e., flow in may not always equal flow out).
   */
  public void makeCyclicPreservingEdgeProfile()
  {
    if (isCyclic)
      return;

    // Go through the back edges.

    Iterator<PPEdge> edgeIter1 = backEdges.iterator();
    while (edgeIter1.hasNext()) {
      PPEdge edge = edgeIter1.next();
      restoreEdge(edge, PPEdge.DUMMY_FOR_BACK_EDGE);
    }

    // Go through the truncated edges.

    Iterator<PPEdge> edgeIter2 = truncatedEdges.iterator();
    while (edgeIter2.hasNext()) {
      PPEdge edge = edgeIter2.next();
      restoreEdge(edge, PPEdge.DUMMY_FOR_TRUNCATED_EDGE);
    }

    // Finally, remove all dummy edges and their frequencies.

    Iterator<PPEdge> dummyEdgeIter = dummyEdges.iterator();
    while (dummyEdgeIter.hasNext()) {
      PPEdge dummyEdge = dummyEdgeIter.next();
      removeEdge(dummyEdge, false);
    }

    // TODO: fix edge profile?

    isCyclic = true;
  }

  /**
   * Restore a previously removed back edge or other truncated edge,
   * and maintain edge frequencies.  Currently, this method is only
   * called for back edges, not truncated edges.
   */
  private void restoreEdge(PPEdge edge, int dummyType)
  {
    PPEdge dummy1 = getEdge(beginBlock, edge.target(), dummyType);
    PPEdge dummy2 = getEdge(edge.source(), endBlock, dummyType);

    long freq1 = dummy1.getFrequency();
    long freq2 = dummy2.getFrequency();
    if (freq1 != freq2)
      // If it's a back edge, take the maximum frequency of the dummy edges;
      // if it's a truncated edge, take the minimum value of the dummy edges
      freq1 = (dummyType == PPEdge.DUMMY_FOR_BACK_EDGE) ?
        Math.max(freq1, freq2) :
        Math.min(freq1, freq2);

    // Add the back edge edge and set its frequency.

    addEdge(edge);
    edge.setFrequency(freq1);
  }
  
  /**
   * Get whether or not the graph is cyclic.
   */
  public boolean isCyclic()
  {
    return isCyclic; 
  }

  /**
   * Validate the CFG.  Throw an error if the CFG is invalid. 
   */
  public void validateCFG()
  {
    HashSet<PPEdge> dummyEdgesEncountered     = WorkArea.<PPEdge>getSet("validateCFG");
    HashSet<PPEdge> backEdgesEncountered      = WorkArea.<PPEdge>getSet("validateCFG");
    HashSet<PPEdge> truncatedEdgesEncountered = WorkArea.<PPEdge>getSet("validateCFG");
    
    Stack<Chord> wl    = WorkArea.<Chord>getStack("validateCFG");
    Chord        start = scribble.getBegin();

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      Chord    firstChord     = wl.pop();
      PPBlock  block          = getBlock(firstChord, false);
      Chord    lastChord      = block.lastChord();
      Chord[]  outgoingChords = lastChord.getOutCfgEdgeArray();
      PPEdge[] outEdges       = block.outgoing();

      // Deal with the outgoing edges.

      if (lastChord.isLoopTail()) { // back edge
        for (int i = 0; i < outgoingChords.length; i++) {
          PPBlock dst      = getBlock(outgoingChords[i], false);
          PPEdge  backEdge = getEdge(block, dst, PPEdge.NORMAL);

          // Acyclic graph

          if (!isCyclic) {
            PPBlock blk = getBlock(outgoingChords[i], false);

            if (!hasEdge(beginBlock, blk, PPEdge.DUMMY_FOR_BACK_EDGE))
              throw new scale.common.InternalError("The first dummy edge is not in the CFG");
            if (!hasEdge(block, endBlock, PPEdge.DUMMY_FOR_BACK_EDGE))
              throw new scale.common.InternalError("The second dummy edge is not in the CFG");

            PPEdge dummy1 = getEdge(beginBlock, blk, PPEdge.DUMMY_FOR_BACK_EDGE);
            PPEdge dummy2 = getEdge(block, endBlock, PPEdge.DUMMY_FOR_BACK_EDGE);
            dummyEdgesEncountered.add(dummy1);
            dummyEdgesEncountered.add(dummy2);
          }

          backEdgesEncountered.add(backEdge);
        }
      } else { // regular edge (i.e., not a back edge)
        for (int i = 0; i < outgoingChords.length; i++) {
          PPBlock blk  = getBlock(outgoingChords[i], false);
          PPEdge  edge = null;
          for (int j = 0; j < outEdges.length; j++) {
            PPEdge e = outEdges[j];
            if (e.target() == blk) {
              edge = e;
              break;
            }
          }
          if (edge != null)
            continue;

          // Check if the edge is in fact a truncated edge (and the
          // graph is acyclic).

          PPEdge nedge = findEdge(block, blk, PPEdge.NORMAL);
          if (!isCyclic && truncatedEdges.contains(nedge)) {
            PPEdge dummy1 = findEdge(beginBlock, blk, PPEdge.DUMMY_FOR_TRUNCATED_EDGE);
            PPEdge dummy2 = findEdge(block, endBlock, PPEdge.DUMMY_FOR_TRUNCATED_EDGE);

            if (dummy1 == null)
              throw new scale.common.InternalError("The first dummy edge is not in the CFG");
            if (dummy2 == null)
              throw new scale.common.InternalError("The second dummy edge is not in the CFG");

            truncatedEdgesEncountered.add(nedge);
            dummyEdgesEncountered.add(dummy1);
            dummyEdgesEncountered.add(dummy2);
            continue;
          }

          throw new scale.common.InternalError("The normal edge is not in the CFG");
        }
      }

      // Push the unvisited successors; this marks all pushed blocks as visited.

      lastChord.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);

    if (!backEdgesEncountered.equals(backEdges))
      throw new scale.common.InternalError("The back edge sets don't match");

    if (!isCyclic) {
      if (!truncatedEdgesEncountered.equals(truncatedEdges))
        throw new scale.common.InternalError("The truncated edge sets don't match");

      if (!dummyEdgesEncountered.equals(dummyEdges))
        throw new scale.common.InternalError("The dummy edge sets don't match");
    }

    // Check that begin and end are correct.

    if (!beginBlock.firstChord().equals(scribble.getBegin()))
      throw new scale.common.InternalError("The begin block isn't right");

    if (!endBlock.lastChord().equals(scribble.getEnd()))
      throw new scale.common.InternalError("The end block isn't right");

    WorkArea.<PPEdge>returnSet(dummyEdgesEncountered);
    WorkArea.<PPEdge>returnSet(backEdgesEncountered);
    WorkArea.<PPEdge>returnSet(truncatedEdgesEncountered);

    // Validate the edges.

    validateEdges();
  }

  /**
   * Make sure the edge set matches the incoming and outgoing sets.
   */
  private void validateEdges()
  {
    // Now do the harder step of checking that they have the correct values.

    HashSet<PPEdge> incomingEdgesEncountered = WorkArea.<PPEdge>getSet("validateEdges");
    HashSet<PPEdge> outgoingEdgesEncountered = WorkArea.<PPEdge>getSet("validateEdges");

    Enumeration<PPBlock> blockIter = blocks.elements();
    while (blockIter.hasMoreElements()) {
      PPBlock block = blockIter.nextElement();
      block.validate(block, incomingEdgesEncountered, outgoingEdgesEncountered);
    }

    /*
    // also check that the back edges are all in the back edges set
    if (isCyclic) {
      HashSet<PPEdge> backEdgesEncountered = new HashSet<PPEdge>();
      Iterator<PPEdge> edgeIter = edges.iterator();
      while (edgeIter.hasNext()) {
        PPEdge edge = (PPEdge)edgeIter.next();
        if (PPEdge.isBackEdge(edge)) {
          backEdgesEncountered.add(edge);
        }
      }
      if (!backEdgesEncountered.equals(backEdges)) {
        throw new scale.common.InternalError("Not all back edges are marked as back edges");
      }
    }
    */

    WorkArea.<PPEdge>returnSet(incomingEdgesEncountered);
    WorkArea.<PPEdge>returnSet(outgoingEdgesEncountered);

  }

  /**
   * Get a list of edges that correspond to the path number.
   */
  private Vector<PPEdge> getEdgesOnPath(long pathNum)
  {
    assert ((pathNum >= 0) && (pathNum < beginBlock.getNumPaths())) :
      "Not a valid path number " +
      pathNum +
      " " +
      beginBlock.getNumPaths() +
      " " +
      scribble.getRoutineDecl().getName();

    // We use a greedy algorithm which is exactly what we want.

    Vector<PPEdge>  edgeList = new Vector<PPEdge>();
    PPBlock         block    = beginBlock;
    while (!block.equals(endBlock)) {
      // Look at all outgoing edges of the block; choose the edge that
      // has the highest value that is less than or equal to the
      // current value of pathNum.

      PPEdge edgeWithHighestInc = block.getHighestOutEdge(pathNum);

      // Now we have the edge we want, so add it to the list of edges;
      // also update pathNum and move to the block.

      edgeList.add(edgeWithHighestInc);
      pathNum -= edgeWithHighestInc.getIncrement();
      block = edgeWithHighestInc.target();
    }

    return edgeList;
  }

  /**
   * Get a list of basic blocks that correspond to the path number.
   */
  private LinkedList<PPBlock> getBlocksOnPath(long pathNum)
  {
    // Cheat and call getEdgesOnPath().

    LinkedList<PPBlock> blockList = new LinkedList<PPBlock>();
    Iterator<PPEdge>    iter      = getEdgesOnPath(pathNum).iterator();
    while (iter.hasNext()) {
      PPEdge edge = iter.next();
      blockList.add(edge.target());
    }

    blockList.add(endBlock);

    return blockList;
  }

  /**
   * Return a list of the basic blocks in the CFG in forward
   * topological order.
   */
  private LinkedList<PPBlock> getBlocksInForwardTopologicalOrder()
  {
    LinkedList<PPBlock> blockList = new LinkedList<PPBlock>();
    forwardTopologicalOrder(beginBlock, blockList, null, new HashSet<Object>());
    return blockList;
  }

  /**
   * Return a list of the edges in the CFG in forward topological
   * order.
   */
  private LinkedList<PPEdge> getEdgesInForwardTopologicalOrder()
  {
    LinkedList<PPEdge> edgeList = new LinkedList<PPEdge>();
    forwardTopologicalOrder(beginBlock, null, edgeList, new HashSet<Object>());
    return edgeList;
  }

  /**
   * A worker method for getBlocksInForwardTopologicalOrder().
   */
  private void forwardTopologicalOrder(PPBlock             block,
                                       LinkedList<PPBlock> blockList,
                                       LinkedList<PPEdge>  edgeList,
                                       HashSet<Object>     visited)
  {
    if (visited.contains(block)) // Only visit a block once.
      return;

    // Make sure that all predecessor blocks have already been
    // visited.

    PPEdge[] li = block.incoming();
    if (li != null) {
      for (int i = 0; i < li.length; i++) {
        PPEdge incomingEdge = li[i];
        if (!incomingEdge.isBackEdge() &&
            !visited.contains(incomingEdge.source()))
          return;
      }
    }

    // Don't visit the block again.

    visited.add(block);

    // Add this block and its outgoing edges to the list(s).

    if (blockList != null)
      blockList.add(block);

    if (edgeList != null)
      block.addAllInEdges(edgeList);

    // Finally, call this function on all successors.

    PPEdge[] lo = block.outgoing();
    if (lo != null) {
      for (int i = 0; i < lo.length; i++) {
        PPEdge outgoingEdge = lo[i];
        forwardTopologicalOrder(outgoingEdge.target(),
                                blockList,
                                edgeList,
                                visited);
      }
    }
  }
  
  /**
   * Return a list of the basic blocks in the CFG in reverse
   * topological order.
   */
  private LinkedList<PPBlock> getBlocksInReverseTopologicalOrder()
  {
    LinkedList<PPBlock> blockList = new LinkedList<PPBlock>();
    reverseTopologicalOrder(beginBlock, blockList, null, new HashSet<Object>());
    return blockList;
  }

  /**
   * Return a list of the edges in the CFG in reverse topological order.
   */
  private LinkedList<PPEdge> getEdgesInReverseTopologicalOrder()
  {
    LinkedList<PPEdge> edgeList = new LinkedList<PPEdge>();
    reverseTopologicalOrder(beginBlock, null, edgeList, new HashSet<Object>());
    return edgeList;
  }

  /**
   * A worker method for getBlocksInReverseTopologicalOrder() and
   * getEdgesInReverseTopologicalOrder().
   */
  private void reverseTopologicalOrder(PPBlock             block,
                                       LinkedList<PPBlock> blockList,
                                       LinkedList<PPEdge>  edgeList,
                                       HashSet<Object>     visited)
  {
    if (visited.contains(block)) // Only visit a block once.
      return;

    visited.add(block);

    // To get reverse topological order, first call this function on
    // all successors.

    PPEdge[] lo = block.outgoing();
    if (lo != null) {
      for (int i = 0; i < lo.length; i++) {
        PPEdge edge = lo[i];
        reverseTopologicalOrder(edge.target(), blockList, edgeList, visited);
      }
    }

    // Finally, add this block and its outgoing edges to the list.

    if (blockList != null)
      blockList.add(block);

    if (edgeList != null) 
      block.addAllOutEdges(edgeList);
  }

  /**
   * Get the routine name.
   */
  public String getRoutineName()
  {
    return routineName;
  }

  /**
   * Set the path to the directory that the generated files will be
   * written to.
   */
  public static void setOutputPath(String path)
  {
    outputPath = path;
  }
  
  private String genFilename(String partialName, String ext)
  {
    StringBuffer buf = new StringBuffer(outputPath);
    buf.append(File.separator);
    buf.append(routineName);
    buf.append("_");
    buf.append(0);
    buf.append("_");
    buf.append(partialName);
    buf.append(ext);
    return buf.toString();
  }

  /**
   * Generate a VCG graph of this CFG to a file.
   * @param mode The graph mode to use when generate the graph.  See
   * the GRAPH_MODE_ constants.
   * @param instrumentationMap is a map from PPEdge or PPBlock to List of
   * Chord.  This param is used only if mode ==
   * GRAPH_MODE_INSTRUMENTATION.
   * @param pathNumber is the path number for a path that we want
   * highlighted.  This param is used only if mode ==
   * GRAPH_MODE_SHOW_PATH.
   * @param valueMap is a map from edges and/or blocks to values of
   * some sort.  This param is used only if mode ==
   * GRAPH_MODE_DEFINITE_FLOW or GRAPH_MODE_MST.
   * @param specialBlocksOrEdges is a collection of blocks and/or
   * edges that will be colored red (blocks) or green (edges).
   * @param flowMetric is the flow metric to use for definite flow if
   * mode == GRAPH_MODE_DEFINITE_FLOW.
   * @return true if the file does not already exist, false otherwise.
   */
  public boolean generateGraph(String                                 partialName,
                               int                                    mode,
                               HashMap<PPEdge, ? extends Object>      instrumentationMap,
                               long                                   pathNumber,
                               HashMap<Object, HashMap<FBPair, Long>> valueMap,
                               Collection<? extends Object>           specialBlocksOrEdges,
                               int                                    flowMetric) {

    String filename = genFilename(partialName, ".vcg");

    if (new File(filename).exists())
      return false;   

    // generate a visualization of the CFG
    try {
      PrintStream ps = new PrintStream(new FileOutputStream(filename));
      generateGraph(partialName,
                    ps,
                    mode,
                    instrumentationMap,
                    pathNumber,
                    valueMap,
                    specialBlocksOrEdges,
                    flowMetric);
      // necessary so that we don't have too many open files
      ps.close();
    } catch (IOException ioex) {
      Msg.reportWarning(Msg.MSG_s, null, 0, 0, ioex.getMessage());
    }
    
    return true;
  }

  /**
   * Generate a VCG graph of this CFG to a file.  See the other
   * generateGraph() method for more info.
   * @param mode The graph mode to use when generate the graph.  See
   * the GRAPH_MODE_ constants.
   * @param instrumentationMap is a map from PPEdge or PPBlock to List of
   * Chord.  This param is used only if mode ==
   * GRAPH_MODE_INSTRUMENTATION.
   * @return true if the file does not already exist, false otherwise.
   */
  public boolean generateGraph(String                            partialName,
                               int                               mode,
                               HashMap<PPEdge, ? extends Object> instrumentationMap) {
    return generateGraph(partialName, mode, instrumentationMap, 0, null, null, 0);
  }

  /**
   * Generate a VCG graph of this CFG.
   * @param ps The PrintStream to write to.
   * @param mode The graph mode to use when generate the graph.  See
   * the GRAPH_MODE_ constants.
   * @param instrumentationMap is a map from PPEdge or PPBlock to List of
   * Chord.  This param is used only if mode ==
   * GRAPH_MODE_INSTRUMENTATION.
   * @param pathNumber is the path number for a path that we want
   * highlighted.  This param is used only if mode ==
   * GRAPH_MODE_SHOW_PATH.
   * @param valueMap is a map from edges and/or blocks to values of
   * some sort.  This param is used only if mode ==
   * GRAPH_MODE_DEFINITE_FLOW or GRAPH_MODE_MST.
   * @param specialBlocksOrEdges is a collection of blocks and/or
   * edges that will be colored red (blocks) or green (edges).
   * @param flowMetric is the flow metric to use for definite flow if
   * mode == GRAPH_MODE_DEFINITE_FLOW.
   */
  public void generateGraph(String                                 partialName,
                            PrintStream                            ps,
                            int                                    mode,
                            HashMap<PPEdge, ? extends Object>      instrumentationMap,
                            long                                   pathNumber,
                            HashMap<Object, HashMap<FBPair, Long>> valueMap,
                            Collection<? extends Object>           specialBlocksOrEdges,
                            int                                    flowMetric)
  {
    ps.println("graph: {");
    ps.println("  display_edge_labels: yes");
    ps.print("  title: \"");
    ps.print(partialName);
    ps.println("\"");
    ps.print("  label: \"");
    ps.print(partialName);
    ps.println("\"");

    // Convert the path number to a set of edges.

    HashSet<PPEdge> edgesOnPath = WorkArea.<PPEdge>getSet("generateGraph");

    if (mode == GRAPH_MODE_SHOW_PATH) {
      edgesOnPath.addAll(getEdgesOnPath(pathNumber));
      // Showing a path is equivalent to displaying path numbering
      // plus highlighting a path.
      mode = GRAPH_MODE_PATH_NUMBERING;
    }

    Enumeration<PPBlock> blockIter = blocks.elements();
    while (blockIter.hasMoreElements()) {
      PPBlock block = blockIter.nextElement();
      ps.println("  node: {");
      ps.print("    title: \"");
      ps.print(block.getDisplayName());
      ps.println("\"");

      ps.print("    label: \"");
      if (mode == GRAPH_MODE_DEFINITE_FLOW_PAIR) {
        long blockActualFlow = getBlockFreq(block);
        ps.print(blockActualFlow);

        HashMap<FBPair, Long> flowMap  = valueMap.get(block);
        Iterator<FBPair>      pairIter = flowMap.keySet().iterator();
        while (pairIter.hasNext()) {
          FBPair pair  = pairIter.next();
          long   count = flowMap.get(pair).longValue();
          ps.print(" [(");
          ps.print(commas(pair.flow()));
          ps.print(", ");
          ps.print(commas(pair.numBranches()));
          ps.print(") -> ");
          ps.print(commas(count));
          ps.print("]");
        }
      } else if (mode == GRAPH_MODE_ABSTRACT_INSTRUMENTATION_WITH_RANGES) {
        ps.print(block.getRangeText());
      } else {
        ps.print(block.getDisplayName());
        ps.print("\\n  np:   ");
        ps.print(block.getNumPaths());
        ps.print("\\n  high: ");
        ps.print(block.getHighRange());
        ps.print("\\n  low:  ");
        ps.print(block.getLowRange());
        Chord chord = block.firstChord();
        while (true) {
          ps.print("\\n");

          String cn  = chord.getClass().getName();
          int    ind = cn.lastIndexOf('.');
          if (ind > 0)
            cn = cn.substring(ind + 1);
          ps.print(cn);
          ps.print('-');
          ps.print(chord.getNodeID());

          if (chord == block.lastChord())
            break;

          chord = chord.getNextChord();
        }
        if (mode == GRAPH_MODE_PATH_NUMBERING) {
          ps.print("\\nNum paths: ");
          ps.print(commas(block.getNumPaths()));
        }
      }
      ps.println("\"");
      if (specialBlocksOrEdges != null && specialBlocksOrEdges.contains(block)) {
        ps.println("    color: green");
      } else if (coldBlocks.contains(block)) {
        ps.println("    color: blue");
      }
      ps.println("  }");
    }

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge edge = block.getOutEdge(i);
      
        // ignore edges that connect to super graph blocks
        if ((edge.source() instanceof PPSupergraphBlock) ||
            (edge.target() instanceof PPSupergraphBlock)) {
          continue;
        }
      
        if (edge.isBackEdge())
          ps.println("  backedge: {");
        else
          ps.println("  edge: {");

        ps.print("    sourcename: \"");
        ps.print(edge.source().getDisplayName());
        ps.println("\"");
        ps.print("    targetname: \"");
        ps.print(edge.target().getDisplayName());
        ps.println("\"");

        StringBuffer buf = new StringBuffer();
        if (mode == GRAPH_MODE_PATH_NUMBERING) {
          buf.append(edge.getIncrement());
        } else if ((mode == GRAPH_MODE_ABSTRACT_INSTRUMENTATION) ||
                   (mode == GRAPH_MODE_ABSTRACT_INSTRUMENTATION_WITH_RANGES)) {
          Instr instr = (Instr) instrumentationMap.get(edge);
          if (instr != null)
            buf.append(instr.getText());

          if (isPgp()) {
            if (instr != null)
              buf.append("\\n");

            Long edgeActualFlow = definiteRawFlowForEdges.get(edge);

            buf.append(commas(getPgpEdge(edge, false).getFrequency()));
            buf.append(" (");
            buf.append(commas(edgeActualFlow.longValue()));
            buf.append(')');
          }
        } else if (mode == GRAPH_MODE_REAL_INSTRUMENTATION) {
          Chord[] chords = (Chord[]) instrumentationMap.get(edge);
          if (chords != null) {
            String delim = "";
            for (int n = 0; n < chords.length; n++) {
              String text = chords[n].getDisplayLabel();
              if (!text.equals("")) {
                buf.append(delim + text);
                delim = "\\n";
              }
            }
          }
        } else if (mode == GRAPH_MODE_EDGE_FREQUENCIES) {
          buf.append(commas(edge.getFrequency()));
        } else if (mode == GRAPH_MODE_DEFINITE_FLOW_PAIR) {
          long edgeActualFlow = edge.getFrequency();
          buf.append(commas(edgeActualFlow));

          HashMap<FBPair, Long> flowMap  = valueMap.get(edge);
          Iterator<FBPair>      pairIter = flowMap.keySet().iterator();
          while (pairIter.hasNext()) {
            FBPair pair  = pairIter.next();
            long   count = flowMap.get(pair).longValue();
            buf.append(" [(");
            buf.append(commas(pair.flow()));
            buf.append(", ");
            buf.append(commas(pair.numBranches()));
            buf.append(") -> ");
            buf.append(commas(count));
            buf.append("]");
          }
        } else if (mode == GRAPH_MODE_MST) {
          buf.append(edge.getWeight());
        }
        if (buf.length() > 0) {
          ps.print("    label: \"");
          ps.print(buf);
          ps.print("\"");
        }

        if (edge.isDummy())
          ps.println("    linestyle: dashed");

        if (edgesOnPath.contains(edge) ||
            ((specialBlocksOrEdges != null) && specialBlocksOrEdges.contains(edge)))
          ps.println("    color: green");
        else if (coldEdges.contains(edge))
          ps.println("    color: blue");
        else if (obviousEdges.contains(edge))
          ps.println("    color: orange");
        else if (edge.isBackEdge())
          ps.println("    color: red");
        else if (truncatedEdges.contains(edge) ||
                 (edge.getType() == PPEdge.DUMMY_FOR_TRUNCATED_EDGE))
          ps.println("    color: lightgrey");

        ps.println("  }");

        WorkArea.<PPEdge>returnSet(edgesOnPath);
      }
    }

    ps.println("}");
  }

  /**
   * Output a mapping of source line numbers to path profiling
   * increments.
   */
  private void generateIncrements(String partialName)
  {
    String filename = genFilename("_increments_" + partialName, ".txt");

    // Generate a visualization of the CFG.

    try {
      PrintStream ps = new PrintStream(new FileOutputStream(filename));
      generateIncrements(ps);
      // Necessary so that we don't have too many open files.
      ps.close();
    } catch (IOException ioex) {
      Msg.reportWarning(Msg.MSG_s, ioex.getMessage());
    }
  }

  /**
   * Output a mapping of Scribble chords to path profiling increments.
   */
  private void generateIncrements(PrintStream ps)
  {
    Stack<Chord> wl    = WorkArea.<Chord>getStack("generateIncrements");
    Chord        start = beginBlock.firstChord();

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      // start at the beginning of a basic block
      PPBlock sourceBlock = getBlock(wl.pop(), false);
      Chord   sourceChord = sourceBlock.lastChord();

      // Output information about each outgoing edge of the basic block.

      int numEdges = sourceChord.numOutCfgEdges();
      for (int n = 0; n < numEdges; n++) {
        Chord   targetChord = sourceChord.getOutCfgEdge(n);
        PPBlock dst         = getBlock(targetChord, false);
        ps.print(sourceChord.getClass().getName());
        ps.print(" -> ");
        ps.print(targetChord.getClass().getName());
        ps.println(" 0");
      }

      // Push the unvisited successors; this marks all pushed blocks
      // as visited.

      sourceChord.pushOutCfgEdges(wl);
    }
    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Return true if this CFG is for profile-guided profiling.
   */
  public boolean isPgp()
  {
    return pgpEdgeProfileCfg != null;
  }

  /**
   * Get an equivalent block in the CFG that has the edge profile.
   */
  public PPBlock getPgpBlock(PPBlock block)
  {
    assert ((block.getCfg() == this) && blocks.containsKey(block.firstChord())) :
      "The block is not in this CFG";

    PPBlock pgpBlock = new PPBlock(block.firstChord(), pgpEdgeProfileCfg);
    assert pgpEdgeProfileCfg.containsBlock(pgpBlock) :
      "The corresponding block is not in the edge profile CFG";

    return pgpBlock;
  }

  /**
   * Given a block from the CFG with the edge profile, return the
   * block in this CFG.
   */
  public PPBlock getPgpBlockInverse(PPBlock pgpBlock, boolean ignoreNotInCfg)
  {
    assert ((pgpBlock.getCfg() == pgpEdgeProfileCfg) &&
            pgpEdgeProfileCfg.containsBlock(pgpBlock)) :
      "The block is not in the edge profile CFG";

    PPBlock block = new PPBlock(pgpBlock.firstChord(), this);
    assert ignoreNotInCfg && !blocks.containsKey(block.firstChord()) :
      "The corresponding block is not in this CFG";

    return block;
  }

  /**
   * Get an equivalent edge in the CFG that has the edge profile.
   * @param edge is the edge in this CFG.
   * @param ignoreNotInEdgeProfileCfg is true if it's okay if the
   * corresponding edge is not in the edge profile CFG (e.g., it might
   * be a truncated edge).
   */
  private PPEdge getPgpEdge(PPEdge edge, boolean ignoreNotInEdgeProfileCfg)
  {
    assert (edge.getCfg() == this) :
      "The edge is not in this CFG";

    PPBlock src     = getPgpBlock(edge.source());
    PPBlock dst     = getPgpBlock(edge.target());
    PPEdge  pgpEdge = pgpEdgeProfileCfg.findEdge(src, dst, edge.getType());
    assert (ignoreNotInEdgeProfileCfg || (pgpEdge != null)) :
      "The corresponding edge is not in the edge profile CFG";

    return pgpEdge;
  }
  
  /**
   * Given an edge from the CFG with the edge profile, return the
   * corresponding edge in this CFG.
   */
  private PPEdge getPgpEdgeInverse(PPEdge pgpEdge, boolean ignoreNotInCfg)
  {
    assert (pgpEdge.getCfg() == pgpEdgeProfileCfg) :
      "The edge is not in the edge profile CFG";

    PPBlock src  = getPgpBlockInverse(pgpEdge.source(), ignoreNotInCfg);
    PPBlock dst  = getPgpBlockInverse(pgpEdge.target(), ignoreNotInCfg);
    PPEdge  edge = findEdge(src, dst, pgpEdge.getType());

    assert (ignoreNotInCfg || (edge != null)) :
      "The corresponding edge is not in this CFG";

    return edge;
  }
  
  /**
   * Implementation of Ball's event counting algorithm, which moves path
   * profiling increments to optimal locations.  Most of the code is
   * based on code from the Ball-Larus-Ammons path profiler.
   */
  private void doPlacement()
  {
    // Compute exact edge weights if specified by using the edge
    // profile, or fix the already-estimated weights.

    if (isPgp() && pgpEventCounting)
      computeExactWeights();
    else
      fixEstimatedWeights();

    // Add an extra edge from END to BEGIN.

    PPEdge extraEdge = getEdge(endBlock, beginBlock, PPEdge.NORMAL);

    addEdge(extraEdge);

    extraEdge.setIncrement(0);

    // Set the weight of the extra edge high so that it's unlikely
    // that it will be instrumented.

    extraEdge.setWeight((double) Long.MAX_VALUE);

    HashSet<PPEdge> maxSpanningTree = findMaxSpanningTree();

    if (generateGraphs)
      generateGraph("mst", GRAPH_MODE_MST, null, 0, null, maxSpanningTree, 0);

    // Move increments around using Ball's algorithm.

    moveIncrements(maxSpanningTree);

    // Remove the extra edge from END to BEGIN; first add its
    // increment to the incoming edges to END.

    long inc = extraEdge.getIncrement();
    if (inc != 0) {
      if (false) {
        System.out.print("** dop ");
        System.out.print(inc);
        System.out.print(" ");
        System.out.println(extraEdge);
        System.out.print("       ");
        System.out.print(scribble.getRoutineDecl().getCallGraph().getName());
        System.out.print(" ");
        System.out.println(scribble.getRoutineDecl().getName());
        assert false : "Did not expect extra edge to get an increment";
      }
      endBlock.incrementInEdges(inc);
    }

    removeEdge(extraEdge, true);
  }

  /**
   * Compute weights using the edge profile from the CFG with that info.
   */
  private void computeExactWeights()
  {
    PPCfg                edgeProfileCfg = pgpEdgeProfileCfg;
    Enumeration<PPBlock> eb             = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge edge = block.getOutEdge(i);
        long   freq = getPgpEdge(edge, false).getFrequency();

        // If the frequency is 0, give it a positive frequency anyway.
        // This should help keep increments from being any closer to
        // BEGIN or END than necessary, I think.

        edge.setWeight((freq == 0) ? 0.01 : freq);
      }
    }
  }

  /**
   * The weight map is for a cyclic CFG, and our CFG is currently
   * acyclic, so fix the weight map.
   */
  private void fixEstimatedWeights()
  {
    // Go through all back edges and move their estimated weights to
    // the corresponding dummy edges.

    Iterator<PPEdge> it1 = backEdges.iterator();
    while (it1.hasNext())
      fixEstimatedWeights(it1.next(), PPEdge.DUMMY_FOR_BACK_EDGE);

    // Do the same for truncated edges.

    Iterator<PPEdge> it2 = truncatedEdges.iterator();
    while (it2.hasNext())
      fixEstimatedWeights(it2.next(), PPEdge.DUMMY_FOR_TRUNCATED_EDGE);
  }

  private void fixEstimatedWeights(PPEdge removed, int type)
  {
    PPBlock dst   = removed.target();
    PPBlock src   = removed.source();
    PPEdge  edge1 = getEdge(beginBlock, dst, type);
    PPEdge  edge2 = getEdge(src, endBlock, type);

    edge1.addWeight(removed);
    edge2.addWeight(removed);
  }

  /**
   * Compute a maximum spanning tree of the edges in a CFG.
   * @return is a spanning tree of the CFG that gives the maximum
   * weighting of edges.
   */
  private HashSet<PPEdge> findMaxSpanningTree()
  {
    HashMap<PPBlock, PPBlock> parentMap = new HashMap<PPBlock, PPBlock>();

    // Actually, don't add any dummy edges to the max spanning tree!

    if (false) {
      // Create the max spanning tree and add the following edges to it:
      // (1) dummy edges that have BEGIN as their source, and
      // (2) dummy edges that have END as their target and are for
      //     truncated edges.
      Enumeration<PPBlock> eb = blocks.elements();
      while (eb.hasMoreElements()) {
        PPBlock  block = eb.nextElement();
        PPEdge[] edges = block.outgoing();
        for (int i =  0; i < edges.length; i++) {
          PPEdge edge    = edges[i];
          PPBlock source = edge.source();
          PPBlock target = edge.target();

          boolean isDummyFromBegin  = edge.isDummy() && source.equals(beginBlock);
          boolean isTruncDummyToEnd = (edge.isDummy() &&
                                       target.equals(endBlock) &&
                                       (edge.getType() == PPEdge.DUMMY_FOR_BACK_EDGE));
          if (isDummyFromBegin || isTruncDummyToEnd) {
            // Check that adding the edge will not violate the tree
            // property.
            assert !uptreeFind(source, parentMap).equals(uptreeFind(target, parentMap)) :
              "Adding the edge will violate the tree property";

            uptreeUnion(source, target, parentMap);
            // maxSpanningTree.add(edge);
          }
        }
      }
    }

    HashSet<PPEdge> maxSpanningTree = new HashSet<PPEdge>();

    // Sort the edges that are not in the max spanning tree in weight
    // order.

    LinkedList<PPEdge> sortedEdges = new LinkedList<PPEdge>(getEdgesInReverseTopologicalOrder());
    sortedEdges.removeAll(maxSpanningTree);
    Collections.sort(sortedEdges);

    // Check each edge in weight order and add to the MST if the two
    // components are not yet connected.

    Iterator<PPEdge> edgeIter = sortedEdges.iterator();
    while (edgeIter.hasNext()) {
      PPEdge  edge   = edgeIter.next();
      PPBlock source = edge.source();
      PPBlock target = edge.target();
      PPBlock src    = uptreeFind(source, parentMap);
      PPBlock dst    = uptreeFind(target, parentMap);

      if (!src.equals(dst)) {
        uptreeUnion(source, target, parentMap);
        maxSpanningTree.add(edge);
      }
    }

    return maxSpanningTree;
  }

  /**
   * Find the parent of a node in an uptree.  Also, update all parent
   * pointers we encounter along the way.
   * @param block is the node in the uptree.
   * @param parentMap is a map from nodes to their parents in the uptree.
   * @return the parent of the node.
   */
  private PPBlock uptreeFind(PPBlock block, HashMap<PPBlock, PPBlock> parentMap)
  {
    PPBlock parent = parentMap.get(block);
    if (parent == null) {
      parentMap.put(block, block);
      block.setRank(0);
      return block;
    }

    if (!parent.equals(block)) {
    parent = uptreeFind(parent, parentMap);
    parentMap.put(block, parent);
    }

    return parent;
  }

  /**
   * Union two uptrees.
   * @param b1 is a member of the first uptree.
   * @param b2 is a member of the second uptree.
   * @param parentMap 
   */
  private PPBlock uptreeUnion(PPBlock b1, PPBlock b2, HashMap<PPBlock, PPBlock> parentMap)
  {
    b1 = uptreeFind(b1, parentMap); 
    b2 = uptreeFind(b2, parentMap); 
    
    int rank1 = b1.getRank();
    int rank2 = b2.getRank();

    if (rank1 > rank2) {
      PPBlock temp = b1;
      b1 = b2;
      b2 = temp;
    } else if (rank1 == rank2) {
      b2.incRank();
    }

    parentMap.put(b1, b2);

    return b2;
  }

  private void moveIncrements(HashSet<PPEdge> spanningTree)
  {
    HashMap<PPEdge, Long> extraIncMap = new HashMap<PPEdge, Long>();
    
    // Compute extra increments.

    incrementDFS(0, beginBlock, null, spanningTree, extraIncMap);

    // For each edge NOT in the spanning tree, add the extra increment
    // to the current increment.  Otherwise, set the final increment
    // to 0.

    Iterator<PPEdge> edgeIter = getEdgesInReverseTopologicalOrder().iterator();
    while (edgeIter.hasNext()) {
      PPEdge edge = edgeIter.next();

      if (spanningTree.contains(edge))
        edge.setIncrement(0);
      else
        edge.addToIncrement(getExtraInc(edge, extraIncMap));
    }
  }

  private void incrementDFS(long                  events,
                            PPBlock               block,
                            PPEdge                edge,
                            HashSet<PPEdge>       spanningTree,
                            HashMap<PPEdge, Long> extraIncMap)
  {
    PPEdge[] li = block.incoming();
    if (li != null) {
      for (int i = 0; i < li.length; i++) {
        PPEdge f = li[i];
        if (spanningTree.contains(f) && !f.equals(edge)) {
          assert ((edge == null) || !edge.source().equals(f.source())) :
            "Unexpected condition";
          long evnts = (f.dir(edge) * events) + f.getIncrement();
          incrementDFS(evnts, f.source(), f, spanningTree, extraIncMap);
        }
      }
    }

    PPEdge[] lo = block.outgoing();
    if (lo != null) {
      for (int i = 0; i < lo.length; i++) {
        PPEdge f = lo[i];
        if (spanningTree.contains(f) && !f.equals(edge)) {
          if ((edge == null) || !edge.target().equals(f.target())) {
            long evnts = (f.dir(edge) * events) + f.getIncrement();
            incrementDFS(evnts, f.target(), f, spanningTree, extraIncMap);
          }
        }
      }
    }

    incrementDFSEnd(li, events, edge, spanningTree, extraIncMap);
    incrementDFSEnd(lo, events, edge, spanningTree, extraIncMap);
  }

  private void incrementDFSEnd(PPEdge[]              edges,
                               long                  events,
                               PPEdge                edge,
                               HashSet<PPEdge>       spanningTree,
                               HashMap<PPEdge, Long> extraIncMap)
  {
    if (edges == null)
      return;

    for (int i = 0; i < edges.length; i++) {
      PPEdge f = edges[i];
      if (!spanningTree.contains(f)) {
        long inc = getExtraInc(f, extraIncMap);
        Long x   = new Long(inc + f.dir(edge) * events);
        extraIncMap.put(f, x);
      }
    }
  }

  private long getExtraInc(PPEdge edge, HashMap<PPEdge, Long> extraIncMap)
  {
    Long longObj = extraIncMap.get(edge);
    if (longObj == null)
      return 0;

    return longObj.longValue();
  }

  private boolean removeUnreachableEdgesAndBlocks(HashSet<PPEdge> coldEdges)
  {
    // An edge that cannot be reached by BEGIN or END is also cold.

    HashSet<PPEdge>  beginReachable = WorkArea.<PPEdge>getSet("removeUnreachableEdgesAndBlocks");
    HashSet<PPBlock> visited        = WorkArea.<PPBlock>getSet("removeUnreachableEdgesAndBlocks");
    Stack<PPBlock>   wl             = WorkArea.<PPBlock>getStack("removeUnreachableEdgesAndBlocks");

    wl.push(beginBlock);
    while (!wl.empty()) {
      PPBlock block = wl.pop();

      if (!visited.add(block))
        continue;

      PPEdge[] lo = block.outgoing();
      if (lo == null)
        continue;

      for (int i = 0; i < lo.length; i++) {
        PPEdge outgoingEdge = lo[i];
        if (!coldEdges.contains(outgoingEdge)) {
          beginReachable.add(outgoingEdge);
          wl.push(outgoingEdge.target());
        }
      }
    }

    HashSet<PPEdge> endReachable = WorkArea.<PPEdge>getSet("removeUnreachableEdgesAndBlocks");

    visited.clear();
    wl.push(endBlock);
    while (!wl.empty()) {
      PPBlock block = wl.pop();

      if (!visited.add(block))
        continue;

      PPEdge[] li = block.incoming();
      if (li == null)
        continue;

      for (int i = 0; i < li.length; i++) {
        PPEdge incomingEdge = li[i];
        if (!coldEdges.contains(incomingEdge)) {
          endReachable.add(incomingEdge);
          wl.push(incomingEdge.source());
        }
      }
    }

    WorkArea.<PPBlock>returnSet(visited);
    WorkArea.<PPBlock>returnStack(wl);

    // Remove the (true and transitive) cold edges.

    Iterator<PPEdge> edgeIter = getEdgesInForwardTopologicalOrder().iterator();
    while (edgeIter.hasNext()) {
      PPEdge edge = edgeIter.next();
      if (!beginReachable.contains(edge) || !endReachable.contains(edge))
        makeEdgeCold(edge);
    }

   WorkArea.<PPEdge>returnSet(beginReachable);
   WorkArea.<PPEdge>returnSet(endReachable);

    // Remove the unreachable blocks.

    if (!beginBlock.equals(endBlock)) {
      Enumeration<PPBlock> blockIter = blocks.elements();
      while (blockIter.hasMoreElements()) {
        PPBlock block = blockIter.nextElement();

        if (block.equals(beginBlock)) {
          if (!block.hasOutEdges())
            return false;

        } else if (block.equals(endBlock)) {
          if (!block.hasInEdges())
            return false;

        } else if (!block.hasOutEdges())
          makeBlockCold(block);
      }
    }

    return true;
  }

  /**
   * Restore back edges and truncated edges to the graph, and remove
   * dummy edges.  Also apply dummy edge increment to other parts of
   * the graph.
   */
  private void restoreEdges()
  {
    assert !isCyclic : "The graph is already cyclic.";
    
    // Remove the dummy edges.

    Iterator<PPEdge> edgeIter1 = dummyEdges.iterator();
    while (edgeIter1.hasNext())
      removeEdge(edgeIter1.next(), false);

    // Restore the back edges and truncated edges.

    Iterator<PPEdge> edgeIter2 = backEdges.iterator();
    while (edgeIter2.hasNext()) {
      PPEdge edge = edgeIter2.next();
      addEdge(edge);
    }

    Iterator<PPEdge> edgeIter3 = truncatedEdges.iterator();
    while (edgeIter3.hasNext()) {
      PPEdge edge = edgeIter3.next();
      addEdge(edge);
    }

    isCyclic = true;
  }

  /**
   * Compute statically-estimated weights for all edges.  This code is
   * based on code from the Ball-Larus-Ammons path profiler.
   */
  private void computeEstimatedExecutionWeights()
  {
    Iterator<PPBlock> blockIter = getBlocksInForwardTopologicalOrder().iterator();
    while (blockIter.hasNext()) {
      PPBlock block  = blockIter.next();
      double  weight = block.getWeight();

      if (block.isLoopHeader() && !block.equals(beginBlock))
        weight *= ESTIMATED_LOOP_TRIP_COUNT;

      int      numOutgoing = 0;
      double   exitWeight  = 0.0;
      PPEdge[] lo          = block.outgoing();
      if (lo != null) {
        for (int i = 0; i < lo.length; i++) {
          PPEdge          edge            = lo[i];
          Chord           targetChord     = edge.target().firstChord();
          boolean         isLoopExit      = false;
          LoopHeaderChord loopHeaderChord = null;

          if (targetChord.isLoopExit()) {
            loopHeaderChord = ((LoopExitChord) targetChord).getLoopHeader();
            if (loopHeaderChord.isFirstInBasicBlock()) // almost always true
              isLoopExit = true;
          }

          if (isLoopExit) {          
            double x = 1.0 / loopHeaderChord.numLoopExits();
            edge.setWeight(x);
            exitWeight += x;
          } else
            numOutgoing++;
        }
      }

      if (numOutgoing > 0) {
        weight = (weight - exitWeight) / numOutgoing;
        if (lo != null) {
          for (int i = 0; i < lo.length; i++) {
            PPEdge          edge            = lo[i];
            Chord           targetChord     = edge.target().firstChord();
            boolean         isLoopExit      = false;
            LoopHeaderChord loopHeaderChord = null;

            if (targetChord.isLoopExit()) {
              loopHeaderChord = ((LoopExitChord) targetChord).getLoopHeader();
              if (loopHeaderChord.isFirstInBasicBlock()) // almost always true
                isLoopExit = true;
            }
            if (!isLoopExit)
              edge.setWeight(weight);
          }
        }
      }
    }
  }

  /**
   * Remove back edges from the graph, and replace each back edge with
   * two dummy edges.  If the architecture is TRIPS, truncate all loop
   * entrance edges.
   */
  private void makeAcyclic()
  {
    assert isCyclic : "The graph is already acylic";

    // Remove the back edges and add dummy edges instead.  It's
    // important to go in an order that will be the same in different
    // runs so that the dummy edges that are added are ordered the
    // same in blocks' incoming and outgoing sets.

    List<PPEdge>     edgeList = getEdgesInReverseTopologicalOrder();
    Iterator<PPEdge> edgeIter = edgeList.iterator();
    while (edgeIter.hasNext()) {
      PPEdge edge = edgeIter.next();
      if (edge.isBackEdge()) {
        truncateEdge(edge);
      } else if (isPgp()) {
        // The CFG is a PGP CFG, then truncate edges that were
        // truncate by the edge profile CFG.

        PPEdge correspondingEdge = getPgpEdge(edge, true);
        if (pgpEdgeProfileCfg.truncatedEdges.contains(correspondingEdge))
          truncateEdge(edge);
      }
    }

    // If specified (probably because the architecture is TRIPS),
    // truncate all loop entrance edges.

    if (truncateLoopEntrances) {
      // It's important to use the same edge list. Recomputing it will
      // yield no back edges because they are removed above.

      edgeIter = edgeList.iterator();
      while (edgeIter.hasNext()) {
        PPEdge edge = edgeIter.next();
        if (edge.isBackEdge()) {
          PPBlock  loopHead = edge.target();
          PPEdge[] li       = loopHead.incoming();
          assert (li != null) && (li.length == 1) :
            "Expected exactly one incoming edges";
          PPEdge incomingEdge = li[0];
          if (incomingEdge.mayTruncate())
            truncateEdge(incomingEdge);
        }
      }
    }

    isCyclic = false;
  }

  /**
   * Remove cold edges and blocks from the CFG.  Part of
   * profile-guided profiling.
   * @return true if there are edges left after this.
   */
  private boolean removeColdEdges()
  {
    // Find all the cold edges.

    long            progRawFlow    = getBlockFreq2(superBegin);
    PPCfg           edgeProfileCfg = pgpEdgeProfileCfg;
    HashSet<PPEdge> coldEdges      = WorkArea.<PPEdge>getSet("removeColdEdges");

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge edge = block.getOutEdge(i);
        PPEdge edgeProfileEdge = getPgpEdge(edge, false);
        long   edgeFreq        = edgeProfileEdge.getFrequency();

        // To be considered for local coldness, an edge must be a branch
        // edge, and it must be cold compared to the frequency sum of
        // itself and all its branch siblings;.

        double  thresh      = pgpLocalColdEdgeThreshold;
        boolean locallyCold = false;
        if (edgeProfileEdge.isBranchEdge()) {
          long branchEdgeFreqTotal = edgeProfileEdge.source().getBranchEdgeFrequency();
          if ((branchEdgeFreqTotal == 0) ||
              (((double) edgeFreq / branchEdgeFreqTotal) < thresh))
            locallyCold = true;
        }

        // Global coldness is simpler: just look at how hot the edge is
        // relative to total program flow.

        boolean globallyCold = ((double) edgeFreq / progRawFlow) < thresh;

        if (locallyCold || globallyCold)
          coldEdges.add(edge);
      }
    }

    // Make sure that a back edge or truncated edge's two dummy edges
    // are either both in or both out of the cold set.

    Iterator<PPEdge> it1 = backEdges.iterator();
    while (it1.hasNext())
      removeColdEdge(it1.next(), PPEdge.DUMMY_FOR_BACK_EDGE);

    Iterator<PPEdge> it2 = truncatedEdges.iterator();
    while (it2.hasNext())
      removeColdEdge(it2.next(), PPEdge.DUMMY_FOR_TRUNCATED_EDGE);

    removeUnreachableEdgesAndBlocks(coldEdges);

    WorkArea.<PPEdge>returnSet(coldEdges);

    return true;
  }

  private void removeColdEdge(PPEdge edge, int type)
  {
    PPBlock src    = edge.source();
    PPBlock dst    = edge.target();
    PPEdge  dummy1 = getEdge(beginBlock, dst, type);
    PPEdge  dummy2 = getEdge(src, endBlock, type);

    if (coldEdges.contains(dummy1) || coldEdges.contains(dummy2)) {
      coldEdges.add(dummy1);
      coldEdges.add(dummy2);
    }
  }

  /**
   * The first half of the Ball-Larus path profiling algorithm:
   * Make the CFG acyclic, and determine the increments that go on the edges.
   * @param analysisOnly true if path profiling will be used for
   * analyzing a profile, rather than for instrumentation.
   * @return a map from edges to abstract instrumentation (only if
   * analysisOnly is false).
   */
  public HashMap<PPEdge, Instr> doAnalysis(boolean analysisOnly, boolean removeColdEdges)
  {
    HashMap<PPEdge, Instr> aiMap = new HashMap<PPEdge, Instr>();

    // Generate a pre-profiling visualization of the CFG.

    if (generateGraphs)
      generateGraph("before_profiling", GRAPH_MODE_BEFORE_PROFILING, null);

    // Compute estimated execution edge weights using static heuristics.

    computeEstimatedExecutionWeights();
    
    // Remove back edges; add dummy edges; maintain the estimated
    // exeuction weight map.  If this is a PGP CFG, truncate edges
    // that were truncated in the edge profile CFG.

    makeAcyclic();

    // Generate a pre-profiling visualization of the CFG.

    if (generateGraphs)
      generateGraph("acyclic", GRAPH_MODE_BEFORE_PROFILING, null);

    // Populated only when aggressive instrumentation, pushing is
    // disabled (for simulating TPP).

    HashSet<PPBlock> blocksWithIncomingColdEdge = WorkArea.<PPBlock>getSet("doAnalysis");
    HashSet<PPBlock> blocksWithOutgoingColdEdge = WorkArea.<PPBlock>getSet("doAnalysis");

    // If doing profile-guided profiling, remove the cold and obvious
    // edges.

    if (this.isPgp()) {
      if (removeColdEdges) {
        boolean edgesLeft = removeColdEdges;
        if (!edgesLeft) {
          WorkArea.<PPBlock>returnSet(blocksWithIncomingColdEdge);
          WorkArea.<PPBlock>returnSet(blocksWithOutgoingColdEdge);
          return null;
        }
      }

      // Disconnect obvious loops.

      HashSet<PPBlock> blocksInObviousLoops = WorkArea.<PPBlock>getSet("doAnalysis");
      boolean          edgesLeft            = disconnectObviousLoops(blocksInObviousLoops);
      if (!edgesLeft) {
        WorkArea.<PPBlock>returnSet(blocksInObviousLoops);
        WorkArea.<PPBlock>returnSet(blocksWithIncomingColdEdge);
        WorkArea.<PPBlock>returnSet(blocksWithOutgoingColdEdge);
        return null;
      }

      // Pre-compute the incoming and outgoing cold edges of each
      // block so that we can query it efficiently. This is needed so
      // that we can disable aggressive instrumentation pushing (to
      // simulate TPP).

      if (pgpDisableAggressivePushing) {
        Iterator<PPEdge> coldEdgeIter = coldEdges.iterator();
        while (coldEdgeIter.hasNext()) {
          PPEdge coldEdge = coldEdgeIter.next();
          if (!blocksInObviousLoops.contains(coldEdge.target()))
            blocksWithIncomingColdEdge.add(coldEdge.target());

          if (!blocksInObviousLoops.contains(coldEdge.source()))
            blocksWithOutgoingColdEdge.add(coldEdge.source());
        }
      }

      WorkArea.<PPBlock>returnSet(blocksInObviousLoops);

      edgesLeft = removeObviousEdges(blocksWithIncomingColdEdge, blocksWithOutgoingColdEdge);
      if (!edgesLeft) {
        WorkArea.<PPBlock>returnSet(blocksWithIncomingColdEdge);
        WorkArea.<PPBlock>returnSet(blocksWithOutgoingColdEdge);
        return null;
      }

      // Generate visualization of the CFG with cold edges removed.

      if (generateGraphs)
        generateGraph("edges_removed", GRAPH_MODE_BEFORE_PROFILING, null);
    }

    // Do the Ball-Larus path profiling algorithm.

    computeIncrements();

    // Do the Ball event counting algorithm to minimally instrument
    // the CFG output a text file containing the increments on each
    // inter-basic block edge in the CFG.

    if (generateIncrements)
      generateIncrements("before_placement");

    // Generate a post-path-numbering visualization of the CFG.

    if (generateGraphs)
      generateGraph("path_numbering_before_placement", GRAPH_MODE_PATH_NUMBERING, null);

    if (!analysisOnly) {
      // Do the Ball event counting algorithm to minimally instrument
      // the CFG.

      doPlacement();

      // Output a text file containing the increments on each
      // inter-basic block edge in the CFG.

      if (generateIncrements)
        generateIncrements("after_placement");

      // Generate a post-path-numbering visualization of the CFG.

      if (generateGraphs)
        generateGraph("path_numbering_after_placement", GRAPH_MODE_PATH_NUMBERING, null);

      // Fix instrumentation locations; this should be necessary only
      // with profile-guided profiling.

      boolean changed = fixIncrementPlacement(blocksWithIncomingColdEdge, blocksWithOutgoingColdEdge);
      if (changed) {
        if (debuggingOutput)
          System.out.println("Instrumentation placement FIXED!");

        if (generateGraphs)
          generateGraph("fix_increment_placement", GRAPH_MODE_PATH_NUMBERING, null);
      }

      // Assign instrumentation to the CFG's edges.  Don't create
      // actual Chords -- just decide what the Chords will look like.

      aiMap = assignAbstractInstrumentation(blocksWithIncomingColdEdge, blocksWithOutgoingColdEdge);

      // If doing profile-guided profiling, assign instrumentation to
      // the cold edges as necessary.  Restore the cold and obvious
      // edges and blocks.  TODO: move statement inside if statement.

      assignColdInstrumentationAndRestoreEdges(aiMap);
      if (isPgp()) {
      }

      // Generate visualization of the CFG with cold edges removed.
      if (generateGraphs)
        generateGraph("abstract_instrumentation", GRAPH_MODE_ABSTRACT_INSTRUMENTATION, aiMap);

      if (isPgp())
        computeFlowMeasured(aiMap);
    }

    WorkArea.<PPBlock>returnSet(blocksWithIncomingColdEdge);
    WorkArea.<PPBlock>returnSet(blocksWithOutgoingColdEdge);

    // Validate the CFG (unless we're doing profile-guided profiling,
    // which would cause it to fail).

    if (!isPgp())
      validateCFG();

    if (debuggingOutput) {
      System.out.print(  "Routine:                     ");
      System.out.println(getRoutineName());
      System.out.print(  "  Number of blocks:          ");
      System.out.println(commas(numBlocks()));

      if (isPgp()) {
        System.out.print("  Number of cold blocks:     ");
        System.out.println(commas(coldBlocks.size()));
      }

      System.out.print(  "  Number of edges:           ");
      System.out.println(commas(numEdges()));
      System.out.print(  "  Number of back edges:      ");
      System.out.println(commas(backEdges.size()));
      System.out.print(  "  Number of truncated edges: ");
      System.out.println(commas(truncatedEdges.size()));

      if (isPgp()) {
        System.out.print("  Number of cold edges:      ");
        System.out.println(commas(coldEdges.size()));
        System.out.print("  Number of obvious edges:   ");
        System.out.println(commas(obviousEdges.size()));
      }

      System.out.print(  "  Number of paths:           ");
      System.out.println(commas(beginBlock.getNumPaths()));

      if (isPgp()) {
        System.out.print("    w/o cold edge removal:   ");
        System.out.println(commas(pgpEdgeProfileCfg.beginBlock.getNumPaths()));
      }

      System.out.print(  "  Use hashing:               ");
      System.out.println(useHashing());

      if (isPgp()) {
        System.out.print("    w/o cold edge removal:   ");
        System.out.println(pgpEdgeProfileCfg.useHashing());
      }

      System.out.print(  "  Size of path table:        ");
      System.out.println(commas(getPathTableSize()));

      if (isPgp()) {
        System.out.print("    w/o cold edge removal:   ");
        System.out.println(commas(pgpEdgeProfileCfg.getPathTableSize()));
      }
    }

    return aiMap;
  }

  /**
   * The second half of the Ball-Larus path profiling algorithm:
   * Restore the CFG's back edges, place instrumentation on the edges,
   * and insert that instrumentation.  The modifications to the CFG
   * are persistent.
   * @param pathRegister is the variable declaration for the path register.
   * @param counterArray is the variable declaration for the address
   * of the array of counters.
   * @param tempVar is the variable declaration for the temporary
   * variable we used for path profiling.
   * @param profileInfoVar is the variable declaration for the profile
   * information.
   * @param pfit64 is the 64-bit integer type in Scribble.
   * @param hashRoutineDecl is the declaration for __pf_hash_path().
   */
  public void doInstrumentation(VariableDecl pathRegister,
                                VariableDecl counterArray,
                                VariableDecl tempVar,
                                VariableDecl profileInfoVar,
                                IntegerType  pfit64,
                                RoutineDecl  hashRoutineDecl)
  {
    HashMap<PPEdge, Instr> aiMap = null;
    if (getPgp())
      aiMap = PPCfg.getPgpAbstractInstrMap(this);
    else
      aiMap = doAnalysis(false, false);

    // Restore the back edges and truncated edges and remove the dummy edges.

    restoreEdges();
    validateCFG();

    // Assign instrumentation to the edges of the CFG.  dD this
    // optimally: push initialization down and recording up, and
    // combine operations where possible.  instrumentationMap is a map
    // from PPEdge to Chord[].

    HashMap<PPEdge, Chord[]> instrumentationMap = assignRealInstrumentation(aiMap,
                                                           pathRegister,
                                                           counterArray,
                                                           tempVar,
                                                           profileInfoVar,
                                                           pfit64,
                                                           hashRoutineDecl);

    // Generate a pre-instrumentation visualization of the CFG.

    if (generateGraphs)
      generateGraph("real_instrumentation",
                    GRAPH_MODE_REAL_INSTRUMENTATION,
                    instrumentationMap);

    // Apply instrumentation to the edges of the Scribble CFG.  This
    // destroys our PPCfg representation since it adds Scribble Chords
    // along PPEdges, but that's okay because we're done.

    applyInstrumentation(instrumentationMap);
  }

  private boolean disconnectObviousLoops(HashSet<PPBlock> blocksInObviousLoops)
  {
    PPEdge[] lo = beginBlock.outgoing();
    if (lo == null)
      return true;

    Stack<PPEdge>   wl           = WorkArea.<PPEdge>getStack("disconnectObviousLoops");
    HashSet<PPEdge> visited      = WorkArea.<PPEdge>getSet("disconnectObviousLoops");
    int             obvLoopCount = 1;

    for (int i = 0; i < lo.length; i++) {
      PPEdge outgoingEdge = lo[i];

      // Make sure the CFG still contains the outgoing edge!

      if (outgoingEdge.getType() == PPEdge.DUMMY_FOR_BACK_EDGE) {
        HashSet<PPEdge> newColdEdges = considerDisconnectingObviousLoop(outgoingEdge.target());
        if ((newColdEdges != null) && !newColdEdges.isEmpty()) {
          if (generateGraphs)
            generateGraph("obvious_loop_" + obvLoopCount,
                          GRAPH_MODE_ABSTRACT_INSTRUMENTATION,
                          new HashMap<PPEdge, Object>());
          obvLoopCount++;

          // Start over in our search for BEGIN's outgoing dummy (for
          // back edge) edges.

          boolean edgesLeft = removeUnreachableEdgesAndBlocks(newColdEdges);
          if (!edgesLeft) {
            WorkArea.<PPEdge>returnStack(wl);
            WorkArea.<PPEdge>returnSet(visited);
            return false;
          }
          
          // Important: we don't want the loop to be instrumented, but
          // this will occur only if the routine that finds obvious
          // paths finds the entire obvious loop to be obvious. This
          // might not happen if aggressive pushing is disabled, so we
          // need to make sure that aggressive pushing occurs in this
          // loop.

          visited.clear();

          wl.push(outgoingEdge);
          while (!wl.isEmpty()) {
            PPEdge edge = wl.pop();
            if (!visited.contains(edge)) {
              visited.add(edge);
              PPBlock target = edge.target();
              if (!target.equals(endBlock)) {
                blocksInObviousLoops.add(target);
                target.addAllOutEdges(wl);
              }
            }
          }

          // See comment above about starting over.

          lo = beginBlock.outgoing();
          if (lo == null)
            break;

          i = 0;
        }
      }
    }

    WorkArea.<PPEdge>returnStack(wl);
    WorkArea.<PPEdge>returnSet(visited);

    return true;
  }

  private HashSet<PPEdge> considerDisconnectingObviousLoop(PPBlock loopHeader)
  {
    // Make sure the loop tail dummy edge is also not cold.

    LoopHeaderChord lh    = (LoopHeaderChord) loopHeader.firstChord();
    Chord   flt           = lh.getLoopTail().firstInBasicBlock();
    PPBlock loopTail      = new PPBlock(flt, this);
    PPEdge  loopTailDummy = findEdge(loopTail,
                                       endBlock,
                                       PPEdge.DUMMY_FOR_BACK_EDGE);

    if (loopTailDummy == null) {
      assert coldEdges.contains(loopTailDummy) :
        "Expected this edge to be in the CFG";
      return null;
    }

    // Check if the loop has high enough average trip count to be
    // disconnected.

    PPCfg   edgeProfileCfg        = pgpEdgeProfileCfg;
    PPBlock edgeProfileLoopHeader = getPgpBlock(loopHeader);
    long    headerFreq            = edgeProfileCfg.getBlockFreq(edgeProfileLoopHeader);
    PPEdge  edgeProfileDummy      = edgeProfileCfg.getEdge(beginBlock,
                                                           edgeProfileLoopHeader,
                                                           PPEdge.DUMMY_FOR_BACK_EDGE);
    long    dummyEdgeFreq         = (edgeProfileDummy == null) ? 0 : edgeProfileDummy.getFrequency();
    double  avgTripCount          = (double) dummyEdgeFreq / (headerFreq - dummyEdgeFreq);

    if (avgTripCount < pgpLoopDisconnectThreshold)
      // Don't disconnect this loop -- its average trip count is too low.
      return null;

    // get the entry edges
    HashSet<PPEdge> entryEdges = new HashSet<PPEdge>();
    loopHeader.addAllInEdges(entryEdges);

    PPEdge edge = getEdge(beginBlock, loopHeader, PPEdge.DUMMY_FOR_BACK_EDGE);
    entryEdges.remove(edge);
    assert (entryEdges.size() <= 1) : "Did not expect more than one entry edge";

    // Do a forward traversal: place fake instrumentation and find the
    // exit edges, too.

    HashMap<PPEdge, Instr> fakeInstrMap = new HashMap<PPEdge, Instr>();
    HashSet<PPEdge>        exitEdges    = WorkArea.<PPEdge>getSet("considerDisconnectingObviousLoop");
    Stack<PPBlock>         wl           = WorkArea.<PPBlock>getStack("considerDisconnectingObviousLoop");

    wl.add(loopHeader);

    while (!wl.isEmpty()) {
      PPBlock  block = wl.pop();
      PPEdge[] lo    = block.outgoing();
      if (lo == null)
        continue;

      for (int i = 0; i < lo.length; i++) {
        PPEdge  outgoingEdge = lo[i];
        PPBlock tBlk         = outgoingEdge.target();

        // If the edge has the loop tail as a target, then instrument here.

        if (tBlk.equals(loopTail)) {
          fakeInstrMap.put(outgoingEdge, new Instr(Instr.INIT, 0));
          continue;
        }

        // If the edge is a dummy edge, ignore it.

        if (outgoingEdge.isDummy())
          continue;

        // If the edge exits the loop, then add it as an exit edge.

        if (tBlk.firstChord().isLoopExit() &&
            ((LoopExitChord) tBlk.firstChord()).getLoopHeader().equals(lh)) {
          exitEdges.add(outgoingEdge);
          continue;
        }

        // If the edge's target has no other incoming edge, add the
        // next block to the set.

        if (tBlk.numInEdges() == 1) {
          wl.push(tBlk);
          continue;
        }

        // Otherwise, add instrumentation here.

        fakeInstrMap.put(outgoingEdge, new Instr(Instr.INIT, 0));
      }
    }

    // Now do a backward traversal: place more fake instrumentation.

    wl.add(loopTail);

    while (!wl.isEmpty()) {
      PPBlock  block = wl.pop();
      PPEdge[] li    = block.incoming();
      if (li == null)
        continue;

      for (int i = 0; i < li.length; i++) {
        PPEdge incomingEdge = li[i];

        // If this edge already has instrumentation, combine
        // instrumentation.

        if (fakeInstrMap.containsKey(incomingEdge)) {
          fakeInstrMap.put(incomingEdge, new Instr(Instr.RECORD_CONST, 0));
          continue;
        } 

        assert !incomingEdge.source().equals(loopHeader) :
          "Expected to encounter instrumentation before loop header";

        // If the edge is a dummy, don't bother with it.

        if (incomingEdge.isDummy())
          continue;

        // If the edge's source has no other outgoing edge, add the
        // next block to the set.  Be sure to not count the exit
        // edges.

        HashSet<PPEdge> outgoingEdges = new HashSet<PPEdge>();
        incomingEdge.source().addAllOutEdges(outgoingEdges);
        outgoingEdges.removeAll(exitEdges);

        assert !outgoingEdges.isEmpty() : "There should be at least one outgoing edge";

        // If this is the only outgoing edge, add the next block to the set.

        if (outgoingEdges.size() == 1) {
          wl.push(incomingEdge.source());
          continue;
        }

        // Otherwise, we would be putting non-constant path counting
        // instrumentation here, which means the loop body is not
        // obvious.

        return null;
      }
    }

    // We want the entry and exit edges removed.

    HashSet<PPEdge> edgesToRemove = new HashSet<PPEdge>(entryEdges);
    edgesToRemove.addAll(exitEdges);

    WorkArea.<PPEdge>returnSet(exitEdges);
    WorkArea.<PPBlock>returnStack(wl);

    return edgesToRemove;
  }

  /**
   * Assigns increments to the edges of the acyclic CFG.
   */
  private void computeIncrements()
  {
    // Repeatedly try to assign increments.  Every time we fail,
    // truncate the edges where failure occurred.

    long    truncationThreshold = Long.MAX_VALUE;
    boolean tooManyPaths;
    do {
      tooManyPaths = false;
      try {  // Run the numbering algorithm.
        countPaths(beginBlock, truncationThreshold);
      } catch (TooManyPathsException tmpex) {
        // Copy the list of outgoing edges of the block that caused
        // the exception.

        LinkedList<PPEdge> truncationList = new LinkedList<PPEdge>();
        tmpex.getBlock().addAllOutEdges(truncationList);

        // Now (try to) truncate the edges.

        boolean          truncationOccurred = false;
        Iterator<PPEdge> iter               = truncationList.iterator();
        while (iter.hasNext()) {
          PPEdge edge = iter.next();
          if (edge.mayTruncate()) {
            truncateEdge(edge);
            truncationOccurred = true;
          }
        }

        // If truncation occurred, then we try again with the maximum
        // truncation threshold.  Otherwise, we decrease the threshold
        // and try again.

        truncationThreshold = truncationOccurred ? Long.MAX_VALUE : truncationThreshold / 2;

        // Specify that we want to call countPaths() again.

        tooManyPaths = true;

        // Reset num paths and increments for each edge.

        resetNumPathsAndIncrements();
      }
    } while (tooManyPaths);
  }

  /**
   * Count the number of acyclic paths that start at each block, and
   * set the increment along each edge.  This is Figure 5 in the
   * Ball-Larus paper.
   */
  private void countPaths(final PPBlock v, long truncationThreshold) throws TooManyPathsException
  {
    // Only count the number of paths if the block has not already
    // been visited.

    if (v.isNumPathsSet())
      return;

    v.setNumPaths(0); // To avoid infinite recursion.

    // For each edge in reverse topological order -- this means that
    // we don't want to visit an edge without first visiting all of
    // its successors.

    // Call countPaths on each of v's successors w. This ordering
    // should be reverse topological order.

    PPEdge[] lo = v.outgoing();
    if (lo != null) {
      for (int i = 0; i < lo.length; i++) {
        PPBlock w = lo[i].target();
        countPaths(w, truncationThreshold);
      }
    }

    v.resetNumPaths(); // To avoid infinite recursion.

    // Now do the Ball-Larus algorithm.

    if (v.numOutEdges() == 0) { // If v is a leaf.
      assert v.isEndBlock() : "A block other than the end block is a leaf!";
      v.setNumPaths(1);  // NumPaths(v) = 1;
    } else {
      v.setNumPaths(0); // NumPaths(v) = 0;

      // For each edge v->w, order by the number of paths to minimize
      // increments.

      if (lo != null) {
        Vector<PPEdge> edgeList = new Vector<PPEdge>();
        v.addAllOutEdges(edgeList);
        Collections.sort(edgeList, new Comparator<PPEdge>() {
            public int compare(PPEdge o1, PPEdge o2) {
              PPEdge e1 = o1;
              PPEdge e2 = o2;

              // If specified, order by hotness (from hot to cold)
              // in order to put the nonzero increments on the colder edges.

              PPCfg cfg = v.getCfg();
              if (cfg.isPgp() && pgpEdgeOrder) {
                long freq1 = cfg.getPgpEdge(e1, false).getFrequency();
                long freq2 = cfg.getPgpEdge(e2, false).getFrequency();
                if (freq1 < freq2)
                  return 1;
                if (freq1 > freq2)
                  return -1;

                // If the frequencies are the
                // same, then just use the
                // static ordering criteria
                // below.
              }

              long np1 = e1.target().getNumPaths();
              long np2 = e2.target().getNumPaths();
              if (np1 < np2)
                return -1;

              if (np1 == np2)
                return 0;

              return 1;
            }
          });

        int  l = edgeList.size();
        long n = v.getNumPaths();
        for (int i = 0; i < l; i++) {
          PPEdge edge = edgeList.get(i);

          // Val(edge) = NumPaths(v)

          edge.setIncrement(n);

          // NumPaths(v) = NumPaths(v) + NumPaths(w)

          PPBlock w = edge.target();

          n += w.getNumPaths();
          v.setNumPaths(n);

          // Check if we exceeded the truncation threshold (or if we
          // overflowed).

          if ((n > truncationThreshold) || (n < 0))
            throw new TooManyPathsException(v);
        }
      }
    }
  }

  /**
   * Push increments down and up as much as possible.  This is really
   * only necessary for profile-guided profiling, when it's possible
   * for increments to not be placed as high or low as they can be.
   * @return True if the increments changed at all; false otherwise.
   */
  private boolean fixIncrementPlacement(HashSet<PPBlock> blocksWithIncomingColdEdge,
                                        HashSet<PPBlock> blocksWithOutgoingColdEdge)
  {
    boolean         changed   = false;
    HashSet<PPEdge> stopEdges = WorkArea.<PPEdge>getSet("fixIncrementPlacement");
    Stack<PPEdge>   wl        = WorkArea.<PPEdge>getStack("fixIncrementPlacement");

    beginBlock.addAllOutEdges(wl);

    while (!wl.isEmpty()) {
      PPEdge  edge   = wl.pop();
      PPBlock target = edge.target();

      if (target.equals(endBlock) ||
          (target.numInEdges() > 1) ||
          blocksWithIncomingColdEdge.contains(target)) {

        // We're at a merge or the end of a routine; stop here.

        stopEdges.add(edge);
        continue;
      } 

      long currInc = edge.getIncrement();
      if (currInc != 0) {
        // This edge should not have a nonzero increment; apply it to
        // the outgoing edges of the target instead.

        PPEdge[] lo = target.outgoing();
        if (lo != null) {
          for (int i = 0; i < lo.length; i++) {
            PPEdge outgoingEdge = lo[i];
            outgoingEdge.setIncrement(outgoingEdge.getIncrement() + currInc);
            wl.push(outgoingEdge); // add to the worklist
          }
        }

        edge.setIncrement(0);
        changed = true;
        continue;
      }

      target.addAllOutEdges(wl);
    }

    endBlock.addAllInEdges(wl);

    while (!wl.isEmpty()) {
      PPEdge  edge   = wl.pop();
      PPBlock source = edge.source();

      if (stopEdges.contains(edge) ||
          (source.numOutEdges() > 1) ||
          blocksWithOutgoingColdEdge.contains(source))
        continue; // We're at a stopping point; go no further.

      long currInc = edge.getIncrement();
      if (currInc != 0) {
        // This edge should not have a nonzero increment; apply it to
        // the incoming edges of the source instead.

        PPEdge[] li = source.incoming();
        if (li != null) {
          for (int i = 0; i < li.length; i++) {
            PPEdge incomingEdge = li[i];
            incomingEdge.setIncrement(incomingEdge.getIncrement() + currInc);
            wl.push(incomingEdge); // add to the worklist
          }
        }

        edge.setIncrement(0);
        changed = true;
        continue;
      }

      source.addAllInEdges(wl);
    }

    WorkArea.<PPEdge>returnStack(wl);
    WorkArea.<PPEdge>returnSet(stopEdges);

    return changed;
  }
  
  /**
   * Decide what instrumentation goes where.  This is done optimially
   * using Figure 8 from the Ball-Larus paper.
   * @return A map from edges to abstract instrumentation.
   */
  private HashMap<PPEdge, Instr> assignAbstractInstrumentation(HashSet<PPBlock> blocksWithIncomingColdEdge,
                                                               HashSet<PPBlock> blocksWithOutgoingColdEdge)
  {
    HashMap<PPEdge, Instr> aiMap = new HashMap<PPEdge, Instr>();
    
    // Create a working set and add BEGIN to it.

    HashSet<PPBlock> ws = WorkArea.<PPBlock>getSet("assignAbstractInstrumentation");

    ws.add(beginBlock);

    while (!ws.isEmpty()) {
      PPBlock b = ws.iterator().next();
      ws.remove(b);

      // For each outgoing edge of the block.

      PPEdge[] lo = b.outgoing();
      if (lo != null) {
        for (int i = 0; i < lo.length; i++) {
          PPEdge e = lo[i];
        
          // If a nonzero increment, r = inc.

          long currInc = e.getIncrement();
          if (currInc != 0) {
            aiMap.put(e, new Instr(Instr.INIT, currInc));
            continue;
          }

          // If the edge is the only incoming edge of its target block,
          // and the target is not END, then add the target block to the
          // working set.

          PPBlock target = e.target();
          if ((target.numInEdges() == 1) &&
              !blocksWithIncomingColdEdge.contains(target) &&
              !target.equals(endBlock)) {
            ws.add(target);
            continue;
          }

          // r = 0

          aiMap.put(e, new Instr(Instr.INIT, 0));
        }
      }
    }

    // Add EXIT to the working set.

    ws.add(endBlock);

    while (!ws.isEmpty()) {
      PPBlock b = ws.iterator().next();
      ws.remove(b);

      // For each incoming edge of the block (except back edges and
      // truncated edges).

      PPEdge[] li = b.incoming();
      if (li != null) {
        for (int i = 0; i < li.length; i++) {
          PPEdge e = li[i];

          // If there is already instrumentation on e
          // (which must be  r = inc),
          // change the instrumentation to count[inc]++.

          if (aiMap.containsKey(e)) {
            // Clear the current instrumentation and put in different
            // instrumentation.
            aiMap.remove(e);
            aiMap.put(e, new Instr(Instr.RECORD_CONST, e.getIncrement()));
            continue;
          }

          // If there is a nonzero increment at the edge, count[r + inc]++.

          long currInc = e.getIncrement();
          if (currInc != 0) {
            aiMap.put(e, new Instr(Instr.RECORD_VAR, currInc));
            continue;
          }

          // If e is the only outgoing edge of its source block, add the
          // source block to the working set.

          PPBlock source = e.source();
          if ((source.numOutEdges() == 1) &&
              !blocksWithOutgoingColdEdge.contains(source)) {
            ws.add(source);
            continue;
          }

          // count[r]++

          aiMap.put(e, new Instr(Instr.RECORD_VAR, 0));
        }
      }
    }

    WorkArea.<PPBlock>returnSet(ws);

    // Finally consider all edges with nonzero increment that have not
    // yet been instrumented.

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge edge    = block.getOutEdge(i);
        long   currInc = edge.getIncrement();
        if ((currInc != 0) && !aiMap.containsKey(edge))
          aiMap.put(edge, new Instr(Instr.INCREMENT, currInc));
      }
    }

    return aiMap;
  }

  /**
   * Remove edges that would get instrumenation of the form
   * count[constant]++.  Also mark all edges on obvious paths as
   * obvious and removes them.
   * @return True if there are edges left (and therefore there's
   * something to instrument; false otherwise.
   */
  private boolean removeObviousEdges(HashSet<PPBlock> blocksWithIncomingColdEdge,
                                     HashSet<PPBlock> blocksWithOutgoingColdEdge)
  {
    // First we need to find the obvious edges.

    HashMap<PPEdge, Instr> fakeInstrMap = new HashMap<PPEdge, Instr>();

    // Create a working set and add BEGIN to it.

    HashSet<PPBlock> ws = WorkArea.<PPBlock>getSet("removeObviousEdges");

    ws.add(beginBlock);

    while (!ws.isEmpty()) {
      PPBlock b = ws.iterator().next();
      ws.remove(b);

      // For each outgoing edge of the block.

      PPEdge[] lo = b.outgoing();
      if (lo == null)
        continue;

      for (int i = 0; i < lo.length; i++) {
        PPEdge  e      = lo[i];
        PPBlock target = e.target();

        // If we reach a merge, r += inc

        if ((target.numInEdges() == 1) &&
            !blocksWithIncomingColdEdge.contains(target) &&
            !target.equals(endBlock)) {
          ws.add(target);
          continue;
        }

        // else r = 0

        fakeInstrMap.put(e, new Instr(Instr.INIT, 0));
      }
    }

    // Add EXIT to the working set.

    ws.add(endBlock);

    while (!ws.isEmpty()) {
      PPBlock b = ws.iterator().next();
      ws.remove(b);

      // For each incoming edge of the block (except back edges and
      // truncated edges).

      PPEdge[] li = b.incoming();
      if (li == null)
        continue;

      for (int i = 0; i < li.length; i++) {
        PPEdge e = li[i];

        // If there is already instrumentation on e (which must be r = inc),
        // change the instrumentation to count[inc]++

        if (fakeInstrMap.containsKey(e)) {
          // Clear the current instrumentation and put in different
          // instrumentation.
          fakeInstrMap.remove(e);
          fakeInstrMap.put(e, new Instr(Instr.RECORD_CONST, 0));
          continue;
        }

        // If there is a nonzero increment at the edge, count[r + inc]++

        PPBlock source = e.source();
        if ((source.numOutEdges() == 1) &&
            !blocksWithOutgoingColdEdge.contains(source)) {
          ws.add(source);
          continue;
        }

        // else count[r]++

        fakeInstrMap.put(e, new Instr(Instr.RECORD_VAR, 0));
      }
    }

    WorkArea.<PPBlock>returnSet(ws);

    // Now use this fake instrumentation to identify the obvious edges.

    HashSet<PPEdge>  obviousEdges  = WorkArea.<PPEdge>getSet("removeObviousEdges");
    HashSet<PPBlock> obviousBlocks = WorkArea.<PPBlock>getSet("removeObviousEdges");

    Iterator<PPEdge> edgeIter = fakeInstrMap.keySet().iterator();
    while (edgeIter.hasNext()) {
      PPEdge edge  = edgeIter.next();
      Instr  instr = fakeInstrMap.get(edge);

      if (instr.type() == Instr.RECORD_CONST) {
        // Go up from the obvious edge until we find a non-obvious
        // merge.

        PPEdge tempEdge = edge;
        while (true) {
          obviousEdges.add(tempEdge);

          // Look at all outgoing edges of the edge's source.

          PPBlock  source     = tempEdge.source();
          boolean  allObvious = true;
          PPEdge[] lo         = source.outgoing();
          if (lo != null) {
            for (int i = 0; i < lo.length; i++) {
              PPEdge outgoingEdge = lo[i];
              if (!obviousEdges.contains(outgoingEdge)) {
                allObvious = false;
                break;
              }
            }
          }

          // If not all obvious, don't go up anymore.

          if (!allObvious || source.equals(beginBlock))
            break;

          // Make the merge block obvious.

          obviousBlocks.add(source);

          PPEdge[] li = source.incoming();
          if (li == null)
            break;

          tempEdge = li[0];
        }

        // Go down from the obvious edge until we find a non-obvious
        // merge.

        tempEdge = edge;
        while (true) {
          obviousEdges.add(tempEdge);

          // Look at all outgoing edges of the edge's source.

          PPBlock  target     = tempEdge.target();
          boolean  allObvious = true;
          PPEdge[] li         = target.incoming();
          if (li != null) {
            for (int i = 0; i < li.length; i++) {
              PPEdge incomingEdge = li[i];
              if (!obviousEdges.contains(incomingEdge)) {
                allObvious = false;
                break;
              }
            }
          }

          // If not all obvious, don't go up anymore.

          if (!allObvious || target.equals(endBlock))
            break;

          // Make the merge block obvious.

          obviousBlocks.add(target);

          PPEdge[] lo = target.outgoing();
          if (lo == null)
            break;

          tempEdge = lo[0];
        }
      }
    }
    
    // Finally, remove the obvious blocks and edges.

    Iterator<PPEdge> edgeIter2 = obviousEdges.iterator();
    while (edgeIter2.hasNext()) {
      PPEdge edge = edgeIter2.next();
      makeEdgeObvious(edge);
    }

    Iterator<PPBlock> blockIter = obviousBlocks.iterator();
    while (blockIter.hasNext()) {
      PPBlock block = blockIter.next();
      makeBlockObvious(block);
    }

    WorkArea.<PPEdge>returnSet(obviousEdges);
    WorkArea.<PPBlock>returnSet(obviousBlocks);

    // Return true if there are edges left and false if we removed all
    // of them.

    if (beginBlock.equals(endBlock))
      return true;

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      if (numEdges > 0)
        return true;
    }

    return false;
  }

  private int numEdges()
  {
    int                  num = 0;
    Enumeration<PPBlock> eb  = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock block = eb.nextElement();
      num += block.numOutEdges();
    }
    return num;
  }

  /**
   * Assign instrumentation to cold edges as necessary.
   * Then restore cold and obvious edges and blocks.
   * @param aiMap A map from edges to abstract instrumentation (Instr).
   */
  private void assignColdInstrumentationAndRestoreEdges(HashMap<PPEdge, Instr> aiMap)
  {
    // Compute for each (non-obvious) block whether it is before or
    // after path initialization and whether it is before or after
    // path counting.

    HashSet<PPBlock>  afterInitBlocks  = WorkArea.<PPBlock>getSet("assignColdInstrumentationAndRestoreEdges");
    HashSet<PPBlock>  afterCountBlocks = WorkArea.<PPBlock>getSet("assignColdInstrumentationAndRestoreEdges");

    Iterator<PPBlock> blockIter = getBlocksInForwardTopologicalOrder().iterator();
    while (blockIter.hasNext()) {
      PPBlock  block = blockIter.next();
      PPEdge[] li    = block.incoming();

      if (li == null)
        continue;

      PPEdge  firstEdge = li[0];
      PPBlock source    = firstEdge.source();
      if (afterInitBlocks.contains(source)) {
        afterInitBlocks.add(block);
        if (afterCountBlocks.contains(source))
          afterCountBlocks.add(block);
      }

      Instr instr = aiMap.get(firstEdge);
      if (instr == null)
        continue;

      switch (instr.type()) {
      case Instr.INIT:
        afterInitBlocks.add(block);
        break;
      case Instr.RECORD_CONST:
      case Instr.RECORD_VAR:
        afterInitBlocks.add(block);
        afterCountBlocks.add(block);
        break;
      }
    }

    // Check that afterInitBlocks is a superset of afterCountBlocks.

    HashSet<PPBlock> tempSet = new HashSet<PPBlock>(afterCountBlocks);
    tempSet.removeAll(afterInitBlocks);
    assert tempSet.isEmpty() : "Expected one set to be superset of another.";

    // Edges that may contribute to path overcounting.

    incomingOvercountEdges = new HashSet<PPEdge>(); // The set of cold overcount edges that have hot sources.
    outgoingOvercountEdges = new HashSet<PPEdge>(); // The set of cold overcount edges that have hot targets.

    // Find cold edges that have hot (and non-obvious) targets. If
    // such an edge is before path initialization, then taking this
    // edge may contribute to overcount.

    Iterator<PPEdge> coldEdgeIter = coldEdges.iterator();
    while (coldEdgeIter.hasNext()) {
      PPEdge  coldEdge = coldEdgeIter.next();
      PPBlock target   = coldEdge.target();
      if (containsBlock(target) && !afterInitBlocks.contains(target))
          incomingOvercountEdges.add(coldEdge);
    }

    // Find cold edges that have hot (and non-obvious) sources. If
    // such an edge is after path counting, then taking this edge may
    // contribute to overcount.

    Iterator<PPEdge> coldEdgeIter2 = coldEdges.iterator();
    while (coldEdgeIter2.hasNext()) {
      PPEdge  coldEdge = coldEdgeIter2.next();
      PPBlock source   = coldEdge.source();
      if (containsBlock(source) && afterCountBlocks.contains(source))
        outgoingOvercountEdges.add(coldEdge);
    }

    // Compute the possible ranges of the path register at each block.

    Iterator<PPBlock> blockIter2 = getBlocksInForwardTopologicalOrder().iterator();
    while (blockIter2.hasNext()) {
      PPBlock block = blockIter2.next();

      if (afterInitBlocks.contains(block) && !afterCountBlocks.contains(block)) {
        PPEdge[] li = block.incoming();
        if (li == null)
          continue;

        for (int i = 0; i < li.length; i++) {
          PPEdge  incomingEdge = li[i];
          PPBlock edgeRange    = incomingEdge.source();
          Instr   instr        = aiMap.get(incomingEdge);
          long    low          = edgeRange.getLowRange();
          long    high         = edgeRange.getHighRange();
          if (instr != null) {
            switch (instr.type()) {
            case Instr.INIT:
              low = instr.val();
              high = instr.val();
              break;
            case Instr.INCREMENT:
              low += instr.val();
              high += instr.val();
              break;
            }
          }

          block.unionRange(low, high);
        }
      }
    }

    // TODO: remove

    if (generateGraphs)
      generateGraph("ranges_without_all_edges", GRAPH_MODE_ABSTRACT_INSTRUMENTATION_WITH_RANGES, aiMap);

    // Find cold edges that have hot targets. If such an edge is
    // between path initialization and path counting, then assign it
    // instrumentation.

    long overallMaxRecordedVal = beginBlock.getNumPaths() - 1;

    Iterator<PPEdge> coldEdgeIter3 = coldEdges.iterator();
    while (coldEdgeIter3.hasNext()) {
      PPEdge  coldEdge = coldEdgeIter3.next();
      PPBlock target   = coldEdge.target();
      if (containsBlock(target)) {
        if (afterInitBlocks.contains(target) && !afterCountBlocks.contains(target)) {
          // Assign a value such that the recorded path number will be >= numPaths.
          long edgeVal = beginBlock.getNumPaths() + target.getLowRange();
          aiMap.put(coldEdge, new Instr(Instr.INIT, edgeVal));
          // Figure out the maximum value of the recorded path if the cold edge is taken.
          long maxRecordedVal = edgeVal + beginBlock.getNumPaths() - 1 - target.getHighRange();
          if (maxRecordedVal > overallMaxRecordedVal)
            overallMaxRecordedVal = maxRecordedVal;
        }
      }
    }

    WorkArea.<PPBlock>returnSet(afterInitBlocks);
    WorkArea.<PPBlock>returnSet(afterCountBlocks);

    // Set the expanded table size.

    expandedTableSize = overallMaxRecordedVal + 1;

    // Restore cold and obvious blocks and edges.

    Iterator<PPBlock> blockIter3 = obviousBlocks.iterator();
    while (blockIter3.hasNext()) {
      PPBlock block = blockIter3.next();
      restoreObviousBlock(block);
    }

    Iterator<PPBlock> blockIter4 = coldBlocks.iterator();
    while (blockIter4.hasNext()) {
      PPBlock block = blockIter4.next();
      restoreColdBlock(block);
    }

    Iterator<PPEdge> edgeIter = obviousEdges.iterator();
    while (edgeIter.hasNext()) {
      PPEdge edge = edgeIter.next();
      restoreObviousEdge(edge);
    }

    Iterator<PPEdge> edgeIter2 = coldEdges.iterator();
    while (edgeIter2.hasNext()) {
      PPEdge edge = edgeIter2.next();
      restoreColdEdge(edge);
    }

    if (generateGraphs)
      generateGraph("ranges_with_all_edges", GRAPH_MODE_ABSTRACT_INSTRUMENTATION_WITH_RANGES, aiMap);
  }

  /**
   * @param aiMap A map from edges to abstract instrumentation (Instr). 
   */
  private void computeFlowMeasured(HashMap<PPEdge, Instr> aiMap)
  {
    PPCfg edgeProfileCfg = pgpEdgeProfileCfg;

    // Get the paths taken through this CFG.

    HashSet<Path> pathsTaken = getActualPaths(superBegin, superEnd, edgeProfileCfg);

    // Three sets of paths that will be useful for figuring out how
    // much flow is measured with our techniques.

    // The set of paths in the CFG that will have their flow measured
    // by path profiling.

    measuredPaths = new HashSet<Path>();

    // The set of paths in the CFG that will have their flow overcounted.

    HashMap<Vector<PPEdge>, Vector<FBPair>> overcountedPathMap = new HashMap<Vector<PPEdge>, Vector<FBPair>>();
    HashSet<Path> definiteFlowPathsUsed = WorkArea.<Path>getSet("computeFlowMeasured");

    // Check whether each path is hot or cold; if cold, figure out
    // whether it will overcount the profile; also construct an actual
    // measured path profile.

    Iterator<Path> pathIter = pathsTaken.iterator();
    while (pathIter.hasNext()) {
      Path           path     = pathIter.next();
      Vector<PPEdge> edgeList = edgeProfileCfg.getEdgesOnPath(path.pathNum());
      long           flow     = path.pair().flow();

      Vector<Vector<PPEdge>> actualPathsCounted = getActualPathsCounted(edgeList, aiMap);
      boolean         takenPathCounted   = false;
      int l = actualPathsCounted.size();
      for (int i = 0; i < l; i++) {
        Vector<PPEdge> countedPath = actualPathsCounted.get(i);
        int            numbr       = getNumBranches(countedPath);
        FBPair         newPair     = new FBPair(flow, numbr);

        // Are the path counted and the path taken the same?

        if (countedPath.equals(edgeList)) {
          Path newPath = new Path(countedPath, newPair);
          measuredPaths.add(newPath);
          takenPathCounted = true;
          continue;
        }

        Vector<FBPair> pairList = overcountedPathMap.get(countedPath);
        if (pairList == null)
          pairList = new Vector<FBPair>();

        pairList.add(newPair);
        overcountedPathMap.put(countedPath, pairList);
      }

      if (!takenPathCounted)
        definiteFlowPathsUsed.add(path);
    }

    // The set of paths in the CFG that will have their flow overcounted.

    overcountedPaths = new HashSet<Path>();

    // Convert the overcounted paths map into a set.

    Iterator<Vector<PPEdge>> pathIter2 = overcountedPathMap.keySet().iterator();
    while (pathIter2.hasNext()) {
      Vector<PPEdge>  actualPathCounted = pathIter2.next();
      Vector<FBPair>  pairList          = overcountedPathMap.get(actualPathCounted);
      long            totalFlow         = 0;
      int             numBranches       = 0;

      int l = pairList.size();
      for (int i = 0; i < l; i++) {
        FBPair pair = pairList.get(i);
        totalFlow += pair.flow();
        numBranches = pair.numBranches();
      }

      FBPair pair    = new FBPair(totalFlow, numBranches);
      Path   newPath = new Path(actualPathCounted, pair);
      overcountedPaths.add(newPath);
    }

    // Retain the definite flow paths that were actually taken.

    definiteFlowPaths.retainAll(definiteFlowPathsUsed);
    WorkArea.<Path>returnSet(definiteFlowPathsUsed);
  }

  /**
   * Return the list of paths (each path is a list of edges) that are
   * actually counted.
   */
  private Vector<Vector<PPEdge>> getActualPathsCounted(Vector<PPEdge> edgeList, HashMap<PPEdge, Instr> aiMap)
  {
    Vector<Vector<PPEdge>> actualPathsCounted = new Vector<Vector<PPEdge>>();
    Vector<PPEdge>         currentPath        = null;

    boolean pathRegSet                      = false;
    boolean encounteredInstrumentedColdEdge = false;
    long    pathRegVal                      = 0;

    int l = edgeList.size();
    for (int i = 0; i < l; i++) {
      PPEdge edgeProfileEdge = edgeList.get(i);
      PPEdge currEdge        = getPgpEdgeInverse(edgeProfileEdge, false);

      // Keep track of the path register.

      Instr instr = aiMap.get(currEdge);
      if (instr == null) {
        if (currentPath != null)
          currentPath.add(edgeProfileEdge);
        continue;
      }

        switch (instr.type()) {
          case Instr.INIT:
            pathRegVal = instr.val();
            pathRegSet = true;
            currentPath = new Vector<PPEdge>();
            currentPath.add(edgeProfileEdge);
            if (coldEdges.contains(currEdge)) {
              encounteredInstrumentedColdEdge = true;
            } else {
              encounteredInstrumentedColdEdge = false;
              PPEdge tempEdge = currEdge;
              while (!tempEdge.source().equals(beginBlock)) {
                tempEdge = getHotEdge(tempEdge.source().incoming());
                currentPath.setElementAt(getPgpEdge(tempEdge, false), 0);
              }
            }
            break;
          case Instr.INCREMENT:
            assert pathRegSet : "Unexpected";
            pathRegVal += instr.val();
            currentPath.add(edgeProfileEdge);
            break;
          case Instr.RECORD_VAR:
            if (!pathRegSet) {
              // TODO: do better than this quick fix!
              Stack<PPEdge> wl       =  WorkArea.<PPEdge>getStack("getActualPathsCounted");
              Instr         oldInstr = aiMap.get(currEdge);

              aiMap.remove(currEdge);
              wl.push(currEdge);

              while (!wl.isEmpty()) {
                PPEdge tempEdge = wl.pop();

                if (aiMap.containsKey(tempEdge)) {
                  Instr tempInstr = aiMap.get(tempEdge);
                  long  val       = oldInstr.val() + tempInstr.val();
                  switch (tempInstr.type()) {
                    case Instr.INIT:
                      Instr init = new Instr(Instr.RECORD_CONST, val);
                      aiMap.put(tempEdge, init);
                      break;
                    case Instr.INCREMENT:
                      Instr inc = new Instr(Instr.RECORD_VAR, val);
                      aiMap.put(tempEdge, inc);
                      break;
                    default:
                      throw new scale.common.InternalError("Unexpected");
                  }
                  continue;
                }

                PPBlock source = tempEdge.source();
                if (source.numOutEdges() > 1) {
                  Instr inst = new Instr(Instr.RECORD_VAR, oldInstr.val());
                  aiMap.put(tempEdge, inst);
                  continue;
                }

                source.addAllInEdges(wl);
              }

              WorkArea.<PPEdge>returnStack(wl);

              // We don't want to record a path, so break out of the case.

              break;
            }

            pathRegVal += instr.val();
            assert (pathRegVal >= 0) && (pathRegVal < expandedTableSize) : "Invalid path value will be recorded";

            currentPath.add(edgeProfileEdge);

            PPEdge tempEdge = currEdge;
            while (!tempEdge.target().equals(endBlock)) {
              tempEdge = getHotEdge(tempEdge.target().outgoing());
              currentPath.add(getPgpEdge(tempEdge, false));
            }

            if (pathRegVal < beginBlock.getNumPaths()) {
              assert !encounteredInstrumentedColdEdge : "Unexpected";
              actualPathsCounted.add(currentPath);
            } else {
              assert encounteredInstrumentedColdEdge : "Unexpected";
            }
            currentPath = null;
            pathRegSet = false;
            encounteredInstrumentedColdEdge = false;
            break;
          default:
            throw new scale.common.InternalError("Other instrumentation not expected");
        }
    }

    return actualPathsCounted;
  }

  private PPEdge getHotEdge(PPEdge[] edgeList)
  {
    if (edgeList == null)
      return null;

    PPEdge   hotEdge  = null;
    for (int i = 0; i < edgeList.length; i++) {
      PPEdge edge = edgeList[i];
      if (!coldEdges.contains(edge) && !obviousEdges.contains(edge)) {
        assert (hotEdge == null) : "Two hot edges unexpected";
        hotEdge = edge;
      }
    }

    return hotEdge;
  }

  /**
   * Use an edge's abstract instrumentation to assign real
   * instrumentation (i.e., with Chords).
   * @param aiMap A map from edges to abstract instrumentation (Instr).
   * @param pathRegister The variable declaration for the path register.
   * @param counterArray The variable declaration for the address of
   * the array of counters.
   * @param tempVar The variable declaration for the temporary
   * variable we use for path profiling.
   * @param profileInfoVar The variable declaration for the profile
   * information.
   * @param pfit64 The 64-bit integer type in Scribble.
   * @param hashRoutineDecl The declaration for __pf_hash_path().
   * @return A map from PPEdge to Chord[].
   */
  private HashMap<PPEdge, Chord[]> assignRealInstrumentation(HashMap<PPEdge, Instr> aiMap,
                                            VariableDecl           pathRegister,
                                            VariableDecl           counterArray,
                                            VariableDecl           tempVar,
                                            VariableDecl           profileInfoVar,
                                            IntegerType            pfit64,
                                            RoutineDecl            hashRoutineDecl)
  {
    HashMap<PPEdge, Chord[]> riMap = new HashMap<PPEdge, Chord[]>(); // The real instrumentation map: from PPEdge to Chord[]

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge edge      = block.getOutEdge(i);
        Vector<Instr> instrList = new Vector<Instr>();
        if (edge.isBackEdge() || truncatedEdges.contains(edge)) {
          int     dummyType = (backEdges.contains(edge) ?
                               PPEdge.DUMMY_FOR_BACK_EDGE :
                               PPEdge.DUMMY_FOR_TRUNCATED_EDGE);
          PPBlock src    = edge.source();
          PPBlock dst    = edge.target();
          PPEdge  dummy1 = findEdge(beginBlock, dst, dummyType);
          PPEdge  dummy2 = findEdge(src, endBlock, dummyType);

          // Add the dummy edge instrumentation to the back edge or
          // truncated edge.  Do the second dummy edge, then the first.

          Instr instr2 = null;
          Instr instr1 = null;
          if (dummy2 != null)
            instr2 = aiMap.get(dummy2);
          if (dummy1 != null)
            instr1 = aiMap.get(dummy1);
          if (instr2 != null)
            instrList.add(instr2);

          if (instr1 != null)
            instrList.add(instr1);
        } else {
          Instr instr = aiMap.get(edge);
          if (instr != null)
            instrList.add(instr);
        }

        if (!instrList.isEmpty()) {
          Chord[] allChords  = null;
          boolean useHashing = useHashing();
          int     l          = instrList.size();
          for (int j = 0; j < l; j++) {
            Instr   instr            = instrList.get(j);
            Chord[] additionalChords = instr.genRealInstr(pathRegister,
                                                          counterArray,
                                                          tempVar,
                                                          profileInfoVar,
                                                          pfit64,
                                                          hashRoutineDecl,
                                                          useHashing);
            allChords = combineChords(allChords, additionalChords);
          }

          assignChords(riMap, edge, allChords);
        }
      }
    }

    // If BEGIN = END, assign instrumentation for the fake edge
    //    BEGIN -> BEGIN.

    if (beginBlock.equals(endBlock)) {
      PPEdge  fakeEdge  = getEdge(beginBlock, beginBlock, PPEdge.NORMAL);
      Instr   absInstr  = new Instr(Instr.RECORD_CONST, 0);
      Chord[] realInstr = absInstr.genRealInstr(pathRegister,
                                                counterArray,
                                                tempVar,
                                                profileInfoVar,
                                                pfit64,
                                                hashRoutineDecl,
                                                useHashing());
      assignChords(riMap, fakeEdge, realInstr);
    }

    return riMap;
  }

  private Chord[] combineChords(Chord[] chords1, Chord[] chords2)
  {
    if (chords1 == null)
      return chords2;

    Chord[] newChords = new Chord[chords1.length + chords2.length];
    System.arraycopy(chords1, 0, newChords, 0, chords1.length);
    System.arraycopy(chords2, 0, newChords, chords1.length, chords2.length);

    return newChords;
  }

  /**
   * Add instrumentation to the actual Scribble graph.
   * @param instrumentationMap A map from PPEdge to Chord[].
   */
  private void applyInstrumentation(HashMap<PPEdge, Chord[]> instrumentationMap)
  {
    // Iterate over all *editable* edges that need instrumentation.

    Iterator<PPEdge> iter = instrumentationMap.keySet().iterator();
    while (iter.hasNext()) {
      PPEdge e = iter.next();

      // Only deal with editable edges that need instrumentation.

      if (e.isEditable() && instrumentationMap.containsKey(e)) {
        // Get the chords that we want to assign to the edge.

        Chord[] instrumentationChords = instrumentationMap.get(e);

        // If the edge is really the fake edge BEGIN -> BEGIN, then do
        // special instrumentation because our CFG is just a single
        // basic block.

        if (e.source().equals(beginBlock) &&
            e.target().equals(beginBlock)) {
          // Add the instrumentation between the first and second
          // chords of BEGIN.
          Chord first = beginBlock.firstChord();
          instrumentEdge(first, first.getNextChord(), instrumentationChords, 0);
        } else {
          // The instrumentation should be added between the last
          // chord of the source block and the first chord of the
          // target block.
          Chord last  = e.source().lastChord();
          Chord first = e.target().firstChord();
          instrumentEdge(last, first, instrumentationChords, 0);
        }
      }
    }

    // Now deal with the *uneditable* edges that need instrumentation.

    Enumeration<PPBlock> eb = blocks.elements();
    while (eb.hasMoreElements()) {
      PPBlock  block    = eb.nextElement();
      int      numEdges = block.numOutEdges();
      for (int i =  0; i < numEdges; i++) {
        PPEdge e = block.getOutEdge(i);

        // Only deal with edges that are *not editable* and that need
        // instrumentation.

        if (!e.isEditable() && instrumentationMap.containsKey(e)) {
          // Get the instrumentation chords that we want to assign to the edge.

          Chord[] instrumentationChords = instrumentationMap.get(e);

          // Check that the edge's source block has no other outgoing
          // edges (which should be true for the two types of uneditable
          // edges -- see PPEdge.isEditable()).

          assert (e.source().numOutEdges() == 1) :
            "Expected uneditable edge's source to have only one outgoing edge";

          // If there are two or more chords in the basic block, put the
          // instrumentation between the next-to-last and last chords.

          if (!e.source().firstChord().equals(e.source().lastChord())) {
            Chord last = e.source().lastChord();
            instrumentEdge(last.getInCfgEdge(), last, instrumentationChords, 0);
            continue;
          }

          // If all incoming edges of the edge's source are editable,
          // put the instrumentation immediately before the edge's
          // source.

          if (PPEdge.isEditable(null, e.source().firstChord())) {
            instrumentEdge(null,  e.source().firstChord(), instrumentationChords, 0);
            continue;
          }

          // We don't know what to do

          throw new scale.common.InternalError("Unable to find suitable place for instrumentation");
        }
      }
    }
  }

  /**
   * Instrument an edge in a Scribble graph.
   * @param sourceChord is the source chord of the edge or null.
   * @param targetChord is the target chord of the edge.
   * @param instrumentationChords is the instrumentation chords to be added.
   * @param index The starting index to use in instrumentationChords.
   */
  private void instrumentEdge(Chord   sourceChord,
                              Chord   targetChord,
                              Chord[] instrumentationChords,
                              int     index)
  {
    // Check that the edges are editable.

    assert PPEdge.isEditable(sourceChord, targetChord) :
      "Attempting to edit uneditable edge: " + sourceChord + " -> " + targetChord;

    // Check if this edge represents multiple identical edges in the
    // Scribble CFG.

    if (targetChord.numOfInCfgEdge(sourceChord) > 1) {
      // This should only occur when sourceChord is a SwitchChord or
      // an IfThenElseChord.

      assert ((sourceChord instanceof SwitchChord) ||
              (sourceChord instanceof IfThenElseChord)) :
          "Expected multiple identical edges to have SwitchChord or IfThenElseChord source";

      // Add the first instrumentation chord along the multi-edge.

      Chord x                 = instrumentationChords[index];
      int   numIdenticalEdges = targetChord.numOfInCfgEdge(sourceChord);
      for (; numIdenticalEdges > 0; numIdenticalEdges--) {
        sourceChord.replaceOutCfgEdge(targetChord, x);
        targetChord.deleteInCfgEdge(sourceChord);
        x.addInCfgEdge(sourceChord);
      }

      x.replaceOutCfgEdge(null, targetChord);
      targetChord.addInCfgEdge(x);

      // Add the rest of the instrumentation chords between the
      // first instrumentation chord and the target chord.  Use a
      // recursive call.

      instrumentEdge(x,
                     targetChord,
                     instrumentationChords,
                     index + 1);
      return;
    }

    // Otherwise, this edge represents only one edge in the Scribble
    // CFG.  Traverse the list of instrumentation chords, and add
    // each one.

    for (; index < instrumentationChords.length; index++) {
      Chord x = instrumentationChords[index];
      if (sourceChord == null) {
        // Insert instrumentation immediately before the target block.
        targetChord.insertBeforeInCfg((SequentialChord) x);
        continue;
      }

      // Insert instrumentation between the source chord and the
      // target chord.

      sourceChord.insertAfterOutCfg(x, targetChord);

      // Update our new position: the next instrumentation chord
      // will be inserted between the first instrumentation
      // chord and the target.

      sourceChord = x;
    }
  }

  /**
   * Assign an array of chords to an edge.
   * @param instrumentationMap is a map from PPEdge to List of Chord.
   * @param edge is the edge that the chord is assigned to.
   * @param chords is an array of chords to be added to the map for
   * the key "edge."
   */
  private void assignChords(HashMap<PPEdge, Chord[]> instrumentationMap, PPEdge edge, Chord[] chords)
  {
    assert !instrumentationMap.containsKey(edge) :
      "This edge has already been assigned instrumentation.";
    instrumentationMap.put(edge, chords);
  }

  /**
   * Represents abstract instrumentation on an edge.
   */
  private static class Instr
  {
    public static final int INIT         = 1;
    public static final int INCREMENT    = 2;
    public static final int RECORD_CONST = 3;
    public static final int RECORD_VAR   = 4;

    private int  type;
    private long val;

    public Instr(int type, long val)
    {
      this.type = type;
      this.val  = val;
    }

    public int type()
    {
      return type;
    }

    public long val()
    {
      return val;
    }

    public Chord[] genRealInstr(VariableDecl pathRegister,
                                VariableDecl counterArray,
                                VariableDecl tempVar,
                                VariableDecl profileInfoVar,
                                IntegerType  pfit64,
                                RoutineDecl  hashRoutineDecl,
                                boolean      useHashing)
    {
      switch (type) {
      case Instr.INIT:
        return genInitPathRegister(pathRegister,
                                   val,
                                   pfit64);
      case Instr.INCREMENT:
        return genUpdatePathRegister(pathRegister,
                                     val,
                                     pfit64);
      case Instr.RECORD_CONST:
        return genRecordPath(null,
                             val,
                             counterArray,
                             tempVar,
                             profileInfoVar,
                             pfit64,
                             hashRoutineDecl,
                             useHashing);
      case Instr.RECORD_VAR:
        return genRecordPath(pathRegister,
                             val,
                             counterArray,
                             tempVar,
                             profileInfoVar,
                             pfit64,
                             hashRoutineDecl,
                             useHashing);
      }
      return null;
    }

    /**
     * Return Scribble code that initializes the path register to a constant value
     * (r = value).
     * @param pathRegister The variable declaration for the path register.
     * @param value The value to initialize the path register to.
     * @param pfit64 The 64-bit integer type in Scribble.
     * @return A chord that initializes the path register.
     */
    private static Chord[] genInitPathRegister(VariableDecl pathRegister,
                                               long         value,
                                               IntegerType  pfit64)
    {
      LoadDeclAddressExpr lhs   = new LoadDeclAddressExpr(pathRegister);
      LiteralExpr         rhs   = new LiteralExpr(LiteralMap.put(value, pfit64));
      final String        dsply = "r = " + commas(value);
      Chord               x     = new ExprChord(lhs, rhs) {
          public String getDisplayLabel()
          {
            return dsply;
          }
        };
      return new Chord[] {x};
    }

    /**
     * Return Scribble code that adds an increment to the path register
     * (r += increment).
     * @param pathRegister The variable declaration for the path register.
     * @param increment The amount to add to the path register.
     * @param pfit64 The type "integer" in Scribble.
     * @return A chord that increments the path register.
     */
    private static Chord[] genUpdatePathRegister(VariableDecl pathRegister,
                                                 long         increment,
                                                 IntegerType  pfit64)
    {
      LoadDeclAddressExpr lhs   = new LoadDeclAddressExpr(pathRegister);
      LoadDeclValueExpr   la    = new LoadDeclValueExpr(pathRegister);
      LiteralExpr         ra    = new LiteralExpr(LiteralMap.put(increment, pfit64));
      AdditionExpr        rhs   = new AdditionExpr(pfit64, la, ra);
      final String        dsply = "r = r + " + commas(increment);
      Chord               x     =  new ExprChord(lhs, rhs) {
          public String getDisplayLabel()
          {
            return dsply;
          }
        };
      return new Chord[] { x };
    }

    /**
     * Return Scribble code that records the path referenced by the path register.
     * (count[r]++).
     * @param pathRegister is the variable declaration for the path register.
     * @param offset is the offset within the array (i.e., a constant that
     * is added to the value of the path register at runtime).
     * @param counterArray is the variable declaration for the address of
     * the array of counters.
     * @param tempVar is the variable declaration for the temporary
     * variable we used for path profiling.
     * @param profileInfoVar is the variable declaration for the profile
     * information.
     * @param pfit64 is the 64-bit integer type in Scribble.
     * @param hashRoutineDecl The declaration for __pf_hash_path().
     * @return is an array of one or two chords; they record the path
     * referenced by the path register.
     */
    private Chord[] genRecordPath(VariableDecl pathRegister,
                                  long         offset,
                                  VariableDecl counterArray,
                                  VariableDecl tempVar,
                                  VariableDecl profileInfoVar,
                                  IntegerType  pfit64,
                                  RoutineDecl  hashRoutineDecl,
                                  boolean      useHashing)
    {
      // Check whether hashing will be used to count paths.

      if (useHashing) {

        // hashing

        Chord[]       chords = new Chord[1];
        Vector<Expr>  args   = new Vector<Expr>(2);

        args.add(new LoadDeclAddressExpr(profileInfoVar)); // pointer to profiling information

        // There are three possibilities for the path number (i.e., the index).

        Expr pathNumExpr = null;
        if (pathRegister == null) {
          pathNumExpr = new LiteralExpr(LiteralMap.put(offset, pfit64));
        } else if (offset == 0) {
          pathNumExpr = new LoadDeclValueExpr(pathRegister);
        } else {
          Expr la = new LoadDeclValueExpr(pathRegister);
          Expr ra = new LiteralExpr(LiteralMap.put(offset, pfit64));
          pathNumExpr = new AdditionExpr(pfit64, la, ra);
        }

        args.add(pathNumExpr); // path number (from path register)

        LoadDeclAddressExpr ldae         = new LoadDeclAddressExpr(hashRoutineDecl);
        CallFunctionExpr    callFuncExpr = new CallFunctionExpr(VoidType.type, ldae, args);

        chords[0] = newIncChord(null, callFuncExpr, pathRegister, offset);

        return chords;
      }


      // no hashing

      Chord[] chords = new Chord[2];

      // Generate the first chord, which gets the address of
      //   counterArray[pathRegister + offset].tempVar = &counterArray[pathRegister]

      {
        // If the path register is not used in this expression, use 0 in
        // its place.

        Expr indexExpr = null;
        if (pathRegister == null)
          indexExpr = new LiteralExpr(LiteralMap.put(0, pfit64));
        else
          indexExpr = new LoadDeclValueExpr(pathRegister);

        Expr lhs = new LoadDeclAddressExpr(tempVar);
        Expr ar  = new LoadDeclAddressExpr(counterArray);
        Expr off = new LiteralExpr(LiteralMap.put(offset, pfit64));
        Type pet = PointerType.create(pfit64);
        Expr rhs = new ArrayIndexExpr(pet, ar, indexExpr, off);
        Chord x  = new ExprChord(lhs, rhs);
        chords[0] = x;
      }

      // Generate the second chord, which increments
      // counterArray[pathRegister + offset] by 1.
      //  *tempVar = *tempVar + 1

      {
        Expr lhs = new LoadDeclValueExpr(tempVar);
        Expr add = new LoadDeclValueExpr(tempVar);
        Expr la  = new LoadValueIndirectExpr(add);
        Expr ra  = new LiteralExpr(LiteralMap.put(1, pfit64));
        Expr rhs = new AdditionExpr(pfit64, la, ra);
        chords[1] = newIncChord(lhs, rhs, pathRegister, offset);
      }

      return chords;
    }

    private Chord newIncChord(Expr         lhs,
                              Expr         rhs,
                              VariableDecl pathRegister,
                              long         offset)
    {
      StringBuffer buf = new StringBuffer("count[");
      if (pathRegister == null) {
        buf.append(commas(offset));
      } else if (offset == 0) {
        buf.append("r");
      } else {
        buf.append("r+");
        buf.append(commas(offset));
      }
      buf.append("]++");
      final String dsply = buf.toString();

      Chord x = new ExprChord(lhs, rhs) {
          public String getDisplayLabel()
          {
            return dsply;
          }
        };
      return x;
    }

    public String getText()
    {
      switch (type) {
      case INIT:         return "r = " + val;
      case INCREMENT:    return "r += " + val;
      case RECORD_CONST: return "count[" + val + "]++";
      case RECORD_VAR:   return "count[r+" + val + "]++";
      }
      return null;
    }
  }
  
  /**
   * The exception that countPaths() throws when the number of paths
   * exceeds a threshold.
   */
  private static class TooManyPathsException extends java.lang.Exception
  {
    private static final long serialVersionUID = 42L;

    /**
     * The block where the number of paths exceeds the threshold.
     */
    private PPBlock block;

    /**
     * Construct this exception.
     */
    private TooManyPathsException(PPBlock block)
    {
      this.block = block;
    }

    /**
     * Return the block at which the number of paths exceeds the threshold.
     */
    public PPBlock getBlock()
    {
      return block;
    }
  }

  /**
   * Get whether or not to output debugging text to the console.
   */
  public static boolean getDebuggingOutput()
  {
    return debuggingOutput;
  }

  /**
   * Get whether or not to fail if the profile information file not
   * found.
   */
  public static boolean getFailWithoutProfile()
  {
    return failWithoutProfile;
  }

  /**
   * Get the index of the profile on which path analysis should be
   * performed (or -1 if none).
   */
  public static int getPathAnalysisIndex()
  {
    return pathAnalysisIndex;
  }

  /**
   * Get whether we should use the simple or complex unrolling
   * heuristic for profile-guided unrolling.
   */
  public static boolean getSimplerUnrollingHeuristic()
  {
    return simplerUnrollingHeuristic;
  }

  /**
   * Get whether or not to perform profile-guided profiling instead of
   * regular path profiling.
   */
  public static boolean getPgp()
  {
    return pgp;
  }

  /**
   * Set the global cold edge threshold to use for profile-guided
   * profiling.
   */
  private static void setPgpGlobalColdEdgeThreshold(double newThreshold)
  {
    pgpGlobalColdEdgeThreshold = newThreshold;
  }

  public static PPCfg getPgpCfg(PPCfg cfg)
  {
    if (pgpCfgMap == null)
      return null;
    return pgpCfgMap.get(cfg);
  }

  public static HashMap<PPEdge, Instr> getPgpAbstractInstrMap(PPCfg cfg)
  {
    if (pgpAbstractInstrMap == null)
      return null;
    return pgpAbstractInstrMap.get(cfg);
  }

  /**
   * Return true if the specified CFG is a member of an unmodifiable
   * set of Scribble CFGs that should not be instrumented.
   */
  public static boolean doNotInstrument(Scribble cfg)
  {
    if (routinesToNotInstrument == null)
      return false;
    return routinesToNotInstrument.contains(cfg);
  }

  /**
   * Add definite or potential flow to the total flow for the program.
   */
  private static void addCfg(PPCfg cfg)
  {
    if (cfgs == null)
      cfgs = new LinkedList<PPCfg>();
    cfgs.add(cfg);
  }

  /**
   * Perform analysis on edge and path profiles through all CFGs.
   */
  public static void doAnalysis()
  {
    if (cfgs == null)
      return;

    Collections.<PPCfg>sort(cfgs);

    // print statistics for each CFG and for the whole program
    Iterator<PPCfg> cfgIter = cfgs.iterator();
    while (cfgIter.hasNext()) {
      PPCfg cfg = cfgIter.next();
      printStats(cfg, System.out);
    }
    printStats(cfgs, System.out);

    // connect all the CFGs in a supergraph
    superBegin = new PPSupergraphBlock(PPSupergraphBlock.SUPERBEGIN, cfgs);
    superEnd = new PPSupergraphBlock(PPSupergraphBlock.SUPEREND, cfgs);
    createSupergraph(superBegin, superEnd);

    // get a list of all blocks in the supergraph in reverse
    // topological order
    LinkedList<PPBlock> blockList = getBlockList(superBegin, superEnd);

    // compute the definite flow
    definiteFlow = computeFlow(blockList, DEFINITE_FLOW, false);

    // compute the potential flow
    potentialFlow = computeFlow(blockList, POTENTIAL_FLOW, false);

    // TODO: fix
    boolean useTwoEdgeProfile = false; // TODO: make this a parameter
    // compute definite and flow information for two-edge profiling
    if (useTwoEdgeProfile) {
      tepDefiniteFlow = computeFlow(blockList, DEFINITE_FLOW, true);
      tepPotentialFlow = computeFlow(blockList, POTENTIAL_FLOW, true);
    }

    // compute and print flow information for each routine
    cfgIter = cfgs.iterator();
    while (cfgIter.hasNext()) {
      PPCfg cfg = cfgIter.next();

      System.out.println("Flow information for " + cfg.getRoutineName() + ":");

      HashSet<Path> cfgActualPaths      = getActualPaths(superBegin, superEnd, cfg);
      PPBlock begin                     = cfg.beginBlock();
      long    cfgActualRawFlow          = getTotalFlow(cfgActualPaths, RAW_METRIC);
      HashMap<FBPair, Long> defhm       = definiteFlow.get(begin);
      HashMap<FBPair, Long> pothm       = potentialFlow.get(begin);
      long    cfgDefiniteRawFlow        = getTotalFlowInFlowMap(defhm, RAW_METRIC);
      long    cfgPotentialRawFlow       = getTotalFlowInFlowMap(pothm, RAW_METRIC);
      long    cfgTepDefiniteRawFlow     = 0;
      long    cfgTepPotentialRawFlow    = 0;
      long    cfgActualBranchFlow       = getTotalFlow(cfgActualPaths, BRANCH_METRIC);
      long    cfgDefiniteBranchFlow     = getTotalFlowInFlowMap(defhm, BRANCH_METRIC);
      long    cfgPotentialBranchFlow    = getTotalFlowInFlowMap(pothm, BRANCH_METRIC);
      long    cfgTepDefiniteBranchFlow  = 0;
      long    cfgTepPotentialBranchFlow = 0;


      if (useTwoEdgeProfile) {
        HashMap<FBPair, Long> tdefhm = tepDefiniteFlow.get(begin);
        HashMap<FBPair, Long> tpothm = tepPotentialFlow.get(begin);
        cfgTepDefiniteRawFlow     = getTotalFlowInFlowMap(tdefhm, RAW_METRIC);
        cfgTepPotentialRawFlow    = getTotalFlowInFlowMap(tpothm, RAW_METRIC);
        cfgTepDefiniteBranchFlow  = getTotalFlowInFlowMap(tdefhm, BRANCH_METRIC);
        cfgTepPotentialBranchFlow = getTotalFlowInFlowMap(tpothm, BRANCH_METRIC);
      }

      printFlowInfo("  raw ",
                    System.out,
                    cfgActualRawFlow,
                    cfgDefiniteRawFlow,
                    cfgPotentialRawFlow,
                    cfgTepDefiniteRawFlow,
                    cfgTepPotentialRawFlow,
                    useTwoEdgeProfile);
      System.out.println();

      printFlowInfo("branch",
                    System.out,
                    cfgActualBranchFlow,
                    cfgDefiniteBranchFlow,
                    cfgPotentialBranchFlow,
                    cfgTepDefiniteBranchFlow,
                    cfgTepPotentialBranchFlow,
                    useTwoEdgeProfile);
      System.out.println();
    }

    // compute and print flow information for the entire program
    System.out.println("WHOLE PROGRAM FLOW INFORMATION:");
    System.out.println();

    // get all paths taken according to path profile
    HashSet<Path> actualPaths = getActualPaths(superBegin, superEnd, null);

    printPathLengthHistogram(actualPaths, false, false);

    HashMap<FBPair, Long> sdefhm        = definiteFlow.get(superBegin);
    HashMap<FBPair, Long> spothm        = potentialFlow.get(superBegin);
    long    totalActualRawFlow          = getTotalFlow(actualPaths, RAW_METRIC);
    long    totalDefiniteRawFlow        = getTotalFlowInFlowMap(sdefhm,  RAW_METRIC);
    long    totalPotentialRawFlow       = getTotalFlowInFlowMap(spothm, RAW_METRIC);
    long    totalTepDefiniteRawFlow     = 0;
    long    totalTepPotentialRawFlow    = 0;
    long    totalActualBranchFlow       = getTotalFlow(actualPaths, BRANCH_METRIC);
    long    totalDefiniteBranchFlow     = getTotalFlowInFlowMap(sdefhm,  BRANCH_METRIC);
    long    totalPotentialBranchFlow    = getTotalFlowInFlowMap(spothm, BRANCH_METRIC);
    long    totalTepDefiniteBranchFlow  = 0;
    long    totalTepPotentialBranchFlow = 0;

    if (useTwoEdgeProfile) {
      HashMap<FBPair, Long> stdefhm = tepDefiniteFlow.get(superBegin);
      HashMap<FBPair, Long> stpothm = tepPotentialFlow.get(superBegin);
      totalTepDefiniteRawFlow     = getTotalFlowInFlowMap(stdefhm, RAW_METRIC);
      totalTepPotentialRawFlow    = getTotalFlowInFlowMap(stpothm, RAW_METRIC);
      totalTepDefiniteBranchFlow  = getTotalFlowInFlowMap(stdefhm, BRANCH_METRIC);
      totalTepPotentialBranchFlow = getTotalFlowInFlowMap(stpothm, BRANCH_METRIC);
    }

    printFlowInfo("  raw ",
                  System.out,
                  totalActualRawFlow,
                  totalDefiniteRawFlow,
                  totalPotentialRawFlow,
                  totalTepDefiniteRawFlow,
                  totalTepPotentialRawFlow,
                  useTwoEdgeProfile);
    System.out.println();

    printFlowInfo("branch",
                  System.out,
                  totalActualBranchFlow,
                  totalDefiniteBranchFlow,
                  totalPotentialBranchFlow,
                  totalTepDefiniteBranchFlow,
                  totalTepPotentialBranchFlow,
                  useTwoEdgeProfile);
    System.out.println();

    System.out.println("END OF WHOLE PROGRAM FLOW INFORMATION");
    System.out.println();

    // Generate a graph with flow information (using flow metric) for each CFG.

    cfgIter = cfgs.iterator();
    while (cfgIter.hasNext()) {
      PPCfg cfg = cfgIter.next();
      if (generateGraphs) {

        // TODO: remove
        cfg.generateGraph("definite_flow_pair",
                          PPCfg.GRAPH_MODE_DEFINITE_FLOW_PAIR,
                          null,
                          0,
                          definiteFlow,
                          null,
                          0);
        cfg.generateGraph("potential_flow_pair",
                          PPCfg.GRAPH_MODE_DEFINITE_FLOW_PAIR,
                          null,
                          0,
                          potentialFlow,
                          null,
                          0);
        if (useTwoEdgeProfile) {
          cfg.generateGraph("tep_definite_flow_pair",
                            PPCfg.GRAPH_MODE_DEFINITE_FLOW_PAIR,
                            null,
                            0,
                            tepDefiniteFlow,
                            null,
                            0);
          cfg.generateGraph("tep_potential_flow_pair",
                            PPCfg.GRAPH_MODE_DEFINITE_FLOW_PAIR,
                            null,
                            0,
                            tepPotentialFlow,
                            null,
                            0);
        }
      }
    }

    // do a lot of hot path analysis
    thoroughHotPathAnalysis(superBegin,
                            superEnd,
                            definiteFlow,
                            DEFINITE_FLOW,
                            "definite",
                            null,
                            actualPaths,
                            false);
    thoroughHotPathAnalysis(superBegin,
                            superEnd,
                            potentialFlow,
                            POTENTIAL_FLOW,
                            "potential",
                            null,
                            actualPaths,
                            false);
    // do hot path analysis using a two-edge profile
    if (useTwoEdgeProfile) {
      thoroughHotPathAnalysis(superBegin,
                              superEnd,
                              tepDefiniteFlow,
                              DEFINITE_FLOW,
                              "TEP definite",
                              null,
                              actualPaths,
                              true);
      thoroughHotPathAnalysis(superBegin,
                              superEnd,
                              tepPotentialFlow,
                              POTENTIAL_FLOW,
                              "TEP potential",
                              null,
                              actualPaths,
                              true);
    }

    defFlowPaths = computeHotPaths(definiteFlow,
                                        superBegin,
                                        superEnd,
                                        Long.MAX_VALUE,
                                        Long.MAX_VALUE,
                                        RAW_METRIC,
                                        DEFINITE_FLOW,
                                        null,
                                        false,
                                        false);

    // Compute the amount of definite, potential, and actual flow on
    // each edge.

    definiteRawFlowForEdges    = computeFlowForEdges(defFlowPaths,
                                                     superBegin,
                                                     superEnd,
                                                     RAW_METRIC);
    definiteBranchFlowForEdges = computeFlowForEdges(defFlowPaths,
                                                     superBegin,
                                                     superEnd,
                                                     BRANCH_METRIC);
    actualBranchFlowForEdges   = computeFlowForEdges(actualPaths,
                                                     superBegin,
                                                     superEnd,
                                                     BRANCH_METRIC);

    // TODO: remove
    /*
    Iterator<Path> pathIter = defFlowPaths.iterator();
    while (pathIter.hasNext()) {
      Path path = pathIter.next();
      path.cfg().generateGraph("definite_flow_raw_path_" + path.pathNum() + "_" + path.pair().flow(),
                               PPCfg.GRAPH_MODE_SHOW_PATH, null, path.pathNum(), null, null, 0);
    }
    */
  }

  /**
   * Do a bunch of different versions of hot path comparisons.
   * @param superBegin
   * @param superEnd
   * @param flows
   * @param flowTypes
   * @param flowNames
   * @param estimatedPaths
   * @param actualPaths
   */
  private static void thoroughHotPathAnalysis(PPSupergraphBlock superBegin,
                                              PPSupergraphBlock superEnd,
                                              HashMap<Object, HashMap<FBPair, Long>> flow,
                                              int               flowType,
                                              String            flowName,
                                              HashSet<Path>     estPath,
                                              HashSet<Path>     actualPaths,
                                              boolean           useTwoEdgeProfile)
  {
    int[]      metrics        = new int[]      { RAW_METRIC, BRANCH_METRIC };
    String[]   metricNames    = new String[]   { "raw",   "branch" };
    double[][] thresholds     = new double[][] { new double[] { 0.00125, 0.00 } };
/*                                               new double[] { 0.01000, 0.00 },
                                                 new double[] { 0.0,     0.50 },
                                                 new double[] { 0.0,     0.90 } };
*/
    boolean[] incorrects      = new boolean[] { false };

    // shoulda used fortran

    for (int j = 0; j < metrics.length; j++) {
      String metricName = metricNames[j];
      int    metric     = metrics[j];
      for (int k = 0; k < thresholds.length; k++) {
        double thresh0 = thresholds[k][0];
        double thresh1 = thresholds[k][1];
        for (int l = 0; l < incorrects.length; l++) {
          hotPathAnalysis(flowName,
                          metricName,
                          flow,
                          estPath,
                          actualPaths,
                          superBegin,
                          superEnd,
                          thresh0,
                          thresh1,
                          metric,
                          flowType,
                          incorrects[l],
                          useTwoEdgeProfile);
        }
      }
    }
  }

  private static HashMap<PPEdge, Long> computeFlowForEdges(HashSet<Path>      paths,
                                             PPSupergraphBlock superBegin,
                                             PPSupergraphBlock superEnd,
                                             int               flowMetric)
  {
    HashMap<PPEdge, Long> flowForEdges = new HashMap<PPEdge, Long>();

    // Give all edges a flow of 0.

    Iterator<PPEdge> edgeIter = getEdgeList(superBegin, superEnd).iterator();
    while (edgeIter.hasNext()) {
      PPEdge edge = edgeIter.next();
      flowForEdges.put(edge, new Long(0));
    }

    // Go through all paths and add their flows to each of their edges.

    Iterator<Path> pathIter = paths.iterator();
    while (pathIter.hasNext()) {
      Path path     = pathIter.next();
      long pathFlow = path.pair().getWeightedFlow(flowMetric);

      // Traverse all paths and add each path's flow to the flow of
      // the edges on the path.

      Iterator<PPEdge> edgeOnPathIter = path.cfg().getEdgesOnPath(path.pathNum()).iterator();
      while (edgeOnPathIter.hasNext()) {
        PPEdge edgeOnPath  = edgeOnPathIter.next();
        long   currentFlow = flowForEdges.get(edgeOnPath).longValue();
        flowForEdges.put(edgeOnPath, new Long(currentFlow + pathFlow));
      }
    }
    
    return flowForEdges;
  }

  /**
   * Construct a supergraph by connecting SUPERBEGIN to all CFGs'
   * BEGIN blocks and SUPEREND to all CFGs' END blocks.
   * 
   */
  private static void createSupergraph(PPSupergraphBlock superBegin,
                                       PPSupergraphBlock superEnd)
  {
    if (cfgs == null)
      return;

    Iterator<PPCfg> iter = cfgs.iterator();
    while (iter.hasNext()) {
      PPCfg   cfg   = iter.next();
      PPBlock bblk  = cfg.beginBlock();
      PPBlock eblk  = cfg.endBlock();
      PPEdge  begin = cfg.getEdge(superBegin, bblk, PPEdge.DUMMY_FOR_BACK_EDGE);
      PPEdge  end   = cfg.getEdge(eblk, superEnd, PPEdge.DUMMY_FOR_BACK_EDGE);
    }
  }

  /**
   * Deconstruct the supergraph by disconnecting SUPERBEGIN and
   * SUPEREND from all CFGs.
   */
  private static void disconnectSupergraph(PPSupergraphBlock superBegin,
                                           PPSupergraphBlock superEnd)
  {
    if (cfgs == null)
      return;

    Iterator<PPCfg> iter = cfgs.iterator();
    while (iter.hasNext()) {
      PPCfg   cfg   = iter.next();
      PPBlock bblk  = cfg.beginBlock();
      PPBlock eblk  = cfg.endBlock();
      PPEdge  begin = cfg.getEdge(superBegin, bblk, PPEdge.DUMMY_FOR_BACK_EDGE);
      PPEdge  end   = cfg.getEdge(eblk, superEnd, PPEdge.DUMMY_FOR_BACK_EDGE);
      cfg.removeEdge(begin, true);
      cfg.removeEdge(end, true);
    }
  }

  /**
   * Get all blocks in the program in reverse topological order.
   */
  private static LinkedList<PPBlock> getBlockList(PPBlock superBegin, PPBlock superEnd)
  {
    LinkedList<PPBlock> blockList = new LinkedList<PPBlock>();
    if (cfgs == null)
      return blockList;

    blockList.add(superEnd);
    Iterator<PPCfg> iter = cfgs.iterator();
    while (iter.hasNext()) {
      PPCfg cfg = iter.next();
      blockList.addAll(cfg.getBlocksInReverseTopologicalOrder());
    }
    blockList.add(superBegin);

    return blockList;
  }

  /**
   * Get all edges in the program in reverse topological order.
   */
  private static LinkedList<PPEdge> getEdgeList(PPBlock superBegin, PPBlock superEnd)
  {
    LinkedList<PPEdge> edgeList = new LinkedList<PPEdge>();

    PPEdge[] sli = superEnd.incoming();
    for (int i = 0; i < sli.length; i++)
      edgeList.add(sli[i]);

    if (cfgs != null) {
      Iterator<PPCfg> iter = cfgs.iterator();
      while (iter.hasNext()) {
        PPCfg cfg = iter.next();
        edgeList.addAll(cfg.getEdgesInReverseTopologicalOrder());
      }
    }

    PPEdge[] slo = superBegin.outgoing();
    for (int i = 0; i < slo.length; i++)
      edgeList.add(slo[i]);

    return edgeList;
  }
  
  /**
   * Get the frequency of a supergraph edge or a normal edge.
   */
  private static long getEdgeFreq(PPEdge edge)
  {
    if (edge.source() instanceof PPSupergraphBlock)
      return getBlockFreq2(edge.target());

    if (edge.target() instanceof PPSupergraphBlock)
      return getBlockFreq2(edge.source());

    return edge.getFrequency();
  }

  /**
   * Get the frequency of a two-edge pair.
   */
  private static long getTwoEdgeFreq(PPEdge incomingEdge, PPEdge outgoingEdge)
  {
    assert incomingEdge.target().equals(outgoingEdge.source()) :
      "Not a valid two-edge pair.";

    if (incomingEdge.source() instanceof PPSupergraphBlock)
      return getEdgeFreq(outgoingEdge);

    if (outgoingEdge.target() instanceof PPSupergraphBlock)
      return getEdgeFreq(incomingEdge);

    PPCfg          cfg = incomingEdge.getCfg();
    Iterator<Long> pathNumIter = cfg.getTakenPathNumbers();
    long           totalFreq = 0;
    while (pathNumIter.hasNext()) {
      long pathNum = pathNumIter.next().longValue();
      if (pathNum != -1) {
        Vector<PPEdge> edgesOnPath = cfg.getEdgesOnPath(pathNum);
        if (edgesOnPath.contains(incomingEdge) &&
            edgesOnPath.contains(outgoingEdge)) {
          totalFreq += cfg.getPathFreq(pathNum);
        }
      }
    }

    return totalFreq;
  }

  /**
   * Compute the definite or potential flow of an edge profile.
   * @param blockList A list of basic blocks in reverse topological
   * order.
   * @param flowType is the type of flow (currently either DEFINITE_FLOW
   * or POTENTIAL_FLOW).
   * @param useTep if true, use a two-edge profile rather than a
   * regular edge profile.
   * @return is a map from each basic block and edge to its definite or
   * potential flow map.
   */
  private static HashMap<Object, HashMap<FBPair, Long>> computeFlow(LinkedList<PPBlock> blockList,
                                     int        flowType,
                                     boolean    useTep)
  {
    HashMap<Object, HashMap<FBPair, Long>> masterMap = new HashMap<Object, HashMap<FBPair, Long>>();
    PPBlock begin     = blockList.getLast();
    PPBlock end       = blockList.getFirst();

    // Set the flow value of END to the flow map [F -> 1].

    HashMap<FBPair, Long> flowMap = new HashMap<FBPair, Long>();
    if (getBlockFreq2(begin) > 0)
      flowMap.put(new FBPair(getBlockFreq2(begin), 0), new Long(1));

    masterMap.put(end, flowMap);

    // Go through the list in reverse topological order.

    Iterator<PPBlock> blockIter = blockList.iterator();
    while (blockIter.hasNext()) {
      PPBlock block = blockIter.next();
     
      if (block.equals(end)) // skip END
        continue;

      PPEdge[] lo = block.outgoing();
      if (lo != null) {
        for (int i = 0; i < lo.length; i++) {
          PPEdge  edge        = lo[i];
          HashMap<FBPair, Long> edgeFlowMap = new HashMap<FBPair, Long>(); // Used to compute the flow map for the edge.

          // two-edge profile or regular edge profile?

          PPBlock target = edge.target();
          if (useTep && !(target instanceof PPSupergraphBlock)) {
            PPEdge[] tlo = target.outgoing();
            if (tlo != null) {
              for (int j = 0; j < tlo.length; j++) { // For each outgoing edge of the target.
                PPEdge outgoingEdge = tlo[j];

                // Compute the (unit) flow "stolen" by all two-edge pairs <*, outgoingEdge>
                // (except <edge, outgoingEdge>).

                long twoEdgeFreq = getTwoEdgeFreq(edge, outgoingEdge);
                long flowStolen  = getEdgeFreq(outgoingEdge) - twoEdgeFreq;

                // Consider each flow value for the outgoing edge.

                HashMap<FBPair, Long>  outgoingEdgeFlowMap = masterMap.get(outgoingEdge);
                Iterator<FBPair>       pairIter            = outgoingEdgeFlowMap.keySet().iterator();
                while (pairIter.hasNext()) {
                  FBPair pair        = pairIter.next();
                  long   freq        = pair.flow();
                  int    numBranches = pair.numBranches() + (outgoingEdge.isBranchEdge() ? 1 : 0);
                  long   pathCount   = outgoingEdgeFlowMap.get(pair).longValue();

                  switch (flowType) {
                  case DEFINITE_FLOW:
                    if (freq > flowStolen) {
                      FBPair paird = new FBPair(freq - flowStolen, numBranches);
                      addFlowMapEntry(edgeFlowMap, paird, pathCount);
                    }
                    break;
                  case POTENTIAL_FLOW:
                    long newFreq = Math.min(freq, twoEdgeFreq);
                    if (newFreq > 0) {
                      FBPair pairp = new FBPair(newFreq, numBranches);
                      addFlowMapEntry(edgeFlowMap, pairp, pathCount);
                    }
                    break;
                  }
                }
              }
            }
          } else {
            long                   freqDiff      = getBlockFreq2(target) - getEdgeFreq(edge); // the amount of (unit) flow "stolen".
            HashMap<FBPair, Long>  targetFlowMap = masterMap.get(target);
            Iterator<FBPair>       pairIter      = targetFlowMap.keySet().iterator();
            while (pairIter.hasNext()) { // For all entries in the flow map of the edge's target.
              FBPair pair        = pairIter.next();
              long   freq        = pair.flow();
              int    numBranches = pair.numBranches();
              long   pathCount   = targetFlowMap.get(pair).longValue();
              switch (flowType) {
              case DEFINITE_FLOW:
                if (freq > freqDiff) {
                  FBPair paird = new FBPair(freq - freqDiff, numBranches);
                  addFlowMapEntry(edgeFlowMap, paird, pathCount);
                }
                break;
              case POTENTIAL_FLOW:
                long newFreq = Math.min(freq, getEdgeFreq(edge));
                if (newFreq > 0) {
                  FBPair pairp = new FBPair(newFreq, numBranches);
                  addFlowMapEntry(edgeFlowMap, pairp, pathCount);
                }
                break;
              }
            }
          }

          masterMap.put(edge, edgeFlowMap); // Assign the flow map to the edge.
        }
      }

      // Compute the flow map for the block.

      HashMap<FBPair, Long> blockFlowMap = new HashMap<FBPair, Long>();
      if (lo != null) {
        for (int i = 0; i < lo.length; i++) { // For all outgoing edges of the block.
          PPEdge                edge = lo[i];
          HashMap<FBPair, Long> me   = masterMap.get(edge);
          addFlowMapEntries(blockFlowMap, me, edge.isBranchEdge());
        }
      }

      masterMap.put(block, blockFlowMap); // Set the flow map for the block.
    }

    return masterMap;  // Return the map from blocks and edges to flow maps.
  }

  /**
   * Add the flow map entry (freq, pathCount) to the flow map flowMap.
   */
  private static void addFlowMapEntry(HashMap<FBPair, Long> flowMap,
                                      FBPair  pair,
                                      long    pathCount)
  {
    long currentPathCount = 0;
    if (flowMap.containsKey(pair))
      currentPathCount = flowMap.get(pair).longValue();

    flowMap.put(pair, new Long(currentPathCount + pathCount));
  }

  /**
   * Add multiple flow map entries in newEntries to the flow map flowMap.
   */
  private static void addFlowMapEntries(HashMap<FBPair, Long> flowMap,
                                        HashMap<FBPair, Long> newEntries,
                                        boolean isBranchEdge)
  {
    Iterator<FBPair> iter = newEntries.keySet().iterator();
    while (iter.hasNext()) {
      FBPair pair        = iter.next();
      long   flow        = pair.flow();
      int    numBranches = pair.numBranches();
      long   count       = newEntries.get(pair).longValue();

      // If the edge that we're adding flow map entries for is a
      // branch edge, then increment the branch counter.

      if (isBranchEdge)
        pair = new FBPair(flow, numBranches + 1);
      
      addFlowMapEntry(flowMap, pair, count);
    }
  }

  /**
   * Return the total amount of flow represented by a flow map.
   * @param flowMap A map from flow to frequency.
   * @param metric A flow metric constant (see *_METRIC).
   */
  private static long getTotalFlowInFlowMap(HashMap<FBPair, Long> flowMap, int metric)
  {
    long             totalFlow = 0;
    Iterator<FBPair> iter      = flowMap.keySet().iterator();
    while (iter.hasNext()) {
      FBPair pair  = iter.next();
      long   count = flowMap.get(pair).longValue();

      totalFlow += (pair.getWeightedFlow(metric) * count);
    }
    return totalFlow;
  }

  /**
   * Get the total flow in a set of paths using the specified metric.
   */
  private static long getTotalFlow(HashSet<Path> paths, int metric)
  {
    long totalFlow = 0;

    Iterator<Path> pathIter = paths.iterator();
    while (pathIter.hasNext()) {
      Path path = pathIter.next();
      totalFlow += path.pair().getWeightedFlow(metric);
    }

    return totalFlow;
  }

  /**
   * Compare a path profile estimate with the actual profile.
   */
  private static void hotPathAnalysis(String            flowName,
                                      String            metricName,
                                      HashMap<Object, HashMap<FBPair, Long>>masterMap,
                                      HashSet<Path>           estimatedPaths,
                                      HashSet<Path>           actualPaths,
                                      PPSupergraphBlock superBegin,
                                      PPSupergraphBlock superEnd,
                                      double            singlePathThreshold,
                                      double            aggregateThreshold,
                                      int               metric,
                                      int               flowType,
                                      boolean           incorrect,
                                      boolean           useTwoEdgeProfile)
  {

    long totalActualWeightedFlow  = getTotalFlow(actualPaths, metric);

    long totalEstimatedWeightedFlow;
    if (estimatedPaths == null) {
      HashMap<FBPair, Long> me = masterMap.get(superBegin);
      totalEstimatedWeightedFlow = getTotalFlowInFlowMap(me, metric);
    } else
      totalEstimatedWeightedFlow = getTotalFlow(estimatedPaths, metric);

    System.out.print("Using ");
    System.out.print(flowName);
    System.out.print(" flow with the ");
    System.out.print(metricName);
    System.out.print(" metric");
    System.out.print((incorrect ? " with INCORRECT path enumeration algorithm": ""));
    System.out.println(":");

    long absoluteSinglePathThreshold = 0;
    long absoluteAggregateThreshold  = Long.MAX_VALUE;
    if (singlePathThreshold != 0.0) {
      System.out.print("  Single path threshold:    ");
      System.out.println(singlePathThreshold);
      absoluteSinglePathThreshold = (long) (singlePathThreshold * totalActualWeightedFlow);
    }
    if (aggregateThreshold != 0.0) {
      System.out.print("  Aggregate path threshold: ");
      System.out.println(aggregateThreshold);
      absoluteAggregateThreshold = (long) (aggregateThreshold * totalActualWeightedFlow);
    }
    System.out.println();

    // get the actual hot paths that meet the threshold
    HashSet<Path> hotActualPaths = computeHotPaths(actualPaths,
                                             absoluteSinglePathThreshold,
                                             absoluteAggregateThreshold,
                                             Integer.MAX_VALUE,
                                             metric);
    long weightedFlowInHotActualPaths = getTotalFlow(hotActualPaths, metric);

    System.out.print("  Distinct actual paths: ");
    System.out.println(commas(hotActualPaths.size()));
    System.out.print("  Amount of flow (");
    System.out.print(metricName);
    System.out.print(" metric): ");
    System.out.print(commas(weightedFlowInHotActualPaths));
    System.out.print(" (");
    System.out.print(100.0 * weightedFlowInHotActualPaths / totalActualWeightedFlow);
    System.out.println("% of total actual flow)");
    System.out.println();

    // get the same number of paths from definite and potential flow
    HashSet<Path> hotEstimatedPaths;
    if (estimatedPaths == null) {
      hotEstimatedPaths = computeHotPaths(masterMap,
                                          superBegin,
                                          superEnd,
                                          hotActualPaths.size(),
                                          Long.MAX_VALUE,
                                          metric, flowType,
                                          null,
                                          incorrect,
                                          useTwoEdgeProfile);
    } else {
      hotEstimatedPaths = computeHotPaths(estimatedPaths,
                                          0,
                                          Long.MAX_VALUE,
                                          hotActualPaths.size(),
                                          metric);
    }

    long weightedFlowInHotEstimatedPaths = getTotalFlow(hotEstimatedPaths, metric);

    System.out.print("  Distinct estimated paths (tried to get ");
    System.out.print(commas(hotActualPaths.size()));
    System.out.print("): ");
    System.out.println(commas(hotEstimatedPaths.size()));
    System.out.print("  Amount of flow (");
    System.out.print(metricName);
    System.out.print(" metric): ");
    System.out.print(commas(weightedFlowInHotEstimatedPaths));
    System.out.print(" (");
    System.out.print((100.0 * weightedFlowInHotEstimatedPaths) / totalEstimatedWeightedFlow);
    System.out.println("% of total estimated flow)");
    System.out.println();

    // Intersect the two sets; the intersection set retains the flow
    // data of the actual paths, not the estimated paths.
    HashSet<Path> intersectionPaths = new HashSet<Path>(hotActualPaths);
    intersectionPaths.retainAll(hotEstimatedPaths);
    long weightedFlowInIntersectionPaths = getTotalFlow(intersectionPaths, metric);

    System.out.print("  Number distinct paths in both sets: ");
    System.out.println(commas(intersectionPaths.size()));
    System.out.print("  Amount of flow (");
    System.out.println(metricName);
    System.out.print(" metric): ");
    System.out.print(commas(weightedFlowInIntersectionPaths));
    System.out.print(" (");
    System.out.print((100.0 * weightedFlowInIntersectionPaths) / weightedFlowInHotActualPaths);
    System.out.println("% of total actual hot flow)");
    System.out.println();
  }

  /**
   * Find the hot paths in a set of paths using thresholds and a metric.
   */
  private static HashSet<Path> computeHotPaths(HashSet<Path> paths,
                                               long          singlePathThreshold,
                                               long          aggregateThreshold,
                                               int           maxNumPaths,
                                               int           metric)
  {
    // put the paths in a list and sort them.

    FBPair.metric = metric;

    LinkedList<Path> sortedPathList = new LinkedList<Path>(paths);
    Collections.<Path>sort(sortedPathList);

    HashSet<Path>  hotPaths  = new HashSet<Path>();
    long           totalFlow = 0;
    Iterator<Path> pathIter  = sortedPathList.iterator();
    int            numPaths  = 0;
    while (pathIter.hasNext()) {
      Path path         = pathIter.next();
      long weightedFlow = path.pair().getWeightedFlow(metric);

      // Check the thresholds; update the total flow.

      if (weightedFlow < singlePathThreshold ||
          totalFlow >= aggregateThreshold ||
          numPaths >= maxNumPaths) {
        break;
      }

      totalFlow += weightedFlow;

      // Add the path to the set of hot paths.

      hotPaths.add(path);
      numPaths++;
    }

    return hotPaths;
  }

  /**
   * Using a map of flow values, compute the paths that have flow
   * greater than or equal to threshold.  This is Figure 11 from the
   * Showdown paper.
   * @param masterMap is a map from blocks and edges to flow maps.
   * @param begin is the BEGIN node of the graph or supergraph we're looking at.
   * @param end is the END node of the graph or supergraph we're looking at.
   * @param threshold is the minimum flow value for a hot path.
   * @return is a set of Path objects.
   */
  private static HashSet<Path> computeHotPaths(HashMap<Object, HashMap<FBPair, Long>> masterMap,
                                         PPBlock begin,
                                         PPBlock end,
                                         long    numPathsThreshold,
                                         long    aggregateFlowThreshold,
                                         int     metric,
                                         int     flowType,
                                         PPCfg   cfg,
                                         boolean incorrect,
                                         boolean useTwoEdgeProfile)
  {
    HashMap<FBPair, Long> flowMap;
    if (cfg == null)
      flowMap = masterMap.get(begin);
    else
      flowMap = masterMap.get(cfg.beginBlock());

    // Sort the flow pairs from highest to lowest.

    FBPair.metric = metric;

    LinkedList<FBPair> sortedPairList = new LinkedList<FBPair>(flowMap.keySet());
    Collections.<FBPair>sort(sortedPairList);

    // Go through the frequencies from highest to lowest; enumerate
    // the hot paths.

    HashSet<Path>    hotPaths            = new HashSet<Path>();
    Iterator<FBPair> pairIter            = sortedPairList.iterator();
    long             numPaths            = 0;
    long             totalFlowEnumerated = 0;
    while (pairIter.hasNext()) {
      FBPair pair        = pairIter.next();
      long   f           = pair.flow();
      int    numBranches = pair.numBranches();
      long   count       = flowMap.get(pair).longValue();

      assert (f != 0) : "Did not expect unit flow to be zero";

      // We don't want to go over the number-of-paths threshold.

      if (numPaths + count > numPathsThreshold)
        count = numPathsThreshold - numPaths;

      // We don't want to go over the aggregate flow threshold.

      long wflow = pair.getWeightedFlow(metric);
      if (((wflow * count) + totalFlowEnumerated) > aggregateFlowThreshold)
        count = (aggregateFlowThreshold - totalFlowEnumerated) / wflow;

      // Enumerate all the paths for this frequency.

      enumeratePaths(begin,
                     new Vector<PPEdge>(),
                     f,
                     f,
                     numBranches,
                     numBranches,
                     count,
                     hotPaths,
                     masterMap,
                     end,
                     flowType,
                     cfg,
                     incorrect,
                     useTwoEdgeProfile);

      // Update the number of paths and the flow enumerated; check if
      // it reached/exceeded the threshold.

      numPaths += count;
      totalFlowEnumerated += (pair.getWeightedFlow(metric) * count);

      if (totalFlowEnumerated >= aggregateFlowThreshold)
        break;

      if (incorrect) {
        if (hotPaths.size() >= numPathsThreshold)
          break;
      } else {
        if (numPaths >= numPathsThreshold)
          break;
      }
    }
    
    // TODO: what if there are ties, and we get more than the # of
    // paths we want?
    
    return hotPaths;
  }

  /**
   * A helper method of computeHotPaths().  It is enumerate() from
   * Figure 11 of the Showdown paper.
   */
  private static void enumeratePaths(PPBlock v,
                                     Vector<PPEdge> p,
                                     long    currF,
                                     long    origF,
                                     int     currNumBranches,
                                     int     origNumBranches,
                                     long    count,
                                     HashSet<Path> hotPaths,
                                     HashMap<Object, HashMap<FBPair, Long>> masterMap,
                                     PPBlock end,
                                     int     flowType,
                                     PPCfg   cfg,
                                     boolean incorrect,
                                     boolean useTwoEdgeProfile) {
    long             countPrime = count;
    HashSet<Object>  used       = new HashSet<Object>();

    // If we're at the end of the supergraph, designate the current
    // path and its frequency as a hot path.

    if (v.equals(end)) {
      assert (currNumBranches == 0) : "Number of branches mismatch";

      Path newPath = new Path(p, new FBPair(origF, origNumBranches));
      if (hotPaths.contains(newPath))
          // Do nothing, with the incorrect algorithm we tolerate this.
        assert incorrect : "Duplicate hot path unexpected";
      else
        hotPaths.add(newPath);
    } else {
      while (countPrime > 0) {
        PPEdge     prevEdge    = (p.isEmpty() ? null : p.lastElement());
        EFBTriplet bestTriplet = getBestTriplet(v,
                                                currF,
                                                currNumBranches,
                                                masterMap,
                                                used,
                                                flowType,
                                                cfg,
                                                incorrect,
                                                prevEdge, useTwoEdgeProfile);
        PPEdge e = bestTriplet.edge();

        // If the chosen edge is a branch edge, then decrement the
        // number of branches on the path.

        int newNumBranches = currNumBranches;
        if (e.isBranchEdge())
          newNumBranches = currNumBranches - 1;

        long       g           = bestTriplet.flow();
        int        numBranches = bestTriplet.numBranches();
        HashMap<FBPair, Long>    edgeFlowMap = masterMap.get(e);
        FBPair     pair        = new FBPair(g, numBranches);
        long       gcount      = edgeFlowMap.get(pair).longValue();
        long       debit       = Math.min(countPrime, gcount);
        Vector<PPEdge>     newPathList = new Vector<PPEdge>(p);

        newPathList.add(e);

        long newF;
        if (incorrect) { // Don't change f if using incorrect algorithm.
          newF = currF;
        } else {
          if (useTwoEdgeProfile) {
            switch (flowType) {
              case DEFINITE_FLOW:
                // Add flow loss to the amount of flow we expect to find.
                long x = ((prevEdge == null) ? getEdgeFreq(e) : getTwoEdgeFreq(prevEdge, e));
                long stolenFlow = getEdgeFreq(e) - x;
                newF = currF + stolenFlow;
                break;
              case POTENTIAL_FLOW:
                // For the rest of the path, we want to look for edges
                // with potential flow of at least the amount we've
                // already encountered.
                newF = bestTriplet.flow();
                break;
              default:
                throw new scale.common.InternalError("Unrecognized flow type");
            }
          } else {
            switch (flowType) {
              case DEFINITE_FLOW:
                // Add flow loss to the amount of flow we expect to find.
                newF = currF + (getBlockFreq2(e.target()) - getEdgeFreq(e));
                break;
              case POTENTIAL_FLOW:
                // For the rest of the path, we want to look for edges
                // with potential flow of at least the amount we've
                // already encountered.
                newF = bestTriplet.flow();
                break;
              default:
                throw new scale.common.InternalError("Unrecognized flow type");
            }
          }
        }
        enumeratePaths(e.target(),
                       newPathList,
                       newF,
                       origF,
                       newNumBranches,
                       origNumBranches,
                       debit,
                       hotPaths,
                       masterMap,
                       end,
                       flowType,
                       cfg,
                       incorrect,
                       useTwoEdgeProfile);
        used.add(bestTriplet);
        countPrime -= debit;
      }
    }
  }

  /**
   * A helper method of enumerateHotPath().
   */
  private static EFBTriplet getBestTriplet(PPBlock v,
                                           long    f,
                                           int     currNumBranches,
                                           HashMap<Object, HashMap<FBPair, Long>> masterMap,
                                           HashSet<Object> used,
                                           int     flowType,
                                           PPCfg   cfg,
                                           boolean incorrect,
                                           PPEdge  prevEdge,
                                           boolean useTwoEdgeProfile)
  {
    // If this is definite flow:
    //   Find the outgoing edge that (1) has g = freq and (2) (edge,
    //   g) not in used, where (g -> gcount) in the edge's flow map.
    // If this is potential flow:
    //   Find the outgoing edge that (1) has minimal g with g >= freq
    //   and (2) (edge, g) not in used, where (g -> gcount) in the
    //   edge's flow map.
    // Note that we do different stuff if we're using a two-edge profile.

    EFBTriplet bestTriplet = null;
    PPEdge[]   lo          = v.outgoing();
    if (lo != null) {
      for (int i = 0; i < lo.length; i++) {
        PPEdge tempEdge = lo[i];

        // If a CFG was specified, make sure the path is on that CFG.

        if ((cfg != null) && (!tempEdge.getCfg().equals(cfg)))
          continue;

        // If the edge is a branch edge, then decrement the number of
        // branches that we're looking for.

        int numBranchesSought = currNumBranches;
        if (tempEdge.isBranchEdge())
          numBranchesSought = currNumBranches - 1;

        long targetFlow = f;

        // If we're using a two-edge profile, we need to modify the
        // target flow.

        if (useTwoEdgeProfile) {
          if (prevEdge != null) {
            if (flowType == DEFINITE_FLOW && !incorrect) {
              long stolenFlow = getEdgeFreq(tempEdge) - getTwoEdgeFreq(prevEdge, tempEdge);
              targetFlow += stolenFlow;
            } else {
              // targetFlow = getTwoEdgeFreq(prevEdge, tempEdge); //
              // TODO: decide whether we need this (probably not).
            }
          }
        }

        // Search the edge's flow map for the best frequency according
        // to the criteria in the comment above.

        HashMap<FBPair, Long>  edgeFlowMap = masterMap.get(tempEdge);
        Iterator<FBPair> pairIter    = edgeFlowMap.keySet().iterator();
        while (pairIter.hasNext()) {
          FBPair     pair    = pairIter.next();
          EFBTriplet triplet = new EFBTriplet(tempEdge, pair.flow(), pair.numBranches());
          if (flowType == DEFINITE_FLOW && !incorrect) { 
            if ((triplet.flow() == targetFlow) &&
                !used.contains(triplet) &&
                (triplet.numBranches() == numBranchesSought))
              return triplet;
          } else {
            if (useTwoEdgeProfile) {
              long expectedFlow = triplet.flow();
              if (prevEdge != null)
                expectedFlow = Math.min(triplet.flow(), getTwoEdgeFreq(prevEdge, tempEdge));

              if ((expectedFlow == targetFlow) &&
                  !used.contains(triplet) &&
                  (triplet.numBranches() == numBranchesSought))
                return triplet;

            } else {
              if ((triplet.flow() >= targetFlow) &&
                  !used.contains(triplet) &&
                  (triplet.numBranches() == numBranchesSought) &&
                  ((bestTriplet == null) || (triplet.flow() < bestTriplet.flow())))
                bestTriplet = triplet;
            }
          }
        }
      }
    }

    // This only applies to potential flow or the incorrect algorithm.

    assert (bestTriplet != null):
      "Expected a triplet to be chosen " + prevEdge + " " + f + " " + v.getCfg().getRoutineName(); // TODO: fix

    return bestTriplet;
  }

  private static class FBPair implements java.lang.Comparable<FBPair>
  {
    public static int metric;

    private long flow;
    private int  numBranches;
    
    public FBPair(long flow, int numBranches)
    {
      this.flow        = flow;
      this.numBranches = numBranches;
    }

    public long flow()
    {
      return flow;
    }

    public int numBranches()
    {
      return numBranches;
    }

    public boolean equals(Object o)
    {
      FBPair pair = (FBPair) o;
      return (flow == pair.flow) && (numBranches == pair.numBranches);
    }

    public long getWeightedFlow(int metric)
    {
      switch (metric) {
      case PPCfg.RAW_METRIC:    return flow;
      case PPCfg.BRANCH_METRIC: return flow * numBranches;
      }
      throw new scale.common.InternalError("Unknown metric");
    }

    public int hashCode()
    {
      return (int) ((flow / 2) + (numBranches / 2));
    }

    public int compareTo(FBPair o2)
    {
      FBPair pair2 = o2;
      long   p1    = getWeightedFlow(metric);
      long   p2    = pair2.getWeightedFlow(metric);
      if (p2 < p1)
        return -1;
      if (p2 > p1)
        return 1;
      return 0;
    }
  }

  /**
   * Represents an (edge, flow, numBranches) triplet.
   */
  private static class EFBTriplet
  {
    private PPEdge edge;
    private long   flow;
    private int    numBranches;

    public EFBTriplet(PPEdge edge, long flow, int numBranches)
    {
      this.edge        = edge;
      this.flow        = flow;
      this.numBranches = numBranches;
    }

    public PPEdge edge()
    {
      return edge;
    }

    public long flow()
    {
      return flow;
    }

    public int numBranches()
    {
      return numBranches;
    }

    public boolean equals(Object o)
    {
      EFBTriplet triplet = (EFBTriplet) o;
      return edge.equals(triplet.edge) &&
             (flow == triplet.flow) &&
             (numBranches == triplet.numBranches);
    }

    public int hashCode()
    {
      return (int) (edge.hashCode() + flow + numBranches) / 3;
    }
  }

  private static class Path implements java.lang.Comparable<Path>
  {
    private PPCfg  cfg;
    private long   pathNum;
    private FBPair pair;

    public Path(Vector<PPEdge> edgeList, FBPair pair)
    {
      this.cfg = edgeList.firstElement().target().getCfg();
      this.pair = pair;

      assert (pair.numBranches() == PPCfg.getNumBranches(edgeList)) :
        "Number of branches mismatch";

      // Compute the path number.

      pathNum = 0;
      int l = edgeList.size();
      for (int i = 0; i < l; i++) {
        PPEdge edge = edgeList.get(i);
        if (!(edge.source() instanceof PPSupergraphBlock) &&
            !(edge.target() instanceof PPSupergraphBlock)) {
          pathNum += edge.getIncrement();
        }
      }
    }

    public Path(PPCfg cfg, long pathNum, FBPair pair)
    {
      this.cfg     = cfg;
      this.pathNum = pathNum;
      this.pair    = pair;
    }

    public PPCfg cfg()
    {
      return cfg;
    }

    public long pathNum()
    {
      return pathNum;
    }

    public FBPair pair()
    {
      return pair;
    }

    public int compareTo(Path o2)
    {
      FBPair pair1 = pair;
      FBPair pair2 = o2.pair;
      return pair1.compareTo(pair2);
    }

    public boolean equals(Object o)
    {
      Path path = (Path) o;
      return cfg.equals(path.cfg) && (pathNum == path.pathNum);
    }

    public int hashCode()
    {
      return (int) ((cfg.hashCode() / 2) + (pathNum / 2));
    }
  }

  /**
   * Get the number of branches on a path.
   */
  private static int getNumBranches(Vector<PPEdge> edgeList)
  {
    int numBranches = 0;
    int l           = edgeList.size();
    for (int i = 0; i < l; i++) {
      PPEdge edge = edgeList.get(i);
      if (edge.isBranchEdge())
        numBranches++;
    }
    return numBranches;
  }

  /**
   * Compute all paths (or all paths in a CFG) using the path profile.
   * @return A set of Path objects.
   */
  private static HashSet<Path> getActualPaths(PPSupergraphBlock superBegin,
                                       PPSupergraphBlock superEnd,
                                       PPCfg             cfgToUse)
  {
    HashSet<Path> hotPaths = new HashSet<Path>();

    PPEdge[] lo = superBegin.outgoing();
    if (lo == null)
      return hotPaths;

    for (int i = 0; i < lo.length; i++) { // For each cfg.
      PPEdge edge = lo[i];
      PPCfg  cfg  = edge.target().getCfg();

      if ((cfgToUse == null) || cfg.equals(cfgToUse)) {
        HashMap<Long, Long> pathFreqMap = cfg.pathFreqMap;
       
        Iterator<Long> pathNumIter = pathFreqMap.keySet().iterator();
        while (pathNumIter.hasNext()) { // For each taken path in the CFG.
          Long pn      = pathNumIter.next();
          long pathNum = pn.longValue();

          if (pathNum == -1) // Ignore lost paths.
            continue;

          // Construct the path; add SUPERBEGIN to the beginning and
          // SUPEREND to the end.

          Vector<PPEdge> path  = cfg.getEdgesOnPath(pathNum);
          PPBlock        begin = cfg.beginBlock();
          PPBlock        end   = cfg.endBlock();
          PPEdge         be    = cfg.getEdge(superBegin, begin, PPEdge.DUMMY_FOR_BACK_EDGE);
          PPEdge         ee    = cfg.getEdge(end, superEnd, PPEdge.DUMMY_FOR_BACK_EDGE);
          path.insertElementAt(be, 0);
          path.add(ee);

          // Add the path to the set of hot paths.

          long   freq  =  pathFreqMap.get(pn).longValue();
          FBPair pair  = new FBPair(freq, getNumBranches(path));
          Path   npath = new Path(path, pair);
          hotPaths.add(npath);
        }
      }
    }

    return hotPaths;
  }

  /**
   * Print flow information for a CFG or an entire program.
   */
  private static void printFlowInfo(String      flowName,
                                    PrintStream ps,
                                    long        actual,
                                    long        definite,
                                    long        potential,
                                    long        tepDefinite,
                                    long        tepPotential,
                                    boolean     useTwoEdgeProfile)
  {
    ps.print("  Actual ");
    ps.print(flowName);
    ps.print(" flow:           ");
    ps.println(commas(actual));
    ps.print("  Definite ");
    ps.print(flowName);
    ps.print(" flow:         ");
    ps.println(commas(definite));
    ps.print("  Potential ");
    ps.print(flowName);
    ps.print(" flow:        ");
    ps.println(commas(potential));

    if (useTwoEdgeProfile) {
      ps.print("  TEP Definite  ");
      ps.print(flowName);
      ps.print(" flow:    ");
      ps.println(commas(tepDefinite));
      ps.print("  TEP Potential ");
      ps.print(flowName);
      ps.print(" flow:    ");
      ps.println(commas(tepPotential));
    }

    StringBuffer padding = new StringBuffer();
    for (int n = 0; n < flowName.length(); n++) {
      padding.append(" ");
    }

    ps.print("  Definite / Actual:      ");
    ps.print(padding.toString());
    ps.println((actual > 0 ? (double)definite / actual : 0));
    ps.print("  Actual / Potential:     ");
    ps.print(padding.toString());
    ps.println((potential > 0 ? (double)actual / potential : 0));

    if (useTwoEdgeProfile) {
      ps.print("  TEP Definite / Actual:   ");
      ps.print(padding.toString());
      ps.println((actual > 0 ? (double)tepDefinite / actual : 0));
      ps.print("  TEP Actual / Potential:  ");
      ps.print(padding.toString());
      ps.println((tepPotential > 0 ? (double)actual / tepPotential : 0));
    }
  }

  /**
   * Print statistics for a CFG or the whole program.
   * @param cfg is a PPCfg.
   * @param ps is the PrintStream to print to.
   */
  private static void printStats(PPCfg cfg, PrintStream ps)
  {
    List<PPCfg> cfgList = new LinkedList<PPCfg>();
    cfgList.add(cfg);
    printStats(cfgList, ps);
  }

  /**
   * Print statistics for a CFG or the whole program.
   * @param cfgs is a list of PPCFGs.
   * @param ps is the PrintStream to print to.
   */
  private static void printStats(List<PPCfg> cfgList, PrintStream ps)
  {
    long numRoutines                 = 0;
    long numHashedRoutines           = 0;

    long numBlocks                   = 0;
    long numEdges                    = 0;
    long numBackEdges                = 0;
    long numDummyEdges               = 0;
    long numTruncatedEdges           = 0;

    long numStaticPaths              = 0;
    long numDistinctTakenPaths       = 0;

    long numBeginEndDistinctPaths    = 0;
    long numBeginTailDistinctPaths   = 0;
    long numHeaderEndDistinctPaths   = 0;
    long numHeaderTailDistinctPaths  = 0;

    long numBeginTruncDistinctPaths  = 0;
    long numHeaderTruncDistinctPaths = 0;
    long numTruncEndDistinctPaths    = 0;
    long numTruncTailDistinctPaths   = 0;
    long numTruncTruncDistinctPaths  = 0;

    long numDynamicPaths             = 0;
    long numHashedPaths              = 0;
    long numLostPaths                = 0;

    long numBeginEndDynamicPaths     = 0;
    long numBeginTailDynamicPaths    = 0;
    long numHeaderEndDynamicPaths    = 0;
    long numHeaderTailDynamicPaths   = 0;

    long numBeginTruncDynamicPaths   = 0;
    long numHeaderTruncDynamicPaths  = 0;
    long numTruncEndDynamicPaths     = 0;
    long numTruncTailDynamicPaths    = 0;
    long numTruncTruncDynamicPaths   = 0;

    Iterator<PPCfg> cfgIter = cfgList.iterator();

    while (cfgIter.hasNext()) {
      PPCfg cfg = cfgIter.next();
      numRoutines++;
      if (cfg.useHashing())
        numHashedRoutines++;

      numBlocks         += cfg.numBlocks();
      numEdges          += cfg.numEdges();
      numBackEdges      += cfg.backEdges.size();
      numDummyEdges     += cfg.dummyEdges.size();
      numTruncatedEdges += cfg.truncatedEdges.size();

      numStaticPaths    += cfg.beginBlock().getNumPaths();

      Iterator<Long> pathNumIter = cfg.getTakenPathNumbers();
      while (pathNumIter.hasNext()) {
        long pathNum = pathNumIter.next().longValue();
        long freq;
        if (pathNum == -1) {
          freq = cfg.getLostPathFreq();
          numLostPaths += freq;
        } else {
          freq = cfg.getPathFreq(pathNum);
          numDistinctTakenPaths++;

          Vector<PPEdge> edgeList  = cfg.getEdgesOnPath(pathNum);
          int            firstType = PPEdge.NORMAL;
          int            lastType  = PPEdge.NORMAL;
          if (edgeList.size() > 0) {
            firstType = edgeList.firstElement().getType();
            lastType  = edgeList.lastElement().getType();
          }

          if        (firstType == PPEdge.NORMAL                   && lastType == PPEdge.NORMAL) {
            numBeginEndDistinctPaths++;
            numBeginEndDynamicPaths += freq;
          } else if (firstType == PPEdge.NORMAL                   && lastType == PPEdge.DUMMY_FOR_BACK_EDGE) {
            numBeginTailDistinctPaths++;
            numBeginTailDynamicPaths += freq;
          } else if (firstType == PPEdge.DUMMY_FOR_BACK_EDGE      && lastType == PPEdge.NORMAL) {
            numHeaderEndDistinctPaths++;
            numHeaderEndDynamicPaths += freq;
          } else if (firstType == PPEdge.DUMMY_FOR_BACK_EDGE      && lastType == PPEdge.DUMMY_FOR_BACK_EDGE) {
            numHeaderTailDistinctPaths++;
            numHeaderTailDynamicPaths += freq;
          } else if (firstType == PPEdge.NORMAL                   && lastType == PPEdge.DUMMY_FOR_TRUNCATED_EDGE) {
            numBeginTruncDistinctPaths++;
            numBeginTruncDynamicPaths += freq;
          } else if (firstType == PPEdge.DUMMY_FOR_BACK_EDGE      && lastType == PPEdge.DUMMY_FOR_TRUNCATED_EDGE) {
            numHeaderTruncDistinctPaths++;
            numHeaderTruncDynamicPaths += freq;
          } else if (firstType == PPEdge.DUMMY_FOR_TRUNCATED_EDGE && lastType == PPEdge.NORMAL) {
            numTruncEndDistinctPaths++;
            numTruncEndDynamicPaths += freq;
          } else if (firstType == PPEdge.DUMMY_FOR_TRUNCATED_EDGE && lastType == PPEdge.DUMMY_FOR_BACK_EDGE) {
            numTruncTailDistinctPaths++;
            numTruncTailDynamicPaths += freq;
          } else if (firstType == PPEdge.DUMMY_FOR_TRUNCATED_EDGE && lastType == PPEdge.DUMMY_FOR_TRUNCATED_EDGE) {
            numTruncTruncDistinctPaths++;
            numTruncTruncDynamicPaths += freq;
          } else {
            throw new scale.common.InternalError("Unexpected condition");
          }
        }
        numDynamicPaths += freq;
        if (cfg.useHashing()) {
          numHashedPaths += freq;
        }
      }
    }

    ps.println("WHOLE PROGRAM STATISTICS:");
    ps.println();
    ps.print("  Routines:                         ");
    ps.println(commas(numRoutines));
    ps.print("    Array routines:                 ");
    ps.println(commas(numRoutines - numHashedRoutines));
    ps.print("    Hashed routines:                ");
    ps.println(commas(numHashedRoutines));

    ps.println();
    ps.print("  Blocks:                           " );
    ps.println(commas(numBlocks));
    ps.println();
    ps.print("  Edges (acyclic):                  ");
    ps.println(commas(numEdges));
    ps.print("    Dummy edges:                    ");
    ps.println(commas(numDummyEdges));
    ps.print("  Edges (cyclic):                   ");
    ps.println(commas(numEdges + numBackEdges + numTruncatedEdges - numDummyEdges));
    ps.print("    Back edges:                     ");
    ps.println(commas(numBackEdges));
    ps.print("    Truncated edges:                ");
    ps.println(commas(numTruncatedEdges));
    ps.println();
    ps.print("  Static paths:                     ");
    ps.println(commas(numStaticPaths));
    ps.println();
    ps.print("  Distinct taken paths:             ");
    ps.println(commas(numDistinctTakenPaths));
    ps.print("    BEGIN  -> END   distinct paths: ");
    ps.println(commas(numBeginEndDistinctPaths));
    ps.print("    BEGIN  -> TAIL  distinct paths: ");
    ps.println(commas(numBeginTailDistinctPaths));
    ps.print("    HEADER -> END   distinct paths: ");
    ps.println(commas(numHeaderEndDistinctPaths));
    ps.print("    HEADER -> TAIL  distinct paths: ");
    ps.println(commas(numHeaderTailDistinctPaths));
    ps.println();
    ps.print("    BEGIN  -> TRUNC distinct paths: ");
    ps.println(commas(numBeginTruncDistinctPaths));
    ps.print("    HEADER -> TRUNC distinct paths: ");
    ps.println(commas(numHeaderTruncDistinctPaths));
    ps.print("    TRUNC  -> END   distinct paths: ");
    ps.println(commas(numTruncEndDistinctPaths));
    ps.print("    TRUNC  -> TAIL  distinct paths: ");
    ps.println(commas(numTruncTailDistinctPaths));
    ps.print("    TRUNC  -> TRUNC distinct paths: ");
    ps.println(commas(numTruncTruncDistinctPaths));
    ps.println();
    ps.print("  Dynamic paths:                    ");
    ps.println(commas(numDynamicPaths));
    ps.print("    Dynamic array paths :           ");
    ps.println(commas(numDynamicPaths - numHashedPaths));
    ps.print("    Dynamic hashed paths:           ");
    ps.println(commas(numHashedPaths));
    ps.print("      Dynamic lost paths:           ");
    ps.println(commas(numLostPaths));
    ps.println();
    ps.print("    BEGIN  -> END   dynamic paths:  ");
    ps.println(commas(numBeginEndDynamicPaths));
    ps.print("    BEGIN  -> TAIL  dynamic paths:  ");
    ps.println(commas(numBeginTailDynamicPaths));
    ps.print("    HEADER -> END   dynamic paths:  ");
    ps.println(commas(numHeaderEndDynamicPaths));
    ps.print("    HEADER -> TAIL  dynamic paths:  ");
    ps.println(commas(numHeaderTailDynamicPaths));
    ps.println();
    ps.print("    BEGIN  -> TRUNC dynamic paths:  ");
    ps.println(commas(numBeginTruncDynamicPaths));
    ps.print("    HEADER -> TRUNC dynamic paths:  ");
    ps.println(commas(numHeaderTruncDynamicPaths));
    ps.print("    TRUNC  -> END   dynamic paths:  ");
    ps.println(commas(numTruncEndDynamicPaths));
    ps.print("    TRUNC  -> TAIL  dynamic paths:  ");
    ps.println(commas(numTruncTailDynamicPaths));
    ps.print("    TRUNC  -> TRUNC dynamic paths:  ");
    ps.println(commas(numTruncTruncDynamicPaths));
    ps.println();

    ps.println("END OF WHOLE PROGRAM STATISTICS");

    ps.println();
  }

  private static String commas(long num)
  {
    StringBuffer buf = new StringBuffer();
    if (num < 0) {
      buf.append("-");
      num = -num;
    }

    String s = Long.toString(num);
    int    l = s.length();
    for (int i = 0; i < l; i++) {
      buf.append(s.charAt(i));
      if (((l - i) % 3 == 1) && (i != l - 1))
        buf.append(',');
    }

    return buf.toString();
  }

  /**
   * Called by Suite.addProfiling() in an order determined by
   * profile-guided profiling.  This allows us to not instrument
   * certain routines if we get enough ADF from instrumenting some
   * routines which must include the module containing "main".
   * @param profileOptions specifies which profiling instrumentation to insert
   */
  public static void addProfilingInSpecialOrder(int profileOptions)
  {
    HashSet<Path> allProgDefiniteFlowPaths = defFlowPaths;

    // Put the CFGs in a list.

    LinkedList<PPCfg> cfgList = new LinkedList<PPCfg>();
    PPEdge[]          lo      = superBegin.outgoing();
    if (lo != null) {
      for (int i = 0; i < lo.length; i++) {
        PPEdge edge = lo[i];
        cfgList.add(edge.target().getCfg());
      }
    }

    // Get the proportion of program flow that routine must have to
    // not be cold.

    double coldRoutineThreshold = pgpColdRoutineThreshold;

    // Get the total flow through the program.

    HashSet<Path> progTakenPaths = getActualPaths(superBegin, superEnd, null);
    long    progActualRawFlow    = getBlockFreq2(superBegin);
    long    progActualBranchFlow = getTotalFlow(progTakenPaths, BRANCH_METRIC);

    // Find the cold routines and also compute attribution of definite
    // flow (ADF).

    HashSet<PPCfg>               coldRoutines = WorkArea.<PPCfg>getSet("addProfilingInSpecialOrder");
    final HashMap<PPCfg, Double> rawAdfMap    = new HashMap<PPCfg, Double>();
    final HashMap<PPCfg, Double> branchAdfMap = new HashMap<PPCfg, Double>();

    Iterator<PPCfg> cfgIter = cfgList.iterator();
    while (cfgIter.hasNext()) {
      PPCfg         cfg                 = cfgIter.next();
      HashSet<Path> cfgTakenPaths       = getActualPaths(superBegin, superEnd, cfg);
      long          cfgActualRawFlow    = cfg.getBlockFreq2(cfg.beginBlock());
      long          cfgActualBranchFlow = getTotalFlow(cfgTakenPaths, BRANCH_METRIC);

      // Identify cold routines using the branch metric.

      if (((double) cfgActualBranchFlow / progActualBranchFlow) < coldRoutineThreshold)
        coldRoutines.add(cfg);

      // Compute ADF for the CFG.

      HashMap<FBPair, Long> df           = definiteFlow.get(cfg.beginBlock());
      long    cfgTotalDefiniteRawFlow    = getTotalFlowInFlowMap(df, RAW_METRIC);
      long    cfgTotalDefiniteBranchFlow = getTotalFlowInFlowMap(df, BRANCH_METRIC);

      double rawAdf = 1.0;
      if (cfgActualRawFlow > 0)
        rawAdf = (double)cfgTotalDefiniteRawFlow / cfgActualRawFlow;

      double branchAdf = 1.0;
      if (cfgActualBranchFlow > 0)
        branchAdf = (double) cfgTotalDefiniteBranchFlow / cfgActualBranchFlow;

      rawAdfMap.put(cfg, new Double(rawAdf));
      branchAdfMap.put(cfg, new Double(branchAdf));
    }

    // Get the program's total definite flow.

    HashMap<FBPair, Long> dfs                         = definiteFlow.get(superBegin);
    long    progTotalDefiniteRawFlow    = getTotalFlowInFlowMap(dfs, RAW_METRIC);
    long    progTotalDefiniteBranchFlow = getTotalFlowInFlowMap(dfs, BRANCH_METRIC);

    // Sort the CFGs by attribution of definite flow using the branch
    // metric.

    Collections.sort(cfgList, new Comparator<PPCfg>() {
                                public int compare(PPCfg o1, PPCfg o2) {
                                  PPCfg cfg1 = o1;
                                  PPCfg cfg2 = o2;
                                  return branchAdfMap.get(cfg1).compareTo(branchAdfMap.get(cfg2));
                                }   
                              });

    // The total amount of flow that we can determine from the edge
    // profile and the instrumentation we're adding.  We start with no
    // instrumentation, so we just have the definite flow from the
    // edge profile so far.  We have to use the raw metric for this.
    // We keep track of the minimum amount of raw flow measured and
    // the actual amount of raw flow measured.

    long progTotalMinimumRawFlowMeasured = progTotalDefiniteRawFlow;
    long progTotalActualRawFlowMeasured  = progTotalDefiniteRawFlow;

    // We'll also keep track of the total amount of branch flow
    // measured, although a real dynamic optimizer couldn't do that at
    // this point.

    long progTotalActualBranchFlowMeasured = progTotalDefiniteBranchFlow;

    // Figure out how much total flow we want to be able to measure
    // for this program.

    long progTotalRawFlowDesired = (long) (progActualRawFlow * pgpDesiredAdf);

    // Adjustment amounts.

    long deductedProgTotalMinimumRawFlowMeasured = 0;
    long deductedProgTotalRawFlowDesired         = 0;

    if (debuggingOutput) {
      System.out.print("PGP: Total raw flow is                ");
      System.out.println(commas(progActualRawFlow));
      System.out.print("     Raw flow from edge profile is    ");
      System.out.println(commas(progTotalDefiniteRawFlow));
      System.out.print("     Raw flow desired is              ");
      System.out.println(commas(progTotalRawFlowDesired));
      System.out.println("---------------------------------------------------");
      System.out.print("     Total branch flow is             ");
      System.out.println(commas(progActualBranchFlow));
      System.out.print("     Branch flow from edge profile is ");
      System.out.println(commas(progTotalDefiniteBranchFlow));
    }

    HashSet<Path>   progMeasuredPaths                   = WorkArea.<Path>getSet("addProfilingInSpecialOrder");
    HashSet<Path>   progDefiniteFlowPaths               = WorkArea.<Path>getSet("addProfilingInSpecialOrder");
    HashSet<Path>   progHotPathsFromRawPotentialFlow    = WorkArea.<Path>getSet("addProfilingInSpecialOrder");
    HashSet<Path>   progHotPathsFromBranchPotentialFlow = WorkArea.<Path>getSet("addProfilingInSpecialOrder");
    HashSet<Path>   progOvercountedPaths                = WorkArea.<Path>getSet("addProfilingInSpecialOrder");
    HashSet<PPEdge> progIncomingOvercountEdges          = WorkArea.<PPEdge>getSet("addProfilingInSpecialOrder");
    HashSet<PPEdge> progOutgoingOvercountEdges          = WorkArea.<PPEdge>getSet("addProfilingInSpecialOrder");

    // Instrument routines in increasing ADF order until we have the
    // proportion we're aiming for. Ignore cold routines.

    cfgIter = cfgList.iterator();
    while (cfgIter.hasNext()) {
      PPCfg cfg = cfgIter.next();

      // Compute info about the routine.

      HashSet<Path> cfgTakenPaths              = getActualPaths(superBegin, superEnd, cfg);
      long    cfgActualRawFlow                 = cfg.getBlockFreq2(cfg.beginBlock());
      long    cfgActualBranchFlow              = getTotalFlow(cfgTakenPaths, BRANCH_METRIC);
      HashMap<FBPair, Long> df                 = definiteFlow.get(cfg.beginBlock());
      long    cfgTotalDefiniteRawFlow          = getTotalFlowInFlowMap(df, RAW_METRIC);
      long    cfgTotalDefiniteBranchFlow       = getTotalFlowInFlowMap(df, BRANCH_METRIC);
      HashSet<Path> cfgActualHotPathsUsingBranchFlow = computeHotPaths(cfgTakenPaths,
                                                                 (long)(0.00125 * progActualBranchFlow),
                                                                 Long.MAX_VALUE,
                                                                 Integer.MAX_VALUE,
                                                                 BRANCH_METRIC);      

      // decide whether to instrument the routine
      boolean instrumentRoutine = ((!coldRoutines.contains(cfg)) &&
                                   (((progTotalMinimumRawFlowMeasured - deductedProgTotalMinimumRawFlowMeasured) <
                                     (progTotalRawFlowDesired - deductedProgTotalRawFlowDesired)) &&
                                    (((double) cfgTotalDefiniteBranchFlow / cfgActualBranchFlow) <
                                     pgpRoutineAdfThreshold)));

      if (debuggingOutput) {
        System.out.println("****************************************************************");
        if (instrumentRoutine) {
          System.out.print("PGP: Let's instrument ");
          System.out.print(cfg.getRoutineName());
          System.out.println("...");
        } else {
          System.out.print("PGP: Decided to NOT instrument ");
          System.out.print(cfg.getRoutineName());
          System.out.print(" because ");
          System.out.println((coldRoutines.contains(cfg) ? "it's cold" : "we already have enough ADF"));
        }
        System.out.print("     Raw    actual flow is ");
        System.out.print(commas(cfgActualRawFlow));
        System.out.print("; DF is ");
        System.out.print(commas(cfgTotalDefiniteRawFlow));
        System.out.print(" (");
        System.out.print(rawAdfMap.get(cfg).doubleValue() * 100);
        System.out.println("%)");
        System.out.print("     Branch actual flow is ");
        System.out.print(commas(cfgActualBranchFlow));
        System.out.print("; DF is ");
        System.out.print(commas(cfgTotalDefiniteBranchFlow));
        System.out.print(" (");
        System.out.print(branchAdfMap.get(cfg).doubleValue() * 100);
        System.out.println("%)");
        if (!cfgActualHotPathsUsingBranchFlow.isEmpty()) {
          System.out.print("     Contains ");
          System.out.print(cfgActualHotPathsUsingBranchFlow.size());
          System.out.println(" actual paths that are hot:");
          Iterator<Path> pathIter = cfgActualHotPathsUsingBranchFlow.iterator();
          while (pathIter.hasNext()) {
            Path path       = pathIter.next();
            long branchFlow = path.pair().getWeightedFlow(BRANCH_METRIC);
            System.out.print("       Path with length ");
            System.out.print(path.pair().numBranches());
            System.out.print(" and branch flow ");
            System.out.print(commas(branchFlow));
            System.out.print(" (");
            System.out.print((100.0 * branchFlow / cfgActualBranchFlow));
            System.out.print("% routine, ");
            System.out.print((100.0 * branchFlow / progActualBranchFlow));
            System.out.println("% total)");
          }
        }
      }

      WorkArea.<PPCfg>returnSet(coldRoutines);

      Scribble    scribble  = cfg.scribble;
      RoutineDecl rd        = scribble.getRoutineDecl();
      CallGraph   callGraph = rd.getCallGraph();
      boolean     isMain    = (rd == callGraph.getMain());

      // Try doing path profiling on the routine.

      boolean usingPotentialFlowForCfg = false;
      if (instrumentRoutine) {
        // We want to keep cold edges if the original CFG uses an
        // array AND we're in TPP mode.

        boolean avoidRemovingColdEdges = !cfg.useHashing() && !pgpAlwaysRemoveColdEdges;

        // Create the path profiling CFG and do all the analysis for
        // instrumentation in order to see if we really want to do
        // instrumentation.

        PPCfg   cfgWeMightInstrument = new PPCfg(scribble, cfg);
        HashMap<PPEdge, Instr> aiMap = cfgWeMightInstrument.doAnalysis(false, !avoidRemovingColdEdges);
        if (aiMap == null) {
          instrumentRoutine = false;
          if (debuggingOutput) {
            System.out.print("PGP: Decided to NOT instrument ");
            System.out.print(cfg.getRoutineName());
            System.out.println(" because there was nothing left after removing cold and obvious edges");
          }
        } else if (cfgWeMightInstrument.useHashing()) {
          // Check if we should even instrument this.

          if (pgpAvoidHopelessHashRoutines) {
            // Hashing is too expensive, so don't instrument this routine.
            instrumentRoutine = false;
            // Adjust the targeted amount of ADF that we're trying to get.
            deductedProgTotalRawFlowDesired         += (long) (cfgActualRawFlow * pgpDesiredAdf);
            deductedProgTotalMinimumRawFlowMeasured += cfgTotalDefiniteRawFlow;

            // Get a set of hot paths using potential flow.

            usingPotentialFlowForCfg = true;

            HashSet<Path> cfgHotPathsFromRawPotentialFlow    = computeHotPaths(potentialFlow,
                                                                         superBegin,
                                                                         superEnd,
                                                                         Long.MAX_VALUE,
                                                                         cfgActualRawFlow,
                                                                         RAW_METRIC,
                                                                         POTENTIAL_FLOW,
                                                                         cfg,
                                                                         false,
                                                                         false);
            HashSet<Path> cfgHotPathsFromBranchPotentialFlow = computeHotPaths(potentialFlow,
                                                                         superBegin,
                                                                         superEnd,
                                                                         Long.MAX_VALUE,
                                                                         cfgActualBranchFlow,
                                                                         BRANCH_METRIC,
                                                                         POTENTIAL_FLOW,
                                                                         cfg,
                                                                         false,
                                                                         false);

            // Add these paths to the set of program paths predicted from potential flow.

            progHotPathsFromRawPotentialFlow.addAll(cfgHotPathsFromRawPotentialFlow);
            progHotPathsFromBranchPotentialFlow.addAll(cfgHotPathsFromBranchPotentialFlow);

            if (debuggingOutput) {
              System.out.print("PGP: Decided to NOT instrument ");
              System.out.print(cfg.getRoutineName());
              System.out.println(" because it still uses hashing");
              System.out.println("     Let's use potential flow instead to predict hot paths");
              System.out.print("     Raw    metric: ");
              System.out.print(commas(progHotPathsFromRawPotentialFlow.size()));
              System.out.print(" paths with ");
              System.out.print((100.0 * getTotalFlow(cfgHotPathsFromRawPotentialFlow, RAW_METRIC) /
                                cfgActualRawFlow));
              System.out.println("% flow");
              System.out.print("     Branch metric: ");
              System.out.print(commas(progHotPathsFromBranchPotentialFlow.size()));
              System.out.print(" paths with ");
              System.out.print((100.0 * getTotalFlow(cfgHotPathsFromRawPotentialFlow, RAW_METRIC) /
                                cfgActualRawFlow));
              System.out.println("% flow");

              // Now let's see how well potential flow predicts the hot paths in this routine.

              hotPathAnalysis("CFG partial potential flow",
                              "branch",
                              null,
                              cfgHotPathsFromBranchPotentialFlow,
                              cfgActualHotPathsUsingBranchFlow,
                              superBegin,
                              superEnd,
                              0.0,
                              0.0,
                              BRANCH_METRIC,
                              0,
                              false,
                              false);
            }
          } else if (!pgpAlwaysRemoveColdEdges) {
            // We don't want to remove the cold edges because we have
            // to hash anyway; do instrumentation again.
            if (debuggingOutput)
              System.out.println("PGP: The routine is still hashed after removing edges," +
                                 " so let's try again but not remove cold edges this time");
            cfgWeMightInstrument = new PPCfg(scribble, cfg);
            aiMap = cfgWeMightInstrument.doAnalysis(false, false);
            assert (aiMap != null) :
              "No edges left in CFG; would have expected instrumentation to fail earlier";
          } else if (pgpFlexibleColdFactor > 1.0) {
            // Let's increase the global cold edge criterion.
            
            double coldFactor    = pgpFlexibleColdFactor;
            double origThreshold = pgpGlobalColdEdgeThreshold;
            double currThreshold = origThreshold;

            if (debuggingOutput) {
              System.out.print("PGP: Let's raise the global cold edge threshold (starting at ");
              System.out.print(origThreshold);
              System.out.println(")");
            }

            do {
              currThreshold *= coldFactor;
              setPgpGlobalColdEdgeThreshold(currThreshold);

              if (debuggingOutput)
                System.out.println("     Trying " + currThreshold + "...");

              cfgWeMightInstrument = new PPCfg(scribble, cfg);
              aiMap = cfgWeMightInstrument.doAnalysis(false, true);

              if (aiMap == null) {
                if (debuggingOutput) {
                  System.out.println("     No edges left in CFG; routine will not be instrumented");
                  instrumentRoutine = false;
                }
                break;
              }
            } while (cfgWeMightInstrument.useHashing());

            if (aiMap != null)
              System.out.println("     Success!");

            // Important: restore the global edge threshold.

            setPgpGlobalColdEdgeThreshold(origThreshold);
          }
        }

        // Check if we still want to instrument the routine.

        if (instrumentRoutine) {
          // We will indeed instrument this routine, so save all the
          // work we just did.
          if (pgpCfgMap == null)
            pgpCfgMap = new HashMap<PPCfg, PPCfg>();
          pgpCfgMap.put(cfg, cfgWeMightInstrument);
          if (pgpAbstractInstrMap == null)
            pgpAbstractInstrMap = new HashMap<PPCfg, HashMap<PPEdge, Instr>>();
          pgpAbstractInstrMap.put(cfgWeMightInstrument, aiMap);
        }
      }

      // Add profiling instrumentation to the routine.

      if (instrumentRoutine) {
        scribble.addProfiling(profileOptions, isMain);

        PPCfg instrumentedCfg = scribble.getPPCfg();

        // Compute the minimum and actual amounts of flow measured.

        HashSet<Path>   cfgMeasuredPaths          = instrumentedCfg.measuredPaths;
        HashSet<Path>   cfgDefiniteFlowPaths      = instrumentedCfg.definiteFlowPaths;
        HashSet<Path>   cfgOvercountedPaths       = instrumentedCfg.overcountedPaths;
        HashSet<PPEdge> cfgIncomingOvercountEdges = instrumentedCfg.incomingOvercountEdges;
        HashSet<PPEdge> cfgOutgoingOvercountEdges = instrumentedCfg.outgoingOvercountEdges;

        long cfgMeasuredPathsRawFlow         = getTotalFlow(cfgMeasuredPaths,     RAW_METRIC);
        long cfgMeasuredPathsBranchFlow      = getTotalFlow(cfgMeasuredPaths,     BRANCH_METRIC);
        long cfgDefiniteFlowPathsRawFlow     = getTotalFlow(cfgDefiniteFlowPaths, RAW_METRIC);
        long cfgDefiniteFlowPathsBranchFlow  = getTotalFlow(cfgDefiniteFlowPaths, BRANCH_METRIC);
        long cfgOvercountedPathsRawFlow      = getTotalFlow(cfgOvercountedPaths,  RAW_METRIC);
        long cfgOvercountedPathsBranchFlow   = getTotalFlow(cfgOvercountedPaths,  BRANCH_METRIC);
        long cfgIncomingOvercountEdgeRawFlow = getEdgeFreqSum(cfgIncomingOvercountEdges);
        long cfgOutgoingOvercountEdgeRawFlow = getEdgeFreqSum(cfgOutgoingOvercountEdges);

        if (debuggingOutput) {
          System.out.print("     Profiled path flow:           ");
          System.out.print(commas(cfgMeasuredPathsRawFlow));
          System.out.print(" (");
          System.out.print(commas(cfgMeasuredPathsBranchFlow));
          System.out.println(" branch)");
          System.out.print("     Unmeasured definite flow:     ");
          System.out.print(commas(cfgDefiniteFlowPathsRawFlow));
          System.out.print(" (");
          System.out.print(commas(cfgDefiniteFlowPathsBranchFlow));
          System.out.println(" branch)");
          System.out.print("     Overcounted path flow:        ");
          System.out.print(commas(cfgOvercountedPathsRawFlow));
          System.out.print(" (");
          System.out.print(commas(cfgOvercountedPathsBranchFlow));
          System.out.println(" branch)");
          System.out.print("     Incoming overcount edge flow: ");
          System.out.println(commas(cfgIncomingOvercountEdgeRawFlow));
          System.out.print("     Outgoing overcount edge flow: ");
          System.out.println(commas(cfgOutgoingOvercountEdgeRawFlow));
        }

        // update the amount of total flow that we'll be able to measure now

        long cfgMinimumRawFlowMeasured   = (cfgMeasuredPathsRawFlow         +
                                            cfgDefiniteFlowPathsRawFlow     -
                                            cfgIncomingOvercountEdgeRawFlow -
                                            cfgOutgoingOvercountEdgeRawFlow);
        long cfgActualRawFlowMeasured    = (cfgMeasuredPathsRawFlow     +
                                            cfgDefiniteFlowPathsRawFlow -
                                            cfgOvercountedPathsRawFlow);
        long cfgActualBranchFlowMeasured = (cfgMeasuredPathsBranchFlow     +
                                            cfgDefiniteFlowPathsBranchFlow -
                                            cfgOvercountedPathsBranchFlow);

        if (debuggingOutput) {
          System.out.print("     Minimum total flow measured:    ");
          System.out.println(commas(cfgMinimumRawFlowMeasured));
          System.out.print("     Actual  total flow measured:    ");
          System.out.print(commas(cfgActualRawFlowMeasured));
          System.out.print(" (");
          System.out.print(commas(cfgActualBranchFlowMeasured));
          System.out.println(" branch)");
        }

        // update the total program info
        progTotalMinimumRawFlowMeasured   += (cfgMinimumRawFlowMeasured - cfgTotalDefiniteRawFlow); // this is the amount we're sure we can measure
        progTotalActualRawFlowMeasured    += (cfgActualRawFlowMeasured - cfgTotalDefiniteRawFlow); // this is the amount we end up being able to measure
        progTotalActualBranchFlowMeasured += (cfgActualBranchFlowMeasured - cfgTotalDefiniteBranchFlow);

        progMeasuredPaths.addAll(cfgMeasuredPaths);
        progDefiniteFlowPaths.addAll(cfgDefiniteFlowPaths);
        progOvercountedPaths.addAll(cfgOvercountedPaths);
        progIncomingOvercountEdges.addAll(cfgIncomingOvercountEdges);
        progOutgoingOvercountEdges.addAll(cfgOutgoingOvercountEdges);
      } else {
        if (routinesToNotInstrument == null)
          routinesToNotInstrument = new HashSet<Scribble>();
        routinesToNotInstrument.add(cfg.scribble);

        // We use all (and only) definite flow paths in routines we
        // don't instrument, unless we already used potential flow.

        if (!usingPotentialFlowForCfg) {
          Iterator<Path> pathIter = allProgDefiniteFlowPaths.iterator();
          while (pathIter.hasNext()) {
            Path path = pathIter.next();
            if (path.cfg().equals(cfg))
              progDefiniteFlowPaths.add(path);
          }
        }
      }
    }

    if (debuggingOutput) {
      System.out.println("PGP: We're done!");
      System.out.print("PGP: Minimum measured raw   flow is ");
      System.out.print(commas(progTotalMinimumRawFlowMeasured));
      System.out.print(" (");
      System.out.print((((double)progTotalMinimumRawFlowMeasured / progActualRawFlow) * 100));
      System.out.println("% of actual raw    flow)");
      System.out.print("PGP: Actual  measured raw   flow is ");
      System.out.print(commas(progTotalActualRawFlowMeasured));
      System.out.print(" (");
      System.out.print((((double)progTotalActualRawFlowMeasured / progActualRawFlow) * 100));
      System.out.println("% of actual raw    flow)");
      System.out.print("PGP: After instrumenting, measured branch flow is ");
      System.out.print(commas(progTotalActualBranchFlowMeasured));
      System.out.print(" (");
      System.out.print((((double)progTotalActualBranchFlowMeasured / progActualBranchFlow) * 100));
      System.out.println("% of actual branch flow)");
      System.out.println();

      long progMeasuredPathsRawFlow         = getTotalFlow(progMeasuredPaths,                   RAW_METRIC);
      long progMeasuredPathsBranchFlow      = getTotalFlow(progMeasuredPaths,                   BRANCH_METRIC);
      long progDefiniteFlowPathsRawFlow     = getTotalFlow(progDefiniteFlowPaths,               RAW_METRIC);
      long progDefiniteFlowPathsBranchFlow  = getTotalFlow(progDefiniteFlowPaths,               BRANCH_METRIC);
      long progPotentialFlowPathsRawFlow    = getTotalFlow(progHotPathsFromRawPotentialFlow,    RAW_METRIC);
      long progPotentialFlowPathsBranchFlow = getTotalFlow(progHotPathsFromBranchPotentialFlow, BRANCH_METRIC);
      long progOvercountedPathsRawFlow      = getTotalFlow(progOvercountedPaths,                RAW_METRIC);
      long progOvercountedPathsBranchFlow   = getTotalFlow(progOvercountedPaths,                BRANCH_METRIC);
      long progIncomingOvercountEdgeRawFlow = getEdgeFreqSum(progIncomingOvercountEdges);
      long progOutgoingOvercountEdgeRawFlow = getEdgeFreqSum(progOutgoingOvercountEdges);

      if (debuggingOutput) {
        System.out.println("------------------------------------------");
        System.out.print("PGP: Profiled path flow:       ");
        System.out.print(commas(progMeasuredPathsRawFlow));
        System.out.print(" (");
        System.out.print(commas(progMeasuredPathsBranchFlow));
        System.out.println(" branch)");
        System.out.print("PGP: Unmeasured definite flow: ");
        System.out.print(commas(progDefiniteFlowPathsRawFlow));
        System.out.print(" (");
        System.out.print(commas(progDefiniteFlowPathsBranchFlow));
        System.out.println(" branch)");
        System.out.print("PGP: Potential flow used:      ");
        System.out.print(commas(progPotentialFlowPathsRawFlow));
        System.out.print(" (");
        System.out.print(commas(progPotentialFlowPathsBranchFlow));
        System.out.println(" branch)");
        System.out.print("PGP: Overcounted path flow:    ");
        System.out.print(commas(progOvercountedPathsRawFlow));
        System.out.print(" (");
        System.out.print(commas(progOvercountedPathsBranchFlow));
        System.out.println(" branch)");
        System.out.print("PGP: Incoming overcount edge flow:      ");
        System.out.println(commas(progIncomingOvercountEdgeRawFlow));
        System.out.print("PGP: Outgoing overcount edge flow:      ");
        System.out.println(commas(progOutgoingOvercountEdgeRawFlow));
        System.out.println("------------------------------------------");
      }

      long progMinimumRawFlowMeasured   = (progMeasuredPathsRawFlow         +
                                           progDefiniteFlowPathsRawFlow     -
                                           progIncomingOvercountEdgeRawFlow -
                                           progOutgoingOvercountEdgeRawFlow);
      long progActualRawFlowMeasured    = (progMeasuredPathsRawFlow     +
                                           progDefiniteFlowPathsRawFlow -
                                           progOvercountedPathsRawFlow);
      long progActualBranchFlowMeasured = (progMeasuredPathsBranchFlow     +
                                           progDefiniteFlowPathsBranchFlow -
                                           progOvercountedPathsBranchFlow);

      if (debuggingOutput) {
        System.out.println("------------------------------------------");
        System.out.print("PGP: Minimum total flow measured:    ");
        System.out.println(commas(progMinimumRawFlowMeasured));
        System.out.print("PGP: Actual  total flow measured:    ");
        System.out.print(commas(progActualRawFlowMeasured));
        System.out.print(" (");
        System.out.print(commas(progActualBranchFlowMeasured));
        System.out.println(" branch)");
        System.out.println("------------------------------------------");
      }
    }

    // Print information about the paths that we're profiling.

    System.out.println("The paths we're profiling:");
    printPathLengthHistogram(progMeasuredPaths, true, false);

    // Now do hot path comparison: the estimated path profile vs. the
    // real path profile.

    HashSet<Path> estimatedPaths = combinePathSets(progDefiniteFlowPaths,
                                             combinePathSets(progHotPathsFromBranchPotentialFlow,
                                                             combinePathSets(progMeasuredPaths,
                                                                             progOvercountedPaths)));

    // Print information about the paths that we're measuring.

    System.out.println("All paths that we're using (we just assume everything is array, not hashed):");
    printPathLengthHistogram(estimatedPaths, true, true);
    
    thoroughHotPathAnalysis(superBegin,
                            superEnd,
                            null,
                            0,
                            "ESTIMATED",
                            estimatedPaths,
                            progTakenPaths,
                            false);

    WorkArea.<Path>returnSet(progMeasuredPaths);
    WorkArea.<Path>returnSet(progDefiniteFlowPaths);
    WorkArea.<Path>returnSet(progHotPathsFromRawPotentialFlow);
    WorkArea.<Path>returnSet(progHotPathsFromBranchPotentialFlow);
    WorkArea.<Path>returnSet(progOvercountedPaths);
    WorkArea.<PPEdge>returnSet(progIncomingOvercountEdges);
    WorkArea.<PPEdge>returnSet(progOutgoingOvercountEdges);
  }

  private static HashSet<Path> combinePathSets(HashSet<Path> pathSet1, HashSet<Path> pathSet2)
  {
    HashSet<Path> combinedPathSet = new HashSet<Path>();

    HashMap<Path, Path>  map      = new HashMap<Path, Path>();
    Iterator<Path>       pathIter = pathSet1.iterator();
    while (pathIter.hasNext()) {
      Path path = pathIter.next();
      map.put(path, path); // just map the from itself to itself
      combinedPathSet.add(path);
    }

    pathIter = pathSet2.iterator();
    while (pathIter.hasNext()) {
      Path path      = pathIter.next();
      Path otherPath = map.get(path);

      if (otherPath != null) {
        long   flow  = path.pair().flow() + otherPath.pair().flow();
        int    numbr = path.pair().numBranches();
        FBPair pair  = new FBPair(flow,numbr);
        path = new Path(path.cfg(), path.pathNum(), pair);
      }

      combinedPathSet.remove(path);
      combinedPathSet.add(path);
    }

    return combinedPathSet;
  }

  private static long getEdgeFreqSum(HashSet<PPEdge> edges)
  {
    Iterator<PPEdge> edgeIter  = edges.iterator();
    long     totalFreq = 0;
    while (edgeIter.hasNext()) {
      PPEdge edge = edgeIter.next();
      totalFreq += edge.getFrequency();
    }

    return totalFreq;
  }

  public static void printPathLengthHistogram(HashSet<Path> paths,
                                              boolean       useLatestCfgs,
                                              boolean       assumeAllArrays)
  {
    TreeMap<Integer, long[]> pathLengthMap   = new TreeMap<Integer, long[]>();
    long[]  totalRawFlow    = new long[] { 0, 0 };
    long[]  totalBranchFlow = new long[] { 0, 0 };

    Iterator<Path> pathIter = paths.iterator();
    while (pathIter.hasNext()) {
      Path    path         = pathIter.next();
      Integer numBranches  = new Integer(path.pair().numBranches());
      long    currentFreq  = 0;
      long[]  currentFreqs = pathLengthMap.get(numBranches);

      if (currentFreqs == null)
        currentFreqs = new long[] { 0, 0 };

      int arrayIndex = 0;
      if (!assumeAllArrays) {
        PPCfg cfg = useLatestCfgs ? path.cfg().scribble.getPPCfg() : path.cfg();
        arrayIndex = cfg.useHashing() ? 1 : 0;
      }

      currentFreqs[arrayIndex] += path.pair().flow();

      pathLengthMap.put(numBranches, currentFreqs);

      totalRawFlow[arrayIndex]    += path.pair().getWeightedFlow(RAW_METRIC);
      totalBranchFlow[arrayIndex] += path.pair().getWeightedFlow(BRANCH_METRIC);
    }

    Iterator<Integer> numBranchesIter = pathLengthMap.keySet().iterator();
    System.out.println("-------------------------");
    while (numBranchesIter.hasNext()) {
      Integer numBranches = numBranchesIter.next();
      long[]  freqs       = pathLengthMap.get(numBranches);
      System.out.print("Dynamic raw flow from paths with ");
      System.out.print(numBranches);
      System.out.print(" branches: ");
      System.out.print(commas(freqs[0] + freqs[1]));
      System.out.print(" (");
      System.out.print(commas(freqs[0]));
      System.out.print(" array, ");
      System.out.print(commas(freqs[1]));
      System.out.println(" hashed)");
    }
    System.out.print("Raw    flow from all paths:                 ");
    System.out.print(commas(totalRawFlow[0] + totalRawFlow[1]));
    System.out.print(" (");
    System.out.print(commas(totalRawFlow[0]));
    System.out.print(" array, ");
    System.out.print(commas(totalRawFlow[1]));
    System.out.println(" hashed)");
    System.out.print("Branch flow from all paths:                 ");
    System.out.print(commas(totalBranchFlow[0] + totalBranchFlow[1]));
    System.out.print(" (");
    System.out.print(commas(totalBranchFlow[0]));
    System.out.print(" array, ");
    System.out.print(commas(totalBranchFlow[1]));
    System.out.println(" hashed)");
    System.out.println("-------------------------");

    // Now also do a histogram for the number of instructions (well,
    // Chords) in paths.

    TreeMap<Integer, Long> pathInstrMap = new TreeMap<Integer, Long>();
    pathIter = paths.iterator();
    while (pathIter.hasNext()) {
      Path     path      = pathIter.next();
      Iterator<PPBlock> blockIter = path.cfg().getBlocksOnPath(path.pathNum()).iterator();
      int      numInstrs = 0;
      while (blockIter.hasNext()) {
        PPBlock block = blockIter.next();
        numInstrs += block.getNumChords();
      }

      long currentFlow = 0;
      if (pathInstrMap.containsKey(new Integer(numInstrs)))
        currentFlow = pathInstrMap.get(new Integer(numInstrs)).longValue();

      pathInstrMap.put(new Integer(numInstrs), new Long(currentFlow + path.pair().flow()));
    }

    Iterator<Integer> numInstrsIter      = pathInstrMap.keySet().iterator();
    long     totalDynamicInstrs = 0;
    long     totalDynamicPaths  = 0;
    while (numInstrsIter.hasNext()) {
      Integer numInstrs       = numInstrsIter.next();
      long    numDynamicPaths = pathInstrMap.get(numInstrs).longValue();
      totalDynamicInstrs += numInstrs.intValue() * numDynamicPaths;
      totalDynamicPaths += numDynamicPaths;
      System.out.print("No. paths with ");
      System.out.print(numInstrs);
      System.out.print(" instructions: ");
      System.out.print(commas(numDynamicPaths));
      System.out.print(" (");
      System.out.print(commas(totalDynamicPaths));
      System.out.print(" cumulative paths, ");
      System.out.print(commas(totalDynamicInstrs));
      System.out.println(" cumulative instrs)");
    }
    System.out.println("-------------------------");
  }

  public static void cleanup()
  {
    routinesToNotInstrument    = null;
    defFlowPaths               = null;
    pgpCfgMap                  = null;
    pgpAbstractInstrMap        = null;
    definiteFlow               = null;
    tepDefiniteFlow            = null;
    potentialFlow              = null;
    tepPotentialFlow           = null;
    definiteBranchFlowForEdges = null;
    definiteRawFlowForEdges    = null;
    actualBranchFlowForEdges   = null;
    cfgs                       = null;
    superBegin                 = null;
    superEnd                   = null;
    programFlow                = null;
    outputPath                 = null;
  }
}
