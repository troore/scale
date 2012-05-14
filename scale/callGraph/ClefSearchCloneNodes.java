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
public class ClefSearchCloneNodes extends scale.clef.DescendPredicate
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

	public void visitDeclaration(Declaration n)
	{
		// Shorten search - do not call visitNode() because we don't need
		// to visit the children of most declarations.
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

	public void visitType(Type n)
	{
		// shorten search - avoid calling visitNode()
	}

	public void visitRoutineDecl(RoutineDecl rd)
	{
		if ((rd instanceof ForwardProcedureDecl)  || rd.isSpecification())
			return;

		cn = rd;

		Statement s = rd.getBody();

		if (s != null)
			s.visit(this);
	}

	public void visitIdReferenceOp(IdReferenceOp id) 
	{
	/*	Declaration decl = id.getDecl();
		if (decl.isRoutineDecl())
			cg.addFunction((RoutineDecl) decl);*/
	}

	public void visitCallOp(CallOp fun) 
	{
	/*	Expression ftn = fun.getRoutine();

		if (ftn instanceof IdAddressOp) {
			RoutineDecl rd = (RoutineDecl) ((IdReferenceOp) ftn).getDecl();
			cg.addCallee(cn, rd);
		} else { // call by function pointer
			ProcedureType pt = (ProcedureType) ftn.getPointedToCore();
			Type          rt = pt.getReturnType();
			cn.addCandidate(rt);
		}*/

		// Check if the arguments contain calls to other routines.

		int l = fun.getNumArgs();
		for (int i = 0; i < l; i++)
			fun.getArg(i).visit(this);
	}

	public void visitValueDecl(ValueDecl vd)
	{
		Expression exp = vd.getValue();

		if (exp != null)
			exp.visit(this);
	}

	public void visitAggregationElements(AggregationElements ag)
	{
		Vector<Object> ea = ag.getElementVector();
		int            l  = ea.size();
		for (int i = 0; i < l; i++) {
			Object x = ea.elementAt(i);
			if (x instanceof Node) {
				Node el = (Node) x;
				el.visit(this);
			}
		}
	}

	public void visitAddressLiteral(AddressLiteral al)
	{
	/*	Declaration d = al.getDecl();
		if (d != null)
			cg.addTopLevelDecl(d);*/
	}
}
