package scale.score.trans;

import java.util.Enumeration;
import java.util.Iterator;
import java.math.*;

import scale.common.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.type.*;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.LiteralMap;

import scale.score.analyses.*;
import scale.score.pred.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.*;

/**
 * Perform the Partial Redundancy Elimination optimization.
 * <p>
 * $Id: PRE.java,v 1.90 2007-10-04 19:58:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a.,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The algorithm is based Fred Chow's PLDI paper,
 * "A New Algorithm for Partial Redundancy Elimination based on SSA Form".
 */
public class PRE extends Optimization
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
  private static int removedOps       = 0;

  private static final String[] stats = {
    "deadCFGNodes",
    "newCFGNodes",
    "removedOperations"};

  static
  {
    Statistics.register("scale.score.trans.PRE", stats);
  }

  /**
   * Return the number of dead nodes removed.
   */
  public static int deadCFGNodes()
  {
    return deadCFGNodeCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * Return the number of operations removed.
   */
  public static int removedOperations()
  {
    return removedOps;
  }

  private static class OperandVersion
  {
    public ExprChord s;
    public int       i;
    public Expr      e0;
    public Expr      e1;
    public Object    v0;
    public Object    v1;

    public OperandVersion(ExprChord s, int i, Expr e0, Expr e1, Object v0, Object v1)
    {
      this.s  = s;
      this.i  = i;
      this.e0 = e0;
      this.e1 = e1;
      this.v0 = v0;
      this.v1 = v1;
    }
  }

  private Vector<ExprChord>      exprPhiPlaces; // All the ExprPhiExpr Chords generated for an expression
  private Vector<OperandVersion> realUses    = new Vector<OperandVersion>(29);
  private Vector<OperandVersion> operandVers = new Vector<OperandVersion>(29);
  private Vector<Expr>           newExprs    = new Vector<Expr>(29);
  private HashSet<Chord>         occurs;                                   // All occurrances of the current expression
  private Stack<Object>          variableVersion0 = new Stack<Object>(); // keep track of the versions of the variables of current expression
  private Stack<Object>          variableVersion1 = new Stack<Object>(); // keep track of the versions of the variables of current expression
  private Stack[]        variableStack       = new Stack[2];       // keep track of the versions of the operands of current expression
  private Stack[]        exprStack           = new Stack[2];
  private ExprChord[]    availableDefinition = new ExprChord[100];
  private VariableDecl[] availableTemporary  = new VariableDecl[100];
  private ExprChord[]    versionToDef        = new ExprChord[100]; // Map the version to definition Chord
  private Vector[]       versionToUses       = new Vector[100];    // Map the version to its uses
  private Chord[]        relabelChord        = new Chord[100];     // Chords that have been labeled with a version number - the entries are not unique.
  private int[]          relabelVersion      = new int[100];       // Version number with which the Chord was labeled

  private References refs;

  private int        round               = 0;
  private int        versionCount        = 0;                  // Keep track of expression versions
  private int        relabelCount        = 0;                  // Number of Chords that have been labeled

  /**
   * @param scribble is the Scribble CFG in SSA form
   */
  public PRE(Scribble scribble)
  {
    super(scribble, "_pr");

    variableStack[0]  = new Stack<Object>();
    variableStack[1]  = new Stack<Object>();
    exprStack[0]      = new Stack<Expr>();
    exprStack[1]      = new Stack<Expr>();
    refs              = scribble.getRefs();
  }

  /**
   * The expression uses the specified version.
   * @param e is the expression
   * @param s contains e
   * @param ver is the version
   */
  @SuppressWarnings("unchecked")
  private void addUse(int ver, Chord s, Expr e)
  {
    if (ver >= versionToUses.length) {
      Vector[] nv = new Vector[ver + 100];
      System.arraycopy(versionToUses, 0, nv, 0, versionToUses.length);
      versionToUses = nv;
    }
      
    Vector<Object> set = versionToUses[ver];
    if (set == null) {
      set = new Vector<Object>(6);
      versionToUses[ver] = set;
    }
    set.addElement(s);
    set.addElement(e);
  }

  @SuppressWarnings("unchecked")
  private void addDef(ExprChord s)
  {
    if (versionCount >= versionToDef.length) {
      ExprChord[] nv = new ExprChord[versionCount + 100];
      System.arraycopy(versionToDef, 0, nv, 0, versionToDef.length);
      versionToDef = nv;
    }
    versionToDef[versionCount] = s;
  }

  private OperandVersion getExprPhiOperandVersion(ExprChord s, int i)
  {
    int l = operandVers.size();
    for (int j = 0; j < l; j++) {
      OperandVersion p = operandVers.elementAt(j);
      if ((p.s == s) && (p.i == i))
        return p;
    }
    return null;
  }

  /**
   * Associate a version with the Chord. 
   * Record the information so that the Chord may be re-labeled when necessary.
   * @param s is the Chord to be labeled
   * @param version is the version number used as the label
   */
  private void labelChord(Chord s, int version)
  {
    if (relabelCount >= relabelChord.length) {
      Chord[] rc = new Chord[relabelCount + 100];
      System.arraycopy(relabelChord, 0, rc, 0, relabelChord.length);
      relabelChord = rc;
      int[] rv = new int[relabelCount + 100];
      System.arraycopy(relabelVersion, 0, rv, 0, relabelVersion.length);
      relabelVersion = rv;
    }
    relabelChord[relabelCount] = s;
    relabelVersion[relabelCount] = version;
    relabelCount++;
    s.setLabel(version);
  }
    
      
  /**
   * Re-associate the version with the Chord.  After domination has
   * been computed, the label value associated with a Chord has been
   * changed.  This method re-labels the Chord with its version
   * number.  Note that a particular Chord could have a different
   * version number associated with different expressions (e.g.,
   * CallFunctionExpr).
   * @param begin the index of the first Chord to be relabeled
   * @param end is the index + 1 of the last Chord to be relabeled
   */
  private void relabelChords(int begin, int end)
  {
    for (int i = begin; i < end; i++) {
      relabelChord[i].setLabel(relabelVersion[i]);
    }
  }

  /**
   * Perform PRE on the SSA form of a Scribble graph.  The algorithm
   * is based Fred Chow's PLDI paper, A New Algorithm for Partial
   * Redundancy Elimination based on SSA Form
   */
  public void perform() 
  { 
    ExpressionList el         = new ExpressionList(); 
    Domination     dom        = scribble.getDomination(); // Force dominator information to be created.
    HashMap<BinaryExpr, Vector<ExprChord>> insertions = new HashMap<BinaryExpr, Vector<ExprChord>>(13); // Map from the original expression to the ExprPhiExpr Chords inserted for it

    versionCount = 0;
    relabelCount = 0;
    round        = 0;

    /* Traverse the CFG and collect all expression occurences. */
 
    Chord        start = scribble.getBegin(); 
    Stack<Chord> wl    = WorkArea.<Chord>getStack("perform PRE");

    wl.push(start); 
    Chord.nextVisit(); 
    start.setVisited(); 
 
    while (!wl.empty()) { // find the binary expressions.
      Chord s = wl.pop(); 
      s.pushOutCfgEdges(wl); 
      s.visit(el); 
    } 
 
    WorkArea.<Chord>returnStack(wl);

    int   numExpressions = el.numExpressions();     // Number of different binary expressions found
    int[] rli            = new int[numExpressions]; // Array of indexes into relabelChords
 
    // Process each different binary expression by inserting the ExprPhiExpr Chords.
    
    Enumeration<Expr> e1 = el.allExprs();
    while (e1.hasMoreElements()) {
      BinaryExpr curExpr = (BinaryExpr) e1.nextElement();

      occurs = el.allOccurs(curExpr); // The set of all Chords containing the current expression
      if (2 > occurs.size())
        continue; // If less than two there is nothing to do.

      exprPhiPlaces = new Vector<ExprChord>(10);
 
      dChanged |= exprPhiInsertion(curExpr); /* Uses dominance frontiers */
 
      computeWillBeAvailable(curExpr);  // Do the renaming.
 
      rli[round++] = relabelCount; // Record which Chords for curExpr were labeled with the version number.
 
      insertions.put(curExpr, exprPhiPlaces);
    }
 
    if (dChanged) {
      scribble.recomputeDominators();
      dom = scribble.getDomination(); // Make sure the computation is done now.
    }
 
    // Process each different binary expression by converting the ExprPhiExpr Chords.
 
    round   = 0;
    dChanged = false;

    Enumeration<Expr> e2 = el.allExprs();
    while (e2.hasMoreElements()) {
      BinaryExpr curExpr = (BinaryExpr) e2.nextElement();
 
      occurs = el.allOccurs(curExpr); // The set of all Chords containing the current expression
      if (2 > occurs.size())
        continue; // If less than two there is nothing to do.
 
      int b = 0;
      if (round > 0)
        b = rli[round - 1];
      relabelChords(b, rli[round]); // Re-associate the version number with the Chord after Dominators have been computed.
 
      exprPhiPlaces = insertions.get(curExpr);
 
      dChanged |= finalizeVisit(curExpr); /* Uses domination */
      round++;
    }
 
    int len = newExprs.size();
    for (int i = 0; i < len; i++) {
      Expr ne = newExprs.elementAt(i);
      if (ne.getOutDataEdge() == null)
        ne.unlinkExpression();
    }

    if (dChanged)
      scribble.recomputeDominators();
  } 

  /**
   * Insert ExprPhi nodes for expressions
   */
  private boolean exprPhiInsertion(BinaryExpr curExpr)
  {
    boolean           changed = false;
    DominanceFrontier df      = scribble.getDominanceFrontier();
    Type              type    = curExpr.getType();
    Declaration       left    = extractVariable(curExpr.getLeftArg());
    Declaration       right   = extractVariable(curExpr.getRightArg());
    Stack<Chord>      wl      = WorkArea.<Chord>getStack("exprPhiInsertion");

    HashSet<Chord>        dfPhis  = WorkArea.<Chord>getSet("exprPhiInsertion");
    HashSet<PhiExprChord> varPhis = WorkArea.<PhiExprChord>getSet("exprPhiInsertion");

    Iterator<Chord> eOccurs = occurs.iterator();
    while (eOccurs.hasNext()) {
      Chord s = eOccurs.next();

      // Compute iterative dominance frontier

      Iterator<Chord> edf = df.getDominanceFrontier(s);
      while (edf.hasNext()) {
        Chord cur = edf.next();
        if (dfPhis.add(cur))
          wl.push(cur);
      }

      while (!wl.empty()) {
        Chord dfn = wl.pop();

        Iterator<Chord> ie = df.getDominanceFrontier(dfn);
        while (ie.hasNext()) {
          Chord cur = ie.next();
          if (dfPhis.add(cur))
            wl.push(cur);
        }
      }

      setVarPhis(s, varPhis, left, right);
    }

    WorkArea.<Chord>returnStack(wl);

    /* Insert ExprPhi chords */

    Iterator<Chord> e = dfPhis.iterator();
    while (e.hasNext()) {
      Chord   s                = e.next();
      Chord   fibb             = s.firstInBasicBlock();
      Chord   blk              = fibb;
      boolean inSameBasicBlock = false;

      /* Check if s is in the same basic block with any element in varPhis */

      do {
        if (varPhis.contains(blk)) {
          inSameBasicBlock = true;
          break;
        }
        if (blk.isLastInBasicBlock())
          break;
        blk = blk.getNextChord();
      } while (true);

      if (inSameBasicBlock)
        continue;

      ExprChord ns = constructExprPhiExpr(type, fibb.numInCfgEdges());

      if (s.isLoopHeader())
        s = s.getNextChord();

      s.insertBeforeInCfg(ns);
      newCFGNodeCount++;
      changed = true;
    }

    Iterator<PhiExprChord> ev = varPhis.iterator();
    while (ev.hasNext()) {
      PhiExprChord s         = ev.next();
      Chord        nextChord = s.getNextChord();
      int          numOps    = s.getPhiFunction().numOperands();
      ExprChord    ns        = constructExprPhiExpr(type, numOps);

      s.insertAfterOutCfg(ns, nextChord);
      newCFGNodeCount++;
      changed = true;
    }

    WorkArea.<Chord>returnSet(dfPhis);
    WorkArea.<PhiExprChord>returnSet(varPhis);

    return changed;
  }

  private Declaration extractVariable(Expr op)
  {
    if (op instanceof LoadExpr)
      return ((LoadExpr) op).getDecl();
    return null;
  }

  /**
   * @param s Chord containing a use of one of the variables from the
   * binary expression
   * @param varPhis the set being constructed
   * @param left the Declaration from the left argument of the binary
   * expression
   * @param right the Declaration from the right argument of the
   * binary expression
   */
  private void setVarPhis(Chord s, HashSet<PhiExprChord> varPhis, Declaration left, Declaration right)
  {
    Vector<Declaration> varList = s.getDeclList();
    if (varList == null)
      return;

    int l = varList.size();
    for (int i = 0; i < l; i++) {
      Declaration d = varList.elementAt(i);
      if ((d != left) && (d != right))
        continue;

      Iterator<Chord> defs = refs.getDefChords(d);
      while (defs.hasNext()) { // enumerate all variables in that Chord
        Chord defChord = defs.next();
        if ((defChord.isPhiExpr()) && !varPhis.add((PhiExprChord) defChord)) {
          setVarPhis(defChord, varPhis, left, right);
        }
      }
    }
  }

  private ExprChord constructExprPhiExpr(Type type, int numOperands)
  {
    Vector<Expr> operands = new Vector<Expr>(numOperands);

    for (int i = 0; i < numOperands; i++)
      operands.addElement(null);

    ExprPhiExpr ne = new ExprPhiExpr(type, operands);
    ExprChord   ns = new ExprChord(ne);
    exprPhiPlaces.addElement(ns);
    newExprs.addElement(ne);
    return ns;
  }


  private void computeWillBeAvailable(BinaryExpr curExpr)
  {
    int l = exprPhiPlaces.size();

    for (int i = 0; i < l; i++) {
      ExprChord   s   = exprPhiPlaces.elementAt(i);
      ExprPhiExpr phi = (ExprPhiExpr) s.getRValue();

      phi.setDownSafe(true);
      phi.setCanBeAvail(true);
      phi.setLater(false);
    }

    rename(scribble.getBegin(), curExpr);

    for (int i = 0; i < l; i++) {
      ExprChord   s   = exprPhiPlaces.elementAt(i);
      ExprPhiExpr phi = (ExprPhiExpr) s.getRValue();

      if (phi.getDownSafe())
        continue;

      int n = phi.numOperands();
      for (int j = 0; j < n; j++)
        resetDownSafe(s, j);
    }

    for (int i = 0; i < l; i++) {
      ExprChord   s   = exprPhiPlaces.elementAt(i);
      ExprPhiExpr phi = (ExprPhiExpr) s.getRValue();

      if (!phi.getDownSafe() && phi.getCanBeAvail()) {
        Expr[] array = phi.getOperandArray();
        for (int j = 0; j < array.length; j++) {
          if (array[j] == null) {
            resetCanBeAvail(s);
            break;
          }
        }
      } else if (phi.getCanBeAvail()) {
        int n = phi.numOperands();
        for (int j = 0; j < n; j++) {
          OperandVersion p = getExprPhiOperandVersion(s, j);
          if (p == null)
            continue; //???
          if ((p.v0 == null) || (p.v1 == null)) {               
            resetCanBeAvail(s);
            break;
          }
        }
      }
    }

    for (int i = 0; i < l; i++) {
      ExprChord   s   = exprPhiPlaces.elementAt(i);
      ExprPhiExpr phi = (ExprPhiExpr) s.getRValue();

      phi.setLater(phi.getCanBeAvail());
    }

    for (int i = 0; i < l; i++) {
      ExprChord   s   = exprPhiPlaces.elementAt(i);
      ExprPhiExpr phi = (ExprPhiExpr) s.getRValue();

      if (!phi.getLater())
        continue;

      int n = phi.numOperands();
      for (int j = 0; j < n; j++) {
        if ((phi.getOperand(j) != null) && hasRealUse(s, j))
          resetLater(s);
      }
    }

    for (int i = 0; i < l; i++) {
      ExprChord   s   = exprPhiPlaces.elementAt(i);
      ExprPhiExpr phi = (ExprPhiExpr) s.getRValue();

      phi.setWillBeAvail(phi.getCanBeAvail() && !phi.getLater());
    }
  }

  @SuppressWarnings("unchecked")
  private void rename(Chord root, BinaryExpr curExpr)
  {
    Vector<ExprChord> pV = new Vector<ExprChord>();

    for (int i = 0; i < 2; i++) {
      Expr   exp = curExpr.getOperand(i);
      Object v   = null;

      if (exp.isLiteralExpr())
        v = ((LiteralExpr) exp).getLiteral();

      variableStack[i].push(v);
      exprStack[i].push(exp);
    }

    HashSet<Chord>   done  = WorkArea.<Chord>getSet("rename PRE");
    Stack<ExprChord> stack = WorkArea.<ExprChord>getStack("rename PRE");
    Stack<Object>    wl    = WorkArea.<Object>getStack("rename PRE");
    Chord.nextVisit();
    root.setVisited();
    wl.push(root);

    while (!wl.empty()) {
      Object o = wl.pop();

      if (!(o instanceof Chord)) { // Reverse pushes
        if (o == this) {
          stack.pop();
          variableVersion0.pop();
          variableVersion1.pop();
        } else if (o instanceof Stack)
          ((Stack) o).pop();

        continue;
      }

      Chord curChord = (Chord) o;

      if (curChord.isExprChord()) {
        ExprChord s   = (ExprChord) curChord;
        checkExprChord(curExpr, s, stack, wl);
      } else if ((curChord instanceof LeaveChord) || (curChord instanceof ExitChord)) {
        if (!stack.empty()) {
          ExprChord s    = stack.peek();
          Expr      sexp = s.getRValue();
          if (sexp instanceof ExprPhiExpr)
            ((ExprPhiExpr) sexp).setDownSafe(false);
        }
      }

      Chord nxt = curChord.getNextChord();
      if (nxt != null) {
        if (!nxt.visited()) {
          wl.push(nxt);
          nxt.setVisited();
        }
      } else {
        int n = curChord.numOutCfgEdges();
        for (int i = 0; i < n; i++) {
          Chord nc = curChord.getOutCfgEdge(i);
          if (!nc.visited()) {
            wl.push(nc);
            nc.setVisited();
          }
        }
      }
      if (!curChord.isLastInBasicBlock())
        continue;

      /**
       * The following logic is more complex than one would expect due
       * to the following circumstance: there exits a CFG node A with
       * more than one out-going CFG edge to the same phi-function CFG
       * node B.  This can happen in when there is a
       * <code>switch</code> statement with multiple cases that are
       * just <code>break</code> statements.  This problem is
       * exacerbated by loop unrolling.
       */

      done.clear();

      Chord[] outEdges = curChord.getOutCfgEdgeArray();
      int     numEdges = outEdges.length;
      for (int i = 0; i < numEdges; i++) {

        // find ExprPhi and update version of its operand

        Chord next = outEdges[i];
        if (!done.add(next))
          continue;

        if (next.numInCfgEdges() <= 1)
          continue; // No Phi functions in this basic block
        int num  = next.numOfInCfgEdge(curChord);

        assert (num > 0) : "Invalid CFG " + curChord;

        pV.removeAllElements();
        collectExprPhiExpr(next, pV);

        for (int k = 0; k < num; k++) {
          int j = next.nthIndexOfInCfgEdge(curChord, k);

          checkExprPhis(pV, j, stack);
        }
      }
    }

    WorkArea.<Chord>returnSet(done);
    WorkArea.<Object>returnStack(wl);
    WorkArea.<ExprChord>returnStack(stack);

    variableStack[0].removeAllElements();
    variableStack[1].removeAllElements();
    exprStack[0].removeAllElements();
    exprStack[1].removeAllElements();
    variableVersion0.removeAllElements();
    variableVersion1.removeAllElements();
  }

  //   private void resetExprPhiExprEdges()
  //   {
  //     int l = exprPhiPlaces.size();
  //     for (int i = 0; i < l; i++) {
  //       ExprChord pc = (ExprChord) exprPhiPlaces.elementAt(i);
  //       ExprPhiExpr phi = (ExprPhiExpr) pc.getRValue();
  //       phi.clearEdgeMarkers();
  //     }
  //   }

  private void collectExprPhiExpr(Chord cur, Vector<ExprChord> pV)
  {
    while (true) { // collect all the ExprPhiExpr in the basic block
      if (cur.isExprChord()) {
        ExprChord s   = (ExprChord) cur;
        Expr      exp = s.getRValue();
        if ((exp instanceof ExprPhiExpr) && exprPhiPlaces.contains(cur))
          pV.addElement(s);
      }
      if (cur.isLastInBasicBlock())
        return;
      cur = cur.getNextChord();
    }
  }

  /**
   * Check the CFG node to see if it does anything that might effect
   * the current set of partial redundant expressions.
   * @param curExpr is the current binary expression
   * @param s is the CFG node to be checked
   * @param stack is the stack of relevant CFG nodes
   * @param wl is the work stack
   */
  @SuppressWarnings("unchecked")
  private void checkExprChord(BinaryExpr       curExpr,
                              ExprChord        s,
                              Stack<ExprChord> stack,
                              Stack<Object>    wl)
  {
    Expr exp = s.getRValue();
    if (exp == null)
      return;

    Object v0 = variableStack[0].peek();
    Object v1 = variableStack[1].peek();

    if ((exp instanceof ExprPhiExpr) && exprPhiPlaces.contains(s)) { // generate new version
      ((ExprPhiExpr) exp).setVersion(versionCount);
      stack.push(s);
      variableVersion0.push(v0);
      variableVersion1.push(v1);
      addDef(s);
      labelChord(s, versionCount);
      versionCount++;
      wl.push(this); // Use this as a marker for reversing the push later.
      return;
    }

    if (occurs.contains(s)) { // real occurrence
      boolean match = !(variableVersion0.empty() || variableVersion1.empty());
      if (match)
        match = ((variableVersion0.peek() == v0) && (variableVersion1.peek() == v1));
      if (match) {
        ExprChord oldChord = stack.peek();
        int       over     = oldChord.getLabel();
        Expr      op       = exp;

        stack.push(s);
        labelChord(s, over);
        variableVersion0.push(variableVersion0.peek());
        variableVersion1.push(variableVersion1.peek());

        addUse(over, s, op);
        wl.push(this); // Use this as a marker for reversing the push later.
      } else {
        if (!stack.empty()) {
          ExprChord rs    = stack.peek();
          Expr      rsexp = rs.getRValue();
          if (rsexp instanceof ExprPhiExpr)
            ((ExprPhiExpr) rsexp).setDownSafe(false);
        }

        // new version for real occurence

        stack.push(s);
        variableVersion0.push(v0);
        variableVersion1.push(v1);
        addDef(s);
        labelChord(s, versionCount);
        versionCount++;
        wl.push(this); // Use this as a marker for reversing the push later.
      }
    }

    for (int i = 0; i < 2; i++) {
      Expr op = curExpr.getOperand(i);

      if (!((op instanceof LoadDeclValueExpr) ||
            (op instanceof LoadDeclAddressExpr)))
        continue;

      Object      versionv = null;
      Object      versionr = null;
      LoadExpr    realExpr = null;
      LoadExpr    le       = (LoadExpr) op;
      MayUse  mu       = le.getMayUse();
      Declaration subsetv  = null;

      if (mu != null) { // Get the virtual variable
        Declaration vd = mu.getDecl().getOriginal();
        if (exp instanceof CallExpr) {
          Enumeration<MayDef> em = ((CallExpr) exp).getMayDef();
          while (em.hasMoreElements()) {
            MayDef     md  = em.nextElement();
            LoadExpr   lex = (LoadExpr) md.getLhs();
            VirtualVar vv  = (VirtualVar) lex.getSubsetDecl();
            if (vv.subsetEquiv((VirtualVar) mu.getSubsetDecl())) {
              versionv = lex.getDecl();
              break;
            }
          }
        }

          MayDef md = s.getMayDef();
          if (md != null) {
            LoadExpr   lex = (LoadExpr) md.getLhs();
            VirtualVar vv  = (VirtualVar) lex.getSubsetDecl();
            if (vv.subsetEquiv((VirtualVar) mu.getSubsetDecl())) {
              versionv = lex.getDecl();
            }
        }
      }

      // If the definition is not of the superset or the same subset,
      // ignore

      // Get the version of variable defined in s, which is the i-th
      // operand of the curExpr.

      if (s.isAssignChord()) {
        Expr lhs = s.getLValue();
        if (lhs instanceof LoadDeclAddressExpr) {
          VariableDecl d = (VariableDecl) ((LoadDeclAddressExpr) lhs).getDecl();

          if (originalDecl(le) == d.getOriginal()) {
            realExpr = new LoadDeclValueExpr(d);
            newExprs.addElement(realExpr);
            realExpr.setUseDef(s);
            versionr = d;
          }
        }
      }

      if (versionv != null) {
        variableStack[i].push(versionv);
        wl.push(variableStack[i]);
        if (versionr != null) {
          exprStack[i].push(realExpr);
          wl.push(exprStack[i]);
        }
      } else if (versionr != null) {
        variableStack[i].push(versionr);
        exprStack[i].push(realExpr);
        wl.push(variableStack[i]);
        wl.push(exprStack[i]);
      }
    }
  }

  private void checkExprPhis(Vector<ExprChord> pV, int pos, Stack<ExprChord> stack)
  {
    int l = pV.size();
    for (int i = 0; i < l; i++) {
      ExprChord es = pV.elementAt(i);
        
      if (variableStack[0].empty() || variableStack[1].empty())
        continue;

      Object         v0  = variableStack[0].peek();
      Object         v1  = variableStack[1].peek();
      OperandVersion opv = new OperandVersion(es,
                                              pos,
                                              (Expr) exprStack[0].peek(),
                                              (Expr) exprStack[1].peek(),
                                              v0,
                                              v1);

      operandVers.addElement(opv);

      if (variableVersion0.empty() || variableVersion1.empty())
        continue;

      if ((variableVersion0.peek() != v0) || (variableVersion1.peek() != v1))
        continue;

      ExprChord ss = stack.peek();
      if (!(ss.getRValue() instanceof ExprPhiExpr)) // Set j-th operand of the ExprPhiExpr expression's hasRealUse to true.
        realUses.addElement(opv);

      // Get a version for ExprPhi jth Operand

      int ver = ss.getLabel();
      assert (ver >= 0) :  "No version " + ss;

      ExprPhiExpr exp  = (ExprPhiExpr) es.getRValue();
      ExprPhiExpr pexp = new ExprPhiExpr(ver);
      Expr        old  = exp.setOperand(pexp, pos);

      if (old != null)
        old.unlinkExpression();

      addUse(ver, es, pexp);
    }
  }

  private void resetDownSafe(ExprChord s, int i)
  {
    if (hasRealUse(s, i))
      return;

    Expr op = s.getRValue().getOperand(i);
    if (op == null)
      return;

    int ver = ((ExprPhiExpr) op).getVersion();
    if (ver < 0)
      return;

    ExprChord def = versionToDef[ver];
    if (def == null)
      return;

    Expr exp = def.getRValue();
    if (exp instanceof ExprPhiExpr) {
      ExprPhiExpr phi = (ExprPhiExpr) exp;
      if (!phi.getDownSafe())
        return;

      phi.setDownSafe(false); // newly added

      int l = exp.numOperands();
      for (int k = 0; k < l; k++) {
        resetDownSafe(def, k);
      }
    }
  }     

  private void resetCanBeAvail(ExprChord sp)
  {
    ExprPhiExpr phi = (ExprPhiExpr) sp.getRValue();
    phi.setCanBeAvail(false);

    int ver = sp.getLabel();
    if (ver >= versionToUses.length)
      return;

    @SuppressWarnings("unchecked")
    Vector<Object> v = versionToUses[ver];
    if (v == null)
      return;

    Enumeration<Object> e = v.elements();
    while (e.hasMoreElements()) {
      ExprChord s    = (ExprChord) e.nextElement();
      Expr      expr = (Expr) e.nextElement();

      if (!(expr instanceof ExprPhiExpr))
        continue;

      int         j    = 0;
      ExprPhiExpr sexp = (ExprPhiExpr) s.getRValue();
      Expr[]      ops  = sexp.getOperandArray();

      for (j = 0; j < ops.length; j++)
        if (ops[j] == expr)
          break;

      if ((j < ops.length) && !hasRealUse(s, j)) {
        Expr old = sexp.setOperand(null, j);
        if (old != null)
          old.unlinkExpression();
        if (!sexp.getDownSafe() && sexp.getCanBeAvail())
          resetCanBeAvail(s);
      }
    }
  }

  private void resetLater(ExprChord sp)
  {
    ExprPhiExpr phi = (ExprPhiExpr) sp.getRValue();
    phi.setLater(false);

    int ver = sp.getLabel();
    if (ver >= versionToUses.length)
      return;

    @SuppressWarnings("unchecked")
    Vector<Object> v = versionToUses[ver];
    if (v == null)
      return;

    Enumeration<Object> e = v.elements();
    while (e.hasMoreElements()) {
      ExprChord s    = (ExprChord) e.nextElement();
      Expr      expr = (Expr) e.nextElement();
      Expr      sexp = s.getRValue();

      if (sexp instanceof ExprPhiExpr) {
        ExprPhiExpr sphi = (ExprPhiExpr) sexp;
        
        if (sphi.getLater())
          resetLater(s);        
      }
    }
  }

  private boolean finalizeVisit(BinaryExpr curExpr)
  {
    Domination dom     = scribble.getDomination();
    boolean    changed = false;
    Chord      root    = scribble.getBegin();
    Vector<ExprChord>  pV      = new Vector<ExprChord>();
    HashSet<ExprChord> saves   = WorkArea.<ExprChord>getSet("finalizeVisit");
    HashSet<Chord>     done    = WorkArea.<Chord>getSet("finalizeVisit");
    HashSet<ExprChord> reloads = WorkArea.<ExprChord>getSet("finalizeVisit");
    Stack<Chord>       wl      = WorkArea.<Chord>getStack("finalizeVisit");

    if (versionCount >= availableTemporary.length) {
      VariableDecl[] nat = new VariableDecl[versionCount + 100];
      ExprChord[]    nad = new ExprChord[versionCount + 100];
      System.arraycopy(availableTemporary, 0, nat, 0, availableTemporary.length);
      System.arraycopy(availableDefinition, 0, nad, 0, availableDefinition.length);
      availableTemporary = nat;
      availableDefinition = nad;
    }

    Chord.nextVisit();
    wl.push(root);
    root.setVisited();

    while (!wl.empty()) {
      Chord curChord = wl.pop();
      curChord.pushOutCfgEdges(wl);

      // Establish the initial definitions for the expression

      if (curChord.isExprChord()) {
        ExprChord ns  = (ExprChord) curChord;
        Expr      exp = ns.getRValue();
        if ((exp instanceof ExprPhiExpr) && exprPhiPlaces.contains(ns)) {
          int         vs  = curChord.getLabel();
          ExprPhiExpr phi = (ExprPhiExpr) exp;
          if (phi.getWillBeAvail()) {
            availableDefinition[vs] = ns;
          }
        } else if (occurs.contains(ns)) {
          int       vs = ns.getLabel();
          ExprChord ss = availableDefinition[vs];

          if (ss != null) {     // Check if s is dominated by ss
            Chord cur = ns;

            do {
              if (cur == ss) { // s is dominated by ss
                if (!(ss.getRValue() instanceof ExprPhiExpr)) {
                  saves.add(ss);
                }
                reloads.add(ns);
                ns = ss;
                break;
              }
              cur = dom.getDominatorOf(cur);
            } while (cur != null);
          }
          availableDefinition[vs] = ns;
        }
      }

      if (!curChord.isLastInBasicBlock())
        continue;

      /**
       * The following logic is more complex than one would expect due
       * to the following circumstance: there exits a CFG node A with
       * more than one out-going CFG edge to the same phi-function CFG
       * node B.  This can happen in when there is a
       * <code>switch</code> statement with multiple cases that are
       * just <code>break</code> statements.  This problem is
       * exacerbated by loop unrolling.
       */

      done.clear();

      Chord[] outEdges = curChord.getOutCfgEdgeArray();
      int     numEdges = outEdges.length;
      for (int i = 0; i < numEdges; i++) {
        Chord next = outEdges[i];
        if (!done.add(next))
          continue;

        // find ExprPhi and update version of its operand

        if (next.numInCfgEdges() <= 1)
          continue; // No Phi functions in this basic block.

        pV.removeAllElements();
        collectExprPhiExpr(next, pV);

	Chord[] in = next.getInCfgEdgeArray();
        for (int k = 0; k < in.length; k++) {
	  if (in[k] == curChord)
	    changed |= replacePhiArgs(curChord, curExpr, next, k, pV, saves);
        }
      }
    }

    WorkArea.<Chord>returnSet(done);
    WorkArea.<Chord>returnStack(wl);

    // Code motion

    Iterator<ExprChord> e = saves.iterator();
    while (e.hasNext()) {
      ExprChord    s   = e.next();
      int          ver = s.getLabel();
      VariableDecl d   = availableTemporary[ver];

      if (d == null) {
        d = genTemp(curExpr.getType().getNonAttributeType(), ver);
        availableTemporary[ver] = d;
      } else
        scribble.invalidSSAForm();

      LoadExpr  rv = new LoadDeclValueExpr(d);
      LoadExpr  lv = new LoadDeclAddressExpr(d);
      Expr      c  = changeExprInChord(s, curExpr, rv);
      ExprChord ns = new ExprChord(lv, c);

      availableDefinition[ver] = ns;

      rv.setUseDef(ns);

      s.insertBeforeInCfg(ns);
      ns.copySourceLine(s);
      s.removeRefs(refs);
      s.recordRefs(refs);
      ns.recordRefs(refs);
      newCFGNodeCount++;
      changed = true;
    }

    WorkArea.<ExprChord>returnSet(saves);

    // Generate the new Phi functions and define the associated variables.

    int l = exprPhiPlaces.size();
    for (int i = 0; i < l; i++) {
      ExprChord    s    = exprPhiPlaces.elementAt(i);
      ExprPhiExpr  phi  = (ExprPhiExpr) s.getRValue();
      PhiExprChord nphi = null;

      if (phi.getWillBeAvail()) {
        int ver = s.getLabel();

        assert (availableTemporary[ver] == null) :
          "availableTemporary(" + ver + ") not null for phi " + s;

        int    n        = phi.numOperands();
        Vector<Expr> operands = new Vector<Expr>(n);

        for (int j = 0; j < n; j++) {
          Expr op = phi.getOperand(j);
          assert (op != null) :
            "Null not replaced " + j + " " + s.numInCfgEdges() + " " + phi;
          phi.setOperand(null, j);
          operands.addElement(op);
        }

        PhiExpr      rv = new PhiExpr(curExpr.getType(), operands);
        VariableDecl d  = genTemp(curExpr.getType().getNonAttributeType(), ver);
        Expr         lv = new LoadDeclAddressExpr(d);

        nphi = new PhiExprChord(lv, rv);

        availableTemporary[ver] = d;
        availableDefinition[ver] = nphi;

        s.insertBeforeInCfg(nphi);
        nphi.recordRefs(refs);
        newCFGNodeCount++;
      }

      exprPhiPlaces.setElementAt(nphi, i);
      s.removeRefs(refs);
      s.removeFromCfg();
      deadCFGNodeCount++;

      changed = true;
    }

    Iterator<ExprChord> loads = reloads.iterator();
    while (loads.hasNext()) {
      ExprChord    s   = loads.next();
      int          ver = s.getLabel();
      VariableDecl d   = availableTemporary[ver];
      ExprChord    se  = availableDefinition[ver];

      assert ((d != null) && (se != null)) : "version " + ver + " no temp " + s;

      LoadExpr rv  = new LoadDeclValueExpr(d);
      Expr     old = changeExprInChord(s, curExpr, rv);

      old.unlinkExpression();
      s.removeRefs(refs);
      s.recordRefs(refs);
      removedOps++;
      rv.setUseDef(se);
    }

    WorkArea.<ExprChord>returnSet(reloads);

    // Fix up the new phi functions now that all the variables have
    // been defined.

    for (int i = 0; i < l; i++) {
      PhiExprChord s = (PhiExprChord) exprPhiPlaces.elementAt(i);

      if (s == null)
        continue;

      PhiExpr phi = s.getPhiFunction();
      int     n   = phi.numOperands();

      for (int j = 0; j < n; j++) {
        Expr op = phi.getOperand(j);
        assert (op != null) : "Undefined in " + s + " for " + curExpr;

        if (!(op instanceof ExprPhiExpr))
          continue;

        int          over = ((ExprPhiExpr) op).getVersion();
        VariableDecl od   = availableTemporary[over];
        ExprChord    def  = availableDefinition[over];

        assert ((def != null) && (od != null)) :
          " op " + j + " " + over + " " + def + " " + od;

        LoadExpr operand = new LoadDeclValueExpr(od);

        operand.setUseDef(def);
        Expr old = phi.setOperand(operand, j);
        if (old != null)
          old.unlinkExpression();
      }
      s.removeRefs(refs);
      s.recordRefs(refs);
    }

    return changed;
  }

  private boolean replacePhiArgs(Chord              curChord,
                                 Expr               curExpr,
                                 Chord              next,
                                 int                pos,
                                 Vector<ExprChord>  pV,
                                 HashSet<ExprChord> saves)
  {
    boolean     changed  = false;
    Chord       before   = curChord;
    Enumeration<ExprChord> exprPhis = pV.elements();
    while (exprPhis.hasMoreElements()) {
      ExprChord   es    = exprPhis.nextElement();
      ExprPhiExpr esexp = (ExprPhiExpr) es.getRValue();

      if (!esexp.getWillBeAvail())
	continue;

      ExprPhiExpr op     = (ExprPhiExpr) esexp.getOperand(pos);
      boolean     insert = (op == null);
      int         ver    = -1;

      if (!insert) {
	ver = op.getVersion();
	assert (ver >= 0) : "No version for " + es;

	ExprChord defs = versionToDef[ver];
	Expr      exp  = defs.getRValue();
	if (exp instanceof ExprPhiExpr) {
	  ExprPhiExpr phi = (ExprPhiExpr) exp;
	  insert = (!hasRealUse(es, pos) && !phi.getWillBeAvail());
	}
      }

      if (!insert) {
	ExprChord ad = availableDefinition[ver];
	if ((ad != null) && !(ad.getRValue() instanceof ExprPhiExpr))
	  saves.add(ad);
	continue;
      }

      // The pos-th operand of the ExprPhi Chord satisfies the
      // insertion criterion.

      OperandVersion p    = getExprPhiOperandVersion(es, pos);
      Expr           rv   = curExpr.copy();
      int            nver = versionCount;
      VariableDecl   d    = genTemp(curExpr.getType().getNonAttributeType(), nver);
      LoadExpr       lv   = new LoadDeclAddressExpr(d);
      ExprChord      news = new ExprChord(lv, rv);

      if (p.e0 instanceof LoadExpr) {
	LoadExpr e0   = (LoadExpr) p.e0;
	LoadExpr left = (LoadExpr) rv.getOperand(0);
	left.setDecl(e0.getDecl());
	left.setUseDef(e0.getUseDef());
	left.addMayUse(null);
      }

      if (p.e1 instanceof LoadExpr) {
	LoadExpr e1    = (LoadExpr) p.e1;
	LoadExpr right = (LoadExpr) rv.getOperand(1);
	right.setDecl(e1.getDecl());
	right.setUseDef(e1.getUseDef());
	right.addMayUse(null);
      }

      news.copySourceLine(next);

      if (before.isLoopPreHeader()) {
	before.insertBeforeInCfg(news);
      } else {
	before.insertAfterOutCfg(news, next);
	before = news;
      }

      news.recordRefs(refs);
      newCFGNodeCount++;
      changed = true;

      addDef(news);
      versionCount++;
      labelChord(news, nver);
      Expr old = esexp.setOperand(new ExprPhiExpr(nver), pos);
      if (old != null)
	old.unlinkExpression();
      es.removeRefs(refs);
      es.recordRefs(refs);

      if (nver >= availableTemporary.length) {
	VariableDecl[] nat = new VariableDecl[nver + 100];
	ExprChord[]    nad = new ExprChord[nver + 100];
	System.arraycopy(availableTemporary, 0, nat, 0, availableTemporary.length);
	System.arraycopy(availableDefinition, 0, nad, 0, availableDefinition.length);
	availableTemporary = nat;
	availableDefinition = nad;
      }

      availableTemporary[nver] = d;
      availableDefinition[nver] = news;
    }
 
    return changed;
  }

  /**
   * Return true if the i-th operand of s has real use.
   */
  private boolean hasRealUse(Chord s, int i)
  {
    Enumeration<OperandVersion> e = realUses.elements();
    while (e.hasMoreElements()) {
      OperandVersion p = e.nextElement();
      if ((p.s == s) && (p.i == i))
        return true;
    }
    return false;
  }

  /**
   * Return the name of original declaration.
   */
  private Declaration originalDecl(LoadExpr le)
  {
    VariableDecl d = (VariableDecl) le.getDecl();
    return d.getOriginal();
  }

  /**
   * Generate a temporary.  The name of the temporary is computed by
   * this routine.
   * @param t the type of the temporary.
   * @param version is the version of the variable
   * @return the declaration for the temporary
   */
  private VariableDecl genTemp(Type t, int version)
  {
    StringBuffer buf = new StringBuffer("_pre");
    buf.append(String.valueOf(round)); // For debugging to distinguish between original expressions
    buf.append("#s_");
    buf.append(String.valueOf(version));

    VariableDecl vd = new VariableDecl(buf.toString(), t);
    vd.setTemporary();
    scribble.addDeclaration(vd);

    return vd;
  }

  private Expr changeExprInChord(Chord thisChord, Expr prototype, Expr val)
  {
    Stack<Note> wl = WorkArea.<Note>getStack("changeExprInChord");

    wl.push(thisChord);

    assert (val != null) : "null val " + thisChord;

    boolean saveuo = LoadExpr.setUseOriginal(true);

    while (!wl.isEmpty()) {     
      Note   curExpr = wl.pop();
      Expr[] array   = curExpr.getInDataEdgeArray();

      for (int pos = array.length - 1; pos >= 0; pos--) {
        Expr exp = array[pos];

        if (prototype.equivalent(exp)) {
          LoadExpr.setUseOriginal(saveuo);
          curExpr.changeInDataEdge(exp, val);
          WorkArea.<Note>returnStack(wl);
          return exp;
        }

        wl.push(exp);
      }
    }

    throw new scale.common.InternalError("Expression " +
                                         prototype +
                                         " not found in " +
                                         thisChord);
  }
}
