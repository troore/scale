package scale.visual;

import java.io.*;

import java.util.StringTokenizer;

import scale.common.*;

/**
 * This class implements methods to generate commands to display a
 * graph using vcg.
 * <p>
 * $Id: Vcg.java,v 1.5 2007-10-04 19:58:41 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class Vcg extends DisplayGraph
{
  private static final String[] vcgShapes   = {"box", "rhomb", "ellipse", "ellipse"};
  private static final String[] vcgPatterns = {"continuous", "dashed", "dashed", "dotted", "continuous"};

  private HashMap<String, Object>                  map;        // Map from name to node or edge.
  private HashMap<String, HashMap<String, Object>> contextMap; // Map from conext to map of nodes and edges.
  private HashSet<DisplayNode>                     snodes;     // Set of all nodes and edges processed.
  private StringBuffer nodes;          // Node descriptions.
  private StringBuffer edges;          // Edge descriptions.
  private String       context;
  private int          edgeLabelCount; // Edge label counter.
  private boolean      top;            // Places the root node at the top of the display if true.

  public Vcg()
  {
    this.edgeLabelCount = 0;
  }

  /**
   * Initialize for a new graph.
   * Only one graph can be created at a time.
   * @param context is the name associated with the graph
   * @param top if true places the root node at the top of the display
   */
  public void newGraph(String context, boolean top)
  {
    this.context = context;
    this.top     = top;
    this.map     = new HashMap<String, Object>(203);
    this.nodes   = new StringBuffer();
    this.edges   = new StringBuffer();
    this.snodes  = new HashSet<DisplayNode>(203);

    if (this.contextMap == null)
      this.contextMap = new HashMap<String, HashMap<String, Object>>(11);
    this.contextMap.put(context, this.map);
  }

  /**
   * Retrn attribute for dotted edges.
   */
  public String dottedEdgeAttr()
  {
    return "dotted";
  }

  /**
   * Return attribute for dashed edges.
   */
  public String dashedEdgeAttr()
  {
    return "dashed";
  }

  /**
   * Return true if the node has been processed.
   * @param n is the node to check
   */
  public boolean visited(DisplayNode n)
  {
    return !snodes.add(n);
  }

  /**
   * Add a node to the graph.
   * This method should be used only for nodes that are not connected
   * with an edge.
   * @param n is the node
   */
  public void addNode(DisplayNode n)
  {
    String nodeID = n.getDisplayName();
    if (map.put(nodeID, n) != null)
      return;

    nodes.append("node: {\n  title: \"");
    nodes.append(nodeID);
    nodes.append("\"\n  label: \"");
    nodes.append(n.getDisplayLabel());
    nodes.append("\"\n  color: ");
    nodes.append(n.getDisplayColorHint().sName());
    nodes.append("\n  shape: ");
    nodes.append(vcgShapes[n.getDisplayShapeHint().ordinal()]);
    nodes.append("\n  }\n");
  }

  private String formatEdgeId(String n1, String n2)
  {
    StringBuffer s = new StringBuffer(n1);
    s.append("->");
    s.append(n2);
    s.append(" <");
    s.append(String.valueOf(edgeLabelCount++)); // make them unique in case of duplicates
    s.append('>');
    return s.toString();
  }

  /**
   * Add an edge to the graph from node n1 to node n2.  Attributes of
   * the edge are specified by an integer that contains three fields.
   * <table>
   * <thead>
   * <tr><th>bits</th><th>use</th>
   * </thead>
   * <tbody>
   * <tr><td>5-0</td><td>color</td>
   * <tr><td>6</td><td>type: normal, backedge</td>
   * <tr><td>8-7</td>pattern: solid, dashed, dotted<td></td>
   * </tbody>
   * </table>
   * @param n1 is the first node
   * @param n2 is the second node
   * @param color specifies the edge color
   * @param edgeAttributes specifies the type, and form of the edge
   * @param edgeInfo is additional information about the edge
   */
  public void addEdge(DisplayNode n1,
                      DisplayNode n2,
                      DColor      color,
                      DEdge       edgeAttributes,
                      Object      edgeInfo)
  {
    if ((n1 == null) || (n2 == null))
      return; // No edge if one end doesn't exist.

    addNode(n1);
    addNode(n2);

    if (top) {
      DisplayNode t = n1;
      n1 = n2;
      n2 = t;
    }

    String n1n = n1.getDisplayName();
    String n2n = n2.getDisplayName();
    String en  = formatEdgeId(n1n, n2n);

    if (edgeInfo != null)
      map.put(en, edgeInfo);

    String et = (edgeAttributes == DEdge.BACK) ? "backedge" : "edge";

    StringBuffer s  = new StringBuffer(et);
    s.append(": {\n  sourcename: \"");
    s.append(n1n);
    s.append("\"\n  targetname: \"");
    s.append(n2n);
    s.append("\"\n  label: \"");
    s.append((edgeInfo == null) ? en : edgeInfo);
    s.append("\"\n  color: ");
    s.append(color.ordinal());
    s.append("\n  linestyle: ");
    s.append(vcgPatterns[edgeAttributes.ordinal()]);
    s.append("\n  }\n");
    edges.append(s.toString());
  }

  /**
   * Terminates the visualizer process.
   */
  public void terminate()
  {
    contextMap = null;
  }

  /**
   * This opens a new visualization window.
   * The context string should be unique from that used for any
   * other window.
   * @param context is the user defined string to distinguish between
   * different windows
   * @param title is title for the window
   */
  public void openWindow(String context, String title, int graphAttributes)
  {
    // Specify a new context (i.e., a new window).

    StringBuffer buf = new StringBuffer(outputPath);
    buf.append(File.separator);
    buf.append((title == null) ? context : title);
    buf.append(".vcg");
    String filename = buf.toString().replace(' ', '_');;

    try {
      PrintStream ps = new PrintStream(new FileOutputStream(filename));
      ps.println("graph: {");
      ps.print("  title:  \"");
      ps.print(context);
      ps.println("\"");

      for (DColor color : DColor.values()) {
        ps.print("  colorentry ");
        ps.print(color.ordinal());
        ps.print(" : ");
        ps.print(color.red());
        ps.print(" ");
        ps.print(color.green());
        ps.print(" ");
        ps.println(color.blue());
      }

      if ((graphAttributes & DISPLAY_EDGE_LABELS) != 0)
        ps.println("display_edge_labels: yes");
      ps.println(nodes.toString());
      ps.println(edges.toString());
      ps.println("}");
      ps.close();
    } catch (IOException ioex) {
      Msg.reportWarning(Msg.MSG_s, null, 0, 0, ioex.getMessage());
    }
  }

  /**
   * This closes an existing new visualization window.
   * The context string should be unique from that used for any
   * other window.
   * @param context is the user defined string to distinguish between
   * different windows
   */
  public void closeWindow(String context)
  {
  }

  /**
   * Return true if the window for the given context still exists.
   */
  public boolean windowExists(String context)
  {
    return false; // I don't have time to do the right thing now.
  }

  /**
   * Respond to interative events with this display.
   * The method terminates when the display is terminated.
   */
  public void interact()
  {
  }
}
