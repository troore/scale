package scale.frontend;

import java.io.*;

import scale.common.*;
import scale.callGraph.*;

/** 
 * This is the base class for all source code parsers.
 * <p>
 * $Id: Parser.java,v 1.9 2007-09-20 18:49:42 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public abstract class Parser
{
  /**
   * A mapping from file extension to parser.
   */
  private static final String[] parsers = {
    "c",   "c.C99",
    "f",   "fortran.F95",
    "f90", "fortran.F95",
    "for", "fortran.F95",
    "FOR", "fortran.F95",
    "f77", "fortran.F95",
  };

  protected scale.test.Scale top;
  protected String           extension;

  public Parser(scale.test.Scale top, String extension)
  {
    this.top       = top;
    this.extension = extension;
  }

  /**
   * Parse the specified file.  If <code>macroText</code> is not
   * <code>null</code>, the set of defined macros is added to it.
   * @param name the name of the Clef AST (i.e., the file name)
   * @param suite is the collection of call graphs
   * @param macroText is <code>null</code> or is used to collect macro
   * definitions as text strings
   * @return new CallGraph
   */
  public abstract CallGraph parse(String name, Suite suite, Vector<String> macroText);

  /**
   * Return the parser to use for the specified file.  The file
   * extension is used to look up the appropriate parser.
   */
  public static Parser getParser(String filename, scale.test.Scale top)
  {
    int di = filename.lastIndexOf('.');
    if ((di < 0) || (di >= filename.length() - 1))
      return null;

    String extension = filename.substring(di + 1);
    for (int i = 0; i < parsers.length; i += 2) {
      if (parsers[i].equals(extension)) {
        String parserName = "scale.frontend." + parsers[i + 1];
        try {
          Class[]       argNamesm = new Class[2];
          Object[]      argsm     = new Object[2];
          @SuppressWarnings("unchecked")
          Class<Parser> opClass   = (Class<Parser>) Class.forName(parserName);

          argNamesm[0] = top.getClass();
          argsm[0]     = top;

          argNamesm[1] = extension.getClass();
          argsm[1]     = extension;

          java.lang.reflect.Constructor cnstr = opClass.getDeclaredConstructor(argNamesm);
          Parser parser = (Parser) cnstr.newInstance(argsm);
          return parser;
        } catch (java.lang.Exception ex) {
          return null;
        }
      }
    }

    return null;
  }

  /**
   * Return the reader to use for the specified file.  The file
   * extension is used to look up the appropriate reader.
   */
  public static void runPreprocessor(PrintStream      out,
                                     String           filename,
                                     scale.test.Scale top) throws java.io.IOException
  {
    int di = filename.lastIndexOf('.');
    if ((di < 0) || (di >= filename.length() - 1)) {
      Msg.reportError(Msg.MSG_Unknown_file_extension_s, null, 0, 0, filename);
      return;
    }

    String extension = filename.substring(di + 1);
    for (int i = 0; i < parsers.length; i += 2) {
      if (parsers[i].equals(extension)) {
        String parserName = "scale.frontend." + parsers[i + 1];
        try {
          Class[]  argNamesm = new Class[3];
          Object[] argsm     = new Object[3];
          @SuppressWarnings("unchecked")
          Class<Parser> opClass = (Class<Parser>) Class.forName(parserName);

          argNamesm[0] = out.getClass();
          argsm[0]     = out;

          argNamesm[1] = filename.getClass();
          argsm[1]     = filename;

          argNamesm[2] = top.getClass();
          argsm[2]     = top;

          java.lang.reflect.Method method = opClass.getDeclaredMethod("runPreprocessor", argNamesm);
          method.invoke(null, argsm);
          return;
        } catch (java.lang.Exception ex) {
          assert Debug.printStackTrace(ex);
          break;
        }
      }
    }

    if (top.cpDm.specified())
      return; // No macro definitions to report.

    // Use the default preprocessor which just reads and prints each
    // line.

    BufferedReader reader = new BufferedReader(new FileReader(filename));
    while (true) {
      String line = reader.readLine();
      if (line == null)
        break;

      out.println(line);
    }

    reader.close();
  }

  /**
   * Return the correct source langauge instance for this parser.
   */
  public abstract SourceLanguage getSourceLanguage();
}
