package scale.score.expr;

import java.util.AbstractCollection;
import java.util.Iterator;

import scale.common.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.expr.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;

import scale.score.*;
import scale.score.dependence.*;
import scale.score.analyses.AliasAnnote;
import scale.score.pred.References;

/**
 * This class represents a subscript operation which computes an
 * address of an array element (at a high-level).
 * <p>
 * $Id: SubscriptExpr.java,v 1.140 2007-10-17 13:40:01 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Regardless of whether the source language uses row major or column
 * major array ordering, the subscripts represented by this expression are
 * always in row major (C style) ordering.
 */
public class SubscriptExpr extends Expr
{
  /**
   * The array address expression.
   */
  private Expr array;
  /**
   * The array index expressions.
   */
  private Expr[] subscripts;
  /**
   * The array index origin expressions.
   */
  private Expr[] mins;
  /**
   * The array index size expressions.
   */
  private Expr[] sizes;
  /**
   * @param type must be a PointerType
   * @param array is the expression node holding address of array
   * being subscripted
   * @param subscripts is the vector of expressions representing array
   * subscripts
   * @param mins is the vector of expressions represnting the index
   * origin for each dimension
   * @param sizes is the vector of expressions representing the size
   * for each dimension
   */
  public SubscriptExpr(Type   type,
                       Expr   array,
                       Expr[] subscripts,
                       Expr[] mins,
                       Expr[] sizes)
  {
    super(type);

    assert type.getCoreType().isPointerType() : "Not pointer type - " + type;

    int n = subscripts.length;
    assert ((n > 0) && (n == mins.length) && (n == sizes.length)) :
      "Invalid SubscriptExpr constructor.";

    this.array       = array;
    this.subscripts  = subscripts;
    this.mins        = mins;
    this.sizes       = sizes;

    array.setOutDataEdge(this);
    for (int i = 0; i < n; i++) {
      subscripts[i].setOutDataEdge(this); // Add other side of data edges.
      mins[i].setOutDataEdge(this); // Add other side of data edges.
      sizes[i].setOutDataEdge(this); // Add other side of data edges.
    }
  }

  public Expr copy()
  {
    int    len  = subscripts.length;
    Expr[] newI = new Expr[len];
    Expr[] newM = new Expr[len];
    Expr[] newS = new Expr[len];

    for (int i = 0; i < len; i++) {
      newI[i] = subscripts[i].copy();
      newM[i] = mins[i].copy();
      newS[i] = sizes[i].copy();
    }

    return new SubscriptExpr(getType(), array.copy(), newI, newM, newS);
  }

  /**
   * Return the expression specifying the array.
   */
  public Expr getArray() 
  {
    return array;
  }

  /**
   * Return the number of subscripts to the array.
   */
  public int numSubscripts()
  {
    if (subscripts == null)
      return 0;
    return subscripts.length;
  }

  /**
   * Return the expression representing the i-th subscript.
   */
  public Expr getSubscript(int i) 
  {
    return subscripts[i];
  }

  /**
   * Return the expression representing the index origin of the i-th
   * subscript.
   */
  public Expr getIndexOrigin(int i) 
  {
    return mins[i];
  }

  /**
   * Return the expression representing the dimension of the i-th
   * subscript.
   */
  public Expr getDimension(int i) 
  {
    return sizes[i];
  }

  public void visit(Predicate p)
  {
    p.visitSubscriptExpr(this);
  }

  public String getDisplayLabel()
  {
    StringBuffer s = new StringBuffer();

    if (array instanceof LoadDeclAddressExpr) {
      LoadDeclAddressExpr ldae = (LoadDeclAddressExpr) array;
      s.append(ldae.getDecl().getName());
    } else
      s.append(array.getDisplayLabel());

    s.append('[');

    int numInd = numSubscripts();
    for (int i = 0; i < numInd; i++) {
      Expr subs = getSubscript(i);
      if (i > 0)
        s.append(", ");
      s.append(subs.getDisplayLabel());
    }

    s.append(']');

    return s.toString();
  }

