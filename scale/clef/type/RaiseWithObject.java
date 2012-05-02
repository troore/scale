package scale.clef.type;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * Un-used.
 * <p>
 * $Id: RaiseWithObject.java,v 1.27 2005-03-24 13:57:11 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class RaiseWithObject extends Raise
{
  /**
   * The exception.
   */
  private ExceptionDecl exception;

  public RaiseWithObject(ExceptionDecl e)
  {
    exception = e;
  }

  public final ExceptionDecl getException()
  {
    return exception;
  }

  public void visit(Predicate p)
  {
    p.visitRaiseWithObject(this);
  }

  protected final void setException(ExceptionDecl e)
  {
    exception = e;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return exception;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  public String toStringSpecial()
  {
    return "";
  }
}
