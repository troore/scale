package scale.score;

import scale.common.*;
import scale.score.chords.ExprChord;
import scale.score.expr.*;
import scale.score.chords.Chord;
import scale.score.chords.LoopHeaderChord;
import scale.score.chords.LoopTailChord;
import scale.score.chords.IfThenElseChord;
import scale.clef.decl.Declaration;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.decl.VariableDecl;
import scale.clef.LiteralMap;

import scale.score.dependence.AffineExpr;

/**
 * Record information about a loop induction variable.
 * <p>
 * $Id: InductionVar.java,v 1.42 2007-10-04 19:58:18 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <p>
 * The loop induction variable is the variable that is modified on
 * each iteration of the loop and controls whether the loop
 * terminates.  For example, in
 * <pre>
 *    for (i = init; i < n; i += step) { }
 * </pre>
 * The variable <code>i</code> is the induction variable.  The
 * expression <code>init</code> is the initialization expression,
 * <code>i &lt; n</code> is the termination expression, and <code>i +
 * step</code> is the induction expression.
 */
public final class InductionVar
{
  private LoopHeaderChord loop;      // The loop associated with this induction variable.
  private VariableDecl    var;       // The representative variable.
  private MatchExpr       termExpr;  // The expression used in the loop exit test.
  private Expr            stepExpr;  // The expression used in the loop exit test.
                                     // Note - the stepExpr does not have any use-def links.
  private Expr            initExpr;  // The expression used to initialize the induction variable.
  private AffineExpr      fe;        // Forward substituted expression for the variable.
  private Expr            endExpr;   // The last value of the induction variable in the loop;
  private long            stepValue; // The actual increment if it is a constant.
  private boolean         forward = false;
  private boolean         primary = false; // True if this is the primary induction variable.
  private boolean         reverse = false; // True if the termination comparison should be reversed.
  private boolean         hasEnd  = false; // True if the last value of the induction variable is known.

