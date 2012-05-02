package scale.backend;

import java.util.Iterator;

import scale.common.*;

/**
 * This class computes the dominators and post dominators of nodes in
 * a graph.
 * <p>
 * $Id: Domination.java,v 1.9 2007-10-04 19:57:48 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * As described in A Fast Algorithm for Finding Dominators in a Flowgraph
 * by Lengauer and Tarjan, ACM. Tran. on Prog. Lang and System, July 1979.
 */
public class Domination
{
  private static int deadNodeCount = 0; // A count of nodes removed because they were un-reachable.
  private static int computedCount = 0; // A count of times the dominance frontier was computed.
  private static int createdCount  = 0; // A count of all the created instances of this class.

  static
  {
    Statistics.register("scale.backend.Domination", "deadNodes");
    Statistics.register("scale.backend.Domination", "created");
    Statistics.register("scale.backend.Domination", "computed");
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
   * Return the number of dead nodes removed.
   */
  public static int deadNodes()
  {
    return deadNodeCount;
  }

  /**
   * Map from dominator node to domination information.
   */
  private HashMap<Node, Node[]> domination; 
  /**
   * For computation of dominators.
   */
  private Node[] vertex;     
  /**
   * For computation of dominators.
   */
  private Node[] parent;
  /**
   * For computation of dominators.
   */
  private Node[] label;
  /**
   * For computation of dominators.
   */
  private Node[] ancestor;
  /**
   * A node in the list of nodes in this bucket.
   */
  private Node[] bucketNode;
  /**
   * Index of the next node in the list.
   */
  private int[] bucketNext;
  /**
   * Index of the first bucket for this node.
   */
  private int[] bucketHead;
  /**
   * Index of the next available bucket.  This value always starts
   * with 1 so that a value of 0 can mean end-of-list.
   */
  private int bucketPtr;
  /**
   * For computation of dominators.
   */
  private int[] semi;
  /**
   * Is it post dominators?
   */
  private boolean post;

  /**
   * @param post false for dominators, true for post dominators to be
   * determined
   * @param start the starting node in the graph (or end if post
   * dominators)
   */
  public Domination(boolean post, Node start)
  {
    this.post = post;

    createdCount++;
    computedCount++;

    int num = setNodeLabels(start);

    vertex     = new Node[num];
    semi       = new int[num];
    parent     = new Node[num];
    ancestor   = new Node[num];
    label      = new Node[num];
    bucketNode = new Node[num];
    bucketNext = new int[num];
    bucketHead = new int[num];
    bucketPtr  = 1;
    domination = new HashMap<Node, Node[]>(1 + num / 4);

    for (int i = 0; i < num; i++) {
      semi[i]     = -1;
      vertex[i]   = null;
      parent[i]   = null;
      ancestor[i] = null;
      label[i]    = null;

      bucketNode[i] = null;
      bucketNext[i] = 0;
      bucketHead[i] = 0;
    }

    int n = DFSFromArcs(start);

    // Make sure we record any nodes that are dead

    for (int j = 0; j < num; j++)
      if (vertex[j] != null)
        if (post)
          n = processOutEdges(vertex[j], n);
        else
          n = processInEdges(vertex[j], n);

    // Compute the dominators

    generate(start, num);

    semi     = null;
    parent   = null;
    ancestor = null;
    label    = null;

    bucketHead = null;
    bucketNode = null;
    bucketNext = null;
  }

  /**
   * Label nodes using a breadth-first search.
   */
  private int setNodeLabels(Node d)
  {
    int         n  = 0;
    Stack<Node> wl = WorkArea.<Node>getStack("setNodeLabels");
     
    wl.push(d);
    d.nextVisit();
    d.setVisited();

    while (!wl.empty()) {
      Node v = wl.remove(0);
      v.setLabel(n);
      n++;

      v.pushInEdges(wl);
      v.pushOutEdges(wl);
    }
    
    WorkArea.<Node>returnStack(wl);
    
    return n;
  }

  private int DFSFromArcs(Node d)
  {
    int         n  = 0;
    Stack<Node> wl = WorkArea.<Node>getStack("DFSFromArcs");

    wl.push(d);

    while (!wl.empty()) {
      Node v   = wl.pop();
      int        vid = v.getLabel();
      if (semi[vid] >= 0)
        continue;

      semi[vid]  = n;
      vertex[n]  = v;
      label[vid] = v;
      n++;

      if (post) {
        int l = v.numInEdges();
        for (int i = 0; i < l; i++) {
          Node w   = v.getInEdge(i);
          int        wid = w.getLabel();
          if (semi[wid] == -1) {
            parent[wid] = v;
            wl.push(w);
          }
        }
      } else {
        int l = v.numOutEdges();
        for (int i = 0; i < l; i++) {
          Node w   = v.getOutEdge(i);
          int        wid = w.getLabel();
          if (semi[wid] == -1) {
            parent[wid] = v;
            wl.push(w);
          }
        }
      }
    }

    WorkArea.<Node>returnStack(wl);

    return n;
  }

  /**
   * Handle the cases where there is dead code.  That is, nodes for
   * which there is no path from the start node.
   */
  private int processInEdges(Node d, int n)
  {
    Stack<Node> wl = WorkArea.<Node>getStack("processInEdges");

    wl.push(d);

    while (!wl.empty()) {
      Node v = wl.pop();
      int  l = v.numInEdges();
      for (int i = 0; i < l; i++) {
        Node w   = v.getInEdge(i);
        int        wid = w.getLabel();
        if (semi[wid] == -1) {
          semi[wid]   = n;
          vertex[n]   = w;
          label[wid]  = w;
          parent[wid] = v;
          n++;
          wl.push(w);
        }
      }
    }

    WorkArea.<Node>returnStack(wl);

    return n;
  }

  /**
   * Handle the cases where there is dead code.  That is, nodes for
   * which there is no path to the end node.
   */
  private int processOutEdges(Node d, int n)
  {
    Stack<Node> wl = WorkArea.<Node>getStack("processOutEdges");

    wl.push(d);

    while (!wl.empty()) {
      Node v = wl.pop();
      int  l = v.numOutEdges();
      for (int i = 0; i < l; i++) {
        Node w   = v.getOutEdge(i);
        int        wid = w.getLabel();
        if (semi[wid] == -1) {
          semi[wid]   = n;
          vertex[n]   = w;
          label[wid]  = w;
          parent[wid] = v;
          n++;
          wl.push(w);
        }
      }
    }

    WorkArea.<Node>returnStack(wl);

    return n;
  }

  /**
   * Node calculation: dominator and post dominator As described in A
   * Fast Algorithm for Finding Dominators in a Flowgraph by Lengauer
   * and Tarjan, ACM. Tran. on Prog. Lang and System, July 1979.
   */
  private void generate(Node start, int n)
  {
    for (int i = n - 1; i >= 1; i--) {
      Node w   = vertex[i];
      int        wid = w.getLabel();

      // step 2

      if (post) {
        int l = w.numOutEdges();
        for (int j = 0; j < l; j++) {
          Node v = w.getOutEdge(j);
          if (v == w)
            continue;

          Node u   = eval(v);
          int        uid = u.getLabel();
          if (semi[uid] < semi[wid])
            semi[wid] = semi[uid];        
        }
      } else {
        int l = w.numInEdges();
        for (int j = 0; j < l; j++) {
          Node v = w.getInEdge(j);
          if (v == w)
            continue;

          Node u   = eval(v);
          int        uid = u.getLabel();
          if (semi[uid] < semi[wid])
            semi[wid] = semi[uid];        
        }
      }

      Node pw   = parent[wid];
      int  pwid = pw.getLabel();
      
      ancestor[wid] = pw;

      addToBucket(vertex[semi[wid]], w);

      // Step 3

      int b = bucketHead[pwid];
      while (b != 0) {
        Node v = bucketNode[b];

        bucketNode[b] = null;
        b = bucketNext[b];
        if (v == null)
          continue;

        Node u   = eval(v);
        int  vid = v.getLabel();
        int  uid = u.getLabel();
        Node dom = (semi[uid] < semi[vid]) ? u : pw;
        setDominator(v, dom);
      }
    }

    // Step 4

    for (int j = 1; j < n; j++) {
      Node w   = vertex[j];
      int  wid = w.getLabel();
      Node pd  = getDominatorOf(w);
      if ((pd != null) && (pd != vertex[semi[wid]]))
        setDominator(w, getDominatorOf(pd));
    }

    setDominator(start, null);

    // Add dominatees to dominators.

    for (int i = 1; i < n; i++) {
      Node dominatee = vertex[i];
      addDominatee(getDominatorOf(dominatee), dominatee);
    }
  }

  private void addToBucket(Node v, Node w)
  {
    int bid = v.getLabel();
    int b   = bucketHead[bid];

    while (b != 0) {
      if (bucketNode[b] == null) {
        bucketNode[b] = w;
        return;
      }

      if (bucketNode[b] == w)
        return;

      b = bucketNext[b];
    }

    if (bucketPtr >= bucketNode.length) {
      Node[] nbn = new Node[2 * bucketPtr];
      System.arraycopy(bucketNode, 0, nbn, 0, bucketPtr);
      bucketNode = nbn;
      int[] nbx = new int[2 * bucketPtr];
      System.arraycopy(bucketNext, 0, nbx, 0, bucketPtr);
      bucketNext = nbx;
    }

    b = bucketPtr++;
    bucketNode[b] = w;
    bucketNext[b] = bucketHead[bid];
    bucketHead[bid] = b;
  }

  private Node eval(Node v) 
  {
    int vid = v.getLabel();

    if (ancestor[vid] == null) 
      return v;
    
    compress(v);
    return label[vid];
  }

  private void compress(Node v)
  {
    Stack<Node> wl = WorkArea.<Node>getStack("compress");

    // We have to start at the bottom of the dominator tree.

    while (true) {
      int        vid   = v.getLabel();
      Node anv   = ancestor[vid];
      int        anvid = anv.getLabel();

      if (ancestor[anvid] == null)
        break;

      wl.push(v);
      v = anv;
    }

    while (!wl.empty()) {
      v = wl.pop();

      int  vid   = v.getLabel();
      Node anv   = ancestor[vid];
      int  anvid = anv.getLabel();

      if (semi[label[anvid].getLabel()] < semi[label[vid].getLabel()])
        label[vid] = label[anvid];
      ancestor[vid] = ancestor[anvid];
    }

    WorkArea.<Node>returnStack(wl);
  }

  /**
   * Return the dominator of v.
   */
  public final Node getDominatorOf(Node v)
  {
    Node[] dom = domination.get(v);
    if (dom == null)
      return null;
    return dom[0];
  }
  
  private void setDominator(Node me, Node d)
  {
    Node[] med = domination.get(me);
    if (med == null) {
      if (d == null)
        return;
      med = new Node[2];
      domination.put(me, med);
    }
    med[0] = d; // Make d the dominator of me.
  }

  private void addDominatee(Node d, Node me)
  {
    if (d == null)
      return;

    // Make me a dominatee of d.

    Node[] dd = domination.get(d);
    if (dd == null) {
      dd = new Node[2];
      domination.put(d, dd);
      dd[1] = me;
      return;
    }

    for (int i = 1; i < dd.length; i++) {
      Node c = dd[i];
      if (c == me)
        return;
      if (c == null) {
        dd[i] = me;
        return;
      }
    }

    Node[] nd = new Node[dd.length + 4];
    System.arraycopy(dd, 0, nd, 0, dd.length);
    nd[dd.length] = me;
    domination.put(d, nd);
  }

  /**
   * Return an iteration of the nodes that <code>n</code> strictly dominates.
   */
  public final Node[] getDominatees(Node n)
  { 
    Node[] dominated = domination.get(n);
    if (dominated == null)
      return new Node[0];

    int cnt = 0;
    for (int i = 1; i < dominated.length; i++)
      if (dominated[i] != null)
        cnt++;

    int    k  = 0;
    Node[] nd = new Node[cnt];
    for (int i = 1; i < dominated.length; i++)
      if (dominated[i] != null)
        nd[k++] = dominated[i];

    return nd;
  }

  /**
   * Push onto the stack all of the nodes that n strictly dominates.
   * The nodes are placed on the stack in descending order by the node's label.
   * The nodes will be popped from the stack in reverse post-order.
   */
  public final void pushDominatees(Node n, Stack<Object> wl)
  { 
    Node[] dom = domination.get(n);
    if (dom == null)
      return;

    for (int i = 1; i < dom.length - 1; i++) {
      Node f = dom[i];
      if (f == null)
        break;
      for (int j = i + 1; j < dom.length; j++) {
        Node s = dom[j];
        if (s == null)
          break;
        if (f.getLabel() < s.getLabel()) {
          dom[j] = f;
          dom[i] = s;
          f = s;
        }
      }
    }

    for (int i = 1; i < dom.length; i++) {
      Node d = dom[i];
      if (d == null)
        break;
      wl.push(d);
    }
  }

  /**
   * Return the number of the nodes that n dominates.
   */
  public final int numDominatees(Node n)
  { 
    Node[] dom = domination.get(n);
    if (dom == null)
      return 0;

    int num = 0;
    for (int i = 1; i < dom.length; i++) {
      Node d = dom[i];
      if (d == null)
        break;
      num++;
    }
    return num;
  }

  /**
   * Return the set of all nodes strictly dominated by node n and all 
   * nodes dominated by nodes dominated by n, and so on.
   * @param n is the dominator node
   */
  public final Vector<Node> getIterativeDomination(Node n)
  {
    Vector<Node> v   = new Vector<Node>(20);
    Node[]       dom = domination.get(n);
    if (dom == null)
      return v;

    for (int i = 1; i < dom.length; i++)
      v.addElement(dom[i]);
    
    for (int j = 0; j < v.size(); j++) {
      Node c = v.elementAt(j);
      if (c == null) {
        v.removeElementAt(j);
        j--;
        continue;
      }

      Node[] d = domination.get(c);
      if (d == null)
        continue;

      for (int i = 1; i < d.length; i++)
        v.addElement(d[i]);
    }

    return v;
  }

  /**
   * Return the set of all nodes strictly dominated by node n and all 
   * nodes dominated by nodes dominated by n, and so on.
   * @param n is the dominator node
   * @param v is the vector to which the dominated nodes are added
   */
  protected void getIterativeDomination(Node n, Vector<Node> v)
  {
    int    start = v.size();
    Node[] dom   = domination.get(n);
    if (dom == null)
      return;

    for (int i = 1; i < dom.length; i++)
      v.addElement(dom[i]);
    
    for (int j = start; j < v.size(); j++) {
      Node c = v.elementAt(j);
      if (c == null) {
        v.removeElementAt(j);
        j--;
        continue;
      }

      Node[] d = domination.get(c);
      if (d == null)
        continue;

      for (int i = 1; i < d.length; i++)
        v.addElement(d[i]);
    }
  }

  /**
   * Return true if PFG node n dominates node d.
   * @param n the node to test
   * @param d the node to test
   */
  public final boolean inDominatees(Node n, Node d)
  {
    Node[] dom = domination.get(n);
    if (dom == null)
      return false;

    for (int i = 1; i < dom.length; i++) {
      if (d == dom[i])
        return true;
    }
    return false;
  }

  /**
   * Print out my dominance relations.
   * @param s the stream to write to
   */
  public final void displayDominance(java.io.PrintStream s, Node n)
  {
    Node[] dom = domination.get(n);
    if (dom == null)
      return;

    s.print(n.getLabel());
    s.print(" is dominated by ");
    s.print(dom[0]);
    s.print(" and dominates ");
    for (int i = 1; i < dom.length; i++) {
      Node d = dom[i];
      if (d == null)
        continue;

      s.print(" ");
      s.print(d.getLabel());
    }
    s.println("");
  }

  /**
   * Return true if this domination is a post domination.
   */
  public final boolean isPostDomination()
  {
    return post;
  }
}
