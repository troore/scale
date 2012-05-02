package scale.score.expr;

/**
 * This enum specifies the comparison mode - equals,
 * not equals, etc.
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
public enum CompareMode
{
  /**
   * Equality compare
   */
  EQ(true, true, "=="),
  /**
   * Less than or equal compare
   */
  LE(true, false, "<="),
  /**
   * Less than compare
   */
  LT(false, false, "<"),
  /**
   * Greater than compare
   */
  GT(false, false, ">"),
  /**
   * Greater than or equal compare
   */
  GE(true, false, ">="),
  /**
   * Not equals compare
   */
  NE(false, true, "!=");

  private String  cName;
  private boolean eq;
  private boolean commutative;

  private CompareMode(boolean eq, boolean commutative, String cName)
  {
    this.eq          = eq;
    this.commutative = commutative;
    this.cName       = cName;
  }

  /**
   * CompareMode if the arguments are swapped.
   */
  public CompareMode argswap()
  {
    return argswapa[ordinal()];
  }

  /**
   * CompareMode if branch sense is reversed.
   */
  public CompareMode reverse()
  {
    return brreverse[ordinal()];
  }

  /**
   * True if comparison for equal included.
   */
  public boolean eq()
  {
    return eq;
  }

  /**
   * True if the comparison is commutative.
   */
  public boolean commutative()
  {
    return commutative;
  }

  /**
   * Return the C operator.
   */
  public String cName()
  {
    return cName;
  }

  private static final CompareMode[] argswapa  = {EQ, GE, GT, LT, LE, NE};
  private static final CompareMode[] brreverse = {NE, GT, GE, LE, LT, EQ};
}
