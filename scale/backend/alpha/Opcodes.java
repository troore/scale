package scale.backend.alpha;

import scale.common.*;
import scale.backend.*;

/** 
 * This class provides Alpha instruction information.
 * <p>
 * $Id: Opcodes.java,v 1.10 2007-10-04 19:57:51 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Alpha opcodes are represented by an integer that is split into two
 * fields.
 * <ol>
 * <li> Bits <10:0> are the secondary function code.
 * <li> Bits <17:12> are the instruction opcode.
 * </ol>
 * Many instructions do not have a secondary function code.  The Jmp
 * instruction uses the secondary function code as a branch hint.
 * Some memory instructions use the secondary function code to specify
 * the instruction.
 */

public class Opcodes
{
  public static final int PAL00 = 0x00000;
  public static final int OPC01 = 0x01000;
  public static final int OPC02 = 0x02000;
  public static final int OPC03 = 0x03000;
  public static final int OPC04 = 0x04000;
  public static final int OPC05 = 0x05000;
  public static final int OPC06 = 0x06000;
  public static final int OPC07 = 0x07000;
  public static final int LDA   = 0x08000;
  public static final int LDAH  = 0x09000;
  public static final int LDBU  = 0x0A000;
  public static final int LDQ_U = 0x0B000;
  public static final int LDWU  = 0x0C000;
  public static final int STW   = 0x0D000;
  public static final int STB   = 0x0E000;
  public static final int STQ_U = 0x0F000;
  public static final int OP1   = 0x10000;
  public static final int OP2   = 0x11000;
  public static final int OP3   = 0x12000;
  public static final int OP4   = 0x13000;
  public static final int OP5   = 0x14000;
  public static final int VAX   = 0x15000;
  public static final int FOP1  = 0x16000;
  public static final int FOP2  = 0x17000;
  public static final int PAGE  = 0x18000;
  public static final int PAL19 = 0x19000;
  public static final int JMPOP = 0x1A000;
  public static final int PAL1B = 0x1B000;
  public static final int MM    = 0x1C000;
  public static final int PAL1D = 0x1D000;
  public static final int PAL1E = 0x1E000;
  public static final int PAL1F = 0x1F000;
  public static final int LDF   = 0x20000;
  public static final int LDG   = 0x21000;
  public static final int LDS   = 0x22000;
  public static final int LDT   = 0x23000;
  public static final int STF   = 0x24000;
  public static final int STG   = 0x25000;
  public static final int STS   = 0x26000;
  public static final int STT   = 0x27000;
  public static final int LDL   = 0x28000;
  public static final int LDQ   = 0x29000;
  public static final int LDL_L = 0x2A000;
  public static final int LDQ_L = 0x2B000;
  public static final int STL   = 0x2C000;
  public static final int STQ   = 0x2D000;
  public static final int STL_C = 0x2E000;
  public static final int STQ_C = 0x2F000;
  public static final int BR    = 0x30000;
  public static final int FBEQ  = 0x31000;
  public static final int FBLT  = 0x32000;
  public static final int FBLE  = 0x33000;
  public static final int BSR   = 0x34000;
  public static final int FBNE  = 0x35000;
  public static final int FBGE  = 0x36000;
  public static final int FBGT  = 0x37000;
  public static final int BLBC  = 0x38000;
  public static final int BEQ   = 0x39000;
  public static final int BLT   = 0x3A000;
  public static final int BLE   = 0x3B000;
  public static final int BLBS  = 0x3C000;
  public static final int BNE   = 0x3D000;
  public static final int BGE   = 0x3E000;
  public static final int BGT   = 0x3F000;

  public static final int HALT  = 0x00000;
  public static final int IMB   = 0x00086;

  public static final int ADDL    = 0x10000;
  public static final int ADDQ    = 0x10020;
  public static final int ADDLV   = 0x10040;
  public static final int ADDQV   = 0x10060;
  public static final int CMPULE  = 0x1003D;
  public static final int CMPBGE  = 0x1000F;
  public static final int S4ADDL  = 0x10002;
  public static final int S4ADDQ  = 0x10022;
  public static final int CMPEQ   = 0x1002D;
  public static final int CMPLT   = 0x1004D;
  public static final int CMPLE   = 0x1006D;
  public static final int CMPULT  = 0x1001D;
  public static final int S8ADDL  = 0x10012;
  public static final int S8ADDQ  = 0x10032;
  public static final int S8SUBL  = 0x1001B;
  public static final int S8SUBQ  = 0x1003B;
  public static final int SUBL    = 0x10009;
  public static final int SUBLV   = 0x10049;
  public static final int SUBQ    = 0x10029;
  public static final int SUBQV   = 0x10069;
  public static final int S4SUBL  = 0x1000B;
  public static final int S4SUBQ  = 0x1002B;

  public static final int AND     = 0x11000;
  public static final int BIC     = 0x11008;
  public static final int CMOVEQ  = 0x11024;
  public static final int CMOVNE  = 0x11026;
  public static final int CMOVLBS = 0x11014;
  public static final int BIS     = 0x11020;
  public static final int ORNOT   = 0x11028;
  public static final int CMOVLT  = 0x11044;
  public static final int CMOVGE  = 0x11046;
  public static final int CMOVLBC = 0x11016;
  public static final int XOR     = 0x11040;
  public static final int EQV     = 0x11048;
  public static final int AMASK   = 0x11061;
  public static final int CMOVLE  = 0x11064;
  public static final int CMOVGT  = 0x11066;
  public static final int IMPLVER = 0x1106C;

  public static final int SLL     = 0x12039;
  public static final int SRA     = 0x1203C;
  public static final int SRL     = 0x12034;
  public static final int EXTBL   = 0x12006;
  public static final int EXTWL   = 0x12016;
  public static final int EXTLL   = 0x12026;
  public static final int EXTQL   = 0x12036;
  public static final int EXTWH   = 0x1205A;
  public static final int EXTLH   = 0x1206A;
  public static final int EXTQH   = 0x1207A;
  public static final int INSBL   = 0x1200B;
  public static final int INSWL   = 0x1201B;
  public static final int INSLL   = 0x1202B;
  public static final int INSQL   = 0x1203B;
  public static final int INSWH   = 0x12057;
  public static final int INSLH   = 0x12067;
  public static final int INSQH   = 0x12077;
  public static final int MSKBL   = 0x12002;
  public static final int MSKWL   = 0x12012;
  public static final int MSKLL   = 0x12022;
  public static final int MSKQL   = 0x12032;
  public static final int MSKWH   = 0x12052;
  public static final int MSKLH   = 0x12062;
  public static final int MSKQH   = 0x12072;
  public static final int ZAP     = 0x12030;
  public static final int ZAPNOT  = 0x12031;

  public static final int MULL    = 0x13000;
  public static final int MULQV   = 0x13060;
  public static final int MULLV   = 0x13040;
  public static final int UMULH   = 0x13030;
  public static final int MULQ    = 0x13020;

  public static final int SQRTF = 0x1408A;
  public static final int SQRTG = 0x140AA;
  public static final int SQRTS = 0x1408B;
  public static final int SQRTT = 0x140AB;
  public static final int ITOFF = 0x14014;
  public static final int ITOFS = 0x14004;
  public static final int ITOFT = 0x14024;

  public static final int ADDF    = 0X15080;
  public static final int ADDFC   = 0X15000;
  public static final int ADDFU   = 0X15180;
  public static final int ADDFUC  = 0X15100;
  public static final int ADDFS   = 0X15480;
  public static final int ADDFSC  = 0X15400;
  public static final int ADDFSU  = 0X15580;
  public static final int ADDFSUC = 0X15500;

  public static final int CVTDG    = 0X1509E;
  public static final int CVTDGC   = 0X1501E;
  public static final int CVTDGU   = 0X1519E;
  public static final int CVTDGUC  = 0X1511E;
  public static final int CVTDGS   = 0X1549E;
  public static final int CVTDGSC  = 0X1541E;
  public static final int CVTDGSU  = 0X1559E;
  public static final int CVTDGSUC = 0X1551E;

  public static final int ADDG    = 0X150A0;
  public static final int ADDGC   = 0X15020;
  public static final int ADDGU   = 0X151A0;
  public static final int ADDGUC  = 0X15120;
  public static final int ADDGS   = 0X154A0;
  public static final int ADDGSC  = 0X15420;
  public static final int ADDGSU  = 0X155A0;
  public static final int ADDGSUC = 0X15520;

