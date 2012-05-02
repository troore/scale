package scale.clef.decl;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents the declaration of a procedure.
 * <p>
 * $Id: ProcedureDecl.java,v 1.39 2007-10-04 19:58:04 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ProcedureDecl extends RoutineDecl
{
  /**
   * The nesting level.
   */
  private int level;
  /**
   * If level > 0, the parent routine.
   */
  private RoutineDecl parentRoutine;

  private static String fixName(String name)
  {
    if (name.startsWith("__builtin_"))
      name = "_scale_" + name.substring(10);
    return name;
  }

  /**
   * Create a new procedure declaration.
   * @param name is the procedure name
   * @param type is the procedure type
   * @param level is the nesting level of this procedure
   * @param parentRoutine is the parent (enclosing) routine or null
   * @param body is the body of the procedure
   */
  public ProcedureDecl(String        name,
                       ProcedureType type,
                       int           level,
                       RoutineDecl   parentRoutine,
                       Statement     body)
  {
    super(fixName(name), type, body);
    this.level = level;
    this.parentRoutine = parentRoutine;
  }

  /**
   * Create a new procedure declaration.
   * @param name is the procedure name
   * @param type is the procedure type
   * @param level is the nesting level of this procedure
   * @param parentRoutine is the parent (enclosing) routine or null
   */
  public ProcedureDecl(String        name,
                       ProcedureType type,
                       int           level,
                       RoutineDecl   parentRoutine)
  {
    this(name, type, level, parentRoutine, null);
  }

  /**
   * Create a new procedure declaration.
   * @param name is the procedure name
   * @param type is the procedure type
   */
  public ProcedureDecl(String name, ProcedureType type)
  {
    this(name, type, 0, null, null);
  }

  /**
   * Create a new procedure declaration.
   * @param name is the procedure name
   * @param type is the procedure type
   * @param level is the nesting level of this procedure
   * @param body is the body of the procedure
   */
  public ProcedureDecl(String        name,
                       ProcedureType type,
                       int           level,
                       Statement     body)
  {
    this(name, type, level, null, body);
  }

  public String toStringSpecial()
  {
    return super.toStringSpecial() + " (level " + level + ")";
  }

  public void visit(Predicate p)
  {
    p.visitProcedureDecl(this);
  }

  /**
   * Return the nested depth of this routine.
   * This value is currently always 0.
   */
  public final int getLevel()
  {
    return level;
  }

  /**
   * Return the enclosing routine of this routine.
   * This value is currently always <code>null</code>.
   */
  public final RoutineDecl getParentRoutine()
  {
    return parentRoutine;
  }

  /**
   * Set the nested depth of this routine.
   * This value is currently always 0.
   */
  public final void setLevel(int lev)
  {
    level = lev;
  }

  /**
   * Set the enclosing routine of this routine.
   * This value is currently always <code>null</code>.
   */
  public final void setParentRoutine(RoutineDecl parentRoutine)
  {
    this.parentRoutine = parentRoutine;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getBody();

    assert (i == 1) : "No such child " + i;

    return parentRoutine;
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
   */
  public Declaration copy(String name)
  {
    return new ProcedureDecl(name,
                             getSignature(),
                             level,
                             parentRoutine,
                             getBody());
  }

  public final boolean isProcedureDecl()
  {
    return true;
  }

  public final ProcedureDecl returnProcedureDecl()
  {
    return this;
  }
}
