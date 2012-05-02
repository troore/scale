package scale.score.expr;

import scale.common.*;
import scale.clef.expr.Literal;
import scale.clef.expr.IntArrayLiteral;
import scale.clef.expr.FloatArrayLiteral;
import scale.clef.LiteralMap;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.decl.FieldDecl;
import scale.clef.expr.Expression;
import scale.clef.type.Type;
import scale.clef.type.PointerType;
import scale.clef.type.ArrayType;
import scale.score.*;
import scale.score.analyses.*;
import scale.score.dependence.AffineExpr;
import scale.score.pred.References;

// Don't import scale.score.chords; it causes problems for the java
// compiler.

/**
 * This class represents the indirect load operator.
 * <p>
 * $Id: LoadValueIndirectExpr.java,v 1.91 2007-10-04 19:58:31 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Other loads are handled by other nodes.  Load immediate is handled
 * by {@link LiteralExpr LiteralExpr}, load address is handled by
 * {@link LoadDeclAddressExpr LoadDeclAddressExpr}, load of a value is
 * handled by {@link LoadDeclValueExpr LoadDeclValueExpr}, and load
 * value from address is handled by {@link LoadValueIndirectExpr
 * LoadValueIndirectExpr}.
 * @see LoadDeclAddressExpr
 * @see LoadDeclValueExpr
 * @see LoadValueIndirectExpr
 * @see LiteralExpr
 */
public class LoadValueIndirectExpr extends UnaryExpr
{
  /**
   * may use information - due to aliasing.
   */
  private MayUse mayUse;

  /**
   * Reuse level of the array reference.
   */
  private int reuseLevel = 0;

  /**
   * This method builds a load indirect operation. The type is
   * determined from the argument.
   * @param addr address of value to be loaded
   */
  public LoadValueIndirectExpr(Expr addr)
  {
    super(addr.getCoreType().getPointedTo(), addr);
    this.mayUse = null;
  }

  /**
   * Create an expression representing the load from the specified
   * address.  Use this method when the argument class is not known.
   */
  public static Expr create(Expr addr)
  {
    assert addr.getType().isPointerType() : "Not pointer " + addr;

    if (addr instanceof LoadDeclAddressExpr) {
      Declaration       decl = ((LoadDeclAddressExpr) addr).getDecl();
      LoadDeclValueExpr ldve = new LoadDeclValueExpr(decl);
      return ldve;
    }

    if (addr instanceof LoadFieldAddressExpr) {
      LoadFieldAddressExpr lfae   = (LoadFieldAddressExpr) addr;
      Expr                 struct = lfae.getStructure().copy();
      FieldDecl            field  = lfae.getField();
      LoadFieldValueExpr   lfve   = new LoadFieldValueExpr(struct, field);
      return lfve;
    }

    return new LoadValueIndirectExpr(addr.conditionalCopy());
  }

  /**
   * Return the Expr representing the address loaded by this
   * expression.
   */
  public final Expr getAddr()
  {
    return getArg();
  }
  
  /**
   * Make a copy of this load expression.
   * The use - def information is copied too.
   */
  public Expr copy()
  {
    Expr                  arg    = getArg().copy();
    LoadValueIndirectExpr ld     = new LoadValueIndirectExpr(arg);
    MayUse                mayUse = getMayUse();
    if (mayUse != null)
      mayUse.copy(ld);
    return ld;
  }

  /**
   * Make a copy of this load expression without the use - def
   * information.
   */
  public Expr copyNoUD()
  {
    LoadValueIndirectExpr ld = new LoadValueIndirectExpr(getArg().copy());
    return ld;
   }

  public void visit(Predicate p)
  {
    p.visitLoadValueIndirectExpr(this);
  }

  public String getDisplayLabel()
  {
    return "*";
  }

  /**
   * Return the variable reference for the expression.  We use this
   * to get the variable reference for operations such as array
   * subscript and structure operations.
   */
  public Expr getReference()
  {
    return getArg().getReference();
  }

  /**
   * Add may use information to the load expression.
   * @param mayUse the expresion representing the may use
   */
  public void addMayUse(MayUse mayUse)
  {
    assert ((this.mayUse == mayUse) ||
            (this.mayUse == null) ||
            (mayUse == null)) : "Changing may use.";
    this.mayUse = mayUse;
    if (mayUse != null)
      mayUse.setGraphNode(this);
  }

  /**
   * Return the may use information assocaited with the load.
   */
  public MayUse getMayUse()
  {
    return mayUse;
  }

  /**
   * Remove any inormation such as use - def links, may use links,
   * etc.
   */
  public void removeUseDef()
  {
    super.removeUseDef();
    mayUse = null;
  }

