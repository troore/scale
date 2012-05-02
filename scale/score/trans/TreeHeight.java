package scale.score.trans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import scale.common.*;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.type.Type;

/**
 * Perform expression tree height reduction on a Scribble graph.
 * <p>
 * $Id: TreeHeight.java,v 1.25 2007-02-28 18:00:38 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The tree height reduction optimization attempts to improve the
 * performance of individual expressions by allowing more operations
 * to be performed in parallel.  For eample, consider the expression
 * <code>(a + (b + (c + d)))</code>.  Using this order of precedence,
 * the three addition operations must be performed sequentially.  If
 * the expression is transformed to <code>((a + b) + (c + d))</code>
 * then two of the additons may be performed simultaneously.  This
 * will benefit superscalar architectures.
 * <p>
 * In this example, the two expressions are equivalent.  However, on a
 * digital computer using floating point operations, these two
 * expressions may result in two different values because floating
 * point addition is not associative.
 * <p>
 * Note also that to calculate the value of the first expression
 * requires only five registers.  One is used to hold the sum and the
 * others hold the values of the variables.  Six registers are
 * required for the second expression as a partial sum must be kept
 * while the other partial sum is calculated. (The number of registers
 * required actually depends on which variables have been allocated to
 * registers.)  The net result is an increase in register pressure.
 * <p>
 * A transformation is {@link scale.score.trans#legal legal} if:
 * <ul>
 * <li>the operator is binary-associative,
 * <li>the operands are scalars,
 * <li>the operands are integers or the user has allowed operations
 * with floating point operands to be re-arranged, and
 * <li>the expression has four or more leaves.
 * </ul>
 * It is {@link scale.score.trans#legal beneficial} if
 * it is legal.
 * </ul>
 */
