package scale.frontend.c;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/** 
 * This class reads a C file and transforms it by translating special
 * character sequences and catenating "continued" lines.
 * <p>
 * $Id: CReader.java,v 1.10 2006-05-15 19:37:06 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class CReader extends java.io.Reader
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  private String     filename;     /* Currently accessed file's name. */
  private FileReader reader;       /* Currently accessed file's reader. */
  private int        lineNumber;   /* Current line number in current file. */
  private char[]     buffer;       /* Current buffer of characters from the stream. */
  private int        bufLength;    /* The number of characters in the buffer. */
  private int        bufPosition;  /* Current position in the buffer.*/
  private char[]     line;         /* Current buffer of characters from the stream. */
  private int        lineLength;   /* The number of characters in the line. */
  private int        linePosition; /* Current position in the line.*/

  public CReader(String filename) throws IOException
  {
    super();

    this.buffer        = new char[1024];
    this.line          = new char[1024];
    this.filename      = filename;
    this.reader        = new FileReader(filename);
    this.lineNumber    = 1;
    this.bufLength     = 0;
    this.bufPosition   = 0;
    this.lineLength    = 0;
    this.linePosition  = 0;
  }

  public void close() throws IOException
  {
    reader.close();
    reader = null;
  }

  /**
   * @return true if at end-of-file
   */
  private boolean fillBuffer() throws IOException
  {
    bufPosition = 0;
    bufLength = reader.read(buffer, 0, buffer.length);
    return (bufLength <= 0);
  }

  /**
   * Read characters into a portion of an array.
   * @param cbuf - destination buffer
   * @param off - offset at which to start storing characters
   * @param len - maximum number of characters to read
   * @return the number of bytes read, or -1 if the end of the stream has already been reached
   */
  public int read(char[] cbuf, int off, int len) throws IOException
  {
    if ((linePosition >= lineLength) && fillLine())
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
  public String readLine() throws IOException
  {
    if ((linePosition >= lineLength) && fillLine())
      return null;
    String str = new String(line, linePosition, lineLength - linePosition - 1);
    lineLength = 0;
    return str;
  }

  public final boolean getLineArray(char[] text, int position)
  {
    if (text.length < position + lineLength)
      return true;

    if (classTrace)
      System.out.println("CR " + (lineNumber - 1) + "   :" + new String(line, 0, lineLength - 1));
    System.arraycopy(line, 0, text, position, lineLength);
    return true;
  }

  /**
   * Return the current position in the current line.
   */
  public final int getLinePosition()
  {
    return linePosition;
  }

  /**
   * Return the length of the current line.
   */
  public final int getLineLength()
  {
    return lineLength;
  }

  /**
   * Fill the line buffer.  Convert trigraphs. Converts '\r' to '\n'
   * and all Unicode space characters to ' '.  Keep track of the
   * source file line number.
   * @return true if at end-of-file
   */
  public boolean fillLine() throws IOException
  {
    linePosition = 0;

    if ((bufPosition >= bufLength) && fillBuffer())
      return true;

    int     p  = 0;
    boolean tg = false;
    while (true) {
    fill:
      for (int i = bufPosition; i < bufLength; i++) {
        if (p >= line.length - 1) {
          char[] nl = new char[line.length * 2];
          System.arraycopy(line, 0, nl, 0, line.length);
          line = nl;
        }

        char c = buffer[i];
        if (c == '\n') {
          lineNumber++;
          bufPosition = i + 1;
          if ((p > 0) && line[p - 1] == '\\') {
            p--;
            continue;
          }
          line[p++] = c;
          lineLength = p;
          if (tg)
            processTrigraphs();
          return false;
        }

        if (c == '\r') {
          bufPosition = i + 1;
          lineNumber++;
          if (bufPosition >= bufLength) {
            fillBuffer();
            i = -1;
          }
          if (buffer[bufPosition] == '\n') {
            bufPosition++;
            i++;
          }
          if ((p > 0) && line[p - 1] == '\\') {
            p--;
            continue;
          }
          line[p++] = '\n';
          lineLength = p;
          if (tg)
            processTrigraphs();
          return false;
        }

        if (Character.isSpaceChar(c)) {
          c = ' ';
        } else if (c == '?')
          tg = true;

        line[p++] = c;
      }

      if (fillBuffer()) {
        lineLength = p;
        if (tg)
          processTrigraphs();
        return false;
      }
    }
  }

  private void processTrigraphs()
  {
    int k = 0;
    int i = 0;
    while (i < lineLength) {
      char c = line[i];
      if (c == '?') {
        if (line[i + 1] == '?') {
          char g = line[i + 2];
          if (g == '=') {
            c = '#';
            i += 2;
          } else if (g == '(')  {
            c = '[';
            i += 2;
          } else if (g == '/')  {
            c = '\\';
            i += 2;
          } else if (g == ')')  {
            c = ']';
            i += 2;
          } else if (g == '\'')  {
            c = '^';
            i += 2;
          } else if (g == '<')  {
            c = '{';
            i += 2;
          } else if (g == '!') {
            c = '|';
            i += 2;
          } else if (g == '>') {
            c = '}';
            i += 2;
          } else if (g == '-') {
            c = '~';
            i += 2;
          }
        }
      }
      line[k++] = c;
      i++;
    }
    lineLength = k;
  }

  /**
   * Read a single character. Line terminators are compressed into single newline ('\n') characters.
   * @return the character read, or -1 if the end of the stream has been reached
   */
  public int read() throws IOException
  {
    if ((linePosition >= lineLength) && fillLine())
      return -1;
    char c = line[linePosition++];
    return c;
  }

  /**
   * Set the line number of the next line to be read.
   * The lines are numbered starting at 1.
   */
  public void setLineNumber(int lineNumber)
  {
    this.lineNumber = lineNumber;
  }

  /**
   * Return the line number of the next line to be read.
   * The lines are numbered starting at 1.
   */
  public int getLineNumber()
  {
    return lineNumber - 1;
  }

  /**
   * Set the file name associated with this reader.
   */
  public void setFilename(String filename)
  {
    this.filename = filename;
  }

  /**
   * Return the file name associated with this reader.
   */
  public String getFilename()
  {
    return filename;
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
      if (fillLine())
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
    throw new java.io.IOException("Mark not supported");
  }

  /**
   * Marking is not supported by this Reader.
   */
  public boolean markSupported()
  {
    return false;
  }

  /**
   * Reset the stream to the most recent mark.
   */
  public void reset() throws IOException
  {
    throw new java.io.IOException("Mark not supported");
  }
}
