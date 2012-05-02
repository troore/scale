package scale.backend.sparc;

import java.util.Enumeration;
import java.util.BitSet;
import java.util.Iterator;
import java.math.BigInteger;

import scale.backend.*;
import scale.common.*;
import scale.callGraph.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.score.*;
import scale.clef.type.*;
import scale.clef.LiteralMap;
import scale.score.chords.*;
import scale.score.expr.*;

/** 
 * This class converts Scribble into Sparc instructions.
 * <p>
 * $Id: SparcGenerator.java,v 1.214 2007-10-04 19:57:57 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Provides generated code for the following ISAs:
 * <dl>
 * <dt>v8<dd>Compile for the SPARC-V8 ISA.  Enables the compiler to
 * generate code for good performance on the V8 architecture.
 * Example: SPARCstation 10
 * <dt>v8plus<dd>Compile for the V8plus version of the SPARC V9 ISA.
 * By definition, V8plus means the V9 ISA, but limited to the 32-bit
 * subset defined by the V8plus ISA specification, without the Visual
 * Instruction Set (VIS), and without other implementation- specific
 * ISA extensions.  This option enables the compiler to generate code
 * for good performance on the V8plus ISA.  The resulting object code
 * is in SPARC-V8+ ELF32 format and only executes in a Solaris
 * UltraSPARC environment -- it does not run on a V7 or V8 processor.
 * Example: Any system based on the UltraSPARC chip architecture
 * <dt>v8plusa<dd>Compile for the V8plusa version of the SPARC V9
 * ISA.  By definition, V8plusa means the V8plus architecture, plus
 * the Visual Instruction Set (VIS) version 1.0, and with UltraSPARC
 * exten- sions.  This option enables the compiler to generate code
 * for good performance on the UltraSPARC architecture, but limited to
 * the 32-bit subset defined by the V8plus specifi- cation.  The
 * resulting object code is in SPARC-V8+ ELF32 format and only
 * executes in a Solaris UltraSPARC environment -- it does not run on
 * a V7 or V8 processor.  Example: Any system based on the UltraSPARC
 * chip architecture
 * <dt>v8plusb<dd>Compile for the V8plusb version of the SPARC V8plus
 * ISA with UltraSPARC-III extensions.  Enables the compiler to
 * generate object code for the UltraSPARC architecture, plus the
 * Visual Instruction Set (VIS) version 2.0, and with UltraSPARC-III
 * extensions.  The result- ing object code is in SPARC-V8+ ELF32
 * format and executes only in a Solaris UltraSPARC-III environment.
 * Compiling with this option uses the best instruction set for good
 * performance
 * <dt>v9<dd>Compile for the SPARC-V9 ISA.  Enables the compiler to
 * generate code for good performance on the V9 SPARC architec- ture
 * using the 64-bit ABI and can only be linked with other SPARC-V9
 * object files in the same for- mat.  The resulting executable can
 * only be run on an UltraSPARC processor running a 64-bit enabled
 * Solaris operating environment with the 64-bit kernel.
 * <dt>v9a<dd>Compile for the SPARC-V9 ISA with UltraSPARC extensions.
 * Adds to the SPARC-V9 ISA the Visual Instruc- tion Set (VIS) and
 * extensions specific to UltraSPARC processors, and enables the com-
 * piler to generate code for good performance on the V9 SPARC
 * architectureusing the 64-bit ABI and can only be linked with other
 * SPARC-V9 object files in the same format.  The resulting exe-
 * cutable can only be run on an UltraSPARC pro- cessor running a
 * 64-bit enabled Solaris oper- ating environment with the 64-bit
 * kernel.
 * <dt>v9a<dd>v9b<dd>Compile for the SPARC-V9 ISA with UltraSPARC- III
 * extensions.  Adds UltraSPARC-III extensions and VIS ver- sion 2.0
 * to the V9a version of the SPARC-V9 ISA.  Compiling with this option
 * uses the best instruction set for good performance in a Solaris
 * UltraSPARC-III environmentusing the 64-bit ABI and can only be
 * linked with other SPARC-V9 object files in the same format.  The
 * resulting executable can only be run on an UltraSPARC-III processor
 * running a 64-bit enabled Solaris operating environment with the
 * 64-bit kernel.
 * </dl>
 */

public class SparcGenerator extends scale.backend.Generator
{
  /**
   * Un-initialized large data area.
   */
  public static final int BSS = 0;
  /**
   * Initialized data area.
   */
  public static final int DATA = 1;
  /**
   * Initialized large data area.
   */
  public static final int DATA1 = 2;
  /**
   * Initialized read-only data area.
   */
  public static final int RODATA = 3;
  /**
   * Initialized read-only large data area.
   */
  public static final int RODATA1 = 4;
  /**
   * Instructions.
   */
  public static final int TEXT = 5;
  /**
   * Comment section.
   */
  public static final int COMMENT = 6;
  /**
   * Debugging information.
   */
  public static final int DEBUG = 7;
  /**
   * Finalization code
   */
  public static final int FINI = 8;
  /**
   * Initialization code
   */
  public static final int INIT = 9;
  /**
   * Line number information.
   */
  public static final int LINE = 10;
  /**
   * Note information.
   */
  public static final int NOTE = 11;

  public static final int FCC0Flg =  1;
  public static final int FCC1Flg =  2;
  public static final int FCC2Flg =  4;
  public static final int FCC3Flg =  8;
  public static final int ICCFlg  = 16;
  public static final int XCCFlg  = 32;
  public static final int YFlg    = 64;

  /**
   * Specifies the FCC0 condition code.
   */
  public static final int FCC0 = 0;
  /**
   * Specifies the FCC1 condition code.
   */
  public static final int FCC1 = 1;
  /**
   * Specifies the FCC2 condition code.
   */
  public static final int FCC2 = 2;
  /**
   * Specifies the FCC3 condition code.
   */
  public static final int FCC3 = 3;
  /**
   * Specifies the ICC condition code.
   */
  public static final int ICC  = 4;
  /**
   * Specifies the XCC condition code.
   */
  public static final int XCC  = 5;

  /**
   * Map from CC code to string.
   */
  public static final String[] ccTab = {
    "fcc0", "fcc1", "fcc2", "fcc3",
    "icc", "xcc"};
  /**
   * Map from CC code to CC flag.
   */
  public static final byte[] ccFlgTab = {
    FCC0Flg, FCC1Flg, FCC2Flg, FCC3Flg,
    ICCFlg, XCCFlg};

  /**
   * Privileged Registers
   */
  public static final int TPC        = 0;
  public static final int TNPC       = 1;
  public static final int TSTATE     = 2;
  public static final int TT         = 3;
  public static final int TICK       = 4;
  public static final int TBA        = 5;
  public static final int PSTATE     = 6;
  public static final int TL         = 7;
  public static final int PIL        = 8;
  public static final int CWP        = 9;
  public static final int CANSAVE    = 10;
  public static final int CANRESTORE = 11;
  public static final int CLEANWIN   = 12;
  public static final int OTHERWIN   = 13;
  public static final int WSTATE     = 14;
  public static final int FQ         = 15;
  public static final int VER        = 31;

  public static final String[] pRegs = {
    "tpc", "tnpc", "tstate", "tt",
    "tick", "tba", "pstate", "tl",
    "pil", "cwp", "cansave", "canrestore",
    "cleanwin", "otherwin", "wstate", "fq",
    "?16", "?17", "?18", "?19",
     "?20", "?21", "?22", "?23",
    "?24", "?25", "?26", "?27",
     "?28", "?29", "?30", "VER"
  };

  /**
   * State Registers
   */
  public static final int SR_Y     = 0;
  public static final int SR_CCR   = 2;
  public static final int SR_ASI   = 3;
  public static final int SR_STICK = 4;
  public static final int SR_PC    = 5;
  public static final int SR_FPRS  = 6;
  public static final int SR_ASR   = 7;

  public static final String[] sRegs = {
    "y", "ccr", "asi", "tick", "pc",
    "fprs", "asr"};

  public static final int FT_NONE = 0;
  public static final int FT_HI   = 1;
  public static final int FT_LO   = 2;
  public static final int FT_HH   = 3;
  public static final int FT_HM   = 4;
  public static final String[] ftns = {"", "%hi", "%lo", "%hh", "%hm"};

  public static final int MAX_IMM13 = 4095;
  public static final int MIN_IMM13 = -4096;
  /**
   * Offset to the argument save area.
   */
  public static final int ARG_SAVE_OFFSET = 68;

  private static final short[] singleIntUses = {SparcRegisterSet.O0_REG};
  private static final short[] doubleIntUses = {SparcRegisterSet.O0_REG,
                                                SparcRegisterSet.O1_REG};
  private static final short[] doubleFltUses = {SparcRegisterSet.O0_REG,
                                                SparcRegisterSet.O1_REG};
  private static final short[] quadIntUses   = {SparcRegisterSet.O0_REG,
                                                SparcRegisterSet.O1_REG,
                                                SparcRegisterSet.O2_REG,
                                                SparcRegisterSet.O3_REG};
  private static final short[] doubleDefs    = {SparcV8RegisterSet.F0_REG,
                                                SparcV8RegisterSet.F1_REG,
                                                SparcV8RegisterSet.D0_REG};
  private static final short[] fltDefs       = {SparcV8RegisterSet.F0_REG};
  private static final short[] intDefs       = {SparcV8RegisterSet.O0_REG};
  private static final short[] longLongDefs  = {SparcV8RegisterSet.O0_REG,
                                                SparcV8RegisterSet.O1_REG};

  private static final int   SAVED_REG_SIZE       =  4; // Size of registers saved on the stack.
  private static final int   RETURN_STRUCT_OFFSET = 64; // Offset to structure return address.
  private static final int   MAX_ARG_SIZE         =  8; // Maximum argument size passed in registers.
  private static final int   MAX_ARG_REGS         =  6; // Maximum number of arguments passed in registers.

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

  private int[]        nxtMvReg;        // Hold temporary register values.
  private int          localVarSize;    // Size of the area on the stack used for local variables.
  private int          argsOnStackSize; // Size of the area on the stack used for passing arguments.
  private int          isa;             // The ISA in use.
  private boolean      v8plus;          // True if isa > V8.
  private boolean      elf64;           // True if isa >= V9 implying that the 64-bit ABI is to be used.
  private Displacement ftnDisp;         // Displacement for this function.
  private Displacement allocaDisp;      // Displacement for alloca().
  private Displacement complexDisp;     // Used for area to return complex value from a complex transcendental function.
  private Stabs        stabs;           // Debugging information.
  private Label        startLabel;      // Label placed after function prolog.
  private Vector<Instruction> lookBack;        // Work list for delay slot logic.

  /**
   * @param cg is the call graph to be transformed
   * @param machine specifies machine details
   * @param features controls the instructions generated (not used)
   */
  public SparcGenerator(CallGraph cg, Machine machine, int features)
  {
    super(cg, null, machine, features);

    SparcMachine sm = (SparcMachine) machine;
    this.isa    = sm.getISA();
    this.v8plus = isa > SparcMachine.V8;  // Sparc V9 instructions.
    this.elf64  = isa >= SparcMachine.V9; // 64-bit ABI

    if (elf64)
      throw new scale.common.NotImplementedError("64-bit ABI not available.");

    if (v8plus)
      this.registers = new SparcV9RegisterSet();
    else
      this.registers = new SparcV8RegisterSet();

    this.un       = new UniqueName(".L_");
    this.lookBack = new Vector<Instruction>();
    this.stabs    = new Stabs(machine, Stabs.FMT_SOLARIS, isFortran() ? Stabs.N_SO_FORTRAN : Stabs.N_SO_C);
    this.nxtMvReg = new int[5];

    readOnlyDataArea = RODATA;
  }

  /**
   * Generate the machine instructions for a CFG.
   */
  public void generateScribble()
  {
    localVarSize     = 0;
    argsOnStackSize  = 0;
    allocaDisp       = null;
    complexDisp      = null;
    ftnDisp          = new SymbolDisplacement(currentRoutine.getName(), 0);
    startLabel       = createLabel();
    stkPtrReg        = SparcRegisterSet.FP_REG;

    String name = currentRoutine.getName();

    // Generate stab info for this routine.

    if (currentRoutine.isMain()) {
      if (currentRoutine.isGlobal())
        stabs.addXStabs(name, Stabs.N_MAIN, 0, 0, 0);
      stabs.addStabs(name, Stabs.N_MAIN, 0, 0, 0);
    }
    if (currentRoutine.isGlobal())
      stabs.addXStabs(name, Stabs.N_FUN, 0, 0, 0);

    int startStab = stabs.genFtnDescriptor(currentRoutine);

    // Set up the translation of the CFG to instructions.

    BeginChord start = scribble.getBegin();

    labelCfgForBackend();

    Instruction firstInstruction = startRoutineCode();

    findLastInstruction(firstInstruction);

    // Process the arguments to the routine.

    layoutParameters();

    LabelDisplacement ld        = new LabelDisplacement(startLabel);
    Displacement      startDisp = new DiffDisplacement(ld, ftnDisp);
    stabs.addStabn(Stabs.N_LBRAC, 0, 1, startDisp);

    // Process the local variables of the routine.

    stabs.startCommon();
    processDecls();
    stabs.endCommon();

    // Generate instructions.

    generateProlog((ProcedureType) processType(currentRoutine));

    convertCFG(start);

    // Do some instruction level optimizations before register allocation.

    resetForBasicBlock();

    copyPropagate(firstInstruction);

    peepholeBeforeRegisterAllocation(firstInstruction);

    if (trace)
      System.out.println("\n\nEnd code generation");

    int[] map = allocateRegisters(firstInstruction, trace);

    if (trace)
      System.out.println("\n\nEnd register allocation");

    // Do some instruction level optimizations after register
    // allocation.

    peepholeAfterRegisterAllocation(firstInstruction);

    // Now we can fix up the routine prolog code.

    endRoutineCode(map);

    if (trace)
      System.out.println("\n\nEnd routine");

    removeUnneededInstructions(firstInstruction);

    saveGeneratedCode(firstInstruction);

    Displacement labDisp = new LabelDisplacement(lastLabel);
    Displacement endDisp = new DiffDisplacement(labDisp, ftnDisp);
    int          endStab = stabs.addStabn(Stabs.N_RBRAC, 0, 1, endDisp);

    stabs.renumberRegisters(startStab, endStab, map, registers.numRealRegisters());
  }

  /**
   * Called at the beginning of a module.
   */
  protected void startModule()
  {
    String path = cg.getName();
    int    si   = path.lastIndexOf('/');
    String file = path.substring(si + 1);

    path = path.substring(0, si + 1);

    stabs.addXStabs(path, Stabs.N_SO, 0, 0, 0);
    stabs.addXStabs(file, Stabs.N_SO, 0, 3, 0);
    stabs.addXStabs("", Stabs.N_OBJ, 0, 0, 0);
    stabs.addXStabs("", Stabs.N_OBJ, 0, 0, 0);
    stabs.addXStabs(genDebugInfo ? "g" : "", Stabs.N_OPT, 0, 0, 0);

    stabs.addStabs(path, Stabs.N_SO, 0, 0, 0);
    stabs.addStabs(file, Stabs.N_SO, 0, 3, 0);
    stabs.addStabs("", Stabs.N_OBJ, 0, 0, 0);
    stabs.addStabs("", Stabs.N_OBJ, 0, 0, 0);
    stabs.addStabs(genDebugInfo ? "g" : "", Stabs.N_OPT, 0, 0, 0);
  }

  /**
   * Called at the end of a module.
   */
  protected void endModule()
  {
    stabs.addStabn(Stabs.N_ENDM, 0, 0, 0);
  }

  /**
   * Called at the end of code generation for a routine.  On the Sparc
   * we need to pass some critical information to the assembler.
   */
  protected void endRoutineCode(int[] regMap)
  {
    Instruction first    = currentBeginMarker;
    Instruction last     = returnInst;
    int         nrr      = registers.numRealRegisters();

    if ((last == null) && Debug.debug(1)) {
      System.out.print("** Warning: ");
      System.out.print(currentRoutine.getName());
      System.out.println("() does not return.");
    }

    appendLabel(createLabel());

    argsOnStackSize = (argsOnStackSize + 7) & -8;
    if (allocaDisp != null)
      // Adjust the addresses returned by __builtin_alloca to account
      // for the argument passing area.
      allocaDisp.adjust(argsOnStackSize);

    int          frameSize = (((92 + localVarSize + argsOnStackSize + 15)
                               / 16) * 16); // Frame size must be a multiple of 16.
    Instruction  saveInst  = null;
    Instruction  st        = first;
    Displacement fdispm    = getDisp(-frameSize);

    if (frameSize <= MAX_IMM13) {
      saveInst = new IntOpLitInstruction(Opcodes.SAVE,
                                         SparcRegisterSet.SP_REG,
                                         fdispm,
                                         FT_NONE,
                                         SparcRegisterSet.SP_REG);
    } else {
      st = insertInstruction(new SethiInstruction(Opcodes.SETHI,
                                                  SparcRegisterSet.G1_REG,
                                                  fdispm,
                                                  FT_HI),
                             st);
      st = insertInstruction(new IntOpLitInstruction(Opcodes.ADD,
                                                     SparcRegisterSet.G1_REG,
                                                     fdispm,
                                                     FT_LO,
                                                     SparcRegisterSet.G1_REG),
                             st);
      saveInst = new IntOpInstruction(Opcodes.SAVE,
                                      SparcRegisterSet.SP_REG,
                                      SparcRegisterSet.G1_REG,
                                      SparcRegisterSet.SP_REG);
    }
    insertInstruction(saveInst, st);
    appendInstruction(new EndMarker(currentRoutine));

    if (trace) {
      System.out.print("eR: " + currentRoutine.getName());
      System.out.print(" fs ");
      System.out.print(frameSize);
      System.out.print(" lvs ");
      System.out.print(localVarSize);
      System.out.print(" eos ");
      System.out.println(argsOnStackSize);
    }
  }

  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public static String displayDisp(Displacement disp, int ftn)
  {
    if (ftn == FT_NONE)
      return disp.toString();

    StringBuffer buf = new StringBuffer(SparcGenerator.ftns[ftn]);
    buf.append('(');
    buf.append(disp.toString());
    buf.append(')');
    return buf.toString();
  }

  /**
   * Do peephole optimizations after registers are allocated.
   */
  protected void peepholeAfterRegisterAllocation(Instruction first)
  {
    Instruction inst = first;

    lookBack.clear();

    while (inst != null) {
      Instruction next = inst.getNext();
      if (inst.isBranch()) {
        SparcBranch      sb = (SparcBranch) inst;
        SparcInstruction si = sb.getDelaySlot();
        if (si == null) {
          if (sb.isUnconditional())
            lookBackDelay(lookBack, sb);
          else {
            boolean flg = false;
            if (sb.canBeAnnulled())
              flg = lookForwardDelay(sb);
            if (!flg)
              lookBackDelay(lookBack, sb);
          }
        }
        lookBack.clear();
      } else
        lookBack.addElement(inst);

      inst = next;
    }
  }

  /**
   * Find an instruction previous to the branch that can be used in
   * the delay slot.
   * @param lookBack is a stack of possible candidate instructions
   * @param sb is the branch instruction
   */
  private void lookBackDelay(Vector<Instruction> lookBack, SparcBranch sb)
  {
    int     l   = lookBack.size();
    boolean lnk = ((sb.getOpcode() == Opcodes.CALL) ||
                   (sb.getOpcode() == Opcodes.JMPL));

    outer:
    for (int i = l - 1; i >= 1; i--) {
      Instruction inst = lookBack.elementAt(i);
      if (inst.nullified())
        continue;

      if (inst.isLabel())
        return; // Can't look back past a label.

      if (!(inst instanceof SparcInstruction))
        continue; // Not an instruction that can be used.

      if (lnk && inst.uses(SparcRegisterSet.O7_REG, registers))
        continue;

      if (inst.getOpcode() == Opcodes.NOP)
        continue;  // Does no good and may remove essential padding between FP ops.

      SparcInstruction candidate = (SparcInstruction) inst;
      if (!sb.independent(candidate, registers))
        continue; // Branch depends on this instruction.

      // Check all those instructions in-between the branch and the candidate.

      for (int j = i + 1; j < l; j++) {
        Instruction inbetween = lookBack.elementAt(j);
        if (!(inbetween instanceof SparcInstruction))
          continue;
        if (!(inbetween.independent(candidate, registers) &&
            candidate.independent(inbetween, registers)))
          continue outer; // Interference between the two instructions.
      }

      // Move the candidate instruction into the delay slot.

      Instruction last = lookBack.elementAt(i - 1);
      sb.setDelaySlot(candidate);

      last.setNext(candidate.getNext());
      candidate.setNext(null);
      return;
    }
    // Failed to find a suitable instruction for the delay slot.
  }

  /**
   * Find an instruction on the branch path that can be used in the
   * delay slot if it is annulled.
   * @param sb is the branch instruction
   * @return true if the slot was filled.
   */
  private boolean lookForwardDelay(SparcBranch sb)
  {
    if (sb.numTargets() > 2)
      return false;

    Label lab = sb.getTarget(0);
    if (lab.numPredecessors() > 1)
      return false; // If more than one entry into this basic block.

    Instruction inst = lab.getNext();
    while (inst instanceof SparcLineMarker)
      inst = inst.getNext();

    if (!(inst instanceof SparcInstruction))
      return false; // Not an instruction that can be used.

    SparcInstruction si = (SparcInstruction) inst;
//      if (!sb.independent(si))
//        return false;

    // Use this instruction in the delay slot.  Replace it in the
    // instruction stream by an AnnulMarker since it is annulled.

    sb.setDelaySlot(si);
    sb.setAnnulled(true);

    AnnulMarker am = new AnnulMarker(si);
    am.setNext(si.getNext());
    lab.setNext(am);
    si.setNext(null);
  
    return true;
  }

  /**
   * Generate assembly language file.
   * @param emit is the stream to use.
   * @param source is the source file name
   * @param comments is a list of Strings containing comments
   */
  public void assemble(Emit emit, String source, Enumeration<String> comments)
  {
    while (comments.hasMoreElements()) {
      String comment = comments.nextElement();
      if (comment.startsWith("ident ")) {
        emit.emit("\t.ident ");
        emit.emit(comment.substring(6));
      } else {
        emit.emit("\t! ");
        emit.emit(comment);
      }
      emit.endLine();
    }

    SparcAssembler asm = new SparcAssembler(this, source);
    asm.assemble(emit, dataAreas);
    stabs.assemble(asm, emit);
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
   * Associate information with a variable Declaration kept in a register.
   * @param decl is the variable
   * @param register is the register allocated for the variable
   * @param regha is mode of the register
   */
  protected void defineDeclInRegister(Declaration decl, int register, ResultMode regha)
  {
    super.defineDeclInRegister(decl, register, regha);
    if (genDebugInfo)
      stabs.defineDeclInRegister(decl, register);
  }

  /**
   * Associate information with a Declaration kept on the stack.
   * A unique integer value is set into the declaration label.
   * This value can be used to retrieve the information supplied.
   * @param decl is the variable
   * @param disp - displacement associated with declaration
   */
  protected void defineDeclOnStack(Declaration decl, Displacement disp)
  {
    super.defineDeclOnStack(decl, disp);
    if (genDebugInfo)
      stabs.defineDeclOnStack(decl, disp);
  }

  /**
   * Associate information with a Declaration kept in memory.
   * A unique integer value is set into the declaration label.
   * This value can be used to retrieve the information supplied.
   * @param decl is the variable
   * @param disp - displacement associated with declaration
   */
  protected void defineDeclInMemory(Declaration decl, Displacement disp)
  {
    super.defineDeclInMemory(decl, disp);
    if (genDebugInfo)
      stabs.defineDeclInMemory(decl, disp);
  }

  /**
   * Associate information with a Declaration in COMMON.
   * A unique integer value is set into the declaration label.
   * This value can be used to retrieve the information supplied.
   * @param decl is the variable
   * @param disp - displacement associated with declaration
   */
  protected void defineDeclInCommon(Declaration decl, Displacement disp)
  {
    super.defineDeclInCommon(decl, disp);
    if (genDebugInfo)
      stabs.defineDeclInCommon(decl, disp);
  }

  /**
   * Associate information with a routine.
   * A unique integer value is set into the declaration label.
   * This value can be used to retrieve the information supplied.
   * @param rd is the routine
   * @param disp - displacement associated with declaration
   */
  protected void defineRoutineInfo(RoutineDecl rd, Displacement disp)  
  {
    super.defineRoutineInfo(rd, disp);
    if (genDebugInfo && (rd.getScribbleCFG() == null))
      stabs.defineRoutineInfo(rd, disp);
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
      area = readOnly ? RODATA : DATA;

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
    if (currentRoutine.usesSetjmp()) {
      assignDeclToStack(vd);
      return;
    }

    Type dt   = vd.getType();
    Type vt   = processType(dt);
    int  regd = registers.newTempRegister(vt.getTag());
    defineDeclInRegister(vd, regd, ResultMode.NORMAL_VALUE);
  }

  protected void assignDeclToStack(VariableDecl vd)
  {
    Type dt  = vd.getType();
    Type vt  = processType(dt);
    int  sts = vt.memorySizeAsInt(machine);

    localVarSize += Machine.alignTo(sts, 8);
    Displacement sdisp = new StackDisplacement(-localVarSize);
    defineDeclOnStack(vd, sdisp);
  }

  /**
   * The RoutineDecl is assigned a tag.  The tag can be used to
   * retrieve information about the declaration.  For a RoutineDecl,
   * the information is a Displacement, the data area for the routine
   * and the label to be used for BSR calls to the routine.
   * @param rd is the declaration
   * @param topLevel is true if this declaration is defined outside of
   * a routine
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

  protected void processTypeDecl(TypeDecl td, boolean complete)
  {
    if (genDebugInfo && complete)
      stabs.genTypeDecl(td);
  }

  protected void processTypeName(TypeName tn)
  {
    if (genDebugInfo)
      stabs.genTypeName(tn);
  }

  /**
   * Create a new read-only data area whose value is a table of
   * displacements.
   */
  private Displacement createAddressTable(Chord[] entries,
                                          long[]  indexes,
                                          int     min,
                                          int     max)
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
    int          handle = allocateData(name, TEXT, SpaceAllocation.DAT_ADDRESS, 4, true, v, 1, 4);
    Displacement disp   = new SymbolDisplacement(name, handle);
    associateDispWithArea(handle, disp);

    return disp;
  }

  private void checkCont(int reg)
  {
    assert (!registers.virtualRegister(reg) ||
            (registers.doubleRegister(reg) && registers.continueRegister(reg + 1))) :
      "Invalid double " + registers.display(reg);
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
    appendInstruction(new SethiInstruction(Opcodes.SETHI, dest, disp, FT_HI));
    appendInstruction(new IntOpLitInstruction(Opcodes.ADD, dest, disp, FT_LO, dest));
    return dest;
  }

  private int loadAddressHigh(Displacement disp)
  {
    int adrReg = registers.getResultRegister(RegisterSet.ADRREG);
    appendInstruction(new SethiInstruction(Opcodes.SETHI, adrReg, disp));
    return adrReg;
  }

  /**
   * Generate instructions to load an immediate integer value into a
   * register.
   * @param value is the value to load
   * @param dest is the register containing the result
   * @return the register containing the value (usually dest but may
   * be a hardware zero register)
   */
  protected int genLoadImmediate(long value, int dest)
  {
    if ((value == 0) && !registers.doubleRegister(dest))
      return SparcRegisterSet.G0_REG;
    genLoadImmediate(value, SparcRegisterSet.G0_REG, dest);
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
    Displacement disp = defFloatValue(value, destSize);
    int          adr  = loadAddressHigh(disp);

    appendInstruction(new LoadLitInstruction(destSize > 4 ? Opcodes.LDDF : Opcodes.LDF, adr, disp, FT_LO, dest));
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
    genLoadImmediate(disp.getDisplacement(), SparcRegisterSet.FP_REG, dest);
    return dest;
  }

