package scale.score.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.VoidType;
import scale.clef.decl.*;
import scale.score.*;
import scale.score.dependence.AffineExpr;
import scale.score.analyses.*;
import scale.score.pred.References;

/** 
 * This class is the base class for expressions that represent
 * references to memory.
 * <p>
 * $Id: LoadExpr.java,v 1.90 2007-10-17 13:40:00 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents use-def chains.
 */
public abstract class LoadExpr extends Expr
{
  /**
   * true if the non-SSA original variable should be compared in
   * equivalent() method.
   */
  private static boolean useOriginal;

  /**
   * true if the may-use information should be compared in
   * equivalent() method.
   */
  private static boolean useMayUse;

  /**
   * Use->def link.
   */
  private scale.score.chords.ExprChord useDef;

  /**
   * The declaration for the value loaded - if any.
   */
  private Declaration decl;
  
  /**
   * may use information - due to aliasing.
   */
  private MayUse mayUse;

  /**
   * Reuse level of the array reference.
   */
  private int reuseLevel = 0;

  public LoadExpr(Type t, Declaration decl)
  {
    super(t);
    this.decl   = decl;
    this.useDef = null;
    this.mayUse = null;
  }

  protected LoadExpr()
  {
    this(VoidType.type, null);
  }

  /**
   * Return the original declaration of the value loaded.  This is the
   * variable before SSA renumbering is used to determine whether two
   * virtual vars are in the same subset or if one is a
   * superset. Hence the name.
   */ 
  public Declaration getSubsetDecl()
  {
    if (decl.isVariableDecl())
      return ((VariableDecl) decl).getOriginal();
    return decl;
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the equivalent() method of the derived
   * classes.
   * @return true if the expressions are equivalent
   * @see #setUseOriginal
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;

    Declaration od = ((LoadExpr) exp).decl;
    Declaration nd = decl;

    if (useOriginal && nd.isVariableDecl() && od.isVariableDecl()) {
      nd = ((VariableDecl) nd).getOriginal();
      od = ((VariableDecl) od).getOriginal();
    }

    if (nd != od)
      return false;

    if (useMayUse) {
      MayUse mayUse1 = ((LoadExpr) exp).mayUse;
      if ((mayUse == null) && (mayUse1 == null))
        return true;
      if ((mayUse == null) || (mayUse1 == null))
        return false;

      return (mayUse.findMayDef() == mayUse1.findMayDef());
    }

    return true;
  }

  /**
   * Specify whether the equivalent() method should use the non-SSA
   * original {@link scale.clef.decl.VariableDecl VariableDecl}.
   * @return the previous setting
   * @see #equivalent
   * @see scale.score.trans.PRE
   */
  public static boolean setUseOriginal(boolean flg)
  {
    boolean s = useOriginal;
    useOriginal = flg;
    return s;
  }

  /**
   * Specify whether the <code>equivalent()</code> method should use
   * the may-use information.
   * @return the previous setting
   * @see #equivalent
   * @see scale.score.trans.ValNum
   */
  public static boolean setUseMayUse(boolean flg)
  {
    boolean s = useMayUse;
    useMayUse = flg;
    return s;
  }

  /**
   * Make a copy of this load expression without the use - def
   * information.
   */
  public abstract Expr copyNoUD();

  /**
   * Define a link from this load of a value to the
   * scale.score.chords.ExprChord that defines the value.
   * @param expr the new use-def link
   */
  public final void setUseDef(scale.score.chords.ExprChord expr)
  {
    if (useDef != null)
      useDef.removeDefUse(this);
    useDef = expr;
    if (useDef != null)
      useDef.addDefUse(this);
  }

  /**
   * Return the {@link scale.score.chords.ExprChord ExprChord} that defines
   * the value load by this load expression.
   */
  public final scale.score.chords.ExprChord getUseDef()
  {
    return useDef;
  }

  /**
   * Add may use information to the load expression.
   * @param mayUse the expresion representing the may use
   */
  public void addMayUse(MayUse mayUse)
  {
    assert ((this.mayUse == mayUse) || (this.mayUse == null) || (mayUse == null)) :
      "Resetting may-use information " + this;

    this.mayUse = mayUse;
    if (mayUse != null)
      mayUse.setGraphNode(this);
  }

  /**
   * Return the may use information assocaited with the load.
   */
  public final MayUse getMayUse()
  {
    return mayUse;
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    setUseDef(null);
    mayUse = null;
  }

  /**
   * Return the Clef declaration of the variable being loaded.
   */
  public final Declaration getDecl()
  {
    return decl;
  }

  /**
   * Change the declaration associated with the load operation.
   * @param decl is the new declataion
   */
  public final void setDecl(Declaration decl)
  {
    this.decl = decl;
  }

  /**
   * Return the name of the declaration being loaded.
   */
  public String getName()
  {
    if (decl == null)
      return "??";
    return decl.getName();
  }

