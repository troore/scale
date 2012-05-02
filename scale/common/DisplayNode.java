package scale.common;

/**
 * All classes whose instances will be displayed graphically by a
 * {@link scale.common.DisplayGraph DisplayGraph} must implement this
 * interface.
 * <p>
 * $Id: DisplayNode.java,v 1.18 2006-12-05 21:02:07 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see DisplayGraph
 */
public interface DisplayNode
{
  /**
   * Return the <b>unique</b> node identifier.
   */
  public String getDisplayName();

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.
   */
  public String getDisplayLabel();

  /**
   * Return an interger specifying the color to use for coloring this
   * node in a graphical display.  The color specified is not
   * guaranteed to be used.
   * @see DColor
   */
  public DColor getDisplayColorHint();

  /**
   * Return an integer specifying a shape to use when drawing this
   * node in a graphical display.  The shape specified is not
   * guaranteed to be used.
   * @see DShape
   */
  public DShape getDisplayShapeHint();
}
