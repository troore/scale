package scale.backend.sparc;

import scale.backend.*;
import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.RealType;

/** 
 * This class describes the register set of the Sparc V9 using the 64-bit ABI.
 * <p>
 * $Id: SparcV9RegisterSet.java,v 1.12 2005-02-07 21:27:39 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The Sparc V9 has 96 programmable registers.  The registers 0..31
 * are used for integer values and addresses and registers 32..95 are
 * used for floating point.  The integer registers include the stack
 * pointer and other housekeeping registers.  Register 0 contains a
 * hardwired 0 value.
 * <p>
 * Registers 0 through 31 are used for 32 and 64-bit integers.
 * Registers 32 through 95 are used for floating point values.
 * Each floating point register contains 32-bits.
 * Single precision values use 32-bits from the first 32 floating point registers.
 * Double precision values use 64-bits from each floating point register pair.
 * Quad precision values use four 32-bit registers.
 * <p>
 * This register description defines a set of pseudo registers which map onto the
 * actual hardware registers.
 */

public class SparcV9RegisterSet extends SparcRegisterSet
{
  /**
   * The types of real registers on the Sparc V9. 
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

    // 32 64-bit floating point registers

    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 064-067   d0,  f1,  d2,  f3
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 068-071   d4,  f5,  d6,  f7
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 072-075   d8,  f9, d10, f11
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 076-079  d12, f13, d14, f15
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 080-083  d16, f17, d18, f19
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 084-087  d20, f21, d22, f23
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 088-091  d22, f25, d26, f27
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 092-095  d24, f29, d30, f31
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 096-099  d32, f33, d34, f35
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 100-103  d36, f37, d38, f39
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 104-107  d40, f41, d42, f43
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 108-111  d44, f45, d46, f47
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 112-115  d48, f49, d50, f51
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 116-119  d52, f53, d54, f55
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 120-123  d56, f57, d58, f59
    FLTREG + DBLEREG        , FLTREG + CNTREG        , FLTREG + DBLEREG        , FLTREG + CNTREG        , // 124-127  d60, f61, d62, f63

     // 16 128-bit floating point registers

    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 128-131   q0,  f1,  f2,  f3
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 132-135   q4,  f5,  f6,  f7
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 136-139   q8,  f9, f10, f11
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 140-143  q12, f13, f14, f15
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 144-147  q16, f17, f18, f19
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 148-151  q20, f21, f22, f23
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 152-155  q24, f25, f26, f27
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 156-159   q28, f29, f30, f31
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 160-163  q32, f33, f34, f35
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 164-167  q36, f37, f38, f39
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 168-171  q40, f41, f42, f43
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 172-175  q44, f45, f46, f47
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 176-179  q48, f49, f50, f52
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 180-183  q52, f53, f54, f55
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 184-187  q56, f57, f58, f59
    FLTREG + QUADREG        , FLTREG + CNTREG        , FLTREG + CNTREG         , FLTREG + CNTREG        , // 188-191  q60, f61, f62, f63
  };

  /**
   * The names of real registers on the Sparc V8. 
   */
  private static final String[] regNames = {
     // 32 64-bit integer registers

     "%g0",  "%g1",  "%g2",  "%g3",  "%g4",  "%g5",  "%g6",  "%g7", // 000 - 007
     "%o0",  "%o1",  "%o2",  "%o3",  "%o4",  "%o5",  "%sp",  "%o7", // 008 - 015
     "%l0",  "%l1",  "%l2",  "%l3",  "%l4",  "%l5",  "%l6",  "%l7", // 016 - 023
     "%i0",  "%i1",  "%i2",  "%i3",  "%i4",  "%i5",  "%fp",  "%i7", // 024 - 031

     // 32 32-bit floating point registers

     "%f0",  "%f1",  "%f2",  "%f3",  "%f4",  "%f5",  "%f6",  "%f7", // 032 - 039
     "%f8",  "%f9", "%f10", "%f11", "%f12", "%f13", "%f14", "%f15", // 040 - 047
    "%f16", "%f17", "%f18", "%f19", "%f20", "%f21", "%f22", "%f23", // 048 - 055
    "%f24", "%f25", "%f26", "%f27", "%f28", "%f29", "%f30", "%f31", // 056 - 063

    // 32 64-bit floating point registers

      "%d0",  "%f1",  "%d2",  "%f3",  "%d4",  "%f5",  "%d6",  "%f7", // 064-071
      "%d8",  "%f9", "%d10", "%f11", "%d12", "%f13", "%d14", "%f15", // 072-079
     "%d16", "%f17", "%d18", "%f19", "%d20", "%f21", "%d22", "%f23", // 080-087
     "%d24", "%f25", "%d26", "%f27", "%d28", "%f29", "%d30", "%f31", // 088-095
     "%d32", "%f33", "%d34", "%f35", "%d36", "%f37", "%d38", "%f39", // 096-103
     "%d40", "%f41", "%d42", "%f43", "%d44", "%f45", "%d46", "%f47", // 104-111
     "%d48", "%f49", "%d50", "%f51", "%d52", "%f53", "%d54", "%f55", // 112-119
     "%d56", "%f57", "%d58", "%f59", "%d60", "%f61", "%d62", "%f63", // 120-127

     // 16 128-bit floating point registers

      "%q0",  "%f1",  "%f2",  "%f3",  "%q4",  "%f5",  "%f6",  "%f7", // 128-135
      "%q8",  "%f9", "%f10", "%f11", "%q12", "%f13", "%f14", "%f15", // 136-143
     "%q16", "%f17", "%f18", "%f19", "%q20", "%f21", "%f22", "%f23", // 144-151
     "%q24", "%f25", "%f26", "%f27", "%q28", "%f29", "%f30", "%f31", // 152-159
     "%q32", "%f33", "%f34", "%f35", "%q36", "%f37", "%f38", "%f39", // 160-167
     "%q40", "%f41", "%f42", "%f43", "%q44", "%f45", "%f46", "%f47", // 168-175
     "%q48", "%f49", "%f50", "%f51", "%q52", "%f53", "%f54", "%f55", // 176-183
     "%q56", "%f57", "%f58", "%f59", "%q50", "%f61", "%f62", "%f63", // 184-191
  };

