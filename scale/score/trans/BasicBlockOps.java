package scale.score.trans;

import java.io.*;
import java.util.Iterator;
import java.util.Enumeration;

import scale.common.*;
import scale.callGraph.CallGraph;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.StringLiteral;
import scale.clef.expr.Expression;
import scale.clef.type.*;
import scale.score.*;
import scale.score.analyses.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.pred.*;
import scale.clef.decl.*;
import scale.clef.type.CompositeType;
import scale.clef.LiteralMap;

/**
 * Perform optimizations on each basic block.
 * <p>
 * $Id: BasicBlockOps.java,v 1.57 2007-10-04 19:58:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This module performs optimizations that are local to a basic block.
 * For this module a basic block ends at a function call as well as at
 * a branch point in the CFG.  These optimizations do not require
 * use-def links and may be performed in or out of SSA form.  Current
 * basic block optimizations:
 * <ol>
 * <li>Eliminate duplicate loads from a structure field.
 * <li>Eliminate duplicate loads from scalar variables (e.g., global
 * or static scalar variables).
 * <li>Eliminate duplicate memory address generation.
 * <li>Eliminate un-needed stores into scalar variables.
 * </ol>
 * <p>
 * Elimination of a store to a variable is {@link
 * scale.score.trans#legal legal} if:
 * <ul>
 * <li>it is followed by another store to the same variable in the
 * same basic block,
 * <li>the variable is in memory,
 * <li>the variable has no hidden aliases,
 * <li>the variable's address is not taken, and
 * <li>the variable is not volatile.
 * </ul>
 * It is always {@link scale.score.trans#legal beneficial}.
 * <p>
 * Eliminating a load increases the live range of any definition of a
 * register value.  This increases register pressure.
 * <p>
 * Elimination of a load from a variable is {@link
 * scale.score.trans#legal legal} if:
 * <ul>
 * <li>it is preceded by another load from the same variable in the
 * same basic block,
 * <li>the variable is in memory,
 * <li>the variable has no hidden aliases, and
 * <li>the variable is not volatile.
 * </ul>
 * If it is legal, it is always eliminated.
 * <p>
 * Replacing a reference to a constant with a reference to a variable
 * is {@link scale.score.trans#legal legal} if the variable has been
 * loaded with the constant value previously in the basic block.
 * It is {@link scale.score.trans#legal beneficial} if the cost of
 * generating the constant value in a register is greater than <code>
 * minimumExecutionCost</code>.

 */
