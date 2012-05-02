package scale.visual;

import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.event.*;

import scale.common.*;

/**
 * This class implements graph displays for Scale using Java2D and
 * Java Swing.
 * <p>
 * $Id: SGD.java,v 1.24 2007-10-04 19:58:40 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class SGD extends DisplayGraph
{
  private static final String aboutText =
    "This mess was thrown together by James H. Burrill.\n" +
    "Computer Science Department\n" + 
    "University of Massachusetts\n" +
    "Amherst, MA 01003";

  private static final String laynoneText =
    "No layout algorithmn has been developed.\n" +
    "Perhaps you would like to write one.";

  private static int startx = 70;
  private static int starty = 70;

  /**
   * This class represents a node in a directed graph.
   */
  private static final class SGDNode
  {
    private Object info;   // Display info.
    private String id;     // Identifier.
    private String text;   // Text displayed to represent the node.
    private int    x;      // X position
    private int    y;      // Y Position
    private int    label;  // For use by layout algorithms.
    private short  width;
    private short  height;
    private DColor color;  // Index into the color map.
    private DShape shape;  // Shape to use to display this node.

    public SGDNode(DShape shape, String id, String text, DColor color, Object info)
    {
      this.shape = shape;
      this.id    = id;
      this.text  = text;
      this.color = color;
      this.info  = info;

      initColors();
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(Node ");
      buf.append(text);
      if (info != null) {
        buf.append(' ');
        buf.append(info);
      }
      buf.append(')');
      return buf.toString();
    }

    /**
     * Calculate the size of the rectangle required based on the space
     * required for the text to be displayed.
     */
    public void calc(Graphics2D g2, Font font)
    {
      FontRenderContext frc = g2.getFontRenderContext();
      Rectangle2D       fr  = font.getStringBounds(text, frc);

      double w    = fr.getWidth();
      double h    = fr.getHeight();
      double addx = 5.0;
      double addy = 10.0;

      switch (shape) {
      case BOX:     addx = 10.0;              break;
      case RHOMBUS: addx = 30.0; addy = 30.0; break;
      case CIRCLE:  addx = 10.0; addy = 10.0; h = w; break;
      case ELLIPSE: addx = 20.0; addy = 20.0; break;
      default:      addx =  5.0;              break;
      }

      w += addx;
      h += addy;

      if (h < 20.0)
        h = 20.0;
      if (w < 40.0)
        w = 40.0;

      height = (short) h;
      width  = (short) w;
    }

    /**
     * Position the node so that it's center is at the specified location.
     */
    public void move(int x, int y)
    {
      this.x = x - width / 2;
      if (x < 0)
        x = 0;
      this.y = y - height / 2;
      if (y < 0)
        y = 0;
    }

    public DShape getShape()
    {
      return shape;
    }

    public String getID()
    {
      return id;
    }

    public Object getInfo()
    {
      return info;
    }

    public String getText()
    {
      return text;
    }

    public DColor getColor()
    {
      return color;
    }

    public int getLabel()
    {
      return label;
    }

    public void setLabel(int label)
    {
      this.label = label;
    }

    public int getX()
    {
      return x;
    }

    public void setX(int x)
    {
      this.x = x;
    }

    public int getY()
    {
      return y;
    }

    public void setY(int y)
    {
      this.y = y;
    }

    public int getWidth()
    {
      return width;
    }

    public int getHeight()
    {
      return height;
    }

    public double getCenterX()
    {
      return x + ((double) width) / 2.0;
    }

    public double getCenterY()
    {
      return y + ((double) height) / 2.0;
    }

    public double getInX()
    {
      return x + ((double) width) / 2.0;
    }

    public double getInY()
    {
      return y + height / 4;
    }

    public double getOutX()
    {
      return x + ((double) width) / 2.0;
    }

    public double getOutY()
    {
      return y + (3 * height) / 4;
    }
  }

  /**
   * This class represents an edge in a directed graph.
   * The edge can have three line segments:
   * <ol>
   * <li> head         => head+offset1
   * <li> head+offset1 => tail+offset2
   * <li> tail+offset2 => tail
   * </ol>
   * Because this class implements the <code>Shape</code> interface it
   * can be passed directly to the <code>draw()</code> method of the
   * <code>Graphics</code>class.  An instance of this class is its own
   * path iterator instance.
   */
  private static final class SGDEdge implements Shape, PathIterator
  {
    private SGDFrame frame;
    private SGDNode  head;     // Edge head.
    private SGDNode  tail;     // Edge tail.
    private DEdge    info;     // Display info.
    private DColor   color;    // Index into the color map.
    private byte     state;    // For the PathIterator
    private short    x1offset; // Define a bend point in the line.
    private short    y1offset; // head=>head+offset1=>tail+offset2=>tail
    private short    x2offset; // Define a bend point in the line.
    private short    y2offset;

    private AffineTransform at;

    public SGDEdge(SGDFrame frame, SGDNode head, SGDNode tail, DEdge info, DColor color)
    {
      this.frame = frame;
      this.head  = head;
      this.tail  = tail;
      this.info  = info;
      this.color = color;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(Edge ");
      buf.append(head.getID());
      buf.append("=>");
      buf.append(tail.getID());
      if (info != null) {
        buf.append(' ');
        buf.append(info);
      }
      buf.append(')');
      return buf.toString();
    }

    /**
     * Return the node that is the head of the edge.
     */
    public SGDNode getHead()
    {
      return head;
    }

    /**
     * Return the node that is the tail of the edge.
     */
    public SGDNode getTail()
    {
      return tail;
    }

    public double getX1()
    {
      return head.getCenterX();
    }

    public double getY1()
    {
      return head.getCenterY();
    }

    public double getX2()
    {
      return tail.getCenterX();
    }

    public double getY2()
    {
      return tail.getCenterY();
    }

    public Object getInfo()
    {
      return info;
    }

    public DColor getColor()
    {
      return color;
    }

    public double getCenterX()
    {
      return (head.getCenterX() + tail.getCenterX()) / 2;
    }

    public double getCenterY()
    {
      return (head.getCenterY() + tail.getCenterY()) / 2;
    }

    /**
     * Specify bend points in the line.
     */
    public void setOffsets()
    {
      // Adjust the lines so that they are not on top of one another.
      // This is the poor man's line layout.  Or, maybe it's just the
      // lazy man's line layout.

      x1offset = 0;
      x2offset = 0;
      y1offset = 0;
      y2offset = 0;

      int    cnt  = 0;
      double hx   = head.getOutX();
      double hy   = head.getOutY();
      double tx   = tail.getInX();
      double ty   = tail.getInY();
      double midx = hx + (hx - tx) / 2.0;
      double midy = hy + (hy - ty) / 2.0;

      SGDNode[] nodes = frame.getNodes();
      for (int i = 0; i < nodes.length; i++) {
        SGDNode node = nodes[i];
        if ((node == null) || (node == head) || (node == tail))
          continue;

        double  x1 = node.getX();
        double  y1 = node.getY();
        double  w  = node.getWidth();
        double  h  = node.getHeight();

        if (Line2D.linesIntersect(hx, hy, tx, ty, x1, y1, x1 + w, y1)) {
          cnt++;
          continue;
        }
        if (Line2D.linesIntersect(hx, hy, tx, ty, x1, y1, x1, y1 + h)) {
          cnt++;
          continue;
        }
        if (Line2D.linesIntersect(hx, hy, tx, ty, x1 + w, y1, x1 + w, y1 + h)) {
          cnt++;
          continue;
        }
        if (Line2D.linesIntersect(hx, hy, tx, ty, x1, y1 + h, x1 + w, y1 + h)) {
          cnt++;
          continue;
        }
      }

      if (cnt == 0)
        return;

      int mx1 = -1;
      int my1 = 1;
      int mx2 = -1;
      int my2 = 1;
      if (Math.abs(hx - tx) > 1.0) {
        mx1 = (hx <= tx) ? 1 : -1;
        mx2 = -mx1;
      }
      if (Math.abs(hy - ty) > 1.0) {
        my1 = (hy <= ty) ? 1 : -1;
        my2 = -my1;
      }

      x1offset = (short) (mx1 * ((head.getWidth()  / 2) * cnt));
      x2offset = (short) (mx2 * ((tail.getWidth()  / 2) * cnt));
      y1offset = (short) (my1 * ((head.getHeight() / 2) * cnt));
      y2offset = (short) (my2 * ((tail.getHeight() / 2) * cnt));
      if (((hx < tx) && ((hx + x1offset) > (tx + x2offset))) ||
          ((hx > tx) && ((hx + x1offset) < (tx + x2offset)))) {
        x1offset = 0;
        x2offset = 0;
      }
      if (((hy < ty) && ((hy + y1offset) > (ty + y2offset)))  ||
          ((hy > ty) && ((hy + y1offset) < (ty + y2offset)))) {
        y1offset = 0;
        y2offset = 0;
      }
    }

    private boolean intersects(double x1,
                               double y1,
                               double x2,
                               double y2,
                               double x,
                               double y,
                               double w,
                               double h)
    {
      return
        Line2D.linesIntersect(x1, y1, x2, y2, x    , y    , x + w, y    ) ||
        Line2D.linesIntersect(x1, y1, x2, y2, x    , y    , x    , y + h) ||
        Line2D.linesIntersect(x1, y1, x2, y2, x + w, y    , x + w, y + h) ||
        Line2D.linesIntersect(x1, y1, x2, y2, x    , y + h, x + w, y + h);
    }

    public boolean intersects(double x, double y, double w, double h)
    {
      double x1 = head.getOutX();
      double y1 = head.getOutY();
      double x2 = tail.getInX();
      double y2 = tail.getInY();
      return
        intersects(x1 + x1offset, y1 + y1offset, x2 + x2offset, y2 + y2offset, x, y, w, h) ||
        intersects(x1           , y1           , x1 + x1offset, y1 + y1offset, x, y, w, h) ||
        intersects(x2 + x2offset, y2 + y2offset, x2           , y2           , x, y, w, h);
    }

    public boolean intersects(double x1, double y1, double x2, double y2, Rectangle2D r)
    {
      return r.intersectsLine(x1, y1, x2, y2);
    }

    public boolean intersects(Rectangle2D r)
    {
      double x1 = head.getOutX();
      double y1 = head.getOutY();
      double x2 = tail.getInX();
      double y2 = tail.getInY();
      return
        intersects(x1 + x1offset, y1 + y1offset, x2 + x2offset, y2 + y2offset, r) ||
        intersects(x1           , y1           , x1 + x1offset, y1 + y1offset, r) ||
        intersects(x2 + x2offset, y2 + y2offset, x2           , y2           , r);
    }

    public boolean contains(double x, double y, double w, double h)
    {
      return contains(x, y) && (w <= 1.0) && (y <= 1.0);
    }

    public boolean contains(Rectangle2D r)
    {
      return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    private boolean contains(double x1,
                             double y1,
                             double x2,
                             double y2,
                             double x,
                             double y)
    {
      if (x2 == x1)
        return (x == x1) && (y == y1);

      double d  =(y - (x - x1) * (y2 - y1) / (x2 - x1));
      return ((d < .0005) && (d > -0.0005));
    }

    public boolean contains(double x, double y)
    {
      double x1 = head.getCenterX();
      double y1 = head.getCenterY();
      double x2 = tail.getCenterX();
      double y2 = tail.getCenterY();
      return
        (contains(x1 + x1offset, y1 + y1offset, x2 + x2offset, y2 + y2offset, x, y) ||
         contains(x1           , y1           , x1 + x1offset, y1 + y1offset, x, y) ||
         contains(x2 + x2offset, y2 + y2offset, x2           , y2           , x, y));
    }

    public boolean contains(Point2D p)
    {
      return contains(p.getX(), p.getY());
    }

    public Rectangle getBounds()
    {
      double x1 = head.getX();
      double y1 = head.getY();
      double x2 = tail.getX();
      double y2 = tail.getY();
      if (x1 > x2) {
        double t = x1;
        x1 = x2;
        x2 = t;
      }
      if (y1 > y2) {
        double t = y1;
        y1 = y2;
        y2 = t;
      }
      return new Rectangle((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
    }

    public Rectangle2D getBounds2D()
    {
      double x1 = head.getX();
      double y1 = head.getY();
      double x2 = tail.getX();
      double y2 = tail.getY();
      if (x1 > x2) {
        double t = x1;
        x1 = x2;
        x2 = t;
      }
      if (y1 > y2) {
        double t = y1;
        y1 = y2;
        y2 = t;
      }
      return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
    }

    public PathIterator getPathIterator(AffineTransform at)
    {
      this.at = at;
      this.state = 0;
      return this;
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness)
    {
      this.at = at;
      this.state = 0;
      return this;
    }

    public void next()
    {
      state++;
      if (state > 4)
        state = 4;
    }

    public boolean isDone()
    {
      return state >= 4;
    }

    public int getWindingRule()
    {
      return WIND_EVEN_ODD;
    }

    @SuppressWarnings("fallthrough")
    public int currentSegment(double[] coords)
    {
      switch (state) {
      case 0: {
        setOffsets();

        double x1 = head.getOutX();
        double y1 = head.getOutY();
        coords[0] = x1;
        coords[1] = y1;
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_MOVETO;
      }
      case 1:
        if ((x1offset != 0) || (y1offset != 0)) {
          double x1 = head.getOutX();
          double y1 = head.getOutY();
          coords[0] = x1 + x1offset;
          coords[1] = y1 + y1offset;
          if (at != null)
            at.transform(coords, 0, coords, 0, 2);
          return SEG_LINETO;
        }
        state = 2;
       // fall through
      case 2:
        if ((x2offset != 0) || (y2offset != 0)) {
          double x2 = tail.getInX();
          double y2 = tail.getInY();
          coords[0] = x2 + x2offset;
          coords[1] = y2 + y2offset;
          if (at != null)
            at.transform(coords, 0, coords, 0, 2);
          return SEG_LINETO;
        }
        state = 3;
        // fall through
      case 3: {
        double x2 = tail.getInX();
        double y2 = tail.getInY();
        coords[0] = x2;
        coords[1] = y2;
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_LINETO;
      }
      }
      return SEG_LINETO;
    }

    @SuppressWarnings("fallthrough")
    public int currentSegment(float[] coords)
    {
      switch (state) {
      case 0: {
        setOffsets();

        double x1 = head.getOutX();
        double y1 = head.getOutY();
        coords[0] = (float) x1;
        coords[1] = (float) y1;
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_MOVETO;
      }
      case 1:
        if ((x1offset != 0) || (y1offset != 0)) {
          double x1 = head.getOutX();
          double y1 = head.getOutY();
          coords[0] = (float) x1 + x1offset;
          coords[1] = (float) y1 + y1offset;
          if (at != null)
            at.transform(coords, 0, coords, 0, 2);
          return SEG_LINETO;
        }
        state = 2;
        // fall through
      case 2:
        if ((x2offset != 0) || (y2offset != 0)) {
          double x2 = tail.getInX();
          double y2 = tail.getInY();
          coords[0] = (float) x2 + x2offset;
          coords[1] = (float) y2 + y2offset;
          if (at != null)
            at.transform(coords, 0, coords, 0, 2);
          return SEG_LINETO;
        }
        state = 3;
        // fall through
      case 3: {
        double x2 = tail.getInX();
        double y2 = tail.getInY();
        coords[0] = (float) x2;
        coords[1] = (float) y2;
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_LINETO;
      }
      }
      return SEG_LINETO;
    }
  }

  /**
   * This class provides the canvas upon which the directed graph is drawn.
   */
  private static final class DrawingArea extends JPanel implements Printable
  {
    private static final long serialVersionUID = 42L;

    private RoundRectangle2D rt   = new RoundRectangle2D.Float();
    private Ellipse2D        el   = new Ellipse2D.Float();
    private SGDRhombus       rb   = new SGDRhombus.Float();
    private SGDNode[]   nodes;       // The set of graph nodes.
    private int         numNodes;
    private SGDEdge[]   edges;       // The set of graph edges.
    private int         numEdges;
    private double      ratio;       // Compress/expand ratio.
    private Font        font;        // The font in use.
    private SGDNode     currentNode; // The current, selected graph node.
    private SGDEdge     currentEdge; // The current, selected graph edge.
    private BasicStroke bigStroke;   // Stroke used to highlight nodes.
    private BasicStroke regStroke;   // Stroke used to draw un-highlighted nodes.
    private boolean     fast;        // If true, use fast methods.

    public DrawingArea(SGDNode[] nodes, int numNodes, SGDEdge[] edges, int numEdges)
    {
      super();

      this.bigStroke = new BasicStroke(4.0f,
                                       BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND);
      this.regStroke = new BasicStroke(1.0f,
                                       BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND);
      this.ratio     = 1.0;
      this.nodes     = nodes;
      this.edges     = edges;
      this.numNodes  = numNodes;
      this.numEdges  = numEdges;
      if (numNodes > 0)
        this.currentNode = nodes[0];
    }

    public void calc()
    {
      Graphics2D g = (Graphics2D) getGraphics();

      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        node.calc(g, font);
      }        
    }

    public void setFast(boolean fast)
    {
      this.fast = fast;
    }

    public void setFont(Font font)
    {
      this.font = font;
    }

    public Font getFont()
    {
      return font;
    }

    public void setRatio(double ratio)
    {
      this.ratio = ratio;
    }

    public double getRatio()
    {
      return ratio;
    }

    public void setCurrentNode(SGDNode node)
    {
      currentNode = node;
    }

    public SGDNode getCurrentNode()
    {
      return currentNode;
    }

    public void setCurrentEdge(SGDEdge edge)
    {
      currentEdge = edge;
    }

    public SGDEdge getCurrentEdge()
    {
      return currentEdge;
    }

    /**
     * Draw the graph.
     */
    public void paintComponent(Graphics g)
    {
      Dimension d = new Dimension(0, 0);
      paintComponent(g, 0.0, 0.0, d);
      setPreferredSize(d); // We may have a new size now.
      revalidate();
    }

    public synchronized void paintComponent(Graphics  g,
                                            double    cx,
                                            double    cy,
                                            Dimension d)
    {
      int maxx = 0;
      int maxy = 0;
      int minx = Integer.MAX_VALUE;
      int miny = Integer.MAX_VALUE;
      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        int     dx   = node.getX();
        int     dy   = node.getY();
        int     w    = node.getWidth();
        int     h    = node.getHeight();

        if (dx + w > maxx)
          maxx = dx + w;
        if (dy + h > maxy)
          maxy = dy + h;

        if (dx < minx)
          minx = dx;
        if (dy < miny)
          miny = dy;
      }

      if ((minx != 0) || (miny != 0)) {
        for (int i = 0; i < numNodes; i++) {
          SGDNode node = nodes[i];
          node.setX(node.getX() - minx + 5);
          node.setY(node.getY() - miny + 5);
        }
        maxx -= minx;
        maxy -= miny;
      }

      if (d != null) {
        d.width = 5 + (int) (maxx * ratio);
        d.height = 5 + (int) (maxy * ratio);
      }

      super.paintComponent(g);

      Graphics2D g2 = (Graphics2D) g;

      g2.translate(cx, cy);
      g2.scale(ratio, ratio);

      if (!fast)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

      // Draw the line first so that they are underneath.

      for (int i = 0; i < numEdges; i++) {
        SGDEdge line = edges[i];
        
        g2.setStroke((line == currentEdge) ? bigStroke : regStroke);
        g2.setPaint(line.getColor().color());
        g2.draw(line);
      }

      // Draw the graph nodes.

      for (int i = 0; i < numNodes; i++) {
        SGDNode          node = nodes[i];
        RectangularShape s    = rt;
        int              dx   = node.getX();
        int              dy   = node.getY();
        int              w    = node.getWidth();
        int              h    = node.getHeight();

        int tx = dx + 5;
        int ty = dy + h / 2 + 5;
        switch(node.getShape()) {
        case BOX:
          rt.setRoundRect(dx, dy, w, h, 0.3 * h, 0.3 * h);
          break;
        case RHOMBUS:
          s = rb;
          tx = dx + 15;
          ty = dy + h / 2 + 5;
          rb.setFrame(dx, dy, w, h);
          break;
        case CIRCLE:
          s = el;
          tx = dx + 5;
          ty = dy + h / 2 + 5;
          el.setFrame(dx, dy, w, h);
          break;
        case ELLIPSE:
          s = el;
          tx = dx + 10;
          ty = dy + h / 2 + 5;
          el.setFrame(dx, dy, w, h);
          break;
        }

        g2.setPaint(node.getColor().color());
        g2.fill(s);
        g2.setStroke((node == currentNode) ? bigStroke : regStroke);
        g2.setPaint(Color.black);
        g2.draw(s);
        g2.drawString(node.getText(), tx, ty);
      }
    }

    /**
     * Print the graph on the specified <code>Graphics</code>.
     */
    public int print(Graphics g, PageFormat pf, int page)
    {
      if (page != 0)
        return NO_SUCH_PAGE;
      boolean buffered = isDoubleBuffered();
      setDoubleBuffered(false);
      paintComponent(g, pf.getImageableX(), pf.getImageableY(), null);
      setDoubleBuffered(buffered);
      return PAGE_EXISTS;
    }

    /**
     * Return the node containing the specified point or return
     * <code>null</code> if none.  Note, the node contains the area of
     * the containing rectangle.
     */
    public SGDNode findNode(int x, int y)
    {
      x = (int) (x / ratio);
      y = (int) (y / ratio);

      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        int     nx   = node.getX();
        int     ny   = node.getY();
        int     nxx  = nx + node.getWidth();
        int     nyy  = ny + node.getHeight();
        if ((x >= nx && x < nxx) && (y >= ny && y < nyy))
         return node;
      }
      return null;
    }

    /**
     * Return the edge that passes through a small rectangle at the
     * specified point or return <code>null</code> if none.
     */
    public SGDEdge findLine(int x, int y)
    {
      x = (int) (x / ratio);
      y = (int) (y / ratio);
      Rectangle2D r2d = new Rectangle2D.Float.Float();
      r2d.setFrame(x - 3, y - 3, 6.0, 6.0);
      for (int i = 0; i < numEdges; i++) {
        SGDEdge line = edges[i];
        if (line.intersects(r2d))
          return line;
      }
      return null;
    }

    /**
     * Move the specified node to the specified position.
     */
    public void move(SGDNode node, int x, int y)
    {
      node.move((int) (x / ratio), (int) (y / ratio));
    }

    /**
     * Place the specified node in the center of the display.
     */
    public synchronized void centerViewOnNode(JViewport view, SGDNode node)
    {
      if (node == null)
        return;

      setCurrentNode(node);
      revalidate();
      repaint();

      Dimension extents  = view.getExtentSize();
      Point     position = view.getViewPosition();
      double    w        = extents.getWidth();
      double    h        = extents.getHeight();
      double    x        = node.getX() * ratio;
      double    y        = node.getY() * ratio;
      double    px       = position.getX();
      double    py       = position.getY();

      // If the node is in the view, just exit.

      if ((x >= px) && ((x + node.getWidth() * ratio) < (px + w)) &&
          (y >= py) && ((y + node.getHeight() * ratio) < (py + h)))
        return;

      // Calculate new view that centers the node.

      x = node.getCenterX() * ratio - w / 2.0;
      if (x < 0)
        x = 0;

      y = node.getCenterY() * ratio - h / 2.0;
      if (y < 0)
        y = 0;

      position.move((int) (x + 0.5), (int) (y + 0.5));
      view.repaint(0, (int) x, (int) y, (int) w, (int) h);
      view.setViewPosition(position);
    }

    public void findHead(JViewport jp)
    {
      if (currentEdge == null)
        return;
      centerViewOnNode(jp, currentEdge.getHead());
    }

    public void findTail(JViewport jp)
    {
      if (currentEdge == null)
        return;
      centerViewOnNode(jp, currentEdge.getTail());
    }
  }

  /**
   * This class provides the dialog for entering text for the
   * searching facility.
   */
  private static final class FindDialog extends JDialog implements ActionListener
  {
    private static final long serialVersionUID = 42L;

    private SGDFrame   frame;
    private JTextField text;
    private JCheckBox  caseb;

    public FindDialog(SGDFrame frame)
    {
      super(frame, "Find Text", false);
      this.frame = frame;
      Graphics2D g = (Graphics2D) frame.getGraphics();

      JButton find   = new JButton("find");
      JButton cancel = new JButton("cancel");
      JLabel  lab    = new JLabel("Find: ");

      caseb  = new JCheckBox("case sensitive", false);
      text   = new JTextField(20);
      text.setSize(new Dimension(100, 50));
      text.setActionCommand("text");

      JPanel tp = new JPanel();
      tp.setLayout(new BoxLayout(tp, BoxLayout.X_AXIS));
      tp.add(lab);
      tp.add(text);

      FontRenderContext frc = g.getFontRenderContext();
      Rectangle2D       fr  = frame.getFont().getStringBounds("aaaaaaaaaabbbbbbbbbbcccccccccc", frc);
      int               w   = ((int) fr.getWidth()) + 10;
      int               h   = ((int) fr.getHeight()) + 10;
      Dimension         dim = new Dimension(w, h);

      tp.setSize(dim);
      tp.setMaximumSize(dim);
      tp.setMinimumSize(dim);
      tp.setPreferredSize(dim);

      JPanel tb = new JPanel();
      tb.setLayout(new BoxLayout(tb, BoxLayout.X_AXIS));
      tb.add(caseb);
      tb.add(find);
      tb.add(cancel);

      JPanel fdp = new JPanel();
      fdp.setLayout(new BoxLayout(fdp, BoxLayout.Y_AXIS));
      fdp.add(tp);
      fdp.add(tb);

      find.addActionListener(this);
      cancel.addActionListener(this);
      text.addActionListener(this);
      setLocationRelativeTo(frame);
      getContentPane().add(fdp);
      setSize(w + 50, 2 * h + 30);
    }

    /**
     * Return the current text displayed.
     */
    public String getText()
    {
      return text.getText();
    }

    /**
     * Return true if the search is case sensitive.
     */
    public boolean isCaseSensitive()
    {
      return caseb.isSelected();
    }

    public void actionPerformed(ActionEvent action)
    {
      String as = action.getActionCommand();
      if ("find".equals(as) || "text".equals(as)) {
        SGDNode node = frame.findNode(text.getText(), caseb.isSelected());
        if (node != null) {
          System.out.println("Found " + node);
          frame.centerNode(node);
        }
        return;
      }
      if ("cancel".equals(as)) {
        setVisible(false);
        return;
      }
    }
  }

  /**
   * This class creates the user interface.
   */
  private final class SGDFrame extends JFrame implements MouseListener,
                                                         ChangeListener,
                                                         ActionListener
  {
    private static final long serialVersionUID = 42L;

    private HashMap<String, Object> map;         // Map from name to node or edge
    private SGDNode[]    nodes;       // The set of graph nodes.
    private int          numNodes;
    private SGDEdge[]    edges;       // The set of graph edges.
    private int          numEdges;
    private boolean      top;         // Places the root node at the top of the display if true.
    private BasicStroke  basicStroke;
    private DrawingArea  canvas;
    private JSlider      slider;
    private FindDialog   findDialog;
    private DisplayGraph dg;
    private String       context;
    private int          searchPosition;
    private JScrollPane  jp;
    private SGDNode      saveNode = null;
    private SGDLayout    layout;

    private JCheckBoxMenuItem fastItem;
    private JCheckBoxMenuItem horizontalItem;
    private JCheckBoxMenuItem verticalItem;

    private PrinterJob pj;
    private PageFormat pf;

    public SGDFrame(DisplayGraph dg, String context, String title, boolean top)
    {
      super(title);
      this.dg             = dg;
      this.context        = context;
      this.top            = top;
      this.map            = new HashMap<String, Object>(203);
      this.nodes          = new SGDNode[256];
      this.numNodes       = 0;
      this.edges          = new SGDEdge[256];
      this.numEdges       = 0;
      this.basicStroke    = new BasicStroke();
      this.searchPosition = 0;
    }

    public SGDEdge[] getEdges()
    {
      return edges;
    }

    public SGDNode[] getNodes()
    {
      return nodes;
    }

    public void mouseReleased(MouseEvent e)
    {
      boolean changed = false;
      if (SwingUtilities.isLeftMouseButton(e)) {
        int x = e.getX();
        int y = e.getY();
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        if (saveNode != null) {
          canvas.move(saveNode, x, y);
          canvas.revalidate();
          canvas.repaint();
        }
        saveNode = null;
        return;
      }

      if (SwingUtilities.isRightMouseButton(e)) {
        saveNode = null;
        int x = e.getX();
        int y = e.getY();
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        SGDNode node = canvas.findNode(x, y);
        if (node != null) {
          System.out.println(map.get(node.getID()));
          canvas.setCurrentNode(node);
          canvas.revalidate();
          canvas.repaint();
          return;
        }
        SGDEdge line = canvas.findLine(x, y);
        if (line != null) {
          System.out.println(line);
          canvas.setCurrentEdge(line);
          canvas.revalidate();
          canvas.repaint();
        }
      }
    }

    public void mouseClicked(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}

    public void mousePressed(MouseEvent e)
    {
      if (SwingUtilities.isLeftMouseButton(e)) {
        int x = e.getX();
        int y = e.getY();
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        saveNode = canvas.findNode(x, y);
      }
    }


    public void stateChanged(ChangeEvent e)
    {
      double ratio = (double) slider.getValue() / 25.0;
      canvas.setRatio(ratio);
      canvas.validate();
      canvas.repaint();
    }

    public void actionPerformed(ActionEvent action)
    {
      String as = action.getActionCommand();
      if ("Find Text".equals(as)) {
        findDialog.setVisible(true);
        return;
      }

      if ("Find Again".equals(as)) {
        SGDNode node = findNode(findDialog.getText(), findDialog.isCaseSensitive());
        if (node != null) {
          System.out.println("Again " + node);
          centerNode(node);
        }
        return;
      }

      if ("Find Head".equals(as)) {
        canvas.findHead(jp.getViewport());
        return;
      }

      if ("Find Tail".equals(as)) {
        canvas.findTail(jp.getViewport());
        return;
      }

      if ("Close".equals(as)) {
        dg.closeWindow(context);
        return;
      }

      if ("Exit".equals(as)) {
        dg.terminate();
        return;
      }

      if ("FastD".equals(as)) {
        canvas.setFast(fastItem.isSelected());
        return;
      }

      if ("About".equals(as)) {
        JOptionPane.showMessageDialog(this,
                                      aboutText,
                                      "OK",
                                      JOptionPane.INFORMATION_MESSAGE);
        return;
      }

      if ("Cascade".equals(as)) {
        if (!(layout instanceof CascadeLayout)) {
          layout = new CascadeLayout();
          layout.setGraph(nodes, numNodes, edges, numEdges);
        }
        layout.layout(horizontalItem.isSelected(), verticalItem.isSelected());
        centerNode(canvas.getCurrentNode());
        return;
      }

      if ("Layout2".equals(as) || "Layout3".equals(as)) {
        JOptionPane.showMessageDialog(this,
                                      laynoneText,
                                      "OK",
                                      JOptionPane.INFORMATION_MESSAGE);
      }

      if ("Page Setup".equals(as)) {
        setupPrinting();
        pf = pj.pageDialog(pf);
      }

      if ("Print".equals(as)) {
        print(false);
      }
    }

    private void setupPrinting()
    {
      if (pj != null)
        return;

      pj = PrinterJob.getPrinterJob();
      pf = pj.defaultPage();
    }

    private void print(boolean view)
    {
      setupPrinting();
      pj.setPrintable(canvas, pf);
      if (pj.printDialog()) {
        try {
          pj.print();
        } catch (PrinterException e) {
          JOptionPane.showMessageDialog(this,
                                        e.getMessage(),
                                        "OK",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }
    }

    /**
     * Return the node containing the specified text. or
     * <code>null</code> if none.
     */
    public SGDNode findNode(String searchText, boolean caseSensitive)
    {
      if (!caseSensitive)
        searchText = searchText.toLowerCase();

      if (numNodes <= 0)
        return null;

      int start = searchPosition;
      while (true) {
        if (searchPosition >= numNodes) {
          searchPosition = 0;
          if (start == searchPosition)
            return null;
        }

        SGDNode node     = nodes[searchPosition++];
        String  nodeText = node.getText();
        if (!caseSensitive)
          nodeText = nodeText.toLowerCase();

        if (nodeText.indexOf(searchText) >= 0)
          return node;
      
        if (start == searchPosition)
          return null;
      }
    }

    /**
     * Put the specified node in the (center of the) view.
     */
    public void centerNode(SGDNode node)
    {
      canvas.centerViewOnNode(jp.getViewport(), node);
    }

    /**
     * Make the GUI visible.
     */
    public void open()
    {
      addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            dg.closeWindow(context);
          }
        });

      slider = new JSlider(JSlider.HORIZONTAL, 0, 50, 25);
      slider.setMinorTickSpacing(2);
      slider.setMajorTickSpacing(10);
      slider.setPaintTicks(true);
      slider.setPaintLabels(false);
      slider.setVisible(true);
      slider.addChangeListener(this);

      JMenuItem closeItem = new JMenuItem("Close", 'C');
      JMenuItem pageItem  = new JMenuItem("Page Setup", 'S');
      JMenuItem printItem = new JMenuItem("Print", 'P');
      JMenuItem exitItem  = new JMenuItem("Exit", 'E');
      JMenu     fileMenu  = new JMenu("File");

      closeItem.addActionListener(this);
      closeItem.setToolTipText("Close this window.");
      exitItem.addActionListener(this);
      exitItem.setToolTipText("Close all graphics windows.");
      pageItem.addActionListener(this);
      pageItem.setToolTipText("Select print page format.");
      printItem.addActionListener(this);
      printItem.setToolTipText("Prints as much as possible of the complete graph on one sheet of paper.");
      fileMenu.add(closeItem);
      fileMenu.add(new JSeparator());
      fileMenu.add(pageItem);
      fileMenu.add(printItem);
      fileMenu.add(new JSeparator());
      fileMenu.add(exitItem);

      JMenuItem findItem  = new JMenuItem("Find Text", 'F');
      JMenuItem againItem = new JMenuItem("Find Again", 'A');
      JMenuItem headItem  = new JMenuItem("Find Head", 'H');
      JMenuItem tailItem  = new JMenuItem("Find Tail", 'T');
      JMenu     findMenu  = new JMenu("Find");

      findItem.addActionListener(this);
      findItem.setToolTipText("Find text in the display.");
      againItem.addActionListener(this);
      againItem.setToolTipText("Find the next occurrence.");
      headItem.addActionListener(this);
      headItem.setToolTipText("Find the head of the selected edge.");
      tailItem.addActionListener(this);
      tailItem.setToolTipText("Find the tail of the selected edge.");
      findMenu.add(findItem);
      findMenu.add(againItem);
      findMenu.add(headItem);
      findMenu.add(tailItem);

      JMenuItem layout1 = new JMenuItem("Cascade", '1');
      JMenuItem layout2 = new JMenuItem("Layout2", '2');
      JMenuItem layout3 = new JMenuItem("Layout3", '3');
      JMenu     layMenu = new JMenu("Layout");

      layout1.addActionListener(this);
      layout1.setToolTipText("Use the 'Cascading Sequences' layout method.");
      layout2.addActionListener(this);
      layout2.setToolTipText("Not implemented - perhaps you would like to write it.");
      layout3.addActionListener(this);
      layout3.setToolTipText("Not implemented - perhaps you would like to write it.");

      layMenu.add(layout1);
      layMenu.add(layout2);
      layMenu.add(layout3);

      fastItem = new JCheckBoxMenuItem("Fast Draw", numNodes > 100);
      fastItem.setActionCommand("FastD");
      fastItem.addActionListener(this);
      fastItem.setToolTipText("Use faster, less pretty drawing (e.g., no anti-aliasing).");

      horizontalItem = new JCheckBoxMenuItem("Horizontal Compress", false);
      horizontalItem.setToolTipText("The graph is compressed horizontally when laid out.");

      verticalItem = new JCheckBoxMenuItem("Vertical Compress", false);
      verticalItem.setToolTipText("The graph is compressed vertically when laid out.");

      JMenu optionMenu = new JMenu("Options");
      optionMenu.add(fastItem);
      optionMenu.add(horizontalItem);
      optionMenu.add(verticalItem);

      JMenuItem about    = new JMenuItem("About", 'A');
      JMenu     helpMenu = new JMenu("Help");
      helpMenu.add(about);
      about.addActionListener(this);

      JMenuBar  jmb = new JMenuBar();
      jmb.add(fileMenu);
      jmb.add(findMenu);
      jmb.add(layMenu);
      jmb.add(optionMenu);
      jmb.add(slider);
      jmb.add(helpMenu);
      setJMenuBar(jmb);

      canvas = new DrawingArea(nodes, numNodes, edges, numEdges);
      canvas.setBackground(Color.white);
      canvas.setVisible(true);
      canvas.addMouseListener(this);
      canvas.setToolTipText("left mouse: click and move - right mouse: select & display");

      jp = new JScrollPane(canvas); 
      jp.getHorizontalScrollBar().setUnitIncrement(10); 
      jp.getVerticalScrollBar().setUnitIncrement(10); 
      jp.setPreferredSize(new Dimension(500, 500));

      setContentPane(jp);

      pack();

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension frameSize  = getSize();
      startx += 30;
      starty += 30;
      if (startx > (screenSize.width - frameSize.width))
        startx = 70;
      if (starty > (screenSize.height - frameSize.height))
        starty = 70;
      setLocation(startx, starty);

      findDialog = new FindDialog(this);

      canvas.calc();

      layout = new CascadeLayout();
      layout.setGraph(nodes, numNodes, edges, numEdges);
      layout.layout(false, false);

      setVisible(true);

      for (int i = 0; i < 40; i++)
        Thread.yield();

      canvas.centerViewOnNode(jp.getViewport(), nodes[0]);
    }

    /**
     * Add a node to the graph.
     * This method should be used only for nodes that are not connected
     * with an edge.
     * @param n is the node
     */
    public SGDNode addNode(DisplayNode node)
    {
      String   nodeID = node.getDisplayName();
      SGDNode  s      = (SGDNode) map.get(nodeID);
      if (s != null)
        return s;

      DShape shape  = node.getDisplayShapeHint();
      DColor color  = node.getDisplayColorHint();
      String text   = node.getDisplayLabel();

      StringBuffer buf = new StringBuffer(text);
      buf.append(" (");
      buf.append(nodeID);
      buf.append(')');

      s = new SGDNode(shape, nodeID, buf.toString(), color, node);

      if (numNodes >= nodes.length) {
        SGDNode[] nn = new SGDNode[numNodes * 2];
        System.arraycopy(nodes, 0, nn, 0, numNodes);
        nodes = nn;
      }
      nodes[numNodes++] = s;
      map.put(nodeID, s);
      return s;
    }

    private String formatEdgeId(String n1, String n2)
    {
      StringBuffer s = new StringBuffer(n1);
      s.append("->");
      s.append(n2);
      s.append(" <");
      s.append(String.valueOf(numEdges)); // make them unique in case of duplicates
      s.append('>');
      return s.toString();
    }

    /**
     * Add an edge to the graph from node n1 to node n2.
     * @param n1 is the first node
     * @param n2 is the second node
     * @param color specifies the edge color
     * @param edgeAttributes specifies the edge type.
     * @param edgeInfo is additional information about the edge
     */
    public SGDEdge addEdge(DisplayNode n1,
                           DisplayNode n2,
                           DColor      color,
                           DEdge       edgeAttributes,
                           Object      edgeInfo)
    {
      if ((n1 == null) || (n2 == null))
        return null; // No edge if one end doesn't exist.

      if (top) {
        DisplayNode t = n1;
        n1 = n2;
        n2 = t;
      }

      String  n1n  = n1.getDisplayName();
      String  n2n  = n2.getDisplayName();
      String  en   = formatEdgeId(n1n, n2n);
      SGDNode head = addNode(n1);
      SGDNode tail = addNode(n2);

      SGDEdge s = new SGDEdge(this,
                              head,
                              tail,
                              edgeAttributes,
                              color);
      if (numEdges >= edges.length) {
        SGDEdge[] ne = new SGDEdge[numEdges * 2];
        System.arraycopy(edges, 0, ne, 0, numEdges);
        edges = ne;
      }
      edges[numEdges++] = s;
      map.put(en, s);
      return s;
    }

    /**
     * Close the window.
     */
    public void dispose()
    {
      pj = null;
      super.dispose();
    }
  }

  /**
   * This is the abstract class for all graph layout algorithms.
   */
  private static abstract class SGDLayout
  {
    protected SGDNode[] nodes;       // The set of graph nodes.
    protected int       numNodes;
    protected SGDEdge[] edges;       // The set of graph edges.
    protected int       numEdges;

    public SGDLayout()
    {
    }

    /**
     * Provide the graph information.
     */
    public void setGraph(SGDNode[] nodes, int numNodes, SGDEdge[] edges, int numEdges)
    {
      this.nodes     = nodes;
      this.edges     = edges;
      this.numNodes  = numNodes;
      this.numEdges  = numEdges;
    }

    /**
     * Layout the graph on an infinite canvas.
     * The location of each node of the graph is determined.
     * @param hCompress true if the graph is to be compressed horizontally
     * @param vCompress true if the graph is to be compressed vertically
     */
    public abstract void layout(boolean hCompress, boolean vCompress);
  }

  /**
   * Implement the "cascade sequence" graph layout algorithm.
   * This algorithm works by creating chains of nodes.  A chain is
   * a sequence of nodes that are joined by an edge and will be
   * displayed in the same column of the graph display.  It then
   * tries to place chains, that are linked together, in columns
   * that are contiguous.
   */
  private static final class CascadeLayout extends SGDLayout
  {
    private int[] link; // The next node in the chain.
    /**
     * For each node determine the number of incoming edges it has.  Zero
     * means none, a node label means one, and -1 means multiple.
     */
    private int[] incoming;
    /**
     * For each node determine the number of outgoing edges it has.  Zero
     * means none, a node label means one, and -1 means multiple.
     */
    private int[]  outgoing;
    private int[]  heads;     // The root of each chain.
    private int[]  tails;     // The last node in the chain.
    private int[]  sorted;
    private int[]  chain;     // The chain of each node.
    private long[] value ;    // The "value" of the chain.
    private int    numChains; // The number of chains.
    private int    maxy;      // Maximum number of rows.

    /**
     * Provide the graph information.
     */
    public void setGraph(SGDNode[] nodes, int numNodes, SGDEdge[] edges, int numEdges)
    {
      super.setGraph(nodes, numNodes, edges, numEdges);

      // Label the nodes with their position in the nodes array.  We
      // will use the x position of the node to hold this label.  The
      // label 0 is reserved to indicate "end-of-sequence".

      link = new int[numNodes + 1]; // The next node in the chain.
      for (int i = 0; i < numNodes; i++) {
        nodes[i].setLabel(i + 1);
        link[i] = 0;
      }

      // For each node determine how many incoming edges it has.  Zero
      // means none, a node label means one, and -1 means multiple.
      // Do the same for outgoing edges.

      incoming = new int[numNodes + 1];
      outgoing = new int[numNodes + 1];
      for (int i = 0; i < numEdges; i++) {
        SGDEdge edge = edges[i];
        SGDNode head = edge.getHead();
        SGDNode tail = edge.getTail();
        int     labt = tail.getLabel();
        int     labh = head.getLabel();
        incoming[labt] = (incoming[labt] == 0) ? head.getLabel() : -1;
        outgoing[labh] = (outgoing[labh] == 0) ? tail.getLabel() : -1;
      }

      // For each node with just one incoming edge, attach it to one
      // of the chains if possible.  Some initial chains have been
      // started with the nodes that have no incoming edges.  Use the
      // node Y position to point to the next node on the chain.
      // After this step each existing chain ends with a node with
      // zero or more than one outgoing edges and some nodes have not
      // been placed on a chain.

      // A node is done if it is in a chain but is not the first node
      // of the chain.

      BitVect done = new BitVect(numNodes + 1);
      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        int     lab  = node.getLabel();
        int     n    = incoming[lab];
        if (n > 0) {
          assert (outgoing[n] != 0) : "How can this happen?";
          if (outgoing[n] == lab) {
            // If this node has only one incoming edge and the head of
            // that edge has only one outgoing edge, link them up.
            link[n] = lab;
            done.set(lab);
          }
        }
      }

      // Grow the chains that are rooted by adding on children that
      // have more than one incoming edge. Start with the chains that
      // will never become part of another chain.  This will display
      // loops better.

      for (int i = 1; i <= numNodes; i++) {
        if (incoming[i] != 0)
          continue;

        int k = i - 1;
        while (true) {
          SGDNode node = nodes[k];
          int     next = link[node.getLabel()];
          while (next != 0) {
            node = nodes[next - 1];
            next = link[next];
          }
          int lab   = node.getLabel();
          int child = outgoing[lab];
          if (child == 0)
            break;

          if (child < 0) // Pick one.
            child = findChain(node, done);

          if ((child > 0) && !done.get(child)) {
            link[node.getLabel()] = child;
            done.set(child);
            k = child - 1;
            continue;
          }

          break;
        }
      }

      // Grow the chains by adding on children that have more than one
      // incoming edge.

      for (int i = 1; i <= numNodes; i++) {
        if (incoming[i] == 0)
          continue;

        if (done.get(i))
          continue;

        int k = i - 1;
        while (true) {
          SGDNode node = nodes[k];
          int     next = link[node.getLabel()];
          while (next != 0) {
            node = nodes[next - 1];
            next = link[next];
          }
          int lab   = node.getLabel();
          int child = outgoing[lab];
          if (child == 0)
            break; // No edge.

           if (child < 0) // Pick one.
            child = findChain(node, done);

          if ((child > 0) && !done.get(child)) {
            link[node.getLabel()] = child;
            done.set(child);
            k = child - 1;
            continue;
          }

          break;
        }
      }

      // At this point we have grown the chains as long as possible.
      // Now it is time to group the chains.  Two chains should be
      // side-by-side if one links into the other.

      // Determine the number of chains.

      numChains = 0;
      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        int     lab  = node.getLabel();
        int     n    = incoming[lab];
        if ((n == 0) || !done.get(lab))
          numChains++;
      }

      // Specify the chain for each node and collect the chain heads.

      chain  = new int[numNodes + 1]; // The chain of each node.
      heads  = new int[numChains];    // The root of each chain.
      tails  = new int[numChains];    // The last node in the chain.
      value  = new long[numChains];   // The "value" of the chain.
      sorted = new int[numChains];

      int    k      = 0;
      for (int i = 1; i <= numNodes; i++) {
        if ((incoming[i] == 0) || // No incoming edges.
            !done.get(i)) {       // My parent has multiple children.

          SGDNode node = nodes[i - 1];
          int     next = node.getLabel();

          sorted[k] = k;
          value[k] = k * numChains;
          heads[k] = i;
          chain[i] = k;

          while (next != 0) {
            node = nodes[next - 1];
            chain[next] = k;
            next = link[node.getLabel()];
          }

          tails[k] = node.getLabel();
          k++;
        }
      }

      // Determine the order of the chains in the graph.

      for (int i = 0; i < numChains; i++) {
        int hlink = incoming[heads[i]];
        if (hlink > 0) { // Move the two chains together.
          int ch = chain[hlink];
          value[i] = value[ch] - i;
          continue;
        }
        int tlink = outgoing[tails[i]];
        if (tlink > 0) { // Move the two chains together.
          int ch = chain[tlink];
          value[i] = value[ch] + i;
          continue;
        }
      }

      // Place the chains in order using a combination sort.

      int     jumpSize = numChains;
      boolean flag;
      do {
        flag = false;
        jumpSize = (10 * jumpSize + 3) / 13;
        int ul = numChains - jumpSize;
        for (int i = 0; i < ul; i++) {
          int j  = i + jumpSize;
          int i1 = sorted[i];
          int i2 = sorted[j];
          boolean swap = (value[i2] < value[i1]);
          if (swap) {
            sorted[i] = i2;
            sorted[j] = i1;
            flag = true;
          }
        }
      } while (flag || (jumpSize > 1));
    }

    private int findChain(SGDNode node, BitVect done)
    {
      int child = -1;

      for (int j = 0; j < numEdges; j++) {
        SGDEdge edge = edges[j];
        SGDNode head = edge.getHead();
        if (head != node)
          continue;

        SGDNode tail = edge.getTail();
        int     next = tail.getLabel();
        if (done.get(next))
          continue;

        child = next;
        break;
      }

      return child;
    }

    public void layout(boolean hCompress, boolean vCompress)
    {
      // Determine the x & y positions for each node.  At this
      // stage we assume that each row is one pixel wide and each
      // column is one pixel high.

      // Initialize each node with an (x,y) where x is the chain and y
      // is the position in the chain.

      maxy = 0;
      for (int i = 0; i < numChains; i++) {
        SGDNode node = nodes[heads[sorted[i]] - 1];
        int     next = link[node.getLabel()];
        int     posy = 0;
        node.setX(i);
        node.setY(posy++);
        while (next != 0) {
          node = nodes[next - 1];
          next = link[node.getLabel()];
          node.setX(i);
          node.setY(posy++);
        }
        if (posy > maxy)
          maxy = posy;
      }

      // Try to move nodes closer in the y direction.

      boolean   bigChange;
      boolean[] moved = new boolean[numChains]; // Prevent never ending descent.
      if (!vCompress) {
        do {
          bigChange = false;
          boolean changed = false;

          do { // Move the chains down so that their tails are above the joins.
            changed = moveChainDownToJoinHead(moved);
          } while (changed);

          do { // Move the chains down so that their heads are below the joins.
            changed = moveChainDownToJoinTail(moved);
            if (changed)
              bigChange = true;
          } while (changed);
        } while (bigChange);
      }

      // Create a square array with a value for each node.
      // The value for each node is:
      //  0 - no node associated with that position
      //  1 - an un-moved node is in that position
      //  2 - a node that was moved left is in that position
      //  3 - a node that was moved right is in that position
      // The direction is used to avoid infinite loops; once a node is
      // moved one way, it's never moved the other way.

      byte[] narray = new byte[numChains * maxy];
      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        narray[node.getX() + node.getY() * numChains] = 1;
      }

      // Try to move nodes closer in the x direction.

      if (hCompress) {
        boolean changed = false;
        do {
          changed = moveLeft(narray);
        } while (changed);
      } else {
        do {
          bigChange = false;
          boolean changed;
          do {
            changed = moveTailAcrossToJoinHead(narray);
          } while (changed);
          do {
            changed = moveHeadAcrossToJoinTail(narray);
            if (changed)
              bigChange = true;
          } while (changed);
        } while (bigChange);
      }

      // Calculate the size of each row and column.

      int[] rowsize = new int[maxy];
      int[] colsize = new int[numChains];
      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        int     x    = node.getX();
        int     y    = node.getY();
        int     w    = node.getWidth() + 10;
        int     h    = node.getHeight() + 10;
        if (rowsize[y] < h)
          rowsize[y] = h;
        if (colsize[x] < w)
          colsize[x] = w;
      }

      // Calculate the position of each row and column.

      for (int i = 1; i < maxy; i++) {
        int pc = rowsize[i];
        if (pc > 0)
          pc += 15;
        rowsize[i] = pc + rowsize[i - 1];
      }
      for (int i = 1; i < numChains; i++) {
        int pc = colsize[i];
        if (pc > 0)
          pc += 15;
        colsize[i] = pc + colsize[i - 1];
      }

      // Assign the nodes their final position.

      for (int i = 0; i < numNodes; i++) {
        SGDNode node = nodes[i];
        int     x    = node.getX();
        int     y    = node.getY();
        int     w    = node.getWidth();
        int     cp   = colsize[x];
        int     cs   = (x > 0) ? (cp - colsize[x - 1]) : cp;
        x = (x > 0) ? colsize[x - 1] : 0;
        y = (y > 0) ? rowsize[y - 1] : 0;
        x += (cs - w) / 2;
        node.setX(x + 5);
        node.setY(y + 5);
      }

    }

    private boolean moveChainDownToJoinTail(boolean[] moved)
    {
      boolean changed = false;
      for (int i = 0; i < numChains; i++) {
        if (moved[i])
          continue;

        int tail = tails[i];

        // Find the chain this chain links to.

        int next = outgoing[tail];
        if (next == 0) // No chain.
          continue;

        int dt = nodes[tail - 1].getY();
        int dn = maxy;
        if (next < 0) {
          // Multiple - pick the one where the join point is the
          // highest (lowest y position).
          for (int j = 0; j < numEdges; j++) {
            SGDEdge edge = edges[j];
            SGDNode hd   = edge.getHead();
            SGDNode tl   = edge.getTail();

            if (hd == tl)
              continue;

            int nxt = tl.getLabel();
            if (chain[nxt] == chain[tail])
              continue;

            if ((hd == nodes[tail - 1]) && (tl.getY() < dn)) {
              next = nxt;
              dn = tl.getY();
            }
          }
          if (next < 0)
            continue;
        }

        if (next <= 0)
          continue;
        dn = nodes[next - 1].getY();

        if (next == tail)
          continue;

        int diff = dn - (dt + 1);
        if (diff > 0) {
          moved[i] = true;
          changed = true;
          SGDNode node = nodes[heads[i] - 1];
          next = link[node.getLabel()];
          int posy = node.getY() + diff;
          node.setY(posy++);
          while (next != 0) {
            node = nodes[next - 1];
            next = link[node.getLabel()];
            node.setY(posy++);
          }
          if (posy > maxy)
            maxy = posy;
        }
      }
      return changed;
    }

    private boolean moveChainDownToJoinHead(boolean[] moved)
    {
      boolean changed = false;
      for (int i = 0; i < numChains; i++) {
        if (moved[i])
          continue;

        int     head = heads[i];
        SGDNode node = nodes[head - 1];
        SGDNode curn = node;

        // Find the chain this chain links to.

        int dn   = 0;
        int next = incoming[head];
        if (next == 0) { // No link to the head of this chain.
          // See if any children have an incoming link.
          int cur = link[head];
          while (cur != 0) {
            int in = incoming[cur];
            if (in < 0) // More than one incoming edge.
              break;
            if (chain[in] == chain[head]) {
              cur = 0;
              break;
            }
            cur = link[cur];
          }
          if (cur == 0)
            continue;
          next = -1;
          curn = nodes[cur - 1];
          dn = curn.getY();
        }

        int dt = nodes[head - 1].getY();
        if (next < 0) {
          // Multiple - pick the one where the join point is the
          // lowest (highest y position).
          for (int j = 0; j < numEdges; j++) {
            SGDEdge edge = edges[j];
            SGDNode hd   = edge.getHead();
            SGDNode tl   = edge.getTail();

            if (hd == tl)
              continue;

            int nxt = hd.getLabel();
            if (chain[nxt] == chain[head])
              continue;

            if ((tl == curn) && (hd.getY() > dn)) {
              next = nxt;
              dn = hd.getY();
            }
          }

          if (next < 0)
            continue;
        } else
          dn = nodes[next - 1].getY();

        if (next == head)
          continue;

        int diff = (dn - dt) + 1;
        if (diff > 0) {
          moved[i] = true;
          changed = true;
          next = link[node.getLabel()];
          int posy = node.getY() + diff;
          node.setY(posy++);
          while (next != 0) {
            node = nodes[next - 1];
            next = link[node.getLabel()];
            node.setY(posy++);
          }
          if (posy > maxy)
            maxy = posy;
        }
      }

      return changed;
    }

    private boolean moveHeadAcrossToJoinTail(byte[] narray)
    {
      boolean changed = false;
      for (int j = 0; j < numEdges; j++) {
        SGDEdge edge = edges[j];
        SGDNode head = edge.getHead();

        if (outgoing[head.getLabel()] < 0)
          continue;

        SGDNode tail  = edge.getTail();
        int     x     = head.getX();
        int     tx    = tail.getX();
        int     diffx = tx - x;

        if (diffx == 0)
          continue; // It's in a chain.

        int y    = head.getY();
        int nx   = x;
        int ypos = y * numChains;

        if (tail.getY() < y)
          continue; // If this edge points up don't bother.

        if ((diffx > 0) && (narray[tx - 1 + ypos] == 0)) {
          nx = tx - 1;
        } else if ((diffx < 0) && (narray[tx + 1 + ypos] == 0)) {
          nx = tx + 1;
        } else if ((diffx < 0) && (narray[x + ypos] != 3)) {
          int lim = tail.getX();
          if (tail.getY() == (y + 1))
            lim--;
          for (int nnx = x - 1; nnx > lim; nnx--) {
            if (narray[nnx + ypos] == 0)
              nx = nnx;
          }
        } else if ((diffx > 0) && (narray[x + ypos] != 2)) {
          int lim = tail.getX();
          if (tail.getY() == (y + 1))
            lim++;
          for (int nnx = x + 1; nnx < lim; nnx++) {
            if (narray[nnx + ypos] == 0)
              nx = nnx;
          }
        }

        if (nx == x)
          continue;

        changed = true;
        narray[x + ypos] = 0;
        narray[nx + ypos] = (byte) ((diffx < 0) ? 2 : 3);
        head.setX(nx);
      }

      return changed;
    }

    private boolean moveTailAcrossToJoinHead(byte[] narray)
    {
      boolean changed = false;
      for (int j = 0; j < numEdges; j++) {
        SGDEdge edge = edges[j];
        SGDNode tail = edge.getTail();

        if (incoming[tail.getLabel()] < 0)
          continue;

        SGDNode head  = edge.getHead();
        int     x     = tail.getX();
        int     hx    = head.getX();
        int     diffx = hx - x;

        if (diffx == 0)
          continue; // It's in a chain.

        int y    = tail.getY();
        int nx   = x;
        int ypos = y * numChains;

        if ((diffx < 0) && (narray[x + ypos] != 3)) {
          int lim = head.getX();
          if (head.getY() == y - 1)
            lim--;
          for (int nnx = x - 1; nnx > lim; nnx--) {
            if (narray[nnx + ypos] == 0)
              nx = nnx;
          }
        } else if ((diffx > 0) && (narray[x + ypos] != 2)) {
          int lim = head.getX();
          if (head.getY() == y - 1)
            lim++;
          for (int nnx = x + 1; nnx < lim; nnx++) {
            if (narray[nnx + ypos] == 0)
              nx = nnx;
          }
        }

        if (nx == x)
          continue;

        changed = true;
        narray[x + ypos] = 0;
        narray[nx + ypos] = (byte) ((diffx < 0) ? 2 : 3);
        tail.setX(nx);
      }

      return changed;
    }

    private boolean moveLeft(byte[] narray)
    {
      boolean changed = false;
      for (int j = 0; j < numNodes; j++) {
        SGDNode node = nodes[j];
        int     x    = node.getX();
        int     y    = node.getY();
        int     ypos = y * numChains;

        while ((x > 0) && narray[x - 1 + ypos] == 0) {
          changed = true;
          narray[x + ypos] = 0;
          narray[x - 1 + ypos] = 1;
          node.setX(x - 1);
        }
      }

      return changed;
    }
  }

  private HashSet<DisplayNode>      snodes;    // Set of all nodes and edges processed.
  private HashMap<String, SGDFrame> windowMap; // Map from title to frame.
  private SGDFrame                  current;   // The current graph.

  public SGD()
  {
    snodes    = new HashSet<DisplayNode>(203);
    windowMap = new HashMap<String, SGDFrame>(11);
  }

  /**
   * Initialize for a new graph.
   * Only one graph can be created at a time.
   * @param context is the name associated with the graph
   * @param top if true places the root node at the top of the display
   */
  public synchronized void newGraph(String context, boolean top)
  {
    current = new SGDFrame(this, context, context, top);
    windowMap.put(context, current);
  }

  private void frameError(String msg)
  {
    System.err.println("** No such context " + msg);
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
   * Respond to interative events with this display.
   * The method terminates when the display is terminated.
   */
  public void interact()
  {
    while (windowMap.size() > 0) 
      try {
        Thread.currentThread().sleep(2000);
      } catch (java.lang.Exception ex) {
        System.out.println(ex);
        break;
      }
  }

  /**
   * This opens a new visualization window.  The context string should
   * be unique from that used for any other window.
   * @param context is the user defined string to distinguish between
   * different windows
   * @param title is title for the window
   */
  public synchronized void openWindow(String context, String title, int graphAttributes)
  {
    current = windowMap.get(context);
    if (current == null) {
      frameError(context);
      return;
    }
    current.open();
  }

  /**
   * This closes an existing visualization window.
   * The context string should be unique from that used for any
   * other window.
   * @param context is the user defined string to distinguish between
   * different windows
   */
  public synchronized void closeWindow(String context)
  {
    SGDFrame win = windowMap.get(context);
    if (win == null) {
      frameError(context);
      return;
    }
    win.dispose();
    if (win == current)
      current = null;
    windowMap.remove(context);
  }

  /**
   * Terminates the visualizer process.
   */
  public synchronized void terminate()
  {
    Enumeration<SGDFrame> en = windowMap.elements();
    while (en.hasMoreElements()) {
      en.nextElement().dispose();
    }
    windowMap.clear();
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
   * Add a node to the graph.  This method should be used only for
   * nodes that are not connected with an edge.
   * @param n is the node
   */
  public void addNode(DisplayNode n)
  {
    if (current == null) {
      frameError("no open frame");
      return;
    }
    current.addNode(n);
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
    if (current == null) {
      frameError("no open frame");
      return;
    }
    current.addEdge(n1, n2, color, edgeAttributes, edgeInfo);
  }

  /**
   * Return true if the window for the given context still exists.
   */
  public synchronized boolean windowExists(String context)
  {
    SGDFrame win = windowMap.get(context);
    return (win != null);
  }
}
