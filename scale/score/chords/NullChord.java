package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/** 
 * This class is used as a place holder in the construction of the CFG.
 * <p>
 * $Id: NullChord.java,v 1.23 2005-02-07 21:28:36 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents a non-action.
 * We really don't want null actions in Score, but having this
 * class simplifies the translation from Clef to Scribble.  These nodes are
 * removed from Score (Scribble) as soon as the initial, complete Cfg is generated.
 */

public class NullChord extends SequentialChord
{
  /**
   * Create a NullChord.
   * @param next is the out-going CFG edge and may be null
   */
  public NullChord(Chord next)
  {
    super(next);
  }

  public NullChord()
  {
    this(null);
  }

  public Chord copy()
  {
    return new NullChord(getNextChord());
  }

  /**
   * Return true if this is chord was added for the convenience of the compiler
   * and does not correspond to actual source code in the user program.
   */
  public boolean isSpecial()
  {
    return true;
  }

  public void visit(Predicate p)
  {
    p.visitNullChord(this);
  }
}
