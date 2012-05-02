package scale.score.chords;

import scale.common.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.score.expr.Expr;
import scale.score.expr.LoadExpr;
import scale.score.analyses.MayDef;
import scale.score.expr.LoadDeclAddressExpr;
import scale.score.Predicate;
import scale.score.pred.References;
import scale.clef.decl.Declaration;

/** 
 * This class is used to represent an assignment operation in a CFG.
 * The value may be assigned to the bit-bucket.
 * <p>
 * $Id: ExprChord.java,v 1.66 2007-10-17 13:39:58 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * If the left-hand-side of the <code>ExprChord</code> instance is not
 * <code>null</code> it is always an expression that evaluates to an
 * address.  It is usually a {@link
 * scale.score.expr.LoadDeclAddressExpr LoadDeclAddressExpr} instance
 * but may be a {@link scale.score.expr.LoadDeclValueExpr
 * LoadDeclValueExpr} instance.

 */

public class ExprChord extends SequentialChord
{
  /**
   * This field holds the target of the assignment or <code>null</code>.
   */
  private Expr lhs = null;  
  /**
   * This field holds the source expression for the assignment or <code>null</code>.
   */
  private Expr rhs = null;  

  /**
   * The predicate that determines if the right hand side is evaluated and the 
   * result stored in the target.
   */
  private Expr predicate;

  /**
   * A def->use edge (Expr) or a set of def->use edges (vector).
   */
  private Object defUse; 
  /**
   *  The MayDef info represents an assignment that indirectly may change the
   * value of a variable.  However, it may not, which means that the old
   * defintion is still valid.
   */
  private MayDef mayDef; 

  /**
   * The store is enabled if onTrue is true and the predicate value is non-zero
   * or onTrue is false and the predicate value is zero.
   */
  private boolean onTrue;
  /**
   * The operation is a va_copy()..
   */
  private boolean vaCopy;

  /**
   * Create a node that holds a computation.
   * @param lhs is the target of the assignment or <code>null</code>
   * @param rhs is the source expression for the assignment or <code>null</code>
   * @param next is the out-going CFG edge and may be null
   */
  public ExprChord(Expr lhs, Expr rhs, Chord next)
  {
    super(next);
    this.lhs = lhs;
    if (lhs != null)
      lhs.setOutDataEdge(this);
    this.rhs = rhs;
    if (rhs != null)
      rhs.setOutDataEdge(this);
    this.defUse = null;
    this.mayDef = null;
    this.predicate = null;
    this.onTrue = false;
  }

  /**
   * Create a node that holds a computation.
   * @param rhs is the source expression or <code>null</code>
   * @param next is the out-going CFG edge and may be null
   */
  public ExprChord(Expr rhs, Chord next)
  {
    this(null, rhs, next);
  }

  /**
   * Create a node that holds a computation.
   * @param rhs is the source expression or <code>null</code>
   */
  public ExprChord(Expr rhs)
  {
    this(null, rhs, null);
  }

  /**
   * Create a node that holds a computation.
   * @param lhs is the target of the assignment or <code>null</code>
   * @param rhs is the source expression for the assignment or <code>null</code>
   */
  public ExprChord(Expr lhs, Expr rhs)
  {
    this(lhs, rhs, null);
  }

  /**
   * Create a node that holds a computation.
   * @param lhs is the target of the assignment or <code>null</code>
   * @param rhs is the source expression for the assignment or <code>null</code>
   * @param predicate pecifies the predicate expression
   * @param onTrue is true if the operation is enabled if onTrue is true and the 
   * predicate value is non-zero or onTrue is false and the predicate value is zero
   */
  public ExprChord(Expr lhs, Expr rhs, Expr predicate, boolean onTrue)
  {
    this(lhs, rhs, null);
    this.predicate = predicate;
    this.onTrue = onTrue;
    predicate.setOutDataEdge(this); // Add other side of data edges.
  }

  /**
   * Return true if this is an expression chord.
   */
  public final boolean isExprChord()
  {
    return true;
  }

  /**
   * Use this method when you may be modifying an in-coming data edge
   * to this expression while iterating over the in-coming edges.
   * @return an array of in-coming data edges.  
   */
  public final Expr[] getInDataEdgeArray()
  {
    int n = 0;
    if (lhs != null)
      n++;
    if (rhs != null)
      n++;

    int i = 0;
    Expr[] array = new Expr[n];
    if (lhs != null)
      array[i++] = lhs;
    if (rhs != null)
      array[i++] = rhs;

    return array;
  }

