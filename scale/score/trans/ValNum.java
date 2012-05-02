package scale.score.trans;

import scale.common.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.expr.Literal;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.IntLiteral;
import scale.clef.LiteralMap;

import scale.score.*;
import scale.score.pred.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.analyses.*;

/**
 * Global value numbering optimization.
 * <p>
 * $Id: ValNum.java,v 1.100 2007-10-04 19:58:38 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a.,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Global value numbering based on the dominator-based value numbering
 * technique by Preston Briggs et al, "Value Numbering", Software --
 * Practice and Experience, 1990.
 * <p>
 * Global value numbering is essentially a common subexpression.
 * elimination algorithm.  It attempts to find multiple occurrences of
 * the same expression and replace the subsequent ones with a
 * reference to a new variable whose value is obtained from the first
 * occrrence and are dominated by the first occurrence.
 * <p>
 * If the first occurrence of the expression is assigned to a
 * variable, this optimization will attempt to use that variable as
 * the replacement variable.
 * <p>
 * It is possible, though unlikely, for a transformation to reduce
 * register pressure.  If the expression references more than one
 * variable that are referenced nowhere else in the scope of the def
 * of the replacement variable, register pressure will be reduced.  If
 * there is more than one variable that is referenced in other
 * expressions, register pressure will increase.
 * <p>
 * A transformation is {@link scale.score.trans#legal legal} if:
 * <ul>
 * <li>all the variables in the the expression
 * <ul>
 * <li>are not global or static,
 * <li>do not have their addresses taken,
 * <li>are not <code>volatile</code>,
 * <li>do not have hidden aliases, and
 * </ul>
 * <li>the expression does not have side effects.
 * </ul>
 * It is {@link scale.score.trans#legal beneficial} if:
 * <ul>
 * <li>none of the variables in the the expression are floating point
 * or the architecture has non-volatile floating point registers,
 * <li>the expression is not a simple add or subtract of an integer
 * constant,
 * <li>the expression is not a constant that can be easily generated, and
 * <li>there is more than one occurrence of the expression,
 * </ul>
 */

