package scale.score.trans;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.dependence.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.IntLiteral;
 
/**
 * The base class for all loop transformation optimizations.
 * <p>
 * $Id: LoopTrans.java,v 1.68 2007-10-04 19:58:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <P>
 */

public abstract class LoopTrans extends Optimization
{
  private static int permuteLoopCount = 0; // A count of the times loops were permuted.

  private static final String[]  stats = {"permutedLoops"};

  static
  {
    Statistics.register("scale.score.trans.LoopTrans", stats);
  }

  /**
   * Return the current number of implicit loops found.
   */
  public static int permutedLoops()
  {
    return permuteLoopCount;
  }

  private static final int outer = 0;
  private static final int inner = 1;

  private static ProcedureDecl minFuncDecl    = null;
  private static ProcedureDecl maxFuncDecl    = null;
  private static ProcedureDecl floorFuncDecl  = null;
  private static ProcedureDecl boundFuncDecl  = null;
  private static ProcedureDecl lboundFuncDecl = null;
  private static ProcedureDecl uboundFuncDecl = null;

  protected static IntegerType intType;
  protected static FloatType   floatType;
  protected static IntegerType unsignedType;
  protected static PointerType intPointer;
  protected static PointerType unsignedPointer;
  protected static PointerType voidp;

  private ExprChord  outerInit = null;
  private Expr       outerLo1  = null; // Outer arg 1 of lower bound max op
  private Expr       outerLo2  = null; // Outer arg 2 of lower bound max op
  private Expr       outerHi1  = null; // Outer arg 1 of upper bound min op
  private Expr       outerHi2  = null; // Outer arg 2 of upper bound min op

  private ExprChord firstBound = null;
  private ExprChord lastBound  = null;

  /**
   * @param scribble is the CFG containing the loops
   */
  public LoopTrans(Scribble scribble, String tempPrefix)
  {
    super(scribble, tempPrefix);

    if (intType == null) {
      intType         = Machine.currentMachine.getSignedIntType();
      floatType       = Machine.currentMachine.getFloatType();
      unsignedType    = Machine.currentMachine.getUnsignedIntType();
      intPointer      = PointerType.create(intType);
      unsignedPointer = PointerType.create(unsignedType);
      voidp           = Machine.currentMachine.getVoidStarType();
    }
  }

  /**
   * Initialize everything in preparation for a loop transform.
   * @param prefix is used in generating temporary variable names
   * @return true if all dependencies were obtainable
   */
  protected boolean initializeTransform(String prefix)
  {
    this.un = new UniqueName(prefix);
    
    return scribble.getLoopTree().isDDComplete();
  }

  protected ExprChord findIndexInit(Chord c)
  {
    if (c == null)
      return null;

    Chord[] inEdges = c.getInCfgEdgeArray ();

    for (int i = 0; i < inEdges.length; i++) {
      Chord p = inEdges[i];

      if (p.isLoopPreHeader())
        return findIndexInit(p);

      if (p.isExprChord())
        return (ExprChord) p;
    }

    return null;
  }

  protected ExprChord findIndexInc(Chord c)
  {
    if (c == null)
      return null;

    Chord[] inEdges = c.getInCfgEdgeArray ();

    for (int i = 0; i < inEdges.length; i++) {
      Chord p = inEdges[i];
      if (p.isExprChord())
        return (ExprChord) p;
    }

    return null;
  }

  protected Cost tripProduct(Vector<LoopHeaderChord> loopNest, LoopHeaderChord thisLoop)
  {
    Cost tripProd = new Cost(1.0, 0);
    Cost n        = new Cost(1.0, 1);

    for (int i = 0; i < loopNest.size(); i++) {
      LoopHeaderChord loop = loopNest.elementAt(i);
      if (loop == thisLoop)
        continue;

      Cost lc = loop.getTripCount();
      if (lc == null)
        return null;
      if (lc.order() > 0)
        lc = n.copy();

      tripProd.multiply(lc);
    }

    return tripProd;
  }

  /**
   * Convert two loops to use tiling.
   * @param oHeader is the outer loop
   * @param iHeader is the inner loop
   * @param tileSize is the size of the tile
   * @param tileOffset is the offset for each tile
   */
  protected void performLoopTile(LoopHeaderChord oHeader,
                                 LoopHeaderChord iHeader,
                                 int             tileSize,
                                 int             tileOffset)
  {
    if (!initializeTransform("_lt"))
      return;

    LoopHeaderChord l1outer = tileLoop(oHeader, tileSize, tileOffset, outer);
    LoopHeaderChord l2outer = tileLoop(iHeader, tileSize, tileOffset, inner);

    performLoopInterchange(oHeader, l2outer);

    scribble.recomputeDominators();
  }

