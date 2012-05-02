package scale.score.trans;

import java.io.*;

import scale.common.*;
import scale.score.*;
import scale.score.analyses.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.pred.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.type.CompositeType;
import scale.clef.LiteralMap;

/**
 * Perform copy propagation optimization on a Scribble graph.
 * <p>
 * $Id: CP.java,v 1.70 2007-10-04 19:58:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The algorithm is from .....
 * <p>
 * When a right argument to an assignment statement is propagated to
 * the places where the left argument is used, the original assignment
 * is no longer needed.  However, since the CFG is in SSA form,
 * removing this assignment at this time causes a problem that results
 * in poorer code being generated.  Removing the assignment often
 * results in code of the form
 * <pre>
 *   var_3 = var_1;
 * ...
 *   var_1 = var_3;
 * </pre>
 * These statments are never removed.  The reason this happens is that
 * by removing the assignment, the lifetime of the variable is not
 * terminated prior to the assignment.  Thus, the coalescing of
 * variables, that is performed when leaving SSA form, can not change
 * this code to
 * <pre>
 *   var = var;
 * ...
 *   var = var;
 * </pre>
 * As a result, the logic to remove useless copies does not remove
 * these unneeded assignments.  If the original assignment is left in,
 * then coalescing works, the useless assignments are removed, and
 * dead variable elimination gets rid of the original assignment.
 * <p>
 * Register pressure is usually increased because the live range of a
 * variable is increased.  This tends to outweight the advantage of
 * eliminating the references to the other variable.
 * <p>
 * A transformation is {@link scale.score.trans#legal legal} if:
 * <li>the left-hand-side of the assignment is a variable,
 * <ul>
 * <li>the variable is not global or static,
 * <li>the variable's address is not taken,
 * <li>the variable is <code>volatile</code>,
 * <li>the variable has hidden aliases, and
 * </ul>
 * <li>the right-hand-side is an expression in which all variables
 * referenced are
 * <ul>
 * <li>not global or static,
 * <li>do not have their addresses taken,
 * <li>not <code>volatile</code>, and
 * <li>do not have hidden aliases.
 * </ul>
 * </ul>
 * It is {@link scale.score.trans#legal beneficial} if the
 * right-hand-side is
 * <ul>
 * <li>a variable or
 * <li>an addition or subtraction expression whose right operand is a
 * constant and whose left operand is a variable.
 * </ul>
 */
