package scale.frontend.c;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import scale.common.*;
import scale.callGraph.*;
import scale.frontend.*;

/** 
 * This is the parser for the C99 version of C.
 * <p>
 * $Id: C99.java,v 1.24 2007-09-20 18:49:43 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class C99 extends Parser
{
  /**
   * GCC mode.
   */
  public static final int GCC   = 2;
  /**
   * C99 mode.
   */
  public static final int C99   = 3;
  /**
   * Strict ANSI C mode.
   */
  public static final int ANSIC = 4;
  /**
   * Generate line numbers.
   */
  public static final int LINENUMS = 8;

  /**
   * Default location the preprocessor will search for system include
   * files.  You can override this with "-I-" on the command line.
   */
  private final static String[] hfiles = {"/usr/include"};
  private final static String   hostOS   = System.getProperty("os.name");
  private final static String[] scaleMacros = {
    "__LANGUAGE_C__=1",
    "__scale__=1",
    };
  private final static String[] gnuMacros = {
    "__CHAR_BIT__=8",
    "CHAR_BIT=8",
    "__DBL_DENORM_MIN__=4.9406564584124654e-324",
    "__DBL_DIG__=15",
    "__DBL_EPSILON__=2.2204460492503131e-16",
    "__DBL_HAS_INFINITY__=1",
    "__DBL_HAS_QUIET_NAN__=1",
    "__DBL_MANT_DIG__=53",
    "__DBL_MAX_10_EXP__=308",
    "__DBL_MAX__=1.7976931348623157e+308",
    "__DBL_MAX_EXP__=1024",
    "__DBL_MIN_10_EXP__=(-307)",
    "__DBL_MIN__=2.2250738585072014e-308",
    "__DBL_MIN_EXP__=(-1021)",
    "__DECIMAL_DIG__=21",
    "__ELF__=1",
    "__FINITE_MATH_ONLY__=0",
    "__FLT_DENORM_MIN__=1.40129846e-45F",
    "__FLT_DIG__=6",
    "__FLT_EPSILON__=1.19209290e-7F",
    "__FLT_EVAL_METHOD__=2",
    "__FLT_HAS_INFINITY__=1",
    "__FLT_HAS_QUIET_NAN__=1",
    "__FLT_MANT_DIG__=24",
    "__FLT_MAX_10_EXP__=38",
    "__FLT_MAX__=3.40282347e+38F",
    "__FLT_MAX_EXP__=128",
    "__FLT_MIN_10_EXP__=(-37)",
    "__FLT_MIN__=1.17549435e-38F",
    "__FLT_MIN_EXP__=(-125)",
    "__FLT_RADIX__=2",
    "__GNUC__=4",
    "__GNUC_MINOR__=1",
    "__GXX_ABI_VERSION=1002",
    "__INT_MAX__=2147483647",
    "INT_MAX=2147483647",
    "__INTMAX_MAX__=9223372036854775807LL",
    "__INTMAX_TYPE__=long long int",
    "INT_MIN=(-INT_MAX - 1)",
    "__LDBL_DENORM_MIN__=3.64519953188247460253e-4951L",
    "__LDBL_DIG__=18",
    "__LDBL_EPSILON__=1.08420217248550443401e-19L",
    "__LDBL_HAS_INFINITY__=1",
    "__LDBL_HAS_QUIET_NAN__=1",
    "__LDBL_MANT_DIG__=64",
    "__LDBL_MAX_10_EXP__=4932",
    "__LDBL_MAX__=1.18973149535723176502e+4932L",
    "__LDBL_MAX_EXP__=16384",
    "__LDBL_MIN_10_EXP__=(-4931)",
    "__LDBL_MIN__=3.36210314311209350626e-4932L",
    "__LDBL_MIN_EXP__=(-16381)",
    "LLONG_MAX=9223372036854775807LL",
    "LLONG_MIN=(-LLONG_MAX - 1LL)",
    "__LONG_LONG_MAX__=9223372036854775807LL",
    "__LONG_MAX__=2147483647L",
    "__NO_INLINE__=1",
    "__PRETTY_FUNCTION__=__FUNCTION__",
    "__PTRDIFF_TYPE__=ptrdiff_t",
    "__REGISTER_PREFIX__",
    "__SCHAR_MAX__=127",
    "SCHAR_MAX=127",
    "SCHAR_MIN=(-128)",
    "__SHRT_MAX__=32767",
    "SHRT_MAX=32767",
    "SHRT_MIN=(-32768)",
    "__SIZE_TYPE__=size_t",
    "UCHAR_MAX=255",
    "UINT_MAX=4294967295U",
    "UINT_MAX=4294967295U",
    "__UINTMAX_TYPE__=unsigned long long int",
    "ULLONG_MAX=18446744073709551615ULL",
    "__USER_LABEL_PREFIX__",
    "USHRT_MAX=65535",
    "__WCHAR_MAX__=2147483647",
    "__WCHAR_TYPE__=wchar_t",
    "__WINT_TYPE__=unsigned int",
  };

  private static final String[] linuxMacros = {
    "__linux=1",
    "__linux__=1",
    "linux=1",
    "__unix=1",
    "__unix__=1",
    "unix=1",
    "__gnu_linux__=1",
  };

  private Vector<String> userMacros;
  private Vector<String> userDirs;
  private Vector<String> systemDirs;
  private boolean ansic;
  private boolean gcc;
  private boolean c99;
  private boolean kr;
  private boolean linux;

  /**
   * @param top is the top level of the compiler
   * @param extension is the file extension of the file to be parsed
   */
  public C99(scale.test.Scale top, String extension)
  {
    super(top, extension);

    gcc   = top.cpGcc.specified();
    c99   = top.cpC99.specified();
    kr    = top.cpCkr.specified();
    ansic = top.cpAnsi.specified();
    linux = "linux".equals(hostOS.toLowerCase()) && !top.isCrossCompile();

    if (linux)
      gcc = true;

    userMacros = top.cpD.getStringValues();
    if (userMacros == null)
      userMacros = new Vector<String>(2);

    addMacros(userMacros, gcc, c99, kr, ansic, linux);

    userDirs   = top.cpIncl.getStringValues();
    systemDirs = top.cpIncls.getStringValues();

    if ((systemDirs == null) && !top.isCrossCompile()) {
      systemDirs = new Vector<String>();
      for (int j = 0; j < hfiles.length; j++) {
        systemDirs.addElement(hfiles[j]);
      }
      if (linux)
        systemDirs.addElement("/usr/include/linux");
    }
  }

  /**
   * Parse the specified C file.  If <code>macroText</code> is not
   * <code>null</code>, the set of defined macros is added to it.
   * @param name the name of the Clef AST (i.e., the file name)
   * @param suite is the collection of call graphs
   * @param macroText is <code>null</code> or is used to collect macro
   * definitions as text strings
   * @return new CallGraph
   */
  public CallGraph parse(String name, Suite suite, Vector<String> macroText)
  {
    try {
      Vector<String> v      = top.cpDm.specified() ? macroText : null;
      CPreprocessor  reader = new CPreprocessor(name,
                                                userMacros,
                                                userDirs,
                                                systemDirs,
                                                v);
      C99Lexer      lexer  = new C99Lexer(reader);
      C99Parser     parser = new C99Parser(lexer);

      lexer.setReader(reader);
      lexer.setParser(parser);

      parser.allowGNUExtensions = gcc;
      reader.allowGNUExtensions = gcc;
      parser.allowC99Extensions = c99;
      parser.strictANSIC        = ansic;
      reader.strictANSIC        = ansic;

      CallGraph cg = new CallGraph(name, suite, new SourceC());
      lexer.setCallGraph(cg);

      cg = parser.parse(cg, reader, macroText);
      reader.close();
      System.gc(); // To actually close files!
      return cg;

    } catch(java.io.FileNotFoundException e) {
      Msg.reportError(Msg.MSG_Source_file_not_found_s, null, 0, 0, name);
      return null;
    } catch(java.lang.Exception e) {
      String msg = e.getMessage();
      if (true || (msg == null) || (msg.length() < 1))
        e.printStackTrace();
      else if (!(e instanceof antlr.RecognitionException))
        Msg.reportError(Msg.MSG_s, name, 0, 0, msg);
      return null;
    } catch(java.lang.Error e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Run just the C preprocessor.  Each line of the specified file is
   * read, processed, and printed on the specified print stream.
   */
  public static void runPreprocessor(PrintStream      out,
                                     String           filename,
                                     scale.test.Scale top) throws java.io.IOException
  {
    boolean gcc   = top.cpGcc.specified();
    boolean c99   = top.cpC99.specified();
    boolean kr    = top.cpCkr.specified();
    boolean ansic = top.cpAnsi.specified();
    boolean linux = "linux".equals(hostOS.toLowerCase()) && !top.isCrossCompile();

    if (linux)
      gcc = true;

    Vector<String> userMacros = top.cpD.getStringValues();
    if (userMacros == null)
      userMacros = new Vector<String>(2);

    addMacros(userMacros, gcc, c99, kr, ansic, linux);

    Vector<String> userDirs   = top.cpIncl.getStringValues();
    Vector<String> systemDirs = top.cpIncls.getStringValues();

    if ((systemDirs == null) && !top.isCrossCompile()) {
      systemDirs = new Vector<String>();
      for (int j = 0; j < hfiles.length; j++) {
        systemDirs.addElement(hfiles[j]);
      }
      if (linux)
        systemDirs.addElement("/usr/include/linux");
    }

    boolean        dm  = top.cpDm.specified();
    Vector<String> x   = dm ? new Vector<String>(10) : null;
    CPreprocessor  cpp = new CPreprocessor(filename,
                                           userMacros,
                                           userDirs,
                                           systemDirs,
                                           x);

    cpp.allowGNUExtensions = gcc;
    cpp.strictANSIC        = ansic;

    while (true) {
      String line = cpp.readLine();
      if (line == null)
        break;

      if (!dm)
        out.println(line);
    }

    cpp.close();
    if (dm) {
      int lw = x.size();
      for (int j = 0; j < lw; j++)
        out.println(x.get(j));
    }
  }

  /**
   * Return the correct source langauge instance for this parser.
   */
  public SourceLanguage getSourceLanguage()
  {
    return new SourceC();
  }

  private static void addMacros(Vector<String>  userMacros,
                                boolean gcc,
                                boolean c99,
                                boolean kr,
                                boolean ansic,
                                boolean linux)
  {
    addMacros(userMacros, scaleMacros);

    if (ansic)
      userMacros.addElement("__STRICT_ANSI__=1");

    if (linux)
      addMacros(userMacros, linuxMacros);

    if (gcc) {
      addMacros(userMacros, gnuMacros);
      boolean big = (Machine.currentMachine.getSignedLongType().bitSize() == 64);
      userMacros.addElement(big ? "LONG_MAX=9223372036854775807L" : "LONG_MAX=2147483647L");
    }

    if (!kr)
      userMacros.addElement("__STDC__=1");

    Machine.currentMachine.addCPPFlags(userMacros);
  }

  private static void addMacros(Vector<String> set, String[] macros)
  {
      for (int i = 0; i < macros.length; i++)
        set.addElement(macros[i]);
  }
}
