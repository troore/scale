package scale.score.expr;

import scale.common.*;
import scale.clef.expr.CastMode;
import scale.clef.expr.TypeConversionOp;
import scale.clef.expr.Literal;
import scale.clef.type.Type;
import scale.clef.type.IntegerType;
import scale.clef.type.PointerType;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.LiteralMap;
import scale.score.Note;
import scale.score.Predicate;
import scale.score.dependence.AffineExpr;

/**
 * This class represents the type conversion or cast operation.
 * <p>
 * $Id: ConversionExpr.java,v 1.71 2007-10-04 19:58:29 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The type of the expression represents the value being cast to and
 * the type of the operand represents the type cast from.

 * <p>
 * Casts are not lowered until we translate into a specific machine
 * representation.
 * @see scale.clef.expr.CastMode
 */
public class ConversionExpr extends UnaryExpr
{
  private CastMode conversion; // The name of the language defined conversion routine.

  /**
   * @param type type to be converted to
   * @param value expression value to be converted
   * @param conversion the type of the conversion
   * @see scale.clef.expr.CastMode
   */
  private ConversionExpr(Type type, Expr value,  CastMode conversion)
  {
    super(type, value);
    this.conversion = conversion;

    if (value instanceof ConversionExpr) {
      // Avoid "(long) ((long) x)" which happens more often than you would expect.
      ConversionExpr ce = (ConversionExpr) value;
      if ((ce.conversion == conversion) &&
          ((conversion == CastMode.CAST) ||
           (ce.getCoreType() == type.getCoreType()))) {
        Expr op = ce.getArg();
        op.deleteOutDataEdge(ce);
        setOperand(op, 0);
      }
    }
  }