  public static final int CMPGEQ  = 0X150A5;
  public static final int CMPGEQS = 0X154A5;
  public static final int CMPGLT  = 0X150A6;
  public static final int CMPGLTS = 0X154A6;
  public static final int CMPGLE  = 0X150A7;
  public static final int CMPGLES = 0X154A7;

  public static final int CVTGF    = 0X150AC;
  public static final int CVTGFC   = 0X1502C;
  public static final int CVTGFU   = 0X151AC;
  public static final int CVTGFUC  = 0X1512C;
  public static final int CVTGFS   = 0X154AC;
  public static final int CVTGFSC  = 0X1542C;
  public static final int CVTGFSU  = 0X155AC;
  public static final int CVTGFSUC = 0X1552C;

  public static final int CVTGD    = 0X150AD;
  public static final int CVTGDC   = 0X1502D;
  public static final int CVTGDU   = 0X151AD;
  public static final int CVTGDUC  = 0X1512D;
  public static final int CVTGDS   = 0X154AD;
  public static final int CVTGDSC  = 0X1542D;
  public static final int CVTGDSU  = 0X155AD;
  public static final int CVTGDSUC = 0X1552D;

  public static final int CVTQF    = 0X150BC;
  public static final int CVTQFC   = 0X1503C;

  public static final int CVTQG    = 0X150BE;
  public static final int CVTQGC   = 0X1503E;

  public static final int DIVF    = 0X15083;
  public static final int DIVFC   = 0X15003;
  public static final int DIVFU   = 0X15183;
  public static final int DIVFUC  = 0X15103;
  public static final int DIVFS   = 0X15483;
  public static final int DIVFSC  = 0X15403;
  public static final int DIVFSU  = 0X15583;
  public static final int DIVFSUC = 0X15503;

  public static final int DIVG    = 0X150A3;
  public static final int DIVGC   = 0X15023;
  public static final int DIVGU   = 0X151A3;
  public static final int DIVGUC  = 0X15123;
  public static final int DIVGS   = 0X154A3;
  public static final int DIVGSC  = 0X15423;
  public static final int DIVGSU  = 0X155A3;
  public static final int DIVGSUC = 0X15523;

  public static final int MULF    = 0X15082;
  public static final int MULFC   = 0X15002;
  public static final int MULFU   = 0X15182;
  public static final int MULFUC  = 0X15102;
  public static final int MULFS   = 0X15482;
  public static final int MULFSC  = 0X15402;
  public static final int MULFSU  = 0X15582;
  public static final int MULFSUC = 0X15502;

  public static final int MULG    = 0X150A2;
  public static final int MULGC   = 0X15022;
  public static final int MULGU   = 0X151A2;
  public static final int MULGUC  = 0X15122;
  public static final int MULGS   = 0X154A2;
  public static final int MULGSC  = 0X15422;
  public static final int MULGSU  = 0X155A2;
  public static final int MULGSUC = 0X15522;

  public static final int SUBF    = 0X15081;
  public static final int SUBFC   = 0X15001;
  public static final int SUBFU   = 0X15181;
  public static final int SUBFUC  = 0X15101;
  public static final int SUBFS   = 0X15481;
  public static final int SUBFSC  = 0X15401;
  public static final int SUBFSU  = 0X15581;
  public static final int SUBFSUC = 0X15501;

  public static final int SUBG    = 0X150A1;
  public static final int SUBGC   = 0X15021;
  public static final int SUBGU   = 0X151A1;
  public static final int SUBGUC  = 0X15121;
  public static final int SUBGS   = 0X154A1;
  public static final int SUBGSC  = 0X15421;
  public static final int SUBGSU  = 0X155A1;
  public static final int SUBGSUC = 0X15521;

  public static final int ADDS   = 0X16080;
  public static final int ADDSC  = 0X16000;
  public static final int ADDSM  = 0X16040;
  public static final int ADDSD  = 0X160C0;
  public static final int ADDSU  = 0X16180;
  public static final int ADDSUC = 0X16100;
  public static final int ADDSUM = 0X16140;
  public static final int ADDSUD = 0X161C0;

  public static final int ADDSSU    = 0X16580;
  public static final int ADDSSUC   = 0X16500;
  public static final int ADDSSUM   = 0X16540;
  public static final int ADDSSUD   = 0X165C0;
  public static final int ADDSSUI   = 0X16780;
  public static final int ADDSUSUIC = 0X16700;
  public static final int ADDSUSUIM = 0X16740;
  public static final int ADDSUSUID = 0X167C0;

  public static final int ADDT   = 0X160A0;
  public static final int ADDTC  = 0X16020;
  public static final int ADDTM  = 0X16060;
  public static final int ADDTD  = 0X160E0;
  public static final int ADDTU  = 0X161A0;
  public static final int ADDTUC = 0X16120;
  public static final int ADDTUM = 0X16160;
  public static final int ADDTUD = 0X161E0;

  public static final int ADDTSU    = 0X165A0;
  public static final int ADDTSUC   = 0X16520;
  public static final int ADDTSUM   = 0X16560;
  public static final int ADDTSUD   = 0X165E0;
  public static final int ADDTSUI   = 0X167A0;
  public static final int ADDTUSUIC = 0X16720;
  public static final int ADDTUSUIM = 0X16760;
  public static final int ADDTUSUID = 0X167E0;

  public static final int CMPTUN = 0X160A4;
  public static final int CMPTEQ = 0X160A5;
  public static final int CMPTLT = 0X160A6;
  public static final int CMPTLE = 0X160A7;

  public static final int CMPTUNSU = 0X165A4;
  public static final int CMPTEQSU = 0X165A5;
  public static final int CMPTLTSU = 0X165A6;
  public static final int CMPTLESU = 0X165A7;

  public static final int CVTQS   = 0X160BC;
  public static final int CVTQSC  = 0X1603C;
  public static final int CVTQSM  = 0X1607C;
  public static final int CVTQSD  = 0X160FC;

  public static final int CVTQSSUI   = 0X167BC;
  public static final int CVTQSSUIC  = 0X1673C;
  public static final int CVTQSSUIM  = 0X1677C;
  public static final int CVTQSSUID  = 0X167FC;

  public static final int CVTQT   = 0X167BE;
  public static final int CVTQTC  = 0X1673E;
  public static final int CVTQTM  = 0X1677E;
  public static final int CVTQTD  = 0X167FE;

  public static final int CVTQTSUI   = 0X160BE;
  public static final int CVTQTSUIC  = 0X1603E;
  public static final int CVTQTSUIM  = 0X1607E;
  public static final int CVTQTSUID  = 0X160FE;

  public static final int CVTTS   = 0X160AC;
  public static final int CVTTSC  = 0X1602C;
  public static final int CVTTSM  = 0X1606C;
  public static final int CVTTSD  = 0X160EC;
  public static final int CVTTSU  = 0X161AC;
  public static final int CVTTSUC = 0X1612C;
  public static final int CVTTSUM = 0X1616C;
  public static final int CVTTSUD = 0X161EC;

  public static final int CVTTSSU   = 0X165AC;
  public static final int CVTTSSUC  = 0X1652C;
  public static final int CVTTSSUM  = 0X1656C;
  public static final int CVTTSSUD  = 0X165EC;
  public static final int CVTTSSUI  = 0X167AC;
  public static final int CVTTSSUIC = 0X1672C;
  public static final int CVTTSSUIM = 0X1676C;
  public static final int CVTTSSUID = 0X167EC;

  public static final int DIVS   = 0X16083;
  public static final int DIVSC  = 0X16003;
  public static final int DIVSM  = 0X16043;
  public static final int DIVSD  = 0X160C3;
  public static final int DIVSU  = 0X16183;
  public static final int DIVSUC = 0X16103;
  public static final int DIVSUM = 0X16143;
  public static final int DIVSUD = 0X161C3;

  public static final int DIVSSU   = 0X16583;
  public static final int DIVSSUC  = 0X16503;
  public static final int DIVSSUM  = 0X16543;
  public static final int DIVSSUD  = 0X165C3;
  public static final int DIVSSUI  = 0X16783;
  public static final int DIVSSUIC = 0X16703;
  public static final int DIVSSUIM = 0X16743;
  public static final int DIVSSUID = 0X167C3;

