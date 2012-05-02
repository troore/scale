package scale.clef.symtab;

import scale.common.*;
import scale.clef.decl.Declaration;

import scale.clef.type.*;

/**
 * This class represents an entry in the symbol table.
 * <p>
 * $Id: SymtabEntry.java,v 1.27 2006-03-17 14:47:08 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A symbol table entry is a {@link scale.clef.decl.Declaration declataion}
 * and a link to its {@link scale.clef.symtab.SymtabScope scope}.
 */

public final class SymtabEntry
{
  /**
   * The scope for this symbol.
   */
  private SymtabScope scope;
  /**
   * The declaration for this symbol.
   */
  private Declaration declaration;

  /**
   * Create a symbol table entry for the declaration in the specified scope.
   */
  public SymtabEntry(SymtabScope scope, Declaration declaration)
  {
    this.scope       = scope;
    this.declaration = declaration;
  }

  /**
   * Return the number of the scope.
   */
  public final int getScopeNumber()
  {
    return scope.getScopeNumber();
  }

  /**
   * Return the scope in which this entry resides.
   */
  public final SymtabScope getScope()
  {
    return scope;
  }

  /**
   * Return the name of the symbol.
   */
  public final String getIdentifier()
  {
    return declaration.getName();
  }

  /**
   * Return the declaration for the symbol.
   */
  public final Declaration getDecl()
  {
    return declaration;
  }

  /**
   * Return the declaration for the symbol.
   */
  public final void setDecl(Declaration decl)
  {
    declaration = decl;
  }

  /**
   * Return the type of the symbol.
   */
  public final Type getType()
  {
    return declaration.getType();
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(SymtabEntry \"");
    buf.append(declaration.getName());
    buf.append("\" ");
    buf.append(scope.getScopeNumber());
    buf.append(' ');
    buf.append(declaration);
    buf.append(")");
    return buf.toString();
  }
}
