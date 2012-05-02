package scale.score.dependence.omega;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.LoopHeaderChord;
import scale.score.dependence.*;
import scale.score.dependence.omega.omegaLib.*;

/**
 * A class which implements the Omega test.
 * <p>
 * $Id: OmegaTest.java,v 1.51 2007-01-04 17:08:09 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<p>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><p>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<p>
 * Amherst MA. 01003, USA<p>
 * All Rights Reserved.<p>
 * <p>
 * A class which implements the Omega test.  The Omega test is a
 * practical algorithm for exact array dependence analysis developed
 * by William Pugh at Maryland.  The Omega test is an integer
 * programming technique that combines new methods for eliminating
 * equality constraints with an extension of Fourier-Motzkin variable
 * elimination.
 * <p>
 * The code in this class is directly based upon the code in
 * <i>Petit</i>, a tool that comes with the Omega code that illustates
 * dependence analysis (especially files, <tt>ddomega.[ch]</tt> and
 * <tt>Zima.[ch]</tt>).  The Zima file implements a dependence
 * interface which facilitates creating relations for program
 * analysis.  The Zima interface is based on the notation suggested by
 * Hans Zima in <i>Supercompilers for Parallel and Vector
 * Computers</i>.
 * <p>
 * Our code significantly differs from the <i>Petit</i> in several
 * ways.  <i>Petit</i> computes all the dependences and stores them
 * into the graph during a single pass over the array references.  Our
 * code differs since we compute forward and backward dependences in
 * separate calls.  Also, we compute loop-carried and loop-independent
 * dependences during separate calls (although this probably doesn't
 * waist much time).  If time is a critical factor then, we should
 * rewrite the routines to compute the forward and backward
 * dependences at the same time.
 * <p>
 * The Omega test uses the Omega library (<tt>scale.omega</tt>) to
 * create dependence equations and compute direction vectors and
 * distance values.
 * <p>
 * <b>Improvement:</b><p>
 * I think we can improve the efficiency of this code by generating
 * all the dependences during the call to <tt>ddTest</tt> (during a
 * single pass over the dependence relations) and then the
 * <tt>getForwardDependence</tt> and <tt>getBackwardDependence</tt>
 * would just return the appropriate information (this current
 * requires two passes over the dependence relations).
 *
 * @see scale.score.dependence.omega.omegaLib.Relation
 * @see scale.score.dependence.banerjee.BanerjeeTest
 */