  /**
   * The given expression is defined if the SubscriptExpr expression
   * is defined and the given expression is the array.
   * @param lv the expression reprsenting the array name
   * @return true if the array is defined.
   */
  public boolean isDefined(Expr lv)
  {
    boolean d = this.isDefined();
    boolean l = (array == lv);
    return d && l;
  }

  /**
   * Return the array associated with the subscript expression. We
   * use the array name to represent the access of the array
   * - instead of the array index value.
   */
  public Expr getReference()
  {
    return array.getReference();
  }

  /**
   * Return an array of the operands to the expression.
   */
  public final Expr[] getOperandArray()
  {
    int    len   = subscripts.length;
    Expr[] edges = new Expr[len * 3 + 1];
    int    k     = 1;

    edges[0] = array;

    for (int i = 0; i < len; i++) {
      edges[k + 0] = subscripts[i];
      edges[k + 1] = mins[i];
      edges[k + 2] = sizes[i];
      k += 3;
    }

    return edges;
  }

  /**
   * Set the array expression to this expression.
   */
  public void setArray(Expr array)
  {
    if (this.array != null) {
      this.array.deleteOutDataEdge(this);
      this.array.unlinkExpression();
    }
    this.array = array;
    array.setOutDataEdge(this);
  }

  /**
   * Set the size of the specified dimension.
   * @param size is the new dimension size
   * @param i specifies the dimension
   */
  public void setDimension(Expr size, int i)
  {
    Expr oldSize = sizes[i];
    if (oldSize != null) {
      oldSize.deleteOutDataEdge(this);
      oldSize.unlinkExpression();
    }
    sizes[i] = size;
    size.setOutDataEdge(this);
  }

  /**
   * Set the index into the specified dimension.
   * @param index is the new dimension index
   * @param i specifies the dimension
   */
  public void setSubscripts(Expr index, int i)
  {
    Expr oldIndex = subscripts[i];
    if (oldIndex != null) {
      oldIndex.deleteOutDataEdge(this);
      oldIndex.unlinkExpression();
    }
    subscripts[i] = index;
    index.setOutDataEdge(this);
  }

  protected void setOperand(Expr operand)
  {
    throw new scale.common.InternalError("SetOperand(Expr) may not be used on SubscriptExpr.");
  }

  public Expr setOperand(Expr operand, int position)
  {
    Expr op = null;

    if (position == 0) {
      op    = array;
      array = operand;
    } else {

      position--;

      int k = position / 3;
      int i = position % 3;

      switch (i) {
      case 0: op = subscripts[k]; subscripts[k] = operand; break;
      case 1: op = mins[k];    mins[k]    = operand; break;
      case 2: op = sizes[k];   sizes[k]   = operand; break;
      }
    }

    if (op != null)
      op.deleteOutDataEdge(this);

    if (operand != null)
      operand.setOutDataEdge(this); // Add other side of data edges.

    return op;
  }

  /**
   * Return the number of operands to this expression.
   */
  public int numOperands()
  {
    return subscripts.length * 3 + 1;
  }

  /**
   * Return the nth operand.
   * @param position the index of the operand
   */
  public final Expr getOperand(int position)
  {
    if (position == 0)
      return array;

    position--;

    int k = position / 3;
    int i = position % 3;
    switch (i) {
    case 0:  return subscripts[k];
    case 1:  return mins[k];
    case 2:  return sizes[k];
    }
    return null;
  }

  /** 
   * Allow some expressions such as VectorExpr to remove an operand.
   */
  protected void removeOperand(int i)
  {
    throw new scale.common.InternalError("removeOperand(int) may not be used on SubscriptExpr.");
  }

  /**
   *  Return a new vector with copies of the expression arguments.
   */
  public Vector<Expr> getArguments()
  {
    int          len  = subscripts.length;
    Vector<Expr> args = new Vector<Expr>(len * 3 + 1);

    args.addElement(array);

    for (int i = 0; i < len; i++) {
      args.addElement(subscripts[i].copy());
      args.addElement(mins[i].copy());
      args.addElement(sizes[i].copy());
    }

    return args;
  }

