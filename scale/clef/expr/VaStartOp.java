package scale.clef.expr;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;

/**
 * A class which represents the va_start(va_list, parmN) C construct in the Clef AST.
 * <p>
 * $Id: VaStartOp.java,v 1.25 2005-03-24 13:57:05 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class VaStartOp extends VarArgOp
{
  /**
   * The reference parameter of the calling function.
   */
  private FormalDecl parmN; 

  public VaStartOp(Type type, Expression va_list, FormalDecl pn)
  {
    super(type, va_list);
    parmN = pn;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    VaStartOp op = (VaStartOp) exp;
    return (parmN == op.parmN);
  }

  public void visit(Predicate p)
  {
    p.visitVaStartOp(this);
  }

  /**
   * Return the reference parameter of the calling function.
   */
  public final FormalDecl getParmN()
  {
    return parmN;
  }

  /**
   * Specify the reference parameter of the calling function.
   */
  protected final void setParmN(FormalDecl pn)
  {
    parmN = pn;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getVaList();

    return parmN;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }
}
