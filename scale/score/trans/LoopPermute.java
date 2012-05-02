package scale.score.trans;

import java.util.Enumeration;
import java.util.Iterator;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.dependence.*;
import scale.score.pred.References;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.IntLiteral;
 
/**
 * This optimization permutes the loops of a routine based on cache
 * order.
 * $Id: LoopPermute.java,v 1.41 2007-10-04 19:58:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <p>
 * See <cite>Improving Data Locality with Loop Transformation</cite>
 * by Kathryn S. McKinley, Steve Carr, and Chau-wen Tseng, ACM
 * Transactions on Programming Languages and Systems, 18(4):424-453,
 * July 1996.
 */
public class LoopPermute extends LoopTrans
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  private DDGraph graph = null;

  /**
   * @param scribble is the CFG containing the loops
   */
  public LoopPermute(Scribble scribble)
  {
    super(scribble, "_lp");
    assert setTrace(classTrace);
  }

  public void perform()
  {
    if (trace)
      System.out.println("** LP " + scribble.getRoutineDecl().getName());

    LoopHeaderChord         lt         = scribble.getLoopTree();
    Vector<LoopHeaderChord> innerLoops = lt.getInnerLoops();

    for (int li = 0; li < innerLoops.size(); li++) {
      LoopHeaderChord loop = innerLoops.elementAt(li);   
      InductionVar    ivar = loop.getPrimaryInductionVar();

      if (trace)
        System.out.println("   lp " + loop.nestedLevel() + " " + loop);
 
      if ((ivar == null) || !loop.isPerfectlyNested()) {
        innerLoops.addVectors(loop.getInnerLoops());
        continue;
      }

      if (loop.nestedLevel() < 2) // no need to check permutation for a simple nest
        continue;

      tryPermute(loop);
    }
  }

  private void tryPermute(LoopHeaderChord topLoop)
  {
    Vector<LoopHeaderChord> loopNest = topLoop.getTightlyNestedLoops();
    if (loopNest == null)
      return;

    int             loopDepth = loopNest.size();            
    LoopHeaderChord bottom    = loopNest.get(loopDepth - 1);
    if (!unsafe && !legalLoop(bottom))
      return;

    // Set the loop costs for the loops in a loop nest. The cost for a loop is
    // the cost of executing the nest with that loop in the innermost nesting.
  
    Table<Declaration, SubscriptExpr> arrayRefs = new Table<Declaration, SubscriptExpr>();
    if (!topLoop.getSubscriptsRecursive(arrayRefs))
      return;

    graph = topLoop.getDDGraph(false);
    if (graph == null)
      return;

    if (trace)
      System.out.println("     " + graph);

    int[]  loopIndex    = new int[loopDepth];
    Cost[] loopCostList = new Cost[loopDepth]; 
    Vector<RefGroup> refGroups = new Vector<RefGroup>(20);

    for (int i = 0; i < loopDepth; i++) {
      LoopHeaderChord loop = loopNest.elementAt(i);
      if (trace)
        System.out.println("   " + i + " " + loop);

      computeRefGroups(loop.getNestedLevel(), 2, 2, arrayRefs, refGroups);

      Cost lc = computeRefCostSum(loop, refGroups);
      if (trace)
        System.out.println("   " + i + " " + lc);

      loopCostList[i] = lc;
      loopIndex[i]    = i; //the outtermost loop is at position 0

      Cost tp = tripProduct(loopNest, loop);
      if (trace)
        System.out.println("   " + i + " " + tp);
      lc.multiply(tp);
      if (trace)
        System.out.println("   " + i + " " + lc);
    }

    boolean permuted = sortByCost(loopCostList, loopIndex);

    if (!permuted)
      return;
    if (trace) {
      System.out.print("   permute " + loopDepth);
      System.out.print(":");
      for (int i = 0; i < loopIndex.length; i++)
        System.out.print(" " + loopIndex[i]);
      System.out.println("");
    }

    int[][] ddVec = getDDVec(arrayRefs, loopDepth);
    if (trace)
      printDDInfo(ddVec, loopDepth);

                
    if (!isLegal(loopIndex, ddVec))
      return;

    if (trace)
      System.out.println("   permute " + loopDepth);

    int[] rank = new int[loopDepth];

    // We will do sorting on the rank vector, which corresponds to the interchange we need.

    for (int i = 0; i < loopDepth; i++) {
      int loopNum = loopIndex[i];
      rank[loopNum] = i;
    }
            
    if (trace)
      printOrder(rank);

    boolean changed  = true;

    while (changed) {
      changed = false;

      for (int i = 0; i < loopDepth - 1; i++) {
        int j         = i + 1;
        int outerRank = rank[i];
        int innerRank = rank[j];

        if (innerRank >= outerRank)
          continue;

        LoopHeaderChord innerLoop = loopNest.elementAt(j);
        LoopHeaderChord outerLoop = loopNest.elementAt(i);

        if (!outerLoop.isDDComplete() || outerLoop.inhibitLoopPermute())
          continue;

        if (!innerLoop.isDDComplete() || innerLoop.inhibitLoopPermute())
          continue;

        changed = true;

        rank[i] = innerRank;
        rank[j] = outerRank;

        loopNest.setElementAt(innerLoop, i);
        loopNest.setElementAt(outerLoop, j);

        performLoopInterchange(innerLoop, outerLoop);
      }
    }
  }

  private boolean sortByCost(Cost[] loopCostList, int[] loopIndex)
  {
    int     maxlooptemp = loopCostList.length - 1;
    boolean exchanged   = true;
    boolean permuted    = false;

    while (exchanged) { // Bubble sort the LoopCosts.
      exchanged = false;
                  
      for (int i = 0; i < maxlooptemp; i++) {
        int  j    = i + 1;
        Cost lci  = loopCostList[i];
        Cost lci1 = loopCostList[j];
        if (lci1 == null)
          continue;

        if (lci.lessThan(lci1)) {
          int temp = loopIndex[i];
          loopIndex[i] = loopIndex[j];
          loopIndex[j] = temp;
                        
          loopCostList[i] = lci1;
          loopCostList[j] = lci;
          exchanged = true;
        }
      }

      permuted |= exchanged;

      maxlooptemp--;
    }

    return permuted;
  }

  private void printDDInfo(int[][] ddVec, int loopDepth)
  {
    System.out.println("** DD Info ");
    for (int di = 0; di < ddVec.length; di++) {
      System.out.print(di);
      for (int dj = 0; dj < loopDepth; dj++) {
        System.out.print(" ");
        System.out.print(ddVec[di][dj]);
      }
      System.out.println();
    }
  }

  private void printOrder(int[] rank)
  {
    System.out.print("** The final order is");
    for (int i = 0; i < rank.length; i++) {
      System.out.print(" ");
      System.out.print(rank[i]);
    }
    System.out.println();
  }

  private static class RefGroup
  {
    private String aname;
    private Vector<Object> refs;

    public RefGroup(String name, SubscriptExpr r)
    {
      this.refs  = new Vector<Object>();
      this.aname = name;

      refs.addElement(r);
    }

    public void add(DDEdge edge)
    {
      if (refs.contains(edge))
        return;

      refs.addElement(edge);
    }

    public void add(RefGroup r)
    {
      int l = r.refs.size();
      for (int i = 0; i < l; i++) {
        Object ref = r.refs.elementAt(i);
        if (!refs.contains(ref))
          refs.addElement(ref);
      }
    }

    public String getName()
    {
      return aname;
    }

    public SubscriptExpr getRepresentative()
    {
      if (refs.size() > 0)
        return (SubscriptExpr) refs.elementAt(0);

      return null;
    }

    public boolean contains(Expr d)
    {
      return refs.contains(d);
    }

    public boolean contains(RefGroup r)
    {
      int l = r.refs.size();

      for (int k = 0; k < l; k++)
        if (refs.contains(r.refs.elementAt(k)))
          return true;

      return false;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("{RG ");
      int          len = refs.size();
      if (len > 3)
        len = 3;
      for (int i = 0; i < len; i++) {
        if (i > 0)
          buf.append(',');
        buf.append(refs.elementAt(i));
      }
      if (len < refs.size())
        buf.append(", ...");
      buf.append('}');
      return buf.toString();
    }
  }
  
  private void printRefGroups(Vector<RefGroup> refGroups)
  {
    for (int i = 0; i < refGroups.size(); i++) {
      RefGroup rg = refGroups.elementAt(i);
      System.out.println(rg);
    }
  }

  /**
   *  Compute the reference groups for this loop. 
   *  Two array references are in the same group with respect to this loop if
   *  - there is a loop-independent dependence between them, or 
   *  - the dependence distance(dependence vector entry dl) for this loop is 
   *    less than some constant *dist*, and all other dependence vector entries
   *    are 0.
   *  - the two array refer to the same array and differ by at most *dist2* in the
   *    first subscript dimension, where d is less than or equal to the cache
   *    line size in terms of array elements. All other subscripts must be
   *    identical.
   *  Notes: Here we assume dist1 = dist2 = 2
   */
  private void computeRefGroups(int level,
                                int dist1,
                                int dist2,
                                Table<Declaration, SubscriptExpr> arrayRefs,
                                Vector<RefGroup>   refGroups)
  {
    if (arrayRefs == null)
      return;

    Enumeration<Declaration> ek = arrayRefs.keys();
    while (ek.hasMoreElements()) {
      VariableDecl vd = (VariableDecl) ek.nextElement();
      String       s  = vd.getName(); // array name
      Object[]     v  = arrayRefs.getRowArray(vd);    // vector of SubscriptExpr's
      int          vi = v.length;

      for (int j = vi - 1; j >= 0; j--) {
        SubscriptExpr           sr              = (SubscriptExpr) v[j];
        Vector<LoopHeaderChord> allRelatedLoops = sr.allRelatedLoops(); // ** Incorrect when something like a[(j+i][j]
        int                     arls            = allRelatedLoops.size();
        int                     firstsub        = arls - 1; // ** Making an invalid assumption here

        // Process the list of references r' with which r has a data
        // dependence, and  r is the source(data flows from r to r').

        RefGroup rg    = new RefGroup(s, sr);
        Object[] edges = graph.getEdges(sr);
        int      len   = edges.length;

        for (int i = 0; i < len; i++) {
          DDEdge edge = (DDEdge) edges[i];

          if (edge.isSpatial())
            continue;

          // Condition(1)-(a) in McKinley's paper

          if (edge.isLoopIndependentDependency()) { // add rP to the RefGroup of r:
            rg.add(edge);
            continue;
          }

          // Condition(1)-(b) in McKinley's paper

          computeEdgeRefs(edge, rg, level, dist1);

          if (arls <= 0)
            continue;

          // Condition(2) in McKinley's paper
          // rlevel is the level of the loop related to the first subscript.

          int rlevel = allRelatedLoops.elementAt(firstsub).getNestedLevel();
                      
          computeEdgeRefs(edge, rg, rlevel, dist2);
        }

        boolean isInExistingRefGroups = false;
        int     rgl                   = refGroups.size();
        for (int i = 0; i < rgl; i++) {
          RefGroup rg2 = refGroups.elementAt(i);
          if (!rg2.getName().equals(s))
            continue;
                
          isInExistingRefGroups = rg2.contains(rg);
                
          if (isInExistingRefGroups) {
            rg2.add(rg);
            break;
          }
        }
            
        if (!isInExistingRefGroups)
          refGroups.addElement(rg);
      }
    }
  }

  private void computeEdgeRefs(DDEdge edge, RefGroup rg, int level, int dist)
  {
    if (!edge.isDistanceKnown(level))
      return;

    int distance = edge.getDistance(level);
    if (distance < 0)
      distance = -distance;

    if (distance > dist)
      return;

    long[] ddinfo = edge.getDDInfo();
    for (int k = 0; k < ddinfo.length; k++)
      if ((k != level) &&
          DDInfo.isDistanceKnown(ddinfo[k]) &&
          (DDInfo.getDistance(ddinfo[k]) != 0)) {
        rg.add(edge);
        break;
      }
  }

  private Cost computeRefCostSum(LoopHeaderChord  loop,
                                 Vector<RefGroup> refGroups)
  {
    Cost lc = new Cost();
    int  l  = refGroups.size();
    for (int i = 0; i < l; i++) {
      RefGroup      rg   = refGroups.elementAt(i);
      SubscriptExpr se   = rg.getRepresentative();
      Cost          cost = computeRefCost(loop, se);

      if (cost == null)
        continue;

      lc.add(cost);
    }
    return lc;
  }

  private int[][] getDDVec(Table<Declaration, SubscriptExpr> arrayRefs, int loopDepth)
  {
    if (arrayRefs == null)
      return new int[0][0];

    int           numEdges = 0;
    Stack<DDEdge> wl       = WorkArea.<DDEdge>getStack("g<SubscriptExpr>etDDVec");

    Enumeration<DDEdge> k = graph.getEdges();
    while (k.hasMoreElements()) { // Check out every array variable in the loop nest.
      DDEdge edge = k.nextElement();
      if (edge.isSpatial())
        continue;

      if (edge.isLoopIndependentDependency())
        continue;

      if (edge.representsAllInput())
        continue;

      wl.push(edge);
      numEdges++;
    }

    int[][] DDVector = new int[numEdges][loopDepth];

    int i = 0;
    while (!wl.empty()) {
      DDEdge edge = wl.pop();
      if (trace)
        System.out.println("   edge " + edge);

      // Find dependence distance.

      for (int level = 1; level <= loopDepth; level++)
        DDVector[i][level - 1] = edge.getDistance(level);

      if (trace) {
        System.out.print(i); 
        System.out.print(" "); 
        System.out.println(edge); 
      }

      i++;
    }

    WorkArea.<DDEdge>returnStack(wl);

    return DDVector;
  }

  public Cost computeRefCost(LoopHeaderChord loop, SubscriptExpr se)
  {
    Cost         tripL = loop.getTripCount();
    InductionVar ivar  = loop.getPrimaryInductionVar();
    if (ivar == null)
      return tripL;

    // Compute 
    // tripL = (ubL - lbL + stepL) / stepL

    int             nsubs = se.numSubscripts(); //number of subscripts in the subscript
    LoopHeaderChord tloop = loop.getTopLoop();
    VariableDecl    iv    = ivar.getVar();

    for (int i = 0; i < nsubs - 1; i++) { // for c code
      // If the loop index variable of this loop appears in any of the
      // subscripts other then the last, then return tripL.
      Expr       sI = se.getSubscript(i);
      AffineExpr ae = tloop.isAffine(sI);

      if (ae == null)
        return null;

      if (ae.hasTerm(iv))
        return tripL;
    }

    int        fs = nsubs - 1;
    Expr       s0 = se.getSubscript(fs);
    AffineExpr ae = tloop.isAffine(s0);
    if (ae == null)
      return null;

    int          at    = ae.getTermIndexOrig(iv);
    long         coeff = 0;
    if (at >= 0)
      coeff = ae.getCoefficient(at);
    
    if (coeff == 0) // Invariant Reuse.
      return new Cost(1.0, 0);
   
    long stepL  = loop.getStepValue();
    long stride = stepL * coeff;
    if (stride < 0)
      stride = -stride;

    // Unit Reuse.

    Type et = se.getCoreType().getPointedTo();
    int  bs = et.memorySizeAsInt(Machine.currentMachine);
    int  cs = Machine.currentMachine.getCacheSize(bs);
    if (stride <= cs) { // cache line or block size
      tripL.multiply(stride);
      tripL.divide(cs);
      return tripL;
    }

    // No Reuse.

    return tripL;
  }

  private boolean isLegal(int[] loopIndex, int[][] ddVec)
  {
    if ((ddVec == null) || (ddVec.length == 0))
      return true;

    boolean[] legalSafe = new boolean[ddVec.length];
    int       loopDepth = ddVec[0].length;
    for (int i = 0; i < loopDepth; i++) {
      int level = loopIndex[i];
      for (int j = 0; j < ddVec.length; j++) {
        if (legalSafe[j])
          continue;

        if (ddVec[j][level] == 0)
          continue;

        if (ddVec[j][level] > 0) {
          legalSafe[j] = true;
          continue;
        }

        for (int k = 0; k < ddVec[j].length; k++)
          if ((k != level) && (ddVec[j][k] != 0))
            return false;
      }
    }

    return true;
  }

  /**
   * Return true if this is a legal loop.  A legal loop contains no
   * function calls and has no scalar variable cycles.  A cycle exists
   * when the variable is referenced before it is defed.  We go to
   * some trouble to allow loops containing cycles such as
   * <pre>
   *   s = s + a(i,j)
   * </pre>
   * to be permuted.
   */
  private boolean legalLoop(LoopHeaderChord loop)
  {
    Stack<Chord> wl   = WorkArea.<Chord>getStack("legalLoop");
    References   refs = scribble.getRefs();

    Chord.nextVisit();
    wl.push(loop);
    loop.setVisited();

    int n = loop.numLoopExits();
    for (int i = 0; i < n; i++)
      loop.getLoopExit(i).setVisited();

    boolean legal = true;

    outer:
    while (!wl.empty()) {
      Chord c = wl.pop();

      if (c.getCall(true) != null) {
        legal = false;
        break;
      }

      if ((c instanceof DecisionChord) && (c != loop.getLoopTest())) {
        legal = false;
        break;
      }

      if (c.isAssignChord() && !c.isPhiExpr()) {
        ExprChord ec  = (ExprChord) c;
        Expr      lhs = ec.getLValue();
        Expr      rhs = ec.getRValue();

        // The variable is both defed and used in the loop and
        // we don't know how it is used.  For example, it could be
        //   s = s + 1
        //   c(i,j) = s
        // or
        //   c(i,j) = s
        //   s = s + 1
        // We want to allow
        //   s = s + c(i,j)
        // because we know that s is not used to specify the
        // value of an array element.  We want to allow
        //   s = ...
        //     = s
        // since there is no cycle.

        if (lhs instanceof LoadDeclAddressExpr) {
          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
          VariableDecl        vd   = ldae.getDecl().returnVariableDecl();
          if ((vd != null) && !loop.isLoopIndex(vd)) {
            boolean         cycle = false;
            Iterator<Chord> it1   = refs.getUseChords(vd);
            while (it1.hasNext()) {
              Chord s = it1.next();
              cycle |= (s == c);
            }

            Iterator<Chord> it2 = refs.getUseChords(vd);
            while (it2.hasNext()) {
              Chord s = it2.next();
              if (c == s)
                continue;

              if (s.getLoopHeader() != loop)
                continue;

              if (cycle) {
                // There was a cycle and another use.
                //   s = s + 1
                //     = s
                legal = false;
                break outer;
              }

              // Check for a use before the def.
              while (true) {
                s = s.getNextChord();
                if (s == null)
                  break;
                if (s == c) {
                  legal = false;
                  break outer;
                }
              }
            }
          }
        }
      }

      c.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);

    return legal;
  }

  /**
   * Return whether this optimization requires that the CFG be in SSA form.
   * It returns either
   * <dl>
   * <dt><b>NO_SSA</b><dd>the CFG must not be in SSA form,
   * <dt>IN_SSA<dd>the CFG must be in SSA form,
   * <dt>VALID_SSA<dd>the CFG must be in valid SSA form, or
   * <dt>NA_SSA<dd>the CFG does not need to be in valid SSA form.
   * </dl>
   */
  public int requiresSSA()
  {
    return NO_SSA;
  }
}
