package scale.score;

import java.io.*;
import java.util.Set;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.lang.Exception;
import java.lang.Runtime;

import scale.common.*;
import scale.callGraph.*;

import scale.score.*;
import scale.score.chords.*;

/**
 * The CDG class builds the control dependence graph for the scribble
 * graph input.
 * <p>
 * $Id: CDG.java,v 1.30 2007-10-04 19:58:18 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The representation is a hash set to a vector of Chords. 
 * <ul>
 * <li>The hash key is the branch chord (i.e., the <tt>last</tt> chord
 * in a basic block).
 * <li>The vector entries are the <tt>first</tt> chords of the basic
 * blocks dependent on the branch point.
 * </ul>
 * This representation is useful for branch prediction analysis
 * but may be better if the hash keys are the <tt>first</tt> chord
 * in a basic block if class is made part of Scale proper.
 * <p>
 * The reverse representation is a hash of the <tt>first</tt> chords
 * to a vector of the controlling branch points (yes, there 
 * can be more than one), again, each of which is the <tt>last</tt> 
 * chord in the basic block with the branch point.
 */
public class CDG
{
  private Scribble  scribble;
  private Hashtable<Chord, Vector<Chord>> forwardCDGtable;
  private Hashtable<Chord, HashMap<Chord, Boolean>> reverseCDGtable;

  /**
   * Calculates the CDG from the scribble graph.
   * @param scribble is the Scribble graph representing a single routine
   */
  public CDG(Scribble scribble)
  {
    this.scribble        = scribble;
    this.forwardCDGtable = new Hashtable<Chord, Vector<Chord>>();
    this.reverseCDGtable = new Hashtable<Chord, HashMap<Chord, Boolean>>();

    createCDG();
    
    if (Debug.debug(2))
      dumpCDG();
  }

  /**
   * Calculate the CDG of the scribble graph. The information is
   * represented by a two Hashtables of Vectors. One table for forward
   * flow information and the other for the reverse flow.
   * <p>
   * <b>Note - this method uses {@link
   * scale.score.chords.Chord#nextVisit nextVisit()}.</b>
   */
  private void createCDG()
  {
    // Must add CFG edge between the special begin chord of the scribble
    // graph and its end chord. To do this we insert a decision node after
    // the begin node that also points to the exit node.

    SequentialChord start   = scribble.getBegin();
    Chord           end     = scribble.getEnd();
    Chord           oldnext = start.getNextChord();
    IfThenElseChord special = new IfThenElseChord(null, oldnext, end);
    Stack<Chord>    wl      = WorkArea.<Chord>getStack("createCDG");

    start.changeOutCfgEdge(oldnext, special);

    // Since we've changed the scribble graph, clear any previous calculations.

    scribble.recomputeDominators();

    DominanceFrontier postdomfrontier = scribble.getPostDominanceFrontier();
    Domination        postdom         = scribble.getPostDomination();

    // Traverse the CFG and create the CDG information.  Also skip
    // over the start chord since we really want the special chord to
    // act as the start chord.

    wl.push(special);
    Chord.nextVisit();
    special.setVisited();

    while (!wl.empty()) {
      Chord s = wl.pop();
      s.pushOutCfgEdges(wl);

      // Work only with basic blocks, so skip if chord does not start a block.

      if (!s.isFirstInBasicBlock())
        continue;

      // Process chord s. de is list of nodes in s's frontier.
      Iterator<Chord> de = postdomfrontier.getDominanceFrontier(s);
      
      Chord ss = s;

      // Remap reference to special to start.
      if (s == special)
        s = start;

      HashMap<Chord, Boolean> vv = reverseCDGtable.get(s);

      while (de.hasNext()) {
        Chord x = de.next();

        // Here we ignore chords referencing 'start' and replace the reference
        // to 'special' with 'start'. Doing this here saves a messy traversal
        // of the CDG tables to do the swap later.
        //
        // Another method recommended by Jim Burrill is to directly
        // manipulate the post-dominator info by removing the start chord
        // from all sets and adding it to the exit node's pd set. I (SGD) 
        // am unsure if there are any other ramifications in doing this. 
        // If there are none then it would be an even simpler method.

        if (x == start)
          continue;

        if (x == special)
          x = start;

        // Get current vector of nodes with arcs from x in the CDG.
        // Add y to set if not already there.

        Vector<Chord> v = forwardCDGtable.get(x);
        if (v == null) {
          v = new Vector<Chord>();
          forwardCDGtable.put(x, v);
        }

        if (!v.contains(ss))
          v.addElement(ss);

        // Add opposite arc for quick CDG parent lookup.

        if (vv == null) {
          vv = new HashMap<Chord, Boolean>();
          reverseCDGtable.put(s, vv);
        }
        
        if (!vv.containsKey(x))
          vv.put(x, null);
        
        // Add the edge (true or false) that y is control dependent on.
      
        if (x instanceof IfThenElseChord) {
          IfThenElseChord ifc = (IfThenElseChord) x;
          if (ifc.getTrueCfgEdge() == ss)
            vv.put(x, Boolean.TRUE);
          else if (ifc.getFalseCfgEdge() == ss)
            vv.put(x, Boolean.FALSE);
          else {
            Vector<Chord> ipd = postdom.getIterativeDomination(ss);
            if (ipd.contains(ifc.getTrueCfgEdge()))
              vv.put(x, Boolean.TRUE);
            else if (ipd.contains(ifc.getFalseCfgEdge()))
              vv.put(x, Boolean.FALSE);
            else 
              throw new scale.common.InternalError("Error determining control dependent edge.");
          }
        } 
      }
    }

    WorkArea.<Chord>returnStack(wl);

    // Make the begin and end chord's control dependent on start.  
    // The algorithm above does not handle this.

    HashMap<Chord, Boolean> map = new HashMap<Chord, Boolean>();
    map.put(start, null);
    reverseCDGtable.put(end, map);
    reverseCDGtable.put(start, map);  
    
    // Remove the 'special' node from the CFG now that we have the CDG

    start.changeOutCfgEdge(special, oldnext);
    end.deleteInCfgEdge(special);
    oldnext.deleteInCfgEdge(special);
    special.deleteOutCfgEdges();
    special.expungeFromCfg();

    scribble.recomputeDominators();
  }

