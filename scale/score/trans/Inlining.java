package scale.score.trans;

import java.io.PrintWriter;
import java.util.Iterator;
import java.text.DecimalFormat;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.pred.*;
import scale.score.chords.*;
import scale.score.pp.PPCfg;
import scale.score.analyses.Aliases;

import scale.callGraph.*;
import scale.annot.*;
import scale.clef.Node;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.*;

import scale.backend.ICEstimator;

/**
 * This class performs inlining.
 * <p>
 * $Id: Inlining.java,v 1.84 2007-10-04 19:58:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Inlining replaces routine calls by the code of the routine called.
 * It ranks all routine calls across all routines and replaces the
 * calls with inlined code in the order of that ranking.
 * <p>
 * The ranking depends on the nested loop depth of the call and the
 * "cost" of the inlined code.  The deeper the call is in a loop nest
 * the more likely the call will be replaced by inlined code.  The
 * smaller the routine is the more likely it will be inlined.  These
 * metrics can be over-ridden by using profile-guided inlining.
 * <p>
 * Not all calls are replaced that satisfy the above criteria.  No
 * routine is inlined if the resultant module code size will exceed
 * the bloat factor.  No routine is inlined if a heuristic determines
 * that the call is a poor choice to be replaced by an inlined
 * routine.
 * <p>
 * <b>Code Bloat</b>: A floating point value, called the bloat factor,
 * is used.  This value is multiplied by the current module size prior
 * to inlining to give the maximum size to which that the resultant
 * module is allowed to grow. No routine call is replaced with an
 * inlined routine if the result will cause the module to exceed the
 * maximum size.  Values for the bloat factor can be greater than or
 * equal to 1.0.  (If the value specified is less than 1.0, the value
 * of 1.0 is used.  Note - this may still allow some inlining if the
 * inlined routine is smaller than the call overhead.)
 * <p>
 * <b>Heuristics</b>: The following heuristics are used to inhibit the
 * replacement of a call by an inlined routine:
 * <ul>
 * <li>No indirect calls are replaced.
 * <li>No recursive routines are inlined.
 * <li>No routine from a different module that references
 * <code>static</code> routines or variables is inlined. (Note: when
 * multi-compilation is used, the compiler converts all
 * <code>static</code> routines and variables to globals.)
 * <li>No call is replaced by a routine whose parameter list does not
 * match the call's argument list.  This inhibits inlining any routine
 * that uses <code>va_start()</code>.
 * <li>No routine is inlined that has a variable with an address
 * initialization.  For example,
 * <pre>
 *   int (*abc)()[2] = {ftn1, ftn2};
 * </pre>
 * <li>Complex routines are inlined only into "simple" routines.  A
 * simple routine is one with a small number of estimated
 * instructions.  A complex routine is one that contains more than one
 * loop.  The instruction count limit is tunable.  The inlining
 * optimization can be told to ignore this heuristic.
 * </ul>
 */
public final class Inlining
{
  /**
   * Assumed cost, in instructions, of a subroutine call.
   */
  public static int callOverhead = 5;
  /**
   * Maximum size in instructions of a "simple" function.
   */
  public static int simpleFtnLimit = 30;
  /**
   * True if "complexity" heuristic for inhibiting the inlining of a
   * routine should be ignored.
   */
  public static boolean ignoreComplexityHeuristic = false;

  public static boolean classTrace;

  private static int newCFGNodeCount = 0; // A count of new nodes created.
  private static int inLineCount     = 0; // The number of times a routine was inlined.

  private static final String[] stats = {"inlined", "newCFGNodes"};

  static
  {
    Statistics.register("scale.score.trans.Inlining", stats);
  }

  /**
   * Return the number of times inlining was performed.
   */
  public static int inlined()
  {
    return inLineCount;
  }

  /**
   * Return the number of new nodes created.
   */
  public static int newCFGNodes()
  {
    return newCFGNodeCount;
  }

  private static final String PREFIX = "_in"; // Prefix to new variable names

  /**
   * Call inlining status.
   */
  private static final byte CANDIDATE  =  0; // The call can be inlined.
  private static final byte INDIRECT   =  1; // The call is indirect.
  private static final byte RECURSIVE  =  2; // The call is to a recursive routine.
  private static final byte EXTERN     =  3; // The call is to a routine outside of the module.
  private static final byte STATICS    =  4; // The called function references statics.
  private static final byte ARGUMENTS  =  5; // The argument list and parameter list disagree.
  private static final byte ADDRESSLIT =  6; // The called function uses address literals.
  private static final byte COMPLEX    =  7; // The called function is complex.
  private static final byte MAXSIZE    =  8; // The bloat factor would be exceeded.
  private static final byte FORM       =  9; // The form of the call is not handled.
  private static final byte RULEDOUT   = 10; // The callee can't be inlined.
  private static final byte INLINED    = 11; // The call was inlined.

  private static final String[] statusText = {
    "candidate", "indirect",  "recursive",  "extern",
    "statics",   "arguments", "addresslit", "complex",
    "maxsize",   "form",      "ruledout",   "inlined"
  };

  private HashMap<RoutineDecl, Boolean> staticRefMap; // See hasNonConstStaticReference()
  private Vector<RoutineDecl>           procedureMap; // The list of routines that could be inlined.

  private Suite         suite;
  private UniqueName    un;
  private ICEstimator   ice;           // Estimates size of things in instructions.
  private RoutineDecl[] callers;       // The caller.
  private RoutineDecl[] callees;       // The callee.
  private int[]         callLoopDepth; // The nested loop depth of the call.
  private CallExpr[]    calls;         // The call expression.
  private byte[]        status;        // The "inlining" status of the call.
  private int[]         sorted;        // The sorted order of the calls.
  private int           callCount;     // The number of calls.
  private int           numCalls;      // The number of calls in the sorted list.
  private int           largestSize;   // Size of the largest routine.
  private double        bloatFactor;   // The maximum expansion factor.

  private boolean trace;
  private boolean allowCrossModuleInlining; // True if cross-module inlining has not been disabled.

