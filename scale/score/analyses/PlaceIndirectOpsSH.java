package scale.score.analyses;

import scale.common.*;
import scale.alias.*;
import scale.alias.steensgaard.*;
import scale.alias.shapirohorowitz.*;
import scale.annot.*;
import scale.clef.decl.Declaration;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.score.pred.*;

/**
 * This class visits nodes and places information, at specific nodes, which is
 * used to represent aliases and indirect operations in SSA form.
 * <p>
 * $Id: PlaceIndirectOpsSH.java,v 1.24 2007-10-04 19:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class visits nodes and places information at specific nodes which is
 * used to represents aliases and indirect operations in SSA form.
 * <p>
 * We expect that alias analysis has been performed before we add information
 * for indirection operations to the SSA form.
 * <p>
 * For now, we expect that Steensgaard's algorithm is used to create alias groups.
 * An alias group is a collection of variables (and access paths) that contain
 * the same alias relationships.
 */
public class PlaceIndirectOpsSH extends PlaceIndirectOps
{
  /**
   * Construct an object to place special annotations in a Scribble graph
   * so that we can correctly handle indirect operations and aliases.
   */
  public PlaceIndirectOpsSH(Aliases aliases)
  {
    super(aliases);
  }

  /**
   * Add annotations to handle simple variable references that are
   * aliases.  We add a Mu operator for a use.  Definitions are
   * handled by Chi operators that are added to the <tt>ExprChord</tt>
   * expression.
   */
  public void visitLoadDeclValueExpr(LoadDeclValueExpr e) 
  {
    AliasVar av  = getAliasVar(e);
    ECR      ecr = ((TypeVar) av).getECR();

    if (av.isAlias() && e.isScalar())
      addMayUse(e, ecr); // add a Mu annotation.
  }

  /**
   * We add a Mu operator to a use of an indirect variable.  Note that
   * we handle assignments through indirect variables elsewhere.  The Mu
   * operator represents any variable that may be used via the indirect
   * variable. 
   * <p>
   * A <i>LoadValueIndirectExpr</i> represents a <tt>*expr</tt> operation.
   *
   * @see LoadValueIndirectExpr
   * @see scale.score.expr.LoadValueIndirectExpr
   */
  public void visitLoadValueIndirectExpr(LoadValueIndirectExpr e)
  {
    if (e.isDefined())
      return;

    Note no = e.getOutDataEdge();
    if (!(no instanceof ExprChord)) // check that this node is involved in an assignment
      return;

    if (((ExprChord) no).getLValue() instanceof LoadDeclAddressExpr)
      return;

    // Add a Mu annotation

    AliasVar av = getAliasVar(e);

    assert (av != null) : "LoadValueIndirect should have alias var - " + e;

    // Get the location vector that this one points-to.

    Vector<ECR> vc  = ((LocationTypeCat) ((TypeVar) av).getECR().getType()).getLocations();
    ECR         ecr = null;
    VirtualVar  v   = null;
    int         l   = vc.size();

    for (int i = 0; i < l; i++) {
      ecr = vc.elementAt(i);
      if (ecr.getType() != AliasType.BOT) {
        v = aliases.getVirtualVar(ecr);
        break;
      }
    }

    if (v != null) { // Get the superset virtual variable of this variable.
      VirtualVar supvv  = ((SubVirtualVar) v).getSuperset();
      addMayUse(e, supvv);
    }
  }

  /**
   * We add Chi and Mu operators to routine calls to mark the use and
   * definition of variables accross calls.  In the absence of MOD/REF
   * information, we must assume that a function call defines and uses any
   * global variable.  Also, we must assume that the function call defines
   * any parameter that is passed by address.
   */
  public void visitCallExpr(CallExpr e)
  {
    int l = e.numArguments();

    for (int i = 0; i < l; i++) {
      Expr arg = e.getArgument(i);
      AliasVar av  = getAliasVar(arg);

      if (av == null)
        continue;

      if (arg.getReference() instanceof LoadDeclAddressExpr) {
        // e.g., &a
        VirtualVar v = aliases.getVirtualVar( ((TypeVar) av).getECR());
        e.addMayDef(createMayDefInfo(v));
      } else {
        // all other cases - add a maydef for the locations it points to
        definePointers(e, av);
      }
    }
  }

