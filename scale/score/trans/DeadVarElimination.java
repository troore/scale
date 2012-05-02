package scale.score.trans;

import scale.common.*;
import scale.score.*;
import scale.clef.LiteralMap;

/**
 * This class performs dead variable elimination.
 * <p>
 * $Id: DeadVarElimination.java,v 1.9 2007-02-28 18:00:36 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <p>
 * A variable is dead if
 * <ul>
 * <li>it is not a global variable,
 * <li>its address is not taken,
 * <li>it is not a COMMON variable,
 * <li>it is not a <code>static</code> variable,
 * <li>it is not a function parameter,
 * <li>the expression ssspecifying its value has side effects, and
 * <li>its value is not used.
 * </ul>
 * <p>
 * Unlike most optimizations this optimization never increases
 * register pressure.  Consequently, there is never a case where a
 * {@link scale.score.trans#legal legal} transformation is not also
 * {@link scale.score.trans#legal beneficial}.
 */

public class DeadVarElimination extends Optimization
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * If true, use heuristics that prune the cases where the
   * optimization is applied.
   */
  public static boolean useHeuristics;

  public DeadVarElimination(Scribble scribble)
  {
    super(scribble, "_dv");
  }

  /**
   * Removes statements that set the value of a variable that is not
   * used.  See Algorithm 19.12 in "Modern Compiler Implementation in
   * Java" by Appel.
   * @see scale.score.SSA#coalesceVariables
   */
  public void perform()
  {
    boolean ssa = Scribble.notSSA != scribble.inSSA();
    scribble.removeDeadVariables(ssa);
  }

  /**
   * Return whether this optimization requires that the CFG be in SSA
   * form.
   */
  public int requiresSSA()
  {
    return NO_SSA;
  }
}
