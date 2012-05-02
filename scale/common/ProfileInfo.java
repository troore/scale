package scale.common;

/**
 * An instance of this class is used to hold the profilie information
 * generated from a previous execution of an instrumented CFG..
 * <p>
 * $Id: ProfileInfo.java,v 1.2 2007-10-29 13:36:05 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class ProfileInfo
{
  /**
   * The characteristic value for the CFG.  This is used to determine
   * if the CFG is the same as the ione that was instrumented.
   */
  public int hash;
  /**
   * The number of loops in the CFG.
   */
  public int numLoops;
  /**
   * The block counts for each basic block in the CFG.
   */
  public int[] blockArray;
  /**
   * The edge counts for each edge in the CFG.
   */
  public int[] edgeArray;
  /**
   * A map from path numbers to path frequencies (Integer -> Integer).
   */
  public HashMap<Long, Long> pathMap;
  /**
   * A map from a loop number to a loop trip count histogram.
   */
  public IntMap<long[]> loopHistMap;
  /**
   * A map from loop numbers to unroll counts.
   */
  public int[] ucArray;
  /**
   * A map from loop numbers to instruction counts.
   */
  public int[] icArray;

  /**
   * @param hash is characteristic value for the CFG.
   */
  public ProfileInfo(int hash)
  {
    this.hash = hash;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(ProfileInfo ");
    buf.append(hash);
    buf.append(' ');
    buf.append(numLoops);
    buf.append(' ');
    buf.append(edgeArray);
    buf.append(' ');
    buf.append(pathMap);
    buf.append(' ');
    buf.append(loopHistMap);
    buf.append(' ');
    buf.append(ucArray);
    buf.append(' ');
    buf.append(icArray);
    buf.append(')');
    return buf.toString();
  }
}
