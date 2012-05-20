package scale.callGraph;

import java.io.*;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;
import scale.clef.symtab.*;

/**
 * This class is used to proceed some Clef AST's Loop Nodes which need clone operations.
 * $Id: ClefSearchCloneNodes.java, 2012-05-14	troore $
 */
public class ClefSearchCloneNodes extends scale.clef.SearchCloneNodesPredicate
{
	private CallGraph   cg; /* The call graph that is being built. */
	private RoutineDecl cn; /* Current call node */

	/**
	 * Search the to be operated Loop Nodes by scanning the Clef AST.
	 * @param cg the call graph which will be constructed
	 * @see CallGraph
	 */
	public ClefSearchCloneNodes (CallGraph cg)
	{
		this.cg = cg;

		Node root = cg.getAST();
		if (root == null)
			return;

		root.visit(this);
	}

	public void visitCloneForLoopStmt (CloneForLoopStmt n)
	{
		/*
		 * Modify the increment expression of this clone for statement
		 * according to its clnNum.
		 */
		long clnNum = n.getClnNum ();
		Expression exprInc = n.getExprInc ();

		if (exprInc instanceof AssignSimpleOp)
		{
			Expression rhs = ((AssignSimpleOp)exprInc).getRhs ();
			if (rhs instanceof AdditionOp)
			{
				Expression expr2 = ((AdditionOp)rhs).getExpr2 ();
				if (expr2 instanceof IntLiteral)
				{
					((IntLiteral)expr2).setValue ((long)clnNum);
				}
			}
		}
		else if ((exprInc instanceof PreIncrementOp) || (exprInc instanceof PostIncrementOp))
		{
			IdValueOp id = (IdValueOp)((PostIncrementOp)exprInc).getExpr ();
			Declaration decl = id.getDecl ();
			Type type = id.getType ();

			Expression e1 = new IdValueOp (decl);
			Expression e2 = LiteralMap.put ((long)clnNum, type);
			Expression rhs = new AdditionOp (type, e1, e2);
			Expression lhs = new IdAddressOp (PointerType.create (type), decl);
			Expression expr = new AssignSimpleOp (type, lhs, rhs);

			n.setExprInc (expr);
		}
		else
		{
		}

		/*
		 * Visit its statements of this for's BlockStmt and operate clone if necessary.
		 */
		new ClefClone (n);
	}

	public void visitFileDecl(FileDecl f)
	{
		// Get top level declarations. Necessary because of definition of
		// visitDeclaration().

		int l = f.getNumDecls();

		for (int i = 0; i < l; i++)
		{
			Declaration d = f.getDecl(i);

			if (d != null)
				d.visit(this);
		}
	}

}
