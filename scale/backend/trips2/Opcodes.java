package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;

/**
 * This class provides Trips instruction information.
 * <p>
 * $Id: Opcodes.java,v 1.19 2005-10-11 14:38:00 burrill Exp $
 * <p>
 * Copyright 2004 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Trips opcodes are represented by an integer.
 *
 */

public class Opcodes
{
  /**
   * This indicates an enter directive
   */
  public static final byte ENT = 0x0;
  /**
   * This indicates a Trips General Instruction with no operands
   */
  public static final byte G0 = 0x1;
  /**
   * This indicates a Trips General Instruction with one operand
   */
  public static final byte G1 = 0x2;
  /**
   * This indicates a Trips General Instruction with two operands
   */
  public static final byte G2 = 0x3;
  /**
   * This indicates a Trips Immediate Instruction with no operands
   */
  public static final byte I0 = 0x4;
  /**
   * This indicates a Trips Immediate Instruction with one operand
   */
  public static final byte I1 = 0x5;
  /**
   * This indicates a Trips Load Instruction
   */
  public static final byte L1 = 0x6;
  /**
   * This indicates a Trips Store Instruction
   */
  public static final byte S2 = 0x7;
  /**
   * This indicates a Trips Branch Instruction with no operands
   */
  public static final byte B0 = 0x8;
  /**
   * This indicates a Trips Branch Instruction with one operand
   */
  public static final byte B1 = 0x9;
  /**
   * This indicates a Trips Constant Instruction with no operands
   */
  public static final byte C0 = 0xA;
  /**
   * This indicates a Trips Constant Instruction with one operand
   */
  public static final byte C1 = 0xB;
  /**
   * This indicates a Trips Read Instruction with one operand
   */
  public static final byte R0 = 0xC;
  /**
   * This indicates a Trips Write Instruction with one operand
   */
  public static final byte W1 = 0xD;
  /**
   * This indicates a Phi Instruction
   */
  public static final byte PHI = 0xE;
  /**
   * This indicates an LPF instruction
   */
  public static final byte LPF = 0xF;
  /**
   * This indicates an unknown/unused instruction
   */
  public static final byte UNK = 0x10;

  // Integer Operations
  
  public static final int ADD   = 0x0000;
  public static final int SUB   = 0x0001;
  public static final int MUL   = 0x0002;
  public static final int DIVS  = 0x0003;
  public static final int DIVU  = 0x0004;
  public static final int AND   = 0x0005;
  public static final int OR    = 0x0006;
  public static final int XOR   = 0x0007;
  public static final int SLL   = 0x0008;
  public static final int SRL   = 0x0009;
  public static final int SRA   = 0x000A;
  public static final int TEQ   = 0x000C;
  public static final int TNE   = 0x000D;
  public static final int TLE   = 0x000E;
  public static final int TLEU  = 0x000F;
  public static final int TLT   = 0x0010;
  public static final int TLTU  = 0x0011;
  public static final int TGE   = 0x0012;
  public static final int TGEU  = 0x0013;
  public static final int TGT   = 0x0014;
  public static final int TGTU  = 0x0015;

  
  // Integer Immediate Instructions

  public static final int ADDI  = 0x0018; // 1-input operand + 1 immediate
  public static final int SUBI  = 0x0019; // 1-input operand + 1 immediate
  public static final int MULI  = 0x001A; // 1-input operand + 1 immediate
  public static final int DIVSI = 0x001B; // 1-input operand + 1 immediate
  public static final int DIVUI = 0x001C; // 1-input operand + 1 immediate
  public static final int ANDI  = 0x001D; // 1-input operand + 1 immediate
  public static final int ORI   = 0x001E; // 1-input operand + 1 immediate
  public static final int XORI  = 0x001F; // 1-input operand + 1 immediate
  public static final int SLLI  = 0x0020; // 1-input operand + 1 immediate
  public static final int SRLI  = 0x0021; // 1-input operand + 1 immediate
  public static final int SRAI  = 0x0022; // 1-input operand + 1 immediate
  public static final int TEQI  = 0x0024; // 1-input operand + 1 immediate
  public static final int TNEI  = 0x0025; // 1-input operand + 1 immediate
  public static final int TLEI  = 0x0026; // 1-input operand + 1 immediate
  public static final int TLEUI = 0x0027; // 1-input operand + 1 immediate
  public static final int TLTI  = 0x0028; // 1-input operand + 1 immediate
  public static final int TLTUI = 0x0029; // 1-input operand + 1 immediate
  public static final int TGEI  = 0x002A; // 1-input operand + 1 immediate
  public static final int TGEUI = 0x002B; // 1-input operand + 1 immediate
  public static final int TGTI  = 0x002C; // 1-input operand + 1 immediate
  public static final int TGTUI = 0x002D; // 1-input operand + 1 immediate

