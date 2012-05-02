package scale.score.dependence;

import java.util.Enumeration;
import java.util.Iterator;

import scale.common.*;
import scale.annot.*;
import scale.score.*;
import scale.score.chords.Chord;
import scale.score.chords.LoopHeaderChord;
import scale.score.expr.*;
import scale.score.expr.SubscriptExpr;
import scale.score.dependence.omega.*;
import scale.score.dependence.banerjee.*;
import scale.visual.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;

/**
 * This class represents the data dependence graph.
 * <p>
 * $Id: DDGraph.java,v 1.75 2007-10-04 19:58:24 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The entire dependence information for one function is contained in
 * a single <code>DDGraph</code> instance.  This class contains a
 * table of dependence edges that maps from array (by name) to the
 * {@link scale.score.dependence.DDEdge edges}.  The edges have a
 * source and a sink which are {@link scale.score.expr.SubscriptExpr
 * SubscriptExpr} instances.  This table is discarded when the {@link
 * scale.score.Scribble Scribble.reduceMemory()} method is called
 * after all optimizations or when the {@link scale.score.Scribble
 * Scribble.recomputeDataDependence()} method is called because the
 * CFG was changed.  Note - the data dependence information is only
 * computed on demand.  Currently, only {@link
 * scale.score.trans.ScalarReplacement ScalarReplacement} demands it.
 * <p>
 * <img src="../../../data_dependence.jpg" alt="Data Dependence Graph Example">
 * <p>
 * This graph shows the data dependence graph for a simple program:
 * <pre>
 *     int A[10];
 *     for (I = 0; I < 10; I++) {
 *       X = &A[I];
 *       Y = &A[I];
 *       S = S + *X * *X + *Y * *Y
 *     }
 * </pre>

 * In this example there is one <code>DDEdge</code> instance which
 * connects the two {@link scale.score.expr.SubscriptExpr
 * SubscriptExpr} instances.  The {@link
 * scale.score.expr.SubscriptExpr SubscriptExpr} instances are the
 * <i>ends</i> of the edge.
 * <p>
 * Even though there is only one <code>DDEdge</code> instance in the
 * above example, there are <i>implied</i> edges between all of the
 * <i>uses</i> of the address specified by one {@link
 * scale.score.expr.SubscriptExpr SubscriptExpr} instance.  For these
 * implied edges the distance is always known and is zero
 * <emph>because they all refer to the same array element</emph>.  If
 * there are <code>n</code> uses (e.q., {@link
 * scale.score.expr.LoadDeclValueExpr LoadDeclValueExpr} instances) of
 * one {@link scale.score.expr.SubscriptExpr SubscriptExpr} instance
 * then there are <code>n &times; (n - 1) / 2</code> <i>implied</i>
 * data dependence edges.
 * <p>
 * When it is proved that there is a data dependence edge between
 * two {@link scale.score.expr.SubscriptExpr SubscriptExpr} instances
 * as shown above in the graph, then it is <i>asserted</i> that there
 * is an edge between each of the uses of the two {@link
 * scale.score.expr.SubscriptExpr SubscriptExpr} instances.  These
 * edges all have the same distance and direction.  If there are
 * <code>n</code> uses of one {@link scale.score.expr.SubscriptExpr
 * SubscriptExpr} instance and <code>m</code> uses of the other, then
 * there are <code>n &times; m</code> <i>asserted</i> data dependence
 * edges.  But, they are all represented by a single {@link
 * scale.score.dependence.DDEdge DDEdge} instance.  (The graph shows
 * only one of the four <i>asserted</i> edges.)
 * <p>
 * Whether an edge is <i>implied</i> or <i>asserted</i>, the Scale
 * system creates a {@link scale.score.dependence.DDEdge DDEdge}
 * instance to represent it.  (The graph only shows the
 * <code>DDEdge</code> instance for the <i>asserted</i> edges.)
 * <p>
 * The {@link scale.score.dependence.DDEdge DDEdge} instances do not
 * provide all the information about the edges.  Whether an edge is a
 * {@link DDEdge#cFlow flow}, {@link DDEdge#cAnti anti}, {@link
 * DDEdge#cInput input}, or {@link DDEdge#cOutput output} edge can
 * only be determined by looking at the actual uses.  The {@link
 * scale.score.dependence.DDEdge DDEdge} classes provide several
 * methods for obtaining all of the <i>implied</i> and <i>asserted</i>
 * edges and the information about them.  The {@link
 * scale.score.dependence.DDEdge#getEnds getEnds} and {@link
 * scale.score.dependence.DDEdge#iterator iterator} methods provide
 * access to the {@link scale.score.expr.SubscriptExpr SubscriptExpr}
 * instances.  The {@link scale.score.dependence.DDEdge#getEdgeType
 * getEdgeType} method provides the edge type (e.g., {@link
 * DDEdge#cFlow flow}, {@link DDEdge#cAnti anti}, {@link DDEdge#cInput
 * input}, or {@link DDEdge#cOutput output}).  From a {@link
 * scale.score.expr.SubscriptExpr SubscriptExpr} instance you can
 * obtain the uses of the array element address by using the {@link
 * scale.score.expr.SubscriptExpr#addUses addUses} method.
 * <p>
 * @see DDEdge
 * @see DDNormalEdge
 * @see DDTransEdge
 * @see DataDependence
 */