  /**
   * Return a cast of the expression.  If the cast is redundant,
   * return the argument.  Casts of literals may return a different
   * literal.
   * @param type type to be converted to
   * @param value expression value to be converted
   * @param conversion the type of the conversion
   * @see scale.clef.expr.CastMode
   */
  public static Expr create(Type type, Expr value, CastMode conversion)
  {
    Type vt = value.getType();
    if (type.equivalent(vt)) {
      switch (conversion) {
      case CAST:
        return value;
      case REAL:
        if (vt.isRealType() && !vt.isComplexType())
          return value;
        break;
      case TRUNCATE:
        if (vt.isIntegerType())
          return value;
        break;
      case NONE:
        return value;
      case INVALID:
        throw new scale.common.InternalError("Invalid cast.");
      case IMAGINARY:
      case FLOOR:
      case CEILING:
      case ROUND:
      default:
        break;
      }
    }

    if (conversion == CastMode.CAST) {
      if (type == vt)
        return value;

      if (value instanceof ConversionExpr) {
        ConversionExpr cv = (ConversionExpr) value;
        if (cv.conversion == conversion) {
          cv.setType(type);
          return cv;
        }
      }
    } else if (value.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) value).getLiteral().getConstantValue();
      if (conversion == CastMode.TRUNCATE) {
        if (type.isIntegerType()) {
          if (lit instanceof IntLiteral) {
            long val = ((IntLiteral) lit).getLongValue();
            return new LiteralExpr(LiteralMap.put(val, type));
          }
          else if (lit instanceof FloatLiteral) {
            double val = ((FloatLiteral) lit).getDoubleValue();
            return new LiteralExpr(LiteralMap.put((long) val, type));
          }
        }
      } else if (conversion == CastMode.REAL) {
        if (lit instanceof FloatLiteral) {
          double val = ((FloatLiteral) lit).getDoubleValue();
          return new LiteralExpr(LiteralMap.put(val, type));
        } else if (lit instanceof IntLiteral) {
          long val = ((IntLiteral) lit).getLongValue();
          return new LiteralExpr(LiteralMap.put((double) val, type));
        }
      }
    } else if ((value instanceof ConversionExpr) && (type == vt)) {
      ConversionExpr cv = (ConversionExpr) value;
      if (cv.conversion == conversion)
        return value;
    }

    return new ConversionExpr(type, value, conversion);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int se = super.sideEffects();

    switch (conversion) {
    default: return se; // Mainly address casts.
    case FLOOR:   // Fall through
    case CEILING: // Fall through
    case ROUND:   // Fall through
    case TRUNCATE:
      Type ty1 = getArg().getType();
      if (ty1.isRealType())
        se |= SE_DOMAIN + SE_OVERFLOW;
      return se;
    case REAL:
      Type ty2 = getArg().getType();
      if (!ty2.isRealType())
        return se; // Conversion of integers may lose precision.

      if(!ty2.equivalent(getType()))
        se |= SE_DOMAIN;
      return se;
    }
  }

  public Expr copy()
  {
    return new ConversionExpr(getType(), getArg().copy(), conversion);
  }

  /**
   * Return the type of conversion.
   */
  public CastMode getConversion()
  {
    return conversion;
  }

  /**
   * Return true if this expression is a cast of an address.
   */
  public boolean isCast()
  {
    return (conversion == CastMode.CAST);
  }

  public void visit(Predicate p)
  {
    p.visitConversionExpr(this);
  }

  public String getDisplayLabel()
  {
    return conversion.toString();
  }

  /**
   * Return the variable reference for the expression.  We use this
   * to get the variable reference for operations such as array
   * subscript and structure operations.
   */
  public Expr getReference()
  {
    return getArg().getReference();
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
    
    return (conversion == ((ConversionExpr) exp).conversion);
  }

  /**
   * @return a unique value representing this particular expression
   */
  public long canonical()
  {
    long canon = (super.canonical() << 1) ^ conversion.ordinal();
    return canon;
  }

  protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>  affines,
                                               scale.score.chords.LoopHeaderChord      thisLoop)
  {
    if (!isValuePreserving())
      return AffineExpr.notAffine;

    AffineExpr ae = getArg().getAffineExpr(affines, thisLoop);
    if (ae == null)
      return AffineExpr.notAffine;

    return ae;
  }

  /**
   * Return true if the conversion results in the same value.  For
   * example, a cast of an address to a different type still results
   * in the same address and a conversion of an int to a long results
   * in the same value also.
   */
  public boolean isValuePreserving()
  {
    switch (conversion) {
    case CAST:
      return true;
    case TRUNCATE:
      Expr        arg = getArg();
      IntegerType t1  = getCoreType().returnIntegerType();
      IntegerType t2  = arg.getCoreType().returnIntegerType();
      if ((t1 != null) && (t2 != null)) {
        int bs1 = t1.bitSize();
        int bs2 = t2.bitSize();
        if (bs1 >= bs2)
          return true;
      }
      return false;
    default:
      return false;
    }
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

    Expr arg = getArg();

    r = getConstantValue(cvMap, arg, arg.getConstantValue(cvMap));

    cvMap.put(this, r);
    return r;
  }

  private Literal getConstantValue(HashMap<Expr, Literal> cvMap, Expr arg, Literal lax)
  {
    Type     type = getType();
    CastMode cr   = getConversion();

    switch (cr) {
    default:
      return Lattice.Bot;
    case CAST:
      if (lax instanceof IntLiteral)
        return LiteralMap.put(((IntLiteral) lax).getLongValue(), getType());
      if (lax.getType().isPointerType())
        return lax;
      return Lattice.Bot;
    case REAL:
      if (arg instanceof ComplexValueExpr)
        return ((ComplexValueExpr) arg).getReal().getConstantValue(cvMap);
      if (lax instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lax;
        return LiteralMap.put(il.convertToDouble(), getType());
      }
      return Lattice.Bot;
    case IMAGINARY:
      if (arg instanceof ComplexValueExpr)
        return ((ComplexValueExpr) arg).getImaginary().getConstantValue(cvMap);
      return Lattice.Bot;
    case FLOOR:
    case CEILING:
    case ROUND:
      if (type.isIntegerType()) {
        Type atype = arg.getCoreType();
        if (atype.isIntegerType())
          return lax;
        if (atype.isRealType() && (lax instanceof FloatLiteral)) {
          double v = ((FloatLiteral) lax).getDoubleValue();
          if (Double.isNaN(v))
            return Lattice.Bot;
          if (Double.isInfinite(v))
            return Lattice.Bot;
          if (cr == CastMode.FLOOR)
            return LiteralMap.put((long) v, getType());
          if (cr == CastMode.CEILING)
            return LiteralMap.put((long) Math.ceil(v), getType());
          // Round
          if (v < 0.0)
            return LiteralMap.put((long) Math.ceil(v - 0.5), type);
          return LiteralMap.put((long) Math.floor(v + 0.5), type);
        }
      } else if (type.isRealType() && (lax instanceof FloatLiteral)) {
        double v = ((FloatLiteral) lax).getDoubleValue();
        if (Double.isNaN(v))
          return lax;
        if (Double.isInfinite(v))
          return lax;
        if (cr == CastMode.FLOOR)
          return LiteralMap.put((double) ((long) v), type);
        if (cr == CastMode.CEILING)
          return LiteralMap.put(Math.ceil(v), type);
        // Round
        if (v < 0.0)
          return LiteralMap.put(Math.ceil(v - 0.5), type);
        return LiteralMap.put(Math.floor(v + .5), type);
      }
      return Lattice.Bot;
    case TRUNCATE:
      Type ctype = type.getCoreType();
      if (ctype == arg.getCoreType())
        return lax;
      if (lax instanceof IntLiteral)
        return LiteralMap.put(((IntLiteral) lax).getLongValue(), type);
      if (Lattice.isZero(lax))
        return LiteralMap.put(0, type);

      return Lattice.Bot;
    }
  }

  /**
   * Return the constant value of the expression.  Do not follow
   * use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Expr arg = getArg();

    return getConstantValue(arg, arg.getConstantValue());
  }

  private Literal getConstantValue(Expr arg, Literal lax)
  {
    Type     type = getType();
    CastMode cr   = getConversion();

    switch (cr) {
    default:
      return Lattice.Bot;
    case CAST:
      if (lax instanceof IntLiteral)
        return LiteralMap.put(((IntLiteral) lax).getLongValue(), getType());
      if (lax.getType().isPointerType())
        return lax;
      return Lattice.Bot;
    case REAL:
      if (arg instanceof ComplexValueExpr)
        return ((ComplexValueExpr) arg).getReal().getConstantValue();
      if (lax instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lax;
        return LiteralMap.put(il.convertToDouble(), getType());
      }
      return Lattice.Bot;
    case IMAGINARY:
      if (arg instanceof ComplexValueExpr)
        return ((ComplexValueExpr) arg).getImaginary().getConstantValue();
      return Lattice.Bot;
    case FLOOR:
    case CEILING:
    case ROUND:
      if (type.isIntegerType()) {
        Type atype = arg.getCoreType();
        if (atype.isIntegerType())
          return lax;
        if (atype.isRealType() && (lax instanceof FloatLiteral)) {
          double v = ((FloatLiteral) lax).getDoubleValue();
          if (Double.isNaN(v))
            return Lattice.Bot;
          if (Double.isInfinite(v))
            return Lattice.Bot;
          if (cr == CastMode.FLOOR)
            return LiteralMap.put((long) v, getType());
          if (cr == CastMode.CEILING)
            return LiteralMap.put((long) Math.ceil(v), getType());
          // Round
          if (v < 0.0)
            return LiteralMap.put((long) Math.ceil(v - 0.5), type);
          return LiteralMap.put((long) Math.floor(v + 0.5), type);
        }
      } else if (type.isRealType() && (lax instanceof FloatLiteral)) {
        double v = ((FloatLiteral) lax).getDoubleValue();
        if (Double.isNaN(v))
          return lax;
        if (Double.isInfinite(v))
          return lax;
        if (cr == CastMode.FLOOR)
          return LiteralMap.put((double) ((long) v), type);
        if (cr == CastMode.CEILING)
          return LiteralMap.put(Math.ceil(v), type);
        // Round
        if (v < 0.0)
          return LiteralMap.put(Math.ceil(v - 0.5), type);
        return LiteralMap.put(Math.floor(v + .5), type);
      }
      return Lattice.Bot;
    case TRUNCATE:
      Type ctype = type.getCoreType();
      if (ctype == arg.getCoreType())
        return lax;
      if (lax instanceof IntLiteral)
        return LiteralMap.put(((IntLiteral) lax).getLongValue(), type);
      if (Lattice.isZero(lax))
        return LiteralMap.put(0, type);

      return Lattice.Bot;
    }
  }

  /**
   * Return the SubscriptExpr that this load uses.  This method
   * assumes that we already know that there is a use-def link to an
   * existing SubscriptExpr.
   */
  public SubscriptExpr findSubscriptExpr()
  {
    return getArg().findSubscriptExpr();
  }

  /**
   * Return this Note unless it is a non-essential expression.  For
   * {@link scale.score.chords.Chord Chord} this method returns
   * <code>this</code>.  For a {@link scale.score.expr.DualExpr
   * DualExpr} or an address cast (e.g., {@link
   * scale.score.expr.ConversionExpr ConversionExpr}) this method
   * returns the out data edge.
   */
  public Note getEssentialUse()
  {
    if (conversion != CastMode.CAST)
      return this;

    return getOutDataEdge().getEssentialUse();
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    Expr arg  = getArg();
    int  cost = arg.executionCostEstimate();

    switch (conversion) {
    case CAST:      break;
    case TRUNCATE:  break;
    case IMAGINARY: break;

    case REAL:
      cost++;
      if (!arg.getCoreType().isRealType() || !getCoreType().isRealType())
        cost += 2;
      break;

    default: cost += 4; break;
    }
    return cost;
  }

  /**
   * The given expression is defined if the dual expression is defined
   * and the given expression is either the low or high level
   * expression.
   * @param lv the low or high level expression.
   * @return true if the low expression is defined.
   */
  public boolean isDefined(Expr lv)
  {
    return ((conversion == CastMode.CAST) &&
            (getArg() == lv) && 
            this.isDefined());
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return isValuePreserving() && getArg().isSimpleExpr();
  }
}
