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
 * This class is used to visit Clef AST's CloneForLoop and do clone operations to its statements.
 * $Id: ClefCloneFor.java, 2012-05-16	troore $
 */
public class ClefClone extends scale.clef.SpecialClonePredicate
{
	private CloneTestLoopStmt	cls;	/* The For Loop Statement that is being cloned. */
	private SymtabScope			saveScope;
	private SymtabScope			cntScope;
	private int					sn;			/* Slice number. */	
	private int					cn;			/* Clone number. */	
	private String				loopVar;

	/**
	 * Clone children of this Clone For Loop by scanning the CloneForLoopStmt hang on Clef AST.
	 * @param forloop the CloneForLoopStmt which will be constructed
	 * @see CloneForLoopStmt
	 */
	public ClefClone (CloneTestLoopStmt cls)
	{
		this.cls = cls;
		this.saveScope = cls.getScope ();
		this.cntScope = saveScope;
		this.sn = cls.getClnNum ();
		this.cn = cls.getSlcNum ();
		this.loopVar = cls.getLoopVarName ();

		Statement root = cls.getStmt ();

		if (root == null)
			return;
		if (!(root instanceof BlockStmt))
		{
		}

		incBlockStmt ((BlockStmt)root);
	}

	public void incBlockStmt (BlockStmt n)
	{
		int l = n.numChildren ();
		boolean flag = true;

		saveScope = cntScope;
		cntScope = n.getScope ();

		int index = 0;
		for (int i = 0; i < l; i++)
		{
			Statement child = (Statement)n.getChild (index);
			if (child instanceof DeclStmt)
			{
				for (int j = 1; j < cn; j++)
				{
					Statement dup = (Statement)child.copy (this, j);
					n.addDeclStmt ((DeclStmt)dup);
				}
				index = index + 1;
			}
			else
			{
				if (flag)
				{
					index = index + i * (cn - 1);
					flag = false;
				}
				if (child instanceof ForLoopStmt)
				{
					Expression initExpr = ((ForLoopStmt)child).getExprInit ();
					Expression incExpr = ((ForLoopStmt)child).getExprInc ();

					Expression minuendExpr = ((LessOp)incExpr).getExpr2 ();
					Expression subtrahendExpr = ((LessOp)initExpr).getExpr2 ();
					Expression diffExpr = new SubtractionOp (minuendExpr.getType (), minuendExpr, subtrahendExpr);
					Type type = diffExpr.getType ();

					Expression snExpr = LiteralMap.put ((long)sn, type);
					Expression slcExpr = new DivisionOp (type, diffExpr, snExpr);
					
					ForLoopStmt cntForLoop = (ForLoopStmt)child;
					for (int j = 0; j < sn; j++)
					{
						cntForLoop = cntForLoop.copy (this, slcExpr, j);	
						if (index == (n.numChildren () - 1))
						{
							n.addStmt (cntForLoop);
						}
						else
						{
							n.insertStmt (cntForLoop, index + j);
						}
					}
					/** remove the "child" just being copied from current block. **/
					n.removeStmt (index);
					/** get index of the next statement being copied. **/
					index += (sn - 1);
				}
				else
				{
					for (int j = 1; j < cn; j++)
					{
						Statement dup = (Statement)child.copy (this, j);
						if (index == (n.numChildren () - 1))
						{
							n.addStmt (dup);
						}
						else
						{
							n.insertStmt (dup, index + j);
						}
					}
					index += (cn - 1);
				}
			}
		}

		cntScope = saveScope;
	}

	/**
	 * Clone a local variable.
	 */
	public DeclStmt copyDeclStmt (DeclStmt n, int i)
	{
		Declaration decl = (VariableDecl)n.getDecl ();

		Declaration decli = decl.copy (this, i);

		return new DeclStmt (decli);
	}

	public ForLoopStmt copyForLoopStmt (ForLoopStmt n, Expression slcExpr, int i)
	{
		Expression initExpr = n.getExprInit ();
		Expression testExpr = n.getExpr ();
		Expression incExpr = n.getExprInc ();
		Type type = initExpr.getType ();

		Expression lhs = ((AssignmentOp)initExpr).getExpr2 ();
		Expression rhs = ((DyadicOp)testExpr).getExpr2 ();

		AdditionOp newRhs = new AdditionOp (type, rhs, slcExpr);
		LessOp newTestExpr = new LessOp (type, lhs, newRhs);

		BlockStmt bs = (BlockStmt)n.getStmt ();
		if (i == 0)
		{
			incBlockStmt (bs);
		}

		return new ForLoopStmt (bs, initExpr, newTestExpr, incExpr);
	}

	public EvalStmt copyEvalStmt (EvalStmt n, int i)
	{
		Expression expr = n.getExpr ();
		Expression copyExpr = expr.copy (this, i);

		return new EvalStmt (copyExpr);
	}

	public AssignSimpleOp copyAssignSimpleOp (AssignSimpleOp n, int i)
	{
		Expression expr1 = n.getExpr1 ();
		Expression expr2 = n.getExpr2 ();
		Type type = n.getType ();

		Expression copyExpr1 = expr1.copy (this, i);
		Expression copyExpr2 = expr2.copy (this, i);

		return new AssignSimpleOp (type, copyExpr1, copyExpr2);
		
	}

	public IdAddressOp copyIdAddressOp (IdAddressOp n, int i)
	{
		Declaration decl = n.getDecl ();
		Type type = decl.getType ();
		Declaration copyDecl = decl.copy (this, i);

		return new IdAddressOp (PointerType.create (type), copyDecl);
	}

	public Expression copyIdValueOp (IdValueOp n, int i)
	{
		Declaration decl = n.getDecl ();
		Type type = decl.getType ();

		if (decl.getName () == loopVar)
		{
			Expression expr1 = new IdValueOp (decl);
			Expression expr2 = LiteralMap.put ((long)i, type);
			Expression addExpr = new AdditionOp (type, expr1, expr2);

			return addExpr;
		}

		return n;
	}

	public VariableDecl copyVariableDecl (VariableDecl n, int i)
	{
		String name = n.getName ();
		Type type = n.getType ();

		if (cntScope.lookupSymbol ((Declaration)n) != null)
		{
			String namei = name + i;
			boolean isCopyed = true;
			Vector<SymtabEntry> syms = cntScope.lookup (namei);

			Declaration d = null;
			if (syms == null)
			{
				isCopyed = false;
			}
			else
			{
				int l = syms.size();
				for (int k = 0; k < l; k++)
				{
					SymtabEntry entry = syms.elementAt (k);
					d = entry.getDecl ();
					isCopyed = (d.getName () == namei);
					if (isCopyed)
					{
						break;
					}
				}
			}
			if (isCopyed == false)
			{
				VariableDecl decli = new VariableDecl (namei, type);
				decli.setVisibility (n.visibility());

				cntScope.addEntry (decli);

				return decli;
			}
			else
			{
				return (VariableDecl)d;
			}
		}

		return n;
	}
}
