package scale.common;

/** 
 * The base class for run-time exceptions thrown by the Scale system.  
 * <p>
 * $Id: RuntimeException.java,v 1.15 2007-10-04 19:58:11 burrill Exp $
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
 * Run-time exceptions are not checked and do not have to appear in the 
 * signature of the method that throws them.
 * <p>
 * To define a new exception, a developer should define a new class
 * that inherits either from this class or one of its descendents and
 * write a constructor which can be used in a throw statement.
 */
public class RuntimeException extends java.lang.RuntimeException 
{
  private static final long serialVersionUID = 42L;

  public RuntimeException(String info)
  {
    super("** " + info);
  }

  public void handler()
  {
    System.out.println(getMessage());
    printStackTrace();
  }
}

