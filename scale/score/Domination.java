package scale.score;

import java.util.Iterator;
import scale.common.Stack;

import scale.common.*;
import scale.score.chords.Chord;
import scale.score.chords.LoopPreHeaderChord;
import scale.score.chords.LoopHeaderChord;
import scale.score.chords.LoopExitChord;
import scale.score.expr.Expr;

/**
 * This class computes the dominators and post dominators of nodes in
 * a graph.

 * <p>
 * $Id: Domination.java,v 1.76 2007-10-04 19:58:18 burrill Exp $
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
public class Domination extends Root
{
  private static int deadCFGNodeCount = 0; // A count of nodes removed because they were un-reachable.
  private static int computedCount    = 0; // A count of times the dominance frontier was computed.
  private static int createdCount     = 0; // A count of all the created instances of this class.

  static
  {
    Statistics.register("scale.score.Domination", "deadCFGNodes");
    Statistics.register("scale.score.Domination", "created");
    Statistics.register("scale.score.Domination", "computed");
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
  public static int deadCFGNodes()
  {
    return deadCFGNodeCount;
  }

  private HashMap<Chord, Chord[]> domination; // Map from dominator node to domination information.

  private Chord[] vertex;     // For computation of dominators.
  private Chord[] parent;     // For computation of dominators.
  private Chord[] label;      // For computation of dominators.
  private Chord[] ancestor;   // For computation of dominators.
  private Chord[] bucketNode; // A node in the list of nodes in this bucket.
  private int[]   bucketNext; // Index of the next node in the list.
  private int[]   bucketHead; // Index of the first bucket for this node.
  private int[]   semi;       // For computation of dominators.
  private int     bucketPtr;  // Index of the next available bucket.  This
                              // value always starts with 1 so that a value
                              // of 0 can mean end-of-list.
  private boolean post;       // Is it post dominators?

  /**
   * @param post false for dominators, true for post dominators to be
   * determined
   * @param start the starting node in the graph (or end if post
   * dominators)
   */
  public Domination(boolean post, Chord start)
  {
    super();
    this.post = post;

    createdCount++;
    computedCount++;

    int num  = setChordLabels(start);

    vertex     = new Chord[num];
    semi       = new int[num];
    parent     = new Chord[num];
    ancestor   = new Chord[num];
    label      = new Chord[num];
    bucketNode = new Chord[num];
    bucketNext = new int[num];
    bucketHead = new int[num];
    bucketPtr  = 1;
    domination = new HashMap<Chord, Chord[]>(1 + num / 4);

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

  private int setChordLabels(Chord d)
  {
    int           n  = 0;
    Stack<Chord> wl = WorkArea.<Chord>getStack("setChordLabels");

    wl.push(d);
    Chord.nextVisit();
    d.setVisited();

    while (!wl.empty()) {
      Chord v = wl.pop();
      v.setLabel(n);
      n++;

      v.pushInCfgEdges(wl);
      v.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);

    return n;
  }

  private int DFSFromArcs(Chord d)
  {
    int          n  = 0;
    Stack<Chord> wl = WorkArea.<Chord>getStack("DFSFromArcs");

    wl.push(d);

    while (!wl.empty()) {
      Chord v   = wl.pop();
      int   vid = v.getLabel();
      if (semi[vid] >= 0)
        continue;

      semi[vid]  = n;
      vertex[n]  = v;
      label[vid] = v;
      n++;

      if (post) {
        int l = v.numInCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord w   = v.getInCfgEdge(i);
          int   wid = w.getLabel();
          if (semi[wid] == -1) {
            parent[wid] = v;
            wl.push(w);
          }
        }
      } else {
        int l = v.numOutCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord w   = v.getOutCfgEdge(i);
          int   wid = w.getLabel();
          if (semi[wid] == -1) {
            parent[wid] = v;
            wl.push(w);
          }
        }
      }
    }

    WorkArea.<Chord>returnStack(wl);

    return n;
  }

  /**
   * Handle the cases where there is dead code.  That is, nodes for
   * which there is no path from the start node.
   */
  private int processInEdges(Chord d, int n)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("processInEdges");

    wl.push(d);

    while (!wl.empty()) {
      Chord v = wl.pop();
      int   l = v.numInCfgEdges();
      for (int i = 0; i < l; i++) {
        Chord w   = v.getInCfgEdge(i);
        int   wid = w.getLabel();
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

    WorkArea.<Chord>returnStack(wl);

    return n;
  }

  /**
   * Handle the cases where there is dead code.  That is, nodes for
   * which there is no path to the end node.
   */
  private int processOutEdges(Chord d, int n)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("processOutEdges");

    wl.push(d);

    while (!wl.empty()) {
      Chord v = wl.pop();
      int   l = v.numOutCfgEdges();
      for (int i = 0; i < l; i++) {
        Chord w   = v.getOutCfgEdge(i);
        int   wid = w.getLabel();
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

    WorkArea.<Chord>returnStack(wl);

    return n;
  }

  /**
   * Chord calculation: dominator and post dominator As described in A
   * Fast Algorithm for Finding Dominators in a Flowgraph by Lengauer
   * and Tarjan, ACM. Tran. on Prog. Lang and System, July 1979
   */

  private void generate(Chord start, int n)
  {
    for (int i = n - 1; i >= 1; i--) {
      Chord w   = vertex[i];
      int   wid = w.getLabel();

      // step 2

      if (post) {
        int l = w.numOutCfgEdges();
        for (int j = 0; j < l; j++) {
          Chord v = w.getOutCfgEdge(j);
          if (v == w)
            continue;

          Chord u   = eval(v);
          int   uid = u.getLabel();
          if (semi[uid] < semi[wid])
            semi[wid] = semi[uid];        
        }
      } else {
        int l = w.numInCfgEdges();
        for (int j = 0; j < l; j++) {
          Chord v = w.getInCfgEdge(j);
          if (v == w)
            continue;

          Chord u   = eval(v);
          int   uid = u.getLabel();
          if (semi[uid] < semi[wid])
            semi[wid] = semi[uid];        
        }
      }

      Chord pw   = parent[wid];
      int   pwid = pw.getLabel();

      ancestor[wid] = pw;

      addToBucket(vertex[semi[wid]], w);

      // Step 3

      int b = bucketHead[pwid];
      while (b != 0) {
        Chord v = bucketNode[b];

        bucketNode[b] = null;
        b = bucketNext[b];
        if (v == null)
          continue;

        Chord u   = eval(v);
        int   vid = v.getLabel();
        int   uid = u.getLabel();
        Chord dom = (semi[uid] < semi[vid]) ? u : pw;
        setDominator(v, dom);
      }
    }

    // Step 4

    for (int j = 1; j < n; j++) {
      Chord w   = vertex[j];
      int   wid = w.getLabel();
      Chord pd  = getDominatorOf(w);
      if ((pd != null) && (pd != vertex[semi[wid]]))
        setDominator(w, getDominatorOf(pd));
    }

    setDominator(start, null);

    // Add dominatees to dominators.

    for (int i = 1; i < n; i++) {
      Chord dominatee = vertex[i];
      addDominatee(getDominatorOf(dominatee), dominatee);
    }
  }

  private void addToBucket(Chord v, Chord w)
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
      Chord[] nbn = new Chord[2 * bucketPtr];
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

  private Chord eval(Chord v) 
  {
    int vid = v.getLabel();

    if (ancestor[vid] == null) 
      return v;

    compress(v);
    return label[vid];
  }

  private void compress(Chord v)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("compress");

    // We have to start at the bottom of the dominator tree.

    while (true) {
      int   vid   = v.getLabel();
      Chord anv   = ancestor[vid];
      int   anvid = anv.getLabel();

      if (ancestor[anvid] == null)
        break;

      wl.push(v);
      v = anv;
    }

    while (!wl.empty()) {
      v = wl.pop();

      int   vid   = v.getLabel();
      Chord anv   = ancestor[vid];
      int   anvid = anv.getLabel();

      if (semi[label[anvid].getLabel()] < semi[label[vid].getLabel()])
        label[vid] = label[anvid];
      ancestor[vid] = ancestor[anvid];
    }

    WorkArea.<Chord>returnStack(wl);
  }

//   private void compress(Chord v)
//   {
//     Stack<Chord> wl = WorkArea.<Chord>getStack("compress");

//     // We have to start at the bottom of the dominator tree.

//     int   vid   = v.getLabel();
//     Chord anv   = ancestor[vid];
//     int   anvid = anv.getLabel();

//     if (ancestor[anvid] == null)
//       return;

//     compress(anv);

//     if (semi[label[anvid].getLabel()] < semi[label[vid].getLabel()])
//       label[vid] = label[anvid];
//     ancestor[vid] = ancestor[anvid];
//   }

  /**
   * Return the dominator of v.
   */
  public final Chord getDominatorOf(Chord v)
  {
    Chord[] dom = domination.get(v);
    if (dom == null)
      return null;
    return dom[0];
  }

  private void setDominator(Chord me, Chord d)
  {
    Chord[] med = domination.get(me);
    if (med == null) {
      if (d == null)
        return;
      med = new Chord[2];
      domination.put(me, med);
    }
    med[0] = d; // Make d the dominator of me.
  }

  /**
   * Specify that node <code>d</code> dominates node <code>me</code>.
   * This method should only be used to update the domination
   * information after the dominators have been computed.
   */
  public final void addDominatee(Chord d, Chord me)
  {
    if (d == null)
      return;

    // Make me a dominatee of d.

    Chord[] dd = domination.get(d);
    if (dd == null) {
      dd = new Chord[2];
      domination.put(d, dd);
      dd[1] = me;
      return;
    }

    for (int i = 1; i < dd.length; i++) {
      Chord c = dd[i];
      if (c == me)
        return;
      if (c == null) {
        dd[i] = me;
        return;
      }
    }

    Chord[] nd = new Chord[dd.length + 4];
    System.arraycopy(dd, 0, nd, 0, dd.length);
    nd[dd.length] = me;
    domination.put(d, nd);
  }

  /**
   * Specify that node <code>d</code> no longer dominates node
   * <code>me</code>.  This method should only be used to update the
   * domination information after the dominators have been computed.
   */
  public final void removeDominatee(Chord d, Chord me)
  {
    if (d == null)
      return;

    // Remove me as a dominatee of d.

    Chord[] dd = domination.get(d);
    if (dd == null)
      return;

    for (int i = 1; i < dd.length; i++) {
      Chord c = dd[i];
      if (c == me) {
        int l = dd.length - i - 1;
        if (l > 0)
          System.arraycopy(dd, i + 1, dd, i, l);
        dd[dd.length - 1] = null;
        return;
      }
    }
  }

  /**
   * Return the set of CFG nodes that <code>dominator</code>
   * strictly dominates.
   */
  public final Chord[] getDominatees(Chord dominator)
  { 
    Chord[] dominated = domination.get(dominator);
    if (dominated == null)
      return new Chord[0];

    int cnt = 0;
    for (int i = 1; i < dominated.length; i++)
      if (dominated[i] != null)
        cnt++;

    int     k  = 0;
    Chord[] nd = new Chord[cnt];
    for (int i = 1; i < dominated.length; i++)
      if (dominated[i] != null)
        nd[k++] = dominated[i];

    return nd;
  }


  /**
   * Push onto the stack all of the nodes that <code>dominator</code>
   * strictly dominates.  The nodes are placed on the stack in
   * ascending order by the node's label.
   * @see Scribble#labelCFG
   */
  public final void pushDominatees(Chord dominator, Stack<Chord> wl)
  { 
    Chord[] dominated = domination.get(dominator);
    if (dominated == null)
      return;

    for (int i = 1; i < dominated.length - 1; i++) {
      Chord f = dominated[i];
      if (f == null)
        break;
      for (int j = i + 1; j < dominated.length; j++) {
        Chord s = dominated[j];
        if (s == null)
          break;
        if (f.getLabel() > s.getLabel()) {
          dominated[j] = f;
          dominated[i] = s;
          f = s;
        }
      }
    }

    for (int i = 1; i < dominated.length; i++) {
      Chord d = dominated[i];
      if (d == null)
        break;
      wl.push(d);
    }
  }

  /**
   * Return the number of the nodes that <code>dominator</code> strictly
   * dominates.
   */
  public final int numDominatees(Chord dominator)
  { 
    Chord[] dominated = domination.get(dominator);
    if (dominated == null)
      return 0;

    int num = 0;
    for (int i = 1; i < dominated.length; i++) {
      Chord d = dominated[i];
      if (d == null)
        break;
      num++;
    }
    return num;
  }

  /**
   * Return the set of all nodes dominated by node <code>n</code> and
   * all nodes dominated by nodes dominated by <code>n</code>, and so
   * on.  However, don't go past a non-pure subroutine call.
   * <p>
   * Consider the case where a subroutine call exists inside of a loop
   * dominated by <code>c</code>.  In this case, all of the nodes in
   * the loop must be eliminated from the set returned.
   * <p>
   * We can go to the ends of the CFG as long as there is no join.
   * For example, in
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     y = abc();
   *  (4)   y = g + 4
   * </pre>
   * even though line (1) dominates line (4), the call to abc on line
   * (3) prevents the nodes of line (4) from being included in the
   * same set.  However, in
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     return x;
   *  (4)   y = g + 4
   * </pre>
   * line (4) can be included because line (3) is an "end".
   * Note - In this case:
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     z = 123;
   *  (4)   y = g + 4
   * </pre>
   * line (4) is still not included because there is a join to line
   * (4) and our logic is not powerful enough to know that line(3) is
   * not a problem.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param dominator is the dominator node
   * @param idom is the vector to which the dominated nodes are added
   */
  public void getIterativeDominationNF(Chord dominator, Vector<Chord> idom)
  {
    getIterativeDominationNF(dominator, idom, true, null);
  }

  /**
   * Return the set of all nodes dominated by node <code>c</code> and
   * all nodes dominated by nodes dominated by <code>c</code>, and so
   * on.  However, don't go past a non-pure subroutine call or one of
   * the nodes specified by the <code>stops</code>.
   * <p>
   * Consider the case where a subroutine call exists inside of a loop
   * dominated by <code>c</code>.  In this case, all of the nodes in
   * the loop must be eliminated from the set returned.
   * <p>
   * We can go to the ends of the CFG as long as there is no join.
   * For example, in
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     y = abc();
   *  (4)   y = g + 4
   * </pre>
   * even though line (1) dominates line (4), the call to abc on line (3)
   * prevents the nodes of line (4) from being included in the same set.
   * However, in
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     return x;
   *  (4)   y = g + 4
   * </pre>
   * line (4) can be included because line (3) is an "end".
   * Note - In this case:
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     z = 123;
   *  (4)   y = g + 4
   * </pre>
   * line (4) is still not included because there is a join to line
   * (4) and our logic is not powerful enough to know that line(3) is
   * not a problem.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param dominator is the dominator node
   * @param idom is the vector to which the dominated nodes are added
   * @param stops is the list of nodes that should terminate the collection
   */
  public void getIterativeDominationNF(Chord          dominator,
                                       Vector<Chord>  idom,
                                       HashSet<Chord> stops)
  {
    getIterativeDominationNF(dominator, idom, true, stops);
  }

  /**
   * Return the set of all nodes dominated by node <code>c</code> and
   * all nodes dominated by nodes dominated by <code>c</code>, and so
   * on.  If specified, do not go past a non-pure subroutine call.  Do
   * not go past one of the nodes specified by the <code>stops</code>.
   * <p>
   * Consider the case where a subroutine call exists inside of a loop
   * dominated by <code>c</code>.  In this case, all of the nodes in
   * the loop must be eliminated from the set returned.
   * <p>
   * We can go to the ends of the CFG as long as there is no join.
   * For example, in
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     y = abc();
   *  (4)   y = g + 4
   * </pre>
   * even though line (1) dominates line (4), the call to abc on line (3)
   * prevents the nodes of line (4) from being included in the same set.
   * However, in
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     return x;
   *  (4)   y = g + 4
   * </pre>
   * line (4) can be included because line (3) is an "end".
   * Note - In this case:
   * <pre>
   *  (1)   x = g + 3;
   *  (2)   if (exp)
   *  (3)     z = 123;
   *  (4)   y = g + 4
   * </pre>
   * line (4) is still not included because there is a join to line
   * (4) and our logic is not powerful enough to know that line(3) is
   * not a problem.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   * @param dominator is the dominator node
   * @param idom is the vector to which the dominated nodes are added
   * @param calls is true if a call to a non-pure function should terminate
   * @param stops is the list of nodes that should terminate the collection
  */
  public void getIterativeDominationNF(Chord          dominator,
                                       Vector<Chord>  idom,
                                       boolean        calls,
                                       HashSet<Chord> stops)
  {
    if (calls && (dominator.getCall(true) != null))
        return;

    if ((stops != null) && stops.contains(dominator))
      return;

    if (dominator.isLoopPreHeader()) {
      Chord.nextVisit();
      if (getIterativeDominationLH((LoopPreHeaderChord) dominator, idom, calls, stops)) {
        LoopHeaderChord lh = (LoopHeaderChord) dominator.getNextChord();
        int             l  = lh.numLoopExits();
        for (int i = 0; i < l; i++) {
          LoopExitChord lex = lh.getLoopExit(i);
          if (lex != null)
            getIterativeDominationNF(lex, idom, calls, stops);
        }
      }
      return;
    }

    Chord[] dom = domination.get(dominator);
    if (dom == null)
      return;

    int start = idom.size();

    addDominated(dominator, dom, idom, calls, stops);

    Chord.nextVisit();

    for (int j = start; j < idom.size(); j++) {
      Chord c = idom.elementAt(j);
      if (c.visited())
        continue;

      if (calls && (c.getCall(true) != null)) {
        c.setVisited();
        continue;
      }

      if ((stops != null) && stops.contains(c)) {
        c.setVisited();
        idom.removeElementAt(j);
        j--;
        continue;
      }

      if (c.isLoopPreHeader()) {
        c.setVisited();
        if (getIterativeDominationLH((LoopPreHeaderChord) c, idom, calls, stops)) {
          LoopHeaderChord lh = (LoopHeaderChord) c.getNextChord();
          int             l  = lh.numLoopExits();
          for (int i = 0; i < l; i++) {
            LoopExitChord lex = lh.getLoopExit(i);
            if (lex != null)
              idom.addElement(lex);
          }
        } else {
          idom.removeElementAt(j);
          j--;
        }

        continue;
      }

      c.setVisited();
      Chord[] dominated = domination.get(c);
      if (dominated == null)
        continue;

      addDominated(c, dominated, idom, calls, stops);
    }
  }

  private void addDominated(Chord          dominator,
                            Chord[]        dom,
                            Vector<Chord>  idom,
                            boolean        calls,
                            HashSet<Chord> stops)
  {
    if (!calls && (stops == null)) {
      for (int i = 1; i < dom.length; i++) {
        Chord c = dom[i];
        if (c == null)
          continue;
        idom.addElement(c);
      }
      return;
    }

    // Try to determine if a join has any call (or stops) on a path
    // from its dominator to it. If it does, we can't add it to the
    // list of dominated nodes.  See note in above javadoc for
    // getIterativeDominationNF.

    Stack<Chord>   wl   = null;
    HashSet<Chord> done = null;

    outer:
    for (int i = 1; i < dom.length; i++) {
      Chord c = dom[i];
      if (c == null)
        continue;

      // Scan backwards to the dominator looking for calls, etc.

      if (wl == null) {
        wl = WorkArea.<Chord>getStack("addDominated");
        done = WorkArea.<Chord>getSet("addDominated");
      } else {
        wl.clear();
        done.clear();
      }

      c.pushInCfgEdges(wl, done);
      while(!wl.empty()) {
        Chord p = wl.pop();
        if (p == dominator)
          continue;
        if ((calls && (p.getCall(true) != null)) ||
            ((stops != null) && stops.contains(p)))
          continue outer;
        p.pushInCfgEdges(wl, done);
      }

      // No calls found - add it to the list of dominated nodes.

      idom.addElement(c);
    }

    if (wl != null) {
      WorkArea.<Chord>returnStack(wl);
      WorkArea.<Chord>returnSet(done);
    }
  }

  /**
   * Return true if the entire loop is dominated by the loop
   * pre-header without interference from calls, etc.
   * It is necessary to do a
   * <pre>
   *   Chord.nextVisit();
   * </pre>
   * before calling this method.
   * @param lph is the dominator node
   * @param idom is the vector to which the dominated nodes are added
   * @param calls is true if non-pure calls should be checked
   * @param stops is the list of nodes that should terminate the
   * collection
   */
  public boolean getIterativeDominationLH(LoopPreHeaderChord lph,
                                          Vector<Chord>      idom,
                                          boolean            calls,
                                          HashSet<Chord>     stops)
  {
    LoopHeaderChord lh  = (LoopHeaderChord) lph.getNextChord();
    Chord[]         dom = domination.get(lh);
    if (dom == null)
      return false;

    int start = idom.size();

    idom.add(lh);

    addDominated(lh, dom, idom, calls, stops);

    for (int j = start; j < idom.size(); j++) {
      Chord c = idom.elementAt(j);
      if (c == null) {
        idom.removeElementAt(j);
        j--;
        continue;
      }

      if (c.visited())
        continue;

      if (calls && (c.getCall(true) != null)) {
        idom.setSize(start);
        return false;
      }

      if ((stops != null) && stops.contains(c)) {
        idom.setSize(start);
        return false;
      }

      if (c.isLoopPreHeader()) {
        c.setVisited();
        if (getIterativeDominationLH((LoopPreHeaderChord) c, idom, calls, stops)) {
          LoopHeaderChord lh2 = (LoopHeaderChord) c.getNextChord();
          int             l   = lh2.numLoopExits();
          for (int i = 0; i < l; i++) {
            LoopExitChord lex = lh2.getLoopExit(i);
            if (lex != null)
              idom.addElement(lex);
          }
          continue;
        } else {
          idom.setSize(start);
          return false;
        }
      }

      if (c.isLoopExit()) {
        LoopExitChord ex = (LoopExitChord) c;
        if (ex.getLoopHeader() == lh)
          continue; // Leave it for my caller to process.
      }

      c.setVisited();
      Chord[] d = domination.get(c);
      if (d == null)
        continue;

      addDominated(c, d, idom, calls, stops);
    }

    return true;
  }

  /**
   * Return the set of all nodes strictly dominated by node
   * <code>n</code> and all nodes dominated by nodes dominated by
   * <code>n</code>, and so on.
   * @param n is the dominator node
   */
  public final Vector<Chord> getIterativeDomination(Chord n)
  {
    Vector<Chord>  v   = new Vector<Chord>(20);
    Chord[]        dom = domination.get(n);
    if (dom == null)
      return v;

    for (int i = 1; i < dom.length; i++)
      v.addElement(dom[i]);
    
    for (int j = 0; j < v.size(); j++) {
      Chord c = v.elementAt(j);
      if (c == null) {
        v.removeElementAt(j);
        j--;
        continue;
      }

      Chord[] d = domination.get(c);
      if (d == null)
        continue;

      for (int i = 1; i < d.length; i++)
        v.addElement(d[i]);
    }

    return v;
  }

  /**
   * Return the set of all nodes strictly dominated by node
   * <code>n</code> and all nodes dominated by nodes dominated by
   * <code>n</code>, and so on.
   * @param n is the dominator node
   * @param v is the vector to which the dominated nodes are added
   */
  public void getIterativeDomination(Chord n, Vector<Chord> v)
  {
    int   start = v.size();
    Chord[] dom = domination.get(n);
    if (dom == null)
      return;

    for (int i = 1; i < dom.length; i++)
      v.addElement(dom[i]);
    
    for (int j = start; j < v.size(); j++) {
      Chord c = v.elementAt(j);
      if (c == null) {
        v.removeElementAt(j);
        j--;
        continue;
      }

      Chord[] d = domination.get(c);
      if (d == null)
        continue;

      for (int i = 1; i < d.length; i++)
        v.addElement(d[i]);
    }
  }

  /**
   * Return true if CFG node <code>n</code> strictly dominates node
   * <code>d</code>.
   * @param n the node to test
   * @param d the node to test
   */
  public final boolean inDominatees(Chord n, Chord d)
  {
    Chord[] dom = domination.get(n);
    if (dom == null)
      return false;

    for (int i = 1; i < dom.length; i++) {
      if (d == dom[i])
        return true;
    }
    return false;
  }

  /**
   * Return true if CFG node <code>n</code> dominates node
   * <code>d</code>.
   * @param n the node to test
   * @param d the node to test
   */
  public final boolean inIterativeDominatees(Chord n, Chord d)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("inIterativeDominatees");

    wl.push(n);

    while (!wl.empty()) {
      Chord   x   = wl.pop();
      Chord[] dom = domination.get(x);
      if (dom == null)
        continue;

      for (int i = 1; i < dom.length; i++) {
        Chord xx = dom[i];
        if (xx == null)
          continue;

        if (d == xx) {
          WorkArea.<Chord>returnStack(wl);
          return true;
        }

        wl.push(xx);
      }
    }

    WorkArea.<Chord>returnStack(wl);

    return false;
  }

  /**
   * Print out the dominance relations for node <code>n</code>.
   * @param s the stream to write to
   */
  public final void displayDominance(java.io.PrintStream s, Chord n)
  {
    Chord[] dom = domination.get(n);
    if (dom == null)
      return;

    s.print(n);
    s.print(" is dominated by ");
    s.print(dom[0]);
    s.println(" and dominates ");
    for (int i = 1; i < dom.length; i++) {
      Chord d = dom[i];
      if (d == null)
        continue;

      s.print("   ");
      s.println(d);
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
  /**
   * Removes all nodes that do not have a dominator.
   * If a node is found that has no dominator, it and all of its
   * children in the dominator tree are removed from the CFG.
   */
  public final void removeDeadCode()
  {
    if (vertex == null)
      return;

    HashSet<Chord> remove = WorkArea.<Chord>getSet("removeDeadCode");

    for (int i = 1; i < vertex.length; i++) {
      Chord d = vertex[i];
      if ((d != null) && (getDominatorOf(d) == null)) {
        vertex[i] = null;
        markForRemoval(d, remove);
      }
    }

    Iterator<Chord> e = remove.iterator();
    while (e.hasNext()) {
      Chord dead = e.next();
      dead.expungeFromCfg(); // No need to maintain graph integrity since everything that is connected is deleted.
      deadCFGNodeCount++;
    }

    for (int j = 1; j < vertex.length; j++) {
      Chord d = vertex[j];
      if ((d != null) && remove.contains(d))
        vertex[j] = null;
    }

    WorkArea.<Chord>returnSet(remove);
    vertex = null;
  }

  private void markForRemoval(Chord d, HashSet<Chord> remove)
  {
    remove.add(d);

    Chord[] dom = domination.get(d);
    if (dom == null)
      return;

    for (int i = 1; i < dom.length; i++)
      if (dom[i] != null)
        markForRemoval(dom[i], remove);
  }

  public String toStringSpecial()
  {
    return post ? "post" : "";
  }
}
