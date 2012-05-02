package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.decl.FormalDecl;
import scale.clef.type.*;

import scale.score.*;
import scale.score.analyses.*;
import scale.score.dependence.AffineExpr;

/**
 * This class represents the value of a declaration.
 * <p>
 * $Id: LoadDeclValueExpr.java,v 1.96 2007-10-04 19:58:31 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The type of an IdAddressOp is always the same as the declaration type.
 */
public class LoadDeclValueExpr extends LoadExpr
{
  public LoadDeclValueExpr(Declaration decl)
  {
    super(decl.getType(), decl);
  }

  /**
   * Make a copy of this load expression.
   * The use - def information is copied too.
   */
  public Expr copy()
  {
    LoadDeclValueExpr ld     = new LoadDeclValueExpr(getDecl());
    MayUse            mayUse = getMayUse();
    if (mayUse != null)
      mayUse.copy(ld);
    ld.setUseDef(getUseDef());
    return ld;
  }

  /**
   * Make a copy of this load expression without the use - def
   * information.
   */
  public Expr copyNoUD()
  {
    LoadDeclValueExpr ld = new LoadDeclValueExpr(getDecl());
    return ld;
  }

  public void visit(Predicate p)
  {
    p.visitLoadDeclValueExpr(this);
  }

  public String getDisplayLabel()
  {
    return getName();
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   * @see scale.clef.decl.ValueDecl
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return r;

    Literal rr = Lattice.Top;
    if (getDecl().addressTaken())
      rr = Lattice.Bot;
    else {
      MayUse mu  = getMayUse();
      scale.score.chords.ExprChord ud  = getUseDef();
      Literal udr = Lattice.Top;
      Literal mur = Lattice.Top;

      if (ud != null) {
        Type ty = getType();
        setType(VoidType.type);
        udr =  ud.getRValue().getConstantValue(cvMap);
        setType(ty);
      } else {
        udr =  getDecl().getConstantValue();
        // We don't want to go moving aggregates around.
        if (udr instanceof scale.clef.expr.AggregationElements)
          udr = Lattice.Bot;
      }

      if ((mu != null) && !scale.score.trans.Optimization.unsafe)
        rr = Lattice.Bot;
      else
        rr  = udr;
    }

    cvMap.put(this, rr);
    return rr;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   * @see scale.clef.decl.ValueDecl
   */
  public Literal getConstantValue()
  {
    if (getDecl().addressTaken())
      return Lattice.Bot;

    Literal rr = getDecl().getConstantValue();

    // We don't want to go moving aggregates around.

    if (rr instanceof scale.clef.expr.AggregationElements)
      rr = Lattice.Bot;

    return rr;
  }

  /**
   * Determine the coefficent of a linear expression. 
   * @param indexVar is the index variable associated with the
   * coefficient.
   * @param thisLoop is the loop containing the index variable
   * reference
   * @return the coefficient value.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {   
    // If this identifier represents the same identifier that we
    // are looking for then return 1. Otherwise, return 0.  This
    // has the effect of singling out a particular term from the
    // linear expression.

    Declaration decl = getDecl();
    if (!decl.isVariableDecl())
      return 0;

    VariableDecl vd = (VariableDecl) decl;
    if (vd.getOriginal() == indexVar)
      return 1;

    // If this is an induction variable then just return 0;
    // we don't want to follow the right-hand side of the definition.

    scale.score.chords.LoopHeaderChord curLoop = this.getLoopHeader();

    if ((curLoop != null) && (curLoop.getNestedLevel() >= thisLoop.getNestedLevel())) {
      do {
        if (curLoop.isPrimaryLoopIndex(vd))
          return 0;

        if (curLoop == thisLoop)
          break;
        curLoop = curLoop.getParent();
      } while (curLoop != null);
    }

    // At this point we have a non-induction variable load.

    scale.score.chords.ExprChord se = getUseDef();
    if (se == null)
      return 0;

    Expr r = se.getRValue();
    return r.findLinearCoefficient(indexVar, thisLoop);
  }

  protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>          affines,
                                               scale.score.chords.LoopHeaderChord thisLoop)
  {
    VariableDecl vd = (VariableDecl) getDecl();
    if (vd.addressTaken() && thisLoop.loopContainsCall())
      return AffineExpr.notAffine;

    scale.score.chords.LoopHeaderChord curLoop = this.getLoopHeader();

    if ((curLoop != null) && (curLoop.getNestedLevel() >= thisLoop.getNestedLevel())) {
      do {
        if (curLoop.isPrimaryLoopIndex(vd))
          // Need to check if index variable of a loop enclosing the
          // loop we are interested in: 'loop'. Otherwise this is just
          // a varaiable.
          return new AffineExpr(vd, curLoop);

        if (curLoop == thisLoop)
          break;
        curLoop = curLoop.getParent();
      } while (curLoop != null);
    }

    // If this expression is constant (invariant in all the scopes), then
    // it is linear.

    if (thisLoop.isInvariant(this))
      return new AffineExpr(vd, curLoop);

    if (!thisLoop.isLoopIndex(vd))
      return AffineExpr.notAffine;

    scale.score.chords.ExprChord se = getUseDef();
    if (se == null)
      return new AffineExpr(vd, curLoop);

    Expr       r  = se.getRValue();
    AffineExpr ae = r.getAffineExpr(affines, thisLoop);
    return (ae == null) ? AffineExpr.notAffine : ae;
  }

  /**
   * Return the Chord with the highest label value from the set of
   * Chords that must be executed before this expression.  Return 0 if
   * none found.
   * @param lMap is used to memoize the expression to critical Chord
   * information
   * @param independent is returned if the expression is not dependent
   * on anything
   */
  protected scale.score.chords.Chord findCriticalChord(HashMap<Expr, scale.score.chords.Chord> lMap,
                                                       scale.score.chords.Chord                independent)
  {
    Declaration decl = getDecl();
    Type        type = decl.getType();

    if (decl.isVirtual() || type.isVolatile() || !type.isAtomicType() || decl.addressTaken())
      return getChord(); // No need to move these.

    if (type.isConst())
      return independent;

    MayUse mayUse = getMayUse();

    if ((mayUse != null) && !scale.score.trans.Optimization.unsafe)
      return getChord();

    scale.score.chords.ExprChord useDef = getUseDef();
    if ((useDef == null) && !(decl instanceof scale.clef.decl.FormalDecl)) {
      if (decl.isGlobal())
        return getChord();
    }

    if (useDef != null)
      return useDef;
 
    return independent;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    VariableDecl decl = (VariableDecl) getDecl();
    return decl.optimizationCandidate();
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    VariableDecl vd = (VariableDecl) getDecl();
    if (vd.shouldBeInRegister())
      return 0;
    return 5;
  }

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    VariableDecl vd = getDecl().returnVariableDecl();
    if ((vd == null) || vd.isNotSSACandidate())
      return false; // No easy way to determine if a global is loop invariant.

    // Find definition point.

    MayUse mayUse = getMayUse();
    if (mayUse != null) {
      MayDef maydef = mayUse.getMayDef();
      if (maydef != null) {
        if (!isUDLoopInvariant(maydef.getGraphNode(), loop))
          return false;
      }
    }

    Note ud = getUseDef();
    return isUDLoopInvariant(ud, loop);
  }

  private boolean isUDLoopInvariant(Note ud, scale.score.chords.LoopHeaderChord loop)
  {
    if (ud == null)
      return true;

    if (ud instanceof CallExpr)
      return false;

    if (!(ud instanceof scale.score.chords.ExprChord))
      return false;

    scale.score.chords.ExprChord       c  = (scale.score.chords.ExprChord) ud;
    scale.score.chords.LoopHeaderChord lh = c.getLoopHeader();
    if (lh == null) // The node may not yet be linked - be conservative.
      return false;

    if (lh != loop)
      return true;

    // The definition is in the loop.

    return loop.isInvariant(c.getLValue()) &&
           loop.isInvariant(c.getRValue());
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return getType().isAtomicType() && !getDecl().isGlobal();
  }
}
