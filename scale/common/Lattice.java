package scale.common;

import java.lang.*;
import scale.clef.type.Type;
import scale.clef.type.VoidType;
import scale.clef.type.BooleanType;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.CharLiteral;
import scale.clef.expr.BooleanLiteral;
import scale.clef.LiteralMap;

/**
 * This class performs lattice arithmetic operations on values wrapped
 * as objects.
 * <p>
 * $Id: Lattice.java,v 1.52 2007-10-04 19:58:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * All the methods are static.  This class implements Lattice
 * operations using unique objects as the top and bottom values.
 */

public class Lattice 
{
  /**
   * The <i>top</i> lattice value.
   */
  public static final Literal Top = new Literal(VoidType.type);
  /**
   * The <i>bottom</i> lattice value.
   */
  public static final Literal Bot = new Literal(VoidType.type);

  private Lattice()
  {
  }

  /**
   * Combine two Lattice values to obtain a new Lattice value.
   * <table>
   * <tbody>
   * <tr><td>any</td><td>meet</td><td>top</td><td>=</td><td>any</td><td></td>
   * <tr><td>any</td><td>meet</td><td>unknown</td><td>=</td><td>unknown</td><td></td>
   * <tr><td>X</td><td>meet</td><td>Y</td><td>=</td><td>X</td><td>if X == Y</td>
   * <tr><td>X</td><td>meet</td><td>Y</td><td>=</td><td>unknown</td><td>if X != Y</td>
   * </tbody>
   * </table>
   */
  public static Literal meet(Literal X, Literal Y)
  {
    if (Y == Top)
      return X;
    if (X == Top)
      return Y;
    if (Y == Bot)
      return Bot;
    if (X == Bot)
      return Bot;
    if (X.equals(Y))
      return X;
    return Bot;
  }

  /**
   * Return the integer value of the object.
   * @throws scale.common.InvalidException if the object is not recognized.
   */
  public static long convertToLongValue(Literal v) throws scale.common.InvalidException
  {
    if (v instanceof IntLiteral)
      return ((IntLiteral) v).getLongValue();
    if (v instanceof CharLiteral)
      return ((CharLiteral) v).getCharacterValue();
    if (v instanceof BooleanLiteral)
      return ((BooleanLiteral) v).getBooleanValue() ? 1 : 0;
    if (v instanceof FloatLiteral) {
      double vd = ((FloatLiteral) v).getDoubleValue();
      long   vl = (long) vd;
      if (((double) vl) == vd)
        return vl;
    }
    throw new scale.common.InvalidException("Can't convert to long " + v);
  }

  /**
   * Return the floating point value of the object.
   * @throws scale.common.InvalidException if the object is not recognized.
   */
  public static double convertToDoubleValue(Literal v) throws scale.common.InvalidException
  {
    if (v instanceof FloatLiteral)
      return ((FloatLiteral) v).getDoubleValue();
    if (v instanceof IntLiteral)
      return (double) ((IntLiteral) v).getLongValue();
    if (v instanceof CharLiteral)
      return (double) ((CharLiteral) v).getCharacterValue();
    if (v instanceof BooleanLiteral)
      return ((BooleanLiteral) v).getBooleanValue() ? 1.0 : 0.0;
    throw new scale.common.InvalidException("Can't convert to double " + v);
  }

  /**
   * Return the boolean value of the object.
   * @throws scale.common.InvalidException if the object is not recognized.
   */
  public static boolean convertToBooleanValue(Literal v) throws scale.common.InvalidException
  {
    if (v instanceof IntLiteral)
      return (((IntLiteral) v).getLongValue() == 0) ? false : true;
    if (v instanceof BooleanLiteral)
      return ((BooleanLiteral) v).getBooleanValue() ? true : false;
    if (v instanceof CharLiteral)
      return (((CharLiteral) v).getCharacterValue() == 0) ? false : true;
    if (v instanceof FloatLiteral)
      return (((FloatLiteral) v).getDoubleValue() == 0.0) ? false : true;
    throw new scale.common.InvalidException("Can't convert to boolean " + v);
  }