  /**
   * Return true if the expressions are equivalent.  Assume subscripts
   * are AffineExpr expressions.  This method should be called by the
   * equivalentImp() method of any derived classes.
   */
  public boolean equivalentImp(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;

    SubscriptExpr o = (SubscriptExpr) exp;

    if (!array.equivalent(o.array))
      return false;

    int n = subscripts.length;
    if (n != o.subscripts.length)
      return false;

    scale.score.chords.LoopHeaderChord oloop = o.getLoopHeader().getTopLoop();
    scale.score.chords.LoopHeaderChord tloop = getLoopHeader().getTopLoop();
    for (int i = 0; i < n; i++) {
      AffineExpr arrayexprs1 = tloop.isAffine(subscripts[i]);
      AffineExpr arrayexprs2 = oloop.isAffine(o.subscripts[i]);

      if (!arrayexprs1.equivalent(arrayexprs2))
        return false;

      if (!mins[i].equivalent(o.mins[i]))
        return false;

      if (!sizes[i].equivalent(o.sizes[i]))
        return false;
    }

    return true;
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the equivalent() method of the derived
   * classes.
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;

    SubscriptExpr o = (SubscriptExpr) exp;

    if (!array.equivalent(o.array))
      return false;

    int n = subscripts.length;
    if (n != o.subscripts.length)
      return false;

    for (int i = 0; i < n; i++) {
      if (!subscripts[i].equivalent(o.subscripts[i]))
        return false;

      if (!mins[i].equivalent(o.mins[i]))
        return false;

      if (!sizes[i].equivalent(o.sizes[i]))
        return false;
    }

    return true;
  }

  /**
   * Return an enumeration of the subscripts of a subscript operator.
   */
  public Expr[] getSubscripts()
  { 
    return subscripts;
  }

  /**
   * Add this <code>SubscriptExpr</code> to the table if it has a
   * valid array reference.  A valid subscript expression is one that
   * dependence testing can utilize.  Examples of invalid subscript
   * expressions include
   * <pre>
   *   (func(a,b))[i]
   *   strct.arr[i]
   * </pre>
   * @return true if valid use
   */
  public boolean addUse(Table<Declaration, SubscriptExpr> arraySubscripts)
  {
    // Find the array expression.

    Expr a = array;
    while (a.isCast())
      a = a.getOperand(0);
 
    if (!(a instanceof LoadExpr)) {
      while (!a.isMemRefExpr() && (a.numInDataEdges() > 0))
        a = a.getInDataEdge(0);

      if (!(a instanceof LoadExpr))
        return false;

      Declaration decl  = ((LoadExpr) a).getDecl();

      if (!decl.isVariableDecl())
        return false;

      ((VariableDecl) decl).setInvalidArrayReferences();
      arraySubscripts.remove(decl.getName());
      return false;
    }

    // Record each use of the array subscript.

    Declaration decl = ((LoadExpr) a).getDecl();
    if (!decl.isVariableDecl())
      return false;

    VariableDecl vd = (VariableDecl) decl;
    if (vd.hasInvalidArrayReferences())
      return false;

    arraySubscripts.add(decl, this);
    return true;
  }

  /**
   * Add all the uses of the address, specified by this
   * <code>SubscriptExpr</code> instance, to the <code>Vector</code>.
   * The uses are obtained by following the use-def links.
   * @see #isUse
   */
  public void addUses(AbstractCollection<Note> uses)
  {
    addUses(uses, null);
  }

  /**
   * Add all the uses of the address, specified by this
   * <code>SubscriptExpr</code> instance, to the <code>Vector</code>.
   * The CFG nodes of uses must be in the dominated list.
   * The uses are obtained by following the use-def links.
   * @see #isUse
   */
  public void addUses(AbstractCollection<Note> uses, Vector<scale.score.chords.Chord> dominated)
  {
    Stack<Expr>   wl   = WorkArea.<Expr>getStack("addUses");
    HashSet<Note> done = WorkArea.<Note>getSet("addUses");

    wl.push(this);

    while (!wl.empty()) {
      Expr exp = wl.pop();

      Note d = exp.getOutDataEdge().getEssentialUse();
      if (d instanceof PhiExpr)
        continue;

      if ((d instanceof Expr) || (exp.isDefined())) {
        if ((dominated == null) || dominated.contains(exp.getChord()))
          uses.add(d);
        continue;
      }

      if (d instanceof scale.score.chords.ExprChord) {
        scale.score.chords.ExprChord se = (scale.score.chords.ExprChord) d;
        int ndu = se.numDefUseLinks();

        for (int id = 0; id < ndu; id++) {
          LoadExpr use = se.getDefUse(id);
          wl.push(use);
        }
      }
    }

    WorkArea.<Expr>returnStack(wl);
    WorkArea.<Note>returnSet(done);
  }