  // Floating Point Instructions

  public static final int FADD  = 0x0030;
  public static final int FSUB  = 0x0031;
  public static final int FMUL  = 0x0032;
  public static final int FDIV  = 0x0033;
  public static final int FEQ   = 0x0034;
  public static final int FNE   = 0x0035;
  public static final int FLE   = 0x0036;
  public static final int FLT   = 0x0037;
  public static final int FGE   = 0x0038;
  public static final int FGT   = 0x0039;
  public static final int FITOD = 0x003C; // 1-input operand
  public static final int FDTOI = 0x003D; // 1-input operand
  public static final int FSTOD = 0x003E; // 1-input operand
  public static final int FDTOS = 0x003F; // 1-input operand

  // Constant Generation Instructions

  public static final int GENS  = 0x0040; // 0-input operands + 1 constant
  public static final int GENU  = 0x0041; // 0-input operands + 1 constant
  public static final int APP   = 0x0042; // 1-input operand + 1 constant

  // Load/Store Instructions

  public static final int LB    = 0x0044; // 1-input operand (+ opt immediate)
  public static final int LH    = 0x0045; // 1-input operand (+ opt immediate)
  public static final int LW    = 0x0046; // 1-input operand (+ opt immediate)
  public static final int LD    = 0x0047; // 1-input operand (+ opt immediate)
  public static final int LBS   = 0x0048; // 1-input operand (+ opt immediate)
  public static final int LHS   = 0x0049; // 1-input operand (+ opt immediate)
  public static final int LWS   = 0x004A; // 1-input operand (+ opt immediate)
 
  public static final int SB    = 0x004C; //  (+ opt immediate)
  public static final int SH    = 0x004D; //  (+ opt immediate)
  public static final int SW    = 0x004E; //  (+ opt immediate)
  public static final int SD    = 0x004F; //  (+ opt immediate)

  // Sign Extension Instructions

  public static final int EXTSB = 0x0050; // 1-input operand
  public static final int EXTSH = 0x0051; // 1-input operand
  public static final int EXTSW = 0x0052; // 1-input operand
  public static final int EXTUB = 0x0054; // 1-input operand
  public static final int EXTUH = 0x0055; // 1-input operand
  public static final int EXTUW = 0x0056; // 1-input operand

  // Branch Instructions

  public static final int BR    = 0x0058; // 1-input operand
  public static final int CALL  = 0x0059; // 1-input operand
  public static final int RET   = 0x005A; // 1-input operand
  public static final int BRO   = 0x005C;
  public static final int CALLO = 0x005D;
  public static final int SCALL = 0x005E;

  // Data Movement Instructions

  public static final int MOV   = 0x0060; // 1-input operand
  public static final int MOVI  = 0x0061; // 0-input operand
  public static final int MFPC  = 0x0062;
  public static final int WRITE = 0x0063;
  public static final int READ  = 0x0064;
  public static final int NULL  = 0x0065;
  
  // Pseudo instructions

  public static final int _NOP     = 0x0066;
  public static final int _ENTER   = 0x0067;
  public static final int _ENTERA  = 0x0068;
  public static final int _ENTERB  = 0x0069;
  public static final int _PHI     = 0x006A;
  public static final int _SDSPILL = 0x006B;
  public static final int _LDSPILL = 0x006C;
  public static final int _DUMMYSD = 0x006D;
  public static final int _LPF     = 0x006E;
 
  public static final int _LAST    = 0x006F;

  
  private static final String[] ops = {
    "add",     "sub",    "mul",   "divs",
    "divu",    "and",    "or",    "xor",
    "sll",     "srl",    "sra",   "??0B",
    "teq",     "tne",    "tle",   "tleu",
	
    "tlt",     "tltu",   "tge",   "tgeu",
    "tgt",     "tgtu",   "??16",  "??17",
    "addi",    "subi",   "muli",  "divsi",
    "divui",   "andi",   "ori",   "xori",
	
    "slli",    "srli",   "srai",  "??23", 
    "teqi",    "tnei",   "tlei",  "tleui",
    "tlti",    "tltui",  "tgei",  "tgeui",
    "tgti",    "tgtui",  "??2E",  "??2F",

    "fadd",    "fsub",   "fmul",  "fdiv",
    "feq",     "fne",    "fle",   "flt",
    "fge",     "fgt",    "??3A",  "??3B",
    "fitod",   "fdtoi",  "fstod", "fdtos",
	
    "gens",    "genu",   "app",   "??43",
    "lb",      "lh",     "lw",    "ld",
    "lbs",     "lhs",    "lws",   "??4B",
    "sb",      "sh",     "sw",    "sd",
	
    "extsb",   "extsh",  "extsw", "??53",
    "extub",   "extuh",  "extuw", "??57",
    "br",      "call",   "ret",   "??5B",
    "bro",     "callo",  "scall", "??5F",
	
    "mov",     "movi",   "mfpc",  "write",
    "read",    "null",   "nop",   "enter",  
    "entera",  "enterb", "phi",   "sdspill",
    "ldspill", "dummysd","lpf"
  };

