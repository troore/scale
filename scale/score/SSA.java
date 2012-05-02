package scale.score;

import java.util.Iterator;

import scale.common.*;

import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.expr.IntLiteral;

import scale.score.chords.*;
import scale.score.pred.*;
import scale.score.expr.*;
import scale.score.analyses.*;
import scale.alias.*;
import scale.alias.steensgaard.*;
import scale.alias.shapirohorowitz.*;

import scale.score.analyses.VirtualVar;

/**
 * This class converts a Scribble CFG into the SSA form of the CFG.
 * <p>
 * $Id: SSA.java,v 1.191 2007-10-04 19:58:19 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * See page 447 in <cite>Modern Compiler Implementation in Java</cite>
 * by Appel, Cambridge, 1998 or Kathryn S. McKinley's CS 710 lecture
 * notes.
 * <p>
 * The algorithm creates new Clef variable declarations for the
 * renamed variables generated for the SSA form.  The new variable
 * declarations are named by appending the original name with a '#'
 * followed by a sequence of digits. Phi operations are inserted into
 * the CFG and reference the renamed variables.
 * <p>
 * Our baseline technique for including alias information in SSA form is
 * described in the paper by Chow et. al.  It helps to understand this
 * paper to understand our extensions.
 * <p>
 * The main contribution by Chow et. al. is the use of virtual variables
 * to represent indirect operations which unifies the treatment of
 * indirect and scalar variables for SSA analyses and optimizations.  The
 * treatment of aliases is based upon the work by Choi et. al. which
 * distinguishes between MayDefs (preserving definitions or weak
 * updates), MustDefs, and MayUses.  Chow et. al. add Chi and Mu
 * operators to model MayDef and MayUse effects, respectively.  The Chi
 * and Mu operators work on virtual variables rather than scalar
 * variables.  Thus, a definition (Chi) of a virtual variable is a
 * definition of a group of actual variables that share similar alias
 * characteristics.  Correspondingly, a Mu operator represents a use.
 * <p>
 * A virtual variable represents a group of program, or user, variables
 * that have similar alias characteristics.  We originally used
 * Steensgaard's alias analysis algorithm to create these groups which we
 * also call abstract locations (abstract location and virtual variable
 * can be used interchangeably).  For example, in the sequence, "p = &a;
 * p = &b", a virtual variable is created to represent "a", "b", and
 * "*p".  This means that, conservatively, any use of "*p" may use "a" or
 * "b".  A very conservative approach may create one virtual variable for
 * every user variable.
 * <p>
 * Our basic algorithm works as follows:
 * <ol>
 * <li> Perform alias analysis to identify the virtual variables
 * <li> Perform a pass over the program and insert Chi and Mu nodes for
 * the virtual variables.
 * <li> Convert to SSA with the virtual variables
 * </ol>
 * In step 2, we insert a Mu node for each use of an aliased user
 * variable.  We insert a Chi node for each definition of an aliased user
 * variable.  We also insert a Chi at function calls for each actual
 * aliased actual parameter.  Using our small example from above, if we
 * create a virtual variable, _v, to represent "a", "b", and "*p" then we
 * add a Mu(_v)/Chi(_v) to each use/definition of "a", "b", or "*p".
 * Note - the Chi(_v) is actually an assignment of the form _v = Chi(_v).
 * <p>
 * When the program is translated in SSA form, the virtual variables as
 * as well as the user variables are renamed.  This means that Phi nodes
 * are created for the virtual variables and the virtual variables obey
 * the single assignment rule of SSA form.
 * <p>
 * The scheme as described above works because each indirect variable
 * (e.g., *p) represents at most one virtual variable (i.e., p points to
 * at most one abstract location).  Unfortunately, the scheme does not
 * work when p may point to multiple locations.  In the Shapiro/Horwitz
 * algorithm, p may point to multiple locations.  We need to extend our
 * alias SSA representation to handle the Shapiro analysis.
 * <p>
 * Again, using the example from above, let's say the Shapiro algorithm
 * partitions the user variables such that "p"->{"a"} or "p"->{"b"}
 * (i.e., "p" points to "a" or to "b", but "a" and "b" are not aliases).
 * This is different from the Steensgaard algorithm in which "p"->{"a",
 * "b"} (i.e., "a" and "b" are aliases).  To incorporate the Shapiro
 * results into SSA form, we need a different scheme to represent the
 * MayUse and MayDefs.  For example, a use/def of "a" is not a use/def of
 * "b", but a use/def of "*p" may be a use/def of "a" or "b".
 * <p>
 * We extend the basic scheme for including alias information by changing
 * the meaning of a virtual variable.  In our new scheme, a virtual
 * variable represents multiple locations and we attach information to
 * the variable to distinguish between the locations.  We assign a
 * superset and subset name to the virtual variable (yes, we need better
 * names).  Essentially, the superset name represents indirect variables
 * (e.g., *p), and the subset name represents user variables.  In our
 * example, we can represent the relations among the variables using set
 * notation, where a virtual variable represents the set {*p, {"a"},
 * {"b"}}.  This means, "a" and "b" belong to distinct subsets but they
 * are members of the same enclosing set which includes "*p".
 * <p>
 * In theory, our extended algorithm for adding alias information to SSA
 * isn't much different that the original.
 * <ol>
 * <li> Perform alias analysis to identify the virtual variables
 * <li> Perform a pass over the program and insert Chi and Mu nodes for
 * the virtual variables.
 * <li> Convert to SSA with the virtual variables
 * </ol>
 * In Step 1, we create virtual variables using the new name scheme.  The
 * number of subset names depends upon the number of categories used in
 * the Shapiro pointer analysis.  In Step 2, inserting the Chi and Mu
 * nodes is slightly different.  We insert a Chi/Mu for a superset
 * virtual variable for a definition/use of an indirect variable.  We
 * insert a Chi/Mu for a subset virtual variable for a definition/use of
 * a user variable.  In Step 3, we also convert a program to SSA, but we
 * ignore the subset names for the purposes of creating the the
 * MayDef/Use links.  The subset names must be preserved for the
 * optimizations to make use of the more precise alias information.
 * <p>
 * Note that the MayDef/Use links (and SSA numbers) ignore the subset
 * name.  But, the optimizations must check for the subset names when
 * propagating information.  Analysis information should only be
 * propagated when the virtual variables are equivalent.  Two virtual
 * variables are equivalent under the following conditions: 1) The subset
 * and superset names are the same, 2) One virtual variable only contains
 * a superset name and both the superset names are the same.
 * <p>
 * The superset and subset distinctions are intended to help the
 * optimization phases.  We only want to propagate information for
 * equivalent virtual variables.  That is, when propagation information
 * associated with subset name, we ignore different subset names but we
 * must perform an appropriate action for a virtual variable with the
 * same subset name or superset name.  This is the main difference
 * between the old scheme and the new scheme. In the old scheme, analysis
 * information is always propagated down may def/use links.  But, in the
 * new scheme, we selectively propagate analysis information down the may
 * def/use links.
 * <p>
 * Using our example, we create 3 virtual variables, _v#p, _v#p.a, and
 * _v#p.b (our implementation actually uses numbers, so it's _v#0.0,
 * _v#0.1 and _v#0.2, but here letters are used to hopefully make things
 * clearer.  The description of constant propagation (CP) below is not
 * meant to match the algorithm in Scale.  Instead, it is an attempt to
 * illustrate how an optimization phase should use our SSA extensions.
 * <p>
 * We use the following program to illustrate the extensions (given that
 * "p"->{"a"}, or "p"->{"b"}).
 * <pre>
 * a = 1;
 * b = 2;
 *   = *p;
 *   = b;
 *   = a;
 * </pre>
 * Using Steensgaard analysis (or Shapiro's analysis with 1 category), we
 * cannot propagate 1 to the use of "a" or 2 to the use of "b".  But,
 * with Shapiro's analysis with multiple categories we can.  After converting
 * to SSA form:
 * <pre>
 * a = 1;  _v#p.a_1 = Chi(_v#p.a)
 * b = 2;  _v#p.b_2 = Chi(_v#p.b_1)
 *   = *p; Mu(_v#p_2)
 *   = b;  Mu(_v#p.b_2)
 *   = a;  Mu(_v#p.a_2)
 * </pre>
 * In the example, CP associates 1 with _v#p.a_1 and _v#p.b_1 after
 * statement 1. The may def-use link for _v#p.a_1 points to the use of
 * _v#p.b_1 at statement 2.  Since the virtual variable at statement 2 is
 * not equivalent to the virtual variable at statement 1, CP does not
 * propagate the value 1 to _v#p.b_1.  Instead, CP associates 2 with
 * _v#p.b_2 and _v#p_2.  Then, CP follows the may def-use links to the Mu
 * nodes at statements 3, 4, 5.  At statement 3, _v#p_2 is 2 so the use
 * of "*p" can be replaced by 2.  The same action occurs at statement 4
 * for "b". At statement 4, "a" can be replaced by 1.
 * <p>
 * If there is control flow:
 * <pre>
 * if ()
 *   a = 1; _v#p.a_1 = Chi(_v#p.a)
 * else
 *   b = 2; _v#p.b_2 = Chi(_v#p.b_1)
 *       _v#p_3 = Phi(_v#p_1,_v#p_2)
 *  = *p;   Mu(_v#p_3)
 *  = b;    Mu(_v#p.b_3)
 *  = a;    Mu(_v#p.a_3)
 * </pre>
 * In this case, CP associates not a constant with _v#p_3, _v#p.a_3, and
 * _v#p.b_3 at the Phi node.  Then, none of the uses can be replaced by a
 * constant.
 * <p>
 * Note - in the presence of control flow, we only need to add Phi
 * functions for superset names because the definition of a superset is
 * equivalent to definitions for each of the subsets.
 * <p>
 * Fred Chow, Sun Chan, Shin-Ming Liu, Raymond Lo and Mark Streich.
 * <cite>Effective Representation of Aliases and Indirect Memory Operations in
 * SSA Form</cite>,  In Compiler Construction, 6th International Conference,
 * CC'96, Linkoping, Sweden, Apr 1996.
 */
