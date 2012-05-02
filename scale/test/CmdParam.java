package scale.test;

import java.io.PrintStream;

import scale.common.*;

/**
 * This class provides standard processing of command line parameters.
 * <p>
 * $Id: CmdParam.java,v 1.43 2007-10-04 19:58:39 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class provides standard processing of command line parameters.
 * Command line parameters can be of the form
 * <dl>
 * <dt>string<dd>-s abcdefg
 * <dt>integer<dd>-l 9
 * <dt>real<dd>-prob 0.89
 * <dt>switch<dd>-w
 * <dt>list<dd>-i file1 file2 file3
 * </dl>
 * <ul>
 * <li>Command line parameters can be required or optional.
 * <li>Default values may be provided for optional parameters.
 * <li>Lists may be surrounded by parens.  Or, the list is terminated 
 * by the end of the parameters or by a - character.
 * <li>Parameters must be preceded by one or more '-' characters.
 * <li>The '-' character in command line parameters provided by the user
 * are replaced by the '_' character. For example,
 * <pre>
 *  -this-is-a-switch
 * </pre>
 * and
 * <pre>
 *  -this_is_a_switch
 * </pre>
 * are equivalent.
 * <li>A short form of the parameter name can be used if the name
 * contains multiple words; the short name form is the first character
 * of each word in the long parameter name.
 * <li>For switches, two forms are available:
 * <ol>
 * <li> <code>-switch</code> turns on the switch.
 * <li> <code>-no_switch</code> (negative form) turns off the switch.
 * </ol>
 * For the short parameter name form it would be:
 * <ol>
 * <li> <code>-s</code> turns on the switch.
 * <li> <code>-ns</code> (negative form) turns off the switch.
 * </ol>
 * No separate <code>CmdParam</code> instance is required for the
 * <b>negative</b> form.
 * </ul>
 */
public class CmdParam 
{
  /**
   * Parameter is a String.
   */
  public final static int STRING = 0;
  /**
   * Parameter is an integer.
   */
  public final static int INT = 1;
  /**
   * Parameter is a real.
   */
  public final static int REAL = 2;
  /**
   * Parameter is a switch.
   */
  public final static int SWITCH = 3;
  /**
   * Parameter is a list of strings.
   */
  public final static int LIST = 4;

  private static final String[] types = {"string", "int", "real", "switch", "list"};

  private static final CmdParam help  = new CmdParam("help", true, STRING, null, Msg.HLP_help);

  private String  name;         // Parameter name used for matching against command line parameters
  private String  shortName;    // Abreviated parameter name used for matching against command line parameters
  private Object  defaultValue; // Default value
  private Object  value;        // Value for parameter supplied by parameters
  private String  description;  // A description of the parameter
  private int     helpMsg;      // An index to the description of the parameter
  private int     type;         // Is it a switch, string, int, or real parameter?
  private boolean optional;     // Is the parameter optional?
  private boolean specified;    // True if the command parameter has been provided or enabled.
  private boolean on;           // Specified state of a switch parameter.
  /**
   * Define a parameter.
   * @param longName of the parameter
   * @param optional true if this is an optional parameter
   * @param type the parameter type
   * @param defaultValue the default value - may be null
   * @param helpMsg an index to a description of the parameter - may be null
   * @see scale.common.Msg
   */
  public CmdParam(String longName, boolean optional, int type, Object defaultValue, int helpMsg)
  {
    this.name         = longName.replace('-', '_');
    this.optional     = optional;
    this.type         = type;
    this.defaultValue = defaultValue;
    this.helpMsg      = helpMsg;
    this.description  = null;
    this.specified    = false;
    this.shortName    = null;

    int index = name.indexOf('_');
    if (index <= 0)
      return;

    if ((index + 1) >= name.length())
      return;

    StringBuffer buf = new StringBuffer("");
    buf.append(name.charAt(0));

    do {
      buf.append(name.charAt(index + 1));
      index = name.indexOf('_', index + 1);
    } while (index > 0);

    this.shortName = buf.toString();
  }

  /**
   * Define a parameter.
   * @param name of the parameter
   * @param optional true if this is an optional parameter
   * @param type the parameter type
   * @param defaultValue the default value - may be null
   * @param description a description of the parameter - may be null
   * @see scale.common.Msg
   */
  public CmdParam(String name, boolean optional, int type, Object defaultValue, String description)
  {
    this.name         = name;
    this.optional     = optional;
    this.type         = type;
    this.defaultValue = defaultValue;
    this.helpMsg      = -1;
    this.description  = description;
    this.specified    = false;
    this.shortName    = null;
  }

