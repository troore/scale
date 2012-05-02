package scale.backend.mips;

import scale.backend.*;
import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.RealType;

/** 
 * This class describes the register set of the Mips.
 * <p>
 * $Id: MipsRegisterSet.java,v 1.11 2005-02-07 21:27:24 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The Mips has 64 programmable registers.  31 of the first 32 registers are used for integer 
 * value and addresses; register 0 constains a hardwired 0 value.  This includes the stack pointer
 * and other housekeeping registers.
 * Registers 32 through 63 are used for floating point values.
 */

public class MipsRegisterSet extends scale.backend.RegisterSet
{
  /**
   * Zero register
   */
  public static final int ZERO_REG = 0;
  /**
   * Pointer to global area
   */
  public static final int GP_REG = 28;
  /**
   * Stack pointer register
   */
  public static final int SP_REG = 29;
  /**
   * Frame pointer register
   */
  public static final int FP_REG = 30;
  /**
   * Return address register
   */
  public static final int RA_REG = 31;

  /**
   * Last integer register
   */
  public static final int LAST_INT_REG = 31;

  /**
   * Integer result register 1 of 2
   */
  public static final int IR_REG = 2;
  /**
   * Integer result register 2 of 2
   */
  public static final int IR2_REG = 3;
  /**
   * Floating point result register
   */
  public static final int FR_REG = 32;
  /**
   * Second floating point result register (complex part)
   */
  public static final int FR2_REG = 34;
  /**
   * First argument register
   */
  public static final int IA0_REG = 4;
  /**
   * Integer argument register
   */
  public static final int IA1_REG = 5;
  /**
   * Integer argument register
   */
  public static final int IA2_REG = 6;
  /**
   * Integer argument register
   */
  public static final int IA3_REG = 7;
  /**
   * Integer argument register
   */
  public static final int IA4_REG = 8;
  /**
   * Integer argument register
   */
  public static final int IA5_REG = 9;
  /**
   * Integer argument register
   */
  public static final int IA6_REG = 10;
  /**
   * Integer argument register
   */
  public static final int IA7_REG = 11;
  /**
   * Floating point argument register
   */
  public static final int FA0_REG = 44;
  /**
   * Floating point argument register
   */
  public static final int FA1_REG = 45;
  /**
   * Floating point argument register
   */
  public static final int FA2_REG = 46;
  /**
   * Floating point argument register
   */
  public static final int FA3_REG = 47;
  /**
   * Floating point argument register
   */
  public static final int FA4_REG = 48;
  /**
   * Floating point argument register
   */
  public static final int FA5_REG = 49;
  /**
   * Floating point argument register
   */
  public static final int FA6_REG = 50;
  /**
   * Floating point argument register
   */
  public static final int FA7_REG = 51;

  /**
   * Temp register 9; used for function calling
   */
  public static final int T9_REG = 25;


  /*
   * FCC0 cc "register"
   */
  public static final int FCC0 = 64;

  /*
   * LO special register
   */
  public static final int LO_REG = 72;

  /*
   * HI special register
   */
  public static final int HI_REG = 73;

  /**
   * Size in bytes of integer register. 
   */
  public static final int IREG_SIZE = 8;
  /**
   * Size in bytes of floating point register. 
   */
  public static final int FREG_SIZE = 8;


  public static final int ZEROREG = AIREG + RDREG;

  public static final int RESERVEDREG = AIREG + RDREG;

  public static final int CCREG = INTREG + RDREG;
    
  public static final int SPECIAL = INTREG + RDREG;

  /**
   * The types of real registers on the Mips. 
   */

  private static final short[] actualRegisters = {
    ZEROREG, AIREG,  AIREG,       AIREG,       AIREG,  AIREG,  AIREG,  AIREG,
    AIREG,   AIREG,  AIREG,       AIREG,       AIREG,  AIREG,  AIREG,  AIREG,
    AIREG,   AIREG,  AIREG,       AIREG,       AIREG,  AIREG,  AIREG,  AIREG,
    AIREG,   AIREG,  RESERVEDREG, RESERVEDREG, AIREG,  AIREG,  AIREG,  AIREG, // $26, $27 reserved for os kernal
    FLTREG,  FLTREG, FLTREG,      FLTREG,      FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG,  FLTREG, FLTREG,      FLTREG,      FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG,  FLTREG, FLTREG,      FLTREG,      FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG,  FLTREG, FLTREG,      FLTREG,      FLTREG, FLTREG, FLTREG, FLTREG, // Last "true" registers
    CCREG,   CCREG,  CCREG,       CCREG,       CCREG,  CCREG,  CCREG,  CCREG,  // The floating point cc "registers"
    SPECIAL, SPECIAL,                                                          // The LO and HI special registers
  };

