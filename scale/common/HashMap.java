package scale.common;

import java.util.Enumeration;
import java.util.Map;

/**
 * A Scale cover class for a {@link java.util.Hashtable java.util.Hashtable}.
 * <p>
 * $Id: HashMap.java,v 1.29 2006-01-25 20:07:14 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */
public class HashMap<K,V> extends java.util.Hashtable<K,V> implements Cloneable 
{
  private static final long serialVersionUID = 42L;

  public HashMap()
  {
    super();
  }

  public HashMap(int capacity)
  {
    super(capacity, 0.75f);
  }

  public HashMap(Map<K, V> map)
  {
    super(map);
  }

  public synchronized String toString()
  {
    StringBuffer tmp = new StringBuffer("{");

    if (!isEmpty()) {
      Enumeration<K> e1 = keys();
      while (e1.hasMoreElements()) {
        K o = e1.nextElement();
        tmp.append('(');
        tmp.append(o);
        tmp.append(", ");
        tmp.append(get(o));
        tmp.append(')');
      }
    }
    tmp.append("}");
    return tmp.toString();
  }

  public Enumeration<K> uniqueKeys()
  {
    return keys();
  }

  /**
   * Merge this hash map with the argument hash map;
   * @param hm HashMap to merge with.
   */
  public void merge(HashMap<K, V> hm)
  {
    if (hm == null)
      return;

    Enumeration<K> e = hm.keys();
    while (e.hasMoreElements()) {
      K key = e.nextElement();
      put(key, hm.get(key));
    }
  }

  public final HashMap<K,V> clone()
  {
    return new HashMap<K,V>(this);
  }
}
