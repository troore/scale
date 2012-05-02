package scale.backend.xyz;

import java.util.Enumeration;
import java.math.BigInteger;

import scale.backend.*;
import scale.common.*;
import scale.callGraph.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.score.chords.*;
import scale.score.expr.*;

/** 
 * This class converts Scribble into Xyz instructions.
 * <p>
 * $Id: XyzGenerator.java,v 1.2 2007-02-15 15:32:11 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class generates the instructions for the specific
 * architecture.  Most of the methods below generate a "not
 * implemented error".  These must be replaced with code that
 * generates the proper instruction sequence.  The other methods may
 * need to be modified.  Use the code generators from the other
 * architectures to help you understand how to make changes for your
 * architecture.
 * @see scale.backend.Generator
 * @see scale.backend.alpha.AlphaGenerator
 * @see scale.backend.sparc.SparcGenerator
 * @see scale.backend.ppc.PPCGenerator
 */

public class XyzGenerator extends scale.backend.Generator
{
  // Change these loader-understood areas as needed.
  /**
   * Un-initialized large data area.
   */
  public static final int BSS = 0;
  /**
   * Un-initialized small data area.
   */
  public static final int SBSS = 1;
  /**
   * Initialized large data area.
   */
  public static final int DATA = 2;
  /**
   * Initialized 4-byte data area.
   */
  public static final int LIT4 = 3;
  /**
   * Initialized 8-byte data area.
   */
  public static final int LIT8 = 4;
  /**
   * Initialized address data area.
   */
  public static final int LITA = 5;
  /**
   * Read-only constants
   */
  public static final int RCONST = 6;
  /**
   * Read-only data.
   */
  public static final int RDATA = 7;
  /**
   * Initialized small data area.
   */
  public static final int SDATA = 8;
  /**
   * Instructions.
   */
  public static final int TEXT = 9;

  /**
   * @param cg is the call graph to be transformed
   * @param machine specifies machine details
   * @param features contains various flags
   */
  public XyzGenerator(CallGraph cg, Machine machine, int features)
  {
    super(cg, new XyzRegisterSet(), machine, features);

    assert (machine instanceof XyzMachine) : "Not correct machine " + machine;

    this.un = new UniqueName("$$");

    // Determine what additional Xyz features are available.

    XyzMachine am = (XyzMachine) machine;

    readOnlyDataArea = RDATA;
  }

  public void generateScribble()
  {
    stkPtrReg = XyzRegisterSet.SP_REG;

    super.generateScribble();
  }

  protected void peepholeBeforeRegisterAllocation(Instruction first)
  {
    Instruction inst = first;

    while (inst != null) {
      Instruction next   = inst.getNext();
      int         opcode = inst.getOpcode();

      // Recognize patterns, if any, here.

      inst = next;
    }
  }

  public void assemble(Emit emit, String source, Enumeration<String> comments)
  {
    while (comments.hasMoreElements()) {
      emit.emit("\t# ");
      emit.emit(comments.nextElement());
      emit.endLine();
    }
    if (!nis) {
      emit.emit("\t# Instruction Scheduling");
      emit.endLine();
    }
    XyzAssembler asm = new XyzAssembler(this, source, !nis);
    asm.assemble(emit, dataAreas);
  }

  public int dataType(int size, boolean flt)
  {
    int t = SpaceAllocation.DAT_NONE;
    switch (size) {
    case 1: t = SpaceAllocation.DAT_BYTE;  break;
    case 2: t = SpaceAllocation.DAT_SHORT; break;
    case 4: t = flt ? SpaceAllocation.DAT_FLT : SpaceAllocation.DAT_INT;   break;
    case 8: t = flt ? SpaceAllocation.DAT_DBL : SpaceAllocation.DAT_LONG;  break;
    case 16: t = flt ? SpaceAllocation.DAT_LDBL : SpaceAllocation.DAT_LONG;  break;
    default: throw new scale.common.InternalError("Can't allocate objects of size " + size);
    }
    return t;
  }

  public int getSAType(Type type)
  {
    throw new scale.common.NotImplementedError("getSAType");
  }

  protected void assignDeclToMemory(String name, VariableDecl vd)
  {
    throw new scale.common.NotImplementedError("assignDeclToMemory");
  }

  protected void assignDeclToRegister(VariableDecl vd)
  {
    throw new scale.common.NotImplementedError("assignDeclToRegister");
  }

