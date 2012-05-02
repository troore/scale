package scale.score.pred;

import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;

/**
 * This class is an implementation of the Score predicate interface used for the visit pattern.
 * <p>
 * $Id: Supertype.java,v 1.57 2007-03-21 13:32:13 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This predicate does not do anything useful itself but rather simply
 * calls up the class hierarchy.  This class may be useful for other
 * classes to extend.
 */
public class Supertype implements Predicate 
{
  public void visitAbsoluteValueExpr(AbsoluteValueExpr e) { visitUnaryExpr(e); }
  public void visitAdditionExpr(AdditionExpr e)     { visitBinaryExpr(e); }
  public void visitAllocateExpr(AllocateExpr e)     { visitUnaryExpr(e);  }
  public void visitAndExpr(AndExpr e)               { visitBinaryExpr(e); }
  public void visitBeginChord(BeginChord c)         { visitSequentialChord(c);}
  public void visitBinaryExpr(BinaryExpr e)         { visitExpr(e);       }
  public void visitBitAndExpr(BitAndExpr e)         { visitBinaryExpr(e); }
  public void visitBitComplementExpr(BitComplementExpr e) { visitUnaryExpr(e); }
  public void visitBitOrExpr(BitOrExpr e)           { visitBinaryExpr(e); }
  public void visitBitShiftExpr(BitShiftExpr e)     { visitBinaryExpr(e); }
  public void visitBitXorExpr(BitXorExpr e)         { visitBinaryExpr(e); }
  public void visitBranchChord(BranchChord c)       { visitChord(c);      }
  public void visitCallExpr(CallExpr e)             { visitNaryExpr(e);   }
  public void visitCallFunctionExpr(CallFunctionExpr e) { visitCallExpr(e); }
  public void visitCallMethodExpr(CallMethodExpr e) { visitCallExpr(e);   }
  public void visitChord(Chord c)                   { visitNote(c);       }
  public void visitCompareExpr(CompareExpr e)       { visitBinaryExpr(e); }
  public void visitComplexValueExpr(ComplexValueExpr e) { visitBinaryExpr(e); }
  public void visitConditionalExpr(ConditionalExpr e) { visitTernaryExpr(e);}
  public void visitConversionExpr(ConversionExpr e) { visitUnaryExpr(e);  }
  public void visitDecisionChord(DecisionChord c)   { visitChord(c);      }
  public void visitDivisionExpr(DivisionExpr e)     { visitBinaryExpr(e); }
  public void visitDualExpr(DualExpr e)             { visitExpr(e);       }
  public void visitEndChord(EndChord e)             { visitSequentialChord(e);}
  public void visitEqualityExpr(EqualityExpr e)     { visitBinaryExpr(e); }
  public void visitExitChord(ExitChord c)           { visitExprChord(c); }
  public void visitExponentiationExpr(ExponentiationExpr e) { visitBinaryExpr(e); }
  public void visitExpr(Expr e)                     { visitNote(e);       }
  public void visitExprChord(ExprChord c)           { visitSequentialChord(c);}
  public void visitExprPhiExpr(ExprPhiExpr e)       { visitPhiExpr(e);    }
  public void visitFieldExpr(FieldExpr e)           { visitUnaryExpr(e);  }
  public void visitGotoChord(GotoChord c)           { visitBranchChord(c);}
  public void visitGreaterEqualExpr(GreaterEqualExpr e) { visitBinaryExpr(e); }
  public void visitGreaterExpr(GreaterExpr e)       { visitBinaryExpr(e); }
  public void visitIfThenElseChord(IfThenElseChord c) { visitDecisionChord(c);}
  public void visitLeaveChord(LeaveChord c)         { visitEndChord(c);   }
  public void visitLessEqualExpr(LessEqualExpr e)   { visitBinaryExpr(e); }
  public void visitLessExpr(LessExpr e)             { visitBinaryExpr(e); }
  public void visitLiteralExpr(LiteralExpr e)       { visitValueExpr(e);  }
  public void visitLoadDeclAddressExpr(LoadDeclAddressExpr e) { visitLoadExpr(e); }
  public void visitLoadDeclValueExpr(LoadDeclValueExpr e) { visitLoadExpr(e); }
  public void visitLoadExpr(LoadExpr e)             { visitExpr(e);       }
  public void visitLoadFieldAddressExpr(LoadFieldAddressExpr e) { visitFieldExpr(e);  }
  public void visitLoadFieldValueExpr(LoadFieldValueExpr e) { visitFieldExpr(e);  }
  public void visitLoadValueIndirectExpr(LoadValueIndirectExpr e) { visitUnaryExpr(e);}
  public void visitLoopExitChord(LoopExitChord c)   { visitSequentialChord(c);}
  public void visitLoopHeaderChord(LoopHeaderChord c) { visitSequentialChord(c);}
  public void visitLoopPreHeaderChord(LoopPreHeaderChord c) { visitSequentialChord(c);}
  public void visitLoopTailChord(LoopTailChord c)   { visitSequentialChord(c);}
  public void visitLoopInitChord(LoopInitChord c)   { visitSequentialChord(c);}
  public void visitMarkerChord(MarkerChord c)       { visitSequentialChord(c);}
  public void visitMaxExpr(MaxExpr e)               { visitBinaryExpr(e); }
  public void visitMinExpr(MinExpr e)               { visitBinaryExpr(e); }
  public void visitMultiplicationExpr(MultiplicationExpr e) { visitBinaryExpr(e); }
  public void visitNaryExpr(NaryExpr e)             { visitExpr(e);       }
  public void visitNegativeExpr(NegativeExpr e)     { visitUnaryExpr(e);  }
  public void visitNilExpr(NilExpr e)               { visitValueExpr(e);  }
  public void visitNotEqualExpr(NotEqualExpr e)     { visitBinaryExpr(e); }
  public void visitNotExpr(NotExpr e)               { visitUnaryExpr(e);  }
  public void visitNote(Note n)                     { /* Do nothing! */   }
  public void visitNullChord(NullChord c)           { visitSequentialChord(c); }
  public void visitOrExpr(OrExpr e)                 { visitBinaryExpr(e); }
  public void visitPhiExpr(PhiExpr e)               { visitNaryExpr(e);   }
  public void visitPhiExprChord(PhiExprChord c)     { visitExprChord(c);  }
  public void visitRemainderExpr(RemainderExpr e)   { visitBinaryExpr(e); }
  public void visitReturnChord(ReturnChord c)       { visitLeaveChord(c); }
  public void visitSequentialChord(SequentialChord c) { visitChord(c);    }
  public void visitSubscriptExpr(SubscriptExpr e)   { visitExpr(e);       }
  public void visitArrayIndexExpr(ArrayIndexExpr e) { visitTernaryExpr(e);}
  public void visitSubtractionExpr(SubtractionExpr e) { visitBinaryExpr(e); }
  public void visitSwitchChord(SwitchChord c)       { visitDecisionChord(c); }
  public void visitTernaryExpr(TernaryExpr e)       { visitExpr(e); }
  public void visitTranscendentalExpr(TranscendentalExpr e) { visitUnaryExpr(e);  }
  public void visitTranscendental2Expr(Transcendental2Expr e) { visitBinaryExpr(e); }
  public void visitUnaryExpr(UnaryExpr e)           { visitExpr(e);       }
  public void visitVaArgExpr(VaArgExpr e)           { visitVarArgExpr(e); }
  public void visitVaEndExpr(VaEndExpr e)           { visitVarArgExpr(e); }
  public void visitVaStartExpr(VaStartExpr e)       { visitVarArgExpr(e); }
  public void visitValueExpr(ValueExpr e)           { visitExpr(e);       }
  public void visitVarArgExpr(VarArgExpr e)         { visitExpr(e);       }
  public void visitVectorExpr(VectorExpr e)         { visitNaryExpr(e);   }
}