  /**
   * Prepare for an inlining batch.  
   * @param suite the suite containing the procedures that will be
   * inlined and the list of globals that might be modified
   */
  public Inlining(Suite suite, boolean allowCrossModuleInlining) 
  {
    this.un              = new UniqueName(PREFIX);
    this.suite           = suite;
    this.procedureMap    = new Vector<RoutineDecl>(100);
    this.trace           = classTrace || Debug.debug(3);
    this.allowCrossModuleInlining = allowCrossModuleInlining;
    this.ice             = Machine.sGetInstructionCountEstimator();

    this.callers       = new RoutineDecl[100];
    this.callees       = new RoutineDecl[100];
    this.callLoopDepth = new int[100];
    this.calls         = new CallExpr[100];
    this.status        = new byte[100];
    this.sorted        = new int[100];
    this.numCalls      = 0;
    this.callCount     = 0;

    if (trace)
      System.out.println("Starting Inlining");

    Iterator<RoutineDecl> it = suite.allRoutines();
    while (it.hasNext()) {
      RoutineDecl rd       = it.next();
      Scribble    scribble = rd.getScribbleCFG();

      // We can't inline into routines that are not optimizable
      // because this means that their CFG form is not correct.  There
      // may be missing loop headers and/or loop exits so logic that
      // depends on these being valid will fail or worse.

      if ((scribble == null) || scribble.isIrreducible())
        continue;

      procedureMap.addElement(rd);
    }
  }

  /**
   * This is the main function for this class.  It 
   * makes a list of all the candidate calls, and gives each
   * call a priority for inlining, based on an estimate of
   * how many times each one is called dynamically, and
   * how large the function being inlined is.  
   * <p>
   * In order of decreasing priority, each call that can be 
   * inlined within the given code-bloat maximum is, until 
   * we run out of candidates or reach our minimum priority
   * bound.
   * @param bloatFactor the level of inlining to be performed --
   * higher levels allow for more code bloat
   */
  public void optimize(double bloatFactor)
  {
    this.bloatFactor = bloatFactor;
    if (bloatFactor < 1.0)
      bloatFactor = 1.0;

    int originalSize = calcRoutineSizes();
    int currentSize  = originalSize;
    int maxSize      = (int) (originalSize * bloatFactor);

    numCalls = 0;

    pruneCalls(0);

    // Inline the calls in order of importance.
    // Note - inlining may create more calls; that's why we iterate.

    boolean flag = true;
    do { // Until we fail to inline anything.
      int l = sortCandidates();
      if (l <= 0) // No candidates left.
        break;

      // Inline the best candidates.

      flag = false;
      for (int i = 0; i < l; i++) {
        int         ci        = sorted[i];
        RoutineDecl callee    = callees[ci];
        RoutineDecl caller    = callers[ci];
        int         numArgs   = callee.getSignature().numFormals();
        int         addedSize = (callee.getCost() - (callOverhead + 2 * numArgs));

        if (status[ci] != CANDIDATE)
          continue;

        int newSize = currentSize + addedSize;
        if (newSize > maxSize) {
          status[ci] = MAXSIZE;
          continue;
        }
        
        int start = callCount;

        // This one is not too big to inline in space remaining.

        boolean inlined = inlineSingleCall(ci);

        if (!inlined)
          continue;

        status[ci] = INLINED; // Mark as done.

        pruneCalls(start);

        flag = true;
        currentSize = newSize;
      }
    } while (flag);

    if (Debug.debug(1)) {
      int l = procedureMap.size();
      for (int i = 0; i < l; i++) {
        RoutineDecl pi       = procedureMap.elementAt(i);
        Scribble    scribble = pi.getScribbleCFG();
        scribble.validateCFG();
      }
    }
  }

  /**
   * Eliminate from the list of calls those calls that we
   * do not want to inline.
   * @param start is the index of the first call in the list to check
   */
  private void pruneCalls(int start)
  {
    for (int i = start; i < callCount; i++) {
      if (!isCandidate(i))
        continue;
      sorted[numCalls] = i;
      numCalls++;
    }
  }

  /**
   * Determine the size, in instructions, of each routine.
   * @return the sum of all the routine sizes.
   */
  private int calcRoutineSizes()
  {
    largestSize = 0;

    int totalSize = 0;
    int ll        = procedureMap.size();
    for (int i = 0; i < ll; i++) {
      RoutineDecl d        = procedureMap.elementAt(i);
      Scribble    scribble = d.getScribbleCFG();

      // If we're using the profile (or maintaining it), convert the
      // path profiling CFG to a cyclic CFG.

      PPCfg ppcfg = scribble.getPPCfg();
      if ((ppcfg != null) && !ppcfg.isCyclic())
        ppcfg.makeCyclicPreservingEdgeProfile();

      int size = findCalls(scribble.getBegin(), d, null);

      d.setCost(size);
      totalSize += size;
      if (size > largestSize)
        largestSize = size;
    }
    return totalSize;
  }

  /**
   * Change the form of the call such that it is alone on the RHS and
   * is assigned to a LoadExpr.
   */
  private ExprChord fixForm(CallExpr call, Chord callStmt, RoutineDecl caller)
  {
    // Create a variable to hold the call's result.
    Type t = call.getType();
    VariableDecl vd = genTemp(t, caller.getScribbleCFG());
    LoadDeclAddressExpr ldae = new LoadDeclAddressExpr(vd);
    LoadDeclValueExpr ldve = new LoadDeclValueExpr(vd);

    // Remove the call expression from its previous CFG node.
    Note n = call.getOutDataEdge();
    n.changeInDataEdge(call, ldve);

    // Create a CFG node for the assignment.
    ExprChord ec = new ExprChord(ldae, call);

    // Link the CFG node properly.
    callStmt.insertBeforeInCfg(ec);

    return ec;
  }

