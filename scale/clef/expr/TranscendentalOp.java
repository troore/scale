package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents the monadic intrinsic functions.
 * <p>
 * $Id: TranscendentalOp.java,v 1.20 2007-03-21 13:31:54 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If would have been better to name this class the BuiltInOperation
 * class.  It represents those complex operations which it may be
 * possible to implement on the target architecture with a simple
 * sequence of instructions.  An example is the square root function
 * which some architectures provide as a single instruction.
 */

public class TranscendentalOp extends MonadicOp 
{
  private TransFtn ftn; // ftn is the transcendental function.

  public TranscendentalOp(Type type, Expression e, TransFtn ftn)
  {
    super(type, e);
    this.ftn = ftn;
  }

  /**
   * Return a value indicating the transcendental function represented.
   */
  public TransFtn getFtn()
  {
    return ftn;
  }

  /**
   * @return short description of current node.
   */
  public String getDisplayLabel()
  {
    return ftn.sName();
  }

  public void visit(Predicate p)
  {
    p.visitTranscendentalOp(this);
  }

  /**
   * Return the constant value of the expression.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return scale.common.Lattice.Bot;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(ftn);
    return buf.toString();
  }
}
