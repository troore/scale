package scale.test;

import java.io.*;
import java.util.Enumeration;
import java.lang.reflect.*;

import scale.annot.*;
import scale.common.*;
import scale.clef.decl.Declaration;
import scale.callGraph.*;

/**
 * This class reads Annotations from a file.
 * <p>
 * $Id: AnnotationFile.java,v 1.18 2007-10-04 19:58:39 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Each annotation in the file has the following form:
 * <pre>
 *   annotation-class declaration-class declaration-name parameter parameter ... ;
 * </pre>
 * Annotations may span multiple lines and more than one annotation
 * can be on a single line.
 * <p>
 * The example annotation:
 * <pre>
 *   scale.clef.PureFunctionAnnotation scale.clef.decl.RoutineDecl my_sqrt PURE ;
 * </pre>
 * applies the PureFunctionAnnotation to the my_sqrt routine
 * declaration with parameter "PURE" as with
 * <pre>
 *   scale.clef.PureFunctionAnnotation.create(my_sqrt_decl, creator, support, "PURE");
 * </pre>
 */

public class AnnotationFile
{
  /**
   * Keep the characters in the current line in the Stream.
   */
  private static class MyInputStreamReader extends java.io.InputStreamReader
  {
    private char[]  line = new char[10];
    private int     in   = 0;
    private int     out  = 0;
    private boolean eof  = false;

    public MyInputStreamReader(InputStream cin)
    {
      super(cin);
    }

    private void expand()
    {
      char[] newline = new char[line.length * 2];
      for (int j = 0; j < in; j++)
        newline[j] = line[j];
      line = newline;
    }

    private void fill() throws IOException
    {
      if (eof)
        return;

      while (true) {
        if (in >= line.length)
          expand();
        int c = super.read();
        if (c < 0) {
          c = '\n';
          eof = true;
        }
        line[in] = (char) c;
        in++;
        if (c == '\n')
          return;
      }
    }

    public int read() throws IOException
    {
      if (eof)
        return -1;

      if (out >= in) {
        in = 0;
        out = 0;
        fill();
      }
      return line[out++];
    }

    public String getLine()
    {
      return new String(line, 0, in);
    }

    public int position()
    {
      return out - 1;
    }

    public void nextLine()
    {
      in = 0;
    }

    public void consume(char delimiter) throws IOException
    {
      while (!eof) {
        int c = read();
        if (c == delimiter)
          return;
      }
    }
  }

  /**
   * The creator of the annotations is "user".
   */
  protected Creator creator = new CreatorSource("user");
  /**
   * User belief is true.
   */
  protected Support support = Support.create(true, Support.Belief.True);

  /**
   * Warn about errors only.
   */
  public static final int NORMAL = 0;
  /**
   * Announce added annotations.
   */
  public static final int FOUND = 1;
  /**
   * Announce un-added annotations.
   */
  public static final int NOTFOUND = 2;
  /**
   * Do not announce errors.
   */
  public static final int NOERRORS = 4;

  private int warningLevel = 0;

  public AnnotationFile(int warningLevel)
  {
    this.warningLevel = warningLevel;
  }

  /**
   * Print an error message.
   * @param aFile is the file containing the error
   * @param msg is the error message
   * @param line is the line in error
   * @param lineno is the line number of the file line in error
   */
  private void printInvalid(String aFile,
                            String msg,
                            String line,
                            int    lineno,
                            int    position)
  {
    if ((warningLevel & NOERRORS) != 0)
      return;

    System.out.print(aFile);
    System.out.print(":");
    System.out.print(lineno);
    System.out.print(": ");
    System.out.println(msg);
    System.out.print(line);
    for (int i = 0; i < position; i++)
      System.out.print(" ");
    System.out.println("^");
  }

