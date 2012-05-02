package scale.common;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * This class defines a table structure that is used to record various
 * pieces of information.
 * <p>
 * $Id: Table.java,v 1.27 2006-08-18 21:54:37 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The table has a very simple structure: a {@link HashMap HashMap}
 * points to {@link HashSet HashSet}s which point to individual
 * objects.
 * <p>
 * The table is optimized to not create a HashSet for a row if the row
 * contains just one element.  This means that the elements of a row
 * can not be declarted as HashSets.
 * @see HashMap
 * @see HashSet
 */
public class Table<K,V>
{
  private static final long serialVersionUID = 42L;

  private HashMap<K,Object> tab;

  public Table()
  {
    this.tab = new HashMap<K,Object>(203);
  }

  public Table(int capacity)
  {
    this.tab = new HashMap<K,Object>(capacity);
  }

  @SuppressWarnings("unchecked")
  public Table(Table<K,V> old)
  {
    this.tab = new HashMap<K,Object>(old.tab.size());
    Enumeration<K> keys = old.tab.keys();
    while (keys.hasMoreElements()) {
      K      key = keys.nextElement();
      Object val = old.tab.get(key);
      if (val instanceof HashSet)
        val = ((HashSet<V>) val).clone();
      tab.put(key, val);
    }
  }

  /**
   * Empty the table.
   */
  public void clear()
  {
    tab.clear();
  }

  /**
   * This method adds a table to this table.
   */
  @SuppressWarnings("unchecked")
  public void add(Table<K,V> old)
  {
    Enumeration<K> keys = old.tab.keys();
    while (keys.hasMoreElements()) {
      K      key = keys.nextElement();
      Object val = old.tab.get(key);
      Object mv  = tab.get(key);

      if (val instanceof HashSet) {
        HashSet<V> hs = (HashSet<V>) val;

        if (mv == null)
          tab.put(key, hs.clone());
        else if (mv instanceof HashSet) {
          HashSet<V> mvhs = (HashSet<V>) mv;
          mvhs.add(hs);
        } else {
          hs.add((V) mv);
          tab.put(key, hs);
        }
      } else if (mv instanceof HashSet) {
        HashSet<V> mvhs = (HashSet<V>) mv;
        mvhs.add((V) val);
      } else if (mv != null) {
        HashSet<V> s = new HashSet<V>(3);
        s.add((V) mv);
        s.add((V) val);
        tab.put(key, s);
      } else
        tab.put(key, val);
    }
  }

  /**
   * This method adds a new object into the table.  
   * @param key is the row index
   * @param value is the column index (it may not be a HashSet)
   * @return true if the value was not already in the Table
   */
  @SuppressWarnings("unchecked")
  public boolean add(K key, V value)
  {
    if (value instanceof HashSet)
      throw new scale.common.InternalError("A HashSet is not allowed as an element in a Table.");

    Object set = tab.get(key);
    if (set == null) {
      tab.put(key, value);
      return true;
    }

    if (set instanceof HashSet) {
      HashSet<V> hs = (HashSet<V>) set;
      return hs.add(value);
    }

    if (set != value) {
      HashSet<V> s = new HashSet<V>(3);
      tab.put(key, s);
      s.add((V) set);
      return s.add(value);
    }

    return false;
  }

  /**
   * This method adds a new object into the table.  
   * @param key is the row index
   * @param value is the column index (it may not be a HashSet)
   * @return null
   */
  public Object put(K key, V value)
  {
    add(key, value);
    return null;
  }

  /**
   * This method determines if an object is already in the table.
   * @param key is the row index
   * @param value is the column index (it may not be a HashSet)
   * @return the object if it is in the table or null.
   */
  @SuppressWarnings("unchecked")
  public V get(K key, V value)
  {
    Object set = tab.get(key);
    if (set instanceof HashSet) {
      if (((HashSet) set).contains(value))
        return value;
      return null;
    } else if (set == value)
      return value;
    return null;
  }

  /**
   * Return an array of the objects in a row.
   * @param key the row index
   */
  @SuppressWarnings("unchecked")
  public Object[] getRowArray(K key)
  {
    Object set = tab.get(key);

    if (set instanceof HashSet)
      return ((HashSet<V>) set).toArray();

    if (set == null)
      return new Object[0];

    Object[] v = new Object[1];
    v[0] = set;
    return v;
  }

  /**
   * Return a HashSet of the objects in a row.
   * @param key the row index
   */
  @SuppressWarnings("unchecked")
  public HashSet<V> getRowSet(K key)
  {
    Object set = tab.get(key);

    if (set instanceof HashSet)
      return (HashSet<V>) set;

    HashSet<V> s = new HashSet<V>(3);
    if (set != null)
      s.add((V) set);
    tab.put(key, s);
    return s;
  }

  /**
   * Return true if the row contains the value.
   * Uses the equals() method.
   */
  public boolean rowContains(K key, Object value)
  {
    Object set = tab.get(key);
    if (set == null)
      return false;

    if (set instanceof HashSet)
      return ((HashSet) set).contains(value);

    return set.equals(value);
  }

  /**
   * Return true if the row is empty.
   * @param key the row index
   */
  public boolean isRowEmpty(K key)
  {
    Object set = tab.get(key);

    if (set == null)
      return true;

    return false;
  }