  /**
   * Remove all the in-coming data edges.
   */
  public final void deleteInDataEdges()
  {
    if (lhs != null) {
      lhs.deleteOutDataEdge(this);
      lhs.unlinkExpression();
      lhs = null;
    }
    if (rhs != null) {
      rhs.deleteOutDataEdge(this);
      rhs.unlinkExpression();
      rhs = null;
    }

    if (defUse == null)
      return;

    if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoadExpr> v = (Vector<LoadExpr>) defUse;
      int    l = v.size();

      defUse = null; // Must be nulled before call to setUseDef().

      for (int i = 0; i < l; i++) {
        LoadExpr exp = v.elementAt(i);
        exp.setUseDef(null);
      }

      return;
    }

    LoadExpr exp = (LoadExpr) defUse;
    exp.setUseDef(null);
    defUse = null;
  }

  /**
   * Return the specified in-coming data edge.
   */
  public final scale.score.expr.Expr getInDataEdge(int i)
  {
    Expr res = null;
    if (i == 0) {
      if (lhs != null)
        res = lhs;
      else if (rhs != null)
        res = rhs;
      else if (predicate != null)
        res = predicate;
    } else if (i == 1) {
      if (rhs != null)
        res = rhs;
      else if (predicate != null)
        res = predicate;
    } else if (i == 2)
      res = predicate;

    if (res != null)
      return res;

    throw new scale.common.InternalError("No incoming data edge (" + i + ") " + this);
  }

  /**
   * Return the number of in-coming data edges.
   */
  public final int numInDataEdges()
  {
    int n = 0;
    if (lhs != null)
      n++;
    if (rhs != null)
      n++;
    if (predicate != null)
      n++;
    return n;
  }

  /**
   * Push all incoming data edges on the stack.
   */
  public void pushInDataEdges(Stack<Expr> wl)
  {
    if (lhs != null)
      wl.push(lhs);
    if (rhs != null)
      wl.push(rhs);;
    if (predicate != null)
      wl.push(predicate);
  }

  public Chord copy()
  {
    ExprChord ec = null;

    if (lhs != null) {
      if (rhs != null)
        ec = new ExprChord(lhs.copy(), rhs.copy(), getNextChord());
    } else if (rhs != null)
      ec = new ExprChord(null, rhs.copy(), getNextChord());

    if (ec == null)
      ec = new ExprChord(null, null, getNextChord());

    ec.copySourceLine(this);

    if (mayDef != null) {
      MayDef md = mayDef.copy();
      ec.mayDef = md;
      md.setGraphNode(ec);
    }

    if (predicate != null) {
      ec.predicate = predicate.copy();
      ec.onTrue = onTrue;
    }
    
    ec.vaCopy = vaCopy;

    return ec;
  }

  /**
   * Return a String containing additional information about this
   * class instance.
   */
  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    if (lhs != null) {
      buf.append(lhs);
      buf.append(' ');
    }
    buf.append(rhs);
    return buf.toString();
  }

  /**
   * Return true if this node holds an expression that represent an
   * assignment statement.
   */
  public final boolean isAssignChord()
  {
    return (lhs != null);
  }

  /**
   * Set the lvalue.
   */
  public final void setLValue(Expr lhs)
  {
    if (this.lhs != null)
      this.lhs.deleteOutDataEdge(this);
    this.lhs = lhs;
  }

  /**
   * Set the rvalue.
   */
  public final void setRValue(Expr rhs)
  {
    if (this.rhs != null)
      this.rhs.deleteOutDataEdge(this);
    this.rhs = rhs;
  }

  /**
   * Return the lvalue if this node holds an expression that is an
   * assignment statement or <code>null</code>.
   */
  public final Expr getLValue()
  {
    return lhs;
  }

  /**
   * Return the rvalue or <code>null</code>.
   */
  public final Expr getRValue()
  {
    return rhs;
  }

  /** 
   * This method changes an incoming data edge to point to a new
   * expression.
   * <p>
   * This method ensures that the node previously pointing to
   * this one is updated properly, as well as, the node which will now
   * point to this node.
   * <p>
   * Expr and Chord nodes have a fixed number of incoming
   * edges with specific meaning applied to each.
   * @param oldExpr is the expression to be replaced
   * @param newExpr is the new expression
   */
  public final void changeInDataEdge(scale.score.expr.Expr oldExpr,
                                     scale.score.expr.Expr newExpr)
  {
    assert (newExpr != null) : "ExprChord requires an incoming data edge.";

    if (oldExpr == lhs) {
      if (lhs != null) 
        lhs.deleteOutDataEdge(this);
      lhs = newExpr;
      lhs.setOutDataEdge(this);
      return;
    }

    assert (oldExpr == rhs) : "Old incoming data edge not found " + oldExpr;

    if (rhs != null) 
      rhs.deleteOutDataEdge(this);
    rhs = newExpr;
    rhs.setOutDataEdge(this);
  }

  public void visit(Predicate p)
  {
    p.visitExprChord(this);
  }

  /**
   * Return a vector of all {@link scale.clef.decl.Declaration
   * declarations} referenced in this CFG node or <code>null</code>.
   */
  public final Vector<Declaration> getDeclList()
  {
    if ((lhs == null) && (rhs == null))
      return null;

    Vector<Declaration> varList = new Vector<Declaration>(20);

    if (lhs != null)
      lhs.getDeclList(varList);

    if (rhs != null)
      rhs.getDeclList(varList);

    return varList;
  }

  /**
   * Return a vector of all {@link scale.score.expr.LoadExpr LoadExpr}
   * instances in this CFG node or <code>null</code>.
   */
  public final Vector<LoadExpr> getLoadExprList()
  {
    if ((lhs == null) && (rhs == null))
      return null;

    Vector<LoadExpr> expList = new Vector<LoadExpr>(20);
    if (lhs != null)
      lhs.getLoadExprList(expList);

    if (rhs != null)
      rhs.getLoadExprList(expList);

    return expList;
  }

  /**
   * Return a vector of all {@link scale.score.expr.Expr Expr
   * instances} in this CFG node or <code>null</code>.
   */
  public final Vector<Expr> getExprList()
  {
    if ((lhs == null) && (rhs == null))
      return null;

    Vector<Expr> expList = new Vector<Expr>(20);
    if (lhs != null)
      lhs.getExprList(expList);
    if (rhs != null)
      rhs.getExprList(expList);
    return expList;
  }

  /**
   * Replace all occurrances of a {@link scale.clef.decl.Declaration
   * Declaration} with another {@link scale.clef.decl.Declaration
   * Declaration}.
   * @return true if a replace occurred
   */
  public final boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    boolean f = false;

    if (lhs != null)
      f = lhs.replaceDecl(oldDecl, newDecl);
    if (rhs != null)
      f |= rhs.replaceDecl(oldDecl, newDecl);

    return f;
  }

  /**
   * Return the {@link scale.score.expr.CallExpr call expression} or
   * <code>null</code> if none.
   * @param ignorePure is true if pure function calls are to be ignored.
   */
  public final scale.score.expr.CallExpr getCall(boolean ignorePure)
  {
    if (rhs == null)
      return null;
    return rhs.getCall(ignorePure);
  }

  /**
   * Check this node for validity.  This method throws an error if
   * the node is not linked properly.
   */
  public void validate()
  {
    super.validate();

    if (lhs != null) {
      lhs.validate();
      if (lhs.getOutDataEdge() != this)
        throw new scale.common.InternalError(" expr " + this + " -> " + lhs);
    }
    if (rhs != null) {
      rhs.validate();
      if (rhs.getOutDataEdge() != this)
        throw new scale.common.InternalError(" expr " + this + " -> " + rhs);
    }
    if (predicate != null) {
      predicate.validate();
      if (predicate.getOutDataEdge() != this)
        throw new scale.common.InternalError("One way " + this + " -> " + predicate);
    }

    if (defUse == null)
      return;

    if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoadExpr> v = (Vector<LoadExpr>) defUse;
      int              l = v.size();
      for (int i = 0; i < l; i++)
        validateUseDef(v.elementAt(i));
      return;
    }

    validateUseDef((LoadExpr) defUse);
  }

  /**
   * Record any variable references in this CFG node from the {@link
   * scale.score.pred.References table of references}.
   */
  public void recordRefs(References refs)
  {
    if (lhs != null) {
      Expr l = lhs;
      while (l instanceof scale.score.expr.ConversionExpr)
        l = ((scale.score.expr.ConversionExpr) l).getArg();

      if (l instanceof LoadDeclAddressExpr) {
        LoadDeclAddressExpr loada = (LoadDeclAddressExpr) l;
        Declaration         decl  = loada.getDecl();
        Expr                obj   = loada.getObject();

        refs.recordDef(this, obj, decl);
      } else
        l.recordRefs(this, refs);
    }

    if (rhs != null)
      rhs.recordRefs(this, refs);

    if (predicate != null)
      predicate.recordRefs(this, refs);
  }

  /**
   * Remove any variable references in this CFG node in the {@link
   * scale.score.pred.References table of references}.
   */
  public void removeRefs(References refs)
  {
    if (lhs != null)
      lhs.removeRefs(this, refs);

    if (rhs != null)
      rhs.removeRefs(this, refs);

    if (predicate != null)
      predicate.removeRefs(this, refs);
  }

  /**
   * Return a relative cost estimate for executing this CFG node.
   */
  public int executionCostEstimate()
  {
    int  cost = 0;
    if (rhs != null)
      cost = getRValue().executionCostEstimate();

    if (lhs instanceof LoadDeclAddressExpr) {
      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) lhs;
      VariableDecl        vd   = (VariableDecl) ldae.getDecl();
      if (vd.shouldBeInRegister())
        return cost;
    }

    return 5 + cost;
  }

  /**
   * Return an indication of the side effects execution of this CFG
   * node may cause.
   * @see scale.score.expr.Expr#SE_NONE
   */
  public int sideEffects()
  {
    int side = Expr.SE_NONE;
    if (rhs != null)
      side = rhs.sideEffects();

    if (lhs instanceof LoadDeclAddressExpr) {
      Declaration d = ((LoadDeclAddressExpr) lhs).getDecl();
      if (d.isTemporary())
        return side;
    }

    return side | Expr.SE_STATE;
  }

  /**
   * Add a new link from the definition to the use.
   * This method DOES NOT update the use to def link in the use.
   * @param expr the new use-def link
   */
  public final void addDefUse(LoadExpr expr)
  {
    if (expr == null)
      return;

    if (defUse == null)
      defUse = expr;
    else if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoadExpr> du = (Vector<LoadExpr>) defUse;
      du.addElement(expr);
    } else {
      Vector<Expr> v = new Vector<Expr>(2);
      v.addElement((LoadExpr) defUse);
      v.addElement(expr);
      defUse = v;
    }
  }

  /**
   * Remove an existing link from the definition to the use.
   * This method DOES NOT update the use to def link in the use.
   * @param expr the existing use-def link
   */
  public final void removeDefUse(LoadExpr expr)
  {
    if (defUse == expr)
      defUse = null;
    else if (defUse instanceof Vector)
      ((Vector) defUse).removeElement(expr);
  }

  /**
   * Return the single def-use link or null if there are more than one
   * or none.
   */
  public final Expr singleDefUse()
  {
    if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoadExpr> duv = (Vector<LoadExpr>) defUse;
      if (duv.size() == 1)
        return duv.elementAt(0);
      return null;
    }

    return (Expr) defUse;
  }

  /**
   * Return the number of def-use links.
   */
  public int numDefUseLinks()
  {
    if (defUse == null)
      return 0;

    if (defUse instanceof Vector)
      return ((Vector) defUse).size();

    return 1;
  }

  /**
   * Return the i-th def-use link.  Use {@link
   * scale.score.chords.ExprChord#getDefUseArray getDefUseArray} if
   * the def-use links will be modified while iterating over them.
   */
  public final LoadExpr getDefUse(int i)
  {
    assert (defUse != null) : "Incorrect def-use request " + i;

    if (defUse instanceof Vector)
      return (LoadExpr) ((Vector) defUse).elementAt(i);

    assert (i == 0) : "Incorrect def-use request " + i;

    return (LoadExpr) defUse;
  }

  /**
   * Return an array of the use-def links.
   */
  public final LoadExpr[] getDefUseArray()
  {
    if (defUse == null)
      return new LoadExpr[0];

    if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoadExpr>   v   = (Vector<LoadExpr>) defUse;
      int                l   = v.size();
      LoadExpr[] a = new LoadExpr[l];
      for (int i = 0; i < l; i++)
        a[i] = v.elementAt(i);
      return a;
    }

    LoadExpr[] a = new LoadExpr[1];
    a[0] = (LoadExpr) defUse;
    return a;
  }

  /**
   * Return true is a def-use link exists.
   */
  public final boolean checkDefUse(Expr use)
  {
    if (defUse == null)
      return false;

    if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoadExpr> v = (Vector<LoadExpr>) defUse;
      int              l = v.size();
      for (int i = 0; i < l; i++)
        if (use == v.elementAt(i))
          return true;
      return false;
    }

    return (defUse == use);
  }

  /**
   * Add may definition information to the store.
   * @param mayDef the may definition info.
   */
  public void addMayDef(MayDef mayDef)
  {
    assert ((this.mayDef == null) || (mayDef == null) || (this.mayDef == mayDef)) :
      "Resetting may-def information " + this;

    this.mayDef = mayDef;
    if (mayDef != null)
      this.mayDef.setGraphNode(this);
  }

  /**
   * Return the may definition info assciated with the store.
   */
  public MayDef getMayDef()
  {
    return mayDef;
  }

  /**
   * Remove any information such as use - def links, may use links, etc.
   */
  public void removeUseDef()
  {
    if (lhs != null)
      lhs.removeUseDef();
    if (rhs != null)
      rhs.removeUseDef();
    if (predicate != null)
      predicate.removeUseDef();
    defUse = null;
    mayDef = null;
  }

  /**
   * The expression is a defined if it is on the left hand side of
   * the store expression.
   * @param lv the expression we're checking 
   * @return true if lv is an lvalue, otherwise return false
   */
  public boolean isDefined(Expr lv)
  {
    boolean d = (lhs == lv);
    return d;
  }

  /**
   * If this CFG node results in a variable being given a new value,
   * return the {@link scale.score.expr.Expr Expr} instance that
   * specifies the variable.
   * @return a null or a LoadExpr that defines a variable
   */
  public Expr getDefExpr()
  {
    if (lhs == null)
      return null;

    return lhs.getReference();
  }

  private void validateUseDef(LoadExpr le)
  {
    ExprChord ud = le.getUseDef();
    if (ud != this)
      throw new scale.common.InternalError("Wrong use-def link: " +
                                           this +
                                           " -> " +
                                           le + 
                                           " -> " +
                                           ud);

    if (le.getChord() == null)
      throw new scale.common.InternalError("No chord for " +
                                           le +
                                           " <- " +
                                           this);

    Expr lhs = getLValue();
    if (!(lhs instanceof LoadDeclAddressExpr))
      return;

    if (((LoadExpr) lhs).getDecl() != le.getDecl())
      throw new scale.common.InternalError("Use-def decls different: " +
                                           this +
                                           " -> " +
                                           le +
                                           " -> " +
                                           ud);
  }

  /**
   * Display the def-use links.
   */
  public void printDefUse()
  {
    System.out.println("** def-use for " + this);
    if (defUse instanceof Vector) {
      @SuppressWarnings("unchecked")
      Vector<LoadExpr> v = (Vector<LoadExpr>) defUse;
      for (int i = 0; i < v.size(); i++) {
        Expr exp = v.elementAt(i);
        System.out.print("   " + exp + " -> ");
        if (exp != null)
          System.out.println("   " + exp.getChord());
        System.out.println("");
      }
      return;
    }

    Expr exp = (Expr) defUse;
    System.out.print("   " + exp + " -> ");
    if (exp != null)
      System.out.println("   " + exp.getChord());
    System.out.println("");
    System.out.println("   " + defUse);
    return;
  }

  /**
   * Return the predicate expression or null if the store is not predicated.
   */
  public final Expr getPredicate()
  {
    return predicate;
  }

  /**
   * Change this predicated store into a normal store.
   */
  public final Expr removePredication()
  {
    if (predicate != null)
      predicate.deleteOutDataEdge(this);
    Expr old = predicate;
    predicate = null;
    return old;
  }

  /**
   * Specify predicate for this operation.
   */
  public final void specifyPredicate(Expr predicate, boolean onTrue)
  {
    assert (this.predicate == null) : "Can't predicate twice " + this;

    this.predicate = predicate;
    this.onTrue = onTrue;
    predicate.setOutDataEdge(this);
  }

  /**
   * Return true if the store is enabled when the predicate value is
   * non-zero.  Return false if the store is enabled when the
   * predicate value is zero.
   */
  public final boolean predicatedOnTrue()
  {
    return onTrue;
  }

  /**
   * Mark this store as a special case - va_copy().
   */
  public final void setVaCopy()
  {
    vaCopy = true;
  }

  /**
   * Is this store as a special case - va_copy()?
   */
  public final boolean isVaCopy()
  {
    return vaCopy;
  }

  /**
   * Remove all {@link scale.score.expr.DualExpr DualExpr} instances
   * from this CFG node.  Use the lower form.  This eliminates
   * references to variables that may no longer be needed.
   */
  public boolean removeDualExprs()
  {
    boolean found = false;
    if (rhs != null)
      found |= rhs.removeDualExprs();
    if (lhs != null)
      found |= lhs.removeDualExprs();
    if (predicate != null)
      found |= predicate.removeDualExprs();
    return found;
  }
}