  /**
   * Apply the annotations in a file to the declarations in the Suite.
   * @param suite is queried for the declarations
   * @param aFile is the pathname of the annotation file
   */
  protected void processAnnotationFile(Suite suite, String aFile)
  {
    FileInputStream fos = null;
    try {
      fos  = new FileInputStream(aFile);

      Vector<Object>      line = new Vector<Object>(5);
      MyInputStreamReader cin  = new MyInputStreamReader(fos);
      StreamTokenizer     izer = new StreamTokenizer(cin);

      izer.resetSyntax();
      izer.whitespaceChars(0, 32);
      izer.whitespaceChars(',', ',');
      izer.wordChars('a', 'z');
      izer.wordChars('A', 'Z');
      izer.wordChars('0', '9');
      izer.wordChars('.', '.');
      izer.wordChars('/', '/');
      izer.wordChars('_', '_');
      izer.quoteChar('\'');
      izer.quoteChar('"');
      izer.parseNumbers();
      izer.eolIsSignificant(true);
      izer.slashSlashComments(true);
      izer.slashStarComments(true);

      while (izer.ttype != StreamTokenizer.TT_EOF) {
        int t = izer.nextToken();
        if (t == StreamTokenizer.TT_EOF)
          break;
        else if (t == StreamTokenizer.TT_NUMBER) {
          long value = (long) izer.nval;
          if (izer.nval != (double) value) {
            printInvalid(aFile, "not an integer",
                         cin.getLine(),
                         izer.lineno(),
                         cin.position());
            cin.consume(';');
            line.removeAllElements();
          } else
            line.addElement(new Double(izer.nval));
        } else if (t == StreamTokenizer.TT_EOL) {
          continue;
        } else if ((t == StreamTokenizer.TT_WORD) ||
                   (t == '"') || (t == '\'')) {
          line.addElement(izer.sval);
        } else if (t >= 0) {
          try {
            if (t == ';') {
              processLine(suite, line, cin.getLine());
              line.removeAllElements();
            } else if (t == '[') {
              Vector<Object> v = extractVector(izer);
              line.addElement(v);
            }
          } catch(scale.common.InvalidException in) {
            printInvalid(aFile,
                         in.getMessage(),
                         cin.getLine(),
                         izer.lineno(),
                         cin.position());
            line.removeAllElements();
          }
        }
      }

      if (line.size() > 0) {
        printInvalid(aFile,
                     "Missing semicolon",
                     cin.getLine(),
                     izer.lineno(),
                     cin.position());
      }
    } catch(java.lang.Exception ex) {
      ex.printStackTrace();
    } catch(java.lang.Error er) {
      er.printStackTrace();
    }
    if (fos != null)
      try {
        fos.close();
      } catch(java.lang.Exception ex) {
        ex.printStackTrace();
      }
  }

  /**
   * Create a vector from the following tokens until a ')' is reached.
   * @param izer is the StreamTokenizer
   */
  private Vector<Object> extractVector(StreamTokenizer izer)
    throws scale.common.InvalidException, java.io.IOException
  {
    Vector<Object> v = new Vector<Object>(2);

    while (izer.ttype != StreamTokenizer.TT_EOF) {
      int t = izer.nextToken();

      if (t == StreamTokenizer.TT_EOF)
        break;
      else if (t == StreamTokenizer.TT_NUMBER)
        v.addElement(new Double(izer.nval));
      else if (t >= 0) {
        if (t == ';')
          break;
        else if (t == '[') {
          Vector<Object> vv = extractVector(izer);
          v.addElement(vv);
        } else if (t == ']') {
          return v;
        }
      } else if (t == StreamTokenizer.TT_WORD) {
        v.addElement(izer.sval);
      }
    }
    throw new scale.common.InvalidException("invalid vector termination");
  }

  /**
   * Return a string representation of the create invocation.
   * @param line is the vector of tokens.
   * @param msg is a string to be appended to the string representation
   */
  private String stringize(Vector<Object> line, String msg)
  {
    StringBuffer buf = new StringBuffer("method ");

    buf.append((String) line.elementAt(0));
    buf.append(".create(");
    buf.append((String) line.elementAt(1));
    buf.append(".");
    buf.append((String) line.elementAt(2));
    buf.append(",creator,support");

    int n = line.size();
    for (int i = 3; i < n; i++) {
      Object x = line.elementAt(i);
      buf.append(",");
      if (x instanceof String)
        buf.append("\"");
      buf.append(x.toString());
      if (x instanceof String)
        buf.append("\"");
    }
    buf.append(") ");
    if (msg != null)
      buf.append(msg);
    return buf.toString();
  }

