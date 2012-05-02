package scale.score.trans;

import scale.common.Stack;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;

/**
 * Map from an expression to another expression.
 * <p>
 * $Id: ExprMap.java,v 1.33 2007-10-04 19:58:35 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class is used solely by the value numbering optimization.
 * @see scale.score.trans.ValNum
 */
public final class ExprMap
{
  private static class Entry
  {
    /**
     * The key to this entry.
     */
    public Expr key;
    /**
     * The value maps to this literal.
     */
    public Expr value;
    /**
     * The use-def link to use.
     */
    public ExprChord ud;
    /**
     * True if a copy into a new temporary variable is required.
     */
    public boolean insertCopy;
    /**
     * True if there were multiple occurrences of this expression.
     */
    public boolean multipleOccurrences;
    /**
     * The next entry with the same hash code.
     */
    public Entry next;
 
    public Entry(Expr key, Expr value, ExprChord ud, boolean insertCopy, Entry next)
    {
      this.value      = value;
      this.key        = key;
      this.ud         = ud;
      this.next       = next;
      this.insertCopy = insertCopy;
      this.multipleOccurrences = false;
    }

    public void clear()
    {
      key        = null;
      value      = null;
      ud         = null;
      insertCopy = false;
    }

    public void reuse(Expr key, Expr value, ExprChord ud, boolean insertCopy)
    {
      this.value      = value;
      this.key        = key;
      this.ud         = ud;
      this.insertCopy = insertCopy;
    }
  }

  /**
   * The array of lists indexed by the hash value
   */
  private Entry[] map;
  /**
   * The number of entries in the map
   */
  private int number;
  /**
   * The current hash entry.
   */
  private Entry current;

  /**
   * @param capacity initial capacity - should be prime
   */
  public ExprMap(int capacity)
  {
    map = new Entry[capacity];
    for (int i = 0; i < map.length; i++)
      map[i] = null;
    number = 0;
  }

  private int hash(Expr key)
  {
    long canon = (key.getClass().hashCode() << 1) ^ key.getCoreType().hashCode();
    return ((int) (canon & 0x7fffffff)) % map.length;
  }

  private void enlarge()
  {
    Entry[] oldmap = map;
    map = new Entry[number * 2 + 1];

    for (int j = 0; j < oldmap.length; j++) {
      Entry entry = oldmap[j];
      while (entry != null) {
        Entry next  = entry.next;
        if (entry.key != null) {
          int index = hash(entry.key);
          entry.next = map[index];
          map[index] = entry;
        }
        entry = next;
      }
    }
  }

  /**
   * Place an entry in the map unless it is already there.
   * There can be only one tuple with this value.
   * If the entry is created, push the key on the stack.
   * @param key map from this expression
   * @param value map to this expression
   * @param ud use-def link
   * @param insertCopy is true if a copy into a new temporary variable
   * is needed
   * @param hashedExprs is the set of expressions that are mapped
   * @return the equivalent expression
   */
  public final synchronized Expr put(Expr        key,
                                     Expr        value,
                                     ExprChord   ud,
                                     boolean     insertCopy,
                                     Stack<Object> hashedExprs)
  {
    int   index = hash(key);
    Entry entry = map[index];
    Entry ent   = null;
    while (entry != null) {
      if (key.equivalent(entry.key)) {
        current = entry;
        entry.multipleOccurrences = true;
        return entry.value;
      } else if (entry.key == null)
        ent = entry;
      entry = entry.next;
    }

    int nv = value.numInDataEdges();
    if (nv > 0)
      value = value.copy();

    if (ent != null)
      ent.reuse(key, value, ud, insertCopy);
    else {
      if (number > (4 * map.length)) {
        enlarge();
        index = hash(key);
      }

      ent = new Entry(key, value, ud, insertCopy, map[index]);
      map[index] = ent;
      number++;
    }

    hashedExprs.push(ent);
    return value;
  }

  /**
   * Specify that the current entry has multiple occurrences.
   */
  public void specifyMultipleOccurrences()
  {
    current.multipleOccurrences = true;
  }

  /**
   * Return <code>true</code> if the expression has more than one occurrence.
   */
  public boolean hasMultipleOccurrences()
  {
    return current.multipleOccurrences;
  }

  /**
   * Return the mapped value for the specified key.
   * Return null if it is not found.
   * Remember this entry so that it may be accessed directly.
   * @see #getUseDef
   * @see #setUseDef
   */
  public final synchronized Expr get(Expr key)
  {
    int   index = hash(key);
    Entry entry = map[index];

    while (entry != null) {
      if (key.equivalent(entry.key)) {
        current = entry;
        return current.value;
      }
      entry = entry.next;
    }

    return null;
  }

  /**
   * Return true if a copy into a temporary is required.
   * Reset the switch so that false is returned the next time
   * making this a one-time switch.
   */
  public final synchronized boolean insertCopyRequired()
  {
    boolean ic = current.insertCopy;
    current.insertCopy = false;
    return ic;
  }

  /**
   * Return the ExprChord to use for the use-def link.
   */
  public final synchronized ExprChord getUseDef()
  {
    return current.ud;
  }

  /**
   * Return the actual expression used as the key.
   */
  public final synchronized Expr getKey()
  {
    return current.key;
  }

  /**
   *  Set the ExprChord to use for the use-def link.
   */
  public final synchronized void setUseDef(ExprChord se)
  {
    current.ud = se;
  }

  /**
   * Remove the specified mapping.
   */
  public final synchronized void remove(Stack<Object> hashedExprs)
  {
    while (!hashedExprs.empty()) {
      Entry ent = (Entry) hashedExprs.pop();
      if (ent == current)
        current = null;
      ent.clear();
    }
  }
}
