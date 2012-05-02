package scale.clef.expr;

/**
 * This enum specifies the different transcendental functions - sin,
 * cos, etc.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public enum TransFtn
{
  /**
   * The sqrt() function.
   */
  Sqrt("sqrt", "sqrt"),
  /**
   * The exp() function.
   */
  Exp("exp", "exp"),
  /**
   * The log() function.
   */
  Log("log", "log"),
  /**
   * The log10() function.
   */
  Log10("log10", "log10"),
  /**
   * The sin() function.
   */
  Sin("sin", "sin"),
  /**
   * The cos() function.
   */
  Cos("cos", "cos"),
  /**
   * The tan() function.
   */
  Tan("tan", "tan"),
  /**
   * The asin() function.
   */
  Asin("asin", "asin"),
  /**
   * The acos() function.
   */
  Acos("acos", "acos"),
  /**
   * The atan() function.
   */
  Atan("atan", "atan"),
  /**
   * The sinh() function.
   */
  Sinh("sinh", "sinh"),
  /**
   * The cosh() function.
   */
  Cosh("cosh", "cosh"),
  /**
   * The tanh() function.
   */
  Tanh("", ""),
  /**
   * The conjg() function.
   */
  Conjg("conjg", "conjg"),
  /**
   * The alloca() function.
   */
  Alloca("_scale_alloca", "alloca"),
  /**
   * The builtin_return_address function.
   */
  ReturnAddress("_scale_return_address", "return_address"),
  /**
   * The builtin_frame_address() function.
   */
  FrameAddress("_scale_frame_address", "frame_address"),
  /**
   * The dealloca function.  This function is used to reset the stack
   * pointer when an <code>alloca</code>ed variable is no longer
   * needed.  Used by Fortran 90 for intermediate allocatble arrays.
   */
  Dealloca("_scale_dealloca", "dealloca");

  private String sName;
  private String cName;

  private TransFtn(String sName, String cName)
  {
    this.sName = sName;
    this.cName = cName;
  }

  public String sName()
  {
    return sName;
  }

  public String cName()
  {
    return cName;
  }
}
