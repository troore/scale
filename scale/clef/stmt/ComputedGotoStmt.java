package scale.clef.stmt;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class represents the Fortran 77 computed goto statement.
 * <p>
 * $Id: ComputedGotoStmt.java,v 1.22 2006-03-31 23:29:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ComputedGotoStmt extends MultiBranchStmt
{
  public ComputedGotoStmt(Expression e)
  {
    this(e, new Vector<LabelDecl>());
  }

  /**
   * @param expr is the selection value
   * @param labels is the set of statement labels.
   */
  public ComputedGotoStmt(Expression expr, Vector<LabelDecl> labels)
  {
    super(expr, labels);
  }

  public void visit(Predicate p)
  {
    p.visitComputedGotoStmt(this);
  }
}
