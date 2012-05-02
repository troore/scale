package scale.backend;

import scale.common.Stack;
import java.util.Enumeration;
import java.util.Iterator;

import scale.common.*;
import scale.frontend.SourceLanguage;
import scale.callGraph.*;
import scale.clef.decl.*;
import scale.clef.expr.*;
import scale.clef.type.*;
import scale.clef.LiteralMap;
import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;

/** 
 * This class is the base class for code generators.
 * <p>
 * $Id: Generator.java,v 1.303 2007-10-04 19:57:48 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * A generator translates Scribble CFGs into a linear sequence of
 * machine instructions.
 * <p>
 * <b>The primary rule</b>: <em>Thou shall not use a register obtained
 * from a visit() as a destination register until all other registers
 * obtained from visit() have been used for the last time</em> for the
 * simple reason that they may be the same register.
 * <p>
 * <b>The secondary rule</b>:<em>Thou shall not define the destination
 * register until the very last instruction in the sequence </em> for
 * the simple reason that the register allocator does not know where a
 * sequence begins and ends.
 * <p>
 * The most important methods are those dealing with the calling
 * conventions.  The {@link #visitCallFunctionExpr
 * visitCallFunctionExpr} method operates the viewpoint of the caller
 * and generates the instructions to call a sub-function.  The {@link
 * #generateProlog generateProlog}, {@link #layoutParameters
 * layoutParameters}, and {@link #endRoutineCode endRoutineCode}
 * methods operate from the viewpoint of the callee.
 */

public abstract class Generator implements scale.score.Predicate
{
  private static int avoidedLoadCount       = 0; // How many variable loads were eliminated.
  private static int avoidedAdrCount        = 0; // How many variable address calculations were eliminated.
  private static int regenerateAddressCount = 0; // How many spill loads were avoided because of regenerating the address value.
  private static int regenerateValueCount   = 0; // How many spills were avoided because of regenerating the value.
  private static int regenerateLiteralCount = 0; // How many spills were avoided because of regenerating the literal value.

  private static final String[] stats = {"avoidedLoads",
                                         "avoidedAddressCalcs",
                                         "regeneratedAddresses",
                                         "regeneratedValues",
                                         "regeneratedLiterals"};

  static
  {
    Statistics.register("scale.backend.Generator", stats);
  }

  /**
   * Return the number of variable loads that were eliminated.
   */
  public static int avoidedLoads()
  {
    return avoidedLoadCount;
  }

  /**
   * Return the number of variable address calculations that were eliminated.
   */
  public static int avoidedAddressCalcs()
  {
    return avoidedAdrCount;
  }

  /**
   * Return the number of spill loads that were avoided because of
   * regenerating the address value.
   */
  public static int regeneratedAddresses()
  {
    return regenerateAddressCount;
  }

  /**
   * Return the number of spills were avoided because of regenerating
   * the value.
   */
  public static int regeneratedValues()
  {
    return regenerateValueCount;
  }

  /**
   * Return the number of spills were avoided because of regenerating
   * the literal value.
   */
  public static int regeneratedLiterals()
  {
    return regenerateLiteralCount;
  }

  private static final Long long0 = new Long(0);

  /**
   * Debug info should be generated.
   */
  public static final int DEBUG = 1;
  /**
   * Indirect loads are not aligned.
   */
  public static final int NALN = 2;
  /**
   * Inhibit instruction scheduling.
   */
  public static final int NIS = 4;
  /**
   * Inhibit peephole optimization.
   */
  public static final int NPH = 8;
  /**
   * Specify ANSI C. 
   */
  public static final int ANSIC = 32;
  /**
   * Generate source line information. 
   */
  public static final int LINENUM = 64;
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * True if annotation comments are to be generated.
   * Not all code generators provide annotations.
   */
  public static boolean annotateCode = false;
  /**
   * Add operation
   * @see #commutative
   */
  protected static final int ADD =  0;
  /**
   * Subtract operation
   * @see #commutative
   */
  protected static final int SUB =  1;
  /**
   * Multiply operation
   * @see #commutative
   */
  protected static final int MUL =  2;
  /**
   * Divide operation
   * @see #commutative
   */
  protected static final int DIV =  3;
  /**
   * Bit and operation
   * @see #commutative
   */
  protected static final int AND =  4;
  /**
   * Bit or operation
   * @see #commutative
   */
  protected static final int OR  =  5;
  /**
   * Bit exclusive or operation
   * @see #commutative
   */
  protected static final int XOR =  6;
  /**
   * Shift right arithmetic operation
   * @see #commutative
   */
  protected static final int SRA =  7;
  /**
   * Shift right logical operation
   * @see #commutative
   */
  protected static final int SRL =  8;
  /**
   * Shift left logical operation
   * @see #commutative
   */
  protected static final int SLL =  9;
  /**
   * Modulo operation
   * @see #commutative
   */
  protected static final int MOD =  10;
  /**
   * Is the dyadic operation commutative?
   */
  protected static final boolean[] commutative = {
    true, false, true, false,
    true, true, true, false,
    false, false, false};
  /**
   * Map operation to string.
   */
  protected static final String[] operation = {
    "ADD", "SUB", "MUL", "DIV",
    "AND", "OR", "XOR", "SRA",
    "SRL", "SLL", "MOD"};
  /**
   * Map from lower three bits of an offset to the alignment.
   */
  protected static final int[] fieldAlignment = {8, 1, 2, 1, 4, 1, 2, 1};
  /**
   * True if tracing requested.
   */
  protected boolean trace;
  /**
   * True if target machine is little-endian.
   */
  protected boolean little;
  /**
   * True if programmer variables should be placed in memory.
   */
  protected boolean useMemory;
  /**
   * True if information for debuggers should be generated.
   */
  protected boolean genDebugInfo;
  /**
   * True if we assume indirect loads are not aligned.
   */
  protected boolean naln;
  /**
   * True if instructions should not be scheduled.
   */
  protected boolean nis;
  /**
   * True if the peephole optimizer should not be run.
   */
  protected boolean nph;
  /**
   * True if ANSI C source code specified.
   */
  protected boolean ansic;
  /**
   * True if source line number information should be placed in the
   * assembly code.
   */
  protected boolean lineNumbers;
  /**
   * True if the current routine uses <code>__builtin_alloca()</code>.
   */
  protected boolean usesAlloca;
  /**
   * True if the current routine uses <code>va_start()</code>.
   */
  protected boolean usesVaStart;
  /**
   * True if this routine calls another routine.
   */
  protected boolean callsRoutine;
  /**
   * The successor to the current CFG node.
   * It's <code>null</code> if the last CFG node generated a branch.
   */
  protected Chord successorCFGNode;
  /**
   * The declaration associated with this routine.
   */
  protected RoutineDecl currentRoutine;
  /**
   * The CFG associated with this routine.
   */
  protected Scribble scribble;
  /**
   * Last displacement generated for an address.
   */
  protected Displacement addrDisp;
  /**
   * Mark the start of the routine's code.
   */
  protected Marker currentBeginMarker;
  /**
   * The system wide set of scale.clef.expr.Literal instances.
   */
  private IntegerDisplacement[] disps; // Map from integer to displacement with that integer value.

  private int     loopNumber;         // Loop number of the current loop being processed.
  private int     nextDecl;           // Index of next free location in declInfo.
  private int     labelID;            // Label display value.
  private int     labelIndex;         // CFG Node label: Zero indicates no label
  private Label[] labels;             // Indexed by CFG Node label.

  private int currentStrength;
  private int maxBitFieldSize;        // Maximum size for a bit field.

  private BBIS    bbis;             // The instruction scheduler.
  private BitVect inRegister;       // True if the variable is in a register.                                    
  private BitVect adrInRegister;    // True if the address of the variable is in a register.                     
  private BitVect assignedRegister; // True if the virtual register is permanently assigned to a variable.

  private Stack<Object>       wlCFG;        // Work list for processing CFG nodes.                                       
  private IntMap<Declaration> regToAdrDecl; // Map from virtual register to Declaration whose address is in the register.
  private IntMap<Literal>     regToLiteral; // Map from virtual register to the Literal whose value is in the register.

  private boolean fortran; // True if the source language is Fortran.                                   

  /**
   * The call graph being processed.
   */
  protected CallGraph cg;
  /**
   * Last instruction generated.
   */
  protected Instruction lastInstruction;
  /**
   * Last label generated.
   */
  protected Label lastLabel;
  /**
   * The instruction just before the one and only return instruction.
   */
  protected Instruction returnInst;
  /**
   * The register set definition.
   */
  protected RegisterSet registers;
  /**
   * Array of data areas - indexed by handle.
   */
  protected SpaceAllocation[] dataAreas;
  /**
   * Next available in dataAreas.
   */
  protected int nextArea;
  /**
   * Map from routine to instructions.
   */
  protected HashMap<String, SpaceAllocation> codeMap;
  /**
   * Machine specific information.
   */
  protected Machine machine;
  /**
   * Generate unique names for constants in memory.
   */
  protected UniqueName un;
  /**
   * The register to use to access the stack frame for the function.
   */
  protected int stkPtrReg;
  /**
   * The data area to use to for constant data.
   */
  protected int readOnlyDataArea;


  /**
   * The type of the {@link #resultReg resultReg} value.
   * @see ResultMode
   */
  protected ResultMode resultRegMode;

  /**
   * Register containing the last generated expression result, usually
   * as a result of a call to visit().
   * <p>
   * It is very important that the register returned as a result of a
   * visit NEVER be used as the destination of any instruction; It may
   * be the register containing a variable.  And, if a source register
   * must be used in multiple instructions, the destination register
   * must not be used for intermediate values.
   */
  protected int resultReg;
  
  /**
   * If <code>resultRegMode</code> is <code>ResultMode.ADDRESS</code>,
   * this value is alignment of the address contained in the register
   * specified by <code>resultReg</code>.  The value 8 is used to
   * specify the normal alignment for variables and the value 1 is
   * used if the alignment is not known.  Typical values are 8 and 4.
   * <b>Note</b> - this alignment does not account for the value in
   * <code>resultRegAddressOffset</code>.
   */
  protected int resultRegAddressAlignment;

  /**
   * If <code>resultRegMode</code> is <code>ResultMode.ADDRESS</code>,
   * this value is offset from the address contained in the register
   * specified by <code>resultReg</code>.
   * @see #resultRegMode
   */
  protected long resultRegAddressOffset;

  /**
   * If <code>resultRegMode</code> is <code>ResultMode.STRUCT_VALUE</code>,
   * this value is size of the struct contained in the register
   * specified by <code>resultReg</code>.
   * @see #resultRegMode
   */
  protected long resultRegSize;

  /**
   * Register containing the current predicate. Set by generateProlog().  
   * If <code>predicateReg</code> is -1, there is no predicate set. 
   */
  protected int predicateReg;
   
  /**
   * Indicates if an expression predicated by <code>predicateReg</code> 
   * is predicated on true or false.
   */
  protected boolean predicatedOnTrue;

  /**
   * Probability of most recent IfThenElseChord taking the true branch.
   */
  protected double branchPrediction;
  
  /**
   * This class is used to convert the Scribble CFG to machine instructions.
   * @param cg is the call graph to be used
   * @param registers is the register set definition to be used
   * @param machine specifies the target architecture
   * @param features contains various flags that control the backend
   */
  public Generator(CallGraph   cg,
                   RegisterSet registers,
                   Machine     machine,
                   int         features)
  {
    this.cg           = cg;
    this.registers    = registers;
    this.machine      = machine;
    this.genDebugInfo = (features & DEBUG) != 0;
    this.naln         = (features & NALN) != 0;
    this.nis          = (features & NIS) != 0;
    this.nph          = (features & NPH) != 0;
    this.ansic        = (features & ANSIC) != 0;
    this.lineNumbers  = (features & LINENUM) != 0;
    
    this.labels       = new Label[100];
    this.labels[0]    = null;
    this.labelIndex   = 1;
    this.labelID      = 1;
    this.dataAreas    = new SpaceAllocation[100];
    this.codeMap      = new HashMap<String, SpaceAllocation>(11);
    this.nextDecl     = 0;
    this.trace        = false;
    this.little       = machine.littleEndian();
    this.maxBitFieldSize = machine.maxBitFieldSize();
    this.disps        = new IntegerDisplacement[512];
    this.predicateReg = -1;
    this.nextArea     = 1; // Don't want to use a zero index.
    this.fortran      = cg.getSourceLanguage().isFortran();
    this.returnInst   = null;

    this.resultRegAddressOffset = 0;
    this.resultRegMode          = ResultMode.NORMAL_VALUE;
    this.resultReg              = 0;

    this.inRegister       = new BitVect();
    this.adrInRegister    = new BitVect();
    this.assignedRegister = new BitVect();
    this.regToAdrDecl     = new IntMap<Declaration>(203);
    this.regToLiteral     = new IntMap<Literal>(203);

    boolean nois = nis || machine.hasCapability(Machine.HAS_PREDICATION);
    this.bbis = nois ? null : new BBIS(this);

    SymbolDisplacement.reset();
    Type.nextVisit();
  }

  /**
   * Return the machine definition in use.
   */
  public Machine getMachine()
  {
    return machine;
  }

  /**
   * Return the current routine being processed.
   */
  public RoutineDecl getCurrentRoutine()
  {
    return currentRoutine;
  }

  /**
   * Return the register set definition in use.
   */
  public RegisterSet getRegisterSet()
  {
    return registers;
  }
  
   /**
    * Return the register assignment of the stack pointer.
    */
   public int getStackPtr()
   {
     return stkPtrReg;
   }
   
  /**
   * Return true if the source language is Fortran.
   */
  public final boolean isFortran()
  {
    return fortran;
  }
  
  /**
   * Generate the machine instructions for each routine in the call
   * graph.
   */
  public void generate()
  {
    startModule();

    useMemory = false;

    if (genDebugInfo) {
      Iterator<Declaration> ed00 = cg.topLevelDecls();
      while (ed00.hasNext()) {
        Declaration decl = ed00.next();
        if (decl instanceof TypeDecl)
          processTypeDecl((TypeDecl) decl, false);
      }
      Iterator<Declaration> ed01 = cg.topLevelDecls();
      while (ed01.hasNext()) {
        Declaration decl = ed01.next();
        if (decl instanceof TypeName)
          processTypeName((TypeName) decl);
      }
      Iterator<Declaration> ed02 = cg.topLevelDecls();
      while (ed02.hasNext()) {
        Declaration decl = ed02.next();
        if (decl instanceof TypeDecl)
          processTypeDecl((TypeDecl) decl, true);
      }
    }

    // Do the first pass over the declarations referenced in this call
    // graph.

    Iterator<RoutineDecl> edr1 = cg.allRoutines();
    while (edr1.hasNext()) {
      RoutineDecl cn = edr1.next();
      processRoutineDecl(cn, true);
    }

    // Do base variables first.

    boolean               cv   = false;
    Iterator<Declaration> ed11 = cg.topLevelDecls();
    while (ed11.hasNext()) {
      Declaration decl = ed11.next();
      if (decl.isEquivalenceDecl()) {
        cv = true;
        continue;
      }

      VariableDecl vd = decl.returnVariableDecl();
      if (vd != null) {
        processVariableDecl(vd, true);
        continue;
      }

      RoutineDecl rd = decl.returnRoutineDecl();
      if (rd != null)
        processRoutineDecl(rd, true);
    }

    if (cv) {
      Iterator<Declaration>ed1 = cg.topLevelDecls();
      while (ed1.hasNext()) {
        Declaration decl = ed1.next();
        if (decl.isEquivalenceDecl())
          processVariableDecl((VariableDecl) decl, true);
      }
    }

    // Do routines.

    Iterator<RoutineDecl> en  = cg.allRoutines();
    while (en.hasNext()) {
      currentRoutine = en.next();
      scribble       = currentRoutine.getScribbleCFG();

      if (scribble == null)
        continue;

      if (currentRoutine.inlineSpecified() &&
          (currentRoutine.visibility() == Visibility.EXTERN))
        continue;

      usesAlloca             = currentRoutine.usesAlloca();
      usesVaStart            = currentRoutine.usesVaStart();
      callsRoutine           = false;
      returnInst             = null;
      addrDisp               = null;
      lastInstruction        = null;
      currentBeginMarker     = null;
      resultRegAddressOffset = 0;
      resultRegMode          = ResultMode.NO_VALUE;
      resultReg              = 0;

      String name = currentRoutine.getName();

      assert setTrace(name);

      if (trace)
        System.out.println("Assembling " + name);
      else
        Statistics.reportStatus(Msg.MSG_Assembling_s, name, 2);

      registers.initialize();
      assignedRegister.reset();

      int cntv = 0;
      int cntt = 0;
      int l    = scribble.numDecls();
      for (int i = 0; i < l; i++) {
        Declaration decl = scribble.getDecl(i);
        if (decl.isTemporary())
          cntt++;
        else if (decl.getStorageLoc() == Assigned.IN_REGISTER)
          cntv++;
      }
 
      int na = registers.numAllocatableRegisters();
      useMemory = (cntv > 2 * na) || (cntt > 10 * na);

      generateScribble();
    }

    endModule();
  }

  private boolean setTrace(String name)
  {
    trace = Debug.trace(name, classTrace, 3);
    return true;
  }

  /**
   * Called for every {@link scale.clef.decl.TypeDecl TypeDecl}
   * instance so that the target code generator can perform any needed
   * processing such as making Stabs entries.
   */
  protected void processTypeDecl(TypeDecl td, boolean complete)
  {
  }

  /**
   * Called for every {@link scale.clef.decl.TypeName TypeName}
   * instance so that the target code generator can perform any needed
   * processing such as making Stabs entries.
   */
  protected void processTypeName(TypeName td)
  {
  }

  /**
   * Called at the beginning of a call graph (module).
   */
  protected void startModule()
  {
  }

  /**
   * Called at the end of a call graph (module).
   */
  protected void endModule()
  {
  }

  /**
   * Label the basic blocks.
   */
  protected void labelCfgForBackend()
  {
    labelIndex = scribble.labelCfgForBackend(labelIndex);
  }

  /**
   * Find the last instruction.
   */
  protected void findLastInstruction(Instruction firstInstruction)
  {
    lastInstruction = firstInstruction;
    while(lastInstruction.getNext() != null)
      lastInstruction = lastInstruction.getNext();
  }

  /**
   * Generate the machine instructions for a CFG.
   */
  public void generateScribble()
  {
    BeginChord start = scribble.getBegin();

    labelCfgForBackend();

    Instruction firstInstruction = startRoutineCode();

    findLastInstruction(firstInstruction);

    layoutParameters();

    processDecls();

    generateProlog((ProcedureType) processType(currentRoutine));

    convertCFG(start); // Generate instructions.

    resetForBasicBlock();

    copyPropagate(firstInstruction);

    peepholeBeforeRegisterAllocation(firstInstruction);

    if (trace)
      System.out.println("\n\nEnd code generation");

    if (bbis != null) {
      firstInstruction = bbis.schedule(firstInstruction, true | trace);
      Instruction cur = lastInstruction;
      while (true) {
        Instruction nxt = cur.getNext();
        if (nxt == null)
          break;
        cur = nxt;
      }
      lastInstruction = cur;
    }

    adjustImmediates(firstInstruction);

    int[] map = allocateRegisters(firstInstruction, trace);

    if (trace)
      System.out.println("\n\nEnd register allocation");

    peepholeAfterRegisterAllocation(firstInstruction);

    endRoutineCode(map);

    if (trace)
      System.out.println("\n\nEnd routine");

    removeUnneededInstructions(firstInstruction);

    saveGeneratedCode(firstInstruction);
  }

  /**
   * Adjust large immediate values.  For example, some instructions
   * have an immediate field but the instruction limits it to a small
   * size such as 16 bits. As a result, the instruction cannot
   * represent large immediate values. These large values have to be
   * split into two instructions, one for the MSBs and the other for
   * the LSBs.
   */
  public void adjustImmediates(Instruction firstInstruction)
  {
  }

  /**
   * Save the generated code in a TEXT section using the name of the
   * current routine.
   */
  protected void saveGeneratedCode(Instruction first)
  {
    String             name   = currentRoutine.getName();
    SymbolDisplacement disp   = (SymbolDisplacement) currentRoutine.getDisplacement();
    int                handle = disp.getHandle();
    SpaceAllocation    sa     = dataAreas[handle];

    sa.setValue(first);
    codeMap.put(name, sa);
  }

