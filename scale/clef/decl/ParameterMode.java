package scale.clef.decl;

/**
 * This enum specifies the parameter passing mode - by value,
 * by areference, etc.
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
public enum ParameterMode
{
  /**
   * The formal parameter is the actual value.  This represents
   * pass-by-value but the formal's value may be altered.  This mode
   * is the default for Modula-3 and the only one for C/C++.
   */
  VALUE("value"),
  /**
   * The formal parameter is an alias for the argument.  This
   * represents pass-by-reference where the address of the value is
   * passed as the argument.  Therefore, an update to the formal
   * parameter is a direct update the argument (actual parameter) as
   * well.  This mode is the only parameter mode for Fortran and
   * represents the <tt>var</tt> mode for Modula-3.
   */
  REFERENCE("reference"),
  /**
   * The formal parameter is the actual value.  This represents
   * pass-by-value but the formal's value may NOT be altered.  This
   * mode is used for Ada.
   */
  IN_VALUE("in-value"),
  /**
   * The formal parameter is an alias for the argument.  This
   * represents pass-by-reference where the address of the value is
   * passed as the argument and represents copy-in and copy-out
   * semantics.  This mode is used for Ada's <tt>inout</tt> mode.
   */
  VALUE_RESULT("ValueResult"),
  /**
   * The formal parameter is an alias for the argument.  This
   * represents pass-by-reference where the address of the value is
   * passed as the argument but the value cannot be updated.  This
   * mode supports Modula-3's <tt>readonly</tt> mode, and is efficient
   * for large data structures.
   */
  READ_ONLY("read-only");

  private final String name;

  private ParameterMode(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return name;
  }
}
