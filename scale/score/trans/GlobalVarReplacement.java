package scale.score.trans;

import java.util.Iterator;

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
 * This class replaces references to global variables with references
 * to local variables.
 * <p>
 * $Id: GlobalVarReplacement.java,v 1.62 2007-10-04 19:58:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <p>
 * An attempt is made to put the first load of the global variable
 * outside of the loop.  This optimization must be performed outside
 * of SSA form because
 * <ul>
 * <li>it does not use the use-def information, and
 * <li>it is too hard to add the use-def links and phi functions if it
 * is in SSA form.
 * </ul>
 * <p>
 * In the following algorithm the relation "dominated by" means those
 * nodes in the iterative dominance domain.  The iterative dominance
 * domain terminates at calls to subroutines.
 * <pre>
 * For each global variable g
 *   Obtain set uses of the uses of g
 *   Pushd CFG start node on stack wl
 *   While set uses is not empty
 *     firstNode = null
 *     insertNode = null
 *     while stack wl is not empty
 *       sNode = pop(wl)
 *       push children of sNode on wl
 *       if sNode is a loop header and all exits of the loop are dominated by sNode
 *         insert = sNode
 *       if sNode is in set uses
 *         firstNode = sNode
 *     end while stack wl
 *     if firstNode == null
 *       exit while set uses
 *     if insertNode == null
 *       insertNode = firstNode
 *     else if firstNode is not dominated by the loop header insertNode
 *       insert = first
 *     Insert a load of the global variable into a temporary variable before insertNode
 *     Replace references to g in firstNode with references to the temporary
 *     Change all references to g in the nodes specified by uses
 *            that are dominated by firstNode
 *     remove firstNode from set uses
 *     Change all defs of g in the nodes specified by defs that
 *             are dominated by firstNode to def
 *        both the temporary and the global variable
 *   End while set uses
 * End for each global variable g
 * </pre>
 * <p>
 * Register pressure is always increased because a new variable is
 * introduced that has a live range that is always greater than the
 * live range of the reference to the gloabl variable it represents.
 * <p>
 * A transformation is {@link scale.score.trans#legal legal} if:
 * <ul>
 * <li>the variable is a global (or static),
 * <li>the variable is not <code>volatile</code>,
 * <li>the variable has no hidden aliases, and
 * <li>the variable's address is not taken.
 * </ul>
 * It is {@link scale.score.trans#legal beneficial} if:
 * <ul>
 * <li>the variable has more than one reference, and
 * <li>the live range of the surrogate variable would not be too great.
 * </ul>
 * The heuristic used to determine if the live range would be too
 * great divides the number of nodes dominated by the def of the
 * surrogate variable by the number of references and compares that to
 * a constant value.  This constant is 60 unless the variable is
 * floating point and the target architecture does not have
 * non-volatile floating point registers.  In this case the value of
 * 30 is used.
 */

public class GlobalVarReplacement extends Optimization
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

  private static int replacedLoadCount = 0;
  private static int outOfLoopCount    = 0;
  private static int newCFGNodeCount   = 0;

  private static final String[] stats = {
    "replacedLoads",
    "outOfLoops",
    "newCFGNodes"
  };

  static
  {
    Statistics.register("scale.score.trans.GlobalVarReplacement", stats);
  }

  /**
   * Return the current number of array loads replaced.
   */
  public static int replacedLoads()
  {
    return replacedLoadCount;
  }

  /**
   * Return the current number of loads placed outside of loops.
   */
  public static int outOfLoops()
  {
    return outOfLoopCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  private Vector<Chord> goodUses;  // Set of uses that are dominated by the insert point.
  private Vector<Chord> lhdom;     // Set of CFG nodes that are dominated by the insert point.
  private Vector<Chord> goodUses2; // Temporary work vector.
  private Vector<Chord> lhdom2;    // Temporary work vector.
  private Stack<Chord>  linear;    // A linearization of the CFG.
  private Stack<Chord>  wl;        // A work list for CFG spanning.

  private Domination dom;         // The CFG dominator information.
  private References refs;        // Uses & Defs of the variables in the CFG;
  private Chord      start;       // The root node of the CFG.
  private Chord      end;         // The termination node of the CFG.
  private boolean    isRecursive; // True if this routine is recursive.

  public GlobalVarReplacement(Scribble scribble)
  {
    super(scribble, "_gr");
    assert setTrace(classTrace);
  }

  public void perform()
  {
    if (scribble.isIrreducible())
      return;
  
    isRecursive = scribble.getRoutineDecl().isRecursive();

    int olc = replacedLoadCount;
    int osc = outOfLoopCount;

    dom      = scribble.getDomination();
    refs     = scribble.getRefs();
    end      = scribble.getEnd();
    start    = scribble.getBegin();
    dChanged = false;

    wl        = WorkArea.<Chord>getStack("perform GlobalVarReplacement");
    linear    = WorkArea.<Chord>getStack("perform GlobalVarReplacement");
    goodUses  = new Vector<Chord>(10);
    lhdom     = new Vector<Chord>();
    goodUses2 = new Vector<Chord>(10);
    lhdom2    = new Vector<Chord>();

    scribble.linearize(linear);


    // Check each global variable.
  
    Iterator<Declaration> itg = refs.getGlobalVars();
    while (itg.hasNext()) {
      Declaration decl = itg.next();
      processDecl(decl);
    }

    Iterator<Declaration> its = refs.getStaticVars();
    while (its.hasNext()) {
      Declaration decl = its.next();
      processDecl(decl);
    }

    WorkArea.<Chord>returnStack(linear);
    WorkArea.<Chord>returnStack(wl);

    if (dChanged) {
      scribble.recomputeDominators();
      scribble.recomputeRefs();
    }

    int dl = replacedLoadCount - olc;
    int ds = outOfLoopCount - osc;
    assert assertTrace(trace && ((dl + ds) > 0),
                       "** GlobalVarReplacement " + dl + " " + ds, null);
  }

  private void processDecl(Declaration decl)
  {
    if (!decl.isVariableDecl())
      return;

    Type type = decl.getType();
    if (!type.isAtomicType() || type.isVolatile())
      return;

    if (decl.addressTaken())
      return;

    if (((VariableDecl) decl).hasHiddenAliases())
      return;

    HashSet<Chord> uses = refs.getUseChordSet(decl);
    if (uses.size() < 1)
      return;

    boolean        global = decl.isGlobal() || isRecursive; // Inhibit past subroutine calls.
    HashSet<Chord> defs   = refs.getDefChordSet(decl);
    VariableDecl   vd     = null;
    boolean        hnvfp  = Machine.currentMachine.hasCapability(Machine.HAS_NON_VOLATILE_FP_REGS);
    int            hmax   = (!decl.getCoreType().isRealType() || hnvfp) ? 60 : 30;

    // Scan for nodes and globaal references that can be replaced.

    int numNodes = linear.size();
    int nextNode = 0;
    while (!uses.isEmpty()) {
      Chord   first       = null;  // First use of the global variable.
      Chord   insert      = null;  // Position to insert the def of the local variable.
      boolean moveOutside = false; // True if the loads can be placed
                                   // before a loop and/or the stores
                                   // don't have to be placed
                                   // immediately after the defs.
      boolean firstIsDef  = false; // True if the first "use" is a def.

      // Determine where the load can be inserted.

      for (; nextNode < numNodes; nextNode++) { // Traverse the CFG
        Chord s = linear.elementAt(nextNode);

        // Must detect the defs first!!!
        // Consider the case of
        //   g = g - 1;

        if (defs.contains(s)) { // First use is def of the global variable.
          first = s;
          firstIsDef = true;
          break;
        }

        if (uses.contains(s)) { // First use of the global variable.
          first = s;
          break;
        }
      }

      nextNode++;

      if (first == null) // No position was found.
        return;

      lhdom.clear();

      int                numUses       = 0;
      int                goodUsesLimit = 0;
      LoopHeaderChord    lh            = first.getLoopHeader();
      LoopPreHeaderChord lph           = lh.getPreHeader();
      boolean            okLoop        = false;
      if ((lph != null) && lh.isInnerMostLoop()) {
          okLoop = true;
          dom.getIterativeDominationNF(lph, lhdom, global, null);

          int l = lhdom.size();
          for (int i = 0; i < l; i++)
            if (uses.contains(lhdom.get(i)))
              numUses++;
      }

      if (firstIsDef && (numUses == 0)) {
        // Find iterative dominance set of the global variable def.
        goodUsesLimit = 1;
        dom.getIterativeDominationNF(first, lhdom, global, null);
        moveOutside = !lh.isTrueLoop() && lhdom.contains(end);
        insert = first;
      } else {
        if (okLoop) {
          // If the loop exits are all in the allowed dominatees,
          // the loop pre-header can be used as the place to insert
          // the load outside of the loop.

          insert = lph;

          // If the iterative dominator set contains the end node,
          // then we can put any needed stores back into the global
          // variable at the end of the function.  Otherwise, we can
          // put it after the loop exits if the loop exits are in
          // the set.  It's too hard to determine which def of a
          // global variable is the "last" one and if it is in the
          // loop.

          moveOutside = true;
          if (!lhdom.contains(end)) {
            int nle = lh.numLoopExits();
            for (int i = 0; i < nle; i++) {
              if (!lhdom.contains(lh.getLoopExit(i))) {
                moveOutside = false;
                insert = null;
                break;
              }
            }
          }
        }

        if ((insert == null) || !lhdom.contains(first)) {
          // If no usable LoopPreHeader found or if the first use is
          // not dominated by the LoopPreHeader.  Find iterative
          // dominance set of the global variable use.

          insert = first;
          goodUsesLimit = 1;
          lhdom.clear();
          dom.getIterativeDominationNF(first, lhdom, global, null);
          moveOutside = !lh.isTrueLoop() && lhdom.contains(end);
        } else
          outOfLoopCount++; // The load will be moved outside of the loop.
      }

      // Find the uses that are dominated by the insert point.

      if (goodUses(uses, lhdom, goodUses, first))
        continue; // All uses are calls.

      // Try to find an insert point that dominates more uses without
      // greatly increasing register pressure.

      int n = goodUses.size();
      if (!moveOutside) {
        while (true) {
          Chord fibb = insert.firstInBasicBlock();

          if ((fibb == start) ||
              (fibb.isLoopExit()) ||
              (fibb.numInCfgEdges() > 1))
            break;

          Chord p = fibb.getInCfgEdge();

          // See if p dominates more uses.

          lhdom2.clear();
          dom.getIterativeDominationNF(p, lhdom2, global, null);

          goodUses(uses, lhdom2, goodUses2, first);
          int m = goodUses2.size();

          if (m <= n)
            break;

          // Found a better insert point.

          insert = p;
          n = m;

          Vector<Chord> t1 = goodUses;
          goodUses = goodUses2;
          goodUses2 = t1;

          Vector<Chord> t2 = lhdom;
          lhdom = lhdom2;
          lhdom2 = t2;
        }
      }

      if ((n <= 0) || (n < goodUsesLimit))
        continue; // It's not worth doing.

      if (useHeuristics && ((n > 0) && ((lhdom.size() / n) > hmax)))
        continue; // It's not worth doing.

      // Make a local variable surrogate for the global variable and
      // initialize it with the same value as the global variable.

      if (vd == null)
        vd = genTemp(decl.getType().getNonAttributeType());

      if (firstIsDef && (insert != lph)) {
        // Initilize local variable copy with initializer for global.

        ExprChord           ecg   = (ExprChord) first;
        LoadDeclAddressExpr ldaeg = (LoadDeclAddressExpr) ecg.getLValue();

        if ((ldaeg != null) && (ldaeg.getDecl() == decl) && (ecg.getCall(false) == null)) {
          LoadDeclAddressExpr ldael = new LoadDeclAddressExpr(vd);
          ecg.removeRefs(refs);
          ecg.setLValue(ldael);
          ldael.setOutDataEdge(ecg);
          ecg.recordRefs(refs);

          // Store local variable copy into global variable.

          LoadDeclValueExpr   gv   = new LoadDeclValueExpr(vd);
          ExprChord           ec   = new ExprChord(ldaeg, gv);

          first.insertAfterOutCfg(ec, first.getNextChord());
          ec.copySourceLine(first);
          newCFGNodeCount++;
          ec.recordRefs(refs);

          // Def the local variable with the value of the global at the
          // insert point.  This def will not be needed down the CFG
          // path from the insert point to first.

          if (insert != first) {
            Expr      lhs = new LoadDeclAddressExpr(vd);
            Expr      rhs = new LoadDeclValueExpr(decl);
            ExprChord ec2 = new ExprChord(lhs, rhs);
            insert.insertBeforeInCfg(ec2);
            ec2.copySourceLine(insert);
            newCFGNodeCount++;
            ec2.recordRefs(refs);
          }
        } else
          beforeAfter(vd, decl, ecg, true);

      } else { // Insert a load of the global variable into the local variable.
        LoadDeclValueExpr   gv   = new LoadDeclValueExpr(decl);
        LoadDeclAddressExpr ldae = new LoadDeclAddressExpr(vd);
        ExprChord           ec   = new ExprChord(ldae, gv);
        insert.insertBeforeInCfg(ec);
        ec.copySourceLine(insert);
        newCFGNodeCount++;
        ec.recordRefs(refs);

        // Change all references in the first use to the temporary variable.

        Vector<LoadExpr> expList = first.getLoadExprList();
        if (expList != null) {
          first.removeRefs(refs);

          int l = expList.size();
          for (int i = 0; i < l; i++) {
            LoadExpr le = expList.elementAt(i);
            if (le.getDecl() != decl)
              continue;
            if (le instanceof LoadDeclAddressExpr)
              continue;

            le.setDecl(vd);
            replacedLoadCount++;
          }

          first.recordRefs(refs);
        }
      }
      dChanged = true;

      // Change all references in the remaining uses to the
      // temporary variable.

      int l = goodUses.size();
      for (int i = 0; i < l; i++) {
        Chord  s       = goodUses.elementAt(i);
        Vector<LoadExpr> eList = s.getLoadExprList();
        if (eList != null) {
          s.removeRefs(refs);

          int len = eList.size();
          for (int j = 0; j < len; j++) {
            LoadExpr le = eList.elementAt(j);
            if (le.getDecl() != decl)
              continue;
            if (le instanceof LoadDeclAddressExpr)
              continue;

            le.setDecl(vd);
            replacedLoadCount++;
          }

          s.recordRefs(refs);
        }

        uses.remove(s);
      }

      uses.remove(first);
      if (insert != lph)
        defs.remove(first);

      // Collect the set of defs that are dominated.

      Iterator<Chord>   di           = defs.iterator();
      boolean           thereAreDefs = false;
      Vector<ExprChord> v            = new Vector<ExprChord>();
      while (di.hasNext()) {
        ExprChord dc = (ExprChord) di.next();
        if (!lhdom.contains(dc) && (dc != first))
          continue;
        thereAreDefs = true;
        v.add(dc);
      }

      // Change all the defs in the iterative dominance set to stores
      // to both the local copy and the global variable.  This is done
      // in two parts to avoid conflicts with the iterator above.

      int vl = v.size();
      for (int vi = 0; vi < vl; vi++) {
        ExprChord           dc    = v.get(vi);
        LoadDeclAddressExpr lhs   = (LoadDeclAddressExpr) dc.getLValue();

        if ((lhs != null) && (lhs.getDecl() == decl) && (dc.getCall(false) == null)) {

          if (!moveOutside) { // Insert a store after the def.
            LoadDeclAddressExpr ldae2 = new LoadDeclAddressExpr(decl);
            LoadDeclValueExpr   ldve  = new LoadDeclValueExpr(vd);
            ExprChord           ec2   = new ExprChord(ldae2, ldve);
            ec2.recordRefs(refs);
            dc.insertAfterOutCfg(ec2, dc.getNextChord());
            ec2.copySourceLine(dc);
            newCFGNodeCount++;
            dChanged = true;
          }

          dc.removeRefs(refs);
          lhs.setDecl(vd);
          dc.recordRefs(refs);
          continue;
        }

        beforeAfter(vd, decl, dc, false);
      }

      // If the global variable was defined and we were able to avoid
      // placing the stores after the defs then we insert required
      // stores here.

      if (thereAreDefs && moveOutside) {
        if (lhdom.contains(end)) { // Insert stores at the end of the function.
          LoadDeclAddressExpr ldae2 = new LoadDeclAddressExpr(decl);
          LoadDeclValueExpr   ldve  = new LoadDeclValueExpr(vd);
          ExprChord           ec2   = new ExprChord(ldae2, ldve);
          ec2.recordRefs(refs);
          end.insertBeforeInCfg(ec2);
          ec2.copySourceLine(end);
          newCFGNodeCount++;
        } else if (insert instanceof LoopPreHeaderChord) {
          // We replaced every reference in the dominated nodes so now
          // we must insert a store at every exit from the dominated
          // nodes.

          LoadDeclAddressExpr ldae2 = new LoadDeclAddressExpr(decl);
          LoadDeclValueExpr   ldve  = new LoadDeclValueExpr(vd);

          newCFGNodeCount += insertStores(lhdom, ldae2, ldve);
        } else
          throw new scale.common.InternalError("** Improper move point " + insert);
      }
    }
  }

  /**
   * The address of the global is passed to a function.  Insert a copy
   * to the global before the call and a copy from the global after
   * the call.  Note, the same global variable may be referenced more
   * than once in the call statememnt.
   */
  private void beforeAfter(VariableDecl vd, Declaration decl, ExprChord ecg, boolean isFirst)
  {
    LoadDeclAddressExpr ldae2 = new LoadDeclAddressExpr(vd);
    LoadDeclValueExpr   ldve2 = new LoadDeclValueExpr(decl);
    ExprChord           ec2   = new ExprChord(ldae2, ldve2);
    ec2.recordRefs(refs);
    ecg.insertAfterOutCfg(ec2, ecg.getNextChord());
    ec2.copySourceLine(ecg);
    newCFGNodeCount++;
    dChanged = true;

    if (isFirst)
      return;

    LoadDeclAddressExpr ldae3 = new LoadDeclAddressExpr(decl);
    LoadDeclValueExpr   ldve3 = new LoadDeclValueExpr(vd);
    ExprChord           ec3   = new ExprChord(ldae3, ldve3);
    ec3.recordRefs(refs);
    ecg.insertBeforeInCfg(ec3);
    ec3.copySourceLine(ecg);
    newCFGNodeCount++;
  }

  /**
   * Find those uses in the uses set that are dominated by the
   * iterative dominator set.  This is just the intersection of the
   * two sets.
   * @param uses is the set of uses
   * @param lhdom is the iterative dominator set.
   * @param goodUses is the resulting set of uses that are dominated
   */
  private boolean goodUses(HashSet<Chord> uses, Vector<Chord> lhdom, Vector<Chord> goodUses, Chord first)
  {
    goodUses.clear();

    boolean         allCalls = true;
    Iterator<Chord> ui       = uses.iterator();
    while (ui.hasNext()) {
      Chord uc = ui.next();
      if (uc == first)
        continue;

      if (!lhdom.contains(uc))
        continue;

      goodUses.addElement(uc);
      if (uc.getCall(false) == null)
        allCalls = false;
    }

    return allCalls;
  }

  /**
   * Specify that this optimization requires that the CFG NOT be in
   * SSA form.
   */
  public int requiresSSA()
  {
    return NO_SSA;
  }
}
