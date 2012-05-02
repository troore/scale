package scale.backend.trips2;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.*;

import scale.backend.*;
import scale.common.*;
import scale.callGraph.*;
import scale.clef.expr.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.score.trans.Optimization;


/**
 * This class converts Scribble into TRIPS instructions.
 * <p>
 * $Id: Trips2Generator.java,v 1.386 2007-10-31 23:47:51 bmaher Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The TRIPS memory and branch instructions use a displacement field
 * that is relative to an address in a register or to the program
 * counter.  Because this displacement is not known until load time,
 * the assembly program must specify how to generate the correct value
 * at load time.  We use an instance of a Displacement class to hold
 * this information.
 * <p>
 */

public class Trips2Generator extends scale.backend.Generator
{
  /**
   * Set true to graphically display hyperblocks.
   */
  public static boolean display = false;
  /**
   * Set true to output analysis of hyperblock cuts.
   */
  public static boolean doCutAnalysis = false;
  /**
   * Set true to output analysis of hyperblock cuts.
   */
  public static boolean doBranchIds = true;
  /**
   * Tag instructions with basic block identifiers.
   */
  public static boolean doBBID = false;
  /**
   * Set true to cause stab entries to be placed in the .til file..
   */
  public static boolean enableStabs = false;
  /**
   * Set true to generate one source line per block.
   */
  public static boolean srcLinePerBlock = false;
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
   * ELF section names.
   */
  public static final String[] areaNames = {
    "BSS",   "SBSS", "DATA",   "LIT4",
    "LIT8",  "LITA", "RCONST", "RDATA",
    "SDATA", "TEXT"};

  /**
   * Offset to the argument save area.
   */
  public static final int ARG_SAVE_OFFSET = 24;
  /**
   * Size of registers saved on the stack.
   */
  public static final int SAVED_REG_SIZE  = 8;
  /**
   * Maximum number of arguments passed in registers.
   */
  public static final int MAX_ARG_REGS    = 8;

  private static final int ASSIGN_LOAD_STORE_IDS = 0x00000001; 
  private static final int ASSIGN_BRANCH_IDS     = 0x00000010; 
  private static final int MERGE_INSTRUCTIONS    = 0x00001000;
  
  /**
   * Specify register usage for various calls to internal routines.
   */
  private static final short[] intReturn      = {Trips2RegisterSet.IR_REG, Trips2RegisterSet.RA_REG};
  private static final short[] realReturn     = {Trips2RegisterSet.FR_REG, Trips2RegisterSet.RA_REG};
  private static final short[] complexReturn  = {
    Trips2RegisterSet.FR_REG, Trips2RegisterSet.FR_REG + 1,
    Trips2RegisterSet.RA_REG};

  /**
   * Index from operation to opcode.  This is a Nx4 table where the row is
   * indexed by the operation and the column by the type.
   */
  private static final int[] binops = {
    /* 32bit */   /* 64bit */   /* 32bit-fp */  /* 64bit-fp */
    Opcodes.ADD,  Opcodes.ADD,  Opcodes.FADD,   Opcodes.FADD,
    Opcodes.SUB,  Opcodes.SUB,  Opcodes.FSUB,   Opcodes.FSUB,
    Opcodes.MUL,  Opcodes.MUL,  Opcodes.FMUL,   Opcodes.FMUL,
    Opcodes.DIVS, Opcodes.DIVS, Opcodes.FDIV,   Opcodes.FDIV,
    Opcodes.AND,  Opcodes.AND,  0x0100,         0x0100,
    Opcodes.OR,   Opcodes.OR,   0x0100,         0x0100,
    Opcodes.XOR,  Opcodes.XOR,  0x0100,         0x0100,
    Opcodes.SRA,  Opcodes.SRA,  0x0100,         0x0100,
    Opcodes.SRL,  Opcodes.SRL,  0x0100,         0x0100,
    Opcodes.SLL,  Opcodes.SLL,  0x0100,         0x0100
  };

  /**
   * If true, use a library routine to do floating point division.
   */
  private boolean softwareFDIV = false;
  
  /**
   * True if the binops operation can generate a larger result.
   */
  private boolean[] gensCarry = {
    true,  true,  true,  true,
    false, false, false, false,
    false, true};

  /**
   * Integer comparison tests.
   */
  private static final int[] itops   = {
    Opcodes.TEQ,  Opcodes.TLE, Opcodes.TLT,
    Opcodes.TGT,  Opcodes.TGE, Opcodes.TNE};
  private static final int[] iitops  = {
    Opcodes.TEQI, Opcodes.TLEI, Opcodes.TLTI,
    Opcodes.TGTI, Opcodes.TGEI, Opcodes.TNEI};
  private static final int[] iutops  = {
    Opcodes.TEQ,  Opcodes.TLEU, Opcodes.TLTU,
    Opcodes.TGTU, Opcodes.TGEU, Opcodes.TNE};
  private static final int[] iiutops = {
    Opcodes.TEQI,  Opcodes.TLEUI, Opcodes.TLTUI,
    Opcodes.TGTUI, Opcodes.TGEUI, Opcodes.TNEI};

  /**
   * Floating point comparison tests.
   */
  private static final int[] ftops  = {
    Opcodes.FEQ, Opcodes.FLE, Opcodes.FLT,
    Opcodes.FGT, Opcodes.FGE, Opcodes.FNE};

  private Hyperblock   returnBlock;               // The hyperblock with the return instruction.
  private Displacement ftnDisp;                   // Displacement for this function.
  private StackDisplacement complexDisp;          // Displacement used for returning complex values from transcendental functions.
  private Stabs        stabs;                     // Debugging information.
  protected Hyperblock hbStart;                   // The entry point to the hyperblock flow graph;
  private int          structAddress;             // Register containing structure address for routines that return structures.
  private int          structSize;                // Register containing size of structure for routines that return structures.
  private int          argBuildSize;              // Size of the area required for passing large arguments to subroutines.
  private int          localVarSize;              // Size of the area on the stack used for local variables.
  private int          savedPredicateReg;         // used by save/restorePredicate()
  private boolean      savedPredicatedOnTrue;     // used by save/restorePredicate()

  private Stack<Hyperblock>         blocksWithSpills; 
  private Vector<StackDisplacement> localVar;                  // Set of Displacements that reference variables on the stack.
  private Vector<StackDisplacement> paramVar;                  // Set of Displacements that reference parameter variables on the stack.
  private Vector<Declaration>       regVars = new Vector<Declaration>(20);
  private Hashtable<String, Vector<VariableDecl>> ftnVars = new Hashtable<String, Vector<VariableDecl>>(); // Variables declared in functions. This is for the assembler.
  
  /**
   * @param cg is the call graph to be transformed
   * @param machine specifies machine details
   * @param features controls the instructions generated
   */
  public Trips2Generator(CallGraph cg, Machine machine, int features)
  {
    super(cg, new Trips2RegisterSet(), machine, features);
    
    this.un               = new UniqueName("$$");   
    this.readOnlyDataArea = RDATA;
    this.softwareFDIV     = machine.hasCapability(Machine.HAS_NO_FP_DIVIDE);

    if (enableStabs && genDebugInfo) {
      int sty = isFortran() ? Stabs.N_SO_FORTRAN : Stabs.N_SO_C;
      this.stabs = new Stabs(machine, Stabs.FMT_GCC, sty);
    }
  }

  /**
   * Generate the machine instructions for a CFG.
   */
  public void generateScribble()
  {
    argBuildSize  = 0;
    localVarSize  = 0;
    localVar      = new Vector<StackDisplacement>(23);
    paramVar      = new Vector<StackDisplacement>(11);
    structAddress = 0;
    structSize    = 0;
    lastLabel     = null;
    returnBlock   = null;
    stkPtrReg     = usesAlloca ? Trips2RegisterSet.FP_REG : Trips2RegisterSet.SP_REG;
    ftnDisp       = new SymbolDisplacement(currentRoutine.getName(), 0);
    complexDisp   = null;

    String name = currentRoutine.getName();
    
    // Generate stab info for this routine.

    if ((stabs != null) && currentRoutine.isMain())
      stabs.addStabs(name, Stabs.N_MAIN, 0, 0, 0);

    int startStab = 0;
    if (stabs != null)
      startStab = stabs.genFtnDescriptor(currentRoutine);

    // Set up the translation of the CFG to instructions.

    ftnVars.put(name, new Vector<VariableDecl>(20)); // for the TIL writer

    BeginChord start = scribble.getBegin();

    labelCfgForBackend();

    Instruction firstInstruction = startRoutineCode();

    findLastInstruction(firstInstruction);

    layoutParameters();

    if (stabs != null) {
      Displacement startDisp = getDisp(0);
      stabs.addStabn(Stabs.N_LBRAC, 0, 1, startDisp);
      stabs.startCommon();
    }

    // Process the local variables of the routine.

    processDecls();

    if (stabs != null)
      stabs.endCommon();

    generateProlog((ProcedureType) processType(currentRoutine));

    if (genDebugInfo) {
      Label lab = createLabel();
      generateUnconditionalBranch(lab);
      appendLabel(lab);
    }

    convertCFG(start); // Generate instructions.

    if (trace)
      System.out.println("\n\nEnd code generation");
 
    hbStart = Hyperblock.enterHyperblockFlowGraph(firstInstruction, this);

    splitHyperblocks();
    
    if (doBBID)
      tagBasicBlocks();

    optimizeHyperblocks(false); 

    hyperblockFormation();

    removeDeadCode();

    setLoopSizes();

    // The register allocator uses arrays indexed by the number of
    // virtual registers.  Going in an out of SSA form increases this
    // number, which greatly increases the memory requirements of the
    // register allocator.  We rename all virtual registers to reduce
    // the memory usage of the allocator.
    
    renameRegisters();
    
    int[] map = splitAndAllocate();

    if (trace)
      System.out.println("\n\nEnd register allocation");
    
    endRoutineCode(map);  

    if (doCutAnalysis)
      analyzeHyperblocks();
    
    enterSSA();

    removeRedundantLoads();    

    assignLoadStoreIds();
    
    if (doBranchIds)
      assignBranchIds();
    
    // Load/store ids have to be assigned before peepholing.  The
    // peepholer is going to run dead code elimination and if the only
    // instruction in a predicate block is the null/store pair (which
    // hasnt been inserted yet), dead code elimination will kill the
    // instruction which defines the predicate for the predicate
    // block.

    if (!nph) 
      peepholeAfterRegisterAllocation(); 

    leaveSSA();
        
    firstInstruction = Hyperblock.leaveHyperblockFlowGraph(hbStart, this);
    
    expandPseudos(firstInstruction);
    
    if (trace)
      System.out.println("\n\nEnd routine");

    renameRegisters(firstInstruction);

    saveGeneratedCode(firstInstruction);

    if (stabs != null) {
      Label endLabel = createLabel();
      appendLabel(endLabel);

      // Generate entries to specify where the lines are in the
      // instructions.

      Label       lab = null;
      Instruction f   = firstInstruction;
      while (f != null) {
        if (f.isMarker()) {
          if (f instanceof Trips2LineMarker) {
            Trips2LineMarker lm   = (Trips2LineMarker) f;
            int              line = lm.lineNumber();
            if (lab == null)
              stabs.addStabn(Stabs.N_SLINE, 0, line, getDisp(0));
            else {
              LabelDisplacement ld = new LabelDisplacement(lab);
              DiffDisplacement  dd = new DiffDisplacement(ld, ftnDisp);
              stabs.addStabn(Stabs.N_SLINE, 0, line, dd);
            }
          } else if (f instanceof Label)
            lab = (Label) f;
        }
        f = f.getNext();
      }

      LabelDisplacement ld      = new LabelDisplacement(endLabel);
      Displacement      endDisp = new DiffDisplacement(ld, ftnDisp);
      int               endStab = stabs.addStabn(Stabs.N_RBRAC, 0, 1, endDisp);

      stabs.renumberRegisters(startStab, endStab, map, registers.numRealRegisters());
    }
  }

  /**
   * Enter SSA after register allocation.
   */
  protected void enterSSA()
  {
    Stack<Node>      wl = WorkArea.<Node>getStack("enterSSA");
    DataflowAnalysis df = new DataflowAnalysis(hbStart, registers);

    df.computeLiveness();
    
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.enterSSA();  
    }

