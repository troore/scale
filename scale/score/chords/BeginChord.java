package scale.score.chords;

import scale.common.*;
import scale.score.*;

/**
 * This class is used to represent the very first node in the CFG.
 * <p>
 * $Id: BeginChord.java,v 1.28 2005-02-07 21:28:34 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class BeginChord extends LoopHeaderChord
{
  private static int createdCount = 0; /* A count of all the created instances of this class. */

  static
  {
    Statistics.register("scale.score.chords.BeginChord", "created"); // Register this class as keeping a "number" statistic.
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  /**
   * Create the first node in the CFG.
   * @param scribble represents the entire CFG
   * @param next is the out-going CFG edge
   */
  public BeginChord(Scribble scribble, Chord next)
  {
    super(scribble, null, next);
    createdCount++;
  }

  /**
   * Create the first node in the CFG.
   * @param scribble represents the entire CFG
   */
  public BeginChord(Scribble scribble)
  {
    this(scribble, null);
  }

  public Chord copy()
  {
    throw new scale.common.InternalError("You can not copy the BeginChord.");
  }

  public void visit(Predicate p)
  {
    p.visitBeginChord(this);
  }

  public void addInCfgEdge(Chord node)
  {
    throw new InvalidMutationError("You cannot have anything before the BeginChord.");
  }

  /**
   * Return true if this loop is an actual loop in the program.
   */
  public boolean isTrueLoop()
  { 
    return false;
  }

  /**
   * Return <code>null</code> for the <code>LoopPreHeaderChord</code> instance for this "loop".
   */
  public LoopPreHeaderChord getPreHeader()
  {
    return null;
  }
}
