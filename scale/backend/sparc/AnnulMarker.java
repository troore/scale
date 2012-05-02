package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class marks the effective position of an annulled instruction.
 * <p>
 * $Id: AnnulMarker.java,v 1.9 2005-03-24 13:56:52 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An annulled instruction acts as if it is part of the true edge of a
 * branch instruction.  For analysis purposes, it must seem to exist
 * on that edge.   For example,
 * <pre>
 *   sdivcc %l0,%l1,%l2
 *   bvs,a  .l1
 *   sethi  %hi(0x80000000),%l2
 * .l1:
 * </pre>
 * is equivalent to
 * <pre>
 *   sdivcc %l0,%l1,%l2
 *   bvs,a  .l1
 *   nop
 *   ba  .l0
 * .l1:
 *   sethi  %hi(0x80000000),%l2
 * .l0:
 * </pre>
 * In this example, the definition of register <tt>%l2</tt> by the <tt>sethi</tt> must occur on one edge only
 * or the definition of <tt>%l2</tt> by the <tt>sdivcc</tt> instruction is not seen as needed and the <tt>sdivcc</tt>
 * instruction will be removed by the register allocation pass.
 */

public class AnnulMarker extends Marker
{
  /**
   * - the link to the annulled instruction.
   */
  private SparcInstruction annulled;

  public AnnulMarker(SparcInstruction annulled)
  {
    this.annulled = annulled;
  }

  /**
   * Specify the registers used by this instruction.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator rs, int index, int strength)
  {
    annulled.specifyRegisterUsage(rs, index, strength);
  }

  public void assembler(Assembler asm, Emit emit)
  {
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("AnnulMarker ");
    buf.append(annulled);
    return buf.toString();
  }
}