  /**
   * Return true if the argument is a use of the
   * <code>SubscriptExpr</code> instance address.
   * @see #addUses
   */
  public boolean isUse(Note arg)
  {
    if (this == arg)
      return true;

    Note dd = getOutDataEdge().getEssentialUse();
    if (dd == arg)
      return true;

    Stack<Expr>   wl   = WorkArea.<Expr>getStack("isUse");
    HashSet<Expr> done = WorkArea.<Expr>getSet("isUse");

    wl.push(this);

    while (!wl.empty()) {
      Expr exp = wl.pop();
      Note d   = exp.getOutDataEdge().getEssentialUse();

      if (d == arg) {
        WorkArea.<Expr>returnStack(wl);
        WorkArea.<Expr>returnSet(done);
        return true;
      }

      if (d instanceof PhiExpr) {
        PhiExpr pe = (PhiExpr) d;
        if (done.add(pe))
          wl.push(pe);
        continue;
      }

      if ((d instanceof Expr) || (exp.isDefined()))
        continue;

      scale.score.chords.ExprChord se = (scale.score.chords.ExprChord) d;
      int ndu = se.numDefUseLinks();
      for (int id = 0; id < ndu; id++) {
        LoadExpr use = se.getDefUse(id);
        wl.push(use);
      }
    }

    WorkArea.<Expr>returnStack(wl);
    WorkArea.<Expr>returnSet(done);
    return false;
  }

  public String toStringSpecial()
  {
    StringBuffer s = new StringBuffer(getType().toString());
    s.append(' ');
    s.append(array);
    s.append(' ');

    s.append('[');

    int numInd = numSubscripts();
    for (int i = 0; i < numInd; i++) {
      Expr sub = getSubscript(i);
      if (i > 0)
        s.append(',');
      s.append(getStr(sub));
      s.append('(');

      s.append(getStr(mins[i]));
      s.append(',');
      s.append(getStr(sizes[i]));
      s.append(')');
    }
    
    s.append(']');
    return s.toString();
  }

  private String getStr(Expr x)
  {
    if (x instanceof LiteralExpr) {
      Literal lit = ((LiteralExpr) x).getLiteral();
      return lit.getGenericValue();
    }

    return x.toString();
  }

  /**
   * Return loops whose indexes this subscript expression is using.
   * No assumption should be made concerning the order that the loops
   * are placed in the result.
   */
  public Vector<scale.score.chords.LoopHeaderChord> allRelatedLoops()
  { 
    Vector<scale.score.chords.LoopHeaderChord> loops = new Vector<scale.score.chords.LoopHeaderChord>();
    Expr[] ind   = getSubscripts();
    int    index = 0;

    scale.score.chords.LoopHeaderChord tloop = getLoopHeader().getTopLoop();

    for (int k = 0; k < ind.length; k++) { // Enumerate all subscripts
      Expr       sub = ind[k];  // Next index expression
      AffineExpr ae  = tloop.isAffine(sub);  // Create affine expression for that index.

      if (ae == null) // This index is not affine, skip it.
        continue;

      int numTerms = ae.numTerms();
      for (int i = 0; i < numTerms; i++) {
        if (ae.getCoefficient(i) == 0)  
          continue;
        
        VariableDecl indexVar = ae.getVariable(i);
        if (indexVar == null) // constant term       
          continue;

        scale.score.chords.LoopHeaderChord loop = ae.getLoop(i);
        if (loop.isLoopIndex(indexVar)) {
          if (!loops.contains(loop))
            loops.addElement(loop);
        }
      }
    }
    
    return loops;
  }