  /**
   * Return the value of the parameter.
   */
  public Object getValue()
  {
    return value;
  }

  /**
   * Return the <code>int</code> value of the parameter.
   */
  public int getIntValue()
  {
    assert (type == INT);
    return ((Integer) value).intValue();
  }
  /**
   * Return the string value of the parameter.
   */
  public String getStringValue()
  {
    assert (type == STRING);
    return (String) value;
  }

  /**
   * Return the string value of the parameter.
   */
  @SuppressWarnings("unchecked")
  public Vector<String> getStringValues()
  {
    assert (type == LIST);
    return (Vector<String>) value;
  }

  /**
   * Return true if the parameter was specified.
   */
  public boolean specified()
  {
    return specified;
  }

  /**
   * Parse a string for parameters.
   * @param command is the command used
   * @param cmdParameters the array of strings specifying the command
   * line parameters
   * @param parameters an array of allowed parameters
   * @exception InvalidKeyException if there is a command line
   * parameter error
   */
  public static void parse(String     command,
                           String[]   cmdParameters,
                           CmdParam[] parameters) throws InvalidKeyException
  {
    parse(command, cmdParameters, parameters, null);
  }

  /**
   * Parse a string for parameters.
   * @param command is the command used
   * @param cmdParameters the array of strings specifying the command
   * line parameters
   * @param parameters an array of allowed parameters
   * @param unspecified is non-null if prameters may be supplied without switches
   * @return true if system should terminate with status 0
   * @exception InvalidKeyException if there is a command line parameter error
   */
  public static boolean parse(String     command,
                              String[]   cmdParameters,
                              CmdParam[] parameters,
                              CmdParam   unspecified) throws InvalidKeyException
  {
    assert uniqueTest(parameters) : "Non unique parameter names.";

    // Initialize the SWITCH parameters with their defaults.

    for (int j = 0; j < parameters.length; j++) {
      CmdParam par = parameters[j];
      if ((par.type == SWITCH) && (par.defaultValue instanceof Boolean))
        par.specified = ((Boolean) par.defaultValue).booleanValue();
    }

    int    i      = 0;
    Vector<String> result = null;

    while (i < cmdParameters.length) {
      String arg = cmdParameters[i++].trim();

      // Is it an unspecified parameter.

      if (arg.length() == 0)
        continue;

      if (arg.charAt(0) != '-') {
        if (unspecified == null) {
          Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, arg);
          throw new InvalidKeyException(arg);
        }
        if (result == null)
          result = new Vector<String>(3);
        result.addElement(arg);
        continue;
      }

      // Remove one or more '-'.

      int ii = 1;
      while (arg.charAt(ii) == '-')
       ii++;
      arg = arg.substring(ii);
      String   p  = arg.replace('-', '_');
      CmdParam cp = findParameter(parameters, p);

      if (cp == null) {
        if (arg.equals("help")) { // Respond to -help.
          userHelp(command, cmdParameters, parameters, unspecified);
          return true;
        }

        Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, arg);
        throw new InvalidKeyException(arg);
      }

      cp.specified = true;

      String runon = null;
      if (!cp.name.equals(p) &&
          ((cp.shortName == null) ||
          !cp.shortName.equals(p))) {
        // It was a run-on parameter such as -I/usr/include
        if (p.startsWith(cp.name))
          runon = arg.substring(cp.name.length());
        else if (cp.shortName != null)
          runon = arg.substring(cp.shortName.length());
      }

      if ((cp.type != SWITCH) && (i >= cmdParameters.length) && (runon == null)) {
        Msg.reportError(Msg.MSG_Parameter_s_requires_a_value, null, 0, 0, arg);
        throw new InvalidKeyException(arg);
      }

      // Notify the user of the use of any deprecated parameters.

      String description = cp.description;
      if (description == null)
        description = Msg.getHelpMessage(cp.helpMsg);
      if (description.startsWith("deprecated"))
        Msg.reportInfo(Msg.MSG_Deprecated_s, null, 0, 0, cp.name);

      // Obtain the parameter value.

