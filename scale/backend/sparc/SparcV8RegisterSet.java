package scale.backend.sparc;

import scale.backend.*;
import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.RealType;

/** 
 * This class describes the register set of the Sparc V8  using the 32-bit ABI.
 * <p>
 * $Id: SparcV8RegisterSet.java,v 1.18 2005-02-07 21:27:39 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The Sparc V8 has 64 programmable registers.  The registers 0..31
 * are used for integer values and addresses and registers 32..63 are
 * used for floating point.  The integer registers include the stack
 * pointer and other housekeeping registers.  Register 0 contains a
 * hardwired 0 value.
 * <p>
 * Registers 0 through 31  are used for 32-bit integers.
 * Registers 32 through 63 are used for single precision floating point values.
 * Registers 64 through 63 are used for double precision floating point values.
 * Registers 64 through 63 are used for quad precision floating point values.
 * <p>
 * This register description defines a set of pseudo registers which map onto the
 * actual hardware registers.
 */

public class SparcV8RegisterSet extends SparcRegisterSet
{
  /**
   * 64-bit integer register %o0
   */
  public static final int LO0_REG =  136;
  /**
   * 64-bit integer register %i0
   */
  public static final int LI0_REG =  152;

  /**
   * The types of real registers on the Sparc V8. 
   */
  private static final short[] actualRegisters = {
    // 32 32-bit integer registers

    AIREG + RDREG          , AIREG                   , AIREG                   , AIREG                  , // 000-003   g0,  g1,  g2,  g3
    AIREG                  , AIREG + RDREG           , AIREG + RDREG           , AIREG + RDREG          , // 004-007   g4,  g5,  g6,  g7
    AIREG                  , AIREG                   , AIREG                   , AIREG                  , // 008-011   o0,  o1,  o2,  o3
    AIREG                  , AIREG                   , AIREG + RDREG           , AIREG                  , // 012-015   o4,  o5,  o6,  o7
    AIREG                  , AIREG                   , AIREG                   , AIREG                  , // 016-019   l0,  l1,  l2,  l3
    AIREG                  , AIREG                   , AIREG                   , AIREG                  , // 020-023   l4,  l5,  l6,  l7
    AIREG                  , AIREG                   , AIREG                   , AIREG                  , // 024-027   i0,  i1,  i2,  i3
    AIREG                  , AIREG                   , AIREG + RDREG           , AIREG                  , // 028-031   i4,  i5,  i6,  i7

     // 32 32-bit floating point registers

    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 032-035   f0,  f1,  f2,  f3
    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 036-039   f4,  f5,  f6,  f7
    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 040-043   f8,  f9, f10, f11
    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 044-047  f12, f13, f14, f15
    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 048-051  f16, f17, f18, f19
    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 052-055  f20, f21, f22, f23
    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 056-059  f24, f25, f26, f27
    FLTREG                 , FLTREG                  , FLTREG                  , FLTREG                 , // 060-063  f28, f29, f30, f31

    // 16 64-bit floating point registers

    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 064-067   d0,  f1,  d2,  f3
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 068-071   d4,  f5,  d6,  f7
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 072-075   d8,  f9, d10, f11
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 076-079  d12, f13, d14, f15
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 080-083  d16, f17, d18, f19
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 084-087  d20, f21, d22, f23
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 088-091  d22, f25, d26, f27
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 092-095  d24, f29, d30, f31

     // 8 128-bit floating point registers

    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 096-099   q0,  f1,  f2,  f3
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 100-103   q4,  f5,  f6,  f7
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 104-107   q8,  f9, f10, f11
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 108-111  q12, f13, f14, f15
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 112-115  q16, f17, f18, f19
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 116-119  q20, f21, f22, f23
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 120-123  q24, f25, f26, f27
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 124-127  q28, f29, f30, f31

     // 16 64-bit integer point registers

    AIREG  + DBLEREG + RDREG, AIREG  + CNTREG + RDREG, AIREG  + DBLEREG        , AIREG  + CNTREG        , // 128-131   g0,  g1,  g2,  g3
    AIREG  + DBLEREG + RDREG, AIREG  + CNTREG + RDREG, AIREG  + DBLEREG + RDREG, AIREG  + CNTREG + RDREG, // 132-135   g4,  g5,  g6,  g7
    AIREG  + DBLEREG        , AIREG  + CNTREG        , AIREG  + DBLEREG        , AIREG  + CNTREG        , // 136-139   o0,  o1,  o2,  o3
    AIREG  + DBLEREG        , AIREG  + CNTREG        , AIREG  + DBLEREG + RDREG, AIREG  + CNTREG        , // 140-143   o4,  o5,  o6,  o7
    AIREG  + DBLEREG        , AIREG  + CNTREG        , AIREG  + DBLEREG        , AIREG  + CNTREG        , // 144-147   l0,  l1,  l2,  l3
    AIREG  + DBLEREG        , AIREG  + CNTREG        , AIREG  + DBLEREG        , AIREG  + CNTREG        , // 148-151   l4,  l5,  l6,  l7
    AIREG  + DBLEREG        , AIREG  + CNTREG        , AIREG  + DBLEREG        , AIREG  + CNTREG        , // 152-155   i0,  i1,  i2,  i3
    AIREG  + DBLEREG        , AIREG  + CNTREG        , AIREG  + DBLEREG + RDREG, AIREG  + CNTREG        , // 156-159   i4,  i5,  i6,  i7
  };

