package scale.clef.type;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * Un-used.
 * <p>
 * $Id: RaiseWithType.java,v 1.28 2005-03-24 13:57:11 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class RaiseWithType extends Raise
{
  /**
   * The type.
   */
  private Type type;

  public RaiseWithType(Type t)
  {
    type = t;
  }

  public Type getType()
  {
    return type;
  }

  public void visit(Predicate p)
  {
    p.visitRaiseWithType(this);
  }

  public String toStringSpecial()
  {
    return "";
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return type;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }
}
