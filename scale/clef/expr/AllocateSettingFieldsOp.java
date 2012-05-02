package scale.clef.expr;

import java.util.AbstractCollection;
import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents a heap allocation operation for an aggregate
 * entity and then uses the specified positional arguments to initialize
 * the entity. 
 * <p>
 * $Id: AllocateSettingFieldsOp.java,v 1.35 2007-08-28 17:58:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The result type of the operation should be a pointer to
 * the object type (ie., a pointer to <tt>type</tt>. 
 */
public class AllocateSettingFieldsOp extends HeapOp
{
  private Vector<Expression> argList; // A vector of arguments.

  /**
   * @param rt is the type of the result
   * @param alloctype is the type of the allocated object, not the type of the result
   * @param al is the list of initialization expressions
   */
  public AllocateSettingFieldsOp(Type rt, Type alloctype, Vector<Expression> al)
  {
    super(rt, alloctype);
    setArgList(al);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    return false;
  }

  public void visit(Predicate p)
  {
    p.visitAllocateSettingFieldsOp(this);
  }

  /**
   * Return the list of initialization expressions.
   */
  public final Enumeration<Expression> getArgList()
  {
    return argList.elements();
  }

  /**
   * Specify the list of initialization expressions.
   */
  protected final void setArgList(Vector<Expression> argList)
  {
    this.argList = argList;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getAllocType();

    return argList.elementAt(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1 + argList.size();
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    int l = argList.size();
    for (int i = 0; i < l; i++) {
      Expression exp = argList.get(i);
      exp.getDeclList(varList);
    }
  }
}
