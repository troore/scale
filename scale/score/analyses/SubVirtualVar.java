package scale.score.analyses;

import scale.alias.steensgaard.ECR;
import scale.clef.decl.Declaration;

/**
 * A class that represents a subset virtual variable.
 * <p>
 * $Id: SubVirtualVar.java,v 1.12 2005-03-24 13:57:16 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class SubVirtualVar extends VirtualVar 
{
  /**
   * - superset virtual variable
   */
  private SuperVirtualVar supvv;

  /**
   * Create a new virtual variable with a given name and associated ECR.
   * and pointer to the superset virtual variable
   */
  public SubVirtualVar(String n, ECR v, SuperVirtualVar supvv)
  {
    super(n, v);
    this.supvv = supvv;
  }
    
  /**
   * Return the superset virtual variable.
   */
  public VirtualVar getSuperset()
  {
    return supvv;
  }

  /**
   * Return true if this is a superset variable.
   */
  public boolean isSuper()
  {
    return false;
  }

  /**
   * Make a copy of this declaration using a new name.
   */
  public Declaration copy(String name)
  {
    return new SubVirtualVar(name, var, supvv);
  }
}