public class SSA
{
  private static int newVariableDeclCount = 0; // A count of the new variable declarations created.
  private static int deadVariableCount    = 0; // A count of variables removed because they were coalesced.
  private static int deadCFGNodeCount     = 0; // A count of nodes removed.
  private static int newCFGNodeCount      = 0; // A count of new nodes created.
  private static int varCount             = 0; // A count of variables put in SSA form.
  private static int callSiteCount        = 0; // A count of the call sites in the CFG.
  private static int acnbbCount           = 0; // A count of the number of times we avoided creating a new basic block when exiting SSA form.
  private static int acnbbfCount          = 0; // A count of the number of times we failed to avoid creating a new basic block because of variable coalescing.
  private static int coalescedCount       = 0; // A count of the number of renamed variables that were coalesced.
  private static int notCoalescedCount    = 0; // A count of the number of renamed variables that were not coalesced.

  private static final String[] stats = {"newVariableDecls",
                                         "deadVariables",
                                         "deadCFGNodes",
                                         "newCFGNodes",
                                         "vars",
                                         "callSites",
                                         "acnbbCnt",
                                         "acnbbfCnt",
                                         "coalesced",
                                         "notCoalesced"};

  static
  {
    Statistics.register("scale.score.SSA", stats);
  }

  /**
   * Return the current number of new variable declarations created.
   */
  public static int newVariableDecls()
  {
    return newVariableDeclCount;
  }

