package scale.score.trans;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.AbstractCollection;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.pred.*;
import scale.score.dependence.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.IntLiteral;

/**
 * This class replaces references to array elements with references to
 * scalar variables.  If array elements are set, the CFG is no longer
 * in a completely valid SSA form.
 * <p>
 * $Id: ScalarReplacement.java,v 1.74 2007-10-18 17:01:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <PRE>
 * For each array s
 *   Sort subscript expressions in control dependence order
 *   For each subscript expression r1 of s
 *     For each remaining subscript expression r2
           of s in r1's iterated dominance domain
 *       If all edges from r1 to r2 have zero distance
 *         Add edges from r1 to r2 to list of edges
 *     End for each remaining r2 of s
 *     Allocate new variable vd
 *     For each def-use link from r1
 *       Sort in control dependence order
 *       Replace first load with a load into vd.
 *       Replace all remaining loads with a load of vd.
 *       For each store
 *         Replace array reference with reference to vd
 *         Insert a store from vd to the array and record this store.
 *       End for each store
 *     End for each def-use link
 *     For each data dependence edge in list of edges
 *       Switch on edge type
 *         case input, flow
 *           For each def-use link from r2
 *             Sort in control dependence order
 *             Replace first load with a load into vd.
 *             Replace all remaining loads with a load of vd.
 *             For each store
 *               Replace array reference with reference to vd
 *               Insert a store from vd to the array and record this store.
 *             End each store
 *           End each def-use link
 *         End case
 *         case anti, output
 *           For each def-use link from r2
 *             Sort in control dependence order
 *             For each store
 *               Replace array reference with reference to vd
 *               Insert a store from vd to the array and record this store.
 *             End each store
 *           End each def-use
 *         End case
 *       End switch
 *     End for each data dependence edge
 *     For each recorded store s1
 *       If there exists a straight line control path to another recorded store s2
 *         remove s1
 *     End for each recorded store
 *   End for each r1 of s
 * End for each array
 * </pre>
 */

