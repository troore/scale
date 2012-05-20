package scale.clef;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This is an abstract class that implements a recursive descent visit
 * of a CloneForLoopStmt.
 * 
 * $Id: SpecialForPredicate.java, 2012-05-16 troore $
 *
 * Sub-classes should implement override methods for the leaf nodes
 * that matter to them.
 * 
 */
public abstract class SpecialClonePredicate
{
//	public Node copyNode (Node n, int i) { return null; }
	public Statement copyStatement (Statement n, int i) { return null; }
	public Expression copyExpression (Expression n, int i) { return null; }
	public Declaration copyDeclaration (Declaration n, int i) { return null; }

	public DeclStmt copyDeclStmt (DeclStmt n, int i) { return null; }
	public EvalStmt copyEvalStmt (EvalStmt n, int i) { return null; }
	public VariableDecl copyVariableDecl (VariableDecl n, int i) { return null; }
	public AssignSimpleOp copyAssignSimpleOp (AssignSimpleOp n, int i) { return null; }
	public IdAddressOp copyIdAddressOp (IdAddressOp n, int i) { return null; }
	public Expression copyIdValueOp (IdValueOp n, int i) { return null; }
	public ForLoopStmt copyForLoopStmt (ForLoopStmt n, Expression slcExpr, int i) { return null; }
}
