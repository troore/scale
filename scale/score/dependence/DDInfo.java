package scale.score.dependence;

import scale.common.*;

/**
 * A class which represents data dependence information between
 * two array references.
 * <p>
 * $Id: DDInfo.java,v 1.20 2007-10-04 19:58:24 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents both direction
 * and distance vectors.  The dependence information for a loop
 * nest is an array of <i>n</i> elements, where <i>n</i> is the
 * deepest common loop nest.
 * <p>
 * The direction vector represents dependence information for each
 * loop level.  We represent the direction information as a set of
 * three possible values.  These are
 * <b>forward</b>, <b>equal</b>, and <b>backward</b> dependences.  If
 * the direction is unknown, we represent the dependence information
 * as a set which contains all three values.  
 * <p>
 * The distance value is valid for loop-carried dependences.  It
 * contains the number of iterations that are carried by the dependence
 * (in constant, the direction value only indicates which direction
 * the dependence occurs in).  That is, a loop-carried
 * dependence between two references does not mean that the dependence
 * occurs at the next iteration.
 */
public final class DDInfo
{
  // Contants which represent dependence direction values.  We
  // represent direction values using a bitset so these values indicate
  // the actual bits.

  /**
   * A forward data dependence.  The dependence crosses an iteration boundary
   * in the forward direction (<i>e.g.,</i> from iteration i to i+1).  Also
   * represented as "<".
   */
  public static final int ddForward  = 1;
  /**
   * An equal dependence.  The dependence does not cross an iteration.  Also
   * reprsented as "=".
   */
  public static final int ddEqual = 2;
  /**
   * A backwards data dependence. The dependence crosses an iteration
   * boundary in the backwards direction.  Also, represented as ">".
   */
  public static final int ddBackward = 4;
  /**
   * Is the distance known.
   */
  public static final int ddKnown = 8;

  /**
   * Indicates an arbitrary relationship between the components of two
   * iteration vectors.  That is, it represents the set of all the
   * direction vectors.
   */
  public static final byte ddAll = ddForward + ddEqual + ddBackward;

  private static final String[] dirStr  = {"u ", "+ ", "0 ", "0+", "- ", "+-", "0-", "* "}; 
  private static final byte[]   reverse = {0, 4, 2, 6, 1, 5, 3, 7};

  /**
   * Data dependence information indicating 0 distance.
   */
  public static final long cDist0 = create(0, true, ddEqual);
  /**
   * Data dependence information indicating unknown distance.
   */
  public static final long cDistUnknown = create(0, false, ddAll);

  /**
   * Create an object which represents data dependence information.
   * @param distance is the data dependence distance
   * @param distanceKnown is true if the distance is known
   * @param direction is the data dependence direction
   */
  public static long create(int distance, boolean distanceKnown, int direction)
  {
    return (((long) distance << 4) |
            ((distanceKnown ? ddKnown : 0x0) + (direction & ddAll)));
  }

  /**
   * Create a copy of the data dependence information.  We make a copy
   * of the direction vector and distance information.
   * @return a copy of the data dependence information.
   */
  public static long copy(long ddinfo)
  {
    return ddinfo;
  }

  /**
   * Create a copy of the data dependence information.  We make a copy
   * of the direction vector and distance information with the
   * direction reversed.
   * @return a copy of the data dependence information.
   */
  public static long inverseCopy(long ddinfo)
  {
    return create((int) (ddinfo >> 4),
                  (ddinfo & ddKnown) != 0,
                  reverse[(int) (ddinfo & ddAll)]);
  }

  /**
   * Create a copy of the data dependence information.  We make a copy
   * of the direction vector and distance information with the
   * direction reversed.
   * @return a copy of the data dependence information.
   */
  public static long copyNot(long ddinfo, int nDirection)
  {
    return DDInfo.create((int) (ddinfo >> 4),
                         (ddinfo & ddKnown) != 0,
                         (int) ddinfo & ~nDirection);
  }

  /**
   * Return true if the direction value is set to true.
   */
  public static boolean isDirectionSet(long ddinfo, int direction) 
  {
    assert (direction <= ddBackward) && (direction >= 0) :
      "Unknown direction value " + direction;
    return (((ddinfo & ddAll) & direction) != 0);
  }

  /**
   * Return true if the direction value is <code>ddEqual</code>.
   */
  public static boolean isDirectionEqual(long ddinfo) 
  {
    return ((ddinfo & ddAll) == ddEqual);
  }

  /**
   * Return true if no direction values are set to true.
   */
  public static boolean noDirectionSet(long ddinfo) 
  {
    return ((ddinfo & ddAll) == 0);
  }

  /**
   * Have we set the distance information.
   * @return true if the distance value is known.
   */
  public static boolean isDistanceKnown(long ddinfo) 
  {
    return 0 != (ddinfo & ddKnown);
  }

  /**
   * Return the distance value. 
   */
  public static int getDistance(long ddinfo)
  {
    if (0 != (ddinfo & ddKnown))
      return (int) (ddinfo >> 4);
    return -999;
  }
  
  /**
   * Return the direction. 
   */
  public static int getDirection(long ddinfo)
  {
    return ((int) ddinfo & ddAll);
  }
  
  /**
   * Return true if the distance is known at any level.
   */
  public static boolean isAnyDistanceKnown(long[] ddinfo)
  {
    for (int i = 0; i < ddinfo.length; i++)
     if (DDInfo.isDistanceKnown(ddinfo[i]))
       return true;
    return false;
  }

  /**
   * Return true if the distance is not known at any level.
   */
  public static boolean isAnyDistanceNotKnown(long[] ddinfo)
  {
    for (int i = 0; i < ddinfo.length; i++)
     if (!DDInfo.isDistanceKnown(ddinfo[i]))
       return true;
    return false;
  }

  /**
   * Return true if any distance is unknown or not zero at any level.
   */
  public static boolean isAnyDistanceNonZero(long[] ddinfo)
  {
    for (int i = 0; i < ddinfo.length; i++) {
     if (!DDInfo.isDistanceKnown(ddinfo[i]))
       return true;
     if (DDInfo.getDistance(ddinfo[i]) != 0)
       return true;
    }
    return false;
  }

  /**
   * Create a formatted string of the dependence information.
   * @return a string representation of the dependence.
   */
  public static String toString(long ddinfo)
  {
    if (0 != (ddinfo & ddKnown))
      return Long.toString(ddinfo >> 4);

    return dirStr[(int) ddinfo & ddAll];
  }

  /**
   * Print the data dependence distance and direction.
   */
  public static void printDDInfo(long[] ddinfo)
  {
    System.out.println("DDInfo");
    for (int i = 0; i < ddinfo.length; i++)
      System.out.println("   " + i + " " + toString(ddinfo[i]));
  }
}