  private LoopHeaderChord tileLoop(LoopHeaderChord loop,
                                   int             tileSize,
                                   int             tileOffset,
                                   int             oi)
  {
    if (minFuncDecl == null)
      createSpecialFuncs();

    LoopInitChord   indexInit    = loop.getLoopInit();
    Chord           postInit     = indexInit.getNextChord();
    LoopTailChord   tail         = loop.getLoopTail();
    ExprChord       indexInc     = findIndexInc(tail);
    IfThenElseChord dec          = loop.getLoopTest();
    Chord           decFalseEdge = dec.getFalseCfgEdge();
    Chord[]         preInits     = indexInit.getInCfgEdgeArray();
    Expr            hi           = loop.getUpperBound(); // Get the loop upper bound expression.
    Expr            lo           = loop.getLowerBound(); // Get the loop lower bound expression.
    Type            t            = lo.getType();
    VariableDecl    it0Decl      = genTemp(t);
    VariableDecl    it1Decl      = genTemp(t);
    VariableDecl    it2Decl      = genTemp(t);
    VariableDecl    itlbDecl     = genTemp(t);
    VariableDecl    itubDecl     = genTemp(t);
    ExprChord       itInitFirstc = null;
    ExprChord       itubLastc    = null;

    if (oi == outer) { // Create initialization node for the(new) outer loop induction variable. 
      computeBound(ldVal(lo), tileSize, tileOffset, itlbDecl); // it = floor((lo-to)/ts) * ts + to;
      itInitFirstc = firstBound;
      outerInit = createStore(ldVal(itlbDecl), it0Decl);
      lastBound.setTarget(outerInit);      
      computeBound(ldVal(hi), tileSize, tileOffset, itubDecl); // it = floor((hi-to)/ts) * ts + to;
      itubLastc = lastBound;
      outerInit.setTarget(firstBound);
    } else if (oi == inner) {
      // 1. 
      // FOR it = max (floor((outerlo-to)/ts) * ts + to,
      //               floor((outerit-to)/ts) * ts + to) TO
      //          min(floor((outerhi-to)/ts) * ts + to,
      //               floor(((outerit+ts-1)-to)/ts) * ts + to) DO
      //
      //

      VariableDecl cb1Decl = genTemp(t);
      VariableDecl cb2Decl = genTemp(t);
      VariableDecl cb3Decl = genTemp(t);
      VariableDecl cb4Decl = genTemp(t);
      Vector<Expr> args1   = new Vector<Expr>(2);
      Vector<Expr> args2   = new Vector<Expr>(2);

      args1.addElement(ldVal(cb1Decl));
      args1.addElement(ldVal(cb2Decl));

      args2.addElement(ldVal(cb3Decl));
      args2.addElement(ldVal(cb4Decl));

      computeBound(outerLo1, tileSize, tileOffset, cb1Decl);

      itInitFirstc = firstBound;

      ExprChord cb1Lastc  = lastBound;

      computeBound(outerLo2, tileSize, tileOffset, cb2Decl);

      ExprChord cb2Firstc = firstBound;
      ExprChord cb2Lastc  = lastBound;

      cb1Lastc.setTarget(cb2Firstc);

      // Compute the max.
      
      ExprChord itMaxDef = createFuncCall(maxFuncDecl, intType, args1, itlbDecl);
      outerInit = createStore(ldVal(itlbDecl), it0Decl);

      cb2Lastc.setTarget(itMaxDef);
      itMaxDef.setTarget(outerInit);

      computeBound(outerHi1, tileSize, tileOffset, cb3Decl);

      ExprChord cb3Firstc = firstBound;
      ExprChord cb3Lastc  = lastBound;

      computeBound(outerHi2, tileSize, tileOffset, cb4Decl);

      // Compute the min.
      
      itubLastc = createFuncCall(minFuncDecl, intType, args2, itubDecl);
      
      outerInit.setTarget(cb3Firstc);
      cb3Lastc.setTarget(firstBound);
      lastBound.setTarget(itubLastc);
    }

    // Adjust the outgoing edge of the node(s) before the original
    // initialization expression.

    for (int i = 0; i < preInits.length; i++) {
      preInits[i].replaceOutCfgEdge(indexInit, itInitFirstc);
      indexInit.deleteInCfgEdge(preInits[i]);
      itInitFirstc.addInCfgEdge(preInits[i]);
    }

    // Create Phi expression for the outside loop induction variable:

    Vector<Expr> itTemps = new Vector<Expr>(2);

    itTemps.addElement(new LoadDeclValueExpr(it0Decl));
    itTemps.addElement(new LoadDeclValueExpr(it2Decl));

    PhiExpr             itPhiExpr  = new PhiExpr(t, itTemps);
    LoadDeclAddressExpr it1Addr    = new LoadDeclAddressExpr(it1Decl);
    PhiExprChord        itPhiChord = new PhiExprChord(it1Addr, itPhiExpr);

    // Create the initialization expression for the inner induction variable.
    // I = max (lo, it);

    // Create the procedure that finds the max of two integer variables.

    Vector<Expr> args = new Vector<Expr>(2);
    args.addElement(ldVal(lo));
    args.addElement(ldVal(it1Decl));
    
    if (oi == outer) {
      outerLo1 = ldVal(lo);
      outerLo2 = ldVal(it1Decl);
    }

    VariableDecl innerMaxDecl = genTemp(intType);
    ExprChord    innerMaxDef  = createFuncCall(maxFuncDecl, intType, args, innerMaxDecl);

    innerMaxDef.setTarget(indexInit);

    // Create new LoopPreHeaderChord and LoopHeaderChord.

    LoopHeaderChord    lheader  = loop.getParent();
    LoopHeaderChord    itHeader = new LoopHeaderChord(scribble, lheader, itPhiChord);
    LoopPreHeaderChord lphc     = new LoopPreHeaderChord(itHeader);

    loop.setParent(itHeader);

    itubLastc.setTarget(lphc); // Adjust CFG edges.

    // Create the predicate and decision chord for new outer loop.

    LessEqualExpr   le   = new LessEqualExpr(t, ldVal(it1Decl), ldVal(itubDecl));
    IfThenElseChord itec = new IfThenElseChord(le, innerMaxDef, decFalseEdge);

    itPhiChord.setTarget(itec); // Adjust CFG edges.

    // Create increment expression for the new variable it.
    // it2 = it1 + stepSize;
    // Use stripSize as the loop step.

    AdditionExpr  itAdd     = intAdd(ldVal(it1Decl), tileSize);
    ExprChord     itInc     = createStore(itAdd, it2Decl);
    LoopExitChord innerExit = new LoopExitChord(itHeader, itInc); // Create new LoopExitChord.

    // Adjust the false edge of the inner (original) loop decision chord.

    dec.setFalseEdge(innerExit);

    // Create new LoopTailChord and adjust edges.

    LoopTailChord itTail = new LoopTailChord(itHeader);

    itInc.setTarget(itTail);

    // Create the new upper bound expressions for inner loop and replace the original one.
    // Create the function call to compute the upper bound:
    // minVar = min(hi, it + ts - 1);
    // Create the expression(s) to compute minArg2 = (it + tileSize - 1);
    // Create literal expression for the loop step expression.

    LiteralExpr     itStep       = new LiteralExpr(LiteralMap.put(tileSize, t));
    AdditionExpr    ae           = new AdditionExpr(intType, ldVal(it1Decl), itStep);
    VariableDecl    tmp1Decl     = genTemp(intType);
    ExprChord       tmp1Chord    = createStore(ae, tmp1Decl);
    Expr            se           = intSub(ldVal(tmp1Decl), 1);
    VariableDecl    minArg2Decl  = genTemp(intType);
    ExprChord       minArg2Chord = createStore(se, minArg2Decl);

    tmp1Chord.setTarget(minArg2Chord);
    
    // Create min function call.

    args = new Vector<Expr>(2);
    args.addElement(ldVal(hi));
    args.addElement(ldVal(minArg2Decl));

    if (oi == outer) {
      outerHi1 = ldVal(hi);
      outerHi2 = ldVal(minArg2Decl);
    }
    
    VariableDecl minVarDecl = genTemp(intType);
    ExprChord    minVarDef  = createFuncCall(minFuncDecl, intType, args, minVarDecl);

    minArg2Chord.setTarget(minVarDef);
    minVarDef.setTarget(postInit);

    indexInit.setTarget(tmp1Chord);

    // Create the new predicate and decision chord for inner loop.

    BinaryExpr      pred      = (BinaryExpr) dec.getPredicateExpr();
    LoadExpr        predle    = (LoadExpr) pred.getLeftArg();
    VariableDecl    innerDecl = (VariableDecl) predle.getDecl();
    LessEqualExpr   inle      = new LessEqualExpr(t, ldVal(innerDecl), ldVal(minVarDecl));
    Chord           te        = dec.getTrueCfgEdge();
    Chord           fe        = dec.getFalseCfgEdge();
    IfThenElseChord innerdec  = new IfThenElseChord(inle, te, fe);

    // Adjust incoming CFG edges.

    Chord[] inEdges = dec.getInCfgEdgeArray ();
    for (int i = 0; i < inEdges.length; i++) {
      Chord c = inEdges[i];
      c.replaceOutCfgEdge(dec, innerdec);
      innerdec.addInCfgEdge(c);
      dec.deleteInCfgEdge(c);
    }

    // Create new LoopHeaderChord object for inner loop.

    lheader.recomputeLoop();
    itHeader.recomputeLoop();
    loop.recomputeLoop();

    return itHeader;
  }

