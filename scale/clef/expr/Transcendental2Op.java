package scale.clef.expr;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/**
 * This class represents dyadic intrinsic functions.
 * <p>
 * $Id: Transcendental2Op.java,v 1.15 2006-09-11 19:07:17 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If would have been better to name this class the BuiltInOperation class.
 * It represents those complex operations which it may be possible to
 * implement on the target architecture with a simple sequence of instructions.
 * An example is the square root function which some architectures provide
 * as a single instruction.
 */

public class Transcendental2Op extends DyadicOp 
{
  /**
   * The atan(a,b) function.
   */
  public static final int cAtan2 = 0;
  /**
   * The sign(a,b) function.
   */
  public static final int cSign = 1;
  /**
   * The dim(a,b) function.
   */
  public static final int cDim = 2;

  /**
   * Map from function index to string.
   */
  public static final String[] trans2Ftn = {"atan2", "sign", "dim"};

  /**
   * ftn is the transcendental function.
   */
  private int ftn;

  public Transcendental2Op(Type type, Expression e1, Expression e2, int ftn)
  {
    super(type, e1, e2);
    this.ftn = ftn;
  }

  /**
   * Return a value indicating the transcendental function represented.
   */
  public int getFtn()
  {
    return ftn;
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    if (getCoreType().isIntegerType()) {
      switch (ftn) {
      case cSign: return "_scale_isign";
      case cDim: return "_scale_idim";
      }
    } else {
      switch (ftn) {
      case cAtan2: return "atan2";
      case cSign: return "_scale_dsign";
      case cDim: return "_scale_ddim";
      }
    }
    throw new scale.common.InternalError("Invalid transcendental " + trans2Ftn[ftn]);
  }

  public void visit(Predicate p)
  {
    p.visitTranscendental2Op(this);
  }

  /**
   * Return the constant value of the expression
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
    buf.append(trans2Ftn[ftn]);
    return buf.toString();
  }
}
