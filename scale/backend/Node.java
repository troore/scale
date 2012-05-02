package scale.backend;

import scale.common.*;

/**
 * This is an abstract class which represents a node in a graph.
 * <p>
 * $Id: Node.java,v 1.10 2007-10-31 16:39:16 bmaher Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public abstract class Node extends Root implements DisplayNode
{  
  protected Vector<Node> successors;   // The successors of the block.
  protected Vector<Node> predecessors; // The predecessors of the block. 
  protected int    color;            
  protected int    label;            // A unique identifier for this block.
  protected int    tag;              // A tag associated with the block; used by optimizations. 
  
  /**
   * The default constructor.
   */
  public Node(int label)
  {
    this.successors   = new Vector<Node>();
    this.predecessors = new Vector<Node>(); 
    this.color        = 0;
    this.tag          = 0;
    this.label        = label;
  }
  
  /**
   * Add the predecessors of this block to the stack if they haven't been visited before.
   */
  public final void pushInEdges(Stack<Node> wl)
  {
    int l = predecessors.size();
    for (int i = 0; i < l; i++) {
      Node node = predecessors.elementAt(i);
      if (!node.visited()) {
        wl.push(node);
        node.setVisited();
      }
    }
  }
  
  /**
   * Add the predecessors of this block to the stack if they haven't been visited before.
   * @param wl the stack
   * @param visited the set of visited nodes
   */
  public final void pushInEdges(Stack<Node> wl, HashSet<Node> visited)
  {
    int l = predecessors.size();
    for (int i = 0; i < l; i++) {
      Node node = predecessors.elementAt(i);
      if (!visited.contains(node)) {
        wl.push(node);
        visited.add(node);
      }
    }
  }
  
  /**
   * Add the successors of this block to the stack if they haven't been visited before.
   */
  public final void pushOutEdges(Stack<Node> wl)
  {
    int l = successors.size();
    for (int i = 0; i < l; i++) {
      Node node = successors.elementAt(i);
      if (!node.visited()) {
        wl.push(node);
        node.setVisited();
      }
    }
  }
  
  /**
   * Add the successors of this block to the stack if they haven't been visited before.
   * @param wl the stack
   * @param visited the set of visited nodes
   */
  public final void pushOutEdges(Stack<Node> wl, HashSet<Object> visited)
  {
    int l = successors.size();
    for (int i = 0; i < l; i++) {
      Node node = successors.elementAt(i);
      if (!visited.contains(node)) {
        wl.push(node);
        visited.add(node);
      }
    }
  }
  
  /**
   * Return the number of in-coming edges.
   */
  public final int numInEdges()
  {
    return predecessors.size();
  }
  
  /**
   * Return the number of out-going edges.
   */
  public final int numOutEdges()
  {
    return successors.size();
  }
  
  /**
   * Return the i-th predecessor node.
   * Caution must be used when using this method while adding or removing nodes from the PFG.
   */
  public final Node getInEdge(int i)
  {
    return predecessors.elementAt(i);
  }

  /**
   * Return the i-th successor node.
   * Caution must be used when using this method while adding or removing nodes from the PFG.
   */
  public final Node getOutEdge(int i)
  {
    return successors.elementAt(i);
  }
  
  /**
   * Return an array containing all predecessors.
   */
  public final Vector<Node> getInEdges()
  {
    Vector<Node> copy = new Vector<Node>();
    copy.addVectors(predecessors);
    return copy;
  }
  
  /**
   * Return an array containing all successors.
   */
  public final Vector<Node> getOutEdges()
  {
    Vector<Node> copy = new Vector<Node>();
    copy.addVectors(successors);
    return copy;
  }
  
  /**
   * Return the index of the specified in-coming CFG edge.
   * Return -1 if it's not an edge.
   */
  public final int indexOfInEdge(Node in)
  {
    return predecessors.indexOf(in);
  }
  
  /**
   * Return the index of the specified out-going CFG edge.
   * Return -1 if it's not an edge.
   */
  public final int indexOfOutEdge(Node out)
  {
    return successors.indexOf(out);
  }
  
  /**
   * Replace the existing incoming edge with a new edge. 
   */
  public final void replaceInEdge(Node oldEdge, Node newEdge)
  {
    if (predecessors.contains(newEdge)) {
      predecessors.remove(oldEdge);
      return;
    }
      
    predecessors.setElementAt(newEdge, indexOfInEdge(oldEdge));
  }
  
  /**
   * Replace the existing outgoing edge with a new edge. 
   */
  public final void replaceOutEdge(Node oldEdge, Node newEdge)
  {
    if (successors.contains(newEdge)) {
      successors.remove(oldEdge);
      return;
    }
    
    successors.setElementAt(newEdge, indexOfOutEdge(oldEdge));
  }
  
  /**
   * Add an incoming edge.
   */
  public final void addInEdge(Node in)
  {
    if (!predecessors.contains(in))
      predecessors.add(in);
  }
  
  /**
   * Add an out going edge.
   */
  public final void addOutEdge(Node out)
  {
    if (!successors.contains(out))
      successors.add(out);
  }
  
  /**
   * Delete an outgoing edge.
   */
  public final void deleteOutEdge(Node out)
  {
    successors.remove(out);
  }
  
  /**
   * Delete an incoming edge.
   */
  public final void deleteInEdge(Node in)
  {
    predecessors.remove(in);
  }
  
  /**
   * Remove this node from the graph.
   */
  public final void unlink()
  {
    int il = numInEdges();
    for (int i = il - 1; i > -1; i--) {
      Node in = getInEdge(i);
      in.deleteOutEdge(this);
      this.deleteInEdge(in);
    }
    
    int ol = numOutEdges();
    for (int i = ol - 1; i > -1; i--) {
      Node out = getOutEdge(i);
      out.deleteInEdge(this);
      this.deleteOutEdge(out);
    }
   
    predecessors = null;
    successors   = null;
  }
  
  /**
   * Return the unique identifier for this block.
   */
  public final int getLabel()
  {
    return label;
  }
    
  /**
   * Set the unique identifier for this block.
   */
  public final void setLabel(int label)
  {
    this.label = label;
  }
   
  /**
   * Set the tag for the block. 
   */
  public final void setTag(int tag)
  {
    this.tag = tag;
  }
    
  /**
   * Get the tag for the block.
   */
  public final int getTag()
  {
    return tag;
  }
    
  /**
   * Mark that the block has been visited.
   */
  protected abstract void setVisited();

  /**
   * Return true if the block has been visited.
   */
  public abstract boolean visited(); 
  
  /**
   * The next unique color for traversing the graph.
   * <br>
   * This method is not static in order to support traversing graphs, 
   * where each node in the graph is itself a graph made up of nodes.
   */
  protected abstract void nextVisit();
}
