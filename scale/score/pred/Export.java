package scale.score.pred;

import java.io.*;

import scale.common.*;
import scale.annot.*;
import scale.clef.Node;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.CallExpr;
import scale.score.expr.Expr;
import scale.score.expr.LoadExpr;
import scale.score.analyses.MayDef;
import scale.score.analyses.MayUse;

/**
 * This predicate class exports a Scribble graph so that it can be
 * visualized.
 * <p>
 * $Id: Export.java,v 1.58 2006-02-28 16:37:17 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The primary difference in the different edges is the color and/or
 * type of line used to represent it.
 */

public abstract class Export extends Supertype
{
  private static final DEdge cfgEdge  = DEdge.SOLID;
  private static final DEdge dataEdge = DEdge.DASHED;
  private static final DEdge badEdge  = DEdge.DOTTED;

  // Colors for edges:

  private static final DColor trueColor   = DColor.DARKGREEN;
  private static final DColor falseColor  = DColor.RED;
  private static final DColor dataColor   = DColor.PURPLE;
  private static final DColor domColor    = DColor.GREEN;
  private static final DColor useDefColor = DColor.GREEN;
  private static final DColor defUseColor = DColor.BROWN;
  private static final DColor mayUseColor = DColor.PINK;
  private static final DColor mayDefColor = DColor.ORANGE;
  private static final DColor liveColor   = DColor.BLUE;
  private static final DColor phiColor    = DColor.MAGENTA;
  private static final DColor badColor    = DColor.BLACK;
  private static final DColor annoColor   = DColor.RED;
  private static final DColor clefColor   = DColor.MAGENTA;

  /**
   * The display graph object.
   */
  protected DisplayGraph da;

  /**
   * @param da is the display graph object
   */
  protected Export(DisplayGraph da)
  {
    this.da = da;
  }

  /**
   * Perform the traversal of the nodes.
   * @param start is the first node to traverse
   */
  public abstract void traverse(Chord start);

  /**
   * Add the out-going data edges of the node to the graph.
   */
  protected void addDataEdges(Note n)
  {
    if (n instanceof Expr) {
      Expr exp    = (Expr) n;
      Note child  = exp.getOutDataEdge();
      addDataEdge(n, child);
    }
  }

  /**
   * Add the out-going CFG edges of the node to the graph.
   */
  protected void addCfgEdges(Chord n)
  {
    int l = n.numOutCfgEdges();
    for (int i = 0; i < l; i++) {
      Chord child = n.getOutCfgEdge(i);
      addCfgEdge(n, child);
    }
  }

  /**
   * Add a regular use-def edge in the graph.
   * @param n1 the use node
   * @param n2 the def node
   */
  protected void addUseDefEdge(Note n1, Note n2)
  {
    da.addEdge(n1, n2, useDefColor, badEdge, "use-def");
  }

  /**
   * Add a regular use-def edge in the graph.
   * @param n1 the use node
   * @param n2 the def node
   */
  protected void addDefUseEdge(Note n1, Note n2)
  {
    da.addEdge(n1, n2, defUseColor, badEdge, "def-use");
  }

  /**
   * Add an edge representing a May Use.
   * @param n1 the node reprsenting the use
   * @param mayUse the node containing info on the potential definition
   * @see scale.score.analyses.MayUse
   */
  protected void addMayUseEdge(Note n1, MayUse mayUse)
  {
    da.addEdge(mayUse, n1, mayUseColor, badEdge, "may-use");
  }

  /**
   * Add an edge representing a May Def.
   * @param n1 the node causing the may definition.
   * @param mayDef a node that representing the previous definition.
   * @see scale.score.analyses.MayDef
   */
  protected void addMayDefEdge(Note n1, MayDef mayDef)
  {
    da.addEdge(mayDef, n1, mayDefColor, badEdge, "may-def");
  }

  /**
   * Add an edge representing a May Def graph node link.
   * @param n1 the node causing the may definition.
   * @param mayDef a node that representing the previous definition.
   * @see scale.score.analyses.MayDef
   */
  protected void addGraphNodeEdge(Note n1, MayDef mayDef)
  {
    da.addEdge(n1, mayDef, badColor, badEdge, "graph-node");
  }

  /**
   * Create a CFG edge from n1 to n2.
   */
  protected void addCfgEdge(Chord n1, Chord n2)
  {
    da.addEdge(n1, n2, liveColor, cfgEdge, "CFG");
  }

  /**
   * Used for if-then-else constructs to create an edge from n1 to n2.
   */
  protected void addTrueCfgEdge(Chord n1, Chord n2)
  {
    da.addEdge(n1, n2, trueColor, cfgEdge, "CFG True");
  }

  /**
   * Used for if-then-else constructs to create an edge from n1 to n2.
   */
  protected void addFalseCfgEdge(Chord n1, Chord n2)
  {
    da.addEdge(n1, n2, falseColor, cfgEdge, "CFG False");
  }

  /**
   * Display an "data" edge between the two objects.
   */
  protected void addDataEdge(Note n1, Note n2)
  {
    da.addEdge(n1, n2, dataColor, dataEdge, "Data");
  }

  /**
   * Create a "dominance" edge from n1 to n2.
   */
  protected void addDomEdge(Note n2, Note n1)
  {
    da.addEdge(n1, n2, domColor, badEdge, "Domination");
  }

  /**
   * Create an edge from a node to an annotation.
   */
  protected void addAnnotationEdge(Annotation n2, Note n1)
  {
    da.addEdge(n1, n2, annoColor, badEdge, "Annotation");
  }

  /**
   * Create an AST edge from n1 to n2.
   */
  protected void addClefEdge(Node n1, Note n2)
  {
    da.addEdge(n1, n2, clefColor, badEdge, "Clef");
  }

  /**
   * Create an edge from n1 to n2 where the link is not correct.
   */
  protected void addBadEdge(Note n1, Note n2)
  {
    da.addEdge(n1, n2, badColor, badEdge, "Bad");
  }
}
