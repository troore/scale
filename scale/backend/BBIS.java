package scale.backend;

import scale.common.*;
import scale.clef.decl.Declaration;


/** 
 * This class provides basic block instruction scheduling.
 * <p>
 * $Id: BBIS.java,v 1.15 2007-10-04 19:57:48 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The basic organization for this instruction scheduler was inspired
 * by Chapter 12 of <cite>"Engineering a Compiler"</cite> by Keith
 * D. Cooper and Linda Torczon, Morgan Kaufman Publishers, ISBN:
 * 1-55860-698-X.
 * <p>
 * The basic list scheduling algorithm is used.  Both forward and
 * backward list scheduling are tried for each basic block to obtain
 * the best schedule.  The scheduler picks the priority scheme used to
 * select the next instruction based on metrics of the basic block.
 * <p>
 * Anit-dependencies are handled without renaming.  When the scheduler
 * is run prior to rgister allocation very little renaming is
 * required.  When run after register allocation, renaming cannot be
 * used.
 * <p>
 * The schuler requires two pieces on information from each
 * instruction in addition to the registers used or def-ed by the
 * instruction.
 * <ol>
 * <li> The length of time the instruction takes to execute in cycles.
 * <li> The type of instruction - that is the functional unit that will 
 *      execute it.
 * </ol>
 * The scheduler will ask the code generator for the number of
 * functional units that are available for a given type.
 * <p>
 * To add a different instruction scheduler, derive it from this class
 * and implement the {@link #schedule schedule()} method.  Then
 * modify the {@link Generator code generator} to use the new
 * instruction scheduler.
 */
