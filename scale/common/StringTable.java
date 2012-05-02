package scale.common;

import java.util.Enumeration;

/**
 * This class maps from an integer value to a String.
 * <p>
 * $Id: StringTable.java,v 1.13 2007-01-04 16:57:59 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class StringTable
{
  private static class Entry
  {
    public long    key;   /* The key to this entry */
    public String  value; /* The key maps to this String */
    public int     flag;  /* A flag value. */
    public Entry   next;  /* The next entry with the same hash code. */

    public Entry(long key, String value, int flag, Entry next)
    {
      this.key   = key;
      this.value = value;
      this.flag  = flag;
      this.next  = next;
    }
  }

  /**
   * The array of lists indexed by the hash code.
   */
  private Entry[] map;
  /**
   * The number of entries in the map..
   */
  private int   number; 

  /**
   * @param capacity initial capacity - should be prime
   */
  public StringTable(int capacity)
  {
    reset(capacity);
  }

  /**
   * Remove all entries from the map.
   */
  public void clear()
  {
    reset(map.length);
  }

  /**
   * Remove all entries with the specified flag value from the map.
   * @see #put
   */
  public void clear(int flag)
  {
    for (int i = 0; i < map.length; i++) {
      Entry entry = map[i];
      Entry last = null;
      while (entry != null) {
        Entry next = entry.next;
        if (entry.flag == flag) {
          if (last == null) {
            map[i] = next;
            entry = null;
          } else {
            last.next = next;
            entry = last;
          }
        }
        last  = entry;
        entry = next;
      }
    }
  }

  private void reset(int capacity)
  {
    map = new Entry[capacity];
    for (int i = 0; i < map.length; i++)
      map[i] = null;
    number = 0;
  }

  private int hash(long key)
  {
    return ((int) (key & 0x7fffffff)) % map.length;
  }

  private void enlarge()
  {
    Entry[] oldmap = map;
    map = new Entry[number * 2 + 1];
    for (int i = 0; i < map.length; i++)
      map[i] = null;
    for (int j = 0; j < oldmap.length; j++) {
      Entry entry = oldmap[j];
      while (entry != null) {
        Entry next  = entry.next;
        int   index = hash(entry.key);
        entry.next = map[index];
        map[index] = entry;
        entry = next;
      }
    }
  }

  /**
   * Place an entry in the map unless it is already there.
   * There can be only one tuple with this key.
   * @param key map from this key to the value
   * @param flag is a separate integer value attached to the mapping
   * @return the previous value
   */
  public synchronized String put(long key, String value, int flag)
  {
    int   index = hash(key);
    Entry entry = map[index];
    while (entry != null) {
      if (entry.key == key) {
        String old = entry.value;
        entry.value = value;
        return old;
      }
      entry = entry.next;
    }

    if (number > 4 * map.length) {
      enlarge();
      index = hash(key);
    }


    map[index] = new Entry(key, value, flag, map[index]);
    number++;
    return null;
  }

  /**
   * Find an entry in the map.
   * There can be only one tuple with this key.
   * @param key map from this key to the value
   * @return the value
   */
  public synchronized String get(long key)
  {
    int   index = hash(key);
    Entry entry = map[index];
    while (entry != null) {
      if (entry.key == key) {
        return entry.value;
      }
      entry = entry.next;
    }
    return null;
  }

  private class StringTableEnum implements Enumeration<String>
  {
    int   index = -1;
    Entry next  = null;

    public StringTableEnum()
    {
      getNextRow();
    }

    private void getNextRow()
    {
      do {
        index++;
        if (index >= map.length)
          break;
        next = map[index];
      } while (next == null);
    }

    private void getNext()
    {
       next = next.next;
       if (next != null)
         return;
       getNextRow();
    }
          
        
    public boolean hasMoreElements()
    {
      return next != null;
    }

    public String nextElement()
    {
      String v = next.value;
      getNext();
      return v;
    }

    public String nextName()
    {
      String v = next.value;
      getNext();
      return v;
    }
  }

  /**
   * Return an enumeration of all of the elements in the map.
   */
  public Enumeration<String> elements()
  {
    return new StringTableEnum();
  }
}
