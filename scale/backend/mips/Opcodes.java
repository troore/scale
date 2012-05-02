package scale.backend.mips;

import scale.common.*;
import scale.backend.*;

/** 
 * This class provides Mips instruction information.
 * <p>
 * $Id: Opcodes.java,v 1.9 2007-01-04 16:48:47 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * MIPS opcodes come in three formats:
 * <ol>
 * <li> R (register) format: Bits <31:26> are the opcode, bits <5:0> are the function
 * <li> I (immediate) format: Bits <31:26> are the opcode
 * <li> J (jump) format: Bits <31:26> are the opcode
 */

public class Opcodes
{
    /**
     * NOP is SLL $0, $0, $0
     */
    public static final int NOP     = 0x00000000;

    public static final int ADD     = 0x00000020;
    public static final int ADDI    = 0x20000000;
    public static final int ADDIU   = 0x24000000;
    public static final int ADDU    = 0x00000021;
    public static final int AND     = 0x00000024;
    public static final int ANDI    = 0x30000000;
    public static final int BEQ     = 0x10000000;
    public static final int BEQL    = 0x50000000;
    public static final int BGEZ    = 0x04010000;
    public static final int BGEZAL  = 0x04110000;
    public static final int BGEZALL = 0x04130000;
    public static final int BGEZL   = 0x04030000;
    public static final int BGTZ    = 0x1C000000;
    public static final int BGTZL   = 0x5C000000;
    public static final int BLEZ    = 0x18000000;
    public static final int BLEZL   = 0x58000000;
    public static final int BLTZ    = 0x04000000;
    public static final int BLTZAL  = 0x04100000;
    public static final int BLTZALL = 0x04120000;
    public static final int BLTZL   = 0x04020000;
    public static final int BNE     = 0x14000000;
    public static final int BNEL    = 0x54000000;
    public static final int BREAK   = 0x0000000D;
//  public static final int COP0    = 0x40000000;
//  public static final int COP1    = 0x44000000;
//  public static final int COP2    = 0x48000000;
//  public static final int COP3    = 0x4C000000;
    public static final int DADD    = 0x0000002C;
    public static final int DADDI   = 0x60000000;
    public static final int DADDIU  = 0x64000000;
    public static final int DADDU   = 0x0000002D;
    public static final int DDIV    = 0x0000001E;
    public static final int DDIVU   = 0x0000001F;
    public static final int DIV     = 0x0000001A;
    public static final int DIVU    = 0x0000001B;
    public static final int DMULT   = 0x0000001C;
    public static final int DMULTU  = 0x0000001D;
    public static final int DSLL    = 0x00000038;
    public static final int DSLL32  = 0x0000003C;
    public static final int DSLLV   = 0x00000014;
    public static final int DSRA    = 0x0000003B;
    public static final int DSRA32  = 0x0000003F;
    public static final int DSRAV   = 0x00000017;
    public static final int DSRL    = 0x0000003A;
    public static final int DSRL32  = 0x0000003E;
    public static final int DSRLV   = 0x00000016;
    public static final int DSUB    = 0x0000002E;
    public static final int DSUBU   = 0x0000002F;
    public static final int J       = 0x08000000;
    public static final int JAL     = 0x0C000000;
    public static final int JALR    = 0x00000009;
    public static final int JR      = 0x00000008;
    public static final int LB      = 0x80000000;
    public static final int LBU     = 0x90000000;
    public static final int LD      = 0xDC000000;
//  public static final int LDC1    = 0xD4000000;
//  public static final int LDC2    = 0xD8000000;
    public static final int LDL     = 0x68000000;
    public static final int LDR     = 0x6C000000;
    public static final int LH      = 0x84000000;
    public static final int LHU     = 0x94000000;
    public static final int LL      = 0xC0000000;
    public static final int LLD     = 0xD0000000;
    public static final int LUI     = 0x3C000000;
    public static final int LW      = 0x8C000000;
//  public static final int LWC1    = 0xC4000000;
//  public static final int LWC2    = 0xC8000000;
//  public static final int LWC3    = 0xCC000000;
    public static final int LWL     = 0x88000000;
    public static final int LWR     = 0x98000000;
    public static final int LWU     = 0x9C000000;
    public static final int MFHI    = 0x00000010;
    public static final int MFLO    = 0x00000012;
    public static final int MOVN    = 0x0000000B;
    public static final int MOVZ    = 0x0000000A;
    public static final int MTHI    = 0x00000011;
    public static final int MTLO    = 0x00000013;
    public static final int MULT    = 0x00000018;
    public static final int MULTU   = 0x00000019;
    public static final int NOR     = 0x00000027;
    public static final int OR      = 0x00000025;
    public static final int ORI     = 0x34000000;
    public static final int PREF    = 0xCC000000;
    public static final int SB      = 0xA0000000;
    public static final int SC      = 0xE0000000;
    public static final int SCD     = 0xF0000000;
    public static final int SD      = 0xFC000000;
//  public static final int SDC1    = 0xF4000000;
//  public static final int SDC2    = 0xF8000000;
    public static final int SDL     = 0xB0000000;
    public static final int SDR     = 0xB4000000;
    public static final int SH      = 0xA4000000;
    public static final int SLL     = 0x00010000; // changed so value != 0, really SLL = 0x000000000;
    public static final int SLLV    = 0x00000004;
    public static final int SLT     = 0x0000002A;
    public static final int SLTI    = 0x28000000;
    public static final int SLTIU   = 0x2C000000;
    public static final int SLTU    = 0x0000002B;
    public static final int SRA     = 0x00000003;
    public static final int SRAV    = 0x00000007;
    public static final int SRL     = 0x00000002;
    public static final int SRLV    = 0x00000006;
    public static final int SUB     = 0x00000022;
    public static final int SUBU    = 0x00000023;
    public static final int SW      = 0xAC000000;
//  public static final int SWC1    = 0xE4000000;
//  public static final int SWC2    = 0xE8000000;
//  public static final int SWC3    = 0xEC000000;
    public static final int SWL     = 0xA8000000;
    public static final int SWR     = 0xB8000000;
    public static final int SYNC    = 0x0000000F;
    public static final int SYSCALL = 0x0000000C;
    public static final int TEQ     = 0x00000034;
    public static final int TEQI    = 0x040C0000;
    public static final int TGE     = 0x00000030;
    public static final int TGEI    = 0x04080000;
    public static final int TGEIU   = 0x04090000;
    public static final int TGEU    = 0x00000031;
    public static final int TLT     = 0x00000032;
    public static final int TLTI    = 0x040A0000;
    public static final int TLTIU   = 0x040B0000;
    public static final int TLTU    = 0x00000033;
    public static final int TNE     = 0x00000036;
    public static final int TNEI    = 0x040E0000;
    public static final int XOR     = 0x00000026;
    public static final int XORI    = 0x38000000;

