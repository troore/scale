package scale.common;

/**
 * A class which implements a vector of bits similar to {@link
 * java.util.BitSet java.util.BitSet}.
 * <p>
 * $Id: BitVect.java,v 1.37 2007-10-04 19:58:10 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class differs from java.util.BitSet in that it saves space by not storing words,
 * at the beginning and end of a vector, that are all zero bits.
 * @see java.util.BitSet
 */
public final class BitVect implements Cloneable
{
  private int firstWord; // The index of the first word in the vector.  All prior words contain zeros.
  private int[] vector;  // The actual bits.
  private int numBits;   // Count of the bits set.

  /**
   * Create a bit vector.
   */
  public BitVect()
  {
    firstWord = -1;
    numBits   = 0;
  }

  /**
   * Create a bit vector with pre-allocated size for <code>nbits</code> bits.
   */
  public BitVect(int nbits)
  {
    firstWord = 0;
    numBits   = 0;
    vector = new int[(nbits + 31) / 32];
  }

  /**
   * Create a bit vector with pre-allocated size for a range of bits.
   */
  public BitVect(int firstBit, int lastBit)
  {
    numBits = 0;

    if (firstBit > lastBit) {
      firstWord = -1;
      return;
    }

    firstWord = firstBit / 32;

    int lastWord = lastBit / 32;
    int numWords = lastWord - firstWord + 1;

    vector = new int[numWords];
  }

  public BitVect clone()
  {
    BitVect result = new BitVect();
    if (vector != null) {
      result.vector = new int[vector.length];
      System.arraycopy(vector, 0, result.vector, 0, vector.length);
    }
    result.firstWord = firstWord;
    result.numBits = numBits;

    return result;
  }

  /**
   * Copy the bit vector into the specified <code>int</code> array.
   * The bit vector is represented by 32 bits per <code>int</code>.
   * Bits are numbered right to left within a word.
   * The specified array must be large enough to hold all the bits.
   */
  public void copyTo(int[] bits)
  {
    if (firstWord < 0) {
      for (int i = 0; i < bits.length; i++)
        bits[i] = 0;
      return;
    }

    int k = 0;
    for (int i = 0; i < firstWord; i++)
      bits[k++] = 0;

    for (int i = 0; i < vector.length; i++)
      bits[k++] = vector[i];

    for (; k < bits.length; k++)
      bits[k] = 0;
  }

  private void preExtend(int k)
  {
    int[] n = new int[vector.length + k];
    System.arraycopy(vector, 0, n, k, vector.length);
    vector = n;
    firstWord -= k;
  }

  private void postExtend(int k)
  {
    int[] n = new int[vector.length + k];
    System.arraycopy(vector, 0, n, 0, vector.length);
    vector = n;
  }

  /**
   * Empty the bit vector.
   */
  public void reset()
  {
    numBits = 0;

    if (vector == null) {
      firstWord = -1;
      return;
    }

    for (int i = 0; i < vector.length; i++)
      vector[i] = 0;
  }

  /**
   * If possible, reduce the space required by this bit vector.
   */
  public final void trim()
  {
    if (firstWord < 0)
      return;

    int k = -1;
    int m = vector.length - 1;
    for (int i = m; i >= 0; i--)
      if (vector[i] != 0) {
        k = i;
        break;
      }

    if (k < 0) {
      firstWord = -1;
      vector = null;
      numBits = 0;
      return;
    }

    int l = k;
    for (int i = 0; i < k; i++)
      if (vector[i] != 0) {
        l = i;
        break;
      }

    if ((l == 0) && (k == m))
      return;

    int nl  = k + 1 - l;
    int[] n = new int[nl];
    System.arraycopy(vector, l, n, 0, nl);
    vector = n;
    firstWord += l;
  }

