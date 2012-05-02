package scale.common;

/** 
 * The base class for exceptions thrown by the Scale system.
 * <p>
 * $Id: Exception.java,v 1.14 2005-02-07 21:28:21 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Using this base class aids Scale in standardizing the appearance of
 * all its exception messages.
 * <p>
 * Exceptions are checked and must appear in the signature of the
 * method that throws them.
 * <p>
 * To define a new exception, a developer should define a new class
 * that inherits either from this class or one of its descendents and
 * write a constructor which can be used in a throw statement.
 */
public class Exception extends java.lang.Exception 
{
  private static final long serialVersionUID = 42L;

  /**
   * @param info - information about the exception
   */
  public Exception(String info)
  {
   super(info);
  }

  /**
   * Displays the exception and the java execution stack.
   */
  public void handler()
  {
    System.out.println(getMessage());
    printStackTrace();
  }
}