  public static Literal add(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav + rav, type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav + rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal subtract(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav - rav, type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav - rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal divide(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType()) {
        long rav = convertToLongValue(ra);
        if (rav != 0) {
          long lav = convertToLongValue(la);
          if (type.isSigned() || ((lav > 0) && (rav > 0)))
            return LiteralMap.put(lav / rav, type);
        }
      } else if (type.isRealType()) {
        double rav = convertToDoubleValue(ra);
        if (rav != 0.0) {
          double lav = convertToDoubleValue(la);
          return LiteralMap.put(lav / rav, type);
        }
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal multiply(Type type, Literal la, Literal ra)
  {
    try {
      if (la == Bot) {
        if ((ra != Bot) && (ra != Top)) {
          if (type.isIntegerType() || type.isPointerType()) {
            long rav = convertToLongValue(ra);
            if (rav == 0)
              return LiteralMap.put(0, type);
          } else {
            double rav = convertToDoubleValue(ra);
            if (rav == 0.0)
              return LiteralMap.put(0.0, type);
          }
        }
        return Bot;
      }

      if (la == Top) {
        if ((ra != Bot) && (ra != Top)) {
          if (type.isIntegerType() || type.isPointerType()) {
            long rav = convertToLongValue(ra);
            if (rav == 0)
              return LiteralMap.put(0, type);
          } else {
            double rav = convertToDoubleValue(ra);
            if (rav == 0.0)
              return LiteralMap.put(0.0, type);
          }
          return Top;
        }
        return ra;
      }

      if (ra == Bot) {
        if (type.isIntegerType() || type.isPointerType()) {
          long lav = convertToLongValue(la);
          if (lav == 0)
            return LiteralMap.put(0, type);
        } else {
          double lav = convertToDoubleValue(la);
          if (lav == 0.0)
            return LiteralMap.put(0.0, type);
        }
        return Bot;
      }

      if (ra == Top) {
        if (type.isIntegerType() || type.isPointerType()) {
          long lav = convertToLongValue(la);
          if (lav == 0)
            return LiteralMap.put(0, type);
        } else {
          double lav = convertToDoubleValue(la);
          if (lav == 0.0)
            return LiteralMap.put(0.0, type);
        }
        return Top;
      }

      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav * rav, type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav * rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal remainder(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav % rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal abs(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.abs(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.abs(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal negate(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() ||
          type.isPointerType() ||
          type.isEnumerationType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(-lav, type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(-lav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal bitComplement(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(~lav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal bitAnd(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav & rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal bitOr(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav | rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal bitXor(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav ^ rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal not(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      boolean l = convertToBooleanValue(la);
      return new BooleanLiteral(BooleanType.type, !l);
    } catch(InvalidException e) {
      return Bot;
    }
  }

  public static Literal andCond(Type type, Literal la, Literal ra)
  {
    boolean l;
    boolean r;
    try {
      if (la == Bot) {
        if (ra == Bot)
          return Bot;
        else if (ra == Top)
          return Top;
        r = convertToBooleanValue(ra);
        if (!r)
          return new BooleanLiteral(BooleanType.type, r);
      } else if (la == Top) {
        if ((ra == Bot) || (ra == Top))
          return Top;
        r = convertToBooleanValue(ra);
        if (!r)
          return new BooleanLiteral(BooleanType.type, r);
      }
      l = convertToBooleanValue(la);
      if (!l)
        return new BooleanLiteral(BooleanType.type, l);
      if ((ra == Bot) || (ra == Top))
        return Top;
      r = convertToBooleanValue(ra);
      return new BooleanLiteral(BooleanType.type, l && r);
    } catch(InvalidException e) {
      return Bot;
    }
  }

  public static Literal orCond(Type type, Literal la, Literal ra)
  {
    boolean l;
    boolean r;
    try {
      if (la == Bot) {
        if (ra == Bot)
          return Bot;
        else if (ra == Top)
          return Top;
        r = convertToBooleanValue(ra);
        if (r)
          return new BooleanLiteral(BooleanType.type, r);
      } else if (la == Top) {
        if ((ra == Bot) || (ra == Top))
          return Top;
        r = convertToBooleanValue(ra);
        if (r)
          return new BooleanLiteral(BooleanType.type, r);
      }
      l = convertToBooleanValue(la);
      if (l)
        return new BooleanLiteral(BooleanType.type, l);
      if ((ra == Bot) || (ra == Top))
        return Top;
      r = convertToBooleanValue(ra);
      return new BooleanLiteral(BooleanType.type, l || r);
    } catch(InvalidException e) {
      return Bot;
    }
  }

  public static Literal equal(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav == rav, BooleanType.type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav == rav, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  /**
   * Return true if the value of the argument is zero.
   */
  public static boolean isZero(Literal la)
  {
    if (la == Bot)
      return false;
    if (la == Top)
      return false;

    if (la instanceof IntLiteral)
      return ((IntLiteral) la).getLongValue() == 0;
    if (la instanceof CharLiteral)
      return ((CharLiteral) la).getCharacterValue() == 0;
    if (la instanceof BooleanLiteral)
      return !((BooleanLiteral) la).getBooleanValue();
    if (la instanceof FloatLiteral)
      return (long) ((FloatLiteral) la).getDoubleValue() == 0.0;
    return false;
  }

  /**
   * Return true if the value of the argument is zero.
   */
  public static boolean isNonZero(Literal la)
  {
    if (la == Bot)
      return false;
    if (la == Top)
      return false;

    if (la instanceof IntLiteral)
      return ((IntLiteral) la).getLongValue() != 0;
    if (la instanceof CharLiteral)
      return ((CharLiteral) la).getCharacterValue() != 0;
    if (la instanceof BooleanLiteral)
      return ((BooleanLiteral) la).getBooleanValue();
    if (la instanceof FloatLiteral)
      return (long) ((FloatLiteral) la).getDoubleValue() != 0.0;
    return false;
  }

  public static Literal notEqual(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav != rav, BooleanType.type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav != rav, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal less(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav < rav, BooleanType.type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav < rav, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal lessUnsigned(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        boolean r = lav < rav;
        if (lav < 0) {
          if (rav >= 0)
            r = false;
        } else if (rav < 0)
          r = true;

        return LiteralMap.put(r, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal lessEqual(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav <= rav, BooleanType.type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav <= rav, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal lessEqualUnsigned(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        boolean r = lav <= rav;
        if (lav < 0) {
          if (rav >= 0)
            r = false;
        } else if (rav < 0)
          r = true;

        return LiteralMap.put(r, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal greater(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav > rav, BooleanType.type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav > rav, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal greaterUnsigned(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        boolean r = lav > rav;
        if (lav < 0) {
          if (rav >= 0)
            r = true;
        } else if (rav < 0)
          r = false;

        return LiteralMap.put(r, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal greaterEqual(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav >= rav, BooleanType.type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav >= rav, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal greaterEqualUnsigned(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        boolean r = lav >= rav;
        if (lav < 0) {
          if (rav >= 0)
            r = true;
        } else if (rav < 0)
          r = false;

        return LiteralMap.put(r, BooleanType.type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal compare(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    boolean flt = la.getCoreType().isRealType() || ra.getCoreType().isRealType();
    long v = 0;
    try {
      if (!flt) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        if (lav < rav)
          v = -1;
        if (lav > rav)
          v = 1;
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        if (lav < rav)
          v = -1;
        if (lav > rav)
          v = 1;
      }
    } catch(InvalidException e) {
      return Bot;
    }
    return LiteralMap.put(v, type);
  }

  public static Literal minimum(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav >= rav ? rav : lav, type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav >= rav ? rav : lav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal maximum(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav < rav ? rav : lav, type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(lav < rav ? rav : lav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal shiftLeft(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav << rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal shiftSignedRight(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav >> rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal shiftUnsignedRight(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(lav >>> rav, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal power(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put((long) Math.pow(lav, rav), type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(Math.pow(lav, rav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal atan2(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put((long) Math.atan2(lav, rav), type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(Math.atan2(lav, rav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal sign(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        long av = Math.abs(lav);
        return LiteralMap.put(rav < 0 ? - av : av, type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        double av  = Math.abs(lav);
        return LiteralMap.put(rav < 0.0 ? - av : av, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal dim(Type type, Literal la, Literal ra)
  {
    if ((la == Bot) || (ra == Bot))
      return Bot;
    if ((la == Top) || (ra == Top))
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        long rav = convertToLongValue(ra);
        return LiteralMap.put(rav < lav ? lav - rav : 0, type);
      } else {
        double lav = convertToDoubleValue(la);
        double rav = convertToDoubleValue(ra);
        return LiteralMap.put(rav < lav ? lav - rav : 0.0, type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal sqrt(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.sqrt(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.sqrt(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal exp(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.exp(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(StrictMath.exp(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal log(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.log(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.log(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal log10(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

//      try {
//        double bdla = convertToDoubleValue(la);
//        double     val  = Math.log10(bdla.doubleValue());
//        return new double(val);
//      } catch(Throwable e) {
//        return Bot;
//      }
    return Bot;
  }

  public static Literal sin(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.sin(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.sin(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal cos(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.cos(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.cos(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal tan(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.tan(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.tan(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal asin(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.asin(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.asin(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal acos(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.acos(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.acos(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal atan(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

    try {
      if (type.isIntegerType() || type.isPointerType()) {
        long lav = convertToLongValue(la);
        return LiteralMap.put(Math.atan(lav), type);
      } else {
        double lav = convertToDoubleValue(la);
        return LiteralMap.put(Math.atan(lav), type);
      }
    } catch(InvalidException e) {
    }
    return Bot;
  }

  public static Literal sinh(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

//      try {
//        double bdla = convertToDoubleValue(la);
//        double     val  = Math.sinh(bdla.doubleValue());
//        return new double(val);
//      } catch(Throwable e) {
//        return Bot;
//      }
    return Bot;
  }

  public static Literal cosh(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

//      try {
//        double bdla = convertToDoubleValue(la);
//        double     val  = Math.cosh(bdla.doubleValue());
//        return new double(val);
//      } catch(Throwable e) {
//        return Bot;
//      }
    return Bot;
  }

  public static Literal tanh(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

//      try {
//        double bdla = convertToDoubleValue(la);
//        double     val  = Math.tanh(bdla.doubleValue());
//        return new double(val);
//      } catch(Throwable e) {
//        return Bot;
//      }
    return Bot;
  }

  public static Literal conjg(Type type, Literal la)
  {
    if (la == Bot)
      return Bot;
    if (la == Top)
      return Top;

//      try {
//        double bdla = convertToDoubleValue(la);
//        double     val  = Math.conjg(bdla.doubleValue());
//        return new double(val);
//      } catch(Throwable e) {
//        return Bot;
//      }
    return Bot;
  }

  public static Literal stringCat(Type type, Literal la, Literal ra)
  {
    throw new NotImplementedError("Lattice stringCat");
  }

  /**
   * Return x in (value = 2**x) or -1 if value is zero, negative or not a power of two.
   */
  public static int powerOf2(long value)
  {
    if (value > 0) {
      int shift = 0;
      if ((value & 0xffffffff) == 0) {
        value >>= 32;
        shift += 32;
      }
      if ((value & 0xffff) == 0) {
        value >>= 16;
        shift += 16;
      }
      if ((value & 0xff) == 0) {
        value >>= 8;
        shift += 8;
      }
      if ((value & 0xf) == 0) {
        value >>= 4;
        shift += 4;
      }
      if ((value & 0x3) == 0) {
        value >>= 2;
        shift += 2;
      }
      if ((value & 0x1) == 0) {
        value >>= 1;
        shift += 1;
      }
      if (value == 1)
        return shift;
    }
    return -1;
  }
}