public class CP extends Optimization
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

  private static int aliasInhibitedCount = 0; // The number of times an alias inhibited a copy propagation.
  private static int propagationCount    = 0; // The number of times copy propagation was performed.

  static
  {
    Statistics.register("scale.score.trans.CP", "aliasInhibited");
    Statistics.register("scale.score.trans.CP", "propagations");
  }

  /**
   * Return the number of times an alias inhibited a copy propagation.
   */
  public static int aliasInhibited()
  {
    return aliasInhibitedCount;
  }

  /**
   * Return the number of times copy propagation was performed.
   */
  public static int propagations()
  {
    return propagationCount;
  }

  /**
   * @param scribble is the Scribble graph to be transformd by copy
   * propagation
   */
  public CP(Scribble scribble)
  {
    super(scribble, "_cp");
  }

  /**
   * Perform the actual copy propagation.
   */
  public void perform()
  {
    Stack<Chord> wl           = WorkArea.<Chord>getStack("perform CP");
    Chord        start        = scribble.getBegin();
    boolean      doSubscripts = scribble.scalarReplacementPerformed();

    // If subscript addresses are propagated before scalar replacement
    // is performed, scalar replacement generates incorrect code.

    wl.push(start);
    Chord.nextVisit();
    start.setVisited();

    while (!wl.empty()) {
      Chord s = wl.pop();
      s.pushOutCfgEdges(wl);

      if (s.isAssignChord()) {
        visitExprChord((ExprChord) s, doSubscripts);
      }
    }

    WorkArea.<Chord>returnStack(wl);

    if (rChanged)
      scribble.recomputeRefs();
  }

  private void visitExprChord(ExprChord se, boolean doSubscripts)
  {
    if (se.getPredicate() != null)
      return;

    Expr rvalue = se.getRValue();
    if (rvalue instanceof PhiExpr)
      return;

    if (rvalue instanceof DualExpr) {
      if (!doSubscripts &&
          (((DualExpr) rvalue).getHigh() instanceof SubscriptExpr))
        return; // Don't mess up ScalarReplacement.
      rvalue = ((DualExpr) rvalue).getLow();
    }

    // Check if the statement is a simple assignment to a variable.

    Expr lvalue = se.getLValue();
    if (!(lvalue instanceof LoadDeclAddressExpr))
      return;

    if (!rvalue.getCoreType().isAtomicType())
      return;

    LoadDeclAddressExpr lhs   = (LoadDeclAddressExpr) lvalue;
    VariableDecl        ldecl = (VariableDecl) lhs.getDecl();

    if (ldecl.isNotAliasCandidate())
      return;

    if (doSubscripts)
      while (rvalue instanceof DualExpr)
        rvalue = rvalue.getLow();

    Expr rv = rvalue;

    while (rv.isCast())
      rv = rv.getOperand(0);

    if (!(rv instanceof LoadDeclValueExpr)) {
      if (!(rv instanceof AdditionExpr) &&
          !(rv instanceof SubtractionExpr))
        return;

      if (!rv.optimizationCandidate())
        return;

      BinaryExpr be = (BinaryExpr) rv;
      Expr       ra = be.getRightArg();
      if (!ra.isLiteralExpr())
        return;

      if (!ra.getCoreType().isIntegerType())
        return;

      Expr la = be.getLeftArg();
      while (la.isCast())
        la = la.getOperand(0);

      if (!(la instanceof LoadDeclValueExpr))
        return; // It's not a simple add or subtract.

      // Undo some of the damage caused by ValNum & LICM.  Move simple
      // adds & subtracts to reduce register pressure.

      // It makes no sense to propagate an addition expression that
      // references a variable that should not be aliased.  For
      // example, if the variable is in memory, this could increase
      // the number of memory references and undo much of what global
      // variable replacement tries to do.

      VariableDecl org = ((VariableDecl) ((LoadDeclValueExpr) la).getDecl()).getOriginal();
      if ((org == ldecl.getOriginal()) || org.isNotAliasCandidate())
        return;

      LoopHeaderChord lh  = be.getChord().getLoopHeader();
      LoadExpr[]      uda = se.getDefUseArray();
      for (int i = 0; i < uda.length; i++) {
        LoadExpr        ud  = uda[i];
        Chord           s   = ud.getChord();
        LoopHeaderChord slh = s.getLoopHeader();
        Note            out = ud.getOutDataEdge();

        // Propagate the expression.

        out.changeInDataEdge(ud, rvalue.copy());
        ud.unlinkExpression();
        rChanged = true;
      }

      return;
    }

    LoadDeclValueExpr rhs   = (LoadDeclValueExpr) rv;
    VariableDecl      rdecl = (VariableDecl) rhs.getDecl();

    if (!rdecl.optimizationCandidate())
      return;

    if ((rdecl == rdecl.getOriginal()) && (ldecl.getOriginal() == rdecl))
      return; // The original variable is special in SSA form.

    ExprChord rhsud = rhs.getUseDef();
    if (!doSubscripts && (rhsud != null)) {
      // Don't mess up scalar replacement by creating new data
      // dependence edges.
      Expr rhsv = rhsud.getRValue();
      if (rhsv instanceof DualExpr)
        return;
    }

    // For now if the may use info is not null, do not propagate.

    if (rhs.getMayUse() != null)
      return;

    MayDef     md   = se.getMayDef();
    LoadExpr[] uses = se.getDefUseArray();
    for (int i = 0; i < uses.length; i++) {
      LoadExpr use = uses[i];

      // Copy propagation of a variable into a phi function just
      // results in extra copy operations.  For example, consider
      //      x1 = y
      //   l: x3 = phi(x1, x4)
      //
      // After copy propagation
      //      x1 = y
      //   l: x3 = phi(y, x4)
      //
      // After phi removal
      //      x1 = y
      //      x3 = y
      //   l:
      //
      // After variable coalescing
      //      x = y
      //      x = y

      if (use.getOutDataEdge() instanceof PhiExpr)
        continue;

      boolean doit = true;

      // If there is may def information associated with the
      // lhs of the expression, check to see if it can be
      // propagated.

      if (!unsafe) {
        MayUse mu = use.getMayUse();

        if (md != null) {
          if (mu == null) {
            doit = false;
          } else {
            Declaration d1 = null;
            MayDef      ud = mu.findMayDef();

            if (ud != null)
              d1 = ((LoadExpr) (ud.getLhs())).getDecl();

            /*Declaration d1 = mu.getDecl();*/
            Declaration d2 = ((LoadExpr) md.getLhs()).getDecl();
            doit = (d1 == d2);
          }
        }
      }

      if (doit) { // Change the variable in the use.
        if (rv == rvalue) {
          use.setDecl(rdecl);
          use.setUseDef(rhsud);
        } else {
          Note out = use.getOutDataEdge();
          out.changeInDataEdge(use, rvalue.copy());
          use.unlinkExpression();
        }

        rChanged = true;

        propagationCount++;
      } else {
        aliasInhibitedCount++;
      }
    }
  }
}
