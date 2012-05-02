package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class provides Sparc instruction information.
 * <p>
 * $Id: Opcodes.java,v 1.21 2007-10-04 19:57:57 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Sparc opcodes are represented by an integer that is split into two fields.
 * <ol>
 * <li> Bits <11:00> are the subop function code (opf) if applicable.
 * <li> Bits <15:12> are the condition code if applicable.
 * <li> Bits <23:16> are the function code (op3 or op2).
 * <li> Bits <27:24> are the instruction format opcode.
 * </ol>
 */

public class Opcodes
{
  public static final int ILLTRAP   = 0X0000000;
  public static final int BP        = 0X0010000;
  public static final int BPN       = 0X0010000;
  public static final int BPE       = 0X0011000;
  public static final int BPLE      = 0X0012000;
  public static final int BPL       = 0X0013000;
  public static final int BPLEU     = 0X0014000;
  public static final int BPCS      = 0X0015000;
  public static final int BPNEG     = 0X0016000;
  public static final int BPVS      = 0X0017000;
  public static final int BPA       = 0X0018000;
  public static final int BPNE      = 0X0019000;
  public static final int BPG       = 0X001A000;
  public static final int BPGE      = 0X001B000;
  public static final int BPGU      = 0X001C000;
  public static final int BPCC      = 0X001D000;
  public static final int BPPOS     = 0X001E000;
  public static final int BPVC      = 0X001F000;
  public static final int B         = 0X0020000;
  public static final int BN        = 0X0020000;
  public static final int BE        = 0X0021000;
  public static final int BLE       = 0X0022000;
  public static final int BL        = 0X0023000;
  public static final int BLEU      = 0X0024000;
  public static final int BCS       = 0X0025000;
  public static final int BNEG      = 0X0026000;
  public static final int BVS       = 0X0027000;
  public static final int BA        = 0X0028000;
  public static final int BNE       = 0X0029000;
  public static final int BG        = 0X002A000;
  public static final int BGE       = 0X002B000;
  public static final int BGU       = 0X002C000;
  public static final int BCC       = 0X002D000;
  public static final int BPOS      = 0X002E000;
  public static final int BVC       = 0X002F000;
  public static final int BR        = 0X0030000;
  public static final int BRZ       = 0X0031000;
  public static final int BRLEZ     = 0X0032000;
  public static final int BRLZ      = 0X0033000;
  public static final int BRNZ      = 0X0035000;
  public static final int BRGZ      = 0X0036000;
  public static final int BRGEZ     = 0X0037000;
  public static final int SETHI     = 0X0040000;
  public static final int NOP       = 0X0040000;
  public static final int FBP       = 0X0050000;
  public static final int FBPN      = 0X0050000;
  public static final int FBPNE     = 0X0051000;
  public static final int FBPLG     = 0X0052000;
  public static final int FBPUL     = 0X0053000;
  public static final int FBPL      = 0X0054000;
  public static final int FBPUG     = 0X0055000;
  public static final int FBPG      = 0X0056000;
  public static final int FBPU      = 0X0057000;
  public static final int FBPA      = 0X0058000;
  public static final int FBPE      = 0X0059000;
  public static final int FBPUE     = 0X005A000;
  public static final int FBPGE     = 0X005B000;
  public static final int FBPUGE    = 0X005C000;
  public static final int FBPLE     = 0X005D000;
  public static final int FBPULE    = 0X005E000;
  public static final int FBPO      = 0X005F000;
  public static final int FB        = 0X0060000;
  public static final int FBN       = 0X0060000;
  public static final int FBNE      = 0X0061000;
  public static final int FBLG      = 0X0062000;
  public static final int FBUL      = 0X0063000;
  public static final int FBL       = 0X0064000;
  public static final int FBUG      = 0X0065000;
  public static final int FBG       = 0X0066000;
  public static final int FBU       = 0X0067000;
  public static final int FBA       = 0X0068000;
  public static final int FBE       = 0X0069000;
  public static final int FBUE      = 0X006A000;
  public static final int FBGE      = 0X006B000;
  public static final int FBUGE     = 0X006C000;
  public static final int FBLE      = 0X006D000;
  public static final int FBULE     = 0X006E000;
  public static final int FBO       = 0X006F000;
  public static final int OP070     = 0X0070000;
  public static final int CALL      = 0X1000000;
  public static final int ADD       = 0X2000000;
  public static final int AND       = 0X2010000;
  public static final int OR        = 0X2020000;
  public static final int XOR       = 0X2030000;
  public static final int SUB       = 0X2040000;
  public static final int ANDN      = 0X2050000;
  public static final int ORN       = 0X2060000;
  public static final int XNOR      = 0X2070000;
  public static final int ADDC      = 0X2080000;
  public static final int MULX      = 0X2090000;
  public static final int UMUL      = 0X20A0000;
  public static final int SMUL      = 0X20B0000;
  public static final int SUBC      = 0X20C0000;
  public static final int UDIVX     = 0X20D0000;
  public static final int UDIV      = 0X20E0000;
  public static final int SDIV      = 0X20F0000;
  public static final int ADDCC     = 0X2100000;
  public static final int ANDCC     = 0X2110000;
  public static final int ORCC      = 0X2120000;
  public static final int XORCC     = 0X2130000;
  public static final int SUBCC     = 0X2140000;
  public static final int ANDNCC    = 0X2150000;
  public static final int ORNCC     = 0X2160000;
  public static final int XNORCC    = 0X2170000;
  public static final int ADDCCC    = 0X2180000;
  public static final int OP192     = 0X2190000;
  public static final int UMULCC    = 0X21A0000;
  public static final int SMULCC    = 0X21B0000;
  public static final int SUBCCC    = 0X21C0000;
  public static final int OP1D2     = 0X21D0000;
  public static final int UDIVCC    = 0X21E0000;
  public static final int SDIVCC    = 0X21F0000;
  public static final int TADDCC    = 0X2200000;
  public static final int TSUBCC    = 0X2210000;
  public static final int TADDCCTV  = 0X2220000;
  public static final int TSUBCCTV  = 0X2230000;
  public static final int MULSCC    = 0X2240000;
  public static final int SLL       = 0X2250000;
  public static final int SLLX      = 0X2250001;
  public static final int SRL       = 0X2260000;
  public static final int SRLX      = 0X2260001;
  public static final int SRA       = 0X2270000;
  public static final int SRAX      = 0X2270001;
  public static final int RR        = 0X2280000;
  public static final int MEMBAR    = 0X2280000;
  public static final int STBAR     = 0X2280000;
  public static final int OP292     = 0X2290000;
  public static final int RDPR      = 0X22A0000;
  public static final int FLUSHW    = 0X22B0000;
  public static final int MOVN      = 0x22C0000;
  public static final int MOVE      = 0x22C1000;
  public static final int MOVLE     = 0x22C2000;
  public static final int MOVL      = 0x22C3000;
  public static final int MOVLEU    = 0x22C4000;
  public static final int MOVCS     = 0x22C5000;
  public static final int MOVNEG    = 0x22C6000;
  public static final int MOVVS     = 0x22C7000;
  public static final int MOVA      = 0x22C8000;
  public static final int MOVNE     = 0x22C9000;
  public static final int MOVG      = 0x22CA000;
  public static final int MOVGE     = 0x22CB000;
  public static final int MOVGU     = 0x22CC000;
  public static final int MOVCC     = 0x22CD000;
  public static final int MOVPOS    = 0x22CE000;
  public static final int MOVVC     = 0x22CF000;
  public static final int SDIVX     = 0X22D0000;
  public static final int POPC      = 0X22E0000;
  public static final int MOVR      = 0X22F0000;
  public static final int MOVRZ     = 0X22F1000;
  public static final int MOVRLEZ   = 0X22F2000;
  public static final int MOVRLZ    = 0X22F3000;
  public static final int MOVRNZ    = 0X22F5000;
  public static final int MOVRGZ    = 0X22F6000;
  public static final int MOVRGEZ   = 0X22F7000;
  public static final int WR        = 0X2300000;
  public static final int SR        = 0X2310000;
  public static final int WRPR      = 0X2320000;
  public static final int OP332     = 0X2330000;
  public static final int FPop1     = 0X2340000;
  public static final int FMOVS     = 0X2340001;
  public static final int FMOVD     = 0X2340002;
  public static final int FMOVQ     = 0X2340003;
  public static final int FNEGS     = 0X2340005;
  public static final int FNEGD     = 0X2340006;
  public static final int FNEGQ     = 0X2340007;
  public static final int FABSS     = 0X2340009;
  public static final int FABSD     = 0X234000A;
  public static final int FABSQ     = 0X234000B;
  public static final int FSQRTS    = 0X2340029;
  public static final int FSQRTD    = 0X234002A;
  public static final int FSQRTQ    = 0X234002B;
  public static final int FADDS     = 0X2340041;
  public static final int FADDD     = 0X2340042;
  public static final int FADDQ     = 0X2340043;
  public static final int FSUBS     = 0X2340045;
  public static final int FSUBD     = 0X2340046;
  public static final int FSUBQ     = 0X2340047;
  public static final int FMULS     = 0X2340049;
  public static final int FMULD     = 0X234004A;
  public static final int FMULQ     = 0X234004B;
  public static final int FDIVS     = 0X234004D;
  public static final int FDIVD     = 0X234004E;
  public static final int FDIVQ     = 0X234004F;
  public static final int FSMULD    = 0X2340069;
  public static final int FDMULQ    = 0X234006E;
  public static final int FSTOX     = 0X2340081;
  public static final int FDTOX     = 0X2340082;
  public static final int FQTOX     = 0X2340083;
  public static final int FXTOS     = 0X2340084;
  public static final int FXTOD     = 0X2340088;
  public static final int FXTOQ     = 0X234008C;
  public static final int FITOS     = 0X23400C4;
  public static final int FDTOS     = 0X23400C6;
  public static final int FQTOS     = 0X23400C7;
  public static final int FITOD     = 0X23400C8;
  public static final int FSTOD     = 0X23400C9;
  public static final int FQTOD     = 0X23400CB;
  public static final int FITOQ     = 0X23400CC;
  public static final int FSTOQ     = 0X23400CD;
  public static final int FDTOQ     = 0X23400CE;
  public static final int FSTOI     = 0X23400D1;
  public static final int FDTOI     = 0X23400D2;
  public static final int FQTOI     = 0X23400D3;
  public static final int FPop2     = 0X2350000;
  public static final int FMOVSCC   = 0X2350001;
  public static final int FMOVDCC   = 0X2350002;
  public static final int FMOVQCC   = 0X2350003;
  public static final int FMOVRSZ   = 0X2350025;
  public static final int FMOVRDZ   = 0X2350026;
  public static final int FMOVRQZ   = 0X2350027;
  public static final int FMOVRSLEZ = 0X2350045;
  public static final int FMOVRDLEZ = 0X2350046;
  public static final int FMOVRQLEZ = 0X2350047;
  public static final int FCMPES    = 0X2350055;
  public static final int FCMPED    = 0X2350056;
  public static final int FCMPEQ    = 0X2350057;
  public static final int FCMPS     = 0X2350051;
  public static final int FCMPD     = 0X2350052;
  public static final int FCMPQ     = 0X2350053;
  public static final int FMOVRSLZ  = 0X2350065;
  public static final int FMOVRDLZ  = 0X2350066;
  public static final int FMOVRQLZ  = 0X2350067;
  public static final int FMOVRSNZ  = 0X23500A5;
  public static final int FMOVRDNZ  = 0X23500A6;
  public static final int FMOVRQNZ  = 0X23500A7;
  public static final int FMOVRSGZ  = 0X23500C5;
  public static final int FMOVRDGZ  = 0X23500C6;
  public static final int FMOVRQGZ  = 0X23500C7;
  public static final int FMOVRSGEZ = 0X23500E5;
  public static final int FMOVRDGEZ = 0X23500E6;
  public static final int FMOVRQGEZ = 0X23500E7;
  public static final int IMPDEP1   = 0X2360000;
  public static final int IMPDEP2   = 0X2370000;
  public static final int JMPL      = 0X2380000;
  public static final int RETURN    = 0X2390000;
  public static final int T         = 0X23A0000;
  public static final int TN        = 0X20A0000;
  public static final int TE        = 0X20A1000;
  public static final int TLE       = 0X20A2000;
  public static final int TL        = 0X20A3000;
  public static final int TLEU      = 0X20A4000;
  public static final int TCS       = 0X20A5000;
  public static final int TNEG      = 0X20A6000;
  public static final int TVS       = 0X20A7000;
  public static final int TA        = 0X20A8000;
  public static final int TNE       = 0X20A9000;
  public static final int TG        = 0X20AA000;
  public static final int TGE       = 0X20AB000;
  public static final int TGU       = 0X20AC000;
  public static final int TCC       = 0X20AD000;
  public static final int TPOS      = 0X20AE000;
  public static final int TVC       = 0X20AF000;
  public static final int FLUSH     = 0X23B0000;
  public static final int SAVE      = 0X23C0000;
  public static final int RESTORE   = 0X23D0000;
  public static final int DONE      = 0X23E0000;
  public static final int OP3F2     = 0X23F0000;
  public static final int LDUW      = 0X3000000;
  public static final int LDUB      = 0X3010000;
  public static final int LDUH      = 0X3020000;
  public static final int LDD       = 0X3030000;
  public static final int ST        = 0X3040000;
  public static final int STW       = 0X3040000;
  public static final int STB       = 0X3050000;
  public static final int STH       = 0X3060000;
  public static final int STD       = 0X3070000;
  public static final int LD        = 0X3080000;
  public static final int LDSW      = 0X3080000;
  public static final int LDSB      = 0X3090000;
  public static final int LDSH      = 0X30A0000;
  public static final int LDX       = 0X30B0000;
  public static final int OP0C3     = 0X30C0000;
  public static final int LDSTUB    = 0X30D0000;
  public static final int STX       = 0X30E0000;
  public static final int SWAP      = 0X30F0000;
  public static final int LDUWA     = 0X3100000;
  public static final int LDUBA     = 0X3110000;
  public static final int LDUHA     = 0X3120000;
  public static final int LDDA      = 0X3130000;
  public static final int STWA      = 0X3140000;
  public static final int STBA      = 0X3150000;
  public static final int STHA      = 0X3160000;
  public static final int STDA      = 0X3170000;
  public static final int LDSWA     = 0X3180000;
  public static final int LDSBA     = 0X3190000;
  public static final int LDSHA     = 0X31A0000;
  public static final int LDXA      = 0X31B0000;
  public static final int OP1C3     = 0X31C0000;
  public static final int LDSTUBA   = 0X31D0000;
  public static final int STXA      = 0X31E0000;
  public static final int SWAPA     = 0X31F0000;
  public static final int LDF       = 0X3200000;
  public static final int LDFSR     = 0X3210000;
  public static final int LDQF      = 0X3220000;
  public static final int LDDF      = 0X3230000;
  public static final int STF       = 0X3240000;
  public static final int STFSR     = 0X3250000;
  public static final int STQF      = 0X3260000;
  public static final int STDF      = 0X3270000;
  public static final int OP283     = 0X3280000;
  public static final int OP293     = 0X3290000;
  public static final int OP2A3     = 0X32A0000;
  public static final int OP2B3     = 0X32B0000;
  public static final int OP2C3     = 0X32C0000;
  public static final int PREFETCH  = 0X32D0000;
  public static final int OP2E3     = 0X32E0000;
  public static final int OP2F3     = 0X32F0000;
  public static final int LDFA      = 0X3300000;
  public static final int OP313     = 0X3310000;
  public static final int LDQFA     = 0X3320000;
  public static final int LDDFA     = 0X3330000;
  public static final int STFA      = 0X3340000;
  public static final int OP353     = 0X3350000;
  public static final int STQFA     = 0X3360000;
  public static final int STDFA     = 0X3370000;
  public static final int OP383     = 0X3380000;
  public static final int OP393     = 0X3390000;
  public static final int OP3A3     = 0X33A0000;
  public static final int OP3B3     = 0X33B0000;
  public static final int CASA      = 0X33C0000;
  public static final int PREFETCHA = 0X33D0000;
  public static final int CASXA     = 0X33E0000;
  public static final int OP3F3     = 0X33F0000;

