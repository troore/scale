package scale.clef.decl;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class implement the semantics of a C typedef statement.
 * <p>
 * $Id: TypeName.java,v 1.26 2007-03-21 13:31:52 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see TypeDecl
 */

public class TypeName extends Declaration
{
  public TypeName(String name, Type type)
  {
    super(name, type);
  }

  public void visit(Predicate p)
  {
    p.visitTypeName(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return getType();
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name)
  {
    return new TypeName(name, getType());
  }

  public final boolean isTypeName()
  {
    return true;
  }

  public final TypeName returnTypeName()
  {
    return this;
  }
}
