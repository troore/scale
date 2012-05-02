package scale.score.analyses;

import scale.common.*;
import scale.alias.*;
import scale.alias.steensgaard.*;
import scale.annot.*;
import scale.clef.decl.Declaration;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.score.pred.*;

/**
 * This class visits nodes and places information, at specific nodes,
 * which is used to represent aliases and indirect operations in SSA
 * form.
 * <p>
 * $Id: PlaceIndirectOpsSteen.java,v 1.23 2007-10-04 19:58:21 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * We expect that alias analysis has been performed before we add information
 * for indirection operations to the SSA form.
 * <p>
 * For now, we expect that Steensgaard's algorithm is used to create
 * alias groups.  An alias group is a collection of variables (and
 * access paths) that contain the same alias relationships.
 */
public class PlaceIndirectOpsSteen extends PlaceIndirectOps
{
  /**
   * Construct an object to place special annotations in a Scribble graph
   * so that we can correctly handle indirect operations and aliases.
   */
  public PlaceIndirectOpsSteen(Aliases aliases)
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
    AliasVar av = getAliasVar(e);

    assert (av != null) : "LoadDeclValue should have alias var - " + e;

    if (av.isAlias() && e.isScalar()) // Add a Mu annotation.
      addMayUse(e, ((TypeVar) av).getECR());
  }

  /**
   * We add a Mu operator to a use of an indirect variable.  Note that
   * we handle assignments through indirect variables elsewhere.  The Mu
   * operator represents any variable that may be used via the indirect
   * variable. 
   * <p>
   * A {@link scale.score.expr.LoadValueIndirectExpr LoadValueIndirectExpr}
   * represents a <tt>*expr</tt> operation.
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

    // Get the ECR that this one points-to.

    addMayUse(e, ((LocationType) ((TypeVar) av).getECR().getType()).getLocation());
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

      // We want the alias variable for the variable - not the expression.
      AliasVar av = getAliasVar(arg);
      if (av != null)
        e.addMayDef(createMayDefInfo(((TypeVar) av).getECR()));
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

    Expr lhs  = c.getLValue(); // Use the lowered version by default.;

    if (lhs == null)
      return;

    // Add a Chi annotation if the LHS is an alias

    AliasVar av = getAliasVar(lhs);
    if (av == null)
      return;

    lhs = lhs.getLow();

    ECR ecr = null;
    if ((lhs instanceof LoadDeclAddressExpr) && av.isAlias()) {
      // Base case when lhs is a simple variable reference.
      ecr = ((TypeVar) av).getECR();
    } else if (lhs instanceof LoadDeclValueExpr) {
      // A dereference (*) - get the ECR we point to. we assume its an alias because
      // it has to point to something!
      ecr = ((LocationType) ((TypeVar) av).getECR().getType()).getLocation();
    } else if ((lhs instanceof FieldExpr) ||
               (lhs instanceof SubscriptExpr) ||
               (lhs instanceof ArrayIndexExpr)) {
      Expr object = lhs.getReference();
      if (object instanceof LoadDeclAddressExpr) 
        ecr = ((TypeVar) av).getECR();
      else
        ecr = ((LocationType) ((TypeVar) av).getECR().getType()).getLocation();
    }

    // Check if the lhs is an alias - if so then add a may definition.

    if (ecr != null) { 
      c.addMayDef(createMayDefInfo(ecr));
    }
  }

  //
  // These methods don't have an implementation - we just don't want to
  // default to the visitExpr case.
  //
  
  /** 
   * Don't do anything here - but we don't want to default to visitExpr
   */
  public void visitFieldExpr(FieldExpr e)
  {
    // don't do anything here - but we don't want to default to visitExpr
  }

  /**
   * We don't need to do anything at a subscript expression.
   * So we just leave this function undefined - otherwise
   * the <tt>visitExpr</tt> method is called.
   */
  public void visitSubscriptExpr(SubscriptExpr e)
  {
    // don't do anything here.
    // we need this function, otherwise we default to the visitExpr case
  }

  /**
   * Don't do anything here - but we don't want to default to visitExpr
   * A Dual expression does not effect the placement of indirect operators.
   */
  public void visitDualExpr(DualExpr e)
  {
    // don't do anything here.
    // we need this function, otherwise we default to the visitExpr case
  }
}
