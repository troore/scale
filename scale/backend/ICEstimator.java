package scale.backend;

import scale.common.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;


/** 
 * This class is the base class for code size estimators.
 * <p>
 * $Id: ICEstimator.java,v 1.19 2007-03-21 13:31:45 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class is intended to be used as the base class for the
 * estimator for any architecture that requires instruction count
 * estimates prior to code generation.  This class provides a rough
 * estimate only.
 * <p>
 * The estimator works by visiting every in-coming data edge of a
 * specified node.  It does not follow CFG edges.  To obtain the cost
 * of a CFG node, call the {@link scale.backend.ICEstimator#estimate
 * estimate}() method with the node as the argument.  The
 * <code>estimate()</code> method can also be applied to an
 * expression.  In this case, the estimate for the specified
 * expression sub-tree will be computed.
 * <p>
 * NOTE - to facilitate obtaining the estimate for a CFG or a
 * sub-graph of a CFG, the {@link scale.backend.ICEstimator#resetEstimate
 * resetEstimate}() method is provided.  The estimate returned by any method
 * is always the sum of the individual estimates between calls to
 * <code>reset()</code>.
 */

public class ICEstimator implements scale.score.Predicate
{
  /**
   * Machine specific information.
   */
  protected Machine machine;

  /**
   * The current estimate.
   */
  protected int estimate;

  /**
   * This class is used to convert the Scribble CFG to machine instructions.
   * @param machine specifies the target architecture
   */
  public ICEstimator(Machine machine)
  {
    this.machine  = machine;
    this.estimate = 0;
  }

  /**
   * Compute an estimate and return the current estimate.
   * @see #resetEstimate
   */
  public int estimate(Note n)
  {
    int l = n.numInDataEdges();
    for (int i = 0; i < l; i++) {
      Note in = n.getInDataEdge(i);
      while (in instanceof DualExpr)
        in = ((DualExpr) in).getLow();
      estimate(in);
    }

    n.visit(this);

    return estimate;
  }

  /**
   * Return the machine definition in use.
   */
  public final Machine getMachine()
  {
    return machine;
  }

  /**
   * Return the current estimate.
   */
  public final int getEstimate()
  {
    return estimate;
  }
  
  /**
   * Return the estimate and reset it to zero.
   */
  public final int getEstimateAndReset()
  {
    int x = estimate;
    estimate = 0;
    return x;
  }
  
  /**
   * Set the estimate.
   */
  public final void setEstimate(int estimate)
  {
    this.estimate = estimate;
  }

  /**
   * Set the estimate to zero.
   */
  public final void resetEstimate()
  {
    this.estimate = 0;
  }

  /**
   * Generate an error.
   */
  protected void whatIsThis(Note n)
  {
    throw new scale.common.InternalError("Unexpected  " + n);
  }

  public void visitLoadDeclAddressExpr(LoadDeclAddressExpr e)
  {
    estimate += 2;
  }

  public void visitLoadValueIndirectExpr(LoadValueIndirectExpr e)
  {
    estimate++;
  }

  public void visitLoadDeclValueExpr(LoadDeclValueExpr e)
  {
    Declaration decl = e.getDecl();
    Assigned    loc  = decl.getStorageLoc();

    switch (loc) {
    case IN_COMMON:
    case IN_MEMORY:
      estimate += 2;
      return;
    case ON_STACK:
      estimate++;
      return;
    case IN_REGISTER:
      return;
    }
  }

  public void visitLoadFieldAddressExpr(LoadFieldAddressExpr e)
  {
    estimate++;
  }

  public void visitLoadFieldValueExpr(LoadFieldValueExpr e)
  {
    estimate++;
  }

  public void visitArrayIndexExpr(ArrayIndexExpr e)
  {
    estimate += 2;
  }

  public void visitBeginChord(BeginChord c)
  {
    estimate++;
  }

  public void visitEndChord(EndChord c)
  {
    estimate++;
  }

  public void visitExitChord(ExitChord c)
  {
    estimate++;
  }

  public void visitReturnChord(ReturnChord c)
  {
    estimate++;
  }

  public void visitSwitchChord(SwitchChord c)
  {
    long[] keys = c.getBranchEdgeKeyArray();
    int    num  = keys.length;

    if (num < 5)
      estimate += 2 * num;
    else
      estimate += 6;
  }

  public void visitExprChord(ExprChord c)
  {
    estimate++;
  }

  public void visitIfThenElseChord(IfThenElseChord c)
  {
    estimate += 3;
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    estimate++;
  }

  public void visitAdditionExpr(AdditionExpr e)
  {
    estimate++;
  }

  public void visitAndExpr(AndExpr e)
  {
    estimate++;
  }

  public void visitBitAndExpr(BitAndExpr e)
  {
    estimate++;
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    estimate++;
  }

  public void visitBitOrExpr(BitOrExpr e)
  {
    estimate++;
  }

  public void visitBitXorExpr(BitXorExpr e)
  {
    estimate++;
  }

  public void visitOrExpr(OrExpr e)
  {
    estimate++;
  }

  public void visitCompareExpr(CompareExpr e)
  {
    estimate++;
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    estimate++;
  }