  /**
   * Process the declarations of this CFG.
   */
  protected void processDecls()
  {
    int     l  = scribble.numDecls();
    boolean cv = false;

    // Do base variables first.

    for (int i = 0; i < l; i++) {
      Declaration decl = scribble.getDecl(i);
      if (decl.isEquivalenceDecl()) {
        cv = true;
        continue;
      }

      VariableDecl vd = decl.returnVariableDecl();
      if (vd != null) {
        processVariableDecl(vd, false);
        continue;
      }

      RoutineDecl rd = decl.returnRoutineDecl();
      if (rd != null)
        processRoutineDecl(rd, false);
    }

    if (!cv)
      return;

    for (int i = 0; i < l; i++) {
      Declaration decl = scribble.getDecl(i);
      if (decl.isEquivalenceDecl())
        processVariableDecl((VariableDecl) decl, false);
    }
  }

  /**
   * Associate information with a variable Declaration kept in a register.
   * @param decl is the variable
   * @param register is the register allocated for the variable
   * @param regha is mode of the register
   */
  protected void defineDeclInRegister(Declaration decl, int register, ResultMode regha)
  {
    if (trace) {
      System.out.print("Var: ");
      System.out.print(decl.getName());
      System.out.print(" ");
      System.out.print(regha);
      System.out.print(" ");
      System.out.print(registers.display(register));
      System.out.print(" ");
      System.out.println(decl);
    }

    assignedRegister.set(register);

    decl.setStorageLoc(Assigned.IN_REGISTER);
    decl.setValueRegister(register, regha);
    decl.setTag(nextDecl);
    nextDecl++;
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
    if (trace) {
      System.out.print("Var: ");
      System.out.print(decl.getName());
      System.out.print(" stack ");
      System.out.print(disp);
      System.out.print(" ");
      System.out.println(decl);
    }

    decl.setStorageLoc(Assigned.ON_STACK);
    decl.setDisplacement(disp);
    decl.setTag(nextDecl);
    nextDecl++;
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
    if (trace) {
      System.out.print("Var: ");
      System.out.print(decl.getName());
      System.out.print(" memory ");
      System.out.print(disp);
      System.out.print(" ");
      System.out.println(decl);
    }

    decl.setStorageLoc(Assigned.IN_MEMORY);
    decl.setDisplacement(disp);
    decl.setTag(nextDecl);
    nextDecl++;
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
    if (trace) {
      System.out.print("Var: ");
      System.out.print(decl.getName());
      System.out.print(" common ");
      System.out.print(disp);
      System.out.print(" ");
      System.out.println(decl);
    }

    decl.setStorageLoc(Assigned.IN_COMMON);
    decl.setDisplacement(disp);
    decl.setTag(nextDecl);
    nextDecl++;
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
    if (trace) {  
      System.out.print("Routine: ");  
      System.out.print(rd.getName());  
      System.out.print(" ");  
      System.out.println(disp);  
      System.out.print(" ");  
      System.out.println(rd);  
    }  
   
    rd.setDisplacement(disp);
    rd.setTag(nextDecl);
    nextDecl++;
  }

  /**
   * Determine the offsets for fields of a struct or union.
   * This method assumes 8-bit addressable memory units.
   */
  protected void calcFieldOffsets(AggregateType at)
  {
    long    fieldOffset = 0;
    int     bitOffset   = 0;
    int     l           = at.numFields();
    boolean utf         = at.isUnionType();
    int     binc        = ((maxBitFieldSize + 7) / 8);
    int     mask        = binc - 1;

    for (int i = 0; i < l; i++) {
      FieldDecl fid  = at.getField(i);
      Type      fit  = processType(fid);
      int       bits = fid.getBits();

      if (utf) {
        fid.setFieldTargetAttributes(0, fieldAlignment[0], 0);
        continue;
      }

      long ts = fit.memorySize(machine);
      int  fa = fit.alignment(machine);
      if ((bits != 0) && (bits % 8) == 0) { // Not really a bit field, place on best boundary.
        ts = bits / 8;
        int x = Lattice.powerOf2(ts);
        if (x >= 0) {
          fa = 1 << x;
          bits = 0;
        } else if ((bitOffset & 0x7) != 0) {
          bitOffset = (bitOffset | 0x7) + 1;
        }
      }

      if (bits == 0) { // Not a bit field
        fieldOffset += machine.addressableMemoryUnits(bitOffset);
        fieldOffset = Machine.alignTo(fieldOffset, fa);
        bitOffset = 0;
        long fo = fieldOffset;
        if ((fid.getFieldOffset() > 0) && ((fid.getFieldOffset() % fa) != 0))
          fa = 1;
        fid.setFieldTargetAttributes(fo, fa, 0);
        fieldOffset = fid.getFieldOffset() + ts;
        continue;
      }

      // Bit field
      // For a bit-field, the field offset is the offset of the word
      // containing the field.

      if (bits <= maxBitFieldSize) {
        if ((fieldOffset & mask) != 0) {
          bitOffset += (fieldOffset & mask) * 8;
          fieldOffset &= ~mask;
        }
        if ((bitOffset + bits) > maxBitFieldSize) { // Don't split bit field over the word boundary.
          fieldOffset += binc;
          bitOffset = 0;
        }

        long fo = fieldOffset;
        int  bo = bitOffset;
        fa = fieldAlignment[(int) (fo & 0x3)];
        bitOffset += bits;

        fid.setFieldTargetAttributes(fo, fa, bo);
        continue;
      }

      assert (bits <= 64) : "Large bit field " + bits;

      if ((fieldOffset & 0x7) != 0) {
        bitOffset += (fieldOffset & 0x7) * 8;
        fieldOffset &= ~0x7;
      }
      if ((bitOffset + bits) > 64) { // Don't split bit field over the word boundary.
        fieldOffset += 8;
        bitOffset = 0;
      }

      long fo = fieldOffset;
      int  bo = bitOffset;
      fa = fieldAlignment[(int) (fo & 0x7)];
      bitOffset += bits;

      while (bitOffset > 32) {
        bitOffset -= 32;
        fieldOffset += 4;
      }

      fid.setFieldTargetAttributes(fo, fa, bo);
    }
  }

  /**
   * Generate a mapping from virtual register to real register
   * and then modify the instructions to use real registers.
   */
  protected int[] allocateRegisters(Instruction first, boolean trace)
  {
    RegisterAllocator registerAllocator = new QDRA(this, trace);

    int[] map = registerAllocator.allocate(first);

    // Change the registers in every instruction.
    // map is the mapping from old register number to new register number.
    // first is the first instruction in the sequence.

    Instruction inst = first;
    while (inst != null) {
      inst.remapRegisters(map);
      inst = inst.getNext();
    }

    return map;
  }

  /**
   * Make sure the specified Chord is converted next.  If the Chord
   * has already been processed, the method returns the value false.
   * @return true if the specified Chord will be processed next
   */
  protected boolean doNext(Chord nxt)
  {
    if (nxt.visited()) {
      for (int i = wlCFG.size() - 1; i >= 0; i--) {
        if (wlCFG.elementAt(i) == nxt) {
          wlCFG.setElementAt(null, i);
          wlCFG.push(nxt);
          return true;
        }
      }
      return false;
    }

    wlCFG.push(nxt);
    nxt.setVisited();
    return true;
    
  }
  
  /**
   * Copy-propagate register numbers from move instructions.
   */
  protected void copyPropagate(Instruction first)
  {
    Instruction inst = first;

    while (inst != null) {
      Instruction next = inst.getNext();

      if (inst.isCopy()) {
        int src  = inst.getCopySrc();
        int dest = inst.getCopyDest();
        if (registers.virtualRegister(dest)) // Real registers may have implicit uses.
          propagate(next, src, dest);
      }

      inst = next;
    }
  }

  /**
   * Perform copy propagation for the specified registers
   * within the basic block that begins with the specified instruction.
   */
  protected void propagate(Instruction inst, int newReg, int oldReg)
  {
    while (inst != null) {
      if (inst.isLabel()) // Only for this basic block.
        return;

      inst.remapSrcRegister(oldReg, newReg);

      if (inst.defs(oldReg, registers))
        return;
      if (inst.mods(oldReg, registers))
        return;
      if (inst.defs(newReg, registers))
        return;
      if (inst.mods(newReg, registers))
        return;

      inst = inst.getNext();
    }
  }

  /**
   * This method is called at the end of each basic block
   * and at the start before any CFG nodes are processed.
   */
  protected void resetForBasicBlock()
  {
    inRegister.reset();
    adrInRegister.reset();
  }

  /**
   * This class is used to keep track of various values while spanning
   * the CFG.
   */
  private static class Strength
  {
    public int strength;   // Register allocation strength.
    public int loopNumber; // Current loop number.

    public Strength()
    {
    }

    public String toString()
    {
      return "(" + loopNumber + "," + strength + ")";
    }
  }

  /**
   * Generate the instructions for the CFG node.
   */
  protected void convertCFG(Chord start)
  {
    int lastSla = -1;
    
    wlCFG = WorkArea.<Object>getStack("convertCFG");
      
    loopNumber = 0;

    currentStrength = 1;
    resetForBasicBlock();
    regToAdrDecl.clear();
    regToLiteral.clear();

    Chord.nextVisit();
    wlCFG.push(start);
    start.setVisited();

    Strength strength = null;
    Chord    last     = null;

    Instruction first = lastInstruction;

    successorCFGNode = null;

    while (!wlCFG.empty()) {
      Object o = wlCFG.pop();

      if (o instanceof Strength) {
        // Reset the strength, etc for the nodes remaining on the list.
        strength = (Strength) o;
        currentStrength = strength.strength;
        loopNumber = strength.loopNumber;
        continue;
      }

      Chord s = (Chord) o;
      if (s == null)
        continue;

      // Special nodes requuire special handling. They do not need to
      // be visited because they don't generate instructions.  But, we
      // must be careful to generate any needed labels.

      if (s.isSpecial()) {
        if (s.isLoopHeader()) {
          LoopHeaderChord lh  = (LoopHeaderChord) s;
          loopNumber = lh.getLoopNumber();
        } else {
          if (s.isLoopPreHeader()) {
            if (strength == null)
              strength = new Strength();
            strength.strength = currentStrength;
            strength.loopNumber = loopNumber;
            wlCFG.push(strength);
            strength = null;
            currentStrength <<= 2;
          } else if (s.isLoopExit()) {
            if (strength == null)
              strength = new Strength();
            strength.strength = currentStrength;
            strength.loopNumber = loopNumber;
            wlCFG.push(strength);
            strength = null;

            currentStrength >>= 2;
            if (currentStrength < 1)
              currentStrength = 1;

            loopNumber = s.getLoopHeader().getParent().getLoopNumber();
          } else if (s.isLoopTail()) {
            resetForBasicBlock();
            basicBlockEnd();
            continue;
          }

          Chord nxt = s.getNextChord();
          if ((nxt.getLabel() > 0) && (s.getLabel() == 0)) {
            if (!nxt.visited()) {
              wlCFG.push(nxt);
              nxt.setVisited();
            }
            if (s.isLastInBasicBlock()) {
              resetForBasicBlock();
              basicBlockEnd();
            }
            continue;
          }
        }
      }

      // We need to generate an unconditional branch if we are not
      // going to process the follow-up node from the last one
      // processed.  This usually results because the successor has
      // already been processed.

      if ((successorCFGNode != null) &&
          unconditionalBranchNeeded(last, successorCFGNode, s))
        generateUnconditionalBranch(getBranchLabel(successorCFGNode));

      successorCFGNode = null;

      // Generate any needed labels.

      int   label = s.getLabel();
      Label lab   = null;
      if (label > 0)
        lab = getBranchLabel(s);

      int sla = lineNumbers ? s.getSourceLineNumber() : -1;

      if (lab != null) {
        appendLabel(lab);
        processSourceLine(sla, lab, false);
        lab.setStrength(currentStrength);
      } else if (sla != lastSla) {
        processSourceLine(sla, lastLabel, true);
        lastSla = sla;
      }

      successorCFGNode = s.getNextChord();

      // Queue up this node's successors.

      Chord nxt = s.getNextChord();
      if (nxt != null) {
        if (!nxt.visited()) {
          wlCFG.push(nxt);
          nxt.setVisited();
        }
      } else {
        int n = s.numOutCfgEdges();
        for (int i = 0; i < n; i++) {
          Chord nc = s.getOutCfgEdge(i);
          if (!nc.visited()) {
            wlCFG.push(nc);
            nc.setVisited();
          }
        }
      }
      
      if (trace)
        System.out.println(s);

      // Generate instructions for this node.

      s.visit(this);

      last = s;

      // Note - the visit may change the value of successorCFGNode.

      if (successorCFGNode != null) // Skip un-needed nodes.
        successorCFGNode = getBranchTarget(successorCFGNode);

      if (s.isLastInBasicBlock()) {
        resetForBasicBlock();
        basicBlockEnd();
      }
    }

    if ((successorCFGNode != null) &&
        unconditionalBranchNeeded(last, successorCFGNode, null)) {
      generateUnconditionalBranch(getBranchLabel(successorCFGNode));
      successorCFGNode = null;
    }

    if (false) {
      System.out.println("\n***xx");
      for (Instruction f = first; f != null; f = f.getNext())
        if (f.isLabel())
          System.out.println("   " + f);
        else 
          System.out.println("     " + f);
    }

    WorkArea.<Object>returnStack(wlCFG);
    wlCFG = null;
  }

  /**
   * Return true if an unconditional branch is needed.
   * @param c is the current node
   * @param nxt is the successor to the current node
   * @param actual is the actual node that will be converted to
   * assembly next
   */
  protected boolean unconditionalBranchNeeded(Chord c, Chord nxt, Object actual)
  {
    if (actual != nxt)
      return true;

    if ((nxt.getLabel() > 0) && (nxt.numInCfgEdges() == 1))
      getBranchLabel(nxt).setNotReferenced();

    return false;
  }

  /**
   * Called after the last CFG node in a basic block is processed.
   */
  protected void basicBlockEnd()
  {
  }

  /**
   * Skip nodes that don't result in generating instructions.  Special
   * CFG nodes do not generate code.  But, we must always generate the
   * label for the loop header.
   */
  protected final Chord getBranchTarget(Chord s)
  {
    if (!s.isSpecial() || s.isLoopHeader())
      return s;

    if (s.isLoopTail())
      return s.getNextChord();

    Chord nxt = s.getNextChord();
    if (nxt.getLabel() > 0) {
      return getBranchTarget(nxt);
    }

    return s;
  }

  /**
   * Remove instructions that do not change the machine state.
   */
  public boolean removeUnneededInstructions(Instruction first)
  {
    boolean removed = false;
    
    Instruction inst = first;
    Instruction last = null;

    while (inst != null) {
      Instruction next = inst.getNext();
 
      if (inst.canBeDeleted(registers)) {      	
        removed = true;
        if (trace)
          System.out.println("    DELETED " + inst);
        if (last != null)
          last.setNext(next);
      } else
        last = inst;

      inst = next;
    }
    
    return removed;
  }

  /**
   * Do peephole optimizations before registers are allocated.
   */
  protected void peepholeBeforeRegisterAllocation(Instruction first)
  {
  }

  /**
   * Do peephole optimizations after registers are allocated.
   */
  protected void peepholeAfterRegisterAllocation(Instruction first)
  {
  }

  /**
   * The VariableDecl is assigned a tag.  The tag can be used to
   * retrieve information about the declaration.  For a VariableDecl,
   * the information is a Displacement, the residency of the variable
   * (register, stack, or memory), its size in bytes, and whether it
   * is known to be aligned.
   * <p>
   * For register-assigned variable, the virtual register is specified.
   * For stack-assigned variables, the stack offset is assigned.
   * (The stack offset will be adjusted later.)
   * For memory-assigned variables, a data area is created.
   * @param vd is the declaration
   * @param topLevel is true if this declaration is defined outside of
   * a routine
   */
  @SuppressWarnings("fallthrough")
  protected void processVariableDecl(VariableDecl vd, boolean topLevel)
  {
    Assigned loc = vd.getStorageLoc();
    switch (loc) {
    case IN_COMMON:
      EquivalenceDecl ed    = (EquivalenceDecl) vd;
      VariableDecl    base  = ed.getBaseVariable();
      Displacement    cdisp = base.getDisplacement().unique();
      defineDeclInCommon(ed, cdisp);
      return;

    case IN_MEMORY:
      String     name       = vd.getName();
      Visibility visibility = vd.visibility();

      if (!topLevel && (visibility == Visibility.LOCAL)) { // Avoid name conflicts.
        name = name + un.genName();
        vd.setName(name);
      }

      assignDeclToMemory(name, vd);
      return;

    case IN_REGISTER:
      if ((!genDebugInfo && !useMemory) || vd.isTemporary()) {
        assignDeclToRegister(vd);
        return;
      }

      // Fall through.

    case ON_STACK:
      if (vd.isEquivalenceDecl()) {
        EquivalenceDecl eds   = (EquivalenceDecl) vd;
        VariableDecl    basev = eds.getBaseVariable();
        long            off   = eds.getBaseOffset();
        Displacement    odisp = basev.getDisplacement().offset(off);
        defineDeclOnStack(vd, odisp);
        return;
      }

      assignDeclToStack(vd);
      return;
    }

    throw new scale.common.InternalError("Variable allocation " + loc);
  }

  /**
   * Assign the specified variable to a location in memory using the
   * specified name.  The decision to place this variable in memory
   * has already been made.
   * @see scale.clef.decl.VariableDecl#getStorageLoc
   */
  protected abstract void assignDeclToMemory(String name, VariableDecl vd);
  /**
   * Assign the specified variable to a register.  The decision to
   * place this variable in a register has already been made.
   * @see scale.clef.decl.VariableDecl#getStorageLoc
   */
  protected abstract void assignDeclToRegister(VariableDecl vd);
  /**
   * Assign the specified variable to a location in the stack frame.
   * The decision to place this variable on the stack has already been
   * made.
   * @see scale.clef.decl.VariableDecl#getStorageLoc
   */
  protected abstract void assignDeclToStack(VariableDecl vd);
  /**
   * The RoutineDecl is assigned a tag.  The tag can be used to
   * retrieve information about the declaration.  For a RoutineDecl,
   * the information is a Displacement, the data area for the routine
   * and the label to be used for BSR calls to the routine.
   * @param rd is the declaration
   * @param topLevel is true if this declaration is defined outside of a routine
   */
  protected abstract void processRoutineDecl(RoutineDecl rd, boolean topLevel);
  /**
   * Obtain the information needed for register spill loads and stores.
   * The Object returned will probably specify a memory location.
   * It will be passed to getSpillLoad() and getSpillStore().
   * @param reg specifies which virtual register will be spilled
   */
  public abstract Object getSpillLocation(int reg);
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
  public abstract Instruction insertSpillLoad(int         reg,
                                              Object      spillLocation,
                                              Instruction after);
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
  public abstract Instruction insertSpillStore(int         reg,
                                               Object      spillLocation,
                                               Instruction after);

  /**
   * Return the register used to return the function value.
   * @param regType specifies the type of value
   * @param isCall is true if the calling routine is asking
   */
  public abstract int returnRegister(int regType, boolean isCall);
  /**
   * Return the register used as the first argument in a function call.
   * @param regType specifies the type of argument value
   */
  public abstract int getFirstArgRegister(int regType);

  /**
   * Return true if the value in the register can be easily
   * regenerated.  The value can be easily regenerated if it is just
   * the load of a variable or constant value that is kept in memory.
   * We know that the value in the register is the same as the value
   * in memory at any point in the program because new values of
   * variables, that are stored in memory, are always stored into
   * memory.  We also know that this mapping is never used for
   * variables kept in registers.
   */
  protected boolean shouldBeRegenerated(int reg)
  {
    if (regToAdrDecl.get(reg) != null)
      return true;
    return regToLiteral.get(reg) != null;
  }

