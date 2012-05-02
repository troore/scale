package scale.backend.ppc;

import scale.common.*;
import scale.backend.*;

/** 
 * This class provides PPC instruction information.
 * <p>
 * $Id: Opcodes.java,v 1.14 2006-10-04 13:59:22 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class Opcodes
{
  /**
   * A regular instruction like add.
   */
  public static final int REGULAR = 0;
  /**
   * An instruction that loads from memory.
   */
  public static final int LOAD    = 1;
  /**
   * An instruction that stores to memory.
   */
  public static final int STORE   = 2;
  /**
   * An instruction branches.
   */
  public static final int BRANCH  = 3;
  /**
    * Special Purpose Register (SPR) number for Link Register
        */
  public static final int LR_SPR   = 8;
  /**
    * Special Purpose Register number for counter
    */
  public static final int CTR_SPR  = 9;
  
  /**
   * The different types of branch instructions.
   */
  public static final int BRANCH_ALWAYS = 20;
  public static final int BRANCH_TRUE   = 12;
  public static final int BRANCH_FALSE  = 4;
  /**
   * Condition register bit meanings
   */
  public static final int LT            = 0;
  public static final int GT            = 1;
  public static final int EQ            = 2;
  public static final int SO            = 3; // summary overflow bit
  public static final int FPE           = 4; // floating point exception
  public static final int FPEE          = 5; // floating point enabled exception
  public static final int FPIOE         = 6; // floating point invalid operation exception
  public static final int FPOE          = 7; // floating point overflow expcetion
  

  /**
   * The different instruction forms.
   */
  public static final int A_FORM   = 1;
  public static final int B_FORM   = 2;
  public static final int D_FORM   = 3;
  public static final int DS_FORM  = 4;
  public static final int I_FORM   = 5;
  public static final int M_FORM   = 6;
  public static final int MD_FORM  = 7;
  public static final int MDS_FORM = 8;
  public static final int SC_FORM  = 9;
  public static final int X_FORM   = 10;
  public static final int XFL_FORM = 11;
  public static final int XFX_FORM = 12;
  public static final int XL_FORM  = 13;
  public static final int XO_FORM  = 14;
  public static final int XS_FORM  = 15;

  /**
   * These are the Scale opcodes for the PowerPC instructions.  These
   * "opcodes" are meant to be used to index into tables to access the
   * actual hardware opcode / extendex opcode, etc.  They are defined
   * here in a more convenient form than the actual hardware opcodes.
   */
  public static final int ADD = 1;
  public static final int ADDP = 2;
  public static final int ADDC = 3;
  public static final int ADDCP = 4;
  public static final int ADDCO = 5;
  public static final int ADDCOP = 6;
  public static final int ADDE = 7;
  public static final int ADDEP = 8;
  public static final int ADDEO = 9;
  public static final int ADDEOP = 10;
  public static final int ADDI = 11;
  public static final int ADDIC = 12;
  public static final int ADDICP = 13;
  public static final int ADDIS = 14;
  public static final int ADDME = 15;
  public static final int ADDMEP = 16;
  public static final int ADDMEO = 17;
  public static final int ADDMEOP = 18;
  public static final int ADDO = 19;
  public static final int ADDOP = 20;
  public static final int ADDZE = 21;
  public static final int ADDZEP = 22;
  public static final int ADDZEO = 23;
  public static final int ADDZEOP = 24;
  public static final int AND = 25;
  public static final int ANDP = 26;
  public static final int ANDC = 27;
  public static final int ANDCP = 28;
  public static final int ANDIP = 29;
  public static final int ANDISP = 30;
  public static final int B = 31;
  public static final int BA = 32;
  public static final int BC = 33;
  public static final int BCA = 34;
  public static final int BCCTR = 35;
  public static final int BCCTRL = 36;
  public static final int BCL = 37;
  public static final int BCLA = 38;
  public static final int BCLR = 39;
  public static final int BCLRL = 40;
  public static final int BL = 41;
  public static final int BLA = 42;
  public static final int CMPD = 43;
  public static final int CMPDI = 44;
  public static final int CMPLD = 45;
  public static final int CMPLDI = 46;
  public static final int CMPLW = 47;
  public static final int CMPLWI = 48;
  public static final int CMPW = 49;
  public static final int CMPWI = 50;
  public static final int CNTLZD = 51;
  public static final int CNTLZDP = 52;
  public static final int CNTLZW = 53;
  public static final int CNTLZWP = 54;
  public static final int CRAND = 55;
  public static final int CRANDC = 56;
  public static final int CREQV = 57;
  public static final int CRNAND = 58;
  public static final int CRNOR = 59;
  public static final int CROR = 60;
  public static final int CRORC = 61;
  public static final int CRXOR = 62;
  public static final int DCBA = 63;
  public static final int DCBF = 64;
  public static final int DCBI = 65;
  public static final int DCBST = 66;
  public static final int DCBT = 67;
  public static final int DCBTST = 68;
  public static final int DCBZ = 69;
  public static final int DIVD = 70;
  public static final int DIVDP = 71;
  public static final int DIVDO = 72;
  public static final int DIVDOP = 73;
  public static final int DIVDU = 74;
  public static final int DIVDUP = 75;
  public static final int DIVDUO = 76;
  public static final int DIVDUOP = 77;
  public static final int DIVW = 78;
  public static final int DIVWP = 79;
  public static final int DIVWO = 80;
  public static final int DIVWOP = 81;
  public static final int DIVWU = 82;
  public static final int DIVWUP = 83;
  public static final int DIVWUO = 84;
  public static final int DIVWUOP = 85;
  public static final int ECIWX = 86;
  public static final int ECOWX = 87;
  public static final int EIEIO = 88;
  public static final int EQV = 89;
  public static final int EQVP = 90;
  public static final int EXTSB = 91;
  public static final int EXTSBP = 92;
  public static final int EXTSH = 93;
  public static final int EXTSHP = 94;
  public static final int EXTSW = 95;
  public static final int EXTSWP = 96;
  public static final int FABS = 97;
  public static final int FABSP = 98;
  public static final int FADD = 99;
  public static final int FADDP = 100;
  public static final int FADDS = 101;
  public static final int FADDSP = 102;
  public static final int FCFID = 103;
  public static final int FCFIDP = 104;
  public static final int FCMPO = 105;
  public static final int FCMPU = 106;
  public static final int FCTID = 107;
  public static final int FCTIDP = 108;
  public static final int FCTIDZ = 109;
  public static final int FCTIDZP = 110;
  public static final int FCTIW = 111;
  public static final int FCTIWP = 112;
  public static final int FCTIWZ = 113;
  public static final int FCTIWZP = 114;
  public static final int FDIV = 115;
  public static final int FDIVP = 116;
  public static final int FDIVS = 117;
  public static final int FDIVSP = 118;
  public static final int FMADD = 119;
  public static final int FMADDP = 120;
  public static final int FMADDS = 121;
  public static final int FMADDSP = 122;
  public static final int FMR = 123;
  public static final int FMRP = 124;
  public static final int FMSUB = 125;
  public static final int FMSUBP = 126;
  public static final int FMSUBS = 127;
  public static final int FMSUBSP = 128;
  public static final int FMUL = 129;
  public static final int FMULP = 130;
  public static final int FMULS = 131;
  public static final int FMULSP = 132;
  public static final int FNABS = 133;
  public static final int FNABSP = 134;
  public static final int FNEG = 135;
  public static final int FNEGP = 136;
  public static final int FNMADD = 137;
  public static final int FNMADDP = 138;
  public static final int FNMADDS = 139;
  public static final int FNMADDSP = 140;
  public static final int FNMSUB = 141;
  public static final int FNMSUBP = 142;
  public static final int FNMSUBS = 143;
  public static final int FNMSUBSP = 144;
  public static final int FRES = 145;
  public static final int FRESP = 146;
  public static final int FRSP = 147;
  public static final int FRSQRTE = 148;
  public static final int FRSQRTEP = 149;
  public static final int FSEL = 150;
  public static final int FSELP = 151;
  public static final int FSQRT = 152;
  public static final int FSQRTP = 153;
  public static final int FSQRTS = 154;
  public static final int FSQRTSP = 155;
  public static final int FSUB = 156;
  public static final int FSUBP = 157;
  public static final int FSUBS = 158;
  public static final int FSUBSP = 159;
  public static final int ICBI = 160;
  public static final int ISYNC = 161;
  public static final int LBZ = 162;
  public static final int LBZU = 163;
  public static final int LBZUX = 164;
  public static final int LBZX = 165;
  public static final int LD = 166;
  public static final int LDARX = 167;
  public static final int LDU = 168;
  public static final int LDUX = 169;
  public static final int LDX = 170;
  public static final int LFD = 171;
  public static final int LFDU = 172;
  public static final int LFDUX = 173;
  public static final int LFDX = 174;
  public static final int LFS = 175;
  public static final int LFSU = 176;
  public static final int LFSUX = 177;
  public static final int LFSX = 178;
  public static final int LHA = 179;
  public static final int LHAU = 180;
  public static final int LHAUX = 181;
  public static final int LHAX = 182;
  public static final int LHBRX = 183;
  public static final int LHZ = 184;
  public static final int LHZU = 185;
  public static final int LHZUX = 186;
  public static final int LHZX = 187;
  public static final int LMW = 188;
  public static final int LSWI = 189;
  public static final int LSWX = 190;
  public static final int LWA = 191;
  public static final int LWARX = 192;
  public static final int LWAUX = 193;
  public static final int LWAX = 194;
  public static final int LWBRX = 195;
  public static final int LWZ = 196;
  public static final int LWZU = 197;
  public static final int LWZUX = 198;
  public static final int LWZX = 199;
  public static final int MCRF = 200;
  public static final int MCRFS = 201;
  public static final int MCRXR = 202;
  public static final int MFCR = 203;
  public static final int MFFS = 204;
  public static final int MFFSP = 205;
  public static final int MFMSR = 206;
  public static final int MFSPR = 207;
  public static final int MFSR = 208;
  public static final int MFSRIN = 209;
  public static final int MFTB = 210;
  public static final int MTCRF = 211;
  public static final int MTFSB0 = 212;
  public static final int MTFSB0P = 213;
  public static final int MTFSB1 = 214;
  public static final int MTFSB1P = 215;
  public static final int MTFSF = 216;
  public static final int MTFSFP = 217;
  public static final int MTFSFI = 218;
  public static final int MTFSFIP = 219;
  public static final int MTMSR = 220;
  public static final int MTMSRD = 221;
  public static final int MTSPR = 222;
  public static final int MTSR = 223;
  public static final int MTSRD = 224;
  public static final int MTSRDIN = 225;
  public static final int MTSRIN = 226;
  public static final int MULHD = 227;
  public static final int MULHDP = 228;
  public static final int MULHDU = 229;
  public static final int MULHDUP = 230;
  public static final int MULHW = 231;
  public static final int MULHWP = 232;
  public static final int MULHWU = 233;
  public static final int MULHWUP = 234;
  public static final int MULLD = 235;
  public static final int MULLDP = 236;
  public static final int MULLDO = 237;
  public static final int MULLDOP = 238;
  public static final int MULLI = 239;
  public static final int MULLW = 240;
  public static final int MULLWP = 241;
  public static final int MULLWO = 242;
  public static final int MULLWOP = 243;
  public static final int NAND = 244;
  public static final int NANDP = 245;
  public static final int NEG = 246;
  public static final int NEGP = 247;
  public static final int NEGO = 248;
  public static final int NEGOP = 249;
  public static final int NOR = 250;
  public static final int NORP = 251;
  public static final int OR = 252;
  public static final int ORP = 253;
  public static final int ORC = 254;
  public static final int ORCP = 255;
  public static final int ORI = 256;
  public static final int ORIS = 257;
  public static final int RFI = 258;
  public static final int RFID = 259;
  public static final int RLDCL = 260;
  public static final int RLDCLP = 261;
  public static final int RLDCR = 262;
  public static final int RLDCRP = 263;
  public static final int RLDIC = 264;
  public static final int RLDICP = 265;
  public static final int RLDICL = 266;
  public static final int RLDICLP = 267;
  public static final int RLDICR = 268;
  public static final int RLDICRP = 269;
  public static final int RLDIMI = 270;
  public static final int RLDIMIP = 271;
  public static final int RLWIMI = 272;
  public static final int RLWIMIP = 273;
  public static final int RLWINM = 274;
  public static final int RLWINMP = 275;
  public static final int RLWNM = 276;
  public static final int RLWNMP = 277;
  public static final int SC = 278;
  public static final int SLBIA = 279;
  public static final int SLBIE = 280;
  public static final int SLD = 281;
  public static final int SLDP = 282;
  public static final int SLW = 283;
  public static final int SLWP = 284;
  public static final int SRAD = 285;
  public static final int SRADP = 286;
  public static final int SRADI = 287;
  public static final int SRADIP = 288;
  public static final int SRAW = 289;
  public static final int SRAWP = 290;
  public static final int SRAWI = 291;
  public static final int SRAWIP = 292;
  public static final int SRD = 293;
  public static final int SRDP = 294;
  public static final int SRW = 295;
  public static final int SRWP = 296;
  public static final int STB = 297;
  public static final int STBU = 298;
  public static final int STBUX = 299;
  public static final int STBX = 300;
  public static final int STD = 301;
  public static final int STDCXP = 302;
  public static final int STDU = 303;
  public static final int STDUX = 304;
  public static final int STDX = 305;
  public static final int STFD = 306;
  public static final int STFDU = 307;
  public static final int STFDUX = 308;
  public static final int STFDX = 309;
  public static final int STFIWX = 310;
  public static final int STFS = 311;
  public static final int STFSU = 312;
  public static final int STFSUX = 313;
  public static final int STFSX = 314;
  public static final int STH = 315;
  public static final int STHBRX = 316;
  public static final int STHU = 317;
  public static final int STHUX = 318;
  public static final int STHX = 319;
  public static final int STMW = 320;
  public static final int STSWI = 321;
  public static final int STSWX = 322;
  public static final int STW = 323;
  public static final int STWBRX = 324;
  public static final int STWCXP = 325;
  public static final int STWU = 326;
  public static final int STWUX = 327;
  public static final int STWX = 328;
  public static final int SUBF = 329;
  public static final int SUBFP = 330;
  public static final int SUBFC = 331;
  public static final int SUBFCP = 332;
  public static final int SUBFCO = 333;
  public static final int SUBFCOP = 334;
  public static final int SUBFE = 335;
  public static final int SUBFEP = 336;
  public static final int SUBFEO = 337;
  public static final int SUBFEOP = 338;
  public static final int SUBFIC = 339;
  public static final int SUBFME = 340;
  public static final int SUBFMEP = 341;
  public static final int SUBFMEO = 342;
  public static final int SUBFMEOP = 343;
  public static final int SUBFO = 344;
  public static final int SUBFOP = 345;
  public static final int SUBFZE = 346;
  public static final int SUBFZEP = 347;
  public static final int SUBFZEO = 348;
  public static final int SUBFZEOP = 349;
  public static final int SYNC = 350;
  public static final int TD = 351;
  public static final int TDI = 352;
  public static final int TLBIA = 353;
  public static final int TLBIE = 354;
  public static final int TLBSYNC = 355;
  public static final int TW = 356;
  public static final int TWI = 357;
  public static final int XOR = 358;
  public static final int XORP = 359;
  public static final int XORI = 360;
  public static final int XORIS = 361;
  public static final int LA = 362;
  public static final int LI = 363;
  public static final int LIS = 364;
  public static final int MR = 365;
  public static final int SLWI = 366;

  /**
   * Index by the opcode to obtain the string to use for the assembly code.
   */
  public static final String[] opcodes = {
    "??",
    "add",
    "add.",
    "addc",
    "addc.",
    "addco",
    "addco.",
    "adde",
    "adde.",
    "addeo",
    "addeo.",
    "addi",
    "addic",
    "addic.",
    "addis",
    "addme",
    "addme.",
    "addmeo",
    "addmeo.",
    "addo",
    "addo.",
    "addze",
    "addze.",
    "addzeo",
    "addzeo.",
    "and",
    "and.",
    "andc",
    "andc.",
    "andi.",
    "andis.",
    "b",
    "ba",
    "bc",
    "bca",
    "bcctr",
    "bcctrl",
    "bcl",
    "bcla",
    "bclr",
    "bclrl",
    "bl",
    "bla",
    "cmpd",
    "cmpdi",
    "cmpld",
    "cmpldi",
    "cmplw",
    "cmplwi",
    "cmpw",
    "cmpwi",
    "cntlzd",
    "cntlzd.",
    "cntlzw",
    "cntlzw.",
    "crand",
    "crandc",
    "creqv",
    "crnand",
    "crnor",
    "cror",
    "crorc",
    "crxor",
    "dcba",
    "dcbf",
    "dcbi",
    "dcbst",
    "dcbt",
    "dcbtst",
    "dcbz",
    "divd",
    "divd.",
    "divdo",
    "divdo.",
    "divdu",
    "divdu.",
    "divduo",
    "divduo.",
    "divw",
    "divw.",
    "divwo",
    "divwo.",
    "divwu",
    "divwu.",
    "divwuo",
    "divwuo.",
    "eciwx",
    "ecowx",
    "eieio",
    "eqv",
    "eqv.",
    "extsb",
    "extsb.",
    "extsh",
    "extsh.",
    "extsw",
    "extsw.",
    "fabs",
    "fabs.",
    "fadd",
    "fadd.",
    "fadds",
    "fadds.",
    "fcfid",
    "fcfid.",
    "fcmpo",
    "fcmpu",
    "fctid",
    "fctid.",
    "fctidz",
    "fctidz.",
    "fctiw",
    "fctiw.",
    "fctiwz",
    "fctiwz.",
    "fdiv",
    "fdiv.",
    "fdivs",
    "fdivs.",
    "fmadd",
    "fmadd.",
    "fmadds",
    "fmadds.",
    "fmr",
    "fmr.",
    "fmsub",
    "fmsub.",
    "fmsubs",
    "fmsubs.",
    "fmul",
    "fmul.",
    "fmuls",
    "fmuls.",
    "fnabs",
    "fnabs.",
    "fneg",
    "fneg.",
    "fnmadd",
    "fnmadd.",
    "fnmadds",
    "fnmadds.",
    "fnmsub",
    "fnmsub.",
    "fnmsubs",
    "fnmsubs.",
    "fres",
    "fres.",
    "frsp",
    "frsqrte",
    "frsqrte.",
    "fsel",
    "fsel.",
    "fsqrt",
    "fsqrt.",
    "fsqrts",
    "fsqrts.",
    "fsub",
    "fsub.",
    "fsubs",
    "fsubs.",
    "icbi",
    "isync",
    "lbz",
    "lbzu",
    "lbzux",
    "lbzx",
    "ld",
    "ldarx",
    "ldu",
    "ldux",
    "ldx",
    "lfd",
    "lfdu",
    "lfdux",
    "lfdx",
    "lfs",
    "lfsu",
    "lfsux",
    "lfsx",
    "lha",
    "lhau",
    "lhaux",
    "lhax",
    "lhbrx",
    "lhz",
    "lhzu",
    "lhzux",
    "lhzx",
    "lmw",
    "lswi",
    "lswx",
    "lwa",
    "lwarx",
    "lwaux",
    "lwax",
    "lwbrx",
    "lwz",
    "lwzu",
    "lwzux",
    "lwzx",
    "mcrf",
    "mcrfs",
    "mcrxr",
    "mfcr",
    "mffs",
    "mffs.",
    "mfmsr",
    "mfspr",
    "mfsr",
    "mfsrin",
    "mftb",
    "mtcrf",
    "mtfsb0",
    "mtfsb0.",
    "mtfsb1",
    "mtfsb1.",
    "mtfsf",
    "mtfsf.",
    "mtfsfi",
    "mtfsfi.",
    "mtmsr",
    "mtmsrd",
    "mtspr",
    "mtsr",
    "mtsrd",
    "mtsrdin",
    "mtsrin",
    "mulhd",
    "mulhd.",
    "mulhdu",
    "mulhdu.",
    "mulhw",
    "mulhw.",
    "mulhwu",
    "mulhwu.",
    "mulld",
    "mulld.",
    "mulldo",
    "mulldo.",
    "mulli",
    "mullw",
    "mullw.",
    "mullwo",
    "mullwo.",
    "nand",
    "nand.",
    "neg",
    "neg.",
    "nego",
    "nego.",
    "nor",
    "nor.",
    "or",
    "or.",
    "orc",
    "orc.",
    "ori",
    "oris",
    "rfi",
    "rfid",
    "rldcl",
    "rldcl.",
    "rldcr",
    "rldcr.",
    "rldic",
    "rldic.",
    "rldicl",
    "rldicl.",
    "rldicr",
    "rldicr.",
    "rldimi",
    "rldimi.",
    "rlwimi",
    "rlwimi.",
    "rlwinm",
    "rlwinm.",
    "rlwnm",
    "rlwnm.",
    "sc",
    "slbia",
    "slbie",
    "sld",
    "sld.",
    "slw",
    "slw.",
    "srad",
    "srad.",
    "sradi",
    "sradi.",
    "sraw",
    "sraw.",
    "srawi",
    "srawi.",
    "srd",
    "srd.",
    "srw",
    "srw.",
    "stb",
    "stbu",
    "stbux",
    "stbx",
    "std",
    "stdcx.",
    "stdu",
    "stdux",
    "stdx",
    "stfd",
    "stfdu",
    "stfdux",
    "stfdx",
    "stfiwx",
    "stfs",
    "stfsu",
    "stfsux",
    "stfsx",
    "sth",
    "sthbrx",
    "sthu",
    "sthux",
    "sthx",
    "stmw",
    "stswi",
    "stswx",
    "stw",
    "stwbrx",
    "stwcx.",
    "stwu",
    "stwux",
    "stwx",
    "subf",
    "subf.",
    "subfc",
    "subfc.",
    "subfco",
    "subfco.",
    "subfe",
    "subfe.",
    "subfeo",
    "subfeo.",
    "subfic",
    "subfme",
    "subfme.",
    "subfmeo",
    "subfmeo.",
    "subfo",
    "subfo.",
    "subfze",
    "subfze.",
    "subfzeo",
    "subfzeo.",
    "sync",
    "td",
    "tdi",
    "tlbia",
    "tlbie",
    "tlbsync",
    "tw",
    "twi",
    "xor",
    "xor.",
    "xori",
    "xoris",
    "la",
    "li",
    "lis",
    "mr",
    "slwi"
  };

  /**
   * Map from instruction opcode to instruction form.
   */
  public static final byte[] instForm = {
    0,       // ??
    XO_FORM, // add
    XO_FORM, // add.
    XO_FORM, // addc
    XO_FORM, // addc.
    XO_FORM, // addco
    XO_FORM, // addco.
    XO_FORM, // adde
    XO_FORM, // adde.
    XO_FORM, // addeo
    XO_FORM, // addeo.
    D_FORM, // addi
    D_FORM, // addic
    D_FORM, // addic.
    D_FORM, // addis
    XO_FORM, // addme
    XO_FORM, // addme.
    XO_FORM, // addmeo
    XO_FORM, // addmeo.
    XO_FORM, // addo
    XO_FORM, // addo.
    XO_FORM, // addze
    XO_FORM, // addze.
    XO_FORM, // addzeo
    XO_FORM, // addzeo.
    X_FORM, // and
    X_FORM, // and.
    X_FORM, // andc
    X_FORM, // andc.
    D_FORM, // andi.
    D_FORM, // andis.
    I_FORM, // b
    I_FORM, // ba
    B_FORM, // bc
    B_FORM, // bca
    XL_FORM, // bcctr
    XL_FORM, // bcctrl
    B_FORM, // bcl
    B_FORM, // bcla
    XL_FORM, // bclr
    XL_FORM, // bclrl
    I_FORM, // bl
    I_FORM, // bla
    X_FORM, // cmpd
    D_FORM, // cmpdi
    X_FORM, // cmpld
    D_FORM, // cmpldi
    X_FORM, // cmplw
    D_FORM, // cmplwi
    X_FORM, // cmpw
    D_FORM, // cmpwi
    X_FORM, // cntlzd
    X_FORM, // cntlzd.
    X_FORM, // cntlzw
    X_FORM, // cntlzw.
    XL_FORM, // crand
    XL_FORM, // crandc
    XL_FORM, // creqv
    XL_FORM, // crnand
    XL_FORM, // crnor
    XL_FORM, // cror
    XL_FORM, // crorc
    XL_FORM, // crxor
    X_FORM, // dcba
    X_FORM, // dcbf
    X_FORM, // dcbi
    X_FORM, // dcbst
    X_FORM, // dcbt
    X_FORM, // dcbtst
    X_FORM, // dcbz
    XO_FORM, // divd
    XO_FORM, // divd.
    XO_FORM, // divdo
    XO_FORM, // divdo.
    XO_FORM, // divdu
    XO_FORM, // divdu.
    XO_FORM, // divduo
    XO_FORM, // divduo.
    XO_FORM, // divw
    XO_FORM, // divw.
    XO_FORM, // divwo
    XO_FORM, // divwo.
    XO_FORM, // divwu
    XO_FORM, // divwu.
    XO_FORM, // divwuo
    XO_FORM, // divwuo.
    X_FORM, // eciwx
    X_FORM, // ecowx
    X_FORM, // eieio
    X_FORM, // eqv
    X_FORM, // eqv.
    X_FORM, // extsb
    X_FORM, // extsb.
    X_FORM, // extsh
    X_FORM, // extsh.
    X_FORM, // extsw
    X_FORM, // extsw.
    X_FORM, // fabs
    X_FORM, // fabs.
    A_FORM, // fadd
    A_FORM, // fadd.
    A_FORM, // fadds
    A_FORM, // fadds.
    X_FORM, // fcfid
    X_FORM, // fcfid.
    X_FORM, // fcmpo
    X_FORM, // fcmpu
    X_FORM, // fctid
    X_FORM, // fctid.
    X_FORM, // fctidz
    X_FORM, // fctidz.
    X_FORM, // fctiw
    X_FORM, // fctiw.
    X_FORM, // fctiwz
    X_FORM, // fctiwz.
    A_FORM, // fdiv
    A_FORM, // fdiv.
    A_FORM, // fdivs
    A_FORM, // fdivs.
    A_FORM, // fmadd
    A_FORM, // fmadd.
    A_FORM, // fmadds
    A_FORM, // fmadds.
    X_FORM, // fmr
    X_FORM, // fmr.
    A_FORM, // fmsub
    A_FORM, // fmsub.
    A_FORM, // fmsubs
    A_FORM, // fmsubs.
    A_FORM, // fmul
    A_FORM, // fmul.
    A_FORM, // fmuls
    A_FORM, // fmuls.
    X_FORM, // fnabs
    X_FORM, // fnabs.
    X_FORM, // fneg
    X_FORM, // fneg.
    A_FORM, // fnmadd
    A_FORM, // fnmadd.
    A_FORM, // fnmadds
    A_FORM, // fnmadds.
    A_FORM, // fnmsub
    A_FORM, // fnmsub.
    A_FORM, // fnmsubs
    A_FORM, // fnmsubs.
    A_FORM, // fres
    A_FORM, // fres.
    X_FORM, // frsp
    A_FORM, // frsqrte
    A_FORM, // frsqrte.
    A_FORM, // fsel
    A_FORM, // fsel.
    A_FORM, // fsqrt
    A_FORM, // fsqrt.
    A_FORM, // fsqrts
    A_FORM, // fsqrts.
    A_FORM, // fsub
    A_FORM, // fsub.
    A_FORM, // fsubs
    A_FORM, // fsubs.
    X_FORM, // icbi
    XL_FORM, // isync
    D_FORM, // lbz
    D_FORM, // lbzu
    X_FORM, // lbzux
    X_FORM, // lbzx
    DS_FORM, // ld
    X_FORM, // ldarx
    DS_FORM, // ldu
    X_FORM, // ldux
    X_FORM, // ldx
    D_FORM, // lfd
    D_FORM, // lfdu
    X_FORM, // lfdux
    X_FORM, // lfdx
    D_FORM, // lfs
    D_FORM, // lfsu
    X_FORM, // lfsux
    X_FORM, // lfsx
    D_FORM, // lha
    D_FORM, // lhau
    X_FORM, // lhaux
    X_FORM, // lhax
    X_FORM, // lhbrx
    D_FORM, // lhz
    D_FORM, // lhzu
    X_FORM, // lhzux
    X_FORM, // lhzx
    D_FORM, // lmw
    X_FORM, // lswi
    X_FORM, // lswx
    DS_FORM, // lwa
    X_FORM, // lwarx
    X_FORM, // lwaux
    X_FORM, // lwax
    X_FORM, // lwbrx
    D_FORM, // lwz
    D_FORM, // lwzu
    X_FORM, // lwzux
    X_FORM, // lwzx
    XL_FORM, // mcrf
    X_FORM, // mcrfs
    X_FORM, // mcrxr
    X_FORM, // mfcr
    X_FORM, // mffs
    X_FORM, // mffs.
    X_FORM, // mfmsr
    XFX_FORM, // mfspr
    X_FORM, // mfsr
    X_FORM, // mfsrin
    XFX_FORM, // mftb
    XFX_FORM, // mtcrf
    X_FORM, // mtfsb0
    X_FORM, // mtfsb0.
    X_FORM, // mtfsb1
    X_FORM, // mtfsb1.
    XFL_FORM, // mtfsf
    XFL_FORM, // mtfsf.
    X_FORM, // mtfsfi
    X_FORM, // mtfsfi.
    X_FORM, // mtmsr
    X_FORM, // mtmsrd
    XFX_FORM, // mtspr
    X_FORM, // mtsr
    X_FORM, // mtsrd
    X_FORM, // mtsrdin
    X_FORM, // mtsrin
    XO_FORM, // mulhd
    XO_FORM, // mulhd.
    XO_FORM, // mulhdu
    XO_FORM, // mulhdu.
    XO_FORM, // mulhw
    XO_FORM, // mulhw.
    XO_FORM, // mulhwu
    XO_FORM, // mulhwu.
    XO_FORM, // mulld
    XO_FORM, // mulld.
    XO_FORM, // mulldo
    XO_FORM, // mulldo.
    D_FORM, // mulli
    XO_FORM, // mullw
    XO_FORM, // mullw.
    XO_FORM, // mullwo
    XO_FORM, // mullwo.
    X_FORM, // nand
    X_FORM, // nand.
    XO_FORM, // neg
    XO_FORM, // neg.
    XO_FORM, // nego
    XO_FORM, // nego.
    X_FORM, // nor
    X_FORM, // nor.
    X_FORM, // or
    X_FORM, // or.
    X_FORM, // orc
    X_FORM, // orc.
    D_FORM, // ori
    D_FORM, // oris
    XL_FORM, // rfi
    XL_FORM, // rfid
    MDS_FORM, // rldcl
    MDS_FORM, // rldcl.
    MDS_FORM, // rldcr
    MDS_FORM, // rldcr.
    MD_FORM, // rldic
    MD_FORM, // rldic.
    MD_FORM, // rldicl
    MD_FORM, // rldicl.
    MD_FORM, // rldicr
    MD_FORM, // rldicr.
    MD_FORM, // rldimi
    MD_FORM, // rldimi.
    M_FORM, // rlwimi
    M_FORM, // rlwimi.
    M_FORM, // rlwinm
    M_FORM, // rlwinm.
    M_FORM, // rlwnm
    M_FORM, // rlwnm.
    SC_FORM, // sc
    X_FORM, // slbia
    X_FORM, // slbie
    X_FORM, // sld
    X_FORM, // sld.
    X_FORM, // slw
    X_FORM, // slw.
    X_FORM, // srad
    X_FORM, // srad.
    XS_FORM, // sradi
    XS_FORM, // sradi.
    X_FORM, // sraw
    X_FORM, // sraw.
    X_FORM, // srawi
    X_FORM, // srawi.
    X_FORM, // srd
    X_FORM, // srd.
    X_FORM, // srw
    X_FORM, // srw.
    D_FORM, // stb
    D_FORM, // stbu
    X_FORM, // stbux
    X_FORM, // stbx
    DS_FORM, // std
    X_FORM, // stdcx.
    DS_FORM, // stdu
    X_FORM, // stdux
    X_FORM, // stdx
    D_FORM, // stfd
    D_FORM, // stfdu
    X_FORM, // stfdux
    X_FORM, // stfdx
    X_FORM, // stfiwx
    D_FORM, // stfs
    D_FORM, // stfsu
    X_FORM, // stfsux
    X_FORM, // stfsx
    D_FORM, // sth
    X_FORM, // sthbrx
    D_FORM, // sthu
    X_FORM, // sthux
    X_FORM, // sthx
    D_FORM, // stmw
    X_FORM, // stswi
    X_FORM, // stswx
    D_FORM, // stw
    X_FORM, // stwbrx
    X_FORM, // stwcx.
    D_FORM, // stwu
    X_FORM, // stwux
    X_FORM, // stwx
    XO_FORM, // subf
    XO_FORM, // subf.
    XO_FORM, // subfc
    XO_FORM, // subfc.
    XO_FORM, // subfco
    XO_FORM, // subfco.
    XO_FORM, // subfe
    XO_FORM, // subfe.
    XO_FORM, // subfeo
    XO_FORM, // subfeo.
    D_FORM, // subfic
    XO_FORM, // subfme
    XO_FORM, // subfme.
    XO_FORM, // subfmeo
    XO_FORM, // subfmeo.
    XO_FORM, // subfo
    XO_FORM, // subfo.
    XO_FORM, // subfze
    XO_FORM, // subfze.
    XO_FORM, // subfzeo
    XO_FORM, // subfzeo.
    X_FORM, // sync
    X_FORM, // td
    D_FORM, // tdi
    X_FORM, // tlbia
    X_FORM, // tlbie
    X_FORM, // tlbsync
    X_FORM, // tw
    D_FORM, // twi
    X_FORM, // xor
    X_FORM, // xor.
    D_FORM, // xori
    D_FORM, // xoris
    X_FORM, // la
    X_FORM, // li
    X_FORM, // lis
    X_FORM, // mr
    X_FORM  // slwi
  };

  /**
   * Index by the opcode to obtain the type of instruction.
   */
  public static final byte[] instMode = {
    REGULAR, // ??
    REGULAR, // add
    REGULAR, // add.
    REGULAR, // addc
    REGULAR, // addc.
    REGULAR, // addco
    REGULAR, // addco.
    REGULAR, // adde
    REGULAR, // adde.
    REGULAR, // addeo
    REGULAR, // addeo.
    REGULAR, // addi
    REGULAR, // addic
    REGULAR, // addic.
    REGULAR, // addis
    REGULAR, // addme
    REGULAR, // addme.
    REGULAR, // addmeo
    REGULAR, // addmeo.
    REGULAR, // addo
    REGULAR, // addo.
    REGULAR, // addze
    REGULAR, // addze.
    REGULAR, // addzeo
    REGULAR, // addzeo.
    REGULAR, // and
    REGULAR, // and.
    REGULAR, // andc
    REGULAR, // andc.
    REGULAR, // andi.
    REGULAR, // andis.
    BRANCH, // b
    BRANCH, // ba
    BRANCH, // bc
    BRANCH, // bca
    BRANCH, // bcctr
    BRANCH, // bcctrl
    BRANCH, // bcl
    BRANCH, // bcla
    BRANCH, // bclr
    BRANCH, // bclrl
    BRANCH, // bl
    BRANCH, // bla
    REGULAR, // cmpd
    REGULAR, // cmpdi
    REGULAR, // cmpld
    REGULAR, // cmpldi
    REGULAR, // cmplw
    REGULAR, // cmplwi
    REGULAR, // cmpw
    REGULAR, // cmpwi
    REGULAR, // cntlzd
    REGULAR, // cntlzd.
    REGULAR, // cntlzw
    REGULAR, // cntlzw.
    REGULAR, // crand
    REGULAR, // crandc
    REGULAR, // creqv
    REGULAR, // crnand
    REGULAR, // crnor
    REGULAR, // cror
    REGULAR, // crorc
    REGULAR, // crxor
    REGULAR, // dcba
    REGULAR, // dcbf
    REGULAR, // dcbi
    REGULAR, // dcbst
    REGULAR, // dcbt
    REGULAR, // dcbtst
    REGULAR, // dcbz
    REGULAR, // divd
    REGULAR, // divd.
    REGULAR, // divdo
    REGULAR, // divdo.
    REGULAR, // divdu
    REGULAR, // divdu.
    REGULAR, // divduo
    REGULAR, // divduo.
    REGULAR, // divw
    REGULAR, // divw.
    REGULAR, // divwo
    REGULAR, // divwo.
    REGULAR, // divwu
    REGULAR, // divwu.
    REGULAR, // divwuo
    REGULAR, // divwuo.
    REGULAR, // eciwx
    REGULAR, // ecowx
    REGULAR, // eieio
    REGULAR, // eqv
    REGULAR, // eqv.
    REGULAR, // extsb
    REGULAR, // extsb.
    REGULAR, // extsh
    REGULAR, // extsh.
    REGULAR, // extsw
    REGULAR, // extsw.
    REGULAR, // fabs
    REGULAR, // fabs.
    REGULAR, // fadd
    REGULAR, // fadd.
    REGULAR, // fadds
    REGULAR, // fadds.
    REGULAR, // fcfid
    REGULAR, // fcfid.
    REGULAR, // fcmpo
    REGULAR, // fcmpu
    REGULAR, // fctid
    REGULAR, // fctid.
    REGULAR, // fctidz
    REGULAR, // fctidz.
    REGULAR, // fctiw
    REGULAR, // fctiw.
    REGULAR, // fctiwz
    REGULAR, // fctiwz.
    REGULAR, // fdiv
    REGULAR, // fdiv.
    REGULAR, // fdivs
    REGULAR, // fdivs.
    REGULAR, // fmadd
    REGULAR, // fmadd.
    REGULAR, // fmadds
    REGULAR, // fmadds.
    REGULAR, // fmr
    REGULAR, // fmr.
    REGULAR, // fmsub
    REGULAR, // fmsub.
    REGULAR, // fmsubs
    REGULAR, // fmsubs.
    REGULAR, // fmul
    REGULAR, // fmul.
    REGULAR, // fmuls
    REGULAR, // fmuls.
    REGULAR, // fnabs
    REGULAR, // fnabs.
    REGULAR, // fneg
    REGULAR, // fneg.
    REGULAR, // fnmadd
    REGULAR, // fnmadd.
    REGULAR, // fnmadds
    REGULAR, // fnmadds.
    REGULAR, // fnmsub
    REGULAR, // fnmsub.
    REGULAR, // fnmsubs
    REGULAR, // fnmsubs.
    REGULAR, // fres
    REGULAR, // fres.
    REGULAR, // frsp
    REGULAR, // frsqrte
    REGULAR, // frsqrte.
    REGULAR, // fsel
    REGULAR, // fsel.
    REGULAR, // fsqrt
    REGULAR, // fsqrt.
    REGULAR, // fsqrts
    REGULAR, // fsqrts.
    REGULAR, // fsub
    REGULAR, // fsub.
    REGULAR, // fsubs
    REGULAR, // fsubs.
    REGULAR, // icbi
    REGULAR, // isync
    LOAD, // lbz
    LOAD, // lbzu
    LOAD, // lbzux
    LOAD, // lbzx
    LOAD, // ld
    LOAD, // ldarx
    LOAD, // ldu
    LOAD, // ldux
    LOAD, // ldx
    LOAD, // lfd
    LOAD, // lfdu
    LOAD, // lfdux
    LOAD, // lfdx
    LOAD, // lfs
    LOAD, // lfsu
    LOAD, // lfsux
    LOAD, // lfsx
    LOAD, // lha
    LOAD, // lhau
    LOAD, // lhaux
    LOAD, // lhax
    LOAD, // lhbrx
    LOAD, // lhz
    LOAD, // lhzu
    LOAD, // lhzux
    LOAD, // lhzx
    LOAD, // lmw
    LOAD, // lswi
    LOAD, // lswx
    LOAD, // lwa
    LOAD, // lwarx
    LOAD, // lwaux
    LOAD, // lwax
    LOAD, // lwbrx
    LOAD, // lwz
    LOAD, // lwzu
    LOAD, // lwzux
    REGULAR, // lwzx
    REGULAR, // mcrf
    REGULAR, // mcrfs
    REGULAR, // mcrxr
    REGULAR, // mfcr
    REGULAR, // mffs
    REGULAR, // mffs.
    REGULAR, // mfmsr
    REGULAR, // mfspr
    REGULAR, // mfsr
    REGULAR, // mfsrin
    REGULAR, // mftb
    REGULAR, // mtcrf
    REGULAR, // mtfsb0
    REGULAR, // mtfsb0.
    REGULAR, // mtfsb1
    REGULAR, // mtfsb1.
    REGULAR, // mtfsf
    REGULAR, // mtfsf.
    REGULAR, // mtfsfi
    REGULAR, // mtfsfi.
    REGULAR, // mtmsr
    REGULAR, // mtmsrd
    REGULAR, // mtspr
    REGULAR, // mtsr
    REGULAR, // mtsrd
    REGULAR, // mtsrdin
    REGULAR, // mtsrin
    REGULAR, // mulhd
    REGULAR, // mulhd.
    REGULAR, // mulhdu
    REGULAR, // mulhdu.
    REGULAR, // mulhw
    REGULAR, // mulhw.
    REGULAR, // mulhwu
    REGULAR, // mulhwu.
    REGULAR, // mulld
    REGULAR, // mulld.
    REGULAR, // mulldo
    REGULAR, // mulldo.
    REGULAR, // mulli
    REGULAR, // mullw
    REGULAR, // mullw.
    REGULAR, // mullwo
    REGULAR, // mullwo.
    REGULAR, // nand
    REGULAR, // nand.
    REGULAR, // neg
    REGULAR, // neg.
    REGULAR, // nego
    REGULAR, // nego.
    REGULAR, // nor
    REGULAR, // nor.
    REGULAR, // or
    REGULAR, // or.
    REGULAR, // orc
    REGULAR, // orc.
    REGULAR, // ori
    REGULAR, // oris
    REGULAR, // rfi
    REGULAR, // rfid
    REGULAR, // rldcl
    REGULAR, // rldcl.
    REGULAR, // rldcr
    REGULAR, // rldcr.
    REGULAR, // rldic
    REGULAR, // rldic.
    REGULAR, // rldicl
    REGULAR, // rldicl.
    REGULAR, // rldicr
    REGULAR, // rldicr.
    REGULAR, // rldimi
    REGULAR, // rldimi.
    REGULAR, // rlwimi
    REGULAR, // rlwimi.
    REGULAR, // rlwinm
    REGULAR, // rlwinm.
    REGULAR, // rlwnm
    REGULAR, // rlwnm.
    BRANCH, // sc
    REGULAR, // slbia
    REGULAR, // slbie
    REGULAR, // sld
    REGULAR, // sld.
    REGULAR, // slw
    REGULAR, // slw.
    REGULAR, // srad
    REGULAR, // srad.
    REGULAR, // sradi
    REGULAR, // sradi.
    REGULAR, // sraw
    REGULAR, // sraw.
    REGULAR, // srawi
    REGULAR, // srawi.
    REGULAR, // srd
    REGULAR, // srd.
    REGULAR, // srw
    REGULAR, // srw.
    STORE, // stb
    STORE, // stbu
    STORE, // stbux
    STORE, // stbx
    STORE, // std
    STORE, // stdcx.
    STORE, // stdu
    STORE, // stdux
    STORE, // stdx
    STORE, // stfd
    STORE, // stfdu
    STORE, // stfdux
    STORE, // stfdx
    STORE, // stfiwx
    STORE, // stfs
    STORE, // stfsu
    STORE, // stfsux
    STORE, // stfsx
    STORE, // sth
    STORE, // sthbrx
    STORE, // sthu
    STORE, // sthux
    STORE, // sthx
    STORE, // stmw
    STORE, // stswi
    STORE, // stswx
    STORE, // stw
    STORE, // stwbrx
    STORE, // stwcx.
    STORE, // stwu
    STORE, // stwux
    STORE, // stwx
    REGULAR, // subf
    REGULAR, // subf.
    REGULAR, // subfc
    REGULAR, // subfc.
    REGULAR, // subfco
    REGULAR, // subfco.
    REGULAR, // subfe
    REGULAR, // subfe.
    REGULAR, // subfeo
    REGULAR, // subfeo.
    REGULAR, // subfic
    REGULAR, // subfme
    REGULAR, // subfme.
    REGULAR, // subfmeo
    REGULAR, // subfmeo.
    REGULAR, // subfo
    REGULAR, // subfo.
    REGULAR, // subfze
    REGULAR, // subfze.
    REGULAR, // subfzeo
    REGULAR, // subfzeo.
    REGULAR, // sync
    REGULAR, // td
    REGULAR, // tdi
    REGULAR, // tlbia
    REGULAR, // tlbie
    REGULAR, // tlbsync
    REGULAR, // tw
    REGULAR, // twi
    REGULAR, // xor
    REGULAR, // xor.
    REGULAR, // xori
    REGULAR, // xoris
    REGULAR, // la
    REGULAR, // li
    REGULAR, // lis
    REGULAR, // mr
    REGULAR  // slwi
  };

  /**
   * Index by the opcode to obtain the corresponding opcode which operates
   * on registers rather than immediates.
   *
   * For example, the opcode corresponding to CMPWI is CMPW.
   */
  public static final int[] nonImmediateOpcode = {
    0,
    0,      // ADD,
    0,      // ADDP,
    0,      // ADDC,
    0,      // ADDCP,
    0,      // ADDCO,
    0,      // ADDCOP,
    0,      // ADDE,
    0,      // ADDEP,
    0,      // ADDEO,
    0,      // ADDEOP,
    0,      // ADDI,
    0,      // ADDIC,
    0,      // ADDICP,
    0,      // ADDIS,
    0,      // ADDME,
    0,      // ADDMEP,
    0,      // ADDMEO,
    0,      // ADDMEOP,
    0,      // ADDO,
    0,      // ADDOP,
    0,      // ADDZE,
    0,      // ADDZEP,
    0,      // ADDZEO,
    0,      // ADDZEOP,
    0,      // AND,
    0,      // ANDP,
    0,      // ANDC,
    0,      // ANDCP,
    0,      // ANDIP,
    0,      // ANDISP,
    0,      // B,
    0,      // BA,
    0,      // BC,
    0,      // BCA,
    0,      // BCCTR,
    0,      // BCCTRL,
    0,      // BCL,
    0,      // BCLA,
    0,      // BCLR,
    0,      // BCLRL,
    0,      // BL,
    0,      // BLA,
    0,      // CMPD,
    0,      // CMPDI,
    0,      // CMPLD,
    0,      // CMPLDI,
    0,      // CMPLW,
    CMPLW,  // CMPLWI,
    0,      // CMPW,
    CMPW,   // CMPWI,
    0,      // CNTLZD,
    0,      // CNTLZDP,
    0,      // CNTLZW,
    0,      // CNTLZWP,
    0,      // CRAND,
    0,      // CRANDC,
    0,      // CREQV,
    0,      // CRNAND,
    0,      // CRNOR,
    0,      // CROR,
    0,      // CRORC,
    0,      // CRXOR,
    0,      // DCBA,
    0,      // DCBF,
    0,      // DCBI,
    0,      // DCBST,
    0,      // DCBT,
    0,      // DCBTST,
    0,      // DCBZ,
    0,      // DIVD,
    0,      // DIVDP,
    0,      // DIVDO,
    0,      // DIVDOP,
    0,      // DIVDU,
    0,      // DIVDUP,
    0,      // DIVDUO,
    0,      // DIVDUOP,
    0,      // DIVW,
    0,      // DIVWP,
    0,      // DIVWO,
    0,      // DIVWOP,
    0,      // DIVWU,
    0,      // DIVWUP,
    0,      // DIVWUO,
    0,      // DIVWUOP,
    0,      // ECIWX,
    0,      // ECOWX,
    0,      // EIEIO,
    0,      // EQV,
    0,      // EQVP,
    0,      // EXTSB,
    0,      // EXTSBP,
    0,      // EXTSH,
    0,      // EXTSHP,
    0,      // EXTSW,
    0,      // EXTSWP,
    0,      // FABS,
    0,      // FABSP,
    0,      // FADD,
    0,      // FADDP,
    0,      // FADDS,
    0,      // FADDSP,
    0,      // FCFID,
    0,      // FCFIDP,
    0,      // FCMPO,
    0,      // FCMPU,
    0,      // FCTID,
    0,      // FCTIDP,
    0,      // FCTIDZ,
    0,      // FCTIDZP,
    0,      // FCTIW,
    0,      // FCTIWP,
    0,      // FCTIWZ,
    0,      // FCTIWZP,
    0,      // FDIV,
    0,      // FDIVP,
    0,      // FDIVS,
    0,      // FDIVSP,
    0,      // FMADD,
    0,      // FMADDP,
    0,      // FMADDS,
    0,      // FMADDSP,
    0,      // FMR,
    0,      // FMRP,
    0,      // FMSUB,
    0,      // FMSUBP,
    0,      // FMSUBS,
    0,      // FMSUBSP,
    0,      // FMUL,
    0,      // FMULP,
    0,      // FMULS,
    0,      // FMULSP,
    0,      // FNABS,
    0,      // FNABSP,
    0,      // FNEG,
    0,      // FNEGP,
    0,      // FNMADD,
    0,      // FNMADDP,
    0,      // FNMADDS,
    0,      // FNMADDSP,
    0,      // FNMSUB,
    0,      // FNMSUBP,
    0,      // FNMSUBS,
    0,      // FNMSUBSP,
    0,      // FRES,
    0,      // FRESP,
    0,      // FRSP,
    0,      // FRSQRTE,
    0,      // FRSQRTEP,
    0,      // FSEL,
    0,      // FSELP,
    0,      // FSQRT,
    0,      // FSQRTP,
    0,      // FSQRTS,
    0,      // FSQRTSP,
    0,      // FSUB,
    0,      // FSUBP,
    0,      // FSUBS,
    0,      // FSUBSP,
    0,      // ICBI,
    0,      // ISYNC,
    LBZX,   // LBZ,
    0,      // LBZU,
    0,      // LBZUX,
    0,      // LBZX,
    0,      // LD,
    0,      // LDARX,
    0,      // LDU,
    0,      // LDUX,
    0,      // LDX,
    LFDX,   // LFD,
    0,      // LFDU,
    0,      // LFDUX,
    0,      // LFDX,
    LFSX,   // LFS,
    0,      // LFSU,
    0,      // LFSUX,
    0,      // LFSX,
    0,      // LHA,
    0,      // LHAU,
    0,      // LHAUX,
    0,      // LHAX,
    0,      // LHBRX,
    LHZX,   // LHZ,
    0,      // LHZU,
    0,      // LHZUX,
    0,      // LHZX,
    0,      // LMW,
    0,      // LSWI,
    0,      // LSWX,
    0,      // LWA,
    0,      // LWARX,
    0,      // LWAUX,
    0,      // LWAX,
    0,      // LWBRX,
    LWZX,   // LWZ,
    0,      // LWZU,
    0,      // LWZUX,
    0,      // LWZX,
    0,      // MCRF,
    0,      // MCRFS,
    0,      // MCRXR,
    0,      // MFCR,
    0,      // MFFS,
    0,      // MFFSP,
    0,      // MFMSR,
    0,      // MFSPR,
    0,      // MFSR,
    0,      // MFSRIN,
    0,      // MFTB,
    0,      // MTCRF,
    0,      // MTFSB0,
    0,      // MTFSB0P,
    0,      // MTFSB1,
    0,      // MTFSB1P,
    0,      // MTFSF,
    0,      // MTFSFP,
    0,      // MTFSFI,
    0,      // MTFSFIP,
    0,      // MTMSR,
    0,      // MTMSRD,
    0,      // MTSPR,
    0,      // MTSR,
    0,      // MTSRD,
    0,      // MTSRDIN,
    0,      // MTSRIN,
    0,      // MULHD,
    0,      // MULHDP,
    0,      // MULHDU,
    0,      // MULHDUP,
    0,      // MULHW,
    0,      // MULHWP,
    0,      // MULHWU,
    0,      // MULHWUP,
    0,      // MULLD,
    0,      // MULLDP,
    0,      // MULLDO,
    0,      // MULLDOP,
    0,      // MULLI,
    0,      // MULLW,
    0,      // MULLWP,
    0,      // MULLWO,
    0,      // MULLWOP,
    0,      // NAND,
    0,      // NANDP,
    0,      // NEG,
    0,      // NEGP,
    0,      // NEGO,
    0,      // NEGOP,
    0,      // NOR,
    0,      // NORP,
    0,      // OR,
    0,      // ORP,
    0,      // ORC,
    0,      // ORCP,
    0,      // ORI,
    0,      // ORIS,
    0,      // RFI,
    0,      // RFID,
    0,      // RLDCL,
    0,      // RLDCLP,
    0,      // RLDCR,
    0,      // RLDCRP,
    0,      // RLDIC,
    0,      // RLDICP,
    0,      // RLDICL,
    0,      // RLDICLP,
    0,      // RLDICR,
    0,      // RLDICRP,
    0,      // RLDIMI,
    0,      // RLDIMIP,
    0,      // RLWIMI,
    0,      // RLWIMIP,
    0,      // RLWINM,
    0,      // RLWINMP,
    0,      // RLWNM,
    0,      // RLWNMP,
    0,      // SC,
    0,      // SLBIA,
    0,      // SLBIE,
    0,      // SLD,
    0,      // SLDP,
    0,      // SLW,
    0,      // SLWP,
    0,      // SRAD,
    0,      // SRADP,
    0,      // SRADI,
    0,      // SRADIP,
    0,      // SRAW,
    0,      // SRAWP,
    0,      // SRAWI,
    0,      // SRAWIP,
    0,      // SRD,
    0,      // SRDP,
    0,      // SRW,
    0,      // SRWP,
    STBX,   // STB,
    0,      // STBU,
    0,      // STBUX,
    0,      // STBX,
    0,      // STD,
    0,      // STDCXP,
    0,      // STDU,
    0,      // STDUX,
    0,      // STDX,
    STFDX,  // STFD,
    0,      // STFDU,
    0,      // STFDUX,
    0,      // STFDX,
    0,      // STFIWX,
    STFSX,  // STFS,
    0,      // STFSU,
    0,      // STFSUX,
    0,      // STFSX,
    STHX,   // STH,
    0,      // STHBRX,
    0,      // STHU,
    0,      // STHUX,
    0,      // STHX,
    0,      // STMW,
    0,      // STSWI,
    0,      // STSWX,
    STWX,   // STW,
    0,      // STWBRX,
    0,      // STWCXP,
    0,      // STWU,
    0,      // STWUX,
    0,      // STWX,
    0,      // SUBF,
    0,      // SUBFP,
    0,      // SUBFC,
    0,      // SUBFCP,
    0,      // SUBFCO,
    0,      // SUBFCOP,
    0,      // SUBFE,
    0,      // SUBFEP,
    0,      // SUBFEO,
    0,      // SUBFEOP,
    0,      // SUBFIC,
    0,      // SUBFME,
    0,      // SUBFMEP,
    0,      // SUBFMEO,
    0,      // SUBFMEOP,
    0,      // SUBFO,
    0,      // SUBFOP,
    0,      // SUBFZE,
    0,      // SUBFZEP,
    0,      // SUBFZEO,
    0,      // SUBFZEOP,
    0,      // SYNC,
    0,      // TD,
    0,      // TDI,
    0,      // TLBIA,
    0,      // TLBIE,
    0,      // TLBSYNC,
    0,      // TW,
    0,      // TWI,
    0,      // XOR,
    0,      // XORP,
    0,      // XORI,
    0,      // XORIS,
    0,      // LA,
    0,      // LI,
    0,      // LIS,
    0,      // MR,
    0,      // SLWI,
  };

  /**
   * Return the opcode mnemonic for the instruction.
   */
  public static String getOp(Instruction inst)
  {
    int opcode = inst.getOpcode();
    if (inst.nullified())
      return "nop !\t" + getOp(opcode);
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
    return opcodes[opcode];
  }

  /**
   * Return the opcode which operates on registers corresponding to this
   * opcode which works on immediates
   */
  public static int getNonImmediateOpcode(int opcode)
  {
    if (0 == nonImmediateOpcode[opcode]) {
      throw new scale.common.InternalError(
         "getNonImmediateOpcode: No corresponding opcode for " + getOp(opcode));
    }
    return nonImmediateOpcode[opcode];
  }
}
