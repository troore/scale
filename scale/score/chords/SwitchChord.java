package scale.score.chords;

import scale.common.*;
import scale.clef.expr.IntLiteral;
import scale.score.expr.Expr;
import scale.score.Predicate;

/** 
 * This class represents a Scribble CFG node that has multiple
 * out-going CFG edges.
 * <p>
 * $Id: SwitchChord.java,v 1.39 2007-10-04 19:58:23 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This is the base class for all classes that have more than one out-going CFG edge.
 * The edges are selected using a key object.
 */

public class SwitchChord extends DecisionChord
{
  private static int createdCount = 0; // A count of all the created instances of this class.

  static
  {
    Statistics.register("scale.score.chords.SwitchChord", "created");
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * This class represents tuples of the form (key, target)
   * where target is an out-going CFG edge selecetd by the key.
   */
  private static final class OutEdge 
  {
    public Chord target; /* Where this out-going CFG edge goes. */
    public long  key;    /* Map from key to target */
    /**
     * The probability that execution of the program will take the true
     * out-going edge of the CFG is
     * <pre>
     *    branchPrediction / 32767.0
     * </pre>
     * The probability that execution of the program will take the false
     * out-going edge of the CFG is
     * <pre>
     *    1.0 - (branchPrediction / 32767.0)
     * </pre>
     */
    public short   branchPrediction;
    public boolean marked;     /* Is this edge marked? */
    public boolean defaultKey; /* True if this is the "default". */

    private OutEdge(long key, Chord target, boolean defaultKey)
    {
      this.key        = key;
      this.target     = target;
      this.marked     = true;
      this.defaultKey = defaultKey;
    }

    public OutEdge(Object keyo, Chord target)
    {
      if (keyo instanceof IntLiteral) {
        this.key        = ((IntLiteral) keyo).getLongValue();
        this.defaultKey = false;
      } else {
        this.key        = 0;
        this.defaultKey = true;
      }
      this.target = target;
      this.marked = true;
    }

    public OutEdge copy()
    {
      OutEdge n = new OutEdge(key, target, defaultKey);
      n.branchPrediction = branchPrediction;
      n.marked = true;
      return n;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(");
      if (defaultKey)
        buf.append("default");
      else
        buf.append(key);
      buf.append(": ");
      buf.append(target.toString());
      buf.append(") ");
      return buf.toString();
    }
  }

  /**
   * Out-going CFG edges.
   */
  private Vector<OutEdge> edges = null; 

  /**
   * Create a Chord that has more than one out-going CFG edge where
   * the edge is selected by some computation.
   * @param predicate the expression which is evaluated to select the
   * out-going edge
   * @param keys a vector of keys associated with the out-going edges
   * @param targets a vector of out-going CFG edges
   */
  public SwitchChord(Expr predicate, Vector<Object> keys, Vector<Chord> targets)
  {
    super(predicate);
    createdCount++;

    if (keys == null) {
      edges = new Vector<OutEdge>(2);
      return;
    }

    int ks = keys.size();

    assert (ks == targets.size()) :
      "The number of keys must match the number of targets " + this;

    edges = new Vector<OutEdge>(ks);

    for (int i = 0; i < ks; i++) {
      Object key = keys.elementAt(i);
      Chord  d   = targets.elementAt(i);

      edges.addElement(new OutEdge(key, d));
      d.addInCfgEdge(this);
    }
  }

  /**
   * Create a Chord that has more than one out-going CFG edge where
   * the edge is selected by some computation.
   * @param predicate the expression which is evaluated to select the
   * out-going edge
   */
  public SwitchChord(Expr predicate)
  {
    this(predicate, null, null);
  }

  public void visit(Predicate p)
  {
    p.visitSwitchChord(this);
  }

  /**
   * Set both out-going CFG edges to <code>null</code>.
   * This method does not maintain the validity of the CFG as the
   * nodes at the other ends of the edges are not modified.
   */
  public void deleteOutCfgEdges()
  {
    edges.clear();
  }

  /**
   * Make a copy of this Chord with the same out-going CFG edges.
   * The validity of the CFG graph is maintained.
   * @return a copy of this node
   */
  public Chord copy()
  {
    SwitchChord n = new SwitchChord(getPredicateExpr().copy());
    n.copySourceLine(this);
    int         l = edges.size();
    for (int i = 0; i < l; i++) {
      OutEdge oe = edges.elementAt(i);
      OutEdge noe = oe.copy();
      n.edges.addElement(noe);
      noe.target.addInCfgEdge(n);
    }
    return n;
  }

  /**
   * Return the index of the selected out-going CFG edge.
   * @param key specifies the out-going CFG edge
   */
  public int getBranchEdgeIndex(Object key)
  {
    int ks     = edges.size();
    int target = -1;
    if (key instanceof IntLiteral) {
      long keyv = ((IntLiteral) key).getLongValue();
      for (int i = 0; i < ks; i++) {
        OutEdge oe = edges.elementAt(i);
        if (!oe.defaultKey && (oe.key == keyv))
          return i;
        if (oe.defaultKey)
          target = i;
      }
    } else if (key instanceof String) {
      for (int i = 0; i < ks; i++) {
        OutEdge oe = edges.elementAt(i);
        if (oe.defaultKey)
          return i;
      }
    } else
      throw new scale.common.InternalError("What " + key);

    return target;
  }

  /**
   * Return the selected OutEdge instance.
   * @param key specifies the out-going CFG edge
   */
  private OutEdge getOutEdgeKey(Object key)
  {
    int ks = edges.size();
    if (key instanceof IntLiteral) {
      long keyv = ((IntLiteral) key).getLongValue();
      for (int i = 0; i < ks; i++) {
        OutEdge oe = edges.elementAt(i);
        if (oe.defaultKey)
          continue;
        if (oe.key == keyv)
          return oe;
      }
    } else if (key instanceof String) {
      for (int i = 0; i < ks; i++) {
        OutEdge oe = edges.elementAt(i);
        if (oe.defaultKey)
          return oe;
      }
    } else
      throw new scale.common.InternalError("What " + key);

    return null;
  }

  /**
   * Return the selected OutEdge instance.
   * @param key specifies the out-going CFG edge
   */
  private OutEdge getOutEdgeTarget(Chord target)
  {
    int ks = edges.size();
    for (int i = 0; i < ks; i++) {
      OutEdge oe = edges.elementAt(i);
      if (oe.target == target)
        return oe;
    }
    return null;
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
    OutEdge oe = getOutEdgeTarget(oldChord);
    if (oe == null)
      throw new InvalidMutationError(oldChord +
                                     " is not an out-going CFG edge of " +
                                     this);

    oe.target = newChord;
  }

  /**
   * Return the number of out-going CFG edges.
   */
  public final int numOutCfgEdges()
  {
    return edges.size();
  }

  /**
   * Return the specified out-going CFG edge.
   */
  public final Chord getOutCfgEdge(int i)
  {
    return edges.elementAt(i).target;
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
    int l = edges.size();
    for (int i = 0; i < l; i++) {
      Chord out = edges.elementAt(i).target;
      if (out == to) {
        if (skip == 0)
          return i;
        skip--;
      }
    }

    throw new scale.common.InternalError("Edge (" + skip + ") not found " + to);
  }

  /**
   * Return an array of all of the keys used.  They are guaranteed to be
   * in the same order as the CFG out-going edges.
   */
  public final long[] getBranchEdgeKeyArray()
  {
    int   l       = edges.size();
    long[] array = new long[l];
    for (int i = 0; i < l; i++)
      array[i] = edges.elementAt(i).key;
    return array;
  }

  /**
   *  Return the index of the default case or -1 if none.
   */
  public final int getDefaultIndex()
  {
    int l = edges.size();
    for (int i = 0; i < l; i++)
      if (edges.elementAt(i).defaultKey)
        return i;
    return -1;
  }

  /**
   * Use this method when you may be modifying an out-going CFG edge
   * from this Chord while iterating over the out-going edges.
   * @return an array of all of the out-going CFG edges.  They are
   * guaranteed to be in the same order as the branch arm keys.
   */
  public final Chord[] getOutCfgEdgeArray()
  {
    int   l       = edges.size();
    Chord[] array = new Chord[l];
    for (int i = 0; i < l; i++)
      array[i] = edges.elementAt(i).target;
    return array;
  }

  /**
   * This method adds changes an out-going CFG edge of the branch.  It
   * is a lot like ChangeOutCfgEdge, except that it manages the
   * pairing of branch target with key value.  The validity of the CFG
   * graph is maintained.
   * @param key When predicate evaluates to this value, the associated
   * branch will be taken.
   * @param target Target of branch associated with this key.
   */
  public void addBranchEdge(Object key, Chord target)
  {
    assert (key != null)    : "Null key for " + target + " " + this;
    assert (target != null) : "Null target for " + key + " " + this;

    OutEdge oe = getOutEdgeKey(key);

    if (oe != null) {
      Chord oldt = oe.target;
      oldt.deleteInCfgEdge(this);
      if (target != null) {
        oe.target = target;
        target.addInCfgEdge(this);  // Update other end of edge.
      } else {
        edges.removeElementAt(edges.indexOf(oe));
      }
    } else {
      edges.addElement(new OutEdge(key, target));
      target.addInCfgEdge(this);  // Update other end of edge.
    }
  }

  /**
   * Change the out-going CFG edge indicated by the position to the
   * new edge.  The validity of the CFG graph is maintained.
   * @param oldEdge the out-going CFG edge to be changed
   * @param newEdge the new out-going CFG edge
   */
  public void changeOutCfgEdge(Chord oldEdge, Chord newEdge)
  {
    int es = edges.size();
    for (int i = 0; i < es; i++) {
      OutEdge oe = edges.elementAt(i);
      if (oe.target != oldEdge)
        continue;

      Chord oldt = oe.target;
      oldt.deleteInCfgEdge(this);
      if (newEdge != null) {
        oe.target = newEdge;
        newEdge.addInCfgEdge(this);  // Update other end of edge.
      } else
        edges.removeElementAt(i);

      return;
    }

    throw new scale.common.InvalidMutationError("Not an existing out-edge " + oldEdge);
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
    assert ((probability <= 1.0) && (probability >= 0.0)) :
      "Invalid probability " + probability;

    short prob = (short) (probability * 32766.0);
    int   l    = edges.size();
    for (int i = 0; i < l; i++) {
      OutEdge oe = edges.elementAt(i);
      if (edge == oe.target) {
        oe.branchPrediction = prob;
        return;
      }
    }

    throw new scale.common.InternalError("Not an out-going edge " + edge);
  }

  /**
   * Return the probability that the specified edge will be executed
   * next.  If there are multiple out-going CFG edges to the same CFG
   * node, the probability is the sum of the probability of all the
   * edges to that node.  The stored value for the probability is
   * quantized for reasons of space utilization.  The sum of the
   * probabilities of both edges is always (approximately) 1.0.
   */
  public final double getBranchProbability(Chord edge)
  {
    int   l    = edges.size();
    short prob = 0;
    for (int i = 0; i < l; i++) {
      OutEdge oe = edges.elementAt(i);
      if (edge == oe.target)
        prob +=  oe.branchPrediction;
    }

    return ((double) prob) / 32766.0;
  }

  /**
   * Add the successors of this Chord to the stack.
   */
  public final void pushAllOutCfgEdges(Stack<Chord> wl)
  {
    int l = edges.size();
    for (int i = 0; i < l; i++) {
      Chord edge = edges.elementAt(i).target;
      if (edge != null)
        wl.push(edge);
    }
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
    int l = edges.size();
    for (int i = 0; i < l; i++) {
      Chord edge = edges.elementAt(i).target;
      if ((edge != null) && !edge.visited()) {
        wl.push(edge);
        edge.setVisited();
      }
    }
  }

  /**
   * Add the successors of this Chord to the stack if they haven't
   * been visited before.
   * @param done is the set of visited CFG nodes
   */
  public final void pushOutCfgEdges(Stack<Chord> wl, HashSet<Chord> done)
  {
    int l = edges.size();
    for (int i = 0; i < l; i++) {
      Chord edge = edges.elementAt(i).target;
      if ((edge != null) && done.add(edge))
        wl.push(edge);
    }
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
    
    int l = edges.size();
    for (int i = 0; i < l; i++) {
      Chord edge = edges.elementAt(i).target;
      if ((edge != null) && !edge.visited() && edge.parentsVisited())
        wl.push(edge);      
    }
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
    int l = edges.size();
    for (int i = 0; i < l; i++) {
      Chord edge = edges.elementAt(i).target;
      if ((edge != null) && !edge.visited() && edge.parentsFinished(finished)) {
        wl.push(edge);
        edge.setVisited();
      }
    }
  }

  /**
   * Clear all the markers.  Each marker is associated with one
   * out-going CFG edge..
   */
  public void clearEdgeMarkers()
  {
    int s = edges.size();
    for (int i = 0; i < s; i++)
      edges.elementAt(i).marked = false;
  }

  /**
   * Return the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   * @return true is the specified edge is marked
   * @see #getOutCfgEdge
   */
  public boolean edgeMarked(int edge)
  {
    OutEdge oe = edges.elementAt(edge);
    return oe.marked;
  }

  /**
   * Set the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   * @see #getOutCfgEdge
   */
  public void markEdge(int edge)
  {
    OutEdge oe = edges.elementAt(edge);
    oe.marked = true;
  }

  /**
   * Clear the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   * @see #getOutCfgEdge
   */
  public void clearEdge(int edge)
  {
    OutEdge oe = edges.elementAt(edge);
    oe.marked = false;
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
    int es = edges.size();
    for (int i = 0; i < es; i++) {
      OutEdge oe = edges.elementAt(i);
      Chord   nt = nm.get(oe.target);

      if (nt == null)
        continue;

      oe.target.deleteInCfgEdge(this);
      nt.addInCfgEdge(this);
      oe.target = nt;
    }
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 10 + getPredicateExpr().executionCostEstimate();
  }
}
