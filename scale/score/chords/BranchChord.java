package scale.score.chords;

/** 
 * This class is a parent class for branching statements.
 * <p>
 * $Id: BranchChord.java,v 1.17 2005-02-07 21:28:34 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Branching statements have a single outgoing control edge.
 */
public abstract class BranchChord extends SequentialChord
{
  public BranchChord(Chord target)
  {
    super(target);
  }

  /**
   * Link child to parent if it's a SequentialChord and not a BranchChord or EndChord. 
   */
  public void linkTo(Chord child)
  {
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5;
  }
}