  /**
   * Generate CFG nodes to store an expression into a variable.
   */
  private ExprChord createStore(Expr e, VariableDecl tdecl)
  {
    LoadDeclAddressExpr taddr  = new LoadDeclAddressExpr(tdecl);
    return new ExprChord(taddr, e);
  }

  private AdditionExpr intAdd(Expr lde, int i)
  {
    LiteralExpr  lite = new LiteralExpr(LiteralMap.put(i, intType));
    return new AdditionExpr(intType, lde, lite);
  }

  private Expr intSub(Expr lde, int i)
  {
    LiteralExpr lit = new LiteralExpr(LiteralMap.put(i, intType));
    return SubtractionExpr.create(intType, lde, lit);
  }

  private MultiplicationExpr intMul(Expr lde, int i)
  {
    LiteralExpr        lit = new LiteralExpr(LiteralMap.put(i, intType));
    return new MultiplicationExpr(intType, lde, lit);
  }

  private DivisionExpr intDiv(Expr lde, int i)
  {
    LiteralExpr  lit = new LiteralExpr(LiteralMap.put((double) i, floatType));
    return new DivisionExpr(floatType, lde, lit);
  }

  private ExprChord createFuncCall(ProcedureDecl pdecl,
                                   Type          ptype,
                                   Vector<Expr>  args,
                                   VariableDecl  vd)
  {
    Expr             funcAddr = new LoadDeclAddressExpr(pdecl);
    CallFunctionExpr func     = new CallFunctionExpr(ptype, funcAddr, args);
    return createStore(func, vd);
  }

