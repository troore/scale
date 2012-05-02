package scale.clef.decl;

/**
 * This enum specifies the visibility of a declaration - local,
 * global, etc.
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
public enum Visibility
{
  /**
   * Indicates that an entity is visible only within
   * its current scope.  This is the default visibility.
   */
  LOCAL("local"),
  /**
   * Indicates that the entity is visible only within
   * its file scope.  This attribute is intended to represent one meaning
   * of the C/C++ <b>static</b> construct.  Admittedly, this attribute
   * should be redundant with <tt>cLocal</tt> when at the file scope
   * level.  However, this captures the notion of C/C++'s <tt>static</tt>
   * construct; whereas, <tt>cLocal</tt> should never be used.</b>
   */
  FILE("file"),
  /**
   * Indicates that an entity is defined in, and has global visibility
   * outside, this file scope.
   */
  GLOBAL("global"),
  /**
   * Indicates that an entity is defined external to this file scope.
   * This attribute implements the common case of the C <b>extern</b>
   * construct.
   */
  EXTERN("extern");

  private final String name;

  private Visibility(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return name;
  }
}
