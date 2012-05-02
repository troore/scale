package scale.test;

import java.io.*;
import scale.common.*;

/**
 * This class computes LOC metrics on .java, .h, and .c files.
 * <p>
 * $Id: LOC.java,v 1.18 2007-10-04 19:54:58 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class LOC
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
        if (c == '\n') {
          return;
        }
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

    public void skip() throws IOException
    {
      if (eof)
        return;

      if (out >= in) {
        in = 0;
        out = 0;
        fill();
      }
      out++;
      return;
    }

    public void skipline() throws IOException
    {
      if (eof)
        return;

      out = in - 1;
      return;
    }

    public int peek() throws IOException
    {
      if (eof)
        return -1;

      if (out >= in) {
        return -1;
      }
      return line[out];
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

  protected CmdParam   files  = new CmdParam("files", false, CmdParam.LIST,   null,
    "LOC the source file(s) specified.  They can be a .c, .java, or .h files.");
  protected CmdParam   who    = new CmdParam("w",     true,  CmdParam.STRING, "",
    "who");
  protected CmdParam   ext    = new CmdParam("m",     true,  CmdParam.STRING, "java",
    "for files with this extension");
  protected CmdParam   lline  = new CmdParam("l",     true,  CmdParam.SWITCH, null,
    "longest line");
  protected CmdParam[] params = {who, ext, lline};

  private static LOC me;

  private int totalLines   = 0; // Total number of source lines
  private int totalBlank   = 0; // Total number of empty source lines
  private int totalComment = 0; // Total number of comment-only source lines

  public LOC()
  {
  }

  public static void main(String[] args) 
  {
    LOC me = new LOC();
    Msg.setup(null);
    me.doit(args);
  }

  private void doit(String[] args) 
  {
    parseCmdLine(args, params);    // Parse the command line arguments

    Vector<String> inputFiles = files.getStringValues();
    int            l          = inputFiles.size();
    String         extension  = ext.getStringValue();
    boolean        longLines  = lline.specified();

    if (longLines) {
      System.out.println("   columns   longest line# ");
      System.out.println("===========================");
    } else {
      System.out.println("       All       Loc      Blank    Comment");
      System.out.println("==========================================");
    }

    for (int i = 0; i < inputFiles.size(); i++) { // For each file
      String aFile = inputFiles.elementAt(i);

      File file = new File(aFile);
      if (file.isDirectory()) {
        String[] con = file.list();
        for (int j = 0; j < con.length; j++)
          inputFiles.addElement(aFile + File.separator + con[j]);
        continue;
      }
      if (!aFile.endsWith(extension))
        continue;

      int currentLines   = 0; // Per file total number of source lines
      int currentBlank   = 0; // Per file total number of empty source lines
      int currentComment = 0; // Per file total number of comment-only source lines
      int longestLine    = 0;
      int longestLineno  = 0;

      FileInputStream fos = null;

      try {
        fos  = new FileInputStream(aFile);

        MyInputStreamReader cin  = new MyInputStreamReader(fos);

        boolean blank      = true;   // Line is blank so far.
        boolean comment    = false;  // Line is a comment line.
        boolean incomment  = false;  // Scanning inside a comment.
        boolean inquotes   = false;  // Scanning inside quotes.
        boolean noncomment = false;  // Not full line comment.

        int column = 0;
        int lineno = 1;
        while (true) {
          int c = cin.read();

          if (c < 0) // If end of file.
            break;

          column++;

          if (c == ' ') // Skip blanks.
            continue;

          if (c == '\n') {
            currentLines++;
            if (column > longestLine) {
              longestLine = column;
              longestLineno = lineno;
            }
            lineno++;
            column = 0;

            if (blank)
              currentBlank++; // It's an empty line.
            else if (comment)
              currentComment++; // It's a comment line.
            else if (incomment && !noncomment) // It's a multi-line comment line.
              currentComment++;
              
            blank = true;
            comment = false;
            noncomment = false;
            continue;
          }

          if (!inquotes && !incomment && (c == '/')) {
            c = cin.peek();
            if (c == '/') {
              comment |= blank;
              cin.skipline();
            } else if (c == '*') {
              comment |= blank;
              cin.skip();
              blank = false;
              incomment = true;
            }
            blank = false;
            continue;
          }

          blank = false;

          if (!incomment)
            noncomment = true;

          if (incomment) {
            if (c == '*') {
              c = cin.peek();
              if (c == '/') {
                incomment = false;
                comment |= !noncomment;
                cin.skip();
                continue;
              }
            }
          } else if (c == '"') {
            inquotes ^= true;
          }
        }

        if (column > longestLine) {
          longestLine = column;
          longestLineno = lineno;
        }

        cin.close();
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

      if (longLines) {
        System.out.print(formatNumber(longestLine));
        System.out.print("   ");
        System.out.print(formatNumber(longestLineno));
        System.out.print("      ");
        System.out.println(aFile);
      } else
        printMetrics(aFile, currentLines, currentBlank, currentComment);
    
      totalLines   += currentLines;
      totalBlank   += currentBlank;
      totalComment += currentComment;
    }

    if (!longLines) {
      System.out.println("==========================================");
      printMetrics("Total " + who.getStringValue(),
                   totalLines,
                   totalBlank,
                   totalComment);
    }
  }

  private String formatNumber(int x)
  {
    StringBuffer buf = new StringBuffer("          ");
    String       v   = Integer.toString(x);
    buf.replace(9 - v.length(), 9, v);
    return buf.toString();
  }
    
  private void printMetrics(String msg, int m1, int m2, int m3)
  {
    if (m1 <= 0)
      return;

    int loc = m1 - m2 - m3;
    System.out.print(formatNumber(m1));
    System.out.print(" ");
    System.out.print(formatNumber(loc));
    System.out.print(" ");
    System.out.print(formatNumber(m2));
    System.out.print(" ");
    System.out.print(formatNumber(m3));
    System.out.print("   ");
    System.out.println(msg);
  }

  protected void parseCmdLine(String[] args, CmdParam[] params)
  {
    try {
      if (CmdParam.parse(this.getClass().getName(), args, params, files))
        System.exit(0);
    } catch(scale.common.InvalidKeyException ex) {
      System.out.println(ex.getMessage());
      CmdParam.usage(System.out, this.getClass().getName(), params, files);
      System.exit(1);
    }
  }
}
