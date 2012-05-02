package scale.backend.alpha;

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
 * This class converts Scribble into Alpha instructions.
 * <p>
 * $Id: AlphaGenerator.java,v 1.249 2007-10-04 19:57:50 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The conversion process may be modified by specifying various options:
 * <dl>
 * <dt>BWX<dd>Alpha processor supports SEXTB, SEXTW, STB, STW, LDBU & LDWU instructions.
 * <dt>FIX<dd>Alpha processor supports FTOIx, ITOFx, and SQRTx  instructions.
 * <dt>CIX<dd>Alpha processor supports CTLZ, CTPOP, and CTTZ instructions.
 * <dt>MVI<dd>Alpha processor supports MAXUxx, MINUxx, PERR, PKxB, and UNPKBx instructions.
 * </dl>
 * <p>
 * The Alpha memory and branch instructions use a displacement field
 * that is relative to an address in a register or to the program
 * counter.  Because this displacement is not known until load time,
 * the assembly program must specify how to generate the correct value
 * at load time.  We use an instance of a Displacement class to hold
 * this information.
 * <p>
 * Note - the Alpha assembler will do instruction scheduling unless told not to.
 */

public class AlphaGenerator extends scale.backend.Generator
{
  /**
   * Alpha processor supports SEXTB, SEXTW, STB, STW, LDBU & LDWU instructions.
   */
  public static final int BWX = 1;
  /**
   * Alpha processor supports FTOIx, ITOFx, and SQRTx  instructions.
   */
  public static final int FIX = 2;
  /**
   * Alpha processor supports CTLZ, CTPOP, and CTTZ instructions.
   */
  public static final int CIX = 4;
  /**
   * Alpha processor supports MAXUxx, MINUxx, PERR, PKxB, and UNPKBx instructions.
   */
  public static final int MVI = 16;
  /**
   * Run instruction scheduling
   */
  public static final int IS = 64;
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
   * Offset to the argument save area.
   */
  public static final int ARG_SAVE_OFFSET = 0;

  /**
   * Specify register usage for various calls to internal routines.
   */
  private static final short[] intReturn     = {AlphaRegisterSet.IR_REG};
  private static final short[] realReturn    = {AlphaRegisterSet.FR_REG};
  private static final short[] complexReturn = {AlphaRegisterSet.FR_REG,
                                                AlphaRegisterSet.FR_REG + 1};

  private static final int   SAVED_REG_SIZE       = 8; // Size of registers saved on the stack.
  private static final int   MAX_ARG_REGS         = 6; // Maximum number of arguments passed in registers.

  /**
   * Index from operation to opcode.  This is a n by 4 table where the row is 
   * indexed by the operation and the column by the type.
   */
  private static final int[] binops = {
    Opcodes.ADDL,  Opcodes.ADDQ,  Opcodes.ADDS,  Opcodes.ADDT,
    Opcodes.SUBL,  Opcodes.SUBQ,  Opcodes.SUBS,  Opcodes.SUBT,
    Opcodes.MULL,  Opcodes.MULQ,  Opcodes.MULS,  Opcodes.MULT,
    0,             0,             Opcodes.DIVS,  Opcodes.DIVT,
    Opcodes.AND,   Opcodes.AND,   0,             0,
    Opcodes.BIS,   Opcodes.BIS,   0,             0,
    Opcodes.XOR,   Opcodes.XOR,   0,             0,
    Opcodes.SRA,   Opcodes.SRA,   0,             0,
    Opcodes.SRL,   Opcodes.SRL,   0,             0,
    Opcodes.SLL,   Opcodes.SLL,   0,             0
  };

  /**
   * True if the binops operation can generate a larger result.
   */
  private boolean[] gensCarry = {
    true,  true,  true,  true,
    false, false, false, false,
    false, true};

  /**
   * Integer comparison branches.  a op 0
   */
  private static final int[] ibops = {
    Opcodes.BEQ,  Opcodes.BLE,  Opcodes.BLT, 
    Opcodes.BGT,  Opcodes.BGE,  Opcodes.BNE};
  /**
   * Floating comparison branches.  a op 0.0
   */
  private static final int[] fbops = {
    Opcodes.FBEQ, Opcodes.FBLE, Opcodes.FBLT,
    Opcodes.FBGT, Opcodes.FBGE, Opcodes.FBNE};

  /**
   * Relocation type: none
   */
  public static final int RT_NONE          = 0;
  /**
   * Relocation type: literal
   */
  public static final int RT_LITERAL       = 1;
  /**
   * Relocation type: literal base
   */
  public static final int RT_LITUSE_BASE   = 2;
  /**
   * Relocation type: literal use
   */
  public static final int RT_LITUSE_BYTOFF = 3;
  /**
   * Relocation type: literal use in JSR
   */
  public static final int RT_LITUSE_JSR    = 4;
  /**
   * Relocation type: GP displacement
   */
  public static final int RT_GPDISP        = 5;
  /**
   * Relocation type: GP relative high
   */
  public static final int RT_GPRELHIGH     = 6;
  /**
   * Relocation type: GP relative low
   */
  public static final int RT_GPRELLOW      = 7;

  /**
   * Maximum 16-bit field value.
   */
  public static final int MAX_IMM16 = 32767;
  /**
   * Minimum 16-bit field value.
   */
  public static final int MIN_IMM16 = -32768;

  public static final String[] relocTypeNames = {
    "", "literal", "lituse_base", "lituse_bytoff",
    "lituse_jsr", "gpdisp", "gprelhigh", "gprellow"};

  private static int[]  nxtMvReg = new int[5]; // Hold temporary register values.

  private int[]        remap;               // Used to generate the relocation information for the Alpha assembler.
  private int          asmSeq;              // Used to generate the relocation information for the Alpha assembler.
  private int          structAddress;       // Register containing structure address for routines that return structures
  private int          structSize;          // Register containing size of structure for routines that return structures
  private int          mask;                // Integer registers saved during routine execution.
  private int          fmask;               // Floating point registers saved during routine execution.
  private int          argBuildSize;        // Size of the area required for passing large arguments to subroutines.
  private int          localVarSize;        // Size of the area on the stack used for local variables.
  private int          entryOverflowSize;   // Size of the area on the stack used for overflow arguments.
  private boolean      bwx;                 // True if the target hardware supports BWX extensions.
  private boolean      fix;                 // True if the target hardware supports FIX extensions.
  private boolean      cix;                 // True if the target hardware supports CIX extensions.
  private boolean      mvi;                 // True if the target hardware supports MVI extensions.
  private boolean      usesGp;              // True if this routine uses the Gp register.
  private Displacement argDisp;             // Displacement to argument save area.
  private Displacement raDisp;              // Displacement to return address in stack frame.

  private HashMap<RoutineDecl, Label> routineLabel;  // Map from routine to entry label for bsr.
  private Vector<StackDisplacement>   localVar;      // Set of Displacements that reference variables on the stack.
  private Vector<Displacement>        entryOverflow; // Set of Displacements that reference argument overflow during call.

  /**
   * @param cg is the call graph to be transformed
   * @param machine specifies machine details
   * @param features contains various flags
   */
  public AlphaGenerator(CallGraph cg, Machine machine, int features)
  {
    super(cg, new AlphaRegisterSet(), machine, features);

    assert (machine instanceof AlphaMachine) : "Not correct machine " + machine;

    this.un      = new UniqueName("$$");
    this.remap   = null;
    this.asmSeq  = 0;
    this.routineLabel = new HashMap<RoutineDecl, Label>(11);

    // Determine what additional Alpha features are available.

    AlphaMachine am = (AlphaMachine) machine;
    bwx = am.hasBWX();
    fix = am.hasFIX();
    cix = am.hasCIX();
    mvi = am.hasMVI();

    readOnlyDataArea = RDATA;
  }

  /**
   * Generate the machine instructions for a CFG.
   */
  public void generateScribble()
  {
    mask                = 0;
    fmask               = 0;
    argBuildSize        = 0;
    localVarSize        = 0;
    entryOverflowSize   = 0;
    usesGp              = false;
    localVar            = new Vector<StackDisplacement>(23);
    entryOverflow       = new Vector<Displacement>(23);
    structAddress       = 0;
    structSize          = 0;
    argDisp             = null;
    stkPtrReg           = usesAlloca ? AlphaRegisterSet.FP_REG : AlphaRegisterSet.SP_REG;
    raDisp              = new IntegerDisplacement(0);

    super.generateScribble();
  }

  /**
   * Return relocation type in assembler format.
   */
  public String relocationInfo(Displacement disp, int relocType)
  {
    if (relocType != RT_NONE) {
      if (disp instanceof SymbolDisplacement) {
        SymbolDisplacement sd  = (SymbolDisplacement) disp;
        StringBuffer       buf = new StringBuffer("!");

        buf.append(relocTypeNames[relocType]);
        buf.append('!');
        buf.append(remap(sd.getSequence()));
        return buf.toString();
      } else if (disp instanceof OffsetDisplacement) {
        return relocationInfo(((OffsetDisplacement) disp).getBase(), relocType);
      }
    }
    return "";
  }

  /**
   * Insure that the relocation sequence number is lexically, monotonically increasing
   * in the generated assembly code.
   */
  private String remap(int id)
  {
    if (remap == null)
      remap = new int[SymbolDisplacement.used()];
    if (remap[id] == 0)
      remap[id] = 1 + asmSeq++;
    return Integer.toString(remap[id]);
  }

