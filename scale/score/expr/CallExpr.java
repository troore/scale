package scale.score.expr;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.*;
import scale.score.Note;
import scale.score.analyses.*;
import scale.score.pred.References;

/**
 * This is the base class for calls to routines.
 * <p>
 * $Id: CallExpr.java,v 1.46 2007-10-04 19:58:28 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class CallExpr extends NaryExpr
{
  private Vector<MayUse> mayUse;      // May use information - due to aliasing.
  private Vector<MayDef> mayDef;      // May def information - due to aliasing.
  private int            profCallCnt; // The number of times the this call occurred - supplied by profiling.

  /**
   * We may want to add a constructor that accepts the routine as a
   * Declaration node.  
   * @param type result type of the routine call.  Void for procedures.
   * @param routine expression node holding address of routine to call
   * @param arguments vector of expressions representing arguments
   */
  public CallExpr(Type type, Expr routine, Vector<Expr> arguments)
  {
    super(type, arguments);
    arguments.insertElementAt(routine, 0);

    routine.setOutDataEdge(this);

    this.mayUse      = null;
    this.mayDef      = null;
    this.profCallCnt = -1;
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | SE_STATE;
  }

  /**
   * Return the expression representing the function being called,
   */
  public final Expr getFunction() 
  {
    return super.getOperand(0);
  }

  /**
   * Return the expression representing the i-th argument to the
   * function.
   */
  public final Expr getArgument(int i) 
  {
    return super.getOperand(i + 1);
  }

  /**
   * Return the number of function arguments.
   */
  public final int numArguments()
  {
    return super.numOperands() - 1;
  }

  /**
   * Return an array of the arguments.
   */
  public final Expr[] getArgumentArray()
  {
    int  l       = super.numOperands() - 1;
    Expr[] array = new Expr[l];
    for (int i = 0; i < l; i++)
      array[i] = super.getOperand(i + 1);  // skip over the function name
    return array;
  }

  /**
   * Specify the number of times this call occurred during execution.
   */
  public final void setProfCallCnt(int count)
  {
    this.profCallCnt = count;
  }

  /**
   * Return the number of times this call occurred during execution.
   */
  public final int getProfCallCnt()
  {
    return profCallCnt;
  }

  /**
   * Add may definition information to the call expression.
   * @param md the may definition expression
   */
  public void addMayDef(MayDef md)
  {
    if (md == null)
      return;

    if (mayDef == null) 
      mayDef = new Vector<MayDef>(2);

    LoadExpr   le  = (LoadExpr) md.getLhs();
    VirtualVar vv  = (VirtualVar) le.getSubsetDecl();
    int        l   = mayDef.size();
    for (int i = 0; i < l; i++) {
      MayDef     omd = mayDef.elementAt(i);
      LoadExpr   ole = (LoadExpr) omd.getLhs();
      VirtualVar ovv = (VirtualVar) ole.getSubsetDecl();

      if (ovv.subsetEquiv(vv)) // Superset already in list.
        return;

      if (vv.subsetEquiv(ovv)) { // Replace subset with superset.
        mayDef.setElementAt(md, i);
        md.setGraphNode(this);
        omd.setGraphNode(null);
        return;
      }
    }

    mayDef.addElement(md);
    md.setGraphNode(this);
  }

  /**
   * Return a list of the may definition expressions associated with
   * the call expression.
   */
  public Enumeration<MayDef> getMayDef()
  {
    return (mayDef == null ? new EmptyEnumeration<MayDef>() : mayDef.elements());
  }

  /**
   * Add may use information to the call expression.
   * @param mu the may use expression
   */
  public void addMayUse(MayUse mu)
  {
    if (mu == null)
      return;

    if (mayUse == null) 
      mayUse = new Vector<MayUse>(2);

    VirtualVar vv = (VirtualVar) mu.getSubsetDecl();
    int        l  = mayUse.size();
    for (int i = 0; i < l; i++) {
      MayUse     omu = mayUse.elementAt(i);
      VirtualVar ovv = (VirtualVar) omu.getSubsetDecl();

      if (ovv.subsetEquiv(vv)) // Superset already in list.
        return;

      if (vv.subsetEquiv(ovv)) { // Replace subset with superset.
        mayUse.setElementAt(mu, i);
        mu.setGraphNode(this);
        omu.setGraphNode(null);
        return;
      }
    }

    mayUse.addElement(mu);
    mu.setGraphNode(this);
  }

  /**
   * Return a list of the may use expressions associated with the call
   * expression.
   */
  public Enumeration<MayUse> getMayUse()
  {
    return (mayUse == null ? new EmptyEnumeration<MayUse>() : mayUse.elements());
  }

  /**
   * Return true if parameter's declaration already present in mayDef
   * vector
   */
  public boolean inMayDef(Declaration d)
  {
    if (mayDef == null)
      return false;

    int l = mayDef.size();
    for (int i = 0; i < l; i++) {
      MayDef              md = mayDef.elementAt(i);
      LoadDeclAddressExpr ld = md.getLhs();

      if (ld.getDecl() == d)
        return true;
    }

    return false;
  }

  /**
   * Return the Chord with the highest label value from the set of
   * Chords that must be executed before this expression.
   * @param lMap is used to memoize the expression to critical Chord
   * information
   * @param independent is returned if the expression is not dependent
   * on anything
   */
  protected scale.score.chords.Chord findCriticalChord(HashMap<Expr, scale.score.chords.Chord> lMap,
                                                       scale.score.chords.Chord                independent)
  {
    scale.score.chords.Chord cc = super.findCriticalChord(lMap, independent);
    int cmax = cc.getLabel();

    if (mayDef != null) {
      int l = mayDef.size();
      for (int i = 0; i < l; i++) {
        Expr                     exp = mayDef.elementAt(i).getLhs();
        scale.score.chords.Chord nc  = exp.getCriticalChord(lMap, independent);
        if (nc == null)
          return getChord();

        int nmax = nc.getLabel();

        if (nmax <= cmax)
          continue;

        cmax = nmax;
        cc = nc;
      }
    }

    if (mayUse != null) {
      int l = mayUse.size();
      for (int i = 0; i < l; i++) {
        Expr                     exp = mayUse.elementAt(i).getGraphNode();
        scale.score.chords.Chord nc  = exp.getCriticalChord(lMap, independent);
        if (nc == null)
          return getChord();

        int nmax = nc.getLabel();

        if (nmax <= cmax)
          continue;

        cmax = nmax;
        cc = nc;
      }
    }

    return cc;
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.MAGENTA;
  }

  /**
   * Return true if this expression may result in the generation of a
   * call to a subroutine.
   */
  public boolean mayGenerateCall()
  {
    return true;
  }

  /**
   * Return true if this is a call to pure function.
   */
  public boolean isPure()
  {
    Expr fr = getFunction();
    if (!(fr instanceof LoadExpr))
      return false;

    Declaration vd = ((LoadExpr) fr).getDecl();
    if (vd == null)
      return false;

    return vd.isPure();
  }

  /**
   * Return the call expression or null if none.
   * @param ignorePure is true if pure function calls are to be
   * ignored.
   */
  public CallExpr getCall(boolean ignorePure)
  {
    if (!ignorePure)
      return this;

    Expr fr = getFunction();
    if (!(fr instanceof LoadExpr))
      return this;

    Declaration vd = ((LoadExpr) fr).getDecl();
    if (vd == null)
      return this;

    if (!(vd.isRoutineDecl()))
      return this;

    if (!vd.isPure())
      return this;

    return null;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    if (!isPure())
      return false;
    return super.optimizationCandidate();
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 1000;
  }
}