public class TreeHeight extends Optimization
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

  private static int treeCount = 0; // The number of expression trees reduced.

  private static final String[] stats = {"treesReduced"};

  static
  {
    Statistics.register("scale.score.trans.TreeHeight", stats);
  }

  /**
   * Return the number of expression trees found
   */
  public static int treesReduced()
  {
    return treeCount;
  }

  /**
   * Use Huffman coding weight balancing.
   */
  public static boolean doHuffman = false;
  
  /**
   * Map from an expression chord to the weight of its expression tree.
   */
  private HashMap<Expr, Integer> weights;

  /**
   * Map from expressions to their heights.
   */
  private Class[]  argTypes;
  private Object[] args;
  private boolean  inSSA;

  /**
   * @param scribble is the CFG transformed by tree height reduction
   */
  public TreeHeight(Scribble scribble)
  {
    super(scribble, "_th");

    assert setTrace(classTrace);
    
    if (doHuffman)
      weights = new HashMap<Expr, Integer>(31);

    try {
      argTypes = new Class[3];
      argTypes[0] = Class.forName("scale.clef.type.Type");
      argTypes[1] = Class.forName("scale.score.expr.Expr");
      argTypes[2] = Class.forName("scale.score.expr.Expr");
    } catch (java.lang.Exception ex) {
      throw new scale.common.InternalError("Missing classes.");
    }

    args = new Object[3];
  }

  /**
   * Find and reduce the height of expression trees
   */
  public void perform()
  {
    Stack<Chord> wl    = WorkArea.<Chord>getStack("perform TreeHeight");
    Chord        start = scribble.getBegin();

    inSSA = (scribble.inSSA() != Scribble.notSSA);

    wl.push(start);
    Chord.nextVisit();
    start.setVisited();

    while (!wl.isEmpty()) {
      Chord s = wl.pop();
      s.pushOutCfgEdges(wl);

      // Balance expressions used by this CFG node.
      
      if (!s.isAssignChord()) {
        balanceSubExprs(s);
        continue;
      }
      
      ExprChord ec = (ExprChord) s;
      if (!(ec.getLValue() instanceof LoadDeclAddressExpr)) {
        balanceSubExprs(ec);
        continue;
      }

      Expr rval = ec.getRValue();
      if (!(rval instanceof BinaryExpr)) {
        balanceExpr(rval);
        continue;
      }

      BinaryExpr brval = (BinaryExpr) rval;
      if (!brval.isAssociative()) {
        balanceExpr(rval);
        continue;
      }

      Expr use = singleRealUse(ec);
      if (use == null) { // Multiple uses
        balanceExpr(rval);
        continue;
      }

      if (brval.getClass() != use.getOutDataEdge().getClass()) {
        balanceExpr(rval);
        continue;
      }
    }

    WorkArea.<Chord>returnStack(wl);

    scribble.recomputeRefs();
  }

  /**
   * Balance all subexpressions of this node.
   * @param n is the node under which to balance.
   */
  private void balanceSubExprs(Note n)
  {
    int nde = n.numInDataEdges();
    for (int i = 0; i < nde; i++)
      balanceExpr(n.getInDataEdge(i));
  }
  
  /**
   * Return the number of uses that aren't simply copies to a variable
   * that is never used again.  This is hack-ish, but increases the
   * effectiveness of the optimization at little cost.
   */
  private int numRealUses(ExprChord ec)
  {
    int ndu = ec.numDefUseLinks();
    int nru = ndu;
    for (int i = 0; i < ndu; i++) {
      Chord use = ec.getDefUse(i).getChord();
      if (!use.isAssignChord())
        continue;
      ExprChord usec = (ExprChord) use;      
      if ((usec.getLValue() instanceof LoadDeclAddressExpr) &&
          (usec.numDefUseLinks() < 1))
        nru--;
    }
    return nru;
  }
  
  /**
   * Return the single real use of this definition, or null.
   */
  private Expr singleRealUse(ExprChord ec)
  {
    if (numRealUses(ec) != 1)
      return null;

    int ndu = ec.numDefUseLinks();
    for (int i = 0; i < ndu; i++) {
      LoadExpr use  = ec.getDefUse(i);
      Chord    usec = use.getChord();
      if (usec.isAssignChord()) {
        ExprChord expc = (ExprChord) usec;
        if ((expc.getLValue() instanceof LoadDeclAddressExpr) ||
            (expc.numDefUseLinks() >= 1))
          return use;
      }
    }

    return null;
  }

  private void storeTempOperands(BinaryExpr brval)
  {
    Chord bec = brval.getChord();
    Expr la = brval.getLeftArg();

    balanceExpr(la);

    if (!la.optimizationCandidate()) {
      VariableDecl      decl = genTemp(la.getType().getNonConstType());
      LoadDeclValueExpr ldve = new LoadDeclValueExpr(decl);

      brval.setLeftArg(null);

      ExprChord st = new ExprChord(new LoadDeclAddressExpr(decl), la);

      bec.insertBeforeInCfg(st);

      if (inSSA)
        ldve.setUseDef(st);

      brval.setLeftArg(ldve);
    }

    Expr ra = brval.getRightArg();

    balanceExpr(ra);

    if (!ra.optimizationCandidate()) {
      VariableDecl      decl = genTemp(ra.getType().getNonConstType());
      LoadDeclValueExpr ldve = new LoadDeclValueExpr(decl);

      brval.setRightArg(null);

      ExprChord st = new ExprChord(new LoadDeclAddressExpr(decl), ra);

      bec.insertBeforeInCfg(st);

      if (inSSA)
        ldve.setUseDef(st);

      brval.setRightArg(ldve);
    }
  }
  
  /**
   * Balance a tree of associative binary operators.  The leaves of
   * the tree are any expressions that don't use the same operator as
   * the root.
   * @param root the root of the tree
   * @return the weight of the expression tree
   */
  private void balanceExpr(Expr ec)
  {
    ec = ec.getLow();
    Type type = ec.getCoreType();
    
    if (!(ec instanceof BinaryExpr) ||
        !((BinaryExpr) ec).isAssociative() ||
        !type.isAtomicType() ||
        (type.isRealType() && !fpReorder)) {
      balanceSubExprs(ec);
      return;
    }
    
    // Perform balancing on this associative expression.

    BinaryExpr   root   = (BinaryExpr) ec;
    Stack<Expr>  wl     = WorkArea.<Expr>getStack("balanceExpr");
    Vector<Expr> leaves = new Vector<Expr>();
    
    // Find the leaves; each leaf (Expr) in the worklist is preceded
    // by an integer indicating its depth, so that we can calculate
    // height of the whole expression tree.

    wl.push(root);
    
    while (!wl.isEmpty()) {
      // Look for leaves.

      Expr e = wl.pop().getLow();

      if (root.getClass() == e.getClass()) {
        // The expression is an intermediate node in the operator tree.
        // Keep looking for leaves.

        int l = e.numOperands();
        for (int i = l - 1; i >= 0; i--) // Maintain order of leaves in final expr.
          wl.push(e.getOperand(i));

        continue;
      }

      if (e instanceof LoadDeclValueExpr) {
        // We might be able to "look through" this variable.

        Expr thruExp = e;
        do {
          LoadDeclValueExpr ldve = (LoadDeclValueExpr) thruExp;
          ExprChord         ud   = ldve.getUseDef();
          
          if (ud == null)
            break;
          
          if (doHuffman)
            weights.put(e, new Integer(getWeight(ud.getRValue())));
          
          if (numRealUses(ud) != 1)
            break;

          thruExp = ud.getRValue();
        } while (thruExp instanceof LoadDeclValueExpr);

        if ((thruExp instanceof LoadDeclValueExpr) ||
            (thruExp.getClass() != root.getClass()) ||
            (thruExp.getChord().firstInBasicBlock() != e.getChord().firstInBasicBlock())) {

          // The load is a leaf.
          if (doHuffman)
            insertLeaf(leaves, e);
          else
            leaves.addElement(e);
          continue;
        }

        // We may do better if we store any non-optimizable operands
        // into temporaries.

        storeTempOperands((BinaryExpr) thruExp);

        // This expression isn't used anywhere else, so we treat it
        // as an intermediate node.

        wl.push(thruExp);
        continue;
      }

      // See if we can balance other expressions below this leaf, then
      // add it to the list of leaves.

      balanceExpr(e);

      if (doHuffman)
        insertLeaf(leaves, e);
      else
        leaves.addElement(e);
    }
    
    // Store the weight of this subtree for Huffman balancing.
    
    if (doHuffman) {
      int w = 0;
      for (int i = 0; i < leaves.size(); i++)
        w += getWeight(leaves.get(i));
      weights.put(root, new Integer(w));
    }

    // Build a balanced tree
    
    if (leaves.size() < 4) {
      WorkArea.<Expr>returnStack(wl);
      return;
    }

    Expr arg1 = null;
    Expr arg2 = null;
    
    while (leaves.size() > 2) {
      Expr l1 = leaves.removeElementAt(0);
      Expr l2 = leaves.removeElementAt(0);
      Expr ne = createExpr(root, l1.conditionalCopy(), l2.conditionalCopy());
      if (doHuffman) {
        weights.put(ne, new Integer(getWeight(l1) + getWeight(l2)));
        insertLeaf(leaves, ne);
      } else
        leaves.addElement(ne);
    }
    
    arg1 = leaves.removeElementAt(0);
    arg2 = leaves.removeElementAt(0);

    Expr la  = root.getLeftArg();
    Expr ra  = root.getRightArg();

    int inc = 0;
    if (la != arg1) {
      root.changeInDataEdge(la, arg1.conditionalCopy());
      la.unlinkExpression();
      inc = 1;
    }
    if (ra != arg2) {
      root.changeInDataEdge(ra, arg2.conditionalCopy());
      ra.unlinkExpression();
      inc = 1;
    }

    treeCount += inc;
    WorkArea.<Expr>returnStack(wl);
  }

  /**
   * Returns a binary expression using the same operator type as root.
   * @param root the operator of this expression tree
   * @param la the left argument
   * @param ra the right argument
   */
  private Expr createExpr(BinaryExpr root, Expr la, Expr ra)
  {
    Type type = root.getType();
    if (type.isPointerType() &&
        !la.getType().isPointerType() &&
        !ra.getType().isPointerType())
      type = la.getType();

    Class<? extends BinaryExpr> rootType = root.getClass();
    Expr  newExpr = null;
    try {
      args[0] = type;
      args[1] = la;
      args[2] = ra;
      Method create = rootType.getDeclaredMethod("create", argTypes);
      newExpr = (Expr) create.invoke(null, args);
    } catch (java.lang.Exception ex) {
      System.out.println("** Expression tree height reduction " + ex);
    }

    // Reduce register pressure by shortening the live range of the
    // temporary variables we have created.

    if (inSSA &&
        (la instanceof LoadDeclValueExpr) &&
        (ra instanceof LoadDeclValueExpr)) {
      Chord        posl = la.getUseDef();
      Chord        posr = ra.getUseDef();
      VariableDecl vl   = (VariableDecl) ((LoadDeclValueExpr) la).getDecl();
      VariableDecl vr   = (VariableDecl) ((LoadDeclValueExpr) ra).getDecl();
      if ((posl != null) &&
          (posr != null) &&
          !vl.addressTaken() &&
          !vr.addressTaken()) { // Can't do it if not in SSA form.
        Chord fl = posl.firstInBasicBlock();
        Chord fr = posr.firstInBasicBlock();
        if (fl == fr) {

          // Find the position to insert the def.  We could relax the
          // constraints if we were willing to label the CFG and use
          // the Chord.executionOrder() method.

          Chord pos = posr;
          while (fl != null) {
            if (fl == posl)
              break;
            if (fl == posr) {
              pos = posl;
              break;
            }
            fl = fl.getNextChord();
          }

          // Create a new temporary variable to hold the result of the
          // operation we just created.  Insert the def for it immediately
          // after its arguments are defined.

          VariableDecl        decl = genTemp(newExpr.getType().getNonConstType());
          LoadDeclValueExpr   ldve = new LoadDeclValueExpr(decl);
          LoadDeclAddressExpr ldae = new LoadDeclAddressExpr(decl);
          ExprChord           st   = new ExprChord(ldae, newExpr);

          pos.insertAfterOutCfg(st, pos.getNextChord());
          newExpr = ldve;
          ldve.setUseDef(st);
        }
      }
    }

    return newExpr;
  }
  
  /**
   * Inserts a node into the vector, sorted by increasing Huffman weight.
   * @param v the vector of leaves.
   * @param e the expression node to insert.
   */
  private void insertLeaf(Vector<Expr> v, Expr e)
  {
    int w = getWeight(e);
    
    for (int i = 0; i < v.size(); i++) {
      if (w < getWeight(v.get(i))) {
        v.add(i, e);
        return;
      }
    }
    
    v.add(e);
  }

  /**
   * Returns weight of a subtree if found in the lookup table, otherwise
   * returns 1.
   */
  private int getWeight(Expr e)
  {    
    Integer w = weights.get(e);
    if (w == null) {
      if (e.isLiteralExpr())
        return 0;
      else
        return 1;
    }
    return w.intValue();
  }
  
  /**
   * Return whether this optimization requires that the CFG be in SSA form.
   * It returns either
   * <dl>
   * <dt>NO_SSA<dd>the CFG must not be in SSA form,
   * <dt>IN_SSA<dd>the CFG must be in SSA form,
   * <dt>VALID_SSA<dd>the CFG must be in valid SSA form, or
   * <dt>NA_SSA<dd>the CFG does not need to be in valid SSA form.
   * </dl>
   */
  public int requiresSSA()
  {
    return NA_SSA;
  }
}