public class DDGraph
{
  /**
   * Enable use of the simple dependendence test
   */
  public static boolean useBasic = false;
  /**
   * Enable use of the Banerjee dependendence test
   */
  public static boolean useBanerjee = false;
  /**
   * Enable use of the Omega dependendence test
   */
  public static boolean useOmega = false;
  /**
   * Enable use of transitive closure
   */
  public static boolean useTransClosure = false;

  private static int totalCount          = 0; // A count of all the ddTests performed.
  private static int simpleTestCount     = 0; // A count of all the ddTests performed using the simple test.
  private static int omegaTestCount      = 0; // A count of all the ddTests performed using the Omega Library.
  private static int banerjeeTestCount   = 0; // A count of all the ddTests performed using the Banerjee test.
  private static int omegaFailedCount    = 0; // A count of all the ddTests that failed using the Omega Library.
  private static int banerjeeFailedCount = 0; // A count of all the ddTests that failed using the Banerjee test.
  private static int existsCount         = 0; // A count of all the edges that already existed.

  private static final String[] stats = {"totalTests",
                                         "omegaTests",
                                         "banerjeeTests",
                                         "omegaFails",
                                         "banerjeeFails",
                                         "edgeExisted",
                                         "simpleTest"};

  static
  {
    Statistics.register("scale.score.dependence.DDGraph", stats);
  }

  /**
   * Return the count of all the ddTests performed using the Omega
   * Library.
   */
  public static int totalTests()
  {
    return totalCount;
  }

  /**
   * Return the count of all the ddTests performed using the Omega
   * Library.
   */
  public static int omegaTests()
  {
    return omegaTestCount;
  }

  /**
   * Return the count of all the ddTests performed using the Banerjee
   * test.
   */
  public static int banerjeeTests()
  {
    return banerjeeTestCount;
  }

  /**
   * Return the count of all the ddTests performed using the simple
   * test.
   */
  public static int simpleTest()
  {
    return simpleTestCount;
  }

  /**
   * Return the count of all the ddTests that failed using the Omega
   * Library.
   */
  public static int omegaFails()
  {
    return omegaFailedCount;
  }

  /**
   * Return the count of all the ddTests that failed using the
   * Banerjee test.
   */
  public static int banerjeeFails()
  {
    return banerjeeFailedCount;
  }

  /**
   * Return the count of all the edges that already existed.
   */
  public static int edgeExisted()
  {
    return existsCount;
  }

  /**
   * True if trace information should be displayed.
   */
  public static boolean classTrace = false;

  private Table<SubscriptExpr, DDEdge>      edgeList = new Table<SubscriptExpr, DDEdge>();  // The set of dependence edges.
  private Table<Declaration, SubscriptExpr> aSubTable;

  private Scribble     scribble;
  private BanerjeeTest bj;
  private OmegaTest    ot;
  private boolean      createSpatialDependencies; // Set true if spatial edges should be created.
  private boolean      trace;

  /**
   * True if all dependencies were computed.  It is set false if any
   * dependency relation was not able to be determined.
   * @see #computeArrayDependences
   */
  public boolean allComputed;

  /**
   * @param scribble specifies the CFG containing the dependencies
   * @param createSpatialDependencies is true if spatial edges should be created
   */
  public DDGraph(Scribble scribble, boolean createSpatialDependencies)
  {
    this.scribble = scribble;
    this.createSpatialDependencies = createSpatialDependencies;
    assert setTrace();
  }

  private boolean setTrace()
  {
    trace = Debug.trace(scribble.getRoutineDecl().getName(), classTrace, 3);
    return true;
  }

