package scale.clef.stmt;

import scale.common.*;

import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the Fortran 77 DO statement.
 * <p>
 * $Id: DoLoopStmt.java,v 1.34 2007-01-31 18:36:56 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class DoLoopStmt extends LoopStmt
{
  /**
   * The loop index expression.
   */
  private Expression index;     
  /**
   * The initial loop index value.
   */
  private Expression init;      
  /**
   * The maximum value of the index.
   */
  private Expression term;      
  /**
   * The increment amount.
   */
  private Expression inc;       

  /**
   * @param idx - the value expression representing the index variable
   * @param s - the statement to be iterated.
   * @param i - the initial value of the index.
   * @param t - the final value of the index.
   * @param ic - the step value for the index.
   */
  public DoLoopStmt(Expression idx,
                    Statement  s,
                    Expression i,
                    Expression t,
                    Expression ic)
  {
    super(s);

    setIndex(idx);
    setExprInit(i);
    setExprTerm(t);
    setExprInc(ic);
  }

  public void visit(Predicate p)
  {
    p.visitDoLoopStmt(this);
  }

  /**
   * Return the loop index expression.
   */
  public final Expression getIndex()
  {
    return index;
  }

  /**
   * Return the initial loop index value.
   */
  public final Expression getExprInit()
  {
    return init;
  }

  /**
   * Return the maximum value of the index.
   */
  public final Expression getExprTerm()
  {
    return term;
  }

  /**
   * Return the increment amount.
   */
  public final Expression getExprInc()
  {
    return inc;
  }

  /**
   * Specify the initial loop index value.
   */
  protected final void setIndex(Expression index)
  {
    this.index = index;
  }

  protected final void setExprInit(Expression init)
  {
    this.init = init;
  }

  /**
   * Specify the maximum value of the index.
   */
  protected final void setExprTerm(Expression term)
  {
    this.term = term;
  }

  /**
   * Specify the increment amount.
   */
  protected final void setExprInc(Expression inc)
  {
    this.inc = inc;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return getStmt();
    if (i == 1)
      return index;
    if (i == 2)
      return init;
    if (i == 3)
      return term;
    assert (i == 4) : "No such child " + i;
    return inc;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 5;
  }

  public void dump(String indents, java.io.PrintStream out)
  {
    out.print(Debug.formatInt(getSourceLineNumber(), 4));
    out.print(indents);
    out.print("do ");
    out.println(getIndex());
    out.print(Debug.formatInt(getSourceLineNumber(), 4));
    out.print(indents);
    out.print("   =    ");
    out.println(getExprInit());
    out.print(Debug.formatInt(getSourceLineNumber(), 4));
    out.print(indents);
    out.print("   to   ");
    out.println(getExprTerm());
    out.print(Debug.formatInt(getSourceLineNumber(), 4));
    out.print(indents);
    out.print("   step ");
    out.println(getExprInc());
    getStmt().dump(indents + "  ", out);
  }
}