  /**
   * Return an iteration of the objects in a row.
   * @param key the row index
   */
  @SuppressWarnings("unchecked")
  public Iterator<V> getRowEnumeration(K key)
  {
    Object set = tab.get(key);

    if (set == null)
      return new EmptyIterator<V>();

    if (set instanceof HashSet)
      return ((HashSet<V>) set).iterator();

    return new SingleIterator<V>((V) set);
  }

  /**
   * This methhod removes an entire row from the table.
   * @param key the row index
   */
  public void removeRow(K key)
  {
    tab.remove(key);
  }

  /**
   * This method removes an object from the table.
   * @param key is the row index
   * @param value is the column index
   * @return the value if the value was in the row, otherwise null
   */
  public Object remove(K key, V value)
  {
    Object set = tab.get(key);
    if (set == null)
      return null;

    if (set == value) {
      tab.remove(key);
      return value;
    }

    if ((set instanceof HashSet) && (((HashSet) set).remove(value)))
      return value;

    return null;
  }

  /**
   * Return an enumeration of the row key value.
   */
  public Enumeration<K> keys()
  {
    return tab.keys();
  }

  /**
   * Return the number of rows of the table.
   */
  public int numRows()
  {
    return tab.size();
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(Table ");
    buf.append(hashCode());
    buf.append(')');
    return buf.toString();
  }

  private class TableEnumeration<V> implements Enumeration<V>
  {
    V                   next;
    Iterator<V>         it;
    Enumeration<K>      keys;
    Table<K,V>          tab;

    @SuppressWarnings("unchecked")
    public TableEnumeration(Table<K,V> tab)
    {
      this.tab  = tab;
      this.next = null;
      this.it   = null;
      this.keys = tab.keys();

      if (keys.hasMoreElements()) {
        Object s = tab.tab.get(keys.nextElement());
        if (s instanceof HashSet) {
          this.it = ((HashSet<V>) s).iterator();
          this.next = getNext();
        } else
          this.next = (V) s;
      }
    }

   @SuppressWarnings("unchecked")
   private V getNext()
    {
      while (true) {
        if ((it != null) && it.hasNext())
          return it.next();

        if (!keys.hasMoreElements())
          return null;

        K key = keys.nextElement();
        Object s   = tab.tab.get(key);
        if (s instanceof HashSet) {
          it = ((HashSet<V>) s).iterator();
          if (it.hasNext())
            return it.next();
        } else {
          it = null;
          return (V) s;
        }
      }
    }

    public boolean hasMoreElements()
    {
      return (next != null);
    }

    public V nextElement()
    {
      V x = next;
      next = getNext();
      return x;
    }
  }

  /**
   * Return an enumeration of all the elements of this <code>Table</code>.
   */
  public Enumeration<V> elements()
  {
    return new TableEnumeration<V>(this);
  }

  /**
   * Return true if the value is contained in the <code>Table</code>.
   */
  public boolean contains(Object value)
  {
    Enumeration<K> ee = keys();
    while (ee.hasMoreElements()) {
      if (rowContains(ee.nextElement(), value))
        return true;
    }
    return false;
  }

  /**
   * Return true if the value is contained in the <code>Table</code>.
   */
  public boolean containsValue(Object value)
  {
    Enumeration<K> ee = keys();
    while (ee.hasMoreElements()) {
      if (rowContains(ee.nextElement(), value))
        return true;
    }
    return false;
  }

  /**
   * Return the number of elements in a row of this <code>Table</code>.
   */
  public int rowSize(K key)
  {
    Object s = tab.get(key);
    if (s == null)
      return 0;
    if (s instanceof HashSet)
      return ((HashSet) s).size();
    return 1;
  }

  /**
   * Return the number of elements in this <code>Table</code>.
   */
  public int size()
  {
    int size = 0;
    Enumeration<K> ee = keys();
    while (ee.hasMoreElements())
      size += rowSize(ee.nextElement());
    return size;
  }

  /**
   * Remove this value from the table.
   * This method is slow and should be avoided.
   * @return null if the value was not in the table or the value otherwise.
   */
  @SuppressWarnings("unchecked")
  public V remove(Object value)
  {
    HashSet<Object>  dead = null;
    boolean          flg  = false;
    Enumeration<V>   ee   = elements();
    while (ee.hasMoreElements()) {
      V set = ee.nextElement();
      if (set == value) {
        if (dead == null) {
          HashSet<Object> deadx = WorkArea.<Object>getSet("remove");
          dead = deadx;
        }
        dead.add(set);
        flg = true;
        continue;
      }
      if ((set instanceof HashSet) && (((HashSet) set).remove(value)))
        flg = true;
    }

    if (dead != null) {
      Iterator<Object> dit = dead.iterator();
      while (dit.hasNext())
        tab.remove(dit.next());
      WorkArea.<Object>returnSet(dead);
    }

    return flg ? (V) value : null;
  }

  /**
   * Remove the set of values from the table.
   * This method is slow and should be avoided.
   */
  public void remove(HashSet<V> values)
  {
    HashSet<Object>   dead = WorkArea.<Object>getSet("remove");
    Iterator<V>       it   = values.iterator();
    while (it.hasNext()) {
      V              nxt = it.next();
      Enumeration<V> ee  = elements();
      while (ee.hasMoreElements()) {
        V set = ee.nextElement();
        if (set == nxt) {
          dead.add(set);
          continue;
        }
        if (set instanceof HashSet)
          ((HashSet) set).remove(nxt);
      }
    }

    Iterator<Object> dit = dead.iterator();
    while (dit.hasNext())
      tab.remove(dit.next());

    WorkArea.<Object>returnSet(dead);
  }
}