  /**
   * Regenerate a register value instead of performing a spill load.
   */
  protected Instruction regenerateRegister(int reg, Instruction after)
  {
    Instruction next = after.getNext();
    Instruction sl   = lastInstruction;

//     Declaration decl = (Declaration) regToDecl.get(reg);
//     if (decl != null) {
//       Type vt    = decl.getCoreType();
//       int  vr;
//       lastInstruction = after;
//       after.setNext(null);
//       int loc = decl.getStorageLoc();
//       switch (loc) {
//       case Assigned.IN_MEMORY:
//         registers.setResultRegister(reg);
//         loadVariableFromMemory(decl.getDisplacement(), vt);
//         break;
//       case Assigned.IN_COMMON:
//         EquivalenceDecl ed     = (EquivalenceDecl) decl;
//         Declaration     bd     = ed.getBaseVariable();
//         long            offset = ed.getBaseOffset();

//         putAddressInRegisterNO(bd, false);

//         offset += resultRegAddressOffset;
//         registers.setResultRegister(reg);
//         loadVariableFromCommon(resultReg, vt, offset);
//         break;

//       case Assigned.ON_STACK:
//         registers.setResultRegister(reg);
//         loadVariableFromStack(decl.getDisplacement(), vt);
//         break;
//       default:
//         throw new scale.common.InternalError("load value from where? " + loc);
//       }

//       regenerateValueCount++;
//       genRegToReg(resultReg, reg);
//       registers.setResultRegister(-1);

//       Instruction load = lastInstruction;
//       load.setNext(next);
//       lastInstruction = sl;

//       return load;
//     }

    Declaration decl = regToAdrDecl.get(reg);
    if (decl != null) {
      lastInstruction = after;
      after.setNext(null);

      int      adrReg;
      Assigned loc = decl.getStorageLoc();
      switch (loc) {
      case ON_STACK:
        addrDisp = decl.getDisplacement().unique();
        registers.setResultRegister(reg);
        adrReg = loadStackAddress(addrDisp);
        genRegToReg(adrReg, reg);
        registers.setResultRegister(-1);
        break;
      case IN_COMMON:
        EquivalenceDecl ed = (EquivalenceDecl) decl;
        putAddressInRegisterNO(ed.getBaseVariable(), false);
        genLoadImmediate(ed.getBaseOffset() + resultRegAddressOffset, resultReg, reg);
        break;
      case IN_MEMORY:
        addrDisp = decl.getDisplacement().unique();
        registers.setResultRegister(reg);
        adrReg = loadMemoryAddress(addrDisp);
        genRegToReg(adrReg, reg);
        registers.setResultRegister(-1);
        break;
      default:
        throw new scale.common.InternalError("Invalid declaration info (" +
                                             loc +
                                             ") for " +
                                             decl);
      }

      regenerateAddressCount++;

      Instruction load = lastInstruction;
      load.setNext(next);
      lastInstruction = sl;

      return load;
    }

    Literal lit = regToLiteral.get(reg);
    assert (lit != null) : "Can not be regenerated: " + reg;

    lastInstruction = after;
    after.setNext(null);

    registers.setResultRegister(reg);
    generateLiteralValue(lit, false);
    genRegToReg(resultReg, reg);
    registers.setResultRegister(-1);

    regenerateLiteralCount++;

    Instruction load = lastInstruction;
    load.setNext(next);
    lastInstruction = sl;

    return load;
  }

  /**
   * Generate a predicated branch to a single location. This is only relevant
   * when hyperblocks are enabled.
   * @param which specifies the branch test (EQ, NE, LT, ...)
   * @param treg specifies the condition to test
   * @param l is the label to branch to.
   */
  public void generateConditionalBranch(int which, int treg, Label l)
  {
    assert(false);
  }
  
  /**
   * Generate assembly language.
   * @param emit is the stream to use.
   * @param source is the source file name
   * @param comments is a list of Strings containing comments
   */
  public abstract void assemble(Emit emit, String source, Enumeration<String> comments);
  /**
   * Generate an unconditional branch to the label specified.
   */
  protected abstract void generateUnconditionalBranch(Label lab);
  /**
   * The user has requested source line information be included.
   * @param line is the current line number
   * @param label is the last label encountered or <code>null</code>
   * @param newLine is true if a new source line with no label
   */
  protected abstract void processSourceLine(int line, Label label, boolean newLine);
  /**
   * Called at the start of code generation for a routine.
   */
  protected abstract Instruction startRoutineCode();
  /**
   * Called at the end of code generation for a routine.  This method
   * is responsible for inserting any prolog or epilog instructions
   * required.  It may also adjust stack offsets.
   */
  protected abstract void endRoutineCode(int[] regMap);
  /**
   * Determine the layout of routine parameters for the call.  This
   * method determines where the arguments to a function will be kept.
   * Usually arguments are kept in registers.  However, if the address
   * of the argument is taken, the argument will be placed on the
   * stack.  Also, if the argument is passed on the stack, it may be
   * left on the stack.  It's a mistake to assign an argument to a
   * real register because real registers can't be spilled.
   * <p>
   * The {@link scale.backend.Generator#generateProlog
   * generateProlog} method generates instructions to move the
   * arguments to the location determined by this method.
   */
  protected abstract void layoutParameters();
  /**
   * This method is responsible for generating instructions to move
   * function arguments to the position assigned by the {@link
   * scale.backend.Generator#layoutParameters layoutParameters} method.
   */
  protected abstract void generateProlog(ProcedureType pt);
  /**
   * Store a value into a field of a structure.
   * @param lhs specifies the field of the structure
   * @param rhs specifies the value
   */
  protected abstract void storeLfae(LoadFieldAddressExpr lhs, Expr rhs);
  /**
   * Load the value of a variable on the stack into a register.
   * Return the register into which the variable value is loaded in
   * <code>resultReg</code>.
   * @param vdisp is the variable displacement
   * @param vt specifies the type of the value
   */
  protected void loadVariableFromStack(Displacement vdisp, Type vt)
  {
    Displacement disp = vdisp.unique();

    if (!vt.getCoreType().canBeInRegister()) {
      resultReg = loadStackAddress(disp);;
      resultRegMode = ResultMode.ADDRESS;
      resultRegAddressOffset = 0;
      resultRegAddressAlignment = 0;
      return;
    }

    int vr    = registers.getResultRegister(vt.getTag());
    int dsize = vt.memorySizeAsInt(machine);

    loadFromMemoryWithOffset(vr,
                             stkPtrReg,
                             disp,
                             dsize,
                             machine.stackAlignment(vt),
                             vt.isSigned(),
                             vt.isRealType());
        
    resultReg = vr;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultRegAddressOffset = 0;
    resultRegAddressAlignment = 1;
    if (!naln && vt.isPointerType())
      resultRegAddressAlignment = vt.getPointedTo().alignment(machine);
  }

  /**
   * Load the value of a variable in COMMON into a register.  Return
   * the register into which the variable value is loaded in
   * <code>resultReg</code>.
   * @param adr specifies the base address
   * @param vt specifies the type of the value
   * @param offset is the offset of the variable from the base of common
   */
  protected void loadVariableFromCommon(int adr, Type vt, long offset)
  {
    int vr    = registers.getResultRegister(vt.getTag());
    int dsize = processType(vt).memorySizeAsInt(machine);

    loadFromMemoryWithOffset(vr, adr, offset, dsize, offset, vt.isSigned(), vt.isRealType());

    resultReg = vr;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultRegAddressOffset = 0;
    resultRegAddressAlignment = 1;
    if (!naln && vt.isPointerType())
      resultRegAddressAlignment = vt.getPointedTo().alignment(machine);
  }

  /**
   * Load the value of a variable in memory into a register.  Return
   * the register into which the variable value is loaded in
   * <code>resultReg</code>.
   * @param vdisp is the variable displacement
   * @param vt specifies the type of the value
   */
  protected void loadVariableFromMemory(Displacement vdisp, Type vt)
  {
    Displacement disp = vdisp.unique();

    if (!vt.getCoreType().canBeInRegister()) {
      resultReg = loadMemoryAddress(disp);
      resultRegMode = ResultMode.ADDRESS;
      resultRegAddressOffset = 0;
      resultRegAddressAlignment = 0;
      return;
    }

    int vr    = registers.getResultRegister(vt.getTag());
    int dsize = processType(vt).memorySizeAsInt(machine);

    loadRegFromSymbolicLocation(vr, dsize, vt.isSigned(), vt.isRealType(), disp);

    resultReg = vr;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultRegAddressOffset = 0;
    resultRegAddressAlignment = 1;
    if (!naln && vt.isPointerType())
      resultRegAddressAlignment = vt.getPointedTo().alignment(machine);
  }

  /**
   * Load a register from a symbolic location in memory.
   * @param dest is the register
   * @param dsize is the size of the value in addressable memory units
   * @param isSigned is true if the value in the register is signed
   * @param isReal is true if the value in the register is a floating point value
   * @param disp specifies the location
   */
  protected abstract void loadRegFromSymbolicLocation(int          dest,
                                                      int          dsize,
                                                      boolean      isSigned, 
                                                      boolean      isReal,
                                                      Displacement disp);
  /**
   * Generate instructions to load data from memory at the address in
   * a register plus an offset.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be a real
   */
  protected abstract void loadFromMemoryWithOffset(int     dest,
                                                   int     address,
                                                   long    offset,
                                                   int     size,
                                                   long    alignment,
                                                   boolean signed,
                                                   boolean real);
  /**
   * Generate instructions to load data from memory at the address in
   * a register plus an offset.  The offset must not be symbolic.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be a real
   */
  protected abstract void loadFromMemoryWithOffset(int          dest,
                                                   int          address,
                                                   Displacement offset,
                                                   int          size,
                                                   long         alignment,
                                                   boolean      signed,
                                                   boolean      real);
  /**
   * Generate instructions to load data from memory at the address
   * that is the sum of the two index register values.
   * @param dest is the destination register
   * @param index1 is the register containing the first index
   * @param index2 is the register containing the second index
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be a real
   */
  protected abstract void loadFromMemoryDoubleIndexing(int     dest,
                                                       int     index1,
                                                       int     index2,
                                                       int     size,
                                                       long    alignment,
                                                       boolean signed,
                                                       boolean real);
  /**
   * Load an array element into a register.
   * @param aie specifies the array element
   * @param dest specifies the register
   */
  protected abstract void loadArrayElement(ArrayIndexExpr aie, int dest);
  /**
   * Generate instructions to store data into memory at the address in
   * a register plus an offset.
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real
   */
  protected abstract void storeIntoMemoryWithOffset(int     src,
                                                    int     address,
                                                    long    offset,
                                                    int     size,
                                                    long    alignment,
                                                    boolean real);
  /**
   * Generate instructions to store data into memory at the address in
   * a register plus an offset.  The offset must not be symbolic.
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset from the address
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real
   */
  protected abstract void storeIntoMemoryWithOffset(int          src,
                                                    int          address,
                                                    Displacement offset,
                                                    int          size,
                                                    long         alignment,
                                                    boolean      real);
  /**
   * Store a value in a register to a symbolic location in memory.
   * @param src is the value
   * @param dsize is the size of the value in addressable memory units
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param isReal is true if the value in the register is a floating
   * point value
   * @param disp specifies the location
   */
  protected abstract void storeRegToSymbolicLocation(int          src,
                                                     int          dsize,
                                                     long         alignment,
                                                     boolean      isReal,
                                                     Displacement disp);
  /**
   * Generate instructions to store data into memory at the address
   * specified by a register.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real
   */
  protected abstract void storeIntoMemory(int     src,
                                          int     address,
                                          int     size,
                                          long    alignment,
                                          boolean real);

  /**
   * Load the address of a declaration into a register.  The register
   * is specified in <code>resultReg</code>.  The
   * <code>resultRegAddressOffset</code> value is always 0.  The
   * <code>resultRegMode</code> value is always
   * <code>ResultMode.NORMAL_VALUE</code>.
   * @param decl specifies the declaration
   * @param isPredicated is true if this sequence of instructions will
   * be predicated
   */
  protected void putAddressInRegister(Declaration decl, boolean isPredicated)
  {
    int adrReg = decl.getAddressRegister();
    int tag    = decl.getTag();
    if (adrInRegister.get(tag)) {
      avoidedAdrCount++;
      resultReg = adrReg;
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    }

    Assigned loc = decl.getStorageLoc();
    switch (loc) {
    case IN_REGISTER:
      adrReg = decl.getValueRegister();
      assert (decl.valueRegMode() == ResultMode.ADDRESS) :
        "Address of register - " + decl;
      break;
    case ON_STACK:
      addrDisp = decl.getDisplacement();
      assert (addrDisp != null) : "Not processed " + decl;
      adrReg = loadStackAddress(addrDisp);
      break;
    case IN_COMMON:
      adrReg = registers.getResultRegister(RegisterSet.ADRREG); // Must be before the putAddressInRegister() call!
      EquivalenceDecl ed   = (EquivalenceDecl) decl;
      putAddressInRegister(ed.getBaseVariable(), isPredicated);
      genLoadImmediate(ed.getBaseOffset(), resultReg, adrReg);
      break;
    case IN_MEMORY:
      addrDisp = decl.getDisplacement().unique();
      adrReg = loadMemoryAddress(addrDisp);
      break;
    default:
      throw new scale.common.InternalError("Invalid declaration info (" + 
                                           loc +
                                           ") for " +
                                           decl);
    }

    if (!isPredicated)
      specifyAdrReg(decl, adrReg);

    resultReg = adrReg;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultRegAddressOffset = 0;
  }

  /**
   * Load the address of a declaration into a register and place any
   * offset into <code>resultRegAddressOffset</code>.  The register is
   * specified in <code>resultReg</code>.  The
   * <code>resultRegMode</code> value is always
   * <code>ResultMode.NORMAL_VALUE</code>.
   * @param decl specifies the declaration
   * @param isPredicated is true if this sequence of instructions will
   * be predicated
   */
  protected void putAddressInRegisterNO(Declaration decl, boolean isPredicated)
  {
    int adrReg = decl.getAddressRegister();
    int tag    = decl.getTag();
    if (adrInRegister.get(tag)) {
      avoidedAdrCount++;
      resultReg = adrReg;
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    }

    Assigned loc = decl.getStorageLoc();
    switch (loc) {
    case IN_REGISTER:
      adrReg = decl.getValueRegister();
      assert (decl.valueRegMode() == ResultMode.ADDRESS) : "Address of register - " + decl;
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      break;
    case ON_STACK:
      addrDisp = decl.getDisplacement();
      assert (addrDisp != null) : "Not processed " + decl;
      adrReg = loadStackAddress(addrDisp);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      break;
    case IN_COMMON:
      adrReg = registers.getResultRegister(RegisterSet.ADRREG); // Must be before the putAddressInRegister() call!
      EquivalenceDecl ed   = (EquivalenceDecl) decl;
      putAddressInRegisterNO(ed.getBaseVariable(), isPredicated);
      resultRegAddressOffset += ed.getBaseOffset();
      resultRegMode = ResultMode.ADDRESS;
      adrReg = resultReg;
      break;
    case IN_MEMORY:
      addrDisp = decl.getDisplacement().unique();
      adrReg = loadMemoryAddress(addrDisp);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      break;
    default:
      throw new scale.common.InternalError("Invalid declaration info (" +
                                           loc +
                                           ") for " +
                                           decl);
    }

    if (!isPredicated && (resultRegAddressOffset == 0))
      specifyAdrReg(decl, adrReg);

    resultReg = adrReg;
  }

  /**
   * Specify that the address of a variable kept in memory is now in a
   * register.
   */
  private void specifyAdrReg(Declaration decl, int reg)
  {
    if (!registers.virtualRegister(reg))
      // Real registers get modified all the time so we can't use one
      // for this purpose.
      return;

    if (assignedRegister.get(reg))
      // This register is used to hold the value of a variable.  So,
      // this means that this variable is being set to the address of
      // decl.  It may be set again later and so it can't be used as the
      // source for the address of decl.
      return;

    int tag = decl.getTag();
    adrInRegister.set(tag);
    decl.setAddressRegister(reg);
    assignedRegister.set(reg);
    regToAdrDecl.put(reg, decl);
  }

  /**
   * Specify that the value of a variable kept in memory is now in a
   * register.
   */
  protected void specifyInReg(Declaration decl, int reg, ResultMode regha)
  {
    if (!registers.virtualRegister(reg))
      // Real registers get modified all the time so we can't use one
      // for this purpose.
      return;

    if (assignedRegister.get(reg))
      // This register is used to hold the value of a variable.  So,
      // this means that this variable is being set to the value of
      // decl.  It may be set again later and so it can't be used as the
      // source for the value of decl.
      return;

    if (decl.getType().isVolatile())
      // A volatile variable can not be kept in a register.
      return;

    // Specify that the value of decl is now in reg.

    int tag = decl.getTag();
    inRegister.set(tag);
    decl.setValueRegister(reg, regha);
    assignedRegister.set(reg);
  }

  /**
   * Return true if the register is assigned to a variable.
   */
  protected boolean isAssignedRegister(int reg)
  {
    return assignedRegister.get(reg);
  }

  /**
   * Load the address generated by an expression into a register.
   * Return the offset from the address in
   * <code>resultRegAddressOffset</code> and the address is in
   * <code>resultReg</code>.
   * @param exp specifies the expression
   * @param offset is the offset from the address
   */
  protected void calcAddressAndOffset(Expr exp, long offset)
  {
    Type originalType = exp.getType();

    while (exp.isCast())
      exp = ((ConversionExpr) exp).getArg();

    while (exp instanceof AdditionExpr) {
      AdditionExpr add = (AdditionExpr) exp;
      Expr         la  = add.getLeftArg();
      Expr         ra  = add.getRightArg();
      if (la.isLiteralExpr()) {
        Expr t = la;
        la = ra;
        ra = t;
      }

      if (!ra.isLiteralExpr())
        break;

      Literal lit = ((LiteralExpr) ra).getLiteral();
      if (!(lit instanceof IntLiteral))
        break;

      long v = ((IntLiteral) lit).getLongValue();
      offset += v;
      exp = la;
    }

    if (exp instanceof LoadDeclAddressExpr) {
      Declaration decl         = ((LoadDeclAddressExpr) exp).getDecl();
      int         tag          = decl.getTag();
      Chord       chord        = exp.getChord();
      boolean     isPredicated = false;

      if (chord.isExprChord())
        isPredicated = (((ExprChord) chord).getPredicate() != null);
      
      if (adrInRegister.get(tag)) {
        avoidedAdrCount++;
        resultReg = decl.getAddressRegister();
        resultRegAddressOffset = offset;
        resultRegMode = ResultMode.ADDRESS_VALUE;
        resultRegAddressAlignment = 8;
        return;
      }

      Assigned loc = decl.getStorageLoc();
      switch (loc) {
      case IN_REGISTER:
        ResultMode declRegMode    = decl.valueRegMode();
        resultReg                 = decl.getValueRegister();
        resultRegAddressOffset    = offset;
        resultRegAddressAlignment = 8;
        resultRegSize             = decl.getCoreType().memorySize(machine);
        switch (declRegMode) {
        case NORMAL_VALUE:  resultRegMode = ResultMode.NORMAL_VALUE;  break;
        case STRUCT_VALUE:  resultRegMode = ResultMode.STRUCT_VALUE;  break;
        case ADDRESS:       resultRegMode = ResultMode.ADDRESS_VALUE; break;
        case ADDRESS_VALUE: resultRegMode = ResultMode.ADDRESS_VALUE; break;
        }
        return;
      case ON_STACK:
        addrDisp = decl.getDisplacement().unique();
        resultReg = loadStackAddress(addrDisp);
        resultRegAddressOffset = offset;
        resultRegMode = ResultMode.ADDRESS_VALUE;
        resultRegAddressAlignment = 8;
        if (!isPredicated)
          specifyAdrReg(decl, resultReg);
        return;
      case IN_COMMON:
        EquivalenceDecl ed = (EquivalenceDecl) decl;
        putAddressInRegisterNO(ed.getBaseVariable(), isPredicated);
        resultRegAddressOffset += offset + ed.getBaseOffset();
        resultRegMode = ResultMode.ADDRESS_VALUE;
        resultRegAddressAlignment = decl.getType().alignment(machine);
        return;
      case IN_MEMORY:
        addrDisp = decl.getDisplacement().unique();
        resultReg = loadMemoryAddress(addrDisp);
        resultRegAddressOffset = offset;
        resultRegMode = ResultMode.ADDRESS_VALUE;
        resultRegAddressAlignment = decl.getType().alignment(machine);
        if (!isPredicated)
          specifyAdrReg(decl, resultReg);
        return;
      default:
        throw new scale.common.InternalError("Invalid declaration info " +
                                             loc +
                                             " " +
                                             decl);
      }
    }

    if (exp instanceof LoadFieldAddressExpr) {
      calcFieldAddress((LoadFieldAddressExpr) exp, offset);
      return;
    }

    if (exp instanceof ArrayIndexExpr) {
      calcArrayElementAddress((ArrayIndexExpr) exp, offset);
      return;
    }

    exp.visit(this);
    resultRegMode = ResultMode.ADDRESS_VALUE;
    resultRegAddressOffset += offset;
    resultRegAddressAlignment = (naln ?
                                 1 :
                                 originalType.getCoreType().getPointedTo().alignment(machine));
  }

  /**
   * Load the address of an array element into a register.
   * Return the offset from the address in <code>resultRegAddressOffset</code>.
   * The address is specified in <code>resultReg</code>.
   * @param aie specifies the array elementxpression
   * @param offset is the offset from the address
   */
  protected abstract void calcArrayElementAddress(ArrayIndexExpr aie, long offset);

