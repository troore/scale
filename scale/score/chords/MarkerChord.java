package scale.score.chords;

import scale.common.*;
import scale.score.Predicate;

/**
 * This class represents a "marker" node in the CFG.
 * <p>
 * $Id: MarkerChord.java,v 1.2 2007-03-21 13:32:12 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A marker records some information needed by the compiler.  No code
 * may generated for the node.
 */
public class MarkerChord extends SequentialChord
{
  private Object object;
  private long   flags;

  /**
   * Create a MarkerChord.
   * @param object is some <code>Object</code> or <code>null</code>
   * @param flags specifies the flags of marker
   */
  public MarkerChord(Object object, long flags, Chord next)
  {
    super(next);
    this.object = object;
    this.flags  = flags;
  }

  /**
   * Create a MarkerChord.
   * @param object is some <code>Object</code> or <code>null</code>
   * @param flags specifies the flags of marker
   */
  public MarkerChord(Object object, long flags)
  {
    this(object, flags, null);
  }

  public final long getFlags()
  {
    return flags;
  }

  public final Object getObject()
  {
    return object;
  }

  public Chord copy()
  {
    return new MarkerChord(object, flags, null);
  }

  public void visit(Predicate pred)
  {
    pred.visitMarkerChord(this);
  }

  /**
   * Return true if this is chord was added for the convenience of the
   * compiler and does not correspond to actual source code in the
   * user program.
   */
  public final boolean isSpecial()
  {
    return true;
  }

  /**
   * Return true if this is a marker chord.
   */
  public final boolean isMarker()
  {
    return true;
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.PURPLE;
  }
}
