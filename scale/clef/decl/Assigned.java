package scale.clef.decl;

/**
 * This enum specifies where the declaration is allocated - memory,
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
public enum Assigned
{
  /**
   * The variable is not yet allocated.
   */
  NOT_ALLOCATED("not_allocated"),
  /**
   * The variable is allocated in memory.
   */
  IN_MEMORY("in_memory"),
  /**
   * The variable is an EquivalenceDecl.
   */
  IN_COMMON("in_common"),
  /**
   * The variable is allocated on the stack.
   */
  ON_STACK("on_stack"),
  /**
   * The variable is allocated in a register.
   */
  IN_REGISTER("in_register");

  private final String name;

  private Assigned(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return name;
  }
}
