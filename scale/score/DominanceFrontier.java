package scale.score;

import java.util.Iterator;
import scale.common.Stack;

import scale.common.*;

import scale.clef.decl.Declaration;
import scale.score.chords.*;

/**
 * This class computes and manages dominance frontiers.
 * <p>
 * $Id: DominanceFrontier.java,v 1.73 2007-10-04 19:58:18 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class also converts irreducible graphs to reducible graphs by
 * "node-splitting".
 */
public class DominanceFrontier
{
  private static int splitNodeCount  = 0; // A count of the split nodes created.
  private static int computedCount   = 0; // A count of times the dominance frontier was computed.
  private static int createdCount    = 0; // A count of all the current instances of this class.
  private static int newCFGNodeCount = 0; // A count of new nodes created.

  private static final String[] stats = {
    "created",
    "computed",
    "splitNodes",
    "newCFGNodes"};

  static
  {
    Statistics.register("scale.score.DominanceFrontier", stats);
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
   * Return the current number of split nodes created.
   */
  public static int splitNodes()
  {
    return splitNodeCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * This field holds the actual dominance frontiers.
   * <p>
   * Dominances frontiers are computed for each node.  The HashMap
   * holds tuples of the form <Chord, HashSet>, where the HashSet
   * defines the dominance frontier for the associated Chord.
   */
  private HashMap<Chord, Object> frontiers;
  /**
   * This field holds the dominance relations.
   */
  private Domination dom;
  /**
   * This field holds the root of the CFG.
   */
  private Chord begin;

  /**
   * @param begin is the root node of the CFG (or end node for
   * post-dominators)
   * @param dom is the (post) dominance relation ship
   */
  public DominanceFrontier(Chord begin, Domination dom)
  {
    super();
    this.dom       = dom;
    this.begin     = begin;
    this.frontiers = new HashMap<Chord, Object>(203);

    computeDominanceFrontier();
    createdCount++;
  }

  /**
   * Return an iteration of all of the nodes on the dominance frontier
   * of a CFG node.
   * @param bb the node for which the dominance frontier is requested.
   */
  @SuppressWarnings("unchecked")
  public Iterator<Chord> getDominanceFrontier(Chord bb)
  {
    Object df = frontiers.get(bb);
    if (df == null)
      return new EmptyIterator<Chord>();

    if (df instanceof HashSet)
      return ((HashSet<Chord>) df).iterator();

    return new SingleIterator<Chord>((Chord) df);
  }

  /**
   * Return true if b2 is in b1's dominance frontier.
   */
  public boolean inDominanceFrontier(Chord b1, Chord b2)
  {
    Object df = frontiers.get(b1);
    if (df == null)
      return false;
    if (df instanceof HashSet) {
      @SuppressWarnings("unchecked")
      HashSet<Chord> hs = (HashSet<Chord>) df;
      return hs.contains(b2);
    }
    return (df == b2);
  }

  private Object addDf(Object set, Chord newmem)
  {
    if (set == null)
      return newmem;

    if (set instanceof HashSet) {
      @SuppressWarnings("unchecked")
      HashSet<Chord> hs = (HashSet<Chord>) set;
      hs.add(newmem);
      return hs;
    }
    HashSet<Chord> ns = new HashSet<Chord>(3);
    ns.add((Chord) set);
    ns.add(newmem);
    return ns;
  }

  /**
   * Compute the dominance frontier of the Scribble graph.  The
   * dominators must be computed first.  The algorithm first goes
   * depth first on the dominance tree pushing children on a stack.
   * This insures that children in the dominance tree are popped off
   * the stack before their parents.  Then, once this stack is
   * created, dominators are popped off and processed.  Since the
   * dominance frontier of a dominator is the union of the dominance
   * frontiers of its children and those immediate children in the CFG
   * graph that it doesn't dominate, the first have been calculated
   * already and the last can be computed directly.
   * @return the node to dominance frontier mapping
   * @see Domination
   */
  private void computeDominanceFrontier()
  {
    Stack<Chord> wl   = WorkArea.<Chord>getStack("computeDominanceFrontier");
    Stack<Chord> tree = WorkArea.<Chord>getStack("computeDominanceFrontier"); // The dominator tree - sort of

    computedCount++;

    wl.push(begin);

    while (!wl.empty()) { // Make sure children are processed before parents
      Chord n = wl.pop();
      tree.push(n);
      Chord[] ds = dom.getDominatees(n);
      for (Chord c : ds) {
        tree.push(c);
        wl.push(c);
      }
    }

    WorkArea.<Chord>returnStack(wl);

    wl = null;

    while (!tree.empty()) {
      Chord  n = tree.pop();
      Object S = null;

      // Determine those immediate children in the CFG who are not
      // dominated by n.

      if (dom.isPostDomination()) {
        int l = n.numInCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord y = n.getInCfgEdge(i);
          if (n != dom.getDominatorOf(y))
            S = addDf(S, y);
        }
      } else {
        int l = n.numOutCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord y = n.getOutCfgEdge(i);
          if (n != dom.getDominatorOf(y))
            S = addDf(S, y);
        }
      }

      // Union in the dominance frontiers of n's children in the
      // dominator tree.

      Chord[] ds = dom.getDominatees(n);
      for (Chord c : ds) {
        Iterator<Chord> ec = getDominanceFrontier(c); // The child is done first!
        while (ec.hasNext()) {
          Chord w = ec.next();
          if (n != dom.getDominatorOf(w)) {
            S = addDf(S, w);
          }
        }
      }

      if (S != null)
        frontiers.put(n, S);
    }

    WorkArea.<Chord>returnStack(tree);
  }

