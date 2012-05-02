package scale.clef.decl;



import scale.common.Vector;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents the declaration of an exception.
 * <p>
 * $Id: ExceptionDecl.java,v 1.27 2007-03-21 13:31:50 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ExceptionDecl extends Declaration 
{
  /**
   * The type parameter is the type of the parameter to the exception.
   * This reuses the type field because the exception has no type.
   */
  public ExceptionDecl(String name, Type type)
  {
    super(name, type);
  }

  public void visit(Predicate p)
  {
    p.visitExceptionDecl(this);
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name) 
  {
    return new ExceptionDecl(name, getType());
  }

  public final boolean isExceptionDecl()
  {
    return true;
  }

  public final ExceptionDecl returnExceptionDecl()
  {
    return this;
  }
}
