package scale.score.pp;

import java.util.Iterator;

import scale.common.*;
import scale.score.chords.*;

/**
 * Represents an edge in a path profiling CFG (PPCfg).
 * <p>
 * $Id: PPEdge.java,v 1.15 2007-10-29 13:38:15 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * TODO: Description of this class.
 */
public final class PPEdge extends Root implements java.lang.Comparable<PPEdge>
{
  /**
   * The type for a normal (non-dummy) edge.
   */
  public static final int NORMAL = 1;

  /**
   * The type for a dummy edge added because a back edge was removed.
   * This type is also used for dummy edges that are added for reasons
   * other than back edge removal and truncation, such as the END to
   * BEGIN edge added for the minimal instrumentation algorithm and
   * the SUPERBEGIN to BEGIN and END to SUPEREND edges added to make
   * the supergraph for flow analysis.
   */
  public static final int DUMMY_FOR_BACK_EDGE = 2;

  /**
   * The type for a dummy edge added because a non-back edge was truncated.
   */
  public static final int DUMMY_FOR_TRUNCATED_EDGE = 3;

  private PPBlock source;    // The source basic block of this edge.
  private PPBlock target;    // The target basic block of this edge.
  private PPCfg   cfg;       // The CFG that this edge is part of.
  private long    increment; // The edge increment value.
  private long    frequency; // The edge frequency.
  private double  weight;    // The edge weight;
  private int     type;      // The type of this edge.
  private boolean backEdge;  // The edge is a back edge of a loop.

  /**
   * A constructor that sets the source and target basic blocks of this edge,
   * as well as the CFG that this edge is in.
   * @param source is the source basic block of this edge.
   * @param target is the target basic block of this edge.
   * @param type is the type of this edge.  See the constants in this class.
   * @param cfg is the CFG that this edge is in.
   */
  public PPEdge(PPBlock source, PPBlock target, int type, PPCfg cfg)
  {
    this.source    = source;
    this.target    = target;
    this.cfg       = cfg;
    this.type      = type;
    this.increment = 0;
    this.frequency = 0;
    this.weight    = 0.0;

    if ((source instanceof PPSupergraphBlock) ||
        (target instanceof PPSupergraphBlock))
      backEdge = false;
    else
      backEdge = (source.lastChord().isLoopTail() &&
                  target.firstChord().isLoopHeader());
  }