  public static final int DIVT   = 0X160A3;
  public static final int DIVTC  = 0X16023;
  public static final int DIVTM  = 0X16063;
  public static final int DIVTD  = 0X160E3;
  public static final int DIVTU  = 0X161A3;
  public static final int DIVTUC = 0X16123;
  public static final int DIVTUM = 0X16163;
  public static final int DIVTUD = 0X161E3;

  public static final int DIVTSU   = 0X165A3;
  public static final int DIVTSUC  = 0X16523;
  public static final int DIVTSUM  = 0X16563;
  public static final int DIVTSUD  = 0X165E3;
  public static final int DIVTSUI  = 0X167A3;
  public static final int DIVTSUIC = 0X16723;
  public static final int DIVTSUIM = 0X16763;
  public static final int DIVTSUID = 0X167E3;

  public static final int MULS   = 0X16082;
  public static final int MULSC  = 0X16002;
  public static final int MULSM  = 0X16042;
  public static final int MULSD  = 0X160C2;
  public static final int MULSU  = 0X16182;
  public static final int MULSUC = 0X16102;
  public static final int MULSUM = 0X16142;
  public static final int MULSUD = 0X161C2;

  public static final int MULSSU   = 0X16582;
  public static final int MULSSUC  = 0X16502;
  public static final int MULSSUM  = 0X16542;
  public static final int MULSSUD  = 0X165C2;
  public static final int MULSSUI  = 0X16782;
  public static final int MULSSUIC = 0X16702;
  public static final int MULSSUIM = 0X16742;
  public static final int MULSSUID = 0X167C2;

  public static final int MULT   = 0X160A2;
  public static final int MULTC  = 0X16022;
  public static final int MULTM  = 0X16062;
  public static final int MULTD  = 0X160E2;
  public static final int MULTU  = 0X161A2;
  public static final int MULTUC = 0X16122;
  public static final int MULTUM = 0X16162;
  public static final int MULTUD = 0X161E2;

  public static final int MULTSU   = 0X165A2;
  public static final int MULTSUC  = 0X16522;
  public static final int MULTSUM  = 0X16562;
  public static final int MULTSUD  = 0X165E2;
  public static final int MULTSUI  = 0X167A2;
  public static final int MULTSUIC = 0X16722;
  public static final int MULTSUIM = 0X16762;
  public static final int MULTSUID = 0X167E2;

  public static final int SUBS   = 0X16081;
  public static final int SUBSC  = 0X16001;
  public static final int SUBSM  = 0X16041;
  public static final int SUBSD  = 0X160C1;
  public static final int SUBSU  = 0X16181;
  public static final int SUBSUC = 0X16101;
  public static final int SUBSUM = 0X16141;
  public static final int SUBSUD = 0X161C1;

  public static final int SUBSSU   = 0X16581;
  public static final int SUBSSUC  = 0X16501;
  public static final int SUBSSUM  = 0X16541;
  public static final int SUBSSUD  = 0X165C1;
  public static final int SUBSSUI  = 0X16781;
  public static final int SUBSSUIC = 0X16701;
  public static final int SUBSSUIM = 0X16741;
  public static final int SUBSSUID = 0X167C1;

  public static final int SUBT   = 0X160A1;
  public static final int SUBTC  = 0X16021;
  public static final int SUBTM  = 0X16061;
  public static final int SUBTD  = 0X160E1;
  public static final int SUBTU  = 0X161A1;
  public static final int SUBTUC = 0X16121;
  public static final int SUBTUM = 0X16161;
  public static final int SUBTUD = 0X161E1;

  public static final int SUBTSU   = 0X165A1;
  public static final int SUBTSUC  = 0X16521;
  public static final int SUBTSUM  = 0X16561;
  public static final int SUBTSUD  = 0X165E1;
  public static final int SUBTSUI  = 0X167A1;
  public static final int SUBTSUIC = 0X16721;
  public static final int SUBTSUIM = 0X16761;
  public static final int SUBTSUID = 0X167E1;

  public static final int CVTTQ     = 0X160AF;
  public static final int CVTTQC    = 0X1602F;
  public static final int CVTTQV    = 0X161AF;
  public static final int CVTTQVC   = 0X1612F;
  public static final int CVTTQSV   = 0X165AF;
  public static final int CVTTQSVC  = 0X1652F;
  public static final int CVTTQSVI  = 0X167AF;
  public static final int CVTTQSVIC = 0X1672F;

  public static final int CVTTQD     = 0X160EF;
  public static final int CVTTQVD    = 0X161EF;
  public static final int CVTTQSVD   = 0X165EF;
  public static final int CVTTQSVID  = 0X167EF;
  public static final int CVTTQM     = 0X1606F;
  public static final int CVTTQVM    = 0X1616F;
  public static final int CVTTQSVM   = 0X1656F;
  public static final int CVTTQSVIM  = 0X1676F;

  public static final int CVTLQ   = 0X17010;
  public static final int CPYS    = 0X17020;
  public static final int CPYSN   = 0X17021;
  public static final int CPYSE   = 0X17022;
  public static final int MT_FPCR = 0X17024;
  public static final int MF_FPCR = 0X17025;
  public static final int CVTQL   = 0X17030;
  public static final int FCMOVEQ = 0X1702A;
  public static final int FCMOVNE = 0X1702B;
  public static final int FCMOVLT = 0X1702C;
  public static final int FCMOVGE = 0X1702D;
  public static final int FCMOVLE = 0X1702E;
  public static final int FCMOVGT = 0X1702F;
  public static final int CVTQLV  = 0X17130;
  public static final int CVTQLSV = 0X17530;

  public static final int FETCH  = 0X18800;
  public static final int FETCHM = 0X18A00;
  public static final int MB     = 0X18400;
  public static final int RC     = 0X18E00;
  public static final int RPCC   = 0X18C00;
  public static final int RS     = 0X18F00;
  public static final int TRAPB  = 0X18000;
  public static final int EVB    = 0X18E80;
  public static final int EXCB   = 0X18040;
  public static final int WH64   = 0X18F80;
  public static final int WMB    = 0X18440;

  public static final int JMP  = 0X1A000;
  public static final int JSR  = 0X1A001;
  public static final int JSRC = 0X1A003;
  public static final int RET  = 0X1A002;

  public static final int SEXTB  = 0x1C000;
  public static final int SEXTW  = 0x1C001;
  public static final int CTPOP  = 0x1C030;
  public static final int PERR   = 0x1C031;
  public static final int CTLZ   = 0x1C032;
  public static final int CTTZ   = 0x1C033;
  public static final int UNPKBW = 0x1C034;
  public static final int UNPKBL = 0x1C035;
  public static final int PKWB   = 0x1C036;
  public static final int PKLB   = 0x1C037;
  public static final int MINSB8 = 0x1C038;
  public static final int MINSW4 = 0x1C039;
  public static final int MINUB8 = 0x1C03A;
  public static final int MINUW4 = 0x1C03B;
  public static final int MAXUB8 = 0x1C03C;
  public static final int MAXUW4 = 0x1C03D;
  public static final int MAXSB8 = 0x1C03E;
  public static final int MAXSW4 = 0x1C03F;
  public static final int FTOIT  = 0x1C070;
  public static final int FTOIS  = 0x1C078;

  /**
   * Map from primary opcode to assembler string.
   */
  private static final String[] opcodes = {
    "pal00", "opc01", "opc02", "opc03",
    "opc04", "opc05", "opc06", "opc07",
    "lda",   "ldah",  "ldbu",  "ldq_u",
    "ldwu",  "stw",   "stb",   "stq_u",
    "op1",   "op2",   "op3",   "op4", 
    "opc14", "vax",   "fop1",  "fop2",
    "mem",   "pal19", "jmp",   "pal1b",
    "opc1c", "pal1d", "pal1e", "pal1f",
    "ldf",   "ldg",   "lds",   "ldt", 
    "stf",   "stg",   "sts",   "stt", 
    "ldl",   "ldq",   "ldl_l", "ldq_l",
    "stl",   "stq",   "stl_c", "stq_c",
    "br ",   "fbeq",  "fblt",  "fble",
    "bsr",   "fbne",  "fbge",  "fbgt",
    "blbc",  "beq",   "blt",   "ble", 
    "blbs",  "bne",   "bge",   "bgt"
  };

