package scale.score.trans;

import java.util.Enumeration;
import java.util.Iterator;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.pred.References;

import scale.clef.type.Type;
import scale.clef.type.AggregateType;
import scale.clef.type.ArrayType;
import scale.clef.decl.Declaration;
import scale.clef.decl.FieldDecl;
import scale.clef.decl.*;


/**
 * This class replaces references to fields of C structures with
 * references to local variables.
 * <p>
 * $Id: SFIR.java,v 1.25 2007-08-10 14:35:19 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <p>
 * This optimization attempts to replace multiple references to a
 * field of a <code>struct</code> variable with a reference to a
 * surrogate variable (allocated to a register) in the hopes of
 * eliminating references to memory.
 * <p>
 * If the variable is used just within one function, the variable is
 * never referenced as a whole, no aliases are created for any field,
 * and it is not possible for any of the fields of the variable to be
 * referenced outside of the function, each field of the variable may
 * be replaced with a new variable that may be allocted to memory.  In
 * these circumstances, the <code>struct</code> is eliminated all
 * together.
 * <p>
 * Register pressure is always increased because a new variable is
 * introduced that has a live range that is always greater than the
 * live range of the reference to the <code>struct</code> field it
 * represents.
 * <p>
 * A transformation is {@link scale.score.trans#legal legal} if:
 * <ul>
 * <li>the <code>struct</code> variable's address is not taken, and
 * <li>the <code>struct</code> variable is not <code>volatile</code>.
 * </ul>
 * It is {@link scale.score.trans#legal beneficial} if:
 * <ul>
 * <li>more than one reference to a field can be replaced with only
 * one def of the surrogate variable, or
 * <li>one reference to a field can be replaced with a def of the
 * surrogate variable and that def can be placed outside the loop
 * where the reference occurs.
 * </ul>
 */

public class SFIR extends Optimization
{
  private static final int T_ALL = 0;
  private static final int T_VAR = 1; // x.f
  private static final int T_LFA = 2; // x.f1.f2
  private static final int T_IND = 3; // x->f
  private static final int T_ARR = 4; // x[i].f
  private static final int T_LFV = 5; // x.f1->f2
  private static final int T_PTR = 6; // (*x)->f
  private static final int T_OTH = 7; // *(x + exp)->f
  private static final String[] t_refs = {
    "Total   ", "x.f     ", "x.f1.f2", "x->f    ",
    "x[i].f  ", "x.f1->f2", "(*x)->f   ", "Other   "
  };

  private static int specialCount      = 0;
  private static int replacedLoadCount = 0;
  private static int outOfLoopCount    = 0;
  private static int newCFGNodeCount   = 0;

  private static final String[] stats = {
    "replacedLoads",
    "outOfLoops", 
    "newCFGNodes",
    "varsEliminated"
  };