public class BBIS
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * True if all functions should be scheduled - false if just traced
   * functions should be scheduled.
   */
  public static boolean all = true;
  /**
   * Minimum block size to be scheduled.
   */
  public static int min = 0;
  /**
   * Maximum block size to be scheduled.
   */
  public static int max = 0x7ffff;
  /**
   * This instruction can not be issued until its predecessor has completed.
   */
  private static final int EXE_DEPENDENCE = 0;
  /**
   * This instruction can not be issued until its predecessor has issued.
   */
  private static final int ISSUE_DEPENDENCE = 1;
  private static final int DEPENDENCE_MASK = 1;
  private static final int DEPENDENCE_SHIFT = 1;

  protected Generator   gen;
  protected RegisterSet registers;

  private Instruction[] linear; // The linear list of basic block instructions.

  private int[] dgraph; // The dependence graph: the instructions that instruction i depends on.
  private int[] sgraph; // The successor graph: the instructions that require the instruction i to execute first.
  private int[] time;   // The cycle at which the instruction starts.
  private int[] delay;  // The number of cycles the instruction takes.
  private int[] ready;  // The ready list - one bit per instruction.
  private int[] active; // The active list - one bit per instruction.
  private int[] done;   // The finished list - one bit per instruction.
  private int[] schedn; // The no-schedule schedule.
  private int[] schedf; // The forward schedule.
  private int[] schedb; // The backward schedule.

  private byte[] fusa; // The number of instructions allowed per functional unit.
  private byte[] fusi; // The number of instructions issued per functional unit.

  private int     maxSize = 0;
  private boolean trace   = false;

  // The following array is used to represent multiple lists.  Each
  // list entry is represented by two integers.  The first is the
  // value of the list element and the second is the index of the next
  // entry on the list.  A value of 0 indicates the end of the
  // list. Consequently, the array elements at index 0 and 1 are not
  // used.

  private int[] bag;
  private int   bptr;

  public BBIS(Generator gen)
  {
    this.gen       = gen;
    this.linear    = new Instruction[256];
    this.schedn    = new int[256];
    this.schedf    = new int[256];
    this.schedb    = new int[256];
    this.maxSize   = 0;
    this.fusa      = gen.machine.getFunctionalUnitDescriptions();
    this.fusi      = new byte[fusa.length];
  }

  /**
   * Schedule all the instructions in a function.
   * @param insts is the list of instructions
   * @return the list of instructions in scheduled order
   */
  public Instruction schedule(Instruction insts, boolean trace)
  {
    if (insts == null)
      return null;

    this.trace = (trace && classTrace) || Debug.debug(3);
    if (!all && !this.trace)
      return insts;

    if ((Debug.getReportName() != null) &&
        !Debug.getReportName().equals(gen.getCurrentRoutine().getName()))
      return insts;

    if (this.trace) {
      System.out.print("** ");
      System.out.print(gen.getCurrentRoutine().getName());
      System.out.print(" ");
      System.out.println(Debug.getReportName());
    }

    this.registers = gen.getRegisterSet();

    Stack<Instruction> is = WorkArea.<Instruction>getStack("schedule");

    // Schedule each basic block separately.  A basic block consists
    // of those instructions between two labels.

    try {
      Instruction first = insts;
      Instruction last  = null;
      Instruction cur   = insts;
      int         ic    = 0;

      while (cur != null) {
        Instruction nxt = cur.getNext();

        if (!cur.isLabel() && !cur.isBranch() && (nxt != null) && (ic < 200)) {
          last = cur;
          cur = nxt;
          ic++;
          continue;
        }

        if (last != null) {

          // Place the instructions of the basic block in a linear
          // array.

          ic = linearize(first, last);

          buildDependenceGraphs(ic);

          int[] schedule = schedn;
          int   ncycles  = noSchedule(ic, schedn);
          int   fcycles  = ncycles;
          int   bcycles  = ncycles;
          int   scycles  = ncycles;

          if ((ic >= min) && (ic <= max)) {
            fcycles = forwardSchedule(ic, schedf);
            bcycles = backwardSchedule(ic, schedb);
          }

          // Pick the best schedule.

          boolean same = true;
          if (fcycles < scycles) {
            schedule = schedf;
            scycles = fcycles;
            same = false;
          }

          if (bcycles < scycles) {
            schedule = schedb;
            scycles = bcycles;
            same = false;
          }

          if (this.trace && !same) {
            is.push(new CommentMarker("** not scheduled " + ic + " " + ncycles));
            StringBuffer buf = new StringBuffer("  ");
            for (int i = 0; i < ic; i++) {
              genInstInfo(buf, i);
              is.push(new CommentMarker(buf.toString()));
              buf.setLength(2);
            }
            is.push(new CommentMarker("**     scheduled " + ic + " " + scycles));
            for (int i = 0; i < ic; i++) {
              genInstInfo(buf, schedule[i]);
              is.push(new CommentMarker(buf.toString()));
              buf.setLength(2);
            }
          }

          // Place instructions in scheduled order.

          for (int i = 0; i < ic; i++) {
            int ind = schedule[i];
            is.push(linear[ind]);
          }

          ic = 0;
        }

        do {
          is.push(cur);
          cur = cur.getNext();
        } while ((cur != null) && (cur.isBranch() || cur.isMarker()));

        first = cur;
        last = cur;
      }

      // Add the scheduled instructions to the list of instructions.

      Instruction sinsts = is.get(0);
      Instruction prev   = sinsts;
      int         l      = is.size();
      for (int i = 1; i < l; i++) {
        Instruction inst = is.get(i);
        prev.setNext(inst);
        prev = inst;
      }
      prev.setNext(null);

      WorkArea.<Instruction>returnStack(is);

      for (int i = 0; i < maxSize; i++)
        linear[i] = null;

      return sinsts;

    } catch (java.lang.OutOfMemoryError ex) {
      Msg.reportInfo(Msg.MSG_Unable_to_instruction_schedule_s,
                     gen.getCurrentRoutine().getName());
      for (int i = 0; i < linear.length; i++)
        linear[i] = null;
      WorkArea.<Instruction>returnStack(is);
      return insts;
    }
  }

  private void genInstInfo(StringBuffer buf, int ind)
  {
    int         x    = buf.length();
    Instruction inst = linear[ind];
    buf.append('(');
    buf.append(ind);
    buf.append(") ");
    while (buf.length() < (x + 6))
      buf.append(' ');
    buf.append(inst.toString().replace('\t',' '));
    while (buf.length() < (x + 35))
      buf.append(' ');
    buf.append(' ');
    buf.append(inst.isSpillStorePoint() ? 'S' : ' ');
    buf.append(inst.isSpillLoadPoint() ? 'L' : ' ');
    buf.append(' ');
    buf.append(delay[ind]);
    buf.append("c ");
    buf.append(linear[ind].getFunctionalUnit());
    buf.append('u');
    buf.append(' ');
    buf.append(successors(ind));
  }

  private String successors(int insti)
  {
    int sg = sgraph[insti];
    if (sg <= 0)
      return "";

    StringBuffer buf = new StringBuffer("s(");
    int          k   = 0;
    while (sg != 0) {
      if (k > 0)
        buf.append(' ');

      int x = bag[sg];
      sg = bag[sg + 1];

      buf.append(x >> DEPENDENCE_SHIFT);
      buf.append(((x & DEPENDENCE_MASK) == ISSUE_DEPENDENCE) ? 'i' : 'e');

      k++;
    }

    buf.append(')');
    return buf.toString();
  }

 private String predecessors(int insti)
  {
    int sg = dgraph[insti];
    if (sg <= 0)
      return "";

    StringBuffer buf = new StringBuffer("p(");
    int          k   = 0;
    while (sg != 0) {
      if (k > 0)
        buf.append(' ');

      int x = bag[sg];
      sg = bag[sg + 1];

      buf.append(x >> DEPENDENCE_SHIFT);
      buf.append(((x & DEPENDENCE_MASK) == ISSUE_DEPENDENCE) ? 'i' : 'e');

      k++;
    }

    buf.append(')');
    return buf.toString();
  }

  /**
   * Use forward list scheduling.
   * @param ic is the number of instructions in the basic block
   * @param sched is the scheduled list of instructions
   * @return the number of cycles required for the schedule
   */
  private int forwardSchedule(int ic, int[] sched)
  {
    int nws = (ic + 31) / 32;
    for (int i = 0; i < nws; i++) {
      ready[i]  = 0;
      active[i] = 0;
      done[i]   = 0;
    }

    int activeCount = 0;
    int readyCount  = 0;

    for (int i = 0; i < ic; i++) {
      time[i] = 0;
      if (dgraph[i] == 0) { // No initial dependencies.
        int rli = i / 32;
        int rb  = i - (rli * 32);
        assert checkOff("  rdy", ready, i) : "Already ready " + i;
        ready[rli] |= 1 << rb;
        readyCount++;
      }
    }

    int sptr  = 0;
    int cycle = 1;
    while ((readyCount > 0) || (activeCount > 0)) {
      if (readyCount > 0) {

        // Find first ready list instruction.

        int rli; // ready list word index.
        for (rli = 0; rli < nws; rli++) {
          if (ready[rli] != 0)
            break;
        }

        if (rli < nws) {
          int rw = ready[rli];
          int rb = 0; // ready word bit offset.
          if ((rw & 0xffff) == 0)
            rb += 16;
          if (((rw >> rb) & 0xff) == 0)
            rb += 8;
          if (((rw >> rb) & 0x0f) == 0)
            rb += 4;
          if (((rw >> rb) & 0x03) == 0)
            rb += 2;
          if (((rw >> rb) & 0x01) == 0)
            rb += 1;

          // Remove from ready list and schedule it.

          do {
            int bit = 1 << rb; // This is right-to-left.
            int ind = (rli * 32) + rb;

            if ((rw & bit) != 0) { // It's ready.
              int fu = linear[ind].getFunctionalUnit();
              if (fusi[fu] < fusa[fu]) { // Functional unit is ready.
                if ((ind - sptr) < 8) { // Distance not too great.
                  fusi[fu]++;
                  // Execute ready instruction.
                  // Remove instruction from the ready list.

                  rw &= ~bit;
                  ready[rli] = rw;
                  readyCount--;

                  // Add instruction to the active list.

                  assert checkOff("  exe", active, ind) : "Already ready " + ind;

                  int x = active[rli];
                  active[rli] |= bit;
                  if (x != active[rli])
                    activeCount++;

                  // Add instruction to the schedule.

                  sched[sptr++] = ind;
                  time[ind] = cycle;
                }
              }
            }

            if (readyCount <= 0)
              break;

            rb++;
            if (rb >= 32) {
              do {
                rli++;
                if (rli >= nws)
                  break;
                rw  = ready[rli];
              } while (rw == 0);
              rb = 0;
            }
          } while (rli < nws);
        }
      }

      cycle++;

      if (activeCount <= 0)
        continue;

      // For each instruction on the active list that is complete
      // put all its successors on the ready list.

      for (int ali = 0; ali < nws; ali++) { // 32 at a time
        int wrd = active[ali];
        if (wrd == 0)
          continue;

        for (int j = 0; j < 4; j++) { // 8 at a time
          int b = (wrd >> (j * 8)) & 0xff;
          if (b == 0)
            continue;

          for (int k = 0; k < 8; k++) { // one at a time
            if ((b & (1 << k)) == 0)
              continue;

            int     ab  = (j * 8) + k;
            int     ind = (ali * 32) + ab;
            boolean exe = ((time[ind] + delay[ind]) < cycle);

            if (!exe)
              continue;

            // Instruction is completed, remove from active list.

            int bit = 1 << ab;
            assert checkOn("  fin", active, ind) : "Not active " + ind;

            wrd = wrd & ~bit;
            active[ali] = wrd;
            activeCount--;
            int fu = linear[ind].getFunctionalUnit();
            fusi[fu]--;

            // Mark the instruction as done.

            done[ali] |= bit;

            // Check each successor to see if it is now ready.

            int nxt = sgraph[ind];
            while (nxt > 0) {
              int x = bag[nxt];
              nxt = bag[nxt + 1];

              int suc = x >> DEPENDENCE_SHIFT;
              int rli = suc / 32;
              int rb  = suc - (rli * 32);
              int bt  = 1 << rb;

              if (((ready[rli]  & bt) != 0) ||
                  ((done[rli]   & bt) != 0) ||
                  ((active[rli] & bt) != 0))
                continue;

              // Check if all predecessors of the successor are done.

              int     pnxt    = dgraph[suc];
              boolean isReady = true;
              while(pnxt > 0) {
                int pred = bag[pnxt] >> DEPENDENCE_SHIFT;
                pnxt = bag[pnxt + 1];
                if (pred == ind)
                  continue;

                int pli = pred / 32;
                int pb  = pred - (pli * 32);

                if ((done[pli] & (1 << pb)) == 0) {
                  isReady = false;
                  break;
                }
              }

              if (isReady) { // Add successor to ready list.
                ready[rli] |= bt;
                readyCount++;
                assert checkOff("  rdy", done, suc) : "Already executed " + suc;
              }
            }
          }
        }
      }

      if (activeCount <= 0)
        continue;

      // For each instruction on the active list, place its
      // issue-dependent successors on the ready list.

      for (int ali = 0; ali < nws; ali++) { // 32 at a time
        int wrd = active[ali];
        if (wrd == 0)
          continue;

        for (int j = 0; j < 4; j++) { // 8 at a time
          int b = (wrd >> (j * 8)) & 0xff;
          if (b == 0)
            continue;

          for (int k = 0; k < 8; k++) { // one at a time
            if ((b & (1 << k)) == 0)
              continue;

            int     ab  = (j * 8) + k;
            int     ind = (ali * 32) + ab;

            // Check each successor to see if it is now ready.

            int nxt = sgraph[ind];
            while (nxt > 0) {
              int x = bag[nxt];
              nxt = bag[nxt + 1];
              if (((x & DEPENDENCE_MASK) != ISSUE_DEPENDENCE))
                continue;

              int suc = x >> DEPENDENCE_SHIFT;
              int rli = suc / 32;
              int rb  = suc - (rli * 32);
              int bt  = 1 << rb;

              if (((ready[rli]  & bt) != 0) ||
                  ((done[rli]   & bt) != 0) ||
                  ((active[rli] & bt) != 0))
                continue;

              // Check if all predecessors of the successor are done.

              int     pnxt    = dgraph[suc];
              boolean isReady = true;
              while(pnxt > 0) {
                int pred = bag[pnxt] >> DEPENDENCE_SHIFT;
                pnxt = bag[pnxt + 1];
                if (pred == ind)
                  continue;

                int pli = pred / 32;
                int pb  = pred - (pli * 32);

                if ((done[pli] & (1 << pb)) == 0) {
                  isReady = false;
                  break;
                }
              }

              if (isReady) { // Add successor to ready list.
                ready[rli] |= bt;
                readyCount++;
                assert checkOff("  rdy", done, suc) : "Already executed " + suc;
              }
            }
          }
        }
      }
    }

    return cycle;
  }

  private boolean checkOn(String msg, int[] bits, int inst)
  {
    if (false && trace)
      System.out.println(msg + " " + inst + " " + linear[inst]);

    int li = inst / 32;
    int bt = inst - (li * 32);
    return (0 != (bits[li] & (1 << bt)));
  }

  private boolean checkOff(String msg, int[] bits, int inst)
  {
    if (false && trace)
      System.out.println(msg + " " + inst + " " + linear[inst]);

    int li = inst / 32;
    int bt = inst - (li * 32);
    return (0 == (bits[li] & (1 << bt)));
  }

  /**
   * Use backward list scheduling.
   * @param ic is the number of instructions in the basic block
   * @param sched is the scheduled list of instructions
   * @return the number of cycles required for the schedule
   */
  private int backwardSchedule(int ic, int[] sched)
  {
    int nws = (ic + 31) / 32;
    for (int i = 0; i < nws; i++) {
      ready[i]  = 0;
      active[i] = 0;
      done[i]   = 0;
    }

    int activeCount = 0;
    int readyCount  = 0;

    for (int i = 0; i < ic; i++) {
      int ind = ic - i - 1;
      time[ind] = 0;
      if (sgraph[ind] == 0) { // No initial dependencies.
        int rli = i / 32;
        int rb  = i - (rli * 32);
        assert checkOff("  rdy", ready, i) : "Already ready " + ind;
        ready[rli] |= 1 << rb;
        readyCount++;
      }
    }

    int sptr  = ic;
    int cycle = 1;
    while ((readyCount > 0) || (activeCount > 0)) {
      if (readyCount > 0) { // Find first ready list item.
        int rli; // ready list word index.
        for (rli = 0; rli < nws; rli++) {
          if (ready[rli] != 0)
            break;
        }

        if (rli < nws) { // Execute ready instruction.
          int rw = ready[rli];
          int rb = 0; // ready word bit offset.
          if ((rw & 0xffff) == 0)
            rb += 16;
          if (((rw >> rb) & 0xff) == 0)
            rb += 8;
          if (((rw >> rb) & 0x0f) == 0)
            rb += 4;
          if (((rw >> rb) & 0x03) == 0)
            rb += 2;
          if (((rw >> rb) & 0x01) == 0)
            rb += 1;

          // Remove from ready list and schedule it.

          do {
            int bit = 1 << rb; // This is right-to-left.
            int ri  = (rli * 32) + rb;
            int ind = ic - ri - 1;

            if ((rw & bit) != 0) { // It's ready.
              int fu = linear[ind].getFunctionalUnit();
              if (fusi[fu] < fusa[fu]) { // Functional unit is ready.
                if ((sptr - ind) < 8) { // Distance not too great.
                  fusi[fu]++;
                  // Execute ready instruction.
                  // Remove instruction from the ready list.

                  rw &= ~bit;
                  ready[rli] = rw;
                  readyCount--;

                  // Add instruction to the active list.

                  assert checkOff("  exe", active, ri) : "Already ready " + ind;

                  int x = active[rli];
                  active[rli] |= bit;
                  if (x != active[rli])
                    activeCount++;

                  // Add instruction to the schedule.

                  sched[--sptr] = ind;
                  time[ind] = cycle;
                }
              }
            }

            if (readyCount <= 0)
              break;

            rb++;
            if (rb >= 32) {
              do {
                rli++;
                if (rli >= nws)
                  break;
                rw  = ready[rli];
              } while (rw == 0);
              rb = 0;
            }
          } while (rli < nws);
        }
      }

      cycle++;

      if (activeCount <= 0)
        continue;

      // For each instruction on the active list that is complete
      // put all its successors on the ready list.

      for (int ali = 0; ali < nws; ali++) { // 32 at a time
        int wrd = active[ali];
        if (wrd == 0)
          continue;

        for (int j = 0; j < 4; j++) { // 8 at a time
          int b = (wrd >> (j * 8)) & 0xff;
          if (b == 0)
            continue;

          for (int k = 0; k < 8; k++) { // one at a time
            if ((b & (1 << k)) == 0)
              continue;

            int ab  = (j * 8) + k;
            int ai  = ((ali * 32) + ab);
            int ind = ic - ai - 1;
            boolean exe = ((time[ind] + delay[ind]) < cycle);

            if (!exe)
              continue;

            // Instruction is completed, remove from active list.

            int bit = 1 << ab;
            assert checkOn("  fin", active, ai) : "Not active " + ind;

            wrd = wrd & ~bit;
            active[ali] = wrd;
            activeCount--;
            int fu = linear[ind].getFunctionalUnit();
            fusi[fu]--;

            // Mark the instruction as done.

            done[ali] |= bit;

            // Check each predecessor to see if it is now ready.

            int nxt = dgraph[ind];
            while (nxt > 0) {
              int x = bag[nxt];
              nxt = bag[nxt + 1];

              int suc = x >> DEPENDENCE_SHIFT;
              int ri  = ic - suc - 1;
              int rli = ri / 32;
              int rb  = ri - (rli * 32);
              int bt  = 1 << rb;

              if (((ready[rli]  & bt) != 0) ||
                  ((done[rli]   & bt) != 0) ||
                  ((active[rli] & bt) != 0))
                continue;

              // Check if all successors of the predecessor are done.

              int     pnxt    = sgraph[suc];
              boolean isReady = true;
              while(pnxt > 0) {
                int pred = bag[pnxt] >> DEPENDENCE_SHIFT;
                pnxt = bag[pnxt + 1];
                if (pred == ind)
                  continue;

                int pi  = ic - pred - 1;
                int pli = pi / 32;
                int pb  = pi - (pli * 32);

                if ((done[pli] & (1 << pb)) == 0) {
                  isReady = false;
                  break;
                }
              }

              if (isReady) { // Add successor to ready list.
                ready[rli] |= bt;
                readyCount++;
                assert checkOff("  rdy", done, ri) : "Already executed " + suc;
              }
            }
          }
        }
      }

      if (activeCount <= 0)
        continue;

      // For each instruction on the active list, place its
      // issue-dependent successors on the ready list.

      for (int ali = 0; ali < nws; ali++) { // 32 at a time
        int wrd = active[ali];
        if (wrd == 0)
          continue;

        for (int j = 0; j < 4; j++) { // 8 at a time
          int b = (wrd >> (j * 8)) & 0xff;
          if (b == 0)
            continue;

          for (int k = 0; k < 8; k++) { // one at a time
            if ((b & (1 << k)) == 0)
              continue;

            int ab  = (j * 8) + k;
            int ai  = ((ali * 32) + ab);
            int ind = ic - ai - 1;

            // Check each predecessor to see if it is now ready.

            int nxt = dgraph[ind];
            while (nxt > 0) {
              int x = bag[nxt];
              nxt = bag[nxt + 1];
              if (((x & DEPENDENCE_MASK) != ISSUE_DEPENDENCE))
                continue;

              int suc = x >> DEPENDENCE_SHIFT;
              int ri  = ic - suc - 1;
              int rli = ri / 32;
              int rb  = ri - (rli * 32);
              int bt  = 1 << rb;

              if (((ready[rli]  & bt) != 0) ||
                  ((done[rli]   & bt) != 0) ||
                  ((active[rli] & bt) != 0))
                continue;

              // Check if all successors of the predecessor are done.

              int     pnxt    = sgraph[suc];
              boolean isReady = true;
              while(pnxt > 0) {
                int pred = bag[pnxt] >> DEPENDENCE_SHIFT;
                pnxt = bag[pnxt + 1];
                if (pred == ind)
                  continue;

                int pi  = ic - pred - 1;
                int pli = pi / 32;
                int pb  = pi - (pli * 32);

                if ((done[pli] & (1 << pb)) == 0) {
                  isReady = false;
                  break;
                }
              }

              if (isReady) { // Add successor to ready list.
                ready[rli] |= bt;
                readyCount++;
                assert checkOff("  rdy", done, ri) : "Already executed " + suc;
              }
            }
          }
        }
      }
    }

    assert (sptr == 0) : "sptr not right." + sptr;

    return cycle;
  }

  /**
   * Do no scheduling.
   * @param ic is the number of instructions in the basic block
   * @param sched is the scheduled list of instructions
   * @return the number of cycles required for the schedule
   */
  private int noSchedule(int ic, int[] sched)
  {
    int nws  = (ic + 31) / 32;
    for (int i = 0; i < nws; i++) {
      ready[i]  = 0;
      active[i] = 0;
      done[i]   = 0;
    }

    int activeCount = 0;
    int readyCount  = 0;

    for (int i = 0; i < ic; i++) {
      time[i] = 0;
      if (dgraph[i] == 0) { // No initial dependencies.
        int rli = i / 32;
        int rb  = i - (rli * 32);
        assert checkOff("  rdy", ready, i) : "Already ready " + i;
        ready[rli] |= 1 << rb;
        readyCount++;
      }
    }

    int sptr  = 0;
    int cycle = 1;
    int nxtj  = 0;
    while ((readyCount > 0) || (activeCount > 0)) {
      if (readyCount > 0) {
        while (nxtj < ic) {
          int rli = nxtj / 32;
          int rb  = nxtj - (rli * 32);
          int bit = 1 << rb; // This is right-to-left.
          int rw  = ready[rli];

          if ((rw & bit) == 0) // It's not ready.
            break;

          int fu = linear[nxtj].getFunctionalUnit();
          if (fusi[fu] >= fusa[fu]) // Functional unit is not ready.
            break;

          fusi[fu]++;

          // Execute ready instruction.
          // Remove instruction from the ready list.

          rw &= ~bit;
          ready[rli] = rw;
          readyCount--;

          // Add instruction to the active list.

          assert checkOff("  exe", active, nxtj) : "Already ready " + nxtj;

          int x = active[rli];
          active[rli] |= bit;
          if (x != active[rli])
            activeCount++;

          // Add instruction to the schedule.

          sched[sptr++] = nxtj;
          time[nxtj] = cycle;
          nxtj++;

          if (readyCount <= 0)
            break;
        }
      }

      cycle++;

      if (activeCount <= 0)
        continue;

      // For each instruction on the active list that is complete
      // put all its successors on the ready list.

      for (int ali = 0; ali < nws; ali++) { // 32 at a time
        int wrd = active[ali];
        if (wrd == 0)
          continue;

        for (int j = 0; j < 4; j++) { // 8 at a time
          int b = (wrd >> (j * 8)) & 0xff;
          if (b == 0)
            continue;

          for (int k = 0; k < 8; k++) { // one at a time
            if ((b & (1 << k)) == 0)
              continue;

            int     ab  = (j * 8) + k;
            int     ind = (ali * 32) + ab;
            boolean exe = ((time[ind] + delay[ind]) < cycle);

            if (!exe)
              continue;

            int bit = 1 << ab;
            assert checkOn("  fin", active, ind) : "Not active " + ind;

            wrd = wrd & ~bit;
            active[ali] = wrd;
            activeCount--;
            int fu = linear[ind].getFunctionalUnit();
            fusi[fu]--;

            // Mark the instruction as done.

            done[ali] |= bit;

            // Check each successor to see if it is now ready.

            int nxt = sgraph[ind];
            while (nxt > 0) {
              int x = bag[nxt];
              nxt = bag[nxt + 1];

              int suc = x >> DEPENDENCE_SHIFT;
              int rli = suc / 32;
              int rb  = suc - (rli * 32);
              int bt  = 1 << rb;

              if (((ready[rli]  & bt) != 0) ||
                  ((done[rli]   & bt) != 0) ||
                  ((active[rli] & bt) != 0))
                continue;

              // Check if all predecessors of the successor are done.

              int     pnxt    = dgraph[suc];
              boolean isReady = true;
              while(pnxt > 0) {
                int pred = bag[pnxt] >> DEPENDENCE_SHIFT;
                pnxt = bag[pnxt + 1];
                if (pred == ind)
                  continue;

                int pli = pred / 32;
                int pb  = pred - (pli * 32);

                if ((done[pli] & (1 << pb)) == 0) {
                  isReady = false;
                  break;
                }
              }

              if (isReady) { // Add successor to ready list.
                ready[rli] |= bt;
                readyCount++;
                assert checkOff("  rdy", done, suc) : "Already executed " + suc;
              }
            }
          }
        }
      }

      if (activeCount <= 0)
        continue;

      // For each instruction on the active list, place its
      // issue-dependent successors on the ready list.

      for (int ali = 0; ali < nws; ali++) { // 32 at a time
        int wrd = active[ali];
        if (wrd == 0)
          continue;

        for (int j = 0; j < 4; j++) { // 8 at a time
          int b = (wrd >> (j * 8)) & 0xff;
          if (b == 0)
            continue;

          for (int k = 0; k < 8; k++) { // one at a time
            if ((b & (1 << k)) == 0)
              continue;

            int     ab  = (j * 8) + k;
            int     ind = (ali * 32) + ab;

            // Check each successor to see if it is now ready.

            int nxt = sgraph[ind];
            while (nxt > 0) {
              int x = bag[nxt];
              nxt = bag[nxt + 1];
              if (((x & DEPENDENCE_MASK) != ISSUE_DEPENDENCE))
                continue;

              int suc = x >> DEPENDENCE_SHIFT;
              int rli = suc / 32;
              int rb  = suc - (rli * 32);
              int bt  = 1 << rb;

              if (((ready[rli]  & bt) != 0) ||
                  ((done[rli]   & bt) != 0) ||
                  ((active[rli] & bt) != 0))
                continue;

              // Check if all predecessors of the successor are done.

              int     pnxt    = dgraph[suc];
              boolean isReady = true;
              while(pnxt > 0) {
                int pred = bag[pnxt] >> DEPENDENCE_SHIFT;
                pnxt = bag[pnxt + 1];
                if (pred == ind)
                  continue;

                int pli = pred / 32;
                int pb  = pred - (pli * 32);

                if ((done[pli] & (1 << pb)) == 0) {
                  isReady = false;
                  break;
                }
              }

              if (isReady) { // Add successor to ready list.
                ready[rli] |= bt;
                readyCount++;
                assert checkOff("  rdy", done, suc) : "Already executed " + suc;
              }
            }
          }
        }
      }
    }

    return cycle;
  }

  /**
   * Create the linear list of the instructions that are in the basic
   * block.
   * @param first is the first instruction in the basic block
   * @param last is the last instruction in the block
   * @return the number of instructions
   */
  private int linearize(Instruction first, Instruction last)
  {
    Instruction inst = first;
    int         k    = 0;
    while (inst != null) {
      if (k >= linear.length) {
        Instruction[] ln = new Instruction[2 * k];
        System.arraycopy(linear, 0, ln, 0, k);
        linear = ln;
        int[] ss = new int[2 * k];
        System.arraycopy(schedn, 0, ss, 0, k);
        schedn = ss;
        int[] sf = new int[2 * k];
        System.arraycopy(schedf, 0, sf, 0, k);
        schedf = sf;
        int[] sb = new int[2 * k];
        System.arraycopy(schedb, 0, sb, 0, k);
        schedb = sb;
      }

      linear[k++] = inst;

      if (inst == last)
        break;

      inst = inst.getNext();
    }

    if (k > maxSize)
      maxSize = k;

    return k;
  }

  /**
   * Create the dependence graph for the basic block.  The graph is
   * represented as a set of lists.  There are two lists for each
   * instruction.  One list is the set of instructions that can be
   * executed once this instruction has executed.  The other list are
   * those instructions that must complete before this instruction can
   * execute.  The two sets of lists represent a graph where each edge
   * is bi-directional.  There are no back edges in the graph.
   * @param ic is the number of instructions in the basic block
   */
  private void buildDependenceGraphs(int ic)
  {
    if ((dgraph == null) || (dgraph.length < ic)) {
      dgraph = new int[ic];
      sgraph = new int[ic];
      bag    = new int[ic * 2];
      time   = new int[ic];
      delay  = new int[ic];
      int nws = (ic + 31) / 32;
      if ((ready == null) || (ready.length < nws)) {
        ready  = new int[nws];
        active = new int[nws];
        done   = new int[nws];
      }
    } else {
      for (int i = 0; i < ic; i++) {
        dgraph[i] = 0;
        sgraph[i] = 0;
      }
    }

    bptr = 2;

    int lastStore = -1;

    for (int i = 0; i < ic; i++) {
      Instruction insti = linear[i];

      delay[i] = insti.getExecutionCycles();

      if (insti.isMarker()) {
        if (i > 0)
          addDependenceEdge(i - 1, i, ISSUE_DEPENDENCE); // We don't want to move a line marker.
        continue;
      }

      if (insti.isLoad()) {
        if (lastStore >= 0)
          addDependenceEdge(lastStore, i, ISSUE_DEPENDENCE); // We don't want to move a load before a store.
      } else if (insti.isStore()) {
        if (lastStore >= 0)
          addDependenceEdge(lastStore, i, ISSUE_DEPENDENCE); // We don't want to move a store before a store.

        // We don't want to move a store before a load.  We use this
        // loop instead of the easier "lastLoad" trick because we want
        // to be able to move loads before loads.  To use the
        // "lastLoad" trick we would have to prevent a load from being
        // moved before a load in order to keep the proper dependence
        // chanins.

        for (int j = 0; j < i; j++)
          if (linear[j].isLoad())
            addDependenceEdge(j, i, ISSUE_DEPENDENCE);

        lastStore = i;
      }

      int dest = insti.getDestRegister();
      if (dest < 0)
        continue;

      // In order for the register allocator to work properly, the
      // actual registers, assigned to a pseudo-register, must be
      // def-ed in order.  For example, on the Sparc, register %d0 is
      // assigned to actual registers %f0 and %f1 so %f0 must be
      // def-ed before %f1.  This logic attempts to make sure that
      // this order is not changed.  It assumes that %f0 is set in the
      // previous instruction.

      if ((i > 0) && registers.continueRegister(dest))
        addDependenceEdge(i - 1, i, EXE_DEPENDENCE);

      // Add in anti-dependencies. Instruction i is dependent on
      // instruction j if instruction j uses the destination register
      // of instruction i.

      for (int j = i - 1; j >= 0; j--) {
        Instruction instj = linear[j];
        boolean     def   = instj.defs(dest, registers);

        if (!def &&
            !instj.uses(dest, registers) &&
            instj.independent(insti, registers) &&
            !instj.setsSpecialReg() &&
            !insti.setsSpecialReg())
          continue;

        // Make instruction i dependent on instruction j.  If there
        // are two defs with no intervening use, the second def must
        // still come after the first.

        addDependenceEdge(j, i, EXE_DEPENDENCE);

        if (def)
          break;
      }

      // Find the normal dependencies.  Instruction j is dependent on
      // instruction i if instruction j uses the dest register.

      for (int j = i + 1; j < ic; j++) {
        Instruction instj = linear[j];

        if (instj.uses(dest, registers))
          addDependenceEdge(i, j, EXE_DEPENDENCE);

        if (instj.defs(dest, registers))
          break;
      }
    }
  }

  /**
   * Add dependence edge from instruction insti to instruction instj.
   * Instruction instj is dependent on instruction insti.
   */
  private void addDependenceEdge(int insti, int instj, int dType)
  {
    int sg = sgraph[insti];
    while (sg != 0) {
      if ((bag[sg] >> DEPENDENCE_SHIFT) == instj)
        return;
      sg = bag[sg + 1];
    }

    if ((bptr + 4) >= bag.length) {
      int[] ng = new int[bag.length * 2];
      System.arraycopy(bag, 0, ng, 0, bag.length);
      bag = ng;
    }

    bag[bptr + 0] = (instj << DEPENDENCE_SHIFT) + dType;
    bag[bptr + 1] = sgraph[insti];
    sgraph[insti] = bptr;
    bptr += 2;

    bag[bptr + 0] = (insti << DEPENDENCE_SHIFT) + dType;
    bag[bptr + 1] = dgraph[instj];
    dgraph[instj] = bptr;
    bptr += 2;
  }
}
