package scale.score.chords;

import scale.common.*;
import scale.score.expr.Expr;
import scale.score.Predicate;

/** 
 * This class represents return statements.
 * <p>
 * $Id: ReturnChord.java,v 1.17 2005-02-07 21:28:36 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ReturnChord extends LeaveChord
{
  public ReturnChord()
  {
    super();
  }

  /**
   * @param routineValue is the expression specifying the routines value
   */
  public ReturnChord(Expr routineValue)
  {
    super(routineValue);
  }

  public void visit(Predicate p)
  {
    p.visitReturnChord(this);
  }
}

