package scale.score.chords;

import scale.common.*;
import scale.score.*;
import scale.score.pred.References;
import scale.score.expr.*;
import scale.clef.decl.Declaration;

/** 
 * This class represents nodes in a CFG.
 * <p>
 * $Id: Chord.java,v 1.137 2007-10-04 19:58:22 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The Chord class has derived classes which are dependent on the number
 * of out-going CFG edges.  Chord nodes have no out-going data edges.
 * This class handles the in-coming CFG edges.  The derived classes must
 * handle the out-going CFG edges.
 */

public abstract class Chord extends Note 
{
  private static int deletedCFGNodeCount = 0; // A count of nodes removed.
  private static int deadCFGNodeCount    = 0; // A count of nodes removed because they were un-reachable.
  private static int nullCFGNodeCount    = 0; // A count of NullChord nodes removed.
  private static int gotoCFGNodeCount    = 0; // A count of GotoChord nodes removed.
  private static int nextColor           = 0; // A unique value to use in CFG spanning algorithms.

  private static final String[] stats = {
    "deletedCFGNodes",
    "deadCFGNodes",
    "nullCFGNodes",
    "gotoCFGNodes"};

  static
  {
    Statistics.register("scale.score.chords.Chord", stats);
  }

  /**
   * Return the current number of dead nodes removed.
   */
  public static int deletedCFGNodes()
  {
    return deadCFGNodeCount;
  }

  /**
   * Return the current number of dead nodes removed because they were
   * not reachable.
   */
  public static int deadCFGNodes()
  {
    return deadCFGNodeCount;
  }

  /**
   * Return the current number of {@link scale.score.chords.NullChord
   * null nodes} removed.
   */
  public static int nullCFGNodes()
  {
    return nullCFGNodeCount;
  }

  /**
   * Return the current number of {@link scale.score.chords.GotoChord
   * GotoChord} nodes removed.
   */
  public static int gotoCFGNodes()
  {
    return gotoCFGNodeCount;
  }

  /**
   * Set up for a new CFG traversal - that is, use the next color value.
   * This color is used to mark CFG nodes in CFG spanning algorithms.
   * @see #setVisited
   * @see #visited
   * @see #nextVisit
   * @see #pushOutCfgEdges
   * @see #pushInCfgEdges
   */
  public static void nextVisit()
  {
    nextColor++;
  }

  /**
   * This field holds the in-coming Cfg (Chord) edge or edges (vector)..
   */
  private Object inCfgEdges = null; 

  /**
   * This field is for the use of various algorithms so that they can
   * label a CFG node with an integer value.  The Domination class
   * uses it in computing dominators.
   */
  private int label;
  /**
   * The color value - used in traversing the CFG.
   */
  private int color;
  /**
   * The source line number.
   */
  protected int lineNumber;
  
  protected Chord()
  {
    super();
    this.color      =  0;
    this.lineNumber = -1;
  }

  /**
   * Associate an integer value with a CFG node.  The label is for the
   * use of various algorithms so that they can label a CFG node with
   * an integer value.  The {@link scale.score.Domination Domination}
   * class uses it in computing dominators.
   */
  public final void setLabel(int label)
  {
    this.label = label;
  }

  /**
   * Return the integer associated with a CFG node.
   */
  public final int getLabel()
  {
    return label;
  }

  /**
   * Return true if this is a {@link
   * scale.score.chords.SequentialChord SequentialChord} instance.
   */
  public boolean isSequential()
  {
    return false;
  }

  /**
   * Return true if this CFG node may have multiple out-going CFG edges.
   */
  public boolean isBranch()
  {
    return false;
  }

  /**
   * Return true if this CFG node is a {@link LoopHeaderChord
   * LoopHeaderChord} instance.
   */
  public boolean isLoopHeader()
  {
    return false;
  }

  /**
   * Return true if this CFG node is a {@link LoopPreHeaderChord
   * LoopPreHeaderChord} instance.
   */
  public boolean isLoopPreHeader()
  {
    return false;
  }

  /**
   * Return true if this CFG node is a {@link LoopExitChord
   * LoopExitChord} instance.
   */
  public boolean isLoopExit()
  {
    return false;
  }

  /**
   * Return true if this CFG node is a {@link LoopTailChord
   * LoopTailChord} instance.
   */
  public boolean isLoopTail()
  {
    return false;
  }

  /**
   * Return true if this CFG node is a {@link PhiExprChord
   * PhiExprChord} instance.
   */
  public boolean isPhiExpr()
  {
    return false;
  }

  /**
   * Return true if this is CFG node was added for the convenience of
   * the compiler and does not correspond to actual source code in the
   * user program.
   */
  public boolean isSpecial()
  {
    return false;
  }

  /**
   * Return true if this is a marker CFG node.
   */
  public boolean isMarker()
  {
    return false;
  }

  /**
   * Return true if this is an {@link ExprChord ExprChord} instance.
   */
  public boolean isExprChord()
  {
    return false;
  }

  /**
   * Associate the current color value with a CFG node.
   * The color is for the use of CFG spanning algorithms.
   * @see #nextVisit
   * @see #visited
   * @see #pushOutCfgEdges
   * @see #pushInCfgEdges
   */
  public final void setVisited()
  {
    this.color = nextColor;
  }

