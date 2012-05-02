package scale.backend.trips2;

import scale.backend.*;
import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.RealType;

/** 
 * This class describes the register set of the TRIPS Grid Processor.
 * <p>
 * $Id: Trips2RegisterSet.java,v 1.26 2007-08-27 18:30:14 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * TRIPS has 128 programmable registers and makes no distinction between floating 
 * point and integer registers.
 * <table>
 * <tr><td>Reg #</td><td>Usage</td></td>
 * <tr><td>R0</td><td>Program Counter (PC)</td></tr>
 * <tr><td>R1</td><td>Stack Pointer (SP)</td></tr>
 * <tr><td>R2</td><td>Link Register (LR)</td></tr> 
 * <tr><td>R3</td><td>First Argument and Return Values</td></tr> 
 * <tr><td>R4 - R10</td><td>Arguments</td></tr> 
 * <tr><td>R11</td><td>Global Pointer/Reserved</td></tr>    
 * <tr><td>R12 - R69</td><td>Local Variables</td></tr> 
 * <tr><td>R70 - R126</td><td>Scratch Variables</td></tr>  
 * <tr><td>R17</td><td>Reserved</td></tr>  
 * </table>
 */

public class Trips2RegisterSet extends scale.backend.RegisterSet
{
  /**
   * Function value return register. 
   */
  public static final int IR_REG = 3;
  /**
   * First argument register. 
   */
  public static final int IF_REG = 3;
  /**
   * Last rgument register. 
   */
  public static final int IL_REG = 10;
  /**
   * Return address register.
   */
  public static final int RA_REG = 2;
  /**
   * Stack pointer register.
   */
  public static final int SP_REG = 1;
  /**
   * Frame pointer register.  For most functions the frame pointer
   * register is not used.  If the function calls <code>alloca</code>
   * the frame pointer register is used to access the function's stack
   * frame while allowing the stack pointer to be changed by
   * <code>alloca</code> which allocates space for the user on the
   * stack.  Upon entry to these functions, the address in the stack
   * pointer register is decremented and this address is then copied
   * into the frame pointer register.  The address in the frame
   * pointer register is copied back into the stack pointer register
   * just before the stack pointer register is incremented upon
   * function return.
   */
  public static final int FP_REG = 12;
  /**
   * Floating point function value return register. 
   */
  public static final int FR_REG = 3;           // same as integer
  /**
   * First floating point argument register. 
   */
  public static final int FF_REG = 3;
  /**
   * Last floating point argument register. 
   */
  public static final int FL_REG = 10;
  /**
   * Size in bytes of integer register. 
   */
  public static final int IREG_SIZE = 8;
  /**
   * Size in bytes of integer register. 
   */
  public static final int FREG_SIZE = 8;
  /**
   * Specify the number of actual (real) registers.
   * Default is 128 registers.
   * The maximum allowed value is 1024 and the minimum allowed value is 16.
   */
  public static int regSetSize = 128;
  /**
   * Specify the number of banks that (real) registers are divided into.
   * Default is 4 banks.
   * Which bank a register is in is determined by RegNum % NumBanks.
   */
  public static int numBanks = 4;
  /**
   * Specify the number of registers in a bank that can be accessed in the same block.
   * Default is 8 registers.
   * The size of the register set should be evenly divisible by the number of banks.
   */
  public static int bankAccesses = 8;
  /**
   * Specify the number of register accesses allowed in the same block.
   * This is equal to the number of banks * number of allowed bank accesses/block.
   */
  public static int perBlockRegAccesses = numBanks * bankAccesses;
  /**
   * The types of real registers on TRIPS.
   */
  private short[] preferredOrder;
  private short[] calleeUses;
  private short[] calleeSaves;

  /**
   * The preferred order in which registers should be allocated.
   * This array must be the same length as actualRegisters and include all the integers
   * between 0 and the length of the array.
   */
  private static final short[] initialPreferredOrder = {
     70,  71,  72,  73,  74,  75,  76,  77,  78,  79,
     80,  81,  82,  83,  84,  85,  86,  87,  88,  89,
     90,  91,  92,  93,  94,  95,  96,  97,  98,  99,
    100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
    110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
    120, 121, 122, 123, 124, 125, 126, 127,

                     3,   4,   5,   6,   7,   8,   9,     
     10,  11,  12,  13,  14,  15,  16,  17,  18,  19,
     20,  21,  22,  23,  24,  25,  26,  27,  28,  29,
     30,  31,  32,  33,  34,  35,  36,  37,  38,  39,
     40,  41,  42,  43,  44,  45,  46,  47,  48,  49,
     50,  51,  52,  53,  54,  55,  56,  57,  58,  59,
     60,  61,  62,  63,  64,  65,  66,  67,  68,  69
  };

