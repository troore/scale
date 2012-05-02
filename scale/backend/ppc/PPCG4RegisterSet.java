package scale.backend.ppc;

import scale.backend.*;
import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.RealType;

/** 
 * This class describes the register set of the G4 PowerPC.
 * <p>
 * $Id: PPCG4RegisterSet.java,v 1.12 2007-10-04 19:57:55 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The PPC has 67 programmable registers.  The first 32 registers are
 * used for integer values and addresses.  This includes the stack
 * pointer and other housekeeping registers.  Registers 32 through 62
 * are used for floating point values.
 */

public class PPCG4RegisterSet extends PPCRegisterSet
{
  /**
   * Size in bytes of integer register. 
   */
  public static final int IREG_SIZE = 4;
  /**
   * Size in bytes of integer register. 
   */
  public static final int FREG_SIZE = 8;
  /**
   * Boolean indicating macosx operating system (true) or linux (false)
   */
  private boolean macosx;

  /**
   * The types of real registers on the PPC. 
   */
  public static final short[] actualRegisters = {
    // 32 32-bit integer registers
    INTREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,
    AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,
    AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,
    AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,  AIREG,
    // 32 64-bit floating point registers
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG,
    FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG, FLTREG,
    // 16 64-bit integer registers
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    AIREG + DBLEREG, AIREG + CNTREG,  AIREG + DBLEREG, AIREG + CNTREG,
    // 8 128-bit integer registers
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
    AIREG + QUADREG, AIREG + CNTREG,  AIREG + CNTREG, AIREG + CNTREG,
  };

  /**
   * The names of real registers on the PPC for macosx. 
   */
  private static final String[] regNamesMacosx = {
     // 32 32-bit integer registers
     "r0",  "r1",  "r2",  "r3",  "r4",  "r5",  "r6",  "r7",
     "r8",  "r9", "r10", "r11", "r12", "r13", "r14", "r15",
    "r16", "r17", "r18", "r19", "r20", "r21", "r22", "r23",
    "r24", "r25", "r26", "r27", "r28", "r29", "r30", "r31",
     // 32 64-bit floating point registers
     "f0",  "f1",  "f2",  "f3",  "f4",  "f5",  "f6",  "f7",
     "f8",  "f9", "f10", "f11", "f12", "f13", "f14", "f15",
    "f16", "f17", "f18", "f19", "f20", "f21", "f22", "f23",
    "f24", "f25", "f26", "f27", "f28", "f29", "f30", "f31",
     // 16 64-bit integer registers
     "r0",  "r1",  "r2",  "r3",  "r4",  "r5",  "r6",  "r7",
     "r8",  "r9", "r10", "r11", "r12", "r13", "r14", "r15",
    "r16", "r17", "r18", "r19", "r20", "r21", "r22", "r23",
    "r24", "r25", "r26", "r27", "r28", "r29", "r30", "r31",
     // 8 128-bit integer registers
     "r0",  "r1",  "r2",  "r3",  "r4",  "r5",  "r6",  "r7",
     "r8",  "r9", "r10", "r11", "r12", "r13", "r14", "r15",
    "r16", "r17", "r18", "r19", "r20", "r21", "r22", "r23",
    "r24", "r25", "r26", "r27", "r28", "r29", "r30", "r31",
   };
   
  /**
   * The names of real registers on the PPC for linux. 
   */
  private static final String[] regNamesLinux = {
     // 32 32-bit integer registers
     "0",  "1",  "2",  "3",  "4",  "5",  "6",  "7",
     "8",  "9", "10", "11", "12", "13", "14", "15",
    "16", "17", "18", "19", "20", "21", "22", "23",
    "24", "25", "26", "27", "28", "29", "30", "31",
     // 32 64-bit floating point registers
     "0",  "1",  "2",  "3",  "4",  "5",  "6",  "7",
     "8",  "9", "10", "11", "12", "13", "14", "15",
    "16", "17", "18", "19", "20", "21", "22", "23",
    "24", "25", "26", "27", "28", "29", "30", "31",
     // 16 64-bit integer registers
     "0",  "1",  "2",  "3",  "4",  "5",  "6",  "7",
     "8",  "9", "10", "11", "12", "13", "14", "15",
    "16", "17", "18", "19", "20", "21", "22", "23",
    "24", "25", "26", "27", "28", "29", "30", "31",
     // 8 128-bit integer registers
     "0",  "1",  "2",  "3",  "4",  "5",  "6",  "7",
     "8",  "9", "10", "11", "12", "13", "14", "15",
    "16", "17", "18", "19", "20", "21", "22", "23",
    "24", "25", "26", "27", "28", "29", "30", "31",
   };
 
