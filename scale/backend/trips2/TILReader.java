package scale.backend.trips2;

import java.io.*;

import scale.common.*;

import scale.clef.decl.Visibility;
import scale.clef.decl.ProcedureDecl;
import scale.clef.decl.Declaration;
import scale.score.Scribble;
import scale.frontend.c.SourceC;

import scale.backend.SymbolDisplacement;
import scale.backend.LabelDisplacement;
import scale.backend.Instruction;
import scale.backend.IntegerDisplacement;
import scale.backend.Displacement;
import scale.backend.Label;

/**
 * This class reads a TRIPS IL file (.til).
 * <p>
 * $Id: TILReader.java,v 1.10 2007-10-04 19:58:00 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<p>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><p>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<p>
 * Amherst MA. 01003, USA<p>
 * All Rights Reserved.<p>
 */

public class TILReader
{
  private Vector<Instruction>    routines = null; // The first instruction for every routine.
  private Vector<String>         globals  = null; // The functions which have global scope.
  private HashMap<String, Label> labels   = null; // Maps a label to its internal representation (String -> Label).

  private TripsPGenerator gen              = null;  // The instance of TripsPGenerator which called this class.
  private Instruction     firstInstruction = null;  // The first instruction (BeginMarker).
  private Instruction     lastInstruction  = null;  // The last instruction created.
  private Label           lastLabel        = null;  // The last label created.
  private ProcedureDecl   currentRoutine   = null;  // The declaration associated with this routine.
  
  /**
   * The default constructor.
   */
  public TILReader(TripsPGenerator gen) 
  {
    this.gen      = gen;
    this.labels   = new HashMap<String, Label>(10);
    this.routines = new Vector<Instruction>();
    this.globals  = new Vector<String>();
  }

