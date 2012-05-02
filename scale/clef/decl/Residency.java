package scale.clef.decl;

/**
 * This enum specifies the residency of a declaration - auto,
 * register, etc.
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
public enum Residency
{
  /**
   * Marks a data value as being locally allocated (i.e., on the
   * stack).  Local allocation is the default form of allocation and
   * so seldom (if ever) needs to be explicitly specified.  This
   * attribute represents C/C++'s <b>auto</b> construct.
   */
  AUTO("auto"),
  /**
   * Recommends that a value be assigned to a register.  If the value
   * cannot be assigned to a register, it should be allocated on the
   * stack.
   */
  REGISTER("register"),
  /**
   * Recommends that an entity be assigned to permanently allocated
   * space.
   */
  MEMORY("memory");

  private final String name;

  private Residency(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return name;
  }
}
