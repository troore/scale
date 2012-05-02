package scale.jcr;

import java.io.*;
import java.util.BitSet;

/**
 * This class is used to both represent a Java class file code
 * attribute structure and to read that class file code attribute
 * structure.
 * <p>
 * $Id: CodeAttribute.java,v 1.14 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class CodeAttribute extends AttributeInfo
{
  /**
   * The Java byte codes.
   */
  public static final int NOP = 0;
  public static final int ACONST_NULL = 1;
  public static final int ICONST_M1 = 2;
  public static final int ICONST_0 = 3;
  public static final int ICONST_1 = 4;
  public static final int ICONST_2 = 5;
  public static final int ICONST_3 = 6;
  public static final int ICONST_4 = 7;
  public static final int ICONST_5 = 8;
  public static final int LCONST_0 = 9;
  public static final int LCONST_1 = 10;
  public static final int FCONST_0 = 11;
  public static final int FCONST_1 = 12;
  public static final int FCONST_2 = 13;
  public static final int DCONST_0 = 14;
  public static final int DCONST_1 = 15;
  public static final int BIPUSH = 16;
  public static final int SIPUSH = 17;
  public static final int LDC = 18;
  public static final int LDC_W = 19;
  public static final int LDC2_W = 20;
  public static final int ILOAD = 21;
  public static final int LLOAD = 22;
  public static final int FLOAD = 23;
  public static final int DLOAD = 24;
  public static final int ALOAD = 25;
  public static final int ILOAD_0 = 26;
  public static final int ILOAD_1 = 27;
  public static final int ILOAD_2 = 28;
  public static final int ILOAD_3 = 29;
  public static final int LLOAD_0 = 30;
  public static final int LLOAD_1 = 31;
  public static final int LLOAD_2 = 32;
  public static final int LLOAD_3 = 33;
  public static final int FLOAD_0 = 34;
  public static final int FLOAD_1 = 35;
  public static final int FLOAD_2 = 36;
  public static final int FLOAD_3 = 37;
  public static final int DLOAD_0 = 38;
  public static final int DLOAD_1 = 39;
  public static final int DLOAD_2 = 40;
  public static final int DLOAD_3 = 41;
  public static final int ALOAD_0 = 42;
  public static final int ALOAD_1 = 43;
  public static final int ALOAD_2 = 44;
  public static final int ALOAD_3 = 45;
  public static final int IALOAD = 46;
  public static final int LALOAD = 47;
  public static final int FALOAD = 48;
  public static final int DALOAD = 49;
  public static final int AALOAD = 50;
  public static final int BALOAD = 51;
  public static final int CALOAD = 52;
  public static final int SALOAD = 53;
  public static final int ISTORE = 54;
  public static final int LSTORE = 55;
  public static final int FSTORE = 56;
  public static final int DSTORE = 57;
  public static final int ASTORE = 58;
  public static final int ISTORE_0 = 59;
  public static final int ISTORE_1 = 60;
  public static final int ISTORE_2 = 61;
  public static final int ISTORE_3 = 62;
  public static final int LSTORE_0 = 63;
  public static final int LSTORE_1 = 64;
  public static final int LSTORE_2 = 65;
  public static final int LSTORE_3 = 66;
  public static final int FSTORE_0 = 67;
  public static final int FSTORE_1 = 68;
  public static final int FSTORE_2 = 69;
  public static final int FSTORE_3 = 70;
  public static final int DSTORE_0 = 71;
  public static final int DSTORE_1 = 72;
  public static final int DSTORE_2 = 73;
  public static final int DSTORE_3 = 74;
  public static final int ASTORE_0 = 75;
  public static final int ASTORE_1 = 76;
  public static final int ASTORE_2 = 77;
  public static final int ASTORE_3 = 78;
  public static final int IASTORE = 79;
  public static final int LASTORE = 80;
  public static final int FASTORE = 81;
  public static final int DASTORE = 82;
  public static final int AASTORE = 83;
  public static final int BASTORE = 84;
  public static final int CASTORE = 85;
  public static final int SASTORE = 86;
  public static final int POP = 87;
  public static final int POP2 = 88;
  public static final int DUP = 89;
  public static final int DUP_X1 = 90;
  public static final int DUP_X2 = 91;
  public static final int DUP2 = 92;
  public static final int DUP2_X1 = 93;
  public static final int DUP2_X2 = 94;
  public static final int SWAP = 95;
  public static final int IADD = 96;
  public static final int LADD = 97;
  public static final int FADD = 98;
  public static final int DADD = 99;
  public static final int ISUB = 100;
  public static final int LSUB = 101;
  public static final int FSUB = 102;
  public static final int DSUB = 103;
  public static final int IMUL = 104;
  public static final int LMUL = 105;
  public static final int FMUL = 106;
  public static final int DMUL = 107;
  public static final int IDIV = 108;
  public static final int LDIV = 109;
  public static final int FDIV = 110;
  public static final int DDIV = 111;
  public static final int IREM = 112;
  public static final int LREM = 113;
  public static final int FREM = 114;
  public static final int DREM = 115;
  public static final int INEG = 116;
  public static final int LNEG = 117;
  public static final int FNEG = 118;
  public static final int DNEG = 119;
  public static final int ISHL = 120;
  public static final int LSHL = 121;
  public static final int ISHR = 122;
  public static final int LSHR = 123;
  public static final int IUSHR = 124;
  public static final int LUSHR = 125;
  public static final int IAND = 126;
  public static final int LAND = 127;
  public static final int IOR = 128;
  public static final int LOR = 129;
  public static final int IXOR = 130;
  public static final int LXOR = 131;
  public static final int IINC = 132;
  public static final int I2L = 133;
  public static final int I2F = 134;
  public static final int I2D = 135;
  public static final int L2I = 136;
  public static final int L2F = 137;
  public static final int L2D = 138;
  public static final int F2I = 139;
  public static final int F2L = 140;
  public static final int F2D = 141;
  public static final int D2I = 142;
  public static final int D2L = 143;
  public static final int D2F = 144;
  public static final int I2B = 145;
  public static final int I2C = 146;
  public static final int I2S = 147;
  public static final int LCMP = 148;
  public static final int FCMPL = 149;
  public static final int FCMPG = 150;
  public static final int DCMPL = 151;
  public static final int DCMPG = 152;
  public static final int IFEQ = 153;
  public static final int IFNE = 154;
  public static final int IFLT = 155;
  public static final int IFGE = 156;
  public static final int IFGT = 157;
  public static final int IFLE = 158;
  public static final int IF_ICMPEQ = 159;
  public static final int IF_ICMPNE = 160;
  public static final int IF_ICMPLT = 161;
  public static final int IF_ICMPGE = 162;
  public static final int IF_ICMPGT = 163;
  public static final int IF_ICMPLE = 164;
  public static final int IF_ACMPEQ = 165;
  public static final int IF_ACMPNE = 166;
  public static final int GOTO = 167;
  public static final int JSR = 168;
  public static final int RET = 169;
  public static final int TABLESWITCH = 170;
  public static final int LOOKUPSWITCH = 171;
  public static final int IRETURN = 172;
  public static final int LRETURN = 173;
  public static final int FRETURN = 174;
  public static final int DRETURN = 175;
  public static final int ARETURN = 176;
  public static final int RETURN = 177;
  public static final int GETSTATIC = 178;
  public static final int PUTSTATIC = 179;
  public static final int GETFIELD = 180;
  public static final int PUTFIELD = 181;
  public static final int INVOKEVIRTUAL = 182;
  public static final int INVOKESPECIAL = 183;
  public static final int INVOKESTATIC = 184;
  public static final int INVOKEINTERFACE = 185;
  public static final int UNDEF186 = 186;
  public static final int NEW = 187;
  public static final int NEWARRAY = 188;
  public static final int ANEWARRAY = 189;
  public static final int ARRAYLENGTH = 190;
  public static final int ATHROW = 191;
  public static final int CHECKCAST = 192;
  public static final int INSTANCEOF = 193;
  public static final int MONITORENTER = 194;
  public static final int MONITOREXIT = 195;
  public static final int WIDE = 196;
  public static final int MULTIANEWARRAY = 197;
  public static final int IFNULL = 198;
  public static final int IFNONNULL = 199;
  public static final int GOTO_W = 200;
  public static final int JSR_W = 201;
  public static final int UNDEF202 = 202;
  public static final int UNDEF203 = 203;
  public static final int UNDEF204 = 204;
  public static final int UNDEF205 = 205;
  public static final int UNDEF206 = 206;
  public static final int UNDEF207 = 207;
  public static final int UNDEF208 = 208;
  public static final int UNDEF209 = 209;
  public static final int UNDEF210 = 210;
  public static final int UNDEF211 = 211;
  public static final int UNDEF212 = 212;
  public static final int UNDEF213 = 213;
  public static final int UNDEF214 = 214;
  public static final int UNDEF215 = 215;
  public static final int UNDEF216 = 216;
  public static final int UNDEF217 = 217;
  public static final int UNDEF218 = 218;
  public static final int UNDEF219 = 219;
  public static final int UNDEF220 = 220;
  public static final int UNDEF221 = 221;
  public static final int UNDEF222 = 222;
  public static final int UNDEF223 = 223;
  public static final int UNDEF224 = 224;
  public static final int UNDEF225 = 225;
  public static final int UNDEF226 = 226;
  public static final int UNDEF227 = 227;
  public static final int UNDEF228 = 228;
  public static final int UNDEF229 = 229;
  public static final int UNDEF230 = 230;
  public static final int UNDEF231 = 231;
  public static final int UNDEF232 = 232;
  public static final int UNDEF233 = 233;
  public static final int UNDEF234 = 234;
  public static final int UNDEF235 = 235;
  public static final int UNDEF236 = 236;
  public static final int UNDEF237 = 237;
  public static final int UNDEF238 = 238;
  public static final int UNDEF239 = 239;
  public static final int UNDEF240 = 240;
  public static final int UNDEF241 = 241;
  public static final int UNDEF242 = 242;
  public static final int UNDEF243 = 243;
  public static final int UNDEF244 = 244;
  public static final int UNDEF245 = 245;
  public static final int UNDEF246 = 246;
  public static final int UNDEF247 = 247;
  public static final int UNDEF248 = 248;
  public static final int UNDEF249 = 249;
  public static final int UNDEF250 = 250;
  public static final int UNDEF251 = 251;
  public static final int UNDEF252 = 252;
  public static final int UNDEF253 = 253;
  public static final int UNDEF254 = 254;
  public static final int UNDEF255 = 255;

  /**
   * Indexed by an opcode to obtain the instruction length.  An entry
   * of -1 indicates a variable length instruction.
   */
  public static final byte[] opcodeSize = {
    /* NOP */          1,    /* ACONST_NULL */     1,    /* ICONST_M1 */     1,    /* ICONST_0 */      1,
    /* ICONST_1 */     1,    /* ICONST_2 */        1,    /* ICONST_3 */      1,    /* ICONST_4 */      1,
    /* ICONST_5 */     1,    /* LCONST_0 */        1,    /* LCONST_1 */      1,    /* FCONST_0 */      1,
    /* FCONST_1 */     1,    /* FCONST_2 */        1,    /* DCONST_0 */      1,    /* DCONST_1 */      1,
    /* BIPUSH */       2,    /* SIPUSH */          3,    /* LDC */           3,    /* LDC_W */         3,
    /* LDC2_W */       3,    /* ILOAD */           2,    /* LLOAD */         2,    /* FLOAD */         2,
    /* DLOAD */        2,    /* ALOAD */           2,    /* ILOAD_0 */       1,    /* ILOAD_1 */       1,
    /* ILOAD_2 */      1,    /* ILOAD_3 */         1,    /* LLOAD_0 */       1,    /* LLOAD_1 */       1,
    /* LLOAD_2 */      1,    /* LLOAD_3 */         1,    /* FLOAD_0 */       1,    /* FLOAD_1 */       1,
    /* FLOAD_2 */      1,    /* FLOAD_3 */         1,    /* DLOAD_0 */       1,    /* DLOAD_1 */       1,
    /* DLOAD_2 */      1,    /* DLOAD_3 */         1,    /* ALOAD_0 */       1,    /* ALOAD_1 */       1,
    /* ALOAD_2 */      1,    /* ALOAD_3 */         1,    /* IALOAD */        1,    /* LALOAD */        1,
    /* FALOAD */       1,    /* DALOAD */          1,    /* AALOAD */        1,    /* BALOAD */        1,
    /* CALOAD */       1,    /* SALOAD */          1,    /* ISTORE */        2,    /* LSTORE */        2,
    /* FSTORE */       2,    /* DSTORE */          2,    /* ASTORE */        2,    /* ISTORE_0 */      1,
    /* ISTORE_1 */     1,    /* ISTORE_2 */        1,    /* ISTORE_3 */      1,    /* LSTORE_0 */      1,
    /* LSTORE_1 */     1,    /* LSTORE_2 */        1,    /* LSTORE_3 */      1,    /* FSTORE_0 */      1,
    /* FSTORE_1 */     1,    /* FSTORE_2 */        1,    /* FSTORE_3 */      1,    /* DSTORE_0 */      1,
    /* DSTORE_1 */     1,    /* DSTORE_2 */        1,    /* DSTORE_3 */      1,    /* ASTORE_0 */      1,
    /* ASTORE_1 */     1,    /* ASTORE_2 */        1,    /* ASTORE_3 */      1,    /* IASTORE */       1,
    /* LASTORE */      1,    /* FASTORE */         1,    /* DASTORE */       1,    /* AASTORE */       1,
    /* BASTORE */      1,    /* CASTORE */         1,    /* SASTORE */       1,    /* POP */           1,
    /* POP2 */         1,    /* DUP */             1,    /* DUP_X1 */        1,    /* DUP_X2 */        1,
    /* DUP2 */         1,    /* DUP2_X1 */         1,    /* DUP2_X2 */       1,    /* SWAP */          1,
    /* IADD */         1,    /* LADD */            1,    /* FADD */          1,    /* DADD */          1,
    /* ISUB */         1,    /* LSUB */            1,    /* FSUB */          1,    /* DSUB */          1,
    /* IMUL */         1,    /* LMUL */            1,    /* FMUL */          1,    /* DMUL */          1,
    /* IDIV */         1,    /* LDIV */            1,    /* FDIV */          1,    /* DDIV */          1,
    /* IREM */         1,    /* LREM */            1,    /* FREM */          1,    /* DREM */          1,
    /* INEG */         1,    /* LNEG */            1,    /* FNEG */          1,    /* DNEG */          1,
    /* ISHL */         1,    /* LSHL */            1,    /* ISHR */          1,    /* LSHR */          1,
    /* IUSHR */        1,    /* LUSHR */           1,    /* IAND */          1,    /* LAND */          1,
    /* IOR */          1,    /* LOR */             1,    /* IXOR */          1,    /* LXOR */          1,
    /* IINC */         3,    /* I2L */             1,    /* I2F */           1,    /* I2D */           1,
    /* L2I */          1,    /* L2F */             1,    /* L2D */           1,    /* F2I */           1,
    /* F2L */          1,    /* F2D */             1,    /* D2I */           1,    /* D2L */           1,
    /* D2F */          1,    /* I2B */             1,    /* I2C */           1,    /* I2S */           1,
    /* LCMP */         1,    /* FCMPL */           1,    /* FCMPG */         1,    /* DCMPL */         1,
    /* DCMPG */        1,    /* IFEQ */            3,    /* IFNE */          3,    /* IFLT */          3,
    /* IFGE */         3,    /* IFGT */            3,    /* IFLE */          3,    /* IF_ICMPEQ */     3,
    /* IF_ICMPNE */    3,    /* IF_ICMPLT */       3,    /* IF_ICMPGE */     3,    /* IF_ICMPGT */     3,
    /* IF_ICMPLE */    3,    /* IF_ACMPEQ */       3,    /* IF_ACMPNE */     3,    /* GOTO */          3,
    /* JSR */          3,    /* RET */             2,    /* TABLESWITCH */   1,    /* LOOKUPSWITCH */  1,
    /* IRETURN */      1,    /* LRETURN */         1,    /* FRETURN */       1,    /* DRETURN */       1,
    /* ARETURN */      1,    /* RETURN */          1,    /* GETSTATIC */     3,    /* PUTSTATIC */     3,
    /* GETFIELD */     3,    /* PUTFIELD */        3,    /* INVOKEVIRTUAL */ 3,    /* INVOKESPECIAL */ 3,
    /* INVOKESTATIC *  3,    /* INVOKEINTERFACE */ 5,    /* UNDEF186 */      1,    /* NEW */           3,
    /* NEWARRAY */     2,    /* ANEWARRAY */       3,    /* ARRAYLENGTH */   1,    /* ATHROW */        1,
    /* CHECKCAST */    3,    /* INSTANCEOF */      3,    /* MONITORENTER */  1,    /* MONITOREXIT */   1,
    /* WIDE */        -1,    /* MULTIANEWARRAY */  4,    /* IFNULL */        3,    /* IFNONNULL */     3,
    /* GOTO_W */       5,    /* JSR_W */           5,    /* UNDEF202 */      1,    /* UNDEF203 */      1,
    /* UNDEF204 */     1,    /* UNDEF205 */        1,    /* UNDEF206 */      1,    /* UNDEF207 */      1,
    /* UNDEF208 */     1,    /* UNDEF209 *         1,    /* UNDEF210 */      1,    /* UNDEF211 */      1,
    /* UNDEF212 */     1,    /* UNDEF213 */        1,    /* UNDEF214 */      1,    /* UNDEF215 */      1,
    /* UNDEF216 */     1,    /* UNDEF217 */        1,    /* UNDEF218 */      1,    /* UNDEF219 */      1,
    /* UNDEF220 */     1,    /* UNDEF221 */        1,    /* UNDEF222 */      1,    /* UNDEF223 */      1,
    /* UNDEF224 */     1,    /* UNDEF225 */        1,    /* UNDEF226 */      1,    /* UNDEF227 */      1,
    /* UNDEF228 */     1,    /* UNDEF229 */        1,    /* UNDEF230 */      1,    /* UNDEF231 */      1,
    /* UNDEF232 */     1,    /* UNDEF233 */        1,    /* UNDEF234 */      1,    /* UNDEF235 */      1,
    /* UNDEF236 */     1,    /* UNDEF237 */        1,    /* UNDEF238 */      1,    /* UNDEF239 */      1,
    /* UNDEF240 */     1,    /* UNDEF241 */        1,    /* UNDEF242 */      1,    /* UNDEF243 */      1,
    /* UNDEF244 */     1,    /* UNDEF245 */        1,    /* UNDEF246 */      1,    /* UNDEF247 */      1,
    /* UNDEF248 */     1,    /* UNDEF249 */        1,    /* UNDEF250 */      1,    /* UNDEF251 */      1,
    /* UNDEF252 */     1,    /* UNDEF253 */        1,    /* UNDEF254 */      1,    /* UNDEF255 */      1,
  };

  /**
   * Indexed by an opcode to obtain the name of the opcode for debugging.
   */
  public static final String[] opcodeName = {
    "NOP",          "ACONST_NULL",     "ICONST_M1",     "ICONST_0",
    "ICONST_1",     "ICONST_2",        "ICONST_3",      "ICONST_4",
    "ICONST_5",     "LCONST_0",        "LCONST_1",      "FCONST_0",
    "FCONST_1",     "FCONST_2",        "DCONST_0",      "DCONST_1",
    "BIPUSH",       "SIPUSH",          "LDC",           "LDC_W",
    "LDC2_W",       "ILOAD",           "LLOAD",         "FLOAD",
    "DLOAD",        "ALOAD",           "ILOAD_0",       "ILOAD_1",
    "ILOAD_2",      "ILOAD_3",         "LLOAD_0",       "LLOAD_1",
    "LLOAD_2",      "LLOAD_3",         "FLOAD_0",       "FLOAD_1",
    "FLOAD_2",      "FLOAD_3",         "DLOAD_0",       "DLOAD_1",
    "DLOAD_2",      "DLOAD_3",         "ALOAD_0",       "ALOAD_1",
    "ALOAD_2",      "ALOAD_3",         "IALOAD",        "LALOAD",
    "FALOAD",       "DALOAD",          "AALOAD",        "BALOAD",
    "CALOAD",       "SALOAD",          "ISTORE",        "LSTORE",
    "FSTORE",       "DSTORE",          "ASTORE",        "ISTORE_0",
    "ISTORE_1",     "ISTORE_2",        "ISTORE_3",      "LSTORE_0",
    "LSTORE_1",     "LSTORE_2",        "LSTORE_3",      "FSTORE_0",
    "FSTORE_1",     "FSTORE_2",        "FSTORE_3",      "DSTORE_0",
    "DSTORE_1",     "DSTORE_2",        "DSTORE_3",      "ASTORE_0",
    "ASTORE_1",     "ASTORE_2",        "ASTORE_3",      "IASTORE",
    "LASTORE",      "FASTORE",         "DASTORE",       "AASTORE",
    "BASTORE",      "CASTORE",         "SASTORE",       "POP",
    "POP2",         "DUP",             "DUP_X1",        "DUP_X2",
    "DUP2",         "DUP2_X1",         "DUP2_X2",       "SWAP",
    "IADD",         "LADD",            "FADD",          "DADD",
    "ISUB",         "LSUB",            "FSUB",          "DSUB",
    "IMUL",         "LMUL",            "FMUL",          "DMUL",
    "IDIV",         "LDIV",            "FDIV",          "DDIV",
    "IREM",         "LREM",            "FREM",          "DREM",
    "INEG",         "LNEG",            "FNEG",          "DNEG",
    "ISHL",         "LSHL",            "ISHR",          "LSHR",
    "IUSHR",        "LUSHR",           "IAND",          "LAND",
    "IOR",          "LOR",             "IXOR",          "LXOR",
    "IINC",         "I2L",             "I2F",           "I2D",
    "L2I",          "L2F",             "L2D",           "F2I",
    "F2L",          "F2D",             "D2I",           "D2L",
    "D2F",          "I2B",             "I2C",           "I2S",
    "LCMP",         "FCMPL",           "FCMPG",         "DCMPL",
    "DCMPG",        "IFEQ",            "IFNE",          "IFLT",
    "IFGE",         "IFGT",            "IFLE",          "IF_ICMPEQ",
    "IF_ICMPNE",    "IF_ICMPLT",       "IF_ICMPGE",     "IF_ICMPGT",
    "IF_ICMPLE",    "IF_ACMPEQ",       "IF_ACMPNE",     "GOTO",
    "JSR",          "RET",             "TABLESWITCH",   "LOOKUPSWITCH",
    "IRETURN",      "LRETURN",         "FRETURN",       "DRETURN",
    "ARETURN",      "RETURN",          "GETSTATIC",     "PUTSTATIC",
    "GETFIELD",     "PUTFIELD",        "INVOKEVIRTUAL", "INVOKESPECIAL",
    "INVOKESTATIC", "INVOKEINTERFACE", "UNDEF186",      "NEW",
    "NEWARRAY",     "ANEWARRAY",       "ARRAYLENGTH",   "ATHROW",
    "CHECKCAST",    "INSTANCEOF",      "MONITORENTER",  "MONITOREXIT",
    "WIDE",         "MULTIANEWARRAY",  "IFNULL",        "IFNONNULL",
    "GOTO_W",       "JSR_W",           "UNDEF202",      "UNDEF203",
    "UNDEF204",     "UNDEF205",        "UNDEF206",      "UNDEF207",
    "UNDEF208",     "UNDEF209",        "UNDEF210",      "UNDEF211",
    "UNDEF212",     "UNDEF213",        "UNDEF214",      "UNDEF215",
    "UNDEF216",     "UNDEF217",        "UNDEF218",      "UNDEF219",
    "UNDEF220",     "UNDEF221",        "UNDEF222",      "UNDEF223",
    "UNDEF224",     "UNDEF225",        "UNDEF226",      "UNDEF227",
    "UNDEF228",     "UNDEF229",        "UNDEF230",      "UNDEF231",
    "UNDEF232",     "UNDEF233",        "UNDEF234",      "UNDEF235",
    "UNDEF236",     "UNDEF237",        "UNDEF238",      "UNDEF239",
    "UNDEF240",     "UNDEF241",        "UNDEF242",      "UNDEF243",
    "UNDEF244",     "UNDEF245",        "UNDEF246",      "UNDEF247",
    "UNDEF248",     "UNDEF249",        "UNDEF250",      "UNDEF251",
    "UNDEF252",     "UNDEF253",        "UNDEF254",      "UNDEF255"
  };

  /**
   * Types from the NEWARRAY byte code.
   */
  public static final byte T_ADDRESS = 0;
  public static final byte T_BOOLEAN = 4;
  public static final byte T_CHAR    = 5;
  public static final byte T_FLOAT   = 6;
  public static final byte T_DOUBLE  = 7;
  public static final byte T_BYTE    = 8;
  public static final byte T_SHORT   = 9;
  public static final byte T_INT     = 10;
  public static final byte T_LONG    = 11;

  /**
   * Convert from Java type number to Java type specifier.
   */
  public static final char[] typeSpecifier = {
    'A', 'A', 'A', 'A',
    'I', 'I', 'F', 'D',
    'I', 'I', 'I', 'L'};
  /**
   * Is the type an integer type?
   */
  public static final boolean[] isInteger = {
    false, false, false, false,
    true,  true,  false, false,
    true,  true,  true,  true};
  /**
   * Does the type take two words?
   */
  public static final boolean[] isTwo = {
    false, false, false, false,
    false, false, false, true,
    false, false, false, true};

  private int              maxStack;
  private int              maxLocals;
  private byte[]           code;
  private ExceptionEntry[] exceptionTable;
  private AttributeInfo[]  attributes;
  private BitSet           basicBlocks = null;

  public CodeAttribute(int              nameIndex,
                       int              maxStack,
                       int              maxLocals,
                       byte[]           code,
                       ExceptionEntry[] exceptionTable,
                       AttributeInfo[]  attributes)
  {
    super(nameIndex);

    this.maxStack       = maxStack;
    this.maxLocals      = maxLocals;
    this.code           = code;
    this.exceptionTable = exceptionTable;
    this.attributes     = attributes;
  }

  public int getMaxStack()
  {
    return maxStack;
  }

  public int getMaxLocals()
  {
    return maxLocals;
  }

  public int getCodelength()
  {
    return code.length;
  }

  public int getExceptionTableLength()
  {
    return exceptionTable.length;
  }

  public ExceptionEntry[] getExceptionTable()
  {
    return exceptionTable;
  }

  public int getAttributesCount()
  {
    return attributes.length;
  }

  public AttributeInfo[] getAttributes()
  {
    return attributes;
  }

  public int getOpcode(int index)
  {
    return code[index] & 0xff;
  }

  /**
   * @param index is the index of the opcode in the code
   * @return the length of the instruction at the current PC location
   */
  public int getOpcodeLength(int index)
  {
    int opcode = code[index];
    int l = opcodeSize[opcode];
    if (l > 0)
      return l;
    if (opcode == WIDE) {
      int op2 = code[index + 1];
      if (op2 == IINC)
        return 6;
      return 4;
    } else if (opcode == LOOKUPSWITCH) {
      int start = index + (4 - (index & 0x3)) + 4;
      int n     = (((code[start + 0] & 0xff) << 24) | ((code[start + 1] & 0xff) << 16) |
                   ((code[start + 2] & 0xff) << 8)  | ((code[start + 3] & 0xff) << 0));
      return start + 4 + (8 * n) - index;
    } else if (opcode == TABLESWITCH) {
      int start = index + (4 - (index & 0x3));
      int low   = (((code[start + 0] & 0xff) << 24) | ((code[start + 1] & 0xff) << 16) |
                   ((code[start + 2] & 0xff) << 8)  | ((code[start + 3] & 0xff) << 0));
      int high  = (((code[start + 4] & 0xff) << 24) | ((code[start + 5] & 0xff) << 16) |
                   ((code[start + 6] & 0xff) << 8)  | ((code[start + 7] & 0xff) << 0));
      return start + 8 + (4 * (high - low + 1)) - index;
    }
    throw new scale.common.InternalError("Improper modification of table entries at " + opcode);
  }

  private void locateBasicBlocks()
  {
    basicBlocks = new BitSet(code.length + 1);
    basicBlocks.set(0);

    int pc = 0;
    while (pc < code.length) {
      int opcode = code[pc] & 0xff;
      switch (opcode) {
      default:            /* 1 */
        pc++;
        break;
      case IRETURN:
      case LRETURN:
      case FRETURN:
      case DRETURN:
      case ARETURN:
      case RETURN:
      case ATHROW:
        pc++;
        basicBlocks.set(pc);
        break;
      case ALOAD:         /* 2 */
      case ASTORE:
      case BIPUSH:
      case DLOAD:
      case DSTORE:
      case FLOAD:
      case FSTORE:
      case ILOAD:
      case ISTORE:
      case LLOAD:
      case LSTORE:
      case NEWARRAY:
        pc += 2;
        break;
      case RET:
        pc += 2;
        basicBlocks.set(pc);
        break;
      case ANEWARRAY:     /* 3 */
      case CHECKCAST:
      case GETFIELD:
      case GETSTATIC:
      case IINC:
      case INSTANCEOF:
      case INVOKESPECIAL:
      case INVOKESTATIC:
      case INVOKEVIRTUAL:
      case LDC:
      case LDC2_W:
      case LDC_W:
      case NEW:
      case PUTFIELD:
      case PUTSTATIC:
      case SIPUSH:
        pc += 3;
        break;
      case GOTO:
      case IFEQ:
      case IFGE:
      case IFGT:
      case IFLE:
      case IFLT:
      case IFNE:
      case IFNONNULL:
      case IFNULL:
      case IF_ACMPEQ:
      case IF_ACMPNE:
      case IF_ICMPEQ:
      case IF_ICMPGE:
      case IF_ICMPGT:
      case IF_ICMPLE:
      case IF_ICMPLT:
      case IF_ICMPNE:
        basicBlocks.set(pc + getIndex2(pc + 1));
        pc += 3;
        basicBlocks.set(pc);
        break;
      case JSR:
        basicBlocks.set(pc + getIndex2(pc + 1));
        pc += 3;
        break;
      case MULTIANEWARRAY:  /* 4 */
        pc += 4;
        break;
      case GOTO_W:          /* 5 */
        basicBlocks.set(pc + getIndex4(pc + 1));
        pc += 5;
        basicBlocks.set(pc);
        break;
      case INVOKEINTERFACE:
        pc += 5;
        break;
      case JSR_W:
        basicBlocks.set(pc + getIndex4(pc + 1));
        pc += 5;
        break;
      case LOOKUPSWITCH:    /* Variable */
        {
          int opc = pc;
          pc += (4 - (pc & 0x3));
          basicBlocks.set(opc + getIndex4(pc));
          pc += 4;
          int n = getIndex4(pc);
          for (int i = 0; i < n; i++) {
            pc += 4; /* Skip match value */
            basicBlocks.set(opc + getIndex4(pc));
            pc += 4;
          }
        }
        basicBlocks.set(pc);
        break;
      case TABLESWITCH:     /* Variable */
        {
          int opc = pc;
          pc += (4 - (pc & 0x3));
          basicBlocks.set(opc + getIndex4(pc));
          int low  = getIndex4(pc);
          pc += 4;
          int high = getIndex4(pc);
          pc += 4;
          for (int i = low; i < high; i++) {
            basicBlocks.set(opc + getIndex4(pc));
            pc += 4;
          }
        }
        basicBlocks.set(pc);
        break;
      case WIDE:            /* Variable */
        int op2 = code[pc + 1] & 0xff;
        if (op2 == IINC)
          pc += 6;
        pc += 4;
        break;
      }
    }
  }

  /**
   * @param index into the code
   * @return true if this byte starts a basic block in the code
   */
  public boolean isBasicBlock(int index)
  {
    if (basicBlocks == null)
      locateBasicBlocks();
    return basicBlocks.get(index);
  }

  /**
   * @return the 8-bit unsigned value at the specified location
   */
  public int getIndex(int index)
  {
    return code[index] & 0xff;
  }

  /**
   * @return the 16-bit unsigned value at the specified location
   */
  public int getIndex2(int index)
  {
    int x = ((code[index + 0] & 0xff) << 8) | (code[index + 1] & 0xff);
    return x;
  }

  /**
   * @return the 32-bit signed value at the specified location
   */
  public int getIndex4(int index)
  {
    int x = (((code[index + 0] & 0xff) << 24) |
             ((code[index + 1] & 0xff) << 16) |
             ((code[index + 2] & 0xff) << 8)  |
             ((code[index + 3] & 0xff) << 0));
    return x;
  }

  /**
   * @return the 8-bit signed value at the specified location
   */
  public int getByte(int index)
  {
    return code[index];
  }

  /**
   * @return the 16-bit signed value at the specified location
   */
  public int getByte2(int index)
  {
    int x = ((code[index + 0] << 8) & 0xffffff00) | (code[index + 1] & 0xff);
    return x;
  }

  /**
   * Read in the CodeAttribute structure.
   * @exception java.io.IOException if there is an IO problem
   */
  public static CodeAttribute read(ClassFile       cf,
                                   DataInputStream reader,
                                   int             nameIndex) throws java.io.IOException
  {
    int attributeLength = reader.readInt();
    int maxStack        = reader.readUnsignedShort();
    int maxLocals       = reader.readUnsignedShort();
    int codeLength      = reader.readInt();

    byte[] code = new byte[codeLength];
    reader.read(code);

    int exceptionTableLength = reader.readUnsignedShort();
    ExceptionEntry[] exceptionTable = new ExceptionEntry[exceptionTableLength];
    for (int i = 0; i < exceptionTableLength; i++)
      exceptionTable[i] = ExceptionEntry.read(cf, reader);

    int attributesCount = reader.readUnsignedShort();
    AttributeInfo[] attributes = new AttributeInfo[attributesCount];
    for (int i = 0; i < attributesCount; i++)
      attributes[i] = AttributeInfo.read(cf, reader);

    return new CodeAttribute(nameIndex, maxStack, maxLocals, code, exceptionTable, attributes);
  }
}