        public static final int ABS_S     = 0x46000005;
        public static final int ABS_D     = 0x46200005;
        public static final int ADD_S     = 0x46000000;
        public static final int ADD_D     = 0x46200000;
        public static final int BC1F      = 0x45000000;
        public static final int BC1FL     = 0x45020000;
        public static final int BC1T      = 0x45010000;
        public static final int BC1TL     = 0x45030000;
        public static final int C_F_S     = 0x46000030;
        public static final int C_F_D     = 0x46200030;
        public static final int C_UN_S    = 0x46000031;
        public static final int C_UN_D    = 0x46200031;
        public static final int C_EQ_S    = 0x46000032;
        public static final int C_EQ_D    = 0x46200032;
        public static final int C_UEQ_S   = 0x46000033;
        public static final int C_UEQ_D   = 0x46200033;
        public static final int C_OLT_S   = 0x46000034;
        public static final int C_OLT_D   = 0x46200034;
        public static final int C_ULT_S   = 0x46000035;
        public static final int C_ULT_D   = 0x46200035;
        public static final int C_OLE_S   = 0x46000036;
        public static final int C_OLE_D   = 0x46200036;
        public static final int C_ULE_S   = 0x46000037;
        public static final int C_ULE_D   = 0x46200037;
        public static final int C_SF_S    = 0x46000038;
        public static final int C_SF_D    = 0x46200038;
        public static final int C_NGLE_S  = 0x46000039;
        public static final int C_NGLE_D  = 0x46200039;
        public static final int C_SEQ_S   = 0x4600003A;
        public static final int C_SEQ_D   = 0x4620003A;
        public static final int C_NGL_S   = 0x4600003B;
        public static final int C_NGL_D   = 0x4620003B;
        public static final int C_LT_S    = 0x4600003C;
        public static final int C_LT_D    = 0x4620003C;
        public static final int C_NGE_S   = 0x4600003D;
        public static final int C_NGE_D   = 0x4620003D;
        public static final int C_LE_S    = 0x4600003E;
        public static final int C_LE_D    = 0x4620003E;
        public static final int C_NGT_S   = 0x4600003F;
        public static final int C_NGT_D   = 0x4620003F;
        public static final int CEIL_L_S  = 0x4600000A;
        public static final int CEIL_L_D  = 0x4620000A;
        public static final int CEIL_W_S  = 0x4600000E;
        public static final int CEIL_W_D  = 0x4620000E;
        public static final int CFC1      = 0x44400000;
        public static final int CTC1      = 0x44C00000;
        public static final int CVT_D_S   = 0x46000021;
        public static final int CVT_D_W   = 0x46800021;
        public static final int CVT_D_L   = 0x46A00021;
        public static final int CVT_L_S   = 0x46000025;
        public static final int CVT_L_D   = 0x46200025;
        public static final int CVT_S_D   = 0x46200020;
        public static final int CVT_S_W   = 0x46800020;
        public static final int CVT_S_L   = 0x46A00020;
        public static final int CVT_W_S   = 0x46000024;
        public static final int CVT_W_D   = 0x46200024;
        public static final int DIV_S     = 0x46000003;
        public static final int DIV_D     = 0x46200003;
        public static final int DMFC1     = 0x44200000;
        public static final int DMTC1     = 0x44A00000;
        public static final int FLOOR_L_S = 0x4600000B;
        public static final int FLOOR_L_D = 0x4620000B;
        public static final int FLOOR_W_S = 0x4600000F;
        public static final int FLOOR_W_D = 0x4620000F;
        public static final int LDC1      = 0xD4000000;
        public static final int LDXC1     = 0x4C000001;
        public static final int LWC1      = 0xC4000000;
        public static final int LWXC1     = 0x4C000000;
        public static final int MADD_S    = 0x4C000020;
        public static final int MADD_D    = 0x4C000021;
        public static final int MFC1      = 0x44000000;
        public static final int MOV_S     = 0x46000006;
        public static final int MOV_D     = 0x46200006;
        public static final int MOVF      = 0x00000001;
        public static final int MOVF_S    = 0x46000011;
        public static final int MOVF_D    = 0x46200011;
        public static final int MOVN_S    = 0x46000013;
        public static final int MOVN_D    = 0x46200013;
        public static final int MOVT      = 0x00010001;
        public static final int MOVT_S    = 0x46010011;
        public static final int MOVT_D    = 0x46210011;
        public static final int MOVZ_S    = 0x46000012;
        public static final int MOVZ_D    = 0x46200012;
        public static final int MSUB_S    = 0x4C000028;
        public static final int MSUB_D    = 0x4C000029;
        public static final int MTC1      = 0x44800000;
        public static final int MUL_S     = 0x46000002;
        public static final int MUL_D     = 0x46200002;
        public static final int NEG_S     = 0x46000007;
        public static final int NEG_D     = 0x46200007;
        public static final int NMADD_S   = 0x4C000030;
        public static final int NMADD_D   = 0x4C000031;
        public static final int NMSUB_S   = 0x4C000038;
        public static final int NMSUB_D   = 0x4C000039;
        public static final int PREFX     = 0x4C00000F;
        public static final int RECIP_S   = 0x46000015;
        public static final int RECIP_D   = 0x46200015;
        public static final int ROUND_L_S = 0x46000008;
        public static final int ROUND_L_D = 0x46200008;
        public static final int ROUND_W_S = 0x4600000C;
        public static final int ROUND_W_D = 0x4620000C;
        public static final int RSQRT_S   = 0x46000016;
        public static final int RSQRT_D   = 0x46200016;
        public static final int SDC1      = 0xF4000000;
        public static final int SDXC1     = 0x4C000009;
        public static final int SQRT_S    = 0x46000004;
        public static final int SQRT_D    = 0x46200004;
        public static final int SUB_S     = 0x46000001;
        public static final int SUB_D     = 0x46200001;
        public static final int SWC1      = 0xE4000000;
        public static final int SWXC1     = 0x4C000008;
        public static final int TRUNC_L_S = 0x46000009;
        public static final int TRUNC_L_D = 0x46200009;
        public static final int TRUNC_W_S = 0x4600000D;
        public static final int TRUNC_W_D = 0x4620000D;