public class BasicBlockOps extends Optimization
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
   * Maximum number of CFG nodes that we will process in one "chunk".
   */
  public static int maxBlockSize = 200;

  private static int newCFGNodeCount                 = 0; // A count of new nodes created.
  private static int loadEliminatedCount             = 0; // The number of times a load from memory was eliminated.
  private static int constantEliminatedCount         = 0; // The number of times the generation of a constant was eliminated.
  private static int loadAddressEliminatedCount      = 0; // The number of times the generation of a variable address was eliminated.
  private static int loadFieldEliminatedCount        = 0; // The number of times a load from a structure field was eliminated.
  private static int loadFieldAddressEliminatedCount = 0; // The number of times the generation of a structure field address was eliminated.
  private static int loadArrayAddressEliminatedCount = 0; // The number of times the generation of an array element address was eliminated.
  private static int loadIndirectEliminatedCount     = 0; // The number of times a load indirect was eliminated.
  private static int storeEliminatedCount            = 0; // The number of times a store to memory was eliminated.

  private static final String[] stats = {
    "loadEliminated",
    "constantEliminated",
    "loadAddressEliminated",
    "loadFieldEliminated",
    "loadFieldAddressEliminated",
    "loadArrayAddressEliminated",
    "loadIndirectEliminated",
    "storeEliminated",
    "newCFGNodes",
  };

  static
  {
    Statistics.register("scale.score.trans.BasicBlockOps", stats);
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  /**
   * Return the number of times a load from memory was eliminated.
   */
  public static int loadEliminated()
  {
    return loadEliminatedCount;
  }

  /**
   * Return the number of times the generation of a constant was eliminated.
   */
  public static int constantEliminated()
  {
    return constantEliminatedCount;
  }

  /**
   * Return the number of times a load from memory was eliminated.
   */
  public static int storeEliminated()
  {
    return storeEliminatedCount;
  }

  /**
   * Return the number of times a load from a structure field was eliminated.
   */
  public static int loadFieldEliminated()
  {
    return loadFieldEliminatedCount;
  }

  /**
   * Return the number of times a load indirect was eliminated.
   */
  public static int loadIndirectEliminated()
  {
    return loadIndirectEliminatedCount;
  }

  /**
   * Return the number of times the generation of a memory address was
   * eliminated.
   */
  public static int loadAddressEliminated()
  {
    return loadAddressEliminatedCount;
  }

  /**
   * Return the number of times the generation of a structure field
   * address was eliminated.
   */
  public static int loadFieldAddressEliminated()
  {
    return loadFieldAddressEliminatedCount;
  }

  /**
   * Return the number of times the generation of an array element
   * address was eliminated.
   */
  public static int loadArrayAddressEliminated()
  {
    return loadArrayAddressEliminatedCount;
  }

  private boolean inSSA;     // True if the CFG is in SSA form.
  private boolean nvFloats;  // True if FP vaalues not kept in registers over a call.
  private boolean notDoneDV; // Look for variable loads.
  private boolean notDoneDA; // Look for variable address loads.
  private boolean notDoneLE; // Look for literals.
  private boolean notDoneFV; // Look for struct field loads.
  private boolean notDoneFA; // Load for struct field address loads.
  private boolean notDoneAA; // Look for array address loads.
  private boolean notDoneAV; // Look for array address + index loads.
  private boolean notDoneIN; // Look for load value indirect.
  private boolean doCopyPropagate;
  private boolean putsValid; // The putsDef value has been determined.
  private boolean putcharValid; // The putcharDef value has been determined.

  private Vector<Object>              loadMap;
  private HashMap<Declaration, Chord> storeMap;
  private Stack<Expr>                 ewl;

  private RoutineDecl putsDef;    // The def for the puts function.
  private RoutineDecl putcharDef; // The def for the putchar function.
  private int         opCount;


  /**
   * @param scribble is the CFG
   */
  public BasicBlockOps(Scribble scribble)
  {
    super(scribble, "_bb");
    this.inSSA    = (scribble.inSSA() != Scribble.notSSA);
    this.nvFloats = Machine.currentMachine.hasCapability(Machine.HAS_NON_VOLATILE_FP_REGS);
  }

  /**
   * Perform the actual basic block optimizations.
   */
  public void perform()
  {
    rChanged = false;
    ewl      = WorkArea.<Expr>getStack("perform BasicBlockOps");
    loadMap  = new Vector<Object>(23);

    Stack<Chord> wl    = WorkArea.<Chord>getStack("perform BasicBlockOps");
    Chord        start = scribble.getBegin();

    wl.push(start);
    Chord.nextVisit();
    start.setVisited();

    while (!wl.empty()) {
      Chord bbegin = wl.pop(); // Get basic block first CFG node.
      Chord bend   = bbegin;           // Track the last node in the basic block.
      Chord cur    = bbegin;
      int   cnt    = 0;

      // Find the next basic block.

      while (true) {
        bend = cur;

        Expr call = cur.getCall(true);
        if (call != null)
          // It is not worth saving a register over a call to a
          // function.
          break;

        if (cur.isLastInBasicBlock())
          break; // End of actual basic block.

        if (useHeuristics && (cnt > maxBlockSize))
          // Large basic blocks increase register pressure too much.
          break;

        cur = cur.getNextChord();
        cnt++;
      }

      bend.pushOutCfgEdges(wl);

      // Eliminate loads and stores from the basic block.

      removeDuplicateStores(bbegin, bend);
      removeDuplicateLoads(bbegin, bend);
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Expr>returnStack(ewl);

    loadMap = null;

    if (dChanged)
      scribble.recomputeDominators();

    if (rChanged)
      scribble.recomputeRefs();
  }

  /**
   * Find and remove duplicate stores in a basic block.
   * @param bbegin is the first node in the basic block
   * @param bend is the last node in the basic block
   */
  private void removeDuplicateStores(Chord bbegin, Chord bend)
  {
    Chord scur = bend;
    Chord cur  = scur;
    loadMap.clear();

    do {
      cur = scur;
      scur = scur.getFirstInCfgEdge();

      if (cur instanceof ExprChord) {
        ExprChord se  = (ExprChord) cur;
        Expr      lhs = se.getLValue();

        if (lhs instanceof LoadDeclAddressExpr) {
          LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
          Declaration         decl = ldae.getDecl();
          if ((decl.residency() == Residency.MEMORY) &&
              !decl.hasHiddenAliases() &&
              !decl.addressTaken() &&
              !decl.getType().isVolatile()) {
            if (loadMap.contains(decl)) {
              Expr rhs = se.getRValue();
              if ((rhs.sideEffects() <= Expr.SE_OVERFLOW) && (cur != bbegin))
                cur.removeFromCfg();
              else
                se.setLValue(null);
            } else
              loadMap.add(decl);
          }
        }
      }

      cur.pushInDataEdges(ewl);
      while (!ewl.empty()) {
        Expr cure =  ewl.pop().getLow();
        if (cure instanceof LoadDeclValueExpr) {
          LoadDeclValueExpr ldve = (LoadDeclValueExpr) cure;
          Declaration       decl = ldve.getDecl();
          loadMap.remove(decl);
        } else
          cure.pushOperands(ewl);
      }

    } while (cur != bbegin);
  }

  /**
   * Find and remove duplicate loads in a basic block.
   * @param bbegin is the first node in the basic block
   * @param bend is the last node in the basic block
   */
  private void removeDuplicateLoads(Chord bbegin, Chord bend)
  {
    Chord scur = bbegin;

    // The following loops can not be combined.  If they are, then
    // many opportunities for replacement will be lost as there will
    // be ordering problems.

    // Load value of variable.

    loadMap.clear();
    storeMap = null;

    notDoneDV = true;  // Look for variable loads.
    notDoneDA = true;  // Look for variable address loads.
    notDoneLE = false; // Look for literals.
    notDoneFV = false; // Look for struct field loads.
    notDoneFA = false; // Load for struct field address loads.
    notDoneAA = false; // Look for array address loads.
    notDoneAV = false; // Look for array address + index loads.
    notDoneIN = false; // Look for load value indirect.

    opCount = 0;
    doCopyPropagate = false;
    while (notDoneDV) {
      notDoneDV = (scur != bend);
      Chord cur = scur;

      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      if (!cur.isAssignChord() && (cur instanceof ExprChord)) {
        // Determine if a call to printf can be optimized.
        // The return value of printf must not be used.
        ExprChord ec  = (ExprChord) cur;
        Expr      rhs = ec.getRValue();
        if (rhs instanceof CallFunctionExpr) {
          CallFunctionExpr call = (CallFunctionExpr) rhs;
          Expr             nc   = optimizePrintfCall(call);
          if (nc != null) { // Optimized.
            if (nc instanceof NilExpr) // printf("")
              ec.removeFromCfg();
            else {
              ec.changeInDataEdge(call, nc);
              call.unlinkExpression();
            }
          }
        }
      }

      doLoadDecl(cur);
    }

    // Insert the final store from the temporary surrogate 
    // variables to the corresponding variables in memory.

    insertStores(storeMap);
    storeMap = null;
    loadEliminatedCount += opCount;
    opCount = 0;

    // Constant generation.

    loadMap.clear();
    scur = bbegin;
    while (notDoneLE) {
      notDoneLE = (scur != bend);
      Chord cur = scur;
      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      doLiterals(cur);
    }

    constantEliminatedCount += opCount;
    opCount = 0;

    // Load array address.

    doCopyPropagate = true;
    loadMap.clear();
    scur = bbegin;
    while (notDoneAA) {
      notDoneAA = (scur != bend);
      Chord cur = scur;
      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      doArrayAddresses(cur);
    }

    loadArrayAddressEliminatedCount += opCount;
    opCount = 0;

    // Try to merge multiple array element address calculations in a
    // basic block.  If the array element addresses differ by only an
    // offset (e.g., a[i + 1] and a[i + 2]), then the address of a[i]
    // can be calculated once and a constant offset used thereafter.

    doCopyPropagate = true;
    loadMap.clear();
    scur = bbegin;
    while (notDoneAV) {
      notDoneAV = (scur != bend);
      Chord cur = scur;
      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      doArrayLoads(cur);
    }

    loadArrayAddressEliminatedCount += opCount;
    opCount = 0;

    // Load value of field.

    loadMap.clear();
    scur = bbegin;
    doCopyPropagate = false;
    while (notDoneFV) {
      notDoneFV = (scur != bend);
      Chord cur = scur;
      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      doFieldLoads(cur);
    }

    loadFieldEliminatedCount += opCount;
    opCount = 0;

    // Load address of field.

    loadMap.clear();
    scur = bbegin;
    doCopyPropagate = true;
    while (notDoneFA) {
      notDoneFA = (scur != bend);
      Chord cur = scur;
      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      doFieldAddresses(cur);
    }

    loadFieldAddressEliminatedCount += opCount;
    opCount = 0;

    // Load value indirect.

    loadMap.clear();
    scur = bbegin;
    doCopyPropagate = true;
    while (notDoneIN) {
      notDoneIN = (scur != bend);
      Chord cur = scur;
      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      doIndirectLoads(cur);
    }

    loadIndirectEliminatedCount += opCount;
    opCount = 0;

    // Load address of variable.

    loadMap.clear();
    scur = bbegin;
    doCopyPropagate = true;
    while (notDoneDA) {
      notDoneDA = (scur != bend);
      Chord cur = scur;
      scur = scur.getNextChord();

      if (cur.isPhiExpr())
        continue;

      doLoadAddresses(cur);
    }

    loadAddressEliminatedCount += opCount;
    opCount = 0;
  }


  private void doLoadDecl(Chord cur)
  {
    // Find all the expressions that load a value (address or otherwise).

    if (cur.isAssignChord())
      ewl.push(((ExprChord) cur).getRValue());
    else
      cur.pushInDataEdges(ewl);

    while (!ewl.empty()) {
      Expr cure = ewl.pop().getLow();

      if (cure instanceof LoadDeclValueExpr) {
        Note o = cure.getOutDataEdge();
        if (cure.getCoreType().isAtomicType())
          checkLoadExpr((LoadDeclValueExpr) cure);
        continue;
      } 

      if (cure.isLiteralExpr()) {
        if (cure.getCoreType().isAtomicType())
          notDoneLE = true;
        continue;
      }

      if (cure instanceof LoadDeclAddressExpr) {
        notDoneDA = true;
        continue;
      }

      if (cure instanceof FieldExpr) {
        notDoneFV = true;
        notDoneFA = true;
      } else if (cure instanceof ArrayIndexExpr) {
        notDoneAA = true;
        notDoneAV = true;
      } else if (cure instanceof AdditionExpr) {
        if (cure.getCoreType().isPointerType())
          notDoneAA = true;
      } else if (cure instanceof LoadValueIndirectExpr) {
        notDoneIN = true;
      }

      cure.pushOperands(ewl);
    }

    if (!cur.isAssignChord())
      return;

    ExprChord se  = (ExprChord) cur;
    Expr      lhs = se.getLValue();

    // See if the variable is defined.

    if (lhs instanceof LoadDeclAddressExpr) {
      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
      Declaration         decl = ldae.getDecl();
      if (!inSSA) {
        Note s = lookup(decl);
        if (s != null) {
          if (s instanceof ExprChord) { // We can eliminate stores to the variable too.
            Declaration ndecl = ((LoadDeclAddressExpr) ((ExprChord) s).getLValue()).getDecl();
            ldae.setDecl(ndecl);
            post(decl, cur);
          } else {

            // This is the second reference to the variable.
            // Store first into a temporary variable, replace second by temporary.
            // Create a surrogate temporary variable to hold the value.

            Expr              first = (Expr) s;
            Chord             pos   = first.getChord();
            Type              ft    = getTempVarType(first.getType());
            VariableDecl      temp  = genTemp(ft);
            Note              fout  = first.getOutDataEdge();
            LoadDeclValueExpr fldve = new LoadDeclValueExpr(temp);

            fout.changeInDataEdge(first, fldve);

            ExprChord sef = new ExprChord(new LoadDeclAddressExpr(temp), first.addCast(ft));
            sef.setVisited();
            sef.copySourceLine(pos);

            if (inSSA) {
              if (fldve.getChord() != null)
                fldve.setUseDef(sef);

              copyPropagate(sef, temp, fout, fldve);
            }

            newCFGNodeCount++;
            pos.insertBeforeInCfg(sef);
            dChanged = true;
            rChanged = true;
            ldae.setDecl(temp);
            post(decl, sef); // Map from name to store into temporary.
            opCount++;
          }

          if (storeMap == null)
            storeMap = new HashMap<Declaration, Chord>(11);

          storeMap.put(decl, cur); // Record that we will need a store eventually.
          storeEliminatedCount++;
          return;
        }
      }

      Type type = ldae.getCoreType().getPointedTo();
      if ((decl.residency() != Residency.MEMORY) ||
          !type.isAtomicType() ||
          type.isVolatile() ||
          decl.addressTaken() ||
          decl.hasHiddenAliases()) {
        reset(loadMap, decl);
        return;
      }

      if (!isReferencedAgain(cur, decl, ewl))
        return;

      // We may have encountered loads before.  Ignore them.
      // Otherwise, we will have to screw around with renamed SSA
      // variables.  This logic handles a store followed by load.
      // We will use a temp variable, without knowing if there any
      // more references, just to keep the logic simple.

      Type                ft   = getTempVarType(type);
      VariableDecl        temp = genTemp(ft);
      LoadDeclAddressExpr ldt  = new LoadDeclAddressExpr(temp);

      cur.changeInDataEdge(ldae, ldt);
      ldae.unlinkExpression();

      if (inSSA)
        copyPropagate(se, temp, se, null);

      post(decl, cur); // Map from name to store

      if (storeMap == null)
        storeMap = new HashMap<Declaration, Chord>(11);

      storeMap.put(decl, cur); // Record that we will need a store eventually.
      storeEliminatedCount++;
      rChanged = true;

      return;
    }

    // Handle case of store indirect which might define the scalar variable.

    if (lhs instanceof LoadDeclValueExpr) {
      LoadDeclValueExpr ldve = (LoadDeclValueExpr) lhs;
      ExprChord         se2  = ldve.getUseDef();

      if (inSSA && (se2 != null)) {
        Expr exp2 = se2.getRValue().getLow();
        if (exp2 instanceof ArrayIndexExpr)
          return; // Assume stores into an array do not define a scalar variable.
        if (exp2 instanceof LoadFieldAddressExpr)
          return; // Assume stores into a structure do not define a scalar variable.
      }

      // If the user program is ISO C compliant then an indirect
      // store will not define a scalar variable.  If the program is
      // compliant, the user should specify unsafe to be true.

      if (!unsafe) {
        loadMap.clear();
        insertStores(storeMap);
        storeMap = null;
      }
    }
  }

  private void doLiterals(Chord cur)
  {
    // Find all the literal expressions.

    cur.pushInDataEdges(ewl);
    while (!ewl.empty()) {
      Expr cure = ewl.pop().getLow();
      if (cure.isLiteralExpr()) {
        if (cure.executionCostEstimate() >= minimumExecutionCost)
          checkExpr(cure);
      } else
        cure.pushOperands(ewl);
    }
    if (!nvFloats) {
      if (cur.getCall(false) != null)
        loadMap.clear();
    }
  }

  private void doArrayAddresses(Chord cur)
  {
      // Find some of the expressions that load an address.

      cur.pushInDataEdges(ewl);
      while (!ewl.empty()) {
        Expr cure = ewl.pop().getLow();

        if (cure.getType().isPointerType() &&
            (cure.executionCostEstimate() >= minimumExecutionCost) &&
            ((cure instanceof ArrayIndexExpr) || (cure instanceof AdditionExpr)))
          checkExpr(cure);

        cure.pushOperands(ewl);
      }

      if (!cur.isAssignChord())
        return;

      Expr lhs = ((ExprChord) cur).getLValue().getReference();
      if (lhs instanceof LoadDeclAddressExpr) {
        Declaration decl = ((LoadDeclAddressExpr) lhs).getDecl();
        reset(loadMap, decl);
        return;
      }

      loadMap.clear();
  }

  private void doArrayLoads(Chord cur)
  {
    // Find some of the expressions that compute an array element
    // address.

    cur.pushInDataEdges(ewl);
    while (!ewl.empty()) {
      Expr cure = ewl.pop().getLow();

      if (cure instanceof ArrayIndexExpr) {
        Expr ne = cure.reduce();
        Note n  = cure.getOutDataEdge();
        if (ne != cure) {
          n.changeInDataEdge(cure, ne);
          cure.unlinkExpression();
          cure = ne;
        }
        cure = checkArrayIndexExpr((ArrayIndexExpr) cure);
      }

      if (cure != null)
        cure.pushOperands(ewl);
    }

    if (!cur.isAssignChord())
      return;

    Expr lhs = ((ExprChord) cur).getLValue().getReference();
    if (lhs instanceof LoadDeclAddressExpr) {
      Declaration decl = ((LoadDeclAddressExpr) lhs).getDecl();
      // Remove all mappings where the expression contains a reference
      // to the variable.
      int n = loadMap.size();
      for (int i = n - 3; i >= 0; i -= 3) {
        Note exp = (Note) loadMap.elementAt(i + 2);
        if (exp instanceof Expr) {
          if (((Expr) exp).containsDeclaration(decl)) {
            loadMap.removeElementAt(i + 2);
            loadMap.removeElementAt(i + 1);
            loadMap.removeElementAt(i + 0);
          }
          continue;
        }
        ExprChord ec = (ExprChord) exp;
        if (ec.getRValue().containsDeclaration(decl) ||
            ec.getLValue().containsDeclaration(decl)) {
          loadMap.removeElementAt(i + 2);
          loadMap.removeElementAt(i + 1);
          loadMap.removeElementAt(i + 0);
        }
      }
      return;
    }

    loadMap.clear();
  }

  private void doFieldLoads(Chord cur)
  {
    // Find all the expressions that load a value (address or otherwise).

    cur.pushInDataEdges(ewl);
    while (!ewl.empty()) {
      Expr cure = ewl.pop().getLow();
      if ((cure instanceof LoadFieldValueExpr) &&
          cure.getCoreType().isAtomicType()) 
        checkExpr(cure);
      cure.pushOperands(ewl);
    }

    if (!cur.isAssignChord())
      return;

    Expr lhs = ((ExprChord) cur).getLValue();
    if (lhs instanceof LoadDeclAddressExpr) {
      Declaration decl = ((LoadDeclAddressExpr) lhs).getDecl();
      reset(loadMap, decl);
      return;
    }

    if (inSSA && (lhs instanceof LoadDeclValueExpr)) {
      LoadDeclValueExpr ldve = (LoadDeclValueExpr) lhs;
      ExprChord         se   = ldve.getUseDef();
      if (se != null)
        lhs  = se.getRValue();
    }

    if (lhs instanceof LoadFieldAddressExpr) {
      resetField(loadMap, (LoadFieldAddressExpr) lhs);
      return;
    }

    resetFieldAddress(loadMap);
  }

  private void doFieldAddresses(Chord cur)
  {
    // Find all the expressions that load a value (address or otherwise).

    cur.pushInDataEdges(ewl);
    while (!ewl.empty()) {
      Expr cure = ewl.pop().getLow();
      if (cure instanceof LoadFieldAddressExpr) {
        if (!(cure.getOutDataEdge() instanceof LoadFieldAddressExpr)) {
          LoadFieldAddressExpr lfae = (LoadFieldAddressExpr) cure;
          boolean doit = (lfae.getField().getBits() == 0);
          if (doit) {
            Expr struct = lfae.getStructure();
            if (struct instanceof LoadDeclAddressExpr) {
              VariableDecl vd = (VariableDecl) ((LoadDeclAddressExpr) struct).getDecl();
              doit = Assigned.IN_REGISTER != vd.getStorageLoc();
            }
          }
          if (doit)
            checkExpr(cure);
        }
      }
      cure.pushOperands(ewl);
    }

    if (!cur.isAssignChord())
      return;

    Expr lhs = ((ExprChord) cur).getLValue();
    if (lhs instanceof LoadDeclAddressExpr) {
      Declaration decl = ((LoadDeclAddressExpr) lhs).getDecl();
      reset(loadMap, decl);
      return;
    }

    resetFieldAddress(loadMap);
  }

  private void doIndirectLoads(Chord cur)
  {
    // Find all the expressions that load a value (address or otherwise).

    if (cur.isAssignChord())
      ewl.push(((ExprChord) cur).getRValue());
    else
      cur.pushInDataEdges(ewl);

    while (!ewl.empty()) {
      Expr cure = ewl.pop().getLow();
      if ((cure instanceof LoadValueIndirectExpr) &&
          cure.getCoreType().isAtomicType() &&
          !cure.getType().isVolatile()) {
        checkExpr(cure);
      }
      cure.pushOperands(ewl);
    }

    if (!cur.isAssignChord())
      return;

    ExprChord set = (ExprChord) cur;
    Expr      lhs = set.getLValue();
    if (lhs instanceof LoadDeclAddressExpr) {
      Declaration decl = ((LoadDeclAddressExpr) lhs).getDecl();
      if (decl.residency() == Residency.MEMORY)
        loadMap.clear();
      else
        reset(loadMap, decl);
      return;
    }

    if (lhs instanceof LoadDeclValueExpr) {
      // Handle indirect stores on the left-hand-side.  We don't
      // eliminate any indirect stores.  But, we do set up to use a
      // temporary for the value in memory in case it is used multiple
      // times.

      Expr              rhs  = set.getRValue();
      LoadDeclValueExpr ldve = (LoadDeclValueExpr) lhs;
      Type              pt   = ldve.getType();
      Type              type = pt.getCoreType().getPointedTo();
      Declaration       decl = ldve.getDecl();

      if (type.isAtomicType() &&
          !pt.isVolatile() &&
          isReferencedAgain(cur, decl, ewl)) {

        // We may have encountered loads before.  Ignore them.
        // Otherwise, we will have to screw around with renamed SSA
        // variables.  This logic handles a store followed by load.
        // We will use a temp variable, without knowing if there any
        // more references, just to keep the logic simple.

        Type                ft   = getTempVarType(type);
        VariableDecl        temp = genTemp(ft);
        LoadDeclAddressExpr ldae = new LoadDeclAddressExpr(temp);
        LoadDeclValueExpr   ld   = new LoadDeclValueExpr(temp);

        cur.changeInDataEdge(rhs, ld);

        ExprChord se = new ExprChord(ldae, rhs);
        se.setVisited();
        se.copySourceLine(cur);

        if (inSSA)
          ld.setUseDef(se);

        newCFGNodeCount++;
        cur.insertBeforeInCfg(se);

        LoadDeclValueExpr     ldve2 = new LoadDeclValueExpr(ldve.getDecl());
        LoadValueIndirectExpr lvie  = new LoadValueIndirectExpr(ldve2);
        loadMap.clear();
        post(lvie, se); // Map from name to store

        dChanged = true;
        rChanged = true;
        return;
      }
    }

    // Some other form of a store.

    if (hasDummyAliases ||
        !(lhs instanceof LoadFieldAddressExpr) ||
        (cur.getCall(true) != null))
      loadMap.clear();
  }

//   private void dump(Chord start, Chord end)
//   {
//     Chord   c = start;
//     boolean cond = true;
//     int i = 0;
//     do {
//       cond = (c != end);
//       System.out.println("     " + i++ + " " + c.getNodeID() + " " + c);
//       c = c.getNextChord();
//     } while (cond);
//   }

  private void doLoadAddresses(Chord cur)
  {
    // Find all the expression that load a value (address or otherwise).

    cur.pushInDataEdges(ewl);
    while (!ewl.empty()) {
      Expr cure = ewl.pop().getLow();
      if (cure instanceof LoadDeclAddressExpr) {
        LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) cure;

        if (!ldae.referencesVariableInCommon())
          checkLoadExpr(ldae);

        continue;
      }
      cure.pushOperands(ewl);
    }
  }

  /**
   * Insert a store into the memory variable for every surrogate
   * temporary variable.  The argument is a mapping from the memory
   * variable to the last store into the surrogate temporary variable.
   */
  private void insertStores(HashMap<Declaration, Chord> st)
  {
    if (st == null)
      return;

    Enumeration<Declaration> en = st.keys();
    while (en.hasMoreElements()) {
      Declaration         decl = en.nextElement(); // Memory variable.
      ExprChord           se   = (ExprChord) st.get(decl);       // Last store into the surrogate.
      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) se.getLValue();
      Chord               c    = se.getNextChord();
      LoadDeclAddressExpr lhs  = new LoadDeclAddressExpr(decl);
      LoadDeclValueExpr   rhs  = new LoadDeclValueExpr(ldae.getDecl());
      ExprChord           ec   = new ExprChord(lhs, rhs);

      ec.setVisited();
      ec.copySourceLine(c);
      newCFGNodeCount++;

      if (c.numInCfgEdges() > 1)
        se.insertAfterOutCfg(ec, c);
      else
        c.insertBeforeInCfg(ec);
      storeEliminatedCount--;
    }
  }

  /**
   * Remove all mappings where the expression contains a reference to
   * the variable.
   */
  private void reset(Vector<Object> map, Declaration decl)
  {
    int n = map.size();
    for (int i = n - 2; i >= 0; i -= 2) {
      Object o = map.elementAt(i);
      if (o instanceof Expr) {
        if (((Expr) o).containsDeclaration(decl)) {
          map.removeElementAt(i + 1);
          map.removeElementAt(i + 0);
        }
        continue;
      }
      Note exp = (Note) map.elementAt(i + 1);
      if (exp instanceof Expr) {
        if (((Expr) exp).containsDeclaration(decl)) {
          map.removeElementAt(i + 1);
          map.removeElementAt(i + 0);
        }
        continue;
      }
      ExprChord ec = (ExprChord) exp;
      if (ec.getRValue().containsDeclaration(decl) ||
          ec.getLValue().containsDeclaration(decl)) {
        map.removeElementAt(i + 1);
        map.removeElementAt(i + 0);
      }
    }
  }

  /**
   * Remove all mappings where the structure address is not fixed.
   */
  private void resetFieldAddress(Vector<Object> map)
  {
    int n = map.size();
    for (int i = n - 2; i >= 0; i -= 2) {
      Note exp = (Note) map.elementAt(i + 1);
      if (exp instanceof ExprChord)
        exp = ((ExprChord) exp).getRValue();
      if (exp instanceof FieldExpr) {
        FieldExpr lfae      = (FieldExpr) exp;
        Expr      structure = lfae.getStructure();
        if (!(structure instanceof LoadDeclAddressExpr)) {
          map.removeElementAt(i + 1);
          map.removeElementAt(i);
        }
      }
    }
  }

  /**
   * Remove mappings that reference this field.
   */
  private void resetField(Vector<Object> map, LoadFieldAddressExpr lfae)
  {
    int       n         = map.size();
    FieldDecl fd        = lfae.getField();
    Expr      structure = lfae.getStructure().getReference();

    if (structure instanceof LoadDeclAddressExpr) {
      for (int i = n - 2; i >= 0; i -= 2) {
        Note exp2 = (Note) map.elementAt(i + 1);
        if (exp2 instanceof ExprChord)
          exp2 = ((ExprChord) exp2).getRValue();
        while (exp2 instanceof ConversionExpr)
          exp2 = ((ConversionExpr) exp2).getArg();
        LoadFieldValueExpr lfve = (LoadFieldValueExpr) exp2;
        if ((lfve.getField() == fd) &&
            structure.equivalent(lfve.getStructure().getReference())) {
          map.removeElementAt(i + 1);
          map.removeElementAt(i);
        }
      }
      return;
    }

    // If the structure reference is not simple, we have to go by just
    // the field name.

    for (int i = n - 2; i >= 0; i -= 2) {
      Note exp2 = (Note) map.elementAt(i + 1);
      if (exp2 instanceof ExprChord)
        exp2 = ((ExprChord) exp2).getRValue();
      while (exp2 instanceof ConversionExpr)
        exp2 = ((ConversionExpr) exp2).getArg();
      LoadFieldValueExpr lfve = (LoadFieldValueExpr) exp2;
      if (lfve.getField() == fd) {
        map.removeElementAt(i + 1);
        map.removeElementAt(i);
      }
    }
  }

  private Note lookup(Declaration decl)
  {
    int l = loadMap.size();
    for (int i = 0; i < l; i += 2)
      if (decl == loadMap.elementAt(i))
        return (Note) loadMap.elementAt(i + 1);
    return null;
  }

  private Note lookup(Expr key)
  {
    int l = loadMap.size();
    for (int i = 0; i < l; i += 2)
      if (key.equivalent((Expr) loadMap.elementAt(i)))
        return (Note) loadMap.elementAt(i + 1);

    return null;
  }

  /**
   * See if another array element address expression with the same
   * array and index have been found.  We don't care if the offsets
   * are different.
   */
  private Note lookupArrayIndexExpr(ArrayIndexExpr key)
  {
    Expr karray = key.getArray();
    Expr kindex = key.getIndex();
    int  l      = loadMap.size();
    for (int i = 0; i < l; i += 3) {
      Expr arr = (Expr) loadMap.elementAt(i + 0);
      Expr ind = (Expr) loadMap.elementAt(i + 1);
      if (karray.equivalent(arr) && kindex.equivalent(ind))
        return (Note) loadMap.elementAt(i + 2);
    }

    return null;
  }

  private void post(Declaration decl, Note value)
  {
    int l = loadMap.size();
    for (int i = 0; i < l; i += 2)
      if (decl == loadMap.elementAt(i)) {
        loadMap.setElementAt(value, i + 1);
        return;
      }
    loadMap.addElement(decl);
    loadMap.addElement(value);
  }

  private void post(Expr key, Note value)
  {
    int l = loadMap.size();
    for (int i = 0; i < l; i += 2)
      if (key.equivalent((Expr) loadMap.elementAt(i))) {
        loadMap.setElementAt(value, i + 1);
        return;
      }
    loadMap.addElement(key);
    loadMap.addElement(value);
  }

  /**
   * We care only if the array address and array index are the same.
   * We don't care if the offset is different.
   */
  private void postArrayIndexExpr(ArrayIndexExpr key, Note value)
  {
    Expr karray = key.getArray();
    Expr kindex = key.getIndex();
    int  l      = loadMap.size();

    for (int i = 0; i < l; i += 3) {
      Expr arr = (Expr) loadMap.elementAt(i + 0);
      Expr ind = (Expr) loadMap.elementAt(i + 1);
      if (karray.equivalent(arr) && kindex.equivalent(ind)) {
        loadMap.setElementAt(value, i + 2);
        return;
      }
    }

    loadMap.addElement(karray);
    loadMap.addElement(kindex);
    loadMap.addElement(value);
  }

  private void delete(Declaration decl)
  {
    int l = loadMap.size();
    for (int i = 0; i < l; i += 2)
      if (decl == loadMap.elementAt(i)) {
        loadMap.removeElementAt(i + 1);
        loadMap.removeElementAt(i);
        return;
      }
  }

  private void delete(Expr key)
  {
    int l = loadMap.size();
    for (int i = 0; i < l; i += 2)
      if (key.equivalent((Expr) loadMap.elementAt(i))) {
        loadMap.removeElementAt(i + 1);
        loadMap.removeElementAt(i);
        return;
      }
  }

  /**
   * Replace the expression with a load of the surrogate variable.
   */
  private void replace(ExprChord se, Expr old)
  {
    LoadExpr          le        = (LoadExpr) se.getLValue();
    Declaration       surrogate = le.getDecl();
    LoadDeclValueExpr third     = new LoadDeclValueExpr(surrogate);
    Note              sout      = old.getOutDataEdge();

    sout.changeInDataEdge(old, third);
    old.unlinkExpression();
    if (inSSA) {
      if (third.getChord() != null)
        third.setUseDef(se);
      copyPropagate(se, surrogate, sout, third);
    }
  }

  /**
   * Create a addition expression that computes the array element
   * address with the specified offset.
   * @param address is the address of the element at offset 0 (e.g.,
   * &a[i])
   * @param offset is the offset for the element address (e.g., &a[i +
   * offset])
   * @param se is the use-def link for the variable containing the
   * array address
   */
  private Expr genArrayElementAddress(Declaration address,
                                      Expr        offset,
                                      ExprChord   se)
  {
    Type              ot    = offset.getType();
    Type              pt    = address.getType();
    long              mult  = pt.getCoreType().getPointedTo().memorySize(Machine.currentMachine);
    Expr              elesz = new LiteralExpr(LiteralMap.put(mult, ot));
    Expr              off   = MultiplicationExpr.create(ot, offset.conditionalCopy(), elesz);
    LoadDeclValueExpr ldve  = new LoadDeclValueExpr(address);

    if (inSSA)
      ldve.setUseDef(se);

    return new AdditionExpr(pt, ldve, off);
  }

  /**
   * Replace the expression with a load of the surrogate variable.
   */
  private void replaceArrayIndexExpr(ExprChord se, ArrayIndexExpr old)
  {
    Expr offset = old.getOffset();

    old.setRA(null);

    LoadDeclAddressExpr le        = (LoadDeclAddressExpr) se.getLValue();
    Declaration         surrogate = le.getDecl();
    Expr                third     = genArrayElementAddress(surrogate, offset, se);
    Note                sout      = old.getOutDataEdge();

    sout.changeInDataEdge(old, third);
    old.unlinkExpression();
  }

  /**
   * Perform copy propagation if possible.
   */
  private void copyPropagate(ExprChord   se,
                             Declaration surrogate,
                             Note        sout,
                             Expr        third)
  {
    if (!doCopyPropagate)
      return;

    if (sout instanceof DualExpr)
      sout = ((DualExpr) sout).getOutDataEdge();

    if (!(sout instanceof ExprChord))
      return;

    ExprChord s   = (ExprChord) sout;
    Expr      lhs = s.getLValue();
    if (lhs == third)
      return;

    if (!(lhs instanceof LoadDeclAddressExpr))
      return;

    if (((LoadDeclAddressExpr) lhs).getDecl().addressTaken())
      return;

    int n = s.numDefUseLinks();
    for (int i = n - 1; i >= 0; i--) {
      LoadExpr ld = s.getDefUse(i);
      if ((ld instanceof LoadDeclValueExpr) &&
          !(ld.getChord() instanceof PhiExprChord)) {
        ld.setDecl(surrogate);
        ld.setUseDef(se);
      }
    }
  }

  /**
   * Check this load of a variable to see if it can be replaced.
   * @param load is the load expression
   */
  private void checkLoadExpr(LoadExpr ldve)
  {
    Declaration decl = ldve.getDecl();
    if ((decl.residency() != Residency.MEMORY) || decl.getType().isVolatile())
      return;  // Other optimizations take care of non-memory variables.

    if (decl.hasHiddenAliases())
      return;

    Note first = lookup(decl);
    if (first == null) { // Record first use of a variable.
      post(decl, ldve); // Map from name to first load
      return;
    }

    if (first instanceof ExprChord) { // Handle third & fourth references to the variable.
      replace((ExprChord) first, ldve);
      opCount++;
      return;
    }

    // This is the second reference to the variable.
    // Store first into a temporary variable, replace second by temporary.

    ExprChord se = mapIt((LoadExpr) first, ldve);

    post(decl, se); // Map from name to store into temporary.
    opCount++;
  }

  /**
   * Check this expression to see if it can be replaced.
   * @param expr is the expression
   */
  private void checkExpr(Expr expr)
  {
    Note first = lookup(expr);
    if (first == null) { // Record first use of a variable.
      post(expr, expr); // Map from name to first load
      return;
    }

    if (first instanceof ExprChord) { // Handle third & fourth references to the variable.
      replace((ExprChord) first, expr);
      opCount++;
      return;
    }

    // This is the second reference to the variable.
    // Store first into a temporary variable, replace second by temporary.

    ExprChord se = mapIt((Expr) first, expr);

    post(expr, se); // Map from name to store into temporary.
    opCount++;
  }

   /**
   * Check this array element address expression to see if it can be
   * replaced.
   * @param expr is the expression
   * @return the expression (first time) or null (second and after)
   */
  private Expr checkArrayIndexExpr(ArrayIndexExpr expr)
  {
    Note f = lookupArrayIndexExpr(expr);
    if (f == null) { // Record first use of a variable.
      postArrayIndexExpr(expr, expr); // Map from name to first load
      return expr;
    }

    if (f instanceof ExprChord) { // Handle third & fourth references to the variable.
      replaceArrayIndexExpr((ExprChord) f, expr);
      opCount++;
      return null;
    }

    // This is the second reference to the variable.
    // Store first into a temporary variable, replace second by temporary.
    // Create a surrogate temporary variable to hold the array element
    // address with a 0 offset.

    ArrayIndexExpr first = (ArrayIndexExpr) f;
    Expr           array = first.getArray();
    Expr           index = first.getIndex();

    first.setLA(null);
    first.setMA(null);

    Chord               pos   = first.getChord();
    Type                ft    = first.getType().getNonAttributeType();
    VariableDecl        temp  = genTemp(ft);
    Note                fout  = first.getOutDataEdge();
    Note                sout  = expr.getOutDataEdge();
    LiteralExpr         zero  = new LiteralExpr(LiteralMap.put(0, index.getType()));
    ArrayIndexExpr      naie  = new ArrayIndexExpr(ft, array, index, zero);
    LoadDeclAddressExpr ldae  = new LoadDeclAddressExpr(temp);
    ExprChord           se    = new ExprChord(ldae, naie);
    LoadDeclValueExpr   sldve = new LoadDeclValueExpr(temp);
    Expr                as    = genArrayElementAddress(temp, expr.getOffset(), se);
    LoadDeclValueExpr   fldve = new LoadDeclValueExpr(temp);
    Expr                af    = genArrayElementAddress(temp, first.getOffset(), se);

    se.setVisited();
    se.copySourceLine(pos);

    sout.changeInDataEdge(expr, as);
    expr.unlinkExpression();
    fout.changeInDataEdge(first, af);
    first.unlinkExpression();

    newCFGNodeCount++;
    pos.insertBeforeInCfg(se);
    dChanged = true;
    rChanged = true;

    postArrayIndexExpr(expr, se); // Map from name to store into temporary.
    opCount++;

    return null;
  }

  private Type getTempVarType(Type type)
  {
    Type ft = type.getNonAttributeType();
    if (ft.getCoreType().isPointerType()) {
      Type      ty = ft.getCoreType().getPointedTo();
      ArrayType at = ty.getCoreType().returnFixedArrayType();
      if (at != null)
        ft = PointerType.create(at.getElementType());
    }
    return ft;
  }

   /**
   * Create a surrogate temporary variable to hold the value or address.
   * @param first is the first occurrance of the address or variable
   * @param second is the second  occurrance of the address or variable
   */
  private ExprChord mapIt(Expr first, Expr second)
  {
    Chord             pos   = first.getChord();
    Type              ft    = getTempVarType(first.getType());
    VariableDecl      temp  = genTemp(ft);
    Note              fout  = first.getOutDataEdge();
    Note              sout  = second.getOutDataEdge();
    LoadDeclValueExpr sldve = new LoadDeclValueExpr(temp);
    LoadDeclValueExpr fldve = new LoadDeclValueExpr(temp);

    sout.changeInDataEdge(second, sldve);
    second.unlinkExpression();
    fout.changeInDataEdge(first, fldve);

    ExprChord se = new ExprChord(new LoadDeclAddressExpr(temp), first.addCast(ft));
    se.setVisited();
    se.copySourceLine(pos);

    if (inSSA) {
      if (sldve.getChord() != null)
        sldve.setUseDef(se);
      if (fldve.getChord() != null)
        fldve.setUseDef(se);

      copyPropagate(se, temp, sout, sldve);
      copyPropagate(se, temp, fout, fldve);
    }

    newCFGNodeCount++;
    pos.insertBeforeInCfg(se);
    dChanged = true;
    rChanged = true;
    return se;
  }

  /**
   * Return true if a declaration is referenced in the remainder of
   * the basic block.  Start at the CFG node after the specified node.
   */
  private boolean isReferencedAgain(Chord c, Declaration decl, Stack<Expr> ewl)
  {
    do {
      c = c.getNextChord();
      if (c == null)
        break;

      if (c.isAssignChord())
        ewl.push(((ExprChord) c).getRValue());
      else
        c.pushInDataEdges(ewl);

      while (!ewl.empty()) {
        Expr cure = ewl.pop().getLow();
        if (cure.containsDeclaration(decl))
          return true;
      }
    } while (!c.isLastInBasicBlock());

    return false;
  }

  /**
   * Return whether this optimization requires that the CFG be in SSA
   * form.  It returns either
   * <dl>
   * <dt><b>NO_SSA</b><dd>the CFG must not be in SSA form,
   * <dt>IN_SSA<dd>the CFG must be in SSA form,
   * <dt>VALID_SSA<dd>the CFG must be in valid SSA form, or
   * <dt>NA_SSA<dd>the CFG does not need to be in valid SSA form.
   * </dl>
   */
  public int requiresSSA()
  {
    return NA_SSA;
  }

  /**
   * Convert some calls to printf() to calls to faster fuunctions.
   * The idea for this came from a web site that discusses the
   * optimizations that gcc performs for printf().
   *
   * Case 1: printf("A")           => putchar('A')
   * Case 2: printf("abc\n")       => puts("abc")
   * Case 3: printf("%s\n", "abc") => puts("abc")
   * Case 4: printf("%c", 'a')     => putchar('a')
   * Case 5: printf("")            =>
   */
  private Expr optimizePrintfCall(CallFunctionExpr cf)
  {
    int l = cf.numArguments();
    if (l <= 0)
      return null;

    Expr ftn = cf.getFunction();
    if (!(ftn instanceof LoadDeclAddressExpr))
      return null;

    LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) ftn;
    RoutineDecl         rd   = (RoutineDecl) ldae.getDecl();
    if (!rd.getName().equals("printf"))
      return null;

    if (rd.visibility() != Visibility.EXTERN)
      return null;

    StringLiteral lit = getStringLiteral(cf.getArgument(0));
    if (lit == null)
      return null;

    String str       = lit.getStringValue();
    int    sl        = str.length();
    Type   char_type = Machine.currentMachine.getSignedCharType();

    if (l == 1) { // Case 1: printf("A") => putchar('A')
      if (str.charAt(sl - 1) != '\0')
        return null;

      if (sl == 1)
        return new NilExpr();

      if (sl == 2) {
        RoutineDecl putchar = defPutchar();
        if (putchar == null)
          return null;

        Vector<Expr> args = new Vector<Expr>(1);
        args.add(new LiteralExpr(LiteralMap.put((int) str.charAt(0), char_type)));
        LoadDeclAddressExpr ldftn = new LoadDeclAddressExpr(putchar);
        return  new CallFunctionExpr(VoidType.type, ldftn, args);
      }

      if ((sl > 2) &&
          (str.charAt(sl - 2) == '\n') &&
          (0 > str.indexOf('%'))) { // Case 2: printf("abc\n") => puts("abc")
        RoutineDecl puts = defPuts();
        if (puts == null)
          return null;

        Vector<Expr> args = new Vector<Expr>(1);

        str = str.substring(0, sl - 2) + '\0';

        Type          et  = RefType.create(char_type, RefAttr.Const);
        Type          nt  = FixedArrayType.create(0, sl - 1, et);
        StringLiteral ns  = LiteralMap.put(str, nt);
        VariableDecl  nvd = new VariableDecl(un.genName(), nt, ns);

        scribble.addDeclaration(nvd);
        nvd.setResidency(Residency.MEMORY);
        nvd.setReferenced();

        args.add(new LoadDeclAddressExpr(nvd));
        LoadDeclAddressExpr ldftn = new LoadDeclAddressExpr(puts);
        return  new CallFunctionExpr(VoidType.type, ldftn, args);
      }

      return null;
    }

    if (l == 2) {
      if ("%s\n\0".equals(str)) { // Case 3: printf("%s\n", "abc") => puts("abc")
        RoutineDecl puts = defPuts();
        if (puts == null)
          return null;

        Vector<Expr> args = new Vector<Expr>(1);
        args.add(cf.getArgument(1).copy());
        LoadDeclAddressExpr ldftn = new LoadDeclAddressExpr(puts);
        return  new CallFunctionExpr(VoidType.type, ldftn, args);
      }

      if ("%c\0".equals(str)) { // Case 4: printf("%c", 'a') => putchar('a')
        RoutineDecl putchar = defPutchar();
        if (putchar == null)
          return null;

        Vector<Expr> args = new Vector<Expr>(1);
        args.add(cf.getArgument(1).copy());
        LoadDeclAddressExpr ldftn = new LoadDeclAddressExpr(putchar);
        return  new CallFunctionExpr(VoidType.type, ldftn, args);
      }
    }

    return null;
  }

  private StringLiteral getStringLiteral(Object arg)
  {
    if (!(arg instanceof LoadDeclAddressExpr))
      return null;

    LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) arg;
    VariableDecl        vd   = ldae.getDecl().returnVariableDecl();
    if (vd == null)
      return null;

    FixedArrayType at = vd.getCoreType().getPointedToCore().returnFixedArrayType();
    if (at == null)
      return null;

    Expression val = vd.getValue();
    if (!(val instanceof StringLiteral))
      return null;

    return (StringLiteral) val;
  }

  private RoutineDecl defPuts()
  {
    if (putsValid)
      return putsDef;

    putsDef = null;
    putsValid = true;

    Type                  char_type = Machine.currentMachine.getSignedCharType();
    CallGraph             cg        = scribble.getRoutineDecl().getCallGraph();
    Iterator<RoutineDecl> it        = cg.allRoutines();
    while (it.hasNext()) {
      RoutineDecl rd = it.next();
      if (!rd.getName().equals("puts"))
        continue;

      ProcedureType pt = (ProcedureType) rd.getCoreType();
      if (pt.numFormals() != 1)
        return null;

      FormalDecl fd = pt.getFormal(0);
      Type       ft = fd.getCoreType();
      if (!ft.isPointerType())
        return null;

      if (ft.getPointedToCore() != char_type)
        return null;

      putsDef = rd;
      return rd;
    }

    Type               charp_type = PointerType.create(char_type);
    Vector<FormalDecl> formals    = new Vector<FormalDecl>(1);

    formals.add(new FormalDecl("a", charp_type));

    ProcedureType pt = ProcedureType.create(VoidType.type, formals, null);

    putsDef = new ProcedureDecl("puts", pt);
    putsDef.setVisibility(Visibility.EXTERN);
    cg.recordRoutine(putsDef);

    return putsDef;
  }

  private RoutineDecl defPutchar()
  {
    if (putcharDef != null)
      return putcharDef;

    putcharDef = null;
    putcharValid = true;

    Type                  int_type = Machine.currentMachine.getSignedIntType();
    CallGraph             cg        = scribble.getRoutineDecl().getCallGraph();
    Iterator<RoutineDecl> it        = cg.allRoutines();
    while (it.hasNext()) {
      RoutineDecl rd = it.next();
      if (!rd.getName().equals("putchar"))
        continue;

      if (rd.visibility() != Visibility.EXTERN)
        return null;

      ProcedureType pt = (ProcedureType) rd.getCoreType();
      if (pt.numFormals() != 1)
        return null;

      FormalDecl fd = pt.getFormal(0);
      if (!fd.getCoreType().isIntegerType())
        return null;

      putcharDef = rd;
      return rd;
    }

    Vector<FormalDecl> formals = new Vector<FormalDecl>(1);

    formals.add(new FormalDecl("a", int_type));

    ProcedureType pt = ProcedureType.create(VoidType.type, formals, null);

    putcharDef = new ProcedureDecl("putchar", pt);
    putcharDef.setVisibility(Visibility.EXTERN);
    cg.recordRoutine(putcharDef);

    return putcharDef;
  }
}
