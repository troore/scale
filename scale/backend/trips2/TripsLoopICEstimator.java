
package scale.backend.trips2;

import scale.backend.ICEstimator;
import scale.common.Machine;
import scale.common.Lattice;

import scale.score.chords.*;
import scale.score.expr.*;
import scale.score.Note;

import scale.clef.expr.*;
import scale.clef.decl.Assigned;
import scale.clef.decl.Declaration;
import scale.clef.type.Type;


/** 
 * This class estimates instruction counts for TRIPS loops.
 * <p>
 * $Id: TripsLoopICEstimator.java,v 1.6 2007-03-21 13:31:48 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class TripsLoopICEstimator extends ICEstimator
{
  /**
   * This class is used to estimate TRIPS loop instruction counts from the
   * Scribble CFG.
   */
  public TripsLoopICEstimator()
  {
    super(Machine.currentMachine);
  }

  protected void whatIsThis(Note n)
  {
    throw new scale.common.InternalError("Unexpected  " + n);
  }

  private static final boolean showDetails = false;

  private void estim(String s)
  {
    if (showDetails)
      System.out.println(s + " (" + estimate + ")");
  }

  public void visitLoadDeclAddressExpr(LoadDeclAddressExpr e)
  {
    Declaration decl = e.getDecl();
    Assigned    loc  = decl.getStorageLoc();
    int         est;

    if (e.getOutDataEdge() instanceof PhiExprChord) {
      estim("LDAE(phi):0");
      return;
    }

    switch (loc) {
    case IN_COMMON:
    case IN_MEMORY:
      // 3: 1 for the load instruction, 2 for the ENTERA for the address
      estimate += 3;
      estim("LDAE(mem):3");
      break;
    case ON_STACK:
      estimate++;
      estim("LDAE(stk):1");
      break;
    case IN_REGISTER:
      estim("LDAE(reg):0");
      break;
    default:
      assert(false) : "visitLDAE: unknown declaration type";
    }
  }

  public void visitLoadValueIndirectExpr(LoadValueIndirectExpr e)
  {
    estimate++;
    estim("LDVI:1");
  }

  public void visitLoadDeclValueExpr(LoadDeclValueExpr e)
  {
    // XXX: indentical to visitLDAE()?
    Declaration decl = e.getDecl();
    Assigned    loc  = decl.getStorageLoc();
    int         est  = 0;

    if (e.getOutDataEdge() instanceof PhiExpr) {
      estim("LDVE(phi):0");
      return;
    }

    switch (loc) {
    case IN_COMMON:
    case IN_MEMORY:
      // 3: 1 for the load instruction, 2 for the ENTERA for the address
      estimate += 3;
      estim("LDVE(mem):3");
      break;
    case ON_STACK:
      estimate++;
      estim("LDVE(stk):1");
      break;
    case IN_REGISTER:
      estim("LDVE(reg):0");
      break;
    }
  }

//  // Assume that we will have the base pointer to the struct already
//  // computed.  Then we have:
//  // - 1 instruction for the load/store.
//  // - 1 instruction for the presumed fan-out required.
//  // - 0 or 1 instructions for the offset (depends on its size, whether it
//  //   fits in the load/store immediate field).
//  //   XXX: although note that an offset that fits in early repeats of the
//  //   loop body may not fit if in later repeats if the loop is unrolled
//  //   enough times!
  public void visitLoadFieldAddressExpr(LoadFieldAddressExpr e)
  { 
//    long offset = e.getField().getFieldOffset();
//    int  est    = (-256 <= offset && offset < 256 ? 2 : 3);
    int est = 1;
    estimate += est;
    estim("LFAE:" + est);
  }

  public void visitLoadFieldValueExpr(LoadFieldValueExpr e)
  {
    estimate++;
    estim("LFVE:1");
  }

  // XXX: why 2?
  public void visitArrayIndexExpr(ArrayIndexExpr e)
  {
    estimate += 2;
    estim("ArrayIndex:2");
  }

  public void visitBeginChord(BeginChord c)
  {
    estimate += 3;
  }

  public void visitEndChord(EndChord c)
  {
    estimate += 3;
  }

  public void visitExitChord(ExitChord c)
  {
    estimate += 3;
  }

  public void visitReturnChord(ReturnChord c)
  {
    estimate += 3;
  }

  public void visitSwitchChord(SwitchChord c)
  {
    //assert(false) : "visitSwitchChord";

    long[] keys = c.getBranchEdgeKeyArray();
    int    num  = keys.length;
    int    est;

    if (num < 5)
      est = 2 * num;
    else
      est = 6;

    estimate += est;
    estim("switch:" + est);
  }

  public void visitExprChord(ExprChord c)
  {
    estim("assign:0");
  }