  /**
   * The preferred order in which registers should be allocated.
   * This array must be the same length as actualRegisters.
   */
  public static final short[] preferredOrder = {
     // 30 32-bit integer registers
      3,  4,  5,  6,  7,  8,  9, 10,
     11, 12, 13,  0, 14, 15, 16, 17,
     18, 19, 20, 21, 21, 23, 24, 25,
     26, 27, 28, 29, 30, 31,
     // 32 64-bit floating point registers
     32, 33, 34, 35, 36, 37, 38, 39,
     40, 41, 42, 43, 44, 45, 46, 47,
     48, 49, 50, 51, 52, 53, 54, 55,
     56, 57, 58, 59, 60, 61, 62, 63,
     // 14 64-bit integer registers
     68, 70, 72, 74, 76, 78, 80, 82,
     84, 86, 88, 90, 92, 94,
     // 7 64-bit integer registers
     100, 104, 108, 112, 116, 120, 124
  };

  /**
   * The registers that a callee can use without saving and restoring.
   */
  public static final short[] calleeUsesLinux = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45
  };

  /**
   * The registers that a callee can use without saving and restoring.
   */
  public static final short[] calleeUsesMacosx = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45
  };

  /**
   * The registers that a callee must save and restore if they are
   * used by the callee.
   */
  public static final short[] calleeSavesMacosx = {
    13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
    77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95
  };
  
    /**
   * The registers that a callee must save and restore if they are
   * used by the callee.
   */
  public static final short[] calleeSavesLinux = {
    14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
    78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95
  };

  /**
   * First physical register accessed by actual register.
   */
  private static final int[] firstAliasReg = {
    // 32 32-bit integer registers

     0,  1,  2,  3,  4,  5,  6,  7,  // 000-007 single to single
     8,  9, 10, 11, 12, 13, 14, 15,  // 008-015 single to single
    16, 17, 18, 19, 20, 21, 22, 23,  // 016-023 single to single
    24, 25, 26, 27, 28, 29, 30, 31,  // 024-031 single to single

    // 32 64-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

    // 16 64-bit integer registers
    
      0,  0,  2,  2,  4,  4,  6,  6,  // 064-071 single to double
      8,  8, 10, 10, 12, 12, 14, 14,  // 072-079 single to double
     16, 16, 18, 18, 20, 20, 22, 22,  // 080-087 single to double
     24, 24, 26, 26, 28, 28, 30, 30,  // 088-095 single to double
     
    // 8 128-bit integer registers
    
      0,  0,  0,  0,  4,  4,  4,  4,  // 064-071 single to double
      8,  8,  8,  8, 12, 12, 12, 12,  // 072-079 single to double
     16, 16, 16, 16, 20, 20, 20, 20,  // 080-087 single to double
     24, 24, 24, 24, 28, 28, 28, 28   // 088-095 single to double
  };
  
  /**
   * Last physical register accessed by actual register.
   */
  private static final int[] lastAliasReg = {
    // 32 32-bit integer registers

     0,  1,  2,  3,  4,  5,  6,  7,  // 000-007 single to single
     8,  9, 10, 11, 12, 13, 14, 15,  // 008-015 single to single
    16, 17, 18, 19, 20, 21, 22, 23,  // 016-023 single to single
    24, 25, 26, 27, 28, 29, 30, 31,  // 024-031 single to single

    // 32 64-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

    // 16 64-bit integer registers
    
      1,  1,  3,  3,  5,  5,  7,  7,  // 064-071 single to double
      9,  9, 11, 11, 13, 13, 15, 15,  // 072-079 single to double
     17, 17, 19, 19, 21, 21, 23, 23,  // 080-087 single to double
     25, 25, 27, 27, 29, 29, 31, 31,  // 088-095 single to double

    // 8 128-bit integer registers
    
      3,  3,  3,  3,  7,  7,  7,  7,  // 064-071 single to double
     11, 11, 11, 11, 15, 15, 15, 15,  // 072-079 single to double
     19, 19, 19, 19, 23, 23, 23, 23,  // 080-087 single to double
     27, 27, 27, 27, 31, 31, 31, 31   // 088-095 single to double
  };
  
    private static final int[] actualReg = {
    // 32 32-bit integer registers

     0,  1,  2,  3,  4,  5,  6,  7,  // 000-007 single to single
     8,  9, 10, 11, 12, 13, 14, 15,  // 008-015 single to single
    16, 17, 18, 19, 20, 21, 22, 23,  // 016-023 single to single
    24, 25, 26, 27, 28, 29, 30, 31,  // 024-031 single to single

    // 32 64-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

    // 16 64-bit integer registers
    
      0,  1,  2,  3,  4,  5,  6,  7,  // 064-071 single to double
      8,  9, 10, 11, 12, 13, 14, 15,  // 072-079 single to double
     16, 17, 18, 19, 20, 21, 22, 23,  // 080-087 single to double
     24, 25, 26, 27, 28, 29, 30, 31,  // 088-095 single to double

    // 8 128-bit integer registers
    
      0,  1,  2,  3,  4,  5,  6,  7,  // 064-071 single to double
      8,  9, 10, 11, 12, 13, 14, 15,  // 072-079 single to double
     16, 17, 18, 19, 20, 21, 22, 23,  // 080-087 single to double
     24, 25, 26, 27, 28, 29, 30, 31   // 088-095 single to double
  };

  public PPCG4RegisterSet(boolean macosx)
  {
    super(actualRegisters);
    this.macosx = macosx;
  }

  /**
   * Convert a register number into its assembly language form.
   */
  public String registerName(int reg)
  {
    if (reg < actualRegisters.length)
      if (macosx)
        return regNamesMacosx[reg];
      else
        return regNamesLinux[reg];

    if (floatRegister(reg))
      return "%fv" + reg;

    return "%v" + reg;
  }

  /**
   * Return a mapping from an order index to a real register number.
   * This mapping allows the order of allocation of real registers to
   * be specified.
   */
  public short[] getPreferredOrder()
  {
    return preferredOrder;
  }

  /**
   * Return the callee saves registers.
   */
  public short[] getCalleeSaves()
  {
    if (macosx)
      return calleeSavesMacosx;
        return calleeSavesLinux;
  }

  /**
   * Return the callee uses registers.
   */
  public short[] getCalleeUses()
  {
    if (macosx)
      return calleeUsesMacosx;
        return calleeUsesLinux;
  }
  /**
   * Return the first real register that is affected when this
   * register is modified.
   */
  public final int rangeBegin(int reg)
  {
    if (reg >= actualRegisters.length)
      return reg;
    return firstAliasReg[reg];
  }

  /**
   * Return the last real register that is affected when this register
   * is modified.
   */
  public final int rangeEnd(int reg)
  {
    if (reg >= actualRegisters.length)
      return reg;
    return lastAliasReg[reg];
  }

  /**
   * For architecture where a pseudo register are mapped onto actual
   * registers, return the actual register.  An example is shown by
   * the {@link scale.backend.sparc.SparcV8RegisterSet
   * SparcV8RegisterSet} where a 64-bit <code>long long</code> register
   * is mapped onto a set of two 32-bit registers.
   */
  public int actualRegister(int reg)
  {
    if (reg >= actualRegisters.length)
      return reg;
    return actualReg[reg];
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

    if (!type.isRealType() && (bs > 4) && (bs <= 8))
      rt |= RegisterSet.DBLEREG;
    
    if (bs > 8)
      rt |= RegisterSet.QUADREG;

    return rt;
  }
  
  /**
   * Return the type with the size information added.
   * @param type is the type required
   * @param bs is the size required
   */
  public int tempRegisterType(int type, int bs)
  {
    if ((type & INTREG) != 0) {
      if (bs > 4)
        if (bs > 8)
          return type | QUADREG;
        else
          return type | DBLEREG;
      return type;
    }

    return type;
  }
}
