package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.expr.TransFtn;
import scale.score.Predicate;

import scale.score.dependence.AffineExpr;
import scale.score.Note;



/**
 * This class represents the monadic intrinsic functions.
 * <p>
 * $Id: TranscendentalExpr.java,v 1.23 2007-10-04 19:58:33 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If would have been better to name this class the BuiltInOperation
 * class.  It represents those complex operations which it may be
 * possible to implement on the target architecture with a simple
 * sequence of instructions.  An example is the square root function
 * which some architectures provide as a single instruction.
 * @see scale.clef.expr.TranscendentalOp
 */
public class TranscendentalExpr extends UnaryExpr
{
  private TransFtn ftn; // ftn represents which transcendental function.

  public TranscendentalExpr(Type t, Expr e1, TransFtn ftn)
  {
    super(t, e1);
    this.ftn = ftn;
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the equivalent() method of the derived
   * classes.
   * @return true if the expressions are equivalent
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    TranscendentalExpr o = (TranscendentalExpr) exp;
    return (o.ftn == ftn);
  }

  /**
   * Return a value indicating the transcendental function represented.
   */
  public TransFtn getFtn()
  {
    return ftn;
  }

  public Expr copy()
  {
    return new TranscendentalExpr(getType(), getArg().copy(), ftn);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return SE_DOMAIN | SE_OVERFLOW | ((ftn == TransFtn.Alloca) ? SE_STATE : 0);
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return getArg().optimizationCandidate() && (ftn != TransFtn.Alloca);
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
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    if (ftn == TransFtn.Alloca)
      return false;
    return super.isLoopInvariant(loop);
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
    if (ftn == TransFtn.Alloca)
      return getChord();
    return super.findCriticalChord(lMap, independent);
  }

  public void visit(Predicate p)
  {
    p.visitTranscendentalExpr(this);
  }

  public String getDisplayLabel()
  {
    return ftn.sName();
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return r;

    Literal la   = getArg().getConstantValue(cvMap);
    Type    type = getCoreType();
    switch (ftn) {
    case Sqrt:  r = Lattice.sqrt(type, la);  break;
    case Exp:   r = Lattice.exp(type, la);   break;
    case Log:   r = Lattice.log(type, la);   break;
    case Log10: r = Lattice.log10(type, la); break;
    case Sin:   r = Lattice.sin(type, la);   break;
    case Cos:   r = Lattice.cos(type, la);   break;
    case Tan:   r = Lattice.tan(type, la);   break;
    case Asin:  r = Lattice.asin(type, la);  break;
    case Acos:  r = Lattice.acos(type, la);  break;
    case Atan:  r = Lattice.atan(type, la);  break;
    case Sinh:  r = Lattice.sinh(type, la);  break;
    case Cosh:  r = Lattice.cosh(type, la);  break;
    case Tanh:  r = Lattice.tanh(type, la);  break;
    case Conjg: r = Lattice.conjg(type, la); break;
    default:    r = Lattice.Bot;             break;
    }

    cvMap.put(this, r);
    return r;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal la   = getArg().getConstantValue();
    Type    type = getCoreType();
    switch (ftn) {
    case Sqrt:  return Lattice.sqrt(type, la); 
    case Exp:   return Lattice.exp(type, la);  
    case Log:   return Lattice.log(type, la);  
    case Log10: return Lattice.log10(type, la);
    case Sin:   return Lattice.sin(type, la);  
    case Cos:   return Lattice.cos(type, la);  
    case Tan:   return Lattice.tan(type, la);  
    case Asin:  return Lattice.asin(type, la); 
    case Acos:  return Lattice.acos(type, la); 
    case Atan:  return Lattice.atan(type, la); 
    case Sinh:  return Lattice.sinh(type, la); 
    case Cosh:  return Lattice.cosh(type, la); 
    case Tanh:  return Lattice.tanh(type, la); 
    case Conjg: return Lattice.conjg(type, la);
    default:    return Lattice.Bot;            
    }
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 25 + getArg().executionCostEstimate();
  }
}