  /**
   * Allocate a location on the stack for storing a value of the
   * specified size.  Put the address of the location in the register.
   * @param adrReg specifies the register to receive the address
   * @param type is the type of the value
   * @return the size of the value to be stored
   */
  protected int allocStackAddress(int adrReg, Type type)
  {
    int ts = type.memorySizeAsInt(machine);
    localVarSize += Machine.alignTo(ts, 8);

    if (ts < 4) // A full word store is faster.
      ts = 4;

    genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, adrReg);
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
                              int          dftn,
                              int          size,
                              long         alignment,
                              boolean      signed)
  {
    if (registers.pairRegister(dest)) {
      int s = size / 2;
      assert (s <= 16) : "Paired register size " + size;
      loadFromMemoryX(dest, address, disp, dftn, s, alignment, signed);
      loadFromMemoryX(dest + registers.numContiguousRegisters(dest), address, disp.offset(s), dftn, s, alignment, signed);
      return;
    }

    if (size <= 16) {
      loadFromMemoryX(dest, address, disp, dftn, size, alignment, signed);
      return;
    }

    // Structs

    while (true) {
      int s = size;
      if (s > 16)
        s = 16;
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
  protected void loadFromMemoryX(int          dest,
                                 int          address,
                                 Displacement disp,
                                 int          dftn,
                                 int          size,
                                 long         alignment,
                                 boolean      signed)
  {
    int     r1      = registers.lastRegister(dest);
    boolean isFloat = registers.floatRegister(dest);
    boolean regAln  = v8plus || registers.tempRegister(dest) || ((dest & 1) == 0);

    switch (size) {
    case 1:
      if (signed) {
        appendInstruction(new LoadLitInstruction(Opcodes.LDSB, address, disp, dftn, r1));
        return;
      }
      
      appendInstruction(new LoadLitInstruction(Opcodes.LDUB, address, disp, dftn, r1));
      return;
    case 2:
      if ((alignment % 2) != 0) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadLitInstruction(signed ? Opcodes.LDSB : Opcodes.LDUB, address, disp, dftn, r1));
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, r1, getDisp(8), FT_NONE, r1));
        appendInstruction(new LoadLitInstruction(Opcodes.LDUB, address, disp.offset(1), dftn, tr));
        appendInstruction(new IntOpInstruction(Opcodes.OR, r1, tr, r1));
        return;
      }

      appendInstruction(new LoadLitInstruction(signed ? Opcodes.LDSH : Opcodes.LDUH, address, disp, dftn, r1));
      return;

    case 4:
      if ((alignment % 4) != 0) {
        if (dftn != FT_NONE) {
          int atr = registers.newTempRegister(RegisterSet.ADRREG);
          appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
          address = atr;
          disp = getDisp(0);
          dftn = FT_NONE;
        }

        int tr1 = r1;
        if (isFloat)
          tr1 = registers.newTempRegister(RegisterSet.INTREG);

        load4Bytes(tr1, address, disp, dftn, alignment, signed);

        if (!isFloat)
          return;

        Displacement disp2 = getDisp(-4);
        appendInstruction(new StoreLitInstruction(Opcodes.ST, SparcRegisterSet.SP_REG, disp2, FT_NONE, tr1));
        address = SparcRegisterSet.SP_REG;
        disp = disp2;
        dftn = FT_NONE;
      }

      appendInstruction(new LoadLitInstruction(isFloat ? Opcodes.LDF : Opcodes.LD, address, disp, dftn, r1));
      return;

    case 8:
      if (regAln && ((alignment % 8) == 0)) {
        int opcode = Opcodes.LDDF;
        if (!isFloat)
          opcode = (v8plus) ? Opcodes.LDX : Opcodes.LDD;
        appendInstruction(new LoadLitInstruction(opcode, address, disp, dftn, dest));
        return;
      }

      r1 = registers.rangeBegin(dest);
      int dest1 = r1;
      int dest2 = dest1 + 1;

      if (dftn != FT_NONE) {
        int atr = registers.newTempRegister(RegisterSet.ADRREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
        address = atr;
        disp = getDisp(0);
        dftn = FT_NONE;
      }

      if ((alignment % 4) != 0) {
        if (isFloat) {
          dest1 = registers.newTempRegister(RegisterSet.INTREG);
          dest2 = registers.newTempRegister(RegisterSet.INTREG);
        }
        load4Bytes(dest1, address, disp,           dftn, alignment, signed);
        load4Bytes(dest2, address, disp.offset(4), dftn, alignment, signed);

        if (!isFloat)
          return;

        Displacement disp2 = getDisp(-8);
        appendInstruction(new StoreLitInstruction(Opcodes.ST, SparcRegisterSet.SP_REG, disp2,           FT_NONE, dest1));
        appendInstruction(new StoreLitInstruction(Opcodes.ST, SparcRegisterSet.SP_REG, disp2.offset(4), FT_NONE, dest2));
        address = SparcRegisterSet.SP_REG;
        disp = disp2;
        dftn = FT_NONE;
      }

      if (isFloat) {
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, address, disp,           dftn, r1 + 0));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, address, disp.offset(4), dftn, r1 + 1));
        return;
      }

      if (v8plus) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadLitInstruction(Opcodes.LDUW, address, disp,           dftn, tr));
        appendInstruction(new LoadLitInstruction(Opcodes.LDUW, address, disp.offset(4), dftn, dest1));
        appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, tr, getDisp(32), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.OR, tr, dest1, dest1));
        return;
      }

      appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp,           dftn, dest1));
      appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp.offset(4), dftn, dest2));
      return;

    case 16:
      if (dftn != FT_NONE) {
        int atr = registers.newTempRegister(RegisterSet.ADRREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
        address = atr;
        disp = getDisp(0);
        dftn = FT_NONE;
      }

      if (registers.floatRegister(dest)) {
        int dests = registers.rangeBegin(dest);
        if ((alignment % 8) == 0) {
          if (v8plus) {
            appendInstruction(new LoadLitInstruction(Opcodes.LDQF, address, disp, dftn, dests + 0));
            return;
          }
          appendInstruction(new LoadLitInstruction(Opcodes.LDDF, address, disp,            dftn, dests + 0));
          appendInstruction(new LoadLitInstruction(Opcodes.LDDF, address, disp.offset(8),  dftn, dests + 2));
          return;
        }

        appendInstruction(new LoadLitInstruction(Opcodes.LDF, address, disp,            dftn, dests + 0));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, address, disp.offset(4),  dftn, dests + 1));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, address, disp.offset(8),  dftn, dests + 2));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, address, disp.offset(12), dftn, dests + 3));
        return;
      }
      break;
    }
    throw new scale.common.InternalError("Unknown data type size (" + size + ")");
  }

  private void load4Bytes(int          dest,
                          int          address,
                          Displacement disp,
                          int          dftn,
                          long         alignment,
                          boolean      signed)
  {
    int tr  = registers.newTempRegister(RegisterSet.INTREG);
    int tr2 = dest;
    if (dest == address)
      tr2 = registers.newTempRegister(RegisterSet.INTREG);

    if ((alignment % 2) != 0) {
      appendInstruction(new LoadLitInstruction(signed ? Opcodes.LDSB : Opcodes.LDUB, address, disp, dftn, tr2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr2, getDisp(24), FT_NONE, tr2));
      appendInstruction(new LoadLitInstruction(Opcodes.LDUB, address, disp.offset(1), dftn, tr));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr, getDisp(16), FT_NONE, tr));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr, tr2));
      appendInstruction(new LoadLitInstruction(Opcodes.LDUB, address, disp.offset(2), dftn, tr));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr, getDisp(8), FT_NONE, tr));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr, tr2));
      appendInstruction(new LoadLitInstruction(Opcodes.LDUB, address, disp.offset(3), dftn, tr));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr, dest));
      return;
    }

    appendInstruction(new LoadLitInstruction(signed ? Opcodes.LDSH : Opcodes.LDUH, address, disp, dftn, tr2));
    appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr2, getDisp(16), FT_NONE, tr2));
    appendInstruction(new LoadLitInstruction(Opcodes.LDUH, address, disp.offset(2), dftn, tr));
    appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr, dest));
  }

  /**
   * Generate instructions to load data from memory at the address
   * that is the sum of the two index register values.
   * @param dest is the destination register
   * @param address is the register containing the first index
   * @param offset is the register containing the second index
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected void loadFromMemoryDoubleIndexing(int     dest,
                                              int     address,
                                              int     offset,
                                              int     size,
                                              long    alignment,
                                              boolean signed,
                                              boolean real)
  {
    if (!v8plus) {
      if (!registers.tempRegister(dest))
        alignment = ((dest & 1) == 0) ? alignment : 1;
    }
 
    if ((size > 8) || ((size > 4) && ((alignment % 4) != 0))) {
      int tr = address;
      if (offset != 0) {
        tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.ADD, address, offset, tr));
      }
      loadFromMemory(dest, tr, getDisp(0), FT_NONE, size, alignment, signed);
      return;
    }

    int r1 = registers.lastRegister(dest);
    switch (size) {
    case 1:
      if (signed) {
        appendInstruction(new LoadInstruction(Opcodes.LDSB, address, offset, r1));
        return;
      }
      
      appendInstruction(new LoadInstruction(Opcodes.LDUB, address, offset, r1));
      return;
    case 2:
      if (signed) {
        appendInstruction(new LoadInstruction(Opcodes.LDSH, address, offset, r1));
        return;
      }

      appendInstruction(new LoadInstruction(Opcodes.LDUH, address, offset, r1));
      return;
    case 4:
      int opcode4 = Opcodes.LD;
      if (registers.floatRegister(dest))
        opcode4 = Opcodes.LDF;
 
      appendInstruction(new LoadInstruction(opcode4, address, offset, r1));
      return;
    case 8:
      int opcode8 = Opcodes.LDDF;
      if (!registers.floatRegister(dest))
        opcode8 = (v8plus) ? Opcodes.LDX : Opcodes.LDD;

      appendInstruction(new LoadInstruction(opcode8, address, offset, dest));
      return;
    }
    throw new scale.common.InternalError("Unknown data type size (" + size + ")");
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   */
  private void storeIntoMemory(int src, int address, Displacement disp, int dftn, int size, long alignment)
  {
    if (registers.pairRegister(src)) {
      int s = size / 2;
      assert (s <= 16) : "Paired register size " + size;
      storeIntoMemoryX(src + 0, address, disp,           dftn, s, alignment);
      storeIntoMemoryX(src + registers.numContiguousRegisters(src), address, disp.offset(s), dftn, s, alignment);
      return;
    }

    if (size <= 16) {
      storeIntoMemoryX(src, address, disp, dftn, size, alignment);
      return;
    }

    // Structs

    while (true) {
      int s = size;
      if (s > 16)
        s = 16;
      storeIntoMemoryX(src, address, disp, dftn, s, alignment);
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
   * @param address is the register containing the address of the data
   * in memory
   * @param disp specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   */
  private void storeIntoMemoryX(int          src,
                                int          address,
                                Displacement disp,
                                int          dftn,
                                int          size,
                                long         alignment)
  {
    int     r5      = registers.lastRegister(src);
    boolean isFloat = registers.floatRegister(r5);

    switch (size) {
    case 1:
      appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp, dftn, r5));
      return;

    case 2:
      if ((alignment % 2) != 0) {
        if (dftn != FT_NONE) {
          int atr = registers.newTempRegister(RegisterSet.ADRREG);
          appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
          address = atr;
          disp = getDisp(0);
          dftn = FT_NONE;
        }

        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.SRL, r5, getDisp(8), FT_NONE, tr));
        appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp, dftn, tr));
        appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp.offset(1), dftn, r5));
        return;
      }

      appendInstruction(new StoreLitInstruction(Opcodes.STH, address, disp, dftn, r5));
      return;

    case 4:
      if ((alignment % 4) != 0) {
        if (isFloat) {
          Displacement ta  = getDisp(-4);
          int          tr2 = registers.newTempRegister(RegisterSet.INTREG);
          appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, ta, FT_NONE, r5));
          appendInstruction(new LoadLitInstruction(Opcodes.LD,  SparcRegisterSet.SP_REG, ta, FT_NONE, tr2));
          r5 = tr2;
        }

        if (dftn != FT_NONE) {
          int atr = registers.newTempRegister(RegisterSet.ADRREG);
          appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
          address = atr;
          disp = getDisp(0);
          dftn = FT_NONE;
        }

        store4Bytes(r5, address, disp, dftn, alignment);
        return;
      }

      appendInstruction(new StoreLitInstruction(isFloat ? Opcodes.STF : Opcodes.ST, address, disp, dftn, r5));
      return;

    case 8:
      if (v8plus && ((alignment % 8) == 0)) {
        int opcode = Opcodes.STDF;
        if (!registers.floatRegister(src))
          opcode = v8plus ? Opcodes.STX : Opcodes.STD;

        appendInstruction(new StoreLitInstruction(opcode, address, disp, dftn, src));
        return;
      }

      if (dftn != FT_NONE) {
        int atr = registers.newTempRegister(RegisterSet.ADRREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
        address = atr;
        disp = getDisp(0);
        dftn = FT_NONE;
      }

      int src1 = registers.rangeBegin(src);
      int src2 = src1 + 1;

      if (!isFloat && v8plus) {
        src2 = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.SRLX, src1, getDisp(32), FT_NONE, src2));
        int t = src1;
        src1 = src2;
        src2 = t;
      }

      if ((alignment % 4) != 0) {
        if (isFloat) {
          Displacement ta  = getDisp(-8);
          Displacement ta4 = getDisp(-4);
          int          tr2 = registers.newTempRegister(RegisterSet.INTREG);
          int          tr3 = registers.newTempRegister(RegisterSet.INTREG);
          appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, ta,           FT_NONE, src1));
          appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, ta4, FT_NONE, src2));
          appendInstruction(new LoadLitInstruction(Opcodes.LD,   SparcRegisterSet.SP_REG, ta,           FT_NONE, tr2));
          appendInstruction(new LoadLitInstruction(Opcodes.LD,   SparcRegisterSet.SP_REG, ta4, FT_NONE, tr3));
          src1 = tr2;
          src2 = tr3;
        }

        store4Bytes(src1, address, disp,           dftn, alignment);
        store4Bytes(src2, address, disp.offset(4), dftn, alignment);
        return;
      }

      if (isFloat) {
        appendInstruction(new StoreLitInstruction(Opcodes.STF, address, disp,           dftn, src1));
        appendInstruction(new StoreLitInstruction(Opcodes.STF, address, disp.offset(4), dftn, src2));
        return;
      }

      appendInstruction(new StoreLitInstruction(Opcodes.STW, address, disp,           dftn, src1));
      appendInstruction(new StoreLitInstruction(Opcodes.STW, address, disp.offset(4), dftn, src2));
      return;

    case 16:
      if (dftn != FT_NONE) {
        int atr = registers.newTempRegister(RegisterSet.ADRREG);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
        address = atr;
        disp = getDisp(0);
        dftn = FT_NONE;
      }

      if (registers.floatRegister(src)) {
        int srcs = registers.rangeBegin(src);
        if (v8plus && ((alignment % 16) == 0)) {
          appendInstruction(new StoreLitInstruction(Opcodes.STQF, address, disp, dftn, srcs + 0));
          return;
        } else if (v8plus && ((alignment % 8) == 0)) {
          appendInstruction(new StoreLitInstruction(Opcodes.STDF, address, disp,            dftn, srcs + 0));
          appendInstruction(new StoreLitInstruction(Opcodes.STDF, address, disp.offset(8),  dftn, srcs + 2));
          return;
        }

        appendInstruction(new StoreLitInstruction(Opcodes.STF, address, disp,            dftn, srcs + 0));
        appendInstruction(new StoreLitInstruction(Opcodes.STF, address, disp.offset(4),  dftn, srcs + 1));
        appendInstruction(new StoreLitInstruction(Opcodes.STF, address, disp.offset(8),  dftn, srcs + 2));
        appendInstruction(new StoreLitInstruction(Opcodes.STF, address, disp.offset(12), dftn, srcs + 3));
        return;
      }
      break;
    }
    throw new scale.common.InternalError("Unknown data type size (" + size + ")");
  }

  private void store4Bytes(int          src,
                           int          address,
                           Displacement disp,
                           int          dftn,
                           long         alignment)
  {
    int tr = registers.newTempRegister(RegisterSet.INTREG);
    if ((alignment % 2) != 0) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, src, getDisp(24), FT_NONE, tr));
      appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp, dftn, tr));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, src, getDisp(16), FT_NONE, tr));
      appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp.offset(1), dftn, tr));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, src, getDisp(8), FT_NONE, tr));
      appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp.offset(2), dftn, tr));
      appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp.offset(3), dftn, src));
      return;
    }

    appendInstruction(new IntOpLitInstruction(Opcodes.SRL, src, getDisp(16), FT_NONE, tr));
    appendInstruction(new StoreLitInstruction(Opcodes.STH, address, disp, dftn, tr));
    appendInstruction(new StoreLitInstruction(Opcodes.STH, address, disp.offset(2), dftn, src));
    return;
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data
   * in memory
   * @param offset is the register containing the offset
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   */
  protected void storeIntoMemoryX(int  src,
                                  int  address,
                                  int  offset,
                                  int  size,
                                  long alignment)
  {
    int r5 = registers.lastRegister(src);
    switch (size) {
    case 1:
      appendInstruction(new StoreInstruction(Opcodes.STB, address, offset, r5));
      return;
    case 2:
      if ((alignment % 2) != 0)
        break;
      appendInstruction(new StoreInstruction(Opcodes.STH, address, offset, r5));
      return;
    case 4:
      if ((alignment % 4) != 0)
        break;
      int opcode4 = Opcodes.ST;
      if (registers.floatRegister(src))
        opcode4 = Opcodes.STF;

      appendInstruction(new StoreInstruction(opcode4, address, offset, r5));
      return;
    case 8:
      if ((alignment % 8) != 0)
        break;
      int opcode8 = Opcodes.STD;
      if (registers.floatRegister(src))
        opcode8 = Opcodes.STDF;

      appendInstruction(new StoreInstruction(opcode8, address, offset, src));
      return;
    }

    int tr = address;
    if (offset != 0) {
      tr = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpInstruction(Opcodes.ADD, address, offset, tr));
    }
    storeIntoMemory(src, tr, getDisp(0), FT_NONE, size, alignment);
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param disp specifies the offset from the address register value
   * @param dftn - FT_NONE, FT_LO
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
    int span = bits + bitOffset;
    int op   = signed ? Opcodes.SRA : Opcodes.SRL;

    if (span <= 8) {
      dest = registers.lastRegister(dest);
      if (signed) {
        appendInstruction(new LoadLitInstruction(Opcodes.LDSB, address, disp, dftn, dest));
        if ((bitOffset > 0) || (bits != 8)) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, getDisp(24 + bitOffset), FT_NONE, dest));
          appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, getDisp(32 - bits), FT_NONE, dest));
        }
        return;
      }
      appendInstruction(new LoadLitInstruction(Opcodes.LDUB, address, disp, dftn, dest));
      int shift = 8 - span;
      if (shift > 0)
        appendInstruction(new IntOpLitInstruction(Opcodes.SRL, dest, getDisp(shift), FT_NONE, dest));
      if (bits != 8)
        appendInstruction(new IntOpLitInstruction(Opcodes.AND, dest, getDisp((1 << bits) - 1), FT_NONE, dest));
      return;
    }

    if (span <= 16) {
      dest = registers.lastRegister(dest);
      loadFromMemoryX(dest, address, disp, dftn, 2, alignment, false);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, getDisp(16 + bitOffset), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(op, dest, getDisp(32 - bits), FT_NONE, dest));
      return;
    }

    if (span <= 32) {
      dest = registers.lastRegister(dest);
      loadFromMemoryX(dest, address, disp, dftn, 4, alignment, false);
      if (bitOffset > 0)
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, getDisp(bitOffset), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(op, dest, getDisp(32 - bits), FT_NONE, dest));
      return;
    }

    if (bits == 64) {
      loadFromMemoryX(dest, address, disp, dftn, 8, alignment, signed);
      return;
    }
    int shift = 64 - span;
    if (v8plus) {
      int tr = registers.newTempRegister(RegisterSet.INTREG);
      loadFromMemory(tr, address, disp, dftn, 8, alignment, signed);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, tr, getDisp(bitOffset), FT_NONE, tr));
      appendInstruction(new IntOpLitInstruction(signed ? Opcodes.SRAX : Opcodes.SRLX, tr, getDisp(64 - bits), FT_NONE, dest));
      return;
    }

    assert bits > 32 : "Bits (" + bits + ") <= 32";
    assert bitOffset < 32 : "Bit offset (" + bitOffset + ") >= 32";

    int tr0 = registers.newTempRegister(RegisterSet.INTREG);
    int tr1 = registers.newTempRegister(RegisterSet.INTREG);
    int tr2 = registers.newTempRegister(RegisterSet.INTREG);
    int tr3 = registers.newTempRegister(RegisterSet.INTREG);
    int eb  = bits - 32;

    if (dftn != FT_NONE) {
      int atr = registers.newTempRegister(RegisterSet.ADRREG);
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
      address = atr;
      disp = getDisp(0);
      dftn = FT_NONE;
    }

    if (shift > 0) {
      appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp,           dftn, tr0));
      appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp.offset(4), dftn, tr1));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr0, getDisp(32 - shift), FT_NONE, tr2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, tr1, getDisp(shift),      FT_NONE, tr3));
      appendInstruction(new IntOpLitInstruction(op, tr0, getDisp(shift), FT_NONE, bitOffset > 0 ? tr0 : dest));

      if (((eb > 8) || signed) && (bitOffset > 0)) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr0, getDisp(64 - bits), FT_NONE, tr0));
        appendInstruction(new IntOpLitInstruction(op, tr0, getDisp(64 - bits), FT_NONE, dest + 0));
        appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr3, dest + 1));
        return;
      }

      if (bitOffset > 0)
        appendInstruction(new IntOpLitInstruction(Opcodes.AND, tr0, getDisp((1 << eb) - 1), FT_NONE, dest + 0));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr3, dest + 1));
      return;
    }

    assert bitOffset > 0 : "Bit offset (" + bitOffset + ") <= 0";

    appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp,           dftn, dest + 0));
    appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp.offset(4), dftn, dest + 1));
    lastInstruction.specifyNotSpillLoadPoint();

    if ((eb > 8) || signed) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest + 0, getDisp(64 - bits), FT_NONE, dest + 0));
      lastInstruction.specifyNotSpillLoadPoint();
      appendInstruction(new IntOpLitInstruction(op, dest + 0, getDisp(64 - bits), FT_NONE, dest + 0));
      lastInstruction.specifyNotSpillLoadPoint();
      return;
    }

    appendInstruction(new IntOpLitInstruction(Opcodes.AND, dest + 0, getDisp((1 << eb) - 1), FT_NONE, dest + 0));
    lastInstruction.specifyNotSpillLoadPoint();
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data
   * in memory
   * @param disp specifies the offset from the address register value
   * @param dftn - FT_NONE, FT_LO
   * @param bits specifies the size of the data in bits to be loaded -
   * must be 32 or less
   * @param bitOffset specifies the offset in bits to the data
   * @param alignment specifies the alignment of the address
   */
  protected void storeBitsIntoMemory(int          src,
                                     int          address,
                                     Displacement disp,
                                     int          dftn,
                                     int          bits,
                                     int          bitOffset,
                                     int          alignment)
  {
    int span = bits + bitOffset;
    int trs  = registers.newTempRegister(RegisterSet.INTREG);
    int trd  = registers.newTempRegister(RegisterSet.INTREG);

    if (span <= 8) {
      src = registers.lastRegister(src);
      int shift = 8 - span;
      if (bits == 8) {
        appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp, dftn, src));
        return;
      }
      int mask  = ((1 << bits) - 1) << shift;
      appendInstruction(new LoadLitInstruction(Opcodes.LDUB, address, disp, dftn, trd));
      if (shift > 0) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(shift), FT_NONE, trs));
        if (mask != 0xff)
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, trs, getDisp(mask), FT_NONE, trs));
      } else
        appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, getDisp(mask), FT_NONE, trs));
      appendInstruction(new IntOpLitInstruction(Opcodes.ANDN, trd, getDisp(mask), FT_NONE, trd));
      appendInstruction(new IntOpInstruction(Opcodes.OR, trs, trd, trd));
      appendInstruction(new StoreLitInstruction(Opcodes.STB, address, disp, dftn, trd));
      return;
    }

    if (span <= 16) {
      src = registers.lastRegister(src);
      int shift = 16 - span;
      if (bits == 16) {
        storeIntoMemoryX(src, address, disp, dftn, 2, alignment);
        return;
      }
      int mask  = ((1 << bits) - 1) << shift;
      loadFromMemoryX(trd, address, disp, dftn, 2, alignment, false);
      if ((mask <= MAX_IMM13) && (mask >= MIN_IMM13)) {
        if (shift > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(shift), FT_NONE, trs));
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, trs, getDisp(mask), FT_NONE, trs));
        } else
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, getDisp(mask), FT_NONE, trs));
        appendInstruction(new IntOpLitInstruction(Opcodes.ANDN, trd, getDisp(mask), FT_NONE, trd));
      } else {
        int trm = registers.newTempRegister(RegisterSet.INTREG);
        trm = genLoadImmediate(mask, trm);
        if (shift > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(shift), FT_NONE, trs));
          appendInstruction(new IntOpInstruction(Opcodes.AND, trs, trm, trs));
        } else
          appendInstruction(new IntOpInstruction(Opcodes.AND, src, trm, trs));
        appendInstruction(new IntOpInstruction(Opcodes.ANDN, trd, trm, trd));
      }
      appendInstruction(new IntOpInstruction(Opcodes.OR, trs, trd, trd));
      storeIntoMemoryX(trd, address, disp, dftn, 2, alignment);
      return;
    }

    if (span <= 32) {
      src = registers.lastRegister(src);
      int shift = 32 - span;
      if (bits == 32) {
        storeIntoMemoryX(src, address, disp, dftn, 4, alignment);
        return;
      }
      int mask  = ((1 << bits) - 1) << shift;
      loadFromMemoryX(trd, address, disp, dftn, 4, alignment, false);
      if ((mask <= MAX_IMM13) && (mask >= MIN_IMM13)) {
        if (shift > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(shift), FT_NONE, trs));
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, trs, getDisp(mask), FT_NONE, trs));
        } else
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, getDisp(mask), FT_NONE, trs));
        appendInstruction(new IntOpLitInstruction(Opcodes.ANDN, trd, getDisp(mask), FT_NONE, trd));
      } else {
        int trm = registers.newTempRegister(RegisterSet.INTREG);
        trm = genLoadImmediate(mask, trm);
        if (shift > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(shift), FT_NONE, trs));
          appendInstruction(new IntOpInstruction(Opcodes.AND, trs, trm, trs));
        } else
          appendInstruction(new IntOpInstruction(Opcodes.AND, src, trm, trs));
        appendInstruction(new IntOpInstruction(Opcodes.ANDN, trd, trm, trd));
      }
      appendInstruction(new IntOpInstruction(Opcodes.OR, trs, trd, trd));
      storeIntoMemoryX(trd, address, disp, dftn, 4, alignment);
      return;
    }

    if (bits == 64) {
      storeIntoMemory(src, address, disp, dftn, 8, alignment);
      return;
    }

    int shift = 64 - span;
    int mask  = ((1 << bits) - 1) << shift;
    if (v8plus) {
      appendInstruction(new LoadLitInstruction(Opcodes.LDX, address, disp, dftn, trd));
      if ((mask <= MAX_IMM13) && (mask >= MIN_IMM13)) {
        if (shift > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, src, getDisp(shift), FT_NONE, trs));
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, trs, getDisp(mask), FT_NONE, trs));
        } else
          appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, getDisp(mask), FT_NONE, trs));
        appendInstruction(new IntOpLitInstruction(Opcodes.ANDN, trd, getDisp(mask), FT_NONE, trd));
      } else {
        int trm = registers.newTempRegister(RegisterSet.INTREG);
        trm = genLoadImmediate(mask, trm);
        if (shift > 0) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, src, getDisp(shift), FT_NONE, trs));
          appendInstruction(new IntOpInstruction(Opcodes.AND, trs, trm, trs));
        } else
          appendInstruction(new IntOpInstruction(Opcodes.AND, src, trm, trs));
        appendInstruction(new IntOpInstruction(Opcodes.ANDN, trd, trm, trd));
      }
      appendInstruction(new IntOpInstruction(Opcodes.OR, trs, trd, trd));
      appendInstruction(new StoreLitInstruction(Opcodes.STX, address, disp, dftn, trd));
      return;
    }

    assert bits > 32 : "Bits (" + bits + ") <= 32";
    assert bitOffset < 32 : "Bit offset (" + bitOffset + ") >= 32";

    if (dftn != FT_NONE) {
      int atr = registers.newTempRegister(RegisterSet.ADRREG);
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, address, disp, dftn, atr));
      address = atr;
      disp = getDisp(0);
      dftn = FT_NONE;
    }

    int tr0 = registers.newTempRegister(RegisterSet.INTREG);
    int tr1 = registers.newTempRegister(RegisterSet.INTREG);
    int tr2 = registers.newTempRegister(RegisterSet.INTREG);
    int tr3 = registers.newTempRegister(RegisterSet.INTREG);
    appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp,           dftn, tr0));
    appendInstruction(new LoadLitInstruction(Opcodes.LD, address, disp.offset(4), dftn, tr1));

    int eb = bits - 32;
    genLoadImmediate(((1 << eb) - 1), trs);
    appendInstruction(new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(-1), FT_NONE, trd));

    if (shift > 0) {
      Displacement dsl = getDisp(shift);
      Displacement dsr = getDisp(32 - shift);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, trs, dsl, FT_NONE, tr2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, trd, dsr, FT_NONE, tr3));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr3, trs));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, trd, dsl, FT_NONE, trd));

      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, dsl, FT_NONE, tr2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, src + 1, dsr, FT_NONE, tr3));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr3, tr2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src + 1, dsl, FT_NONE, tr3));
    }

    appendInstruction(new IntOpInstruction(Opcodes.ANDN, tr0, trs, tr0));
    appendInstruction(new IntOpInstruction(Opcodes.ANDN, tr1, trd, tr1));
    if (shift > 0) {
      appendInstruction(new IntOpInstruction(Opcodes.AND, tr2, trs, tr2));
      appendInstruction(new IntOpInstruction(Opcodes.AND, tr3, trd, tr3));
    } else {
      appendInstruction(new IntOpInstruction(Opcodes.AND, src + 0, trs, tr2));
      appendInstruction(new IntOpInstruction(Opcodes.AND, src + 1, trd, tr3));
    }
    appendInstruction(new IntOpInstruction(Opcodes.OR, tr0, tr2, tr0));
    appendInstruction(new IntOpInstruction(Opcodes.OR, tr1, tr3, tr1));

    appendInstruction(new StoreLitInstruction(Opcodes.STW, address, disp,           dftn, tr0));
    appendInstruction(new StoreLitInstruction(Opcodes.STW, address, disp.offset(4), dftn, tr1));
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

    loadFromMemory(dest, resultReg, getDisp(off), FT_NONE, size, alignment, signed);

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

    int        srr   = resultReg;
    ResultMode srrha = resultRegMode;
    long       srrof = resultRegAddressOffset;
    int        off   = (int) genLoadHighImmediate(offset.getDisplacement(), address);

    loadFromMemory(dest, resultReg, getDisp(off), FT_NONE, size, alignment, signed);

    resultRegAddressOffset = srrof;
    resultRegMode = srrha;
    resultReg = srr;
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

    storeIntoMemory(src, resultReg, getDisp(off), FT_NONE, size, alignment);

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

    int        srr   = resultReg;
    ResultMode srrha = resultRegMode;
    long       srrof = resultRegAddressOffset;
    int        off   = (int) genLoadHighImmediate(offset.getDisplacement(), address);

    storeIntoMemory(src, resultReg, getDisp(off), FT_NONE, size, alignment);

    resultRegAddressOffset = srrof;
    resultRegMode = srrha;
    resultReg = srr;
  }

  /**
   * Generate instructions to store data into memory at the address
   * specified by a register.
   * @param src is the source register
   * @param address is the register containing the address of the data
   * in memory
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real - this
   * argument is not used in this architecture and will be ignored
   */
  protected void storeIntoMemory(int     src,
                                 int     address,
                                 int     size,
                                 long    alignment,
                                 boolean real)
  {
    storeIntoMemory(src, address, getDisp(0), FT_NONE, size, alignment);
  }

  /**
   * Load a register from a symbolic location in memory.
   * @param dest is the register
   * @param dsize is the size of the value in addressable memory units
   * @param isSigned is true if the value in the register is signed
   * @param isReal is true if the value in the register is a floating
   * point value
   * @param disp specifies the location
   */
  protected void loadRegFromSymbolicLocation(int          dest,
                                             int          dsize,
                                             boolean      isSigned,
                                             boolean      isReal,
                                             Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

    int adr = loadAddressHigh(disp);

    loadFromMemory(dest, adr, disp, FT_LO, dsize, 4, isSigned);
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
    int  bs = ct.memorySizeAsInt(machine);

    if (ct.isRealType()) {
      if (ct.isComplexType()) {
        doComplexOp(which, bs, la, ra, ir);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }

      needValue(la);
      int laReg = resultReg;

      needValue(ra);
      int raReg = resultReg;

      int opcode = Opcodes.fopOp(which, bs);

      assert (opcode != 0) : "Invalid opcode";

      appendInstruction(new FltOp2Instruction(opcode, laReg, raReg, ir));
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    // Integer operations

    boolean  flag   = false;
    long     value  = 0;
    int opcode = Opcodes.iopOp(which, bs);

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
      } else if (lit instanceof SizeofLiteral) {
        value = valueOf((SizeofLiteral) lit);
        flag = true;
      }

      if (flag && (value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        switch (which) {
        case ADD:
          if (value == 0) {
            resultRegAddressOffset = 0;
            resultRegMode = ResultMode.NORMAL_VALUE;
            resultReg = laReg;
            return;
          }
          genAdditionLit(laReg, value, ir, bs, ct.isSigned());
          return;
        case SUB:
          if (value == 0) {
            resultRegAddressOffset = 0;
            resultRegMode = ResultMode.NORMAL_VALUE;
            resultReg = laReg;
            return;
          }
          genSubtractionLit(laReg, value, ir, bs, ct.isSigned());
          return;
        case MUL:
          genMultiplyLit(laReg, value, ir, bs, ct.isSigned());
          return;
        case DIV:
          genDivideLit(laReg, value, ir, bs, ct.isSigned());
          return;
        case AND:
          genAndLit(laReg, value, ir, bs, ct.isSigned());
          return;
        case OR:
          if (value == 0) {
            resultRegAddressOffset = 0;
            resultRegMode = ResultMode.NORMAL_VALUE;
            resultReg = laReg;
            return;
          }
          genOrLit(laReg, value, ir, bs, ct.isSigned());
          return;
        case XOR:
          genXorLit(laReg, value, ir, bs, ct.isSigned());
          return;
        case SRA:
          genShiftRightLit(laReg, value, ir, bs);
          return;
        case SRL:
          genShiftRightLogicalLit(laReg, value, ir, bs);
          return;
        case SLL:
          genShiftLeftLogicalLit(laReg, value, ir, bs);
          return;
        }
        return;
      }
    }

    needValue(ra);
    int raReg = resultReg;

    switch (which) {
    case ADD:
      genAddition(laReg, raReg, ir, bs, ct.isSigned());
      break;
    case SUB:
      genSubtraction(laReg, raReg, ir, bs, ct.isSigned());
      break;
    case MUL:
      genMultiply(laReg, raReg, ir, bs, ct.isSigned());
      break;
    case DIV:
      genDivide(laReg, raReg, ir, bs, ct.isSigned());
      break;
    case AND:
      genAnd(laReg, raReg, ir, bs, ct.isSigned());
      break;
    case OR:
      genOr(laReg, raReg, ir, bs, ct.isSigned());
      break;
    case XOR:
      genXor(laReg, raReg, ir, bs, ct.isSigned());
      break;
    case SRA:
      genShiftRight(laReg, raReg, ir, bs);
      break;
    case SRL:
      genShiftRightLogical(laReg, raReg, ir, bs);
      break;
    case SLL:
      genShiftLeftLogical(laReg, raReg, ir, bs);
      break;
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private Displacement defFloatValue(double v, int size)
  {
    int section = RODATA;
    int type    = SpaceAllocation.DAT_FLT;
    int aln     = 4;

    if (size > 4) {
      if (size > 8) {
        type = SpaceAllocation.DAT_LDBL;
        aln = 8;
      } else {
        type = SpaceAllocation.DAT_DBL;
        aln = 8;
      }
    }

    Displacement disp = findAreaDisp(section, type, true, size, v, aln);

    if (disp == null) {
      String name   = un.genName();
      int    handle = allocateData(name, section, type, size, true, new Double(v), 1, aln);
      disp   = new SymbolDisplacement(name, handle);
      associateDispWithArea(handle, disp);
    }

    return disp;
  }

  private Displacement defLongValue(long v, int size)
  {
    int section = RODATA;
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

  protected Displacement defStringValue(String v, int size)
  {
    int          section = RODATA;
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
   * Called at the start of code generation for a routine.
   */
  protected Instruction startRoutineCode()
  {
    currentBeginMarker = new BeginMarker(scribble);

    return currentBeginMarker;
  }

  /**
   * Generate instructions to move a 4-byte (or less) value from one
   * register to another.  Transfers between integer and floating
   * point registers are done through memory.
   */
  protected void move04(int src, int dest)
  {
    boolean p = registers.pairRegister(src);

    if (!registers.floatRegister(src)) {
      if (!registers.floatRegister(dest)) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, src + 0, dest + 0));
        if (p)
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, src + 1, dest + 1));
        return;
      } 

      Displacement to0 = getDisp(-4);
      appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to0, FT_NONE, src + 0));
      appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to0, FT_NONE, dest + 0));
      if (p) {
        appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to0, FT_NONE, src + 1));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to0, FT_NONE, dest + 1));
      }
      return;
    }

    if (registers.floatRegister(dest)) {
      appendInstruction(new FltOpInstruction(Opcodes.FMOVS, src + 0, dest + 0));
      if (p)
        appendInstruction(new FltOpInstruction(Opcodes.FMOVS, src + 1, dest + 1));
      return;
    }

    Displacement to0 = getDisp(-4);
    appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to0, FT_NONE, src + 0));
    appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to0, FT_NONE, dest + 0));
    if (p) {
      appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to0, FT_NONE, src + 1));
      appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to0, FT_NONE, dest + 1));
    }
  }

  /**
   * Generate instructions to move a 8-byte value from one register to
   * another on a V8 processor.  Transfers between integer and
   * floating point registers are done through memory.
   */
  private void move08v8(int src, int dest)
  {
    int srcs  = registers.rangeBegin(src);
    int dests = registers.rangeBegin(dest);

    if (srcs == dests)
      return;

    boolean p = registers.pairRegister(src);

    if (!registers.floatRegister(src)) {
      if (!registers.floatRegister(dest)) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 0, dests + 0));
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 1, dests + 1));
        if (p) {
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 2, dests + 2));
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 3, dests + 3));
        }
        return;
      } 

      Displacement to0 = getDisp(-8);
      Displacement to4 = getDisp(-4);
      appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 0));
      appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to4, FT_NONE, srcs + 1));
      appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 0));
      appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to4, FT_NONE, dests + 1));
      if (p) {
        appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 2));
        appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to4, FT_NONE, srcs + 3));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 2));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to4, FT_NONE, dests + 3));
      }
      return;
    }

    if (registers.floatRegister(dest)) {
      appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 0, dests + 0));
      appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 1, dests + 1));
      if (p) {
        appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 2, dests + 2));
        appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 3, dests + 3));
      }
      return;
    }

    Displacement to0 = getDisp(-8);
    Displacement to4 = getDisp(-4);
    appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 0));
    appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to4, FT_NONE, srcs + 1));
    appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 0));
    appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to4, FT_NONE, dests + 1));
    if (p) {
      appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 2));
      appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to4, FT_NONE, srcs + 3));
      appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 2));
      appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to4, FT_NONE, dests + 3));
    }
  }

  /**
   * Generate instructions to move a 8-byte value from one register to
   * another on a V9 processor.  Transfers between integer and
   * floating point registers are done through memory.
   */
  private void move08v8plus(int src, int dest)
  {
    int srcs  = registers.rangeBegin(src);
    int dests = registers.rangeBegin(dest);
    int srce  = registers.rangeEnd(src);
    int deste = registers.rangeEnd(dest);

    if (srcs == dests)
      return;

    boolean p = registers.pairRegister(src);

    if (!registers.floatRegister(src)) {
      if (!registers.floatRegister(dest)) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 0, dests + 0));
        if (p) {
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 1, dests + 1));
        }
        return;
      } 

      Displacement to0 = getDisp(-8);
      appendInstruction(new StoreLitInstruction(Opcodes.STX, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 0));
      appendInstruction(new LoadLitInstruction(Opcodes.LDDF, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 0));
      if (p) {
        appendInstruction(new StoreLitInstruction(Opcodes.STX, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 2));
        appendInstruction(new LoadLitInstruction(Opcodes.LDDF, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 2));
      }
      return;
    }

    if (registers.floatRegister(dest)) {
      appendInstruction(new FltOpInstruction(Opcodes.FMOVD, srcs + 0, dests + 0));
      if (p) {
        appendInstruction(new FltOpInstruction(Opcodes.FMOVD, srcs + 2, dests + 2));
      }
      return;
    }

    Displacement to0 = getDisp(-8);
    Displacement to4 = getDisp(-4);
    appendInstruction(new StoreLitInstruction(Opcodes.STDF, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 0));
    appendInstruction(new LoadLitInstruction(Opcodes.LDX, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 0));
    if (p) {
      appendInstruction(new StoreLitInstruction(Opcodes.STDF, SparcRegisterSet.SP_REG, to0, FT_NONE, srcs + 2));
      appendInstruction(new LoadLitInstruction(Opcodes.LDX, SparcRegisterSet.SP_REG, to0, FT_NONE, dests + 2));
    }
  }

  /**
   * Generate instructions to move a 16-byte value from one register
   * to another on a V8 processor.  Transfers between integer and
   * floating point registers are done through memory.
   */
  private void move16v8(int src, int dest)
  {
    int srcs  = registers.rangeBegin(src);
    int dests = registers.rangeBegin(dest);

    if (srcs == dests)
      return;

    boolean p = registers.pairRegister(src);

    if (!registers.floatRegister(src)) {
      if (!registers.floatRegister(dest)) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 0, dests + 0));
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 1, dests + 1));
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 2, dests + 2));
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 3, dests + 3));
        if (p) {
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 4, dests + 4));
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 5, dests + 5));
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 6, dests + 6));
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 7, dests + 7));
        }
        return;
      } 

      Displacement to00 = getDisp(-16);
      Displacement to04 = getDisp(-12);
      Displacement to08 = getDisp(-8);
      Displacement to12 = getDisp(-4);
      appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 0));
      appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to04, FT_NONE, srcs + 1));
      appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to08, FT_NONE, srcs + 2));
      appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to12, FT_NONE, srcs + 3));
      appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 0));
      appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to04, FT_NONE, dests + 1));
      appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to08, FT_NONE, dests + 2));
      appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to12, FT_NONE, dests + 3));
      if (p) {
        appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 4));
        appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to04, FT_NONE, srcs + 5));
        appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to08, FT_NONE, srcs + 6));
        appendInstruction(new StoreLitInstruction(Opcodes.STW, SparcRegisterSet.SP_REG, to12, FT_NONE, srcs + 7));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 4));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to04, FT_NONE, dests + 5));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to08, FT_NONE, dests + 6));
        appendInstruction(new LoadLitInstruction(Opcodes.LDF, SparcRegisterSet.SP_REG, to12, FT_NONE, dests + 7));
      }
      return;
    } 

    if (registers.floatRegister(dest)) {
      appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 0, dests + 0));
      appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 1, dests + 1));
      appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 2, dests + 2));
      appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 3, dests + 3));
      if (p) {
        appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 4, dests + 4));
        appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 5, dests + 5));
        appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 6, dests + 6));
        appendInstruction(new FltOpInstruction(Opcodes.FMOVS, srcs + 7, dests + 7));
      }
      return;
    }

    Displacement to00 = getDisp(-16);
    Displacement to04 = getDisp(-12);
    Displacement to08 = getDisp(-8);
    Displacement to12 = getDisp(-4);
    appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 0));
    appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to04, FT_NONE, srcs + 1));
    appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to08, FT_NONE, srcs + 2));
    appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to12, FT_NONE, srcs + 3));
    appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 0));
    appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to04, FT_NONE, dests + 1));
    appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to08, FT_NONE, dests + 2));
    appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to12, FT_NONE, dests + 3));
    if (p) {
      appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 4));
      appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to04, FT_NONE, srcs + 5));
      appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to08, FT_NONE, srcs + 6));
      appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.SP_REG, to12, FT_NONE, srcs + 7));
      appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 4));
      appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to04, FT_NONE, dests + 5));
      appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to08, FT_NONE, dests + 6));
      appendInstruction(new LoadLitInstruction(Opcodes.LDSW, SparcRegisterSet.SP_REG, to12, FT_NONE, dests + 7));
    }
  }

  /**
   * Generate instructions to move a 16-byte value from one register
   * to another on a V9 processor.  Transfers between integer and
   * floating point registers are done through memory.
   */
  private void move16v8plus(int src, int dest)
  {
    int srcs  = registers.rangeBegin(src);
    int dests = registers.rangeBegin(dest);

    if (srcs == dests)
      return;

    boolean p = registers.pairRegister(src);

    if (!registers.floatRegister(src)) {
      if (!registers.floatRegister(dest)) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 0, dests + 0));
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 1, dests + 1));
        if (p) {
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 2, dests + 2));
          appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, srcs + 3, dests + 3));
        }
        return;
      } 

      Displacement to00 = getDisp(-16);
      Displacement to08 = getDisp(-8);
      appendInstruction(new StoreLitInstruction(Opcodes.STX, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 0));
      appendInstruction(new StoreLitInstruction(Opcodes.STX, SparcRegisterSet.SP_REG, to08, FT_NONE, srcs + 1));
      appendInstruction(new LoadLitInstruction(Opcodes.LDQF, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 0));
      if (p) {
        appendInstruction(new StoreLitInstruction(Opcodes.STX, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 2));
        appendInstruction(new StoreLitInstruction(Opcodes.STX, SparcRegisterSet.SP_REG, to08, FT_NONE, srcs + 3));
        appendInstruction(new LoadLitInstruction(Opcodes.LDQF, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 4));
      }
      return;
    } 

    if (registers.floatRegister(dest)) {
      appendInstruction(new FltOpInstruction(Opcodes.FMOVQ, srcs + 0, dests + 0));
      if (p) {
        appendInstruction(new FltOpInstruction(Opcodes.FMOVQ, srcs + 4, dests + 4));
      }
      return;
    }

    Displacement to00 = getDisp(-16);
    Displacement to04 = getDisp(-12);
    Displacement to08 = getDisp(-8);
    Displacement to12 = getDisp(-4);
    appendInstruction(new StoreLitInstruction(Opcodes.STQF, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 0));
    appendInstruction(new LoadLitInstruction(Opcodes.LDX, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 0));
    appendInstruction(new LoadLitInstruction(Opcodes.LDX, SparcRegisterSet.SP_REG, to08, FT_NONE, dests + 1));
    if (p) {
      appendInstruction(new StoreLitInstruction(Opcodes.STQF, SparcRegisterSet.SP_REG, to00, FT_NONE, srcs + 4));
      appendInstruction(new LoadLitInstruction(Opcodes.LDX, SparcRegisterSet.SP_REG, to00, FT_NONE, dests + 2));
      appendInstruction(new LoadLitInstruction(Opcodes.LDX, SparcRegisterSet.SP_REG, to08, FT_NONE, dests + 3));
    }
  }

  /**
   * Generate instructions to move data from one register to another.
   * If one is an integer register and the other is a floating point
   * register, a memory location may be required.
   */
  protected void genRegToReg(int src, int dest)
  {
    assert (src >= 0) : "Negative source register " + src + " to " + dest;

    if (registers.rangeBegin(src) == registers.rangeBegin(dest))
      return;

    int srcSize = registers.registerSize(src);

    if (v8plus) {
      switch (srcSize) {
      case 1:
      case 2:
      case 4:  move04(src, dest); return;
      case 8:  move08v8plus(src, dest); return;
      case 16: move16v8plus(src, dest); return;
      }
      throw new scale.common.InternalError("Unknown size " + srcSize);
    }

    switch (srcSize) {
    case 1:
    case 2:
    case 4:  move04(src, dest); return;
    case 8:  move08v8(src, dest); return;
    case 16: move16v8(src, dest); return;
    }
    throw new scale.common.InternalError("Unknown size " + srcSize);
  }

  /**
   *  Generate an add of address registers <code>laReg</codeand
   *  <code>raReg</code>.
   */
  protected void addRegs(int laReg, int raReg, int dest)
  {
    appendInstruction(new IntOpInstruction(Opcodes.ADD, laReg, raReg, dest));
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
  protected int genRealToInt(int     src,
                             int     srcSize,
                             int     dest,
                             int     destSize,
                             boolean destSigned)
  {
    int ft2 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, destSize));
    int it  = dest;

    if (!destSigned)
      it = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, destSize));

    if ((srcSize <= 8) && ((destSize <= 4) || v8plus)) {
      Displacement      disp  = srcSize <= 4 ? defLongValue(0x4f000000, 4) : defLongValue(0x41e0000000000000L, 8);
      int               adr   = loadAddressHigh(disp);
      int               tr1   = registers.newTempRegister(RegisterSet.FLTREG + ((srcSize > 4) ? RegisterSet.DBLEREG : 0));
      int               tr2   = registers.newTempRegister(RegisterSet.INTREG);
      Label             labf  = createLabel();
      Label             labt  = createLabel();
      Label             labj  = createLabel();
      LabelDisplacement dispt = new LabelDisplacement(labt);
      SparcInstruction  test  = new FltCmpInstruction(Opcodes.cmpfOp(srcSize), src, tr1, FCC0);

      loadFromMemory(tr1, adr, disp, FT_LO, srcSize, 0, true);

      test.setSetCC(FCC0);
      appendInstruction(test);

      if (!v8plus)
        appendInstruction(new SethiInstruction(Opcodes.NOP, 0, getDisp(0), FT_NONE));
 
      SparcInstruction delay = new FltOp2Instruction(Opcodes.fopOp(SUB, srcSize), src, tr1, src);
      SparcBranch      inst  = new BranchInstruction(v8plus ? Opcodes.FBPUGE : Opcodes.FBUGE,
                                                     dispt,
                                                     true,
                                                     2,
                                                     delay);
      AnnulMarker      am    = new AnnulMarker(delay);

      inst.addTarget(labf, 0);
      inst.addTarget(labt, 1);
      labf.setNotReferenced();

      appendInstruction(inst);
      appendLabel(labf);
      appendInstruction(am);

      appendInstruction(new FltOpInstruction(Opcodes.fcvtiOp(srcSize, destSize), src, ft2));
      genRegToReg(ft2, it);
      generateUnconditionalBranch(labj);
      appendLabel(labt);
      appendInstruction(new FltOpInstruction(Opcodes.fcvtiOp(srcSize, destSize), src, ft2));
      genRegToReg(ft2, it);
      genLoadImmediate(0x80000000, SparcRegisterSet.G0_REG, tr2);
      appendInstruction(new IntOpInstruction(Opcodes.OR, it, tr2, it));
      appendLabel(labj);
    } else if (destSize <= 8) {
      if (srcSize > 8) {
        localVarSize += srcSize;
        Displacement to0 = new StackDisplacement(-localVarSize);
        storeIntoMemory(src, SparcRegisterSet.FP_REG, to0, FT_NONE, srcSize, 0);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD,
                                                  SparcRegisterSet.FP_REG,
                                                  to0,
                                                  FT_NONE,
                                                  SparcRegisterSet.O0_REG));
        String ftn = "_Q_qtoll";
        if (destSize < 8) {
          if (!destSigned)
            ftn = "_Q_qtou";
          else
            ftn = "_Q_qtoi";
        } else if (!destSigned)
          ftn = "_Q_qtoull";
        genFtnCall(ftn, singleIntUses, createRoutineDefs(true, destSize));
        genRegToReg(returnRegister(registers.getType(dest), true), it);
      } else if (srcSize == 8) {
        genRegToReg(src, SparcRegisterSet.O0_REG);
        if (destSigned)
          genFtnCall(destSize > 4 ? "__dtoll" : "__dtol",
                     doubleIntUses,
                     createRoutineDefs(true, destSize));
        else 
          genFtnCall(destSize > 4 ? "__dtoull" : "__dtoul",
                     doubleIntUses,
                     createRoutineDefs(true, destSize));
        genRegToReg(returnRegister(registers.getType(dest), true), it);
      } else {
        genRegToReg(src, SparcRegisterSet.O0_REG);
        if (destSigned)
          genFtnCall(destSize > 4 ? "__ftoll" : "__ftol",
                     doubleIntUses,
                     createRoutineDefs(true, destSize));
        else
          genFtnCall(destSize > 4 ? "__ftoull" : "__ftoul",
                     doubleIntUses,
                     createRoutineDefs(true, destSize));
        genRegToReg(returnRegister(registers.getType(dest), true), it);
      }
    }

    if (it == dest)
      return dest;

    return convertIntRegValue(it, destSize, true, dest, destSize, destSigned);
  }

  /**
   * Convert a real value in a real register to a 
   * real value in a real register.
   */
  protected void genRealToReal(int src, int srcSize, int dest, int destSize)
  {
    appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(srcSize, destSize), src, dest));
  }

  /**
   * Convert an integer value in an integer register to a 
   * real value in a real register.
   */
  protected void genIntToReal(int src, int srcSize, int dest, int destSize)
  {
    if ((srcSize <= 4) || (v8plus && (srcSize <= 8))) {
      int treg = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG,
                                                                      srcSize));
      genRegToReg(src, treg);
      appendInstruction(new FltOpInstruction(Opcodes.icvtfOp(srcSize,
                                                             destSize),
                                             treg,
                                             dest));
      return;
    }

    if (srcSize <= 8) {
      int tr = dest;
      if (destSize <= 8)
        tr = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.QUADREG);

      int adr = registers.newTempRegister(RegisterSet.ADRREG);
      genRegToReg(src, SparcRegisterSet.O0_REG);
      localVarSize += Machine.alignTo(destSize, 8);
      genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, adr);
      appendInstruction(new StoreLitInstruction(Opcodes.STW,
                                                SparcRegisterSet.SP_REG,
                                                getDisp(RETURN_STRUCT_OFFSET),
                                                FT_NONE,
                                                adr));
      JmplLitInstruction jsr = (JmplLitInstruction) genFtnCall("_Q_lltoq",
                                                               doubleIntUses,
                                                               createRoutineDefs(true, 16));
      jsr.setReturnedStructSize(16);
      loadFromMemory(tr,
                     SparcRegisterSet.FP_REG,
                     getDisp(-localVarSize),
                     FT_NONE,
                     16,
                     4,
                     true);

      if (tr != dest)
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(16, destSize), tr, dest));

      return;
    }

    throw new NotImplementedError("Conversion of " + srcSize + "-byte integer to real");
  }

  /**
   * Convert an unsigned integer value in an integer register to a 
   * real value in a real register.
   */
  protected void genUnsignedIntToReal(int src, int srcSize, int dest, int destSize)
  {
    if (srcSize <= 4) {
      int          treg  = registers.newTempRegister(RegisterSet.DBLEREG + RegisterSet.FLTREG);
      int          treg2 = registers.newTempRegister(RegisterSet.DBLEREG + RegisterSet.FLTREG);
      Displacement disp  = defLongValue(0X4330000000000000L, 8);
      int          adr   = loadAddressHigh(disp);

      loadFromMemory(treg, adr, disp, FT_LO, 8, 0, true);
      move04(treg, treg2);
      move04(src, treg2 + 1);

      if (destSize != 8) {
        appendInstruction(new FltOp2Instruction(Opcodes.fopOp(SUB, 8), treg2, treg, treg));
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(8, destSize), treg, dest));
      } else
        appendInstruction(new FltOp2Instruction(Opcodes.fopOp(SUB, 8), treg2, treg, dest));
      return;
    }

    if (srcSize <= 8) {
      int tr = dest;
      if (destSize <= 8)
        tr = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.QUADREG);

      int adr = registers.newTempRegister(RegisterSet.ADRREG);
      genRegToReg(src, SparcRegisterSet.O0_REG);
      localVarSize += 16;
      Displacement to0 = new StackDisplacement(-localVarSize);
      genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, adr);
      appendInstruction(new StoreLitInstruction(Opcodes.STW,
                                                SparcRegisterSet.SP_REG,
                                                getDisp(RETURN_STRUCT_OFFSET),
                                                FT_NONE,
                                                adr));
      JmplLitInstruction jsr = (JmplLitInstruction) genFtnCall("_Q_ulltoq",
                                                               doubleIntUses,
                                                               createRoutineDefs(true, destSize));
      jsr.setReturnedStructSize(16);
      loadFromMemory(tr, SparcRegisterSet.FP_REG, getDisp(-localVarSize), FT_NONE, 16, 4, true);

      if (tr != dest)
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(16, destSize), tr, dest));

      return;
    }

    throw new scale.common.NotImplementedError("unsigned 64-bit integer to real");
  }

  /**
   * Create a DefRegisters marker to specify the registers used
   * to return the value of the routine.
   */
  protected short[] createRoutineDefs(boolean isReal, int bs)
  {
    short[] defs = null;
    if (isReal) {
      if (bs > SAVED_REG_SIZE)
        defs = doubleDefs;
      else
        defs = fltDefs;
    } else if (bs <= SAVED_REG_SIZE)
      defs = intDefs;
    else
      defs = longLongDefs;

    return defs;
  }

  protected Branch genFtnCall(String name, short[] uses, short[] defs)
  {
    int                handle = allocateTextArea(name, TEXT);
    Displacement       disp   = new SymbolDisplacement(name, handle);
    Label              lab    = createLabel();
    int                cr     = registers.newTempRegister(RegisterSet.ADRREG);
    SparcInstruction   sethi  = new SethiInstruction(Opcodes.SETHI, cr, disp, FT_HI);
    JmplLitInstruction jsr    = new JmplLitInstruction(cr, disp, FT_LO, SparcRegisterSet.O7_REG, 1, null);

    jsr.additionalRegsUsed(uses);
    jsr.additionalRegsKilled(registers.getCalleeUses());
    jsr.additionalRegsSet(defs);
    jsr.addTarget(lab, 0);
    jsr.markAsCall();

    lastInstruction.specifySpillStorePoint();
    appendInstruction(sethi);
    appendInstruction(jsr);
    appendLabel(lab);
    lab.markAsFirstInBasicBlock();
    lab.setNotReferenced();
    return jsr;
  }

  /**
   * Generate instructions to compute the floor of a real vaue in a
   * real register to a real register.
   */
  protected void genFloorOfReal(int src, int srcSize, int dest, int destSize)
  {
    StringBuffer buf = new StringBuffer("__aint");

    if (destSize <= 4)
      buf.append('f');

    genRegToReg(src, SparcRegisterSet.O0_REG);
    genFtnCall(buf.toString(), doubleFltUses, createRoutineDefs(true, destSize));
    genRegToReg(returnRegister(registers.getType(dest), false), dest);
  }

  /**
   * Convert real value in a real register to a rounded real value in
   * a real register.  The result is rounded to the nearest integer.
   */
  protected void genRoundReal(int src, int srcSize, int dest, int destSize)
  {
    StringBuffer buf = new StringBuffer("__anint");

    if (destSize <= 4)
      buf.append('f');

    genRegToReg(src, SparcRegisterSet.O0_REG);
    genFtnCall(buf.toString(), doubleFltUses, createRoutineDefs(true, destSize));
    genRegToReg(returnRegister(registers.getType(dest), false), dest);
  }

  /**
   * Convert integer value in a real register to an integer value in a
   * real register.  The result is rounded to the nearest integer.
   */
  protected void genRealToIntRound(int src, int srcSize, int dest, int destSize)
  {
    int               ft    = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);
    SparcInstruction  delay = new FltOpInstruction(Opcodes.FNEGS, ft, ft);
    SparcBranch       br    = new BranchInstruction(Opcodes.FBL, disp, true, 2, delay);
    AnnulMarker       am    = new AnnulMarker(delay);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);

    labf.setNotReferenced();

    genLoadDblImmediate(0.0, ft, srcSize);
    appendInstruction(new FltCmpInstruction(Opcodes.cmpfOp(srcSize), src, ft, FCC0));
    genLoadDblImmediate(0.5, ft, srcSize);
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
    appendInstruction(new FltOp2Instruction(Opcodes.fopOp(ADD, srcSize), ft, src, ft));
    appendInstruction(new FltOpInstruction(Opcodes.fcvtiOp(srcSize, destSize), ft, dest));
  }

  /**
   * Generate code to zero out a floating point register.
   */
  protected void zeroFloatRegister(int dest, int destSize)
  {
    genLoadDblImmediate(0.0, dest, destSize);
  }

  protected void processSourceLine(int line, Label lab, boolean newLine)
  {
    if (line < 0)
      return;

    String fileName = currentRoutine.getCallGraph().getName();
    appendInstruction(new SparcLineMarker(fileName, line));

    if (stabs == null)
      return;

    if (lab == null) {
      lab = createLabel();
      appendLabel(lab);
    }

    lab.setReferenced();

    LabelDisplacement ld = new LabelDisplacement(lab);
    DiffDisplacement  dd = new DiffDisplacement(ld, ftnDisp);
    stabs.addStabn(Stabs.N_SLINE, 0, line, dd);
  }

  /**
   * Generate an unconditional branch to the label specified.
   */
  public void generateUnconditionalBranch(Label lab)
  {
    LabelDisplacement disp = new LabelDisplacement(lab);
    Branch            inst = new BranchInstruction(Opcodes.BA, disp, false, 1, null);

    inst.addTarget(lab, 0);
    appendInstruction(inst);
  }

  /**
   * Generate an instruction sequence to move words from one location
   * to another.  The destination offset must not be symbolic.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move.
   * @param aln is the alignment that can be assumed for both the
   * source and destination addresses
   */
  protected void moveWords(int          src,
                           long         srcoff,
                           int          dest,
                           Displacement destoff,
                           int          size,
                           int          aln)
  {
    assert destoff.isNumeric() : "Symbolic displacement " + destoff;

    moveWords(src, srcoff, dest, destoff.getDisplacement(), size, aln);
    return;
  }

  /**
   * Generate an instruction sequence to move words from one location
   * to another.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move.
   * @param aln is the alignment that can be assumed for both the
   * source and destination addresses
   */
  protected void moveWords(int  src,
                           long srcoff,
                           int  dest,
                           long destoff,
                           int  size,
                           int  aln)
  {
    int ms  = 4;
    int lop = Opcodes.LD;
    int sop = Opcodes.ST;

    if ((v8plus) && ((aln & 0x7) == 0) && ((size & 0x7) == 0)) {
      ms = 8;
      lop = Opcodes.LDX;
      sop = Opcodes.STX;
    } else if (((aln & 0x3) != 0) || (size & 0x3) != 0) {
      ms  = 2;
      lop = Opcodes.LDUH;
      sop = Opcodes.STH;
      if (((aln & 0x1) != 0) || (size & 0x1) != 0) {
        ms  = 1;
        lop = Opcodes.LDUB;
        sop = Opcodes.STB;
      }
    }

    if (size <= (ms * 5)) { // Generate straight-line load-stores to move data.
      int soffset = 0;
      if ((srcoff >= (MAX_IMM13 - size)) || (srcoff <= (MIN_IMM13 + size))) {
        int sr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(srcoff, src, sr);
        src = sr;
      } else
         soffset = (int) srcoff;

      int nexsr = 0;
      for (int k = 0; k < size; k += ms) { // Generate loads.
        Displacement odisp = getDisp(soffset);
        nxtMvReg[nexsr] = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadLitInstruction(lop, src, odisp, FT_NONE, nxtMvReg[nexsr]));
        soffset += ms;
        nexsr++;
      }

      int doffset = 0;
      if ((destoff >= (MAX_IMM13 - size)) || (destoff <= (MIN_IMM13 + size))) {
        int dr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(destoff, dest, dr);
        dest = dr;
      } else
          doffset = (int) destoff;

      int nexdr = 0;
      for (int k = 0; k < size; k += ms) { // Generate stores.
        Displacement odisp = getDisp(doffset);
        appendInstruction(new StoreLitInstruction(sop, dest, odisp, FT_NONE, nxtMvReg[nexdr]));
        doffset += ms;
        nexdr++;
      }

      return;
    }

    // Generate loop to move data.

    if (srcoff != 0) {
      int tr = registers.newTempRegister(RegisterSet.ADRREG);
      genLoadImmediate(srcoff, src, tr);
      src = tr;
    }

    if (destoff != 0) {
      int tr = registers.newTempRegister(RegisterSet.ADRREG);
      genLoadImmediate(destoff, dest, tr);
      dest = tr;
    }

    int               valreg  = registers.newTempRegister(RegisterSet.INTREG);
    int               testreg = registers.newTempRegister(RegisterSet.INTREG);
    int               increg  = registers.newTempRegister(RegisterSet.INTREG);
    Label             labl    = createLabel();
    Label             labn    = createLabel();
    Displacement      odisp   = getDisp(0);
    LabelDisplacement ldisp   = new LabelDisplacement(labl);
    Displacement      ddisp   = getDisp(ms);
    SparcInstruction  inc     = new IntOpLitInstruction(Opcodes.ADD, increg, ddisp, FT_NONE, increg);
    Branch            br      = new BranchInstruction(Opcodes.BG, ldisp, false, 2, inc);
    SparcInstruction  test    = new IntOpLitInstruction(Opcodes.SUBCC, testreg, ddisp, FT_NONE, testreg);

    br.addTarget(labl, 0);
    br.addTarget(labn, 1);
    test.setSetCC(ICC);

    appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, increg));
    genLoadImmediate(size, testreg);
    appendLabel(labl);
    appendInstruction(new LoadInstruction(lop, src, increg, valreg));
    appendInstruction(test);
    appendInstruction(new StoreInstruction(sop, dest, increg, valreg));
    appendInstruction(br);
    appendLabel(labn);
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

    localVarSize += Machine.alignTo(registers.registerSize(reg), 8);
    return new StackDisplacement(-localVarSize);
  }

  /**
   * Load the arguments into registers for a routine call.  Only the
   * first six words of arguements are placed into registers.  The
   * remaining words are placed on the stack.
   */
  protected short[] callArgs(Expr[] rtArgs, boolean specialFirstArg)
  {
    boolean allSimple = true; // True if only simple arguments;
    for (int i = 0; i < rtArgs.length; i++) {
      Expr rtArg = rtArgs[i];

      if (isSimple(rtArg))
        continue;

      allSimple = false;
      break;
    }

    int[]   src  = new int[MAX_ARG_REGS];   // These arrays must be allocated each time 
    short[] dest = new short[MAX_ARG_REGS]; // because of possible recursion of callArgs.

    int   rk          = 0;
    int   firstArgReg = specialFirstArg ? 1 : 0;
    int   nextArg     = firstArgReg;
    int   numUses     = 0;
    int   offset      = 0;
    int   ai          = 0;

    while (nextArg < MAX_ARG_REGS) {
      if (ai >= rtArgs.length)
        break;

      Expr arg    = rtArgs[ai++];
      Type vt     = processType(arg);
      int  size   = vt.memorySizeAsInt(machine);
      int  nexr   = SparcRegisterSet.O0_REG + nextArg;
      int  stkpos = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;

      if (trace)
        System.out.println("CFA: " + arg);

      if (vt.isAtomicType()) {
        int rinc = (size + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;
        if (rinc > 2) { // Argument is passed in memory, address of argument is passed on the stack.
          putAddressInRegister(arg);

          nextArg++;
          numUses++;

          // Postpone transfer because of things like divide.

          src[rk] = resultReg;
          dest[rk] = (byte) nexr;
          rk++;
          continue;
        }

        boolean flg = allSimple && ((nextArg + rinc - 1) < MAX_ARG_REGS);

        if (flg)
          registers.setResultRegister(nexr);

        needValue(arg);

        if (flg)
          registers.setResultRegister(-1); // test

        int argV = resultReg;
        if (resultRegMode == ResultMode.ADDRESS) {
          size = SAVED_REG_SIZE;
          nextArg++;
          numUses++;
          src[rk] = argV;
          dest[rk] = (byte) nexr;
          rk++;
          continue;
        }

        if (v8plus && (rinc == 2) && ((nextArg + rinc - 1) < MAX_ARG_REGS)) {
          int tr1  = argV;
          int tr2  = argV + 1;
          if ((argV != nexr) || registers.floatRegister(argV)) {
            if (allSimple) {
              tr1 = nexr;
              tr2 = nexr + 1;
            } else {
              tr1  = registers.newTempRegister(RegisterSet.INTREG);
              tr2  = registers.newTempRegister(RegisterSet.INTREG);
            }
          }

          if (registers.floatRegister(argV)) {
            Displacement to0 = getDisp(-8);
            appendInstruction(new StoreLitInstruction(Opcodes.STD, SparcRegisterSet.SP_REG, to0, FT_NONE, argV));
            appendInstruction(new LoadLitInstruction(Opcodes.LD, SparcRegisterSet.SP_REG, to0, FT_NONE, tr1));
            appendInstruction(new LoadLitInstruction(Opcodes.LD, SparcRegisterSet.SP_REG, to0.offset(4), FT_NONE, tr2));
          } else {
            appendInstruction(new IntOpLitInstruction(Opcodes.SRL, argV, getDisp(0), FT_NONE, tr2));
            appendInstruction(new IntOpLitInstruction(Opcodes.SRLX, argV, getDisp(32), FT_NONE, tr1));
          }
          argV = tr1;
        }
        
        if ((nextArg + rinc - 1) >= MAX_ARG_REGS) { // Argument is passed on the stack.
          storeIntoMemory(argV, SparcRegisterSet.SP_REG, getDisp(stkpos), FT_NONE, size, stkpos);

          offset += Machine.alignTo(size, SAVED_REG_SIZE);

          // Argument may need to be split between the stack and the registers

          if ((rinc > 1) && (nextArg < MAX_ARG_REGS)) {
            int areg = SparcRegisterSet.O0_REG + nextArg;
            int tir  = allSimple ? areg : registers.newTempRegister(RegisterSet.INTREG);
            if (registers.floatRegister(argV))
              loadFromMemory(tir, SparcRegisterSet.SP_REG, getDisp(stkpos), FT_NONE, SAVED_REG_SIZE, 4, true);
            else if (v8plus)
              appendInstruction(new IntOpLitInstruction(Opcodes.SRLX, argV, getDisp(32), FT_NONE, tir));
            else if (argV != tir)
              appendInstruction(new IntOpInstruction(Opcodes.OR, argV, SparcRegisterSet.G0_REG, tir));

            src[rk] = tir;
            dest[rk] = (byte) areg;
            numUses++;
            rk++;
          }

          nextArg += rinc;
          continue;
        }

        nextArg += rinc;
        numUses += rinc;

        // Postpone transfer because of things like divide.

        if (v8plus && (rinc == 2)) {
          for (int ii = 0; ii < rinc; ii++) {
            src[rk] = argV + ii;
            dest[rk] = (byte) (nexr + ii);
            rk++;
          }
          continue;
        }

        src[rk] = argV;
        dest[rk] = (byte) nexr;
        rk++;
        continue;
      }

      if (vt.isAggregateType()) {
        int destR = (allSimple && (nextArg < MAX_ARG_REGS)) ? nexr : registers.newTempRegister(RegisterSet.ADRREG);

        localVarSize += Machine.alignTo(size, 8);

        arg.visit(this);
        int  address   = resultReg;
        long addressof = resultRegAddressOffset;

        assert (resultRegMode == ResultMode.ADDRESS) : "Huh " + arg;

        genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, destR);
        moveWords(address, addressof, destR, 0, size, resultRegAddressAlignment);
        nextArg++;
        numUses++;

        // Postpone transfer because of things like divide.

        src[rk] = destR;
        dest[rk] = (byte) nexr;
        rk++;
        continue;
      }

      assert (vt.isArrayType()) : "Argument type " + arg;

      LoadDeclValueExpr ldve = (LoadDeclValueExpr) arg;
      VariableDecl      ad   = (VariableDecl) ldve.getDecl();

      if (allSimple)
        registers.setResultRegister(nexr);

      putAddressInRegister(ad, false);

      if (allSimple)
        registers.setResultRegister(-1);

      nextArg++;
      numUses++;

      // Postpone transfer because of things like divide.

      src[rk] = resultReg;
      dest[rk] = (byte) nexr;
      rk++;
    }

    while (ai < rtArgs.length) {
      Expr arg    = rtArgs[ai++];
      Type vt     = processType(arg);
      int  size   = vt.memorySizeAsInt(machine);
      int  rinc   = 1;
      int  stkpos = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;

      if (vt.isAtomicType()) {
        rinc = (size + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;
        if (rinc > 2) { // Argument is passed in memory.
          putAddressInRegister(arg);
          storeIntoMemory(resultReg, SparcRegisterSet.SP_REG, getDisp(stkpos), FT_NONE, SAVED_REG_SIZE, stkpos);
          offset += Machine.alignTo(size, SAVED_REG_SIZE);
          nextArg++;
          continue;
        }

        needValue(arg);
        int argV = resultReg;

        if (resultRegMode == ResultMode.ADDRESS) {
          size = SAVED_REG_SIZE;
          storeIntoMemory(argV, SparcRegisterSet.SP_REG, getDisp(stkpos), FT_NONE, SAVED_REG_SIZE, stkpos);
          offset += Machine.alignTo(size, SAVED_REG_SIZE);
          nextArg++;
          continue;
        }

        storeIntoMemory(argV, SparcRegisterSet.SP_REG, getDisp(stkpos), FT_NONE, size, stkpos);
        offset += rinc * SAVED_REG_SIZE;
        nextArg += rinc;
        continue;
      }

      if (vt.isAggregateType()) {
        int destR = registers.newTempRegister(RegisterSet.ADRREG);

        localVarSize += Machine.alignTo(size, 8);

        arg.visit(this);
        int  address   = resultReg;
        long addressof = resultRegAddressOffset;

        assert (resultRegMode == ResultMode.ADDRESS) : "Huh " + arg;

        genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, destR);
        moveWords(address, addressof, destR, 0, size, resultRegAddressAlignment);
        storeIntoMemory(destR, SparcRegisterSet.SP_REG, getDisp(stkpos), FT_NONE, SAVED_REG_SIZE, stkpos);
        offset += SAVED_REG_SIZE;
        nextArg++;
        continue;
      }

      assert (vt.isArrayType()) : "Argument type " + arg;

      LoadDeclValueExpr ldve = (LoadDeclValueExpr) arg;
      VariableDecl      ad   = (VariableDecl) ldve.getDecl();

      putAddressInRegister(ad, false);

      storeIntoMemory(resultReg, SparcRegisterSet.SP_REG, getDisp(stkpos), FT_NONE, SAVED_REG_SIZE, stkpos);
      offset += SAVED_REG_SIZE;
      nextArg++;
    }

    // We must do the copy to the argument register in two passes because
    // the Sparc uses %o0 as both the value return register and an
    // argument register.

    for (int i = 0; i < rk; i++) {
      int reg = src[i];
      if (reg == SparcRegisterSet.O0_REG)
        genRegToReg(reg, dest[i]);
    }

    for (int i = 0; i < rk; i++) {
      int reg = src[i];
      if (reg != SparcRegisterSet.O0_REG)
        genRegToReg(reg, dest[i]);
    }

    if (offset > argsOnStackSize)
      argsOnStackSize = offset;

    lastInstruction.specifySpillStorePoint(); // Because flt. point registers can not be saved over a call.

    return genUses(firstArgReg + numUses);
  }

  private short[] genUses(int numUses)
  {
    short[] usedRegs = new short[numUses + 1];
    int ui = 0;
    for (int k = 0; k < numUses; k++) {
      usedRegs[ui] = (short) (SparcRegisterSet.O0_REG + ui);
      ui++;
    }
    usedRegs[ui] = SparcRegisterSet.SP_REG;

    return usedRegs;
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    Type ct  = processType(e);
    Expr arg = e.getArg();
    int  ir  = registers.getResultRegister(ct.getTag());

    assert !ct.isRealType() : "Bit complement not allowed on " + arg;

    needValue(arg);
    int src = resultReg;

    genXorLit(src, -1, ir, registers.registerSize(src), false);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
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
        if (value == 1) { // X**1
          genRegToReg(resultReg, ir);
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }

        if (value == 2) { // X**2
          if (cmplx)
            doComplexOp(MUL, bs, resultReg, resultReg, ir);
          else {
            if (flt)
              appendInstruction(new FltOp2Instruction(Opcodes.fopOp(MUL, bs), resultReg, resultReg, ir));
            else
              genMultiply(resultReg, resultReg, ir, bs, signed);
          }
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
        int opcode = Opcodes.fopOp(MUL, bs);
        if (value == 3) { // X**3
          if (cmplx) {
            int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
            doComplexOp(MUL, bs, resultReg, resultReg, tr);
            doComplexOp(MUL, bs, resultReg, tr, ir);
          } else {
            int tr = registers.newTempRegister(ct.getTag());
            if (flt) {
              appendInstruction(new FltOp2Instruction(opcode, resultReg, resultReg, tr));
              appendInstruction(new FltOp2Instruction(opcode, resultReg, tr, ir));
            } else {
              genMultiply(resultReg, resultReg, tr, bs, signed);
              genMultiply(resultReg, tr, ir, bs, signed);
            }
          }
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
        if (cmplx) {
          int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
          doComplexOp(MUL, bs, resultReg, resultReg, ir);
          for (int i = 0; i < value - 2; i++)
            doComplexOp(MUL, bs, resultReg, tr, tr);
          doComplexOp(MUL, bs, resultReg, tr, ir);
        } else {
          int tr = registers.newTempRegister(ct.getTag());
          if (flt) {
            appendInstruction(new FltOp2Instruction(opcode, resultReg, resultReg, tr));
            for (int i = 0; i < value - 3; i++)
              appendInstruction(new FltOp2Instruction(opcode, resultReg, tr, tr));
            appendInstruction(new FltOp2Instruction(opcode, resultReg, tr, ir));
          } else {
            genMultiply(resultReg, resultReg, tr, bs, signed);
            for (int i = 0; i < value - 3; i++)
              genMultiply(resultReg, tr, tr, bs, signed);
            genMultiply(resultReg, tr, ir, bs, signed);
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
      genRegToReg(laReg, SparcRegisterSet.O0_REG);
      genRegToReg(raReg, SparcRegisterSet.O2_REG);
      genFtnCall("pow", quadIntUses, createRoutineDefs(flt, bs));
      genRealToReal(SparcV8RegisterSet.D0_REG, 8, ir, bs);
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
    genFtnCall(fname, uses, createRoutineDefs(flt, bs));
    genRegToReg(returnRegister(ct.getTag(), true), ir);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    Type ct = processType(e);
    int  bs = ct.memorySizeAsInt(machine);
    Expr ra = e.getRightArg();
    Expr la = e.getLeftArg();

    if (ct.isRealType()) {
      doBinaryOp(e, DIV);
      return;
    }

    int ir = registers.getResultRegister(ct.getTag());
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
        needValue(la);
        genDivideLit(resultReg, value, ir, bs, ct.isSigned());
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    needValue(la);
    int laReg = resultReg;
    needValue(ra);
    int raReg = resultReg;
    genDivide(laReg, raReg, ir, bs, ct.isSigned());

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitRemainderExpr(RemainderExpr e)
  {
    Type ct = processType(e);
    Expr la = e.getLeftArg();
    Expr ra = e.getRightArg();
    int  ir = registers.getResultRegister(ct.getTag());
    int  bs = ct.memorySizeAsInt(machine);

    if (ct.isRealType()) {
      Type lt  = processType(la);
      int  lts = lt.memorySizeAsInt(machine);
      Type rt  = processType(ra);
      int  rts = rt.memorySizeAsInt(machine);

      String fname  = "fmod";
      if ((bs != 8) ||
          !lt.isRealType() ||
          !rt.isRealType() ||
          (lts != 8) ||
          (rts != 8)) {
        StringBuffer ftn   = new StringBuffer("_scale_mod");
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
      genFtnCall(fname, uses, createRoutineDefs(true, bs));
      genRegToReg(returnRegister(ct.getTag(), true), ir);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    boolean signed = ct.isSigned();

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
        needValue(la);
        int rt  = registers.getType(ir);
        int tr1 = registers.newTempRegister(rt);
        int tr2 = registers.newTempRegister(rt);
        genDivideLit(resultReg, value, tr1, bs, ct.isSigned());
        genMultiplyLit(tr1, value, tr2, bs, signed);
        genSubtraction(resultReg, tr2, ir, bs, ct.isSigned());
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    boolean      ll  = bs >= 8;
    StringBuffer buf = new StringBuffer(ll ? "__" : ".");
    if (!ct.isSigned())
      buf.append('u');
    buf.append("rem");
    if (ll)
      buf.append("64");

    short[] uses = callArgs(e.getOperandArray(), false);
    genFtnCall(buf.toString(), uses, createRoutineDefs(false, ct.memorySizeAsInt(machine)));
    genRegToReg(returnRegister(ct.getTag(), true), ir);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  /**
   * Generate a function return.
   */
  public void generateReturn(short[] uses)
  {
    ProcedureType pt = (ProcedureType) currentRoutine.getCoreType();
    Type          rt = pt.getReturnType().getCoreType();
    int           os = 8;

    if (!rt.isVoidType()) {
      int rts = rt.memorySizeAsInt(machine);
      if ((rt.isAggregateType() || (rts > 8)) && !isFortran())
        os = 12;
    }

    SparcInstruction rest = new IntOpInstruction(Opcodes.RESTORE,
                                                 SparcRegisterSet.G0_REG,
                                                 SparcRegisterSet.G0_REG,
                                                 SparcRegisterSet.G0_REG);
    SparcBranch      inst = new JmplLitInstruction(SparcRegisterSet.I7_REG,
                                                   getDisp(os),
                                                   FT_NONE,
                                                   SparcRegisterSet.G0_REG,
                                                   0,
                                                   rest);
    inst.additionalRegsUsed(uses);
    returnInst = inst;
    appendInstruction(inst);
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

    if (!ft.isAtomicType()) {
      resultRegAddressAlignment = (((fieldOffset & 0x3) == 0) ? 4 : 1);
      resultRegMode = ResultMode.ADDRESS;
      return;
    }

    Displacement disp;
    if ((fieldOffset >= MIN_IMM13) && (fieldOffset <= MAX_IMM13))
      disp = getDisp((int) fieldOffset);
    else {
      int tr = registers.newTempRegister(RegisterSet.ADRREG);
      genLoadImmediate(fieldOffset, adr, tr);
      disp = getDisp(0);
      adr = tr;
    }

    int alignment = naln ? 1 : ((adraln < fa) ? adraln : fa);
    if (bits == 0)
      loadFromMemory(dest, adr, disp, FT_NONE, byteSize, alignment, ft.isSigned());
    else
      loadBitsFromMemory(dest, adr, disp, FT_NONE, bits, bitOffset, alignment, ft.isSigned());

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = dest;
  }

  private CompareMode genCC(CompareMode which, int laReg, int raReg, int bs)
  {
    if ((bs <= 4) || ((bs <= 8) && v8plus)) {
      SparcInstruction test = new IntOpInstruction(Opcodes.SUBCC, laReg, raReg, SparcRegisterSet.G0_REG);
      test.setSetCC(bs <= 4 ? ICC : XCC);
      appendInstruction(test);
    } else if ((which == CompareMode.EQ) || (which == CompareMode.NE)) {
      int tr1 = registers.newTempRegister(RegisterSet.INTREG);
      int tr2 = registers.newTempRegister(RegisterSet.INTREG);

      SparcInstruction t2 = new IntOpInstruction(Opcodes.XOR, laReg + 0, raReg + 0, tr1);
      SparcInstruction t1 = new IntOpInstruction(Opcodes.XOR, laReg + 1, raReg + 1, tr2);
      SparcInstruction t3 = new IntOpInstruction(Opcodes.ORCC, tr1, tr2, SparcRegisterSet.G0_REG);

      appendInstruction(t1);
      appendInstruction(t2);
      t3.setSetCC(ICC);
      appendInstruction(t3);
    } else {
      int op1 = Opcodes.SUBCC;
      int op2 = Opcodes.SUBCCC;
      if (which == CompareMode.GT) {
        which = CompareMode.GE;
        SparcInstruction t0 = new IntOpLitInstruction(Opcodes.SUBCC,
                                                      SparcRegisterSet.G0_REG,getDisp(1),
                                                      FT_NONE,
                                                      SparcRegisterSet.G0_REG);
        t0.setSetCC(ICC);
        appendInstruction(t0);
        op1 = Opcodes.SUBCCC;
      } else if (which == CompareMode.LE) {
        which = CompareMode.LT;
        SparcInstruction t0 = new IntOpLitInstruction(Opcodes.SUBCC,
                                                      SparcRegisterSet.G0_REG,getDisp(1),
                                                      FT_NONE,
                                                      SparcRegisterSet.G0_REG);
        t0.setSetCC(ICC);
        appendInstruction(t0);
        op1 = Opcodes.SUBCCC;
      }

      SparcInstruction t1 = new IntOpInstruction(op1, laReg + 1, raReg + 1, SparcRegisterSet.G0_REG);
      SparcInstruction t2 = new IntOpInstruction(op2, laReg + 0, raReg + 0, SparcRegisterSet.G0_REG);

      t1.setSetCC(ICC);
      appendInstruction(t1);
      t2.setSetCC(ICC);
      appendInstruction(t2);
      lastInstruction.specifyNotSpillLoadPoint();
    }

    return which;
  }

  private CompareMode genCCLit(CompareMode which, int laReg, long iv, int bs)
  {
    if ((bs <= 4) || ((bs <= 8) && v8plus)) {
      SparcInstruction test = new IntOpLitInstruction(Opcodes.SUBCC,
                                                      laReg,
                                                      getDisp((int) iv),
                                                      FT_NONE,
                                                      SparcRegisterSet.G0_REG);
      test.setSetCC(bs <= 4 ? ICC : XCC);
      appendInstruction(test);
      return which;
    }

    if ((which == CompareMode.EQ) || (which == CompareMode.NE)) {
      if (iv == 0) {
        SparcInstruction t3 = new IntOpInstruction(Opcodes.ORCC,
                                                   laReg,
                                                   laReg + 1,
                                                   SparcRegisterSet.G0_REG);
        t3.setSetCC(ICC);
        appendInstruction(t3);
        return which;
      }

      int tr1 = registers.newTempRegister(RegisterSet.INTREG);
      int tr2 = registers.newTempRegister(RegisterSet.INTREG);
      SparcInstruction t1 = new IntOpLitInstruction(Opcodes.XOR,
                                                    laReg + 0,
                                                    getDisp((int) (iv >> 32)),
                                                    FT_NONE,
                                                    tr1);
      SparcInstruction t2 = new IntOpLitInstruction(Opcodes.XOR,
                                                    laReg + 1,
                                                    getDisp((int) iv),
                                                    FT_NONE, tr2);
      SparcInstruction t3 = new IntOpInstruction(Opcodes.ORCC,
                                                 tr1,
                                                 tr2,
                                                 SparcRegisterSet.G0_REG);

      appendInstruction(t1);
      appendInstruction(t2);

      t3.setSetCC(ICC);
      appendInstruction(t3);
      return which;
    }

    int op1 = Opcodes.SUBCC;
    int op2 = Opcodes.SUBCCC;
    if (which == CompareMode.GT) {
      which = CompareMode.GE;
      SparcInstruction t0 = new IntOpLitInstruction(Opcodes.SUBCC,
                                                    SparcRegisterSet.G0_REG,
                                                    getDisp(1),
                                                    FT_NONE,
                                                    SparcRegisterSet.G0_REG);
      t0.setSetCC(ICC);
      appendInstruction(t0);
      op1 = Opcodes.SUBCCC;
    } else if (which == CompareMode.LE) {
      which = CompareMode.LT;
      SparcInstruction t0 = new IntOpLitInstruction(Opcodes.SUBCC,
                                                    SparcRegisterSet.G0_REG,
                                                    getDisp(1),
                                                    FT_NONE,
                                                    SparcRegisterSet.G0_REG);
      t0.setSetCC(ICC);
      appendInstruction(t0);
      op1 = Opcodes.SUBCCC;
    }

    SparcInstruction t1 = new IntOpLitInstruction(op1,
                                                  laReg + 1,
                                                  getDisp((int) iv),
                                                  FT_NONE,
                                                  SparcRegisterSet.G0_REG);
    SparcInstruction t2 = new IntOpLitInstruction(op2,
                                                  laReg + 0,
                                                  getDisp((int) (iv >> 32)),
                                                  FT_NONE,
                                                  SparcRegisterSet.G0_REG);

    t1.setSetCC(ICC);
    appendInstruction(t1);
    t2.setSetCC(ICC);
    appendInstruction(t2);
    lastInstruction.specifyNotSpillLoadPoint();

    return which;
  }

  /**
   * Generate a branch based on a relational expression.
   * @param rflag true if the test condition should be reversed
   * @param predicate specifies the relational expression
   * @param tc specifies the path if the test succeeds
   * @param fc specifies the path if the test fails
   */
  protected void genIfRelational(boolean   rflag,
                                 MatchExpr predicate,
                                 Chord     tc,
                                 Chord     fc)
  {
    CompareMode which            = predicate.getMatchOp();
    Expr        la               = predicate.getLeftArg();
    Expr        ra               = predicate.getRightArg();
    boolean     flag             = false;
    boolean     fflag            = false;
    boolean     genUnconditional = false;
    boolean     sense            = doNext(tc);

    if (rflag)
      which = which.reverse();

    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
      which = which.argswap();
    }

    Type lt = processType(la);
    long iv = 0;
    if (lt.isRealType())
      fflag = true;
    else if (ra.isLiteralExpr()) {
      LiteralExpr le  = (LiteralExpr) ra;
      Literal     lit = le.getLiteral();

      if (lit instanceof BooleanLiteral) {
        BooleanLiteral bl = (BooleanLiteral) lit;
        flag = true;
        if (bl.getBooleanValue())
          which = which.argswap();
      } else if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        iv = il.getLongValue();
        if ((iv >= MIN_IMM13) && (iv <= MAX_IMM13))
          flag = true;
      } else if (lit instanceof SizeofLiteral) {
        iv = valueOf((SizeofLiteral) lit);
        if ((iv >= MIN_IMM13) && (iv <= MAX_IMM13))
          flag = true;
      }
    }

    if (flag && (iv == 0)) { // Test against zero.
      genIfRegister(which, la, tc, fc);
      return;
    }

    // Generate comparison

    needValue(la);
    int laReg = resultReg;
    int raReg = 0;
    if (!flag) {
      needValue(ra);
      raReg = resultReg;
    }

    Label labt = getBranchLabel(tc);
    Label labf = getBranchLabel(fc);

    if (!sense) {
      if (!lt.isComplexType() && doNext(fc)) {
        which = which.reverse();
        Label t = labt;
        labt = labf;
        labf = t;
      } else
        genUnconditional = true;
    }

    int               bs   = lt.memorySizeAsInt(machine);
    LabelDisplacement disp = new LabelDisplacement(labf);
    if (fflag) {
      int opcode = Opcodes.bopfOp(which.reverse().ordinal(), v8plus);
      if (lt.isComplexType()) {
        bs = bs / 2;
        int              laRegP = laReg + registers.numContiguousRegisters(laReg);
        int              raRegP = raReg + registers.numContiguousRegisters(raReg);
        SparcInstruction test   = new FltCmpInstruction(Opcodes.cmpfOp(bs), laRegP, raRegP, FCC0);
        Label            labm   = createLabel();
        SparcBranch      inst   = new BranchInstruction(opcode, disp, false, 2, null);

        test.setSetCC(FCC0);
        appendInstruction(test);

        if (!v8plus)
          appendInstruction(new SethiInstruction(Opcodes.NOP, 0, getDisp(0), FT_NONE));

        inst.addTarget(labf, 0);
        inst.addTarget(labm, 1);
        labm.setNotReferenced();

        appendInstruction(inst);
        appendLabel(labm);
      }
      SparcInstruction test = new FltCmpInstruction(Opcodes.cmpfOp(bs), laReg, raReg, FCC0);
      test.setSetCC(FCC0);
      appendInstruction(test);

      if (!v8plus)
        appendInstruction(new SethiInstruction(Opcodes.NOP, 0, getDisp(0), FT_NONE));
 
      SparcBranch inst = new BranchInstruction(opcode, disp, false, 2, null);

      inst.addTarget(labf, 0);
      inst.addTarget(labt, 1);

      lastInstruction.specifySpillStorePoint();

      appendInstruction(inst);

      if (genUnconditional)
        generateUnconditionalBranch(labt);
      return;
    }

    which = which.reverse();

    if (flag)
      which = genCCLit(which, laReg, iv, bs);
    else
      which = genCC(which, laReg, raReg, bs);

    int         opcode = Opcodes.bopiOp(which.ordinal(), v8plus, lt.isSigned());
    SparcBranch br     = null;
    if (v8plus)
      br = new BranchCCInstruction(opcode, bs <= 4 ? ICC : XCC , disp, false, 2, null);
    else
      br = new BranchInstruction(opcode, disp, false, 2, null);

    br.addTarget(labf, 0);
    br.addTarget(labt, 1);

    lastInstruction.specifySpillStorePoint();

    appendInstruction(br);

    if (genUnconditional)
      generateUnconditionalBranch(labt);
  }

  public void visitNotExpr(NotExpr e)
  {
    Type ct  = processType(e);
    Expr arg = e.getArg();
    int  ir  = registers.getResultRegister(ct.getTag());

    needValue(arg);
    int src = resultReg;
    assert !registers.floatRegister(src) : "Not not allowed on " + arg;

    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);
    SparcInstruction  delay = new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(1), FT_NONE, ir);
    SparcBranch       br    = new BranchInstruction(Opcodes.BE, disp, true, 2, delay);
    AnnulMarker       am    = new AnnulMarker(delay);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);

    labf.setNotReferenced();

    appendInstruction(new IntOpInstruction(Opcodes.ORCC, src, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG));
    appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, ir));
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  /**
   * Calculate the element index specified.  Return the offset from
   * the address in <code>resultRegAddressOffset</code>.  resultReg is
   * set to the register containing the index value.
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

      if (off == SparcRegisterSet.G0_REG) {
        resultReg = ind;
        return;
      }

      if (negOff) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new IntOpInstruction(Opcodes.SUB, ind, off, tr));
        resultReg = tr;
        return;
      }

      if (ind == SparcRegisterSet.G0_REG) {
        resultReg = off;
        return;
      }
      
      int tr = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpInstruction(Opcodes.ADD, ind, off, tr));
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
        appendInstruction(new IntOpInstruction(Opcodes.SUB, SparcRegisterSet.G0_REG, resultReg, tr2));
        resultReg = tr2;
      }
      resultRegAddressOffset = ival;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    default:
    case 3:
      resultRegAddressOffset = oval + ival;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = SparcRegisterSet.G0_REG;
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
    boolean     signed  = et.isSigned();
    int         aln     = vt.getPointedTo().alignment(machine);

    calcAddressAndOffset(array, 0);
    long       offseta = resultRegAddressOffset;
    int        arr     = resultReg;
    ResultMode arrha   = resultRegMode;

    calcArrayOffset(offset, index);
    long offseth = resultRegAddressOffset;
    int  tr      = resultReg;

    long off = offseta + offseth * bs;
    if (tr == SparcRegisterSet.G0_REG) {
      if ((off < MAX_IMM13) && (off > MIN_IMM13)) {
        loadFromMemory(dest, arr, getDisp((int) off), FT_NONE, bs, aln, signed);
        return;
      }
      int tr2 = registers.newTempRegister(RegisterSet.INTREG);
      genLoadImmediate(off, arr, tr2);
      loadFromMemory(dest, tr2, getDisp(0), FT_NONE, bs, aln, signed);
      return;
    } 

    if ((off <= MAX_IMM13) && (off >= MIN_IMM13)) {
      int tr3 = registers.newTempRegister(RegisterSet.INTREG);
      int tr4 = tr;
      if (bs != 1) {
        genMultiplyLit(tr, bs, tr3, registers.registerSize(tr), true);
        tr4 = tr3;
      }
      appendInstruction(new IntOpInstruction(Opcodes.ADD, tr4, arr, tr3));
      loadFromMemoryWithOffset(dest, tr3, off, bs, aln, signed, false);
      return;
    }

    int tr2 = tr;
    if (offseth != 0) {
      tr2 = registers.newTempRegister(RegisterSet.INTREG);
      genLoadImmediate(offseth, tr, tr2);
    }

    int tr3 = tr2;
    if (bs != 1) {
      tr3 = registers.newTempRegister(RegisterSet.INTREG);
      genMultiplyLit(tr2, bs, tr3, registers.registerSize(tr), true);
    }

    if (offseta == 0) {
      loadFromMemoryDoubleIndexing(dest, arr, tr3, bs, aln, signed, false);
      return;
    }

    int tr4 = registers.newTempRegister(RegisterSet.INTREG);
    appendInstruction(new IntOpInstruction(Opcodes.ADD, tr3, arr, tr4));
    loadFromMemoryWithOffset(dest, tr4, offseta, bs, aln, signed, false);
  }

  /**
   * Load the address of an array element into a register.  Return the
   * offset from the address in <code>resultRegAddressOffset</code>.
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

    if (tr == SparcRegisterSet.G0_REG) {
      resultRegAddressOffset += offseth * bs;
      resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
      return;
    }

    offseta = resultRegAddressOffset + bs * offseth;
    int arr = resultReg;

    if (bs == 1) {
      appendInstruction(new IntOpInstruction(Opcodes.ADD, arr, tr, ir));
      resultRegAddressOffset = offseta;
      resultRegMode = ResultMode.ADDRESS_VALUE;
      resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
      resultReg = ir;
      return;
    }

    int tr3 = registers.newTempRegister(RegisterSet.INTREG);
    genMultiplyLit(tr, bs, tr3, registers.registerSize(tr), true);
    appendInstruction(new IntOpInstruction(Opcodes.ADD, arr, tr3, ir));
    resultRegAddressOffset = offseta;
    resultRegMode = ResultMode.ADDRESS_VALUE;
    resultRegAddressAlignment = naln ? 1 : et.getCoreType().alignment(machine);
    resultReg = ir;
    return;
  }

  public void visitMultiplicationExpr(MultiplicationExpr e)
  { 
    Type    ct     = processType(e);
    int     bs     = ct.memorySizeAsInt(machine);
    Expr    ra     = e.getRightArg();
    Expr    la     = e.getLeftArg();
    boolean signed = ct.isSigned();

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
          resultReg = SparcRegisterSet.G0_REG;
          return;
        }
        int ir = registers.getResultRegister(ct.getTag());
        needValue(la);
        genMultiplyLit(resultReg, value, ir, bs, signed);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    doBinaryOp(e, MUL);
  }

  protected void genAlloca(Expr arg, int reg)
  {
    int tr = registers.newTempRegister(RegisterSet.AIREG);

    // The displaement value used will be adjusted by endRoutineCode
    // to accommodate the size of the largest argument passing area.

    if (allocaDisp == null)
      allocaDisp = new IntegerDisplacement(96);

    if (arg.isLiteralExpr()) {
      Object lit = ((LiteralExpr) arg).getLiteral().getConstantValue();
      if (lit instanceof IntLiteral) {
        long size = (((IntLiteral) lit).getLongValue() + 7) & -8;
        if ((size >= 0) && (size <= MAX_IMM13)) {
          appendInstruction(new IntOpLitInstruction(Opcodes.SUB,
                                                    SparcRegisterSet.SP_REG,
                                                    getDisp((int) size),
                                                    FT_NONE,
                                                    SparcRegisterSet.SP_REG));
          appendInstruction(new IntOpLitInstruction(Opcodes.ADD,
                                                    SparcRegisterSet.SP_REG,
                                                    allocaDisp,
                                                    FT_NONE,
                                                    reg));
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = reg;
          return;
        }
      }
    }

    needValue(arg);

    appendInstruction(new IntOpLitInstruction(Opcodes.ADD, resultReg, getDisp(7), FT_NONE, tr));
    appendInstruction(new IntOpLitInstruction(Opcodes.AND, tr, getDisp(-8), FT_NONE, tr));
    appendInstruction(new IntOpInstruction(Opcodes.SUB, SparcRegisterSet.SP_REG, tr, SparcRegisterSet.SP_REG));
    appendInstruction(new IntOpLitInstruction(Opcodes.ADD, SparcRegisterSet.SP_REG, allocaDisp, FT_NONE, reg));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = reg;
  }

  private void genFtnCall(String fname, int dest, int src, Type type)
  {
    int     bs    = type.memorySizeAsInt(machine);
    boolean flt   = registers.floatRegister(dest);
    boolean cmplx = type.isComplexType();

    if (!cmplx) {
      if (flt && (bs <= 4)) {
        StringBuffer buf = new StringBuffer("__");
        buf.append(fname);
        buf.append('f');
        fname = buf.toString();
      }
      genRegToReg(src, SparcRegisterSet.O0_REG);
      genFtnCall(fname, genUses(bs / 4), createRoutineDefs(true, bs));
      genRegToReg(returnRegister(type.getTag(), true), dest);
      return;
    }

    // The called transcendental function is written in C and, as
    // such, can not return a complex value in the registers.  The
    // called function expects a "struct" argument and returns a
    // "struct" result.  We can eliminate this logic when we
    // handle complex value types in C and re-write the "scale_"
    // functions.  Or, when we write the complex transcendental
    // function in TIL.

    StringBuffer buf = new StringBuffer("_scale_");
    buf.append(fname);
    buf.append(bs < 16 ? 'c' : 'z');
    fname = buf.toString();

    // Store argument as if it were a struct.

    localVarSize += Machine.alignTo(bs, 8);
    genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, SparcRegisterSet.O0_REG);

    storeIntoMemory(src, SparcRegisterSet.O0_REG, getDisp(0), FT_NONE, bs, 8);

    // Create place for the result.

    if (complexDisp == null) {
      localVarSize += Machine.alignTo(16, 8);
      complexDisp = new StackDisplacement(-localVarSize);
    }
    int trr = loadStackAddress(complexDisp);
    appendInstruction(new StoreLitInstruction(Opcodes.STW,
                                              SparcRegisterSet.SP_REG,
                                              getDisp(RETURN_STRUCT_OFFSET),
                                              FT_NONE,
                                              trr));

    lastInstruction.specifySpillStorePoint(); // Because flt. point registers can not be saved over a call.

    JmplLitInstruction jsr = (JmplLitInstruction) genFtnCall(fname,
                                                             singleIntUses,
                                                             createRoutineDefs(false, 4));
    jsr.setReturnedStructSize(bs);

    loadFromMemory(dest, SparcRegisterSet.O0_REG, getDisp(0), FT_NONE, bs, 8, true);
  }

  protected void genSqrtFtn(int dest, int src, Type type)
  {
    if (type.isComplexType()) {
      genFtnCall("sqrt", dest, src, type);
      return;
    }

    int bs = type.memorySizeAsInt(machine);
    appendInstruction(new FltOpInstruction(Opcodes.fsqrtOp(bs), src, dest));
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
    int dest1 = dest + registers.numContiguousRegisters(dest);
    genRegToReg(src, dest);
    appendInstruction(new FltOpInstruction(Opcodes.FNEGS, dest1, dest1));
  }

  protected void genReturnAddressFtn(int dest, int src, Type type)
  {
    if (src == SparcRegisterSet.G0_REG) {
      appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.I7_REG, SparcRegisterSet.I7_REG, dest));
      return;
    }

    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);
    SparcInstruction  delay = new IntOpInstruction(Opcodes.OR, SparcRegisterSet.I7_REG, SparcRegisterSet.I7_REG, dest);
    SparcBranch       br    = new BranchInstruction(Opcodes.BE, disp, true, 2, delay);
    AnnulMarker       am    = new AnnulMarker(delay);
    SparcInstruction  test  = new IntOpInstruction(Opcodes.SUBCC, src, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);
    labf.setNotReferenced();
    test.setSetCC(ICC);

    appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
    appendInstruction(test);
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
  }

  protected void genFrameAddressFtn(int dest, int src, Type type)
  {
    if (src == SparcRegisterSet.G0_REG) {
      appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.SP_REG, SparcRegisterSet.SP_REG, dest));
      return;
    }

    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);
    SparcInstruction  delay = new IntOpInstruction(Opcodes.OR, SparcRegisterSet.SP_REG, SparcRegisterSet.SP_REG, dest);
    SparcBranch       br    = new BranchInstruction(Opcodes.BE, disp, true, 2, delay);
    AnnulMarker       am    = new AnnulMarker(delay);
    SparcInstruction  test  = new IntOpInstruction(Opcodes.SUBCC, src, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);
    labf.setNotReferenced();
    test.setSetCC(ICC);

    appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
    appendInstruction(test);
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
  }

  protected void genSignFtn(int dest, int laReg, int raReg, Type rType)
  {
    int bs = rType.memorySizeAsInt(machine);

    if (rType.isRealType()) {
      int               zr    = registers.newTempRegister(RegisterSet.FLTREG);
      Label             labt  = createLabel();
      Label             labf  = createLabel();
      LabelDisplacement disp  = new LabelDisplacement(labt);
      SparcInstruction  delay = new FltOpInstruction(Opcodes.FNEGS, dest, dest);
      SparcBranch       br    = new BranchInstruction(Opcodes.bopfOp(CompareMode.LT.ordinal(), false), disp, true, 2, delay);
      AnnulMarker       am    = new AnnulMarker(delay);
      SparcInstruction  test  = new FltCmpInstruction(Opcodes.cmpfOp(bs), raReg, zr, FCC0);

      br.addTarget(labt, 0);
      br.addTarget(labf, 1);
      labf.setNotReferenced();

      genRegToReg(laReg, dest);
      genLoadDblImmediate(0.0, zr, bs);
      appendInstruction(test);
      appendInstruction(new FltOpInstruction(Opcodes.FABSS, dest, dest));
      if (!v8plus)
        appendInstruction(new SethiInstruction(Opcodes.NOP, 0, getDisp(0), FT_NONE));
      appendInstruction(br);
      appendLabel(labt);
      appendInstruction(am);
      appendLabel(labf);
      return;
    }

    int ir = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.
    int tr = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.

    appendInstruction(new IntOpInstruction(Opcodes.XOR, raReg, laReg, ir));
    appendInstruction(new IntOpLitInstruction(Opcodes.SRA, ir, getDisp(31), FT_NONE, ir));
    appendInstruction(new IntOpInstruction(Opcodes.XOR, laReg, ir, tr));
    appendInstruction(new IntOpInstruction(Opcodes.SUB, tr, ir, dest));
  }

  protected void genAtan2Ftn(int dest, int laReg, int raReg, Type rType)
  {
    int    bs    = rType.memorySizeAsInt(machine);
    String fname = "atan2";

    if (bs <= 4)
      fname = "_scale_atan2f";

    int nxt = registers.numContiguousRegisters(laReg);
    genRegToReg(laReg, SparcRegisterSet.O0_REG);
    genRegToReg(raReg, SparcRegisterSet.O0_REG + nxt);
    short[] uses = (nxt > 1) ? quadIntUses : doubleIntUses;
    genFtnCall(fname, uses, createRoutineDefs(true, bs));
    genRegToReg(returnRegister(rType.getTag(), true), dest);
  }

  protected void genDimFtn(int dest, int laReg, int raReg, Type rType)
  {
    int bs = rType.memorySizeAsInt(machine);

    if (rType.isRealType()) {
      Label             labt2 = createLabel();
      Label             labf2 = createLabel();
      LabelDisplacement disp2 = new LabelDisplacement(labt2);
      SparcBranch       br2   = new BranchInstruction(Opcodes.bopfOp(CompareMode.LE.ordinal(), false), disp2, true, 2, null);

      br2.addTarget(labt2, 0);
      br2.addTarget(labf2, 1);
      labf2.setNotReferenced();

      appendInstruction(new FltCmpInstruction(Opcodes.cmpfOp(bs), raReg, laReg, FCC0));
      appendInstruction(new FltOp2Instruction(Opcodes.fopOp(SUB, bs), laReg, raReg, dest));
      if (!v8plus)
        appendInstruction(new SethiInstruction(Opcodes.NOP, 0, getDisp(0), FT_NONE));

      appendInstruction(br2);
      appendLabel(labf2);
      genLoadDblImmediate(0.0, dest, bs);
      appendLabel(labt2);
      return;
    }

    if (bs > 4)
      throw new scale.common.NotImplementedError("ABS() of " + bs);

    int ir = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.

    appendInstruction(new IntOpInstruction(Opcodes.SUB, laReg, raReg, dest));
    appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, getDisp(31), FT_NONE, ir));
    appendInstruction(new IntOpInstruction(Opcodes.ANDN, dest, ir, dest));
  }

  /**
   * Store a value in a register to a symbolic location in memory.
   * @param src is the value
   * @param dsize is the size of the value in addressable memory units
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param isReal is true if the value in the register is a floating
   * point value
   * @param disp specifies the location
   */
  protected void storeRegToSymbolicLocation(int          src,
                                            int          dsize,
                                            long         alignment,
                                            boolean      isReal,
                                            Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

    int dest = loadAddressHigh(disp);
    storeIntoMemory(src, dest, disp, FT_LO, dsize, alignment);
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
    int        adr    = resultReg;
    ResultMode adrha  = resultRegMode;
    int        adraln = resultRegAddressAlignment;

    if (srcha == ResultMode.ADDRESS) {
      int aln = (((fieldOffset & 0x3) == 0) ? 4 : 1);
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

    Displacement disp;
    if ((fieldOffset >= MIN_IMM13) && (fieldOffset <= MAX_IMM13))
      disp = getDisp((int) fieldOffset);
    else {
      int tr = registers.newTempRegister(RegisterSet.ADRREG);
      genLoadImmediate(fieldOffset, adr, tr);
      disp = getDisp(0);
      adr = tr;
    }

    int alignment = naln ? 1 : ((adraln < fa) ? adraln : fa);
    if (bits == 0)
      storeIntoMemory(src, adr, disp, FT_NONE, byteSize, alignment);
    else
      storeBitsIntoMemory(src, adr, disp, FT_NONE, bits, bitOffset, alignment);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = src;
  }

  protected boolean genSwitchUsingIfs(int     testReg,
                                      Chord[] cases,
                                      long[]  keys,
                                      int     num,
                                      long    spread)
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
      SparcBranch       inst  = new BranchInstruction(Opcodes.BE, disp, false, 2, null);

      labf.setNotReferenced();

      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        SparcInstruction test = new IntOpLitInstruction(Opcodes.SUBCC,
                                                        testReg,
                                                        getDisp((int) value),
                                                        FT_NONE,
                                                        SparcRegisterSet.G0_REG);
        test.setSetCC(ICC);
        appendInstruction(test);
      } else {
        int rb  = registers.newTempRegister(registers.getType(testReg));
        rb = genLoadImmediate(value, rb);
        SparcInstruction test = new IntOpInstruction(Opcodes.SUBCC,
                                                     testReg,
                                                     rb,
                                                     SparcRegisterSet.G0_REG);
        test.setSetCC(ICC);
        appendInstruction(test);
      }
        
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

  protected void genSwitchUsingTransferVector(int     testReg,
                                              Chord[] cases,
                                              long[]  keys,
                                              Label   labd,
                                              long    min,
                                              long    max)
  {
    // Use a transfer vector.

    int               rt     = registers.getType(testReg);
    int               tmp    = registers.newTempRegister(rt);
    Label             labf   = createLabel();
    Label             labf2  = createLabel();
    LabelDisplacement dflt   = new LabelDisplacement(labd);
    Displacement      jtab   = createAddressTable(cases, keys, (int) min, (int) max);
    SparcInstruction  dslot1 = null;
    boolean           cmp    = (min >= MIN_IMM13) && (min <= MAX_IMM13);

    if (cmp) {
      dslot1 = new IntOpLitInstruction(Opcodes.SUBCC,
                                       testReg,
                                       getDisp((int) min),
                                       FT_NONE,
                                       tmp);
      dslot1.setSetCC(ICC);
    }
      
    SparcBranch       brg    = new BranchInstruction(Opcodes.BG,
                                                     dflt,
                                                     false,
                                                     2,
                                                     dslot1);
    SparcInstruction  mult   = new IntOpLitInstruction(Opcodes.SLL,
                                                       tmp,
                                                       getDisp(2),
                                                       FT_NONE,
                                                       tmp);
    SparcBranch       brl    = new BranchInstruction(Opcodes.BL, dflt, false, 2, mult);
    SparcBranch       brto   = new JmplLitInstruction(tmp,
                                                      getDisp(0),
                                                      FT_NONE,
                                                      SparcRegisterSet.G0_REG,
                                                      cases.length,
                                                      null);

    labf.setNotReferenced();
    labf2.setNotReferenced();

    brg.addTarget(labd, 0);
    brg.addTarget(labf, 1);

    brl.addTarget(labd, 0);
    brl.addTarget(labf2, 1);

    for (int i = 0; i < cases.length; i++)
      brto.addTarget(getBranchLabel(cases[i]), i);

    if ((max >= MIN_IMM13) && (max <= MAX_IMM13)) {
      SparcInstruction test = new IntOpLitInstruction(Opcodes.SUBCC,
                                                      testReg,
                                                      getDisp((int) max),
                                                      FT_NONE,
                                                      SparcRegisterSet.G0_REG);
      test.setSetCC(ICC);
      appendInstruction(test);
    } else {
      int rb  = registers.newTempRegister(registers.getType(testReg));
      rb = genLoadImmediate(max, rb);
      SparcInstruction test = new IntOpInstruction(Opcodes.SUBCC,
                                                   testReg,
                                                   rb,
                                                   SparcRegisterSet.G0_REG);
      test.setSetCC(ICC);
      appendInstruction(test);
    }

    appendInstruction(brg);
    appendLabel(labf);
    if (!cmp) {
      int rb  = registers.newTempRegister(registers.getType(testReg));
      rb = genLoadImmediate(min, rb);
      SparcInstruction test = new IntOpInstruction(Opcodes.SUBCC, testReg, rb, tmp);
      test.setSetCC(ICC);
      appendInstruction(test);
    }
    appendInstruction(brl);
    appendLabel(labf2);

    int               adr  = loadMemoryAddress(jtab);
    SparcInstruction  load = new LoadInstruction(Opcodes.LD, adr, tmp, tmp);

    appendInstruction(load);
    appendInstruction(brto);
  }

  public int getMaxAreaIndex()
  {
    return NOTE;
  }

  /**
   * Return the register used to return the function value.
   * @param regType specifies the type of value
   * @param isCall is true if the calling routine is asking
   */
  public int returnRegister(int regType, boolean isCall)
  {
    int size = registers.numContiguousType(regType);
    if (registers.isFloatType(regType)) {
      if (size > 2)
        return SparcV8RegisterSet.Q0_REG;
      if (size > 1)
        return SparcV8RegisterSet.D0_REG;
      return SparcV8RegisterSet.F0_REG;
    }

    if (v8plus)
      return (isCall ? SparcV8RegisterSet.O0_REG : SparcV8RegisterSet.I0_REG);

    if (isCall)
      return ((size > 1) ? SparcV8RegisterSet.LO0_REG : SparcRegisterSet.O0_REG);

    return ((size > 1) ? SparcV8RegisterSet.LI0_REG : SparcRegisterSet.I0_REG);    
  }

  /**
   * Return the register used as the first argument in a function call.
   * @param regType specifies the type of argument value
   */
  public final int getFirstArgRegister(int regType)
  {
    return SparcRegisterSet.O0_REG;
  }

  protected short[] genSingleUse(int reg)
  {
    short[] uses = new short[1];
    uses[0] = (short) reg;
    return uses;
  }

  protected short[] genDoubleUse(int reg1, int reg2)
  {
    short[] uses = new short[2];
    uses[0] = (short) reg1;
    uses[1] = (short) reg2;
    return uses;
  }

  /**
   * Assign the routine's arguments to registers or the stack.
   */
  protected void layoutParameters()
  {
    ProcedureType pt      = (ProcedureType) processType(currentRoutine);
    int           nextArg = 0;
    Type          rt      = processType(pt.getReturnType());
    int           l       = pt.numFormals();
    int           fdi     = 0;

    if (!rt.isVoidType()) {
      int rts = rt.memorySizeAsInt(machine);
      if (isFortran() && (!rt.isAtomicType() || (rts > 8))) {
        nextArg++; // Result is passed in location specified as first argument.
      }
    }

    while (nextArg < MAX_ARG_REGS) {
      if (fdi >= l)
        break;

      FormalDecl fd = pt.getFormal(fdi++);

      if (fd instanceof UnknownFormals) {
        int          loc  = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;
        Displacement disp = new StackDisplacement(loc);
        defineDeclOnStack(fd, disp);
        return;
      }

      Type vt   = processType(fd);
      int  ts   = vt.memorySizeAsInt(machine);
      int  rinc = (ts + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;
      int  nr   = nextArg + SparcRegisterSet.I0_REG;

      if (genDebugInfo)
        stabs.defineParameterInRegister(fd, nr);

      if ((rinc > 2) || vt.isAggregateType()) { // Address of argument is passed in a register.
        defineDeclInRegister(fd, nr, ResultMode.ADDRESS);
        nextArg++;
        continue;
      }

      assert vt.isAtomicType() : "Parameter type " + fd;

      // The first six scaler arguments are in the argument registers, 
      // remaining words have already been placed on the stack by the caller.

      if (((nextArg + rinc - 1) >= MAX_ARG_REGS) ||
          fd.addressTaken() ||
          usesVaStart ||
          fd.getType().isVolatile() ||
          vt.isRealType() ||
          (v8plus && (rinc > 1))) { // Function prolog will save argument to the stack.

        // For v8plus, 64-bit integer variables must be kept on the
        // stack because the stack save mechanism saves only 32-bits
        // of the registers.

        int loc = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;
        if (vt.isRealType() && pt.isOldStyle() && (rinc < 2)) {
          rinc = 2;
          localVarSize += Machine.alignTo(ts, 8);
          loc = -localVarSize;
        }
        Displacement disp = new StackDisplacement(loc);

        defineDeclOnStack(fd, disp);
        nextArg += rinc;
        continue;
      }

      // Arguement is in register.

      if (vt.isRealType()) {
        int loc = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;
        if (pt.isOldStyle() && (rinc < 2)) {
          rinc = 2;
          localVarSize += Machine.alignTo(ts, 8);
          loc = -localVarSize;
        }
        Displacement disp = new StackDisplacement(loc);

        defineDeclOnStack(fd, disp);

        nextArg += rinc;
        continue;
      }

      // It's a mistake to try and use a real register because real
      // registers can't be spilled.

      int reg = registers.newTempRegister(vt.getTag());
      defineDeclInRegister(fd, reg, ResultMode.NORMAL_VALUE);
      nextArg += rinc;
    }

    while (fdi < l) { // Argument is passed on the stack.
      FormalDecl fd  = pt.getFormal(fdi++);
      int        loc = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;

      if (fd instanceof UnknownFormals) {
        Displacement disp = new StackDisplacement(loc);
        defineDeclOnStack(fd, disp);
        return;
      }

      Type vt   = processType(fd);
      int  ts   = vt.memorySizeAsInt(machine);
      int  rinc = (ts + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;

      if (genDebugInfo)
        stabs.defineParameterOnStack(fd, new StackDisplacement(loc));

      if (vt.isAggregateType()) { // Address of struct is on the stack.
        int nr = registers.newTempRegister(RegisterSet.ADRREG);
        defineDeclInRegister(fd, nr, ResultMode.ADDRESS);
        nextArg += 1;
        continue;
      }

      if (vt.isRealType() && pt.isOldStyle() && (rinc < 2)) {
        rinc = 2;
        localVarSize += Machine.alignTo(ts, 8);
        loc = -localVarSize;
      }

      Displacement disp = new StackDisplacement(loc);

      defineDeclOnStack(fd, disp);
      nextArg += rinc;
    }
  }

  /**
   * Insure that <code>short</code>s and <code>char</code> are
   * properly sign extended.
   */
  private void signExtend(int dest, int bs, boolean signed)
  {
    if (bs >= 4)
      return;

    Displacement shift = getDisp((4 - bs) * 8);
    if (signed) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, shift, FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, shift, FT_NONE, dest));
      return;
    }

    if (bs == 1) {
      appendInstruction(new IntOpLitInstruction(Opcodes.AND, dest, getDisp(255), FT_NONE, dest));
      return;
    }

    appendInstruction(new IntOpLitInstruction(Opcodes.SLL, dest, shift, FT_NONE, dest));
    appendInstruction(new IntOpLitInstruction(Opcodes.SRL, dest, shift, FT_NONE, dest));
  }

  private void genAdditionLit(int     laReg,
                              long    value,
                              int     dest,
                              int     bs,
                              boolean signed)
  {
    if ((bs <= 4) || ((bs <= 8) && v8plus)) {
      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD, laReg, getDisp((int) value), FT_NONE, dest));
        signExtend(dest, bs, signed);
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genLoadImmediate(value, tr);
      appendInstruction(new IntOpInstruction(Opcodes.ADD, laReg, tr, dest));
      signExtend(dest, bs, signed);
      return;
    }

    if (bs <= 8) {
      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDCC, laReg + 1, getDisp((int) value), FT_NONE, dest + 1));
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDC, laReg, getDisp(value < 0 ? -1 : 0), FT_NONE, dest));
        lastInstruction.specifyNotSpillLoadPoint();
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genLoadImmediate(value, tr);
      appendInstruction(new IntOpInstruction(Opcodes.ADDCC, laReg + 1, tr + 1, dest + 1));
      appendInstruction(new IntOpInstruction(Opcodes.ADDC, laReg, tr, dest));
      lastInstruction.specifyNotSpillLoadPoint();
      return;
    }

    throw new scale.common.NotImplementedError("ADD for " + bs);
  }

  private void genAddition(int laReg, int raReg, int dest, int bs, boolean signed)
  {
    if ((bs <= 4) || ((bs <= 8) && v8plus)) {
      appendInstruction(new IntOpInstruction(Opcodes.ADD, laReg, raReg, dest));
      signExtend(dest, bs, signed);
      return;
    }

    if (bs <= 8) {
      appendInstruction(new IntOpInstruction(Opcodes.ADDCC, laReg + 1, raReg + 1, dest + 1));
      appendInstruction(new IntOpInstruction(Opcodes.ADDC, laReg, raReg, dest));
      lastInstruction.specifyNotSpillLoadPoint();
      return;
    }

    throw new scale.common.NotImplementedError("ADD for " + bs);
  }

  private void genSubtractionLit(int     laReg,
                                 long    value,
                                 int     dest,
                                 int     bs,
                                 boolean signed)
  {
    if ((bs <= 4) || ((bs <= 8) && v8plus)) {
      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SUB, laReg, getDisp((int) value), FT_NONE, dest));
        signExtend(dest, bs, signed);
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genLoadImmediate(value, tr);
      appendInstruction(new IntOpInstruction(Opcodes.SUB, laReg, tr, dest));
      signExtend(dest, bs, signed);
      return;
    }

    if (bs <= 8) {
      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SUBCC, laReg + 1, getDisp((int) value), FT_NONE, dest + 1));
        appendInstruction(new IntOpLitInstruction(Opcodes.SUBC, laReg, getDisp(value < 0 ? -1 : 0), FT_NONE, dest));
        lastInstruction.specifyNotSpillLoadPoint();
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genLoadImmediate(value, tr);
      appendInstruction(new IntOpInstruction(Opcodes.SUBCC, laReg + 1, tr + 1, dest + 1));
      appendInstruction(new IntOpInstruction(Opcodes.SUBC, laReg, tr, dest));
      return;
    }

    throw new scale.common.NotImplementedError("SUB for " + bs);
  }

  private void genSubtraction(int     laReg,
                              int     raReg,
                              int     dest,
                              int     bs,
                              boolean signed)
  {
    if ((bs <= 4) || ((bs <= 8) && (v8plus))) {
      appendInstruction(new IntOpInstruction(Opcodes.SUB, laReg, raReg, dest));
      signExtend(dest, bs, signed);
      return;
    }

    if (bs <= 8) {
      appendInstruction(new IntOpInstruction(Opcodes.SUBCC, laReg + 1, raReg + 1, dest + 1));
      appendInstruction(new IntOpInstruction(Opcodes.SUBC, laReg, raReg, dest));
      lastInstruction.specifyNotSpillLoadPoint();
      return;
    }

    throw new scale.common.NotImplementedError("SUB for " + bs);
  }

  private void genMultiply(int laReg, int raReg, int dest, int bs, boolean signed)
  {
    if ((bs <= 4) && !v8plus) {
      appendInstruction(new IntOpInstruction(signed ? Opcodes.SMUL : Opcodes.UMUL, laReg, raReg, dest));
      signExtend(dest, bs, signed);
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpInstruction(Opcodes.MULX, laReg, raReg, dest));
        signExtend(dest, bs, signed);
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genRegToReg(laReg, SparcV8RegisterSet.O0_REG);
      genRegToReg(raReg, SparcV8RegisterSet.O2_REG);
      genFtnCall("__mul64", quadIntUses, createRoutineDefs(true, bs));
      genRegToReg(returnRegister(registers.getType(dest), true), dest);
      return;

    }

    throw new scale.common.NotImplementedError("MUL for " + bs);
  }

  private int multNeedsTemp(int src, int dest)
  {
    if (src != dest)
      return dest;
    return registers.newTempRegister(registers.getType(dest));
  }

  private void genMultiplyLit(int src, long value, int dest, int bs, boolean signed)
  {
    if (bs > 8)
      throw new scale.common.NotImplementedError("MUL for " + bs);

    if (v8plus) {
      if (value == 0) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
        return;
      }
      if (value == 1) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, src, SparcRegisterSet.G0_REG, dest));
        return;
      }
      if (value == -1) {
        appendInstruction(new IntOpInstruction(Opcodes.SUB, SparcRegisterSet.G0_REG, src, dest));
        signExtend(dest, bs, signed);
        return;
      }
      if (value == 2) {
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, src, dest));
        signExtend(dest, bs, signed);
        return;
      }
      int shift = Lattice.powerOf2(value);
      if (shift >= 0) { // It is a multiply by a power of 2.
        appendInstruction(new IntOpLitInstruction(Opcodes.SLLX , src, getDisp(shift), FT_NONE, dest));
        signExtend(dest, bs, signed);
        return;
      }
      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        appendInstruction(new IntOpLitInstruction(Opcodes.MULX, src, getDisp((int) value), FT_NONE, dest));
        signExtend(dest, bs, signed);
        return;
      }
      int tr = multNeedsTemp(src, dest);
      genLoadImmediate(value, SparcRegisterSet.G0_REG, tr);
      appendInstruction(new IntOpInstruction(Opcodes.MULX, src, tr, dest));
      signExtend(dest, bs, signed);
      return;
    }

    if (bs <= 4) {
      genMultiplyLitInt(src, value, dest, bs, signed);
      signExtend(dest, bs, signed);
      return;
    }

    genMultiplyLitLL(src, value, dest, bs, signed);
  }

  private void genMultiplyLitInt(int     src,
                                 long    value,
                                 int     dest,
                                 int     bs,
                                 boolean signed)
  {
    if ((value < 21) && (value > -2)) {
      int tr;
      switch ((int) value) {
      case -1:
        appendInstruction(new IntOpInstruction(Opcodes.SUB, SparcRegisterSet.G0_REG, src, dest));
        return;
      case 0:
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
        return;
      case 1:
        appendInstruction(new IntOpInstruction(Opcodes.OR, src, SparcRegisterSet.G0_REG, dest));
        return;
      case 2:
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, src, dest));
        return;
      case 3:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, dest));
        return;
      case 4:
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(2), FT_NONE, dest));
        return;
      case 5:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(2), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, dest));
        return;
      case 6:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, tr, dest));
        return;
      case 7:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(3), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.SUB, tr, src, dest));
        return;
      case 8:
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(3), FT_NONE, dest));
        return;
      case 9:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(3), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, src, dest));
        return;
      case 10:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(2), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, tr, dest));
        return;
      case 11:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr, getDisp(2), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.SUB, tr, src, dest));
        return;
      case 12:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr, getDisp(2), FT_NONE, dest));
        return;
      case 13:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr, getDisp(2), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, src, dest));
        return;
      case 14:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(4), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.SUB, tr, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.SUB, tr, src, dest));
        return;
      case 15:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(4), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.SUB, tr, src, dest));
        return;
      case 16:
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(4), FT_NONE, dest));
        return;
      case 17:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(4), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, src, dest));
        return;
      case 18:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(3), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, tr, dest));
        return;
      case 19:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(3), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, src, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, tr, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, src, dest));
        return;
      case 20:
        tr = multNeedsTemp(src, dest);
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(2), FT_NONE, tr));
        appendInstruction(new IntOpInstruction(Opcodes.ADD, src, tr, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, tr, getDisp(2), FT_NONE, dest));
        return;
      }
    }

    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // It is a multiply by a power of 2.
      appendInstruction(new IntOpLitInstruction(v8plus ? Opcodes.SLLX : Opcodes.SLL, src, getDisp(shift), FT_NONE, dest));
      return;
    }

    if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
      int opcode = v8plus ? Opcodes.MULX : (signed ? Opcodes.SMUL : Opcodes.UMUL);
      appendInstruction(new IntOpLitInstruction(opcode, src, getDisp((int) value), FT_NONE, dest));
      return;
    }

    int tr2 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
    genLoadImmediate(value, tr2);
    genMultiply(src, tr2, dest, bs, signed);
  }

  private void genMultiplyLitLL(int     src,
                                long    value,
                                int     dest,
                                int     bs,
                                boolean signed)
  {
    if ((value < 21) && (value > -2)) {
      int tr;
      switch ((int) value) {
      case -1:
        appendInstruction(new IntOpInstruction(Opcodes.SUBCC, SparcRegisterSet.G0_REG, src + 1, dest + 1));
        appendInstruction(new IntOpInstruction(Opcodes.SUBC, SparcRegisterSet.G0_REG, src, dest));
        lastInstruction.specifyNotSpillLoadPoint();
        return;
      case 0:
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest + 1));
        return;
      case 1:
        appendInstruction(new IntOpInstruction(Opcodes.OR, src, SparcRegisterSet.G0_REG, dest));
        appendInstruction(new IntOpInstruction(Opcodes.OR, src + 1, SparcRegisterSet.G0_REG, dest + 1));
        return;
      case 2:
        appendInstruction(new IntOpInstruction(Opcodes.ADDCC, src + 1, src + 1, dest + 1));
        appendInstruction(new IntOpInstruction(Opcodes.ADDC, src, src, dest));
        lastInstruction.specifyNotSpillLoadPoint();
        return;
      }
    }

    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // It is a multiply by a power of 2.
      genShiftLeftLogicalLit(src, shift, dest, bs);
      return;
    }

    int tr2 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
    genLoadImmediate(value, tr2);
    genMultiply(src, tr2, dest, bs, signed);
  }

  private void dividePower2(long    value,
                            int     shift,
                            int     src,
                            int     dest,
                            int     bs,
                            boolean signed)
  {
    if (bs > 4)
      throw new scale.common.NotImplementedError("DIV for " + bs);

    if (shift == 0) { // Dividing by the value 1.
      genRegToReg(src, dest);
      return;
    }

    if (signed) { // Divide a signed integer.
      long mv = value - 1;

      if ((mv < MIN_IMM13) || (mv > MAX_IMM13)) {
        int  tr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(value, tr);
        genDivide(src, tr, dest, bs, signed);
        return;
      }

      SparcInstruction  delay = new IntOpLitInstruction(Opcodes.ADD, src, getDisp((int) mv), FT_NONE, dest);
      AnnulMarker       am    = new AnnulMarker(delay);
      Label             labt  = createLabel();
      Label             labf  = createLabel();
      LabelDisplacement disp  = new LabelDisplacement(labt);
      SparcBranch       br    = new BranchInstruction(Opcodes.BL, disp, true, 2, delay);

      br.addTarget(labt, 0);
      br.addTarget(labf, 1);

      labf.setNotReferenced();

      appendInstruction(new IntOpInstruction(Opcodes.ORCC, src, SparcRegisterSet.G0_REG, dest));
      appendInstruction(br);
      appendLabel(labt);
      appendInstruction(am);
      appendLabel(labf);
      appendInstruction(new IntOpLitInstruction(v8plus ? Opcodes.SRAX: Opcodes.SRA,
                                                dest,
                                                getDisp(shift),
                                                FT_NONE,
                                                dest));
      return;
    }

    // Divide an unsigned integer.

    appendInstruction(new IntOpLitInstruction(Opcodes.SRL, src, getDisp(shift), FT_NONE, dest));
  }

  private void genSDIVCC(int laReg, int dest, SparcInstruction sdivcc)
  {
    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);
    int               tr    = registers.newTempRegister(RegisterSet.INTREG);
    SparcInstruction  delay = new SethiInstruction(Opcodes.SETHI, dest, getDisp(0x80000000));
    SparcBranch       br    = new BranchInstruction(Opcodes.BVS, disp, true, 2, delay);
    AnnulMarker       am    = new AnnulMarker(delay);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);

    labf.setNotReferenced();

    appendInstruction(new IntOpLitInstruction(Opcodes.SRA, laReg, getDisp(31), FT_NONE, tr));
    appendInstruction(new WriteRegInstruction(Opcodes.WR, tr, SparcRegisterSet.G0_REG, SR_Y));
    appendInstruction(sdivcc);
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
  }

  private void genDivide(int laReg, int raReg, int dest, int bs, boolean signed)
  {
    if ((bs <= 4) && !v8plus) {
      if (signed) {
        genSDIVCC(laReg, dest, new IntOpInstruction(Opcodes.SDIVCC, laReg, raReg, dest));
        return;
      }

      appendInstruction(new WriteRegInstruction(Opcodes.WR,
                                                SparcRegisterSet.G0_REG,
                                                SparcRegisterSet.G0_REG,
                                                SR_Y));
      appendInstruction(new IntOpInstruction(Opcodes.UDIV, laReg, raReg, dest));
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpInstruction(signed ? Opcodes.SDIVX : Opcodes.UDIVX,
                                               laReg,
                                               raReg,
                                               dest));
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genRegToReg(laReg, SparcV8RegisterSet.O0_REG);
      genRegToReg(raReg, SparcV8RegisterSet.O2_REG);
      genFtnCall(signed ? "__div64" : "__udiv64", quadIntUses, createRoutineDefs(true, bs));
      genRegToReg(returnRegister(registers.getType(dest), true), dest);
      return;
    }

    throw new scale.common.NotImplementedError("DIV for " + bs);
  }

  private void genDivideLit(int laReg, long value, int dest, int bs, boolean signed)
  {
    if ((bs <= 4) && !v8plus) {
      if ((value > 0) && (value < 0x4000000)) {
        int shift = Lattice.powerOf2(value);
        if (shift >= 0) { // It is a divide by a power of 2.
          dividePower2(value, shift, laReg, dest, bs, signed);
          return;
        }
      }

      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        Displacement disp = getDisp((int) value);
        if (signed) {
          genSDIVCC(laReg, dest, new IntOpLitInstruction(Opcodes.SDIVCC, laReg, disp, FT_NONE, dest));
          return;
        }

        appendInstruction(new WriteRegInstruction(Opcodes.WR,
                                                  SparcRegisterSet.G0_REG,
                                                  SparcRegisterSet.G0_REG,
                                                  SR_Y));
        appendInstruction(new IntOpLitInstruction(Opcodes.UDIV, laReg, disp, FT_NONE, dest));
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genLoadImmediate(value, tr);
      genDivide(laReg, tr, dest, bs, signed);
      return;
    }

    if (bs <= 8) {
      if (!signed && (value > 0) && (value < 0x4000000)) {
        int shift = Lattice.powerOf2(value);
        if (shift >= 0) { // It is a divide by a power of 2.
          genShiftRightLogicalLit(laReg, shift, dest, bs);
          return;
        }
      }

      if (v8plus) {
        if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
          appendInstruction(new IntOpLitInstruction(signed ? Opcodes.SDIVX : Opcodes.UDIVX,
                                                    laReg,
                                                    getDisp((int) value),
                                                    FT_NONE,
                                                    dest));
          return;
        }
        int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
        genLoadImmediate(value, tr);
        appendInstruction(new IntOpInstruction(signed ? Opcodes.SDIVX : Opcodes.UDIVX, laReg, tr, dest));
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genRegToReg(laReg, SparcV8RegisterSet.O0_REG);
      genLoadImmediate(value, tr);
      genRegToReg(tr, SparcV8RegisterSet.O2_REG);
      genFtnCall(signed ? "__div64" : "__udiv64", quadIntUses, createRoutineDefs(true, bs));
      genRegToReg(returnRegister(registers.getType(dest), true), dest);
      return;
    }

    throw new scale.common.NotImplementedError("DIV for " + bs);
  }

  private void genShiftLeftLogicalLit(int laReg, long value, int dest, int bs)
  {
    if ((value < MIN_IMM13) || (value > MAX_IMM13))
      throw new scale.common.NotImplementedError("SLL of " + value);

    if (bs <= 4) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, laReg, getDisp((int) value), FT_NONE, dest));
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, laReg, getDisp((int) value), FT_NONE, dest));
        return;
      }

      if (value >= 32) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SLL, laReg + 1, getDisp((int) value - 32), FT_NONE, dest));
        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest + 1));
        return;
      }

      if (value == 0) {
        genRegToReg(laReg, dest);
        return;
      }

      int tr1 = registers.newTempRegister(RegisterSet.INTREG);
      int tr2 = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, laReg, getDisp((int) value), FT_NONE, tr1));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, laReg + 1, getDisp((int) (32 - value)), FT_NONE, tr2));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr1, tr2, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, laReg + 1, getDisp((int) value), FT_NONE, dest + 1));
      return;
    }

    throw new scale.common.NotImplementedError("SLL for " + bs);
  }

  private void genShiftLeftLogical(int laReg, int raReg, int dest, int bs)
  {
    if (bs <= 4) {
      appendInstruction(new IntOpInstruction(Opcodes.SLL, laReg, raReg, dest));
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpInstruction(Opcodes.SLLX, laReg, raReg, dest));
        return;
      }
      int t1 = registers.newTempRegister(RegisterSet.INTREG);
      int t2 = registers.newTempRegister(RegisterSet.INTREG);
      int t3 = registers.newTempRegister(RegisterSet.INTREG);
      int t4 = registers.newTempRegister(RegisterSet.INTREG);
      int t5 = registers.newTempRegister(RegisterSet.INTREG);
      int t6 = registers.newTempRegister(RegisterSet.INTREG);
      int t7 = registers.newTempRegister(RegisterSet.INTREG);
      int t8 = registers.newTempRegister(RegisterSet.INTREG);
      int zr = SparcRegisterSet.G0_REG;
      int sf = registers.lastRegister(raReg);

      appendInstruction(new IntOpLitInstruction(Opcodes.OR,    zr,    getDisp(32), FT_NONE, t1));
      appendInstruction(new    IntOpInstruction(Opcodes.OR,    zr,    laReg + 0,            t4));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBCC, t1,    sf,                   t3));
      appendInstruction(new    IntOpInstruction(Opcodes.OR,    zr,    laReg + 1,            t2));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBC,  zr,    zr,                   t1));
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBCC, t3,    getDisp(1),  FT_NONE, zr));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBC,  t1,    zr,                   t5));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA,   t5,    getDisp(2),  FT_NONE, t6));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDCC, sf,    getDisp(-1), FT_NONE, zr));
      appendInstruction(new    IntOpInstruction(Opcodes.ANDN,  t2,    t6,                   t7));
      appendInstruction(new    IntOpInstruction(Opcodes.AND,   t2,    t6,                   t8));
      appendInstruction(new    IntOpInstruction(Opcodes.SRL,   t7,    t3,                   t1));
      appendInstruction(new    IntOpInstruction(Opcodes.ANDN,  t4,    t6,                   t3));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBC,  zr,    zr,                   t2));
      appendInstruction(new    IntOpInstruction(Opcodes.OR,    t3,    t8,                   t5));
      appendInstruction(new    IntOpInstruction(Opcodes.AND,   t1,    t2,                   t4));
      appendInstruction(new    IntOpInstruction(Opcodes.SLL,   t5,    sf,                   t6));
      appendInstruction(new    IntOpInstruction(Opcodes.OR,    t6,    t4,                   dest + 0));
      appendInstruction(new    IntOpInstruction(Opcodes.SLL,   t7,    sf,                   dest + 1));
      return;
    }

    throw new scale.common.NotImplementedError("SLL for " + bs);
  }

  private void genShiftRightLogicalLit(int laReg, long value, int dest, int bs)
  {
    if ((value < MIN_IMM13) || (value > MAX_IMM13))
      throw new scale.common.NotImplementedError("Shift right of " + value);

    if (bs <= 4) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, laReg, getDisp((int) value), FT_NONE, dest));
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SRLX, laReg, getDisp((int) value), FT_NONE, dest));
        return;
      }

      if (value >= 32) {
        int tr = dest;
        if (laReg == dest)
          tr = registers.newTempRegister(registers.getType(dest));

        appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SRL, laReg, getDisp((int) value - 32), FT_NONE, tr + 1));
        genRegToReg(tr, dest);
        return;
      }

      if (value == 0) {
        genRegToReg(laReg, dest);
        return;
      }

      int tr1 = registers.newTempRegister(RegisterSet.INTREG);
      int tr2 = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, laReg, getDisp((int) (32 - value)), FT_NONE, tr1));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, laReg + 1, getDisp((int) value), FT_NONE, tr2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, laReg, getDisp((int) value), FT_NONE, dest));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr1, tr2, dest + 1));
      return;
    }

    throw new scale.common.NotImplementedError("SRL for " + bs);
  }

  private void genShiftRightLogical(int laReg, int raReg, int dest, int bs)
  {
    if (bs <= 4) {
      appendInstruction(new IntOpInstruction(Opcodes.SRL, laReg, raReg, dest));
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpInstruction(Opcodes.SRLX, laReg, raReg, dest));
        return;
      }
      int t1 = registers.newTempRegister(RegisterSet.INTREG);
      int t2 = registers.newTempRegister(RegisterSet.INTREG);
      int t3 = registers.newTempRegister(RegisterSet.INTREG);
      int t4 = registers.newTempRegister(RegisterSet.INTREG);
      int t5 = registers.newTempRegister(RegisterSet.INTREG);
      int t6 = registers.newTempRegister(RegisterSet.INTREG);
      int t7 = registers.newTempRegister(RegisterSet.INTREG);
      int zr = SparcRegisterSet.G0_REG;
      int sf = registers.lastRegister(raReg);

      appendInstruction(new IntOpLitInstruction(Opcodes.OR,    zr,        getDisp(32), FT_NONE, t1));
      appendInstruction(   new IntOpInstruction(Opcodes.SUBCC, t1,        sf,                   t2));
      appendInstruction(   new IntOpInstruction(Opcodes.SUBC,  zr,        zr,                   t1));
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBCC, t2,        getDisp(1),  FT_NONE, zr));
      appendInstruction(   new IntOpInstruction(Opcodes.SUBC,  t1,        zr,                   t4));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA,   t4,        getDisp(2),  FT_NONE, t3));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDCC, sf,        getDisp(-1), FT_NONE, zr));
      appendInstruction(   new IntOpInstruction(Opcodes.SUBC,  zr,        zr,                   t4));
      appendInstruction(   new IntOpInstruction(Opcodes.ANDN,  laReg + 0, t3,                   t5));
      appendInstruction(   new IntOpInstruction(Opcodes.AND,   laReg + 0, t3,                   t6));
      appendInstruction(   new IntOpInstruction(Opcodes.ANDN,  laReg + 1, t3,                   t7));
      appendInstruction(   new IntOpInstruction(Opcodes.SLL,   t5,        t2,                   t1));
      appendInstruction(   new IntOpInstruction(Opcodes.OR,    t7,        t6,                   t2));
      appendInstruction(   new IntOpInstruction(Opcodes.SRL,   t2,        sf,                   t7));
      appendInstruction(   new IntOpInstruction(Opcodes.AND,   t1,        t4,                   t3));
      appendInstruction(   new IntOpInstruction(Opcodes.SRL,   t5,        sf,                   dest + 0));
      appendInstruction(   new IntOpInstruction(Opcodes.OR,    t7,        t3,                   dest + 1));
      return;
    }

    throw new scale.common.NotImplementedError("SRL for " + bs);
  }

  private void genShiftRightLit(int laReg, long value, int dest, int bs)
  {
    if ((value < MIN_IMM13) || (value > MAX_IMM13))
      throw new scale.common.NotImplementedError("SLL of " + value);

    if (bs <= 4) {
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, laReg, getDisp((int) value), FT_NONE, dest));
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpLitInstruction(Opcodes.SRAX, laReg, getDisp((int) value), FT_NONE, dest));
        return;
      }

      if (value >= 32) {
        int tr = dest;
        if (laReg == dest)
          tr = registers.newTempRegister(registers.getType(dest));

        appendInstruction(new IntOpLitInstruction(Opcodes.SRA, laReg, getDisp(31), FT_NONE, tr));
        appendInstruction(new IntOpLitInstruction(Opcodes.SRA, laReg, getDisp((int) value - 32), FT_NONE, tr + 1));
        genRegToReg(tr, dest);
        return;
      }

      if (value == 0) {
        genRegToReg(laReg, dest);
        return;
      }

      int tr1 = registers.newTempRegister(RegisterSet.INTREG);
      int tr2 = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, laReg, getDisp((int) (32 - value)), FT_NONE, tr1));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, laReg + 1, getDisp((int) value), FT_NONE, tr2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, laReg, getDisp((int) value), FT_NONE, dest));
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr1, tr2, dest + 1));
      return;
    }

    throw new scale.common.NotImplementedError("SRA for " + bs);
  }

  private void genShiftRight(int laReg, int raReg, int dest, int bs)
  {
    if (bs <= 4) {
      appendInstruction(new IntOpInstruction(Opcodes.SRA, laReg, raReg, dest));
      return;
    }

    if (bs <= 8) {
      if (v8plus) {
        appendInstruction(new IntOpInstruction(Opcodes.SRAX, laReg, raReg, dest));
        return;
      }
      int t1 = registers.newTempRegister(RegisterSet.INTREG);
      int t2 = registers.newTempRegister(RegisterSet.INTREG);
      int t3 = registers.newTempRegister(RegisterSet.INTREG);
      int t4 = registers.newTempRegister(RegisterSet.INTREG);
      int t5 = registers.newTempRegister(RegisterSet.INTREG);
      int t6 = registers.newTempRegister(RegisterSet.INTREG);
      int t7 = registers.newTempRegister(RegisterSet.INTREG);
      int t8 = registers.newTempRegister(RegisterSet.INTREG);
      int zr = SparcRegisterSet.G0_REG;
      int sf = registers.lastRegister(raReg);

      appendInstruction(new IntOpLitInstruction(Opcodes.OR,    zr,         getDisp(32), FT_NONE, t1));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBCC, t1,         sf,                   t2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA,   laReg + 0,  getDisp(31), FT_NONE, t3));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBC,  zr,         zr,                   t1));
      appendInstruction(new IntOpLitInstruction(Opcodes.SUBCC, t2,         getDisp(1),  FT_NONE, zr));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBC,  t1,         zr,                   t4));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA,   t4,         getDisp(2),  FT_NONE, t5));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDCC, sf,         getDisp(-1), FT_NONE, zr));
      appendInstruction(new    IntOpInstruction(Opcodes.ANDN,  laReg + 0,  t5,                   t1));
      appendInstruction(new    IntOpInstruction(Opcodes.AND,   t3,         t5,                   t6));
      appendInstruction(new    IntOpInstruction(Opcodes.OR,    t1,         t6,                   t3));
      appendInstruction(new    IntOpInstruction(Opcodes.AND,   laReg + 0,  t5,                   t7));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBC,  zr,         zr,                   t6));
      appendInstruction(new    IntOpInstruction(Opcodes.SLL,   t3,         t2,                   t1));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADDCC, t2,         getDisp(-1), FT_NONE, zr));
      appendInstruction(new    IntOpInstruction(Opcodes.SUBC,  zr,         zr,                   t8));
      appendInstruction(new    IntOpInstruction(Opcodes.ANDN,  laReg + 1,  t5,                   t4));
      appendInstruction(new    IntOpInstruction(Opcodes.AND,   t6,         t8,                   t8));
      appendInstruction(new    IntOpInstruction(Opcodes.OR,    t4,         t7,                   t5));
      appendInstruction(new    IntOpInstruction(Opcodes.AND,   t1,         t8,                   t4));
      appendInstruction(new    IntOpInstruction(Opcodes.SRL,   t5,         sf,                   t2));
      appendInstruction(new    IntOpInstruction(Opcodes.SRA,   t3,         sf,                   dest + 0));
      appendInstruction(new    IntOpInstruction(Opcodes.OR,    t2,         t4,                   dest + 1));
      return;
    }

    throw new scale.common.NotImplementedError("SRA for " + bs);
  }

  private void genLogicalLit(int laReg, long value, int dest, int bs, int opcode)
  {
    if ((bs <= 4) || ((bs <= 8) && v8plus)) {
      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        appendInstruction(new IntOpLitInstruction(opcode, laReg, getDisp((int) value), FT_NONE, dest));
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genLoadImmediate(value, tr);
      appendInstruction(new IntOpInstruction(opcode, laReg, tr, dest));
      return;
    }

    if (bs <= 8) {
      if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
        appendInstruction(new IntOpLitInstruction(opcode, laReg, getDisp(value < 0 ? -1 : 0), FT_NONE, dest));
        appendInstruction(new IntOpLitInstruction(opcode, laReg + 1, getDisp((int) value), FT_NONE, dest + 1));
        lastInstruction.specifyNotSpillLoadPoint();
        return;
      }

      int tr = registers.newTempRegister(registers.tempRegisterType(RegisterSet.INTREG, bs));
      genLoadImmediate(value, tr);
      appendInstruction(new IntOpInstruction(opcode, laReg, tr, dest));
      appendInstruction(new IntOpInstruction(opcode, laReg + 1, tr + 1, dest + 1));
      lastInstruction.specifyNotSpillLoadPoint();
      return;
    }

    throw new scale.common.NotImplementedError("Logical op for " + bs);
  }

  private void genLogical(int laReg, int raReg, int dest, int bs, int opcode)
  {
    if ((bs <= 4) || ((bs <= 8) && v8plus)) {
      appendInstruction(new IntOpInstruction(opcode, laReg, raReg, dest));
      return;
    }

    if (bs <= 8) {
      appendInstruction(new IntOpInstruction(opcode, laReg, raReg, dest));
      appendInstruction(new IntOpInstruction(opcode, laReg + 1, raReg + 1, dest + 1));
      lastInstruction.specifyNotSpillLoadPoint();
      return;
    }

    throw new scale.common.NotImplementedError("Logical op for " + bs);
  }

  private void genAndLit(int laReg, long value, int dest, int bs, boolean signed)
  {
    genLogicalLit(laReg, value, dest, bs, Opcodes.AND);
  }

  private void genAnd(int laReg, int raReg, int dest, int bs, boolean signed)
  {
    genLogical(laReg, raReg, dest, bs, Opcodes.AND);
  }

  private void genOrLit(int laReg, long value, int dest, int bs, boolean signed)
  {
    genLogicalLit(laReg, value, dest, bs, Opcodes.OR);
  }

  private void genOr(int laReg, int raReg, int dest, int bs, boolean signed)
  {
    genLogical(laReg, raReg, dest, bs, Opcodes.OR);
  }

  private void genXorLit(int laReg, long value, int dest, int bs, boolean signed)
  {
    genLogicalLit(laReg, value, dest, bs, Opcodes.XOR);
  }

  private void genXor(int laReg, int raReg, int dest, int bs, boolean signed)
  {
    genLogical(laReg, raReg, dest, bs, Opcodes.XOR);
  }

  private void genXnorLit(int laReg, long value, int dest, int bs, boolean signed)
  {
    genLogicalLit(laReg, value, dest, bs, Opcodes.XNOR);
  }

  private void genXnor(int laReg, int raReg, int dest, int bs, boolean signed)
  {
    genLogical(laReg, raReg, dest, bs, Opcodes.XNOR);
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
    int bsh = bs / 2;

    int opcode = Opcodes.fopOp(which, bsh);
    int destP  = dest + registers.numContiguousRegisters(dest);
    int laRegP = laReg + registers.numContiguousRegisters(laReg);
    int raRegP = raReg + registers.numContiguousRegisters(raReg);

    switch (which) {
    case ADD:
      if (registers.pairRegister(laReg)) {
        int t = laReg;
        laReg = raReg;
        raReg = t;
        t = laRegP;
        laRegP = raRegP;
        raRegP = t;
      }
      appendInstruction(new FltOp2Instruction(opcode, laReg, raReg, dest));
      if (registers.pairRegister(laReg)) {
        appendInstruction(new FltOp2Instruction(opcode, laRegP, raRegP, destP));
      } else {
        genRegToReg(raRegP, destP);
      }
      return;
    case SUB:
      if (registers.pairRegister(laReg)) {
        if (registers.pairRegister(raReg)) {
          appendInstruction(new FltOp2Instruction(opcode, laReg, raReg, dest));
          appendInstruction(new FltOp2Instruction(opcode, laRegP, raRegP, destP));
        } else {
          appendInstruction(new FltOp2Instruction(opcode, laReg, raReg, dest));
          genRegToReg(laRegP, destP);
        }
      } else {
        appendInstruction(new FltOp2Instruction(opcode, laReg, raReg, dest));
        genRegToReg(raRegP, destP);
        // Only have to do top half if destination and source are the same.
        appendInstruction(new FltOpInstruction(Opcodes.FNEGS, destP, destP));
      }
      return;
    case MUL:
      if (registers.pairRegister(laReg)) {
        int t = laReg;
        laReg = raReg;
        raReg = t;
        t = laRegP;
        laRegP = raRegP;
        raRegP = t;
      }
      if (registers.pairRegister(laReg)) {
        int ty    = registers.tempRegisterType(RegisterSet.FLTREG, bsh);
        int tr    = registers.newTempRegister(ty);
        int tr2   = registers.newTempRegister(ty);
        int tr3   = registers.newTempRegister(ty);
        int addop = Opcodes.fopOp(ADD, bsh);
        int subop = Opcodes.fopOp(SUB, bsh);
        appendInstruction(new FltOp2Instruction(opcode, laReg,  raReg,  tr2));
        appendInstruction(new FltOp2Instruction(opcode, laRegP, raRegP, tr3));
        appendInstruction(new FltOp2Instruction(opcode, laReg,  raRegP, tr));
        appendInstruction(new FltOp2Instruction(opcode, laRegP, raReg,  destP));
        appendInstruction(new FltOp2Instruction(subop,  tr2,    tr3,    dest));
        appendInstruction(new FltOp2Instruction(addop,  tr,     destP,  destP));
      } else {
        appendInstruction(new FltOp2Instruction(opcode, laReg, raReg,  dest));
        appendInstruction(new FltOp2Instruction(opcode, laReg, raRegP, destP));
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

        int addop = Opcodes.fopOp(ADD, bsh);
        int subop = Opcodes.fopOp(SUB, bsh);
        int mulop = Opcodes.fopOp(MUL, bsh);
        int r1  = laReg;
        int i1  = laRegP;
        int r2  = raReg;
        int i2  = raRegP;
        int r3  = dest;
        int i3  = destP;
        int ft  = registers.tempRegisterType(RegisterSet.FLTREG, bsh);
        int it  = registers.tempRegisterType(RegisterSet.INTREG, bsh);
        int t3  = registers.newTempRegister(it + RegisterSet.PAIRREG);
        int t3P = t3 + registers.numContiguousRegisters(t3);
        int t17 = registers.newTempRegister(RegisterSet.INTREG);
        int f12 = registers.newTempRegister(ft);
        int f13 = registers.newTempRegister(ft);
        int f14 = registers.newTempRegister(ft);
        int f15 = registers.newTempRegister(ft);
        int f16 = registers.newTempRegister(ft);
        int f17 = registers.newTempRegister(ft);

        Label             labt  = createLabel();
        Label             labf  = createLabel();
        LabelDisplacement disp  = new LabelDisplacement(labt);
        SparcInstruction  delay = new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, t3P, t3);
        SparcBranch       br    = new BranchInstruction(Opcodes.BL, disp, true, 2, delay);
        AnnulMarker       am    = new AnnulMarker(delay);
        SparcInstruction  test  = new IntOpInstruction(Opcodes.SUBCC, t3, t3P, SparcRegisterSet.G0_REG);

        br.addTarget(labt, 0);
        br.addTarget(labf, 1);
        labf.setNotReferenced();
        test.setSetCC(ICC);

        if (!registers.pairRegister(laReg)) {
          i1 = registers.newTempRegister(ft);
          genLoadDblImmediate(0.0, i1, bsh);
        }

        genLoadImmediate(0x7f800000, t17);
        genRegToReg(r2, t3);
        appendInstruction(new IntOpInstruction(Opcodes.AND, t3, t17, t3));
        appendInstruction(new IntOpInstruction(Opcodes.AND, t3P, t17, t3P));
        appendInstruction(test);
        appendInstruction(br);
        appendLabel(labt);
        appendInstruction(am);
        appendLabel(labf);
        appendInstruction(new IntOpInstruction(Opcodes.SUB, t17, t3, t17));
        genRegToReg(t17, f13); //  f13 = scale
        appendInstruction(new FltOp2Instruction(mulop,  r2, f13, f12)); // f12 = r2 * scale
        appendInstruction(new FltOp2Instruction(mulop,  i2, f13, f13)); // f13 = i2 * scale
        appendInstruction(new FltOp2Instruction(mulop,  r2, f12, f14)); // f14 = r2 * r2 * scale
        appendInstruction(new FltOp2Instruction(mulop,  i2, f13, f15)); // f15 = i2 * i2 * scale
        appendInstruction(new FltOp2Instruction(mulop,  r1, f12, f16)); // f16 = r1 * r2 * scale
        appendInstruction(new FltOp2Instruction(mulop,  i1, f13, f17)); // f17 = i1 * i2 * scale
        appendInstruction(new FltOp2Instruction(mulop,  i1, f12, f12)); // f12 = i1 * r2 * scale
        appendInstruction(new FltOp2Instruction(mulop,  r1, f13, f13)); // f13 = r1 * i2 * scale
        appendInstruction(new FltOp2Instruction(addop, f14, f15, f14)); // f14 = r2 * r2 * scale + i2 * i2 * scale
        genLoadDblImmediate(1.0, f15, bsh);
        appendInstruction(new FltOp2Instruction(addop,  f16, f17, f17)); // f17 = r1 * r2 * scale + i1 * i2 * scale
        appendInstruction(new FltOp2Instruction(subop,  f12, f13, f13)); // f13 = i1 * r2 * scale - r1 * i2 * scale
        appendInstruction(new FltOp2Instruction(opcode, f15, f14, f14)); // f14 = 1.0 / ( r2 * r2 * scale + i2 * i2 * scale)
        appendInstruction(new FltOp2Instruction(mulop,  f17, f14,  r3)); // r3  = (r1 * r2 * scale + i1 * i2 * scale) * (1.0 / ( r2 * r2 * scale + i2 * i2 * scale))
        appendInstruction(new FltOp2Instruction(mulop,  f13, f14,  i3)); // i3  = (i1 * r2 * scale - r1 * i2 * scale) * (1.0 / ( r2 * r2 * scale + i2 * i2 * scale))
      } else {
        appendInstruction(new FltOp2Instruction(opcode, laReg,  raReg, dest));
        appendInstruction(new FltOp2Instruction(opcode, laRegP, raReg, destP));
      }
      return;
    default:
      throw new scale.common.InternalError("Invalid complex op " + which);
    }
  }

  private void genCompareIntLit(CompareMode which,
                                int         laReg,
                                int         value,
                                int         dest,
                                int         bs,
                                boolean     signed)
  {
    if ((value < MIN_IMM13) || (value > MAX_IMM13)) {
      int tr = registers.newTempRegister(registers.getType(laReg));
      genLoadImmediate(value, SparcRegisterSet.G0_REG, tr);
      genCompareInt(which, laReg, tr, dest, bs, signed);
      return;
    }

    Label             labt   = createLabel();
    Label             labf   = createLabel();
    LabelDisplacement disp   = new LabelDisplacement(labt);
    SparcInstruction  delay  = new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(1), FT_NONE, dest);
    AnnulMarker       am     = new AnnulMarker(delay);

    which = genCCLit(which, laReg, value, bs);

    int         opcode = Opcodes.bopiOp(which.ordinal(), false, signed);
    SparcBranch br     = null;
    if (v8plus)
      br = new BranchCCInstruction(Opcodes.bopiOp(which.ordinal(), true, signed), (bs <= 4) ? ICC : XCC , disp, true, 2, delay);
    else
      br = new BranchInstruction(Opcodes.bopiOp(which.ordinal(), false, signed), disp, true, 2, delay);


    br.addTarget(labt, 0);
    br.addTarget(labf, 1);
    labf.setNotReferenced();

    appendInstruction(new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(0), FT_NONE, dest));
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
  }

  private void genCompareInt(CompareMode which,
                             int         laReg,
                             int         raReg,
                             int         dest,
                             int         bs,
                             boolean     signed)
  {
    Label             labt   = createLabel();
    Label             labf   = createLabel();
    LabelDisplacement disp   = new LabelDisplacement(labt);
    SparcInstruction  delay  = new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(1), FT_NONE, dest);
    AnnulMarker       am     = new AnnulMarker(delay);

    which = genCC(which, laReg, raReg, bs);

    int         opcode = Opcodes.bopiOp(which.ordinal(), false, signed);
    SparcBranch br     = null;
    if (v8plus)
      br = new BranchCCInstruction(Opcodes.bopiOp(which.ordinal(), true, signed), (bs <= 4) ? ICC : XCC , disp, true, 2, delay);
    else
      br = new BranchInstruction(Opcodes.bopiOp(which.ordinal(), false, signed), disp, true, 2, delay);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);
    labf.setNotReferenced();

    appendInstruction(new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(0), FT_NONE, dest));
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
  }

  /**
   * Generate instructions to do a comparison of two value.
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
    int     bs = lt.memorySizeAsInt(machine);
    boolean si = lt.isSigned();

    needValue(la);
    int laReg = resultReg;

    if (!lt.isRealType()) { // Integer
      if (ra.isLiteralExpr()) {
        Literal lit   = ((LiteralExpr) ra).getLiteral().getConstantValue();
        long    value = MAX_IMM13 + 1;

        if (lit instanceof IntLiteral)
          value = ((IntLiteral) lit).getLongValue();
        else if (lit instanceof CharLiteral)
          value = ((CharLiteral) lit).getCharacterValue();

        if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
          genCompareIntLit(which, laReg, (int) value, ir, bs, si);
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
      }

      needValue(ra);
      int raReg = resultReg;

      genCompareInt(which, laReg, raReg, ir, bs, si);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    // Floating point

    needValue(ra);
    int raReg = resultReg;

    int               bytes = registers.registerSize(laReg);
    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);
    SparcInstruction  delay = new IntOpLitInstruction(Opcodes.OR,
                                                      SparcRegisterSet.G0_REG,
                                                      getDisp(1),
                                                      FT_NONE,
                                                      ir);
    SparcBranch       br    = new BranchInstruction(Opcodes.bopfOp(which.ordinal(), v8plus),
                                                    disp,
                                                    true,
                                                    2,
                                                    delay);
    SparcInstruction  test  = new FltCmpInstruction(Opcodes.cmpfOp(bytes),
                                                    laReg,
                                                    raReg,
                                                    FCC0);
    AnnulMarker       am    = new AnnulMarker(delay);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);

    test.setSetCC(FCC0);
    appendInstruction(test);
    appendInstruction(new IntOpLitInstruction(Opcodes.OR,
                                              SparcRegisterSet.G0_REG,
                                              getDisp(0),
                                              FT_NONE,
                                              ir));

    if (lt.isComplexType()) {
      int              laRegP = laReg + registers.numContiguousRegisters(laReg);
      int              raRegP = raReg + registers.numContiguousRegisters(raReg);
      Label            labm   = createLabel();
      int              bop    = Opcodes.bopfOp(which.reverse().ordinal(), v8plus);
      SparcBranch      br2    = new BranchInstruction(bop, disp, false, 2, null);
      SparcInstruction test2  = new FltCmpInstruction(Opcodes.cmpfOp(bytes),
                                                      laRegP,
                                                      raRegP,
                                                      FCC0);

      appendInstruction(br2);
      br.addTarget(labf, 0);
      br.addTarget(labm, 1);
      labm.setNotReferenced();
      test2.setSetCC(FCC0);
      appendInstruction(test2);
      if (!v8plus)
        appendInstruction(new SethiInstruction(Opcodes.NOP, 0, getDisp(0), FT_NONE));
    } else
      labf.setNotReferenced();

    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private void gen64BitImmediate(long value, int base, int dest)
  {
    int          val  = (int) value;
    Displacement disp = getDisp(val);
    int          vlo  = val & 0x0003ff;
    int          val2  = (int) (value >> 32);
    Displacement disp2 = getDisp(val2);
    int          vlo2  = val2 & 0x0003ff;

    int tr  = registers.newTempRegister(RegisterSet.INTREG);
    int tr2 = registers.newTempRegister(RegisterSet.INTREG);
    if (vlo == 0) {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
    } else {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, disp, FT_LO, tr));
    }

    if ((val2 >= MIN_IMM13) && (val2 <= MAX_IMM13)) {
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, SparcRegisterSet.G0_REG, disp2, FT_NONE, tr2));
    } else if (vlo2 == 0) {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr2, disp, FT_HI));
    } else {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr2, disp2, FT_HI));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr2, disp2, FT_LO, tr2));
    }

    appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, tr2, getDisp(32), FT_NONE, tr2));

    if (base != SparcRegisterSet.G0_REG) {
      appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr, tr));
      appendInstruction(new IntOpInstruction(Opcodes.ADD, tr, base, dest));
      return;
    }

    appendInstruction(new IntOpInstruction(Opcodes.OR, tr2, tr, dest));
    return;
  }

  /**
   * Generate instructions to load an immediate integer value added to the 
   * value in a register into a register.
   * @param value is the value to load
   * @param base is the base register (e.g., SparcRegisterSet.G0_REG)
   * @param dest is the register conatining the result
   */
  protected void genLoadImmediate(long value, int base, int dest)
  {
    if (value == 0) {
      genRegToReg(base, dest);
      if (registers.registerSize(base) < registers.registerSize(dest))
        genRegToReg(base, dest + 1);
      return;
    }

    boolean bis0 = (base == SparcRegisterSet.G0_REG);
    boolean disd = registers.doubleRegister(dest);

    if ((value >= MIN_IMM13) && (value <= MAX_IMM13)) {
      if (disd) {
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDCC,
                                                  bis0 ? base : base + 1,
                                                  getDisp((int) value),
                                                  FT_NONE,
                                                  dest + 1));
        appendInstruction(new IntOpLitInstruction(Opcodes.ADDC, base,
                                                  getDisp((int) (value >> 32)),
                                                  FT_NONE,
                                                  dest));
        lastInstruction.specifyNotSpillLoadPoint();
        return;
      }

      appendInstruction(new IntOpLitInstruction(Opcodes.ADD,
                                                base,
                                                getDisp((int) value),
                                                FT_NONE,
                                                dest));
      return;
    }

    int val  = (int) value;
    int vlo  = val & 0x0003ff;

    if ((val == value) || (value == (value & 0xffffffffL))) {
      if (vlo == 0) {
        if (!bis0) {
          int tr = registers.newTempRegister(RegisterSet.INTREG);
          if (disd) {
            Displacement disp = getDisp(val);
            appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
            appendInstruction(new IntOpInstruction(Opcodes.ADDCC, base + 1, tr, dest + 1));
            appendInstruction(new IntOpLitInstruction(Opcodes.ADDC, base,
                                                      getDisp((value < 0) ? -1 : 0),
                                                      FT_NONE,
                                                      dest));
            lastInstruction.specifyNotSpillLoadPoint();
          }

          if (v8plus && (value < 0)) {
            gen64BitImmediate(value, base, dest);
            return;
          }

          Displacement disp = getDisp(val);
          appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
          appendInstruction(new IntOpInstruction(Opcodes.ADD, base, tr, dest));
          return;
        } else {
          if (disd) {
            Displacement disp = getDisp(val);
            appendInstruction(new IntOpLitInstruction(Opcodes.OR, base, getDisp((value < 0) ? -1 : 0), FT_NONE, dest));
            appendInstruction(new SethiInstruction(Opcodes.SETHI, dest + 1, disp, FT_HI));
            return;
          }

          if (v8plus && (value < 0)) {
            gen64BitImmediate(value, base, dest);
            return;
          }

          Displacement disp = getDisp(val);
          appendInstruction(new SethiInstruction(Opcodes.SETHI, dest, disp, FT_HI));
          return;
        }
      } else {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        if (!bis0) {
          if (disd) {
            Displacement disp = getDisp(val);
            appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
            appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, disp, FT_LO, tr));
            appendInstruction(new IntOpInstruction(Opcodes.ADDCC, base + 1, tr, dest + 1));
            appendInstruction(new IntOpLitInstruction(Opcodes.ADDC, base, getDisp((value < 0) ? -1 : 0), FT_NONE, dest));
            lastInstruction.specifyNotSpillLoadPoint();
            return;
          }

          if (v8plus && (value < 0)) {
            gen64BitImmediate(value, base, dest);
            return;
          }

          Displacement disp = getDisp(val);
          appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
          appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, disp, FT_LO, tr));
          appendInstruction(new IntOpInstruction(Opcodes.ADD, base, tr, dest));
          return;
        } else {
          if (disd) {
            Displacement disp = getDisp(val);
            appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
            appendInstruction(new IntOpLitInstruction(Opcodes.ADDCC, tr, disp, FT_LO, dest + 1));
            appendInstruction(new IntOpLitInstruction(Opcodes.ADDC, base, getDisp(value < 0 ? -1 : 0), FT_NONE, dest));
            lastInstruction.specifyNotSpillLoadPoint();
            return;
          }

          if (v8plus && (value < 0)) {
            gen64BitImmediate(value, base, dest);
            return;
          }

          Displacement disp = getDisp(val);
          if ((val >= MIN_IMM13) && (val <= MAX_IMM13)) {
            appendInstruction(new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, disp, FT_NONE, dest));
            return;
          }

          appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
          appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, disp, FT_LO, dest));
          return;
        }
      }
    }

    if (v8plus) {
      gen64BitImmediate(value, base, dest);
      return;
    }

    Displacement disp = getDisp(val);
    int          val2  = (int) (value >> 32);
    Displacement disp2 = getDisp(val2);
    int          vlo2  = val2 & 0x0003ff;

    int tr = registers.newTempRegister(RegisterSet.INTREG + RegisterSet.DBLEREG);
    if ((val2 >= MIN_IMM13) && (val2 <= MAX_IMM13)) {
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, SparcRegisterSet.G0_REG, disp2, FT_NONE, bis0 ? dest : tr));
    } else if (vlo2 == 0) {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, bis0 ? dest : tr, disp2, FT_HI));
    } else {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp2, FT_HI));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, disp2, FT_LO, bis0 ? dest : tr));
    }

    if ((val >= MIN_IMM13) && (val <= MAX_IMM13)) {
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, SparcRegisterSet.G0_REG, disp, FT_NONE, bis0 ? dest + 1: tr + 1));
    } else if (vlo == 0) {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, bis0 ? dest + 1 : tr + 1, disp, FT_HI));
    } else {
      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr + 1, disp, FT_HI));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr + 1, disp, FT_LO, bis0 ? dest + 1 : tr + 1));
    }

    if (!bis0)
      genAddition(base, tr, dest, 8, true);
  }

  /**
   * Generate instructions to calculate a valid offset.
   * The *resultReg* register is set to the register containing the address to be used
   * and the remaining offset is returned.
   * @param value is the value to add to the base address
   * @param base is the base address
   * @return the lower 16 bits of the constant
   */
  protected long genLoadHighImmediate(long value, int base)
  {
    assert (((value >> 32) == 0) || ((value >> 32) == -1)) : "genLoadHighImmediate 0x" + Long.toHexString(value);
    int val = (int) value;

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = base;

    if ((val >= MIN_IMM13) && (val <= MAX_IMM13))
      return val;

    int low = val & 0x3ff;
    int tr  = registers.newTempRegister(RegisterSet.ADRREG);
    int tr2 = registers.newTempRegister(RegisterSet.ADRREG);

    appendInstruction(new SethiInstruction(Opcodes.SETHI, tr2, getDisp(val), FT_HI));
    appendInstruction(new IntOpInstruction(Opcodes.ADD, tr2, base, tr));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = tr;

    return low;
  }

  /**
   * Insert the instruction(s) to restore a spilled register.  At this
   * point, we are using a one-to-one mapping between real registers
   * and virtual registers, so only a single instruction is required.
   * @param reg specifies which virtual register will be loaded
   * @param spillLocation specifies the offset on the stack to the
   * spill location
   * @param after specifies the instruction to insert the load after
   * @return the last instruction inserted
   * @see #getSpillLocation
   */
  public Instruction insertSpillLoad(int         reg,
                                     Object      spillLocation,
                                     Instruction after)
  {
    if (spillLocation == null)
      return regenerateRegister(reg, after);

    int     opcode = -1;
    int     bs     = registers.registerSize(reg);
    boolean isFlt  = registers.floatRegister(reg);

    if (isFlt)
      opcode = Opcodes.ldfOp(bs, v8plus);
    else
      opcode = Opcodes.ldsiOp(bs, v8plus);

    StackDisplacement disp   = (StackDisplacement) spillLocation;
    long              offset = disp.getDisplacement();
    Instruction       sl     = lastInstruction;
    Instruction       next   = after.getNext();
    Instruction       load;

    lastInstruction = after;
    after.setNext(null);

    if ((offset > MAX_IMM13) || (offset < MIN_IMM13)) {
      int tr = reg;
      if (registers.floatRegister(reg))
        tr = SparcRegisterSet.G1_REG;

      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, disp, FT_LO, tr));
      load = new LoadInstruction(opcode, tr, SparcRegisterSet.FP_REG, reg);
      if (isFlt && (bs > 8)) {
        appendInstruction(load);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, getDisp(8), FT_NONE, tr));
        load = new LoadInstruction(opcode, tr, SparcRegisterSet.FP_REG, reg + 2);
      }
    } else {
      load = new LoadLitInstruction(opcode, SparcRegisterSet.FP_REG, disp, FT_NONE, reg);
      if (isFlt && (bs > 8)) {
        appendInstruction(load);
        load = new LoadLitInstruction(opcode, SparcRegisterSet.FP_REG, disp.offset(8), FT_NONE, reg + 2);
      }
    }

    appendInstruction(load);

    load.setNext(next);
    lastInstruction = sl;

    return load;
  }

  /**
   * Insert the instruction(s) to save a spilled register.  At this
   * point, we are using a one-to-one mapping between real registers
   * and virtual registers, so only a single instruction is required.
   * @param reg specifies which virtual register will be stored
   * @param spillLocation specifies the offset on the stack to the
   * spill location
   * @param after specifies the instruction to insert the store after
   * @return the last instruction inserted
   * @see #getSpillLocation
   */
  public Instruction insertSpillStore(int         reg,
                                      Object      spillLocation,
                                      Instruction after)
  {
    int     opcode = -1;
    int     bs     = registers.registerSize(reg);
    boolean isFlt  = registers.floatRegister(reg);

    if (isFlt)
      opcode = Opcodes.stfOp(bs, v8plus);
    else
      opcode = Opcodes.stiOp(bs, v8plus);

    StackDisplacement disp   = (StackDisplacement) spillLocation;
    long              offset = disp.getDisplacement();
    int               adr    = SparcRegisterSet.FP_REG;
    Instruction       sl     = lastInstruction;
    Instruction       next   = after.getNext();
    Instruction       store;

    lastInstruction = after;
    after.setNext(null);

    if ((offset > MAX_IMM13) || (offset < MIN_IMM13)) {
      int tr = SparcRegisterSet.G1_REG;
      appendInstruction(new SethiInstruction(Opcodes.SETHI, tr, disp, FT_HI));
      appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, disp, FT_LO, tr));
      store = new StoreInstruction(opcode, tr, SparcRegisterSet.FP_REG, reg);
      if (isFlt && (bs > 8)) {
        appendInstruction(store);
        appendInstruction(new IntOpLitInstruction(Opcodes.ADD, tr, getDisp(8), FT_NONE, tr));
        store = new StoreInstruction(opcode, tr, SparcRegisterSet.FP_REG, reg + 2);
      }
    } else {
      store = new StoreLitInstruction(opcode, SparcRegisterSet.FP_REG, disp, FT_NONE, reg);
      if (isFlt && (bs > 8)) {
        appendInstruction(store);
        store = new StoreLitInstruction(opcode, SparcRegisterSet.FP_REG, disp.offset(8), FT_NONE, reg + 2);
      }
    }

    appendInstruction(store);

    store.setNext(next);
    lastInstruction = sl;

    return store;
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    Type  ct  = processType(e);
    Expr  op  = e.getArg();
    Type  ot  = processType(op);
    int   reg = registers.getResultRegister(ct.getTag());
    int   bs  = ct.memorySizeAsInt(machine);

    if (ot.isComplexType()) {
      needValue(op);

      int     opreg = resultReg;
      int     bsa   = ot.memorySizeAsInt(machine);
      short[] uses  = new short[4];
      uses[0] = SparcRegisterSet.O0_REG;
      uses[1] = SparcRegisterSet.O1_REG;
      uses[2] = SparcRegisterSet.O2_REG;
      uses[3] = SparcRegisterSet.O3_REG;
      
      if (bsa > 8) {
        genRegToReg(opreg, SparcRegisterSet.O0_REG);
      } else {
        int tr1 = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.DBLEREG);
        int tr2 = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.DBLEREG);
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(4, 8), opreg, tr1));
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(4, 8), opreg + 1, tr2));
        genRegToReg(tr1, SparcRegisterSet.O0_REG);
        genRegToReg(tr2, SparcRegisterSet.O2_REG);
      }

      genFtnCall("f__cabs", uses, createRoutineDefs(true, bs));
      if (bs <= 4)
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(8, 4), SparcRegisterSet.F0_REG, reg));
      else
        genRegToReg(SparcV8RegisterSet.D0_REG, reg);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = reg;
      return;
    } 

    needValue(op);

    int raReg = resultReg;
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = reg;

    if (ot.isRealType()) {
      genRegToReg(raReg, resultReg);
      appendInstruction(new FltOpInstruction(Opcodes.FABSS, resultReg, resultReg));
      return;
    }

    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);

    if (bs > 4) {
      SparcBranch      br   = new BranchInstruction(Opcodes.BGE, disp, false, 2, null);
      SparcInstruction test = new IntOpInstruction(Opcodes.SUBCC, resultReg, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG);

      br.addTarget(labt, 0);
      br.addTarget(labf, 1);
      labf.setNotReferenced();
      test.setSetCC(ICC);

      genRegToReg(raReg, resultReg);
      appendInstruction(test);
      appendInstruction(br);
      appendLabel(labf);
      appendInstruction(new IntOpInstruction(Opcodes.SUBCC, SparcRegisterSet.G0_REG, raReg + 1, resultReg + 1));
      appendInstruction(new IntOpInstruction(Opcodes.SUBC, SparcRegisterSet.G0_REG, raReg, resultReg));
      appendLabel(labt);
      return;
    }

    SparcInstruction  delay = new IntOpInstruction(Opcodes.SUB, SparcRegisterSet.G0_REG, resultReg, resultReg);
    SparcBranch       br    = new BranchInstruction(Opcodes.BL, disp, true, 2, delay);
    AnnulMarker       am    = new AnnulMarker(delay);
    SparcInstruction  test  = new IntOpInstruction(Opcodes.SUBCC, resultReg, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG);

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);
    labf.setNotReferenced();
    test.setSetCC(ICC);

    genRegToReg(raReg, resultReg);
    appendInstruction(test);
    appendInstruction(br);
    appendLabel(labt);
    appendInstruction(am);
    appendLabel(labf);
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
    int  l       = pt.numFormals();
    int  fdi     = 0;

    if (!rt.isVoidType()) {
      int rts = rt.memorySizeAsInt(machine);
      if (isFortran() && (!rt.isAtomicType() || (rts > 8))) {
        nextArg++; // Result is passed in location specified as first argument.
      }
    }

    // Lay out the arguments in memory.

    while (nextArg < MAX_ARG_REGS) {
      if (fdi >= l)
        break;

      FormalDecl fd  = pt.getFormal(fdi++);

      if (fd instanceof UnknownFormals) {
        while (nextArg < MAX_ARG_REGS) {
          int          off  = (int) genLoadHighImmediate((ARG_SAVE_OFFSET +
                                                          (nextArg * SAVED_REG_SIZE)),
                                                         SparcRegisterSet.FP_REG);
          Displacement disp = getDisp(off);
          storeIntoMemory(SparcRegisterSet.I0_REG + nextArg,
                          resultReg,
                          disp,
                          FT_NONE,
                          SAVED_REG_SIZE,
                          4);
          nextArg++;
        }
        break;
      }

      Type    vt    = processType(fd);
      int     ts    = vt.memorySizeAsInt(machine);
      int     rinc  = (ts + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;
      int     src   = SparcRegisterSet.I0_REG + nextArg;

      if ((rinc > 2) || vt.isAggregateType()) { // Address of argument is passed in a register.
        nextArg++;
        continue;
      }

      // The first six scaler arguments are in the argument registers, 
      // remaining words have already been placed on the stack by the caller.

      Assigned loc = fd.getStorageLoc();
      if (loc == Assigned.IN_REGISTER) {
        int srcSize = (rinc * SAVED_REG_SIZE);
        int reg     = fd.getValueRegister();
        int treg    = convertIntRegValue(src, srcSize, vt.isSigned(), reg, ts, vt.isSigned());
        genRegToReg(treg, reg);
        if (!v8plus && (rinc == 2))
          genRegToReg(treg + 1, reg + 1);
        nextArg += rinc;
        continue;
      }

      assert (loc == Assigned.ON_STACK) : "Argument is where " + fd;

      // Caller placed all or part of the argument on the stack.

      boolean cvt = false;
      if (vt.isRealType() && pt.isOldStyle() && (rinc < 2)) { // Move and convert double to float.
        rinc = 2;
        cvt = true;
      }

      int sloc   = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;
      int offset = sloc;
      for (int ii = 0; ii < rinc; ii++) {
        if ((nextArg + ii) >= MAX_ARG_REGS)
          break;
        Displacement sdisp = new StackDisplacement(sloc);
        int          areg  = SparcRegisterSet.I0_REG + nextArg + ii;
        storeIntoMemory(areg, SparcRegisterSet.FP_REG, getDisp(offset), FT_NONE, SAVED_REG_SIZE, 4);
        offset += SAVED_REG_SIZE;
      }

      if (cvt) { // Move and convert double to float.
        int tr = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.DBLEREG);
        loadFromMemory(tr, SparcRegisterSet.FP_REG, getDisp(sloc), FT_NONE, 8, sloc, true);
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(8, 4), tr, tr));
        Displacement disp   = fd.getDisplacement();
        appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.FP_REG,disp, FT_NONE, tr));
      }

      nextArg += rinc;
    }

    while (fdi < l) { // Argument is passed on the stack.
      FormalDecl fd  = pt.getFormal(fdi++);

      if (fd instanceof UnknownFormals)
        break;

      Type vt   = processType(fd);
      int  ts   = vt.memorySizeAsInt(machine);
      int  rinc = (ts + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;

      int loc = ARG_SAVE_OFFSET + nextArg * SAVED_REG_SIZE;

      if (vt.isAggregateType()) { // Address of struct is on the stack.
        int tr = fd.getValueRegister();
        loadFromMemory(tr, SparcRegisterSet.FP_REG, getDisp(loc), FT_NONE, SAVED_REG_SIZE, loc, true);
        nextArg++;
        continue;
      }

      if (vt.isRealType() && pt.isOldStyle() && (rinc < 2)) {
        rinc = 2;
        int tr = registers.newTempRegister(RegisterSet.FLTREG + RegisterSet.DBLEREG);
        loadFromMemory(tr, SparcRegisterSet.FP_REG, getDisp(loc), FT_NONE, 8, loc, true);
        appendInstruction(new FltOpInstruction(Opcodes.fcvtfOp(8, 4), tr, tr));
        Displacement disp   = fd.getDisplacement();
        appendInstruction(new StoreLitInstruction(Opcodes.STF, SparcRegisterSet.FP_REG, disp, FT_NONE, tr));
      }

      nextArg += rinc;
    }

    lastInstruction.specifySpillStorePoint();
    appendLabel(startLabel);
  }

  /**
   * This method generates instructions to call a sub-function.  It
   * places arguments in the appropriate registers or on the stack as
   * required.
   */
  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    Type          rt         = processType(e);
    int           rts        = 0;
    Expr[]        args       = e.getArgumentArray();
    Expr          ftn        = e.getFunction();
    Declaration   decl       = null;
    SparcBranch   jsr;
    int           ir         = -1;

    // Check if there is an intrinsic this function could be replaced
    // with.
    
    boolean setjmp = false; // For setjmp which doesn't save complete environment.
    if (ftn instanceof LoadDeclAddressExpr) {
      decl = ((LoadExpr) ftn).getDecl();
      String fname = decl.getName();

      if (args.length == 1) {
        if (fname.equals("_scale_setjmp") || fname.equals("setjmp")) {
          RoutineDecl        rd      = (RoutineDecl) decl;
          int                handle  = ((SymbolDisplacement) rd.getDisplacement()).getHandle();
          SymbolDisplacement symDisp = new SymbolDisplacement("setjmp", handle);
          rd.setDisplacement(symDisp);
          setjmp = true;
        } else if (fname.equals("setjmp")) {
          setjmp = true;
        } else if (ansic && fname.equals("alloca")) {
          genAlloca(args[0], registers.getResultRegister(rt.getTag()));
          return;
        }
      } else if (args.length == 2) {
        if (fname.equals("_scale_longjmp")) {
          RoutineDecl        rd      = (RoutineDecl) decl;
          int                handle  = ((SymbolDisplacement) rd.getDisplacement()).getHandle();
          SymbolDisplacement symDisp = new SymbolDisplacement("longjmp", handle);
          rd.setDisplacement(symDisp);
        }
      }
    }

    boolean specialFirstArg = false;
    if (!rt.isVoidType()) {
      rts = rt.memorySizeAsInt(machine);
      if (!rt.isAtomicType() || (rts > 8)) {
        if (isFortran()) { // Result is passed in location specified as first argument.
          specialFirstArg = true;
        } else {
          // If the routine returns a structure, it is passed through
          // memory and the memory address to use is on the stack.

          int tr = registers.newTempRegister(RegisterSet.ADRREG);
          localVarSize += Machine.alignTo(rt.memorySizeAsInt(machine), 8);
          genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, tr);
          appendInstruction(new StoreLitInstruction(Opcodes.STW,
                                                    SparcRegisterSet.SP_REG,
                                                    getDisp(RETURN_STRUCT_OFFSET),
                                                    FT_NONE,
                                                    tr));
        }
      } else
        ir = registers.getResultRegister(rt.getTag());
    }

    short[] uses = callArgs(args, specialFirstArg);
    if (specialFirstArg) {
      localVarSize += Machine.alignTo(rts, 8);
      genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, SparcRegisterSet.O0_REG);
    }

    if (decl == null) { // Indirection
      needValue(ftn);

      jsr = new JmplInstruction(SparcRegisterSet.G0_REG,
                                resultReg,
                                SparcRegisterSet.O7_REG,
                                1,
                                null);
    } else {
      addrDisp = decl.getDisplacement().unique();

      if (decl.visibility() != Visibility.EXTERN) {
        jsr = new CallInstruction(addrDisp, 1, null);
      } else {
        int cr = registers.newTempRegister(RegisterSet.AIREG);
        appendInstruction(new SethiInstruction(Opcodes.SETHI, cr, addrDisp, FT_HI));
        jsr = new JmplLitInstruction(cr, addrDisp, FT_LO, SparcRegisterSet.O7_REG, 1, null);
      }
    }

    short[] kills = registers.getCalleeUses();

    if (rt.isVoidType()) {
      appendCallInstruction(jsr, createLabel(), uses, kills, null, true);
      if (setjmp)
        resetForBasicBlock();
      return;
    }

    if (specialFirstArg) {
      appendCallInstruction(jsr, createLabel(), uses, kills, null, true);
      genLoadImmediate(-localVarSize, SparcRegisterSet.FP_REG, SparcRegisterSet.O0_REG);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.ADDRESS;
      resultReg = SparcRegisterSet.O0_REG;
      if (setjmp)
        resetForBasicBlock();
      return;
    }

    if (!rt.isAtomicType() || (rts > 8)) { // Returning structs is a pain.
      appendCallInstruction(jsr, createLabel(), uses, kills, null, true);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.ADDRESS;
      resultReg = SparcRegisterSet.O0_REG;
      resultRegAddressAlignment = 4;
      jsr.setReturnedStructSize(rt.memorySizeAsInt(machine));
      if (setjmp)
        resetForBasicBlock();
      return;
    }

    // Normal value return.

    appendCallInstruction(jsr, createLabel(), uses, kills, createRoutineDefs(rt.isRealType(), rts), true);

    int vsz  = rt.getTag();
    int rreg = returnRegister(vsz, true);
    int nr   = registers.numContiguousRegisters(rreg);

    genRegToReg(rreg, ir);

    if (rt.isComplexType())
      genRegToReg(rreg + nr, ir + nr);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
    if (setjmp)
      resetForBasicBlock();
  }

  private void gen3WayFltCompare(int laReg, int raReg, int dest, int bs)
  {
    if (bs > 8)
      throw new scale.common.NotImplementedError("gen3WayFltCompare of " + bs);

    Label             labt1  = createLabel();
    Label             labf1  = createLabel();
    Label             labt2  = createLabel();
    Label             labf2  = createLabel();
    LabelDisplacement disp1  = new LabelDisplacement(labt1);
    LabelDisplacement disp2  = new LabelDisplacement(labt2);
    SparcInstruction  delay1 = new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(1), FT_NONE, dest);
    SparcInstruction  delay2 = new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(-1), FT_NONE, dest);
    SparcBranch       br1    = new BranchInstruction(Opcodes.FBG, disp1, true, 2, delay1);
    SparcBranch       br2    = new BranchInstruction(Opcodes.FBL, disp2, true, 2, delay2);
    AnnulMarker       am1    = new AnnulMarker(delay1);
    AnnulMarker       am2    = new AnnulMarker(delay2);

    br1.addTarget(labt1, 0);
    br1.addTarget(labf1, 1);
    labf1.setNotReferenced();

    br2.addTarget(labt2, 0);
    br2.addTarget(labf2, 1);
    labf2.setNotReferenced();

    appendInstruction(new FltCmpInstruction(Opcodes.cmpfOp(bs), laReg, raReg, FCC0)); 
    appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest)); 
    appendInstruction(br1);
    appendLabel(labt1);
    appendInstruction(am1);
    appendLabel(labf1);
    appendInstruction(br2);
    appendLabel(labt2);
    appendInstruction(am2);
    appendLabel(labf2);
  }

  private void gen3WayIntCompare(int laReg, int raReg, int dest, int bs)
  {
    if (bs > 4)
      throw new scale.common.NotImplementedError("gen3WayIntCompare of " + bs);

    Label             labt1  = createLabel();
    Label             labf1  = createLabel();
    Label             labt2  = createLabel();
    Label             labf2  = createLabel();
    LabelDisplacement disp1  = new LabelDisplacement(labt1);
    LabelDisplacement disp2  = new LabelDisplacement(labt2);
    SparcInstruction  delay1 = new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(1), FT_NONE, dest);
    SparcInstruction  delay2 = new IntOpLitInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, getDisp(-1), FT_NONE, dest);
    SparcBranch       br1    = new BranchInstruction(Opcodes.BG, disp1, true, 2, delay1);
    SparcBranch       br2    = new BranchInstruction(Opcodes.BL, disp2, true, 2, delay2);
    AnnulMarker       am1    = new AnnulMarker(delay1);
    AnnulMarker       am2    = new AnnulMarker(delay2);
    SparcInstruction  test   = new IntOpInstruction(Opcodes.SUBCC, laReg, raReg, SparcRegisterSet.G0_REG);

    test.setSetCC(ICC);
    br1.addTarget(labt1, 0);
    br1.addTarget(labf1, 1);
    labf1.setNotReferenced();

    br2.addTarget(labt2, 0);
    br2.addTarget(labf2, 1);
    labf2.setNotReferenced();

    appendInstruction(test); 
    appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest)); 
    appendInstruction(br1);
    appendLabel(labt1);
    appendInstruction(am1);
    appendLabel(labf1);
    appendInstruction(br2);
    appendLabel(labt2);
    appendInstruction(am2);
    appendLabel(labf2);
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

    switch (mode) {
    case CompareExpr.Normal:    /* Use normal compare.     */
      int bs = rt.memorySizeAsInt(machine);
      if (rt.isRealType()) {
        gen3WayFltCompare(laReg, raReg, ir, bs);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }

      gen3WayIntCompare(laReg, raReg, ir, bs);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;

    case CompareExpr.FloatL:      /* If either argument is Nan, return -1.       */
      break;
    case CompareExpr.FloatG:      /* If either argument is Nan, return 1.       */
      break;
    }

    throw new scale.common.NotImplementedError("CompareExpr not converted for floating point " + e);
  }

  private static final int DBYTE   = 0; // destination  8-bits
  private static final int DSHORT  = 1; // destination 16-bits
  private static final int DINT    = 2; // destination 32-bits
  private static final int DLLONG  = 3; // destination 64-bits
  private static final int SBYTE   = DBYTE  << 2; // source  8-bits
  private static final int SSHORT  = DSHORT << 2; // source 16-bits
  private static final int SINT    = DINT   << 2; // source 32-bits
  private static final int SLLONG  = DLLONG << 2; // source 64-bits
  private static final int SSIGNED = 1 << 4;
  private static final int DSIGNED = 1 << 5;

  private static final int[] smapSize = {-1, SBYTE, SSHORT, -1, SINT, -1, -1, -1, SLLONG};
  private static final int[] dmapSize = {-1, DBYTE, DSHORT, -1, DINT, -1, -1, -1, DLLONG};

  private static final int RSRC       =  0; // Return source
  private static final int ESB        =  1; // Extract byte & sign extend to 32-bit int
  private static final int ESBX       =  2; // Extract byte & sign extend to 64-bit int
  private static final int ESS        =  3; // Extract short & sign extend to 32-bit int
  private static final int ESSX       =  4; // Extract short & sign extend to 64-bit int
  private static final int ESI        =  5; // Extract int & sign extend to 32-bit int
  private static final int ESIX       =  6; // Extract int & sign extend to 64-bit int
  private static final int EUB        =  7; // Extract unsigned byte to 32-bit int
  private static final int EUBX       =  8; // Extract unsigned byte to 64-bit int
  private static final int EUS        =  9; // Extract unsigned short to 32-bit int
  private static final int EUSX       = 10; // Extract unsigned short to 64-bit int
  private static final int EUI        = 11; // Extract unsigned int to 32-bit int
  private static final int EUIX       = 12; // Extract unsigned int to 64-bit int
  private static final int ESBL       = 13; // Extract byte & sign extend to long long
  private static final int ESSL       = 14; // Extract short & sign extend to long long
  private static final int ESIL       = 15; // Extract int & sign extend to long long
  private static final int EUBL       = 16; // Extract unsigned byte to long long
  private static final int EUSL       = 17; // Extract unsigned short to long long
  private static final int EUIL       = 18; // Extract unsigned int to long long
  private static final int ELSB       = 19; // Extract long long to signed byte
  private static final int ELSS       = 20; // Extract long long to signed short
  private static final int ELSI       = 21; // Extract long long to signed int
  private static final int ELUB       = 22; // Extract long long to unsigned byte
  private static final int ELUS       = 23; // Extract long long to unsigned short
  private static final int ELUI       = 24; // Extract long long to unsigned int
  private static final String[] convs = {
    "0-bytes", "byte", "short", "3-bytes",
    "int", "5-bytes", "6-bytes", "7-bytes",
    "long long"};

  private static final byte[] ccasev8 = {
    RSRC, //                     SBYTE  + DBYTE
    RSRC, //                     SBYTE  + DSHORT
    RSRC, //                     SBYTE  + DINT
    EUBL, //                     SBYTE  + DLLONG
    EUB,  //                     SSHORT + DBYTE
    RSRC, //                     SSHORT + DSHORT
    RSRC, //                     SSHORT + DINT
    EUSL, //                     SSHORT + DLLONG
    EUB,  //                     SINT   + DBYTE
    EUS,  //                     SINT   + DSHORT
    RSRC, //                     SINT   + DINT
    EUIL, //                     SINT   + DLLONG
    ELUB, //                     SLLONG + DBYTE
    ELUS, //                     SLLONG + DSHORT
    ELUI, //                     SLLONG + DINT
    RSRC, //                     SLLONG + DLLONG

    EUB,  // SSIGNED           + SBYTE  + DBYTE
    EUS,  // SSIGNED           + SBYTE  + DSHORT
    EUI,  // SSIGNED           + SBYTE  + DINT
    ESBL, // SSIGNED           + SBYTE  + DLLONG
    EUB,  // SSIGNED           + SSHORT + DBYTE
    EUS,  // SSIGNED           + SSHORT + DSHORT
    EUI,  // SSIGNED           + SSHORT + DINT
    ESSL, // SSIGNED           + SSHORT + DLLONG
    EUB,  // SSIGNED           + SINT   + DBYTE
    EUS,  // SSIGNED           + SINT   + DSHORT
    EUI,  // SSIGNED           + SINT   + DINT
    ESIL, // SSIGNED           + SINT   + DLLONG
    ELUB, // SSIGNED           + SLLONG + DBYTE
    ELUS, // SSIGNED           + SLLONG + DSHORT
    ELUI, // SSIGNED           + SLLONG + DINT
    RSRC, // SSIGNED           + SLLONG + DLLONG

    ESB,  //           DSIGNED + SBYTE  + DBYTE
    RSRC, //           DSIGNED + SBYTE  + DSHORT
    RSRC, //           DSIGNED + SBYTE  + DINT
    EUBL, //           DSIGNED + SBYTE  + DLLONG
    ESB,  //           DSIGNED + SSHORT + DBYTE
    ESS,  //           DSIGNED + SSHORT + DSHORT
    RSRC, //           DSIGNED + SSHORT + DINT
    EUSL, //           DSIGNED + SSHORT + DLLONG
    ESB,  //           DSIGNED + SINT   + DBYTE
    ESS,  //           DSIGNED + SINT   + DSHORT
    ESI,  //           DSIGNED + SINT   + DINT
    EUIL, //           DSIGNED + SINT   + DLLONG
    ELSB, //           DSIGNED + SLLONG + DBYTE
    ELSS, //           DSIGNED + SLLONG + DSHORT
    ELSI, //           DSIGNED + SLLONG + DINT
    RSRC, //           DSIGNED + SLLONG + DLLONG

    RSRC, // SSIGNED + DSIGNED + SBYTE  + DBYTE
    RSRC, // SSIGNED + DSIGNED + SBYTE  + DSHORT
    RSRC, // SSIGNED + DSIGNED + SBYTE  + DINT
    ESBL, // SSIGNED + DSIGNED + SBYTE  + DLLONG
    ESB,  // SSIGNED + DSIGNED + SSHORT + DBYTE
    RSRC, // SSIGNED + DSIGNED + SSHORT + DSHORT
    RSRC, // SSIGNED + DSIGNED + SSHORT + DINT
    ESSL, // SSIGNED + DSIGNED + SSHORT + DLLONG
    ESB,  // SSIGNED + DSIGNED + SINT   + DBYTE
    ESS,  // SSIGNED + DSIGNED + SINT   + DSHORT
    RSRC, // SSIGNED + DSIGNED + SINT   + DINT
    ESIL, // SSIGNED + DSIGNED + SINT   + DLLONG
    ELSB, // SSIGNED + DSIGNED + SLLONG + DBYTE
    ELSS, // SSIGNED + DSIGNED + SLLONG + DSHORT
    ELSI, // SSIGNED + DSIGNED + SLLONG + DINT
    RSRC, // SSIGNED + DSIGNED + SLLONG + DLLONG
  };

  private static final byte[] ccasev8plus = {
    RSRC, //                     SBYTE  + DBYTE
    RSRC, //                     SBYTE  + DSHORT
    RSRC, //                     SBYTE  + DINT
    RSRC, //                     SBYTE  + DLLONG
    EUBX, //                     SSHORT + DBYTE
    RSRC, //                     SSHORT + DSHORT
    RSRC, //                     SSHORT + DINT
    RSRC, //                     SSHORT + DLLONG
    EUB,  //                     SINT   + DBYTE
    EUSX, //                     SINT   + DSHORT
    RSRC, //                     SINT   + DINT
    RSRC, //                     SINT   + DLLONG
    EUBX, //                     SLLONG + DBYTE
    EUSX, //                     SLLONG + DSHORT
    EUIX, //                     SLLONG + DINT
    RSRC, //                     SLLONG + DLLONG

    EUBX, // SSIGNED           + SBYTE  + DBYTE
    EUSX, // SSIGNED           + SBYTE  + DSHORT
    EUIX, // SSIGNED           + SBYTE  + DINT
    ESBX, // SSIGNED           + SBYTE  + DLLONG
    EUBX, // SSIGNED           + SSHORT + DBYTE
    EUSX, // SSIGNED           + SSHORT + DSHORT
    EUIX, // SSIGNED           + SSHORT + DINT
    ESSX, // SSIGNED           + SSHORT + DLLONG
    EUBX, // SSIGNED           + SINT   + DBYTE
    EUSX, // SSIGNED           + SINT   + DSHORT
    EUIX, // SSIGNED           + SINT   + DINT
    ESIX, // SSIGNED           + SINT   + DLLONG
    EUBX, // SSIGNED           + SLLONG + DBYTE
    EUSX, // SSIGNED           + SLLONG + DSHORT
    EUIX, // SSIGNED           + SLLONG + DINT
    RSRC, // SSIGNED           + SLLONG + DLLONG

    ESBX, //           DSIGNED + SBYTE  + DBYTE
    RSRC, //           DSIGNED + SBYTE  + DSHORT
    RSRC, //           DSIGNED + SBYTE  + DINT
    EUBX, //           DSIGNED + SBYTE  + DLLONG
    ESBX, //           DSIGNED + SSHORT + DBYTE
    ESSX, //           DSIGNED + SSHORT + DSHORT
    RSRC, //           DSIGNED + SSHORT + DINT
    EUSX, //           DSIGNED + SSHORT + DLLONG
    ESBX, //           DSIGNED + SINT   + DBYTE
    ESSX, //           DSIGNED + SINT   + DSHORT
    ESIX, //           DSIGNED + SINT   + DINT
    EUIX, //           DSIGNED + SINT   + DLLONG
    ESBX, //           DSIGNED + SLLONG + DBYTE
    ESSX, //           DSIGNED + SLLONG + DSHORT
    ESIX, //           DSIGNED + SLLONG + DINT
    RSRC, //           DSIGNED + SLLONG + DLLONG

    RSRC, // SSIGNED + DSIGNED + SBYTE  + DBYTE
    RSRC, // SSIGNED + DSIGNED + SBYTE  + DSHORT
    RSRC, // SSIGNED + DSIGNED + SBYTE  + DINT
    RSRC, // SSIGNED + DSIGNED + SBYTE  + DLLONG
    ESBX, // SSIGNED + DSIGNED + SSHORT + DBYTE
    RSRC, // SSIGNED + DSIGNED + SSHORT + DSHORT
    RSRC, // SSIGNED + DSIGNED + SSHORT + DINT
    RSRC, // SSIGNED + DSIGNED + SSHORT + DLLONG
    ESBX, // SSIGNED + DSIGNED + SINT   + DBYTE
    ESSX, // SSIGNED + DSIGNED + SINT   + DSHORT
    RSRC, // SSIGNED + DSIGNED + SINT   + DINT
    RSRC, // SSIGNED + DSIGNED + SINT   + DLLONG
    ESBX, // SSIGNED + DSIGNED + SLLONG + DBYTE
    ESSX, // SSIGNED + DSIGNED + SLLONG + DSHORT
    ESIX, // SSIGNED + DSIGNED + SLLONG + DINT
    RSRC, // SSIGNED + DSIGNED + SLLONG + DLLONG
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
    if (src == SparcRegisterSet.G0_REG)
      return src;

    int which = smapSize[srcSize] + dmapSize[destSize];
    if (srcSigned)
      which += SSIGNED;
    if (destSigned)
      which += DSIGNED;

    int sel = v8plus ? ccasev8plus[which] : ccasev8[which];
    switch (sel) {
    case RSRC: // Return source
      return src;
    case ESBX: // Extract byte & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, src, getDisp(56), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRAX, dest, getDisp(56), FT_NONE, dest));
      return dest;
    case ESB: // Extract byte & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(24), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, getDisp(24), FT_NONE, dest));
      return dest;
    case ESSX: // Extract short & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, src, getDisp(48), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRAX, dest, getDisp(48), FT_NONE, dest));
      return dest;
    case ESS: // Extract short & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(16), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, getDisp(16), FT_NONE, dest));
      return dest;
    case ESIX: // Extract int & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, src, getDisp(32), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRAX, dest, getDisp(32), FT_NONE, dest));
      return dest;
    case ESI: // Extract int & sign extend
      return src;
    case EUBX: // Extract unsigned byte
    case EUB: // Extract unsigned byte
      appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, getDisp(0xff), FT_NONE, dest));
      return dest;
    case EUSX: // Extract unsigned short
      appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, src, getDisp(48), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRLX, dest, getDisp(48), FT_NONE, dest));
      return dest;
    case EUS: // Extract unsigned short
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(16), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, dest, getDisp(16), FT_NONE, dest));
      return dest;
    case EUIX: // Extract unsigned int
      appendInstruction(new IntOpLitInstruction(Opcodes.SLLX, src, getDisp(32), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRLX, dest, getDisp(32), FT_NONE, dest));
      return dest;
    case EUI: // Extract unsigned int
      return src;
    case ESBL: // Extract byte & sign extend
      int trb = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(24), FT_NONE, trb));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, trb, getDisp(31), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, trb, getDisp(24), FT_NONE, dest + 1));
      return dest;
    case ESSL: // Extract short & sign extend
      int trs = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(16), FT_NONE, trs));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, trs, getDisp(31), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, trs, getDisp(16), FT_NONE, dest + 1));
      return dest;
    case ESIL: // Extract int & sign extend
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, src, getDisp(31), FT_NONE, dest));
      appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, src + 0, dest + 1));
      return dest;
    case EUBL: // Extract unsigned byte
      appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.AND, src, getDisp(0xff), FT_NONE, dest + 1));
      return dest;
    case EUSL: // Extract unsigned short
      int dest2 =  dest + 1;
      appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src, getDisp(16), FT_NONE, dest2));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, dest2, getDisp(16), FT_NONE, dest2));
      return dest;
    case EUIL: // Extract unsigned int
      appendInstruction(new IntOpInstruction(Opcodes.OR, SparcRegisterSet.G0_REG, SparcRegisterSet.G0_REG, dest));
      appendInstruction(new IntOpInstruction(Opcodes.OR, src + 0, SparcRegisterSet.G0_REG, dest + 1));
      return dest;
    case ELSB: // Extract long long to signed byte
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src + 1, getDisp(24), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, getDisp(24), FT_NONE, dest));
      return dest;
    case ELSS: // Extract long long to signed short
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src + 1, getDisp(16), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRA, dest, getDisp(16), FT_NONE, dest));
      return dest;
    case ELSI: // Extract long long to signed int
      appendInstruction(new IntOpInstruction(Opcodes.OR, src + 1, SparcRegisterSet.G0_REG, dest));
      return dest;
    case ELUB: // Extract long long to unsigned byte
      appendInstruction(new IntOpLitInstruction(Opcodes.AND, src + 1, getDisp(0xff), FT_NONE, dest));
      return dest;
    case ELUS: // Extract long long to unsigned short
      appendInstruction(new IntOpLitInstruction(Opcodes.SLL, src + 1, getDisp(16), FT_NONE, dest));
      appendInstruction(new IntOpLitInstruction(Opcodes.SRL, dest, getDisp(16), FT_NONE, dest));
      return dest;
    case ELUI: // Extract long long to unsigned int
      appendInstruction(new IntOpInstruction(Opcodes.OR, src + 1, SparcRegisterSet.G0_REG, dest));
      return dest;
    }
    throw new scale.common.InternalError("Funny conversion: " +
                                         (srcSigned ? "" : "signed ") +
                                         convs[srcSize] +
                                         " to " +
                                         (destSigned ? "" : "signed ") +
                                         convs[destSize]);
  }

  /**
   * Generate a branch based on the value of an expression compared to zero.
   * The value is always integer on the Sparc and it is never a value pair.
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
    LabelDisplacement disp = new LabelDisplacement(labf);

    if (v8plus) {
      SparcBranch br = new BranchRegInstruction(Opcodes.bropOp(which.ordinal()), treg, disp, false, 2, null);
      br.addTarget(labf, 0);
      br.addTarget(labt, 1);
      appendInstruction(br);
      return;
    }

    which = genCCLit(which, treg, 0, registers.registerSize(treg));

    int         opcode = Opcodes.bopiOp(which.ordinal(), false, signed);
    SparcBranch br     = new BranchInstruction(opcode, disp, false, 2, null);

    br.addTarget(labf, 0);
    br.addTarget(labt, 1);

    appendInstruction(br);
  }

  public void visitNegativeExpr(NegativeExpr e)
  {
    Type ct  = processType(e);
    int  bs  = ct.memorySizeAsInt(machine);
    Expr arg = e.getArg();
    int  ir  = registers.getResultRegister(ct.getTag());

    needValue(arg);

    if (resultReg == SparcRegisterSet.G0_REG)
      return;

    if (ct.isRealType()) {
      int nr = registers.numContiguousRegisters(ir);
      genRegToReg(resultReg, ir);
      // Only have to do top half if destination and source are the same.
      appendInstruction(new FltOpInstruction(Opcodes.FNEGS, ir, ir));
      if (ct.isComplexType())
        appendInstruction(new FltOpInstruction(Opcodes.FNEGS, ir + nr, ir + nr));
    } else if ((bs <= 4) || (v8plus && (bs <= 8))) {
      appendInstruction(new IntOpInstruction(Opcodes.SUB, SparcRegisterSet.G0_REG, resultReg, ir));
    } else {
      appendInstruction(new IntOpInstruction(Opcodes.SUBCC, SparcRegisterSet.G0_REG, resultReg + 1, ir + 1));
      appendInstruction(new IntOpInstruction(Opcodes.SUBC, SparcRegisterSet.G0_REG, resultReg, ir));
      lastInstruction.specifyNotSpillLoadPoint();
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private static final int[] fbrop = {Opcodes.FBNE, Opcodes.FBG, Opcodes.FBGE,
                                      Opcodes.FBLE, Opcodes.FBL, Opcodes.FBE};
  private static final int[] ibrop = {Opcodes.BNE, Opcodes.BG, Opcodes.BGE,
                                      Opcodes.BLE, Opcodes.BL, Opcodes.BE};

  public void visitConditionalExpr(ConditionalExpr e)
  {
    Type              ct    = processType(e);
    int               tr    = registers.getResultRegister(ct.getTag());
    int               bs    = ct.memorySizeAsInt(machine);
    Label             labt  = createLabel();
    Label             labf  = createLabel();
    LabelDisplacement disp  = new LabelDisplacement(labt);

    Expr predicate = e.getTest();
    Expr trueExpr  = e.getTrueExpr();
    Expr falseExpr = e.getFalseExpr();

    needValue(trueExpr);
    int treg = resultReg;

    needValue(falseExpr);
    int freg = resultReg;

    SparcInstruction delay   = null;
    SparcBranch      br      = null;
    Instruction      cmpi    = null;
    boolean          neednop = false;

    if (predicate.isMatchExpr()) {
      MatchExpr   be  = (MatchExpr) predicate;
      Expr        la  = be.getLeftArg();
      Expr        ra  = be.getRightArg();
      CompareMode cmp = be.getMatchOp();
      int         bs2 = processType(ra).memorySizeAsInt(machine);

      needValue(la);
      int laReg = resultReg;
      needValue(ra);
      int raReg = resultReg;

      if ((tr == treg) || (tr == freg))
        tr = registers.newTempRegister(ct.getTag());

      if (bs <= 4) { // We will use an annulled branch on the V8 chip for word size values.
        if (ct.isRealType())
          delay = new FltOpInstruction(Opcodes.movfOp(bs), freg, tr);
        else
          delay = new IntOpInstruction(Opcodes.OR, freg, SparcRegisterSet.G0_REG, tr);
      }

      if (delay == null)
        cmp = cmp.reverse();

      genRegToReg(treg, tr);

      if (la.getCoreType().isRealType()) {
        br   = new BranchInstruction(fbrop[cmp.ordinal()], disp, delay != null, 2, delay);
        cmpi = new FltCmpInstruction(Opcodes.cmpfOp(bs2), laReg, raReg, FCC0);
        neednop = true;
      } else {
        br   = new BranchInstruction(ibrop[cmp.ordinal()], disp, delay != null, 2, delay);
        cmpi = new IntOpInstruction(Opcodes.SUBCC, laReg, raReg, SparcRegisterSet.G0_REG);
      }
    } else {
      needValue(predicate);
      int         preg = resultReg;
      CompareMode cmp  = CompareMode.NE;

      if ((tr == treg) || (tr == preg) || (tr == freg))
        tr = registers.newTempRegister(ct.getTag());

      if (bs <= 4) { // We will use an annulled branch on the V8 chip for word size values.
        if (ct.isRealType())
          delay = new FltOpInstruction(Opcodes.movfOp(bs), freg, tr);
        else
          delay = new IntOpInstruction(Opcodes.OR, freg, SparcRegisterSet.G0_REG, tr);
      }

      if (delay == null)
        cmp = cmp.reverse();

      genRegToReg(treg, tr);

      br   = new BranchInstruction(ibrop[cmp.ordinal()], disp, delay != null, 2, delay);
      cmpi = new IntOpInstruction(Opcodes.ORCC, preg, SparcRegisterSet.G0_REG, preg);
    }

    br.addTarget(labt, 0);
    br.addTarget(labf, 1);

    appendInstruction(cmpi);
    if (neednop)
      appendInstruction(new SethiInstruction(Opcodes.NOP, 0, getDisp(0), FT_NONE));
    appendInstruction(br);

    if (delay == null) {
      appendLabel(labf);
      genRegToReg(freg, tr);
      appendLabel(labt);
    } else {
      labf.setNotReferenced();

      appendLabel(labt);
      appendInstruction(new AnnulMarker(delay));
      appendLabel(labf);
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = tr;
  }

  private static final short[] intReturn = {SparcRegisterSet.I0_REG};

  public void visitReturnChord(ReturnChord c)
  {
    Expr a = c.getResultValue();

    if (a == null) {
      generateReturn(null);
      return;
    }

    Type at = processType(a);
    int  bs = at.memorySizeAsInt(machine);

    if (isFortran() && (!at.isAtomicType() || (bs > 8))) {
      // Result is passed in location specified as first argument.
      a.visit(this);

      if (resultRegMode == ResultMode.ADDRESS)
        moveWords(resultReg, resultRegAddressOffset,
                  SparcRegisterSet.I0_REG,
                  0,
                  bs,
                  resultRegAddressAlignment);
      else // Complex result?
        storeIntoMemory(resultReg, SparcRegisterSet.I0_REG, getDisp(0), FT_NONE, bs, 4);

      generateReturn(intReturn);
      return;
    }

    if (bs > 8) { // A large result is returned in memory.
      a.visit(this);

      appendInstruction(new LoadLitInstruction(Opcodes.LD,
                                               SparcRegisterSet.FP_REG,
                                               new StackDisplacement(64),
                                               FT_NONE,
                                               SparcRegisterSet.I0_REG));

      if (resultRegMode == ResultMode.ADDRESS)
        moveWords(resultReg, resultRegAddressOffset,
                  SparcRegisterSet.I0_REG,
                  0,
                  bs,
                  resultRegAddressAlignment);
      else // Complex result?
        storeIntoMemory(resultReg, SparcRegisterSet.I0_REG, getDisp(0), FT_NONE, bs, 4);

      generateReturn(intReturn);
      return;
    }

    int     dest = returnRegister(at.getTag(), false);
    int     nr   = (bs + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;
    boolean flg  = at.isAtomicType();

    if (flg)
      registers.setResultRegister(dest);

    a.visit(this);
    int  vReg    = resultReg;
    long vRegOff = resultRegAddressOffset;

    if (flg)
      registers.setResultRegister(-1);

    if (resultRegMode == ResultMode.ADDRESS) { // A small structure is returned in memory.
      appendInstruction(new LoadLitInstruction(Opcodes.LD,
                                               SparcRegisterSet.FP_REG,
                                               new StackDisplacement(64),
                                               FT_NONE,
                                               SparcRegisterSet.I0_REG));
      moveWords(vReg, vRegOff, SparcRegisterSet.I0_REG, 0, bs, resultRegAddressAlignment);
      generateReturn(intReturn);
      return;
    }

    if (vRegOff != 0) {
      int tr = registers.newTempRegister(registers.getType(vReg));
      genLoadImmediate(vRegOff, vReg, tr);
      vReg = tr;
    }

    // Normal values are returned in a register.

    short[] uses = new short[nr];
    boolean mv   = (registers.rangeBegin(vReg) != registers.rangeBegin(dest));
    if (at.isRealType()) {
      for (int i = 0; i < nr; i++)
        uses[i] = (short) (dest + i);
      if (mv)
        genRegToReg(vReg, dest);
    } else {
      if (v8plus && (nr > 1) && (registers.rangeBegin(vReg) != registers.rangeBegin(dest))) {
        appendInstruction(new IntOpInstruction(Opcodes.OR, vReg, SparcRegisterSet.G0_REG, dest));
        appendInstruction(new IntOpInstruction(Opcodes.OR, vReg + 1, SparcRegisterSet.G0_REG, dest + 1));
        vReg = dest;
        mv = false;
      }

      for (int i = 0; i < nr; i++) {
        uses[i] = (short) (dest + i);
        if (mv)
          appendInstruction(new IntOpInstruction(Opcodes.OR,
                                                 registers.rangeBegin(vReg) + i,
                                                 SparcRegisterSet.G0_REG,
                                                 registers.rangeBegin(dest) + i));
      }
    }

    generateReturn(uses);
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    FormalDecl    parmN  = e.getParmN();
    Expr          vaList = e.getVaList();
    Type          vat    = processType(vaList);
    ProcedureType pt     = (ProcedureType) processType(currentRoutine);
    int           offset = 0;
    int           l      = pt.numFormals();

    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      Type       ft = fd.getCoreType();
      int        bs = SAVED_REG_SIZE;

      if (ft.isAtomicType())
        bs = ((ft.memorySizeAsInt(machine) + SAVED_REG_SIZE - 1) &
              ~(SAVED_REG_SIZE - 1));

      offset += bs;

      if (fd == parmN) {
        needValue(vaList);

        int adr  = resultReg;
        int ir   = registers.newTempRegister(RegisterSet.INTREG);
        int size = vat.memorySizeAsInt(machine);

        genLoadImmediate(offset + ARG_SAVE_OFFSET, SparcRegisterSet.FP_REG, ir);
        storeIntoMemoryWithOffset(ir, adr, 0, size, 4, false);

        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    throw new scale.common.InternalError("Parameter not found " + parmN);
  }

  public void visitVaArgExpr(VaArgExpr e)
  {
    Expr vaList = e.getVaList();
    Type ct     = processType(e);
    int  bs     = ct.memorySizeAsInt(machine);
    int  ir     = registers.getResultRegister(ct.getTag());
    int  inc    = (bs + SAVED_REG_SIZE - 1) & ~(SAVED_REG_SIZE - 1);
    int  tr     = ir;

    boolean    getAdr = false;
    ResultMode isAdr  = ResultMode.NORMAL_VALUE;
    if (!ct.isAtomicType()) {
      inc = SAVED_REG_SIZE;
      tr = registers.newTempRegister(RegisterSet.ADRREG);
      isAdr = ResultMode.ADDRESS;
      bs = SAVED_REG_SIZE;
      ir = tr;
    } else if (bs > MAX_ARG_SIZE) {
      inc = SAVED_REG_SIZE;
      tr = registers.newTempRegister(RegisterSet.ADRREG);
      getAdr = true;
    }

    if (vaList instanceof LoadDeclAddressExpr) { // Generate better code by avoiding the visit.
      VariableDecl val  = (VariableDecl) ((LoadDeclAddressExpr) vaList).getDecl();
      Type         valt = processType(val);

      if (val.getStorageLoc() == Assigned.ON_STACK) {
        Displacement disp = val.getDisplacement();
        int          incs = valt.memorySizeAsInt(machine);
        int          adr  = registers.newTempRegister(RegisterSet.ADRREG);

        loadFromMemoryWithOffset(adr, stkPtrReg, disp, 4, machine.stackAlignment(valt), false, false);
        loadFromMemory(tr, adr, getDisp(0), FT_NONE, inc, 4, false); // Load arg
        genAdditionLit(adr, inc, adr, incs, true);
        storeIntoMemoryWithOffset(adr, stkPtrReg, disp, 4, machine.stackAlignment(valt), false);

        if (getAdr) // Arg is address of actual arg
          loadFromMemory(ir, tr, getDisp(0), FT_NONE, bs, 4, ct.isSigned());

        resultRegAddressOffset = 0;
        resultRegMode = isAdr;
        resultReg = ir;
        return;
      }
    }

    Type valt = processType(vaList.getCoreType().getPointedTo());
    int  incs = valt.memorySizeAsInt(machine);
    int  adr  = registers.getResultRegister(valt.getTag());

    vaList.visit(this);
    int va = resultReg;

    loadFromMemory(adr, va, getDisp(0), FT_NONE, incs, 4, false);
    loadFromMemory(tr, adr, getDisp(0), FT_NONE, inc, 4, false);
    genAdditionLit(adr, inc, adr, incs, true);
    storeIntoMemory(adr, va, getDisp(0), FT_NONE, incs, 4);

    if (getAdr)
      loadFromMemory(ir, tr, getDisp(0), FT_NONE, bs, 4, ct.isSigned());

    resultRegAddressOffset = 0;
    resultRegMode = isAdr;
    resultReg = ir;
  }
}