  /**
   * Return the number of intersections.
   */
  public int intersectCount(BitVect o)
  {
    if ((firstWord < 0) || (o.firstWord < 0))
      return 0;

    int d  = firstWord - o.firstWord;
    int no = 0;
    int oo = 0;
    int l  = vector.length;

    if (d != 0) {
      if (d > 0) {
        oo = d;
      } else if (d < 0) {
        no = -d;
      }
    }

    if ((l + oo) > o.vector.length) {
      l = o.vector.length - oo;
    }

    if ((l + no) > vector.length) {
      l = vector.length - no;
    }

    int cnt = 0;
    for (int i = 0; i < l; i++) {
      int intersect = vector[i + no] & o.vector[i + oo];
      if (intersect != 0)
        cnt += count(intersect);
    }

    return cnt;
  }

  /**
   * Return true if the intersection is non-null.
   */
  public boolean intersect(BitVect o)
  {
    if ((firstWord < 0) || (o.firstWord < 0))
      return false;

    int     d    = firstWord - o.firstWord;
    int     no   = 0;
    int     oo   = 0;
    int     l    = vector.length;

    if (d != 0) {
      if (d > 0) {
        oo = d;
      } else if (d < 0) {
        no = -d;
      }
    }

    if ((l + oo) > o.vector.length) {
      l = o.vector.length - oo;
    }

    if ((l + no) > vector.length) {
      l = vector.length - no;
    }

    for (int i = 0; i < l; i++) {
      int intersect = vector[i + no] & o.vector[i + oo];
      if (intersect != 0)
        return true;
    }

    return false;
  }

  /**
   * And this bit vector with the specified bit vector.
   */
  public void and(BitVect o)
  {
    if (firstWord < 0) // All zero already.
      return;

    if (o.firstWord < 0) { // And with all zeros.
      firstWord = -1;
      vector = null;
      numBits = 0;
      return;
    }

    int l = vector.length;
    if (firstWord <= o.firstWord) {
      int n = (firstWord + l) - o.firstWord;
      if (n <= 0) { // No overlap
        firstWord = -1;
        vector = null;
        numBits = 0;
        return;
      }

      int i = 0;
      while (i < l - n)
        vector[i++] = 0;

      if (n > o.vector.length)
        n = o.vector.length;

      int k = 0;
      while ((i < l) && (k < n))
        vector[i++] &= o.vector[k++];

      while (i < l)
        vector[i++] = 0;

      numBits = -1;
      return;
    }

    int ol = o.vector.length;
    int n  = (o.firstWord + ol) - firstWord;
    if (n <= 0) { // No overlap
      firstWord = -1;
      vector = null;
      numBits = 0;
      return;
    }

    int k = ol - n;
    if (n > vector.length)
      n = vector.length;

    int i = 0;
    while ((i < n) && (k < ol))
      vector[i++] &= o.vector[k++];

    while (i < l)
      vector[i++] = 0;

    numBits = -1;
  }

  /**
   * And this bit vector with the logical complement of the specified
   * bit vector.
   */
  public void andNot(BitVect o)
  {
    if (firstWord < 0)
      return;

    if (o.firstWord < 0)
      return;

    int k = o.firstWord + o.vector.length - (firstWord + vector.length);
    if (k > 0)
      postExtend(k);
    int j = firstWord - o.firstWord;
    if (j > 0)
      preExtend(j);

    int l  = o.vector.length;
    int no = o.firstWord - firstWord;

    for (int i = 0; i < l; i++)
      vector[i + no] &= ~o.vector[i];

    numBits = -1;
  }

  /**
   * And the specified array with the logical complement of this bit
   * vector.  This bit vector is not changed.
   */
  public void andNotTo(int[] bits)
  {
    if (firstWord < 0)
      return;

    int l = vector.length;
    for (int i = 0; i < l; i++)
      bits[i + firstWord] &= ~vector[i];
  }

  /**
   * Or this bit vector with the specified bit vector.
   */
  public void or(BitVect o)
  {
    if (firstWord < 0) {
      if (o.vector != null) {
        vector = new int[o.vector.length];
        System.arraycopy(o.vector, 0, vector, 0, o.vector.length);
      }
      firstWord = o.firstWord;
      numBits = o.numBits;

      return;
    }

    if (o.firstWord < 0)
      return;

    int k = o.firstWord + o.vector.length - (firstWord + vector.length);
    if (k > 0)
      postExtend(k);
    int j = firstWord - o.firstWord;
    if (j > 0)
      preExtend(j);

    int l  = o.vector.length;
    int no = o.firstWord - firstWord;

    for (int i = 0; i < l; i++)
      vector[i + no] |= o.vector[i];

    numBits = -1;
  }