  /**
   * Return true if this CFG node has been visited during the current
   * visit (i.e., is the current color).
   * @see #nextVisit
   * @see #setVisited
   * @see #pushOutCfgEdges
   * @see #pushInCfgEdges
   */
  public final boolean visited()
  {
    return (color == nextColor);
  }
  
  /**
   * Return true if the parents of this CFG node have been visited.
   */
  public boolean parentsVisited()
  {
    if (inCfgEdges == null)
      return true;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord>  v = (Vector<Chord>) inCfgEdges;
      int            n = v.size();
      
      for (int i = 0; i < n; i++) {
        Chord parent = v.elementAt(i);
        if (!parent.visited())
          return false;
      }
      return true;
    }
    
    Chord parent = (Chord) inCfgEdges;
    return parent.visited();
  }
  
  /**
   * Return true if all predecessor CFG nodes are in the finished set.
   * @param finished is the set to test. 
   */
  public boolean parentsFinished(HashSet<Chord> finished)
  {
    if (inCfgEdges == null)
      return true;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord>  v = (Vector<Chord>) inCfgEdges;
      int            n = v.size();
      
      for (int i = 0; i < n; i++) {
        Chord parent = v.elementAt(i);
        if (!finished.contains(parent))
          return false;
      }
      return true;
    }
    
    Chord parent = (Chord) inCfgEdges;
    return finished.contains(parent);
  }
  
  /**
   * Add the successors of this CFG node to the stack.
   */
  public abstract void pushAllOutCfgEdges(Stack<Chord> wl);

  /**
   * Add the successors of this CFG node to the stack if they haven't
   * been visited before.
   * @see scale.score.chords.Chord#nextVisit
   * @see scale.score.chords.Chord#setVisited
   * @see scale.score.chords.Chord#visited
   */
  public abstract void pushOutCfgEdges(Stack<Chord> wl);

  /**
   * Add the successors of this CFG node to the stack if they haven't
   * been visited before.
   * @param done is the set of visited CFG nodes
   */
  public abstract void pushOutCfgEdges(Stack<Chord> wl, HashSet<Chord> done);
  
  /**
   * Add the successors of this CFG node to the stack if they haven't
   * been visited, and all their parents have.  Traversing the graph
   * in this fashion yields a topological sort of the graph (with loop
   * back edges removed).
   */
  public abstract void pushSortedOutCfgEdges(Stack<Chord> wl);

  /**
   * Add the successors of this CFG node to the stack if they haven't
   * been visited, and all their parents have.  Traversing the graph
   * in this fashion yields a topological sort of the graph (with loop
   * back edges removed).
   * @param finished is the set of finished nodes.
   */
  public abstract void pushSortedOutCfgEdges(Stack<Chord> wl, HashSet<Chord> finished);
  
  /**
   * Add the predecessors of this CFG node to the stack if they haven't
   * been visited before.
   * @see scale.score.chords.Chord#nextVisit
   * @see scale.score.chords.Chord#setVisited
   * @see scale.score.chords.Chord#visited
   */
  public final void pushInCfgEdges(Stack<Chord> wl)
  {
    if (inCfgEdges == null)
      return;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           n = v.size();
      for (int i = 0; i < n; i++) {
        Chord parent = v.elementAt(i);
        if (!parent.visited()) {
          parent.setVisited();
          wl.push(parent);
        }
      }
      return;
    }

    Chord parent = (Chord) inCfgEdges;
    if (!parent.visited()) {
      parent.setVisited();
      wl.push(parent);
    }
  }

  /**
   * Add the predecessors of this CFG node to the stack if they haven't
   * been visited before.
   * @param done is the set of visited CFG nodes
   */
  public final void pushInCfgEdges(Stack<Chord> wl, HashSet<Chord> done)
  {
    if (inCfgEdges == null)
      return;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           n = v.size();
      for (int i = 0; i < n; i++) {
        Chord parent = v.elementAt(i);
        if (done.add(parent))
          wl.push(parent);
      }
      return;
    }

    Chord parent = (Chord) inCfgEdges;
    if (done.add(parent))
      wl.push(parent);
  }

  /**
   * Add the predecessors of this CFG node to the stack.
   */
  public final void pushAllInCfgEdges(Stack<Chord> wl)
  {
    if (inCfgEdges == null)
      return;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           n = v.size();
      for (int i = 0; i < n; i++) {
        Chord parent = v.elementAt(i);
        wl.push(parent);
       }
      return;
    }

    Chord parent = (Chord) inCfgEdges;
    wl.push(parent);
  }

  /**
   * Place this CFG node on the stack when all its predecessor nodes
   * have been visited.  Assign it a label value and increment the
   * label value by one.  Note that CFG nodes past a loop exit may be
   * pushed, and given lower label values, even though CFG nodes in
   * the loop have not been visited.  If this matters, you may want to
   * use {@link scale.score.chords.LoopHeaderChord#labelCFGLoopOrder
   * LoopHeaderChord.labelCFGLoopOrder()}.
   * @return the new label value
   * @see scale.score.Scribble#labelCFG
   */
  public final int pushChordWhenReady(Stack<Chord> wl, int label)
  {
    if (visited())
      return label;

    if (this.isLoopHeader()) {
      setLabel(label++);
      wl.push(this);
      return label;
    }

    // Don't visit child until all its parents are visited.

    if (inCfgEdges != null) {
      if (inCfgEdges instanceof Vector) {
        @SuppressWarnings("unchecked")
        Vector<Chord> v = (Vector<Chord>) inCfgEdges;
        int           l = v.size();
        for (int j = 0; j < l; j++) {
          Chord in = v.elementAt(j);
          if (!in.visited())
            return label;
        }
      } else if (!((Chord) inCfgEdges).visited())
        return label;
    }

    setLabel(label++);
    wl.push(this);
    return label;
  }

  /**
   * Place this CFG node on the stack when all its predecessor nodes
   * have been visited.  Note that CFG nodes past a loop exit may be
   * pushed even though CFG nodes in the loop have not been visited.
   * @see scale.score.Scribble#linearize
   */
  public final void pushChordWhenReady(Stack<Chord> wl)
  {
    if (visited())
      return;

    if (this.isLoopHeader()) {
      wl.push(this);
      return;
    }

    // Don't visit child until all its parents are visited.

    if (inCfgEdges != null) {
      if (inCfgEdges instanceof Vector) {
        @SuppressWarnings("unchecked")
        Vector<Chord> v = (Vector<Chord>) inCfgEdges;
        int           l = v.size();
        for (int j = 0; j < l; j++) {
          Chord in = v.elementAt(j);
          if (!in.visited())
            return;
        }
      } else if (!((Chord) inCfgEdges).visited())
        return;
    }

    wl.push(this);
  }

  /**
   * Return a <code>String</code> suitable for labeling this node in a
   * graphical display.  This method should be over-ridden as it
   * simplay returns the class name.
   */
  public String getDisplayLabel()
  {
    String mnemonic = toStringClass();
    if (mnemonic.endsWith("Chord"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 5);
    return mnemonic;
  }

  /**
   * Return a <code>String</code> specifying the color to use for
   * coloring this node in a graphical display.  This method should be
   * over-ridden as it simplay returns the color lightblue.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTBLUE;
  }

  /**
   * Return a <code>String</code> specifying a shape to use when
   * drawing this node in a graphical display.  This method should be
   * over-ridden as it simplay returns the shape "ellipse".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.ELLIPSE;
  }

  /**
   * Make a copy of this CFG node with the same out-going CFG edges.
   * The validity of the CFG graph is maintained.
   * @return a copy of this node
   */
  public abstract Chord copy();

  /**
   * Remove myself from the CFG.  This only works for nodes with only
   * one out CFG edge or nodes with no in CFG edges.  The validity of
   * the CFG graph is maintained.
   * <p>
   * Each node in the CFG has a link back to the nodes that point to
   * it.  When a node is removed using {@link #removeFromCfg
   * removeFromCfg ()}, the nodes that point to the node being removed
   * are changed to point to the successor of the node being removed.
   * It can't do this if there is more than one successor.  It this
   * way, the node is removed but the CFG graph remains valid.
   * <p>
   * The {@link #expungeFromCfg expungeFromCfg()} method does not link
   * the parent nodes to the successor node.  It breaks the CFG graph.
   * It is up to the calling method to insure that the CFG graph is
   * valid.
   * @see #expungeFromCfg
   */
  public final void removeFromCfg()
  {
    unlinkChord();
    extractFromCfg();
    deletedCFGNodeCount++;
  }

  /**
   * Break any un-needed links from a CFG node that is being deleted.
   */
  public void unlinkChord()
  {
    deleteInDataEdges();
  }

  /**
   * Remove myself from the CFG but preserve me for future use.  This
   * only works for nodes with only one out CFG edge or nodes with no
   * in CFG edges.  The validity of the CFG graph is maintained.
   * <p>

   * Each node in the CFG has a link back to the nodes that point to
   * it.  When a node is removed using {@link #removeFromCfg
   * removeFromCfg()}, the nodes that point to the node being removed
   * are changed to point to the successor of the node being removed.
   * It can't do this if there is more than one successor.  It this
   * way, the node is removed but the CFG graph remains valid.
   * <p>
   * The <{@link #expungeFromCfg expungeFromCfg()} method does not
   * link the parent nodes to the successor node.  It breaks the CFG
   * graph.  It is up to the calling method to insure that the CFG
   * graph is valid.
   * @see #expungeFromCfg
   */
  public final void extractFromCfg()
  {
    if ((numOutCfgEdges() > 1)  && (numInCfgEdges() > 0))
      throw new InvalidMutationError("More than one out-going CFG edge " + this);

    Chord out         = null;
    Chord[] outEdges  = getOutCfgEdgeArray();

    if (outEdges.length > 0)
      out = outEdges[0];

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           n = v.size();
      for (int i = 0; i < n; i++) {
        Chord in = v.elementAt(i);
        if (in != null)
          in.replaceOutCfgEdge(this, out);
        if (out != null)
          out.replaceInCfgEdge(this, in);
      }
    } else if (inCfgEdges != null) {
      Chord in = (Chord) inCfgEdges;
      if (in != null)
        in.replaceOutCfgEdge(this, out);
      if (out != null)
        out.replaceInCfgEdge(this, in);
    } else {
      for (int i = 0; i < outEdges.length; i++) {
        if (outEdges[i] == null)
          continue;
        outEdges[i].replaceInCfgEdge(this, null);
      }
    }

    deleteOutCfgEdges();
    inCfgEdges = null;
  }

  /**
   * Remove any CFG nodes that do not have an incoming CFG edge and
   * any un-needed nodes from the CFG.  It doesn't remove loops that
   * are dead code.  Un-needed nodes include {@link NullChord
   * NullChord} and {@link GotoChord GotoChord} nodes.
   * @param chords a list of all statement nodes to be checked.
   */
  public static void removeDeadCode(Stack<Chord> chords)
  {
    Stack<Chord> wl = chords;

    while (wl != null) {
      Stack<Chord> ns = null;

      while (!wl.isEmpty()) {
        Chord s = wl.pop();

        if (s instanceof GotoChord) {
          s.removeFromCfg();
          gotoCFGNodeCount++;
          continue;
        }

        if (s instanceof NullChord) {
          s.removeFromCfg();
          nullCFGNodeCount++;
          continue;
        }

        if (s.numInCfgEdges() != 0)
          continue;
 
        if (ns == null)
          ns = new Stack<Chord>();

        if (s.isLoopTail()) {
          // If the loop tail is not connected then we don't actually
          // need the loop at all so get rid of the other special
          // nodes for the loop.  The result is normal, straight-line
          // code.  This happens when there are constructs such as:

          //   while (1) {
          //     if (...) break;
          //     ...
          //     break;
          //  }

          // Some people actually code like this.  Can you believe it?

          LoopHeaderChord lh = (LoopHeaderChord) s.getNextChord();
          if (lh != null) {
            LoopPreHeaderChord lph = lh.getPreHeader();
            LoopInitChord      li  = lh.getLoopInit();

            int l = lh.numLoopExits();
            for (int i = 0; i < l; i++) {
              LoopExitChord le = lh.getLoopExit(i);
              le.removeFromCfg();
              deadCFGNodeCount++;
              wl.remove(le);
            }

            lh.removeFromCfg();
            wl.remove(lh);

            lph.removeFromCfg();
            wl.remove(lph);

            s.removeFromCfg();
            ns.remove(s);

            deadCFGNodeCount += 3;

            if (li != null) {
              li.removeFromCfg();
              wl.remove(lph);
              deadCFGNodeCount++;
            }

            continue;
          }
        }

        s.pushAllOutCfgEdges(ns);

        s.removeFromCfg();
        deadCFGNodeCount++;
      }
      wl = ns;
    }
  }

  /**
   * Replace the existing out-going CFG edge with a new edge.
   * Maintaining the validity of the CFG graph is the responsibility
   * of the caller.
   * @param oldChord is the old edge
   * @param newChord is the new edge
   */
  public abstract void replaceOutCfgEdge(Chord oldChord, Chord newChord);

  /**
   * Replace the existing in-coming CFG edge with a new edge.  If the
   * old edge is not found, the new edge is added.  Maintaining the
   * validity of the CFG graph is the responsibility of the caller.
   * @param oldChord is the old edge
   * @param newChord is the new edge
   */
  @SuppressWarnings("unchecked")
  public final void replaceInCfgEdge(Chord oldChord, Chord newChord)
  {
    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           n = v.size();
      for (int i = 0; i < n; i++) {
        Chord in = v.elementAt(i);
        if (in == oldChord) {
          if (newChord != null)
            v.setElementAt(newChord, i);
          else {
            v.removeElementAt(i);
            int l = v.size();
            if (l == 0)
              inCfgEdges = null;
            else if (l == 1)
              inCfgEdges = v.elementAt(0);
          }
          return;
        }
      }
    } else if (inCfgEdges != null) {
      Chord in = (Chord) inCfgEdges;
      if (in == oldChord) {
        inCfgEdges = newChord;
        return;
      }
    }

    // Old edge not found - add new edge

    if (newChord != null)
      addInCfgEdge(newChord);
  }

  /**
   * Remove myself from the CFG.  Maintaining the validity of the CFG
   * is the responsibility of the caller.
   * @see #removeFromCfg
   */
  public final void expungeFromCfg()
  {
    Chord[] outEdges = getOutCfgEdgeArray();
    for (int i = 0; i < outEdges.length; i++) {
      Chord out = outEdges[i]; // only one for a null statement
      if (out != null)
        out.deleteInCfgEdge(this);
    }

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           n = v.size();
      for (int i = n - 1; i >= 0; i--) { // From end so we don't have to clone!
        Chord in = v.elementAt(i);
        in.changeOutCfgEdge(this, null);
      }
    } else if (inCfgEdges != null) {
      Chord in = (Chord) inCfgEdges;
      in.changeOutCfgEdge(this, null);
      inCfgEdges = null;
    }

    unlinkChord();

    deletedCFGNodeCount++;
  }

  /**
   * Insert a new node before me in the CFG.  It inherits all of my
   * in-coming Cfg edges.  The validity of the CFG is maintained.
   * @param newChord the node to be inserted.
   */
  public final void insertBeforeInCfg(scale.score.chords.SequentialChord newChord)
  {
    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> in = ((Vector<Chord>) inCfgEdges).clone(); // Necessary because original vector is changed by the changeOutCfgEdge method.
      int           n  = in.size();
      for (int i = 0; i < n; i++) {
        Chord p = in.elementAt(i);
        p.changeOutCfgEdge(this, newChord);
      }
    } else {
      Chord p = (Chord) inCfgEdges;
                     
      p.changeOutCfgEdge(this, newChord);
    }
    newChord.setTarget(this);
  }

  /**
   * Link child to parent if it's a {@link SequentialChord
   * SequentialChord} instance and not a {@link BranchChord
   * BranchChord} instance or {@link EndChord EndChord} instance.
   */
  public void linkTo(Chord child)
  {
  }

  /**
   * Insert a new node between me and my out-going CFG edges.  The new
   * node inherits all of my out-going CFG edges.  The validity of the
   * CFG is maintained.
   * @param newChord the node to be inserted.
   * @param outEdge is my out-going CFG edge
   */
  public final void insertAfterOutCfg(Chord newChord, Chord outEdge)
  {
    assert newChord.isSequential() : "The new chord must be sequential chord: " + newChord;

    // SequentialChord s = (SequentialChord) newChord;

    outEdge.replaceInCfgEdge(this, newChord);
    newChord.replaceOutCfgEdge(null, outEdge);
    this.replaceOutCfgEdge(outEdge, newChord);
    newChord.addInCfgEdge(this);
  }

  /**
   * Add an in-coming CFG edge.  Maintaining the validity of the CFG
   * is the responsibility of the caller.
   * @param node CFG node which is pointing to me
   */
  public void addInCfgEdge(Chord node)
  {
    assert (node != null) : "Cannot add null CFG edge.";

    if (inCfgEdges == null) {
      inCfgEdges = node;
      return;
    }

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      v.addElement(node);
      return;
    }

    Vector<Chord> v = new Vector<Chord>(2);
    v.addElement((Chord) inCfgEdges);
    v.addElement(node);
    inCfgEdges = v;
  }

  /**
   * Remove an in-coming CFG edge.  Maintaining the validity of the
   * CFG is the responsibility of the caller.
   * @param node specifies the in-coming CFG edge
   */
  public final void deleteInCfgEdge(Chord node)
  {
    if (node == null)
      return;

    if (inCfgEdges == node) {
      inCfgEdges = null;
      return;
    }

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      if (v.removeElement(node)) {
        int l = v.size();
        if (l == 1)
          inCfgEdges = v.elementAt(0);
        else if (l == 0)
          inCfgEdges = null;

        return;
      }
    }

    throw new InvalidMutationError("Not an incoming edge " + node + " -> " + this + ".");
  }

  /**
   * Use this method when you may be modifying an in-coming CFG edge
   * to this CFG node while iterating over the in-coming edges.
   * @return an enumeration of the in-coming CFG edges.
   */
  public final Chord[] getInCfgEdgeArray()
  {
    Chord[] array;
    if (inCfgEdges == null)
      array = new Chord[0];
    else if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           l = v.size();
      array = new Chord[l];
      for (int i = 0; i < l; i++)
        array[i] = v.elementAt(i);
    } else {
      array = new Chord[1];
      array[0] = (Chord) inCfgEdges;
    }
    return array;
  }

  /**
   * Return the predecessor node.  If there is more than one
   * predecessor CFG node, this method will return <code>null</code>.
   */
  public final Chord getInCfgEdge()
  {
    if (inCfgEdges == null)
      return null;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           l = v.size();

      if (l == 1)
        return v.elementAt(0);

      assert (l == 0) : "More than one predecessor " + this;
      return null;
    }

    return (Chord) inCfgEdges;
  }

  /**
   * Return the predecessor node.  If there is more than one
   * predecessor CFG node, this method will return one of them.  If
   * there is none, <code>null</code> is returned.
   */
  public final Chord getFirstInCfgEdge()
  {
    if (inCfgEdges == null)
      return null;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      return v.elementAt(0);
    }

    return (Chord) inCfgEdges;
  }

  /**
   * Return the i-th predecessor node.  Caution must be used when
   * using this method while adding or removinge nodes from the CFG.
   */
  public final Chord getInCfgEdge(int i)
  {
    assert (inCfgEdges != null) : "No i-th in CFG edge - " + i;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      return v.elementAt(i);
    }

    assert (i == 0) : "No i-th in CFG edge - " + i;

    return (Chord) inCfgEdges;
  }

  /**
   * Return the number of in-coming CFG edges.
   */
  @SuppressWarnings("unchecked")
  public final int numInCfgEdges()
  {
    if (inCfgEdges == null)
      return 0;
    if (inCfgEdges instanceof Vector)
      return ((Vector) inCfgEdges).size();
    return 1;
  }

  /**
   * Return the index of the specified in-coming CFG edge.
   * Return -1 if it's not an edge.
   */
  public final int indexOfInCfgEdge(Chord in)
  {
    if (in == inCfgEdges)
      return 0;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      return v.indexOf(in);
    }

    return -1;
  }

  /**
   * Return the index of the nth occurrence of the specified in-coming
   * CFG edge.  This uses 0-origin indexing.  Return -1 if it's not an
   * edge.
   */
  public final int nthIndexOfInCfgEdge(Chord in, int n)
  {
    if (in == inCfgEdges)
      return (0 == n) ? 0 : -1;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           l = v.size();
      for (int i = 0; i < l; i++)
        if (v.elementAt(i) == in) {
          if (n == 0)
            return i;
          n--;
        }
    }

    return -1;
  }

  /**
   * Return the number of duplicate in-coming CFG edges from the
   * specified node.
   */
  public int numOfInCfgEdge(Chord in)
  {
    if (in == inCfgEdges)
      return 1;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           l = v.size();
      int    c = 0;
      for (int i = 0; i < l; i++)
        if (v.elementAt(i) == in)
          c++;
      return c;
    }

    return 0;
  }

  /**
   * Change all my predecessors to point to another CFG node.
   */
  public final void changeParentOutCfgEdge(Chord newEdge)
  {
    if (inCfgEdges == null)
      return;

    if (inCfgEdges instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<Chord> v = (Vector<Chord>) inCfgEdges;
      int           l = v.size();
      for (int i = l - 1; i >= 0; i--)
        v.elementAt(i).changeOutCfgEdge(this, newEdge);
      return;
    }

    ((Chord) inCfgEdges).changeOutCfgEdge(this, newEdge);
  }

  /**
   * Return the first CFG node in this basic block.
   * @see #isFirstInBasicBlock
   * @see #isLastInBasicBlock
   * @see #lastInBasicBlock
   */
  public final Chord firstInBasicBlock()
  {
    Chord  fibb = this;
    Object in   = fibb.inCfgEdges;

    while (in != null) {
      if (in instanceof Vector) {
        @SuppressWarnings("unchecked")
        Vector<Chord> v = (Vector<Chord>) in;
        if (v.size() != 1)
          break;  // I have more than one in-coming CFG edge
        in = v.elementAt(0);
      }
      Chord parent = (Chord) in;
      if (parent.isLastInBasicBlock())
        break; // Parent has more than one out-going CFG edge
      fibb = parent;
      in = fibb.inCfgEdges;
    }
    return fibb;
  }

  /**
   * Return true if this CFG node is the start of a basic block.  A
   * basic block begins with a node that has more than one in-coming
   * CFG edge or whose predecessor has more than one out-going CFG
   * edge.
   * @see #firstInBasicBlock
   * @see #isLastInBasicBlock
   * @see #lastInBasicBlock
   */
  @SuppressWarnings("unchecked")
  public final boolean isFirstInBasicBlock()
  {
    if (inCfgEdges == null)
      return true;

    if (inCfgEdges instanceof Chord) {
      Chord in = (Chord) inCfgEdges;

      if (in.isBranch())
        return true;

      return (in.numOutCfgEdges() > 1);
    }

    assert (((Vector<Chord>) inCfgEdges).size() > 1) : "In-edge error " + this;

    return true;
  }

  /**
   * Return true if this is the last CFG node in this basic block.
   * @see #lastInBasicBlock
   * @see #isFirstInBasicBlock
   * @see #firstInBasicBlock
   */
  public abstract boolean isLastInBasicBlock();

  /**
   * Return the last CFG node in this basic block.
   * @see #isLastInBasicBlock
   * @see #isFirstInBasicBlock
   * @see #firstInBasicBlock
   */
  public final Chord lastInBasicBlock()
  {
    Chord c = this;
    while (!c.isLastInBasicBlock())
      c = c.getNextChord();
    return c;
  }

  /**
   * Return the next CFG node in the CFG.
   * If the there is more than one, return null.
   */
  public abstract Chord getNextChord();

  /**
   * Return the number of out-going CFG edges.
   */
  public abstract int numOutCfgEdges();

  /**
   * Return the specified out-going CFG edge.
   */
  public abstract Chord getOutCfgEdge(int i);

  /**
   * Use this method when you may be modifying an out-going CFG edge
   * from this CFG node while iterating over the out-going edges.
   * @return an array of all of the out-going CFG edges
   */
  public abstract Chord[] getOutCfgEdgeArray();

  /**
   * Change the out-going CFG edge indicated by the position to the
   * new edge.  The validity of the CFG is maintained.
   * @param oldEdge the out-going CFG edge to be changed
   * @param newEdge the new out-going CFG edge
   */
  public abstract void changeOutCfgEdge(Chord oldEdge, Chord newEdge);

  /**
   * Use this method when you may be modifying an in-coming data edge
   * to this expression while iterating over the in-coming edges.
   * @return an array of in-coming data edges.  
   */
  public Expr[] getInDataEdgeArray()
  {
    return new Expr[0];
  }

  /**
   * Return the specified in-coming data edge.
   */
  public scale.score.expr.Expr getInDataEdge(int i)
  {
    throw new scale.common.InternalError("No incoming data edge " + this);
  }

  /**
   * Return the number of in-coming data edges.
   */
  public int numInDataEdges()
  {
    return 0;
  }

  /**
   * Push all incoming data edges on the stack.
   */
  public void pushInDataEdges(Stack<Expr> wl)
  {
  }

  /** 
   * This method changes an incoming data edge to point to a new expression.
   * <p>
   * This method ensures that the node previously pointing to
   * this one is updated properly, as well as the node which will now
   * point to this node.
   * <p>
   * {@link scale.score.expr.Expr Expr} instances and CFG nodes have a
   * fixed number of incoming data edges with specific meaning applied
   * to each.
   * @param oldExpr is the expression to be replaced
   * @param newExpr is the new expression
   */
  public void changeInDataEdge(scale.score.expr.Expr oldExpr,
                               scale.score.expr.Expr newExpr)
  {
    throw new InvalidMutationError("No in-coming data edge " + this);
  }

  /**
   * This routine is needed because it is possible for more than one
   * out-going CFG edge from a CFG node to go the the same CFG node.
   * This method should be over-ridden by the derived classes for speed.
   * @param to is the target CFG node
   * @param skip ispecifies how many occurrances of <code>to</code> to skip
   * @return the index into the outgoing edges of this node that matches
   * the specified in-coming edge of the target node.
   */
  public abstract int indexOfOutCfgEdge(Chord to, int skip);

  /**
   * Remove all the in-coming data edges.
   */
  public abstract void deleteInDataEdges();

  /**
   * Remove all the out-going CFG edges.
   */
  public abstract void deleteOutCfgEdges();

  /**
   * Clear all the markers.  Each marker is associated with one
   * out-going CFG edge.
   */
  public abstract void clearEdgeMarkers();

  /**
   * Return the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   * @return true is the specified edge is marked
   */
  public abstract boolean edgeMarked(int edge);

  /**
   * Set the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   */
  public abstract void markEdge(int edge);

  /**
   * Clear the marker associated with the specified out-going CFG edge.
   * @param edge specifies the edge associated with the marker
   */
  public abstract void clearEdge(int edge);

  /**
   * If this CFG node results in a variable being given a new value,
   * return the {@link scale.score.expr.Expr Expr} instance
   * that specifies the variable.
   * @return a <code>null</code> or a {@link scale.score.expr.Expr
   * Expr} instance that defines a variable
   */
  public scale.score.expr.Expr getDefExpr()
  {
    return null;
  }

  /**
   * Return the source line number associated with this node or -1 if
   * not known.
   */
  public final int getSourceLineNumber()
  {
    return lineNumber;
  }

  /**
   * Set the source line number associated with this node.
   */
  public final void setSourceLineNumber(int lineNumber)
  {
    this.lineNumber = lineNumber;
  }

  /**
   * Copy the source line information.
   */
  public void copySourceLine(Chord from)
  {
    if (from != null)
      lineNumber = from.lineNumber;
  }

  /**
   * Determine the execution ordering of the two CFG nodes.
   * This routine returns either 0, 1, or -1 based on the following
   * conditions:
   * <ul>
   * <li> 0 if <i>this</i> and <i>n</i> are in the same CFG node.
   * <li> 1 if <i>this</i> occurs before <i>n</i>.
   * <li> -1 if <i>n</i> occurs before <i>this</i>.
   * </ul>
   * <p>
   * NOTE - this method will not perform correctly if the Chords have
   * not been labeled.
   * @param n the node to compare against.
   * @return 0, 1, or -1 based upon the lexical ordering.
   * @see scale.score.Scribble#labelCFG
   */
   public int executionOrder(Chord n) 
   {
     int lab1 = getLabel();
     int lab2 = n.getLabel();

     if (lab1 < lab2)
       return 1;
     if (lab1 > lab2)
       return -1;

     return 0;
   }
  
  /**
   * Return the {@link LoopHeaderChord LoopHeaderChord} object for the
   * loop that contains this node.  The method will return a
   * <code>null</code> if the CFG node is not connected.
   * <p>
   * For {@link LoopInitChord LoopInitChord} and {@link
   * LoopPreHeaderChord LoopPreHeaderChord} instances, this method
   * returns the loop header for the loop that they are in - not the
   * loop header for which they are the initial nodes.
   */
  public LoopHeaderChord getLoopHeader()
  {
    Object in = inCfgEdges;

    while (in != null) {
      if (in instanceof Vector) {
        @SuppressWarnings("unchecked")
        Vector<Chord> v = (Vector<Chord>) in;
        in = v.elementAt(0);
      }

      if (in instanceof LoopHeaderChord)
        return (LoopHeaderChord) in;

      if (in instanceof LoopExitChord)
        return ((LoopExitChord) in).getLoopHeader().getParent();

      if (in == this)
        break; // Must be an implicit loop.

      Chord pred = (Chord) in;
      in = pred.inCfgEdges;
    }

    return null;
  }

  /**
   * Return the {@link LoopHeaderChord LoopHeaderChord} object for the
   * loop that contains this node.  The method will return a
   * <code>null</code> if the CFG node is not connected.
   * <p>
   * For {@link LoopInitChord LoopInitChord} and {@link
   * LoopPreHeaderChord LoopPreHeaderChord} instances, this method
   * returns the loop header for the loop that they are in - not the
   * loop header for which they are the initial nodes.
   * @param limit is a limit on how many nodes are checked before we give up
   */
  public LoopHeaderChord getLoopHeader(int limit)
  {
    Object in = inCfgEdges;
    int cnt = 0;

    while (in != null) {
      if (in instanceof Vector) {
        @SuppressWarnings("unchecked")
        Vector<Chord> v = (Vector<Chord>) in;
        in = v.elementAt(0);
      }

      if (in instanceof LoopHeaderChord)
        return (LoopHeaderChord) in;

      if (in instanceof LoopExitChord)
        return ((LoopExitChord) in).getLoopHeader().getParent();

      if (in == this)
        break; // Must be an implicit loop.

      if (cnt++ > limit)
        break; // Must be an implicit loop.

      Chord pred = (Chord) in;
      in = pred.inCfgEdges;
    }

    return null;
  }

  public int getLoopNumber()
  {
    return getLoopHeader().getLoopNumber();
  }

  /**
   * Return a vector of all of the {@link PhiExprChord phi chords} in
   * the basic block starting at this node.
   */
  public Vector<PhiExprChord> findPhiChords()
  {
    Vector<PhiExprChord> chords = new Vector<PhiExprChord>();
    Chord                cur    = this;

    while (true) {
      Chord next = cur.getNextChord();
      
      if (cur.isPhiExpr())
        chords.addElement((PhiExprChord) cur);

      if (next == null)
        return chords;

      if (next.numInCfgEdges() > 1)
        return chords;

      cur = next;
    }
  }

  /**
   * Eliminate associated loop information.
   */
  public void loopClean()
  {
    int l = numInDataEdges();
    for (int i = 0; i < l; i++)
      getInDataEdge(i).loopClean();
  }

  /**
   * Return true if this node holds an expression that represents an
   * assignment statement.
   */
  public boolean isAssignChord()
  {
    return false;
  }

  /**
   * Return true if this CFG node is in the basic block specified by first.
   */
  public final boolean inBasicBlock(Chord first)
  {
    while (true) {
      if (first == this)
        return true;

      first = first.getNextChord();

      if (first == null)
        return false;

      if (first.numInCfgEdges() > 1)
        return false;
    }
  }

  /**
   * Return the {@link LoopExitChord LoopExitChord} instance, for the
   * specified loop, that is reachable from this CFG node.  Return
   * <code>null</code> if none is found.
   */
  public LoopExitChord findLoopExit(LoopHeaderChord header)
  {
    Chord ex = getNextChord();
    while (ex != null) {
      if (ex.isLoopExit()) {
        LoopExitChord ec = (LoopExitChord) ex;
                
        if (ec.getLoopHeader() == header) // If LoopExitChord for this loop.
          return ec;

        break;
      }
      ex = ex.getNextChord();
    }

    return null;
  }

  /**
   * Return the call expression or <code>null</code> if none.
   * @param ignorePure is true if pure function calls are to be ignored.
   */
  public scale.score.expr.CallExpr getCall(boolean ignorePure)
  {
    return null;
  }

  /**
   * Return a vector of all {@link scale.clef.decl.Declaration
   * declarations} referenced in this CFG node or <code>null</code>.
   */
  public abstract Vector<Declaration> getDeclList();

  /**
   * Return a vector of all {@link scale.score.expr.LoadExpr LoadExpr}
   * instances in this CFG node or <code>null</code>.
   */
  public abstract Vector<LoadExpr> getLoadExprList();

  /**
   * Return a vector of all {@link scale.score.expr.Expr Expr}
   * instances in this CFG node or <code>null</code>.
   */
  public abstract Vector<Expr> getExprList();
  /**
   * Replace all occurrances of a {@link scale.clef.decl.Declaration
   * Declaration} with another declaration.  Return true if a replace
   * occurred.
   */
  public abstract boolean replaceDecl(Declaration oldDecl, Declaration newDecl);
  /**
   * Remove any use - def links, may - use links, etc.
   */
  public abstract void removeUseDef();

  /**
   * Link a new CFG node that contains old links.
   * When a CFG node is copied, the copy has the same links as
   * the original node.  This method updates those links.
   * @param nm is a map from the old nodes to the new nodes.
   * @see scale.score.Scribble#linkSubgraph
   */
  public abstract void linkSubgraph(HashMap<Chord, Chord> nm);

  /**
   * Reorder the incoming CFG edges of a new CFG node to match the
   * order of the in-coming edges of the this CFG node.
   * @param nm is the mapping from old nodes to new nodes
   * @see scale.score.Scribble#linkSubgraph
   */
  @SuppressWarnings("unchecked")
  public void reorderInCfgEdgesOfCopy(HashMap<Chord, Chord> nm)
  {
    if (!(inCfgEdges instanceof Vector))
      return;

    @SuppressWarnings("unchecked")
    Vector<Chord> vo = (Vector<Chord>) inCfgEdges;
    int    l  = vo.size();
    if (l <= 1)
      return;

    Chord         n = nm.get(this);
    Vector<Chord> vn;
    Vector<Chord> xxx;
    if (n.inCfgEdges instanceof Vector) {
      vn = (Vector<Chord>) n.inCfgEdges;
      xxx = vn.clone();
      vn.clear();
    } else {
      vn = new Vector<Chord>(l);
      xxx = new Vector<Chord>(0);
    }

    for (int i = 0; i < l; i++) {
      Chord o = nm.get(vo.elementAt(i));
      assert (xxx.indexOf(o) >= 0) : "*** oops " + this;
      vn.addElement(o);
    }
  }

  /**
   * Record any variable {@link scale.score.pred.References
   * references} in this CFG node in the table of references.
   */
  public void recordRefs(References refs)
  {
  }

  /**
   * Remove any variable {@link scale.score.pred.References
   * references} in this CFG node from the table of references.
   */
  public void removeRefs(References refs)
  {
  }

  /**
   * Remove all {@link scale.score.expr.DualExpr DualExpr} instances
   * from the CFG.  Use the lower form.  This eliminates references to
   * variables that may no longer be needed.
   */
  public boolean removeDualExprs()
  {
    return false;
  }

  /**
   * Return a <code>String</code> containing additional information
   * about this CFG node.
   */
  public String toStringSpecial()
  {
    if (lineNumber < 0)
      return "";
    return "l:" + lineNumber + " ";
  }
}
