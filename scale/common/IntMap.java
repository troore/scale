package scale.common;

import java.util.Enumeration;

/**
 * This class maps from an integer value to an Object.
 * <p>
 * $Id: IntMap.java,v 1.17 2007-10-04 19:58:10 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class IntMap<T>
{
  private static class Entry
  {
    public int    key;   /* The key to this entry */
    public Object value; /* The key maps to this Object */
    public Entry  next;  /* The next entry with the same hash code. */

    public Entry(int key, Object value, Entry next)
    {
      this.key   = key;
      this.value = value;
      this.next  = next;
    }
  }

  private Entry[] map;    // The array of lists indexed by the hash code.
  private int     number; // The number of entries in the map..

  /**
   * @param capacity initial capacity - should be prime
   */
  public IntMap(int capacity)
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

  private void reset(int capacity)
  {
    number = 0;

    if ((map == null) || (capacity >= map.length)) {
      map = new Entry[capacity];
      return;
    }

    for (int i = 0; i < map.length; i++)
      map[i] = null;
  }

  private int hash(int key)
  {
    return ((key & 0x7fffffff) % map.length);
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
   * @return the previous value
   */
  @SuppressWarnings("unchecked")
  public synchronized T put(int key, T value)
  {
    int   index = hash(key);
    Entry entry = map[index];
    while (entry != null) {
      if (entry.key == key) {
        T old = (T) entry.value;
        entry.value = value;
        return old;
      }
      entry = entry.next;
    }

    if (number > 4 * map.length) {
      enlarge();
      index = hash(key);
    }


    map[index] = new Entry(key, value, map[index]);
    number++;
    return null;
  }

  /**
   * Find an entry in the map.
   * There can be only one tuple with this key.
   * @param key map from this key to the value
   * @return the value
   */
  @SuppressWarnings("unchecked")
  public synchronized T get(int key)
  {
    int   index = hash(key);
    Entry entry = map[index];
    while (entry != null) {
      if (entry.key == key) {
        return (T) entry.value;
      }
      entry = entry.next;
    }
    return null;
  }

  private class IntMapEnum<T> implements Enumeration<T>
  {
    int   index = -1;
    Entry next  = null;

    public IntMapEnum()
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

    @SuppressWarnings("unchecked")
    public T nextElement()
    {
      Object v = next.value;
      getNext();
      return (T) v;
    }
  }

  /**
   * Return an enumeration of all of the elements in the map.
   */
  public Enumeration<T> elements()
  {
    return new IntMapEnum<T>();
  }
}
