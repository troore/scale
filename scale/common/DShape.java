package scale.common;

/**
 * This enum specifies graphical display shapes - box,
 * circle, etc.
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
public enum DShape
{
  BOX("box"),
  RHOMBUS("rhombus"),
  CIRCLE("circle"),
  ELLIPSE("ellipse");

  private String name;

  private DShape(String name)
  {
    this.name = name;
  }

  public String sName()
  {
    return name;
  }
}