  public InductionVar(LoopHeaderChord loop, VariableDecl var)
  {
    this.loop     = loop;
    this.var      = var.getOriginal();
    this.termExpr = null;
    this.initExpr = null;
    this.fe       = null;

    this.stepValue = 0;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(iv ");
    buf.append(var);
    buf.append(' ');
    if (forward)
      buf.append("f");
    if (primary)
      buf.append("p");
    buf.append(" i: ");
    buf.append(initExpr);
    buf.append(" s: ");
    if (stepValue != 0)
      buf.append(stepValue);
    else
      buf.append(stepExpr);
    buf.append(" t: ");
    buf.append(termExpr);
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return the loop associ8ated with this induction variable.
   */
  public LoopHeaderChord getLoopHeader()
  {
    return loop;
  }

  /**
   * Return a reference to the induction variable.
   */
  public VariableDecl getVar()
  {
    return var;
  }

  /**
   * Return the expression that terminates the loop.
   */
  public Expr getTermExpr()
  {
    return termExpr;
  }

  /**
   * Set the expression that terminates the loop.
   * @param term is the test expression
   * @param reverse is true if the test is reversed for the exit from the loop
   */
  public void setTermExpr(MatchExpr term, boolean reverse)
  {
    this.termExpr = term;
    this.reverse  = reverse;
    this.hasEnd   = false;
    this.endExpr  = null;
  }

  /**
   * Return the expression that specifies the initial value of the
   * induction variable.
   */
  public Expr getInitExpr()
  {
    return initExpr;
  }

  /**
   * Set the expression that specifies the initial value of the
   * induction variable.
   */
  public void setInitExpr(Expr init)
  {
    assert !(init instanceof PhiExpr) :
      "The induction variable can not be initialized by a phi expression. " + init;

    this.initExpr = init;
  }

  /**
   * Return the end value of the induction variable or
   * <code>null</code> if it is not known.  Note: this is not
   * necessarily the value the induction variable will have after the
   * loop terminates.  Rather, it is the last value the induction
   * variable will have inside the loop.
   */
  public Expr getEndExpr()
  {
    if (hasEnd)
      return endExpr;

    endExpr = null;
    hasEnd = true;

    if (termExpr == null)
      return null;

    Chord c = termExpr.getChord();
    if (!(c instanceof IfThenElseChord))
      return null;

    MatchExpr mex = termExpr;
    Expr      ra  = mex.getRightArg();
    Expr      la  = mex.getLeftArg();

    IfThenElseChord ifc = (IfThenElseChord) c;
    if ((ifc.getTrueCfgEdge().isLoopTail()) ||
        (ifc.getFalseCfgEdge().isLoopTail())) { // Loop test at end

      // If the loop exit test is at the end of the loop, the end value
      // is what is in the comparison.

      if (isInductionVarReference(la))
        endExpr = ra;
      else if (isInductionVarReference(ra))
        endExpr = la;

      return endExpr;
    }

    // If the loop exit test is at the start of the loop, the end
    // value is not necessarily what is in the comparison.
    // Consider
    //   for (i=3; --i; )
    // In this case, the end value is actually 0 + 1.

    la = findStart(la);
    ra = findStart(ra);

    if (isInductionVarReference(la))
      endExpr = ra;
    else if (isInductionVarReference(ra))
      endExpr = la;

    if ((endExpr != null) || !(ra instanceof LiteralExpr))
      return endExpr; // E.g., for (i = 0; i < 6; i++)

    Literal lit1 = ((LiteralExpr) ra).getLiteral();
    if (!(lit1 instanceof IntLiteral))
      return endExpr;  // E.g., for (i = 0; i < 6.0; i++)

    long v1 = ((IntLiteral) lit1).getLongValue();
    if ((la instanceof SubtractionExpr)){
      SubtractionExpr se  = (SubtractionExpr) la;
      Expr            ra2 = se.getRightArg();
      Expr            la2 = se.getLeftArg();
      if ((ra2 instanceof LiteralExpr) &&
          isInductionVarReference(la2) &&
          (lit1 instanceof IntLiteral)) {
        Literal lit2 = ((LiteralExpr) ra2).getLiteral();
        if (lit2 instanceof IntLiteral) {
          long v = v1 + ((IntLiteral) lit2).getLongValue();
          endExpr = new LiteralExpr(LiteralMap.put(v, lit1.getCoreType()));
        }
      }
    } else if (la instanceof AdditionExpr) {
      AdditionExpr se  = (AdditionExpr) la;
      Expr         ra2 = se.getRightArg();
      Expr         la2 = se.getLeftArg();
      if ((ra2 instanceof LiteralExpr) &&
          isInductionVarReference(la2) &&
          (lit1 instanceof IntLiteral)) {
        Literal lit2 = ((LiteralExpr) ra2).getLiteral();
        if (lit2 instanceof IntLiteral) {
          long v = v1 - ((IntLiteral) lit2).getLongValue();
          endExpr = new LiteralExpr(LiteralMap.put(v, lit1.getCoreType()));
        }
      }
    }

    return endExpr;
  }

  private Expr findStart(Expr la)
  {
    while (la != null) {
      if ((la instanceof ConversionExpr) && ((ConversionExpr) la).isCast())
        la = ((ConversionExpr) la).getArg();

      if (!(la instanceof LoadDeclValueExpr))
        return la;

      LoadDeclValueExpr ldve = (LoadDeclValueExpr) la;

      ExprChord se = ldve.getUseDef();
      if (se == null)
        return la;

      Expr x = se.getRValue();
      if (x instanceof PhiExpr)
        break;
      la = x;
    }

    return la;
  }

  /**
   * It's possible to have a loop exit test that is indirectly
   * dependent on the induction variable.  It is a lot of work
   * to determine the valid loop parameters in that case and we
   * are just too lazy to do it; especially if it would have
   * marginal impact.
   */
  private boolean isInductionVarReference(Expr lax)
  {
    while (lax != null) {
      if ((lax instanceof ConversionExpr) && ((ConversionExpr) lax).isValuePreserving())
        lax = ((ConversionExpr) lax).getArg();

      if (!(lax instanceof LoadDeclValueExpr))
        return false;

      LoadDeclValueExpr ldve = (LoadDeclValueExpr) lax;

      VariableDecl ld = (VariableDecl) ldve.getDecl();
      if (ld.getOriginal() == var)
        return true;

      ExprChord se = ldve.getUseDef();
      if (se == null)
        return false;

      lax = se.getLValue();
    }
    return false;
  }

  /**
   * Return the number of times the induction variable will be
   * iterated or -1 if not known.
   */
  public long getIterationCount()
  {
    long step = getStepValue();
    if (step == 0)
      return -1;

    boolean isDec = false;
    if (step < 0) {
      step = - step;
      isDec = true;
    }

    Expr lb = getInitExpr();
    if (lb == null)
      return -1;

    Expr ub = getEndExpr();
    if (ub == null)
      return -1;

    if (!lb.isLiteralExpr()) {
      Expr n = SubtractionExpr.create(lb.getType(), ub.copy(), lb.copy());
      if (!n.isLiteralExpr()) {
        n.unlinkExpression();
        return -1;
      }

      try {
        Literal cv   = ((LiteralExpr) n).getLiteral().getConstantValue();
        double  high = Lattice.convertToDoubleValue(cv);
        n.unlinkExpression();
        return computeDiff(0.0, high, step, isDec);
      } catch (InvalidException ex) {
        return -1;
      }
    }

    if (!ub.isLiteralExpr())
      return -1;

    try {
      Literal cub  = ((LiteralExpr) ub).getLiteral().getConstantValue();
      Literal clb  = ((LiteralExpr) lb).getLiteral().getConstantValue();
      double  low  = Lattice.convertToDoubleValue(clb);
      double  high = Lattice.convertToDoubleValue(cub);
      return computeDiff(low, high, step, isDec);
    } catch (InvalidException ex) {
      return -1;
    }
  }

  private long computeDiff(double low, double high, long step, boolean isDec)
  {
    if (isDec) {
      double t = low;
      low = high;
      high = t;
    }

    double range = (high - low);
    if (((long) range) != range)
      return -1;

    if (low >= high)
      range = 0;

    CompareMode mop = termExpr.getMatchOp();
    if (reverse)
      mop = mop.reverse();

    if (mop.eq())
      range++;

    return (((long) range) + step - 1) / step;
  }

  /**
   * Return the induction step value or zero if it is not known or not
   * a constant value.
   */
  public long getStepValue()
  {
    return stepValue;
  }

  /**
   * Set the induction step value or zero if it is not known.
   * The step expression must be loop invariant.
   */
  public void setStepExpr(Expr step)
  {
    stepExpr  = reduceStep(step);
    stepValue = 0;

    if (stepExpr == null)
      return;

    if (stepExpr.isLiteralExpr()) {
      try {
        Literal lit = ((LiteralExpr) stepExpr).getLiteral();
        stepValue = Lattice.convertToLongValue(lit.getConstantValue());
      } catch (InvalidException ex) {
      }
    }
  }

  /**
   * Convert the expression into a constant.  Only + and - expressions
   * are handled.  Someday, maybe we will be more general.  We assume
   * any variable references are to the induction variable (or
   * pseudonymn).
   */
  private Expr reduceStep(Expr step)
  {
    if (step.isLiteralExpr())
      return step;

    if (step instanceof AdditionExpr) {
      AdditionExpr ae = (AdditionExpr) step;
      Expr         la = ae.getLeftArg();
      Expr         ra = ae.getRightArg();

      ae.setLeftArg(null);
      ae.setRightArg(null);
      ae.unlinkExpression();

      if ((la instanceof LoadDeclValueExpr) &&
          (var == ((VariableDecl) ((LoadDeclValueExpr) la).getDecl()).getOriginal())) {
        la.unlinkExpression();
        return reduceStep(ra);
      }
      if ((ra instanceof LoadDeclValueExpr) &&
          (var == ((VariableDecl) ((LoadDeclValueExpr) ra).getDecl()).getOriginal())) {
        ra.unlinkExpression();
        return reduceStep(la);
      }
      la = reduceStep(la);
      ra = reduceStep(ra);
      if (la == null) {
        if (ra != null)
          ra.unlinkExpression();
        return null;
      }
      if (ra == null) {
        if (la != null)
          la.unlinkExpression();
        return null;
      }
      return AdditionExpr.create(la.getType(), la, ra);
    }

    if (step instanceof SubtractionExpr) {
      SubtractionExpr ae = (SubtractionExpr) step;
      Expr            la = ae.getLeftArg();
      Expr            ra = ae.getRightArg();

      ae.setLeftArg(null);
      ae.setRightArg(null);
      ae.unlinkExpression();

      if ((la instanceof LoadDeclValueExpr) &&
          (var == ((VariableDecl) ((LoadDeclValueExpr) la).getDecl()).getOriginal())) {
        la.unlinkExpression();
        ra = reduceStep(ra);
        if (ra == null)
          return null;
        return NegativeExpr.create(ra.getCoreType().getSignedType(), ra);
      }
      if ((ra instanceof LoadDeclValueExpr) &&
          (var == ((VariableDecl) ((LoadDeclValueExpr) ra).getDecl()).getOriginal())) {
        ra.unlinkExpression();
        return reduceStep(la);
      }
      la = reduceStep(la);
      ra = reduceStep(ra);
      if (la == null) {
        if (ra != null)
          ra.unlinkExpression();
        return null;
      }
      if (ra == null) {
        if (la != null)
          la.unlinkExpression();
        return null;
      }
      return SubtractionExpr.create(la.getType(), la, ra);
    }

    if (step instanceof LoadDeclValueExpr) {
      LoadDeclValueExpr ldve = (LoadDeclValueExpr) step;
      VariableDecl      vd   = (VariableDecl) ldve.getDecl();

      if (!vd.isNotSSACandidate()) {
        ExprChord se = step.getUseDef();
        step.setUseDef(null);
        if (se == null)
          return step;

        if (!se.getLoopHeader().isSubloop(loop))
          return step;
      }
    }

    step.unlinkExpression();
    return null;
  }

  /**
   * Return the step expression for the induction variable.  Note, the
   * value returned may be <code>null</code>.  It is guaranteed to be
   * loop invariant.  Note - the epression returned does not have any
   * use-def links.
   */
  public Expr getStepExpr()
  {
    return stepExpr;
  }

  /**
   * Return true if this is the primary induction variable for the loop.
   */
  public boolean isPrimary()
  {
    return primary;
  }

  /**
   * Specify that this induction variable is the primary induction
   * variable for the loop.
   */
  public void markPrimary()
  {
    primary = true;
  }

  /**
   * Return true if this induction variable has a forward affine
   * expression.
   */
  public boolean hasForward()
  {
    return forward;
  }

  /**
   * Specify that this induction variable has a forward affine
   * expression.
   */
  public void markForward()
  {
    forward = true;
  }

  /**
   * Return the induction variable's forward affine expression.
   */
  public AffineExpr getForwardExpr()
  {
    return fe;
  }

  /**
   * Specify the induction variable's forward affine expression.
   */
  public void setForwardExpr(AffineExpr ae)
  {
    fe = ae;
    forward = true;
  }

  /**
   * Return the name of the induction variable.
   */
  public String getName()
  {
    return var.getName();
  }

  /**
   * Return true if this induction variable is better than the
   * specified induction variable.  Better means more useful for
   * standard loop transformations.
   */
  public boolean moreCompleteThan(InductionVar ivar)
  {
    if (ivar == null)
      return true;

    if (termExpr != null) {
      if (ivar.termExpr == null)
        return true;
    } else if (ivar.termExpr != null)
      return false;

    Expr endi = ivar.getEndExpr();
    Expr endt = this.getEndExpr();
    if (endt != null) {
      if (endi == null)
        return true;
    } else if (endi != null)
      return false;

    long stepi = ivar.getStepValue();
    long stept = this.getStepValue();
    if (stept != 0) {
      if (stepi == 0)
        return true;
    } else if (stepi != 0)
      return false;

    if (initExpr != null) {
      if (ivar.initExpr == null)
        return true;
    } else if (ivar.initExpr != null)
      return false;

    return true;
  }

  /**
   * Unlink any expression referenced by theis induction variable
   * instance.
   */
  public void clean()
  {
    if (termExpr != null) {
      termExpr.unlinkExpression();
      termExpr = null;
    }

    if (stepExpr != null) {
      stepExpr.unlinkExpression();
      stepExpr = null;
    }

    if (initExpr != null) {
      initExpr.unlinkExpression();
      initExpr = null;
    }

    if (endExpr != null) {
      endExpr.unlinkExpression();
      endExpr = null;
    }

    var = null;
    fe = null;
    loop = null;
  }
}