  protected void assignDeclToStack(VariableDecl vd)
  {
    throw new scale.common.NotImplementedError("assignDeclToStack");
  }

  protected void processRoutineDecl(RoutineDecl rd, boolean topLevel)
  {
    throw new scale.common.NotImplementedError("processRoutineDecl");
  }

  protected void layoutParameters()
  {
    throw new scale.common.NotImplementedError("layoutParameters");
  }

  public int returnRegister(int regType, boolean isCall)
  {
    throw new scale.common.NotImplementedError("returnRegister");
  }

  public final int getFirstArgRegister(int regType)
  {
    throw new scale.common.NotImplementedError("getFirstArgRegister");
  }

  protected Displacement defStringValue(String v, int size)
  {
    throw new scale.common.NotImplementedError("defStringValue");
  }

  protected void genRegToReg(int src, int dest)
  {
    throw new scale.common.NotImplementedError("genRegToReg");
  }

  protected void addRegs(int laReg, int raReg, int dest)
  {
    throw new scale.common.NotImplementedError("addRegs");
  }

  protected int loadMemoryAddress(Displacement disp)
  {
    throw new scale.common.NotImplementedError("loadMemoryAddress");
  }

  protected int loadStackAddress(Displacement disp)
  {
    throw new scale.common.NotImplementedError("loadStackAddress");
  }

  protected void genLoadImmediate(long value, int base, int dest)
  {
    throw new scale.common.NotImplementedError("genLoadImmediate");
  }

  protected int genLoadImmediate(long value, int dest)
  {
    throw new scale.common.NotImplementedError("genLoadImmediate");
  }

  protected int genLoadDblImmediate(double value, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genLoadDblImmediate");
  }

  protected long genLoadHighImmediate(long value, int base)
  {
    throw new scale.common.NotImplementedError("genLoadHighImmediate");
  }

  protected void basicBlockEnd()
  {
    throw new scale.common.NotImplementedError("basicBlockEnd");
  }

  protected int allocStackAddress(int adrReg, Type type)
  {
    throw new scale.common.NotImplementedError("allocStackAddress");
  }

  protected void loadFromMemoryWithOffset(int dest, int address, long offset, int size, long alignment, boolean signed, boolean real)
  {
    throw new scale.common.NotImplementedError("loadFromMemoryWithOffset");
  }

  protected void loadFromMemoryWithOffset(int dest, int address, Displacement offset, int size, long alignment, boolean signed, boolean real)
  {
    throw new scale.common.NotImplementedError("loadFromMemoryWithOffset");
  }

  protected void loadFromMemoryDoubleIndexing(int dest, int index1, int index2, int size, long alignment, boolean signed, boolean real)
  {
    throw new scale.common.NotImplementedError("loadFromMemoryDoubleIndexing");
  }

  protected void storeIntoMemory(int src, int address, int size, long alignment, boolean real)
  {
    throw new scale.common.NotImplementedError("storeIntoMemory");
  }

  protected  void storeIntoMemoryWithOffset(int src, int address, long offset, int size, long alignment, boolean real)
  {
    throw new scale.common.NotImplementedError("storeIntoMemoryWithOffset");
  }

  protected  void storeIntoMemoryWithOffset(int src, int address, Displacement offset, int size, long alignment, boolean real)
  {
    throw new scale.common.NotImplementedError("storeIntoMemoryWithOffset");
  }

  protected void moveWords(int src, long srcoff, int dest, Displacement destoff, int size, int aln)
  {
    throw new scale.common.NotImplementedError("moveWords");
  }

  protected void moveWords(int src, long srcoff, int dest, long destoff, int size, int aln)
  {
    throw new scale.common.NotImplementedError("moveWords");
  }

  protected void loadRegFromSymbolicLocation(int dest, int dsize, boolean isSigned,  boolean isReal, Displacement disp)
  {
    throw new scale.common.NotImplementedError("loadRegFromSymbolicLocation");
  }

  protected void doBinaryOp(int which, Type ct, Expr la, Expr ra, int ir)
  {
    throw new scale.common.NotImplementedError("doBinaryOp");
  }

  protected void doCompareOp(BinaryExpr c, CompareMode which)
  {
    throw new scale.common.NotImplementedError("doCompareOp");
  }

