package scale.score.dependence;

import scale.common.*;
import scale.annot.*;
import scale.score.*;
import scale.score.dependence.*;
import scale.score.expr.*;
import scale.score.chords.LoopHeaderChord;

/**
 * The base class for computing array data dependences.
 * <p>
 * $Id: DataDependence.java,v 1.42 2007-10-04 19:58:25 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * After a data dependence object is created, dependence testing is
 * performed by calling <tt>ddTest</tt>.   After testing is complete, 
 * the dependence object contains information in both directions.  
 * That is, from the first node to the second and the second node
 * to the first.  To add dependence edges or print dependence information,
 * the <tt>getForwardDependence</tt> and <tt>getBackwardDependence</tt>
 * methods must be called to filter the dependence testing results.
 */
public abstract class DataDependence
{
  /**
   * Indicates that the data dependence test found that the two array
   * accesses never access the same array element.  For example,
   * <code>a[2]</code> and <code>a[3]</code> never access the same
   * array element.
   */
  public static final int cIndependent = 0;
  /**
   * Indicates that the data dependence test found that the two array
   * accesses may access the same array element.  For example,
   * <code>a[2]</code> and <code>a[i]</code> will access the same
   * element when <code>i &equiv; 2</code>.
   */
  public static final int cUnkDependent = 1;
  /**
   * Indicates that the data dependence test found that the two array
   * accesses may access the same array element for different values
   * of the loop induction variable.  The distance and direction may
   * be known.  For example, <code>a[i]</code> and <code>a[i-1]</code>
   * are dependent with a distance of 1 and a direction of &lt; while
   * <code>a[i]</code> and <code>a[i]</code> are dependent with a
   * distance of 0 and a direction of =.
   */
  public static final int cLCDependent = 2;
  /**
   * Indicates that the data dependence test found that the two array
   * accesses access the same array element for every value of the
   * loop induction variable.  The distance and direction may be
   * known.  For example, <code>a[i]</code> and <code>a[i]</code> are
   * dependent with a distance of 0 and a direction of =.
   */
  public static final int cEqDependent = 3;
  /**
   * Indicates that the data dependence test failed.
   */
  public static final int cFailed = 4;

  /**
   * Map the data dependence result from an integer to a string representation.
   */
  public static final String[] result = {
    "independent", "unknown-dependent", "lc-dependent",
    "eq-dependence", "failed"};
  /**
   * An array reference.
   */
  protected SubscriptExpr onode;
  /**
   * An array reference.
   */
  protected SubscriptExpr inode;
  /**
   *  It is 1 if <code>inode</code> is execute before <code>onode, -1
   * if <code>onode</code> is executed befoe inode, and 0 if they are
   * "executed in parallel".
   */
  protected int precedes;
  /**
   * Loop containing <code>onode</code>.
   */
  protected LoopHeaderChord oloop;
  /**
   * Loop containing <code>inode</code>.
   */
  protected LoopHeaderChord iloop;
  /**
   * Loop containing both.
   */
  protected LoopHeaderChord cloop;
  /**
   * The common nesting level of the two references.
   */
  protected int bnest;
  /**
   * The data dependence information of the two references.  The array
   * is indexed by loop nest level (and loops start at level 1).  
   * The 0th level represents dependences that start outside of loops.
   * Currently, we don't use the 0th entry.
   */
  protected long[] ddinfo;
  /**
   * The loop information for the current references.  We access the
   * array by nest level (so the 0th entry is unused because the
   * outermost nest level is 1).
   */
  protected LoopHeaderChord[] loopInfo;

  /**
   * The CFG from which the data dependence isfound.
   */
  protected Scribble scribble;
 
  /**
   * Create an object for dependence testing.
   */
  public DataDependence(Scribble scribble)
  {
    this.scribble = scribble;
  }