    WorkArea.<Node>returnStack(wl);
  }

  /**
   * Leave SSA after register allocation.
   */
  protected void leaveSSA()
  {
    Stack<Node> wl = WorkArea.<Node>getStack("leaveSSA");

    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.leaveSSA(false);
    }

    WorkArea.<Node>returnStack(wl);
  }

  /**
   * Eliminate dead code.
   */
  protected void removeDeadCode()
  {    
    DataflowAnalysis df = new DataflowAnalysis(hbStart, registers);
    df.computeLiveness3();
   
    Stack<Node> wl = WorkArea.<Node>getStack("optimizeHyperblocks");
 
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
    
    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.enterSSA();  // enterSSA performs dead code elimination
      hb.analyzeLeaveSSA();
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Apply optimizations on predicated code.
   */
  protected void optimizeHyperblocks(boolean removeLoads)
  {    
    DataflowAnalysis df = new DataflowAnalysis(hbStart, registers);
    df.computeLiveness3();
   
    Stack<Node> wl = WorkArea.<Node>getStack("optimizeHyperblocks");
 
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
    
    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.optimize(removeLoads, df);
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Return the total size of a hyperblock, including fanout and spills.
   */
  private int totalSize(Hyperblock h)
  {
    return (h.getBlockSize() + h.getFanout() + h.getSpillSize());
  }

  /**
   * True if h1 dominates h2.
   */
  private boolean dominates(Hyperblock h1, Hyperblock h2, Domination dom)
  {
    Node d = h2;
    while (d != null) {
      if (d == h1)
        return true;
      d = dom.getDominatorOf(d);
    }
    return false;
  }

  /**
   * True if h is a loop header.
   */
  private boolean isLoopHeader(Hyperblock h, Domination dom)
  {
    for (int i = 0; i < h.numInEdges(); i++) {
      Hyperblock p = (Hyperblock) h.getInEdge(i);
      if (dominates(h, p, dom)) {
        return true;
      }
    }
    return false;
  }

  /**
   * True if h is a single-block loop.
   */
  private boolean oneBlockLoop(Hyperblock h)
  {
    for (int i = 0; i < h.numInEdges(); i++) {
      Hyperblock p = (Hyperblock) h.getInEdge(i);
      if (p == h)
        return true;
    }
    return false;
  }

  /**
   * Print out a reason for each pair of hyperblocks not merged.
   */
  protected void analyzeHyperblocks()
  {
    //Hyperblock.writeDotFlowGraph(hbStart, this.currentRoutine.getName());

    Domination dom = new Domination(false, hbStart);
    
    Stack<Node> wl = new Stack<Node>();
    
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
    
    try {
      PrintWriter out = new PrintWriter(new FileWriter(this.cg.getName() + ".cut", true), true);
      while (!wl.empty()) {
        Hyperblock p = (Hyperblock) wl.pop();
        p.pushOutEdges(wl);
        for (int i = 0; i < p.numOutEdges(); i++) {
          Hyperblock s = (Hyperblock) p.getOutEdge(i);
          String     w = "";
          
          if (!p.hasBranchTo(s)) {
            w = "function_call_return";
          } else if (p.hasSwitch()) {
            w = "indirect_branch";
          } else if (isLoopHeader(s, dom) && (p == s) && oneBlockLoop(s)) {
            w = "unrolling_candidate";
          } else if (isLoopHeader(s, dom) && (p != s) && oneBlockLoop(s)) {
            w = "peeling_candidate";
          } else if (isLoopHeader(s, dom) && !oneBlockLoop(s) && !dominates(s, p, dom)) {
            w = "partial_peeling";
          } else if (isLoopHeader(s, dom) && !oneBlockLoop(s) && dominates(s, p, dom)) {
            w = "multiple_block_unrolling";
          } else if (s.numInEdges() > 1) {
            w = "tail_duplication_candidate";
          } else {
            w = "if_conversion_candidate";
          }
          
          out.println(p.getBlockName() + " " + s.getBlockName() + " " + totalSize(p) + " " + totalSize(s) + " " + w);
        }
      }
    } catch (IOException e) {
    }
  }
  
  /**
   * Perform inter-block predicate minimization.
   */
  protected void minimizePredicates()
  {    
    if (!Hyperblock.enableInterBlockPredicateMinimization)
      return;
      
    DataflowAnalysis df = new DataflowAnalysis(hbStart, registers);
    df.computeLiveness3();  
   
    Stack<Node> wl = WorkArea.<Node>getStack("minimizePredicates");
 
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
    
    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.interBlockPredicateMinimization(df);
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Split blocks.
   */
  protected void splitHyperblocks()
  {
    BlockSplitter bSplitter = new BlockSplitter(this);  
    bSplitter.split(hbStart);
  }
  
  /**
   * Create hyperblocks.
   */
  protected void hyperblockFormation()
  {
    if (!scale.backend.trips2.HyperblockFormation.enableHyperblockFormation)
      return;
    
    HyperblockFormation hbf = new HyperblockFormation(this, hbStart, true);
    hbStart = hbf.createHyperblocks();
  }  

  /**
   * Update the scribble with the size of the unrolled loop.
   */
  protected void setLoopSizes()
  {
    Stack<Node> wl = WorkArea.<Node>getStack("setLoopSizes");
    
    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);      
    
      // If the HB is in an unrollable loop, record the size.
      int loopNumber = hb.getLoopNumber();
      if (loopNumber > 0) {
        int blockSize = hb.getBlockSize();
        int fanout    = hb.getFanout();
        int totalSize = blockSize + fanout;

        scribble.incrementLoopInstCount(loopNumber, totalSize);

        // Now that we have the loop size measurement, clear the loop
        // number from the hyperblock.  We do this because if the block is
        // split, when we re-analyze it we don't want to overwrite the
        // measurement.
        hb.setLoopNumber(0);
      }
    }
    
    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Tag instructions with a block ID.
   */
  protected void tagBasicBlocks()
  {
    Stack<Node> hbwl = new Stack<Node>();
    hbStart.nextVisit();
    hbStart.setVisited();
    hbwl.push(hbStart);
    
    while (!hbwl.empty()) {
      Hyperblock hb = (Hyperblock) hbwl.pop();
      hb.pushOutEdges(hbwl);
      
      PredicateBlock pbStart = hb.getFirstBlock();
      Marker m = (Marker) pbStart.getFirstInstruction();
      int li = 0;
      if (m.isLabel()) {
        li = ((Label)m).getLabelIndex();
      } 
      
      Stack<Node> pbwl = new Stack<Node>();
      pbStart.nextVisit();
      pbStart.setVisited();
      pbwl.push(pbStart);
      
      while (!pbwl.empty()) {
        PredicateBlock pb = (PredicateBlock) pbwl.pop();
        pb.pushOutEdges(pbwl);
        Instruction i = pb.getFirstInstruction();
        while (i != null) {
          i.setBBID(li);
          i = i.getNext();
        }
      }
      
    }
  }
  
  /**
   * Called at the beginning of a module.
   */
  protected void startModule()
  {
    if (stabs == null)
      return;

    String path = cg.getName();
    int    si   = path.lastIndexOf('/');
    String file = path.substring(si + 1);

    path = path.substring(0, si + 1);

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
    if (stabs == null)
      return;

    stabs.addStabn(Stabs.N_ENDM, 0, 0, 0);
  }

  public String displayGraph(String msg)
  {
    DisplayGraph visualizer = DisplayGraph.getVisualizer();
    if (visualizer == null)
      return null;

    StringBuffer buf = new StringBuffer("Hyperblock ");
    buf.append(scribble.getRoutineDecl().getName());
    buf.append(' ');
    buf.append(msg);
    String context = buf.toString();

    visualizer.newGraph(context, true);

    Stack<Hyperblock>     wl  = WorkArea.<Hyperblock>getStack("displayGraph");         
    Stack<PredicateBlock> wl2 = WorkArea.<PredicateBlock>getStack("displayGraph");

    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);
      
    while (!wl.isEmpty()) {
      Hyperblock hb  = wl.pop(); 
      int        l   = hb.numOutEdges();
      for (int i = 0; i < l; i++) {
        Hyperblock chb = (Hyperblock) hb.getOutEdge(i);
        if (!chb.visited()) {
          wl.push(chb);
          chb.setVisited();
        }
        visualizer.addEdge(hb, chb, DColor.GREEN, DEdge.SOLID, "pfg");
      }

      PredicateBlock start = hb.getFirstBlock();
      if (start == null)
        continue;

      visualizer.addEdge(hb, start, DColor.MAGENTA, DEdge.SOLID, "pb");
 
      start.nextVisit();
      start.setVisited();
      wl2.add(start);
      
      while (!wl2.isEmpty()) {
        PredicateBlock b  = wl2.pop();
        int            ll = b.numOutEdges();
        for (int j = 0; j < ll; j++) {
          PredicateBlock cb = (PredicateBlock) b.getOutEdge(j);
          if (!cb.visited()) {
            wl2.push(cb);
            cb.setVisited();
          }
          visualizer.addEdge(b, cb, DColor.MAGENTA, DEdge.SOLID, "pb");
        } 
      }
    }

    visualizer.openWindow(context, context, 0);

    WorkArea.<PredicateBlock>returnStack(wl2);
    WorkArea.<Hyperblock>returnStack(wl);

    return context;
  }

  private void displayWait(String context)
  {
    if (context == null)
      return;

    DisplayGraph visualizer = DisplayGraph.getVisualizer();
    if (visualizer == null)
      return;

    while (visualizer.windowExists(context)) {
      try {
        Thread.sleep(100);
      } catch (java.lang.Exception e) {
        System.out.println(e);
        break;
      }
    }
  }

  /**
   * Split blocks and register allocate.
   */
  protected int[] splitAndAllocate()
  {
    blocksWithSpills = WorkArea.<Hyperblock>getStack("splitAndAllocate");

    if (display && currentRoutine.getName().equals(Debug.getReportName()))
      displayWait(displayGraph("before"));

    Trips2Allocator registerAllocator = Trips2AllocatorHybrid.enabled ?
      new Trips2AllocatorHybrid(this, hbStart, trace) :
      new Trips2Allocator(this, hbStart, trace); 

    for (int i = 0; i < 20; i++) {
      boolean       split     = false;
      BlockSplitter bSplitter = new BlockSplitter(this);
      int[]         map       = registerAllocator.allocate();
		  
      if (!blocksWithSpills.isEmpty())
        split = bSplitter.splitBlocksWithSpills(hbStart, blocksWithSpills);
      
      if (display && currentRoutine.getName().equals(Debug.getReportName()))
        displayWait(displayGraph("Nodeafter"));

      if (!split) { // We are done.
        registerAllocator.computeStats(currentRoutine.getName(), i);
        remapRegisters(map);
        WorkArea.<Hyperblock>returnStack(blocksWithSpills);
        return map;
      }
		
      blocksWithSpills.clear();
    }

    if (display && currentRoutine.getName().equals(Debug.getReportName()))
      displayWait(displayGraph("failed"));

    throw new scale.common.InternalError("Too many attempts trying to split blocks and register allocate: " +
                                         currentRoutine.getName());
  }

  /**
   * Rename registers using the register map.
   */
  private void remapRegisters(int[] map)
  {
    Stack<Node> hwl = WorkArea.<Node>getStack("remapRegisters");
    Stack<Node> wl  = WorkArea.<Node>getStack("remapRegisters");

    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);
    
    while (!hwl.isEmpty()) {
      Hyperblock hb = (Hyperblock) hwl.pop();
      hb.pushOutEdges(hwl);
   
      PredicateBlock start = hb.getFirstBlock();
      start.nextVisit();
      start.setVisited();
      wl.add(start);
      
      while (!wl.isEmpty()) {
        PredicateBlock block = (PredicateBlock) wl.pop();        
        block.pushOutEdges(wl);
        
        for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext()) 
          inst.remapRegisters(map);
        
        // Rename the predicate for the predicate block.  Dead code
        // elimination does not maintain the PFG.  It's possible to
        // have a block that's empty whose predicate has been killed.
        // See gcc torture test 960416-1.c
        
        if (block.isPredicated()) {       
          int rp  = block.getPredicate();
          if (rp < map.length) {
            int nrp = map[rp];
            if (nrp != -1)  
              block.setPredicate(nrp);
          }
        }
      }
       
      hb.determinePredicatesBranches();
    }

    WorkArea.<Node>returnStack(hwl);
    WorkArea.<Node>returnStack(wl);

    remapDeclInfo(map);
  }

  /**
   * Rename registers to reduce the memory used during register allocation.
   */
  protected void renameRegisters()
  {
    int nr = registers.numRegisters();
    if (nr < 2000)  // Only rename if the number of registers is greater than this.
      return;
      
    int   nrr              = registers.numRealRegisters();
    int[] map              = new int[nr];
    int   nextTempRegister = nrr;
    
    for (int i = 0; i < nrr; i++)
      map[i] = i;
    
    for (int i = nrr; i < nr; i++)
      map[i] = -1;
    
    // Create the rename map.
    
    Stack<Node> wl  = WorkArea.<Node>getStack("renameRegisters");
    Stack<Node> hwl = WorkArea.<Node>getStack("renameRegisters");
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);
    
    while (!hwl.isEmpty()) {
      Hyperblock hb = (Hyperblock) hwl.pop();      
      hb.pushOutEdges(hwl);
      
      PredicateBlock start = hb.getFirstBlock();
      start.nextVisit();
      start.setVisited();
      wl.add(start);

      while (!wl.isEmpty()) {
        PredicateBlock block = (PredicateBlock) wl.pop();        
        block.pushOutEdges(wl);        
    
        for (Instruction inst = block.getFirstInstruction(); inst != null; inst = inst.getNext()) {
          if (inst.isLabel())
            continue;

          int[] srcs = inst.getSrcRegisters();
          if (srcs != null) {
            for (int i = 0; i < srcs.length; i++) {
              int rb  = srcs[i];
              int reg = map[rb];
              if (reg < 0) 
                map[rb] = nextTempRegister++;
            }
          }
          
          int ra = inst.getDestRegister();
          if (ra != -1) {
            int reg = map[ra];
            if (reg < 0)
              map[ra] = nextTempRegister++;
          }
        }
      }  
    }

    WorkArea.<Node>returnStack(wl);
    WorkArea.<Node>returnStack(hwl);
   
    remapRegisters(map);  // Rename all the virtual registers.
    
    ((Trips2RegisterSet) registers).setRegisters(map, nextTempRegister);
  }
  
  /**
   * Rename temporary registers so each block starts at 0.  
   * <br> 
   * Only call this after SSA. This routine assumes all the registers are
   * named properly.
   */
  protected void renameRegisters(Instruction first)
  { 
    int[] rmap              = new int[registers.numRegisters()];
    int   firstTempRegister = registers.newTempRegister(RegisterSet.AFIREG);
    int   nextTempRegister  = firstTempRegister;
    
    ((BeginMarker) first).setLowTmpReg(firstTempRegister);
        
    for (int i = 0; i < rmap.length; i++)
      rmap[i] = -1;

    for (Instruction inst = first; inst != null; inst = inst.getNext()) {
      if (inst.isLabel()) {
        for (int i = 0; i < rmap.length; i++)
          rmap[i] = -1;
        nextTempRegister = firstTempRegister;
        continue;
      }
      
      int opcode = inst.getOpcode();
      
      if (opcode != Opcodes.READ) {
        int[] srcs = inst.getSrcRegisters();
        if (srcs != null) {
          for (int i = 0; i < srcs.length; i++) {
            int rb  = srcs[i];            
            int reg = rmap[rb]; 
            inst.remapSrcRegister(rb, reg);
          }
        }
      }
      
      if (opcode != Opcodes.WRITE) {
        int ra = inst.getDestRegister();
        if (ra != -1) {        
          int reg = rmap[ra];
          if (reg < 0) {
            reg = nextTempRegister++;
            rmap[ra] = reg;
          }
        
          inst.remapDestRegister(ra, reg);
        }
      }
    }
  }
  
  /**
   * Do peephole optimizations after registers are allocated.
   */
  protected void peepholeAfterRegisterAllocation()
  {    
    // TODO: the mov-mov pattern is never safe after hyperblock
    // formation, because it can increase fanout (hence the "false"
    // argument below).  The optimization should be fanout-aware if it
    // is reenabled.
    Peepholer ph = new Peepholer(hbStart, false);
    ph.peephole();
  }
  
  /**
   * Perform optimizations on predicated code.
   */
  private void optimizePredicates(int opts)
  {    
    Stack<Node> hwl = WorkArea.<Node>getStack("optimize");
    
    hbStart.nextVisit();
    hbStart.setVisited();
    hwl.push(hbStart);
    
    while (!hwl.isEmpty()) {
      Hyperblock hb = (Hyperblock) hwl.pop(); 
      hb.pushOutEdges(hwl);   
      
      if ((opts & ASSIGN_LOAD_STORE_IDS) != 0)
        hb.assignLoadStoreQueueIds();      
      
      if ((opts & MERGE_INSTRUCTIONS) != 0)
        hb.mergeInstructions(true);   
      
      if ((opts & ASSIGN_BRANCH_IDS) != 0) {
        hb.assignBranchIds(false); 
      }
    }
    
    WorkArea.<Node>returnStack(hwl);
  }

  /**
   * Remove redundant loads
   */
  protected void removeRedundantLoads()
  {
    Stack<Node> wl = WorkArea.<Node>getStack("removeRedundantLoads");

    hbStart.nextVisit();
    hbStart.setVisited();
    wl.push(hbStart);

    while (!wl.isEmpty()) {
      Hyperblock hb = (Hyperblock) wl.pop();
      hb.pushOutEdges(wl);
      hb.removeRedundantLoads();
    }

    WorkArea.<Node>returnStack(wl);
  }
  
  /**
   * Insert nullification and assign load/store ids.
   */
  protected void mergeInstructions()
  {
    optimizePredicates(MERGE_INSTRUCTIONS);
  }
  
  /**
   * Insert nullification and assign load/store ids.
   */
  protected void assignLoadStoreIds()
  {
    optimizePredicates(ASSIGN_LOAD_STORE_IDS);
  }
  
  protected void assignBranchIds()
  { 
    optimizePredicates(ASSIGN_BRANCH_IDS);
  }
  
  /**
   * Output all instructions for debugging.
   */
  protected void dumpAssembly(Instruction first, String phase)
  {
    System.out.println("\n*** " + phase + " ***\n");
    for (Instruction inst = first; inst != null; inst = inst.getNext()) {
      if (inst.isLabel())
        System.out.println("");
      System.out.println(inst);
    }
  }
  
  /**
   * Set the return hyperblock.
   */
  protected void setReturnBlock(Hyperblock returnBlock)
  {
    this.returnBlock = returnBlock;
  }
  
  /**
   * Return the return hyperblock.
   */
  protected Hyperblock getReturnBlock()
  {
    return returnBlock;
  }
  
  /**
   * Set the last instruction.
   */
  protected void setLastInstruction(Instruction lastInstruction)
  {
    this.lastInstruction = lastInstruction;
  }
  
  /**
   * Set the BeginMarker.
   */
  protected void setBeginMarker(BeginMarker currentBeginMarker)
  {
    this.currentBeginMarker = currentBeginMarker;
  }

  /**
   * Expand pseudo-ops to real TRIPS instructions.
   */
  private void expandPseudos(Instruction first)
  {
    for (Instruction inst = first, last = null; inst != null; last = inst, inst = inst.getNext()) {
      int bbid = inst.getBBID();
      if (inst.isStore()) {
        StoreInstruction pseudo = (StoreInstruction) inst;
        Displacement     sd     = pseudo.getDisp();
        long             imm    = (sd != null) ? sd.getDisplacement() : pseudo.getImm();
         
        if (!Trips2Machine.isImmediate(imm)) {
          int     ra    = registers.newTempRegister(RegisterSet.AFIREG);
          int     rb    = pseudo.getRb();
          int     simm  = (imm < 0) ? Trips2Machine.minImmediate : Trips2Machine.maxImmediate;
          long    rimm  = ((imm < 0) ?
                           (imm - Trips2Machine.minImmediate) :
                           (imm - Trips2Machine.maxImmediate));   
          
          // Split the immediate between the store and the addi, this
          // will allow us to address a wider range without using an
          // enter

          ImmediateInstruction ii = new ImmediateInstruction(Opcodes.ADDI, ra, rb, rimm);
          pseudo.setRb(ra);
          ii.setBBID(bbid);
 
          if (sd != null) {
            // Load and store instructions for spill code, share the
            // same displacement.  If we adjust the displacement here,
            // we risk adjusting it in the load also.  Just clear the
            // displacement field and set the immediate.
 
            pseudo.setDisp(null);
          }
          
          pseudo.setImm(simm);
          last.setNext(ii);
          ii.setNext(inst);
          inst = ii; // Purposely set to ADDI, we may need to expand the immediate
        }
      } else if (inst.isLoad()) {
        LoadInstruction pseudo = (LoadInstruction) inst;
        Displacement    ld     = pseudo.getDisp();
        long            imm    = (ld != null) ? ld.getDisplacement() : pseudo.getImm();
                
        if (!Trips2Machine.isImmediate(imm)) {
          int     rb    = pseudo.getRb();
          int     rc    = registers.newTempRegister(RegisterSet.AFIREG);
          int     limm  = (imm < 0) ? Trips2Machine.minImmediate : Trips2Machine.maxImmediate;
          long    rimm  = ((imm < 0) ?
                           (imm - Trips2Machine.minImmediate) :
                           (imm - Trips2Machine.maxImmediate));
          
          // Split the immediate between the load and the addi, this will allow
          // us to address a wider range without using an enter
          
          ImmediateInstruction ii = new ImmediateInstruction(Opcodes.ADDI, rc, rb, rimm);
          pseudo.setRb(rc);
          ii.setBBID(bbid);
          
          if (ld != null) {
            // Load and store instructions for spill code, share the
            // same displacement.  If we adjust the displacement here,
            // we risk adjusting it in the load also.  Just clear the
            // displacement field and set the immediate.
 
            pseudo.setDisp(null);
          }
          
          pseudo.setImm(limm);
          last.setNext(ii);
          ii.setNext(pseudo);
          inst = ii; // Purposely set to ADDI, we may need to expand the immediate
        }
      } 
      
      // We want to fall through here. We may have introduced an
      // immediate during load/store expansion above.
      
      if (inst instanceof ImmediateInstruction) {
        ImmediateInstruction pseudo = (ImmediateInstruction) inst;
        long                 imm    = ((pseudo.disp != null) ?
                                       pseudo.disp.getDisplacement() :
                                       pseudo.getImm());

        if (!Trips2Machine.isImmediate(imm)) {
          int              op    = pseudo.getOpcode();
          int              newop = Opcodes.getIntOp(op);
          int              ra    = pseudo.getRa();
          int              rb    = pseudo.getRb();
          int              rc    = registers.newTempRegister(RegisterSet.AFIREG);
          EnterInstruction ent   = new EnterInstruction(Opcodes._ENTER,
                                                        rc,
                                                        new IntegerDisplacement(imm));
          GeneralInstruction ni    = null;
          
          if (newop == Opcodes.MOV)
            ni = new GeneralInstruction(newop, ra, rc);
          else
            ni = new GeneralInstruction(newop, ra, rb, rc);
          
          ent.setBBID(bbid);
          ni.setBBID(bbid);

          if (pseudo.definesPredicate())
            ni.setDefinesPredicate();
          if (pseudo.isPredicated())
            ni.setPredicates(pseudo.getPredicates(), pseudo.isPredicatedOnTrue());
          
          last.setNext(ent);
          ent.setNext(ni);
          ni.setNext(inst.getNext());
          inst = ni;
        } else if (imm == 0) {
          int op = pseudo.getOpcode();

          if ((op == Opcodes.ADDI) || (op == Opcodes.SUBI) || (op == Opcodes.SLLI)) {
            int                ra = pseudo.getRa();
            int                rb = pseudo.getRb();
            GeneralInstruction ni = new GeneralInstruction(Opcodes.MOV, ra, rb);
            
            if (pseudo.isPredicated())
              ni.setPredicates(pseudo.getPredicates(), pseudo.isPredicatedOnTrue());
            
            ni.setBBID(bbid);
            ni.setNext(inst.getNext());
            last.setNext(ni);
            inst = ni;
          }
        } else if (imm == 1) {
          int op = pseudo.getOpcode();

          if ((op == Opcodes.MULI) || (op == Opcodes.DIVUI) || (op == Opcodes.DIVSI)) {
            int                ra = pseudo.getRa();
            int                rb = pseudo.getRb();
            GeneralInstruction ni = new GeneralInstruction(Opcodes.MOV, ra, rb);
            
            if (pseudo.isPredicated())
              ni.setPredicates(pseudo.getPredicates(), pseudo.isPredicatedOnTrue());
            
            ni.setBBID(bbid);
            ni.setNext(inst.getNext());
            last.setNext(ni);
            inst = ni;
          }
        }
      } else if (inst instanceof EnterInstruction) {
        EnterInstruction pseudo = (EnterInstruction) inst;
        Displacement     disp   = pseudo.getDisp();

        if (disp.isNumeric()) {
          long imm = disp.getDisplacement();
          
          if (Trips2Machine.isImmediate(imm)) {
            int                  dest  = pseudo.getDestRegister();
            ImmediateInstruction ni    = new ImmediateInstruction(Opcodes.MOVI, dest, imm);
            
            if (pseudo.isPredicated()) {
              ni.setPredicates(pseudo.getPredicates(), pseudo.isPredicatedOnTrue());
              if (pseudo.definesPredicate())
                ni.setDefinesPredicate();
            }
            
            ni.setBBID(bbid);
            ni.setNext(inst.getNext());
            last.setNext(ni);
            inst = ni;
            continue;
          }
        }

        if (pseudo.isPredicated()) {
          int                dest  = pseudo.getDestRegister();
          int                ir    = registers.newTempRegister(RegisterSet.AFIREG);
          GeneralInstruction gi    = new GeneralInstruction(Opcodes.MOV, dest, ir);
          
          // Do not predicate enter instructions, the scheduler will
          // try to expand them into a predicated genu instruction
          // which is not allowed - Aaron

          gi.setBBID(bbid);
          gi.setPredicates(pseudo.getPredicates(), pseudo.isPredicatedOnTrue());
          pseudo.removePredicates();
          pseudo.ra = ir;
          inst      = insertInstruction(gi, pseudo);
        }
      }
    }
  }
 
  /**
   * The difference between this routine and the one in Generator is
   * this routine will add the predicate to an instruction if it is
   * not already predicated.
   */
  protected void appendInstruction(Instruction inst)
  {
    super.appendInstruction(inst);
    if ((predicateReg > -1) && !inst.isPredicated())
      inst.setPredicate(predicateReg, predicatedOnTrue);
  }
  
  /**
   * Return true if an unconditional branch is needed.
   * For the Trips architecture, every block must end in a branch.
   * @param c is the current node
   * @param nxt is the successor to the current node
   * @param actual is the actual node that will be converted to
   * assembly next
   */
  protected boolean unconditionalBranchNeeded(Chord c, Chord nxt, Object actual)
  {
    // If current node is a function call whose return value is
    // assigned (e.g s= call();) AND the successor node has multiple
    // in edges, we needed to insert a block to handle the assignment
    // and therefore need a branch.  In all other cases where it is a
    // function call we do not.

    if ((c != null) && c.getCall(false) != null)
      return (c.isAssignChord() && (nxt.numInCfgEdges() != 1));

    if (actual != nxt)
      return true;

    if (nxt.getLabel() > 0) // If we just finished a block.
      return true;

    return false;
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
      emit.emit(";# ");
      emit.emit(comments.nextElement());
      emit.endLine();
    }
    Trips2Assembler asm = new Trips2Assembler(this, source);
    asm.assemble(emit, dataAreas);
    if (stabs != null)
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
   * @param regha specifies whether the register contains the address
   * of the variable
   */
  protected void defineDeclInRegister(Declaration decl, int register, ResultMode regha)
  {
    regVars.addElement(decl);

    super.defineDeclInRegister(decl, register, regha);
    if (stabs != null)
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
    Type type = decl.getCoreType();

    super.defineDeclOnStack(decl, disp);
    if (stabs != null)
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
    if (stabs != null)
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
    if (stabs != null)
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
    if ((stabs != null) && (rd.getScribbleCFG() == null))
      stabs.defineRoutineInfo(rd, disp);
  }

  public Type processType(Declaration decl)
  {
    return super.processType(decl);
  }
  
  protected void assignDeclToMemory(String name, VariableDecl vd)
  {
    addVariableDecl(vd);        // for the TIL writer

    Type       dt         = vd.getType();
    boolean    readOnly   = dt.isConst();
    Type       vt         = processType(dt);
    Expression init       = vd.getInitialValue();
    Visibility visibility = vd.visibility();

    int  aln = vd.isCommonBaseVar() ? machine.generalAlignment() : dt.alignment(machine);
    long ts  = 1;

    try {
      ts = vt.memorySize(machine);
    } catch (java.lang.Error ex) {
    }

    // Place everything on word boundaries.

    if ((ts < 4) && vt.isIntegerType())
      ts = 4;
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
    addVariableDecl(vd);        // for the TIL writer

    Type dt = vd.getType();
    Type vt = processType(dt);

    if (vt.isAggregateType()) {
      int  regd = registers.newTempRegister(vt.getTag());
      defineDeclInRegister(vd, regd, ResultMode.STRUCT_VALUE);
      return;
    }

    int regd = registers.newTempRegister(vt.getTag());
    defineDeclInRegister(vd, regd, ResultMode.NORMAL_VALUE);
    return;

  }

  protected void assignDeclToStack(VariableDecl vd)
  {
    addVariableDecl(vd);        // for the TIL writer

    Type              dt    = vd.getType();
    Type              vt    = processType(dt);
    long              sts   = vt.memorySize(machine);
    StackDisplacement sdisp = new StackDisplacement(localVarSize);

    defineDeclOnStack(vd, sdisp);
    localVarSize += Machine.alignTo(sts, SAVED_REG_SIZE);
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

  protected void processTypeDecl(TypeDecl td, boolean complete)
  {
    if (complete && (stabs != null))
      stabs.genTypeDecl(td);
  }

  protected void processTypeName(TypeName tn)
  {
    if (stabs != null)
      stabs.genTypeName(tn);
  }

  /**
   * Assign the routine's arguments to registers or the stack.  Callee side.
   */
  protected void layoutParameters()
  {
    ProcedureType pt      = (ProcedureType) processType(currentRoutine);
    int           nextArg = 0;
    Type          rt      = processType(pt.getReturnType());

    if (!(rt.isAtomicType() || rt.isVoidType()))
      nextArg++;

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);

      addVariableDecl(fd);      // for the TIL writer

      if (fd instanceof UnknownFormals)
        break;

      boolean volat = fd.getType().isVolatile();
      Type    vt    = processType(fd);
      int     size  = vt.memorySizeAsInt(machine);
      int     nxtr  = nextArg + Trips2RegisterSet.IF_REG;

      if (vt.isAtomicType()) {

        // The first MAX_ARG_REGS scaler arguments are in the argument
        // registers, remaining words have already been placed on the
        // stack by the caller.

        if (usesVaStart || genDebugInfo) { // Argument must be saved on the stack.
          int               loc  = nextArg * SAVED_REG_SIZE;
          StackDisplacement disp = new StackDisplacement(loc + SAVED_REG_SIZE - size);

          if (stabs != null)
            stabs.defineParameterOnStack(fd, disp);

          super.defineDeclOnStack(fd, disp);
          paramVar.addElement(disp);
          nextArg++;
          continue;
        }

        if (nextArg >= MAX_ARG_REGS) { // Argument is on the stack.
          int               loc  = nextArg * SAVED_REG_SIZE;
          StackDisplacement disp = new StackDisplacement(loc + SAVED_REG_SIZE - size);
          if (stabs != null)
            stabs.defineParameterOnStack(fd, disp);
          defineDeclOnStack(fd, disp);
          paramVar.addElement(disp);
          nextArg++;
          continue;
        }

        if (fd.addressTaken() || volat) { // Argument value will be transferred to the stack.
          StackDisplacement disp = new StackDisplacement(localVarSize);
          if (stabs != null)
            stabs.defineParameterInRegister(fd, nxtr);
          defineDeclOnStack(fd, disp);
          localVarSize += Machine.alignTo(size, Trips2RegisterSet.IREG_SIZE);
          localVar.addElement(disp); // This stack offset will be modified later.
          nextArg++;
          continue;
        }

        // Place argument in register.

        int nr = registers.newTempRegister(vt.getTag());

        if (stabs != null)
          stabs.defineParameterInRegister(fd, nxtr);

        defineDeclInRegister(fd, nr, ResultMode.NORMAL_VALUE);
        nextArg++;
        continue;
      }

      AggregateType at = vt.returnAggregateType();
      if (at != null) {

        // The first eight words of a struct are in the argument
        // registers, remaining words have already been placed on the
        // stack by the caller.

        int loc = nextArg * SAVED_REG_SIZE;
        int inc = (size + Trips2RegisterSet.IREG_SIZE - 1) / Trips2RegisterSet.IREG_SIZE;
      
        if (!usesVaStart &&
            !useMemory &&
            !fd.addressTaken() &&
            ((nextArg + inc) <= MAX_ARG_REGS) &&
            machine.keepTypeInRegister(at, true)) {
          int regsz = ((size > 8) ? RegisterSet.DBLEREG : 0);
          int regd  = registers.newTempRegister(RegisterSet.INTREG + regsz);
          defineDeclInRegister(fd, regd, ResultMode.STRUCT_VALUE);
        } else {
          if (size < SAVED_REG_SIZE)
            loc += SAVED_REG_SIZE - size - addIn[size];
          StackDisplacement  disp = new StackDisplacement(loc);
          if (stabs != null) {
            if (nextArg >= MAX_ARG_REGS) // Argument is on the stack.
              stabs.defineParameterOnStack(fd, disp);
            else
              stabs.defineParameterInRegister(fd, nxtr);
          }
          defineDeclOnStack(fd, disp);
          paramVar.addElement(disp);
        }

        nextArg += inc;
        continue;
      }

      throw new scale.common.InternalError("Parameter type " + fd);
    }
  }

  /**
   * Return the register used to return the function value.
   * @param regType specifies the type of value
   * @param isCall is true if the calling routine is asking
   */
  public final int returnRegister(int regType, boolean isCall)
  {
    return Trips2RegisterSet.IR_REG;
  }

  /**
   * Return the register used as the first argument in a function
   * call.
   * @param regType specifies the type of argument value
   */
  public final int getFirstArgRegister(int regType)
  {
    return Trips2RegisterSet.IF_REG;
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

    for (int i = 0; i < num; i++)
      v[i] = def;

    for (int i = 0; i < entries.length - 1; i++) {
      int in = (int) (indexes[i] - min);
      if (in >= 0)
        v[in] = getBranchLabel(entries[i]);
    }

    String       name   = un.genName();
    int          handle = allocateData(name,
                                       RDATA,
                                       SpaceAllocation.DAT_ADDRESS,
                                       8 * num,
                                       true,
                                       v,
                                       1,
                                       8);
    Displacement disp   = new SymbolDisplacement(name, handle);
    associateDispWithArea(handle, disp);

    return disp;
  }

  /**
   * Create a new Label.
   */
  protected Label createNewLabel()
  {
    return new TripsLabel(currentRoutine);
  }

  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnter(Displacement disp, int dest)
  {
    appendInstruction(new EnterInstruction(Opcodes._ENTER, dest, disp));
    return dest;
  }

  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnterA(Displacement disp)
  {
    int dest = registers.newTempRegister(RegisterSet.AFIREG);
    appendInstruction(new EnterInstruction(Opcodes._ENTERA, dest, disp));
    return dest;
  }
  
  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnterA(Displacement disp, int rp, boolean onTrue)
  {
    int dest = registers.newTempRegister(RegisterSet.AFIREG);
    appendInstruction(new EnterInstruction(Opcodes._ENTERA, dest, disp, rp, onTrue));
    return dest;
  }
  
  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnterB(Displacement disp, int dest)
  {
    appendInstruction(new EnterInstruction(Opcodes._ENTERB, dest, disp));
    return dest;
  }
  
  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnterB(Displacement disp)
  {
    int dest = registers.newTempRegister(RegisterSet.AFIREG);
    return generateEnterB(disp, dest);
  }
  
  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnter(Displacement disp)
  {
    int dest = registers.newTempRegister(RegisterSet.AFIREG);
    return generateEnter(disp, dest);
  }

  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnter(long value, int dest)
  {
    int x = (int) value;

    Displacement disp;
    if (value == x)
      disp = getDisp(x);
    else
      disp = new IntegerDisplacement(value);
    
    return generateEnter(disp, dest);
  }

  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnter(long value)
  {
    int dest = registers.newTempRegister(RegisterSet.AFIREG);
    return generateEnter(value, dest);
  }

  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnter(int value)
  {
    Displacement disp = getDisp(value);
    return generateEnter(disp);
  }

  /**
   * Generate an enter instruction that will be expanded later.
   */
  private EnterInstruction generateEnterEndRoutine(long value, int dest)
  {
    int x = (int) value;

    Displacement disp;
    if (value == x)
      disp = getDisp(x);
    else
      disp = new IntegerDisplacement(value);
    
    return new EnterInstruction(Opcodes._ENTER, dest, disp);
  }
  
  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnter(double value, int dest, int destSize)
  {
    // If the program uses a float value, we want to get the double value
    // that most closely matches what the float value would be.
    if (destSize < 8)
      value = (double) ((float) value);

    Displacement disp = new FloatDisplacement(value);
    return generateEnter(disp, dest);
  }

  /**
   * Generates an enter instruction that will be expanded later.
   */
  private int generateEnter(double value)
  {
    Displacement disp = new FloatDisplacement(value);
    return generateEnter(disp);
  }

  /**
   * Generate instructions to move data from one register to another.
   */
  protected void genRegToReg(int src, int dest)
  {
    assert (src >= 0) : "Negative source register " + src + " to " + dest;

    if (src == dest)
       return;
      
    int srcSize = registers.registerSize(src);

    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, src));
    if (srcSize > Trips2RegisterSet.IREG_SIZE)
      appendInstruction(new GeneralInstruction(Opcodes.MOV, dest + 1, src + 1));

    if (registers.pairRegister(src))
      appendInstruction(new GeneralInstruction(Opcodes.MOV, dest + 1, src + 1));
  }

  /**
   * Generate an add of address registers <code>laReg</code> and
   * <code>raReg</code>.
   */
  protected void addRegs(int laReg, int raReg, int dest)
  {
    appendInstruction(new GeneralInstruction(Opcodes.ADD, dest, laReg, raReg));
  }

  /**
   * Load an address of a memory location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement
   * or offset of one)
   * @return the register that is set with the address
   */
  protected int loadMemoryAddress(Displacement disp)
  {
    return generateEnterA(disp);
  }

  /**
   * Load an address of a stack location into a register.
   * @param disp specifies the address (should be a SymbolDisplacement
   * or offset of one)
   * @return the register that is set with the address
   */
  protected int loadStackAddress(Displacement disp)
  {
    int dest = registers.newTempRegister(RegisterSet.ADRREG);
    appendInstruction(new ImmediateInstruction(Opcodes.ADDI, dest, stkPtrReg, disp));
    return dest;
  }

  /**
   * Generate instructions to load an immediate integer value added to
   * the value in a register into a register.
   * @param value is the value to load
   * @param base is the base register
   * @param dest is the register conatining the result
   */
  protected void genLoadImmediate(long value, int base, int dest)
  {
    // values which are too large will be expanded by ExpandPseudos.expandAll()
    appendInstruction(new ImmediateInstruction(Opcodes.ADDI, dest, base, value));
  }

  /**
   * Generate instructions to calculate a valid offset.  The
   * *resultReg* register is set to the register containing the
   * address to be used and the remaining offset is returned.
   * @param value is the value to add to the base address
   * @param base is the base address
   * @return the lower 16 bits of the constant
   */
  protected long genLoadHighImmediate(long value, int base)
  {
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = base;
    return value;
  }

  /**
   * Generate instructions to load an immediate integer value into a
   * register.
   * @param value is the value to load
   * @param dest is the register conatining the result
   * @return the register containing the value
   */
  protected int genLoadImmediate(long value, int dest)
  {
    return generateEnter(value, dest);
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
   * @return the register containing the value
   */
  protected int genLoadDblImmediate(double value, int dest, int destSize)
  {
    return generateEnter(value, dest, destSize);
  }

  /**
   * Called after the last CFG node in a basic block is processed.
   */
  protected void basicBlockEnd()
  {
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
    int               ts   = type.memorySizeAsInt(machine);
    StackDisplacement disp = new StackDisplacement(localVarSize);

    localVarSize += Machine.alignTo(ts, SAVED_REG_SIZE);
    localVar.addElement(disp); // This stack offset will be modified later.

    if (ts < 8) // A full word store is faster.
      ts = 8;

    appendInstruction(new ImmediateInstruction(Opcodes.ADDI, adrReg, stkPtrReg, disp));    
    return ts;
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

      long off = 0;

    if (offset.isStack()) {
      int adr = registers.newTempRegister(RegisterSet.ADRREG);
      appendInstruction(new ImmediateInstruction(Opcodes.ADDI, adr, address, offset));
      address = adr;
    } else {
      off = offset.getDisplacement();
    }

    loadFromMemoryWithOffset(dest, address, off, size, alignment, signed, real);
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real
   */
  protected void loadFromMemoryWithOffset(int     dest,
                                          int     address,
                                          long    offset,
                                          int     size,
                                          long    alignment,
                                          boolean signed,
                                          boolean real)
  {
    if (registers.pairRegister(dest)) {
      int s = size / 2;
      assert (s <= Trips2RegisterSet.IREG_SIZE) : "Paired register size " + size;
      loadFromMemoryWithOffsetX(dest + 0, address, offset,     s, alignment, signed, real);
      loadFromMemoryWithOffsetX(dest + 1, address, offset + s, s, alignment, signed, real);
      return;
    }

    if (size <= Trips2RegisterSet.IREG_SIZE) {
      loadFromMemoryWithOffsetX(dest, address, offset, size, alignment, signed, real);
      return;
    }

    // Structs

    while (true) {
      int s = size;
      if (s > Trips2RegisterSet.IREG_SIZE)
        s = Trips2RegisterSet.IREG_SIZE;
      loadFromMemoryWithOffsetX(dest, address, offset, s, alignment, signed, real);
      dest++;
      size -= s;
      if (size <= 0)
        break;
      offset += s;
    }
  }

  /**
   * Generate instructions to load data from memory at the address in
   * a register plus an offset.  This method can load unaligned data
   * of any size less than or equal to 8 bytes.
   * <p>
   * When a C <code>struct</code> is in a register(s), it is aligned
   * so that it is easy to load the value when the <code>struct</code>
   * is aligned on the proper boundary.  In the table below an
   * <code>x</code> specifies a byte of the structure.  The bytes are
   * left to right in memory order.  The integer specifies the size of
   * the <code>struct</code> in bytes.

   * <pre>
   *  1 0000000x
   *  2 000000xx
   *  3 0000xxx0
   *  4 0000xxxx
   *  5 xxxxx000
   *  6 xxxxxx00
   *  7 xxxxxxx0
   *  8 xxxxxxxx
   *  9 xxxxxxxx 0000000x
   * 10 xxxxxxxx 000000xx
   * 11 xxxxxxxx 0000xxx0
   * 12 xxxxxxxx 0000xxxx
   * 13 xxxxxxxx xxxxx000
   * 14 xxxxxxxx xxxxxx00
   * 15 xxxxxxxx xxxxxxx0
   * 16 xxxxxxxx xxxxxxxx
   * </pre>
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset is the offset value - usually zero
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param signed is true if the data is to be sign extended
   * @param real is true if the data is known to be real
   */
  private void loadFromMemoryWithOffsetX(int     dest,
                                         int     address,
                                         long    offset,
                                         int     size,
                                         long    alignment,
                                         boolean signed,
                                         boolean real)
  {
    // If a shift is needed fdest will take the value of the
    // destination register and dest will become another temporary
    // register.
    boolean signExtend = signed && !real && (size != 8);
    int     lop        = Opcodes.LD;
    
    switch (size) {
    case 1:
      lop = signExtend ? Opcodes.LBS : Opcodes.LB;
      appendInstruction(new LoadInstruction(lop, dest, address, offset));
      return;
    case 2:
      if ((alignment % 2) == 0) {
        lop = signExtend ? Opcodes.LHS : Opcodes.LH;
        appendInstruction(new LoadInstruction(lop, dest, address, offset));
        return;
      }
      break;
    case 3:
      if ((alignment % 4) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LW, dest, address, offset));
        return;
      }
      if ((alignment % 2) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new LoadInstruction(Opcodes.LH, r0, address, offset));
        appendInstruction(new LoadInstruction(Opcodes.LB, dest, address, offset + 2));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, r0, r0, 16));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, dest, dest, 16));
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest, r0, dest));
        return;
      }
      break;
    case 4:
      if ((alignment % 4) == 0) {
        lop = signExtend ? Opcodes.LWS : Opcodes.LW;
        appendInstruction(new LoadInstruction(lop, dest, address, offset));
        if (real) // convert to double after load.
          genTransformReal(dest, 4, dest, 8);
        return;
      }
      if ((alignment % 2) == 0) {
        int tr1 = registers.newTempRegister(RegisterSet.INTREG);
        int tr2 = registers.newTempRegister(RegisterSet.INTREG);
        lop = signExtend ? Opcodes.LHS : Opcodes.LH;
        appendInstruction(new LoadInstruction(lop, tr1, address, offset));
        appendInstruction(new LoadInstruction(Opcodes.LH, tr2, address, offset + 2));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr1, tr1, 16));
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest, tr1, tr2));
        if (real) // convert to double after load.
          genTransformReal(dest, 4, dest, 8);
        return;
      }
      break;
    case 5:
      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LD, dest, address, offset));
        return;
      }
      if ((alignment % 4) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new LoadInstruction(Opcodes.LW, r0, address, offset));
        appendInstruction(new LoadInstruction(Opcodes.LW, dest, address, offset + 4));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, r0, r0, 32));
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest, r0, dest));
        return;
      }
      break;
    case 6:
      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LD, dest, address, offset));
        return;
      }
      if ((alignment % 4) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new LoadInstruction(Opcodes.LW, r0, address, offset));
        appendInstruction(new LoadInstruction(Opcodes.LW, dest, address, offset + 4));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, r0, r0, 32));
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest, r0, dest));
        return;
      }
      break;
    case 7:
      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LD, dest, address, offset));
        return;
      }
      if ((alignment % 4) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new LoadInstruction(Opcodes.LW, r0, address, offset));
        appendInstruction(new LoadInstruction(Opcodes.LW, dest, address, offset + 4));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, r0, r0, 32));
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest, r0, dest));
        return;
      }
      break;
    case 8:
      if ((alignment % 8) == 0) {
        appendInstruction(new LoadInstruction(Opcodes.LD, dest, address, offset));
        return;
      }
      if ((alignment % 4) == 0) {
        int tr  = registers.newTempRegister(RegisterSet.INTREG);
        int br = registers.newTempRegister(RegisterSet.INTREG);
        int mr = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadInstruction(Opcodes.LW, tr, address, offset));
        appendInstruction(new LoadInstruction(Opcodes.LW, br, address, offset + 4));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, mr, tr, 32));
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest, mr, br));
        return;
      }
      if ((alignment % 2) == 0) {
        int tr1 = registers.newTempRegister(RegisterSet.INTREG);
        int tr2 = registers.newTempRegister(RegisterSet.INTREG);
        int tr3 = registers.newTempRegister(RegisterSet.INTREG);
        int tr4 = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new LoadInstruction(Opcodes.LH, tr1, address, offset));
        appendInstruction(new LoadInstruction(Opcodes.LH, tr2, address, offset + 2));
        appendInstruction(new LoadInstruction(Opcodes.LH, tr3, address, offset + 4));
        appendInstruction(new LoadInstruction(Opcodes.LH, tr4, address, offset + 6));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr1, tr1, 48));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr2, tr2, 32));
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr3, tr3, 16));
        appendInstruction(new GeneralInstruction(Opcodes.OR, tr1, tr1, tr2));
        appendInstruction(new GeneralInstruction(Opcodes.OR, tr3, tr3, tr4));
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest, tr3, tr1));
        return;
      }
      break;
    }

    if (size > Trips2RegisterSet.IREG_SIZE)
      throw new scale.common.InternalError("Wrong size for load " + size);

    // Simulate un-aligned load.  Result is aligned in the register on
    // the next higher boundary (e.g., 0, 2, or 4).

    int tr  = registers.newTempRegister(RegisterSet.INTREG);
    int tr5 = registers.newTempRegister(RegisterSet.INTREG);
    int m   = size + addIn[size & 0x7] - 1;
    int shift = 8 * m;
    appendInstruction(new LoadInstruction(Opcodes.LB, tr, address, offset));
    appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr5, tr, shift));
    shift -= 8;
    for (int k = 1; k < size; k++) {
      int tr3 = registers.newTempRegister(RegisterSet.INTREG);
      int tr4 = registers.newTempRegister(RegisterSet.INTREG);
      int tr6 = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new LoadInstruction(Opcodes.LB, tr3, address, offset + k));
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr4, tr3, shift));
      appendInstruction(new GeneralInstruction(Opcodes.OR, tr6, tr5, tr4));
      shift -= 8;
      tr5 = tr6;
    }

    if (real && (size == 4)) { // convert to double after load.
      genTransformReal(tr5, 4, dest, 8);
    } else if (signExtend) {
      int tr7 = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr7, tr5, 8 * (8 - size)));
      appendInstruction(new ImmediateInstruction(Opcodes.SRAI, dest, tr7, 8 * (8 - size)));
    } else {
      appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, tr5));
    }
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
   */
  protected void loadFromMemoryDoubleIndexing(int     dest,
                                              int     index1,
                                              int     index2,
                                              int     size,
                                              long    alignment,
                                              boolean signed,
                                              boolean real)
  {
    int adr = registers.newTempRegister(RegisterSet.ADRREG);
    appendInstruction(new GeneralInstruction(Opcodes.ADD, adr, index1, index2));
    loadFromMemoryWithOffset(dest, adr, 0, size, alignment, signed, real);
  }

  /**
   * Generate instructions to load data from the specified data area.
   * @param dest is the destination register
   * @param address is the register containing the address of the data
   * @param offset specifies the offset from the address register value
   * @param bits specifies the size of the data to be loaded in bits -
   * must be 32 or less
   * @param bitOffset specifies the offset to the field in bits
   * @param alignment specifies the alignment of the address
   * @param signed is true if the data is to be sign extended
   */
  private void loadBitsFromMemory(int     dest,
                                  int     address,
                                  long    offset,
                                  int     bits,
                                  int     bitOffset,
                                  long    alignment,
                                  boolean signed)
  {
    int span = bits + bitOffset;
    int op   = signed ? Opcodes.SRAI : Opcodes.SRLI;

    if (span <= 8) {
      int tr1 = registers.newTempRegister(RegisterSet.ADRREG);
      int tr2 = tr1;
      dest = registers.lastRegister(dest);
      appendInstruction(new LoadInstruction(Opcodes.LB, tr1, address, offset));
      if (signed) {
        tr2 = registers.newTempRegister(RegisterSet.ADRREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr2, tr1, 56 + bitOffset));
        appendInstruction(new ImmediateInstruction(Opcodes.SRAI, dest, tr2, 64 - bits));
        return;
      }
      
      int shift = 8 - span;
      if (shift > 0) {
        tr2 = registers.newTempRegister(RegisterSet.ADRREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, tr2, tr1, shift));
      }
     
      if (bits != 8)
        appendInstruction(new ImmediateInstruction(Opcodes.ANDI, dest, tr2, (1 << bits) - 1));
      else
        appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, tr2));
      return;
    }

    if (span <= 16) {
      int tr1 = registers.newTempRegister(RegisterSet.ADRREG);
      int tr2 = registers.newTempRegister(RegisterSet.ADRREG);
      dest = registers.lastRegister(dest);
      loadFromMemoryWithOffsetX(tr1, address, offset, 2, alignment, false, false);
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr2, tr1, bitOffset + 48));
      appendInstruction(new ImmediateInstruction(op, dest, tr2, 64 - bits));
      return;
    }

    if (span <= 32) {
      int tr1 = registers.newTempRegister(RegisterSet.ADRREG);
      int tr2 = registers.newTempRegister(RegisterSet.ADRREG);
      dest = registers.lastRegister(dest);
      loadFromMemoryWithOffsetX(tr1, address, offset, 4, alignment, false, false);
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr2, tr1, bitOffset + 32));
      appendInstruction(new ImmediateInstruction(op, dest, tr2, 64 - bits));
      return;
    }

    if (span <= 64) {
      boolean aligned = (alignment >= 8);
      if (aligned && (bits == 64)) {
        loadFromMemoryWithOffset(dest, address, offset, 8, alignment, signed, false);
        return;
      }

      int tr1 = registers.newTempRegister(RegisterSet.INTREG);
      int tr2 = tr1;
      loadFromMemoryWithOffset(tr1, address, offset, 8, alignment, signed, false);
      if (bitOffset > 0) {
        tr2 = registers.newTempRegister(RegisterSet.ADRREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr2, tr1, bitOffset));
      }
      appendInstruction(new ImmediateInstruction(op, dest, tr2, 64 - bits));
      return;
    }

    StringBuffer buf = new StringBuffer("Load - bits (");
    buf.append(bits);
    buf.append(") + bitOffset (");
    buf.append(bitOffset);
    buf.append(") <= 32 + span (");
    buf.append(span);
    buf.append(")\ndestreg (");
    buf.append(dest);
    buf.append(") + address (");
    buf.append(address);
    buf.append(") + offset (");
    buf.append(offset);
    buf.append(") + align (");
    buf.append(alignment);
    buf.append(")");
    throw new scale.common.InternalError(buf.toString());
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data
   * in memory
   * @param offset specifies the offset from the address register value
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real
   */
  protected void storeIntoMemoryWithOffset(int     src,
                                           int     address,
                                           long    offset,
                                           int     size,
                                           long    alignment,
                                           boolean real)
  {
    if (registers.pairRegister(src)) {
      int s = size / 2;
      assert (s <= Trips2RegisterSet.IREG_SIZE) : "Paired register size " + size;
      storeIntoMemoryWithOffsetX(src + 0, address, offset,     s, alignment, real);
      storeIntoMemoryWithOffsetX(src + 1, address, offset + s, s, alignment, real);
      return;
    }

    if (size <= Trips2RegisterSet.IREG_SIZE) {
      storeIntoMemoryWithOffsetX(src, address, offset, size, alignment, real);
      return;
    }

    // Structs

    while (true) {
      int s  = size;
      int tr = src;
      if (s > Trips2RegisterSet.IREG_SIZE)
        s = Trips2RegisterSet.IREG_SIZE;

      storeIntoMemoryWithOffsetX(tr, address, offset, s, alignment, real);
      src++;
      size -= s;
      if (size <= 0)
        break;
      offset += s;
    }
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
    long off   = 0;

    assert offset.isNumeric() : "Symbolic displacement " + offset;

    if (offset.isStack()) {
      int adr = registers.newTempRegister(RegisterSet.ADRREG);
      appendInstruction(new ImmediateInstruction(Opcodes.ADDI, adr, address, offset));
      address = adr;
    } else {
      off = offset.getDisplacement();
    }

    storeIntoMemoryWithOffset(src, address, off, size, alignment, real);
  }

  /**
   * Generate instructions to store data into memory at the address in
   * a register plus an offset.  When a C <code>struct</code> is in a
   * register(s), it is aligned so that it is easy to load the value
   * when the <code>struct</code> is aligned on the proper boundary.
   * In the table below an <code>x</code> specifies a byte of the
   * structure.  The bytes are left to right in memory order.  The
   * integer specifies the size of the <code>struct</code> in bytes.
   * <pre>
   *  1 0000000x
   *  2 000000xx
   *  3 0000xxx0
   *  4 0000xxxx
   *  5 xxxxx000
   *  6 xxxxxx00
   *  7 xxxxxxx0
   *  8 xxxxxxxx
   *  9 xxxxxxxx 0000000x
   * 10 xxxxxxxx 000000xx
   * 11 xxxxxxxx 0000xxx0
   * 12 xxxxxxxx 0000xxxx
   * 13 xxxxxxxx xxxxx000
   * 14 xxxxxxxx xxxxxx00
   * 15 xxxxxxxx xxxxxxx0
   * 16 xxxxxxxx xxxxxxxx
   * </pre>
   * @param src is the register containing the value to be stored
   * @param address is the register containing the address of the data
   * @param offset is the offset value - usually zero
   * @param size specifies the size of the data to be stored
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real
   */
  private void storeIntoMemoryWithOffsetX(int     src,
                                          int     address,
                                          long    offset,
                                          int     size,
                                          long    alignment,
                                          boolean real)
  {
    // offsets which are too large will be expanded by ExpandPseudos.expandAll()
    switch (size) {
    case 1:
      appendInstruction(new StoreInstruction(Opcodes.SB, offset, address, src));
      return;
    case 2:
      if ((alignment % 2) == 0) {
        appendInstruction(new StoreInstruction(Opcodes.SH, offset, address, src));
        return;
      }
      break;
    case 3:
      if ((alignment % 2) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 16));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset, address, r0));
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 8));
        appendInstruction(new StoreInstruction(Opcodes.SB, offset + 2, address, r0));
        return;
      }
      break;
    case 4:
      if (real) { // If double, convert to single before store.
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        genTransformReal(src, 8, r0, 4);
        src = r0;
      }
      if ((alignment % 4) == 0) {
        appendInstruction(new StoreInstruction(Opcodes.SW, offset, address, src));
        return;
      }
      if ((alignment % 2) == 0) {
        int tr0 = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, tr0, src, 16));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 0, address, tr0));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 2, address, src));
        return;
      }
      break;
    case 5:
      if ((alignment % 4) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 32));
        appendInstruction(new StoreInstruction(Opcodes.SW, offset, address, r0));
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 24));
        appendInstruction(new StoreInstruction(Opcodes.SB, offset + 4, address, r0));
        return;
      }
      break;
    case 6:
      if ((alignment % 4) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 32));
        appendInstruction(new StoreInstruction(Opcodes.SW, offset, address, r0));
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 16));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 4, address, r0));
        return;
      }
      break;
    case 7:
      if ((alignment % 4) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 32));
        appendInstruction(new StoreInstruction(Opcodes.SW, offset, address, r0));
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 16));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 4, address, r0));
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 8));
        appendInstruction(new StoreInstruction(Opcodes.SB, offset + 6, address, r0));
        return;
      }
      break;
    case 8:
      if ((alignment % 8) == 0) {
        appendInstruction(new StoreInstruction(Opcodes.SD, offset, address, src));
        return;
      }
      if ((alignment % 4) == 0) {
        int r0 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, r0, src, 32));
        appendInstruction(new StoreInstruction(Opcodes.SW, offset, address, r0));
        appendInstruction(new StoreInstruction(Opcodes.SW, offset + 4, address, src));
        return;
      }
      if ((alignment % 2) == 0) {
        int tr0 = registers.newTempRegister(RegisterSet.INTREG);
        int tr1 = registers.newTempRegister(RegisterSet.INTREG);
        int tr2 = registers.newTempRegister(RegisterSet.INTREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, tr0, src, 48));
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, tr1, src, 32));
        appendInstruction(new ImmediateInstruction(Opcodes.SRLI, tr2, src, 16));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 0, address, tr0));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 2, address, tr1));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 4, address, tr2));
        appendInstruction(new StoreInstruction(Opcodes.SH, offset + 6, address, src));
        return;
      }
      break;
    }

    if (size > Trips2RegisterSet.IREG_SIZE)
      throw new scale.common.InternalError("Wrong size for store " + size);

    // Simulate un-aligned store.

    int m   = size + addIn[size & 0x7] - 1;
    int shift = 8 * m;
    for (int k = 0; k < size; k++) {
      int tr = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new ImmediateInstruction(Opcodes.SRLI, tr, src, shift));
      appendInstruction(new StoreInstruction(Opcodes.SB, offset + k, address, tr));
      shift -= 8;;
    }
   }

  /**
   * Generate instructions to store data into memory at the address
   * specified by a register.
   * @param src is the source register
   * @param address is the register containing the address of the data in memory
   * @param size specifies the size of the data to be loaded
   * @param alignment is the alignment of the data (usually 1, 2, 4, or 8)
   * @param real is true if the data is known to be real
   */
  protected void storeIntoMemory(int     src,
                                 int     address,
                                 int     size,
                                 long    alignment,
                                 boolean real)
  {
    storeIntoMemoryWithOffset(src, address, 0, size, alignment, real);
  }

  /**
   * Generate instructions to store data into the specified data area.
   * @param src is the source register
   * @param address is the register containing the address of the data
   * in memory
   * @param offset specifies the offset from the address register value
   * @param bits specifies the size of the data in bits to be loaded -
   * must be 32 or less
   * @param bitOffset specifies the offset in bits to the data
   * @param alignment specifies the alignment of the address
   */
  protected void storeBitsIntoMemory(int  src,
                                     int  address,
                                     long offset,
                                     int  bits,
                                     int  bitOffset,
                                     int  alignment)
  {
    int span = bits + bitOffset;
    int trs  = registers.newTempRegister(RegisterSet.INTREG);
    int trd  = registers.newTempRegister(RegisterSet.INTREG);

    if (span <= 8) {
      src = registers.lastRegister(src);
      int shift = 8 - span;
      if (bits == 8) {
        appendInstruction(new StoreInstruction(Opcodes.SB, offset, address, src));
        return;
      }
      long mask  = ((1L << bits) - 1) << shift;
      appendInstruction(new LoadInstruction(Opcodes.LB, trd, address, offset));
      if (shift > 0) {
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, trs, src, shift));
        if (mask != 0xff)
          appendInstruction(new ImmediateInstruction(Opcodes.ANDI, trs, trs, mask));
      } else
        appendInstruction(new ImmediateInstruction(Opcodes.ANDI, trs, src, mask));
      appendInstruction(new ImmediateInstruction(Opcodes.ANDI, trd, trd, ~mask));
      appendInstruction(new GeneralInstruction(Opcodes.OR, trd, trd, trs));
      appendInstruction(new StoreInstruction(Opcodes.SB, offset, address, trd));
      return;
    }

    if (span <= 16) {
      boolean aligned = (alignment >= 2);
      src = registers.lastRegister(src);
      int shift = 16 - span;
      if (aligned && (bits == 16)) {
        appendInstruction(new StoreInstruction(Opcodes.SH, offset, address, src));
        return;
      }

      long mask  = ((1L << bits) - 1) << shift;
      int  trm  = generateEnter(mask);
      int  trmc = registers.newTempRegister(RegisterSet.INTREG);

      appendInstruction(new ImmediateInstruction(Opcodes.XORI, trmc, trm, -1)); // Form the complement of the mask.
      loadFromMemoryWithOffsetX(trd, address, offset, 2, alignment, false, false);
      
      if (shift > 0) {
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, trs, src, shift));
        appendInstruction(new GeneralInstruction(Opcodes.AND, trs, trm, trs));
      } else
        appendInstruction(new GeneralInstruction(Opcodes.AND, trs, trm, src));
      
      appendInstruction(new GeneralInstruction(Opcodes.AND, trd, trmc, trd));
      appendInstruction(new GeneralInstruction(Opcodes.OR, trd, trs, trd));
      storeIntoMemoryWithOffsetX(trd, address, offset, 2, alignment, false);
      return;
    }

    if (span <= 32) {
      boolean aligned = (alignment >= 4);
      src = registers.lastRegister(src);
      int shift = 32 - span;
      if (aligned && (bits == 32)) {
        appendInstruction(new StoreInstruction(Opcodes.SW, offset, address, src));
        return;
      }

      long mask = ((1L << bits) - 1) << shift;
      int  trm  = generateEnter(mask);
      int  trmc = registers.newTempRegister(RegisterSet.INTREG);

      appendInstruction(new ImmediateInstruction(Opcodes.XORI, trmc, trm, -1)); // Form the complement of the mask.
      loadFromMemoryWithOffsetX(trd, address, offset, 4, alignment, false, false);

      if (shift > 0) {
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, trs, src, shift));
        appendInstruction(new GeneralInstruction(Opcodes.AND, trs, trm, trs));
      } else
        appendInstruction(new GeneralInstruction(Opcodes.AND, trs, trm, src));
      
      appendInstruction(new GeneralInstruction(Opcodes.AND, trd, trmc, trd));
      appendInstruction(new GeneralInstruction(Opcodes.OR, trd, trs, trd));
      storeIntoMemoryWithOffsetX(trd, address, offset, 4, alignment, false);
      return;
    }

    if (span <= 64) {
      boolean aligned = (alignment >= 8);
      if (aligned && (bits == 64)) {
        appendInstruction(new StoreInstruction(Opcodes.SD, offset, address, src));
        return;
      }

      int  shift = 64 - span;
      long mask  = ((1L << bits) - 1) << shift;
      int  trm   = generateEnter(mask);
      int  trmc  = registers.newTempRegister(RegisterSet.INTREG);

      appendInstruction(new ImmediateInstruction(Opcodes.XORI, trmc, trm, -1)); // Form the complement of the mask.
      loadFromMemoryWithOffsetX(trd, address, offset, 8, alignment, false, false);

      if (shift > 0) {
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, trs, src, shift));
        appendInstruction(new GeneralInstruction(Opcodes.AND, trs, trm, trs));
      } else
        appendInstruction(new GeneralInstruction(Opcodes.AND, trs, trm, src));
      appendInstruction(new GeneralInstruction(Opcodes.AND, trd, trmc, trd));

      appendInstruction(new GeneralInstruction(Opcodes.OR, trd, trs, trd));
      storeIntoMemoryWithOffsetX(trd, address, offset, 8, alignment, false);
      return;
    }

    StringBuffer buf = new StringBuffer("Store - bits (");
    buf.append(bits);
    buf.append(") + bitOffset (");
    buf.append(bitOffset);
    buf.append(") <= 32 + span (");
    buf.append(span);
    buf.append(")\ndestreg (");
    buf.append(src);
    buf.append(") + address (");
    buf.append(address);
    buf.append(") + offset (");
    buf.append(offset);
    buf.append(") + align (");
    buf.append(alignment);
    buf.append(")");
    throw new scale.common.InternalError(buf.toString());
  }

  /**
   * Generate an instruction sequence to move words from one location
   * to another.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move
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

    long off = 0;

    if (destoff.isStack()) {
      int adr = generateEnterA(destoff);
      appendInstruction(new GeneralInstruction(Opcodes.ADD, adr, dest, adr));
      dest = adr;
    } else {
      off = destoff.getDisplacement();
    }

    moveWords(src, srcoff, dest, off, size, aln);
  }

  /**
   * Generate an instruction sequence to move words from one location
   * to another.
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move
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
    int MAX_INST = 32;   // max number of instructions for inline copy
    int ms       = 1;
    int lop      = Opcodes.LB;
    int sop      = Opcodes.SB;

    if (((aln & 0x7) == 0) && ((size & 0x7) == 0)) {
      ms  = 8;
      lop = Opcodes.LD;
      sop = Opcodes.SD;
    } else if (((aln & 0x3) == 0) && ((size & 0x3) == 0)) {
      ms  = 4;
      lop = Opcodes.LW;
      sop = Opcodes.SW;
    } else if (((aln & 0x1) == 0) && ((size & 0x1) == 0)) {
      ms  = 2;
      lop = Opcodes.LH;
      sop = Opcodes.SH;
    }

    if (((size / ms) * 4) <= MAX_INST) {
      // The data size is small enough that we can generate the
      // sequence inline.
      moveWordsInline(src, srcoff, dest, destoff, size, ms, lop, sop);
    } else {
      // The data size is too large so we will generate a loop to copy
      // the data.
      moveWordsWithLoop(src, srcoff, dest, destoff, size, ms, lop, sop);
    }
  }

  /**
   * Generate an instruction sequence to move words from one location
   * to another using a loop.  This method is called by moveWords().
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move
   * @param ms the number of bytes of data that can be copied at a
   * time (8, 4, 2, 1)
   * @param lop the load instruction to use
   * @param sop the store instruction to use
   */
  private void moveWordsWithLoop(int  srcreg,
                                 long srcoff,
                                 int  destreg,
                                 long destoff,
                                 int  size,
                                 int  ms,
                                 int  lop,
                                 int  sop)
  {
    assert (predicateReg == -1) : "Attemping to generate a memcpy inside a hyperblock.";
    
    int    src     = registers.newTempRegister(RegisterSet.AIREG);
    int    dest    = registers.newTempRegister(RegisterSet.AIREG);
    int    cntreg  = registers.newTempRegister(RegisterSet.AIREG);
    int    testreg = registers.newTempRegister(RegisterSet.AIREG);
    int    datareg = registers.newTempRegister(RegisterSet.AIREG);
    int    pr      = registers.newTempRegister(RegisterSet.AIREG);
    Label  labl    = createLabel();
    Label  labn    = createLabel();
    Branch br      = new TripsBranch(Opcodes.BRO, labl, 1);
    Branch brt     = new TripsBranch(Opcodes.BRO, labl, 1, pr, true);
    Branch brf     = new TripsBranch(Opcodes.BRO, labn, 1, pr, false);

    genLoadImmediate(srcoff, srcreg, src);
    genLoadImmediate(destoff, destreg, dest);

    br.addTarget(labl, 0);
    brt.addTarget(labl, 0);
    brf.addTarget(labn, 0);

    genLoadImmediate(size, testreg);
    genLoadImmediate(ms, cntreg);
    appendInstruction(br);

    appendLabel(labl);

    appendInstruction(new LoadInstruction(lop, datareg, src, 0));
    appendInstruction(new StoreInstruction(sop, 0, dest, datareg));
    appendInstruction(new GeneralInstruction(Opcodes.TLTU, pr, cntreg, testreg));
    appendInstruction(new ImmediateInstruction(Opcodes.ADDI, cntreg, cntreg, ms));
    appendInstruction(new ImmediateInstruction(Opcodes.ADDI, src, src, ms));
    appendInstruction(new ImmediateInstruction(Opcodes.ADDI, dest, dest, ms));
    appendInstruction(brt);
    appendInstruction(brf);

    appendLabel(labn);
  }

  /**
   * Generate an instruction sequence to move words from one location
   * to another inline.  This method is called by moveWords().
   * @param src specifies the register containing the source address
   * @param srcoff specifies the offset from the source address
   * @param dest specifies the register containing the destination address
   * @param destoff specifies the offset from the destination address
   * @param size specifes the number of bytes to move
   * @param ms the number of bytes of data that can be copied at a
   * time (8, 4, 2, 1)
   * @param lop the load instruction to use
   * @param sop the store instruction to use
   */
  private void moveWordsInline(int  src,
                               long srcoff,
                               int  dest,
                               long destoff,
                               int  size,
                               int  ms,
                               int  lop,
                               int  sop)
  {
    for (int k = 0; k < size; k += ms) {
      int vr  = registers.newTempRegister(RegisterSet.AFIREG);
      appendInstruction(new LoadInstruction(lop, vr, src, srcoff));
      appendInstruction(new StoreInstruction(sop, destoff, dest, vr));
      srcoff += ms;
      destoff += ms;
    }
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

    int adr = loadMemoryAddress(disp);

    loadFromMemoryWithOffset(dest, adr, 0, dsize, 8, isSigned, isReal);
  }

  private void genMultiplyQuad(long value, int src, int dest)
  {
    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // Multiply by a power of 2.
      genLeftShiftUnsigned(src, shift, dest, 8);
      return;
    }
    
    // Values which are too large will be expanded by
    // ExpandPseudos.expandAll().
    appendInstruction(new ImmediateInstruction(Opcodes.MULI, dest, src, value));
  }

  private void genMultiplyLong(long value, int src, int dest)
  {
    int shift = Lattice.powerOf2(value);
    if (shift >= 0) { // multiply by a power of 2
      genLeftShiftSigned(src, shift, dest, 4);
      return;
    }

    int tr = registers.newTempRegister(RegisterSet.AFIREG);
    // Values which are too large will be expanded by
    // ExpandPseudos.expandAll().
    appendInstruction(new ImmediateInstruction(Opcodes.MULI, tr, src, value));
    appendInstruction(new GeneralInstruction(Opcodes.EXTSW, dest, tr));
  }


  private void dividePower2(long value, int shift, int src, int dest, boolean quad, boolean signed)
  {
    if (shift == 0) { // Dividing by the value 1.
      genRegToReg(src, dest);
      return;
    }

    if (signed) { // Divide a signed integer.
      int pr = registers.newTempRegister(RegisterSet.INTREG);
      int tr = registers.newTempRegister(RegisterSet.INTREG);
      appendInstruction(new ImmediateInstruction(Opcodes.TLTI, pr, src, 0));
      appendInstruction(new ImmediateInstruction(Opcodes.ADDI, tr, src, value - 1, pr, true));
      appendInstruction(new GeneralInstruction(Opcodes.MOV, tr, src, pr, false));
      appendInstruction(new ImmediateInstruction(Opcodes.SRAI, dest, tr, shift));
      return;
    }

    // Divide an unsigned integer.
    appendInstruction(new ImmediateInstruction(Opcodes.SRLI, dest, src, shift));
  }

  /**
   * Return the appropriate opcode for the operation and type of
   * operands.
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
      doComplexOp(which, bs, la, ra, ir);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    boolean  flt       = ct.isRealType();                          
    int      opcode    = getBinaryOpcode(which, flt, bs);
    boolean  isLiteral = false;
    long     value     = 0;

    assert (opcode != 0x100) : "Invalid opcode:" + opcode;

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
        isLiteral = true;
      } else if (lit instanceof SizeofLiteral) {
        value = valueOf((SizeofLiteral) lit);
        isLiteral = true;
      } else if ((lit instanceof FloatLiteral) && (which == DIV)) {
        FloatLiteral fl = (FloatLiteral) lit;
        double       dv = fl.getDoubleValue();
        int          mv = generateEnter(1.0 / dv);
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, ir, laReg, mv));
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }
    
    boolean removeCarry = (!flt && (gensCarry[which] && (bs <= 4)));

    if (isLiteral && !flt && Trips2Machine.isImmediate(value)) {
      if ((value == 0) &&
          ((which == ADD) ||
           (which == SUB) ||
           (which == OR) ||
           (which == SRA) ||
           (which == SRL) ||
           (which == SLL))) {
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = laReg;
        return;
      } else if (opcode == Opcodes.SLL) {
        if (ct.isSigned())
          genLeftShiftSigned(laReg, value, ir, bs);
        else
          genLeftShiftUnsigned(laReg, value, ir, bs);

        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      } else {
        if (removeCarry) {
          int tr = registers.newTempRegister(RegisterSet.AFIREG);
          appendInstruction(new ImmediateInstruction(Opcodes.getIntImmOp(opcode), tr, laReg, value));
          if (bs <= 4) 
            if (ct.isSigned())
              ir = maybeSignExtend(tr, ir, bs);
            else
              ir = maybeZeroExtend(tr, ir, bs);
        } else
          appendInstruction(new ImmediateInstruction(Opcodes.getIntImmOp(opcode), ir, laReg, value));
        
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    needValue(ra);
    int raReg = resultReg;

    if (removeCarry) {
      int tr = registers.newTempRegister(RegisterSet.AFIREG);
      appendInstruction(new GeneralInstruction(opcode, tr, laReg, raReg));
      if (bs <= 4)
        if (ct.isSigned())
          ir = maybeSignExtend(tr, ir, bs);
        else
          ir = maybeZeroExtend(tr, ir, bs);
    } else
      appendInstruction(new GeneralInstruction(opcode, ir, laReg, raReg));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  /**
   * Sign-extend for a 64-bit register.
   */
  private void signExtend(int src, int dest, int sz)
  {
    switch (sz) {
    case 1:
      appendInstruction(new GeneralInstruction(Opcodes.EXTSB, dest, src));
      break;
    case 2:
      appendInstruction(new GeneralInstruction(Opcodes.EXTSH, dest, src));
      break;
    case 4:
      appendInstruction(new GeneralInstruction(Opcodes.EXTSW, dest, src));
      break;
    default:
      throw new scale.common.InternalError("Invalid sign extension of " + sz + "bytes.");
    }
  }

  /**
   * Zero-extend for a 64-bit register.
   */
  private void zeroExtend(int src, int dest, int sz)
  {
    switch (sz) {
    case 1:
      appendInstruction(new GeneralInstruction(Opcodes.EXTUB, dest, src));
      break;
    case 2:
      appendInstruction(new GeneralInstruction(Opcodes.EXTUH, dest, src));
      break;
    case 4:
      appendInstruction(new GeneralInstruction(Opcodes.EXTUW, dest, src));
      break;
    default:
      throw new scale.common.InternalError("Invalid unsign extension of " + sz + " bytes.");
    }
  }

  /**
   * Possibly sign-extend for a 64-bit register, depending on the value of
   * -wrap-on-overflow.  
   * @return the register that holds the result (which is the src if no
   * instruction is generated)
   */
  private int maybeSignExtend(int src, int dest, int sz)
  {
    if (Optimization.signedIntsWrapOnOverflow) {
      signExtend(src, dest, sz);
      return dest;
    } else {
      return src;
    }
  }

  /**
   * Possibly zero-extend for a 64-bit register, depending on the value of
   * -wrap-on-overflow.  
   * @return the register that holds the result (which is the src if no
   * instruction is generated)
   */
  private int maybeZeroExtend(int src, int dest, int sz)
  {
    if (Optimization.unsignedIntsWrapOnOverflow) {
      zeroExtend(src, dest, sz);
      return dest;
    } else {
      return src;
    }
  }

  /**
   * Generate a logical left shift for a signed type.
   * @param src the source register
   * @param value the number of places to shift
   * @param dest the destination register
   * @param destSize the size in bytes of the destination register
   */
  private void genLeftShiftSigned(int src, long value, int dest, int destSize)
  {
    long shift = destSize > 4 ? value : value + 32;

    appendInstruction(new ImmediateInstruction(Opcodes.SLLI, dest, src, shift));
    if (destSize <= 4) {
      // In C operations are done on ints.
      appendInstruction(new ImmediateInstruction(Opcodes.SRAI, dest, dest, 32));
    }
  }

  /**
   * Generate a logical left shift for an unsigned type.
   * @param src the source register
   * @param value the number of places to shift
   * @param dest the destination register
   * @param destSize the size in bytes of the destination register
   */
  private void genLeftShiftUnsigned(int src, long value, int dest, int destSize)
  {
    appendInstruction(new ImmediateInstruction(Opcodes.SLLI, dest, src, value));
    if (destSize <= 4) {
      // In C operations are done on ints.
      zeroExtend(dest, dest, destSize);
    }
  }

  /**
   * Generate a logical left shift for an unsigned type.
   * @param value the data that will be shifted
   * @param shift the number of places to shift
   * @return the register with the result
   */
  private int genLeftShiftUnsigned(int value, int shift)
  {
    int dest = registers.newTempRegister(RegisterSet.AFIREG);
    int src  = generateEnter(value);
    genLeftShiftUnsigned(src, shift, dest, 8);
    return dest;
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

    doComplexOp(which, laReg, raReg, dest);
  }

  private void doComplexOp(int which, int laReg, int raReg, int dest)
  {
    switch (which) {
    case ADD:
      if (registers.pairRegister(laReg)) {
        int t = laReg;
        laReg = raReg;
        raReg = t;
      }
      appendInstruction(new GeneralInstruction(Opcodes.FADD, dest, laReg, raReg));
      if (registers.pairRegister(laReg))
        appendInstruction(new GeneralInstruction(Opcodes.FADD, dest + 1, laReg + 1, raReg + 1));
      else
        appendInstruction(new GeneralInstruction(Opcodes.OR, dest + 1, raReg + 1, raReg + 1));
      return;
    case SUB:
      appendInstruction(new GeneralInstruction(Opcodes.FSUB, dest + 0, laReg + 0, raReg + 0));
      if (registers.pairRegister(laReg)) {
        if (registers.pairRegister(raReg))
          appendInstruction(new GeneralInstruction(Opcodes.FSUB, dest + 1, laReg + 1, raReg + 1));
        else
          appendInstruction(new GeneralInstruction(Opcodes.OR,   dest + 1, laReg + 1, laReg + 1));
      } else {
        int vs0 = generateEnter(0.0);
        appendInstruction(new GeneralInstruction(Opcodes.FSUB, dest + 1, vs0,   raReg + 1));
      }
      return;
    case MUL:
      if (registers.pairRegister(laReg)) {
        int t = laReg;
        laReg = raReg;
        raReg = t;
      }
      if (registers.pairRegister(laReg)) {
        int tr  = registers.newTempRegister(RegisterSet.AFIREG);
        int tr2 = registers.newTempRegister(RegisterSet.AFIREG);
        int tr3 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, tr2,      laReg + 0, raReg + 0));
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, tr3,      laReg + 1, raReg + 1));
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, tr,       laReg + 0, raReg + 1));
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, dest + 1, laReg + 1, raReg + 0));
        appendInstruction(new GeneralInstruction(Opcodes.FSUB, dest + 0, tr2,       tr3));
        appendInstruction(new GeneralInstruction(Opcodes.FADD, dest + 1, tr,        dest + 1));
      } else {
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, dest + 0, laReg, raReg + 0));
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, dest + 1, laReg, raReg + 1));
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
        int r1  = laReg;
        int i1  = laReg + 1;
        int r2  = raReg;
        int i2  = raReg + 1;
        int t3  = registers.newTempRegister(RegisterSet.AFIREG + RegisterSet.PAIRREG);
        int t17 = generateEnter(2047);
        int t18 = registers.newTempRegister(RegisterSet.AFIREG);
        int f12 = registers.newTempRegister(RegisterSet.AFIREG);
        int f13 = registers.newTempRegister(RegisterSet.AFIREG);
        int f14 = registers.newTempRegister(RegisterSet.AFIREG);
        int f15 = registers.newTempRegister(RegisterSet.AFIREG);
        int f16 = registers.newTempRegister(RegisterSet.AFIREG);
        int f17 = registers.newTempRegister(RegisterSet.AFIREG);

        if (!registers.pairRegister(laReg))
          i1 = generateEnter(0.0);

        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, t17, t17, 52));
        genRegToReg(r2, t3);
        appendInstruction(new GeneralInstruction(Opcodes.AND, t3, t17, t3));
        appendInstruction(new GeneralInstruction(Opcodes.AND, t3 + 1, t17, t3 + 1));
        appendInstruction(new GeneralInstruction(Opcodes.TLT, t18, t3, t3 + 1));
        appendInstruction(new GeneralInstruction(Opcodes.MOV, t3, t3 + 1, t18, true));
        appendInstruction(new GeneralInstruction(Opcodes.MOV, t3, t3, t18, false));
        appendInstruction(new GeneralInstruction(Opcodes.SUB, t17, t3, t17));
        genRegToReg(t17, f13); //  f13 = scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f12,  r2, f13)); // f12 = r2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f13,  i2, f13)); // f13 = i2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f14,  r2, f12)); // f14 = r2 * r2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f15,  i2, f13)); // f15 = i2 * i2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f16,  r1, f12)); // f16 = laReg * r2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f17,  i1, f13)); // f17 = i1 * i2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f12,  i1, f12)); // f12 = i1 * r2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FMUL, f13,  r1, f13)); // f13 = laReg * i2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FADD, f14, f14, f15)); // f14 = r2 * r2 * scale + i2 * i2 * scale
        f15 = generateEnter(1.0);
        appendInstruction(new GeneralInstruction(Opcodes.FADD, f17,  f16, f17)); // f17 = laReg * r2 * scale + i1 * i2 * scale
        appendInstruction(new GeneralInstruction(Opcodes.FSUB, f13,  f12, f13)); // f13 = i1 * r2 * scale - laReg * i2 * scale
        // f14 = 1.0 / ( r2 * r2 * scale + i2 * i2 * scale)
        if (softwareFDIV) {
          genRegToReg(f15, Trips2RegisterSet.FF_REG);
          genRegToReg(f14, Trips2RegisterSet.FF_REG +1);
          genFtnCall("_float64_div",
                     genDoubleUse(Trips2RegisterSet.FF_REG, Trips2RegisterSet.FF_REG + 1),
                     null);
          genRegToReg(Trips2RegisterSet.FR_REG, f14);
        } else
          appendInstruction(new GeneralInstruction(Opcodes.FDIV, f14, f15, f14));
        // dest  = (laReg * r2 * scale + i1 * i2 * scale) * (1.0 / ( r2 * r2 * scale + i2 * i2 * scale))
        appendInstruction(new GeneralInstruction(Opcodes.FMUL,  dest,  f17, f14));
        // dest + 1  = (i1 * r2 * scale - laReg * i2 * scale) * (1.0 / ( r2 * r2 * scale + i2 * i2 * scale))
        appendInstruction(new GeneralInstruction(Opcodes.FMUL,  dest + 1,  f13, f14));
      } else {
        if (softwareFDIV) {
          int t17 = registers.newTempRegister(RegisterSet.AFIREG);
          genRegToReg(laReg + 0, Trips2RegisterSet.FF_REG);
          genRegToReg(raReg, Trips2RegisterSet.FF_REG + 1);
          genFtnCall("_float64_div",
                     genDoubleUse(Trips2RegisterSet.FF_REG, Trips2RegisterSet.FF_REG + 1),
                     null);
          genRegToReg(Trips2RegisterSet.FR_REG, t17);  // Can't set dest yet because it may be the same as raReg.
          genRegToReg(laReg + 1, Trips2RegisterSet.FF_REG);
          genRegToReg(raReg, Trips2RegisterSet.FF_REG + 1);
          // second argument remains the same
          genFtnCall("_float64_div",
                     genDoubleUse(Trips2RegisterSet.FF_REG, Trips2RegisterSet.FF_REG + 1),
                     null);
          genRegToReg(t17, dest + 0);
          genRegToReg(Trips2RegisterSet.FR_REG, dest + 1);
        } else {
          appendInstruction(new GeneralInstruction(Opcodes.FDIV, dest + 0, laReg + 0, raReg));
          appendInstruction(new GeneralInstruction(Opcodes.FDIV, dest + 1, laReg + 1, raReg));
        }
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
    Expr la = c.getOperand(0);
    Expr ra = c.getOperand(1);
    
    // Swap the arguments if the left arg. is a literal so we can use
    // an immediate instruction.
    
    if (la.isLiteralExpr()) {
      Expr t = la;
      la = ra;
      ra = t;
      which = which.argswap();
    }

    needValue(la);
    int laReg = resultReg;

    Type    lt = processType(la);
    boolean si = lt.isSigned();
    if (lt.isRealType()) {
      needValue(ra);
      int raReg = resultReg;

      appendInstruction(new GeneralInstruction(ftops[which.ordinal()], ir, laReg, raReg));
      if (lt.isComplexType()) {
        int tr     = registers.newTempRegister(RegisterSet.AFIREG);
        int laRegP = laReg + registers.numContiguousRegisters(laReg);
        int raRegP = raReg + registers.numContiguousRegisters(raReg);
        int op     = (which == CompareMode.EQ) ? Opcodes.AND : Opcodes.OR;
        appendInstruction(new GeneralInstruction(ftops[which.ordinal()], tr, laRegP, raRegP));
        appendInstruction(new GeneralInstruction(op, ir, tr, ir));
      }
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }

    if (ra.isLiteralExpr()) {
      LiteralExpr le    = (LiteralExpr) ra;
      Literal     lit   = le.getLiteral().getConstantValue();
      long        value = -1;
      boolean     flag = false;

      if (lit instanceof IntLiteral) {
        value = ((IntLiteral) lit).getLongValue();
        flag = true;
      } else if (lit instanceof CharLiteral) {
        value = ((CharLiteral) lit).getCharacterValue();
        flag = true;
      }

      if (flag) {
        int op = si ? iitops[which.ordinal()] : iiutops[which.ordinal()];
        appendInstruction(new ImmediateInstruction(op, ir, laReg, value));
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    needValue(ra);
    int raReg = resultReg;
    appendInstruction(new GeneralInstruction(si ? itops[which.ordinal()] : iutops[which.ordinal()], ir, laReg, raReg));
    
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
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

    if (newLine && srcLinePerBlock) {
      Label nlab = createLabel();
      generateUnconditionalBranch(nlab);
      appendLabel(nlab);
    }

    String fileName = currentRoutine.getCallGraph().getName();
    appendInstruction(new Trips2LineMarker(fileName, line));
  }

  /**
   * Generate a predicated branch to a single location. This is only
   * relevant when hyperblocks are enabled.
   * @param which specifies the branch test (EQ, NE, LT, ...)
   * @param treg specifies the condition to test
   * @param lab is the label to branch to.
   */
  public void generateConditionalBranch(CompareMode which, int treg, Label lab)
  {
    lastInstruction.specifySpillStorePoint();

    boolean pred1 = true;

    if (which == CompareMode.NE)
      pred1 = false;
    else if (which != CompareMode.EQ)
      throw new scale.common.InternalError("Invalid which " + which);
    
//     if (!lastInstruction.defs(treg, registers)) { // So that predicates are not placed in global registers.
//       int reg = registers.newTempRegister(RegisterSet.INTREG);
//       appendInstruction(new ImmediateInstruction(Opcodes.TNEI, reg, treg, 0));
//       treg = reg;
//     }
    
    TripsBranch brt = new TripsBranch(Opcodes.BRO, lab, 1, treg, pred1);
    brt.addTarget(lab, 0);
    lab.setReferenced();
    appendInstruction(brt);
  }
  
  /**
   * Generate an unconditional branch to the label specified.
   */
  public void generateUnconditionalBranch(Label lab)
  {
    TripsBranch inst = new TripsBranch(Opcodes.BRO, lab, 1);

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
    localVarSize += Machine.alignTo(registers.registerSize(reg), SAVED_REG_SIZE);
    localVar.addElement(disp);
    return disp;
  }

  /**
   * The insertSpillLoad(int, Object, Instruction) method should be used.
   * @see #insertSpillStore(int, Object, Hyperblock)
   */
  public Instruction insertSpillLoad(int reg, Object spillLocation, Instruction after)
  {
    throw new scale.common.InternalError("Use insertSpillLoad(int, Object, Hyperblock).");
  }
  
  /**
   * Insert the instruction(s) to restore a spilled register.  At this
   * point, we are using a one-to-one mapping between real registers
   * and virtual registers, so only a single instruction is required.
   * @param reg specifies which virtual register will be loaded
   * @param spillLocation specifies the offset on the stack to the spill location
   * @param hb
   * @see #getSpillLocation
   */
  protected void insertSpillLoad(int reg, Object spillLocation, Hyperblock hb)
  { 
    StackDisplacement disp  = (StackDisplacement) spillLocation;
    Instruction       load  = new LoadInstruction(Opcodes._LDSPILL, reg, stkPtrReg, disp);
    PredicateBlock    start = hb.getFirstBlock();
    Instruction       after = start.getFirstInstruction(); 
    
    start.insertInstructionAfter(after, load);
    hb.addSpill(start, load);
    
    if (!blocksWithSpills.contains(hb))
      blocksWithSpills.add(hb);
  }

  /**
   * The insertSpillStore(int, Object, Instruction) method should be used.
   * @see #insertSpillStore(int, Object, Hyperblock)
   */
  public Instruction insertSpillStore(int reg, Object spillLocation, Instruction after)
  { 
    throw new scale.common.InternalError("Use insertSpillStore(int, Object, Hyperblock).");
  }

  /**
   * Insert the instruction(s) to save a spilled register. 
   * @param reg specifies which virtual register will be stored
   * @param spillLocation specifies the offset on the stack to the
   * spill location
   * @param hb
   * @see #getSpillLocation
   */  
  protected void insertSpillStore(int reg, Object spillLocation, Hyperblock hb)
  { 
    StackDisplacement disp  = (StackDisplacement) spillLocation;
    Stack<Node>       wl    = new Stack<Node>();
    PredicateBlock    head  = hb.getFirstBlock();
    
    head.nextVisit();
    head.setVisited();
    wl.add(head);
    
    while (!wl.isEmpty()) {
      PredicateBlock b = (PredicateBlock) wl.pop();
      b.pushOutEdges(wl);
      
      for (Instruction inst = b.getFirstInstruction(); inst != null; inst = inst.getNext()) {
        int ra = inst.getDestRegister();
        if (ra == reg) { 
          Instruction store = new StoreInstruction(Opcodes._SDSPILL, disp, stkPtrReg, reg);
          if (b.isPredicated())
            store.setPredicate(b.getPredicate(), b.isPredicatedOnTrue());
          b.insertInstructionAfter(inst, store);
          hb.addSpill(b, store);  
        }
      }
    }
    
    if (!blocksWithSpills.contains(hb))
      blocksWithSpills.add(hb);
  }

  /**
   * Called at the end of code generation for a routine to generate
   * the stackframe.  This method is responsible for inserting any
   * prolog or epilog instructions required.  It may also adjust stack
   * offsets.
   * <p>
   * For a called routine, the stack looks like:<br>
   * <img src="../../../trips_stack.jpg" alt="Trips call stack"><br>
   * Note - all locations are accessed using offsets from the
   * <tt>SP</tt> register.
   */
  protected void endRoutineCode(int[] regMap)
  {  
    int   nrr = registers.numRealRegisters();
    int   n   = regMap.length;

    if (callsRoutine) // Callee needs place to save args if it uses va_start.
      argBuildSize += MAX_ARG_REGS * SAVED_REG_SIZE;

    argBuildSize = (int) Machine.alignTo(argBuildSize, 16);

    // Determine which registers need to be saved.

    boolean[] newmap = new boolean[nrr];
    for (int i = nrr; i < n; i++) {
      int rr = regMap[i];
      if (rr < nrr) /* In case of debugging. */
        newmap[rr] = true;
    }

    long mask = 0;
    if (usesAlloca) {
      newmap[Trips2RegisterSet.FP_REG] = true;
      mask |= (1L << 63); // Set "uses frame pointer flag".
    }

    short[] calleeSaves  = registers.getCalleeSaves();
    int     fr           = -1;
    int     lr           = -2;
    int     numToSave    = 0;
    int     savedRegSize = 0;

    for (int i = 0; i < calleeSaves.length; i++) { // See if any used in this routine.
      int reg = calleeSaves[i];
      
      if (!newmap[reg])
        continue;
      
      if (fr < 0)
        fr = reg;

      numToSave++;
      mask |= (1L << (reg - fr));
      savedRegSize += SAVED_REG_SIZE;
      lr = reg;
    }

    int srOffset  = argBuildSize + ARG_SAVE_OFFSET;        // Offset from SP to saved registers.
    int x         = savedRegSize + localVarSize + srOffset + 15;
    int frameSize = (x / 16) * 16; // Frame size must be a multiple of 16.

    // Generate the prologue for this function

    generatePrologue(frameSize, numToSave, fr, lr, mask);
    
    // Generate the epilogue for this function.
    
    generateEpilogue(frameSize, numToSave, fr, lr, mask);
    
    if (trace) {
      System.out.print("Calc sp fs:");
      System.out.print(frameSize);
      System.out.print(" sro:");
      System.out.print(srOffset);
      System.out.print(" lvs:");
      System.out.print(localVarSize);
      System.out.print(" abs:");
      System.out.print(argBuildSize);
      System.out.print(" srs:");
      System.out.print(savedRegSize);
      System.out.print(" ");
      System.out.println(currentRoutine.getName());
    }

    // Adjust stack offsets now that we know how big everything is.
    
    adjustStackOffsets(frameSize, srOffset);
  }
  
  private Hyperblock splitPrologueEpilogue(Hyperblock hb, Instruction split, PredicateBlock block)
  {  
    Vector<Hyperblock> hbs   = new Vector<Hyperblock>();
    PredicateBlock     bNew  = block.cut(split, this);
    Hyperblock         hbNew = new Hyperblock(bNew, (Trips2RegisterSet) registers);
        
    hb.findLastBlock();
    hb.determinePredicatesBranches();
    hb.invalidateDomination();     
    
    hbNew.findLastBlock();
    hbNew.determinePredicatesBranches();
    hbs.add(hbNew);
    
    // Insert the new hyperblocks into the HFG.

    HashMap<Instruction, Hyperblock> entries = BlockSplitter.computeEntries(hb, hbs);
    hbs.add(0, hb);  // Keep the list ordered.
    Hyperblock.computeHyperblockFlowGraph(hbs, entries);
    
    if (hb == getReturnBlock())
      setReturnBlock(hbNew);
 
    return hbNew;
  }
  
  /**
   * Generate the prologue of the stack frame.
   */
  private void generatePrologue(int frameSize, int numToSave, int fr, int lr, long mask)
  {
    int               numStores = 0;   
    int[]             readBanks = new int[Trips2RegisterSet.numBanks];  
    Hyperblock        hb        = hbStart;
    PredicateBlock    block     = hb.getFirstBlock();
    Instruction       after     = block.getFirstInstruction();
    Trips2RegisterSet regs      = (Trips2RegisterSet) registers;

    // Put the prologue in its own block.  We will try to merge the
    // blocks together after we generate the stack frame.
    
    splitPrologueEpilogue(hb, after, block);

    // Save caller's stack pointer (will be optimized away later if
    // not needed).

    int callerSP = registers.newTempRegister(RegisterSet.AFIREG);
    after = block.insertInstructionAfter(after,
                                         new GeneralInstruction(Opcodes.MOV,
                                                                callerSP,
                                                                Trips2RegisterSet.SP_REG));
    readBanks[regs.getBank(Trips2RegisterSet.SP_REG)]++;

    // Update the stack pointer to point to this function's stack
    // frame.  If the frame size is small enough we will use ADDI's.
    // (Will be optimized away later if not needed).

    if (frameSize > (Trips2Machine.maxImmediate * 4)) {
      int tr = registers.newTempRegister(RegisterSet.AFIREG);
      after = block.insertInstructionAfter(after, generateEnterEndRoutine(-frameSize, tr)); 
      after = block.insertInstructionAfter(after,
                                           new GeneralInstruction(Opcodes.ADD,
                                                                  Trips2RegisterSet.SP_REG,
                                                                  Trips2RegisterSet.SP_REG,
                                                                  tr));
    } else {
      int fs = frameSize;
      while (fs > 0) {
        int inc = (fs > Trips2Machine.maxImmediate) ? Trips2Machine.maxImmediate : fs;
        after = block.insertInstructionAfter(after,
                                             new ImmediateInstruction(Opcodes.ADDI,
                                                                      Trips2RegisterSet.SP_REG,
                                                                      Trips2RegisterSet.SP_REG,
                                                                      -inc));
        fs -= inc;
      }
    } 

    // Write the parts of the link area as needed.

    // Save address of caller's stack frame.  This is always needed.
    // Otherwise, in the presence of alloca() a debugger has no
    // chance of finding the frame pointer value.
    
    after = block.insertInstructionAfter(after,
                                         new StoreInstruction(Opcodes.SD,
                                                              0,
                                                              Trips2RegisterSet.SP_REG,
                                                              callerSP));
    numStores++;
    
    // A debugger can find the return address value by seeing if there
    // is a store of the return address register.  If not, the return
    // address is in the return address register.  If so, then the
    // return address is in the location to which it was stored.

    if (callsRoutine) { // Save callee's return address.
      after = block.insertInstructionAfter(after,
                                           new StoreInstruction(Opcodes.SD,
                                                                8,
                                                                Trips2RegisterSet.SP_REG,
                                                                Trips2RegisterSet.RA_REG));
      numStores++;
      readBanks[regs.getBank(Trips2RegisterSet.RA_REG)]++;
    }

    // A debugger can determine which non-volatile registers were
    // saved by analyzing the function's prologue.

//     if (genDebugInfo) { // Save saved-register-mask.
//       int mr = registers.newTempRegister(RegisterSet.AFIREG);
//       after = block.insertInstructionAfter(after, generateEnterEndRoutine(mask, mr)); 
//       after = block.insertInstructionAfter(after, new StoreInstruction(Opcodes.SD, 16, Trips2RegisterSet.SP_REG, mr));
//       numStores++;
//     }

    // Save any non-volatile registers that this function modifies.

    long bm       = 1;
    int  sof      = -numToSave * Trips2RegisterSet.IREG_SIZE;
    int  savedReg = -1;
      
    for (int i = fr; i <= lr; i++) {
      boolean save = (0L != (mask & bm));
      bm <<= 1;

      if (!save)
        continue;
 
      int     reg       = i;
      int     bank      = regs.getBank(reg);
      boolean violation = ((readBanks[bank] == Trips2RegisterSet.bankAccesses) ||
                           (numStores == Trips2Machine.maxLSQEntries));
      
      if (!violation) {
        after = block.insertInstructionAfter(after,
                                             new StoreInstruction(Opcodes.SD,
                                                                  sof,
                                                                  callerSP,
                                                                  reg));
        sof += Trips2RegisterSet.IREG_SIZE;
        numStores++;
        readBanks[bank]++;
        savedReg = reg;
      } else {
        // There is a block violation. Split the block and use the last
        // global register saved to communicate the stack pointer across
        // the block boundary.
        
        after = block.insertInstructionAfter(after,
                                             new GeneralInstruction(Opcodes.MOV,
                                                                    savedReg,
                                                                    callerSP));
        hb    = splitPrologueEpilogue(hb, after, block);
        block = hb.getFirstBlock();
        after = block.getFirstInstruction(); // The label
        after = block.insertInstructionAfter(after,
                                             new GeneralInstruction(Opcodes.MOV,
                                                                    callerSP,
                                                                    savedReg));
        
        numStores = 0;
        readBanks = new int[Trips2RegisterSet.numBanks];  
        readBanks[regs.getBank(savedReg)]++;
        readBanks[regs.getBank(Trips2RegisterSet.SP_REG)]++;

        i--;  // Come back to this reg.
        bm >>>= 1;
      }
    }

    if (usesVaStart) { // This function uses va_start - save the args
      int offseti = ARG_SAVE_OFFSET;
      for (int i = Trips2RegisterSet.IF_REG; i <= Trips2RegisterSet.IL_REG; i++) {
        int     bank      = regs.getBank(i);
        boolean violation = ((readBanks[bank] == Trips2RegisterSet.bankAccesses) ||
                             (numStores == Trips2Machine.maxLSQEntries));
        
        if (!violation) {
          after = block.insertInstructionAfter(after,
                                               new StoreInstruction(Opcodes.SD,
                                                                    offseti,
                                                                    callerSP,
                                                                    i));
          offseti += Trips2RegisterSet.IREG_SIZE;
          numStores++;
          readBanks[bank]++;
          savedReg = i;
        } else {          
          // There is a block violation. Split the block and use the last
          // global register saved to communicate the stack pointer across
          // the block boundary.
          
          after = block.insertInstructionAfter(after,
                                               new GeneralInstruction(Opcodes.MOV,
                                                                      savedReg,
                                                                      callerSP));
          hb    = splitPrologueEpilogue(hb, after, block);
          block = hb.getFirstBlock();
          after = block.getFirstInstruction(); // The label
          after = block.insertInstructionAfter(after,
                                               new GeneralInstruction(Opcodes.MOV,
                                                                      callerSP,
                                                                      savedReg));
          
          numStores = 0;
          readBanks = new int[Trips2RegisterSet.numBanks];  
          readBanks[regs.getBank(savedReg)]++;
          
          i--;  // Come back to this reg.
        }
      }
    }

    if (usesAlloca) // Save stack pointer at routine entry.
      after = block.insertInstructionAfter(after,
                                           new GeneralInstruction(Opcodes.MOV,
                                                                  stkPtrReg,
                                                                  Trips2RegisterSet.SP_REG));

    HyperblockFormation hbf = new HyperblockFormation(this, hbStart, false);
    hbStart = hbf.mergePrologueEpilogue(hbStart);
  }
   
  /**
   * Generate the epilogue of the stack frame.
   */
  private void generateEpilogue(int frameSize, int numToSave, int fr, int lr, long mask)
  {
    // Don't create the epilogue if there is no return instruction.
    
    if (returnBlock == null) {
      if (Debug.debug(1))
        System.out.println("** Warning: " + currentRoutine.getName() + "() does not return.");
      return;
    }
    
    int[]              writeBanks   = new int[Trips2RegisterSet.numBanks]; 
    int                numLoads     = 2; // Keep track of loads used in epilogue (2=SP+LR). 
    Trips2RegisterSet  regs         = (Trips2RegisterSet) registers;    
    Hyperblock         hb           = returnBlock;
    Hyperblock         origEpilogue = returnBlock;
    PredicateBlock     block        = hb.getLastBlock();
    Instruction        first        = block.getFirstInstruction();
    TripsBranch        ret          = null;
    Instruction        after        = null;
       
    // Insert the epilogue before the return instruction.

    for (Instruction inst = first, prev = null; inst != null; prev = inst, inst = inst.getNext()) {
      if (inst.getOpcode() == Opcodes.RET) {
        after = prev;
        ret   = (TripsBranch) inst;
        break;
      }
    }
       
    boolean split = (after != null) || (block != hb.getFirstBlock());
   
    // If the return is the only instruction in the block, we insert a
    // marker at the beginning of the block so we can insert after it.
    
    if (after == null) {
      after = new CommentMarker("epilogue");
      block.insertInstructionAtHead(after);
    } 
    
    // Generate the epilogue in its own block.
    
    if (split) {  
      hb    = splitPrologueEpilogue(hb, after, block);
      block = hb.getLastBlock();
      after = block.getFirstInstruction();
    }
   
    if (callsRoutine) {  // Restore the return address.
      after = block.insertInstructionAfter(after,
                                           new LoadInstruction(Opcodes.LD,
                                                               Trips2RegisterSet.RA_REG,
                                                               Trips2RegisterSet.SP_REG,
                                                               8));
      numLoads++;
      writeBanks[regs.getBank(Trips2RegisterSet.RA_REG)]++;
      
      // Since we generate the return instruction, before we generate
      // the stack frame, RA_REG looks like it is live in to the block
      // with the return instruction.  SSA renamed the return
      // instruction and it no longer references RA_REG.  We have to
      // rename the return instruction here.
 
      ret.remapSrcRegister(ret.getRb(), Trips2RegisterSet.RA_REG);
    }
    
    if (usesAlloca)  // Restore stack pointer after alloca().
      after = block.insertInstructionAfter(after,
                                           new GeneralInstruction(Opcodes.MOV,
                                                                  Trips2RegisterSet.SP_REG,
                                                                  stkPtrReg));
    
    // Restore the caller's stack pointer.
    
    writeBanks[regs.getBank(Trips2RegisterSet.SP_REG)]++;

    if (frameSize > Trips2Machine.maxImmediate * 4) {
      int tr = registers.newTempRegister(RegisterSet.AFIREG);
      after = block.insertInstructionAfter(after, generateEnterEndRoutine(frameSize, tr)); 
      after = block.insertInstructionAfter(after, new GeneralInstruction(Opcodes.ADD,
                                                                         Trips2RegisterSet.SP_REG,
                                                                         Trips2RegisterSet.SP_REG,
                                                                         tr));
    } else {
      int fs = frameSize;
      while (fs > 0) {
        int inc  = (fs > Trips2Machine.maxImmediate) ? Trips2Machine.maxImmediate : fs;
        after = block.insertInstructionAfter(after,
                                             new ImmediateInstruction(Opcodes.ADDI,
                                                                      Trips2RegisterSet.SP_REG,
                                                                      Trips2RegisterSet.SP_REG,
                                                                      inc));
        fs -= inc;
      }
    } 
    
    long bm  = 1;
    int  sof = -numToSave * Trips2RegisterSet.IREG_SIZE;

    for (int i = fr; i <= lr; i++) {
      boolean save = (0L != (mask & bm));
      bm <<= 1;
      if (!save)
        continue;

      int     reg       = i;
      int     bank      = regs.getBank(reg);
      boolean violation = ((writeBanks[bank] == Trips2RegisterSet.bankAccesses) ||
                           (numLoads == Trips2Machine.maxLSQEntries));

      if (!violation) {
        after = block.insertInstructionAfter(after,
                                             new LoadInstruction(Opcodes.LD,
                                                                 reg,
                                                                 Trips2RegisterSet.SP_REG,
                                                                 sof));
        sof += Trips2RegisterSet.IREG_SIZE;
        numLoads++;
        writeBanks[bank]++;   
      } else {
        hb         = splitPrologueEpilogue(hb, after, block);
        block      = hb.getLastBlock();
        after      = block.getFirstInstruction();
        numLoads   = 2; // 2 = SP + LR
        writeBanks = new int[Trips2RegisterSet.numBanks];  
 
        i--;  // Come back to this reg.
        bm >>>= 1;
      }
    }
    
    // Try to merge the epilogue.
    
    HyperblockFormation hbf = new HyperblockFormation(this, hbStart, false);
    hbStart = hbf.mergeEpilogue(origEpilogue);
  }

  /**
   * Adjust stack offsets after the stack frame size is known.
   */
  private void adjustStackOffsets(int frameSize, int srOffset)
  {
    int lsv = localVar.size();
    for (int i = 0; i < lsv; i++) {
      StackDisplacement disp = localVar.elementAt(i);
      disp.adjust(srOffset);
    }

    int lpv = paramVar.size();
    for (int i = 0; i < lpv; i++) {
      StackDisplacement disp = paramVar.elementAt(i);
      disp.adjust(frameSize + ARG_SAVE_OFFSET);
    }
  }
  
  protected Branch genFtnCall(String name, short[] uses, short[] defs)
  {
    int          handle = allocateTextArea(name, TEXT);
    Displacement disp   = new SymbolDisplacement(name, handle);
    Label        lab    = createLabel();
    Branch       jsr    = new TripsBranch(Opcodes.CALLO, (SymbolDisplacement) disp, 1);

    associateDispWithArea(handle, disp);

    // Place current location into return register
    generateEnterB(new LabelDisplacement(lab), Trips2RegisterSet.RA_REG);

    lastInstruction.specifySpillStorePoint();

    jsr.additionalRegsUsed(uses);
    jsr.additionalRegsKilled(registers.getCalleeUses());
    jsr.additionalRegsSet(defs);
    jsr.addTarget(lab, 0);
    jsr.markAsCall();

    lastInstruction.specifySpillStorePoint();
    appendInstruction(jsr);
    appendLabel(lab);
    lab.markAsFirstInBasicBlock();
    lab.setReferenced();
    callsRoutine = true;
    return jsr;
  }

  /**
   * Set up the arguments to a binary function routine (e.g., divide). 
   */
  private void doBinaryCallArgs(BinaryExpr c)
  {
    Expr la = c.getOperand(0);
    Type lt = processType(la);
    int  lr = Trips2RegisterSet.IF_REG;
    Expr ra = c.getOperand(1);
    Type rt = processType(ra);
    int  rr = Trips2RegisterSet.IF_REG + 1;

    if (lt.isRealType()) {
      lr = Trips2RegisterSet.FF_REG;
    }

    if (rt.isRealType()) {
      rr = Trips2RegisterSet.FF_REG + 1;
    }

    needValue(la);
    int laReg = resultReg;

    needValue(ra);

    genRegToReg(laReg, lr);
    genRegToReg(resultReg, rr);
  }

  /**
   * Generate an integer-literal operate instruction.
   */
  private void doIntOperate(int opcode, int src, long value, int dest)
  {
    appendInstruction(new ImmediateInstruction(Opcodes.getIntImmOp(opcode), dest, src, value));
  }

  public void visitAbsoluteValueExpr(AbsoluteValueExpr e)
  {
    Expr op  = e.getArg();
    int  reg = registers.getResultRegister(processType(e).getTag());
    Type ot  = processType(op);
    
    if (ot.isComplexType()) {
      short[] uses = callArgs(e.getOperandArray(), false);
      genFtnCall("f__cabs", uses, null); // Call f2c routine.
      genRegToReg(Trips2RegisterSet.FR_REG, reg);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = reg;
      return;
    }
    
    needValue(op);
    int raReg = resultReg;

    if (ot.isRealType()) {
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, reg, raReg, 1)); 
      appendInstruction(new ImmediateInstruction(Opcodes.SRLI, reg, reg, 1)); 
    } else {
      int ir = registers.newTempRegister(RegisterSet.AFIREG);
      int zr = generateEnter(0);
      appendInstruction(new ImmediateInstruction(Opcodes.TLTI, ir, raReg, 0));
      appendInstruction(new GeneralInstruction(Opcodes.MOV, reg, raReg, ir, false));
      appendInstruction(new GeneralInstruction(Opcodes.SUB, reg, zr, raReg, ir, true));
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

    // Lay out the arguments in memory.

    if (rt.isAggregateType() || rt.isArrayType()) {
      structAddress = registers.newTempRegister(RegisterSet.AFIREG);
      structSize = rt.memorySizeAsInt(machine);
      genRegToReg(Trips2RegisterSet.IF_REG + nextArg++, structAddress);
    }

    int l = pt.numFormals();
    for (int i = 0; i < l; i++) {
      if (nextArg >= MAX_ARG_REGS)
        break;

      FormalDecl fd = pt.getFormal(i);
      Type       vt = processType(fd);

      if (fd instanceof UnknownFormals)
        break;

      int      ts  = vt.memorySizeAsInt(machine);
      boolean  vr  = vt.isRealType();
      Assigned loc = fd.getStorageLoc();

      if (loc == Assigned.IN_REGISTER) {
        int        reg  = fd.getValueRegister();
        ResultMode mode = fd.valueRegMode();

        if (mode == ResultMode.STRUCT_VALUE) {
          int src  = Trips2RegisterSet.IF_REG;
          while (ts > 0) {
            genRegToReg(src + nextArg++, reg++);
            ts -= Trips2RegisterSet.IREG_SIZE;
          }
          continue;
        }

        // If the argument is not referenced we dont want to generate
        // code for it.
        if (!fd.isReferenced()) {
          nextArg++;
          continue;
        }
        
        appendInstruction(new GeneralInstruction(Opcodes.MOV,
                                                 reg,
                                                 Trips2RegisterSet.FF_REG + nextArg));
        if (!vr) {
          int treg = convertIntRegValue(reg,
                                        Trips2RegisterSet.IREG_SIZE,
                                        vt.isSigned(),
                                        reg,
                                        ts,
                                        vt.isSigned());
          genRegToReg(treg, reg);
        }
        nextArg++;
        continue;
      }

      if (usesVaStart)
        continue;

      if (loc == Assigned.ON_STACK) {
        Displacement disp = fd.getDisplacement();

        if (vt.isAtomicType() ||
            (vt.getCoreType().isFortranCharType() && vt.getCoreType().canBeInRegister())) {
          int adr = registers.newTempRegister(RegisterSet.AFIREG);
          appendInstruction(new ImmediateInstruction(Opcodes.ADDI, adr, stkPtrReg, disp));
          storeIntoMemoryWithOffset(Trips2RegisterSet.IF_REG + nextArg, adr, 0, ts, 0, vr);
          nextArg++;
          continue;
        }

        if (vt.isAggregateType()) {
          int inc = (ts + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;
          int adr = registers.newTempRegister(RegisterSet.AFIREG);
          int x   = MAX_ARG_REGS - nextArg;

          appendInstruction(new ImmediateInstruction(Opcodes.ADDI, adr, stkPtrReg, disp));

          if (x < inc) {
            storeIntoMemoryWithOffset(Trips2RegisterSet.IF_REG + nextArg,
                                      adr,
                                      0,
                                      x * SAVED_REG_SIZE,
                                      0,
                                      vr);
            nextArg += x;
          } else {
            storeIntoMemoryWithOffset(Trips2RegisterSet.IF_REG + nextArg, adr, 0, ts, 0, vr);
            nextArg += inc;
          }
          continue;
        }
      }

      throw new scale.common.InternalError("Argument is where " + fd);
    }

    lastInstruction.specifySpillStorePoint();
  }

  public void visitBitComplementExpr(BitComplementExpr e)
  {
    Type ct  = processType(e);
    int  bs  = ct.memorySizeAsInt(machine);
    Expr arg = e.getOperand(0);
    int  ir  = registers.getResultRegister(RegisterSet.AFIREG);

    needValue(arg);
    int src = resultReg;

    appendInstruction(new ImmediateInstruction(Opcodes.XORI, ir, src, -1)); // XORI w/-1 == NOT
    if ((bs <= 4) && !ct.isSigned())
      ir = convertIntRegValue(ir, Trips2RegisterSet.IREG_SIZE, false, ir, bs, false);
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  /**
   * Assign arguments to locations (either register or memory).
   * Only the first MAX_ARG_REGS arguments are passed in registers,
   * the remaining arguments are passed on the stack.
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
    int stkpos  = ARG_SAVE_OFFSET;

    if (retStruct) {
      amap[0] = Trips2RegisterSet.IF_REG;
      umap[0] = (short) Trips2RegisterSet.IF_REG;
      rk = 1;
      nextArg = 1;
      stkpos += SAVED_REG_SIZE;
    }

    int ai = 0;
    while (nextArg < MAX_ARG_REGS) {
      if (ai >= args.length)
        break;

      Expr arg  = args[ai++];
      Type vt   = processType(arg);
      int  size = vt.memorySizeAsInt(machine);
      int  x    = ((size + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE) * SAVED_REG_SIZE;

      if (trace)
        System.out.println("CFA: " + arg);

      if (vt.isAtomicType()) {
        if (vt.isComplexType()) { // Treat complex as structs for intrinsic routines.
          int nexr = Trips2RegisterSet.FF_REG + nextArg;
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
            storeIntoMemoryWithOffset(resultReg + 1,
                                      Trips2RegisterSet.SP_REG,
                                      stkpos,
                                      size / 2,
                                      0,
                                      true);
            offset += Trips2RegisterSet.FREG_SIZE;
          }
          stkpos += 2 * Trips2RegisterSet.IREG_SIZE;
          continue;
        }

        int nexr = Trips2RegisterSet.IF_REG + nextArg;

        if (simple)
          registers.setResultRegister(nexr);

        needValue(arg);

        if (simple)
          registers.setResultRegister(-1);

        // Postpone transfer because of things like divide
        // which may cause a subroutine call.

        amap[rk] = resultReg;
        umap[rk] = (short) nexr;
        ++rk;
        ++nextArg;
        stkpos += Trips2RegisterSet.IREG_SIZE;
        continue;
      }

      if (vt.isAggregateType()) {
        arg.visit(this);
        int        address    = resultReg;
        long       addressoff = resultRegAddressOffset;
        int        addressaln = resultRegAddressAlignment;
        ResultMode addressha  = resultRegMode;

        if (addressha == ResultMode.STRUCT_VALUE) {
          while (size > 0) {
            if (nextArg < MAX_ARG_REGS) {
              int nexr = Trips2RegisterSet.IF_REG + nextArg;
              int areg = address++;
              amap[rk] = areg;
              umap[rk] = (byte) nexr;
              rk++;
              nextArg++;
              stkpos += Trips2RegisterSet.IREG_SIZE;
              size -= Trips2RegisterSet.IREG_SIZE;
            } else {
              int nexr = address++;
              storeIntoMemoryWithOffset(nexr,
                                        Trips2RegisterSet.SP_REG,
                                        stkpos,
                                        size,
                                        0,
                                        false);
              offset += x;
              stkpos += x;
              break;
            }
          }
          continue;
        }

        assert (addressha == ResultMode.ADDRESS) : "Huh " + arg;

        int k = 0;
        while (size > 0) {
          if (nextArg < MAX_ARG_REGS) {
            int ldsize = (size > SAVED_REG_SIZE) ? SAVED_REG_SIZE : size;
            int nexr   = Trips2RegisterSet.IF_REG + nextArg;
            int areg   = simple ? nexr :  registers.newTempRegister(RegisterSet.INTREG);
            loadFromMemoryWithOffset(areg,
                                     address,
                                     k + addressoff,
                                     ldsize,
                                     addressaln,
                                     false,
                                     false);
            amap[rk] = areg;
            umap[rk] = (byte) nexr;
            ++rk;
            nextArg++;
            size -= ldsize;
            k += SAVED_REG_SIZE;
            stkpos += SAVED_REG_SIZE;
          } else {
            moveWords(address, k, Trips2RegisterSet.SP_REG, stkpos, size, addressaln);
            int xx = ((size + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE) * SAVED_REG_SIZE;
            offset += xx;
            stkpos += xx;
            break;
          }
        }
        continue;
      }

      if (vt.isArrayType()) {
        LoadDeclValueExpr ldve         = (LoadDeclValueExpr) arg;
        VariableDecl      ad           = (VariableDecl) ldve.getDecl();
        int               nexr         = Trips2RegisterSet.IF_REG + nextArg;
        boolean           isPredicated = (((ExprChord) ldve.getChord()).getPredicate() != null);  

        if (simple)
          registers.setResultRegister(nexr);

        putAddressInRegister(ad, isPredicated);

        if (simple)
          registers.setResultRegister(-1);

        amap[rk] = resultReg;
        umap[rk] = (short) nexr;
        ++rk;
        ++nextArg;
        stkpos += Trips2RegisterSet.IREG_SIZE;
        continue;
      }

      throw new scale.common.InternalError("Argument type " + arg);
    }

    while (ai < args.length) {
      Expr arg  = args[ai++];
      Type vt   = processType(arg);
      int  size = vt.memorySizeAsInt(machine);
      int  x    = ((size + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE) * SAVED_REG_SIZE;

      if (vt.isAtomicType()) {
        needValue(arg);

        storeIntoMemoryWithOffset(resultReg,
                                  Trips2RegisterSet.SP_REG,
                                  stkpos + SAVED_REG_SIZE - size,
                                  size,
                                  0,
                                  vt.isRealType());
        offset += Machine.alignTo(size, Trips2RegisterSet.IREG_SIZE);
        stkpos += x;
        continue;
      }

      if (vt.isAggregateType()) {
        arg.visit(this);
        int        address    = resultReg;
        long       addressoff = resultRegAddressOffset;
        int        addressaln = resultRegAddressAlignment;
        ResultMode addressha  = resultRegMode;

        if (addressha == ResultMode.STRUCT_VALUE) {
          storeIntoMemoryWithOffset(address, Trips2RegisterSet.SP_REG, stkpos, size, 0, false);
          offset += x;
          stkpos += x;
          continue;
        }

        assert (addressha == ResultMode.ADDRESS) : "Huh " + arg;

          int tr = registers.newTempRegister(RegisterSet.INTREG);
          int l  = (size + SAVED_REG_SIZE - 1) / SAVED_REG_SIZE;
          for (int i = 0; i < l; i++) {
            loadFromMemoryWithOffset(tr,
                                     address,
                                     addressoff + i * 8,
                                     SAVED_REG_SIZE,
                                     addressaln,
                                     false,
                                     false);
          storeIntoMemoryWithOffset(tr, Trips2RegisterSet.SP_REG, stkpos, SAVED_REG_SIZE, 0, false);
          offset += SAVED_REG_SIZE;
          stkpos += SAVED_REG_SIZE;
        }
        continue;
      }

      if (vt.isArrayType()) {
        LoadDeclValueExpr ldve         = (LoadDeclValueExpr) arg;
        VariableDecl      ad           = (VariableDecl) ldve.getDecl();
        boolean           isPredicated = (((ExprChord) ldve.getChord()).getPredicate() != null);
        putAddressInRegister(ad, isPredicated);
        storeIntoMemoryWithOffset(resultReg,
                                  Trips2RegisterSet.SP_REG,
                                  stkpos,
                                  Trips2RegisterSet.IREG_SIZE,
                                  0,
                                  false);
        offset += Machine.alignTo(size, Trips2RegisterSet.IREG_SIZE);
        stkpos += SAVED_REG_SIZE;
        continue;
      }

      throw new scale.common.InternalError("Argument type " + arg);
    }

    // Move arguments to the register they are passed in if necessary.

    for (int i = 0; i < rk; ++i)
      genRegToReg(amap[i], umap[i]);

    if (offset > argBuildSize)
      argBuildSize = offset;

    // Create the set of used registers for this function call.

    short[] uses = new short[rk + (usesAlloca ? 3 : 2)];
    System.arraycopy(umap, 0, uses, 0, rk);
    uses[rk + 0] = Trips2RegisterSet.SP_REG;
    uses[rk + 1] = Trips2RegisterSet.RA_REG;
    if (usesAlloca)
      uses[rk + 2] = Trips2RegisterSet.FP_REG;

    return uses;
  }

  /**
   * Generate a prefetch instruction.  __builtin_prefetch takes from
   * one to three aarguments.  The first argument is the address to
   * prefetch.  The second (optional) argument specifies whether a
   * read or a write is expected after the prefetch.  A value of zero
   * (default) indicates a read and a value of non-zero indicates a
   * write.  The third (optional) argument specifies the locality and
   * defaults to zero: the higher the value, the higher the temporal
   * locality.
   */
  private void generatePrefetch(Expr[] args)
  {
    needValue(args[0]);
    appendInstruction(new LoadInstruction(Opcodes._LPF, 0, resultReg, 0));
  }

  /**
   * This method generates instructions to call a sub-function.  It
   * places arguments in the appropriate registers or on the stack as
   * required.
   */
  public void visitCallFunctionExpr(CallFunctionExpr e)
  {
    Type        rt        = processType(e);       // function return type
    Expr[]      args      = e.getArgumentArray();
    Expr        ftn       = e.getFunction();
    Declaration decl      = null;
    boolean     retStruct = false;
    int         ir        = registers.getResultRegister(rt.getTag());

    // Check if there is an intrinsic this function could be replaced with.
    
    if (ftn instanceof LoadDeclAddressExpr) {
      decl = ((LoadExpr) ftn).getDecl();
      String fname = decl.getName();
      
      if (fname.equals("_scale_setjmp") || fname.equals("__builtin_setjmp")) {
        RoutineDecl        rd = (RoutineDecl) decl;
        int                handle = ((SymbolDisplacement) rd.getDisplacement()).getHandle();
        SymbolDisplacement symDisp = new SymbolDisplacement("setjmp", handle);
        rd.setDisplacement(symDisp);
      } else if (fname.equals("_scale_longjmp") || fname.equals("__builtin_longjmp")) {
        RoutineDecl        rd = (RoutineDecl) decl;
        int                handle = ((SymbolDisplacement) rd.getDisplacement()).getHandle();
        SymbolDisplacement symDisp = new SymbolDisplacement("longjmp", handle);
        rd.setDisplacement(symDisp);
      } else if (fname.equals("_scale_prefetch") || fname.equals("__builtin_prefetch")) {
        if ((args.length >= 1) && (args.length <= 3)) {
          generatePrefetch(args);
          return;
        }
      }
    }

    if (!rt.isVoidType() && !rt.isAtomicType())
      retStruct = true;

    short[] uses = callArgs(args, retStruct); // Assign the arguments to their locations (either register or memory).

    // If the successor chord has already been generated, we want to
    // use its label, but only if it is not an assignment OR it is an
    // assignment but current chord is it's only predecessor.
    //
    // Specifically we do NOT want to use the label if the current
    // chord is an assignment (e.g. s=call();) AND the successor chord
    // has more than one in edge.  In that case we need to place the
    // assignment in it's own block.
    
    boolean genLabel = false;
    Label   lab      = null;
    Chord   c1       = e.getChord();
    Chord   nxt      = getBranchTarget(c1.getNextChord());
    if (nxt != null) {
      if (nxt.getLabel() > 0)
        if (!c1.isAssignChord())
          lab = getBranchLabel(nxt);
        else if (nxt.numInCfgEdges() == 1) {
          lab = getBranchLabel(nxt);
          // Necessary to force appendCallInstruction to append the label
          // before the assignment.
          genLabel = true;
          nxt.setLabel(0);
        }
    }
    if (lab == null) {
      lab = createLabel();
      genLabel = true;
    }

    // callArgs() may generate code to call another function (such as
    // runtime support for interger divide). If a hidden argument is
    // being used to pass the return address to a function, the call
    // created by callArgs() will destroy this hidden argument.  We
    // avoid this by setting up the hidden argument after callArgs()
    // has completed.
    
    if (retStruct) {
      // If the routine returns a structure, it is passed through
      // memory and the memory address to use is the first argument.

      StackDisplacement returnDisp = new StackDisplacement(localVarSize);
      appendInstruction(new ImmediateInstruction(Opcodes.ADDI,
                                                 Trips2RegisterSet.IF_REG,
                                                 stkPtrReg,
                                                 returnDisp));
      localVarSize += Machine.alignTo(rt.memorySizeAsInt(machine), SAVED_REG_SIZE);
      localVar.addElement(returnDisp);
    }
     
    
    // Place current location into return register.

    generateEnterB(new LabelDisplacement(lab), Trips2RegisterSet.RA_REG);
    
    TripsBranch jsr;
    if (decl == null) {
      ftn.visit(this);
      jsr = new TripsBranch(Opcodes.CALL, resultReg, 1);
    } else {
      if ("_trips_libcall".equals(decl.getName())) {
        jsr = new TripsBranch(Opcodes.SCALL, 1);
      } else {
        RoutineDecl rd = (RoutineDecl) decl;
        addrDisp = rd.getDisplacement().unique();
        jsr = new TripsBranch(Opcodes.CALLO, (SymbolDisplacement) addrDisp, 1);
      }
    }

    callsRoutine = true;

    // Handle return values.
    
    if (rt.isVoidType()) {
      appendCallInstruction(jsr, lab, uses, registers.getCalleeUses(), null, genLabel);
      lab.setReferenced();
      return;
    }

    resultRegAddressOffset = 0;

    if (!rt.isAtomicType()) {
      // Returning a structure.
      
      appendCallInstruction(jsr, lab, uses, registers.getCalleeUses(), null, genLabel);
      lab.setReferenced();
      resultRegMode = ResultMode.ADDRESS;
      resultReg = Trips2RegisterSet.IF_REG;
      resultRegAddressAlignment = 8;
      return;
    }

    resultRegMode = ResultMode.NORMAL_VALUE;

    if (!rt.isRealType()) {
      appendCallInstruction(jsr, lab, uses, registers.getCalleeUses(), intReturn, genLabel);
      lab.setReferenced();
      resultReg = Trips2RegisterSet.IR_REG;
      return;
    }

    if (!rt.isComplexType()) {
      appendCallInstruction(jsr, lab, uses, registers.getCalleeUses(), realReturn, genLabel);
      lab.setReferenced();
      resultReg = Trips2RegisterSet.FR_REG;
      return;
    }

    appendCallInstruction(jsr, lab, uses, registers.getCalleeUses(), complexReturn, genLabel);
    lab.setReferenced();
    
    genRegToReg(Trips2RegisterSet.FR_REG, ir);
    genRegToReg(Trips2RegisterSet.FR_REG + 1, ir + 1);
      
    resultReg = ir;
  }

  private void gen3WayFltCompare(int laReg, int raReg, int pReg, int dest)
  {
    int tr   = registers.newTempRegister(RegisterSet.AFIREG);

    appendInstruction(new GeneralInstruction(Opcodes.FEQ, pReg, laReg, raReg));
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, 0, pReg, true));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, tr, laReg, pReg, false));
    appendInstruction(new GeneralInstruction(Opcodes.FLT, pReg, tr, raReg));
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, -1, pReg, true));
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, 1, pReg, false));
  }

  private void gen3WayIntCompare(int laReg, int raReg, int pReg, int dest)
  {
    int tr   = registers.newTempRegister(RegisterSet.AFIREG);

    appendInstruction(new GeneralInstruction(Opcodes.TEQ, pReg, laReg, raReg));
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, 0, pReg, true));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, tr, laReg, pReg, false));
    appendInstruction(new GeneralInstruction(Opcodes.TLT, pReg, tr, raReg));
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, -1, pReg, true));
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, 1, pReg, false));
  }

  public void visitCompareExpr(CompareExpr e)
  {
    Expr la   = e.getOperand(0);
    Expr ra   = e.getOperand(1);
    Type rt   = processType(la);
    int  mode = e.getMode();
    int  ir   = registers.getResultRegister(RegisterSet.AFIREG);

    needValue(la);
    int laReg = resultReg;
    needValue(ra);
    int raReg = resultReg;

    switch (mode) {
    case CompareExpr.Normal:    /*      Use normal compare      */
      int tmp2 = registers.newTempRegister(RegisterSet.AFIREG);
      if (rt.isRealType()) {
        gen3WayFltCompare(laReg, raReg, tmp2, ir);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      } else {
        gen3WayIntCompare(laReg, raReg, tmp2, ir);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    case CompareExpr.FloatL:    /*      If either argument is Nan, return -1    */
      break;
    case CompareExpr.FloatG:    /*      If either argument is Nan, return 1     */
      break;
    }

    throw new scale.common.NotImplementedError("CompareExpr not converted for floating point " + e);
  }

  private static final int DBYTE        = 0;
  private static final int DSHORT       = 1;
  private static final int DINT         = 2;
  private static final int DLONG        = 3;
  private static final int SBYTE        = DBYTE  << 2;
  private static final int SSHORT       = DSHORT << 2;
  private static final int SINT         = DINT   << 2;
  private static final int SLONG        = DLONG  << 2;
  private static final int SSIGNED      = 1 << 4;
  private static final int DSIGNED      = 1 << 5;
  private static final int[] smapSize   = {-1, SBYTE, SSHORT, -1, SINT, -1, -1, -1, SLONG};
  private static final int[] dmapSize   = {-1, DBYTE, DSHORT, -1, DINT, -1, -1, -1, DLONG};
  private static final int RSRC         = 0; // Return source
  private static final int EBEXT        = 1; // Extract byte & sign extend
  private static final int ESEXT        = 2; // Extract short & sign extend
  private static final int EIEXT        = 3; // Extract int & sign extend
  private static final int EUB          = 4; // Extract unsigned byte
  private static final int EUS          = 5; // Extract unsigned short
  private static final int EUI          = 6; // Extract unsigned int

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
    int which = smapSize[srcSize] + dmapSize[destSize];

    if (srcSigned)
      which += SSIGNED;
    if (destSigned)
      which += DSIGNED;

    switch (ccase[which]) {
    case RSRC: // Return source
      return src;
    case EBEXT: // Extract byte & sign extend
      appendInstruction(new GeneralInstruction(Opcodes.EXTSB, dest, src));
      return dest;
    case ESEXT: // Extract short & sign extend
      appendInstruction(new GeneralInstruction(Opcodes.EXTSH, dest, src));
      return dest;
    case EIEXT: // Extract int & sign extend
      appendInstruction(new GeneralInstruction(Opcodes.EXTSW, dest, src));
      return dest;
    case EUB: // Extract unsigned byte
      appendInstruction(new GeneralInstruction(Opcodes.EXTUB, dest, src));
      return dest;
    case EUS: // Extract unsigned short
      appendInstruction(new GeneralInstruction(Opcodes.EXTUH, dest, src));
      return dest;
    case EUI: // Extract unsigned int
      appendInstruction(new GeneralInstruction(Opcodes.EXTUW, dest, src));
      return dest;
    }
    throw new scale.common.InternalError("Funny conversion " + which);
  }

  /**
   * Generate code to zero out a floating point register.
   */
  protected void zeroFloatRegister(int dest, int destSize)
  {
    genLoadImmediate(0, dest);
  }

  /**
   * Generate code to obtain the real part of a complex value.
   */
  protected void genRealPart(int src, int srcSize, int dest, int destSize)
  {
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, src));
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
    int tr = registers.newTempRegister(RegisterSet.AFIREG);
    appendInstruction(new GeneralInstruction(Opcodes.FDTOI, tr, src));
    return convertIntRegValue(tr, Trips2RegisterSet.IREG_SIZE, true, dest, destSize, destSigned);
  }

  /**
   * Convert a real value in a real register to a
   * real value in a real register.
   */
  protected void genRealToReal(int src, int srcSize, int dest, int destSize)
  {
    if ((srcSize == 8) && (destSize==4)) {
      //  If the program has specified a double-to-float cast, we need
      //  to honor it, but we also need to convert the float back to a
      //  double since TRIPS only supports doubles.
      int tr = registers.newTempRegister(RegisterSet.FLTREG);
      appendInstruction(new GeneralInstruction(Opcodes.FDTOS, tr, src));
      appendInstruction(new GeneralInstruction(Opcodes.FSTOD, dest, tr));
    } else {
      // No conversion is needed since it is guarenteed that all reals
      // in registers are doubles.  Don't use genRegToReg because we may
      // be just getting the real part of a complex value.
      appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, src));
    }
  }

  /**
   * Convert between single and double reals.  Only used on loads and
   * stores to memory.
   */
  protected void genTransformReal(int src, int srcSize, int dest, int destSize)
  {
    if ((destSize == 4) && (srcSize == 8))
      appendInstruction(new GeneralInstruction(Opcodes.FDTOS, dest, src));
    else
      appendInstruction(new GeneralInstruction(Opcodes.FSTOD, dest, src));
  }

  /**
   * Convert real value in a real register to an integer value
   * in a real register.  The result is rounded  to the nearest integer.
   */
  protected void genRealToIntRound(int src, int srcSize, int dest, int destSize)
  {
    int irp = generateEnter(0.5);
    int irm = generateEnter(-0.5);
    int vs0 = generateEnter(0);
    int tr  = registers.newTempRegister(RegisterSet.AFIREG);
    int t2  = registers.newTempRegister(RegisterSet.AFIREG);
    int t3  = registers.newTempRegister(RegisterSet.AFIREG);

    appendInstruction(new GeneralInstruction(Opcodes.FLT, tr, src, vs0));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, t2, irm, tr, true));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, t2, irp, tr, false));
    appendInstruction(new GeneralInstruction(Opcodes.FADD, t3, t2, src));
    appendInstruction(new GeneralInstruction(Opcodes.FDTOI, dest, t3));
  }

  /**
   * Convert real value in a real register to a rounded real value
   * in a real register.  The result is rounded to the nearest integer.
   */
  protected void genRoundReal(int src, int srcSize, int dest, int destSize)
  {
    int t0 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int t1 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.FLTREG, srcSize));
    int t2 = generateEnter(0x3fe0000000000000L);
    int t3 = generateEnter(0x43e0000000000000L);
    int t4 = registers.newTempRegister(RegisterSet.AFIREG);
    
    appendInstruction(new ImmediateInstruction(Opcodes.SLLI, t1, src, 1));
    appendInstruction(new ImmediateInstruction(Opcodes.SRLI, t1, t1, 1)); // abs(src)
    appendInstruction(new ImmediateInstruction(Opcodes.SRLI, t0, src, 63));
    appendInstruction(new ImmediateInstruction(Opcodes.SLLI, t0, t0, 63));
    appendInstruction(new GeneralInstruction(Opcodes.OR, t2, t2, t0)); // 0.5 or -0.5
    appendInstruction(new GeneralInstruction(Opcodes.FADD, dest, src, t2));
    appendInstruction(new GeneralInstruction(Opcodes.FDTOI, t2, dest));
    appendInstruction(new GeneralInstruction(Opcodes.FLT, t4, t1, t3));
    appendInstruction(new GeneralInstruction(Opcodes.FITOD, t2, t2));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, t2, t4, true));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, src, t4, false));
 }

  /**
   * Convert an integer value in an integer register to a
   * real value in a real register.
   */
  protected void genIntToReal(int src, int srcSize, int dest, int destSize)
  {
    appendInstruction(new GeneralInstruction(Opcodes.FITOD, dest, src));
  }

  /**
   * Convert an unsigned integer value in an integer register to a
   * real value in a real register.
   */
  protected void genUnsignedIntToReal(int src, int srcSize, int dest, int destSize)
  {
    if (srcSize < Trips2RegisterSet.IREG_SIZE) {
      appendInstruction(new GeneralInstruction(Opcodes.FITOD, dest, src));
      return;
    }

    int tr  = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int tr1 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int tr2 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int tr5 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int tr6 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int tr7 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int tr8 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int vs0 = generateEnter(0);

    appendInstruction(new GeneralInstruction(Opcodes.TLT, tr, src, vs0));

    int trm2 = registers.newTempRegister(registers.tempRegisterType(RegisterSet.AFIREG, srcSize));
    int trm  = generateEnter(0x000fffffffffffffL);
    
    appendInstruction(new GeneralInstruction(Opcodes.AND, tr2, trm, src));
    appendInstruction(new ImmediateInstruction(Opcodes.XORI, trm2, trm, -1));
    appendInstruction(new GeneralInstruction(Opcodes.AND, tr1, trm2, src));

    int tr3 = genLeftShiftUnsigned(0x43F, 52);
    
    appendInstruction(new GeneralInstruction(Opcodes.FITOD, tr5, tr2));
    appendInstruction(new GeneralInstruction(Opcodes.FITOD, tr6, tr1));
    appendInstruction(new GeneralInstruction(Opcodes.FADD, tr7, tr5, tr3));
    appendInstruction(new GeneralInstruction(Opcodes.FADD, tr8, tr6, tr7, tr, true));
    appendInstruction(new GeneralInstruction(Opcodes.FITOD, tr8, src, tr, false));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, tr8));
  }

  /**
   * Generate floor().
   */
  protected void genFloorOfReal(int src, int srcSize, int dest, int destSize)
  {
    int tr1 = registers.newTempRegister(RegisterSet.AFIREG);
    int tr2 = registers.newTempRegister(RegisterSet.AFIREG);
    int tr3 = generateEnter(0x43e0000000000000L);
    int tr4 = registers.newTempRegister(RegisterSet.AFIREG);

    // if (abs(src) < 9.223372036854776e+18)
    //   return (double) (long) src;
    // else
    //   return src;

    appendInstruction(new GeneralInstruction(Opcodes.FDTOI, tr1, src));  
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, tr2, -1));
    appendInstruction(new ImmediateInstruction(Opcodes.SRLI, tr2, tr2, 1));
    appendInstruction(new GeneralInstruction(Opcodes.AND, tr2, src, tr2));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, src));
    appendInstruction(new GeneralInstruction(Opcodes.FITOD, tr1, tr1));  
    appendInstruction(new GeneralInstruction(Opcodes.FLT, tr4, tr2, tr3));  
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, tr1, tr4, true));
  }

  public void visitExponentiationExpr(ExponentiationExpr e)
  {
    Type    ct = processType(e);
    int     bs = ct.memorySizeAsInt(machine);
    int     ir = registers.getResultRegister(ct.getTag());
    Expr    la = e.getOperand(0);
    Expr    ra = e.getOperand(1);
    boolean cmplx = ct.isComplexType();
    
    int opcode = Opcodes.MUL;
    if (ct.isRealType())
      opcode = Opcodes.FMUL;

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
          appendInstruction(new GeneralInstruction(Opcodes.MOV, ir, resultReg));
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
        if (value == 2) { // X**2
          if (cmplx)
            doComplexOp(MUL, resultReg, resultReg, ir);
          else
            appendInstruction(new GeneralInstruction(opcode, ir, resultReg, resultReg));
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
        if (value == 3) { // X**3
          if (cmplx) {
            int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
            doComplexOp(MUL, resultReg, resultReg, tr);
            doComplexOp(MUL, resultReg, tr, ir);
          } else {
            int tr = registers.newTempRegister(RegisterSet.AFIREG);
            appendInstruction(new GeneralInstruction(opcode, tr, resultReg, resultReg));
            appendInstruction(new GeneralInstruction(opcode, ir, resultReg, tr));
          }
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = ir;
          return;
        }
        if (cmplx) {
          int tr = registers.newTempRegister(registers.tempRegisterType(ct.getCoreType(), bs));
          doComplexOp(MUL, resultReg, resultReg, ir);
          for (int i = 0; i < value - 2; i++)
            doComplexOp(MUL, resultReg, tr, tr);
          doComplexOp(MUL, resultReg, tr, ir);
        } else {
          int tr = registers.newTempRegister(RegisterSet.AFIREG);
          appendInstruction(new GeneralInstruction(opcode, tr, resultReg, resultReg));
          for (int i = 0; i < value - 3; i++)
            appendInstruction(new GeneralInstruction(opcode, tr, resultReg, tr));
          appendInstruction(new GeneralInstruction(opcode, ir, resultReg, tr));
        }
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    Type   lt  = processType(la);
    Type   rt  = processType(ra);
    String ftn = "pow";
    int    rr  = Trips2RegisterSet.FR_REG;

    // Use the __pow function to do the exponentiation.

    if (lt.isIntegerType() && rt.isIntegerType()) {
      ftn = "_pow_ii";
      rr   = Trips2RegisterSet.IR_REG;
    } else if (!rt.isRealType())
      ftn = "_pow_di";


    doBinaryCallArgs(e);
    genFtnCall(ftn,
               genDoubleUse(Trips2RegisterSet.IF_REG, Trips2RegisterSet.IF_REG + 1),
               null);
    genRegToReg(rr, ir);

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitDivisionExpr(DivisionExpr e)
  {
    Type ct = processType(e);
    int  ir = registers.getResultRegister(ct.getTag());

    if (ct.isRealType()) {
      if (softwareFDIV && !ct.isComplexType()) {
        doBinaryCallArgs(e);
        genFtnCall("_float64_div",
                   genDoubleUse(Trips2RegisterSet.FF_REG, Trips2RegisterSet.FF_REG + 1),
                   null);
        genRegToReg(Trips2RegisterSet.FR_REG, ir);
      
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
      } else
        doBinaryOp(e, DIV);
    
      return;
    }

    int  bs = ct.memorySizeAsInt(machine);
    Expr ra = e.getRightArg();
    Expr la = e.getLeftArg();

    if (ra.isLiteralExpr()) {
      LiteralExpr le    = (LiteralExpr) ra;
      Literal     lit   = le.getLiteral();
      int         shift = -1;
      long        value = 0;

      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        value = il.getLongValue();
        shift = Lattice.powerOf2(value);
      } else if (lit instanceof SizeofLiteral) {
        value = valueOf((SizeofLiteral) lit);
        shift = Lattice.powerOf2(value);
      }

      if (shift >= 0) {
        needValue(la);
        dividePower2(value, shift, resultReg, ir, bs > 4, ct.isSigned());
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
 
    int op = ct.isSigned()? Opcodes.DIVS : Opcodes.DIVU;
    appendInstruction(new GeneralInstruction(op, ir, laReg, raReg));
    
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitRemainderExpr(RemainderExpr e)
  {
    Type ct = processType(e);
    int  ir = registers.getResultRegister(ct.getTag());
    Expr ra = e.getRightArg();
    Expr la = e.getLeftArg();

    if (ct.isRealType()) {
      needValue(e.getLeftArg());
      int lr = resultReg;
      needValue(e.getRightArg());
      int rr = resultReg;
      genRegToReg(lr, Trips2RegisterSet.IF_REG);
      genRegToReg(rr, Trips2RegisterSet.IF_REG + 1);
      genFtnCall("fmod", 
                 genDoubleUse(Trips2RegisterSet.IF_REG, Trips2RegisterSet.IF_REG + 1),
                 null);
      genRegToReg(Trips2RegisterSet.FR_REG, ir);
      resultRegAddressOffset = 0;
      resultRegMode = ResultMode.NORMAL_VALUE;
      resultReg = ir;
      return;
    }
    
    if (ra.isLiteralExpr()) {
      LiteralExpr le    = (LiteralExpr) ra;
      Literal     lit   = le.getLiteral().getConstantValue();
      int         shift = -1;

      if (lit instanceof IntLiteral) {
        IntLiteral il = (IntLiteral) lit;
        long value = il.getLongValue();
        shift = Lattice.powerOf2(value);
      }

      if (shift > 0) {
        needValue(la);

        if (shift == 0)
          return;

        long shiftVal = (1L << shift) - 1;
        appendInstruction(new ImmediateInstruction(Opcodes.ANDI, ir, resultReg, shiftVal));
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

    int qr = registers.newTempRegister(RegisterSet.AFIREG);
    int tr = registers.newTempRegister(RegisterSet.AFIREG);
    int op = ct.isSigned() ? Opcodes.DIVS : Opcodes.DIVU;
    appendInstruction(new GeneralInstruction(op, qr, laReg, raReg));
    appendInstruction(new GeneralInstruction(Opcodes.MUL, tr, qr, raReg));
    appendInstruction(new GeneralInstruction(Opcodes.SUB, ir, laReg, tr));
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  protected short[] genSingleUse(int reg)
  {
    short[] uses = new short[usesAlloca ? 4 : 3];
    uses[0] = (short) reg;
    uses[1] = Trips2RegisterSet.SP_REG;
    uses[2] = Trips2RegisterSet.RA_REG;
    if (usesAlloca)
      uses[3] = Trips2RegisterSet.FP_REG;
    return uses;
  }

  protected short[] genDoubleUse(int reg1, int reg2)
  {
    short[] uses = new short[usesAlloca ? 5 : 4];
    uses[0] = (short) reg1;
    uses[1] = (short) reg2;
    uses[2] = Trips2RegisterSet.SP_REG;
    uses[3] = Trips2RegisterSet.RA_REG;
    if (usesAlloca)
      uses[4] = Trips2RegisterSet.FP_REG;
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
    Type    ft        = processType(fd);
    boolean isReal    = ft.isRealType();
    boolean signed    = ft.isSigned();
    int     byteSize  = ft.memorySizeAsInt(machine);
    int     bits      = fd.getBits();
    int     fa        = fd.getFieldAlignment();
    int     bitOffset = fd.getBitOffset();

    if (adrha == ResultMode.STRUCT_VALUE) {
      assert (fieldOffset < 32) : "Field offset too large " + fieldOffset;
      // Structure is in registers, not memory, and is 32 bytes or
      // less in size.

      int ts = (int) adrrs;

      resultReg = dest;
      resultRegAddressOffset = 0;
      resultRegMode = ft.isAtomicType()? ResultMode.NORMAL_VALUE : ResultMode.STRUCT_VALUE;
      resultRegSize = ft.memorySize(machine);

      if (fieldOffset >= 8) {
        fieldOffset -= 8;
        ts -= 8;
        adr++;
      }

      bitOffset += 8 * ((int) fieldOffset);
      if (bits == 0)
        bits = 8 * byteSize;

      if (bits == 64) {
        appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, adr));
        resultRegMode = ResultMode.NORMAL_VALUE;
        return;
      }

      // Data from a structure is always right-aligned in a register.

      if (ts < Trips2RegisterSet.IREG_SIZE)
        bitOffset = (64 - ((ts + addIn[ts]) * 8)) + bitOffset;

      int ls = 64 - bitOffset - bits;

      if (isReal) {
        assert (bits == 32) : "Float size?";
        if (ls == 32) {
          appendInstruction(new ImmediateInstruction(Opcodes.SRLI, dest, adr, 32));
          appendInstruction(new GeneralInstruction(Opcodes.FSTOD, dest, dest));
          return;
        } else if (ls == 0) {
          appendInstruction(new GeneralInstruction(Opcodes.FSTOD, dest, adr));
          return;
        } else
          throw new scale.common.InternalError("Float alignment in struct.");
      }

      if (ls == 0) {
        if (bits == 8) {
          int op = signed ? Opcodes.EXTSB : Opcodes.EXTUB;
          appendInstruction(new GeneralInstruction(op, dest, adr));
          return;
        } else if (bits == 16) {
          int op = signed ? Opcodes.EXTSH : Opcodes.EXTUH;
          appendInstruction(new GeneralInstruction(op, dest, adr));
          return;
        } else if (bits == 32) {
          int op = signed ? Opcodes.EXTSW : Opcodes.EXTUW;
          appendInstruction(new GeneralInstruction(op, dest, adr));
          return;
        }
      }

      int tr = registers.newTempRegister(RegisterSet.INTREG);
      int sr = adr;

      if (bitOffset > 0) {
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr, adr, bitOffset));
        sr = tr;
      }
      int op = signed ? Opcodes.SRAI : Opcodes.SRLI;
      appendInstruction(new ImmediateInstruction(op, dest, sr, 64 - bits));
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

    boolean fieldAligned = !naln && (0 == (adraln % 2));
    if (bits == 0)
      loadFromMemoryWithOffset(dest,
                               adr,
                               fieldOffset,
                               byteSize,
                               fieldAligned ? fa : 1,
                               signed,
                               isReal);
    else
      loadBitsFromMemory(dest,
                         adr,
                         fieldOffset,
                         bits,
                         bitOffset,
                         fieldAligned ? fa : 1,
                         signed);

    resultReg = dest;
  }

  /**
   * Generate a branch based on the value of an expression which is
   * guaranteed to be either 0 or 1.
   * @param treg specifies the register to test
   * @param tc specifies the path if the test succeeds
   * @param fc specifies the path if the test fails
   */
  protected void genTrueFalseBranch(int treg, Chord tc, Chord fc)
  {
    lastInstruction.specifySpillStorePoint();

    Label labt = getBranchLabel(tc);
    Label labf = getBranchLabel(fc);

    Instruction nxtt = labt.getNext();
    if ((nxtt != null) && nxtt.isBranch()) {
      TripsBranch nbr = (TripsBranch) labt.getNext();
      if (!nbr.isPredicated() && (nbr.getOpcode() == Opcodes.BRO))
        labt = nbr.getTarget();
    }
    TripsBranch brt = new TripsBranch(Opcodes.BRO, labt, 1, treg, true, branchPrediction);
    brt.addTarget(labt, 0);
    labt.setReferenced();
    appendInstruction(brt);
    
    Instruction nxtf = labf.getNext();
    if ((nxtf != null) && nxtf.isBranch()) {
      TripsBranch nbr = (TripsBranch) labf.getNext();
      if (!nbr.isPredicated() && (nbr.getOpcode() == Opcodes.BRO))
        labf = nbr.getTarget();
    }
    TripsBranch brf = new TripsBranch(Opcodes.BRO, labf, 1, treg, false, 1.0 - branchPrediction);
    brf.addTarget(labf, 0);
    labf.setReferenced();
    appendInstruction(brf);  
  }

  /**
   * Generate a branch based on the value of an expression.
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
    lastInstruction.specifySpillStorePoint();

    boolean pred1 = true;
    boolean pred2 = false;

    if (which == CompareMode.NE) {
      pred1 = false;
      pred2 = true;
    } else if (which != CompareMode.EQ)
      throw new scale.common.InternalError("Invalid which " + which);

    int reg = registers.newTempRegister(RegisterSet.INTREG);
    appendInstruction(new ImmediateInstruction(Opcodes.TNEI, reg, treg, 0));
    treg = reg;

    if (labt != null) {
      Instruction nxt = labt.getNext();
      if ((nxt != null) && nxt.isBranch()) {
        TripsBranch nbr = (TripsBranch) labt.getNext();
        if (!nbr.isPredicated() && (nbr.getOpcode() == Opcodes.BRO))
          labt = nbr.getTarget();
      }
      TripsBranch brt = new TripsBranch(Opcodes.BRO, labt, 1, treg, pred1, branchPrediction);
      brt.addTarget(labt, 0);
      labt.setReferenced();
      appendInstruction(brt);
    }
    
    if (labf != null) {
      Instruction nxt = labf.getNext();
      if ((nxt != null) && nxt.isBranch()) {
        TripsBranch nbr = (TripsBranch) labf.getNext();
        if (!nbr.isPredicated() && (nbr.getOpcode() == Opcodes.BRO))
          labf = nbr.getTarget();
      }
      TripsBranch brf = new TripsBranch(Opcodes.BRO, labf, 1, treg, pred2, 1.0 - branchPrediction);
      brf.addTarget(labf, 0);
      labf.setReferenced();
      appendInstruction(brf);  
    }
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
    doCompareOp(predicate, which);
    if (rflag) {
      Chord t = tc;
      tc = fc;
      fc = t;
    }
    genTrueFalseBranch(resultReg, tc, fc);
  }

  /**
   * Calculate both the index and offset of an array element.  Return
   * the offset from the address in
   * <code>resultRegAddressOffset</code>.  resultReg is set to the
   * register containing the index value.
   * @param offset is an expression specifying the offset field of the
   * ArrayIndexExpr
   * @param index is an expression specifying the index field of the
   * ArrayIndexExpr
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

      if (negOff) {
        int tr = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new GeneralInstruction(Opcodes.SUB, tr, ind, off));
        resultReg = tr;
      } else {
        int tr = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new GeneralInstruction(Opcodes.ADD, tr, ind, off));
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
        int tr = registers.newTempRegister(RegisterSet.AFIREG);
        int tr2 = generateEnter(0);
        appendInstruction(new GeneralInstruction(Opcodes.SUB, tr, tr2, resultReg));
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
      resultReg = -1;
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
    if (tr == -1)
      tr2 = arr;
    else {
      int tr3;
      tr2 = registers.newTempRegister(RegisterSet.AFIREG);
      switch (bs) {
      case 1:
        appendInstruction(new GeneralInstruction(Opcodes.ADD, tr2, tr, arr));
        break;
      case 4:
        tr3 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr3, tr, 2));
        appendInstruction(new GeneralInstruction(Opcodes.ADD, tr2, tr3, arr));
        break;
      case 8:
        tr3 = registers.newTempRegister(RegisterSet.AFIREG);
        appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr3, tr, 3));
        appendInstruction(new GeneralInstruction(Opcodes.ADD, tr2, tr3, arr));
        break;
      default:
        tr3 = registers.newTempRegister(RegisterSet.AFIREG);
        genMultiplyQuad(bs, tr, tr3);
        appendInstruction(new GeneralInstruction(Opcodes.ADD, tr2, tr3, arr));
        break;
      }
    }

    long offsetl = offseth * bs + offseta;
    loadFromMemoryWithOffset(dest, tr2, offsetl, bs, daln, et.isSigned(), et.isRealType());
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
    if (tr == -1) {
      resultRegAddressOffset += offseth * bs;
      return;
    }

    offseta = resultRegAddressOffset + bs * offseth;
    int arr = resultReg;

    int tr3;
    switch (bs) {
    case 1:
      appendInstruction(new GeneralInstruction(Opcodes.ADD, ir, tr, arr));
      break;
    case 4:
      tr3 = registers.newTempRegister(RegisterSet.AFIREG);
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr3, tr, 2));
      appendInstruction(new GeneralInstruction(Opcodes.ADD, ir, tr3, arr));
      break;
    case 8:
      tr3 = registers.newTempRegister(RegisterSet.AFIREG);
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tr3, tr, 3));
      appendInstruction(new GeneralInstruction(Opcodes.ADD, ir, tr3, arr));
      break;
    default:
      tr3 = registers.newTempRegister(RegisterSet.AFIREG);
      genMultiplyQuad(bs, tr, tr3);
      appendInstruction(new GeneralInstruction(Opcodes.ADD, ir, tr3, arr));
      break;
    }

    resultRegAddressOffset = offseta;
    resultRegMode = ResultMode.ADDRESS_VALUE;
    resultReg = ir;
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
      }

      if (flag) {
        int ir = registers.getResultRegister(ct.getTag());
        needValue(la);
        if (bs > 4) {
          genMultiplyQuad(value, resultReg, ir);
        } else {
          genMultiplyLong(value, resultReg, ir);
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
    Expr arg = e.getOperand(0);
    int  ir  = registers.getResultRegister(ct.getTag());

    needValue(arg);

    int zr = generateEnter(0);
    if (ct.isRealType()) {
      appendInstruction(new GeneralInstruction(Opcodes.FSUB, ir, zr, resultReg));
      if (ct.isComplexType())
        appendInstruction(new GeneralInstruction(Opcodes.FSUB, ir + 1, zr, resultReg + 1));
    } else {
      appendInstruction(new GeneralInstruction(Opcodes.SUB, ir, zr, resultReg));
      if ((bs <= 4) && !ct.isSigned())
        zeroExtend(ir, ir, bs);
    }

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  protected void genAlloca(Expr arg, int reg)
  {
    assert usesAlloca;

    // Obtain the size of the area to be allocated.

    needValue(arg);
    assert (resultRegMode == ResultMode.NORMAL_VALUE);

    // Decrement stack pointer by the size of the allocated area.

    int tr4 = registers.newTempRegister(RegisterSet.AIREG);
    appendInstruction(new GeneralInstruction(Opcodes.SUB, tr4, Trips2RegisterSet.SP_REG, resultReg));

    // New SP value - make sure it's on a 16-byte boundary.

    int tr3 = registers.newTempRegister(RegisterSet.AIREG);
    appendInstruction(new ImmediateInstruction(Opcodes.ANDI, tr3, tr4, -16));

    // Move the link area (back chain, et al) to the new location.

    moveWords(Trips2RegisterSet.SP_REG, 0, tr3, 0, ARG_SAVE_OFFSET, 16);

    // Update the SP register to the new value.

    appendInstruction(new GeneralInstruction(Opcodes.MOV, Trips2RegisterSet.SP_REG, tr3));

    // Calculate the allocated area address (account for the saved
    // parameter area & link area).

    StackDisplacement disp = new StackDisplacement(0);
    int               tr2  = generateEnter(disp);
    localVar.add(disp);
    appendInstruction(new GeneralInstruction(Opcodes.ADD, reg, tr3, tr2));

    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = reg;
  }

  private void genFtnCall(String fname, int dest, int src, Type type)
  {
    boolean cmplx = type.isComplexType();

    if (cmplx) {
      // The called transcendental function is written in C and, as
      // such, can not return a complex value in the registers.  The
      // called function expects a "struct" argument and returns a
      // "struct" result.  We can eliminate this logic when we
      // handle complex value types in C and re-write the "scale_"
      // functions.  Or, when we write the complex transcendental
      // function in TIL.

      int          bs  = type.memorySizeAsInt(machine);
      StringBuffer buf = new StringBuffer("_scale_");
      buf.append(fname);
      buf.append('z');
      fname = buf.toString();
      if (complexDisp == null)
        complexDisp = new StackDisplacement(localVarSize);
      localVarSize += Machine.alignTo(bs, SAVED_REG_SIZE);
      localVar.addElement(complexDisp); // This stack offset will be modified later.
      appendInstruction(new ImmediateInstruction(Opcodes.ADDI,
                                                 Trips2RegisterSet.FF_REG,
                                                 stkPtrReg,
                                                 complexDisp));
      genRegToReg(src, Trips2RegisterSet.FF_REG + 1);
    } else
      genRegToReg(src, Trips2RegisterSet.FF_REG);

    genFtnCall(fname,
               genDoubleUse(Trips2RegisterSet.IF_REG, Trips2RegisterSet.IF_REG + 1),
               null);

    if (cmplx) {
      int tra = registers.newTempRegister(RegisterSet.ADRREG);
      appendInstruction(new ImmediateInstruction(Opcodes.ADDI, tra, stkPtrReg, complexDisp));
      loadFromMemoryWithOffset(dest, tra, 0, 16, 8, true, true);
    } else
      genRegToReg(Trips2RegisterSet.FR_REG, dest);
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

  protected void genConjgFtn(int dest, int src, Type rType)
  {
    int zr = generateEnter(0.0);
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, src));
    appendInstruction(new GeneralInstruction(Opcodes.FSUB, dest + 1, zr, src + 1));
  }

  protected void genReturnAddressFtn(int dest, int src, Type type)
  {
    int    cnt  = registers.newTempRegister(RegisterSet.INTREG);
    int    sp   = registers.newTempRegister(RegisterSet.INTREG);
    int    preg = registers.newTempRegister(RegisterSet.INTREG);
    Label  labt = createLabel();
    Label  labf = createLabel();
    Branch br   = new TripsBranch(Opcodes.BRO, labt, 1);
    Branch br1  = new TripsBranch(Opcodes.BRO, labt, 1, preg, true);
    Branch br2  = new TripsBranch(Opcodes.BRO, labf, 1, preg, false);

    br.addTarget(labt, 0);
    br1.addTarget(labt, 0);
    br2.addTarget(labf, 0);

    genRegToReg(src, cnt);
    genRegToReg(Trips2RegisterSet.SP_REG, sp);
    if (callsRoutine)
      appendInstruction(new LoadInstruction(Opcodes.LD, dest, sp, 8));
    else
      appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, Trips2RegisterSet.RA_REG));
    appendInstruction(br);

    appendLabel(labt);

    appendInstruction(new ImmediateInstruction(Opcodes.TGTI, preg, cnt, 0));

    appendInstruction(new ImmediateInstruction(Opcodes.SUBI, cnt, cnt, 1, preg, true));
    appendInstruction(new LoadInstruction(Opcodes.LD, sp, sp, 0, preg, true));
    appendInstruction(new LoadInstruction(Opcodes.LD, dest, sp, 8, preg, true));

    appendInstruction(br1);
    appendInstruction(br2);

    appendLabel(labf);
  }

  protected void genFrameAddressFtn(int dest, int src, Type type)
  {
    int    cnt  = registers.newTempRegister(RegisterSet.INTREG);
    int    sp   = registers.newTempRegister(RegisterSet.INTREG);
    int    preg = registers.newTempRegister(RegisterSet.INTREG);
    Label  labt = createLabel();
    Label  labf = createLabel();
    Branch br   = new TripsBranch(Opcodes.BRO, labt, 1);
    Branch br1  = new TripsBranch(Opcodes.BRO, labt, 1, preg, true);
    Branch br2  = new TripsBranch(Opcodes.BRO, labf, 1, preg, false);

    br.addTarget(labt, 0);
    br1.addTarget(labt, 0);
    br2.addTarget(labf, 0);

    genRegToReg(src, cnt);
    genRegToReg(Trips2RegisterSet.SP_REG, sp);
    appendInstruction(new LoadInstruction(Opcodes.LD, dest, sp, 0));
    appendInstruction(br);

    appendLabel(labt);

    appendInstruction(new ImmediateInstruction(Opcodes.TGTI, preg, cnt, 0));

    appendInstruction(new ImmediateInstruction(Opcodes.SUBI, cnt, cnt, 1, preg, true));
    appendInstruction(new LoadInstruction(Opcodes.LD, sp, sp, 0, preg, true));
    appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, sp, preg, true));

    appendInstruction(br1);
    appendInstruction(br2);

    appendLabel(labf);
  }

  protected void genSignFtn(int dest, int laReg, int raReg, Type rType)
  {
    int bs = rType.memorySizeAsInt(machine);

    // r = sign(a, b);
    // r = (b < 0) ? - abs(a) : abs(a);
    if (rType.isRealType()) {
      int srr = registers.newTempRegister(RegisterSet.INTREG);
      int slr = registers.newTempRegister(RegisterSet.INTREG);
      int sr  = registers.newTempRegister(RegisterSet.INTREG);
      int zr  = registers.newTempRegister(RegisterSet.INTREG);
      // determine if sign matches
      appendInstruction(new ImmediateInstruction(Opcodes.SRAI, srr, raReg, 63));
      appendInstruction(new ImmediateInstruction(Opcodes.SRAI, slr, laReg, 63));
      appendInstruction(new GeneralInstruction(Opcodes.TEQ, sr, srr, slr));
      // true -> laReg, false -> (0 - laReg)
      appendInstruction(new GeneralInstruction(Opcodes.MOV, dest, laReg, sr, true));
      appendInstruction(new ImmediateInstruction(Opcodes.MOVI, zr, 0, sr, false));
      appendInstruction(new GeneralInstruction(Opcodes.FSUB, dest, zr, laReg, sr, false));
      return;
    }

    int ir = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.
    int tr = registers.newTempRegister(RegisterSet.INTREG); // raReg & dest could be the same register.

    appendInstruction(new GeneralInstruction(Opcodes.XOR, ir, raReg, laReg));
    appendInstruction(new ImmediateInstruction(Opcodes.SRAI, ir, ir, 63));
    appendInstruction(new GeneralInstruction(Opcodes.XOR, tr, laReg, ir));
    appendInstruction(new GeneralInstruction(Opcodes.SUB, dest, tr, ir));
  }

  protected void genAtan2Ftn(int dest, int laReg, int raReg, Type rType)
  {
    genRegToReg(laReg, Trips2RegisterSet.FF_REG);
    genRegToReg(raReg, Trips2RegisterSet.FF_REG + 1);
    genFtnCall("atan2", genDoubleUse(Trips2RegisterSet.IF_REG, Trips2RegisterSet.IF_REG + 1), null);
    genRegToReg(Trips2RegisterSet.FR_REG, dest);
  }

  protected void genDimFtn(int dest, int laReg, int raReg, Type rType)
  {
    int bs = rType.memorySizeAsInt(machine);

    // r = dim(a, b);
    // r = (a > b) ? a - b : 0;

    int treg = registers.newTempRegister(RegisterSet.INTREG);

    if (rType.isRealType()) {
      appendInstruction(new GeneralInstruction(Opcodes.FSUB, dest, laReg, raReg));
      appendInstruction(new GeneralInstruction(Opcodes.FLT, treg, laReg, raReg));
      appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, 0, treg, true));
      return;
    }

    appendInstruction(new GeneralInstruction(Opcodes.SUB, dest, laReg, raReg));
    appendInstruction(new ImmediateInstruction(Opcodes.TLTI, treg, dest, 0));
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI, dest, 0, treg, true));
  }

  public void visitNotExpr(NotExpr e)
  {
    Expr arg = e.getArg();
    int  ir  = registers.getResultRegister(RegisterSet.AFIREG);

    needValue(arg);
    int src = resultReg;

    appendInstruction(new ImmediateInstruction(Opcodes.TEQI, ir, src, 0)); 
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = ir;
  }

  public void visitReturnChord(ReturnChord c)
  {
    Expr    a    = c.getResultValue();
    short[] uses = null;
    int     k    = 0;

    if (a != null) {
      Type    at   = processType(a);
      int     bs   = at.memorySizeAsInt(machine);
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

      if (resultRegMode == ResultMode.ADDRESS) {
        moveWords(src, srcoff, structAddress, 0, structSize, srcaln);
        genRegToReg(structAddress, dest);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.ADDRESS;
        resultReg = structAddress;
      } else if (srcha == ResultMode.STRUCT_VALUE){
        storeIntoMemoryWithOffset(src, structAddress, 0, structSize, structSize, false); 
        genRegToReg(structAddress, dest);
        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.ADDRESS;
        resultReg = structAddress;
      } else {
        if (srcoff != 0) {
          int tr = registers.newTempRegister(RegisterSet.ADRREG);
          genLoadImmediate(srcoff, src, tr);
          resultReg = tr;
        }
        if (!at.isRealType() && bs <= 4) {
          int tr = registers.newTempRegister(RegisterSet.INTREG);
          if (at.isSigned()) 
            signExtend(resultReg, tr, bs);
          else
            zeroExtend(resultReg, tr, bs);
          resultRegAddressOffset = 0;
          resultRegMode = ResultMode.NORMAL_VALUE;
          resultReg = tr;
        }
        genRegToReg(resultReg, dest);
      }

      int nr = registers.numContiguousRegisters(resultReg);
      int fr = registers.rangeBegin(dest);

      if (registers.pairRegister(resultReg))
        nr *= 2;

      uses = new short[nr + (usesAlloca ? 2 : 1)];
      for (int i = 0; i < nr; i++)
        uses[k++] = (short) (fr + i);

    } else
      uses = new short[usesAlloca ? 2 : 1];

    uses[k++] = Trips2RegisterSet.SP_REG;
    if (usesAlloca)
      uses[k++] = Trips2RegisterSet.FP_REG;
    
    TripsBranch inst = new TripsBranch(Opcodes.RET, Trips2RegisterSet.RA_REG, 0);
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
  protected void storeRegToSymbolicLocation(int          src,
                                            int          dsize,
                                            long         alignment,
                                            boolean      isReal,
                                            Displacement disp)
  {
    assert !disp.isNumeric() : "Numeric displacement " + disp;

    int adr = generateEnterA(disp.unique());
    storeIntoMemoryWithOffset(src, adr, 0, dsize, alignment, isReal);
  }

  /**
   * Store a value into a field of a structure.
   * @param lhs specifies the field of the structure
   * @param rhs specifies the value
   */
  protected void storeLfae(LoadFieldAddressExpr lhs, Expr rhs)
  {
    Expr struct = lhs.getStructure();
    Type type   = processType(struct);

    FieldDecl fd          = lhs.getField();
    long      fieldOffset = fd.getFieldOffset();
    Type      ft          = processType(fd);
    int       bits        = fd.getBits();
    boolean   signed      = ft.isSigned();
    boolean   isReal      = ft.isRealType();

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
      // Structure is in registers, not memory, and is 32 bytes or
      // less in size.

      while (struct instanceof LoadFieldAddressExpr) {
        struct = ((LoadFieldAddressExpr) struct).getStructure();
        type = processType(struct);
      }

      int ts = type.getPointedTo().memorySizeAsInt(machine);

      if (fieldOffset >= 8) {
        fieldOffset -= 8;
        ts -= 8;
        adr++;
      }

      bitOffset += 8 * ((int) fieldOffset);
      if (bits == 0)
        bits = 8 * byteSize; 

      if (bits == 64) {
        appendInstruction(new GeneralInstruction(Opcodes.MOV, adr, src));
        resultRegAddressOffset = srcoff;
        resultRegMode = srcha;
        resultReg = src;
        resultRegSize = srcrs;
        return;
      }

      if (isReal) {
        assert (bits == 32) : "Float size?";
        int  trr  = registers.newTempRegister(RegisterSet.FLTREG);
        appendInstruction(new GeneralInstruction(Opcodes.FDTOS, trr, src));
        src = trr;
      }

      // Data from a structure is always right-aligned in a register.

      if (ts < Trips2RegisterSet.IREG_SIZE)
        bitOffset = (64 - ((ts + addIn[ts]) * 8)) + bitOffset;

      long mk   = (1L << bits) - 1;
      int  ls   = 64 - bitOffset - bits;
      long mask = (mk << ls);
      int  trs  = registers.newTempRegister(RegisterSet.INTREG);

      resultRegAddressOffset = srcoff;
      resultRegMode = srcha;
      resultReg = trs;
      resultRegSize = srcrs;

      if (ls > 0) {
        if (ls == 32 && bits == 32) {
          // Special case:  if we left-shift by 32, we don't need to mask
          // the bottom 32 bits because they'll be zero after the left-shift.
          appendInstruction(new ImmediateInstruction(Opcodes.SLLI, trs, src, ls));
        } else {
          appendInstruction(new ImmediateInstruction(Opcodes.SLLI, trs, src, ls));
          appendInstruction(new ImmediateInstruction(Opcodes.ANDI, trs, trs, mask));
        }
      } else {
        if (bits == 32) {
          if ((ts * 8) == bits) {
            if (isReal)
              appendInstruction(new GeneralInstruction(Opcodes.MOV, adr, src));
            else {
              int op = signed ? Opcodes.EXTSW : Opcodes.EXTUW;
              appendInstruction(new GeneralInstruction(op, adr, src));
            }
            resultReg = adr;
            return;
          }
          // If it's a 32-bit real, we already did an FDTOS above so we
          // don't need to EXTUW, since FDTOS zeroes the result's top 32 bits.
          if (isReal)
            trs = src;
          else
            appendInstruction(new GeneralInstruction(Opcodes.EXTUW, trs, src));

        } else if (bits == 16) {
          if ((ts * 8) == bits) {
            int op = signed ? Opcodes.EXTSH : Opcodes.EXTUH;
            appendInstruction(new GeneralInstruction(op, adr, src));
            resultReg = adr;
            return;
          }
          appendInstruction(new GeneralInstruction(Opcodes.EXTUH, trs, src));
        } else if (bits == 8) {
          if ((ts * 8) == bits) {
            int op = signed ? Opcodes.EXTSB : Opcodes.EXTUB;
            appendInstruction(new GeneralInstruction(op, adr, src));
            resultReg = adr;
            return;
          }
          appendInstruction(new GeneralInstruction(Opcodes.EXTUB, trs, src));
        } else {
          appendInstruction(new ImmediateInstruction(Opcodes.ANDI, trs, src, mask));
        }
      }

      appendInstruction(new ImmediateInstruction(Opcodes.ANDI, adr, adr, ~mask));
      appendInstruction(new GeneralInstruction(Opcodes.OR, adr, trs, adr));
      appendInstruction(new ImmediateInstruction(Opcodes.SLLI, trs, src, 64 - bits));

      int op = signed ? Opcodes.SRAI : Opcodes.SRLI;
      appendInstruction(new ImmediateInstruction(op, trs, trs, 64 - bits));

      return;
    }

    // Structure is in memory.

    boolean fieldAligned = (0 == (adraln % 2));
    if (bits == 0)
      storeIntoMemoryWithOffset(src, adr, fieldOffset, byteSize, fieldAligned ? fa : 1, isReal);
    else
      storeBitsIntoMemory(src, adr, fieldOffset, bits, bitOffset, fieldAligned ? fa : 1);

    resultRegAddressOffset = srcoff;
    resultRegMode = srcha;
    resultReg = src;
    resultRegSize = srcrs;
  }

  protected boolean genSwitchUsingIfs(int     testReg,
                                      Chord[] cases,
                                      long[]  keys,
                                      int     num,
                                      long    spread)
  {
    if (num > Trips2Machine.maxBranches)
      return false;

    generateSwitchFromIfMulti(testReg, cases, keys);

    return true;
  }

  /**
   * Generate a switch using individual tests and a single branch.
   */
  private void generateSwitchFromIfSingle(int     tReg,
                                          Chord[] cases,
                                          long[]  values,
                                          int     locd,
                                          int     num)
  {
    int breg = registers.newTempRegister(RegisterSet.AFIREG);

    savePredicate();      // save the predicate (if any)

    for (int i = 0; i < num - 1; i++) {
      int               rp    = registers.newTempRegister(RegisterSet.AFIREG);
      long              value = values[i];
      Label             labt  = getBranchLabel(cases[i]);
      LabelDisplacement dispt = new LabelDisplacement(labt);
      int               loct  = generateEnterB(dispt);

      doIntOperate(Opcodes.TEQ, tReg, value, rp);
      appendInstruction(new GeneralInstruction(Opcodes.MOV, breg, loct, rp, true));

      // After the first iteration of this loop, the generateEnter()
      // function above will be predicated on false and will execute
      // when this case is not taken.
      setPredicate(rp, false);
    }

    appendInstruction(new GeneralInstruction(Opcodes.MOV, breg, locd, predicateReg, false));
    TripsBranch inst = new TripsBranch(Opcodes.BR, breg, cases.length);

    for (int i = 0; i < cases.length; i++)
      inst.addTarget(getBranchLabel(cases[i]), i);

    setPredicate(-1, false); // clear the predicate
    
    appendInstruction(inst);

    restorePredicate();  // restore the predicate (if any)
  }
  
  /**
   * Generate a switch using individual tests.
   */
  private void generateSwitchFromIfMulti(int tReg, Chord[] cases, long[] values)
  {
    savePredicate();      // save the predicate (if any)

    int num  = values.length;

    for (int i = 0; i < num - 1; i++) {
      int               rp    = registers.newTempRegister(RegisterSet.AFIREG);
      long              value = values[i];
      Label             labt  = getBranchLabel(cases[i]);

      doIntOperate(Opcodes.TEQ, tReg, value, rp);
      
      TripsBranch brt = new TripsBranch(Opcodes.BRO, labt, 1, rp, true);
      brt.addTarget(labt, 0);
      appendInstruction(brt);
      
      // After the first iteration of this loop, the generateEnter()
      // function above will be predicated on false and will execute
      // when this case is not taken.
      setPredicate(rp, false);
    }

    Chord       deflt = cases[num - 1];
    Label       labd  = getBranchLabel(deflt);
    TripsBranch brd   = new TripsBranch(Opcodes.BRO, labd, 1, predicateReg, false);
    
    brd.addTarget(labd, 0);
    setPredicate(-1, false); // clear the predicate
    appendInstruction(brd);

    restorePredicate();  // restore the predicate (if any)
  }

  /**
   * Generate a switch using a transfer vector.
   */
  protected void genSwitchUsingTransferVector(int     testReg,
                                              Chord[] cases,
                                              long[]  values,
                                              Label   labd,
                                              long    min,
                                              long    max)
  {
    int locd  = generateEnterB(new LabelDisplacement(labd));
    int ir    = resultReg;
    if (min != 0) {
      ir = registers.newTempRegister(RegisterSet.AFIREG);
      doIntOperate(Opcodes.SUB, resultReg, min, ir);
    }

    int          rp    = registers.newTempRegister(RegisterSet.AFIREG);
    int          tmp   = registers.newTempRegister(RegisterSet.AFIREG);
    int          adr   = registers.newTempRegister(RegisterSet.AFIREG);
    Displacement disp  = createAddressTable(cases, values, (int) min, (int) max);
    TripsBranch  instj = new TripsBranch(Opcodes.BR, adr, cases.length);

    for (int i = 0; i < cases.length; i++)
      instj.addTarget(getBranchLabel(cases[i]), i);

    doIntOperate(Opcodes.TLEU, ir, max - min, rp);

    // If the key is out of range use the default case
    
    appendInstruction(new GeneralInstruction(Opcodes.MOV, adr, locd, rp, false));

    // If the key is in range use the jump table.

    int radr = generateEnterA(disp, rp, true);        
    appendInstruction(new ImmediateInstruction(Opcodes.SLLI, tmp, ir, 3, rp, true));
    appendInstruction(new GeneralInstruction(Opcodes.ADD, radr, tmp, radr, rp, true));
    appendInstruction(new LoadInstruction(Opcodes.LD, adr, radr, 0, rp, true));

    appendInstruction(instj);
  }

  /**
   * Saves the predicate.
   */
  private void savePredicate()
  {
    savedPredicateReg     = predicateReg;
    savedPredicatedOnTrue = predicatedOnTrue;
  }
  
  /**
   * Restores the saved predicate.
   */
  private void restorePredicate()
  {
    predicateReg     = savedPredicateReg;
    predicatedOnTrue = savedPredicatedOnTrue;
  }
  
  /**
   * Set the predicate.
   */
  private void setPredicate(int predicateReg, boolean predicatedOnTrue)
  {
    this.predicateReg     = predicateReg;
    this.predicatedOnTrue = predicatedOnTrue;
  }

  public void visitVaStartExpr(VaStartExpr e)
  {
    FormalDecl    parmN  = e.getParmN();
    Expr          vaList = e.getVaList();
    Type          vat    = processType(vaList);
    ProcedureType pt     = (ProcedureType) processType(currentRoutine);
    Type          rt     = processType(pt.getReturnType());
    int           offset = 0;
    int           l      = pt.numFormals();

    if (!(rt.isAtomicType() || rt.isVoidType()))
      offset += SAVED_REG_SIZE; // Procedure returns structure.

    int callerSP = registers.newTempRegister(RegisterSet.AFIREG);
    loadFromMemoryWithOffset(callerSP,
                             stkPtrReg,
                             0,
                             Trips2RegisterSet.IREG_SIZE,
                             0,
                             false,
                             false);

    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      int        bs = ((fd.getCoreType().memorySizeAsInt(machine) +
                        SAVED_REG_SIZE - 1) &
                       ~(SAVED_REG_SIZE - 1));

      offset += bs;

      if (fd == parmN) {
        needValue(vaList);

        int adr  = resultReg;
        int ir   = registers.newTempRegister(RegisterSet.INTREG);
        int size = vat.memorySizeAsInt(machine);

        genLoadImmediate(offset + ARG_SAVE_OFFSET, callerSP, ir);
        storeIntoMemoryWithOffset(ir, adr, 0, size, 0, false);

        resultRegAddressOffset = 0;
        resultRegMode = ResultMode.NORMAL_VALUE;
        resultReg = ir;
        return;
      }
    }

    throw new scale.common.InternalError("Parameter not found " + parmN);
  }

  //                                   0, 1, 2, 3, 4, 5, 6, 7
  private static final int[] addIn = { 0, 0, 0, 1, 0, 3, 2, 1};

  public void visitVaArgExpr(VaArgExpr e)
  {
    Expr vaList = e.getVaList();
    Type ct     = processType(e);
    int  bs     = ct.memorySizeAsInt(machine);
    int  ir     = registers.getResultRegister(ct.getTag());
    int  inc    = (bs + SAVED_REG_SIZE - 1) & ~(SAVED_REG_SIZE - 1);
    int  adr2   = registers.newTempRegister(RegisterSet.AFIREG);

    bs += addIn[bs & 0x7];

    ResultMode isAdr = (ct.isAtomicType() ? ResultMode.NORMAL_VALUE : ResultMode.ADDRESS);

    if (vaList instanceof LoadDeclAddressExpr) { // Generate better code by avoiding the visit.
      VariableDecl val  = (VariableDecl) ((LoadDeclAddressExpr) vaList).getDecl();
      Type         valt = processType(val);

      if (val.getStorageLoc() == Assigned.ON_STACK) {
        Displacement disp = val.getDisplacement();
        int          adr  = registers.newTempRegister(RegisterSet.ADRREG);

        loadFromMemoryWithOffset(adr,
                                 stkPtrReg,
                                 disp,
                                 8,
                                 machine.stackAlignment(valt),
                                 false,
                                 false);
       
        if (isAdr != ResultMode.ADDRESS)
          loadFromMemoryWithOffset(ir,
                                   adr,
                                   SAVED_REG_SIZE - bs,
                                   bs,
                                   0,
                                   ct.isSigned(),
                                   false); // Load arg
        else if (bs < SAVED_REG_SIZE)
          appendInstruction(new ImmediateInstruction(Opcodes.ADDI,
                                                     adr2,
                                                     adr,
                                                     SAVED_REG_SIZE - bs));
        else
          genRegToReg(adr,
                      adr2);

        appendInstruction(new ImmediateInstruction(Opcodes.ADDI, adr, adr, inc));
        storeIntoMemoryWithOffset(adr,
                                  stkPtrReg,
                                  disp,
                                  8,
                                  machine.stackAlignment(valt),
                                  false);


        resultRegAddressOffset = 0;
        resultRegMode = isAdr;
        resultReg = (isAdr == ResultMode.ADDRESS) ? adr2 : ir;
        return;
      }
    }

    Type valt = processType(vaList.getCoreType().getPointedTo());
    int  incs = valt.memorySizeAsInt(machine);
    int  adr  = registers.getResultRegister(valt.getTag());

    vaList.visit(this);
    int  va    = resultReg;
    long vaoff = resultRegAddressOffset;

    loadFromMemoryWithOffset(adr, va, vaoff, incs, 0, false, false);
    if (isAdr != ResultMode.ADDRESS)
      loadFromMemoryWithOffset(ir,
                               adr,
                               SAVED_REG_SIZE - bs,
                               bs,
                               0,
                               ct.isSigned(),
                               false);
    else if (bs < SAVED_REG_SIZE)
      appendInstruction(new ImmediateInstruction(Opcodes.ADDI,
                                                 adr2,
                                                 adr,
                                                 SAVED_REG_SIZE - bs));
    else
      genRegToReg(adr, adr2);

    appendInstruction(new ImmediateInstruction(Opcodes.ADDI, adr, adr, inc));
    storeIntoMemoryWithOffset(adr, va, vaoff, incs, 0, true);

    resultRegAddressOffset = 0;
    resultRegMode = isAdr;
    resultReg = (isAdr == ResultMode.ADDRESS) ? adr2 : ir;
  }
  
  public void visitConditionalExpr(ConditionalExpr e)
  {
    Type ct        = processType(e);
    int  tr        = registers.getResultRegister(ct.getTag());
    Expr predicate = e.getTest();
    Expr trueExpr  = e.getTrueExpr();
    Expr falseExpr = e.getFalseExpr();
    
    boolean trueIsLiteral = false;
    long    trueValue     = -1;
    
    if (trueExpr.isLiteralExpr()) {
      LiteralExpr le  = (LiteralExpr) trueExpr;
      Literal     lit = le.getLiteral();
      
      if (lit instanceof IntLiteral) {
        trueValue     = ((IntLiteral) lit).getLongValue();
        trueIsLiteral = true;
      } else if (lit instanceof FloatLiteral) {
        FloatLiteral il = (FloatLiteral) lit;
        double       fv = il.getDoubleValue();
        
        if (fv == 0.0) {
          trueValue     = 0;
          trueIsLiteral = true;
        }
      } else if (lit instanceof SizeofLiteral) {
        trueValue     = valueOf((SizeofLiteral) lit);
        trueIsLiteral = true;
      } 
    }
    
    int treg = -1;
    if (!trueIsLiteral) {
      needValue(trueExpr);
      treg = resultReg;
    }
    
    boolean falseIsLiteral = false;
    long    falseValue     = -1;
    
    if (falseExpr.isLiteralExpr()) {
      LiteralExpr le  = (LiteralExpr) falseExpr;
      Literal     lit = le.getLiteral();
      
      if (lit instanceof IntLiteral) {
        falseValue     = ((IntLiteral) lit).getLongValue();
        falseIsLiteral = true;
      } else if (lit instanceof FloatLiteral) {
        FloatLiteral il = (FloatLiteral) lit;
        double       fv = il.getDoubleValue();
        
        if (fv == 0.0) {
          falseValue = 0;
          falseIsLiteral = true;
        }
      } else if (lit instanceof SizeofLiteral) {
        falseValue = valueOf((SizeofLiteral) lit);
        falseIsLiteral = true;
      } 
    }
    
    int freg = -1;
    if (!falseIsLiteral) {
      needValue(falseExpr);
      freg = resultReg;
    }
        
    needValue(predicate);
    int preg = resultReg;

    int rreg = preg;
    if (!predicate.hasTrueFalseResult()) {
      rreg = registers.newTempRegister(RegisterSet.AFIREG);
      appendInstruction(new ImmediateInstruction(Opcodes.TEQI, rreg, preg, 1));
    }
    
    if (trueIsLiteral)
      appendInstruction(new ImmediateInstruction(Opcodes.MOVI, tr, trueValue, rreg, true));
    else
      appendInstruction(new GeneralInstruction(Opcodes.MOV, tr, treg, rreg, true));
      
    if (falseIsLiteral)
      appendInstruction(new ImmediateInstruction(Opcodes.MOVI, tr, falseValue, rreg, false));
     else
      appendInstruction(new GeneralInstruction(Opcodes.MOV, tr, freg, rreg, false));
    
    resultRegAddressOffset = 0;
    resultRegMode = ResultMode.NORMAL_VALUE;
    resultReg = tr;
  }

  public void visitExprChord(ExprChord c)
  {
    Expr lhs       = c.getLValue();
    Expr rhs       = c.getRValue();
    Expr predicate = c.getPredicate();

    if (predicate != null) {
      needValue(predicate);
      setPredicate(resultReg, c.predicatedOnTrue());
    }
    
    doStore(lhs, rhs, c.isVaCopy());
    setPredicate(-1, false);
  }

  public int getMaxAreaIndex()
  {
    return TEXT;
  }

  private void remapDeclInfo(int[] map)
  {
    int l = regVars.size();
    for (int i = 0; i < l; i++) {
      Declaration decl = regVars.elementAt(i);
      int         reg  = decl.getValueRegister();
      if ((reg >= 0) && (reg < map.length)) {
        reg = map[reg];
        decl.setValueRegister(reg, decl.valueRegMode());
      }
    }
  }

  /**
   * Record the name of a new variable for the assembler.
   */
  private void addVariableDecl(VariableDecl vd)
  {
    Vector<VariableDecl> v;
    if (currentRoutine != null) {
      v = ftnVars.get(currentRoutine.getName());
    } else {
      // == null when this is a global variable and when ???
      v = ftnVars.get("unknown");
      if (v == null) {
        v = new Vector<VariableDecl>(20);
        ftnVars.put("unknown", v);
      }
    }

    v.add(vd);
  }

  /**
   * Return the variables for a function.
   */
  public Vector<VariableDecl> getVariables(String func)
  {
    return ftnVars.get(func);
  }

  
  /*** Intrinsics ***/
  
  // Implements the C standard library function:  int abs (int n);
  protected void __builtin_abs()
  {
    int ir2 = registers.newTempRegister(RegisterSet.AFIREG);
    int ir3 = registers.newTempRegister(RegisterSet.AFIREG);
    int ir4 = registers.newTempRegister(RegisterSet.AFIREG);
    int ir5 = registers.newTempRegister(RegisterSet.AFIREG);

    int r3 = 3; // n

    appendInstruction(new GeneralInstruction  (Opcodes.EXTSW, r3,  r3));              // extsw $t130, $g3
    appendInstruction(new ImmediateInstruction(Opcodes.MOVI,  ir2, 0));               // movi $t132, 0
    appendInstruction(new GeneralInstruction  (Opcodes.SUB,   ir3, ir2, r3));         // sub $t131, $t132, $t130
    appendInstruction(new GeneralInstruction  (Opcodes.TLE,   ir4, ir2, r3));         // tle $t133, $t132, $t130
    appendInstruction(new GeneralInstruction  (Opcodes.MOV,   ir5, r3,  ir4, true));  // mov_t<$t133> $t129, $t130
    appendInstruction(new GeneralInstruction  (Opcodes.MOV,   ir5, ir3, ir4, false)); // mov_f<$t133> $t129, $t131
    appendInstruction(new GeneralInstruction  (Opcodes.MOV,   r3,  ir5));             // mov $g3, $t129
  }
}

