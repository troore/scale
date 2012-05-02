package scale.clef.stmt;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;

/**
 * This class is the abstract (base) class for all language imperative statements.
 * <p>
 * $Id: Statement.java,v 1.44 2007-08-09 17:29:22 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public abstract class Statement extends Node 
{
  private PragmaStk.Pragma pragma;
  private int              lineNumber;

  public Statement()
  {
    super();
    this.lineNumber = -1;
    this.pragma     = PragmaStk.defaultPragma;
  }

  public final void setPragma(PragmaStk.Pragma pragma)
  {
    this.pragma = pragma;
  }

  public final PragmaStk.Pragma getPragma()
  {
    return pragma;
  }

  /**
   * Return a String suitable for labeling this node in a graphical display.
   * This method should be over-ridden as it simplay returns the class name.
   */
  public String getDisplayLabel()
  {
    String mnemonic = toStringClass();
    if (mnemonic.endsWith("Stmt"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 4);
    return mnemonic;
  }

  /**
   * Return a String specifying the color to use for coloring this node in a graphical display.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTBLUE;
  }

  /**
   * Return a String specifying a shape to use when drawing this node in a graphical display.
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.ELLIPSE;
  }

  public void visit(Predicate p)
  {
    p.visitStatement(this);
  }

  /**
   * Return the source line number associated with this node or -1 if not known.
   */
  public final int getSourceLineNumber()
  {
    return lineNumber;
  }

  /**
   * Set the source line number associated with this node or -1 if not known.
   */
  public void setSourceLineNumber(int lineNumber)
  {
    this.lineNumber = lineNumber;
  }

  /**
   * Return true if this statement is, or contains, a return statement
   * or a call to <code>exit()</code>.
   */
  public boolean hasReturnStmt()
  {
    return false;
  }

  /**
   * Return the number of statements represented by this statement.
   * Most statements return 1.  A block statement returns sum of the
   * number of statements in each statement in the block.
   */
  public int numTotalStmts()
  {
    return 1;
  }

  /**
   * Return true if this statement is a loop statement or contains a
   * loop statement.
   */
  public boolean containsLoopStmt()
  {
    return false;
  }

  public void dump(String indents, java.io.PrintStream out)
  {
    out.print(Debug.formatInt(getSourceLineNumber(), 4));
    out.print(indents);
    out.print(" ");
    out.println(this);
  }
}
