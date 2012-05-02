package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the repeat-while construct.
 * <p>
 * $Id: RepeatWhileLoopStmt.java,v 1.20 2005-02-07 21:28:10 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class RepeatWhileLoopStmt extends TestLoopStmt
{
  public RepeatWhileLoopStmt(Statement stmt, Expression exp)
  {
    super(stmt, exp);
  }

  public void visit(Predicate p)
  {
    p.visitRepeatWhileLoopStmt(this);
  }
}