public class OmegaTest extends DataDependence 
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * The current relation that we're creating.  We create the relation
   * in <tt>ddTest</tt> and then clone it to compute the dependences.
   */
  private Relation        depRelation;
  private AccessIteration a;
  private AccessIteration b;
  private OmegaLib        omegaLib;
  private boolean         trace;

  /**
   * Create a data dependence object using the Omega test.  The Omega
   * test is an exact dependence test created by William Pugh.
   */
  public OmegaTest(Scribble scribble)
  {
    super(scribble);

    assert setTrace();

    this.a        = new AccessIteration();
    this.b        = new AccessIteration();
    this.omegaLib = new OmegaLib(trace);
  }

  private boolean setTrace()
  {
    trace = Debug.trace(scribble.getRoutineDecl().getName(), classTrace, 3);
    return true;
  }

  /**
   * Set up the relation for the Omega test.  This method does not
   * actually compute dependences, it just sets up the Omega relation
   * between two array accesses using the Omega library.  The relation
   * is a mathematical representation of the dependence between the
   * references.
   * <p>
   * We create an {@link
   * scale.score.dependence.omega.omegaLib.Relation Omega Relation}
   * object and we add constraints to the relation to set up the
   * dependence test.  We create <tt>AccessIteration</tt> objects to
   * represent the array accesses.  Then, we add constraints which
   * indicate the conditions which cause a data dependence between the
   * two accesses.
   * <p>
   * The inode must precede the onode in execution order.
   * @param onode is a node access
   * @param inode is a node access
   * @param oloop is the loop coressponding to the <code>onode</code> access
   * @param iloop is the loop coressponding to the <code>inode</code> access
   * @param precedes specifies the execution order.
   *  It is 1 if inode is execute before onode,
   * -1 if onode is executed befoe inode,
   * and 0 if they are "executed in parallel".
   * @see scale.score.dependence.DDInfo
   * @see OmegaTest#getForwardDependence
   * @see OmegaTest#getBackwardDependence
   * @see scale.score.dependence.omega.omegaLib.Relation
   * @return the data dependence test result
   */
  public int ddTest(SubscriptExpr   onode,
                    SubscriptExpr   inode,
                    LoopHeaderChord oloop,
                    LoopHeaderChord iloop,
                    int             precedes)
  {
    super.initialize(onode, inode, oloop, iloop, precedes);

    if (!oloop.isLoopInfoComplete() || !iloop.isLoopInfoComplete())
      return cFailed;

    try {
      // Create a data dependence relation for the two array accesses
      // using the omega library.

      omegaLib.initialize();

      depRelation = new Relation(omegaLib, oloop.getNestedLevel(), iloop.getNestedLevel());

      if (trace) {
        int insl = inode.getChord().getSourceLineNumber();
        int onsl = onode.getChord().getSourceLineNumber();
        System.out.println("** omegatest inode " + insl + " " + inode);
        System.out.println("             onode " + onsl + " " + onode);
        depRelation.prefixPrint();
      }

      a.initialize(onode, oloop, omegaLib, depRelation, VarDecl.INPUT_TUPLE);
      b.initialize(inode, iloop, omegaLib, depRelation, VarDecl.OUTPUT_TUPLE);

      if (trace) {
        System.out.println("  ddTest [AFTER ai:]");
        depRelation.prefixPrint();
      }

      FAnd f = depRelation.addAnd();

      if (trace) {
        System.out.println("  ddTest [AFTER addand:]");
        depRelation.prefixPrint();
      }

      a.inBounds(f);
      b.inBounds(f);

      if (trace) {
        System.out.println("  ddTest [AFTER inBounds:]");
        depRelation.prefixPrint();
      }

      a.sameMemory(f, b);

      if (trace) {
        System.out.println("  ddTest [AFTER sameMemory:]");
        depRelation.prefixPrint();
      }
 
      // No dependence if the relation isn't satisfiable.  The Omega
      // Library considers an upper bounds to be satisiable if it is
      // not definitely known to be un-satisfiable.  This is not
      // acceptable for us.

      if (depRelation.isNotSatisfiable()) {
        ddinfo = null;
        if (trace) {
          System.out.println("  ddTest INDEPENDENT - relation not satisfiable");
          depRelation.printWithSubs();
        }

        return cIndependent;
      }

      if (trace)
        System.out.println("  ddTest DEPENDENT - relation satisfiable");

      return cLCDependent;

    } catch(java.lang.Throwable ex) {
      if (trace) {
        assert Debug.printMessage("** ddTest exception " + ex.getMessage());
        assert Debug.printStackTrace(ex);
      }
      return cFailed;
    }
  }

  /**
   * Print dependence relation to stdout.  The relation is a
   * mathematical representation of the dependence between the
   * references.
   * @see scale.score.dependence.omega.omegaLib.Relation
   */
  public void printDepRelation()
  {
    depRelation.printWithSubs();
  }

  /**
   * Create a data dependence relation for the two array accesses
   * using the omega library.
   * <p>
   * The inode must precede the onode in execution order.
   * @param onode is a node access
   * @param inode is a node access
   * @param oloop is the loop coressponding to the <code>onode</code> access
   * @param iloop is the loop coressponding to the <code>inode</code> access
   * @param precedes specifies the execution order.
   *  It is 1 if inode is execute before onode,
   * -1 if onode is executed befoe inode,
   * and 0 if they are "executed in parallel".
   */
  public void spatialTest(SubscriptExpr   onode,
                          SubscriptExpr   inode,
                          LoopHeaderChord oloop,
                          LoopHeaderChord iloop,
                          int             precedes) throws java.lang.Exception
  {
    super.initialize(onode, inode, oloop, iloop, precedes);

    OmegaLib omegaLib = new OmegaLib(trace);

    depRelation = new Relation(omegaLib, oloop.getNestedLevel(), iloop.getNestedLevel());

    a.initialize(onode, oloop, omegaLib, depRelation, VarDecl.INPUT_TUPLE);
    b.initialize(inode, iloop, omegaLib, depRelation, VarDecl.OUTPUT_TUPLE);

    FAnd f = depRelation.addAnd();

    a.inBounds(f);
    b.inBounds(f);

    a.adjMemory(f, b, 4); //?? 4 is block size  

    // No dependence if the relation isn't satisfiable.

    if (!depRelation.isUpperBoundSatisfiable()) {
      ddinfo = null;
      if (trace)
        System.out.println("Independent - relation not satisfiable");
    }
  }

  /**
   * Return forward dependence information.  That is, the information
   * from <code>onode</code> to <code>inode</code>. Recall that the
   * dependence tester computes dependences in both directions.  This
   * routine just checks if there is a forward dependence.
   * <p>
   * Our code is much different than
   * the code it is based upon (<i>Petit</i>).  <i>Petit</i> determines
   * all dependences and stores them in the graph during the same pass.
   * We make separate passes to compute forward and backward dependences.
   * <p>
   * We need to make a clone of the dependence relation information and
   * operate on the cloned version.  We do this because the 
   * <tt>depRelation</tt> relation is altered by this method, but we
   * need the unaltered relation for <tt>getBackwardDependence</tt>.
   *
   * @return the data dependence information for foward dependences.
   */
  public Vector<long[]> getForwardDependence()
  {
    return getDependenceInfo(true);
  }

  /**
   * Return backwards dependence information.  That is, the
   * information from <code>inode</code> to <code>onode</code>.
   * Recall that the dependence tester computes dependences in both
   * directions.  This routine just checks if there is a backward
   * dependence.
   * <p>
   * Our code is much different than
   * the code it is based upon (<i>Petit</i>).  <i>Petit</i> determines
   * all dependences and stores them in the graph during the same pass.
   * We make separate passes to compute forward and backward dependences.
   * <p>
   * We need to make a clone of the dependence relation information and
   * operate on the cloned version.  We do this because the 
   * <tt>depRelation</tt> relation is altered by this method, but we
   * would like to maintain the unaltered relation (for example, if 
   * the method <tt>getForwardDependence</tt> is called after this method.
   *
   * @return the data dependence info for backward dependences.
   */
  public Vector<long[]> getBackwardDependence()
  {
    return getDependenceInfo(false);
  }

  /**
   * Compute data dependences in one direction.  The code for
   * determining forward and backward dependences is pretty much the
   * same except for a few details.  The algorithm works by computing
   * loop-carried and loop-independent dependences separately.  We
   * then merge the dependences into a single structure.  Note - we only
   * compute self dependences if <i>forward</i> is true.  Also, 
   * we work on a copy of the dependence relation since we change the
   * relation when we test for dependences.  However, we need to retain
   * the original relation because we will call this routine twice.
   * <p>
   * The code in <i>Petit</i> works the same way - we just separate
   * the computation of loop-carried and loop-independence into different 
   * methods.
   * 
   * @param forward is true if we want forward dependence information.
   * @return the dependence information (in one direction).
   */
  public Vector<long[]> getDependenceInfo(boolean forward)
  {
    // Check if we've already determined that there is no dependence.
 
    if ((ddinfo == null) || !depRelation.isUpperBoundSatisfiable())
      return null;

    // Self dependences are only computed in the forward direction.

    if (!forward && (onode == inode))
      return null;

    Relation old = depRelation;
    depRelation = new Relation(omegaLib, depRelation);

    Vector<long[]> ddinfo1 = getLoopCarriedDeps(forward);
    long[] ddinfo2 = getLoopIndependentDeps(forward);

    if ((ddinfo1 == null) && (ddinfo2 == null)) {
      depRelation = old;
      return null;
    }

    Vector<long[]> depVect = ddinfo1;

    if (depVect == null)
      depVect = new Vector<long[]>();
    
    depVect.addElement(ddinfo2);

    depRelation = old; // Reset the relation object.

    return depVect;
  }

  /**
   * Compute the loop-carried dependences between two references.
   * This routine only computes the dependences in one direction
   * (<i>e.g.,</i> <code>onode -> inode</code>).
   * <p>
   * Since the Omega test is an exact test, the dependence information
   * array is initially empty.  That is, no direction values are set.
   * For an inexact dependence test the array must be completely full
   * (that is each direction is "*").
   * <p>
   * We expect that the loop-carried dependences are merged with the
   * loop-independent dependences to obtain all dependences.
   * <p>
   * This code is very similar to the procedure
   * <code>findDirectionVector</code> in <i>petit</i>.
   * 
   * @param forward get information from onode -> inode or inode -> onode.
   * @return the dependence information for the appropriate direction.
   * @see OmegaTest#getLooIndependentDeps
   */
  private Vector<long[]> getLoopCarriedDeps(boolean forward)
  {
    Vector<long[]>  ddinfoVect = new Vector<long[]>();
    boolean selfDep    = (onode == inode);

    if (trace) {
      System.out.print("getLoopCarriedDeps ");
      System.out.print(bnest);
      System.out.print(" ");
      System.out.print(oloop.getSourceLineNumber());
      System.out.print(" ");
      System.out.println(iloop.getSourceLineNumber());
      System.out.print("getLoopCarriedDeps ");
      System.out.print(bnest);
      System.out.print(" ");
      System.out.print(oloop.getNestedLevel());
      System.out.print(" ");
      System.out.println(iloop.getNestedLevel());

      depRelation.prefixPrint();
    }

    if (bnest > 0)
      depRelation = depRelation.makeLevelCarriedTo(bnest);

    for (int level = 1; level <= bnest; level++) {
      int      loopDir = (loopInfo[level].getStepValue() >= 0) ? 1 : -1;
      Relation rel     = null;

      if (trace) {
        System.out.print("Considering dependencies carried at depth ");
        System.out.print(level);
        System.out.print(" ");
        System.out.print(loopDir);
        System.out.print(" ");
        System.out.println(forward);

        depRelation.prefixPrint();
      }

      if (forward) {
        rel = depRelation.extractDNFByCarriedLevel(level, loopDir);
      } else { // backward
        if (!selfDep) {
          rel = depRelation.extractDNFByCarriedLevel(level, -loopDir);
          rel = rel.inverse();
        }
      }

      if ((depRelation.numberOfConjuncts() == 0) && (rel.numberOfConjuncts() == 0))
        break;
 
      // Create an array of empty dependence information to fill in.

      int[]     direction = new int[ddinfo.length];
      int[]     distance  = new int[ddinfo.length];
      boolean[] distKnown = new boolean[ddinfo.length];
      for (int i = 0; i < ddinfo.length; i++) {
        distance[i]  = DDInfo.getDistance(ddinfo[i]);
        distKnown[i] = DDInfo.isDistanceKnown(ddinfo[i]);
        direction[i] = DDInfo.getDirection(ddinfo[i]);
      }

      boolean noneSet = true;
      if (forward || (!forward && !selfDep)) {
        if (rel.numberOfConjuncts() > 0) {

          if (trace) {
            System.out.println ("Dependencies carried by level " + level);
            rel.prefixPrint();
          }

          int[] bounds = new int[2];
          for (int l = level; l <= bnest; l++) {
            boolean guaranteed = rel.queryDifference(rel.outputVar(l), rel.inputVar(l), bounds);
            int     lb         = bounds[0];
            int     ub         = bounds[1];

            if (trace) {
              System.out.print("lb = ");
              System.out.print(lb);
              System.out.print(", ub = ");
              System.out.print(ub);
              System.out.print(", outer level = ");
              System.out.print(level);
              System.out.print(", inner level = ");
              System.out.println(l);
            }

            if (l == level) {
              direction[l] = DDInfo.ddForward;
              noneSet = false;
            } else {
              boolean iloopDir = loopInfo[l].getStepValue() >= 0;

              if (lb < 0) {
                int dir = (iloopDir ? DDInfo.ddBackward : DDInfo.ddForward);
                direction[l] = dir;
                noneSet = false;
              }

              if (ub > 0) {
                int dir = (iloopDir ? DDInfo.ddForward : DDInfo.ddBackward);
                direction[l] = dir;
                noneSet = false;
              }

              if ((lb <= 0) && (ub >= 0)) {
                direction[l] = DDInfo.ddEqual;
                noneSet = false;
              }
            }
 
            if (lb == ub) {
              distance[l] = lb;
              distKnown[l] = true;
              noneSet = false;
            } 
          }
        }
      }

      if (!noneSet) {
        long[] ddinfoNew = new long[ddinfo.length];
        for (int i = 0; i < ddinfo.length; i++)
          ddinfoNew[i] = DDInfo.create(distance[i], distKnown[i], direction[i]);
        ddinfoVect.addElement(ddinfoNew);
      }
    }

    return ((ddinfoVect.size() > 0) ? ddinfoVect : null);
  }

  /**
   * Compute the loop-independent dependences between two references.
   * This routine only computes the dependences in one direction
   * (<i>e.g.,</i> onode -> inode).  To get all the dependences we
   * need to merge the result from this method with 
   * <code>getLoopCarriedDeps</code>.
   * <p>
   * We assume that the routine <code>getLoopCarriedDeps</code> is
   * called before this routine.
   * 
   * @param forward get information from onode -> inode or inode->onode.
   * @return the dependence information for the appropriate direction,
   * return null if there are no loop-independent dependences.
   * @see OmegaTest#getLoopCarriedDeps
   */
  private long[] getLoopIndependentDeps(boolean forward)
  {
    int      loopDir = 0;
    boolean  selfDep = (onode == inode);
    Relation rel     = depRelation;

    // Extract the "other" direction from the relation to check for
    // loop independents.

    for (int level = 1; level <= bnest; level++) {
      loopDir = (loopInfo[level].getStepValue() >= 0) ? 1 : -1;
      if (forward) {
        if (!selfDep)
          rel.extractDNFByCarriedLevel(level, -loopDir);
      } else
        rel.extractDNFByCarriedLevel(level, loopDir);
    }

    if (trace) {
      System.out.println("Considering independent dependence");
      depRelation.prefixPrint();
    }

    // Consider the spceial case when the two references are on the
    // same line.

    boolean precede = forward ^ !(precedes > 0);

    // Input dependences exist regardless of lexical order.

    if ((rel.numberOfConjuncts() <= 0) || !precede)
      return null;

    if (trace)
      System.out.println("Loop independent dependence found!");

    // Create loop-independent dependence information.

    long[]  ddinfoNew = new long[ddinfo.length];
    boolean eq        = inode.equivalent(onode);

    for (int i = 0; i < ddinfo.length; i++)
      ddinfoNew[i] = DDInfo.create(0, eq, DDInfo.ddEqual);
      
    return ddinfoNew;
  }
}