  private void createSpecialFuncs()
  {
    Vector<FormalDecl> formals0 = new Vector<FormalDecl>(2);
    Vector<FormalDecl> formals1 = new Vector<FormalDecl>(2);
    Vector<FormalDecl> formals2 = new Vector<FormalDecl>(1);
    Vector<FormalDecl> formals3 = new Vector<FormalDecl>(3);
    Vector<FormalDecl> formals4 = new Vector<FormalDecl>(4);
    Vector<FormalDecl> formals5 = new Vector<FormalDecl>(4);

    FormalDecl argi1    = new FormalDecl("argi1", intType);
    FormalDecl argi2    = new FormalDecl("argi2", intType);
    FormalDecl argf1    = new FormalDecl("argf1", floatType);
    FormalDecl hilo     = new FormalDecl("hilo", intType);
    FormalDecl hi       = new FormalDecl("hi", intType);
    FormalDecl lo       = new FormalDecl("lo", intType);
    FormalDecl ts       = new FormalDecl("ts", intType);
    FormalDecl to       = new FormalDecl("to", intType);

    formals0.addElement(argi1);
    formals0.addElement(argi2);

    formals1.addElement(argi1);
    formals1.addElement(argi2);

    formals2.addElement(argf1);

    formals3.addElement(hilo);
    formals3.addElement(ts);
    formals3.addElement(to);

    formals4.addElement(hi);
    formals4.addElement(lo);
    formals4.addElement(ts);
    formals4.addElement(to);

    formals5.addElement(hi);
    formals5.addElement(lo);
    formals5.addElement(ts);
    formals5.addElement(to);

    ProcedureType pt0 = ProcedureType.create(intType, formals0, null);
    ProcedureType pt1 = ProcedureType.create(intType, formals1, null);
    ProcedureType pt2 = ProcedureType.create(intType, formals2, null);
    ProcedureType pt3 = ProcedureType.create(intType, formals3, null);
    ProcedureType pt4 = ProcedureType.create(intType, formals4, null);
    ProcedureType pt5 = ProcedureType.create(intType, formals5, null);

    maxFuncDecl    = new ProcedureDecl("__max",    pt0);
    minFuncDecl    = new ProcedureDecl("__min",    pt1);
    floorFuncDecl  = new ProcedureDecl("__floor",  pt2);
    boundFuncDecl  = new ProcedureDecl("__bound",  pt3);
    lboundFuncDecl = new ProcedureDecl("__ubound", pt4);
    uboundFuncDecl = new ProcedureDecl("__ubound", pt5);

    maxFuncDecl.setVisibility(Visibility.EXTERN);
    maxFuncDecl.setVisibility(Visibility.EXTERN);
    floorFuncDecl.setVisibility(Visibility.EXTERN);
    boundFuncDecl.setVisibility(Visibility.EXTERN);
    lboundFuncDecl.setVisibility(Visibility.EXTERN);
    uboundFuncDecl.setVisibility(Visibility.EXTERN);
  }

