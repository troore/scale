package scale.clef.expr;

/**
 * This enum specifies the type conversion - address cast,
 * real, etc.
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
public enum CastMode
{
  /**
   * No cast needed.
   */
  NONE("None"),
  /**
   * Cast an address to the specified type.
   */
  CAST("Cast"),
  /**
   * Return the real value part.
   */
  REAL("Real"),
  /**
   * Return the imaginary part.  Returns 0 for everything except a complex value.
   */
  IMAGINARY("Imaginary"),
  /**
   * Return the next lower integer value.
   * This is the same computation as the Fortran AINT() function.
   */
  FLOOR("Floor"),
  /**
   * Return the next higher integer value.
   */
  CEILING("Ceiling"),
  /**
   * Return the nearest integer value.
   * This is the same computation as the Fortran ANINT() function.
   */
  ROUND("Round"),
  /**
   * Return the integer part of the value.
   */
  TRUNCATE("Truncate"),
  /**
   * No valid cast.
   */
  INVALID("invalid");

  private final String name;

  private CastMode(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return name;
  }
}
