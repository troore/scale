package scale.clef.decl;

import scale.common.*;

import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * A FileDecl is a collection of all the declarations in a source file.
 * <p>
 * $Id: FileDecl.java,v 1.40 2007-05-10 16:48:02 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class FileDecl extends Declaration
{
  private Vector<Declaration> decls; // A vector of the declarations in this file.

  public FileDecl(String name)
  {
    this(name, null);
  }

  public FileDecl(String name, Vector<Declaration> decls)
  {
    super(name);
    setDecls(decls);
  }

  public void visit(Predicate p)
  {
    p.visitFileDecl(this);
  }

  /**
   * Specify the set of declarations that comprise the source file.
   */
  public void setDecls(Vector<Declaration> decls)
  {
    this.decls = decls;
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name)
  {
    throw new scale.common.InternalError("Can't copy a FileDecl.");
  }

  /**
   * Return the <code>i</code>-th declaration.
   */
  public final Declaration getDecl(int i)
  {
    return decls.elementAt(i);
  }

  /**
   * Return the number of declarations.
   */
  public final int getNumDecls()
  {
    if (decls == null)
      return 0;

    return decls.size();
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    return (Node) decls.elementAt(i);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    if (decls == null)
      return 0;

    return decls.size();
  }

  public final boolean isFileDecl()
  {
    return true;
  }

  public final FileDecl returnFileDecl()
  {
    return this;
  }
}