  /**
   * Create a graphic display of the dependence graph.  You must call
   * <code>computeArrayDependences</code> first.
   * @param da is the graph display
   * @param addChord is true if a link between the expression and its
   * Chord should be shown
   */
  public void graphDependence(DisplayGraph da, boolean addChord)
  {
    HashSet<DDEdge> done  = WorkArea.<DDEdge>getSet("graphDependence");
    HashSet<Note>   nodes = null;
    if (addChord)
      nodes = WorkArea.<Note>getSet("graphDependence");

    Enumeration<DDEdge> ee = edgeList.elements();
    while (ee.hasMoreElements()) {
      DDEdge edge = ee.nextElement();
      if (done.add(edge))
        edge.graphDependence(da, addChord, nodes, this);
    }

    if (addChord) {
      Iterator<Note> it2 = nodes.iterator();
      while (it2.hasNext()) {
        Note  exp = it2.next();
        Chord c   = exp.getChord();

        if (c != null)
          da.addEdge(exp, c, DColor.GREEN, DEdge.DOTTED, "DD");
      }
    }

    WorkArea.<DDEdge>returnSet(done);
    if (addChord)
      WorkArea.<Note>returnSet(nodes);
  }

  /**
   *  Dump out all data dependence edges.
   */
  public void dumpEdgeList()
  {
    HashSet<DDEdge> set = WorkArea.<DDEdge>getSet("dumpEdgeList");

    System.out.println("edge List");
    Enumeration<DDEdge> ee = edgeList.elements();
    while (ee.hasMoreElements()) {
      DDEdge edge = ee.nextElement();
      if (set.add(edge))
        System.out.println("  " + edge);
    }

    WorkArea.<DDEdge>returnSet(set);
  }

  /**
   * Return an enumeration of all the edges.
   */
  public Enumeration<DDEdge> getEdges()
  {
    return edgeList.elements();
  }

  /**
   * Return an array of all edges that contain the specified expression.
   */
  public DDEdge[] getEdges(SubscriptExpr src)
  {
    Object[] el    = edgeList.getRowArray(src);
    DDEdge[] edges = new DDEdge[el.length];
    for (int i = 0; i < el.length; i++)
      edges[i] = (DDEdge) el[i];
    return edges;
  }

  /**
   * Return a vector of all edges for the specified Loop.  You must
   * call <code>computeArrayDependences</code> first.
   */
  public Vector<DDEdge> getEdges(LoopHeaderChord loop)
  {
    Vector<DDEdge>      v  = new Vector<DDEdge>(10);
    Enumeration<DDEdge> ee = edgeList.elements();
    while (ee.hasMoreElements()) {
      DDEdge edge = ee.nextElement();
      if (edge.forLoop(loop))
        v.addElement(edge);
    }
    return v;
  }

  /**
   * Perform array dependence testing for subscript expressions in the
   * specified loop nest.
   * @param loop is the loop for which dependencies are to be checked
   */
  public void computeArrayDependences(LoopHeaderChord loop)
  {
    aSubTable = null;

    if (!(useBasic || useBanerjee || useOmega))
      return;

    aSubTable = new Table<Declaration, SubscriptExpr>();
    loop.getSubscriptsRecursive(aSubTable);

    scribble.getLoopTree().setDDComplete();

    if (trace)
      System.out.println("** DDGraph " + scribble.getRoutineDecl().getName());

    Enumeration<Declaration> k = aSubTable.keys();
    while (k.hasMoreElements()) {
      Declaration arr   = k.nextElement();
      String      aname = arr.getName();
      Object[]    se    = aSubTable.getRowArray(arr);

      SubscriptExpr[] subscripts = new SubscriptExpr[se.length];
      for (int i = 0; i < se.length; i++)
        subscripts[i] = (SubscriptExpr) se[i];

      if (trace)
        System.out.println("           " + subscripts.length + " " + aname);

      checkDependences(loop, subscripts, (VariableDecl) arr);

      if (createSpatialDependencies)
        checkSpatialDependences(subscripts, aname);

      if (trace) {
        int len = subscripts.length;
        for (int i = 0; i < len; i++) {
          SubscriptExpr expr = subscripts[i];
          dumpLocality(expr, aname);
        }
      }
    }
  }

  /**
   * Return the table of subscripts used to compute the dependencies.
   */
  public Table<Declaration, SubscriptExpr> getSubscriptsUsed()
  {
    return aSubTable;
  }