  public static final String[] condi = {
    "n", "e",  "le", "l",  "leu", "cs", "neg", "vs",
    "a", "ne", "g",  "ge", "gu",  "cc", "pos", "vc"};
  public static final String[] condf = {
    "n", "ne", "lg", "ul", "l",  "ug", "g",  "pu",
    "a", "e",  "ue", "ge", "uge", "le", "ule", "o"};
  public static final String[] rcond = {
    "?", "z", "lez", "lz", "??", "nz", "gz", "gez"};
  public static final String[] rr = {
    "rdy",   "rr??",  "rdccr", "rdasi", "rdtick", "rdpc",  "rdfprs", "rdasr", 
    "rdasr", "rdasr", "rdasr", "rdasr", "rdasr",  "rdasr", "rdasr",  "stbar"};
  public static final String[] wr = {
    "wry",   "wr??",  "wrccr", "wrasi", "wrasr", "wrasr", "wrfprs", "wrasr",
    "wrasr", "wrasr", "wrasr", "wrasr", "wrasr", "wrasr", "wrasr",  "sir"};

  private static final int FMTSHFT = 24;
  private static final int FTNSHFT = 16;
  private static final int CNDSHFT = 12;
  private static final int SOPSHFT = 00;

  private static final int FMTMASK = 0x00f;
  private static final int FTNMASK = 0x0ff;
  private static final int CNDMASK = 0x00f;
  private static final int SOPMASK = 0xfff;

