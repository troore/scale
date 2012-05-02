package scale.clef;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;
import scale.annot.Annotation;

/**
 * A class which generates information to generate a graph of a Clef
 * tree.
 * <p>
 * $Id: Display.java,v 1.39 2007-10-04 19:58:02 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Do a visit on the node that you want to be the root and supply an
 * instance of this class as the Predicate.  Then use getCommand() to
 * get the commands for daVinci.
 */
public class Display extends DescendPredicate
{
  private boolean      exprTypes   = false; // True to display the types of expressions
  private boolean      annotations = false; // True to display annotations
  private boolean      expressions = false; // True to display expressions
  private DisplayGraph da;

  /**
   * Display the Clef AST graphically.
   * @param da the display graph object
   * @param displayFlags specify what should be displayed
   * @see scale.common.DisplayNode
   */
  public Display(DisplayGraph da, int displayFlags)
  {
    this.da          = da;
    this.exprTypes   = (displayFlags & DisplayGraph.SHOW_TYPE) != 0;
    this.annotations = (displayFlags & DisplayGraph.SHOW_ANNO) != 0;
    this.expressions = (displayFlags & DisplayGraph.SHOW_EXPR_MASK) != 0;
  }

  private void visitAnnotations(Node n)
  {
    if (!annotations)
      return;

    Enumeration<Annotation> ea = n.allAnnotations();
    while (ea.hasMoreElements()) {
      Annotation a = ea.nextElement();
      da.addEdge(a, n, a.getDisplayColorHint(), DEdge.DOTTED, null);
    }
  }

  /**
   * A routine to visit the children of a node and generate an edge.
   * @param parent a clef node
   */
  public void visitChildren(Node parent)
  {
    int l = parent.numChildren();
    for (int i = 0; i < l; i++) {
      Node child = parent.getChild(i);
      if (child == null)
        continue;
      if (!da.visited(child))
        child.visit(this);
      da.addEdge(child,
                 parent,
                 parent.getDisplayColorHint(),
                 DEdge.SOLID,
                 null);
    }
    visitAnnotations(parent);
  }

  public void visitExpression(Expression exp)
  {
    if (!expressions)
      return;

    visitChildren(exp);
    if (exprTypes) {
      Type t = exp.getType();
      if (t != null) {
        if (!da.visited(t))
          t.visit(this);
        da.addEdge(t,
                   exp,
                   t.getDisplayColorHint(),
                   DEdge.DOTTED,
                   null);
      }
    }
  }
  
  public void visitDeclaration(Declaration d)
  {  
    visitChildren(d);
    if (exprTypes) {
      Type t = d.getType();
      if (t != null) {
        if (!da.visited(t))
          t.visit(this);
        da.addEdge(t,
                   d,
                   t.getDisplayColorHint(),
                   DEdge.DOTTED,
                   null);
      }
    }
  }
  
  public void visitAssignmentOp(AssignmentOp assign)
  {  
    if (!expressions)
      return;

    Expression lhs = assign.getLhs();
    Expression rhs = assign.getRhs();

    if (!da.visited(rhs))
      rhs.visit(this);

    if (!da.visited(lhs))
      lhs.visit(this);

    da.addEdge(rhs,
               assign,
               rhs.getDisplayColorHint(),
               DEdge.DOTTED,
               null);
    da.addEdge(lhs,
               assign,
               lhs.getDisplayColorHint(),
               DEdge.DOTTED,
               null);

    if (exprTypes) {
      Type t = assign.getType();
      if (t != null) {
        if (!da.visited(t))
          t.visit(this);
        da.addEdge(t,
                   assign,
                   t.getDisplayColorHint(),
                   DEdge.DOTTED,
                   null);
      }
    }
    visitAnnotations(assign);
  }

  public void visitSubscriptOp(SubscriptOp n)
  {  
    if (expressions)
      visitExpression(n);
  }
}