  /**
   * At a call expression, create maydefs for all the objects that
   * this alias variable points to.  
   * @param ce the call expression we attach the may def info to
   * @param t the alias info
   */
  private void definePointers(CallExpr ce, AliasVar tv)
  {
    Vector<ECR> v = new Vector<ECR>();
    ECR.nextVisit();

    tv.allPointsTo(v);

    int l = v.size();
    for (int i = 0; i < l; i++) {
      ECR        e  = v.elementAt(i);
      VirtualVar vv = aliases.getVirtualVar(e);
      ce.addMayDef(createMayDefInfo(vv));
    }
  }

  /**
   * We add a Chi operator to store operations that assign to indirect
   * variables.  The Chi operator represents any variable that may be
   * defined by the store (via the indirect variable). 
   */
  public void visitExprChord(ExprChord c)
  {
    visitChord(c);

    Expr lhs  = c.getLValue(); // Use the lowered version by default.

    if (lhs == null)
      return;

    // Add a Chi annotation if the LHS is an alias

    AliasVar av = getAliasVar(lhs);
    if (av == null)
      return;

    lhs = lhs.getLow();

    if ((lhs instanceof LoadDeclAddressExpr)) {
      if (!av.isAlias())
        return;
      // base case when lhs is a simple variable reference
      ECR ecr = ((TypeVar) av).getECR();
      if (ecr != null) {
        VirtualVar v = aliases.getVirtualVar(ecr);
        assert (v != null) : "Where is the virtual variable for the ECR: " + ecr;
        c.addMayDef(createMayDefInfo(v));
      }
      return;
    }

    if (lhs instanceof LoadDeclValueExpr) {
      // A dereference (*) - get the ECR we point to. We assume its an alias because
      // it has to point to something!
        
      // get the location vector that this one points-to

    } else if ((lhs instanceof FieldExpr) ||
               (lhs instanceof SubscriptExpr) ||
               (lhs instanceof ArrayIndexExpr)) {
      Expr object = lhs.getReference();
      if (object instanceof LoadDeclAddressExpr) {
        ECR ecr = ((TypeVar) av).getECR();
	if (ecr != null) {
	  VirtualVar v = aliases.getVirtualVar(ecr);
	  assert (v != null) : "Where is the virtual variable for the ECR: " + ecr;
	  c.addMayDef(createMayDefInfo(v));
	}
	return;
      }
    }

    Vector<ECR> vc = ((LocationTypeCat) ((TypeVar) av).getECR().getType()).getLocations();
    int         l  = vc.size();
    VirtualVar  v  = null;
    for (int i = 0; i < l; i++) {
      ECR ecr = vc.elementAt(i);
      if (ecr.getType() != AliasType.BOT) {
	v = aliases.getVirtualVar(ecr);
	assert (v != null) : "Where is the virtual variable for the ECR: " + ecr;
	break;
      }
    }

    if (v != null) {
      // get the superset virtual variable of this variable
      VirtualVar supvv = ((SubVirtualVar) v).getSuperset();
      c.addMayDef(createMayDefInfo(supvv));
    }
  }
 
  /** 
   *
   */
  public void visitFieldExpr(FieldExpr e)
  {
    // Don't do anything here - we don't want to default to visitExpr.
  }

  /**
   * We don't need to do anything at a subscript expression.
   * So we just leave this function undefined - otherwise
   * the <tt>visitExpr</tt> method is called.
   */
  public void visitSubscriptExpr(SubscriptExpr e)
  {
    // Don't do anything here.
    // We need this function, otherwise we default to the visitExpr case.
  }

  /**
   * Don't do anything here - but we don't want to default to visitExpr
   * A field offset expression does not effect the placement of 
   * indirect operators.
   */
  public void visitDualExpr(DualExpr e)
  {
    // Don't do anything here.
    // We need this function, otherwise we default to the visitExpr case.
  }
}