 /**
   * Given a load expression, return the object expression for the
   * load.  This is the converse of <tt>getReference</tt>.  If the
   * expression is the structure or array name which is part of a
   * field reference or array subscript then return the expression
   * representing the field or the subscript.  For a scalar load, the
   * method returns the load.
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
   * Return true if the expression loads a value from memory.
   */
  public boolean isMemRefExpr()
  {
    return true;
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
    MayUse mayUse = getMayUse();
    if ((mayUse != null) && !scale.score.trans.Optimization.unsafe)
      return getChord();

    return getArg().getCriticalChord(lMap, independent);
  }

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    Expr arg = getArg();

    // This is questionable.  The conservative approach would assume
    // that all indirect references were loop variant.  The reason we
    // do this is because of Fortran by-reference argumennts.

    if (arg instanceof LoadDeclValueExpr)
      return loop.isInvariant(arg);

    return false;
  }


  /**
   * Return the {@link SubscriptExpr SubscriptExpr} that this load
   * uses or <code>null</code> if none is found.  This method uses the
   * use-def link to find an existing <code>SubscriptExpr</code
   * instance.
   */
  public SubscriptExpr findSubscriptExpr()
  {
    return getArg().findSubscriptExpr();
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();
    getArg().loopClean();
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates.
   * <p>
   * For a load indirect (e.g., <code>*ptr</code>), we can treat it as
   * optimizatible if the <code>restrict</code> attribute is set as in
   * <pre>
   *   int * restrict ptr;
   * </pre>
   * This allows global value numbering to change <code>(*ptr +
   * *ptr)</code> to <code>(x = *ptr, (x + x))</code>.
   */
  public boolean optimizationCandidate()
  {
    Expr arg = getArg();
    if (arg instanceof LoadDeclValueExpr) {
      VariableDecl vd = (VariableDecl) ((LoadDeclValueExpr) arg).getDecl();
      return vd.isRestricted() && vd.optimizationCandidate();
    }
    return false;
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
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 5 + getArg().executionCostEstimate();
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    return super.sideEffects() | SE_DOMAIN;
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   * @see scale.clef.decl.ValueDecl
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return r;

    Literal rr  = Lattice.Bot;
    Expr    arg = getArg().getLow();

    while (arg.isCast())
      arg = arg.getOperand(0);

    if (arg instanceof LoadDeclAddressExpr) {
      VariableDecl vd = ((LoadDeclAddressExpr) arg).getDecl().returnVariableDecl();
      if ((vd != null) && vd.isConst())
        rr = vd.getConstantValue();
    } else if (arg instanceof ArrayIndexExpr) {
      ArrayIndexExpr aie   = (ArrayIndexExpr) arg;
      Expr           array = aie.getArray();

      while (array.isCast())
        array = array.getOperand(0);

      if (array instanceof LoadDeclAddressExpr) {
        VariableDecl vd = ((LoadDeclAddressExpr) array).getDecl().returnVariableDecl();
        ArrayType    at = vd.getCoreType().returnArrayType();
        if ((at != null) && at.getElementType().isConst()) {
          Literal offset = aie.getOffset().getConstantValue(cvMap);
          Literal index  = aie.getIndex().getConstantValue(cvMap);
          try {
            long       off = Lattice.convertToLongValue(offset);
            long       ind = Lattice.convertToLongValue(index);
            long       x   = off + ind;
            Expression val = vd.getValue();
            if (val != null)
              return vd.getValue().getConstantValue().getElement(x);
          } catch (InvalidException ex) {
          }
        }
      }
    }

    cvMap.put(this, rr);
    return rr;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   * @see scale.clef.decl.ValueDecl
   */
  public Literal getConstantValue()
  {
    Expr arg = getArg().getLow();

    while (arg.isCast())
      arg = arg.getOperand(0);

    if (arg instanceof LoadDeclAddressExpr) {
      VariableDecl vd = ((LoadDeclAddressExpr) arg).getDecl().returnVariableDecl();
      if ((vd != null) && vd.isConst())
        return vd.getConstantValue();
    } else if (arg instanceof ArrayIndexExpr) {
      ArrayIndexExpr aie   = (ArrayIndexExpr) arg;
      Expr           array = aie.getArray();

      while (array.isCast())
        array = array.getOperand(0);

      if (array instanceof LoadDeclAddressExpr) {
        VariableDecl vd = ((LoadDeclAddressExpr) array).getDecl().returnVariableDecl();
        ArrayType    at = vd.getCoreType().returnArrayType();
        if ((vd != null) && at.getElementType().isConst()) {
          Literal offset = aie.getOffset().getConstantValue();
          Literal index  = aie.getIndex().getConstantValue();
          try {
            long off = Lattice.convertToLongValue(offset);
            long ind = Lattice.convertToLongValue(index);
            long x   = off + ind;
            return vd.getValue().getConstantValue().getElement(x);
          } catch (InvalidException ex) {
          }
        }
      }
    }

    return Lattice.Bot;
  }
}
