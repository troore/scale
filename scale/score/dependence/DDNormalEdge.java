package scale.score.dependence;

import java.util.Iterator;

import scale.score.*;
import scale.score.expr.Expr;
import scale.score.chords.Chord;
import scale.score.chords.ExprChord;
import scale.score.chords.LoopHeaderChord;
import scale.score.expr.SubscriptExpr;
import scale.common.*;

/**
 * This class represents a set of dependence edges from one source to
 * one sink in the data dependence graph.
 * <p>
 * $Id: DDNormalEdge.java,v 1.20 2007-10-04 19:58:24 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>, <br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>, <br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see DDEdge
 * @see DDTransEdge
 * @see DataDependence
 * @see DDGraph
 * @see DDInfo
 */
public final class DDNormalEdge extends DDEdge
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.score.dependence.DDNormalEdge", stats);
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  private static Vector<long[]> infos = new Vector<long[]>();

  private SubscriptExpr source; // The starting end of the edge.
  private SubscriptExpr sink;   // The ending end of the edge.
  private long[]        ddinfo; // The computed data dependence info by loop level.
  private int           level;  // The lowest nested level of one end of the edge.

  /**
   * Create an edge for the data dependence graph.  The creater of the
   * edge must guarantee that <code>src</code> precedes
   * <code>sink</code>.
   * @param src is the one end of the edge
   * @param sink is the other end of the edge
   * @param ddinfo is the computed data dependence information
   * @param aname is the name of array (or scalar) involved in the dependence
   * @param spatial is true if the edge records a spatial dependence
   * @see DataDependence
   * @see DDGraph
   */
  public DDNormalEdge(SubscriptExpr src,
                      SubscriptExpr sink,
                      long[]        ddinfo,
                      String        aname,
                      boolean       spatial)
  {
    super(aname, spatial);
    this.source  = src;
    this.sink    = sink;

    int     l  = infos.size();
    boolean ni = true;
    outer:
    for (int i = 0; i < l; i++) {
      long[] info = infos.elementAt(i);
      if (info.length != ddinfo.length)
        continue;
      for (int j = 0; j < info.length; j++) {
        if (info[j] != ddinfo[j])
          continue outer;
      }
      ni = false;
      ddinfo = info;
      break;
    }

    if (ni)
      infos.addElement(ddinfo);

    this.ddinfo  = ddinfo;

    this.level = src.getLoopHeader().getNestedLevel();

    int lev2 = sink.getLoopHeader().getNestedLevel();
    if (lev2 < this.level)
      this.level = lev2;

    createdCount++;
  }

  /**
   * Return the source end of the edge.
   */
  public SubscriptExpr getSource()
  {
    return source;
  }

  /**
   * Return the sink end of the edge.
   */
  public SubscriptExpr getSink()
  {
    return sink;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(DDNormalEdge-");
    buf.append(getNodeID());
    buf.append(' ');
    buf.append(getArrayName());
    Chord c1 = source.getChord();
    if (c1 != null) {
      buf.append(' ');
      buf.append(c1.getSourceLineNumber());
    }
    Chord c2 = sink.getChord();
    if (c2 != null) {
      buf.append(' ');
      buf.append(c2.getSourceLineNumber());
    }
    buf.append(' ');
    if (isSpatial())
      buf.append("spatial ");
    buf.append(formatDDInfo());
    buf.append(' ');
    buf.append(source);
    buf.append(' ');
    buf.append(sink);
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return an {@link java.util.Iterator iterator} over the {@link
   * scale.score.expr.SubscriptExpr SubscriptExpr} instances that are
   * the edge ends.
   */
  public Iterator<SubscriptExpr> iterator()
  {
    return new DoubleIterator<SubscriptExpr>(source, sink);
  }

  /**
   * Return true if the expression is an end of an edge represented by
   * this instance.
   */
  public boolean contains(SubscriptExpr exp)
  {
    if (exp == source)
      return true;
    return (exp == sink);
  }

  /**
   * Return true if every edge represented is an input edge.
   */
  public boolean representsAllInput()
  {
    Vector<Note> uses = new Vector<Note>();
    source.addUses(uses);
    sink.addUses(uses);

    int len = uses.size();
    for (int i = 0; i < len - 1; i++) {
      Note src = uses.elementAt(i);
      for (int j = i + 1; j < len; j++) {
        Note sink = uses.elementAt(j);
        if (getEdgeType(src, sink) != cInput)
          return false;
      }
    }

    return true;
  }

  /**
   * Add the {@link scale.score.expr.SubscriptExpr SubscriptExpr}
   * instances, that are the edge ends, to the Vector.
   */
  public void getEnds(Vector<Note> v)
  {
    source.addUses(v);
    sink.addUses(v);
  }

  /**
   * Return a metric for the number of data dependence edges
   * represented.  This is the number of edge end points - 1.
   */
  public int numberEdges()
  {
    return 1;
  }

  /**
   * Return the computed data dependence information.
   */
  public long[] getDDInfo()
  {
    return ddinfo;
  }

  /**
   * Return true if the edge is loop-independent dependency.
   */
  public boolean isLoopIndependentDependency()
  {
    for (int i = 0; i < ddinfo.length; i++) {
      if (!DDInfo.isDirectionEqual(ddinfo[i]))
        return false;
    }
    return true;
  }

  /**
   * Return the distance for the specified level.
   */
  public int getDistance(int level)
  {
    if (level >= ddinfo.length)
      return -999;

    return DDInfo.getDistance(ddinfo[level]);
  }

  /**
   * Return true if the distance is known at the specified level.
   */
  public boolean isDistanceKnown(int level)
  {
    if (level >= ddinfo.length)
      return false;
    return DDInfo.isDistanceKnown(ddinfo[level]);
  }

  /**
   * Return true if the distance is known at any level.
   */
  public boolean isAnyDistanceKnown()
  {
    return DDInfo.isAnyDistanceKnown(ddinfo);
  }

  /**
   * Return true if the distance is not known at any level.
   */
  public boolean isAnyDistanceNotKnown()
  {
    return DDInfo.isAnyDistanceNotKnown(ddinfo);
  }

  /**
   * Return true if any distance is unknown or not zero at any level.
   */
  public boolean isAnyDistanceNonZero()
  {
    return DDInfo.isAnyDistanceNonZero(ddinfo);
  }

  /**
   * Return true if this is a transitive edge set.
   */
  public  boolean isTransitive()
  {
    return false;
  }

  /**
   * Return the string representation of a whole dependence vector.
   */
  private String formatDDInfo()
  {
    StringBuffer buf = new StringBuffer();
    buf.append('(');
    for (int i = 0; i < ddinfo.length; i++) {
      if (i > 0)
        buf.append(',');
      buf.append(DDInfo.toString(ddinfo[i]));
    }
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return the data dependence type - {@link DDEdge#cFlow flow},
   * {@link DDEdge#cAnti anti}, {@link DDEdge#cInput input}, or {@link
   * DDEdge#cOutput output}.  This logic assumes that the <tt>s1</tt>
   * precedes the <tt>s2</tt>.  The <tt>source</tt> and <tt>sink</tt>
   * must the uses of the addresses represented by the {@link
   * scale.score.expr.SubscriptExpr SubscriptExpr} instances that are
   * the ends of the data dependence edge.
   * @see DDGraph
   * @see DataDependence
   */
  public int getEdgeType(Note source, Note sink)
  {
    assert (this.source.isUse(source) && this.sink.isUse(sink)) :
      "Not ends of this edge.";

    boolean       s1Write  = ((source instanceof ExprChord) ||
                              ((source instanceof Expr) &&
                               ((Expr) source).isMemoryDef()));
    boolean       s2Write  = ((sink instanceof ExprChord) ||
                              ((sink instanceof Expr) &&
                               ((Expr) sink).isMemoryDef()));
    int           oitype   = cFlow; // FLOW dependence
    int           iotype   = cAnti;

    // Determine if the subscript is a read or a write.

    if (s1Write) {
      if (s2Write) { // OUTPUT dependence
        oitype = cOutput;
        iotype = cOutput;
      } else { // Flow dependence
        oitype = cFlow;
        iotype = cAnti;
      }
    } else if (!s2Write) { // INPUT dependence
      oitype = cInput;
      iotype = cInput;
    } else { // Anti dependence
      iotype = cFlow;
      oitype = cAnti;
    }

    // If this is a loop independent dependence then we have
    // to make sure the source precedes the sink.

    if (isLoopIndependentDependency()) {
      int lab1 = source.getChord().getLabel();
      int lab2 = sink.getChord().getLabel();
      if (lab1 < lab2)
        return oitype;
      if (lab1 > lab2)
        return cNone;
      if (source instanceof ExprChord)
        return oitype;
      return cNone;
    }
  
    // This is a loop carried dependence so array ref order doesn't matter.

    return oitype;
  }

  /**
   * Print to stdout the information about the data dependence.
   */
  public void printDDInfo(Note s1, Note s2)
  {
    System.out.println("** source " + s1);
    System.out.println("   sink   " + s2);
    System.out.print(format(s1, s2, getArrayName(), getEdgeType(s1, s2)));
    System.out.print(" (");

    if (ddinfo.length > 1)
      System.out.print(DDInfo.toString(ddinfo[1]));

    for (int j = 2; j < ddinfo.length; j++) {
      System.out.print(",");
      System.out.print(DDInfo.toString(ddinfo[j]));
    }

    System.out.print(") (");
    if (ddinfo.length > 1)
      System.out.print(DDInfo.isDistanceKnown(ddinfo[1]));

    for (int j = 2; j < ddinfo.length; j++) {
      System.out.print(",");
      System.out.print(DDInfo.isDistanceKnown(ddinfo[j]));
    }

    System.out.print(") (");
    if (ddinfo.length > 1)
      System.out.print(DDInfo.getDistance(ddinfo[1]));

    for (int j = 2; j < ddinfo.length; j++) {
      System.out.print(",");
      System.out.print(DDInfo.getDistance(ddinfo[j]));
    }

    System.out.println(")");
  }

  public String format(Note s1, Note s2, String aname, int ddtype)
  {
    assert (source.isUse(s1) && sink.isUse(s2)) : "Not ends of this edge.";

    StringBuffer sb   = new StringBuffer("N ");

    if (isSpatial())
      sb.append("spatial_");

    sb.append(dependenceName[ddtype]);
    sb.append(' ');

    if (sb.length() < 10)
      sb.append("        ".substring(0, 10 - sb.length()));

    for (int i = 0; i < ddinfo.length; i++)
      sb.append(DDInfo.toString(ddinfo[i]));

    if (sb.length() < 15)
      sb.append("             ".substring(0, 15 - sb.length()));

    int index = sb.length();
    Chord c1   = s1.getChord();
    if (c1 != null) {
      int   s1ln = c1.getSourceLineNumber();
      if (s1ln >= 0) {
        String s1lns = Integer.toString(s1ln);
        int    len   = s1lns.length();

        if (len < 4)
          sb.append("     ".substring(0, 4 - len));

        sb.append(s1lns);
        sb.append(':');
      }
    }

    sb.append(aname);
    sb.append('#');
    sb.append(s1.getDisplayLabel());

    if (sb.length() > (index + 30)) {
      sb.setLength(index + 27);
      sb.append("...");
    } else if (sb.length() < (index + 20)) {
      sb.append("                     ".substring(0, index + 20 - sb.length()));
    }

    sb.append(" --> ");
    index = sb.length();

    Chord c2   = s2.getChord();
    if (c2 != null) {
      int   s2ln = c2.getSourceLineNumber();
      if (s2ln >= 0) {
        String s2lns = Integer.toString(s2ln);
        int    len   = s2lns.length();

        if (len < 4)
          sb.append("     ".substring(0, 4 - len));

        sb.append(s2lns);
        sb.append(':');
      }
    }

    sb.append(aname);
    sb.append('#');
    sb.append(s2.getDisplayLabel());

    if (sb.length() > (index + 30)) {
      sb.setLength(index + 27);
      sb.append("...");
    } else if (sb.length() < (index + 20)) {
      sb.append("                     ".substring(0, index + 20 - sb.length()));
    }

    return sb.toString();    
  }

  /**
   * Create a graphic display of the edges represented by this instancce.
   * @param da is the graph display
   * @param addChord is true if the ends of each edge should be added
   * to the <code>nodes</code> set
   * @param nodes is the set of
   */
  public void graphDependence(DisplayGraph  da,
                              boolean       addChord,
                              HashSet<Note> nodes,
                              DDGraph       graph)
  {
    if (addChord) {
      nodes.add(source);
      nodes.add(sink);
    }

    Vector<Note> srcs  = new Vector<Note>();
    Vector<Note> sinks = new Vector<Note>();
    source.addUses(srcs);
    sink.addUses(sinks);

    int srcl = srcs.size();
    int snkl = sinks.size();
    for (int i = 0; i < srcl; i++) {
      Note src = srcs.elementAt(i);
      for (int j = 0; j < snkl; j++) {
        Note   snk      = sinks.elementAt(j);
        int    dt       = getEdgeType(src, snk);
        Object edgeInfo = format(src, snk, getArrayName(), dt);

        // Display the edge on the graph.  Note multiple edges may go
        // from the same source to the same sink and be displayed on
        // top of one another.

        da.addEdge(src, snk, colors[dt], lineType[dt], edgeInfo);
      }
    }
  }

  /**
   * Return true if this edge has a source or sink in the specified
   * loop.
   */
  public boolean forLoop(LoopHeaderChord loop)
  {
    return ((source.getLoopHeader() == loop) || (sink.getLoopHeader() == loop));
  }

  /**
   * Return the level of dependence.
   */
  public final int getLevel()
  {
    return level;
  }
}
