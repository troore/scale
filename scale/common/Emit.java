package scale.common;

import java.io.PrintWriter;

/**
 * An abstract class for emitting code sequences. 
 * <p>
 * $Id: Emit.java,v 1.17 2005-02-07 21:28:20 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The interface supports indentation.
 */
public abstract class Emit
{
  /**
   * <code>true</code> if we are at the start of a line in the generated output. 
   */
  protected boolean lineStart;     
  /**
   * The current line number in the generated output. 
   */
  protected int currentLine;   
  /**
   * Number of indentation levels - used to record the amount of identatation. 
   */
  protected int numIndent;     
  /**
   * The amount of indentation spaces per indentation level. 
   */
  protected int spaces;        
  /**
   * The current output column. 
   */
  protected int currentColumn; 

  /**
   * Create a class to use in writing out generated code.
   * @param indent is the number of spaces to indent
   */
  public Emit(int indent)
  {
    this(0, 0, indent);
  }

  /**
   * Create a class to use in writing out generated code.
   * @param currentLine is the current output line number
   * @param currentColumn is the current output column number
   * @param indent is the number of spaces to indent
   */
  public Emit(int currentLine, int currentColumn, int indent)
  {
    this.lineStart     = (currentColumn > 0);
    this.currentLine   = currentLine;
    this.currentColumn = currentColumn;
    this.numIndent     = 0;
    this.spaces        = indent;
  }

  /**
   * Print a string to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public abstract void emit(String s);

  /**
   * Print an integer to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public abstract void emit(int n);

  /**
   * Print a long to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public abstract void emit(long n);

  /**
   * Print a character to the output.  If we are at the beginning of 
   * a line, then generate some indentation.
   */
  public abstract void emit(char c);

  /**
   * Signal the end of the current line and start a new one.
   */
  public void endLine() 
  {
    lineStart = true;
    currentLine++;
    currentColumn = 0;
  }

  /**
   * Add indentation to output
   */
  protected void indent()
  {
    currentColumn = numIndent;
    lineStart = false;
  }

  /**
   * Specify that a new line has been started.
   */
  protected void newLine()
  {
    lineStart = true;
    currentLine++;
    currentColumn = 0;
  }

  /**
   * Increment the number of spaces to indent
   */
  public void incIndLevel()
  {
    numIndent += spaces;
  }

  /**
   * Decrease the amount of space for indentation
   */
  public void decIndLevel()
  {
    numIndent -= spaces;
  }

  /**
   * Set the amount of indentation space for each level.
   */
  public void setIndentation(int n)
  {
    spaces = n;
  }

  /**
   * Return the current line number of the line being generated.
   */
  public int getCurrentLine()
  {
    return currentLine;
  }

  /**
   * Return the current output column.
   */
  public int getCurrentColumn()
  {
    return currentColumn;
  }
}
