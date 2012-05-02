package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the Fortran 77 assigned goto statement.
 * <p>
 * $Id: AssignedGotoStmt.java,v 1.21 2005-02-07 21:28:07 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class AssignedGotoStmt extends MultiBranchStmt 
{
  public AssignedGotoStmt(Expression e)
  {
    this(e, new Vector<LabelDecl>());
  }

  public AssignedGotoStmt(Expression e, Vector<LabelDecl> l)
  {
    super(e, l);
  }

  public void visit(Predicate p)
  {
    p.visitAssignedGotoStmt(this);
  }
}
