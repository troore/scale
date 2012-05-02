package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/** 
 * This class is used to mark the exit point of loops.
 * <p>
 * $Id: LoopExitChord.java,v 1.32 2006-02-28 16:37:08 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents a non-action.
 * No code is generated for this node.
 * <emph>Not all loops have LoopExitChord nodes.</emph>  Some may have more than one.
 * <pre>
 * int sumfor2(int n)
 * {
 *   int sum, i;
 *   sum = 0;
 *   for (i = 0; i < n; i++) {
 *     sum += i;
 *     if (sum > 100)
 *       return -1;
 *   }
 *   return sum;
 * }
 * <pre>
 * <p>
 * <img src="../../../loop2x.jpg" alt="Loop with Two Exits">
 * <p>
 * @see LoopHeaderChord
 * @see LoopPreHeaderChord
 * @see LoopHeaderChord
 * @see LoopTailChord
 * @see LoopInitChord
 */

public class LoopExitChord extends SequentialChord
{
  /**
   * The loop header where the chord exits from.
   */ 
  private LoopHeaderChord header; 

  public LoopExitChord(LoopHeaderChord header, Chord next)
  {
    super(next);
    setLoopHeader(header);
  }

  public LoopExitChord(LoopHeaderChord header)
  {
    this(header, null);
  }

  /**
   * Return the LoopHeaderChord associated with this loop exit.
   */
  public LoopHeaderChord getLoopHeader()
  {
    return header;
  }

  /**
   * Specify the loop header of this loop.
   * @param header is the loop header of this loop
   */
  public void setLoopHeader(LoopHeaderChord header)
  {
    if (this.header != null)
      this.header.removeLoopExit(this);

    this.header = header;
    if (header != null)
      header.addLoopExit(this);
  }

  public Chord copy()
  {
    Chord lec = new LoopExitChord(header, getNextChord());
    lec.copySourceLine(this);
    return lec;
  }

  /**
   * Break any un-needed links from a Chord that is being deleted.
   */
  public void unlinkChord()
  {
    if (header != null)
      header.removeLoopExit(this);
    header = null;
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
   * Return true if this chord is a LoopExitChord.
   */
  public final boolean isLoopExit()
  {
    return true;
  }

  public void visit(Predicate p)
  {
    p.visitLoopExitChord(this);
  }

  /**
   * Return a String containing additional information about this Chord.
   */
  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(header);
    return buf.toString();
  }

  /**
   * Return the LoopExitChord, for the specified loop, that is  reachable from this Chord.
   * Return null if none is found.
   */
  public final LoopExitChord findLoopExit(LoopHeaderChord header)
  {
    if (this.header == header) // If LoopExitChord for this loop.
      return this;
    return null;
  }

  /**
   * Return a String specifying the color to use for coloring this node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTGREY;
  }

  /**
   * Check this node for validity.
   * This method throws an exception if the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    int l = header.numLoopExits();
    for (int i = 0; i < l; i++)
      if (header.getLoopExit(i) == this)
        return;
    throw new scale.common.InternalError("loop exit " + this + " -> " + header);
  }
}
