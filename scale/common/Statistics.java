package scale.common;

import java.lang.Class;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Iterator;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class registers statistics that are gathered by different classes.
 * <p>
 * $Id: Statistics.java,v 1.35 2007-08-27 18:37:51 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A statistic is a scalar value kept by class.  The name of the
 * statistic is used to invoke a static method that returns the
 * statistic for that class.
 * <p>
 * The status level is used to determine whether or not the "created"
 * statistics should be collected.  A level of 1 causes statistics to
 * be collected.  A level of 2 collects the "created" statistic as
 * well.
 */

public class Statistics
{
  private static java.util.Hashtable<String, java.util.HashSet<Class>> map;
  private static java.util.Hashtable<String, java.util.Hashtable<Class, RecordSnap>> snaps;
  private static int                 statusLevel = 0; // The level at which status messages are printed.
  private static String              forWhat     = "???"; // A name to use to identify statistical output.
  private static long                currentTime = System.currentTimeMillis(); // The beginning time in milliseconds.

  /**
   * Set the status level.  The status level control the amount of
   * status information that is printed.
   * @param level a positive integer control the status level
   * @param tag to specify for whom the statistic was generated
   */
  public static void setStatusLevel(int level, String tag)
  {
    statusLevel = level;
    forWhat = tag;
    Msg.reportInfo(Msg.MSG_Stat_level_is_s,
                   null,
                   0,
                   0,
                   Integer.toString(statusLevel));
  }

  /**
   * Print out the statistics if this status level is selected.
   * @param level the status level for this status
   */
  public static void reportStatistics(int level, String file)
  {
    if (statusLevel < level)
      return;

    PrintStream ps = System.out;
    if ((file != null) && (file.length() > 0)) {
      try {
        FileOutputStream fos = new FileOutputStream(file, true);
        ps = new PrintStream(fos);
      } catch(IOException ex) {
        ex.printStackTrace();
      }
    }

    Statistics.print(ps, forWhat);
    ps.flush();
    if (ps != System.out)
      ps.close();
  }

  /**
   * Print out the message if this status level is selected.
   * @param msg is the status message
   * @param text1 is additional information
   * @param text2 is additional information
   * @param level is the status level for this status
   * @see Msg
   */
  public static void reportStatus(int msg, String text1, String text2, int level)
  {
    if (statusLevel < level)
      return;

    if (Msg.reportInfo && (statusLevel >= level + 1)) {
      long ct = System.currentTimeMillis() - currentTime;
      Msg.reportInfo(Msg.MSG_Elapsed_time_s, null, 0, 0, String.valueOf(ct));
      currentTime = System.currentTimeMillis();
    }

    Msg.reportInfo(msg, null, 0, 0, text1, text2);
  }

  /**
   * Print out the message if this status level is selected.
   * @param msg is the status message
   * @param text1 is additional information
   * @param level is the status level for this status
   * @see Msg
   */
  public static void reportStatus(int msg, String text1, int level)
  {
    reportStatus(msg, text1, null, level);
  }

  /**
   * Print out the message is the status level is one.
   * @param msg is the status message
   * @see Msg
   */
  public static void reportStatus(int msg)
  {
    reportStatus(msg, null, 1);
  }

  /**
   * Print out the message is the status level is one.
   * @param msg is the status message
   * @param level is the status level for this status
   * @see Msg
   */
  public static void reportStatus(int msg, int level)
  {
    reportStatus(msg, null, level);
  }

  /**
   * Print out the message is the status level is one.
   * @param msg is the status message
   * @param text is additional information
   * @see Msg
   */
  public static void reportStatus(int msg, String text)
  {
    reportStatus(msg, text, 1);
  }

  /**
   * Get the status level.  The status level control the amount of
   * status information that is printed.
   * @param level the status level
   * @return true if at level level or greater
   */
  public static boolean status(int level)
  {
    return statusLevel >= level;
  }

  /**
   * Register a class as keeping a list of statistics.
   * @param stats is a list of the names of the statistics
   * @param cn is the class name.
   */
  public static void register(String cn, String[] stats)
  {
    Class c = null;

    try {
      c = Class.forName(cn);
    } catch(java.lang.Exception ex) {
      System.out.println("** Statistic " + cn + " not found.");
      return;
    }

    for (int i = 0; i < stats.length; i++)
      register(c, stats[i]);
  }

  /**
   * Register a class as keeping a statistic.
   * @param stat is the name of the statistic
   * @param cn is the class name.
   */
  public static void register(String cn, String stat)
  {
    Class c = null;

    try {
      c = Class.forName(cn);
    } catch(java.lang.Exception ex) {
      System.out.println("** Statistic " + cn + " not found.");
      return;
    }

    register(c, stat);
  }

  private static void register(Class c, String stat)
  {
    if ("created".equals(stat) && (statusLevel <= 1))
      return;

    if (map == null)
      map = new java.util.Hashtable<String, java.util.HashSet<Class>> (213, 0.75f);

    java.util.HashSet<Class> s = map.get(stat);
    if (s == null) {
      s = new java.util.HashSet<Class>(213, 0.75f);
      map.put(stat, s);
    }
    s.add(c);
  }

  private static class RecordSnap
  {
    public int    count;
    public int    total;
    public int    max;
    public String maxName;

    public RecordSnap(int value, String snName)
    {
      this.total   = value;
      this.max     = value;
      this.maxName = snName;
      this.count   = 1;
    }

    public void addSnap(int value, String snName)
    {
      int diff = value - total;
      total = value;
      count++;

      if (diff > max) {
        max = diff;
        maxName = snName;
      }
    }
  }

