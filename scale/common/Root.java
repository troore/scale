package scale.common;

import java.util.Enumeration;
import java.io.PrintStream;

import scale.annot.*;

/**
 * This class is the top level class for all Scale classes that may be
 * annotated or graphically displayed.
 * <p>
 * $Id: Root.java,v 1.22 2007-08-13 12:32:02 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Each instance has a unique id number associated with it.
 * @see scale.annot.Annotation
 * @see scale.clef.Node
 * @see scale.score.Note
 */     

public abstract class Root implements AnnotationInterface, DisplayNode
{
  private static int nodeCount = 0;

  /**
   * The unique node id.
   */
  private int nodeID = 0;

  protected Root()
  {
    nodeID = nodeCount++;
  }

  /**
   * Return the unique node label.
   */
  public final int getNodeID()
  {
    return nodeID;
  }

  /**
   * Print out a trace message to stdout if the node id of this node
   * matches.  The trace message is the specified messsage followed by
   * the display of this node.
   * @param id is the node id to match
   * @param msg is the beginning message text
   */
  public final void trace(int id, String msg)
  {
    if (nodeID != id)
      return;

    traceInt(id, msg, false, System.out);
  }

  /**
   * Print out a trace message to stdout if the node id of this node
   * matches.  The trace message is the specified messsage followed by
   * the display of this node.
   * @param id is the node id to match
   * @param msg is the beginning message text
   * @param stkTrace is true for a display of the Java stack
   */
  public final void trace(int id, String msg, boolean stkTrace)
  {
    if (nodeID != id)
      return;

    traceInt(id, msg, stkTrace, System.out);
  }

  /**
   * Print out a trace message to specified stream if the node id of
   * this node matches.  The trace message is the specified messsage
   * followed by the display of this node.
   * @param id is the node id to match
   * @param msg is the beginning message text
   * @param stkTrace is true for a stack trace
   * @param str is the {@link java.io.PrintStream stream} to use
   */
  public final void trace(int id, String msg, boolean stkTrace, PrintStream str)
  {
    if (nodeID != id)
      return;

    traceInt(id, msg, stkTrace, str);
  }

  /**
   * Make it private so that we know we can always discard the top two
   * stack entries.
   */
  private void traceInt(int id, String msg, boolean stkTrace, PrintStream str)
  {
    str.print(msg);
    str.print(" ");
    str.println(this.toString());

    if (!stkTrace)
      return;

    try {
      throw new InternalError("stack trace");
    } catch(InternalError ex) {
      StackTraceElement[] stk = ex.getStackTrace();
      for (int i = 2; i < stk.length; i++) {
        str.print("\tat ");
        str.println(stk[i].toString());
      }
    }
  }

  /**
   * Return the number of nodes created so far.
   */
  public static final int getNodeCount()
  {
    return nodeCount;
  }

  /**
   * Return a unique label for graphical displays.
   */
  public String getDisplayName()
  {
    return "N" + nodeID;
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.  This method should be over-ridden as it simplay returns
   * the class name.
   */
  public String getDisplayLabel()
  {
    return toStringClass();
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.  This method should be over-ridden
   * as it simplay returns the color red.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.RED;
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.  This method should be over-ridden as it
   * simplay returns the shape "box".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.BOX;
  }

  /**
   * Adds an annotation to this node's annotation list.
   * Redundant annotations are permitted.
   */
  public final void addAnnotation(Annotation a)
  {
    if (a != null)
      a.addNode(this);
  }

  /**
   * Delete the given annotation.
   */
  public final void removeAnnotation(Annotation a)
  {
    a.removeNode(this);
  }

  /**
   * Delete all annotations which match the given key.
   */
  public final void removeAnnotations(Object key)
  {
    Annotation.removeNode(this, key);
  }

  /**
   * Returns an arbitrary annotation of the indicated kind.
   */
  public final Annotation getAnnotation(Object annotation_key)
  {
    return Annotation.getAnnotation(annotation_key, this);
  }

  /**
   * Returns true if this node has the indicated kind of annotation.
   */
  public final boolean hasAnnotation(Object annotation_key)
  {
    return Annotation.hasAnnotation(annotation_key, this);
  }

  /**
   * Returns true if this node has an annotation equal to the
   * given annotation.
   */
  public final boolean hasEqualAnnotation(Annotation a)
  {
    return Annotation.hasEqualAnnotation(a, this);
  }

  /**
   * Returns an enumeration of all the annotations associated
   * with this node.
   */
  public final Enumeration<Annotation> allAnnotations()
  {
    return Annotation.allAnnotations(this);
  }

  /**
   * Returns an enumeration of all the annotations of the given
   * kind associated with this node.
   */
  public final Enumeration<Annotation> allMatchingAnnotations(Object annotation_key)
  {
    return Annotation.allMatchingAnnotations(annotation_key, this);
  }

  /**
   * Convert the annotations of this node to a string representation.
   */
  public final String toStringAnnotations()
  {
    return Annotation.toStringAnnotations(this);
  }

  /**
   * Return any special information of a node that is not a child or 
   * annotation.  This method is meant to be overridden by nodes that
   * need to provide special output. 
   */
  public String toStringSpecial()
  {
    return "";
  }

  /**
   * Convert the class name of this node to a string representation.
   */
  public final String toStringClass()
  {
    String c = getClass().toString();
    return c.substring(c.lastIndexOf('.') + 1);
  }

  public String toString()
  {
    StringBuffer s = new StringBuffer("(");
    s.append(toStringClass());
    s.append('-');
    s.append(getNodeID());
    s.append(' ');
    s.append(toStringSpecial());
    s.append(toStringAnnotations());
    s.append(")");
    return s.toString();
  }

  /**
   * Use the node ID as the hash code so that the order of processing
   * is not affected by changes to the Java code.  Without this
   * method, the order in which enumerations of hash set and hash maps
   * return elements depends on the address of the element which is
   * affected by many things including the particular parameters
   * selected when executing the Scale compiler.
   */
  public int hashCode()
  {
    return nodeID;
  }

  /**
   * Convert a string to a form suitable for display.  For example, a
   * double quote (") is converted to "\"" and "\0" is converted to
   * "\\0".
   * @param v the original string
   * @return the converted string
   */
  public String getDisplayString(String v)
  {
    StringBuffer buf = new StringBuffer("");
    int          n   = v.length();

    for (int i = 0; i < n; i++) {
      char ch = v.charAt(i);
      switch (ch) {
      case '\0': buf.append("\\0"); break;
      case '\b': buf.append("\\b"); break;
      case '\t': buf.append("\\t"); break;
      case '\n': buf.append("\\n"); break;
      case '\f': buf.append("\\f"); break;
      case '\r': buf.append("\\r"); break;
      case '"' : buf.append("\\\""); break;
      case '\'': buf.append("\\\'"); break;
      case '\\': buf.append("\\\\"); break;
      default:
        if (((ch >= '\u0001') && (ch <= '\u001F')) ||
            ((ch >  '\u007F') && (ch <= '\u00FF'))) {
          buf.append("\\x");
          buf.append(Long.toHexString(ch));
        } else {
          buf.append(ch);
        }
        break;
      }
    }

    return buf.toString();
  }
}