  private void computeBound(Expr hilo, int ts, int to, VariableDecl d)
  {
    Type t = hilo.getType();

    // Compute it = floor((hilo-to)/ts) * ts + to;

    Expr            se   = intSub(ldVal(hilo), to); // Compute(hilo-to).
    VariableDecl    t1d  = genTemp(t);
    ExprChord       t1c  = createStore(se, t1d);
    DivisionExpr    de   = intDiv(ldVal(t1d), ts); // Compute(hilo-to)/ts.
    VariableDecl    t2d  = genTemp(t);
    ExprChord       t2c  = createStore(de, t2d);
    Vector<Expr>    args = new Vector<Expr>(1);

    args.addElement(ldVal(t2d));

    VariableDecl       t3d = genTemp(intType);  // Compute floor((hilo-to)/ts).
    ExprChord          t3c = createFuncCall(floorFuncDecl, intType, args, t3d);
    MultiplicationExpr me  = intMul(ldVal(t3d), ts); // Compute floor((hilo-to)/ts) * ts.
    VariableDecl       t4d = genTemp(intType);
    ExprChord          t4c = createStore(me, t4d);
    AdditionExpr       ae  = intAdd(ldVal(t4d), to); // Compute floor((hilo-to)/ts) * ts + to.
    ExprChord          t5c = createStore(ae, d);     // it = floor((hilo-to)/ts) * ts + to;

    t1c.setTarget(t2c);
    t2c.setTarget(t3c);
    t3c.setTarget(t4c);
    t4c.setTarget(t5c);

    firstBound = t1c;
    lastBound = t5c;
  }

  private Expr ldVal(Expr e)
  {
    if (e.isLiteralExpr())
      return e.copy();

    return ldVal(((LoadExpr) e).getDecl());
  }

  private Expr ldVal(Declaration d)
  {
    return new LoadDeclValueExpr(d);
  }

  // This code works only for a positive step size, that is, when
  // the value of the loop index variable is increasing between
  // iterations.

  // We need to adjust def-use links!

  // We need to re-compute loop tree, invariants and index variables!
  // We can do this incrementally. 

  // Add a new loop in the parent loop before the inner loop.
  // Define the new variable as an index variable for the loop.
  // Same with loop lower and upper bounds.

  // The outer loop index is invariant in the inner loop. The new upper
  // bound of the inner loop is also invariant within that loop.

  // Assumes that the Scribble graph is in SSA form