  public static final byte[] iltypeMap = {
    0, 0, 1, 2, 2, 3, 3, 3, 3};
  public static final byte[] ftypeMap  = {
    0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2};
  public static final byte[] itypeMap  = {
    0, 0, 0, 0, 0, 1, 1, 1, 1};

  //                                   byte   short  int    long
  private static final int[] iloadv9  = {LDSB,  LDSH,  LDSW,  LDX};
  private static final int[] iloadv8  = {LDSB,  LDSH,  LD,    LDD};

  /**
   * @param bytes is the number of bytes to be loaded
   * @param v9 is true for the Version 9 of the processor
   * @return the opcode for the signed integer load
   */
  public static int ldsiOp(int bytes, boolean v9)
  {
    if (v9)
      return iloadv9[iltypeMap[bytes]];
    return iloadv8[iltypeMap[bytes]];

  }

  //                                     byte   short  int    long
  private static final int[] uloadV9  = {LDUB,  LDUH,  LDUW,  LDX};
  private static final int[] uloadV8  = {LDUB,  LDUH,  LD,    LDD};

  /**
   * @param bytes is the number of bytes to be loaded
   * @param v9 is true for the Version 9 of the processor
   * @return the opcode for the unsigned integer load
   */
  public static int lduiOp(int bytes, boolean v9)
  {
    if (v9)
      return uloadV9[iltypeMap[bytes]];
    return uloadV8[iltypeMap[bytes]];
  }

  //                                     byte   short  int    long
  private static final int[] istoreV9 = {STB,   STH,   STW,   STX};
  private static final int[] istoreV8 = {STB,   STH,   ST,    STD};

  /**
   * @param bytes is the number of bytes to be stored
   * @param v9 is true for the Version 9 of the processor
   * @return the opcode for the integer store
   */
  public static int stiOp(int bytes, boolean v9)
  {
    if (v9)
      return istoreV9[iltypeMap[bytes]];
    return istoreV8[iltypeMap[bytes]];
  }

  //                                     float  double quad
  private static final int[] floadV9  = {LDF,   LDDF,  LDQF};
  private static final int[] floadV8  = {LDF,   LDDF,  LDDF};

  /**
   * @param bytes is the number of bytes to be loaded
   * @param v9 is true for the Version 9 of the processor
   * @return the opcode for the floating point load
   */
  public static int ldfOp(int bytes, boolean v9)
  {
    if (v9)
      return floadV9[iltypeMap[bytes]];
    return floadV8[ftypeMap[bytes]];
  }

  //                                     float  double quad
  private static final int[] fstoreV9 = {STF,   STDF,  STQF};
  private static final int[] fstoreV8 = {STF,   STDF,  STDF};

  /**
   * @param bytes is the number of bytes to be loaded
   * @param v9 is true for the Version 9 of the processor
   * @return the opcode for the floating point store
   */
  public static int stfOp(int bytes, boolean v9)
  {
    if (v9)
      return fstoreV9[iltypeMap[bytes]];
    return fstoreV8[ftypeMap[bytes]];
  }

  //                                   float  double quad
  private static final int[] fmove  = {FMOVS, FMOVD, FMOVQ};

  /**
   * @param bytes is the number of bytes to be moved
   * @return the opcode for the floating point move
   */
  public static int movfOp(int bytes)
  {
    return fmove[ftypeMap[bytes]];
  }

  //                                   float  double quad
  private static final int[] fneg  = {FNEGS, FNEGD, FNEGQ};

  /**
   * @param bytes is the number of bytes to be moved
   * @return the opcode for the floating point negate
   */
  public static int negfOp(int bytes)
  {
    return fneg[ftypeMap[bytes]];
  }

  //                                   float  double quad
  private static final int[] fabs  = {FABSS, FABSD, FABSQ};

  /**
   * @param bytes is the number of bytes to be moved
   * @return the opcode for the floating point fabs
   */
  public static int absfOp(int bytes)
  {
    return fabs[ftypeMap[bytes]];
  }

