package scale.clef;

import scale.clef.stmt.*;

/**
 * The class for the visit pattern of Clef Statements.
 * <p>
 * $Id: StmtPredicate.java,v 1.18 2006-06-28 16:39:01 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see scale.clef.Predicate
 */

public interface StmtPredicate
{
  public void visitStatement(Statement n);
  public void visitBlockStmt(BlockStmt n);
  public void visitLoopStmt(LoopStmt n);
  public void visitTestLoopStmt(TestLoopStmt n);
  public void visitIfStmt(IfStmt n);
  public void visitMultiBranchStmt(MultiBranchStmt n);
  public void visitIfThenElseStmt(IfThenElseStmt n);
  public void visitArithmeticIfStmt(ArithmeticIfStmt n);
  public void visitComputedGotoStmt(ComputedGotoStmt n);
  public void visitAssignLabelStmt(AssignLabelStmt n);
  public void visitAssignedGotoStmt(AssignedGotoStmt n);
  public void visitAltCase(AltCase n);
  public void visitCaseStmt(CaseStmt n);
  public void visitSwitchStmt(SwitchStmt n);
  public void visitWhileLoopStmt(WhileLoopStmt n);
  public void visitRepeatWhileLoopStmt(RepeatWhileLoopStmt n);
  public void visitRepeatUntilLoopStmt(RepeatUntilLoopStmt n);
  public void visitDoLoopStmt(DoLoopStmt n);
  public void visitForLoopStmt(ForLoopStmt n);
  public void visitBreakStmt(BreakStmt n);
  public void visitContinueStmt(ContinueStmt n);
  public void visitGotoStmt(GotoStmt n);
  public void visitReturnStmt(ReturnStmt n);
  public void visitExitStmt(ExitStmt n);
  public void visitEvalStmt(EvalStmt n);
  public void visitDeclStmt(DeclStmt n);
  public void visitNullStmt(NullStmt n);
  public void visitLabelStmt(LabelStmt n);
}
