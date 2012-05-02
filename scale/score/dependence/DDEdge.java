package scale.score.dependence;

import java.util.Iterator;

import scale.score.*;
import scale.score.expr.Expr;
import scale.score.chords.Chord;
import scale.score.chords.LoopHeaderChord;
import scale.score.expr.SubscriptExpr;
import scale.common.*;

/**
 * This class is the base class for data dependence edges.
 * <p>
 * $Id: DDEdge.java,v 1.35 2007-10-04 19:58:24 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>, <br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>, <br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An edge in the data dependence graph goes from a source node to a
 * sink node where a node is some {@link
 * scale.score.expr.SubscriptExpr SubscriptExpr} instance expression
 * in the CFG.  The edge type may be for flow dependence,
 * antidependence, input dependence, output dependence, or spatial
 * dependence.
 * <p>
 * Note that while edges connect {@link scale.score.expr.SubscriptExpr
 * SubscriptExpr} instances, the actual edges are between the uses of
 * the address specified by the {@link scale.score.expr.SubscriptExpr
 * SubscriptExpr} instance.  It is necessary to have the use at each
 * end in order to determine the type of data dependence edge.

 * <p>
 * There are two types of edges recorded.  A normal edge connects
 * between two nodes.  A transitive edge is used to reduce the amount
 * of memory used by the compiler to represent the data dependence
 * edges.  A transitive edge can represent many actual edges.  For
 * example, if there is an edge from a to b and an edge from b to c,
 * then there is also an edge from a to c if all the edges have zero
 * distance.
 * <p>
 * @see DDTransEdge
 * @see DDNormalEdge
 * @see DataDependence
 * @see DDGraph
 * @see DDInfo
 */
public abstract class DDEdge
{
  /**
   * The colors used to graph the different edge types.
   */
  public static final DColor[] colors = {
    DColor.CYAN,
    DColor.RED,
    DColor.GREEN,
    DColor.BLUE,
    DColor.PURPLE,
    DColor.BLACK
  }; // Colors for edges.

  /**
   * The line types used to graph the different edge types.
   */
  public static final DEdge[] lineType = {
    DEdge.SOLID,
    DEdge.SOLID,
    DEdge.DOTTED,
    DEdge.DASHED,
    DEdge.THICK,
    DEdge.THICK,
  }; // Line types for edges.

  /**
   * A flow, or true, dependence.  A value defined in S1 is used in S2.
   */
  public static final int cFlow = 0;
  /**
   * An anti dependence.  A value used in S1 is defined in S2.
   */
  public static final int cAnti = 1;
  /**
   * An output dependence. A value defined in S1 is defined in S2
   */
  public static final int cOutput = 2;
  /**
   *  An input dependence. A value used both in S1 and in S2
   */
  public static final int cInput = 3;
  /**
   *  No dependence. There is a dependence from S2 to S1 but not S1 to S2.  
   */
  public static final int cNone = 4;
  /**
   * Map from dependence to string.
   */
  public static final String[] dependenceName = {"flow", "anti", "output", "input", "none"};

  private static int count = 0;

  private String  aname;   // Name of array (or scalar) involved in the dependence.
  private int     id;
  private boolean spatial; // True if this is a spatial edge. 

  /**
   * Create an edge for the data dependence graph.
   * @param aname is the name of array (or scalar) involved in the dependence
   * @param spatial is true if the edge records a spatial dependence
   * @see DataDependence
   * @see DDGraph
   * @see DDNormalEdge
   * @see DDTransEdge
   */
  public DDEdge(String aname, boolean spatial)
  {
    this.aname   = aname;
    this.spatial = spatial;
    this.id      = count++;
  }

  /**
   * Return a unique integer specifying this edge instance.
   */
  public final int getNodeID()
  {
    return id;
  }

  /**
   * Return an {@link java.util.Iterator iterator} over the {@link
   * scale.score.expr.SubscriptExpr SubscriptExpr} instances that are
   * the edge ends.
   */
  public abstract Iterator<SubscriptExpr> iterator();

  /**
   * Return true if the expression is an end of an edge represented by
   * this instance.
   */
  public abstract boolean contains(SubscriptExpr exp);

  /**
   * Return true if every edge represented is an input edge.
   */
  public abstract boolean representsAllInput();

  /**
   * Add the {@link scale.score.expr.SubscriptExpr SubscriptExpr} instances,
   * that are the edge ends, to the Vector.
   */
  public abstract void getEnds(Vector<Note> v);

  /**
   * Return a metric for the number of data dependence edges represented.
   * This is the number of edge end points - 1.
   */
  public abstract int numberEdges();

  /**
   * Return the name of array (or scalar) involved in the dependence.
   */
  public final String getArrayName()
  {
    return aname;
  }

  /**
   * Return the computed data dependence information.
   */
  public abstract long[] getDDInfo();

  /**
   * Return true if the edge is loop-independent dependency.
   */
  public abstract boolean isLoopIndependentDependency();

  /**
   * Return the distance for the specified level.
   */
  public abstract int getDistance(int level);

  /**
   * Return true if the distance is known at the specified level.
   */
  public abstract boolean isDistanceKnown(int level);

  /**
   * Return true if the distance is known at any level.
   */
  public abstract boolean isAnyDistanceKnown();

  /**
   * Return true if the distance is not known at any level.
   */
  public abstract boolean isAnyDistanceNotKnown();

  /**
   * Return true if any distance is unknown or not zero at any level.
   */
  public abstract boolean isAnyDistanceNonZero();

  /**
   * Return the data dependence type -  {@link DDEdge#cFlow flow},
   * {@link DDEdge#cAnti anti}, {@link DDEdge#cInput input}, or {@link DDEdge#cOutput output}.
   * This logic assumes that the <tt>source</tt> precedes the <tt>sink</tt>.
   * The <tt>source</tt> and <tt>sink</tt> must the uses of the addresses
   * represented by the {@link scale.score.expr.SubscriptExpr SubscriptExpr}
   * instances that are the ends of the data dependence edge.
   * @see DDGraph
   * @see DataDependence
   */
  public abstract int getEdgeType(Note source, Note sink);

  /**
   * Return true if this is a spatial edge.
   */
  public final boolean isSpatial()
  {
    return spatial;
  }

  /**
   * Return true if this is a transitive edge set.
   */
  public abstract boolean isTransitive();

  /**
   * Print to stdout the information about the data dependence.
   */
  public abstract void printDDInfo(Note source, Note sink);

  /**
   * Return a string representation of a data dependence edge.
   * @param s1 is one end of the edge
   * @param s2 is another end of the edge
   * @param aname is the array name
   * @param ddtype is the edge type
   */
  public abstract String format(Note s1, Note s2, String aname, int ddtype);

  /**
   * Create a graphic display of the edges represented by this instancce.
   * @param da is the graph display
   * @param addChord is true if the ends of each edge should be added
   * to the <code>nodes</code> set
   * @param nodes is the set of
   */
  public abstract void graphDependence(DisplayGraph  da,
                                       boolean       addChord,
                                       HashSet<Note> nodes,
                                       DDGraph       graph);

  /**
   * Return true if this edge has a source or sink in the specified loop.
   */
  public abstract boolean forLoop(LoopHeaderChord loop);
}
