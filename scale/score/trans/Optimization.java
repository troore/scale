package scale.score.trans;

import java.util.AbstractCollection;
import java.util.Iterator;

import scale.common.*;
import scale.score.*;
import scale.score.expr.Expr;
import scale.score.chords.*;
import scale.clef.type.Type;
import scale.clef.decl.VariableDecl;
import scale.clef.LiteralMap;

/**
 * This class is the base class for all optimizations performed on the
 * CFG.
 * <p>
 * $Id: Optimization.java,v 1.27 2007-10-04 19:58:36 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * All optimizations applied to the CFG must be defined in this package
 * and must be derived from this class.
 */
public abstract class Optimization
{
  /**
   * The optimization requires that the CFG not be in SSA form.
   */
  public static final int NO_SSA = 0;
  /**
   * The optimization requires that the CFG be in SSA form.
   */
  public static final int IN_SSA = 1;
  /**
   * The optimization requires that the CFG be in SSA form.
   */
  public static final int VALID_SSA = 2;
  /**
   * The optimization does not require SSA form or non-SSA form.
   */
  public static final int NA_SSA = 3;
  /**
   * Do not move expressions whose execution cost estimate is less
   * than minimumExecutionCost.
   */
  public static int minimumExecutionCost = 3;
  /**
   * True if unsafe optimuzations are allowd.  An unsafe optimization
   * is one that will cause the generated program to be incorrect if
   * there is aliasing.
   */
  public static boolean unsafe = false; 
  /**
   * True if floating point operations may be reordered.  Reordering
   * floating point operations may result in the generation of
   * slightly different floating point results by the compiled
   * program.
   */
  public static boolean fpReorder = false; 
  /**
   * Should signed integers wrap upon overflow?
   */
  public static boolean signedIntsWrapOnOverflow = true; 
  /**
   * Should unsigned integers wrap upon overflow?
   */
  public static boolean unsignedIntsWrapOnOverflow = true; 
  /**
   * True if there may be aliases between address arguments to
   * subroutines.  
   */
  public static boolean hasDummyAliases = false;
  /**
   * The CFG. 
   */
  protected Scribble scribble; 
  /**
   * The name generator to use for any variables created. 
   */
  protected UniqueName un;       
  /**
   * True if CFG nodes added or deleted by the optimization. 
   */
  protected boolean dChanged = false; 
  /**
   * True if the variable {@link scale.score.pred.References
   * references} are no longer valid.
   */
  protected boolean rChanged;
  /**
   * True if the optimization should be traced.
   */
  protected boolean trace;

  /**
   * @param scribble is the CFG to be transformed
   * @param tempPrefix is used to generate temporary variable names
   */
  public Optimization(Scribble scribble, String tempPrefix)
  {
    super();

    this.scribble = scribble;
    this.un       = new UniqueName(tempPrefix);
  }

  /**
   * Sets the <code>trace</code> flag.
   */
  protected boolean setTrace(boolean classTrace)
  {
    trace = Debug.trace(scribble.getRoutineDecl().getName(), classTrace, 3);
    return true;
  }

  /**
   * Create a new temporary variable for use by the optimized code.
   * The variable definition is added to the set of variables for this CFG.
   */
  protected VariableDecl genTemp(Type t)
  {
    VariableDecl vd = new VariableDecl(un.genName(), t);
    vd.setTemporary();
    scribble.addDeclaration(vd);
    return vd;
  }

  /**
   * Perform the optimization.  Each optimization must specify
   * <ul>
   * <li>if the CFG is left in valid SSA form
   * <li>if the dominance information is still valid
   * <li>if the variable reference information is still valid
   * </ul>
   * after it completes.  The {@link scale.score.Scribble Scribble}
   * class provides methods that may be used to sepcify this
   * information.
   */
  public abstract void perform();

  /**
   * Return whether this optimization requires that the CFG be in SSA form.
   * It returns either
   * <dl>
   * <dt>NO_SSA<dd>the CFG must not be in SSA form,
   * <dt>IN_SSA<dd>the CFG must be in SSA form,
   * <dt>VALID_SSA<dd>the CFG must be in valid SSA form, or
   * <dt>NA_SSA<dd>the CFG does not need to be in valid SSA form.
   * </dl>
   */
  public int requiresSSA()
  {
    return VALID_SSA;
  }