  private static final byte[] format = { // Instruction format
    G2,     G2,     G2,     G2,
    G2,     G2,     G2,     G2,
    G2,     G2,     G2,     UNK,
    G2,     G2,     G2,     G2,

    G2,     G2,     G2,     G2,
    G2,     G2,     UNK,    UNK,
    I1,     I1,     I1,     I1,
    I1,     I1,     I1,     I1,
	
    I1,     I1,     I1,     UNK,
    I1,     I1,     I1,     I1,
    I1,     I1,     I1,     I1,
    I1,     I1,     UNK,    UNK,

    G2,     G2,     G2,     G2,
    G2,     G2,     G2,     G2,
    G2,     G2,     UNK,    UNK,
    G1,     G1,     G1,     G1,
	
    C0,     C0,     C1,     UNK,
    L1,     L1,     L1,     L1,
    L1,     L1,     L1,     UNK,
    S2,     S2,     S2,     S2,
	
    G1,     G1,     G1,     UNK,
    G1,     G1,     G1,     UNK,
    B1,     B1,     B1,     UNK,
    B0,     B0,     B0,     UNK,
	
    G1,     I0,     I0,     W1,
    R0,     G0,     C0,     ENT,
    ENT,    ENT,    PHI,    S2,
    L1,     S2,     LPF
  };
  
  private static final byte[] targets = { // Number of targets for a format
  // ENT, G0, G1, G2, I0, I1, L1, S2, B0, B1, C0, C1, R0, W1, PHI, LPF 
     1,   1,  2,  2,  1,  1,  1,  0,  0,  0,  1,  1,  2,  0,  0,   0
  };
  
  /**
   * Return the number of targets.
   */
  public static int getNumTargets(Instruction inst)
  {
    int opcode = inst.getOpcode();
    return targets[getFormat(opcode)];
  }
  
  /**
   * Return the number of targets.
   */
  public static int getNumTargets(int opcode)
  {
    return targets[getFormat(opcode)];
  }
  
  /**
   * Return the non-immediate form of an integer instruction.
   */
  public static int getIntOp(int op)
  {
    if ((op >= ADDI) && (op <= TGTUI))
      return op-(ADDI-ADD);
    if (op == MOVI)
      return MOV;
    return op;
  }
  
  /**
   * Return the immediate form of an integer instruction.
   */
  public static int getIntImmOp(int op)
  {
    if ((op >= ADD) && (op <= TGTU))
      return op+(ADDI-ADD);
    if (op == MOV)
      return MOVI;
    return op;
  }
  
  /**
   * Return the opcode mnemonic for the instruction.
   */
  public static String getOp(Instruction inst)
  {
    int opcode = inst.getOpcode();
    if (inst.nullified())
       return ops[_NOP] + ";\t" + getOp(opcode);
    
    return getOp(opcode);
  }

  /**
   * Return the opcode mnemonic for the instruction.
   */
  public static String getOp(Branch inst)
  {
    int opcode = inst.getOpcode();
    return getOp(opcode);
  }

  /**
   * Return the opcode mnemonic for the instruction opcode.
   */
  public static String getOp(int opcode)
  {
    if ((opcode < 0x0000) || (opcode > _LAST)) // Unknown or pseudo op.
      return "OP" + Integer.toHexString(opcode);

    return ops[opcode];
  }

  /**
   * Return the opcode for a mneumonic
   */
  public static int getOp(String opcode)
  {
    String str = opcode.toLowerCase();
    
    for (int i = 0; i < ops.length; i++) {
      if (ops[i].equals(str))
        return i;
    }
    
    return -1;
  }
  
  /**
   * Return the instruction format.
   */
  public static byte getFormat(int opcode) 
  {
    return format[opcode];
  }
}
