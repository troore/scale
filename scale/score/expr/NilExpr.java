package scale.score.expr;

import scale.common.*;

import scale.clef.expr.Literal;
import scale.score.Predicate;
import scale.clef.type.*;
import scale.clef.decl.Declaration;
import scale.clef.LiteralMap;

/**
 * This class represents the null or nil address.
 * <p>
 * $Id: NilExpr.java,v 1.33 2007-04-27 18:04:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class NilExpr extends ValueExpr
{
  public NilExpr(Type t)
  {
    super(t);
  }

  /**
   * The expression type is generated.
   */
  public NilExpr()
  {
    this(PointerType.create(VoidType.type));
  }

  public Expr copy()
  {
    return new NilExpr(getType());
  }

  public void visit(Predicate p)
  {
    p.visitNilExpr(this);
  }

  public String getDisplayLabel()
  {
    return "null";
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    return getConstantValue();
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return LiteralMap.put(0, getCoreType());
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public boolean optimizationCandidate()
  {
    return true;
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 0;
  }
}