  /**
   * Return the count of variables removed because they were coalesced.
   */
  public static int deadVariables()
  {
    return deadVariableCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * Return the number of dead nodes removed.
   */
  public static int deadCFGNodes()
  {
    return deadCFGNodeCount;
  }

  /**
   * Return the number of variables processed.
   */
  public static int vars()
  {
    return varCount;
  }

  /**
   * Return the number of call sites.
   */
  public static int callSites()
  {
    return callSiteCount;
  }

  /**
   * Return the count of the number of times we avoided creating a new
   * basic block when exiting SSA form.
   */
  public static int acnbbCnt()
  {
    return acnbbCount;
  }

  /**
   * Return the count of the number of times we failed to avoid
   * creating a new basic block because of variable coalescing.
   */
  public static int acnbbfCnt()
  {
    return acnbbfCount;
  }

  /**
   * Return the count of the number of times a renamed variable was
   * coalsced.
   */
  public static int coalesced()
  {
    return coalescedCount;
  }

  /**
   * Return the count of the number of times a renamed variable was
   * not coalsced.
   */
  public static int notCoalesced()
  {
    return notCoalescedCount;
  }

  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  /**
   * Specify the level of "extra branch" elimination to apply when
   * removing phi functions.
   */
  public static int rpCase = 2;
  /**
   * Set true to inhibit variable coalescing.  This is provided for
   * debugging the compiler and it is desireable to see the generated
   * code without coalescing.
   */
  public static boolean inhibitCoalescing = false;
  /**
   * Set true to run special back propagation algorithm.
   */
  public static boolean doBackPropagation = true;
  /**
   * Control back propagation.
   */
  public static boolean onlyBPtoPhis = false;

  private boolean trace;

  /**
   * Map of variables to VarDef instances.
   */
  private HashMap<VariableDecl, VarDef> varDefs;    
  /**
   * The set of renamed variables and the original variable..
   */
  private Vector<VariableDecl> renamed;    
  /**
   * the scribble tree.
   */
  private Scribble scribble;   
  /**
   * null or the class to use to process new CFG nodes added..
   */
  private PlaceIndirectOps pio = null; 

  /**
   * Compute the dominance frontier for each node in a CFG.
   * @param scribble is the Scribble graph to be transformed
   * @param pio is null or the class to use to process new CFG nodes added
   * @see DominanceFrontier
   */
  public SSA(Scribble scribble, PlaceIndirectOps pio)
  {
    this.scribble = scribble;
    this.pio      = pio;
    this.varDefs  = new HashMap<VariableDecl, VarDef>(203);
    this.renamed  = new Vector<VariableDecl>(200);

    assert setTrace();
    pio.setTrace(this.trace);
  }

  private boolean setTrace()
  {
    trace = Debug.trace(scribble.getRoutineDecl().getName(), classTrace, 3);
    return true;
  }

  /**
   * Do any additional processing required when a node is added to the
   * CFG while it is in SSA form.  This method does not add use-def
   * links.
   */
  public void addNewNode(Chord s)
  {
    s.visit(pio);
  }

  /**
   * Do any additional processing required when a load of a value is
   * added to the CFG while it is in SSA form.  This method does not
   * add use-def links.
   */
  public void addNewLoad(LoadDeclValueExpr load)
  {
    load.visit(pio);
  }

  /**
   * Do any additional processing required when nodes are added to the CFG 
   * while it is in SSA form.  This method does not add use-def links.
   */
  public void addNewNodes(Vector<Chord> chords)
  {
    int l = chords.size();
    for (int i = 0; i < l; i++)
      chords.elementAt(i).visit(pio);
  }

  /**
   * Build the SSA form with alias information.  Creating SSA form
   * involves several steps.  We assume that alias analysis has been
   * performed and that alias groups have been created.
   * <ol>
   * <li> Assign virtual variables to indirect variables
   * <li> Insert Mu and Chi annotations for all variables
   * <li> Insert phi functions at merge points (the dominance frontier)
   * <li> Rename scalar and virtual variables.
   * <li> Compute zero version (TBD).
   * </ol>
   * The actual implementation determines where to place the phi
   * functions, then renames, and then actually inserts the phi functions.
   */
  public final void buildSSA()
  {
    // Induction variable information obtained syntactically does
    // contain the required use-def links.  Induction variable
    // information obtained from previous SSA forms may not be correct
    // anymore.

    scribble.getLoopTree().newSSAForm();

    Chord          start     = scribble.getBegin();
    HashSet<Chord> callSites = new HashSet<Chord>(23); // A list of all Chords with CallExprs.
    Stack<Chord>   wl        = WorkArea.<Chord>getStack("buildSSA");

    // Add information to the Scribble graph to handle indirect references
    // (i.e., alias information).  We perform a traversal over the graph
    // and annotate it with alias information (Mu and Chi nodes).

    wl.push(start);
    Chord.nextVisit();
    start.setVisited();

    while (!wl.empty()) {
      Chord n = wl.pop();
      if (pio != null)
        n.visit(pio);

      if (n.isExprChord()) { // Record all the call sites
        Expr exp = ((ExprChord) n).getRValue();
        if (exp instanceof CallExpr) {
          callSites.add(n);
          callSiteCount++;
        }
      }

      n.pushOutCfgEdges(wl);
    }
    
    // Since we've added (virtual) variables, we need to recompute use
    // def list.

    scribble.recomputeRefs();

    WorkArea.<Chord>returnStack(wl);

    placePhiFunctions(callSites);

    callSites = null;

    rename(scribble.getBegin());

    // Generate renamed variable set for coalescing phase.

    varDefs = null; // Not of any use after CFG is in SSA form.

    scribble.recomputeRefs();
    scribble.recomputeDominators();
  }

  /**
   * Determine the variables in the alias SSA form that are not for 
   * <i>real</i> occurrences.  We call occurrences of variables in the
   * original program before conversion to SSA form <i>real</i> occurrences.
   * The <i>zero versions</i> are versions of variables that have no
   * real occurrence and whose values come from at least one Chi function
   * with zero or more intervening Phi functions.
   * <p>
   * We recursively define zero versions as follows:
   * <ol>
   * <li>The lhs of a Chi is a zero version if it has no real occurrence.
   * <li>If an operand of a Phi is zero version, the result of the Phi is
   * zero version if it has no real occurrence.
   * </ol>
   */
  private void computeZeroVersions()
  {
    throw new scale.common.NotImplementedError("Compute zero version hasn't been implemented");
  }

  /**
   * Return a new RenamedVariableDecl.
   */
  public final VariableDecl createRenamedVariable(VariableDecl original,
                                                  boolean      addOrig)
  {
    VariableDecl nvf = new RenamedVariableDecl(original);
    nvf.setTemporary();
    newVariableDeclCount++;
    renamed.addElement(nvf);
    if (!original.isVirtual()) {
      scribble.addDeclaration(nvf); // Don't need declarations of virtual variables.
      if (addOrig)
        renamed.addElement(original);
    }
    return nvf;
  }

  /**
   * A class used in the renaming of variable references.  See page
   * 447 in <cite>Modern Compiler Implementation in Java</cite> by
   * Appel, Cambridge, 1998.  or Kathryn S. McKinley's CS 710 lecture
   * notes.
   */
  private final class VarDef 
  {
    private int            count;
    private Stack<Object>  stack;    // A stack of generated declarations and expressions for the generated declarations.
    private VariableDecl   original; // The original Clef declaration.
    private boolean        ignore;   // True if it is a variable that should not be in SSA form.

    public final String toString()
    {
      StringBuffer buf = new StringBuffer("(Vd ");
      buf.append(hashCode());
      buf.append(' ');
      buf.append(original.getName());
      buf.append(' ');
      buf.append(String.valueOf(count));
      buf.append(")");
      return buf.toString();
    }

    public VarDef(VariableDecl original)
    {
      this.original = original;
      this.count    = 1;
      this.stack    = new Stack<Object>();
      this.ignore   = original.isNotSSACandidate();
      this.stack.push(null);
      this.stack.push(original);
    }

    public final VariableDecl pop()
    {
      VariableDecl v = (VariableDecl) stack.pop();
      stack.pop();
      return v;
    }

    public final VariableDecl peek()
    {
      return (VariableDecl) stack.peek();
    }

    public final ExprChord peekExp()
    {
      return (ExprChord) stack.peekd();
    }

    public final VariableDecl push(ExprChord exp)
    {
      VariableDecl nvf = original;

      if (!ignore)
        nvf = createRenamedVariable(original, count == 1);

      stack.push(exp);
      stack.push(nvf);
      count++;

      return nvf;
    }

    public final boolean renamed()
    {
      return (count > 1) && !ignore;
    }

    public int getCount()
    {
      return count;
    }
  }

  /**
   * Place the Phi function nodes in the CFG.
   */
  private void placePhiFunctions(HashSet<Chord> callSites)
  {
    Iterator<Declaration> ea   = scribble.getNonLocalVars(); // Kills domination & reference info.
    References            refs = scribble.getRefs();
    DominanceFrontier     df   = scribble.getDominanceFrontier();
    HashSet<Chord>        aPhi = WorkArea.<Chord>getSet("placePhiFunctions");
    Chord                 cpos = scribble.getBegin().getNextChord();

    while (ea.hasNext()) {
      VariableDecl a = (VariableDecl) ea.next();
      if (a.isNotSSACandidate())
        continue;

      HashSet<Chord> aOrig = null;

      if (a.isVirtual()) {
        a = a.getOriginal();

        VirtualVar av = (VirtualVar) a;
        int        l  = av.numSubsets();
        for (int i = 0; i < l; i++) {
          VariableDecl   sub = av.getSubset(i);
          HashSet<Chord> tmp = refs.getDefChordSet(sub);

          if (aOrig != null)
            aOrig = aOrig.union(tmp);
          else 
            aOrig = new HashSet<Chord>(tmp);
        }

        HashSet<Chord> tmp = refs.getDefChordSet(a);

        if (aOrig != null)
          aOrig = aOrig.union(tmp);
        else
          aOrig = new HashSet<Chord>(tmp);

      } else
        aOrig = refs.getDefChordSet(a);
      
      varCount++;

      // Generate a copy for each variable at the start of the program.

      if (!a.isVirtual()) {
        LoadExpr  l   = new LoadDeclAddressExpr(a);
        LoadExpr  r   = new LoadDeclValueExpr(a);
        ExprChord sto = new ExprChord(l, r);

        cpos.insertBeforeInCfg(sto);
        newCFGNodeCount++;
        aOrig.add(sto);
        if (pio != null)
          sto.visit(pio);
        sto.recordRefs(refs);
      }

      aPhi.clear();

      HashSet<Chord> aW = new HashSet<Chord>(aOrig);

      while (!aW.isEmpty()) {
        Chord n = aW.remove();

        Iterator<Chord> dfn = df.getDominanceFrontier(n);
        while (dfn.hasNext()) {
          Chord aY = dfn.next();

          if (aPhi.add(aY)) {

            // Construct the the PhiExpr and the Chord for it.

            Chord        fibb     = aY.firstInBasicBlock();
            int          ne       = fibb.numInCfgEdges();
            Vector<Expr> operands = new Vector<Expr>(ne);
            for (int k = 0; k < ne; k++) {
              LoadDeclValueExpr ld = new LoadDeclValueExpr(a);
              operands.addElement(ld);
            }

            PhiExpr      phi = new PhiExpr(a.getType(), operands);
            LoadExpr     lhs = new LoadDeclAddressExpr(a);
            PhiExprChord s   = new PhiExprChord(lhs, phi);

            if ((pio != null) && !a.isVirtual())
              s.visit(pio);

            phi.clearEdgeMarkers();
            s.recordRefs(refs);

            Chord ins  = aY;
            if (ins.isLoopHeader())
              ins = ins.getNextChord();

            ins.insertBeforeInCfg(s);
            newCFGNodeCount++;

            if (!aOrig.contains(aY))
              aW.add(aY);
          }
        }
      }
    }

    WorkArea.<Chord>returnSet(aPhi);
  }

  private VarDef getVd(VariableDecl vf)
  {
    VarDef vd = varDefs.get(vf);
    if (vd == null) {
      vd = new VarDef(vf);
      varDefs.put(vf, vd);
    }

    return vd;
  }

  private void genName(Expr exp, Stack<Object> wl)
  {
    LoadExpr     le  = (LoadExpr) exp.getReference();
    VariableDecl rvf = (VariableDecl) le.getDecl();
    VarDef       vd  = getVd(rvf);

    ExprChord    se  = (ExprChord) exp.getChord();
    VariableDecl nvf = vd.push(se);

    wl.push(vd);

    if (rvf.isVirtual()) {
      rvf = rvf.getOriginal();
      VirtualVar superset = ((VirtualVar) rvf).getSuperset();
      if (superset != rvf) {
        VarDef svd = getVd(superset);
        svd.push(se);
        wl.push(svd);
      }
      int l = superset.numSubsets();
      for (int i = 0; i < l; i++) {
        VirtualVar sv = superset.getSubset(i);
        if (sv == rvf)
          continue;
        VarDef svd = getVd(sv);
        svd.push(se);
        wl.push(svd);
      }
    }

    le.setDecl(nvf);
  }

  private void replaceName(LoadExpr useExp)
  {
    if (useExp == null)
      return; // An example is a LiteralExpr of an AddressLiteral.
    replaceName(useExp, (VariableDecl) useExp.getDecl());
  }

  private void replaceName(LoadExpr useExp, VariableDecl vf)
  {
    VarDef vd = getVd(vf);

    if (vd.renamed()) {
      VariableDecl nvf    = vd.peek();
      ExprChord    defExp = vd.peekExp();

      useExp.setDecl(nvf);
      useExp.setUseDef(defExp);
    }

    MayUse mu = useExp.getMayUse();
    if (mu != null) {
      VariableDecl vf2 = mu.getDecl();
      VarDef       vd2 = getVd(vf2);

      if (vd2.renamed()) {
        VariableDecl nvf2    = vd2.peek();
        ExprChord    defExp2 = vd2.peekExp();

        mu.setDecl(nvf2);
        mu.setMayDef(defExp2.getMayDef());
      }
    }
  }

  private void insertCopyChordsBefore(Chord x, Iterator<Declaration> esv)
  {
    while (esv.hasNext()) {
      VariableDecl v  = (VariableDecl) esv.next();
      VarDef       vd = getVd(v);

      if (!vd.renamed())
        continue;

      VariableDecl nvf = vd.peek();
      if (nvf == v)
        continue;

      ExprChord def = vd.peekExp();
      LoadExpr  lhs = new LoadDeclAddressExpr(v);
      LoadExpr  rhs = new LoadDeclValueExpr(nvf);
      ExprChord st  = new ExprChord(lhs, rhs);

      x.insertBeforeInCfg(st);
      st.copySourceLine(x);
      newCFGNodeCount++;
      rhs.setUseDef(def);
    }
  }

  /**
   * Return true if this declaration has a purity level of PUREGV.
   */
  private boolean isPure(Declaration d)
  {
    if (d == null)
      return false;
    return d.isPure();
  }

  /**
   * Generate new declarations (rename) for all of the variable references.
   * @param s statement to have variable references renamed
   * @param done set of statements that have been processed
   */
  private void rename(Chord start)
  {
    References   refs = scribble.getRefs();
    Stack<Object> wl   = WorkArea.<Object>getStack("rename");

    Chord.nextVisit();
    start.setVisited();
    wl.push(start);

    while (!wl.empty()) {
      Object o = wl.pop();

      if (o instanceof VarDef) { // Pop the VarDefs that were pushed by X
        VarDef vd = (VarDef) o;
        vd.pop();
        continue;
      }

      Chord X = (Chord) o;

      if (X.isPhiExpr()) { // Generate new name for the result variable of the Phi function.
        genName(((ExprChord) X).getLValue(), wl);
      } else {
        Expr set = null;
        Expr lhs = X.getDefExpr();
        if (lhs instanceof LoadDeclAddressExpr)
          set = lhs;

        replaceNames(X, set); // Rename variables at use sites.
        if (set != null) // Generate new names at def sites.
          genName(set, wl);
        X.recordRefs(refs); // Rebuild references at this site because of the name change.
      }

      // Process Phi functions in the successors to X.

      if (X.isLastInBasicBlock()) {
        int l = X.numOutCfgEdges();
        for (int i = 0; i < l; i++) {
          Chord   Y     = X.getOutCfgEdge(i);
          Chord   fibb  = Y;
          Chord[] fin = fibb.getInCfgEdgeArray();

          // Note - there might be two or more in-coming edges to Y
          // from the same Chord (X).

          while (true) { // Scan the basic block for Phi functions
            if (Y.isPhiExpr()) {
              PhiExprChord s       = (PhiExprChord) Y;
              PhiExpr      phi     = s.getPhiFunction();
              for (int j = 0; j < fin.length; j++) {
                Chord    next = fin[j];
                LoadExpr vfe  = (LoadExpr) phi.getOperand(j);

                if ((X == next) && !phi.edgeMarked(j)) {
                  replaceName(vfe);
                  phi.markEdge(j);
                }
              }
            }
            if (Y.isLastInBasicBlock())
              break;
            Y = Y.getNextChord();
          }
        }
      }

      // Insert statements to copy into static at exits from the program.

      if (X instanceof EndChord) {
        SequentialChord S = (SequentialChord) X;
        insertCopyChordsBefore(X, refs.getStaticVars());
      }

      // Rename the variables in the children of this CFG node.

      Chord nxt = X.getNextChord();
      if (nxt != null) {
        if (!nxt.visited()) {
          wl.push(nxt);
          nxt.setVisited();
        }
      } else {
        int n = X.numOutCfgEdges();
        for (int i = 0; i < n; i++) {
          Chord nc = X.getOutCfgEdge(i);
          if (!nc.visited()) {
            wl.push(nc);
            nc.setVisited();
          }
        }
      }
    }

    scribble.inSSAForm();

    varDefs = null;

    WorkArea.<Object>returnStack(wl);
  }

  private void replaceNames(Chord c, Expr set)
  {
    Vector<LoadExpr> expList = c.getLoadExprList();
    if (expList == null)
      return;

    int l = expList.size();
    for (int i = 0; i < l; i++) {
      LoadExpr le = expList.elementAt(i);
      if (le == set)
        continue;

      Declaration decl = le.getDecl();
      if (!decl.isVariableDecl())
        continue;

      replaceName(le);
    }
  }

  /**
   * Create may-def expressions for the pointed-to objects by a global
   * variable around a function call.
   */
  private void definePointers(CallExpr ce, Declaration d)
  {
    AliasAnnote annote = (AliasAnnote) d.getAnnotation(AliasAnnote.annotationKey());
    AliasVar    av     = annote.getAliasVar();

    if (av == null)
      return;

    ECR.nextVisit();
       
    Vector<ECR> v = new Vector<ECR>(1);
    av.allPointsTo(v);

    int l = v.size();

    for (int i = 0; i < l; i++) {
      ECR        e  = v.elementAt(i);
      VirtualVar vv = pio.getVirtualVar(e);
      assert (vv != null) : "Where is the virtual variable for the ECR?";
      if (!ce.inMayDef(vv)) { 
        ce.addMayDef(createMayDefInfo(ce, vv));
        ce.addMayUse(createMayUseInfo(ce, vv));
      }
    }
  }

  /**
   * Create a may definition expression to repesent the aliasing
   * characteristics of an expression.
   * @param expr the expression that the information is added to
   * @param v the virtual variable that the may def is associated with
   * @return the may definition expression
   */
  protected MayDef createMayDefInfo(Expr expr, VirtualVar v)
  {
    LoadDeclValueExpr   rhs    = new LoadDeclValueExpr(v);
    LoadDeclAddressExpr lhs    = new LoadDeclAddressExpr(v);
    MayDef              mayDef = new MayDef(lhs, rhs);

    return mayDef;
  }

  /**
   * Create a may use expression to repesent the aliasing characteristics
   * of an expression.
   *
   * @param expr the expression that the information is added to
   * @param v the virtual variable that the may def is associated with
   * @return the may use expression
   */
  protected MayUse createMayUseInfo(Expr expr, VirtualVar v)
  {
    MayUse mayUse = new MayUse(v);

    return mayUse;
  }

  /**
   * Remove any PhiExprs still in the Scribble graph.  <p> This uses a
   * different algorithm from the Briggs algorithm to handle both the
   * "Lost Copy" and "Swap" problem when removing Phi functions from
   * SSA form.
   * <p>
   * This is my algoritm for removing phi functions from a CFG in SSA
   * form It is cheaper than the algorithm in Briggs, et al for our
   * representation of basic blocks.  The Briggs algorithm stops at
   * each basic block and looks for phi functions in each of the
   * block's successors.  It then inserts copies at the end of the
   * block for each phi function in each of the block's successors.
   * For our representation, this means that a particular block is
   * scanned from beginning to end once per in-coming CFG edge.
   * <p>
   * In my algorithm, I do the insert of the copy statements in the
   * predecessors of the block as in the naive algorithm used by
   * Cytron.  This eliminates the multiple scan of each basic block
   * for its phi functions.
   * <p>
   * Before the inserts are done, I order the copy statements to
   * eliminate the "Lost Copy" problem.  Then, to eliminate the "Swap"
   * problem, when the destination of one phi function is used as the
   * source of another phi function and that second phi function's
   * destination is also used by another phi function, I insert copies
   * to temporaries and copies from temporaries.
   * <p>
   * For example, consider the phi functions
   * <pre>
   *   a1 <- phi(a2, b1)
   *   c1 <- phi(c2, c3)
   *   b1 <- phi(b2, a1)
   *   x1 <- phi(x2, a1)
   * </pre>
   * These are ordered first for the second in-coming CFG edge as
   * <pre>
   *   c1 <- phi(c2, c3)
   *   x1 <- phi(x2, a1)
   *   a1 <- phi(a2, b1)
   *   b1 <- phi(b2, a1)
   * </pre>
   * because the x1 and c1 phi function destinations are not used by
   * another phi function.  Then, copies are inserted along the two
   * in-coming CFG edges as
   * <pre>
   *   left edge          right edge
   * ----------------------------------
   *   a1 <- a2            c1 <- c3
   *   c1 <- c2            x1 <- a1
   *   b1 <- b2         a_t_1 <- b1        
   *   x1 <- x2         b_t_1 <- a1      
   *                       a1 <- a_t_1      
   *                       b1 <- b_t_1       
   * </pre>
   * This algorithm is more conservative than necessary because it
   * assumes that if a phi function has a destination that is used by
   * another phi function whose destination is used by ..., then the
   * two phi functions MAY exhibit the "Swap" problem.  It would be
   * possible, with more effort, to order these phi functions to
   * eliminate the need to use some of the additional temporaries in
   * some cases.  For example, in the case
   * <pre>
   *   x1 <- phi(x2, x3)
   *   y1 <- phi(y2, x1)
   *   z1 <- phi(z2, y1)
   * </pre>
   * the copies for the second edge would be
   * <pre>
   *      z1 <- y1
   *   x_t_0 <- x3
   *   y_t_0 <- x1
   *      x1 <- x_t_0
   *      y1 <- y_t_0
   * </pre>
   * This could be reduced to
   * <pre>
   *      z1 <- y1
   *   x_t_0 <- x3
   *      y1 <- x1
   *      x1 <- x_t_0
   * </pre>

   * I believe that this case will be rare enough that the cost of
   * detecting it is not warranted and that the extra copies will not
   * be a problem.
   * <br>  James H. Burrill, February 3, 1999
   */
  public final void removePhis()
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("removePhis");

    doBackPropagation(wl);

    Chord          begin    = scribble.getBegin();
    Vector<PhiExprChord> phis = new Vector<PhiExprChord>(10); // All the PhiExprChords in a basic block.
    boolean        changed  = false;
    int            count    = 0;
    Stack<Chord>   dead     = WorkArea.<Chord>getStack("removePhis");
    int            maxnp    = 0;
    Expr[]         source   = null;           // dest <- source
    LoadExpr[]     dest     = null;           // dest <- source
    boolean[]      temp     = null;           // true if a copy to a temporary is needed
    VariableDecl[] td       = null;           // Temporary variable declaration
    int[]          order    = null;           // Order in which to insert copies
    Vector<Chord>  copyNode = new Vector<Chord>();

    Chord.nextVisit();
    begin.setVisited();

    // The work list will contain all of the first Chords in basic
    // blocks.

    wl.push(begin);

    while (!wl.empty()) {
      Chord   s    = wl.pop();
      Chord   fibb = s;                        // First Chord in basic block.
      Chord[] fin  = fibb.getInCfgEdgeArray();
      int     ne   = fin.length;               // Number of in-coming CFG edges to the basic block.

      // Skip to the end of the basic block while collecting Phi
      // functions.

      phis.removeAllElements();

      while (true) {
        if (s.isPhiExpr()) {
          PhiExprChord se = (PhiExprChord) s;
          LoadExpr     le = (LoadExpr) se.getLValue();
          VariableDecl d  = (VariableDecl) le.getDecl();

          if ((d != null) && !d.isVirtual())
            phis.addElement(se);

          dead.push(s);
        } else if (s instanceof NullChord) {
          dead.push(s);
        }

        if (s.isLastInBasicBlock())
          break;
        s = s.getNextChord();
      }

      // We are now at the end of the basic block and have collected
      // all of the Phi functions in the basic block.

      // Put the successor basic blocks on the work list.

      s.pushOutCfgEdges(wl);

      int np = phis.size(); // Number of phi functions.
      if (np <= 0)
        continue;

      // Insert the copy statements.

      if (np > maxnp) { // Avoid new allocations if possible.
        maxnp  = np;
        source = new Expr[np];         // dest <- source
        dest   = new LoadExpr[np];     // dest <- source
        temp   = new boolean[np];      // true if a copy to a temporary is needed
        td     = new VariableDecl[np]; // Temporary variable declaration
        order  = new int[np];          // Order in which to insert copies
      }
      changed = true;

      for (int i = 0; i < ne; i++) { // Insert copies for each in-coming CFG edge
         for (int j = 0; j < np; j++) { // Find the phi function operand corresponding to the edge
          PhiExprChord pse = phis.elementAt(j);
          PhiExpr      pe  = (PhiExpr) pse.getRValue();

          dest[j]   = (LoadExpr) pse.getLValue().copy();
          source[j] = pe.getOperand(i).copy();
          temp[j]   = false;
        }

        Chord   inEdge = fin[i];
        Chord   exit   = null;
        boolean isTail = inEdge.isLoopTail();
        boolean acnbb  = false; // True if we can avoid creating a new basic block at the end of the loop.

        if (isTail && (inEdge.numInCfgEdges() == 1) && (rpCase > 0)) {
          Chord p = inEdge.getFirstInCfgEdge();

          acnbb = (p instanceof IfThenElseChord); // Loop test is at the end of the loop.

          if (acnbb) {
            LoopHeaderChord lh = inEdge.getLoopHeader();
            if (lh.numLoopExits() > 1)
              acnbb = false; // Too many exits from the loop.
            else {
              LoopExitChord le = lh.getFirstExit();
              if ((le != null) && (le.firstInBasicBlock().numInCfgEdges() != 1))
                acnbb = false; // Phi function at exit causes problems.
            }
          }

          // If the loop exit test is at the bottom of the loop,
          // inserting the copies between it and the LoopTailChord
          // creates a new basic block.  This causes the loop to have
          // one branch to exit the loop and another to go to the top
          // of the loop.  If we can insert the copies before the loop
          // exit test, one of the branches is eliminated.

          // We can avoid creating the new basic block if we insert
          // two copies.  For example, if
          //   i2 = i1
          // is normally inserted, we can insert
          //   t0 = i2;
          //   i2 = i1
          // and then use t0 in place of i2 outside of the loop.

          if (acnbb) {
            for (int j = 0; j < np; j++) {
              // Moving the copy might prevent the name from being
              // coalesced.  So, if any of the variables has its
              // address taken, we can't do it.
              if (dest[j].getDecl().addressTaken()) {
                acnbb = false;
                acnbbfCount++;
                break;
              }
            }
          }

          if (acnbb) {
            IfThenElseChord ifc = (IfThenElseChord) p;
            exit = ifc.getTrueCfgEdge();
            if (exit == inEdge)
              exit = ifc.getFalseCfgEdge();

            if (rpCase == 1) { // Don't do it if any induction variable requires a temporary to be created.
              for (int j = 0; j < np; j++) { // Find the phi function operand corresponding to the edge
                PhiExprChord    pse  = phis.elementAt(j);
                LoadExpr[]      ud   = pse.getDefUseArray();
                LoopHeaderChord loop = pse.getChord().getLoopHeader();

                if (usedOutsideOfLoop(ud, inEdge, exit, loop)) {
                  acnbb = false;
                  acnbbfCount++;
                  break;
                }
              }
            }
          }

          if (acnbb) { // Yes - we are still going to do it.
            if (p.numInCfgEdges() != 1) { // Force node p to have only one in-coming CFG edge.
              NullChord nc = new NullChord();
              p.insertBeforeInCfg(nc);
              newCFGNodeCount++;
            }
            acnbbCount++;
            inEdge = p;
          }
        }

        // Determine if temporaries are needed.

        int lf = 0;
        int ll = np;

        for (int j = 0; j < np; j++) {
          VariableDecl d = (VariableDecl) dest[j].getDecl();
          int k;
          for (k = 0; k < np; k++) {
            if (source[k].containsDeclaration(d))
              break;
          }
          if (k < np)
            order[--ll] = j; // Put the phi functions, whose destinations are used, at the end.
          else
            order[lf++] = j;
        }

        // Temporaries are needed if one phi function destination is
        // used by a phi function whose destination is used.  This is
        // more conservative than is strictly necessary.

        for (int m = lf; m < np; m++) {
          int          j = order[m];
          VariableDecl d = (VariableDecl) dest[j].getDecl();
          for (int n = lf; n < np; n++) {
            int k = order[n];
            if (source[k].containsDeclaration(d)) {
              temp[j] = true; // My destination is used as a source before
              break;
            }
          }
        }

        // Insert copies into the i-th in-coming CFG edge

        copyNode.removeAllElements();

        for (int m = 0; m < np; m++) {
          int      j   = order[m];
          LoadExpr dst = dest[j];
          Expr     src = source[j];

          if ((src instanceof LoadDeclValueExpr) &&
              (dst.getDecl() == ((LoadDeclValueExpr) src).getDecl())) {
            temp[j] = false;
            continue; // No need to create a useless copy.
          }

          if (acnbb) {
            ExprChord       pse  = (ExprChord) phis.elementAt(j);
            LoadExpr[]      ud   = pse.getDefUseArray();
            LoopHeaderChord loop = pse.getChord().getLoopHeader();

            if (usedOutsideOfLoop(ud, inEdge, exit, loop)) { // Create a new variable to hold a copy of the value.
              VariableDecl      nd = genNewTemp(dst, count++);
              LoadDeclValueExpr ld = new LoadDeclValueExpr(dst.getDecl());
              Chord             cc = new ExprChord(new LoadDeclAddressExpr(nd), ld);

              copyNode.addElement(cc);
              newCFGNodeCount++;

              for (int n = 0; n < ud.length; n++) {
                LoadExpr x = ud[n];
                Chord    c = x.getChord();

                if (c == null)
                  continue;

                if ((c == inEdge) || (c == exit) || c.inBasicBlock(exit)) {
                  x.setDecl(nd);
                  continue;
                }

                LoopHeaderChord lh = c.getLoopHeader();
                if ((lh != null) && !lh.isSubloop(loop)) {
                  x.setDecl(nd);
                  continue;
                }
              }
            }
          }

          if (temp[j]) { // Create a variable to use as a temporary destination.
            VariableDecl nd = genNewTemp(dst, count++);
            td[j] = nd;
            dst = new LoadDeclAddressExpr(nd);
          }

          ExprChord cc = new ExprChord(dst, src);
          copyNode.addElement(cc);
          newCFGNodeCount++;
        }

        // After all the copies, it may be necessary to copy from any
        // temporaries created.

        for (int m = 0; m < np; m++) {
          int  j = order[m];
          if (temp[j]) {
            LoadDeclValueExpr src = new LoadDeclValueExpr(td[j]);
            Chord             cc  = new ExprChord(dest[j], src);
            copyNode.addElement(cc);
            newCFGNodeCount++;
          }
        }

        // Link copies together in one chain.

        int n = copyNode.size();
        if (n <= 0)
          continue;

        SequentialChord f = (SequentialChord) copyNode.elementAt(0);
        SequentialChord l = f;
        for (int m = 1; m < n; m++) {
          SequentialChord cc = (SequentialChord) copyNode.elementAt(m);
          l.setTarget(cc);
          l = cc;
        }

        // Link chain to CFG.

        f.copySourceLine(inEdge);

        if (isTail) {
          inEdge.changeParentOutCfgEdge(f);
          l.setTarget(inEdge);
          continue;
        }

        if (inEdge.isLoopPreHeader()) {
          inEdge.changeParentOutCfgEdge(f);
          l.setTarget(inEdge);
          continue;
        }

        inEdge.changeOutCfgEdge(fibb, f);
        l.setTarget(fibb);
      }
    }

    // Remove the use-def links - they are no longer valid.

    Chord.nextVisit();
    begin.setVisited();
    wl.push(begin);

    while (!wl.empty()) {
      Chord s = wl.pop();
      s.pushOutCfgEdges(wl);
      s.removeUseDef();
    }

    WorkArea.<Chord>returnStack(wl);

    // Remove any un-needed CFG nodes.

    while (!dead.empty()) {
      Chord s = dead.pop();
      s.removeFromCfg();
      deadCFGNodeCount++;
      changed = true;
    }

    WorkArea.<Chord>returnStack(dead);

    if (changed) {
      scribble.recomputeRefs();
      scribble.recomputeDominators();
      scribble.recomputeLoops();
    }

    scribble.notSSAForm();
  }

