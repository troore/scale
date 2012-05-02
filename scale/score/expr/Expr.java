package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.LiteralMap;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.score.*;
import scale.score.dependence.*;
import scale.score.analyses.AliasAnnote;
import scale.score.pred.References;

/**
 * The base class for Score expression classes.
 * <p>
 * $Id: Expr.java,v 1.160 2007-10-17 13:40:00 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * All expression instances have an associated type.
 * <p>
 * An expression instance is an operation plus some number of
 * arguments.  Each argument is an expression instance as well.  The
 * value of an expression may propagate to another expression (as one
 * of its operands) or to a CFG node (e.g., {@link
 * scale.score.chords.ExprChord ExprChord} instance).
 * <p>
 * An expression instance has a fixed number of incoming data edges
 * (i.e., its operands) and one outgoing data edge.
 * <p>

 * As modeled by this class, an expression has some number of operands
 * which are represented as incoming data edges, and a value which is
 * represented as the outgoing data edge.  Some expression classes
 * have extra information which is not an operand.  This information
 * is sometimes properly part of the operator (e.g., the type of
 * conversion for {@link scale.score.expr.ConversionExpr
 * ConversionExpr} instances), and sometimes it is not even that
 * (e.g., the value of a literal).  This extra information is in some
 * sense not part of the CFG (i.e., these are non-graph edges).
 * <p>
 * Since an expression may have only one outgoing data edges an
 * expression instance is not unique in that there may be other 
 * instances that are equivalent (identical in a loose sense).
 * The copy() method is provided so that an equivalent expression
 * instance may be created from an exisiting expression instance.
 * Attempting to link in an expression instance that is already
 * linked into the CFG results in a scale.common.InternalError
 * exception.
 * <p>
 * Memory references are defined by instances of the {@link
 * scale.score.expr.LoadExpr LoadExpr} abstract class and the {@link
 * scale.score.expr.LoadValueIndirectExpr LoadValueIndirectExpr}
 * class.  
 * <ul>
 * <li>A {@link scale.score.expr.LoadDeclAddressExpr
 * LoadDeclAddressExpr} instance on the right-hand-side never
 * references memory - it evaluates to an address.
 * <li>A {@link scale.score.expr.LoadDeclValueExpr LoadDeclValueExpr}
 * instance always causes a memory reference (or register copy).
 * <li>A {@link scale.score.expr.LoadValueIndirectExpr
 * LoadValueIndirectExpr} instance always causes a memory reference.
 * </ul>
 */
  
public abstract class Expr extends Note
{
  /**
   * Side effect - none.
   */
  public static final int SE_NONE = 0;
  /**
   * Side effect - may cause overflow or underflow fault.
   */
  public static final int SE_OVERFLOW = 1;
  /**
   * Side effect - may cause fault because of domain values.
   */
  public static final int SE_DOMAIN = 2;
  /**
   * Side effect - changes some global state (e.g., memory location)
   */
  public static final int SE_STATE = 4;

  /**
   * True if floating point operations may be reordered.  Reordering
   * floating point operations may result in the generation of
   * slightly different floating point results by the compiled
   * program.  In this case floating point divides may be converted to
   * multiplies by the reciprocal.
   */
  public static boolean fpReorder = false; 

  private Type type;        // Expression type (e.g., int, etc.).
  private Note outDataEdge; // The expression that usees this expression's value.

  /**
   * Build an expression node.
   * @param type is the type of the value that results from this expression.
   */
  protected Expr(Type type) 
  {
    assert (type != null) : "Expresion with null type.";

    this.type        = type;
    this.outDataEdge = null;
  }

