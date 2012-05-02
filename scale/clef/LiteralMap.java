package scale.clef;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.expr.CharLiteral;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.BooleanLiteral;
import scale.clef.expr.StringLiteral;
import scale.clef.type.Type;

/**
 * This class maps from a value to a Clef Literal.
 * <p>
 * $Id: LiteralMap.java,v 1.24 2007-10-04 19:58:03 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class allows us to have only one instance of a (type, value) tuple.
 */
public final class LiteralMap
{
  private static class Entry
  {
    public long    value; // The key to this entry
    public Type    type;  // The types must match too.
    public Literal lit;   // The value maps to this literal
    public Entry   next;  // The next entry with the same hash code.

    public Entry(long value, Type type, Literal lit, Entry next)
    {
      this.value = value;
      this.type  = type;
      this.lit   = lit;
      this.next  = next;
    }
  }

  /**
   * The array of lists indexed by the hash value
   */
  private static Entry[] map;
  /**
   * The number of entries in the map
   */
  private static int number;

  /**
   * @param capacity initial capacity - should be prime
   */
  static
  {
    clear();
  }

  /**
   * Clear out all literals and start over.
   */
  public static synchronized void clear()
  {
    map = new Entry[1011];
    number = 0;
  }

  private static int hash(long key)
  {
    return ((int) (key & 0x7fffffff)) % map.length;
  }

  private static void enlarge()
  {
    Entry[] oldmap = map;
    map = new Entry[number * 2 + 1];
    for (int i = 0; i < map.length; i++)
      map[i] = null;
    for (int j = 0; j < oldmap.length; j++) {
      Entry entry = oldmap[j];
      while (entry != null) {
        Entry next  = entry.next;
        int   index = hash(entry.value);
        entry.next = map[index];
        map[index] = entry;
        entry = next;
      }
    }
  }

  /**
   * Place an entry in the map unless it is already there.
   * There can be only one tuple with this value and type.
   * @param value map from this value to the literal
   * @param type must match for the entry to be there
   * @return the Literal
   */
  public static synchronized IntLiteral put(long value, Type type)
  {
    int   index = hash(value);
    Entry entry = map[index];
    while (entry != null) {
      if ((entry.value == value) && (entry.type == type))
        return (IntLiteral) entry.lit;
      entry = entry.next;
    }

    if (number > (4 * map.length)) {
      enlarge();
      index = hash(value);
    }


    IntLiteral lit = new IntLiteral(type, value);
    map[index] = new Entry(value, type, lit, map[index]);
    number++;
    return lit;
  }

  /**
   * Place an entry in the map unless it is already there.
   * There can be only one tuple with this value and type.
   * @param value map from this value to the literal
   * @param type must match for the entry to be there
   * @return the Literal
   */
  public static synchronized CharLiteral put(char value, Type type)
  {
    int   index = hash(value);
    Entry entry = map[index];
    while (entry != null) {
      if ((entry.value == value) && (entry.type == type))
        return (CharLiteral) entry.lit;
      entry = entry.next;
    }

    if (number > (4 * map.length)) {
      enlarge();
      index = hash(value);
    }

    CharLiteral lit = new CharLiteral(type, value);
    map[index] = new Entry(value, type, lit, map[index]);
    number++;
    return lit;
  }

  /**
   * Place an entry in the map unless it is already there.
   * There can be only one tuple with this value and type.
   * @param value map from this value to the literal
   * @param type must match for the entry to be there
   * @return the Literal
   */
  public static synchronized FloatLiteral put(double value, Type type)
  {
    long  hc    = Double.doubleToLongBits(value);
    int   index = hash(hc >> 32);
    Entry entry = map[index];
    while (entry != null) {
      if ((entry.value == hc) && (entry.type == type))
        return (FloatLiteral) entry.lit;

      entry = entry.next;
    }

    if (number > (4 * map.length)) {
      enlarge();
      index = hash(hc);
    }

    FloatLiteral lit = new FloatLiteral(type, value);
    map[index] = new Entry(hc, type, lit, map[index]);
    number++;
    return lit;
  }

   /**
   * Place an entry in the map unless it is already there.
   * There can be only one tuple with this value and type.
   * @param value map from this value to the literal
   * @param type must match for the entry to be there
   * @return the Literal
   */
 public static synchronized BooleanLiteral put(boolean value, Type type)
  {
    long  hc    = value ? 407 : 203;
    int   index = hash(hc);
    Entry entry = map[index];
    while (entry != null) {
      if ((entry.value == hc) && (entry.type == type))
        return (BooleanLiteral) entry.lit;
      entry = entry.next;
    }

    if (number > (4 * map.length)) {
      enlarge();
      index = hash(hc);
    }

    BooleanLiteral lit = new BooleanLiteral(type, value);
    map[index] = new Entry(hc, type, lit, map[index]);
    number++;
    return lit;
  }

   /**
   * Place an entry in the map unless it is already there.
   * There can be only one tuple with this value and type.
   * @param value map from this value to the literal
   * @param type must match for the entry to be there
   * @return the Literal
   */
  public static synchronized StringLiteral put(String value, Type type)
  {
    long  hc    = value.hashCode();
    int   index = hash(hc);
    Entry entry = map[index];
    while (entry != null) {
      if ((entry.value == hc) &&
          (entry.type == type) &&
          value.equals(((StringLiteral) entry.lit).getStringValue()))
        return (StringLiteral) entry.lit;
      entry = entry.next;
    }

    if (number > (4 * map.length)) {
      enlarge();
      index = hash(hc);
    }

    StringLiteral lit = new StringLiteral(type, value);
    map[index] = new Entry(hc, type, lit, map[index]);
    number++;
    return lit;
  }
}
