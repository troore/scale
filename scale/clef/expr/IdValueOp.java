package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;

/**
 * A class which represents the value of a Declaration.
 * <p>
 * $Id: IdValueOp.java,v 1.13 2007-06-04 00:30:13 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class IdValueOp extends IdReferenceOp 
{
  /**
   * Create the representation of the value of the specified
   * declaration.
   */
  public IdValueOp(Declaration decl)
  {
    super(decl.getType(), decl);
    assert !decl.getType().isArrayType();
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    return getDecl().getName();
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(getDecl().getName());
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitIdValueOp(this);
  }

  /**
   * Returns true if the IdValueOp represents a constant value.
   */
  public boolean isConstant()
  {
    Object cval = getConstantValue();
    if (cval == null)
      return false;
    if (cval == scale.common.Lattice.Top)
      return false;
    return true;
  }

  /**
   * This method suite computes the value of a constant expression.
   * <p>
   * Okay, this is a bit messy.  For IdValueOps, we have to ensure
   * that the declared entity is a constant.  The fact that this is a
   * constant is stored with the the type.
   * <p>
   * These method instances currently handle all IdValueOps (i.e., the
   * subclasses of this class).  If SelectScopeOp and
   * SelectGlobalScopeOp ever get completed, this may have to be moved
   * down into IdReference.
   * <p>
   * @see IdValueOp
   */
  public Literal getConstantValue()
  {
    return getDecl().getConstantValue();
  }
}

