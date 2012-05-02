package scale.backend;

import scale.common.*;
import scale.clef.type.Type;

/** 
 * This is the base class for describing the register set of the machine.
 * <p>
 * $Id: RegisterSet.java,v 1.35 2007-10-04 19:57:49 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class RegisterSet
{
  /**
   * Register spans two contiguous registers.
   */
  public static final short DBLEREG = 0x01;
  /**
   * Register spans four contiguous registers.
   */
  public static final short QUADREG = 0x03;
  /**
   * Register may be used for integer values.
   */
  public static final short INTREG = 0x04;
  /**
   * Register may be used for floating point value.
   */
  public static final short FLTREG = 0x08;
  /**
   * Register may be used for addresses.
   */
  public static final short ADRREG = 0x10;
  /**
   * Register may be used for special information.
   */
  public static final short SPCREG = 0x20;
  /**
   * Register is read-only.
   */
  public static final short RDREG = 0x40;
  /**
   * Register is first register of a two register pair.  A PAIRREG
   * differs from a DBLEREG in that no <i>real</i> register is a pair
   * register.  PAIRREG is used to represent two-values that are
   * treated as one such as a complex value.  Virtual registers may be
   * paired registers.  As such, they must have consecutive virtual
   * register numbers.  The lower numbered virtual register is used to
   * refer to the pair.  The second virtual register DOES NOT have the
   * PAIRREG attribute.  PAIRREG is not used when checking for
   * register compatibility.
   */
  public static final short PAIRREG = 0x100;
  /**
   * Register is a continuation of another register.
   */
  public static final short CNTREG = 0x200;
  /**
   * Register may be used for floating point or integer values.
   */
  public static final short FIREG = INTREG + FLTREG;
  /**
   * Register may be used for address or integer values.
   */
  public static final short AIREG = INTREG + ADRREG;
  /**
   * Register may be used for address, floating point or integer values.
   */
  public static final short AFIREG = INTREG + FLTREG + ADRREG;
  /**
   * Mask out the size information from the register attributes.
   */
  public static final short sizeMask  = QUADREG;
  /**
   * Mask out the type information from the register attributes.
   */
  public static final short typeMask  = INTREG + FLTREG + ADRREG + SPCREG;
  /**
   * The sizeMask + RDREG.
   */
  public static final short checkMask = sizeMask + RDREG;
  /**
   * Index of next virtual register.
   */
  protected int nextRegister;
  /**
   * Indexed by register number.
   */
  protected short[] registers;

  private short[] actualRegisters; // Indexed by register number. Read Only.
  private int     useThisReg = -1; // The register to use as the next result register.

  /**
   * @param actualRegisters is a list of the types of the actual
   * registers of the machine that specifies how each real register
   * may be used
   */
  public RegisterSet(short[] actualRegisters)
  {
    this.actualRegisters = actualRegisters;
  }

  /**
   * Allocate a new virtual/temporary register.
   * @param type specifies the type of register
   * @return the register number of the temporary register
   */
  public int newTempRegister(int type)
  {
    assert (type > 0) : "Unknown register type " + type;

    int rr  = nextRegister;
    int nr  = (type & sizeMask) + 1;
    int nar = nr;
    if ((type & PAIRREG) != 0)
      nar *= 2;

    if ((nextRegister + nar) >= registers.length) {
      short[] nreg = new short[nextRegister + 256];
      System.arraycopy(registers, 0, nreg, 0, registers.length);
      registers = nreg;
    }
    registers[nextRegister++] = (short) type;
    if (nar == 1) 
      return rr;

    // Set proper flags for continue registers.

    short nt1 = (short) (CNTREG | (type & ~(sizeMask | PAIRREG)));
    short nt2 = (short) (type & ~PAIRREG);
    for (int i = 1; i < nar; i++)
      registers[nextRegister++] = ((i % nr) == 0) ? nt2 : nt1;

    return rr;
  }

  /**
   * Return a register to be used as the result register.
   * @see #setResultRegister
   */
  public int getResultRegister(int type)
  {
    if ((useThisReg >= 0) && compatibleType(registers[useThisReg], type)) {
      int reg = useThisReg;
      useThisReg = -1;
      return reg;
    }
    useThisReg = -1;
    return newTempRegister(type);
  }

  /**
   * Specify a register to be used for the next result register.
   * This register will be used if possible.
   * @see #getResultRegister
   */
  public void setResultRegister(int reg)
  {
    useThisReg = reg;
  }
    
  /**
   * Return the register attributes.
   * This information must be specified.
   */
  public int getType(int register)
  {
    return registers[register];
  }

  /**
   * Initialize for a new procedure.
   */
  public void initialize()
  {
    nextRegister = actualRegisters.length;
    registers    = new short[this.nextRegister + 256];

    System.arraycopy(actualRegisters, 0, this.registers, 0, this.nextRegister);
  }

  /**
   * Return the number of registers currently defined.
   */
  public int numRegisters()
  {
    return nextRegister;
  }

  /**
   * Return the number of addressable registers.
   * This includes registers that map to other registers.
   * @return the number of addressable registers
   */
  public int numRealRegisters()
  {
    return actualRegisters.length;
  }

  /**
   * Return the number of unique registers that can hold programmer
   * values.
   * @return the number of unique registers that can hold programmer
   * values.
   */
  public abstract int numAllocatableRegisters();

  /**
   * Return a mapping from an order index to a real register number.
   * This mapping allows the order of allocation of real registers to
   * be specified.  This method SHOULD BE overridden by derived
   * classes.
   */
  public short[] getPreferredOrder()
  {
    int   l     = actualRegisters.length;
    short[] map = new short[l];
    for (int i = 0; i < l; i++)
      map[i] = (short) i;

    return map;
  }

  /**
   * Return true if virtual registers, that require more than one real
   * register, must be allocated to contiguous real registers.
   */
  public boolean useContiguous()
  {
    return true;
  }

  /**
   * Return true if the register is a virtual register.
   */
  public final boolean tempRegister(int reg)
  {
    return (reg >= actualRegisters.length);
  }

  /**
   * Return true if the register contains floating point value.
   */
  public final boolean floatRegister(int reg)
  {
    return (0 != (registers[reg] & FLTREG));
  }

  /**
   * Return true if the register type allows floating point values.
   */
  public final boolean isFloatType(int regType)
  {
    return (0 != (regType & FLTREG));
  }

  /**
   * Return true if the register contains integer value.
   */
  public final boolean intRegister(int reg)
  {
    return (0 != (registers[reg] & INTREG));
  }

  /**
   * Return true if the register type alllows integers.
   */
  public final boolean isIntType(int regType)
  {
    return (0 != (regType & INTREG));
  }

  /**
   * Return true if the register contains address value.
   */
  public final boolean adrRegister(int reg)
  {
    return (0 != (registers[reg] & ADRREG));
  }

  /**
   * Return true if the register type allows addresses.
   */
  public final boolean isAdrType(int regType)
  {
    return (0 != (regType & ADRREG));
  }

  /**
   * Return true if the register contains special values such as a
   * status register.
   */
  public final boolean specialRegister(int reg)
  {
    return (0 != (registers[reg] & SPCREG));
  }

  /**
   * Return true if the register type is for special values such as a
   * status register.
   */
  public final boolean isSpecialType(int regType)
  {
    return (0 != (regType & SPCREG));
  }

  /**
   * Return true if the register can only be read.
   */
  public final boolean readOnlyRegister(int reg)
  {
    return (0 != (registers[reg] & RDREG));
  }

  /**
   * Return true if the register can only be read.
   */
  public final boolean isReadOnlyType(int regType)
  {
    return (0 != (regType & RDREG));
  }

  /**
   * Return true if the register is first of a two register pair.
   */
  public final boolean pairRegister(int reg)
  {
    return (0 != (registers[reg] & PAIRREG));
  }

  /**
   * Return true if the register type is for the first of a two
   * register pair.
   */
  public final boolean isPairType(int regType)
  {
    return (0 != (regType & PAIRREG));
  }

  /**
   * Return true if the register is a virtual register.
   */
  public final boolean virtualRegister(int reg)
  {
    return (reg >= actualRegisters.length);
  }

  /**
   * Return true if the register is a continuation of a multi-register
   * register.
   */
  public final boolean continueRegister(int reg)
  {
    return (0 != (registers[reg] & CNTREG));
  }

  /**
   * Return true if the register type is for a continuation of a
   * multi-register register.
   */
  public final boolean isContinueType(int regType)
  {
    return (0 != (regType & CNTREG));
  }

  /**
   * Return true if the register is a double register.
   */
  public final boolean doubleRegister(int reg)
  {
    return (DBLEREG == (registers[reg] & sizeMask));
  }

  /**
   * Return true if the register type is for a double register.
   */
  public final boolean isDoubleType(int regType)
  {
    return (DBLEREG == (regType & sizeMask));
  }

  /**
   * Return true if the register is a quad register.
   */
  public final boolean quadRegister(int reg)
  {
    return (QUADREG == (registers[reg] & sizeMask));
  }

  /**
   * Return true if the register type is for a quad register.
   */
  public final boolean isQuadType(int regType)
  {
    return (QUADREG == (regType & sizeMask));
  }

  /**
   * Return true if the register is has specified bit set.
   */
  public final boolean modRegister(int reg, int bit)
  {
    return (0 != (registers[reg] & bit));
  }

  /**
   * Return true if the register type has has specified bit set.
   */
  public final boolean isModType(int regType, int bit)
  {
    return (0 != (regType & bit));
  }

  /**
   * Return the first real register that is affected when this
   * register is modified.  This method will be shadowed by a machine
   * specific register definition class for a machine that catenates
   * multiple registers to provide a higher precision register.
   */
  public int rangeBegin(int reg)
  {
    return reg;
  }

  /**
   * Return the last real register that is affected when this register
   * is modified.  This method will be shadowed by a machine specific
   * register definition class for a machine that catenates multiple
   * registers to provide a higher precision register.
   */
  public int rangeEnd(int reg)
  {
    return reg;
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
    return reg;
  }

  /**
   * Return true if the real register is able to hold part of the virtual register.
   */
  public final boolean compatibleNS(int realReg, int virtualReg)
  {
    int virtualType = registers[virtualReg];
    int realType    = registers[realReg];
    return (((realType & typeMask & virtualType) != 0) &&
            ((realType & RDREG) == (virtualType & RDREG)));
  }

  /**
   * Return true if the real register is able to hold the virtual
   * register.
   */
  public final boolean compatible(int realReg, int virtualReg)
  {
    return compatibleType(registers[realReg], registers[virtualReg]);
  }

  /**
   * Return true if the real register is able to hold a register of
   * the specified type.
   */
  private boolean compatibleType(int realType, int virtualType)
  {
    return (((realType & typeMask & virtualType) != 0) &&
            ((realType & checkMask) == (virtualType & checkMask)));
  }

  /**
   * Convert a register number into its assembly language form.
   */
  public abstract String registerName(int reg);
  /**
   * Return the size of the register in addressable memory units.
   */
  public abstract int registerSize(int reg);
  /**
   * Return the callee saves registers.
   */
  public abstract short[] getCalleeSaves();
  /**
   * Return the callee uses registers.
   */
  public abstract short[] getCalleeUses();

  /**
   * Return the register type with the size information added.
   * @param type is the type required
   * @param bs is the size required
   */
  public int tempRegisterType(int type, int bs)
  {
    return type;
  }

  /**
   * Return the register type with the size information added.
   * @param type is the type required
   * @param bs is the size required
   */
  public abstract int tempRegisterType(Type type, long bs);

  /**
   * Return the number of continguous real registers that this virtual
   * register requires.
   */
  public final int numContiguousRegisters(int reg)
  {
    return (registers[reg] & sizeMask) + 1;
  }

  /**
   * Return the register number of the last register in the sequence
   * of continguous real registers that this virtual register
   * requires.
   */
  public final int lastRegister(int reg)
  {
    return reg + (registers[reg] & sizeMask);
  }

  /**
   * Return the number of contiguous registers required for this type.
   */
  public final int numContiguousType(int regType)
  {
    return (regType & sizeMask) + 1;
  }

  private static final String[] sizes = {"S", "D", "?", "Q"};

  /**
   * Return a string representation of the register.
   */
  public String display(int register)
  {
    StringBuffer buf = new StringBuffer("reg-");
    buf.append(register);
    buf.append('-');
    if (intRegister(register))
      buf.append('i');
    if (adrRegister(register))
      buf.append('a');
    if (floatRegister(register))
      buf.append('f');
    if (specialRegister(register))
      buf.append('s');
    if (continueRegister(register))
      buf.append('c');
    if (readOnlyRegister(register))
      buf.append('r');
    if (pairRegister(register))
      buf.append('p');
    buf.append(sizes[numContiguousRegisters(register) - 1]);
    return buf.toString();
  }
}
