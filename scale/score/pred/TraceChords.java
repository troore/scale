package scale.score.pred;

import scale.common.*;
import scale.score.*;
import scale.score.expr.Expr;
import scale.score.chords.Chord;

/**
 * This class visits all the expressions in the a Scribble graph.
 * <p>
 * $Id: TraceChords.java,v 1.36 2007-10-04 19:58:34 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class visits all the expressions in the a Scribble graph, but
 * does so on a per statement basis.  Hence, each expression node may
 * be visited multiple times.  
 * <p>
 * For each statement in a Scribble graph, this class initiates a
 * traversal of the data dependence graph associated with the current
 * node.  The current statement is recorded in a "global" variable.
 */

public abstract class TraceChords extends Supertype 
{
  /**
   * The current statment in the traversal.
   */
  protected Chord thisChord = null;
  /**
   * Work list of expressions to visit.
   */
  protected Stack<Expr> wl = new Stack<Expr>();
  /**
   * Allows expressions to be visited in leaf -> root order.
   */
  protected Stack<Expr> rl = new Stack<Expr>();

  /**
   * Construct a predicate to visit the statements in a Scribble graph.
   */
  public TraceChords()
  {
    super();
  }

  /**
   * Return the current statment begin traversed.
   */
  public Chord getChord()
  {
    return thisChord;
  }

  /**
   * Set the statement to be used.
   * This method is used when only expressions are visited.
   * @param stmt is the statement associated with the expression.
   */
  protected void setChord(Chord stmt)
  {
    thisChord = stmt;
  }

  /**
   * Visit each statement and traverse the expression tree (if any)
   * connected to it.  The expression tree is traversed leaf-to-root
   * which is necessary for some algorithms such as alias analyses.
   * <p>
   * Note - this logic assumes that the expression tree is a tree and
   * not a dag.
   */
  public void visitChord(Chord c)
  {
    thisChord = c;

    int l = c.numInDataEdges();
    for (int i = 0; i < l; i++) {
      Expr exp = c.getInDataEdge(i);
      if (exp == null)
        continue;
      wl.push(exp);
      rl.push(exp);
    }

    while (!rl.empty()) {
      Expr r = rl.pop();

      int ll = r.numInDataEdges();
      for (int i = 0; i < ll; i++) {
        Expr exp = r.getInDataEdge(i);
        if (exp == null)
          continue;
        wl.push(exp);
        rl.push(exp);
      }
    }

    while (!wl.empty()) {
      Expr exp = wl.pop();
      exp.visit(this);
    }

    // rl & wl are empty upon exit.

    visitNote(c);
  }
}
