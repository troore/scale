package scale.backend.alpha;

import scale.backend.*;
import scale.clef.type.Type;
import scale.clef.type.AggregateType;

/** 
 * This class describes the register set of the Alpha.
 * <p>
 * $Id: AlphaRegisterSet.java,v 1.26 2007-03-21 13:31:46 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The Alpha has 64 programmable registers.  The first 31 registers are used for integer values 
 * and addresses.  This includes the stack pointer and other housekeeping registers.
 * Registers 31 and 63 contain a hardwired 0 value.  Registers 32 through 62 are used for 
 * floating point values.
 */

public class AlphaRegisterSet extends scale.backend.RegisterSet
{
  /**
   * Integer function value return register. 
   */
  public static final int IR_REG =  0;
  /**
   * Frame pointer register 
   */
  public static final int FP_REG = 15;
  /**
   * First integer argument register. 
   */
  public static final int IF_REG = 16;
  /**
   * Last integer argument register. 
   */
  public static final int IL_REG = 21;
  /**
   * Return address register 
   */
  public static final int RA_REG = 26;
  /**
   * Routine address register.
   */
  public static final int PV_REG = 27;
  /**
   * Used by the assembler. 
   */
  public static final int AT_REG = 28;
  /**
   * Global Pointer register 
   */
  public static final int GP_REG = 29;
  /**
   * Stack pointer register 
   */
  public static final int SP_REG = 30;
  /**
   * Integer zero register 
   */
  public static final int I0_REG = 31;
  /**
   * Floating point function value return register. 
   */
  public static final int FR_REG = 32;
  /**
   * First floating point argument register. 
   */
  public static final int FF_REG = 48;
  /**
   * Last floating point argument register. 
   */
  public static final int FL_REG = 53;
  /**
   * Floating point zero register. 
   */
  public static final int F0_REG = 63;
  /**
   * Size in bytes of integer register. 
   */
  public static final int IREG_SIZE = 8;
  /**
   * Size in bytes of integer register. 
   */
  public static final int FREG_SIZE = 8;

  /**
   * The types of real registers on the Alpha. 
   */
  private static final short[] actualRegisters = {
     AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,
     AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,
     AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,
     AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG + RDREG,
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG + RDREG,
  };

  /**
   * The preferred order in which registers should be allocated.
   * This array must be the same length as actualRegisters.
   */
  private static final short[] preferredOrder = {
    16, 17, 18, 19, 20, 21,             // $16 - $21 argument passing registers
     0,  1,  2,  3,  4,  5,  6,  7,  8, // $0 - $8 scratch registera
    26, 27, 15,                         // $ra, $pv, $fp
    22, 23, 24, 25, 28,                 // $22 - $25, $28 scratch registers
     9, 10, 11, 12, 13, 14,             // $9 - $14 saved over calls
    32, 33, 42, 43, 44, 45, 46, 47,     // %f0, $f1, $f10 - $f15 scratch registers
    54, 55, 56, 57, 58, 59, 60, 61, 62, // $f54 - $f30 scratch registers
    48, 49, 50, 51, 52, 53,             // $f16 - $f53 argument registers
    34, 35, 36, 37, 38, 39, 40, 41,     // $f2 - $f9 saved over calls
    29, 30                              // $gp, $sp
  };

  /**
   * The registers that a callee can use without saving and restoring.
   */
  private static final short[] calleeUses = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    32, 33, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58,59, 60, 61, 62, 63
  };

  /**
   * The registers that a callee must save and restore if they are used by the callee.
   */
  private static final short[] calleeSaves = {9, 10, 11, 12, 13, 14, 15, 34, 35, 36, 37, 38, 39, 40, 41};

  /**
   * The names of real registers on the Alpha. 
   */
  private static final String[] regNames = {
      "$0",   "$1",   "$2",   "$3",   "$4",   "$5",   "$6",   "$7",
      "$8",   "$9",  "$10",  "$11",  "$12",  "$13",  "$14",  "$15",
     "$16",  "$17",  "$18",  "$19",  "$20",  "$21",  "$22",  "$23",
     "$24",  "$25",  "$26",  "$27",  "$28",  "$gp",  "$sp",  "$31",
     "$f0",  "$f1",  "$f2",  "$f3",  "$f4",  "$f5",  "$f6",  "$f7",
     "$f8",  "$f9", "$f10", "$f11", "$f12", "$f13", "$f14", "$f15",
    "$f16", "$f17", "$f18", "$f19", "$f20", "$f21", "$f22", "$f23",
    "$f24", "$f25", "$f26", "$f27", "$f28", "$f29", "$f30", "$f31",
  };

  public AlphaRegisterSet()
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
      return "$fv" + reg;

    return "$v" + reg;
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
   * Return true if virtual registers, that require more than one real
   * register, must be allocated to contiguous real registers.
   */
  public boolean useContiguous()
  {
    return false;
  }

  /**
   * Return the size of the register in addressable memory units.
   */
  public int registerSize(int reg)
  {
    return ((getType(reg) & sizeMask) + 1) * 8;
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
   */
  public int numAllocatableRegisters()
  {
    return 62;
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
    else if (type.isRealType()) {
      rt = RegisterSet.FLTREG;
      if (type.isComplexType()) {
        rt |= RegisterSet.PAIRREG;
        bs >>= 1;
      }
    } else if (type.isAggregateType()) {
      int ft = ((AggregateType) type).allFieldsType();
      rt = (ft == AggregateType.FT_INT) ? RegisterSet.INTREG : RegisterSet.FLTREG;
    }

    if (bs > IREG_SIZE)
      rt |= RegisterSet.DBLEREG;

    return rt;
  }
}
