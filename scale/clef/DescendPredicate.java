package scale.clef;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This is an abstract class that implements a recursive descent visit
 * of a Clef AST class tree.
 * <p>
 * $Id: DescendPredicate.java,v 1.68 2007-10-04 19:58:02 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Sub-classes should implement override methods for the leaf nodes
 * that matter to them.
 * <p>
 * @see scale.clef
 */
public abstract class DescendPredicate implements Predicate
{
  /**
   * Visit all of the Node elements of a Vector.
   */
  public void visitChildren(Node parent)
  {
    int l = parent.numChildren();
    for (int i = 0; i < l; i++) {
      Node child = parent.getChild(i);
      if (child != null)
        child.visit(this);
    }
  }

  public void visitAbsoluteValueOp(AbsoluteValueOp n)                 { visitMonadicOp(n); }
  public void visitAdditionAssignmentOp(AdditionAssignmentOp n)       { visitCompoundAssignmentOp(n); }
  public void visitAdditionOp(AdditionOp n)                           { visitDyadicOp(n); }
  public void visitAddressLiteral(AddressLiteral n)                   { visitLiteral(n); }
  public void visitAddressOp(AddressOp n)                             { visitMonadicOp(n); }
  public void visitAggregateOp(AggregateOp n)                         { visitMonadicOp(n); }
  public void visitAggregateType(AggregateType n)                     { visitCompositeType(n); }
  public void visitAggregationElements(AggregationElements n)         { visitLiteral(n); }
  public void visitAllocArrayType(AllocArrayType n)                   { visitArrayType(n); }
  public void visitAllocatePlacementOp(AllocatePlacementOp n)         { visitHeapOp(n); }
  public void visitAllocateSettingFieldsOp(AllocateSettingFieldsOp n) { visitHeapOp(n); }
  public void visitAltCase(AltCase n)                                 { visitNode(n); }
  public void visitAndConditionalOp(AndConditionalOp n)               { visitDyadicOp(n); }
  public void visitAndOp(AndOp n)                                     { visitDyadicOp(n); }
  public void visitArithmeticIfStmt(ArithmeticIfStmt n)               { visitIfStmt(n); }
  public void visitArrayType(ArrayType n)                             { visitCompositeType(n); }
  public void visitAssignLabelStmt(AssignLabelStmt n)                 { visitStatement(n); }
  public void visitAssignSimpleOp(AssignSimpleOp n)                   { visitAssignmentOp(n); }
  public void visitAssignedGotoStmt(AssignedGotoStmt n)               { visitMultiBranchStmt(n); }
  public void visitAssignmentOp(AssignmentOp n)                       { visitDyadicOp(n); }
  public void visitAtomicType(AtomicType n)                           { visitType(n); }
  public void visitBitAndAssignmentOp(BitAndAssignmentOp n)           { visitCompoundAssignmentOp(n); }
  public void visitBitAndOp(BitAndOp n)                               { visitDyadicOp(n); }
  public void visitBitComplementOp(BitComplementOp n)                 { visitMonadicOp(n); }
  public void visitBitOrAssignmentOp(BitOrAssignmentOp n)             { visitCompoundAssignmentOp(n); }
  public void visitBitOrOp(BitOrOp n)                                 { visitDyadicOp(n); }
  public void visitBitShiftAssignmentOp(BitShiftAssignmentOp n)       { visitCompoundAssignmentOp(n); }
  public void visitBitShiftOp(BitShiftOp n)                           { visitDyadicOp(n); }
  public void visitBitXorAssignmentOp(BitXorAssignmentOp n)           { visitCompoundAssignmentOp(n); }
  public void visitBitXorOp(BitXorOp n)                               { visitDyadicOp(n); }
  public void visitBlockStmt(BlockStmt n)                             { visitStatement(n); }
  public void visitBooleanLiteral(BooleanLiteral n)                   { visitLiteral(n); }
  public void visitBooleanType(BooleanType n)                         { visitNumericType(n); }
  public void visitBound(Bound n)                                     { visitNode(n); }
  public void visitBreakStmt(BreakStmt n)                             { visitStatement(n); }
  public void visitCallFunctionOp(CallFunctionOp n)                   { visitCallOp(n); }
  public void visitCallOp(CallOp n)                                   { visitExpression(n); }
  public void visitCaseLabelDecl(CaseLabelDecl n)                     { visitLabelDecl(n); }
  public void visitCaseStmt(CaseStmt n)                               { visitStatement(n); }
  public void visitCharLiteral(CharLiteral n)                         { visitLiteral(n); }
  public void visitCharacterType(CharacterType n)                     { visitNumericType(n); }
  public void visitComplexLiteral(ComplexLiteral n)                   { visitLiteral(n); }
  public void visitComplexOp(ComplexOp n)                             { visitDyadicOp(n); }
  public void visitComplexType(ComplexType n)                         { visitNumericType(n); }
  public void visitCompositeType(CompositeType n)                     { visitType(n); }
  public void visitCompoundAssignmentOp(CompoundAssignmentOp n)       { visitAssignmentOp(n); }
  public void visitComputedGotoStmt(ComputedGotoStmt n)               { visitMultiBranchStmt(n); }
  public void visitContinueStmt(ContinueStmt n)                       { visitStatement(n); }
  public void visitDeclStmt(DeclStmt n)                               { visitStatement(n); }
  public void visitDeclaration(Declaration n)                         { visitNode(n); }
  public void visitDefOp(DefOp n)                                     { visitMonadicOp(n); }
  public void visitDeleteArrayOp(DeleteArrayOp n)                     { visitMonadicOp(n); }
  public void visitDeleteOp(DeleteOp n)                               { visitMonadicOp(n); }
  public void visitDereferenceOp(DereferenceOp n)                     { visitMonadicOp(n); }
  public void visitDivisionAssignmentOp(DivisionAssignmentOp n)       { visitCompoundAssignmentOp(n); }
  public void visitDivisionOp(DivisionOp n)                           { visitDyadicOp(n); }
  public void visitDoLoopStmt(DoLoopStmt n)                           { visitLoopStmt(n); }
  public void visitDyadicOp(DyadicOp n)                               { visitExpression(n); }
  public void visitEnumElementDecl(EnumElementDecl n)                 { visitDeclaration(n); }
  public void visitEnumerationType(EnumerationType n)                 { visitNumericType(n); }
  public void visitEqualityOp(EqualityOp n)                           { visitDyadicOp(n); }
  public void visitEquivalenceDecl(EquivalenceDecl n)                 { visitVariableDecl(n); }
  public void visitEvalStmt(EvalStmt n)                               { visitStatement(n); }
  public void visitExceptionDecl(ExceptionDecl n)                     { visitDeclaration(n); }
  public void visitExitStmt(ExitStmt n)                               { visitStatement(n); }
  public void visitExponentiationOp(ExponentiationOp n)               { visitDyadicOp(n); }
  public void visitExpression(Expression n)                           { visitNode(n); }
  public void visitExpressionIfOp(ExpressionIfOp n)                   { visitTernaryOp(n); }
  public void visitFieldDecl(FieldDecl n)                             { visitValueDecl(n); }
  public void visitFileDecl(FileDecl n)                               { visitDeclaration(n); }
  public void visitFixedArrayType(FixedArrayType n)                   { visitArrayType(n); }
  public void visitFloatArrayLiteral(FloatArrayLiteral n)             { visitLiteral(n); }
  public void visitFloatLiteral(FloatLiteral n)                       { visitLiteral(n); }
  public void visitFloatType(FloatType n)                             { visitRealType(n); }
  public void visitForLoopStmt(ForLoopStmt n)                         { visitTestLoopStmt(n); }
  public void visitFormalDecl(FormalDecl n)                           { visitVariableDecl(n); }
  public void visitForwardProcedureDecl(ForwardProcedureDecl n)       { visitProcedureDecl(n); }
  public void visitGotoStmt(GotoStmt n)                               { visitStatement(n); }
  public void visitGreaterEqualOp(GreaterEqualOp n)                   { visitDyadicOp(n); }
  public void visitGreaterOp(GreaterOp n)                             { visitDyadicOp(n); }
  public void visitHeapOp(HeapOp n)                                   { visitExpression(n); }
  public void visitIdAddressOp(IdAddressOp n)                         { visitIdReferenceOp(n); }
  public void visitIdReferenceOp(IdReferenceOp n)                     { visitExpression(n); }
  public void visitIdValueOp(IdValueOp n)                             { visitIdReferenceOp(n); }
  public void visitIfStmt(IfStmt n)                                   { visitStatement(n); }
  public void visitIfThenElseStmt(IfThenElseStmt n)                   { visitIfStmt(n); }
  public void visitIncompleteType(IncompleteType n)                   { visitType(n); }
  public void visitIncrementOp(IncrementOp n)                         { visitMonadicOp(n); }
  public void visitIntArrayLiteral(IntArrayLiteral n)                 { visitLiteral(n); }
  public void visitIntLiteral(IntLiteral n)                           { visitLiteral(n); }
  public void visitIntegerType(IntegerType n)                         { visitNumericType(n); }
  public void visitSignedIntegerType(SignedIntegerType n)             { visitIntegerType(n); }
  public void visitUnsignedIntegerType(UnsignedIntegerType n)         { visitIntegerType(n); }
  public void visitFortranCharType(FortranCharType n)                 { visitCompositeType(n); }
  public void visitLabelDecl(LabelDecl n)                             { visitDeclaration(n); }
  public void visitLabelStmt(LabelStmt n)                             { visitStatement(n); }
  public void visitLessEqualOp(LessEqualOp n)                         { visitDyadicOp(n); }
  public void visitLessOp(LessOp n)                                   { visitDyadicOp(n); }
  public void visitLiteral(Literal n)                                 { visitExpression(n); }
  public void visitLoopStmt(LoopStmt n)                               { visitStatement(n); }
  public void visitMaximumOp(MaximumOp n)                             { visitDyadicOp(n); }
  public void visitMinimumOp(MinimumOp n)                             { visitDyadicOp(n); }
  public void visitModulusOp(ModulusOp n)                             { visitDyadicOp(n); }
  public void visitMonadicOp(MonadicOp n)                             { visitExpression(n); }
  public void visitMultiBranchStmt(MultiBranchStmt n)                 { visitStatement(n); }
  public void visitMultiplicationAssignmentOp(MultiplicationAssignmentOp n) { visitCompoundAssignmentOp(n); }
  public void visitMultiplicationOp(MultiplicationOp n)               { visitDyadicOp(n); }
  public void visitNegativeOp(NegativeOp n)                           { visitMonadicOp(n); }
  public void visitNilOp(NilOp n)                                     { visitLiteral(n); }
  public void visitNode(Node n)                                       { visitChildren(n); }
  public void visitNotEqualOp(NotEqualOp n)                           { visitDyadicOp(n); }
  public void visitNotOp(NotOp n)                                     { visitMonadicOp(n); }
  public void visitNullStmt(NullStmt n)                               { visitStatement(n); }
  public void visitNumericType(NumericType n)                         { visitAtomicType(n); }
  public void visitOrConditionalOp(OrConditionalOp n)                 { visitDyadicOp(n); }
  public void visitOrOp(OrOp n)                                       { visitDyadicOp(n); }
  public void visitParenthesesOp(ParenthesesOp n)                     { visitMonadicOp(n); }
  public void visitPointerType(PointerType n)                         { visitAtomicType(n); }
  public void visitPositiveOp(PositiveOp n)                           { visitMonadicOp(n); }
  public void visitPostDecrementOp(PostDecrementOp n)                 { visitIncrementOp(n); }
  public void visitPostIncrementOp(PostIncrementOp n)                 { visitIncrementOp(n); }
  public void visitPreDecrementOp(PreDecrementOp n)                   { visitIncrementOp(n); }
  public void visitPreIncrementOp(PreIncrementOp n)                   { visitIncrementOp(n); }
  public void visitProcedureDecl(ProcedureDecl n)                     { visitRoutineDecl(n); }
  public void visitProcedureType(ProcedureType n)                     { visitType(n); }
  public void visitRaise(Raise n)                                     { visitNode(n); }
  public void visitRaiseWithObject(RaiseWithObject n)                 { visitRaise(n); }
  public void visitRaiseWithType(RaiseWithType n)                     { visitRaise(n); }
  public void visitRealType(RealType n)                               { visitNumericType(n); }
  public void visitRecordType(RecordType n)                           { visitAggregateType(n); }
  public void visitRefType(RefType n)                                 { visitType(n); }
  public void visitRemainderAssignmentOp(RemainderAssignmentOp n)     { visitCompoundAssignmentOp(n); }
  public void visitRemainderOp(RemainderOp n)                         { visitDyadicOp(n); }
  public void visitRenamedVariableDecl(RenamedVariableDecl n)         { visitVariableDecl(n); }
  public void visitRepeatUntilLoopStmt(RepeatUntilLoopStmt n)         { visitTestLoopStmt(n); }
  public void visitRepeatWhileLoopStmt(RepeatWhileLoopStmt n)         { visitTestLoopStmt(n); }
  public void visitReturnStmt(ReturnStmt n)                           { visitStatement(n); }
  public void visitRoutineDecl(RoutineDecl n)                         { visitDeclaration(n); }
  public void visitSelectIndirectOp(SelectIndirectOp n)               { visitAggregateOp(n); }
  public void visitSelectOp(SelectOp n)                               { visitAggregateOp(n); }
  public void visitSeriesOp(SeriesOp n)                               { visitDyadicOp(n); }
  public void visitSizeofLiteral(SizeofLiteral n)                     { visitLiteral(n); }
  public void visitStatement(Statement n)                             { visitNode(n); }
  public void visitStatementOp(StatementOp n)                         { visitMonadicOp(n); }
  public void visitStringLiteral(StringLiteral n)                     { visitLiteral(n); }
  public void visitSubscriptAddressOp(SubscriptAddressOp n)           { visitSubscriptOp(n); }
  public void visitSubscriptOp(SubscriptOp n)                         { visitExpression(n); }
  public void visitSubscriptValueOp(SubscriptValueOp n)               { visitSubscriptOp(n); }
  public void visitSubstringOp(SubstringOp n)                         { visitTernaryOp(n); }
  public void visitSubtractionAssignmentOp(SubtractionAssignmentOp n) { visitCompoundAssignmentOp(n); }
  public void visitSubtractionOp(SubtractionOp n)                     { visitDyadicOp(n); }
  public void visitSwitchStmt(SwitchStmt n)                           { visitStatement(n); }
  public void visitTernaryOp(TernaryOp n)                             { visitExpression(n); }
  public void visitTestLoopStmt(TestLoopStmt n)                       { visitLoopStmt(n); }
  public void visitThisOp(ThisOp n)                                   { visitExpression(n); }
  public void visitTranscendental2Op(Transcendental2Op n)             { visitDyadicOp(n); }
  public void visitTranscendentalOp(TranscendentalOp n)               { visitMonadicOp(n); }
  public void visitType(Type n)                                       { visitNode(n); }
  public void visitTypeConversionOp(TypeConversionOp n)               { visitMonadicOp(n); }
  public void visitTypeDecl(TypeDecl n)                               { visitDeclaration(n); }
  public void visitTypeName(TypeName n)                               { visitDeclaration(n); }
  public void visitUnionType(UnionType n)                             { visitRecordType(n); }
  public void visitUnknownFormals(UnknownFormals n)                   { visitFormalDecl(n); }
  public void visitVaArgOp(VaArgOp n)                                 { visitVarArgOp(n); }
  public void visitVaCopyOp(VaCopyOp n)                               { visitAssignmentOp(n); }
  public void visitVaEndOp(VaEndOp n)                                 { visitVarArgOp(n); }
  public void visitVaStartOp(VaStartOp n)                             { visitVarArgOp(n); }
  public void visitValueDecl(ValueDecl n)                             { visitDeclaration(n); }
  public void visitVarArgOp(VarArgOp n)                               { visitExpression(n); }
  public void visitVariableDecl(VariableDecl n)                       { visitValueDecl(n); }
  public void visitVoidType(VoidType n)                               { visitType(n); }
  public void visitWhileLoopStmt(WhileLoopStmt n)                     { visitTestLoopStmt(n); }
}