  /**
   * Apply an annotation to the declaration in the Suite.
   * @param suite is queried for the declarations
   * @param line is a vector of tokens specifying the annotation
   * @param ann is the text of the annotation
   */
  private void processLine(Suite          suite,
                           Vector<Object> line,
                           String         ann) throws scale.common.InvalidException
  {
    int numArgs = line.size() - 3; /* Number of arguments provided for the annotation. */
    if (numArgs < 0)
      throw new scale.common.InvalidException("annotation, declaration, instance names missing");

    String   annoName = (String) line.elementAt(0); /* Name of the annotation class. */
    String   declName = (String) line.elementAt(1); /* Name of the declaration class. */
    String   instName = (String) line.elementAt(2); /* Name of the declaration instance. */
    Class    aclass   = null;                       /* The class of the annotation. */
    Class    dclass   = null;                       /* The class of the declaration. */
    Object[] args     = new Object[numArgs + 3];

    try {
      aclass = Class.forName(annoName);
    } catch(java.lang.Exception ex) {
      throw new scale.common.InvalidException(annoName + " annotation class not found");
    }
    try {
      dclass = Class.forName(declName);
    } catch(java.lang.Exception ex) {
      throw new scale.common.InvalidException(declName + " declaration class not found");
    }

    // Set up the arguments to the invoke method.

    args[1] = creator;
    args[2] = support;
    for (int i = 0; i < numArgs; i++)
      args[i + 3] = line.elementAt(i + 3);

    // Find the create method with the correct number of parameters.

    Method[] methods = aclass.getMethods();
    Method   method  = null;

    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      if (m.getName().equals("create")) {
        Class[] classes = m.getParameterTypes();
        if (classes.length == (numArgs + 3)) {
          int j = 0;
          for (j = 3; j < classes.length; j++) {
            Class  c   = classes[j];
            Object arg = args[j];
            if (c.isPrimitive()) {
              if (!(arg instanceof Double))
                break;
              long   value = ((Double) arg).longValue();
              Double d     = new Double((double) value);
              if (! d.equals(arg))
                break;
              if ((c == Integer.TYPE) &&
                  (value >= Integer.MIN_VALUE) &&
                  (value <= Integer.MAX_VALUE)) {
                arg = new Integer((int) value);
              } else if ((c == Long.TYPE) &&
                         (value >= Long.MIN_VALUE) &&
                         (value <= Long.MAX_VALUE)) {
              } else if ((c == Byte.TYPE) &&
                         (value >= Byte.MIN_VALUE) &&
                         (value <= Byte.MAX_VALUE)) {
                arg = new Byte((byte) value);
              } else if ((c == Short.TYPE) && 
                         (value >= Short.MIN_VALUE) &&
                         (value <= Short.MAX_VALUE)) {
                arg = new Short((short) value);
              } else if ((c == Character.TYPE) &&
                         (value >= Character.MIN_VALUE) &&
                         (value <= Character.MAX_VALUE)) {
                arg = new Character((char) value);
              } else
                break;
            } else if (c.isInstance(arg)) {
            } else
              break;
          }
          if (j >= classes.length) {
            method = m;
            break;
          }
        }
      }
    }

    if (method == null)
      throw new scale.common.InvalidException(stringize(line, "not found"));

    // Fix up primitive arguments to be the correct wrapper class.

    Class[] classes = method.getParameterTypes();
    for (int j = 3; j < classes.length; j++) {
      Class  c   = classes[j];
      Object arg = args[j];
      if (c.isPrimitive()) {
        long value = ((Double) arg).longValue();
        if (c == Integer.TYPE) {
          args[j] = new Integer((int) value);
        } else if (c == Long.TYPE) {
        } else if (c == Byte.TYPE) {
          args[j] = new Byte((byte) value);
        } else if (c == Short.TYPE) {
          args[j] = new Short((short) value);
        } else if (c == Character.TYPE) {
          args[j] = new Character((char) value);
        }
      }
    }

    // Invoke the create method on each declaration with the correct
    // class and name.

    boolean                  flag = true;
    Enumeration<Declaration> et   = suite.otherDecls(instName);
    while (et.hasMoreElements()) {
      Declaration d = et.nextElement();
      if (dclass.isInstance(d)) {
        args[0] = d;
        flag = false;

        try {
          method.invoke(null, args);
        } catch(java.lang.reflect.InvocationTargetException iex) {
          Throwable ex = iex.getTargetException();
          throw new scale.common.InvalidException(stringize(line, ex.getMessage()));
        } catch(java.lang.IllegalAccessException aex) {
          throw new scale.common.InvalidException(stringize(line, "inaccessible"));
        } catch(java.lang.IllegalArgumentException aex) {
          throw new scale.common.InvalidException(stringize(line, "argument type mismatch"));
        }
      }
    }

    if (flag) {
      if ((warningLevel & NOTFOUND) != 0)
        throw new scale.common.InvalidException("instance " +
                                                declName +
                                                "." +
                                                instName +
                                                " not found");
    } else if ((warningLevel & FOUND) != 0) {
      if ((warningLevel & FOUND) != 0)
        System.out.println("Added " + ann + " to " + declName + "." + instName);
    }
  }
}