      switch (cp.type) {
      case STRING:
        if (runon != null)
          cp.value = runon;
        else
          cp.value = cmdParameters[i++];
        break;
      case INT:
        if (runon != null)
          cp.value = new Integer(runon);
        else
          cp.value = new Integer(cmdParameters[i++]);
        break;
      case REAL:
        if (runon != null)
          cp.value = new Double(runon);
        else
          cp.value = new Double(cmdParameters[i++]);
        break;
      case SWITCH:
        cp.specified = cp.on;
        break;
      case LIST:
        boolean         parens = false;
        Vector<String>  list   = cp.getStringValues();
        int             l      = cmdParameters.length;

        if (list == null) {
          list = new Vector<String>(2);
          cp.value = list;
        }

        if (runon != null) {
          list.addElement(runon);
          break;
        } else if ((i < cmdParameters.length) && cmdParameters[i].equals("(")) {
          parens = true;
          i++;
        } else
          l =  i + 1;

        int ll = list.size();
        for (int k = i; k < l; k++) {
          String v = cmdParameters[k];
          if (v.startsWith("-"))
            break;
          if (parens && v.equals(")"))
            break;
          list.addElement(v);
          i++;
        }

        if (ll == list.size())
          Msg.reportWarning(Msg.MSG_Parameter_s_requires_a_value, null, 0, 0, arg);
        break;
      default:
        Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, arg);
        throw new InvalidKeyException(types[cp.type]);
      }
    }

    // Check to see if any non-optional parameters are missing.

    for (int l = 0; l < parameters.length; l++) {
      CmdParam cp = parameters[l];
      if (!(cp.optional || cp.specified)) {
        Msg.reportError(Msg.MSG_Required_parameter_s_not_specified, cp.name);
        throw new InvalidKeyException(cp.name);
      }
      if (!cp.specified)
        cp.value = cp.defaultValue;
    }

    if (unspecified != null) {
      if (!unspecified.optional && (result == null)) {
        Msg.reportError(Msg.MSG_Required_parameter_s_not_specified, unspecified.name);
        throw new InvalidKeyException(unspecified.name);
      }
      unspecified.value = result;
    }

    return false;
  }

  private static CmdParam findParameter(CmdParam[] parameters, String p)
  {
    CmdParam cp = null;

    // Find the specified parameter information.

    for (int k = 0; k < parameters.length; k++) {
      CmdParam par = parameters[k];
      if (par.name.equals(p) ||
          ((par.shortName != null) && par.shortName.equals(p))) {
        cp = par;
        cp.on = true;
        break;
      }

      // If the parameter is a SWITCH it may be prefaced with a
      // negative indicator.

      if (par.type == SWITCH) {
        if (p.startsWith("no_")) {
          String a2 = p.substring(3);
          if (par.name.equals(a2)) {
            cp = par;
            cp.on = false;
            break;
          }
        } else if ((p.charAt(0) == 'n') && (par.shortName != null)) {
          String a2 = p.substring(1);
          if (par.shortName.equals(a2)) {
            cp = par;
            cp.on = false;
            break;
          }
        }
      }
    }

    if (cp != null)
      return cp;

    // See if it is a run-on parameter - e.g., -I/usr/include

    for (int k = 0; k < parameters.length; k++) {
      CmdParam par = parameters[k];
      if (par.type != SWITCH) {
        if (p.startsWith(par.name) &&
            ((cp == null) || (par.name.length() > cp.name.length())))
          cp = par;
        else if ((par.shortName != null) &&
                 p.startsWith(par.shortName) &&
                 ((cp == null) || (par.shortName.length() > cp.name.length())))
          cp = par;
      }
    }

    return cp;
  }

  /**
   * Generate help display.
   * @param command is the command used
   * @param cmdParameters the array of strings specifying the command
   * line parameters
   * @param parameters an array of allowed parameters
   * @param unspecified is non-null if prameters may be supplied without switches
   */
  private static void userHelp(String     command,
                               String[]   cmdParameters,
                               CmdParam[] parameters,
                               CmdParam   unspecified)
  {
    if (cmdParameters.length != 2) { // -help
      CmdParam.usage(System.out, command, parameters, unspecified);
      return;
    }

    // -help parameter-name

    String arg = cmdParameters[1];
    int    ii  = 0;
    while (arg.charAt(ii) == '-')
      ii++;
    arg = arg.substring(ii);

    if (arg.equals("all")) { // -help all - Display help for all parameters.
      for (int j = 0; j < parameters.length; j++)
        System.out.println(displayParamDescription(parameters[j]));

      if (unspecified != null)
        System.out.println(displayParamDescription(unspecified));

      return;
    }

    if ((unspecified != null) && unspecified.name.equals(arg)) {
      System.out.println(displayParamDescription(unspecified));
      return;
    }

    String   p   = arg.replace('-', '_');
    CmdParam par = findParameter(parameters, p);
    if (par == null) {
      Msg.reportError(Msg.MSG_Invalid_parameter_s, null, 0, 0, arg);
      CmdParam.usage(System.out, command, parameters, unspecified);
      return;
    }

    System.out.println(displayParamDescription(par));
  }

  private static String displayParam(CmdParam cp, boolean unspecified)
  {
    StringBuffer s = new StringBuffer(" ");

    if (cp.optional)
      s.append("[");

    if (!unspecified)
      s.append("-");

    s.append(cp.name);
    if (!unspecified)
      switch (cp.type) {
      case STRING:
        s.append(" string");
        break;
      case INT:
        s.append(" integer");
        break;
      case REAL:
        s.append(" real");
        break;
      case SWITCH:
        break;
      case LIST:
        s.append(" [string]+");
        break;
      default:
        s.append(" error");
        break;
      }
      
    if (cp.optional)
      s.append("]");

    return s.toString();
  }

  private static String displayParamDescription(CmdParam cp)
  {
    StringBuffer s = new StringBuffer("   ");
    s.append(displayParam(cp, false));

    String description = cp.description;
    if (description == null)
      description = Msg.getHelpMessage(cp.helpMsg);

    int n1 = description.indexOf('\n');
    int nl = description.length();

    s.append(" - ");
    if (n1 < 0) {
      s.append(description);
      n1 = nl;
    } else {
      s.append(description.substring(0, n1));
      n1++;
    }

    if (cp.defaultValue != null) {
      s.append(" - ");
      s.append(Msg.getMessage(Msg.MSG_Default_is));
      s.append(' ');
      if (cp.defaultValue instanceof String) {
        s.append('\"');
        s.append(cp.defaultValue);
        s.append('\"');
      } else if (cp.defaultValue instanceof Character) {
        s.append('\'');
        s.append(cp.defaultValue);
        s.append('\'');
      } else
        s.append(cp.defaultValue.toString());
      s.append('.');
    }

    while (n1 < nl) {
      int n2 = description.indexOf('\n', n1);

      s.append("\n        ");
      if (n2 < 0) {
        s.append(description.substring(n1));
        break;
      }
      s.append(description.substring(n1, n2));
      n1 = n2 + 1;
    }

    return s.toString();
  }

  /**
   * Print out a usage message based upon the parameters.
   * @param s is the stream to be printed on
   * @param command is the command name
   * @param parameters is the array of parameter definitions
   */
  public static void usage(PrintStream s, String command, CmdParam[] parameters)
  {
    usage(s, command, parameters, null);
  }

  /**
   * Print out a usage message based upon the parameters.
   * @param s is the stream to be printed on
   * @param command is the command name
   * @param parameters is the array of parameter definitions
   * @param unspecified is non-null if prameters may be supplied without switches
   */
  public static void usage(PrintStream s, String command, CmdParam[] parameters, CmdParam unspecified)
  {
    s.print(Msg.insertText(Msg.MSG_Usage_java_s, command));
    for (int i = 0; i < parameters.length; i++) {
      s.print(displayParam(parameters[i], false));
    }

    if (unspecified != null) {
      s.print(displayParam(unspecified, true));     
    }

    s.println("");
    s.println(Msg.insertText(Msg.getHelpMessage(Msg.HLP_phelp), command, command));
  }

  /**
   * Return true if there are no duplicates in a set of parameter names.
   */
  private static boolean uniqueTest(CmdParam[] parameters)
  {
    HashSet<String> parms = WorkArea.<String>getSet("");
    try {
      for (int i = 0; i < parameters.length; i++) {
        CmdParam par = parameters[i];
        if (!parms.add(par.name) || !parms.add("no_" + par.name)) {
          System.out.println("** Parameter " + par);
          return false;
        }
        if ((par.shortName != null) &&
            (!parms.add(par.shortName) || !parms.add("n" + par.shortName))) {
          System.out.println("** Parameter " + par);
          return false;
        }
      }
      return true;
    } finally {
      WorkArea.<String>returnSet(parms);
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(CmdParam ");
    buf.append(displayParamDescription(this));
    if (type == SWITCH) {
      buf.append(", on = ");
      buf.append(specified);
    } else {
      buf.append(", value = ");
      buf.append(value);
    }
    buf.append(')');
    return buf.toString();
  }
}