  /**
   * Inline a single function call.  
   * @param ci specifies the call to inline
   * @return true if the inlining attempt was successfull
   */
  private boolean inlineSingleCall(int ci) 
  {
    CallExpr    call     = calls[ci];
    Chord       callStmt = call.getChord();
    RoutineDecl callee   = callees[ci];
    RoutineDecl caller   = callers[ci];

    if (!(callStmt instanceof ExprChord)) {
      // I think FORM is reported when a call is successfully inlined,
      // because the original call now exists somewhere disconnected
      // from the CFG.  I'm not 100% sure, but someone might want to
      // check this in the future.  -- bmaher, 9/22/08
      status[ci] = FORM;
      return false;
    }

    ExprChord se  = (ExprChord) callStmt;
    Expr      lhs = se.getLValue();
    Expr      rhs = se.getRValue();
    boolean   fixit = false;

    if (rhs != call) {
      fixit = true;
    }

    LoadExpr rv = null;
    if (lhs != null) {
      if (!(lhs instanceof LoadExpr)) {
        fixit = true;
      } else {
        rv = (LoadExpr) lhs;
      }
    }

    if (fixit) {
      ExprChord newStmt = fixForm(call, callStmt, caller);
      callStmt = newStmt;
      lhs = newStmt.getLValue();
      rhs = newStmt.getRValue();
      rv  = (LoadExpr) lhs;
    }

    Chord           out   = callStmt.getNextChord();
    LoopHeaderChord lh    = callStmt.getLoopHeader();
    HashMap<Chord, Chord> nm    = new HashMap<Chord, Chord>(203); // Map from old CFG nodes to new ones.
    Chord           newIn = inlineFtnCall(ci, call, out, lh, rv, nm);

    if (newIn == null) { // Remove call.

      if (true) {
        System.err.println("Caller: " + caller.getName());
        System.err.println("Callee: " + callee.getName());
        throw new scale.common.RuntimeException("Did not expect to reach here");
      }

      callStmt.removeFromCfg();

      PPCfg callerPPCfg = caller.getScribbleCFG().getPPCfg();
      if (callerPPCfg != null)
        callerPPCfg.removeAndUpdate(callee.getScribbleCFG().getPPCfg(),
                                    callStmt,
                                    nm);

      return true;
    }

    Chord[] inArray = callStmt.getInCfgEdgeArray();
    for (int i = 0; i < inArray.length; i++) {
      callStmt.deleteInCfgEdge(inArray[i]);
      inArray[i].replaceOutCfgEdge(callStmt, newIn);    
      newIn.addInCfgEdge(inArray[i]);
    }

    callStmt.removeFromCfg();

    PPCfg callerPPCfg = caller.getScribbleCFG().getPPCfg();
    if (callerPPCfg != null)
      callerPPCfg.inlineCall(callee.getScribbleCFG().getPPCfg(),
                             callStmt,
                             newIn,
                             nm);

    // Add new candidates and update the caller procedure's size.

    findCalls(newIn, caller, out);

    caller.addCost(callee.getCost());

    return true;
  }

