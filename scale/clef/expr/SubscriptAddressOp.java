package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.Declaration;
import scale.clef.type.*;

/**
 * A class representing an array subscript operation that returns a
 * address.
 * <p>
 * $Id: SubscriptAddressOp.java,v 1.22 2007-10-04 19:58:06 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result is the address of the array element.
 */

public class SubscriptAddressOp extends SubscriptOp
{
  public SubscriptAddressOp(Type type, Expression array, Vector<Expression> subscripts)
  {
    super(type, array, subscripts);
  }

  public void visit(Predicate p)
  {
    p.visitSubscriptAddressOp(this);
  }

  /**
   * Create a <code>SubscriptValueOp</code> instance from this.
   */
  public Expression makeRValue()
  {
    return new SubscriptValueOp(PointerType.create(getType()),
                                getArray(),
                                getSubscripts());
  }

  /**
   * Return the constant value of the expression which is an {@link
   * AddressLiteral AddressLiteral} instance.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    long offset = getConstantIndex();
    if (offset < 0)
      return Lattice.Bot;

    int        l        = numSubscripts();
    long[]     indicies = new long[l];
    Expression array    = getArray();
    Type       addt     = array.getPointedToCore();
    Bound[]    dims     = new Bound[l];
    int        r        = 0;

    while (r < l) {
      ArrayType arrt = addt.getCoreType().returnArrayType();
      if (arrt == null)
        break;

      int rank = arrt.getRank();

      for (int i = 0; (i < rank) && (r < l); i++)
        dims[r++] = arrt.getIndex(i);

      addt = arrt.getElementType().getCoreType();
    }

    Type      et = addt.getPointedTo();
    ArrayType at = et.getCoreType().returnArrayType();
    if (at != null)
      et = at.getElementType();

    offset *= et.memorySize(Machine.currentMachine);

    if (array instanceof IdAddressOp)
      return new AddressLiteral(PointerType.create(et),
                                ((IdReferenceOp) array).getDecl(),
                                offset);

    Literal lit = array.getConstantValue();
    if ((lit == Lattice.Bot) || (lit == Lattice.Top))
      return Lattice.Bot;

    if (lit instanceof AddressLiteral) {
      AddressLiteral adl  = (AddressLiteral) lit;
      long           off  = adl.getOffset();
      Declaration    decl = adl.getDecl();
      offset += off;
      if (decl != null)
        return new AddressLiteral(PointerType.create(et), decl, offset);
      lit = adl.getValue();
    }

    Literal al = new AddressLiteral(PointerType.create(et), lit, offset);
    return al;
  }
}