  static
  {
    Statistics.register("scale.score.trans.SFIR", stats);
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

  /**
   * Return the number of struct variables eliminated..
   */
  public static int varsEliminated()
  {
    return specialCount;
  }

  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * If true, use heuristics that prune the cases where the
   * optimization is applied.
   */
  public static boolean useHeuristics = true;

  private boolean    isRecursive; // True if this routine is recursive.
  private Domination dom;
  private References refs;
  private Table[]    tables;
  private int[]      numInst; // Number of occurrences by form.

  public SFIR(Scribble scribble)
  {
    super(scribble, "_sf");
    assert setTrace(classTrace);
  }

  public void perform()
  {
    if (scribble.isIrreducible())
      return;
  
    if (scribble.getSourceLanguage().isFortran())
      return;

    isRecursive = scribble.getRoutineDecl().isRecursive();
    dom         = scribble.getDomination();

    // There is one table for each form.  For the T_VAR and T_IND
    // forms, the table rows are indexed by the struct variable
    // declaration and the column entries are the FieldExpr instances.

    tables = new Table[T_OTH + 1];
    numInst = new int[T_OTH + 1]; // Number of occurrences by form.

    // Collect all references to struct fields. Sort them by form.

    scribble.getLoopTree().labelCFGLoopOrder();

    Stack<Chord> wl    = WorkArea.<Chord>getStack("SFIR perform");
    Stack<Expr>  ewl   = new Stack<Expr>();
    Chord        start = scribble.getBegin();

    numInst = new int[T_OTH + 1]; // Number of occurrences by form.

    Chord.nextVisit();
    wl.push(start);

    while (!wl.empty()) { // For each CFG node.
      Chord c = wl.pop();
      c.setVisited();

      int   l = c.numOutCfgEdges();
      for (int i = 0; i < l; i++)
        c.getOutCfgEdge(i).pushChordWhenReady(wl);

      c.pushInDataEdges(ewl);
      processExpressions(ewl);
    }

    WorkArea.<Chord>returnStack(wl);

    // Handle the easy and infrequent x.f form.

    if (numInst[T_VAR] > 0)
      doSimpleVars(T_VAR, false);

    // Handle the frequent x->f form.

    if (numInst[T_IND] > 0) {
      // We can't safely do the x->f form if we can't tell if the memory
      // location could be changed by some other mechanism.

      int otherCount = numInst[T_PTR] + numInst[T_LFV] + numInst[T_OTH];
      if (((otherCount == 0) || unsafe) && !hasDummyAliases)
        doSimpleVars(T_IND, true);
    }

    // We don't do anything with the other forms.

    if (rChanged)
      scribble.recomputeRefs();

    tables = null;

    if (dChanged)
      scribble.recomputeDominators();
  }

  private void processExpressions(Stack<Expr> ewl)
  {
    while (!ewl.empty()) { // For each CFG node expression.
      Expr exp = ewl.pop();
      exp.pushOperands(ewl);

      if (!(exp instanceof FieldExpr))
        continue;

      FieldExpr fe   = (FieldExpr) exp;
      Expr      se   = fe.getStructure();
      Type      type = se.getPointedToCore();

      ArrayType at = type.getCoreType().returnArrayType();
      if (at != null)
        type = at.getElementType().getCoreType();

      assert type.isAggregateType() : "Not an aggregate type " + type;

      if (type.isUnionType()) // We don't do unions.
        continue;

      while (se instanceof ConversionExpr)
        se = ((ConversionExpr) se).getArg();

      se = se.getLow();

      int cat = classify(se);
      switch (cat) {
      case T_IND: { // x->f
        LoadDeclValueExpr             ldve = (LoadDeclValueExpr) se;
        Declaration                   decl = ldve.getDecl();
        Table<Declaration, FieldExpr> tab  = getTable(cat);
        tab.put(decl, fe);
        numInst[cat]++;
        continue;
      }
      case T_VAR: { // x.f
        LoadDeclAddressExpr           ldae = (LoadDeclAddressExpr) se;
        Declaration                   decl = ldae.getDecl();
        Table<Declaration, FieldExpr> tab  = getTable(cat);
        tab.put(decl, fe);
        numInst[cat]++;
        continue;
      }
      case T_LFA: {
        LoadFieldAddressExpr lfae   = (LoadFieldAddressExpr) se;
        Expr                 struct = lfae.getStructure();
        int                  cat2   = classify(struct);
        if (false && (T_VAR == cat2)) { // x.f1.f2
          LoadDeclAddressExpr           ldae = (LoadDeclAddressExpr) struct;
          Declaration                   decl = ldae.getDecl();
          Table<Declaration, FieldExpr> tab  = getTable(cat2);
          tab.put(decl, fe);
          numInst[cat2]++;
          continue;
        } else if (false && (T_IND == cat2)) { // ((*x)->f1).f2
          LoadDeclValueExpr             ldae = (LoadDeclValueExpr) struct;
          Declaration                   decl = ldae.getDecl();
          Table<Declaration, FieldExpr> tab  = getTable(cat2);
          tab.put(decl, fe);
          numInst[cat2]++;
          continue;
        }
        numInst[cat] += 2;
        continue;
      }
      case T_ARR: // x[i].f
        numInst[cat]++;
        continue;

      case T_LFV: // x.f1->f2
        numInst[cat]++;
        continue;

      case T_PTR: // (*x)->f
        numInst[cat]++;
        continue;

      default:
        numInst[cat]++;
        assert assertTrace(trace, " ", se.getClass().getName());
      }
    }
  }

  /**
   * Process form <code>x.f</code>.
   * @param cat specifies the mapping from struct variable to field
   * references
   * @param indirect is true if the variables contain the address of
   * the structure
   */
  @SuppressWarnings("unchecked")
  private void doSimpleVars(int cat, boolean indirect)
  {
    Table<Declaration, FieldExpr> tab = (Table<Declaration, FieldExpr>) tables[cat];
    if (tab == null)
      return;

    assert assertTrace(trace, "*** do simple", null);

    refs = scribble.getRefs();

    if (!indirect)
      eliminateSpecialVars();

    HashSet<Chord> stops = WorkArea.<Chord>getSet("doSimpleVars");
    Vector<Chord>  lhdom = new Vector<Chord>();
    Chord          end   = scribble.getEnd();
    FieldExpr[]    v     = new FieldExpr[10];

    Enumeration<Declaration> decls = tab.keys();
    while (decls.hasMoreElements()) { // For each struct variable.
      VariableDecl decl = (VariableDecl) decls.nextElement();

      assert assertTrace(trace, "  var ", decl);

      if (decl.addressTaken() || decl.getType().isVolatile())
        continue;

      Object[] els = tab.getRowArray(decl);
      int ke = 0;
      for (int i = 0; i < els.length; i++) {
        Note  exp = (Note) els[i];
        Chord c   = exp.getChord();
        if (c == null)
          continue;
        els[ke++] = exp;
      }

      if (ke < 2)
        continue;

      if (ke < els.length) {
        Object[] en = new Object[ke];
        System.arraycopy(els, 0, en, 0, ke);
        els = en;
      }

      boolean global  = (indirect ||
                         decl.isGlobal() ||
                         (isRecursive && decl.isStatic())); // Inhibit past subroutine calls.
      boolean special = indirect ? false : isSpecialVar(decl, els);

      // We can't do any replacements past a reference to the complete
      // variable.

      stops.clear();
      Iterator<Chord> di = refs.getDefChords(decl);
      while (di.hasNext()) {
        Chord dc = di.next();
        if (indirect)
          stops.add(dc);
        else if (dc.isAssignChord()) {
          ExprChord ec  = (ExprChord) dc;
          Expr      lhs = ec.getLValue();
          if (lhs instanceof LoadDeclAddressExpr) {
            LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
            if (decl == ldae.getDecl()) {
              stops.add(dc);
              special = false;
            }
          }
        }
      }

      if (!indirect) {
        Iterator<Chord> ui = refs.getUseChords(decl);
        while (ui.hasNext()) {
          Chord            dc  = ui.next();
          Vector<LoadExpr> lev = dc.getLoadExprList();
          if (lev != null) {
            for (int i = 0; i < lev.size(); i++) {
              LoadExpr le  = lev.get(i);
              Note     out = le.getOutDataEdge();
              if ((decl == le.getDecl()) && !(out instanceof FieldExpr)) {
                stops.add(dc);
                special = false;
              }
            }
          }
        }
      }

      // If the struct variable is special (i.e., local, non-static,
      // etc), it can be completely replaced by surrogate variables
      // for its fields.

      if (special)
        specialCount++;

      assert trace1(els, stops, global, special, indirect, isRecursive,  decl);

      Optimization.sort(els); // Sort by execution order.

      assert trace2(els);

      main:
      for (int i = 0; i < els.length; i++) {
        // For each reference to a different field.

        FieldExpr fe = (FieldExpr) els[i];
        if (fe == null)
          continue;

        FieldDecl fd = fe.getField();
        if (fd.getType().isVolatile())
          continue;

        assert assertTrace(trace, "       field:", fd.getName());

        v[0] = fe;
        els[i] = null; // We won't look at this one again.

        // Collect all the references to the same field.

        assert trace3(v[0], 0);

        int k = 1;
        for (int j = i + 1; j < els.length; j++) {
          FieldExpr next = (FieldExpr) els[j];
          if (next == null)
            continue;

          FieldDecl fdn = next.getField();
          if (fdn != fd)
            continue;

          if (k >= v.length) {
            FieldExpr[] nv = new FieldExpr[v.length * 2];
            System.arraycopy(v, 0, nv, 0, v.length);
            v = nv;
          }

          assert trace3(next, k);
          v[k++] = next;
        }

        if (!special && (k < 2))
          continue;

        // See if the address of this field is taken.

        if (!special)
          for (int ii = 0; ii < k; ii++) {
            FieldExpr fe2 = v[ii];
            if (fe2 instanceof LoadFieldAddressExpr) {
              Chord c = fe2.getChord();
              if ((c != null) && (!c.isAssignChord() ||
                  !((ExprChord) c).isDefined(fe2)))
                continue main;
            }
          }

        // The array v now contains all references to a specific field
        // of the variable.

        lhdom.clear();

        Chord           first       = fe.getChord();
        Expr            struct      = fe.getStructure();
        LoopHeaderChord lh          = first.getLoopHeader();
        Chord           insert      = null;  // Position to insert the load.
        boolean         firstIsDef  = (fe instanceof LoadFieldAddressExpr);
        boolean         mo          = (struct instanceof LoadDeclAddressExpr);
        boolean         moveOutside = false; // True if the loads can be placed
                                             // before a loop and/or the stores
                                             // don't have to be placed
                                             // immediately after the defs.

        assert assertTrace(trace, " first ", first);
        assert assertTrace(trace, "    fe ", fe);

        if (firstIsDef) { // Find iterative dominance set of the field def.
          dom.getIterativeDominationNF(first, lhdom, global, stops);
          moveOutside = mo && !lh.isTrueLoop() && lhdom.contains(end);
        } else {
          if (lh.isInnerMostLoop() && lh.isTrueLoop()) {
            // See if load of field can be moved outside.

            LoopPreHeaderChord lph = lh.getPreHeader();

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

            dom.getIterativeDominationNF(lph, lhdom, global, stops);

            if (mo && lhdom.contains(end))
              moveOutside = true;
            else {
              moveOutside = true;

              int nle = lh.numLoopExits();
              for (int j = 0; j < nle; j++) {
                if (!lhdom.contains(lh.getLoopExit(j))) {
                  moveOutside = false;
                  insert = null;
                  break;
                }
              }
            }
          }

          if (insert == null) // If no usable LoopPreHeader found.
            insert = first; 
          else if (!lhdom.contains(first)) // If the first use is not dominated by the LoopPreHeader.
            insert = first;

          if (insert != first) {
            outOfLoopCount++; // The load will be moved outside of the loop.
          } else { // Find iterative dominance set of the field use.
            lhdom.clear();
            dom.getIterativeDominationNF(first, lhdom, global, stops);
            moveOutside = mo && !lh.isTrueLoop() && lhdom.contains(end);
          }
        }

        lhdom.add(first);

        assert trace4(k, fe, lhdom, moveOutside, first, stops);

        // Find the references that are dominated by the insert point.

        int scnt    = 0; // Number of stores to field.
        int lcnt    = 0; // Number of loads from field.
        int kk      = 1;
        int numRefs = 1;
        for (int ii = 1; ii < k; ii++) {
          FieldExpr use = v[ii];
          Chord     uc  = use.getChord();

          assert trace5(ii, uc, lhdom);

          if (special || lhdom.contains(uc)) {
            v[kk++] = use;

            numRefs++;

            if (use instanceof LoadFieldAddressExpr)
              scnt++;
            else if (use instanceof LoadFieldValueExpr)
              lcnt++;

            // We do not want to reconsider any references dominated
            // by the first reference.

            for (int jj = i + 1; jj < els.length; jj++) {
              if (use == els[jj])
                els[jj] = null;
            }
          }
        }

        assert trace6(numRefs, fe);

        // Check to see if there are enough references to make it
        // worthwhile.  If it is the "special" case, we don't care, we
        // just want to eliminate the struct variable.  If there is
        // only one reference but it will be moved outside of the
        // loop, we still want to do it.

        if (!special && (lcnt < 2) && (scnt < 2) && !moveOutside)
          continue;

        // All the remaining refs can be replaced by a reference to a
        // surrogate scalar variable.

        VariableDecl vd = genTemp(fd.getType().getNonAttributeType());
        assert assertTrace(trace, "          vd:", vd);

        if (firstIsDef) {
          // Initilize local variable copy with initializer for field.

          ExprChord            ecg   = (ExprChord) first;
          LoadDeclAddressExpr  ldael = new LoadDeclAddressExpr(vd);
          ecg.setLValue(ldael);
          ldael.setOutDataEdge(ecg);

          if (special)
            fe.unlinkExpression();
          else { // Store local variable copy into the field.
            LoadDeclValueExpr   gv   = new LoadDeclValueExpr(vd);
            ExprChord           ec   = new ExprChord(fe, gv);

            first.getNextChord().insertBeforeInCfg(ec);
            ec.copySourceLine(first);
            newCFGNodeCount++;
          }
        } else {  // Insert a load of the field into the local variable.
          LoadDeclValueExpr   ldve = new LoadDeclValueExpr(vd);
          LoadDeclAddressExpr ldae = new LoadDeclAddressExpr(vd);
          Note                out  = fe.getOutDataEdge();

          out.changeInDataEdge(fe, ldve);

          ExprChord ec = new ExprChord(ldae, fe);

          insert.insertBeforeInCfg(ec);
          ec.copySourceLine(insert);
          newCFGNodeCount++;
        }

        rChanged = true;
        dChanged = true;

        // Change all uses in the remaining refs to the temporary
        // variable.

        for (int ii = 1; ii < kk; ii++) {
          FieldExpr lfe = v[ii];
          if (lfe instanceof LoadFieldValueExpr) {
            Chord             oc = lfe.getChord();
            Note              s  = lfe.getOutDataEdge();
            LoadDeclValueExpr n  = new LoadDeclValueExpr(vd);
            s.changeInDataEdge(lfe, n);
            lfe.unlinkExpression();
            replacedLoadCount++;
            assert assertTrace(trace, "           use ", lfe);
            assert assertTrace(trace, "               ", s);
          }
        }

        // Change all the defs in the iterative dominance set to
        // stores to both the local copy and the field.

        boolean thereAreDefs = false;
        for (int ii = 1; ii < kk; ii++) {
          FieldExpr lfe = v[ii];
          ExprChord dc  = (ExprChord) lfe.getChord();

          if (lfe instanceof LoadFieldAddressExpr) {
            thereAreDefs = true;

            assert assertTrace(trace, "           def ", lfe);
            assert assertTrace(trace, "           out ", lfe.getOutDataEdge());

            if (!special && !moveOutside) { // Insert a store after the def.
              LoadFieldAddressExpr lfae2 = new LoadFieldAddressExpr(struct.copy(), fd);
              LoadDeclValueExpr    ldve  = new LoadDeclValueExpr(vd);
              ExprChord            ec2   = new ExprChord(lfae2, ldve);
              dc.insertAfterOutCfg(ec2, dc.getNextChord());
              ec2.copySourceLine(dc);
              newCFGNodeCount++;
              assert assertTrace(trace, "               ec2 ", ec2);
            }

            LoadDeclAddressExpr ldve = new LoadDeclAddressExpr(vd);
            dc.setLValue(ldve);
            ldve.setOutDataEdge(dc);
            lfe.unlinkExpression();
            dChanged = true;
            assert assertTrace(trace, "               dc ", dc);
          }
        }

        // If the field was defined and we were able to avoid placing
        // the stores after the defs then we insert required stores
        // here.

        if (thereAreDefs && moveOutside && !special) {
          if (lhdom.contains(end)) { // Insert stores at the end of the function.
            LoadFieldAddressExpr lfae2 = new LoadFieldAddressExpr(struct.copy(), fd);
            LoadDeclValueExpr    ldve  = new LoadDeclValueExpr(vd);
            ExprChord            ec2   = new ExprChord(lfae2, ldve);
            end.insertBeforeInCfg(ec2);
            ec2.copySourceLine(end);
            newCFGNodeCount++;
            assert assertTrace(trace, "           def ", lfae2.getField());
            assert assertTrace(trace, "               ", ec2);
          } else if (insert instanceof LoopPreHeaderChord) {
            // Insert stores after the loop exits.
            // We replaced every reference in the dominated nodes so now
            // we must insert a store at every exit from the dominated
            // nodes.

             LoadFieldAddressExpr lfae2 = new LoadFieldAddressExpr(struct.copy(), fd);
             LoadDeclValueExpr    ldve  = new LoadDeclValueExpr(vd);

             newCFGNodeCount += insertStores(lhdom, lfae2, ldve); 
          } else
            throw new scale.common.InternalError("** Improper move point " + insert);
        }
      }
    }

    WorkArea.<Chord>returnSet(stops);
  }

  /**
   * Return true if the variable is "special".  A variable is special if it
   * <ul>
   * <li> is local,
   * <li> is non-static,
   * <li> contains no volatile field,
   * <li> contains no array or struct field,
   * <li> is not passed as an argument to call,
   * <li> is not treated as a unit in an assign, and
   * <li> has no field whose address is taken.
   * </ul>
   */
  private boolean isSpecialVar(VariableDecl decl, Object[] els)
  {
    if (decl.isGlobal() || decl.isStatic())
      return false;

    // Struct variables with volatile fields cannot be special.

    AggregateType type = (AggregateType) decl.getCoreType();
    int           l    = type.numFields();
    for (int i = 0; i < l; i++) {
      FieldDecl fd = type.getField(i);
      Type      ft = fd.getType();
      if (ft.isVolatile())
        return false;
      if (ft.isCompositeType())
        return false;
    }

    // Struct variables passed as arguments or treated as a unit
    // cannot be special.

    HashSet<Chord>  uses = refs.getUseChordSet(decl);
    Iterator<Chord> ui   = uses.iterator();
    while (ui.hasNext()) {
      Chord use = ui.next();

      if (use.getCall(false) != null)
        return false;

      if (use instanceof EndChord)
        return false;
    }

    // Struct variables, with a field whose address is taken, cannot
    // be special.

    for (int i = 0; i < els.length; i++) {
      FieldExpr fe = (FieldExpr) els[i];
      if (fe instanceof LoadFieldAddressExpr) {
        Chord c = fe.getChord();
        if (!c.isAssignChord() || !((ExprChord) c).isDefined(fe))
          return false;
      }
    }

    return true;
  }

  /**
   * Attempt to replace references of a local struct variable as a
   * whole with references to the individual fields of the struct
   * variable.
   */
  private void eliminateSpecialVars()
  {
    Table<Declaration, FieldExpr> tab    = getTable(T_VAR);
    Enumeration<Declaration>      sdecls = tab.keys();
    while (sdecls.hasMoreElements()) { // For each struct variable.
      VariableDecl decl = (VariableDecl) sdecls.nextElement();
      if (decl.addressTaken() || decl.getType().isVolatile())
        continue;

      Object[] els = tab.getRowArray(decl);
      if (els.length < 2)
        continue;

      boolean special = isSpecialVar(decl, els);
      if (!special)
        continue;

      Iterator<Chord> dif = refs.getDefChords(decl);
      while (dif.hasNext()) {
        Chord dc = dif.next();
        if (dc.isAssignChord()) {
          ExprChord ec  = (ExprChord) dc;
          Expr      lhs = ec.getLValue();
          if (lhs instanceof LoadDeclAddressExpr) {
            LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
            if (decl == ldae.getDecl())
              defEachField(ec, ldae, decl);
          }
        }
      }

      Iterator<Chord> uif = refs.getUseChords(decl);
      while (uif.hasNext()) {
        Chord  dc  = uif.next();
        Vector<LoadExpr> lev = dc.getLoadExprList();
        if (lev != null) {
          for (int i = 0; i < lev.size(); i++) {
            LoadExpr le  = lev.get(i);
            Note     out = le.getOutDataEdge();
            if ((decl == le.getDecl()) &&
                !(out instanceof FieldExpr) &&
                 (le instanceof LoadDeclValueExpr))
              loadEachField(dc, le, decl);
          }
        }
      }
    }

    if (rChanged) {
      rChanged = false;
      scribble.recomputeRefs();
      refs = scribble.getRefs();
    }
  }

  /**
   * Replace a def of a struct variable with a def of each field of
   * the struct variable.
   * @param ec is the assignment 
   * @param ldae is the left-hand-side of ec
   * @param varv is the struct variable
   */
  private void defEachField(ExprChord           ec,
                            LoadDeclAddressExpr ldae,
                            VariableDecl        varv)
  {
    Expr rhs  = ec.getRValue();
    Expr sref = null;

    Table<Declaration, FieldExpr> tabv = getTable(T_VAR);
    Table<Declaration, FieldExpr> tabd = null;
    Declaration                   vard = null;

    if (rhs instanceof LoadDeclValueExpr) {
      vard = ((LoadDeclValueExpr) rhs).getDecl();
      sref = new LoadDeclAddressExpr(vard);
      tabd = getTable(T_VAR);
    } else if (rhs instanceof LoadValueIndirectExpr) {
      Expr exp = ((LoadValueIndirectExpr) rhs).getArg();
      sref = exp;
      while (exp instanceof ConversionExpr)
        exp = ((ConversionExpr) exp).getArg();
      if (exp instanceof LoadDeclValueExpr) {
        vard = ((LoadDeclValueExpr) exp).getDecl();
        tabd = getTable(T_IND);
      } else
        return;
    } else
      return;

    if (sref == null)
      return;

    AggregateType at = (AggregateType) varv.getCoreType();
    int           n  = at.numFields();
    for (int i = 0; i < n; i++) {
      FieldDecl            fd   = at.getField(i);
      LoadFieldAddressExpr lfae = new LoadFieldAddressExpr(ldae.copy(), fd);
      Expr                 cpy  = sref.conditionalCopy();
      LoadFieldValueExpr   lfve = new LoadFieldValueExpr(cpy, fd);
      ExprChord            set  = new ExprChord(lfae, lfve);

      ec.insertBeforeInCfg(set);
      set.copySourceLine(ec);
      set.setLabel(ec.getLabel());
      tabv.put(varv, lfae);
      if (tabd != null)
        tabd.put(vard, lfve);

      dom.addDominatee(set, ec);
      int l = set.numInCfgEdges();
      for (int j = 0; j < l; j++) {
        Chord in = set.getInCfgEdge(j);
        dom.removeDominatee(in, ec);
        dom.addDominatee(in, set);
      }
      newCFGNodeCount++;
    }

    rChanged = true;
    ec.removeFromCfg();
    newCFGNodeCount--;
  }

  /**
   * Replace a use of a struct variable with a duse of each field of
   * the struct variable.
   * @param dc contains the use
   * @param le is the use
   * @param varv is the struct variable
   */
  private void loadEachField(Chord dc, LoadExpr le, VariableDecl varv)
  {
    if (!dc.isAssignChord())
      return;

    ExprChord ec = (ExprChord) dc;
    if (le != ec.getRValue())
      return;

    Expr                          lhs  = ec.getLValue();
    Table<Declaration, FieldExpr> tabv = getTable(T_VAR);
    Table<Declaration, FieldExpr> tabd = null;

    if (lhs instanceof LoadDeclAddressExpr)
      tabd = tabv;
    else if (lhs instanceof LoadDeclValueExpr)
      tabd = getTable(T_IND);
    else
      return;

    Declaration   vard = ((LoadExpr) lhs).getDecl();
    AggregateType at   = (AggregateType) varv.getCoreType();
    int           n    = at.numFields();
    for (int i = 0; i < n; i++) {
      FieldDecl            fd   = at.getField(i);
      LoadFieldAddressExpr lfae = new LoadFieldAddressExpr(lhs.copy(), fd);
      LoadFieldValueExpr   lfve = new LoadFieldValueExpr(new LoadDeclAddressExpr(varv), fd);
      ExprChord            set  = new ExprChord(lfae, lfve);

      ec.insertBeforeInCfg(set);
      dom.addDominatee(set, ec);
      int l = set.numInCfgEdges();
      for (int j = 0; j < l; j++) {
        Chord in = set.getInCfgEdge(j);
        dom.removeDominatee(in, ec);
        dom.addDominatee(in, set);
      }
      set.copySourceLine(ec);
      set.setLabel(ec.getLabel());
      tabv.put(varv, lfve);
      tabd.put(vard, lfae);
      newCFGNodeCount++;
    }

    rChanged = true;
    int l = ec.numInCfgEdges();
    for (int i = 0; i < l; i++) {
      Chord in = ec.getInCfgEdge(i);
      dom.removeDominatee(in, ec);
    }
    ec.removeFromCfg();
    newCFGNodeCount--;
  }

  private int classify(Expr exp)
  {
    if (exp instanceof LoadDeclValueExpr)
      return T_IND;
    if (exp instanceof LoadDeclAddressExpr)
      return T_VAR;
    if (exp instanceof LoadFieldAddressExpr)
      return T_LFA;
    if (exp instanceof LoadFieldValueExpr)
      return T_LFV;
    if (exp instanceof LoadValueIndirectExpr)
      return T_PTR;
    if (exp instanceof ArrayIndexExpr)
      return T_ARR;
    return T_OTH;
  }

  @SuppressWarnings("unchecked")
  private Table<Declaration, FieldExpr> getTable(int cat)
  {
    Table<Declaration, FieldExpr> tab = (Table<Declaration, FieldExpr>) tables[cat];
    if (tab == null) {
      tab = new Table<Declaration, FieldExpr>();
      tables[cat] = tab;
    }
    return tab;
  }

  /**
   * Specify that this optimization requires that the CFG NOT be in
   * SSA form.
   */
  public int requiresSSA()
  {
    return NO_SSA;
  }

  private boolean trace1(Object[]       els,
                         HashSet<Chord> stops,
                         boolean        global,
                         boolean        special,
                         boolean        indirect,
                         boolean        isRecursive,
                         Declaration    decl)
  {
    if (!trace)
      return true;

    System.out.print("    refs:");
    System.out.print(els.length);
    System.out.print(" spc:");
    System.out.print(special);
    System.out.print(" gl:");
    System.out.print(global);
    System.out.print(" ind:");
    System.out.print(indirect);
    System.out.print(" vg:");
    System.out.print(decl.isGlobal());
    System.out.print(" rec:");
    System.out.println(isRecursive);
    System.out.println("       stops: ");

    Iterator<Chord> it = stops.iterator();
    while (it.hasNext()) {
      System.out.print("         ");
      System.out.println(it.next());
    }

    return true;
  }

  private boolean trace2(Object[] els)
  {
    if (!trace)
      return true;

    System.out.println("       els: ");
    for (int i = 0; i < els.length; i++) {
      Note  o = (Note) els[i];
      Chord c = o.getChord();
      System.out.print("         ");
      System.out.print(c.getSourceLineNumber());
      System.out.print(" ");
      System.out.print(c.getNodeID());
      System.out.print(" ");
      System.out.print(c.getLabel());
      System.out.print(" ");
      System.out.println(o);
      System.out.print("           ");
      System.out.println(c);
    }

    return true;
  }

  private boolean trace3(Note x, int k)
  {
    if (!trace)
      return true;

    System.out.print("         v:");
    System.out.print(k);
    System.out.print(x.getChord().getSourceLineNumber());
    System.out.print(" ");
    System.out.print(x.getChord().getNodeID());
    System.out.print(" ");
    System.out.println(x);

    return true;
  }

  private boolean trace4(int            k,
                         FieldExpr      fe,
                         Vector<Chord>  lhdom,
                         boolean        moveOutside,
                         Chord          first,
                         HashSet<Chord> stops)
  {
    if (!trace)
      return true;

    System.out.print("        ");
    System.out.print(" k:");
    System.out.print(k);
    System.out.print(" nd:");
    System.out.print(lhdom.size());
    System.out.print(" mo:");
    System.out.println(moveOutside);

    System.out.print("           ");
    System.out.print(first.getNodeID());
    System.out.print(" stops:");
    System.out.println(stops.contains(first));
    System.out.println("           " + fe);

    for (int ii = 0; ii < lhdom.size(); ii++) {
      System.out.print("            d ");
      System.out.print(ii);
      System.out.print(" l:");
      System.out.print(lhdom.get(ii).getSourceLineNumber());
      System.out.print(" lab:");
      System.out.println(lhdom.get(ii).getLabel());
    }

    System.out.print("          0");
    System.out.print(" l:");
    System.out.print(fe.getChord().getSourceLineNumber());
    System.out.print(" lab:");
    System.out.println(first.getLabel());

    return true;
  }

  private boolean trace5(int ii, Chord uc, Vector<Chord> lhdom)
  {
    if (!trace)
      return true;

    System.out.print("          ");
    System.out.print(ii);
    System.out.print(" l:");
    System.out.print(uc.getSourceLineNumber());
    System.out.print(" lab:");
    System.out.print(uc.getLabel());
    System.out.print(" dom:");
    System.out.println(lhdom.contains(uc));

    return true;
  }

  private boolean trace6(int numRefs, FieldExpr fe)
  {
    if (!trace)
      return true;

    System.out.print("          refs:");
    System.out.print(numRefs);
    System.out.print(" l:");
    System.out.println(fe.getChord().getSourceLineNumber());

    return true;
  }
}
