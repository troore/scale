package scale.backend.ppc;

import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
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
 * This class converts Scribble into PPC instructions.
 * <p>
 * $Id: PPCGenerator.java,v 1.135 2007-10-04 19:57:55 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class PPCGenerator extends scale.backend.Generator
{
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

  public static final int FT_NONE   = 0;
  public static final int FT_LO16   = 1;
  public static final int FT_HI16   = 2;
  public static final int MAX_IMM16 = 32767;
  public static final int MIN_IMM16 = -32768;

  private static final int ARG_SAVE_OFFSET_MACOSX = 24; // Offset to the argument save area.
  private static final int ARG_SAVE_OFFSET_LINUX  = 8;
  private static final int MAX_ARG_REGS           = 8;  // Maximum number of arguments passed in registers.
  private static final int MAX_FARG_REGS_MACOSX   = 13; // OSX allows 13 floating point arguments passed in registers.

  private static final int DBYTE   = 0;
  private static final int DSHORT  = 1;
  private static final int DINT    = 2;
  private static final int DLLONG  = 3;
  private static final int SBYTE   = DBYTE  << 2;
  private static final int SSHORT  = DSHORT << 2;
  private static final int SINT    = DINT   << 2;
  private static final int SLLONG  = DLLONG  << 2;
  private static final int SSIGNED = 1 << 4;
  private static final int DSIGNED = 1 << 5;

  /*
   * smapSize is indexed into, by srcSize (number bits in the source data
   * type). The valid values of srcSize are 1, 2, 4, 8. In
   * convertIntRegValue(), there is a statement
   *    which = smapSize[srcSize] + dmapSize[destSize];
   * This `which' is then used to index into ccase[]. smapSize for the
   * invalid sizes (0, 3, 5, 6, 7) are made -1000, so that `which' does not
   * accidentally index into ccase.
   */
  private static final int[] smapSize = {-1000, SBYTE, SSHORT, -1000, SINT, -1000, -1000, -1000, SLLONG};
  private static final int[] dmapSize = {-1000, DBYTE, DSHORT, -1000, DINT, -1000, -1000, -1000, DLLONG};

  private static final int EBEXT_I     =  0; //  From an int (32 bit), Extract byte & sign extend.
  private static final int ESEXT_I     =  1; //  From an int (32 bit), Extract short & sign extend.
  private static final int ELLEXT_I    =  3; //  From an int (32 bit), Extract long long and sign extend.
  private static final int EUB_I       =  4; //  From an int (32 bit), Extract unsigned byte.
  private static final int EUS_I       =  5; //  From an int (32 bit), Extract unsigned short.
  private static final int EUI_I       =  6; //  From an int (32 bit), Extract unsigned int.
  private static final int EULL_I      =  7; //  From an int (32 bit), Extract unsigned long long.
  private static final int EBEXT_LL    =  8; //  From a long long (64 bit), Extract byte & sign extend.
  private static final int ESEXT_LL    =  9; //  From a long long (64 bit), Extract short & sign extend.
  private static final int EIEXT_LL    = 10; //  From a long long (64 bit), Extract int & sign extend.
  private static final int EUB_LL      = 12; //  From a long long (64 bit), Extract unsigned byte.
  private static final int EUS_LL      = 13; //  From a long long (64 bit), Extract unsigned short.
  private static final int EUI_LL      = 14; //  From a long long (64 bit), Extract unsigned int.
  private static final int EULL_LL     = 15; //  From a long long (64 bit), Extract unsigned long long.

  private static final short[] intReturn     = {PPCRegisterSet.IR_REG};
  private static final short[] lintReturn    = {PPCRegisterSet.IR_REG, PPCRegisterSet.IR_REG + 1};
  private static final short[] realReturn    = {PPCRegisterSet.FR_REG};
  private static final short[] retUses       = {PPCRegisterSet.SP_REG};
  private static final byte[]  ccase = {
    EUI_I,         //                     SBYTE  + DBYTE
    EUI_I,         //                     SBYTE  + DSHORT
    EUI_I,         //                     SBYTE  + DINT
    EULL_I,        //                     SBYTE  + DLLONG
    EUB_I,         //                     SSHORT + DBYTE
    EUI_I,         //                     SSHORT + DSHORT
    EUI_I,         //                     SSHORT + DINT
    EULL_I,        //                     SSHORT + DLLONG
    EUB_I,         //                     SINT   + DBYTE
    EUS_I,         //                     SINT   + DSHORT
    EUI_I,         //                     SINT   + DINT
    EULL_I,        //                     SINT   + DLLONG
    EUB_LL,        //                     SLLONG + DBYTE
    EUS_LL,        //                     SLLONG + DSHORT
    EUI_LL,        //                     SLLONG + DINT
    EULL_LL,       //                     SLLONG + DLLONG

    EUB_I,         // SSIGNED           + SBYTE  + DBYTE
    EUS_I,         // SSIGNED           + SBYTE  + DSHORT
    EUI_I,         // SSIGNED           + SBYTE  + DINT
    ELLEXT_I,      // SSIGNED           + SBYTE  + DLLONG
    EUB_I,         // SSIGNED           + SSHORT + DBYTE
    EUS_I,         // SSIGNED           + SSHORT + DSHORT
    EUI_I,         // SSIGNED           + SSHORT + DINT
    ELLEXT_I,      // SSIGNED           + SSHORT + DLLONG
    EUB_I,         // SSIGNED           + SINT   + DBYTE
    EUS_I,         // SSIGNED           + SINT   + DSHORT
    EUI_I,         // SSIGNED           + SINT   + DINT
    ELLEXT_I,      // SSIGNED           + SINT   + DLLONG
    EUB_LL,        // SSIGNED           + SLLONG + DBYTE
    EUS_LL,        // SSIGNED           + SLLONG + DSHORT
    EUI_LL,        // SSIGNED           + SLLONG + DINT
    EULL_LL,       // SSIGNED           + SLLONG + DLLONG

    EBEXT_I,       //           DSIGNED + SBYTE  + DBYTE
    EUI_I,         //           DSIGNED + SBYTE  + DSHORT
    EUI_I,         //           DSIGNED + SBYTE  + DINT
    EULL_I,        //           DSIGNED + SBYTE  + DLLONG
    EBEXT_I,       //           DSIGNED + SSHORT + DBYTE
    ESEXT_I,       //           DSIGNED + SSHORT + DSHORT
    EUI_I,         //           DSIGNED + SSHORT + DINT
    EULL_I,        //           DSIGNED + SSHORT + DLLONG
    EBEXT_I,       //           DSIGNED + SINT   + DBYTE
    ESEXT_I,       //           DSIGNED + SINT   + DSHORT
    EUI_I,         //           DSIGNED + SINT   + DINT
    EULL_I,        //           DSIGNED + SINT   + DLLONG
    EBEXT_LL,      //           DSIGNED + SLLONG + DBYTE
    ESEXT_LL,      //           DSIGNED + SLLONG + DSHORT
    EUI_LL,        //           DSIGNED + SLLONG + DINT
    EULL_LL,       //           DSIGNED + SLLONG + DLLONG

    EUI_I,         // SSIGNED + DSIGNED + SBYTE  + DBYTE
    EUI_I,         // SSIGNED + DSIGNED + SBYTE  + DSHORT
    EUI_I,         // SSIGNED + DSIGNED + SBYTE  + DINT
    ELLEXT_I,      // SSIGNED + DSIGNED + SBYTE  + DLLONG
    EBEXT_I,       // SSIGNED + DSIGNED + SSHORT + DBYTE
    EUI_I,         // SSIGNED + DSIGNED + SSHORT + DSHORT
    EUI_I,         // SSIGNED + DSIGNED + SSHORT + DINT
    ELLEXT_I,      // SSIGNED + DSIGNED + SSHORT + DLLONG
    EBEXT_I,       // SSIGNED + DSIGNED + SINT   + DBYTE
    ESEXT_I,       // SSIGNED + DSIGNED + SINT   + DSHORT
    EUI_I,         // SSIGNED + DSIGNED + SINT   + DINT
    ELLEXT_I,      // SSIGNED + DSIGNED + SINT   + DLLONG
    EBEXT_LL,      // SSIGNED + DSIGNED + SLLONG + DBYTE
    ESEXT_LL,      // SSIGNED + DSIGNED + SLLONG + DSHORT
    EUI_LL,        // SSIGNED + DSIGNED + SLLONG + DINT
    EULL_LL,       // SSIGNED + DSIGNED + SLLONG + DLLONG
  };

  /*
   * This is a dimensional array. The subscripts are
   * 0th dimension - destSigned
   * 1st dimension - (destSize/4)
   * 2nd dimension - (srcSize/4)
   */
  private static final String[][][] riFuncNames = {
    /* [0] unsigned */
    {
      /* [0][0], invalid            */
      {},
      /* [0][1], dest = uint, ulong */
      { /* [0][1][0] invalid                   */ null,
        /* [0][1][1] src = float               */ "__fixunssfsi",
        /* [0][1][2] src = double, long double */ "__fixunsdfsi",
      },
      /* [0][2], dest = u long long */
      { /* [0][2][0] invalid                   */ null,
        /* [0][2][1] src = float               */ "__fixunssfdi",
        /* [0][2][2] src = double, long double */ "__fixunsdfdi",
      },
    },
    /* [1] signed */
    {
      /* [1][0], invalid            */
      {},
      /* [1][1], dest = int, long   */
      { /* [1][1][0] invalid                   */ null,
        /* [1][1][1] src = float               */ "__fixsfsi",
        /* [1][1][2] src = double, long double */ "__fixdfsi",
      },
      /* [1][2], dest = long long   */
      { /* [1][2][0] invalid                   */ null,
        /* [1][2][1] src = float               */ "__fixsfdi",
        /* [1][2][2] src = double, long double */ "__fixdfdi",
      },
    },
  };

  private static final String[][] irFuncNames = {
    /* row [0], invalid         */
    {},
    /* row [1], src = int, long */
    { /* [1][0] invalid                    */ null,
      /* [1][1] dest = float               */ "__floatsisf",
      /* [1][2] dest = double, long double */ "__floatsidf",
    },
    /* row [2], src = long long */
    { /* [2][0] invalid                    */ null,
      /* [2][1] dest = float               */ "__floatdisf",
      /* [2][2] dest = double, long double */ "__floatdidf",
    },
  };

  public static final String[] ftnsMacosx = {"", "lo16", "ha16"};
  public static final String[] ftnsLinux  = {"", "l", "ha"};

  /**
   * Map from size to integer type.
   * Index is (size - 1)/4.
   */
  private static final String[] intArgType   = {"i", "l", "L", "xxxx"};
  /**
   * Map from size to float type.
   * Index is (size - 1)/4.
   */
  private static final String[] fltArgType  = {"f", "d", "d", "D"};

  private static final short[] quadIntUses   = {
    PPCRegisterSet.FIA_REG + 0, PPCRegisterSet.FIA_REG + 1, PPCRegisterSet.FIA_REG + 2,
    PPCRegisterSet.FIA_REG + 3};
  private static final short[] quadFltUses   = {
    PPCRegisterSet.FFA_REG + 0, PPCRegisterSet.FFA_REG + 1, PPCRegisterSet.FFA_REG + 2,
    PPCRegisterSet.FFA_REG + 3};

  private static int[] nxtMvReg = new int[5]; // Hold temporary register values.

  private int structAddress;     // Register containing structure address for routines that return structures.
  private int structSize;        // Register containing size of structure for routines that return structures.
  private int localVarSize;      // Size of the area on the stack used for local variables.
  private int isa;               // The ISA in use.
  private int os;                // The OS in use.
  private int adrDispReg;
  private int mask;              // Integer registers saved during routine execution.
  private int fmask;             // Floating point registers saved during routine execution.
  private int entryOverflowSize; // Size of the area on the stack used for overflow arguments.
  private int maxOverflowSize;   // The max size of stack area used for overflow arguments of all calls by this routine.

  // If the address of a formal parameter is taken, then that
  // parameter should be moved to stack, even if it was passed in a
  // register. That formal parameter behaves as a local variable,
  // allocated space in the local variables area.  formalAsLocal[i] ==
  // true, if formal parameter i is to be treated as a local.

  private boolean[] formalAsLocal;      
  private boolean   usesAddressDisp;
  private boolean   g5;                 // True if isa > G4.
  private boolean   macosx;             // True if os > linux.
  private boolean   usesFloatReg;       // True if function call uses a floating point argument passing register.

  private StackDisplacement vaArgStart;      // Index in local variable array of first variable argument.
  private Displacement overflowArgArea; // see @setVaListType.
  private Set<String>  calledFuncs;
  private Label        osxLabel;

  private Vector<StackDisplacement> argDisp;  // Displacement to struct address arguments on stack.
  private Vector<StackDisplacement> localVar; // Set of Displacements that reference variables on the stack.
  private Vector<StackDisplacement> paramVar; // Parameters that need to be offset by the stack frame size
                                              // because they belong in the previous stack frame (va args for OS X).

  /**
   * @param cg is the call graph to be transformed
   * @param machine specifies machine details
   * @param features controls the instructions generated
   */
  public PPCGenerator(CallGraph cg, Machine machine, int features)
  {
    super(cg, null, machine, features);
    PPCMachine sm     = (PPCMachine) machine;
    this.isa          = sm.getISA();
    this.os           = sm.getOS();
    this.g5           = isa > PPCMachine.G4;   // PPC G5 instructions.
    this.macosx       = os > PPCMachine.LINUX; // MAC OS X instructions.
    this.calledFuncs  = new HashSet<String>();
    this.un           = new UniqueName("$$");
    this.registers    = (g5 ?
                         (RegisterSet) new PPCG5RegisterSet(macosx) :
                         (RegisterSet) new PPCG4RegisterSet(macosx));

    if (!(machine instanceof PPCMachine))
      throw new scale.common.InternalError("Not correct machine " + machine);

    readOnlyDataArea = RDATA;
  }

  /**
   * Generate the machine instructions for a CFG.
   */
  public void generateScribble()
  {
    mask              = 0;
    fmask             = 0;
    localVarSize      = 0;
    entryOverflowSize = 0;
    maxOverflowSize   = 0;
    usesAddressDisp   = false;
    localVar          = new Vector<StackDisplacement>(23);
    paramVar          = new Vector<StackDisplacement>(23);
    formalAsLocal     = null;
    structAddress     = 0;
    structSize        = 0;
    argDisp           = new Vector<StackDisplacement>(23);
    usesFloatReg      = false;
    osxLabel          = new Label();
    adrDispReg        = 0;
    stkPtrReg         = PPCRegisterSet.SP_REG;
    overflowArgArea   = null;

    super.generateScribble();
  }

  /**
   * Do peephole optimizations before registers are allocated.
   */
  protected void peepholeBeforeRegisterAllocation(Instruction first)
  {
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

    PPCAssembler asm = new PPCAssembler(this, source);
    asm.assemble(emit, dataAreas);

    if (macosx)
      assemblePIC(emit);  //emit code at the end necessary for position
    //independent code handling for macosx
  }

  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public static String displayDisp(Displacement disp, int ftn, boolean macosx)
  {
    if (ftn == FT_NONE)
      return disp.toString();

    if (macosx) {
      StringBuffer buf = new StringBuffer(ftnsMacosx[ftn]);
      buf.append('(');
      buf.append(disp.toString());
      buf.append(')');
      return buf.toString();
    }

    StringBuffer buf = new StringBuffer(disp.toString());
    buf.append('@');
    buf.append(ftnsLinux[ftn]);
    return buf.toString();
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
    case 1:
      t = SpaceAllocation.DAT_BYTE;
      break;
    case 2:
      t = SpaceAllocation.DAT_SHORT;
      break;
    case 4:
      t = flt ? SpaceAllocation.DAT_FLT : SpaceAllocation.DAT_INT;
      break;
    case 8:
      t = flt ? SpaceAllocation.DAT_DBL : SpaceAllocation.DAT_LONG;
      break;
    default:
      throw new scale.common.InternalError("Can't allocate objects of size " + size);
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

    if (visibility == Visibility.LOCAL)
      ;
    else if (macosx && (visibility == Visibility.GLOBAL) && (init == null)) {
      vd.setName("_" + name);
      name = "L_" + name + "$non_lazy_ptr";
    } else {
      name = "_" + name;
      vd.setName(name);
    }
    int aln = vd.isCommonBaseVar() ? machine.generalAlignment() : dt.alignment(machine);
    int ts  = 1;

    try {
      ts = vt.memorySizeAsInt(machine);
    } catch (java.lang.Error ex) {
    }

    // TODO: Check with Katie, if these statements are correct.

    // These statements are incorrect. sbss, bss depend on the size,
    // etc.

    // In linux, uninitialized local variables are in the SBSS section
    // In linux, uninitialized global variables are in the BSS section
    // In macosx it is the BSS always

    int area = readOnly ? RDATA : DATA;
    if (init == null)
      area = (macosx) ? BSS : ((visibility == Visibility.LOCAL) ? SBSS : BSS);

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
      long bs   = vt.memorySize(machine);
      // only put them in int regs for now...
      int rft   = RegisterSet.INTREG;
      int  regd = registers.newTempRegister(rft + ((bs > 4) ?
                                                   ((bs > 8) ?
                                                    RegisterSet.QUADREG :
                                                    RegisterSet.DBLEREG):
                                                   0));
      assert ((ft == AggregateType.FT_F64) || (ft == AggregateType.FT_INT)) :
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
    int               sts   = vt.memorySizeAsInt(machine);
    StackDisplacement sdisp = new StackDisplacement(localVarSize);

    defineDeclOnStack(vd, sdisp);
    localVarSize += Machine.alignTo(sts, PPCG4RegisterSet.IREG_SIZE);
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
    Type   vt   = processType(rd);
    String name = rd.getName();

    if (macosx)
      name = "L_" + name + "$stub";

    int                handle = allocateTextArea(name, TEXT);
    SymbolDisplacement disp   = new SymbolDisplacement(name, handle);

    associateDispWithArea(handle, disp);
    defineRoutineInfo(rd, disp);
    if (macosx)
      calledFuncs.add(rd.getName());
    if (rd.getName().startsWith("_scale_imag")) {
      System.out.println("** prd " + rd);
      Debug.printStackTrace();
    }
  }

  /**
   * Return the register used to return the function value.
   * @param regType specifies the type of value
   * @param isCall is true if the calling routine is asking
   */
  public int returnRegister(int regType, boolean isCall)
  {
    return registers.isFloatType(regType) ? PPCRegisterSet.FR_REG : PPCRegisterSet.IR_REG;
  }

  /**
   * Return the register used as the first argument in a function call.
   * @param regType specifies the type of argument value
   */
  public final int getFirstArgRegister(int regType)
  {
    return registers.isFloatType(regType) ? PPCRegisterSet.FFA_REG : PPCRegisterSet.FIA_REG;
  }

  protected short[] genSingleUse(int reg)
  {
    short[] uses = new short[2];
    uses[0] = (short) reg;
    uses[1] = PPCRegisterSet.SP_REG;
    return uses;
  }

  protected short[] genDoubleUse(int reg1, int reg2)
  {
    short[] uses = new short[3];
    uses[0] = (short) reg1;
    uses[1] = (short) reg2;
    uses[2] = PPCRegisterSet.SP_REG;
    return uses;
  }

  /**
   * Assign the routine's arguments to registers or the stack.
   */
  protected void layoutParameters()
  {
    if(macosx)
      macosxLayoutParameters();
    else
      linuxLayoutParameters();
  }

  /**
   * Assign the routine's arguments to registers or the stack.
   */
  private void macosxLayoutParameters()
  {
    ProcedureType pt          = (ProcedureType) processType(currentRoutine);
    int           nextArgF    = 0;
    int           nextArgG    = 0;
    int           maxFArgRegs = MAX_ARG_REGS;
    Type          rt          = processType(pt.getReturnType());
    long          offset      = ARG_SAVE_OFFSET_MACOSX;

    maxFArgRegs = MAX_FARG_REGS_MACOSX;

    // First register has return address of struct if return type is struct.

    if (!(rt.isAtomicType() || rt.isVoidType())) {
      nextArgG++;
      offset += PPCG4RegisterSet.IREG_SIZE;
    }

    int l = pt.numFormals();
    formalAsLocal = new boolean[l];

    for (int i = 0; i < l; i++) {
      formalAsLocal[i] = false;
      FormalDecl fd = pt.getFormal(i);

      if (fd instanceof UnknownFormals) {
        Displacement disp = new StackDisplacement(offset);
        defineDeclOnStack(fd, disp);
        return;
      }

      boolean volat = fd.getType().isVolatile();
      Type    vt    = processType(fd);
      long    size  = vt.memorySize(machine);

      if (vt.isAtomicType()) {

        // The first MAX_ARG_REGS scaler arguments are in the argument
        // registers, remaining words have already been placed on the
        // stack by the caller.

        if (((nextArgG >= MAX_ARG_REGS) && !vt.isRealType()) ||
            (nextArgF >= maxFArgRegs && vt.isRealType())) { // Argument is on the stack.
          Displacement disp = new StackDisplacement(offset);

          defineDeclOnStack(fd, disp);
          offset += Machine.alignTo(size, PPCG4RegisterSet.IREG_SIZE);
          continue;
        }

        if (fd.addressTaken() || volat) { // Argument value will be transferred to the stack.
          StackDisplacement disp = new StackDisplacement(offset);
          defineDeclOnStack(fd, disp);
        } else { // Place arguement in register.
          int tr;
          if (!vt.isRealType() && size <= 4)
            tr = registers.newTempRegister(RegisterSet.INTREG);
          else if (!vt.isRealType() && size > 4)
            tr = registers.newTempRegister(RegisterSet.INTREG + RegisterSet.DBLEREG);
          else
            tr = registers.newTempRegister(RegisterSet.FLTREG);
          defineDeclInRegister(fd, tr, ResultMode.NORMAL_VALUE);
        }

        nextArgG++;
        if (size > 4)
          nextArgG++;

        if (vt.isRealType())
          nextArgF++;

        offset += Machine.alignTo(size, PPCG4RegisterSet.IREG_SIZE);
        continue;
      }

      AggregateType at = vt.returnAggregateType();
      if (at != null) {
        // The first eight words of a struct are in the argument registers,
        // remaining words have already been placed on the stack by the caller.
        int           ts = at.memorySizeAsInt(machine);

        if (usesVaStart) { // Argument must be saved on the stack.
          Displacement disp = new StackDisplacement(offset);
          offset += ts;
          defineDeclOnStack(fd, disp);
          nextArgG += ((ts + PPCG4RegisterSet.IREG_SIZE - 1) / PPCG4RegisterSet.IREG_SIZE);
          continue;
        }

        int inc = (ts + PPCG4RegisterSet.IREG_SIZE - 1) / PPCG4RegisterSet.IREG_SIZE;
        if (!useMemory &&
            !fd.addressTaken() &&
            ((nextArgG + inc) <= MAX_ARG_REGS) &&
            machine.keepTypeInRegister(at, true)) {
          int ft   = at.allFieldsType();
          int rft  = RegisterSet.INTREG; // always use int regs for now
          int regd = registers.newTempRegister(rft + ((ts > 4) ?
                                                      ((ts > 8) ?
                                                       RegisterSet.QUADREG :
                                                       RegisterSet.DBLEREG) :
                                                      0));
          offset += ts;
          defineDeclInRegister(fd, regd, ResultMode.STRUCT_VALUE);
        } else {
          StackDisplacement disp = new StackDisplacement(offset);
          defineDeclOnStack(fd, disp);
          offset += inc * PPCG4RegisterSet.IREG_SIZE;

          if (nextArgG < MAX_ARG_REGS) {
            int s = (MAX_ARG_REGS - nextArgG) * PPCG4RegisterSet.IREG_SIZE;
            if (s > ts)
              s = ts;
            s = (((s + PPCG4RegisterSet.IREG_SIZE - 1) / PPCG4RegisterSet.IREG_SIZE) *
                 PPCG4RegisterSet.IREG_SIZE);
          }

          nextArgG += inc;
          continue;
        }
      }

      throw new scale.common.InternalError("Parameter type " + fd);
    }
  }

  /**
   * Assign the routine's arguments to registers or the stack. This is done
   * by the callee. Also see generateProlog()
   */
  private void linuxLayoutParameters()
  {
    ProcedureType pt          = (ProcedureType) processType(currentRoutine);
    int           nextArgF    = 0;
    int           nextArgG    = 0;
    long          offset      = ARG_SAVE_OFFSET_LINUX;
    int           maxFArgRegs = MAX_ARG_REGS;
    Type          rt          = processType(pt.getReturnType());

    /*
     * If this function returns a struct/union/long double/whatever
     * (anything that does not meet the requirements for being returned in
     * registers), it has to be returned in a storage buffer
     * allocated by its caller. The address of the buffer is passed in r3
     * (PPCRegisterSet.FIA_REG)
     */

    if (rt.isAggregateType())
      nextArgG++;

    // Process each formal parameter.

    int l = pt.numFormals();
    formalAsLocal = new boolean[l];
    for (int i = 0; i < l; i++) {
      formalAsLocal[i] = false;
      FormalDecl fd = pt.getFormal(i);
      // If this formal parameter is ellipsis (...), stop here

      if (fd instanceof UnknownFormals)
        return;

      boolean volat = fd.getType().isVolatile();
      Type    vt    = processType(fd);
      long    size  = vt.memorySize(machine);

      // Process other types of formal paramters.

      if (vt.isAtomicType()) {
        // This is an atomic type. Will be passed in a register if a
        // register is available, or will be passed in stack. The
        // conditions are the same as given in linuxCallArgs. Please read
        // that linuxCallArgs function for more comments/info about the
        // algorithm.

        boolean isReal     = vt.isRealType();
        boolean isLongLong = !isReal && (64 == ((AtomicType) vt).bitSize());
        if ((isReal &&                 (nextArgF >= MAX_ARG_REGS )) ||
            (!isReal && !isLongLong && (nextArgG >= MAX_ARG_REGS )) ||
            (!isReal &&  isLongLong && (nextArgG >= (MAX_ARG_REGS - 1)))) {

          // The parameter is available on stack

          Displacement disp = new StackDisplacement(offset);
          defineDeclOnStack(fd, disp);
          offset += Machine.alignTo(size, (isReal ?
                                           PPCG4RegisterSet.FREG_SIZE :
                                           PPCG4RegisterSet.IREG_SIZE));
          continue;
        }

        // The parameter is available in a register.

        if (fd.addressTaken() || volat) {
          // This parameter will be transferred to the stack. This will be
          // like a local variable.
          // FIXME: alignment likely to be incorrect

          StackDisplacement disp = new StackDisplacement(localVarSize);
          fd.setDisplacement(disp);
          defineDeclOnStack(fd, disp);
          localVarSize += Machine.alignTo(size,
                                          (isReal ?
                                           PPCG4RegisterSet.FREG_SIZE :
                                           PPCG4RegisterSet.IREG_SIZE));
          localVar.addElement(disp);
          formalAsLocal[i] = true;
        } else { // The parameter will be available in a register.
          int registerType;
          if (isReal)
            registerType = RegisterSet.FLTREG;
          else
            registerType = RegisterSet.INTREG + (isLongLong ? RegisterSet.DBLEREG : 0);

          int tr = registers.newTempRegister(registerType);
          defineDeclInRegister(fd,
                               tr,
                               ResultMode.NORMAL_VALUE);
        }

        // Appropriately moving to the next register.

        if (isReal)
          nextArgF++;
        else {
          if (isLongLong) {
            if ((nextArgG % 2) == 1)
              nextArgG++;

            nextArgG += 2;
          } else
            nextArgG++;
        }
      } else if (vt.isAggregateType()) {
        // This is an aggregate type struct/union etc.
        // The ADDRESS of the struct is passed in an integer registers (if
        // anything is available) or in stack

        if (nextArgG < MAX_ARG_REGS) {
          int nr = registers.newTempRegister(vt.getTag());
          defineDeclInRegister(fd, nr, ResultMode.ADDRESS);
          nextArgG++;
        } else
          throw new scale.common.NotImplementedError("Aggregate in stack");
        // int nr = registers.newTempRegister(RegisterSet.ADRREG);
        // defineDeclInRegister(fd, nr, ResultMode.ADDRESS);
      } else
        throw new scale.common.NotImplementedError("No other formal parameter type has been taken care of");
    }
  }

  private Displacement defFloatValue(double v, int size)
  {
    int section = LIT4;
    int type    = SpaceAllocation.DAT_FLT;
    int aln     = 4;

    if (size > 4) {
      type = SpaceAllocation.DAT_DBL;
      section = LIT8;
      aln = 8;
    }

    Displacement disp = findAreaDisp(section, type, true, size, v, aln);

    if (disp == null) {
      String name   = un.genName();
      int handle = allocateData(name, section, type, size,
                                true, new Double(v), 1, aln);
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
      type = SpaceAllocation.DAT_LONG;
      aln = 8;
    }

    Displacement disp = findAreaDisp(section, type, true, size, v, aln);

    if (disp == null) {
      String name   = un.genName();
      int    handle = allocateData(name, section, type, size, true, new Long(v), 1, aln);
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
    throw new scale.common.NotImplementedError("defStringValue");
  }

  /**
   * Generate instructions to move data from one register to another.
   * If one is an integer register and the other is a floating point register,
   * a memory location may be required.
   */
  protected void genRegToReg(int src, int dest)
  {
    if (registers.rangeBegin(src) == registers.rangeBegin(dest))
      return;

    int srcs  = registers.rangeBegin(src);
    int dests = registers.rangeBegin(dest);

    if (registers.isFloatType(registers.getType(src)) &&
        registers.isFloatType(registers.getType(dest)))
      appendInstruction(new FDrInstruction(Opcodes.FMR, dest, src));
    else if (!registers.isFloatType(registers.getType(src)) &&
             !registers.isFloatType(registers.getType(dest))) {
      if (registers.doubleRegister(src) || registers.doubleRegister(dest)) {
        appendInstruction(new FDrInstruction(Opcodes.MR, dests, srcs));
        appendInstruction(new FDrInstruction(Opcodes.MR, dests + 1, srcs + 1));
      } else if (registers.quadRegister(src) && registers.quadRegister(dest)) {
        appendInstruction(new FDrInstruction(Opcodes.MR, dests, srcs));
        appendInstruction(new FDrInstruction(Opcodes.MR, dests + 1, srcs + 1));
        appendInstruction(new FDrInstruction(Opcodes.MR, dests + 2, srcs + 2));
        appendInstruction(new FDrInstruction(Opcodes.MR, dests + 3, srcs + 3));
      } else
        appendInstruction(new FDrInstruction(Opcodes.MR, dest, src));
    } else if (!registers.isFloatType(registers.getType(src)) &&
               registers.isFloatType(registers.getType(dest))) {
      if (registers.doubleRegister(src))
        genIntToReal(src, 8, dest, PPCG4RegisterSet.FREG_SIZE);
      else
        genIntToReal(src, PPCG4RegisterSet.IREG_SIZE, dest, PPCG4RegisterSet.FREG_SIZE);
    } else if (registers.isFloatType(registers.getType(src)) &&
               !registers.isFloatType(registers.getType(dest))) {
      if (registers.doubleRegister(dest))
        genRealToInt(src, PPCG4RegisterSet.FREG_SIZE, dest, 8, true);
      else
        genRealToInt(src, PPCG4RegisterSet.FREG_SIZE, dest, PPCG4RegisterSet.IREG_SIZE, true);
    }
  }

  /**
   * Load an address of a memory location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement or
   * offset of one)
   * @return the register that is set with the address
   */
  protected int loadMemoryAddress(Displacement disp)
  {
    int adrReg = registers.getResultRegister(RegisterSet.ADRREG);

    if (macosx) {
      Displacement osxDisp = new LabelDisplacement(osxLabel);
      disp = new DiffDisplacement(disp, osxDisp);
      appendInstruction(new FDrdInstruction(Opcodes.ADDIS, adrReg, adrDispReg, disp, FT_HI16, macosx));
      usesAddressDisp = true;
      if (!disp.isNumeric()) {
        String dispString = disp.toString();
        if (dispString.indexOf("$non_lazy_ptr") != -1) {
          appendInstruction(new MemoryInstruction(Opcodes.LWZ, adrReg, adrReg, disp, FT_LO16, macosx));
          return adrReg;
        }
      }
    } else
      appendInstruction(new FDdInstruction(Opcodes.LIS, adrReg, disp, FT_HI16, macosx));

    appendInstruction(new FDdrInstruction(Opcodes.LA, adrReg, adrReg, disp, FT_LO16, macosx));

    return adrReg;
  }

  /**
   * Load an address of a stack location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement or
   * offset of one)
   * @return the register that is set with the address
   */
  protected int loadStackAddress(Displacement disp)
  {
    int dest = registers.getResultRegister(RegisterSet.ADRREG);
    appendInstruction(new FDdrInstruction(Opcodes.LA, dest, PPCRegisterSet.SP_REG, disp, FT_NONE, macosx));
    //  appendInstruction(new FDrdInstruction(Opcodes.ADDIS, dest,
    //        PPCRegisterSet.SP_REG, disp, FT_HI16, macosx));
    //  appendInstruction(new FDdrInstruction(Opcodes.LA, dest,
    //        dest, disp, FT_LO16, macosx));
    return dest;
  }

  /**
   * Calculate the element index specified.
   * Return the offset from the address in <code>resultRegAddressOffset</code>.
   * resultReg is set to the register containing the index value.
   * @param offset is an expression specifying the offset field of the
   * ArrayIndexExpr
   * @param index is an expression specifying the index field of the
   * ArrayIndexExpr
   */
  private void calcArrayOffset(Expr offset, Expr index)
  {
    int  sw   = 0; // Specify which combination of index & offset.
    long oval = 0; // Constant offset value.
    long ival = 0; // Constant index value.

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
      } else {
        throw new scale.common.NotImplementedError("LiteralExpr");
      }
    }

    switch (sw) {
    case 0:
      needValue(index);
      int ind = resultReg;
      needValue(offset);
      int off = resultReg;
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;

      if (negOff) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new FDrInstruction(Opcodes.NEG, tr, off));
        resultReg = tr;
        return;
      }

      int tr = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new FDrrInstruction(Opcodes.ADD, tr, ind, off));
      resultReg = tr;
      return;
    case 1:
      needValue(index);
      resultRegAddressOffset = oval;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    case 2:
      needValue(offset);
      if (negOff) {
        int tr2 = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new FDrInstruction(Opcodes.NEG, tr2, resultReg));
        resultReg = tr2;
      }
      resultRegAddressOffset = ival;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    default:
    case 3:
      //  resultRegAddressOffset = oval + ival;
      //  resultRegMode = ResultMode.NORMAL_VALUE;
      //  tr = registers.newTempRegister(RegisterSet.INTREG);
      //  genLoadImmediate(0, tr);
      //  resultReg = tr;
      //  return;
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      tr = registers.newTempRegister(RegisterSet.INTREG);
      genLoadImmediate(oval + ival, tr);
      resultReg = tr;
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
    Type        et      = vt.getPointedTo().getCoreType();
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

    int tr2;
    tr2 = registers.newTempRegister(RegisterSet.INTREG);
    int tr3 = registers.newTempRegister(RegisterSet.INTREG);
    genMultiplyLit(bs, tr, tr3, PPCG4RegisterSet.IREG_SIZE);
    appendInstruction(new FDrrInstruction(Opcodes.ADD, tr2, tr3, arr));

    long offsetl = offseth * bs + offseta;
    loadFromMemoryWithOffset(dest, tr2, offsetl, bs, daln, et.isSigned(), false);
  }

  /**
   * Generate instructions to load an immediate integer value added to the
   * value in a register into a register.
   * @param value is the value to load
   * @param dest is the register conatining the result
   */
  protected void genLoadImmediate(long value, int base, int dest)
  {
    boolean bis0 = (base == -1);
    boolean disd = registers.doubleRegister(dest);

    if (disd && !bis0)
      throw new scale.common.NotImplementedError("disd && !bis0");

    if (annotateCode) {
      StringBuffer buf = new StringBuffer();
      buf.append(disd ? "(r" : "r");
      buf.append(dest);
      if (disd) {
        buf.append(", r");
        buf.append(dest + 1);
        buf.append(')');
      }

      buf.append(" <- ");

      if (!bis0) {
        buf.append('r');
        buf.append(base);
      }
      buf.append("0x");
      buf.append(Long.toHexString(value & (disd ? 0xffffffffffffffffL : 0xffffffffL)));
      buf.append(" aka ");
      buf.append(Long.toString(value));

      if (annotateCode)
        appendInstruction(new CommentMarker(buf.toString()));
    }

    /*
     * value is a 64 bit quantity.
     * If we are dealing with double register
     *    1. the 32 MSBs of value should go into register `dest'
     *    2. the 32 LSBs should go into register `dest + 1'
     * If we are dealing with a single 32 bit register
     *    the 32 bits of value (implicitly the LSBs) should go into dest
     *
     * We will split `value' into forDest and forDest1
     */
    int forDest;
    int forDest1;

    if (disd) {
      forDest   = (int) (value >> 32);
      forDest1 = (int) (value & 0xffffffff);
    } else {
      assert ((value & 0xffffffff) == value) : "Single register used, but value is huge";
        forDest   = (int) value;
        // dummy initialization. forDest1 will not be used, when disd == 0
        forDest1 = 0;
    }
    /*
     * Putting values into dest. We will be doing this irrespective of
     * whether we are dealing with a double register or not.
     */
    if (bis0)
      // We have no base register. Just load the value, directly into
      // dest forDest might be a large value. see @adjustImmediate.
      appendInstruction(new FDcInstruction(Opcodes.LI, dest, forDest));
    else {
      // We have a base register. We will call genLoadImmediate()
      // without a base register and then add the base register to the
      // result.

      int tmpReg = registers.newTempRegister(RegisterSet.INTREG);
      genLoadImmediate(forDest, tmpReg);
      appendInstruction(new FDrrInstruction(Opcodes.ADD, dest, base, tmpReg));
    }

    // Putting values into dest + 1. We will be doing this only when
    // we have a double register.

    if (disd) {
      if (bis0) {
        // We have no base register. Just load the value, directly into dest + 1.
        // forDest1 might be a large value. see @adjustImmediate

        appendInstruction(new FDcInstruction(Opcodes.LI, dest + 1, forDest1));
      } else {
        // We have a base register. We have to add the value to base and
        // then put it into dest + 1. There will be a carry over which has
        // to be propagated into dest.
        // TODO: Not yet handled
        throw new scale.common.NotImplementedError("genLoadImmediate: 64 bit quantity with a base register");
      }
    }
  }

  protected int genLoadImmediate(long value, int dest)
  {
    genLoadImmediate(value, -1, dest);
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
   * @return the register containing the value (usually dest but may be a
   * hardware zero register)
   */
  protected int genLoadDblImmediate(double value, int dest, int destSize)
  {
    Displacement disp = defFloatValue(value, destSize);
    int          adr  = loadMemoryAddress(disp);

    appendInstruction(new LoadInstruction(destSize > 4 ? Opcodes.LFD : Opcodes.LFS, dest, adr, getDisp(0)));

    return dest;
  }

  /**
   * Return the bottom 16 bits of the value.  Add the upper bits of the
   * value to regb.
   * @param regb is the register conatining the result
   * @return the lower 16 bits of the constant
   */
  protected long genLoadHighImmediate(long value, int regb)
  {
    /*
     * Please refer to PPCGenerator.java version 1.83 for the original code
     * of this function. This function is now removed.
     */
    throw new scale.common.NotImplementedError("genLoadHighImmediate");
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
    throw new scale.common.NotImplementedError("genLoadHighImmediate");
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
    throw new scale.common.NotImplementedError("allocStackAddress");
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param aligned is true if the data address is known to be properly aligned
   * @param signed is true if the data is to be sign extended
   */
  private void loadFromMemory(int          dest,
                              int          address,
                              Displacement disp,
                              int          dftn,
                              int          size,
                              long         alignment,
                              boolean      signed)
  {
    if (size <= PPCG4RegisterSet.FREG_SIZE) {
      loadFromMemoryX(dest, address, disp, dftn, size, alignment, signed);
      return;
    }

    // Structs

    while (true) {
      int s = size;
      if (s > PPCG4RegisterSet.IREG_SIZE)
        s = PPCG4RegisterSet.IREG_SIZE;
      loadFromMemoryX(dest, address, disp, dftn, s, alignment, true);
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
    int     r1     = dest;
    boolean p      = registers.doubleRegister(dest);
    int     opcode = 0;
    int     dests;

    switch (size) {
    case 1:
      appendInstruction(new LoadInstruction(Opcodes.LBZ, r1, address, disp));
      if (signed)
        appendInstruction(new FDrInstruction(Opcodes.EXTSB, r1, r1));
      return;
    case 2:
      appendInstruction(new LoadInstruction(Opcodes.LHZ, r1, address, disp));
      if (signed)
        appendInstruction(new FDrInstruction(Opcodes.EXTSH, r1, r1));
      return;
    case 4:
      opcode = Opcodes.LWZ;
      if (registers.floatRegister(dest))
        opcode = Opcodes.LFS;
      appendInstruction(new LoadInstruction(opcode, r1, address, disp));
      return;
    case 8:
      if (registers.floatRegister(dest)) {
        if ((alignment % 8) == 0) {
          appendInstruction(new LoadInstruction(Opcodes.LFD, dest, address, disp));
        } else {
          System.err.println("loadFromMemoryX: " +
                             "dest: " + dest + " " +
                             "address: " + address + " " +
                             "disp: " + disp + " " +
                             "size: " + size + " " +
                             "alignment: " + alignment + " " +
                             "signed: " + signed);
          throw new scale.common.NotImplementedError("loadFromMemoryX");
        }
        return;
      }

      if (!registers.floatRegister(dest)) {
        if (g5 && alignment == 8) {
          appendInstruction(new LoadInstruction(Opcodes.LD, dest, address, disp));
          return;
        }
        assert (registers.doubleRegister(dest)) : "8-byte non-real value must go in double register";
          appendInstruction(new LoadInstruction(Opcodes.LWZ, dest, address, disp));
          appendInstruction(new LoadInstruction(Opcodes.LWZ, dest + 1, address, disp.offset(4)));
          return;
      }
    }
    throw new scale.common.InternalError("Unknown data type size (" + size + ")");
  }

  private void loadFromMemory(int dest, int address, Displacement disp,
                              int size, long alignment, boolean signed)
  {
    loadFromMemory(dest, address, disp, FT_NONE, size, alignment, signed);
  }

  /**
   * Generate instructions to load data from memory at the address in a
   * register plus an offset.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real - this argument is
   * not used in this architecture and will be ignored
   */
  protected void loadFromMemoryWithOffset(int dest, int address, long offset,
                                          int size, long alignment, boolean signed,
                                          boolean real)
  {
    loadFromMemory(dest, address, getDisp((int) offset), size, alignment, signed);
  }

  /**
   * Generate instructions to load data from memory at the address in a
   * register plus an offset.  The offset must not be symbolic.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real - this argument is
   * not used in this architecture and will be ignored
   */
  protected void loadFromMemoryWithOffset(int dest, int address, Displacement offset, int size,
                                          long alignment, boolean signed, boolean real)
  {
    assert offset.isNumeric() : "Symbolic displacement " + offset;
      loadFromMemory(dest, address, offset, FT_NONE, size, alignment, signed);
  }

  /**
   * Generate instructions to load data from memory at the address that is
   * the sum of the two index register values.
   * @param dest is the destination register
   * @param index1 is the register containing the first index
   * @param index2 is the register containing the second index
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real - this argument is
   * not used in this architecture and will be ignored
   */
  protected void loadFromMemoryDoubleIndexing(int dest, int index1, int index2, int size,
                                              long alignment, boolean signed, boolean real)
  {
    throw new scale.common.NotImplementedError("loadFromMemoryDoubleIndexing");
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param disp specifies the offset from the address register value
   * @param dftn - RT_NONE, RT_LITBASE_USE, etc
   * @param bits specifies the size of the data to be loaded in bits - must
   * be 32 or less
   * @param bitOffset specifies the offset to the field in bits
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   */
  protected void loadBitsFromMemory(int dest, int address, Displacement disp,
                                    int dftn, int bits, int bitOffset, int alignment, boolean signed)
  {
    if (annotateCode) {
      StringBuffer buf = new StringBuffer("loadBitsFromMemory");
      buf.append(" bits: ");
      buf.append(bits);
      buf.append(" bitOffset: ");
      buf.append(bitOffset);
      buf.append(" signed: ");
      buf.append(signed);
      appendInstruction(new CommentMarker(buf.toString()));
    }

    int span = bits + bitOffset;

    if ((bits <= 0) || (64 < bits))
      throw new scale.common.InternalError("Invalid bits: " + bits);

    if ((bitOffset < 0) || (32 <= bitOffset))
      throw new scale.common.InternalError("Invalid bitOffset : " + bitOffset);

    if (span <= 32) {
      /*
       * If the field is of type long long, then dest will be a
       * doubleRegister, in which case we first have to write it to a
       * regular integer register and then convert it to (un)signed long long.
       */
      int dest2;
      if (registers.doubleRegister(dest)) {
        dest2 = registers.newTempRegister(RegisterSet.INTREG);
      } else {
        dest2 = registers.lastRegister(dest);
        if (dest2 != dest) {
          throw new scale.common.InternalError("dest2 != dest");
        }
      }
      /*
       * Loading the data from memory and correspondingly adjusting the
       * bits
       */
      appendInstruction(new LoadInstruction(Opcodes.LWZ, dest2, address, disp, dftn, macosx));
      if (signed) {
        if (0 != bitOffset)
          appendInstruction(new FDrcInstruction(Opcodes.SLWI, dest2, dest2, bitOffset, macosx));

        appendInstruction(new FDrcInstruction(Opcodes.SRAWI, dest2, dest2, 32 - bits, macosx));
      } else
        appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, dest2, dest2, span % 32, 32 - bits, 31));

      /*
       * Now moving the data into `dest' register
       */
      if (dest2 != dest) {
        convertIntRegValue(/* src        */ dest2,
                           /* srcSize    */ 4,
                           /* srcSigned  */ signed,
                           /* dest       */ dest,
                           /* destSize   */ 8,
                           /* destSigned */ signed);
      }
    } else if (span <= 64) {
      // if span > 32, this has to be a field of type "long long", and dest
      // has to be a doubleRegister.

      if (! registers.doubleRegister(dest))
        throw new scale.common.InternalError("src not a long long");

      // We have to read in `bits' bits, say x0 bits from word 0 in memory
      // and x1 bits from word 1 in memory.

      int x0       = 32 - bitOffset;
      int x1       = bits - x0;
      int dataReg0 = registers.newTempRegister(RegisterSet.INTREG);
      int dataReg1 = registers.newTempRegister(RegisterSet.INTREG);
      int addrReg1 = registers.newTempRegister(RegisterSet.ADRREG);

      loadBitsFromMemory(dataReg0, address, disp, dftn, x0, bitOffset, alignment, signed);
      appendInstruction(new FDrcInstruction(Opcodes.ADDI,
                                            addrReg1,
                                            address,
                                            4,
                                            macosx/* FIXME: constant here */));
      loadBitsFromMemory(dataReg1, addrReg1, disp, dftn, x1, 0, alignment, signed);

      // At this point, dataReg0 contains the x0 MSBits of the data and
      // dataReg1 contains the x1 LSBits of the data. We have to merge
      // these two sets of bits into one.

      if (bits <= 32) {
        // We only have to load into the LSRegister of src and then sign
        // extend it into the MSRegister.

        int dest2 = registers.newTempRegister(RegisterSet.INTREG);
        if (signed) {
          if (0 != bitOffset)
            appendInstruction(new FDrcInstruction(Opcodes.SLWI, dest2, dataReg0, bitOffset, macosx));

          appendInstruction(new FDrcInstruction(Opcodes.SRAWI, dest2, dest2, 32 - bits, macosx));
        } else
          appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, dest2, dataReg0, x1, 32 - bits, 31 - x1));

        appendInstruction(new FDrcccInstruction(Opcodes.RLWIMI, dest2, dataReg1, 0, 32 - x1, 31));
        convertIntRegValue(/* src        */ dest2,
                           /* srcSize    */ 4,
                           /* srcSigned  */ signed,
                           /* dest       */ dest,
                           /* destSize   */ 8,
                           /* destSigned */ signed);
      } else {

        // We have to load bits both into the LSRegister and
        // MSRegister of src.  32 bits will go into LSRegister (all
        // x1 bits and (32 - x1) bits from x0) (bits - 32) bits from
        // x0 will go into MSRegister and have to be sign extended
        // Writing to MSRegister first, to keep the register
        // allocator happy

        if (signed)
          appendInstruction(new FDrcInstruction(Opcodes.SRAWI, dest, dataReg0, 32 - x1, macosx));
        else
          appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, dest, dataReg0, x1, 64 - bits, 31));

        // Writing to the LSRegister

        if (32 != x1)
          appendInstruction(new FDrcccInstruction(Opcodes.RLWIMI, dataReg1, dataReg0, x1, 0, 31 - x1));

        appendInstruction(new FDrInstruction(Opcodes.MR, dest + 1, dataReg1));
      }
    } else {
      System.err.println("bits: " + bits);
      System.err.println("bitOffset: " + bitOffset);
      throw new scale.common.NotImplementedError("loadBitsFromMemory");
    }

    if (annotateCode)
      appendInstruction(new CommentMarker("end loadBitsFromMemory"));
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param aligned is true if the data address is known to be properly aligned
   */
  private void storeIntoMemory(int src, int address, Displacement disp,
                               int dftn, int size, long alignment, boolean real)
  {
    if (annotateCode)
      appendInstruction(new CommentMarker("store " + size + " " + registers.display(src)));

    if (registers.pairRegister(src)) {
      int s = size / 2;
      assert (s <= 16) : "Paired register size " + size;
        storeIntoMemoryX(src + 0, address, disp, dftn, s, alignment);
        storeIntoMemoryX(src + registers.numContiguousRegisters(src),
                         address,
                         disp.offset(s),
                         dftn,
                         s,
                         alignment);
        return;
    }

    if (size <= 8) {
      storeIntoMemoryX(src, address, disp, dftn, size, alignment);
      return;
    }

    // Structs

    while (true) {
      int s = size;
      if (s > 8)
        s = 8;
      storeIntoMemoryX(src, address, disp, dftn, s, alignment);
      src++;
      if (s == 8)
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
  private void storeIntoMemoryX(int src, int address, Displacement disp,
                                int dftn, int size, long alignment)
  {
    int     r5     = src;
    boolean p      = registers.pairRegister(src);
    int     opcode = 0;

    switch (size) {
    case 1:
      appendInstruction(new StoreInstruction(Opcodes.STB, r5, address, disp));
      return;
    case 2:
      appendInstruction(new StoreInstruction(Opcodes.STH, r5, address, disp));
      return;
    case 4:
      opcode = Opcodes.STW;
      if (registers.floatRegister(r5))
        opcode = Opcodes.STFS;
      appendInstruction(new StoreInstruction(opcode, r5, address, disp));
      if (p)
        appendInstruction(new StoreInstruction(opcode,
                                               r5 + registers.numContiguousRegisters(src),
                                               address,
                                               disp.offset(4)));
      return;
    case 8:
      if (annotateCode)
        appendInstruction(new CommentMarker("storeIntoMemoryX case 8"));
      if ((alignment % 8) == 0) {
        opcode = Opcodes.STFD;
        if (!registers.floatRegister(src)) {
          if (g5)
            opcode = Opcodes.STD;
          else {
            // FIXME: Minor hack to solve a problem. Don't know if this is
            // a neat way of doing this.
            // The control has reached here to mean that we can to store 8
            // bytes of data into memory. But there are cases when src is
            // not a double register. That mean, we can't use (src) and
            // (src + 1) and write the values to memory, but instead we
            // have to use (constant 0) and (src).

            if (registers.doubleRegister(src)) {
              // MSBs <= src, LSBs <= (src + 1)
              appendInstruction(new StoreInstruction(Opcodes.STW, src, address, disp));
              appendInstruction(new StoreInstruction(Opcodes.STW, src + 1, address, disp.offset(4)));
            } else {
              // MSBs <= immediate 0, LSBs <= src
              if (annotateCode)
                appendInstruction(new CommentMarker("storeIntoMemoryX: 32 bit reg r" +
                                                    src +
                                                    " into 64 bit memory location"));
              int tmpReg = registers.newTempRegister(RegisterSet.INTREG);
              appendInstruction(new FDcInstruction(Opcodes.LI, tmpReg, 0));
              appendInstruction(new StoreInstruction(Opcodes.STW, tmpReg, address, disp));
              appendInstruction(new StoreInstruction(Opcodes.STW, src, address, disp.offset(4)));
              if (annotateCode)
                appendInstruction(new CommentMarker("storeIntoMemoryX: end"));
            }
            return;
          }
        }

        appendInstruction(new StoreInstruction(opcode, src, address, disp));
        if (p)
          appendInstruction(new StoreInstruction(opcode,
                                                 src + registers.numContiguousRegisters(src),
                                                 address, disp.offset(8)));
        return;
      }

      int srcs = registers.rangeBegin(src);
      if (registers.floatRegister(src)) {
        opcode = Opcodes.STFS;

        appendInstruction(new StoreInstruction(opcode, srcs + 0, address, disp));
        appendInstruction(new StoreInstruction(opcode, srcs + 1, address, disp.offset(4)));
        if (p) {
          appendInstruction(new StoreInstruction(opcode, srcs + 2, address, disp.offset(8)));
          appendInstruction(new StoreInstruction(opcode, srcs + 3, address, disp.offset(12)));
        }
        return;
      }

      appendInstruction(new StoreInstruction(Opcodes.STW, srcs + 0, address, disp));
      appendInstruction(new StoreInstruction(Opcodes.STW, srcs + 1, address, disp.offset(4)));
      if (p) {
        appendInstruction(new StoreInstruction(Opcodes.STW, srcs + 2, address, disp.offset(8)));
        appendInstruction(new StoreInstruction(Opcodes.STW, srcs + 3, address, disp.offset(12)));
      }
      return;
    case 16:
      if (registers.floatRegister(src)) {
        srcs = registers.rangeBegin(src);
        if (alignment == 16) {
          appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 0, address, disp));
          appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 2, address, disp.offset(8)));
          if (p) {
            appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 4, address, disp.offset(16)));
            appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 6, address, disp.offset(24)));
          }
          return;
        }

        appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 0, address, disp));
        appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 1, address, disp.offset(4)));
        appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 2, address, disp.offset(8)));
        appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 3, address, disp.offset(12)));
        if (p) {
          appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 4, address, disp.offset(16)));
          appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 5, address, disp.offset(20)));
          appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 6, address, disp.offset(24)));
          appendInstruction(new StoreInstruction(Opcodes.STFD, srcs + 7, address, disp.offset(28)));
        }
        return;
      }
      break;
    }
    throw new scale.common.InternalError("Unknown data type size (" + size + ")");
  }

  /**
   * Generate instructions to store data into memory at the address
   * specified by a register.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real - this argument is
   * not used in this architecture and will be ignored
   */
  protected void storeIntoMemory(int     src,
                                 int     address,
                                 int     size,
                                 long    alignment,
                                 boolean real)
  {
    storeIntoMemory(src, address, getDisp(0), FT_NONE, size, alignment, real);
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param disp specifies the offset from the address register value
   * @param dftn - RT_NONE, etc
   * @param bits specifies the size of the data in bits to be loaded - must
   * be 32 or less
   * @param bitOffset specifies the offset in bits to the data
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   */
  protected void storeBitsIntoMemory(int          src,
                                     int          address,
                                     Displacement disp,
                                     int          dftn,
                                     int          bits,
                                     int          bitOffset,
                                     int          alignment)
  {
    if (annotateCode)
      appendInstruction(new CommentMarker("storeBitsIntoMemory bits: " +
                                          bits +
                                          " bitOffset: " +
                                          bitOffset));

    int span = bits + bitOffset;

    if ((bits <= 0) || (64 < bits))
      throw new scale.common.InternalError("Invalid bits: " + bits);

    if ((bitOffset < 0) || (32 <= bitOffset))
      throw new scale.common.InternalError("Invalid bitOffset : " + bitOffset);

    if (span <= 32) {
      // If the field is of type long long, then src will be a
      // doubleRegister, in which case we are interested only the LSBs,
      // which is available in registers.lastRegister(src).

      int src2 = registers.lastRegister(src);
      // data --- the data that was already stored in the struct. We read
      // the data into a register, modify only those bits of this bit field
      // and write back the whole register.
      int data = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new LoadInstruction(Opcodes.LWZ, data, address, disp, dftn, macosx));
      appendInstruction(new FDrcccInstruction(Opcodes.RLWIMI, data, src2, 32 - span, bitOffset, span - 1));
      appendInstruction(new StoreInstruction(Opcodes.STW, data, address, disp, dftn, macosx));
    } else if (span <= 64) {
      // If span > 32, this has to be a field of type "long long", and src
      // has to be a doubleRegister.
      if (! registers.doubleRegister(src))
        throw new scale.common.InternalError("src not a long long");

      // We have to write into two words of storage. Writing to the 0th
      // word. We have to write x0 = (32 - bitOffset) bits into word 0. For
      // these x0 bits, we have 3 cases. These x0 bits can be
      // 
      // Case 1. Fully in the MSByte of src. This occurs when
      //          bits > 32, bits - x >= 32
      // Case 2. A portion in MSByte of src and the rest in LSByte of src.
      // This occurs when
      //          bits > 32, bits - x < 32
      // Case 3. Fully in the LSByte of src. This occurs when
      //          bits <= 32

      int x0 = (32 - bitOffset);
      int tempDataReg;
      if (bits > 32) {
        if ((bits - x0) >= 32) {
          // Case 1
          // We shift right the MSByte by (bits - x0 - 32) and then do a
          // storeBitsIntoMemory with this data

          int shiftRightAmount = (bits - x0 - 32);
          tempDataReg = registers.newTempRegister(RegisterSet.INTREG);
          if (0 != shiftRightAmount)
            appendInstruction(new FDrcInstruction(Opcodes.SRAWI,
                                                  tempDataReg,
                                                  src,
                                                  shiftRightAmount,
                                                  macosx));
          else
            appendInstruction(new FDrInstruction(Opcodes.MR, tempDataReg, src));
        } else {
          // Case 2
          // Assume that there are y bits in the MSByte of src and z bits
          // in the LSByte of src. We first shift the z bits to the right
          // and then y bits to the left. Also, notice that bits == (y + z)
          // y = bits - 32;
          // z = x0 - y;
          // shiftRightAmount (i.e. for z) = 32 - z;
          // shiftLeftAmount  (i.e. for y) = 32 - shiftRightAmount;
          int y = bits - 32;
          int z = x0 - y;
          if ((32 == z) || (0 == z))
            throw new scale.common.InternalError("Shift amounts 0");

          tempDataReg = registers.newTempRegister(RegisterSet.INTREG);
          appendInstruction(new FDrcInstruction(Opcodes.SRAWI, tempDataReg,
                                                src + 1,
                                                32 - z,
                                                macosx));
          appendInstruction(new FDrcccInstruction(Opcodes.RLWIMI,
                                                  tempDataReg,
                                                  src,
                                                  z,
                                                  32 - y - z,
                                                  31 - z));
        }
      } else {
        // Case 3
        // We shift right the LSByte by (bits - x0) and then do a
        // storeBitsIntoMemory with this data
        int shiftRightAmount = (bits - x0);
        tempDataReg = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new FDrcInstruction(Opcodes.SRAWI,
                                              tempDataReg,
                                              src + 1,
                                              shiftRightAmount,
                                              macosx));
      }

      // Writing to MSWord of the data location

      storeBitsIntoMemory(tempDataReg, address, disp, dftn, x0, bitOffset, alignment);

      // Writing to the 1st word of the data location. We have to write x1
      // = (bits - x0) bits to word 1. x1 <= 32. These x1 are the LSBits of
      // src. We cannot alter disp, because this might have to be adjusted
      // later in endRoutineCode()

      // FIXME. tmpAddrReg = address + 4; What if the size of a word
      // changes? This problem is all over the place. There is 32 used
      // everywhere.

      int x1         = (bits - x0);
      int tmpAddrReg = registers.newTempRegister(RegisterSet.ADRREG);
      appendInstruction(new FDrcInstruction(Opcodes.ADDI, tmpAddrReg, address, 4, macosx));
      storeBitsIntoMemory(src + 1, tmpAddrReg, disp, dftn, x1, 0, alignment);
    } else
      throw new scale.common.NotImplementedError("storeBitsIntoMemory");

    if (annotateCode)
      appendInstruction(new CommentMarker("end storeBitsIntoMemory"));
  }

  /**
   * Generate instructions to store data into memory at the address in a
   * register plus an offset.
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real - this argument is
   * not used in this architecture and will be ignored
   */
  protected  void storeIntoMemoryWithOffset(int     src,
                                            int     address,
                                            long    offset,
                                            int     size,
                                            long    alignment,
                                            boolean real)
  {
    storeIntoMemory(src,
                    address,
                    getDisp((int) offset),
                    FT_NONE,
                    size,
                    alignment,
                    real);
  }

  /**
   * Generate instructions to store data into memory at the address in a
   * register plus an offset.  The offset must not be symbolic.
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real - this argument is
   * not used in this architecture and will be ignored
   */
  protected  void storeIntoMemoryWithOffset(int          src,
                                            int          address,
                                            Displacement offset,
                                            int          size,
                                            long         alignment,
                                            boolean      real)
  {
    assert offset.isNumeric() : "Symbolic displacement " + offset;

      storeIntoMemory(src, address, offset, FT_NONE, size, alignment, real);
  }

  /**
   * Generate an instruction sequence to move words from one location to
   * another.
   * The destination offset must not be symbolic.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move
   * @param aln is the alignment that can be assumed for both the source
   * and destination addresses
   */
  protected void moveWords(int          src,
                           long         srcoff,
                           int          dest,
                           Displacement destoff,
                           int          size,
                           int          aln)
  {
    assert destoff.isNumeric() : "Symbolic displacement " + destoff;

      if (destoff.isStack()) {
        int destAddrReg = registers.getResultRegister(RegisterSet.ADRREG);
        appendInstruction(new FDdrInstruction(Opcodes.LA,
                                              destAddrReg,
                                              dest,
                                              destoff,
                                              FT_NONE,
                                              macosx));
        if (annotateCode)
          appendInstruction(new CommentMarker("moveWords within moveWords 1"));
        moveWords(src, srcoff, destAddrReg, 0, size, aln);
        return;
      }

      moveWords(src, srcoff, dest, destoff.getDisplacement(), size, aln);
      return;
  }

  /**
   * Generate an instruction sequence to move words from one location to
   * another.
   * @param src specifies the register containing the source address
   * @param dest specifies the register containing the destination address
   * @param size specifes the number of bytes to move.
   * @param aln is the alignment that can be assumed for both the source
   * and destination addresses
   */
  protected void moveWords(int  src,
                           long srcoff,
                           int  dest,
                           long destoff,
                           int  size,
                           int  aln)
  {
    int ms  = PPCG4RegisterSet.IREG_SIZE;
    int lop = Opcodes.LWZ;
    int sop = Opcodes.STW;
    boolean aln8 = false;

    if (((aln     & 0x7) == 0) &&
        ((size    & 0x7) == 0) &&
        ((srcoff  & 0x7) == 0) &&
        ((destoff & 0x7) == 0)) {
      ms = 8;
      lop = Opcodes.LFD;
      sop = Opcodes.STFD;
      aln8 = true;
    } else if (((aln     & 0x3) != 0) ||
               ((size    & 0x3) != 0) ||
               ((srcoff  & 0x3) != 0) ||
               ((destoff & 0x3) != 0)) {
      ms  = 2;
      lop = Opcodes.LHZ;
      sop = Opcodes.STH;
      if (((aln     & 0x1) != 0) ||
          ((size    & 0x1) != 0) ||
          ((srcoff  & 0x1) != 0) ||
          ((destoff & 0x1) != 0)) {
        ms  = 1;
        lop = Opcodes.LBZ;
        sop = Opcodes.STB;
      }
    }

    // srcoff (and destoff) can be non-zero.

    int src2 = (0 == srcoff) ? src : registers.newTempRegister(registers.getType(src));

    if (annotateCode)
      appendInstruction(new CommentMarker("moveWords: srcoff = " +
                                          srcoff +
                                          " destoff = " +
                                          destoff));

    if (0 != srcoff)
      appendInstruction(new FDrcInstruction(Opcodes.ADDI, src2, src, (int) srcoff, macosx));

    int dest2 = (0 == destoff) ? dest : registers.newTempRegister(registers.getType(dest));
    if (0 != destoff)
      appendInstruction(new FDrcInstruction(Opcodes.ADDI, dest2, dest, (int) destoff, macosx));

    if (size <= (ms * 5)) { // Generate straight-line load-stores to move data.
      int offset = 0;
      int nexr = 0;

      for (int k = 0; k < size; k += ms) { // Generate loads.
        Displacement odisp = getDisp(offset);
        if (aln8)
          nxtMvReg[nexr] = registers.newTempRegister(RegisterSet.FLTREG);
        else
          nxtMvReg[nexr] = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadInstruction(lop, nxtMvReg[nexr], src2, odisp));
        offset += ms;
        nexr++;
      }

      offset = 0;
      nexr = 0;

      for (int k = 0; k < size; k += ms) { // Generate stores.
        Displacement odisp = getDisp(offset);
        appendInstruction(new StoreInstruction(sop, nxtMvReg[nexr], dest2, odisp));
        offset += ms;
        nexr++;
      }

      return;
    }

    // Generate loop to move data.

    int valreg = registers.newTempRegister(RegisterSet.AIREG);
    if (ms == 8)
      valreg = registers.newTempRegister(RegisterSet.FLTREG);

    int               testreg = registers.newTempRegister(RegisterSet.AIREG);
    int               tsrc    = registers.newTempRegister(RegisterSet.AIREG);
    int               tdest   = registers.newTempRegister(RegisterSet.AIREG);
    Label             labl    = createLabel();
    Label             labn    = createLabel();
    Displacement      odisp   = getDisp(0);
    LabelDisplacement ldisp   = new LabelDisplacement(labl);
    Displacement      ddisp   = getDisp(ms);
    Branch            inst    = new BFormInstruction(Opcodes.BC,
                                                     Opcodes.BRANCH_TRUE,
                                                     Opcodes.GT,
                                                     ldisp,
                                                     2);

    inst.addTarget(labl, 0);
    inst.addTarget(labn, 1);

    genRegToReg(src2, tsrc);
    genRegToReg(dest, tdest);
    appendInstruction(new LoadInstruction(lop, valreg, tsrc, odisp));
    genLoadImmediate(size - ms, testreg);
    appendLabel(labl);
    appendInstruction(new FDdrInstruction(Opcodes.LA, tsrc, tsrc, ddisp, FT_NONE, macosx));
    appendInstruction(new StoreInstruction(sop, valreg, tdest, odisp));
    appendInstruction(new FDdrInstruction(Opcodes.LA, tdest, tdest, ddisp, FT_NONE, macosx));
    appendInstruction(new FDrcInstruction(Opcodes.ADDI, testreg, testreg, -ms, macosx));
    appendInstruction(new LoadInstruction(lop, valreg, tsrc, odisp));
    appendInstruction(new FrcInstruction(Opcodes.CMPWI, testreg, 0));
    appendInstruction(inst);
    appendLabel(labn);
    labn.setNotReferenced();
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
  protected void loadRegFromSymbolicLocation(int          dest,
                                             int          dsize,
                                             boolean      isSigned,
                                             boolean      isReal,
                                             Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;
      int adr = loadMemoryAddress(disp);

      // The alignment of real numbers is the same as their size. From SVR4
      // ABI page 3-4.

      int alignment = isReal ? dsize : 4;
      assert (!isReal || (4 == dsize) || (8 == dsize) || (16 == dsize))
        : "dsize is neither 4, 8 nor 16: " + dsize;

        loadFromMemory(dest, adr, getDisp(0), FT_NONE, dsize, alignment, isSigned);
  }

  /**
   * Generate instructions to do a binary operation on two values.
   * @param which specifies the binary operation (ADD, SUB, ...)
   * @param ct is the result type
   * @param la is the left argument
   * @param ra is the right argument
   * @param dest is the destination register
   */
  protected void doBinaryOp(int which, Type ct, Expr la, Expr ra, int dest)
  {
    int  size = ct.memorySizeAsInt(machine);

    needValue(la);
    int laReg = resultReg;

    needValue(ra);
    int raReg = resultReg;

    doBinaryOp(dest, laReg, raReg, size, ct.isRealType(), ct.isSigned(), which);
  }

  /**
   * Generate instructions to do a binary operation on two values.
   * @param dest     the destination (result) register
   * @param laReg    the register containing the left operand
   * @param raReg    the register containing the right operand
   * @param size     the size of the result
   * @param isReal   true if this is a floating point operation
   * @param isSigned true if this is a signed operation (only if !isReal)
   * @param which    specifies the binary operation (ADD, SUB, etc.)
   */
  private void doBinaryOp(int     dest,
                          int     laReg,
                          int     raReg,
                          int     size,
                          boolean isReal,
                          boolean isSigned,
                          int     which)
  {
    boolean shiftPperation = false; // see comment below inside if (callFunc)

    // For PPC, *INTEGER* subtraction is done using the SUBF for "Subtract
    // From" so subf r0, r1, r2 places r2 - r1 into r0 this is the opposite
    // of the divw in which case divw r0, r1, r2 places r1/r2 into r0.

    if ((SUB == which) && !isReal) {
      int tmp = laReg;
      laReg    = raReg;
      raReg    = tmp;
    }

    int     opcode   = 0;
    int     opcode2  = 0;
    boolean callFunc = false;
    String  funcName = null;

    if (isReal) {
      switch (which) {
      case ADD:
        opcode = (4 < size) ? Opcodes.FADD : Opcodes.FADDS;
        break;
      case SUB:
        opcode = (4 < size) ? Opcodes.FSUB : Opcodes.FSUBS;
        break;
      case MUL:
        opcode = (4 < size) ? Opcodes.FMUL : Opcodes.FMULS;
        break;
      case DIV:
        opcode = (4 < size) ? Opcodes.FDIV : Opcodes.FDIVS;
        break;
      default:
        throw new scale.common.InternalError("Invalid operation: " + which);
      }
    } else {
      switch (which) {
      case ADD:
        opcode  = (4 < size) ? Opcodes.ADDC : Opcodes.ADD;
        opcode2 = (4 < size) ? Opcodes.ADDE : 0;
        break;
      case SUB:
        opcode  = (4 < size) ? Opcodes.SUBFC : Opcodes.SUBF;
        opcode2 = (4 < size) ? Opcodes.SUBFE : 0;
        break;
      case MUL:
        if (4 < size) {
          callFunc = true;
          funcName = "__muldi3";
        } else {
          opcode = Opcodes.MULLW;
        }
        break;
      case DIV:
        if (4 < size) {
          callFunc = true;
          funcName = isSigned ? "__divdi3" : "__udivdi3";
        } else {
          opcode = isSigned ? Opcodes.DIVW : Opcodes.DIVWU;
        }
        break;
      case MOD:
        // Only 64 bit numbers are taken care of here. 32 bits should be
        // done in visitRemainderExpr()

        // FIXME: Not taken care of sign

        if (8 != size)
          throw new scale.common.InternalError("Only 64 bit MOD should come here");

        callFunc = true;
        funcName = isSigned ? "__moddi3" : "__umoddi3";
        break;
      case AND:
        opcode  = Opcodes.AND;
        opcode2 = (4 < size) ? Opcodes.AND : 0;
        break;
      case OR:
        opcode  = Opcodes.OR;
        opcode2 = (4 < size) ? Opcodes.OR : 0;
        break;
      case XOR:
        opcode  = Opcodes.XOR;
        opcode2 = (4 < size) ? Opcodes.XOR : 0;
        break;
      case SRA:
        shiftPperation = true;
        if (4 < size) {
          callFunc = true;
          funcName = "__ashrdi3";
        } else
          opcode = Opcodes.SRAW;

        break;
      case SRL:
        shiftPperation = true;
        if (4 < size) {
          callFunc = true;
          funcName = "__lshrdi3";
        } else
          opcode = Opcodes.SRW;

        break;
      case SLL:
        shiftPperation = true;
        if (4 < size) {
          callFunc = true;
          funcName = "__ashldi3";
        } else
          opcode = Opcodes.SLW;

        break;
      default:
        throw new scale.common.InternalError("Invalid operation: " + which);
      }
    }

    if ((4 >= size) || isReal)
      appendInstruction(new FDrrInstruction(opcode, dest, laReg, raReg));
    else {
      if (callFunc) {
        if (!registers.doubleRegister(laReg))
          throw new scale.common.InternalError("LHS is not a doubleRegister");

        // We are dealing with long long operands and need to call
        // functions to do these tasks, since the machine does not support
        // these operations natively. We put the LHS operand into (r3, r4),
        // the RHS operand into (r5, r6) and call the function.

        // A special case needs to be considered for shift operations.
        // These functions take an ``int'' as the second parameter, so the
        // LHS operand should go into (r3, r4) and the integer (or the 32
        // LSBs of the long long) into r5.

        // Move for the first operand of the function

        genRegToReg(laReg, PPCRegisterSet.FIA_REG);

        // Move for the second operand of the function

        if (shiftPperation) {
          boolean rhsIsDouble = registers.doubleRegister(raReg);
          int     tmpReg      = registers.newTempRegister(RegisterSet.INTREG);
          convertIntRegValue(/* src        */ raReg,
                             /* srcSize    */ rhsIsDouble ? 8 : 4,
                             /* srcSigned  */ isSigned,
                             /* dest       */ tmpReg,
                             /* destSize   */ 4,
                             /* destSigned */ isSigned);
          genRegToReg(tmpReg, PPCRegisterSet.FIA_REG + 2);
        } else {
          if (!registers.doubleRegister(raReg))
            throw new scale.common.InternalError("raReg is not a doublereg");

          genRegToReg(raReg, PPCRegisterSet.FIA_REG + 2);
        }
        short[] uses = new short[shiftPperation ? 4 : 5];
        uses[0] = (short) PPCRegisterSet.SP_REG;
        uses[1] = (short) PPCRegisterSet.FIA_REG;
        uses[2] = (short) PPCRegisterSet.FIA_REG + 1;
        uses[3] = (short) PPCRegisterSet.FIA_REG + 2;
        if (!shiftPperation)
          uses[4] = (short) PPCRegisterSet.FIA_REG + 3;

        // genFtnCall makes the call to the function, after which we get back
        // the long long result from (r3, r4)

        genFtnCall(funcName, uses, null);
        genRegToReg(PPCRegisterSet.IR_REG, dest);
      } else {
        // We have to write to the LSBs of the double register and then to
        // the MSBs. The register allocator does not understand this. So we
        // employ a temporary register and move this to the LSBs

        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new FDrrInstruction(opcode, tr, laReg + 1, raReg + 1));
        appendInstruction(new FDrrInstruction(opcode2, dest, laReg, raReg));
        genRegToReg(tr, dest + 1);
      }
    }

    resultReg = dest;
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
  }

  private void doComplexOp(int which, int bs, int laReg, int raReg, int dest)
  {
    throw new scale.common.NotImplementedError("doComplexOp");
  }

  /**
   * Create a call (BL) to the routine with the specified name.
   * This is used to call things like the divide subroutine.
   */
  protected Branch genFtnCall(String name, short[] uses, short[] defs)
  {
    int          handle = allocateTextArea(macosx ? ("L_" + name + "$stub") : name, TEXT);
    Displacement disp   = new SymbolDisplacement(macosx ? ("L_" + name + "$stub") : name, handle);
    Label        lab    = createLabel();
    Branch       bl     = new IFormInstruction(Opcodes.BL, disp, 1);

    associateDispWithArea(handle, disp);

    bl.additionalRegsUsed(uses);
    bl.additionalRegsKilled(registers.getCalleeUses());
    bl.addTarget(lab, 0);
    bl.markAsCall();

    lastInstruction.specifySpillStorePoint();
    appendInstruction(bl);
    appendLabel(lab);
    lab.markAsFirstInBasicBlock();
    lab.setNotReferenced();
    callsRoutine = true;
    calledFuncs.add(name);

    return bl;
  }

  private Branch genTestBranch(CompareMode which, Label tlab, Label flab)
  {
    LabelDisplacement tdisp = new LabelDisplacement(tlab);
    Branch            brInst;

    switch (which) {
    case EQ:
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_TRUE, Opcodes.EQ, tdisp, 2);
      break;
    case LE:
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_FALSE, Opcodes.GT, tdisp, 2);
      break;
    case LT:
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_TRUE, Opcodes.LT, tdisp, 2);
      break;
    case GT:
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_TRUE, Opcodes.GT, tdisp, 2);
      break;
    case GE:
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_FALSE, Opcodes.LT, tdisp, 2);
      break;
    case NE:
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_FALSE, Opcodes.EQ, tdisp, 2);
      break;
    default:
      throw new scale.common.InternalError("Invalid which " + which);
    }

    brInst.addTarget(tlab, 0);
    brInst.addTarget(flab, 1);

    return brInst;
  }

  /**
   * Generate instructions to do a comparison of two values.
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

    Type    lt   = processType(la);
    int     treg = registers.newTempRegister(RegisterSet.INTREG);
    boolean si   = lt.isSigned();

    needValue(la);
    int laReg = resultReg;
    int opcode;

    // Special case to deal with constants removed (existed in version
    // 1.86). The special cases need to be implemented when rhs is an
    // AddressLiteral.

    // If not taken care of here, it should be easy to handle this in the
    // peephole optimizeer

    needValue(ra);
    int raReg = resultReg;

    Type rt = processType(ra);

    if (!(lt.isRealType() || rt.isRealType())) {
      opcode = si ? Opcodes.CMPW : Opcodes.CMPLW;
      appendInstruction(new FrrInstruction(opcode, laReg, raReg));

      if (registers.doubleRegister(raReg) &&
          registers.doubleRegister(laReg)) {
        Label tlab = createLabel();
        Label flab = createLabel();
        LabelDisplacement tdisp = new LabelDisplacement(tlab);

        Branch brInst = new BFormInstruction(Opcodes.BC,
                                             Opcodes.BRANCH_FALSE,
                                             Opcodes.EQ,
                                             tdisp,
                                             2);
        brInst.addTarget(tlab, 0);
        brInst.addTarget(flab, 1);
        appendInstruction(brInst);
        appendInstruction(flab);
        flab.setNotReferenced();

        appendInstruction(new FrrInstruction(Opcodes.CMPLW, laReg + 1,raReg + 1));
        appendLabel(tlab);
      }
    } else
      appendInstruction(new FcrrInstruction(Opcodes.FCMPO, 0, laReg, raReg));

    Label tlab    = createLabel();
    Label flab    = createLabel();
    Branch brInst = genTestBranch(which, tlab, flab);

    appendInstruction(new FDcInstruction(Opcodes.LI, ir, 1));
    appendInstruction(brInst);
    appendLabel(flab);
    flab.setNotReferenced();
    appendInstruction(new FDcInstruction(Opcodes.LI, ir, 0));
    appendLabel(tlab);

    resultReg = ir;
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    return;
  }

  /**
   * Called at the start of code generation for a routine.
   */
  protected Instruction startRoutineCode()
  {
    if (macosx)
      adrDispReg = registers.newTempRegister(RegisterSet.ADRREG);

    currentBeginMarker  = new BeginMarker(scribble, macosx);

    return currentBeginMarker;
  }

  protected void processSourceLine(int line, Label lab, boolean newLine)
  {
    if (line < 0)
      return;

    String fileName = currentRoutine.getCallGraph().getName();
    appendInstruction(new PPCLineMarker(fileName, line));
  }

  /**
   * Generate an unconditional branch to the label specified.
   */
  public void generateUnconditionalBranch(Label lab)
  {
    LabelDisplacement disp = new LabelDisplacement(lab);
    Branch            inst = new IFormInstruction(Opcodes.B, disp, 1);

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
    if (shouldBeRegenerated(reg))
      return null;

    StackDisplacement disp = new StackDisplacement(localVarSize);
    localVarSize += (registers.floatRegister(reg) ?
                     PPCG4RegisterSet.FREG_SIZE :
                     PPCG4RegisterSet.IREG_SIZE);

    localVar.addElement(disp);
    return disp;
  }

  /**
   * Insert the instruction(s) to restore a spilled register.
   * At this point, we are using a one-to-one mapping between real registers
   * and virtual registers, so only a single instruction is required.
   * @param reg specifies which virtual register will be loaded
   * @param spillLocation specifies the offset on the stack to the spill
   * location
   * @param after specifies the instruction to insert the load after
   * @return the last instruction inserted
   * @see #getSpillLocation
   */
  public Instruction insertSpillLoad(int reg, Object spillLocation, Instruction after)
  {
    if (spillLocation == null)
      return regenerateRegister(reg, after);

    boolean flt = registers.floatRegister(reg);
    int     opl = flt ? Opcodes.LFD : Opcodes.LWZ;

    if (registers.registerSize(reg) > 4)
      opl = flt ? Opcodes.LFD : Opcodes.LD;

    StackDisplacement disp   = (StackDisplacement) spillLocation;
    long              offset = disp.getDisplacement();
    Instruction       sl     = lastInstruction;
    Instruction       next   = after.getNext();
    Instruction       load;

    lastInstruction = after;
    after.setNext(null);

    load = new LoadInstruction(opl, reg, PPCRegisterSet.SP_REG, (Displacement) spillLocation);

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
   * @param spillLocation specifies the offset on the stack to the spill
   * location
   * @param after specifies the instruction to insert the store after
   * @return the last instruction inserted
   * @see #getSpillLocation
   */
  public Instruction insertSpillStore(int reg, Object spillLocation, Instruction after)
  {
    if (spillLocation == null)
      return null;

    boolean flt = registers.floatRegister(reg);
    int     ops = flt ? Opcodes.STFD : Opcodes.STW;
    if (registers.registerSize(reg) > 4)
      ops = flt ? Opcodes.STFD : Opcodes.STD;

    StackDisplacement disp   = (StackDisplacement) spillLocation;
    long              offset = disp.getDisplacement();
    Instruction       sl     = lastInstruction;
    Instruction       next   = after.getNext();
    Instruction       store;

    lastInstruction = after;
    after.setNext(null);

    store = new StoreInstruction(ops, reg, PPCRegisterSet.SP_REG, (Displacement) spillLocation);

    appendInstruction(store);

    store.setNext(next);
    lastInstruction = sl;

    return store;
  }

  /**
   * Called at the end of code generation for a routine.
   * On the PPC we need to pass some critical information to the assembler.
   */
  protected void endRoutineCode(int[] regMap)
  {
    Displacement disp     = null;
    Instruction  first    = currentBeginMarker;
    Instruction  last     = returnInst;
    int          nrr      = registers.numRealRegisters();
    boolean[]    newmap   = new boolean[nrr];
    int          n        = regMap.length;

    if ((last == null) && Debug.debug(1))
      System.out.println("** Warning: " + currentRoutine.getName() + "() does not return.");

    for (int i = nrr; i < n; i++) {
      int vr = regMap[i];
      if (vr < nrr) /* In case of debugging. */
        newmap[vr] = true;
    }

    // Determine size of fp reg save.
    // Determine size of ireg save.
    // Determine which registers need saving.

    int     fpSize      = 0;
    int     gpSize      = 0;
    short[] calleeSaves = registers.getCalleeSaves();
    for (int i = 0; i < calleeSaves.length; i++) {
      int reg = calleeSaves[i];

      if (!newmap[reg])
        continue;

      gpSize += !registers.floatRegister(reg) ? PPCG4RegisterSet.IREG_SIZE : PPCG4RegisterSet.FREG_SIZE;
    }

    // Determine size of parameter save area.

    int argSaveSize = maxOverflowSize;
    if (macosx)
      argSaveSize = PPCG4RegisterSet.IREG_SIZE *8 + maxOverflowSize;

    int linkSize = PPCG4RegisterSet.IREG_SIZE * 2;
    if (macosx)
      linkSize = PPCG4RegisterSet.IREG_SIZE * 6;

    // fp and gp save areas are double word aligned.

    fpSize = (int) Machine.alignTo(fpSize, 8);
    gpSize = (int) Machine.alignTo(gpSize, 8);

    int frameSize = linkSize + argSaveSize + localVarSize + gpSize + fpSize;
    int padding   = (int) Machine.alignTo(frameSize, 16) - frameSize;
    frameSize += padding;

    Instruction st = first;
    if (annotateCode) {
      st = insertInstruction(new CommentMarker("endRoutineCode: frameSize = " + frameSize), st);
      st = insertInstruction(new CommentMarker("linkSize = " + linkSize), st);
      st = insertInstruction(new CommentMarker("argSaveSize = " + argSaveSize), st);
      st = insertInstruction(new CommentMarker("localVarSize = " + localVarSize), st);
      st = insertInstruction(new CommentMarker("gpSize = " + gpSize), st);
      st = insertInstruction(new CommentMarker("fpSize = " + fpSize), st);
      if (callsRoutine)
        st = insertInstruction(new CommentMarker("Saving Link register"), st);
    }

    if (callsRoutine) { // save link register
      st = insertInstruction(new FDcInstruction(Opcodes.MFSPR, 0, Opcodes.LR_SPR), st);
      disp = getDisp(macosx ? 8 : 4);
      st = insertInstruction(new StoreInstruction(Opcodes.STW, 0, PPCRegisterSet.SP_REG, disp), st);
    }

    // Save the stack pointer.
    // We have to check whether frameSize uses more than 16 bits here. This
    // function (endRoutineCode) is called after adjustImmediates(), and so
    // we have to adjust huge offsets, ourselves.

    if (annotateCode)
      st = insertInstruction(new CommentMarker("saving stack, frameSize = " + frameSize), st);

    if ((MIN_IMM16 <= frameSize) && (frameSize <= MAX_IMM16)) {
      disp = getDisp(-frameSize);
      st = insertInstruction(new StoreInstruction(Opcodes.STWU,
                                                  PPCRegisterSet.SP_REG,
                                                  PPCRegisterSet.SP_REG,
                                                  disp),
                             st);
    } else {
      st = insertInstruction(new FDcInstruction(Opcodes.LIS,
                                                0,
                                                ((-frameSize) >> 16)),
                             st);
      st = insertInstruction(new FDrcInstruction(Opcodes.ORI,
                                                 0,
                                                 0,
                                                 ((-frameSize) & 0xffff),
                                                 macosx),
                             st);
      st = insertInstruction(new StorexInstruction(Opcodes.STWUX,
                                                   PPCRegisterSet.SP_REG,
                                                   PPCRegisterSet.SP_REG,
                                                   0),
                             st);
    }

    // Restore stack pointer.

    disp = new StackDisplacement(0);
    Instruction li = last;
    if (last != null)
      last = insertInstruction(new LoadInstruction(Opcodes.LWZ,
                                                   PPCRegisterSet.SP_REG,
                                                   PPCRegisterSet.SP_REG,
                                                   disp),
                               last);

    // Save/restore condition register.  We don't need to do this as
    // long as we aren't allocating condition register fields.

    // Save/restore floating point and general purpose registers.

    int gpRegNum = 0;
    int fpRegNum = 0;
    for (int i = (calleeSaves.length - 1); i >= 0; i--) {
      // Determine which registers need saving.  start at the last register
      // and work backwards because registers are arranged from r31 down.
      int reg = calleeSaves[i];

      if (!newmap[reg])
        continue;

      if (!registers.floatRegister(reg)) {
        gpRegNum++;
        int offset = frameSize - fpSize - gpRegNum*PPCG4RegisterSet.IREG_SIZE;
        if (offset < MAX_IMM16 && offset > MIN_IMM16) {
          disp = getDisp(offset);
          if (annotateCode)
            st = insertInstruction(new CommentMarker("Storing r" + reg), st);
          st = insertInstruction(new StoreInstruction(Opcodes.STW, reg, PPCRegisterSet.SP_REG, disp), st);
          if (li != null) {
            if (annotateCode)
              li = insertInstruction(new CommentMarker("Loading back r" + reg), li);
            li = insertInstruction(new LoadInstruction(Opcodes.LWZ, reg, PPCRegisterSet.SP_REG, disp), li);
          }
        } else {
          st = insertInstruction(new FDcInstruction(Opcodes.LIS, 0, (offset >> 16)), st);
          st = insertInstruction(new FDrcInstruction(Opcodes.ORI, 0, 0, (offset & 0xffff), macosx), st);
          st = insertInstruction(new StorexInstruction(Opcodes.STWX, reg, PPCRegisterSet.SP_REG, 0), st);
          if (li != null) {
            li = insertInstruction(new FDcInstruction(Opcodes.LIS, 0, (offset >> 16)), li);
            li = insertInstruction(new FDrcInstruction(Opcodes.ORI, 0, 0, (offset & 0xffff), macosx), li);
            li = insertInstruction(new LoadxInstruction(Opcodes.LWZX, reg, PPCRegisterSet.SP_REG, 0), li);
          }
        }
        setRegisterSaved(reg);
      } else {
        fpRegNum++;
        int offset = frameSize - fpRegNum*PPCG4RegisterSet.FREG_SIZE;
        if (offset < MAX_IMM16 && offset > MIN_IMM16) {
          disp = getDisp(offset);
          st = insertInstruction(new StoreInstruction(Opcodes.STFD, reg, PPCRegisterSet.SP_REG, disp), st);
          if (li != null)
            li = insertInstruction(new LoadInstruction(Opcodes.LFD, reg, PPCRegisterSet.SP_REG, disp), li);
        } else {
          st = insertInstruction(new FDcInstruction(Opcodes.LIS, 0, (offset >> 16)), st);
          st = insertInstruction(new FDrcInstruction(Opcodes.ORI, 0, 0, (offset & 0xffff), macosx), st);
          st = insertInstruction(new StorexInstruction(Opcodes.STFDX, reg, PPCRegisterSet.SP_REG, 0), st);
          if (li != null) {
            li = insertInstruction(new FDcInstruction(Opcodes.LIS, 0, (offset >> 16)), li);
            li = insertInstruction(new FDrcInstruction(Opcodes.ORI, 0, 0, (offset & 0xffff), macosx), li);
            li = insertInstruction(new LoadxInstruction(Opcodes.LFDX, reg, PPCRegisterSet.SP_REG, 0), li);
          }
        }
        setRegisterSaved(reg);
      }
    }

    if (macosx && usesAddressDisp) {
      // Save the link register if it's not already saved.
      if (!callsRoutine)
        st = insertInstruction(new FDcInstruction(Opcodes.MFSPR, 0, Opcodes.LR_SPR), st);

      Displacement osxDisp = new LabelDisplacement(osxLabel);
      Branch       brInst  = new IFormInstruction(Opcodes.BL, osxDisp, 1, true, true);

      brInst.addTarget(osxLabel, 0);
      st = insertInstruction(brInst, st);
      st = insertLabel(osxLabel, st);
      st = insertInstruction(new FDcInstruction(Opcodes.MFSPR, regMap[adrDispReg], Opcodes.LR_SPR), st);
      if (!callsRoutine)
        st = insertInstruction(new FcrInstruction(Opcodes.MTSPR, Opcodes.LR_SPR, 0), st);
    }

    // Restore link register.

    if (callsRoutine) {
      disp = getDisp(PPCG4RegisterSet.IREG_SIZE);
      if (macosx)
        disp = getDisp(PPCG4RegisterSet.IREG_SIZE*2);

      if (last != null) {
        last = insertInstruction(new LoadInstruction(Opcodes.LWZ, 0, PPCRegisterSet.SP_REG, disp), last);
        last = insertInstruction(new FcrInstruction(Opcodes.MTSPR, Opcodes.LR_SPR, 0), last);
      }
    }


    appendInstruction(new EndMarker(currentRoutine, macosx));

    // Adjust stack offsets now that we know how big everything is.

    int     lsv       = localVar.size();
    int     lsvOffset = linkSize + argSaveSize;
    boolean largeDisp = false;
    for (int i = 0; i < lsv; i++) {
      disp = localVar.elementAt(i);
      disp.adjust(lsvOffset);
      long d = disp.getDisplacement();
      if ((d < MIN_IMM16) || (d > MAX_IMM16))
        largeDisp = true;
    }
    
    int psv = paramVar.size();
    for (int i = 0; i < psv; i++) {
      disp = paramVar.elementAt(i);
      disp.adjust(frameSize);
      long d = disp.getDisplacement();
      if ((d < MIN_IMM16) || (d > MAX_IMM16))
        largeDisp = true;
    }

    ProcedureType pt = (ProcedureType) processType(currentRoutine);
    int           l  = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd  = pt.getFormal(i);
      if (!formalAsLocal[i]) {
        Assigned loc = fd.getStorageLoc();
        if (loc == Assigned.ON_STACK) {
          disp = fd.getDisplacement();
          disp.adjust(frameSize);
          fd.setDisplacement(disp);
          long d = disp.getDisplacement();
          if ((d < MIN_IMM16) || (d > MAX_IMM16))
            largeDisp = true;
        }
      }
    }

    // Adjust overflowArgArea. overflowArgArea is in the caller's stack
    // along with the parameters.

    if (null != overflowArgArea)
      overflowArgArea.adjust(frameSize);

    if (argDisp != null) {
      for (int i = 0; i < argDisp.size(); i++) {
        disp = argDisp.elementAt(i);
        disp.adjust(frameSize);
        long d = disp.getDisplacement();
        if ((d < MIN_IMM16) || (d > MAX_IMM16))
          largeDisp = true;
      }
    }

    if (largeDisp) {
      // We have some large stack offsets that need to be adjusted.
      // throw new scale.common.NotImplementedError
      //   ("endRoutineCode - large stack displacements");
    }

    if (trace) {
      System.out.print("LVS: ");
      System.out.print(currentRoutine.getName());
      System.out.print(" lvs ");
      System.out.print(localVarSize);
    }
  }

  /**
   * Specify that this routine saved a register on the stack.
   */
  private void setRegisterSaved(int reg)
  {
    if (registers.isFloatType(registers.getType(reg)))
      fmask |= 1 << reg;
    else
      mask |= 1 << reg;
  }

  /**
   * Handle abs() expression.
   * TODO: Does a call to fabs(), abs(), etc. Should be rewritten inline.
   * Should take care of constant operands.
   */
  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    // We are going to call the corresponding abs, fabs function.

    Type    ct      = processType(e);
    boolean isReal = ct.isRealType();
    boolean isInt  = ct.isIntegerType();
    int     bs      = ct.memorySizeAsInt(machine);

    // Creating the destination register based on the type of the argument

    int destReg = registers.getResultRegister(ct.getTag());

    // If this is a floating point number, we will call fabs, else we
    // will emit code for the computation.

    if (isReal) { // Setting up the paramenters and calling the function
      short[] uses = callArgs(e.getOperandArray(), false);

      // The function name is based on the type. See manual pages
      // fabbs(3), abs(3).

      String funcname = null;
      if (4 == bs) {
        funcname = "fabsf";
      } else if (8 == bs) {
        funcname = "fabs";
      } else if (16 == bs) {
        // FIXME: Unknown yet, how to get back a long double result from
        // a function.
        funcname = "fabsl";
        throw new scale.common.InternalError("long double not handled");
      } else
        throw new scale.common.InternalError("Invalid real number size");

      genFtnCall(funcname, uses, null);
      genRegToReg(PPCRegisterSet.FR_REG, destReg);
    } else if (isInt) {
      needValue(e.getArg());
      int srcReg = resultReg;

      // This is the code that will be generated.
      // srawi Rx, Rsrc, 31  ; Now, Rx is either all 1's or all 0's
      //                     ; based on sign of Rsrc
      // xor   Ry, Rx, Rsrc
      // subf  Rdest, Rx, Ry ; Rdest = Ry - Rx

      // Similar code is generated for long long.

      // Getting the sign.

      int signReg = registers.getResultRegister(ct.getTag());
      appendInstruction(new FDrcInstruction(Opcodes.SRAWI, signReg, srcReg, 31, macosx));

      if (4 == bs)
        ; // nothing
      else if (8 == bs)
        appendInstruction(new FDrcInstruction(Opcodes.SRAWI, signReg + 1, srcReg, 31, macosx));
      else
        throw new scale.common.InternalError("Invalid integer size");

      // Doing the XOR

      int xorReg  = registers.getResultRegister(ct.getTag());
      doBinaryOp(/* dest     */ xorReg,
                 /* laReg    */ signReg,
                 /* raReg    */ srcReg,
                 /* size     */ bs,
                 /* isReal   */ false,
                 /* isSigned */ false,
                 /* which    */ XOR);

      // Doing the subtraction.

      doBinaryOp(/* dest     */ destReg,
                 /* laReg    */ xorReg,
                 /* raReg    */ signReg,
                 /* size     */ bs,
                 /* isReal   */ false,
                 /* isSigned */ false,
                 /* which    */ SUB);
    } else
      throw new scale.common.InternalError("Invalid type");

    resultRegAddressOffset = 0;
    resultRegMode          = ResultMode.NORMAL_VALUE;
    resultReg              = destReg;
  }

  /**
   * This method is responsible for generating instructions to move
   * function arguments to the position assigned by the {@link
   * scale.backend.Generator#layoutParameters layoutParameters}
   * method.
   */
  public void generateProlog(ProcedureType pt)
  {
    if (macosx)
      macosxGenerateProlog(pt);
    else
      linuxGenerateProlog(pt);
  }

  private void macosxGenerateProlog(ProcedureType pt)
  {
    int  fr      = 0; // next floating point register
    int  gr      = 0; // next general purpose register
    int  offset  = 0;
    Type rt      = processType(pt.getReturnType());

    int fargRegs = MAX_ARG_REGS;
    fargRegs = MAX_FARG_REGS_MACOSX;

    // Lay out the arguments in memory.

    if (!(rt.isAtomicType() || rt.isVoidType())) {
      structAddress = registers.newTempRegister(RegisterSet.AIREG);
      structSize = rt.memorySizeAsInt(machine);
      genRegToReg(PPCRegisterSet.FIA_REG + gr, structAddress);
      offset += PPCG4RegisterSet.IREG_SIZE;
      gr++;
    }

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      Type       vt = processType(fd);

      if (fd instanceof UnknownFormals) {
        StackDisplacement disp  = (StackDisplacement)fd.getDisplacement(); 
        long              ldisp = disp.getDisplacement();
        while (gr < MAX_ARG_REGS) {
          StackDisplacement ufDisp = new StackDisplacement(ldisp);
          appendInstruction(new StoreInstruction(Opcodes.STW,
                                                 PPCRegisterSet.FIA_REG + gr,
                                                 PPCRegisterSet.SP_REG,
                                                 ufDisp));
          gr++;
          ldisp += PPCG4RegisterSet.IREG_SIZE;
          paramVar.addElement(ufDisp);
        }
        return;
      }

      int      ts           = vt.memorySizeAsInt(machine);
      boolean  isFlt        = (vt.isRealType() && ts <= 8);
      boolean  isSimpleType = (vt.isPointerType() || (vt.isAtomicType() && ts <= 8));
      Assigned loc          = fd.getStorageLoc();

      if (loc == Assigned.IN_REGISTER) {
        if (vt.isAggregateType()) {
          int        reg  = fd.getValueRegister();
          ResultMode mode = fd.valueRegMode();

          if (mode == ResultMode.STRUCT_VALUE) {
            int src = PPCRegisterSet.FIA_REG;
            while (ts > 0) {
              genRegToReg(src + gr++, reg++);
              ts -= PPCG4RegisterSet.IREG_SIZE;
            }
            continue;
          }
        } else {
          int reg = fd.getValueRegister();
          if (isFlt) {
            genRegToReg(PPCRegisterSet.FFA_REG + fr, reg);
            fr++;
            // Mac OS X wastes integer registers if floating point
            // arguments are sent.
            gr++;
            offset += PPCG4RegisterSet.IREG_SIZE;
            if (ts > 4) {
              offset += PPCG4RegisterSet.IREG_SIZE;
              gr++;
            }
          } else {
            int treg = convertIntRegValue(PPCRegisterSet.FIA_REG + gr, ts, true, reg, ts, vt.isSigned());
            genRegToReg(treg, reg);
            offset += ts;
            gr++;
            if (ts > 4)
              gr++;
          }
          continue;
        }
      }

      if (loc == Assigned.ON_STACK) {
        Displacement disp = fd.getDisplacement();

        if (vt.isAtomicType()) {
          if (gr < MAX_ARG_REGS && !isFlt || fr <= fargRegs && isFlt) {
            disp = fd.getDisplacement();
            if (isFlt) {
              storeIntoMemory(PPCRegisterSet.FFA_REG + fr,
                              PPCRegisterSet.SP_REG,
                              disp,
                              FT_NONE,
                              ts,
                              ts,
                              true);
              fr++;
              // Mac OS X wastes integer registers if floating point
              // arguments are sent.
              gr++;
              offset += PPCG4RegisterSet.IREG_SIZE;
              if (ts > 4) {
                offset += PPCG4RegisterSet.IREG_SIZE;
                gr++;
              }
            } else {
              storeIntoMemory(PPCRegisterSet.FIA_REG + gr,
                              PPCRegisterSet.SP_REG,
                              disp,
                              FT_NONE,
                              ts,
                              ts,
                              false);
              offset += ts;
              gr++;
              if (ts > 4)
                gr++;
            }
          }
          continue;
        } else if (vt.isAggregateType()) {
          int dsize  = vt.memorySizeAsInt(machine);
          int soffset = 0;

          for (int j = 0; j < dsize; j += PPCG4RegisterSet.IREG_SIZE) {
            if (gr < MAX_ARG_REGS) {
              Displacement disp2 = disp.offset(soffset);
              Instruction  inst  = new StoreInstruction(Opcodes.STW,
                                                        PPCG4RegisterSet.FIA_REG + gr,
                                                        PPCG4RegisterSet.SP_REG,
                                                        disp2);
              appendInstruction(inst);
              offset += PPCG4RegisterSet.IREG_SIZE;
              soffset += PPCG4RegisterSet.IREG_SIZE;
              gr++;
            }
          }
          continue;
        }

        throw new scale.common.InternalError("Argument is where " + fd);
      }
    }

    lastInstruction.specifySpillStorePoint();
    lastLabel = createLabel();
    appendLabel(lastLabel);
    lastLabel.setNotReferenced();
  }

  /**
   * This method is responsible for generating instructions to move
   * function arguments to the position assigned by the
   * layoutParameters method.
   */
  private void linuxGenerateProlog(ProcedureType pt)
  {
    int  nextArgF = 0;
    int  nextArgG = 0;
    int  offset   = 0;
    Type rt       = processType(pt.getReturnType());

    // Taking care of the return value. If it is a struct, we have to
    // store the value, in a buffer created by the caller.

    if (!(rt.isAtomicType() || rt.isVoidType())) {
      structAddress = registers.newTempRegister(RegisterSet.AIREG);
      structSize = rt.memorySizeAsInt(machine);
      genRegToReg(PPCRegisterSet.FIA_REG + nextArgG, structAddress);
      offset += PPCG4RegisterSet.IREG_SIZE;
      nextArgG++;
    }

    // Take care of each formal parameter.

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      Type       vt = processType(fd);

      // If this is ellipsis (...), I dont know what to do yet.

      if (fd instanceof UnknownFormals) {
        // vaArgStart points to the address starting at which registers
        // r3:r10 (and f1:f8 if necessary) are stored. When
        // va_start(list, param) is called, list.regSaveArea will be set
        // to the displacement vaArgStart.

        // regSaveArea needs an alignment of 8 bytes, because following
        // the integer save area is the floating point save area, which
        // needs this alignment.

        // see @setVaListType

        localVarSize = (int) Machine.alignTo(localVarSize, PPCG4RegisterSet.FREG_SIZE);
        vaArgStart = new StackDisplacement(localVarSize);
        localVar.addElement(vaArgStart);

        // Saving the integer registers into stack (in the local
        // variables area)

        for (int reg = PPCRegisterSet.FIA_REG; reg <= PPCRegisterSet.LIA_REG; reg++) {
          StackDisplacement disp = new StackDisplacement(localVarSize);
          localVar.addElement(disp);
          localVarSize += PPCG4RegisterSet.IREG_SIZE;
          appendInstruction(new StoreInstruction(Opcodes.STW, reg, PPCRegisterSet.SP_REG, disp));
        }

        // Save the floating point registers into stack (only if our caller
        // passed floating point values) based on the Condition register 6.

        // The code below checks this condition and branches to.

        Label             floatsNotPassed = createLabel();
        Label             floatsPassed    = createLabel();
        LabelDisplacement labelDisp       = new LabelDisplacement(floatsNotPassed);
        Branch            brInst          = new BFormInstruction(Opcodes.BC,
                                                                 Opcodes.BRANCH_FALSE,
                                                                 Opcodes.FPIOE,
                                                                 labelDisp,
                                                                 2);

        brInst.addTarget(floatsNotPassed, 0);
        brInst.addTarget(floatsPassed, 1);
        appendInstruction(brInst);
        appendLabel(floatsPassed);
        floatsPassed.setNotReferenced();

        // Saving the floating point registers. FIXME: Don't know about
        // alignment here.

        for (int reg = PPCRegisterSet.FFA_REG; reg <= PPCRegisterSet.LFA_REG_LINUX; reg++) {
          StackDisplacement disp = new StackDisplacement(localVarSize);
          localVar.addElement(disp);
          localVarSize += PPCG4RegisterSet.FREG_SIZE;
          appendInstruction(new StoreInstruction(Opcodes.STFD, reg, PPCRegisterSet.SP_REG, disp));
        }

        appendLabel(floatsNotPassed);
        break;
      }

      // If control comes here, we are dealing with a formal parameter
      // that is not ellipsis.

      int     ts           = vt.memorySizeAsInt(machine);
      boolean isReal       = vt.isAtomicType() && vt.isRealType();
      boolean isLongLong   = (vt.isAtomicType() &&
                              !isReal          &&
                              (64 == ((AtomicType) vt).bitSize()));
      boolean  isSimpleType = (vt.isPointerType() || (vt.isAtomicType() && ts <= 8));
      Assigned loc          = fd.getStorageLoc();

      // FIXME: Dont know why this check is necessary. Katie had
      //   isFlt = (vt.isRealType() && ts <= 8);
      // and I don't know when a real would have a size greater than 8. So,
      // ensuring that this assumption (that a real has size <= 8) is true.

      if(isReal && (ts > 8))
        throw new scale.common.InternalError("real number and size greater than 8");

      if (loc == Assigned.IN_REGISTER) {
        if (vt.isAggregateType()) {

          int        reg  = fd.getValueRegister();
          ResultMode mode = fd.valueRegMode();
          if (ResultMode.STRUCT_VALUE == mode)
            throw new scale.common.InternalError("ResultMode.STRUCT_VALUE == mode should not happen here");
          else if (ResultMode.ADDRESS == mode) {
            if (annotateCode) {
              appendInstruction(new CommentMarker("Move (from register) for formal parameter: " + i));
              appendInstruction(new CommentMarker(fd.toString()));
            }
            genRegToReg(PPCRegisterSet.FIA_REG + nextArgG, reg);
            nextArgG++;
            continue;
          }
        } else {
          int reg = fd.getValueRegister();
          if (isReal) {
            if (annotateCode) {
              appendInstruction(new CommentMarker("Move (from register) for formal paramter: " + i));
              appendInstruction(new CommentMarker(fd.toString()));
            }
            genRegToReg(PPCRegisterSet.FFA_REG + nextArgF, reg);
            nextArgF++;
          } else {
            // If it is a long long, we cant be in (r4, r5) or (r5,
            // r6), so change nextArgG accordingly.

            if(isLongLong && ((nextArgG % 2) == 1))
              nextArgG++;

            int treg = convertIntRegValue(PPCRegisterSet.FIA_REG + nextArgG,
                                          ts,
                                          true,
                                          reg,
                                          ts,
                                          vt.isSigned());
            if (annotateCode) {
              appendInstruction(new CommentMarker("Move (from register) for formal paramter: " + i));
              appendInstruction(new CommentMarker(fd.toString()));
            }
            genRegToReg(treg, reg);
            offset += ts;
            nextArgG++;
            if (ts > 4) // A long long occupies two integer registers.
              nextArgG++;
          }
        }
      } else if (loc == Assigned.ON_STACK) {
        Displacement disp = fd.getDisplacement();
        if (vt.isAtomicType()) {
          // FIXME: Is this (nextArgF < MAX_ARG_REGS) or (nextArgF <= MAX_ARG_REGS)

          if ((isReal &&                  (nextArgF < MAX_ARG_REGS )) ||
              (!isReal && !isLongLong && (nextArgG < MAX_ARG_REGS )) ||
              (!isReal &&  isLongLong && (nextArgG < (MAX_ARG_REGS - 1)))) {

            if (annotateCode) {
              appendInstruction(new CommentMarker("Formal parameter " + i + "'s address was taken"));
              appendInstruction(new CommentMarker(fd.toString()));
              appendInstruction(new CommentMarker("The caller passed this in register(s)"));
              appendInstruction(new CommentMarker("So moving this to stack"));
            }

            disp = fd.getDisplacement();
            if (isReal) {
              storeIntoMemory(PPCRegisterSet.FFA_REG + nextArgF,
                              PPCRegisterSet.SP_REG,
                              disp,
                              FT_NONE,
                              ts,
                              ts,
                              true);
              offset += ts;
              nextArgF++;
            } else {
              if (isLongLong && (nextArgG % 2 == 1))
                nextArgG++;

              storeIntoMemory(PPCRegisterSet.FIA_REG + nextArgG,
                              PPCRegisterSet.SP_REG,
                              disp,
                              FT_NONE,
                              ts,
                              ts,
                              false);
              offset += ts;
              nextArgG++;
              if (isLongLong)
                nextArgG++;
            }
          } else if (annotateCode) { // Formal parameter i is in stack. So no move necessary
            appendInstruction(new CommentMarker("Formal parameter " + i + " already on stack."));
            appendInstruction(new CommentMarker(fd.toString()));
            appendInstruction(new CommentMarker("No move instruction necessary"));
          }
        } else if (vt.isAggregateType()) {
          throw new scale.common.NotImplementedError("Not yet taken care of address structs stored on stack");
//           int dsize  = vt.memorySizeAsInt(machine);
//           int soffset = 0;

//           for (int j = 0; j < dsize; j += PPCG4RegisterSet.IREG_SIZE) {
//             if (gr < MAX_ARG_REGS) {
//               Displacement disp2 = disp.offset(soffset);
//               Instruction inst = new StoreInstruction(Opcodes.STW,
//                  PPCG4RegisterSet.FIA_REG + gr, PPCG4RegisterSet.SP_REG, disp2);
//               appendInstruction(inst);
//               offset += PPCG4RegisterSet.IREG_SIZE;
//               soffset += PPCG4RegisterSet.IREG_SIZE;
//               gr++;
//             }
//           }
//           continue;
        } else
          throw new scale.common.InternalError("Argument is where " + fd);
      }
    }

    lastInstruction.specifySpillStorePoint();
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

    if (registers.floatRegister(src))
      throw new scale.common.InternalError("Bit complement not allowed on " + arg);

    appendInstruction(new FDrrInstruction(Opcodes.NAND, ir, src, src));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  /**
   * This method generates instructions to call a sub-function.  It
   * places arguments in the appropriate registers or on the stack as
   * required.
   */
  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    Type             rt         = processType(e);
    Expr[]           args       = e.getArgumentArray();
    Displacement     d8         = getDisp(PPCG4RegisterSet.IREG_SIZE);
    StackDisplacement returnDisp = null; // For functions that return structure values.
    Expr             ftn        = e.getFunction();
    Declaration      decl       = null;
    boolean          rest       = true; // GP register must be restored.
    boolean          retStruct  = false;

    if (ftn instanceof LoadDeclAddressExpr)
      decl = ((LoadExpr) ftn).getDecl();

    if (!rt.isVoidType() && !rt.isAtomicType()) {

      // If the routine returns a structure, it is passed through
      // memory and the memory address to use is the first argument.

      returnDisp = new StackDisplacement(localVarSize);
      appendInstruction(new FDdrInstruction(Opcodes.LA,
                                            PPCRegisterSet.FIA_REG,
                                            PPCRegisterSet.SP_REG,
                                            returnDisp,
                                            FT_NONE,
                                            macosx));
      localVarSize += Machine.alignTo(rt.memorySizeAsInt(machine), PPCG4RegisterSet.IREG_SIZE);
      localVar.addElement(returnDisp);
      retStruct = true;
    }

    short[] uses = callArgs(args, retStruct);

    // CR field 6 decides whether we are passing a floating point parameter
    // or not.

    if (!macosx)
      appendInstruction(new FcccInstruction(usesFloatReg ? Opcodes.CREQV : Opcodes.CRXOR,
                                            Opcodes.FPIOE, Opcodes.FPIOE, Opcodes.FPIOE));

    Branch blInst = null;

    if (ftn instanceof LoadDeclAddressExpr)
      decl = (ProcedureDecl) ((LoadExpr) ftn).getDecl();

    if (decl == null) { // Indirection
      needValue(ftn);
      appendInstruction(new FcrInstruction(Opcodes.MTSPR, Opcodes.CTR_SPR, resultReg));
      blInst = new BFormInstruction(Opcodes.BCCTRL, Opcodes.BRANCH_ALWAYS, 0, 1);
    } else {
      RoutineDecl rd = (RoutineDecl)decl;

      addrDisp = rd.getDisplacement().unique();

      blInst = new IFormInstruction(Opcodes.BL, addrDisp, 1, true, true);
      if (macosx)
        calledFuncs.add(decl.getName());
    }

    callsRoutine = true;

    if (rt.isVoidType()) {
      appendCallInstruction(blInst, createLabel(), uses, registers.getCalleeUses(), null, true);
      return;
    }

    if (!rt.isAtomicType()) { // Returning structs is a pain.
      appendCallInstruction(blInst, createLabel(), uses, registers.getCalleeUses(), null, true);

      int vr = registers.newTempRegister(RegisterSet.AIREG);
      appendInstruction(new FDdrInstruction(Opcodes.LA, vr,
                                            PPCRegisterSet.SP_REG,
                                            returnDisp,
                                            FT_NONE,
                                            macosx));
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.ADDRESS;
      resultReg = vr;
      resultRegAddressAlignment = 8;
      return;
    }

    resultRegMode = ResultMode.NORMAL_VALUE;

    if (!rt.isRealType()) {
      if (rt.memorySizeAsInt(machine) > 4) {
        appendCallInstruction(blInst,
                              createLabel(),
                              uses,
                              registers.getCalleeUses(),
                              lintReturn,
                              true);
        int dreg = registers.newTempRegister(RegisterSet.INTREG + RegisterSet.DBLEREG);
        genRegToReg(PPCRegisterSet.IR_REG, dreg);
        resultReg = dreg;
      } else {
        appendCallInstruction(blInst,
                              createLabel(),
                              uses,registers.getCalleeUses(),
                              intReturn,
                              true);
        resultReg = PPCRegisterSet.IR_REG;
      }
      return;
    }

    appendCallInstruction(blInst, createLabel(),
                          uses,
                          registers.getCalleeUses(),
                          realReturn,
                          true);
    resultReg = PPCRegisterSet.FR_REG;
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
    return (macosx) ? macosxCallArgs(args, retStruct) : linuxCallArgs(args, retStruct);
  }

  /**
   * Load the arguments into registers for a routine call.  Only the
   * first MAX_ARG_REGS words of arguements are placed into registers.
   * The remaining words are placed on the stack.
   * There can be a hidden first argument for large returns.
   * @param retStruct is true if the routine returns a struct
   * @return the registers used by the call
   */
  private short[] macosxCallArgs(Expr[] args, boolean retStruct)
  {
    usesFloatReg = false;
    boolean allSimple = true; // True if only simple arguments.
    for (int i = 0; i < args.length; i++) {
      Expr arg = args[i];
      if (isSimple(arg))
        continue;

      allSimple = false;
      break;
    }

    int maxFArgRegs = MAX_ARG_REGS;
    maxFArgRegs = MAX_FARG_REGS_MACOSX;

    int[]   amap = new int[MAX_ARG_REGS + maxFArgRegs];   // These arrays must be allocated each time
    short[] umap = new short[MAX_ARG_REGS + maxFArgRegs]; // because of possible recursion of callArgs.

    int rk       = 0;
    int nextArgG = 0;
    int nextArgF = 0;

    if (retStruct) {
      amap[0] = PPCRegisterSet.FIA_REG;
      umap[0] = (short) PPCRegisterSet.FIA_REG;
      rk = 1;
      nextArgG = 1;
    }

    int stkpos = ARG_SAVE_OFFSET_MACOSX;
    int ai     = 0;

    while (ai < args.length) {
      Expr arg = args[ai++];
      Type vt  = processType(arg);
      int  size = vt.memorySizeAsInt(machine);

      if (trace)
        System.out.println("CFA: " + arg);

      if (vt.isAtomicType()) {
        if ((vt.isRealType() && nextArgF < maxFArgRegs) || (!vt.isRealType() && nextArgG < MAX_ARG_REGS)) {
          int nexr = PPCRegisterSet.FIA_REG + nextArgG;
          if (vt.isRealType()) {
            nexr = PPCRegisterSet.FFA_REG + nextArgF;
            usesFloatReg = true;
          }

          if (allSimple)
            registers.setResultRegister(nexr);

          arg.visit(this);

          if (allSimple)
            registers.setResultRegister(-1);

          if (resultReg < 0)
            resultReg = -resultReg; // Address of string??

          // Postpone transfer because of things like divide
          // which may cause a subroutine call.

          amap[rk] = resultReg;
          umap[rk] = (byte)nexr;

          rk++;
          if (vt.isRealType()) {
            StackDisplacement disp = new StackDisplacement(localVarSize);
            if (nextArgG < MAX_ARG_REGS) {
              localVarSize = (int)Machine.alignTo(localVarSize, size);
              localVar.addElement(disp);
              localVarSize += PPCG4RegisterSet.FREG_SIZE;
              storeIntoMemory(resultReg,
                              PPCRegisterSet.SP_REG,
                              disp,
                              FT_NONE,
                              size,
                              size,
                              true);
              int ir = registers.newTempRegister(RegisterSet.INTREG);
              loadFromMemory(ir,
                             PPCRegisterSet.SP_REG,
                             disp,
                             PPCG4RegisterSet.IREG_SIZE,
                             PPCG4RegisterSet.IREG_SIZE,
                             false);

              nexr = PPCRegisterSet.FIA_REG + nextArgG;
              amap[rk] = ir;
              umap[rk] = (byte) nexr;
              rk++;
            } else {
              disp = new StackDisplacement(stkpos);
              storeIntoMemory(resultReg,
                              PPCRegisterSet.SP_REG,
                              disp,
                              FT_NONE,
                              size,
                              size,
                              true);
              entryOverflowSize += size;
            }

            nextArgG++;
            stkpos += PPCG4RegisterSet.IREG_SIZE;

            if (size > 4) {
              if (nextArgG < MAX_ARG_REGS) {
                int ir = registers.newTempRegister(RegisterSet.INTREG);
                loadFromMemory(ir,
                               PPCRegisterSet.SP_REG,
                               disp.offset(4),
                               PPCG4RegisterSet.IREG_SIZE,
                               PPCG4RegisterSet.IREG_SIZE,
                               false);

                nexr = PPCRegisterSet.FIA_REG + nextArgG;
                amap[rk] = ir;
                umap[rk] = (byte) nexr;
                rk++;
                nextArgG++;
              } else {
                if (nextArgG == MAX_ARG_REGS) {
                  int ir = registers.newTempRegister(RegisterSet.INTREG);
                  loadFromMemory(ir,
                                 PPCRegisterSet.SP_REG,
                                 disp.offset(4),
                                 PPCG4RegisterSet.IREG_SIZE,
                                 PPCG4RegisterSet.IREG_SIZE,
                                 false);
                  disp = new StackDisplacement(stkpos);
                  storeIntoMemory(ir,
                                  PPCRegisterSet.SP_REG,
                                  disp,
                                  FT_NONE,
                                  PPCG4RegisterSet.IREG_SIZE,
                                  PPCG4RegisterSet.IREG_SIZE,
                                  false);
                  entryOverflowSize += PPCG4RegisterSet.IREG_SIZE;
                } else
                  entryOverflowSize += PPCG4RegisterSet.IREG_SIZE;
              }
              stkpos += PPCG4RegisterSet.IREG_SIZE;
            }
            nextArgF++;
          } else {
            nextArgG++;
            stkpos += PPCG4RegisterSet.IREG_SIZE;
            if (registers.doubleRegister(resultReg)) {
              if (nextArgG < MAX_ARG_REGS) {
                amap[rk] = resultReg + 1;
                nexr = PPCRegisterSet.FIA_REG + nextArgG;
                umap[rk] = (byte)nexr;
                nextArgG++;
                rk++;
              } else {  // place second half of argument on stack
                storeIntoMemoryWithOffset(resultReg + 1,
                                          PPCRegisterSet.SP_REG,
                                          stkpos,
                                          PPCG4RegisterSet.IREG_SIZE,
                                          PPCG4RegisterSet.IREG_SIZE,
                                          vt.isRealType());
                entryOverflowSize += Machine.alignTo(size,
                                                     PPCG4RegisterSet.IREG_SIZE);
              }
              stkpos += PPCG4RegisterSet.IREG_SIZE;
            }
          }
          continue;
        }

        // Put the argument on the stack.

        int  rinc   = 1;
        needValue(arg);

        if (size < 4)
          size = 4;

        storeIntoMemoryWithOffset(resultReg,
                                  PPCRegisterSet.SP_REG,
                                  stkpos,
                                  size,
                                  size,
                                  vt.isRealType());
        stkpos += Machine.alignTo(size, PPCG4RegisterSet.IREG_SIZE);
        Displacement disp = new StackDisplacement(stkpos);
        entryOverflowSize += Machine.alignTo(size, PPCG4RegisterSet.IREG_SIZE);
        continue;
      }

      if (vt.isAggregateType()) {
        arg.visit(this);
        int  address    = resultReg;
        long addressoff = resultRegAddressOffset;
        int  addressaln = resultRegAddressAlignment;

        if (resultRegMode == ResultMode.STRUCT_VALUE) {
          for (int k = 0; k < size; k += PPCG4RegisterSet.IREG_SIZE) {
            if (nextArgG < MAX_ARG_REGS) {
              int nexr = PPCG4RegisterSet.FIA_REG + nextArgG;
              int areg = address++;
              amap[rk] = areg;
              umap[rk] = (byte) nexr;
              rk++;
              nextArgG++;
              stkpos += PPCG4RegisterSet.IREG_SIZE;
            } else {
              int nexr = address++;
              storeIntoMemory(nexr,
                              PPCRegisterSet.SP_REG,
                              getDisp(stkpos),
                              FT_NONE,
                              PPCG4RegisterSet.IREG_SIZE,
                              0,
                              false);
              stkpos += PPCG4RegisterSet.IREG_SIZE;
              entryOverflowSize += PPCG4RegisterSet.IREG_SIZE;
            }
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

          for (int k = 0; k < size; k += PPCG4RegisterSet.IREG_SIZE) {
            Displacement odisp2 = getDisp(k + soffset);
            if (nextArgG < MAX_ARG_REGS) {
              int nexr = PPCG4RegisterSet.FIA_REG + nextArgG;
              int areg = allSimple ? nexr :  registers.newTempRegister(RegisterSet.INTREG);
              loadFromMemory(areg, address, odisp2, PPCG4RegisterSet.IREG_SIZE, addressaln, false);
              amap[rk] = areg;
              umap[rk] = (byte) nexr;
              rk++;
              nextArgG++;
              stkpos += PPCG4RegisterSet.IREG_SIZE;
            } else {
              int nexr = registers.newTempRegister(RegisterSet.AIREG);
              loadFromMemory(nexr,
                             address,
                             odisp2,
                             PPCG4RegisterSet.IREG_SIZE,
                             addressaln,
                             false);
              appendInstruction(new StoreInstruction(Opcodes.STW,
                                                     nexr,
                                                     PPCRegisterSet.SP_REG,
                                                     getDisp(stkpos)));
              stkpos += PPCG4RegisterSet.IREG_SIZE;
              entryOverflowSize += PPCG4RegisterSet.IREG_SIZE;
            }
          }
          continue;
      }

      if (vt.isArrayType())
        throw new scale.common.NotImplementedError("callArgs");

      throw new scale.common.InternalError("Argument type " + arg);
    }

    // Move arguments to the register they are passed in if necessary.

    int ne = rk;
    for (int i = 0; i < rk; i++)
      genRegToReg(amap[i], umap[i]);

    // Create the set of used registers for this function call.

    short[] uses = new short[ne + 1];
    System.arraycopy(umap, 0, uses, 0, ne);
    uses[ne + 0] = PPCRegisterSet.SP_REG;

    if (entryOverflowSize > maxOverflowSize)
      maxOverflowSize = entryOverflowSize;
    entryOverflowSize = 0;

    return uses;
  }

  /**
   * Load the arguments into registers for a routine call.  Only the
   * first MAX_ARG_REGS words of arguements are placed into registers.
   * The remaining words are placed on the stack.
   * There can be a hidden first argument for large returns.
   * @param args are the parameters passed to the callee
   * @param retStruct is true if the callee routine returns a struct
   * @return the registers used by the call
   */
  private short[] linuxCallArgs(Expr[] args, boolean retStruct)
  {
    usesFloatReg = false; // Does this routine use a floating point register?

    // Find if all arguments are simple.

    boolean allSimple = true;
    for (int i = 0; allSimple && (i < args.length); i++)
      allSimple = isSimple(args[i]);

    // These arrays must be allocated each time because of possible
    // recursion of callArgs. The size of each is (MAX_ARG_REGS * 2),
    // the multiplication by two is for floating point arguments.

    int[]   amap = new int[MAX_ARG_REGS * 2];
    short[] umap = new short[MAX_ARG_REGS * 2];

    // The algorithm (for passing parameters) is specified in the
    // System V Release 4 ABI for PowerPC page 3-19.

    int rk       = 0;

    // The integer register into which we can put the next parameter
    // is (FIA_REG + nextArgG). The name G is chosen from the
    // algorithm in the SVR 4 ABI.

    short nextArgG = 0;

    // The float register into which we can put the next parameter is
    // (FFA_REG + nextArgG). The name F is chosen from the algorithm
    // in the SVR 4 ABI.

    short nextArgF = 0;

    // The caller has to allocate space for the struct (to be returned
    // by the callee) somewhere and put its address in r3
    // (PPCRegisterSet.FIA_REG).

    if (retStruct) {
      amap[0] = PPCRegisterSet.FIA_REG;
      umap[0] = (short) PPCRegisterSet.FIA_REG;
      rk = 1;
      nextArgG = 1;
    }

    // stkpos is the place where the next argument will go into stack
    // (if it has to).

    int stkpos = ARG_SAVE_OFFSET_LINUX;
    for(int ai = 0; ai < args.length; ai++) {
      Expr arg  = args[ai];
      Type vt   = processType(arg);
      int  size = vt.memorySizeAsInt(machine);
      if (trace)
        System.out.println("CFA: " + arg);

      if (vt.isAtomicType()) {// Process atomic types such as int, double, pointer to foobar, long long. etc.
        boolean isReal     = vt.isRealType();
        boolean isLongLong = !isReal && (64 == ((AtomicType) vt).bitSize());

        // This if checks if a register is available to pass this paramter
        // A register is available if one of the following is true:
        //      1. We have a real number and a floating point register is
        //         available
        //      2. We have a long long integer and TWO integer registers
        //         are available
        //      3. We have an integer that is (smaller than) not long long,
        //         and an integer register is available

        if ((isReal  &&                 (nextArgF < MAX_ARG_REGS )) ||
            (!isReal && !isLongLong && (nextArgG < MAX_ARG_REGS )) ||
            (!isReal &&  isLongLong && (nextArgG < (MAX_ARG_REGS - 1)))) {

          // We have registers to put the argument.

          // If we have a long long, it spans two registers, and
          // can only start from an odd numbered register. i.e. a long long
          // can only be passed as (r3, r4), (r5, r6), (r7, r8) or (r9,
          // r10). We cannot pass it as (r4, r5). If we have to pass a long
          // long, and the next available register is r4, r6, or r8 skip
          // that register and move to the next (odd numbered) register r5,
          // r7, r9 respectively.

          // Also, the lower addressed word is put in r3 and the higher
          // addressed word in r4, etc,

          // Since PPCRegisterSet.FIA_REG points to r3, we have to
          // increment nextArgG if it is *ODD*

          if (isLongLong && ((nextArgG % 2) == 1))
            nextArgG++;

          int nexr = isReal ? (PPCRegisterSet.FFA_REG + nextArgF) : (PPCRegisterSet.FIA_REG + nextArgG);
          usesFloatReg |= isReal;

          if (allSimple)
            registers.setResultRegister(nexr);

          arg.visit(this);
          if (allSimple)
            registers.setResultRegister(-1);

          if (resultReg < 0)
            resultReg = -resultReg;

          // Do not transfer the value to the register yet, because of
          // things like divide which may cause a subroutine call. We keep
          // track of which value should go into which register.
          // amap[i] -- register which contains the argument i
          // umap[i] -- register which should contain the argument i
          // In the end, we do a move from amap[i] to umap[i]

          amap[rk] = resultReg;
          umap[rk] = (byte)nexr;
          rk++;

          if (isReal)
            nextArgF++;
          else {
            nextArgG++;

            // Special handling of long long is needed.  If we have a
            // long long, it has to be in a doubleRegister.  A sanity
            // check to take care of that.

            if(isLongLong && !registers.doubleRegister(resultReg))
              throw new scale.common.InternalError("linuxCallArgs: long long not in a doubleRegister");

            if (isLongLong) {
              amap[rk] = resultReg + 1;
              nexr = PPCRegisterSet.FIA_REG + nextArgG;
              umap[rk] = (byte)nexr;
              nextArgG++;
              rk++;
            }
          }
        } else {
          // We do not have a register to put this operand into :(
          // We have to put it into stack.

          int rinc = 1;
          needValue(arg);

          if (size < 4)
            size = 4;

          storeIntoMemoryWithOffset(resultReg, PPCRegisterSet.SP_REG, stkpos, size, size, isReal);
          stkpos += Machine.alignTo(size, PPCG4RegisterSet.IREG_SIZE);
          Displacement disp = new StackDisplacement(stkpos);
          entryOverflowSize += Machine.alignTo(size, PPCG4RegisterSet.IREG_SIZE);
        }
      } else if (vt.isAggregateType()) { // Process aggregate types such as structs, unions, etc.
        arg.visit(this);
        int  address    = resultReg;
        long addressoff = resultRegAddressOffset;
        int  addressaln = resultRegAddressAlignment;

        if (ResultMode.STRUCT_VALUE == resultRegMode)
          throw new scale.common.InternalError("ResultMode.STRUCT_VALUE should not occur");

        if (ResultMode.ADDRESS == resultRegMode) {
          if (addressoff != 0)
            throw new scale.common.NotImplementedError("addressoff should be ZERO (for now)");

          // We have the address of the struct/union we are going pass to
          // the callee. We just have to place it in an integer register
          // (if one is available).

          if(nextArgG < MAX_ARG_REGS) { // We can put this address in a register.
            int nexr = PPCG4RegisterSet.FIA_REG + nextArgG;
            amap[rk] = address;
            umap[rk] = (byte) nexr;
            nextArgG++;
            rk++;
          } else {
            // We have to put this address in stack. Create a new store
            // instruction to store this value on stack

            // FIXME. Assuming that the size of a pointer is 4 bytes.
            // Should replace this by a variable

            storeIntoMemoryWithOffset(resultReg, PPCRegisterSet.SP_REG, stkpos, 4, 4L, false);
            stkpos += Machine.alignTo(4, PPCG4RegisterSet.IREG_SIZE);
            Displacement disp = new StackDisplacement(stkpos);
            entryOverflowSize += Machine.alignTo(4, PPCG4RegisterSet.IREG_SIZE);
          }
        } else
          throw new scale.common.NotImplementedError("Only ResultMode.ADDRESS has been implemented for now");
      } else if (vt.isArrayType())
        throw new scale.common.NotImplementedError("callArgs");
      else
        throw new scale.common.InternalError("Argument type " + arg);
    }

    // Move arguments to the register they are passed in, if necessary.

    int ne = rk;
    for (int i = 0; i < rk; i++)
      genRegToReg(amap[i], umap[i]);

    // Create the set of used registers for this function call.

    short[] uses = new short[ne + 1];
    System.arraycopy(umap, 0, uses, 0, ne);
    uses[ne + 0] = PPCRegisterSet.SP_REG;
    if (entryOverflowSize > maxOverflowSize)
      maxOverflowSize = entryOverflowSize;

    entryOverflowSize = 0;
    return uses;
  }

  public void visitCompareExpr(CompareExpr e)
  {
    throw new scale.common.NotImplementedError("visitCompareExpr");
  }

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
   * @return the register containing the convert value
   */
  protected int convertIntRegValue(int src, int srcSize, boolean srcSigned,
                                   int dest, int destSize, boolean destSigned)
  {
    if (annotateCode)
      appendInstruction(new CommentMarker(" r" + dest + " <- " + " r" + src));

    int which = smapSize[srcSize] + dmapSize[destSize];
    if (srcSigned)
      which += SSIGNED;

    if (destSigned)
      which += DSIGNED;

    switch (ccase[which]) {
    case EBEXT_I:// From an int (32 bit), Extract byte & sign extend.
      appendInstruction(new FDrInstruction(Opcodes.EXTSB, dest, src));
      break;
    case ESEXT_I: // From an int (32 bit), Extract short & sign extend.
      appendInstruction(new FDrInstruction(Opcodes.EXTSH, dest, src));
      break;
      // case EIEXT_I:
      // From an int (32 bit), Extract int & sign extend.
    case ELLEXT_I: // From an int (32 bit), Extract long long and sign extend.
      appendInstruction(new FDrcInstruction(Opcodes.SRAWI, dest, src, 31, macosx));
      appendInstruction(new FDrInstruction(Opcodes.MR, dest + 1, src));
      break;
    case EUB_I: // From an int (32 bit), Extract unsigned byte.
      // Converting to an unsigned byte. We need the least significant
      // 8 bits (bits 0 - 7) This call to the RLWINM means dest = (src
      // << 0) & 0xff.

      appendInstruction(new FDrccInstruction(Opcodes.RLWINM, dest, src, 0, 0xff));
      break;
    case EUS_I: // From an int (32 bit), Extract unsigned short.
      // Converting to an unsigned short int. We need the least
      // significant 16 bits (bits 0 - 15) This call to the RLWINM
      // means dest = (src << 0) & 0xffff.

      appendInstruction(new FDrccInstruction
                        (Opcodes.RLWINM, dest, src, 0, 0xffff));
      break;
    case EUI_I: // From an int (32 bit), Extract unsigned int.
      appendInstruction(new FDrInstruction(Opcodes.MR, dest, src));
      break;
    case EULL_I: // From an int (32 bit), Extract unsigned long long.
      appendInstruction(new FDcInstruction(Opcodes.LI, dest, 0));
      appendInstruction(new FDrInstruction(Opcodes.MR, dest + 1, src));
      break;
    case EBEXT_LL: // From a long long (64 bit), Extract byte & sign extend.
      appendInstruction(new FDrInstruction(Opcodes.EXTSB, dest, src + 1));
      break;
    case ESEXT_LL: // From a long long (64 bit), Extract short & sign extend.
      appendInstruction(new FDrInstruction(Opcodes.EXTSH, dest, src + 1));
      break;
    case EIEXT_LL: // From a long long (64 bit), Extract int & sign extend.
      appendInstruction(new FDrInstruction(Opcodes.MR, dest, src + 1));
      break;
      // case ELLEXT_LL:
      /* From a long long (64 bit), Extract long logn and sign extend */
    case EUB_LL: // From a long long (64 bit), Extract unsigned byte.
      // Converting to an unsigned byte. We need the least significant
      // 8 bits (bits 0 - 7) This call to the RLWINM means dest =
      // ((src + 1) << 0) & 0xff.
      appendInstruction(new FDrccInstruction(Opcodes.RLWINM, dest, src + 1, 0, 0xff));
      break;
    case EUS_LL: // From a long long (64 bit), Extract unsigned short.
      // Converting to an unsigned short int. We need the least
      // significant 16 bits (bits 0 - 15) This call to the RLWINM
      // means dest = ((src + 1) << 0) & 0xffff.

      appendInstruction(new FDrccInstruction(Opcodes.RLWINM, dest, src + 1, 0, 0xffff));
      break;
    case EUI_LL: // From a long long (64 bit), Extract unsigned int.
      appendInstruction(new FDrInstruction(Opcodes.MR, dest, src + 1));
      break;
    case EULL_LL: // From a long long (64 bit), Extract unsigned long long.
      appendInstruction(new FDrInstruction(Opcodes.MR, dest, src));
      appendInstruction(new FDrInstruction(Opcodes.MR, dest + 1, src + 1));
      break;
    default:
      throw new scale.common.InternalError("Funny conversion " + which);
    }
    return dest;
  }

  /**
   * Generate code to zero out a floating point register.
   */
  protected void zeroFloatRegister(int dest, int destSize)
  {
    throw new scale.common.NotImplementedError("zeroFloatRegister");
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
   * TODO: Makes a function call. Should make this code inline. Buggy
   * inline code existed in version 1.75
   *
   * @param src is the register containing the source value
   * @param srcSize is the source value size
   * @param dest is the register containing the result
   * @param destSize is the size of the result value
   * @param destSigned is true if the result value is signed
   * @return the register containing the converted value
   */
  protected int genRealToInt(int src, int srcSize, int dest, int destSize, boolean destSigned)
  {
    // It is possible for destSize to be smaller than 4 bytes. For example,
    //   short int i = 1.0;
    // However, the soft float routines that we have, convert only into
    // ints and longlongs. Therefore, we first convert the real number into
    // an integer, and then into a smaller integer type, by calling
    // convertIntRegValue().

    int destSize2 = (destSize < 4) ? 4 : destSize;

    // Now, check the sizes, and see if we can handle them.

    if (((srcSize   != 4) && (srcSize   != 8)) ||
        ((destSize2 != 4) && (destSize2 != 8)))
      throw new scale.common.InternalError("Invalid size");

    String funcname = riFuncNames[destSigned ? 1 : 0][destSize2 / 4][srcSize / 4];

    //* Passing the parameter to the call.

    genRegToReg(src, PPCRegisterSet.FFA_REG);

    // genFtnCall makes the call to the function

    short[] uses = new short[2];
    uses[0] = (short) PPCRegisterSet.SP_REG;
    uses[1] = (short) PPCRegisterSet.FFA_REG;
    genFtnCall(funcname, uses, null);
    if (destSize2 != destSize) {
      convertIntRegValue(/* src        */ PPCRegisterSet.IR_REG,
                         /* srcSize    */ 4,
                         /* srcSigned  */ destSigned,
                         /* dest       */ dest,
                         /* destSize   */ destSize,
                         /* destSigned */ destSigned);
    } else
      genRegToReg(PPCRegisterSet.IR_REG, dest);

    return dest;
  }

  /**
   * Convert a real value in a real register to a real value in a real
   * register.
   */
  protected void genRealToReal(int src, int srcSize, int dest, int destSize)
  {
    if (src != dest)
      appendInstruction(new FDrInstruction(Opcodes.FMR, dest, src));
    if (srcSize == 8 && destSize == 4)
      appendInstruction(new FDrInstruction(Opcodes.FRSP, dest, dest));
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
   * TODO: Makes a function call. Should make this code inline. Buggy
   * inline code existed in version 1.75
   * @param src      the src register
   * @param srcSize  the size of the src register
   * @param dest     the dest register
   * @param destSize the size of the dest register
   */
  protected void genUnsignedIntToReal(int src, int srcSize, int dest, int destSize)
  {
    // The src is of the form x = (MSBit concatenated with y).  To convert
    // x to real ---
    //   convert y to real            call that A
    //   convert MSBit<<30 to real    call that B
    // The result is A + B + B

    // Replace 30 with 62 when src is 64 bit.

    // Convert y to real (call that A).

    int y = registers.newTempRegister(registers.getType(src));
    genRegToReg(src, y);

    // Clear the msb of y which is register y when it is 32 bit or 64
    // bit.  The code below is the instruction CLRLWI y, y, 1.

    appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, y, y, 0, 1, 31));
    int yReal = registers.newTempRegister(registers.getType(dest));
    genIntToReal(y, srcSize, yReal, destSize);

    // Put MSB in position 30 (or 62 for a 64 bit number) and convert
    // that to real (call that B).

    int msbitPow30 = registers.newTempRegister(registers.getType(src));

    // The following instruction takes the MSB of src and puts it in
    // bit position 30.

    appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, msbitPow30, src, 31, 1, 1));
    if (registers.doubleRegister(src))
      appendInstruction(new FDcInstruction(Opcodes.LI, msbitPow30 + 1, 0));

    int msbitPow30Real = registers.newTempRegister(registers.getType(dest));
    genIntToReal(msbitPow30, srcSize, msbitPow30Real, destSize);

    // dest = B + B

    doBinaryOp(/* dest     */ dest,
               /* laReg    */ msbitPow30Real,
               /* raReg    */ msbitPow30Real,
               /* size     */ destSize,
               /* isReal   */ true,
               /* isSigned */ false,
               /* which    */ ADD);

    // dest = dest + A

    doBinaryOp(/* dest     */ dest,
               /* laReg    */ dest,
               /* raReg    */ yReal,
               /* size     */ destSize,
               /* isReal   */ true,
               /* isSigned */ false,
               /* which    */ ADD);
  }

  /**
   * Convert an integer value in an integer register to a
   * real value in a real register.
   * TODO: Makes a function call. Should make this code inline. Buggy
   * inline code existed in version 1.75
   * @param src      the src register containing the integer
   * @param srcSize  the size of the src register
   * @param dest     the dest register to which the real should be written to
   * @param destSize the size of the dest register
   */
  protected void genIntToReal(int src, int srcSize, int dest, int destSize)
  {
    // It is possible for srcSize to be smaller than 4 bytes. For example,
    //   short int i = 1;
    //   double d = i;

    // However, the soft float routines that we have, convert only
    // from ints and longlongs. Therefore, we first convert the
    // smaller integer types into a 4 byte integer, by calling
    // convertIntRegValue(), and then convert to real.

    int nSrcSize = (srcSize < 4) ? 4 : srcSize;
    int nSrc     = (nSrcSize == srcSize) ? src : registers.newTempRegister(RegisterSet.INTREG);
    if (nSrcSize != srcSize) {
      convertIntRegValue(/* src        */ src,
                         /* srcSize    */ srcSize,
                         /* srcSigned  */ true,
                         /* dest       */ nSrc,
                         /* destSize   */ 4,
                         /* destSigned */ true);
    }
    if (((nSrcSize != 4) && (nSrcSize != 8)) ||
        ((destSize != 4) && (destSize != 8)))
      throw new scale.common.InternalError("Unknown sizes");

    //The row index is (nSrcSize/4) i.e. the (integersize/4), and the
    //column index is (destSize/4) i.e.  the (realsize/4).

    String funcname = irFuncNames[nSrcSize/4][destSize/4];

    // Passing the parameter to the call.

    if ((8 == nSrcSize) && !registers.doubleRegister(src))
      throw new scale.common.InternalError("src is not a double register");

    genRegToReg(src, PPCRegisterSet.FIA_REG);

    // genFtnCall makes the call to the function.

    short[] uses = new short[(8 == nSrcSize) ? 3 : 2];
    uses[0] = (short) PPCRegisterSet.SP_REG;
    uses[1] = (short) PPCRegisterSet.FIA_REG;
    if (8 == nSrcSize)
      uses[2] = (short) PPCRegisterSet.FIA_REG + 1;

    genFtnCall(funcname, uses, null);
    genRegToReg(PPCRegisterSet.FR_REG, dest);
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
    Type    ct     = processType(e);
    int     bs     = ct.memorySizeAsInt(machine);
    int     ir     = registers.getResultRegister(ct.getTag());
    Expr    la     = e.getLeftArg();
    Expr    ra     = e.getRightArg();
    boolean signed = ct.isSigned();
    boolean flt    = ct.isRealType();
    boolean cmplx  = ct.isComplexType();

    if (ra.isLiteralExpr()) {
 
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
        int laReg = resultReg;

        if (value == 1) { // X**1
          genRegToReg(laReg, ir);
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }

        if (value == 2) { // X**2
          if (cmplx)
            doComplexOp(MUL, bs, laReg, laReg, ir);
          else {
            if (flt) {
              int opcode = (4 < bs) ? Opcodes.FMUL : Opcodes.FMULS;
              appendInstruction(new FDrrInstruction(opcode, ir, laReg, laReg));
            } else
              genMultiply(laReg, laReg, ir, bs);
          }
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
        if (value == 3) { // X**3
          if (cmplx) {
            int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
            doComplexOp(MUL, bs, laReg, laReg, tr);
            doComplexOp(MUL, bs, laReg, tr, ir);
          } else {
            int tr = registers.newTempRegister(ct.getTag());
            if (flt) {
              int opcode = (4 < bs) ? Opcodes.FMUL : Opcodes.FMULS;
              appendInstruction(new FDrrInstruction(opcode, tr, laReg, laReg));
              appendInstruction(new FDrrInstruction(opcode, ir, tr, laReg));
            } else {
              genMultiply(laReg, laReg, tr, bs);
              genMultiply(laReg, tr, ir, bs);
            }
          }
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
        if (cmplx) {
          int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
          doComplexOp(MUL, bs, laReg, laReg, ir);
          for (int i = 0; i < value - 2; i++)
            doComplexOp(MUL, bs, laReg, tr, tr);
          doComplexOp(MUL, bs, laReg, tr, ir);
        } else {
          int tr = registers.newTempRegister(ct.getTag());
          if (flt) {
            int opcode = (4 < bs) ? Opcodes.FMUL : Opcodes.FMULS;
            appendInstruction(new FDrrInstruction(opcode, tr, laReg, laReg));
            for (int i = 0; i < value - 3; i++)
              appendInstruction(new FDrrInstruction(opcode, tr, laReg, tr));
            appendInstruction(new FDrrInstruction(opcode, ir, laReg, tr));
          } else {
            genMultiply(laReg, laReg, tr, bs);
            for (int i = 0; i < value - 3; i++)
              genMultiply(laReg, tr, tr, bs);
            genMultiply(laReg, tr, ir, bs);
          }
        }
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    Type lt  = processType(la);
    int  lts = lt.memorySizeAsInt(machine);
    Type rt  = processType(ra);
    int  rts = rt.memorySizeAsInt(machine);

    if (!cmplx && rt.isRealType()) {

      // If the right argument is floating point use the math
      // library pow function.

      needValue(la);
      int laReg = 0;
      if (lt.isRealType()) {
        laReg = resultReg;
        if (lts != 8) {
          int tr = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.DBLEREG);
          genRealToReal(resultReg, lts, tr, 8);
          laReg = tr;
        }
      } else {
        laReg = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.DBLEREG);
        genIntToReal(resultReg, lts, laReg, 8);
      }

      needValue(ra);
      int raReg = resultReg;
      if (rts != 8) {
        int tr = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.DBLEREG);
        genRealToReal(resultReg, rts, tr, 8);
        raReg = tr;
      }
      genRegToReg(laReg, PPCRegisterSet.FFA_REG);
      genRegToReg(raReg, PPCRegisterSet.FFA_REG + 2);
      genFtnCall("pow", quadIntUses, null);
      genRealToReal(PPCRegisterSet.FR_REG, 8, ir, bs);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    String fname  = "pow";
    if ((bs != 8) || !lt.isRealType() || !rt.isRealType() || (lts != 8) || (rts != 8)) {
      StringBuffer ftn   = new StringBuffer("_scale_pow");
      int          li    = (lts - 1) / 4;
      String       left  = intArgType[li];
      int          ri    = (rts - 1) / 4;
      String       right = intArgType[ri];

      if (lt.isRealType())
        left = fltArgType[li];
      ftn.append(left);

      if (rt.isRealType())
        right = fltArgType[ri];
      ftn.append(right);

      fname = ftn.toString();
    }

    short[] uses = callArgs(e.getOperandArray(), false);
    genFtnCall(fname, uses, null);
    genRegToReg(returnRegister(ct.getTag(), true), ir);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    int  ir = registers.getResultRegister(ct.getTag());
    Expr ra = e.getOperand(1);
    Expr la = e.getOperand(0);

    if (ct.isRealType()) {
      doBinaryOp(e, DIV);
      return;
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
      } else
        throw new scale.common.NotImplementedError("LiteralExpr");

      if (flag) {
        la.visit(this);
        genDivideLit(value, resultReg, ir, bs > 4, ct.isSigned());
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    doBinaryOp(e, DIV);
  }

  private void genDivideLit(long value, int src, int dest, boolean dble, boolean signed)
  {
    int tr;
    if (registers.floatRegister(dest)) {
      tr = registers.newTempRegister(RegisterSet.FLTREG);
      defLongValue(value, tr);
      if (dble)
        appendInstruction(new FDrrInstruction(Opcodes.FDIV, dest, src, tr));
      else
        appendInstruction(new FDrrInstruction(Opcodes.FDIVS, dest, src, tr));
    } else {
      if (dble) {
        tr = registers.newTempRegister
          (RegisterSet.INTREG + RegisterSet.DBLEREG);
        genLoadImmediate(value, tr);
        genRegToReg(src, PPCRegisterSet.FIA_REG);
        genRegToReg(tr, PPCRegisterSet.FIA_REG + 2);
        short[] uses = new short[5];
        uses[0] = (short) PPCRegisterSet.SP_REG;
        uses[1] = (short) PPCRegisterSet.FIA_REG;
        uses[2] = (short) PPCRegisterSet.FIA_REG + 1;
        uses[3] = (short) PPCRegisterSet.FIA_REG + 2;
        uses[4] = (short) PPCRegisterSet.FIA_REG + 3;

        genFtnCall(signed ? "__divdi3" : "__udivdi3", uses, null);
        genRegToReg(PPCRegisterSet.IR_REG, dest);
      } else {
        tr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(value, tr);
        appendInstruction(new FDrrInstruction(signed ? Opcodes.DIVW : Opcodes.DIVWU, dest, src, tr));
      }
    }
  }


  public void visitRemainderExpr(RemainderExpr e)
  {
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    int  ir = registers.getResultRegister(ct.getTag());
    Expr ra = e.getRightArg();
    Expr la = e.getLeftArg();

    if (ct.isRealType())
      throw new scale.common.NotImplementedError("visitRemainderExpr1");

    // doBinaryOp takes care of 64 bit MOD

    if (8 == bs) {
      doBinaryOp(e, MOD);
      return;
    }

    // Only 32 bit operations are considered here.

    //  if (ra.isLiteralExpr()) {
    //    LiteralExpr le    = (LiteralExpr) ra;
    //    Literal     lit   = le.getLiteral();
    //    boolean     flag  = false;
    //    long        value = 0;

    //    if (lit instanceof IntLiteral) {
    //      IntLiteral il = (IntLiteral) lit;
    //      value = il.getLongValue();
    //      flag = true;
    //    } else if (lit instanceof SizeofLiteral) {
    //      value = valueOf((SizeofLiteral) lit);
    //      flag = true;
    //    }

    //    if (flag) {
    //      needValue(la);
    //      int tr1 = registers.newTempRegister(RegisterSet.INTREG);
    //      int tr2 = registers.newTempRegister(RegisterSet.INTREG);
    //      genDivideLit(value, resultReg, tr1, bs > 4, ct.isSigned());
    //      if ((bs > 4) || !ct.isSigned()) {
    //        genMultiplyLit(value, tr1, tr2, registers.registerSize(tr1));
    //        appendInstruction
    //          (new FDrrInstruction(Opcodes.SUBF, ir, tr2, resultReg));
    //        if (bs < 8)
    //          throw new scale.common.NotImplementedError("visitRemainderExpr2");
    //      } else {
    //        genMultiplyLit(value, tr1, tr2, registers.registerSize(tr1));
    //        appendInstruction
    //          (new FDrrInstruction(Opcodes.SUBF, ir, tr2, resultReg));
    //      }
    //      resultRegAddressOffset = 0;
    //      resultRegMode = ResultMode.NORMAL_VALUE;
    //      resultReg = ir;
    //      return;
    //    }
    //  }

    int tr = registers.newTempRegister(RegisterSet.INTREG);
    needValue(la);
    int lreg = resultReg;
    needValue(ra);
    int rreg = resultReg;
    appendInstruction(new FDrrInstruction(ct.isSigned() ? Opcodes.DIVW : Opcodes.DIVWU, tr, lreg, rreg));
    genMultiply(tr, rreg, tr, registers.registerSize(rreg));
    appendInstruction(new FDrrInstruction(Opcodes.SUBF, ir, tr, lreg));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
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

        if (ft.isRealType())
          // Assume entire struct is in floating point registers and
          // contains only doubles because there is no easy transfer
          // between integer and double registers and there is no
          // conversion from 32-bit single to 64-bit double format that
          // does not go through memory.
          throw new scale.common.NotImplementedError("float structs in registers");

        resultRegMode = ft.isAtomicType() ?  ResultMode.NORMAL_VALUE : ResultMode.STRUCT_VALUE;
        resultRegSize = ft.memorySize(machine);

        if (fieldOffset >= 4) {
          if (fieldOffset >= 8) {
            if (fieldOffset >= 12) {
              fieldOffset -= 12;
              adr += 3;
            } else {
              fieldOffset -= 8;
              adr += 2;
            }
          } else {
            fieldOffset -= 4;
            adr++;
          }
        }

        bitOffset += 4 * ((int) fieldOffset);
        if (bits == 0)
          bits = 8 * byteSize;

        if (bits == 32) {
          appendInstruction(new FDrInstruction(Opcodes.MR, dest, adr));
          resultRegMode = ResultMode.NORMAL_VALUE;
          return;
        }

        int sr = adr;
        if (ft.isSigned())
          appendInstruction(new FDrcInstruction(Opcodes.SRAWI,
                                                dest,
                                                adr,
                                                32 - bits,
                                                macosx));
        else
          appendInstruction(new FDrcccInstruction(Opcodes.RLWINM,
                                                  dest,
                                                  adr,
                                                  bits + bitOffset,
                                                  32 - bits,
                                                  31));

        return;
    }

    // Structure is in memory.

    resultRegMode = ResultMode.NORMAL_VALUE;
    if (!ft.isAtomicType()) {
      //      if (byteSize > 16) {
      resultRegAddressAlignment = (((fieldOffset & 0x7) == 0) ?
                                   8 :
                                   (((fieldOffset & 0x3) == 0) ? 4 : 1));
      resultRegMode = ResultMode.ADDRESS;
      return;
      //      }
      //      resultRegMode = ResultMode.STRUCT_VALUE;
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

      loadFromMemory(dest, adr, disp, FT_NONE, byteSize, alignment, ft.isSigned());
      resultReg = dest;
      return;
    }

    int tr = registers.newTempRegister(RegisterSet.ADRREG);
    genLoadImmediate(fieldOffset, adr, tr);
    adr = tr;

    loadBitsFromMemory(dest, adr, getDisp(0), FT_NONE, bits, bitOffset, alignment, ft.isSigned());
    resultReg = dest;
  }

  /**
   * Generate a branch based on the value of an expression compared to
   * zero.  The value may be floating point or integer but it is never a
   * value pair.
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
    if (registers.doubleRegister(treg)) {
      if (annotateCode)
        appendInstruction(new CommentMarker("genIfRegister: comparing long long to ZERO"));

      // We have a 64 bit comparison. If treg == 0, we go to the else
      // path (labf).

      // We will call the GNU library __cmpdi2 functions to do the
      // comparison. The code below sets up the arguments for the call.
      // 1st argument (r3, r4): treg, the long long number we want to compare
      // 2nd argument (r5, r6): ZERO.

      String funcName = signed ? "__cmpdi2" : "__ucmpdi2";
      genRegToReg(treg, PPCRegisterSet.FIA_REG);
      genLoadImmediate(0, PPCRegisterSet.FIA_REG + 2);
      genLoadImmediate(0, PPCRegisterSet.FIA_REG + 3);
      short[] uses = new short[5];
      uses[0] = (short) PPCRegisterSet.SP_REG;
      uses[1] = (short) PPCRegisterSet.FIA_REG;
      uses[2] = (short) PPCRegisterSet.FIA_REG + 1;
      uses[3] = (short) PPCRegisterSet.FIA_REG + 2;
      uses[4] = (short) PPCRegisterSet.FIA_REG + 3;
      genFtnCall(funcName, uses, null);
      int tempReg = registers.newTempRegister(RegisterSet.INTREG);
      genRegToReg(PPCRegisterSet.IR_REG, tempReg);

      // tempReg has value
      // 0 if treg <  0
      // 1 if treg == 0
      // 2 if treg >  0

      // The original check was comparison of treg against zero. Now we
      // have to check tempReg to see the result of the comparison.

      // +------------+-----------------+
      // |  original  |      new        |
      // | comparison |  comparison     |
      // +------------+-----------------+
      // | treg EQ 0  | tempReg == 1  |
      // | treg NE 0  | tempReg != 1  |
      // | treg LE 0  | tempReg != 2  |
      // | treg LT 0  | tempReg == 0  |
      // | treg GE 0  | tempReg != 0  |
      // | treg GT 0  | tempReg == 2  |
      // +------------+-----------------+

      long compareAgainst;
      int  comparisonSense;
      switch (which) {
      case EQ:
        compareAgainst  = 1;
        comparisonSense = Opcodes.BRANCH_TRUE;
        break;
      case NE:
        compareAgainst  = 1;
        comparisonSense = Opcodes.BRANCH_FALSE;
        break;
      case LE:
        compareAgainst  = 2;
        comparisonSense = Opcodes.BRANCH_FALSE;
        break;
      case LT:
        compareAgainst  = 0;
        comparisonSense = Opcodes.BRANCH_TRUE;
        break;
      case GE:
        compareAgainst  = 0;
        comparisonSense = Opcodes.BRANCH_FALSE;
        break;
      case GT:
        compareAgainst  = 2;
        comparisonSense = Opcodes.BRANCH_TRUE;
        break;
      default:
        throw new scale.common.InternalError("Uknown comparison");
      }

      appendInstruction(new FrcInstruction(Opcodes.CMPWI, tempReg, compareAgainst));

      LabelDisplacement elseDisplacement = new LabelDisplacement(labf);
      Branch            binst            = new BFormInstruction(Opcodes.BC,
                                                                comparisonSense,
                                                                Opcodes.EQ,
                                                                elseDisplacement,
                                                                2);

      binst.addTarget(labf, 0);
      binst.addTarget(labt, 1);
      lastInstruction.specifySpillStorePoint();
      appendInstruction(binst);
      if (annotateCode)
        appendInstruction(new CommentMarker("genIfRegister: END of comparision"));
      return;
    }

    // If control comes here, it means we are dealing with a 32 bit
    // value.

    appendInstruction(new FrcInstruction(signed ? Opcodes.CMPWI : Opcodes.CMPLWI, treg, 0L));
    lastInstruction.specifySpillStorePoint();
    Branch instt = genTestBranch(which, labf, labt);
    appendInstruction(instt);
  }

  /**
   * Create a new read-only data area whose value is a table of
   * displacements.
   */
  private Displacement createAddressTable(Chord[] entries, long[] indexes, int min, int max)
  {
    int     num = max + 1 - min;
    Label[] v   = new Label[num];
    Label   def = getBranchLabel(entries[entries.length - 1]);

    for (int i = 0; i < num; i++)
      v[i] = def;


    for (int i = 0; i < entries.length - 1; i++) {
      int in = (int) indexes[i] - min;
      if (in >= 0)
        v[in] = getBranchLabel(entries[i]);
    }

    String       name   = un.genName();
    int          handle = allocateData(name, TEXT, SpaceAllocation.DAT_ADDRESS, 4, true, v, 1, 4);
    Displacement disp   = new SymbolDisplacement(name, handle);

    associateDispWithArea(handle, disp);

    return disp;
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

      needValue(ra);

      int     raReg  = resultReg;
      int     treg   = registers.newTempRegister(RegisterSet.FLTREG);
      boolean swap   = true;
      int     opcode = 0;

      appendInstruction(new FcrrInstruction(Opcodes.FCMPO, 0, laReg, raReg));
      int ireg = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new FDcInstruction(Opcodes.LI, ireg, 1));

      Label  tlab   = createLabel();
      Label  flab   = createLabel();
      Branch brInst = genTestBranch(which, tlab, flab);;

      lastInstruction.specifySpillStorePoint();

      appendInstruction(brInst);
      appendLabel(flab);
      flab.setNotReferenced();

      appendInstruction(new FDcInstruction(Opcodes.LI, ireg, 0));
      appendLabel(tlab);

      if (lt.isComplexType())
        throw new scale.common.NotImplementedError("genIfRelational");

      genIfRegister(swap ? CompareMode.NE : CompareMode.EQ, ireg, true, tc, fc);

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
      //      predicate = new EqualityExpr(predicate.getType(), la.copy(), ra.copy());
      rflag = !rflag;
      Chord t = tc;
      tc = fc;
      fc = t;
    }

    genIfRegister(rflag ? CompareMode.EQ : CompareMode.NE, predicate, tc, fc);
  }


  /**
   * Load the address of an array element into a register.
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

    offseta = resultRegAddressOffset + bs * offseth;
    int arr = resultReg;

    if (bs == 1) {
      appendInstruction(new FDrrInstruction(Opcodes.ADD, ir, arr, tr));
      resultRegAddressOffset = offseta;
      resultRegMode = ResultMode.ADDRESS_VALUE;
      resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
      resultReg = ir;
      return;
    }

    int tr3 = registers.newTempRegister(RegisterSet.INTREG);
    genMultiplyLit(bs, tr, tr3, registers.registerSize(tr));
    appendInstruction(new FDrrInstruction(Opcodes.ADD, ir, arr, tr3));
    resultRegAddressOffset = offseta;
    resultRegMode = ResultMode.ADDRESS_VALUE;
    resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
    resultReg = ir;
    return;
  }

  public void visitMultiplicationExpr(MultiplicationExpr e)
  {
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    Expr ra = e.getOperand(1);
    Expr la = e.getOperand(0);

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
      } else {
        throw new scale.common.NotImplementedError("LiteralExpr");
      }

      if (flag) {
        int ir = registers.getResultRegister(ct.getTag());
        la.visit(this);
        genMultiplyLit(value, resultReg, ir, registers.registerSize(resultReg));
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    doBinaryOp(e, MUL);
  }

  private void genMultiplyLit(long value, int src, int dest, int bs)
  {
    if (bs > 8)
      throw new scale.common.NotImplementedError("MUL for " + bs);

    if (value == 0) {
      if (registers.doubleRegister(dest)) {
        appendInstruction(new FDrrInstruction(Opcodes.XOR, dest, dest, dest));
        appendInstruction(new FDrrInstruction(Opcodes.XOR, dest + 1, dest + 1, dest + 1));
        return;
      }
      appendInstruction(new FDrrInstruction(Opcodes.XOR, dest, dest, dest));
      return;
    }
    if (value == 1) {
      if (registers.doubleRegister(dest)) {
        appendInstruction(new FDrcInstruction(Opcodes.ORI, dest, src, 0, macosx));
        appendInstruction(new FDrcInstruction(Opcodes.ORI, dest + 1, src + 1, 0, macosx));
        return;
      }
      appendInstruction(new FDrcInstruction(Opcodes.ORI, dest, src, 0, macosx));
      return;
    }
    if (registers.doubleRegister(dest)) {
      int treg = registers.newTempRegister(RegisterSet.DBLEREG + RegisterSet.INTREG);
      genLoadImmediate(value, treg);
      genMultiply(src, treg, dest, 8);
      return;
    }
    if (value == -1) {
      appendInstruction(new FDrInstruction(Opcodes.NEG, dest + 1, src + 1));
      return;
    }
    if (value == 2) {
      appendInstruction(new FDrrInstruction(Opcodes.ADD, dest, src, src));
      return;
    }
    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // It is a multiply by a power of 2.
      appendInstruction(new FDrcInstruction(Opcodes.SLWI , dest, src, shift, macosx));
      return;
    }
    if ((value >= MIN_IMM16) && (value <= MAX_IMM16)) {
      appendInstruction(new FDrcInstruction(Opcodes.MULLI, dest, src, (int)value, macosx));
      return;
    }
    int tr = multNeedsTemp(src, dest);
    genLoadImmediate(value, tr);
    appendInstruction(new FDrrInstruction(Opcodes.MULLW, dest, src, tr));
    return;
  }

  private int multNeedsTemp(int src, int dest)
  {
    if (src != dest)
      return dest;
    return registers.newTempRegister(registers.getType(dest));
  }

  private void genMultiply(int laReg, int raReg, int dest, int bs)
  {
    if (4 >= bs) {
      appendInstruction(new FDrrInstruction(Opcodes.MULLW, dest, laReg, raReg));
    } else if (8 == bs) {
      if (g5) {
        appendInstruction(new FDrrInstruction(Opcodes.MULLD, dest, laReg, raReg));
      } else {

        // laReg may be of a size different (smaller) from 64 bits. So we
        // do a convertIntRegValue() to change laReg to 64 bits and put it
        // in lhsReg.  convertIntRegValue does not always put things into
        // the `dest' register we specify (especially when both srcSize and
        // destSize are the same). We have to explicitly do a genRegToReg()
        // to move the value to lhsReg.

        if (annotateCode)
          appendInstruction(new CommentMarker("genMultiply converting LHS"));
        int lhsSize = registers.doubleRegister(laReg) ? 8 : 4;
        int lhsReg  = registers.newTempRegister(RegisterSet.DBLEREG + RegisterSet.INTREG);
        int lhsDest = convertIntRegValue(laReg /* src */,
                                         lhsSize /* srcSize */,
                                         true /* srcSigned */,
                                         lhsReg /* dest */,
                                         8 /* destSize */, 
                                         true /* destSigned */);
        genRegToReg(lhsDest, lhsReg);

        // Doing the same thing for RHS.

        if (annotateCode)
          appendInstruction(new CommentMarker("genMultiply converting RHS"));
        int rhsSize = registers.doubleRegister(raReg) ? 8 : 4;
        int rhsReg  = registers.newTempRegister(RegisterSet.DBLEREG + RegisterSet.INTREG);
        int rhsDest = convertIntRegValue(raReg /* src */,
                                         rhsSize /* srcSize */,
                                         true /* srcSigned */,
                                         rhsReg /* dest */,
                                         8 /* destSize */,
                                         true /* destSigned */);
        genRegToReg(rhsDest, rhsReg);
        if (annotateCode)
          appendInstruction(new CommentMarker("genMultiply converting RHS ends"));

        // Setting up parameters for the call.

        genRegToReg(lhsReg, PPCRegisterSet.FIA_REG);
        genRegToReg(rhsReg, PPCRegisterSet.FIA_REG + 2);
        genRegToReg(laReg, PPCRegisterSet.FIA_REG);
        genRegToReg(raReg, PPCRegisterSet.FIA_REG + 2);
        short[] uses = new short[5];
        uses[0] = (short) PPCRegisterSet.SP_REG;
        uses[1] = (short) PPCRegisterSet.FIA_REG;
        uses[2] = (short) PPCRegisterSet.FIA_REG + 1;
        uses[3] = (short) PPCRegisterSet.FIA_REG + 2;
        uses[4] = (short) PPCRegisterSet.FIA_REG + 3;

        // genFtnCall makes the call to the function, after which we get
        // back the long long result from (r3, r4).

        genFtnCall("__muldi3", uses, null);
        genRegToReg(PPCRegisterSet.IR_REG, dest);
      }
    } else
      throw new scale.common.NotImplementedError("MUL for " + bs);

    resultReg = dest;
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    Type ct  = processType(e);
    int  bs  = ct.memorySizeAsInt(machine);
    Expr arg = e.getOperand(0);
    int  ir  = registers.getResultRegister(ct.getTag());

    needValue(arg);
    int operandReg = resultReg;

    int opcode;
    if (ct.isRealType()) // FP negation
      appendInstruction(new FDrInstruction(Opcodes.FNEG, ir, operandReg));
    else { // Integer negation
      if (registers.doubleRegister(ir)) {
        if (annotateCode)
          appendInstruction(new CommentMarker("visitNegativeExpr: negation of a long long"));

        // Negation of a long long. We have to do the subtractions of LSBs
        // and MSBs separately, and then move the results to the
        // destination register. The subtraction of the LSBs have to be
        // done first and writing the result directly to the destination
        // register does not work, because the register allocator expects
        // the MSBs to be defined first.

        int msbReg = registers.newTempRegister(RegisterSet.AIREG);
        int lsbReg = registers.newTempRegister(RegisterSet.AIREG);
        // LSB subtraction
        if (annotateCode)
          appendInstruction(new CommentMarker("negate the LSBs"));
        appendInstruction(new FDrcInstruction(Opcodes.SUBFIC, lsbReg, operandReg + 1, 0, macosx));
        // MSB subtraction
        if (annotateCode)
          appendInstruction(new CommentMarker("negate the MSBs"));
        appendInstruction(new FDrInstruction(Opcodes.SUBFZE, msbReg, operandReg));
        // Moving the MSB to the destination register.
        if (annotateCode)
          appendInstruction(new CommentMarker("Move the MSBs to the dest reg"));
        appendInstruction(new FDrInstruction(Opcodes.MR, ir, msbReg));
        // Moving the LSB to the destination register.
        if (annotateCode)
          appendInstruction(new CommentMarker("Move the LSBs to the dest reg"));
        appendInstruction(new FDrInstruction(Opcodes.MR, ir + 1, lsbReg));
        if (annotateCode)
          appendInstruction(new CommentMarker("visitNegativeExpr: negation of a long long (end)"));
      } else // 32 bit integer negation.
        appendInstruction(new FDrInstruction(Opcodes.NEG, ir, operandReg));
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private static final short[] singleIntUse = {PPCRegisterSet.FIA_REG};
  private static final short[] singleFltUse = {PPCRegisterSet.FFA_REG};

  private void genFtnCall(String fname, int dest, int src, Type type)
  {
    boolean flt = registers.floatRegister(src);
    genRegToReg(src, flt ? PPCRegisterSet.FFA_REG : PPCRegisterSet.FIA_REG);
    genFtnCall(fname, flt ? singleFltUse : singleIntUse, null);
    genRegToReg(returnRegister(type.getTag(), true), dest);
  }

  protected void genSqrtFtn(int dest, int src, Type type)
  {
    if (type.isComplexType()) {
      genFtnCall("sqrt", dest, src, type);
      return;
    }

    boolean sa = (type.memorySizeAsInt(machine) <= 4);
    appendInstruction(new FDrInstruction(sa ? Opcodes.FSQRTS : Opcodes.FSQRT, src, dest));
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

  protected void genAlloca(Expr arg, int reg)
  {
    int tr = registers.newTempRegister(RegisterSet.AIREG);
    int tr2 = registers.newTempRegister(RegisterSet.AIREG);

    needValue(arg);
    int src = resultReg;

    appendInstruction(new FDrcInstruction(Opcodes.ADDI, src, src, 15, macosx));
    appendInstruction(new FDrcInstruction(Opcodes.ADDI, tr, src, 15, macosx));
    appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, tr, tr, 28, 4, 31));
    appendInstruction(new FDrcInstruction(Opcodes.SLWI, tr, tr, 4, macosx));
    appendInstruction(new LoadInstruction(Opcodes.LWZ, tr2, PPCRegisterSet.SP_REG, getDisp(0)));
    appendInstruction(new FDrInstruction(Opcodes.NEG, tr, tr));
    appendInstruction(new FDrrInstruction(Opcodes.STWUX, tr2, PPCRegisterSet.SP_REG, tr));
    appendInstruction(new FDrcInstruction(Opcodes.ADDI, tr2, PPCRegisterSet.SP_REG, 64, macosx));
    appendInstruction(new FDrcInstruction(Opcodes.ADDI, tr, tr2, 15, macosx));
    appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, tr, tr, 28, 4, 31));
    appendInstruction(new FDrcInstruction(Opcodes.SLWI, reg, tr, 4, macosx));
  }

  protected void genSignFtn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("Fortran SIGN intrinsic");
  }

  protected void genAtan2Ftn(int dest, int laReg, int raReg, Type rType)
  {
    genRegToReg(laReg, PPCRegisterSet.FFA_REG);
    genRegToReg(raReg, PPCRegisterSet.FFA_REG + 1);
    genFtnCall("atan2", genDoubleUse(PPCRegisterSet.FFA_REG, PPCRegisterSet.FFA_REG + 1), null);
    genRegToReg(PPCRegisterSet.FR_REG, dest);
  }

  protected void genDimFtn(int dest, int laReg, int raReg, Type rType)
  {
    throw new scale.common.NotImplementedError("Fortran DIM intrinsic");
  }

  public void visitNotExpr(NotExpr e)
  {
    Expr arg = e.getOperand(0);
    int  ir  = registers.getResultRegister(RegisterSet.INTREG);

    needValue(arg);
    int src = resultReg;

    if (registers.floatRegister(src))
      throw new scale.common.InternalError("Not not allowed on " + arg);

    appendInstruction(new FDcInstruction(Opcodes.LI, ir, 1));

    Label             labt   = createLabel();
    Label             labf   = createLabel();
    LabelDisplacement tdisp  = new LabelDisplacement(labt);
    Branch            brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_FALSE, Opcodes.EQ, tdisp, 1);

    brInst.addTarget(labt, 0);
    brInst.addTarget(labf, 0);

    appendInstruction(new FDcInstruction(Opcodes.CMPWI, src, 0));
    appendInstruction(brInst);
    appendLabel(labf);
    labf.setNotReferenced();
    appendInstruction(new FDcInstruction(Opcodes.LI, ir, 0));
    appendLabel(labt);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitReturnChord(ReturnChord c)
  {
    Expr    a    = c.getResultValue();
    short[] uses = retUses;

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
        uses = new short[nr + retUses.length];
        for (int i = 0; i < nr; i++)
          uses[i] = (short) (fr + i);
        for (int i = 0; i < retUses.length; i++)
          uses[nr + i] = retUses[i];
        genRegToReg(resultReg, dest);
      }
    } else
      uses = new short[usesAlloca ? 4 : 3];


    returnInst = lastInstruction;
    BFormInstruction inst = new BFormInstruction(Opcodes.BCLR, Opcodes.BRANCH_ALWAYS, 0, 0);
    inst.additionalRegsUsed(uses); // Specify which registers are used.
    appendInstruction(inst);
  }

  /**
   * Store a value in a register to a symbolic location in memory.
   * @param src is the value
   * @param dsize is the size of the value in addressable memory units
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param isReal is true if the value in the register is a floating point
   * value
   * @param disp specifies the location
   */
  protected void storeRegToSymbolicLocation(int          src,
                                            int          dsize,
                                            long         alignment,
                                            boolean      isReal,
                                            Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

      int dest = loadMemoryAddress(disp);
      storeIntoMemory(src, dest, getDisp(0), FT_NONE, dsize, alignment, isReal);
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
    long       srcrs  = resultRegSize;

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

      moveWords(src, srcoff, adr, fieldOffset, byteSize, aln);
      resultRegAddressOffset = 0;
      resultRegMode = srcha;
      resultReg = src;
      resultRegAddressAlignment = srcaln;
      return;
    }

    needValue(src, srcoff, srcha);
    src = resultReg;

    if (adrha == ResultMode.STRUCT_VALUE) {
      assert (fieldOffset < 16) : "Field offset too large " + fieldOffset;
        // Structure is in registers, not memory, and is 16 bytes or
        // less in size.
        if (ft.isRealType())
          // Assume entire struct is in floating point registers and
          // contains only doubles because there is no easy transfer
          // between integer and double registers and there is no
          // conversion from 32-bit single to 64-bit double format that
          // does not go through memory.
          throw new scale.common.NotImplementedError("storeLfae - floating point value struct in register");

        if (fieldOffset >= 4) {
          if (fieldOffset >= 8) {
            if (fieldOffset >= 12) {
              fieldOffset -= 12;
              adr += 3;
            } else {
              fieldOffset -= 8;
              adr += 2;
            }
          } else {
            fieldOffset -= 4;
            adr++;
          }
        }

        bitOffset += 8 * ((int) fieldOffset);
        if (bits == 0)
          bits = 8 * byteSize;

        if (bits == 32) {
          appendInstruction(new FDrInstruction(Opcodes.MR, adr, src));
          resultRegAddressOffset = srcoff;
          resultRegMode = srcha;
          resultReg = src;
          resultRegSize = srcrs;
          return;
        }

        long mk   = (1L << bits) - 1;
        long mask = (mk << bitOffset);
        int span = bits + bitOffset;

        if (span <= 32)
          appendInstruction(new FDrcccInstruction(Opcodes.RLWIMI, adr, src, 32 - span, bitOffset, span - 1));
        else
          throw new scale.common.NotImplementedError("storeBitsIntoMemory");

        resultRegAddressOffset = srcoff;
        resultRegMode = srcha;
        resultReg = src;
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

      storeIntoMemory(src, adr, disp, FT_NONE, byteSize, alignment, ft.isRealType());
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
    storeBitsIntoMemory(src, adr, getDisp(0), FT_NONE, bits, bitOffset, alignment);
    appendInstruction(new FDrcInstruction(Opcodes.SLWI, ir, src, 32 - bits, macosx));

    if (ft.isSigned())
      appendInstruction(new FDrcInstruction(Opcodes.SRAWI, ir, ir, 32 - bits, macosx));
    else
      appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, ir, ir, bits, 32 - bits, 31));

    resultRegAddressOffset = srcoff;
    resultRegMode = srcha;
    resultReg = ir;
    resultRegSize = srcrs;
  }

  protected boolean genSwitchUsingIfs(int     testReg,
                                      Chord[] cases,
                                      long[]  keys,
                                      int     num,
                                      long    spread)
  {
    return false;
  }

  protected void genSwitchUsingTransferVector(int     testReg,
                                              Chord[] cases,
                                              long[]  keys,
                                              Label   labt,
                                              long    min,
                                              long    max)
  {
    int               tmp    = registers.newTempRegister(RegisterSet.AIREG);
    int               adr    = registers.newTempRegister(RegisterSet.AIREG);
    Label             labf   = createLabel();
    Label             labf2  = createLabel();
    LabelDisplacement dispt2 = new LabelDisplacement(labt);
    LabelDisplacement dispt  = new LabelDisplacement(labt);
    Branch            insti  = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_TRUE, Opcodes.GT, dispt, 2);
    Branch            insti2 = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_TRUE, Opcodes.LT, dispt2, 2);
    Branch            instj  = new BFormInstruction(Opcodes.BCCTR, Opcodes.BRANCH_ALWAYS, 0, cases.length);
    Displacement      disp   = createAddressTable(cases, keys, (int) min, (int) max);

    insti.addTarget(labt, 0);
    insti.addTarget(labf, 1);
    insti2.addTarget(labt, 0);
    insti2.addTarget(labf2, 1);

    for (int i = 0; i < cases.length; i++)
      instj.addTarget(getBranchLabel(cases[i]), i);

    // Comparision are with respect to the lowest case value.

    appendInstruction(new FDrcInstruction(Opcodes.ADDI, tmp, testReg, (int) (-min), macosx));
    appendInstruction(new FrcInstruction (Opcodes.CMPWI, tmp, max-min));
    appendInstruction(insti);
    appendLabel(labf);
    labf.setNotReferenced();
    appendInstruction(new FrcInstruction(Opcodes.CMPWI, tmp, 0L));
    appendInstruction(insti2);
    appendLabel(labf2);
    labf2.setNotReferenced();
    genMultiplyLit(4, tmp, adr, PPCG4RegisterSet.IREG_SIZE);
    int tr = loadMemoryAddress(disp);
    appendInstruction(new FDrrInstruction(Opcodes.ADD, adr, adr, tr));
    appendInstruction(new LoadInstruction(Opcodes.LWZ, tmp, adr, getDisp(0)));
    appendInstruction(new FcrInstruction(Opcodes.MTSPR, Opcodes.CTR_SPR, tmp));
    appendInstruction(instj);
  }

  private void macosxVisitVaStartExpr(VaStartExpr e)
  {
    FormalDecl    parmN  = e.getParmN();
    Expr          vaList = e.getVaList();
    int           pr     = registers.getResultRegister(RegisterSet.ADRREG);
    int           or     = registers.getResultRegister(RegisterSet.INTREG);
    ProcedureType pt     = (ProcedureType) processType(currentRoutine);
    Type          rt     = processType(pt.getReturnType());
    long          offset = ARG_SAVE_OFFSET_MACOSX;
    int           nfa    = 0;
    int           nia    = 0;
    int           l      = pt.numFormals();

    if (!(rt.isAtomicType() || rt.isVoidType())) {
      nia++;
      offset += PPCG4RegisterSet.IREG_SIZE;
    }

    Label labStart = createLabel();

    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      Type       ft = fd.getCoreType();
      long    size  = ft.memorySize(machine);
      if (size < 4)
        size = 4;

      if (ft.isRealType()) {
        nfa++;
        offset += size;
      } else {
        nia++;
        offset += size;
      }

      if (fd == parmN) {
        vaList.visit(this);

        int  adr  = resultReg;
        long aoff = resultRegAddressOffset;

        StackDisplacement disp = new StackDisplacement(offset);
        argDisp.addElement(disp);
        appendInstruction(new FDdrInstruction(Opcodes.LA,
                                              pr,
                                              PPCRegisterSet.SP_REG,
                                              disp,
                                              FT_NONE,
                                              macosx));
        storeIntoMemoryWithOffset(pr,
                                  adr,
                                  aoff,
                                  PPCG4RegisterSet.IREG_SIZE,
                                  PPCG4RegisterSet.IREG_SIZE,
                                  false);
//         storeIntoMemory(pr,
//                         adr,
//                         new StackDisplacement(0),
//                         FT_NONE,
//                         PPCG4RegisterSet.IREG_SIZE,
//                         PPCG4RegisterSet.IREG_SIZE,
//                         false);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = pr;
        return;
      }
    }

    throw new scale.common.InternalError("Parameter not found " + parmN);
  }

  /*
   * Sets overflowArgArea, which will be adjusted in endRoutineCode
   */
  private void linuxVisitVaStartExpr(VaStartExpr e)
  {
    if (annotateCode)
      appendInstruction(new CommentMarker("linuxVisitVaStartExpr"));
    FormalDecl    parmN         = e.getParmN();
    Expr          vaList        = e.getVaList();
    ProcedureType pt            = (ProcedureType) processType(currentRoutine);
    Type          rt            = processType(pt.getReturnType());
    int           numFormals    = pt.numFormals();
    int           nextArgG      = 0;
    int           nextArgF      = 0;
    int           offset        = ARG_SAVE_OFFSET_LINUX;

    // Just for a sanity check, to see that formal parmeter specified
    // with the va_start expression matches the function definition.

    boolean       formalMatched = false;

    // The fields of va_list have to be filled appropriately. We have
    // to go through the whole parameter passing algorithm in the ABI
    // again.

    // If this function returns a struct, then, register r3 will
    // contain the address of the struct we have to fill.

    if (!rt.isAtomicType() && !rt.isVoidType())
      nextArgG++;

    // For each formal parameter, see where it would have been passed
    // (in register or stack).

    for (int i = 0; i < numFormals; i++) {
      FormalDecl fd    = pt.getFormal(i);
      Type       vt    = processType(fd);
      long       size  = vt.memorySize(machine);
      if (fd instanceof UnknownFormals)
        // We should have gone out of this loop before we reach
        // ellipsis, because the parameter passed with va_start will
        // match one of the formal parameters before the
        // ellipsis. There is a check at the end of this loop to check
        // if the formal parameter of this iteration matches the
        // formal parameter specified in the va_start expression.
        throw new scale.common.InternalError("fd is ...");

      if (vt.isAtomicType()) {
        // This is an atomic type. This will be passed in a register
        // if a register is available, or will be passed in stack.

        boolean isReal     = vt.isRealType();
        boolean isLongLong = !isReal && (64 == ((AtomicType) vt).bitSize());
        if ((isReal  &&                (nextArgF >= MAX_ARG_REGS )) ||
            (!isReal && !isLongLong && (nextArgG >= MAX_ARG_REGS )) ||
            (!isReal &&  isLongLong && (nextArgG >= (MAX_ARG_REGS - 1)))) // The parameter is available on stack.
          offset += Machine.alignTo(size, isReal ? PPCG4RegisterSet.FREG_SIZE : PPCG4RegisterSet.IREG_SIZE);
        else { // The parameter is available in a register
          if (isReal)
            nextArgF++;
          else {
            if (isLongLong) {
              if ((nextArgG % 2) == 1) {
                nextArgG++;
              }
              nextArgG += 2;
            } else {
              nextArgG++;
            }
          }
        }
      } else if (vt.isAggregateType()) {
        if (nextArgG < MAX_ARG_REGS)
          nextArgG++;
        else
          throw new scale.common.NotImplementedError("Aggregate in stack");
      } else
        throw new scale.common.NotImplementedError("No other formal parameter type has been taken care of");

      // If this is the parameter passed with the va_start,
      // i.e. va_start(list, parmN), then we have to save the offsets
      // of the parameters into list. See @setVaListType for the
      // structure of list.

      if (fd == parmN) {
        formalMatched = true;
        break;
      }
    }

    // At this point, we have reached the formal parameter specified
    // in the va_start expression.

    if (!formalMatched)
      throw new scale.common.InternalError("! formalMatched");

    // Getting the address of the va_list into the register
    // _va_list_address.

    needValue(vaList);
    int vaListAddress = resultReg;

    // The code below fills the fields of va_list (see @setVaListType) with
    // appropriate values.

    // offset 0 list.gpr             = nextArgG;
    // offset 1 list.fpr             = nextArgF;
    // offset 4 list.overflowArgArea = offset;
    // offset 8 list.regSaveArea     = vaArgStart;

    // list.gpr = nextArgG;

    if (annotateCode)
      appendInstruction(new CommentMarker("Setting gpr: " + nextArgG));

    int gprReg = registers.newTempRegister(RegisterSet.INTREG);
    genLoadImmediate((long) nextArgG, gprReg);
    storeIntoMemory(/* src       */ gprReg,
                    /* address   */ vaListAddress,
                    /* disp      */ getDisp(0),
                    /* dftn      */ FT_NONE,
                    /* size      */ 1,
                    /* alignment */ 1,
                    /* real      */ false);

    // list.fpr = nextArgF;

    if (annotateCode)
      appendInstruction(new CommentMarker("Setting fpr " + nextArgF));

    int fprReg = registers.newTempRegister(RegisterSet.INTREG);
    genLoadImmediate((long) nextArgF, fprReg);
    storeIntoMemory(/* src       */ fprReg,
                    /* address   */ vaListAddress,
                    /* disp      */ getDisp(1),
                    /* dftn      */ FT_NONE,
                    /* size      */ 1,
                    /* alignment */ 1,
                    /* real      */ false);

    // list.overflowArgArea = offset;

    if (annotateCode)
      appendInstruction(new CommentMarker("Setting overflowArgArea"));

    overflowArgArea = new StackDisplacement(offset);
    int offsetReg = loadStackAddress(overflowArgArea);
    storeIntoMemory(/* src       */ offsetReg,
                    /* address   */ vaListAddress,
                    /* disp      */ getDisp(4),
                    /* dftn      */ FT_NONE,
                    /* size      */ 4,
                    /* alignment */ 4,
                    /* real      */ false);

    // list.regSaveArea = vaArgStart

    if (annotateCode)
      appendInstruction(new CommentMarker("Setting regSaveArea"));

    int regsaveReg = loadStackAddress(vaArgStart);
    storeIntoMemory(/* src       */ regsaveReg,
                    /* address   */ vaListAddress,
                    /* disp      */ getDisp(8),
                    /* dftn      */ FT_NONE,
                    /* size      */ 4,
                    /* alignment */ 4,
                    /* real      */ false);

    if (annotateCode)
      appendInstruction(new CommentMarker("End linuxVisitVaStartExpr"));
  }

  /**
   * Fill the fields of the va_start with appropriate values.
   */
  public void visitVaStartExpr(VaStartExpr e)
  {
    if (macosx)
      macosxVisitVaStartExpr(e);
    else
      linuxVisitVaStartExpr(e);
  }

  /**
   * Process a va_arg expression.
   */
  public void visitVaArgExpr(VaArgExpr e)
  {
    if (macosx)
      macosxVisitVaArgExpr(e);
    else
      linuxVisitVaArgExpr(e);
  }

  private void macosxVisitVaArgExpr(VaArgExpr e)
  {
    Expr  vaList = e.getVaList();
    Type  ct     = processType(e);
    int   rr;
    int   ir     = registers.newTempRegister(RegisterSet.INTREG);
    int   adr    = registers.newTempRegister(RegisterSet.ADRREG);
    int   bs     = ct.memorySizeAsInt(machine);

    if (vaList instanceof LoadDeclAddressExpr) { // Generate better code by avoiding the visit.
      VariableDecl val  = (VariableDecl) ((LoadDeclAddressExpr) vaList).getDecl();
      Type         valt = processType(val);

      if (val.getStorageLoc() == Assigned.ON_STACK) {
        Displacement disp = val.getDisplacement();
        int          incs = valt.memorySizeAsInt(machine);

        loadFromMemoryWithOffset(adr,
                                 stkPtrReg,
                                 disp,
                                 4,
                                 machine.stackAlignment(valt),
                                 false,
                                 false);

        appendInstruction(new FDrInstruction(Opcodes.MR, ir, adr));

        if (ct.isAtomicType()) {
          appendInstruction(new FDrcInstruction(Opcodes.ADDI, ir, ir, bs, macosx));
          if (ct.isRealType()) {
            int or = registers.getResultRegister(RegisterSet.INTREG);
            rr = registers.getResultRegister(ct.getTag());
            loadFromMemory(rr, adr, getDisp(0), bs, naln ? 1 : bs, false);
            appendInstruction(new FDrcInstruction(Opcodes.ADDI, adr, adr, bs, macosx));
            storeIntoMemoryWithOffset(adr,
                                      stkPtrReg,
                                      disp,
                                      PPCG4RegisterSet.IREG_SIZE,
                                      machine.stackAlignment(valt),
                                      false);
            resultRegMode = ResultMode.NORMAL_VALUE;
          } else {
            rr = registers.getResultRegister(ct.getTag());
            loadFromMemory(rr, adr, getDisp(0), bs, naln ? 1 : bs, false);
            appendInstruction(new FDrcInstruction(Opcodes.ADDI,
                                                  adr,
                                                  adr,
                                                  Math.max(bs,
                                                           PPCG4RegisterSet.IREG_SIZE),
                                                  macosx));
            storeIntoMemoryWithOffset(adr,
                                      stkPtrReg,
                                      disp,
                                      PPCG4RegisterSet.IREG_SIZE,
                                      machine.stackAlignment(valt),
                                      false);
            resultRegMode = ResultMode.NORMAL_VALUE;
          }
        } else {
          int inc = (bs + PPCG4RegisterSet.IREG_SIZE - 1) & ~(PPCG4RegisterSet.IREG_SIZE - 1);
          appendInstruction(new FDrcInstruction(Opcodes.ADDI, ir, ir, inc, macosx));
          rr = registers.getResultRegister(ct.getTag());
          appendInstruction(new FDrInstruction(Opcodes.MR, rr, adr));
          storeIntoMemoryWithOffset(ir, stkPtrReg, disp, 4, machine.stackAlignment(valt), false);
          resultRegAddressAlignment = naln ? 1 : 8;
          resultRegMode = ResultMode.ADDRESS;
        }

        resultRegAddressOffset = 0;
        resultReg = rr;
        return;
      }
    }

    vaList.visit(this);
    int  vr    = resultReg;
    long vroff = resultRegAddressOffset;

    int soffset = 0;
    if ((vroff >= (MAX_IMM16 - PPCG4RegisterSet.IREG_SIZE)) ||
        (vroff <= (MIN_IMM16 + PPCG4RegisterSet.IREG_SIZE))) {
      int sr = registers.newTempRegister(RegisterSet.INTREG);
      genLoadImmediate(vroff, vr, sr);
      vr = sr;
    } else
      soffset = (int) vroff;

    loadFromMemory(adr,
                   vr,
                   getDisp(soffset),
                   PPCG4RegisterSet.IREG_SIZE,
                   naln ? 1 : PPCG4RegisterSet.IREG_SIZE,
                   false);
    appendInstruction(new FDrInstruction(Opcodes.MR, ir, adr));

    if (ct.isAtomicType()) {
      appendInstruction(new FDrcInstruction(Opcodes.ADDI, ir, ir, bs, macosx));
      if (ct.isRealType()) {
        int or = registers.getResultRegister(RegisterSet.INTREG);
        rr = registers.getResultRegister(ct.getTag());
        storeIntoMemory(ir,
                        vr,
                        getDisp(soffset),
                        FT_NONE,
                        PPCG4RegisterSet.IREG_SIZE,
                        PPCG4RegisterSet.IREG_SIZE,
                        false);
        loadFromMemory(rr, adr, getDisp(0), bs, naln ? 1 : bs, false);
        resultRegMode = ResultMode.NORMAL_VALUE;
      } else {
        rr = registers.getResultRegister(ct.getTag());
        loadFromMemory(rr, adr, getDisp(0), bs, naln ? 1 : bs, ct.isSigned());
        storeIntoMemory(ir,
                        vr,
                        getDisp(soffset),
                        FT_NONE,
                        PPCG4RegisterSet.IREG_SIZE,
                        PPCG4RegisterSet.IREG_SIZE,
                        false);
        resultRegMode = ResultMode.NORMAL_VALUE;
      }
    } else {
      int inc = (bs + PPCG4RegisterSet.IREG_SIZE - 1) & ~(PPCG4RegisterSet.IREG_SIZE - 1);
      appendInstruction(new FDrcInstruction(Opcodes.ADDI, ir, ir, inc, macosx));
      rr = registers.getResultRegister(ct.getTag());
      appendInstruction(new FDrInstruction(Opcodes.MR, rr, adr));
      storeIntoMemory(ir,
                      vr,
                      getDisp(soffset),
                      FT_NONE,
                      PPCG4RegisterSet.IREG_SIZE,
                      PPCG4RegisterSet.IREG_SIZE,
                      false);
      resultRegAddressAlignment = naln ? 1 : 8;
      resultRegMode = ResultMode.ADDRESS;
    }

    resultRegAddressOffset = 0;
    resultReg = rr;
  }

  /**
   * Process a va_arg expression for the linux backend.
   */
  private void linuxVisitVaArgExpr(VaArgExpr e)
  {
    Expr vaList = e.getVaList();
    Type type = processType(e);

    if (annotateCode)
      appendInstruction(new CommentMarker("linuxVisitVaArgExpr: type: " + type));

    // Get the address of the va_list into register vaListAddress.

    needValue(vaList);
    int vaListAddress = resultReg;

    // size is the number of bytes to read from the regSaveArea or
    // overflowArgArea.

    int size;
    if (type.isAtomicType())
      size = type.memorySizeAsInt(machine);
    else if (type.isAggregateType())
      // FIXME: Replace this with the size of a pointer. Bad practice to hardcode this as 4.
      size = 4;
    else
      throw new scale.common.NotImplementedError("Type: " + type);

    // Based on the type of the parameter and the possiblilty that it could
    // have been passed in a register, get the parameter either from the
    // register save area or from the parameters area (in the callee's
    // stack). This is what the generated code looks like
    //       Check which area
    //       if in regSaveArea jump to inRegSaveArea
    // in_overflowArgArea:
    //       load from overflowArgArea
    //       jump to va_arg_complete
    // inRegSaveArea:
    //       load from regSaveArea
    // va_arg_complete:

    Label   inRegSaveArea     = createLabel();
    Label   inOverflowArgArea = createLabel();
    Label   vaArgComplete     = createLabel();
    boolean isReal            = type.isRealType();
    boolean isLongLong        = (type.isAtomicType() &&
                                 !isReal &&
                                 (64 == ((AtomicType) type).bitSize()));
    int     indexReg          = registers.newTempRegister(RegisterSet.INTREG);

    // Decide which area (regSaveArea or the overflowArgArea)
    // contains this parameters. Branch to the corresponding area.

    linuxVisitVaArgExprDetermineArea(type,
                                     isReal,
                                     isLongLong,
                                     indexReg,
                                     vaListAddress,
                                     inRegSaveArea,
                                     inOverflowArgArea);

    // Create a register to store the data.

    int dataRegType;
    if (isReal)
      dataRegType = RegisterSet.FLTREG;
    else {
      dataRegType = RegisterSet.INTREG;
      dataRegType += (isLongLong ? RegisterSet.DBLEREG : 0);
    }

    int dataReg = registers.newTempRegister(dataRegType);

    // Load from the over flow area.

    linuxVisitVaArgExprLoadFromOverflowArea(type,
                                            size,
                                            isReal,
                                            isLongLong,
                                            vaListAddress,
                                            dataReg);

    // After loading, jump to vaArgComplete.

    LabelDisplacement vaArgCompleteDisp = new LabelDisplacement(vaArgComplete);
    Branch            br                 = new BFormInstruction(Opcodes.BC,
                                                                Opcodes.BRANCH_ALWAYS,
                                                                0,
                                                                vaArgCompleteDisp,
                                                                1);

    br.addTarget(vaArgComplete, 0);
    appendInstruction(br);

    // Else, load from the register save area.

    appendLabel(inRegSaveArea);
    linuxVisitVaArgExprLoadFromRegisterArea( type,
                                             size,
                                             isReal,
                                             isLongLong,
                                             vaListAddress,
                                             indexReg,
                                             dataReg);

    // Irrespective of whether the load was from the register save
    // area, or the overflow area, come here.

    appendLabel(vaArgComplete);
    if (annotateCode)
      appendInstruction(new CommentMarker("end linuxVisitVaArgExpr"));

    // Finally setting resultReg to dataReg.

    resultReg = dataReg;
    if (type.isAtomicType())
      resultRegMode = ResultMode.NORMAL_VALUE;
    else if (type.isAggregateType()) {
      resultRegMode = ResultMode.ADDRESS;
      resultRegAddressOffset = 0;
    } else
      throw new scale.common.NotImplementedError("Type: " + type);
  }

  /**
   * In va_arg, determine which area (regSaveArea or overflowArgArea)
   * to load the value from. See @setVaListType
   * @param type The type specified in the va_arg expression
   * @param isReal Whether the type is a real type
   * @param isLongLong Whether the type is "(un)signed long long"
   * @param index_reg When this function returns, index_reg will contain
   * va_list.gpr (or fpr). If the variable has to be loaded from the
   * regSaveArea, then index_reg is the index pointing into this area
   * @param va_list_address Register containing the address of va_list
   * structure
   * @param inRegSaveArea The label to jump to, if the variable has to be
   * loaded from the regSaveArea area
   * @param inOverflowArgArea The label to jump to, if the variable has
   * to be loaded from the overflowArgArea
   */
  private void linuxVisitVaArgExprDetermineArea(Type    type,
                                                boolean isReal,
                                                boolean isLongLong,
                                                int     indexReg,
                                                int     vaListAddress,
                                                Label   inRegSaveArea,
                                                Label   inOverflowArgArea)
  {
    // Get va_list.fpr (or va_list.gpr for integer types) into a
    // register, compare it with to MAX_ARG_REGS (or (MAX_ARG_REGS - 1)
    // for long long, since a long long needs two registers) and branch
    // accordingly.

    // if (va_list.fpr < MAX_ARG_REGS) {
    //    Get the data from the regSaveArea;
    // } else {
    //    Get the data from the overflowArgArea;
    // }

    loadFromMemory(/* dest      */ indexReg,
                   /* address   */ vaListAddress,
                   /* disp      */ getDisp(isReal ? 1 : 0),
                   /* dftn      */ FT_NONE,
                   /* size      */ 1,
                   /* alignment */ 1,
                   /* signed    */ false);

    int maxargReg = registers.newTempRegister(RegisterSet.INTREG);
    genLoadImmediate(/* value */ (long) (isLongLong ? (MAX_ARG_REGS - 1) : MAX_ARG_REGS),
                     /* dest  */ maxargReg);

    int differenceReg = registers.newTempRegister(RegisterSet.INTREG);

    doBinaryOp(/* dest      */ differenceReg,
               /* laReg     */ indexReg,
               /* raReg     */ maxargReg,
               /* size      */ 4,
               /* isReal    */ false,
               /* isSigned  */ true,
               /* which     */ SUB);

    genIfRegister(/* which  */ CompareMode.LT,
                  /* treg   */ differenceReg,
                  /* signed */ true,
                  /* labt   */ inOverflowArgArea,
                  /* labf   */ inRegSaveArea);
  }

  /**
   * In a va_arg expression, load the variable from overflowArgArea (see
   * @setVaListType)
   *
   * @param type The type of the variable to be loaded
   * @param size The number of bytes to read from the overflowArgArea
   * @param isReal Whether the type is a real type
   * @param isLongLong Whether the type is "(un)signed long long"
   * @param vaListAddress Register containing the address of va_list
   * structure
   * @param dataReg The register into which the data should be loaded
   */
  private void linuxVisitVaArgExprLoadFromOverflowArea(Type    type,
                                                       int     size,
                                                       boolean isReal,
                                                       boolean isLongLong,
                                                       int     vaListAddress,
                                                       int     dataReg)
  {
    if (annotateCode)
      appendInstruction(new CommentMarker("inOverflowArgArea"));

    // Load the address va_list.overflowArgArea into addrReg.

    int addrReg = registers.newTempRegister(RegisterSet.ADRREG);
    loadFromMemory(/* dest      */ addrReg,
                   /* address   */ vaListAddress,
                   /* disp      */ getDisp(4),
                   /* dftn      */ FT_NONE,
                   /* size      */ 4,
                   /* alignment */ 4,
                   /* signed    */ false);

    // FIXME: alignment has always been an issue. The code below is
    // incorrect.

    int alignment = isReal ? 8 : 4;

    // Now, get the data from the overflowArgArea.

    loadFromMemory(/* dest      */ dataReg,
                   /* address   */ addrReg,
                   /* disp      */ getDisp(0),
                   /* dftn      */ FT_NONE,
                   /* size      */ size,
                   /* alignment */ alignment,
                   /* signed    */ true);

    // We have to change va_list.overflowArgArea to point to the
    // next parameter..

    int  incrementReg = registers.newTempRegister(RegisterSet.INTREG);
    long increment    = Machine.alignTo(size, PPCG4RegisterSet.IREG_SIZE);

    genLoadImmediate(increment, incrementReg);

    doBinaryOp(/* dest     */ addrReg,
               /* laReg    */ addrReg,
               /* raReg    */ incrementReg,
               /* size     */ 4,
               /* isReal   */ false,
               /* isSigned */ false,
               /* which    */ ADD);

    storeIntoMemory(/* src       */ addrReg,
                    /* address   */ vaListAddress,
                    /* disp      */ getDisp(4),
                    /* dftn      */ FT_NONE,
                    /* size      */ 4,
                    /* alignment */ 4,
                    /* real      */ false);
  }

  /**
   * In a va_arg expression, load the variable from regSaveArea (see
   * @setVaListType)
   *
   * @param type The type of the variable to be loaded
   * @param isReal Whether the type is a real type
   * @param isLongLong Whether the type is "(un)signed long long"
   * @param vaListAddress Register containing the address of va_list
   * structure
   * @param indexReg Register containing the index into the regSaveArea
   * @param dataReg The register into which the data should be loaded
   */
  private void linuxVisitVaArgExprLoadFromRegisterArea(Type    type,
                                                       int     size,
                                                       boolean isReal,
                                                       boolean isLongLong,
                                                       int     vaListAddress,
                                                       int     indexReg,
                                                       int     dataReg)
  {
    if (annotateCode)
      appendInstruction(new CommentMarker("inRegSaveArea"));

    // 1. See the ABI.

    if (isLongLong) {
      // We can optimize
      //   if (indexReg is odd) {
      //      indexReg ++;
      //   }
      // by doing the following
      //   lsbReg = LSBit of indexReg
      //   indexReg = indexReg + lsbReg

      int labReg = registers.newTempRegister(RegisterSet.INTREG);

      // The following instruction gets the LSBit.

      appendInstruction(new FDrcccInstruction(Opcodes.RLWINM, labReg, indexReg, 0, 31, 31));
      doBinaryOp(/* dest     */ indexReg,
                 /* laReg    */ indexReg,
                 /* raReg    */ labReg,
                 /* size     */ 4,
                 /* isReal   */ false,
                 /* isSigned */ false,
                 /* which    */ ADD);
    }

    // The offset from regSaveArea is
    //      1. indexReg * IREG_SIZE for an integer argument
    //      2. (8 * IREG_SIZE) + (indexReg * FREG_SIZE) for a real
    //      argument

    int offsetReg = registers.newTempRegister(RegisterSet.ADRREG);
    if (isReal) {
      genMultiplyLit(/* value */ (long) PPCG4RegisterSet.FREG_SIZE,
                     /* src   */ indexReg,
                     /* dest  */ offsetReg,
                     /* bs    */ 4);
      int const32 = registers.newTempRegister(RegisterSet.ADRREG);
      genLoadImmediate(MAX_ARG_REGS * PPCG4RegisterSet.IREG_SIZE, const32);
      doBinaryOp(/* dest     */ offsetReg,
                 /* laReg    */ offsetReg,
                 /* raReg    */ const32,
                 /* size     */ 4,
                 /* isReal   */ false,
                 /* isSigned */ false,
                 /* which    */ ADD);
    } else {
      genMultiplyLit(/* value */ (long) PPCG4RegisterSet.IREG_SIZE,
                     /* size  */ indexReg,
                     /* dest  */ offsetReg,
                     /* bs    */ 4);
    }

    // The address is regSaveArea + offsetReg.

    int addrReg = registers.newTempRegister(RegisterSet.ADRREG);
    loadFromMemory(/* dest      */ addrReg,
                   /* address   */ vaListAddress,
                   /* disp      */ getDisp(8),
                   /* dftn      */ FT_NONE,
                   /* size      */ 4,
                   /* alignment */ 4,
                   /* signed    */ false);

    doBinaryOp(/* dest     */ addrReg,
               /* laReg    */ addrReg,
               /* raReg    */ offsetReg,
               /* size     */ 4,
               /* isReal   */ false,
               /* isSigned */ false,
               /* which    */ ADD);

    // Now, get the data from the register save area. The alignment in
    // the loadFromMemory call below is 8, because the regSaveArea
    // has this alignment. See @linuxGenerateProlog.

    loadFromMemory(/* dest      */ dataReg,
                   /* address   */ addrReg,
                   /* disp      */ getDisp(0),
                   /* dftn      */ FT_NONE,
                   /* size      */ size,
                   /* alignment */ 8,
                   /* signed    */ true);

    // We have to adjust the index into the regSaveArea in va_list
    // If this is of type real,   va_list.fpr = indexReg + 1;
    // If this of type long long, va_list.gpr = indexReg + 2;
    // If this is of type int,    va_list.gpr = indexReg + 1;

    int incrementReg = registers.newTempRegister(RegisterSet.INTREG);
    genLoadImmediate((long) (isLongLong ? 2 : 1), incrementReg);
    doBinaryOp(/* dest     */ indexReg,
               /* laReg    */ indexReg,
               /* raReg    */ incrementReg,
               /* size     */ 4,
               /* isReal   */ false,
               /* isSigned */ false,
               /* which    */ ADD);

    storeIntoMemory(/* src       */ indexReg,
                    /* address   */ vaListAddress,
                    /* disp      */ getDisp(isReal ? 1 : 0),
                    /* dftn      */ FT_NONE,
                    /* size      */ 1,
                    /* alignment */ 1,
                    /* real      */ false);
  }

  /**
   * Generate code for a va_copy().
   */
  private void macosxDoVaCopy(Expr dst, Expr src)
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
                             PPCG4RegisterSet.IREG_SIZE,
                             PPCG4RegisterSet.IREG_SIZE,
                             false,
                             false);
    storeIntoMemoryWithOffset(pr,
                              dr,
                              droff,
                              PPCG4RegisterSet.IREG_SIZE,
                              PPCG4RegisterSet.IREG_SIZE,
                              false);

    resultRegAddressOffset = sroff;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = sr;
  }

  /**
   * Generate code for a va_copy().
   */
  private void linuxDoVaCopy(Expr dest, Expr src)
  {
    needValue(dest);
    int  destReg    = resultReg;
    long destOffset = resultRegAddressOffset;

    needValue(src);
    int  srcReg    = resultReg;
    long srcOffset = resultRegAddressOffset;

    moveWords(/* src     */ srcReg,
              /* srcoff  */ srcOffset,
              /* dest    */ destReg,
              /* destoff */ destOffset,
              /* size    */ 12,
              /* aln     */ 4);

    resultRegAddressOffset = srcOffset;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = srcReg;
  }

  /**
   * Generate code for a va_copy().
   */
  protected void doVaCopy(Expr dst, Expr src)
  {
    if (macosx)
      macosxDoVaCopy(dst, src);
    else 
      linuxDoVaCopy(dst, src);
  }

  public int getMaxAreaIndex()
  {
    return TEXT;
  }

  protected void addRegs(int laReg, int raReg, int dest)
  {
    throw new scale.common.NotImplementedError("addRegs");
  }

  public void assemblePIC(Emit emit)
  {
    int              funcCounter = 0;
    Iterator<String> iter        = calledFuncs.iterator();
    
    while (iter.hasNext()) {
      String onFunc  = iter.next();
      String lazyPtr = "L_" + onFunc + "$lazyPtr";
      String fcs     = Integer.toString(funcCounter);

      emit.endLine();

      emit.emit(".data");
      emit.endLine();

      emit.emit(".section __TEXT,__picsymbolstub1,symbol_stubs,pure_instructions,32");
      emit.endLine();

      emit.emit("\t.align 2");
      emit.endLine();

      emit.emit("L_");
      emit.emit(onFunc);
      emit.emit("$stub:");
      emit.endLine();

      emit.emit("\t.indirect_symbol _");
      emit.emit(onFunc);
      emit.endLine();

      emit.emit("\tmflr r0");
      emit.endLine();
      emit.emit("\tbcl 20,31,L");
      emit.emit(fcs);
      emit.emit("$_");
      emit.emit(onFunc);
      emit.endLine();

      emit.emit("L");
      emit.emit(fcs);
      emit.emit("$_");
      emit.emit(onFunc);
      emit.emit(":");
      emit.endLine();

      emit.emit("\tmflr r11");
      emit.endLine();

      emit.emit("\taddis r11,r11,ha16(");
      emit.emit(lazyPtr);
      emit.emit("-L");
      emit.emit(fcs);
      emit.emit("$_");
      emit.emit(onFunc);
      emit.emit(")");
      emit.endLine();

      emit.emit("\tmtlr r0");
      emit.endLine();

      emit.emit("\tlwzu r12,lo16(");
      emit.emit(lazyPtr);
      emit.emit("-L");
      emit.emit(fcs);
      emit.emit("$_");
      emit.emit(onFunc);
      emit.emit(")(r11)");
      emit.endLine();

      emit.emit("\tmtctr r12");
      emit.endLine();

      emit.emit("\tbctr");
      emit.endLine();

      emit.emit(".data");
      emit.endLine();

      emit.emit(".lazy_symbol_pointer");
      emit.endLine();

      emit.emit(lazyPtr);
      emit.emit(":");
      emit.endLine();

      emit.emit("\t.indirect_symbol _");
      emit.emit(onFunc);
      emit.endLine();

      emit.emit("\t.long dyld_stub_binding_helper");
      emit.endLine();

      funcCounter++;
    }
  }

  public void visitConditionalExpr(ConditionalExpr e)
  {
    Type ct = processType(e);
    int  tr = registers.getResultRegister(ct.getTag());

    Expr predicate = e.getTest();
    Expr trueExpr  = e.getTrueExpr();
    Expr falseExpr = e.getFalseExpr();

    needValue(trueExpr);
    int treg = resultReg;

    //  if (ct.isRealType()) {
    //    if (predicate.isMatchExpr()) {
    //      throw new scale.common.NotImplementedError("visitConditionalExpr");
    //    }

    //    needValue(predicate);
    //    int preg = resultReg;

    //    if ((tr == treg) || (tr == preg) || isAssignedRegister(tr))
    //      tr = registers.newTempRegister(ct.getTag());

    //    genRegToReg(treg, tr);

    //    Label             labt = createLabel();
    //    Label             labf = createLabel();
    //    LabelDisplacement disp = new LabelDisplacement(labt);
    //    Branch            inst = new BFormInstruction
    //      (Opcodes.BC, Opcodes.BRANCH_FALSE, Opcodes.EQ, disp, 2);
    //    inst.addTarget(labt, 0);
    //    inst.addTarget(labf, 1);
    //    appendInstruction(inst);
    //    appendLabel(labf);
    //    labf.setNotReferenced();
    //    registers.setResultRegister(tr);
    //    needValue(falseExpr);
    //    genRegToReg(resultReg, tr);
    //    registers.setResultRegister(-1);
    //    appendLabel(labt);
    //    resultRegAddressOffset = 0;
    //    resultRegMode = ResultMode.NORMAL_VALUE;
    //    resultReg = tr;
    //    return;
    //  }

    needValue(falseExpr);
    int freg = resultReg;

    needValue(predicate);
    int preg = resultReg;

    if (tr == preg)
      tr = registers.newTempRegister(ct.getTag());

    appendInstruction(new FrcInstruction(Opcodes.CMPWI, preg, 0L));

    Branch            brInst;
    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement tdisp = new LabelDisplacement(labt);

    if (tr == freg) {
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_TRUE, Opcodes.EQ, tdisp, 2);
      brInst.addTarget(labt, 0);
      brInst.addTarget(labf, 1);
      appendInstruction(brInst);
      appendLabel(labf);
      labf.setNotReferenced();
      genRegToReg(treg, tr);
    } else if (tr == treg) {
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_FALSE, Opcodes.EQ, tdisp, 2);
      brInst.addTarget(labt, 0);
      brInst.addTarget(labf, 1);
      appendInstruction(brInst);
      appendLabel(labf);
      labf.setNotReferenced();
      genRegToReg(freg, tr);
    } else {
      genRegToReg(treg, tr);
      brInst = new BFormInstruction(Opcodes.BC, Opcodes.BRANCH_FALSE, Opcodes.EQ, tdisp, 2);
      brInst.addTarget(labt, 0);
      brInst.addTarget(labf, 1);
      appendInstruction(brInst);
      appendLabel(labf);
      labf.setNotReferenced();
      genRegToReg(freg, tr);
    }

    appendLabel(labt);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = tr;
  }

  /**
   * Adjust large immediate values.  For example, "LI 3, IMMEDIATE"
   * loads the value IMMEDIATE into register r3. However, the
   * instruction limits the size of IMMEDIATE do be 16 bits. As a
   * result, we cannot have large immediate values. These have to be
   * split into two instructions, one for the MSBs and the other for
   * the LSBs. The same problem occurs when we deal with load or store
   * instructions which have a displacement. The displacement cannot
   * be greater than 16 bits.
   *
   * @param inst the instruction that is to be rewritten
   */
  private Instruction adjustImmediate(Instruction inst)
  {
    Instruction newInstsBegin = null;
    Instruction newInstsEnd   = null;

    if (inst instanceof FDcInstruction) {
      FDcInstruction fdcinst   = (FDcInstruction) inst;
      int            immediate = fdcinst.cv;

      if ((fdcinst.getOpcode() == Opcodes.LI) &&
          ((immediate < MIN_IMM16) || (MAX_IMM16 < immediate))) {

        // This is a Load Immediate instruction LI and has a huge offset,
        // which needs to be adjusted. This is how the adjustment will be
        // made.

        // Replace
        //   LI r3, value
        // with
        //   LIS r3,     value >> 16
        //   ORI r3, r3, value & 0xffff

        int         destReg = fdcinst.rd;
        Instruction newLis  = new FDcInstruction(Opcodes.LIS, destReg, immediate >> 16);
        Instruction newOri  = new FDrcInstruction(Opcodes.ORI, destReg, destReg, immediate & 0xffff, macosx);
        Instruction fi      = newLis;

        if (annotateCode) {
          Instruction comment = new CommentMarker("Big immediate " + inst.toString());
          comment.setNext(newLis);
          fi = comment;
        }

        newLis.setNext(newOri);
        newInstsBegin = fi;
        newInstsEnd = newOri;
      }
    } else if (inst instanceof FrcInstruction) {
      FrcInstruction frcinst   = (FrcInstruction) inst;
      int            opcode    = frcinst.getOpcode();
      long           immediate = frcinst.cv1;
      if (((opcode == Opcodes.CMPWI) || (opcode == Opcodes.CMPLWI)) &&
          ((immediate < MIN_IMM16) || (MAX_IMM16 < immediate))) {
        // This is a CMPLWI, CMPWI instruction that has a large immediate
        // value. We will replace
        //   CMPWI r3, value
        // with
        //   LI rx, value
        //   CMPW r3, rx

        int         registerUsed = frcinst.ra;
        int         regOpcode    = Opcodes.getNonImmediateOpcode(opcode);
        int         tmpReg       = registers.newTempRegister(RegisterSet.INTREG);
        Instruction li           = new FDcInstruction(Opcodes.LI, tmpReg, (int) immediate);
        Instruction cmp          = new FrrInstruction(regOpcode, registerUsed, tmpReg);

        li.setNext(cmp);
        newInstsBegin = li;
        newInstsEnd = cmp;
      }
    } else if (inst instanceof MemoryInstruction) {
      MemoryInstruction meminst = (MemoryInstruction) inst;
      Displacement      disp    = meminst.displacement;
      if ((disp != null) &&
          disp.isNumeric() &&
          ((disp.getDisplacement() < MIN_IMM16) || (disp.getDisplacement() > MAX_IMM16))) {
        // FIXME: At the moment, this translation works correctly for LWZ
        // and STW. Applicability to other instructions should be
        // verified and these instructions should be added to this list.
        // Eventually, the check below should be removed.

        int opcode = inst.getOpcode();
        if ((opcode != Opcodes.LWZ)  &&
            (opcode != Opcodes.LHZ)  &&
            (opcode != Opcodes.LBZ)  &&
            (opcode != Opcodes.LFD)  &&
            (opcode != Opcodes.LFS)  &&
            (opcode != Opcodes.STW)  &&
            (opcode != Opcodes.STH)  &&
            (opcode != Opcodes.STB)  &&
            (opcode != Opcodes.STFD) &&
            (opcode != Opcodes.STFS))
          throw new scale.common.InternalError("Unverified instruction: " + inst);

        // This is a Load/Store instruction.  Replace
        //   STW rd, disp (ra)    | LWZ rd, disp (ra)
        //                        |
        // with                   | with
        //   LA   rx, disp (r0)   | LA   rx, disp (r0)
        //   STWX rd, rx, ra      | LWZX rd, rx, ra

        // I am not completely sure of the semantics of r0 now.

        // In the code below, dataReg is the register which deals with
        // data. Data will be written to dataReg for a load instruction
        // and read from dataReg for a store instruction. addressReg
        // is the register which provides the base address of the
        // memory location to be accessed.

        int         dataReg    = meminst.rd;
        int         addressReg = meminst.ra;
        int         tmpReg     = registers.newTempRegister(RegisterSet.ADRREG);
        Instruction la         = new FDdrInstruction(Opcodes.LA, tmpReg, 0, disp, FT_NONE, macosx);

        // regOpcode is the opcode which deals with a register instead of
        // an immediate displacement.

        int regOpcode = Opcodes.getNonImmediateOpcode(opcode);

        // The new instruction, memx which deals with registers instead
        // of immediate displacements, is either a load or a store.

        Instruction memx = null;
        if (inst instanceof StoreInstruction)
          memx = new StorexInstruction(regOpcode, dataReg, tmpReg, addressReg);
        else
          memx = new LoadxInstruction(regOpcode, dataReg, tmpReg, addressReg);

        Instruction fi = la;
        if (annotateCode) {
          Instruction comment = new CommentMarker("Original " + inst.toString());
          comment.setNext(la);
          fi = comment;
        }

        la.setNext(memx);
        newInstsBegin = fi;
        newInstsEnd = memx;
      }
    } else if (inst instanceof FDdrInstruction) {
      FDdrInstruction fddrinst = (FDdrInstruction) inst;
      int             dftn     = fddrinst.dftn;
      Displacement    disp     = fddrinst.disp;

      if (FT_NONE == dftn) {
        // FIXME: At the moment, this translation works correctly for LA.
        // Applicability to other instructions should be verified and these
        // instructions should be added to this list.  Eventually, the
        // check below should be removed.
        int opcode = inst.getOpcode();
        if (opcode != Opcodes.LA)
          throw new scale.common.InternalError("Unverified instruction: " + inst);

        int         destReg = fddrinst.rd;
        int         srcReg  = fddrinst.ra;
        Instruction addis   = new FDrdInstruction(Opcodes.ADDIS, destReg, srcReg, disp, FT_HI16, macosx);
        Instruction addi    = new FDrdInstruction(Opcodes.ADDI, destReg, destReg, disp, FT_LO16, macosx);

        Instruction fi = addis;
        if (annotateCode) {
          Instruction comment = new CommentMarker("Original " + inst.toString());
          comment.setNext(addis);
          fi = comment;
        }

        addis.setNext(addi);
        newInstsBegin = fi;
        newInstsEnd = addi;
      }
    }

    if (null != newInstsBegin) {
      // We are replacing inst with the stream of instructions from
      // newInstsBegin ... newInstsEnd.

      if (inst == returnInst)
        returnInst = newInstsEnd;

      newInstsEnd.setNext(inst.getNext());
      return newInstsBegin;
    }

    return inst;
  }

  public void adjustImmediates(Instruction first)
  {
    Instruction prev = null;
    for (Instruction inst = first; inst != null;) {
      Instruction modifiedInst = adjustImmediate(inst);
      if (modifiedInst == inst) {
        // If there was no change, move forward and adjust the next
        // instruction.

        prev = inst;
        inst = inst.getNext();
      } else {
        // If there was a change, add the new instruction after prev (thus
        // removing the current instruction from the instruction stream).
        // FIXME: prev should not be null here.
        // Make inst point to the new instruction.

        if (lastInstruction == inst)
          throw new scale.common.InternalError("lastInstruction changed");
        else if (first == inst)
          throw new scale.common.InternalError("firstInstruction changed");

        prev.setNext(modifiedInst);
        inst = modifiedInst;
      }
    }
  }
}
