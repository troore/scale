package scale.score.chords;

import scale.common.*;
import scale.score.expr.Expr;
import scale.score.expr.PhiExpr;
import scale.score.expr.ExprPhiExpr;

import scale.score.Predicate;

/** 
 * This class is used to represent a node in a CFG that contains a
 * PhiExpr expression.
 * <p>
 * $Id: PhiExprChord.java,v 1.29 2007-10-04 19:58:23 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see scale.score.expr.PhiExpr
 */

public class PhiExprChord extends ExprChord
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  static
  {
    Statistics.register("scale.score.chords.PhiExprChord", "created");
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * Create a node that holds a Phi function computation.
   * @param lhs is the target of the assignment or <code>null</code>
   * @param rhs is the source expression for the assignment or <code>null</code>
   * @param next is the out-going CFG edge and may be null
   */
  public PhiExprChord(Expr lhs, Expr rhs, Chord next)
  {
    super(lhs, rhs, next);
    createdCount++;
  }

  /**
   * Create a node that holds a computation.
   * @param rhs is the source expression or <code>null</code>
   * @param next is the out-going CFG edge and may be null
   */
  public PhiExprChord(Expr rhs, Chord next)
  {
    this(null, rhs, next);
  }

  /**
   * Create a node that holds a computation.
   * @param rhs is the source expression or <code>null</code>
   */
  public PhiExprChord(Expr rhs)
  {
    this(null, rhs, null);
  }

  /**
   * Create a node that holds a computation.
   * @param lhs is the target of the assignment or <code>null</code>
   * @param rhs is the source expression for the assignment or
   * <code>null</code>
   */
  public PhiExprChord(Expr lhs, Expr rhs)
  {
    this(lhs, rhs, null);
  }

  public Chord copy()
  {
    PhiExprChord ec = null;
    Expr lhs = getLValue();
    Expr rhs = getRValue();

    if (lhs != null) {
      if (rhs != null)
        ec = new PhiExprChord(lhs.copy(), rhs.copy(), getNextChord());
    } else if (rhs != null)
      ec = new PhiExprChord(null, rhs.copy(), getNextChord());

    if (ec == null)
      ec = new PhiExprChord(null, null, getNextChord());

    return ec;
  }

  /**
   * Return the Phi function of this Chord.
   */
  public PhiExpr getPhiFunction()
  {
    return (PhiExpr) getRValue();
  }

  /**
   * Return true if this is chord was added for the convenience of the
   * compiler and does not correspond to actual source code in the
   * user program.
   */
  public boolean isSpecial()
  {
    return true;
  }

  /**
   * Return true if this chord is a PhiExprChord.
   */
  public final boolean isPhiExpr()
  {
    return true;
  }

  public void visit(Predicate p)
  {
    p.visitPhiExprChord(this);
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTGREY;
  }
}
