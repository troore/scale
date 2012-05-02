package scale.score.pred;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;

import scale.common.*;
import scale.annot.*;

import scale.clef.*;
import scale.clef.decl.Declaration;
import scale.clef.type.Type;
import scale.clef.expr.Literal;

import scale.score.*;
import scale.score.chords.*;

import scale.score.expr.*;
import scale.score.analyses.*;
import scale.score.dependence.DDGraph;

/**
 * This predicate class exports a Scribble CFG so that it can be
 * visualized.
 * <p>
 * $Id: ExportCFG.java,v 1.78 2007-10-04 19:58:34 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Each predicate builds a term representation appropriate for input
 * to the DisplayGraph visualization tool.
 */

public class ExportCFG extends Export 
{

  private boolean  displayDefUse;
  private boolean  displayMayUse;
  private boolean  displayClef;
  private boolean  displayTypes;
  private boolean  displayAnno;
  private boolean  displayExpr;
  private boolean  displayLowExpr;
  private boolean  displayHighExpr;
  private boolean  displayDD;
  private boolean  displayDom;
  private boolean  displayPDom;
  private boolean  displayCDG;
  private int      displayFlags;
  private Display  cDisplay;
  private Scribble scribble;

  /**
   * Display the CFG graphically.
   * @param da the display graph object
   * @param displayFlags specify what should be displayed
   * @see scale.common.DisplayNode
   */
  public ExportCFG(DisplayGraph da, Scribble scribble, int displayFlags)
  {
    super(da);

    this.scribble     = scribble;
    this.displayFlags = displayFlags;

    this.displayDefUse   = (displayFlags & DisplayGraph.SHOW_DEFUSE) != 0;
    this.displayMayUse   = (displayFlags & DisplayGraph.SHOW_MAYUSE) != 0;
    this.displayClef     = (displayFlags & DisplayGraph.SHOW_CLEF) != 0;
    this.displayTypes    = (displayFlags & DisplayGraph.SHOW_TYPE) != 0;
    this.displayAnno     = (displayFlags & DisplayGraph.SHOW_ANNO) != 0;
    this.displayExpr     = (displayFlags & DisplayGraph.SHOW_EXPR_MASK) != 0;
    this.displayLowExpr  = (displayFlags & DisplayGraph.SHOW_LOW_EXPR) != 0;
    this.displayHighExpr = (displayFlags & DisplayGraph.SHOW_HIGH_EXPR) != 0;
    this.displayDD       = (displayFlags & DisplayGraph.SHOW_DD) != 0;
    this.displayDom      = (displayFlags & DisplayGraph.SHOW_DOM) != 0;
    this.displayPDom     = (displayFlags & DisplayGraph.SHOW_PDOM) != 0;
    this.displayCDG      = (displayFlags & DisplayGraph.SHOW_CDG) != 0;

    if (this.displayClef || this.displayTypes)
      this.cDisplay = new Display(da, displayFlags);
  }

  /**
   * Traverse the CFG to actually build the display.  <p> <b>Note -
   * this method uses {@link scale.score.chords.Chord#nextVisit
   * nextVisit()}.</b>
   */
  public void traverse(Chord start)
  {
    if (displayDom)
      traverseDom(start);
    else if (displayPDom)
      traversePDom(start);
    else if (displayCDG)
      traverseCDG(start);
    else
      traverseCFG(start);

    if (displayDD) {
      DDGraph ddGraph = scribble.getLoopTree().getDDGraph(false);
      if (ddGraph != null)
        ddGraph.graphDependence(da, !displayExpr);
    }
  }