public class ValNum extends Optimization
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

  private static int deadCFGNodeCount = 0; // A count of nodes removed because of value numbering.
  private static int newCFGNodeCount  = 0; // A count of nodes created because of value numbering.
  private static int propagationCount = 0; // A count of the expressions removed by value numbering.

  private static final String[] stats = {
    "deadCFGNodes",
    "newCFGNodes",
    "propagations"
  };

  static
  {
    Statistics.register("scale.score.trans.ValNum", stats);
  }

  /**
   * Return the number of dead nodes removed.
   */
  public static int deadCFGNodes()
  {
    return deadCFGNodeCount;
  }

  /**
   * Return the number of new nodes added.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * Return the number of expressions removed.
   */
  public static int propagations()
  {
    return propagationCount;
  }

  private boolean duplicate = false; // True if a sub-expression ocurs more than once in the same expression.
  private ExprMap hashMap;

  public ValNum(Scribble scribble)
  {
    super(scribble, "_vn");
  }

  /**
   * Go through dominance tree to do value numbering.  Basicly we use
   * the SSA name as the value number. For example in
   * <pre>
   *    a1 = b1 + c1
   * </pre>
   * the value number of <tt>b1+c1</tt> is <tt>a1</tt>. An exception
   * ocurrs when the left hand side has alias varibles. In this case,
   * we create a new variable `for the right hand expression.
   */
  @SuppressWarnings("unchecked")
  public void perform()
  {
    Stack<Object> wl          = WorkArea.<Object>getStack("perform ValNum");
    Stack<Object> hashedExprs = null; // Keep track of the hashed nodes in this chord.
    Vector<Chord> dead        = new Vector<Chord>();
    Chord      start          = scribble.getBegin();
    Domination dom            = scribble.getDomination(); // Force the computation of dominators.
    boolean    saveumu        = LoadExpr.setUseMayUse(true);
    boolean    skipSubscripts = !scribble.scalarReplacementPerformed();

    hashMap = new ExprMap(30);

    rChanged = false; // True if references must be recomputed.
    dChanged = false; // True if dominators must be recomputed.

    wl.push(start); 

    while (!wl.isEmpty()) { // Scan the dominator tree.
      Object o = wl.pop();

      if (o instanceof Stack) { // Remove hashed expressions.
        hashedExprs = (Stack<Object>) o;
        hashMap.remove(hashedExprs);
        continue;
      }

      Chord   curChord    = (Chord) o;
      boolean chordRemove = false;

      // Do value numbering on the current chord.

      if (!(curChord.isPhiExpr())) {
        // Replace all uses in the chord by their value numbers.

        replaceExpressionsWithLoads(curChord);

        if (curChord.isExprChord()) {
          if (hashedExprs == null)
            hashedExprs = new Stack<Object>(); // Keep track of the hashed nodes in this chord.
          duplicate = false;
          chordRemove = processChord((ExprChord) curChord, hashedExprs, skipSubscripts);
          if (hashedExprs.size() > 0) {
            // Remove these nodes from the hash table when all
            // successors are visited.
            wl.push(hashedExprs);
            hashedExprs = null;
          }
          if (duplicate) // Expression occurs more than once in the same node..
            replaceExpressionsWithLoads(curChord);
        }
      }

      // Enter all the current Chord's immediate successors in the
      // dominance tree into the queue.  If we visit all predecessors
      // nodes before visiting a node we do a better job of
      // optimization.

      Chord[] dd = dom.getDominatees(curChord);
      for (Chord c : dd)
        wl.push(c);

      if (curChord.isLastInBasicBlock())
        processPhis(curChord); // visit Phi nodes in successor basic blocks
  
      if (chordRemove && !dead.contains(curChord))
        dead.addElement(curChord);
    }

    WorkArea.<Object>returnStack(wl);

    int len = dead.size();
    for (int i = 0; i < len; i++) {
      Chord curChord = dead.elementAt(i);
      if (curChord.isAssignChord()) {
        // This shouldn't happen - but if it does this keeps us safe.
        int n = ((ExprChord) curChord).numDefUseLinks();
        if (n > 0)
          continue;
      }
      if (curChord.isFirstInBasicBlock()) {
        // Preserve the edges, the null will be removed later.
        curChord.insertBeforeInCfg(new NullChord());
        newCFGNodeCount++;
      }
      curChord.removeFromCfg();  
      deadCFGNodeCount++;
      dChanged = true;
      rChanged = true;
    }

    LoadExpr.setUseMayUse(saveumu);

    if (rChanged)
      scribble.recomputeRefs();
    if (dChanged)
      scribble.recomputeDominators();
  }

  private boolean processChord(ExprChord     echord,
                               Stack<Object> hashedExprs,
                               boolean       skipSubscripts)
  {
    // Do value numbering on the assignment statement.

    Expr lexpr = echord.getLValue(); // left hand side
    if (lexpr != null) {
      if (lexpr instanceof DualExpr)
        lexpr = ((DualExpr) lexpr).getLow();

      int l = lexpr.numInDataEdges();
      for (int i = 0; i < l; i++)
        processExpr(echord, hashedExprs, lexpr.getInDataEdge(i), lexpr, skipSubscripts);
    }

    Expr rexpr = echord.getRValue(); // right hand side
    return processExpr(echord, hashedExprs, rexpr, echord, skipSubscripts);
  }

  /**
   * Add every worthwhile expression, in the specified expression
   * tree, to the set of hashed expressions.
   * @param curChord is the CFG node containing rexpr
   * @param hashedExprs is the set of hashed expressions
   * @param rexpr is the expression to process
   * @param src the out-going data edge from rexpr
   * @param skipSubscripts is true if the DualExpr instances must be
   * retained
   * @return true if CFG node can be eliminated
   */
  private boolean processExpr(Chord   curChord,
                              Stack<Object> hashedExprs,
                              Expr    rexpr, 
                              Note    src,
                              boolean skipSubscripts)
  {
    if (rexpr == null)
      return false;

    // Moving the high expression messes up data dependence analysis.
    // Also, the backends down-stream may not be expecting a high
    // expression; The high expression won't be removed if it has been
    // moved out of a dual.

    if (rexpr instanceof DualExpr) {
      src = rexpr;
      rexpr = ((DualExpr) rexpr).getLow();
    }

    if (hashMap.get(rexpr) != null) {
      duplicate = true;
      hashMap.specifyMultipleOccurrences();
      return false;
    }

    int l = rexpr.numInDataEdges();
    for (int i = 0; i < l; i++)
      processExpr(curChord, hashedExprs, rexpr.getInDataEdge(i), rexpr, skipSubscripts);

    Expr      lexpr = null;
    ExprChord se    = null;
    if (src instanceof ExprChord) {
      se = (ExprChord) src;
      lexpr = se.getLValue();
    }

    if (rexpr.optimizationCandidate()) {
      // The expression has not been hashed. Create a new value number.

      if (rexpr.getCoreType().isRealType() &&
          !Machine.currentMachine.hasCapability(Machine.HAS_NON_VOLATILE_FP_REGS))
        return false;

      if ((se == null) ||
          (lexpr instanceof LoadDeclAddressExpr) ||
          (se.getMayDef() != null)) {
        // If the LHS is not a simple address and has alias.

        if (rexpr.numInDataEdges() < 1) // Too simple to use.
          return false;

        if ((rexpr instanceof AdditionExpr) ||
            (rexpr instanceof SubtractionExpr)) {
           BinaryExpr be = (BinaryExpr) rexpr;
           if ((be.getLeftArg() instanceof LoadExpr) &&
               be.getRightArg().isLiteralExpr() &&
               (be.getCoreType().isIntegerType() || be.getCoreType().isPointerType()))
            return false; // Increases register pressure too much.
        }

        if (rexpr.isCast())
          return false; // Increases register pressure too much.

        if (rexpr instanceof LoadFieldAddressExpr)
          return false;

        Type              t        = rexpr.getType().getNonAttributeType();
        VariableDecl      tempDecl = genTemp(t); // May not be used if the expression is not encountered again.
        LoadDeclValueExpr temp     = new LoadDeclValueExpr(tempDecl);

        // It is extremely important that the expression used as a
        // key be the original expression that is still linked into
        // the CFG node.

        hashMap.put(rexpr, temp, null, true, hashedExprs);

        return false;
      }

      if (!(rexpr instanceof LoadDeclValueExpr) &&
          (lexpr instanceof LoadDeclAddressExpr)) {
        // Use LHS as value number.
        LoadDeclAddressExpr ll = (LoadDeclAddressExpr) lexpr;
        VariableDecl        ld = (VariableDecl) ll.getDecl();
        if (ld.isNotAliasCandidate())
          return false;

        if (rexpr.isLiteralExpr() &&
           (((LiteralExpr) rexpr).executionCostEstimate() <= minimumExecutionCost))
          return false;

        LoadDeclValueExpr ve = new LoadDeclValueExpr(ld);
        hashMap.put(rexpr, ve, se, false, hashedExprs); 
        return false;
      }
    }

    if (!(lexpr instanceof LoadDeclAddressExpr))
      return false;

    if (skipSubscripts && (src instanceof DualExpr)) {
      DualExpr de   = (DualExpr) src;
      Expr     high = de.getHigh();

      // Removing the DualExpr instance messes up dependence testing.

      if (high instanceof SubscriptExpr)
        return false;
    }

    // Process possible copy chord.

    LoadDeclAddressExpr ll = (LoadDeclAddressExpr) lexpr;
    VariableDecl        ld = (VariableDecl) ll.getDecl();

    if (ld.isNotAliasCandidate())
      return false;

    if (ld.hasHiddenAliases())
      return false;

    if (rexpr instanceof LoadDeclValueExpr) {
      LoadDeclValueExpr rl = (LoadDeclValueExpr) rexpr;
      VariableDecl      rd = (VariableDecl) rl.getDecl();

      // If it's has no may-use link, it's not global, it's address is
      // not taken, and it's not the original variable in SSA form,
      // then we can do the copy propagation.

      if (!unsafe && (rl.getMayUse() != null))
        return false;
      if (!rl.optimizationCandidate())
        return false;
      if (((rd == rd.getOriginal()) && (ld.getOriginal() == rd)))
        return false;

      // Don't mess up scalar replacement by creating new data
      // dependence edges.

      ExprChord sex = rl.getUseDef();
      if (sex != null) {
        Expr rhs = sex.getRValue();
        if (!rhs.optimizationCandidate())
          return false;
      }
    } else if (rexpr.isLiteralExpr()) {
      int cost = ((LiteralExpr) rexpr).getLiteral().executionCostEstimate();
      if (cost <= minimumExecutionCost)
        return false;
      if (rexpr.getCoreType().isRealType() &&
          !Machine.currentMachine.hasCapability(Machine.HAS_NON_VOLATILE_FP_REGS))
        return false;
    } else
      return false;

    // Set the value number of the LHS to the value number of rexpr.

    LoadDeclValueExpr ve = new LoadDeclValueExpr(ld);
    MayDef            m  = se.getMayDef();
    if (m != null)
      ve.addMayUse(new MayUse((VirtualVar) m.getLhs().getDecl()));

    ExprChord ud = rexpr.getUseDef();

    hashMap.put(ve, rexpr, ud, false, hashedExprs);

    return unsafe || (se.getMayDef() == null);
  }

  /**
   * The following logic is more complex than one would expect due to
   * the following circumstance: there exits a CFG node A with more
   * than one out-going CFG edge to the same phi-function CFG node B.
   * This can happen when there is a <code>switch</code> statement
   * with multiple cases that are just <code>break</code> statements.
   * This problem is exacerbated by loop unrolling.
   */
  private void processPhis(Chord curChord)
  {
    Chord.nextVisit();

    int l = curChord.numOutCfgEdges();
    for (int j = 0; j < l; j++) {
      Chord curo = curChord.getOutCfgEdge(j);
      if (curo.visited())
        continue;

      curo.setVisited();

      int num = curo.numOfInCfgEdge(curChord);

      assert (num > 0) : "Invalid CFG " + curChord;

      for (int i = 0; i < num; i++) {
        int pos = curo.nthIndexOfInCfgEdge(curChord, i);

        Chord cur = curo;
        do {
          if (cur.isPhiExpr()) {
            PhiExprChord es    = (PhiExprChord) cur;
            PhiExpr      phi   = es.getPhiFunction();
            Expr         oprnd = phi.getOperand(pos);

            replaceExpressionsWithLoads(oprnd);
          }

          if (cur.isLastInBasicBlock())
            break;

          cur = cur.getNextChord();
        } while (true);
      }
    }
  }

  /**
   * Replace expressions with loads.
   */
  private boolean replaceExpressionsWithLoads(Note n) 
  {
    if (n == null)
      return false;

    if (n instanceof Expr)
      n = checkEntireExpr((Expr) n);

    if (n == null)
      return true;

    int l = n.numInDataEdges();
    if (l < 1)
      return false;

    boolean f = false;
    for (int pos = 0; pos < l; pos++) {
      Expr exp = n.getInDataEdge(pos);
      f |= replaceExpressionsWithLoads(exp);
    }

    if (f && (n instanceof Expr))
      n = checkEntireExpr((Expr) n);

    return f;
  }

  private Expr checkEntireExpr(Expr exp)
  {
    Expr val = hashMap.get(exp);

    if (val == null)
      return exp;

    if (val.equivalent(exp))
      return exp;

    Expr key = hashMap.getKey();
    if (key == exp)
      return exp; // Don't replace ourselves.

    if (!hashMap.hasMultipleOccurrences())
      return exp;

    Note out = exp.getOutDataEdge();
    if (out == null) {
      exp.unlinkExpression();
      return null; // Already done.
    }

    Expr      vc = val.copy();
    ExprChord ud = hashMap.getUseDef();
    vc.setUseDef(ud);

    out.changeInDataEdge(exp, vc);
    propagationCount++;
    rChanged = true;

    // Don't create the temporary unless the expression is used at
    // least twice in the same dominator tree.

    if (hashMap.insertCopyRequired()) {
      // This section of code is crucial and must be correct in order
      // to insert the copies in the right order.  Consider
      //   a = (b + (c + (d + e)))
      // and contemplate what happens if the sub-expressions are
      // encountered in different orders in the dominator tree scan.

      // We need to insert a temporary for that expression and replace 
      // the expression in the original expression tree.

      // The Chord must be obtained prior to replacing the expression.

      Note  n   = key.getOutDataEdge();
      Chord cur = n.getChord();
      Expr  nvc = vc.copy();

      n.changeInDataEdge(key, nvc);

      LoadDeclAddressExpr ldae = new LoadDeclAddressExpr(((LoadExpr) vc).getDecl());
      ExprChord           nec  = new ExprChord(ldae, key.addCast(vc));

      hashMap.setUseDef(nec);
      vc.setUseDef(nec);
      nvc.setUseDef(nec);
      cur.insertBeforeInCfg(nec);
      nec.copySourceLine(cur);
      dChanged = true;
      newCFGNodeCount++;

      Vector<Expr> exps = cur.getExprList();
      if (exps != null) {
        int l = exps.size();
        for (int k = 0; k < l; k++)
          replaceExpr(exp, exps.elementAt(k), vc, nec);
      }
    }

    exp.unlinkExpression();
    return null;
  }

  private void replaceExpr(Expr key, Expr olde, Expr newe, ExprChord ud)
  {
    if (!key.equivalent(olde))
      return;

    if (key == olde)
      return;

    Expr vc = newe.copy();
    vc.setUseDef(ud);
    Note out = olde.getOutDataEdge();
    out.changeInDataEdge(olde, vc);
    olde.unlinkExpression();
    propagationCount++;
    rChanged = true;
  }
}
