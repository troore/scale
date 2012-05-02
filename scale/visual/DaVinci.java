package scale.visual;

import java.io.*;

import java.util.StringTokenizer;

import scale.common.*;

/**
 * This class implements methods to generate commands to display a
 * graph using daVinci.
 * <p>
 * $Id: DaVinci.java,v 1.40 2007-10-04 19:58:40 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This wrapper uses the daVinci program to provide an Xwindow
 * display of a graph.  This wrapper assumes a single tasking
 * environment, but supports multiple graphs.
 * <p>
 * A user of this class will in general, have the following code:
 * <ol>
 *   <li> visualizer = new DaVinci(); </li>
 *   <li> visualizer.newGraph(name, top); </li>
 *     <p> Called once per graph. </p>
 *   <li> create the graph using visualizer instance </li>
 *   <li> visualizer.openWindow(name, title); </li>
 * </ol>
 * The openWindow() method must be called before newGraph() is called again.
 */
public class DaVinci extends DisplayGraph
{
  private HashMap<String, Object>                  map;            // Map from name to node or edge
  private HashMap<String, HashMap<String, Object>> contextMap;     // Map from conext to map of nodes and edges.
  private HashSet<DisplayNode>                     snodes;         // Set of all nodes and edges processed.

  private StringBuffer nodes;          // Node descriptions.
  private StringBuffer edges;          // Edge descriptions.
  private External     visualizer;     // The external process running the DaVinci system.
  private int          edgeLabelCount; // Edge label counter
  private boolean      top;            // Places the root node at the top of the display if true.

  public DaVinci()
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
    this.top    = top;
    this.map    = new HashMap<String, Object>(203);
    this.nodes  = new StringBuffer();
    this.edges  = new StringBuffer();
    this.snodes = new HashSet<DisplayNode>(203);

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

    nodes.append("new_node(\"");
    nodes.append(nodeID);
    nodes.append("\",\"\",[a(\"OBJECT\",\"");
    nodes.append(n.getDisplayLabel());
    nodes.append(" (");
    nodes.append(nodeID);
    nodes.append(")\"),a(\"COLOR\",\"");
    nodes.append(n.getDisplayColorHint().sName());
    nodes.append("\"),a(\"_GO\",\"");
    nodes.append(n.getDisplayShapeHint().sName());
    nodes.append("\")]),");
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