//  // Estimate zero:
//  // - if it's a loop exit test, we add two for it to the constant factor,
//  //   for:
//  //     bro_f<p>  f_target
//  //     bro_f<p>  t_target
//  // - if it is predicated, there are no extra instructions involved.
  public void visitIfThenElseChord(IfThenElseChord c)
  {
//    if (c == ltc) {
//      estim("if-then-else:0,2");
//    } else {
      estim("if-then-else:0,0");
//    }
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    int est = 1;
    estimate += est;
    estim("absVal:" + est);
  }

  // ICE is 1, unless it's < 64 bits in which case we allow also for the
  // probably-needed sign-extension.
  private int doBinaryOp(BinaryExpr e)
  {
    Type ct = e.getCoreType();
    int  bs = ct.memorySizeAsInt(machine);
    int est = ( bs <= 4 ? 2 : 1 );
    return est;
  }

  public void visitAdditionExpr(AdditionExpr e)
  {
    int est = doBinaryOp(e);
    estimate += est;
    estim("add:" + est);
  }

  public void visitAndExpr(AndExpr e)
  {
    int est = 1;
    estimate += est;
    estim("and:" + est);
  }

  public void visitBitAndExpr(BitAndExpr e)
  {
    // XXX: are sign-extensions common for bitwise-ANDs?
    //int est = doBinaryOp(e);
    int est = 1;
    estimate += est;
    estim("b-and:" + est);
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    int est = 1;
    estimate += est;
    estim("bitComp:" + est);
  }

  public void visitBitOrExpr(BitOrExpr e)
  {
    // XXX: are sign-extensions common for bitwise-ORs?
    //int est = doBinaryOp(e);
    int est = 1;
    estimate += est;
    estim("b-or:" + est);
  }

  public void visitBitXorExpr(BitXorExpr e)
  {
    int est = 1;
    estimate += est;
    estim("b-xor:" + est);
  }

  public void visitOrExpr(OrExpr e)
  {
    int est = 1;
    estimate += est;
    estim("or:" + est);
  }

  public void visitCompareExpr(CompareExpr e)
  {
    int est = 1;
    estimate += est;
    estim("cmp:" + est);
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    //int est = doBinaryOp(e);
    int est = 1;
    estimate += est;
    estim("div:" + est);
  }

  public void visitMultiplicationExpr(MultiplicationExpr e)
  {
    //int est = doBinaryOp(e);
    int est = 1;
    estimate += est;
    estim("mul:" + est);
  }

  public void visitRemainderExpr(RemainderExpr e)
  {
    Expr ra = e.getRightArg();

    if (ra.isLiteralExpr()) {
      LiteralExpr le    = (LiteralExpr) ra;
      Literal     lit   = le.getLiteral();

      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        long value = il.getLongValue();
        int shift = Lattice.powerOf2(value);
        if (0 <= shift && shift <= 255) {
          estimate++;
          estim("mod(mask):1");
          return;
        }
      }
    }

    // Normal mod requires a div, mul, sub
    int est = 4;
    estimate += est;
    estim("mod:" + est);
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    estimate++;
  }

  public void visitNotExpr(NotExpr e)
  {
    int est = 1;
    estimate += est;
    estim("not:" + est);
  }

  public void visitExponentiationExpr(ExponentiationExpr e)
  {
    int est = 1;
    estimate += est;
    estim("exponentiation:" + est);
  }

  public void visitTranscendentalExpr(TranscendentalExpr e)
  {
    int est = 4;
    estimate += est;
    estim("transc:" + est);
  }

  public void visitTranscendental2Expr(Transcendental2Expr e)
  {
    int est = 4;
    estimate += est;
    estim("transc2:" + est);
  }

  public void visitSubtractionExpr(SubtractionExpr e)
  {
////  XXX: if (induction var and a literal)
////      zero + 2
//    int est = doBinaryOp(e);
    int est = 1;
    estimate += est;
    estim("sub:" + est);
  }

  public void visitBitShiftExpr(BitShiftExpr e)
  {
//    // XXX: are sign-extensions common for shifts?
//    int est;
//    int mode = e.getShiftMode();
//    if (mode == BitShiftOp.cSignedRight || mode == BitShiftOp.cUnsignedRight) {
//      est = 1;    // Right-shifts don't need sign-extension
//    } else {
//      est = doBinaryOp(e);
//    }
//
    int est = 1;
    estimate += est;
    estim("shift:" + est);
  }

  public void visitCallMethodExpr(CallMethodExpr e)
  {
    int est = 3;
    estimate += est;
    estim("callMethod:" + est);
  }

  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    int est = 3;
    estimate += est;
    estim("callFunc:" + est);
  }

  public void visitComplexValueExpr(ComplexValueExpr e)
  {
    // do nothing
    estim("complexVal:0");
  }

  public void visitConversionExpr(ConversionExpr e)
  {
    CastMode cr = e.getConversion();
    int      est;

    switch (cr) {
    case CAST:
      est = 0;
      break;
    case TRUNCATE:
      est = 1;
      break;
    case REAL:
      est = 1;
      break;
    case ROUND:
      est = 3;
      break;
    case FLOOR:
      est = 1;
      break;
    case IMAGINARY:
      est = 0;
      break;
    default:
      throw new scale.common.NotImplementedError("Type conversion " + e);
    }
  
    estimate += est;
    estim("conv:" + est);
  }

  public void visitDualExpr(DualExpr e)
  {
    // do nothing
    estim("dual:0");
  }

  public void visitEqualityExpr(EqualityExpr e)
  {
    estimate++;
    estim("EQ:1");
  }

  public void visitGreaterEqualExpr(GreaterEqualExpr e)
  {
    estimate++;
    estim("GE:1");
  }

  public void visitGreaterExpr(GreaterExpr e)
  {
//    int est;
//    if (e.getOutDataEdge() == getLtc()) {
//      est = 0;
//    } else {
//      est = 1;
//    }
    int est = 1;
    estimate += est;
    estim("GT:" + est);
  }

  public void visitLessEqualExpr(LessEqualExpr e)
  {
    estimate++;
    estim("LE:1");
  }

  public void visitLessExpr(LessExpr e)
  {
    estimate++;
    estim("LT:1");
  }

  public void visitNotEqualExpr(NotEqualExpr e)
  {
    estimate++;
    estim("NE:1");
  }

  public void visitLeaveChord(LeaveChord c)
  {
    int est = 1;
    estimate += est;
    estim("leave:" + est);
  }

  // XXX: if you have really big literals, you need multiple instructions to
  // generate them...
  public void visitLiteralExpr(LiteralExpr e)
  {
    int est = e.executionCostEstimate();
    estimate += est;
    estim("Lit:" + est);
  }

  public void visitNilExpr(NilExpr e)
  {
    int est = 1;
    estimate += est;
    estim("Nil:" + est);
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    int est = 1;
    estimate += est;
    estim("VaStart:" + est);
  }

  public void visitVaArgExpr(VaArgExpr e)
  {
    int est = 4;
    estimate += est;
    estim("VaArg:" + est);
  }

  public void visitGotoChord(GotoChord c)
  {
    estim("Goto:0");
    /* Do Nothing */
  }

  public void visitLoopExitChord(LoopExitChord c)
  {
    estim("LoopExitCh:0");
    /* Do Nothing */
  }

  public void visitLoopHeaderChord(LoopHeaderChord c)
  {
    estim("LoopHeaderCh:0");
    /* Do Nothing */
  }

  public void visitLoopPreHeaderChord(LoopPreHeaderChord c)
  {
    estim("LoopPreHeaderCh:0");
    /* Do Nothing */
  }

  public void visitLoopTailChord(LoopTailChord c)
  {
    estim("LoopTailCh:0");
    /* Do Nothing */
  }

  public void visitLoopInitChord(LoopInitChord c)
  {
    estim("LoopInitCh:0");
    /* Do Nothing */
  }

  public void visitNullChord(NullChord c)
  {
    /* Do Nothing */
  }

  // ICE is zero -- Phi nodes are removed before code generation.
  public void visitPhiExpr(PhiExpr e)
  {
    estim("Phi:0");
  }

  // ICE is zero -- Phi nodes are removed before code generation.
  public void visitPhiExprChord(PhiExprChord c)
  {
    estim("PhiCh:0");
  }

  public void visitVaEndExpr(VaEndExpr e)
  {
    estim("VaEnd:0");
    // Do nothing.
  }

  public void visitConditionalExpr(ConditionalExpr e)
  {
    int est = 1;
    estimate += est;
    estim("Cond:" + est);
  }
}