  /**
   * Return an array of the operands to the expression.
   */
  public final Expr[] getOperandArray()
  {
    return new Expr[0];
  }

  /**
   * Return the number of operands to this expression.
   */
  public final int numOperands()
  {
    return 0;
  }

  /**
   * Return the value associated with the load expression.  The
   * value is the load itself. 
   * @return the load expression.
   */
  public Expr getReference()
  {
    return this;
  }

 /**
   * Given a load expression, return the object expression for the load.
   * This is the converse of <tt>getReference</tt>.  If the expression
   * is the structure or array name which is part of a field reference
   * or array subscript then return the expression representing the
   * field or the subscript.  For a scalar load, the method returns
   * the load.
   *
   * @return the object representing the load.
   */
  public Expr getObject()
  {
    Object parent = getOutDataEdge();
    if ((parent instanceof FieldExpr) ||
        (parent instanceof ArrayIndexExpr) ||
        (parent instanceof SubscriptExpr)) {
      return (Expr) parent;
    } 
    return this;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    if (decl != null)
      return (getClass().getName().hashCode() << 1) ^ decl.hashCode();

    return super.canonical(); /* Use the operand instead */
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    if (useDef != null) {
      useDef.removeDefUse(this);
      useDef = null;
    }
    super.unlinkExpression();
  }

  /**
   * Return true if the node reference is a definition. That is, does
   * this node represents a write to a memory location.
   */
  public boolean isMemoryDef()
  {
    return isDefined();
  }

  /**
   * Return the {@link SubscriptExpr SubscriptExpr} that this load
   * uses or <code>null</code> if none is found.  This method uses the
   * use-def link to find an existing <code>SubscriptExpr</code
   * instance.
   */
  public SubscriptExpr findSubscriptExpr()
  {
    scale.score.chords.ExprChord def = getUseDef();
    if (def == null)
      return null;
    Expr      rv  = def.getRValue();
    return rv.findSubscriptExpr();
  }

  public void setTemporalReuse(int level)
  {
    int temp = 2 << (level * 2 + 6);
      
    reuseLevel |= temp;
  }

  public void setCrossloopReuse(int level)
  {
    setTemporalReuse(level);
  }

  public void setSpatialReuse(int level)
  {
    int temp = 1 << (level * 2 + 6);
      
    reuseLevel |= temp;  
  }
 
  public void setStep(int step)
  {
    int t;

    if (step > 0)
      t = step & 0x0001f;
    else
      t = ((-step) & 0x0001f) | 0x0020;

    reuseLevel |= t << 1;  
  }

  public int getReuseLevel()
  {
    return reuseLevel;
  }

  /**
   * Return true if the expression loads a value from memory.
   */
  public boolean isMemRefExpr()
  {
    return true;
  }

  /**
   * Return true if the variable referenced is in Fortran COMMON.
   */
  public final boolean referencesVariableInCommon()
  {
    return decl.isEquivalenceDecl();
  }

  /**
   * Return true if this expression is valid on the left side of an
   * assignment.
   */
  public boolean validLValue()
  {
    return true;
  }

  /**
   * Return true if this expression contains a reference to the variable.
   * @see #setUseOriginal
   */
  public final boolean containsDeclaration(Declaration decl)
  {
    Declaration od = decl;
    Declaration nd = this.decl;

    if (useOriginal && nd.isVariableDecl() && od.isVariableDecl()) {
      nd = ((VariableDecl) nd).getOriginal();
      od = ((VariableDecl) od).getOriginal();
    }

    return (nd == od);
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public final boolean dependsOnDeclaration(Declaration decl)
  {
    if (containsDeclaration(decl))
      return true;

    if (useDef == null)
      return false;

    return useDef.getLValue().dependsOnDeclaration(decl) ||
           useDef.getRValue().dependsOnDeclaration(decl);
  }

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
    AliasAnnote aa = (AliasAnnote) decl.getAnnotation(AliasAnnote.annotationKey());
    if (aa != null)
      return aa;

    assert (decl.isTemporary() || !(decl instanceof VariableDecl)) :
      "Variable without alias variable " + this;

    return null;
  }

  /**
   * Add all declarations referenced in this expression to the Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    if (!varList.contains(decl))
      varList.add(decl);
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    expList.addElement(this);
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
  }

  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
  }

  /**
   * Replace all occurrances of a Declaration with another Declaration.
   * Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    if (oldDecl != decl)
      return false;

    decl = newDecl;
    return true;
  }

  /**
   * Check this node for validity.
   * This method throws an exception if the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    if (useDef == null)
      return;

    if (!useDef.checkDefUse(this))
      throw new scale.common.InternalError("Incorrect use-def link " +
                                           this +
                                           " -> " +
                                           useDef);
  }

  /**
   * Record any variable references in this expression in the table
   * of references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    refs.recordUse(stmt, this, decl);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    refs.remove(stmt, decl);
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return SE_NONE;
  }
}
