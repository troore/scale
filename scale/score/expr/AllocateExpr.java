package scale.score.expr;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.PointerType;
import scale.clef.type.VoidType;
import scale.score.Predicate;

/**
 * This class represents the Allocate function. 
 * <p>
 * $Id: AllocateExpr.java,v 1.18 2007-10-04 19:58:28 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The operand specifies the amount of space to allocate.
 * The value of the operation is the address of the allocated area.
 * <p>
 * We use instances of this class instead of generating functions
 * calls because it may be possible to do allocations by simple
 * pointer operations in registers. By using this class, Scribble2C
 * can generate a function call.  When going direct to machine code,
 * an instance of this class can be converted directly into machine
 * instructions.
 */
public class AllocateExpr extends UnaryExpr 
{
  /**
   * True if the memory for the allocated object should be cleared to zeros.
   */
  private boolean initialize = false;

  /**
   * @param type is the expression type
   * @param arg is the left operand
   * @param initialize is true if the memory for the allocated object
   * should be cleared to zeros
   */
  public AllocateExpr(Type type, Expr arg, boolean initialize)
  {
    super(type, arg);
    this.initialize = initialize;
  }

  /**
   * The expression type is a (void*).
   * @param arg is the left operand
   */
  public AllocateExpr(Expr arg)
  {
    this(PointerType.create(VoidType.type), arg, false);
  }

  public Expr copy()
  {
    return new AllocateExpr(getType(), getArg().copy(), initialize);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | SE_STATE;
  }

  public void visit(Predicate p)
  {
    p.visitAllocateExpr(this);
  }

  public String getDisplayLabel()
  {
    return "allocate";
  }

  /**
   * Return true if the memory for the allocated object should be
   * cleared to zeros.
   */
  public boolean getInitializationMode()
  {
    return initialize;
  }
}