  /**
   * The main entry point.
   */
  protected void readFile(String filename)
  { 
    try {
      // Read everything up to the .text section and pass it through as is.
      
      BufferedReader reader = getBufferedReader(filename);
      
      while (true) {
        String line = reader.readLine();
        
        if (line == null)
          return;
        
        if (line.indexOf(".text") != -1)
          break;
        
        if (line.startsWith(";"))  // Ignore comments.
          continue;
        
        gen.addPassThroughItem(line);
      }
      
      // Parse the .text section.
      
      StreamTokenizer st = new StreamTokenizer(reader);
      st = initialiseTokens(st);
      
      while (true) {
        st.nextToken();
        ignoreEOL(st);
        
        if (st.ttype == StreamTokenizer.TT_EOF) 
          return;

        requireWord(st);

        if (st.sval.startsWith(".global")) {
          st.nextToken();
          requireWord(st);
          globals.add(st.sval);
        } else if (st.sval.startsWith(".bbegin")) 
          parseBlock(st);
      }
    } catch (java.lang.Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Return the first instruction for every routine.
   */
  public Vector<Instruction> getRoutines()
  {
    return routines;  
  }
  
  /**
   * Create a new reader for a file.
   */
  private BufferedReader getBufferedReader(String filename)
  {
    FileInputStream fis = null;
    
    try {
      fis = new FileInputStream(filename);
    } catch (java.lang.Exception e) {
      System.out.println(e.toString());
      e.printStackTrace();
      System.exit(1);
    }
    
    return new BufferedReader(new InputStreamReader(fis));
  }

  /**
   * Initialise the stream tokenizer.
   */
  private StreamTokenizer initialiseTokens(StreamTokenizer st)
  {
    st.eolIsSignificant(true);
    st.commentChar('#');
    st.commentChar(';');
    st.quoteChar('\"');
    
    nonSpecial(st, '0', '9');
    nonSpecial(st, '_');
    nonSpecial(st, '.');
    nonSpecial(st, '-');
    nonSpecial(st, '+');
    nonSpecial(st, '%');
    nonSpecial(st, '$');
    nonSpecial(st, '(');
    nonSpecial(st, ')');
    nonSpecial(st, '[');
    nonSpecial(st, ']');
    nonSpecial(st, '|');
    nonSpecial(st, ':');  // Labels end with ':'.

    return st;
  }
  
  /**
   * Specify that a character is not special.
   */
  private void nonSpecial(StreamTokenizer st, char ch)
  {
    st.ordinaryChars(ch, ch);
    st.wordChars(ch, ch);
  }

  /**
   * Specify that a range of characters are not special.
   */
  private void nonSpecial(StreamTokenizer st, char ch1, char ch2)
  {
    st.ordinaryChars(ch1, ch2);
    st.wordChars(ch1, ch2);
  }
  
  /**
   * Convert a string to the compilers internal form.
   */
  private String convertString(String str)
  {
    StringBuffer buf = new StringBuffer("\"");
    
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      switch (ch) {
      case '\n':
        buf.append("\\n");
        break;
      case '\r':
        buf.append("\\r");
        break;
      case '"':
        buf.append("\\\"");
        break;
      case '\t':
        buf.append("\\t");
        break;
      case '\\':
        buf.append("\\\\");
        break;
      case '\0':
        buf.append("\\000");
        break;
      default:
        buf.append(ch);
      }
    }
    buf.append("\"");
    
    return buf.toString();
  }
  
  /**
   * Return a new label or the existing one.
   */
  private Label createLabel(String name)
  { 
    Label lab = labels.get(name);
    
    if (lab != null)
      return lab;
    
    TripsLabel label = new TripsLabel(currentRoutine);  
    int        index = getLabelIndex(name);
    
    label.setLabelIndex(index);
    labels.put(name, label);
    lastLabel = label;
    
    return label;
  }
  
  /**
   * Create a line marker.
   */
  private void createLineMarker(int line)
  {
    String filename = currentRoutine.getCallGraph().getName();
    appendInstruction(new Trips2LineMarker(filename, line));
  }
  
  /**
   * Parse a block in the TIL.
   */
  private void parseBlock(StreamTokenizer st) throws java.lang.Exception
  {
    st.nextToken();
    if (st.sval.indexOf("$") == -1)
      createBeginMarker(st.sval);
    else {
      Label lab = createLabel(st.sval);
      appendInstruction(lab);
    }
    
    st.nextToken();
    requireEOL(st);

    while (true) {
      ignoreEOL(st);
      requireWord(st);

      if (st.sval.equals(".bend")) 
        break;
      
      String opName = st.sval;
      if (opName.equals("line")) {
        st.nextToken();
        requireWord(st);
        createLineMarker(Integer.parseInt(st.sval));
        st.nextToken();
        requireEOL(st);
      } else 
        parseInstruction(st, opName);
       
      requireEOL(st);
    }
  }
  
  /**
   * Parse an instruction.
   * An instruction looks like:
   * <pre>
   *     opcode[_t|_f <preds>] operand+
   * </pre>
   */
  private void parseInstruction(StreamTokenizer st,
                                String          opName) throws java.lang.Exception
  {
    BitVect        predicates         = null;         // The predicates for the instruction or null.
    Vector<Object> operands           = new Vector<Object>(); // The operands for the instruction (there is always 1).
    boolean        isPredicatedOnTrue = false;           

    // Read any predicates.

    if (isPredicated(opName)) {
      st.nextToken();
      requireAngBracket(st);
      st.nextToken();
      
      predicates         = new BitVect();
      isPredicatedOnTrue = opName.indexOf("_t") != -1 ? true : false;
      opName             = getOp(opName);  // Get the name of the instruction (add, sub, etc).
      
      while (true) {
        predicates.set(getRegisterNumber(st.sval));
        st.nextToken();
        requireCommaOrBracket(st);
        
        if (st.ttype == '>') 
          break;
        
        st.nextToken();
      }
    }
         
    // Read the destination operand.
    
    st.nextToken();
    if (!hasNoTargets(opName)) {
      requireWord(st);
      operands.add(new Integer(getRegisterNumber(st.sval)));
      st.nextToken();
      
      if (st.ttype != StreamTokenizer.TT_EOL) {
        requireComma(st);
        st.nextToken();
      }
    }

    // Read the source operands.
    
    while (st.ttype != StreamTokenizer.TT_EOL) {
      requireWord(st);
      
      if (isRegister(st.sval))
        operands.add(new Integer(getRegisterNumber(st.sval)));
      else if (isIndirectWithOffset(st.sval)) {
        operands.add(new Long(getIndirectOffset(st.sval)));
        operands.add(new Integer(getIndirectRegister(st.sval)));
      } else if (isImmediate(st.sval))
        operands.add(new Long(getImmediate(st.sval)));
      else // Label
        operands.add(st.sval);

      st.nextToken();
      if (st.ttype != StreamTokenizer.TT_EOL) {
        if (isTag(st.sval))
          operands.add(new String(st.sval));
        else
          requireComma(st);
        
        st.nextToken();
      }
    }
    
    createInstruction(opName, predicates.getSetBits(), operands, isPredicatedOnTrue);
  }

  /**
   * Determine which registers are used by a call.
   */
  private short[] specifyCallUses()
  {
    short[] uses = new short[2];
    
    uses[0] = Trips2RegisterSet.SP_REG;
    uses[1] = Trips2RegisterSet.RA_REG;
    
    return uses;
  }
  
  /**
   * Return the handle to the elf section for a symbol.
   */
  private int getHandle(String symbol)
  {
    return 0;   // TODO
  }
  
  /**
   * Create an enter instruction.
   */
  private Instruction createENTInstruction(int opcode, Vector<Object> operands)
  {
    int          op1  = ((Integer) operands.get(0)).intValue();
    Object       op2  = operands.get(1);
    Displacement disp = null;
    
    if (op2 instanceof String) {
      String sym = (String) op2;
      if ((sym.indexOf("$") != -1) && (sym.indexOf("$$") == -1)) {
        Label lab = createLabel(sym);
        disp = new LabelDisplacement(lab);
      } else
        disp = new SymbolDisplacement(sym, getHandle(sym));
    } else {
      long val = ((Long) op2).longValue();
      disp = new IntegerDisplacement(val);
    }
    
    EnterInstruction inst = new EnterInstruction(opcode, op1, disp);
    
    return inst;
  }
  
  /**
   * Create a general instruction in the G0 form.
   */
  private Instruction createG0Instruction(int opcode, Vector<Object> operands)
  {
    int                op1  = ((Integer) operands.get(0)).intValue();
    GeneralInstruction inst = new GeneralInstruction(opcode, op1, -1);  
    
    return inst;
  }

  /**
   * Create a general instruction in the G1 form.
   */
  private Instruction createG1Instruction(int opcode, Vector<Object> operands)
  {
    int                op1  = ((Integer) operands.get(0)).intValue();
    int                op2  = ((Integer) operands.get(1)).intValue();
    GeneralInstruction inst = new GeneralInstruction(opcode, op1, op2);
    
    return inst;
  }
  
  /**
   * Create a general instruction in the G2 form.
   */
  private Instruction createG2Instruction(int opcode, Vector<Object> operands)
  {
    int                op1  = ((Integer) operands.get(0)).intValue();
    int                op2  = ((Integer) operands.get(1)).intValue();
    int                op3  = ((Integer) operands.get(2)).intValue();
    GeneralInstruction inst = new GeneralInstruction(opcode, op1, op2, op3);
    
    return inst;
  }
  
  /**
   * Create a read instruction.
   */
  private Instruction createR0Instruction(int opcode, Vector<Object> operands)
  {
    int                op1  = ((Integer) operands.get(0)).intValue();
    int                op2  = ((Integer) operands.get(1)).intValue();
    GeneralInstruction inst = new GeneralInstruction(opcode, op1, op2);
    
    return inst;
  }
  
  /**
   * Create a write instruction.
   */
  private Instruction createW1Instruction(int opcode, Vector<Object> operands)
  {
    int                op1  = ((Integer) operands.get(0)).intValue();
    int                op2  = ((Integer) operands.get(1)).intValue();
    GeneralInstruction inst = new GeneralInstruction(opcode, op1, op2);
    
    return inst;
  }
  
  /**
   * Create an immediate instruction in the I0 form.
   */
  private Instruction createI0Instruction(int opcode, Vector<Object> operands)
  {
    int                  op1  = ((Integer) operands.get(0)).intValue();
    long                 op2  = ((Long) operands.get(1)).longValue();
    ImmediateInstruction inst = new ImmediateInstruction(opcode, op1, op2);
    
    return inst;
  }
  
  /**
   * Create an immediate instruction in the I1 form.
   */
  private Instruction createI1Instruction(int opcode, Vector<Object> operands)
  {
    int                  op1  = ((Integer) operands.get(0)).intValue();
    int                  op2  = ((Integer) operands.get(1)).intValue();
    long                 op3  = ((Long) operands.get(2)).longValue();
    ImmediateInstruction inst = new ImmediateInstruction(opcode, op1, op2, op3);
    
    return inst;
  }
  
  /**
   * Create a load instruction.
   */
  private Instruction createL1Instruction(int opcode, Vector<Object> operands)
  {
    int  op1    = -1;   
    int  op2    = -1; 
    long op3    = 0;    // Immediate field
    int  loadId = -1;
    int  numOps = operands.size();
    
    // If there is a load id, it is always the last operand.
    
    if (operands.get(numOps - 1) instanceof String) {
      loadId = getLoadStoreQueueId((String) operands.get(numOps - 1));
      numOps--;
    }
    
    if (numOps == 3) {
      op1 = ((Integer) operands.get(0)).intValue();
      op2 = ((Integer) operands.get(2)).intValue();  
      op3 = ((Long) operands.get(1)).longValue();    // op3(op2)
    } else if (numOps == 2) {
      op1 = ((Integer) operands.get(0)).intValue();
      op2 = ((Integer) operands.get(1)).intValue();  
    }
    
    LoadInstruction inst = new LoadInstruction(opcode, op1, op2, op3);
    if (loadId > -1)
      inst.setLSQid(loadId);
    
    return inst;
  }

  /**
   * Create a prefetch instruction.
   */
  private Instruction createLPFInstruction(int opcode, Vector<Object> operands)
  {  
    int  op2    = -1; 
    long op3    = 0;    // Immediate field
    int  numOps = operands.size();
    
    // If there is a load id, it is always the last operand.
    
    if (numOps == 2) {
      op2 = ((Integer) operands.get(2)).intValue();  
      op3 = ((Long) operands.get(1)).longValue();    // op3(op2)
    } else if (numOps == 1) {
      op2 = ((Integer) operands.get(1)).intValue();  
    }
    
    LoadInstruction inst = new LoadInstruction(opcode, 0, op2, op3);
    inst.setLSQid(31);
    
    return inst;
  }

  /**
   * Create a store instruction.
   */
  private Instruction createS2Instruction(int opcode, Vector<Object> operands)
  {
    long op1     = 0;   // Immediate field
    int  op2     = -1;
    int  op3     = -1;
    int  storeId = -1;
    int  numOps  = operands.size();
    
    // If there is a store id, it is always the last operand.
    
    if (operands.get(numOps - 1) instanceof String) {
      storeId = getLoadStoreQueueId((String) operands.get(numOps - 1));
      numOps--;
    }
  
    if (numOps == 3) {
      op1 = ((Long) operands.get(0)).longValue();   // op1(op2)
      op2 = ((Integer) operands.get(1)).intValue(); 
      op3 = ((Integer) operands.get(2)).intValue();
    } else if (numOps == 2) {
      op2 = ((Integer) operands.get(0)).intValue(); 
      op3 = ((Integer) operands.get(1)).intValue();
    }
    
    StoreInstruction inst = new StoreInstruction(opcode, op1, op2, op3);
    if (storeId > -1)
      inst.setLSQid(storeId);
    
    return inst;
  }
  
  /**
   * Create a function call.
   */
  private Instruction createCALL(int opcode, Vector<Object> operands)
  {
    String      label  = (String) operands.get(0);
    int         handle = gen.allocateTextArea(label, Trips2Generator.TEXT);
    TripsBranch br     = new TripsBranch(opcode, new SymbolDisplacement(label, handle), 1);
    
    br.additionalRegsKilled(gen.getRegisterSet().getCalleeUses());
    br.additionalRegsUsed(specifyCallUses());
    br.addTarget(lastLabel, 0);  // Assume the last label created was the return address.
    br.markAsCall();
    
    return br;
  }
  
  /**
   * Create a system call.
   */
  private Instruction createSCALL(int opcode, Vector<Object> operands)
  {
    TripsBranch br = new TripsBranch(opcode, 0);
    
    br.additionalRegsKilled(gen.getRegisterSet().getCalleeUses());
    br.additionalRegsUsed(specifyCallUses());
    br.markAsCall();
    
    return br;
  }
  
  /**
   * Create a branch.
   */
  private Instruction createBR(int opcode, Vector<Object> operands)
  {
    String      label = (String) operands.get(0);
    Label       lab   = createLabel(label);
    TripsBranch br    = new TripsBranch(opcode, lab, 0);
    
    return br;
  }
  
  /**
   * Create a branch instruction in the B0 form.
   */
  private Instruction createB0Instruction(int opcode, Vector<Object> operands)
  {
    switch (opcode) {
    case Opcodes.CALL:
    case Opcodes.CALLO:
      return createCALL(opcode, operands);
    case Opcodes.SCALL:
      return createSCALL(opcode, operands);
    default:
      return createBR(opcode, operands);
    }    
  }
  
  /**
   * Create a branch instruction in the B1 form.
   * <p>
   * NOTE this does not handle switch statements correctly.  
   * 
   */
  private Instruction createB1Instruction(int opcode, Vector<Object> operands)
  {
    int         op1     = ((Integer) operands.get(0)).intValue();
    TripsBranch inst    = new TripsBranch(opcode, op1, 0);
    
    inst.additionalRegsUsed(specifyCallUses());

    return inst;
  }
  
  /**
   * Create an instruction.
   */
  private void createInstruction(String  opName,
                                 int[]   predicates,
                                 Vector<Object>  operands,
                                 boolean isPredicatedOnTrue)
  {
    Instruction inst   = null;
    int         opcode = Opcodes.getOp(opName);
    int         format = Opcodes.getFormat(opcode);
    
    switch(format) {
    case Opcodes.ENT:
      inst = createENTInstruction(opcode, operands);
      break;
    case Opcodes.G0:
      inst = createG0Instruction(opcode, operands);
      break;
    case Opcodes.G1:
      inst = createG1Instruction(opcode, operands);
      break;
    case Opcodes.G2:
      inst = createG2Instruction(opcode, operands);
      break;
    case Opcodes.R0:
      inst = createR0Instruction(opcode, operands);
      break;
    case Opcodes.W1:
      inst = createW1Instruction(opcode, operands);
      break;
    case Opcodes.I0:
      inst = createI0Instruction(opcode, operands);
      break;
    case Opcodes.I1:
      inst = createI1Instruction(opcode, operands);
      break;
    case Opcodes.L1:
      inst = createL1Instruction(opcode, operands);
      break; 
    case Opcodes.LPF:
      inst = createLPFInstruction(opcode, operands);
      break; 
    case Opcodes.S2:
      inst = createS2Instruction(opcode, operands);
      break;
    case Opcodes.B0:
      inst = createB0Instruction(opcode, operands);
      break;
    case Opcodes.B1:
      inst = createB1Instruction(opcode, operands);
      break;
    case Opcodes.C0:
    case Opcodes.C1:
      throw new scale.common.InternalError("Not Implemented -- " + opcode);  // TODO
    }
    
    if (predicates != null) {
      if (inst instanceof TripsBranch)
        ((TripsBranch) inst).setPredicates(predicates, isPredicatedOnTrue);
      else
        ((TripsInstruction) inst).setPredicates(predicates, isPredicatedOnTrue);
    }
    
    appendInstruction(inst);
  }

  /**
   * Append an instruction to the end of the current sequence of
   * instructions.
   */
  private void appendInstruction(Instruction inst)
  {
    lastInstruction.setNext(inst);
    lastInstruction = inst;
  }
 
  /**
   * Create a begin marker.
   */
  private void createBeginMarker(String routineName)
  {
    Scribble scribble     = new Scribble(currentRoutine, new SourceC(), gen.getCallGraph());
    this.currentRoutine   = new ProcedureDecl(routineName, null);
    this.firstInstruction = new BeginMarker(scribble);  
    this.lastInstruction  = firstInstruction;
    
    if (globals.contains(routineName)) 
      currentRoutine.setVisibility(Visibility.GLOBAL);
    
    routines.add(firstInstruction);
  }
  
  /**
   * Return true if the instruction has no targets.
   */
  private boolean hasNoTargets(String opName)
  {
    int opcode = Opcodes.getOp(opName); 
    if (Opcodes.getNumTargets(opcode) == 0)
      return true;
   
    return false;
  }
  
  /**
   * Return true if the instruction is predicated.
   */
  private boolean isPredicated(String operand)
  {
    String str = operand.toLowerCase();
    
    if (str.endsWith("_t"))
      return true;
    
    if (str.endsWith("_f"))
      return true;
    
    if (str.endsWith(">"))
      return true;
    
    return false;
  }

  /**
   * Return true if this is an indirect with offset.
   */
  private boolean isIndirectWithOffset(String operand)
  {
    if (operand.indexOf("(") == -1)
      return false;
    
    if (!operand.endsWith(")"))
      return false;
                
    return true;
  }

  /**
   * Return true if this is an immediate.
   */
  private boolean isImmediate(String operand)
  {
    try {
      getImmediate(operand);
    } catch (java.lang.Exception e) {
      return false;
    }
    
    return true;
  }
  
  /**
   * Return true if this is a register.
   */
  private boolean isRegister(String regName)
  {
    String str = regName.toLowerCase();
    
    if (str.startsWith("$g"))
      return true;
    
    if (str.startsWith("$t"))
      return true;
    
    if (str.startsWith("$p"))
      return true;
    
    return false;
  }
  
  /**
   * Return true if this is an optional tag.
   */
  private boolean isTag(String operand)
  {
    if (operand == null)
      return false;
    
    if (operand.indexOf("[") != -1)
      return true;
    
    return false;
  }
  
  /**
   * Return the instruction name without any modifiers ('_t', '_f',
   * '<', '>').
   */
  private String getOp(String opName)
  {
    int end = opName.indexOf("_");
    
    return opName.substring(0, end);
  }
  
  /**
   * Return the index for a label (i.e. main$1).
   */
  private int getLabelIndex(String operand)
  {
    int    begin = operand.indexOf("$") + 1;
    String index = operand.substring(begin, operand.length());
    
    return (int) getImmediate(index);
  }
  
  /**
   * Return the register number (i.e. $g100).
   */
  private int getRegisterNumber(String operand)
  {
    String str = operand.substring(2, operand.length());
    
    return Integer.parseInt(str);
  }
  
  /**
   * Return the offset for an indirect address.
   */
  private long getIndirectOffset(String operand)
  {
    int    end = operand.indexOf("(");
    String str = operand.substring(0, end);
    
    if (str.equals(""))
      return 0;
    
    return Long.parseLong(str);
  }
  
  /**
   * Return the register for an indirect address.
   */
  private int getIndirectRegister(String operand)
  {
    int    start = operand.indexOf("(") + 1;
    String str   = operand.substring(start, operand.length() - 1);
    
    return getRegisterNumber(str);
  }
  
  /**
   * Return the immediate.
   */
  private long getImmediate(String operand)
  {
    if (operand.startsWith("0x")) {
      operand = operand.substring(2, operand.length());
      return Long.parseLong(operand, 16);
    }
   
    return Long.parseLong(operand);
  }
  
  /**
   * Return the id for a load/store queue.
   */
  private int getLoadStoreQueueId(String operand)
  {
    int start = 2;  // S[ or L[
    int end   = operand.indexOf("]");
    
    return (int) getImmediate(operand.substring(start, end));
  }

  private void requireWord(StreamTokenizer st) throws java.lang.Exception
  {
    if (st.ttype != StreamTokenizer.TT_WORD) 
      throw new java.lang.Exception("Expecting a word at line number: " + st.lineno());
  }

  private void requireEOL(StreamTokenizer st) throws java.lang.Exception
  {
    if (st.ttype != StreamTokenizer.TT_EOL) 
      throw new java.lang.Exception("Expecting an end-of-line marker at line number: " + st.lineno());
  }

  private void requireComma(StreamTokenizer st) throws java.lang.Exception
  {
    if (st.ttype != ',') 
      throw new java.lang.Exception("Expecting a ',' at line number: " + st.lineno());
  }

  private void requireAngBracket(StreamTokenizer st) throws java.lang.Exception
  {
    if (st.ttype != '<') 
      throw new java.lang.Exception("Expecting a '<' at line number: " + st.lineno());
  }

  private void requireCommaOrBracket(StreamTokenizer st) throws java.lang.Exception
  {
    if (st.ttype != ',' && st.ttype != '>') 
      throw new java.lang.Exception("Expect a ',' or '>' at line number: " + st.lineno());
  }

  private void ignoreEOL(StreamTokenizer st) throws java.lang.Exception
  {
    while ((st.ttype == StreamTokenizer.TT_EOL) && (st.ttype != StreamTokenizer.TT_EOF))
      st.nextToken();
  }
}