  /**
   * @param onode a node access 
   * @param inode a node access
   * @param oloop is the loop containing <code>onode</code>
   * @param iloop is the loop containing <code>inode</code>
   * @param precedes specifies the execution order.
   *  It is 1 if <code>inode</code> is execute before <code>onode</code>,
   * -1 if <code>onode</code> is executed befoe <code>inode</code>,
   * and 0 if they are "executed in parallel".
   */
  public void initialize(SubscriptExpr   onode,
                         SubscriptExpr   inode,
                         LoopHeaderChord oloop,
                         LoopHeaderChord iloop,
                         int             precedes)
  {
    this.onode    = onode;
    this.inode    = inode;
    this.precedes = precedes;
    this.oloop    = oloop;
    this.iloop    = iloop;
    this.cloop    = oloop.commonAncestor(iloop);
    this.bnest    = cloop.getNestedLevel();
    this.ddinfo   = new long[bnest + 1];
    this.loopInfo = new LoopHeaderChord[bnest + 1];

   // Obtain the loops that surround these two array references.

    LoopHeaderChord loop      = this.cloop;
    int             nestLevel = bnest;
    while (loop != null) {
      loopInfo[nestLevel--] = loop;
      loop = loop.getParent(); // Get the next outermost loop.
    }
  }
  
  /**
   * Return the data dependence information for these two references.
   */
  public long[] getDDInfo()
  {
    return ddinfo;
  }

  /**
   * Compute data dependences in one direction.
   * @param forward is true if we want forward dependence information.
   * @return the dependence information (in one direction).
   */
  public abstract Vector<long[]> getDependenceInfo(boolean forward);

  /**
   * Determine if there is a dependence between two references.  This
   * method determines depedences between <code>onode</code> and
   * <code>inode</code>.  The methods {@link #getForwardDependences
   * getForwardDependences} and {@link #getBackwardDependences
   * getBackwardDependences} filter the dependence information to
   * account for lexical ordering and direction.
   * <p>
   * If {@link #cLCDependent cLCDependent} is returned, then {@link
   * #getDDInfo getDDInfo} will return valid dependence information.
   * @param onode a node access 
   * @param inode a node access
   * @param oloop is the loop containing <code>onode</code>
   * @param iloop is the loop containing <code>inode</code>
   * @param precedes specifies the execution order.
   *  It is 1 if <code>inode</code> is execute before <code>onode</code>,
   * -1 if <code>onode</code> is executed befoe <code>inode</code>,
   * and 0 if they are "executed in parallel".
   * @return the data dependence test result.
   * @see #cIndependent
   * @see #cUnkDependent
   * @see #cLCDependent
   * @see #cEqDependent
   * @see #cFailed
   */
  public abstract int ddTest(SubscriptExpr   onode,
                             SubscriptExpr   inode,
                             LoopHeaderChord oloop,
                             LoopHeaderChord iloop,
                             int             precedes);

  /**
   * @param onode a node access 
   * @param inode a node access
   * @param oloop is the loop containing <code>onode</code>
   * @param iloop is the loop containing <code>inode</code>
   * @param precedes specifies the execution order.
   *  It is 1 if <code>inode</code> is execute before <code>onode</code>,
   * -1 if <code>onode</code> is executed befoe <code>inode</code>,
   * and 0 if they are "executed in parallel".
   */
  public abstract void spatialTest(SubscriptExpr   onode,
                         SubscriptExpr   inode,
                         LoopHeaderChord oloop,
                         LoopHeaderChord iloop,
                         int             precedes) throws java.lang.Exception;