  /**
   * Propagate phi function results to eliminate needless copies.
   * If the operand of a phi function is the target of store,
   * then the target of the store can be replaced by the target 
   * of the phi function.  This eliminates useless copies and helps
   * predicated code.
   * <p>
   * If hyperblocks have been created then, in order to handle the 
   * null problem correctly, only stores of a phi function
   * can be modified.
   * <p>
   * In no case can a phi function at the start of a loop be modified
   * as doing so makes the sequencing of the variable incorrect.
   */
  private void doBackPropagation(Stack<Chord> wl)
  {
    if (!doBackPropagation)
      return;

    References refs = scribble.getRefs();

    Chord first = scribble.getEnd();
    Chord.nextVisit();
    first.setVisited();
    wl.push(first);

    while (!wl.empty()) {
      Chord s = wl.pop();

      if (s.isLoopExit()) {
        Chord ss = s.getLoopHeader().getLoopTail();
        if (ss != null)
          s = ss;
      }

      s.pushInCfgEdges(wl);

      if (!(s instanceof ExprChord))
        continue;

      ExprChord se = (ExprChord) s;
      Expr      la = se.getLValue();
      if (!(la instanceof LoadDeclAddressExpr))
        continue;

      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) la;
      VariableDecl        a0   = (VariableDecl) ldae.getDecl();
      VariableDecl        a0o  = a0.getOriginal();

      if (se.getPredicate() != null) // If it's predicated we can't do the variable coalescing.
        renamed.remove(a0);

      Expr ra = se.getRValue();
      if (!(ra instanceof PhiExpr))
        continue;

      Chord pfibb = s.firstInBasicBlock();
      if (pfibb.isLoopHeader()) // The phi functions at the start of a loop must not be modified.
        continue;

      PhiExpr phi = (PhiExpr) ra;
      int     ol  = phi.numOperands();
      for (int j = 0; j < ol; j++) {
        Expr exp = phi.getOperand(j);
        if (!(exp instanceof LoadDeclValueExpr))
          continue;

        LoadDeclValueExpr ldve = (LoadDeclValueExpr) exp;
        ExprChord         ud   = ldve.getUseDef();

        if (ud == null)
          continue;

        Chord fibb = ud.getChord().firstInBasicBlock();
        if (fibb.isLoopHeader()) // The phi functions at the start of a loop must not be modified.
          continue;

        LoadDeclAddressExpr t = (LoadDeclAddressExpr) ud.getLValue();
        Expr                r = ud.getRValue();
        if (!(r instanceof PhiExpr) && onlyBPtoPhis)
          continue;

        if (a0o != ((VariableDecl) t.getDecl()).getOriginal())
          continue;  // Obviously, copy propagation changed the variable.

        int ndul = ud.numDefUseLinks();
        if (ndul == 1) {
          t.setDecl(a0);
          se.removeRefs(refs);
          ldve.setDecl(a0);
          se.recordRefs(refs);
        }
      }
    }
  }

  private VariableDecl genNewTemp(LoadExpr dst, int cnt)
  {
    VariableDecl d       = (VariableDecl) dst.getDecl();
    StringBuffer newname = new StringBuffer(d.getName());

    newname.append("_t_");
    newname.append(String.valueOf(cnt));

    VariableDecl nd = new VariableDecl(newname.toString(), d.getType());

    nd.setTemporary();
    nd.setInitialValue(null);
    newVariableDeclCount++;
    scribble.addDeclaration(nd);

    return nd;
  }

  /**
   * Return true if there are any uses of the variable outside of the
   * loop or in the basic block specified or in the node specified.
   */
  private boolean usedOutsideOfLoop(LoadExpr[]      uses,
                                    Chord           node,
                                    Chord           bb,
                                    LoopHeaderChord loop)
  {
    for (int i = 0; i < uses.length; i++) {
      LoadExpr use = uses[i];
      Chord    c   = use.getChord();

      if (c == null)
        continue;

      if ((c == node) || (c == bb) || c.inBasicBlock(bb))
        return true;

      LoopHeaderChord lh = c.getLoopHeader();
      if ((lh != null) && !lh.isSubloop(loop))
        return true;
    }

    return false;
  }

  /**
   * Coalesce variables, created by going into SSA form, into the
   * original variable if there is no interference in the live ranges.
   * See Algorithm 19.17 in "Modern Compiler Implementation in Java"
   * by Appel.
   * <p>
   * In the interest of execution speed, this algorithm does not
   * preserve the may-def information.
   */
  public final void coalesceVariables()
  {
    if (inhibitCoalescing)
      return;

    References refs = scribble.getRefs();

    // Initialize variables with a mark.

    Stack<VariableDecl> wl = WorkArea.<VariableDecl>getStack("coalesceVariables");
    int           l  = renamed.size();
    for (int i = 0; i < l; i++) {
      VariableDecl var = renamed.elementAt(i);
      var.setTag(1); // Mark the variable as not interferring.
      if (!var.isVirtual())
        wl.push(var);
    }

    while (!wl.empty()) {
      VariableDecl var  = wl.pop();
      VariableDecl orig = var.getOriginal();
        if (var == orig)
          continue; // It's the original variable.

      if (var.isVirtual() || var.addressTaken() || orig.inMemory())
        continue;

      if (refs.anyUseChords(var))
        continue; // The variable is used.

      // Remove renamed variables that are not used.

      Iterator<Chord> itd = refs.getDefChords(var);
      while (itd.hasNext()) { // Probably only one.
        Chord s = itd.next();
        if (!s.isAssignChord())
          continue;

        ExprChord se = (ExprChord) s;
        Expr      ra = se.getRValue().getCall(false);
        if (ra != null)
          continue; // Can't remove function calls.

        Expr la = se.getLValue();
        if (!(la instanceof LoadDeclAddressExpr))
          continue;

        if (((LoadExpr) la).getDecl() != var)
          continue;

        // Remove un-needed defs of variables.

        Vector<Declaration> varList = s.getDeclList();
        if (varList != null) {
          int ll = varList.size();
          for (int j = 0; j < ll; j++) {
            Declaration d = varList.elementAt(j);
            if (d instanceof VariableDecl) {
              if (d != var)
                wl.push((VariableDecl) d); // Try this one again.
            }
          }
        }

        s.removeFromCfg();
        itd.remove();
      }

      scribble.removeDeclaration(var);
      deadVariableCount++;
    }

    WorkArea.<VariableDecl>returnStack(wl);

    // Mark CFG nodes where each variable is live.

    Stack<Chord> liveOutAtChord = WorkArea.<Chord>getStack("coalesceVariables");
    for (int i = 0; i < l; i++) {
      VariableDecl var = renamed.elementAt(i);

      if (var.getTag() == 0)
        continue;

      if (var.isVirtual())
        continue;

      Declaration org = var.getOriginal();

      liveOutAtChord.clear();
      Chord.nextVisit();

      Iterator<Chord> es = refs.getUseChords(var);
      while (es.hasNext()) {
        Chord s = es.next();
        assert !s.isPhiExpr() : "Phi function after replace phis?";
        s.pushInCfgEdges(liveOutAtChord);
      }

      /**
       * The logic must handle this case:
       * <pre>
       * A0 =
       * A1 =
       *    = A1
       * A2 =
       *    = A0
       *    = A2
       * </pre>
       * and the case
       * <pre>
       * A0 =
       * A1 =
       *    = A0
       * A2 =
       *    = A2
       *    = A1
       * </pre>
       * where variable are scanned in the order A0, A1, A2.  Note
       * that scanning A2 or A1 does not detect interference but
       * scanning A0 picks up both if the scan of A0 is continued
       * after the first interference with A2 is detected.  Also, if
       * interference is detected with A1, A1 must still be scanned to
       * pick up interference with A2 in the second example.
       */

      while (!liveOutAtChord.empty()) {
        Chord n  = liveOutAtChord.pop();
        Expr  le = n.getDefExpr();

        if (le instanceof LoadDeclAddressExpr) {
          VariableDecl d = (VariableDecl) ((LoadDeclAddressExpr) le).getDecl();

          if (d == var)
            continue;

          if (d.getOriginal() == org) {
            if ((d.getTag() == 1) || (d == org)) {
              boolean interference = true;

              if (n.isAssignChord()) {
                ExprChord se  = (ExprChord) n;
                Expr      rhs = se.getRValue();
                if (rhs instanceof LoadDeclValueExpr) {
                  LoadDeclValueExpr ldve = (LoadDeclValueExpr) rhs;
                  interference = (((VariableDecl) ldve.getDecl()) != var); // Copies don't create interference.
                }
              }

              if (interference) {
                // Record interference between renamed variables of
                // the same original variable only.

                var.setTag(0);
                d.setTag(0);
                if (trace) {
                  System.out.println("** ssa v " + var);
                  System.out.println("       d " + d);
                  System.out.println("       n " + n);
                }
                if (var != org)
                  break;
              }
            }
          }
        }

        n.pushInCfgEdges(liveOutAtChord);
      }
    }

    WorkArea.<Chord>returnStack(liveOutAtChord);
    liveOutAtChord = null;

    HashSet<Chord> mods = WorkArea.<Chord>getSet("coalesceVariables");

    // Do the coalescing.  Record the changes so that the use-def
    // tables may be updated instead of recomputed.

    for (int i = 0; i < l; i++) {
      VariableDecl vn = renamed.elementAt(i);  // Renamed variable
      VariableDecl vo = vn.getOriginal();                     // Original variable

      if (vo == vn)
        continue;

      if (1 == vn.getTag()) { // The variable names may be coalesced.
        replaceVarDecl(vn, vo, refs, mods);
        scribble.removeDeclaration(vn);
        coalescedCount++;
        deadVariableCount++;
        continue;
      }

      if (vn.isRenamed()) { // Eliminate renamed variables.
        assert (!vo.addressTaken() && !vn.addressTaken()) :
          "Not coalesced and address taken - " + vo + " " + vn;

        String       nvn = vn.getOriginal().getName() + "_ssa_" + notCoalescedCount;
        VariableDecl nv  = new VariableDecl(nvn, vn.getType());
        nv.setTemporary();
        scribble.addDeclaration(nv);
        replaceVarDecl(vn, nv, refs, mods);
        notCoalescedCount++;
        scribble.removeDeclaration(vn);
      }
    }

    // Modify the reference information after the nodes have been
    // used.  Otherwise, things get messed up as the underlying
    // set is modified while it is being enumerated.

    Iterator<Chord> em = mods.iterator();
    while (em.hasNext()) {
      Chord s = em.next();
      s.recordRefs(refs);
    }

    renamed = null;
    WorkArea.<Chord>returnSet(mods);
  }

  private void replaceVarDecl(VariableDecl   oldVar,
                              VariableDecl   newVar,
                              References     refs,
                              HashSet<Chord> mods)
  {
    Iterator<Chord> eu = refs.getUseChords(oldVar);
    while (eu.hasNext()) {
      Chord s = eu.next();
      if (s.replaceDecl(oldVar, newVar))
        mods.add(s);
    }
    Iterator<Chord> ed = refs.getDefChords(oldVar);
    while (ed.hasNext()) {
      Chord s = ed.next();
      if (s.replaceDecl(oldVar, newVar))
        mods.add(s);
    }
  }
}