  /**
   * Compare two edges by edge weight.
   */
  public int compareTo(PPEdge o2)
  {
    double w1 = weight;
    double w2 = o2.weight;

    if (w2 < w1)
      return -1;

    if (w2 == w1)
      return 0;

    return 1;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(PPE-");
    buf.append(getNodeID());
    buf.append(' ');
    buf.append((type == NORMAL) ? "normal" : "dummy");
    buf.append(" inc:");
    buf.append(increment);
    buf.append(" freq:");
    buf.append(frequency);
    buf.append(" weight:");
    buf.append(weight);
    buf.append(' ');
    buf.append(cfg);
    buf.append(' ');
    if (source != null) {
      buf.append("PPB-");
      buf.append(source.getNodeID()); //.getNodeID());
    }
    buf.append("->");
    if (target != null) {
      buf.append("PPB-");
      buf.append(target.getNodeID()); //.getNodeID());
    }
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return The source basic block of this edge.
   */
  public PPBlock source()
  {
    return source;
  }

  /**
   * Return The target basic block of this edge.
   */
  public PPBlock target()
  {
    return target;
  }

  /**
   * Return the type of this edge (see the constants in this class).
   */
  public int getType()
  {
    return type;
  }

  /**
   * Tells us whether this edge is a dummy edge.
   * @return True if and only if this edge is a dummy edge.
   */
  public boolean isDummy()
  {
    return (type == DUMMY_FOR_BACK_EDGE) ||
           (type == DUMMY_FOR_TRUNCATED_EDGE);
  }

  /**
   * Retun the increment value of this edge.
   * This is used for path profiling instrumentation.
   */
  public final long getIncrement()
  {
    return increment;
  }

  /**
   * Set the increment value of this edge.
   * This is used for path profiling instrumentation.
   * @param increment The new increment value for this edge.
   */
  public final void setIncrement(long increment)
  {
    this.increment = increment;
  }

  /**
   * Add the increment to the increment value of this edge.
   * This is used for path profiling instrumentation.
   * @param increment The new increment value for this edge.
   */
  public final void addToIncrement(long increment)
  {
    this.increment += increment;
  }

  /**
   * Retun the frequency value of this edge.
   * This is used for path profiling instrumentation.
   */
  public final long getFrequency()
  {
    return frequency;
  }

  /**
   * Set the frequency value of this edge.
   * This is used for path profiling instrumentation.
   * @param frequency The new frequency value for this edge.
   */
  public final void setFrequency(long frequency)
  {
    this.frequency = frequency;
  }

  /**
   * Add the frequency to the frequency value of this edge.
   * This is used for path profiling instrumentation.
   * @param frequency The new frequency value for this edge.
   */
  public final void addToFrequency(long frequency)
  {
    this.frequency += frequency;
  }

  /**
   * Return true if this path profiling CFG edge is editable.
   */
  public boolean isEditable()
  {
    return isEditable(source.lastChord(), target.firstChord());
  }

  /**
   * Return true if the Scribble CFG edge is editable.
   * All edges are editable except the following:
   * (1) edges from LoopPreHeaderChord to LoopHeaderChord;
   * (2) edges from LoopTailChord to LoopHeaderChord.
   * @param sourceChord The source of the edge.  This parameter may be null.
   * @param targetChord The target of the edge.
   */
  public static boolean isEditable(Chord sourceChord, Chord targetChord)
  { 
    // If the source chord is null, check all source chords.

    if (sourceChord == null) {
      Chord[] sourceChords = targetChord.getInCfgEdgeArray();
      for (int n = 0; n < sourceChords.length; n++) {
        if (!isEditable(sourceChords[n], targetChord))
          return false;
      }
      return true;
    }

    if ((sourceChord instanceof LoopPreHeaderChord) &&
        (targetChord instanceof LoopHeaderChord))
      return false;

    if (sourceChord.isLoopTail() && targetChord.isLoopHeader())
      return false;

    return true;
  }

  /**
   * Return true if the edge is a back edge.
   */
  public boolean isBackEdge()
  {
    return backEdge;
  }

  /**
   * Return true if and only if it is okay to truncate this edge.
   */
  public boolean mayTruncate()
  {
    // Can't truncate a dummy edge.

    if (isDummy())
      return false;

    // No point in truncating an edge connected to BEGIN or END.

    PPBlock begin = cfg.beginBlock();
    if (source.equals(begin))
      return false;

    PPBlock end = cfg.endBlock();
    if (target.equals(end))
      return false;

    // The next two conditions ensure we don't have a path
    //    BEGIN -> SOME_BLOCK -> END
    // where both edges are dummy edges.

    if (cfg.hasEdge(begin, source, DUMMY_FOR_BACK_EDGE))
      return false;
    if (cfg.hasEdge(begin, source, DUMMY_FOR_TRUNCATED_EDGE))
      return false;
    if (cfg.hasEdge(target, end, DUMMY_FOR_BACK_EDGE))
      return false;
    if (cfg.hasEdge(target, end, DUMMY_FOR_TRUNCATED_EDGE))
      return false;

    return true;
  }

  /**
   * Return the CFG that contains this edge.
   */
  public PPCfg getCfg()
  {
    return cfg;
  }

  /**
   * Compares two edges.  Two edges are equal if and only if their
   * sources and targets are the same AND they're types are the same.
   */
  public boolean equals(PPBlock source, PPBlock target, int type)
  {
    return (this.source.equals(source) &&
            this.target.equals(target) &&
            (this.type == type));
  }


  /**
   * Compares two edges.  Two edges are equal if and only if their
   * sources and targets are the same AND they're types are the same.
   */
  public boolean equals(Object o)
  {
    if (!(o instanceof PPEdge))
      return false;

    PPEdge edge = (PPEdge)o;
    return equals(edge.source, edge.target, edge.type);
  }

  /**
   * The hash code for a block is a combination of the source, target,
   * and type hash codes.
   */
  public static int hashCode(PPBlock source, PPBlock target, int type)
  {
    return (source.hashCode() % (Integer.MAX_VALUE / 3)) +
            (target.hashCode() % (Integer.MAX_VALUE / 3)) +
            (type % (Integer.MAX_VALUE / 3));
  }

  /**
   * The hash code for a block is a combination of the source, target,
   * and type hash codes.
   */
  public int hashCode()
  {
    return hashCode(source, target, type);
  }

  /**
   * Return the edge's weight
   */
  public double getWeight()
  {
    return weight;
  }

  /**
   * Return the edge's weight
   */
  public void setWeight(double weight)
  {
    this.weight = weight;
  }

  /**
   * Add the weight of one edge to another edge.
   * @param srcEdge The edge to get weight from.
   */
  public void addWeight(PPEdge srcEdge)
  {
    weight += srcEdge.weight;
  }

  /**
   * Return 1 if the edges are in the same direction and -1 otherwise.
   * e1 can be null.
   */
  public int dir(PPEdge e1)
  {
    if ((e1 == null) || e1.source.equals(target) || e1.target.equals(source))
      return 1;

    return -1;
  }

  public boolean isBranchEdge()
  {
    if ((source instanceof PPSupergraphBlock) ||
        (target instanceof PPSupergraphBlock))
      return false;

    // Special case: the edge's source is the CFG's BEGIN block.

    if (source.equals(cfg.beginBlock())) {
      if (isDummy())
        return false;

      // If there is another non-dummy edge coming out of begin, this
      // is a branch edge.

      PPBlock begin = cfg.beginBlock();
      int     l     = begin.numOutEdges();
      for (int i = 0; i < l; i++) {
        PPEdge outgoingEdge = begin.getOutEdge(i);
        if (!outgoingEdge.isDummy() && !outgoingEdge.equals(this))
          return true;
      }

      return false;
    }

    // See if there are any other edges (including dummy edges) coming
    // out of edge's source.

    return (source.outgoing().length > 1);
  }
}

