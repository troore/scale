package scale.common;

/**
 * This enum specifies graphical display edge types - dashed,
 * dotted, etc.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public enum DEdge
{
  /**
   * Use a solid line for the edge.
   */
  SOLID("solid"),
  /**
   * An edge from a node to a predecessor node.
   */
  BACK("back"),
  /**
   * Use a dashed line for the edge.
   */
  DASHED("dashed"),
  /**
   * Use a dotted line for the edge.
   */
  DOTTED("dotted"),
  /**
   * Use a thick line for the edge.
   */
  THICK("thick");

  private String name;

  private DEdge(String name)
  {
    this.name = name;
  }

  public String sName()
  {
    return name;
  }
}
