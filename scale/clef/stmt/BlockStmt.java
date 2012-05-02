package scale.clef.stmt;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;
import scale.clef.symtab.SymtabScope;

/**
 * This class represents a C-style block statement.
 * <p>
 * $Id: BlockStmt.java,v 1.45 2007-05-10 16:48:05 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class BlockStmt extends Statement
{
  private Vector<Statement> stmts; // A vector of the statements in the block.
  private SymtabScope       scope; // The symbol table scope for the block statement.

  public BlockStmt()
  {
    this(new Vector<Statement>(4));
  }

  public BlockStmt(Vector<Statement> stmts)
  {
    this.stmts = stmts;
  }
    
  /**
   * Specify the symbol table scope for the block statement.
   */
  public void setScope(SymtabScope scope)
  {
    this.scope = scope;
  }

  /**
   * Return the symbol table scope of the block statement.
   */
  public SymtabScope getScope()
  {
    return scope;
  }

  public void visit(Predicate p)
  {
    p.visitBlockStmt(this);
  }

  /**
   * Return the number of statements in the block.
   */
  public int numStmts()
  {
    return stmts.size();
  }

  /**
   * Return the specified statement in the block.
   */
  public Statement getStmt(int i)
  {
    return stmts.elementAt(i);
  }

  /**
   * Add a declaration statement to a block.  This is used primarily
   * to add temproary variable declaration statements.  The statement
   * is added after the last previous declaration statment.
   * @return the index of the statement
   */
  public int addDeclStmt(DeclStmt s)
  {
    int l = stmts.size();
    int i = 0;
    for (i = l - 1; i >= 0; i--) {
      Statement st = stmts.get(i);
      if (st instanceof DeclStmt)
        break;
    }
    i++;
    if (i >= l) {
      stmts.add(s);
      return l;
    }

    stmts.insertElementAt(s, i);
    return i;
  }

  /**
   * Add a statement to a block.  The statement is added to the end.
   */
  public void addStmt(Statement s)
  {
    stmts.add(s);
  }

  /**
   * Add a statement to a block before the specified statment.  This
   * is used primarily to add temproary variable declaration
   * statements.
   */
  public void insertStmt(Statement s, int i)
  {
    stmts.insertElementAt(s, i);
  }

  /**
   * Remove the last statement from the block statement.
   * @see scale.clef.expr.StatementOp
   */
  public Statement removeLastStatement()
  {
    return stmts.remove(stmts.size() - 1);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    return (Node) stmts.elementAt(i);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    if (stmts == null)
      return 0;
    return stmts.size();
  }

  /**
   * Set the source line number associated with this node or -1 if not known.
   */
  public final void setSourceLineNumber(int lineNumber)
  {
    super.setSourceLineNumber(lineNumber);

    if (stmts == null)
      return;

    // Propagate the line number down.

    int l = stmts.size();
    for (int i = 0; i < l; i++) {
      Statement stmt = stmts.get(i);
      if (stmt.getSourceLineNumber() > 0)
        break;
      stmt.setSourceLineNumber(lineNumber);
    }
  }

  /**
   * Return true if this statement is, or contains, a return statement
   * or a call to <code>exit()</code>.
   */
  public boolean hasReturnStmt()
  {
    if (stmts == null)
      return false;

    int l = stmts.size();
    for (int i = l - 1; i >= 0; i--) {
      Statement stmt = stmts.get(i);
      if (stmt.hasReturnStmt())
        return true;
    }

    return false;
  }

  /**
   * Return the number of statements represented by this statement.
   * Most statements return 1.  A block statement returns sum of the
   * number of statements in each statement in the block.
   */
  public int numTotalStmts()
  {
    int num = 0;
    int l   = stmts.size();
    for (int i = 0; i < l; i++)
      num += stmts.get(i).numTotalStmts();
    return num;
  }

  /**
   * Return true if this statement is a loop statement or contains a
   * loop statement.
   */
  public boolean containsLoopStmt()
  {
    int l = stmts.size();
    for (int i = 0; i < l; i++)
      if (stmts.get(i).containsLoopStmt())
        return true;
    return false;
  }

  public void dump(String indents, java.io.PrintStream out)
  {
    int l = numStmts();
    for (int i = 0; i < l; i++) {
      Statement s = getStmt(i);
      s.dump(indents + "  ", out);
    }
  }
}