  /**
   * Traverse the CFG to actually build the display.
   */
  private void traverseCFG(Chord start)
  {
    Stack<Chord> wl = WorkArea.<Chord>getStack("traverseCFG");

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();

      c.visit(this);

      traverseCFGNote(c);
      c.pushInCfgEdges(wl);
      c.pushOutCfgEdges(wl);
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Treverse the dominator tree and construct the edges.
   */
  public void traverseDom(Chord start)
  {
    Domination   dom = scribble.getDomination();
    Stack<Chord> wl  = WorkArea.<Chord>getStack("traverseDom");

    wl.push(start);

    while (!wl.empty()) {
      Chord c = wl.pop();

      traverseCFGNote(c);

      Chord[] ed = dom.getDominatees(c);
      for (Chord child : ed) {
        wl.push(child);
        addDomEdge(child, c);
      }
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Treverse the dominator tree and construct the edges.
   */
  public void traversePDom(Chord start)
  {
    Domination   dom = scribble.getPostDomination();
    Stack<Chord> wl  = WorkArea.<Chord>getStack("traversePDom");

    wl.push(start);

    while (!wl.empty()) {
      Chord c = wl.pop();

      traverseCFGNote(c);

      Chord[] ed = dom.getDominatees(c);
      for (Chord child : ed) {
        wl.push(child);
        addDomEdge(child, c);
      }
    }

    WorkArea.<Chord>returnStack(wl);
  }

  /**
   * Treverse the dominator tree and construct the edges.  <p> <b>Note
   * - this method uses {@link scale.score.chords.Chord#nextVisit
   * nextVisit()}.</b>
   */
  private void traverseCDG(Chord start)
  {
    CDG          cdg = new CDG(scribble);
    Stack<Chord> wl  = WorkArea.<Chord>getStack("traverseCDG");

    Chord.nextVisit();
    wl.push(start);
    start.setVisited();

    while (!wl.empty()) {
      Chord c = wl.pop();
      c.pushOutCfgEdges(wl);

      traverseCFGNote(c);

      if (c.isFirstInBasicBlock()) {
        java.util.HashMap<Chord, Boolean> parents = cdg.getParents(c);
        if (parents != null) {
          java.util.Set<Chord> keys = parents.keySet();
          Iterator<Chord>      it   = keys.iterator();
          while (it.hasNext()) {
            Chord   p    = it.next();
            Boolean cond = parents.get(p);
            da.addEdge(p,
                       c,
                       DColor.GREEN,
                       DEdge.SOLID,
                       "CDG " + ((cond == null) ? "always" : cond.toString()));
          }
        }
      }

      Vector<Chord> v = cdg.getDependents(c);
      if (v == null) {
        if (!c.isLoopTail())
          addCfgEdge(c, c.getNextChord());
        continue;
      }

      int l = v.size();
      for (int i = 0; i < l; i++) {
        Chord child = v.get(i);
        da.addEdge(c,
                   child,
                   DColor.RED,
                   DEdge.DOTTED,
                   "CDG forward dependence");
      }
    }

    WorkArea.<Chord>returnStack(wl);
  }

  private void traverseCFGNote(Note n)
  {
    if (displayAnno) {
      Enumeration<Annotation> ea = n.allAnnotations();
      while (ea.hasMoreElements()) {
        Annotation a = ea.nextElement();
        addAnnotationEdge(a, n);
      }
    }

    if (!displayExpr)
      return;

    int l = n.numInDataEdges();
    for (int i = 0; i < l; i++) {
      Expr exp = n.getInDataEdge(i);
      Note oe  = exp.getOutDataEdge();

      if (exp instanceof DualExpr) {
        if (displayHighExpr) {
          exp = ((DualExpr) exp).getHigh();
          addDataEdge(exp, n);
          traverseCFGNote(exp);
          continue;
        } else if (displayLowExpr) {
          exp = exp.getLow();
          addDataEdge(exp, n);
          traverseCFGNote(exp);
          continue;
        }
      }

      if (oe != n)
        addBadEdge(n, exp);

      exp.visit(this);
      traverseCFGNote(exp);
    }
  }

  public void visitExpr(Expr e)
  {
    addDataEdges(e);
    if (displayTypes) {
      Type t = e.getType();
      if (!da.visited(t))
        t.visit(cDisplay);
      addClefEdge(t, e);
    }
  }

  public void visitLoadExpr(LoadExpr e)
  {
    // We add special use-def edges as well as regular data edges for loads.
    // If we have aliases, then we add may-use edges.

    if (displayClef) {
      Declaration d = e.getDecl();
      if (d != null) {
        if (!da.visited(d))
          d.visit(cDisplay);
        addClefEdge(d, e);
      }
    }

    if (displayMayUse) {
      MayUse mue =  e.getMayUse();
      if (mue != null) {
        visitMayUse(mue);
        addMayUseEdge(e, mue);
      }
    }
    visitExpr(e);
  }

  public void visitLiteralExpr(LiteralExpr e)
  {
    // We add special use-def edges as well as regular data edges for
    // loads.  If we have aliases, then we add may-use edges.

    if (displayClef) {
      Literal lit = e.getLiteral();
      if (lit != null) {
        if (!da.visited(lit))
          lit.visit(cDisplay);
        addClefEdge(lit, e);
      }
    }

    visitExpr(e);
  }

  public void visitExprChord(ExprChord c)
  {
    visitChord(c);

    // We add special def-def edges as well as regular data edges for
    // stores.  These are only added for dependences caused by
    // aliases.

    if (displayMayUse) {
      MayDef mde = c.getMayDef();
      if (mde != null) {
        visitMayDef(mde);
        addMayDefEdge(c, mde);
      }
    }

    if (displayDefUse) {
      int ndu = c.numDefUseLinks();
      for (int id = 0; id < ndu; id++) {
        LoadExpr exp = c.getDefUse(id);
        addDefUseEdge(c, exp);
      }
    }

    Expr predicate = c.getPredicate();
    if (predicate == null)
      return;
    da.addEdge(c,
               predicate,
               (c.predicatedOnTrue() ? DColor.GREEN :
                                       DColor.RED),
               DEdge.DASHED,
               "Pred");
  }

  private void visitMayDef(MayDef store)
  {
    // We add special def-def edges as well as regular data edges for
    // stores.  These are only added for dependences caused by
    // aliases.

    store.getLhs().visit(this);
    store.getRhs().visit(this);
    addGraphNodeEdge(store.getGraphNode(), store);
  }

  private void visitMayUse(MayUse use)
  {
    // We add special use-def edges as well as regular data edges for loads.
    // If we have aliases, then we add may-use edges.

    if (displayClef) {
      Declaration d = use.getDecl();
      if (d != null) {
        if (!da.visited(d))
          d.visit(cDisplay);
        da.addEdge(d, use, DColor.MAGENTA, DEdge.DOTTED, "Clef");
      }
    }

    MayUse mue =  use.getMayUse();
    if (mue != null) {
      visitMayUse(mue);
      da.addEdge(mue, use, DColor.PINK, DEdge.DOTTED, "may-use");
    }
  }

  public void visitCallExpr(CallExpr e)
  {
    // We add special use-def and def-def edges as well as regular
    // data edges for calls.  These are only added for dependences
    // caused by aliases.

    if (displayMayUse) {
      Enumeration<MayUse> eu = e.getMayUse();
      while (eu.hasMoreElements()) {
        MayUse mayUse = eu.nextElement();
        visitMayUse(mayUse);
        addMayUseEdge(e, mayUse);
      }
      Enumeration<MayDef> ed = e.getMayDef();
      while (ed.hasMoreElements()) {
        MayDef mayDef = ed.nextElement();
        visitMayDef(mayDef);
        addMayDefEdge(e, mayDef);
      }
    }
    visitExpr(e);
  }

  public void visitChord(Chord c)
  {
    // Don't display CFG links if may-use/ use-def links are
    // displayed.  Otherwise, it's just to complicated to view.

    if (!(displayMayUse || displayDefUse))
      addCfgEdges(c);
  }

  public void visitLoopHeaderChord(LoopHeaderChord c)
  {
    // Don't display CFG links if may-use/ use-def links are displayed.
    // Otherwise, it's just too complicated to view.

    if (displayMayUse || displayDefUse)
      return;

    LoopHeaderChord plh = c.getParent();
    da.addEdge(plh, c, DColor.BLACK, DEdge.DOTTED, "child loop");
    addCfgEdges(c);
  }

  public void visitIfThenElseChord(IfThenElseChord c)
  {
    if (displayMayUse || displayDefUse)
      return;

    Chord t = c.getTrueCfgEdge();
    Chord f = c.getFalseCfgEdge();

    if (t != null)
      addTrueCfgEdge(c, t);

    if (f != null)
      addFalseCfgEdge(c, f);
  }
}