  /**
   * Check for dependences between the specified subscripts.
   * This version is used when there are a small number of subscripts.
   * @param loop specifies the loop for which dependencies are checked
   * @param subscripts a list of subscripts to test.
   * @param arr is the array that is subscripted
   */
  private void checkDependences(LoopHeaderChord loop,
                                Object[]        subscripts,
                                VariableDecl    arr) 
  {
    if (useBanerjee) {// Try Banerjee.
      if (bj == null)
        bj = new BanerjeeTest(scribble);
    }

    if (useOmega) { // Try the Omega Library.
      if (ot == null)
        ot = new OmegaTest(scribble);
    }

    String       aname = arr.getName();
    Vector<Note> v     = new Vector<Note>(10);
    int          len   = subscripts.length;
    for (int i = 0; i < len; i++) {
      SubscriptExpr   s1sub = (SubscriptExpr) subscripts[i];
      Chord           c1    = s1sub.getChord();
      LoopHeaderChord c1lh  = c1.getLoopHeader();
      boolean         unk   = !s1sub.allSubscriptsOptimizationCandidates();
      int             s1ord = c1.getLabel();

      v.clear();
      s1sub.addUses(v);

      // Determine if the subscript expression contains a reference to
      // an induction variable.

      boolean ok = false;
      int     l  = s1sub.numSubscripts();
      for (int j = 0; j < l; j++) {
        Expr       sub = s1sub.getSubscript(j);
        AffineExpr ae  = loop.isAffine(sub);
        if (ae != null) {
          ok = true;
          break;
        }
      }

      if ((v.size() > 0) && ok)
        recordZeroDistDependence(s1sub, s1sub, aname, false, c1lh);

      find:
      for (int j = 0; j < len; j++) {
        SubscriptExpr s2sub = (SubscriptExpr) subscripts[j];
        if (ok && (s1sub == s2sub))
          continue;

        Chord c2    = s2sub.getChord();
        int   s2ord = c2.getLabel();
        if (s1ord > s2ord)
          continue;

        LoopHeaderChord c2lh = c2.getLoopHeader();
        LoopHeaderChord lp   = c1lh.commonAncestor(c2lh);

        if ((lp instanceof scale.score.chords.BeginChord) &&
            (c1lh != lp) &&
            (c2lh != lp))
          continue;

        totalCount++;

        try {
          if (unk || !s2sub.allSubscriptsOptimizationCandidates()) {
            // We don't know what the dependence is.
            // Record a dependence of unknown distance.
            // See K in BLSOLV from 141.apsi when inlining EKMLAY.

            int    dp     = lp.getNestedLevel();
            long[] ddinfo = new long[dp + 1];

            for (int k = 0; k <= dp; k++)
              ddinfo[k] = DDInfo.cDistUnknown;

            recordDependence(s1sub, s2sub, ddinfo, aname, false, lp);
            continue;
          }

          if (useTransClosure) {
            Iterator<DDEdge> it = edgeList.getRowEnumeration(s1sub);
            while (it.hasNext()) {
              DDEdge edge = it.next();
              if (edge.contains(s2sub)) {
                existsCount++;
                continue find;
              }
            }
          }

          if (useBasic && s2sub.equivalent(s1sub)) { // The two subscript expressions are identical.
            simpleTestCount++;

            if (ok && canDistBeKnownSimply(s1sub)) {
              recordZeroDistDependence(s1sub, s2sub, aname, false, lp);
              continue;
            }

            // A subscript index could be an indirect reference.

            int    dp     = lp.getNestedLevel();
            long[] ddinfo = new long[dp + 1];

            for (int k = 0; k <= dp; k++)
              ddinfo[k] = DDInfo.cDistUnknown;

            recordDependence(s1sub, s2sub, ddinfo, aname, false, lp);
            continue;
          }

          if (useBanerjee) { // Try Banerjee.
            int precedes = s1sub.executionOrder(s2sub);
            int result   = bj.ddTest(s1sub, s2sub, c1lh, c2lh, precedes);

            assert assertTrace(trace, "    bj ", bj.result[result], c1, c2);

            banerjeeTestCount++;

            switch (result) {
            case DataDependence.cIndependent:
              continue;

            case DataDependence.cUnkDependent:
              if (!useOmega) {
                recordNotKnownDependence(s1sub, s2sub, aname, lp);
                continue;
              }
              break;

            case DataDependence.cLCDependent:
              if (!useOmega) {
                process(s1sub, s2sub, aname, bj, true, lp, null);
                continue;
              }
              break;

            case DataDependence.cEqDependent:
              process(s1sub, s2sub, aname, bj, true, lp, null);
              continue;

            default:
              banerjeeFailedCount++;
              break;
            }
          }

          if (useOmega) { // Try the Omega Library.
            int precedes = s1sub.executionOrder(s2sub);
            int result   = ot.ddTest(s1sub, s2sub, c1lh, c2lh, precedes);

            assert assertTrace(trace, "    ot ", ot.result[result], c1, c2);

            omegaTestCount++;

            switch (result) {
            case DataDependence.cIndependent:
              continue;

            case DataDependence.cUnkDependent:
              recordNotKnownDependence(s1sub, s2sub, aname, lp);
              continue;

            case DataDependence.cEqDependent:
            case DataDependence.cLCDependent:
              // Process the results to get any dependences.  Query
              // the dependence formula to check for forward and
              // backward dependences.
              process(s1sub, s2sub, aname, ot, true, lp, null);
              continue;

            default:
              omegaFailedCount++;
              break;
            }
          }

        } catch (java.lang.Throwable ex) {
          assert Debug.printStackTrace(ex);
        }

        // We don't know what the dependence is.
        // Record a dependence of unknown distance.

        recordNotKnownDependence(s1sub, s2sub, aname, lp);
      }
    }
  }

