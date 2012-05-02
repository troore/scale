package scale.clef.decl;

import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * A class representing the declaration of a new type.
 * <p>
 * $Id: TypeDecl.java,v 1.26 2007-03-21 13:31:52 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see TypeName
 */

public class TypeDecl extends Declaration
{
  public TypeDecl(String name, Type type)
  {
    super(name, type);
  }

  public void visit(Predicate p)
  {
    p.visitTypeDecl(this);
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

  public Declaration copy(String name)
  {
    return new TypeDecl(name, getType());
  }

  public final boolean isTypeDecl()
  {
    return true;
  }

  public final TypeDecl returnTypeDecl()
  {
    return this;
  }
}
