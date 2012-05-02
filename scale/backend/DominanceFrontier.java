package scale.backend;

import java.util.Iterator;

import scale.common.*;

/**
 * This class computes and manages dominance frontiers for a graph.
 * <p>
 * $Id: DominanceFrontier.java,v 1.2 2005-03-24 13:56:45 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class DominanceFrontier
{
  private static int computedCount = 0; // A count of times the dominance frontier was computed.
  private static int createdCount  = 0; // A count of all the current instances of this class.

  private static final String[] stats = {"created", "computed"};

  static
  {
    Statistics.register("scale.backend.DominanceFrontier", stats);
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * Return the number of times the dominance frontier was computed.
   */
  public static int computed()
  {
    return computedCount;
  }

  /**
   * This field holds the actual dominance frontiers.
   * <p>
   * Dominances frontiers are computed for each node.  The HashMap
   * holds tuples of the form <Node, HashSet>, where the HashSet
   * defines the dominance frontier for the associated Node.
   */
  private HashMap<Node, Object> frontiers;
  /**
   * This field holds the dominance relations.
   */
  private Domination dom;
  /**
   * This field holds the root of the graph.
   */
  private Node begin;

  /**
   * @param begin is the root node of the PFG (or end node for post-dominators)
   * @param dom is the (post) dominance relation ship
   */
  public DominanceFrontier(Node begin, Domination dom)
  {
    this.dom       = dom;
    this.begin     = begin;
    this.frontiers = new HashMap<Node, Object>(203);

    computeDominanceFrontier();
    createdCount++;
  }

  /**
   * Return an iteration of all of the nodes on the dominance frontier of a PFG node.
   * @param bb the node for which the dominance frontier is requested.
   */
  @SuppressWarnings("unchecked")
  public Iterator<Node> getDominanceFrontier(Node bb)
  {
    Object df = frontiers.get(bb);
    if (df == null)
      return new EmptyIterator<Node>();

    if (df instanceof HashSet)
      return ((HashSet<Node>) df).iterator();

    return new SingleIterator<Node>((Node) df);
  }

  /**
   * Return true if b2 is in b1's dominance frontier.
   */
  public boolean inDominanceFrontier(Node b1, Node b2)
  {
    Object df = frontiers.get(b1);
    if (df == null)
      return false;
    if (df instanceof HashSet)
      return ((HashSet) df).contains(b2);
    return (df == b2);
  }

  @SuppressWarnings("unchecked")
  private Object addDf(Object set, Object newmem)
  {
    if (set == null)
      return newmem;

    if (set instanceof HashSet) {
      ((HashSet<Node>) set).add((Node) newmem);
      return set;
    }
    HashSet<Object> ns = new HashSet<Object>(3);
    ns.add(set);
    ns.add(newmem);
    return ns;
  }

  /**
   * Compute the dominance frontier of the PFG.  
   * <br>
   * The dominators must be computed first.  The algorithm first goes depth first on
   * the dominance tree pushing children on a stack.  This insures that children in 
   * the dominance tree are popped off the stack before their parents.  Then, once the
   * stack is created, dominators are popped off and processed.  Since the dominance 
   * frontier ofa dominator is the union of the dominance frontiers of its children 
   * and those immediate children in the PFG that it doesn't dominate, the first have
   * been calculated already and the last can be computed directly.
   * @return the node to dominance frontier mapping
   * @see Domination
   */
  private void computeDominanceFrontier()
  {
    Stack<Node> wl   = WorkArea.<Node>getStack("computeDominanceFrontier");
    Stack<Node> tree = WorkArea.<Node>getStack("computeDominanceFrontier"); /* The dominator tree - sort of */

    computedCount++;

    wl.push(begin);

    while (!wl.empty()) { // Make sure children are processed before parents
      Node n = wl.pop();
      tree.push(n);
      for (Node c : dom.getDominatees(n)) {
        tree.push(c);
        wl.push(c);
      }
    }

    WorkArea.<Node>returnStack(wl);
    wl = null;

    while (!tree.empty()) {
      Node   n = tree.pop();
      Object S = null;

      // Determine those immediate children in the PFG who are not dominated by n.

      if (dom.isPostDomination()) {
        int l = n.numInEdges();
        for (int i = 0; i < l; i++) {
          Node y = n.getInEdge(i);
          if (n != dom.getDominatorOf(y))
            S = addDf(S, y);
        }
      } else {
        int l = n.numOutEdges();
        for (int i = 0; i < l; i++) {
          Node y = n.getOutEdge(i);
          if (n != dom.getDominatorOf(y))
            S = addDf(S, y);
        }
      }

      // Union in the dominance frontiers of n's children in the dominator tree.

      for (Node c : dom.getDominatees(n)) {
        Iterator<Node> ec = getDominanceFrontier(c); // The child is done first!
        while (ec.hasNext()) {
          Node w = ec.next();
          if (n != dom.getDominatorOf(w)) {
            S = addDf(S, w);
          }
        }
      }

      if (S != null)
        frontiers.put(n, S);
    }

    WorkArea.<Node>returnStack(tree);
  }
}