  public void visitMultiplicationExpr(MultiplicationExpr e)
  {
    estimate++;
  }

  public void visitRemainderExpr(RemainderExpr e)
  {
    estimate += 4;
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    estimate++;
  }

  public void visitNotExpr(NotExpr e)
  {
    estimate++;
  }

  public void visitExponentiationExpr(ExponentiationExpr e)
  {
    estimate++;
  }

  public void visitTranscendentalExpr(TranscendentalExpr e)
  {
    estimate += 4;
  }

  public void visitTranscendental2Expr(Transcendental2Expr e)
  {
    estimate += 4;
  }

  public void visitSubtractionExpr(SubtractionExpr e)
  {
    estimate++;
  }

  public void visitBitShiftExpr(BitShiftExpr e)
  {
    estimate++;
  }

  public void visitCallMethodExpr(CallMethodExpr e)
  {
    estimate += 3;
  }

  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    estimate += 3;
  }

  public void visitComplexValueExpr(ComplexValueExpr e)
  {
    // do nothing
  }

  public void visitConversionExpr(ConversionExpr e)
  {
    CastMode cr = e.getConversion();

    switch (cr) {
    case CAST:
      return;
    case TRUNCATE:
      estimate++;
      return;
    case REAL:
      estimate++;
      return;
    case ROUND:
      estimate += 3;
      return;
    case FLOOR:
      estimate++;
      return;
    case IMAGINARY:
      return;
    }

    throw new scale.common.NotImplementedError("Type conversion " + e);
  }

  public void visitDualExpr(DualExpr e)
  {
    // do nothing
  }

  public void visitEqualityExpr(EqualityExpr e)
  {
    estimate++;
  }

  public void visitGreaterEqualExpr(GreaterEqualExpr e)
  {
    estimate++;
  }

  public void visitGreaterExpr(GreaterExpr e)
  {
    estimate++;
  }

  public void visitLessEqualExpr(LessEqualExpr e)
  {
    estimate++;
  }

  public void visitLessExpr(LessExpr e)
  {
    estimate++;
  }

  public void visitNotEqualExpr(NotEqualExpr e)
  {
    estimate++;
  }

  public void visitLeaveChord(LeaveChord c)
  {
    estimate++;
  }

  public void visitLiteralExpr(LiteralExpr e)
  {
    estimate += e.executionCostEstimate();
  }

  public void visitNilExpr(NilExpr e)
  {
    estimate++;
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    estimate++;
  }

  public void visitVaArgExpr(VaArgExpr e)
  {
    estimate += 4;
  }

  public void visitAllocateExpr(AllocateExpr e)
  {
    whatIsThis(e);
  }

  public void visitBinaryExpr(BinaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitBranchChord(BranchChord c)
  {
    whatIsThis(c);
  }

  public void visitCallExpr(CallExpr e)
  {
    whatIsThis(e);
  }

  public void visitChord(Chord c)
  {
    whatIsThis(c);
  }

  public void visitDecisionChord(DecisionChord c)
  {
    whatIsThis(c);
  }

  public void visitExpr(Expr e)
  {
    whatIsThis(e);
  }

  public void visitExprPhiExpr(ExprPhiExpr e)
  {
    whatIsThis(e);
  }

  public void visitLoadExpr(LoadExpr e)
  {
    whatIsThis(e);
  }

  public void visitMaxExpr(MaxExpr e)
  {
    whatIsThis(e);
  }

  public void visitMinExpr(MinExpr e)
  {
    whatIsThis(e);
  }

  public void visitNaryExpr(NaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitNote(Note n)
  {
    whatIsThis(n);
  }

  public void visitMarkerChord(MarkerChord c)
  {
    /* Do Nothing */
  }

  public void visitGotoChord(GotoChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopExitChord(LoopExitChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopHeaderChord(LoopHeaderChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopPreHeaderChord(LoopPreHeaderChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopTailChord(LoopTailChord c)
  {
    estimate++; // For the branch back.
  }

  public void visitLoopInitChord(LoopInitChord c)
  {
    /* Do Nothing */
  }

  public void visitNullChord(NullChord c)
  {
    /* Do Nothing */
  }

  public void visitPhiExpr(PhiExpr e)
  {
    whatIsThis(e);
  }

  public void visitPhiExprChord(PhiExprChord c)
  {
    whatIsThis(c);
  }

  public void visitSequentialChord(SequentialChord c)
  {
    whatIsThis(c);
  }

  public void visitSubscriptExpr(SubscriptExpr e)
  {
    whatIsThis(e);
  }

  public void visitTernaryExpr(TernaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitUnaryExpr(UnaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitValueExpr(ValueExpr e)
  {
    whatIsThis(e);
  }

  public void visitVarArgExpr(VarArgExpr e)
  {
    whatIsThis(e);
  }

  public void visitVaEndExpr(VaEndExpr e)
  {
    // Do nothing.
  }

  public void visitVectorExpr(VectorExpr e)
  {
    whatIsThis(e);
  }

  public void visitConditionalExpr(ConditionalExpr e)
  {
    estimate++;
  }
}