  /**
   * Place array elements in ascending lexigraphical order by CFG node
   * execution order.
   * @param v is an array of CFG nodes or expressions
   * @see scale.score.chords.Chord#pushChordWhenReady
   */
  public static void sort(Object[] v)
  {
    int size = v.length;
    if (size <= 1)
      return;

    int[] sort = new int[size];
    for (int i = 0; i < size; i++) {
      Note  exp = (Note) v[i];
      Chord c   = exp.getChord();
      sort[i] = c.getLabel();
    }

    boolean flag;
    int     jumpSize = size;
    do {
      flag = false;
      jumpSize = (10 * jumpSize + 3) / 13;
      int ul = size - jumpSize;
      for (int i = 0; i < ul; i++) {
        int k  = i + jumpSize;
        int si = sort[i];
        int sk = sort[k];
        boolean swap = false;

        if (si > sk)
          swap = true;
        else if (si == sk) {
          Chord nic = ((Note) v[i]).getChord();
          if (nic == ((Note) v[k]).getChord()) {
            if (nic.isAssignChord()) {
              if ((v[i] instanceof ExprChord) ||
                  ((ExprChord) nic).isDefined((Expr) v[i]))
                swap = true;
            }
          }
        }

        if (swap) {
          Object to = v[i];
          v[i] = v[k];
          v[k] = to;
          sort[i] = sk;
          sort[k] = si;
          flag = true;
        }
      }
    } while (flag || (jumpSize > 1));
  }

  /**
   * Insert new copy CFG nodes between CFG nodes in the specified set
   * and CFG nodes that are not in the set but have an edge to them
   * from a node in the set.
   * @param nodes is the set of CFG nodes
   * @param lhs is the left hand side for new CFG nodes
   * @param rhs is the right hand side for new CFG nodes
   * @return the number of new CFG nodes added
   */
  public static int insertStores(AbstractCollection<Chord> nodes, Expr lhs, Expr rhs)
  {
    int             nnc = 0;
    Iterator<Chord> it2 = nodes.iterator();
    while (it2.hasNext()) {
      Chord ex  = it2.next();
      int   lll = ex.numOutCfgEdges();
      for (int j = 0; j < lll; j++) {
        Chord fc = ex.getOutCfgEdge(j);;
        if (nodes.contains(fc))
          continue;

        ExprChord sc = new ExprChord(lhs.conditionalCopy(), rhs.conditionalCopy());

        sc.setSourceLineNumber(ex.getSourceLineNumber());
        sc.setLabel(ex.getLabel());
        nnc++;

        if (ex.isLoopTail())
          ex.insertBeforeInCfg(sc);
        else
          ex.insertAfterOutCfg(sc, fc);
      }
    }

    return nnc;
  }

  /**
   * Print out a trace message to <code>System.out</code>.  This
   * method is meant to be used in an <code>assert</code> statement.
   * @param msg is the message
   * @param o is an object to append to the message
   * @return true
   */
  public final boolean assertTrace(boolean trace, String msg, Object o)
  {
    if (!trace)
      return true;

    System.out.print(msg);
    if (o != null)
      System.out.print(o);
    System.out.println("");

    return true;
  }

  /**
   * Print out a trace message to <code>System.out</code>.  This
   * method is meant to be used in an <code>assert</code> statement.
   * @param msg is the message
   * @param val is an integer to append to the message
   * @return true
   */
  public final boolean assertTrace(boolean trace, String msg, long val)
  {
    if (!trace)
      return true;

    System.out.print(msg);
    System.out.print(val);
    System.out.println("");

    return true;
  }

  /**
   * Print out a trace message to <code>System.out</code>.  This
   * method is meant to be used in an <code>assert</code> statement.
   * @param msg is the message
   * @param val is an integer to append to the message
   * @return true
   */
  public final boolean assertTrace(boolean trace, String msg, double val)
  {
    if (!trace)
      return true;

    System.out.print(msg);
    System.out.print(val);
    System.out.println("");

    return true;
  }

  /**
   * Print out a trace message to <code>System.out</code>.  This
   * method is meant to be used in an <code>assert</code> statement.
   * @param msg is the message
   * @param val is an integer to append to the message
   * @return true
   */
  public final boolean assertTrace(boolean trace, String msg, boolean val)
  {
    if (!trace)
      return true;

    System.out.print(msg);
    System.out.print(val);
    System.out.println("");

    return true;
  }

  /**
   * Print out a trace message to <code>System.out</code>.  This
   * method is meant to be used in an <code>assert</code> statement.
   * @param msg is the message
   * @param v is a vector of things to display
   * @return true
   */
  public final boolean assertTrace(boolean trace, String msg, Vector<? extends Object> v)
  {
    if (!trace)
      return true;

    if (v == null)
      return true;

    int l = v.size();
    for (int i = 0; i < l; i++) {
      System.out.print(msg);
      System.out.print(i);
      System.out.print(" ");
      System.out.print(v.get(i));
      System.out.println("");
    }

    return true;
  }
}