  /**
   * Map from primary opcode to secondary function code is used
   * indication.
   */
  public static final boolean[] subop = {
    true,  false, false, false, // 00 - 03
    false, false, false, false, // 04 - 07
    false, false, false, false, // 08 - 0B
    false, false, false, false, // 0C - 0F
    true,  true,  true,  true,  // 10 - 13
    false, true,  true,  true,  // 14 - 17
    true,  false, true,  false, // 18 - 1B
    false, false, false, false, // 1C - 1F
    false, false, false, false, // 20 - 23
    false, false, false, false, // 24 - 27
    false, false, false, false, // 28 - 2B
    false, false, false, false, // 2C - 2F
    false, false, false, false, // 30 - 33
    false, false, false, false, // 34 - 37
    false, false, false, false, // 38 - 3B
    false, false, false, false  // 3C - 3F
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
    int op    = (opcode >> 12) & 0x3f;
    int subop = opcode & 0x7ff;

    switch (op) {
    case 0x00:
      switch (subop) {
      case 0x00: return "HALT";
      case 0x86: return "IMB";
      }
      break;
    case 0x10:
      switch (subop) {
      case 0x00: return "addl";
      case 0x20: return "addq";
      case 0x40: return "addlv";
      case 0x60: return "addqv";
      case 0x3D: return "cmpule";
      case 0x0F: return "cmpbge";
      case 0x02: return "s4addl";
      case 0x22: return "s4addq";
      case 0x2D: return "cmpeq";
      case 0x4D: return "cmplt";
      case 0x6D: return "cmple";
      case 0x1D: return "cmpult";
      case 0x12: return "s8addl";
      case 0x32: return "s8addq";
      case 0x1B: return "s8subl";
      case 0x3B: return "s8subq";
      case 0x09: return "subl";
      case 0x49: return "sublv";
      case 0x29: return "subq";
      case 0x69: return "subqv";
      case 0x0B: return "s4subl";
      case 0x2B: return "s4subq";
      }
      break;
    case 0x11:
      switch (subop) {
      case 0x00: return "and";
      case 0x08: return "bic";
      case 0x24: return "cmoveq";
      case 0x26: return "cmovne";
      case 0x14: return "cmovlbs";
      case 0x20: return "bis";
      case 0x28: return "ornot";
      case 0x44: return "cmovlt";
      case 0x46: return "cmovge";
      case 0x16: return "cmovlbc";
      case 0x40: return "xor";
      case 0x48: return "eqv";
      case 0x61: return "amask";
      case 0x64: return "cmovle";
      case 0x66: return "cmovgt";
      case 0x6c: return "implver";
      }
      break;
    case 0x12:
      switch (subop) {
      case 0x39: return "sll";
      case 0x3C: return "sra";
      case 0x34: return "srl";
      case 0x06: return "extbl";
      case 0x16: return "extwl";
      case 0x26: return "extll";
      case 0x36: return "extql";
      case 0x5A: return "extwh";
      case 0x6A: return "extlh";
      case 0x7A: return "extqh";
      case 0x0B: return "insbl";
      case 0x1B: return "inswl";
      case 0x2B: return "insll";
      case 0x3B: return "insql";
      case 0x57: return "inswh";
      case 0x67: return "inslh";
      case 0x77: return "insqh";
      case 0x02: return "mskbl";
      case 0x12: return "mskwl";
      case 0x22: return "mskll";
      case 0x32: return "mskql";
      case 0x52: return "mskwh";
      case 0x62: return "msklh";
      case 0x72: return "mskqh";
      case 0x30: return "zap";
      case 0x31: return "zapnot";
      }
      break;
    case 0x13:
      switch (subop) {
      case 0x00: return "mull";
      case 0x60: return "mulqv";
      case 0x40: return "mullv";
      case 0x30: return "umulh";
      case 0x20: return "mulq";
      }
      break;
    case 0x14:
      switch (subop) {
      case 0x8A: return "sqrtf";
      case 0xAA: return "sqrtg";
      case 0x8B: return "sqrts";
      case 0xAB: return "sqrtt";
      case 0x14: return "itoff";
      case 0x04: return "itofs";
      case 0x24: return "itoft";
      }
      break;
    case 0x15:
      switch (subop) {
      case 0x080: return "addf";
      case 0x000: return "addfc";
      case 0x180: return "addfu";
      case 0x100: return "addfuc";
      case 0x480: return "addfs";
      case 0x400: return "addfsc";
      case 0x580: return "addfsu";
      case 0x500: return "addfsuc";

      case 0x09E: return "cvtdg";
      case 0x01E: return "cvtdgc";
      case 0x19E: return "cvtdgu";
      case 0x11E: return "cvtdguc";
      case 0x49E: return "cvtdgs";
      case 0x41E: return "cvtdgsc";
      case 0x59E: return "cvtdgsu";
      case 0x51E: return "cvtdgsuc";

      case 0x0A0: return "addg";
      case 0x020: return "addgc";
      case 0x1A0: return "addgu";
      case 0x120: return "addguc";
      case 0x4A0: return "addgs";
      case 0x420: return "addgsc";
      case 0x5A0: return "addgsu";
      case 0x520: return "addgsuc";

      case 0x0A5: return "cmpgeq";
      case 0x4A5: return "cmpgeqs";
      case 0x0A6: return "cmpglt";
      case 0x4A6: return "cmpglts";
      case 0x0A7: return "cmpgle";
      case 0x4A7: return "cmpgles";

      case 0x0AC: return "cvtgf";
      case 0x02C: return "cvtgfc";
      case 0x1AC: return "cvtgfu";
      case 0x12C: return "cvtgfuc";
      case 0x4AC: return "cvtgfs";
      case 0x42C: return "cvtgfsc";
      case 0x5AC: return "cvtgfsu";
      case 0x52C: return "cvtgfsuc";

      case 0x0AD: return "cvtgd";
      case 0x02D: return "cvtgdc";
      case 0x1AD: return "cvtgdu";
      case 0x12D: return "cvtgduc";
      case 0x4AD: return "cvtgds";
      case 0x42D: return "cvtgdsc";
      case 0x5AD: return "cvtgdsu";
      case 0x52D: return "cvtgdsuc";

      case 0x0BC: return "cvtqf";
      case 0x03C: return "cvtqfc";

      case 0x0BE: return "cvtqg";
      case 0x03E: return "cvtqgc";

      case 0x083: return "divf";
      case 0x003: return "divfc";
      case 0x183: return "divfu";
      case 0x103: return "divfuc";
      case 0x483: return "divfs";
      case 0x403: return "divfsc";
      case 0x583: return "divfsu";
      case 0x503: return "divfsuc";

      case 0x0A3: return "divg";
      case 0x023: return "divgc";
      case 0x1A3: return "divgu";
      case 0x123: return "divguc";
      case 0x4A3: return "divgs";
      case 0x423: return "divgsc";
      case 0x5A3: return "divgsu";
      case 0x523: return "divgsuc";

      case 0x082: return "mulf";
      case 0x002: return "mulfc";
      case 0x182: return "mulfu";
      case 0x102: return "mulfuc";
      case 0x482: return "mulfs";
      case 0x402: return "mulfsc";
      case 0x582: return "mulfsu";
      case 0x502: return "mulfsuc";

      case 0x0A2: return "mulg";
      case 0x022: return "mulgc";
      case 0x1A2: return "mulgu";
      case 0x122: return "mulguc";
      case 0x4A2: return "mulgs";
      case 0x422: return "mulgsc";
      case 0x5A2: return "mulgsu";
      case 0x522: return "mulgsuc";

      case 0x081: return "subf";
      case 0x001: return "subfc";
      case 0x181: return "subfu";
      case 0x101: return "subfuc";
      case 0x481: return "subfs";
      case 0x401: return "subfsc";
      case 0x581: return "subfsu";
      case 0x501: return "subfsuc";

      case 0x0A1: return "subg";
      case 0x021: return "subgc";
      case 0x1A1: return "subgu";
      case 0x121: return "subguc";
      case 0x4A1: return "subgs";
      case 0x421: return "subgsc";
      case 0x5A1: return "subgsu";
      case 0x521: return "subgsuc";
      }
      break;
    case 0x16:
      switch (subop) {
      case 0x080: return "adds";
      case 0x000: return "addsc";
      case 0x040: return "addsm";
      case 0x0C0: return "addsd";
      case 0x180: return "addsu";
      case 0x100: return "addsuc";
      case 0x140: return "addsum";
      case 0x1C0: return "addsud";

      case 0x580: return "addssu";
      case 0x500: return "addssuc";
      case 0x540: return "addssum";
      case 0x5C0: return "addssud";
      case 0x780: return "addssui";
      case 0x700: return "addssuic";
      case 0x740: return "addssuim";
      case 0x7C0: return "addssuid";

      case 0x0A0: return "addt";
      case 0x020: return "addtc";
      case 0x060: return "addtm";
      case 0x0E0: return "addtd";
      case 0x1A0: return "addtu";
      case 0x120: return "addtuc";
      case 0x160: return "addtum";
      case 0x1E0: return "addtud";

      case 0x5A0: return "addtsu";
      case 0x520: return "addtsuc";
      case 0x560: return "addtsum";
      case 0x5E0: return "addtsud";
      case 0x7A0: return "addtsui";
      case 0x720: return "addtsuic";
      case 0x760: return "addtsuim";
      case 0x7E0: return "addtsuid";

      case 0x0A4: return "cmptun";
      case 0x0A5: return "cmpteq";
      case 0x0A6: return "cmptlt";
      case 0x0A7: return "cmptle";

      case 0x5A4: return "cmptunsu";
      case 0x5A5: return "cmpteqsu";
      case 0x5A6: return "cmptltsu";
      case 0x5A7: return "cmptlesu";

      case 0x0BC: return "cvtqs";
      case 0x03C: return "cvtqsc";
      case 0x07C: return "cvtqsm";
      case 0x0FC: return "cvtqsd";

      case 0x7BC: return "cvtqssui";
      case 0x73C: return "cvtqssuic";
      case 0x77C: return "cvtqssuim";
      case 0x7FC: return "cvtqssuid";

      case 0x7BE: return "cvtqt";
      case 0x73E: return "cvtqtc";
      case 0x77E: return "cvtqtm";
      case 0x7FE: return "cvtqtd";

      case 0x0BE: return "cvtqtsui";
      case 0x03E: return "cvtqtsuic";
      case 0x07E: return "cvtqtsuim";
      case 0x0FE: return "cvtqtsuid";

      case 0x0AC: return "cvtts";
      case 0x02C: return "cvttsc";
      case 0x06C: return "cvttsm";
      case 0x0EC: return "cvttsd";
      case 0x1AC: return "cvttsu";
      case 0x12C: return "cvttsuc";
      case 0x16C: return "cvttsum";
      case 0x1EC: return "cvttsud";

      case 0x5AC: return "cvttssu";
      case 0x52C: return "cvttssuc";
      case 0x56C: return "cvttssum";
      case 0x5EC: return "cvttssud";
      case 0x7AC: return "cvttssui";
      case 0x72C: return "cvttssuic";
      case 0x76C: return "cvttssuim";
      case 0x7EC: return "cvttssuid";

      case 0x083: return "divs";
      case 0x003: return "divsc";
      case 0x043: return "divsm";
      case 0x0C3: return "divsd";
      case 0x183: return "divsu";
      case 0x103: return "divsuc";
      case 0x143: return "divsum";
      case 0x1C3: return "divsud";

      case 0x583: return "divssu";
      case 0x503: return "divssuc";
      case 0x543: return "divssum";
      case 0x5C3: return "divssud";
      case 0x783: return "divssui";
      case 0x703: return "divssuic";
      case 0x743: return "divssuim";
      case 0x7C3: return "divssuid";

      case 0x0A3: return "divt";
      case 0x023: return "divtc";
      case 0x063: return "divtm";
      case 0x0E3: return "divtd";
      case 0x1A3: return "divtu";
      case 0x123: return "divtuc";
      case 0x163: return "divtum";
      case 0x1E3: return "divtud";

      case 0x5A3: return "divtsu";
      case 0x523: return "divtsuc";
      case 0x563: return "divtsum";
      case 0x5E3: return "divtsud";
      case 0x7A3: return "divtsui";
      case 0x723: return "divtsuic";
      case 0x763: return "divtsuim";
      case 0x7E3: return "divtsuid";

      case 0x082: return "muls";
      case 0x002: return "mulsc";
      case 0x042: return "mulsm";
      case 0x0C2: return "mulsd";
      case 0x182: return "mulsu";
      case 0x102: return "mulsuc";
      case 0x142: return "mulsum";
      case 0x1C2: return "mulsud";

      case 0x582: return "mulssu";
      case 0x502: return "mulssuc";
      case 0x542: return "mulssum";
      case 0x5C2: return "mulssud";
      case 0x782: return "mulssui";
      case 0x702: return "mulssuic";
      case 0x742: return "mulssuim";
      case 0x7C2: return "mulssuid";

      case 0x0A2: return "mult";
      case 0x022: return "multc";
      case 0x062: return "multm";
      case 0x0E2: return "multd";
      case 0x1A2: return "multu";
      case 0x122: return "multuc";
      case 0x162: return "multum";
      case 0x1E2: return "multud";

      case 0x5A2: return "multsu";
      case 0x522: return "multsuc";
      case 0x562: return "multsum";
      case 0x5E2: return "multsud";
      case 0x7A2: return "multsui";
      case 0x722: return "multsuic";
      case 0x762: return "multsuim";
      case 0x7E2: return "multsuid";

      case 0x081: return "subs";
      case 0x001: return "subsc";
      case 0x041: return "subsm";
      case 0x0C1: return "subsd";
      case 0x181: return "subsu";
      case 0x101: return "subsuc";
      case 0x141: return "subsum";
      case 0x1C1: return "subsud";

      case 0x581: return "subssu";
      case 0x501: return "subssuc";
      case 0x541: return "subssum";
      case 0x5C1: return "subssud";
      case 0x781: return "subssui";
      case 0x701: return "subssuic";
      case 0x741: return "subssuim";
      case 0x7C1: return "subssuid";

      case 0x0A1: return "subt";
      case 0x021: return "subtc";
      case 0x061: return "subtm";
      case 0x0E1: return "subtd";
      case 0x1A1: return "subtu";
      case 0x121: return "subtuc";
      case 0x161: return "subtum";
      case 0x1E1: return "subtud";

      case 0x5A1: return "subtsu";
      case 0x521: return "subtsuc";
      case 0x561: return "subtsum";
      case 0x5E1: return "subtsud";
      case 0x7A1: return "subtsui";
      case 0x721: return "subtsuic";
      case 0x761: return "subtsuim";
      case 0x7E1: return "subtsuid";

      case 0x0AF: return "cvttq";
      case 0x02F: return "cvttqc";
      case 0x1AF: return "cvttqv";
      case 0x12F: return "cvttqvc";
      case 0x5AF: return "cvttqsv";
      case 0x52F: return "cvttqsvc";
      case 0x7AF: return "cvttqsvi";
      case 0x72F: return "cvttqsvic";

      case 0x0EF: return "cvttqd";
      case 0x1EF: return "cvttqvd";
      case 0x5EF: return "cvttqsvd";
      case 0x7EF: return "cvttqsvid";
      case 0x06F: return "cvttqm";
      case 0x16F: return "cvttqvm";
      case 0x56F: return "cvttqsvm";
      case 0x76F: return "cvttqsvim";
      }
      break;
    case 0x17:
      switch (subop) {
      case 0x010: return "cvtlq";
      case 0x020: return "cpys";
      case 0x021: return "cpysn";
      case 0x022: return "cpyse";
      case 0x024: return "mt_fpcr";
      case 0x025: return "mf_fpcr";
      case 0x030: return "cvtql";
      case 0x02A: return "fcmoveq";
      case 0x02B: return "fcmovne";
      case 0x02C: return "fcmovlt";
      case 0x02D: return "fcmovge";
      case 0x02E: return "fcmovle";
      case 0x02F: return "fcmovgt";
      case 0x130: return "cvtqlv";
      case 0x530: return "cvtqlsv";
      }
      break;
    case 0x18:
      switch (subop) {
      case 0x800: return "fetch";
      case 0xA00: return "fetchm";
      case 0x400: return "mb";
      case 0xE00: return "rc";
      case 0xC00: return "rpcc";
      case 0xF00: return "rs";
      case 0x000: return "trapb";
      case 0xE80: return "evb";
      case 0x040: return "excb";
      case 0xF80: return "wh64";
      case 0x440: return "wmb";
      }
      break;
    case 0x1A:
      switch (subop) {
      case 0x00: return "jmp";
      case 0x01: return "jsr";
      case 0x02: return "ret";
      case 0x03: return "jsr_coroutine";
      }
      break;
    case 0x1C:
      switch (subop) {
      case 0x00: return "sextb";
      case 0x01: return "sextw";
      case 0x30: return "ctpop";
      case 0x31: return "perr";
      case 0x32: return "ctlz";
      case 0x33: return "cttz";
      case 0x34: return "unpkbw";
      case 0x35: return "unpkbl";
      case 0x36: return "pkwb";
      case 0x37: return "pklb";
      case 0x38: return "minsb8";
      case 0x39: return "minsw4";
      case 0x3A: return "minub8";
      case 0x3B: return "minuw4";
      case 0x3C: return "maxsb8";
      case 0x3D: return "maxsw4";
      case 0x3E: return "maxub8";
      case 0x3F: return "maxuw4";
      case 0x70: return "ftoit";
      case 0x78: return "ftois";
      }
      break;
    default:
      if ((op >= 0) && (op <= 0x3f))
        return opcodes[op];
    }
    throw new scale.common.InternalError("Invalid alpha opcode " +
                                         op +
                                         " " +
                                         subop +
                                         " 0x" +
                                         Integer.toHexString(opcode));
  }