  protected Instruction startRoutineCode()
  {
    throw new scale.common.NotImplementedError("startRoutineCode");
  }

  protected void processSourceLine(int line, Label lab, boolean newLine)
  {
    throw new scale.common.NotImplementedError("processSourceLine");
  }

  public void generateUnconditionalBranch(Label lab)
  {
    throw new scale.common.NotImplementedError("generateUnconditionalBranch");
  }

  public Object getSpillLocation(int reg)
  {
    throw new scale.common.NotImplementedError("getSpillLocation");
  }

  public Instruction insertSpillLoad(int reg, Object spillLocation, Instruction after)
  {
    throw new scale.common.NotImplementedError("insertSpillLoad");
  }

  public Instruction insertSpillStore(int reg, Object spillLocation, Instruction after)
  {
    throw new scale.common.NotImplementedError("insertSpillStore");
  }

  protected void endRoutineCode(int[] regMap)
  {
    throw new scale.common.NotImplementedError("endRoutineCode");
  }

  protected Branch genFtnCall(String name, short[] uses, short[] defs)
  {
    throw new scale.common.NotImplementedError("genFtnCall");
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    throw new scale.common.NotImplementedError("visitAbsoluteValueExpr");
  }

  public void generateProlog(ProcedureType pt)
  {
    throw new scale.common.NotImplementedError("generateProlog");
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    throw new scale.common.NotImplementedError("visitBitComplementExpr");
  }

