package scale.clef.type;

import scale.common.*;
import scale.clef.*;

/**
 * This class represents the boolean type.
 * <p>
 * $Id: BooleanType.java,v 1.35 2007-03-21 13:31:55 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class BooleanType extends NumericType 
{
  /**
   * There is only one instance of a boolean type.
   */
  public static final BooleanType type = new BooleanType();  /* There is only one boolean type. */

  private BooleanType()
  {
    super();
  }

  public final boolean isBooleanType()
  {
    return true;
  }

  public final BooleanType returnBooleanType()
  {
    return this;
  }

  public String toString()
  {
    return "<bool>";
  }

  public String toStringSpecial()
  {
    return "";
  }

  public void visit(Predicate p)
  {
    p.visitBooleanType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitBooleanType(this);
  }

  /**
   * Return true if the types are equivalent.
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    return (tc == type);
  }

  public int bitSize()
  {
    return 1; 
  }
}
