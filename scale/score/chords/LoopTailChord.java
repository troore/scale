package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/** 
 * This class is used to collect the loop edges so that the loop
 * header has two and only two in-coming CFG edges.
 * <p>
 * $Id: LoopTailChord.java,v 1.33 2007-10-04 19:58:23 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents a non-action.
 * No code is generated for this node.
 * <p>
 * We need to mark the end of a loop so that there are only two
 * in-coming CFG edges to a {@link LoopHeaderChord LoopHeaderChord}
 * instance.  For example, consider the following C code:
 * <pre>
 * void tail(int k)
 * {
 *   int i;
 *   i = 0;
 *   while (i++ < k) {
 *     if (i == 2)
 *       continue;
 *     ftn(i);
 *   }
 * }
 * </pre>
 * We want to represent this as
 * <p>
 * <img src="../../../looptail.jpg" alt="LoopTailChord Example">
 * <p>
 * @see LoopHeaderChord
 * @see LoopPreHeaderChord
 * @see LoopExitChord
 * @see LoopInitChord
 */

public class LoopTailChord extends SequentialChord
{
  public LoopTailChord(Chord next)
  {
    super(next);
  }

  public LoopTailChord()
  {
    this(null);
  }

  public Chord copy()
  {
    return new LoopTailChord(getNextChord());
  }

  /**
   * Return the LoopHeaderChord associated with this loop tail.
   */
  public LoopHeaderChord getLoopHeader()
  {
    Chord nxt = getNextChord();
    while (nxt != null) {
      LoopHeaderChord lh = nxt.getLoopHeader();
      if (lh != null)
        return lh;
      nxt = nxt.getNextChord();
    }
    return null;
  }

  /**
   * Return true if this is the last Chord in this Basic Block.
   */
  public boolean isLastInBasicBlock()
  {
    return true;
  }

  /**
   * Return true if this is chord was added for the convenience of the compiler
   * and does not correspond to actual source code in the user program.
   */
  public boolean isSpecial()
  {
    return true;
  }

  /**
   * Return true if this CFG node is a {@link LoopTailChord
   * LoopTailChord} instance.
   */
  public boolean isLoopTail()
  {
    return true;
  }

  /**
   * Break any un-needed links from a Chord that is being deleted.
   */
  public void unlinkChord()
  {
    LoopHeaderChord header = getLoopHeader();
    if (header != null)
      header.setLoopTail(null);
    header = null;
  }

  public void visit(Predicate p)
  {
    p.visitLoopTailChord(this);
  }

  /**
   * Return a String specifying the color to use for coloring this node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTGREY;
  }
}
