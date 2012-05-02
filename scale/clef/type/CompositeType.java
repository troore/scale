package scale.clef.type;

import scale.common.*;
import scale.clef.*;

/**
 * This is the abstract class for types that are composed of multiple
 * instances of other types such as arrays and structures.
 * <p>
 * $Id: CompositeType.java,v 1.29 2007-10-04 19:58:08 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
*/

public abstract class CompositeType extends Type 
{
  public CompositeType()
  {
  }

  public void visit(Predicate p)
  {
    p.visitCompositeType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitCompositeType(this);
  }

  /**
   * Return true if type represents a composite type.
   */
  public boolean isCompositeType()
  {
    return true;
  }

  public final CompositeType returnCompositeType()
  {
    return this;
  }
}
