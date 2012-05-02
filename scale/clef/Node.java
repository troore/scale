package scale.clef;

import scale.common.*;

/**
 * The base class for the Clef representation.
 * <p>
 * $Id: Node.java,v 1.83 2007-03-21 13:31:49 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class Node extends Root
{
  private static int toStringLevel       = 0;
  private static int reportLevel         = 2;
  private static int showAnnotationLevel = 0;

  /**
   * Constructor for a Clef node object.
   */
  public Node()
  {
    super();
  }

  /**
   * Process a node by calling its associated routine.
   * See the "visitor" design pattern in <cite>Design Patterns:
   * Elements of Reusable Object-Oriented Software</cite> by E. Gamma,
   * et al, Addison Wesley, ISBN 0-201-63361-2.
   * <p>
   * Each class has a <code>visit(Predicate p)</code> method.  For
   * example, in <code>class ABC</code>:
   * <pre>
   *   public void visit(Predicate p)
   *   {
   *     p.visitABC(this);
   *   }
   * </pre>
   * and the class that implements <code>Predicate</code> has a method
   * <pre>
   *   public void visitABC(Node n)
   *   {
   *     ABC a = (ABC) n;
   *     ...
   *   }
   * </pre>
   * Thus, the class that implements <code>Predicate</code> can call
   * <pre>
   *   n.visit(this);
   * </pre>
   * where <code>n</code> is a <code>Node</code> sub-class without
   * determining which specific sub-class <code>n</code> is.
   * The visit pattern basically avoids implementing a large
   * <code>switch</code> statement or defining different methods
   * in each class for some purpose.
   * @see Predicate
   */
  public abstract void visit(Predicate p);

  /**
   * Return the Type associated with this Node.
   */
  public scale.clef.type.Type getType()
  {
    return null;
  }

  /**
   * Return the actual Type associated with this Node.
   *
   * Note - getType() should always be used when the Type
   * is not examined or when the attributes are needed.
   *
   * @see #getType
   * @see scale.clef.type.RefType
   */
  public scale.clef.type.Type getCoreType()
  {
    return null;
  }

  /**
   * Return any Declaration associated with this Node.
   */
  public scale.clef.decl.Declaration getDecl()
  {
    return null;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 0;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    throw new scale.common.InternalError("No such child " + i);
  }
  
  /**
   * Convert children of this node to a string representation.
   */
  public String toStringChildren()
  {
    StringBuffer s = new StringBuffer();
    int          l = numChildren();
    int          k = 0;
    for (int i = 0; i < l; i++) {
      Node child = getChild(i);
      if (child == null)
        continue;

      if (k >= 10) {
        s.append(" ...");
        break;
      }

      k++;

      String str = child.toString();
      if (str.charAt(0) != ' ')
        s.append(' ');
      s.append(str);
    }

    return s.toString();
  }

  /**
   * Set the depth to which a node displays it's children.
   */
  public static void setReportLevel(int level)
  {
    reportLevel = level;
    Msg.reportInfo(Msg.MSG_Node_report_level_is_s, null, 0, 0, Integer.toString(reportLevel));
  }

  /**
   * Set the depth to which a node displays it's annotations.
   */
  public static void setAnnotationLevel(int level)
  {
    showAnnotationLevel = level;
  }

  public String toString()
  {
    return toString("(", ")");
  }

  /**
   * The <code>toString()</code> method with begin and end delimiters specified.
   */
  public final String toString(String del1, String del2)
  {
    int ln = getSourceLineNumber();
    StringBuffer s = new StringBuffer(del1);
    s.append(toStringClass());
    s.append('-');
    s.append(getNodeID());

    if (ln > 0) {
      s.append(" l:");
      s.append(ln);
    }

    if (toStringLevel < reportLevel) {
      toStringLevel += 1;
      s.append(' ');
      s.append(toStringSpecial());
      s.append(toStringChildren());
      if (toStringLevel < showAnnotationLevel)
        s.append(toStringAnnotations());
      toStringLevel -= 1;
    } else
      s.append(" ...");

    s.append(del2);

    return s.toString();
  }

  /**
   * This method allows sub-classes to provide class specific stuff to the string.
   */
  public String toStringSpecial()
  {
    return "";
  }

  /**
   * Return the source line number associated with this node or -1 if not known.
   */
  public int getSourceLineNumber()
  {
    return -1;
  }

  /**
   * Set the source line number associated with this node or -1 if not known.
   */
  public void setSourceLineNumber(int lineNumber)
  {
  }
}