  /**
   * The names of real registers on the Sparc V8. 
   */
  private static final String[] regNames = {
    // 32 32-bit integer registers

     "%g0",  "%g1",  "%g2",  "%g3",  "%g4",  "%g5",  "%g6",  "%g7", // 000 - 007
     "%o0",  "%o1",  "%o2",  "%o3",  "%o4",  "%o5",  "%sp",  "%o7", // 008 - 015
     "%l0",  "%l1",  "%l2",  "%l3",  "%l4",  "%l5",  "%l6",  "%l7", // 016 - 023
     "%i0",  "%i1",  "%i2",  "%i3",  "%i4",  "%i5",  "%fp",  "%i7", // 024 - 031

     // 32 32-bit loating point registers

     "%f0",  "%f1",  "%f2",  "%f3",  "%f4",  "%f5",  "%f6",  "%f7", // 032 - 039
     "%f8",  "%f9", "%f10", "%f11", "%f12", "%f13", "%f14", "%f15", // 040 - 047
    "%f16", "%f17", "%f18", "%f19", "%f20", "%f21", "%f22", "%f23", // 048 - 055
    "%f24", "%f25", "%f26", "%f27", "%f28", "%f29", "%f30", "%f31", // 056 - 063


    // 16 64-bit double floating point registers

      "%d0",  "%f1",  "%d2",  "%f3",  "%d4",  "%f5",  "%d6",  "%f7", // 064-071
      "%d8",  "%f9", "%d10", "%f11", "%d12", "%f13", "%d14", "%f15", // 072-079
     "%d16", "%f17", "%d18", "%f19", "%d20", "%f21", "%d22", "%f23", // 080-087
     "%d24", "%f25", "%d26", "%f27", "%d28", "%f29", "%d30", "%f31", // 088-095

     // 8 128-bit floating point registers

      "%q0",  "%f1",  "%f2",  "%f3",  "%q4",  "%f5",  "%f6",  "%f7", // 096-103
      "%q8",  "%f9", "%f10", "%f11", "%q12", "%f13", "%f14", "%f15", // 104-111
     "%q16", "%f17", "%f18", "%f19", "%q20", "%f21", "%f22", "%f23", // 112-119
     "%q24", "%f25", "%f26", "%f27", "%q28", "%f29", "%f30", "%f31", // 120-127

     // 16 64-bit integer registers

      "%g0",  "%g1",  "%g2",  "%g3",  "%g4",  "%g5",  "%g6",  "%g7", // 128-135
      "%o0",  "%o1",  "%o2",  "%o3",  "%o4",  "%o5",  "%sp",  "%o7", // 136-143
      "%l0",  "%l1",  "%l2",  "%l3",  "%l4",  "%l5",  "%l6",  "%l7", // 144-151
      "%i0",  "%i1",  "%i2",  "%i3",  "%i4",  "%i5",  "%fp",  "%i7", // 152-159
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

     // 32 32-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

    // 16 64-bit double floating point registers

    32, 32, 34, 34, 36, 36, 38, 38,  // 064-071 double to single
    40, 40, 42, 42, 44, 44, 46, 46,  // 072-079 double to single
    48, 48, 50, 50, 52, 52, 54, 54,  // 080-087 double to single
    56, 56, 58, 58, 60, 60, 62, 62,  // 088-095 double to single

     // 8 128-bit floating point registers

    32, 32, 32, 32, 36, 36, 36, 36,  // 096-103 quad to single
    40, 40, 40, 40, 44, 44, 44, 44,  // 104-111 quad to single
    48, 48, 48, 48, 52, 52, 52, 52,  // 112-119 quad to single
    56, 56, 56, 56, 60, 60, 60, 60,  // 120-127 quad to single

     // 16 64-bit integer registers

     0,  0,  2,  2,  4,  4,  6,  6,  // 128-135 double to single
     8,  8, 10, 10, 12, 12, 14, 14,  // 136-143 double to single
    16, 16, 18, 18, 20, 20, 22, 22,  // 144-151 double to single
    24, 24, 26, 26, 28, 28, 30, 30,  // 152-159 double to single
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

     // 32 32-bit loating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

    // 16 64-bit double floating point registers

    33, 33, 35, 35, 37, 37, 39, 39,  // 064-071 double to single
    41, 41, 43, 43, 45, 45, 47, 47,  // 072-079 double to single
    49, 49, 51, 51, 53, 53, 55, 55,  // 080-087 double to single
    57, 57, 59, 59, 61, 61, 63, 63,  // 088-095 double to single

     // 8 128-bit floating point registers

    35, 35, 35, 35, 39, 39, 39, 39,  // 096-103 quad to single
    43, 43, 43, 43, 47, 47, 47, 47,  // 104-111 quad to single
    51, 51, 51, 51, 55, 55, 55, 55,  // 112-119 quad to single
    59, 59, 59, 59, 63, 63, 63, 63,  // 120-127 quad to single

     // 16 64-bit integer registers

     1,  1,  3,  3,  5,  5,  7,  7,  // 128-135 double to single
     9,  9, 11, 11, 13, 13, 15, 15,  // 136-143 double to single
    17, 17, 19, 19, 21, 21, 23, 23,  // 144-151 double to single
    25, 25, 27, 27, 29, 29, 31, 31,  // 152-159 double to single
  };