  /**
   * Or this bit vector with the specified bit vector.
   * @return true if this bit vector was modified.
   */
  public boolean orAndTest(BitVect o)
  {
    boolean changed = false;
    if (firstWord < 0) {
      if (o.vector != null) {
        vector = new int[o.vector.length];
        System.arraycopy(o.vector, 0, vector, 0, o.vector.length);
        for (int i = 0; i < o.vector.length; i++)
          if (o.vector[i] != 0) {
            changed = true;
            break;
          }
      }
      firstWord = o.firstWord;
      numBits = o.numBits;

      return changed;
    }

    if (o.firstWord < 0)
      return changed;

    int k = o.firstWord + o.vector.length - (firstWord + vector.length);
    if (k > 0)
      postExtend(k);
    int j = firstWord - o.firstWord;
    if (j > 0)
      preExtend(j);

    int l  = o.vector.length;
    int no = o.firstWord - firstWord;

    for (int i = 0; i < l; i++) {
      int vv = vector[i + no];
      int ov = o.vector[i];
      int dv = ov | vv;

      changed |= (vv != dv);
      vector[i + no] = dv;
    }

    if (changed)
      numBits = -1;

    return changed;
  }

  /**
   * Or this bit vector with the specified array.
   * This bit vector is not changed.
   * @return true if the resulting array is not equivalent to this bit
   * vector
   */
  public boolean orTo(int[] bits)
  {
    if (firstWord < 0) {
      for (int i = 0; i < bits.length; i++)
        if (bits[i] != 0)
          return true;
      return false;
    }

    boolean notEqiv = false;
    for (int i = 0; i < firstWord; i++)
      if (bits[i] != 0) {
        notEqiv = true;
        break;
      }

    int l = vector.length;
    int k = firstWord;
    for (int i = 0; i < l; i++) {
      int x = bits[k] | vector[i];
      bits[k++] = x;
      notEqiv |= (vector[i] != x);
    }

    if (!notEqiv)
      for (; k < bits.length; k++)
        if (bits[k] != 0) {
          notEqiv = true;
          break;
        }

    return notEqiv;
  }

  /**
   * Set this bit vector with the specified array of bits.
   * The bit vector is represented by 32 bits per <code>int</code>.
   * Bits are numbered right to left within a word.
   */
  public void define(int[] o)
  {
    int of = -1;
    for  (int i = 0; i < o.length; i++)
      if (o[i] != 0) {
        of = i;
        break;
      }

    firstWord = of;
    if (of < 0) {
      numBits = 0;
      vector = null;
      return;
    }

    numBits = -1;

    int ol = of;
    for (int i = of + 1; i < o.length; i++)
      if (o[i] != 0)
        ol = i;

    int os = ol - of + 1;
    if ((vector == null) || (vector.length < os)) {
      vector = new int[os];
      System.arraycopy(o, of, vector, 0, os);
      return;
    }

    for (int i = 0; i < os; i++)
      vector[i] = o[i + of];
    for (int i = os; i < vector.length; i++)
      vector[i] = 0;
  }

  /**
   * Exclusive or this bit vector with the specified bit vector.
   */
  public void xor(BitVect o)
  {
    if (firstWord < 0) {
      if (o.vector != null) {
        vector = new int[o.vector.length];
        System.arraycopy(o.vector, 0, vector, 0, o.vector.length);
      }
      numBits = o.numBits;
      firstWord = o.firstWord;
      return;
    } else if (o.firstWord < 0)
      return;

    int     d    = firstWord - o.firstWord;
    int     no   = 0;
    int     oo   = 0;
    int     l    = vector.length;

    if (d != 0) {
      if (d > 0) {
        for (int i = 0; i < d; i++) {
          if (o.vector[i] != 0) {
            int k = d - i;
            preExtend(k);
            d -= k;
            break;
          }
        }
        oo = d;
        l = vector.length - d;
      } else if (d < 0) {
        no = -d;
        l = vector.length + d;
      }
    }

    int e = (o.firstWord + o.vector.length) - (firstWord + vector.length);
    if (e > 0) {
      for (int i = e; i >= 1; i--) {
        if (o.vector[o.vector.length - e + i - 1] != 0) {
          postExtend(i);
          break;
        }
      }
    }

    if ((l + no) > vector.length) {
      l = vector.length - no;
    }

    if ((l + oo) > o.vector.length) {
      l = o.vector.length - oo;
    }

    for (int i = 0; i < l; i++) {
      vector[i + no] ^= o.vector[i + oo];
    }
    numBits = -1;
  }