public class ScalarReplacement extends Optimization
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * If true, use heuristics that prune the cases where the
   * optimization is applied.
   */
  public static boolean useHeuristics = true;
  /**
   * If true, data dependence analysis is performed for inner loops only.
   * For 
   * <pre>
   *    do 10 i = 1, N
   *      do 20 j = 1, M
   *        ...
   *        = x(i)
   *        ...
   * 20 continue
   *      do 20 j = 1, M
   *        ...
   *        = x(i)
   *        ...
   * 30 continue
   * 10 continue
   * </pre>
   * setting innerLoopsOnly to <code>false</code> will enable scalar
   * replacement to use one scalar variable for both references to
   * <code>x</code>.  It may also greatly increase the time that the
   * scalar replacement optimization takes.
   */
  public static boolean innerLoopsOnly = false;

  private static int replacedLoadCount  = 0;
  private static int replacedStoreCount = 0;
  private static int newCFGNodeCount    = 0;

  private static final String[] stats = {
    "replacedLoads",
    "replacedStores",
    "newCFGNodes"};

  static
  {
    Statistics.register("scale.score.trans.ScalarReplacement", stats);
  }

  /**
   * Return the current number of array loads replaced.
   */
  public static int replacedLoads()
  {
    return replacedLoadCount;
  }

  /**
   * Return the current number of array stores replaced.
   */
  public static int replacedStores()
  {
    return replacedStoreCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  private Domination             dom;
  private DDGraph                graph;
  private HashSet<SubscriptExpr> done;
  private HashSet<Note>          sinks;
  private HashSet<Chord>         chordsInLoop;
  private HashSet<Chord>         chordsToMove;
  private Vector<ExprChord>      stores;                  // List of stores into the arrays that are generated.
  private Vector<Chord>          idom;
  private Vector<Note>           chks;

  private boolean invariantsOK;               // Loop contains no calls.
  private boolean loopHasUnsafeVarReferences; // Loop references address-taken variables.

  public ScalarReplacement(Scribble scribble)
  {
    super(scribble, "_sr");
    assert setTrace(classTrace);
  }

  public void perform()
  {
    // Initialize everything in preparation for scalar replacement.

    int olc = replacedLoadCount;
    int osc = replacedStoreCount;

    dom          = scribble.getDomination(); // Must be done before labeling.
    done         = WorkArea.<SubscriptExpr>getSet("perform ScalarReplacement");
    sinks        = WorkArea.<Note>getSet("perform ScalarReplacement");
    chordsInLoop = WorkArea.<Chord>getSet("perform ScalarReplacement");
    chordsToMove = WorkArea.<Chord>getSet("perform ScalarReplacement");
    stores       = new Vector<ExprChord>(10);
    idom         = new Vector<Chord>(10);
    chks         = new Vector<Note>(10);

    dChanged = false;

    if (innerLoopsOnly)
      processLoop(scribble.getLoopTree());
    else
      performScalarReplacement(scribble.getLoopTree());

    WorkArea.<SubscriptExpr>returnSet(done);
    WorkArea.<Note>returnSet(sinks);
    WorkArea.<Chord>returnSet(chordsInLoop);
    WorkArea.<Chord>returnSet(chordsToMove);

    int dl = replacedLoadCount - olc;
    int ds = replacedStoreCount - osc;
    assert assertTrace(trace && ((dl + ds) > 0),
                       "  Scalar Replacement loads " + dl + " stores " + ds, null);

    scribble.performedScalarReplacement();

    if (dChanged)
      scribble.recomputeDominators();
    if (rChanged)
      scribble.recomputeRefs();
  }

  private void processLoop(LoopHeaderChord loop)
  {
    Vector<LoopHeaderChord> innerLoops = loop.getInnerLoops();
    int                     ilcnt      = innerLoops.size();

    if (ilcnt == 0) {
      performScalarReplacement(loop);
      return;
    }

    for (int i = 0; i < ilcnt; i++)
      processLoop(innerLoops.elementAt(i));
  }

  /**
   * Do scalar replacement of array accesses of the specified loop.
   */
  private void performScalarReplacement(LoopHeaderChord loop)
  {
    loopHasUnsafeVarReferences = false;

    graph = loop.getDDGraph(false);
    if (graph == null)
      return;

    Table<Declaration, SubscriptExpr> arrayRefs = graph.getSubscriptsUsed();

    // If there are loop invariant array references, they can't be
    // moved out of the loop if the loop contains a function call.
    // It's just too hard to determine if the call would affect the
    // array.  If the array reference is loop invariant, then we need
    // to determine wheether the things it depends on are also loop
    // invariant because loop invariant array references are never
    // moved out of a loop by the loop invariant code motion (LICM)
    // optimization.

    invariantsOK = !loop.loopContainsCall() && loop.isTrueLoop();

    Enumeration<Declaration> ek = arrayRefs.keys();
    while (ek.hasMoreElements()) {
      VariableDecl arr = (VariableDecl) ek.nextElement();
      if (arr.hasHiddenAliases() || arr.hasHiddenPtrAliases())
        continue;

      Object[] v = pruneArrayRefs(arrayRefs, arr);
      if (v == null)
        continue;

      String        s   = arr.getName();
      SubscriptExpr rr1 = (SubscriptExpr) v[0];
      Type          pt1 = rr1.getCoreType().getPointedTo();
      ArrayType     at  = pt1.getCoreType().returnArrayType();

      if (at != null)
        pt1 = at.getElementType();

      Type          t1  = pt1.getCoreType();

      if (!t1.isAtomicType())
        continue; // No need to check any more subscripts of this array.

      // Place the references in execution order.

      Optimization.sort(v);

      int olca = replacedLoadCount;
      int osca = replacedStoreCount;

      done.clear();

      boolean didit = false;
      for (int i = 0; i < v.length; i++) {
        SubscriptExpr sr1 = (SubscriptExpr) v[i];

        if (!done.add(sr1))
          continue;

        didit |= processReference(loop, sr1);
      }

      assert assertTrace(trace && didit, "** " +
                         s +
                         " " +
                         (replacedLoadCount - olca) +
                         " " +
                         (replacedStoreCount - osca), null);
    }

    moveInvariants(loop.getPreHeader());

    chordsInLoop.clear();
    chordsToMove.clear();
  }

  private boolean processReference(LoopHeaderChord loop, SubscriptExpr sr1)
  {
    Chord sc1 = sr1.getChord();
    if (sc1 == null)
      return false;

    // If it has been moved out of the loop by a previous scalar
    // replacement, we don't want to try to do it again.

    if (sc1.getLabel() < loop.getLabel())
      return false;

    stores.clear();
    sinks.clear();
    idom.clear();

    sr1.addUses(sinks);

    // Sometimes the subscript expression is used only to obtain an
    // address that is used later.  When this happens we must insure
    // that we use the proper CFG node to obtain the iterative
    // domination set.  Otherwise we may generate an invalid memory
    // reference because it is placed before the guard code. See
    // fmul.c in 124.m88ksim.

    Chord fc = removeNAUses(sinks);
    if (fc == null)
      return false;

    // This is a patch.  The problem is that we want to use the first
    // valid ref of all the relevant subscript expressions but we do
    // not know which are relevant until we find the first valid
    // ref. See gcc torture test 20010224-1.c when inlined and
    // unrolled.

    if (fc.firstInBasicBlock() != sc1.firstInBasicBlock())
      sc1 = fc;

    DDEdge[] edges = graph.getEdges(sr1);

    // We must check for other uses of the array.  An array reference
    // is invariant if it can be safely moved outside of the loop.

    boolean invariant = false;
    if (invariantsOK && !loopHasUnsafeVarReferences && loop.isInvariant(sr1)) {
      invariant = true;
      for (int i = 0; i < edges.length; i++) {
        DDEdge edge = edges[i];
        if (edge.isAnyDistanceNonZero()) {
          invariant = false;
          break;
        }
      }
    }

    // Find the edges from sr1 that apply.
    // They must be dominated by sr1.

    AbstractCollection<Chord> allowed = null;
    if (invariant) {
      // If the subscript expression is loop invariant, we move any
      // load before the loop, any store after the loop, and
      // replace every reference by the scalar variable.

      if (chordsInLoop.size() == 0)
        loop.getLoopChordsRecursive(chordsInLoop);

      allowed = chordsInLoop;
    } else {
      idom.addElement(sc1);
      dom.getIterativeDominationNF(sc1, idom);
      allowed = idom;
    }

    if (invariant && !checkInvariant(loop, edges, sr1, allowed)) {
      invariant = false;
      idom.clear();
      idom.addElement(sc1);
      dom.getIterativeDominationNF(sc1, idom);
      allowed = idom;
    }

    // Determine the location before which replaces can not be done.

    int minimum = 0;
    if (!invariant) {
      Iterator<Chord> it1 = allowed.iterator();
      while (it1.hasNext()) {
        Chord mc = it1.next();
        if (mc == null)
          continue;

        int max = mc.getLabel();
        if (max > minimum)
          minimum = max;
      }
    }

    minimum = getSinks(edges, sr1, sc1, minimum, allowed);

    removeNAUses(sinks);

    if (!invariant && (sinks.size() < 2))
      return false;

    Note[] sinka = sinks.toArray(new Note[sinks.size()]);
    sort(sinka);

    if (!invariant) {
      int kkc = 0;
      for (int kk = 1; kk < sinka.length; kk++) {
        Note r2 = sinka[kk];
        if (r2.getChord().getLabel() < minimum)
          kkc++;
      }
      if (kkc < 1)
        return false;
    }

    // There must be at least two stores to the array element or
    // two loads from the array element to make scalar replscement
    // worthwhile.

    int scnt = 0; // Number of stores to array element.
    int lcnt = 0; // Number of loads from array element.
    for (int kk = 0; kk < sinka.length; kk++) {
      if (sinka[kk] instanceof ExprChord)
        scnt++;
      else
        lcnt++;
    }

    // While an improvement can be made with multiple stores to an
    // array element, the stores must be in the same basic block
    // to get the improvement. Unless, of course, the store is
    // loop invariant.

    if ((lcnt < 2) && !(invariant && ((lcnt >= 1) || (scnt >= 1))))
      return false;

    // Create the scalar replacement variable.

    Note s1 = sinka[0];
    Type vt = null;
    if (s1 instanceof ExprChord) // The first is a store into the array.
      vt = ((ExprChord) s1).getRValue().getType().getNonConstType();
    else
      vt = ((Expr) s1).getType().getNonConstType();

    VariableDecl        vd1   = genTemp(vt);
    LoadDeclAddressExpr ldae1 = new LoadDeclAddressExpr(vd1);
    LoadDeclValueExpr   ldve1 = new LoadDeclValueExpr(vd1);

    boolean   hasStore = false; // True if there is a store to the array in the loop.
    ExprChord last     = null;  // Last store to the scalar replacement variable.
    Expr      lhs      = null;

    Chord c1 = s1.getChord();

    if (s1 instanceof ExprChord) { // The first is a store into the array.
      ExprChord se1 = (ExprChord) s1;

      lhs = se1.getLValue();

      hasStore = true;

      se1.changeInDataEdge(lhs, ldae1); // Store into a temporary first.

      if (invariant) {
        findInvariants(lhs, 0);

        if ((se1.firstInBasicBlock() != loop) &&
            (se1.lastInBasicBlock() != loop.getLoopTail())) {
          // If the store is guarded, we have to put a load before the
          // loop.

          Expr      load = LoadValueIndirectExpr.create(lhs.copy());
          ExprChord nec1 = new ExprChord(ldae1.copy(), load);

          nec1.setLabel(c1.getLabel());
          newCFGNodeCount++;
          chordsToMove.add(nec1);
          nec1.copySourceLine(c1);
          last = nec1;

          // Use the same scalar replacement variable and know that
          // the SSA form is invalid because of multiple defs of the
          // scalar replacement variable.

          scribble.invalidSSAForm();
        }
      } else { // Store into the array next.
        ExprChord nec1 = new ExprChord(lhs, ldve1);

        ldve1.setUseDef(se1);
        c1.insertAfterOutCfg(nec1, c1.getNextChord());

        nec1.copySourceLine(c1);
        newCFGNodeCount++;
        stores.addElement(nec1);
        last = se1;
      }
    } else if (s1 instanceof LoadValueIndirectExpr) { // The first is a load from the array.
      LoadValueIndirectExpr ind1 = (LoadValueIndirectExpr) s1;

      // Use the temporary from now on.

      Note ss1 = ind1.getOutDataEdge();
      Expr rep = ldve1.addCast(ind1);
      ss1.changeInDataEdge(ind1, rep);

      // Load into the temproary first.

      ExprChord nec1 = new ExprChord(ldae1, ind1);

      ldve1.setUseDef(nec1);
      if (invariant) {
        nec1.setLabel(c1.getLabel());
        chordsToMove.add(nec1);
        findInvariants(ind1, 0);
      } else 
        c1.insertBeforeInCfg(nec1);
      nec1.copySourceLine(c1);
      newCFGNodeCount++;
      last = nec1;

    } else
      throw new scale.common.InternalError("What - " + c1 + "\n   " + s1);

    dChanged = true;
    rChanged = true;

    // If we do one, we have to do them all!

    for (int k = 0; k < sinka.length; k++) {
      Note src  = s1;
      Note sink = sinka[k];

      if (sink == src)
        continue;

      if (!invariant && (sink.getChord().getLabel() >= minimum))
        break;

      Note  s2 = sink;
      Chord c2 = s2.getChord();

      if (s2 instanceof ExprChord) { // The use is a store into the array.
        ExprChord se2 = (ExprChord) s2;

        hasStore = true;

        // Use the same scalar replacement variable and know that
        // the SSA form is invalid because of multiple defs of the
        // scalar replacement variable.

        scribble.invalidSSAForm();

        // Store into a temporary first.

        LoadDeclValueExpr   ldve2 = new LoadDeclValueExpr(vd1);
        LoadDeclAddressExpr ldae2 = new LoadDeclAddressExpr(vd1);
        Expr                lhs2  = se2.getLValue();

        se2.changeInDataEdge(lhs2, ldae2);

        last = se2;

        if (invariant) {
          if (lhs == null) {
            lhs = lhs2;
            findInvariants(lhs, 0);
          } else
            lhs2.unlinkExpression();
        } else { // Store into the array next.
          ExprChord nec2 = new ExprChord(lhs2, ldve2);
          ldve2.setUseDef(se2);
          c2.insertAfterOutCfg(nec2, c2.getNextChord());
          nec2.copySourceLine(c2);
          newCFGNodeCount++;
          stores.addElement(nec2);
        }
      } else if (s2 instanceof LoadValueIndirectExpr) { // The use is a load from the array.
        LoadValueIndirectExpr ind2  = (LoadValueIndirectExpr) s2;
        LoadDeclValueExpr     ldve2 = new LoadDeclValueExpr(vd1);

        // Use the temporary from now on.

        Note ss2 = ind2.getOutDataEdge();
        Expr rep = ldve2.addCast(ind2);

        ss2.changeInDataEdge(ind2, rep);
        ind2.unlinkExpression();
        ldve2.setUseDef(last);
        replacedLoadCount++;
      } else
        throw new scale.common.InternalError("What - " + c2);
    }

    if (invariant) {
      if (hasStore)
        newCFGNodeCount += insertStores(allowed, lhs, ldve1);
    } else { // Remove all any un-needed stores into the array.
      int sl = stores.size();
      for (int is = 0; is < sl - 1; is++) {
        ExprChord ec  = stores.elementAt(is);
        Chord     nxt = ec.getNextChord();

        // This store can be eliminated if another store is reachable 
        // in a straight line path (i.e., no branches).
        // Remember, all of these stores are in the iterated dominance domain
        // of the first array reference.

        while (nxt != null) {
          if (stores.contains(nxt)) { // Another is reachable, eliminate this one.
            ec.removeFromCfg();
            replacedStoreCount++;
            newCFGNodeCount--;
            stores.setElementAt(null, is);
            break;
          }
          nxt = nxt.getNextChord(); // DecisionChord instances return null.
        }
      }
    }

    return true;
  }

  /**
   * We can't treat a reference as invariant if the same array is
   * referenced by a different, loop-variant, subscript expression or
   * if there are any references to a variable whose address has been
   * taken.
   * @return true if still invariant
   */
  private boolean checkInvariant(LoopHeaderChord           loop,
                                 DDEdge[]                  edges,
                                 SubscriptExpr             sr1,
                                 AbstractCollection<Chord> allowed)
  {
    for (int j = 0; j < edges.length; j++) {
      DDEdge edge = edges[j];

      if (edge.isSpatial())
        continue;

      boolean                 chk = edge.isAnyDistanceNonZero();
      Iterator<SubscriptExpr> it  = edge.iterator();
      while (it.hasNext()) {
        SubscriptExpr sr2 = it.next();
        if (sr2 == sr1)
          continue;

        if (!allowed.contains(sr2.getChord()))
          continue;

        if (chk && !loop.isInvariant(sr2))
          return false;
      }
    }

    // Scan for references to variables whose address has been taken.
    // If there is a variable referenced whose address has been taken,
    // then we do not know if the array reference is to the same
    // location.  We could generate invalid code if we changed the
    // order of stores to memory.

    Vector<Declaration> decls = new Vector<Declaration>();
    Iterator<Chord>     it    = allowed.iterator();
    while (it.hasNext()) {
      Chord c  = it.next();
      int   ne = c.numInDataEdges();
      for (int i = 0; i < ne; i++) {
        Expr x = c.getInDataEdge(i);

        x.getDeclList(decls);

        int nd = decls.size();
        for (int j = 0; j < nd; j++) {
          Declaration decl = decls.get(j);
          if (decl.isVariableDecl() && decl.addressTaken()) {
            loopHasUnsafeVarReferences = true;
            return false;
          }
        }

        decls.clear();          
      }
    }

    return true;
  }

  /**
   * Check all the references to the array and select those that we
   * can use.  These are placed in sinks.
   */
  private int getSinks(DDEdge[]           edges,
                       SubscriptExpr      sr1,
                       Chord              sc1,
                       int                minimum,
                       AbstractCollection allowed)
  {
    int             len   = edges.length;
    LoopHeaderChord sr1lh = sc1.getLoopHeader();
    int             lab1  = sc1.getLabel();
    Vector<Chord>   dom   = (idom.size() == 0) ? null : idom;

    for (int j = 0; j < len; j++) {
      DDEdge edge = edges[j];

      if (edge.isSpatial())
        continue;

      boolean                 chk = edge.isAnyDistanceNonZero();
      Iterator<SubscriptExpr> it  = edge.iterator();
      while (it.hasNext()) {
        SubscriptExpr sr2 = it.next();
        if (sr2 == sr1)
          continue;

        if (!allowed.contains(sr2.getChord()))
          continue;

        if (!chk) {
          if (!done.add(sr2))
            continue;

          sr2.addUses(sinks, dom);
          continue;
        }

        // Writes to the array that have non-zero distance may affect
        // loads that we wish to replace by a scalar variable.
        // Determine the span in which loads may be safely replaced.

        chks.clear();
        sr2.addUses(chks);

        int clen = chks.size();
        for (int k = 0; k < clen; k++) {
          Note r2 = chks.elementAt(k);
          if (!(r2 instanceof Chord))
            continue;

          Chord c2   = (Chord) r2;
          int   lab2 = c2.getLabel();

          if (lab1 >= lab2) // If the write comes before the load.
            continue;

          if (lab2 >= minimum) // If the write comes after another write.
            continue;

          // The minimum for a write is really the loop containing the
          // write as the write affects every load in that loop.

          LoopHeaderChord r2lh = c2.getLoopHeader();
          while (r2lh != sr1lh) {
            r2lh = r2lh.getParent();
            if (r2lh == null)
              break;
          }

          if (r2lh == sr1lh)
            minimum = lab2; // Don't replace any loads after this position.
        }
      }
    }

    return minimum;
  }

  /**
   * Find all CFG nodes, that the specified expression is dependent
   * upon, before the specified insert point if they are in the
   * specified set of CFG nodes. This logic is necessary because
   * subscripting expressions are never moved outside of a loop even
   * if they are loop invariant.
   */
  private void findInvariants(Expr exp, int depth)
  {
    if (exp instanceof LoadDeclValueExpr) { // Common case - avoid allocation of vector.
      LoadDeclValueExpr ldve = (LoadDeclValueExpr) exp;
      ExprChord         se = ldve.getUseDef();

      if (se == null)
        return;

      if (se.isPhiExpr())
        return;

      if (chordsInLoop.contains(se)) {
        chordsToMove.add(se);
        findInvariants(se.getLValue(), depth + 1);
        findInvariants(se.getRValue(), depth + 1);
      }
      return;
    }

    if (exp instanceof LoadDeclAddressExpr) // Common case - avoid allocation of vector.
      return;

    Vector<LoadExpr> v = new Vector<LoadExpr>();

    exp.getLoadExprList(v);

    // Find the nodes that exp depends on.

    int l = v.size();
    if (l == 0)
      return;

    int k = 0;
    for (int i = 0; i < l; i++) {
      LoadExpr ld = v.get(i);

      if (ld instanceof LoadDeclValueExpr) {
        LoadDeclValueExpr ldve = (LoadDeclValueExpr) ld;
        ExprChord         se   = ldve.getUseDef();

        if (se == null)
          continue;

        if (se.isPhiExpr())
          continue;

        if (chordsInLoop.contains(se)) {
          chordsToMove.add(se);
          findInvariants(se.getLValue(), depth + 1);
          findInvariants(se.getRValue(), depth + 1);
        }
      }
    }
  }

  /**
   * Move the loop invariants outside of the loop.
   */
  private void moveInvariants(Chord insert)
  {
    int size = chordsToMove.size();
    if (size == 0)
      return;

    int lab = insert.getLabel();

    ExprChord[]     ec = new ExprChord[size];
    Iterator<Chord> it = chordsToMove.iterator();
    int              k  = 0;
    while (it.hasNext()) {
      ExprChord se = (ExprChord) it.next();
      ec[k++] = se;
      it.remove();
    }

    assert (k == size);

    if (k == 1) {
      ExprChord se = ec[0];
      se.extractFromCfg();
      insert.insertBeforeInCfg(se);
      se.setLabel(lab);
      return;
    }

    sort(ec); // Sort them by execution order.

    // Insert them before the loop.

    for (int i = 0; i < k; i++) {
      ExprChord se = ec[i];
      se.extractFromCfg();
      insert.insertBeforeInCfg(se);
      se.setLabel(lab);
    }
  }

  /**
   * Remove uses of the subscript address that don't load or store a
   * value into the array.
   * @return the use with the lowest label value
   */
  private Chord removeNAUses(HashSet<Note> sinks)
  {
    int            minLab  = Integer.MAX_VALUE;
    Chord          minNode = null;
    Iterator<Note> it      = sinks.iterator();
    while (it.hasNext()) {
      Note r1 = it.next();

      if ((r1 instanceof LoadValueIndirectExpr) || (r1 instanceof ExprChord)) {
        Chord sc = r1.getChord();
        if (sc == null)
          continue;

        int lab = sc.getLabel();
        if (lab < minLab) {
          minNode = sc;
          minLab = lab;
        }

        continue;
      }

      it.remove();
    }

    return minNode;
  }

  /**
   * On rare occasions inlinining will cause a synonym for an
   * array to be created.  This will result in a reference via one
   * array name to be eliminated when processing another array
   * name.  Therefore, we must look for and eliminate them now.
   */
  private Object[] pruneArrayRefs(Table<Declaration, SubscriptExpr> arrayRefs, VariableDecl arr)
  {
    Object[] v  = arrayRefs.getRowArray(arr);
    if (v == null)
      return null;

    int vi = 0;
    for (int i = 0; i < v.length; i++) {
      Object o = v[i];
      if (((Note) o).getChord() != null)
        v[vi++] = o;
    }

    if (vi <= 0)
      return null;

    if (vi < v.length) {
      Object[] nv = new Object[vi];
      System.arraycopy(v, 0, nv, 0, vi);
      v = nv;
    }

    return v;
  }
}
