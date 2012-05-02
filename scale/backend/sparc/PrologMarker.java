package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class marks the position for the routine prolog.
 * <p>
 * $Id: PrologMarker.java,v 1.7 2005-02-07 21:27:38 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Assembly language for the Sparc requires that the assembly program specify information
 * about a routine for the loader and debugger.  This "prolog" information must come
 * after various instructions that comprize the routine entry.
 */

public class PrologMarker extends Marker
{
  private boolean usesGp      = false; /* True if the routine uses the GP register. */
  private int     mask        = 0;     /* Which integer registers are saved. */
  private int     fmask       = 0;     /* Which floating point registers are saved. */
  private int     frameSize   = 0;     /* Stack frame size. */
  private int     frameOffset = 0;     /* Offset to saved registers. */
  
  /**
   * Create a marker for generating the routine prolog information.
   * <p>
   * While executing the routine:
   * <ul>
   * <li> virtual_frame_pointer = stack_pointer + frameSize
   * <li> saved register address = virtual_frame_pointer - frameOffset
   * </ul>
   * The stack_pointer is kept in the SP register.
   * @param mask specifies which integer registers are saved by this routine
   * @param fmask specifies which floating point registers are saved by this routine
   * @param frameSize specifies the size in bytes of the stack frame for this routine and must be a multiple of 16
   * @param frameOffset specifies the offset from the virtual frame pointer to the saved registers
   * @param usesGp register is true if this routine requires a GP register value
   */
  public PrologMarker(int mask, int fmask, int frameSize, int frameOffset, boolean usesGp)
  {
    super();
    this.mask        = mask;
    this.fmask       = fmask;
    this.frameSize   = frameSize;
    this.frameOffset = frameOffset;
    this.usesGp      = usesGp;
  }

  /**
   * @return true if the routine requires a GP register value.
   */
  public boolean usesGp()
  {
    return usesGp;
  }

  /**
   * Insert the assembler directive for the prolog.
   */
  public void assembler(Assembler asm, Emit emit)
  {
  }
}


