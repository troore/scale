package scale.backend.mips;

import java.util.Enumeration;
import java.math.BigInteger;

import scale.backend.*;
import scale.common.*;
import scale.callGraph.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.clef.LiteralMap;
import scale.score.*;
import scale.clef.type.*;
import scale.score.chords.*;
import scale.score.expr.*;

/** 
 * This class converts Scribble into Mips instructions.
 * <p>
 * $Id: MipsGenerator.java,v 1.98 2007-10-04 19:57:54 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This code generator was never completed and it hasn't been under
 * regression test.
 */

public class MipsGenerator extends scale.backend.Generator
{
  /**
   * Instructions.
   */
  public static final int TEXT = 0;
  /**
   * Read-only data.
   */
  public static final int RDATA = 1;
  /**
   * Initialized large data area.
   */
  public static final int DATA = 2;
  /**
   * Initialized 8-byte data area.
   */
  public static final int LIT8 = 3; 
  /**
   * Initialized 4-byte data area.
   */
  public static final int LIT4 = 4;
  /**
   * Initialized small data area.
   */
  public static final int SDATA = 5;
  /**
   * Un-initialized small data area.
   */
  public static final int SBSS = 6;
  /**
   * Un-initialized large data area.
   */
  public static final int BSS = 7;


  private Vector<StackDisplacement> localVar;      // Set of Displacements that reference variables on the stack.
  private Vector<Displacement>      entryOverflow; // Set of Displacements that reference argument overflow during call.
  private StackDisplacement         argDisp;       // Displacement to argument save area.

  private BeginMarker  currentBeginMarker;  // Mark the start of the routine's code.
  private RoutineDecl  currentRoutine;      // The declaration associated with this routine.
  private int          localVarSize;        // Size of the area on the stack used for local variables.
  protected Label      startLabel;          // Label placed after function prolog.
  private int          structAddress;       // Register containing structure address for routines that return structures
  private int          structSize;          // Register containing size of structure for routines that return structures
  private boolean      usesGp;              // True if this routine uses the Gp register.
  private boolean      callsRoutine;        // True if this routine calls another routine.
  private int          argumentBuildSize;   // Size of the area required for passing large arguments to subroutines.
  private int          mask;                // Integer registers saved during routine execution.
  private int          fmask;               // Floating point registers saved during routine execution.
  private int          entryOverflowSize;   // Size of the area on the stack used for overflow arguments.

  public static final int MAX_IMM16 = 32767;
  public static final int MIN_IMM16 = -32768;

  public static final int FT_NONE = 0;
  public static final int FT_HI   = 1;
  public static final int FT_LO   = 2;
  public static final int FT_NEG = 4;
  public static final int FT_GPREL = 8;
  public static final int FT_CALL16 = 16;
  public static final int FT_GOTDISP = 32;
  public static final int FT_GOTPAGE = 64;
  public static final int FT_GOTOFST = 128;

  public static final int FT_LAST = 128;

  public static final int FT_HI_NEG_GPREL = FT_NONE + FT_HI + FT_NEG + FT_GPREL;
  public static final int FT_LO_NEG_GPREL = FT_NONE + FT_LO + FT_NEG + FT_GPREL;
  public static final String[] ftns = {
    "",        "%hi",     "%lo",       "%neg",
    "%gp_rel", "%call16", "%got_disp", "%got_page",
    "%got_ofst"};

  private static int[] nxtMvReg = new int[5]; // Hold temporary register values.

  /**
   * Integer comparison branches.  a op 0
   */
  private static final int[] ibops = {
    Opcodes.BEQ,  Opcodes.BLEZ,  Opcodes.BLTZ,  Opcodes.BGTZ,
    Opcodes.BGEZ, Opcodes.BNE};

  private static final int[] binops = {
    Opcodes.ADDU,  Opcodes.DADDU, Opcodes.ADD_S, Opcodes.ADD_D,
    Opcodes.SUBU,  Opcodes.DSUBU, Opcodes.SUB_S, Opcodes.SUB_D,
    0,             0,             Opcodes.MUL_S, Opcodes.MUL_D,
    0,             0,             Opcodes.DIV_S, Opcodes.DIV_D,
    Opcodes.AND,   Opcodes.AND,   0,             0,
    Opcodes.OR,    Opcodes.OR,    0,             0,
    Opcodes.XOR,   Opcodes.XOR,   0,             0,
    Opcodes.SRAV,  Opcodes.DSRAV, 0,             0,
    Opcodes.SRLV,  Opcodes.DSRLV, 0,             0,
    Opcodes.SLLV,  Opcodes.DSLLV, 0,             0
  };

  private static final int[] immediateOps = {
    Opcodes.ADDIU, Opcodes.DADDIU, 0, 0,
    Opcodes.ADDIU, Opcodes.DADDIU, 0, 0,
    0,             0,              0, 0,
    0,             0,              0, 0,
    Opcodes.ANDI,  Opcodes.ANDI,   0, 0,
    Opcodes.ORI,   Opcodes.ORI,    0, 0,
    Opcodes.XORI,  Opcodes.XORI,   0, 0,
    Opcodes.SRA,   Opcodes.DSRA,   0, 0,
    Opcodes.SRL,   Opcodes.DSRL,   0, 0,
    Opcodes.SLL,   Opcodes.DSLL,   0, 0
  };

  /**
   * @param cg is the call graph to be transformed
   * @param machine specifies machine details
   * @param features controls the instructions generated
   */
  public MipsGenerator(CallGraph cg, Machine machine, int features)
  {
    super(cg, new MipsRegisterSet(), machine, features);

    if (!(machine instanceof MipsMachine))
      throw new scale.common.InternalError("Not correct machine " + machine);

    this.un = new UniqueName("$$");

    readOnlyDataArea = RDATA;
  }

  private static int logBase2(int n)
  {
    int i=1, k=1;
    while (k < n) {
      i++;
      k*=2;
    }

    return i;
  }

  private static String lookupFtn(int ftn)
  {
    return ftns[logBase2(ftn)];
  }

  public static String assembleDisp(Assembler asm, Displacement disp, int ftn)
  {
    if (ftn == MipsGenerator.FT_NONE)
      return disp.assembler(asm);

    int closeParens = 0;
    StringBuffer buf = new StringBuffer();

    for (int i=1; i <= FT_LAST; i*=2) {
      if ((ftn & i) != 0) {
        buf.append(lookupFtn(i) + "(");
        closeParens++;
      }
    }

    buf.append(disp.assembler(asm));

    for (int i=0; i < closeParens; i++)
      buf.append(')');

    return buf.toString();
  }

  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public static String displayDisp(Displacement disp, int ftn)
  {
    if (ftn == FT_NONE)
      return disp.toString();

    int closeParens = 0;
    StringBuffer buf = new StringBuffer();

    for (int i=1; i <= FT_LAST; i*=2) {
      if ((ftn & i) != 0) {
        buf.append(lookupFtn(i) + "(");
        closeParens++;
      }
    }

    buf.append(disp.toString());

    for (int i=0; i < closeParens; i++)
      buf.append(')');

    return buf.toString();
  }


  /**
   * Generate the machine instructions for a CFG.
   */
  public void generateScribble()
  {
    startLabel          = createLabel();
    callsRoutine        = false;
    usesGp              = true; // Set to true for now because otherwise it was set to true after the value had already been checked in visitReturnChord
    mask                = 0;
    fmask               = 0;
    argumentBuildSize   = 0;
    argDisp             = null;
    entryOverflowSize   = 0;
    entryOverflow       = new Vector<Displacement>(23);
    localVar            = new Vector<StackDisplacement>(23);
    localVarSize        = 0;
    structAddress       = 0;
    structSize          = 0;
    stkPtrReg           = MipsRegisterSet.SP_REG;

    super.generateScribble();
  }

  /**
   * Do peephole optimizations before registers are allocated.
   */
  //  protected void peepholeBeforeRegisterAllocation(Instruction first)
  //  {
  //      throw new scale.common.NotImplementedError("peephole");
  //  }

  /**
   * Generate assembly language file.
   * @param emit is the stream to use.
   * @param source is the source file name
   * @param comments is a list of Strings containing comments
   */
  public void assemble(Emit emit, String source, Enumeration<String> comments)
  {
    Enumeration<String> ev = comments;
    while (ev.hasMoreElements()) {
      emit.emit("\t# ");
      emit.emit(ev.nextElement());
      emit.endLine();
    }
    MipsAssembler asm = new MipsAssembler(this, source);
    asm.assemble(emit, dataAreas);
  }

  /**
   * Return the data type as an integer.
   * @param size is the size in memory units
   * @param flt is true for floating point values
   */
  public int dataType(int size, boolean flt)
  {
    int t = SpaceAllocation.DAT_NONE;
    switch (size) {
    case 1: t = SpaceAllocation.DAT_BYTE;  break;
    case 2: t = SpaceAllocation.DAT_SHORT; break;
    case 4: t = flt ? SpaceAllocation.DAT_FLT : SpaceAllocation.DAT_INT;   break;
    case 8: t = flt ? SpaceAllocation.DAT_DBL : SpaceAllocation.DAT_LONG;  break;
    default: throw new scale.common.InternalError("Can't allocate objects of size " + size);
    }
    return t;
  }

  protected void assignDeclToMemory(String name, VariableDecl vd)
  {
    Type       dt         = vd.getType();
    boolean    readOnly   = dt.isConst();
    Type       vt         = processType(dt);
    Expression init       = vd.getInitialValue();
    Visibility visibility = vd.visibility();

    int aln = (vd.isCommonBaseVar() ?
               machine.generalAlignment() :
               dt.alignment(machine));
    int ts  = 1;

    try {
      ts = vt.memorySizeAsInt(machine);
    } catch (java.lang.Error ex) {
    }

    int area = BSS;
    if (init != null)
      area = readOnly ? RDATA : DATA;

    int             handle = allocateWithData(name,
                                              vt,
                                              ts,
                                              init,
                                              area,
                                              readOnly,
                                              1,
                                              aln);
    SpaceAllocation sa     = getSpaceAllocation(handle);
    Displacement    disp   = new SymbolDisplacement(name, handle);

    sa.setDisplacement(disp);
    defineDeclInMemory(vd, disp);

    if (visibility == Visibility.EXTERN) {
      sa.setVisibility(SpaceAllocation.DAV_EXTERN);
      if (vd.isWeak()) {
        sa.setWeak(true);
        sa.setValue(vd.getAlias());
      }
    } else if (visibility == Visibility.GLOBAL) {
      sa.setVisibility(SpaceAllocation.DAV_GLOBAL);
      sa.setWeak(vd.isWeak());
    }
  }

  protected void assignDeclToRegister(VariableDecl vd)
  {
    Type dt   = vd.getType();
    Type vt   = processType(dt);
    int  regd = registers.newTempRegister(vt.getTag());

    defineDeclInRegister(vd, regd, ResultMode.NORMAL_VALUE);
  }

  protected void assignDeclToStack(VariableDecl vd)
  {
    Type              dt    = vd.getType();
    Type              vt    = processType(dt);
    int               sts   = vt.memorySizeAsInt(machine);
    StackDisplacement sdisp = new StackDisplacement(localVarSize);

    defineDeclOnStack(vd, sdisp);
    localVarSize += Machine.alignTo(sts, 8);
    localVar.addElement(sdisp); // This stack offset will be modified later.
  }

  /**
   * The RoutineDecl is assigned a tag.  The tag can be used to
   * retrieve information about the declaration.  For a RoutineDecl,
   * the information is a Displacement, the data area for the routine
   * and the label to be used for BSR calls to the routine.
   * @param rd is the declaration
   * @param topLevel is true if this declaration is defined outside of a routine
   */
  protected void processRoutineDecl(RoutineDecl rd, boolean topLevel)
  {
    Type               vt     = processType(rd);
    String             name   = rd.getName();
    int                handle = allocateTextArea(name, TEXT);
    SymbolDisplacement disp   = new SymbolDisplacement(name, handle);

    associateDispWithArea(handle, disp);
    defineRoutineInfo(rd, disp);
  }

  /**
   * Return the register used to return the function value.
   * @param regType specifies the type of value
   * @param isCall is true if the calling routine is asking
   */
  public int returnRegister(int regType, boolean isCall)
  {
    return (registers.isFloatType(regType) ?
            MipsRegisterSet.FR_REG :
            MipsRegisterSet.IR_REG);
  }

  /**
   * Return the register used as the first argument in a function call.
   * @param regType specifies the type of argument value
   */
  public final int getFirstArgRegister(int regType)
  {
    return (registers.isFloatType(regType) ?
            MipsRegisterSet.FA0_REG :
            MipsRegisterSet.IA0_REG);
  }

  protected short[] genSingleUse(int reg)
  {
    short[] uses = new short[3];
    uses[0] = (short) reg;
    uses[1] = MipsRegisterSet.SP_REG;
    uses[2] = MipsRegisterSet.GP_REG;
    return uses;
  }

  protected short[] genDoubleUse(int reg1, int reg2)
  {
    short[] uses = new short[4];
    uses[0] = (short) reg1;
    uses[1] = (short) reg2;
    uses[2] = MipsRegisterSet.SP_REG;
    uses[3] = MipsRegisterSet.GP_REG;
    return uses;
  }

  /**
   * Assign the routine's arguments to registers or the stack.
   */
  protected void layoutParameters()
  {
    ProcedureType     pt      = (ProcedureType) processType(currentRoutine);
    int               nextArg = 0;
    int               offset  = 0;
    Type              rt      = processType(pt.getReturnType());
    StackDisplacement[] argV  = new StackDisplacement[8];

    if (!(rt.isAtomicType() || rt.isVoidType()))
      nextArg++;

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);

      if (fd instanceof UnknownFormals)
        break;

      boolean volat = fd.getType().isVolatile();
      Type    vt    = processType(fd);

      if (vt.isAtomicType()) {

        // The first eight scaler arguments are in the argument registers, 
        // remaining words have already been placed on the stack by the caller.

        if (nextArg >= 8) { // Argument is on the stack.
          StackDisplacement disp = new StackDisplacement(offset);
          int               size = vt.memorySizeAsInt(machine);

          if (size < 8)
            disp.adjust(4);

          defineDeclOnStack(fd, disp);
          offset += Machine.alignTo(size, MipsRegisterSet.IREG_SIZE);
          entryOverflow.addElement(disp);
          continue;
        }

        if (usesVaStart) { // Argument must be saved on the stack.
          throw new scale.common.NotImplementedError("layoutParameters, usesVaStart");
//           int loc = 8 * MipsRegisterSet.FREG_SIZE + nextArg * MipsRegisterSet.IREG_SIZE;

//           if (vt.isRealType())
//             loc = nextArg * MipsRegisterSet.FREG_SIZE;

//        Displacement disp = new StackDisplacement(loc);

//        defineDeclOnStack(fd, disp);
//        argV[nextArg] = disp; // This stack offset will be modified later.
//        localVar.addElement(disp); // This stack offset will be modified later.
//        nextArg++;
//        continue;
        }

        if (fd.addressTaken() || volat) {
          // Argument value will be transferred to the stack.
          StackDisplacement disp = new StackDisplacement(localVarSize);
          int               size = vt.memorySizeAsInt(machine);

          defineDeclOnStack(fd, disp);
          localVarSize += Machine.alignTo(size, MipsRegisterSet.IREG_SIZE);
          localVar.addElement(disp); // This stack offset will be modified later.
          nextArg++;
          continue;
        }

        // Place arguement in register.

        int nr = registers.newTempRegister(vt.getTag());

        defineDeclInRegister(fd, nr, ResultMode.NORMAL_VALUE);
        nextArg++;
        continue;
      }

