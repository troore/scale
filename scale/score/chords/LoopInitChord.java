package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/** 
 * This class is used to mark the beginning of the initialization for a loop.
 * <p>
 * $Id: LoopInitChord.java,v 1.7 2006-02-28 16:37:08 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents a non-action.
 * No code is generated for this node.
 * <p>
 * We need to mark the beginning of the loop initialization in order to perform
 * operations such as loop interchange.  Using the actual initialization
 * operations is unreliable because they may be eliminated by optimizations such
 * as copy propagation.
 * <p>
 * The LoopInitChord is inserted for explicit loops (e.g., <code>do</code> 
 * and <code>for</code> loops) and implicit loops (created by <code>goto</code>
 * statements). For implicit loops, the initializations will be before
 * the LoopInitChord instance.  Even for explicit loops there
 * is still a problem.  Consider
 * <pre>
 *   k = 0;
 *   for (; k < n; k++) { ... }
 * </pre>
 * In this case the LoopInitChord instance will be inserted
 * before the actual loop induction variable initialization.  Thus, the loop
 * transformation optimizations must also check to see  that everything, upon
 * which the initialization of every induction variable depends, occurs after the
 * LoopInitChord instance.
 * <p>
 * <img src="../../../loop.jpg" alt="Loop Example">
 * <p>
 * @see LoopHeaderChord
 * @see LoopPreHeaderChord
 * @see LoopExitChord
 * @see LoopTailChord
 */

public class LoopInitChord extends SequentialChord
{
  public LoopInitChord(Chord next)
  {
    super(next);
  }

  public LoopInitChord()
  {
    this(null);
  }

  public Chord copy()
  {
    Chord lic = new LoopInitChord(getNextChord());
    lic.copySourceLine(this);
    return lic;
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
    p.visitLoopInitChord(this);
  }

  /**
   * Return a String specifying the color to use for coloring this node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTGREY;
  }
}
