package scale.clef;

import scale.clef.expr.*;

/**
 * Predicate class for visit pattern of Clef Expressions.
 * <p>
 * $Id: ExprPredicate.java,v 1.37 2007-05-10 16:47:59 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see scale.clef.Predicate
 */

public interface ExprPredicate
{
  public void visitAbsoluteValueOp(AbsoluteValueOp n);
  public void visitAdditionAssignmentOp(AdditionAssignmentOp n);
  public void visitAdditionOp(AdditionOp n);
  public void visitAddressLiteral(AddressLiteral n);
  public void visitAddressOp(AddressOp n);
  public void visitAggregateOp(AggregateOp n);
  public void visitAggregationElements(AggregationElements n);
  public void visitAllocatePlacementOp(AllocatePlacementOp n);
  public void visitAllocateSettingFieldsOp(AllocateSettingFieldsOp n);
  public void visitAndConditionalOp(AndConditionalOp n);
  public void visitAndOp(AndOp n);
  public void visitMonadicOp(MonadicOp n);
  public void visitAssignSimpleOp(AssignSimpleOp n);
  public void visitAssignmentOp(AssignmentOp n);
  public void visitBitAndAssignmentOp(BitAndAssignmentOp n);
  public void visitBitAndOp(BitAndOp n);
  public void visitBitComplementOp(BitComplementOp n);
  public void visitBitOrAssignmentOp(BitOrAssignmentOp n);
  public void visitBitOrOp(BitOrOp n);
  public void visitBitShiftAssignmentOp(BitShiftAssignmentOp n);
  public void visitBitShiftOp(BitShiftOp n);
  public void visitBitXorAssignmentOp(BitXorAssignmentOp n);
  public void visitBitXorOp(BitXorOp n);
  public void visitBooleanLiteral(BooleanLiteral n);
  public void visitCallFunctionOp(CallFunctionOp n);
  public void visitCallOp(CallOp n);
  public void visitCharLiteral(CharLiteral n);
  public void visitComplexLiteral(ComplexLiteral n);
  public void visitComplexOp(ComplexOp n);
  public void visitCompoundAssignmentOp(CompoundAssignmentOp n);
  public void visitDefOp(DefOp n);
  public void visitDeleteArrayOp(DeleteArrayOp n);
  public void visitDeleteOp(DeleteOp n);
  public void visitDereferenceOp(DereferenceOp n);
  public void visitDivisionAssignmentOp(DivisionAssignmentOp n);
  public void visitDivisionOp(DivisionOp n);
  public void visitDyadicOp(DyadicOp n);
  public void visitEqualityOp(EqualityOp n);
  public void visitExponentiationOp(ExponentiationOp n);
  public void visitExpression(Expression n);
  public void visitExpressionIfOp(ExpressionIfOp n);
  public void visitFloatArrayLiteral(FloatArrayLiteral n);
  public void visitFloatLiteral(FloatLiteral n);
  public void visitGreaterEqualOp(GreaterEqualOp n);
  public void visitGreaterOp(GreaterOp n);
  public void visitHeapOp(HeapOp n);
  public void visitIdAddressOp(IdAddressOp n);
  public void visitIdReferenceOp(IdReferenceOp n);
  public void visitIdValueOp(IdValueOp n);
  public void visitIncrementOp(IncrementOp n);
  public void visitIntArrayLiteral(IntArrayLiteral n);
  public void visitIntLiteral(IntLiteral n);
  public void visitLessEqualOp(LessEqualOp n);
  public void visitLessOp(LessOp n);
  public void visitLiteral(Literal n);
  public void visitMaximumOp(MaximumOp n);
  public void visitMinimumOp(MinimumOp n);
  public void visitModulusOp(ModulusOp n);
  public void visitMultiplicationAssignmentOp(MultiplicationAssignmentOp n);
  public void visitMultiplicationOp(MultiplicationOp n);
  public void visitNegativeOp(NegativeOp n);
  public void visitNilOp(NilOp n);
  public void visitNotEqualOp(NotEqualOp n);
  public void visitNotOp(NotOp n);
  public void visitOrConditionalOp(OrConditionalOp n);
  public void visitOrOp(OrOp n);
  public void visitParenthesesOp(ParenthesesOp n);
  public void visitPositiveOp(PositiveOp n);
  public void visitPostDecrementOp(PostDecrementOp n);
  public void visitPostIncrementOp(PostIncrementOp n);
  public void visitPreDecrementOp(PreDecrementOp n);
  public void visitPreIncrementOp(PreIncrementOp n);
  public void visitRemainderAssignmentOp(RemainderAssignmentOp n);
  public void visitRemainderOp(RemainderOp n);
  public void visitSelectIndirectOp(SelectIndirectOp n);
  public void visitSelectOp(SelectOp n);
  public void visitSeriesOp(SeriesOp n);
  public void visitSizeofLiteral(SizeofLiteral n);
  public void visitStatementOp(StatementOp n);
  public void visitStringLiteral(StringLiteral n);
  public void visitSubscriptAddressOp(SubscriptAddressOp n);
  public void visitSubscriptOp(SubscriptOp n);
  public void visitSubscriptValueOp(SubscriptValueOp n);
  public void visitSubstringOp(SubstringOp n);
  public void visitSubtractionAssignmentOp(SubtractionAssignmentOp n);
  public void visitSubtractionOp(SubtractionOp n);
  public void visitTernaryOp(TernaryOp n);
  public void visitThisOp(ThisOp n);
  public void visitTranscendentalOp(TranscendentalOp n);
  public void visitTranscendental2Op(Transcendental2Op n);
  public void visitTypeConversionOp(TypeConversionOp n);
  public void visitVaArgOp(VaArgOp n);
  public void visitVaCopyOp(VaCopyOp n);
  public void visitVaEndOp(VaEndOp n);
  public void visitVaStartOp(VaStartOp n);
  public void visitVarArgOp(VarArgOp n);
}