  /**
   * Perform the strip mining on the loop.
   * @param loop is the loop to be strip mined
   * @param stripSize specifies the size of the strip
   */
  protected void performLoopStripMining(LoopHeaderChord loop, int stripSize)
  {
    if (!initializeTransform("_lsm"))
      return;

    LoopHeaderChord top          = loop.getParent();
    ExprChord       innerInit    = findIndexInit(loop);
    Chord           postInit     = innerInit.getNextChord();
    LoopTailChord   tail         = loop.getLoopTail();
    ExprChord       indexInc     = findIndexInc(tail);
    IfThenElseChord dec          = loop.getLoopTest();
    Chord           decFalseEdge = dec.getFalseCfgEdge();
    Chord           preInit      = innerInit.getInCfgEdge();
    Expr            ub           = loop.getUpperBound(); // Get the loop upper bound expression.
    Expr            lb           = loop.getLowerBound(); // Get the loop lower bound expression.
    Type            t            = lb.getType(); // Get the type from the lower bound.

    // Create new variable declarations.

    VariableDecl sm0Decl     = genTemp(t);
    VariableDecl sm1Decl     = genTemp(t);
    VariableDecl sm2Decl     = genTemp(t);
    VariableDecl tmp1Decl    = genTemp(t);
    VariableDecl minArg2Decl = genTemp(t);
    VariableDecl minVarDecl  = genTemp(t);

    // Create initialization node for the (new) outer loop induction
    // variable. The original init. expression is used.
    // sm = initExpr;

    LoadDeclAddressExpr sm0Addr  = new LoadDeclAddressExpr(sm0Decl);
    Expr                ie       = loop.getInductionVarInitExpr();
    Expr                initDecl = null;

    if ((ie != null) && ie.isLiteralExpr())
      initDecl = ie;
    else
      initDecl = new LoadDeclValueExpr(((LoadExpr) ie).getDecl());

    // sm0 = initExpr;

    ExprChord outerInit = new ExprChord(sm0Addr, initDecl);

    // Adjust the outgoing edge of the node before the original
    // initialization expression.

    preInit.replaceOutCfgEdge(innerInit, outerInit);
    innerInit.deleteInCfgEdge(preInit);
    outerInit.addInCfgEdge(preInit);

    // Create Phi expression for the outside loop induction variable:

    Vector<Expr>      smTemps = new Vector<Expr>();
    LoadDeclValueExpr sm0Load = new LoadDeclValueExpr(sm0Decl);
    LoadDeclValueExpr sm2Load = new LoadDeclValueExpr(sm2Decl);

    smTemps.addElement(sm0Load);
    smTemps.addElement(sm2Load);

    PhiExpr             smPhiExpr  = new PhiExpr(t, smTemps);
    LoadDeclAddressExpr sm1Addr    = new LoadDeclAddressExpr(sm1Decl);
    PhiExprChord        smPhiChord = new PhiExprChord(sm1Addr, smPhiExpr);
    LoopHeaderChord     smHeader   = new LoopHeaderChord(scribble, top, smPhiChord);
    LoopPreHeaderChord  lphc       = new LoopPreHeaderChord(smHeader);

    loop.setParent(smHeader);
    outerInit.setTarget(lphc); // Adjust CFG edges.

    // Create the predicate and decision chord for this loop.
   
    LoadDeclValueExpr sm1Load1 = new LoadDeclValueExpr(sm1Decl); // Create load expression.
    LessEqualExpr     le       = new LessEqualExpr(t, sm1Load1, ub);
    IfThenElseChord   itec     = new IfThenElseChord(le, innerInit, decFalseEdge);

    smPhiChord.setTarget(itec); // Adjust CFG edges.

    // Create increment expression for the new variable sm:
    // sm2 = sm1 + stepSize;
    // Use stripSize as the loop step.

    // Create literal expression for the loop step expression.

    LiteralExpr         smStep    = new LiteralExpr(LiteralMap.put(stripSize, t));
    LoadDeclValueExpr   sm1Load2  = new LoadDeclValueExpr(sm1Decl); // Create load expression.
    AdditionExpr        smAdd     = new AdditionExpr(t, sm1Load2, smStep); // Create addition expression for loop increment.
    LoadDeclAddressExpr sm2Addr   = new LoadDeclAddressExpr(sm2Decl);
    ExprChord           smInc     = new ExprChord(sm2Addr, smAdd);
    LoopExitChord       innerExit = new LoopExitChord(smHeader, smInc);
    LoopTailChord       smTail    = new LoopTailChord(smHeader);

    // Adjust the false edge of the inner(original) loop decision chord.

    dec.setFalseEdge(innerExit);
    smInc.setTarget(smTail);

    // Create the new upper bound expressions for inner loop and replace
    // the original one

    // Create the function call to compute the upper bound:
    // minVar = min(upperBound, sm + stripSize - 1);
    // Create the expression(s) to compute minArg2 = (sm + stripSize - 1);
    // Create addition expression for tmp1 = (sm + stripSize).

    Expr         la      = new LoadDeclValueExpr(sm1Decl);
    Expr         ra      = new LiteralExpr(LiteralMap.put(stripSize, t));
    AdditionExpr tmp1Add = new AdditionExpr(t, la, ra);

    // Create temporary and load its address to store result into.

    LoadDeclAddressExpr tmp1Addr  = new LoadDeclAddressExpr(tmp1Decl);
    ExprChord           tmp1Chord = new ExprChord(tmp1Addr, tmp1Add);
    
    // Create addition expression for minArg2 = (tmp1 - 1).

    Expr      la2          = new LoadDeclValueExpr(tmp1Decl);
    Expr      ra2          = new LiteralExpr(LiteralMap.put(1, t));
    Expr      subExpr      = new SubtractionExpr(t, la2, ra2);
    Expr      minArg2Addr  = new LoadDeclAddressExpr(minArg2Decl);
    ExprChord minArg2Chord = new ExprChord(minArg2Addr, subExpr);

    tmp1Chord.setTarget(minArg2Chord);
    
    // Create the procedure that finds interface methods:
    
    IntegerType        intType  = SignedIntegerType.create(32);
    Vector<FormalDecl> iformals = new Vector<FormalDecl>(3);

    iformals.addElement(new FormalDecl("arg1", intType));
    iformals.addElement(new FormalDecl("arg2", intType));
    
    ProcedureType ipt         = ProcedureType.create(intType, iformals, null);
    ProcedureDecl minFuncDecl = new ProcedureDecl("__min", ipt);

    minFuncDecl.setVisibility(Visibility.EXTERN);
    
    Vector<Expr> args = new Vector<Expr>(2);

    if ((ub != null) && ub.isLiteralExpr())
      args.addElement(ub);
    else
      args.addElement(new LoadDeclValueExpr(((LoadExpr) ub).getDecl()));

    args.addElement(new LoadDeclValueExpr(minArg2Decl));
    
    Expr                minFuncAddr = new LoadDeclAddressExpr(minFuncDecl);
    CallFunctionExpr    minFunc     = new CallFunctionExpr(intType, minFuncAddr, args);
    LoadDeclAddressExpr minVarAddr  = new LoadDeclAddressExpr(minVarDecl); // Create address load expression for the min variable.
    ExprChord           minVarDef   = new ExprChord(minVarAddr, minFunc); // minVar = min(upperBound, sm + strpSize -1);

    minArg2Chord.setTarget(minVarDef);

    // Create the initialization expression for the inner induction
    // variable:
    // i = sm;

    LoadDeclValueExpr sm1Load3 = new LoadDeclValueExpr(sm1Decl);
    Expr old = innerInit.getRValue();
    innerInit.changeInDataEdge(old, sm1Load3);
    old.unlinkExpression();

    innerInit.setTarget(tmp1Chord);
    minVarDef.setTarget(postInit);

    // Create the new predicate and decision chord for inner loop.

    Declaration       innerDecl  = ((LoadExpr) innerInit.getLValue()).getDecl();
    LoadDeclValueExpr innerLoad  = new LoadDeclValueExpr(innerDecl);
    LoadDeclValueExpr minVarLoad = new LoadDeclValueExpr(minVarDecl);
    LessEqualExpr     inle       = new LessEqualExpr(t, innerLoad, minVarLoad);
    Chord             te         = dec.getTrueCfgEdge();
    Chord             fe         = dec.getFalseCfgEdge();
    IfThenElseChord   innerdec   = new IfThenElseChord(inle, te, fe);

    // Adjust incoming CFG edges.

    Chord[] inEdges = dec.getInCfgEdgeArray();
    for (int i = 0; i < inEdges.length; i++) {
      Chord c = inEdges[i];
      c.replaceOutCfgEdge(dec, innerdec);
      innerdec.addInCfgEdge(c);
      dec.deleteInCfgEdge(c);
    }

    // Create new LoopHeaderChord object for inner loop.

    top.recomputeLoop();
    smHeader.recomputeLoop();
    loop.recomputeLoop();

    scribble.recomputeDominators();
  }