  /**
   * Return true if the two bit vectors are identical.
   */
  public boolean equivalent(BitVect o)
  {
    if (o == null)
      return false;
        
    if (firstWord < 0) {
      if (o.firstWord >= 0)
        for (int i = 0; i < o.vector.length; i++) {
          if (o.vector[i] != 0)
            return false;
        }
      return true;
    }

    if (o.firstWord < 0) {
      for (int i = 0; i < vector.length; i++) {
        if (vector[i] != 0)
          return false;
      }
      return true;
    }

    int     d    = firstWord - o.firstWord;
    int     no   = 0;
    int     oo   = 0;
    int     l    = vector.length;

    if (d != 0) {
      if (d > 0) {
        int ll = (d >= o.vector.length) ? o.vector.length : d;
        for (int i = 0; i < ll; i++) {
          if (o.vector[i] != 0)
            return false;
        }
        oo = d;
      } else if (d < 0) {
        no = -d;
        int ll = (no >= vector.length) ? vector.length : no;
        for (int i = 0; i < ll; i++) {
          if (vector[i] != 0)
            return false;
        }
      }
    }

    if ((l + no) > vector.length)
      l = vector.length - no;

    if ((l + oo) > o.vector.length)
      l = o.vector.length - oo;

    for (int i = 0; i < l; i++)
      if (vector[i + no] != o.vector[i + oo])
        return false;

    int f = l + no;
    if (f < 0)
      f = 0;
    for (int i = f; i < vector.length; i++)
      if (vector[i] != 0)
        return false;

    int fo = l + oo;
    if (fo < 0)
      fo = 0;
    for (int i = fo; i < o.vector.length; i++)
      if (o.vector[i] != 0)
        return false;

    return true;
  }

  /**
   * Set the bit at the specified index.
   */
  public void set(int index)
  {
    int wi = index / 32;
    int bi = index - (wi * 32);

    if (firstWord < 0) {
      vector = new int[1];
      firstWord = wi;
    }

    int f  = wi - firstWord;

    if (f < 0) {
      preExtend(-f);
      f = 0;
    }

    if (f >= vector.length) {
      postExtend(f - vector.length + 1);
    }

    int mask = 1 << bi;
    if ((numBits != -1) && ((vector[f] & mask) == 0))
      numBits++;
    vector[f] |= mask;
  }

  /**
   * Clear the bit at the specified index.
   */
  public void clear(int index)
  {
    if (firstWord < 0)
      return;

    int wi = index / 32;
    int bi = index - (wi * 32);
    int f  = wi - firstWord;

    if (f < 0)
      return;

    if (f >= vector.length)
      return;
    int mask = 1 << bi;
    if ((numBits != -1) && ((vector[f] & mask) != 0))
      numBits--;
    vector[f] &= ~mask;
  }

  /**
   * Return the bit at the specified index.
   */
  public boolean get(int index)
  {
    if (firstWord < 0)
      return false;

    int wi = index / 32;
    int bi = index - (wi * 32);
    int f  = wi - firstWord;

    if (f < 0)
      return false;

    if (f >= vector.length)
      return false;

    return (0 != (vector[f] & (1 << bi)));
  }

