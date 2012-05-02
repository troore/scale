package scale.clef.type;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;

/**
 * A raise represents an exception that <IT>may</IT> be thrown by a procedure.
 * <p>
 * $Id: Raise.java,v 1.26 2005-02-07 21:28:15 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A vector of Raises is used in ProcedureType to represent what
 * exceptions a procedure may throw.  Three kinds of Raises vectors exist:
 * <UL>
 * <LI> <B>The Raises vector is empty:</B> The procedure may not raise
 * any exceptions.
 * <LI> <B>The Raises vector has a single instance of the @see Raise
 * class:</B> The procedure may raise <IT>any</IT> exception.
 * <LI> <B>The Raises vector has instances of subclasses of @see Raise.
 * </UL>
 * @see ProcedureType
 */

public class Raise extends Node 
{
  public Raise()
  {
  }

  public void visit(Predicate p)
  {
    p.visitRaise(this);
  }
}