  /**
   * Return true if the instruction modifies the last operand instead of the first
   */
  public static boolean reversedOperands(int Opcode)
  {
    if (Opcode == CTC1 || Opcode == DMTC1 || Opcode == MTC1)
      return true;
    return false;
  }

    // MIGHT NEED TO CHANGE THESE -Jeff

    private static final int[] fltcmp_d = {C_EQ_D, C_LT_D, C_LT_D, C_LT_D, C_LT_D, C_EQ_D};
    private static final int[] fltcmp_s = {C_EQ_S, C_LT_S, C_LT_S, C_LT_S, C_LT_S, C_EQ_S};

    // Whether or not the arguments have to be reversed in the float comparison
    private static final boolean[] fltcmp_order = {false, true, false, true, false, false};

    private static final int[] fltbranch = {BC1T, BC1F, BC1T, BC1T, BC1F, BC1F};
    private static final int[] fltmovgp = {MOVT, MOVF, MOVT, MOVT, MOVF, MOVF};

    /**
     * Return the appropriate opcode for a float compare
     */
    public static int lookupFltCompare(int size, int which)
    {
        if(size > 4)
            return fltcmp_d[which];

        return fltcmp_s[which];
    }

    public static boolean lookupFltCompareOrder(int size, int which)
    {
        return fltcmp_order[which];
    }

