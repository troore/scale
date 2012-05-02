package scale.j2s;

import scale.common.Stack;
import java.util.BitSet;

import scale.jcr.*;
import scale.common.*;
import scale.annot.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.expr.*;
import scale.callGraph.*;
import scale.frontend.java.SourceJava;

import scale.score.*;
import scale.score.chords.*;
import scale.score.expr.*;
import scale.clef2scribble.ExprTuple;
import scale.clef2scribble.GotoFix;

/**
 * This class processes the Code Attribute for a Java class method and
 * produces a Scribble CFG.  We assume that the byte code has been
 * verified.  We also assume that no loop in the Java byte code exits
 * with a change in the number of entries in the Java stack.
 * <p>
 * $Id: ScribbleGen.java,v 1.46 2007-10-04 19:58:14 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ScribbleGen
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;

  /**
   * A private class to store information about basic blocks.
   */
  private static class BasicBlock
  {
    public int            index;     // Current index into the javaStack.
    public VariableDecl[] stack;     // The Java stack at the beginning of this baic block.
    public ExprTuple      exp;       // The first Chord in the basic block.
    public VariableDecl[] localVars; // The local variables at the beginning of this baic block.

    public BasicBlock(int stackIndex, VariableDecl[] stack, ExprTuple exp, VariableDecl[] lv)
    {
      this.index     = stackIndex;
      this.stack     = stack.clone();
      this.exp       = exp;
      this.localVars = lv.clone();
    }

    public Chord getBegin()
    {
      return exp.getBegin();
    }
  }

  private static UniqueName un = new UniqueName("_j2s"); // Generate names for temporaries.

  private HashMap<String, VariableDecl> localVarMap;       // Map from name to declaration.
  private HashMap<String, Chord>        exceptionMap;      // Map from key to Chord that generates the exception.
  private Vector<Declaration>           tempVars;          // A list of all the temporary variables created.
  private IntMap<BasicBlock>            bbChords;          // Map from basic block pc to basic block information (BasicBlock).
  private Stack<Chord>                  chords;            // Record all the created Chords.
  private VariableDecl[]   localVars;         // The last definition of this local variable.
  private VariableDecl[]   javaStack;         // A pseudo stack that tracks the Java stack during conversion of basic blocks.
  private int[]            typeStack;         // A pseudo stack that tracks the Java stack during conversion of basic blocks.
  private int              stackIndex;        // Current index into the javaStack.
  private int[]            bbStack;           // Stack of basic block indexes (pc)
  private int              bbIndex;           // Current index into bbStack.
  private Scribble         scribble;
  private ExprTuple        exp;
  private GotoFix          gotos;             // Record branch points for later linking.
  private VariableDecl     returnVar;         // The temproary variable used to hold the value of the method.
  private BeginChord       begin;             // First Chord of the CFG.
  private EndChord         end;               // Last Chord of the CFG.
  private Chord[]          exceptionPoints;   // Array of extry points for exception handlers.
  private ExceptionEntry[] exceptionEntries;  // The exeception table for the code.
  private VariableDecl     exceptionTableVar; // The table used at run time to look up handlers for an exception.
  private VariableDecl     exceptionPcVar;    // The variable used to hold the "pc" value at which an exception occured.
  private Chord            runTimeException;  // The runtime switch that selects catch blocks.
  private int              currentPc;         // Java byte code index of the current code.
  private CallGraph        cg;
  private ClassFile        cf;                // The class file information for this class.
  private ProcedureDecl    pd;                // The ProcedureDecl for this method.
  private CodeAttribute    ca;                // The byte codes for this method.
  private Java2Scribble    j2s;               // Global information for the class of this method.
  private ClassStuff       cs;                // Information about the class.
  private int[]            sla;               // Information about source line numbers.
  private int[]            slaPc;             // Pc range for source line numbers.
  private boolean          trace;

  /**
   * @param j2s contains the global information for the class of this method
   * @param cs is the information about the class containing this method
   * @param pd is the ProcedureDecl for this method
   * @param cg is the CallGraph for this method
   */
  public ScribbleGen(Java2Scribble j2s, ClassStuff cs, ProcedureDecl pd, CallGraph cg)
  {
    this.j2s          = j2s;
    this.cs           = cs;
    this.cf           = cs.cf;
    this.pd           = pd;
    this.cg           = cg;
    this.stackIndex   = 0;
    this.javaStack    = new VariableDecl[100];
    this.typeStack    = new int[100];
    this.localVarMap  = new HashMap<String, VariableDecl>(203);
    this.scribble     = new Scribble(this.pd, new SourceJava(), cg);
    this.exceptionMap = new HashMap<String, Chord>(11);
    this.tempVars     = new Vector<Declaration>(100);
    this.bbStack      = new int[128];
    this.bbIndex      = 0;
    this.bbChords     = new IntMap<BasicBlock>(203);
    this.exp          = new ExprTuple(null, null);
    this.gotos        = new GotoFix();
    this.chords       = new Stack<Chord>();

    assert setTrace(pd.getName());

    cg.recordRoutine(pd);
  }

  private boolean setTrace(String name)
  {
    trace = Debug.trace(name, classTrace, 3);
    return true;
  }

  /**
   * Create a new temporary variable for use by the optimized code.
   * The variable definition is added to the set of variables for this
   * CFG.
   */
  protected VariableDecl genTemp(Type t)
  {
    VariableDecl vd = new VariableDecl(un.genName(), t);
    vd.setTemporary();
    return vd;
  }

  /**
   * Create the name of a variable representing a Java local variable.
   * @param index is the local variable index
   * @param type specifies the type
   * @see scale.jcr.CodeAttribute#typeSpecifier
   * @return the name of the variable
   */
  private String localVariableName(int index, int type)
  {
    StringBuffer buf = new StringBuffer("_lv");
    buf.append(CodeAttribute.typeSpecifier[type]);
    buf.append(Integer.toString(index));
    return buf.toString();
  }

  /**
   * Create the name of a variable representing a Java stack location.
   * @param index is the stack location
   * @param type specifies the type
   * @see scale.jcr.CodeAttribute#typeSpecifier
   * @return the name of the variable
   */
  private String stackVariableName(int index, int type)
  {
    StringBuffer buf = new StringBuffer("_sv");
    buf.append(CodeAttribute.typeSpecifier[type]);
    buf.append(Integer.toString(index));
    return buf.toString();
  }

  /**
   * Define a local variable.
   * @param index is the local variable index
   * @param fd is the definition of the local variable
   */
  public void defLocal(int index, VariableDecl fd)
  {
    String name = localVariableName(index, j2s.getTypeSpecifier(fd.getType()));
    localVarMap.put(name, fd);
    localVars[index] = fd;
  }

  private VariableDecl defineLocal(int index, int type)
  {
    String name = localVariableName(index, type);
    VariableDecl vd = localVarMap.get(name);
    if (vd == null) {
      vd = new VariableDecl(name, j2s.typeMap[type]);
      tempVars.addElement(vd);
      localVarMap.put(name, vd);
    }
    localVars[index] = vd;
    return vd;
  }

  private void storeStack(int index, VariableDecl vd, int type)
  {
    if (index >= javaStack.length) {
      VariableDecl[] njs = new VariableDecl[index + 100];
      System.arraycopy(javaStack, 0, njs, 0, javaStack.length);
      javaStack = njs;
      int[] nts = new int[index + 100];
      System.arraycopy(typeStack, 0, nts, 0, javaStack.length);
      typeStack = nts;
    }
    javaStack[index] = vd;
    typeStack[index] = type;
  }

  private VariableDecl defineStackVar(int index, int type)
  {
    String name = stackVariableName(index, type);
    VariableDecl vd = localVarMap.get(name);
    if (vd == null) {
      vd = new VariableDecl(name, j2s.typeMap[type]);
      tempVars.addElement(vd);
      localVarMap.put(name, vd);
    }
    storeStack(index, vd, type);
    return vd;
  }

  private VariableDecl getLocal(int index, int type)
  {
    VariableDecl lvd  = localVars[index];
    String       name = localVariableName(index, type);
    VariableDecl mvd  = localVarMap.get(name);
    if (mvd != lvd)
      throw new scale.common.InternalError("Local var mismatch " + mvd + " <-> " + lvd);
    return lvd;
  }

  private VariableDecl getStackVar(int index, int type)
  {
    VariableDecl lvd  = javaStack[index];
    String       name = stackVariableName(index, type);
    VariableDecl mvd  = localVarMap.get(name);
    if (mvd != lvd)
      throw new scale.common.InternalError("Local var mismatch " + mvd + " <-> " + lvd);
    return lvd;
  }

  private void recordNewChord(Chord s)
  {
    chords.push(s);
    if (slaPc != null) {
      int index = binarySearch(currentPc, slaPc);
      if (index >= 0)
        s.setSourceLineNumber(sla[index]);
    }
  }

  private int binarySearch(int value, int[] array)
  {
    int end   = array.length;
    int start = -1;
    int index = (end - start) / 2;

    while (index != end) {
      if (value >= array[index]) {
        if ((index == array.length - 1) || (value < array[index + 1]))
          return index;
        start = index;
        index += (end - start) / 2;
      } else {
        end = index;
        index -= (end - start) / 2;
      }
    }
    return -1;
  }

  private void makeAssign(VariableDecl vd, Expr rhs)
  {
    LoadDeclAddressExpr lhs = new LoadDeclAddressExpr(vd);
    ExprChord           ec  = new ExprChord(lhs, rhs);

    recordNewChord(ec);
    exp.append(ec);
  }

  private void makeAssign(Expr lhs, Expr rhs)
  {
    ExprChord ec = new ExprChord(lhs, rhs);

    recordNewChord(ec);
    exp.append(ec);
  }

  private Type getNonConstType(Type t)
  {
    if (t.isConst()) {
      RefType rt = t.returnRefType();
      if (rt != null) {
        t = getNonConstType(rt.getRefTo());
      } else
        throw new scale.common.InternalError("Const type not RefType - " + t);
    }
    return t;
  }

  private LoadDeclValueExpr makeTempAssign(Expr rhs)
  {
    VariableDecl tv = genTemp(getNonConstType(rhs.getType()));

    tempVars.addElement(tv);

    makeAssign(tv, rhs);

    return new LoadDeclValueExpr(tv);    
  }

  private BasicBlock getBasicBlock(int pc)
  {
    BasicBlock s = bbChords.get(pc);
    if (s != null)
      return s;

    Chord nc = null;
    if (pc == 0) {
      begin = new BeginChord(scribble);
      nc = begin;
    } else {
      nc = new NullChord();
      recordNewChord(nc);
    }
    s = new BasicBlock(stackIndex, javaStack, new ExprTuple(nc, nc), localVars);
    bbChords.put(pc, s);
    gotos.defineLabel(nc, nc);

    return s;
  }

  private void push(Expr v, int type)
  {
    VariableDecl vd = defineStackVar(stackIndex, type);
    makeAssign(vd, v);
    stackIndex++;
  }

  private void push2(Expr v, int type)
  {
    push(v, type);
    push(v, type);
  }

  private Expr pop()
  {
    VariableDecl vd = javaStack[--stackIndex];
    return new LoadDeclValueExpr(vd);
  }

  private Expr pop2()
  {
    stackIndex--;
    return pop();
  }

  private Expr peek(int index)
  {
    VariableDecl vd = javaStack[index];
    return new LoadDeclValueExpr(vd);
  }

  private Expr peek()
  {
    return peek(stackIndex - 1);
  }

  private void replace(Expr exp, int type)
  {
    VariableDecl vdm1 = defineStackVar(stackIndex - 1, type);
    makeAssign(vdm1, exp);
  }

  private void replace2(Expr exp, int type)
  {
    VariableDecl vdm1 = defineStackVar(stackIndex - 1, type);
    VariableDecl vdm2 = defineStackVar(stackIndex - 2, type);
    makeAssign(vdm1, exp);
    makeAssign(vdm2, exp);
  }

  /**
   * The particular catch to use is determined by the Java byte code
   * PC and the exception instance.  The exception instance is not
   * know at compile time if the "JVM" did not throw the exception.
   * Therefore, a table lookup is used at run time to determine which
   * catch handler to use.  A SwitchChord is constructed that selects
   * the handler based upon the lookup.  The lookup is performed at
   * runtime by the __lookupException procedure.
   * @param curPc is the current Java byte code PC
   * @return the runtime exception dispatcher for this method
   */
  private Chord getExceptionDispatcher(int curPc)
  {
    int len = exceptionEntries.length;

    if (len <=  0) { // If there is none, return the method exit.
      runTimeException = end; // Use the return if no catch handlers.
      return end;
    }

    for (int i = 0; i < len; i++) { // Find if there is a possible handler.
      ExceptionEntry ex = exceptionEntries[i];

      if ((curPc >= ex.startPc) && (curPc < ex.endPc)) { // There is at least one possible handler

        if (ex.catchType == 0)
          return exceptionPoints[i]; // The finally handler is the only possibility..

        if (runTimeException != null) // If we have the exception dispatcher already, return it.
          return runTimeException;

        // Set up the exception dispatcher.

        exceptionTableVar = j2s.makeExceptionTable(cf, pd.getName(), exceptionEntries);

        Vector<Expr> args = new Vector<Expr>(3);
        args.addElement(new LoadDeclValueExpr(exceptionPcVar));
        args.addElement(new LoadDeclValueExpr(j2s.globalExceptionVariable));
        args.addElement(new LoadDeclAddressExpr(exceptionTableVar));

        Expr            pred    = new CallFunctionExpr(j2s.intType, new LoadDeclValueExpr(j2s.lookupExceptionProc), args);
        Vector<Object>  keys    = new Vector<Object>(len + 1);
        Vector<Chord>   targets = new Vector<Chord>(len + 1);
        for (int j = 0; j < len; j++) { // Add a case for each exception handler.
          ExceptionEntry exception = exceptionEntries[j];
          keys.addElement(new Integer(j));
          targets.addElement(getBasicBlock(exception.handlerPc).getBegin());
        }

        keys.addElement(""); // Add default case.
        targets.addElement(end);

        runTimeException = new SwitchChord(pred, keys, targets);
        gotos.defineLabel(runTimeException, runTimeException);
        return runTimeException;
      }
    }

    // If there is no possible exception handler for this exception
    // based upon the current Java byte code PC, use the method exit.

    return end;
  }

  /**
   * Generate a test for the occurance of any exception.
   * This is used after a method call.
   * @param curPc is the current Java byte code PC
   */
  private void genExceptionTest(int curPc)
  {
    LoadDeclValueExpr value = new LoadDeclValueExpr(j2s.globalExceptionVariable);

    if (exceptionPcVar == null) {
      exceptionPcVar = new VariableDecl("__javaPc", j2s.intType);
      tempVars.addElement(exceptionPcVar);
    }

    makeAssign(new LoadDeclAddressExpr(exceptionPcVar), new LiteralExpr(j2s.getIntLiteral(currentPc)));

    Expr            test    = new NotEqualExpr(BooleanType.type, value, new LiteralExpr(j2s.nil));
    NullChord       nc      = new NullChord();
    IfThenElseChord ifchord = new IfThenElseChord(test, getExceptionDispatcher(currentPc), nc);

    recordNewChord(nc);
    recordNewChord(ifchord);

    exp.append(ifchord);
    exp = new ExprTuple(nc, nc);
  }

  /**
   * If both the Java byte code PC and the exception is known at
   * compile time, the appropriate catch handler can be jumped to
   * directly.
   * @param curPc is the Java byte code PC where the exception is generated.
   * @param exception is the qualified exception name
   * @return the catch handler.
   */
  private Chord findException(int curPc, String exception)
  {
    for (int i = 0; i < exceptionEntries.length; i++) {
      ExceptionEntry ex = exceptionEntries[i];

      if ((curPc >= ex.startPc) && (curPc < ex.endPc)) {

        // This exception handles this PC value.
        // Check if it handles this exception.

        if (ex.catchType == 0)
          return exceptionPoints[i]; // It's the finally handler.

        ClassCPInfo cinfo = (ClassCPInfo) cf.getCP(ex.catchType);
        String      cname = cf.getName(cinfo.getNameIndex());
        String      ename = exception;

        while (true) { // Check the super classes of the exception thrown
          if (ename.equals(cname))
            return exceptionPoints[i];

          ClassStuff  ecs = j2s.getClass(ename);
          ClassFile   ecf = ecs.cf;
          int         si  = ecf.getSuperClass();

          if (si == 0)
            break;

          ClassCPInfo einfo = (ClassCPInfo) ecf.getCP(si);
          ename = ecf.getName(einfo.getNameIndex());
        }
      }
    }

    return end;
  }

  /**
   * Generate code that throws an exception at run time.  Exception
   * are generated by the "JVM" using the __makeException procedure.
   * @param exception is the qualified exception name
   * @return the code that generates the exception
   */
  private Chord makeException(String exception)
  {
    Chord  ec   = findException(currentPc, exception);
    String key  = exception + ec.getNodeID();
    Chord  call = exceptionMap.get(key);

    if (call != null)
      return call;

    VariableDecl except = j2s.getClassDecl(exception);
    Vector<Expr> args   = new Vector<Expr>(1);
    args.addElement(new LoadDeclAddressExpr(except));

    Expr                rhs = new CallFunctionExpr(j2s.voidp, new LoadDeclValueExpr(j2s.makeExceptionProc), args);
    LoadDeclAddressExpr lhs = new LoadDeclAddressExpr(j2s.globalExceptionVariable);

    call = new ExprChord(lhs, rhs);

    recordNewChord(call);

    exceptionMap.put(key, call);

    gotos.add(call, ec, Boolean.TRUE);

    return call;
  }

  /**
   * Generate the test for the divide by zero exception.
   * @param value is the value to test at runtime
   */
  private void genZeroValueExceptionTest(Expr value)
  {
    Chord           ec      = makeException("java/lang/ArithmeticException");
    Expr            test    = new EqualityExpr(BooleanType.type, value, new LiteralExpr(j2s.int0));
    NullChord       nc      = new NullChord();
    IfThenElseChord ifchord = new IfThenElseChord(test, ec, nc);

    recordNewChord(nc);
    recordNewChord(ifchord);

    exp.append(ifchord);
    exp = new ExprTuple(nc, nc);
  }

  /**
   * Generate the test for the array store exception.
   * @param array is the array being stored into
   * @param value is the value being stored
   */
  private void genArrayStoreExceptionTest(Expr array, Expr value)
  {
    Chord ec = makeException("java/lang/ArrayStoreException");

    /******* Not correct ************/
    Expr            test    = new LessExpr(BooleanType.type, value, new LiteralExpr(j2s.int0));
    NullChord       nc      = new NullChord();
    IfThenElseChord ifchord = new IfThenElseChord(test, ec, nc);

    recordNewChord(nc);
    recordNewChord(ifchord);

    exp.append(ifchord);
    exp = new ExprTuple(nc, nc);
  }

  /**
   * Generate the test for the illegal monitor state exception.
   * @param value is the value to test at runtime
   */
  private void genIllegalMonitorStateExceptionTest(Expr value)
  {
    Chord ec = makeException("java/lang/IllegalMonitorStateException");

    /******* Not correct ************/
    Expr            test    = new LessExpr(BooleanType.type, value, new LiteralExpr(j2s.int0));
    NullChord       nc      = new NullChord();
    IfThenElseChord ifchord = new IfThenElseChord(test, ec, nc);

    recordNewChord(nc);
    recordNewChord(ifchord);

    exp.append(ifchord);
    exp = new ExprTuple(nc, nc);
  }

  /**
   * Generate the test for the negative allocation size exception.
   * @param value is the value to test at runtime
   */
  private void genNegativeSizeExceptionTest(Expr value)
  {
    Chord ec = makeException("java/lang/NegativeSizeException");

    Expr            test    = new LessExpr(BooleanType.type, value, new LiteralExpr(j2s.int0));
    NullChord       nc      = new NullChord();
    IfThenElseChord ifchord = new IfThenElseChord(test, ec, nc);

    recordNewChord(nc);
    recordNewChord(ifchord);

    exp.append(ifchord);
    exp = new ExprTuple(nc, nc);
  }

  /**
   * Generate the test for the array index out of bounds exception.
   * @param array is the array
   * @param index is the array index value
   */
  private void genArrayIndexOutOfBoundsExceptionTest(Expr array, Expr index)
  {
    Chord           ec       = makeException("java/lang/ArrayIndexOutOfBoundsException");
    Expr            test1    = new LessExpr(BooleanType.type, index.copy(), new LiteralExpr(j2s.int0));
    NullChord       nc1      = new NullChord();
    IfThenElseChord ifchord1 = new IfThenElseChord(test1, ec, nc1);

    recordNewChord(nc1);
    recordNewChord(ifchord1);

    exp.append(ifchord1);
    exp = new ExprTuple(nc1, nc1);

    Expr            op       = getArrayLength(array);
    Expr            al       = makeTempAssign(LoadValueIndirectExpr.create(op));
    Expr            test2    = new GreaterEqualExpr(BooleanType.type, index, al);
    NullChord       nc2      = new NullChord();
    IfThenElseChord ifchord2 = new IfThenElseChord(test2, ec, nc2);

    recordNewChord(nc2);
    recordNewChord(ifchord2);

    exp.append(ifchord2);
    exp = new ExprTuple(nc2, nc2);
  }

  /**
   * Generate the test for the null dereference exception.
   * @param address is the address to test at runtime
   */
  private void genNullPointerExceptionTest(Expr address)
  {
    Chord ec = makeException("java/lang/NullPointerException");

    Expr            test    = new EqualityExpr(BooleanType.type, address, new LiteralExpr(j2s.nil));
    NullChord       nc      = new NullChord();
    IfThenElseChord ifchord = new IfThenElseChord(test, ec, nc);

    recordNewChord(nc);
    recordNewChord(ifchord);

    exp.append(ifchord);
    exp = new ExprTuple(nc, nc);
  }

  private void dup()
  {
    int          tm1  = typeStack[stackIndex - 1];
    Expr         em1  = peek(stackIndex - 1);
    VariableDecl vdp0 = defineStackVar(stackIndex, tm1);

    makeAssign(vdp0, em1);
    stackIndex++;
  }
    
  private void dup_x1()
  {
    int          tm1  = typeStack[stackIndex - 1];
    int          tm2  = typeStack[stackIndex - 2];
    Expr         em1  = peek(stackIndex - 1);
    Expr         em2  = peek(stackIndex - 2);
    VariableDecl vdp0 = defineStackVar(stackIndex + 0, tm1);
    VariableDecl vdm1 = defineStackVar(stackIndex - 1, tm2);
    VariableDecl vdm2 = defineStackVar(stackIndex - 2, tm1);

    makeAssign(vdp0, em1);
    makeAssign(vdm1, em2);
    makeAssign(vdm2, new LoadDeclValueExpr(vdp0));
    stackIndex++;
  }
    
  private void dup_x2()
  {
    int          tm1  = typeStack[stackIndex - 1];
    int          tm2  = typeStack[stackIndex - 2];
    int          tm3  = typeStack[stackIndex - 3];
    Expr         em1  = peek(stackIndex - 1);
    Expr         em2  = peek(stackIndex - 2);
    Expr         em3  = peek(stackIndex - 3);
    VariableDecl vdp0 = defineStackVar(stackIndex + 0, tm1);
    VariableDecl vdm1 = defineStackVar(stackIndex - 1, tm2);
    VariableDecl vdm2 = defineStackVar(stackIndex - 2, tm1);
    VariableDecl vdm3 = defineStackVar(stackIndex - 3, tm1);

    makeAssign(vdp0, em1);
    makeAssign(vdm1, em2);
    makeAssign(vdm2, em3);
    makeAssign(vdm3, new LoadDeclValueExpr(vdp0));
    stackIndex++;
  }
    
  private void dup2()
  {
    int          tm1  = typeStack[stackIndex - 1];
    int          tm2  = typeStack[stackIndex - 2];
    Expr         em1  = peek(stackIndex - 1);
    Expr         em2  = peek(stackIndex - 2);
    VariableDecl vdp1 = defineStackVar(stackIndex + 1, tm1);
    VariableDecl vdp0 = defineStackVar(stackIndex + 0, tm2);

    makeAssign(vdp1, em1);
    makeAssign(vdp0, em2);
    stackIndex += 2;
  }
    
  private void dup2_x1()
  {
    int          tm1  = typeStack[stackIndex - 1];
    int          tm2  = typeStack[stackIndex - 2];
    int          tm3  = typeStack[stackIndex - 3];
    Expr         em1  = peek(stackIndex - 1);
    Expr         em2  = peek(stackIndex - 2);
    Expr         em3  = peek(stackIndex - 3);
    VariableDecl vdp1 = defineStackVar(stackIndex + 1, tm1);
    VariableDecl vdp0 = defineStackVar(stackIndex + 0, tm2);
    VariableDecl vdm1 = defineStackVar(stackIndex - 1, tm3);
    VariableDecl vdm2 = defineStackVar(stackIndex - 2, tm1);
    VariableDecl vdm3 = defineStackVar(stackIndex - 3, tm2);

    makeAssign(vdp1, em1);
    makeAssign(vdp0, em2);
    makeAssign(vdm1, em3);
    makeAssign(vdm2, new LoadDeclValueExpr(vdp1));
    makeAssign(vdm3, new LoadDeclValueExpr(vdp0));
    stackIndex += 2;
  }
    
  private void dup2_x2()
  {
    int          tm1  = typeStack[stackIndex - 1];
    int          tm2  = typeStack[stackIndex - 2];
    int          tm3  = typeStack[stackIndex - 3];
    int          tm4  = typeStack[stackIndex - 4];
    Expr         em1  = peek(stackIndex - 1);
    Expr         em2  = peek(stackIndex - 2);
    Expr         em3  = peek(stackIndex - 3);
    Expr         em4  = peek(stackIndex - 4);
    VariableDecl vdp1 = defineStackVar(stackIndex + 1, tm1);
    VariableDecl vdp0 = defineStackVar(stackIndex + 0, tm2);
    VariableDecl vdm1 = defineStackVar(stackIndex - 1, tm3);
    VariableDecl vdm2 = defineStackVar(stackIndex - 2, tm4);
    VariableDecl vdm3 = defineStackVar(stackIndex - 3, tm1);
    VariableDecl vdm4 = defineStackVar(stackIndex - 4, tm2);

    makeAssign(vdp1, em1);
    makeAssign(vdp0, em2);
    makeAssign(vdm1, em3);
    makeAssign(vdm2, em4);
    makeAssign(vdm3, new LoadDeclValueExpr(vdp1));
    makeAssign(vdm4, new LoadDeclValueExpr(vdp0));
    stackIndex += 2;
  }
    
  private void swap()
  {
    int          tm1  = typeStack[stackIndex - 1];
    int          tm2  = typeStack[stackIndex - 2];
    Expr         em1  = peek(stackIndex - 1);
    Expr         em2  = peek(stackIndex - 2);
    VariableDecl vdm1 = defineStackVar(stackIndex - 1, tm1);
    VariableDecl vdm2 = defineStackVar(stackIndex - 2, tm2);
    Expr         tmp  = makeTempAssign(em1);

    makeAssign(vdm1, em2);
    makeAssign(vdm2, tmp);
  }

  private void pushConst(Literal lit, int type)
  {
    push(new LiteralExpr(lit), type);
  }

  private void pushConst(Literal lit)
  {
    int type = j2s.getTypeSpecifier(lit.getType());
    push(new LiteralExpr(lit), type);
  }

  private void pushConst2(Literal lit, int type)
  {
    push2(new LiteralExpr(lit), type);
  }

  private void pushConst2(Literal lit)
  {
    int type = j2s.getTypeSpecifier(lit.getType());
    push2(new LiteralExpr(lit), type);
  }

  private void loadConst(int index)
  {
    Object lit = j2s.getLiteral(cf, index);
    if (lit instanceof Literal) {
      pushConst((Literal) lit);
      return;
    }
    VariableDecl tv   = genTemp(j2s.stringpType);
    Vector<Expr> args = new Vector<Expr>(2);
    Expr         proc = new LoadDeclValueExpr(j2s.createStringProc);

    args.addElement(new LoadDeclAddressExpr(tv));
    args.addElement(new LoadDeclAddressExpr((VariableDecl) lit));

    Expr      ms = new CallFunctionExpr(VoidType.type, proc, args);
    ExprChord ec = new ExprChord(ms);

    recordNewChord(ec);
    exp.append(ec);

    tempVars.addElement(tv);

    push(new LoadDeclValueExpr(tv), j2s.getTypeSpecifier(j2s.voidp));    
  }

  private void loadConst2(int index)
  {
    Object lit = j2s.getLiteral(cf, index);
    if (lit instanceof Literal) {
      pushConst2((Literal) lit);
      return;
    }
    throw new scale.common.InternalError("Invalid load constant " + lit);    
  }

  private void load(int index, int type)
  {
    push(new LoadDeclValueExpr(localVars[index]), type);
  }

  private void load2(int index, int type)
  {
    push2(new LoadDeclValueExpr(getLocal(index, type)), type);
  }

  private void load(int index)
  {
    push(new LoadDeclValueExpr(localVars[index]), CodeAttribute.T_ADDRESS);
  }

  private void store(int index, int type)
  {
    Expr v = pop();
    makeAssign(defineLocal(index, type), v);
  }

  private void store2(int index, int type)
  {
    Expr v = pop2();
    makeAssign(defineLocal(index, type), v);
  }

  private void store(int index)
  {
    Expr v = pop();
    makeAssign(defineLocal(index, CodeAttribute.T_ADDRESS), v);
  }

  private Expr getArrayField(Expr array, String fname)
  {
    FieldDecl d    = j2s.getArrayField(fname);
    Expr      cast = makeTempAssign(ConversionExpr.create(PointerType.create(j2s.getArrayHeaderType()), array, CastMode.CAST));

    return makeTempAssign(new LoadFieldValueExpr(cast, d));
  }

  private Expr getArrayLength(Expr array)
  {
    return getArrayField(array, "length");
  }

  private Expr createArrayRef(Expr array)
  {
    return getArrayField(array, "array");
  }

  private Expr createSubscriptExpr(Expr index, Expr op, Type pt)
  {
    Expr[] indicies = new Expr[1];
    Expr[] mins     = new Expr[1];
    Expr[] sizes    = new Expr[1];

    indicies[0] = index;
    mins[0]     = new LiteralExpr(j2s.int0);
    sizes[0]    = new LiteralExpr(j2s.int1);

    return new SubscriptExpr(pt, op, indicies, mins, sizes);
  }

  private Expr arrayLoad(Expr index, Expr array, Type type)
  {
    Expr        op = createArrayRef(array);
    PointerType pt = PointerType.create(type);

    return createSubscriptExpr(index, op, pt);
  }

  private void aload(int type)
  {
    Expr index = pop();
    Expr array = pop();

    genNullPointerExceptionTest(array.copy());
    genArrayIndexOutOfBoundsExceptionTest(array.copy(), index.copy());

    Expr value = arrayLoad(index, array, j2s.typeMap[type]);
    push(value, type);
  }

  private void aload2(int type)
  {
    Expr index = pop();
    Expr array = pop();

    genNullPointerExceptionTest(array.copy());
    genArrayIndexOutOfBoundsExceptionTest(array.copy(), index.copy());

    Expr value = arrayLoad(index, array, j2s.typeMap[type]);
    push2(value, type);
  }

  private void aload()
  {
    Expr index = pop();
    Expr array = pop();

    genNullPointerExceptionTest(array.copy());
    genArrayIndexOutOfBoundsExceptionTest(array.copy(), index.copy());

    push(arrayLoad(index, array, j2s.voidp), CodeAttribute.T_ADDRESS);
  }

  private void arrayStore(Expr rhs, Expr index, Expr array, Type type)
  {
    Expr        op  = createArrayRef(array);
    PointerType pt  = PointerType.create(type);
    Expr        lhs = createSubscriptExpr(index, op, pt);
    ExprChord   ec  = new ExprChord(lhs, rhs);

    recordNewChord(ec);

    exp.append(ec);
  }

  private void astore(int type)
  {
    Expr rhs   = pop(); /* Value to be stored */
    Expr index = pop(); /* Index in the array */
    Expr array = pop(); /* The "array" object */

    genNullPointerExceptionTest(array.copy());
    genArrayIndexOutOfBoundsExceptionTest(array.copy(), index.copy());

    arrayStore(rhs, index, array, j2s.typeMap[type]);
  }

  private void astore2(int type)
  {
    Expr rhs   = pop2();
    pop();
    Expr index = pop();
    Expr array = pop();

    genNullPointerExceptionTest(array.copy());
    genArrayIndexOutOfBoundsExceptionTest(array.copy(), index.copy());

    arrayStore(rhs, index, array, j2s.typeMap[type]);
  }

  private void astore()
  {
    Expr rhs   = pop();
    Expr index = pop();
    Expr array = pop();

    genNullPointerExceptionTest(array.copy());
    genArrayIndexOutOfBoundsExceptionTest(array.copy(), index.copy());
    genArrayStoreExceptionTest(array.copy(), rhs.copy());

    Type type  = rhs.getType();
    arrayStore(rhs, index, array, type);
  }

  private void add(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    Expr ad = new AdditionExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void add2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    Expr ad = new AdditionExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void sub(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    Expr ad = new SubtractionExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void sub2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    Expr ad = new SubtractionExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void mult(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    Expr ad = new MultiplicationExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void mult2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    Expr ad = new MultiplicationExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void divide(int type)
  {
    Expr ra = pop();

    if (CodeAttribute.isInteger[type])
      genZeroValueExceptionTest(ra.copy());

    Expr la = peek();
    Expr ad = new DivisionExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void divide2(int type)
  {
    Expr ra = pop2();

    if (CodeAttribute.isInteger[type])
      genZeroValueExceptionTest(ra.copy());

    Expr la = peek();
    Expr ad = new DivisionExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void remainder(int type)
  {
    Expr ra = pop();

    if (CodeAttribute.isInteger[type])
      genZeroValueExceptionTest(ra.copy());

    Expr la = peek();
    Expr ad = new RemainderExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void remainder2(int type)
  {
    Expr ra = pop2();

    if (CodeAttribute.isInteger[type])
      genZeroValueExceptionTest(ra.copy());

    Expr la = peek();
    Expr ad = new RemainderExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void and(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    Expr ad = new BitAndExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void and2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    Expr ad = new BitAndExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void or(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    Expr ad = new BitOrExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void or2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    Expr ad = new BitOrExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void xor(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    Expr ad = new BitXorExpr(j2s.typeMap[type], la, ra);
    replace(ad, type);
  }

  private void xor2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    Expr ad = new BitXorExpr(j2s.typeMap[type], la, ra);
    replace2(ad, type);
  }

  private void negate(int type)
  {
    Expr ra  = peek();
    Expr nra = new NegativeExpr(ra);
    replace(nra, type);
  }

  private void negate2(int type)
  {
    Expr ra  = peek();
    Expr nra = new NegativeExpr(ra);
    replace2(nra, type);
  }

  private void lshift(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    BitShiftExpr ad = new BitShiftExpr(j2s.typeMap[type], la, ra, ShiftMode.Left);
    replace(ad, type);
  }

  private void lshift2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    BitShiftExpr ad = new BitShiftExpr(j2s.typeMap[type], la, ra, ShiftMode.Left);
    replace2(ad, type);
  }

  private void rshift(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    BitShiftExpr ad = new BitShiftExpr(j2s.typeMap[type], la, ra, ShiftMode.SignedRight);
    replace(ad, type);
  }

  private void rshift2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    BitShiftExpr ad = new BitShiftExpr(j2s.typeMap[type], la, ra, ShiftMode.SignedRight);
    replace2(ad, type);
  }

  private void urshift(int type)
  {
    Expr ra = pop();
    Expr la = peek();
    BitShiftExpr ad = new BitShiftExpr(j2s.typeMap[type], la, ra, ShiftMode.UnsignedRight);
    replace(ad, type);
  }

  private void urshift2(int type)
  {
    Expr ra = pop2();
    Expr la = peek();
    BitShiftExpr ad = new BitShiftExpr(j2s.typeMap[type], la, ra, ShiftMode.UnsignedRight);
    replace2(ad, type);
  }

  private void increment(int index, int inc)
  {
    VariableDecl      lv  = getLocal(index, CodeAttribute.T_INT);
    LoadDeclValueExpr v   = new LoadDeclValueExpr(lv);
    LiteralExpr       lit = new LiteralExpr(j2s.getIntLiteral(inc));
    AdditionExpr      add = new AdditionExpr(j2s.intType, v, lit);
    makeAssign(lv, add);
  }

  private void cast21(int type)
  {
    Expr v = pop2();
    push(ConversionExpr.create(j2s.typeMap[type], v, CastMode.CAST), type);
  }

  private void cast22(int type)
  {
    Expr v = pop2();
    push2(ConversionExpr.create(j2s.typeMap[type], v, CastMode.CAST), type);
  }

  private void cast11(int type)
  {
    Expr v = pop();
    push(ConversionExpr.create(j2s.typeMap[type], v, CastMode.CAST), type);
  }

  private void cast12(int type)
  {
    Expr v = pop();
    push2(ConversionExpr.create(j2s.typeMap[type], v, CastMode.CAST), type);
  }

  private void defineBranchPoint(Chord sc, int branchadd, Object select)
  {
    bbPush(branchadd);
    BasicBlock s = getBasicBlock(branchadd);
    gotos.add(sc, s.getBegin(), select);
  }

  private void iftest(int cmp, Expr ra, Expr la, int address, int pc)
  {
    Expr op        = null;
    int  branchadd = pc + address;
    int  fallthru  = pc + 3;

    switch (cmp) {
    case 0: op = new EqualityExpr(BooleanType.type, la, ra);     break;
    case 1: op = new NotEqualExpr(BooleanType.type, la, ra);     break;
    case 2: op = new LessExpr(BooleanType.type, la, ra);         break;
    case 3: op = new GreaterEqualExpr(BooleanType.type, la, ra); break;
    case 4: op = new GreaterExpr(BooleanType.type, la, ra);      break;
    case 5: op = new LessEqualExpr(BooleanType.type, la, ra);    break;
    }

    LoadDeclValueExpr tv      = makeTempAssign(op);
    IfThenElseChord   ifchord = new IfThenElseChord(tv);

    recordNewChord(ifchord);
    exp.append(ifchord);
    defineBranchPoint(ifchord, branchadd, Boolean.TRUE);
    defineBranchPoint(ifchord, fallthru, Boolean.FALSE);
  }

  private int tableSwitch(int pc)
  {
    Expr        index = pop();
    int         opc   = pc;
    SwitchChord sc    = new SwitchChord(index);

    recordNewChord(sc);
    exp.append(sc);

    pc += (4 - (pc & 0x3));
    int defaultadd = opc + ca.getIndex4(pc);
    pc += 4;
    int low = ca.getIndex4(pc);
    pc += 4;
    int high = ca.getIndex4(pc);
    pc += 4;

    for (int i = low; i <= high; i++) {
      defineBranchPoint(sc, opc + ca.getIndex4(pc), Integer.toString(i));
      pc += 4;
    }
    defineBranchPoint(sc, defaultadd, "");
    return pc;
  }

  private int lookupSwitch(int pc)
  {
    Expr        key = pop();
    int         opc = pc;
    SwitchChord sc  = new SwitchChord(key);

    recordNewChord(sc);
    exp.append(sc);

    pc += (4 - (pc & 0x3));
    int defaultadd = opc + ca.getIndex4(pc);
    pc += 4;
    int n = ca.getIndex4(pc);
    pc += 4;

    for (int i = 0; i < n; i++) {
      defineBranchPoint(sc, ca.getIndex4(pc + 4), Integer.toString(ca.getIndex4(pc)));
      pc += 8;
    }
    defineBranchPoint(sc, defaultadd, "");
    return pc;
  }

  private void doReturn(int type, Expr value)
  {
    if (returnVar == null)
      throw new scale.common.InternalError("Return value from void method.");
    makeAssign(returnVar, value);
    gotos.add(exp.getEnd(), end, Boolean.TRUE);
  }

  private void doReturn()
  {
    gotos.add(exp.getEnd(), end, Boolean.TRUE);
  }

  private void compare(int mode, int type)
  {
    Expr ra = pop();
    Expr la = peek();
    Expr ad = new CompareExpr(la, ra, mode);
    replace(ad, type);
  }

  private void compare2(int mode, int type)
  {
    Expr ra = pop2();
    Expr la = pop2();
    Expr ad = new CompareExpr(la, ra, mode);
    push(ad, type);
  }

  private VariableDecl findStatic(int index)
  {
    FieldRefCPInfo    info  = (FieldRefCPInfo) cf.getCP(index);
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(info.getClassIndex());
    NameAndTypeCPInfo ninfo = (NameAndTypeCPInfo) cf.getCP(info.getNameAndTypeIndex());
    String            fname = cf.getName(ninfo.getNameIndex());
    ClassStuff        cs    = j2s.getClass(cf.getName(cinfo.getNameIndex()));
    String            cname = cs.name;
    VariableDecl      vd    = j2s.getGlobalVar(cs, fname);
    return vd;
  }

  private boolean isTwo(Type type)
  {
    NumericType ft = type.getCoreType().returnNumericType();
    return (ft != null) && (ft.bitSize() > 32);
  }

  private void getStatic(int index)
  {
    VariableDecl d    = findStatic(index);
    Expr         exp  = new LoadDeclValueExpr(d);
    int          type = j2s.getTypeSpecifier(d.getType());

    push(exp, type);
    if (isTwo(d.getType()))
      push(exp, type);
  }

  private void putStatic(int index)
  {
    VariableDecl d     = findStatic(index);
    Expr         value = pop();

    if (isTwo(d.getType()))
      pop();

    Expr exp = new LoadDeclAddressExpr(d);

    makeAssign(exp, value);
  }

  private FieldDecl findField(ClassCPInfo cinfo, String fname)
  {
    ClassStuff cs = j2s.getClass(cf.getName(cinfo.getNameIndex()));
    return j2s.findField(cs, fname);
  }

  private void getField(int index)
  {
    FieldRefCPInfo    info  = (FieldRefCPInfo) cf.getCP(index);
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(info.getClassIndex());
    NameAndTypeCPInfo ninfo = (NameAndTypeCPInfo) cf.getCP(info.getNameAndTypeIndex());
    String            fname = cf.getName(ninfo.getNameIndex());
    FieldDecl         d     = findField(cinfo, fname);
    Type              ft    = d.getType();
    Expr              addr  = pop();

    genNullPointerExceptionTest(addr.copy());

    Expr              obj   = typeify(cinfo, addr);
    Expr              op    = makeTempAssign(new LoadFieldValueExpr(obj, d));
    Expr              val   = makeTempAssign(LoadValueIndirectExpr.create(op));
    int               type  = j2s.getTypeSpecifier(ft);

    push(val, type);

    if (isTwo(ft))
      push(val, type);
  }

  private void putField(int index)
  {
    FieldRefCPInfo    info  = (FieldRefCPInfo) cf.getCP(index);
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(info.getClassIndex());
    NameAndTypeCPInfo ninfo = (NameAndTypeCPInfo) cf.getCP(info.getNameAndTypeIndex());
    String            fname = cf.getName(ninfo.getNameIndex());
    FieldDecl         d     = findField(cinfo, fname);
    Type              ft    = d.getType();
    Expr              value = pop();

    if (isTwo(ft))
      pop();

    Expr            addr    = pop();

    genNullPointerExceptionTest(addr.copy());

    Expr obj = typeify(cinfo, addr);
    Expr op  = new LoadFieldAddressExpr(obj, d);

    makeAssign(op, value);
  }

  private void doNew(int index)
  {
    ClassCPInfo     cinfo = (ClassCPInfo) cf.getCP(index);
    ClassStuff      cs    = j2s.getClass(cf.getName(cinfo.getNameIndex()));
    Expr            size  = new LiteralExpr(new SizeofLiteral(j2s.intType, cs.td.getType()));
    Expr            alloc = makeTempAssign(new AllocateExpr(j2s.voidp, size, true));
    Expr            thisp = makeTempAssign(ConversionExpr.create(cs.type, alloc, CastMode.CAST));
    FieldDecl       d     = j2s.findField(cs, "vtable");
    Expr            lhs   = makeTempAssign(new LoadFieldValueExpr(thisp, d));
    VariableDecl    vd    = j2s.getVTableDecl(cs);
    Expr            init  = new LoadDeclAddressExpr(vd);
    Expr            rhs   = makeTempAssign(ConversionExpr.create(j2s.voidp, init, CastMode.CAST));
    ExprChord       ec    = new ExprChord(lhs, rhs);

    recordNewChord(ec);
    exp.append(ec);
    push(thisp.copy(), CodeAttribute.T_ADDRESS);
    j2s.addTopGlobal(cs.td);
    j2s.addTopGlobal(vd);
  }

  private void doNewarray(int type)
  {
    Expr num  = peek();

    genNegativeSizeExceptionTest(num.copy());

    Expr size = new LiteralExpr(new SizeofLiteral(j2s.intType, j2s.typeMap[type]));
    Expr mult = makeTempAssign(new MultiplicationExpr(j2s.intType, num, size));
    Expr add  = makeTempAssign(new AdditionExpr(j2s.intType, mult, new LiteralExpr(new SizeofLiteral(j2s.intType, j2s.intType))));
    Expr allo = new AllocateExpr(j2s.voidp, add, false);

    replace(allo, CodeAttribute.T_ADDRESS);
    makeAssign(getArrayLength(peek()), num.copy());
  }

  private void doANewarray(int index)
  {
    Expr        num   = peek();

    genNegativeSizeExceptionTest(num.copy());

    ClassCPInfo cinfo = (ClassCPInfo) cf.getCP(index);
    ClassStuff  cs    = j2s.getClass(cf.getName(cinfo.getNameIndex()));
    TypeDecl    td    = cs.td;
    Type        type  = PointerType.create(td.getType());
    Expr        size  = new LiteralExpr(new SizeofLiteral(j2s.intType, type));
    Expr        mult  = makeTempAssign(new MultiplicationExpr(j2s.intType, num, size));
    Expr        add   = makeTempAssign(new AdditionExpr(j2s.intType, mult, new LiteralExpr(new SizeofLiteral(j2s.intType, j2s.intType))));
    Expr        allo  = new AllocateExpr(j2s.voidp, add, false);

    replace(allo, CodeAttribute.T_ADDRESS);
    makeAssign(getArrayLength(peek()), num.copy());
    j2s.addTopGlobal(td);
  }

  private void multiANewarray(int index, int dimensions)
  {
    ClassCPInfo cinfo = (ClassCPInfo) cf.getCP(index);
    ClassStuff  cs    = j2s.getClass(cf.getName(cinfo.getNameIndex()));
    TypeDecl    td    = cs.td;
    Type        type  = PointerType.create(td.getType());
    Expr        size  = new LiteralExpr(new SizeofLiteral(j2s.intType, type));
    Expr        num   = pop();

    genNegativeSizeExceptionTest(num.copy());

    for (int i = 0; i < dimensions - 1; i++) {
      Expr ind = pop();
      genNegativeSizeExceptionTest(ind.copy());
      num   = makeTempAssign(new MultiplicationExpr(j2s.intType, num, ind));
    }

    Expr        mult  = makeTempAssign(new MultiplicationExpr(j2s.intType, num, size));
    Expr        add   = makeTempAssign(new AdditionExpr(j2s.intType, mult, new LiteralExpr(new SizeofLiteral(j2s.intType, j2s.intType))));
    Expr        allo  = makeTempAssign(new AllocateExpr(j2s.voidp, add, false));

    replace(allo, CodeAttribute.T_ADDRESS);
    makeAssign(getArrayLength(peek()), num.copy());
    j2s.addTopGlobal(td);
  }

  private void arrayLength()
  {
    Expr array = peek();

    genNullPointerExceptionTest(array.copy());

    Expr op = getArrayLength(array);
    replace(LoadValueIndirectExpr.create(op), CodeAttribute.T_INT);
  }

  private Expr typeify(ClassCPInfo cinfo, Expr op)
  {
    if (op.getCoreType() == j2s.voidp) {
      ClassStuff cs = j2s.getClass(cf.getName(cinfo.getNameIndex()));
      op = makeTempAssign(ConversionExpr.create(cs.type, op, CastMode.CAST));
      j2s.addTopGlobal(cs.td);
    }
    return op;
  }

  private Expr vtableRef(ClassCPInfo cinfo, Expr thisp)
  {
    thisp = typeify(cinfo, thisp);

    FieldDecl d     = findField(cinfo, "vtable");
    Expr      op    = makeTempAssign(new LoadFieldValueExpr(thisp, d));
    return  makeTempAssign(LoadValueIndirectExpr.create(op));
  }

  private Expr vtableMethodsRef(ClassCPInfo cinfo, Expr thisp)
  {
    Expr      vtable = vtableRef(cinfo, thisp);
    FieldDecl d      = j2s.getVTableField("methods");
    return makeTempAssign(new LoadFieldValueExpr(vtable, d));
  }

  private Vector<Expr> getArgs(Vector<Type> parameters, boolean isThis)
  {
    int          inc  = (isThis ? 1 : 0);
    int          n    = parameters.size() - 1;
    Vector<Expr> args = new Vector<Expr>(n + inc);

    for (int i = 0; i < n + inc; i++) args.addElement(null);

    for (int i = n - 1; i >= 0; i--) {
      Type ft  = parameters.elementAt(i);
      Expr arg = pop();
      if (isTwo(ft))
        pop();
      args.setElementAt(arg, i + inc);
    }
    if (isThis)
      args.setElementAt(pop(), 0);
    return args;
  }

  private Type getReturnType(Vector<Type> parameters)
  {
    int  n  = parameters.size() - 1;
    Type rt = parameters.elementAt(n);

//      if (rt.isVoidType())
//        rt = null;

    return rt;
  }

  private void invokeInterface(int index, int nargs)
  {
    InterfaceMethodRefCPInfo info  = (InterfaceMethodRefCPInfo) cf.getCP(index);
    ClassCPInfo              cinfo = (ClassCPInfo) cf.getCP(info.getClassIndex());
    NameAndTypeCPInfo        ninfo = (NameAndTypeCPInfo) cf.getCP(info.getNameAndTypeIndex());
    String                   cname = cf.getName(cinfo.getNameIndex());
    ClassStuff               cs    = j2s.getClass(cname);
    String                   fname = cf.getName(ninfo.getNameIndex());
    String                   desc  = cf.getString(ninfo.getDescriptorIndex());
    Vector<Type>             parm  = j2s.getClefTypes(desc);
    Vector<Expr>             args  = getArgs(parm, true);
    Type                     rt    = getReturnType(parm);
    int                      mi    = cs.findVirtualMethod(fname, desc);
    Expr                     thisp = args.elementAt(0);
    Vector<Expr>             iargs = new Vector<Expr>(3);

    iargs.addElement(new LiteralExpr(j2s.getIntLiteral(mi)));
    iargs.addElement(new LiteralExpr(j2s.getIntLiteral(pd.hashCode())));
    iargs.addElement(vtableRef(cinfo, thisp.copy()));

    genNullPointerExceptionTest(thisp.copy());

    Expr                     meth  = makeTempAssign(new CallFunctionExpr(j2s.voidp, new LoadDeclValueExpr(j2s.findIntMethodProc), iargs));
    PointerType              sig   = PointerType.create(j2s.createMethodType(thisp.getType(), desc, false));
    Expr                     proc  = makeTempAssign(ConversionExpr.create(sig, meth, CastMode.CAST));
    Expr                     proca = makeTempAssign(LoadValueIndirectExpr.create(proc));
    Expr                     call  = new CallMethodExpr(rt, proca, args);

    if (rt == VoidType.type) {
      ExprChord ec = new ExprChord(call);
      recordNewChord(ec);
      exp.append(ec);
    } else {
      int ts = j2s.getTypeSpecifier(rt);
      if (CodeAttribute.isTwo[ts])
        push2(call, ts);
      else
        push(call, ts);
    }

    genExceptionTest(currentPc);

    pd.addCandidate(rt);
  }

  private void invokeVirtual(int index)
  {
    MethodRefCPInfo   info  = (MethodRefCPInfo) cf.getCP(index);
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(info.getClassIndex());
    NameAndTypeCPInfo ninfo = (NameAndTypeCPInfo) cf.getCP(info.getNameAndTypeIndex());
    String            cname = cf.getName(cinfo.getNameIndex());
    ClassStuff        cs    = j2s.getClass(cname);
    String            fname = cf.getName(ninfo.getNameIndex());
    String            desc  = cf.getString(ninfo.getDescriptorIndex());
    Vector<Type>      parm  = j2s.getClefTypes(desc);
    Vector<Expr>      args  = getArgs(parm, true);
    Type              rt    = getReturnType(parm);
    int               mi    = cs.findVirtualMethod(fname, desc);
    Expr              thisp = args.elementAt(0);

    genNullPointerExceptionTest(thisp.copy());

    Type              mt    = j2s.createMethodType(rt, desc, false);
    PointerType       et    = PointerType.create(mt);
    Expr              mfld  = vtableMethodsRef(cinfo, thisp.copy());
    Expr              indx  = new LiteralExpr(j2s.getIntLiteral(mi));
    Expr              meth  = createSubscriptExpr(indx, LoadValueIndirectExpr.create(mfld), PointerType.create(et));
    Expr              proc  = makeTempAssign(ConversionExpr.create(et, LoadValueIndirectExpr.create(meth), CastMode.CAST));
    Expr              call  = new CallMethodExpr(rt, proc, args);

    if (rt == VoidType.type) {
      ExprChord ec = new ExprChord(call);
      recordNewChord(ec);
      exp.append(ec);
    } else {
      int ts = j2s.getTypeSpecifier(rt);
      if (CodeAttribute.isTwo[ts])
        push2(call, ts);
      else
        push(call, ts);
    }

    pd.addCandidate(rt);

    genExceptionTest(currentPc);
  }

  private void invokeSpecial(int index)
  {
    MethodRefCPInfo   info  = (MethodRefCPInfo) cf.getCP(index);
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(info.getClassIndex());
    NameAndTypeCPInfo ninfo = (NameAndTypeCPInfo) cf.getCP(info.getNameAndTypeIndex());
    String            cname = cf.getName(cinfo.getNameIndex());
    ClassStuff        cs    = j2s.getClass(cname);
    String            fname = cf.getName(ninfo.getNameIndex());
    String            desc  = cf.getString(ninfo.getDescriptorIndex());
    Vector<Type>      parm  = j2s.getClefTypes(desc);
    Vector<Expr>      args  = getArgs(parm, true);
    Expr              thisp = args.elementAt(0);

    genNullPointerExceptionTest(thisp.copy());

    Type              rt    = getReturnType(parm);

    ClassFile         csf   = cs.cf;
    String            m     = j2s.genMethodName(csf.getName(((ClassCPInfo) csf.getCP(csf.getThisClass())).getNameIndex()), fname, desc);
    ProcedureDecl     pd    = j2s.getProcedureDecl(cs.type, m, desc, false);
    Expr              call  = new CallMethodExpr(rt, new LoadDeclAddressExpr(pd), args);

    if (rt == VoidType.type) {
      ExprChord ec = new ExprChord(call);
      recordNewChord(ec);
      exp.append(ec);
    } else {
      int ts = j2s.getTypeSpecifier(rt);
      if (CodeAttribute.isTwo[ts])
        push2(call, ts);
      else
        push(call, ts);
    }

    cg.addCallee(pd, pd);

    genExceptionTest(currentPc);
  }

  private void invokeStatic(int index)
  {
    MethodRefCPInfo   info  = (MethodRefCPInfo) cf.getCP(index);
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(info.getClassIndex());
    NameAndTypeCPInfo ninfo = (NameAndTypeCPInfo) cf.getCP(info.getNameAndTypeIndex());
    String            cname = cf.getName(cinfo.getNameIndex());
    ClassStuff        cs    = j2s.getClass(cname);
    String            fname = cf.getName(ninfo.getNameIndex());
    String            desc  = cf.getString(ninfo.getDescriptorIndex());
    Vector<Type>      parm  = j2s.getClefTypes(desc);
    Vector<Expr>      args  = getArgs(parm, false);
    Type              rt    = getReturnType(parm);

    ClassFile         csf   = cs.cf;
    String            m     = j2s.genMethodName(csf.getName(((ClassCPInfo) csf.getCP(csf.getThisClass())).getNameIndex()), fname, desc);
    ProcedureDecl     pd    = j2s.getProcedureDecl(cs.type, m, desc, true);
    Expr              call  = new CallFunctionExpr(rt, new LoadDeclValueExpr(pd), args);

    if (rt == VoidType.type) {
      ExprChord ec = new ExprChord(call);
      recordNewChord(ec);
      exp.append(ec);
    } else {
      int ts = j2s.getTypeSpecifier(rt);
      if (CodeAttribute.isTwo[ts])
        push2(call, ts);
      else
        push(call, ts);
    }

    cg.addCallee(pd, pd);

    genExceptionTest(currentPc);
  }

  private void athrow()
  {
    Expr obj = pop();

    genNullPointerExceptionTest(obj.copy());

    if (exceptionPcVar == null) {
      exceptionPcVar = new VariableDecl("__javaPc", j2s.intType);
      tempVars.addElement(exceptionPcVar);
    }

    makeAssign(new LoadDeclAddressExpr(exceptionPcVar), new LiteralExpr(j2s.getIntLiteral(currentPc)));

    LoadDeclAddressExpr lhs = new LoadDeclAddressExpr(j2s.globalExceptionVariable);
    ExprChord           th  = new ExprChord(lhs, obj);

    recordNewChord(th);

    gotos.add(th, getExceptionDispatcher(currentPc), Boolean.TRUE);

    exp.append(th);
  }

  private void checkCast(int index)
  {
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(index);
    String            cname = cf.getName(cinfo.getNameIndex());
    ClassStuff        cs    = j2s.getClass(cname);
    Chord             ec    = makeException("java/lang/ClassCastException");
    Expr              arg   = peek();
    Expr              test1 = new EqualityExpr(BooleanType.type, arg, new LiteralExpr(j2s.nil));
    Chord             ok    = new NullChord();
    Chord             te1   = new NullChord();
    Chord             beg   = exp.getBegin();
    IfThenElseChord   ifc1  = new IfThenElseChord(test1, ok, te1); // if null

    recordNewChord(ok);
    recordNewChord(te1);
    recordNewChord(ifc1);

    exp.append(ifc1);
    exp = new ExprTuple(te1, te1);

    VariableDecl    vd    = j2s.getVTableDecl(cs);
    Expr            vadd  = new LoadDeclAddressExpr(vd);
    Expr            expr  = vtableRef(cinfo, arg.copy());
    Expr            test2 = new EqualityExpr(BooleanType.type, vadd, expr);
    Chord           te2   = new NullChord();
    IfThenElseChord ifc2  = new IfThenElseChord(test2, ok, te2); // if identical

    recordNewChord(te2);
    recordNewChord(ifc2);

    exp.append(ifc2);
    exp = new ExprTuple(te2, te2);

    Vector<Expr> args = new Vector<Expr>(3);

    args.addElement(expr.copy());
    args.addElement(vadd.copy());

    Expr            call  = new CallFunctionExpr(j2s.intType, new LoadDeclValueExpr(j2s.instanceOfProc), args);
    Expr            inst  = makeTempAssign(call);
    Expr            test3 = new EqualityExpr(BooleanType.type, inst, new LiteralExpr(j2s.int0));
    IfThenElseChord ifc3  = new IfThenElseChord(test3, ec, ok); // if instanceof

    recordNewChord(ifc3);

    exp = new ExprTuple(beg, ok);
  }

  private void instanceOf(int index)
  {
    ClassCPInfo       cinfo = (ClassCPInfo) cf.getCP(index);
    String            cname = cf.getName(cinfo.getNameIndex());
    ClassStuff        cs    = j2s.getClass(cname);

    Expr              arg   = peek();
    LoadDeclValueExpr rv    = makeTempAssign(new LiteralExpr(j2s.int0)); // set result false
    Expr              test1 = new EqualityExpr(BooleanType.type, arg, new LiteralExpr(j2s.nil));
    Chord             exit  = new NullChord();
    Chord             te    = new NullChord();
    Chord             beg   = exp.getBegin();
    IfThenElseChord   ifc1  = new IfThenElseChord(test1, exit, te); // if null

    recordNewChord(exit);
    recordNewChord(te);
    recordNewChord(ifc1);

    exp.append(ifc1);
    exp = new ExprTuple(te, te);

    Expr              expr  = vtableRef(cinfo, arg.copy());
    VariableDecl      vd    = j2s.getVTableDecl(cs);
    Expr              vadd  = new LoadDeclAddressExpr(vd);
    LoadDeclValueExpr ne    = makeTempAssign(new EqualityExpr(BooleanType.type, expr, vadd));
    Chord             sett  = new NullChord();
    Chord             check = new NullChord();
    IfThenElseChord   ifc2  = new IfThenElseChord(ne, sett, check); // if identical

    recordNewChord(sett);
    recordNewChord(check);
    recordNewChord(ifc2);
    exp.append(ifc2);

    exp = new ExprTuple(sett, sett);

    makeAssign((VariableDecl) rv.getDecl(), new LiteralExpr(j2s.int1));

    Chord ve = exp.getEnd();
    ve.linkTo(exit);

    exp = new ExprTuple(check, check);

    Vector<Expr> args = new Vector<Expr>(3);

    args.addElement(expr.copy());
    args.addElement(vadd.copy());

    Expr call = new CallFunctionExpr(j2s.intType, new LoadDeclValueExpr(j2s.instanceOfProc), args);

    makeAssign(rv.copy(), call);

    Chord ce = exp.getEnd();
    ce.linkTo(exit);

    replace(rv.copy(), CodeAttribute.T_INT);
    exp = new ExprTuple(beg, exit);
    j2s.addTopGlobal(vd);
  }

  private void monitorEnter()
  {
    Expr obj = pop();

    genNullPointerExceptionTest(obj.copy());

    /***** Do nothing for now. *****/
  }

  private void monitorExit()
  {
    Expr obj = pop();

    genNullPointerExceptionTest(obj.copy());
    genIllegalMonitorStateExceptionTest(obj.copy());

    /***** Do nothing for now. *****/
  }

  private void bbPush(int index)
  {
    if (bbIndex >= bbStack.length) {
      int[] nbb = new int[bbStack.length + 128];
      System.arraycopy(bbStack, 0, nbb, 0, bbStack.length);
      bbStack = nbb;
    }
    bbStack[bbIndex++] = index;
  }

  /**
   * Convert the Java byte codes into Scribble as part of a RoutineDecl.
   * @see scale.clef.decl.RoutineDecl
   */
  public void generate(MethodInfo method)
  {
    String          name       = pd.getName();
    AttributeInfo[] attributes = method.getAttributes();

    // Find the CodeAttribute for this method.

    ca = null;

    for (int i = 0; i < attributes.length; i++) {
      AttributeInfo ai = attributes[i];
      if (ai instanceof CodeAttribute) {
        ca = (CodeAttribute) ai;
        break;
      }
    }
    if (ca == null)
      throw new scale.common.InternalError("No code for " + name);

    if (trace)
      System.out.println("  processing " + name);

    // Get any line number information

      attributes = ca.getAttributes();
      for (int i = 0; i < attributes.length; i++) {
        AttributeInfo ai = attributes[i];
        if (ai instanceof LineNumberTableAttribute) {
          CreatorSource     creator = new CreatorSource("Java2Scribble");
          LineNumberEntry[] lne     = ((LineNumberTableAttribute) ai).getLineNumberTable();
          int               len     = lne.length;

          slaPc = new int[len];
          sla   = new int[len];

          for (int j = 0; j < len; j++) {
            slaPc[j] = lne[j].startPc;
            sla[j] = lne[j].lineNumber;
          }
          break;
        }
      }

    // Establish the local variables.

    localVars        = new VariableDecl[ca.getMaxLocals() + 1];
    ProcedureType pt = (ProcedureType) pd.getType();
    int           l  = pt.numFormals();
    for (int i = 0; i < l; i++) {
      FormalDecl fd = pt.getFormal(i);
      defLocal(i, fd);
      i++;
    }

    // Define the return variable and the End of the CFG.

    Vector<Type> desc = j2s.getClefTypes(cf.getString(method.getDescriptorIndex()));
    Type rt = desc.lastElement();

    if (rt == VoidType.type) {
      returnVar = null;
      end = new ReturnChord();
    } else {
      returnVar = new VariableDecl("_r_" + name, rt);
      end = new ReturnChord(new LoadDeclValueExpr(returnVar));
      tempVars.addElement(returnVar);
    }

    recordNewChord(end);

    gotos.defineLabel(end, end);

    // Process the catch code.

    exceptionEntries = ca.getExceptionTable();
    exceptionPoints  = new Chord[exceptionEntries.length];

    BitSet done = new BitSet(ca.getCodelength());

    for (int j = 0; j < exceptionEntries.length; j++) {

      // At the beginning of each catch handler generate
      // _svA0 = globalExceptionVariable;
      // globalExceptionVariable = (void *) 0;

      ExceptionEntry      ex   = exceptionEntries[j];
      LoadDeclValueExpr   rhs  = new LoadDeclValueExpr(j2s.globalExceptionVariable);
      LoadDeclAddressExpr lhs  = new LoadDeclAddressExpr(defineStackVar(0, CodeAttribute.T_ADDRESS));
      ExprChord           bg   = new ExprChord(lhs, rhs);
      LiteralExpr         rhs2 = new LiteralExpr(j2s.nil);
      LoadDeclAddressExpr lhs2 = new LoadDeclAddressExpr(j2s.globalExceptionVariable);
      ExprChord           clr  = new ExprChord(lhs2, rhs2); // Zero out globalExceptionVariable
      BasicBlock          s    = new BasicBlock(1, javaStack, new ExprTuple(bg, clr), localVars);

      recordNewChord(bg);
      recordNewChord(clr);

      bg.setTarget(clr);
      exceptionPoints[j] = bg;
      bbChords.put(ex.handlerPc, s);
      gotos.defineLabel(bg, bg);
    }

    for (int j = 0; j < exceptionEntries.length; j++) {
      ExceptionEntry ex = exceptionEntries[j];
      processCode(ex.handlerPc, done);
    }

    // Process the main body of the method.

    processCode(0, done);

    // Finish up the method.

    gotos.fixupGotos(); // Connect the CFG.
    Chord.removeDeadCode(chords); // Remove the place holders.

//      Stack<Object> zl = new Stack<Object>();
//      Chord.nextVisit();
//      zl.push(begin);
//      while (!zl.empty()) {
//        Chord x = (Chord) zl.pop();
//        System.out.println("** " + x);
//        x.addChildren(zl);
//        Chord[] indata = x.getInCfgEdgeArray();
//        Chord[] outdata = x.getOutCfgEdgeArray();
//        for (int ci = 0; ci < indata.length;ci++)
//          System.out.println("    IN: " + indata[ci]);
//        for (int ci = 0; ci < outdata.length;ci++)
//          System.out.println("    OUT: " + outdata[ci]);
//      }

    scribble.instantiate(begin, end, tempVars, false);
    pd.clearAST();
  }

  /**
   * Process a Java byte code sequence.
   * This method processes a complete CFG of Java byte codes.
   * @param start is the beginning Java byte code PC for the CFG
   * @param done is used to determine if an edge of the CFG has been
   * traversed already
   */
  private void processCode(int start, BitSet done)
  {
    int[]   jsrStack = new int[16];
    int     jsrIndex = 0;
    boolean jsrFlag  = false;

    bbPush(start);

    while (bbIndex > 0) {
      bbIndex--;
      int pc  = bbStack[bbIndex];
      int opc = -1;

      boolean terminate = false;

      while (!terminate) {
        if (ca.isBasicBlock(pc)) { // This PC is at the beginning of a basic block in the CFG.
          if (!jsrFlag) { // jsr routines are put inline.
            BasicBlock s = getBasicBlock(pc);

            Chord nc = s.getBegin();

            if (opc == pc) { // Link this basic block to it's immediate predecessor.
              Chord last = exp.getEnd();
              if (last.isSequential())
                gotos.add(last, nc, Boolean.TRUE);
            }

            if (trace)
              System.out.println("**** New basic block (" + pc + ") " + nc);

            if (done.get(pc))
              break; // This basic block has already been processed.

            stackIndex = s.index; // Get the stack , etc for the start of this basic block.
            javaStack  = s.stack;
            localVars  = s.localVars;

            exp = s.exp;
          }
          jsrFlag = false;
        }

        int bc = ca.getOpcode(pc);
        currentPc = pc;
        done.set(pc);

        if (trace) {
          System.out.print(" ");
          System.out.print(pc);
          System.out.print(": (");
          System.out.print(stackIndex);
          System.out.print(") ");
          System.out.println(CodeAttribute.opcodeName[bc]);
        }

        pc++;
        switch (bc) {
        case CodeAttribute.NOP:                                break;
        case CodeAttribute.ACONST_NULL:     pushConst(j2s.nil, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.ICONST_M1:       pushConst(j2s.intm1, CodeAttribute.T_INT); break;
        case CodeAttribute.ICONST_0:        pushConst(j2s.int0, CodeAttribute.T_INT); break;
        case CodeAttribute.ICONST_1:        pushConst(j2s.int1, CodeAttribute.T_INT); break;
        case CodeAttribute.ICONST_2:        pushConst(j2s.int2, CodeAttribute.T_INT); break;
        case CodeAttribute.ICONST_3:        pushConst(j2s.int3, CodeAttribute.T_INT); break;
        case CodeAttribute.ICONST_4:        pushConst(j2s.int4, CodeAttribute.T_INT); break;
        case CodeAttribute.ICONST_5:        pushConst(j2s.int5, CodeAttribute.T_INT); break;
        case CodeAttribute.LCONST_0:        pushConst2(j2s.int0, CodeAttribute.T_LONG); break;
        case CodeAttribute.LCONST_1:        pushConst2(j2s.int1, CodeAttribute.T_LONG); break;
        case CodeAttribute.FCONST_0:        pushConst(j2s.float0, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FCONST_1:        pushConst(j2s.float1, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FCONST_2:        pushConst(j2s.float2, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DCONST_0:        pushConst2(j2s.float0, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DCONST_1:        pushConst2(j2s.float1, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.BIPUSH:          pushConst(j2s.getIntLiteral(ca.getByte(pc)), CodeAttribute.T_BYTE); pc++; break;
        case CodeAttribute.SIPUSH:          pushConst(j2s.getIntLiteral(ca.getByte2(pc)), CodeAttribute.T_SHORT); pc += 2; break;
        case CodeAttribute.LDC:             loadConst(ca.getIndex(pc));   pc++;    break;
        case CodeAttribute.LDC_W:           loadConst(ca.getIndex2(pc));  pc += 2; break;
        case CodeAttribute.LDC2_W:          loadConst2(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.ILOAD:           load(ca.getIndex(pc), CodeAttribute.T_INT);    pc++; break;
        case CodeAttribute.LLOAD:           load2(ca.getIndex(pc), CodeAttribute.T_LONG);   pc++; break;
        case CodeAttribute.FLOAD:           load(ca.getIndex(pc), CodeAttribute.T_FLOAT);  pc++; break;
        case CodeAttribute.DLOAD:           load2(ca.getIndex(pc), CodeAttribute.T_DOUBLE); pc++; break;
        case CodeAttribute.ALOAD:           load(ca.getIndex(pc)); pc++; break;
        case CodeAttribute.ILOAD_0:         load(0, CodeAttribute.T_INT); break;
        case CodeAttribute.ILOAD_1:         load(1, CodeAttribute.T_INT); break;
        case CodeAttribute.ILOAD_2:         load(2, CodeAttribute.T_INT); break;
        case CodeAttribute.ILOAD_3:         load(3, CodeAttribute.T_INT); break;
        case CodeAttribute.LLOAD_0:         load2(0, CodeAttribute.T_LONG); break;
        case CodeAttribute.LLOAD_1:         load2(1, CodeAttribute.T_LONG); break;
        case CodeAttribute.LLOAD_2:         load2(2, CodeAttribute.T_LONG); break;
        case CodeAttribute.LLOAD_3:         load2(3, CodeAttribute.T_LONG); break;
        case CodeAttribute.FLOAD_0:         load(0, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FLOAD_1:         load(1, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FLOAD_2:         load(2, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FLOAD_3:         load(3, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DLOAD_0:         load2(0, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DLOAD_1:         load2(1, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DLOAD_2:         load2(2, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DLOAD_3:         load2(3, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.ALOAD_0:         load(0, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.ALOAD_1:         load(1, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.ALOAD_2:         load(2, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.ALOAD_3:         load(3, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.IALOAD:          aload(CodeAttribute.T_INT); break;
        case CodeAttribute.LALOAD:          aload2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FALOAD:          aload(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DALOAD:          aload2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.AALOAD:          aload(CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.BALOAD:          aload(CodeAttribute.T_BYTE); break;
        case CodeAttribute.CALOAD:          aload(CodeAttribute.T_CHAR); break;
        case CodeAttribute.SALOAD:          aload(CodeAttribute.T_SHORT); break;
        case CodeAttribute.ISTORE:          store(ca.getIndex(pc), CodeAttribute.T_INT); pc++; break;
        case CodeAttribute.LSTORE:          store2(ca.getIndex(pc), CodeAttribute.T_LONG); pc++; break;
        case CodeAttribute.FSTORE:          store(ca.getIndex(pc), CodeAttribute.T_FLOAT); pc++; break;
        case CodeAttribute.DSTORE:          store2(ca.getIndex(pc), CodeAttribute.T_DOUBLE); pc++; break;
        case CodeAttribute.ASTORE:          store(ca.getIndex(pc)); pc++; break;
        case CodeAttribute.ISTORE_0:        store(0, CodeAttribute.T_INT); break;
        case CodeAttribute.ISTORE_1:        store(1, CodeAttribute.T_INT); break;
        case CodeAttribute.ISTORE_2:        store(2, CodeAttribute.T_INT); break;
        case CodeAttribute.ISTORE_3:        store(3, CodeAttribute.T_INT); break;
        case CodeAttribute.LSTORE_0:        store2(0, CodeAttribute.T_LONG); break;
        case CodeAttribute.LSTORE_1:        store2(1, CodeAttribute.T_LONG); break;
        case CodeAttribute.LSTORE_2:        store2(2, CodeAttribute.T_LONG); break;
        case CodeAttribute.LSTORE_3:        store2(3, CodeAttribute.T_LONG); break;
        case CodeAttribute.FSTORE_0:        store(0, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FSTORE_1:        store(1, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FSTORE_2:        store(2, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.FSTORE_3:        store(3, CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DSTORE_0:        store2(0, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DSTORE_1:        store2(1, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DSTORE_2:        store2(2, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DSTORE_3:        store2(3, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.ASTORE_0:        store(0, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.ASTORE_1:        store(1, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.ASTORE_2:        store(2, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.ASTORE_3:        store(3, CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.IASTORE:         astore(CodeAttribute.T_INT); break;
        case CodeAttribute.LASTORE:         astore2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FASTORE:         astore(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DASTORE:         astore2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.AASTORE:         astore(CodeAttribute.T_ADDRESS); break;
        case CodeAttribute.BASTORE:         astore(CodeAttribute.T_BYTE); break;
        case CodeAttribute.CASTORE:         astore(CodeAttribute.T_CHAR); break;
        case CodeAttribute.SASTORE:         astore(CodeAttribute.T_SHORT); break;
        case CodeAttribute.POP:             pop();     break;
        case CodeAttribute.POP2:            pop2();    break;
        case CodeAttribute.DUP:             dup();     break;
        case CodeAttribute.DUP_X1:          dup_x1();  break;
        case CodeAttribute.DUP_X2:          dup_x2();  break;
        case CodeAttribute.DUP2:            dup2();    break;
        case CodeAttribute.DUP2_X1:         dup2_x1(); break;
        case CodeAttribute.DUP2_X2:         dup2_x2(); break;
        case CodeAttribute.SWAP:            swap();    break;
        case CodeAttribute.IADD:            add(CodeAttribute.T_INT); break;
        case CodeAttribute.LADD:            add2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FADD:            add(CodeAttribute.T_INT); break;
        case CodeAttribute.DADD:            add2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.ISUB:            sub(CodeAttribute.T_INT); break;
        case CodeAttribute.LSUB:            sub2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FSUB:            sub(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DSUB:            sub2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.IMUL:            mult(CodeAttribute.T_INT); break;
        case CodeAttribute.LMUL:            mult2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FMUL:            mult(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DMUL:            mult2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.IDIV:            divide(CodeAttribute.T_INT); break;
        case CodeAttribute.LDIV:            divide2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FDIV:            divide(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DDIV:            divide2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.IREM:            remainder(CodeAttribute.T_INT); break;
        case CodeAttribute.LREM:            remainder2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FREM:            remainder(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DREM:            remainder2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.INEG:            negate(CodeAttribute.T_INT); break;
        case CodeAttribute.LNEG:            negate2(CodeAttribute.T_LONG); break;
        case CodeAttribute.FNEG:            negate(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.DNEG:            negate2(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.ISHL:            lshift(CodeAttribute.T_INT); break;
        case CodeAttribute.LSHL:            lshift2(CodeAttribute.T_LONG); break;
        case CodeAttribute.ISHR:            rshift(CodeAttribute.T_INT); break;
        case CodeAttribute.LSHR:            rshift2(CodeAttribute.T_INT); break;
        case CodeAttribute.IUSHR:           urshift(CodeAttribute.T_INT); break;
        case CodeAttribute.LUSHR:           urshift2(CodeAttribute.T_INT); break;
        case CodeAttribute.IAND:            and(CodeAttribute.T_INT); break;
        case CodeAttribute.LAND:            and2(CodeAttribute.T_LONG); break;
        case CodeAttribute.IOR:             or(CodeAttribute.T_INT); break;
        case CodeAttribute.LOR:             or2(CodeAttribute.T_LONG); break;
        case CodeAttribute.IXOR:            xor(CodeAttribute.T_INT); break;
        case CodeAttribute.LXOR:            xor2(CodeAttribute.T_LONG); break;
        case CodeAttribute.IINC:            increment(ca.getIndex(pc), ca.getByte(pc + 1)); pc += 2; break;
        case CodeAttribute.I2L:             cast12(CodeAttribute.T_LONG); break;
        case CodeAttribute.I2F:             cast11(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.I2D:             cast12(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.L2I:             cast21(CodeAttribute.T_INT); break;
        case CodeAttribute.L2F:             cast21(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.L2D:             cast22(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.F2I:             cast11(CodeAttribute.T_INT); break;
        case CodeAttribute.F2L:             cast12(CodeAttribute.T_LONG); break;
        case CodeAttribute.F2D:             cast12(CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.D2I:             cast21(CodeAttribute.T_INT); break;
        case CodeAttribute.D2L:             cast22(CodeAttribute.T_LONG); break;
        case CodeAttribute.D2F:             cast21(CodeAttribute.T_FLOAT); break;
        case CodeAttribute.I2B:             cast11(CodeAttribute.T_BYTE); break;
        case CodeAttribute.I2C:             cast11(CodeAttribute.T_CHAR); break;
        case CodeAttribute.I2S:             cast11(CodeAttribute.T_SHORT); break;
        case CodeAttribute.LCMP:            compare2(CompareExpr.Normal, CodeAttribute.T_INT);    break;
        case CodeAttribute.FCMPL:           compare(CompareExpr.FloatL,  CodeAttribute.T_FLOAT);  break;
        case CodeAttribute.FCMPG:           compare(CompareExpr.FloatG,  CodeAttribute.T_FLOAT);  break;
        case CodeAttribute.DCMPL:           compare2(CompareExpr.FloatL, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.DCMPG:           compare2(CompareExpr.FloatG, CodeAttribute.T_DOUBLE); break;
        case CodeAttribute.IFEQ:            iftest(0, new LiteralExpr(j2s.int0), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IFNE:            iftest(1, new LiteralExpr(j2s.int0), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IFLT:            iftest(2, new LiteralExpr(j2s.int0), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IFGE:            iftest(3, new LiteralExpr(j2s.int0), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IFGT:            iftest(4, new LiteralExpr(j2s.int0), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IFLE:            iftest(5, new LiteralExpr(j2s.int0), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ICMPEQ:       iftest(0, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ICMPNE:       iftest(1, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ICMPLT:       iftest(2, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ICMPGE:       iftest(3, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ICMPGT:       iftest(4, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ICMPLE:       iftest(5, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ACMPEQ:       iftest(0, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IF_ACMPNE:       iftest(1, pop(), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IFNULL:          iftest(0, new LiteralExpr(j2s.nil), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.IFNONNULL:       iftest(1, new LiteralExpr(j2s.nil), pop(), ca.getByte2(pc), pc - 1); pc += 2; break;
        case CodeAttribute.GOTO:            defineBranchPoint(exp.getEnd(), pc + ca.getByte2(pc) - 1, Boolean.TRUE); terminate = true; break;
        case CodeAttribute.GOTO_W:          defineBranchPoint(exp.getEnd(), pc + ca.getIndex4(pc) - 1, Boolean.TRUE); terminate = true; break;
        case CodeAttribute.JSR:
          int reta = pc + 2;
          int jsra = pc + ca.getByte2(pc) - 1;
          pushConst(j2s.getIntLiteral(reta), CodeAttribute.T_INT);
          jsrStack[jsrIndex++] = reta;
          pc = jsra;
          jsrFlag = true;
          break; /* Inline it! */
        case CodeAttribute.JSR_W:
          int retaw = pc + 4;
          int jsraw = pc + ca.getIndex4(pc) - 1;
          pushConst(j2s.getIntLiteral(retaw), CodeAttribute.T_INT);
          jsrStack[jsrIndex++] = retaw;
          pc = jsraw;
          jsrFlag = true;
          break; /* Inline it! */
        case CodeAttribute.RET:             ca.getIndex(pc); jsrIndex--; pc = jsrStack[jsrIndex]; break;
        case CodeAttribute.TABLESWITCH:     pc = tableSwitch(pc - 1); break;
        case CodeAttribute.LOOKUPSWITCH:    pc = lookupSwitch(pc - 1); break;
        case CodeAttribute.IRETURN:         doReturn(CodeAttribute.T_INT, pop());     terminate = true; break;
        case CodeAttribute.LRETURN:         doReturn(CodeAttribute.T_LONG, pop2());   terminate = true; break;
        case CodeAttribute.FRETURN:         doReturn(CodeAttribute.T_FLOAT, pop());   terminate = true; break;
        case CodeAttribute.DRETURN:         doReturn(CodeAttribute.T_DOUBLE, pop2()); terminate = true; break;
        case CodeAttribute.ARETURN:         doReturn(CodeAttribute.T_ADDRESS, pop()); terminate = true; break;
        case CodeAttribute.RETURN:          doReturn();                               terminate = true; break;
        case CodeAttribute.GETSTATIC:       getStatic(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.PUTSTATIC:       putStatic(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.GETFIELD:        getField(ca.getIndex2(pc));  pc += 2; break;
        case CodeAttribute.PUTFIELD:        putField(ca.getIndex2(pc));  pc += 2; break;
        case CodeAttribute.INVOKEVIRTUAL:   invokeVirtual(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.INVOKESPECIAL:   invokeSpecial(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.INVOKESTATIC:    invokeStatic(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.INVOKEINTERFACE: invokeInterface(ca.getIndex2(pc), ca.getByte(pc + 2)); pc += 4; break;
        case CodeAttribute.NEW:             doNew(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.NEWARRAY:        doNewarray(ca.getByte(pc)); pc++; break;
        case CodeAttribute.ANEWARRAY:       doANewarray(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.MULTIANEWARRAY:  multiANewarray(ca.getIndex2(pc), ca.getByte(pc + 2)); pc += 3; break;
        case CodeAttribute.ARRAYLENGTH:     arrayLength(); break;
        case CodeAttribute.ATHROW:          athrow(); break;
        case CodeAttribute.CHECKCAST:       checkCast(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.INSTANCEOF:      instanceOf(ca.getIndex2(pc)); pc += 2; break;
        case CodeAttribute.MONITORENTER:    monitorEnter(); break;
        case CodeAttribute.MONITOREXIT:     monitorExit(); break;

        case CodeAttribute.WIDE:
          bc = ca.getOpcode(pc++);
          switch (bc) {
          case CodeAttribute.IINC:          increment(ca.getIndex2(pc), ca.getByte2(pc + 2));  pc += 4; break;
          case CodeAttribute.ILOAD:         load(ca.getIndex2(pc),   CodeAttribute.T_INT);     pc += 2; break;
          case CodeAttribute.LLOAD:         load(ca.getIndex2(pc),   CodeAttribute.T_LONG);    pc += 2; break;
          case CodeAttribute.FLOAD:         load(ca.getIndex2(pc),   CodeAttribute.T_FLOAT);   pc += 2; break;
          case CodeAttribute.DLOAD:         load(ca.getIndex2(pc),   CodeAttribute.T_DOUBLE);  pc += 2; break;
          case CodeAttribute.ALOAD:         load(ca.getIndex2(pc),   CodeAttribute.T_ADDRESS); pc += 2; break;
          case CodeAttribute.ISTORE:        store(ca.getIndex2(pc),  CodeAttribute.T_INT);     pc += 2; break;
          case CodeAttribute.LSTORE:        store2(ca.getIndex2(pc), CodeAttribute.T_LONG);    pc += 2; break;
          case CodeAttribute.FSTORE:        store(ca.getIndex2(pc),  CodeAttribute.T_FLOAT);   pc += 2; break;
          case CodeAttribute.DSTORE:        store2(ca.getIndex2(pc), CodeAttribute.T_DOUBLE);  pc += 2; break;
          case CodeAttribute.ASTORE:        store(ca.getIndex2(pc),  CodeAttribute.T_ADDRESS); pc += 2; break;
          default:
            throw new scale.common.InternalError("Invalid java wide bytecode " + bc + " at " + (pc - 1));
          }
          break;
        default:
          throw new scale.common.InternalError("Invalid java bytecode " + bc + " at " + (pc - 1));
        }
        opc = pc;
      }
    }
  }
}