  protected Expr() 
  {
    this(null);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public abstract int sideEffects();

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the equivalent() method of the derived
   * classes.
   * @return true if classes are identical and the <i>core</i> types
   * are equivalent
   */
  public boolean equivalent(Expr exp)
  {
    if (exp == null)
      return false;

    if (exp.getClass() != getClass())
      return false;

    return (type.getCoreType() == exp.getCoreType());
  }

  /**
   * Return a String suitable for labeling this node in a graphical
   * display.  This method should be over-ridden as it simplay returns
   * the class name.
   */
  public String getDisplayLabel()
  {
    String mnemonic = toStringClass();
    if (mnemonic.endsWith("Expr"))
      mnemonic = mnemonic.substring(0, mnemonic.length() - 4);
    return mnemonic;
  }

  /**
   * Return a String specifying the color to use for coloring this
   * node in a graphical display.  This method should be over-ridden
   * as it simplay returns the color red.
   */
  public DColor getDisplayColorHint()
  {
    return DColor.LIGHTBLUE;
  }

  /**
   * Return a String specifying a shape to use when drawing this node
   * in a graphical display.  This method should be over-ridden as it
   * simplay returns the shape "box".
   */
  public DShape getDisplayShapeHint()
  {
    return DShape.BOX;
  }

  /**
   * Return the type of the expression.
   */
  public final Type getType()
  {
    return type;
  }

  /**
   * Return the core type of the expression.
   */
  public final Type getCoreType()
  {
    if (type == null)
      return null;
    return type.getCoreType();
  }

  /**
   * Return the type of the thing pointed to by the type of the expression.
   * Equivalent to
   * <pre>
   *   getCoreType().getPointedTo().getCoreType()
   * </pre>
   */
  public final Type getPointedToCore()
  {
    return type.getCoreType().getPointedTo().getCoreType();
  }

  /**
   * Set the type of the expression.
   * @param type is the type of the expression
   */
  public final void setType(Type type) 
  {
    assert (type != null) : "Expressions require a type " + this;
    this.type = type;
  }

  /**
   * Perform a deep copy of the expression tree.  By deep copy, we
   * mean that copies are made of the expression tree.
   * @return a copy of this expression
   */
  public abstract Expr copy();

  /**
   * Return a deep copy of the expression tree if this expression is
   * used.  Otherwise, return this expression.  By deep copy, we mean
   * that copies are made of all of the nodes in the expression tree.
   * An expression is used if it already has an out-going data edge.
   * @return a deep copy of this expression or this expression
   */
  public final Expr conditionalCopy()
  {
    if (outDataEdge == null)
      return this;
    return this.copy();
  }

  /**
   * Set the nth operand of an expression.  This method eliminates the
   * data edge between the previous operand expression and this
   * expression.  The new <code>operand</code> must not be
   * <code>null</code>.
   * @param operand - the new operand
   * @param position - indicates which operand
   * @return the previous operand
   */
  protected Expr setOperand(Expr operand, int position)
  {
    throw new scale.common.InternalError("Invalid operand position - " +
                                         position +
                                         " " +
                                         this);
  }

  /**
   * Return the specified operand.
   * @param position is the index of the operand
   */
  public Expr getOperand(int position)
  {
    throw new scale.common.InternalError("Invalid operand position - " +
                                         position +
                                         " " +
                                         this);
  }

  /**
   * Return an array of the operands to the expression.
   */
  public abstract Expr[] getOperandArray();

  /**
   * Return the number of operands to this expression.
   */
  public abstract int numOperands();

  /**
   * Return the specified in-coming data edge.
   */
  public final Expr getInDataEdge(int i)
  {
    return getOperand(i);
  }

  /**
   * Return the number of in-coming data edges.
   */
  public final int numInDataEdges()
  {
    return numOperands();
  }

  /**
   * Check if the given expression is defined by this expression.
   * @param lv is the given expression to check.
   * @return true if this statement is an assignment statement and expr
`  * appear on the LHS is the lvalue.
   */
  public boolean isDefined(Expr lv)
  {
    return false;
  }

  /**
   * Return true if this is a match expression.
   */
  public boolean isMatchExpr()
  {
    return false;
  }

  /**
   * Return true if this is a literal expression.
   */
  public boolean isLiteralExpr()
  {
    return false;
  }

  /**
   * Return true if this expression is a cast of an address.
   */
  public boolean isCast()
  {
    return false;
  }

  /**
   * This method determines if this expression's value is defined or
   * used by the expression.  We return true if the value is defined.
   * @return true if this expression's value is defined.
   */
  public boolean isDefined()
  {
    // Look at all the outbound edges.
    if (outDataEdge instanceof Expr) { // Is this node an expr?
      Expr expr = (Expr) outDataEdge;
      return expr.isDefined(this);
    }
    if (outDataEdge instanceof scale.score.chords.ExprChord)
      return ((scale.score.chords.ExprChord) outDataEdge).isDefined(this);

    return false;
  }

  /**
   * Return the variable reference for the expression.  We use this
   * to get the variable reference for operations such as array
   * subscript and structure operations.
   *
   * @return null since a general expression doesn't have a variable
   */
  public Expr getReference()
  {
    return null;
  }

  /**
   * Return the lvalue if this statement is an assignment statement.
   */
  public Expr getLValue()
  {
    return null;
  }

  /**
   * Return the rvalue if this statement is an assignment statement.
   */
  public Expr getRValue()
  {
    return null;
  }

  /**
   * Return the low-level representation.
   */
  public Expr getLow()
  {
    return this;
  }

  /**
   * Use this method when you may be modifying an in-coming data edge
   * to this expression while iterating over the in-coming edges.
   * @return an array of in-coming data edges.  
   */
  public final Expr[] getInDataEdgeArray()
  {
    return getOperandArray();
  }

  /**
   * Return the out-going data edge.
   */
  public final Note getOutDataEdge()
  {
    return outDataEdge;
  }

  /**
   * This method adds an outgoing data edge to this node.
   * Only expression nodes have an outbound data edge.
   * @param node Note to which this out edge is pointing
   */
  public final void setOutDataEdge(Note node)
  {
    assert (node != null) : "Note value required.";

    assert (outDataEdge == null) : "Expression already has an out-going data edge " + this;

    outDataEdge = node;
  }

  /** 
   * This method changes an incoming data edge to point to a new
   * expression.
   * <p>
   * This method ensures that the node previously pointing to this one
   * is updated properly, as well as, the node which will now point to
   * this node.
   * <p>
   * {@link Expr Expr} and {@link scale.score.chords.Chord Chord}
   * nodes have a fixed number of incoming edges with specific meaning
   * applied to each.  Hence, the edge being replaced is indicated by
   * position.
   * @param oldExpr is the expression to be replaced
   * @param newExpr is the new expression
   */
  public final void changeInDataEdge(Expr oldExpr, Expr newExpr)
  {
    assert (newExpr != null) : "Requires an incoming data edge " + this;

    int l = numInDataEdges();
    for (int i = 0; i < l; i++) {
      if (oldExpr == getInDataEdge(i)) {
        Expr exp = setOperand(newExpr, i);
        assert (exp == oldExpr) : "How can this be " + this;

        oldExpr.outDataEdge = null;
        return;
      }
    }
    throw new scale.common.InternalError("Old incoming data edge not found\n  " +
                                         oldExpr +
                                         "\n  " +
                                         this);
  }

  /**
   * This method deletes the outgoing data edge.  
   * <p>
   * This method is a helper routine.  It intended to aid another
   * routine which deletes this edge as an incoming edge.  The two
   * methods work together to maintain bi-directional edges. 
   * @param node Indicates edge to be deleted, by designating its opposite end.
   */
  public final void deleteOutDataEdge(Note node)
  {
    assert (node == outDataEdge) : "Edge to be deleted is not found - " + node;

    outDataEdge = null;
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(getDisplayLabel());
    buf.append(' ');
    buf.append(type);
    int n = numOperands();
    for (int i = 0; i < n; i++) {
      buf.append(' ');
      buf.append(getOperand(i));
    }
    return buf.toString();
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    return Lattice.Bot;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return Lattice.Bot;
  }

  /**
   * Return the Chord with the highest label value from the set of
   * Chords that must be executed before this expression.
   * @param lMap is used to memoize the expression to critical Chord
   * information
   * @param independent is returned if the expression is not dependent
   * on anything
   */
  protected scale.score.chords.Chord findCriticalChord(HashMap<Expr, scale.score.chords.Chord> lMap,
                                                       scale.score.chords.Chord                independent)
  {
    scale.score.chords.Chord cc   = getChord();
    int                      cmax = -1;
    int                      l    = numInDataEdges();

    for (int i = 0; i < l; i++) {
      scale.score.chords.Chord nc   = getInDataEdge(i).getCriticalChord(lMap, independent);
      int                      nmax = nc.getLabel();

      if (nmax <= cmax)
        continue;

      cmax = nmax;
      cc = nc;
    }

    return cc;
  }

  /**
   * Return the Chord with the highest label value from the set of
   * Chords that must be executed before this expression.
   * @param lMap is used to memoize the expression to critical Chord
   * information
   * @param independent is returned if the expression is not dependent
   * on anything
   */
  public final scale.score.chords.Chord getCriticalChord(HashMap<Expr, scale.score.chords.Chord> lMap,
                                                         scale.score.chords.Chord independent)
  {
    scale.score.chords.Chord cc = lMap.get(this);
    if (cc != null)
      return cc;

    scale.score.chords.Chord ind = findCriticalChord(lMap, independent);
    if (ind == null)
      return getChord();

    lMap.put(this, ind);
    return ind;
  }

  /**
   * Return true if this expression represents an operation that uses
   * scalar types.
   */
  public boolean isScalar()
  {
    return type.getCoreType().isAtomicType();
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    long canon = (getClass().getName().hashCode() << 1) ^ type.getCoreType().hashCode();
    int  l     = numOperands();
    for (int i = 0; i < l; i++) {
      Expr exp = getOperand(i);
      canon = canon ^ exp.canonical();
    }
    return canon;
  }

  /**
   * Return true if this expression is valid on the left side of an
   * assignment.
   */
  public boolean validLValue()
  {
    return false;
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public abstract void removeUseDef();

  /**
   * If this expression results in a variable being given a new value,
   * return the {@link LoadExpr LoadExpr} that specifies the variable.
   * @return a <code>null</code> or a {@link LoadExpr LoadExpr} that
   * defines a variable
   */
  public Expr getDefExpr()
  {
    return null;
  }

  /**
   * Determine the execution ordering of the two nodes.  This routine
   * returns either 0, 1, or -1 based on the following conditions:
   * <pre>
   * 0 if <i>this</i> and <i>n</i> are in the same CFG node on the 
   *   same side of the assignment.
   * 1 if <i>this</i> occurs before <i>n</i>.
   * -1 if <i>n</i> occurs before <i>this</i>.
   * </pre>
   * Two nodes are equivalent if we can't determine that 
   * one defintely occurs before the other (<i>e.g.,</i> in an assignment
   * statement, <tt>a[i] = a[i-1]</tt>, the execution order is not
   * defined.
   * The right hand side of an assignment is considered to precede
   * the left-hand side.
   * <p>
   * NOTE - this method will not perform correctly if the Chords have
   * not been labeled.
   * @param n the node to compare against.
   * @return 0, 1, or -1 based upon the lexical ordering.
   * @see scale.score.chords.Chord#executionOrder
   * @see scale.score.Scribble#labelCFG
   */
  public final int executionOrder(Expr n) 
  {
    // The right hand side of an assignment is considered to precede
    // the left-hand side.

    scale.score.chords.Chord nc = n.getChord();
    scale.score.chords.Chord tc = getChord();

    if (tc != nc)
      return tc.executionOrder(nc);

    if (isDefined())
      return 1;

    if (n.isDefined())
      return -1;

    if (isMemoryDef()) {
      if (!n.isMemoryDef()) // If this is a read and n is a write, then lexically forward.
        return -1;
      return 0;
    }
     
    if (n.isMemoryDef()) // If this is a write and n is a read, then lexically backward.
      return 1;

    return 0; // Both on the same side of the asignment.
  }

  /**
   * Return the "execution ordinal" of this expression.  Let
   * <code>o1</code> and <code>o2</code> be the execution ordinals of
   * two expressions <code>e1</code> and <code>e2</code> respectively.
   * Then, <code>o1</code> is less than <code>o2</code> <b>iff</b>
   * <code>e1</code> is executed before <code>e2</code>.  The right
   * hand side of an assignment is considered to precede the left-hand
   * side.  Two expressions in the same expression tree will have the
   * same ordinal.
   * <p> NOTE - this method will not perform correctly if the Chords
   * have not been labeled.
   * @see #executionOrder
   * @see scale.score.chords.Chord#executionOrder
   * @see scale.score.Scribble#labelCFG
   */
  public int executionOrdinal() 
  {
    scale.score.chords.Chord nc = getChord();

    return (nc.getLabel() << 1) + (isDefined() ? 1 : 0);
  }

  /**
   * Return the node which represents the loop that contains this node.
   */
  public final scale.score.chords.LoopHeaderChord getLoopHeader() 
  { 
    scale.score.chords.Chord s = getChord();
    if (s == null)
      return null;
    return getChord().getLoopHeader();
  }

  /**
   * Return true if the node reference is a definition. That is, does
   * this node represents a write to a memory location.
   */
  public boolean isMemoryDef()
  {
    return false;
  }

  /**
   * Return the coefficient of a linear expression. Typically, we are
   * trying to determine if an array subscript expression is a linear
   * function of the loop index expression.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {
    return 0;
  }

  /**
   * Return the {@link scale.score.dependence.AffineExpr affine
   * representation} for this expression or <code>null</code> if it is
   * not affine.
   * @param affines is a mapping from an expression to its representation
   * and is used to avoid combinatorial explosion through use-def
   * links
   * @param thisLoop the loop for which this expression may be affine
   * @see scale.score.dependence.AffineExpr
   */
  public final AffineExpr getAffineExpr(HashMap<Expr, AffineExpr>          affines,
                                        scale.score.chords.LoopHeaderChord thisLoop)
  {
    AffineExpr ae = affines.get(this);
    if (ae == null) {
      ae = getAffineRepresentation(affines, thisLoop);
      if (ae == null)
        System.out.println("** expr " + this);
      affines.put(this, ae);
    }
    return (ae == AffineExpr.notAffine) ? null : ae;
  }

  /**
   * Return the {@link scale.score.dependence.AffineExpr affine
   * representation} for this expression.  This method should be
   * called only from the {@link #getAffineExpr getAffineExpr()}
   * method.  This method never returns <code>null</code>.
   * @param affines is a mapping from an expression to its representation
   * and is used to avoid combinatorial explosion through use-def
   * links
   * @param thisLoop the loop for which this expression may be affine
   * @see scale.score.dependence.AffineExpr
   * @see scale.score.dependence.AffineExpr#notAffine
   */
  protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>          affines,
                                               scale.score.chords.LoopHeaderChord thisLoop)
  {
    return AffineExpr.notAffine;
  }

  /**
   * Return the {@link DualExpr DualExpr} containing this {@link
   * SubscriptExpr SubscriptExpr} or null if none.  Follow the use-def
   * chain if necessary.
   */
  public final DualExpr getDualExpr()
  {
    Expr curExpr = this;
    while (curExpr != null) {
      if (curExpr instanceof DualExpr)
        return (DualExpr) curExpr;

      if (curExpr instanceof LoadValueIndirectExpr) {
        curExpr = curExpr.getOperand(0);
        continue;
      }

      scale.score.chords.ExprChord sexp = curExpr.getUseDef();
      if (sexp == null)
        return null;

      curExpr = sexp.getRValue();
    }

    return null;
  }

  /**
   * Return the use-def link for the expression.
   * It's null for most expressions.
   */
  public scale.score.chords.ExprChord getUseDef()
  {
    return null;
  }

  /**
   * Set the use-def link for the expression.
   * It's null for most expressions.
   */
  public void setUseDef(scale.score.chords.ExprChord se)
  {
  }

  /**
   * Return true if the expression loads a value from memory.
   */
  public boolean isMemRefExpr()
  {
    return false;
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return false;
  }

  /**
   * Return the call expression or null if none.
   * @param ignorePure is true if pure function calls are to be ignored.
   */
  public CallExpr getCall(boolean ignorePure)
  {
    int n  = numOperands();
    if (n <= 0)
      return null;

    for (int i = 0; i < n; i++) {
      Expr op = getOperand(i);
      if (op == null)
        continue;

      CallExpr ce = op.getCall(ignorePure);
      if (ce != null)
        return ce;
    }

    return null;
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
  }

  /**
   * This node is no longer needed so sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    int n  = numOperands();
    for (int i = 0; i < n; i++) {
      Expr op = getOperand(i);
      if (op != null)
        op.unlinkExpression();
    }
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   * A node is no longer needed if has no out-going data edge.
   */
  public final void conditionalUnlinkExpression()
  {
    if (outDataEdge != null)
      return;
    unlinkExpression();
  }

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    boolean inv = true;
    int     n   = numOperands();
    for (int i = 0; i < n; i++) {
      Expr op = getOperand(i);
      inv &= loop.isInvariant(op);
    }
    return inv;
  }

