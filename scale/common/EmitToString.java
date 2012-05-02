package scale.common;

import java.io.PrintWriter;

/**
 * A class for emitting code sequences to a string.
 * <p>
 * $Id: EmitToString.java,v 1.10 2005-02-07 21:28:21 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class EmitToString extends Emit
{
  private StringBuffer buf; /* The buffer we send the generated program to. */

  /**
   * Create a class to use in writing out generated code.
   * @param pw is the stream to which the code is written
   * @param indent is the number of spaces to indent
   */
  public EmitToString(StringBuffer pw, int indent)
  {
    super(indent);
    buf = pw;
  }

  /**
   * Print a string to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public void emit(String s) 
  {
    if (lineStart)
      emitIndentation();

    buf.append(s);
    if (s != null)
      currentColumn += s.length();
  }

  /**
   * Print an integer to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public void emit(int n) 
  {
    emit(Integer.toString(n));
  }

  /**
   * Print a long to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public void emit(long n) 
  {
    emit(Long.toString(n));
  }

  /**
   * Print a char to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public void emit(char c) 
  {
    if (lineStart)
      emitIndentation();

    buf.append(c);
    currentColumn++;
  }

  /**
   * Signal the end of the current line and start a new one.
   */
  public void endLine() 
  {
    buf.append('\n');
    newLine();
  }

  /**
   * Add indentation to output
   */
  private void emitIndentation()
  {
    for (int i = 0; i < numIndent; i++)
      buf.append(' ');

    indent();
  }
}