  //                                   float  double quad
  private static final int[] fcmps  = {FCMPS, FCMPD, FCMPQ};

  /**
   * @param bytes is the number of bytes to be compared
   * @return the opcode for the floating point compare
   */
  public static int cmpfOp(int bytes)
  {
    return fcmps[ftypeMap[bytes]];
  }

  //                                   float  double quad
  private static final int[] ffcvti = {FSTOI, FDTOI, FQTOI,
                                       FSTOX, FDTOX, FQTOX};

  /**
   * @param fbytes is the number of bytes in the source floating point
   * value
   * @param ibytes is the number of bytes in the destination integer
   * value
   * @return the instruction opcode
   */
  public static int fcvtiOp(int fbytes, int ibytes)
  {
    return ffcvti[itypeMap[ibytes] * 3 + ftypeMap[fbytes]];
  }

  // index = itypeMap[src bytes] * 3 + ftypeMap[dest bytes];
  //                                   float  double quad
  private static final int[] ficvtf = {FITOS, FITOD, FITOQ,
                                       FXTOS, FXTOD, FXTOQ};

  /**
   * @param ibytes is the number of bytes in the source integer value
   * @param fbytes is the number of bytes in the destination floating
   * point value
   * @return the instruction opcode
   */
  public static int icvtfOp(int ibytes, int fbytes)
  {
    return ficvtf[itypeMap[ibytes] * 3 + ftypeMap[fbytes]];
  }

  // index = ftypeMap[dest bytes] + ftypeMap[source bytes] * 3;
  //                                   float  double quad
  private static final int[] ffcvtf = {FMOVS, FSTOD, FSTOQ,
                                       FDTOS, FMOVD, FDTOQ,
                                       FQTOS, FQTOD, FMOVQ};

  /**
   * @param fbytess is the number of bytes in the source floating
   * point value
   * @param fbytesd is the number of bytes in the destination floating
   * point value
   * @return the instruction opcode
   */
  public static int fcvtfOp(int fbytess, int fbytesd)
  {
    return ffcvtf[ftypeMap[fbytess] * 3 + ftypeMap[fbytesd]];
  }

  /**
   * Add a condition code to an opcode.
   */
  public static int addCond(int opcode, int cond)
  {
    return (opcode & ~(CNDMASK << CNDSHFT)) | ((cond & CNDMASK) << CNDSHFT);
  }

  /**
   * Index from operation to opcode.  This is a n by 2 table where the
   * row is indexed by the operation and the column by the type.
   */
  private static final int[] ibinops = {
    ADD,  ADD,  
    SUB,  SUB,  
    SMUL, SMUL, 
    SDIV, SDIV, 
    AND,  AND,  
    OR,   OR,   
    XOR,  XOR,  
    SRA,  SRAX, 
    SRL,  SRLX, 
    SLL,  SLLX, 
  };

  /**
   * @param which is the number of operation
   * @param bytes is the number of bytes to be compared
   * @return the opcode for the floating point operation
   */
  public static int iopOp(int which, int bytes)
  {
    return ibinops[(which * 2) + itypeMap[bytes]];
  }

  /**
   * Index from operation to opcode.  This is a n by 4 table where the
   * row is indexed by the operation and the column by the type.
   */
  private static final int[] fbinops = {
    FADDS, FADDD, FADDQ,
    FSUBS, FSUBD, FSUBQ,
    FMULS, FMULD, FMULQ,
    FDIVS, FDIVD, FDIVQ,
    0,     0,     0,
    0,     0,     0,
    0,     0,     0,
    0,     0,     0,
    0,     0,     0,
    0,     0,     0
  };

  /**
   * @param which is the number of operation
   * @param bytes is the number of bytes to be compared
   * @return the opcode for the floating point operation
   */
  public static int fopOp(int which, int bytes)
  {
    return fbinops[(which * 3) + ftypeMap[bytes]];
  }

  private static final int[] fsqrtops = {FSQRTS, FSQRTD, FSQRTQ};

  /**
   * @param bytes is the number of bytes to be compared
   * @return the opcode for the floating point operation
   */
  public static int fsqrtOp(int bytes)
  {
    return fsqrtops[ftypeMap[bytes]];
  }

  /**
   * Integer comparison branches.  a op 0
   */
  private static final int[] ibopsv9s = {
    Opcodes.BPE,  Opcodes.BPLE,  Opcodes.BPL,  Opcodes.BPG,
    Opcodes.BPGE, Opcodes.BPNE};
  private static final int[] ibopsv9u = {
    Opcodes.BPE,  Opcodes.BPLEU, Opcodes.BPCS, Opcodes.BPGU,
    Opcodes.BPCC, Opcodes.BPNE};
  private static final int[] ibopsv8s = {
    Opcodes.BE,   Opcodes.BLE,   Opcodes.BL,   Opcodes.BG,
    Opcodes.BGE,  Opcodes.BNE};
  private static final int[] ibopsv8u = {
    Opcodes.BE,   Opcodes.BLEU,  Opcodes.BCS,  Opcodes.BGU,
    Opcodes.BCC,  Opcodes.BNE};

  /**
   * @param which is the number of branch operation
   * @return the opcode for the integer branch operation
   */
  public static int bopiOp(int which, boolean v9, boolean signed)
  {
    if (v9) {
      if (signed)
        return ibopsv9s[which];
      return ibopsv9u[which];
    }
    if (signed)
      return ibopsv8s[which];
    return ibopsv8u[which];
  }

  /**
   * Floating comparison branches.  a op 0.0
   */
  private static final int[] fbopsv9 = {
    Opcodes.FBPE,  Opcodes.FBPLE, Opcodes.FBPL, Opcodes.FBPG,
    Opcodes.FBPGE, Opcodes.FBPNE};
  private static final int[] fbopsv8 = {
    Opcodes.FBE,  Opcodes.FBLE, Opcodes.FBL, Opcodes.FBG,
    Opcodes.FBGE, Opcodes.FBNE};

  /**
   * @param which is the number of branch operation
   * @return the opcode for the floating point branch operation
   */
  public static int bopfOp(int which, boolean v9)
  {
    if (v9)
      return fbopsv9[which];
    return fbopsv8[which];
  }

  private static final int[] bregops = {BRZ, BRLEZ, BRLZ, BRGZ, BRGEZ, BRNZ};

  /**
   * @param which is the number of branch operation
   * @return the opcode for the branch on register operation
   */
  public static int bropOp(int which)
  {
    return bregops[which];
  }

  /**
   * Return the opcode mnemonic for the instruction.
   */
  public static String getOp(SparcInstruction inst)
  {
    int opcode = inst.getOpcode();
    if (inst.nullified())
       return "nop !\t" + getOp(opcode);
    return getOp(opcode);
  }

  /**
   * Return the opcode mnemonic for the instruction.
   */
  public static String getOp(SparcBranch inst)
  {
    int opcode = inst.getOpcode();
    return getOp(opcode);
  }