  /**
   * First physical register accessed by actual register.
   */
  private static final int[] firstAliasReg = {
    // 32 64-bit integer registers

     0,  1,  2,  3,  4,  5,  6,  7,  // 000-007 single to single
     8,  9, 10, 11, 12, 13, 14, 15,  // 008-015 single to single
    16, 17, 18, 19, 20, 21, 22, 23,  // 016-023 single to single
    24, 25, 26, 27, 28, 29, 30, 31,  // 024-031 single to single

     // 32 32-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

     // 32 64-bit floating point registers

    32, 32, 34, 34, 36, 36, 38, 38,  // 064-071 double to single
    40, 40, 42, 42, 44, 44, 46, 46,  // 072-079 double to single
    48, 48, 50, 50, 52, 52, 54, 54,  // 080-087 double to single
    56, 56, 58, 58, 60, 60, 62, 62,  // 088-095 double to single
    64, 64, 66, 66, 68, 68, 70, 70,  // 096-103 double to single
    72, 72, 74, 74, 76, 76, 78, 78,  // 104-111 double to single
    80, 80, 82, 82, 84, 84, 86, 86,  // 112-119 double to single
    88, 88, 90, 90, 92, 92, 94, 94,  // 120-127 double to single

     // 16 128-bit floating point registers

    32, 32, 32, 32, 36, 36, 36, 36,  // 128-135 quad to single
    40, 40, 40, 40, 44, 44, 44, 44,  // 136-143 quad to single
    48, 48, 48, 48, 52, 52, 52, 52,  // 144-151 quad to single
    56, 56, 56, 56, 60, 60, 60, 60,  // 152-159 quad to single
    64, 64, 64, 64, 68, 68, 68, 68,  // 160-167 quad to single
    72, 72, 72, 72, 76, 76, 76, 76,  // 168-175 quad to single
    80, 80, 80, 80, 84, 84, 84, 84,  // 176-183 quad to single
    88, 88, 88, 88, 92, 92, 92, 92,  // 184-191 quad to single
  };

