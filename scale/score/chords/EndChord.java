package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/**
 * This class represents the very last node in the CFG.  There can only be one.
 * <p>
 * $Id: EndChord.java,v 1.22 2005-02-07 21:28:35 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class EndChord extends SequentialChord
{
  /**
   * Create the very last node in the CFG.
   */
  public EndChord()
  {
    super();
  }

  public Chord copy()
  {
    throw new scale.common.InternalError("You can not copy the EndChord.");
  }

  /**
   * Link child to parent if it's a SequentialChord and not a BranchChord or EndChord. 
   */
  public void linkTo(Chord child)
  {
  }

  public void visit(Predicate p)
  {
    p.visitEndChord(this);
  }

  /**
   * Set the out-going CFG edge of this node (i.e., target of the branch).
   * The old out-going CFG edge, if any, is replaced.
   * The validity of the CFG graph is maintained.
   * @param target is the out-going CFG edge
   */
  public void setTarget(Chord target)
  {
    throw new InvalidMutationError("You cannot have anything after the EndChord.");
  }
}
