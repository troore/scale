package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;

/**
 * A class which represents a reference to a Declaration.
 * <p>
 * $Id: IdReferenceOp.java,v 1.43 2007-08-28 17:58:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class IdReferenceOp extends Expression 
{
  /**
   * The declaration associated with the IdReferenceOp.
   */
  private Declaration decl;

  public IdReferenceOp(Type type, Declaration decl)
  {
    super(type);
    this.decl = decl;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    IdReferenceOp op = (IdReferenceOp) exp;
    return (decl == op.decl);
  }

  public void visit(Predicate p)
  {
    p.visitIdReferenceOp(this);
  }

  /**
   * Return the declaration associated with this reference.
   */
  public final Declaration getDecl()
  {
    return decl;
  }

  /**
   * Specify the declaration associated with this reference.
   */
  protected final void setDecl(Declaration decl)
  {
    this.decl = decl;
  }

  /**
   * This method suite computes the value of a constant expression.
   * <p>
   * Okay, this is a bit messy.  For IdReferenceOps, we have to ensure
   * that the declared entity is a constant.  The fact that this is a
   * constant is stored with the the type.
   * <p>
   * These method instances currently handle all IdReferenceOps (i.e., the
   * subclasses of this class).  If SelectScopeOp and
   * SelectGlobalScopeOp ever get completed, this may have to be moved
   * down into IdReference.
   */
  public Literal getConstantValue()
  {
    return decl.getConstantValue();
  }

  /**
   * Return true if this expression contains a reference to the variable.
   */
  public final boolean containsDeclaration(Declaration decl)
  {
    return (this.decl == decl);
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    varList.add(decl);
  }
}
