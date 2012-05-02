package scale.score.chords;

import scale.common.Stack;

import scale.common.*;
import scale.score.Predicate;
import scale.score.expr.*;
import scale.clef.decl.Declaration;

/** 
 * This class is a base class for any node in the CFG which does not
 * alter control flow.
 * <p>
 * $Id: SequentialChord.java,v 1.47 2007-10-17 13:39:59 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * All superclasses of this class have only one out-going CFG edge.
 */

public abstract class SequentialChord extends Chord
{
  /**
   * This field holds the node's outbound cfg edge..
   */
  private Chord nextChord = null; 
  /**
   * Marker for the out-going CFG edge.
   */
  private boolean marker = true;

  /**
   * Construct a new Chord with the indicated out-going CFG edge.
   * @param next is the out-going CFG edge
   */
  public SequentialChord(Chord next)
  {
    super();
    nextChord = next;
    if (next != null)
      next.addInCfgEdge(this);
  }

  /**
   * Construct a new Chord with no out-going CFG edge.
   * @see #setTarget
   */
  public SequentialChord()
  {
    this(null);
  }

  /**
   * Return true if this is a sequential chord.
   */
  public final boolean isSequential()
  {
    return true;
  }

  /**
   * Set the out-going CFG edge to <code>null</code>.
   * This method does not maintain the validity of the CFG as the
   * node at the other ends of the edge is not modified.
   */
  public void deleteOutCfgEdges()
  {
    nextChord = null;
  }

  /**
   * Set the out-going CFG edge of this node (i.e., target of the branch).
   * The old out-going CFG edge, if any, is replaced.
   * The validity of the CFG graph is maintained.
   * @param target is the out-going CFG edge
   */
  public void setTarget(Chord target)
  {
    if (nextChord != null) // Handle old edge.
      nextChord.deleteInCfgEdge(this);

    if (target != null)
      target.addInCfgEdge(this);  // Make them point to us.

    nextChord = target;             // Make us point to them.
    marker = true;
  }

  /**
   * Set the out-going CFG edge of this node (i.e., target of the
   * branch).  The old out-going CFG edge, if any, is replaced.  The
   * validity of the CFG graph is NOT maintained.
   * @param target is the out-going CFG edge
   */
  public void setTargetUnsafe(Chord target)
  {
    if (nextChord != null) // Handle old edge.
      nextChord.deleteInCfgEdge(this);

    nextChord = target;             // Make us point to them.
    marker = true;
  }

  /**
   * Return the out-going CFG edge (i.e., target of this branch).
   */
  public Chord getTarget()
  {
    return nextChord;
  }

  /**
   * Link child to parent if it's a SequentialChord and not a
   * BranchChord or EndChord.
   */
  public void linkTo(Chord child)
  {
    setTarget(child);
  }

  /**
   * Replace the existing out-going CFG edge with a new edge.  No
   * attempt is made to maintain the validity of the CFG graph; that
   * is the responsibility of the caller.
   * @param oldChord is the old edge
   * @param newChord is the new edge
   */
  public final void replaceOutCfgEdge(Chord oldChord, Chord newChord)
  {
    assert (nextChord == oldChord)  : "Not an out-going CFG edge " + oldChord;

    nextChord = newChord;
  }

  /**
   * Return the CFG out-going edge.
   */
  public final Chord getNextChord()
  {
    return nextChord;
  }

  /**
   * Return true if this is the last Chord in this Basic Block.
   */
  public boolean isLastInBasicBlock()
  {
    return ((nextChord == null) || (nextChord.numInCfgEdges() > 1));
  }

  /**
   * Return the number of out-going CFG edges.
   */
  public final int numOutCfgEdges()
  {
    return (nextChord != null) ? 1 : 0;
  }

  /**
   * Return the specified out-going CFG edge.
   */
  public final Chord getOutCfgEdge(int i)
  {
    assert (i == 0) : "Not such edge - " + i;
    return nextChord;
  }

  /**
   * Use this method when you may be modifying an out-going CFG edge
   * from this Chord while iterating over the out-going edges.
   * @return an array of all of the out-going CFG edges
   */
  public final Chord[] getOutCfgEdgeArray()
  {
    int     l     = (nextChord == null) ? 0 : 1;
    Chord[] array = new Chord[l];
    if (l > 0)
      array[0] = nextChord;
    return array;
  }