  /**
   * This method inlines a call given the call expression and the
   * declaration of the variable that will be used to hold the result
   * value of the function.  The <code>out</code> parameter should be
   * a Chord with no in-coming CFG edges to which the inlined code
   * will escape.  In-coming CFG edges will be added to it by this
   * method.
   * <p>
   * If the method returns <code>null</code>, then the function was
   * empty.  If the method returns <code>out</code> then the inlining
   * was not performed.
   * @param ci specifies the function call information
   * @param call is the call being inlined
   * @param out is the target of the inlined function
   * <code>return</code> statements
   * @param lh is the new parent for any top level loops in the
   * inlined code
   * @param returnVar is the variab;e to use for function results
   * @return the first CFG node of the inlined code (which has no
   * in-coming CFG edges)
   */
  private Chord inlineFtnCall(int              ci,
                              CallExpr         call,
                              Chord            out,
                              LoopHeaderChord  lh,
                              Expr             returnExpr,
                              HashMap<Chord, Chord> nm)
  {
    Scribble      callerCfg = callers[ci].getScribbleCFG();
    Scribble      calleeCfg = callees[ci].getScribbleCFG();
    ProcedureType pt        = callees[ci].getSignature();

    inLineCount++;

    if (trace) {
      System.out.print("Adding ");
      System.out.print(calleeCfg.getRoutineDecl().getName());
      System.out.print(" to ");
      System.out.println(callerCfg.getRoutineDecl().getName());
    }

    int lll = calleeCfg.numDecls();
    for (int i = 0; i < lll; i++) {
      Declaration decl = calleeCfg.getDecl(i);
      if (decl instanceof TypeDecl)
        callerCfg.addDeclaration(decl);
    }

    Stack<Chord>                 wl      = WorkArea.<Chord>getStack("inlineFtnCall");
    HashMap<Declaration, Object> declMap = new HashMap<Declaration, Object>(203); // Map from old declaration to the new one.
    BeginChord begin   = calleeCfg.getBegin();
    EndChord   end     = calleeCfg.getEnd();

    // Patch up function value return.

    if ((end instanceof LeaveChord) && (returnExpr != null)) {
      Expr re  = ((LeaveChord) end).getResultValue();
      if (re != null) {
        Type rvt = returnExpr.getCoreType();
        Type ret = re.getCoreType();
        if ((re instanceof LoadDeclValueExpr) && ret.equivalent(rvt)) {
          LoadDeclValueExpr rv = (LoadDeclValueExpr) re;
          if (returnExpr instanceof LoadDeclAddressExpr)
            declMap.put(rv.getDecl(), ((LoadDeclAddressExpr) returnExpr).getDecl());
          else
            declMap.put(rv.getDecl(), returnExpr);
        } else {
          // TODO: What if re is a complex expression.  Does this ever happen?
          Expr rex = re.copy();
          if (rex instanceof LoadDeclValueExpr) {
            LoadDeclValueExpr rv = (LoadDeclValueExpr) rex;
            Declaration       d  = rv.getDecl();
            Declaration       nd = d.copy(getNewName(un.genName(), d.getName()));
            declMap.put(d, nd);
            callerCfg.addDeclaration(nd);
            rv.setDecl(nd);
          }

          CastMode  cr  = TypeConversionOp.determineCast(rvt, ret);
          if ((cr == CastMode.REAL) || (cr == CastMode.TRUNCATE))
            rex = ConversionExpr.create(rvt, rex, cr);
          ExprChord rec = new ExprChord(returnExpr.copy(), rex);
          rec.setTarget(out);
          rec.copySourceLine(out);
          out = rec;
        }
      }
    }

    // Copy the code to be inlined.

    Chord         start = begin.getTarget();
    Vector<Chord> newn  = new Vector<Chord>(256);

    nm.put(begin, lh); // Cause loop parents to be linked.
    nm.put(end, out);  // Cause the return to be eliminated.

    PPCfg callerPPCfg = callerCfg.getPPCfg();
    if (callerPPCfg != null) {

      // Make room in the caller for the callee's code by splitting
      // the call site basic block into two or three basic blocks.

      Chord callc = call.getChord();
      Chord first = callc.firstInBasicBlock();
      if (!callc.isFirstInBasicBlock())
        first = callerPPCfg.splitBlock(first, callc, false).firstChord();

      // Only split the block at out if
      //      (1) it is not already a basic block leader OR
      //      (2) out was changed above (see out = cr) 

      if (!out.isFirstInBasicBlock() || !out.equals(callc.getNextChord()))
        callerPPCfg.splitBlock(first, out, true);
    }

    Scribble.grabSubgraph(start, nm, newn, wl);
    Scribble.linkSubgraph(newn, nm, null);

    // Find and rename static local variables.

    boolean staticRemoved = removeStaticDecls(calleeCfg, declMap, wl);
    if (staticRemoved) {
      calleeCfg.recomputeRefs();
      calleeCfg.recomputeDominators();
    }

    // Either cause the argument to be used in place of the local
    // variable or generate copy nodes from the caller's argument to
    // the callee's parameter.

    Chord           ans  = null;
    SequentialChord last = null; // The chord created or visited last
    int             l    = call.numArguments();
    for (int i = 0; i < l; i++) {
      Expr       arg = call.getArgument(i);
      FormalDecl fd  = pt.getFormal(i);
      Type       ft  = fd.getType();

      // Inlining a routine that has array arguments can create
      // aliases so we try hard to use the original arrays.  This
      // allows us to do scalar replacement later.

      Declaration d   = getReference(arg);
      boolean     hha = false;
      if (d != null) {
        Type dt = d.getCoreType();

        if ((arg instanceof LoadDeclValueExpr) &&
            dt.equivalent(ft.getCoreType()) &&
            dt.getPointedTo().isArrayType()) {
          declMap.put(fd, d);
          continue;
        }

        if (ft.isPointerType()) {
          if ((arg instanceof LoadDeclAddressExpr) &&
              dt.equivalent(ft.getPointedToCore()) &&
              dt.isArrayType()) {
            declMap.put(fd, arg);
            continue;
          }

          VariableDecl vd = d.returnVariableDecl();
          if ((vd != null) && dt.getPointedTo().isArrayType()) {
            vd.setHiddenAliases();
            hha = true;
          }
        }
      }

      VariableDecl vd = new VariableDecl(getNewName(un.genName(), fd.getName()), ft);
      vd.setReferenced();
      callerCfg.addDeclaration(vd);
      declMap.put(fd, vd);

      if (fd.addressTaken())
        vd.setAddressTaken();

      if (hha)
        vd.setHiddenPtrAliases();

      // Remove the link between the expression and the call chord.

      arg.deleteOutDataEdge(call);

      Type ot = ft.getCoreType();
      Type nt = arg.getCoreType();
      Expr nl = new LoadDeclAddressExpr(vd);
      if (!ot.equivalent(nt)) {
        CastMode cr = CastMode.CAST;
        if (ot.isPointerType()) {
          // The source program has a type mismatch.  For example,
          // passing in a char * to a routine that requires a
          // different pointer type.
          cr = scale.clef.expr.CastMode.CAST;
        } else if (ot.isIntegerType() && nt.isIntegerType()) {
          cr = scale.clef.expr.CastMode.TRUNCATE;
        } else if (ot.isRealType() && nt.isRealType()) {
          cr = scale.clef.expr.CastMode.TRUNCATE;
        } else
          throw new scale.common.InternalError("Type mis-match " + arg + " " + fd);

        arg  = ConversionExpr.create(ft, arg, cr);
      }

      ExprChord c = new ExprChord(nl, arg);
      c.copySourceLine(call.getChord());

      if (last == null) {
        ans = c;
      } else {
        last.setTarget(c);
      }
      
      last = c;
    }

    // Link in the new copy statements.

    Chord nextChord = nm.get(start);
    if (last == null) {
      ans = nextChord;
    } else if (nextChord == null) {
      last.setTarget(out);
    } else {
      last.setTarget(nextChord);
    }

    CallGraph cg        = callers[ci].getCallGraph();
    HashMap<String, Declaration>   name2Decl = null;
    boolean   doci      = false;
    
    if (cg != callees[ci].getCallGraph()) { // Make global variables consistent.
      doci = true;
      name2Decl = new HashMap<String, Declaration>(203);

      Iterator<Declaration> e = cg.topLevelDecls();

      while (e.hasNext()) {
        Declaration d = e.next();
        if (d.isVariableDecl() || d.isProcedureDecl())
          name2Decl.put(d.getName(), d);
      }

      Iterator<RoutineDecl> it = cg.allRoutines();
      while (it.hasNext()) {
        RoutineDecl rd = it.next();
        name2Decl.put(rd.getName(), rd);
      }
    }

    // Replace or rename the appropriate variables.

    HashSet<String> callerNames = WorkArea.<String>getSet("inlineFtnCall");

    getNames(callerCfg, callerNames, wl); // Collect the caller's variable names.

    int len = newn.size();
    for (int i = 0; i < len; i++) {
      Chord c = newn.elementAt(i);

      if (c.isLoopHeader())
        ((LoopHeaderChord) c).setScribble(callerCfg);

      Vector<LoadExpr> v = c.getLoadExprList();
      if (v == null)
        continue;

      int ll = v.size();
      for (int j = 0; j < ll; j++) {
        LoadExpr le = v.elementAt(j);
        setDecls(le, callerCfg, declMap, callerNames);
        if (doci)
          setGlobalDecls(le, cg, name2Decl);
      }
    }

    setNames(callerCfg, callerNames, wl); // Rename the caller's variables that conflict.

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<String>returnSet(callerNames);

    if (calleeCfg.getRoutineDecl().usesAlloca())
      callerCfg.getRoutineDecl().setUsesAlloca();

    callerCfg.recomputeRefs();
    callerCfg.recomputeDominators();

    assert (ans != null) : "How can this happen " + call;

    return ans;
  }

  private VariableDecl getReference(Expr x)
  {
    x = x.getReference();

    if (x instanceof LoadExpr) {
      LoadExpr     ldae = (LoadExpr) x;
      VariableDecl vd   = ldae.getDecl().returnVariableDecl();
      if (vd != null)
        return vd;
    }
    return null;
  }