  private boolean assertTrace(boolean trace, String msg, String result, Note sl1, Note sl2)
  {
    if (!trace)
      return true;

    System.out.print(msg);
    System.out.print(result);
    System.out.print(" ");
    System.out.print(sl1.getChord().getSourceLineNumber());
    System.out.print(" ");
    System.out.println(sl2.getChord().getSourceLineNumber());

    return true;
  }

  /**
   * We don't know what the dependence is.
   * Record a dependence of unknown distance.
   */
  private void recordNotKnownDependence(SubscriptExpr   s1sub,
                                        SubscriptExpr   s2sub,
                                        String          aname,
                                        LoopHeaderChord lp)
  {
    int    dp     = lp.getNestedLevel();
    long[] ddinfo = new long[dp + 1];

    for (int k = 0; k <= dp; k++)
      ddinfo[k] = DDInfo.cDistUnknown;

    recordDependence(s1sub, s2sub, ddinfo, aname, false, lp);
  }

  /**
   * Check for spatial dependences between the specified subscripts.
   * @param subscripts a list of subscripts to test.
   */
  private void checkSpatialDependences(Object[] subscripts, String aname) 
  {
    int len = subscripts.length;
    for (int i = 0; i < len; i++) {
      SubscriptExpr   s1sub = (SubscriptExpr) subscripts[i];
      Chord           c1    = s1sub.getChord();
      LoopHeaderChord c1lh  = c1.getLoopHeader();

      for (int j = 0; j < len; j++) {
        SubscriptExpr s2sub = (SubscriptExpr) subscripts[j];
        if (s1sub == s2sub)
          continue;

        Chord c2 = s2sub.getChord();

        if (c1.getLabel() > c2.getLabel())
          continue;

        LoopHeaderChord c2lh  = c2.getLoopHeader();
        LoopHeaderChord lp = c1lh.commonAncestor(c2lh);

        /**
         * Check if spatial reuse exists between s1 and s2.
         * Check spatial locality.
         */

        long[] ddinfo = checkSpatialLocality(s2sub, s1sub, true); // forward
        if (ddinfo != null) { // Add the dependence info to the graph.
          DDEdge edge = recordDependence(s2sub, s1sub, ddinfo, aname, true, lp);
          if (trace)
            edge.printDDInfo(s1sub, s2sub);
        }

        long[] ddinfo1 = checkSpatialLocality(s1sub, s2sub, false); // backward
        if (ddinfo1 != null) { // Add the dependence info to the graph.
          DDEdge edge = recordDependence(s1sub, s2sub, ddinfo1, aname, true, lp);
          if (trace)
            edge.printDDInfo(s1sub, s2sub);
        }
      }
    }
  }

  /**
   *  Query the dependence formula in the direction specified.
   *  Annotate nodes that are dependent on each other.
   */
  private void process(SubscriptExpr   source,
                       SubscriptExpr   sink,
                       String          aname,
                       DataDependence  dd,
                       boolean         forward,
                       LoopHeaderChord lp,
                       HashSet<Object> done)
  {
    Vector<long[]> ddVect = dd.getDependenceInfo(forward);

    if (ddVect == null)
      return;

    int l = ddVect.size();
    for (int i = 0; i < l; i++) {
      long[] ddinfo = ddVect.elementAt(i);
      if (ddinfo == null)
        continue;

      // If the dependence is illegal then fix the direction vector
      // and switch the source and sink.  If we see backward first
      // then the direction vector is invalid.

      boolean valid = true;
      for (int j = 1; j < ddinfo.length; j++) {
	if (DDInfo.isDirectionSet(ddinfo[j], DDInfo.ddForward))
	  break;

        if (DDInfo.isDirectionSet(ddinfo[j], DDInfo.ddBackward)) {
          valid = false;
          break;
        }
      }

      if (!valid) { // Reverse all the dependence directions.
        for (int j = 1; j < ddinfo.length; j++)
          ddinfo[j] = DDInfo.inverseCopy(ddinfo[j]);

        // Reverse source and sink.

        SubscriptExpr temp = source;
        source = sink;
        sink = temp;
      }

      // Add the dependence info to the graph.

      DDEdge edge = recordDependence(source, sink, ddinfo, aname, false, lp);

      if (trace) {
        if (forward)
          System.out.println("DDGraph.process: processing a forward dependence");
        else
          System.out.println("DDGraph.process: processing a backward dependence");

        edge.printDDInfo(source, sink);

        if (dd instanceof OmegaTest)
          ((OmegaTest) dd).printDepRelation();
      
        System.out.print("\n\n");
      }
    }
  }