  /**
   * Return the number of cycles required for the instruction.
   */
  public static int getExecutionCycles(int opcode)
  {
    int op    = (opcode >> 12) & 0x3f;
    int subop = opcode & 0x7ff;

    switch (op) {
    case 0x00:
      switch (subop) {
      case 0x00: return 1; // "HALT"
      case 0x86: return 1; // "IMB"
      }
      break;
    case 0x10:
      switch (subop) {
      case 0x00: return 1; // "addl"
      case 0x20: return 1; // "addq"
      case 0x40: return 1; // "addlv"
      case 0x60: return 1; // "addqv"
      case 0x3D: return 1; // "cmpule"
      case 0x0F: return 1; // "cmpbge"
      case 0x02: return 1; // "s4addl"
      case 0x22: return 1; // "s4addq"
      case 0x2D: return 1; // "cmpeq"
      case 0x4D: return 1; // "cmplt"
      case 0x6D: return 1; // "cmple"
      case 0x1D: return 1; // "cmpult"
      case 0x12: return 1; // "s8addl"
      case 0x32: return 1; // "s8addq"
      case 0x1B: return 1; // "s8subl"
      case 0x3B: return 1; // "s8subq"
      case 0x09: return 1; // "subl"
      case 0x49: return 1; // "sublv"
      case 0x29: return 1; // "subq"
      case 0x69: return 1; // "subqv"
      case 0x0B: return 1; // "s4subl"
      case 0x2B: return 1; // "s4subq"
      }
      break;
    case 0x11:
      switch (subop) {
      case 0x00: return 1; // "and"
      case 0x08: return 1; // "bic"
      case 0x24: return 1; // "cmoveq"
      case 0x26: return 1; // "cmovne"
      case 0x14: return 1; // "cmovlbs"
      case 0x20: return 1; // "bis"
      case 0x28: return 1; // "ornot"
      case 0x44: return 1; // "cmovlt"
      case 0x46: return 1; // "cmovge"
      case 0x16: return 1; // "cmovlbc"
      case 0x40: return 1; // "xor"
      case 0x48: return 1; // "eqv"
      case 0x61: return 1; // "amask"
      case 0x64: return 1; // "cmovle"
      case 0x66: return 1; // "cmovgt"
      case 0x6c: return 1; // "implver"
      }
      break;
    case 0x12:
      switch (subop) {
      case 0x39: return 1; // "sll"
      case 0x3C: return 1; // "sra"
      case 0x34: return 1; // "srl"
      case 0x06: return 1; // "extbl"
      case 0x16: return 1; // "extwl"
      case 0x26: return 1; // "extll"
      case 0x36: return 1; // "extql"
      case 0x5A: return 1; // "extwh"
      case 0x6A: return 1; // "extlh"
      case 0x7A: return 1; // "extqh"
      case 0x0B: return 1; // "insbl"
      case 0x1B: return 1; // "inswl"
      case 0x2B: return 1; // "insll"
      case 0x3B: return 1; // "insql"
      case 0x57: return 1; // "inswh"
      case 0x67: return 1; // "inslh"
      case 0x77: return 1; // "insqh"
      case 0x02: return 1; // "mskbl"
      case 0x12: return 1; // "mskwl"
      case 0x22: return 1; // "mskll"
      case 0x32: return 1; // "mskql"
      case 0x52: return 1; // "mskwh"
      case 0x62: return 1; // "msklh"
      case 0x72: return 1; // "mskqh"
      case 0x30: return 1; // "zap"
      case 0x31: return 1; // "zapnot"
      }
      break;
    case 0x13:
      switch (subop) {
      case 0x00: return 3; // "mull"
      case 0x60: return 3; // "mulqv"
      case 0x40: return 3; // "mullv"
      case 0x30: return 3; // "umulh"
      case 0x20: return 3; // "mulq"
      }
      break;
    case 0x14:
      switch (subop) {
      case 0x8A: return 5; // "sqrtf"
      case 0xAA: return 5; // "sqrtg"
      case 0x8B: return 5; // "sqrts"
      case 0xAB: return 5; // "sqrtt"
      case 0x14: return 5; // "itoff"
      case 0x04: return 5; // "itofs"
      case 0x24: return 5; // "itoft"
      }
      break;
    case 0x15:
      switch (subop) {
      case 0x080: return 2; // "addf"
      case 0x000: return 2; // "addfc"
      case 0x180: return 2; // "addfu"
      case 0x100: return 2; // "addfuc"
      case 0x480: return 2; // "addfs"
      case 0x400: return 2; // "addfsc"
      case 0x580: return 2; // "addfsu"
      case 0x500: return 2; // "addfsuc"

      case 0x09E: return 2; // "cvtdg"
      case 0x01E: return 2; // "cvtdgc"
      case 0x19E: return 2; // "cvtdgu"
      case 0x11E: return 2; // "cvtdguc"
      case 0x49E: return 2; // "cvtdgs"
      case 0x41E: return 2; // "cvtdgsc"
      case 0x59E: return 2; // "cvtdgsu"
      case 0x51E: return 2; // "cvtdgsuc"

      case 0x0A0: return 2; // "addg"
      case 0x020: return 2; // "addgc"
      case 0x1A0: return 2; // "addgu"
      case 0x120: return 2; // "addguc"
      case 0x4A0: return 2; // "addgs"
      case 0x420: return 2; // "addgsc"
      case 0x5A0: return 2; // "addgsu"
      case 0x520: return 2; // "addgsuc"

      case 0x0A5: return 2; // "cmpgeq"
      case 0x4A5: return 2; // "cmpgeqs"
      case 0x0A6: return 2; // "cmpglt"
      case 0x4A6: return 2; // "cmpglts"
      case 0x0A7: return 2; // "cmpgle"
      case 0x4A7: return 2; // "cmpgles"

      case 0x0AC: return 2; // "cvtgf"
      case 0x02C: return 2; // "cvtgfc"
      case 0x1AC: return 2; // "cvtgfu"
      case 0x12C: return 2; // "cvtgfuc"
      case 0x4AC: return 2; // "cvtgfs"
      case 0x42C: return 2; // "cvtgfsc"
      case 0x5AC: return 2; // "cvtgfsu"
      case 0x52C: return 2; // "cvtgfsuc"

      case 0x0AD: return 2; // "cvtgd"
      case 0x02D: return 2; // "cvtgdc"
      case 0x1AD: return 2; // "cvtgdu"
      case 0x12D: return 2; // "cvtgduc"
      case 0x4AD: return 2; // "cvtgds"
      case 0x42D: return 2; // "cvtgdsc"
      case 0x5AD: return 2; // "cvtgdsu"
      case 0x52D: return 2; // "cvtgdsuc"

      case 0x0BC: return 2; // "cvtqf"
      case 0x03C: return 2; // "cvtqfc"

      case 0x0BE: return 2; // "cvtqg"
      case 0x03E: return 2; // "cvtqgc"

      case 0x083: return 5; // "divf"
      case 0x003: return 5; // "divfc"
      case 0x183: return 5; // "divfu"
      case 0x103: return 5; // "divfuc"
      case 0x483: return 5; // "divfs"
      case 0x403: return 5; // "divfsc"
      case 0x583: return 5; // "divfsu"
      case 0x503: return 5; // "divfsuc"

      case 0x0A3: return 5; // "divg"
      case 0x023: return 5; // "divgc"
      case 0x1A3: return 5; // "divgu"
      case 0x123: return 5; // "divguc"
      case 0x4A3: return 5; // "divgs"
      case 0x423: return 5; // "divgsc"
      case 0x5A3: return 5; // "divgsu"
      case 0x523: return 5; // "divgsuc"

      case 0x082: return 3; // "mulf"
      case 0x002: return 3; // "mulfc"
      case 0x182: return 3; // "mulfu"
      case 0x102: return 3; // "mulfuc"
      case 0x482: return 3; // "mulfs"
      case 0x402: return 3; // "mulfsc"
      case 0x582: return 3; // "mulfsu"
      case 0x502: return 3; // "mulfsuc"

      case 0x0A2: return 3; // "mulg"
      case 0x022: return 3; // "mulgc"
      case 0x1A2: return 3; // "mulgu"
      case 0x122: return 3; // "mulguc"
      case 0x4A2: return 3; // "mulgs"
      case 0x422: return 3; // "mulgsc"
      case 0x5A2: return 3; // "mulgsu"
      case 0x522: return 3; // "mulgsuc"

      case 0x081: return 2; // "subf"
      case 0x001: return 2; // "subfc"
      case 0x181: return 2; // "subfu"
      case 0x101: return 2; // "subfuc"
      case 0x481: return 2; // "subfs"
      case 0x401: return 2; // "subfsc"
      case 0x581: return 2; // "subfsu"
      case 0x501: return 2; // "subfsuc"

      case 0x0A1: return 2; // "subg"
      case 0x021: return 2; // "subgc"
      case 0x1A1: return 2; // "subgu"
      case 0x121: return 2; // "subguc"
      case 0x4A1: return 2; // "subgs"
      case 0x421: return 2; // "subgsc"
      case 0x5A1: return 2; // "subgsu"
      case 0x521: return 2; // "subgsuc"
      }
      break;
    case 0x16:
      switch (subop) {
      case 0x080: return 2; // "adds"
      case 0x000: return 2; // "addsc"
      case 0x040: return 2; // "addsm"
      case 0x0C0: return 2; // "addsd"
      case 0x180: return 2; // "addsu"
      case 0x100: return 2; // "addsuc"
      case 0x140: return 2; // "addsum"
      case 0x1C0: return 2; // "addsud"

      case 0x580: return 2; // "addssu"
      case 0x500: return 2; // "addssuc"
      case 0x540: return 2; // "addssum"
      case 0x5C0: return 2; // "addssud"
      case 0x780: return 2; // "addssui"
      case 0x700: return 2; // "addssuic"
      case 0x740: return 2; // "addssuim"
      case 0x7C0: return 2; // "addssuid"

      case 0x0A0: return 2; // "addt"
      case 0x020: return 2; // "addtc"
      case 0x060: return 2; // "addtm"
      case 0x0E0: return 2; // "addtd"
      case 0x1A0: return 2; // "addtu"
      case 0x120: return 2; // "addtuc"
      case 0x160: return 2; // "addtum"
      case 0x1E0: return 2; // "addtud"

      case 0x5A0: return 2; // "addtsu"
      case 0x520: return 2; // "addtsuc"
      case 0x560: return 2; // "addtsum"
      case 0x5E0: return 2; // "addtsud"
      case 0x7A0: return 2; // "addtsui"
      case 0x720: return 2; // "addtsuic"
      case 0x760: return 2; // "addtsuim"
      case 0x7E0: return 2; // "addtsuid"

      case 0x0A4: return 2; // "cmptun"
      case 0x0A5: return 2; // "cmpteq"
      case 0x0A6: return 2; // "cmptlt"
      case 0x0A7: return 2; // "cmptle"

      case 0x5A4: return 2; // "cmptunsu"
      case 0x5A5: return 2; // "cmpteqsu"
      case 0x5A6: return 2; // "cmptltsu"
      case 0x5A7: return 2; // "cmptlesu"

      case 0x0BC: return 2; // "cvtqs"
      case 0x03C: return 2; // "cvtqsc"
      case 0x07C: return 2; // "cvtqsm"
      case 0x0FC: return 2; // "cvtqsd"

      case 0x7BC: return 2; // "cvtqssui"
      case 0x73C: return 2; // "cvtqssuic"
      case 0x77C: return 2; // "cvtqssuim"
      case 0x7FC: return 2; // "cvtqssuid"

      case 0x7BE: return 2; // "cvtqt"
      case 0x73E: return 2; // "cvtqtc"
      case 0x77E: return 2; // "cvtqtm"
      case 0x7FE: return 2; // "cvtqtd"

      case 0x0BE: return 2; // "cvtqtsui"
      case 0x03E: return 2; // "cvtqtsuic"
      case 0x07E: return 2; // "cvtqtsuim"
      case 0x0FE: return 2; // "cvtqtsuid"

      case 0x0AC: return 2; // "cvtts"
      case 0x02C: return 2; // "cvttsc"
      case 0x06C: return 2; // "cvttsm"
      case 0x0EC: return 2; // "cvttsd"
      case 0x1AC: return 2; // "cvttsu"
      case 0x12C: return 2; // "cvttsuc"
      case 0x16C: return 2; // "cvttsum"
      case 0x1EC: return 2; // "cvttsud"

      case 0x5AC: return 2; // "cvttssu"
      case 0x52C: return 2; // "cvttssuc"
      case 0x56C: return 2; // "cvttssum"
      case 0x5EC: return 2; // "cvttssud"
      case 0x7AC: return 2; // "cvttssui"
      case 0x72C: return 2; // "cvttssuic"
      case 0x76C: return 2; // "cvttssuim"
      case 0x7EC: return 2; // "cvttssuid"

      case 0x083: return 5; // "divs"
      case 0x003: return 5; // "divsc"
      case 0x043: return 5; // "divsm"
      case 0x0C3: return 5; // "divsd"
      case 0x183: return 5; // "divsu"
      case 0x103: return 5; // "divsuc"
      case 0x143: return 5; // "divsum"
      case 0x1C3: return 5; // "divsud"

      case 0x583: return 5; // "divssu"
      case 0x503: return 5; // "divssuc"
      case 0x543: return 5; // "divssum"
      case 0x5C3: return 5; // "divssud"
      case 0x783: return 5; // "divssui"
      case 0x703: return 5; // "divssuic"
      case 0x743: return 5; // "divssuim"
      case 0x7C3: return 5; // "divssuid"

      case 0x0A3: return 5; // "divt"
      case 0x023: return 5; // "divtc"
      case 0x063: return 5; // "divtm"
      case 0x0E3: return 5; // "divtd"
      case 0x1A3: return 5; // "divtu"
      case 0x123: return 5; // "divtuc"
      case 0x163: return 5; // "divtum"
      case 0x1E3: return 5; // "divtud"

      case 0x5A3: return 5; // "divtsu"
      case 0x523: return 5; // "divtsuc"
      case 0x563: return 5; // "divtsum"
      case 0x5E3: return 5; // "divtsud"
      case 0x7A3: return 5; // "divtsui"
      case 0x723: return 5; // "divtsuic"
      case 0x763: return 5; // "divtsuim"
      case 0x7E3: return 5; // "divtsuid"

      case 0x082: return 3; // "muls"
      case 0x002: return 3; // "mulsc"
      case 0x042: return 3; // "mulsm"
      case 0x0C2: return 3; // "mulsd"
      case 0x182: return 3; // "mulsu"
      case 0x102: return 3; // "mulsuc"
      case 0x142: return 3; // "mulsum"
      case 0x1C2: return 3; // "mulsud"

      case 0x582: return 3; // "mulssu"
      case 0x502: return 3; // "mulssuc"
      case 0x542: return 3; // "mulssum"
      case 0x5C2: return 3; // "mulssud"
      case 0x782: return 3; // "mulssui"
      case 0x702: return 3; // "mulssuic"
      case 0x742: return 3; // "mulssuim"
      case 0x7C2: return 3; // "mulssuid"

      case 0x0A2: return 3; // "mult"
      case 0x022: return 3; // "multc"
      case 0x062: return 3; // "multm"
      case 0x0E2: return 3; // "multd"
      case 0x1A2: return 3; // "multu"
      case 0x122: return 3; // "multuc"
      case 0x162: return 3; // "multum"
      case 0x1E2: return 3; // "multud"

      case 0x5A2: return 3; // "multsu"
      case 0x522: return 3; // "multsuc"
      case 0x562: return 3; // "multsum"
      case 0x5E2: return 3; // "multsud"
      case 0x7A2: return 3; // "multsui"
      case 0x722: return 3; // "multsuic"
      case 0x762: return 3; // "multsuim"
      case 0x7E2: return 3; // "multsuid"

      case 0x081: return 2; // "subs"
      case 0x001: return 2; // "subsc"
      case 0x041: return 2; // "subsm"
      case 0x0C1: return 2; // "subsd"
      case 0x181: return 2; // "subsu"
      case 0x101: return 2; // "subsuc"
      case 0x141: return 2; // "subsum"
      case 0x1C1: return 2; // "subsud"

      case 0x581: return 2; // "subssu"
      case 0x501: return 2; // "subssuc"
      case 0x541: return 2; // "subssum"
      case 0x5C1: return 2; // "subssud"
      case 0x781: return 2; // "subssui"
      case 0x701: return 2; // "subssuic"
      case 0x741: return 2; // "subssuim"
      case 0x7C1: return 2; // "subssuid"

      case 0x0A1: return 2; // "subt"
      case 0x021: return 2; // "subtc"
      case 0x061: return 2; // "subtm"
      case 0x0E1: return 2; // "subtd"
      case 0x1A1: return 2; // "subtu"
      case 0x121: return 2; // "subtuc"
      case 0x161: return 2; // "subtum"
      case 0x1E1: return 2; // "subtud"

      case 0x5A1: return 2; // "subtsu"
      case 0x521: return 2; // "subtsuc"
      case 0x561: return 2; // "subtsum"
      case 0x5E1: return 2; // "subtsud"
      case 0x7A1: return 2; // "subtsui"
      case 0x721: return 2; // "subtsuic"
      case 0x761: return 2; // "subtsuim"
      case 0x7E1: return 2; // "subtsuid"

      case 0x0AF: return 2; // "cvttq"
      case 0x02F: return 2; // "cvttqc"
      case 0x1AF: return 2; // "cvttqv"
      case 0x12F: return 2; // "cvttqvc"
      case 0x5AF: return 2; // "cvttqsv"
      case 0x52F: return 2; // "cvttqsvc"
      case 0x7AF: return 2; // "cvttqsvi"
      case 0x72F: return 2; // "cvttqsvic"

      case 0x0EF: return 2; // "cvttqd"
      case 0x1EF: return 2; // "cvttqvd"
      case 0x5EF: return 2; // "cvttqsvd"
      case 0x7EF: return 2; // "cvttqsvid"
      case 0x06F: return 2; // "cvttqm"
      case 0x16F: return 2; // "cvttqvm"
      case 0x56F: return 2; // "cvttqsvm"
      case 0x76F: return 2; // "cvttqsvim"
      }
      break;
    case 0x17:
      switch (subop) {
      case 0x010: return 1; // "cvtlq"
      case 0x020: return 1; // "cpys"
      case 0x021: return 1; // "cpysn"
      case 0x022: return 1; // "cpyse"
      case 0x024: return 1; // "mt_fpcr"
      case 0x025: return 1; // "mf_fpcr"
      case 0x030: return 1; // "cvtql"
      case 0x02A: return 1; // "fcmoveq"
      case 0x02B: return 1; // "fcmovne"
      case 0x02C: return 1; // "fcmovlt"
      case 0x02D: return 1; // "fcmovge"
      case 0x02E: return 1; // "fcmovle"
      case 0x02F: return 1; // "fcmovgt"
      case 0x130: return 1; // "cvtqlv"
      case 0x530: return 1; // "cvtqlsv"
      }
      break;
    case 0x18:
      switch (subop) {
      case 0x800: return 1; // "fetch"
      case 0xA00: return 1; // "fetchm"
      case 0x400: return 1; // "mb"
      case 0xE00: return 1; // "rc"
      case 0xC00: return 1; // "rpcc"
      case 0xF00: return 1; // "rs"
      case 0x000: return 1; // "trapb"
      case 0xE80: return 1; // "evb"
      case 0x040: return 1; // "excb"
      case 0xF80: return 1; // "wh64"
      case 0x440: return 1; // "wmb"
      }
      break;
    case 0x1A:
      switch (subop) {
      case 0x00: return 3; // "jmp"
      case 0x01: return 3; // "jsr"
      case 0x02: return 3; // "ret"
      case 0x03: return 3; // "jsr_coroutine"
      }
      break;
    case 0x1C:
      switch (subop) {
      case 0x00: return 1; // "sextb"
      case 0x01: return 1; // "sextw"
      case 0x30: return 1; // "ctpop"
      case 0x31: return 1; // "perr"
      case 0x32: return 1; // "ctlz"
      case 0x33: return 1; // "cttz"
      case 0x34: return 1; // "unpkbw"
      case 0x35: return 1; // "unpkbl"
      case 0x36: return 1; // "pkwb"
      case 0x37: return 1; // "pklb"
      case 0x38: return 1; // "minsb8"
      case 0x39: return 1; // "minsw4"
      case 0x3A: return 1; // "minub8"
      case 0x3B: return 1; // "minuw4"
      case 0x3C: return 1; // "maxsb8"
      case 0x3D: return 1; // "maxsw4"
      case 0x3E: return 1; // "maxub8"
      case 0x3F: return 1; // "maxuw4"
      case 0x70: return 3; // "ftoit"
      case 0x78: return 3; // "ftois"
      }
      break;
    case 0x08: return 1; // "lda"
    case 0x09: return 1; // "ldah"
    case 0x0A: return 7; // "ldbu"
    case 0x0B: return 7; // "ldq_u"
    case 0x0C: return 7; // "ldwu"
    case 0x0D: return 2; // "stw"
    case 0x0E: return 2; // "stb"
    case 0x0F: return 2; // "stq_u"
    case 0x19: return 1; // "pal19"
    case 0x1B: return 1; // "pal1b"
    case 0x1D: return 1; // "pal1d"
    case 0x1E: return 1; // "pal1e"
    case 0x1F: return 1; // "pal1f"
    case 0x20: return 7; // "ldf"
    case 0x21: return 7; // "ldg"
    case 0x22: return 7; // "lds"
    case 0x23: return 7; // "ldt"
    case 0x24: return 2; // "stf"
    case 0x25: return 2; // "stg"
    case 0x26: return 2; // "sts"
    case 0x27: return 2; // "stt"
    case 0x28: return 7; // "ldl"
    case 0x29: return 7; // "ldq"
    case 0x2A: return 7; // "ldl_l"
    case 0x2B: return 7; // "ldq_l
    case 0x2C: return 2; // "stl"
    case 0x2D: return 2; // "stq"
    case 0x2E: return 2; // "stl_c"
    case 0x2F: return 2; // "stq_c"
    case 0x30: return 3; // "br "
    case 0x31: return 3; // "fbeq"
    case 0x32: return 3; // "fblt"
    case 0x33: return 3; // "fble"
    case 0x34: return 3; // "bsr"
    case 0x35: return 3; // "fbne"
    case 0x36: return 3; // "fbge"
    case 0x37: return 3; // "fbgt"
    case 0x38: return 3; // "blbc"
    case 0x39: return 3; // "beq"
    case 0x3A: return 3; // "blt"
    case 0x3B: return 3; // "ble"
    case 0x3C: return 3; // "blbs"
    case 0x3D: return 3; // "bne"
    case 0x3E: return 3; // "bge"
    case 0x3F: return 3; // "bgt"
    default:
      return 1;
    }
    throw new scale.common.InternalError("Invalid alpha opcode " +
                                         op +
                                         " " +
                                         subop +
                                         " " +
                                         getOp(opcode));
  }
}