  private Expr lowerIndexOffset(boolean justIndex)
  {
    IntegerType longType = Machine.currentMachine.getIntegerCalcType(); // 32-bit signed integer

    int  ns     = subscripts.length;
    Expr elecnt = subscripts[0].copy();
    Expr offset = mins[0].copy();

    boolean flg = false;
    for (int k = 1; k < ns; k++) {
      Expr size  = sizes[k];
      if (!size.isLiteralExpr()) {
        flg = true;
        break;
      }
    }

    if (flg && !((offset.isLiteralExpr()) && ((LiteralExpr) offset).isZero())) {
      elecnt = SubtractionExpr.create(longType, elecnt, offset);
      offset = new LiteralExpr(LiteralMap.put(0, longType));
    }

    for (int k = 1; k < ns; k++) {
      Expr index = subscripts[k];
      Expr min   = mins[k];
      Expr size  = sizes[k];

      if (!flg) {
        if (!(size.isLiteralExpr() && ((LiteralExpr) size).isOne())) {
          offset = MultiplicationExpr.create(longType, offset, size.copy());
          elecnt = MultiplicationExpr.create(longType, elecnt, size.copy());
        }

        if (!(min.isLiteralExpr() && ((LiteralExpr) min).isZero()))
          offset = AdditionExpr.create(longType, offset, min.copy());
        offset = offset.reduce();

        if (!(index.isLiteralExpr() && ((LiteralExpr) index).isZero()))
          elecnt = AdditionExpr.create(longType, elecnt, index.copy());
        elecnt = elecnt.reduce();

        continue;
      }

      elecnt = MultiplicationExpr.create(longType, elecnt, size.copy());

      if (!(min.isLiteralExpr() && ((LiteralExpr) min).isZero()))
        index = SubtractionExpr.create(longType, index.copy(), min.copy());
      index = index.reduce();

      if (!(index.isLiteralExpr() && ((LiteralExpr) index).isZero()))
        elecnt = AdditionExpr.create(longType, elecnt, index.copy());
      elecnt = elecnt.reduce();
    }

    if (elecnt.getCoreType().isSigned()) {
      if (elecnt instanceof AdditionExpr) {
        AdditionExpr ae = (AdditionExpr) elecnt;
        Expr         la = ae.getLeftArg();
        Expr         ra = ae.getRightArg();
        if (la.isLiteralExpr()) {
          Expr t = la;
          la = ra;
          ra = t;
        }
        if (ra.isLiteralExpr()) {
          la.deleteOutDataEdge(elecnt);
          ra.deleteOutDataEdge(elecnt);
          elecnt = la;
          offset = SubtractionExpr.create(longType, offset, ra);
        }
      } else if (elecnt instanceof SubtractionExpr) {
        SubtractionExpr ae = (SubtractionExpr) elecnt;
        Expr            la = ae.getLeftArg();
        Expr            ra = ae.getRightArg();
        if (ra.isLiteralExpr()) {
          la.deleteOutDataEdge(elecnt);
          ra.deleteOutDataEdge(elecnt);
          elecnt = la;
          offset = ((LiteralExpr) ra).add(elecnt.getType(), offset);
        }
      }
    }

    if (justIndex)
      return SubtractionExpr.create(longType, elecnt, offset);

    // The type of an ArrayIndexExpr instance is always a pointer to
    // the array element type because the index and offset are
    // computed based on that type.

    Type type = getType();
    Type pet  = type;
    Type petc = pet.getPointedToCore();
    if (petc.isArrayType())
      pet = PointerType.create(((ArrayType) petc).getElementType());

    Expr na  = array.copy();

    if (offset.isLiteralExpr()) {
      Literal lit = ((LiteralExpr) offset).getLiteral().getConstantValue();
      if (lit instanceof IntLiteral) {
        // If it is a simple addition of a constant, we generate an
        // add.  If it is more complex we use an ArrayIndexExpr
        // instance because architectures have different ways of
        // efficiently constructing array index addresses from index
        // expressions.

        long val = ((IntLiteral) lit).getLongValue();
        if (elecnt.isLiteralExpr()) {
          Literal lit2 = ((LiteralExpr) elecnt).getLiteral().getConstantValue();
          if (lit2 instanceof IntLiteral) {
            Type et   = pet.getPointedToCore();
            long mult = et.memorySize(Machine.currentMachine);
            long val2 = (((IntLiteral) lit2).getLongValue() - val) * mult;

            if (val2 == 0)
              return ConversionExpr.create(type, na, CastMode.CAST);

            Expr        arr = ConversionExpr.create(pet, na, CastMode.CAST);
            LiteralExpr le  = new LiteralExpr(LiteralMap.put(val2, longType));
            arr = AdditionExpr.create(pet, arr, le);

            return ConversionExpr.create(type, arr, CastMode.CAST);
          }
        }

        LiteralExpr    le  = new LiteralExpr(LiteralMap.put(-val, longType));
        ArrayIndexExpr arr = new ArrayIndexExpr(pet, na, elecnt, le);
        return ConversionExpr.create(type, arr, CastMode.CAST);
      }
    }

    if (offset instanceof NegativeExpr) {
      NegativeExpr ne = (NegativeExpr) offset;
      offset = ne.getArg();
      ne.setArg(null);
    } else
      offset = NegativeExpr.create(longType, offset);

    ArrayIndexExpr aie = new ArrayIndexExpr(pet, na, elecnt, offset);
    return ConversionExpr.create(type, aie, CastMode.CAST);
  }

