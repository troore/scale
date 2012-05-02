package scale.clef;

import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * This class provides a default implementation of the Predicate visit
 * pattern that generates an error.
 * <p>
 * $Id: ErrorPredicate.java,v 1.62 2007-10-04 19:58:03 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * All of the visit methods in this class throw an error.  To make
 * a useable predicate from this class, you will need to override the
 * visit methods you expect to use.  You will want to inherit from this
 * class if you know that you will invoke only a subset of the visit
 * methods, and want to detect that you have visited a Clef class instance
 * unexpectedly.  
 */

public abstract class ErrorPredicate implements Predicate 
{
  private void GenErr(Node n)
  {
    throw new scale.common.NotImplementedError(getClass().getName() +
                                               " does not define behavior for class " +
                                               n.getClass().getName());
  }

  public void visitAbsoluteValueOp(AbsoluteValueOp n)                 { GenErr(n); } 
  public void visitAdditionAssignmentOp(AdditionAssignmentOp n)       { GenErr(n); } 
  public void visitAdditionOp(AdditionOp n)                           { GenErr(n); } 
  public void visitAddressLiteral(AddressLiteral n)                   { GenErr(n); } 
  public void visitAddressOp(AddressOp n)                             { GenErr(n); } 
  public void visitAggregateOp(AggregateOp n)                         { GenErr(n); } 
  public void visitAggregateType(AggregateType n)                     { GenErr(n); } 
  public void visitAggregationElements(AggregationElements n)         { GenErr(n); } 
  public void visitAllocArrayType(AllocArrayType n)                   { GenErr(n); } 
  public void visitAllocatePlacementOp(AllocatePlacementOp n)         { GenErr(n); } 
  public void visitAllocateSettingFieldsOp(AllocateSettingFieldsOp n) { GenErr(n); } 
  public void visitAltCase(AltCase n)                                 { GenErr(n); } 
  public void visitAndConditionalOp(AndConditionalOp n)               { GenErr(n); } 
  public void visitAndOp(AndOp n)                                     { GenErr(n); } 
  public void visitArithmeticIfStmt(ArithmeticIfStmt n)               { GenErr(n); } 
  public void visitMonadicOp(MonadicOp n)                             { GenErr(n); } 
  public void visitArrayType(ArrayType n)                             { GenErr(n); } 
  public void visitAssignLabelStmt(AssignLabelStmt n)                 { GenErr(n); } 
  public void visitAssignSimpleOp(AssignSimpleOp n)                   { GenErr(n); } 
  public void visitAssignedGotoStmt(AssignedGotoStmt n)               { GenErr(n); } 
  public void visitAssignmentOp(AssignmentOp n)                       { GenErr(n); } 
  public void visitAtomicType(AtomicType n)                           { GenErr(n); } 
  public void visitBitAndAssignmentOp(BitAndAssignmentOp n)           { GenErr(n); } 
  public void visitBitAndOp(BitAndOp n)                               { GenErr(n); } 
  public void visitBitComplementOp(BitComplementOp n)                 { GenErr(n); } 
  public void visitBitOrAssignmentOp(BitOrAssignmentOp n)             { GenErr(n); } 
  public void visitBitOrOp(BitOrOp n)                                 { GenErr(n); } 
  public void visitBitShiftAssignmentOp(BitShiftAssignmentOp n)       { GenErr(n); } 
  public void visitBitShiftOp(BitShiftOp n)                           { GenErr(n); } 
  public void visitBitXorAssignmentOp(BitXorAssignmentOp n)           { GenErr(n); } 
  public void visitBitXorOp(BitXorOp n)                               { GenErr(n); } 
  public void visitBlockStmt(BlockStmt n)                             { GenErr(n); } 
  public void visitBooleanLiteral(BooleanLiteral n)                   { GenErr(n); } 
  public void visitBooleanType(BooleanType n)                         { GenErr(n); } 
  public void visitBound(Bound n)                                     { GenErr(n); } 
  public void visitBreakStmt(BreakStmt n)                             { GenErr(n); } 
  public void visitCallFunctionOp(CallFunctionOp n)                   { GenErr(n); } 
  public void visitCallOp(CallOp n)                                   { GenErr(n); } 
  public void visitCaseLabelDecl(CaseLabelDecl n)                     { GenErr(n); } 
  public void visitCaseStmt(CaseStmt n)                               { GenErr(n); } 
  public void visitCharLiteral(CharLiteral n)                         { GenErr(n); } 
  public void visitCharacterType(CharacterType n)                     { GenErr(n); } 
  public void visitComplexLiteral(ComplexLiteral n)                   { GenErr(n); } 
  public void visitComplexOp(ComplexOp n)                             { GenErr(n); } 
  public void visitComplexType(ComplexType n)                         { GenErr(n); } 
  public void visitCompositeType(CompositeType n)                     { GenErr(n); } 
  public void visitCompoundAssignmentOp(CompoundAssignmentOp n)       { GenErr(n); } 
  public void visitComputedGotoStmt(ComputedGotoStmt n)               { GenErr(n); } 
  public void visitContinueStmt(ContinueStmt n)                       { GenErr(n); } 
  public void visitDeclStmt(DeclStmt n)                               { GenErr(n); } 
  public void visitDeclaration(Declaration n)                         { GenErr(n); } 
  public void visitDefOp(DefOp n)                                     { GenErr(n); } 
  public void visitDeleteArrayOp(DeleteArrayOp n)                     { GenErr(n); } 
  public void visitDeleteOp(DeleteOp n)                               { GenErr(n); } 
  public void visitDereferenceOp(DereferenceOp n)                     { GenErr(n); } 
  public void visitDivisionAssignmentOp(DivisionAssignmentOp n)       { GenErr(n); } 
  public void visitDivisionOp(DivisionOp n)                           { GenErr(n); } 
  public void visitDoLoopStmt(DoLoopStmt n)                           { GenErr(n); } 
  public void visitDyadicOp(DyadicOp n)                               { GenErr(n); } 
  public void visitEnumElementDecl(EnumElementDecl n)                 { GenErr(n); } 
  public void visitEnumerationType(EnumerationType n)                 { GenErr(n); } 
  public void visitEqualityOp(EqualityOp n)                           { GenErr(n); } 
  public void visitEquivalenceDecl(EquivalenceDecl n)                 { GenErr(n); } 
  public void visitEvalStmt(EvalStmt n)                               { GenErr(n); } 
  public void visitExceptionDecl(ExceptionDecl n)                     { GenErr(n); }
  public void visitExitStmt(ExitStmt n)                               { GenErr(n); } 
  public void visitExponentiationOp(ExponentiationOp n)               { GenErr(n); } 
  public void visitExpression(Expression n)                           { GenErr(n); } 
  public void visitExpressionIfOp(ExpressionIfOp n)                   { GenErr(n); } 
  public void visitFieldDecl(FieldDecl n)                             { GenErr(n); } 
  public void visitFileDecl(FileDecl n)                               { GenErr(n); } 
  public void visitFixedArrayType(FixedArrayType n)                   { GenErr(n); } 
  public void visitFloatArrayLiteral(FloatArrayLiteral n)             { GenErr(n); } 
  public void visitFloatLiteral(FloatLiteral n)                       { GenErr(n); } 
  public void visitFloatType(FloatType n)                             { GenErr(n); } 
  public void visitForLoopStmt(ForLoopStmt n)                         { GenErr(n); } 
  public void visitFormalDecl(FormalDecl n)                           { GenErr(n); } 
  public void visitForwardProcedureDecl(ForwardProcedureDecl n)       { GenErr(n); } 
  public void visitGotoStmt(GotoStmt n)                               { GenErr(n); } 
  public void visitGreaterEqualOp(GreaterEqualOp n)                   { GenErr(n); } 
  public void visitGreaterOp(GreaterOp n)                             { GenErr(n); } 
  public void visitHeapOp(HeapOp n)                                   { GenErr(n); } 
  public void visitIdAddressOp(IdAddressOp n)                         { GenErr(n); } 
  public void visitIdReferenceOp(IdReferenceOp n)                     { GenErr(n); } 
  public void visitIdValueOp(IdValueOp n)                             { GenErr(n); } 
  public void visitIfStmt(IfStmt n)                                   { GenErr(n); } 
  public void visitIfThenElseStmt(IfThenElseStmt n)                   { GenErr(n); } 
  public void visitIncompleteType(IncompleteType n)                   { GenErr(n); } 
  public void visitIncrementOp(IncrementOp n)                         { GenErr(n); } 
  public void visitIntArrayLiteral(IntArrayLiteral n)                 { GenErr(n); } 
  public void visitIntLiteral(IntLiteral n)                           { GenErr(n); } 
  public void visitIntegerType(IntegerType n)                         { GenErr(n); } 
  public void visitSignedIntegerType(SignedIntegerType n)             { GenErr(n); } 
  public void visitUnsignedIntegerType(UnsignedIntegerType n)         { GenErr(n); } 
  public void visitFortranCharType(FortranCharType n)                 { GenErr(n); } 
  public void visitLabelDecl(LabelDecl n)                             { GenErr(n); } 
  public void visitLabelStmt(LabelStmt n)                             { GenErr(n); } 
  public void visitLessEqualOp(LessEqualOp n)                         { GenErr(n); } 
  public void visitLessOp(LessOp n)                                   { GenErr(n); } 
  public void visitLiteral(Literal n)                                 { GenErr(n); } 
  public void visitLoopStmt(LoopStmt n)                               { GenErr(n); } 
  public void visitMaximumOp(MaximumOp n)                             { GenErr(n); } 
  public void visitMinimumOp(MinimumOp n)                             { GenErr(n); } 
  public void visitModulusOp(ModulusOp n)                             { GenErr(n); } 
  public void visitMultiBranchStmt(MultiBranchStmt n)                 { GenErr(n); } 
  public void visitMultiplicationAssignmentOp(MultiplicationAssignmentOp n) { GenErr(n); } 
  public void visitMultiplicationOp(MultiplicationOp n)               { GenErr(n); } 
  public void visitNegativeOp(NegativeOp n)                           { GenErr(n); } 
  public void visitNilOp(NilOp n)                                     { GenErr(n); } 
  public void visitNode(Node n)                                       { GenErr(n); } 
  public void visitNotEqualOp(NotEqualOp n)                           { GenErr(n); } 
  public void visitNotOp(NotOp n)                                     { GenErr(n); } 
  public void visitNullStmt(NullStmt n)                               { GenErr(n); } 
  public void visitNumericType(NumericType n)                         { GenErr(n); } 
  public void visitOrConditionalOp(OrConditionalOp n)                 { GenErr(n); } 
  public void visitOrOp(OrOp n)                                       { GenErr(n); } 
  public void visitParenthesesOp(ParenthesesOp n)                     { GenErr(n); } 
  public void visitPointerType(PointerType n)                         { GenErr(n); } 
  public void visitPositiveOp(PositiveOp n)                           { GenErr(n); } 
  public void visitPostDecrementOp(PostDecrementOp n)                 { GenErr(n); } 
  public void visitPostIncrementOp(PostIncrementOp n)                 { GenErr(n); } 
  public void visitPreDecrementOp(PreDecrementOp n)                   { GenErr(n); } 
  public void visitPreIncrementOp(PreIncrementOp n)                   { GenErr(n); } 
  public void visitProcedureDecl(ProcedureDecl n)                     { GenErr(n); } 
  public void visitProcedureType(ProcedureType n)                     { GenErr(n); } 
  public void visitRaise(Raise n)                                     { GenErr(n); }
  public void visitRaiseWithObject(RaiseWithObject n)                 { GenErr(n); } 
  public void visitRaiseWithType(RaiseWithType n)                     { GenErr(n); } 
  public void visitRealType(RealType n)                               { GenErr(n); } 
  public void visitRecordType(RecordType n)                           { GenErr(n); } 
  public void visitRefType(RefType n)                                 { GenErr(n); } 
  public void visitRemainderAssignmentOp(RemainderAssignmentOp n)     { GenErr(n); } 
  public void visitRemainderOp(RemainderOp n)                         { GenErr(n); } 
  public void visitRenamedVariableDecl(RenamedVariableDecl n)         { GenErr(n); } 
  public void visitRepeatUntilLoopStmt(RepeatUntilLoopStmt n)         { GenErr(n); } 
  public void visitRepeatWhileLoopStmt(RepeatWhileLoopStmt n)         { GenErr(n); } 
  public void visitReturnStmt(ReturnStmt n)                           { GenErr(n); } 
  public void visitRoutineDecl(RoutineDecl n)                         { GenErr(n); } 
  public void visitSelectIndirectOp(SelectIndirectOp n)               { GenErr(n); } 
  public void visitSelectOp(SelectOp n)                               { GenErr(n); } 
  public void visitSeriesOp(SeriesOp n)                               { GenErr(n); } 
  public void visitSizeofLiteral(SizeofLiteral n)                     { GenErr(n); } 
  public void visitStatement(Statement n)                             { GenErr(n); } 
  public void visitStatementOp(StatementOp n)                         { GenErr(n); } 
  public void visitStringLiteral(StringLiteral n)                     { GenErr(n); } 
  public void visitSubscriptAddressOp(SubscriptAddressOp n)           { GenErr(n); } 
  public void visitSubscriptOp(SubscriptOp n)                         { GenErr(n); } 
  public void visitSubscriptValueOp(SubscriptValueOp n)               { GenErr(n); } 
  public void visitSubstringOp(SubstringOp n)                         { GenErr(n); } 
  public void visitSubtractionAssignmentOp(SubtractionAssignmentOp n) { GenErr(n); } 
  public void visitSubtractionOp(SubtractionOp n)                     { GenErr(n); } 
  public void visitSwitchStmt(SwitchStmt n)                           { GenErr(n); } 
  public void visitTernaryOp(TernaryOp n)                             { GenErr(n); } 
  public void visitTestLoopStmt(TestLoopStmt n)                       { GenErr(n); } 
  public void visitThisOp(ThisOp n)                                   { GenErr(n); } 
  public void visitTranscendentalOp(TranscendentalOp n)               { GenErr(n); } 
  public void visitTranscendental2Op(Transcendental2Op n)             { GenErr(n); } 
  public void visitType(Type n)                                       { GenErr(n); } 
  public void visitTypeConversionOp(TypeConversionOp n)               { GenErr(n); } 
  public void visitTypeDecl(TypeDecl n)                               { GenErr(n); } 
  public void visitTypeName(TypeName n)                               { GenErr(n); } 
  public void visitUnionType(UnionType n)                             { GenErr(n); } 
  public void visitUnknownFormals(UnknownFormals n)                   { GenErr(n); } 
  public void visitVaArgOp(VaArgOp n)                                 { GenErr(n); } 
  public void visitVaCopyOp(VaCopyOp n)                               { GenErr(n); } 
  public void visitVaEndOp(VaEndOp n)                                 { GenErr(n); } 
  public void visitVaStartOp(VaStartOp n)                             { GenErr(n); } 
  public void visitValueDecl(ValueDecl n)                             { GenErr(n); } 
  public void visitVarArgOp(VarArgOp n)                               { GenErr(n); } 
  public void visitVariableDecl(VariableDecl n)                       { GenErr(n); } 
  public void visitVoidType(VoidType n)                               { GenErr(n); } 
  public void visitWhileLoopStmt(WhileLoopStmt n)                     { GenErr(n); } 
}
