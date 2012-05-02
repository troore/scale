package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.FieldDecl;
import scale.clef.decl.Declaration;

/**
 * A class which represents the address of the value of the "." and
 * "<code>-&gt;</code>" operators in C.
 * <p>
 * $Id: SelectIndirectOp.java,v 1.32 2007-06-05 14:15:24 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The selection operation represents the address of the value of the
 * operation.  The right argument is the address of the structure and
 * the left argument is the field.
 */
public class SelectIndirectOp extends AggregateOp
{
  public SelectIndirectOp(Expression struct, FieldDecl fd)
  {
    super(PointerType.create(fd.getType()), struct, fd);
    ArrayType at = fd.getCoreType().returnArrayType();
    if (at != null)
      setType(PointerType.create(at.getArraySubtype()));
    assert struct.getType().isPointerType() : "Not pointer type - " + struct.getCoreType();
  }

  public void visit(Predicate p)
  {
    p.visitSelectIndirectOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    Literal struct = getStruct().getConstantValue();
    if ((struct == Lattice.Bot) || (struct == Lattice.Top))
      return Lattice.Bot;

    FieldDecl fd = getField();

    Type ty = struct.getPointedToCore();
    if (!ty.isAggregateType())
      return Lattice.Bot;

    ty.memorySize(Machine.currentMachine);

    Type      ft = fd.getType();
    ArrayType at = ft.getCoreType().returnArrayType();
    if (at != null)
      ft = at.getElementType();

    Type    pt = PointerType.create(fd.getType());
    long    of = fd.getFieldOffset();
    Literal al = null;
    if (struct instanceof AddressLiteral) {
      AddressLiteral stl  = (AddressLiteral) struct;
      Declaration    decl = stl.getDecl();
      of += stl.getOffset();
      if (decl != null)
        al = new AddressLiteral(pt, decl, of);
      else
        al = new AddressLiteral(pt, stl.getValue(), of);
    } else
      al = new AddressLiteral(pt, struct, of);

    return al;
  }
}
