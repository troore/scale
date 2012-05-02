package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.type.*;
import scale.clef.decl.Declaration;

/**
 * This is the base class for subscripting operations.
 * <p>
 * $Id: SubscriptOp.java,v 1.64 2007-10-04 19:58:06 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Regardless of whether the source language uses row major or column
 * major array ordering, the subscripts represented by an instance of
 * this class are always in row major (C style) ordering.
 * <p>
 * There is a difference between how subscripting is represented
 * between C and Fortran.
 * <table cols = 4 border=2>
 * <thead>
 * <tr><th>Array Def</th><th>Reference</th><th>C</th><th>Fortran</th>
 * </thead><tbody>
 * <tr>
 * <td><code>int x1[10]</code></td>
 * <td align=center><code>x1</code></td>
 * <td>pointer to <code>int</code></code></td>
 * <td>pointer to 1-D array of <code>int</code></td>
 * <tr>
 * <td><code>int (*p1)[10]</code></td>
 * <td align=center><code>p1</code></td>
 * <td>pointer to 1-D array of <code>int</code></td>
 * <td>NA</td>
 * <tr>
 * <td><code>int x2[10][10]</code></td>
 * <td align=center><code>x2</code></td>
 * <td>pointer to 1-D array of <code>int</code></td>
 * <td>pointer to 2-D array of <code>int</code></td>
 * <tr>
 * <td><code>int (*p2)[10][10]</code></td>
 * <td align=center><code>p2</code></td>
 * <td>pointer to 2-D array of <code>int</code></td>
 * <td>NA</td>
 * </tbody></table>
 * <p>
 * The C representation is necessary in order for pointer-arithmetic
 * and subscripting to be interchangeable.  It also requires that all
 * C arrays be 0-origin indexed.
 * <p>
 * For Fortran, arrays can be defined using any integer origin.
 * Therefore, in order for the proper array offset to be calculated,
 * it is necessary for the complete array information to be available
 * from the type of the array operand of a <code>SubscriptOp</code>
 * instance.
 * <p>
 * An identical representation, as is used after parsing ({@link
 * scale.score.expr.SubscriptExpr SubscriptExpr}, {@link
 * scale.score.expr.ArrayIndexExpr ArrayIndexExpr}), cannot be used by
 * the C parser because the C language allows both subscripting
 * notation and pointer arithmetic to be combined in the same array
 * access expression.
 * @see #setFortranArray
 * @see #isFortranArray
 */

public abstract class SubscriptOp extends Expression
{
  private Expression         array;        // The address of the array.
  private Vector<Expression> subscripts;   // A vector of the index expressions.
  private boolean            fortranArray; // True if the array is a Fortran array.

  public SubscriptOp(Type type, Expression array, Vector<Expression> subscripts)
  {
    super(type);
    setArray(array);
    this.subscripts = subscripts;
    assert array.getType().isPointerType() : "Not pointer type - " + array.getType();
  }

  /**
   * Return true if this instance is a Fortran array subscript.
   */
  public final boolean isFortranArray()
  {
    return fortranArray;
  }

  /**
   * Specify that this instance is a Fortran array subscript.
   */
  public final void setFortranArray()
  {
    fortranArray = true;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    SubscriptOp op = (SubscriptOp) exp;
    int n = subscripts.size();
    if (n != op.subscripts.size())
      return false;
    for (int i = 0; i < n; i++) {
      Expression arg1 = subscripts.elementAt(i);
      Expression arg2 = op.subscripts.elementAt(i);
      if (!arg1.equivalent(arg2))
        return false;
    }
    return array.equivalent(op.array);
  }

  public void visit(Predicate p)
  {
    p.visitSubscriptOp(this);
  }

  /**
   * Return the array address expression.
   */
  public final Expression getArray()
  {
    return array;
  }

  /**
   * Return the vector of index expressions.
   */
  protected Vector<Expression> getSubscripts()
  {
    if (subscripts == null)
      return null;

    Vector<Expression> ns = new Vector<Expression>(subscripts.size());
    for (Expression sub : subscripts)
      ns.add(sub);
    return ns;
  }

  /**
   * Specify the array address expression.
   */
  protected final void setArray(Expression array)
  {
    this.array = array;
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    if (i == 0)
      return array;

    return (Node) subscripts.elementAt(i - 1);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return 1 + subscripts.size();
  }

  /**
   * Return the number of subscripts.
   */
  public final int numSubscripts()
  {
    return subscripts.size();
  }