  /**
   * Take a snapshot of the statistics.
   * @param snName a name to associate with this snapshot
   */
  public static void snapshot(String snName)
  {
    if (map == null)
      return;

    if (snaps == null)
      snaps = new java.util.Hashtable<String, java.util.Hashtable<Class, RecordSnap>>(213, 0.75f);

    Enumeration<String> es = map.keys();
    while (es.hasMoreElements()) {
      String                   stat = es.nextElement();
      java.util.HashSet<Class> s    = map.get(stat);

      if (s == null)
        continue;

      Iterator<Class> e = s.iterator();
      while (e.hasNext()) {
        Class<?> c = e.next();
        try {
          Method m   = c.getMethod(stat);
          Object st  = m.invoke(null);

          if (!(st instanceof Integer))
            continue;

          int value = ((Integer) st).intValue();
          if (value == 0)
            continue;

          java.util.Hashtable<Class, RecordSnap> cs = snaps.get(stat);
          if (cs == null) {
            cs = new java.util.Hashtable<Class, RecordSnap>(213, 0.75f);
            snaps.put(stat, cs);
          }
          RecordSnap rs = cs.get(c);
          if (rs == null) {
            rs = new Statistics.RecordSnap(value, snName);
            cs.put(c, rs);
          } else {
            rs.addSnap(value, snName);
          }
        } catch(java.lang.Exception ex) {
        }
      }
    }
  }

  /**
   * Format a statistic.  A statistic consists of four parts:
   * <ol>
   * <li> the name of the class reporting the statistic,
   * <li> the name of the statistic,
   * <li> the value of the statistic, and
   * <li> a tag to be pre-pended.
   * </ol>
   * @param className is the name of the class reporting the statistic
   * @param stat is the name of the statistic
   * @param value is the value of the statistic
   * @param forWhat is the tag
   * @return the formatted string
   */
  public static String formatStat(String className,
                                  String stat,
                                  Object value,
                                  String forWhat)
  {
    String num = value.toString();
    int    l   = num.length();

    if ((l == 0) || (num.equals("0")))
      return "";

    StringBuffer buf = new StringBuffer("(stat, ");
    buf.append(forWhat);
    buf.append(", ");

    while (l < 11) {
      buf.append(' ');
      l++;
    }

    buf.append(num);
    buf.append(", ");

    buf.append(stat);
    l = stat.length();
    while (l < 16) {
      buf.append(' ');
      l++;
    }

    buf.append(", ");
    buf.append(className);
    buf.append(")");
    return buf.toString();
  }

  /**
   * Format a statistic.  A statistic consists of four parts:
   * <ol>
   * <li> the name of the class reporting the statistic,
   * <li> the name of the statistic,
   * <li> the value of the statistic, and
   * <li> a tag to be pre-pended.
   * </ol>
   * @param className is the name of the class reporting the statistic
   * @param stat is the name of the statistic
   * @param value is the value of the statistic
   * @param forWhat is the tag
   * @return the formatted string
   */
  public static String formatStat(String className,
                                  String stat,
                                  int    value,
                                  String forWhat)
  {
    String num = Integer.toString(value);
    int    l   = num.length();

    StringBuffer buf = new StringBuffer("(stat, ");
    buf.append(forWhat);
    buf.append(", ");

    while (l < 11) {
      buf.append(' ');
      l++;
    }

    buf.append(num);
    buf.append(", ");

    buf.append(stat);
    l = stat.length();
    while (l < 16) {
      buf.append(' ');
      l++;
    }

    buf.append(", ");
    buf.append(className);
    buf.append(")");
    return buf.toString();
  }

  /**
   * Print out the values of the statistic specified for each class.
   * @param out is the PrintStream for the output
   * @param forWhat is a string that is prepended to the statistics
   * printed
   */
  public static void print(java.io.PrintStream out, String forWhat)
  {
    if (map == null)
      return;

    Vector<String>      ord = new Vector<String>(40);
    Enumeration<String> es  = map.keys();
    while (es.hasMoreElements()) {
      String                   stat = es.nextElement();
      java.util.HashSet<Class> s    = map.get(stat);

      if (s == null)
        continue;

      Iterator<Class> e = s.iterator();
      while (e.hasNext()) {
        Class<?> c = e.next();
        try {
          Method m   = c.getMethod(stat);
          Object st  = m.invoke(null);
          String str = formatStat(c.getName(), stat, st, forWhat);

          if (str.length() == 0)
            continue;

          int on = ord.size();
          for (int j = 0; j < on; j++) {
            if (str.compareTo(ord.elementAt(j)) <= 0) {
              ord.insertElementAt(str, j);
              str = null;
              break;
            }
          }
          if (str != null)
            ord.addElement(str);
        } catch(java.lang.Exception ex) {
        }
      }
    }

    if (ord.size() > 0) {
      int on = ord.size();
      for (int j = 0; j < on; j++)
        out.println(ord.elementAt(j));
    }

    /* Print out the values of the statistic snapshots. */

    if (snaps == null)
      return;

    Enumeration<String> ess = snaps.keys();
    while (ess.hasMoreElements()) {
      String                                 stat = ess.nextElement();
      java.util.Hashtable<Class, RecordSnap> s    = snaps.get(stat);

      if (s == null)
        continue;

      Enumeration<Class> e = s.keys();
      while (e.hasMoreElements()) {
        Class      c  = e.nextElement();
        RecordSnap rs = s.get(c);
        out.print(formatStat(c.getName(), stat + ".max", rs.max, forWhat));
        out.print("  // ");
        out.println(rs.maxName);
        out.println(formatStat(c.getName(), stat + ".avg", rs.total / rs.count, forWhat));
      }
    }
  }

  /**
   * Clean up for profiling statistics.
   */
  public static void cleanup()
  {
    snaps = null;
    map   = null;
  }
}