  private DDEdge recordDependence(SubscriptExpr   source,
                                  SubscriptExpr   sink,
                                  long[]          ddinfo,
                                  String          aname,
                                  boolean         spatial,
                                  LoopHeaderChord lp)
  {
    assert (lp != null) : "No containing loop.";

    if (!DDInfo.isAnyDistanceNonZero(ddinfo))
      return recordZeroDistDependence(source, sink, aname, spatial, lp); // Create a new transitive edge set.

    // Create a new normal edge.

    DDEdge edge = new DDNormalEdge(source, sink, ddinfo, aname, spatial);
 
    // Add a new edge into edge lists and maintain all related lists.

    edgeList.add(source, edge);
    edgeList.add(sink, edge);

    if (trace)
      edge.printDDInfo(source, sink);

    return edge;
  }

  private DDEdge recordZeroDistDependence(SubscriptExpr   source,
                                          SubscriptExpr   sink,
                                          String          aname,
                                          boolean         spatial,
                                          LoopHeaderChord lp)
  {
    if (useTransClosure) {
      Iterator<DDEdge> it1 = edgeList.getRowEnumeration(source);
      while (it1.hasNext()) {
        DDEdge edge = it1.next();
        if (!edge.isTransitive())
          continue;
        if (((DDTransEdge) edge).addEdge(sink, spatial)) { // Add to existing edge - transitive closure.
          edgeList.add(sink, edge);
          if (trace)
            edge.printDDInfo(source, sink);
          return edge;
        }
      }
      Iterator<DDEdge> it2 = edgeList.getRowEnumeration(sink);
      while (it2.hasNext()) {
        DDEdge edge = it2.next();
        if (!edge.isTransitive())
          continue;
        if (((DDTransEdge) edge).addEdge(source, spatial)) { // Add to existing edge - transitive closure.
          edgeList.add(source, edge);
          if (trace)
            edge.printDDInfo(source, sink);
          return edge;
        }
      }
    }

    DDEdge edge = new DDTransEdge(source, sink, aname, spatial); // Create a new transitive edge set.

    // Add a new edge into edge lists and maintain all related lists.

    edgeList.add(source, edge);
    edgeList.add(sink, edge);

    if (trace)
      edge.printDDInfo(source, sink);

    return edge;
  }

  /**
   * Remove all edges from or to this expression.
   */
  public void removeEdges(SubscriptExpr exp)
  {
    Object[] edges = getEdges(exp);
    int      len   = edges.length;
    for (int i = 0; i < len; i++) {
      DDEdge edge = (DDEdge) edges[i];

      if (edge.isTransitive()) {
        ((DDTransEdge) edge).removeEdge(exp);
        continue;
      }

      DDNormalEdge  norm   = (DDNormalEdge) edge;
      SubscriptExpr source = norm.getSource();
      SubscriptExpr sink   = norm.getSink();
      edgeList.remove(sink, edge);
      edgeList.remove(source, edge);
    }
    edgeList.removeRow(exp);
  }

  /**
   * Return true if there is no indeterminate subscript index.
   */
  private boolean canDistBeKnownSimply(SubscriptExpr sub)
  {
    Vector<Expr> expList = new Vector<Expr>(10);
    int          len     = sub.numSubscripts();
    for (int j = 0; j < len; j++) {
      Expr ind = sub.getSubscript(j);
      ind.getExprList(expList);
    }

    int l = expList.size();
    for (int i = 0; i < l; i++) {
      Expr expx = expList.elementAt(i);
      if (expx instanceof LoadValueIndirectExpr)
        return false;

      if (expx instanceof LoadExpr) {
        LoadExpr    le = (LoadExpr) expx;
        Declaration d  = le.getDecl();
        if (d.isGlobal())
          return false;
      }
    }

    return true;
  }

