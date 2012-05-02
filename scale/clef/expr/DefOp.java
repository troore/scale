package scale.clef.expr;

import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * This class allows a temporary variable, created by the compiler, to be added to the
 * appropriate places.
 * <p>
 * $Id: DefOp.java,v 1.4 2005-02-07 21:27:56 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The value of this expression is the expression argument.
 */
public class DefOp extends MonadicOp 
{
  private Declaration decl;

  public DefOp(Declaration decl, Expression exp)
  {
    super(exp.getType(), exp);
    this.decl = decl;
  }

  public Declaration getDecl()
  {
    return decl;
  }

  public void visit(Predicate p)
  {
    p.visitDefOp(this);
  }
}
