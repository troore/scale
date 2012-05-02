package scale.clef.symtab;

import scale.common.*;
import scale.clef.*;
import scale.clef.Node;
import scale.clef.decl.*;

/**
 * A class to represent a program's symbol table.
 * <p>
 * $Id: Symtab.java,v 1.48 2007-08-30 01:48:30 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A symbol table comprises many scopes arranged in a tree.  Each
 * scope has a unique identification (the scope number) and a local
 * symbol table.  A local symbol table is a dictionary which pairs
 * identifiers with entries.  A single "entries" is a list of
 * entry nodes, where each node represents a single declared
 * entity.  We maintain a list of entry nodes because an identifier
 * could conceivably be used to name multiple entities even within
 * a single scope.  Though not currently used, we do assume that
 * with a scope, each use of a single identifier names a different
 * kind of entity.
 *
 * The other classes are:
 * <dl>
 *  <dt>{@link scale.clef.symtab.SymtabScope SymtabScope}
 *      <dd>Represents a single scope.
 *  <dt>{@link scale.clef.symtab.SymtabEntry SymtabEntry}
 *      <dd>Represents a single symbol table entry.
 * </dl>
 */
public final class Symtab
{
  /**
   * The root of the scope tree has depth zero.  The currentDepth
   * field should always hold depth of currently active scope.
   */
  private SymtabScope                       rootScope;    // The root of the nested scopes.
  private SymtabScope                       currentScope; // The current scope.
  private HashMap<Declaration, SymtabScope> declScope;    // Map from Declaration to scope.
  private int                               currentDepth; // The current depth of the nested scopes.

  /**
   * Create a symbol table.
   */
  public Symtab()
  {
    rootScope    = null;
    currentScope = null; 
    currentDepth = -1;  // -1 indicates that no scopes are yet active.`
    declScope    = new HashMap<Declaration, SymtabScope>(203);
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(Symtab depth:");
    buf.append(currentDepth);
    buf.append(" Current:");
    buf.append(currentScope);
    buf.append(')');
    return buf.toString();
  }
    
  /**
   * Open a new scope as a child of the current socpe.  The new scope
   * becomes the current scope.
   * @return a pointer to the new current scope.
   * @exception InvalidTableError if we have incorrectly created a new
   * scope
   */
  public final SymtabScope beginScope()
  {
    SymtabScope scope = new SymtabScope(++currentDepth);
    if (rootScope == null) {
      rootScope = scope;
    } else {                    // Subsequent scopes in this symbol table.
      assert (currentScope != null) : "Current Scope not set";
      currentScope.appendChild(scope);
    }
    currentScope = scope;
    return currentScope;
  }

  /**
   * Ends the current scope, and the current scope's
   * outer scope becomes the current scope.
   * @return a pointer to the new current scope.
   */
  public final SymtabScope endScope()
  {
    currentDepth--;
    currentScope = currentScope.getOuterScope();
    // Let root continue to point to the root scope, so we can
    // continue to use the symbol table after it is complete.
    return currentScope;
  }

  /**
   * Return the current scope.
   */
  public final SymtabScope getCurrentScope()
  {
    return currentScope;
  }

  /**
   * Return the root scope.
   */
  public final SymtabScope getRootScope()
  {
    return rootScope;
  }

  /**
   * Return true if the current scope is the root scope.
   */
  public boolean isAtRoot()
  {
    return (currentScope == rootScope);
  }

  /**
   * Set the current scope.  Use this routine when walking through a
   * clef tree to set the scope.  Then, new symbols and scopes may
   * be added to the symbol table at the correct place.
   * @param scope the scope
   */
  public final void setCurrentScope(SymtabScope scope)
  {
    currentScope = scope;
  }

  /**
   * Add a symbol to the current scope.
   * @param decl the declaration for the symbol
   * @return a symbol table entry for the symbol
   */
  public final SymtabEntry addSymbol(Declaration decl)
  {
    declScope.put(decl, currentScope);
    return currentScope.addEntry(decl);
  }

  /**
   * Replace an existing symbol in the current scope.
   * @param oldDecl the old declaration for the symbol
   * @param newDecl the new declaration for the symbol
   * @return a SymtabEntry for the new declaration or
   * <code>null</code> if not found
   */
  public final SymtabEntry replaceSymbol(Declaration oldDecl, Declaration newDecl)
  {
    declScope.remove(oldDecl);
    declScope.put(newDecl, currentScope);
    return currentScope.replaceEntry(oldDecl, newDecl);
  }

  /**
   * Add a symbol to the current scope.
   * @param decl the declaration for the symbol
   * @return a symbol table entry for the symbol
   */
  public final SymtabEntry addRootSymbol(Declaration decl)
  {
    declScope.put(decl, rootScope);
    return rootScope.addEntry(decl);
  }

  /**
   * Return the scope associated with the Declaration.
   */
  public SymtabScope lookupScope(Declaration decl)
  {
    return declScope.get(decl);
  }

  /**
   * Look in the current scope for a symbol table entry matching a
   * specific name.  The current scope includes enclosing scopes as
   * well.  This routine returns a list of symbol table entries since
   * the name may be defined more than once.  It is the responsibility
   * of the user to search through the list for the appropriate entry.
   * The current scope's entries are first in the vector.
   * 
   * @param id the symbol's name
   * @return a vector of SymtabEntry instances in order of scope
   */
  public final Vector<SymtabEntry> lookupSymbol(String id) 
  {
    Vector<SymtabEntry> v = new Vector<SymtabEntry>(10);
    for (SymtabScope cs = currentScope; cs != null; cs = cs.getOuterScope()) {
      Vector<SymtabEntry> entries = cs.lookup(id);

      if (entries == null)
	continue;

      v.addVectors(entries);
    }
    return v;
  }

  /**
   * Return the symbol table entry for a specific declaration.  If the
   * declaration doesn't appear in the current scope, then return
   * null.  The current scope includes the enclosing scopes as well.
   * We return the symbol table entry that is closest to the 
   * scope that we're currently in.
   *
   * @param d the declaration
   * @return the symbol table entry for the declaration (or null if not 
   * found).
   */
  public final SymtabEntry lookupSymbol(Declaration d) 
  {
    Vector<SymtabEntry> entries = lookupSymbol(d.getName());
    if (entries == null)
       return null;

    int l = entries.size();
    for (int i = 0; i < l; i++) {
      SymtabEntry entry = entries.elementAt(i);
      if (entry.getDecl() == d)
        return entry;
    }

    return null;
  }

  /**
   * Return the RoutineDecl specified.
   */
  public RoutineDecl findRoutine(String rname)
  {
    Vector<SymtabEntry> se = lookupSymbol(rname);
    if (se != null) {
      int l = se.size();
      for (int i = 0; i < l; i++) {
        SymtabEntry entry = se.elementAt(i);
        RoutineDecl rd    = entry.getDecl().returnRoutineDecl();
        if (rd != null)
          return rd;
      }
    }
    return null;
  }
}
