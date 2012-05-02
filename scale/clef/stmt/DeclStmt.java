package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents a statement that declares something such as a variable.
 * <p>
 * $Id: DeclStmt.java,v 1.22 2005-03-24 13:57:06 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class DeclStmt extends Statement
{
  /**
   * The declaration.
   */
  private Declaration decl;

  public DeclStmt(Declaration decl)
  {
    this.decl = decl;
  }

  public void visit(Predicate p)
  {
    p.visitDeclStmt(this);
  }

  /**
   * Return the declaration.
   */
  public final Declaration getDecl()
  {
    return decl;
  }

  /**
   * Specify the declaration.
   */
  protected final void setDecl(Declaration decl)
  {
    this.decl = decl;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return decl;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }
}
