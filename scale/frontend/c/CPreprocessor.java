package scale.frontend.c;

import java.io.*;
import scale.common.Stack;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import scale.common.*;

/** 
 * This class reads a file and transforms it using the C preprocessor conventions.
 * <p>
 * $Id: CPreprocessor.java,v 1.51 2007-06-12 20:21:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class will send warning messages to <code>System.err</code> when
 * any problem in the source file is discovered.
 */
public class CPreprocessor extends java.io.Reader
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * Set true if GNU extensions are allowed.
   */
  protected boolean allowGNUExtensions = false;
  /**
   * Set true if strict ANSI C required.
   */
  protected boolean strictANSIC = false;
  /**
   * Pre-defined macros.
   */
  private static final String mline = "__LINE__";
  private static final String mfile = "__FILE__";
  private static final String mdate = "__DATE__";
  private static final String mtime = "__TIME__";
  private static final String mstdc = "__STDC__";
  private static final String mftn  = "__FUNCTION__";

  /**
   * Pre-defined macro types.
   * There is one type for each different pre-defined macro.
   */
  private static final int M_NORMAL  = 0; // For user defined macros.
  private static final int M_LINE    = 1;
  private static final int M_FILE    = 2;
  private static final int M_DEFINED = 3;
  private static final int M_DATE    = 4;
  private static final int M_TIME    = 5;
  private static final int M_STDC    = 6;
  private static final int M_FTN     = 7;

  /**
   * Pre-processor token types.
   */
  private static final int PP_NONE    = 0; // No token.
  private static final int PP_IDENT   = 1;
  private static final int PP_HEADER  = 2;
  private static final int PP_NUM     = 3;
  private static final int PP_STRING  = 4;
  private static final int PP_CHAR    = 5;
  private static final int PP_OP      = 6;
  private static final int PP_PUNCT   = 7;
  private static final int PP_OTHER   = 8;
  private static final int PP_ERROR   = 9;

  private static final boolean[] ppValue = {false, false,   false,    true,  true,     true,   false, false,    false,  false};
  private static final String[]  types   = {"N",   "ident", "header", "num", "string", "char", "op",  "punct", "other", "error"};

  /**
   * Pre-processor operators.
   */
  private static final int OP_LBRACK      = 0;  // [
  private static final int OP_RBRACK      = 1;  // ]
  private static final int OP_LPAREN      = 2;  // (
  private static final int OP_RPAREN      = 3;  // )
  private static final int OP_DOT         = 4;  // .
  private static final int OP_FIELD       = 5;  // ->
  private static final int OP_INC         = 6;  // ++
  private static final int OP_DEC         = 7;  // --
  private static final int OP_AND         = 8;  // &
  private static final int OP_MULT        = 9;  // *
  private static final int OP_ADD         = 10; // +
  private static final int OP_SUB         = 11; // -
  private static final int OP_COMPLEMENT  = 12; // ~
  private static final int OP_NOT         = 13; // !
  private static final int OP_SIZEOF      = 14; // sizeof
  private static final int OP_DIV         = 15; // /
  private static final int OP_MOD         = 16; // %
  private static final int OP_LSHIFT      = 17; // <<
  private static final int OP_RSHIFT      = 18; // >>
  private static final int OP_LT          = 19; // <
  private static final int OP_GT          = 20; // >
  private static final int OP_LE          = 21; // <=
  private static final int OP_GE          = 22; // >=
  private static final int OP_EQ          = 23; // ==
  private static final int OP_NE          = 24; // !=
  private static final int OP_XOR         = 25; // ^
  private static final int OP_OR          = 26; // |
  private static final int OP_CAND        = 27; // &&
  private static final int OP_COR         = 28; // ||
  private static final int OP_SELECT      = 29; // ?
  private static final int OP_COLON       = 30; // :
  private static final int OP_ASSIGN      = 31; // =
  private static final int OP_MULT_ASSIGN = 32; // *=
  private static final int OP_DIV_ASSIGN  = 33; // /=
  private static final int OP_MOD_ASSIGN  = 34; // %=
  private static final int OP_ADD_ASSIGN  = 35; // +=
  private static final int OP_SUB_ASSIGN  = 36; // -=
  private static final int OP_LS_ASSIGN   = 37; // <<=
  private static final int OP_RS_ASSIGN   = 38; // >>=
  private static final int OP_AND_ASSIGN  = 39; // &=
  private static final int OP_XOR_ASSIGN  = 40; // ^=
  private static final int OP_OR_ASSIGN   = 41; // |=
  private static final int OP_COMMA       = 42; // ,
  private static final int OP_HATCH       = 43; // #
  private static final int OP_DHATCH      = 44; // ##
  private static final int OP_LBRACE      = 45; // {
  private static final int OP_RBRACE      = 46; // }

  /**
   * Pre-processor operator precedences.
   */
  private static final int[] precedences = {
       0,  0,  0,  0, 
      20, 20, 20, 20,
      10, 15, 14, 14,
      18, 18, 18, 15,
      15, 13, 13, 12,
      12, 12, 12, 12,
      12, 11, 11, 10,
      10,  5,  5, 10,
      10, 10, 10, 10,
      10, 10, 10, 10,
      10, 10,  5,  5,
      5, 0, 0
  };

  private static final String[] ops = {
    "[",
    "]",
    "(",
    ")",
    ".",
    "->",
    "++",
    "--",
    "&",
    "*",
    "+",
    "-",
    "~",
    "!",
    "sizeof",
    "/",
    "%",
    "<<",
    ">>",
    "<",
    ">",
    "<=",
    ">=",
    "==",
    "!=",
    "^",
    "|",
    "&&",
    "||",
    "?",
    ":",
    "=",
    "*=",
    "/=",
    "%=",
    "+=",
    "-=",
    "<<=",
    ">>=",
    "&=",
    "^=",
    "|=",
    ",",
    "#",
    "##",
    "{",
    "}"
   };

  private static final byte IF_NO   = 0; // This if-clause was not selected.
  private static final byte IF_YES  = 1; // This if-clause was selected.
  private static final byte IF_DONE = 2; // An if-clause has been selected.

  private static final int[]    stateTransition = {IF_YES, IF_DONE, IF_DONE, IF_NO, IF_DONE, IF_DONE};
  private static final String[] states          = {"no", "yes", "done"};

  /**
   * This is the base class for macros.
   */
  private abstract class Macro
  {
    public char[]  name;     // Macro name
    public AMacro  replaces; // Macro arguments
    public Macro   next;     // Next macro with same hash
    public boolean inUse;    // If true, this macro currently has its arguments defined.
    public boolean ignore;   // If true, this macro should be ignored.
    public boolean isRecursive; // If true, this macro references itself.

    public Macro(char[] name, AMacro replaces)
    {
      this.name = name;
      this.next = null;
      this.inUse = false;
      this.replaces = replaces;
    }

    public Macro(String name, AMacro replaces)
    {
      int  l    = name.length();
      this.name = new char[l];

      name.getChars(0, l, this.name, 0);

      this.next = null;
      this.inUse = false;
      this.ignore = false;
      this.replaces = replaces;
    }

    public abstract Macro copy();

    public final AMacro copyReplaces()
    {
      if (replaces == null)
        return null;

      Macro  rep = replaces;
      AMacro res = (AMacro) replaces.copy();
      Macro  nxt = res;
      while (rep.next != null) {
        nxt.next = rep.next.copy();
        nxt = nxt.next;
        rep = rep.next;
      }
      return res;
    }

    public String getName()
    {
      return new String(name);
    }

    /**
     * Return true is the macro has the specified name.
     * @param m is the char array containing the name
     * @param offset is the index into the array of the first character of the name
     * @param length is the number of characters in the name
     */
    public final boolean match(char[] m, int offset, int length)
    {
      if (name.length != length)
        return false;
      for (int i = 0; i < name.length; i++)
        if (m[i + offset] != name[i])
          return false;
      return true;
    }

    /**
     * Return true is the macro has the specified name.
     */
    public final boolean match(String s)
    {
      if (s.length() != name.length)
        return false;
      for (int i = 0; i < name.length; i++)
        if (s.charAt(i) != name[i])
          return false;
      return true;
    }

    public boolean definedWithParens()
    {
      return false;
    }

    /**
     * Put the replacement text into the array at the position specified.
     */
    public abstract void getReplacementChars(char[] arr, int start);
    /**
     * Return the length of the replacement text for the macro.
     */
    public abstract int getReplacementTextLength();
    /**
     * Return the type of the macro.
     */
    public abstract int getType();
    /**
     * Generate the macro as a C statement.
     */
    public abstract String display();
  }

  /**
   * This class represents user-defined macros.
   */
  private final class NMacro extends Macro
  {
    private String  text; // Replacement text
    private boolean withParens; // Defined with parens.

    public NMacro(char[] name, String text, AMacro replaces, boolean parens)
    {
      super(name, replaces);
      this.text = text;
      this.withParens = parens;
      this.isRecursive = false;

      // Determine if the macro calls itself.

      char n = name[0];
      for (int i = 0; i < text.length() - name.length + 1; i++) {
        char c = text.charAt(i);
        if (n != c)
          continue;

        int     k     = 1;
        boolean found = true;
        for (int j = i + 1; j < i + name.length; j++) {
          if (text.charAt(j) != line[k]) {
            found = false;
            break;
          }
          k++;
        }
        if (!found)
          continue;

        if ((i < 1) || !Character.isLetter(text.charAt(i - 1))) {
          int kk = i + name.length;
          if ((kk >= text.length()) || ((text.charAt(kk) != '_') && !Character.isLetterOrDigit(text.charAt(kk)))) {
            this.isRecursive = true;
            return;
          }
        }
      }
    }

    public NMacro(String name, String text, AMacro replaces)
    {
      super(name, replaces);
      this.text = text;
    }

    public Macro copy()
    {
      return new NMacro(name, text, copyReplaces(), withParens);
    }

    public void setReplacementText(String text)
    {
      this.text = text;
    }

    public void getReplacementChars(char[] arr, int start)
    {
      text.getChars(0, text.length(), arr, start);
    }

    public String getReplacementText()
    {
      return text;
    }

    public int getReplacementTextLength()
    {
      return text.length();
    }

    public int getType()
    {
      return M_NORMAL;
    }

    public boolean definedWithParens()
    {
      return withParens;
    }

    /**
     * Return true if this is a valid macro.
     * For example
     * <pre>
     *   #define setjmp(env) setjmp(env)
     * </pre>
     * defines an invalid macro.
     */
    public boolean valid()
    {
      if (name.length > text.length())
        return true;

      for (int j = 0; j < name.length; j++) {
        if (name[j] != text.charAt(j))
          return true;
      }

      StringBuffer buf = new StringBuffer("");
      buf.append(name);
      if (replaces != null) {
        buf.append('(');
        Macro m = replaces;
        while (m != null) {
          buf.append(m.name);
          m = m.next;
          if (m != null)
            buf.append(',');
        }
        buf.append(')');
      }

      return !buf.toString().equals(text);
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(NMacro ");
      buf.append(name);
      if (replaces != null) {
        buf.append('(');
        Macro m = replaces;
        while (m != null) {
          buf.append(m.name);
          m = m.next;
          if (m != null)
            buf.append(',');
        }
        buf.append(')');
      }
      buf.append(":=\"");
      buf.append(text);
      buf.append('"');
      if (ignore)
        buf.append(", ignore");
      if (inUse)
        buf.append(", inUse");
      if (isRecursive)
        buf.append(", recursive");
      if (withParens)
        buf.append(", wp");
      buf.append(')');
      return buf.toString();
    }

    public String display()
    {
      StringBuffer buf = new StringBuffer("#DEFINE ");
      buf.append(name);
      if (replaces != null) {
        buf.append('(');
        Macro m = replaces;
        while (m != null) {
          buf.append(m.name);
          m = m.next;
          if (m != null)
            buf.append(',');
        }
        buf.append(')');
      }
      buf.append(" ");
      buf.append(text);
      return buf.toString();
    }
  }

  /**
   * This class represents macro arguments.
   * This class was created solely to avoid allocating strings for
   * the replacement text of macro arguments.  It depends on the
   * fact that macro replacement is performed on the original
   * <code>char</code> array <code>line</code>.
   */
  private final class AMacro extends Macro
  {
    private char[] text;   // The replacement text.
    private int    length; // Length of the replacement text.

    public AMacro(char[] name)
    {
      super(name, null);
    }

    public AMacro(String name)
    {
      super(name, null);
    }

    public Macro copy()
    {
      return new AMacro(name);
    }

    /**
     * Specify the position in <code>line</code> of the replacement text.
     */
    public void setReplacementText(int offset, int length)
    {
      while ((length > 0) && (line[offset] == ' ')) {
        offset++;
        length--;
      }

      if ((text == null) || (length > text.length))
        text = new char[length + 1];
      System.arraycopy(line, offset, text, 0, length);
      this.length = length;
    }

    public void getReplacementChars(char[] arr, int offset)
    {
      if ((length > 0) && (text != null))
        System.arraycopy(text, 0, arr, offset, this.length);
    }

    public int getReplacementTextLength()
    {
      return length;
    }

    public char[] getReplacementText()
    {
      return text;
    }

    public int getType()
    {
      return M_NORMAL;
    }

    /**
     * Return a hash value for the name specified.
     * @param offset is the offset in name of the first character to include
     * @param length is the number of characters to include
     */
    private int hash()
    {
      if (length == 0)
        return 0;

      int h = 0;
      // text[0]*31^(n) + text[1]*31^(n-1) + ... + text[n]
      int n = length - 1;
      for (int i = 0; i < n; i++)
        h += text[i] * 31 ^ (n - i);

      return (h + text[n]) & 0xff;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("(AMacro ");
      buf.append(name);
      buf.append(":=");
      if ((text != null) && (length > 0))
        buf.append(new String(text, 0, length));
      buf.append(',');
      buf.append(ignore);
      buf.append(',');
      buf.append(inUse);
      buf.append(')');
      return buf.toString();
    }

    public String display()
    {
      return toString();
    }
  }

  /**
   * This class represents special (pre-defined) macros.
   */
  private final class SMacro extends Macro
  {
    private int type;

    public SMacro(int type, char[] name, AMacro replaces)
    {
      super(name, replaces);
      this.type = type;
    }

    public SMacro(int type, String name, AMacro replaces)
    {
      super(name, replaces);
      this.type = type;
    }

    public Macro copy()
    {
      return new SMacro(type, name, copyReplaces());
    }

    public void getReplacementChars(char[] arr, int start)
    {
      String s = null;
      switch (type) {
      default        : throw new scale.common.InternalError("Special macro type is normal.");
      case M_LINE    : s = Integer.toString(lineNumber - 1); break;
      case M_FILE    : s = "\"" + curReader.getFilename() + "\""; break;
      case M_DEFINED :
        // Replace "defined(x)" with 0 or 1.
        int   hash = replaces.hash();
        Macro m    = macros[hash];
        s = "0";
        while (m != null) {
          if (m.match(replaces.getReplacementText(), 0, replaces.getReplacementTextLength())) {
            s = "1";
            break;
          }
          m = m.next;
        }
        break;
      case M_DATE    : s = "\"" + date + "\""; break;
      case M_TIME    : s = "\"" + time + "\""; break;
      case M_STDC    : s = "1"; break;
      case M_FTN     : s = "\"" + currentFtn + "\""; break;
      }
      s.getChars(0, s.length(), arr, start);
    }

    public String getReplacementText()
    {
      switch (type) {
      default        : throw new scale.common.InternalError("Special macro type is normal.");
      case M_LINE    : return Integer.toString(lineNumber - 1);
      case M_FILE    : return "\"" + curReader.getFilename() + "\"";
      case M_DEFINED :
        // Replace "defined(x)" with 0 or 1.
        int   hash = replaces.hash();
        Macro m    = macros[hash];
        while (m != null) {
          if (m.match(replaces.getReplacementText(), 0, replaces.getReplacementTextLength()))
            return "1";
          m = m.next;
        }
        return "0";
      case M_DATE    : return "\"" + date + "\"";
      case M_TIME    : return "\"" + time + "\"";
      case M_STDC    : return "1";
      case M_FTN     : return "\"" + currentFtn + "\"";
      }
    }

    public int getReplacementTextLength()
    {
      switch (type) {
      default        : throw new scale.common.InternalError("Special macro type is normal.");
      case M_LINE    : return Integer.toString(lineNumber - 1).length();
      case M_FILE    : return 2 + curReader.getFilename().length();
      case M_DEFINED : return 1;
      case M_DATE    : return 2 + date.length();
      case M_TIME    : return 2 + time.length();
      case M_STDC    : return 1;
      case M_FTN     : return currentFtn.length() + 2;
      }
    }

    public int getType()
    {
      return type;
    }

    public String toString()
    {
      try {
        StringBuffer buf = new StringBuffer("(");
        buf.append(getClass().getName());
        buf.append(' ');
        buf.append(name);
        if (replaces != null) {
          buf.append('(');
          Macro m = replaces;
          while (m != null) {
            buf.append(m.name);
            m = m.next;
            if (m != null)
              buf.append(',');
          }
          buf.append(')');
        }
        buf.append(":=");
        buf.append(getReplacementText());
        buf.append(')');
        return buf.toString();
      } catch(java.lang.Throwable ex) {
        ex.printStackTrace();
        return "JUNK";
      }
    }

    public String display()
    {
      try {
        StringBuffer buf = new StringBuffer("#DEFINE");
        buf.append(name);
        if (replaces != null) {
          buf.append('(');
          Macro m = replaces;
          while (m != null) {
            buf.append(m.name);
            m = m.next;
            if (m != null)
              buf.append(',');
          }
          buf.append(')');
        }
        buf.append(" ");
        buf.append(getReplacementText());
        return buf.toString();
      } catch(java.lang.Throwable ex) {
        ex.printStackTrace();
        return "JUNK";
      }
    }
  }

  /**
   * Used to report pre-processor detected source code errors.
   */
  private static class Err extends java.lang.Exception
  {
    private static final long serialVersionUID = 42L;

    public int position; // Position in the line at which the error was detected.
    public int errno;

    /**
     * Create an error.
     * @param errno specifies the error message
     * @param position is the position in the line at which the error was detected
     */
    public Err(int errno, int position)
    {
      super(Msg.getMessage(errno));
      this.position = position;
      this.errno = errno;
    }

    public String getMessage()
    {
      return Msg.getMessage(errno);
    }
  }

  private Stack<CReader> hstack;        // Nesting stack for include directives.
  private Vector<String> systemDirs;    // Search directories for system include files.
  private Vector<String> userDirs;      // Search directories for user include files.
  private Vector<String> macroText;     // Used to collect the text of defined macros for display to the user.

  private Macro[] macros;        // Mapping from macro name to definition.
  private Macro   defMacro;      // For #if defined(..)
  private CReader curReader;     // Currently accessed file's reader.
  private char[]  line;          // Array containing the current line from the source program.
  private int     lineLength;    // The number of characters in the line.
  private int     linePosition;  // Current read position in the line.
  private char[]  sline;         // Saved text for after macro processing.
  private int     slinecnt;      // Number of additional newlines that must be sent.
  private int     slineLength;   // Number of chars in sline.

  private Macro[]   resetMacro;   // These values are used to avoid infinite 
  private boolean[] resetFlg;     // recursion when a macro calls another
  private int[]     resetPos;     // macro which then calls the first macro.
  private int       resetPtr;

  private byte    ppType;        // Type of pre-processor token found.
  private long    ppSubType;     // Additional token information.
  private int     ppStart;       // Index to first character of the token.
  private int     ppEnd;         // Index to first character after the token.

  private byte[]  ttype;         // Token type.
  private int[]   tstart;        // Token starting position in line.
  private int[]   tend;          // Token ending position in line.
  private long[]  tvalue;        // Token value.
  private int     tokenPtr = 0;  // Next position in tokens.

  private int     ifState;       // Are we in the then or else clause of an if.
  private byte[]  ifstack;       // Record the state for nested ifs.
  private int     ifstackptr;    // Next position in ifstack.

  private boolean bypass;        // If true, skip source lines.
  private boolean genFirst;      // True if a line number line should be generated.
  private String  date;          // Today's date.
  private String  time;          // Time of day this class instance was created.
  private String  currentFtn;    // Current function name.
  private int     lineNumber;    // Current line number.
  private int     includeIndex;  // Index of the current include file.

  /**
   * Create a reader for C programs that does the C pre-processing.
   * This reader reads from the file specified nad processes it
   * according to the standard C preprocessor rules.
   * <p>

   * A set of user-defined macros can be specified.  They must be in
   * one of the forms
   * <pre>
   *   NAME
   *   NAME=TEXT
   * </pre>
   * <p>
   * Directories to be searched for include files are specified using
   * two arguments.  The <code>userDirs</code> parameter specifies a
   * <code>Vector</code> of <code>String</code> instances that are
   * searched when the include file name is specified in the source
   * file as a string delimited by double quotes.  The pre-processor
   * also includes the directory containing the source file in the list
   * of directories to be searched.
   * <p>
   * The <code>systemDirs</code> parameter specifies an array of
   * <code>String</code> instances that are searched when the include
   * file name is specified as a string delimited by angle-brackets.
   * <p>
   * @param filename is the source file name
   * @param userMacros is a list of user defined macros in the form of
   * <code>name=text</code> or <code>null</code>
   * @param userDirs is a list of directory pathnames to use in
   * finding user include files or <code>null</code>
   * @param systemDirs is a list of directory pathnames to use in
   * finding system include files or <code>null</code>
   * @param macroText is <code>null</code> or is used to collect macro
   * definitions as text strings
   */
  public CPreprocessor(String         filename,
                       Vector<String> userMacros,
                       Vector<String> userDirs,
                       Vector<String> systemDirs,
                       Vector<String> macroText) throws IOException
  {
    super();

    this.userDirs   = userDirs;
    this.systemDirs = systemDirs;
    this.macroText  = macroText;
    this.hstack     = new Stack<CReader>();
    this.macros     = new Macro[256];
    this.line       = new char[1024];
    this.ttype      = new byte[100];
    this.tstart     = new int[100];
    this.tend       = new int[100];
    this.tvalue     = new long[100];
    this.ifstack    = new byte[100];
    this.ifState    = IF_YES; // We start out accepting every source line.
    this.ifstackptr = 0;
    this.bypass     = false;
    this.genFirst   = false;
    this.resetMacro = new Macro[10];   // These values are used to avoid infinite 
    this.resetFlg   = new boolean[10]; // recursion when a macro calls another
    this.resetPos   = new int[10];     // macro which then calls the first macro.
    this.resetPtr   = 0;

    // Get the current date and time.

    Calendar   gc  = new GregorianCalendar();
    Date       dat = gc.getTime();
    DateFormat df  = new SimpleDateFormat("hh:mm:ss MMM d yyyy");
    String     s   = df.format(dat);
    date = s.substring(9);
    time = s.substring(0, 9);

    // Open the include file.

    this.curReader = new CReader(filename);
    lineNumber = 0;

    // Define the predefined macros.

    addMacro(M_LINE, mline, "");
    addMacro(M_FILE, mfile, "");
    addMacro(M_DATE, mdate, "");
    addMacro(M_TIME, mtime, "");
    addMacro(M_STDC, mstdc, "");
    addMacro(M_FTN,  mftn,  "");

    // Handle the defined() function as a macro when processing ifs.

    AMacro x = new AMacro("x");
    defMacro = new SMacro(M_DEFINED, "defined", x);

    if (CReader.classTrace || classTrace)
      System.out.println("Reading: " + filename);

    // Define the user-defined macros.

    if (userMacros != null) {
      int    dvl = userMacros.size();
      for (int j = 0; j < dvl; j++) {
        String um = userMacros.elementAt(j);
        int    eq = um.indexOf('=');
        String m  = um;
        String t  = "1";

        if (eq > 0) {
          m = um.substring(0, eq);
          t = um.substring(eq + 1);
        }

        addMacro(m, t);
      }
    }

    genFirst = true;
  }

  /**
   * Specify the next file to read.
   */
  private CReader newFile(String filename) throws IOException
  {
    CReader xcurReader = new CReader(filename);
    if (CReader.classTrace || classTrace)
      System.out.println("Reading: " + filename);
    genFirst = true;
    lineNumber = 0;
    hstack.push(curReader);
    return xcurReader;
  }

  public CReader getCReader()
  {
    return curReader;
  }

  public int getLineNumber()
  {
    return lineNumber;
  }

  /**
   * Generate a line of the form
   * <pre>
   * #line nnn "filename"
   * </pre>
   * where <code>nnn</code> is the line number specified.
   */
  private void genLine(String filename, int lineno)
  {
    String ln  = Integer.toString(lineno);
    int    lnl = ln.length();
    String fn  = curReader.getFilename();
    int    fnl = fn.length();
    int    i   = 0;
    int    nl  = 6 + lnl + 2 + fnl + 3;

    if (nl > line.length)
      line = new char[nl + 1];

    line[i++] = '#';
    line[i++] = 'l';
    line[i++] = 'i';
    line[i++] = 'n';
    line[i++] = 'e';
    line[i++] = ' ';
    ln.getChars(0, lnl, line, i);
    i += lnl;
    line[i++] = ' ';
    line[i++] = '"';
    fn.getChars(0, fnl, line, i);
    i += fnl;
    line[i++] = '"';
    line[i++] = '\n';
    line[i]   = 0;
    lineLength    = i;
    linePosition  = 0;
    genFirst = false;
  }

  private void printLine()
  {
    System.out.print("** `<l ");
    System.out.print(lineNumber);
    System.out.print(":");
    for (int i = 0; i < lineLength; i++)
      System.out.print(line[i]);
  }

  /**
   * Return from an included file.
   */
  private CReader oldFile()
  {
    if (hstack.empty())
      return null;

    CReader xcurReader = hstack.pop();
    if (CReader.classTrace || classTrace)
      System.out.println("Reading: " + xcurReader.getFilename());

    lineLength    = 0;
    linePosition  = 0;
    lineNumber    = xcurReader.getLineNumber();
    return xcurReader;
  }

  /**
   * Generate an error message.
   */
  private void genError(int errno, String msg, int pos, boolean display)
  {
    Msg.reportWarning(errno, curReader.getFilename(), lineNumber, pos, msg);
    if (!display)
      return;

    if (lineLength > line.length)
      lineLength = line.length;
    System.err.println(new String(line, 0, lineLength - 1));
    while (pos > 4) {
      System.err.print("____");
      pos -= 4;
    }
    System.err.print("______".substring(0, pos));
    System.err.println("^");
  }

  /**
   * Reset the current line to be an empty line.
   */
  private void resetLine()
  {
    line[0]      = '\n';
    line[1]      = 0;
    lineLength   = 1;
    linePosition = 0;
  }

  /**
   * Define a simple macro to be used by the pre-processor.
   * A simple macro has no parameters.
   * @param name is the macro name
   * @param text is the replacement text
   */
  public void addMacro(String name, String text)
  {
    addMacro(M_NORMAL, name, text);
  }

  /**
   * Define a macro.
   */
  private void addMacro(int type, String name, String text)
  {
    int   h = hash(name);
    Macro m = macros[h];
    Macro n = null;

    if (type == M_NORMAL)
      n = new NMacro(name, text, null);
    else
      n = new SMacro(type, name, null);

    n.next = m;
    macros[h] = n;

    if (macroText != null)
      macroText.add(n.display());
  }

  /**
   * Return a hash value for the name specified.
   * @param offset is the offset in name of the first character to include
   * @param length is the number of characters to include
   */
  private int hash(char[] name, int offset, int length)
  {
    if (length == 0)
      return 0;
    int h = 0;
    // name[0]*31^(n) + name[1]*31^(n-1) + ... + name[n]
    int n = length - 1;
    for (int i = 0; i < n; i++)
      h += name[offset + i] * 31 ^ (n - i);

    return (h + name[offset + n]) & 0xff;
  }

  /**
   * Return a hash value for the name specified.
   */
  private int hash(String name)
  {
    int h = 0;
    int n = name.length() - 1;
    // name[0]*31^(n) + name[1]*31^(n-1) + ... + name[n]
    for (int i = 0; i < n; i++)
      h += name.charAt(i) * 31 ^ (n - i);
    return (h + name.charAt(n)) & 0xff;
  }

  /**
   * Return the next if-state based on the current if-state
   * and the current if-test.
   */
  private int nextIfState(boolean bypass, int state)
  {
    int ns = stateTransition[state + (bypass ? 3 : 0)];
    return stateTransition[state + (bypass ? 3 : 0)];
  }

  /**
   * Push this if-state onto the if-stack.
   */
  private void ifStackPush(int state)
  {
    if (ifstackptr >= ifstack.length) {
      byte[] ns = new byte[ifstackptr + 5];
      System.arraycopy(ifstack, 0, ns, 0, ifstackptr);
      ifstack = ns;
    }
    ifstack[ifstackptr++] = (byte) state;
  }

  /**
   * Return the state of the enclosing if of this if
   * and pop the if-stack.
   */
  private byte ifStackPop() throws Err
  {
    if (ifstackptr <= 0)
      throw new Err(Msg.MSG_Endif_without_if, 0);

    ifstackptr--;
    return ifstack[ifstackptr];
  }

  /**
   * Return the state of the enclosing if of this if.
   */
  private byte ifStackPeek()
  {
    if (ifstackptr <= 0)
      return IF_YES;

    return ifstack[ifstackptr - 1];
  }

  /**
   * Convert the characters in <code>line</code> to tokens in the <code>tokens</code> array.
   * Each token is recorded using four integer vales:
   * <ol>
   * <li> The type of token - PP_OP, etc.
   * <li> The index of the starting character in <code>line</code>.
   * <li> The index of the first character in <code>line</code> after the token.
   * <li> Additional information such as the operator for a token of type PP_OP.
   * </ol>
   * @param start is the starting position in <code>line</code> of the expression
   * @param end is one past the end scan position
   */
  private void tokenize(int start, int end)
  {
    int i = start;

    tokenPtr = 0;
    while (true) {
      i = extractToken(i, end);
      if (ppType == PP_NONE)
        return;

      // Record a token in the <code>tokens</code> array.

      if (tokenPtr >= ttype.length) {
        byte[] ntt = new byte[tokenPtr * 2];
        System.arraycopy(ttype, 0, ntt, 0, tokenPtr);
        ttype = ntt;
        int[] nts = new int[tokenPtr * 2];
        System.arraycopy(tstart, 0, nts, 0, tokenPtr);
        tstart = nts;
        int[] nte = new int[tokenPtr * 2];
        System.arraycopy(tend, 0, nte, 0, tokenPtr);
        tend = nte;
        long[] ntv = new long[tokenPtr * 2];
        System.arraycopy(tvalue, 0, ntv, 0, tokenPtr);
        tvalue = ntv;
      }
      ttype[tokenPtr]  = ppType;
      tstart[tokenPtr] = ppStart;
      tend[tokenPtr]   = ppEnd;
      tvalue[tokenPtr] = ppSubType;
      tokenPtr++;
    }
  }

  private void printTokens(int start, int fin, int[] precedence)
  {
    for (int k = start; k < fin; k++) {
      System.out.print("  ");
      System.out.print(k);
      System.out.print("   ");
      System.out.print(precedence[k]);
      System.out.print("   ");
      System.out.println(displayToken(k));
    }
  }

  /**
   * Display a token for debugging.
   * @param p specifies the token in the <code>tokens</code> array
   */
  private String displayToken(int p)
  {
    if (p < 0)
      return "";

    int  s = tstart[p];
    int  e = tend[p];
    long v = tvalue[p];

    switch (ttype[p]) {
    case PP_NONE    : return "N";
    case PP_IDENT   : return new String(line, s, e - s);
    case PP_HEADER  : return "\"" + new String(line, s, e - s) + "\"";
    case PP_NUM     : return Long.toString(v);
    case PP_STRING  : return "\"" + new String(line, s, e - s) + "\"";
    case PP_CHAR    : return "C(" + Integer.toString((int) v) + ")";
    case PP_OP      : return "Op(" + ops[(int) v] + ")";
    case PP_PUNCT   : return "P(" + ops[(int) v] + ")";
    case PP_OTHER   : return "O(" + Integer.toString((int) v) + ")";
    case PP_ERROR   : return "err";
    default:
      return "??";
    }
  }

  /**
   * Display a token for debugging.
   * The token is specified by <code>ppType</code>, et al.
   * @param msg is a string to preappend to the string result
   */
  private String displayToken(String msg)
  {
    StringBuffer buf = new StringBuffer(msg);
    buf.append(' ');
    buf.append(types[ppType]);
    buf.append(' ');
    switch (ppType) {
    case PP_NONE    :
      break;
    case PP_IDENT   :
      buf.append(new String(line, ppStart, ppEnd - ppStart));
      break;
    case PP_HEADER  :
      buf.append("\"");
      buf.append(new String(line, ppStart, ppEnd - ppStart));
      buf.append("\"");
      break;
    case PP_NUM     : 
      buf.append(ppSubType);
      break;
    case PP_STRING  :
      buf.append("\"");
      buf.append(new String(line, ppStart, ppEnd - ppStart));
      buf.append("\"");
      break;
    case PP_CHAR    :
      buf.append("C(");
      buf.append(new String(line, ppStart, ppEnd - ppStart));
      buf.append(")");
      break;
    case PP_OP      :
      buf.append("Op(");
      buf.append(ops[(int) ppSubType]);
      buf.append(")");
      break;
    case PP_PUNCT   :
      buf.append("P(");
      buf.append(ops[(int) ppSubType]);
      buf.append(")");
      break;
    case PP_OTHER   :
      buf.append("O(");
      buf.append(ppSubType);
      buf.append(")");
      break;
    case PP_ERROR   :
      buf.append("err");
      break;
    default:
      buf.append("??");
      break;
    }
    return buf.toString();
  }

  /**
   * Compute the value of the expression.
   * The value is returned in <code>ppSubType</code>.
   * @param start is the starting position in <code>line</code> of the expression
   * @param end is one past the end scan position
   * @return the scan position after computation
   */
  private int computeConstantExpression(int start, int end) throws Err, java.io.IOException
  {
    int i = start;

    // First replace any macro calls.

    end = performMacroSubstitution(i, end, defMacro);
    end = performMacroSubstitution(i, end, null);

    // Tokenise the expression to make parsing easier.

    tokenize(i, end);

    // Compute the precedence of each operator.
    // The precedence is increased when inside a paren expression.

    int[] precedence = new int[tokenPtr];
    int   pv = 0;
    for (int k = 0; k < tokenPtr; k++) {
      switch (ttype[k]) {
      case PP_NONE:
        break;
      case PP_IDENT:
        ttype[k] = PP_NUM;
        tvalue[k] = 0;
        precedence[k] = pv;
        break;
      case PP_CHAR:
        ttype[k] = PP_NUM;
        precedence[k] = pv;
        break;
      case PP_NUM:
        precedence[k] = pv;
        break;
      case PP_OP:
        precedence[k] = pv + precedences[(int) tvalue[k]];
        break;
      case PP_PUNCT:
        if (tvalue[k] == OP_LPAREN)
          pv += 256;
        else if (tvalue[k] == OP_RPAREN) {
          pv -= 256;
          if (pv < 0)
            throw new Err(Msg.MSG_Too_many_closing_parens, ttype[k + 1]);
        }
        ttype[k] = PP_NONE;
        break;
      default:
        if (CReader.classTrace || classTrace)
          System.out.println("** dt " + displayToken(k));
        throw new Err(Msg.MSG_Not_an_integer_constant, ttype[k + 1]);
      }
    }

    if (pv != 0)
      throw new Err(Msg.MSG_Too_few_closing_parens, i);

    int k = 0;
    while (true) {
      // Find the operator with a precedence greater than the following operator
      // and perform the operation.

      // Find an operator.

      int lop = -1;
      int lp = 0;
      while (k < tokenPtr) {
        if (ttype[k] == PP_OP) {
          lp = precedence[k];
          lop = k;
          k++;
          break;
        }
        k++;
      }

      if (lop < 0)
        break;

      // Find the successor operator.

      int rop = -1;
      int rp = -1;
      while (k < tokenPtr) {
        if (ttype[k] == PP_OP) {
          rp = precedence[k];
          rop = k;
          k++;

          if (lp >= rp)
            break;

          lop = rop;
          lp = rp;
          continue;
        }
        k++;
      }

      // Find any left operand.

      int la = lop - 1;
      while (la >= 0) {
        if (ttype[la] != PP_NONE)
          break;
        la--;
      }

      // Find any right operand.

      int ra = lop + 1;
      while (ra < tokenPtr) {
        if (ttype[ra] != PP_NONE)
          break;
        ra++;
      }

      if ((la >= 0) && ppValue[ttype[la]]) {
        if ((ra < tokenPtr) && ppValue[ttype[ra]]) {
          performDyadicOp(lop, la, ra);
        } else {
          performLeftMonadicOp(lop, la);
        }
      } else if (ra >= 0) {
        performRightMonadicOp(lop, ra);
      } else {
        k = lop + 1;
        continue;
      }

      k = 0; // Do it again.
    }

    k = 0;
    int result = -1;
    while (k < tokenPtr) {
      if (ttype[k] != PP_NONE) {
        result = k;
        break;
      }
      k++;
    }

    if ((result < 0) || (ttype[result] != PP_NUM)) {
      System.out.println("** res " + result + " " + (result >= 0 ? displayToken(result) : ""));
      System.out.println(" " + new String(line, 0, end));
      throw new Err(Msg.MSG_Invalid_expression, start);
    }

    ppStart   = i;
    ppSubType = tvalue[result];
    ppType    = PP_NUM;

    return end;
  }

  private void performDyadicOp(int op, int la, int ra)
  {
    if (ttype[la] == PP_IDENT) {
      ttype[la] = PP_NUM;
      tvalue[la] = 0;
    }

    if (ttype[ra] == PP_IDENT) {
      ttype[ra] = PP_NUM;
      tvalue[ra] = 0;
    }

    if ((tvalue[op] == OP_CAND) && (ttype[la] == PP_NUM)  && (tvalue[la] == 0)) {
      ttype[ra] = PP_NONE;
      ttype[op] = PP_NONE;
      return;
    }

    if ((tvalue[op] == OP_COR) && (ttype[la] == PP_NUM)  && (tvalue[la] != 0)) {
      ttype[ra] = PP_NONE;
      ttype[op] = PP_NONE;
      return;
    }

    if ((ttype[la] != PP_NUM) || (ttype[ra] != PP_NUM)) {
      ttype[la] = PP_ERROR;
      ttype[ra] = PP_NONE;
      ttype[op] = PP_NONE;
      return;
    }

    switch ((int) tvalue[op]) {
    case OP_AND         : tvalue[la] = tvalue[la] & tvalue[ra]; break;
    case OP_MULT        : tvalue[la] = tvalue[la] * tvalue[ra]; break;
    case OP_ADD         : tvalue[la] = tvalue[la] + tvalue[ra]; break;
    case OP_SUB         : tvalue[la] = tvalue[la] - tvalue[ra]; break;
    case OP_DIV         : tvalue[la] = tvalue[la] / tvalue[ra]; break;
    case OP_MOD         : tvalue[la] = tvalue[la] % tvalue[ra]; break;
    case OP_LSHIFT      : tvalue[la] = tvalue[la] << tvalue[ra]; break;
    case OP_RSHIFT      : tvalue[la] = tvalue[la] >> tvalue[ra]; break;
    case OP_LT          : tvalue[la] = (tvalue[la] < tvalue[ra]) ? 1 : 0; break;
    case OP_GT          : tvalue[la] = (tvalue[la] > tvalue[ra]) ? 1 : 0; break;
    case OP_LE          : tvalue[la] = (tvalue[la] <= tvalue[ra]) ? 1 : 0; break;
    case OP_GE          : tvalue[la] = (tvalue[la] >= tvalue[ra]) ? 1 : 0; break;
    case OP_ASSIGN      : // Fall through.
    case OP_EQ          : tvalue[la] = (tvalue[la] == tvalue[ra]) ? 1 : 0; break;
    case OP_NE          : tvalue[la] = (tvalue[la] != tvalue[ra]) ? 1 : 0; break;
    case OP_XOR         : tvalue[la] = tvalue[la] ^ tvalue[ra]; break;
    case OP_OR          : tvalue[la] = tvalue[la] | tvalue[ra]; break;
    case OP_CAND        : tvalue[la] = ((tvalue[la] != 0) && (tvalue[ra] != 0)) ? 1 : 0; break;
    case OP_COR         : tvalue[la] = ((tvalue[la] != 0) || (tvalue[ra] != 0)) ? 1 : 0; break;
    case OP_SELECT      :
      int k = ra + 1; 
      while (ttype[k] == PP_NONE)
        k++;
      if ((ttype[k] == PP_OP) && (tvalue[k] == OP_COLON)) {
        tvalue[k] = (tvalue[la] == 0) ? OP_OR : ((tvalue[ra] == 0) ? OP_AND : OP_OR);
        tvalue[la] = tvalue[la] & tvalue[ra];
        break;
      }
      ttype[la] = PP_ERROR;
      ttype[ra] = PP_NONE;
      ttype[op] = PP_NONE;
      return;
    default:
      ttype[la] = PP_ERROR;
      ttype[ra] = PP_NONE;
      ttype[op] = PP_NONE;
      return;
    }

    ttype[ra] = PP_NONE;
    ttype[op] = PP_NONE;
  }

  private void performLeftMonadicOp(int op, int ra)
  {
    if (ttype[ra] == PP_IDENT) {
      ttype[ra] = PP_NUM;
      tvalue[ra] = 0;
    }

    if (ttype[ra] != PP_NUM) {
      System.out.println("** err1");
      ttype[ra] = PP_ERROR;
      ttype[op] = PP_NONE;
      return;
    }

    switch ((int) tvalue[op]) {
    case OP_DEC: tvalue[ra] = tvalue[ra] - 1; break;
    case OP_INC: tvalue[ra] = tvalue[ra] + 1; break;
    default:
      ttype[ra] = PP_ERROR;
      break;
    }

    ttype[op] = PP_NONE;
  }

  private void performRightMonadicOp(int op, int ra)
  {
    if (ttype[ra] == PP_IDENT) {
      ttype[ra] = PP_NUM;
      tvalue[ra] = 0;
    }

    if (ttype[ra] != PP_NUM) {
      System.out.println("** err2");
      ttype[ra] = PP_ERROR;
      ttype[op] = PP_NONE;
      return;
    }

    switch ((int) tvalue[op]) {
    case OP_DEC       : tvalue[ra] = tvalue[ra] - 1; break;
    case OP_INC       : tvalue[ra] = tvalue[ra] + 1; break;
    case OP_COMPLEMENT: tvalue[ra] = ~tvalue[ra]; break;
    case OP_NOT       : tvalue[ra] = (tvalue[ra] == 0) ? 1 : 0; break;
    case OP_SUB       : tvalue[ra] = -tvalue[ra]; break;
    case OP_ADD       : break;
    default:
      System.out.println("ifs p prmo 3 " + displayToken(op));
      ttype[ra] = PP_ERROR;
      break;
    }

    ttype[op] = PP_NONE;

    return;
  }

  /**
   * Close the files associated with this preprocessor.
   */
  public void close() throws java.io.IOException
  {
    if (curReader != null) {
      curReader.close();
      curReader = null;
    }

    while (!hstack.empty()) {
      CReader reader = hstack.pop();
      reader.close();
    }
  }

  /**
   * Read characters into a portion of an array.
   * @param cbuf - destination buffer
   * @param off - offset at which to start storing characters
   * @param len - maximum number of characters to read
   * @return the number of bytes read, or -1 if the end of the stream has already been reached
   */
  public int read(char[] cbuf, int off, int len) throws java.io.IOException
  {
    if ((linePosition >= lineLength) && readLineInt())
      return -1;

    for (int i = 0; i < len; i++) {
      if (linePosition > lineLength)
        return i;
      char c = line[linePosition++];
      cbuf[i + off] = c;
    }
    return len;
  }

  /**
   * Read a line of text. A line is considered to be terminated by any one of a line feed
   * ('\n'), a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
   * @return a String containing the contents of the line, not including any line-termination
   * characters, or null if the end of the stream has been reached 
   */
  public String readLine() throws java.io.IOException
  {
    if ((linePosition >= lineLength) && readLineInt())
      return null;
    String str = new String(line, linePosition, lineLength - linePosition - 1);
    lineLength = 0;
    return str;
  }

  /**
   * Read in one line and process it.  Comments are eliminated and
   * pre-processor commands are performed.  Whitespace characters
   * except '\n' and '\r' are converted to ' ' unless inside double
   * quotes.
   * @return true if no more lines
   */
  private boolean readLineInt() throws java.io.IOException
  {
    if (genFirst) {
      genLine(curReader.getFilename(), lineNumber);
      if (CReader.classTrace || classTrace)
        System.out.println("CP (" + lineNumber + ":" + ifState + "):" + new String(line, 0, lineLength - 1));
      return false;
    }

    lineLength = 0;
    linePosition = 0;

    boolean done = false;
    if (slinecnt > 0) {
      // Present the newlines one at a time so that the compiler can
      // determine the correct line number.
      line[0] = '\n';
      line[1] = 0;
      lineLength = 1;
      slinecnt--;
      lineNumber++;
    } else if (slineLength > 0) {
      char[] t = line;
      line = sline;
      sline = t;
      lineLength = slineLength;
      slineLength = 0;
    }

    done = readAndRemoveComments(0);

    // We have to track the ifs even if they are skipped
    // so that we known which endif is for the if causing the skipping.

    if (processDirective() || bypass)
      resetLine();
    else {
      // If it is not a command, do the macro substitution.
      try {
        performMacroSubstitution(0, lineLength, null);
      } catch (Err ex) {
        genError(ex.errno, null, ex.position, true);
        // The line is not reset here because the user will want to see it.
      } catch (java.lang.Error ex) {
        ex.printStackTrace();
        // The line is not reset here because the user will want to see it.
      }
    }

    if (CReader.classTrace || classTrace)
      System.out.println("CP (" + lineNumber + ":" + ifState + "):" + new String(line, 0, lineLength));

    return done;
  }

  private boolean readAndRemoveComments(int linepos) throws java.io.IOException
  {
    if (curReader == null) {
      lineLength = 1;
      line[0] = 0;
      linePosition = 0;
      return true; // We have read everything!
    }

    boolean inQuote   = false; /* True if in the middle of a string. */
    boolean inComment = false; /* True if in the middle of a comment. */

    do {
      // We read from the real reader one line at a time.

      if (linepos >= lineLength) {
        if (curReader.fillLine()) {
          // The current file was empty - return to the previous one.

          if (inQuote) {
            genError(Msg.MSG_Improper_string_termination, null, 1, true);
            inQuote = false;
          }

          if (inComment) {
            genError(Msg.MSG_Improper_comment_termination, null, 1, true);
            inComment = false;
          }

          try {
            curReader.close();
          } catch(java.lang.Throwable ex) {
          }

          curReader = oldFile();

          if (curReader == null) {
            lineLength = 1;
            line[0] = 0;
            linePosition = 0;
            return true; // We have read everything!
          }

          genFirst = true;  // Generate a line to indicate the current source line.
          resetLine();

          return false;
        }

        // Get the line from the real reader.

        int rl = lineLength + curReader.getLineLength();
        if (rl >= line.length) {
          char[] nl = new char[rl * 2];
          if (lineLength > 0)
            System.arraycopy(line, 0, nl, 0, lineLength);
          line = nl;
        }
        lineLength = rl;

        curReader.getLineArray(line, linepos);
        for (int ii = lineLength; ii < line.length; ii++)
          line[ii] = 0;

        lineNumber = curReader.getLineNumber();
      }

      //  Remove the comments.

      int i = linepos;
      comm:
      for (; i < lineLength; i++) {
        if (inComment) {
          for (int j = i; j < lineLength; j++) {
            if (line[j] != '*')
              continue;
            if (line[j + 1] != '/')
              continue;
            System.arraycopy(line, j + 2, line, i, lineLength - j - 2);
            lineLength -= j + 2 - i;
            line[lineLength] = 0;
            assert (lineLength > 0) : "Empty line.";
            inComment = false;
            i--;
            continue comm;
          }

          // The comment did not end on this line.

          inComment = true;
          line[i++] = '\n';
          line[i] = 0;
          lineLength = i;
          break;
        }

        char c = line[i];
        if (c == '"') {
          if (i > 0) {
            if (line[i - 1] == '\\') {
              int cnt = 1;
              for (int ii = i - 2; ii >= 0; ii--)
                if (line[ii] == '\\')
                  cnt++;
                else
                  break;
              if ((cnt & 1) != 0)
                continue;
            } else if ((i < (lineLength - 1)) && (line[i + 1] == '\'') && (line[i - 1] == '\''))
              continue;
            inQuote ^= true;
          } else
            inQuote = true;
          continue;
        }

        if (inQuote)
          continue;

        if (Character.isWhitespace(c)) {
          if ((c != '\n') && (c != '\r'))
            line[i] = ' ';
          continue;
        }

        if (c != '/')
          continue;

        if ((line[i + 1] == '/') && !strictANSIC) {
          line[i++] = '\n';
          line[i] = 0;
          lineLength = i;
          break;
        }

        if (line[i + 1] != '*')
          continue;

        line[i] = ' ';
        line[i + 1] = ' ';
        for (int j = i + 1; j < lineLength; j++) {
          if (line[j] != '*')
            continue;
          if (line[j + 1] != '/')
            continue;
          System.arraycopy(line, j + 2, line, i + 1, lineLength - j - 2);
          lineLength -= j + 2 - i - 1;
          line[lineLength] = 0;
          assert (lineLength > 0) : "Empty line.";
          inComment = false;
          continue comm;
        }

        // The comment did not end on this line.

        inComment = true;
        line[i++] = '\n';
        line[i] = 0;
        lineLength = i;
        break;
      }

      linepos = i;
    } while (inQuote || inComment);

    return false;
  }

  private char[] extendLine(char[] line, int newLength)
  {
    if (newLength < line.length)
      return line;

    char[] nc = new char[newLength + 10];
    System.arraycopy(line, 0, nc, 0, line.length);
    return nc;
  }

  private void dumpLine(String msg, char[] line, int lineLength, int ptr)
  {
    System.out.print(msg);
    System.out.println(lineLength);
    int  min  = ptr;
    int  max  = lineLength;
    char minc = 'E';
    char maxc = 'V';
    if (lineLength < min) {
      min = lineLength;
      max = ptr;
      minc = 'V';
      maxc = 'E';
    }

    int ii = 0;
    for (; ii < min; ii++)
      System.out.print(" ");
    ii++;
    System.out.print(minc);
    for (; ii < max; ii++)
      System.out.print(" ");
    if (max > min)
      System.out.println(maxc);
    else
      System.out.println("");
    int l = line.length;
    for (int i = line.length - 1; i >= 0; i--) {
      if (line[i] == 0)
        continue;
      l = i + 1;
      break;
    }
    for (int i = 0; i < l; i++) {
      char c = line[i];
      if (c == '\n')
        c = '#';
      else if (c == 0)
        c = 'Z';
      System.out.print(c);
    }
    System.out.println("");
  }

  private boolean traceLine(String msg, char[] line, int lineLength, int ptr)
  {
    if (false && classTrace)
      dumpLine(msg, line, lineLength, ptr);
    return true;
  }

  /**
   * Replace any macros in the line.
   * Macros are expanded in place to avoid memory allocations.
   * This logic must handle the following pathological constructions:
   * <pre>
   * #define x y,x
   * #define M(x) z,M(x)
   * #define MAX(x,y) ((x)<(y))
   * #define s ustr.s
   * #define y ftn(xx)
   * #define xx 1
   * #define	__P_C(__dname__)	_P##__dname__
   * #define __(__args__)	__args__
   * #define	rand_r(__a) _Prand_r(__a)
   * #define W(a) a+1
   * #define B A,1
   * #define A B,2
   * #define PerlLIO_unlink(file)		unlink((file))
   * #define UNLINK PerlLIO_unlink
   * UNLINK(PL_oldname);
   *   int M(2);
   *   MAX(MAX(a,b),c)
   *   y+s;
   *   int x = 0;
   *   extern int	__P_C(rand_r) __((unsigned int *));
   *   ustr.s;
   *   W(s);
   *   A,A;
   * </pre>
   * The correct results are:
   * <pre>
   *   unlink ((PL_oldname));
   *   int  z,M(2);
   *   ((((a)<(b)))<(c))
   *   ftn(1)+ustr.s;
   *   int  ftn(1),x = 0;
   *   extern int       _Prand_r   ( unsigned int * ) ;
   *   ustr.ustr.s;
   *   ustr.s + 1;
   *   A,1,2,A,1,2;
   * </pre>
   * @param start is the current scan position
   * @param end is one past the end scan position
   * @param tempMacros is a list of additional macros to use
   * @return the scan position after macro substitution
   */
  private int performMacroSubstitution(int start, int end, Macro tempMacros) throws Err, java.io.IOException
  {
    assert traceLine("** enter ", line, start, end) : "";

    // We need to do the processing twice in some cases.  If we need
    // to replace the macro arguments, that must be done first before
    // any other macro arguments are replaced.

    int brp = resetPtr;
    int i   = start;
    while (true) {
      if (resetPtr > 0) {
        for (int j = brp; j < resetPtr; j++) {
          if ((resetMacro[j] != null) && (resetPos[j] <= i)) {
            resetMacro[j].ignore = resetFlg[j];
            resetMacro[j] = null;
          }
        }
      }

      int si = i;
      i = extractToken(i, end);

      if (ppType == PP_NONE)
        break; // The line is finished.

      if (ppType != PP_IDENT)
        continue;

      int   ts = ppStart;
      int   te = ppEnd;
      int   l  = te - ts;
      int   h  = hash(line, ts, l);
      Macro fm = null;

        // Find if the token matches a macro name.

      for (Macro m = macros[h]; m != null; m = m.next) {
        if (m.ignore)
          continue;

        if (!m.match(line, ts, l))
          continue;

        if (m.inUse)
          m = m.copy();

        fm = m;
        break;
      }

      if (fm == null) {
        if ((tempMacros != null) && tempMacros.match(line, ts, l))
          fm = tempMacros;
      }

      int sizeReplacementText = 0;
      int more                = 0;

      if (fm == null)
        continue;

      assert traceLine("   " + fm + " ", line, ts, end) : "";

      // Found a macro to use.
      // Extract any arguments.
      // Use the argument values as the replacement text for the "macros" defined for the macro parameters.
      // Very clever :-)

      AMacro rem = fm.replaces;
      if (fm == defMacro) {
        if (te >= end)
          continue; // It's not a macro call.

        while (isWhitespace(line[te]))
          te++;

        if ((line[te] == '(') && (rem != null))
          te = extractMacroArgs(rem, te, end);
        else if (rem != null) {
          int tte = extractToken(te, end);
          rem.setReplacementText(te, tte - te);
          te = tte;
        }
      } else if (fm.definedWithParens()) {
        try {
          te = extractMacroArgs(rem, te, end);
        } catch(Err er) {
          continue; // It's not a macro call.
        }
      }

      i = te;

      // The replacement text is generated at the end of the current line
      // and then moved into the proper place.

      int expansionPtr      = end + 1;
      int sizeRemainingText = end - te; // Size of remaining text.

      sizeReplacementText = fm.getReplacementTextLength();
      line = extendLine(line, expansionPtr + sizeReplacementText);
      fm.getReplacementChars(line, expansionPtr); // Place replacement text in the expansion buffer.

      if (rem != null) { // Replace the current macro parameters with the actual macro arguments.
        boolean sinUse = fm.inUse;
        int     x      = lineLength;
        fm.inUse = true; // Avoid contention if this macro is used in one of its arguments.
        sizeReplacementText = performMacroSubstitution(rem, expansionPtr, expansionPtr + sizeReplacementText) - expansionPtr;
        lineLength = x;
        fm.inUse = sinUse;
      }

      if (fm.definedWithParens() || fm.isRecursive) { // Perform macro replacement on the expanded macro text.
        boolean signore = fm.ignore;
        int     x       = lineLength;
        fm.ignore = fm.isRecursive;
        sizeReplacementText = performMacroSubstitution(expansionPtr, expansionPtr + sizeReplacementText, null) - expansionPtr;
        fm.ignore = signore;
        lineLength = x;
      }

      more = sizeReplacementText + ts - te; // Difference between original and replacement.

      line = extendLine(line, expansionPtr + sizeReplacementText + sizeRemainingText);

      // Move the replacement text back into the original line.

      if (more <= 0) { // Replacement text is smaller or the same size.
        System.arraycopy(line, expansionPtr, line, ts, sizeReplacementText);
        if (more < 0) { // Shorten the line.
          System.arraycopy(line, te, line, ts + sizeReplacementText, sizeRemainingText);
          lineLength += more;
        }
      } else { // Lengthen the line.
        // Copy the remaining text after the replacement text
        // and move the replacement and remaining text text down over the macro call.
        System.arraycopy(line, te, line, expansionPtr + sizeReplacementText, sizeRemainingText);
        System.arraycopy(line, expansionPtr, line, ts, sizeReplacementText + sizeRemainingText);
        line[ts + sizeReplacementText + sizeRemainingText] = '\n';
        line[ts + sizeReplacementText + sizeRemainingText + 1] = 0;
        lineLength = ts + sizeReplacementText + sizeRemainingText;
      }

      end += more;
      i = (fm.definedWithParens() || fm.isRecursive) ? ts + sizeReplacementText : si;

      int ind = resetPtr;
      for (int j = brp; j < resetPtr; j++) {
        if (resetMacro[j] == null) {
          ind = j;
        } else if (resetPos[j] > i)
          resetPos[j] += more;
      }

      if (ind >= resetPtr) {
        if (resetPtr >= resetMacro.length) {
          Macro[] nm = new Macro[resetPtr * 2];
          System.arraycopy(resetMacro, 0, nm, 0, resetPtr);
          resetMacro = nm;
          boolean[] nf = new boolean[resetPtr * 2];
          System.arraycopy(resetFlg, 0, nf, 0, resetPtr);
          resetFlg = nf;
          int[] np = new int[resetPtr * 2];
          System.arraycopy(resetPos, 0, np, 0, resetPtr);
          resetPos = np;
        }
        resetPtr++;
      }
      resetMacro[ind] = fm;
      resetFlg[ind] = fm.ignore;
      fm.ignore = true;
      resetPos[ind] = ts + sizeReplacementText;
    }

    for (int j = brp; j < resetPtr; j++)
      if (resetMacro[j] != null) {
        resetMacro[j].ignore = resetFlg[j];
        resetMacro[j] = null;
      }
    resetPtr = brp;

    assert traceLine("** macro exit ", line, start, end) : "" ;

    return end;
  }

  /**
   * Replace macro parameters with the actual arguments.
   */
  private int performMacroSubstitution(AMacro macros, int start, int end) throws Err
  {
    // We need to do the processing twice in some cases.  If we need
    // to replace the macro arguments, that must be done first before
    // any other macro arguments are replaced.

    boolean shflag = false; // Record if a '#' was encountered.
    boolean dhflag = false; // Record if a '##' was encountered.
    int     hstart = 0;
    int     hend   = 0;

    int i = start;
    while (true) {
      int si = i;
      i = extractToken(i, end);

      if (ppType == PP_NONE)
        break; // The line is finished.

      if (ppType != PP_IDENT) {
        shflag = false;
        dhflag = false;

        if (ppType == PP_OP) {
          if (ppSubType == OP_HATCH) {
            shflag = true;
            hstart = ppStart;
            hend = ppEnd;
          } else if (ppSubType == OP_DHATCH) {
            dhflag = true;
            while ((ppStart > 0) && isWhitespace(line[ppStart - 1])) ppStart--;
            while (isWhitespace(line[ppEnd])) ppEnd++;
            if (ppEnd > i)
              i = ppEnd;
            hstart = ppStart;
            hend = ppEnd;
          }
        }
        continue;
      }

      int ts = ppStart;
      int te = ppEnd;
      int l  = te - ts;

      Macro fm = null;
      for (Macro m = macros; m != null; m = m.next) {
        if (m.ignore)
          continue;

        if (!m.match(line, ts, l))
          continue;

        if (m.inUse)
          m = m.copy();

        fm = m;
        break;
      }

      int more = 0;
      int sizeReplacementText = 0;
      if (fm != null) {
        i = te;

        // The replacement text is generated at the end of the current line
        // and then moved into the proper place.

        int expansionPtr      = end + 1;
        int sizeRemainingText = end - te; // Size of remaining text.

        sizeReplacementText = fm.getReplacementTextLength();
        more                = sizeReplacementText + ts - te; // Difference between original and replacement.

        line = extendLine(line, expansionPtr + sizeReplacementText + sizeRemainingText);
        fm.getReplacementChars(line, expansionPtr); // Place replacement text in the expansion buffer.

        // Move the replacement text back into the original line.

        if (more <= 0) { // Replacement text is smaller or the same size.
          System.arraycopy(line, expansionPtr, line, ts, sizeReplacementText);
          if (more < 0) { // Shorten the line.
            System.arraycopy(line, te, line, ts + sizeReplacementText, sizeRemainingText);
            lineLength += more;
          }
        } else { // Lengthen the line.
          // Copy the remaining text after the replacement text
          // and move the replacement and remaining text text down over the macro call.
          System.arraycopy(line, te, line, expansionPtr + sizeReplacementText, sizeRemainingText);
          System.arraycopy(line, expansionPtr, line, ts, sizeReplacementText + sizeRemainingText);
          line[ts + sizeReplacementText + sizeRemainingText] = '\n';
          line[ts + sizeReplacementText + sizeRemainingText + 1] = 0;
          lineLength = ts + sizeReplacementText + sizeRemainingText;
        }

        end += more;
        i = ts + sizeReplacementText;
      }

      if (shflag) { // Put quotes around the macro argument.
        line[hstart] = '"';
        int e = hstart + sizeReplacementText + 1;
        line = extendLine(line, end + more + 1);
        System.arraycopy(line, e, line, e + 1, end - e + more);
        line[e] = '"';
        end++;
        i++;
        lineLength++;
      } else if (dhflag) { // Catenate the macro argument with the preceding text.
        int ehend = end - hend;
        while (isWhitespace(line[hend]) && (hend < end)) hend++;
        System.arraycopy(line, hend, line, hstart, ehend);
        end -= hend - hstart;
        i = hstart;
        lineLength -= hend - hstart;
      }
      shflag = false;
      dhflag = false;
    }

    return end;
  }

  /**
   * Parse the macro call arguments and record them in the array.  If
   * the macro arguments extend over more than one line, they are
   * collected in a separate buffer.  Any characters left over after
   * the closing right paren of the macro call are then read from that
   * buffer the next time readLineInt() is called.
   * @param args is the list of Macros to hold the arguments.
   * @param start is the current scan position
   * @param end is one past the end scan position
   * @return the scan position after any macro arguments
   */
  private int extractMacroArgs(AMacro args, int start, int end) throws Err, java.io.IOException
  {
    boolean inExtendedArg = false;
    int     sll           = lineLength;
    int     sscan         = 0;
    int     send          = end;

    int i = start;
    if (i >= end)
      return i; // Not a macro call.

    while ((i < end) && isWhitespace(line[i]))
      i++;

    while (line[i] != '(') {
      if ((line[i] != '\n') || curReader.fillLine()) {
        if (inExtendedArg) {
          inExtendedArg = false;
          slineLength = lineLength - i;

          // Save any remaining characters for the next call to readLineInt().

          if (slineLength > 0)
            System.arraycopy(line, i, line, 0, slineLength);

          line[slineLength] = 0;

          // Restore the previous buffer.

          char[] t = line;
          line = sline;
          sline = t;
          i = sscan;
          lineLength = sll;
          end = send;
        }
        throw new Err(Msg.MSG_Not_a_macro_call, i);
      }

      int rl = curReader.getLineLength();
      inExtendedArg = true;
      sscan = i;
      sll = lineLength;

      if (sline == null)
        sline = new char[rl + 10];

      char[] t = sline;
      sline = line;
      line = t;

      slinecnt = -1;

      // Make sure the buffer is big enough.

      if (rl >= line.length)
        line = new char[rl + 10];

      // Place additional line in the buffer.

      lineLength = rl;
      curReader.getLineArray(line, 0);
      readAndRemoveComments(0);
      i = 0;
      end = rl;
      slinecnt++; // Conservation of newlines.
      while ((i < end) && isWhitespace(line[i]))
        i++;
    }

    i++;

    if (args == null) {
      while ((i < end) && isWhitespace(line[i]))
        i++;

      if (line[i] == ')')
        return i + 1;
    }

    boolean valid = false; // Was the terminating paren found.
    int     depth = 0;     // Nesting depth for parenthesis.
    int     str   = i;
    int     etr   = i;

    ascan:
    while (true) {
      int si = i;
      i = extractToken(i, end);
      if (ppType == PP_NONE) { // End of the stuff in the line.
        if (curReader.fillLine())
          break; // No more lines.

        // Macro argument extends over more than one line.  Use a
        // separate buffer to collect them.

        int rl = curReader.getLineLength();
        if (!inExtendedArg) {
          inExtendedArg = true;
          sscan = i;
          sll = lineLength;

          if (sline == null)
            sline = new char[rl + 10 + etr - str];

          char[] t = sline;
          sline = line;
          line = t;

          si = etr - str;
          lineLength = si;
          slinecnt = -1;

          // Move in any argument characters already scanned.

          System.arraycopy(sline, str, line, 0, si);

          etr = si;
          str = 0;
        }

        // Make sure the buffer is big enough.

        int nl = rl + lineLength;
        if (nl >= line.length) {
          char[] nline = new char[nl + 10];
          System.arraycopy(line, 0, nline, 0, lineLength);
          line = nline;
        }

        // Place additional line in the buffer.

        lineLength = si + rl;
        curReader.getLineArray(line, si);
        readAndRemoveComments(si);
        i = si;
        end = si + rl;
        slinecnt++; // Conservation of newlines.
        continue;
      }

      if (ppType == PP_PUNCT) {
        switch ((int) ppSubType) {
        case OP_LPAREN: depth++; break;
        case OP_RPAREN:
          if (depth == 0) {
            if (args == null)
              throw new Err(Msg.MSG_Too_many_macro_arguments, i);

            // Save the argument.

            args.setReplacementText(str, etr - str);
            args = (AMacro) args.next;
            valid = true;

            // If we used a separate buffer to collect the argument,
            // restore the original buffer.

            if (inExtendedArg) {
              inExtendedArg = false;
              slineLength = lineLength - i;

              // Save any remaining characters for the next call to readLineInt().

              if (slineLength > 0)
                System.arraycopy(line, i, line, 0, slineLength);

              line[slineLength] = 0;

              // Restore the previous buffer.

              char[] t = line;
              line = sline;
              sline = t;
              i = sscan;
              lineLength = sll;
              end = send;
            }

            //We are done if the ending right paren was found.

            break ascan;
          }
          depth--;
          break;
        case OP_COMMA:
          if (depth == 0) {
            if (args == null)
              throw new Err(Msg.MSG_Too_many_macro_arguments, i);

            // Save the argument.

            args.setReplacementText(str, etr - str);
            args = (AMacro) args.next;
            str = i;
          }
          break;
        }
      }
      etr = i;
    }

    if (inExtendedArg || !valid || (args != null))
      throw new Err(Msg.MSG_Too_few_macro_arguments, i);

    return i;
  }

  /**
   * Extract the next token from the line.
   * The token type is placed in <code>ppType</code>.
   * The token starting index in line is placed in <code>ppStart</code>.
   * The token index in line of the first character after the token is placed in <code>ppEnd</code>.
   * Additional information may be placed in <code>ppSubType</code>.
   * @param start is the current scan position
   * @param end is one past the end scan position
   * @return the scan position after the token
   */
  private int extractToken(int start, int end)
  {
    int i = start;
    ppType = PP_NONE;

    while ((i < end) && isWhitespace(line[i])) i++;

    if (i >= end)
      return i;

    char c = line[i];
    switch (c) {
    case '\n':
      return i;

    case '0': case '1': case '2': case '3': case '4':
    case '5': case '6': case '7': case '8': case '9':
      return extractInteger(i, end);
    case 'a': case 'b': case 'c': case 'd': case 'e':
    case 'f': case 'g': case 'h': case 'i': case 'j':
    case 'k': case 'l': case 'm': case 'n': case 'o':
    case 'p': case 'q': case 'r': case 's': case 't':
    case 'u': case 'v': case 'w': case 'x': case 'y':
    case 'z':
    case 'A': case 'B': case 'C': case 'D': case 'E':
    case 'F': case 'G': case 'H': case 'I': case 'J':
    case 'K':           case 'M': case 'N': case 'O':
    case 'P': case 'Q': case 'R': case 'S': case 'T':
    case 'U': case 'V': case 'W': case 'X': case 'Y':
    case 'Z':
    case '_':
      return extractIdentifier(i, end);
    case 'L':
      if (line[i + 1] != '\'')
        return extractIdentifier(i, end);
      return extractChar(i + 1, end);
    case '"':
      return extractString(i + 1, end, '"');
    case '\'':
      return extractChar(i, end);
    case '[':
      ppType = PP_PUNCT;
      ppSubType = OP_LBRACK;
      ppStart = i++;
      ppEnd = i;
      return i;
    case ']':
      ppType = PP_PUNCT;
      ppSubType = OP_RBRACK;
      ppStart = i++;
      ppEnd = i;
      return i;
    case '(':
      ppType = PP_PUNCT;
      ppSubType = OP_LPAREN;
      ppStart = i++;
      ppEnd = i;
      return i;
    case ')':
      ppType = PP_PUNCT;
      ppSubType = OP_RPAREN;
      ppStart = i++;
      ppEnd = i;
      return i;
    case '{':
      ppType = PP_PUNCT;
      ppSubType = OP_LBRACE;
      ppStart = i++;
      ppEnd = i;
      return i;
    case '}':
      ppType = PP_PUNCT;
      ppSubType = OP_RBRACE;
      ppStart = i++;
      ppEnd = i;
      return i;
    case ',':
      ppType = PP_PUNCT;
      ppSubType = OP_COMMA;
      ppStart = i++;
      ppEnd = i;
      return i;
    case ':':
      ppType = PP_OP;
      ppSubType = OP_COLON;
      ppStart = i++;
      ppEnd = i;
      return i;
    case '=': 
      ppType = PP_OP;
      ppStart = i++;
      if (line[i] == '=') {
        ppSubType = OP_EQ;
        ppEnd = ++i;
        return i;
      } else {
        ppType = PP_OP;
        ppSubType = OP_ASSIGN;
        ppEnd = i;
        return i;
      }
    case '.': 
      ppType = PP_OP;
      ppSubType = OP_DOT;
      ppStart = i++;
      ppEnd = i;
      return i;
    case '~':
      ppType = PP_OP;
      ppSubType = OP_COMPLEMENT;
      ppStart = i++;
      ppEnd = i;
      return i;
    case '?': 
      ppType = PP_OP;
      ppSubType = OP_SELECT;
      ppStart = i++;
      ppEnd = i;
      return i;
    case '^':
      ppType = PP_OP;
      ppStart = i++;
      if (line[i] == '=') {
        ppSubType = OP_XOR_ASSIGN;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_XOR;
        ppEnd = i;
        return i;
      }
    case '!':
      ppType = PP_OP;
      ppStart = i++;
      if (line[i] == '=') {
        ppSubType = OP_NE;
        ppEnd = ++i;
        return i;
      }
      ppSubType = OP_NOT;
      ppEnd = i;
      return i;
    case '*':
      ppType = PP_OP;
      ppStart = i++;
      if (line[i] == '=') {
        ppSubType = OP_MULT_ASSIGN;
        ppEnd = ++i;
        return i;
      }
      ppSubType = OP_MULT;
      ppEnd = i;
      return i;
    case '%':
      ppType = PP_OP;
      ppStart = i++;
      if (line[i] == '=') {
        ppSubType = OP_MOD_ASSIGN;
        ppEnd = ++i;
        return i;
      }
      ppSubType = OP_MOD;
      ppEnd = i;
      return i;
    case '/':
      ppType = PP_OP;
      ppStart = i++;
      if (line[i] == '=') {
        ppSubType = OP_DIV_ASSIGN;
        ppEnd = ++i;
        return i;
      }
      ppSubType = OP_DIV;
      ppEnd = i;
      return i;
    case '-':
      ppType = PP_OP;
      ppStart = i++;
      c = line[i];
      if (c == '>') {
        ppSubType = OP_FIELD;
        ppEnd = ++i;
        return i;
      } else if (c == '-') {
        ppSubType = OP_DEC;
        ppEnd = ++i;
        return i;
      } else if (c == '=') {
        ppSubType = OP_SUB_ASSIGN;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_SUB;
        ppEnd = i;
        return i;
      }
    case '+':
      ppType = PP_OP;
      ppStart = i++;
      c = line[i];
      if (c == '+') {
        ppSubType = OP_INC;
        ppEnd = ++i;
        return i;
      } else if (c == '=') {
        ppSubType = OP_ADD_ASSIGN;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_ADD;
        ppEnd = i;
        return i;
      }
    case '&':
      ppType = PP_OP;
      ppStart = i++;
      c = line[i];
      if (c == '&') {
        ppSubType = OP_CAND;
        ppEnd = ++i;
        return i;
      } else if (c == '=') {
        ppSubType = OP_AND_ASSIGN;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_AND;
        ppEnd = i;
        return i;
      }
    case '|':
      ppType = PP_OP;
      ppStart = i++;
      c = line[i];
      if (c == '|') {
        ppSubType = OP_COR;
        ppEnd = ++i;
        return i;
      } else if (c == '=') {
        ppSubType = OP_OR_ASSIGN;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_OR;
        ppEnd = i;
        return i;
      }
    case '<':
      ppType = PP_OP;
      ppStart = i++;
      c = line[i];
      if (c == '<') {
        i++;
        if (line[i] == '=') {
          ppSubType = OP_LS_ASSIGN;
          ppEnd = ++i;
          return i;
        } else {
          ppSubType = OP_LSHIFT;
          ppEnd = i;
          return i;
        }
      } else if (c == '=') {
        ppSubType = OP_LE;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_LT;
        ppEnd = i;
        return i;
      }
    case '>':
      ppType = PP_OP;
      ppStart = i++;
      c = line[i];
      if (c == '>') {
        i++;
        if (line[i] == '=') {
          ppSubType = OP_RS_ASSIGN;
          ppEnd = ++i;
          return i;
        } else {
          ppSubType = OP_RSHIFT;
          ppEnd = i;
          return i;
        }
      } else if (c == '=') {
        ppSubType = OP_GE;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_GT;
        ppEnd = i;
        return i;
      }
    case '#':
      ppType = PP_OP;
      ppStart = i++;
      if (line[i] == '#') {
        ppSubType = OP_DHATCH;
        ppEnd = ++i;
        return i;
      } else {
        ppSubType = OP_HATCH;
        ppEnd = i;
        return i;
      }
    default:
      ppType = PP_OTHER;
      ppSubType = c;
      ppStart = i++;
      ppEnd = i;
      return i;
    }
  }

  /**
   * Needs work.
   * @param start is the current scan position
   * @param end is one past the end scan position
   * @return the scan position after the token
   */
  private int extractChar(int start, int end)
  {
    int i = start;
    ppType = PP_NONE;
    while ((i < end) && isWhitespace(line[i])) i++;
    if (i >= end)
      return i;

    if (line[i] != '\'')
      return i;

    int s = 0;

    ppStart = i++;
    while ((i < end) && (line[i] != '\'')) {
      if (line[i] == '\\') {
        i++;
        switch (line[i]) {
        case 'a':  s = '\007'; i++; break;
        case 'b':  s = '\b';   i++; break;
        case 'e':  s = '\033'; i++; break;
        case 'f':  s = '\f';   i++; break;
        case 'n':  s = '\n';   i++; break;
        case 'r':  s = '\r';   i++; break;
        case 't':  s = '\t';   i++; break;
        case 'v':  s = '\013'; i++; break;
        case '\\': s = '\\';   i++; break;
        case '?':  s = '?';    i++; break;
        case '\'': s = '\'';   i++; break;
        case '"':  s = '"';    i++; break;
        case '0': case '1': case '2': case '3':
        case '4': case '5': case '6': case '7':
          while ((i < end) && (line[i] >= '0') && (line[i] <= '7'))
            s = (s << 3) + (line[i++] - '0');
          break;
        default: i++; break;
        }
      } else {
        s = line[i];
        i++;
      }
    }

    ppSubType = s;
    ppType    = PP_CHAR;
    ppEnd     = ++i;

    return i;
  }

  private int extractIdentifier(int start, int end)
  {
    int i = start;
    ppType = PP_NONE;
    while ((i < end) && isWhitespace(line[i]))
      i++;
    if (i >= end)
      return i;

    char c = line[i];
    switch (c) {
    case 'a': case 'b': case 'c': case 'd': case 'e':
    case 'f': case 'g': case 'h': case 'i': case 'j':
    case 'k': case 'l': case 'm': case 'n': case 'o':
    case 'p': case 'q': case 'r': case 's': case 't':
    case 'u': case 'v': case 'w': case 'x': case 'y':
    case 'z':
    case 'A': case 'B': case 'C': case 'D': case 'E':
    case 'F': case 'G': case 'H': case 'I': case 'J':
    case 'K': case 'L': case 'M': case 'N': case 'O':
    case 'P': case 'Q': case 'R': case 'S': case 'T':
    case 'U': case 'V': case 'W': case 'X': case 'Y':
    case 'Z':
    case '_': break;
    default:
      return i;
    }
    ppStart = i++;
    while (i < end) {
      c = line[i];
      switch (c) {
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
      case 'a': case 'b': case 'c': case 'd': case 'e':
      case 'f': case 'g': case 'h': case 'i': case 'j':
      case 'k': case 'l': case 'm': case 'n': case 'o':
      case 'p': case 'q': case 'r': case 's': case 't':
      case 'u': case 'v': case 'w': case 'x': case 'y':
      case 'z':
      case 'A': case 'B': case 'C': case 'D': case 'E':
      case 'F': case 'G': case 'H': case 'I': case 'J':
      case 'K': case 'L': case 'M': case 'N': case 'O':
      case 'P': case 'Q': case 'R': case 'S': case 'T':
      case 'U': case 'V': case 'W': case 'X': case 'Y':
      case 'Z':
      case '_': i++; continue;
      }
      break;
    }

    ppType = PP_IDENT;
    ppEnd = i;
    return i;
  }

  /**
   * @param start is the current scan position
   * @param end is one past the end scan position
   * @param delim is the string delimiter
   * @return the scan position after the token
   */
  private int extractString(int start, int end, char delim)
  {
    int i = start;
    ppType = PP_NONE;
    if (i >= end)
      return i;

    ppType = PP_STRING;
    ppStart = i;
    while (i < end) {
      char c = line[i];
      if (c == delim) {
        ppEnd = i;
        return i + 1;
      }
      if (c == '\\')
        i++;
      i++;
    }
    ppType = PP_NONE;
    return i;
  }

  /**
   * @param start is the current scan position
   * @param end is one past the end scan position
   * @return the scan position after the token
   */
  private int extractInteger(int start, int end)
  {
    int i = start;
    ppType = PP_NONE;
    while ((i < end) && isWhitespace(line[i]))
      i++;

    if (i >= end)
      return i;

    ppType = PP_NUM;
    ppStart = i;

    long s = 0;

    if ((line[i] == '0') && ((line[i + 1] == 'x') || (line[i + 1] == 'X'))) {
      i += 2;
      while (i < end) {
        char c = line[i];
        if (!Character.isDigit(c)) {
          c = Character.toLowerCase(c);

          if (!((c >= 'a') && (c <= 'f')))
            break;
          s = (s << 4) + (10 + (c - 'a'));
        } else
          s = (s << 4) + (c - '0');
        i++;
      }
    } else {
      while (i < end) {
        char c = line[i];
        if (!Character.isDigit(c))
          break;
        s = s * 10 + (c - '0');
        i++;
      }
    }

    while ((line[i] == 'L') || (line[i] == 'l') || (line[i] == 'U') || (line[i] == 'u'))
      i++;

    ppEnd = i;
    ppSubType = s;
    return i;
  }

  /**
   * Return true if the string matches the text at the specified position in <code>line</code>.
   */
  private boolean textMatches(String m, int position)
  {
    int l = m.length();
    for (int j = 0; j < l; j++)
      if (m.charAt(j) != line[position + j])
        return false;
    return true;
  }

  private int setText(int index, String text)
  {
    int l = text.length();
    for (int i = 0; i < l; i++)
      line[index++] = text.charAt(i);
    return index;
  }

  /**
   * Return true if <code>line</code> contains a pre-processor directive.
   * Perform the command if it is.
   */
  private boolean processDirective() throws java.io.IOException
  {
    int i = 0;
    while (isWhitespace(line[i]))
      i++;
    if (line[i] != '#')
      return false;
    i++;
    while (isWhitespace(line[i]))
      i++;
    int s = i++;

    char c = line[s];
    if (c == '\n')
      return true;

    try {
      if (c == 'i') {
        c = line[i++];
        if (c == 'f')
          return processIf(i++);
        if (c == 'n')
          return processInclude(i);
        return false;
      }

      if (c == 'e') {
        c = line[i++];
        if (c == 'l')
          return processElse(i);
        if (c == 'r')
          return processError(i);
        if (c == 'n')
          return processEndif(i);
        return false;
      }

      if (c == 'd')
        return processDefine(i);

      if (c == 'u')
        return processUndef(i);

      if (c == 'l')
        return processLine(i);

      if (c == 'p')
        return processPragma(i);

      if (c == 'w')
        return processWarning(i);
    } catch (Err ex) {
      genError(ex.errno, null, ex.position, true);
      resetLine();
    }

    return false;
  }

  private boolean processDefine(int i) throws Err, java.io.IOException
  {
    if (bypass)
      return false;

    if (!textMatches("efine", i))
      return false;

    i += 5;
    int st = i;
    if (!isWhitespace(line[i++]))
      return false;

    i = extractIdentifier(i, lineLength);
    if (ppType == PP_NONE)
      return false;

    int s = ppStart;
    int e = ppEnd;

    // Extract any macro parameters.

    AMacro  replaces = null;
    Macro   lm       = null;
    boolean parens   = false;
    if (line[i] == '(') {
      parens = true;
      i++;
      while (isWhitespace(line[i]))
        i++;
      if (line[i] != ')') {
        while (true) {
          while (isWhitespace(line[i]))
            i++;
          i = extractIdentifier(i, lineLength);
          if (ppType == PP_NONE)
            throw new Err(Msg.MSG_Invalid_macro_definition, i);
          int    len = ppEnd - ppStart;
          char[] mn  = new char[len];
          System.arraycopy(line, ppStart, mn, 0, len);
          AMacro m = new AMacro(mn);
          if (lm == null)
            replaces = m;
          else
            lm.next = m;

          lm = m;

          while (isWhitespace(line[i]))
            i++;
          if (line[i] == ',') {
            i++;
            continue;
          }
          if (line[i] == ')')
            break;
        }
      }
      i++;
    }

    // See if the macro is already defined and
    // find where to insert it if it has not been defined.

    int    len  = e - s;
    String text = new String(line, i, lineLength - i - 1).trim();
    int    h    = hash(line, s, len);
    Macro  m    = macros[h];
    Macro  last = null;
    while (m != null) {
      if (m.match(line, s, len)) {
        if (m.getType() != M_NORMAL)
          throw new Err(Msg.MSG_Pre_defined_macros_can_not_be_redefined, s);

        if (text.equals(((NMacro) m).getReplacementText()))
          return true;
        genError(Msg.MSG_Macro_redefined_s, new String(line, s, len), i, false);
      }
      last = m;
      m = m.next;
    }

    // Define the new macro.

    char[] name = new char[len];
    System.arraycopy(line, s, name, 0, len);

    NMacro nm = new NMacro(name, text, replaces, parens);
    if (macroText != null)
      macroText.add(nm.display());

    // Do not define macros that just replace themselves.
    // For example
    //   #define setjmp(env) setjmp(env)

    if (!nm.valid())
      return true;

    if (classTrace || CReader.classTrace)
      System.out.println("** cp " + nm);

    if (last == null)
      macros[h] = nm;
    else
      last.next = nm;

    return true;
  }

  private void anyJunk(int i) throws Err
  {
    while (isWhitespace(line[i])) i++;
    if (line[i] == '\n')
      return;

    for (int ii = 0; ii <= i; ii++) {
      System.out.print(" (");
      System.out.print(line[ii]);
      System.out.print(")");
      System.out.print(Integer.toHexString(line[ii]));
    }
    System.out.println("");

    throw new Err(Msg.MSG_Junk_after_directive, i);
  }

  private boolean isWhitespace(char c)
  {
    return (c == ' ') || (c == '\t');
  }

  private boolean processUndef(int i) throws Err
  {
    if (bypass)
      return false;

    if (!textMatches("ndef", i))
      return false;

    i += 4;

    if (!isWhitespace(line[i++]))
      return false;

    i = extractIdentifier(i, lineLength);
    if (ppType == PP_NONE)
      return false;

    anyJunk(i);

    int   l    = ppEnd - ppStart;
    int   h    = hash(line, ppStart, l);
    Macro m    = macros[h];
    Macro last = null;
    while (m != null) {
      if (m.match(line, ppStart, l)) {
        if (m.getType() != M_NORMAL)
          throw new Err(Msg.MSG_Pre_defined_macros_can_not_be_undefined, ppStart);
        if (last == null)
          macros[h] = m.next;
        else
          last.next = m.next;
        return true;
      }
      last = m;
      m = m.next;
    }

    return true;
  }

  private boolean processError(int i)
  {
    if (bypass)
      return false;

    if (!textMatches("ror", i))
      return false;

    i += 3;

    return false;
  }

  private boolean processWarning(int i)
  {
    if (bypass)
      return false;

    if (!textMatches("arning", i))
      return false;

    i += 6;

    return false;
  }

  private boolean processPragma(int i) throws Err, java.io.IOException
  {
    if (bypass)
      return false;

    if (!textMatches("ragma", i))
      return false;

    i += 5;
    while (isWhitespace(line[i]))
      i++;
    if ((line[i] == 'S') &&
        (line[i + 1] == 'T') &&
        (line[i + 2] == 'C') &&
        (line[i + 3] == ' '))
      return false;

    performMacroSubstitution(i, lineLength, null);

    return false;
  }

  private boolean processLine(int i) throws Err, java.io.IOException

  {
    if (bypass)
      return false;

    if (!textMatches("ine", i))
      return false;

    i += 3;

    if (!isWhitespace(line[i++]))
      return false;

    if (extractLineInfo(i))
      return true;

    performMacroSubstitution(i, lineLength, null);

    return extractLineInfo(i);
  }

  private boolean extractLineInfo(int i) throws Err
  {
    while (isWhitespace(line[i]))
      i++;
   
    if (!Character.isDigit(line[i]))
      return false;

    i = extractInteger(i, lineLength);

    int ln = (int) ppSubType;

    while (isWhitespace(line[i]))
      i++;

    if (line[i] == '\n')
      return true;

    if (line[i] != '"')
      return false;

    i = extractString(i + 1, lineLength, '"');
    if (ppType == PP_NONE)
      return false;

    anyJunk(i);

    String filename = new String(line, ppStart, ppEnd - ppStart);
    curReader.setFilename(filename);
    lineNumber = ln;

    return true;
  }

  private boolean processInclude(int i) throws IOException, Err
  {
    if (bypass)
      return false;

    if (!textMatches("clude", i))
      return false;

    i += 5;

    int includePtr = 0;
    if (allowGNUExtensions && textMatches("_next", i)) {
      i += 5;
      includePtr = includeIndex + 1;
    }

    includeIndex = 0;

    while (isWhitespace(line[i]))
      i++;

    boolean useUser = false;
    if (line[i] == '"') {
      i = extractString(i + 1, lineLength, '"');
      useUser = true;
    } else if (line[i] == '<') {
      i = extractString(i + 1, lineLength, '>');
      useUser = false;
    } else
      return false;

    if (ppType == PP_NONE)
      return false;

    anyJunk(i);
    while (isWhitespace(line[i]))
      i++;

    if (line[i] != '\n') {
      genError(Msg.MSG_Junk_after_directive, null, i, true);
      resetLine();
      return false;
    }

    String pathname = new String(line, ppStart, ppEnd - ppStart);
    File   file     = new File(pathname);
    String fn       = file.getName();
    if (fn.equals("stdarg.h") || fn.equals("va_list.h")) {
      lineLength = setText(0, "#include <stdarg.h>\n");
      line[lineLength] = 0;
      return false;
    }

    // Try the current directory.

    String cur = curReader.getFilename();
    int    si  = cur.lastIndexOf(File.separatorChar);
    if (si < 0)
      cur = ".";
    else
      cur = cur.substring(0, si);

    if (useUser) {
      if ((includeIndex >= includePtr) && findIncludeFile(pathname, cur))
          return true;
      includeIndex++;
      if (findIncludeFile(pathname, userDirs, includePtr))
          return true;
      if (findIncludeFile(pathname, systemDirs, includePtr))
          return true;
    } else {
      if (findIncludeFile(pathname, systemDirs, includePtr))
          return true;
      if ((includeIndex >= includePtr) && findIncludeFile(pathname, cur))
          return true;
      includeIndex++;
      if (findIncludeFile(pathname, userDirs, includePtr))
          return true;
    }

    genError(Msg.MSG_Include_file_s_not_found, pathname, i, false);

    return true;
  }

  private boolean findIncludeFile(String filename, String directory)
  {
    File   c    = new File(directory, filename);
    String path = c.getAbsolutePath();

    if (path.equals(curReader.getFilename()))
      return false; // Same file.

    try {
      CReader xcurReader = newFile(path);
      if (xcurReader != null) {
        curReader = xcurReader;
        return true;
      }
    } catch (java.lang.Throwable ex) {
    }

    return false;
  }

  private boolean findIncludeFile(String filename, Vector<String> dirs, int includePtr)
  {
    if (dirs == null)
      return false;

    int l = dirs.size();
    for (int j = 0; j < l; j++) {
      String dir  = dirs.elementAt(j);
      if ((includeIndex >= includePtr) && findIncludeFile(filename, dir))
        return true;
      includeIndex++;
    }
    return false;
  }

  private boolean processEndif(int i) throws Err
  {
    if (!textMatches("dif", i))
      return false;

    i += 3;

    anyJunk(i);

    boolean obp = bypass;
    ifState = ifStackPop();
    bypass = (ifState != IF_YES);

    return true;
  }

  private boolean processElse(int i) throws Err, java.io.IOException
  {
    if (textMatches("se", i)) {
      i += 2;

      anyJunk(i);

      int os = ifStackPeek();
      ifState = nextIfState(os != IF_YES, ifState);
      bypass = (ifState != IF_YES);

      return true;
    }

    if (!textMatches("if", i))
      return false;

    i += 2;

    if (!isWhitespace(line[i++]))
      return false;

    i = computeConstantExpression(i, lineLength);
    if (ppType == PP_NONE)
      return false;

    int os = ifStackPeek();
    ifState = nextIfState((ppSubType == 0) || (os != IF_YES), ifState);
    bypass = (ifState != IF_YES);

    return true;
  }

  private boolean processIf(int i) throws Err, java.io.IOException
  {
    char c = line[i++];

    if (isWhitespace(c) || (c == '(')) {
      i = computeConstantExpression((c == '(') ? i - 1 : i, lineLength);
      if (ppType == PP_NONE)
        return false;

      ifStackPush(ifState);
      ifState = nextIfState((ppSubType == 0) || (ifState != IF_YES), IF_NO);
      bypass = (ifState != IF_YES);
      return true;
    }

    boolean neg = false;
    if (c == 'n') {
      neg = true;
      i++;
    }

    if (!textMatches("def", i - 1))
      return false;
 
    i += 3 - 1;

    if (!isWhitespace(line[i++]))
      return false;

    i = extractIdentifier(i, lineLength);
    if (ppType == PP_NONE)
      return false;

    anyJunk(i);

    Macro macro = null;
    int   len   = ppEnd - ppStart;
    int   hash  = hash(line, ppStart, len);
    Macro m     = macros[hash];
    while (m != null) {
      if (m.match(line, ppStart, len)) {
        macro = m;
        break;
      }
      m = m.next;
    }

    ifStackPush(ifState);
    ifState = nextIfState(((macro == null) ^ neg) || (ifState != IF_YES), IF_NO);
    bypass = (ifState != IF_YES);

    return true;
  }

  /**
   * Read a single character. Line terminators are compressed into single newline ('\n') characters.
   * @return the character read, or -1 if the end of the stream has been reached
   */
  public int read() throws IOException
  {
    if ((linePosition >= lineLength) && readLineInt())
      return -1;
    char c = line[linePosition++];
    return c;
  }

  /**
   * Specify the current function being processed.
   */
  public void setCurrentFtn(String currentFtn)
  {
    this.currentFtn = currentFtn;
  }

  /**
   * Skip characters.
   * @param n - the number of characters to skip
   * @return the number of characters actually skipped
   */
  public long skip(long n) throws IOException
  {
    long avail = lineLength - linePosition;
    long skipped = 0;
    while (avail < n) {
      skipped += avail;
      n -= avail;
      if (readLineInt())
        return skipped;
      avail = lineLength - linePosition;
    }
    if (avail > n)
      avail = n;
    linePosition += (int) avail;
    return skipped + avail;
  }

  /**
   * Mark the present position in the stream. Subsequent calls to reset() will attempt 
   * to reposition the stream to this point, and will also reset the line number appropriately.
   */
  public void mark(int readAheadLimit) throws IOException
  {
    curReader.mark(readAheadLimit);
  }

  /**
   * Reset the stream to the most recent mark.
   */
  public void reset() throws IOException
  {
    curReader.reset();
    linePosition = 0;
    lineLength   = 0;
  }
}
