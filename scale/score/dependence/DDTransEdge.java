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
 * This class represents the set of edges, in the data dependence
 * graph, that have a distance of 0 and the same direction for some
 * array in some loop.  It forms a transitive closure.
 * <p>
 * $Id: DDTransEdge.java,v 1.22 2007-10-04 19:58:24 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>, <br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>, <br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An instance of this class represents a set of data dependence edges
 * for an array where the edge distance is zero.  For example if there
 * is an edge from <code>A</code> to <code>B</code> of zero distance,
 * and there is an edge from <code>A</code> to <code>C</code> of zero
 * distance, then there is also an edge from <code>B</code> to
 * <code>C</code> of zero distance.
 * <p>
 * @see DDEdge
 * @see DDNormalEdge
 * @see DataDependence
 * @see DDGraph
 */
public final class DDTransEdge extends DDEdge
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.
  private static int endCount     = 0; // A count of the number of edge ends.

  private static final String[] stats = {"numberEnds", "created"};

  static
  {
    Statistics.register("scale.score.dependence.DDTransEdge", stats);
  }

  /**
   * Return the number of data dependence edges represented.
   */
  public static int numberEnds()
  {
    return endCount;
  }

  /**
   * Return the number of instances of this class that were created.
   */
  public static int created()
  {
    return createdCount;
  }

  private SubscriptExpr[] ends; // The ends of the edges - a vector of Expr instances.
  private int             num;  // The number of end points.

  /**
   * Create an edge for the data dependence graph.
   * @param end1 is the one end of the edge
   * @param end2 is the other end of the edge
   * @param aname is the name of array (or scalar) involved in the dependence
   * @param spatial is true if the edge records a spatial dependence
   * @see DataDependence
   * @see DDGraph
   */
  public DDTransEdge(SubscriptExpr end1, SubscriptExpr end2, String aname, boolean spatial)
  {
    super(aname, spatial);
    this.ends    = new SubscriptExpr[3];

    this.ends[0] = end1;
    this.num     = 1;
    endCount++;

    if (end1 != end2) {
      this.ends[1] = end2;
      this.num++;
      endCount++;
    }

    createdCount++;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(DDTransEdge-");
    buf.append(getNodeID());
    buf.append(' ');
    buf.append(getArrayName());
    buf.append(' ');
    if (isSpatial())
      buf.append("spatial ");
    buf.append('0');
    int len = num;
    for (int i = 0; i < len; i++) {
      Expr end = ends[i];
      Chord c = end.getChord();
      buf.append(' ');
      buf.append(end);
      if (c != null) {
        buf.append(' ');
        buf.append(ends[i].getChord().getSourceLineNumber());
      }
    }
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return an {@link java.util.Iterator iterator} over the
   * {@link scale.score.expr.SubscriptExpr SubscriptExpr} instances that are the edge ends.
   */
  public Iterator<SubscriptExpr> iterator()
  {
    return new EdgeIterator<SubscriptExpr>(ends, num);
  }

  private class EdgeIterator<T> implements Iterator<T>
  {
    private int index;
    private T[] ends;

    public EdgeIterator(T[] ends, int num)
    {
      this.index = 0;
      this.ends = ends;
    }

    public boolean hasNext()
    {
      return index < num;
    }

    public T next()
    {
      return ends[index++];
    }

    public void remove()
    {
      System.arraycopy(ends, index + 1, ends, index, ends.length - index - 1);
      num--;
      throw new scale.common.InternalError("Remove used!");
    }
  }

  /**
   * Return true if the expression is an end of an edge represented by this instance.
   */
  public boolean contains(SubscriptExpr exp)
  {
    for (int i = 0; i < ends.length; i++)
      if (ends[i] == exp)
        return true;
    return false;
  }

  /**
   * Return true if every edge represented is an input edge.
   */
  public boolean representsAllInput()
  {
    Vector<Note> uses = new Vector<Note>();
    for (int i = 0; i < ends.length; i++) {
      SubscriptExpr src = ends[i];
      src.addUses(uses);
    }

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
   * Specify another data dependence edge end point.
   * If the loop containing the end point is a sub-loop or parent
   * loop of the edge's characteristic loop and the data dependence information
   * matches, the end point is added to this transitive edge.
   * Only edges with zero distance should be added.
   * @param spatial is true if the edge records a spatial dependence
   * @return true if the end point was added
   */
  public boolean addEdge(SubscriptExpr exp, boolean spatial)
  {
    if (spatial ^ this.isSpatial())
      return false;

    for (int i = 0; i < num; i++)
      if (ends[i] == exp)
        return false;

    if (num >= ends.length) {
      SubscriptExpr[] ne = new SubscriptExpr[num * 2];
      System.arraycopy(ends, 0, ne, 0, num);
      ends = ne;
    }

    endCount++;
    ends[num] = exp;
    num++;

    return true;
  }

  /**
   * Remove to / from this expression.
   * Return the number of edge ends.
   */
  public int removeEdge(SubscriptExpr exp)
  {
    int i;
    for (i = 0; i < ends.length; i++)
      if (ends[i] == exp)
        break;

    if (i >= ends.length)
      return num;

    System.arraycopy(ends, i + 1, ends, i, ends.length - i - 1);
    num--;
    return num;
  }

  /**
   * Add the {@link scale.score.expr.SubscriptExpr SubscriptExpr} instances,
   * that are the edge ends, to the Vector.
   */
  public void getEnds(Vector<Note> v)
  {
    for (int i = 0; i < num; i++)
      ends[i].addUses(v);
  }

  /**
   * Return a metric for the number of data dependence edges represented.
   * This is the number of edge end points - 1.
   */
  public int numberEdges()
  {
    return (num - 1);
  }

  /**
   * Return the computed data dependence information.
   */
  public long[] getDDInfo()
  {
    return null;
  }

  /**
   * Return true if the edge is loop-independent dependency.
   */
  public boolean isLoopIndependentDependency()
  {
    return true;
  }

  /**
   * Return the distance for the specified level.
   */
  public int getDistance(int level)
  {
    return 0;
  }

  /**
   * Return true if the distance is known at the specified level.
   */
  public boolean isDistanceKnown(int level)
  {
    return true;
  }

  /**
   * Return true if the distance is known at any level.
   */
  public boolean isAnyDistanceKnown()
  {
    return true;
  }

  /**
   * Return true if the distance is not known at any level.
   */
  public boolean isAnyDistanceNotKnown()
  {
    return false;
  }

  /**
   * Return true if any distance is unknown or not zero at any level.
   */
  public boolean isAnyDistanceNonZero()
  {
    return false;
  }

  /**
   * Return true if this is a transitive edge set.
   */
  public  boolean isTransitive()
  {
    return true;
  }

  /**
   * Return the data dependence type - {@link DDEdge#cFlow flow},
   * {@link DDEdge#cAnti anti}, {@link DDEdge#cInput input}, or {@link
   * DDEdge#cOutput output}.  This logic depends on the CFG nodes
   * being labeled.  The <tt>source</tt> and <tt>sink</tt> must the
   * uses of the address represented by the {@link
   * scale.score.expr.SubscriptExpr SubscriptExpr} instances that are
   * the ends of the data dependence edge.
   * @see DDGraph
   * @see DataDependence
   */
  public int getEdgeType(Note source, Note sink)
  {
    assert chkEnds(source, sink) : "Not ends of this edge.";

      boolean s1Write  = ((source instanceof ExprChord) ||
                          ((source instanceof Expr) && ((Expr) source).isMemoryDef())); // Determine if the subscript is a read or a write.
      boolean s2Write  = ((sink instanceof ExprChord) ||
                          ((sink instanceof Expr) && ((Expr) sink).isMemoryDef()));
      int     oitype   = cFlow; // FLOW dependence
      int     iotype   = cAnti;

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

  public boolean chkEnds(Note source, Note sink)
  {
    boolean i1 = false;
    for (int i = 0; i < num; i++) {
      SubscriptExpr end = ends[i];
      i1 = (source == end) || end.isUse(source);
      if (i1)
        break;
    }
    if (!i1)
      return false;

    boolean i2 = false;
    for (int i = 0; i < num; i++) {
      SubscriptExpr end = ends[i];
      i2 = (sink == end) || end.isUse(sink);
      if (i2)
        break;
    }
    if (!i2)
      return false;

    return true;
  }

  /**
   * Print to stdout the information about the data dependence.
   */
  public void printDDInfo(Note source, Note sink)
  {
    System.out.println("** source " + source);
    System.out.println("   sink   " + sink);
    System.out.println(format(source, sink, getArrayName(), getEdgeType(source, sink)));
  }

  public String format(Note s1, Note s2, String aname, int ddtype)
  {
    StringBuffer sb = new StringBuffer("T ");

    if (isSpatial())
      sb.append("spatial_");

    sb.append(dependenceName[ddtype]);
    sb.append(' ');

    if (sb.length() < 10)
      sb.append("        ".substring(0, 10 - sb.length()));

    sb.append('0');

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
    Vector<Note> uses = new Vector<Note>();
    for (int j = 0; j < num; j++) {
      SubscriptExpr end = ends[j];
      end.addUses(uses);
    }

    // Display the edges as a ring.

    int l = uses.size();
    for (int j = 0; j < l; j++) {
      Note src = uses.elementAt(j);

      if (addChord)
        nodes.add(src);

      int k = j + 1;
      if (k >= l)
        k = 0;

      Note sink = uses.elementAt(k);

      if (addChord)
        nodes.add(sink);

      int    dt       = getEdgeType(src, sink);
      Object edgeInfo = format(src, sink, getArrayName(), dt);

      // Display the edge on the graph.  Note multiple edges may go from
      // the same source to the same sink and be displayed on top of one another.

 
      da.addEdge(src, sink, colors[dt], lineType[dt], edgeInfo);
    }
  }

  /**
   * Return true if this edge has a source or sink in the specified loop.
   */
  public boolean forLoop(LoopHeaderChord loop)
  {
    for (int i = 0; i < num; i++)
      if (ends[i].getLoopHeader() == loop)
        return true;
    return false;
  }
}
