package scale.score.analyses;

import scale.common.*;
import scale.alias.steensgaard.ECR;
import scale.clef.type.VoidType;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;

/**
 * A class that represents a virtual variable which is used to handle
 * aliasing and indirect operations in SSA form.  
 * <p>
 * $Id: VirtualVar.java,v 1.28 2006-02-28 16:37:08 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A virtual variable is a pseudo-declaration.  We don't actually generate code for the 
 * declaration, but we treat it like a declaration in the representation.
 */
public class VirtualVar extends VariableDecl 
{
  private static int createdCount = 0; /* A count of all the instances of this class created. */

  static
  {
    Statistics.register("scale.score.analyses.VirtualVar", "created");
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }

 /**
   * A virtual variable represents a group of variables with similar alias
   * characteristics.  We represent the group of variables using Steensgaard's
   * ECR data structure.
   */
  protected ECR var;

  /**
   * Create a new virtual variable with a given name and associated ECR.
   * The virtual variable does not have a type.
   */
  public VirtualVar(String name, ECR v)
  {
    super(name, VoidType.type);
    this.var = v;
    createdCount++;
  }

  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer(super.getDisplayLabel());
    buf.append(" ECR ");
    if (var != null)
      buf.append(var.getID());
    return buf.toString();
  }

  /**
   * Return the superset virtual variable.
   */
  public VirtualVar getSuperset()
  {
    return this;
  }

  /**
   * Return true if this is a superset virtual variable.
   */
  public boolean isSuper()
  {
    return true;
  }
    
  /**
   * Return true if the declaration is a virtual variable.
   */
  public boolean isVirtual()
  {
    return true;
  }

  /**
   * Return true if vv is in the subset.
   */
  public boolean inSubsets(VirtualVar vv)
  {
    return (vv == this);
  }

  /**
   * Return the number of subsets of the super virtual variable.
   */
  public int numSubsets()
  {
    return 1;
  }

  /**
   * Return the specified subset variable.
   */
  public VirtualVar getSubset(int i)
  {
    return (i == 0) ? this : null;
  }

  /**
   * Return true if virtual variable param is equal to this, or this is a superset.
   */
  public boolean subsetEquiv(VirtualVar vv)
  {
    return  (vv == this) || ((isSuper() && inSubsets(vv)));
  }

  /**
   * Make a copy of this declaration using a new name.
   */
  public Declaration copy(String name)
  {
    return new VirtualVar(name, var);
  }

  public ECR getECR()
  {
    return var;
  }
}