  /**
   * Return the opcode mnemonic for the instruction opcode.
   */
  public static String getOp(int opcode)
  {
    int fmt   = (opcode >> FMTSHFT) & FMTMASK;
    int ftn   = (opcode >> FTNSHFT) & FTNMASK;
    int cond  = (opcode >> CNDSHFT) & CNDMASK;
    int subop = (opcode >> SOPSHFT) & SOPMASK;

    switch (fmt) {
    case 0:
      switch (ftn) {
      case (ILLTRAP >> FTNSHFT) & 0xff: return "illtrap";
      case (BP      >> FTNSHFT) & 0xff: return "bp" + condi[cond];
      case (B       >> FTNSHFT) & 0xff: return "b" + condi[cond];
      case (BR      >> FTNSHFT) & 0xff: return "br" + rcond[cond];
      case (SETHI   >> FTNSHFT) & 0xff: return "sethi";
      case (FBP     >> FTNSHFT) & 0xff: return "fbp" + condf[cond];
      case (FB      >> FTNSHFT) & 0xff: return "fb" + condf[cond];
      case (OP070   >> FTNSHFT) & 0xff: return "op070";
      }
      break;
    case 1: return "call";
    case 2:
      switch (ftn) {
      case (ADD      >> FTNSHFT) & 0xff: return "add";
      case (AND      >> FTNSHFT) & 0xff: return "and";
      case (OR       >> FTNSHFT) & 0xff: return "or";
      case (XOR      >> FTNSHFT) & 0xff: return "xor";
      case (SUB      >> FTNSHFT) & 0xff: return "sub";
      case (ANDN     >> FTNSHFT) & 0xff: return "andn";
      case (ORN      >> FTNSHFT) & 0xff: return "orn";
      case (XNOR     >> FTNSHFT) & 0xff: return "xnor";
      case (ADDC     >> FTNSHFT) & 0xff: return "addc";
      case (MULX     >> FTNSHFT) & 0xff: return "mulx";
      case (UMUL     >> FTNSHFT) & 0xff: return "umul";
      case (SMUL     >> FTNSHFT) & 0xff: return "smul";
      case (SUBC     >> FTNSHFT) & 0xff: return "subc";
      case (UDIVX    >> FTNSHFT) & 0xff: return "udivx";
      case (UDIV     >> FTNSHFT) & 0xff: return "udiv";
      case (SDIV     >> FTNSHFT) & 0xff: return "sdiv";
      case (ADDCC    >> FTNSHFT) & 0xff: return "addcc";
      case (ANDCC    >> FTNSHFT) & 0xff: return "andcc";
      case (ORCC     >> FTNSHFT) & 0xff: return "orcc";
      case (XORCC    >> FTNSHFT) & 0xff: return "xorcc";
      case (SUBCC    >> FTNSHFT) & 0xff: return "subcc";
      case (ANDNCC   >> FTNSHFT) & 0xff: return "andncc";
      case (ORNCC    >> FTNSHFT) & 0xff: return "orncc";
      case (XNORCC   >> FTNSHFT) & 0xff: return "xnorcc";
      case (ADDCCC   >> FTNSHFT) & 0xff: return "andccc";
      case (OP192    >> FTNSHFT) & 0xff: return "op192";
      case (UMULCC   >> FTNSHFT) & 0xff: return "umulcc";
      case (SMULCC   >> FTNSHFT) & 0xff: return "smulcc";
      case (SUBCCC   >> FTNSHFT) & 0xff: return "subccc";
      case (OP1D2    >> FTNSHFT) & 0xff: return "op1d2";
      case (UDIVCC   >> FTNSHFT) & 0xff: return "udivcc";
      case (SDIVCC   >> FTNSHFT) & 0xff: return "sdivcc";
      case (TADDCC   >> FTNSHFT) & 0xff: return "taddcc";
      case (TSUBCC   >> FTNSHFT) & 0xff: return "tsubcc";
      case (TADDCCTV >> FTNSHFT) & 0xff: return "taddcctv";
      case (TSUBCCTV >> FTNSHFT) & 0xff: return "tsubcctv";
      case (MULSCC   >> FTNSHFT) & 0xff: return "mulscc";
      case (SLL      >> FTNSHFT) & 0xff: return (subop == 0) ? "sll" : "sllx";
      case (SRL      >> FTNSHFT) & 0xff: return (subop == 0) ? "srl" : "srlx";
      case (SRA      >> FTNSHFT) & 0xff: return (subop == 0) ? "sra" : "srax";
      case (RR       >> FTNSHFT) & 0xff: return (subop < 16) ? rr[subop] : "rdasr";
      case (OP292    >> FTNSHFT) & 0xff: return "op292";
      case (RDPR     >> FTNSHFT) & 0xff: return "rdpr";
      case (FLUSHW   >> FTNSHFT) & 0xff: return "flushw";
      case (MOVCC    >> FTNSHFT) & 0xff: return "movcc";
      case (SDIVX    >> FTNSHFT) & 0xff: return "sdivx";
      case (POPC     >> FTNSHFT) & 0xff: return "popc";
      case (MOVR     >> FTNSHFT) & 0xff: return "movr" + rcond[cond];
      case (WR       >> FTNSHFT) & 0xff: return wr[subop];
      case (SR       >> FTNSHFT) & 0xff: return (subop == 0) ? "saved" : "restored";
      case (WRPR     >> FTNSHFT) & 0xff: return "wrpr";
      case (OP332    >> FTNSHFT) & 0xff: return "op332";
      case (FPop1    >> FTNSHFT) & 0xff:
        int col34 = (subop >> 0) & 0x0f;
        int row34 = (subop >> 4) & 0xff;
        switch (row34) {
        case 0X00:
          switch (col34) {
          case 0X1: return "fmovs";
          case 0X2: return "fmovd";
          case 0X3: return "fmovq";
          case 0X5: return "fnegs";
          case 0X6: return "fnegd";
          case 0X7: return "fnegq";
          case 0X9: return "fabss";
          case 0XA: return "fabsd";
          case 0XB: return "fabsq";
          }
          break;
        case 0X02:
          switch (col34) {
          case 0X9: return "fsqrts";
          case 0XA: return "fsqrtd";
          case 0XB: return "fsqrtq";
          }
          break;
        case 0X04:
          switch (col34) {
          case 0X1: return "fadds";
          case 0X2: return "faddd";
          case 0X3: return "faddq";
          case 0X5: return "fsubs";
          case 0X6: return "fsubd";
          case 0X7: return "fsubq";
          case 0X9: return "fmuls";
          case 0XA: return "fmuld";
          case 0XB: return "fmulq";
          case 0XD: return "fdivs";
          case 0XE: return "fdivd";
          case 0XF: return "fdivq";
          }
          break;
        case 0X06:
          switch (col34) {
          case 0X9: return "fsmuld";
          case 0XE: return "fdmulq";
          }
          break;
        case 0X08:
          switch (col34) {
          case 0X1: return "fstox";
          case 0X2: return "fdtox";
          case 0X3: return "fqtox";
          case 0X4: return "fxtos";
          case 0X8: return "fxtod";
          case 0XC: return "fxtoq";
          }
          break;
        case 0X0C:
          switch (col34) {
          case 0X4: return "fitos";
          case 0X6: return "fdtos";
          case 0X7: return "fqtos";
          case 0X8: return "fitod";
          case 0X9: return "fstod";
          case 0XB: return "fqtod";
          case 0XC: return "fitoq";
          case 0XD: return "fstoq";
          case 0XE: return "fdtoq";
          }
          break;
        case 0X0D:
          switch (col34) {
          case 0X1: return "fstoi";
          case 0X2: return "fdtoi";
          case 0X3: return "fqtoi";
          }
          break;
        }
        break;
      case (FPop2    >> FTNSHFT) & 0xff:
        int col35 = (subop >> 0) & 0x0f;
        int row35 = (subop >> 4) & 0xFf;
        switch (row35) {
        case 0X00:
          switch (col35) {
          case 0X1: return "fmovs" + condf[cond];
          case 0X2: return "fmovd" + condf[cond];
          case 0X3: return "fmovq" + condf[cond];
          }
          break;
        case 0X02:
          switch (col35) {
          case 0X5: return "fmovrsz";
          case 0X6: return "fmovrdz";
          case 0X7: return "fmovrqz";
          }
          break;
        case 0X04:
          switch (col35) {
          case 0X1: return "fmovs" + condf[cond];
          case 0X2: return "fmovd" + condf[cond];
          case 0X3: return "fmovq" + condf[cond];
          case 0X5: return "fmovrslez";
          case 0X6: return "fmovrdlez";
          case 0X7: return "fmovrqlez";
          }
          break;
        case 0X05:
          switch (col35) {
          case 0X1: return "fcmps";
          case 0X2: return "fcmpd";
          case 0X3: return "fcmpq";
          case 0X5: return "fcmpes";
          case 0X6: return "fcmpds";
          case 0X7: return "fcmpqs";
          }
          break;
        case 0X06:
          switch (col35) {
          case 0X5: return "fmovrslz";
          case 0X6: return "fmovrdlz";
          case 0X7: return "fmovrqlz";
          }
          break;
        case 0X08:
          switch (col35) {
          case 0X1: return "fmovs" + condf[cond];
          case 0X2: return "fmovd" + condf[cond];
          case 0X3: return "fmovq" + condf[cond];
          }
          break;
        case 0X0A:
          switch (col35) {
          case 0X5: return "fmovrsnz";
          case 0X6: return "fmovrdnz";
          case 0X7: return "fmovrqnz";
          }
          break;
        case 0X0C:
          switch (col35) {
          case 0X1: return "fmovs" + condf[cond];
          case 0X2: return "fmovd" + condf[cond];
          case 0X3: return "fmovq" + condf[cond];
          case 0X5: return "fmovrsgz";
          case 0X6: return "fmovrdgz";
          case 0X7: return "fmovrqgz";
          }
          break;
        case 0X0E:
          switch (col35) {
          case 0X5: return "fmovrsgez";
          case 0X6: return "fmovrdgez";
          case 0X7: return "fmovrqgez";
          }
          break;
        case 0X10:
          switch (col35) {
          case 0X1: return "fmovs" + condf[cond];
          case 0X2: return "fmovd" + condf[cond];
          case 0X3: return "fmovq" + condf[cond];
          }
          break;
        case 0X18:
          switch (col35) {
          case 0X1: return "fmovs" + condf[cond];
          case 0X2: return "fmovd" + condf[cond];
          case 0X3: return "fmovq" + condf[cond];
          }
          break;
        }
        break;
      case (IMPDEP1  >> FTNSHFT) & 0xff: return "impdep1";
      case (IMPDEP2  >> FTNSHFT) & 0xff: return "impdep2";
      case (JMPL     >> FTNSHFT) & 0xff: return "jmpl";
      case (RETURN   >> FTNSHFT) & 0xff: return "return";
      case (T        >> FTNSHFT) & 0xff: return "T" + condi[cond];
      case (FLUSH    >> FTNSHFT) & 0xff: return "flush";
      case (SAVE     >> FTNSHFT) & 0xff: return "save";
      case (RESTORE  >> FTNSHFT) & 0xff: return "restore";
      case (DONE     >> FTNSHFT) & 0xff: return (subop == 0) ? "done" : "retry";
      case (OP3F2    >> FTNSHFT) & 0xff: return "op3f2";
      }
      break;
    case 3:
      switch (ftn) {
      case (LDUW      >> FTNSHFT) & 0xff: return "lduw";
      case (LDUB      >> FTNSHFT) & 0xff: return "ldub";
      case (LDUH      >> FTNSHFT) & 0xff: return "lduh";
      case (LDD       >> FTNSHFT) & 0xff: return "ldd";
      case (STW       >> FTNSHFT) & 0xff: return "st";
      case (STB       >> FTNSHFT) & 0xff: return "stb";
      case (STH       >> FTNSHFT) & 0xff: return "sth";
      case (STD       >> FTNSHFT) & 0xff: return "std";
      case (LDSW      >> FTNSHFT) & 0xff: return "ld";
      case (LDSB      >> FTNSHFT) & 0xff: return "ldsb";
      case (LDSH      >> FTNSHFT) & 0xff: return "ldsh";
      case (LDX       >> FTNSHFT) & 0xff: return "ldx";
      case (OP0C3     >> FTNSHFT) & 0xff: return "op0c3";
      case (LDSTUB    >> FTNSHFT) & 0xff: return "ldstub";
      case (STX       >> FTNSHFT) & 0xff: return "stx";
      case (SWAP      >> FTNSHFT) & 0xff: return "swap";
      case (LDUWA     >> FTNSHFT) & 0xff: return "lduwa";
      case (LDUBA     >> FTNSHFT) & 0xff: return "lduba";
      case (LDUHA     >> FTNSHFT) & 0xff: return "lduha";
      case (LDDA      >> FTNSHFT) & 0xff: return "ldda";
      case (STWA      >> FTNSHFT) & 0xff: return "stwa";
      case (STBA      >> FTNSHFT) & 0xff: return "stba";
      case (STHA      >> FTNSHFT) & 0xff: return "stha";
      case (STDA      >> FTNSHFT) & 0xff: return "stda";
      case (LDSWA     >> FTNSHFT) & 0xff: return "ldswa";
      case (LDSBA     >> FTNSHFT) & 0xff: return "ldsba";
      case (LDSHA     >> FTNSHFT) & 0xff: return "ldsha";
      case (LDXA      >> FTNSHFT) & 0xff: return "ldxa";
      case (OP1C3     >> FTNSHFT) & 0xff: return "op1c3";
      case (LDSTUBA   >> FTNSHFT) & 0xff: return "ldstuba";
      case (STXA      >> FTNSHFT) & 0xff: return "stxa";
      case (SWAPA     >> FTNSHFT) & 0xff: return "swapa";
      case (LDF       >> FTNSHFT) & 0xff: return "ld";
      case (LDFSR     >> FTNSHFT) & 0xff: return "ldfsr";
      case (LDQF      >> FTNSHFT) & 0xff: return "ldq";
      case (LDDF      >> FTNSHFT) & 0xff: return "ldd";
      case (STF       >> FTNSHFT) & 0xff: return "st";
      case (STFSR     >> FTNSHFT) & 0xff: return "st";
      case (STQF      >> FTNSHFT) & 0xff: return "stq";
      case (STDF      >> FTNSHFT) & 0xff: return "std";
      case (OP283     >> FTNSHFT) & 0xff: return "op283";
      case (OP293     >> FTNSHFT) & 0xff: return "op293";
      case (OP2A3     >> FTNSHFT) & 0xff: return "op2a3";
      case (OP2B3     >> FTNSHFT) & 0xff: return "op2b3";
      case (OP2C3     >> FTNSHFT) & 0xff: return "op2c3";
      case (PREFETCH  >> FTNSHFT) & 0xff: return "prefetch";
      case (OP2E3     >> FTNSHFT) & 0xff: return "op2e3";
      case (OP2F3     >> FTNSHFT) & 0xff: return "op2f3";
      case (LDFA      >> FTNSHFT) & 0xff: return "ldfa";
      case (OP313     >> FTNSHFT) & 0xff: return "op313";
      case (LDQFA     >> FTNSHFT) & 0xff: return "ldqfa";
      case (LDDFA     >> FTNSHFT) & 0xff: return "lddfa";
      case (STFA      >> FTNSHFT) & 0xff: return "stfa";
      case (OP353     >> FTNSHFT) & 0xff: return "op353";
      case (STQFA     >> FTNSHFT) & 0xff: return "stqfa";
      case (STDFA     >> FTNSHFT) & 0xff: return "stdfa";
      case (OP383     >> FTNSHFT) & 0xff: return "op383";
      case (OP393     >> FTNSHFT) & 0xff: return "op393";
      case (OP3A3     >> FTNSHFT) & 0xff: return "op3a3";
      case (OP3B3     >> FTNSHFT) & 0xff: return "op3b3";
      case (CASA      >> FTNSHFT) & 0xff: return "case";
      case (PREFETCHA >> FTNSHFT) & 0xff: return "prefetcha";
      case (CASXA     >> FTNSHFT) & 0xff: return "casxa";
      case (OP3F3     >> FTNSHFT) & 0xff: return "op3f3";
      }
      break;
    case 15: return "nop\t! 0x" + Integer.toHexString(opcode) + " ";
    }
    throw new scale.common.InternalError("Invalid sparc opcode " +
                                         fmt +
                                         " " +
                                         ftn +
                                         " " +
                                         subop);
  }