  /**
   * Return the specified subscript.  Regardless of whether the
   * source language uses row major or column major array ordering,
   * the subscripts represented by this expression are always in row
   * major (C style) ordering.
   */
  public Expression getSubscript(int i)
  {
    return subscripts.elementAt(i);
  }

  /**
   * Add another index to the subscript expression.
   */
  public final void addIndex(Expression index)
  {
    subscripts.add(index);
  }

  /**
   * Add another index to the subscript expression.
   */
  public final void addToIndex(int i, Expression add)
  {
    Expression exp = subscripts.get(i);
    subscripts.set(i, new AdditionOp(exp.getType(), exp, add));
  }

  /**
   * Return the constant linear index or a negative value if not constant.
   */
  public long getConstantIndex()
  {
    int        nsub     = numSubscripts();
    long[]     indicies = new long[nsub];
    Expression array    = getArray();
    Type       addt     = array.getPointedToCore();
    int        rank     = addt.getRank();
    ArrayType  at       = addt.returnArrayType();

    for (int i = 0; i < nsub; i++) {
      Literal ind = getSubscript(i).getConstantValue();
      if (!(ind instanceof IntLiteral))
        return -1;
      indicies[i] = ((IntLiteral) ind).getLongValue();
    }

    boolean fortranArray = isFortranArray();
    Bound[] dims         = null;
    if (fortranArray) {
      if (rank != nsub)
        throw new scale.common.InternalError("What subscripting is this? " +
                                             this +
                                             " " +
                                             rank +
                                             " " +
                                             nsub);
      dims = new Bound[nsub];
      for (int r = 0; r < rank; r++)
        dims[r] = at.getIndex(r);
    } else {
      if (rank >= nsub) {
        int    x  = rank + 1;
        long[] ni = new long[x];
        System.arraycopy(indicies, 0, ni, 0, nsub);
        indicies = ni;
        while (nsub < x)
          indicies[nsub++] = 0;
      } else if (rank != (nsub - 1))
        throw new scale.common.InternalError("What subscripting is this? " +
                                             this +
                                             " " +
                                             rank +
                                             " " +
                                             nsub);

      dims = new Bound[nsub];
      dims[0] = Bound.noBound;

      for (int r = 0; r < rank; r++)
        dims[r + 1] = at.getIndex(r);
    }

    long[] mins  = new long[nsub];
    long[] sizes = new long[nsub];

    for (int k = 0; k < nsub; k++) {
      Bound bd = dims[k];
      if (bd == Bound.noBound) {
        mins[k] =  0;
        sizes[k] = 0;
        continue;
      }

      long mine = 0;
      try {
        mine = bd.getConstMin();
      } catch (scale.common.InvalidException ex) {
        Expression min = bd.getMin();
        if (min == null)
          mine = 0;
        else {
          min = min.getConstantValue();
          if (!(min instanceof IntLiteral))
            return -1;
          mine = ((IntLiteral) min).getLongValue();
        }
      }

      mins[k] = mine;

      try {
        // The dimension is known at compile time.
        sizes[k] = bd.numberOfElements();
      } catch (scale.common.InvalidException ex1) { // The dimension must be calculated at run-time.
        long maxe = 0;
        try {
          maxe = bd.getConstMax();
        } catch (scale.common.InvalidException ex2) {
          Expression max = bd.getMax();
          if (max == null)
            maxe = 0;
          else {
            max = max.getConstantValue();
            if (!(max instanceof IntLiteral))
              return -1;
            maxe = ((IntLiteral) max).getLongValue();
          }
        }

        sizes[k] = (maxe - mine) + 1;
      }
    }

    long offset = indicies[0] - mins[0];
    for (int i = 1; i < nsub; i++) {
      offset = offset * sizes[i] + indicies[i] - mins[i];
      // System.out.println("   " + i + " " + offset + " " + sizes[i] + " "+ indicies[i] + " " + mins[i]);
    }

    return offset;
  }

  /**
   * Return true if this expression contains a reference to the
   * variable.
   */
  public boolean containsDeclaration(Declaration decl)
  {
    if (array.containsDeclaration(decl))
      return true;

    int l = subscripts.size();
    for (int i = 0; i < l; i++) {
      Expression exp = subscripts.get(i);
      if (exp.containsDeclaration(decl))
        return true;
    }
    return false;
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    array.getDeclList(varList);
    int l = subscripts.size();
    for (int i = 0; i < l; i++) {
      Expression exp = subscripts.get(i);
      exp.getDeclList(varList);
    }
  }
}
