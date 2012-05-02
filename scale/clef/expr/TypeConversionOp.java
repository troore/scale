package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.*;

/**
 * A class which represents a language defined type conversion operation.
 * <p>
 * $Id: TypeConversionOp.java,v 1.56 2007-10-04 19:58:07 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Type conversion rules vary substantially between languages.  Hence,
 * user code is responsible for ensuring that all implicit conversions
 * are made explicit.  
 * <p>
 * Some languages provide a fixed set of type conversions, but C++ allows
 * users to define conversion routines for classes.  Hence in the
 * generation interface, a type conversion is a triple consisting of the
 * expression to be converted, the type to which it is to be converted,
 * and a routine for performing the conversion.  For language defined
 * conversions, the generation interface provides an enumerated list of
 * recognized conversions.  The elements of the following enumeration
 * follow Modula-3 semantics.
 *
 */

public class TypeConversionOp extends MonadicOp
{
  /**
   * The type of conversion..
   */
  private CastMode conversion; 

  /**
   * A constructor which allows either a language-defined
   * conversion routine to be specified.
   * @param conversion specifies the type conversion performed
   */
  public TypeConversionOp(Type resultType, Expression e, CastMode conversion)
  {
    super(resultType, e);

    this.conversion = conversion;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    TypeConversionOp op = (TypeConversionOp) exp;
    return conversion == op.conversion;
  }

  public void visit(Predicate p)
  {
    p.visitTypeConversionOp(this);
  }

  /**
   * Return the type of conversion.
   */
  public final CastMode getConversion()
  {
    return conversion;
  }

  protected final void setConversion(CastMode c)
  {
    conversion = c;
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Expression exp = getExpr();
    Literal    la  = exp.getConstantValue();

    if ((la != Lattice.Bot) && (la != Lattice.Top))
      exp = la;

    switch (conversion) {
    case CAST:
      if (exp instanceof IntLiteral)
        return LiteralMap.put(((IntLiteral) exp).getLongValue(), getType());
      if (exp.getType().isPointerType())
        return exp.getConstantValue();
      break;
    case REAL:
      if (exp instanceof FloatLiteral)
        return la;
      if (exp instanceof IntLiteral) {
        IntLiteral il   = (IntLiteral) exp;
        return LiteralMap.put(il.convertToDouble(), getType());
      }
      if (exp instanceof ComplexOp)
        return ((ComplexOp) exp).getExpr1().getConstantValue();
      if (exp instanceof ComplexLiteral)
        return ((ComplexLiteral) exp).getRealPart();
      break;
    case IMAGINARY:
      if (exp instanceof ComplexOp)
        return ((ComplexOp) exp).getExpr2().getConstantValue();
      if (exp instanceof ComplexLiteral)
        return ((ComplexLiteral) exp).getImaginaryPart();
      break;

    case TRUNCATE:
      if (exp instanceof IntLiteral) {
        if (getCoreType() == exp.getCoreType())
          return la;
        return LiteralMap.put(((IntLiteral) exp).getLongValue(), getType());
      }
      if (exp instanceof BooleanLiteral)
        return LiteralMap.put(((BooleanLiteral) exp).isZero() ? 0 : 1, getType());
      if (exp instanceof FloatLiteral)
        return LiteralMap.put((long) ((FloatLiteral) exp).getDoubleValue(), getType());
      if (exp instanceof AddressLiteral)
        return la;
      break;
    case FLOOR:
      if (exp instanceof FloatLiteral) {
        Type type = getType();
        if (type.isIntegerType())
          return LiteralMap.put((long) ((FloatLiteral) exp).getDoubleValue(), type);
      }
      break;
    }
    return Lattice.Bot;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(conversion);
    return buf.toString();
  }

  /**
   * Return the type of cast required.  If no cast is required,
   * <code>cNone</code> is returned.  If no valid cast is available,
   * <code>cInvalid</code> is returned.
   */
  public static CastMode determineCast(Type targetType, Type sourceType)
  {
    targetType = targetType.getNonConstType();

    if (targetType == sourceType.getNonConstType())
      return CastMode.NONE;

    Type from = sourceType.getCoreType();
    Type to   = targetType.getCoreType();

    if (to.equivalent(from))
      return CastMode.NONE;

    if (to == VoidType.type)
      return CastMode.CAST;

      if (to.isComplexType())
        return CastMode.INVALID;

    if (from.isPointerType()) {
      if (to.isPointerType())
        return CastMode.CAST;
      if (to.isIntegerType())
        return CastMode.TRUNCATE;
    } else if (from.isIntegerType()) {
      if (to.isIntegerType())
        return CastMode.TRUNCATE;
      if (to.isEnumerationType())
        return CastMode.TRUNCATE;
      if (to.isPointerType())
        return CastMode.CAST;
      if (to.isRealType())
        return CastMode.REAL;
    } else if (from.isComplexType()) {
      if (to.isRealType())
        return CastMode.REAL;
    } else if (from.isRealType()) {
      if (to.isRealType())
        return CastMode.REAL;
      if (to.isIntegerType())
        return CastMode.TRUNCATE;
    } else if (from.isEnumerationType()) {
      if (to.isIntegerType() || to.isEnumerationType() || to.isBooleanType())
        return CastMode.TRUNCATE;
      if (to.isRealType())
        return CastMode.REAL;
    } else if (from.isBooleanType()) {
      if (to.isIntegerType())
        return  CastMode.TRUNCATE;
    } else if (from.isFortranCharType() && to.isFortranCharType())
      return CastMode.NONE;

    if (from.isArrayType() && to.isArrayType())
      return CastMode.NONE;

    return CastMode.INVALID;
  }
}
