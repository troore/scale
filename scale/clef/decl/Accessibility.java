package scale.clef.decl;

/**
 * This enum specifies the accessibility of a declaration - public,
 * private, etc.
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
public enum Accessibility
{
  /**
   * Specifies that the method is accessible outside of its namespace.
   * This value is the default.
   */
  PUBLIC("public"),
  /**
   * Specifies than an identifier is accessible only within its
   * namespace.  This attribute may only be used for class members, in
   * which case it denotes the semantics of C++'s <b>protected</b>
   * construct.
   */
  PROTECTED("protected"),
  /**
   * Specifies that the method is accessible only within its
   * namespace.
   */
  PRIVATE("private");

  private final String name;

  private Accessibility(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return name;
  }
}