  /**
   * Return true if this expression may result in the generation of a
   * call to a subroutine.
   */
  public boolean mayGenerateCall()
  {
    boolean mgc = false;
    int     n   = numOperands();
    for (int i = 0; i < n; i++) {
      Expr op = getOperand(i);
      mgc |= op.mayGenerateCall();
    }
    return mgc;
  }

  /**
   * Return the {@link SubscriptExpr SubscriptExpr} that this load
   * uses or <code>null</code> if none is found.  This method uses the
   * use-def link to find an existing {@link SubscriptExpr SubscriptExpr}
   * instance.
   */
  public SubscriptExpr findSubscriptExpr()
  {
    return null;
  }

  public void setTemporalReuse(int level)
  {
    throw new scale.common.InternalError("Not allowed " + this);
  }

  public void setCrossloopReuse(int level)
  {
    throw new scale.common.InternalError("Not allowed " + this);
  }

  public void setSpatialReuse(int level)
  {
    throw new scale.common.InternalError("Not allowed " + this);
  }
 
  public void setStep(int step)
  {
    throw new scale.common.InternalError("Not allowed " + this);
  }

  public int getReuseLevel()
  {
    return 0;
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    return false;
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    return false;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   */
  public abstract boolean optimizationCandidate();

  /**
   * Get the alias annotation associated with a Scribble operator.
   * Most Scribble operators do not have alias variables so this
   * routine may return null.  Typically, the alias variable
   * information is attached to the declaration node associated with
   * the load operations.  However, we sometimes need to create alias
   * variables to hold alias information that is not directly assigned
   * to a user variable (e.g., <tt>**x</tt>).
   * @return the alias annotation associated with the expression
   */
  public AliasAnnote getAliasAnnote()
  {
    // Check if the expression node has the alias info - this means
    // that we've created a 'temporary' variable to hold the alias
    // info.  We also remove the 'temporary' alias variable from the
    // expression to reclaim memory - it's a create once, use once
    // value.

    AliasAnnote aa = (AliasAnnote) getAnnotation(AliasAnnote.annotationKey());
    if (aa == null)
      return null;

    removeAnnotation(aa);
    return aa;
  }

  /**
   * Add all declarations referenced in this expression to the
   * Vector.
   */
  public abstract void getDeclList(AbstractCollection<Declaration> varList);

  /**
   * Add all {@link LoadExpr LoadExpr} instances in this expression to
   * the Vector.
   */
  public abstract void getLoadExprList(Vector<LoadExpr> expList);

  /**
   * Add all {@link Expr Expr} instances in this expression to the Vector.
   */
  public abstract void getExprList(Vector<Expr> expList);
  /**
   * Push all of the operands of this expression on the Stack.
   */
  public abstract void pushOperands(Stack<Expr> wl);
  /**
   * Replace all occurrances of a Declaration with another
   * Declaration.
   * @return true if a replace occurred.
   */
  public abstract boolean replaceDecl(Declaration oldDecl, Declaration newDecl);
  /**
   * Record any variable references in this expression in the table of
   * references.
   */
  public abstract void recordRefs(scale.score.chords.Chord stmt, References refs);
  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public abstract void removeRefs(scale.score.chords.Chord stmt, References refs);

  /**
   * Return a simplied equivalent expression.
   * This method may modify the original expression.
   * This is used in the lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr reduce()
  {
    return this;
  }

  /**
   * Check this node for validity.  This method throws an exception if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();
    if (getChord() == null)
      throw new scale.common.InternalError("No chord for " + this);
  }

  /**
   * Remove occurances of {@link DualExpr DualExpr} instances.
   * @return true if a {@link DualExpr DualExpr} instance was removed.
   */
  public boolean removeDualExprs()
  {
    int l = numOperands();;
    boolean changed = false;
    for (int i = 0; i < l; i++)
      changed |= getOperand(i).removeDualExprs();

    if (!(this instanceof DualExpr))
      return changed;

    DualExpr de = (DualExpr) this;

    de.lowerPermanently();

    return true;
  }

  /**
   * Add a cast of an address if required.  Optimizations often skip
   * over address casts when finding applicable expressions.  This
   * adds those casts back in.
   * @param orig is the expression being replaced
   * @return the replacement expression of a cast of the replacement
   * expression
   */
  public final Expr addCast(Expr orig)
  {
    Type t = getCoreType();
    if (!t.isPointerType())
      return this;

    return ConversionExpr.create(orig.getType(),
                                 this,
                                 scale.clef.expr.CastMode.CAST);
  }

  /**
   * Add a cast of an address if required.  Optimizations often skip
   * over address casts when finding applicable expressions.  This
   * adds those casts back in.
   * @param orig is the type of the expression being replaced
   * @return the replacement expression of a cast of the replacement expression
   */
  public final Expr addCast(Type orig)
  {
    Type t = getCoreType();
    if (!t.isPointerType() && !orig.isPointerType())
      return this;

    return ConversionExpr.create(orig, this, scale.clef.expr.CastMode.CAST);
  }

  /**
   *  Return true if the result of the expression is either true (1)
   *  or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    return false;
  }
}
