package scale.score.pp;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.AbstractCollection;

import scale.common.*;
import scale.score.chords.Chord;

/**
 * Represents the fake supergraph basic blocks SUPERBEGIN and
 * SUPEREND, which are connected to the BEGIN and END, respectively,
 * of each CFG.  This class should not be needed for developing
 * path-guided optimizations.
 * <p>
 * $Id: PPSupergraphBlock.java,v 1.5 2007-10-04 19:53:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class PPSupergraphBlock extends PPBlock
{
  /**
   * Represents that this supergraph block is SUPERBEGIN.
   */
  public static final int SUPERBEGIN = 1;

  /**
   * Represents that this supergraph block is SUPEREND.
   */
  public static final int SUPEREND = 2;

  private int  type; // The type of supergraph block -- either superbegin or end block.
  private List<PPCfg> cfgs; // A list of the CFGs in the program.

  public PPSupergraphBlock(int type, List<PPCfg> cfgs)
  {
    assert (type == SUPERBEGIN) || (type == SUPEREND) : "Unexpected supergraph block type.";

    this.type = type;
    this.cfgs = cfgs;
  }

  /**
   * Get the first Chord of this block.
   */
  public Chord firstChord()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Get the last Chord of this block.
   */
  public Chord lastChord()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return a list of the incoming edges of this block.
   */
  public PPEdge[] incoming()
  {
    if (type == SUPEREND) {
      PPEdge[] list = new PPEdge[cfgs.size()];
      Iterator<PPCfg> iter = cfgs.iterator();
      int      k    = 0;
      while (iter.hasNext()) {
        PPCfg cfg = iter.next();
        list[k++] = cfg.getEdge(cfg.endBlock(), this, PPEdge.DUMMY_FOR_BACK_EDGE);
      }
      return list;
    }

    return null;
  }

  /**
   * Return a list of the incoming edges of this block.
   */
  public PPEdge[] outgoing()
  {
    if (type == SUPERBEGIN) {
      PPEdge[] list = new PPEdge[cfgs.size()];
      Iterator<PPCfg> iter = cfgs.iterator();
      int      k    = 0;
      while (iter.hasNext()) {
        PPCfg cfg = iter.next();
        list[k++] = cfg.getEdge(this, cfg.beginBlock(), PPEdge.DUMMY_FOR_BACK_EDGE);
      }
      return list;
    }

    return null;
  }

  /**
   * Return the specified incoming edge.
   */
  public PPEdge getInEdge(int i)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return the specified outgoing edge.
   */
  public PPEdge getOutEdge(int i)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Add the edge to the incoming edges.
   * @return true if the edge was already there
   */
  public boolean addInEdge(PPEdge edge)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Add the edge to the outgoing edges.
   * @return true if the edge was already there
   */
  public boolean addOutEdge(PPEdge edge)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Add all incoming edges to the specified collection.
   */
  public void addAllInEdges(AbstractCollection<PPEdge> list)
  {
    if (type == SUPEREND) {
      Iterator<PPCfg> iter = cfgs.iterator();
      int      k    = 0;
      while (iter.hasNext()) {
        PPCfg cfg = iter.next();
        list.add(cfg.getEdge(cfg.endBlock(), this, PPEdge.DUMMY_FOR_BACK_EDGE));
      }
    }

  }

  /**
   * Add all outgoing edges to the specified collection.
   */
  public void addAllOutEdges(AbstractCollection<PPEdge> list)
  {
    if (type == SUPERBEGIN) {
      Iterator<PPCfg> iter = cfgs.iterator();
      int      k    = 0;
      while (iter.hasNext()) {
        PPCfg cfg = iter.next();
        list.add(cfg.getEdge(this, cfg.beginBlock(), PPEdge.DUMMY_FOR_BACK_EDGE));
      }
    }
  }

  /**
   * Remove the specified incoming edge.
   * @return true if the edge was an incoming edge
   */
  public boolean removeInEdge(PPEdge edge)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Remove the specified outgoing edge.
   * @return true if the edge was an outgoing edge
   */
  public boolean removeOutEdge(PPEdge edge)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return the number of incoming edges.
   */
  public int numInEdges()
  {
    if (type == SUPEREND)
      return cfgs.size();

    return 0;
  }

  /**
   * Return the number of incoming edges.
   */
  public int numOutEdges()
  {
    if (type == SUPERBEGIN)
      return cfgs.size();

    return 0;
  }

  /**
   * Return true if there are any incoming edges.
   */
  public boolean hasInEdges()
  {
    return numInEdges() > 0;
  }

  /**
   * Return true if there are any outgoing edges.
   */
  public boolean hasOutEdges()
  {
    return numOutEdges() > 0;
  }

  /**
   * Return the incoming edge frequency.
   */
  public long getInEdgeFrequency()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return the outgoing edge frequency.
   */
  public long getOutEdgeFrequency()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return the outgoing branch edge frequency.
   */
  public long getBranchEdgeFrequency()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return the outgoing edge with the highest increment.
   */
  public PPEdge getHighestOutEdge(long pathNum)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Add the specified increment to all of the incoming edges.
   */
  public void incrementInEdges(long inc)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Get the average trip count for a loop.
   */
  public double getAvgTripCount()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return the number of paths for this block.
   * This is used for path profiling instrumentation.
   */
  public long getNumPaths()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Set the number of paths for this block.
   * This is used for path profiling instrumentation.
   * @param numPaths The number of paths for this block.
   */
  public void setNumPaths(long numPaths)
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return True if and only the number of paths for this block has been set.
   * This is used for path profiling instrumentation.
   */
  public boolean isNumPathsSet()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Returns true if and only if this basic block is BEGIN.
   */
  public boolean isBeginBlock()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Returns true if and only if this basic block is END.
   */
  public boolean isEndBlock()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Return the path profiling CFG representation that this block is part of.
   */
  public PPCfg getCfg()
  {
    throw new scale.common.InternalError("Unsupported for PPSupergraphBlock instances.");
  }

  /**
   * Two supergraph blocks are the same if they have the same type.
   */
  public boolean equals(Object o)
  {
    if (!(o instanceof PPSupergraphBlock))
      return false;

    PPSupergraphBlock supergraphBlock = (PPSupergraphBlock)o;
    return type == supergraphBlock.type;
  }

  /**
   * The hash code for a supergraph block is based on its type.
   */
  public int hashCode()
  {
    return type;
  }
}

