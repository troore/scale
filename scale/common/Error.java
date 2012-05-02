package scale.common;

/**
 * This class is the base class for all errors.
 * <p>
 * $Id: Error.java,v 1.15 2005-02-07 21:28:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Using this base class aids Scale in standardizing the appearance of
 * all its error messages.
 * <p>
 * Errors are unchecked exceptions and do not have to appear in the 
 * signature of the method that throws them.
 * <p>
 * To define a new error, a developer should define a new class that
 * inherits either from this class or one of its descendents and write
 * a constructor which can be used in a throw statement.
 */
public class Error extends java.lang.Error 
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info - information about the error
   */
  public Error(String info)
  {
    super(info);
  }

  /**
   * Display the error and the java execution stack trace.
   */
  public void handler()
  {
    System.out.println(getMessage());
    printStackTrace();
  }
}

