package scale.common;

/**
 * A class for aiding in debuging.
 * <p>
 * $Id: Debug.java,v 1.37 2007-01-04 16:58:27 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Use <code>Debug.trace(...)</code> to decide if debug trace
 * information should be printed.  The standard way of using this
 * class is to:
 * <ol>
 * <li>Put
 * <pre>
 * public static boolean classTrace = false;
 * </pre>
 * in the class that you wish to be able to trace.
 * <li>Put
 * <pre>
 * private boolean trace;
 * </pre>
 * in the class as well.
 * <li>Add the statement
 * <pre>
 *   trace = Debug.trace("routine name", classTrace, level);
 * </pre>
 * to the class constructor where the level is some intger.
 * Or,
 * <pre>
 *  this.trace = classTrace || Debug.debug(level);
 * </pre>
 * for classes that operate over more than one routine at a time,
 * <li>Use constructs such as
 * <pre>
 *  if (trace)
 *    System.out.println(...);
 * </pre>
 * to generate the trace information.
 * </ol>
 * <p>
 * Use <code>Debug.debug(level)</code> to decide if extra validity
 * tests should be performed or to print more detailed warnings, etc.
 * <p>
 * The debug level is meant for displaying general debugging
 * information.  A debug level of 1 causes validity checking at
 * various places of the compiler.  A level of 2 causes the output of
 * general information.  A level of 3 causes the output of some large
 * tables.
 */
public class Debug 
{
  /**
   * Debug level off.
   */
  public final static int OFF = 0;
  /**
   * Debug level minimum.
   */
  public final static int MINIMUM = 1;
  /**
   * Debug level between MINIMUM and FULL.
   */
  public final static int MEDIUM = 2;
  /**
   * Debug level FULL.
   */
  public final static int FULL = 3;
  /**
   * The debug level.  Controls the amount of debug information that
   * is printed.
   */
  private static int debugLevel = 0;
  
  /**
   * A selected name to be traced.
   */
  private static String reportName = null;

  private Debug()
  {
  }

  /**
   * Set the debug level.  The debug level control the amount of debug
   * level that is printed.
   * @param level a positive integer control the debug level
   */
  public static void setDebugLevel(int level)
  {
    debugLevel = (level > FULL ? FULL : level);
    Msg.reportInfo(Msg.MSG_Debug_level_is_s, null, 0, 0, Integer.toString(debugLevel));
  }

  /**
   * Set the report name.  
   * @param name is the name to be traced
   */
  public static void setReportName(String name)
  {
    reportName = name;
    Msg.reportInfo(Msg.MSG_Report_name_is_s, null, 0, 0, reportName);
  }

  /**
   * Get the report name.
   */
  public static String getReportName()
  {
    return reportName;
  }

  /**
   * Return the debug level value.
   */
  public static int getDebugLevel()
  {
    return debugLevel;
  }

  /**
   * Return true if the debug level is greater than or equal to the
   * given level.
   * @param level the debug level
   * @return true if at level level or greater
   */
  public static final boolean debug(int level)
  {
    return (debugLevel >= level);
  }      

  /**
   * Return true if
   * <ul>
   * <li>the report name is the same as the specified name and the
   * flag argument is true, or
   * <li>the debug level is greater than or equal to the given level.
   * </ul>
   * @param name is name to be traced
   * @param flag is the (class) trace flag
   * @param level is the debug level
   * @return true if tracing should be done
   */
  public static final boolean trace(String name, boolean flag, int level)
  {
    return ((debugLevel >= level) || (flag && (reportName != null) && reportName.equals(name)));
  }      

  /**
   * Print out a stack trace. This method is designed so that it can
   * be used with the <code>assert</code> Java statement.
   * @return true
   */
  public static boolean printStackTrace()
  {
    try {
      throw new InternalError("stack trace");
    } catch(InternalError ex) {
      ex.printStackTrace();
    }
    return true;
  }

  /**
   * Print out the specified stack trace. This method is designed so
   * that it can be used with the <code>assert</code> Java statement.
   * @param ex is the exception to be displayed
   * @return true
   */
  public static boolean printStackTrace(java.lang.Throwable ex)
  {
    ex.printStackTrace();
    return true;
  }

  /**
   * Print out the specified message. This method is designed so that
   * it can be used with the <code>assert</code> Java statement.
   * @param msg is the message
   * @return true
   */
  public static boolean printMessage(String msg)
  {
    System.out.println(msg);
    return true;
  }

  /**
   * Print out the specified message followed by the object. If the
   * object is <code>null</code> only the message is printed.  This
   * method is designed so that it can be used with the
   * <code>assert</code> Java statement.
   * @param msg is the message
   * @param o is the object
   * @return true
   */
  public static boolean printMessage(String msg, Object o)
  {
    System.out.print(msg);
    if (o != null)
      System.out.print(o);
    System.out.println("");
    return true;
  }

  /**
   * Print out the specified message followed by the object. If the
   * object is <code>null</code> only the message is printed.  This
   * method is designed so that it can be used with the
   * <code>assert</code> Java statement.
   * @param msg is the message
   * @param o is the object
   * @param indent specifies the number of spaces to output before the
   * message
   * @return true
   */
  public static boolean printMessage(String msg, Object o, int indent)
  {
    for (int i = 0; i < indent; i++)
      System.out.print("  ");
    System.out.print(msg);
    if (o != null)
      System.out.print(o);
    System.out.println("");
    return true;
  }

  /**
   * Return the amount of memory used in bytes.
   * This memory includes the class representations.
   * This method depends on the garbage collection algorithm.
   * Not all un-referenced objects are guarantteed to be
   * collected when the memory stats are obtained.
   */
  public static long trackMemory()
  {
    Runtime rt = Runtime.getRuntime();
    rt.gc();
    rt.runFinalization();
    rt.gc();
    return (rt.totalMemory() - rt.freeMemory());
  }

  /**
   * Return the string representation of a long integer value.  The
   * length of the string will be exactly <code>size</code> characters,
   * pre-appended with blanks as necessary.  If the value cannot be
   * represented in the space provided, a string of asterisks is
   * returned instead.
   * @param value is the value
   * @param size is the number of characters required.
   */
  public static String formatInt(long value, int size)
  {
    String s = Long.toString(value);
    int    l = s.length();
    if (l == size)
      return s;
    if (l > size)
      return "*********************************".substring(0, size);
    return ("             ".substring(0, size - l) + s);
  }
}