  private long[] checkSpatialLocality(SubscriptExpr n1,
                                      SubscriptExpr n2,
                                      boolean       forward)
  {
    LoopHeaderChord loop = n1.getLoopHeader();

    if ((loop.getNestedLevel() == 0) || (loop != n2.getLoopHeader()))
      return null;
      
    LoopHeaderChord innerLoop     = loop;
    int             reuseDistance = 0;

    if (loop.getNestedLevel() == n2.getLoopHeader().getNestedLevel()) {
      Expr[] ind1 = n1.getSubscripts();
      Expr[] ind2 = n2.getSubscripts();

      loop = loop.getTopLoop();
        
      if (ind1.length != ind2.length)
        return null;

      int distance = 4; // machine.getCacheDistance(t.byteSize(machine)) - 1;
        
      for (int k = 0; k < ind1.length; k++) { // enumerate all subscripts
        Expr       sub1 = ind1[k];  // next index expression
        Expr       sub2 = ind2[k];
        AffineExpr ae1  = loop.isAffine(sub1);
        AffineExpr ae2  = loop.isAffine(sub2);

        if ((ae1 == null) || (ae2 == null))
          return null;

        if (k < (ind1.length - 1)) {
          if (!ae1.equivalent(ae2)) // Other subscripts should be equivalent.
            return null;
          continue;
        }

        // First index for C - Last index for Fortran
        // Find index var and its loop of least significat indice of ae2.

        int          numTerms = ae2.numTerms();
        VariableDecl indexVar = null;        
        int          curDepth = -1;
        long         coff     = 0;
            
        for (int i = 0; i < numTerms; i++) { // Find innermost loop involved in the expression.
          long c = ae2.getCoefficient(i);
          if (c == 0)  
            continue;

          VariableDecl curVar = ae2.getVariable(i);
          if (curVar == null) // constant term         
            continue;

          if (!loop.isLoopIndex(curVar))
            continue;

          if (loop.getNestedLevel() <= curDepth)
            continue;

          indexVar  = curVar;
          innerLoop = loop;

          curDepth  = innerLoop.getNestedLevel();
          coff      = c;
        }
            
        if (indexVar == null) // The index is loop invariant.
          return null;      
            
        // Set of step value for the subscript expression.
        /******************************************/
        // Does n1 need to be a LoadExpr?
        /******************************************/

        if ((n1 == n2) && forward) {
          Type t1      = n1.getPointedToCore();
          int  bitSize = 1;

          if (t1.isRealType())
            bitSize = ((RealType) t1).bitSize() / 32;
              
          n1.setStep((int) (coff * innerLoop.getStepValue() * bitSize));
        }
 
        // Check cross-iteration locality.

        if ((sub1 == sub2) ||
            !forward ||
            !ae1.differenceWithin(ae2, distance)) {                                    
          long step = innerLoop.getStepValue();

          ae2 = ae2.copy();
          for (int i = 0; i < 4; i++) {
            ae2.addTerm(coff * step);             
            if (ae1.differenceWithin(ae2, distance)) {
              reuseDistance = i + 1;
              break;
            }
          }     
              
          if (reuseDistance == 0)
            return null;
        } 
      }
    }

    // Create reuse distance vector.

    if (innerLoop.getNestedLevel() == 0)
      return null;

    int len    = n1.getLoopHeader().getNestedLevel() + 1;
    int dirDep = innerLoop.getNestedLevel();

    if (dirDep >= len) {
      if (trace) {
        System.out.println("** Odd loop structure?");
        System.out.print("   ");
        System.out.print(n1.getChord().getSourceLineNumber());
        System.out.print(" ");
        System.out.println(n1.getChord());
        System.out.print("   ");
        System.out.print(n1.getLoopHeader());
        System.out.print("   ");
        System.out.print(n2.getChord().getSourceLineNumber());
        System.out.print(" ");
        System.out.println(n2.getChord());
        System.out.print("   ");
        System.out.println(n2.getLoopHeader());
        System.out.print("   ");
        System.out.println(innerLoop);
      }
      return null;
    }

    long[] dd = new long[len];
    if (reuseDistance == 0) {
      for (int i = 0; i < dd.length; i++)
        dd[i] = DDInfo.cDist0;
    } else {
      for (int i = 0; i < dd.length; i++)
        dd[i] = DDInfo.create(reuseDistance, true, DDInfo.ddForward);
    }

    return dd;
  }

  private void dumpLocality(SubscriptExpr e, String aname)
  { 
    LoopHeaderChord loop = e.getLoopHeader();
    while (loop.getNestedLevel() != 0) {
      int level = e.getLoopHeader().getNestedLevel();
      if (hasSpatialReuse(e, loop.getNestedLevel(), false, level)) {
        System.out.print(aname);
        System.out.print(e.getDisplayLabel());
        System.out.print(" spatial reuse at ");
        System.out.println(loop.getNestedLevel());
      }
      if (hasTemporalReuse(e, loop.getNestedLevel(), level)) {
        System.out.print(aname);
        System.out.print(e.getDisplayLabel());
        System.out.print(" temporal reuse at ");
        System.out.println(loop.getNestedLevel());
      }
      loop = loop.getParent();
    }
  }