    public static int lookupFltBranch(int size, int which)
    {
        return fltbranch[which];
    }

    public static int lookupFltMovGP(int size, int which)
    {
        return fltmovgp[which];
    }

  /**
   * Return the opcode mnemonic for the instruction.
   */
  public static String getOp(Instruction inst)
  {
    int opcode = inst.getOpcode();
    if (inst.nullified())
       return "nop #\t" + getOp(opcode);
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
    switch (opcode) {
      case 0x00000020: return "add";
      case 0x20000000: return "addi";
      case 0x24000000: return "addiu";
      case 0x00000021: return "addu";
      case 0x00000024: return "and";
      case 0x30000000: return "andi";
      case 0x10000000: return "beq";
      case 0x50000000: return "beql";
      case 0x04010000: return "bgez";
      case 0x04110000: return "bgezal";
      case 0x04130000: return "bgezall";
      case 0x04030000: return "bgezl";
      case 0x1c000000: return "bgtz";
      case 0x5c000000: return "bgtzl";
      case 0x18000000: return "blez";
      case 0x58000000: return "blezl";
      case 0x04000000: return "bltz";
      case 0x04100000: return "bltzal";
      case 0x04120000: return "bltzall";
      case 0x04020000: return "bltzl";
      case 0x14000000: return "bne";
      case 0x54000000: return "bnel";
      case 0x0000000d: return "break";
//    case 0x40000000: return "cop0";
//    case 0x44000000: return "cop1";
//    case 0x48000000: return "cop2";
//    case 0x4c000000: return "cop3";
      case 0x0000002c: return "dadd";
      case 0x60000000: return "daddi";
      case 0x64000000: return "daddiu";
      case 0x0000002d: return "daddu";
      case 0x0000001e: return "ddiv";
      case 0x0000001f: return "ddivu";
      case 0x0000001a: return "div";
      case 0x0000001b: return "divu";
      case 0x0000001c: return "dmult";
      case 0x0000001d: return "dmultu";
      case 0x00000038: return "dsll";
      case 0x0000003c: return "dsll32";
      case 0x00000014: return "dsllv";
      case 0x0000003b: return "dsra";
      case 0x0000003f: return "dsra32";
      case 0x00000017: return "dsrav";
      case 0x0000003a: return "dsrl";
      case 0x0000003e: return "dsrl32";
      case 0x00000016: return "dsrlv";
      case 0x0000002e: return "dsub";
      case 0x0000002f: return "dsubu";
      case 0x08000000: return "j";
      case 0x0c000000: return "jal";
      case 0x00000009: return "jalr";
      case 0x00000008: return "jr";
      case 0x80000000: return "lb";
      case 0x90000000: return "lbu";
      case 0xdc000000: return "ld";
//    case 0xd4000000: return "ldc1";
//    case 0xd8000000: return "ldc2";
      case 0x68000000: return "ldl";
      case 0x6c000000: return "ldr";
      case 0x84000000: return "lh";
      case 0x94000000: return "lhu";
      case 0xc0000000: return "ll";
      case 0xd0000000: return "lld";
      case 0x3c000000: return "lui";
      case 0x8c000000: return "lw";
//    case 0xc4000000: return "lwc1";
//    case 0xc8000000: return "lwc2";
//    case 0xcc000000: return "lwc3";
      case 0x88000000: return "lwl";
      case 0x98000000: return "lwr";
      case 0x9c000000: return "lwu";
      case 0x00000010: return "mfhi";
      case 0x00000012: return "mflo";
      case 0x0000000b: return "movn";
      case 0x0000000a: return "movz";
      case 0x00000011: return "mthi";
      case 0x00000013: return "mtlo";
      case 0x00000018: return "mult";
      case 0x00000019: return "multu";
      case 0x00000027: return "nor";
      case 0x00000025: return "or";
      case 0x34000000: return "ori";
      case 0xcc000000: return "pref";
      case 0xa0000000: return "sb";
      case 0xe0000000: return "sc";
      case 0xf0000000: return "scd";
      case 0xfc000000: return "sd";
//    case 0xf4000000: return "sdc1";
//    case 0xf8000000: return "sdc2";
      case 0xb0000000: return "sdl";
      case 0xb4000000: return "sdr";
      case 0xa4000000: return "sh";
      case 0x00010000: return "sll"; // value changed so sll != 0
      case 0x00000004: return "sllv";
      case 0x0000002a: return "slt";
      case 0x28000000: return "slti";
      case 0x2c000000: return "sltiu";
      case 0x0000002b: return "sltu";
      case 0x00000003: return "sra";
      case 0x00000007: return "srav";
      case 0x00000002: return "srl";
      case 0x00000006: return "srlv";
      case 0x00000022: return "sub";
      case 0x00000023: return "subu";
      case 0xac000000: return "sw";
//    case 0xe4000000: return "swc1";
//    case 0xe8000000: return "swc2";
//    case 0xec000000: return "swc3";
      case 0xa8000000: return "swl";
      case 0xb8000000: return "swr";
      case 0x0000000f: return "sync";
      case 0x0000000c: return "syscall";
      case 0x00000034: return "teq";
      case 0x040c0000: return "teqi";
      case 0x00000030: return "tge";
      case 0x04080000: return "tgei";
      case 0x04090000: return "tgeiu";
      case 0x00000031: return "tgeu";
      case 0x00000032: return "tlt";
      case 0x040a0000: return "tlti";
      case 0x040b0000: return "tltiu";
      case 0x00000033: return "tltu";
      case 0x00000036: return "tne";
      case 0x040e0000: return "tnei";
      case 0x00000026: return "xor";
      case 0x38000000: return "xori";

      case 0x46000005: return "abs.s";
      case 0x46200005: return "abs.d";
      case 0x46000000: return "add.s";
      case 0x46200000: return "add.d";
      case 0x45000000: return "bc1f";
      case 0x45020000: return "bc1fl";
      case 0x45010000: return "bc1t";
      case 0x45030000: return "bc1tl";
      case 0x46000030: return "c.f.s";
      case 0x46200030: return "c.f.d";
      case 0x46000031: return "c.un.s";
      case 0x46200031: return "c.un.d";
      case 0x46000032: return "c.eq.s";
      case 0x46200032: return "c.eq.d";
      case 0x46000033: return "c.ueq.s";
      case 0x46200033: return "c.ueq.d";
      case 0x46000034: return "c.olt.s";
      case 0x46200034: return "c.olt.d";
      case 0x46000035: return "c.ult.s";
      case 0x46200035: return "c.ult.d";
      case 0x46000036: return "c.ole.s";
      case 0x46200036: return "c.ole.d";
      case 0x46000037: return "c.ule.s";
      case 0x46200037: return "c.ule.d";
      case 0x46000038: return "c.sf.s";
      case 0x46200038: return "c.sf.d";
      case 0x46000039: return "c.ngle.s";
      case 0x46200039: return "c.ngle.d";
      case 0x4600003a: return "c.seq.s";
      case 0x4620003a: return "c.seq.d";
      case 0x4600003b: return "c.ngl.s";
      case 0x4620003b: return "c.ngl.d";
      case 0x4600003c: return "c.lt.s";
      case 0x4620003c: return "c.lt.d";
      case 0x4600003d: return "c.nge.s";
      case 0x4620003d: return "c.nge.d";
      case 0x4600003e: return "c.le.s";
      case 0x4620003e: return "c.le.d";
      case 0x4600003f: return "c.ngt.s";
      case 0x4620003f: return "c.ngt.d";
      case 0x4600000a: return "ceil.l.s";
      case 0x4620000a: return "ceil.l.d";
      case 0x4600000e: return "ceil.w.s";
      case 0x4620000e: return "ceil.w.d";
      case 0x44400000: return "cfc1";
      case 0x44c00000: return "ctc1";
      case 0x46000021: return "cvt.d.s";
      case 0x46800021: return "cvt.d.w";
      case 0x46a00021: return "cvt.d.l";
      case 0x46000025: return "cvt.l.s";
      case 0x46200025: return "cvt.l.d";
      case 0x46200020: return "cvt.s.d";
      case 0x46800020: return "cvt.s.w";
      case 0x46a00020: return "cvt.s.l";
      case 0x46000024: return "cvt.w.s";
      case 0x46200024: return "cvt.w.d";
      case 0x46000003: return "div.s";
      case 0x46200003: return "div.d";
      case 0x44200000: return "dmfc1";
      case 0x44a00000: return "dmtc1";
      case 0x4600000b: return "floor.l.s";
      case 0x4620000b: return "floor.l.d";
      case 0x4600000f: return "floor.w.s";
      case 0x4620000f: return "floor.w.d";
      case 0xd4000000: return "ldc1";
      case 0x4c000001: return "ldxc1";
      case 0xc4000000: return "lwc1";
      case 0x4c000000: return "lwxc1";
      case 0x4c000020: return "madd.s";
      case 0x4c000021: return "madd.d";
      case 0x44000000: return "mfc1";
      case 0x46000006: return "mov.s";
      case 0x46200006: return "mov.d";
      case 0x00000001: return "movf";
      case 0x46000011: return "movf.s";
      case 0x46200011: return "movf.d";
      case 0x46000013: return "movn.s";
      case 0x46200013: return "movn.d";
      case 0x00010001: return "movt";
      case 0x46010011: return "movt.s";
      case 0x46210011: return "movt.d";
      case 0x46000012: return "movz.s";
      case 0x46200012: return "movz.d";
      case 0x4c000028: return "msub.s";
      case 0x4c000029: return "msub.d";
      case 0x44800000: return "mtc1";
      case 0x46000002: return "mul.s";
      case 0x46200002: return "mul.d";
      case 0x46000007: return "neg.s";
      case 0x46200007: return "neg.d";
      case 0x4c000030: return "nmadd.s";
      case 0x4c000031: return "nmadd.d";
      case 0x4c000038: return "nmsub.s";
      case 0x4c000039: return "nmsub.d";
      case 0x4c00000f: return "prefx";
      case 0x46000015: return "recip.s";
      case 0x46200015: return "recip.d";
      case 0x46000008: return "round.l.s";
      case 0x46200008: return "round.l.d";
      case 0x4600000c: return "round.w.s";
      case 0x4620000c: return "round.w.d";
      case 0x46000016: return "rsqrt.s";
      case 0x46200016: return "rsqrt.d";
      case 0xf4000000: return "sdc1";
      case 0x4c000009: return "sdxc1";
      case 0x46000004: return "sqrt.s";
      case 0x46200004: return "sqrt.d";
      case 0x46000001: return "sub.s";
      case 0x46200001: return "sub.d";
      case 0xe4000000: return "swc1";
      case 0x4c000008: return "swxc1";
      case 0x46000009: return "trunc.l.s";
      case 0x46200009: return "trunc.l.d";
      case 0x4600000d: return "trunc.w.s";
      case 0x4620000d: return "trunc.w.d";

      default: break;
    }
    throw new scale.common.InternalError("Invalid mips opcode " + opcode);
  }
}