  /**
   * This routine is needed because it is possible for more than one
   * out-going edge from a CFG node to go the the same CFG node.
   * This method should be over-ridden by the derived classes for speed.
   * @param to is the target CFG node
   * @param skip ispecifies how many occurrances of <code>to</code> to skip
   * @return the index into the outgoing edges of this node that matches
   * the specified in-coming edge of the target node.
   */
  public final int indexOfOutCfgEdge(Chord to, int skip)
  {
    assert ((to == nextChord) && (skip == 0)) :
      "Edge (" + skip + ") not found " + to;
    return 0;
  }

  /**
   * Add the successors of this Chord to the stack.
   */
  public final void pushAllOutCfgEdges(Stack<Chord> wl)
  {
    if (nextChord != null)
      wl.push(nextChord);
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
    if ((nextChord != null) && !nextChord.visited()) {
      wl.push(nextChord);
      nextChord.setVisited();
    }
  }

  /**
   * Add the successors of this Chord to the stack if they haven't
   * been visited before.
   * @param done is the set of visited CFG nodes
   */
  public final void pushOutCfgEdges(Stack<Chord> wl, HashSet<Chord> done)
  {
    if ((nextChord != null) && done.add(nextChord))
      wl.push(nextChord);
  }
  
  /**
   * Add the successors of this Chord to the stack if they haven't
   * been visited, and all their parents have.  Traversing the graph
   * in this fashion yields a topological sort of the graph (with loop
   * back edges removed).
   */
  public void pushSortedOutCfgEdges(Stack<Chord> wl)
  {
    setVisited();
    if ((nextChord != null) &&
        !nextChord.visited() &&
        nextChord.parentsVisited())
      wl.push(nextChord);
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
    if ((nextChord != null) &&
        !nextChord.visited() &&
        nextChord.parentsFinished(finished)) {
      wl.push(nextChord);
      nextChord.setVisited();
    }
  }

  /**
   * Clear all the markers.  Each marker is associated with one
   * out-going CFG edge..
   */
  public final void clearEdgeMarkers()
  {
     marker = false;
  }

  /**
   * Return the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   * @return true is the specified edge is marked
   */
  public final boolean edgeMarked(int edge)
  {
    assert (edge == 0) : "No such edge (" + edge + ") " + this;
    return marker;
  }

  /**
   * Set the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   */
  public final void markEdge(int edge)
  {
    assert (edge == 0) : "No such edge (" + edge + ") " + this;
    marker = true;
  }

  /**
   * Clear the marker associated with the specified out-going CFG
   * edge.
   * @param edge specifies the edge associated with the marker
   */
  public final void clearEdge(int edge)
  {
    assert (edge == 0) : "No such edge (" + edge + ") " + this;
    marker = false;
  }

  /**
   * Remove all the in-coming adat edges.
   */
  public void deleteInDataEdges()
  {
  }

  /**
   * Change the out-going CFG edge indicated by the position to the
   * new edge.  The validity of the CFG graph is maintained.
   * @param oldEdge the out-going CFG edge to be changed
   * @param newEdge the new out-going CFG edge
   */
  public void changeOutCfgEdge(Chord oldEdge, Chord newEdge)
  {
    if (oldEdge != nextChord)
      throw new scale.common.InvalidMutationError("Not an existing out-edge " + oldEdge);

    setTarget(newEdge);
  }

  /**
   * Return a vector of all declarations referenced in this CFG node
   * or <code>null</code>.
   */
  public Vector<Declaration> getDeclList()
  {
    return null;
  }

  /**
   * Return a vector of all {#link scale.score.expr.LoadExpr LoadExpr}
   * instances in this CFG node or <code>null</code>.
   */
  public Vector<LoadExpr> getLoadExprList()
  {
    return null;
  }

  /**
   * Return a vector of all {@link scale.score.expr.Expr Expr}
   * instances in this CFG node or <code>null</code>.
   */
  public Vector<Expr> getExprList()
  {
    return null;
  }

  /**
   * Replace all occurrances of a Declaration with another Declaration.
   * Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    return false;
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
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
    if (nextChord == null)
      return;

    Chord nt = nm.get(nextChord);
    if (nt != null)
      changeOutCfgEdge(nextChord, nt);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 0;
  }
}
