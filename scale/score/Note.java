package scale.score;

import scale.common.Stack;

import scale.common.*;
import scale.score.expr.Expr;

import scale.score.chords.Chord;

/**
 * This class is the base class for the CFG class hierarchy. 
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Score has two kinds of nodes:
 * <ul>
 * <li> expressions ({@link scale.score.expr.Expr Expr}), and
 * <li> CFG nodes ({@link scale.score.chords.Chord Chord}).
 * </ul>
 * Expressions represent the expected notion of expression (e.g., a +
 * b).  {@link scale.score.chords.Chord Chords} represent executable
 * statements in a program.  These two kinds of nodes are realized as
 * derived classes of this class.
 * <p>
 * The CFG does not use the special/region node subhierarchy.  The CFG
 * uses "cfg" edges instead of control edges.  We build a {@link
 * scale.score.CDG CDG} on top of an existing CFG.
 * <p>
 * Chords have an unknown number of incoming cfg edges
 * (aka, data flow edges) and a fixed number of outgoing CFG edges.
 * Chords also have a fixed number of incoming data edges but no
 * outgoing data edges.  Exprs have a fixed number of incoming data
 * edges and one outgoing data edge.
 * <p>
 * Our represntation maintains bi-directional edges.  For every graph
 * edge which points to this node, this node also has a pointer back
 * to the source of the graph edge.  Hence, a CFG has twice as
 * many edges as the conceptual graph which it is implementing.
 * <p>
 * From a very high-level view, nodes support a fully general
 * graph.  But in reality, different types of nodes have different
 * restrictions on their incoming and outgoing edges.
 */     

public abstract class Note extends Root
{
  private static int toStringLevel       = 0;
  private static int reportLevel         = 2;
  private static int showAnnotationLevel = 0;

  /**
   * Constructor for a CFG node instance.
   */
  protected Note()
  {
    super();
  }

  /**
   * Use this method when you may be modifying an in-coming data edge
   * while iterating over the edges.
   * @return an array of in-coming data edges.  
   */
  public abstract scale.score.expr.Expr[] getInDataEdgeArray();

  /**
   * Return the specified in-coming data edge.
   */
  public abstract scale.score.expr.Expr getInDataEdge(int i);

  /**
   * Return number of in-coming data edges.
   */
  public abstract int numInDataEdges();

  /** 
   * This method changes an incoming data edge to point to a new
   * expression.
   * <p>
   * This method ensures that the node previously pointing to
   * this one is updated properly, as well as, the node which will now
   * point to this node.
   * <p>
   * Expr and Chord nodes have a fixed number of incoming
   * edges with specific meaning applied to each.  Hence, the edge
   * being replaced is indicated by position.
   * @param oldExpr is the expression to be replaced
   * @param newExpr is the new expression
   */
  public abstract void changeInDataEdge(scale.score.expr.Expr oldExpr,
                                        scale.score.expr.Expr newExpr);

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
   *   public void visitABC(Note n)
   *   {
   *     ABC a = (ABC) n;
   *     ...
   *   }
   * </pre>
   * Thus, the class that implements <code>Predicate</code> can call
   * <pre>
   *   n.visit(this);
   * </pre>
   * where <code>n</code> is a <code>Note</code> sub-class without
   * determining which specific sub-class <code>n</code> is.
   * The visit pattern basically avoids implementing a large
   * <code>switch</code> statement or defining different methods
   * in each class for some purpose.
   * @see Predicate
   */
  public abstract void visit(Predicate p);

  /**
   * Set the depth to which a node displays its children.
   */
  public static void setReportLevel(int level)
  {
    reportLevel = level;
    Msg.reportInfo(Msg.MSG_Note_report_level_is_s, Integer.toString(reportLevel));
  }

  /**
   * Set the depth to which a node displays it's annotations.
   */
  public static void setAnnotationLevel(int level)
  {
    showAnnotationLevel = level;
  }

  public final String toString()
  {
    StringBuffer s = new StringBuffer("(");

    s.append(toStringClass());
    s.append('-');
    s.append(getNodeID());

    if (toStringLevel < reportLevel) {
      toStringLevel += 1;
      s.append(' ');
      s.append(toStringSpecial());
      if (toStringLevel < showAnnotationLevel)
        s.append(toStringAnnotations());
      toStringLevel -= 1;
    } else
      s.append("...");

    s.append(")");

    return s.toString();
  }

  /**
   * Return the {@link scale.score.chords.Chord Chord} instance
   * containing this Note.
   */
  public final Chord getChord()
  {
    Note n = this;

    while (n instanceof Expr) {
      Expr exp = (Expr) n;
      n = exp.getOutDataEdge();
    }
    return (Chord) n;
  }

  /**
   * Check this node for validity.  This method throws an error if
   * the node is not linked properly.
   */
  public void validate()
  {
  }

  /**
   * Return this Note instance unless it is a non-essential
   * expression.  For {@link scale.score.chords.Chord Chord} this
   * method returns <code>this</code>.  For a {@link
   * scale.score.expr.DualExpr DualExpr} or an address cast (e.g.,
   * {@link scale.score.expr.ConversionExpr ConversionExpr}) this
   * method returns the out data edge.
   */
  public Note getEssentialUse()
  {
    return this;
  }

  /**
   * Return a relative cost estimate for executing this node.
   */
  public abstract int executionCostEstimate();
}