  /**
   * Place the address of the argument in a register.
   * The result register is in <tt>resultReg</tt>.
   * The <code>resultRegAddressOffset</code> value is always 0.
   * The <code>resultRegMode</code> value is always <code>false</code>.
   * Return the register containing the address in <code>resultReg</code>.
   */
  protected void putAddressInRegister(Expr expr)
  {
    if (expr instanceof LoadValueIndirectExpr) {
      needValue(expr.getOperand(0));
      return;
    }

    if (expr instanceof LoadDeclValueExpr) {
      Declaration decl = ((LoadDeclValueExpr) expr).getDecl();
      Assigned    loc  = decl.getStorageLoc();
      if (loc != Assigned.IN_REGISTER) {
        boolean isPredicated = (((ExprChord) expr.getChord()).getPredicate() != null);
        putAddressInRegister(decl, isPredicated);
        return;
      }
    }

    Type type = processType(expr);

    expr.visit(this);
    int        src    = resultReg;
    ResultMode srcha  = resultRegMode;
    long       srcoff = resultRegAddressOffset;

    if (srcha == ResultMode.ADDRESS) { // It's already an address
      resultRegMode = ResultMode.NORMAL_VALUE;
      if (srcoff != 0) {
        int tr = registers.newTempRegister(RegisterSet.ADRREG);
        genLoadImmediate(srcoff, src, tr);
        resultReg = tr;
        resultRegAddressOffset = 0;
      }
      return;
    }

    int adrReg = registers.newTempRegister(RegisterSet.ADRREG);
    int ts     = allocStackAddress(adrReg, type);

    needValue(src, srcoff, srcha);
    storeIntoMemory(resultReg, adrReg, ts, ts, false);

    resultReg = adrReg;
    resultRegAddressOffset = 0;
    return;
  }

  /**
   * Allocate a location on the stack for storing a value of the
   * specified size.  Put the address of the location in the register.
   * @param adrReg specifies the register to receive the address
   * @param type is the type of the value
   * @return the size of the value to be stored
   */
  protected abstract int allocStackAddress(int adrReg, Type type);

  /**
   * Return the displacement for a string.
   * @param v is the string
   * @param size is the length of the string
   */
  protected abstract Displacement defStringValue(String v, int size);
  /**
   * Load an address of a memory location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement
   * or offset of one)
   * @return the register that is set with the address
   */
  protected abstract int loadMemoryAddress(Displacement disp);
  /**
   * Load an address of a stack location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement
   * or offset of one)
   * @return the register that is set with the address
   */
  protected abstract int loadStackAddress(Displacement disp);
  /**
   * Generate instructions to load an immediate integer value into a
   * register.
   * @param value is the value to load
   * @param dest is the register conatining the result
   * @return the register containing the value (usually dest but may
   * be a hardware zero register)
   */
  protected abstract int genLoadImmediate(long value, int dest);
  /**
   * Generate instructions to calculate a valid offset.  Most ISAs
   * provide only a small number of bits for an offset from an address
   * in a register for load and store instructions.  The Alpha
   * provides 16-bit signed offsets and the Sparc provides 13-bit
   * signed offsets.  The *resultReg* register is set to the register
   * containing the address to be used and the remaining offset is
   * returned.
   * @param value is the value to add to the base address
   * @param base is the base address
   * @return the lower 16 bits of the constant
   */
  protected abstract long genLoadHighImmediate(long value, int base);
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
  protected abstract int genLoadDblImmediate(double value, int dest, int destSize);
  /**
   * Return the data type as an integer.
   * @param size is the size in memory units
   * @param flt is true for floating point values
   * @see SpaceAllocation
   */
  public abstract int dataType(int size, boolean flt);
  /**
   * Generate instructions to move data from one register to another.
   * If one is an integer register and the other is a floating point register,
   * a memory location may be required.
   * @param src specifies the source register
   * @param dest specifies the destination register
   */
  protected abstract void genRegToReg(int src, int dest);
  /**
   *  Generate an add of address registers <code>laReg</code> and
   *  <code>raReg</code>.
   */
  protected abstract void addRegs(int laReg, int raReg, int dest);
  /**
   * Generate an instruction sequence to move words from one location
   * to another.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the address
   * @param dest specifies the register containing the destination
   * address
   * @param destoff specifies the offset from the address
   * @param size specifes the number of bytes to move
   * @param aln is the alignment that can be assumed for both the
   * source and destination addresses
   */
  protected abstract  void moveWords(int  src,
                                     long srcoff,
                                     int  dest,
                                     long destoff,
                                     int  size,
                                     int  aln);
  /**
   * Generate an instruction sequence to move words from one location
   * to another.  The destination offset must not be symbolic.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the address
   * @param size specifes the number of bytes to move
   * @param aln is the alignment that can be assumed for both the
   * source and destination addresses
   */
  protected abstract  void moveWords(int          src,
                                     long         srcoff,
                                     int          dest,
                                     Displacement destoff,
                                     int          size,
                                     int          aln);
  /**
   * Generate a branch based on the value of an expression compared to zero.
   * The value may be floating point or integer but it is never a value pair.
   * @param which specifies the branch test (EQ, NE, LT, ...)
   * @param treg specifies the register containing the value
   * @param signed is true if the value is signed
   * @param labt specifies the path if the test fails
   * @param labf specifies the path if the test succeeds
   */
  protected abstract void genIfRegister(CompareMode which,
                                        int         treg,
                                        boolean     signed,
                                        Label       labt,
                                        Label       labf);
  /**
   * Generate instructions to load an immediate integer value added to the 
   * value in a register into a register.
   * @param value is the value to load
   * @param base is the base register (e.g., AlphaRegisterSet.I0_REG)
   * @param dest is the register conatining the result
   */
  protected abstract void genLoadImmediate(long value, int base, int dest);
  /**
   * Generate a branch based on a relational expression.
   * @param rflag true if the test condition should be reversed
   * @param predicate specifies the relational expression
   * @param tc specifies the path if the test succeeds
   * @param fc specifies the path if the test fails
   */
  protected abstract void genIfRelational(boolean   rflag,
                                          MatchExpr predicate,
                                          Chord     tc,
                                          Chord     fc);

  /**
   * Create a call to the routine with the specified name.
   * This is used to call things like the divide subroutine.
   * @param name is the name of the function
   * @param uses is the set of registers used by the call
   * @param defs is the set of registers defined by the call or <code>null</code>
   * @return the branch instruction generated
   */
  protected abstract Branch genFtnCall(String name, short[] uses, short[] defs);

  /**
   * Load the arguments into registers for a routine call.  Only the
   * first machine-dependent words of arguements are placed into
   * registers.  The remaining words are placed on the stack.
   * @param args is the set of arguments
   * @param specialFirstArg is true if the first argument register is reserved
   * @return the set of registers used for the argument values
   */
  protected abstract short[] callArgs(Expr[] args, boolean specialFirstArg);

  private static int binaryOpDepth = 0;