  /**
   * The actual hardware register.
   */
  private static final int[] actualReg = {
    // 32 32-bit integer registers

     0,  1,  2,  3,  4,  5,  6,  7,  // 000-007 single to single
     8,  9, 10, 11, 12, 13, 14, 15,  // 008-015 single to single
    16, 17, 18, 19, 20, 21, 22, 23,  // 016-023 single to single
    24, 25, 26, 27, 28, 29, 30, 31,  // 024-031 single to single

     // 32 32-bit loating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

    // 16 64-bit double floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 064-071 double to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 072-079 double to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 080-087 double to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 088-095 double to single

     // 8 128-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 096-103 quad to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 104-111 quad to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 112-119 quad to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 120-127 quad to single

     // 16 64-bit integer registers

     0,  1,  2,  3,  4,  5,  6,  7,  // 128-135 double to single
     8,  9, 10, 11, 12, 13, 14, 15,  // 136-143 double to single
    16, 17, 18, 19, 20, 21, 22, 23,  // 144-151 double to single
    24, 25, 26, 27, 28, 29, 30, 31,  // 152-159 double to single
  };

  /**
   * The preferred order in which registers should be allocated.
   * This array must be the same length as actualRegisters.
   */
  private static final short[] preferredOrder = {
    // 25 32-bit integer registers

     8,   9,  10,  11,  12,  13,  15,
     1,   2,   3,   4,
    16,  17,  18,  19,  20,  21,  22,  23,
    24,  25,  26,  27,  28,  29,

    // 32 32-bit floating point registers

    32,  33,  34,  35,  36,  37,  38,  39,
    40,  41,  42,  43,  44,  45,  46,  47,
    48,  49,  50,  51,  52,  53,  54,  55,
    56,  57,  58,  59,  60,  61,  62,  63,

    // 16 64-bit integer registers

    64,  66,  68,  70,  72,  74,  76,  78,
    80,  82,  84,  86,  88,  90,  92,  94,

    // 8 128-bit floating point registers

    96, 100, 104, 108, 112, 116, 120, 124,
 
    // 16 64-bit integer point registers

    128, 130, 132, 134, 136, 138, 140, 142,
    144, 146, 148, 150, 152, 154, 156, 158
 };

  /**
   * The registers that a callee can use without saving and restoring.
   */
  private static final short[] calleeUses = {
          1,   2,   3,   4,
     8,   9,  10,  11,  12,  13,  14,  15,

    32,  33,  34,  35,  36,  37,  38,  39,
    40,  41,  42,  43,  44,  45,  46,  47,
    48,  49,  50,  51,  52,  53,  54,  55,
    56,  57,  58,  59,  60,  61,  62,  63,
  };

  /**
   * The registers that a callee must save and restore if they are used by the callee.
   */
  private static final short[] calleeSaves = {};

  public SparcV8RegisterSet()
  {
    super(actualRegisters);

    assert (actualRegisters.length == regNames.length) : "regNames table incompatable.";
    assert (actualRegisters.length == firstAliasReg.length) :"firstAliasReg table incompatable.";
    assert (actualRegisters.length == lastAliasReg.length) : "lastAliasReg tables incompatable.";
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
   * Return the first real register that is affected when this register is modified.
   */
  public final int rangeBegin(int reg)
  {
    if (reg >= actualRegisters.length)
      return reg;
    return firstAliasReg[reg];
  }

  /**
   * Return the last real register that is affected when this register is modified.
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
   * Return the type with the size information added.
   * @param type is the type required
   * @param bs is the size required
   */
  public int tempRegisterType(int type, int bs)
  {
    if ((type & FLTREG) != 0) {
      if (bs > 8)
        return type | QUADREG;
      if (bs > 4)
        return type | DBLEREG;
      return type;
    }

    if ((type & INTREG) != 0) {
      if (bs > 8)
        return type | QUADREG;
      if (bs > 4)
        return type | DBLEREG;
      return type;
    }

    return type;
  }
}
