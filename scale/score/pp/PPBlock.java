package scale.score.pp;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.AbstractCollection;

import scale.common.*;
import scale.score.chords.Chord;
import scale.score.chords.LoopHeaderChord;
import scale.score.chords.LoopTailChord;

/**
 * Represents a basic block in a path profiling CFG (PPCfg).
 * <p>
 * $Id: PPBlock.java,v 1.14 2007-10-29 13:38:14 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class PPBlock extends Root
{
  private Chord    firstChord;    // The first chord in the basic block.
  private PPCfg    cfg;           // The Cfg that this basic block is in.
  private PPEdge[] incomingEdges; // The set of incoming CFG edges.
  private PPEdge[] outgoingEdges; // The set of outgoing CFG edges.
  private long     numPaths;      // The number of paths for a block. This is used for path profiling instrumentation.
  private long     low;           // Range low value.
  private long     high;          // Range high value.
  private int      rank;          // Used in the uptree algorithm.
  private boolean  numPathsSet;   // True if the number of paths has been set.

  /**
   * Constructor that sets the first and last chords in the basic block.
   * @param firstChord The first Chord (from Scribble) of a basic block.
   * @param cfg The CFG that this basic block is part of.
   */
  public PPBlock(Chord firstChord, PPCfg cfg)
  {
    this.firstChord  = firstChord;
    this.cfg         = cfg;
    this.numPaths    = 0;
    this.numPathsSet = false;
    this.low         = Long.MAX_VALUE;
    this.high        = Long.MIN_VALUE;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(PPB-");
    buf.append(getNodeID());
    buf.append(" np:");
    buf.append(numPaths);
    buf.append(" high:");
    buf.append(high);
    buf.append(" low:");
    buf.append(low);
    buf.append(' ');
    buf.append(firstChord);
    buf.append(')');
    return buf.toString();
  }

  public String getDisplayName()
  {
    return "PPBlock-" + getNodeID();
  }

  /**
   * A constructor used by subclass PPSupergraphBlock.
   */
  protected PPBlock()
  {
    // do nothing
  }

  /**
   * Return the first Chord of this block.
   */
  public Chord firstChord()
  {
    return firstChord;
  }

  /**
   * Return the last Chord of this block.
   */
  public Chord lastChord()
  {
    return firstChord.lastInBasicBlock();
  }

  /**
   * Return a list of the incoming edges of this block.
   */
  public PPEdge[] incoming()
  {
    if (incomingEdges == null)
      return null;
    return incomingEdges.clone();
  }

  /**
   * Return a list of the incoming edges of this block.
   */
  public PPEdge[] outgoing()
  {
    if (outgoingEdges == null)
      return null;
    return outgoingEdges.clone();
  }

  /**
   * Add the edge to the incoming edges.
   * @return true if the edge was already there
   */
  public boolean addInEdge(PPEdge edge)
  {
    if (incomingEdges == null) {
      incomingEdges = new PPEdge[1];
      incomingEdges[0] = edge;
      return false;
    }

    for (int i = 0; i < incomingEdges.length; i++)
      if (incomingEdges[i].equals(edge))
          return true;

    PPEdge[] ni = new PPEdge[incomingEdges.length + 1];
    System.arraycopy(incomingEdges, 0, ni, 0, incomingEdges.length);
    incomingEdges = ni;
    incomingEdges[incomingEdges.length - 1] = edge;
    return false;
  }

  /**
   * Add the edge to the outgoing edges.
   * @return true if the edge was already there
   */
  public boolean addOutEdge(PPEdge edge)
  {
    if (outgoingEdges == null) {
      outgoingEdges = new PPEdge[1];
      outgoingEdges[0] = edge;
      return false;
    }

    for (int i = 0; i < outgoingEdges.length; i++)
      if (outgoingEdges[i].equals(edge))
          return true;

    PPEdge[] ni = new PPEdge[outgoingEdges.length + 1];
    System.arraycopy(outgoingEdges, 0, ni, 0, outgoingEdges.length);
    outgoingEdges = ni;
    outgoingEdges[outgoingEdges.length - 1] = edge;
    return false;
  }

  /**
   * Remove the specified incoming edge.
   * @return true if the edge was an incoming edge
   */
  public boolean removeInEdge(PPEdge edge)
  {
    if (incomingEdges == null)
      return false;

    for (int i = 0; i < incomingEdges.length; i++)
      if (incomingEdges[i].equals(edge)) {
        if ((i == 0) && (incomingEdges.length == 1)) {
          incomingEdges = null;
          return true;
        }
        PPEdge[] ni = new PPEdge[incomingEdges.length - 1];

        if (i > 0)
          System.arraycopy(incomingEdges, 0, ni, 0, i);
        if (i < (incomingEdges.length - 1))
          System.arraycopy(incomingEdges, i + 1, ni, i, incomingEdges.length - i - 1);
        incomingEdges = ni;
        return true;
      }

    return false;    
  }

  /**
   * Remove the specified outgoing edge.
   * @return true if the edge was an outgoing edge
   */
  public boolean removeOutEdge(PPEdge edge)
  {
    if (outgoingEdges == null)
      return false;

    for (int i = 0; i < outgoingEdges.length; i++)
      if (outgoingEdges[i].equals(edge)) {
        if ((i == 0) && (outgoingEdges.length == 1)) {
          outgoingEdges = null;
          return true;
        }
        PPEdge[] ni = new PPEdge[outgoingEdges.length - 1];
        if (i > 0)
          System.arraycopy(outgoingEdges, 0, ni, 0, i);
        if (i < (outgoingEdges.length - 1))
          System.arraycopy(outgoingEdges, i + 1, ni, i, outgoingEdges.length - i - 1);
        outgoingEdges = ni;
        return true;
      }

    return false;    
  }

  /**
   * Add all incoming edges to the specified collection.
   */
  public void addAllInEdges(AbstractCollection<PPEdge> list)
  {
    if (incomingEdges == null)
      return;

    for (int i = 0; i < incomingEdges.length; i++) {
      PPEdge edge = incomingEdges[i];
      list.add(edge);
    }
  }

  /**
   * Add all outgoing edges to the specified collection.
   */
  public void addAllOutEdges(AbstractCollection<PPEdge> list)
  {
    if (outgoingEdges == null)
      return;

    for (int i = 0; i < outgoingEdges.length; i++) {
      PPEdge edge = outgoingEdges[i];
      list.add(edge);
    }
  }

  /**
   * Return the number of incoming edges.
   */
  public int numInEdges()
  {
    if (incomingEdges == null)
      return 0;

    return incomingEdges.length;
  }

  /**
   * Return the specified incoming edge.
   */
  public PPEdge getInEdge(int i)
  {
    return incomingEdges[i];
  }

  /**
   * Return the specified outgoing edge.
   */
  public PPEdge getOutEdge(int i)
  {
    return outgoingEdges[i];
  }

  /**
   * Return the number of incoming edges.
   */
  public int numOutEdges()
  {
    if (outgoingEdges == null)
      return 0;

    return outgoingEdges.length;
  }

  /**
   * Return true if there are any incoming edges.
   */
  public boolean hasInEdges()
  {
    if (incomingEdges != null)
      return true;

    return false;
  }

  /**
   * Return true if there are any outgoing edges.
   */
  public boolean hasOutEdges()
  {
    if (outgoingEdges != null)
      return true;

    return false;
  }

  /**
   * Return true if there are any edges.
   */
  public final boolean hasEdges()
  {
    return hasInEdges() || hasOutEdges();
  }

  /**
   * Return the incoming edge frequency.
   */
  public long getInEdgeFrequency()
  {
    if (incomingEdges == null)
      return 0;

    long freq = 0;
    for (int i = 0; i < incomingEdges.length; i++) {
      PPEdge edge = incomingEdges[i];
      freq += edge.getFrequency();
    }

    return freq;
  }

  /**
   * Return the outgoing edge frequency.
   */
  public long getOutEdgeFrequency()
  {
    if (outgoingEdges == null)
      return 0;

    long freq = 0;
    for (int i = 0; i < outgoingEdges.length; i++) {
      PPEdge edge = outgoingEdges[i];
      freq += edge.getFrequency();
    }

    return freq;
  }

  /**
   * Return the outgoing branch edge frequency.
   */
  public long getBranchEdgeFrequency()
  {
    int  num  = 0;
    long freq = 0;

    if (outgoingEdges != null) {
      for (int i = 0; i < outgoingEdges.length; i++) {
        PPEdge edge = outgoingEdges[i];
        if (edge.isBranchEdge()) {
          freq += edge.getFrequency();
          num++;
        }
      }
    }

    assert (num >= 2) : "Expected more branch edges";

    return freq;
  }

  /**
   * Return the outgoing edge that has the highest value that is less
   * than or equal to the current value of pathNum..
   */
  public PPEdge getHighestOutEdge(long pathNum)
  {
    if (outgoingEdges == null)
      return null;

    PPEdge high = null;
    long   max  = Integer.MIN_VALUE;
    for (int i = 0; i < outgoingEdges.length; i++) {
      PPEdge edge   = outgoingEdges[i];
      long   curInc = edge.getIncrement();
      if ((curInc <= pathNum) && (curInc > max)) {
        high = edge;
        max = curInc;
      }
    }

    return high;
  }

  public void dumpEdges()
  {
    if (outgoingEdges == null)
      return;

    System.out.println("** DE   " + this);
    for (int i = 0; i < outgoingEdges.length; i++) {
      PPEdge edge   = outgoingEdges[i];
      long   curInc = edge.getIncrement();
      System.out.println("   " + curInc + " " + edge);
    }
  }

  /**
   * Add the specified increment to all of the incoming edges.
   */
  public void incrementInEdges(long inc)
  {
    if (incomingEdges == null)
      return;

    for (int i = 0; i < incomingEdges.length; i++) {
      PPEdge edge = incomingEdges[i];
      edge.addToIncrement(inc);
    }
  }

  /**
   * Get the average trip count for a loop.
   */
  public double getAvgTripCount()
  {
    assert isLoopHeader() : "Expected loop header block";

    // Find the back edge.

    if (incomingEdges == null)
      return 0.0;

    PPEdge backEdge = null;
    for (int i = 0; i < incomingEdges.length; i++) {
      PPEdge edge = incomingEdges[i];
      if (edge.source().lastChord().isLoopTail()) {
        backEdge = edge;
        break;
      }
    }

    // Divide the back edge freq by the sum of the frequencies of the
    // other incoming edges.


    double backEdgeFreq  = (double) backEdge.getFrequency();
    double otherEdgeFreq = ((double) getInEdgeFrequency()) - backEdgeFreq;

    return backEdgeFreq / otherEdgeFreq;
  }

  /**
   * Check the block edges and add them to the specified sets.
   */
  public void validate(PPBlock         block,
                       HashSet<PPEdge> incomingEdgesEncountered,
                       HashSet<PPEdge> outgoingEdgesEncountered)
  {
    if (incomingEdges != null) {
      for (int i = 0; i < incomingEdges.length; i++) {
        PPEdge incomingEdge = incomingEdges[i];
        if (!incomingEdge.target().equals(block))
          throw new scale.common.InternalError("The incoming edge's target is the wrong block");

        if (!incomingEdgesEncountered.add(incomingEdge))
          throw new scale.common.InternalError("Duplicate edge.");

        if (incomingEdge.source().equals(incomingEdge.target()))
          throw new scale.common.InternalError("Unexpected self loop");
      }
    }

    if (outgoingEdges != null) {
      for (int i = 0; i < outgoingEdges.length; i++) {
        PPEdge outgoingEdge = outgoingEdges[i];
        if (!outgoingEdge.source().equals(block))
          throw new scale.common.InternalError("The outgoing edge's source is the wrong block");

        if (!outgoingEdgesEncountered.add(outgoingEdge))
          throw new scale.common.InternalError("Duplicate edge");

        if (outgoingEdge.source().equals(outgoingEdge.target()))
          throw new scale.common.InternalError("Unexpected self loop");
      }
    }
  }

  /**
   * Return the number of paths for this block.
   * This is used for path profiling instrumentation.
   */
  public long getNumPaths()
  {
    return numPaths;
  }

  /**
   * Set the number of paths for this block.
   * This is used for path profiling instrumentation.
   * @param numPaths The number of paths for this block.
   */
  public void setNumPaths(long numPaths)
  {
    this.numPaths    = numPaths;
    this.numPathsSet = true;
  }

  /**
   * Reset the number of paths for this block.
   * This is used for path profiling instrumentation.
   */
  public void resetNumPaths()
  {
    this.numPaths    = 0;
    this.numPathsSet = false;
  }

  /**
   * Return true if and only the number of paths for this block has
   * been set.  This is used for path profiling instrumentation.
   */
  public boolean isNumPathsSet()
  {
    return numPathsSet;
  }

  /**
   * Return true if and only if this basic block is BEGIN.
   */
  public boolean isBeginBlock()
  {
    return equals(cfg.beginBlock());
  }

  /**
   * Return true if and only if this basic block is END.
   */
  public boolean isEndBlock()
  {
    return equals(cfg.endBlock());
  }

  /**
   * Return the path profiling CFG representation that this block is
   * part of.
   */
  public PPCfg getCfg()
  {
    return cfg;
  }

  public int getNumChords()
  {
    Chord temp = firstChord;
    Chord last = lastChord();

    int numChords = 1;
    while (!temp.equals(last)) {
      temp = temp.getNextChord();
      numChords++;
    }

    return numChords;
  }
  
  /**
   * Return true if the basic block is a loop header.
   */
  public boolean isLoopHeader()
  {
    return firstChord.isLoopHeader();
  }
  
  /**
   * Two blocks are the same if they have the same first chord.
   */
  public boolean equals(Object o)
  {
    if (!(o instanceof PPBlock))
      return false;

    PPBlock b = (PPBlock) o;
    return firstChord == b.firstChord;
  }

  /**
   * Return the hash code for this block which is just the first
   * Chord's hash code.
   */
  public int hashCode()
  {
    return firstChord.hashCode();
  }

  /**
   * The computed weight of the block.  The weight is computed from
   * edge weights in weightMap.
   * @return The computed weight of the block.
   */
  public double getWeight()
  {
    if (incomingEdges == null)
      return 1.0;

    double blockWeight = 0.0;
    for (int i = 0; i < incomingEdges.length; i++) {
      PPEdge edge = incomingEdges[i];
      if (!edge.isBackEdge()) {
        double edgeWeight = edge.getWeight();
        blockWeight += edgeWeight;
      }
    }

    return blockWeight;
  }

  public final long getLowRange()
  {
    return low;
  }

  public final void setLowRange(long low)
  {
    this.low = low;
  }

  public final long getHighRange()
  {
    return high;
  }
    
  public final void setHighRange(long high)
  {
    this.high = high;
  }
    
  public final void unionRange(long low, long high)
  {
    this.low = Math.min(low, this.low);
    this.high = Math.max(high, this.high);
  }

  public final String getRangeText()
  {
    if ((low == Long.MAX_VALUE) && (high == Long.MIN_VALUE))
      return "";

    StringBuffer buf = new StringBuffer("[");
    buf.append(low);
    buf.append(", ");
    buf.append(high);
    buf.append(']');
    return buf.toString();
  }

  public final int getRank()
  {
    return rank;
  }
    
  public final void setRank(int rank)
  {
    this.rank = rank;
  }

  public final void incRank()
  {
    rank++;
  }
}

