package scale.clef.decl;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * UnknownFormals are used to specify that the remaining parameter
 * declarations of the function are unknown.
 * <p>
 * $Id: UnknownFormals.java,v 1.29 2007-10-04 19:58:04 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class UnknownFormals extends FormalDecl
{
  /**
   * Construct a unknown formal parameter list (&hellip; in C).
   * Creates a Formal Declaration with the name
   * <code>__unknown__</code>, no type, pass by value, and no default
   * expression.
   */
  public UnknownFormals()
  {
    this(VoidType.type);
  }

  /**
   * Construct a unknown formal parameter list (&hellip; in C).
   */
  public UnknownFormals(Type type)
  {
    super("__unknown__", type, ParameterMode.VALUE);
  }

  public void visit(Predicate p)
  {
    p.visitUnknownFormals(this);
  }

  /**
   * Return a copy of this Declaration but with a different name.
   * The name is ignored.
   */
  public Declaration copy(String name)
  {
    return new UnknownFormals(getType());
  }

  public final boolean isUnknownFormals()
  {
    return true;
  }

  public final UnknownFormals returnUnknownFormals()
  {
    return this;
  }
}
