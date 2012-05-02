package scale.clef.decl;

import scale.common.*;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class declares a variable that was renamed from another variable.
 * <p>
 * $Id: RenamedVariableDecl.java,v 1.28 2007-08-28 13:34:32 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * see scale.score.SSA
 */
public final class RenamedVariableDecl extends VariableDecl
{
  private static int createdCount = 0; // A count of all the instances of this class created.

  /**
   * The variable that this variable is based upon..
   */
  private VariableDecl original; 

  /**
   * @param original is the variable that this variable was renamed from
   * @see scale.score.SSA
   */
  public RenamedVariableDecl(VariableDecl original)
  {
    super(genName(original.getName()), original.getType());
    this.original = original;
    if (original.isTemporary())
      setTemporary();
    createdCount++;
  }

  private static String genName(String nameRoot)
  {
    StringBuffer newName = new StringBuffer(nameRoot);
    newName.append("#s_");
    newName.append(String.valueOf(createdCount));
    return newName.toString();
  }

  public void visit(Predicate p)
  {
    p.visitRenamedVariableDecl(this);
  }

  /**
   * Return the variable that this variable was renamed from.
   */
  public VariableDecl getOriginal()
  {
    return original;
  }

  /**
   * Set the variable that this variable was renamed from.
   */
  public void setOriginal(VariableDecl original)
  {
    this.original = original;
    computeAttributes();
  }

  /**
   * Return true if the address of this Declaration has been taken.
   */
  public boolean addressTaken()
  {
    return super.addressTaken() || ((original != null) && original.addressTaken());
  }

  /**
   * Return true if the declaration is a virtual variable.
   */
  public boolean isVirtual()
  {
    return original.isVirtual();
  }

  /**
   * Return true if the declaration is a renamed variable.
   */
  public boolean isRenamed()
  {
    return true;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getValue();

    assert (i == 1) : "No such child " + i;

    return original;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 2;
  }

  /**
   * Return a copy of this Declaration but with a different name.
   * The name is ignored.
   */
  public Declaration copy(String name)
  {
    return new RenamedVariableDecl(original);
  }

  public final boolean isRenamedVariableDecl()
  {
    return true;
  }

  public final RenamedVariableDecl returnRenamedVariableDecl()
  {
    return this;
  }
}
