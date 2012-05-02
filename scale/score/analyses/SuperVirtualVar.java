package scale.score.analyses;

import scale.alias.steensgaard.ECR;

import scale.common.*;
import scale.clef.decl.Declaration;

/**
 * A SuperVirtualVar contains subset virtual virtual variables.
 * <p.
  * $Id: SuperVirtualVar.java,v 1.13 2005-03-24 13:57:16 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Used in category alias analysis.
 */
public final class SuperVirtualVar extends VirtualVar
{
  /** 
   * - a vector of the subset virtual variables that this variable encompasses
   */
   private Vector<VirtualVar> subsets;

  /**
   * - the name that will be used to create the subsets
   */
   private String name;

  public SuperVirtualVar(String name)
  {
   this(name, new Vector<VirtualVar>());
  }

  /**
   * Create a new super virtual variable with given name and subsets list
   */
  public SuperVirtualVar(String name, Vector<VirtualVar> subsets)
  {
    super(name + ".0", null);
    this.name    = name;
    this.subsets = subsets;
  }

  /**
   * Create a {@link scale.score.analyses.SubVirtualVar SubVirtualVar}
   * from this SuperVirtualVar.
   */
  public SubVirtualVar createSubset(ECR ecr)
  {
    StringBuffer buf = new StringBuffer(name);
    buf.append('.');
    buf.append(subsets.size() + 1);
    SubVirtualVar sub = new SubVirtualVar(buf.toString(), ecr, this);
    subsets.addElement(sub);
    return sub;
  }

  /**
   * Return the number of subsets of the super virtual variable.
   */
  public final int numSubsets()
  {
    return subsets.size();
  }

  /**
   * Return the specified subset variable.
   */
  public final VirtualVar getSubset(int i)
  {
    return subsets.elementAt(i);
  }

  /**
   * Return the superset virtual variable.
   */
  public final VirtualVar getSuperset()
  {
    return this;
  }

  /**
   * Return true if vv is in the subset.
   */
  public boolean inSubsets(VirtualVar vv)
  {
    return subsets.contains(vv);
  }

  /**
   * Make a copy of this declaration using a new name.
   */
  public Declaration copy(String name)
  {
    Vector<VirtualVar> v  = new Vector<VirtualVar>(subsets.size());
    for (VirtualVar vv : subsets)
      v.add(vv);

    return new SuperVirtualVar(name, v);
  }

  /**
   * Return the specified AST child of this node.
   */
  public scale.clef.Node getChild(int i)
  {
    if (i == 0)
      return getValue();
    return (scale.clef.Node) subsets.elementAt(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    if (subsets == null)
      return 1;
    return 1 + subsets.size();
  }
}