  /**
   * The registers that a callee can use without saving and restoring.
   */
  private static final short[] initialCalleeUses = {
                     3,   4,   5,   6,   7,   8,   9,
     10,  11,

     70,  71,  72,  73,  74,  75,  76,  77,  78,  79,
     80,  81,  82,  83,  84,  85,  86,  87,  88,  89,
     90,  91,  92,  93,  94,  95,  96,  97,  98,  99,
    100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
    110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
    120, 121, 122, 123, 124, 125, 126, 127
  };

  /**
   * The registers that a callee must save and restore if they are used by the callee.
   */
  private static final short[] initialCalleeSaves = {
               12,  13,  14,  15,  16,  17,  18,  19,
     20,  21,  22,  23,  24,  25,  26,  27,  28,  29,
     30,  31,  32,  33,  34,  35,  36,  37,  38,  39,
     40,  41,  42,  43,  44,  45,  46,  47,  48,  49,
     50,  51,  52,  53,  54,  55,  56,  57,  58,  59,
     60,  61,  62,  63,  64,  65,  66,  67,  68,  69
  };
  
  /**
   * The lowest temp register being used by the current routine. It is
   * the assembler's responsibility to update this value when it
   * begins outputting each routine by calling setLowTempReg().
   */
  private int lowTempReg = 128;


  private static int countNeeded(int max, short[] regs)
  {
    int cnt = 0;
    for (int i = 0; i < regs.length; i++)
      if (regs[i] < max)
        cnt++;
    return cnt;
  }

  private static short[] genRegs(int n, short[] initial)
  {
    short[] regs = new short[n];
    int   k      = 0;

    for (int i = 0; i < initial.length; i++) {
      short reg = initial[i];
      if (reg < regSetSize)
        regs[k++] = reg;
    }

    return regs;
  }

  private static short[] genActualRegisters()
  {
    regSetSize = ((Trips2Machine) Machine.currentMachine).getConfigValue("NUM_GPR_REGISTERS");

    if (regSetSize < 16)
      regSetSize = 16;
    else if (regSetSize > 1024)
      regSetSize = 1024;

    short[] actualRegisters = new short[regSetSize];

    for (int i = 0; i < regSetSize; i++)
      actualRegisters[i] = RegisterSet.AFIREG;

    return actualRegisters;
  }

  public Trips2RegisterSet()
  {
    super(genActualRegisters());

    if (regSetSize < initialPreferredOrder.length) {
      preferredOrder  = genRegs(countNeeded(regSetSize, initialPreferredOrder), initialPreferredOrder);
    } else if (regSetSize > initialPreferredOrder.length) {
      preferredOrder  = new short[regSetSize];
      System.arraycopy(initialPreferredOrder, 0, preferredOrder, 0, initialPreferredOrder.length);
      for (short i = (short) initialPreferredOrder.length; i < regSetSize; i++)
        preferredOrder[i] = i;
    } else
      preferredOrder = initialPreferredOrder;

    calleeUses  = genRegs(countNeeded(regSetSize, initialCalleeUses), initialCalleeUses);
    calleeSaves = genRegs(countNeeded(regSetSize, initialCalleeSaves), initialCalleeSaves);
  }

  /**
   * Update the register allocator with the lowest used temp register
   * for the current routine.  Should be called just prior to
   * generating code for this routine.
   */
  protected void setLowTmpReg(int lowTempReg)
  {
    this.lowTempReg = lowTempReg;
  }

  /**
   * Convert a register number into its assembly language form.
   */
  public String registerName(int reg)
  {
    if (reg < regSetSize)
      return "$g" + reg;

    return "$t" + (reg-lowTempReg);
  }
  
  /**
   * Convert a predicate register number into its assembly language form.
   */
  public String predicateRegisterName(int reg)
  {
    if (reg < regSetSize)
      return "$g" + reg;

    return "$p" + (reg-lowTempReg);
  }
  
  /**
   * Return which bank a given (real) register is in.
   */
  public int getBank(int reg) 
  {
    assert !virtualRegister(reg) : "Virtual reg:" + reg + " cannot be in a bank";
    
    return (reg % numBanks);
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
    return ((getType(reg) & sizeMask) + 1) * IREG_SIZE;
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
    return 126; // 128 - (PC/SP)
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

    if (bs > IREG_SIZE)
      rt |= RegisterSet.DBLEREG;

    return rt;
  }
  
  /**
   * Update the registers array and next temp register.
   */
  protected void setRegisters(int[] map, int nextRegister)
  {
    short[] regs = new short[nextRegister];
    
    for (int i = 0; i < map.length; i++) {
      int reg = map[i];
      if (reg < 0)
        continue;
      regs[reg] = registers[i];
    }
    
    this.nextRegister = nextRegister;
    this.registers    = regs;
  }
}
