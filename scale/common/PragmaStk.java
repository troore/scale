package scale.common;

/**
 * This class is used to hold and process #pragma information.
 * <p>
 * $Id: PragmaStk.java,v 1.5 2007-08-16 17:27:39 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This implementation allows multiple pragma stacks which, in turn,
 * allows multiple threads to compile separate source files.
 * <p>
 * The parser allocates a new <code>PragmaStk</code> and uses the
 * {@link #newPragma newPragma()} method to obtain the pragma that
 * contains the appropriate information.  The AST nodes can then link
 * to this {@link .Pragma Pragma} instance.
 * <p>
 * Pragmas have the following syntax:
 * <pre>
 *   pragma := "#pragma" text
 *   text   := ["push" | "pop"] [cmd]
 *   cmd    := switch | unroll 
 *   switch := sw {"on" | "off" | "default"} 
 *   sw     := "STDC FP_CONTRACT" | "STDC FENV_ACCESS" |
 *             "STDC CX_LIMITED_RANGE" | "LOOP_PERMUTE" |
 *             "LOOP_TEST_AT_END"
 *   unroll := "UNROLL" integer
 * </pre>
 * The <code>push</code> prefix causes the current pragma values to
 * be pushed on a stack and a copy made that is then changed.  The
 * <code>pop</code> prefix causes the pragma values on the top of
 * the stack to be poped off and used as the current values before
 * being changed.
 */
public final class PragmaStk
{
  public static final int FP_CONTRACT      = 0x01;
  public static final int FENV_ACCESS      = 0x02;
  public static final int CX_LIMITED_RANGE = 0x04;
  public static final int LOOP_PERMUTE     = 0x08;
  public static final int LOOP_TEST_AT_END = 0x10;

  private static int defaultFlags = LOOP_PERMUTE;

  private static final String[] flagStr = {
    "STD FP_CONTRACT", "STD FENV_ACCESS", "STD CX_LIMITED_RANGE",
    "LOOP_PERMUTE",    "LOOP_TEST_AT_END",
  };

  /**
   * The user specified loop unrolling factor.  The default is 0 which
   * means that the compiler is to determine the unroll factor.  An
   * unroll factor of 1 inhibits unrolling.
   */
  public static final int UNROLL         = 0;
  /**
   * Not used.  It's here to show how new ones should be added.
   */
  public static final int JUST_TO_HAVE_2 = 1;

  private static       int[]    defaultValues = {0, 0};
  private static final String[] valueStr      = {"UNROLL", "JUST_TO_HAVE_2"};

  /**
   * The default prama contains the defaults switch settings and
   * values.  Some are set by compiler command line switches.  This
   * pragma is used when no pragmas have been provided.  For example,
   * Fortran has no pragma-like facility.
   */
  public static final Pragma defaultPragma = new Pragma(defaultFlags);

  public static final class Pragma
  {
    protected int   flags;
    private   int[] values;

    private Pragma(int flags)
    {
      this.flags  = flags;
      this.values = null;
    }

    public Pragma copy()
    {
      Pragma np = new Pragma(flags);
      if (values != null) {
        np.values = new int[values.length];
        System.arraycopy(values, 0, np.values, 0, values.length);
      }
      return np;
    }

    /**
     * Return the specified value.
     * @see #UNROLL
     */
    public int getValue(int index)
    {
      assert (index >= 0) && (index < valueStr.length);

      if (values == null)
        return defaultValues[index];

      return values[index];
    }

    /**
     * Return true if the specified flag is set.
     * @see #CX_LIMITED_RANGE
     */
    public boolean isSet(int flag)
    {
      return (flags & flag) != 0;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(PRAGMA");

      int flg = flags;
      for (int i = 0; flg != 0; i++, flg >>= 1) {
        if ((flg & 1) != 0) {
          buf.append(' ');
          buf.append(flagStr[i]);
        }
      }

      int v[] = values;
      if (v == null)
        v = defaultValues;

      for (int i = 0; i < v.length; i++) {
        if (v[i] == 0)
          continue;

        buf.append(' ');
        buf.append(valueStr[i]);
        buf.append('=');
        buf.append(Integer.toString(v[i]));
      }

      buf.append(')');
      return buf.toString();
    }
  }

  private Stack<Pragma>  stk;
  private Pragma         top;
  private String         token;
  private String         filename;

  /**
   * Create a pragma stack to be used in processing pragma
   * information.
   * @param filename is the filename of the source code that will be
   * parsed
   */
  public PragmaStk(String filename)
  {
    this.filename = filename;
    this.stk      = null;
    this.top      = defaultPragma;
  }

