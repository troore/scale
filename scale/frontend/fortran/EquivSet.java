package scale.frontend.fortran;

import scale.common.*;
import scale.clef.decl.VariableDecl;
import scale.clef.decl.EquivalenceDecl;

/** 
 * This class tracks equivalence sets.
 * <p>
 * $Id: EquivSet.java,v 1.4 2007-03-21 13:32:06 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An equivalence set specifies that all variables in the set share
 * the same memory location at some offset in each variable when
 * the variables are mapped to memory.
 */

public final class EquivSet
{
  private EquivSet       prev;    // The previous equivalence set.
  private VariableDecl   base;    // The base variable of the COMMON area if any.
  private VariableDecl[] decls;   // The variables in this equivalence set.
  private long[]         offsets; // The offset into the variable of the equivalence point.
  private int            number;  // The number of variables in this equivalence set.

  /**
   * Create a new equivalence set and link it to the previous sets to
   * form a linked list of sets.
   */
  public EquivSet(EquivSet prev)
  {
    this.prev    = prev;
    this.decls   = new VariableDecl[6];
    this.offsets = new long[6];
    this.number  = 0;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(EQ ");
    if (base != null) {
      buf.append("base:");
      buf.append(base.getName());
    }

    for (int i = 0; i < number; i++) {
      VariableDecl vd = decls[i];
      if (vd == null)
        continue;

      buf.append(" (");
      buf.append(vd.getName());
      buf.append(", ");
      buf.append(offsets[i]);
      buf.append(", ");
      buf.append(vd.getValue() != null);
      buf.append(')');
    }

    buf.append(')');

    return buf.toString();
  }

  /**
   * Return the next set in the linked list.
   */
  public EquivSet getPrevious()
  {
    return prev;
  }

  /**
   * If any of the variables in the equivalence set are in COMMON,
   * return the COMON variable or <code>null</code> if none.
   */
  public VariableDecl getBaseVariable()
  {
    if (base == null) {
      for (int i = 0; i < number; i++) {
        if (decls[i].isEquivalenceDecl()) {
          base = ((EquivalenceDecl) decls[i]).getBaseVariable();
          break;
        }
      }
    }
    return base;
  }

  /**
   * Return the number of variables in the set.
   */
  public int numInSet()
  {
    return number;
  }

  /**
   * Return the specified variable in the set.
   */
  public VariableDecl getDecl(int i)
  {
    return decls[i];
  }

  /**
   * Return the offset of the specified variable in the set.
   */
  public long getOffset(int i)
  {
    return offsets[i];
  }

  /**
   * Return the index of the equivalenced variable to use as the base.
   */
  public int getIndexLargestOffset()
  {
    if (number == 0)
      return -1;

    if (base != null)
      for (int i = 1; i < number; i++)
        if (decls[i].isEquivalenceDecl())
          return i;

    int  lind = 0;
    long max  = offsets[0];
    for (int i = 1; i < number; i++)
      if (offsets[i] > max) {
        lind = i;
        max = offsets[i];
      }

    return lind;
  }

  /**
   * Replace a variable instance with an equivalence variable
   * instance.
   */
  public void update(VariableDecl decl, EquivalenceDecl ev) throws InvalidException
  {
    for (int i = 0; i < number; i++) {
      if (decls[i] == decl) {
        VariableDecl base2 = ev.getBaseVariable();
        if ((base != null) && (base != base2))
          throw new InvalidException("Two different commons.");
        base = base2;
        decls[i] = ev;
        return;
      }
    }
    if (prev != null)
      prev.update(decl, ev);
  }

  /**
   * Add the variable to this equivalence set.  If the variable
   * appears in another equivalence set, attempt to merge that set
   * into this one.  A set cannot be merged if it would result in two
   * locations in a variable mapping to the same place. A set
   * cannot be merged if it contains a variable that is in another
   * COMMON area.
   * @param decl is the variable
   * @param offset is the offset in the variable of the equivalence point
   * @throws InvalidException if two sets can not be merged
   */
  public void addEntry(VariableDecl decl, long offset) throws InvalidException
  {
    int index = lookup(decl);
    if (index >= 0) {
      if (offsets[index] != offset)
        throw new InvalidException("2 locs to 1");
      return;
    }

    if (decl.isEquivalenceDecl()) {
      VariableDecl b = ((EquivalenceDecl) decl).getBaseVariable();
      if ((base != null) && (b != base))
        throw new InvalidException("Different COMMONs.");
      base = b;
    }

    // Add new entry.

    add(decl, offset);

    // See if this decl is in any other set.

    EquivSet last = this;
    EquivSet cur  = prev;
    while (cur != null) {
      int ind = cur.lookup(decl);
      if (ind < 0){
        last = cur;
        cur = cur.prev;
        continue;
      }

      // Must merge the two sets.

      merge(cur, number - 1, ind);

      // Remove the merged set.

      last.prev = cur.prev;
      return;
    }
  }

  private void add(VariableDecl decl, long offset)
  {
    if (number >= decls.length) {
      VariableDecl[] nd = new VariableDecl[number * 2];
      System.arraycopy(decls, 0, nd, 0, number);
      decls = nd;
      long[] no = new long[number * 2];
      System.arraycopy(offsets, 0, no, 0, number);
      offsets = no;
    }

    decls[number] = decl;
    offsets[number] = offset;
    number++;
  }

  private int lookup(VariableDecl decl)
  {
    for (int i = 0; i < number; i++)
      if (decls[i] == decl)
        return i;
    return -1;
  }

  private void merge(EquivSet old, int newIndex, int oldIndex) throws InvalidException
  {
    assert (decls[newIndex] == old.decls[oldIndex]) : "Entries don't match.";

    if ((base != old.base) && (base != null) && (old.base != null))
      throw new InvalidException("Different COMMONs.");
    else if (base == null)
      base = old.base;

    long chgOffset = offsets[newIndex] - old.offsets[oldIndex];

    for (int i = 0; i < old.number; i++) {
      if (i == oldIndex)
        continue;

      VariableDecl decl   = old.decls[i];
      long         offset = old.offsets[i] + chgOffset;
      int          index  = lookup(decl);

      if (index >= 0) {
        if (offsets[index] != offset)
          throw new InvalidException("2 locs to 1");
        continue;
      }

      add(decl, offset);
    }
  }
}
