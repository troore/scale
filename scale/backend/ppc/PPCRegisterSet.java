package scale.backend.ppc;

import scale.backend.*;
import scale.common.*;

/** 
 * This class describes the register set of the PowerPC.
 * <p>
 * $Id: PPCRegisterSet.java,v 1.8 2005-02-07 21:27:32 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The PPC has 64 programmable registers.  The first 32 registers are used for integer values 
 * and addresses.  This includes the stack pointer and other housekeeping registers.
 * Registers 32 through 63 are used for floating point values.
 */

public abstract class PPCRegisterSet extends scale.backend.RegisterSet
{
  /**
   * Stack pointer register.
   */
  public static final int SP_REG  = 1;
  /**
   * Table of contents register.
   */
  public static final int TOC_REG = 2;
  /**
   * First integer argument register.
   */
  public static final int FIA_REG  = 3;
  /**
   * Last integer argument register.
   */
  public static final int LIA_REG  = 10;
  /**
   * First floating point argument register.
   */
  public static final int FFA_REG  = 33;
  /**
   * Last floating point argumenmt register for MACOSX.
   */
  public static final int LFA_REG_MACOSX  = 45;
  /**
   * Last floating point argument register for Linux.
   */
  public static final int LFA_REG_LINUX   = 40;
  /**
   * Integer function value return register.
   */
  public static final int IR_REG   = 3;
  /**
   * Floating point function value return register. 
   */
  public static final int FR_REG   = 33;

  public PPCRegisterSet(short[] actualRegisters)
  {
    super(actualRegisters);
  }

  /**
   * Return the size of the register in bytes.
   */
  public int registerSize(int reg)
  {
    return ((getType(reg) & sizeMask) + 1) * 4;
  }

  /**
   * Return the number of unique registers that can hold programmer values.
   */
  public int numAllocatableRegisters()
  {
    return 64;
  }  
}