  /**
   * Return the index plus one of the last bit set.
   */
  public int length()
  {
    if (firstWord < 0)
      return 0;

    for (int j = vector.length - 1; j >= 0; j--) {
      int last = vector[j];
      for (int i = 0; i < 32; i++) {
        if ((last & (1 << i)) != 0)
          return (firstWord + j) * 32 + i;
      }
    }
    return 0;
  }

  //                                 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, f
  private static final byte[] cnt = {0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};

  /**
   * Return the number of bits set.
   */
  private int count(int v)
  {
    if (v == 0)
      return 0;

    int sum = cnt[v & 0xf];
    sum += cnt[(v >> 4) & 0xf];
    sum += cnt[(v >> 8) & 0xf];
    sum += cnt[(v >> 12) & 0xf];
    sum += cnt[(v >> 16) & 0xf];
    sum += cnt[(v >> 20) & 0xf];
    sum += cnt[(v >> 24) & 0xf];
    sum += cnt[(v >> 28) & 0xf];
    return sum;
  }

  /**
   * Return the number of bits set.
   */
  public int count()
  {
    if (firstWord < 0)
      return 0;

    if (numBits >= 0)
      return numBits;

    int sum = 0;
    for (int j = 0; j < vector.length; j++) {
      int last = vector[j];
      if (last == 0)
        continue;
      sum += count(last);
    }

    numBits = sum;

    return sum;
  }

  /**
   * Return true if the bit vector is all zeros.
   */
  public boolean empty()
  {
    if (firstWord < 0)
      return true;
    for (int j = 0; j < vector.length; j++)
      if (vector[j] != 0)
        return false;
    return true;
  }

  /**
   * Return the number of bits actually in use to represent this bit vector.
   */
  public int size()
  {
    if (firstWord < 0)
      return 0;
    return vector.length * 32;
  }

  // 0,   1,   2,   3,   4,   5,   6,   7,   8,   9,   a,   b,   c,  d,   e,    f
  private static final char[] hex = {
    '0', '8', '4', 'c', '2', 'a', '6', 'e', '1', '9', '5', 'd', '3', 'b', '7', 'f'};

  /**
   * Return a String representation of the bit vector.
   * @return a String representation of the bit vector
   */
  public String toString()
  {
    int lv = 0;

    if (vector != null)
      lv = vector.length;

    int l = lv + firstWord;

    if (l < 3)
      l = 3;

    StringBuffer buf = new StringBuffer("(");
    int pos = 1;
    for (int i = 0; i < l; i++) {
      int bits = 0;
      if ((i >= firstWord) && ((i - firstWord) < lv))
        bits = vector[i - firstWord];

      for (int j = 0; j < 32; j += 4)
        buf.append(hex[(bits >> j) & 0xf]);
    }

    buf.append(')');
    return buf.toString();
  }

  /**
   * Increase the histogram by one in every position there is a bit in
   * the bit vector.
   */
  public void histogram(int[] hist)
  {
    if (firstWord < 0)
      return;

    for (int j = 0; j < vector.length; j++) {
      int last = vector[j];

      if (last == 0)
        continue;

      int ind = (firstWord + j) * 32;
      for (int bi = 0; bi < 32; bi++) {
        if (((last >> bi) & 1) != 0)
          hist[ind + bi]++;
      }
    }
  }

  /**
   * Transpose a matrix of bits.
   * This algorithm is quick if the input array is sparse.
   * @param out specifies the output array
   * @param in is the array of BitVect instances
   */
  public static void transpose(BitVect[] out, BitVect[] in)
  {
    for (int i = 0; i < in.length; i++) {
      BitVect bv = in[i];
      if (bv == null)
        continue;
      int[] v = bv.vector;
      if (v == null)
        continue;
      int fw = bv.firstWord * 32;
      for (int j = 0; j < v.length; j++, fw+= 32) {
        int vv = v[j];
        if (vv == 0)
          continue;
        int ri = fw;
        for (int k = 0; k < 32; k += 8, ri += 8) {
          int chunk = (vv >> k) & 0xff;
          if (chunk == 0)
            continue;
          for (int l = 0; l < 8; l++) {
            if (((chunk >> l) & 1) == 0)
              continue;
            int reg = ri + l;
            if (out[reg] == null)
              out[reg] = new BitVect(i, i);
            out[reg].set(i);
          }
        }
      }
    }
    for (int i = 0; i < out.length; i++) {
      if (out[i] != null)
        continue;
      out[i] = new BitVect(1, 0);
    }
  }