  /**
   * Interchange two loops.  The outerloop must be perfectly nested.
   * Works for Fortran do loops with lots of assumptions.
   */
  protected void performLoopInterchange(LoopHeaderChord innerLoop,
                                        LoopHeaderChord outerLoop) 
  {
    LoopInitChord   iPreFirstc    = innerLoop.getLoopInit();
    LoopExitChord   iExit         = innerLoop.getLoopExit(0);
    Chord           iTail         = innerLoop.getLoopTail();
    IfThenElseChord iTst          = innerLoop.getLoopTest();
    LoopInitChord   oPreFirstc    = outerLoop.getLoopInit();
    LoopExitChord   oExit         = outerLoop.getLoopExit(0);
    Chord           oTail         = outerLoop.getLoopTail();
    IfThenElseChord oTst          = outerLoop.getLoopTest();
    Chord           iFirstc       = iPreFirstc.getNextChord();
    Chord           oFirstc       = oPreFirstc.getNextChord();
    Chord           oTstFalseEdge = oTst.getFalseCfgEdge();
    Chord           iTstFalseEdge = iTst.getFalseCfgEdge();
    Chord           oTstTrueEdge  = oTst.getTrueCfgEdge();
    Chord           iTstTrueEdge  = iTst.getTrueCfgEdge();

    InductionVar ivar = outerLoop.getPrimaryInductionVar();
    if (ivar == null)
      return;

    VariableDecl iv = ivar.getVar();

    if (oTstTrueEdge == iPreFirstc) {
      // Loop test at beginning of loop.

      if (iTstTrueEdge.findLoopExit(innerLoop) != null)
        return;

      ExprChord oIndexInc = findIndexInc(oTail);    
      ExprChord iIndexInc = findIndexInc(iTail);

      if (scanFor(iv, iPreFirstc, iTst) || scanFor(iv, iIndexInc, innerLoop))
        return;

      if (iIndexInc.numInCfgEdges() > 1)
        iIndexInc.insertBeforeInCfg(new NullChord());
      
      Chord iPreInc = iIndexInc.getInCfgEdge();

      if (oIndexInc.numInCfgEdges() > 1)
        oIndexInc.insertBeforeInCfg(new NullChord());

      Chord oPreInc = oIndexInc.getInCfgEdge();

      // Interchange the loops.

      iTst.changeOutCfgEdge(iTstFalseEdge, oTstFalseEdge);
      oTst.changeOutCfgEdge(oTstFalseEdge, iTstFalseEdge);
      oTst.changeOutCfgEdge(oTstTrueEdge, iTstTrueEdge);
      iTst.changeOutCfgEdge(iTstTrueEdge, oTstTrueEdge);

      iPreInc.changeOutCfgEdge(iIndexInc, oIndexInc);
      oPreInc.changeOutCfgEdge(oIndexInc, iIndexInc);

      oPreFirstc.changeOutCfgEdge(oFirstc, iFirstc);
      iPreFirstc.changeOutCfgEdge(iFirstc, oFirstc);

    } else if (oTstTrueEdge == oTail) {
      // Loop test at the end of loop.

      if (iTstTrueEdge != iTail)
        return;

      ExprChord oIndexInc = findIndexInc(oTst);    
      ExprChord iIndexInc = findIndexInc(iTst);

      if (scanFor(iv, iPreFirstc, innerLoop) || scanFor(iv, iIndexInc, iTst))
        return;

      if (iIndexInc.numInCfgEdges() > 1)
        iIndexInc.insertBeforeInCfg(new NullChord());
      
      Chord iPreInc = iIndexInc.getInCfgEdge();

      if (oIndexInc.numInCfgEdges() > 1)
        oIndexInc.insertBeforeInCfg(new NullChord());

      Chord oPreInc = oIndexInc.getInCfgEdge();

      // Interchange the loops.

      iTst.changeOutCfgEdge(iTstFalseEdge, oTstFalseEdge);
      oTst.changeOutCfgEdge(oTstFalseEdge, iTstFalseEdge);

      Chord iSt   = innerLoop;
      Chord iNext = innerLoop.getNextChord();
      while (iNext.isPhiExpr()) {
        iSt = iNext;
        iNext = ((PhiExprChord) iNext).getNextChord();
      }

      Chord oSt   = outerLoop;
      Chord oNext = outerLoop.getNextChord();
      while (oNext.isPhiExpr()) {
        oSt = oNext;
        oNext = ((PhiExprChord) oNext).getNextChord();
      }

      iSt.changeOutCfgEdge(iNext, oNext);
      oSt.changeOutCfgEdge(oNext, iNext);

      iPreInc.changeOutCfgEdge(iIndexInc, oIndexInc);
      oPreInc.changeOutCfgEdge(oIndexInc, iIndexInc);

      oPreFirstc.changeOutCfgEdge(oFirstc, iFirstc);
      iPreFirstc.changeOutCfgEdge(iFirstc, oFirstc);
    } else
      return;

    if (trace) {
      System.out.print("** Swap outer ");
      System.out.println(scribble.getRoutineDecl().getName());
      System.out.print("        outer ");
      System.out.println(outerLoop);
      System.out.print("        inner ");
      System.out.println(innerLoop);
    }

    // Swap levels.

    iExit.setLoopHeader(outerLoop);
    oExit.setLoopHeader(innerLoop);
    outerLoop.setLoopInit(iPreFirstc);
    innerLoop.setLoopInit(oPreFirstc);

    // Adjust the parent links of the LoopHeaderChords.

    LoopHeaderChord parent = outerLoop.getParent();
    outerLoop.setParent(innerLoop);
    innerLoop.setParent(parent);

    permuteLoopCount++;

    // Adjust loop and parent links.

    innerLoop.recomputeLoop();
    outerLoop.recomputeLoop();

    scribble.recomputeDominators();
  }

