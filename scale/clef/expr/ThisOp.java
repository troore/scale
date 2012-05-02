package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * A class which represents the use of the <tt>this</tt> keyword.
 * <p>
 * $Id: ThisOp.java,v 1.26 2007-08-28 17:58:21 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ThisOp extends Expression
{
  public ThisOp(Type type)
  {
    super(type);
  }

  public void visit(Predicate p)
  {
    p.visitThisOp(this);
  }

  public void getDeclList(AbstractCollection varList)
  {
  }
}