  /**
   * Return a column of a bit array that is represented by an array of
   * BitVect instances.
   * @param slice specifies the column
   * @param in is the array of BitVect instances
   */
  public static BitVect getSlice(int slice, BitVect[] in)
  {
    int m   = in.length;
    int wis = slice / 32;
    int bis = slice - (wis * 32);
    int msk = (1 << bis);

    // Find the first bit.

    int firstBit = -1;
    for (int j = 0; j < m; j++) {
      BitVect bv  = in[j];
      int     fws = bv.firstWord;

      if (fws < 0)
        continue;

      int fs = wis - fws;

      if (fs < 0)
        continue;

      int[] v = bv.vector;
      if (fs >= v.length)
        continue;

      if (0 == (v[fs] & msk))
        continue;

      firstBit = j;
      break;
    }

    if (firstBit < 0) // No one bits.
      return new BitVect(1, 0);

    // Find the last bit.

    int lastBit = -1;
    for (int j = m - 1; j >= firstBit; j--) {
      BitVect bv  = in[j];
      int     fws = bv.firstWord;

      if (fws < 0)
        continue;

      int fs = wis - fws;

      if (fs < 0)
        continue;

      int[] v = bv.vector;
      if (fs >= v.length)
        continue;

      if (0 == (v[fs] & msk))
        continue;

      lastBit = j;
      break;
    }

    // Get the slice.

    BitVect bs  = new BitVect(firstBit, lastBit);
    int     wrd = 0;
    int     l   = 0;
    int     bit = firstBit % 32;
    for (int j = firstBit; j <= lastBit; j++, bit++) {
      if (bit >= 32) {
        bs.vector[l] = wrd;
        wrd = 0;
        l++;
        bit = 0;
      }

      BitVect bv  = in[j];
      int     fws = bv.firstWord;

      if (fws < 0)
        continue;

      int fs = wis - fws;

      if (fs < 0)
        continue;

      int[] v = bv.vector;
      if (fs >= v.length)
        continue;

      if (0 == (v[fs] & msk))
        continue;

      wrd |= (1 << bit);
      bs.numBits++;
    }

    if (wrd != 0)
      bs.vector[l] = wrd;

    return bs;
  }
 
  /**
   * Return an array of integers specifying which bits are set.
   */
  public int[] getSetBits()
  {
       
    if (firstWord < 0)
      return (new int[0]);
        
    int   numBits  = count();
    int[] result   = new int[numBits];
    int   index    = 0;
        
    for (int j = 0; j < vector.length; j++) {
      int last = vector[j];

      if (last == 0)
        continue;

      int ind = (firstWord + j) * 32;
      for (int bi = 0; bi < 32; bi++) {
        if (((last >> bi) & 1) != 0)
          result[index++] = ind + bi;
      }
    }

    return result;
  }

  /**
   * Set an array of integers specifying which bits are set.  It is
   * assumed that the array is large enough; use the {@link #count
   * count()} method to determine the proper size.
   * @return the number set
   */
  public int getSetBits(int[] result)
  {
       
    if (firstWord < 0)
      return 0;
        
    int numBits = count();
    int index   = 0;
        
    for (int j = 0; j < vector.length; j++) {
      int last = vector[j];

      if (last == 0)
        continue;

      int ind = (firstWord + j) * 32;
      for (int bi = 0; bi < 32; bi++) {
        if (((last >> bi) & 1) != 0)
          result[index++] = ind + bi;
      }
    }

    return index;
  }

  /**
   * Display the bits set by their index values.
   * The display goes to <code>System.out</code>.
   */
  public void outputSetBits()
  {
    System.out.print("[");
    int[] setBits = getSetBits();
    for (int i = 0; i < numBits; i++) {
      if (i > 0) 
        System.out.print(",");
      System.out.print(setBits[i]);
    }
    System.out.print("]");
  }
}