  /**
   * Return the current (top) pragma entry.
   */
  public Pragma getTop()
  {
    return top;
  }

  /**
   * Specify the default for the specified flag.
   */
  public static void setDefaultFlag(int flag, boolean on)
  {
    if (on)
      defaultFlags |= flag;
    else
      defaultFlags &= ~flag;

    defaultPragma.flags = defaultFlags;
  }

  /**
   * Specify the default for the specified value.
   */
  public static void setDefaultValue(int index, int value)
  {
    assert (index >= 0) && (index < valueStr.length);
    defaultValues[index] = value;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(PragmaStk ");
    buf.append(filename);
    buf.append(' ');
    buf.append(top.toString());
    buf.append(' ');
    buf.append(stk);
    buf.append(')');
    return buf.toString();
  }

  private int nextToken(int i, String text)
  {
    int l = text.length();
    while ((i < l) && (text.charAt(i) == ' ')) i++;
    if (i >= l) {
      token = null;
      return i;
    }
    int j = i + 1;
    while ((j < l) && (text.charAt(j) != ' ')) j++;

    token = text.substring(i, j);
    return j;
  }

  /**
   * Obtain the pragma information to be used.  The text is parsed.
   * It must be of the form (whitespace required between elements):
   * <pre>
   *   text   := ["push" | "pop"] [cmd]
   *   cmd    := switch | unroll 
   *   switch := sw {"on" | "off" | "default"} 
   *   sw     := "STDC FP_CONTRACT" | "STDC FENV_ACCESS" |
   *             "STDC CX_LIMITED_RANGE" | "LOOP_PERMUTE" |
   *             "LOOP_TEST_AT_END"
   *   unroll := "UNROLL" integer
   * </pre>
   * The <code>push</code> prefix causes the current pragma values to
   * be pushed on a stack and a copy made that is then changed.  The
   * <code>pop</code> prefix causes the pragma values on the top of
   * the stack to be poped off and used as the current values before
   * being changed.
   * @param pragmaText is the pragma text to be processed
   * @param lineno is the current line in the source code
   * @return true if the pragma was not valid
   */
  public boolean newPragma(String pragmaText, int lineno)
  {
    int i = nextToken(0, pragmaText);

    if (token == null)
      return false;

    if ("push".equals(token) || "PUSH".equals(token)) {
      if (stk == null)
        stk = new Stack<Pragma>();
      stk.push(top);
      i = nextToken(i, pragmaText);
      if (token == null)
        return false;
    } else if ("pop".equals(token) || "POP".equals(token)) {
      if ((stk != null) && !stk.empty())
        top = stk.pop();
      i = nextToken(i, pragmaText);
      if (token == null)
        return false;
    }

    int    flg = 0;
    String p   = token;
    if ("STDC".equals(p)) {
      i = nextToken(i, pragmaText);
      p = p + " " + token;
    } else if (p != null)
      p = p.toUpperCase();

    // Look up flag name.

    for (int j = 0; j < flagStr.length; j++) {
      if (flagStr[j].equals(p)) {
        flg = 1 << j;
        break;
      }
    }

    if (flg != 0) { // Valid flag name found.
      Pragma np = top.copy();

      i = nextToken(i, pragmaText);
      int sw = 0;
      if ("on".equals(token) || "ON".equals(token))
        np.flags |= flg;
      else if ("off".equals(token) || "OFF".equals(token))
        np.flags &= ~flg;
      else if ("default".equals(token) || "DEFAULT".equals(token))
        np.flags = (np.flags & ~flg) | (defaultFlags & flg);
      else
        return true; // Pragma not recognized.

      top = np;

      return false;
    }

    // Look up value name.

    int index = -1;
    for (int j = 0; j < valueStr.length; j++) {
      if (valueStr[j].equals(p)) {
        index = j;
        break;
      }
    }

    if (index < 0)
      return true; // Pragma not recognized.

    i = nextToken(i, pragmaText);
    if (token == null) // Pragma not recognized.
      return true;

    try { // Valid value name found.
      int    value = Integer.decode(token).intValue();
      Pragma np    = top.copy();

      if (np.values == null) {
        np.values = new int[valueStr.length];

        for (int j = 0; j < np.values.length; j++)
          np.values[j] = defaultValues[j];
      }

      np.values[index] = value;
      top = np;

      return false;
    } catch (java.lang.NumberFormatException ex) {
      return true; // Pragma not recognized.
    }
  }
}
