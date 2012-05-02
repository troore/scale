package scale.clef.decl;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents the declaration of a member of an enumeration.
 * <p>
 * $Id: EnumElementDecl.java,v 1.24 2007-03-21 13:31:50 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class EnumElementDecl extends ValueDecl
{
  public EnumElementDecl(String name, Type type, Expression value)
  {
    super(name, type, value);
  }

  public EnumElementDecl(String name, Type type)
  {
    this(name, type, null);
  }

  public void visit(Predicate p)
  {
    p.visitEnumElementDecl(this);
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name) 
  {
    return new EnumElementDecl(name, getType(), getValue());
  }

  /**
   * Return true.
   */
  public boolean isConst()
  {
    return true;
  }

  public final boolean isEnumElementDecl()
  {
    return true;
  }

  public final EnumElementDecl returnEnumElementDecl()
  {
    return this;
  }
}
