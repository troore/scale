package scale.backend.sparc;

import scale.backend.*;
import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.RealType;

/** 
 * This class describes the register set of the Sparc.
 * <p>
 * $Id: SparcRegisterSet.java,v 1.13 2005-02-07 21:27:39 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The Sparc has 96 programmable registers.  The registers 0..31 are used for integer values 
 * and addresses.  This includes the stack pointer and other housekeeping registers.
 * Registers 0 contains a hardwired 0 value.
 * <p>
 * Registers 32 through 63 are used for single precision floating point values.
 * Registers 64 through 95 are used for double precision floating point values.
 * Registers 96 through 111 are used for quad precision floating point values.
 * Note that registers [32:63] overlap registers [64:79] and [96:103].
 * This is handled in register allocation by the rangebegin() and rangeEnd()
 * methods.
 */

public abstract class SparcRegisterSet extends scale.backend.RegisterSet
{
  /**
   * Integer global register %g0. 
   */
  public static final int G0_REG =  0;
  /**
   * Integer global register %g1. 
   */
  public static final int G1_REG =  1;
  /**
   * Integer global register %g2. 
   */
  public static final int G2_REG =  2;
  /**
   * Integer global register %g3. 
   */
  public static final int G3_REG =  3;
  /**
   * Integer global register %g4. 
   */
  public static final int G4_REG =  4;
  /**
   * Integer global register %g5. 
   */
  public static final int G5_REG =  5;
  /**
   * Integer global register %g6. 
   */
  public static final int G6_REG =  6;
  /**
   * Integer global register %g7. 
   */
  public static final int G7_REG =  7;
  /**
   * Integer global register %o0. 
   */
  public static final int O0_REG =  8;
  /**
   * Integer global register %o1. 
   */
  public static final int O1_REG =  9;
  /**
   * Integer global register %o2. 
   */
  public static final int O2_REG =  10;
  /**
   * Integer global register %o3. 
   */
  public static final int O3_REG =  11;
  /**
   * Integer global register %o4. 
   */
  public static final int O4_REG =  12;
  /**
   * Integer global register %o5. 
   */
  public static final int O5_REG =  13;
  /**
   * Integer global register %o6. 
   */
  public static final int O6_REG =  14;
  /**
   * Integer global register %o7. 
   */
  public static final int O7_REG =  15;
  /**
   * Integer global register %l0. 
   */
  public static final int L0_REG =  16;
  /**
   * Integer global register %l1. 
   */
  public static final int L1_REG =  17;
  /**
   * Integer global register %l2. 
   */
  public static final int L2_REG =  18;
  /**
   * Integer global register %l3. 
   */
  public static final int L3_REG =  19;
  /**
   * Integer global register %l4. 
   */
  public static final int L4_REG =  20;
  /**
   * Integer global register %l5. 
   */
  public static final int L5_REG =  21;
  /**
   * Integer global register %l6. 
   */
  public static final int L6_REG =  22;
  /**
   * Integer global register %l7. 
   */
  public static final int L7_REG =  23;
  /**
   * Integer global register %i0. 
   */
  public static final int I0_REG =  24;
  /**
   * Integer global register %i1. 
   */
  public static final int I1_REG =  25;
  /**
   * Integer global register %i2. 
   */
  public static final int I2_REG =  26;
  /**
   * Integer global register %i3. 
   */
  public static final int I3_REG =  27;
  /**
   * Integer global register %i4. 
   */
  public static final int I4_REG =  28;
  /**
   * Integer global register %i5. 
   */
  public static final int I5_REG =  29;
  /**
   * Integer global register %i6. 
   */
  public static final int I6_REG =  30;
  /**
   * Integer global register %i7. 
   */
  public static final int I7_REG =  31;
  /**
   * Floating point register %f0. 
   */
  public static final int F0_REG =  32;
  /**
   * Floating point register %f1. 
   */
  public static final int F1_REG =  33;
  /**
   * Frame pointer register.
   */
  public static final int FP_REG = 30;
  /**
   * Stack pointer register 
   */
  public static final int SP_REG = 14;
  /**
   * Floating point double-precision register %d0. 
   */
  public static final int D0_REG =  64;
  /**
   * Floating point double-precision register %d2. 
   */
  public static final int D2_REG =  66;
  /**
   * Floating point quad-precision register %q0. 
   */
  public static final int Q0_REG =  96;
  /**
   * Floating point quad-precision register %q4. 
   */
  public static final int Q2_REG =  98;

  public SparcRegisterSet(short[] actualRegisters)
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
    return 63;
  }

  /**
   * Return the register type with the size information added.
   * @param type is the type required
   * @param bs is the size required
   */
  public int tempRegisterType(Type type, long bs)
  {
    int rt = RegisterSet.INTREG;

    if (type.isPointerType())
      rt = RegisterSet.ADRREG;
    else if (type.isRealType())
      rt = RegisterSet.FLTREG;

    if (type.isComplexType()) {
      rt |= RegisterSet.PAIRREG;
      bs >>= 1;
    }

    if (bs > 4) {
      if (bs > 8)
        rt |= RegisterSet.QUADREG;
      else
        rt |= RegisterSet.DBLEREG;
    }

    return rt;
  }
}
