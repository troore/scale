package scale.score.chords;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.score.Note;
import scale.score.expr.Expr;
import scale.score.Predicate;

/** 
 * This class represents a if-then-else statement node in a Scribble CFG.  
 * <p>
 * $Id: IfThenElseChord.java,v 1.41 2007-10-04 19:58:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class IfThenElseChord extends DecisionChord
{
  /**
   * The true CFG edge.
   */
  private Chord trueEdge;
  /**
   * The false CFG edge.
   */
  private Chord falseEdge;
  /**
   * The probability that execution of the program will take the true
   * out-going edge of the CFG is
   * <pre>
   *    branchPrediction / 32766.0
   * </pre>
   * The probability that execution of the program will take the false
   * out-going edge of the CFG is
   * <pre>
   *    1.0 - (branchPrediction / 32766.0)
   * </pre>
   */
  private short branchPrediction;
  /**
   * Marker for the true edge.
   */
  private boolean trueEdgeMarker;
  /**
   * Marker for the false edge.
   */
  private boolean falseEdgeMarker;

  /**
   * @param predicate is the test value
   * @param trueEdge is the next Chord if the predicate evaluates to true
   * @param falseEdge is the next Chord if the predicate evaluates to false
   */
  public IfThenElseChord(Expr predicate, Chord trueEdge, Chord falseEdge)
  {
    super(predicate);
    setTrueEdge(trueEdge);
    setFalseEdge(falseEdge);
    branchPrediction = 32766 >> 1; // Assume equal probability.
  }

  /**
   * @param predicate is the test value
   */
  public IfThenElseChord(Expr predicate)
  {
    this(predicate, null, null);
  }

  public Chord copy()
  {
    Chord ifc = new IfThenElseChord(getPredicateExpr().copy(), trueEdge, falseEdge);
    ifc.copySourceLine(this);
    return ifc;
  }

  /**
   * Return the edge that is followed if the predicate evaluates to
   * true.
   */
  public Chord getTrueCfgEdge()
  { 
    return trueEdge; 
  }

  /**
   * Return the edge that is followed if the predicate evaluates to
   * false.
   */
  public Chord getFalseCfgEdge()
  { 
    return falseEdge; 
  }

  /**
   * Set both out-going CFG edges to <code>null</code>.
   * This method does not maintain the validity of the CFG as the
   * nodes at the other ends of the edges are not modified.
   */
  public void deleteOutCfgEdges()
  {
    trueEdge = null;
    falseEdge = null;
  }

  /**
   * Specify the probability that the specified edge will be executed
   * next.  The stored value for the probability is quantized for
   * reasons of space utilization.  The probability of both edges is
   * set.  The sum of the probabilities of both edges is always
   * (approximately) 1.0.
   */
  public final void specifyBranchProbability(Chord edge, double probability)
  {
    assert (edge == trueEdge) || (edge == falseEdge) :
      "Not an out-going edge " + edge;
    assert ((probability <= 1.0) && (probability >= 0.0)) :
      "Invalid probability " + probability;

    short prob = (short) (probability * 32766.0);
    branchPrediction = (edge == trueEdge) ? prob : (short) (32766 - prob);
  }

  /**
   * Return the probability that the specified edge will be executed
   * next.  The stored value for the probability is quantized for
   * reasons of space utilization.  The sum of the probabilities of
   * both edges is always (approximately) 1.0.
   */
  public final double getBranchProbability(Chord edge)
  {
    assert (edge == trueEdge) || (edge == falseEdge) :
      "Not an out-going edge " + edge;

    double prob = ((double) branchPrediction) / 32766.0;
    return (edge == trueEdge) ? prob : 1.0 - prob;
  }

  public void visit(Predicate p)
  {
    p.visitIfThenElseChord(this);
  }

  /**
   * Specify the true out-going CFG edge.
   */
  public void setTrueEdge(Chord trueEdge)
  {
    if (this.trueEdge != null)
      this.trueEdge.deleteInCfgEdge(this);
    this.trueEdge = trueEdge;
    if (trueEdge != null) {
      trueEdge.addInCfgEdge(this);
      trueEdgeMarker = true;
    } else {
      trueEdgeMarker = false;
    }
  }

  /**
   * Specify the false out-going CFG edge.
   */
  public void setFalseEdge(Chord falseEdge)
  {
    if (this.falseEdge != null)
      this.falseEdge.deleteInCfgEdge(this);
    this.falseEdge = falseEdge;
    if (falseEdge != null) {
      falseEdge.addInCfgEdge(this);
      falseEdgeMarker = true;
    } else {
      falseEdgeMarker = false;
    }
  }

  /**
   * Add the successors of this Chord to the stack.
   */
  public final void pushAllOutCfgEdges(Stack<Chord> wl)
  {
    if (trueEdge != null)
      wl.push(trueEdge);

    if (falseEdge != null)
      wl.push(falseEdge);
  }

  /**
   * Add the successors of this Chord to the stack if they haven't
   * been visited before.
   * @see scale.score.chords.Chord#nextVisit
   * @see scale.score.chords.Chord#setVisited
   * @see scale.score.chords.Chord#visited
   */
  public final void pushOutCfgEdges(Stack<Chord> wl)
  {
    if ((trueEdge != null) && !trueEdge.visited()) {
      wl.push(trueEdge);
      trueEdge.setVisited();
    }

    if ((falseEdge != null) && !falseEdge.visited()) {
      wl.push(falseEdge);
      falseEdge.setVisited();
     }
  }

  /**
   * Add the successors of this Chord to the stack if they haven't
   * been visited before.
   * @param done is the set of visited CFG nodes
   */
  public final void pushOutCfgEdges(Stack<Chord> wl, HashSet<Chord> done)
  {
    if ((trueEdge != null) && done.add(trueEdge))
      wl.push(trueEdge);

    if ((falseEdge != null) && done.add(falseEdge))
      wl.push(falseEdge);
  }
  
  /**
   * Add the successors of this Chord to the stack if they haven't
   * been visited, and all their parents have.  Traversing the graph
   * in this fashion yields a topological sort of the graph (with loop
   * back edges removed).
   */
  public final void pushSortedOutCfgEdges(Stack<Chord> wl)
  {
    setVisited();
    
    if ((trueEdge != null) &&
        !trueEdge.visited() &&
        trueEdge.parentsVisited())
      wl.push(trueEdge);
    
    if ((falseEdge != null) &&
        !falseEdge.visited() &&
        falseEdge.parentsVisited())
      wl.push(falseEdge);
  }
  
  /**
   * Add the successors of this Chord to the stack if they haven't
   * been visited, and all their parents have.  Traversing the graph
   * in this fashion yields a topological sort of the graph (with loop
   * back edges removed).
   * @param finished is the set of finished nodes.
   */
  public void pushSortedOutCfgEdges(Stack<Chord> wl, HashSet<Chord> finished)
  {
    if ((trueEdge != null) &&
        !trueEdge.visited() &&
        trueEdge.parentsFinished(finished)) {
      wl.push(trueEdge);
      trueEdge.setVisited();
    }
    
    if ((falseEdge != null) &&
        !falseEdge.visited() &&
        falseEdge.parentsFinished(finished)) {
      wl.push(falseEdge);
      falseEdge.setVisited();
    }
  }

  /**
   * Clear all the markers.  Each marker is associated with one
   * out-going CFG edge.
   */
  public void clearEdgeMarkers()
  {
    trueEdgeMarker  = false;
    falseEdgeMarker = false;
  }

  /**
   * Return the marker associated with the specified out-going CFG
   * edge.
   * @param edge specifies the edge associated with the marker
   * @return true is the specified edge is marked
   * @see #getOutCfgEdge
   */
  public boolean edgeMarked(int edge)
  {
    if (edge == 0)
      return trueEdgeMarker;
   
    assert (edge == 1) : "No such edge (" + edge + ") " + this;

    return falseEdgeMarker;
  }

  /**
   * Set the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   * @see #getOutCfgEdge
   */
  public void markEdge(int edge)
  {
    if (edge == 0) {
      trueEdgeMarker = true;
      return;
    }

    assert (edge == 1) : "No such edge (" + edge + ") " + this;

    falseEdgeMarker = true;
  }

  /**
   * Clear the marker associated with the specified out-going CFG
   * edge.
   * @param edge specifies the edge associated with the marker
   * @see #getOutCfgEdge
   */
  public void clearEdge(int edge)
  {
    if (edge == 0) {
      trueEdgeMarker = false;
      return;
    }

    assert (edge == 1) : "No such edge (" + edge + ") " + this;

    falseEdgeMarker = false;
  }

  /**
   * Return the number of out-going CFG edges.
   */
  public final int numOutCfgEdges()
  {
    return 2;
  }

  /**
   * Return the specified out-going CFG edge.
   */
  public final Chord getOutCfgEdge(int i)
  {
    if (i == 0)
      return trueEdge;

    assert (i == 1) : "No such edge (" + i + ") " + this;

    return falseEdge;
  }

  /**
   * Use this method when you may be modifying an out-going CFG edge
   * from this Chord while iterating over the out-going edges.
   * @return an array of all of the out-going CFG edges.
   */
  public final Chord[] getOutCfgEdgeArray()
  {
    Chord[] array = new Chord[2];

    array[0] = trueEdge;
    array[1] = falseEdge;
    return array;
  }

  /**
   * This routine is needed because it is possible for more than one
   * out-going edge from a CFG node to go the the same CFG node.  This
   * method should be over-ridden by the derived classes for speed.
   * @param to is the target CFG node
   * @param skip ispecifies how many occurrances of <code>to</code> to
   * skip
   * @return the index into the outgoing edges of this node that matches
   * the specified in-coming edge of the target node.
   */
  public final int indexOfOutCfgEdge(Chord to, int skip)
  {
    if (to == trueEdge) {
      if (skip == 0)
        return 0;
      if ((to == falseEdge) && (skip == 1))
        return 1;
    } else if ((to == falseEdge) && (skip == 0))
      return 1;

    throw new scale.common.InternalError("Edge (" + skip + ") not found " + to);
  }

  /**
   * Replace the existing out-going CFG edge with a new edge.  No
   * attempt is made to maintain the validity of the CFG graph; that
   * is the responsibility of the caller.
   * @param oldChord is the old edge
   * @param newChord is the new edge
   */
  public void replaceOutCfgEdge(Chord oldChord, Chord newChord)
  {
    if (oldChord == trueEdge)
      trueEdge = newChord;
    else if (oldChord == falseEdge)
      falseEdge = newChord;
    else
      throw new scale.common.InvalidMutationError(oldChord +
                                                  " is not an out-going CFG edge of " +
                                                  this);
  }

  /**
   * Change the out-going CFG edge indicated by the position to the
   * new edge.  The validity of the CFG graph is maintained.
   * @param oldEdge the out-going CFG edge to be changed
   * @param newEdge the new out-going CFG edge
   */
  public void changeOutCfgEdge(Chord oldEdge, Chord newEdge)
  {
    if (oldEdge == trueEdge)
      setTrueEdge(newEdge);
    else if (oldEdge == falseEdge)
      setFalseEdge(newEdge);
    else
      throw new scale.common.InvalidMutationError("Not an existing out-edge " + oldEdge);
  }

  /**
   * Link a new CFG node that contains old links.
   * When a CFG node is copied, the copy has the same links as
   * the original node.  This method updates those links.
   * @param nm is a map from the old nodes to the new nodes.
   * @see scale.score.Scribble#linkSubgraph
   */
  public void linkSubgraph(HashMap<Chord, Chord> nm)
  {
    Chord nt = nm.get(trueEdge);
    if (nt != null) 
      setTrueEdge(nt);
    Chord ft = nm.get(falseEdge);
    if (ft != null)
      setFalseEdge(ft);
  }

  /**
   * Return the index of the selected out-going CFG edge.
   * @param key specifies the out-going CFG edge
   */
  public int getBranchEdgeIndex(Object key)
  {
    assert (key instanceof Literal) : "Invalid key " + key;
    try {
      boolean t = Lattice.convertToBooleanValue((Literal) key);
      return t ? 0 : 1;
    } catch(InvalidException ex) {
      throw new scale.common.InternalError("Invalid key " + key);
    }
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5 + getPredicateExpr().executionCostEstimate();
  }
}
