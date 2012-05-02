package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;

/**
 * A class which represents the address of a Declaration.
 * <p>
 * $Id: IdAddressOp.java,v 1.14 2007-06-04 00:30:13 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class IdAddressOp extends IdReferenceOp 
{
  /**
   * Create the representation of the address of the specified
   * declaration.  The type must be specified, and not obtained from
   * the declaration, because Fortran a C represent the address of an
   * array variable differently.
   */
  public IdAddressOp(Type type, Declaration decl)
  {
    super(type, decl);
    assert type.isPointerType() : "Not pointer type - " + type;
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    return "&" + getDecl().getName();
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(" &");
    buf.append(getDecl().getName());
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitIdAddressOp(this);
  }

  /**
   * This method suite computes the value of a constant expression.
   * <p>
   * Okay, this is a bit messy.  For IdAddressOps, we have to ensure
   * that the declared entity is a constant.  The fact that this is a
   * constant is stored with the the type.
   * <p>
   * These method instances currently handle all IdAddressOps (i.e., the
   * subclasses of this class).  If SelectScopeOp and
   * SelectGlobalScopeOp ever get completed, this may have to be moved
   * down into IdReference.
   * <p>
   * @see IdAddressOp
   */
  public Literal getConstantValue()
  {
    return new AddressLiteral(getType(), getDecl(), 0);
  }
}