  /**
   * Do peephole optimizations before registers are allocated.
   */
  protected void peepholeBeforeRegisterAllocation(Instruction first)
  {
    Instruction inst = first;

    while (inst != null) {
      Instruction next   = inst.getNext();
      int         opcode = inst.getOpcode();

      if (opcode == Opcodes.EQV) { // Remove not operation if possible.
        IntOpInstruction iop   = (IntOpInstruction) inst;
        int              nxtOp = next.getOpcode();
        if ((nxtOp == Opcodes.BEQ) || (nxtOp == Opcodes.BNE)) {
          BranchInstruction bop = (BranchInstruction) next;
          int               tr  = bop.getTestRegister();
          if (iop.match(tr, AlphaRegisterSet.I0_REG, tr)) {
            bop.setOpcode((nxtOp == Opcodes.BEQ) ? Opcodes.BNE : Opcodes.BEQ);
            inst.nullify(registers);
            inst = next;
            continue;
          }
        }
      } else if (inst.isStore() && next.isLoad()) {
        // Remove a load that is from the same place as the store
        // preceding it.
        StoreInstruction store = (StoreInstruction) inst;
        LoadInstruction  load  = (LoadInstruction) next;
        Displacement     sdisp = store.getDisplacement();
        Displacement     ldisp = load.getDisplacement();
        int              saReg = store.getRb();
        int              laReg = load.getRb();
        if ((saReg == laReg) && sdisp.equivalent(ldisp)) {
          Instruction move  = null;
          int         svReg = store.getRa();
          int         lvReg = load.getRa();
          int         ops   = store.getOpcode();
          int         opl   = load.getOpcode();
          if (registers.floatRegister(svReg)) {
            if (((ops == Opcodes.STS) && (opl == Opcodes.LDS)) ||
                ((ops == Opcodes.STT) && (opl == Opcodes.LDT)))
              move = new FltOpInstruction(Opcodes.CPYS, svReg, svReg, lvReg);
          } else if (!registers.floatRegister(lvReg)) {
            if      ((ops == Opcodes.STQ) && (opl == Opcodes.LDQ))
              move = new IntOpLitInstruction(Opcodes.BIS, svReg, 0, lvReg);
            else if ((ops == Opcodes.STL) && (opl == Opcodes.LDL))
              move = new IntOpLitInstruction(Opcodes.ADDL, svReg, 0, lvReg);
            else if ((ops == Opcodes.STB) && (opl == Opcodes.LDBU))
              move = new IntOpLitInstruction(Opcodes.ZAPNOT, svReg, 1, lvReg);
            else if ((ops == Opcodes.STW) && (opl == Opcodes.LDWU))
              move = new IntOpLitInstruction(Opcodes.ZAPNOT, svReg, 3, lvReg);
          }

          if (move != null) {
//             System.out.println("** peep " + inst);
//             System.out.println("        " + next);
//             System.out.println("        " + move);
            move.setNext(load.getNext());
            store.setNext(move);
            inst = move;
            continue;
          }
        }
      }

      inst = next;
    }
  }

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
    if (!nis) {
      emit.emit("\t# Instruction Scheduling");
      emit.endLine();
    }
    AlphaAssembler asm = new AlphaAssembler(this, source, !nis);
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
    case 16: t = flt ? SpaceAllocation.DAT_LDBL : SpaceAllocation.DAT_LONG;  break;
    default: throw new scale.common.InternalError("Can't allocate objects of size " + size);
    }
    return t;
  }

  /**
   * Return the SpaceAllocation type for the specified Type.
   */
  public int getSAType(Type type)
  {
    type = processType(type);

    if (type.isAtomicType()) {
      if (type.isPointerType())
        return SpaceAllocation.DAT_ADDRESS;
      int sz = type.memorySizeAsInt(machine);
      if ((sz < 4) && type.isIntegerType())
        sz = 4;
      else if (type.isComplexType())
        sz >>= 1;
      return dataType(sz, type.isRealType());
    }

    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null) {
      Type et = at.getElementType();
      int  sz = et.memorySizeAsInt(machine);
      if (et.isFortranCharType())
        sz = 1;
      return dataType(sz, et.isRealType());
    }

    return SpaceAllocation.DAT_BYTE;
  }

  protected void assignDeclToMemory(String name, VariableDecl vd)
  {
    Type       dt         = vd.getType();
    boolean    readOnly   = dt.isConst();
    Expression init       = vd.getInitialValue();
    Type       vt         = processType(dt);
    int        aln        = (vd.isCommonBaseVar() ?
                             machine.generalAlignment() :
                             dt.alignment(machine));
    long       ts         = 1;
    Visibility visibility = vd.visibility();

    try {
      ts = vt.memorySize(machine);
    } catch (java.lang.Error ex) {
    }

    if ((ts < 8) && vt.isIntegerType())
      ts = 8;
    if (aln < 4) {
      if (ts <= 4)
        aln = 4;
      else
        aln = 8;
    }

    int area = BSS;
    if (init != null)
      area = readOnly ? RDATA : DATA;

    int             handle = allocateWithData(name, vt, ts, init, area, readOnly, 1, aln);
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
    Type dt = vd.getType();
    Type vt = processType(dt);

    AggregateType agt = vt.returnAggregateType();
    if (agt != null) {
      int  ft   = agt.allFieldsType();
      int  regd = registers.newTempRegister(vt.getTag());
      assert (ft == AggregateType.FT_F64) || (ft == AggregateType.FT_INT):
        "Mis-match on structure in register " + vd;
      defineDeclInRegister(vd, regd, ResultMode.STRUCT_VALUE);
      return;
    }

    int regd = registers.newTempRegister(vt.getTag());
    defineDeclInRegister(vd, regd, ResultMode.NORMAL_VALUE);
  }

  protected void assignDeclToStack(VariableDecl vd)
  {
    Type              dt    = vd.getType();
    Type              vt    = processType(dt);
    long              sts   = vt.memorySize(machine);
    StackDisplacement sdisp = new StackDisplacement(localVarSize);

    defineDeclOnStack(vd, sdisp);

    localVarSize += Machine.alignTo(sts, AlphaRegisterSet.IREG_SIZE);
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
    int                info2  = newLabel();
    Label              lab    = getLabel(info2);

    associateDispWithArea(handle, disp);
    defineRoutineInfo(rd, disp);
    routineLabel.put(rd, lab);
  }

  /**
   * Assign the routine's arguments to registers or the stack.
   */
  protected void layoutParameters()
  {
    ProcedureType pt      = (ProcedureType) processType(currentRoutine);
    int           nextArg = 0;
    long          offset  = 0;
    Type          rt      = processType(pt.getReturnType());

    if (!(rt.isAtomicType() || rt.isVoidType()))
      nextArg++;

    if (usesVaStart) // Must save arguments on the stack.
      argDisp = new StackDisplacement(0); // This stack offset will be modified later.

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);

      if (fd instanceof UnknownFormals)
        break;

      boolean volat = fd.getType().isVolatile();
      Type    vt    = processType(fd);

      if (vt.isAtomicType()) {

        // The first MAX_ARG_REGS scaler arguments are in the argument registers, 
        // remaining words have already been placed on the stack by the caller.

        if (usesVaStart) { // Argument must be saved on the stack.
          int loc = MAX_ARG_REGS * SAVED_REG_SIZE + nextArg * SAVED_REG_SIZE;

          if (vt.isRealType() && (nextArg < MAX_ARG_REGS))
            loc = nextArg * SAVED_REG_SIZE;

          Displacement disp = argDisp.offset(loc);

          defineDeclOnStack(fd, disp);
          nextArg++;
          continue;
        }

        if (nextArg >= MAX_ARG_REGS) { // Argument is on the stack.
          StackDisplacement disp = new StackDisplacement(offset);
          long              size = vt.memorySize(machine);

          defineDeclOnStack(fd, disp);
          offset += Machine.alignTo(size, AlphaRegisterSet.IREG_SIZE);
          entryOverflow.addElement(disp);
          continue;
        }

        if (fd.addressTaken() || volat) { // Argument value will be transferred to the stack.
          StackDisplacement disp = new StackDisplacement(localVarSize);
          long              size = vt.memorySize(machine);

          defineDeclOnStack(fd, disp);
          localVarSize += Machine.alignTo(size, AlphaRegisterSet.IREG_SIZE);
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

      assert vt.isAggregateType() : "Parameter type " + fd;

      // The first six words of a struct are in the argument registers, 
      // remaining words have already been placed on the stack by the caller.

      AggregateType at   = (AggregateType) vt;
      int           ts   = at.memorySizeAsInt(machine);

      if (usesVaStart) { // Argument must be saved on the stack.
        int loc = ((MAX_ARG_REGS * AlphaRegisterSet.FREG_SIZE) +
                   (nextArg * AlphaRegisterSet.IREG_SIZE));

        if (vt.isRealType())
          loc = nextArg * AlphaRegisterSet.FREG_SIZE;

        Displacement disp = argDisp.offset(loc);

        defineDeclOnStack(fd, disp);
        nextArg += ((ts + AlphaRegisterSet.IREG_SIZE - 1) / AlphaRegisterSet.IREG_SIZE);
        continue;
      }

      int inc = (ts + AlphaRegisterSet.IREG_SIZE - 1) / AlphaRegisterSet.IREG_SIZE;
      if (!useMemory &&
          !fd.addressTaken() &&
          ((nextArg + inc) <= MAX_ARG_REGS) &&
          machine.keepTypeInRegister(at, true)) {
        int ft   = at.allFieldsType();
        int rft  = (ft == AggregateType.FT_INT) ? RegisterSet.INTREG : RegisterSet.FLTREG;
        int regd = registers.newTempRegister(rft + ((ts > 8) ? RegisterSet.DBLEREG : 0));
        defineDeclInRegister(fd, regd, ResultMode.STRUCT_VALUE);
      } else {
        StackDisplacement disp = new StackDisplacement(offset);
        defineDeclOnStack(fd, disp);
        entryOverflow.addElement(disp);
        offset += inc * AlphaRegisterSet.IREG_SIZE;

        if (nextArg < MAX_ARG_REGS) {
          int s = (MAX_ARG_REGS - nextArg) * AlphaRegisterSet.IREG_SIZE;
          if (s > ts)
            s = ts;
          s = (((s + AlphaRegisterSet.IREG_SIZE - 1) / AlphaRegisterSet.IREG_SIZE) *
               AlphaRegisterSet.IREG_SIZE);
          entryOverflowSize += s;
        }
      }

      nextArg += inc;
    }
  }

  /**
   * Return the register used to return the function value.
   * @param regType specifies the type of value
   * @param isCall is true if the calling routine is asking
   */
  public int returnRegister(int regType, boolean isCall)
  {
    return registers.isFloatType(regType) ? AlphaRegisterSet.FR_REG : AlphaRegisterSet.IR_REG;
  }

  /**
   * Return the register used as the first argument in a function call.
   * @param regType specifies the type of argument value
   */
  public final int getFirstArgRegister(int regType)
  {
    return registers.isFloatType(regType) ? AlphaRegisterSet.FF_REG : AlphaRegisterSet.IF_REG;
  }

  private Displacement defFloatValue(double v, int size)
  {
    int section = LIT4;
    int type    = SpaceAllocation.DAT_FLT;
    int aln     = 4;

    if (size > 4) {
      section = LIT8;
      type = SpaceAllocation.DAT_DBL;
      aln = 8;
    }

    Displacement disp = findAreaDisp(section, type, true, size, v, aln);

    if (disp == null) {
      String name   = un.genName();
      int    handle = allocateData(name, section, type, size, true, new Double(v), 1, aln);
      disp   = new SymbolDisplacement(name, handle);
      associateDispWithArea(handle, disp);
    } else
      disp = disp.unique();

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
      int    handle = allocateData(name, section, type, size, true, new Long(v), 1, aln);
      disp   = new SymbolDisplacement(name, handle);
      associateDispWithArea(handle, disp);
    } else
      disp = disp.unique();

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
    } else
      disp = disp.unique();

    return disp;
  }

  /**
   * Create a new read-only data area whose value is a table of
   * displacements.
   */
  protected Displacement createAddressTable(Chord[] entries,
                                            long[]  indexes,
                                            int     min,
                                            int     max)
  {
    int     num = max + 1 - min;
    Label[] v   = new Label[num];
    Label   def = getBranchLabel(entries[entries.length - 1]);

    for (int i = 0; i < num; i++) {
      v[i] = def;
    }

    for (int i = 0; i < entries.length - 1; i++) {
      int in = (int) (indexes[i] - min);
      if (in >= 0)
        v[in] = getBranchLabel(entries[i]);
    }

    String       name   = un.genName();
    int          handle = allocateData(name,
                                       RDATA,
                                       SpaceAllocation.DAT_ADDRESS,
                                       4,
                                       true,
                                       v,
                                       1,
                                       8);
    Displacement disp   = new SymbolDisplacement(name, handle);
    associateDispWithArea(handle, disp);

    return disp;
  }

  /**
   * Return the stack offset to a temporary used to transfer values
   * between integer and floating point registers.
   */
  private Displacement getTransferOffset(int size)
  {
    return getDisp(-8 * ((size + 7) / 8));
  }

  /**
   * Generate instructions to move data from one register to another.
   * If one is an integer register and the other is a floating point
   * register, a memory location may be required.
   */
  protected void genRegToReg(int src, int dest)
  {
    assert (src >= 0) : "Negative source register " + src + " to " + dest;

    if (src == dest)
      return;

    if (registers.floatRegister(src)) {
      if (registers.floatRegister(dest))
        appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, src, dest));
      else if (fix) {
        appendInstruction(new FltOpInstruction(Opcodes.FTOIT, src, AlphaRegisterSet.F0_REG, dest));
      } else {
        Displacement to = getTransferOffset(AlphaRegisterSet.IREG_SIZE);
        appendInstruction(new StoreInstruction(Opcodes.STT, src, AlphaRegisterSet.SP_REG, to));
        appendInstruction(new LoadInstruction(Opcodes.LDQ, dest, AlphaRegisterSet.SP_REG, to));
      }
    } else if (registers.floatRegister(dest)) {
      if (fix) {
        appendInstruction(new FltOpInstruction(Opcodes.ITOFT,
                                               src,
                                               AlphaRegisterSet.F0_REG,
                                               dest));
      } else {
        Displacement to = getTransferOffset(AlphaRegisterSet.IREG_SIZE);
        appendInstruction(new StoreInstruction(Opcodes.STQ, src, AlphaRegisterSet.SP_REG, to));
        appendInstruction(new LoadInstruction(Opcodes.LDT, dest, AlphaRegisterSet.SP_REG, to));
      }
    } else
      appendInstruction(new IntOpLitInstruction(Opcodes.BIS, src, 0, dest));

    if (registers.pairRegister(src) || (registers.registerSize(src) > AlphaRegisterSet.IREG_SIZE))
      genRegToReg(src + 1, dest + 1);
  }

  /**
   *  Generate an add of address registers <code>laReg</code> and <code>raReg</code>.
   */
  protected void addRegs(int laReg, int raReg, int dest)
  {
    appendInstruction(new IntOpInstruction(Opcodes.ADDQ, laReg, raReg, dest));
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
    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          dest,
                                          AlphaRegisterSet.GP_REG,
                                          disp,
                                          RT_LITERAL));
    appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                 dest,
                                                 dest,
                                                 disp,
                                                 RT_LITUSE_BASE));
    usesGp = true;
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
    appendInstruction(new LoadAddressInstruction(Opcodes.LDA, dest, stkPtrReg, disp));
    appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, dest, dest, disp));
    return dest;
  }

  /**
   * Generate instructions to load an immediate integer value added to the 
   * value in a register into a register.
   * @param value is the value to load
   * @param base is the base register (e.g., AlphaRegisterSet.I0_REG)
   * @param dest is the register conataining the result
   */
  protected void genLoadImmediate(long value, int base, int dest)
  {
    if (value >= 0) {
      if (value < 256) {
        if (value == 0) {
          genRegToReg(base, dest);
          return;
        }
        if (base != AlphaRegisterSet.I0_REG)
          appendInstruction(new IntOpLitInstruction(Opcodes.ADDQ, base, (int) value, dest));
        else
          appendInstruction(new IntOpLitInstruction(Opcodes.BIS, base, (int) value, dest));
        return;
      }
    } else if ((value < 0) && (value > -256)) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, base, (int) -value, dest));
      return;
    }

    long    sign  = value >> 32;
    int     val   = (int) value;
    boolean upper = ((value & 0xffffffffL) == 0);
    if (!upper && (((sign == 0) && (val >= 0)) || ((sign == -1) && (val < 0)))) {
      int low   = (val & 0xffff);
      int tmp1  = val - ((low << 16) >> 16);
      int high  = (tmp1 >> 16) & 0xffff;
      int tmp2  = tmp1 - (((high << 16) >> 16) << 16);
      int extra = 0;

      if (tmp2 != 0) {
        extra = 0x4000;
        tmp1 -= 0x40000000;
        high = (tmp1 >> 16) & 0xffff;
      }

      long tot  = (((((long) high) << 48) >> 32) +
                   ((((long) low) << 48) >> 48) +
                   ((((long) extra) << 48) >> 32));
      int which = 0;
      if (low != 0)
        which |= 1;
      if (extra != 0)
        which |= 2;
      if (high != 0)
        which |= 4;

      boolean flg   = (tot != value);
      int     sbase = base;
      int     tr    = dest;
      if (flg) {
        base = AlphaRegisterSet.I0_REG;
        tr = registers.newTempRegister(RegisterSet.AIREG);
      }

      switch (which) {
      default:
        System.out.println(" value " + Long.toHexString(value));
        System.out.println(" sign  " + Long.toHexString(sign));
        System.out.println(" val   " + Integer.toHexString(val));
        System.out.println(" low   " + Integer.toHexString(low));
        System.out.println(" high   " + Integer.toHexString(high));
        System.out.println(" tmp2   " + Integer.toHexString(tmp2));
        System.out.println(" extra   " + Integer.toHexString(extra));
        throw new scale.common.InternalError("This can't happen!");
      case 1:
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,  tr, base, getDisp(low)));
        break;
      case 2:       
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr, base, getDisp(extra)));
        break;
      case 3:
        int tr3 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,  tr3, base, getDisp(low)));
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr, tr3, getDisp(extra)));
        break;
      case 4:
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr, base, getDisp(high)));
        break;
      case 5:
        int tr5 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,  tr5, base, getDisp(low)));
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr, tr5, getDisp(high)));
        break;
      case 6:
        int tr6 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr6, base, getDisp(extra)));
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr, tr6, getDisp(high)));
        break;
      case 7:
        int tr7 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,  tr7, base, getDisp(low)));
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr7, tr7, getDisp(extra)));
        appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr, tr7, getDisp(high)));
        break;
      }

      if (flg) {
        if (sbase != AlphaRegisterSet.I0_REG) {
          appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, tr, 15, tr));
          appendInstruction(new IntOpInstruction(Opcodes.ADDQ, tr, sbase, dest));
        } else {
          appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, tr, 15, dest));
        }
      }
      return;
    }

    if (upper) {
      int tr8 = registers.newTempRegister(RegisterSet.AIREG);
      genLoadImmediate(sign, base, tr8);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr8, 32, dest));
      return;
    }

    // Have to load a literal.
    // Generate instructions to load an immediate integer value into a register.

    int          adr  = registers.newTempRegister(RegisterSet.ADRREG);
    Displacement disp = defLongValue(value, 8);

    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          adr,
                                          AlphaRegisterSet.GP_REG,
                                          disp,
                                          RT_LITERAL));
    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          dest,
                                          adr,
                                          disp,
                                          RT_LITUSE_BASE));
    usesGp = true;

    if (base != AlphaRegisterSet.I0_REG) {
      assert (base != dest) : "genLoadImmediate - base == dest";
      appendInstruction(new IntOpInstruction(Opcodes.ADDQ, dest, base, dest));
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
      return AlphaRegisterSet.I0_REG;
    genLoadImmediate(value, AlphaRegisterSet.I0_REG, dest);
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
    if ((value == 0.0) && (Double.doubleToLongBits(value) == 0)) // Beware the dreaded -0.0.
      return AlphaRegisterSet.F0_REG;

    if (value == 2.0) {
      appendInstruction(new FltOpInstruction(Opcodes.CMPTEQ,
                                             AlphaRegisterSet.F0_REG,
                                             AlphaRegisterSet.F0_REG,
                                             dest));
      return dest;
    }

    Displacement disp = defFloatValue(value, destSize);
    int          adr  = registers.newTempRegister(RegisterSet.ADRREG);

    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          adr,
                                          AlphaRegisterSet.GP_REG,
                                          disp,
                                          RT_LITERAL));

    int op = destSize > 4 ? Opcodes.LDT : Opcodes.LDS;
    appendInstruction(new LoadInstruction(op, dest, adr, disp, RT_LITUSE_BASE));
    usesGp = true;

    return dest;
  }

  /**
   * Return the bottom 16 bits of the value.  Add the upper bits of
   * the value to base.  The resultReg field is set to the address
   * register to use.
   * @param base is the base register
   * @return the lower 16 bits of the constant
   */
  protected long genLoadHighImmediate(long value, int base)
  {
    int val = (int) value;
    assert (value == val) : "genLoadHighImmediate " + value;

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = base;

    if ((val >= 0) && (val < 256))
      return val;

    int   low   = val & 0xffff;
    int   x     = (low << 16) >> 16;
    int   tmp1  = val - x;
    int   high  = (tmp1 >> 16);
    int   y     = (high << 16) >> 16;
    int   tmp2  = tmp1 - (y << 16);
    int   extra = 0;

    if (tmp2 != 0) {
      extra = 0x4000;
      tmp1 -= 0x40000000;
      high = tmp1 >> 16;
    }

    int which = 0;
    if (low != 0)
      which |= 1;
    if (extra != 0)
      which |= 2;
    if (high != 0)
      which |= 4;

    if (which == 1)
      return x;

    int regb = registers.newTempRegister(RegisterSet.ADRREG);

    resultReg = regb;

    switch (which) {
    default: throw new scale.common.InternalError("This can't happen!");
    case 1:
      return x;
    case 2:       
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(extra)));
      return 0;
    case 3:
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(extra)));
      return x;
    case 4:
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(high)));
      return 0;
    case 5:
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(high)));
      return x;
    case 6:
    case 7:
      int tr6 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr6, base, getDisp(extra)));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, tr6, getDisp(high)));
      return x;
    } 
  }

  /**
   * Return the displacement remaining after adding the upper bits
   * of the displacement to the base register value.
   * The resultReg field is set to the address register to use.
   * @param base is the base register
   * @return the lower 16 bits of the constant
   */
  private Displacement genLoadHighImmediate(Displacement disp, int base)
  {
    long value = disp.getDisplacement();
    int  val   = (int) value;
    assert (value == val) : "genLoadHighImmediate " + value;

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = base;

    if ((val >= 0) && (val < 256))
      return disp;

    int   low   = val & 0xffff;
    int   x     = (low << 16) >> 16;
    int   tmp1  = val - x;
    int   high  = (tmp1 >> 16);
    int   y     = (high << 16) >> 16;
    int   tmp2  = tmp1 - (y << 16);
    int   extra = 0;

    if (tmp2 != 0) {
      extra = 0x4000;
      tmp1 -= 0x40000000;
      high = tmp1 >> 16;
    }

    int which = 0;
    if (low != 0)
      which |= 1;
    if (extra != 0)
      which |= 2;
    if (high != 0)
      which |= 4;

    if (which == 1)
      return disp;

    int regb = AlphaRegisterSet.AT_REG;

    resultReg = regb;

    switch (which) {
    default: throw new scale.common.InternalError("This can't happen!");
    case 1:
      return disp;
    case 2:       
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(extra)));
      return getDisp(x);
    case 3:
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(extra)));
      return getDisp(x);
    case 4:
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(high)));
      return getDisp(x);
    case 5:
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, base, getDisp(high)));
      return getDisp(x);
    case 6:
    case 7:
      int tr6 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, tr6, base, getDisp(extra)));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, regb, tr6, getDisp(high)));
      return getDisp(x);
    } 
  }

  /**
   * Called after the last CFG node in a basic block is processed.
   */
  protected void basicBlockEnd()
  {
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
    int               ts   = type.memorySizeAsInt(machine);
    StackDisplacement disp = new StackDisplacement(localVarSize);

    localVarSize += Machine.alignTo(ts, 8);
    localVar.addElement(disp); // This stack offset will be modified later.

    if (ts < 4) // A full word store is faster.
      ts = 4;

    appendInstruction(new LoadAddressInstruction(Opcodes.LDA, adrReg, stkPtrReg, disp));

    return ts;
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   */
  private void loadFromMemory(int          dest,
                              int          address,
                              Displacement disp,
                              int          relocType,
                              int          size,
                              long         alignment,
                              boolean      signed)
  {
    if (registers.pairRegister(dest)) {
      int s = size / 2;
      assert (s <= AlphaRegisterSet.IREG_SIZE) : "Paired register size " + size;
      loadFromMemoryX(dest + 0, address, disp,           relocType, s, alignment, signed);
      loadFromMemoryX(dest + 1, address, disp.offset(s), relocType, s, alignment, signed);
      return;
    }

    if (size <= AlphaRegisterSet.IREG_SIZE) {
      loadFromMemoryX(dest, address, disp, relocType, size, alignment, signed);
      return;
    }

    // Structs

    while (true) {
      int s = size;
      if (s > AlphaRegisterSet.IREG_SIZE)
        s = AlphaRegisterSet.IREG_SIZE;
      loadFromMemoryX(dest, address, disp, relocType, s, alignment, true);
      dest++;
      size -= s;
      if (size <= 0)
        break;
      disp = disp.offset(s);
    }
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   */
  private void loadFromMemoryX(int          dest,
                               int          address,
                               Displacement disp,
                               int          relocType,
                               int          size,
                               long         alignment,
                               boolean      signed)
  {
    boolean isFloat = registers.floatRegister(dest);

    int r2;
    int r3;

    switch (size) {
    case 1:
      if (bwx) {
        appendInstruction(new LoadInstruction(Opcodes.LDBU, dest, address, disp));
        if (signed)
          appendInstruction(new IntOpInstruction(Opcodes.SEXTB,
                                                 AlphaRegisterSet.I0_REG,
                                                 dest,
                                                 dest));
        return;
      }

      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDL, dest, address, disp, relocType));
        if (signed) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, 56, dest));
          appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, 56, dest));
        } else {
          appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, dest, 1, dest));
        }
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS, address, 0, r0));
        address = r0;
      }

      if (signed) {
        r3 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                              dest,
                                              address,
                                              disp,
                                              relocType));
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                     r3,
                                                     address,
                                                     disp.offset(1),
                                                     relocType));
        appendInstruction(new IntOpInstruction(Opcodes.EXTQH, dest, r3, dest));
        appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, 56, dest));
        return;
      }

      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, dest, address, disp, relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r3, address, disp, relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTBL, dest, r3, dest));
      return;

    case 2:
      if (bwx) {
        appendInstruction(new LoadInstruction(Opcodes.LDWU, dest, address, disp));
        if (signed)
          appendInstruction(new IntOpInstruction(Opcodes.SEXTW,
                                                 AlphaRegisterSet.I0_REG,
                                                 dest,
                                                 dest));
        return;
      }

      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDL, dest, address, disp, relocType));
        if (signed) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, 48, dest));
          appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, 48, dest));
        } else {
          appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, dest, 3, dest));
        }
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS, address, 0, r0));
        address = r0;
      }
      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            dest,
                                            address,
                                            disp,
                                            relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            r2,
                                            address,
                                            disp.offset(1),
                                            relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   r3,
                                                   address,
                                                   disp,
                                                   relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTWL,
                                             dest,
                                             r3,
                                             dest));
      appendInstruction(new IntOpInstruction(Opcodes.EXTWH,
                                             r2,
                                             r3,
                                             r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                             r2,
                                             dest,
                                             dest));
      if (signed) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL,
                                                  dest,
                                                  48,
                                                  dest));
        appendInstruction(new IntOpLitInstruction(Opcodes.SRA,
                                                  dest,
                                                  48,
                                                  dest));
      }

      return;

    case 3:
      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDL,
                                              dest,
                                              address,
                                              disp,
                                              relocType));
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS, address, 0, r0));
        address = r0;
      }
      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            dest,
                                            address,
                                            disp,
                                            relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            r2,
                                            address,
                                            disp.offset(2),
                                            relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   r3,
                                                   address,
                                                   disp,
                                                   relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTLL,
                                             dest,
                                             r3,
                                             dest));
      appendInstruction(new IntOpInstruction(Opcodes.EXTLH,
                                             r2,
                                             r3,
                                             r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                             r2,
                                             dest,
                                             dest));

      return;

    case 4:
      if ((alignment % 4) == 0) {
        if (isFloat) {
          appendInstruction(new LoadInstruction(Opcodes.LDS, dest, address, disp, relocType));
          return;
        }
        appendInstruction(new LoadInstruction(Opcodes.LDL, dest, address, disp, relocType));
        if (!signed)
          appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, dest, 0xf, dest));          
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS, address, 0, r0));
        address = r0;
      }

      int tdest = dest;
      if (isFloat)
        tdest = registers.newTempRegister(RegisterSet.AIREG);

      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            tdest,
                                            address,
                                            disp,
                                            relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            r2,
                                            address,
                                            disp.offset(3),
                                            relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   r3,
                                                   address,
                                                   disp,
                                                   relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTLL,
                                             tdest,
                                             r3,
                                             tdest));
      appendInstruction(new IntOpInstruction(Opcodes.EXTLH,
                                             r2,
                                             r3,
                                             r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                             r2,
                                             tdest,
                                             tdest));

      if (isFloat) {
        Displacement ta = getTransferOffset(4);
        appendInstruction(new StoreInstruction(Opcodes.STL,
                                               tdest,
                                               stkPtrReg,
                                               ta,
                                               RT_NONE));
        appendInstruction(new LoadInstruction(Opcodes.LDS,
                                              dest,
                                              stkPtrReg,
                                              ta,
                                              RT_NONE));
        return;
      }

      if (signed) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, 32, dest));
        appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, 32, dest));
      }
      return;

    case 5:
      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDQ, dest, address, disp, relocType));
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS, address, 0, r0));
        address = r0;
      }
      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            dest,
                                            address,
                                            disp,
                                            relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            r2,
                                            address,
                                            disp.offset(4),
                                            relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   r3,
                                                   address,
                                                   disp,
                                                   relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQL,
                                             dest,
                                             r3,
                                             dest));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQH,
                                             r2,
                                             r3,
                                             r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                             r2,
                                             dest,
                                             dest));

      return;

    case 6:
      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                              dest,
                                              address,
                                              disp,
                                              relocType));
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS, address, 0, r0));
        address = r0;
      }
      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            dest,
                                            address,
                                            disp,
                                            relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            r2,
                                            address,
                                            disp.offset(5),
                                            relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   r3,
                                                   address,
                                                   disp,
                                                   relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQL,
                                             dest,
                                             r3,
                                             dest));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQH,
                                             r2,
                                             r3,
                                             r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                             r2,
                                             dest,
                                             dest));

      return;

    case 7:
      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                              dest,
                                              address,
                                              disp,
                                              relocType));
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS,
                                                  address,
                                                  0,
                                                  r0));
        address = r0;
      }
      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            dest,
                                            address,
                                            disp,
                                            relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            r2,
                                            address,
                                            disp.offset(6),
                                            relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   r3,
                                                   address,
                                                   disp,
                                                   relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQL,
                                             dest,
                                             r3,
                                             dest));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQH,
                                             r2,
                                             r3,
                                             r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                             r2,
                                             dest,
                                             dest));

      return;

    case 8:
      if ((alignment % 8) == 0) {
        int op = isFloat ? Opcodes.LDT : Opcodes.LDQ;
        appendInstruction(new LoadInstruction(op,
                                              dest,
                                              address,
                                              disp,
                                              relocType));
        return;
      }

      if (address == dest) {
        int r0 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS,
                                                  address,
                                                  0,
                                                  r0));
        address = r0;
      }

      int tr2 = dest;
      if (isFloat)
        tr2 = registers.newTempRegister(RegisterSet.AIREG);

      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            tr2,
                                            address,
                                            disp,
                                            relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U,
                                            r2,
                                            address,
                                            disp.offset(7),
                                            relocType));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   r3,
                                                   address,
                                                   disp,
                                                   relocType));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQL,
                                             tr2,
                                             r3,
                                             tr2));
      appendInstruction(new IntOpInstruction(Opcodes.EXTQH,
                                             r2,
                                             r3,
                                             r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                             r2,
                                             tr2,
                                             tr2));

      if (!isFloat)
        return;

      Displacement ta = getTransferOffset(8);
      appendInstruction(new StoreInstruction(Opcodes.STQ,
                                             tr2,
                                             stkPtrReg,
                                             ta,
                                             RT_NONE));
      appendInstruction(new LoadInstruction(Opcodes.LDT,
                                            dest,
                                            stkPtrReg,
                                            ta,
                                            RT_NONE));
      return;
    }

    throw new scale.common.InternalError("Unknown data type size (" +
                                         size +
                                         ") " +
                                         registers.display(dest));
  }

  private void loadFromMemory(int          dest,
                              int          address,
                              Displacement disp,
                              int          size,
                              long         alignment,
                              boolean      signed)
  {
    loadFromMemory(dest, address, disp, RT_NONE, size, alignment, signed);
  }

  /**
   * Generate instructions to load data from memory at the address in
   * a register plus an offset.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset value - usually zero
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
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

    loadFromMemory(dest, resultReg, getDisp(off), RT_NONE, size, alignment, signed);

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
    loadFromMemory(dest, address, offset, RT_NONE, size, alignment, signed);
  }

  /**
   * Generate instructions to load data from memory at the address
   * that is the sum of the two index register values.
   * @param dest is the destination register
   * @param index1 is the register containing the first index
   * @param index2 is the register containing the second index
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
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
    appendInstruction(new IntOpInstruction(Opcodes.ADDQ, index1, index2, adr));
    loadFromMemory(dest, adr, getDisp(0), RT_NONE, size, alignment, signed);
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param bits specifies the size of the data to be loaded in bits -
   * must be 32 or less
   * @param bitOffset specifies the offset to the field in bits
   * @param alignment specifies the alignment of the address
   * @param signed is true if the data is to be sign extended
   */
  protected void loadBitsFromMemory(int     dest,
                                    int     address,
                                    int     bits,
                                    int     bitOffset,
                                    long    alignment,
                                    boolean signed)
  {
    Displacement disp = getDisp(0);
    int          span = bits + bitOffset;
    int          op   = signed ? Opcodes.SRA : Opcodes.SRL;

    if (((span <= 32) && (alignment >= 4)) || ((span <= 64) && (alignment >= 8))) {
      if (span <= 32)
        appendInstruction(new LoadInstruction(Opcodes.LDL, dest, address, disp, RT_NONE));
      else
        appendInstruction(new LoadInstruction(Opcodes.LDQ, dest, address, disp, RT_NONE));
      if ((bitOffset == 0) && !signed && (bits <= 8))
        appendInstruction(new IntOpLitInstruction(Opcodes.AND, dest, (1 << bits) - 1, dest));
      else {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, 64 - bitOffset - bits, dest));
        appendInstruction(new IntOpLitInstruction(op, dest, 64 - bits, dest));
      }
      return;
    }

    if (span <= 32) {
      int opcode = 0;

      if (span <= 8)
        opcode = Opcodes.EXTBL;
      else if (span <= 16)
        opcode = Opcodes.EXTWL;
      else if (span <= 32)
        opcode = Opcodes.EXTLL;
      else
        throw new scale.common.InternalError("Store - bits (" +
                                             bits +
                                             ") + bitOffset (" +
                                             bitOffset +
                                             ") <= 32");

      int     trm = dest;
      boolean fc  = !signed && ((bits == 8) || (bits == 16) || (bits == 32));
      if (!fc)
        trm = registers.newTempRegister(RegisterSet.INTREG);

      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, trm, address, disp, RT_NONE));
      appendInstruction(new IntOpInstruction(opcode, trm, address, trm));
      if (!fc) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, trm, 64 - span, trm));
        appendInstruction(new IntOpLitInstruction(op, trm, 64 - bits, dest));
      }
      return;
    }

    Displacement disp2 = disp.offset(4);
    int          trmh  = registers.newTempRegister(RegisterSet.INTREG);
    int          trml  = registers.newTempRegister(RegisterSet.INTREG);

    appendInstruction(new LoadInstruction(Opcodes.LDQ_U, trmh, address, disp, RT_NONE));
    appendInstruction(new LoadInstruction(Opcodes.LDQ_U, trml, address, disp2, RT_NONE));
    appendInstruction(new IntOpInstruction(Opcodes.EXTQH, trmh, address, trmh));
    appendInstruction(new IntOpInstruction(Opcodes.EXTQL, trml, address, trml));
    appendInstruction(new IntOpLitInstruction(Opcodes.SLL, trmh, 64 - span, trmh));
    appendInstruction(new IntOpLitInstruction(Opcodes.SLL, trml, 64 - span, trml));
    appendInstruction(new IntOpLitInstruction(op, trmh, 64 - bits, trmh));
    appendInstruction(new IntOpLitInstruction(op, trml, 64 - bits, trml));
    appendInstruction(new IntOpInstruction(Opcodes.BIS, trml, trmh, dest));
    return;
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   */
  private void storeIntoMemory(int          src,
                               int          address,
                               Displacement disp,
                               int          relocType,
                               int          size,
                               long         alignment)
  {
    if (registers.pairRegister(src)) {
      int s = size / 2;
      assert (s <= AlphaRegisterSet.IREG_SIZE) : "Paired register size " + size;
      storeIntoMemoryX(src + 0, address, disp,           relocType, s, alignment);
      storeIntoMemoryX(src + 1, address, disp.offset(s), relocType, s, alignment);
      return;
    }

    if (size <= AlphaRegisterSet.IREG_SIZE) {
      storeIntoMemoryX(src, address, disp, relocType, size, alignment);
      return;
    }

    // Structs

    while (true) {
      int s = size;
      if (s > AlphaRegisterSet.IREG_SIZE)
        s = AlphaRegisterSet.IREG_SIZE;
      storeIntoMemoryX(src, address, disp, relocType, s, alignment);
      src++;
      size -= s;
      if (size <= 0)
        break;
      disp = disp.offset(s);
    }
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   */
  private void storeIntoMemoryX(int          src,
                                int          address,
                                Displacement disp,
                                int          relocType,
                                int          size,
                                long         alignment)
  {
    boolean isFloat = registers.floatRegister(src);

    int r1;
    int r2;
    int r3;
    int r4;
    int r6;
    int r7;
    int r8;

    switch (size) {
    case 1:
      if (bwx) {
        appendInstruction(new StoreInstruction(Opcodes.STB, src, address, disp, relocType));
        return;
      }

      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);

      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDL, r1, address, disp, relocType));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAP, r1, 1, r1));
        appendInstruction(new IntOpLitInstruction(Opcodes.INSBL, src, 0, r3));
        appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
        appendInstruction(new StoreInstruction(Opcodes.STL, r1, address, disp, relocType));
        return;
      }

      r6 = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpInstruction(Opcodes.INSBL, src, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.MSKBL, r1, r6, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));

      return;

    case 2:
      if (bwx) {
        appendInstruction(new StoreInstruction(Opcodes.STW, src, address, disp));
        return;
      }

      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);

      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDL, r2, address, disp, relocType));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAP, r2, 3, r2));
        appendInstruction(new IntOpLitInstruction(Opcodes.INSWL, src, 0, r3));
        appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r3, r2));
        appendInstruction(new StoreInstruction(Opcodes.STL, r2, address, disp, relocType));
        return;
      }

      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r4 = registers.newTempRegister(RegisterSet.AIREG);
      r6 = registers.newTempRegister(RegisterSet.AIREG);
      Displacement disp1 = disp.offset(1);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r2, address, disp1, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpInstruction(Opcodes.INSWH, src, r6, r4));
      appendInstruction(new IntOpInstruction(Opcodes.INSWL, src, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.MSKWH, r2, r6, r2));
      appendInstruction(new IntOpInstruction(Opcodes.MSKWL, r1, r6, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r4, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r2, address, disp1, relocType));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));

      return;

    case 3:
      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r2 = registers.newTempRegister(RegisterSet.AIREG);

      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDL, r1, address, disp, relocType));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAP, r1, 0x7, r1));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x7, r2));
        appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r2, r1));
        appendInstruction(new StoreInstruction(Opcodes.STL, r1, address, disp, relocType));
        return;
      }

      r3 = registers.newTempRegister(RegisterSet.AIREG);
      r4 = registers.newTempRegister(RegisterSet.AIREG);
      r6 = registers.newTempRegister(RegisterSet.AIREG);
      r7 = registers.newTempRegister(RegisterSet.AIREG);
      r8 = registers.newTempRegister(RegisterSet.AIREG);
      Displacement disp2 = disp.offset(2);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r2, address, disp2, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, 1, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, r7, 0x7, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x7, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSLH, r3, r6, r4));
      appendInstruction(new IntOpInstruction(Opcodes.INSLL, r3, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSLH, r7, r6, r8));
      appendInstruction(new IntOpInstruction(Opcodes.INSLL, r7, r6, r7));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r4, r8, r4));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r3, r7, r3));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r2, r8, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r1, r7, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r4, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r2, address, disp2, relocType));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));

      return;

    case 4:
      if ((alignment % 4) == 0) {
        int op = isFloat ? Opcodes.STS : Opcodes.STL;
        appendInstruction(new StoreInstruction(op, src, address, disp, relocType));
        return;
      }

      if (isFloat) {
        Displacement ta  = getTransferOffset(4);
        int          tr1 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new StoreInstruction(Opcodes.STS, src, stkPtrReg, ta, RT_NONE));
        appendInstruction(new LoadInstruction(Opcodes.LDL, tr1, stkPtrReg, ta, RT_NONE));
        src = tr1;
      }

      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      r4 = registers.newTempRegister(RegisterSet.AIREG);
      r6 = registers.newTempRegister(RegisterSet.AIREG);
      Displacement disp3 = disp.offset(3);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r2, address, disp3, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpInstruction(Opcodes.INSLH, src, r6, r4));
      appendInstruction(new IntOpInstruction(Opcodes.INSLL, src, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.MSKLH, r2, r6, r2));
      appendInstruction(new IntOpInstruction(Opcodes.MSKLL, r1, r6, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r4, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r2, address, disp3, relocType));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));

      return;

    case 5:
      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r2 = registers.newTempRegister(RegisterSet.AIREG);

      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDQ, r1, address, disp, relocType));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAP, r1, 0x1f, r1));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x1f, r2));
        appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r2, r1));
        appendInstruction(new StoreInstruction(Opcodes.STQ, r1, address, disp, relocType));
        return;
      }

      r3 = registers.newTempRegister(RegisterSet.AIREG);
      r4 = registers.newTempRegister(RegisterSet.AIREG);
      r6 = registers.newTempRegister(RegisterSet.AIREG);
      r7 = registers.newTempRegister(RegisterSet.AIREG);
      r8 = registers.newTempRegister(RegisterSet.AIREG);
      Displacement disp4 = disp.offset(4);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r2, address, disp4, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, 1, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, r7, 0x1f, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x1f, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSQH, r3, r6, r4));
      appendInstruction(new IntOpInstruction(Opcodes.INSQL, r3, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSQH, r7, r6, r8));
      appendInstruction(new IntOpInstruction(Opcodes.INSQL, r7, r6, r7));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r4, r8, r4));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r3, r7, r3));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r2, r8, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r1, r7, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r4, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r2, address, disp4, relocType));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));

      return;

    case 6:
      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r2 = registers.newTempRegister(RegisterSet.AIREG);

      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDQ, r1, address, disp, relocType));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAP, r1, 0x3f, r1));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x3f, r2));
        appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r2, r1));
        appendInstruction(new StoreInstruction(Opcodes.STQ, r1, address, disp, relocType));
        return;
      }

      r3 = registers.newTempRegister(RegisterSet.AIREG);
      r4 = registers.newTempRegister(RegisterSet.AIREG);
      r6 = registers.newTempRegister(RegisterSet.AIREG);
      r7 = registers.newTempRegister(RegisterSet.AIREG);
      r8 = registers.newTempRegister(RegisterSet.AIREG);
      Displacement disp5 = disp.offset(5);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r2, address, disp5, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, 1, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, r7, 0x3f, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x3f, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSQH, r3, r6, r4));
      appendInstruction(new IntOpInstruction(Opcodes.INSQL, r3, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSQH, r7, r6, r8));
      appendInstruction(new IntOpInstruction(Opcodes.INSQL, r7, r6, r7));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r4, r8, r4));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r3, r7, r3));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r2, r8, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r1, r7, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r4, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r2, address, disp5, relocType));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));

      return;

    case 7:
      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r2 = registers.newTempRegister(RegisterSet.AIREG);

      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LDQ, r1, address, disp, relocType));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAP, r1, 0x7f, r1));
        appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x7f, r2));
        appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r2, r1));
        appendInstruction(new StoreInstruction(Opcodes.STQ, r1, address, disp, relocType));
        return;
      }

      r3 = registers.newTempRegister(RegisterSet.AIREG);
      r4 = registers.newTempRegister(RegisterSet.AIREG);
      r6 = registers.newTempRegister(RegisterSet.AIREG);
      r7 = registers.newTempRegister(RegisterSet.AIREG);
      r8 = registers.newTempRegister(RegisterSet.AIREG);
      Displacement disp6 = disp.offset(6);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r2, address, disp6, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, 1, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, r7, 0x7f, r7));
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 0x7f, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSQH, r3, r6, r4));
      appendInstruction(new IntOpInstruction(Opcodes.INSQL, r3, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.INSQH, r7, r6, r8));
      appendInstruction(new IntOpInstruction(Opcodes.INSQL, r7, r6, r7));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r4, r8, r4));
      appendInstruction(new IntOpInstruction(Opcodes.AND, r3, r7, r3));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r2, r8, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, r1, r7, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r4, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r2, address, disp6, relocType));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));

      return;

    case 8:
      if ((alignment % 8) == 0) {
        int op = isFloat ? Opcodes.STT : Opcodes.STQ;
        appendInstruction(new StoreInstruction(op, src, address, disp, relocType));
        return;
      }

      if (isFloat) {
        Displacement ta  = getTransferOffset(8);
        int          tr1 = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new StoreInstruction(Opcodes.STT, src, stkPtrReg, ta, RT_NONE));
        appendInstruction(new LoadInstruction(Opcodes.LDQ, tr1, stkPtrReg, ta, RT_NONE));
        src = tr1;
      }

      r1 = registers.newTempRegister(RegisterSet.AIREG);
      r2 = registers.newTempRegister(RegisterSet.AIREG);
      r3 = registers.newTempRegister(RegisterSet.AIREG);
      r4 = registers.newTempRegister(RegisterSet.AIREG);
      r6 = registers.newTempRegister(RegisterSet.AIREG);
      Displacement disp7 = disp.offset(7);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, r6, address, disp, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r2, address, disp7, relocType));
      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, r1, address, disp, relocType));
      appendInstruction(new IntOpInstruction(Opcodes.INSQH, src, r6, r4));
      appendInstruction(new IntOpInstruction(Opcodes.INSQL, src, r6, r3));
      appendInstruction(new IntOpInstruction(Opcodes.MSKQH, r2, r6, r2));
      appendInstruction(new IntOpInstruction(Opcodes.MSKQL, r1, r6, r1));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r2, r4, r2));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, r1, r3, r1));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r2, address, disp7, relocType));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, r1, address, disp, relocType));
      return;
    }

    throw new scale.common.InternalError("Unknown data type size (" + size + ")");
  }

  private void storeIntoMemory(int src, int address, Displacement disp, int size, long alignment)
  {
    storeIntoMemory(src, address, disp, RT_NONE, size, alignment);
  }

  /**
   * Generate instructions to store data into memory at the address
   * specified by a register.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected void storeIntoMemory(int src, int address, int size, long alignment, boolean real)
  {
    storeIntoMemory(src, address, getDisp(0), RT_NONE, size, alignment);
  }

  /**
   * Generate instructions to store data into memory at the address in
   * a register plus an offset.
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected  void storeIntoMemoryWithOffset(int     src,
                                            int     address,
                                            long    offset,
                                            int     size,
                                            long    alignment,
                                            boolean real)
  {
    int        srr   = resultReg;
    ResultMode srrha = resultRegMode;
    long       srrof = resultRegAddressOffset;
    int        off   = (int) genLoadHighImmediate(offset, address);

    storeIntoMemory(src, resultReg, getDisp(off), RT_NONE, size, alignment);

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
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
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

    storeIntoMemory(src, address, offset, RT_NONE, size, alignment);
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param bits specifies the size of the data in bits to be loaded - must be 32 or less
   * @param bitOffset specifies the offset in bits to the data
   * @param alignment specifies the alignment of the address
   */
  protected void storeBitsIntoMemory(int  src,
                                     int  address,
                                     int  bits,
                                     int  bitOffset,
                                     long alignment)
  {
    int          span  = bits + bitOffset;
    int          shift = bitOffset;
    long         mk    = (1L << bits) - 1;
    long         mask  = (mk << shift);
    Displacement disp  = getDisp(0);

    if (((span <= 32) && (alignment >= 4)) || ((span <= 64) && (alignment >= 8))) {
      int trs  = registers.newTempRegister(RegisterSet.INTREG);
      int trd  = registers.newTempRegister(RegisterSet.INTREG);

      if (span <= 32)
        appendInstruction(new LoadInstruction(Opcodes.LDL, trd, address, disp, RT_NONE));
      else
        appendInstruction(new LoadInstruction(Opcodes.LDQ, trd, address, disp, RT_NONE));

      if ((mask >= 0) && (mask <= 255)) {
        if (shift > 0) {
          if (src != AlphaRegisterSet.I0_REG) {
            appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, shift, trs));
            appendInstruction(new IntOpLitInstruction(Opcodes.AND, trs, (int) mask, trs));
          } else
            trs = src;
        } else if (src != AlphaRegisterSet.I0_REG)
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, (int) mask, trs));
        else
          trs = src;
        appendInstruction(new IntOpLitInstruction(Opcodes.BIC, trd, (int) mask, trd));
      } else {
        int trm = registers.newTempRegister(RegisterSet.INTREG);
        if ((mask >= 0) && (mask <= MAX_IMM16))
          trm = genLoadImmediate(mask, trm);
        else {
          int tmp = registers.newTempRegister(RegisterSet.INTREG);
          if (bits < 16)
            appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                         tmp,
                                                         AlphaRegisterSet.I0_REG,
                                                         getDisp((1 << bits) - 1)));
          else {
            appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ,
                                                      AlphaRegisterSet.I0_REG,
                                                      1,
                                                      tmp));
            appendInstruction(new IntOpLitInstruction(Opcodes.SRL,
                                                      tmp,
                                                      64 - bits,
                                                      tmp));
          }
          if (shift > 0)
            appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tmp, shift, trm));
          else
            trm = tmp;
        }
        if (shift > 0) {
          if (src != AlphaRegisterSet.I0_REG) {
            appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, shift, trs));
            appendInstruction(new IntOpInstruction(Opcodes.AND, trs, trm, trs));
          } else
            trs = src;
        } else if (src != AlphaRegisterSet.I0_REG)
          appendInstruction(new IntOpInstruction(Opcodes.AND, src, trm, trs));
        else
          trs = src;
        appendInstruction(new IntOpInstruction(Opcodes.BIC, trd, trm, trd));
      }
      if (trs != AlphaRegisterSet.I0_REG)
        appendInstruction(new IntOpInstruction(Opcodes.BIS, trs, trd, trd));
      if (span <= 32)
        appendInstruction(new StoreInstruction(Opcodes.STL, trd, address, disp, RT_NONE));
      else
        appendInstruction(new StoreInstruction(Opcodes.STQ, trd, address, disp, RT_NONE));
      return;
    }

    if (span <= 32) {
      int trs    = registers.newTempRegister(RegisterSet.INTREG);
      int trd    = registers.newTempRegister(RegisterSet.INTREG);
      int trm    = registers.newTempRegister(RegisterSet.INTREG);
      int opcode = 0;

      if (span <= 8)
        opcode = Opcodes.INSBL;
      else if (span <= 16)
        opcode = Opcodes.INSWL;
      else if (span <= 32)
        opcode = Opcodes.INSLL;
      else
        throw new scale.common.InternalError("Store - bits ("
                                             + bits +
                                             ") + bitOffset (" +
                                             bitOffset +
                                             ") <= 32");

      appendInstruction(new LoadInstruction(Opcodes.LDQ_U, trd, address, disp, RT_NONE));

      if ((mask >= 0) && (mask <= 0x7fffffff))
        trm = genLoadImmediate(mask, trm);
      else {
        int tmp = genLoadImmediate((1L << bits) - 1, trm);
        if (shift > 0)
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tmp, shift, trm));
        else
          trm = tmp;
      }

      appendInstruction(new IntOpInstruction(opcode, trm, address, trm));
      appendInstruction(new IntOpInstruction(Opcodes.BIC, trd, trm, trd));
      if (shift > 0) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, shift, trs));
        src = trs;
      }
      appendInstruction(new IntOpInstruction(opcode, src, address, trs));
      appendInstruction(new IntOpInstruction(Opcodes.BIS, trs, trd, trs));
      appendInstruction(new StoreInstruction(Opcodes.STQ_U, trs, address, disp, RT_NONE));
      return;
    }

    int          trsh  = registers.newTempRegister(RegisterSet.INTREG);
    int          trsl  = registers.newTempRegister(RegisterSet.INTREG);
    int          trmh  = registers.newTempRegister(RegisterSet.INTREG);
    int          trml  = registers.newTempRegister(RegisterSet.INTREG);
    int          srcl  = registers.newTempRegister(RegisterSet.INTREG);
    int          srch  = registers.newTempRegister(RegisterSet.INTREG);
    Displacement disp2 = disp.offset(4);

    if ((mask >= 0) && (mask <= 0x7fffffff))
      trml = genLoadImmediate(mask, trml);
    else {
      int tmp = genLoadImmediate((1L << bits) - 1, trml);
      if (shift > 0)
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tmp, shift, trml));
      else
        trml = tmp;
    }
    appendInstruction(new LoadInstruction(Opcodes.LDQ_U, trsh, address, disp, RT_NONE));
    appendInstruction(new LoadInstruction(Opcodes.LDQ_U, trsl, address, disp2, RT_NONE));
    if (shift > 0) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, shift, srcl));
      src = srcl;
    }

    appendInstruction(new IntOpInstruction(Opcodes.INSQH, trml, address, trmh));
    appendInstruction(new IntOpInstruction(Opcodes.INSQL, trml, address, trml));
    appendInstruction(new IntOpInstruction(Opcodes.INSQH, src, address, srch));
    appendInstruction(new IntOpInstruction(Opcodes.INSQL, src, address, srcl));
    appendInstruction(new IntOpInstruction(Opcodes.BIC, trsh, trmh, trsh));
    appendInstruction(new IntOpInstruction(Opcodes.BIC, trsl, trml, trsl));
    appendInstruction(new IntOpInstruction(Opcodes.BIS, trsh, srch, trsh));
    appendInstruction(new IntOpInstruction(Opcodes.BIS, trsl, srcl, trsl));
    appendInstruction(new StoreInstruction(Opcodes.STQ_U, trsh, address, disp, RT_NONE));
    appendInstruction(new StoreInstruction(Opcodes.STQ_U, trsl, address, disp2, RT_NONE));
  }

  /**
   * Generate an instruction sequence to move words from one location to another.
   * The destination offset must not be symbolic.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move
   * @param aln is the alignment that can be assumed for both the
   * source and destination addresses
   */
  protected void moveWords(int src, long srcoff, int dest, Displacement destoff, int size, int aln)
  {
    assert destoff.isNumeric() : "Symbolic displacement " + destoff;

    if (destoff.isStack()) {
      int adr = registers.newTempRegister(RegisterSet.ADRREG);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, adr, dest, destoff));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, adr, adr, destoff));
      moveWords(src, srcoff, adr, 0, size, aln);
      return;
    }

    moveWords(src, srcoff, dest, destoff.getDisplacement(), size, aln);
  }

  /**
   * Generate an instruction sequence to move words from one location to another.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move.
   * @param aln is the alignment that can be assumead for both the
   * source and destination addresses
   */
  protected void moveWords(int src, long srcoff, int dest, long destoff, int size, int aln)
  {
    if (((aln & 0x3) != 0) || ((size & 0x3) != 0)) {
      short[] uses = new short[usesAlloca ? 6 : 5];
      uses[0] = AlphaRegisterSet.SP_REG;
      uses[1] = AlphaRegisterSet.GP_REG;
      uses[2] = AlphaRegisterSet.IF_REG;
      uses[3] = AlphaRegisterSet.IF_REG + 1;
      uses[4] = AlphaRegisterSet.IF_REG + 2;
      if (usesAlloca)
        uses[5] = AlphaRegisterSet.FP_REG;

      genLoadImmediate(destoff, dest, AlphaRegisterSet.IF_REG);
      genLoadImmediate(size, AlphaRegisterSet.I0_REG, AlphaRegisterSet.IF_REG + 1);
      genLoadImmediate(srcoff, src, AlphaRegisterSet.IF_REG + 2);
      genFtnCall("_OtsMove", uses, null);
      return;
    }

    int ms  = AlphaRegisterSet.IREG_SIZE;
    int lop = Opcodes.LDQ;
    int sop = Opcodes.STQ;

    if (((aln & 0x7) != 0) || (size & 0x7) != 0) {
      lop = Opcodes.LDL;
      sop = Opcodes.STL;
      ms = 4;
    }

    if (size <= (ms * 5)) { // Generate straight-line load-stores to move data.
      int soffset = 0;
      if ((srcoff >= (MAX_IMM16 - size)) || (srcoff <= (MIN_IMM16 + size))) {
        int sr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(srcoff, src, sr);
        src = sr;
      } else
         soffset = (int) srcoff;

      int nexsr = 0;
      for (int k = 0; k < size; k += ms) { // Generate loads.
        Displacement odisp = getDisp(soffset);
        nxtMvReg[nexsr] = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadInstruction(lop, nxtMvReg[nexsr], src, odisp));
        lastInstruction.specifyNotSpillLoadPoint();
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
      for (int k = 0; k < size; k += ms) { // Generate stores.
        Displacement odisp = getDisp(doffset);
        appendInstruction(new StoreInstruction(sop, nxtMvReg[nexdr], dest, odisp));
        lastInstruction.specifyNotSpillLoadPoint();
        doffset += ms;
        nexdr++;
      }

      lastInstruction.specifySpillStorePoint();
      return;
    }

    // Generate loop to move data.

    int               valreg  = registers.newTempRegister(RegisterSet.AIREG);
    int               testreg = registers.newTempRegister(RegisterSet.AIREG);
    int               tsrc    = registers.newTempRegister(RegisterSet.AIREG);
    int               tdest   = registers.newTempRegister(RegisterSet.AIREG);
    Label             labl    = createLabel();
    Label             labn    = createLabel();
    Displacement      odisp   = getDisp(0);
    LabelDisplacement ldisp   = new LabelDisplacement(labl);
    Displacement      ddisp   = getDisp(ms);
    Branch            inst    = new BranchInstruction(Opcodes.BGT, testreg, ldisp, 2);

    inst.addTarget(labl, 0);
    inst.addTarget(labn, 1);

    genLoadImmediate(srcoff, src, tsrc);
    genLoadImmediate(destoff, dest, tdest);
    appendInstruction(new LoadInstruction(lop, valreg, tsrc, odisp));
    genLoadImmediate(size - ms, AlphaRegisterSet.I0_REG, testreg);
    appendLabel(labl);
    appendInstruction(new LoadAddressInstruction(Opcodes.LDA, tsrc, tsrc, ddisp));
    lastInstruction.specifyNotSpillLoadPoint();
    appendInstruction(new StoreInstruction(sop, valreg, tdest, odisp));
    lastInstruction.specifyNotSpillLoadPoint();
    appendInstruction(new LoadAddressInstruction(Opcodes.LDA, tdest, tdest, ddisp));
    lastInstruction.specifyNotSpillLoadPoint();
    appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, testreg, ms, testreg));
    lastInstruction.specifyNotSpillLoadPoint();
    appendInstruction(new LoadInstruction(lop, valreg, tsrc, odisp));
    lastInstruction.specifyNotSpillLoadPoint();
    appendInstruction(inst);
    lastInstruction.specifyNotSpillLoadPoint();
    appendLabel(labn);
    labn.setNotReferenced();
    appendInstruction(new StoreInstruction(sop, valreg, tdest, odisp));
    lastInstruction.specifyNotSpillLoadPoint();
    lastInstruction.specifySpillStorePoint();
  }

  /**
   * Load a register from a symbolic location in memory.
   * @param dest is the register
   * @param dsize is the size of the value in addressable memory units
   * @param isSigned is true if the value in the register is signed
   * @param isReal is true if the value in the register is a floating point value
   * @param disp specifies the location
   */
  protected void loadRegFromSymbolicLocation(int          dest,
                                             int          dsize,
                                             boolean      isSigned,
                                             boolean      isReal,
                                             Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

    int adr = registers.newTempRegister(RegisterSet.AIREG);

    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          adr,
                                          AlphaRegisterSet.GP_REG,
                                          disp,
                                          RT_LITERAL));
    loadFromMemory(dest, adr, disp, RT_LITUSE_BASE, dsize, 8, isSigned);
    usesGp = true;
  }

  private int multNeedsTemp(int src, int dest)
  {
    if (src != dest)
      return dest;
    return registers.newTempRegister(registers.getType(dest));
  }

  private boolean multWithAdds(long value, int src, int dest, boolean quad)
  {
    int add  = quad ? Opcodes.ADDQ   : Opcodes.ADDL;
    int sub  = quad ? Opcodes.SUBQ   : Opcodes.SUBL;
    int add4 = quad ? Opcodes.S4ADDQ : Opcodes.S4ADDL;
    int sub4 = quad ? Opcodes.S4SUBQ : Opcodes.S4SUBL;
    int add8 = quad ? Opcodes.S8ADDQ : Opcodes.S8ADDL;
    int sub8 = quad ? Opcodes.S8SUBQ : Opcodes.S8SUBL;
    int zero = AlphaRegisterSet.I0_REG;

    int temp;
    switch ((int) value) {
    case -1:
      appendInstruction(new IntOpInstruction(sub, zero, src, dest));
      return true;
    case 0:
      appendInstruction(new IntOpInstruction(Opcodes.BIS, zero, zero, dest));
      return true;
    case 1:
      appendInstruction(new IntOpInstruction(add, src, zero, dest));
      return true;
    case 2:
      appendInstruction(new IntOpInstruction(add, src, src, dest));
      return true;
    case 3:
      appendInstruction(new IntOpInstruction(sub4, src, src, dest));
      return true;
    case 4:
      appendInstruction(new IntOpInstruction(add4, src, zero, dest));
      return true;
    case 5:
      appendInstruction(new IntOpInstruction(add4, src, src, dest));
      return true;
    case 6:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add, src, src, temp));
      appendInstruction(new IntOpInstruction(add4, src, temp, dest));
      return true;
    case 7:
      appendInstruction(new IntOpInstruction(sub8, src, src, dest));
      return true;
    case 8:
      appendInstruction(new IntOpInstruction(add8, src, zero, dest));
      return true;
    case 9:
      appendInstruction(new IntOpInstruction(add8, src, src, dest));
      return true;
    case 10:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, src, temp, dest));
      return true;
    case 11:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub4, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, src, temp, dest));
      return true;
    case 12:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, zero, temp));
      appendInstruction(new IntOpInstruction(add8, src, temp, dest));
      return true;
    case 13:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, src, temp, dest));
      return true;
    case 14:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(add, temp, temp, dest));
      return true;
    case 15:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, src, temp, dest));
      return true;
    case 16:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, zero, temp));
      appendInstruction(new IntOpInstruction(add8, src, temp, dest));
      return true;
    case 17:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, src, temp, dest));
      return true;
    case 18:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, src, temp));
      appendInstruction(new IntOpInstruction(add, temp, temp, dest));
      return true;
    case 19:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(sub4, temp, src, dest));
      return true;
    case 20:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(add4, temp, zero, dest));
      return true;
    case 21:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(add4, temp, src, dest));
      return true;
    case 23:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub4, src, src, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, src, dest));
      return true;
    case 24:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub4, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, zero, dest));
      return true;
    case 25:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(add4, temp, temp, dest));
      return true;
    case 27:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub4, src, src, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, temp, dest));
      return true;
    case 28:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, zero, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, temp, dest));
      return true;
    case 29:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(add4, temp, src, dest));
      return true;
    case 31:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, zero, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, src, dest));
      return true;
    case 33:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, zero, temp));
      appendInstruction(new IntOpInstruction(add8, temp, src, dest));
      return true;
    case 35:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, temp, dest));
      return true;
    case 36:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, zero, temp));
      appendInstruction(new IntOpInstruction(add8, temp, temp, dest));
      return true;
    case 37:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, src, temp));
      appendInstruction(new IntOpInstruction(add4, temp, src, dest));
      return true;
    case 39:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, src, dest));
      return true;
    case 40:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, zero, dest));
      return true;
    case 45:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add4, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, temp, dest));
      return true;
    case 49:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, temp, dest));
      return true;
    case 55:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, src, dest));
      return true;
    case 56:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, zero, dest));
      return true;
    case 57:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, src, dest));
      return true;
    case 63:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(sub8, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, temp, dest));
      return true;
    case 65:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, zero, temp));
      appendInstruction(new IntOpInstruction(add8, temp, src, dest));
      return true;
    case 71:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, src, temp));
      appendInstruction(new IntOpInstruction(sub8, temp, src, dest));
      return true;
    case 72:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, zero, temp));
      appendInstruction(new IntOpInstruction(add8, temp, temp, dest));
      return true;
    case 73:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, src, dest));
      return true;
    case 81:
      temp = multNeedsTemp(src, dest);
      appendInstruction(new IntOpInstruction(add8, src, src, temp));
      appendInstruction(new IntOpInstruction(add8, temp, temp, dest));
      return true;
    }
    return false;
  }

  private void genMultiplyQuad(long value, int src, int dest)
  {
    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // It is a multiply by a power of 2.
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, shift, dest));
      return;
    }

    if (multWithAdds(value, src, dest, true))
      return;

    if ((value >= 0) && (value < 256)) {
      appendInstruction(new IntOpLitInstruction(Opcodes.MULQ, src, (int) value, dest));
      return;
    }

    int tr2 = registers.newTempRegister(RegisterSet.INTREG);
    tr2 = genLoadImmediate(value, tr2);
    appendInstruction(new IntOpInstruction(Opcodes.MULQ, src, tr2, dest));
  }

  private void genMultiplyLong(long value, int src, int dest)
  {
    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // It is a multiply by a power of 2.
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, shift, dest));
      return;
    }

    if (multWithAdds(value, src, dest, false))
      return;

    if ((value >= 0) && (value < 256)) {
      appendInstruction(new IntOpLitInstruction(Opcodes.MULL, src, (int) value, dest));
      return;
    }

    int tr2 = registers.newTempRegister(RegisterSet.INTREG);
    tr2 = genLoadImmediate(value, tr2);
    appendInstruction(new IntOpInstruction(Opcodes.MULL, src, tr2, dest));
  }

  private void dividePower2(long    value,
                            int     shift,
                            int     src,
                            int     dest,
                            boolean quad,
                            boolean signed)
  {
    if (shift == 0) { // Dividing by the value 1.
      appendInstruction(new IntOpLitInstruction(Opcodes.BIS, src, 0, dest));
      return;
    }

    if (signed) { // Divide a signed integer.
      int tr = registers.newTempRegister(RegisterSet.INTREG);
      int op = quad ? Opcodes.ADDQ : Opcodes.ADDL;
      appendInstruction(new IntOpLitInstruction(op, src, (int) (value - 1), tr));
      appendInstruction(new IntOpInstruction(Opcodes.CMOVGE, src, src, tr));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, tr, shift, dest));
      return;
    }

    // Divide an unsigned integer.

    appendInstruction(new IntOpLitInstruction(Opcodes.SRL, src, shift, dest));
  }

  private void genDivide(long value, int src, int dest, boolean quad, boolean signed)
  {
    if ((value > 0) && (value < 0x4000000)) {
      int shift = Lattice.powerOf2(value);
      if (shift >= 0) { // It is a divide by a power of 2.
        dividePower2(value, shift, src, dest, quad, signed);
        return;
      }
    }

    boolean neg = (value < 0);
    if (neg)
      value = -value;

    if (value == 1) {
      if (neg)
        appendInstruction(new IntOpInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, src, dest));
      else
        appendInstruction(new IntOpInstruction(Opcodes.BIS, src, AlphaRegisterSet.I0_REG, dest));
      return;
    }

    // Divide by multiplying the reciprecal of the divisor.
    // To integer divide by multiplying where b is a constant
    //   a / b == (a * (n / b)) / n
    // where n is a power of two.  For 64-bit values
    // n is 1**64.  This requires a 128 bit product to get the
    // correct results for all values of a & b.  For 32 signed
    // values only a 63 bit product is needed.  For 32 unsigned
    // and some values of b, n must be 1**64 requiring a 128 bit product.
    //
    // The alpha provides a UMULH instruction that does 64-bit multiply and returns
    // the top 64 bits of the 128-bit product.

    long    fi    = 0;
    boolean upper = quad;
    int     shift = 0;

    if (!upper) { // If 32-bit operation.
      fi = (1L << 62) / value;

      for (shift = 0; shift < 61; shift++) {
        if ((fi & (1L << (61 - shift))) != 0)
          break;
      }

      // If possible see if we can use the faster MULQ instead of the UMULH
      // instruction.  We can if there won't be any overflow out of the 64th bit.

      long fi1 = (fi & (0xffffffffL << (30 - shift))) + (1L << ((30 - shift)));
      long fi2 = (fi & (0xffffffffL << (30 - shift))) + (1L << ((29 - shift)));
      long d2  = fi - fi2;
      if (signed || (d2 >= 0)) { // Use MULQ.
        fi = (fi1 >> (30 - shift)) & 0xffffffffL;
      } else { // Use UMULH.
        upper = true;
        fi = fi2 << 2;
        shift = 0;
      }
    } else { // If 64-bit operation.
      BigInteger g = BigInteger.valueOf(value);
      BigInteger f = BigInteger.ONE.shiftLeft(64).divide(g);
      for (shift = 0; shift < 64; shift++) {
        if (f.testBit(64 - shift - 1))
          break;
      }
      fi = BigInteger.ONE.shiftLeft(64 + shift).divide(g).add(BigInteger.ONE).longValue();
    }

    int tr = registers.newTempRegister(RegisterSet.INTREG);
    int fr = registers.newTempRegister(RegisterSet.INTREG);

    fr = genLoadImmediate(fi, fr);
    if (signed) {
      appendInstruction(new IntOpInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, src, tr));
      appendInstruction(new IntOpInstruction(Opcodes.CMOVGE, src, src, tr));
      if (upper) {
        appendInstruction(new IntOpInstruction(Opcodes.UMULH, tr, fr, tr));
        if (shift > 0)
          appendInstruction(new IntOpLitInstruction(Opcodes.SRL, tr, shift, tr));
      } else {
        appendInstruction(new IntOpInstruction(Opcodes.MULQ, tr, fr, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SRL, tr, 32 + shift, tr));
      }
      if (src == dest) {
        int tr2 = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, tr, tr2));
        appendInstruction(new IntOpInstruction(Opcodes.CMOVGE, src, tr, tr2));
        genRegToReg(tr2, dest);
      } else {
        appendInstruction(new IntOpInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, tr, dest));
        appendInstruction(new IntOpInstruction(Opcodes.CMOVGE, src, tr, dest));
      }
    } else {
      if (upper) {
        if (shift > 0) {
          appendInstruction(new IntOpInstruction(Opcodes.UMULH, src, fr, tr));
          appendInstruction(new IntOpLitInstruction(Opcodes.SRL, tr, shift, dest));
        } else
          appendInstruction(new IntOpInstruction(Opcodes.UMULH, src, fr, dest));
      } else {
        appendInstruction(new IntOpInstruction(Opcodes.MULQ, src, fr, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SRL, tr, 32 + shift, dest));
      }
    }
    if (neg)
      appendInstruction(new IntOpInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, dest, dest));
  }

  /**
   * Return the appropriate opcode for the operation and type of operands.
   * @param which specifies the binary operation
   * @param isFlt is true for floating point operation
   * @param size is the size of the operand
   */
  private int getBinaryOpcode(int which, boolean isFlt, int size)
  {
    int k      = ((isFlt ? 2 : 0) + ((size > 4) ? 1 : 0));
    int index  = (which * 4) + k;
    return binops[index];
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
    int bs = ct.memorySizeAsInt(machine);

    if (ct.isComplexType()) {
      doComplexOp(which, bs / 2, la, ra, ir);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    boolean  flt    = ct.isRealType();
    int      opcode = getBinaryOpcode(which, flt, bs);
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
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      if ((value == 0) && ((which == ADD) || (which == SUB) || (which == OR))) {
        resultReg = laReg;
        return;
      }
      if (flt) {
        if (value == 0) {
          appendInstruction(new FltOpInstruction(opcode, laReg, AlphaRegisterSet.F0_REG, ir));
          resultReg = ir;
          return;
        }
      } else if (opcode == Opcodes.SLL) {
        if (ct.isSigned())
          genLeftShiftSigned(laReg, value, ir, bs);
        else
          genLeftShiftUnsigned(laReg, value, ir, bs);

        resultReg = ir;
        return;
      } else {
        appendInstruction(new IntOpLitInstruction(opcode, laReg, (int) value, ir));
        if (bs <= 4) {
          if (!ct.isSigned())
            appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, ir, (1 << bs) - 1, ir));
          else if (opcode == Opcodes.SLL) {
            if (bs == 4)
              appendInstruction(new IntOpLitInstruction(Opcodes.ADDL, ir, 0, ir));
            else {
              appendInstruction(new IntOpLitInstruction(Opcodes.SLL, ir, (8 - bs) * 8, ir));
              appendInstruction(new IntOpLitInstruction(Opcodes.SRA, ir, (8 - bs) * 8, ir));
            }
          }
        }
        resultReg = ir;
        return;
      }
    }

    needValue(ra);
    int raReg = resultReg;

    if (((raReg == AlphaRegisterSet.F0_REG) || (raReg == AlphaRegisterSet.I0_REG)) &&
        ((which == ADD) || (which == SUB))) {
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = laReg;
      return;
    }
      
    if (flt) {
      appendInstruction(new FltOpInstruction(opcode, laReg, raReg, ir));
    } else {
      appendInstruction(new IntOpInstruction(opcode, laReg, raReg, ir));
      if (bs <= 4) {
        if (!ct.isSigned())
          appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, ir, (1 << bs) - 1, ir));
        else if (opcode == Opcodes.SLL) {
          if (bs == 4)
            appendInstruction(new IntOpLitInstruction(Opcodes.ADDL, ir, 0, ir));
          else {
            appendInstruction(new IntOpLitInstruction(Opcodes.SLL, ir, (8 - bs) * 8, ir));
            appendInstruction(new IntOpLitInstruction(Opcodes.SRA, ir, (8 - bs) * 8, ir));
          }
        }
      }
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private void genLeftShiftSigned(int src, long value, int dest, int destSize)
  {
    if (destSize > 4) {
      if (value == 2)
        appendInstruction(new IntOpInstruction(Opcodes.S4ADDQ,
                                               src,
                                               AlphaRegisterSet.I0_REG,
                                               dest));
      else if (value == 3)
        appendInstruction(new IntOpInstruction(Opcodes.S8ADDQ,
                                               src,
                                               AlphaRegisterSet.I0_REG,
                                               dest));
      else {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL,
                                                  src,
                                                  (int) value,
                                                  dest));
      }
    } else {
      if (value == 2)
        appendInstruction(new IntOpInstruction(Opcodes.S4ADDL,
                                               src,
                                               AlphaRegisterSet.I0_REG,
                                               dest));
      else if (value == 3)
        appendInstruction(new IntOpInstruction(Opcodes.S8ADDL,
                                               src,
                                               AlphaRegisterSet.I0_REG,
                                               dest));
      else {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL,
                                                  src,
                                                  (int) value,
                                                  dest));
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDL,
                                                  dest,
                                                  0,
                                                  dest));
      }
    }
  }

  private void genLeftShiftUnsigned(int src, long value, int dest, int destSize)
  {
    appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, (int) value, dest));
    if (destSize < 8)
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT,
                                                dest,
                                                (1 << destSize) - 1,
                                                dest));
  }

  private void doComplexOp(int which, int bs, Expr la, Expr ra, int dest)
  {
    if (la instanceof ComplexValueExpr) {
      ComplexValueExpr cve = (ComplexValueExpr) la;
      Expr cla = cve.getLeftArg();
      Expr cra = cve.getRightArg();
      if (cra.isLiteralExpr() && ((LiteralExpr) cra).isZero()) {
        la = cla;
      }
    } else if (ra instanceof ComplexValueExpr) {
      ComplexValueExpr cve = (ComplexValueExpr) ra;
      Expr cla = cve.getLeftArg();
      Expr cra = cve.getRightArg();
      if (cra.isLiteralExpr() && ((LiteralExpr) cra).isZero()) {
        ra = cla;
      }
    }

    needValue(la);
    int laReg = resultReg;

    needValue(ra);
    int raReg = resultReg;

    doComplexOp(which, bs, laReg, raReg, dest);
  }

  private void doComplexOp(int which, int bs, int laReg, int raReg, int dest)
  {
    int k      = (bs > 4) ? 3 : 2;
    int index  = (which * 4) + k;
    int opcode = binops[index];

    switch (which) {
    case ADD:
      if (registers.pairRegister(laReg)) {
        int t = laReg;
        laReg = raReg;
        raReg = t;
      }
      appendInstruction(new FltOpInstruction(opcode, laReg, raReg, dest));
      if (registers.pairRegister(laReg)) {
        appendInstruction(new FltOpInstruction(opcode, laReg + 1, raReg + 1, dest + 1));
      } else {
        appendInstruction(new FltOpInstruction(Opcodes.CPYS, raReg + 1, raReg + 1, dest + 1));
      }
      return;
    case SUB:
      if (registers.pairRegister(laReg)) {
        if (registers.pairRegister(raReg)) {
          appendInstruction(new FltOpInstruction(opcode, laReg, raReg, dest));
          appendInstruction(new FltOpInstruction(opcode, laReg + 1, raReg + 1, dest + 1));
        } else {
          appendInstruction(new FltOpInstruction(opcode, laReg, raReg, dest));
          appendInstruction(new FltOpInstruction(Opcodes.CPYS, laReg + 1, laReg + 1, dest + 1));
        }
      } else {
        appendInstruction(new FltOpInstruction(opcode, laReg, raReg, dest));
        appendInstruction(new FltOpInstruction(opcode,
                                               AlphaRegisterSet.F0_REG,
                                               raReg + 1,
                                               dest + 1));
      }
      return;
    case MUL:
      if (registers.pairRegister(laReg)) {
        int t = laReg;
        laReg = raReg;
        raReg = t;
      }
      if (registers.pairRegister(laReg)) {
        int tr    = registers.newTempRegister(RegisterSet.FLTREG);
        int tr2   = registers.newTempRegister(RegisterSet.FLTREG);
        int tr3   = registers.newTempRegister(RegisterSet.FLTREG);
        int addop = binops[(ADD * 4) + k];
        int subop = binops[(SUB * 4) + k];
        appendInstruction(new FltOpInstruction(opcode, laReg,     raReg,     tr2));
        appendInstruction(new FltOpInstruction(opcode, laReg + 1, raReg + 1, tr3));
        appendInstruction(new FltOpInstruction(opcode, laReg,     raReg + 1, tr));
        appendInstruction(new FltOpInstruction(opcode, laReg + 1, raReg,     dest + 1));
        appendInstruction(new FltOpInstruction(subop,  tr2,       tr3,       dest));
        appendInstruction(new FltOpInstruction(addop,  tr,        dest + 1,  dest + 1));
      } else {
        appendInstruction(new FltOpInstruction(opcode, laReg, raReg,     dest));
        appendInstruction(new FltOpInstruction(opcode, laReg, raReg + 1, dest + 1));
      }
      return;
    case DIV:
      if (registers.pairRegister(raReg)) {
        // (r3, i3i) = (r1, i1i) / (r2, i2i)
        // (r3,i3i) * (r2, i2i) = (r1, i1i)
        // r3*r2 + r3i2i + i3i * r2 + i3i * i2i = (r1, i1i)
        // r1 = r3*r2 + i3i * i2i = r3*r2 - i3*i2
        // i1i = r3*i2i + i3i * r2
        // i1 = r3*i2 + i3*r2

        // r3 = (r1 + i3*i2)/r2
        // i3 = (i1 - r3*i2)/r2

        // r3 = (r1 + (i1 - r3*i2)*i2)/r2)/r2
        // r3*r2 = r1 + (i1 - r3*i2)*i2/r2
        // r3*r2*r2 = r1*r2 + i1*i2 - r3*i2*i2
        // r3(r2*r2 +i2*i2) = r1*r2 + i1*i2
        // r3 = (r1*r2 + i1*i2)/(r2*r2 + i2*i2)

        // i3 = (i1 - i2*(r1 + i3*i2)/r2))/r2
        // i3*r2 = i1 - i2 * (r1 + i3 * i2)/r2
        // i3*r2*r2 = i1*r2 - i2*r1 - i2*i2*i3
        // i3*(r2*r2 + i2*i2) = i1*r2 - i2*r1
        // i3 = (i1*r2 - i2*r1)/(r2*r2 + i2*i2)
        int addop = binops[(ADD * 4) + k];
        int subop = binops[(SUB * 4) + k];
        int mulop = binops[(MUL * 4) + k];
        int r1  = laReg;
        int i1  = laReg + 1;
        int r2  = raReg;
        int i2  = raReg + 1;
        int r3  = dest;
        int i3  = dest + 1;
        int t3  = registers.newTempRegister(RegisterSet.INTREG + RegisterSet.PAIRREG);
        int t17 = registers.newTempRegister(RegisterSet.INTREG);
        int t18 = registers.newTempRegister(RegisterSet.INTREG);
        int f12 = registers.newTempRegister(RegisterSet.FLTREG);
        int f13 = registers.newTempRegister(RegisterSet.FLTREG);
        int f14 = registers.newTempRegister(RegisterSet.FLTREG);
        int f15 = registers.newTempRegister(RegisterSet.FLTREG);
        int f16 = registers.newTempRegister(RegisterSet.FLTREG);
        int f17 = registers.newTempRegister(RegisterSet.FLTREG);

        if (!registers.pairRegister(laReg))
          i1 = AlphaRegisterSet.F0_REG;

        genLoadImmediate(2047, t17);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, t17, 52, t17));
        genRegToReg(r2, t3);
        appendInstruction(new IntOpInstruction(Opcodes.AND, t3, t17, t3));
        appendInstruction(new IntOpInstruction(Opcodes.AND, t3 + 1, t17, t3 + 1));
        appendInstruction(new IntOpInstruction(Opcodes.CMPLT, t3, t3 + 1, t18));
        appendInstruction(new IntOpInstruction(Opcodes.CMOVNE, t18, t3 + 1, t3));
        appendInstruction(new IntOpInstruction(Opcodes.SUBQ, t17, t3, t17));
        genRegToReg(t17, f13); //  f13 = scale
        appendInstruction(new FltOpInstruction(mulop,  r2, f13, f12)); // f12 = r2 * scale
        appendInstruction(new FltOpInstruction(mulop,  i2, f13, f13)); // f13 = i2 * scale
        appendInstruction(new FltOpInstruction(mulop,  r2, f12, f14)); // f14 = r2 * r2 * scale
        appendInstruction(new FltOpInstruction(mulop,  i2, f13, f15)); // f15 = i2 * i2 * scale
        appendInstruction(new FltOpInstruction(mulop,  r1, f12, f16)); // f16 = r1 * r2 * scale
        appendInstruction(new FltOpInstruction(mulop,  i1, f13, f17)); // f17 = i1 * i2 * scale
        appendInstruction(new FltOpInstruction(mulop,  i1, f12, f12)); // f12 = i1 * r2 * scale
        appendInstruction(new FltOpInstruction(mulop,  r1, f13, f13)); // f13 = r1 * i2 * scale
        appendInstruction(new FltOpInstruction(addop, f14, f15, f14)); // f14 = r2 * r2 * scale + i2 * i2 * scale
        genLoadDblImmediate(1.0, f15, bs); //  f15 = 1.0
        appendInstruction(new FltOpInstruction(addop,  f16, f17, f17)); // f17 = r1 * r2 * scale + i1 * i2 * scale
        appendInstruction(new FltOpInstruction(subop,  f12, f13, f13)); // f13 = i1 * r2 * scale - r1 * i2 * scale
        appendInstruction(new FltOpInstruction(opcode, f15, f14, f14)); // f14 = 1.0 / ( r2 * r2 * scale + i2 * i2 * scale)
        appendInstruction(new FltOpInstruction(mulop,  f17, f14,  r3)); // r3  = (r1 * r2 * scale + i1 * i2 * scale) * (1.0 / ( r2 * r2 * scale + i2 * i2 * scale))
        appendInstruction(new FltOpInstruction(mulop,  f13, f14,  i3)); // i3  = (i1 * r2 * scale - r1 * i2 * scale) * (1.0 / ( r2 * r2 * scale + i2 * i2 * scale))
        usesGp = true;
      } else {
        appendInstruction(new FltOpInstruction(opcode, laReg,     raReg, dest));
        appendInstruction(new FltOpInstruction(opcode, laReg + 1, raReg, dest + 1));
      }
      return;
    default:
      throw new scale.common.InternalError("Invalid complex op " + which);
    }
  }

  /**
   * Generate instructions to do a comparison of two value.
   * @param c is the compare expression
   * @param which specifies the compare (EQ, NE, ...)
   */
  protected void doCompareOp(BinaryExpr c, CompareMode which)
  {
    int  ir = registers.getResultRegister(processType(c).getTag());
    Expr la = c.getLeftArg();
    Expr ra = c.getRightArg();

    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
      which = which.argswap();
    }

    Type    lt = processType(la);
    boolean si = lt.isSigned();
    needValue(la);
    int laReg = resultReg;

    if (!lt.isRealType()) { // Integer
      if (ra.isLiteralExpr()) {
        Literal lit   = ((LiteralExpr) ra).getLiteral().getConstantValue();
        long    value = -1;

        if (lit instanceof IntLiteral)
          value = ((IntLiteral) lit).getLongValue();
        else if (lit instanceof CharLiteral)
          value = ((CharLiteral) lit).getCharacterValue();

        if ((value >= 0) && (value < 256)) {
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;

          switch (which) {
          case EQ:
            appendInstruction(new IntOpLitInstruction(Opcodes.CMPEQ, laReg, (int) value, ir));
            resultReg = ir;
            return;
          case LE:
            if (si)
              appendInstruction(new IntOpLitInstruction(Opcodes.CMPLE, laReg, (int) value, ir));
            else
              appendInstruction(new IntOpLitInstruction(Opcodes.CMPULE, laReg, (int) value, ir));
            resultReg = ir;
            return;
          case LT:
            if (si)
              appendInstruction(new IntOpLitInstruction(Opcodes.CMPLT, laReg, (int) value, ir));
            else
              appendInstruction(new IntOpLitInstruction(Opcodes.CMPULT, laReg, (int) value, ir));
            resultReg = ir;
            return;
          case NE:
            if (value == 0) {
              appendInstruction(new IntOpInstruction(Opcodes.CMPULT,
                                                     AlphaRegisterSet.I0_REG,
                                                     laReg,
                                                     ir));
              resultReg = ir;
              return;
            }
            appendInstruction(new IntOpLitInstruction(Opcodes.CMPEQ, laReg, (int) value, ir));
            appendInstruction(new IntOpLitInstruction(Opcodes.XOR, ir, 1, ir));
            resultReg = ir;
            return;
          }
        }
      }

      needValue(ra);
      int raReg = resultReg;

      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;

      switch (which) {
      case EQ:
        appendInstruction(new IntOpInstruction(Opcodes.CMPEQ, laReg, raReg, ir));
        break;
      case LE:
        if (si)
          appendInstruction(new IntOpInstruction(Opcodes.CMPLE, laReg, raReg, ir));
        else
          appendInstruction(new IntOpInstruction(Opcodes.CMPULE, laReg, raReg, ir));
        break;
      case LT:
        if (si)
          appendInstruction(new IntOpInstruction(Opcodes.CMPLT, laReg, raReg, ir));
        else
          appendInstruction(new IntOpInstruction(Opcodes.CMPULT, laReg, raReg, ir));
        break;
      case GT:
        if (si)
          appendInstruction(new IntOpInstruction(Opcodes.CMPLT, raReg, laReg, ir));
        else
          appendInstruction(new IntOpInstruction(Opcodes.CMPULT, raReg, laReg, ir));
        break;
      case GE:
        if (si)
          appendInstruction(new IntOpInstruction(Opcodes.CMPLE, raReg, laReg, ir));
        else
          appendInstruction(new IntOpInstruction(Opcodes.CMPULE, raReg, laReg, ir));
        break;
      case NE:
        appendInstruction(new IntOpInstruction(Opcodes.CMPEQ, laReg, raReg, ir));
        appendInstruction(new IntOpLitInstruction(Opcodes.XOR, ir, 1, ir)); break;
      default:
        throw new scale.common.InternalError("Invalid which " + which);
      }

      resultReg = ir;
      return;
    }

    // Floating point

    boolean flag = true;

    if (ra.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) ra).getLiteral();
      if (lit instanceof FloatLiteral) {
        double value = ((FloatLiteral) lit).getDoubleValue();
        flag = (value != 0.0);
      }
    }

    if (flag) {
      int nla = registers.newTempRegister(RegisterSet.FLTREG);
      needValue(ra);
      int raReg = resultReg;
      appendInstruction(new FltOpInstruction(Opcodes.SUBT, laReg, raReg, nla));
      if (lt.isComplexType()) {
        int treg2  = registers.newTempRegister(RegisterSet.FLTREG);
        int laRegP = laReg + registers.numContiguousRegisters(laReg);
        int raRegP = raReg + registers.numContiguousRegisters(raReg);
        appendInstruction(new FltOpInstruction(Opcodes.SUBT, laRegP, raRegP, treg2));
        appendInstruction(new FltOpInstruction(Opcodes.FCMOVEQ, nla, treg2, nla));
      }
      laReg = nla;
    }

    Label             labb = createLabel();
    Label             labf = createLabel();
    LabelDisplacement disp = new LabelDisplacement(labb);
    Branch            inst = new BranchInstruction(fbops[which.ordinal()], laReg, disp, 2);

    inst.addTarget(labb, 0);
    inst.addTarget(labf, 1);

    appendInstruction(new IntOpLitInstruction(Opcodes.BIS,
                                              AlphaRegisterSet.I0_REG,
                                              1,
                                              ir));
    appendInstruction(inst);
    appendLabel(labf);
    labf.setNotReferenced();
    appendInstruction(new IntOpInstruction(Opcodes.BIS,
                                           AlphaRegisterSet.I0_REG,
                                           AlphaRegisterSet.I0_REG,
                                           ir));
    appendLabel(labb);

    resultReg = ir;
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
  }

  /**
   * Called at the start of code generation for a routine.
   */
  protected Instruction startRoutineCode()
  {
    currentBeginMarker = new BeginMarker(scribble);

    return currentBeginMarker;
  }

  protected void processSourceLine(int line, Label lab, boolean newLine)
  {
    if (line < 0)
      return;
    String fileName = currentRoutine.getCallGraph().getName();
    appendInstruction(new AlphaLineMarker(fileName, line));
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
    Branch            inst = new BranchInstruction(Opcodes.BR, AlphaRegisterSet.I0_REG, disp, 1);

    inst.addTarget(lab, 0);
    appendInstruction(inst);
  }

  /**
   * Restore the value of the GP register after a JSR instruction.
   */
  private void restoreGpReg()
  {
    if (usesGp) {
      Displacement disp = new SymbolDisplacement("", 0);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH,
                                                   AlphaRegisterSet.GP_REG,
                                                   AlphaRegisterSet.RA_REG,
                                                   disp,
                                                   RT_GPDISP));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   AlphaRegisterSet.GP_REG,
                                                   AlphaRegisterSet.GP_REG,
                                                   disp,
                                                   RT_GPDISP));
    }
  }

  /**
   * Specify that this routine saved a register on the stack.
   */
  private void setRegisterSaved(int reg)
  {
    if (reg <= AlphaRegisterSet.I0_REG) {
      mask |= 1 << reg;
      mask |= 1 << AlphaRegisterSet.RA_REG;
    } else {
      fmask |= 1 << reg;
      mask |= 1 << AlphaRegisterSet.RA_REG;
    }
  }

  /**
   * Obtain the information needed for register spill loads and stores.
   * The Object returned will probably specify a memory location.
   * It will be passed to getSpillLoad() and getSpillStore().
   * @param reg specifies which virtual register will be spilled
   */
  public Object getSpillLocation(int reg)
  {
    if (shouldBeRegenerated(reg))
      return null;

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
    if (spillLocation == null) {
      Instruction nxt = after.getNext();
      if (nxt instanceof MemoryInstruction) {
        // The "literal use" is always the second instruction in a
        // pare.  Consequently, it is never ok to insert the
        // regeneration of the value just before it.
        MemoryInstruction mem   = (MemoryInstruction) nxt;
        int               reloc = mem.getRelocType();
        if ((reloc >= RT_LITUSE_BASE) && (reloc <= RT_LITUSE_JSR))
          return nxt;
      }
      return regenerateRegister(reg, after);
    }

    boolean flt = registers.floatRegister(reg);
    int     opl = flt ? Opcodes.LDT : Opcodes.LDQ;

    StackDisplacement disp   = (StackDisplacement) spillLocation;
    long              offset = disp.getDisplacement();
    Instruction       sl     = lastInstruction;
    Instruction       next   = after.getNext();
    Instruction       load;

    lastInstruction = after;
    after.setNext(null);

    if ((offset > MAX_IMM16) || (offset < MIN_IMM16)) {
      int tr = reg;
      if (registers.floatRegister(reg))
        tr = AlphaRegisterSet.IF_REG;
      genLoadImmediate(offset, stkPtrReg, tr);
      load = new LoadInstruction(opl, reg, tr, getDisp(0));
    } else
      load = new LoadInstruction(opl, reg, stkPtrReg, (Displacement) spillLocation);


    appendInstruction(load);

    load.setNext(next);
    lastInstruction = sl;

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
    if (spillLocation == null)
      return null;

    boolean flt = registers.floatRegister(reg);
    int     ops = flt ? Opcodes.STT : Opcodes.STQ;

    StackDisplacement disp   = (StackDisplacement) spillLocation;
    long              offset = disp.getDisplacement();
    Instruction       sl     = lastInstruction;
    Instruction       next   = after.getNext();
    Instruction       store;

    lastInstruction = after;
    after.setNext(null);

    if ((offset > MAX_IMM16) || (offset < MIN_IMM16)) {
      int tr = 28;
      genLoadImmediate(offset, stkPtrReg, tr);
      store = new StoreInstruction(ops, reg, tr, getDisp(0));
    } else
      store = new StoreInstruction(ops, reg, stkPtrReg, (Displacement) spillLocation);

    appendInstruction(store);

    store.setNext(next);
    lastInstruction = sl;

    return store;
  }

  /**
   * Called at the end of code generation for a routine.
   * On the Alpha we need to pass some critical information to the assembler.
   * <p>
   * For a called routine, the stack looks like:  
   * <pre>  
   *  -------------------------- <= High memory
   *  |                        |      
   *  |  Arguements 7..n       |      
   *  |                        |      
   *  -------------------------- <= SP + frameSize
   *  |                        |      
   *  |  Structure Overflow    |      
   *  |   (may not exist)      |      
   *  |                        |      
   *  -------------------------- <= SP + frameSize - entryOverflowSize  
   *  |                        |      
   *  |  Argument Save Area    |      
   *  |   (may not exist)      |      
   *  |                        |      
   *  --------------------------  
   *  |                        |      
   *  |  Local Variables       |      
   *  |                        |      
   *  -------------------------- <= SP + srOffset  
   *  |                        |      
   *  |  Saved Registers       |      
   *  |                        |      
   *  -------------------------- <= SP + argumentBuildSize  
   *  |                        |      
   *  |  Argument build        |  
   *  |  Area                  |      
   *  |                        |      
   *  -------------------------- <= SP  
   * </pre>  
   * Note - all locations are accessed using offsets from the <tt>SP</tt> register.  
   * The <em>structure overflow</em> area is used to save the <em>first</em> <tt>n</tt> words  
   * of a structure passed by value that does not completely fit into the  
   * six argument registers.
   */
  protected void endRoutineCode(int[] regMap)
  {
    appendInstruction(new EndMarker(currentRoutine));

    Instruction last = returnInst;
    while (last != null) {
      Instruction nxt = last.getNext();
      if ((nxt != null) && nxt.isBranch())
        break;
      last = nxt;
    }

    if ((last == null) && Debug.debug(1))
      System.out.println("** Warning: " + currentRoutine.getName() + "() does not return.");

    int       nrr    = registers.numRealRegisters();
    boolean[] newmap = new boolean[nrr];
    int       n      = regMap.length;

    for (int i = nrr; i < n; i++) {
      int vr = regMap[i];
      if (vr < nrr) /* In case of debugging. */
        newmap[vr] = true;
    }

    if (usesAlloca)
      newmap[AlphaRegisterSet.FP_REG] = true;

    Instruction first = currentBeginMarker;
    if (usesGp) {
      Instruction li  = lastInstruction;
      Instruction nxt = currentBeginMarker.getNext();
      lastInstruction = currentBeginMarker;
      Displacement  disp = new SymbolDisplacement("", 0);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH,
                                                   AlphaRegisterSet.GP_REG,
                                                   27,
                                                   disp,
                                                   RT_GPDISP));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   AlphaRegisterSet.GP_REG,
                                                   AlphaRegisterSet.GP_REG,
                                                   disp,
                                                   RT_GPDISP));
      usesGp = true;
      lastInstruction.setNext(nxt);
      lastInstruction = li;
    }

    argBuildSize = (int) Machine.alignTo(argBuildSize, 16);

    short[] calleeSaves = registers.getCalleeSaves();
    int     numToSave   = 0;
    for (int i = 0; i < calleeSaves.length; i++) { // Determine which registers need saving.
      int reg = calleeSaves[i];

      if (!newmap[reg])
        continue;
      numToSave++;
    }

    int ci        = (callsRoutine ? AlphaRegisterSet.IREG_SIZE : 0);
    int srOffset  = (argBuildSize + (AlphaRegisterSet.FREG_SIZE * numToSave) + ci);
    int frameSize = ((localVarSize + srOffset + entryOverflowSize + 15) / 16) * 16; // Frame size must be a multiple of 16.
    if (argDisp != null)
      frameSize += 6 * (AlphaRegisterSet.IREG_SIZE + AlphaRegisterSet.FREG_SIZE);

    if (trace) {
      System.out.print("LVS: ");
      System.out.print(currentRoutine.getName());
      System.out.print(" fs ");
      System.out.print(frameSize);
      System.out.print(" sro ");
      System.out.print(srOffset);
      System.out.print(" lvs ");
      System.out.print(localVarSize);
      System.out.print(" abs ");
      System.out.print(argBuildSize);
      System.out.print(" fo ");
      System.out.print(frameSize - argBuildSize);
      System.out.print(" eos ");
      System.out.print(entryOverflowSize);
      System.out.print(" xxx ");
      System.out.println(localVarSize + srOffset + entryOverflowSize);
    }

    Instruction li  = lastInstruction;
    lastInstruction = routineLabel.get(currentRoutine);
    Instruction nxt = lastInstruction.getNext();

    if (frameSize > 0) {
      int fs = frameSize;
      while (fs > 0) {
        int          inc    = (fs > 0x7fff) ? 0x7fff : fs;
        Displacement fdispm = getDisp(-inc);
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                     AlphaRegisterSet.SP_REG,
                                                     AlphaRegisterSet.SP_REG,
                                                     fdispm));
        fs -= inc;
      }
    }

    int srop = argBuildSize;
    raDisp.adjust(srop);
    if (callsRoutine) {
      appendInstruction(new StoreInstruction(Opcodes.STQ,
                                             AlphaRegisterSet.RA_REG,
                                             AlphaRegisterSet.SP_REG,
                                             raDisp));
      setRegisterSaved(AlphaRegisterSet.RA_REG);
      srop += AlphaRegisterSet.IREG_SIZE;
    }

    if (numToSave > 0) {
      for (int i = 0; i < calleeSaves.length; i++) { // Determine which registers need saving.
        int reg = calleeSaves[i];

        if (!newmap[reg])
          continue;

        Displacement disp = getDisp(srop);
        int          opcode = registers.floatRegister(reg) ? Opcodes.STT : Opcodes.STQ;
        appendInstruction(new StoreInstruction(opcode, reg, AlphaRegisterSet.SP_REG, disp));
        setRegisterSaved(reg);
        srop += AlphaRegisterSet.FREG_SIZE;
      }
    }

    if (argDisp != null) {
      int offseti = AlphaRegisterSet.FREG_SIZE * MAX_ARG_REGS;
      int offsetf = 0;
      for (int i = 0; i < MAX_ARG_REGS; i++) {
        appendInstruction(new StoreInstruction(Opcodes.STT,
                                               AlphaRegisterSet.FF_REG + i,
                                               AlphaRegisterSet.SP_REG,
                                               argDisp.offset(offsetf)));
        offsetf += AlphaRegisterSet.FREG_SIZE;
      }
      for (int i = 0; i < MAX_ARG_REGS; i++) {
        appendInstruction(new StoreInstruction(Opcodes.STQ,
                                               AlphaRegisterSet.IF_REG + i,
                                               AlphaRegisterSet.SP_REG,
                                               argDisp.offset(offseti)));
        offseti += AlphaRegisterSet.IREG_SIZE;
      }
    }

    if (usesAlloca)
      genRegToReg(AlphaRegisterSet.SP_REG, AlphaRegisterSet.FP_REG);

    appendInstruction(new PrologMarker(mask,
                                       fmask,
                                       stkPtrReg,
                                       frameSize,
                                       frameSize - argBuildSize,
                                       usesGp));

    lastInstruction.setNext(nxt);
    lastInstruction = li;

    if (last != null) {
      li  = lastInstruction;
      lastInstruction = last;
      nxt = last.getNext();

      if (usesAlloca)
        genRegToReg(AlphaRegisterSet.FP_REG, AlphaRegisterSet.SP_REG);

      int sroe = argBuildSize;
      if (callsRoutine) {
        appendInstruction(new StoreInstruction(Opcodes.LDQ,
                                               AlphaRegisterSet.RA_REG,
                                               AlphaRegisterSet.SP_REG, raDisp));
        sroe += AlphaRegisterSet.IREG_SIZE;
      }
      if (numToSave > 0) {
        for (int i = 0; i < calleeSaves.length; i++) { // Determine which registers need saving.
          int reg = calleeSaves[i];

          if (!newmap[reg])
            continue;
          Displacement disp   = getDisp(sroe);
          int          opcode = registers.floatRegister(reg) ? Opcodes.LDT : Opcodes.LDQ;
          appendInstruction(new LoadInstruction(opcode, reg, AlphaRegisterSet.SP_REG, disp));
          sroe += AlphaRegisterSet.FREG_SIZE;
        }
      }

      if (frameSize > 0) {
        int fs = frameSize;
        while (fs > 0) {
          int          inc    = (fs > 0x7fff) ? 0x7fff : fs;
          Displacement fdisp  = getDisp(inc);
          appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                       AlphaRegisterSet.SP_REG,
                                                       AlphaRegisterSet.SP_REG, fdisp));
          fs -= inc;
        }
      }

      lastInstruction.setNext(nxt);
      lastInstruction = li;
    }

    // Adjust stack offsets now that we know how big everything is.

    int lsv = localVar.size();
    boolean largeDisp = false;
    for (int i = 0; i < lsv; i++) {
      StackDisplacement disp = localVar.elementAt(i);
      disp.adjust(srOffset);
      long d = disp.getDisplacement();
      if ((d < MIN_IMM16) || (d > MAX_IMM16))
        largeDisp = true;
    }

    if (largeDisp) {
      // We have some large stack offsets that need to be adjusted.
      last = null;
      for (Instruction inst = currentBeginMarker;
           inst != null;
           last = inst, inst = inst.getNext()) {
        if (inst.isLoad() || inst.isStore()) {
          MemoryInstruction mi   = (MemoryInstruction) inst;
          Displacement      disp = mi.getDisplacement();
          if (disp instanceof StackDisplacement) {
            long d = disp.getDisplacement();
            if ((d < MIN_IMM16) || (d > MAX_IMM16)) {
              Instruction lasti = lastInstruction;
              lastInstruction = last;
              disp = genLoadHighImmediate(disp, ((mi.getRb() == AlphaRegisterSet.FP_REG) ?
                                                 AlphaRegisterSet.FP_REG :
                                                 AlphaRegisterSet.SP_REG));
              mi.setDisplacement(disp);
              mi.setRb(AlphaRegisterSet.AT_REG);
              lastInstruction.setNext(mi);
              lastInstruction = lasti;
            }
          }
        }
      }
    }

    int leo = entryOverflow.size();
    for (int i = 0; i < leo; i++) {
      Displacement disp = entryOverflow.elementAt(i);
      disp.adjust(frameSize - entryOverflowSize);
    }
    if (argDisp != null)
      argDisp.adjust(frameSize - 6 * (AlphaRegisterSet.IREG_SIZE + AlphaRegisterSet.FREG_SIZE));
  }

  protected Branch genFtnCall(String name, short[] uses, short[] defs)
  {
    int          handle = allocateTextArea(name, TEXT);
    Displacement disp   = new SymbolDisplacement(name, handle);
    Label        lab    = createLabel();
    Branch       jsr    = new JmpInstruction(Opcodes.JSR,
                                             AlphaRegisterSet.RA_REG,
                                             AlphaRegisterSet.PV_REG,
                                             disp,
                                             1);

    associateDispWithArea(handle, disp);

    jsr.additionalRegsUsed(uses);
    jsr.additionalRegsKilled(registers.getCalleeUses());
    jsr.additionalRegsSet(defs);
    jsr.addTarget(lab, 0);
    jsr.markAsCall();

    usesGp = true;

    lastInstruction.specifySpillStorePoint();
    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          AlphaRegisterSet.PV_REG,
                                          AlphaRegisterSet.GP_REG,
                                          disp,
                                          RT_LITERAL));
    appendInstruction(jsr);
    appendLabel(lab);
    lab.markAsFirstInBasicBlock();
    lab.setNotReferenced();
    restoreGpReg();
    callsRoutine = true;

    return jsr;
  }

  /**
   * Generate an integer-literal operate instruction.
   */
  private void doIntOperate(int opcode, int ra, long value, int rc)
  {
    if ((value >= 0) && (value < 256)) {
      appendInstruction(new IntOpLitInstruction(opcode, ra, (int) value, rc));
      return;
    }

    int rb  = registers.newTempRegister(RegisterSet.AIREG);
    genLoadImmediate(value, rb);
    appendInstruction(new IntOpInstruction(opcode, ra, rb, rc));
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    Expr op  = e.getArg();
    int  reg = registers.getResultRegister(processType(e).getTag());
    Type ot  = processType(op);

    if (ot.isComplexType()) {
      short[] uses = callArgs(e.getOperandArray(), false);
      genFtnCall("cabs", uses, null);
      genRegToReg(AlphaRegisterSet.FR_REG, reg);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = reg;
      return;
    }

    needValue(op);
    int raReg = resultReg;

    if (registers.floatRegister(raReg)) {
      appendInstruction(new FltOpInstruction(Opcodes.CPYS, AlphaRegisterSet.F0_REG, raReg, reg));
    } else {
      int bs = ot.memorySizeAsInt(machine);
      int so = getBinaryOpcode(SUB, false, bs);
      int ir = registers.newTempRegister(RegisterSet.INTREG); // raReg & reg could be the same register.
      appendInstruction(new IntOpInstruction(so, AlphaRegisterSet.I0_REG, raReg, ir));
      appendInstruction(new IntOpInstruction(Opcodes.CMOVGE, raReg, raReg, ir));
      genRegToReg(ir, reg);
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

    // Append the label that will be used by other routines in this
    // call graph to call this routine.

    appendLabel(routineLabel.get(currentRoutine));

    // Lay out the arguments in memory.

    if (rt.isAggregateType() || rt.isArrayType()) {
      // If this routine returns a struct it actually stores the
      // struct at the address passed as the first argument to this routine.
      structAddress = registers.newTempRegister(RegisterSet.AIREG);
      structSize = rt.memorySizeAsInt(machine);
      genRegToReg(AlphaRegisterSet.IF_REG + nextArg++, structAddress);
    }
 
    // Move each argument to the location it will have during the
    // execution of the function.  The layoutParameters method has
    // already determined where each argument will be kept.  This
    // method moves it from the argument passing registers to that
    // place.

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      Type       vt = processType(fd);

      if (fd instanceof UnknownFormals)
        // This function must use va_start to get the rest and they
        // will be placed on the stack by endRoutineCode.
        break;

      int      ts  = vt.memorySizeAsInt(machine);
      Assigned loc = fd.getStorageLoc();

      if (loc == Assigned.IN_REGISTER) {

        // Move the argument from the real register it was p[assed in
        // to the virtual register it was assigned to.

        int        reg  = fd.getValueRegister();
        ResultMode mode = fd.valueRegMode();

        if (mode == ResultMode.STRUCT_VALUE) {  // Argument is a struct and kept in the registers.
          int src  = AlphaRegisterSet.IF_REG;
          while (ts > 0) {
            genRegToReg(src + nextArg++, reg++);
            ts -= AlphaRegisterSet.IREG_SIZE;
          }
          continue;
        }

        if (vt.isRealType()) { // Argumment is a floating point value.
          genRegToReg(AlphaRegisterSet.FF_REG + nextArg, reg);
          if (vt.isComplexType()) {
            genRegToReg(AlphaRegisterSet.FF_REG + nextArg + 1, reg + 1);
            nextArg++;
          }
          nextArg++;
          continue;
        }

        // Integer arguments may need to be sign extended.
        // For example, if the function where defined by the caller using K&R form of
        //   extern void ftn();
        // but the real definition is
        //   void ftn(unsigned char x) {...}
        // we must make certain that only the bottom 8-bits are used.

        int treg = convertIntRegValue(AlphaRegisterSet.IF_REG + nextArg,
                                      AlphaRegisterSet.IREG_SIZE,
                                      true,
                                      reg,
                                      ts,
                                      vt.isSigned());
        genRegToReg(treg, reg);
        nextArg++;
        continue;
      }

      if (usesVaStart)
        // If the routine uses va_start endRoutineCode will generate
        // instructions to place the arguments on the stack.
        continue;

      assert  (loc == Assigned.ON_STACK) : "Argument is where " + fd;
      // Probably an argument whose address is taken so it must be
      // kept in memory.
      Displacement disp = fd.getDisplacement();

      if (vt.isAtomicType()) {
        // A simple store will do to place it on the stack so that
        // its address may be taken.
        if (nextArg < MAX_ARG_REGS) {
          // The caller did not place it on the stack so we have to.
          if (vt.isRealType())
            storeIntoMemory(AlphaRegisterSet.FF_REG + nextArg, stkPtrReg, disp, ts, ts);
          else
            storeIntoMemory(AlphaRegisterSet.IF_REG + nextArg, stkPtrReg, disp, ts, ts);
          nextArg++;
        }
        continue;
      }

      assert vt.isAggregateType() : "Argument is where " + fd;

      // Structs are passed in registers but placed in memory
      // unless thay are small.
      int dsize  = vt.memorySizeAsInt(machine);
      int offset = 0;

      // Move any words of the struct to the stack unless the call
      // already put them there.

      for (int j = 0; j < dsize; j += AlphaRegisterSet.IREG_SIZE) {
        if (nextArg < MAX_ARG_REGS) {
          Displacement disp2 = disp.offset(offset);
          entryOverflow.addElement(disp2);
          Instruction inst = new StoreInstruction(Opcodes.STQ,
                                                  AlphaRegisterSet.IF_REG + nextArg,
                                                  stkPtrReg,
                                                  disp2);
          appendInstruction(inst);
          offset += AlphaRegisterSet.IREG_SIZE;
          nextArg++;
        }
      }
    }

    lastInstruction.specifySpillStorePoint(); // Specify an end of a sequnce for use by the spilling logic.

    // We need to insure that there is at least one label in this
    // routine that is after the prolog and before the return.  This
    // label is used by the endRoutineCode method to insert the
    // epilog.

    lastLabel = createLabel();
    appendLabel(lastLabel);
    lastLabel.setNotReferenced();
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    Type ct  = processType(e);
    int  bs  = ct.memorySizeAsInt(machine);
    Expr arg = e.getArg();
    int  ir  = registers.getResultRegister(RegisterSet.INTREG);

    needValue(arg);
    int src = resultReg;

    assert !registers.floatRegister(src) : "Bit complement not allowed on " + arg;

    appendInstruction(new IntOpInstruction(Opcodes.EQV, src, AlphaRegisterSet.I0_REG, ir));
    if ((bs <= 4) && !ct.isSigned())
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, ir, (1 << bs) - 1, ir));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

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
    boolean simple = true; // True if only simple arguments.
    for (int i = 0; i < args.length; i++) {
      Expr arg = args[i];
      if (isSimple(arg))
        continue;

      simple = false;
      break;
    }

    int[]   amap = new int[MAX_ARG_REGS];   // These arrays must be allocated each time 
    short[] umap = new short[MAX_ARG_REGS]; // because of possible recursion of callArgs.

    int rk      = 0;
    int nextArg = 0;
    int offset  = 0;

    if (retStruct) {
      amap[0] = AlphaRegisterSet.IF_REG;
      umap[0] = (short) AlphaRegisterSet.IF_REG;
      rk = 1;
      nextArg = 1;
    }

    int stkpos = 0;
    int ai     = 0;
    while (nextArg < MAX_ARG_REGS) {
      if (ai >= args.length)
        break;

      Expr arg  = args[ai++];
      Type vt   = processType(arg);
      int  size = vt.memorySizeAsInt(machine);

      if (trace)
        System.out.println("CFA: " + arg);

      if (vt.isAtomicType()) {
        if (vt.isComplexType()) { // Treat complex as structs for intrinsic routines.
          int nexr = AlphaRegisterSet.FF_REG + nextArg;
          needValue(arg);
          amap[rk] = resultReg;
          umap[rk] = (short) nexr;
          nextArg++;
          rk++;

          if (nextArg < MAX_ARG_REGS) {
            amap[rk] = resultReg + 1;
            umap[rk] = (short) (nexr + 1);
            rk++;
            nextArg++;
          } else {
            storeIntoMemory(resultReg + 1,
                            AlphaRegisterSet.SP_REG,
                            getDisp(offset),
                            RT_NONE,
                            size / 2,
                            size / 2);
            offset += AlphaRegisterSet.FREG_SIZE;
          }
          continue;
        }

        int nexr = AlphaRegisterSet.IF_REG + nextArg;
        if (vt.isRealType())
          nexr = AlphaRegisterSet.FF_REG + nextArg;

        if (simple)
          registers.setResultRegister(nexr);

        needValue(arg);

        if (simple)
          registers.setResultRegister(-1);

        // Postpone transfer because of things like divide
        // which may cause a subroutine call.

        amap[rk] = resultReg;
        umap[rk] = (byte) nexr;
        rk++;
        nextArg++;
        continue;
      }

      if (vt.isAggregateType()) {
        arg.visit(this);
        int  address    = resultReg;
        long addressoff = resultRegAddressOffset;
        int  addressaln = resultRegAddressAlignment;

        if (resultRegMode == ResultMode.STRUCT_VALUE) {
          for (int k = 0; k < size; k += AlphaRegisterSet.IREG_SIZE) {
            if (nextArg < MAX_ARG_REGS) {
              int nexr = AlphaRegisterSet.IF_REG + nextArg;
              int areg = address++;
              amap[rk] = areg;
              umap[rk] = (byte) nexr;
              rk++;
              nextArg++;
            } else {
              int nexr = address++;
              storeIntoMemory(nexr,
                              AlphaRegisterSet.SP_REG,
                              getDisp(stkpos), RT_NONE,
                              AlphaRegisterSet.IREG_SIZE,
                              0);
              offset += AlphaRegisterSet.IREG_SIZE;
              stkpos += AlphaRegisterSet.IREG_SIZE;
            }
          }
          continue;
        }

        assert (resultRegMode == ResultMode.ADDRESS) :
          "Huh " + resultRegMode + " " + arg + " " + arg.getChord();

        int soffset = 0;
        if ((addressoff >= (MAX_IMM16 - size)) || (addressoff <= (MIN_IMM16 + size))) {
          int sr = registers.newTempRegister(RegisterSet.INTREG);
          genLoadImmediate(addressoff, address, sr);
          address = sr;
        } else
          soffset = (int) addressoff;

        for (int k = 0; k < size; k += AlphaRegisterSet.IREG_SIZE) {
          Displacement odisp2 = getDisp(k + soffset);
          if (nextArg < MAX_ARG_REGS) {
            int nexr = AlphaRegisterSet.IF_REG + nextArg;
            int areg = simple ? nexr :  registers.newTempRegister(RegisterSet.INTREG);
            loadFromMemory(areg, address, odisp2, AlphaRegisterSet.IREG_SIZE, addressaln, false);
            amap[rk] = areg;
            umap[rk] = (byte) nexr;
            rk++;
            nextArg++;
          } else {
            int nexr = registers.newTempRegister(RegisterSet.AIREG);
            loadFromMemory(nexr, address, odisp2, AlphaRegisterSet.IREG_SIZE, addressaln, false);
            appendInstruction(new StoreInstruction(Opcodes.STQ,
                                                   nexr,
                                                   AlphaRegisterSet.SP_REG,
                                                   getDisp(stkpos)));
            offset += AlphaRegisterSet.IREG_SIZE;
            stkpos += AlphaRegisterSet.IREG_SIZE;
          }
        }
        continue;
      }

      assert vt.isArrayType() : "Argument type " + arg;

      LoadDeclValueExpr ldve = (LoadDeclValueExpr) arg;
      VariableDecl      ad   = (VariableDecl) ldve.getDecl();
      int               nexr = AlphaRegisterSet.IF_REG + nextArg;

      if (simple)
        registers.setResultRegister(nexr);

      putAddressInRegister(ad, false);

      if (simple)
        registers.setResultRegister(-1);

      amap[rk] = resultReg;
      umap[rk] = (byte) nexr;
      rk++;
      nextArg++;
    }

    while (ai < args.length) {
      Expr arg    = args[ai++];
      Type vt     = processType(arg);
      int  size   = vt.memorySizeAsInt(machine);
      int  rinc   = 1;

      if (vt.isAtomicType()) {
        needValue(arg);

        if (size < 4)
          size = 4;

        storeIntoMemory(resultReg, AlphaRegisterSet.SP_REG, getDisp(stkpos), size, size);
        offset += Machine.alignTo(size, AlphaRegisterSet.IREG_SIZE);
        stkpos += SAVED_REG_SIZE;
        continue;
      }

      AggregateType at = vt.returnAggregateType();
      if (at != null) {
        arg.visit(this);
        int  address    = resultReg;
        long addressoff = resultRegAddressOffset;
        int  addressaln = resultRegAddressAlignment;

        if (resultRegMode == ResultMode.STRUCT_VALUE) {
          for (int k = 0; k < size; k += AlphaRegisterSet.IREG_SIZE) {
            int nexr = address++;
            storeIntoMemory(nexr,
                            AlphaRegisterSet.SP_REG,
                            getDisp(stkpos), RT_NONE,
                            AlphaRegisterSet.IREG_SIZE,
                            AlphaRegisterSet.IREG_SIZE);
            offset += AlphaRegisterSet.IREG_SIZE;
            stkpos += AlphaRegisterSet.IREG_SIZE;
          }
          continue;
        }

        assert (resultRegMode == ResultMode.ADDRESS) : "Huh " + arg;

        int soffset = 0;
        if ((addressoff >= (MAX_IMM16 - size)) || (addressoff <= (MIN_IMM16 + size))) {
          int sr = registers.newTempRegister(RegisterSet.INTREG);
          genLoadImmediate(addressoff, address, sr);
          address = sr;
        } else
          soffset = (int) addressoff;

        for (int k = 0; k < size; k += AlphaRegisterSet.IREG_SIZE) {
          int nexr = registers.newTempRegister(RegisterSet.AIREG);
          loadFromMemory(nexr,
                         address,
                         getDisp(k + soffset),
                         AlphaRegisterSet.IREG_SIZE,
                         addressaln,
                         false);
          appendInstruction(new StoreInstruction(Opcodes.STQ,
                                                 nexr,
                                                 AlphaRegisterSet.SP_REG,
                                                 getDisp(stkpos)));
          offset += AlphaRegisterSet.IREG_SIZE;
          stkpos += AlphaRegisterSet.IREG_SIZE;
        }
        continue;
      }

      assert vt.isArrayType() : "Argument type " + arg;

      LoadDeclValueExpr ldve = (LoadDeclValueExpr) arg;
      VariableDecl      ad   = (VariableDecl) ldve.getDecl();

      putAddressInRegister(ad, false);

      appendInstruction(new StoreInstruction(Opcodes.STQ,
                                             resultReg,
                                             AlphaRegisterSet.SP_REG,
                                             getDisp(stkpos)));
      offset += AlphaRegisterSet.IREG_SIZE;
      stkpos += SAVED_REG_SIZE;
    }

    // Move arguments to the register they are passed in if necessary.

    for (int i = 0; i < rk; i++)
      genRegToReg(amap[i], umap[i]);

    if (offset > argBuildSize)
      argBuildSize = offset;

    // Create the set of used registers for this function call.

    short[] uses = new short[rk + (usesAlloca ? 3 : 2)];
    System.arraycopy(umap, 0, uses, 0, rk);
    uses[rk + 0] = AlphaRegisterSet.SP_REG;
    uses[rk + 1] = AlphaRegisterSet.GP_REG;
    if (usesAlloca)
      uses[rk + 2] = AlphaRegisterSet.FP_REG;

    return uses;
  }

  /**
   * This method generates instructions to call a sub-function.  It
   * places arguments in the appropriate registers or on the stack as
   * required.
   */
  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    StackDisplacement returnDisp = null; // For functions that return structure values.

    Type         rt         = processType(e);
    Expr[]       args       = e.getArgumentArray();
    Displacement d8         = getDisp(AlphaRegisterSet.IREG_SIZE);
    Expr         ftn        = e.getFunction();
    Declaration  decl       = null;
    boolean      rest       = true; // GP register must be restored.
    boolean      retStruct  = false;
    int          ir         = registers.getResultRegister(rt.getTag());

    // Check if there is an intrinsic this function could be replaced with.
    
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
          genAlloca(args[0], ir);
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

    if (!rt.isVoidType() && !rt.isAtomicType()) {

      // If the routine returns a structure, it is passed through
      // memory and the memory address to use is the first argument.

      returnDisp = new StackDisplacement(localVarSize);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, 
                                                   AlphaRegisterSet.IF_REG,
                                                   stkPtrReg,
                                                   returnDisp));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH,
                                                   AlphaRegisterSet.IF_REG,
                                                   AlphaRegisterSet.IF_REG, returnDisp));
      localVarSize += Machine.alignTo(rt.memorySizeAsInt(machine), SAVED_REG_SIZE);
      localVar.addElement(returnDisp);
      retStruct = true;
    }

    Instruction li   = lastInstruction;
    short[]     uses = callArgs(args, retStruct);

    // If the calling routine uses __builtin_alloca(), then space must
    // be reserved for any arguments that can't be passed in
    // registers.  The value in argBuildSize will always be large
    // enough but may be larger than needed.  The effort to use the
    // exactly right size for this call is just more than I can bear
    // at this time.

    boolean insertSPmod = usesAlloca && (argBuildSize > 0);
    if (insertSPmod)
      insertInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   AlphaRegisterSet.SP_REG,
                                                   AlphaRegisterSet.SP_REG,
                                                   getDisp(-argBuildSize)),
                        li);

    // Determine the type of call - direct or indirect.

    Branch jsr;
    if (decl == null) { // Indirection requires JSR instruction.

      registers.setResultRegister(AlphaRegisterSet.PV_REG);

      ftn.visit(this);
      int src     = resultReg;
      int soffset = 0;

      if ((resultRegAddressOffset >= MAX_IMM16) || (resultRegAddressOffset <= MIN_IMM16)) {
        int sr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(resultRegAddressOffset, src, sr);
        src = sr;
      } else
         soffset = (int) resultRegAddressOffset;

      registers.setResultRegister(-1);

      genRegToReg(src, AlphaRegisterSet.PV_REG);
      lastInstruction.specifySpillStorePoint();

      jsr = new JmpInstruction(Opcodes.JSR,
                               AlphaRegisterSet.RA_REG,
                               AlphaRegisterSet.PV_REG,
                               getDisp(soffset), 1);
    } else {
      RoutineDecl rd = (RoutineDecl) decl;

      lastInstruction.specifySpillStorePoint();

      if (decl.visibility() == Visibility.EXTERN) { // Use JSR for external routines
        addrDisp = rd.getDisplacement().unique();

        appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                              AlphaRegisterSet.PV_REG,
                                              AlphaRegisterSet.GP_REG,
                                              addrDisp, RT_LITERAL));
        usesGp = true;

        jsr = new JmpInstruction(Opcodes.JSR,
                                 AlphaRegisterSet.RA_REG,
                                 AlphaRegisterSet.PV_REG,
                                 addrDisp,
                                 1);

      } else { // Use BSR for this routine.
        Displacement dispc = new LabelDisplacement(routineLabel.get(rd));
        jsr = new BranchInstruction(Opcodes.BSR, AlphaRegisterSet.RA_REG, dispc, 1);
        rest = false;
      }
    }

    callsRoutine = true;

    if (rt.isVoidType()) {  // No return value.
      appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), null, true);

      if (rest)
        restoreGpReg();

      if (insertSPmod)
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                     AlphaRegisterSet.SP_REG,
                                                     AlphaRegisterSet.SP_REG,
                                                     getDisp(argBuildSize)));
      return;
    }

    resultRegAddressOffset = 0;

    if (!rt.isAtomicType()) { // Returning structs is a pain.
      appendCallInstruction(jsr, createLabel(), uses, registers.getCalleeUses(), null, true);
      if (rest)
        restoreGpReg();
      if (insertSPmod)
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                     AlphaRegisterSet.SP_REG,
                                                     AlphaRegisterSet.SP_REG,
                                                     getDisp(argBuildSize)));

      int vr = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA, vr, stkPtrReg, returnDisp));
      appendInstruction(new LoadAddressInstruction(Opcodes.LDAH, vr, vr, returnDisp));
      resultRegMode = ResultMode.ADDRESS;
      resultReg = vr;
      resultRegAddressAlignment = 8;
      return;
    }

    resultRegMode = ResultMode.NORMAL_VALUE;

    if (!rt.isRealType()) { // Returning a non-real atomic value (e.g., int).
      appendCallInstruction(jsr,
                            createLabel(),
                            uses,
                            registers.getCalleeUses(),
                            intReturn,
                            true);
      if (rest)
        restoreGpReg();
      if (insertSPmod)
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                     AlphaRegisterSet.SP_REG,
                                                     AlphaRegisterSet.SP_REG,
                                                     getDisp(argBuildSize)));

      resultReg = AlphaRegisterSet.IR_REG;
      return;
    }

    if (!rt.isComplexType()) { // Returning a complex value.
      appendCallInstruction(jsr,
                            createLabel(),
                            uses,
                            registers.getCalleeUses(),
                            realReturn,
                            true);
      if (rest)
        restoreGpReg();

      if (insertSPmod)
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                     AlphaRegisterSet.SP_REG,
                                                     AlphaRegisterSet.SP_REG,
                                                     getDisp(argBuildSize)));
      resultReg = AlphaRegisterSet.FR_REG;
      return;
    }

    // Returning a real value.

    appendCallInstruction(jsr,
                          createLabel(),
                          uses,
                          registers.getCalleeUses(),
                          complexReturn, true);
    if (rest)
      restoreGpReg();
    if (insertSPmod)
      appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                   AlphaRegisterSet.SP_REG,
                                                   AlphaRegisterSet.SP_REG,
                                                   getDisp(argBuildSize)));
    genRegToReg(AlphaRegisterSet.FR_REG, ir);
    genRegToReg(AlphaRegisterSet.FR_REG + 1, ir + 1);
    resultReg = ir;
  }

  private void gen3WayFltCompare(int laReg, int raReg, int tmp2, int dest)
  {
    int               temp   = registers.newTempRegister(RegisterSet.FLTREG);
    Label             labeq  = createLabel();
    Label             labne  = createLabel();
    Label             lablt  = createLabel();
    LabelDisplacement dispeq = new LabelDisplacement(labeq);
    Branch            insteq = new BranchInstruction(Opcodes.FBEQ, temp, dispeq, 2);
    Branch            instgt = new BranchInstruction(Opcodes.FBGT, temp, dispeq, 2);

    insteq.addTarget(labeq, 0);
    insteq.addTarget(labne, 1);
    instgt.addTarget(labeq, 0);
    instgt.addTarget(lablt, 1);

    appendInstruction(new FltOpInstruction(Opcodes.SUBT, laReg, raReg, temp));
    appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.I0_REG, 0, dest));
    appendInstruction(insteq);
    appendLabel(labne);
    labne.setNotReferenced();
    appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.I0_REG, 1, dest));
    appendInstruction(instgt);
    appendLabel(lablt);
    lablt.setNotReferenced();
    appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, AlphaRegisterSet.I0_REG, 1, dest));
    appendLabel(labeq);
  }

  private void gen3WayIntCompare(int laReg, int raReg, int temp, int dest)
  {
    assert ((dest != laReg) && (dest != raReg)) : "gen3WayIntCompare!!!";

    appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.I0_REG, 1, dest));
    appendInstruction(new IntOpInstruction(Opcodes.CMPLT, laReg, raReg, temp)); 
    appendInstruction(new IntOpLitInstruction(Opcodes.CMOVNE, temp, 0, dest)); 
    appendInstruction(new IntOpInstruction(Opcodes.CMPLT, raReg, laReg, temp)); 
    appendInstruction(new IntOpLitInstruction(Opcodes.CMOVNE, temp, 2, dest)); 
    appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ, dest, 1, dest));
  }

  public void visitCompareExpr(CompareExpr e)
  {
    Expr la   = e.getLeftArg();
    Expr ra   = e.getRightArg();
    Type rt   = processType(la);
    int  mode = e.getMode();
    int  ir   = registers.getResultRegister(RegisterSet.AIREG);

    needValue(la);
    int laReg = resultReg;
    needValue(ra);
    int raReg = resultReg;

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;

    switch (mode) {
    case CompareExpr.Normal:    /* Use normal compare.     */
      int tmp2 = registers.newTempRegister(RegisterSet.AIREG);
      if (rt.isRealType()) {
        gen3WayFltCompare(laReg, raReg, tmp2, ir);
        resultReg = ir;
        return;
      } else {
        gen3WayIntCompare(laReg, raReg, tmp2, ir);
        resultReg = ir;
        return;
      }
    case CompareExpr.FloatL: // If either argument is Nan, return -1.      
      break;
    case CompareExpr.FloatG: // If either argument is Nan, return 1.      
      break;
    }

    throw new scale.common.NotImplementedError("CompareExpr not converted for floating point " + e);
  }

  private static final int DBYTE   = 0;
  private static final int DSHORT  = 1;
  private static final int DINT    = 2;
  private static final int DLONG   = 3;
  private static final int SBYTE   = DBYTE  << 2;
  private static final int SSHORT  = DSHORT << 2;
  private static final int SINT    = DINT   << 2;
  private static final int SLONG   = DLONG  << 2;
  private static final int SSIGNED = 1 << 4;
  private static final int DSIGNED = 1 << 5;
  private static final int[] smapSize = {-1, SBYTE, SSHORT, -1, SINT, -1, -1, -1, SLONG};
  private static final int[] dmapSize = {-1, DBYTE, DSHORT, -1, DINT, -1, -1, -1, DLONG};
  private static final int RSRC    = 0; // Return source
  private static final int EBEXT   = 1; // Extract byte & sign extend
  private static final int ESEXT   = 2; // Extract short & sign extend
  private static final int EIEXT   = 3; // Extract int & sign extend
  private static final int EUB     = 4; // Extract unsigned byte
  private static final int EUS     = 5; // Extract unsigned short
  private static final int EUI     = 6; // Extract unsigned int

  private static final byte[] ccase = {
    RSRC, //                     SBYTE  + DBYTE
    RSRC, //                     SBYTE  + DSHORT
    RSRC, //                     SBYTE  + DINT
    RSRC, //                     SBYTE  + DLONG
    EUB,  //                     SSHORT + DBYTE
    RSRC, //                     SSHORT + DSHORT
    RSRC, //                     SSHORT + DINT
    RSRC, //                     SSHORT + DLONG
    EUB,  //                     SINT   + DBYTE
    EUS,  //                     SINT   + DSHORT
    RSRC, //                     SINT   + DINT
    RSRC, //                     SINT   + DLONG
    EUB,  //                     SLONG  + DBYTE
    EUS,  //                     SLONG  + DSHORT
    EUI,  //                     SLONG  + DINT
    RSRC, //                     SLONG  + DLONG

    EUB,  // SSIGNED           + SBYTE  + DBYTE
    EUS,  // SSIGNED           + SBYTE  + DSHORT
    EUI,  // SSIGNED           + SBYTE  + DINT
    RSRC, // SSIGNED           + SBYTE  + DLONG
    EUB,  // SSIGNED           + SSHORT + DBYTE
    EUS,  // SSIGNED           + SSHORT + DSHORT
    EUI,  // SSIGNED           + SSHORT + DINT
    RSRC, // SSIGNED           + SSHORT + DLONG
    EUB,  // SSIGNED           + SINT   + DBYTE
    EUS,  // SSIGNED           + SINT   + DSHORT
    EUI,  // SSIGNED           + SINT   + DINT
    RSRC, // SSIGNED           + SINT   + DLONG
    EUB,  // SSIGNED           + SLONG  + DBYTE
    EUS,  // SSIGNED           + SLONG  + DSHORT
    EUI,  // SSIGNED           + SLONG  + DINT
    RSRC, // SSIGNED           + SLONG  + DLONG

    EBEXT, //           DSIGNED + SBYTE  + DBYTE
    RSRC,  //           DSIGNED + SBYTE  + DSHORT
    RSRC,  //           DSIGNED + SBYTE  + DINT
    RSRC,  //           DSIGNED + SBYTE  + DLONG
    EBEXT, //           DSIGNED + SSHORT + DBYTE
    ESEXT, //           DSIGNED + SSHORT + DSHORT
    RSRC,  //           DSIGNED + SSHORT + DINT
    RSRC,  //           DSIGNED + SSHORT + DLONG
    EBEXT, //           DSIGNED + SINT   + DBYTE
    ESEXT, //           DSIGNED + SINT   + DSHORT
    EIEXT, //           DSIGNED + SINT   + DINT
    RSRC,  //           DSIGNED + SINT   + DLONG
    EBEXT, //           DSIGNED + SLONG  + DBYTE
    ESEXT, //           DSIGNED + SLONG  + DSHORT
    EIEXT, //           DSIGNED + SLONG  + DINT
    RSRC,  //           DSIGNED + SLONG  + DLONG

    RSRC,  // SSIGNED + DSIGNED + SBYTE  + DBYTE
    RSRC,  // EBEXT, // SSIGNED + DSIGNED + SBYTE  + DSHORT
    RSRC,  // EBEXT, // SSIGNED + DSIGNED + SBYTE  + DINT
    RSRC,  // EBEXT, // SSIGNED + DSIGNED + SBYTE  + DLONG
    EBEXT, // SSIGNED + DSIGNED + SSHORT + DBYTE
    RSRC,  // SSIGNED + DSIGNED + SSHORT + DSHORT
    RSRC,  // ESEXT, // SSIGNED + DSIGNED + SSHORT + DINT
    RSRC,  // ESEXT, // SSIGNED + DSIGNED + SSHORT + DLONG
    EBEXT, // SSIGNED + DSIGNED + SINT   + DBYTE
    ESEXT, // SSIGNED + DSIGNED + SINT   + DSHORT
    RSRC,  // SSIGNED + DSIGNED + SINT   + DINT
    RSRC,  // EIEXT, // SSIGNED + DSIGNED + SINT   + DLONG
    EBEXT, // SSIGNED + DSIGNED + SLONG  + DBYTE
    ESEXT, // SSIGNED + DSIGNED + SLONG  + DSHORT
    EIEXT, // SSIGNED + DSIGNED + SLONG  + DINT
    RSRC,  // SSIGNED + DSIGNED + SLONG  + DLONG
  };

  /**
   * Generate instructions to convert an integer value in an integer
   * register to an integer value of a different size.  The source and
   * destination may be the same register.  This logic assumes that
   * the value in the source register conforms to the specified type.
   * @param src is the register containing the source value
   * @param srcSize is the source value size
   * @param srcSigned is true if the source value is signed
   * @param dest is the register containing the result
   * @param destSize is the size of the result value
   * @param destSigned is true if the result value is signed
   * @return the register containing the converted value
   */
  protected int convertIntRegValue(int     src,
                                   int     srcSize,
                                   boolean srcSigned,
                                   int     dest,
                                   int     destSize,
                                   boolean destSigned)
  {
    if (src == AlphaRegisterSet.I0_REG)
      return src;

    int which = smapSize[srcSize] + dmapSize[destSize];
    if (srcSigned)
      which += SSIGNED;
    if (destSigned)
      which += DSIGNED;

    switch (ccase[which]) {
    case RSRC: // Return source
      return src;
    case EBEXT: // Extract byte & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.EXTQH, src, 1, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, 56, dest));
      return dest;
    case ESEXT: // Extract short & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.EXTQH, src, 2, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, 48, dest));
      return dest;
    case EIEXT: // Extract int & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDL, src, 0, dest));
      return dest;
    case EUB: // Extract unsigned byte
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 1, dest));
      return dest;
    case EUS: // Extract unsigned short
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 3, dest));
      return dest;
    case EUI: // Extract unsigned int
      appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, src, 15, dest));
      return dest;
    }
    throw new scale.common.InternalError("Funny conversion " + which);
  }

  /**
   * Generate code to zero out a floating point register.
   */
  protected void zeroFloatRegister(int dest, int destSize)
  {
    appendInstruction(new FltOpInstruction(Opcodes.CPYS,
                                           AlphaRegisterSet.F0_REG,
                                           AlphaRegisterSet.F0_REG,
                                           dest));
  }

  /**
   * Generate code to obtain the real part of a complex value.
   */
  protected void genRealPart(int src, int srcSize, int dest, int destSize)
  {
    appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, src, dest));
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
    int ft = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int it = dest;

    if (!destSigned || (destSize != AlphaRegisterSet.IREG_SIZE)) {
      int rt = registers.tempRegisterType(RegisterSet.INTREG, AlphaRegisterSet.IREG_SIZE);
      it = registers.newTempRegister(rt);
    }

    appendInstruction(new FltCvtInstruction(Opcodes.CVTTQC, src, ft));
    genRegToReg(ft, it);

    if (it == dest)
      return dest;

    return convertIntRegValue(it,
                              AlphaRegisterSet.IREG_SIZE,
                              true,
                              dest,
                              destSize,
                              destSigned);
  }

  /**
   * Convert a real value in a real register to a 
   * real value in a real register.
   */
  protected void genRealToReal(int src, int srcSize, int dest, int destSize)
  {
    if ((destSize == 4) && (srcSize == 8))
      appendInstruction(new FltCvtInstruction(Opcodes.CVTTS, src, dest));
    else
      appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, src, dest));
  }

  /**
   * Convert real value in a real register to an integer value
   * in a real register.  The result is rounded  to the nearest integer.
   */
  protected void genRealToIntRound(int src, int srcSize, int dest, int destSize)
  {
    int t2 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int t4 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int a1 = registers.newTempRegister(RegisterSet.ADRREG);
    int a2 = registers.newTempRegister(RegisterSet.ADRREG);
    Displacement pt5Disp = defLongValue(0x3f000000, 4);

    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          a1,
                                          AlphaRegisterSet.GP_REG,
                                          pt5Disp,
                                          RT_LITERAL));
    appendInstruction(new LoadInstruction(Opcodes.LDS,
                                          t2,
                                          a1,
                                          pt5Disp,
                                          RT_LITUSE_BASE));

    appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, t2, t2));

    if (srcSize > 4)
      appendInstruction(new FltOpInstruction(Opcodes.ADDTC, src, t2, t4));
    else
      appendInstruction(new FltOpInstruction(Opcodes.ADDSC, src, t2, t4));

    appendInstruction(new FltCvtInstruction(Opcodes.CVTTQC, t4, dest));
  }

  /**
   * Convert real value in a real register to a rounded real value
   * in a real register.  The result is rounded to the nearest integer.
   */
  protected void genRoundReal(int src, int srcSize, int dest, int destSize)
  {
    int t1 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int t2 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int t3 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int a1 = registers.newTempRegister(RegisterSet.ADRREG);
    int a2 = registers.newTempRegister(RegisterSet.ADRREG);
    Displacement pt5Disp = defLongValue(0x3f000000, 4);
    Displacement fnDisp  = defLongValue(0x5f000000, 4);

    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          a1,
                                          AlphaRegisterSet.GP_REG,
                                          pt5Disp,
                                          RT_LITERAL));
    appendInstruction(new LoadInstruction(Opcodes.LDS,
                                          t2,
                                          a1,
                                          pt5Disp,
                                          RT_LITUSE_BASE));
    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          a2,
                                          AlphaRegisterSet.GP_REG,
                                          fnDisp,
                                          RT_LITERAL));
    appendInstruction(new LoadInstruction(Opcodes.LDS,
                                          t3,
                                          a2,
                                          fnDisp,
                                          RT_LITUSE_BASE));

    appendInstruction(new FltOpInstruction(Opcodes.CPYS,
                                           AlphaRegisterSet.F0_REG,
                                           src,
                                           t1));
    appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, t2, t2));

    if (srcSize > 4)
      appendInstruction(new FltOpInstruction(Opcodes.ADDTC, src, t2, dest));
    else
      appendInstruction(new FltOpInstruction(Opcodes.ADDSC, src, t2, dest));

    appendInstruction(new FltCvtInstruction(Opcodes.CVTTQC, dest, t2));
    appendInstruction(new FltOpInstruction(Opcodes.CMPTLT, t1, t3, t3));
    appendInstruction(new FltCvtInstruction(Opcodes.CVTQT, t2, t2));
    appendInstruction(new FltOpInstruction(Opcodes.FCMOVNE, t3, t2, dest));
  }

  /**
   * Convert an integer value in an integer register to a 
   * real value in a real register.
   */
  protected void genIntToReal(int src, int srcSize, int dest, int destSize)
  {
    int treg = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));

    genRegToReg(src, treg);
    appendInstruction(new FltCvtInstruction(Opcodes.CVTQT, treg, dest));
  }

  /**
   * Convert an unsigned integer value in an integer register to a 
   * real value in a real register.
   */
  protected void genUnsignedIntToReal(int src, int srcSize, int dest, int destSize)
  {
    int treg = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));

    if (srcSize < AlphaRegisterSet.IREG_SIZE) {
      int rt   = registers.tempRegisterType(RegisterSet.INTREG, AlphaRegisterSet.IREG_SIZE);
      int ireg = registers.newTempRegister(rt);
      int creg = convertIntRegValue(src, srcSize, false, ireg, AlphaRegisterSet.IREG_SIZE, true);
      genRegToReg(creg, treg);
      int op = destSize > 4 ? Opcodes.CVTQT : Opcodes.CVTQS;
      appendInstruction(new FltCvtInstruction(op, treg, dest));
      return;
    }

    int               rt    = registers.tempRegisterType(RegisterSet.FLTREG, srcSize);
    int               treg2 = registers.newTempRegister(rt);
    int               treg3 = registers.newTempRegister(rt);
    int               adr   = registers.newTempRegister(RegisterSet.ADRREG);
    Label             labt  = createLabel();
    Label             labf  = createLabel();
    Label             labj  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);
    Branch            br    = new BranchInstruction(Opcodes.BGE, src, disp, 2);
    Displacement      dispv = defLongValue(0x5F800000, 4);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);

    genRegToReg(src, treg);
    appendInstruction(br);
    appendLabel(labf);
    labf.setNotReferenced();
    appendInstruction(new FltOpInstruction(Opcodes.CPYSE,
                                           treg,
                                           AlphaRegisterSet.F0_REG,
                                           treg2));
    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          adr,
                                          AlphaRegisterSet.GP_REG,
                                          dispv,
                                          RT_LITERAL));
    appendInstruction(new FltOpInstruction(Opcodes.CPYSE,
                                           AlphaRegisterSet.F0_REG,
                                           treg,
                                           treg));
    appendInstruction(new LoadInstruction(Opcodes.LDS,
                                          treg3,
                                          adr,
                                          dispv,
                                          RT_LITUSE_BASE));
    appendInstruction(new FltCvtInstruction(Opcodes.CVTQT,
                                            treg2,
                                            treg2));
    appendInstruction(new FltCvtInstruction(Opcodes.CVTQT,
                                            treg,
                                            treg));
    appendInstruction(new FltOpInstruction(Opcodes.ADDT,
                                           treg3,
                                           treg2,
                                           treg2));
    appendInstruction(new FltOpInstruction(Opcodes.ADDT,
                                           treg,
                                           treg2,
                                           dest));
    generateUnconditionalBranch(labj);
    appendLabel(labt);

    appendInstruction(new FltCvtInstruction(Opcodes.CVTQT, treg, dest));
    appendLabel(labj);

    usesGp = true;
  }

  protected void genFloorOfReal(int src, int srcSize, int dest, int destSize)
  {
    int          tr1       = registers.getResultRegister(RegisterSet.FLTREG);
    int          tr2       = registers.getResultRegister(RegisterSet.FLTREG);
    int          tr3       = registers.getResultRegister(RegisterSet.FLTREG);
    int          adr       = registers.newTempRegister(RegisterSet.ADRREG);
    Displacement floorDisp = defLongValue(0x5f000000, 4);

    // if (abs(src) < 9.223372036854776e+18)
    //   return (double) (long) src;
    // else
    //   return src;

    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          adr,
                                          AlphaRegisterSet.GP_REG,
                                          floorDisp, RT_LITERAL));
    appendInstruction(new LoadInstruction(Opcodes.LDS,
                                          tr3,
                                          adr,
                                          floorDisp,
                                          RT_LITUSE_BASE));
    usesGp = true;

    appendInstruction(new FltCvtInstruction(Opcodes.CVTTQC, src, tr1));
    appendInstruction(new FltOpInstruction(Opcodes.CPYS, AlphaRegisterSet.F0_REG, src, tr2));
    appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, src, dest));
    appendInstruction(new FltOpInstruction(Opcodes.CMPTLT, tr2, tr3, tr3));
    appendInstruction(new FltCvtInstruction(Opcodes.CVTQT, tr1, tr1));
    appendInstruction(new FltOpInstruction(Opcodes.FCMOVNE, tr3, tr1, dest));
  }

  public void visitExponentiationExpr(ExponentiationExpr e)
  {
    Type    ct = processType(e);
    int     bs = ct.memorySizeAsInt(machine);
    int     ir = registers.getResultRegister(ct.getTag());
    Expr    la = e.getLeftArg();
    Expr    ra = e.getRightArg();
    boolean cmplx = ct.isComplexType();

    if (ct.isRealType() && ra.isLiteralExpr()) {

      // Determine if it is X**n where n is an integer constant.

      LiteralExpr le    = (LiteralExpr) ra;
      Literal     lit   = le.getLiteral().getConstantValue();
      boolean     flag  = false;
      long        value = 0;

      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        value = il.getLongValue();
        flag = true;
      } else if (lit instanceof CharLiteral) {
        value = ((CharLiteral) lit).getCharacterValue();
        flag = true;
      }

      if (flag && (value < 8) && (value > 0)) {
        needValue(la);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        if (value == 1) { // X**1
          appendInstruction(new FltOpInstruction(Opcodes.CPYS, resultReg, resultReg, ir));
          resultReg = ir;
          return;
        }
        int opcode = (bs > 4) ? Opcodes.MULT : Opcodes.MULS;
        if (value == 2) { // X**2
          if (cmplx)
            doComplexOp(MUL, bs, resultReg, resultReg, ir);
          else
            appendInstruction(new FltOpInstruction(opcode, resultReg, resultReg, ir));
          resultReg = ir;
          return;
        }
        if (value == 3) { // X**3
          if (cmplx) {
            int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
            doComplexOp(MUL, bs, resultReg, resultReg, tr);
            doComplexOp(MUL, bs, resultReg, tr, ir);
          } else {
            int tr = registers.newTempRegister(RegisterSet.FLTREG);
            appendInstruction(new FltOpInstruction(opcode, resultReg, resultReg, tr));
            appendInstruction(new FltOpInstruction(opcode, resultReg, tr, ir));
          }
          resultReg = ir;
          return;
        }
        if (cmplx) {
          int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
          doComplexOp(MUL, bs, resultReg, resultReg, ir);
          for (int i = 0; i < value - 3; i++)
            doComplexOp(MUL, bs, resultReg, tr, tr);
          doComplexOp(MUL, bs, resultReg, tr, ir);
        } else {
          int tr = registers.newTempRegister(RegisterSet.FLTREG);
          appendInstruction(new FltOpInstruction(opcode, resultReg, resultReg, tr));
          for (int i = 0; i < value - 3; i++)
            appendInstruction(new FltOpInstruction(opcode, resultReg, tr, tr));
          appendInstruction(new FltOpInstruction(opcode, resultReg, tr, ir));
        }
        resultReg = ir;
        return;
      }
    }

    Type         lt     = processType(la);
    Type         rt     = processType(ra);
    StringBuffer ftn    = new StringBuffer("__pow");
    String       suffix = (bs > 4) ? "_e" : "_ef";
    int          rr     = AlphaRegisterSet.FR_REG;

    // Use the __pow function to do the exponentiation.

    if (!ct.isRealType()) {
      ftn.append("ii");
      rr   = AlphaRegisterSet.IR_REG;
      suffix = "_e";
    } else if (!rt.isRealType()) {
      ftn.append("i");
    }
    ftn.append(suffix);

    short[] uses = callArgs(e.getOperandArray(), false);
    genFtnCall(ftn.toString(), uses, null);
    genRegToReg(rr, ir);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    int  ir = registers.getResultRegister(ct.getTag());

    if (ct.isRealType()) {
      doBinaryOp(e, DIV);
      return;
    }

    Expr ra = e.getRightArg();
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
        Expr la = e.getLeftArg();
        needValue(la);
        genDivide(value, resultReg, ir, bs > 4, ct.isSigned());
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    // Use the _OtsDivide function to do the divide.

    short[]  uses = callArgs(e.getOperandArray(), false);
    String rname = (bs > 4) ? "_OtsDivide64" : "_OtsDivide32";
    if (!ct.isSigned())
      rname = rname + "Unsigned";

    genFtnCall(rname, uses, null);
    int tr = convertIntRegValue(AlphaRegisterSet.IR_REG,
                                AlphaRegisterSet.IREG_SIZE,
                                ct.isSigned(),
                                ir,
                                bs,
                                ct.isSigned());
    genRegToReg(tr, ir);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitRemainderExpr(RemainderExpr e)
  {
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    int  ir = registers.getResultRegister(ct.getTag());

    if (ct.isRealType()) {
      putAddressInRegister(e.getLeftArg());
      int lr = resultReg;
      putAddressInRegister(e.getRightArg());
      int rr = resultReg;
      genRegToReg(lr, AlphaRegisterSet.IF_REG);
      genRegToReg(rr, AlphaRegisterSet.IF_REG + 1);
      genFtnCall((bs > 4) ? "r_fmod" : "r_fmodf",
                 genDoubleUse(AlphaRegisterSet.IF_REG, AlphaRegisterSet.IF_REG + 1),
                 null);
      genRegToReg(AlphaRegisterSet.FR_REG, ir);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    Expr ra = e.getRightArg();
    if (ra.isLiteralExpr()) {
      LiteralExpr le    = (LiteralExpr) ra;
      Literal     lit   = le.getLiteral().getConstantValue();
      boolean     flag  = false;
      long        value = 0;

      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        value = il.getLongValue();
        flag = true;
      }

      if (flag) {
        Expr la = e.getLeftArg();
        needValue(la);
        int laReg = resultReg;
        int bits  = Lattice.powerOf2(value);

        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;

        if ((bits > 0) && (bits < 8)) {
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, laReg, (1 << bits) - 1, ir));
          return;
        }

        int tr1 = registers.newTempRegister(RegisterSet.INTREG);
        int tr2 = registers.newTempRegister(RegisterSet.INTREG);

        if (bs > 4) {
          genDivide(value, laReg, tr1, bs > 4, ct.isSigned());
          genMultiplyQuad(value, tr1, tr2);
          appendInstruction(new IntOpInstruction(Opcodes.SUBQ, laReg, tr2, ir));
        } else {
          genDivide(value, laReg, tr1, bs > 4, ct.isSigned());
          genMultiplyLong(value, tr1, tr2);
          appendInstruction(new IntOpInstruction(Opcodes.SUBL, laReg, tr2, ir));
        }
        if (bs <= 4) {
          if (ct.isSigned())
            appendInstruction(new IntOpInstruction(Opcodes.ADDL, ir, AlphaRegisterSet.I0_REG, ir));
          else
            appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, ir, (1 << bs) - 1, ir));
        }
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    short[] uses = callArgs(e.getOperandArray(), false);
    String rname = (bs > 4) ? "_OtsRemainder64" : "_OtsRemainder32";
    if (!ct.isSigned())
      rname = rname + "Unsigned";

    genFtnCall(rname, uses, null);
    int tr = convertIntRegValue(AlphaRegisterSet.IR_REG,
                                AlphaRegisterSet.IREG_SIZE,
                                ct.isSigned(),
                                ir,
                                bs,
                                ct.isSigned());
    genRegToReg(tr, ir);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = tr;
  }

  protected short[] genSingleUse(int reg)
  {
    short[] uses = new short[usesAlloca ? 4 : 3];
    uses[0] = (short) reg;
    uses[1] = AlphaRegisterSet.SP_REG;
    uses[2] = AlphaRegisterSet.GP_REG;
    if (usesAlloca)
      uses[3] = AlphaRegisterSet.FP_REG;
    return uses;
  }

  protected short[] genDoubleUse(int reg1, int reg2)
  {
    short[] uses = new short[usesAlloca ? 5 : 4];
    uses[0] = (short) reg1;
    uses[1] = (short) reg2;
    uses[2] = AlphaRegisterSet.SP_REG;
    uses[3] = AlphaRegisterSet.GP_REG;
    if (usesAlloca)
      uses[4] = AlphaRegisterSet.FP_REG;
    return uses;
  }

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

    if (adrha == ResultMode.STRUCT_VALUE) {
      assert (fieldOffset < 16) : "Field offset too large " + fieldOffset;
      // Structure is in registers, not memory, and is 16 bytes or
      // less in size.

      resultReg = dest;
      resultRegAddressOffset = 0;

      if (ft.isRealType()) {
        // Assume entire struct is in floating point registers and
        // contains only doubles because there is no easy transfer
        // between integer and double registers and there is no
        // conversion from 32-bit single to 64-bit double format that
        // does not go through memory.
        assert (bits == 0) && (byteSize == 8) : "Bit field " + ft;
        int sr = adr + ((fieldOffset >= 8) ? 1 : 0);
        appendInstruction(new FltOpInstruction(Opcodes.CPYS, sr, sr, dest));
        return;
      }

      resultRegMode = ft.isAtomicType() ? ResultMode.NORMAL_VALUE : ResultMode.STRUCT_VALUE;
      resultRegSize = ft.memorySize(machine);

      if (fieldOffset >= 8) {
        fieldOffset -= 8;
        adr++;
      }

      bitOffset += 8 * ((int) fieldOffset);
      if (bits == 0)
        bits = 8 * byteSize;

      if (bits == 64) {
        appendInstruction(new IntOpInstruction(Opcodes.BIS, adr, adr, dest));
        resultRegMode = ResultMode.NORMAL_VALUE;
        return;
      }

      int tr = registers.newTempRegister(RegisterSet.INTREG);
      int sr = adr;
      int ls = 64 - bitOffset - bits;
      if (ls > 0) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, adr, ls, tr));
        sr = tr;
      }
      int op = ft.isSigned() ? Opcodes.SRA : Opcodes.SRL;
      appendInstruction(new IntOpLitInstruction(op, sr, 64 - bits, dest));
      return;
    }

    // Structure is in memory.

    resultRegMode = ResultMode.NORMAL_VALUE;
    if (!ft.isAtomicType()) {
      if (!machine.keepTypeInRegister(ft, true)) {
        resultRegAddressAlignment = (((fieldOffset & 0x7) == 0) ?
                                     8 :
                                     (((fieldOffset & 0x3) == 0) ? 4 : 1));
        resultRegMode = ResultMode.ADDRESS;
        return;
      }
      resultRegMode = ResultMode.STRUCT_VALUE;
      resultRegSize = ft.memorySize(machine);
    }

    resultRegAddressOffset = 0;

    int alignment = naln ? 1 : ((adraln < fa) ? adraln : fa);
    if (bits == 0) {
      Displacement disp;
      if ((fieldOffset >= MIN_IMM16) && (fieldOffset <= MAX_IMM16))
        disp = getDisp((int) fieldOffset);
      else {
        int tr = registers.newTempRegister(RegisterSet.ADRREG);
        genLoadImmediate(fieldOffset, adr, tr);
        disp = getDisp(0);
        adr = tr;
      }

      loadFromMemory(dest, adr, disp, RT_NONE, byteSize, alignment, ft.isSigned());
      resultReg = dest;
      return;
    }

    int tr = registers.newTempRegister(RegisterSet.ADRREG);
    genLoadImmediate(fieldOffset, adr, tr);
    adr = tr;

    loadBitsFromMemory(dest, adr, bits, bitOffset, alignment, ft.isSigned());
    resultReg = dest;
  }

  /**
   * Generate a branch based on the value of an expression compared to zero.
   * The value may be floating point or integer but it is never a value pair.
   * @param which specifies the branch test (EQ, NE, LT, ...)
   * @param treg specifies the condition (register value) to test
   * @param signed is true if the value is signed
   * @param labt specifies the path if the test fails
   * @param labf specifies the path if the test succeeds
   */
  protected void genIfRegister(CompareMode which,
                               int         treg,
                               boolean     signed,
                               Label       labt,
                               Label       labf)
  {
    int opcode = ibops[which.ordinal()];
    if (registers.floatRegister(treg))
      opcode = fbops[which.ordinal()];

    LabelDisplacement disp = new LabelDisplacement(labf);
    Branch            br   = new BranchInstruction(opcode, treg, disp, 2);
 
    br.addTarget(labf, 0);
    br.addTarget(labt, 1);

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
    CompareMode which = predicate.getMatchOp();
    boolean     flag  = false;
    Expr        la    = predicate.getLeftArg();
    Expr        ra    = predicate.getRightArg();
    CompareMode ow    = which;

    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
      which = which.argswap();
    }

    Type lt = processType(la);

    if (lt.isRealType()) { // Floating point comparison.
      if (rflag)
        which = which.reverse();

      needValue(la);
      int laReg = resultReg;

      if (!lt.isComplexType() && ra.isLiteralExpr()) {
        LiteralExpr le  = (LiteralExpr) ra;
        Literal     lit = le.getLiteral();
        if (lit instanceof FloatLiteral) {
          FloatLiteral fl = (FloatLiteral) lit;
          double       fv = fl.getDoubleValue();
          if (fv == 0.0) {
            genIfRegister(which, laReg, true, tc, fc);
            return;
          }
        }
      }
      
      needValue(ra);

      int     raReg  = resultReg;
      int     treg   = registers.newTempRegister(RegisterSet.FLTREG);
      boolean swap   = true;
      int     opcode = 0;

      switch (which) {
      case EQ: opcode = Opcodes.CMPTEQ; break;
      case LE: opcode = Opcodes.CMPTLE; break;
      case LT: opcode = Opcodes.CMPTLT; break;
      case GT: opcode = Opcodes.CMPTLE; swap = false; break;
      case GE: opcode = Opcodes.CMPTLT; swap = false; break;
      case NE: opcode = Opcodes.CMPTEQ; swap = false; break;
      default: throw new scale.common.InternalError("No such comparison " + which);
      }

      appendInstruction(new FltOpInstruction(opcode, laReg, raReg, treg));
      if (lt.isComplexType()) {
        int treg2  = registers.newTempRegister(RegisterSet.FLTREG);
        int laRegP = laReg + registers.numContiguousRegisters(laReg);
        int raRegP = raReg + registers.numContiguousRegisters(raReg);
        appendInstruction(new FltOpInstruction(opcode, laRegP, raRegP, treg2));
        appendInstruction(new FltOpInstruction(Opcodes.FCMOVEQ, treg2, treg2, treg));
      }
      genIfRegister(swap ? CompareMode.NE : CompareMode.EQ, treg, true, tc, fc);

      return;
    }

    if (ra.isLiteralExpr()) {
      LiteralExpr le  = (LiteralExpr) ra;
      Literal     lit = le.getLiteral();

      if (lit instanceof BooleanLiteral) {
        BooleanLiteral bl = (BooleanLiteral) lit;
        flag = !bl.getBooleanValue();
      } else if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        long       iv = il.getLongValue();
        if (iv == 0)
          flag = true;
      } else if (lit instanceof SizeofLiteral) {
        long iv = valueOf((SizeofLiteral) lit);
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
      rflag = !rflag;
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


      if (off == AlphaRegisterSet.I0_REG) {
        resultReg = ind;
        return;
      }

      if (negOff) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.SUBQ, ind, off, tr));
        resultReg = tr;
        return;
      }

      if (ind == AlphaRegisterSet.I0_REG) {
        resultReg = off;
        return;
      }

      int tr = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpInstruction(Opcodes.ADDQ, ind, off, tr));
      resultReg = tr;
      return;
    case 1:
      needValue(index);
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultRegAddressOffset = oval;
      return;
    case 2:
      needValue(offset);
      if (negOff) {
        int tr2 = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.SUBQ,
                                               AlphaRegisterSet.I0_REG,
                                               resultReg,
                                               tr2));
        resultReg = tr2;
      }
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultRegAddressOffset = ival;
      return;
    default:
    case 3:
      resultRegAddressOffset = oval + ival;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = AlphaRegisterSet.I0_REG;
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
    PointerType vt     = (PointerType) processType(aie);
    Type        et     = vt.getPointedTo().getCoreType();
    int         bs     = et.memorySizeAsInt(machine);
    Expr        array  = aie.getArray();
    Expr        index  = aie.getIndex();
    Expr        offset = aie.getOffset();
    int         daln   = vt.getPointedTo().alignment(machine);

    calcAddressAndOffset(array, 0);
    long offseta = resultRegAddressOffset;
    int  arr     = resultReg;

    calcArrayOffset(offset, index);
    long offseth = resultRegAddressOffset;
    int  tr      = resultReg;

    int tr2;
    if (tr == AlphaRegisterSet.I0_REG)
      tr2 = arr;
    else {
      tr2 = registers.newTempRegister(RegisterSet.INTREG);
      switch (bs) {
      case 1:
        appendInstruction(new IntOpInstruction(Opcodes.ADDQ, tr, arr, tr2));
        break;
      case 4:
        appendInstruction(new IntOpInstruction(Opcodes.S4ADDQ, tr, arr, tr2));
        break;
      case 8:
        appendInstruction(new IntOpInstruction(Opcodes.S8ADDQ, tr, arr, tr2));
        break;
      default:
        int tr3 = registers.newTempRegister(RegisterSet.INTREG);
        genMultiplyQuad(bs, tr, tr3);
        appendInstruction(new IntOpInstruction(Opcodes.ADDQ, tr3, arr, tr2));
        break;
      }
    }

    long offsetl = offseth * bs + offseta;
    loadFromMemoryWithOffset(dest, tr2, offsetl, bs, daln, et.isSigned(), false);
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

    int daln = et.alignment(machine);
    if (daln < resultRegAddressAlignment)
      resultRegAddressAlignment = daln;

    if (tr == AlphaRegisterSet.I0_REG) {
      resultRegMode = ResultMode.ADDRESS_VALUE;
      resultRegAddressOffset += offseth * bs;
      resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
      return;
    }

    offseta = resultRegAddressOffset + bs * offseth;
    int arr = resultReg;

    switch (bs) {
    case 1:
      appendInstruction(new IntOpInstruction(Opcodes.ADDQ, tr, arr, ir));
      break;
    case 4:
      appendInstruction(new IntOpInstruction(Opcodes.S4ADDQ, tr, arr, ir));
      break;
    case 8:
      appendInstruction(new IntOpInstruction(Opcodes.S8ADDQ, tr, arr, ir));
      break;
    default:
      int tr3 = registers.newTempRegister(RegisterSet.ADRREG);
      genMultiplyQuad(bs, tr, tr3);
      appendInstruction(new IntOpInstruction(Opcodes.ADDQ, tr3, arr, ir));
      break;
    }

    resultRegAddressOffset = offseta;
    resultRegMode = ResultMode.ADDRESS_VALUE;
    resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
    resultReg = ir;
  }

  public void visitMultiplicationExpr(MultiplicationExpr e)
  { 
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    Expr ra = e.getRightArg();
    Expr la = e.getLeftArg();

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
          resultReg = AlphaRegisterSet.I0_REG;
          return;
        }
        int ir = registers.getResultRegister(ct.getTag());
        needValue(la);
        if (bs > 4)
          genMultiplyQuad(value, resultReg, ir);
        else {
          genMultiplyLong(value, resultReg, ir);
          if (!ct.isSigned())
            appendInstruction(new IntOpLitInstruction(Opcodes.ZAPNOT, ir, (1 << bs) - 1, ir));
        }
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    doBinaryOp(e, MUL);
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    Type ct  = processType(e);
    int  bs  = ct.memorySizeAsInt(machine);
    Expr arg = e.getArg();
    int  ir  = registers.getResultRegister(ct.getTag());

    needValue(arg);

    if ((resultReg == AlphaRegisterSet.I0_REG) || (resultReg == AlphaRegisterSet.F0_REG))
      return;

    if (ct.isRealType()) {
      int op = (bs > 4) ? Opcodes.SUBT : Opcodes.SUBS;
      appendInstruction(new FltOpInstruction(op, AlphaRegisterSet.F0_REG, resultReg, ir));
      if (ct.isComplexType())
        appendInstruction(new FltOpInstruction(op, AlphaRegisterSet.F0_REG, resultReg + 1, ir + 1));
    } else {
      int op = (bs > 4) ? Opcodes.SUBQ : Opcodes.SUBL;
      appendInstruction(new IntOpInstruction(op, AlphaRegisterSet.I0_REG, resultReg, ir));
    }
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  protected void genAlloca(Expr arg, int reg)
  {
    int tr = registers.newTempRegister(RegisterSet.AIREG);

    if (arg.isLiteralExpr()) {
      Object lit = ((LiteralExpr) arg).getLiteral().getConstantValue();
      if (lit instanceof IntLiteral) {
        long size = (((IntLiteral) lit).getLongValue() + 15) & -16;
        if ((size >= 0) && (size <= MAX_IMM16)) {
          appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                       AlphaRegisterSet.SP_REG,
                                                       AlphaRegisterSet.SP_REG,
                                                       getDisp((int) -size)));
          appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.SP_REG, 0, reg));
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = reg;
          return;
        }
      }
    }

    needValue(arg);

    appendInstruction(new IntOpInstruction(Opcodes.SUBQ, AlphaRegisterSet.SP_REG, resultReg, tr));
    appendInstruction(new IntOpLitInstruction(Opcodes.BIC, tr, 0xf, AlphaRegisterSet.SP_REG));
    appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.SP_REG, 0, reg));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = reg;
  }

  protected void genSignFtn(int dest, int laReg, int raReg, Type rType)
  {
    int bs = rType.memorySizeAsInt(machine);

    if (rType.isRealType()) {
      appendInstruction(new FltOpInstruction(Opcodes.CPYS, raReg, laReg, dest));
      return;
    }

    int op = getBinaryOpcode(SUB, false, bs);
    int ir = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.
    int tr = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.
    appendInstruction(new IntOpInstruction(Opcodes.XOR, raReg, laReg, ir));
    appendInstruction(new IntOpLitInstruction(Opcodes.SRA, ir, 63, ir));
    appendInstruction(new IntOpInstruction(Opcodes.XOR, laReg, ir, tr));
    appendInstruction(new IntOpInstruction(op, tr, ir, dest));
  }

  protected void genAtan2Ftn(int dest, int laReg, int raReg, Type rType)
  {
    genRegToReg(laReg, AlphaRegisterSet.FF_REG);
    genRegToReg(raReg, AlphaRegisterSet.FF_REG + 1);
    genFtnCall("atan2", genDoubleUse(AlphaRegisterSet.FF_REG, AlphaRegisterSet.FF_REG + 1), null);
    genRegToReg(AlphaRegisterSet.FR_REG, dest);
  }

  protected void genDimFtn(int dest, int laReg, int raReg, Type rType)
  {
    int bs = rType.memorySizeAsInt(machine);

    // r = dim(a, b);
    // r = (a > b) ? a - b : 0;

    if (rType.isRealType()) {
      int op = getBinaryOpcode(SUB, true, bs);
      appendInstruction(new FltOpInstruction(op, laReg, raReg, dest));
      appendInstruction(new FltOpInstruction(Opcodes.FCMOVLT, dest, AlphaRegisterSet.F0_REG, dest));
      return;
    }

    int op = getBinaryOpcode(SUB, false, bs);
    int ir = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.
    appendInstruction(new IntOpInstruction(op, laReg, raReg, dest));
    appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, 63, ir));
    appendInstruction(new IntOpInstruction(Opcodes.BIC, dest, ir, dest));
  }

  private void genFtnCall(String fname, int dest, int src, Type type)
  {
    boolean cmplx = type.isComplexType();

    if (cmplx) {
      int          bs  = type.memorySizeAsInt(machine);
      StringBuffer buf = new StringBuffer("c");
      buf.append(fname);
      if (bs <= 8)
        buf.append('f');
      fname = buf.toString();
    }

    boolean flt = registers.floatRegister(src);
    int     arg = flt ? AlphaRegisterSet.FF_REG : AlphaRegisterSet.IF_REG;

    genRegToReg(src, arg);
    genFtnCall(fname, cmplx ? genDoubleUse(arg, arg + 1) : genSingleUse(arg), null);
    int rr = returnRegister(type.getTag(), true);
    genRegToReg(rr, dest);

    if (cmplx)
      genRegToReg(rr + 1, dest + 1);
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
    int bs = type.memorySizeAsInt(machine);
    bs >>= 1;
    appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, src, dest));
    int op = bs > 4 ? Opcodes.SUBT : Opcodes.SUBS;
    appendInstruction(new FltOpInstruction(op, AlphaRegisterSet.F0_REG, src + 1, dest + 1));
  }

  protected void genReturnAddressFtn(int dest, int src, Type type)
  {
    int ir = AlphaRegisterSet.RA_REG;
    if (callsRoutine) {
      ir = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new StoreInstruction(Opcodes.LDQ, ir, AlphaRegisterSet.SP_REG, raDisp));
    }

    if (src == AlphaRegisterSet.I0_REG) {
      appendInstruction(new IntOpLitInstruction(Opcodes.BIS, ir, 0, dest));
      return;
    }

    appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.I0_REG, 0, dest));
    appendInstruction(new IntOpInstruction(Opcodes.CMOVEQ, src, ir, dest));
  }

  protected void genFrameAddressFtn(int dest, int src, Type type)
  {
    if (src == AlphaRegisterSet.I0_REG) {
      appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.SP_REG, 0, dest));
      return;
    }

    appendInstruction(new IntOpLitInstruction(Opcodes.BIS, AlphaRegisterSet.I0_REG, 0, dest));
    appendInstruction(new IntOpInstruction(Opcodes.CMOVEQ, src, AlphaRegisterSet.SP_REG, dest));
  }

  public void visitNotExpr(NotExpr e)
  {
    Expr arg = e.getArg();
    int  ir  = registers.getResultRegister(RegisterSet.INTREG);

    needValue(arg);
    int src = resultReg;

    assert !registers.floatRegister(src) : "Not not allowed on " + arg;

    appendInstruction(new IntOpInstruction(Opcodes.CMPEQ, src, AlphaRegisterSet.I0_REG, ir));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitReturnChord(ReturnChord c)
  {
    Expr    a    = c.getResultValue();
    int     k    = 0;
    short[] uses = null;

    if (a != null) {
      Type    at   = processType(a);
      int     dest = returnRegister(at.getTag(), false);
      boolean flg  = at.isAtomicType();

      if (flg)
        registers.setResultRegister(dest);

      a.visit(this);
      int        src    = resultReg;
      long       srcoff = resultRegAddressOffset;
      ResultMode srcha  = resultRegMode;
      int        srcaln = resultRegAddressAlignment;

      if (flg)
        registers.setResultRegister(-1);

      if (srcha == ResultMode.ADDRESS) {
        moveWords(src, srcoff, structAddress, 0, structSize, srcaln);
        uses = new short[usesAlloca ? 4 : 3];
      } else if (srcha == ResultMode.STRUCT_VALUE){
        storeIntoMemoryWithOffset(src, structAddress, 0, structSize, structSize, false); 
        uses = new short[usesAlloca ? 4 : 3];
      } else {
        needValue(resultReg, srcoff, srcha);
        int nr = registers.numContiguousRegisters(resultReg);
        int fr = registers.rangeBegin(dest);
        if (registers.pairRegister(resultReg))
          nr *= 2;
        uses = new short[nr + (usesAlloca ? 4 : 3)];
        for (int i = 0; i < nr; i++)
          uses[k++] = (short) (fr + i);
        genRegToReg(resultReg, dest);
      }
    } else
      uses = new short[usesAlloca ? 4 : 3];

    if (!callsRoutine)
      uses[k++] = AlphaRegisterSet.RA_REG;
    uses[k++] = AlphaRegisterSet.SP_REG;
    uses[k++] = AlphaRegisterSet.GP_REG;
    if (usesAlloca)
      uses[k++] = AlphaRegisterSet.FP_REG;

    Branch inst = new JmpInstruction(Opcodes.RET, AlphaRegisterSet.RA_REG, 0);
    inst.additionalRegsUsed(uses); // Specify which registers are used.
    returnInst = lastInstruction;
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
  protected void storeRegToSymbolicLocation(int          src,
                                            int          dsize,
                                            long         alignment,
                                            boolean      isReal,
                                            Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

      int dest = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                            dest,
                                            AlphaRegisterSet.GP_REG,
                                            disp,
                                            RT_LITERAL));
      storeIntoMemory(src, dest, disp, RT_LITUSE_BASE, dsize, alignment);
      usesGp = true;
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
    long       srcrs  = 0;

    calcAddressAndOffset(struct, fieldOffset);
    fieldOffset = resultRegAddressOffset;
    int        adr    = resultReg;
    ResultMode adrha  = resultRegMode;
    int        adraln = resultRegAddressAlignment;

    if (srcha == ResultMode.ADDRESS) {
      int aln = (((fieldOffset & 0x7) == 0) ? 8 : ((fieldOffset & 0x3) == 0) ? 4 : 1);
      if (adraln < aln)
        aln = adraln;
      if (srcaln < aln)
        aln = srcaln;

      if (adrha == ResultMode.ADDRESS_VALUE) {
        moveWords(src, srcoff, adr, fieldOffset, byteSize, aln);
        resultRegAddressOffset = 0;
        resultRegMode = srcha;
        resultReg = src;
        resultRegAddressAlignment = srcaln;
        return;
      }

      if (adrha != ResultMode.STRUCT_VALUE)
        throw new scale.common.InternalError("Where should it be put? " + adrha);

      // Load it into a register.

      long bs = ft.memorySize(machine);
      assert (bs <= 2 * SAVED_REG_SIZE) : "Struct too big.";

      int dr = registers.newTempRegister(ft.getTag());
      loadFromMemoryWithOffset(dr, src, 0, (int) bs, aln, false, false);
      src = dr;
      srcha =  ResultMode.STRUCT_VALUE;
      srcoff = 0;
      srcrs = bs;
    }

    needValue(src, srcoff, srcha);
    src = resultReg;

    if (adrha == ResultMode.STRUCT_VALUE) {
      assert (fieldOffset < 16) : "Field offset too large " + fieldOffset;
      // Structure is in registers, not memory, and is 16 bytes or
      // less in size.
      if (ft.isRealType()) {
        // Assume entire struct is in floating point registers and
        // contains only doubles because there is no easy transfer
        // between integer and double registers and there is no
        // conversion from 32-bit single to 64-bit double format that
        // does not go through memory.
        assert (bits == 0) && (byteSize == 8) : "Bit field " + ft;
        int dr = adr + ((fieldOffset >= 8) ? 1 : 0);
        appendInstruction(new FltOpInstruction(Opcodes.CPYS, src, src, dr));
        return;
      }

      if (fieldOffset >= 8) {
        fieldOffset -= 8;
        adr++;
      }

      bitOffset += 8 * ((int) fieldOffset);
      if (bits == 0)
        bits = 8 * byteSize;

      if (bits == 64) {
        appendInstruction(new IntOpInstruction(Opcodes.BIS, src, src, adr));
        resultRegAddressOffset = srcoff;
        resultRegMode = srcha;
        resultReg = src;
        resultRegSize = srcrs;
        return;
      }

      long mk   = (1L << bits) - 1;
      long mask = (mk << bitOffset);
      int  trs  = registers.newTempRegister(RegisterSet.INTREG);

      if ((mask >= 0) && (mask <= 255)) {
        if (bitOffset > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, bitOffset, trs));
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, trs, (int) mask, trs));
        } else
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, (int) mask, trs));
        appendInstruction(new IntOpLitInstruction(Opcodes.BIC, adr, (int) mask, adr));
      } else {
        int trm = registers.newTempRegister(RegisterSet.INTREG);
        if ((mask >= 0) && (mask <= MAX_IMM16))
          trm = genLoadImmediate(mask, trm);
        else {
          int tmp = registers.newTempRegister(RegisterSet.INTREG);
          if (bits < 16)
            appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                         tmp,
                                                         AlphaRegisterSet.I0_REG,
                                                         getDisp((1 << bits) - 1)));
          else {
            appendInstruction(new IntOpLitInstruction(Opcodes.SUBQ,
                                                      AlphaRegisterSet.I0_REG,
                                                      1,
                                                      tmp));
            appendInstruction(new IntOpLitInstruction(Opcodes.SRL,
                                                      tmp,
                                                      64 - bits,
                                                      tmp));
          }
          if (bitOffset > 0)
            appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tmp, bitOffset, trm));
          else
            trm = tmp;
        }
        if (bitOffset > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, bitOffset, trs));
          appendInstruction(new IntOpInstruction(Opcodes.AND, trs, trm, trs));
        } else
          appendInstruction(new IntOpInstruction(Opcodes.AND, src, trm, trs));
        appendInstruction(new IntOpInstruction(Opcodes.BIC, adr, trm, adr));
      }
      appendInstruction(new IntOpInstruction(Opcodes.BIS, trs, adr, adr));

      int ir = src;
      if ((fd.getBits() != 0) && (bits < 64)) {
        ir = trs;
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, 64 - bits, ir));
        int op = ft.isSigned() ? Opcodes.SRA : Opcodes.SRL;
        appendInstruction(new IntOpLitInstruction(op, ir, 64 - bits, ir));
      }

      resultRegAddressOffset = srcoff;
      resultRegMode = srcha;
      resultReg = ir;
      resultRegSize = srcrs;
      return;
    }

    // Structure is in memory.

    int alignment = naln ? 1 : ((adraln < fa) ? adraln : fa);
    if (bits == 0) {
      Displacement disp;
      if ((fieldOffset >= MIN_IMM16) && (fieldOffset <= MAX_IMM16))
        disp = getDisp((int) fieldOffset);
      else {
        int tr = registers.newTempRegister(RegisterSet.ADRREG);
        genLoadImmediate(fieldOffset, adr, tr);
        disp = getDisp(0);
        adr = tr;
      }

      storeIntoMemory(src, adr, disp, RT_NONE, byteSize, alignment);
      resultRegAddressOffset = srcoff;
      resultRegMode = srcha;
      resultReg = src;
      resultRegSize = srcrs;
      return;
    }

    int tr = registers.newTempRegister(RegisterSet.ADRREG);
    genLoadImmediate(fieldOffset, adr, tr);
    adr = tr;

    int ir = registers.newTempRegister(RegisterSet.ADRREG);
    storeBitsIntoMemory(src, adr, bits, bitOffset, alignment);

    if (src != AlphaRegisterSet.I0_REG) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, 64 - bits, ir));
      if (ft.isSigned())
        appendInstruction(new IntOpLitInstruction(Opcodes.SRA, ir, 64 - bits, ir));
      else
        appendInstruction(new IntOpLitInstruction(Opcodes.SRL, ir, 64 - bits, ir));
    } else
      ir = src;

    resultRegAddressOffset = srcoff;
    resultRegMode = srcha;
    resultReg = ir;
    resultRegSize = srcrs;
  }

  protected boolean genSwitchUsingIfs(int testReg, Chord[] cases, long[] keys, int num, long spread)
  {
    if ((num > 5) && ((spread <= 512) || ((spread / num) <= 10)))
      return false;

    // Use individual tests.

    for (int i = 0; i < num - 1; i++) {
      int               tmp   = registers.newTempRegister(RegisterSet.AIREG);
      long              value = keys[i];
      Label             labt  = getBranchLabel(cases[i]);
      Label             labf  = createLabel();
      LabelDisplacement disp  = new LabelDisplacement(labt);
      Branch            inst;

      if (value == 0) {
        inst = new BranchInstruction(Opcodes.BEQ, testReg, disp, 2);
      } else {
        doIntOperate(Opcodes.CMPEQ, testReg, value, tmp);
        inst = new BranchInstruction(Opcodes.BNE, tmp, disp, 2);
      }

      inst.addTarget(labt, 0);
      inst.addTarget(labf, 1);
      appendInstruction(inst);
      appendLabel(labf);
      labf.setNotReferenced();
    }

    Chord deflt = cases[num - 1];
    if (!doNext(deflt))
      generateUnconditionalBranch(getBranchLabel(deflt));
    return true;
  }

  protected void genSwitchUsingTransferVector(int     testReg,
                                              Chord[] cases,
                                              long[]  keys,
                                              Label   labt,
                                              long    min,
                                              long    max)
  {
    // Use a transfer vector.

    int ir = testReg;
    if (min != 0) {
      ir = registers.newTempRegister(RegisterSet.INTREG);
      doIntOperate(Opcodes.SUBQ, testReg, min, ir);
    }
    int               tmp   = registers.newTempRegister(RegisterSet.AIREG);
    int               adr   = registers.newTempRegister(RegisterSet.AIREG);
    Label             labf  = createLabel();
    LabelDisplacement dispt = new LabelDisplacement(labt);
    Branch            insti = new BranchInstruction(Opcodes.BEQ, tmp, dispt, 2);
    Displacement      disp  = createAddressTable(cases, keys, (int) min, (int) max);
    Branch            instj = new JmpInstruction(Opcodes.JMP,
                                                 AlphaRegisterSet.I0_REG,
                                                 adr,
                                                 cases.length);

    insti.addTarget(labt, 0);
    insti.addTarget(labf, 1);

    for (int i = 0; i < cases.length; i++)
      instj.addTarget(getBranchLabel(cases[i]), i);

    doIntOperate(Opcodes.CMPULE, ir, max - min, tmp);

    appendInstruction(insti);
    appendLabel(labf);
    labf.setNotReferenced();
    appendInstruction(new LoadInstruction(Opcodes.LDQ,
                                          adr,
                                          AlphaRegisterSet.GP_REG,
                                          disp,
                                          RT_LITERAL));
    appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                 adr,
                                                 adr,
                                                 disp,
                                                 RT_LITUSE_BASE));
    appendInstruction(new IntOpInstruction(Opcodes.S4ADDQ, ir, adr, adr));
    appendInstruction(new LoadInstruction(Opcodes.LDL, adr, adr));
    appendInstruction(new IntOpInstruction(Opcodes.ADDQ, adr, AlphaRegisterSet.GP_REG, adr));
    appendInstruction(instj);

    usesGp = true;
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    FormalDecl    parmN  = e.getParmN();
    Expr          vaList = e.getVaList();
    int           pr     = registers.getResultRegister(RegisterSet.ADRREG);
    int           or     = registers.getResultRegister(RegisterSet.INTREG);
    ProcedureType pt     = (ProcedureType) processType(currentRoutine);
    Type          rt     = processType(pt.getReturnType());
    Displacement  sdisp  = argDisp.offset(6 * AlphaRegisterSet.IREG_SIZE);
    int           offset = 0;
    int           l      = pt.numFormals();

    if (!(rt.isAtomicType() || rt.isVoidType()))
      offset += AlphaRegisterSet.IREG_SIZE; // Procedure returns structure.

    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      int        ms = fd.getCoreType().memorySizeAsInt(machine);
      int        bs = (ms + AlphaRegisterSet.IREG_SIZE - 1) & ~(AlphaRegisterSet.IREG_SIZE - 1);

      offset += bs;

      if (fd == parmN) {
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA, pr, stkPtrReg, sdisp));
        needValue(vaList);
        int vr = resultReg;
        storeIntoMemory(pr,
                        vr,
                        getDisp(0),
                        RT_NONE,
                        AlphaRegisterSet.IREG_SIZE,
                        AlphaRegisterSet.IREG_SIZE);
        appendInstruction(new LoadAddressInstruction(Opcodes.LDA,
                                                     or,
                                                     AlphaRegisterSet.I0_REG,
                                                     getDisp(offset)));
        storeIntoMemory(or, vr, getDisp(AlphaRegisterSet.IREG_SIZE),
                        RT_NONE,
                        AlphaRegisterSet.IREG_SIZE,
                        AlphaRegisterSet.IREG_SIZE);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = vr;
        return;
      }
    }

    throw new scale.common.InternalError("Parameter not found " + parmN);
  }

  /**
   * Generate code for a va_copy().
   */
  protected void doVaCopy(Expr dst, Expr src)  
  {
    int pr  = registers.getResultRegister(RegisterSet.ADRREG);
    int or  = registers.getResultRegister(RegisterSet.INTREG);

    dst.visit(this);
    int  dr    = resultReg;
    long droff = resultRegAddressOffset;

    src.visit(this);
    int  sr    = resultReg;
    long sroff = resultRegAddressOffset;

    loadFromMemoryWithOffset(pr,
                             sr,
                             sroff,
                             AlphaRegisterSet.IREG_SIZE,
                             0,
                             true,
                             false);
    loadFromMemoryWithOffset(or,
                             sr,
                             sroff + AlphaRegisterSet.IREG_SIZE,
                             AlphaRegisterSet.IREG_SIZE,
                             0,
                             true,
                             false);
    storeIntoMemoryWithOffset(pr,
                              dr,
                              droff,
                              AlphaRegisterSet.IREG_SIZE,
                              0,
                              false);
    storeIntoMemoryWithOffset(or,
                              dr,
                              droff + AlphaRegisterSet.IREG_SIZE,
                              AlphaRegisterSet.IREG_SIZE,
                              0,
                              false);

    resultRegAddressOffset = sroff;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = sr;
  }

  public void visitVaArgExpr(VaArgExpr e)
  {
    Expr vaList = e.getVaList();
    Type ct     = processType(e);
    int  rr;
    int  ir     = registers.newTempRegister(RegisterSet.INTREG);
    int  adr    = registers.newTempRegister(RegisterSet.ADRREG);
    int  bs     = ct.memorySizeAsInt(machine);

    vaList.visit(this);
    int  vr    = resultReg;
    long vroff = resultRegAddressOffset;

    int soffset = 0;
    if ((vroff >= (MAX_IMM16 - AlphaRegisterSet.IREG_SIZE)) ||
        (vroff <= (MIN_IMM16 + AlphaRegisterSet.IREG_SIZE))) {
      int sr = registers.newTempRegister(RegisterSet.INTREG);
      genLoadImmediate(vroff, vr, sr);
      vr = sr;
    } else
      soffset = (int) vroff;

    loadFromMemory(ir, vr, getDisp(soffset + AlphaRegisterSet.IREG_SIZE),
                   AlphaRegisterSet.IREG_SIZE,
                   0,
                   false);
    loadFromMemory(adr, vr, getDisp(soffset), AlphaRegisterSet.IREG_SIZE, 0, false);

    Displacement disp = getDisp(soffset + AlphaRegisterSet.IREG_SIZE);
    if (ct.isAtomicType()) {
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDQ,
                                                ir,
                                                AlphaRegisterSet.IREG_SIZE,
                                                ir));

      if (ct.isRealType()) {
        int or = registers.getResultRegister(RegisterSet.INTREG);
        int tr = registers.getResultRegister(RegisterSet.INTREG);
        rr = registers.getResultRegister(RegisterSet.FLTREG);
        appendInstruction(new IntOpInstruction(Opcodes.ADDQ, adr, ir, adr));
        appendInstruction(new IntOpLitInstruction(Opcodes.CMPLE,
                                                  ir,
                                                  6 * AlphaRegisterSet.IREG_SIZE,
                                                  tr));
        storeIntoMemory(ir,
                        vr,
                        disp,
                        AlphaRegisterSet.IREG_SIZE,
                        AlphaRegisterSet.IREG_SIZE);
        appendInstruction(new IntOpLitInstruction(Opcodes.BIS,
                                                  AlphaRegisterSet.I0_REG,
                                                  AlphaRegisterSet.IREG_SIZE,
                                                  or));
        appendInstruction(new IntOpLitInstruction(Opcodes.CMOVNE, tr, 56, or));
        appendInstruction(new IntOpInstruction(Opcodes.SUBQ, adr, or, adr));
        loadFromMemory(rr, adr, getDisp(0), bs, 0, false);
        resultRegMode = ResultMode.NORMAL_VALUE;
      } else {
        rr = registers.getResultRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.ADDQ, adr, ir, adr));
        loadFromMemory(rr, adr, getDisp(-AlphaRegisterSet.IREG_SIZE), bs, 0, ct.isSigned());
        storeIntoMemory(ir, vr, disp, AlphaRegisterSet.IREG_SIZE, AlphaRegisterSet.IREG_SIZE);
        resultRegMode = ResultMode.NORMAL_VALUE;
      }
    } else {
      int inc = (bs + AlphaRegisterSet.IREG_SIZE - 1) & ~(AlphaRegisterSet.IREG_SIZE - 1);
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDQ, ir, inc, ir));
      rr = registers.getResultRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpInstruction(Opcodes.ADDQ, adr, ir, adr));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDQ, adr, -inc, rr));
      storeIntoMemory(ir, vr, disp, AlphaRegisterSet.IREG_SIZE, AlphaRegisterSet.IREG_SIZE);
      resultRegAddressAlignment = naln ? 1 : 8;
      resultRegMode = ResultMode.ADDRESS;
    }

    resultRegAddressOffset = 0;
    resultReg = rr;
  }

  private static final int[] cond1 = {Opcodes.FCMOVEQ, Opcodes.FCMOVEQ, Opcodes.FCMOVEQ, 
                                      Opcodes.FCMOVNE, Opcodes.FCMOVNE, Opcodes.FCMOVNE};
  private static final int[] cond2 = {Opcodes.CMPTEQ, Opcodes.CMPTLE, Opcodes.CMPTLT,
                                      Opcodes.CMPTLE, Opcodes.CMPTLT, Opcodes.CMPTEQ};
  private static final int[] cond3 = {Opcodes.FCMOVNE, Opcodes.FCMOVGT, Opcodes.FCMOVGE,
                                      Opcodes.FCMOVLE, Opcodes.FCMOVLT, Opcodes.FCMOVNE};

  public void visitConditionalExpr(ConditionalExpr e)
  {
    Type ct = processType(e);
    int  tr = registers.getResultRegister(ct.getTag());

    Expr predicate = e.getTest();
    Expr trueExpr  = e.getTrueExpr();
    Expr falseExpr = e.getFalseExpr();

    needValue(trueExpr);
    int treg = resultReg;

    if (ct.isRealType()) {
      if (predicate.isMatchExpr()) {
        MatchExpr be      = (MatchExpr) predicate;
        Expr      la      = be.getLeftArg();
        if (la.getCoreType().isRealType()) {
          Expr        ra      = be.getRightArg();
          CompareMode cmp     = be.getMatchOp();
          int         opcode2 = cond2[cmp.ordinal()];
          int         opcode1 = cond1[cmp.ordinal()];
          int         opcode3 = cond3[cmp.ordinal()];

          needValue(la);
          int laReg = resultReg;
          needValue(ra);
          int raReg = resultReg;

          needValue(falseExpr);
          int freg = resultReg;

          if ((tr == treg) || (tr == freg))
            tr = registers.newTempRegister(ct.getTag());

          genRegToReg(treg, tr);

          int preg = registers.newTempRegister(ct.getTag());

          if (raReg == AlphaRegisterSet.F0_REG) {
            appendInstruction(new FltOpInstruction(opcode3, laReg, freg, tr));
          } else {
            appendInstruction(new FltOpInstruction(opcode2, laReg, raReg, preg));
            appendInstruction(new FltOpInstruction(opcode1, preg, freg, tr));
          }

          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = tr;
          return;
        }
      }

      needValue(predicate);
      int preg = resultReg;

      if ((tr == treg) || (tr == preg) || isAssignedRegister(tr))
        tr = registers.newTempRegister(ct.getTag());

      genRegToReg(treg, tr);

      Label             labt = createLabel();
      Label             labf = createLabel();
      LabelDisplacement disp = new LabelDisplacement(labt);
      Branch            inst = new BranchInstruction(Opcodes.BNE, preg, disp, 2);
      inst.addTarget(labt, 0);
      inst.addTarget(labf, 1);
      appendInstruction(inst);
      appendLabel(labf);
      labf.setNotReferenced();
      registers.setResultRegister(tr);
      needValue(falseExpr);
      genRegToReg(resultReg, tr);
      registers.setResultRegister(-1);
      appendLabel(labt);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = tr;
      return;
    }

    needValue(falseExpr);
    int freg = resultReg;

    needValue(predicate);
    int preg = resultReg;

    if (tr == preg)
      tr = registers.newTempRegister(ct.getTag());

    if (tr == freg) {
      appendInstruction(new IntOpInstruction(Opcodes.CMOVNE, preg, treg, tr));
    } else if (tr == treg) {
      appendInstruction(new IntOpInstruction(Opcodes.CMOVEQ, preg, freg, tr));
    } else {
      genRegToReg(treg, tr);
      appendInstruction(new IntOpInstruction(Opcodes.CMOVEQ, preg, freg, tr));
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = tr;
  }

  public int getMaxAreaIndex()
  {
    return TEXT;
  }
}