  /**
   * Return true if the operation defines the register.
   * @param opcode specifies the instruction
   * @param instr specifies the register encoded in the instruction as
   * the destination register
   * @param reg is the register being tested
   */
  public static boolean defs(int opcode, int instr, int reg)
  {
    if (instr == reg)
      return true;

    int fmt   = (opcode >> FMTSHFT) & FMTMASK;
    int ftn   = (opcode >> FTNSHFT) & FTNMASK;
    int cond  = (opcode >> CNDSHFT) & CNDMASK;
    int subop = (opcode >> SOPSHFT) & SOPMASK;

    switch (fmt) {
    case 2: 
      switch (ftn) {
      case (FPop1    >> FTNSHFT) & 0xff:
        int col34 = (subop >> 0) & 0x0f;
        int row34 = (subop >> 4) & 0xff;
        switch (row34) {
        case 0X00:
          switch (col34) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fnegd */ return ((instr + 1) == reg);
          case 0X7: /*fnegq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XA: /*fabsd */ return ((instr + 1) == reg);
          case 0XB: /*fabsq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X02:
          switch (col34) {
          case 0XA: /*fsqrtd */ return ((instr + 1) == reg);
          case 0XB: /*fsqrtq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X04:
          switch (col34) {
          case 0X2: /*faddd */ return ((instr + 1) == reg);
          case 0X3: /*faddq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fsubd */ return ((instr + 1) == reg);
          case 0X7: /*fsubq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XA: /*fmuld */ return ((instr + 1) == reg);
          case 0XB: /*fmulq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XE: /*fdivd */ return ((instr + 1) == reg);
          case 0XF: /*fdivq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X06:
          switch (col34) {
          case 0X9: /*fsmuld */ return ((instr + 1) == reg);
          case 0XE: /*fdmulq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X08:
          switch (col34) {
          case 0X8: /*fxtod */ return ((instr + 1) == reg);
          case 0XC: /*fxtoq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0C:
          switch (col34) {
          case 0X8: /*fitod */ return ((instr + 1) == reg);
          case 0X9: /*fstod */ return ((instr + 1) == reg);
          case 0XB: /*fqtod */ return ((instr + 1) == reg);
          case 0XC: /*fitoq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XD: /*fstoq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XE: /*fdtoq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        default: return false;
        }
      case (FPop2    >> FTNSHFT) & 0xff:
        int col35 = (subop >> 0) & 0x0f;
        int row35 = (subop >> 4) & 0xFf;
        switch (row35) {
        case 0X00:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X02:
          switch (col35) {
          case 0X6: /*fmovrdz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X04:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fmovrdlez */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqlez */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X06:
          switch (col35) {
          case 0X6: /*fmovrdlz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqlz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X08:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0A:
          switch (col35) {
          case 0X6: /*fmovrdnz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqnz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0C:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fmovrdgz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqgz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0E:
          switch (col35) {
          case 0X6: /*fmovrdgez */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqgez */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X10:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X18:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        default: return false;
        }
        default: return false;
      }
    case 3:
      switch (ftn) {
      case (LDUW      >> FTNSHFT) & 0xff:
      case (LDUB      >> FTNSHFT) & 0xff:
      case (LDUH      >> FTNSHFT) & 0xff:
      case (LD        >> FTNSHFT) & 0xff:
      case (LDSB      >> FTNSHFT) & 0xff:
      case (LDSH      >> FTNSHFT) & 0xff:
      case (LDUWA     >> FTNSHFT) & 0xff:
      case (LDUBA     >> FTNSHFT) & 0xff:
      case (LDUHA     >> FTNSHFT) & 0xff:
      case (LDSWA     >> FTNSHFT) & 0xff:
      case (LDSBA     >> FTNSHFT) & 0xff:
      case (LDSHA     >> FTNSHFT) & 0xff:
      case (LDF       >> FTNSHFT) & 0xff:
      case (LDFSR     >> FTNSHFT) & 0xff:
        return (instr == reg);
      case (LDD       >> FTNSHFT) & 0xff:
      case (LDDA      >> FTNSHFT) & 0xff:
      case (LDDF      >> FTNSHFT) & 0xff:
      case (LDDFA     >> FTNSHFT) & 0xff:
        return (instr <= reg) && (reg <= instr + 1);
      case (LDQF      >> FTNSHFT) & 0xff:
      case (LDQFA     >> FTNSHFT) & 0xff:
        return (instr <= reg) && (reg <= instr + 3);
      default: return false;
      }
      default: return false;
    }
  }

  /**
   * Return true if the operation uses the register.
   * @param opcode specifies the instruction
   * @param instr specifies the register encoded in the instruction as
   * a source register
   * @param reg is the register being tested
   */
  public static boolean uses(int opcode, int instr, int reg)
  {
    if (instr == reg)
      return true;

    int fmt   = (opcode >> FMTSHFT) & FMTMASK;
    int ftn   = (opcode >> FTNSHFT) & FTNMASK;
    int cond  = (opcode >> CNDSHFT) & CNDMASK;
    int subop = (opcode >> SOPSHFT) & SOPMASK;

    switch (fmt) {
    case 2:
      switch (ftn) {
      case (FPop1    >> FTNSHFT) & 0xff:
        int col34 = (subop >> 0) & 0x0f;
        int row34 = (subop >> 4) & 0xff;
        switch (row34) {
        case 0X00:
          switch (col34) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fnegd */ return ((instr + 1) == reg);
          case 0X7: /*fnegq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XA: /*fabsd */ return ((instr + 1) == reg);
          case 0XB: /*fabsq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X02:
          switch (col34) {
          case 0XA: /*fsqrtd */ return ((instr + 1) == reg);
          case 0XB: /*fsqrtq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X04:
          switch (col34) {
          case 0X2: /*faddd */ return ((instr + 1) == reg);
          case 0X3: /*faddq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fsubd */ return ((instr + 1) == reg);
          case 0X7: /*fsubq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XA: /*fmuld */ return ((instr + 1) == reg);
          case 0XB: /*fmulq */ return (instr <= reg) && (reg <= instr + 3);
          case 0XE: /*fdivd */ return ((instr + 1) == reg);
          case 0XF: /*fdivq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X06:
          switch (col34) {
          case 0XE: /*fdmulq */ return ((instr + 1) == reg);
          default: return false;
          }
        case 0X08:
          switch (col34) {
          case 0X2: /*fdtox */ return ((instr + 1) == reg);
          case 0X3: /*fqtox */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0C:
          switch (col34) {
          case 0X6: /*fdtos */ return ((instr + 1) == reg);
          case 0X7: /*fqtos */ return (instr <= reg) && (reg <= instr + 3);
          case 0XB: /*fqtod */ return (instr <= reg) && (reg <= instr + 3);
          case 0XE: /*fdtoq */ return ((instr + 1) == reg);
          default: return false;
          }
        case 0X0D:
          switch (col34) {
          case 0X2: /*fdtoi */ return ((instr + 1) == reg);
          case 0X3: /*fqtoi */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        default: return false;
        }
      case (FPop2    >> FTNSHFT) & 0xff:
        int col35 = (subop >> 0) & 0x0f;
        int row35 = (subop >> 4) & 0xFf;
        switch (row35) {
        case 0X00:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X02:
          switch (col35) {
          case 0X6: /*fmovrdz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X04:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fmovrdlez */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqlez */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X05:
          switch (col35) {
          case 0X2: /*fcmpd */ return ((instr + 1) == reg);
          case 0X3: /*fcmpq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fcmpds */ return ((instr + 1) == reg);
          case 0X7: /*fcmpqs */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X06:
          switch (col35) {
          case 0X6: /*fmovrdlz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqlz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X08:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0A:
          switch (col35) {
          case 0X6: /*fmovrdnz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqnz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0C:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          case 0X6: /*fmovrdgz */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqgz */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X0E:
          switch (col35) {
          case 0X6: /*fmovrdgez */ return ((instr + 1) == reg);
          case 0X7: /*fmovrqgez */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X10:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        case 0X18:
          switch (col35) {
          case 0X2: /*fmovd */ return ((instr + 1) == reg);
          case 0X3: /*fmovq */ return (instr <= reg) && (reg <= instr + 3);
          default: return false;
          }
        default: return false;
        }
      default: return false;
      }
    case 3:
      switch (ftn) {
      case (ST        >> FTNSHFT) & 0xff:
      case (STB       >> FTNSHFT) & 0xff:
      case (STH       >> FTNSHFT) & 0xff:
      case (STWA      >> FTNSHFT) & 0xff:
      case (STBA      >> FTNSHFT) & 0xff:
      case (STHA      >> FTNSHFT) & 0xff:
      case (STF       >> FTNSHFT) & 0xff:
      case (STFSR     >> FTNSHFT) & 0xff:
      case (STFA      >> FTNSHFT) & 0xff:
        return (instr == reg);
      case (STD       >> FTNSHFT) & 0xff:
      case (STDA      >> FTNSHFT) & 0xff:
      case (STDF      >> FTNSHFT) & 0xff:
      case (STDFA     >> FTNSHFT) & 0xff:
        return (instr <= reg) && (reg <= instr + 1);
      case (STQF      >> FTNSHFT) & 0xff:
      case (STQFA     >> FTNSHFT) & 0xff:
        return (instr <= reg) && (reg <= instr + 3);
      default: return false;
      }
    default: return false;
    }
  }

  /**
   * Return true if the instruction sets the CC flag specified.
   * @param cc specifies the cc flag
   * @param opcode is the instruction opcode
   */
  public static boolean setsCC(int cc, int opcode)
  {
    int fmt = (opcode >> FMTSHFT) & FMTMASK;

    if (fmt != 2)
      return false;

    // Arithmetic & Miscellaneous

    int ftn = (opcode >> FTNSHFT) & FTNMASK;
    switch (cc) {
    case SparcGenerator.ICC:
    case SparcGenerator.XCC: 
      // See table 32 in "The Sparc Architecture Manual V9".
      int col = (ftn >> 4) & 0x3;
      if (col == 1)
        return true;
      int row = (ftn & 0xf);
      return ((col == 2) && (row < 5));
    case SparcGenerator.FCC0:
    case SparcGenerator.FCC1:
    case SparcGenerator.FCC2:
    case SparcGenerator.FCC3:
      if (ftn != 0x35)
        return false;
      // We really should have the cc0 & cc1 fields from the
      // instruction here!  See table 35 in "The Sparc Architecture
      // Manual V9".
      int subop = (opcode >> SOPSHFT) & SOPMASK;
      return (((subop >> 4) & 0x1f) == 5); // True if FCMP
    }
    throw new scale.common.InternalError("Unknown CC " + cc);
  }

  /**
   * Return true if the instruction uses the CC flag specified.
   * @param cc specifies the cc flag
   */
  public static boolean usesCC(int cc, int opcode)
  {
    int fmt = (opcode >> FMTSHFT) & FMTMASK;
    int ftn = (opcode >> FTNSHFT) & FTNMASK;

    if (fmt == 2) { // Arithmetic & Miscellaneous
      switch (cc) {
      case SparcGenerator.ICC:
      case SparcGenerator.XCC:
        // See table 32 in "The Sparc Architecture Manual V9".
        if (ftn == 0x2c)
          return true;
        if (((ftn >> 4) & 0x3) > 1)
          return false;
        int rowi = (ftn & 0xf);
        return ((rowi == 8) || (rowi == 12));
      case SparcGenerator.FCC0:
      case SparcGenerator.FCC1:
      case SparcGenerator.FCC2:
      case SparcGenerator.FCC3:
        if (ftn != 0x35)
          return false;
        // We really should have the cc0 & cc1 fields from the
        // instruction here!  See table 35 in "The Sparc Architecture
        // Manual V9".
        int subop = (opcode >> SOPSHFT) & SOPMASK;
        int col = subop & 0xf;
        if (col > 3)
          return false;
        int row = (subop >> 4) & 0x1f;
        return (row != 5);
      }
      throw new scale.common.InternalError("Unknown CC " + cc);
    } else if (fmt == 1) { // Branches
      switch (cc) {
      case SparcGenerator.ICC:
      case SparcGenerator.XCC:
        // We really should have the cc0 & cc1 fields from the
        // instruction here!  See table 31 in "The Sparc Architecture
        // Manual V9".
        return ((ftn == 1) || (ftn == 2));
      case SparcGenerator.FCC0:
      case SparcGenerator.FCC1:
      case SparcGenerator.FCC2:
      case SparcGenerator.FCC3:
        // We really should have the cc0 & cc1 fields from the
        // instruction here!  See table 31 in "The Sparc Architecture
        // Manual V9".
        return ((ftn == 5) || (ftn == 6));
      }
      throw new scale.common.InternalError("Unknown CC " + cc);     
    }
    return false;
  }
}
