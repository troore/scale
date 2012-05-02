package scale.clef.expr;

import java.util.AbstractCollection;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.stmt.*;
import scale.clef.type.*;

/**
 * The AggregationElements class represents an aggregation of values
 * used to initialize a variable.
 * <p>
 * $Id: AggregationElements.java,v 1.67 2007-10-04 19:58:05 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * The elements of the aggregation comprize a sequence of {@link
 * scale.clef.expr.Literal Literal} instances and {@link
 * scale.clef.expr.PositionOp PositionOp} instances.
 * @see Literal
 * @see PositionOp
 * @see PositionFieldOp
 * @see PositionIndexOp
 * @see PositionOffsetOp
 * @see PositionRepeatOp
 */

public class AggregationElements extends Literal
{
  private Vector<Object> elements; // A vector of values and positioning information.

  /**
   * Create an initialization expression to represent multiple values.
   * Each element of the vector must be a {@link Literal Literal}
   * instance or a {@link PositionOp PositionOp} instance.
   * @param type is of no consequence
   * @param elements is a vector of the elements and positioning information
   */
  public AggregationElements(Type type, Vector<Object> elements)
  {
    super(type);
    this.elements = elements;
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;

    AggregationElements al = (AggregationElements) exp;
    int                 l  = elements.size();

    if (l != al.elements.size())
      return false;

    for (int i = 0; i < l; i++) {
      Object e1 = elements.elementAt(i);
      Object e2 = al.elements.elementAt(i);

      if (e1 == e2)
        continue;

      if (e1.getClass() != e2.getClass())
        return false;

      if (e1 instanceof Expression) {
        Expression exp1 = (Expression) e1;

        if (!exp1.equivalent(e2))
          return false;
        continue;
      }

      if (e1 instanceof PositionOp) {
        PositionOp po = (PositionOp) e1;
        if (!po.equivalent(e2))
          return false;
        continue;
      }

      throw new scale.common.InternalError("What's this " + e1);
    }

    return true;
  }

  public void visit(Predicate p)
  {
    p.visitAggregationElements(this);
  }

  /**
   * Return the specified element of the aggregation.
   */
  public final Object getElement(int i)
  {
    return elements.elementAt(i);
  }

  /**
   * Return a vector of the elements of the aggregation.
   */
  public final Vector<Object> getElementVector()
  {
    return elements;
  }

  /**
   * Return the number of elements in the aggregation.
   */
  public int numElements()
  {
    return elements.size();
  }

  /**
   * Return the number of elements in the aggregation.  This is the
   * number of values specified - not the number of elements in the
   * type.
   */
  public int getCount()
  {
    int l   = elements.size();
    int cnt = 0;
    for (int i = 0; i < l; i++) {
      Object o = elements.get(i);

      if (o instanceof Literal) {
        cnt += ((Literal) o).getCount();
        continue;
      }

      if (o instanceof PositionRepeatOp) {
        PositionRepeatOp pop = (PositionRepeatOp) o;
        cnt += pop.getCount() * ((Literal) elements.get(++i)).getCount();
      }
    }

    return cnt;
  }

  /**
   * Return the specified element of the constant.
   */
  public Literal getElement(long index) throws InvalidException
  {
    int l   = elements.size();
    int cnt = 0;
    for (int i = 0; i < l; i++) {
      Object o = elements.get(i);

      if (o instanceof Literal) {
        int x = ((Literal) o).getCount();
        if ((x + cnt) >= index)
          return ((Literal) o).getElement(index - cnt);
        cnt += x;
        continue;
      }

      if (o instanceof PositionRepeatOp) {
        PositionRepeatOp pop = (PositionRepeatOp) o;
        int              r   = pop.getCount();
        Literal          lit = (Literal) elements.get(++i);
        int              s   = lit.getCount();
        int              x   = r * s;
        if ((x + cnt) >= index)
          return ((Literal) elements.get(i + 1)).getElement((index - cnt) % s);
        cnt += x;
      }
    }
    throw new InvalidException("No such element");
   }

  /**
   * Return true if the all the elements are zero.
   */
  public final boolean containsAllZeros() 
  {
    int l = elements.size();
    for (int i = 0; i < l; i++) {
      Object o = elements.get(i);

      if (o instanceof AggregationElements) {
        if (!((AggregationElements) o).containsAllZeros())
          return false;
        continue;
      }

      if (o instanceof Literal) {
        if (!((Literal) o).isZero())
          return false;
      } else
        return false;
    }
    return true;
  }

  /**
   * Return true if the all elements are instances of the {@link
   * Literal Literal} class and none of the literals are {@link
   * AddressLiteral AddressLiteral} instances that reference constants
   * or variables whose addresses are not constant.
   */
  public boolean containsAllLiterals()
  {
    int l = elements.size();

    for (int i = 0; i < l; i++) {
      Object x = elements.elementAt(i);

      if (x instanceof PositionOp)
        continue;

      if (!(x instanceof Literal))
        return false;

      if (x instanceof AggregationElements) {
        AggregationElements ag = (AggregationElements) x;
        if (!ag.containsAllLiterals())
          return false;
      } else if (x instanceof AddressLiteral) {
        Declaration decl = ((AddressLiteral) x).getDecl();
        if ((decl == null) || (decl.residency() != Residency.MEMORY))
          return false;
      }
    }

    return true;
  }

  /**
   * Get the string version of the literal using C syntax.
   * @return a String already converted for display using C syntax.
   */
  public String getGenericValue()
  {
    throw new scale.common.InternalError("No generic value for " + this);
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    return "{ ... }";
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    int n = 0;
    int l = elements.size();
    for (int j = 0; j < l; j++) {
      Object x = elements.get(j);
      if (x instanceof Node) {
        if (n == i)
          return (Node) x;
        n++;
      }
    }
    throw new scale.common.InternalError("No such element " + i);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    int n = 0;
    int l = elements.size();
    for (int i = 0; i < l; i++)
      if (elements.get(i) instanceof Node)
        n++;
    return n;
  }

  /**
   * Convert children of this node to a string representation.
   */
  public String toStringChildren()
  {
    StringBuffer s = new StringBuffer();
    int          l = elements.size();
    int          k = 0;
    for (int j = 0; j < l; j++) {
      Object x = elements.get(j);
      if (x == null)
        continue;

      if (k >= 10) {
        s.append(" ...");
        break;
      }

      k++;

      String str = x.toString();
      if (str.charAt(0) != ' ')
        s.append(' ');
      s.append(str);
    }

    return s.toString();
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    int l = elements.size();
    for (int i = 0; i < l; i++) {
      Object o = elements.get(i);
      if (!(o instanceof Expression))
          continue;
      Expression exp = (Expression) o;
      if (exp != null)
        exp.getDeclList(varList);
    }
  }
}