  /**
   * Last physical register accessed by actual register.
   */
  private static final int[] lastAliasReg = {
    // 32 64-bit integer registers

     0,  1,  2,  3,  4,  5,  6,  7,  // 000-007 single to single
     8,  9, 10, 11, 12, 13, 14, 15,  // 008-015 single to single
    16, 17, 18, 19, 20, 21, 22, 23,  // 016-023 single to single
    24, 25, 26, 27, 28, 29, 30, 31,  // 024-031 single to single

     // 32 32-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 032-039 single to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 040-047 single to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 048-055 single to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 056-063 single to single

     // 32 64-bit floating point registers

    33, 33, 35, 35, 37, 37, 39, 39,  // 064-071 double to single
    41, 41, 43, 43, 45, 45, 47, 47,  // 072-079 double to single
    49, 49, 51, 51, 53, 53, 55, 55,  // 080-087 double to single
    57, 57, 59, 59, 61, 61, 63, 63,  // 088-095 double to single
    65, 65, 67, 67, 69, 69, 71, 71,  // 096-103 double to single
    73, 73, 75, 75, 77, 77, 79, 79,  // 104-111 double to single
    81, 81, 83, 83, 85, 85, 87, 87,  // 112-119 double to single
    89, 89, 91, 91, 93, 93, 95, 95,  // 120-127 double to single

     // 16 128-bit floating point registers

    35, 35, 35, 35, 39, 39, 39, 39,  // 128-135 quad to single
    43, 43, 43, 43, 47, 47, 47, 47,  // 136-143 quad to single
    51, 51, 51, 51, 55, 55, 55, 55,  // 144-151 quad to single
    59, 59, 59, 59, 63, 63, 63, 63,  // 152-159 quad to single
    67, 67, 67, 67, 71, 71, 71, 71,  // 160-167 quad to single
    75, 75, 75, 75, 79, 79, 79, 79,  // 168-175 quad to single
    83, 83, 83, 83, 87, 87, 87, 87,  // 176-183 quad to single
    91, 91, 91, 91, 95, 95, 95, 95,  // 184-191 quad to single
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

    // 32 64-bit double floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 064-071 double to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 072-079 double to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 080-087 double to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 088-095 double to single
    64, 65, 66, 67, 68, 69, 70, 71,  // 096-103 double to single
    72, 73, 74, 75, 76, 77, 78, 79,  // 104-111 double to single
    80, 81, 82, 83, 84, 85, 86, 87,  // 112-119 double to single
    88, 89, 90, 91, 92, 93, 94, 95,  // 120-127 double to single

     // 16 128-bit floating point registers

    32, 33, 34, 35, 36, 37, 38, 39,  // 128-135 quad to single
    40, 41, 42, 43, 44, 45, 46, 47,  // 136-143 quad to single
    48, 49, 50, 51, 52, 53, 54, 55,  // 144-151 quad to single
    56, 57, 58, 59, 60, 61, 62, 63,  // 152-159 quad to single
    64, 65, 66, 67, 68, 69, 70, 71,  // 160-167 quad to single
    72, 73, 74, 75, 76, 77, 78, 79,  // 168-175 quad to single
    80, 81, 82, 83, 84, 85, 86, 87,  // 176-183 quad to single
    88, 89, 90, 91, 92, 93, 94, 95,  // 184-191 quad to single
  };

  /**
   * The preferred order in which registers should be allocated.
   * This array must be the same length as actualRegisters.
   */
  private static final short[] preferredOrder = {
    // 25 64-bit integer registers

     8,   9,  10,  11,  12,  13,  15,
     1,   2,   3,   4,
    16,  17,  18,  19,  20,  21,  22,  23,
    24,  25,  26,  27,  28,  29,

     // 32 32-bit floating point registers

    32,  33,  34,  35,  36,  37,  38,  39,
    40,  41,  42,  43,  44,  45,  46,  47,
    48,  49,  50,  51,  52,  53,  54,  55,
    56,  57,  58,  59,  60,  61,  62,  63,

     // 32 64-bit integer registers

    64,  66,  68,  70,  72,  74,  76,  78,
    80,  82,  84,  86,  88,  90,  92,  94,
    96,  98, 100, 102, 104, 106, 108, 110,
   112, 114, 116, 118, 120, 122, 124, 126,

     // 16 128-bit floating point registers

   160, 164, 168, 172, 176, 180, 184, 188,
   192, 196, 200, 204, 208, 212, 216, 220

//   0,   5,   6,   7,  14,  30,  31,
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

    64,  65,  66,  67,  68,  69,  70,  71,
    72,  73,  74,  75,  76,  77,  78,  79,
    80,  81,  82,  83,  84,  85,  86,  87,
    88,  89,  90,  91,  92,  93,  94,  95,
  };

  /**
   * The registers that a callee must save and restore if they are used by the callee.
   */
  private static final short[] calleeSaves = {};

  public SparcV9RegisterSet()
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
  public int rangeBegin(int reg)
  {
    if (reg >= actualRegisters.length)
      return reg;
    return firstAliasReg[reg];
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
   * Return the last real register that is affected when this register is modified.
   */
  public int rangeEnd(int reg)
  {
    if (reg >= actualRegisters.length)
      return reg;
    return lastAliasReg[reg];
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

    return type;
  }
}