  /**
   * Lower the SubscriptExpr to an {@link ArrayIndexExpr
   * ArrayIndexExpr} or {@link AdditionExpr AdditionExpr} instance.
   * Lowering may result in a sequence of CFG nodes as well as the
   * lowered expression.  These nodes are appended to the supplied
   * vector in the order in which they should be in the CFG.  Also,
   * some temporary variables may be created to hold intermediate
   * results.  These new variable declarations are added to the
   * supplied vector.
   */
  public Expr lower()
  {
    return lowerIndexOffset(false);
  }

  /**
   * Lower the SubscriptExpr to just the index calculation.  Lowering
   * may result in a sequence of CFG nodes as well as the lowered
   * expression.  These nodes are appended to the supplied vector in
   * the order in which they should be in the CFG.  Also, some
   * temporary variables may be created to hold intermediate results.
   * These new variable declarations are added to the supplied vector.
   */
  public Expr lowerIndex()
  {
    return lowerIndexOffset(true);
  }

  /**
   * Clean up any loop related information.
   */
  public void loopClean()
  {
    super.loopClean();

    array.loopClean();
    for (int i = 0; i < mins.length; i++) {
      mins[i].loopClean();
      sizes[i].loopClean();
      subscripts[i].loopClean();
    }
  }

  /**
   * If the node is no longer needed, sever its use-def link, etc.
   */
  public void unlinkExpression()
  {
    super.unlinkExpression();

    array.unlinkExpression();
    for (int i = 0; i < mins.length; i++) {
      mins[i].unlinkExpression();
      sizes[i].unlinkExpression();
      subscripts[i].unlinkExpression();
    }
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean containsDeclaration(Declaration decl)
  {
    if (array.containsDeclaration(decl))
      return true;

    for (int i = 0; i < subscripts.length; i++) {
      Expr exp = subscripts[i];
      if (exp.containsDeclaration(decl))
        return true;
      if (mins[i].containsDeclaration(decl))
        return true;
      if (sizes[i].containsDeclaration(decl))
        return true;
    }
    return false;
  }

  /**
   * Return true if this expression's value depends on the variable.
   * The use-def links are followed.
   * @see scale.score.expr.LoadExpr#setUseOriginal
   */
  public boolean dependsOnDeclaration(Declaration decl)
  {
    if (array.dependsOnDeclaration(decl))
      return true;

    for (int i = 0; i < subscripts.length; i++) {
      Expr exp = subscripts[i];
      if (subscripts[i].dependsOnDeclaration(decl))
        return true;
      if (mins[i].dependsOnDeclaration(decl))
        return true;
      if (sizes[i].dependsOnDeclaration(decl))
        return true;
    }
    return false;
  }

  /**
   * Return true if all the subscripts of the subscript expression are
   * optimization candidates.
   */
  public boolean allSubscriptsOptimizationCandidates()
  {
    for (int i = 0; i < subscripts.length; i++) {
      Expr exp = subscripts[i];
      if (!exp.optimizationCandidate())
        return false;
    }
    return true;
  }

  /**
   * Return true if the expression can be moved without problems.
   * Expressions that reference global variables, non-atomic
   * variables, etc are not optimization candidates. For a
   * SubscriptExpr instance return false.  Array accesses must be
   * optimized using data dependence testing.
   * @see scale.score.trans.ScalarReplacement
   */
  public boolean optimizationCandidate()
  {
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
    return this;
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
    AliasAnnote aa = super.getAliasAnnote();
    if (aa != null)
      return aa;
    return getArray().getAliasAnnote();
  }

  /**
   * Add all declarations referenced in this expression to the
   * Vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    for (int i = 0; i < subscripts.length; i++) {
      subscripts[i].getDeclList(varList);
      mins[i].getDeclList(varList);
      sizes[i].getDeclList(varList);
    }
    array.getDeclList(varList);
  }

  /**
   * Add all LoadExpr instances in this expression to the Vector.
   */
  public void getLoadExprList(Vector<LoadExpr> expList)
  {
    for (int i = 0; i < subscripts.length; i++) {
      subscripts[i].getLoadExprList(expList);
      mins[i].getLoadExprList(expList);
      sizes[i].getLoadExprList(expList);
    }
    array.getLoadExprList(expList);
  }

  /**
   * Add all Expr instances in this expression to the Vector.
   */
  public void getExprList(Vector<Expr> expList)
  {
    expList.addElement(this);
    for (int i = 0; i < subscripts.length; i++) {
      subscripts[i].getExprList(expList);
      mins[i].getExprList(expList);
      sizes[i].getExprList(expList);
    }
    array.getExprList(expList);
  }


  /**
   * Push all of the operands of this expression on the Stack.
   */
  public void pushOperands(Stack<Expr> wl)
  {
    for (int i = 0; i < subscripts.length; i++) {
      wl.push(subscripts[i]);
      wl.push(mins[i]);
      wl.push(sizes[i]);
    }
    wl.push(array);
  }

  /**
   * Replace all occurrances of a Declaration with another
   * Declaration.  Return true if a replace occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    boolean changed = false;
    for (int i = 0; i < subscripts.length; i++) {
      changed |= subscripts[i].replaceDecl(oldDecl, newDecl);
      changed |= mins[i].replaceDecl(oldDecl, newDecl);
      changed |= sizes[i].replaceDecl(oldDecl, newDecl);
    }
    changed |= array.replaceDecl(oldDecl, newDecl);
    return changed;
  }

  /**
   * Remove any use - def links, may - use links, etc.
   */
  public void removeUseDef()
  {
    boolean changed = false;
    for (int i = 0; i < subscripts.length; i++) {
      subscripts[i].removeUseDef();
      mins[i].removeUseDef();
      sizes[i].removeUseDef();
    }
    array.removeUseDef();
  }

  /**
   * Record any variable references in this expression in the table of
   * references.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    for (int i = 0; i < subscripts.length; i++) {
      subscripts[i].recordRefs(stmt, refs);
      mins[i].recordRefs(stmt, refs);
      sizes[i].recordRefs(stmt, refs);
    }
    array.recordRefs(stmt, refs);
  }

  /**
   * Remove any variable references in this expression from the table
   * of references.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    for (int i = 0; i < subscripts.length; i++) {
      subscripts[i].removeRefs(stmt, refs);
      mins[i].removeRefs(stmt, refs);
      sizes[i].removeRefs(stmt, refs);
    }
    array.removeRefs(stmt, refs);
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return 0; // We never want to move one of these.
  }

  /**
   * Return an indication of the side effects execution of this
   * expression may cause.
   * @see #SE_NONE
   */
  public int sideEffects()
  {
    int se = SE_NONE;
    for (int i = 0; i < subscripts.length; i++) {
      se |= subscripts[i].sideEffects();
      se |= mins[i].sideEffects();
      se |= sizes[i].sideEffects();
    }
    se |= array.sideEffects();
    return se;
  }
}