  /**
   * Generate instructions to do a binary operation on two values.
   * @param c is the binary expression
   * @param which specifies the binary operation (ADD, SUB, ...)
   */
  protected void doBinaryOp(BinaryExpr c, int which)
  {
    Type ct = processType(c);
    int  ir = registers.getResultRegister(ct.getTag());
    Expr la = c.getLeftArg();
    Expr ra = c.getRightArg();

    if ((which == ADD) &&
        (la instanceof LoadDeclAddressExpr) &&
        ra.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) ra).getLiteral();
      if (lit instanceof IntLiteral) {
        IntLiteral il           = (IntLiteral) lit;
        long       value        = il.getLongValue();
        Chord      chord        = la.getChord();
        boolean    isPredicated = false;

        if (chord.isExprChord())
          isPredicated = (((ExprChord) chord).getPredicate() != null);

        putAddressInRegisterNO(((LoadDeclAddressExpr) la).getDecl(), isPredicated);
        genLoadImmediate(resultRegAddressOffset + value, resultReg, ir);
        resultReg = ir;
        resultRegMode = ResultMode.NORMAL_VALUE;
        return;
      }
    }

    binaryOpDepth++;
    Instruction seqb = lastInstruction;
    doBinaryOp(which, ct, la, ra, ir);
    binaryOpDepth--;
  }

  /**
   * Generate instructions to do a binary operation on two values.
   * @param which specifies the binary operation (ADD, SUB, ...)
   * @param ct is the result type
   * @param la is the left argument
   * @param ra is the right argument
   * @param dest is the destination register
   */
  protected abstract void doBinaryOp(int  which,
                                     Type ct,
                                     Expr la,
                                     Expr ra,
                                     int  dest);
  /**
   * Generate instructions to do a comparison of two values.
   * @param c is the compare expression
   * @param which specifies the compare (EQ, NE, ...)
   */
  protected abstract void doCompareOp(BinaryExpr c, CompareMode which);
  /**
   * Return the maximum area index value.
   */
  public abstract int getMaxAreaIndex();

  /**
   * Generate an integer displacement.
   * Reuse an exisiting one if possible.
   * @param value specifies the value of the displacement
   * @return the displacement
   */
  protected final IntegerDisplacement getDisp(int value)
  {
    int ival = value + 256;

    if ((ival < 512) && (ival >= 0)) {
      IntegerDisplacement disp = disps[ival];
      if (disp == null) {
        disp = new IntegerDisplacement(value);
        disps[ival] = disp;
      }
      return disp;
    }

    return new IntegerDisplacement(value);
  }

  /**
   * Insure that all types have their register types specified and all
   * structure fields have had their offsets determined.  The tag
   * field of the Type is set to the register type required to hold an
   * element of that type (or its address).
   * @return the type
   */
  private Type processCoreType(Type type)
  {
    if (type == null)
      return null;

    if (type.visited())
      return type;

    type.setVisited();

    AggregateType agt = type.returnAggregateType();
    if (agt != null)
      calcFieldOffsets(agt);
    else {
      ArrayType at = type.returnArrayType();
      if (at != null)
        processType(at.getElementType());
      else if (type.isPointerType())
        processType(type.getPointedTo());
    }

    int rt = 0;
    type.specifyCanBeInRegister(machine.keepTypeInRegister(type, true));
    if (type.canBeInRegister()) {
      int bs = type.memorySizeAsInt(machine);
      rt = registers.tempRegisterType(type, bs);
    } else {
      rt = RegisterSet.ADRREG;
    }

    type.setTag(rt);
    return type;
  }

  /**
   * Insure that all types have their register types specified and all
   * structure fields have had their offsets determined.  The tag
   * field of the Type is set to the register type required to hold an
   * element of that type (or its address).
   * @return the core type
   */
  protected Type processType(Type type)
  {
    return processCoreType(type.getCoreType());
  }

  /**
   * Insure that all types have their register types specified and all
   * structure fields have had their offsets determined.  The tag
   * field of the Type is set to the register type required to hold an
   * element of that type (or its address).
   * @return the core type
   */
  protected Type processType(Declaration decl)
  {
    return processCoreType(decl.getCoreType());
  }

  /**
   * Insure that all types have their register types specified and all
   * structure fields have had their offsets determined.  The tag
   * field of the Type is set to the register type required to hold an
   * element of that type (or its address).
   * @return the core type
   */
  protected Type processType(Expr expr)
  {
    return processCoreType(expr.getCoreType());
  }

  /**
   * Insure that all types have their register types specified and all
   * structure fields have had their offsets determined.  The tag
   * field of the Type is set to the register type required to hold an
   * element of that type (or its address).
   * @return the core type
   */
  protected Type processType(Expression expr)
  {
    return processCoreType(expr.getCoreType());
  }

  /**
   * Append the instruction to the end of the sequence of instructions.
   */
  protected void appendInstruction(Instruction inst)
  {
    if (trace) {
      if (!(inst instanceof Label))
        System.out.print("\t");
      System.out.println(inst);
    }

    // Record which loop (if any) this instruction is in.

    inst.setLoopNumber(loopNumber);

    lastInstruction.setNext(inst);
    lastInstruction = inst;
  }

  /**
   * Append the subroutine call instruction to the end of the sequence
   * of instructions.
   * @param call is the call instruction
   * @param lab is the label of the call return point
   * @param uses specifies what registers are used by the call
   * @param kills specifies what registers are killed by the call
   * @param defs specifies what registers are defined by the call
   * @param genLabel if the return label should be appended
   */
  protected void appendCallInstruction(Branch  call,
                                       Label   lab,
                                       short[] uses,
                                       short[] kills,
                                       short[] defs,
                                       boolean genLabel)
  {
    call.addTarget(lab, 0); // All call instruction must specify their return points.
    call.additionalRegsUsed(uses); // Specify which additional registers are used.
    call.additionalRegsKilled(kills);
    call.additionalRegsSet(defs);
    call.markAsCall();

    appendInstruction(call);
    if (genLabel) {
      appendLabel(lab);
      lab.markAsFirstInBasicBlock();
      lab.setNotReferenced();
    }
    inRegister.reset();
    adrInRegister.reset();
  }

  /**
   * Append the label to the end of the sequence of instructions.
   * Record the last Label appended.
   */
  protected void appendLabel(Label label)
  {
    label.setLabelIndex(labelID++);
    appendInstruction(label);
    lastLabel = label;
  }

  /**
   * Insert the instruction after the specified instruction.
   * @return the instruction inserted
   */
  protected Instruction insertInstruction(Instruction inst, Instruction location)
  {
    Instruction nxt = location.getNext();

    if (trace) {
      if (!(inst instanceof Label))
        System.out.print("\t");
      System.out.println(inst);
    }

    location.setNext(inst);
    inst.setNext(nxt);
    
    if (nxt == null)
      lastInstruction = inst;
    
    return inst;
  }

  /**
   * Insert the label after the specified instruction.
   * @return the instruction inserted
   */
  protected Instruction insertLabel(Label label, Instruction location)
  {  
    label.setLabelIndex(labelID++);
    Instruction inst = insertInstruction(label, location);
    lastLabel = label;
    return inst;
  }
  
  /**
   * Update the unique identifier for a label.
   */
  public void updateLabelIndex(Label label)
  {  
    label.setLabelIndex(labelID++);
  }
  
  /**
   * Move a sequence of instructions to another position.
   * If the position (after) is null, the instructions are deleted
   * @param prior is the instruction prior to the first instruction in
   * the sequence
   * @param last is the last instruction in the sequence
   * @param after is the instruction to insert the sequence after
   */
  protected void moveInstructionSequence(Instruction prior,
                                         Instruction last,
                                         Instruction after)
  {
    Instruction first  = prior.getNext();
    Instruction follow = last.getNext();

    // Remove the sequence.

    prior.setNext(follow);
    if (follow == null) 
      lastInstruction = prior;

    if (after == null)
      return;

    // Insert the sequence.

    follow = after.getNext();
    after.setNext(first);
    last.setNext(follow);
    
    if (follow == null)
      lastInstruction = last;
    
    if (trace) {
      while (after != last.getNext()) {
        System.out.println("*MOVE*\t" + after);
        after = after.getNext();
      }
    }
  }

  /**
   * Allocate a machine-specific label.
   * This method should be called only by scale.backend.Generator.
   */
  protected Label createNewLabel()
  {
    return new Label();
  }

  /**
   * Create a new Label and return it.
   */
  public final Label createLabel()
  {
    int li = newLabel(); // Do not combine this with the following statement or it will use the wrong array!
    return labels[li];
  }

  /**
   * Create a new Label and return its index.
   */
  public final int newLabel()
  {
    int   li  = labelIndex;
    Label lab = createNewLabel();

    lab.setStrength(currentStrength);

    if (li >= labels.length) {
      Label[] nm = new Label[li + 100];
      System.arraycopy(labels, 0, nm, 0, labels.length);
      labels = nm;
    }

    labels[li] = lab;
    labelIndex++;

    return li;
  }

  /**
   * Return the label whose index is specified.
   */
  public final Label getLabel(int index)
  {
    return labels[index];
  }

  /**
   * Return the label for a branch location.
   */
  protected final Label getBranchLabel(Chord location)
  {
    int labelIndex = location.getLabel();

    assert (labelIndex > 0) : "Node not labeled " + location;

    if (labelIndex >= labels.length) {
      Label[] nm = new Label[labelIndex + 100];
      System.arraycopy(labels, 0, nm, 0, labels.length);
      labels = nm;
    }

    Label lab = labels[labelIndex];
    if (lab == null) {
      lab = createNewLabel();
      lab.setStrength(currentStrength);
      labels[labelIndex] = lab;

      if (location.isFirstInBasicBlock())
        lab.markAsFirstInBasicBlock();
    }

    return lab;
  }

  /**
   * Return the {@link scale.backend.SpaceAllocation SpaceAllocation}
   * associated with the specified handle.
   */
  public final SpaceAllocation getSpaceAllocation(int handle)
  {
    return dataAreas[handle];
  }

  /**
   * Find an allocation of a floating point value.
   * Return the area handle or -1 if not found.
   * @param section specifies the section in which the loader should
   * place the area
   * @param type specifies the type of data
   * @param readOnly is true if the area is read-only
   * @param size is the number of addressable units required
   * @param value is the initial data value for the area
   * @param alignment specifies the address alignment required
   * @see SpaceAllocation
   * @return the index of the area
   */
  private int findAreaHandle(int     section,
                             int     type,
                             boolean readOnly,
                             long    size,
                             double  value,
                             int     alignment)
  {
    for (int i = nextArea - 1; i >= 0; i--) {
      SpaceAllocation sa = dataAreas[i];
      if (sa == null)
        continue;
      if (!sa.matches(section, type, size, readOnly))
        continue;
      Object val = sa.getValue();
      if (val instanceof Double) {
        double d = ((Double) val).doubleValue();
        if (d == value)
          return i;
      }
    }
    return -1;
  }

  /**
   * Find an allocation of a long value.
   * Return the area handle or -1 if not found.
   * @param section specifies the section in which the loader should
   * place the area
   * @param type specifies the type of data
   * @param readOnly is true if the area is read-only
   * @param size is the number of addressable units required
   * @param value is the initial data value for the area
   * @param alignment specifies the address alignment required
   * @see SpaceAllocation
   * @return the index of the area
   */
  private int findAreaHandle(int     section,
                             int     type,
                             boolean readOnly,
                             long    size,
                             long    value,
                             int     alignment)
  {
    for (int i = nextArea - 1; i >= 0; i--) {
      SpaceAllocation sa = dataAreas[i];
      if (sa == null)
        continue;
      if (!sa.matches(section, type, size, readOnly))
        continue;
      Object val = sa.getValue();
      if (val instanceof Long) {
        long d = ((Long) val).longValue();
        if (d == value)
          return i;
      }
    }
    return -1;
  }

  /**
   * Find an allocation of a String value.
   * Return the area handle or -1 if not found.
   * @param section specifies the section in which the loader should
   * place the area
   * @param type specifies the type of data
   * @param readOnly is true if the area is read-only
   * @param size is the number of addressable units required
   * @param value is the initial data value for the area
   * @param alignment specifies the address alignment required
   * @see SpaceAllocation
   * @return the index of the area
   */
  private int findAreaHandle(int     section,
                             int     type,
                             boolean readOnly,
                             long    size,
                             String  value,
                             int     alignment)
  {
    for (int i = nextArea - 1; i >= 0; i--) {
      SpaceAllocation sa = dataAreas[i];
      if (sa == null)
        continue;
      if (!sa.matches(section, type, size, readOnly))
        continue;
      Object val = sa.getValue();
      if (val instanceof String) {
        String d = (String) val;
        if (d.equals(value))
          return i;
      }
    }
    return -1;
  }

  /**
   * Find an allocation of a floating point value.
   * Return the Displacement or null if not found.
   * @param section specifies the section in which the loader should
   * place the area
   * @param type specifies the type of data
   * @param readOnly is true if the area is read-only
   * @param size is the number of addressable units required
   * @param value is the initial data value for the area
   * @param alignment specifies the address alignment required
   * @see SpaceAllocation
   * @return the index of the area
   */
  public final Displacement findAreaDisp(int     section,
                                         int     type,
                                         boolean readOnly,
                                         long    size,
                                         double  value,
                                         int     alignment)
  {
    int handle = findAreaHandle(section, type, readOnly, size, value, alignment);
    if (handle >= 0)
      return dataAreas[handle].getDisplacement();
    return null;
  }

  /**
   * Find an allocation of a long value.
   * Return the Displacement or null if not found.
   * @param section specifies the section in which the loader should
   * place the area
   * @param type specifies the type of data
   * @param readOnly is true if the area is read-only
   * @param size is the number of addressable units required
   * @param value is the initial data value for the area
   * @param alignment specifies the address alignment required
   * @see SpaceAllocation
   * @return the index of the area
   */
  public final Displacement findAreaDisp(int     section,
                                         int     type,
                                         boolean readOnly,
                                         long    size,
                                         long    value,
                                         int     alignment)
  {
    int handle = findAreaHandle(section, type, readOnly, size, value, alignment);
    if (handle >= 0)
      return dataAreas[handle].getDisplacement();
    return null;
  }

  /**
   * Find an allocation of a String value.
   * Return the Displacement or null if not found.
   * @param section specifies the section in which the loader should
   * place the area
   * @param type specifies the type of data
   * @param readOnly is true if the area is read-only
   * @param size is the number of addressable units required
   * @param value is the initial data value for the area
   * @param alignment specifies the address alignment required
   * @see SpaceAllocation
   * @return the index of the area
   */
  public final Displacement findAreaDisp(int     section,
                                         int     type,
                                         boolean readOnly,
                                         long    size,
                                         String  value,
                                         int     alignment)
  {
    int handle = findAreaHandle(section, type, readOnly, size, value, alignment);
    if (handle >= 0)
      return dataAreas[handle].getDisplacement();
    return null;
  }

  /**
   * Associate a displacement with an area.
   */
  public final void associateDispWithArea(int handle, Displacement disp)
  {
    dataAreas[handle].setDisplacement(disp);
  }

  /**
   * Return the value of the SizeofLiteral.
   */
  protected long valueOf(SizeofLiteral il)
  {
    Type t = processType(il.getSizeofType());
    long s = t.memorySize(machine);
    return s;
  }

  /**
   * Make sure all the elements of the aggregation are Literals and
   * that every AddressLiteral references a variable.
   */
  private void reduceAggregation(AggregationElements ag)
  {
    Vector<Object> vv = ag.getElementVector();
    int    l  = vv.size();
    for (int i = 0; i < l; i++) {
      Object o   = vv.get(i);
      if (!(o instanceof Expression))
        continue;
      Expression exp = (Expression) o;
      Literal    lit = exp.getConstantValue();

      assert ((lit != Lattice.Bot) && (lit != Lattice.Top)) :
        "It must be a constant! " + ag + "\n   " + exp + "\n   " + lit;

      if (lit instanceof AggregationElements)
        reduceAggregation((AggregationElements) lit);
      else if (lit instanceof AddressLiteral)
        lit = convertAddressLiteral((AddressLiteral) lit);

      vv.set(i, lit);
    }
  }

  /**
   * If the AddressLiteral references a constant, allocate that
   * constant and make the AddressLiteral reference that memory.
   */
  private AddressLiteral convertAddressLiteral(AddressLiteral al)
  {
    if (al.getDecl() != null)
      return al;

    Literal v    = al.getValue().getConstantValue();

    assert ((v != Lattice.Bot) && (v != Lattice.Top)) :
      "It must be a constant! " + al;

    Type    vdt  = v.getType();
    Type    vvt  = processType(vdt);
    int     valn = vvt.alignment(machine);
    long    vts  = 1;

    try {
      vts = vvt.memorySize(machine);
    } catch (java.lang.Error ex) {
    }

    String       vname  = un.genName();
    int          handle = allocateWithData(vname, vvt, vts, v, readOnlyDataArea, true, 1, valn);
    VariableDecl nvd    = new VariableDecl(vname, vvt);

    return new AddressLiteral(PointerType.create(vvt), nvd, al.getOffset());
  }

  /**
   * Allocate an area for the data specified by the Clef Expression.
   * @param name is the name of the data
   * @param type is the type of the variable
   * @param ts is the size of the area to allocate
   * @param init is the initial value
   * @param area is the data area to use
   * @param readOnly is true if the data should be read-only
   * @param reps is the number os times to repeat the data to fill the area
   * @param aln is the required alignment for the data
   * @return data area handle
   */
  protected final int allocateWithData(String     name,
                                       Type       type,
                                       long       ts,
                                       Expression init,
                                       int        area,
                                       boolean    readOnly,
                                       int        reps,
                                       int        aln)
  {
    if (init == null)
      return allocateData(name, area, SpaceAllocation.DAT_NONE, ts, false, null, reps, aln);

    Literal lit = init.getConstantValue();
    assert ((lit != Lattice.Bot) && (lit != Lattice.Top)) :
      "Invalid initializer " + name + " " + init;

    if (lit instanceof AggregationElements) {
      AggregationElements ag = (AggregationElements) lit;
      reduceAggregation(ag);
      return allocateWithAgData(name, type, ts, ag, area, readOnly, reps);
    }

    if (lit instanceof AddressLiteral)
      lit = convertAddressLiteral((AddressLiteral) lit);

    Type t  = processType(lit);
    int  ga = machine.generalAlignment();
    if (aln <= 0)
      aln  = (name != null) ?  ga : t.alignment(machine);

    int dt = getSAType(type);

    return allocateDataSpace(name, dt, ts, lit, area, readOnly, reps, aln);
  }

  /**
   * Allocate an area for the data specified by the Clef
   * AggregationElements expression.
   * @param name is the name of the data
   * @param type is the type of the variable
   * @param ts is the size of the area to allocate
   * @param ag is the initial value
   * @param area is the data area to use
   * @param readOnly is true if the data should be read-only
   * @param repeats is the number of times to repeat the data to fill
   * the area
   * @return data area handle
   */
  private int allocateWithAgData(String              name,
                                 Type                type,
                                 long                ts,
                                 AggregationElements ag,
                                 int                 area,
                                 boolean             readOnly,
                                 int                 repeats)
  {
    AggregateType agt = type.getCoreType().returnAggregateType();
    if (agt != null)
      return allocateStructWithData(name, agt, ts, ag, area, readOnly, repeats);

    ArrayType at = type.getCoreType().returnArrayType();
    if (at != null)
      return allocateArrayWithData(name, at, ts, ag, area, readOnly, repeats);

    FortranCharType fct = type.getCoreType().returnFortranCharType();
    if (fct != null) {
      Type      sct  = machine.getSignedCharType();
      ArrayType fcta = FixedArrayType.create(0, fct.getLength() - 1, sct);
      return allocateArrayWithData(name, fcta, ts, ag, area, readOnly, repeats);
    }

    throw new scale.common.InternalError("Unknown type " +
                                         type +
                                         " for " +
                                         name +
                                         " " +
                                         ag);
  }

  /**
   * Allocate an area for the aggregate data specified by the Clef
   * Expression.
   * @param name is the name of the data
   * @param type is the type of the variable
   * @param ts is the size of the area to allocate
   * @param init is the initial value
   * @param area is the data area to use
   * @param readOnly is true if the data should be read-only
   * @param reps is the number os times to repeat the data to fill the
   * area
   * @param aln is the required alignment for the data
   * @return data area handle
   */
  private int allocateAgWithData(String     name,
                                 Type       type,
                                 long       ts,
                                 Expression init,
                                 int        area,
                                 boolean    readOnly,
                                 int        reps,
                                 int        aln)
  {
    if (ts <= 0)
      return -1;

    if (init instanceof AggregationElements) {
      AggregationElements agi = (AggregationElements) init;
      return allocateWithAgData(name, type, ts, agi, area, readOnly, reps);
    }

    Type t    = processType(init);
    int  ga   = machine.generalAlignment();
    if (aln <= 0)
      aln  = (name != null) ? ga : t.alignment(machine);

    type = processType(type);

    int dt = 0;
    if (type.isAtomicType()) {
      if (type.isPointerType())
        dt = SpaceAllocation.DAT_ADDRESS;
      else if (type.isComplexType()) {
        int sz = type.memorySizeAsInt(machine);
        dt = dataType(sz / 2, type.isRealType());
      } else {
        int sz = type.memorySizeAsInt(machine);
        dt = dataType(sz, type.isRealType());
      }
    } else {
      ArrayType at = type.getCoreType().returnArrayType();
      if (at != null) {
        Type et = at.getElementType();
        int  sz = et.memorySizeAsInt(machine);
        if (et.isFortranCharType())
          dt = SpaceAllocation.DAT_BYTE;
        else
          dt = dataType(sz, et.isRealType());
      } else if (type.isAggregateType()) {
        dt = SpaceAllocation.DAT_BYTE;
      } else if (type.isFortranCharType()) {
        dt = SpaceAllocation.DAT_BYTE;
      } else
        throw new scale.common.InternalError("Unknown type " + type + " " + init);
    }

    return allocateDataSpace(name, dt, ts, init, area, readOnly, reps, aln);
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
      if (type.isComplexType())
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

  /**
   * Allocate an area for the data specified by the Clef Expression.
   * @param name is the name of the data
   * @param type is the type of the variable as an integer
   * @param ts is the size of the area to allocate
   * @param init is the initial value
   * @param area is the data area to use
   * @param readOnly is true if the data should be read-only
   * @param reps is the number os times to repeat the data to fill the area
   * @param aln is the required alignment for the data
   * @return data area handle
   */
  private int allocateDataSpace(String     name,
                                int        type,
                                long       ts,
                                Expression init,
                                int        area,
                                boolean    readOnly,
                                int        reps,
                                int        aln)
  {
    if (init instanceof IntLiteral)
      return allocateData(name, area, type, ts, readOnly, init, reps, aln);

    if (init instanceof FloatLiteral)
      return allocateData(name, area, type, ts, readOnly, init, reps, aln);

    if (init instanceof CharLiteral)
      return allocateData(name, area, type, ts, readOnly, init, reps, aln);

    if (init instanceof StringLiteral) {
      String str  = ((StringLiteral) init).getString();
      return allocateData(name, area, type, ts, readOnly, str, reps, aln);
    }

    if (init instanceof SizeofLiteral) {
      long value = valueOf((SizeofLiteral) init);
      return allocateData(name, area, type, ts, readOnly, new Long(value), reps, aln);
    }

    if (init instanceof AddressLiteral) {
      AddressLiteral al = (AddressLiteral) init;
      assert (al.getDecl() != null) : "This should have been handled already " + al;
      return allocateData(name, area, type, ts, readOnly, al, reps, aln);
    }

    if (init instanceof FloatArrayLiteral) {
      ArrayType at   = processType(init).getCoreType().returnArrayType();
      Type      et   = processType(at.getElementType());
      int       ealn = (name != null) ? machine.generalAlignment() : et.alignment(machine);
      return allocateData(name, area, type, ts, readOnly, init, reps, ealn);
    }

    if (init instanceof IntArrayLiteral) {
      ArrayType at   = processType(init).getCoreType().returnArrayType();
      Type      et   = processType(at.getElementType());
      int       ealn = (name != null) ? machine.generalAlignment() : et.alignment(machine);
      return allocateData(name, area, type, ts, readOnly, init, reps, ealn);
    }

    if (init instanceof ComplexLiteral)
      return allocateData(name, area, type, ts, readOnly, init, reps, aln);

    Literal o = init.getConstantValue();
    if ((o == Lattice.Bot) || (o == Lattice.Top))
      throw new scale.common.InternalError("Weird initializer " + init);

    assert (o != null) : "Invalid initialization " + init;

    return allocateDataSpace(name, type, ts, o, area, readOnly, reps, aln);
  }

  private int  bitOffset    = 0; /* How many bit-field bits so far. */
  private long bitBuffer    = 0; /* For assembling bit-fields. */
  private long structOffset = 0; /* Offset from start of structure. */

  private int dumpBits(String name, int area, boolean readOnly)
  {
    if (bitOffset <= 0)
      return -1;

    int  l      = (bitOffset + 7) / 8;
    byte[] data = new byte[l];

    if (little) {
      for (int i = 0; i < l; i++) {
        data[i] = (byte) bitBuffer;
        bitBuffer >>= 8;
      }
    } else {
      int x = bitOffset % 8;
      if (x != 0)
        bitBuffer <<= 8 - x;
      for (int i = l - 1; i >= 0; i--) {
        data[i] = (byte) bitBuffer;
        bitBuffer >>= 8;
      }
    }

    structOffset += machine.addressableMemoryUnits(bitOffset);
    bitOffset = 0;
    bitBuffer = 0;

    return allocateData(name, area, SpaceAllocation.DAT_BYTE, l, readOnly, data, 1, 1);
  }

  private int addBits(String name, int area, boolean readOnly, long value, int bits)
  {
    int  hh   = -1;
    long mask = (1L << bits) - 1;
    int  mbfs = maxBitFieldSize;

    if ((bits > 32) || (bitOffset > 32))
      mbfs = 64;

    if ((bits + bitOffset) > mbfs) {
      bitOffset = mbfs;
      hh = dumpBits(name, area, readOnly);
    }

    if (little)
      bitBuffer |= (value & mask) << bitOffset;
    else {
      bitBuffer <<= bits;
      bitBuffer |= value & mask;
    }

    bitOffset += bits;

    return hh;
  }

  /**
   * Allocate an area for the <code>struct</code> data specified by
   * the Clef AggregationElements expression.
   * @param name is the name of the data
   * @param type is the type of the variable
   * @param ts is the size of the area to allocate
   * @param ag is the initial value
   * @param area is the data area to use
   * @param readOnly is true if the data should be read-only
   * @param repeats is the number of times to repeat the data to fill
   * the area
   * @return data area handle
   */
  private int allocateStructWithData(String              name,
                                     AggregateType       at,
                                     long                ts,
                                     AggregationElements ag,
                                     int                 area,
                                     boolean             readOnly,
                                     int                 repeats)
  {
    Vector<Object>    v      = ag.getElementVector();
    Vector<FieldDecl> fields = at.getAgFields();

    int  l      = v.size();
    int  handle = -1;
    long sso    = structOffset;
    long sbb    = bitBuffer;
    int  sbo    = bitOffset;
    int  fi     = 0;

    // System.out.println("** aswd " + name);

    structOffset = 0;
    bitBuffer    = 0;
    bitOffset    = 0;

    for (int r = 0; r < repeats; r++) {
      for (int i = 0; i < l; i++) {
        Object     x    = v.elementAt(i);
        Expression lit  = null;
        // System.out.println("    " + x);

        if (x instanceof PositionFieldOp) {
          FieldDecl fd = ((PositionFieldOp) x).getField();
          fi = fields.indexOf(fd);
          assert (fi >= 0) : "Field not found " + fd;
          continue;
        }

        if (x instanceof PositionOffsetOp) {
          long      pos = ((PositionOffsetOp) x).getOffset();
          FieldDecl fd  = at.getFieldFromOffset(pos);
          fi = fields.indexOf(fd);
          assert (fi >= 0) : "Field not found " + fd;
          continue;
        }

        int reps = 1;
        if (x instanceof PositionRepeatOp) {
          reps = ((PositionRepeatOp) x).getCount();
          i++;
          x = v.elementAt(i);
          // System.out.println("         " + x);
        }

        assert (x instanceof Expression) : "What's this " + x;
        lit = (Expression) x;
        // System.out.println("      " + fields.get(fi));

        FieldDecl fd = fields.get(fi++);

        Type ft    = fd.getCoreType();
        long pos   = fd.getFieldOffset();
        int  bits  = fd.getBits();
        int  align = fd.getFieldAlignment();

        if (fd.getName().startsWith("_F")) {
          i--;
          if (ft.isRealType())
            lit = LiteralMap.put(0.0, ft);
          else
            lit = LiteralMap.put(0, ft);
        }

        if (bits > 0) { // Bit-field
          IntLiteral il    = (IntLiteral) lit;
          long       value = il.getLongValue();
          int        hh    = addBits(name, area, readOnly, value, bits);
          if ((handle < 0) && (hh >= 0)) {
            handle = hh;
            name = null;
          }
          continue;
        }

        int hh = dumpBits(name, area, readOnly);
        if (handle < 0) {
          handle = hh;
          if (handle >= 0)
            name = null;
        }

        if (pos > structOffset) { // Need alignment.
          int aln = (int) (pos - structOffset);
          int hhh = allocateData(name,
                                 area,
                                 SpaceAllocation.DAT_BYTE,
                                 aln,
                                 readOnly,
                                 long0,
                                 aln,
                                 1);
          if ((handle < 0) && (hhh >= 0)) {
            handle = hhh;
            name = null;
          }
          structOffset = pos;
        }

        long ts2  = ft.memorySize(machine);
        int  hhhh = allocateAgWithData(name,
                                       ft,
                                       ts2,
                                       lit,
                                       area,
                                       readOnly,
                                       reps,
                                       ((structOffset % align) == 0) ? align : 1);
        if ((handle < 0) && (hhhh > 0)) {
          handle = hhhh;
          name = null;
        }

        structOffset += ts2;
      }

      int hh = dumpBits(name, area, readOnly);
      if ((handle < 0) && (hh >= 0)) {
        handle = hh;
        name = null;
      }
    }

    if (structOffset < ts) {
      long size = ts - structOffset;
      allocateData(null, area, SpaceAllocation.DAT_BYTE, size, readOnly, long0, (int) size, 1);
    }

    if (handle >= 0)
      dataAreas[handle].setAlignment(at.alignment(machine));

    structOffset = sso;
    bitBuffer    = sbb;
    bitOffset    = sbo;

    return handle;
  }

  /**
   * Allocate an area for the array data specified by the Clef
   * AggregationElements expression.
   * @param name is the name of the data
   * @param type is the type of the variable
   * @param ts is the size of the area to allocate
   * @param ag is the initial value
   * @param area is the data area to use
   * @param readOnly is true if the data should be read-only
   * @param repeats is the number of times to repeat the data to fill the area
   * @return data area handle
   */
  private int allocateArrayWithData(String              name,
                                    ArrayType           at,
                                    long                ts,
                                    AggregationElements ag,
                                    int                 area,
                                    boolean             readOnly,
                                    int                 repeats)
  {
    int  handle = -1;
    long sso    = structOffset;
    long sbb    = bitBuffer;
    int  sbo    = bitOffset;

    structOffset = 0;
    bitBuffer    = 0;
    bitOffset    = 0;

    // System.out.println("*** aawd " + name);
    // System.out.println("         " + at);
    // System.out.println("         " + ag);

    Vector<Object> v = ag.getElementVector();

    int    l     = v.size();
    long   index = 0;
    Type   et    = at.getElementType();
    int    aln   = et.alignment(machine);
    long   bs    = et.memorySize(machine);

    int rk = at.getRank();
    if (!fortran && rk > 1) {
      try {
        bs = bs * at.getIndex(rk - 1).numberOfElements();
      } catch (java.lang.Exception ex) {
      }
    }

    // System.out.println("         " + bs + " " + et);

    for (int r = 0; r < repeats; r++) {
      for (int i = 0; i < l; i++) {
        Object     x    = v.elementAt(i);
        int        reps = 1;
        Expression lit = null;
        // System.out.println("         " + x);

        if (x instanceof PositionIndexOp) {
          index = ((PositionIndexOp) x).getIndex();
          continue;
        } else if (x instanceof PositionOffsetOp) {
          index = ((PositionOffsetOp) x).getOffset() / bs;
          continue;
        } else if (x instanceof PositionRepeatOp) {
          reps = ((PositionRepeatOp) x).getCount();
          i++;
          lit = (Expression) v.elementAt(i);
          // System.out.println("         " + lit);
        } else if (x instanceof Expression) {
          lit = (Expression) x;
        } else
          throw new scale.common.InternalError("What's this " + x);

        long pos  = index * bs;
        long diff = pos - structOffset;
        index += reps;

        if (diff > 0) {
          int hhh = allocateData(name,
                                 area,
                                 SpaceAllocation.DAT_BYTE,
                                 diff,
                                 readOnly,
                                 long0,
                                 (int) diff,
                                 1);
          if ((handle < 0) && (hhh >= 0)) {
            handle = hhh;
            name = null;
          }
          structOffset = pos;
        }

        Type ft = processType(lit);
        long sz = ft.memorySizeAsInt(machine);
        if (bs > sz)
          sz = bs;
        sz *= reps;
        int  hh = allocateAgWithData(name, ft, sz, lit, area, readOnly, reps, 0);
        if ((handle < 0) && (hh >= 0)) {
          handle = hh;
          name = null;
        }

        structOffset += sz;
      }
    }

    if (structOffset < ts) {
      long size = ts - structOffset;
      int  hh   = allocateData(name,
                               area,
                               SpaceAllocation.DAT_BYTE,
                               size,
                               readOnly,
                               long0,
                               (int) size,
                               1);
      if ((handle < 0) && (hh >= 0)) {
        handle = hh;
        name = null;
      }
    }

    dataAreas[handle].setAlignment(at.alignment(machine));

    structOffset = sso;
    bitBuffer    = sbb;
    bitOffset    = sbo;

    return handle;
  }

  private void expandDataAreas(int nextArea)
  {
    if (nextArea < dataAreas.length)
      return;

    int ns = nextArea + ((nextArea > 4096) ? 4096 : nextArea);
    SpaceAllocation[] na = new SpaceAllocation[ns];
    System.arraycopy(dataAreas, 0, na, 0, nextArea);
    dataAreas = na;
  }

  /**
   * Return the handle of a new memory area.
   * @param name is the name of the area (variable, routine, etc) or
   * null
   * @param section specifies the section in which the loader should
   * place the area
   * @param type specifies the type of data
   * @param size is the number of addressable units required
   * @param readOnly is true if the area is read-only
   * @param value is the initial data value for the area
   * @param reps is the number of times the value must be replicated
   * @param alignment specifies the address alignment required
   * @see SpaceAllocation
   */
  protected final int allocateData(String  name,
                                   int     section,
                                   int     type,
                                   long    size,
                                   boolean readOnly,
                                   Object  value,
                                   int     reps,
                                   int     alignment)
  {
    expandDataAreas(nextArea);

    SpaceAllocation sa    = new SpaceAllocation(name,
                                                section,
                                                type,
                                                readOnly,
                                                size,
                                                reps,
                                                alignment,
                                                value);
    int             index = nextArea++;

    dataAreas[index] = sa;

    return index;
  }

  /**
   * Return the handle of a new area of memory to contain instructions.
   */
  public final int allocateTextArea(String name, int area)
  {
    return allocateData(name, area, SpaceAllocation.DAT_TEXT, 0, true, null, 1, 16);
  }

  /**
   * Return the name associated with an area of memory.
   */
  public final String getName(int handle)
  {
    return dataAreas[handle].getName();
  }

  /**
   * Return the source language of the original program.
   */
  public final SourceLanguage getSourceLanguage()
  {
    return cg.getSourceLanguage();
  }

  /**
   * Return the {@link scale.callGraph.CallGraph call graph}
   * associated with this invocation of the code generator.
   */
  public final CallGraph getCallGraph()
  {
    return cg;
  }

  /**
   * Generate an error.
   */
  protected void whatIsThis(Note n)
  {
    throw new scale.common.InternalError("Unexpected  " + n);
  }

  /**
   * Process the expression and if the result is an address value, add
   * any offset required to the base address.  The result is in
   * <code>resultReg</code>, et al.
   */
  protected final void needValue(Expr exp)
  {
    exp.visit(this);
    int        src    = resultReg;    
    long       srcoff = resultRegAddressOffset;    
    ResultMode srcha  = resultRegMode;

    if (srcha == ResultMode.ADDRESS_VALUE) {
      if (srcoff != 0) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(srcoff, src, tr);
        resultReg = tr;
        resultRegAddressOffset = 0;
      }
      resultRegMode = ResultMode.NORMAL_VALUE;
    }
  }

  /**
   * If the register contains an address value, add any offset
   * required to the base address.  The result is in
   * <code>resultReg</code>, et al.
   */
  protected final void needValue(int src, long srcoff, ResultMode srcha)
  {
    if (srcha == ResultMode.ADDRESS_VALUE) {
      if (srcoff != 0) {
        int tr = registers.newTempRegister(RegisterSet.INTREG);
        genLoadImmediate(srcoff, src, tr);
        src = tr;
      }
      srcha = ResultMode.NORMAL_VALUE;
    }

    assert (srcha == ResultMode.NORMAL_VALUE) || (srcha == ResultMode.STRUCT_VALUE):
      "Value mode " + srcha;

    resultRegMode          = srcha;
    resultRegAddressOffset = srcoff;
    resultReg              = src;
  }

  /**
   * Load the value of a variable into a register.  Return the
   * register into which the variable value is loaded in
   * <code>resultReg</code>.  If the value can not be loaded into a
   * register, return the 2's complement of the register containing
   * the address of the data.
   * @param vd specifies the variable
   * @param vt specifies the type of the value
   */
  protected void loadVariable(VariableDecl vd, Type vt, boolean isPredicated)
  {
    Assigned loc  = vd.getStorageLoc();
    Type     type = processType(vd);

    if (loc == Assigned.IN_REGISTER) {
      int        reg   = vd.getValueRegister();
      ResultMode regha = vd.valueRegMode();
      if ((regha == ResultMode.ADDRESS) && type.getCoreType().canBeInRegister()) {
        int tr = registers.newTempRegister(type.getTag());
        loadFromMemoryWithOffset(tr,
                                 reg,
                                 0,
                                 type.memorySizeAsInt(machine),
                                 0,
                                 type.isSigned(),
                                 type.isRealType());
        resultReg = tr;
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        if (!naln && type.isPointerType())
          resultRegAddressAlignment = type.getPointedTo().alignment(machine);
        return;
      }
      resultReg = reg;
      resultRegAddressOffset = 0;
      resultRegMode = regha;
      if (!naln && type.isPointerType())
        resultRegAddressAlignment = type.getPointedTo().alignment(machine);
      return;
    }

    int tag = vd.getTag();
    if (inRegister.get(tag) && !(vd.isEquivalenceDecl())) {
      // Even though the variable is in memory or on the stack, we
      // know its most recent value is in a register.  So, use the
      // register and avoid the load.
      avoidedLoadCount++;
      resultReg = vd.getValueRegister();
      resultRegAddressOffset = 0;
      resultRegMode = vd.valueRegMode();
      if (!naln && type.isPointerType())
        resultRegAddressAlignment = type.getPointedTo().alignment(machine);
      return;
    }

    Instruction first = lastInstruction;

    switch (loc) {
    case ON_STACK:
      assert (vd.getDisplacement() != null) : "Processed? " + vd;
      loadVariableFromStack(vd.getDisplacement(), vt);
      break;
    case IN_MEMORY:
      loadVariableFromMemory(vd.getDisplacement(), vt);
      break;
    case IN_COMMON :
      EquivalenceDecl ed      = (EquivalenceDecl) vd;
      Declaration     bd      = ed.getBaseVariable();
      long            offset  = ed.getBaseOffset();
      putAddressInRegisterNO(bd, isPredicated);
      loadVariableFromCommon(resultReg, vt, offset + resultRegAddressOffset);
      break;
    default:
      throw new scale.common.InternalError("load value from where? " + loc);
    }

    if (vt.isVolatile()) {
      // If the variable is volatile, we can't eliminate any loads
      // from memory for it.

      markLoadsAsMandatory(first.getNext());

      return;
    }

    // Since we have loaded the variable to a register, use the value
    // in the register for the next reference instead of re-loading
    // the variable from memory.

    if (!isPredicated)
      specifyInReg(vd, resultReg, resultRegMode);
  }

  private void markLoadsAsMandatory(Instruction first)
  {
    for (Instruction inst = first; inst != null; inst = inst.getNext())
      if (inst.isLoad())
        inst.setMandatory();
  }

  /**
   * Return true if the expression will not result in a call to a
   * routine.
   */
  public boolean isSimple(Expr arg)
  {
    if (arg.numInDataEdges() == 0)
      return true;

    if (arg instanceof UnaryExpr) {
      UnaryExpr ua = (UnaryExpr) arg;
      if (ua instanceof TranscendentalExpr)
        return false;
      return isSimple(ua.getArg());
    }

    if ((arg instanceof AdditionExpr) || (arg instanceof SubtractionExpr)) {
      BinaryExpr be = (BinaryExpr) arg;
      return isSimple(be.getLeftArg()) && isSimple(be.getRightArg());
    }

    return false;
  }

  public void visitLoadDeclAddressExpr(LoadDeclAddressExpr e)
  {
    Declaration decl         = e.getDecl();
    boolean     isPredicated = false;
    Chord       chord        = e.getChord();

    if (chord.isExprChord())
      isPredicated = (((ExprChord) chord).getPredicate() != null);
    
    putAddressInRegister(decl, isPredicated);
  }

  /**
   * Load the value of a declaration into a register.
   */
  protected void loadDeclValue(Declaration decl, Type vt, boolean isPredicated)
  {
    VariableDecl vd = decl.returnVariableDecl();
    if (vd == null) {
      assert (decl.isRoutineDecl()) : "Improper declaration for load " + decl;

      putAddressInRegister(decl, isPredicated);
      return;
    }

    if (vt.isAtomicType()) {
      loadVariable(vd, vt, isPredicated);
      return;
    }

    if (vt.isAggregateType()) {
      // Load the address instead of the value.
      Assigned loc = vd.getStorageLoc();

      if (loc == Assigned.IN_REGISTER) {
        resultRegAddressOffset = 0;
        resultRegMode          = vd.valueRegMode();
        resultReg              = vd.getValueRegister();
        return;
      }

      putAddressInRegisterNO(vd, isPredicated);
      resultRegAddressAlignment = vt.alignment(machine);
      resultRegMode = ResultMode.ADDRESS;
      return;
    }

    if (vt.canBeInRegister()) {
      loadVariable(vd, vt, isPredicated);
      return;
    }

    putAddressInRegisterNO(vd, isPredicated);
    resultRegAddressAlignment = vt.alignment(machine);
    resultRegMode = ResultMode.ADDRESS;
    return;
  }

  public void visitLoadValueIndirectExpr(LoadValueIndirectExpr lvie)
  {
    Expr adr = lvie.getArg().getLow();
    Type vt  = processType(lvie);

    if (!vt.getCoreType().canBeInRegister()) {
      adr.visit(this);
      resultRegMode = ResultMode.ADDRESS;
      resultRegAddressAlignment = naln ? 1 : vt.alignment(machine);;
      return;
    }

    int ir = registers.getResultRegister(vt.getTag());

    calcAddressAndOffset(adr, 0);

    if (resultRegMode == ResultMode.NORMAL_VALUE)
      return;

    assert (resultRegMode == ResultMode.ADDRESS_VALUE);

    int  address = resultReg;
    long offset  = resultRegAddressOffset;
    long align   = resultRegAddressAlignment;
    int  bs      = vt.memorySizeAsInt(machine);
    int  oaln    = ((offset % bs) == 0) ? bs : 1;

    if (oaln < align)
      align = oaln;

    loadFromMemoryWithOffset(ir, address, offset, bs, align, vt.isSigned(), vt.isRealType());
    resultRegAddressOffset = 0;
    resultRegMode = vt.isAggregateType() ? ResultMode.STRUCT_VALUE : ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitLoadDeclValueExpr(LoadDeclValueExpr e)
  {
    Declaration decl         = e.getDecl();
    Chord       chord        = e.getChord();
    boolean     isPredicated = false;

    if (chord.isExprChord())
      isPredicated = (((ExprChord) chord).getPredicate() != null);

    loadDeclValue(decl, processType(e.getType()), isPredicated);
  }

  public void visitLoadFieldAddressExpr(LoadFieldAddressExpr e)
  {
    calcFieldAddress(e, 0);
  }

  public void visitLoadFieldValueExpr(LoadFieldValueExpr e)
  {
    Expr      struct      = e.getStructure();
    FieldDecl fd          = e.getField();
    long      fieldOffset = fd.getFieldOffset();
    Type      ft          = processType(fd);
    int       ir          = registers.getResultRegister(ft.getTag());

    processType(struct);

    calcAddressAndOffset(struct, fieldOffset);
    int        adr    = resultReg;
    ResultMode adrha  = resultRegMode;
    int        adraln = resultRegAddressAlignment;
    long       adrrs  = resultRegSize;

    fieldOffset = resultRegAddressOffset;

    Instruction first = lastInstruction;

    loadFieldValue(fd, fieldOffset, adr, adrha, adraln, adrrs, ir);

    if (!fd.getType().isVolatile() &&
        !struct.getCoreType().getPointedTo().isVolatile())
      return;

    // If the field is volatile, we can't eliminate any loads
    // from memory for it.

    markLoadsAsMandatory(first.getNext());
  }

  /**
   * Load the value of a field to a register.
   * @param fd defines the field
   * @param fieldOffset is the offset from the specified address
   * @param adr is the register holding the address
   * @param adrha specifies the type of address
   * @param adraln specifies the alignment of the address
   * @param adrrs specifies the size of the structure if it is in a
   * register
   * @param dest specifies the register to hold the field value
   */
  protected abstract void loadFieldValue(FieldDecl  fd,
                                         long       fieldOffset,
                                         int        adr,
                                         ResultMode adrha,
                                         int        adraln,
                                         long       adrrs,
                                         int        dest);

  /**
   * Calculate the offset and base address of a field.  The base
   * address is returned in a register that is specified by the
   * <code>resultReg</code> member.  Return the offset from the
   * address in <code>resultRegAddressOffset</code>.  The address is
   * specified in <code>resultReg</code>.
   * @param offset is the initial offset (normally 0)
   */
  protected void calcFieldAddress(LoadFieldAddressExpr c, long offset)
  {
    Expr      struct = c.getStructure();
    FieldDecl fd     = c.getField();

    processType(struct);

    assert (fd.getBits() == 0) : "Can't take address of bit field " + c;

    calcAddressAndOffset(struct, offset + fd.getFieldOffset());
    resultRegAddressAlignment = fd.getFieldAlignment();
  }

  public void visitArrayIndexExpr(ArrayIndexExpr e)
  {
    calcArrayElementAddress(e, 0);
  }

  public void visitBeginChord(BeginChord c)
  {
    // Do nothing.
  }

  public void visitExitChord(ExitChord c)
  {
    String  fname = "exit";
    Expr    x     = c.getRValue();
    short[] uses;

    if (x != null) {
      int  far = getFirstArgRegister(RegisterSet.INTREG);
      Type xt  = processType(x);

      registers.setResultRegister(far);
      needValue(x);
      genRegToReg(resultReg, far);
      registers.setResultRegister(-1);

      FixedArrayType ty = xt.getCoreType().returnFixedArrayType();
      if (ty != null) {
        int dest = genLoadImmediate(ty.memorySizeAsInt(machine), far + 1);

        genRegToReg(dest, far + 1);
        fname = "_scale_stop";
        uses = genDoubleUse(far, far + 1);
      } else
        uses = genSingleUse(far);
    } else {
      int far  = getFirstArgRegister(RegisterSet.INTREG);
      int dest = genLoadImmediate(0, far);
      genRegToReg(dest, far);
      uses = genSingleUse(far);
    }
    genFtnCall(fname, uses, null);
  }

  /**
   * Generate the array of registers that a call to a function
   * requires.  The specified register is the argument register.
   * Other registers required by the calling convention are added to
   * the array.
   */
  protected abstract short[] genSingleUse(int reg);
  /**
   * Generate the array of registers that a call to a function
   * requires.  The specified registers are the two argument
   * registers.  Other registers required by the calling convention
   * are added to the array.
   */
  protected abstract short[] genDoubleUse(int reg1, int reg2);

  public void visitExprChord(ExprChord c)
  {
    Expr lhs = c.getLValue();
    Expr rhs = c.getRValue();

    Expr predicate = c.getPredicate();

    if (predicate == null) {
      doStore(lhs, rhs, c.isVaCopy());
      lastInstruction.specifySpillStorePoint();
      return;
    }

    Label       labt  = createLabel();
    Label       labf  = createLabel();
    CompareMode which = c.predicatedOnTrue() ? CompareMode.NE : CompareMode.EQ;

    needValue(predicate);

    genIfRegister(which, resultReg, true, labt, labf);
    appendLabel(labt);
    doStore(lhs, rhs, c.isVaCopy());
    lastInstruction.specifySpillStorePoint();
    appendLabel(labf);
  }

  /**
   * Generate code for a copy (i.e., assignment).
   */
  protected void doStore(Expr lhs, Expr rhs, boolean vaCopy)
  {
    if (lhs == null) {
      rhs.visit(this);
      return;
    }

    if (vaCopy) {
      doVaCopy(lhs, rhs);
      return;
    }

    if (lhs instanceof LoadDeclAddressExpr) {
      storeLdae((LoadDeclAddressExpr) lhs, rhs);
      lastInstruction.specifySpillStorePoint();
      return;
    } 

    if (lhs instanceof LoadFieldAddressExpr) {
      storeLfae((LoadFieldAddressExpr) lhs, rhs);
      lastInstruction.specifySpillStorePoint();
      return;
    }

    inRegister.reset();

    if (lhs instanceof LoadValueIndirectExpr) {
      storeLvie((LoadValueIndirectExpr) lhs, rhs);
      return;
    }

    if (lhs.isLiteralExpr()) {
      storeLiteral((LiteralExpr) lhs, rhs);
      return;
    }

    if (lhs instanceof ArrayIndexExpr) {
      ArrayIndexExpr aie  = (ArrayIndexExpr) lhs;
      Type           et   = aie.getCoreType().getPointedTo();
      int            size = et.memorySizeAsInt(machine);

      rhs.visit(this);
      int        raReg = resultReg;
      long       raOff = resultRegAddressOffset;
      int        raln  = resultRegAddressAlignment;
      ResultMode raha  = resultRegMode;

      lhs.visit(this);
      int  laReg = resultReg;
      long laOff = resultRegAddressOffset;
      int  laln  = resultRegAddressAlignment;

      if (raha == ResultMode.ADDRESS) {
        int  aln   = (laln < raln) ? laln : raln;
        moveWords(raReg, raOff, laReg, laOff, size, aln);
        resultReg = raReg;
        resultRegMode = ResultMode.ADDRESS;
        resultRegAddressOffset = raOff;
        resultRegAddressAlignment = raln;
      } else {
        needValue(raReg, raOff, raha);
        storeIntoMemoryWithOffset(resultReg,
                                  laReg,
                                  laOff,
                                  size,
                                  et.alignment(machine),
                                  et.isRealType());
      }
      return;
    }

    Type lt = processType(lhs);
    Type et = lt.getPointedTo();

    while (lhs.isCast())
      lhs = lhs.getOperand(0);

    if (lhs instanceof LoadDeclValueExpr) {
      storeLdve((LoadDeclValueExpr) lhs, rhs);
      return;
    }

    if (lhs instanceof AdditionExpr) {
      storeAdd(et, (AdditionExpr) lhs, rhs, 1);
      return;
    }

    if (lhs instanceof SubtractionExpr) {
      storeAdd(et, (SubtractionExpr) lhs, rhs, -1);
      return;
    }

    throw new scale.common.InternalError("Unknown left hand side " + lhs);
  }

  private void storeAdd(Type et, BinaryExpr add, Expr rhs, int mult)
  {
    Expr la = add.getLeftArg();
    Expr ra = add.getRightArg();

    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
    }

    int size = et.memorySizeAsInt(machine);

    if (et.getCoreType().canBeInRegister() && ra.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) ra).getLiteral().getConstantValue();
      if (lit instanceof IntLiteral) {
        long offset = ((IntLiteral) lit).getLongValue();
        needValue(rhs);
        int raReg = resultReg;
        la.visit(this);
        int laReg = resultReg;
        int laaln = resultRegAddressAlignment;
        int oaln  = ((offset % size) == 0) ? size : 1;

        offset += resultRegAddressOffset;

        if (oaln < laaln)
          laaln = oaln;
        storeIntoMemoryWithOffset(raReg, laReg, mult * offset, size, laaln, et.isRealType());
        return;
      }
    }

    rhs.visit(this);
    int        raReg = resultReg;
    long       raOff = resultRegAddressOffset;
    int        raln  = resultRegAddressAlignment;
    ResultMode raha  = resultRegMode;

    boolean flag   = false;
    long    offset = 0;

    if (ra.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) ra).getLiteral().getConstantValue();
      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        offset = mult * il.getLongValue();
        flag = true;
      }
    }

    int  laReg;
    long laOff;
    int  laln;
    if (flag) {
      la.visit(this);
      laReg = resultReg;
      laOff = resultRegAddressOffset + offset;
      laln  = resultRegAddressAlignment;
      int oaln  = ((laOff % size) == 0) ? size : 1;
      if (oaln < laln)
        laln = oaln;
    } else {
      add.visit(this);
      laReg = resultReg;
      laOff = resultRegAddressOffset;
      laln  = naln ? 1 : et.alignment(machine);
    }

    if (raha == ResultMode.ADDRESS) {
      int aln = (laln < raln) ? laln : raln;
      moveWords(raReg, raOff, laReg, laOff, size, aln);
      return;
    }

    needValue(raReg, raOff, raha);
    storeIntoMemoryWithOffset(resultReg, laReg, laOff, size, laln, et.isRealType());
  }

  /**
   * Generate code for a va_copy().
   */
  protected void doVaCopy(Expr lhs, Expr rhs)
  {
    int pr = registers.getResultRegister(RegisterSet.ADRREG);
    int bs = processType(rhs).memorySizeAsInt(machine);

    lhs.visit(this);
    int  dr    = resultReg;
    long droff = resultRegAddressOffset;

    rhs.visit(this);
    int  sr    = resultReg;
    long sroff = resultRegAddressOffset;

    loadFromMemoryWithOffset(pr, sr, sroff, bs, 0, true, false);
    storeIntoMemoryWithOffset(pr, dr, droff, bs, 0, false);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = sr;
  }

  public void visitIfThenElseChord(IfThenElseChord c)
  {
    Expr    p     = c.getPredicateExpr();
    Chord   fc    = c.getFalseCfgEdge();
    Chord   tc    = c.getTrueCfgEdge();
    boolean rflag = false;

    branchPrediction = c.getBranchProbability(tc);

    tc = getBranchTarget(tc);
    fc = getBranchTarget(fc);

    if (p instanceof NotExpr) {
      p = ((NotExpr) p).getOperand(0);
      rflag = true;
    }

    if (p.isLiteralExpr()) { // No branch needed.
      if (((LiteralExpr) p).isZero() ^ rflag) {
        if (tc.numInCfgEdges() == 1) // No branch - this edge not needed.
          wlCFG.remove(tc);
        if (fc.numInCfgEdges() == 1) // No branch - no label needed.
          fc.setLabel(0);
        doNext(fc); // The generated code falls through.
        successorCFGNode = fc;
        return;
      }

      if (fc.numInCfgEdges() == 1) // No branch - this edge not needed.
        wlCFG.remove(fc);

      if (tc.numInCfgEdges() == 1) // No branch - no label needed.
        tc.setLabel(0);

      doNext(tc); // The generated code falls through.
      successorCFGNode = tc;
      return;
    }

    if (p.isMatchExpr()) {
      genIfRelational(rflag, (MatchExpr) p, tc, fc);
      return;
    }

    if (p.hasTrueFalseResult()) {
      needValue(p);
      if (rflag) {
        Chord t = tc;
        tc = fc;
        fc = t;
      }
      genTrueFalseBranch(resultReg, tc, fc);
      return;
    }

    // Result of the predicate should be an integer zero or one.

    genIfRegister(rflag ? CompareMode.EQ : CompareMode.NE, p, tc, fc);
  }

  public void visitSwitchChord(SwitchChord c)
  {
    Expr    p      = c.getPredicateExpr();
    long[]  keys   = c.getBranchEdgeKeyArray();
    Chord[] cases  = c.getOutCfgEdgeArray();
    int     num    = keys.length;
    int     def    = c.getDefaultIndex();
    long    min    = Long.MAX_VALUE;
    long    max    = Long.MIN_VALUE;

    for (int i = 0; i < cases.length; i++)
      cases[i] = getBranchTarget(cases[i]);

    // Put the default last.

    if (def >= 0) { // Computed gotos have no default.
      long ot = keys[num - 1];
      keys[num - 1] = keys[def];
      keys[def] = ot;

      Chord ct = cases[num - 1];
      cases[num - 1] = cases[def];
      cases[def] = ct;
    }

    // Find the minimum and maximum key values.

    for (int i = 0; i < num - 1; i++) {
      if (keys[i] < min)
        min = keys[i];
      if (keys[i] > max)
        max = keys[i];
    }

    needValue(p);
    int testReg = resultReg;

    lastInstruction.specifySpillStorePoint();

    // It is probably not beneficial to generate large blocks of 
    // nested if's.
    
    if (genSwitchUsingIfs(testReg, cases, keys, num, max - min))
      return;

    // Use transfer vector.

    Label labd = getBranchLabel(cases[num - 1]);
    
    genSwitchUsingTransferVector(testReg, cases, keys, labd, min, max);
  }

  /**
   * Generate the code for a <code>switch</code> statement using
   * branches for each case.
   * @param testReg is the register holding the selected key value
   * @param cases is the list of CFG nodes for the switch cases
   * @param keys is the list of case values
   * @param num is the number of cases
   * @param spread is a measure of the density of the cases values
   * @return true if code for the switch statement was generated
   */
  protected abstract boolean genSwitchUsingIfs(int     testReg,
                                               Chord[] cases,
                                               long[]  keys,
                                               int     num,
                                               long    spread);
  /**
   * Generate the code for a <code>switch</code> statement using
   * branches for each case.
   * @param testReg is the register holding the selected key value
   * @param cases is the list of CFG nodes for the switch cases
   * @param keys is the list of case values
   * @param dflt is the label for the default case
   * @param min is the smallest case value
   * @param max is the largest case value
   */
  protected abstract void genSwitchUsingTransferVector(int     testReg,
                                                       Chord[] cases,
                                                       long[]  keys,
                                                       Label   dflt,
                                                       long    min,
                                                       long    max);
  /**
   * Generate a branch based on the value of an expression which is
   * guaranteed to be either 0 or 1.
   * @param predicate specifies the register to test
   * @param tc specifies the path if the test succeeds
   * @param fc specifies the path if the test fails
   */
  protected void genTrueFalseBranch(int predicate, Chord tc, Chord fc)
  {
    genIfRegister(CompareMode.NE, predicate, true, tc, fc);
  }

  /**
   * Generate a branch based on the value of an expression compared to zero.
   * @param which specifies the branch test (EQ, NE, LT, ...)
   * @param predicate specifies the condition to test
   * @param tc specifies the path if the test succeeds
   * @param fc specifies the path if the test fails
   */
  protected void genIfRegister(CompareMode which, Expr predicate, Chord tc, Chord fc)
  {
    Type pt = processType(predicate);
    needValue(predicate);
    genIfRegister(which, resultReg, pt.isSigned(), tc, fc);
  }

  /**
   * Generate a branch based on the value of an expression compared to
   * zero.
   * @param which specifies the branch test (EQ, NE, LT, ...)
   * @param treg specifies the condition (register value) to test
   * @param signed is true if the value is signed
   * @param tc specifies the path if the test succeeds
   * @param fc specifies the path if the test fails
   */
  protected void genIfRegister(CompareMode which,
                               int         treg,
                               boolean     signed,
                               Chord       tc,
                               Chord       fc)
  {
    lastInstruction.specifySpillStorePoint();
    which = which.reverse();

    // Pick the branch point to be the one that has more branches to it.

    if (tc.numInCfgEdges() > fc.numInCfgEdges()) {
      which = which.reverse();
      Chord c = tc;
      tc = fc;
      fc = c;
      branchPrediction = 1.0 - branchPrediction;
    }

    // Branch handling for non-hyperblocked CFG's.
    
    boolean genUnconditional = false;
    if (!doNext(tc)) {
      if (doNext(fc)) {
        which = which.reverse();
        Chord c = tc;
        tc = fc;
        fc = c;
        branchPrediction = 1.0 - branchPrediction;
      } else
        genUnconditional = true;
    }
    
    Label labt = getBranchLabel(tc);
    Label labf = getBranchLabel(fc);
    Label labn = labt;

    if (genUnconditional)
      labn = createLabel();

    genIfRegister(which, treg, signed, labn, labf);

    if (genUnconditional) {
      appendLabel(labn);
      labn.setNotReferenced();
      generateUnconditionalBranch(labt);
    } else if (tc.numInCfgEdges() == 1)
      labt.setNotReferenced();
  }

  public void visitAdditionExpr(AdditionExpr e)
  {
    doBinaryOp(e, ADD);
  }

  public void visitAndExpr(AndExpr e)
  {
    doBinaryOp(e, AND);
  }

  public void visitBitAndExpr(BitAndExpr e)
  {
    doBinaryOp(e, AND);
  }

  public void visitBitOrExpr(BitOrExpr e)
  {
    doBinaryOp(e, OR);
  }

  public void visitBitXorExpr(BitXorExpr e)
  {
    doBinaryOp(e, XOR);
  }

  public void visitOrExpr(OrExpr e)
  {
    doBinaryOp(e, OR);
  }

  public void visitSubtractionExpr(SubtractionExpr e)
  {
    doBinaryOp(e, SUB);
  }

  public void visitBitShiftExpr(BitShiftExpr e)
  {
    ShiftMode mode = e.getShiftMode();

    switch (mode) {
    case SignedRight:   doBinaryOp(e, SRA); break;
    case UnsignedRight: doBinaryOp(e, SRL); break;
    case Left:          doBinaryOp(e, SLL); break;
      //      case BitShiftOp.cLeftRotate:    doBinaryOp(e, OR); break;
      //      case BitShiftOp.cRightRotate:   doBinaryOp(e, OR); break;
    default:
      throw new scale.common.InternalError("Unknown shift " + e);
    }
  }

  /**
   * Generate the code for the Fortran ATAN2()</code> intrinsic
   * function.
   */
  protected abstract void genAtan2Ftn(int dest, int laReg, int raReg, Type rType);
  /**
   * Generate the code for the Fortran SIGN()</code> intrinsic
   * function.
   */
  protected abstract void genSignFtn(int dest, int laReg, int raReg, Type rType);
  /**
   * Generate the code for the Fortran <code>DIM()</code> intrinsic
   * function.
   */
  protected abstract void genDimFtn(int dest, int laReg, int raReg, Type rType);
  /**
   * Generate the code for the <code>sqrt()</code> function.
   */
  protected abstract void genSqrtFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>alloca()</code> function.
   */
  protected abstract void genAlloca(Expr arg, int reg);
  /**
   *  Generate the code for the <code>exp()</code> function.
   */
  protected abstract void genExpFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>log()</code> function.
   */
  protected abstract void genLogFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>log10()</code> function.
   */
  protected abstract void genLog10Ftn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>sin()</code> function.
   */
  protected abstract void genSinFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>cos()</code> function.
   */
  protected abstract void genCosFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>tan()</code> function.
   */
  protected abstract void genTanFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>asin()</code> function.
   */
  protected abstract void genAsinFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>acos()</code> function.
   */
  protected abstract void genAcosFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>atan()</code> function.
   */
  protected abstract void genAtanFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>sinh()</code> function.
   */
  protected abstract void genSinhFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>cosh()</code> function.
   */
  protected abstract void genCoshFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>tanh()</code> function.
   */
  protected abstract void genTanhFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>conjg()</code> function.
   */
  protected abstract void genConjgFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>builtin_return_address()</code> function.
   */
  protected abstract void genReturnAddressFtn(int dest, int src, Type type);
  /**
   * Generate the code for the <code>builtin_fram_address()</code> function.
   */
  protected abstract void genFrameAddressFtn(int dest, int src, Type type);

  public void visitTranscendentalExpr(TranscendentalExpr e)
  {
    Type     ct  = processType(e);
    TransFtn ftn = e.getFtn();
    int      reg = registers.getResultRegister(ct.getTag());
    Expr     arg = e.getArg();
    Type     at  = processType(arg);

    if (ftn == TransFtn.Alloca) {
      genAlloca(arg, reg);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = reg;
      return;
    }

    needValue(arg);
    int src = resultReg;

    assert (resultRegMode == ResultMode.NORMAL_VALUE);

    switch (ftn) {
    case Sqrt:
      genSqrtFtn(reg, src, ct);
      break;
    case Exp:
      genExpFtn(reg, src, ct);
      break;
    case Log:
      genLogFtn(reg, src, ct);
      break;
    case Log10:
      genLog10Ftn(reg, src, ct);
      break;
    case Sin:
      genSinFtn(reg, src, ct);
      break;
    case Cos:
      genCosFtn(reg, src, ct);
      break;
    case Tan:
      genTanFtn(reg, src, ct);
      break;
    case Asin:
      genAsinFtn(reg, src, ct);
      break;
    case Acos:
      genAcosFtn(reg, src, ct);
      break;
    case Atan:
      genAtanFtn(reg, src, ct);
      break;
    case Sinh:
      genSinhFtn(reg, src, ct);
      break;
    case Cosh:
      genCoshFtn(reg, src, ct);
      break;
    case Tanh:
      genTanhFtn(reg, src, ct);
      break;
    case Conjg:
      assert processType(arg).isComplexType() : "Not complex type for conjugate intrinsic.";
      genConjgFtn(reg, src, ct);
      break;
    case ReturnAddress:
      genReturnAddressFtn(reg, src, ct);
      break;
    case FrameAddress:
      genFrameAddressFtn(reg, src, ct);
      break;
    default:
      throw new scale.common.InternalError("Unknown transcendental.");
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = reg;
  }

  public void visitTranscendental2Expr(Transcendental2Expr e)
  {
    Type ct  = processType(e);
    int  ftn = e.getFtn();
    int  reg = registers.getResultRegister(ct.getTag());

    needValue(e.getLeftArg());
    int laReg = resultReg;

    needValue(e.getRightArg());
    int raReg = resultReg;

    switch (ftn) {
    case scale.clef.expr.Transcendental2Op.cAtan2:
      genAtan2Ftn(reg, laReg, raReg, ct);
      break;
    case scale.clef.expr.Transcendental2Op.cSign:
      genSignFtn(reg, laReg, raReg, ct);
      break;
    case scale.clef.expr.Transcendental2Op.cDim:
      genDimFtn(reg, laReg, raReg, ct);
      break;
    default:
      throw new scale.common.NotImplementedError(e.getDisplayLabel());
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = reg;
  }

  public void visitCallMethodExpr(CallMethodExpr e)
  {
    visitCallFunctionExpr((CallFunctionExpr) (CallExpr) e);
  }

  public void visitComplexValueExpr(ComplexValueExpr e)
  {
    Type ct   = processType(e);
    Expr real = e.getOperand(0);
    Expr imag = e.getOperand(1);
    int  cr   = registers.getResultRegister(ct.getTag());
 
    needValue(real);
    int rr = resultReg;

    needValue(imag);
    int ir = resultReg;

    genRegToReg(rr, cr);
    genRegToReg(ir, cr + registers.numContiguousRegisters(cr));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = cr;
  }

  /**
   * Generate instructions to convert an integer value in an integer
   * register to an integer value of a different size.  The source and
   * destination may be the same register.  This logic assumes that
   * the value in the source register conforms to the specified type.
   * <p>
   * The semantics are that the register passed in as
   * <code>dest</code> is the register the caller wants used.  But,
   * the register returned as the value of the method is the actual
   * register used which may or may not be <code>dest</code>.  For
   * example, the register passed in as the source may not need to be
   * modified.  Thus, a move can be avoided if <code>src</code> is
   * used instead of the specified destination.  In some cases the
   * source may be a register that is a hard-wired zero such as $31 on
   * the alpha or %g0 on the sparc.  The caller may still need to do a
   * genRegToReg if it must have the result in the specified register.
   * Note that <code>genRegToReg</code> generates no move if the
   * source and destination registers are the same.
   * @param src is the register containing the source value
   * @param srcSize is the source value size
   * @param srcSigned is true if the source value is signed
   * @param dest is the suggested register to contain the result
   * @param destSize is the size of the result value
   * @param destSigned is true if the result value is signed
   * @return the register containing the converted value
   */
  protected abstract int convertIntRegValue(int     src,
                                            int     srcSize,
                                            boolean srcSigned,
                                            int     dest,
                                            int     destSize,
                                            boolean destSigned);
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
  protected abstract int genRealToInt(int     src,
                                      int     srcSize,
                                      int     dest,
                                      int     destSize,
                                      boolean destSigned);
  /**
   * Convert a real value in a real register to a 
   * real value in a real register.
   */
  protected abstract void genRealToReal(int src,
                                        int srcSize,
                                        int dest,
                                        int destSize);
  /**
   * Convert an integer value in an integer register to a 
   * real value in a real register.
   * @param src is the register containing the source integer value
   * @param srcSize is the size of the integer value
   * @param dest is the register that will conatin the result real
   * value
   * @param destSize is the size of the real value
   */
  protected abstract void genIntToReal(int src,
                                       int srcSize,
                                       int dest,
                                       int destSize);
  /**
   * Convert an unsigned integer value in an integer register to a 
   * real value in a real register.
   * @param src is the register containing the source integer value
   * @param srcSize is the size of the integer value
   * @param dest is the register that will conatin the result real value
   * @param destSize is the size of the real value
   */
  protected abstract void genUnsignedIntToReal(int src,
                                               int srcSize,
                                               int dest,
                                               int destSize);
  /**
   * Convert real value in a real register to an integer value
   * in a real register.  The result is rounded to the nearest integer.
   */
  protected abstract void genRealToIntRound(int src,
                                            int srcSize,
                                            int dest,
                                            int destSize);
  /**
   * Convert real value in a real register to a rounded real value in
   * a real register.  The result is rounded to the nearest integer.
   */
  protected abstract void genRoundReal(int src,
                                       int srcSize,
                                       int dest,
                                       int destSize);
  /**
   * Generate instructions to compute the floor of a real vaue in a
   * real register to a real register.
   */
  protected abstract void genFloorOfReal(int src,
                                         int srcSize,
                                         int dest,
                                         int destSize);
  /**
   * Generate code to zero out a floating point register.
   */
  protected abstract void zeroFloatRegister(int dest, int destSize);

  public void visitConversionExpr(ConversionExpr e)
  {
    CastMode cr  = e.getConversion();
    Expr     arg = e.getOperand(0);
    Type     tr  = processType(e);

    switch (cr) {
    case CAST:
      genCastConversion(arg, tr);
      return;
    case TRUNCATE:
      genTruncateConversion(arg, tr);
      return;
    case REAL:
      genRealConversion(arg, tr);
      return;
    case ROUND:
      genRoundConversion(arg, tr);
      return;
    case FLOOR:
      genFloorConversion(arg, tr);
      return;
    case IMAGINARY:
      genImagConversion(arg, tr);
      return;
    }

    throw new scale.common.NotImplementedError("Type conversion " + e);
  }

  /**
   * Generate instructions to do a cast of an address.
   * @param arg is the argument expression.
   * @param tr is the type of the result
   */
  private void genCastConversion(Expr arg, Type tr)
  {
    arg.visit(this);
  }

  /**
   * Generate instructions to do a truncate conversion.
   */
  private void genTruncateConversion(Expr arg, Type tr)
  {
    Type ta = processType(arg);
    int  sw = (ta.isRealType() ? 1 : 0) + (tr.isRealType() ? 2 : 0);

    needValue(arg);

    int        raReg   = resultReg;
    ResultMode raRegha = resultRegMode;

    assert (raRegha != ResultMode.ADDRESS) :
      "Value's address? " + arg + " " + arg.getChord();

    int destSize = tr.memorySizeAsInt(machine);
    int srcSize  = ta.memorySizeAsInt(machine);

    switch (sw) {
    case 0: // Integer result, integer argument
      if (false && !ta.isSigned() && (srcSize <= destSize)) {
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = raReg;
      } else {
        int ir = registers.getResultRegister(tr.getTag());
        resultReg = convertIntRegValue(raReg,
                                       srcSize,
                                       ta.isSigned(),
                                       ir, destSize,
                                       tr.isSigned());
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
      }
      return;

    case 1: // Integer result, real argument
      int ir = registers.getResultRegister(tr.getTag());
      if (ta.isComplexType())
        srcSize >>= 1;
      resultReg = genRealToInt(raReg, srcSize, ir, destSize, tr.isSigned());
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;

    case 3: // Real    result, real    argument
      int irf = registers.getResultRegister(tr.getTag());
      genRealToReal(raReg, srcSize, irf, destSize);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = irf;
      return;
    }
    throw new scale.common.NotImplementedError("Truncate conversion " + sw +
                                               " " +
                                               arg +
                                               " " +
                                               tr);
  }

  private void genRealConversion(Expr arg, Type tr)
  {
    Type ta = processType(arg);

    if (ta == tr) {
      arg.visit(this);
      return;
    }

    int ir = registers.getResultRegister(tr.getTag());
    needValue(arg);

    int raReg    = resultReg;
    int sw       = (ta.isRealType() ? 1 : 0) + (tr.isRealType() ? 2 : 0);
    int destSize = tr.memorySizeAsInt(machine);
    int srcSize  = ta.memorySizeAsInt(machine);

    switch (sw) {
    case 2: // Real    result, integer argument
      if (ta.isSigned())
        genIntToReal(raReg, srcSize, ir, destSize);
      else 
        genUnsignedIntToReal(raReg, srcSize, ir, destSize);

      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;

    case 3: // Real    result, real    argument
      if (ta.isComplexType()) {
        genRealToReal(raReg, srcSize / 2, ir, destSize);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }

      genRealToReal(raReg, srcSize, ir, destSize);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;

      return;
    }

    throw new scale.common.NotImplementedError("Real conversion " + arg + " " + tr);
  }

  private void genRoundConversion(Expr arg, Type tr)
  {
    Type ta = processType(arg);
    int  sw = (ta.isRealType() ? 1 : 0) + (tr.isRealType() ? 2 : 0);
    int  ir = registers.getResultRegister(tr.getTag());

    needValue(arg);
    int raReg = resultReg;

    int destSize = tr.memorySizeAsInt(machine);
    int srcSize  = ta.memorySizeAsInt(machine);

    switch (sw) {
    case 1: // Integer result, real    argument
      int ft = registers.newTempRegister(RegisterSet.FLTREG);

      genRealToIntRound(raReg, srcSize, ft, destSize);
      genRegToReg(ft, ir);

      resultReg = convertIntRegValue(ir, destSize, tr.isSigned(), ir, destSize, tr.isSigned());
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      return;
    case 3: // Real result, real argument
      genRoundReal(raReg, srcSize, ir, destSize);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    throw new scale.common.NotImplementedError("Round conversion " + arg);
  }

  private void genFloorConversion(Expr arg, Type tr)
  {
    Type ta      = processType(arg);
    int  regType = tr.getTag();
    int  dest    = registers.getResultRegister(regType);

    assert (ta.isRealType() && tr.isRealType()) : "Floor conversion " + arg;

    int destSize = tr.memorySizeAsInt(machine);
    int srcSize  = ta.memorySizeAsInt(machine);

    needValue(arg);
    int src = resultReg;
    genFloorOfReal(src, srcSize, dest, destSize);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = dest;
  }

  private void genImagConversion(Expr arg, Type tr)
  {
    Type ta = processType(arg);

    if (ta == tr) {
      arg.visit(this);
      return;
    }

    assert (ta.isComplexType() && tr.isRealType()) : "Imag conversion " + arg;

    int destSize = tr.memorySizeAsInt(machine);
    int srcSize  = ta.memorySizeAsInt(machine);
    int ir       = registers.getResultRegister(tr.getTag());

    needValue(arg);

    int ireg = resultReg + registers.numContiguousRegisters(resultReg);
    genRealToReal(ireg, srcSize / 2, ir, destSize);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  private void genComplexConversion(Expr arg, Type tr)
  {
    Type ta = processType(arg);

    if ((ta == tr) || ta.isComplexType()) {
      arg.visit(this);
      return;
    }

    assert (ta.isRealType() && tr.isRealType()) : "Complex conversion " + arg;

    int destSize = tr.memorySizeAsInt(machine);
    int srcSize  = ta.memorySizeAsInt(machine);
    int ir       = registers.getResultRegister(tr.getTag());

    needValue(arg);
    genRealToReal(resultReg, srcSize, ir, destSize / 2);
    zeroFloatRegister(ir + registers.numContiguousRegisters(ir), destSize / 2);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitDualExpr(DualExpr e)
  {
    Expr low = e.getLow();
    low.visit(this);
  }

  public void visitEqualityExpr(EqualityExpr e)
  {
    doCompareOp(e, CompareMode.EQ);
  }

  public void visitGreaterEqualExpr(GreaterEqualExpr e)
  {
    doCompareOp(e, CompareMode.GE);
  }

  public void visitGreaterExpr(GreaterExpr e)
  {
    doCompareOp(e, CompareMode.GT);
  }

  public void visitLessEqualExpr(LessEqualExpr e)
  {
    doCompareOp(e, CompareMode.LE);
  }

  public void visitLessExpr(LessExpr e)
  {
    doCompareOp(e, CompareMode.LT);
  }

  public void visitNotEqualExpr(NotEqualExpr e)
  {
    doCompareOp(e, CompareMode.NE);
  }

  public void visitLiteralExpr(LiteralExpr e)
  {
    boolean isPredicated = false;
    Chord   chord        = e.getChord();

    if (chord.isExprChord())
      isPredicated = (((ExprChord) chord).getPredicate() != null);
    
    processType(e);

    generateLiteralValue(e.getLiteral(), isPredicated);

    if (!registers.virtualRegister(resultReg))
      // Real registers get modified all the time so we can't use one
      // as the source for this literal.
      return;

    if (assignedRegister.get(resultReg))
      // This register is used to hold the value of a variable.  So,
      // this means that this variable is being set to the value of
      // this literal.  It may be set again later and so it can't be
      // used as the source for this literal.
      return;

    // Specify that the address of a variable kept in memory is now in
    // a register.

    assignedRegister.set(resultReg);
    regToLiteral.put(resultReg, e.getLiteral());
  }

  private void generateLiteralValue(Literal l, boolean isPredicated)
  {
    Type ct = l.getCoreType();
    int  bs = ct.memorySizeAsInt(machine);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;

    if (l instanceof IntLiteral) {
      IntLiteral il    = (IntLiteral) l;
      long       value = il.getLongValue();
      int        rt    = registers.tempRegisterType(RegisterSet.INTREG, bs);
      int        reg   = registers.getResultRegister(rt);
      resultReg = genLoadImmediate(value, reg);
      return;
    }

    if (l instanceof FloatLiteral) {
      FloatLiteral fl    = (FloatLiteral) l;
      int          rt    = registers.tempRegisterType(RegisterSet.FLTREG, bs);
      int          reg   = registers.getResultRegister(rt);
      resultReg = genLoadDblImmediate(fl.getDoubleValue(), reg, bs);
      return;
    }

    if (l instanceof StringLiteral) { // Save a string to be placed in a read-only data segment.
      StringLiteral sl    = (StringLiteral) l;
      String        value = sl.getStringValue();
      Displacement  disp  = defStringValue(value, bs);
      resultReg = loadMemoryAddress(disp);
      return;
    }

    if (l instanceof SizeofLiteral) {
      long value = valueOf((SizeofLiteral) l);
      int  rt    = registers.tempRegisterType(RegisterSet.INTREG, bs);
      int  reg   = registers.getResultRegister(rt);
      resultReg = genLoadImmediate(value, reg);
      return;
    }

    if (l instanceof AddressLiteral) {
      AddressLiteral il = (AddressLiteral) l;
      long           of = il.getOffset();
      Declaration    ad = il.getDecl();

      if (ad != null) {
        int rt = registers.tempRegisterType(RegisterSet.ADRREG, bs);
        int tr = registers.getResultRegister(rt);
        putAddressInRegisterNO(ad, isPredicated);
        genLoadImmediate(of + resultRegAddressOffset, resultReg, tr);
        resultReg = tr;
        return;
      }

      Literal lit = il.getValue();
      Type    dt  = lit.getType();
      Type    vt  = processType(dt);
      int     aln = vt.alignment(machine);

      long    ts  = 1;
      try {
        ts = vt.memorySize(machine);
      } catch (java.lang.Error ex) {
      }

      String       name   = un.genName();
      int          handle = allocateWithData(name, vt, ts, lit, readOnlyDataArea, true, 1, aln);
      Displacement disp   = new SymbolDisplacement(name, handle).offset(of);

      associateDispWithArea(handle, disp);
      resultReg = loadMemoryAddress(disp);
      return;
    }

    if (l instanceof BooleanLiteral) {
      BooleanLiteral bl  = (BooleanLiteral) l;
      int            rt  = registers.tempRegisterType(RegisterSet.AIREG, bs);
      int            reg = registers.getResultRegister(rt);
      resultReg = genLoadImmediate(bl.getBooleanValue() ? 1 : 0, reg);
      return;
    }

    if (l instanceof CharLiteral) {
      CharLiteral cl  = (CharLiteral) l;
      char        ch  = cl.getCharacterValue();
      int         rt = registers.tempRegisterType(RegisterSet.AIREG, bs);
      int         reg = registers.getResultRegister(rt);
      resultReg = genLoadImmediate(ch, reg);
      return;
    }

    if (l instanceof ComplexLiteral) {
      int            sz = bs >> 1;
      ComplexLiteral cl = (ComplexLiteral) l;
      double         rv = cl.getReal();
      double         iv = cl.getImaginary();
      int            cr = registers.getResultRegister(ct.getTag());
      int            nx = cr + registers.numContiguousRegisters(cr);
      int            rr = genLoadDblImmediate(rv, cr, sz);
      int            ir = genLoadDblImmediate(iv, nx, sz);

      genRegToReg(rr, cr);
      genRegToReg(ir, nx);

      resultReg = cr;
      return;
    }

    assert (l instanceof AggregationElements) : "Literal " + l;

    AggregationElements ag     = (AggregationElements) l;
    Type                agt    = processType(ag);
    String              name   = un.genName();
    int                 handle = allocateWithData(name,
                                                  agt,
                                                  agt.memorySize(machine),
                                                  ag,
                                                  readOnlyDataArea,
                                                  true,
                                                  1,
                                                  1);
    SymbolDisplacement  disp   = new SymbolDisplacement(name, handle);

    associateDispWithArea(handle, disp);
    resultRegAddressAlignment = dataAreas[disp.getHandle()].getAlignment();

    resultReg = loadMemoryAddress(disp);
    resultRegMode = ResultMode.ADDRESS;
  }

  public void visitNilExpr(NilExpr e)
  {
    Type ct = processType(e);

    resultReg = genLoadImmediate(0, registers.getResultRegister(ct.getTag()));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
  }

  /**
   * Store a value into the location specied by a literal value.
   * @param le specifies the literal
   * @param rhs specifies the value
   */
  protected void storeLiteral(LiteralExpr le,  Expr rhs)
  {
    le.visit(this);
    int adr = resultReg;
    needValue(rhs);
    int  val  = resultReg;
    Type type = le.getCoreType().getPointedTo();
    int  size = type.memorySizeAsInt(machine);

    storeIntoMemoryWithOffset(val, adr, 0, size, 1, type.isSigned());
  }

  /**
   * Store a value into the location specied by an address in a
   * variable.
   * @param lhs specifies the variable
   * @param rhs specifies the value
   */
  protected void storeLdve(LoadDeclValueExpr lhs, Expr rhs)
  {
    VariableDecl decl         = (VariableDecl) lhs.getDecl();
    Type         vt           = processType(lhs);
    Type         v2           = processType(rhs);
    int          bs           = v2.memorySizeAsInt(machine);
    boolean      isPredicated = (((ExprChord) lhs.getChord()).getPredicate() != null);

    rhs.visit(this);
    int        src    = resultReg;
    long       srcoff = resultRegAddressOffset;
    ResultMode srcha  = resultRegMode;
    int        srcaln = resultRegAddressAlignment;

    loadVariable(decl, vt, isPredicated);
    int  dest    = resultReg;
    long destoff = resultRegAddressOffset;
    int  destaln = resultRegAddressAlignment;

    if (srcha == ResultMode.ADDRESS) {
      moveWords(src, srcoff, dest, destoff, bs, (destaln < srcaln) ? destaln : srcaln);
      resultRegAddressOffset = srcoff;
      resultRegMode = srcha;
      resultReg = src;
      resultRegAddressAlignment = srcaln;
      return;
    }

    needValue(src, srcoff, srcha);
    long alignment = naln ? 1 : vt.getPointedTo().alignment(machine);
    storeIntoMemoryWithOffset(resultReg, dest, destoff, bs, alignment, v2.isRealType());
  } 

  /**
   * Store a value into the location specied by the value of an
   * expression.
   * @param lhs specifies the expression
   * @param rhs specifies the value
   */
  protected void storeLvie(LoadValueIndirectExpr lhs, Expr rhs)
  {
    Type    vt      = processType(rhs);
    int     bs      = vt.memorySizeAsInt(machine);
    Expr    adr     = lhs.getArg();

    rhs.visit(this);
    int        src    = resultReg;
    ResultMode srcha  = resultRegMode;
    long       srcoff = resultRegAddressOffset;
    int        srcaln = resultRegAddressAlignment;

    adr.visit(this);
    int        dest    = resultReg;
    ResultMode destha  = resultRegMode;
    long       destoff = resultRegAddressOffset;
    int        destaln = resultRegAddressAlignment;

    if (destha == ResultMode.ADDRESS) {
      moveWords(src, srcoff, dest, destoff, bs, (srcaln < destaln) ? srcaln : destaln);
      resultRegAddressAlignment = srcaln;
      resultRegAddressOffset = srcoff;
      resultRegMode = srcha;
      resultReg = src;
      return;
    }

    needValue(src, srcoff, srcha);
    storeIntoMemoryWithOffset(resultReg, dest, destoff, bs, destaln, vt.isRealType());
  }

  /**
   * Store a value into a variable.
   * @param lhs specifies the variable
   * @param rhs specifies the value
   */
  protected void storeLdae(LoadDeclAddressExpr lhs, Expr rhs)
  {
    VariableDecl vd           = (VariableDecl) lhs.getDecl();
    Assigned     loc          = vd.getStorageLoc();
    Type         rt           = processType(rhs);
    int          ssize        = rt.memorySizeAsInt(machine);
    Type         vt           = lhs.getCoreType().getPointedTo();
    int          dsize        = vt.memorySizeAsInt(machine);
    boolean      isReal       = vt.isRealType();
    boolean      isPredicated = (((ExprChord) lhs.getChord()).getPredicate() != null);

    if (loc == Assigned.IN_REGISTER) {
      int        dest     = vd.getValueRegister();
      ResultMode destha   = vd.valueRegMode();
      long       destoff  = 0;
      int        destSize = vd.getCoreType().memorySizeAsInt(machine);
      int        destaln  = machine.stackAlignment(vt);

      registers.setResultRegister(dest);

      needValue(rhs);
      int        src    = resultReg;    
      long       srcoff = resultRegAddressOffset;    
      ResultMode srcha  = resultRegMode;
      int        srcaln = resultRegAddressAlignment;

      if (false && (ssize > destSize)) {
        System.out.print("** sldae   ");
        System.out.println(lhs);
        System.out.print("        vt ");
        System.out.print(vt);
        System.out.print("     dsize ");
        System.out.print(vt.memorySizeAsInt(machine));
        System.out.print(" ");
        System.out.println(vt.getPointedTo());
        System.out.print("           ");
        System.out.println(vt.getPointedTo().getCoreType());
        System.out.print("       rhs ");
        System.out.println(rhs);
        System.out.print("  rt ssize ");
        System.out.print(rt.memorySizeAsInt(machine));
        System.out.print(" ");
        System.out.println(rt);
        System.out.print("           ");
        System.out.println(rt.getPointedTo());
        System.out.print("           ");
        System.out.println(rt.getPointedTo().getCoreType());
        System.out.print("        vd ");
        System.out.println(vd);
        System.out.print("  destSize ");
        System.out.print(vd.getCoreType().memorySizeAsInt(machine));
        System.out.print(" ");
        System.out.println(vd.getType());
        System.out.print("           ");
        System.out.println(vd.getType().getPointedTo());
        System.out.print("           ");
        System.out.println(vd.getType().getPointedTo().getCoreType());
      }

      assert (ssize <= destSize) || rt.isAggregateType() :
        "Size mismatch. " + ssize + " " + destSize + " " + dsize + " " + lhs.getChord();

      registers.setResultRegister(-1);

      if (srcha == ResultMode.ADDRESS) {
        if (destha == ResultMode.ADDRESS) {
          moveWords(src, srcoff, dest, destoff, ssize, srcaln);
          return;
        }
        loadFromMemoryWithOffset(dest, src, srcoff, ssize, srcaln, rt.isSigned(), false);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = dest;
        return;
      }

      if (destha == ResultMode.ADDRESS) {
        storeIntoMemoryWithOffset(src, dest, destoff, ssize, destaln, isReal);
        return;
      }

      ssize = rhs.getCoreType().memorySizeAsInt(machine);
      int tr = src;
      if (rt.isIntegerType())
        tr = convertIntRegValue(src,
                                ssize,
                                rt.isSigned(),
                                dest,
                                destSize,
                                vd.getCoreType().isSigned());

      genRegToReg(tr, dest);
      resultRegAddressOffset = 0;
      resultRegMode = srcha;
      resultReg = dest;
      return;
    }

    needValue(rhs);
    int        src    = resultReg;
    long       srcoff = resultRegAddressOffset;
    ResultMode srcha  = resultRegMode;
    int        srcaln = resultRegAddressAlignment;

    switch (loc) {
    case ON_STACK:
      Displacement offset = vd.getDisplacement();

      if (srcha == ResultMode.ADDRESS) {
        moveWords(src, srcoff, stkPtrReg, offset, ssize, srcaln);
        break;
      }

      storeIntoMemoryWithOffset(src,
                                stkPtrReg,
                                offset,
                                dsize,
                                machine.stackAlignment(vt),
                                isReal);
      break;
    case IN_MEMORY:
      Displacement disp = vd.getDisplacement().unique();

      if (srcha == ResultMode.ADDRESS) {
        int d = loadMemoryAddress(disp);
        moveWords(src, srcoff, d, 0, ssize, srcaln);
        break;
      }

      storeRegToSymbolicLocation(src, dsize, vt.alignment(machine), isReal, disp);

      break;
    case IN_COMMON:
      EquivalenceDecl ed  = (EquivalenceDecl) vd;
      Declaration     bd  = ed.getBaseVariable();
      long            off = ed.getBaseOffset();

      putAddressInRegisterNO(bd, isPredicated);
      storeIntoMemoryWithOffset(src,
                                resultReg,
                                off + resultRegAddressOffset,
                                dsize,
                                vt.alignment(machine),
                                isReal);
      break;
    default:
      throw new scale.common.InternalError("Store where ? " + loc);
    }

    resultRegAddressOffset = srcoff;
    resultRegMode = srcha;
    resultReg = src;

    inRegister.clear(vd.getTag());

    if (ssize < registers.registerSize(src))
      return;

    if (assignedRegister.get(src)) {

      // If the register is assigned to a another variable, the
      // register may be changed before the next use for this
      // variable.  So, copy the value to another register.  Hopefully
      // the copy will be eliminated if the value is not used again.

      int tr = registers.newTempRegister(registers.getType(src));
      genRegToReg(src, tr);
      src = tr;
    }

    // Since we are storing the value of a variable from a register,
    // if the variable is referenced aagain we can avoid a load by 
    // reusing the value in the reigster.  But we cannot do this for 
    // predicated expressions because the variable may not have been 
    // loaded on all predicate paths.

    if (!isPredicated)
      specifyInReg(vd, src, srcha);
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    FormalDecl  parmN        = e.getParmN();
    Type        pt           = processType(parmN);
    int         bs           = pt.memorySizeAsInt(machine);
    Expr        vaList       = e.getVaList();
    Type        vat          = processType(vaList);
    boolean     isPredicated = (((ExprChord) e.getChord()).getPredicate() != null);

    needValue(vaList);
    int  adr   = resultReg;
    long adrof = resultRegAddressOffset;

    putAddressInRegister(parmN, isPredicated);
    int ir   = resultReg;
    int size = vat.memorySizeAsInt(machine);

    genLoadImmediate(bs, ir, ir);
    storeIntoMemoryWithOffset(ir, adr, adrof, size, 0, false);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitAllocateExpr(AllocateExpr e)
  {
    whatIsThis(e);
  }

  public void visitBinaryExpr(BinaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitBranchChord(BranchChord c)
  {
    whatIsThis(c);
  }

  public void visitCallExpr(CallExpr e)
  {
    whatIsThis(e);
  }

  public void visitChord(Chord c)
  {
    whatIsThis(c);
  }

  public void visitDecisionChord(DecisionChord c)
  {
    whatIsThis(c);
  }

  public void visitExpr(Expr e)
  {
    whatIsThis(e);
  }

  public void visitExprPhiExpr(ExprPhiExpr e)
  {
    whatIsThis(e);
  }

  public void visitLoadExpr(LoadExpr e)
  {
    whatIsThis(e);
  }

  public void visitMaxExpr(MaxExpr e)
  {
    whatIsThis(e);
  }

  public void visitMinExpr(MinExpr e)
  {
    whatIsThis(e);
  }

  public void visitNaryExpr(NaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitNote(Note n)
  {
    whatIsThis(n);
  }

  public void visitMarkerChord(MarkerChord c)
  {
    /* Do Nothing */
  }

  public void visitGotoChord(GotoChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopExitChord(LoopExitChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopHeaderChord(LoopHeaderChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopPreHeaderChord(LoopPreHeaderChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopTailChord(LoopTailChord c)
  {
    /* Do Nothing */
  }

  public void visitLoopInitChord(LoopInitChord c)
  {
    /* Do Nothing */
  }

  public void visitNullChord(NullChord c)
  {
    /* Do Nothing */
  }

  public void visitPhiExpr(PhiExpr e)
  {
    whatIsThis(e);
  }

  public void visitPhiExprChord(PhiExprChord c)
  {
    whatIsThis(c);
  }

  public void visitSequentialChord(SequentialChord c)
  {
    whatIsThis(c);
  }

  public void visitEndChord(EndChord c)
  {
    whatIsThis(c);
  }

  public void visitLeaveChord(LeaveChord c)
  {
    whatIsThis(c);
  }

  public void visitSubscriptExpr(SubscriptExpr e)
  {
    whatIsThis(e);
  }

  public void visitTernaryExpr(TernaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitUnaryExpr(UnaryExpr e)
  {
    whatIsThis(e);
  }

  public void visitValueExpr(ValueExpr e)
  {
    whatIsThis(e);
  }

  public void visitVarArgExpr(VarArgExpr e)
  {
    whatIsThis(e);
  }

  public void visitVaEndExpr(VaEndExpr e)
  {
    // Do nothing.
  }

  public void visitVectorExpr(VectorExpr e)
  {
    whatIsThis(e);
  }

  public void visitConditionalExpr(ConditionalExpr e)
  {
    whatIsThis(e);
  }
}
