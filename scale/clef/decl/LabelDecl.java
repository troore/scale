package scale.clef.decl;

import scale.common.Vector;
import scale.clef.*;
import scale.clef.expr.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * This class represents a label in a program.
 * <p>
 * $Id: LabelDecl.java,v 1.24 2007-03-21 13:31:51 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
*/

public class LabelDecl extends Declaration 
{
  public LabelDecl(String name)
  {
    super(name);
  }

  public void visit(Predicate p)
  {
    p.visitLabelDecl(this);
  }

  /**
   * Return a copy of this Declaration but with a different name.
   */
  public Declaration copy(String name) 
  {
    return new LabelDecl(name);
  }

  public final boolean isLabelDecl()
  {
    return true;
  }

  public final LabelDecl returnLabelDecl()
  {
    return this;
  }
}
