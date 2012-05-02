package scale.clef.symtab;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;

/**
 * This class represents a single, local scope (e.g., for a procedure
 * or local block).
 * <p>
 * $Id: SymtabScope.java,v 1.45 2007-08-28 13:34:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The primary function of a scope is to maintain a table of
 * (identifier, decl) pairs.  A list of the entries in the order they
 * were added to the scope is also maintained for use when C code is
 * generated.
*/

public class SymtabScope
{
  private static int lastScopeNumber = 0; // Last scope no. allocated.

  private HashMap<String, Vector<SymtabEntry>> localSymtab; // This scope's local symbol table.
  private SymtabScope              outerScope;  // Points to the outer scope containg this scope.
  private Vector<SymtabScope>      children;    // A vector of SymtabScope nodes.
  private Vector<SymtabEntry>      order;       // The order that symbols are added.
  private int                      scopeNumber; // This field is this scope's unique identifier.
  private int                      scopeDepth;  // The depth of this scope in the scope tree.

  /**
   * Constructor to create a symbol table scope.
   * @param depth the current nesting depth of the scope block
   */
  public SymtabScope(int depth)
  {
    scopeNumber = ++lastScopeNumber;
    scopeDepth  = depth;
    localSymtab = new HashMap<String, Vector<SymtabEntry>>(23);
    order       = new Vector<SymtabEntry>(3);
    children    = new Vector<SymtabScope>(2);
    outerScope  = null;
  }

  /**
   * Return a unique integer that represents this scope.
   */
  public final int getScopeNumber()
  {
    return scopeNumber;
  }

  /**
   * Return the depth of the scope in the scope tree.
   */
  public final int getScopeDepth()
  {
    return scopeDepth;
  }

  /**
   * This method looks for the identifier in this scope.  If the
   * identifier is present, the list of entry nodes is returned.
   * Otherwise, NULL is returned.
   *
   * @param id the name of a symbol
   * @return the list of symbol table entries for the name
   */
  public final Vector<SymtabEntry> lookup(String id)
  {
    return localSymtab.get(id);
  }

  /**
   * Look for a declaration in this scope.  If the declaration
   * is present, then return the symbol table entry.  Otherwise,
   * return null
   */
  public final SymtabEntry lookupSymbol(Declaration d) 
  {
    Vector<SymtabEntry> syms = lookup(d.getName());
    if (syms == null)
      return null;
    int l = syms.size();
    for (int i = 0; i < l; i++) {
      SymtabEntry entry = syms.elementAt(i);
      if (entry.getDecl() == d) {
        return entry;
      }
    }
    return null;
  }

  /**
   * This method adds a new entry to the local symbol table.  It is
   * responsible for finding or creating an appropriate {@link
   * SymtabEntry SymtabEntry}.
   *
   * @param decl the declaration for the symbol
   * @return a SymtabEntry for the symbol
   */
  public final SymtabEntry addEntry(Declaration decl)
  {
    String      id      = decl.getName();
    SymtabEntry entry   = new SymtabEntry(this, decl);
    Vector<SymtabEntry> entries = lookup(id);

    if (entries != null) {
      entries.addElement(entry);
    } else {
      entries = new Vector<SymtabEntry>();
      entries.addElement(entry);
      localSymtab.put(id, entries);
    }
    order.addElement(entry);
    return entry;
  }

  /**
   * This method replaces an existing declaration in an existing entry
   * of the local symbol table.
   *
   * @param oldDecl the old declaration for the symbol
   * @param newDecl the new declaration for the symbol
   * @return a SymtabEntry for the new declaration or
   * <code>null</code> if not found
   */
  public final SymtabEntry replaceEntry(Declaration oldDecl, Declaration newDecl)
  {
    String id      = oldDecl.getName();
    Vector<SymtabEntry> entries = lookup(id);

    if (entries != null) {
      int l = entries.size();
      for (int i = 0; i < l; i++) {
        SymtabEntry entry = entries.get(i);
        if (oldDecl == entry.getDecl()) {
          entry.setDecl(newDecl);
          return entry;
        }
      }
    }

    return null;
  }

  /**
   * Make this entry the last in the ordered list.  If it already
   * exists in the ordered list, it is moved to the end of the list.
   */
  public final void reorder(SymtabEntry se)
  {
    int index = order.indexOf(se);
    assert (index >= 0) : "Entry not from scope " + se;
    order.remove(index);
    order.addElement(se);
  }

  /**
   * Return the scope containing this scope.
   */
  public final SymtabScope getOuterScope()
  { 
    return outerScope; 
  }

  /**
   * Add a child to the scope - represents a nested scope
   * @param child the nested scope
   */
  public final void appendChild(SymtabScope child)
  { 
    children.addElement(child);
    child.setOuterScope(this);
  }

  /**
   * Returns an enumeration of symbols in the scope - in the order that
   * the symbols were added to the scope.
   */
  public final synchronized Enumeration<SymtabEntry> orderedElements()
  {
    return order.elements();
  }
  
  public String toString()
  {
    StringBuffer tmp = new StringBuffer("(Scope ");

    tmp.append(getScopeNumber());
    tmp.append(' ');
    tmp.append(getScopeDepth());

    Enumeration<String> e = localSymtab.keys();
    while (e.hasMoreElements()) {
      String              id = e.nextElement();
      Vector<SymtabEntry> v  = localSymtab.get(id);
      int    l  = v.size();

      tmp.append(' ');
      tmp.append(id);
      tmp.append('{');

      for (int i = 0; i < l; i++) {
        if (i > 0)
          tmp.append(", ");
        tmp.append(v.elementAt(i));
      }
      tmp.append('}');
    }

    tmp.append(')');

    return tmp.toString();
  }

  /**
   * Specify the outer scope of this scope.
   */
  private final void setOuterScope(SymtabScope outerScope)
  {
    this.outerScope = outerScope;
  }
}
