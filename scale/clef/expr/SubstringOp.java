package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents the substring operationapplied to a
 * Fortran CHARACTER variable.
 * <p>
 * $Id: SubstringOp.java,v 1.2 2007-10-04 19:58:07 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public final class SubstringOp extends TernaryOp
{
  /**
   * Create an substring instance of a Fortran CHARACTER string from
   * another string.  The type is computed.
   * @param str specifies the CHARACTER string
   * @param first specifies the index of the starting character (1-origin indexing)
   * @param last specifies the index of the ending character (1-origin indexing)
   */
  public SubstringOp(Expression str, Expression first, Expression last)
  {
    super(calcType(first, last), str, first, last);
  }

  public void visit(Predicate p)
  {
    p.visitSubstringOp(this);
  }

  /**
   * Return the string expression.
   */
  public final Expression getStr()
  {
    return getExpr1();
  }

  /**
   * Return the expression that specifies the index of the starting
   * character (1 -origin).
   */
  public final Expression getFirst()
  {
    return getExpr2();
  }

  /**
   * Return the expression that specifies the index of the ending
   * character (1 -origin).
   */
  public final Expression getLast()
  {
    return getExpr3();
  }

  /**
   * Return the FortranCharType required for the specified starting
   * and ending character indexes.
   */
  public static FortranCharType calcType(Expression fci, Expression lci)
  {
    Literal first = fci.getConstantValue();
    Literal last  = lci.getConstantValue();

    try {
      long f = Lattice.convertToLongValue(first);
      long l = Lattice.convertToLongValue(last);
      if (f <= l) {
        long   sz = l - f + 1;
        if ((sz <= Integer.MAX_VALUE) && (sz > 0))
          return FortranCharType.create((int) sz);
      }
    } catch (scale.common.InvalidException ex) {
    }
    return FortranCharType.create(0);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal str = getExpr1().getConstantValue();
    if (!(str instanceof StringLiteral))
      return Lattice.Bot;

    Literal first = getExpr2().getConstantValue();
    Literal last  = getExpr3().getConstantValue();

    try {
      long f = Lattice.convertToLongValue(first);
      long l = Lattice.convertToLongValue(last);
      if (f > l)
        return Lattice.Bot;

      String s  = ((StringLiteral) str).getStringValue();
      long   sz = l - f + 1;
      if ((f > Integer.MAX_VALUE) || (f < 1))
        return Lattice.Bot;
      if ((l > Integer.MAX_VALUE) || (l < 1))
        return Lattice.Bot;
      if ((sz > Integer.MAX_VALUE) || (sz < 1))
        return Lattice.Bot;
      return LiteralMap.put(s.substring((int) (f - 1), (int) (l - 1)),
                            FortranCharType.create((int) sz));
    } catch (scale.common.InvalidException ex) {
      return Lattice.Bot;
    }
  }
}