  /**
   * Create duplicates of a node and the nodes it dominates.  It links
   * the parent to this new child, the child to the nodes it dominates
   * and these dominated nodes to the nodes their doppelgangers are
   * linked to.
   * @param parent the in-coming CFG edge
   * @param ochild the head of the sub-graph that is duplicated
   */
  private void duplicateSubGraph(Chord parent, Chord ochild)
  {
    HashMap<Chord, Chord> nm   = new HashMap<Chord, Chord>(23); // Mapping from old nodes to their new copies.
    Stack<Chord>          wl   = WorkArea.<Chord>getStack("duplicateSubGraph");
    Vector<Chord>         newn = new Vector<Chord>();

    wl.push(ochild);

    nm.put(parent, parent);
    newn.addElement(parent);

    // Get all the nodes in the dominator sub-tree and make copies.

    while (!wl.empty()) {
      Chord child = wl.pop();
      if (nm.get(child) != null)
        continue;


      Chord nn = child.copy();

      newCFGNodeCount++;
      splitNodeCount++;

      nm.put(child, nn);
      newn.addElement(nn);

      Chord[] ds = dom.getDominatees(child);
      for (Chord d : ds) {
        if (!(d instanceof scale.score.chords.EndChord))
          wl.push(d);
      }
    }

    WorkArea.<Chord>returnStack(wl);
    Scribble.linkSubgraph(newn, nm, null);
  }

  /**
   * Reduce an irreducible graph by spliting nodes.  The logic has to
   * handle the following case.  That's why we select the node with
   * the fewest in-coming edges.
   * <pre>
   * int cmd_exec(int cmdparm)
   * {
   *     if (cmdparm == 5)
   *       goto finish_while;
   * 
   *   until_loop:
   *     if (cmdparm == 6) {
   *       if (cmdparm == 1) {
   *       finish_while:
   *    if (cmdparm != 2)
   *      goto until_loop;
   *       }
   *       if (cmdparm == 3) {
   *    goto until_loop;
   *       }
   *     }
   *   return 0;
   * }
   * </pre>

   * If the CFG is modified, new dominators and a new dominance
   * frontier must be computed and makeGraphReducible() must be called
   * again.  This loop must be iterated until makeGraphReducible()
   * returns false.
   * <p>
   * If there are too many possible implicit loops, this logic can
   * take a very long time and result in very large CFGs.  We punt in
   * this case.  As a result, other optimizations that SSA form are
   * not performed.
   * @param maxImplicitLoopFactor is a limit on the complexity of the
   * graph that will be reduced
   * @return true if the CFG has been modified
   * @exception scale.common.ResourceException if there are too many
   * possible implicit loops
   */
  public boolean makeGraphReducible(int maxImplicitLoopFactor)
    throws scale.common.ResourceException
  {
    HashSet<Chord> done  = WorkArea.<Chord>getSet("makeGraphReducible");
    Stack<Chord>   wl    = WorkArea.<Chord>getStack("makeGraphReducible");
    Chord   sn    = null;
    Chord   pn    = null;
    int     ine   = 512;
    int     total = 0;

    done.add(begin);
    wl.push(begin);
    wl.push(null);

    int numNodes = 0;
    while (!wl.empty()) { // Find all the nodes that are part of an irreducible graph
      Chord parent = wl.pop();
      Chord n      = wl.pop();
      numNodes++;

      if (n.isLoopHeader()) {
        // If the loop header and its successor are in each other's
        // dominance frontier, we want to fix it via the successor.
        // An example:
        //    for () {
        //  label:
        //      switch() {
        //        ...
        //        goto label2;
        //        ...
        //      }
        //    }
        //    ...
        // label2:
        //    ...
        //    goto label;
        //    ...
        // See eval() routine in perl to see why this is needed.

        Chord c = n.getNextChord();
        if (done.add(c)) {
          wl.push(c);
          wl.push(n);
        }
        continue;
      }

      if (n.numInCfgEdges() > 1) {
        int      cnt = 0;
        Iterator<Chord> e   = getDominanceFrontier(n);
        while (e.hasNext()) {
          Chord y = e.next();
          if ((y != n) && inDominanceFrontier(y, n)) {
            cnt++;
            total++;
          }
        }

        if ((cnt > 0) && (ine >= cnt)) { // Select the one with the fewest loops.
          sn  = n;
          ine = cnt;
          pn  = parent;
        }
      }

      int l = n.numOutCfgEdges();
      for (int i = 0; i < l; i++) {
        Chord c = n.getOutCfgEdge(i);
        if (done.add(c)) {
          wl.push(c);
          wl.push(n);
        }
      }
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(done);

    if (sn == null)
      return false;

    if (total > maxImplicitLoopFactor)
      throw new scale.common.ResourceException("Too many implicit loops.");

    LoopHeaderChord snlh = sn.getLoopHeader(numNodes);
    int             l    = sn.numInCfgEdges();
    for (int i = 0; i < l; i++) {
      Chord in = sn.getInCfgEdge(i);
      if (in.getLoopHeader(numNodes) != snlh)
        throw new scale.common.ResourceException("Branch into loop.");
    }

    duplicateSubGraph(pn, sn);

    return true;
  }
}
