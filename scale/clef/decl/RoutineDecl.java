package scale.clef.decl;

import java.util.Iterator;
import java.util.Map;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;

/** 
 * This is the base class representing all routine declarations.
 * <p> 
 * $Id: RoutineDecl.java,v 1.86 2007-10-04 19:58:04 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class RoutineDecl extends Declaration 
{
  private scale.clef.stmt.Statement        body;     // The body of the routine.
  private scale.score.Scribble             scribble; // Scribble graph generated for this call node.
  private scale.callGraph.CallGraph        cg;       // The callGraph containing this RoutineDecl.
  private scale.backend.SymbolDisplacement disp;     // Displacement used by the backend code generators.

  private Vector<CallSite>    callees;          // all routines called by this routine.
  private Vector<RoutineDecl> callers;          // all routines calling this routine.
  private Vector<Type>        calleeCandidates; // Keep track of all function pointers in the body of this routine.

  private VariableDecl ftnResultVar;     // The declared result variable for a function (if any).
  private int          lineNumber;       // The source line number.
  private int          cost;             // Used by inlining.
  private int          adrReg;           // Used by the code generator to specify the register containing the address of the variable.
  private int          profCallCnt;      // The number of times the this call occurred - supplied by profiling.
  private short        builtinFtn;       // 0 or index of builtin function. // Builtin function index - 0 means not builtin.
  private short        flags;            // Other attributes of the declaration.


  private static final int IS_REF_MASK        = 0x0001; // True if this routine has a body or is referenced in the Clef AST.
  private static final int IS_MAIN_MASK       = 0x0002; // True if this is the program's main procedure.
  private static final int USES_VA_START_MASK = 0x0004; // True if this routine uses va_start.
  private static final int USES_ALLOCA_MASK   = 0x0008; // True if this routine uses __builtin_alloca.
  private static final int INLINE_MASK        = 0x0010; // True if the programmer specified to inline this routine.
  private static final int NOINLINE_MASK      = 0x0020; // True if the programmer specified to not inline this routine.
  private static final int USES_SETJMP_MASK   = 0x0040; // True if this routine uses setjmp().
  private static final int RECURSIVE_MASK     = 0x0180; // Is the routine recursive.
  private static final int CANTINLINE_MASK    = 0x0200; // True if the routine cannot be inlined.
  private static final int RECURSIVE_SHIFT    = 7;

  /**
   * The function is completely "pure".
   */
  public final static int PURE = 7;
  /**
   * The function does not have side effects or use global variables.
   */
  public final static int PURESGV = 6;
  /**
   * The function does not have side effects.
   */
  public final static int PURESE = 4;
  /**
   * The function does not reference any global variables of this
   * program or modify any locations referenced by an argument.
   */
  public final static int PUREGVA = 3;
  /**
   * The function does not reference any global variables of this program.
   */
  public final static int PUREGV = 2;
  /**
   * The function does not modify any locations referenced by an argument.
   */
  public final static int PUREARGS = 1;
  /**
   * The function
   * <ul>
   * <li> may have side effects,
   * <li> may reference any global variables of this program, and
   * <li> may modify any locations referenced by an argument.
   * </ul>
   */
  public final static int NOTPURE = 0;

  private static final String[] purity = {
    "", "pa", "pg", "pga",
    "ps", "psa", "psg", "pure"};

  private static final int PURITY_LEVEL_SHIFT = 4;
  private static final int PURITY_LEVEL_MASK  = 0x70;

  /** 
   * This class represents directed edges between a pair of call nodes.
   * A call graph is really a multigraph, that is a pair of nodes may
   * have more than one edge between them.  This class groups together
   * all edges between two nodes.  
   */
  private static class CallSite 
  {
    private RoutineDecl callee;
    private int         sites; /* Number of sites */
    
    public CallSite(RoutineDecl callee)
    {
      this.callee = callee;
      sites = 1;
    }
    
    public void addNewSite()
    {
      sites++; 
    }
    
    public int numSites()
    {
      return sites;
    }
    
    public RoutineDecl getCallee()
    {
      return callee;
    }
  }

  /**
   * Create a routine with the specified name and type.
   * @param name is the method name
   * @param type is the method type
   * @param body is the method body
   */
  protected RoutineDecl(String                    name,
                        ProcedureType             type,
                        scale.clef.stmt.Statement body)
  {
    super(name, type);
    setBody(body);
    this.cg               = null;
    this.callees          = null;
    this.callers          = null;
    this.calleeCandidates = null;
    this.builtinFtn       = 0;
    this.lineNumber       = -1;
    this.profCallCnt      = -1;
  }

  protected RoutineDecl(String name, ProcedureType type)
  {
    this(name, type, null);
  }

  /**
   * Specify the call graph that contains this routine.
   */
  public final void specifyCallGraph(scale.callGraph.CallGraph cg)
  {
    this.cg = cg;
  }

  /**
   * Return true if the declaration is referenced somewhere in the
   * Clef AST or has a body.
   */
  public final boolean isReferenced()
  {
    return ((flags & IS_REF_MASK) != 0);
  }

  /**
   * Specify that the routine is referenced somewhere in the Clef AST.
   */
  public final void setReferenced()
  {
    flags |= IS_REF_MASK;
  }

  /**
   * Return true if the routine uses <code>va_start()</code>.
   */
  public final boolean usesVaStart()
  {
    return ((flags & USES_VA_START_MASK) != 0);
  }

  /**
   * Specify that the routine uses <code>va_start</code>.
   */
  public final void setUsesVaStart()
  {
    flags |= USES_VA_START_MASK;
  }

  /**
   * Return true if the routine uses <code>va_start()</code>.
   */
  public final boolean usesSetjmp()
  {
    return ((flags & USES_SETJMP_MASK) != 0);
  }

  /**
   * Specify that the routine uses <code>va_start</code>.
   */
  public final void setUsesSetjmp()
  {
    flags |= USES_SETJMP_MASK;
  }

  /**
   * Return true if the routine uses <code>__builtin_alloca()</code>.
   */
  public final boolean usesAlloca()
  {
    return ((flags & USES_ALLOCA_MASK) != 0);
  }

  /**
   * Specify that the routine uses <code>__builtin_alloca()</code>.
   */
  public final void setUsesAlloca()
  {
    flags |= USES_ALLOCA_MASK;
  }

  /**
   * Return true if the declaration is the <code>main</code> procedure.
   */
  public final boolean isMain()
  {
    return ((flags & IS_MAIN_MASK) != 0);
  }

  /**
   * Indicates that this procedure is the main procedure.  This marks
   * the <code>main</code> procedure in the C sense.
   */
  public final void setMain()
  {
    flags |= IS_MAIN_MASK;
  }

  /**
   * Return true if the programmer specified to inline this routine.
   */
  public final boolean inlineSpecified()
  {
    return ((flags & INLINE_MASK) != 0);
  }

  /**
   * Specifiy that the programmer wants this routine inlined.
   */
  public final void setInlineSpecified()
  {
    flags = (byte) ((flags & ~NOINLINE_MASK) | INLINE_MASK);
  }

  /**
   * Specifiy that the programmer wants this routine to not be inlined.
   */
  public final void setNoinlineSpecified()
  {
    flags = (byte) ((flags & ~INLINE_MASK) | NOINLINE_MASK);
  }

  /**
   * Return true if this routine cannot be inlined for whatever reason.
   */
  public final boolean cantInline()
  {
    return ((flags & CANTINLINE_MASK) != 0) || ((flags & NOINLINE_MASK) != 0);
  }

  /**
   * Specifiy that this routine cannot be inlined.
   */
  public final void setCantInline()
  {
    flags = (byte) (flags | CANTINLINE_MASK);
  }

  /**
   * Return the level of purity of the routine.  The level is a byte
   * where each bit in the byte represents some "pureness".
   * <table>
   * <thead>
   * <tr><th>bit</th><th>meaning</th>
   * </thead>
   * <tbody>
   * <tr><td>4</td>
   *     <td>does not have any side effects</td>
   * <tr><td>2</td> <td>does not reference any global variables of the
   *     program (it may modify file buffers and other OS things)</td>
   * <tr><td>1</td> <td>the routine does not modify any of its
   *     arguments or what they point to</td>
   * </tbody>
   * </table>
   */
  public int getPurityLevel()
  {
    return (flags & PURITY_LEVEL_MASK) >> PURITY_LEVEL_SHIFT;
  }

  public void setPurityLevel(int level)
  {
    flags = (byte) ((flags & ~PURITY_LEVEL_MASK) |
                    ((level << PURITY_LEVEL_SHIFT) & PURITY_LEVEL_MASK));
  }

  /**
   * Return true if this declaration has a purity level of PUREGV.
   */
  public boolean isPure()
  {
    return 0 != (flags & (PUREGV << PURITY_LEVEL_SHIFT));
  }

  /**
   * Return the declared function result variable (if any).
   */
  public VariableDecl getFtnResultVar()
  {
    return ftnResultVar;
  }

  /**
   * Specify the number of times this call occurred during execution.
   */
  public final void setProfCallCnt(int count)
  {
    this.profCallCnt = count;
  }

  /**
   * Return the number of times this call occurred during execution.
   */
  public final int getProfCallCnt()
  {
    return profCallCnt;
  }

  /**
   * Specify the builtin function index.  A value of zero indicates
   * that calls to this function are not a candidates for being
   * converted to in-line code.  This only applies to compilations
   * that specify <code>-gcc</code>.
   */
  public final void setBuiltIn(int index)
  {
    this.builtinFtn = (short) index;
  }

  /**
   * Return builtin function index.  A value of zero indicates that
   * calls to this function are not a candidates for being converted
   * to in-line code.  This only applies to compilations that specify
   * <code>-gcc</code>.
   */
  public final int getBuiltIn()
  {
    return builtinFtn;
  }

  /**
   * Return true if calls to this function are a candidates for being
   * converted to in-line code.  This only applies to compilations
   * that specify <code>-gcc</code>.
   */
  public final boolean isBuiltIn()
  {
    return (builtinFtn > 0);
  }

  /**
   * Declare the function result variable.
   */
  public void setFtnResultVar(VariableDecl ftnResultVar)
  {
    this.ftnResultVar = ftnResultVar;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    if (isMain())
      buf.append("main ");
    if (isReferenced())
      buf.append("ref ");
    if (inlineSpecified())
      buf.append("inline ");
    if (cantInline())
      buf.append("no-inl ");
    if (usesVaStart())
      buf.append("vas ");
    int pl = getPurityLevel();
    if (pl > 0) {
      buf.append(purity[pl]);
      buf.append(' ');
    }
    if (body == null)
      buf.append("* ");
    return buf.toString();
  }

  /**
   * Return the CallGraph that contains this RoutineDecl.
   */
  public scale.callGraph.CallGraph getCallGraph()
  {
    return cg;
  }

  /**
   * Add the profiling information to this call node.
   * @param profileOptions specifies which profiling instrumentation
   * to insert
   * @return the variable definition for the profiling information or
   * null if none
   */
  public VariableDecl addProfiling(int profileOptions)
  {
    if (scribble == null)
      return null;

    return scribble.addProfiling(profileOptions, isMain());
  }

  /**
   * Return the number of routines called by this routine.
   */
  public int numCallees()
  {
    if (callees == null)
      return 0;

    return callees.size();
  }

  /**
   * Return the specified callee.
   */
  public RoutineDecl getCallee(int i)
  {
    CallSite cs = callees.elementAt(i);
    return cs.getCallee();
  }
  
  /**
   * Return the number of routines that call this routine.
   */
  public int numCallers()
  {
    if (callers == null)
      return 0;

    return callers.size();
  }

  /**
   * Return the specified caller.
   */
  public RoutineDecl getCaller(int i)
  {
    return callers.elementAt(i);
  }
  
  /**
   * Return the number of routines that call this routine.
   */
  public int numCalleeCandidates()
  {
    if (calleeCandidates == null)
      return 0;

    return calleeCandidates.size();
  }

  /**
   * Return the return type of the specified caller.
   */
  public Type getCalleeCandidate(int i)
  {
    return calleeCandidates.elementAt(i);
  }
  
  /**
   * Return the name of the routine that the call node represents.
   */
  public final String getRoutineName()
  {
    return getName();
  }

  /**
   * Add a link in the call graph to the callee.
   * @param callee the call node that represents the routine we're
   * calling
   */
  public void addCallee(RoutineDecl callee)
  {
    if (callee != null)
      callee.addCaller(this);

    if (callees == null)
      callees = new Vector<CallSite>(5, 5);

    int i = callees.indexOf(callee);
    if (i >= 0) {
      callees.elementAt(i).addNewSite();
      return;
    }

    CallSite cs = new CallSite(callee);
    callees.addElement(cs);
  } 

  /**
   * Specify a caller of this call node.
   */
  public void addCaller(RoutineDecl caller)
  {
    if (callers == null) {
      callers = new Vector<RoutineDecl>(5);
      callers.addElement(caller);
      return;
    }
    if (callers.indexOf(caller) >= 0)
      return;

    callers.addElement(caller);
  }

  /**
   * Add a potential (indirect) call.
   * @param type is the return type of the routine called.
   */
  public void addCandidate(Type type)
  {
    if (calleeCandidates == null)
      calleeCandidates = new Vector<Type>(5, 5);
    calleeCandidates.addElement(type);
  }

  public void printCallees()
  {
    if (callees == null)
      return;

    int l = callees.size();
    for (int i = 0; i < l; i++) {
      if (i > 0)
        System.out.print(", ");
      CallSite cs = callees.elementAt(i);
      System.out.print(cs.getCallee().getName());
      System.out.print("(");
      System.out.print(cs.numSites());
      System.out.print(")");
    }
  }

  /**
   * Attach the Scribble CFG to the call node.  Note - this will
   * delete the Clef AST associated with the routine and the list of
   * candidate indirect calls.
   * @param scribble is the Scribble CFG
   */
  public void attachScribbleCFG(scale.score.Scribble scribble)
  {
    this.scribble = scribble;
  }

  /**
   * Remove all Clef AST stuff.
   */
  public void clearAST()
  {
    setBody(null); // Free up some space by eliminating the Clef AST.
    calleeCandidates = null;
  }

  /**
   * Return the Scribble graph associated with this node.
   */
  public scale.score.Scribble getScribbleCFG()
  {
    return scribble;
  }

  /**
   * Return the code generator displacement associated with this call
   * node.
   */
  public scale.backend.Displacement getDisplacement()
  {
    return disp;
  }

  /**
   * Specify the code generator displacement associated with this call
   * node.
   */
  public void setDisplacement(scale.backend.Displacement disp)
  {
    assert (disp instanceof scale.backend.SymbolDisplacement) :
      "Wrong displacement " + disp;
    this.disp = (scale.backend.SymbolDisplacement) disp;
  }

  /**
   * Return the register the code generator assigned to the address of
   * this variable.
   */
  public int getAddressRegister()
  {
    return adrReg;
  }

  /**
   * Specify the register the code generator assigned to the address
   * of this variable.
   */
  public void setAddressRegister(int reg)
  {
    this.adrReg = reg;
  }

  /**
   * Return the cost associated with this call node.
   */
  public int getCost()
  {
    return cost;
  }

  /**
   * Specify the cost associated with this call node.
   */
  public void setCost(int cost)
  {
    this.cost = cost;
  }

  /**
   * Add to the cost associated with this call node.
   */
  public void addCost(int cost)
  {
    this.cost += cost;
  }

  public void visit(Predicate p)
  {
    p.visitRoutineDecl(this);
  }

  /**
   * Return this routine's type
   */
  public ProcedureType getSignature()
  {
    return (ProcedureType) getType().getCoreType();
  }

  /**
   * Specify this routine's type
   */
  public void setSignature(ProcedureType s)
  {
    ProcedureType ot = (ProcedureType) getCoreType();
    if ((ot != null) && ot.isOldStyle())
      s.markAsOldStyle();
    setType(s);
  }

  /**
   * Return true if this is a specification of the routine and not the
   * actual routine.
   */
  public final boolean isSpecification()
  {
    return (body == null) && (scribble == null);
  }

  /**
   * Specify the Clef AST for this routine or <code>null</code> if none.
   */
  public final void setBody(scale.clef.stmt.Statement body)
  {
    this.body = body;
    if (body != null)
      setReferenced();
  }

  /**
   * Return the Clef AST for this routine or <code>null</code>.
   */
  public final scale.clef.stmt.Statement getBody()
  {
    return body;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    assert (i == 0) : "No such child " + i;
    return body;
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1;
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.  This method should be over-ridden
   * as it simplay returns the color red.
   */
  public DColor getDisplayColorHint()
  {
    if (scribble == null)
      return DColor.RED;

    return ((numCallees() == 0) ? DColor.GREEN :
                                  DColor.LIGHTBLUE);
  }

  /**
   * Returns true if this call node routine calls the specified
   * routine or calls a subroutine that results in a call to the
   * specified routine.
   * @param target is the specified routine
   * @param done is used to avoid infinite recursion and conbinatorial
   * explosion
   */
  public boolean calls(RoutineDecl target, HashSet<RoutineDecl> done)
  {
    if (!done.add(this))
      return false;

    if (callees == null)
      return false;

    int l = callees.size();
    for (int i = 0; i < l; i++) {
      CallSite    cs     = callees.elementAt(i);
      RoutineDecl callee = cs.getCallee();
      if (callee == target)
        return true;

      if (callee.calls(target, done))
        return true;
    }

    return false;
  }

  /**
   * Returns true if this call node routine calls itself or calls a
   * subroutine that results in a call to itself.
   */
  public boolean isRecursive()
  {
    int rec = (flags & RECURSIVE_MASK) >> RECURSIVE_SHIFT;

    if (rec == 0) {
      HashSet<RoutineDecl> done  = new HashSet<RoutineDecl>(23);
      boolean              isrec = calls(this, done);
      flags = (short) ((flags & ~RECURSIVE_MASK) |
                       ((isrec ? 3 : 1) << RECURSIVE_SHIFT));
      return isrec;
    }

    return (rec == 3);
  }

  /**
   * Return the source line number associated with this node or -1 if
   * not known.
   */
  public final int getSourceLineNumber()
  {
    return lineNumber;
  }

  /**
   * Set the source line number associated with this node or -1 if not
   * known.
   */
  public final void setSourceLineNumber(int lineNumber)
  {
    this.lineNumber = lineNumber;
  }

  public final boolean isRoutineDecl()
  {
    return true;
  }

  public final RoutineDecl returnRoutineDecl()
  {
    return this;
  }
}