  private boolean scanFor(VariableDecl iv, Chord first, Chord last)
  {
    iv = iv.getOriginal();

    while (first != null) {
      Vector<Declaration> v = first.getDeclList();
      if (v != null) {
        int l = v.size();
        for (int i = 0; i < l; i++) {
          VariableDecl vd = (VariableDecl) v.get(i);
          if (vd.getOriginal() == iv)
            return true;
        }
      }

      if (first == last)
        break;

      first = first.getOutCfgEdge(0);
    }

    return false;
  }

  /**
   * This method is totally bogus.
   */
  private void moveBackNonIndexPhis(LoopHeaderChord   iLoop,
                                    LoopHeaderChord   oLoop,
                                    Vector<ExprChord> phis)
  {
    Chord preChord  = oLoop;
    Chord nextChord = oLoop.getNextChord();
    int   l         = phis.size();

    for (int i = 0; i < l; i++) {
      ExprChord curChord  = phis.elementAt(i);
      Expr      le        = curChord.getLValue();
      boolean   movedBack = true;

      InductionVar iv = iLoop.getInductionVar(le);
      if (iv != null) {
        // If the phi node is for a primary induction variable, keep it there.

        if (iv.isPrimary())
          movedBack = false;    
      }     

      // Delete unnessary phi chords.

      if (movedBack) {
        if (le instanceof LoadDeclAddressExpr) {
          VariableDecl vd = (VariableDecl) ((LoadExpr) le).getDecl();
          VariableDecl vr = oLoop.getLoopIndexVar();
          
          if ((vr != null) && (vr == vd.getOriginal())) {
            curChord.removeFromCfg();
            movedBack = false;
          }  
        }
      }

      // Move other chords into its original position.

      if (movedBack) {
        curChord.extractFromCfg();
        preChord.insertAfterOutCfg(curChord, nextChord);
        preChord = curChord;
      } 
    }
  }

  /**
   * Clean up for profiling statistics.
   */
  public static void cleanup()
  {
    minFuncDecl    = null;
    maxFuncDecl    = null;
    floorFuncDecl  = null;
    boundFuncDecl  = null;
    lboundFuncDecl = null;
    uboundFuncDecl = null;

    intType         = null;
    floatType       = null;
    unsignedType    = null;
    intPointer      = null;
    unsignedPointer = null;
    voidp           = null;
  }
}