        AggregateType at = vt.returnAggregateType();
      if (at != null) {

        // The first eight words of a struct are in the argument
        // registers, remaining words have already been placed on the
        // stack by the caller.

        int               ts   = at.memorySizeAsInt(machine);
        StackDisplacement disp = new StackDisplacement(offset);
        int               inc  = ((ts + MipsRegisterSet.IREG_SIZE - 1) /
                                  MipsRegisterSet.IREG_SIZE);

        defineDeclOnStack(fd, disp);

        if (nextArg < 8) {
          int s = (8 - nextArg) * MipsRegisterSet.IREG_SIZE;
          if (s > ts)
            s = ts;
          s = ((s + MipsRegisterSet.IREG_SIZE - 1) /
               MipsRegisterSet.IREG_SIZE) * MipsRegisterSet.IREG_SIZE;
          entryOverflowSize += s;
        }

        nextArg += inc;
        offset += inc * MipsRegisterSet.IREG_SIZE;
        entryOverflow.addElement(disp);
        continue;
      }

      throw new scale.common.InternalError("Parameter type " + fd);
    }

    if (usesVaStart) { // Must save arguments on the stack.
      int argOffset = localVarSize;
      localVarSize += 8 * (MipsRegisterSet.IREG_SIZE  + MipsRegisterSet.FREG_SIZE);
      argDisp = new StackDisplacement(argOffset);
      localVar.addElement(argDisp); // This stack offset will be modified later.
      for (int i = 0; i < 8; i++) {
        StackDisplacement disp = argV[i];
        if (disp == null)
          continue;
 
        disp.adjust(argOffset);
      }
    }
  }

  private Displacement defFloatValue(double v, int size)
  {
    int section = DATA;
    int type    = SpaceAllocation.DAT_FLT;
    int aln     = 4;

    if (size > 4) {
      section = DATA;
      type = SpaceAllocation.DAT_DBL;
      aln = 8;
    }

    Displacement disp = findAreaDisp(section, type, true, size, v, aln);

    if (disp == null) {
      String name   = un.genName();
      int    handle = allocateData(name,
                                   section,
                                   type,
                                   size,
                                   true,
                                   new Double(v),
                                   1,
                                   aln);
      disp   = new SymbolDisplacement(name, handle);
      associateDispWithArea(handle, disp);
    }

    return disp;
  }

  private Displacement defLongValue(long v, int size)
  {
    int section = LIT4;
    int type    = SpaceAllocation.DAT_INT;
    int aln     = 4;

    if (size > 4) {
      section = LIT8;
      type = SpaceAllocation.DAT_LONG;
      aln = 8;
    }

    Displacement disp = findAreaDisp(section, type, true, size, v, aln);

    if (disp == null) {
      String name   = un.genName();
      int    handle = allocateData(name,
                                   section,
                                   type,
                                   size,
                                   true,
                                   new Long(v),
                                   1,
                                   aln);
      disp   = new SymbolDisplacement(name, handle);
      associateDispWithArea(handle, disp);
    }

    return disp;
  }

  /**
   * Return the displacement for a string.
   * @param v is the string
   * @param size is the length of the string
   */
  protected Displacement defStringValue(String v, int size)
  {
    int          section = LIT4;
    int          type    = SpaceAllocation.DAT_BYTE;
    int          aln     = 4;
    Displacement disp    = findAreaDisp(section, type, true, size, v, aln);

    if (disp == null) {
      String name   = un.genName();
      int    handle = allocateData(name, section, type, size, true, v, 1, aln);
      disp   = new SymbolDisplacement(name, handle);
      associateDispWithArea(handle, disp);
    }

    return disp;
  }

  /**
   * Generate instructions to move data from one register to another.
   * If one is an integer register and the other is a floating point register,
   * a memory location may be required.
   */
  protected void genRegToReg(int src, int dest)
  {
    genRegToReg(src, MipsRegisterSet.IREG_SIZE, dest, MipsRegisterSet.IREG_SIZE);
  }

  protected void genRegToReg(int src, int srcSize, int dest, int destSize)
  {
    assert (src >= 0) : "Negative source register " + src + " to " + dest;

    if (src == dest)
      return;
    if (registers.floatRegister(src)) {
      if (registers.floatRegister(dest))
        genRealToReal(src, srcSize, dest, destSize);
      else {
        if (srcSize != destSize)
          genRealToReal(src, srcSize, dest, destSize);
        appendInstruction(new FltOpInstruction(destSize > 4 ? Opcodes.DMFC1 : Opcodes.MFC1, dest, src));
      }
    } else if (registers.floatRegister(dest)) {
      appendInstruction(new FltOpInstruction(srcSize > 4 ? Opcodes.DMTC1 : Opcodes.MTC1, src, dest));
    } else
      appendInstruction(new IntOpInstruction(srcSize > 4 ? Opcodes.DADDU : Opcodes.ADDU, dest, MipsRegisterSet.ZERO_REG, src));
  }

  /**
   *  Generate an add of address registers <code>laReg</code> and <code>raReg</code>.
   */
  protected void addRegs(int laReg, int raReg, int dest)
  {
    appendInstruction(new IntOpInstruction(Opcodes.ADDU, dest, laReg, raReg));
  }

  /**
   * Load an address of a memory location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement
   * or offset of one)
   * @return the register that is set with the address
   */
  protected int loadMemoryAddress(Displacement disp)
  {
    int dest = registers.getResultRegister(RegisterSet.ADRREG);
    appendInstruction(new LoadInstruction(Opcodes.LW, dest, MipsRegisterSet.GP_REG, disp, FT_GOTDISP));
    usesGp = true;
//     appendInstruction(new LoadImmediateInstruction(Opcodes.LUI, dest, disp, FT_HI));
//     appendInstruction(new IntOpLitInstruction(Opcodes.ORI, dest, dest, disp, FT_LO));
    return dest;
  }

  /**
   * Load an address of a stack location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement
   * or offset of one)
   * @return the register that is set with the address
   */
  protected int loadStackAddress(Displacement disp)
  {
    int dest = registers.getResultRegister(RegisterSet.ADRREG);
    genLoadImmediate(disp.getDisplacement(), MipsRegisterSet.SP_REG, dest);
    return dest;
  }

  /**
   * Generate instructions to load an immediate integer value added to the 
   * value in a register into a register.
   * @param value is the value to load
   * @param base is the base register (e.g., MipsRegisterSet.ZERO_REG)
   * @param dest is the register conatining the result
   */
  protected void genLoadImmediate(long value, int base, int dest)
  {
    int val = (int) value;
    if ((val == value) || (value == (value & 0xffffffff))) {
      if ((value >= MIN_IMM16) && (value <= MAX_IMM16)) {
        if ((value == 0) && (base == dest))
          return;
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, dest, base, getDisp(val), FT_NONE));
        return;
      }

      int          vlo     = val & 0x00ffff;
      int          vhi     = val >>> 16;
      Displacement disp    = getDisp(val);
      Displacement vloDisp = getDisp(vlo);
      Displacement vhiDisp = getDisp(vhi);
      appendInstruction(new LoadImmediateInstruction(Opcodes.LUI, dest, vhiDisp, FT_NONE));
      if (vlo != 0)
        appendInstruction(new IntOpLitInstruction(Opcodes.ORI, dest, dest, vloDisp, FT_NONE));

      if (base != MipsRegisterSet.ZERO_REG)
        appendInstruction(new IntOpInstruction(Opcodes.ADDU, dest, base, dest));
      return;
    }

    // Have to load a literal.
    // Generate instructions to load an immediate integer value into a register.

    Displacement disp = defLongValue(value, 8);
    int          adr  = loadMemoryAddress(disp);
    
    appendInstruction(new LoadInstruction(Opcodes.LD, dest, adr, getDisp(0), FT_NONE));

    if (base != MipsRegisterSet.ZERO_REG) {
      if (base == dest)
        throw new scale.common.InternalError("genLoadImmediate - base == dest");
      appendInstruction(new IntOpInstruction(Opcodes.ADDU, dest, base, dest));
    }

    return;
  }

  /**
   * Generate instructions to load an immediate integer value into a register.
   * @param value is the value to load
   * @param dest is the register conatining the result
   * @return the register containing the value (usually dest but may
   * be a hardware zero register)
   */
  protected int genLoadImmediate(long value, int dest)
  {
    if (value == 0)
      return MipsRegisterSet.ZERO_REG;
    genLoadImmediate(value, MipsRegisterSet.ZERO_REG, dest);
    return dest;
  }

  /**
   * Generate instructions to load an immediate integer value into a
   * register.  The destination register type is ignored so that
   * single values can be loaded into PAIRREG registers.  Note, we
   * know that any value referenced from memory is aligned because it
   * is created by this routine.
   * @param value is the value to load
   * @param dest is the register conatining the result
   * @param destSize is the size of the value
   * @return the register containing the value (usually dest but may
   * be a hardware zero register)
   */
  protected int genLoadDblImmediate(double value, int dest, int destSize)
  {
    if (value == 0.0) {
      zeroFloatRegister(dest, destSize);
      return dest;
    }

    Displacement disp = defFloatValue(value, destSize);
    int          op;

    if (registers.floatRegister(dest))
      op = destSize > 4 ? Opcodes.LDC1 : Opcodes.LWC1;
    else
      op = destSize > 4 ? Opcodes.LD : Opcodes.LW;

    int adr = loadMemoryAddress(disp); // Should optimize --Jeff
    appendInstruction(new LoadInstruction(op, dest, adr, getDisp(0)));

    return dest;
  }

  private void genLoadImmediate(Displacement disp, int base, int dest)
  {
    // Warning, for now assuming disp always < MAX_IMM16
    // Can't check now because disp may be changed
    appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, dest, base, disp, FT_NONE));
  }

  /**
   * Add the upper bits of the value to the base.
   * resultReg contains the register conatining the result
   * @param value is the value
   * @param base is the register containing the base value
   * @return the lower 16 bits of the constant
   */
  protected long genLoadHighImmediate(long value, int base)
  {
    int val = (int) value;
    if (value != (long) val)
      throw new scale.common.InternalError("genLoadHighImmediate " + value);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = base;

    if ((val >= MIN_IMM16) && (val <= MAX_IMM16))
      return val;

    throw new scale.common.NotImplementedError("genLoadHighImmediate, val more than 16 bits");
  }

  /**
   * Allocate a location on the stack for storing a value of the specified size.
   * Put the address of the location in the register.
   * @param adrReg specifies the register to receive the address
   * @param type is the type of the value
   * @return the size of the value to be stored
   */
  protected int allocStackAddress(int adrReg, Type type)
  {
    int          ts   = type.memorySizeAsInt(machine);
    Displacement disp = new StackDisplacement(localVarSize);

    localVarSize += Machine.alignTo(ts, 8);

    if (ts < 4) // A full word store is faster.
      ts = 4;

    appendInstruction(new IntOpLitInstruction(Opcodes.ADDU, adrReg, MipsRegisterSet.SP_REG, disp));
    return ts;
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment specifies the alignment of the address
   * @param signed is true if the data is to be sign extended
   */
  private void loadFromMemory(int dest, int address, Displacement disp, int size, long alignment, boolean signed, int dftn)
  {

    int r1 = dest;
    int r2;
    int r3;
    boolean p = registers.pairRegister(dest);

    if (p)
      size = size / 2;

    switch (size) {
    case 1:
      if (signed)
        appendInstruction(new LoadInstruction(Opcodes.LB, r1, address, disp, dftn));
      else
        appendInstruction(new LoadInstruction(Opcodes.LBU, r1, address, disp, dftn));
      return;

    case 2:
      if (signed)
        appendInstruction(new LoadInstruction(Opcodes.LH, r1, address, disp, dftn));
      else
        appendInstruction(new LoadInstruction(Opcodes.LHU, r1, address, disp, dftn));
      return;

    case 4:
      if (registers.floatRegister(dest)) {
        appendInstruction(new LoadInstruction(Opcodes.LWC1, r1, address, disp, dftn));
        if (p)
          appendInstruction(new LoadInstruction(Opcodes.LWC1, r1 + 1, address, disp.offset(4), dftn));
        return;
      }

      if ((alignment % 4) == 0) {
        if (signed)
          appendInstruction(new LoadInstruction(Opcodes.LW, r1, address, disp, dftn));
        else
          appendInstruction(new LoadInstruction(Opcodes.LWU, r1, address, disp, dftn));          
        return;
      }

      throw new scale.common.NotImplementedError("loadFromMemory, unaligned");

    case 8:
      if (registers.floatRegister(dest)) {
        appendInstruction(new LoadInstruction(Opcodes.LDC1, r1, address, disp, dftn));
        if (p)
          appendInstruction(new LoadInstruction(Opcodes.LDC1, r1 + 1, address, disp.offset(8), dftn));
        return;
      }

      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LD, r1, address, disp, dftn));
        return;
      }

      throw new scale.common.NotImplementedError("loadFromMemory, unaligned");
    }
    throw new scale.common.InternalError("Unknown data type size (" + size + ")");

  }

  /**
   * Generate instructions to load data from memory at the address in
   * a register plus an offset.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset value - usually zero
   * @param size specifies the size of the data to be loaded
   * @param alignment specifies the alignment of the address
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected void loadFromMemoryWithOffset(int     dest,
                                          int     address,
                                          long    offset,
                                          int     size,
                                          long    alignment,
                                          boolean signed,
                                          boolean real)
  {
    int        srr   = resultReg;
    ResultMode srrha = resultRegMode;
    long       srrof = resultRegAddressOffset;
    int        off   = (int) genLoadHighImmediate(offset, address);

    loadFromMemory(dest, resultReg, getDisp(off), size, alignment, signed, FT_NONE);

    resultRegAddressOffset = srrof;
    resultRegMode = srrha;
    resultReg = srr;
  }

  /**
   * Generate instructions to load data from memory at the address in
   * a register plus an offset.  The offset must not be symbolic.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected void loadFromMemoryWithOffset(int          dest,
                                          int          address,
                                          Displacement offset,
                                          int          size,
                                          long         alignment,
                                          boolean      signed,
                                          boolean      real)
  {
    assert offset.isNumeric() : "Symbolic displacement " + offset;

    loadFromMemory(dest, address, offset, size, alignment, signed, FT_NONE);
  }

  /**
   * Generate instructions to load data from memory at the address
   * that is the sum of the two index register values.
   * @param dest is the destination register
   * @param index1 is the register containing the first index
   * @param index2 is the register containing the second index
   * @param size specifies the size of the data to be loaded
   * @param alignment specifies the alignment of the address
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected void loadFromMemoryDoubleIndexing(int     dest,
                                              int     index1,
                                              int     index2,
                                              int     size,
                                              long    alignment,
                                              boolean signed,
                                              boolean real)
  {
    int adr = registers.newTempRegister(RegisterSet.AIREG);
    appendInstruction(new IntOpInstruction(Opcodes.ADDU, adr, index1, index2));
    loadFromMemoryWithOffset(dest, adr, 0, size, alignment, signed, real);
  }

  private void loadFromMemory(int          dest,
                              int          address,
                              Displacement disp,
                              int          size,
                              long         alignment,
                              boolean      signed)
  {
    loadFromMemory(dest, address, disp, size, alignment, signed, FT_NONE);
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param disp specifies the offset from the address register value
   * @param dftn - RT_NONE, RT_LITBASE_USE, etc
   * @param bits specifies the size of the data to be loaded in bits -
   * must be 32 or less
   * @param bitOffset specifies the offset to the field in bits
   * @param alignment specifies the alignment of the address
   * @param signed is true if the data is to be sign extended
   */
  protected void loadBitsFromMemory(int          dest,
                                    int          address,
                                    Displacement disp,
                                    int          dftn,
                                    int          bits,
                                    int          bitOffset,
                                    int          alignment,
                                    boolean      signed)
  {
    throw new scale.common.NotImplementedError("loadBitsFromMemory");
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment specifies the alignment of the address
   * @param dftn specifies the function to apply to the displacement (e.g. %lo, %hi)
   */
  private void storeIntoMemory(int src, int address, Displacement disp, int size, long alignment, int dftn)
  {
    boolean p = registers.pairRegister(src);

    if (p)
      size = size / 2;

    switch (size) {
    case 1:
      appendInstruction(new StoreInstruction(Opcodes.SB, src, address, disp, dftn));
      return;

    case 2:
      appendInstruction(new StoreInstruction(Opcodes.SH, src, address, disp, dftn));
      return;

    case 4:
      if (registers.floatRegister(src)) {
        appendInstruction(new StoreInstruction(Opcodes.SWC1, src, address, disp, dftn));
        if (p)
          appendInstruction(new StoreInstruction(Opcodes.SWC1, src + 1, address, disp.offset(4), dftn));

        return;
      }
      if ((alignment % 4) == 0) {
        appendInstruction(new StoreInstruction(Opcodes.SW, src, address, disp, dftn));
        return;
      }

      throw new scale.common.NotImplementedError("storeIntoMemory, unaliged");
    case 8:
      if (registers.floatRegister(src)) {
        appendInstruction(new StoreInstruction(Opcodes.SDC1, src, address, disp, dftn));
        if (p)
          appendInstruction(new StoreInstruction(Opcodes.SDC1, src + 1, address, disp.offset(8), dftn));
        return;
      }

      if ((alignment % 4) == 0) {
        appendInstruction(new StoreInstruction(Opcodes.SD, src, address, disp, dftn));
        return;
      }

      throw new scale.common.NotImplementedError("storeIntoMemory, unaliged");
    }

    throw new scale.common.InternalError("Unknown data type size (" + size + ")");
  }

  private void storeIntoMemory(int src, int address, Displacement disp, int size, long alignment)
  {
    storeIntoMemory(src, address, disp, size, alignment, FT_NONE);
  }

  /**
   * Generate instructions to store data into memory at the address
   * specified by a register.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param size specifies the size of the data to be loaded
   * @param alignment specifies the alignment of the address
   * @param real is true if the data is known to be real - this argument is not used in this architecture and will be ignored
   */
  protected void storeIntoMemory(int src, int address, int size, long alignment, boolean real)
  {
    storeIntoMemory(src, address, getDisp(0), size, alignment, FT_NONE);
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param disp specifies the offset from the address register value
   * @param dftn - RT_NONE, etc
   * @param bits specifies the size of the data in bits to be loaded -
   * must be 32 or less
   * @param bitOffset specifies the offset in bits to the data
   * @param alignment specifies the alignment of the address
   */
  protected void storeBitsIntoMemory(int src, int address, Displacement disp, int dftn, int bits, int bitOffset, int alignment)
  {
    throw new scale.common.NotImplementedError("storeBitsIntoMemory");
  }

  /**
   * Generate instructions to store data into memory at the address in
   * a register plus an offset.
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment specifies the alignment of the address
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected  void storeIntoMemoryWithOffset(int src, int address, long offset, int size, long alignment, boolean real)
  {
    int        srr   = resultReg;
    ResultMode srrha = resultRegMode;
    long       srrof = resultRegAddressOffset;
    int        off   = (int) genLoadHighImmediate(offset, address);

    storeIntoMemory(src, resultReg, getDisp(off), size, alignment, FT_NONE);

    resultRegAddressOffset = srrof;
    resultRegMode = srrha;
    resultReg = srr;
  }

  /**
   * Generate instructions to store data into memory at the address in
   * a register plus an offset.  The offset must not be symbolic.
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment specifies the alignment of the address
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected  void storeIntoMemoryWithOffset(int          src,
                                            int          address,
                                            Displacement offset,
                                            int          size,
                                            long         alignment,
                                            boolean      real)
  {
    assert offset.isNumeric() : "Symbolic displacement " + offset;

    storeIntoMemory(src, resultReg, offset, size, alignment, FT_NONE);
  }

  /**
   * Generate an instruction sequence to move words from one location to another.
   * The destination offset must not be symbolic.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move.
   * @param aln is the alignment that can be assumed for both the
   * source and destination addresses
   */
  protected void moveWords(int src, long srcoff, int dest, Displacement destoff, int size, int aln)
  {
    assert destoff.isNumeric() : "Symbolic displacement " + destoff;

    moveWords(src, srcoff, dest, destoff.getDisplacement(), size, aln);
  }

  /**
   * Generate an instruction sequence to move words from one location to another.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move.
   * @param aln is the alignment that can be assumed for both the source and destination addresses
   */
  protected void moveWords(int src, long srcoff, int dest, long destoff, int size, int aln)
  {
    int ms  = MipsRegisterSet.IREG_SIZE;
    int lop = Opcodes.LD;
    int sop = Opcodes.SD;

    if (((aln & 0x7) != 0) || (size & 0x7) != 0) {
      ms  = 4;
      lop = Opcodes.LW;
      sop = Opcodes.SW;
      if (((aln & 0x3) != 0) || (size & 0x3) != 0) {
        ms  = 2;
        lop = Opcodes.LH;
        sop = Opcodes.SH;
        if (((aln & 0x1) != 0) || (size & 0x1) != 0) {
          ms  = 1;
          lop = Opcodes.LB;
          sop = Opcodes.SB;
        }
      }
    }

    if (size <= (ms * 5)) {
      int soffset = 0;
      if ((srcoff >= (MAX_IMM16 - size)) || (srcoff <= (MIN_IMM16 + size))) {
        int sr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(srcoff, src, sr);
        src = sr;
      } else
         soffset = (int) srcoff;

      int nexsr = 0;
      for (int k = 0; k < size; k += ms) {
        Displacement odisp = getDisp(soffset);
        nxtMvReg[nexsr] = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadInstruction(lop, nxtMvReg[nexsr], src, odisp));
        soffset += ms;
        nexsr++;
      }

      int doffset = 0;
      if ((destoff >= (MAX_IMM16 - size)) || (destoff <= (MIN_IMM16 + size))) {
        int dr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(destoff, dest, dr);
        dest = dr;
      } else
          doffset = (int) destoff;

      int nexdr = 0;
      for (int k = 0; k < size; k += ms) {
        Displacement odisp = getDisp(doffset);
        appendInstruction(new StoreInstruction(sop, nxtMvReg[nexdr], dest, odisp));
        doffset += ms;
        nexdr++;
      }
      return;
    }

    int               valreg  = registers.newTempRegister(RegisterSet.AIREG);
    int               testreg = registers.newTempRegister(RegisterSet.AIREG);
    int               tsrc    = registers.newTempRegister(RegisterSet.AIREG);
    int               tdest   = registers.newTempRegister(RegisterSet.AIREG);
    Label             labl    = createLabel();
    Label             labn    = createLabel();
    Displacement      odisp   = getDisp(0);
    LabelDisplacement ldisp   = new LabelDisplacement(labl);
    Displacement      ddisp   = getDisp(ms);
    MipsBranch        inst    = new CmpBranchInstruction(Opcodes.BNE, testreg, MipsRegisterSet.ZERO_REG, ldisp, 2, null, false);

    inst.addTarget(labl, 0);
    inst.addTarget(labn, 1);

    genLoadImmediate(srcoff, src, tsrc);
    genLoadImmediate(destoff, dest, tdest);
    appendInstruction(new LoadInstruction(lop, valreg, tsrc, odisp));
    appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, tsrc, tsrc, ddisp));
    genLoadImmediate(size - ms, MipsRegisterSet.ZERO_REG, testreg);
    appendLabel(labl);
    appendInstruction(new StoreInstruction(sop, valreg, tdest, odisp));
    appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, tdest, tdest, ddisp));
    appendInstruction(new LoadInstruction(lop, valreg, tsrc, odisp));
    appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, testreg, testreg, getDisp(-ms)));
    inst.setDelaySlot(new IntOpLitInstruction(Opcodes.ADDIU, tsrc, tsrc, ddisp));
    appendInstruction(inst);
    appendLabel(labn);
    appendInstruction(new StoreInstruction(sop, valreg, tdest, odisp));
  }

  /**
   * Load a register from a symbolic location in memory.
   * @param dest is the register
   * @param dsize is the size of the value in addressable memory units
   * @param isSigned is true if the value in the register is signed
   * @param isReal is true if the value in the register is a floating point value
   * @param disp specifies the location
   */
  protected void loadRegFromSymbolicLocation(int dest, int dsize, boolean isSigned,  boolean isReal, Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

    int adr = loadMemoryAddress(disp);

    loadFromMemory(dest, adr, getDisp(0), dsize, 8, isSigned);
  }

  /**
   * Generate instructions to do a binary operation on two values.
   * @param which specifies the binary operation (ADD, SUB, ...)
   * @param ct is the result type
   * @param la is the left argument
   * @param ra is the right argument
   * @param ir is the destination register
   */
  protected void doBinaryOp(int which, Type ct, Expr la, Expr ra, int ir)
  {
    boolean  flt    = ct.isRealType();
    boolean  cmplx  = flt && registers.pairRegister(ir);
    int      bs     = ct.memorySizeAsInt(machine) >> (cmplx ? 1 : 0);
    int      k      = ((flt ? 2 : 0) + ((bs > 4) ? 1 : 0));
    int      index  = (which * 4) + k;
    int      opcode = binops[index];
    int      immop  = immediateOps[index];
    boolean  flag   = false;
    long     value  = 0;

    assert (opcode != 0) : "Invalid opcode.";

    if (commutative[which] && la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
    }

    needValue(la);
    int laReg = resultReg;

    if (ra.isLiteralExpr()) {
      LiteralExpr le = (LiteralExpr) ra;
      Literal     lit = le.getLiteral();

      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        value = il.getLongValue();
        flag = true;
      } else if (lit instanceof FloatLiteral) {
        FloatLiteral il = (FloatLiteral) lit;
        double       fv = il.getDoubleValue();
        if (fv == 0.0) {
          flag = true;
          value = 0;
        }
      } else if (lit instanceof SizeofLiteral) {
        value = valueOf((SizeofLiteral) lit);
        flag = true;
      }
    }

    if (flag && (value >= 0) && (value < 256)) {
      if ((value == 0) && ((which == ADD) || (which == SUB) || (which == OR))) {
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = laReg;
        return;
      }
      if (flt) {
        ;
      } else if (immop == Opcodes.SLL) {
        genLeftShift(laReg, value, ir, bs > 4);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      } else {
        appendInstruction(new IntOpLitInstruction(immop, ir, laReg, getDisp(which == SUB ? -(int)value : (int)value), FT_NONE));
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    needValue(ra);
    int raReg = resultReg;

    if ((raReg == MipsRegisterSet.ZERO_REG) &&
        ((which == ADD) || (which == SUB))) {
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = laReg;
      return;
    }
      
    if (cmplx) {
      doComplexOp(which, bs, laReg, raReg, ir);
    } else if (flt) {
      appendInstruction(new FltOpInstruction(opcode, ir, laReg, raReg));
    } else {
      appendInstruction(new IntOpInstruction(opcode, ir, laReg, raReg));
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }


  private void doComplexOp(int which, int bs, int laReg, int raReg, int dest) {
    throw new scale.common.NotImplementedError("doComplexOp");
  }

  private void genLeftShift(int src, long value, int dest, boolean Long)
  {
    appendInstruction(new IntOpLitInstruction(Long ? Opcodes.DSLL : Opcodes.SLL, dest, src, getDisp((int)value)));
  }


  /**
   * Generate instructions to do a comparison of two value.
   * @param c is the compare expression
   * @param which specifies the compare (EQ, NE, ...)
   */
  protected void doCompareOp(BinaryExpr c, CompareMode which)
  {
    int  ir = registers.getResultRegister(processType(c).getTag());
    Expr la = c.getOperand(0);
    Expr ra = c.getOperand(1);

    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
      which = which.argswap();
    }

    Type    rt   = processType(la);
    boolean si   = rt.isSigned();
    int     size = rt.memorySizeAsInt(machine);
    needValue(la);
    int laReg = resultReg;

    if (!rt.isRealType()) { // Integer
      if (ra.isLiteralExpr()) {
        Literal lit   = ((LiteralExpr) ra).getLiteral();
        long    value = -1;

        if (lit instanceof IntLiteral)
          value = ((IntLiteral) lit).getLongValue();
        else if (lit instanceof SizeofLiteral)
          value = valueOf((SizeofLiteral) lit);

        if ((value > MIN_IMM16) && (value <= MAX_IMM16)) {
          switch (which) {
            // case EQ:
            // appendInstruction(new IntOpLitInstruction(size > 4 ? Opcodes.DADDIU : Opcodes.ADDIU, laReg, (int) -value, ir));
            // resultRegAddressOffset = 0;
            // resultRegMode = ResultMode.NORMAL_VALUE;
            // resultReg = ir;
            // return;
          case GE:
            if (si)
              appendInstruction(new IntOpLitInstruction(Opcodes.SLTI, ir, laReg, getDisp((int)value)));
            else
              appendInstruction(new IntOpLitInstruction(Opcodes.SLTIU, ir, laReg, getDisp((int)value)));
            
            appendInstruction(new IntOpLitInstruction(Opcodes.XORI, ir, ir, getDisp(1)));
            resultRegAddressOffset = 0;
            resultRegMode = ResultMode.NORMAL_VALUE;
            resultReg = ir;
            return;
          case LT:
            if (si)
              appendInstruction(new IntOpLitInstruction(Opcodes.SLTI, ir, laReg, getDisp((int)value)));
            else
              appendInstruction(new IntOpLitInstruction(Opcodes.SLTIU, ir, laReg, getDisp((int)value)));
            resultRegAddressOffset = 0;
            resultRegMode = ResultMode.NORMAL_VALUE;
            resultReg = ir;
            return;

            //case NE:
            //if (value == 0) {
            //  appendInstruction(new IntOpInstruction(Opcodes.CMPULT, AlphaRegisterSet.I0_REG, laReg, ir));
            //  resultRegAddressOffset = 0;
            // resultRegMode = ResultMode.NORMAL_VALUE;
            // resultReg = ir;
            //  return;
            //}
            //appendInstruction(new IntOpLitInstruction(Opcodes.CMPEQ, laReg, (int) value, ir));
            //appendInstruction(new IntOpLitInstruction(Opcodes.XOR, ir, 1, ir));
            //resultRegAddressOffset = 0;
            // resultRegMode = ResultMode.NORMAL_VALUE;
            // resultReg = ir;
            //return;
          }
        }
      }

      needValue(ra);
      int raReg = resultReg;

      switch (which) {
      case EQ:
        appendInstruction(new IntOpInstruction(Opcodes.XOR, ir, laReg, raReg));
        appendInstruction(new IntOpLitInstruction(Opcodes.SLTIU, ir, ir, getDisp(1)));
        break;
      case LE:
        appendInstruction(new IntOpInstruction(si ? Opcodes.SLT : Opcodes.SLTU, ir, raReg, laReg));
        appendInstruction(new IntOpLitInstruction(Opcodes.XORI, ir, ir, getDisp(1)));
        break;
      case LT:
        appendInstruction(new IntOpInstruction(si ? Opcodes.SLT : Opcodes.SLTU, ir, laReg, raReg));
        break;
      case GT:
        appendInstruction(new IntOpInstruction(si ? Opcodes.SLT : Opcodes.SLTU, ir, raReg, laReg));
        break;
      case GE:
        appendInstruction(new IntOpInstruction(si ? Opcodes.SLT : Opcodes.SLTU, ir, laReg, raReg));
        appendInstruction(new IntOpLitInstruction(Opcodes.XORI, ir, ir, getDisp(1)));
        break;
      case NE:
        appendInstruction(new IntOpInstruction(Opcodes.XOR, ir, laReg, raReg));
        appendInstruction(new IntOpInstruction(Opcodes.SLTU, ir, MipsRegisterSet.ZERO_REG, ir));
        break;
      default:
        throw new scale.common.InternalError("Invalid which " + which);
      }

      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    // Floating point

    needValue(ra);

    int     op          = Opcodes.lookupFltCompare(size, which.reverse().ordinal());
    boolean swappedArgs = Opcodes.lookupFltCompareOrder(size, which.reverse().ordinal());

    if (swappedArgs)
      appendInstruction(new FltCmpInstruction(op, resultReg, laReg));
    else
      appendInstruction(new FltCmpInstruction(op, laReg, resultReg));
    appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, ir, MipsRegisterSet.ZERO_REG, getDisp(1)));

    int opm = Opcodes.lookupFltMovGP(size, which.reverse().ordinal());
    appendInstruction(new CondMovInstruction(opm, ir, MipsRegisterSet.ZERO_REG, MipsRegisterSet.FCC0));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }


  /**
   * Called at the start of code generation for a routine.
   */
  protected Instruction startRoutineCode()
  {
    currentBeginMarker  = new BeginMarker(scribble);

    return currentBeginMarker;
  }

  protected void processSourceLine(int line, Label lab, boolean newLine)
  {
    if (line < 0)
      return;

    String fileName = currentRoutine.getCallGraph().getName();
    appendInstruction(new MipsLineMarker(fileName, line));
  }

  //      protected void appendInstruction(Instruction inst)
  //      {
  //        if ((inst instanceof IntOpInstruction) && (inst.getOpcode() == Opcodes.SUBL)) {
  //          System.out.println(inst);
  //          Debug.printStackTrace();
  //        }
  //        super.appendInstruction(inst);
  //      }

  /**
   * Generate an unconditional branch to the label specified.
   */
  public void generateUnconditionalBranch(Label lab)
  {
    LabelDisplacement disp = new LabelDisplacement(lab);
    Branch            inst = new JumpLabelInstruction(Opcodes.J, disp, 1, null, false);

    inst.addTarget(lab, 0);
    appendInstruction(inst);
  }

  /**
   * Obtain the information needed for register spill loads and stores.
   * The Object returned will probably specify a memory location.
   * It will be passed to getSpillLoad() and getSpillStore().
   * @param reg specifies which virtual register will be spilled
   */
  public Object getSpillLocation(int reg)
  {
    StackDisplacement disp = new StackDisplacement(localVarSize);
    localVarSize += Machine.alignTo(registers.registerSize(reg), 8);
    localVar.addElement(disp);
    return disp;
  }

  /**
   * Insert the instruction(s) to restore a spilled register.
   * At this point, we are using a one-to-one mapping between real registers
   * and virtual registers, so only a single instruction is required.
   * @param reg specifies which virtual register will be loaded
   * @param spillLocation specifies the offset on the stack to the spill location
   * @param after specifies the instruction to insert the load after
   * @return the last instruction inserted
   * @see #getSpillLocation
   */
  public Instruction insertSpillLoad(int reg, Object spillLocation, Instruction after)
  {
    boolean flt = registers.floatRegister(reg);
    int     opl = flt ? Opcodes.LDC1 : Opcodes.LD;

    Instruction load = new LoadInstruction(opl, reg, MipsRegisterSet.SP_REG, (Displacement) spillLocation);
    insertInstruction(load, after);

    return load;
  }

  /**
   * Insert the instruction(s) to save a spilled register.
   * At this point, we are using a one-to-one mapping between real registers
   * and virtual registers, so only a single instruction is required.
   * @param reg specifies which virtual register will be stored
   * @param spillLocation specifies the offset on the stack to the spill location
   * @param after specifies the instruction to insert the store after
   * @return the last instruction inserted
   * @see #getSpillLocation
   */
  public Instruction insertSpillStore(int reg, Object spillLocation, Instruction after)
  {
    boolean flt = registers.floatRegister(reg);
    int     ops = flt ? Opcodes.SDC1 : Opcodes.SD;

    Instruction store = new StoreInstruction(ops, reg, MipsRegisterSet.SP_REG, (Displacement) spillLocation);
    insertInstruction(store, after);

    return store;
  }



  /**
   * Specify that this routine saved a register on the stack.
   */
  private void setRegisterSaved(int reg)
  {
    if (reg <= MipsRegisterSet.LAST_INT_REG)
      mask |= 1 << reg;
    else
      fmask |= 1 << reg;
    mask |= 1 << MipsRegisterSet.RA_REG;
  }

  /**
   * Called at the end of code generation for a routine.
   * On the Mips we need to pass some critical information to the assembler.
   */
  protected void endRoutineCode(int[] regMap)
  {
    Instruction first    = currentBeginMarker;
    Instruction last     = returnInst;
    int         nrr      = registers.numRealRegisters();
    boolean[]     newmap = new boolean[nrr];
    int         n        = regMap.length;

    if ((last == null) && Debug.debug(1))
      System.out.println("** Warning: " + currentRoutine.getName() + "() does not return.");

    for (int i = nrr; i < n; i++) {
      int vr = regMap[i];
      if (vr < nrr) /* In case of debugging. */
        newmap[vr] = true;
    }

    Instruction st = first;
    argumentBuildSize = (int) Machine.alignTo(argumentBuildSize, 16);
    int srOffset = argumentBuildSize;  // Offset from SP to saved registers.

    if (usesGp) {
      Displacement disp = getDisp(srOffset);
      first = insertInstruction(new StoreInstruction(Opcodes.SW, MipsRegisterSet.GP_REG, MipsRegisterSet.SP_REG, disp), first);
      if (last !=  null)
        last = insertInstruction(new LoadInstruction(Opcodes.LW, MipsRegisterSet.GP_REG, MipsRegisterSet.SP_REG, disp), last);
      setRegisterSaved(MipsRegisterSet.GP_REG);
      srOffset += MipsRegisterSet.IREG_SIZE;
    }

    if (callsRoutine) {
      Displacement disp = getDisp(srOffset);
      first = insertInstruction(new StoreInstruction(Opcodes.SW, MipsRegisterSet.RA_REG, MipsRegisterSet.SP_REG, disp), first);
      if (last != null)
        last = insertInstruction(new LoadInstruction(Opcodes.LW, MipsRegisterSet.RA_REG, MipsRegisterSet.SP_REG, disp), last);
      setRegisterSaved(MipsRegisterSet.RA_REG);
      srOffset += MipsRegisterSet.IREG_SIZE;
    }

    short[] calleeSaves = registers.getCalleeSaves();
    for (int i = 0; i < calleeSaves.length; i++) { // Determine which registers need saving.
      int reg = calleeSaves[i];

      if (!newmap[reg])
        continue;

      Displacement disp = getDisp(srOffset);
      if (!registers.floatRegister(reg)) {
        first = insertInstruction(new StoreInstruction(Opcodes.SD, reg, MipsRegisterSet.SP_REG, disp), first);
        if (last != null)
          last = insertInstruction(new LoadInstruction(Opcodes.LD, reg, MipsRegisterSet.SP_REG, disp), last);
        setRegisterSaved(reg);
        srOffset += MipsRegisterSet.IREG_SIZE;
      } else {
        first = insertInstruction(new StoreInstruction(Opcodes.SDC1, reg, MipsRegisterSet.SP_REG, disp), first);
        if (last != null)
          last = insertInstruction(new LoadInstruction(Opcodes.LDC1, reg, MipsRegisterSet.SP_REG, disp), last);
        setRegisterSaved(reg);
        srOffset += MipsRegisterSet.FREG_SIZE;
      }
    }

    if (argDisp != null) { // Used for variable parameters
      throw new scale.common.NotImplementedError("endRoutineCode: argDisp != null");
    }

    int frameSize = ((localVarSize + srOffset +
                      (entryOverflowSize + 15)) / 16) * 16; // Frame size must be a multiple of 16.
    int lvOffset  = frameSize - localVarSize; // Offset from SP to local variables.

    if (trace) {
      System.out.print("LVS: fs ");
      System.out.print(frameSize);
      System.out.print(" lvo ");
      System.out.print(lvOffset);
      System.out.print(" sro ");
      System.out.print(srOffset);
      System.out.print(" lvs ");
      System.out.print(localVarSize);
      System.out.print(" abs ");
      System.out.print(argumentBuildSize);
      System.out.print(" eos ");
      System.out.print(entryOverflowSize);
      System.out.print(" fo ");
      System.out.println(frameSize - argumentBuildSize);
    }

    if (frameSize > 0) {
      int fs = frameSize;
      while (fs > 0) {
        int          inc    = (fs > 0x6ff0) ? 0x6ff0 : fs;
        Displacement fdisp  = getDisp(inc);
        Displacement fdispm = getDisp(-inc);
        insertInstruction(new IntOpLitInstruction(Opcodes.ADDIU,
                                                  MipsRegisterSet.SP_REG,
                                                  MipsRegisterSet.SP_REG,
                                                  fdispm),
                          st);
        if (last != null)
          last = insertInstruction(new IntOpLitInstruction(Opcodes.ADDIU,
                                                           MipsRegisterSet.SP_REG,
                                                           MipsRegisterSet.SP_REG,
                                                           fdisp),
                                   last);
        fs -= inc;
      }
    }

    first = insertInstruction(new PrologMarker(mask, fmask, frameSize, frameSize - argumentBuildSize), first);

    appendInstruction(new EndMarker(currentRoutine));

    // Adjust stack offsets now that we know how big everything is.

    int lsv = localVar.size();
    for (int i = 0; i < lsv; i++) {
      StackDisplacement disp = localVar.elementAt(i);
      disp.adjust(srOffset); //disp.adjust(lvOffset);
    }
    int leo = entryOverflow.size();
    for (int i = 0; i < leo; i++) {
      StackDisplacement disp = (StackDisplacement) entryOverflow.elementAt(i);
      disp.adjust(frameSize - entryOverflowSize);
    }
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    Expr op  = e.getOperand(0);
    int  reg = registers.getResultRegister(processType(e).getTag());
    Type t   = e.getType();
    int  bs  = t.memorySizeAsInt(machine);

    needValue(op);

    int raReg = resultReg;

    if (registers.pairRegister(raReg)) {
      throw new scale.common.NotImplementedError("visitAbsolutionValueExpr, pairRegister");
    } else if (registers.floatRegister(raReg)) {
      appendInstruction(new FltOpInstruction(bs > 4 ? Opcodes.ABS_D : Opcodes.ABS_S, reg, raReg));
    } else {
      int ir = registers.newTempRegister(RegisterSet.INTREG); // raReg & reg could be the same register.
      appendInstruction(new IntOpLitInstruction(bs > 4 ? Opcodes.DSRA32 : Opcodes.SRA, ir, raReg, getDisp(31)));
      appendInstruction(new IntOpInstruction(Opcodes.XOR, reg, ir, raReg));
      appendInstruction(new IntOpInstruction(bs > 4 ? Opcodes.DSUBU : Opcodes.SUBU, reg, reg, ir));
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = reg;
  }

  /**
   * This method is responsible for generating instructions to move
   * function arguments to the position assigned by the {@link
   * scale.backend.Generator#layoutParameters layoutParameters} method.
   */
  public void generateProlog(ProcedureType pt)
  {
    int  nextArg = 0;
    Type rt      = processType(pt.getReturnType());

    if (currentRoutine.visibility() == Visibility.GLOBAL)
      usesGp = true;

    // Lay out the arguments in memory.

    if (!(rt.isAtomicType() || rt.isVoidType())) {
      if (rt.memorySizeAsInt(machine) <= (MipsRegisterSet.IREG_SIZE * 2))
        ;
      else {
        structAddress = registers.newTempRegister(RegisterSet.AIREG);
        structSize = rt.memorySizeAsInt(machine);
        genRegToReg(MipsRegisterSet.IA0_REG, 4, structAddress, 4); // Will need to be changed for 64-bit addresses
        nextArg++;
      }
    }
 
    int l = pt.numFormals();
    for (int ii = 0; ii < l; ii++) {
      FormalDecl fd = pt.getFormal(ii);
      Type       vt = processType(fd);

      if (fd instanceof UnknownFormals)
        break;

      int      ts  = vt.memorySizeAsInt(machine);     
      Assigned loc = fd.getStorageLoc();

      if (loc == Assigned.IN_REGISTER) {
        int reg = fd.getValueRegister();
        if (vt.isRealType())
          genRegToReg(MipsRegisterSet.FA0_REG + nextArg, ts, reg, ts);
        else {
          int treg = convertIntRegValue(MipsRegisterSet.IA0_REG + nextArg,
                                        MipsRegisterSet.IREG_SIZE, vt.isSigned(),
                                        reg,
                                        ts,
                                        vt.isSigned());
          genRegToReg(treg, reg);
        }
        nextArg++;
        continue;
      }

      if (loc == Assigned.ON_STACK) {
        Displacement disp = fd.getDisplacement();

        if (vt.isAtomicType()) {
          if (nextArg < 8) {
            if (usesVaStart)
              continue;

            if (vt.isRealType())
              storeIntoMemory(MipsRegisterSet.FA0_REG + nextArg,
                              MipsRegisterSet.SP_REG,
                              disp,
                              vt.memorySizeAsInt(machine),
                              4);
            else
              storeIntoMemory(MipsRegisterSet.IA0_REG + nextArg,
                              MipsRegisterSet.SP_REG,
                              disp,
                              vt.memorySizeAsInt(machine),
                              4);
            nextArg++;
          }
          continue;
        }

        AggregateType agt = vt.returnAggregateType();
        if (agt != null) {
          int       dsize   = vt.memorySizeAsInt(machine);
          int       offset  = 0;
          boolean[] fltReg  = getStructFloatRegs(agt); // An array of whether each chunk of a struct is stored in a floating point register 

          for (int i = 0; i < dsize; i += MipsRegisterSet.IREG_SIZE) {
            if (nextArg < 8) {
              Displacement disp2 = disp.offset(offset);
              entryOverflow.addElement(disp2);
              MipsInstruction inst;
              if (fltReg[i/ MipsRegisterSet.IREG_SIZE])
                inst = new StoreInstruction(Opcodes.SDC1, MipsRegisterSet.FA0_REG + nextArg,
                                            MipsRegisterSet.SP_REG,
                                            disp2);
              else
                inst = new StoreInstruction(Opcodes.SD, MipsRegisterSet.IA0_REG + nextArg,
                                            MipsRegisterSet.SP_REG,
                                            disp2);
              appendInstruction(inst);
              offset += MipsRegisterSet.IREG_SIZE;
              nextArg++;
            }
          }
          continue;
        }
      }
      throw new scale.common.InternalError("Argument is where " + fd);
    }

    lastInstruction.specifySpillStorePoint();
    lastLabel = createLabel();
    appendLabel(lastLabel);
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    Expr arg = e.getOperand(0);
    int  ir  = registers.getResultRegister(RegisterSet.INTREG);

    needValue(arg);
    int src = resultReg;

    if (registers.floatRegister(src))
      throw new scale.common.InternalError("Bit complement not allowed on " + arg);

    appendInstruction(new IntOpInstruction(Opcodes.NOR, ir, src, MipsRegisterSet.ZERO_REG));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private int getFirstUnknownFormal (Expr ftn)
  {
    if (ftn instanceof LoadDeclAddressExpr) {
      LoadDeclAddressExpr ldae  = (LoadDeclAddressExpr)ftn;
      Declaration         fdecl = ldae.getDecl();
      if (fdecl instanceof ProcedureDecl) {
        ProcedureDecl pd = (ProcedureDecl)fdecl;
        ProcedureType pt = pd.getSignature();
        int l = pt.numFormals();
        for (int i = 0; i < l; i++) {
          if (pt.getFormal(i) instanceof UnknownFormals)
            return i;
        }
      }
    }
    return -1;
  }

  /**
   * Move a store of a register onto the stack to just after the register is defined.
   * This helps out the register allocater (see subroutine PSET in benchmark apsi.f).
   */
  private void moveStackStore(int reg, Instruction prior, Instruction last)
  {
    Instruction start = lastLabel.getNext();
    Instruction after = null;

    while (start != null) {
      if (start.defs(reg, registers))
        after = start;
      start = start.getNext();
    }
    if (after != null)
      moveInstructionSequence(prior, last, after);
  }

  private boolean[] getStructFloatRegs(AggregateType at)
  {
    boolean[] toReturn = {false, false, false, false, false, false, false, false};
    int     l          = at.numFields();
    for (int i = 0; i < l; i++) {
      FieldDecl fd = at.getField(i);
      Type      ft = processType(fd);
      if (ft.isRealType() && ft.memorySizeAsInt(machine) == MipsRegisterSet.FREG_SIZE) {
        int fieldOffset = (int) fd.getFieldOffset();
        int regNum      = fieldOffset / MipsRegisterSet.IREG_SIZE;
        if (regNum < 8)
          toReturn[regNum] = true;
      }
    }
    return toReturn;
  }

  public boolean structReturnsInFPRegs (Type type)
  {
    if (type.isAtomicType())
      return false;

    AggregateType at = (AggregateType) type;
    if (at.memorySize(machine) > (2 * MipsRegisterSet.IREG_SIZE))
      return false;

    int l = at.numFields();
    if (l > 2)
      l = 2;
    int i;
    for (i = 0; i < l; i++) {
      FieldDecl fd = at.getField(i);
      Type      ft = processType(fd);
      if (!ft.isRealType())
        return false;
    }

    if (i == 0)
      return false;

    return true;
  }

  public int[] fieldFloatSizes (Type type)
  {
    int[] toReturn = { 0, 0 };
    if (!structReturnsInFPRegs(type))
      return toReturn;
    AggregateType at = (AggregateType) type;
    int l = at.numFields();
    if (l > 2)
      l = 2;
    
    for (int i = 0; i < l; i++) {
      FieldDecl fd = at.getField(i);
      Type      ft = processType(fd);
      toReturn[i] = ft.memorySizeAsInt(machine);
    }

    return toReturn;
  }

  private int[]   amap     = new int[8];
  private short[] umap     = new short[8];
  private int[]   sizemap  = new int[8];

  /**
   * Load the arguments into registers for a routine call.  Only the
   * first MAX_ARG_REGS words of arguements are placed into registers.
   * The remaining words are placed on the stack.
   * There can be a hidden first argument for large returns.
   * @param args is the set of arguments
   * @param retStruct is true if the routine returns a struct
   * @return the registers used by the call
   */
  protected short[] callArgs(Expr[] args, boolean retStruct)
  {
    boolean simple = true; // True if only simple arguments;
    for (int i = 0; i < args.length; i++) {
      Expr arg = args[i];

      if (isSimple(arg))
        continue;

      simple = false;
      break;
    }

    int rk      = 0;
    int nextArg = 0;
    int offset  = 0;

    if (retStruct) {
      amap[rk] = MipsRegisterSet.IA0_REG;
      umap[rk] = (byte) MipsRegisterSet.IA0_REG;
      rk++;
      nextArg++;
    }

    // Load the arguments into registers.  Only the first eight words of 
    // arguements are placed into registers.  The remaining words are 
    // placed on the stack.

    for (int i = 0; i < args.length; i++) {
      Expr arg  = args[i];
      Type vt   = processType(arg);
      int  size = vt.memorySizeAsInt(machine);
      int  nexr;

      if (trace)
        System.out.println("CFA: " + arg);

      if (vt.isAtomicType() && !vt.isComplexType()) {
        if (nextArg >= 8) {     
          needValue(arg);

          IntegerDisplacement odisp = getDisp(offset);
          Instruction         prior = lastInstruction;

          //      if (size < 8)  // To get stack argument passing to match the compiler --Why is this necessary?
          //        odisp.adjust(4);

          Displacement disp = size < 8 ? getDisp((int) odisp.getDisplacement() + 4): odisp;
          storeIntoMemory(resultReg, MipsRegisterSet.SP_REG, disp, size, 4);
          moveStackStore(resultReg, prior, lastInstruction); // Help out register allocater.
          offset += Machine.alignTo(size, MipsRegisterSet.IREG_SIZE);
        } else {
          sizemap[rk] = vt.memorySizeAsInt(machine);

          nexr = MipsRegisterSet.IA0_REG + nextArg;

          if (simple)
            registers.setResultRegister(nexr);

          needValue(arg);

          if (simple)
            registers.setResultRegister(-1);

          if (resultRegMode == ResultMode.ADDRESS) // Address of string??
            resultRegAddressOffset = 0;

          // Postpone transfer because of things like divide which may cause a subroutine call.

          amap[rk] = resultReg;
          umap[rk] = (byte) nexr;
          rk++;
          nextArg++;
        }

        continue;
      }

      AggregateType at = vt.returnAggregateType();
      if (at != null) {
        int       address = registers.newTempRegister(RegisterSet.AIREG);
        boolean[] fltReg  = getStructFloatRegs(at); // An array of whether each chunk of a struct is stored in a floating point register 

        needValue(arg);
        address = resultReg;

        assert (resultRegMode == ResultMode.ADDRESS) : "Huh " + arg;

        int offset2 = 0;

        for (int k = 0; k < size; k += MipsRegisterSet.IREG_SIZE) {
          Displacement odisp2 = getDisp(offset2);
          if (nextArg < 8) {
            if (fltReg[k / MipsRegisterSet.IREG_SIZE]) {
              nexr = MipsRegisterSet.FA0_REG + nextArg;
              resultRegAddressOffset = 0;
              resultRegMode = ResultMode.NORMAL_VALUE;
              resultReg = simple ? nexr : registers.newTempRegister(RegisterSet.FLTREG); // Might need to be changed for MIPS -Jeff
            } else {
              nexr = MipsRegisterSet.IA0_REG + nextArg;
              resultRegAddressOffset = 0;
              resultRegMode = ResultMode.NORMAL_VALUE;
              resultReg = simple ? nexr : registers.newTempRegister(RegisterSet.INTREG);
            }

            loadFromMemory(resultReg, address, odisp2, MipsRegisterSet.IREG_SIZE, 4, false);
            amap[rk] = resultReg;
            umap[rk] = (byte) nexr;
            rk++;
            nextArg++;
          } else {
            Displacement odisp = getDisp(offset);
            nexr = registers.newTempRegister(RegisterSet.AIREG);
            loadFromMemory(nexr, address, odisp2, MipsRegisterSet.IREG_SIZE, 4, false);
            appendInstruction(new StoreInstruction(Opcodes.SD, nexr, MipsRegisterSet.SP_REG, odisp));

            offset += MipsRegisterSet.IREG_SIZE;
          }
          offset2 += MipsRegisterSet.IREG_SIZE;
        }

        continue;
      }

      if (vt.isArrayType()) {
        LoadDeclValueExpr ldve = (LoadDeclValueExpr) arg;
        VariableDecl      ad   = (VariableDecl) ldve.getDecl();

        nexr = MipsRegisterSet.IA0_REG + nextArg;

        if (simple)
          registers.setResultRegister(nexr);

        putAddressInRegister(ad, false);

        if (simple)
          registers.setResultRegister(-1);

        amap[rk] = resultReg;
        umap[rk] = (byte) nexr;
        rk++;
        nextArg++;
        continue;
      }

      if (vt.isComplexType()) { // Treat complex as structs for intrinsic routines.
        throw new scale.common.NotImplementedError("visitCallFunctionExpr, vt.isComplexType");
      } else {
        throw new scale.common.InternalError("Argument type " + arg);
      }
    }

    for (int i = 0; i < rk; i++) {
      genRegToReg(amap[i], sizemap[i], umap[i], sizemap[i]); // Need to fix with data type sizes -Jeff
    }

    if (offset > argumentBuildSize)
      argumentBuildSize = offset;

    short[] uses = new short[rk + 2];
    System.arraycopy(umap, 0, uses, 0, rk);
    uses[rk + 0] = MipsRegisterSet.SP_REG;
    uses[rk + 1] = MipsRegisterSet.GP_REG;
    return uses;
  }

  /**
   * This method is responsible for generating instructions to move
   * function arguments to the position assigned by the {@link
   * scale.backend.Generator#layoutParameters layoutParameters} method.
   */
  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    Type         rt                 = processType(e);
    int          rts                = 0;
    Expr[]       args               = e.getArgumentArray();
    Displacement d8                 = getDisp(MipsRegisterSet.IREG_SIZE);
    StackDisplacement returnDisp    = null;          /* For functions that return structure values. */
    Expr         ftn                = e.getFunction();
    Declaration  decl               = null;
    Branch       jsr;
    int          ir                 = -1;

    int          unknownFormals     = getFirstUnknownFormal(ftn);
    boolean      usesUnknownFormals = (unknownFormals != -1);
    boolean      retStruct  = false;

    callsRoutine = true;
    usesGp = true;

    if (ftn instanceof LoadDeclAddressExpr) {
      decl = ((LoadExpr) ftn).getDecl();
      String fname = decl.getName();

      if (args.length == 1) {
        if (fname.equals("_scale_setjmp")) {
          RoutineDecl        rd = (RoutineDecl) decl;
          int                handle = ((SymbolDisplacement) rd.getDisplacement()).getHandle();
          SymbolDisplacement symDisp = new SymbolDisplacement("setjmp", handle);
          rd.setDisplacement(symDisp);
        } else if (ansic && fname.equals("alloca")) {
          genAlloca(args[0], registers.getResultRegister(rt.getTag()));
          return;
        }
      } else if (args.length == 2) {
        if (fname.equals("_scale_longjmp")) {
          RoutineDecl        rd = (RoutineDecl) decl;
          int                handle = ((SymbolDisplacement) rd.getDisplacement()).getHandle();
          SymbolDisplacement symDisp = new SymbolDisplacement("longjmp", handle);
          rd.setDisplacement(symDisp);
        }
      }
    }

    if (!rt.isVoidType()) {
      if (!rt.isAtomicType()) {
        if (rt.memorySizeAsInt(machine) <= (2 * MipsRegisterSet.IREG_SIZE)) {
          ;
        } else {

          // If the routine returns a structure, it is passed through
          // memory and the memory address to use is the first
          // argument.

          returnDisp = new StackDisplacement(localVarSize);
          appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU,
                                                    MipsRegisterSet.IA0_REG,
                                                    MipsRegisterSet.SP_REG,
                                                    returnDisp));

          localVarSize += Machine.alignTo(rt.memorySizeAsInt(machine), 8);
          localVar.addElement(returnDisp);
          retStruct = false;
        }
      } else
        ir = registers.getResultRegister(rt.getTag());
    }

    short[] uses = callArgs(args, retStruct);

    if (decl == null) { // Indirection
      needValue(ftn);

      genRegToReg(resultReg, 4, MipsRegisterSet.T9_REG, 4);
      
      jsr = new JumpRegInstruction(Opcodes.JALR, MipsRegisterSet.T9_REG, 1, null, true);
    } else {
      RoutineDecl rd = (RoutineDecl) decl;

      addrDisp = rd.getDisplacement().unique();
      usesGp = true;

      if (decl.visibility() == Visibility.EXTERN) {
        appendInstruction(new LoadInstruction(Opcodes.LW,
                                              MipsRegisterSet.T9_REG,
                                              MipsRegisterSet.GP_REG,
                                              addrDisp,
                                              FT_CALL16));
        jsr = new JumpRegInstruction(Opcodes.JALR, MipsRegisterSet.T9_REG, 1, null, true);
      } else {
        appendInstruction(new LoadInstruction(Opcodes.LW,
                                              MipsRegisterSet.T9_REG,
                                              MipsRegisterSet.GP_REG,
                                              addrDisp,
                                              FT_GOTPAGE));
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU,
                                                  MipsRegisterSet.T9_REG,
                                                  MipsRegisterSet.T9_REG,
                                                  addrDisp,
                                                  FT_GOTOFST));
        jsr = new JumpRegInstruction(Opcodes.JALR, MipsRegisterSet.T9_REG, 1, null, true);
      }
    }

    appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), null, true);

    if (rt.isVoidType()) {
      appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), null, true);
      return;
    }

    if (!rt.isAtomicType()) { // Returning structs is a pain.
      int vr = registers.newTempRegister(RegisterSet.AIREG);
      if (rt.memorySizeAsInt(machine) > (2 * MipsRegisterSet.IREG_SIZE)) {
        appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), null, true);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, vr, MipsRegisterSet.SP_REG, returnDisp));
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.ADDRESS;
        resultReg = vr;
        resultRegAddressAlignment = 8;
        return;
      }

      boolean srfr = structReturnsInFPRegs(rt);
      appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), srfr ? realReturn : intReturn, true);

      returnDisp = new StackDisplacement(localVarSize);
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDIU, vr, MipsRegisterSet.SP_REG, returnDisp));
            
      localVarSize += Machine.alignTo(rt.memorySizeAsInt(machine), 8);
      localVar.addElement(returnDisp);

      if (srfr) {
        int[] floatSizes = fieldFloatSizes(rt);
        int   op0        = floatSizes[0] > 4 ? Opcodes.SDC1 : Opcodes.SWC1;
        appendInstruction(new StoreInstruction(op0, MipsRegisterSet.FR_REG, vr, getDisp(0)));
        if (floatSizes[1] > 0) {
          int          op1  = floatSizes[1] > 4 ? Opcodes.SDC1 : Opcodes.SWC1;
          Displacement disp = getDisp(rt.memorySizeAsInt(machine) == 8 ? 8 : 4);
          appendInstruction(new StoreInstruction(op1, MipsRegisterSet.FR2_REG, vr, disp));
        }
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.ADDRESS;
        resultReg = vr;
        resultRegAddressAlignment = 8;
        return;
      }

      if (rt.memorySizeAsInt(machine) > 0) {
        appendInstruction(new StoreInstruction(Opcodes.SD, MipsRegisterSet.IR_REG, vr, getDisp(0)));
        if (rt.memorySizeAsInt(machine) > MipsRegisterSet.IREG_SIZE)
          appendInstruction(new StoreInstruction(Opcodes.SD, MipsRegisterSet.IR2_REG, vr, getDisp(MipsRegisterSet.IREG_SIZE)));
      }

      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.ADDRESS;
      resultReg = vr;
      resultRegAddressAlignment = 8;

      return;
    }

    if (!rt.isRealType()) {
      appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), intReturn, true);
      genRegToReg(MipsRegisterSet.IR_REG, rt.memorySizeAsInt(machine), ir, rt.memorySizeAsInt(machine));
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), realReturn, true);
    int size = rt.memorySizeAsInt(machine);

    if (rt.isComplexType())
      genRegToReg(MipsRegisterSet.FR2_REG, size, ir + 1, size);

    genRegToReg(MipsRegisterSet.FR_REG, size, ir, size);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private static final short[] intReturn = {MipsRegisterSet.IR_REG, MipsRegisterSet.IR2_REG};
  private static final short[] realReturn = {MipsRegisterSet.FR_REG, MipsRegisterSet.FR2_REG};

  public void visitCompareExpr(CompareExpr e)
  {
    Expr la   = e.getOperand(0);
    Expr ra   = e.getOperand(1);
    Type rt   = processType(la);
    int  mode = e.getMode();
    int  ir   = registers.getResultRegister(RegisterSet.AIREG);

    throw new scale.common.NotImplementedError("visitCompareExpr");
  }

  /**
   * Generate instructions to convert an integer value in an integer register to an integer value of a different size.
   * The source and destination may be the same register.
   * This logic assumes that the value in the source register conforms to the specified type.
   * @param src is the register containing the source value
   * @param srcSize is the source value size
   * @param srcSigned is true if the source value is signed
   * @param dest is the register containing the result
   * @param destSize is the size of the result value
   * @param destSigned is true if the result value is signed
   * @return the register containing the convert value
   */
  protected int convertIntRegValue(int src, int srcSize, boolean srcSigned, int dest, int destSize, boolean destSigned)
  {
    if (destSize == srcSize)
      return src;

    if (destSigned) {
      switch (destSize) {
      case 1:
        appendInstruction(new IntOpLitInstruction(Opcodes.DSLL32, dest, src, getDisp(24)));
        appendInstruction(new IntOpLitInstruction(Opcodes.DSRA32, dest, dest, getDisp(24)));
        return dest;
      case 2:
        appendInstruction(new IntOpLitInstruction(Opcodes.DSLL32, dest, src, getDisp(16)));
        appendInstruction(new IntOpLitInstruction(Opcodes.DSRA32, dest, dest, getDisp(16)));
        return dest;
      case 4:
        appendInstruction(new IntOpInstruction(Opcodes.SLLV, dest, src, MipsRegisterSet.ZERO_REG)); // Sign extend
        return dest;
      }
    } else {
      switch (destSize) {
      case 1:
        appendInstruction(new IntOpLitInstruction(Opcodes.ANDI, dest, src, getDisp(0xFF)));
        return dest;
      case 2:
        appendInstruction(new IntOpLitInstruction(Opcodes.ANDI, dest, src, getDisp(0xFFFF)));
        return dest;
      case 4:
        appendInstruction(new IntOpLitInstruction(Opcodes.DSLL32, dest, src, getDisp(0)));
        appendInstruction(new IntOpLitInstruction(Opcodes.DSRL32, dest, dest, getDisp(0)));
        return dest;
      }
    }
    throw new scale.common.InternalError("Funny register size " + destSize);
  }

  protected Branch genFtnCall(String name, short[] uses, short[] defs)
  {
    int          handle = allocateTextArea(name, TEXT);
    Displacement disp   = new SymbolDisplacement(name, handle);
    Label        lab    = createLabel();
    Branch       jsr    = new JumpRegInstruction(Opcodes.JALR, MipsRegisterSet.T9_REG, 1, null, true);

    associateDispWithArea(handle, disp);

    jsr.additionalRegsUsed(uses);
    jsr.additionalRegsKilled(registers.getCalleeUses());
    jsr.additionalRegsSet(defs);
    jsr.addTarget(lab, 0);
    jsr.markAsCall();

    usesGp = true;

    lastInstruction.specifySpillStorePoint();
    appendInstruction(new LoadInstruction(Opcodes.LW, MipsRegisterSet.T9_REG, MipsRegisterSet.GP_REG, disp, FT_CALL16));
    appendInstruction(jsr);
    appendLabel(lab);
    lab.markAsFirstInBasicBlock();
    lab.setNotReferenced();
    callsRoutine = true;

    return jsr;
  }

  protected void genAlloca(Expr arg, int reg)
  {
    throw new scale.common.NotImplementedError("genAlloca");
  }

  /**
   * Generate code to zero out a floating point register.
   */
  protected void zeroFloatRegister(int dest, int destSize)
  {
    appendInstruction(new FltOpInstruction(Opcodes.DMTC1, MipsRegisterSet.ZERO_REG, dest));
  }

  /**
   * Generate code to obtain the real part of a complex value.
   */
  protected void genRealPart(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genRealPart");
  }

  /**
   * Convert real value in a real register to an integer value
   * in a real register.  The result is rounded down.
   * @param src is the register containing the source value
   * @param srcSize is the source value size
   * @param dest is the register containing the result
   * @param destSize is the size of the result value
   * @param destSigned is true if the result value is signed
   * @return the register containing the converted value
   */
  protected int genRealToInt(int src, int srcSize, int dest, int destSize, boolean destSigned)
  {
    int ds = destSize;
    if (srcSize > ds)
      ds = srcSize;

    int ft = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int it = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, destSize));

    genRegToReg(src, ft);
    int op;
    if (srcSize > 4)
      op = destSize > 4 ? Opcodes.TRUNC_L_D : Opcodes.TRUNC_W_D;
    else
      op = destSize > 4 ? Opcodes.TRUNC_L_S : Opcodes.TRUNC_W_S;

    appendInstruction(new FltOpInstruction(op, ft, ft)); 
    genRegToReg(ft, it);

    return convertIntRegValue(it, destSize, true, dest, destSize, destSigned);
  }

  /**
   * Convert a real value in a real register to a 
   * real value in a real register.
   */
  protected void genRealToReal(int src, int srcSize, int dest, int destSize)
  {
    if (srcSize == destSize) {
      appendInstruction(new FltOpInstruction(destSize > 4 ? Opcodes.MOV_D : Opcodes.MOV_S, dest, src));
      return;
    }
      
    if (srcSize == 8 && destSize == 4)
      appendInstruction(new FltOpInstruction(Opcodes.CVT_S_D, dest, src));
    else if (srcSize == 4 && destSize == 8)
      appendInstruction(new FltOpInstruction(Opcodes.CVT_D_S, dest, src));
  }

  /**
   * Convert integer value in a real register to an integer value
   * in a real register.  The result is rounded  to the nearest integer.
   */
  protected void genRealToIntRound(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genRealToIntRound");
  }

  /**
   * Convert an unsigned integer value in an integer register to a 
   * real value in a real register.
   */
  protected void genUnsignedIntToReal(int src, int srcSize, int dest, int destSize)
  {
    int treg = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));

    genRegToReg(src, treg);
    genIntToReal(treg, srcSize, dest, destSize);
  }

  /**
   * Convert an integer value in an integer register to a 
   * real value in a real register.
   */
  protected void genIntToReal(int src, int srcSize, int dest, int destSize)
  {
    int op;
    int treg = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));

    genRegToReg(src, treg);

    if (srcSize > 4)
      op = destSize > 4 ? Opcodes.CVT_D_L : Opcodes.CVT_S_L;
    else
      op = destSize > 4 ? Opcodes.CVT_D_W : Opcodes.CVT_S_W;

    appendInstruction(new FltOpInstruction(op, dest, treg));
  }

  protected void genFloorOfReal(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genFloorOfReal");
  }

  protected void genRoundReal(int src, int srcSize, int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("genRoundReal");
  }

  public void visitExponentiationExpr(ExponentiationExpr e)
  {
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    int  ir = registers.getResultRegister(ct.getTag());
    Expr la = e.getOperand(0);
    Expr ra = e.getOperand(1);

    throw new scale.common.NotImplementedError("visitExponentiationExpr");
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    Type ct = processType(e);
    int  ir = registers.getResultRegister(ct.getTag());
    int  bs = ct.memorySizeAsInt(machine);
    Expr ra = e.getOperand(1);
    Expr la = e.getOperand(0);
    Type rt = ra.getType();
    Type lt = la.getType();

    if (ct.isRealType()) {
      doBinaryOp(e, DIV);
      return;
    }

    boolean signed = (rt.isSigned() && lt.isSigned());

    int opcode;

    if (signed) {
      if (rt.memorySizeAsInt(machine) > 4)
        opcode = Opcodes.DDIV;
      else
        opcode = Opcodes.DIV;
    } else {
      if (rt.memorySizeAsInt(machine) > 4)
        opcode = Opcodes.DDIVU;
      else
        opcode = Opcodes.DIVU;
    }

    // Should optimize for multiplication with constants -Jeff

    needValue(la);
    int laReg = resultReg;

    needValue(ra);
    int raReg = resultReg;
    
    appendInstruction(new MultInstruction(opcode, laReg, raReg));
    appendInstruction(new TrapInstruction(Opcodes.TEQ, MipsRegisterSet.ZERO_REG, raReg));
    appendInstruction(new MFSpecialInstruction(Opcodes.MFLO, ir));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitRemainderExpr(RemainderExpr e)
  {
    Type ct = processType(e);
    int  ir = registers.getResultRegister(ct.getTag());
    int  bs = ct.memorySizeAsInt(machine);
    Expr ra = e.getOperand(1);
    Expr la = e.getOperand(0);
    Type rt = ra.getType();
    Type lt = la.getType();

    if (ct.isRealType()) {
      throw new scale.common.NotImplementedError("visitRemainderExpr, realType");
    }

    // Future optimization: check for constants, to optimize i%2, i%4, etc. -Jeff

    boolean signed = (rt.isSigned() && lt.isSigned());

    int opcode;

    if (signed) {
      if (rt.memorySizeAsInt(machine) > 4)
        opcode = Opcodes.DDIV;
      else
        opcode = Opcodes.DIV;
    } else {
      if (rt.memorySizeAsInt(machine) > 4)
        opcode = Opcodes.DDIVU;
      else
        opcode = Opcodes.DIVU;
    }

    needValue(la);
    int laReg = resultReg;

    needValue(ra);
    int raReg = resultReg;
    
    appendInstruction(new MultInstruction(opcode, laReg, raReg));
    appendInstruction(new TrapInstruction(Opcodes.TEQ, MipsRegisterSet.ZERO_REG, raReg));
    appendInstruction(new MFSpecialInstruction(Opcodes.MFHI, ir));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private static final short[] retUses = {MipsRegisterSet.SP_REG};

  /**
   * Load the value of a field to a register.
   * @param fd defines the field
   * @param fieldOffset is the offset from the specified address
   * @param adr is the register holding the address
   * @param adrha specifies the type of address
   * @param adraln specifies the alignment of the address
   * @param adrrs specifies the size of the structure if it is in a register
   * @param dest specifies the register to hold the field value
   */
  protected void loadFieldValue(FieldDecl  fd,
                                long       fieldOffset,
                                int        adr,
                                ResultMode adrha,
                                int        adraln,
                                long       adrrs,
                                int        dest)
  {
    Type ft        = processType(fd);
    int  bits      = fd.getBits();
    int  fa        = fd.getFieldAlignment();
    int  bitOffset = fd.getBitOffset();
    int  byteSize  = ft.memorySizeAsInt(machine);

    Displacement disp;
    if ((fieldOffset >= -0x7fff) && (fieldOffset <= 0x7fff))
      disp = getDisp((int) fieldOffset);
    else {
      int tr = registers.newTempRegister(RegisterSet.ADRREG);
      genLoadImmediate(fieldOffset, adr, tr);
      disp = getDisp(0);
      adr = tr;
    }

    int faln = 8;
    if (byteSize <= 4)
      faln = 4;

    boolean fieldAligned = (0 == (adraln % 4));
    if (bits == 0)
      loadFromMemory(dest, adr, disp, byteSize, fieldAligned ? fa : 1, ft.isSigned());
    else
      throw new scale.common.NotImplementedError("loadFieldValueExpr, bits");
    //loadBitsFromMemory(dest, adr, disp, bits, bitOffset, fa, ft.isSigned());
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = dest;
  }

  /**
   * Generate a branch based on the value of an expression.
   * The value may be floating point or integer but it is never a value pair.
   * @param which specifies the branch test (EQ, NE, LT, ...)
   * @param treg specifies the register containing the value
   * @param signed is true if the value is signed
   * @param labt specifies the path if the test fails
   * @param labf specifies the path if the test succeeds
   */
  protected void genIfRegister(CompareMode which, int treg, boolean signed, Label labt, Label labf)
  {
    int opcode = ibops[which.ordinal()];
    if (registers.floatRegister(treg))
      throw new scale.common.NotImplementedError("if-register on real type");

    LabelDisplacement disp = new LabelDisplacement(labf);
    Branch            br;

    if (which == CompareMode.EQ || which == CompareMode.NE)
      br = new CmpBranchInstruction(opcode, treg, MipsRegisterSet.ZERO_REG, disp, 2, null, false);
    else
      br = new CmpBranchInstruction(opcode, treg, disp, 2, null, false);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);

    appendInstruction(br);
  }

  /**
   * Generate a branch based on a relational expression.
   * @param rflag true if the test condition should be reversed
   * @param predicate specifies the relational expression
   * @param tc specifies the path if the test succeeds
   * @param fc specifies the path if the test fails
   */
  protected void genIfRelational(boolean rflag, MatchExpr predicate, Chord tc, Chord fc)
  {
    CompareMode which            = predicate.getMatchOp();
    boolean     flag             = false;
    boolean     fflag            = false;
    Expr        la               = predicate.getOperand(0);
    Expr        ra               = predicate.getOperand(1);
    CompareMode ow               = which;
    boolean     genUnconditional = false;
    boolean     sense            = doNext(tc);

    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
      which = which.argswap();
    }

    Type lt = processType(la);
    if (lt.isRealType()) {    
      int size          = lt.memorySizeAsInt(machine);

      needValue(la);
      int laReg = resultReg;
      needValue(ra);
      int raReg = resultReg;

      Label labt = getBranchLabel(tc);
      Label labf = getBranchLabel(fc);

      if (!sense) {
        if (doNext(fc)) {
          which = which.reverse();
          Label t = labt;
          labt = labf;
          labf = t;
        } else
          genUnconditional = true;
      }

      if (rflag)
        which = which.reverse();

      which = which.reverse();

      int     compareOpcode       = Opcodes.lookupFltCompare(size, which.ordinal());
      boolean compareOrderSwapped = Opcodes.lookupFltCompareOrder(size, which.ordinal());
      int     branchOpcode        = Opcodes.lookupFltBranch(size, which.ordinal());

      if (compareOrderSwapped)
        appendInstruction(new FltCmpInstruction(compareOpcode, raReg, laReg));
      else
        appendInstruction(new FltCmpInstruction(compareOpcode, laReg, raReg));

      LabelDisplacement disp = new LabelDisplacement(labf);
      MipsBranch inst = new FltBranchInstruction(branchOpcode, disp, 2, null);

      inst.addTarget(labt, 1);
      inst.addTarget(labf, 0);

      appendInstruction(inst);

      if (genUnconditional)
        generateUnconditionalBranch(labt);

      return;
    } else if (ra.isLiteralExpr()) {
      LiteralExpr le  = (LiteralExpr) ra;
      Literal     lit = le.getLiteral();

      if (lit instanceof BooleanLiteral) {
        BooleanLiteral bl = (BooleanLiteral) lit;
        flag = !bl.getBooleanValue();
      } else if (lit instanceof IntLiteral && lt.isSigned()) {
        IntLiteral il = (IntLiteral) lit;
        long       iv = il.getLongValue();
        if (iv == 0)
          flag = true;
      } else if (lit instanceof SizeofLiteral) {
        int iv = (int) valueOf((SizeofLiteral) lit);
        if (iv == 0)
          flag = true;
      }
    }

    if (flag) {
      if (rflag)
        which = which.reverse();
      genIfRegister(which, la, tc, fc);
      return;
    }
 
    if (ow == CompareMode.NE) {
      predicate = new EqualityExpr(predicate.getType(), la.copy(), ra.copy());
      Chord t = tc;
      tc = fc;
      fc = t;
    }

    genIfRegister(rflag ? CompareMode.EQ : CompareMode.NE, predicate, tc, fc);
  }

  /**
   * Calculate both the index and offset of an array element.
   * Return the offset from the address in <code>resultRegAddressOffset</code>.
   * resultReg is set to the register containing the index value.
   * @param offset is an expression specifying the offset field of the ArrayIndexExpr
   * @param index is an expression specifying the index field of the ArrayIndexExpr
   * @return the offset value
   */
  private void calcArrayOffset(Expr offset, Expr index)
  {
    int  sw   = 0; // Specify which combination of index & offset.
    long oval = 0; // Constant offset value.
    int  off  = 0; // Register containing offset value.
    long ival = 0; // Constant index value.
    int  ind  = 0; // Register containing index value.

    if (index.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) index).getLiteral();
      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        ival = il.getLongValue();
        sw += 2;
      } else if (lit instanceof SizeofLiteral) {
        ival = valueOf((SizeofLiteral) lit);
        sw += 2;
      }
    }

    boolean negOff = false;
    if (offset instanceof NegativeExpr) {
      offset = offset.getOperand(0);
      negOff = true;
    }

    if (offset.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) offset).getLiteral();
      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        oval = il.getLongValue();
        if (negOff) {
          negOff = false;
          oval = -oval;
        }
        sw += 1;
      } else if (lit instanceof SizeofLiteral) {
        oval = valueOf((SizeofLiteral) lit);
        if (negOff) {
          negOff = false;
          oval = -oval;
        }
        sw += 1;
      }
    }

    switch (sw) {
    case 0:
      needValue(index);
      ind = resultReg;
      needValue(offset);
      off = resultReg;
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;

      if (off == MipsRegisterSet.ZERO_REG) {
        resultReg = ind;
      } else if (negOff) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.DSUB, tr, ind, off));
        resultReg = tr;
      } else if (ind == MipsRegisterSet.ZERO_REG) {
        resultReg = off;
      } else {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.DADDU, tr, ind, off));
        resultReg = tr;
      }
      return;
    case 1:
      needValue(index);
      resultRegAddressOffset = oval;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    case 2:
      needValue(offset);
      if (negOff) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.DSUB, tr, MipsRegisterSet.ZERO_REG, resultReg));
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = tr;
      }
      resultRegAddressOffset = ival;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    default:
    case 3:
      resultRegAddressOffset = oval + ival;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = MipsRegisterSet.ZERO_REG;
      return;
    }
  }


  /**
   * Load an array element into a register.
   * @param aie specifies the array element
   * @param dest specifies the register
   */
  protected void loadArrayElement(ArrayIndexExpr aie, int dest)
  {
    PointerType vt      = (PointerType) processType(aie);
    Type        et      = vt.getPointedTo();
    int         bs      = et.memorySizeAsInt(machine);
    Expr        array   = aie.getArray();
    Expr        index   = aie.getIndex();
    Expr        offset  = aie.getOffset();
    int         daln    = vt.getPointedTo().alignment(machine);

    calcAddressAndOffset(array, 0);
    long offseta = resultRegAddressOffset;
    int  arr     = resultReg;

    calcArrayOffset(offset, index);
    long offseth = resultRegAddressOffset;
    int  tr      = resultReg;

    int tr2 = registers.newTempRegister(RegisterSet.INTREG);
    if (tr == MipsRegisterSet.ZERO_REG)
      genRegToReg(arr, tr2);
    else {
      switch (bs) {
      case 1:
        appendInstruction(new IntOpInstruction(Opcodes.ADDU, tr2, tr, arr));
        break;
      default:
        int tr3 = registers.newTempRegister(RegisterSet.INTREG);
        genMultiply(bs, tr, tr3, true); //genMultiplyLong(bs, tr, tr3, true);
        appendInstruction(new IntOpInstruction(Opcodes.ADDU, tr2, tr3, arr));
        break;
      }
    }

    long offsetl = genLoadHighImmediate(offseta + offseth * bs, tr2);
    loadFromMemoryWithOffset(dest, tr2, offsetl, bs, daln, et.isSigned(), et.isRealType());
  }

  /**
   * Load the address of an array element into a register.
   * Return the offset from the address in <code>resultRegAddressOffset</code>.
   * The address is specified in <code>resultReg</code>.
   * @param aie specifies the array elementxpression
   * @param offseta is the offset from the address
   */
  protected void calcArrayElementAddress(ArrayIndexExpr aie, long offseta)
  {
    PointerType vt     = (PointerType) processType(aie);
    Type        et     = vt.getPointedTo();
    int         bs     = et.memorySizeAsInt(machine);
    Expr        array  = aie.getArray();
    Expr        index  = aie.getIndex();
    Expr        offset = aie.getOffset();
    int         ir     = registers.getResultRegister(vt.getTag());

    calcArrayOffset(offset, index);
    long offseth = resultRegAddressOffset;
    int  tr      = resultReg;

    calcAddressAndOffset(array, offseta);
    if (tr == MipsRegisterSet.ZERO_REG) {
      resultRegAddressOffset += offseth * bs;
      resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
      return;
    }

    int  arr     = resultReg;

    offseta = resultRegAddressOffset + bs * offseth;

    switch (bs) {
    case 1:
      appendInstruction(new IntOpInstruction(Opcodes.DADDU, ir, tr, arr));
      break;
    default:
      int tr3 = registers.newTempRegister(RegisterSet.ADRREG);
      genMultiply(bs, tr, tr3, true);
      appendInstruction(new IntOpInstruction(Opcodes.ADDU, ir, tr, arr));
      break;
    }

    resultRegAddressOffset = offseta;
    resultRegMode = ResultMode.ADDRESS_VALUE;
    resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
    resultReg = ir;
  }

  private void genMultiplyLong(long value, int src, int dest, boolean signed)
  {
    if ((value < 2) && (value > -2)) {
      switch ((int) value) {
      case -1:
        appendInstruction(new IntOpInstruction(Opcodes.DSUBU, dest, MipsRegisterSet.ZERO_REG, src));
        return;
      case 0:
        appendInstruction(new IntOpInstruction(Opcodes.OR, dest, MipsRegisterSet.ZERO_REG, MipsRegisterSet.ZERO_REG));
        return;
      case 1:
        appendInstruction(new IntOpInstruction(Opcodes.DADDU, dest, src, MipsRegisterSet.ZERO_REG));
        return;
      }
    }

    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // It is a multiply by a power of 2.
      if (shift >= 32)
        appendInstruction(new IntOpLitInstruction(Opcodes.DSLL32, dest, src, getDisp(shift - 32)));
      else
        appendInstruction(new IntOpLitInstruction(Opcodes.DSLL, dest, src, getDisp(shift)));
      return;
    }

    int tr = registers.newTempRegister(RegisterSet.INTREG);
    tr = genLoadImmediate(value, tr);

    appendInstruction(new MultInstruction(signed ? Opcodes.DMULT : Opcodes.DMULTU, src, tr));
    appendInstruction(new MFSpecialInstruction(Opcodes.MFLO, dest));
  }

  private void genMultiply(long value, int src, int dest, boolean signed)
  {
    if ((value < 2) && (value > -2)) {
      switch ((int) value) {
      case -1:
        appendInstruction(new IntOpInstruction(Opcodes.DSUB, dest, MipsRegisterSet.ZERO_REG, src));
        return;
      case 0:
        appendInstruction(new IntOpInstruction(Opcodes.OR, dest, MipsRegisterSet.ZERO_REG, MipsRegisterSet.ZERO_REG));
        return;
      case 1:
        appendInstruction(new IntOpInstruction(Opcodes.ADDU, dest, src, MipsRegisterSet.ZERO_REG));
        return;
      }
    }

    int shift = Lattice.powerOf2(value);
    if (shift >= 0 && shift < 32) { // It is a multiply by a power of 2.
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, src, getDisp(shift)));
      return;
    }

    int tr = registers.newTempRegister(RegisterSet.INTREG);
    tr = genLoadImmediate(value, tr);
    
    appendInstruction(new MultInstruction(signed ? Opcodes.MULT : Opcodes.MULTU, src, tr));
    appendInstruction(new MFSpecialInstruction(Opcodes.MFLO, dest));
  }

  public void visitMultiplicationExpr(MultiplicationExpr e)
  { 
    Type    ct     = processType(e);
    int     ir     = registers.getResultRegister(ct.getTag());
    int     bs     = ct.memorySizeAsInt(machine);
    Expr    ra     = e.getOperand(1);
    Expr    la     = e.getOperand(0);
    Type    rt     = ra.getType();
    Type    lt     = la.getType();
    boolean signed = (rt.isSigned() && lt.isSigned());

    if (ct.isRealType()) {
      doBinaryOp(e, MUL);
      return;
    }

    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
    }

    if (ra.isLiteralExpr()) {
      LiteralExpr le    = (LiteralExpr) ra;
      Literal     lit   = le.getLiteral();
      boolean     flag  = false;
      long        value = 0;

      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        value = il.getLongValue();
        flag = true;
      } else if (lit instanceof SizeofLiteral) {
        value = valueOf((SizeofLiteral) lit);
        flag = true;
      }

      if (flag) {
        if (value == 0) {
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = MipsRegisterSet.ZERO_REG;
          return;
        }
        ir = registers.getResultRegister(ct.getTag());
        needValue(la);
        if (bs > 4)
          genMultiplyLong(value, resultReg, ir, signed);
        else
          genMultiply(value, resultReg, ir, signed);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    int opcode;

    if (signed) {
      if (rt.memorySizeAsInt(machine) > 4)
        opcode = Opcodes.DMULT;
      else
        opcode = Opcodes.MULT;
    } else {
      if (rt.memorySizeAsInt(machine) > 4)
        opcode = Opcodes.DMULTU;
      else
        opcode = Opcodes.MULTU;
    }

    // Should optimize for multiplication with constants -Jeff

    needValue(la);
    int laReg = resultReg;

    ra.visit(this);
    int raReg = resultReg;
    
    appendInstruction(new MultInstruction(opcode, laReg, raReg));
    appendInstruction(new MFSpecialInstruction(Opcodes.MFLO, ir));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    Type ct  = processType(e);
    int  bs  = ct.memorySizeAsInt(machine);
    Expr arg = e.getOperand(0);

    needValue(arg);

    if (resultReg == MipsRegisterSet.ZERO_REG)
      return;

    int ir  = registers.getResultRegister(ct.getTag());
    if (ct.isRealType()) {
      appendInstruction(new FltOpInstruction((bs > 4) ? Opcodes.NEG_D : Opcodes.NEG_S, ir, resultReg));
      if (ct.isComplexType())
        appendInstruction(new FltOpInstruction((bs > 4) ? Opcodes.NEG_D : Opcodes.NEG_S, ir + 1, resultReg + 1));
    } else {
      appendInstruction(new IntOpInstruction((bs > 4) ? Opcodes.DSUBU : Opcodes.SUBU, ir, MipsRegisterSet.ZERO_REG, resultReg));
    }
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  protected void genSignFtn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("Fortran SIGN intrinsic");
  }

  protected void genAtan2Ftn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("Fortran ATAN2 intrinsic");
  }

  protected void genDimFtn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("Fortran DIM intrinsic");
  }

  private static final short[] singleIntUse = {MipsRegisterSet.IA0_REG};
  private static final short[] singleFltUse = {MipsRegisterSet.FA0_REG};

  private void genFtnCall(String fname, int dest, int src, Type type)
  {
    boolean flt = registers.floatRegister(src);
    genRegToReg(src, flt ? MipsRegisterSet.FA0_REG : MipsRegisterSet.IA0_REG);
    genFtnCall(fname, flt ? singleFltUse : singleIntUse, null);
    genRegToReg(returnRegister(type.getTag(), true), dest);
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
    throw new scale.common.NotImplementedError("conjg");
  }

  protected void genReturnAddressFtn(int dest, int src, Type type)
  {
    genFtnCall("_scale_return_address", dest, src, type);
  }

  protected void genFrameAddressFtn(int dest, int src, Type type)
  {
    genFtnCall("_scale_frame_address", dest, src, type);
  }

  public void visitNotExpr(NotExpr e)
  {
    Expr arg = e.getOperand(0);
    int  ir  = registers.getResultRegister(RegisterSet.INTREG);

    needValue(arg);
    int src = resultReg;

    if (registers.floatRegister(src))
      throw new scale.common.InternalError("Not not allowed on " + arg);

    appendInstruction(new IntOpLitInstruction(Opcodes.SLTIU, ir, src, getDisp(1)));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitReturnChord(ReturnChord c)
  {
    Expr    a    = c.getResultValue();
    short[] uses = retUses;

    if (a != null) {
      Type at   = processType(a);
      int  dest = returnRegister(at.getTag(), false);

      registers.setResultRegister(dest);

      a.visit(this);

      registers.setResultRegister(-1);

      if (!at.isAtomicType()) {
        if (at.memorySizeAsInt(machine) <= (2 * MipsRegisterSet.IREG_SIZE)) {
          int off = 0;
          if ((resultRegAddressOffset > (MAX_IMM16 - 8)) || (resultRegAddressOffset < (MIN_IMM16 + 8))) {
            int tr = registers.newTempRegister(RegisterSet.ADRREG);
            genLoadImmediate(resultRegAddressOffset, resultReg, tr);
            resultReg = tr;
          } else
              off = (int) resultRegAddressOffset;

          if (structReturnsInFPRegs(at)) {
            int usesCount    = 1;
            int[] fieldSizes = fieldFloatSizes(at);
            int lop          = fieldSizes[0] > 4 ? Opcodes.LDC1 : Opcodes.LWC1;
            appendInstruction(new LoadInstruction(lop, MipsRegisterSet.FR_REG, resultReg, getDisp(off)));
            if (fieldSizes[1] > 0) {
              int no = at.memorySizeAsInt(machine) == 8 ? 4 : 8;
              appendInstruction(new LoadInstruction(lop, MipsRegisterSet.FR2_REG, resultReg, getDisp(off + no)));
              usesCount++;
            }

            uses = new short[usesCount + retUses.length];
            int[] canUse = { MipsRegisterSet.FR_REG, MipsRegisterSet.FR2_REG };
            for (int i=0; i < usesCount; i++)
              uses[i] = (short) canUse[i];
            for (int i = 0; i < retUses.length; i++)
              uses[usesCount + i] = retUses[i];
          } else {
            int usesCount = 0;
            if (at.memorySizeAsInt(machine) > 0) {
              appendInstruction(new LoadInstruction(Opcodes.LD, MipsRegisterSet.IR_REG, resultReg, getDisp(off)));
              usesCount++;
              if (at.memorySizeAsInt(machine) > MipsRegisterSet.IREG_SIZE) {
                Displacement disp = getDisp(off + MipsRegisterSet.IREG_SIZE);
                appendInstruction(new LoadInstruction(Opcodes.LD, MipsRegisterSet.IR2_REG, resultReg, disp));
                usesCount++;
              }
            }
            uses = new short[usesCount + retUses.length];
            int[] canUse = { MipsRegisterSet.IR_REG, MipsRegisterSet.IR2_REG };

            for (int i=0; i < usesCount; i++)
              uses[i] = (short) canUse[i];
            for (int i = 0; i < retUses.length; i++)
              uses[usesCount + i] = retUses[i];
          }
        } else {
          moveWords(resultReg,
                    resultRegAddressOffset,
                    structAddress,
                    0,
                    structSize,
                    resultRegAddressAlignment);
        }
      } else {
        int nr = registers.numContiguousRegisters(resultReg);
        int fr = registers.rangeBegin(dest);
        if (registers.pairRegister(resultReg))
          nr *= 2;
        uses = new short[nr + retUses.length];
        for (int i = 0; i < nr; i++)
          uses[i] = (short) (fr + i);
        for (int i = 0; i < retUses.length; i++)
          uses[nr + i] = retUses[i];
        genRegToReg(resultReg, at.memorySizeAsInt(machine), dest, at.memorySizeAsInt(machine));
      }
    }

    if (usesGp) {
      Instruction   first = currentBeginMarker;
      ProcedureType pt    = (ProcedureType) processType(currentRoutine);

      Displacement orig = currentRoutine.getDisplacement();
      addrDisp = orig.unique();
      int addr = registers.newTempRegister(RegisterSet.INTREG);
      
      Instruction temp = first;

      first = insertInstruction(new LoadImmediateInstruction(Opcodes.LUI,
                                                             addr,
                                                             addrDisp,
                                                             FT_HI_NEG_GPREL),
                                first);
      first = insertInstruction(new IntOpLitInstruction(Opcodes.ADDIU,
                                                        addr,
                                                        addr,
                                                        addrDisp,
                                                        FT_LO_NEG_GPREL),
                                first);
      first = insertInstruction(new IntOpInstruction(Opcodes.ADDU,
                                                     MipsRegisterSet.GP_REG,
                                                     MipsRegisterSet.T9_REG,
                                                     addr),
                                first);

      first = temp;
    }

    returnInst = lastInstruction;
    Branch inst = new JumpRegInstruction(Opcodes.JR, MipsRegisterSet.RA_REG, 0, null, false);
    inst.additionalRegsUsed(uses); // Specify which registers are used.
    appendInstruction(inst);
  }

  /**
   * Store a value in a register to a symbolic location in memory.
   * @param src is the value
   * @param dsize is the size of the value in addressable memory units
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param isReal is true if the value in the register is a floating point value
   * @param disp specifies the location
   */
  protected void storeRegToSymbolicLocation(int src, int dsize, long alignment, boolean isReal, Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

    //appendInstruction(new LoadImmediateInstruction(Opcodes.LUI, dest, disp, FT_HI));
    int dest = loadMemoryAddress(disp);

    storeIntoMemory(src, dest, getDisp(0), dsize, alignment);
  }

  /**
   * Store a value into a field of a structure.
   * @param lhs specifies the field of the structure
   * @param rhs specifies the value
   */
  protected void storeLfae(LoadFieldAddressExpr lhs, Expr rhs)
  {
    Expr struct = lhs.getStructure();

    processType(struct);

    FieldDecl fd          = lhs.getField();
    long      fieldOffset = fd.getFieldOffset();
    Type      ft          = processType(fd);
    int       bits        = fd.getBits();

    int fa        = fd.getFieldAlignment();
    int bitOffset = fd.getBitOffset();
    int byteSize  = ft.memorySizeAsInt(machine);

    rhs.visit(this);
    int        src    = resultReg;
    long       srcoff = resultRegAddressOffset;
    ResultMode srcha  = resultRegMode;
    int        srcaln = resultRegAddressAlignment;

    calcAddressAndOffset(struct, fieldOffset);
    fieldOffset = resultRegAddressOffset;
    int adr    = resultReg;
    int adraln = resultRegAddressAlignment;

    if (srcha == ResultMode.ADDRESS) {
      int aln = (((fieldOffset & 0x7) == 0) ? 8 : ((fieldOffset & 0x3) == 0) ? 4 : 1);
      if (resultRegAddressAlignment < aln)
        aln = resultRegAddressAlignment;
      if (srcaln < aln)
        aln = srcaln;

      moveWords(src, srcoff, adr, fieldOffset, byteSize, aln);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = src;
      resultRegAddressAlignment = srcaln;
      return;
    }

    needValue(src, srcoff, srcha);
    src = resultReg;

    Displacement disp;
    if ((fieldOffset >= MIN_IMM16) && (fieldOffset <= MAX_IMM16))
      disp = getDisp((int) fieldOffset);
    else {
      int tr = registers.newTempRegister(RegisterSet.ADRREG);
      genLoadImmediate(fieldOffset, adr, tr);
      disp = getDisp(0);
      adr = tr;
    }

    int faln = 8;
    if (byteSize <= 4)
      faln = 4;

    boolean fieldAligned = (0 == (adraln % 4));
    if (bits == 0)
      storeIntoMemory(src, adr, disp, byteSize, fieldAligned ? fa : 1);
    else
      throw new scale.common.NotImplementedError("storeLfae, bits");
    //storeBitsIntoMemory(src, adr, disp, bits, bitOffset, fa);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = src;
  }

  /**
   * Create a new read-only data area whose value is a table of displacements.
   */
  protected Displacement createAddressTable(Chord[] entries, long[] indexes, int min, int max)
  {
    int   num = max + 1 - min;
    Label[] v = new Label[num];
    Label def = getBranchLabel(entries[entries.length - 1]);

    for (int i = 0; i < num; i++) {
      v[i] = def;
    }

    for (int i = 0; i < entries.length - 1; i++) {
      int in = (int) indexes[i] - min;
      if (in >= 0)
        v[in] = getBranchLabel(entries[i]);
    }

    String       name   = un.genName();
    int          handle = allocateData(name, RDATA, SpaceAllocation.DAT_ADDRESS, 4, true, v, 1, 4);
    Displacement disp   = new SymbolDisplacement(name, handle);
    associateDispWithArea(handle, disp);

    return disp;
  }

  /**
   * Generate an integer-literal operate instruction.
   */
  private void doIntOperate(int opcode, int opcodeImm, int rc, int ra, long value, boolean subtract)
  {
    if ((value >= MIN_IMM16) && (value < MAX_IMM16)) {
      appendInstruction(new IntOpLitInstruction(opcodeImm, rc, ra, subtract ? getDisp(-1 * (int)value) : getDisp((int)value)));
      return;
    }

    int rb  = registers.newTempRegister(RegisterSet.AIREG);
    genLoadImmediate(value, rb);
    appendInstruction(new IntOpInstruction(opcode, rc, ra, rb));
  }

  protected boolean genSwitchUsingIfs(int testReg, Chord[] cases, long[] keys, int num, long spread)
  {
    if ((num > 5) && ((spread <= 512) || ((spread / num) <= 10)))
      return false;

    // Use individual tests.

    for (int i = 0; i < num - 1; i++) {
      int   tmp   = registers.newTempRegister(RegisterSet.AIREG);
      long  value = keys[i];
      Label labt  = getBranchLabel(cases[i]);
      Label labf  = createLabel();

      int rb  = registers.newTempRegister(registers.getType(resultReg));
      rb = genLoadImmediate(value, rb);

      LabelDisplacement disp  = new LabelDisplacement(labt);
      MipsBranch        inst  = new CmpBranchInstruction(Opcodes.BEQ, testReg, rb, disp, 2, null, false);

      labf.setNotReferenced();

      inst.addTarget(labt, 0);
      inst.addTarget(labf, 1);
      appendInstruction(inst);
      appendLabel(labf);
    }

    Chord deflt = cases[num - 1];
    if (!doNext(deflt))
      generateUnconditionalBranch(getBranchLabel(deflt));
    return true;
  }

  protected void genSwitchUsingTransferVector(int testReg, Chord[] cases, long[] keys, Label labt, long min, long max)
  {
    // Use a transfer vector.

    int ir = testReg;
    if (min != 0) {
      ir = registers.newTempRegister(RegisterSet.INTREG);
      doIntOperate(Opcodes.SUBU, Opcodes.ADDIU, ir, testReg, min, true);
    }
    int               tmp   = registers.newTempRegister(RegisterSet.AIREG);
    Label             labf  = createLabel();
    LabelDisplacement dispt = new LabelDisplacement(labt);
    MipsBranch        insti = new CmpBranchInstruction(Opcodes.BEQ, tmp, MipsRegisterSet.ZERO_REG, dispt, 2, null, false);
    Displacement      disp  = createAddressTable(cases, keys, (int) min, (int) max);
    int               adr   = loadMemoryAddress(disp);
    MipsBranch        instj = new JumpRegInstruction(Opcodes.JR, adr, cases.length, null, false);

    insti.addTarget(labt, 0);
    insti.addTarget(labf, 1);

    for (int i = 0; i < cases.length; i++)
      instj.addTarget(getBranchLabel(cases[i]), i);

    doIntOperate(Opcodes.SLTU, Opcodes.SLTIU, tmp, ir, 1 + max - min, false);

    appendInstruction(insti);
    appendLabel(labf);
    int tmp2 = registers.newTempRegister(RegisterSet.AIREG);
    appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tmp2, ir, getDisp(2)));
    appendInstruction(new IntOpInstruction(Opcodes.ADDU, adr, tmp2, adr));
    appendInstruction(new LoadInstruction(Opcodes.LW, adr, adr, getDisp(0)));
    appendInstruction(instj);
  }

  public void visitVaArgExpr(VaArgExpr e)
  {
    Expr vaList = e.getVaList();
    Type ct     = processType(e);
    int  rr;
    int  ir     = registers.getResultRegister(RegisterSet.INTREG);
    int  vr     = registers.getResultRegister(RegisterSet.ADRREG);
    int  adr    = registers.getResultRegister(RegisterSet.ADRREG);
    int  bs     = ct.memorySizeAsInt(machine);

    throw new scale.common.NotImplementedError("visitVaArgExpr");
  }

  public int getMaxAreaIndex()
  {
    return BSS;
  }
}
