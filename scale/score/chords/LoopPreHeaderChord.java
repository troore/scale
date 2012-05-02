package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/** 
 * This class represents the start of a loop but is not part of the loop.
 * <p>
 * $Id: LoopPreHeaderChord.java,v 1.30 2006-02-28 16:37:08 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * No code is generated for this node.
 * The out-going CFG edge always points to a {@link LoopHeaderChord LoopHeaderChord}.
 * <p>
 * We need to mark the end of a loop so that there are only two in-coming CFG edges
 * to a {@link LoopHeaderChord LoopHeaderChord} instance.  
 * <p>
 * Don't assume that a LoopPreHeaderChord instance will always end a basic block.
 * For example, a
 * <pre>
 *   do { } while (false);
 * </pre>
 * will not have a {@link LoopTailChord LoopTailChord} after SCC.
 * <p>
 * <img src="../../../loop.jpg" alt="Loop Example">
 * <p>
 * @see LoopHeaderChord
 * @see LoopTailChord
 * @see LoopExitChord
 * @see LoopInitChord
 */

public class LoopPreHeaderChord extends SequentialChord
{
  public LoopPreHeaderChord(Chord next)
  {
    super(next);
  }

  public LoopPreHeaderChord()
  {
    this(null);
  }

  public Chord copy()
  {
    Chord lph = new LoopPreHeaderChord(getNextChord());
    lph.copySourceLine(this);
    return lph;
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
   * Return true if this chord is a LoopPreHeaderChord.
   */
  public final boolean isLoopPreHeader()
  {
    return true;
  }

  public void visit(Predicate p)
  {
    p.visitLoopPreHeaderChord(this);
  }

  /**
   * Return a String specifying the color to use for coloring this node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTGREY;
  }
}
