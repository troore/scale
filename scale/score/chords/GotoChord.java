package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/** 
 * This class represents a goto statement.
 * <p>
 * $Id: GotoChord.java,v 1.21 2006-01-25 19:47:59 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p> 
 * We really don't want gotos in Score, but having this
 * class simplifies the translation from Clef to Scribble.  These nodes are
 * removed from Score (Scribble) as soon as the initial, complete Cfg is generated.
 * They are replaced by a Cfg edge.
 */

public class GotoChord extends BranchChord
{
  public GotoChord(Chord target)
  {
    super(target);
  }

  public Chord copy()
  {
    Chord gc = new GotoChord(getTarget());
    gc.copySourceLine(this);
    return gc;
  }

  public void visit(Predicate p)
  {
    p.visitGotoChord(this);
  }
}