  /**
   * Check if this expression has temporal reuse across iterations of
   * loop.
   * @return true if all distances are equal.
   */
  public boolean hasTemporalReuse(SubscriptExpr srcR, int depth, int checkDepth)
  {
    Object[] edges = getEdges(srcR);
    int      len   = edges.length;
    for (int j = 0; j < len; j++) {
      DDEdge edge = (DDEdge) edges[j];

      if (edge.isSpatial())
        continue;

      if (edge.isTransitive())
        continue;

      DDNormalEdge nedge = (DDNormalEdge) edge;
      if (nedge.getLevel() < depth)
        continue;

      long[] d = nedge.getDDInfo();

      if (DDInfo.isDirectionSet(d[depth], DDInfo.ddBackward))
        continue;

      if (DDInfo.isDirectionSet(d[depth], DDInfo.ddEqual) && (depth != checkDepth))
        continue;

      if (DDInfo.noDirectionSet(d[depth]))
        continue;

      boolean allEqual = true;
      for (int i = 1; i < depth; i++)
        if (!DDInfo.noDirectionSet(d[i]) && !DDInfo.isDirectionSet(d[i], DDInfo.ddEqual)) {
          allEqual = false;
          break;
        }
      if (allEqual)
        return true;
    }

    return false;
  }

  /**
   * Return true if this expression has loop carried temporal reuse
   * across iterations of loop.
   */
  public boolean hasLoopCarriedTemporalReuse(SubscriptExpr srcR,
                                             int           depth,
                                             int           checkDepth)
  {
    Object[] edges = getEdges(srcR);
    int      len   = edges.length;
    for (int j = 0; j < len; j++) {
      DDEdge edge = (DDEdge) edges[j];

      if (edge.isSpatial())
        continue;

      if (edge.isLoopIndependentDependency())
        continue;

      if (edge.isTransitive())
        continue;

      DDNormalEdge nedge = (DDNormalEdge) edge;
      if (nedge.getLevel() < depth)
        continue;

      long[] d = edge.getDDInfo();
        
      if (DDInfo.isDirectionSet(d[depth], DDInfo.ddBackward))
        continue;

      if (DDInfo.isDirectionSet(d[depth], DDInfo.ddEqual) && (depth != checkDepth))
        continue;

      if (DDInfo.noDirectionSet(d[depth]))
        continue;

      boolean allEqual = true;
      for (int i = 1; i < depth; i++)
        if (!DDInfo.noDirectionSet(d[i]) && !DDInfo.isDirectionSet(d[i], DDInfo.ddEqual)) {
          allEqual = false;
          break;
        }
      if (allEqual)
        return true;
    }

    return false;
  }

  /**
   * Return true if this expression has spatial reuse across
   * iterations of the loop.
   */
  public boolean hasSpatialReuse(SubscriptExpr srcR,
                                 int           depth,
                                 boolean       group,
                                 int           checkDepth)
  {
    Object[] edges = getEdges(srcR);
    int      len   = edges.length;
    for (int j = 0; j < len; j++) {
      DDEdge edge = (DDEdge) edges[j];

      if (!edge.isSpatial())
        continue;
        
      if (group) // It's a self edge only.
        continue;
            
      if (edge.isTransitive())
        continue;

      DDNormalEdge nedge = (DDNormalEdge) edge;
      if (nedge.getLevel() < depth)
        continue;

      long[] d = edge.getDDInfo();
      if (DDInfo.isDirectionSet(d[depth], DDInfo.ddBackward))
        continue;

      if (DDInfo.isDirectionSet(d[depth], DDInfo.ddEqual) && (depth != checkDepth))
        continue;

      boolean allEqual = true;
      for (int i = 0; i < depth; i++)
        if (!DDInfo.isDirectionSet(d[i], DDInfo.ddEqual)) {
          allEqual = false;
          break;
        }

      if (allEqual) {
        for (int i = depth + 1; i < d.length; i++)
          if (!DDInfo.isDirectionSet(d[i], DDInfo.ddEqual)) {
            allEqual = false;
            break;
          } 

        if (allEqual)
          return true;
      }
    }

    return false;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(DDGraph ");
    buf.append(createSpatialDependencies ? "spatial " : "");
    buf.append(allComputed ? "all " : "");
    buf.append(edgeList);
    buf.append(')');
    return buf.toString();
  }
}