  /**
   * This method returns true if a declaration should be "treated as a
   * global".  It returns true if the declaration should be associated
   * with the scribble graph instead of the file.  The thing is that a
   * global equivalence decl is still local to a routine.
   */
  private boolean treatGlobal(Declaration d) 
  {
    return d.isGlobal() && !d.isEquivalenceDecl();
  }

  /**
   * The call with the highest priority is considered first for
   * inlining.  Currently, the priority is based only on a procedure's
   * size and how deep in a loop structure it is.  We assume that a
   * call that is nested three loops down is called more often than a
   * call that is only two loops down,
   */
  private double getPriority(int i) 
  {
    if (status[i] != CANDIDATE)
      return 0.0; // Already processed.

    RoutineDecl cn   = callees[i];
    int         size = cn.getCost();

    if (size <= 0)
      return 0.0;

    double sizeRatio = ((double) largestSize) / size;

    // Use path profiling if available.

    PPCfg ppcfg = cn.getScribbleCFG().getPPCfg();
    if (ppcfg != null) {
      // If we're using the profile (or maintaining it), make the
      // callee CFG cyclic.

      if (!ppcfg.isCyclic())
        ppcfg.makeCyclicPreservingEdgeProfile();

      Chord first = calls[i].getChord().firstInBasicBlock();

      // Profiling currently implies multi-compilation, so don't
      // consider largestSize.

      long freq = ppcfg.getBlockFreq(first);
      if (freq >= 0)
        return (freq * sizeRatio);
    }

    // Use block profiling if available.

    int callCnt = calls[i].getProfCallCnt();
    if (callCnt >= 0)
      return (callCnt * sizeRatio);

    // Set the priority based on the loop depth of the call and the
    // size of the callee.

    int level = callLoopDepth[i];
    return ((level + 1) * sizeRatio);
  }

