package scale.common;

import scale.common.Stack;

/**
 * This class helps to eliminate JVM Heap allocations.
 * <p>
 * $Id: WorkArea.java,v 1.7 2007-08-27 18:37:51 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class provides {@link scale.common.Stack Stack} and {@link
 * scale.common.HashSet HashSet} instances for re-use.  Stacks &
 * HashSets are often used for spanning algorithms.  By re-using
 * exisiting instances many allocations of large structures can be
 * avoided.
 */
public class WorkArea
{
  private static int maxStackCount = 0;
  private static int maxSetCount = 0;

  private static final String[] stats = {"maxStacks", "maxSets", "inUseStacks", "inUseSets"};

  static
  {
    Statistics.register("scale.common.WorkArea", stats);
  }

  /**
   * Return the maximum number of stacks allocated.
   */
  public static int maxStacks()
  {
    return maxStackCount;
  }

  /**
   * Return the number of in use stacks.
   */
  public static int inUseStacks()
  {
    int cnt = 0;
    int l   = stacks.length;
    for (int i = 0; i < l; i++) {
      if (inUseStack[i]) {
        System.out.println("** Stack in use - " + nameStack[i]);
        cnt++;
      }
    }
    return cnt;
  }

  /**
   * Return the maximum number of sets allocated.
   */
  public static int maxSets()
  {
    return maxSetCount;
  }

  /**
   * Return the number of in use sets.
   */
  public static int inUseSets()
  {
    int cnt = 0;
    int l   = sets.length;
    for (int i = 0; i < l; i++) {
      if (inUseSet[i]) {
        System.out.println("** Set in use - " + nameSet[i]);
        cnt++;
      }
    }
    return cnt;
  }

  private static Stack[]   stacks     = new Stack[10];
  private static HashSet[] sets       = new HashSet[10];
  private static boolean[] inUseStack = new boolean[10];
  private static boolean[] inUseSet   = new boolean[10];
  private static String[]  nameStack  = new String[10];
  private static String[]  nameSet    = new String[10];

  /**
   * Obtain a {@link scale.common.Stack Stack} for temporary use.  The
   * {@link scale.common.Stack Stack} obtained must not be attached to
   * anything whose lifetime is longer than the invocation time of the
   * method calling the getStack method.  It must be returned for
   * further use by using {@link #returnStack returnStack}.
   * @param name is used to detect who didn't return a {@link scale.common.Stack Stack} for re-use
   */
  @SuppressWarnings("unchecked")
  public static <T> Stack<T> getStack(String name)
  {
    int l = stacks.length;
    for (int i = 0; i < l; i++) {
      if (!inUseStack[i]) {
        Stack stk = stacks[i];
        if (stk == null) {
          stk = new Stack<Object>();
          stacks[i] = stk;
          maxStackCount++;
        }
        inUseStack[i] = true;
        nameStack[i] = name;
        return (Stack<T>) stk;
      }
    }

    maxStackCount++;

    Stack[] ns = new Stack[l + 5];
    System.arraycopy(stacks, 0, ns, 0, l);
    stacks = ns;

    boolean[] nb = new boolean[l + 5];
    System.arraycopy(inUseStack, 0, nb, 0, l);
    inUseStack = nb;

    String[] nn = new String[l + 5];
    System.arraycopy(nameStack, 0, nn, 0, l);
    nameStack = nn;

    inUseStack[l] = true;
    nameStack[l] = name;

    Stack<Object> nstk = new Stack<Object>();
    stacks[l] = nstk;
    return (Stack<T>) nstk;
  }

  /**
   * Release the {@link scale.common.Stack Stack} so that it can be used again.
   */
  public static <T> void returnStack(Stack<T> stk)
  {
    stk.clear();
    int l = stacks.length;
    for (int i = 0; i < l; i++) {
      if (stk == stacks[i]) {
        inUseStack[i] = false;
        nameStack[i] = null;
        return;
      }
    }
    throw new scale.common.InternalError("Invalid returned stack");
  }

  /**
   * Obtain a {@link scale.common.HashSet HashSet} instance for
   * temporary use.  The {@link scale.common.HashSet HashSet} instance
   * obtained must not be attached to anything whose lifetime is
   * longer than the invocation time of the method calling the getSet
   * method.  It must be returned for further use by using {@link
   * #returnSet returnSet}.
   * @param name is used to detect who didn't return a {@link
   * scale.common.HashSet HashSet} instance for re-use
   */
  @SuppressWarnings("unchecked")
  public static <T> HashSet<T> getSet(String name)
  {
    int l = sets.length;
    for (int i = 0; i < l; i++) {
      if (!inUseSet[i]) {
        HashSet set = sets[i];
        if (set == null) {
          set = new HashSet<Object>(203);
          sets[i] = set;
          maxSetCount++;
        }
        inUseSet[i] = true;
        nameSet[i] = name;
        return (HashSet<T>) set;
      }
    }

    maxSetCount++;

    HashSet[] ns = new HashSet[l + 5];
    System.arraycopy(sets, 0, ns, 0, l);
    sets = ns;

    boolean[] nb = new boolean[l + 5];
    System.arraycopy(inUseSet, 0, nb, 0, l);
    inUseSet = nb;

    String[] nn = new String[l + 5];
    System.arraycopy(nameSet, 0, nn, 0, l);
    nameSet = nn;

    inUseSet[l] = true;
    nameSet[l] = name;
    HashSet<Object> nset = new HashSet<Object>(203);
    sets[l] = nset;
    return (HashSet<T>) nset;
  }

  /**
   * Release the {@link scale.common.HashSet HashSet} instance so that
   * it can be used again.
   */
  public static <T> void returnSet(HashSet<T> set)
  {
    set.clear();
    int l = sets.length;
    for (int i = 0; i < l; i++) {
      if (set == sets[i]) {
        String name = nameSet[i];
        inUseSet[i] = false;
        nameSet[i] = null;
        return;
      }
    }
    throw new scale.common.InternalError("Invalid returned set");
  }

  /**
   * Clean up for profiling statistics.
   */
  public static void cleanup()
  {
    stacks     = null;
    sets       = null;
    inUseStack = null;
    inUseSet   = null;
    nameStack  = null;
    nameSet    = null;
  }
}