  /**
   * The preferred order in which registers should be allocated.
   * This array must be the same length as actualRegisters.
   * {Preliminary version (Jeff)}
   */
  private static final short[] preferredOrder = {
    2,  3,  1, 12, 13, 14, 15, 24, 25,             // at, results, temp int registers
    4,  5,  6,  7,  8,  9, 10, 11,                 // int argument passing registers
    16, 17, 18, 19, 20, 21, 22, 23,                 // int saved registers
    32, 34, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43, // fp results, temp registers
    52, 53, 54, 55,                                 // fp temp registers
    44, 45, 46, 47, 48, 49, 50, 51,                 // fp argument registers
    56, 57, 58, 59, 60, 61, 62, 63,                 // fp saved registers
    28, 29, 30, 31, 26, 27,  0,

    64, 65, 66, 67, 68, 69, 70, 71, 72, 73          // cc and LO/HI special registers
  };

  /**
   * The registers that a callee can use without saving and restoring.
   */
  private static final short[] calleeUses = {
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 24, 25,
    31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55 
  };

  /**
   * The registers that a callee must save and restore if they are used by the callee.
   */
  private static final short[] calleeSaves = {
    16, 17, 18, 19, 20, 21, 22, 23, 28, 29, 30, 56, 57, 58, 59, 60, 61, 62, 63 };

  /**
   * The names of real registers on the Mips. 
   */
  private static final String[] regNames = {
    "$0",    "$1",    "$2",    "$3",    "$4",    "$5",    "$6",    "$7",
    "$8",    "$9",   "$10",   "$11",   "$12",   "$13",   "$14",   "$15",
    "$16",   "$17",   "$18",   "$19",   "$20",   "$21",   "$22",   "$23",
    "$24",   "$25",   "$26",   "$27",   "$gp",   "$sp",   "$30",   "$31",
    "$f0",   "$f1",   "$f2",   "$f3",   "$f4",   "$f5",   "$f6",   "$f7",
    "$f8",   "$f9",  "$f10",  "$f11",  "$f12",  "$f13",  "$f14",  "$f15",
    "$f16",  "$f17",  "$f18",  "$f19",  "$f20",  "$f21",  "$f22",  "$f23",
    "$f24",  "$f25",  "$f26",  "$f27",  "$f28",  "$f29",  "$f30",  "$f31",
    "$fcc0", "$fcc1", "$fcc2", "$fcc3", "$fcc4", "$fcc5", "$fcc6", "$fcc7",
    "$LO",   "$HI",
  };

  public MipsRegisterSet()
  {
    super(actualRegisters);
  }

  /**
   * Convert a register number into its assembly language form.
   */
  public String registerName(int reg)
  {
    if (reg < actualRegisters.length)
      return regNames[reg];

    if (floatRegister(reg))
      return "%fv" + reg;

    return "%v" + reg;
  }

  /**
   * Return a mapping from an order index to a real register number.
   * This mapping allows the order of allocation of real registers to be specified.
   */
  public short[] getPreferredOrder()
  {
    return preferredOrder;
  }

  /**
   * Return the size of the register in addressable memory units.
   */
  public int registerSize(int reg)
  {
    return ((getType(reg) & sizeMask) + 1) * 4;
  }

  /**
   * Return the callee saves registers.
   */
  public short[] getCalleeSaves()
  {
    return calleeSaves;
  }

  /**
   * Return the callee uses registers.
   */
  public short[] getCalleeUses()
  {
    return calleeUses;
  }

  /**
   * Return the number of unique registers that can hold programmer values.
   * @return the number of unique registers that can hold programmer values.
   */
  public int numAllocatableRegisters()
  {
    return 61;
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

    if (bs > 4) {
      if (type.isComplexType())
        rt |= RegisterSet.PAIRREG;
      else if (bs > 8)
        rt |= RegisterSet.QUADREG;
      else
        rt |= RegisterSet.DBLEREG;
    }

    return rt;
  }
}