  /**
   * Returns a map from the child CFG node to the Chord(s) upon which
   * the child is control dependent.
   * <p>
   * The parent node is the key in the map and the condition
   * (true, false or null) is the value.  A value of null should only
   * be associated with the {@link scale.score.chords.BeginChord BeginChord}.
   * <p>

   * It is legal for the map to contain the {@link
   * scale.score.chords.BeginChord BeginChord} instance along with other
   * CFG nodes.  To check for control independence, the caller should
   * check there is only one parent and that it is the BeginChord or
   * for null.
   * @param child the Chord whose parent is returned.
   */
  public HashMap<Chord, Boolean> getParents(Chord child)
  {
    Chord childBB = child.firstInBasicBlock();     // Ensure lookup is done based on the basic blocks
    return reverseCDGtable.get(childBB); // Find the vector of parents for the childBB.
  }

  /**
   * Return a list of CFG nodes that are immediately dependent on
   * this node.  Note, if this CFG node is not a control flow node a
   * <code>null</code> is returned.
   */
  public Vector<Chord> getDependents(Chord parent)
  {
    return forwardCDGtable.get(parent);
  }

  /**
   * CDG lookups are done based on the first basic block, if a client
   * changes the basic blocks, they must update the CDG.  This keeps
   * the CDG tables small.
   * @param oldKey old chord that was the first basic block
   * @param newKey new chord that is the first basic block
   */
  public void updateKey(Chord oldKey, Chord newKey)
  {
    HashMap<Chord, Boolean> vv = reverseCDGtable.get(oldKey);
    reverseCDGtable.remove(oldKey);
    reverseCDGtable.put(newKey, vv);
  }
  
  /**
   * Output the CDG for debugging.
   */
  public void dumpCDG()
  {
    String fname = scribble.getRoutineDecl().getName();
    System.out.println("\n*** Reverse CDG Table (" + fname + ") ***");
    dumpCDGTable(reverseCDGtable);
  }
  
  private void dumpCDGTable(Hashtable<Chord, HashMap<Chord, Boolean>> table)
  {
    Enumeration<Chord> e = table.keys();
    while (e.hasMoreElements()) {
      Chord key = e.nextElement();
      HashMap<Chord, Boolean> element = table.get(key);
      Set<Chord>              keys    = element.keySet();

      System.out.println("Key    : " + key);
      for (Chord c : keys) {
        Boolean bool = element.get(c); 
        System.out.println("Element: " + c);
        System.out.println("Cond   : " + ((bool == null) ? "always" : bool.toString()));
      }
    }
  }
}