  /**
   * Return forward dependence information.  That is, the information
   * from <code>onode</code> to <code>inode</code> Recall that the
   * dependence tester computes dependences in both directions.  This
   * routine just checks if there is a forward dependence.  There is a
   * forward dependence if there is a loop-independent dependence and
   * <code>onode</code> occurs before <code>inode</code> in lexical
   * execution order or if it is a loop-carried dependence.  We also
   * strip any nonsensical '&lt;' directions.  That is a '&gt;'
   * direction that occurs at the same nesting level as the first
   * '&lt;' direction.
   *
   * @return the data dependence information for foward dependences.
   */
  public long[] getForwardDependences()
  {
    if (ddinfo == null)
      return null;

    // Check for loop-carried or loop-independent dependence.  Also,
    // check if the first direction is just a '>' direction.

    boolean carried = false;
    for (int n = 1;  !carried && (n <= bnest); n++) {
      if (DDInfo.isDirectionSet(ddinfo[n], DDInfo.ddForward)) {
        carried = true;
      } else if (DDInfo.isDirectionSet(ddinfo[n], DDInfo.ddEqual)) {
        // don't do anything
      } else {
        // first direction is a '>', there is no dependence
        return null;
      }
    }

    if (!carried && (precedes >= 0))
      return null;

    // Create a new array of data depedence information that just
    // represents forward dependences.

    long[] forwardDDInfo = new long[ddinfo.length];
    forwardDDInfo[0] = ddinfo[0];

    // Fixup the direction vector.  We remove any '>' directions that
    // appear before (or at the same level as) the first '<' direction.

    boolean disallowed = true;
    int     nless      = 0;
    int     lessLevel  = 0;
    if (precedes >= 0) {
      for (int i = 1; i <= bnest; i++) {
        if (DDInfo.isDirectionSet(ddinfo[i], DDInfo.ddForward)) {
          nless++;
          lessLevel = i;
        }
      }
    }

    if (nless != 1)
      lessLevel = 0;

    for (int i = 1; i <= bnest; i++) {
      if (disallowed) {
        if (i == lessLevel) // check for a '=' that is upwards or to self
          forwardDDInfo[i] = DDInfo.copyNot(ddinfo[i], DDInfo.ddEqual + DDInfo.ddBackward);
        else
          forwardDDInfo[i] = DDInfo.copyNot(ddinfo[i], DDInfo.ddBackward);
      } else if (i == lessLevel) // check for a '=' that is upwards or to self
        forwardDDInfo[i] = DDInfo.copyNot(ddinfo[i], DDInfo.ddEqual);
      else
        forwardDDInfo[i] = ddinfo[i];

      if (DDInfo.isDirectionSet(forwardDDInfo[i], DDInfo.ddForward))
        disallowed = false;
    }

    for (int i = bnest + 1; i < ddinfo.length; i++)
      forwardDDInfo[i] = ddinfo[i];

    return forwardDDInfo;
  }

  /**
   * Return backwards dependence information.  That is, the
   * information from <code>inode</code> to <code>onode</code>.
   * Recall that the dependence tester computes dependences in both
   * directions.  This routine just checks if there is a backward
   * dependence.  There is a backward dependence if there is a
   * loop-independent dependence and <code>inode</code> occurs before
   * <code>onode</code> in lexical execution order or if it is a loop
   * carried dependence.  We also strip any nonsensical '&lt;'
   * directions.
   *
   * @return the data dependence info for backward dependences.
   */
  public long[] getBackwardDependences()
  {
    if (ddinfo == null)
      return null;

    // Check for loop-carried or loop-independent dependence.  Also,
    // check if the first direction is just a '>' direction.

    boolean carried = false;
    for (int n = 1;  !carried && (n <= bnest); n++) {
      if (DDInfo.isDirectionSet(ddinfo[n], DDInfo.ddBackward)) {
        carried = true;
      } else if (DDInfo.isDirectionSet(ddinfo[n], DDInfo.ddEqual)) {
        // don't do anything
      } else {
        // first direction is a '>', there is no dependence
        return null;
      }
    }
    
    if ((inode == onode) || (!carried && (precedes < 0)))
      return null;

    // Create a new array of data dependence info. that just represents
    // backward dependences.

    long[] backwardDDInfo = new long[ddinfo.length];
    backwardDDInfo[0] = DDInfo.inverseCopy(ddinfo[0]);

    // Fixup the direction vector.  We remove any '>' directions that
    // appear before the (or at the same level) first '>' direction.

    boolean disallowed = true;
    int     nless      = 0;
    int     lessLevel  = 0;
    if (precedes <= 0) {
      for (int i = 1; i <= bnest; i++) {
        if (DDInfo.isDirectionSet(ddinfo[i], DDInfo.ddForward)) {
          nless++;
          lessLevel = i;
        }
      }
    }

    if (nless != 1)
      lessLevel = 0;

    for (int i = 1; i <= bnest; i++) {
      if (disallowed) {
        if (i == lessLevel) // check for a '=' that is upwards or to self
          backwardDDInfo[i] = DDInfo.copyNot(ddinfo[i], DDInfo.ddEqual + DDInfo.ddBackward);
        else
          backwardDDInfo[i] = DDInfo.copyNot(ddinfo[i], DDInfo.ddBackward);
      } else if (i == lessLevel) // check for a '=' that is upwards or to self
        backwardDDInfo[i] = DDInfo.copyNot(ddinfo[i], DDInfo.ddEqual);
      else
        backwardDDInfo[i] = ddinfo[i];

      if (DDInfo.isDirectionSet(backwardDDInfo[i], DDInfo.ddForward))
        disallowed = false;
    } 

    for (int i = bnest + 1; i < ddinfo.length; i++)
      backwardDDInfo[i] = ddinfo[i];

    return backwardDDInfo;
  }
}