  /**
   * Get the names of all the local variables of the routine.
   * @param scribble is the CFG for the routine
   * @param declSet returns the declarations
   * @param wl is a work stack - it is empty upon return
   */
  private void getNames(Scribble scribble, HashSet<String> declSet, Stack<Chord> wl)
  {
    RoutineDecl   d  = scribble.getRoutineDecl();
    ProcedureType pt = d.getSignature();
    int           ll = pt.numFormals();

    for (int i = 0; i < ll; i++)
      declSet.add(pt.getFormal(i).getName());

    Chord start = scribble.getBegin();

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();

      c.pushOutCfgEdges(wl);

      Vector<Declaration> v = c.getDeclList();
      if (v == null)
        continue;

      int l = v.size();
      for (int j = 0; j < l; j++) {
        Declaration decl = v.elementAt(j);

        if (treatGlobal(decl))
          continue;

        declSet.add(decl.getName());
      }
    }
  }

  /**
   * Change names in the caller's CFG.  The getNames method and
   * setNames method, and callerNames argument of the setDecls are
   * used to make sure that if a variable in the caller function hides
   * a global variable accessed in the callee function then the caller
   * function's variable is renamed.  This is important primarily
   * only when generating C code.
   * @param scribble specifies the caller's CFG
   * @param declSet specifies those names that do not need to be changed
   * @param wl is a work stack - it is empty upon return
   */
  private void setNames(Scribble        scribble,
                        HashSet<String> declSet,
                        Stack<Chord>    wl)
  {
    RoutineDecl   d  = scribble.getRoutineDecl();
    ProcedureType pt = d.getSignature();
    int           l  = pt.numFormals();

    for (int i = 0; i < l; i++) {
      FormalDecl fd   = pt.getFormal(i);
      String     name = fd.getName();
        
      if (declSet.contains(name))
        continue;

      fd.setName(getNewName(un.genName(), name));
      declSet.add(fd.getName());
    }

    Chord start = scribble.getBegin();
    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();

      c.pushOutCfgEdges(wl);

      Vector<Declaration> v = c.getDeclList();
      if (v == null)
        continue;

      int ll = v.size();
      for (int i = 0; i < ll; i++) {
        Declaration decl = v.elementAt(i);
        String      name = decl.getName();
        if (declSet.contains(name))
          continue;

        if (treatGlobal(decl))
          continue;

        decl.setName(getNewName(un.genName(), name));
        declSet.add(decl.getName());
      }
    }
  }    

  /**
   * This method is used to create new declarations for each
   * non-global declaration in the inlined code.
   * @param le contains the declaration to be changed
   * @param scribble is the caller's CFG
   * @param declMap maps from old declarations to new declarations
   * @param callerNames is a list of names used in the caller
   */
  private void setDecls(LoadExpr le,
                        Scribble scribble,
                        HashMap<Declaration, Object>  declMap,
                        HashSet<String>  callerNames)
  {
    Declaration decl = le.getDecl();
    String      name = decl.getName();

    if (treatGlobal(decl)) {
      callerNames.remove(name);
      return;
    }

    Object o = declMap.get(decl);
    if (o == null) {
      Declaration     newDecl = null;
      EquivalenceDecl ed1     = decl.returnEquivalenceDecl();
      if (ed1 != null) {
        // Find the equivalent COMMON variable in the caller's program.
        Type         ty1 = ed1.getType();
        VariableDecl bv1 = ed1.getBaseVariable();
        long         of1 = ed1.getBaseOffset();
        int          l   = scribble.numDecls();

        boolean sameBase = false; // True if same COMMON referenced in both caller and callee.
        for (int i = 0; i < l; i++) {
          Declaration decl2 = scribble.getDecl(i);
          if (!decl2.isEquivalenceDecl())
            continue;

          EquivalenceDecl ed2 = (EquivalenceDecl) decl2;
          if (bv1 != ed2.getBaseVariable())
            continue; // Different COMMON.

          sameBase = true;

          if (of1 != ed2.getBaseOffset())
            continue; // Different variable.

          if (!ty1.equivalent(ed2.getType()))
            continue; // Declared with a different type;

          newDecl = ed2;
          break;
        }
        if (sameBase && (newDecl == null)) {
          // Couldn't find the equivalent COMMON variable in the
          // caller.  The programmer must have laid out the COMMON
          // differently in the caller and the callee.
          bv1.setHiddenAliases();
        }
      }

      if (newDecl == null) {
        newDecl = decl.copy(getNewName(un.genName(), name));
        scribble.addDeclaration(newDecl);
      }

      declMap.put(decl, newDecl);

      le.setDecl(newDecl);
      return;
    }

    if (o instanceof Declaration) {
      Declaration newDecl = (Declaration) o;
      le.setDecl(newDecl);
      return;
    }

    le.getOutDataEdge().changeInDataEdge(le, ((Expr) o).copy());
    le.unlinkExpression();
  }

  /**
   * Copy the static declarations of the called routine and make these
   * declarations global.
   * @param calleeCFG is the original code's CFG
   * @param declMap maps from old declarations to new declarations in
   * the inlined code
   * @param wl is a work stack - empty on exit
   * @return true if a static variable was removed from the callee and
   * made global
   */
  private boolean removeStaticDecls(Scribble calleeCFG, HashMap<Declaration, Object> declMap, Stack<Chord> wl)
  {
    HashMap<Declaration, Declaration>   staticMap = new HashMap<Declaration, Declaration>(203);
    boolean   removed   = false;
    CallGraph cg        = calleeCFG.getRoutineDecl().getCallGraph();
    Chord     start     = calleeCFG.getBegin();

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();

      c.pushOutCfgEdges(wl);

      Vector<LoadExpr> v = c.getLoadExprList();
      if (v == null)
        continue;

      int l = v.size();
      for (int j = 0; j < l; j++) {
        LoadExpr    le   = v.elementAt(j);
        Declaration decl = le.getDecl();

        if (decl.isGlobal())
          continue;

        if (!decl.isVariableDecl() || decl.isEquivalenceDecl())
          continue;

        if (decl.residency() != Residency.MEMORY)
          continue;

        Declaration newDecl = staticMap.get(decl);
        if (newDecl != null) {
          le.setDecl(newDecl);
          continue;
        }

        newDecl = decl.copy(getNewName(un.genName(), decl.getName()));
        newDecl.setVisibility(Visibility.FILE);
        newDecl.setResidency(Residency.MEMORY);
        newDecl.setReferenced();

        staticMap.put(decl, newDecl);
        declMap.put(decl, newDecl);
        le.setDecl(newDecl);
        cg.addTopLevelDecl(newDecl);
        calleeCFG.removeDeclaration(decl);

        if (trace)
          System.out.println("Added new global " + newDecl.toString());

        removed = true;
      }
    }

    return removed;
  }

  /**
   * This method is similar to setDecls, but rather than copying
   * the non-global declarations, it makes global declarations 
   * consistent with the new CallGraph that they are inside
   * (creating external declarations as appropriate).
   * <p>
   * This method must be used only if the caller and callee
   * RoutineDecl's are not the same.  In that case, we know that any
   * globals found are associated with the wrong node.
   */
  private void setGlobalDecls(LoadExpr le, CallGraph cg, HashMap<String, Declaration> name2Decl)
  {
    Declaration decl = le.getDecl();

    if (!treatGlobal(decl))
       return;

    String name = decl.getName();

    if ((decl.visibility() == Visibility.FILE) && !decl.isConst()) { // We need to make this a global variable
      if (trace)
        System.out.println("Making declaration global: " + name);
        
      decl.setName(getNewName(un.genName(), name));
      decl.setVisibility(Visibility.GLOBAL);
    }

    Declaration newDecl = name2Decl.get(name);
    if ((newDecl != null) && !newDecl.isVariableDecl() && !newDecl.isProcedureDecl())
        newDecl = null;

    if ((newDecl != null) &&
        (newDecl.visibility() == Visibility.FILE)) {

      // We need to rename the File-level variable so it doesn't interfere 
      // with the global.

      if (trace) 
        System.out.print("Renaming file-level global: " + newDecl.getName());

      name2Decl.remove(name);

      newDecl.setName(getNewName(un.genName(), name));

      if (trace)
        System.out.println(" to: " + newDecl.getName());

      name2Decl.put(newDecl.getName(), newDecl); 
      newDecl = null;
    }

    if (newDecl == null) {
      newDecl = decl.copy(decl.getName());
      if (newDecl instanceof ValueDecl)
        ((ValueDecl) newDecl).setValue(null);

      // This is necessary because a cloned var decl has local
      // visibility.

      newDecl.setVisibility(decl.visibility());

      if (newDecl.visibility() == Visibility.GLOBAL)
        newDecl.setVisibility(Visibility.EXTERN);

      RoutineDecl rd = newDecl.returnRoutineDecl();
      if (rd != null)
        cg.recordRoutine(rd); // Add routine.

      cg.addTopLevelDecl(newDecl);

      name2Decl.put(newDecl.getName(), newDecl);        

      if (trace)
        System.out.println("Added external global " + newDecl);
    }

    le.setDecl(newDecl);
  }
    
  /**
   * Return the procedure declaration for the procedure called in the
   * given CallExpr.
   */
  private RoutineDecl getRoutineDecl(CallExpr call)
  {
    Expr ftn = call.getFunction();
    if (!(ftn instanceof LoadDeclAddressExpr))
      return null;

    LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) ftn;
    RoutineDecl         rd   = (RoutineDecl) ldae.getDecl();

    if (rd.getScribbleCFG() != null)
      return rd;

    if (!rd.isGlobal())
      return null;

    // Maybe it's in another call graph if we are doing multi-compilation.

    Iterator<RoutineDecl> er   = suite.allDefRoutines();
    String                name = rd.getName();
    while (er.hasNext()) {
      RoutineDecl n  = er.next();
      String      rn = n.getName();
      if (rn.equals(name) && n.isGlobal()) {
        Scribble cfg = n.getScribbleCFG();
        if (cfg != null)
          return n;
      }
    }

    return rd;
  }

  /**
   * Return true if the AST contains a declaration.
   */
  private boolean lookForLiteral(Node n)
  {
    if (n instanceof Declaration)
      return true;

    if ((n instanceof AddressLiteral) && ((AddressLiteral) n).getDecl() != null)
      return true;

    int l = n.numChildren();
    for (int i = 0; i < l; i++) {
      Node child = n.getChild(i);
      if ((child != null) && lookForLiteral(child))
        return true;
    }

    Declaration assocDecl = n.getDecl(); 
    if ((assocDecl != null) && (assocDecl != n))
      return lookForLiteral(assocDecl);

    return false;
  }

  /**
   * Returns true if the call can be inlined.
   */
  private boolean isCandidate(int ci)
  {
    CallExpr    call   = calls[ci];
    RoutineDecl caller = callers[ci];
    RoutineDecl callee = callees[ci];
    if (callee == null) {
      status[ci] = INDIRECT;
      return false;
    }

    if (callee.cantInline()) {
      status[ci] = RULEDOUT;
      return false;
    }

    Scribble calleeCFG = callee.getScribbleCFG();
    if (calleeCFG == null) {
      callee.setCantInline();
      status[ci] = EXTERN;
      return false;
    }

    if (callee.isRecursive()) {
      callee.setCantInline();
      status[ci] = RECURSIVE;
      return false;
    }

    // We can't copy a literal containing an address at this point, so
    // if there is a declaration inside a literal we have to give up.

    int l = calleeCFG.numDecls();
    for (int i = 0; i < l; i++) {
      Declaration decl = calleeCFG.getDecl(i);
      if (!(decl instanceof ValueDecl))
        continue;
      Node expr = ((ValueDecl) decl).getValue();
      if ((expr != null) && lookForLiteral(expr)) {
        callee.setCantInline();
        status[ci] = ADDRESSLIT;
        return false;
      }
    }

    // If the call graphs differ, and cross-module inlining is
    // disabled, return false.

    CallGraph callerCallGraph = caller.getCallGraph();
    CallGraph calleeCallGraph = callee.getCallGraph();
    if (!allowCrossModuleInlining && (callerCallGraph != calleeCallGraph)) {
      callee.setCantInline();
      status[ci] = EXTERN;
      return false;
    }
    
    if (callerCallGraph != calleeCallGraph) {
    // Don't inline routines that reference static functions in a
    // different module from the caller.

      int ll = callee.numCallees();
      for (int i = 0; i < ll; i++) {
        RoutineDecl rd = callee.getCallee(i);
        if ((rd.getCallGraph() != callerCallGraph) &&
            (rd.visibility() == Visibility.FILE)) {
          status[ci] = STATICS;
          return false;
        }
      }

      // Don't inline routines that reference static variables in a
      // different module from the caller.

      if (hasNonConstStaticReference(callee)) {
        status[ci] = STATICS;
        return false;
      }
    }

    ProcedureType callType = callee.getSignature();
    int           numArgs  = call.numArguments();
    if (numArgs != callType.numFormals()) {
      status[ci] = ARGUMENTS;  // If the actual args & formal args don't agree, we can't inline.
      return false;
    }

    // Check that the arguments match the formal parameters.

    for (int i = 0; i < numArgs; i++) {
      Expr       arg = call.getArgument(i);
      FormalDecl fd  = callType.getFormal(i);

      if (fd instanceof UnknownFormals) {
        callee.setCantInline();
        status[i] = ARGUMENTS; // Too bad for printf() :-)
        return false;
      }

      // The code might work even if the a declaration is not passed
      // by value or reference ...

      if ((fd.getMode() != ParameterMode.VALUE) &&
          (fd.getMode() != ParameterMode.REFERENCE)) {
        callee.setCantInline();
        status[i] = ARGUMENTS; // Too bad for printf() :-)
        return false;
      }

      Type ot = fd.getCoreType();
      Type nt = arg.getCoreType();
      if (ot.equivalent(nt))
        continue;

      // The source program has a type mismatch.  For example, passing
      // in a char * to a routine that requires a different pointer
      // type.

      if (ot.isPointerType() && nt.isPointerType())
        continue;

      if (ot.isIntegerType() && nt.isIntegerType())
        continue;
 
      if (ot.isRealType() && nt.isRealType())
        continue;
 
      status[ci] = ARGUMENTS;
      return false;
    }

    if (callee.inlineSpecified() || ignoreComplexityHeuristic) {
      status[ci] = CANDIDATE;
      return true;
    }

    // If the caller is simple, we can inline complex callees.

    if (caller.getScribbleCFG().isSimpleFunction() &&
        (simpleFtnLimit > caller.getCost())) {
      status[ci] = CANDIDATE;
      return true;
    }

    // We only want to inline simple functions.  If the function
    // contains more than one loop or the loop is complex, it's not
    // worth inlining.

    if (calleeCFG.isSimpleFunction()) {
      status[ci] = CANDIDATE;
      return true;
    }

    callee.setCantInline();
    status[ci] = COMPLEX;
    return false;
  }

  /**
   * Return true if this routine references a non-constant static
   * variable.
   */
  private boolean hasNonConstStaticReference(RoutineDecl rd)
  {
    if (staticRefMap == null)
      staticRefMap = new HashMap<RoutineDecl, Boolean>(33);
    
    Boolean res = staticRefMap.get(rd);
    if (res != null)
      return res.booleanValue();

    Scribble cfg = rd.getScribbleCFG();
    if (cfg == null)
      return false;

    Stack<Chord>   wl  = WorkArea.<Chord>getStack("hasNonConstStaticReference");
    HashSet<Chord> dne = WorkArea.<Chord>getSet("hasNonConstStaticReference");
    boolean flg = false;
    Chord   beg = cfg.getBegin();

    wl.push(beg);
    dne.add(beg);

    while (!wl.empty()) {
      Chord c = wl.pop();

      c.pushOutCfgEdges(wl, dne);

      Vector<Declaration> decls = c.getDeclList();
      if (decls == null)
        continue;

      int l = decls.size();
      for (int i = 0; i < l; i++) {
        Declaration d = decls.get(i);
        if (d.isConst())
          continue;

        if ((d.visibility() == Visibility.FILE) || d.isEquivalenceDecl()) {
          flg = true;
          break;
        }
        if ((d.visibility() == Visibility.LOCAL) &&
            (d.residency() == Residency.MEMORY)) {
          flg = true;
          break;
        }
      }
    }

    WorkArea.<Chord>returnStack(wl);
    WorkArea.<Chord>returnSet(dne);

    staticRefMap.put(rd, flg ? Boolean.TRUE : Boolean.FALSE);

    return flg;
  }

  /**
   * Generate a reasonably legible unique name by combining the name
   * of the variable this variable is based on with the unique name
   * generated by our UniqueName instance.
   */
  private String getNewName(String uniqueName, String oldName)
  {
    if (oldName.startsWith(PREFIX)) {
      String str = oldName.substring(PREFIX.length());
      int    k   = 0;

      while ((k < str.length()) && (str.charAt(k) == '_'))
        k++;

      if (k > 0)
        str = str.substring(k);

      return uniqueName + "_" + str;
    }

    return uniqueName + "_" + oldName;
  }

  /**
   * This method adds all the useable candidates calls to the list of
   * calls.  It scans the CFG starting at the specified
   * (<code>cc</code>) node and terminates when it reaches the
   * specified (<code>term</code>) node, if not null, or when the
   * entire CFG has been spanned.  A useable call is one that will be
   * replaced by an inlined function.
   * @return the size of this CFG in instructions
   */
  private int findCalls(Chord       cc,
                        RoutineDecl caller,
                        Chord       term) 
  {
    // Traverses the CFG that extends from cc, adding call candidates
    // where it finds them.

    Stack<Chord> wl = WorkArea.<Chord>getStack("findCalls");

    Chord.nextVisit();
    wl.push(cc);
    cc.setVisited();

    if (term != null)
      term.setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();

      c.pushOutCfgEdges(wl);

      ice.estimate(c);

      CallExpr call = c.getCall(true);
      if (call == null)
        continue;

      RoutineDecl callee = getRoutineDecl(call);

      if (callCount >= calls.length) {
        int n = calls.length * 2;
        RoutineDecl[] ncr = new RoutineDecl[n];
        System.arraycopy(callers, 0, ncr, 0, callCount);
        callers = ncr;
        RoutineDecl[] nce = new RoutineDecl[n];
        System.arraycopy(callees, 0, nce, 0, callCount);
        callees = nce;
        int[] nl = new int[n];
        System.arraycopy(callLoopDepth, 0, nl, 0, callCount);
        callLoopDepth = nl;
        CallExpr[] nc = new CallExpr[n];
        System.arraycopy(calls, 0, nc, 0, callCount);
        calls = nc;
        byte[] ns = new byte[n];
        System.arraycopy(status, 0, ns, 0, callCount);
        status = ns;
        int[] nss = new int[n];
        System.arraycopy(sorted, 0, nss, 0, callCount);
        sorted = nss;
      }

      calls[callCount]         = call;
      callers[callCount]       = caller;
      callees[callCount]       = callee;
      callLoopDepth[callCount] = call.getLoopHeader().getNestedLevel();
      status[callCount]        = CANDIDATE;
      sorted[callCount]        = 0;
      callCount++;
    }

    WorkArea.<Chord>returnStack(wl);

    return ice.getEstimateAndReset();
  }

  /**
   * Sort the candidate function calls in order of highest priority.
   * Return the number of calls that have priority greater than  0.
   */
  private int sortCandidates() 
  {
    boolean  flag;
    int      jumpSize = numCalls;
    double[] sort     = new double[numCalls];

    for (int ci = 0; ci < numCalls; ci++)
      sort[ci] = getPriority(sorted[ci]);

    if (numCalls > 1) {
      do {
        flag = false;
        jumpSize = (10 * jumpSize + 3) / 13;
        int ul = numCalls - jumpSize;
        for (int i = 0; i < ul; i++) {
          int    k     = i + jumpSize;
          double si    = sort[i];
          double sk    = sort[k];

          if (si < sk) {
            sort[i] = sk;
            sort[k] = si;
            int t = sorted[i];
            sorted[i] = sorted[k];
            sorted[k] = t;
            flag = true;
          }
        }
      } while (flag || (jumpSize > 1));
    }

//     for (int i = 0; i < numCalls; i++) {
//       int ci = sorted[i];
//       System.out.print(callers[ci].getRoutineName());
//       System.out.print("(");
//       System.out.print(callers[ci].getCost());
//       System.out.print(") calls ");
//       System.out.print(callees[ci].getRoutineName());
//       System.out.print("(");
//       System.out.print(callees[ci].getCost());
//       System.out.print(") at ");
//       System.out.print(calls[ci].getChord().getSourceLineNumber());
//       System.out.print(" : ");
//       System.out.println(sort[ci]);
//     }

    for (int ci = 0; ci < numCalls; ci++)
      if (sort[ci] <= 0.0)
        return ci;

    return numCalls;
  }

  public void displayStatus(PrintWriter cout)
  {
    DecimalFormat form = new DecimalFormat("0.000E00 ");
    int callermx = 15;
    int calleemx = 15;
    for (int i = 0; i < callCount; i++) {
      int ln = callers[i].getName().length();
      if (ln > callermx)
        callermx = ln;

      RoutineDecl callee = callees[i];
      if (callee == null)
        continue;

      int lnn = callee.getName().length();
      if (lnn > calleemx)
        calleemx = lnn;
    }

//     cout.print("  ");
//     cout.print("caller");
//     printBlanks(callermx - 6, cout);
//     cout.print(" ");
//     cout.print("callee");
//     printBlanks(calleemx - 6, cout);
//     cout.print(" ");
//     cout.println("depth status");

    for (int i = 0; i < callCount; i++) {
      cout.print("  ");

      String caller = callers[i].getName();
      int    ln = caller.length();
      cout.print(caller);
      printBlanks(callermx - ln, cout);
      cout.print(" ");

      int lnn = 0;
      if (callees[i] != null) {
        String callee = callees[i].getName();
        cout.print(callee);
        lnn = callee.length();
      } else {
        cout.print(calls[i].getFunction().toString());
        lnn = calleemx;
      }
      printBlanks(calleemx - lnn, cout);
      cout.print(" ");

      String callDepth = Integer.toString(callLoopDepth[i]);
      printBlanks(5 - callDepth.length(), cout);
      cout.print(callDepth);
      cout.print(" ");

      cout.print(statusText[status[i]]);
      if (status[i] == FORM) {
        cout.print(" ");
        cout.print(calls[i]);
      }
      cout.println("");
    }
  }

  private void printBlanks(int n, PrintWriter cout)
  {
    while (n > 20) {
      cout.print("                    ");
      n -= 20;
    }
    if (n > 0)
      cout.print("                    ".substring(0, n));
  }

  /**
   * Create a new temporary variable for use by the optimized code.
   * The variable definition is added to the set of variables for this
   * CFG.
   */
  protected VariableDecl genTemp(Type t, Scribble scribble)
  {
    VariableDecl vd = new VariableDecl(un.genName(), t);
    vd.setTemporary();
    scribble.addDeclaration(vd);
    return vd;
  }
}