  protected short[] callArgs(Expr[] args, boolean retStruct)
  {
    throw new scale.common.NotImplementedError("callArgs");
  }

  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    throw new scale.common.NotImplementedError("visitCallFunctionExpr");
  }

  public void visitCompareExpr(CompareExpr e)
  {
    throw new scale.common.NotImplementedError("visitCompareExpr");
  }

  protected int convertIntRegValue(int src, int srcSize, boolean srcSigned, int dest, int destSize, boolean destSigned)
  {
    throw new scale.common.NotImplementedError("convertIntRegValue");
  }

  protected void zeroFloatRegister(int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("zeroFloatRegister");
  }

  protected void genRealPart(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genRealPart");
  }

  protected int genRealToInt(int src, int srcSize, int dest, int destSize, boolean destSigned)
  {
    throw new scale.common.NotImplementedError("genRealToInt");
  }

  protected void genRealToReal(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genRealToReal");
  }

  protected void genRealToIntRound(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genRealToIntRound");
  }

  protected void genRoundReal(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genRoundReal");
  }

  protected void genIntToReal(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genIntToReal");
  }

  protected void genUnsignedIntToReal(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genUnsignedIntToReal");
  }

  protected void genFloorOfReal(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genFloorOfReal");
  }

  public void visitExponentiationExpr(ExponentiationExpr e)
  {
    throw new scale.common.NotImplementedError("visitExponentiationExpr");
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    throw new scale.common.NotImplementedError("visitDivisionExpr");
  }

  public void visitRemainderExpr(RemainderExpr e)
  {
    throw new scale.common.NotImplementedError("visitRemainderExpr");
  }

  protected short[] genSingleUse(int reg)
  {
    throw new scale.common.NotImplementedError("genSingleUse");
  }

  protected short[] genDoubleUse(int reg1, int reg2)
  {
    throw new scale.common.NotImplementedError("genDoubleUse");
  }

  protected void loadFieldValue(FieldDecl  fd,
                                long       fieldOffset,
                                int        adr,
                                ResultMode adrha,
                                int        adraln,
                                long       adrrs,
                                int        dest)
  {
    throw new scale.common.NotImplementedError("loadFieldValue");
  }

  protected void genIfRegister(CompareMode which, int treg, boolean signed, Label labt, Label labf)
  {
    throw new scale.common.NotImplementedError("genIfRegister");
  }

  protected void genIfRelational(boolean rflag, MatchExpr predicate, Chord tc, Chord fc)
  {
    throw new scale.common.NotImplementedError("genIfRelational");
  }

  protected void loadArrayElement(ArrayIndexExpr aie, int dest)
  {
    throw new scale.common.NotImplementedError("loadArrayElement");
  }

  protected void calcArrayElementAddress(ArrayIndexExpr aie, long offseta)
  {
    throw new scale.common.NotImplementedError("calcArrayElementAddress");
  }

  public void visitMultiplicationExpr(MultiplicationExpr e)
  { 
    throw new scale.common.NotImplementedError("visitMultiplicationExpr");
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    throw new scale.common.NotImplementedError("visitNegativeExpr");
  }

  protected void genAlloca(Expr arg, int reg)
  {
    throw new scale.common.NotImplementedError("genAlloca");
  }

  protected void genSignFtn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("genSignFtn");
  }

  protected void genAtan2Ftn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("genAtan2Ftn");
  }

  protected void genDimFtn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("genDimFtn");
  }


  private void genFtnCall(String fname, int dest, int src, Type type)
  {
    throw new scale.common.NotImplementedError("genFtnCall");
  }


  protected void genSqrtFtn(int dest, int src, Type type)
  {
    genFtnCall("sqrt", dest, src, type);
  }

  protected void genExpFtn(int dest, int src, Type type)
  {
    genFtnCall("exp", dest, src, type);
  }

  protected void genLogFtn(int dest, int src, Type type)
  {
    genFtnCall("log", dest, src, type);
  }

  protected void genLog10Ftn(int dest, int src, Type type)
  {
    genFtnCall("log10", dest, src, type);
  }

  protected void genSinFtn(int dest, int src, Type type)
  {
    genFtnCall("sin", dest, src, type);
  }

  protected void genCosFtn(int dest, int src, Type type)
  {
    genFtnCall("cos", dest, src, type);
  }

  protected void genTanFtn(int dest, int src, Type type)
  {
    genFtnCall("tan", dest, src, type);
  }

  protected void genAsinFtn(int dest, int src, Type type)
  {
    genFtnCall("asin", dest, src, type);
  }

  protected void genAcosFtn(int dest, int src, Type type)
  {
    genFtnCall("acos", dest, src, type);
  }

  protected void genAtanFtn(int dest, int src, Type type)
  {
    genFtnCall("atan", dest, src, type);
  }

  protected void genSinhFtn(int dest, int src, Type type)
  {
    genFtnCall("sinh", dest, src, type);
  }

  protected void genCoshFtn(int dest, int src, Type type)
  {
    genFtnCall("cosh", dest, src, type);
  }

  protected void genTanhFtn(int dest, int src, Type type)
  {
    genFtnCall("tanh", dest, src, type);
  }

  protected void genConjgFtn(int dest, int src, Type type)
  {
    throw new scale.common.NotImplementedError("genConjgFtn");
  }

  protected void genReturnAddressFtn(int dest, int src, Type type)
  {
    throw new scale.common.NotImplementedError("genReturnAddressFtn");
  }

  protected void genFrameAddressFtn(int dest, int src, Type type)
  {
    throw new scale.common.NotImplementedError("genFrameAddressFtn");
  }

  public void visitNotExpr(NotExpr e)
  {
    throw new scale.common.NotImplementedError("visitNotExpr");
  }

  public void visitReturnChord(ReturnChord c)
  {
    throw new scale.common.NotImplementedError("visitReturnChord");
  }

  protected void storeRegToSymbolicLocation(int src, int dsize, long alignment, boolean isReal, Displacement disp)
  {
    throw new scale.common.NotImplementedError("storeRegToSymbolicLocation");
  }

  protected void storeLfae(LoadFieldAddressExpr lhs, Expr rhs)
  {
    throw new scale.common.NotImplementedError("storeLfae");
  }

  protected boolean genSwitchUsingIfs(int testReg, Chord[] cases, long[] keys, int num, long spread)
  {
    throw new scale.common.NotImplementedError("genSwitchUsingIfs");
  }

  protected void genSwitchUsingTransferVector(int testReg, Chord[] cases, long[] keys, Label labt, long min, long max)
  {
    throw new scale.common.NotImplementedError("genSwitchUsingTransferVector");
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    throw new scale.common.NotImplementedError("visitVaStartExpr");
  }

  protected void doVaCopy(Expr dst, Expr src)  
  {
    throw new scale.common.NotImplementedError("doVaCopy");
  }

  public void visitVaArgExpr(VaArgExpr e)
  {
    throw new scale.common.NotImplementedError("visitVaArgExpr");
  }

  public void visitConditionalExpr(ConditionalExpr e)
  {
    throw new scale.common.NotImplementedError("visitConditionalExpr");
  }

  public int getMaxAreaIndex()
  {
    throw new scale.common.NotImplementedError("getMaxAreaIndex");
  }
}
