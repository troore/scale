package scale.backend.x86;

import scale.backend.*;
import scale.clef.type.Type;
import scale.clef.type.AggregateType;

/** 
 * This class describes the register set of the X86 architecture.
 * <p>
 * $Id: X86RegisterSet.java,v 1.1 2007-11-01 16:52:30 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents the register set for the specific
 * architecture.  Most of the methods below generate a "not
 * implemented error".  These must be replaced with code that is
 * specific to the register set of the architecture.  The other
 * methods may need to be modified.  Use the register set classes from
 * the other architectures to help you understand how to make changes
 * for your architecture.
 * @see scale.backend.RegisterSet
 * @see scale.backend.alpha.AlphaRegisterSet
 * @see scale.backend.sparc.SparcRegisterSet
 * @see scale.backend.ppc.PPCRegisterSet
 */

public class X86RegisterSet extends scale.backend.RegisterSet
{
  public static final int EAX =  0;
  public static final int AX  =  1;
  public static final int AH  =  2;
  public static final int AL  =  3;
  public static final int EBX =  4;
  public static final int BX  =  5;
  public static final int BH  =  6;
  public static final int BL  =  7;
  public static final int ECX =  8;
  public static final int CX  =  9;
  public static final int CH  = 10;
  public static final int CL  = 11;
  public static final int EDX = 12;
  public static final int DX  = 13;
  public static final int DH  = 14;
  public static final int DL  = 15;
  public static final int EBP = 16;
  public static final int BP  = 17;
  public static final int ESI = 18;
  public static final int SI  = 19;
  public static final int EDI = 20;
  public static final int DI  = 21;
  public static final int ESP = 22;
  public static final int SP  = 23;
  public static final int CS  = 24;
  public static final int DS  = 25;
  public static final int SS  = 26;
  public static final int ES  = 27;
  public static final int FS  = 28;
  public static final int GS  = 29;
  public static final int CR0 = 30;
  public static final int CR1 = 31;
  public static final int CR2 = 32;
  public static final int CR3 = 33;
  public static final int DR0 = 34;
  public static final int DR1 = 35;
  public static final int DR2 = 36;
  public static final int DR3 = 37;
  public static final int DR4 = 38;
  public static final int DR5 = 39;
  public static final int DR6 = 40;
  public static final int DR7 = 41;
  public static final int EFLAGS = 42;

  private static final short[] actualRegisters = {
    AIREG, AIREG + RDREG, AIREG + RDREG, AIREG + RDREG, // EAX, AX, AH, AL
    AIREG, AIREG + RDREG, AIREG + RDREG, AIREG + RDREG, // EBX, BX, BH, BL
    AIREG, AIREG + RDREG, AIREG + RDREG, AIREG + RDREG, // ECX, CX, CH, CL
    AIREG, AIREG + RDREG, AIREG + RDREG, AIREG + RDREG, // EDX, DX, DH, DL
    AIREG + RDREG, AIREG + RDREG,                       // EBP, BP
    AIREG, AIREG + RDREG,                               // ESI, SI
    AIREG, AIREG + RDREG,                               // EDI, DI
    AIREG + RDREG, AIREG + RDREG,                       // ESP, SP
    AIREG + RDREG,                                      // CS
    AIREG + RDREG,                                      // DS
    AIREG + RDREG,                                      // SS
    AIREG + RDREG,                                      // ES
    AIREG + RDREG,                                      // FS
    AIREG + RDREG,                                      // GS
    AIREG + RDREG,                                      // CR0
    AIREG + RDREG,                                      // CR1
    AIREG + RDREG,                                      // CR2
    AIREG + RDREG,                                      // CR3
    AIREG + RDREG,                                      // DR0
    AIREG + RDREG,                                      // DR1
    AIREG + RDREG,                                      // DR2
    AIREG + RDREG,                                      // DR3
    AIREG + RDREG,                                      // DR4
    AIREG + RDREG,                                      // DR5
    AIREG + RDREG,                                      // DR6
    AIREG + RDREG,                                      // DR7
    AIREG + RDREG,                                      // EFLAGS
  };

  private static final short[] actualReg = {
     0,  0,  0,  0, // EAX, AX, AH, AL
     4,  4,  4,  4, // EBX, BX, BH, BL
     8,  8,  8,  8, // ECX, CX, CH, CL
    12, 12, 12, 12, // EDX, DX, DH, DL
    16, 16,         // EBP, BP
    18, 18,         // ESI, SI
    20, 20,         // EDI, DI
    22, 22,         // ESP, SP
    24, 25, 26, 27, // CS, DS, SS, ES,
    28, 29,         // FS, GS
    30, 31, 32, 33, // CR0, CR1, CR2, CR3
    34, 35, 36, 37, // DR0, DR1, DR2, DR3,
    38, 39, 40, 41, // DR4, DR5, DR6, DR7
    42,             // EFLAGS
  };

  private static final short[] preferredOrder = {EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP};

  private static final short[] calleeUses = { };

  private static final short[] calleeSaves = { };

  private static final String[] regNames = {
    "%EAX", "%AX", "%AH", "%AL",
    "%EBX", "%BX", "%BH", "%BL",
    "%ECX", "%CX", "%CH", "%CL",
    "%EDX", "%DX", "%DH", "%DL",
    "%EBP", "%BP",
    "%ESI", "%SI",
    "%EDI", "%DI",
    "%ESP", "%SP",
    "%CS",
    "%DS",
    "%SS",
    "%ES",
    "%FS",
    "%GS",
    "%CR0",
    "%CR1",
    "%CR2",
    "%CR3",
    "%DR0",
    "%DR1",
    "%DR2",
    "%DR3",
    "%DR4",
    "%DR5",
    "%DR6",
    "%DR7",
    "%EFLAGS",
  };

  public X86RegisterSet()
  {
    super(actualRegisters);
    assert (actualRegisters.length == regNames.length) : "regNames table incompatable.";
    assert (actualRegisters.length == actualReg.length) :"actualReg table incompatable.";
  }

  public String registerName(int reg)
  {
    if (reg < actualRegisters.length)
      return regNames[reg];

    if (floatRegister(reg))
      return "$fv" + reg;

    return "$v" + reg;
  }

  public short[] getPreferredOrder()
  {
    return preferredOrder;
  }

  public boolean useContiguous()
  {
    return false;
  }

  public int registerSize(int reg)
  {
    return ((getType(reg) & sizeMask) + 1) * 4;
  }

  public int actualRegister(int reg)
  {
    if (reg >= actualRegisters.length)
      return reg;
    return actualReg[reg];
  }

  public short[] getCalleeSaves()
  {
    return calleeSaves;
  }

  public short[] getCalleeUses()
  {
    return calleeUses;
  }

  public int numAllocatableRegisters()
  {
    return 8;
  }

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