    StringBuffer s  = new StringBuffer("new_edge(\"");
    s.append(en);
    s.append("\",\"\",[a(\"EDGECOLOR\",\"");
    s.append(color.sName());
    s.append("\")");
    if (edgeAttributes != DEdge.SOLID) {
      s.append(",a(\"EDGEPATTERN\",\"");
      s.append(edgeAttributes.sName());
      s.append("\")");
    }
    s.append("],\"");
    s.append(n1n);
    s.append("\",\"");
    s.append(n2n);
    s.append("\"),");
    edges.append(s.toString());
  }

  /**
   * Return the graph representation for daVinci which results from
   * having traversed the Clef tree.
   */
  private void addGraphCommands(StringBuffer buf)
  {
    buf.append("[");
    buf.append(nodes.toString());
    buf.append("],[");
    buf.append(edges.toString());
    buf.append("]");

    edges = new StringBuffer();  // Clear the string buffer.
    nodes = new StringBuffer();  // Clear the string buffer.
  }

  /**
   * Return the node or edge represented by the key in the specified graph.
   */
  public Object getLocation(String context, String key)
  {
    HashMap<String, Object> nm = contextMap.get(context);
    if (nm == null)
      return null;
    return nm.get(key);
  }

  /**
   * Print the node at the location.
   * @param location is the position returned by DaVinci when a mouse
   * button is clicked.
   */
  private String clickedNode(String context, String location)
  {
    DisplayNode n = (DisplayNode) getLocation(context, location);
    String      s = n.toString();
    System.out.println(s);
    return s;
  }

  /**
   * Print the edge at the location.
   * @param location is the position returned by DaVinci when a mouse
   * button is clicked.
   */
  private String clickedEdge(String context, String location)
  {
    Object       n   = getLocation(context, location);
    StringBuffer buf = new StringBuffer("edge(");
    buf.append(location);
    if (n != null) {
      buf.append(' ');
      buf.append(n);
    }
    buf.append(')');
    System.out.println(buf.toString());
    return buf.toString();
  }

  /**
   * No action defined for a click on the background.
   */
  private String clickedBackground(String context)
  {
   return "Click on a node or an edge.";
  }

  /**
   * Terminates the visualizer process.
   */
  public void terminate()
  {
    try {
      if (visualizer != null)
        visualizer.send("menu(file(exit))");
    } catch(java.io.IOException e) {
      e.printStackTrace();
    }
    visualizer = null;
    contextMap = null;
  }

  /**
   * This opens a new visualization window.  The context string should
   * be unique from that used for any other window.
   * @param context is the user defined string to distinguish between
   * different windows
   * @param title is title for the window
   */
  public void openWindow(String context, String title, int graphAttributes)
  {
    // Start the graph visualizer in a separate process and
    // establishes communication with it.

    if (visualizer == null) {
      try {
        visualizer = new External("daVinci -pipe");
      } catch(java.io.IOException e) {
        try {
          visualizer = new External("uDrawGraph -pipe");
        } catch(java.io.IOException ex) {
        }
      }

      if (visualizer == null) {
        Msg.reportError(Msg.MSG_Unable_to_open_window_s, null, 0, 0, title);
        return;
      }

      if (!checkOk())               // Check for initial "ok".
        throw new scale.common.RuntimeException("Error initializing visualization.");

      try {
        visualizer.send("nothing");   // Double check socket with null command.
        if (!checkOk())
          throw new scale.common.RuntimeException("Error initializing visualization.");
      } catch(java.lang.Exception e) {
        Msg.reportError(Msg.MSG_s, null, 0, 0, e.getMessage());
        if (visualizer != null)
          contextMap = null;
          visualizer = null;
          return;
      }
    }

    // Specify a new context (i.e., a new window).

    StringBuffer buf = new StringBuffer("multi(open_context(\"");
    buf.append(context);
    buf.append("\"))");

    try {
      visualizer.send(buf.toString());
      processResponse(context);
    } catch(java.io.IOException e) {
      Msg.reportError(Msg.MSG_Unable_to_open_window_s, null, 0, 0, context);
      System.err.println(e.getMessage());
      return;
    }

    // Title the new window.

    buf.setLength(0);
    buf.append("window(title(\"");
    buf.append((title == null) ? context : title);
    buf.append("\"))");
    sendCommand(context, buf.toString());
    processResponse(context);

    // Send the commands to draw the graph.

    StringBuffer command = new StringBuffer("graph(update(");
    addGraphCommands(command);
    command.append("))");

    sendCommand(context, command.toString());
    if (processResponse(context))
      System.out.println(command.toString());
  }

  /**
   * This closes an existing new visualization window.  The context
   * string should be unique from that used for any other window.
   * @param context is the user defined string to distinguish between
   * different windows
   */
  public void closeWindow(String context)
  {
    sendCommand(context, "exit");
  }

  /**
   * High level send command that handles sending a command.
   * <p>
   * To operate in a multiple graph environment, we have to set the
   * current context before sending.
   * @param context selects window to which command is sent
   * @param command command to be sent
   */
  private void sendCommand(String context, String command)
  {
    StringBuffer buf = new StringBuffer("multi(set_context(\"");
    buf.append(context);
    buf.append("\"))");

    try {
      visualizer.send(buf.toString());
      processResponse(context);
      if (visualizer != null)
        visualizer.send(command);
    } catch(java.io.IOException e) {
      Msg.reportError(Msg.MSG_Unable_to_send_to_s, null, 0, 0, context);
      System.err.println(e.getMessage());
    }
  }
  
  /**
   * Checks input from visualizer to see if it has returned an "ok".
   * <p>
   * To avoid problems if user manually terminates the
   * visualization tool, this method returns true if the input is null.
   * Null input occurs when the pipe is broken.
   * @return true if ok response
   */
  protected boolean checkOk()
  {
    if (visualizer == null)
      return true;

    String res = visualizer.read();
    if (res == null)
      return true;  // Assume daVinci shut down pipe.

    if (res.equals("ok"))
      return true;

    Msg.reportError(Msg.MSG_s, null, 0, 0, res);
    return false;
  }

  /**
   * Reads line from input which is expected to be a context indicator
   * and parses it to extract the context.
   * @return the context
   */
  protected String getContext()
  {
    if (visualizer == null)
      return null;

    String input = visualizer.read();
    if (input == null)
      return null;

    StringTokenizer tok = new StringTokenizer(input, "()\"");
    if (!tok.hasMoreTokens())
      return null;

    if (!tok.nextToken().equals("context"))
      return null;

    if (!tok.hasMoreTokens())
      return null;

    return tok.nextToken();
  }

  /**
   * High level method which handles reading input until an
   * acknowledgement is received from the expected window.
   * <p>
   * This method currently doesn't handle errors well.  It simply
   * returns whether or not it encounters an error.
   * <p>
   * This method may have to be made public once we support mouse input.  
   * @param context indicates from which window input is expected
   * @return true if the response was not recognized
   */
  protected boolean processResponse(String context)
  {
    String result = getContext();
    if (result == null)
      return false; // Error, so give up.

    String input = visualizer.read();  // Get the next line.
    if (input == null)
      return false; // Error, give up.

    StringTokenizer tok      = new StringTokenizer(input, "(),\"[]");
    String          response = tok.nextToken();

    if (response.equals("ok")) {
      if (context.equals(result))
        return false;

      // Did not get the "ok" from the right context.

      Msg.reportError(Msg.MSG_Unrecognized_visulaizer_response_s, null, 0, 0, input);
      return true;
    }

    if (response.equals("quit")) { // daVinci has ended!
      contextMap = null;
      visualizer = null;
      return false;
    }

    if (response.equals("close"))
      return false;

    Msg.reportError(Msg.MSG_Unrecognized_visulaizer_response_s, null, 0, 0, input);
    return true;
  }

  /**
   * Show a message in the window.  
   * @param context indicates to which window the graph is to be drawn
   * @param message is the message to display
   */
  protected void showMessage(String context, String message)
  {
    StringBuffer buf = new StringBuffer("window(show_message(\"");
    addDisplayString(message, buf);

    if (buf.length() > 150) {
      buf.setLength(147);
      buf.append("...");
    }

    buf.append("\"))");

    sendCommand(context, buf.toString());
    if (processResponse(context))
      System.out.println(message);
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
    while (true) {
      String msg     = "";
      String context = getContext();

      if (context == null)
        return; // Error, so give up.

      String input = visualizer.read();  // Get the next line.
      if (input == null)
        return; // Error, give up.

      StringTokenizer tok      = new StringTokenizer(input, "(),\"[]");
      String          response = tok.nextToken();

      if (response.equals("node_selections_labels")) {
        if (tok.hasMoreTokens()) {
          // daVinci can return a list of tokens.
          while (tok.hasMoreTokens())
            showMessage(context, clickedNode(context, tok.nextToken()));
          continue;
        }

        showMessage(context, clickedBackground(context));
        continue;
      }

      if (response.equals("edge_selection_label")) {
        if (tok.hasMoreTokens()) {
          showMessage(context, clickedEdge(context, tok.nextToken()));
          continue;
        }

        showMessage(context, clickedBackground(context));
        continue;
      }

      if (response.equals("quit")) {
        contextMap = null;
        visualizer = null;
        return;
      }

      showMessage(context, "Unrecognized response from visualizer: " + input);
      continue;
    }
  }
}
