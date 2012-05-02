package scale.score.expr;

import java.math.*;
import java.util.AbstractCollection;

import scale.common.*;
import scale.score.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.expr.Expression;
import scale.clef.expr.Literal;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.BooleanLiteral;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.AggregationElements;
import scale.clef.expr.AddressLiteral;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.LiteralMap;

import scale.score.dependence.*;
import scale.score.pred.References;


/**
 * This class represents the instantiation of a literal in Scribble.
 * <p>
 * $Id: LiteralExpr.java,v 1.97 2007-10-17 13:40:00 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This literal can be an aggregation of scalar values, a string, or a
 * scalar value.  The literal's actual value is stored in a seperate
 * object.
 * @see scale.clef.expr.Literal
 */
public class LiteralExpr extends ValueExpr
{
  /**
   * The literal of this expression.
   */
  private Literal literal;

  /**
   * This method builds an expression node with this operation as the
   * operator.  
   */
  public LiteralExpr(Literal literal)
  {
    super(literal.getCoreType());

    // Address literals in the CFG cause problems because they are not
    // noticed.  It is important that LoadDeclAddressExpr instances be
    // used instead.
    assert !(literal instanceof AddressLiteral);

    this.literal = literal;
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the {@link #equivalent equivalent()} method
   * of the derived classes.
   * @return true if the expressions are equivalent
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    LiteralExpr o = (LiteralExpr) exp;
    return literal.equivalent(o.literal);
  }

  /**
   * Return the {@link scale.clef.expr.Literal literal} associated
   * with this expression.
   */
  public Literal getLiteral()
  {
    return literal;
  }

  /**
   * Set the {@link scale.clef.expr.Literal literal} associated with
   * this expression.
   */
  public void setLiteral(Literal literal)
  {
    this.literal = literal;
  }

  public Expr copy()
  {
    return new LiteralExpr(literal);
  }

  public void visit(Predicate p)
  {
    p.visitLiteralExpr(this);
  }

  public String getDisplayLabel()
  {
    if (literal instanceof AggregationElements)
      return "{...}";
    if (literal == null)
      return "??";
    return literal.getGenericValue();
  }

  /**
   * Return the constant value of the expression.
   * Follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue(HashMap<Expr, Literal> cvMap)
  {
    Literal r = cvMap.get(this);
    if (r != null)
      return r;

    r = literal.getConstantValue();
    cvMap.put(this, r);
    return r;
  }

  /**
   * Return the constant value of the expression.
   * Do not follow use-def links.
   * @see scale.common.Lattice
   */
  public Literal getConstantValue()
  {
    return literal.getConstantValue();
  }

  /**
   * Return the {@link scale.score.chords.Chord Chord} with the
   * highest label value from the set of {@link
   * scale.score.chords.Chord Chords} that must be executed before
   * this expression.
   * @param lMap is used to memoize the expression to critical {@link
   * scale.score.chords.Chord Chord} information
   * @param independent is returned if the expression is not dependent
   * on anything
   */
  protected scale.score.chords.Chord findCriticalChord(HashMap<Expr, scale.score.chords.Chord> lMap,
                                                       scale.score.chords.Chord                independent)
  {
    return independent;
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    return literal.canonical();
  }

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    return true;
  }

  /**
   * Determine the coefficent of a linear expression.  Return the
   * coefficient value if the literal is an integer.
   */
  public int findLinearCoefficient(VariableDecl                       indexVar,
                                   scale.score.chords.LoopHeaderChord thisLoop)
  {
    return literal.findCoefficient();
  }

  protected AffineExpr getAffineRepresentation(HashMap<Expr, AffineExpr>          affines,
                                               scale.score.chords.LoopHeaderChord thisLoop)
  {
    if (literal instanceof IntLiteral)
      return new AffineExpr(((IntLiteral) literal).getLongValue());
    return AffineExpr.notAffine;
  }

  /**
   * Return true if the literal value is an atomic type.
   */
  public boolean optimizationCandidate()
  {
    return getCoreType().isAtomicType();
  }

  /**
   * Return a relative cost estimate for executing the expression.
   */
  public int executionCostEstimate()
  {
    return literal.executionCostEstimate();
  }

  /**
   * Add all declarations referenced in this expression to the
   * vector.
   */
  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    if (!(literal instanceof AddressLiteral))
      return;

    AddressLiteral al   = (AddressLiteral) literal;
    Declaration    decl = al.getDecl();

    if (decl == null)
      return;

    if (!varList.contains(decl))
      varList.add(decl);
  }

  /**
   * Replace all occurrances of a {@link scale.clef.decl.Declaration
   * declaration} with another declaration.  Return true if a replace
   * occurred.
   */
  public boolean replaceDecl(Declaration oldDecl, Declaration newDecl)
  {
    if (!(literal instanceof AddressLiteral))
      return false;

    AddressLiteral al   = (AddressLiteral) literal;
    Declaration    decl = al.getDecl();

    if (decl == null)
      return false;

    if (decl != oldDecl)
      return false;

    literal = new AddressLiteral(newDecl.getType(), newDecl, al.getOffset());

    return true;
  }

  /**
   * Record any variable references in this expression in the table of
   * {@link scale.score.pred.References references}.
   */
  public void recordRefs(scale.score.chords.Chord stmt, References refs)
  {
    if (literal instanceof AddressLiteral) {
      AddressLiteral la = (AddressLiteral) literal;
      Object         o  = la.getValue();
      if (o instanceof Declaration) {
        Declaration decl = (Declaration) o;
        refs.recordUse(stmt, this, decl);
        decl.setAddressTaken();
      }
    }    
  }

  /**
   * Remove any variable references in this expression from the table
   * of {@link scale.score.pred.References references}.
   */
  public void removeRefs(scale.score.chords.Chord stmt, References refs)
  {
    if (literal instanceof AddressLiteral) {
      AddressLiteral la = (AddressLiteral) literal;
      Object         o  = la.getValue();
      if (o instanceof Declaration) {
        Declaration decl = (Declaration) o;
        refs.remove(stmt, decl);
      }
    }    
  }

  private LiteralExpr genLiteral(long value, Type type)
  {
    if (type.isPointerType())
      type = Machine.currentMachine.getIntegerCalcType();
    return new LiteralExpr(LiteralMap.put(value, type));
  }

  /**
   * Return an expression that represents the addition of this
   * expression to the argument expression.  This is used in the
   * lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr add(Type type, Expr arg)
  {
    Object cv = literal.getConstantValue();

    if (arg.isLiteralExpr()) {
      Object lita = ((LiteralExpr) arg).literal.getConstantValue();
      if ((lita instanceof IntLiteral) && (cv instanceof IntLiteral)) {
        long value = ((IntLiteral) cv).getLongValue() + ((IntLiteral) lita).getLongValue();
        return genLiteral(value, type);
      } else if ((lita instanceof FloatLiteral) && (cv instanceof FloatLiteral)) {
        double value = (((FloatLiteral) cv).getDoubleValue() +
                        ((FloatLiteral) lita).getDoubleValue());
        return new LiteralExpr(LiteralMap.put(value, type));
      }
    }

    if (!(cv instanceof IntLiteral))
      return new AdditionExpr(type, arg, this);

    long value = ((IntLiteral) cv).getLongValue();

    if (arg instanceof AdditionExpr) {
      AdditionExpr ae = (AdditionExpr) arg;
      Expr         la = ae.getLeftArg();
      Expr         ra = ae.getRightArg();
      int          sw = (la.isLiteralExpr() ? 2 : 0) + (ra.isLiteralExpr() ? 1 : 0);
      switch (sw) {
      case 1:
        Object litra = ((LiteralExpr) ra).literal.getConstantValue();
        if (litra instanceof IntLiteral) {
          ae.setLeftArg(null);
          value += ((IntLiteral) litra).getLongValue();
          if (value == 0)
            return la.addCast(type);
          return AdditionExpr.create(type, la, genLiteral(value, type));
        }
        return AdditionExpr.create(type, this, arg);
      case 2:
        Object litla = ((LiteralExpr) la).literal.getConstantValue();
        if (litla instanceof IntLiteral) {
          ae.setRightArg(null);
          value += ((IntLiteral) litla).getLongValue();
          if (value == 0)
            return ra.addCast(type);
          return AdditionExpr.create(type, ra, genLiteral(value, type));
        }
        return AdditionExpr.create(type, this, arg);
      case 3:
        Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
        if (litla2 instanceof IntLiteral) {
          Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
          if (litra2 instanceof IntLiteral) {
            value += (((IntLiteral) litla2).getLongValue() +
                      ((IntLiteral) litra2).getLongValue());
            return genLiteral(value, type);
          }
        }
        break;
      case 0:
      default:
        break;
      }
    } else if (arg instanceof SubtractionExpr) {
      SubtractionExpr ae = (SubtractionExpr) arg;
      Expr            la = ae.getLeftArg();
      Expr            ra = ae.getRightArg();
      int             sw = (la.isLiteralExpr() ? 2 : 0) + (ra.isLiteralExpr() ? 1 : 0);
      switch (sw) {
      case 1:
        Object litra = ((LiteralExpr) ra).literal.getConstantValue();
        if (litra instanceof IntLiteral) {
          ae.setLeftArg(null);
          value -= ((IntLiteral) litra).getLongValue();
          if (value == 0)
            return la.addCast(type);
          return AdditionExpr.create(type, la, genLiteral(value, type));
        }
        return AdditionExpr.create(type, this, arg);
      case 2:
        Object litla = ((LiteralExpr) la).literal.getConstantValue();
        if (litla instanceof IntLiteral) {
          ae.setRightArg(null);
          value += ((IntLiteral) litla).getLongValue();
          if (value == 0)
            return new NegativeExpr(type, ra.addCast(type));
          return SubtractionExpr.create(type, genLiteral(value, type), ra);
        }
        return AdditionExpr.create(type, this, arg);
      case 3:
        Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
        if (litla2 instanceof IntLiteral) {
          Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
          if (litra2 instanceof IntLiteral) {
            value += (((IntLiteral) litla2).getLongValue() -
                      ((IntLiteral) litra2).getLongValue());
            return genLiteral(value, type);
          }
        }
        break;
      case 0:
      default:
        break;
      }
    }

    if (value == 0)
      return arg.addCast(type);

    return new AdditionExpr(type, arg, this);
  }

  /**
   * Return an expression that represents the subtraction of
   * the argument expression from expression.  This is used in the
   * lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr subtract(Type type, Expr arg)
  {
    Object cv = literal.getConstantValue();

    if (arg.isLiteralExpr()) {
      Object lita = ((LiteralExpr) arg).literal.getConstantValue();
      if ((lita instanceof IntLiteral) && (cv instanceof IntLiteral)) {
        long value = ((IntLiteral) cv).getLongValue() - ((IntLiteral) lita).getLongValue();
        return genLiteral(value, type);
      } else if ((lita instanceof FloatLiteral) && (cv instanceof FloatLiteral)) {
        double value = (((FloatLiteral) cv).getDoubleValue() -
                        ((FloatLiteral) lita).getDoubleValue());
        return new LiteralExpr(LiteralMap.put(value, type));
      }
    }

    if (!(cv instanceof IntLiteral))
      return new SubtractionExpr(type, this, arg);

    long value = ((IntLiteral) cv).getLongValue();

    if (arg instanceof AdditionExpr) {
      AdditionExpr ae = (AdditionExpr) arg;
      Expr         la = ae.getLeftArg();
      Expr         ra = ae.getRightArg();
      int          sw = (la.isLiteralExpr() ? 2 : 0) + (ra.isLiteralExpr() ? 1 : 0);
      switch (sw) {
      case 1:
        Object litra = ((LiteralExpr) ra).literal.getConstantValue();
        if (litra instanceof IntLiteral) {
          ae.setLeftArg(null);
          value -= ((IntLiteral) litra).getLongValue();
          if (value == 0)
            return new NegativeExpr(type, la.addCast(type));
          return SubtractionExpr.create(type, genLiteral(value, type), la);
        }
        return SubtractionExpr.create(type, this, arg);
      case 2:
        Object litla = ((LiteralExpr) la).literal.getConstantValue();
        if (litla instanceof IntLiteral) {
          ae.setRightArg(null);
          value -= ((IntLiteral) litla).getLongValue();
          if (value == 0)
            return new NegativeExpr(type, ra.addCast(type));
          return SubtractionExpr.create(type, genLiteral(value, type), ra);
        }
        return SubtractionExpr.create(type, this, arg);
      case 3:
        Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
        if (litla2 instanceof IntLiteral) {
          Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
          if (litra2 instanceof IntLiteral) {
            value -= (((IntLiteral) litra2).getLongValue() +
                      ((IntLiteral) litla2).getLongValue());
            return genLiteral(value, type);
          }
        }
        break;
      case 0:
      default:
        break;
      }
    } else if (arg instanceof SubtractionExpr) {
      SubtractionExpr ae = (SubtractionExpr) arg;
      Expr            la = ae.getLeftArg();
      Expr            ra = ae.getRightArg();
      int             sw = (la.isLiteralExpr() ? 2 : 0) + (ra.isLiteralExpr() ? 1 : 0);
      switch (sw) {
      case 1:
        Object litra = ((LiteralExpr) ra).literal.getConstantValue();
        if (litra instanceof IntLiteral) {
          ae.setLeftArg(null);
          value += ((IntLiteral) litra).getLongValue();
          if (value == 0)
            return new NegativeExpr(type, la.addCast(type));
          return SubtractionExpr.create(type, genLiteral(value, type), la);
        }
        return SubtractionExpr.create(type, this, arg);
      case 2:
        Object litla = ((LiteralExpr) la).literal.getConstantValue();
        if (litla instanceof IntLiteral) {
          ae.setRightArg(null);
          value -= ((IntLiteral) litla).getLongValue();
          if (value == 0)
            return ra.addCast(type);
          return AdditionExpr.create(type, ra, genLiteral(value, type));
        }
        return SubtractionExpr.create(type, this, arg);
      case 3:
        Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
        if (litla2 instanceof IntLiteral) {
          Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
          if (litra2 instanceof IntLiteral) {
            value -= (((IntLiteral) litla2).getLongValue() -
                      ((IntLiteral) litra2).getLongValue());
            return genLiteral(value, type);
          }
        }
        break;
      case 0:
      default:
        break;
      }
    }

    if (value == 0)
      return new NegativeExpr(type, arg.addCast(type));

    return new SubtractionExpr(type, this, arg);
  }

  /**
   * Return an expression that represents the multiplication of this
   * expression to the argument expression.  This is used in the
   * lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr multiply(Type type, Expr arg)
  {
    Object cv = literal.getConstantValue();
    if (arg.isLiteralExpr()) {
      Object lita = ((LiteralExpr) arg).literal.getConstantValue();
      if ((lita instanceof IntLiteral) && (cv instanceof IntLiteral)) {
        long value = ((IntLiteral) cv).getLongValue() * ((IntLiteral) lita).getLongValue();
        return genLiteral(value, type);
      } else if ((lita instanceof FloatLiteral) && (cv instanceof FloatLiteral)) {
        double value = (((FloatLiteral) cv).getDoubleValue() *
                        ((FloatLiteral) lita).getDoubleValue());
        return new LiteralExpr(LiteralMap.put(value, type));
      }
      return new MultiplicationExpr(type, arg, this);
    }

    int op = 0;
    if (arg instanceof AdditionExpr)
      op = 0;
    else if (arg instanceof SubtractionExpr)
      op = 1;
    else if (arg instanceof MultiplicationExpr)
      op = 2;
    else if (arg instanceof DivisionExpr)
      op = 3;
    else
      return new MultiplicationExpr(type, arg, this);

    BinaryExpr ae = (BinaryExpr) arg;
    Expr       la = ae.getLeftArg();
    Expr       ra = ae.getRightArg();

    long   ivalue = 0;
    double dvalue = 0.0;
    int    sw     = (la.isLiteralExpr() ? 2 : 0) + (ra.isLiteralExpr() ? 1 : 0);
    if (cv instanceof IntLiteral) {
      ivalue = ((IntLiteral) cv).getLongValue();
      if (ivalue == 0) {
        arg.unlinkExpression();
        return this;
      }
      if (ivalue == 1)
        return arg;
      if (ivalue == -1)
        return NegativeExpr.create(type, arg);
    } else if (cv instanceof FloatLiteral) {
      dvalue = ((FloatLiteral) cv).getDoubleValue();
      if (dvalue == 0.0) {
        arg.unlinkExpression();
        return this;
      }
      if (dvalue == 1.0)
        return arg;
      if (dvalue == -1.0)
        return NegativeExpr.create(type, arg);
      sw += 4;
    } else
      return new MultiplicationExpr(type, arg, this);

    switch (sw) {
    case 3: {
      Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
      Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
      if (litla2 instanceof IntLiteral) {
        if (litra2 instanceof IntLiteral) {
          long v1     = ((IntLiteral) litla2).getLongValue();
          long v2     = ((IntLiteral) litra2).getLongValue();
          switch (op) {
          case 0:
            ivalue *= (v1 + v2);
            break;
          case 1:
            ivalue *= (v1 - v2);
            break;
          case 2:
            ivalue *= (v1 * v2);
            break;
          case 3:
            if (v2 == 0)
              return new MultiplicationExpr(type, arg, this);
            ivalue *= (v1 / v2);
            break;
          }
          return genLiteral(ivalue, type);
        }
      }
      break;
    }
    case 7: {
      Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
      Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
      if (litla2 instanceof FloatLiteral) {
        if (litra2 instanceof FloatLiteral) {
          double v1 = ((FloatLiteral) litla2).getDoubleValue();
          double v2 = ((FloatLiteral) litra2).getDoubleValue();
          switch (op) {
          case 0:
            dvalue *= (v1 + v2);
            break;
          case 1:
            dvalue *= (v1 - v2);
            break;
          case 2:
            dvalue *= (v1 * v2);
            break;
          case 3:
            if (v2 == 0.0)
              return new MultiplicationExpr(type, arg, this);
            dvalue *= (v1 / v2);
            break;
          }
          return new LiteralExpr(LiteralMap.put(dvalue, type));
        }
      }
      break;
    }
    case 1:
    case 5:
      if (op == 2) { // k * (y * c) <=> y * (k * c)
        ae.setLeftArg(null);
        ae.setRightArg(null);
        return new MultiplicationExpr(type, la, this.multiply(type, ra));
      } else if ((op == 3) && type.isRealType()) { // k * (y / c) <=> y * (k / c)
        ae.setLeftArg(null);
        ae.setRightArg(null);
        return new MultiplicationExpr(type, la, this.divide(type, ra));
      } else if (op == 1) { // k * (y - c) <=> k * y - k * c
        ae.setLeftArg(null);
        ae.setRightArg(null);
        return new SubtractionExpr(type, this.multiply(type, la), this.multiply(type, ra));
      } else if (op == 0) { // k * (y + c) <=> k * y + k * c
        ae.setLeftArg(null);
        ae.setRightArg(null);
        return new AdditionExpr(type, this.multiply(type, la), this.multiply(type, ra));
      }
      break;
    case 2:
    case 6:
      if (op == 2) { // k * (c * y) <=> y * (c * k)
        ae.setLeftArg(null);
        ae.setRightArg(null);
        return new MultiplicationExpr(type, ra, ((LiteralExpr) la).multiply(type, this));
      } else if ((op == 3) && type.isRealType()) { // k * (c / y) <=> (k * c) / y
        ae.setLeftArg(null);
        ae.setRightArg(null);
        return new DivisionExpr(type, this.multiply(type, la), ra);
      }
      break;
    case 0:
    case 4:
      break;
    }

    return new MultiplicationExpr(type, arg, this);
  }

  /**
   * Return an expression that represents the division of this
   * expression by the argument expression.  This is used in the
   * lowering of subscript expressions.
   * @see scale.score.expr.SubscriptExpr#lower
   */
  public Expr divide(Type type, Expr arg)
  {
    Object cv = literal.getConstantValue();

    if (arg.isLiteralExpr()) {
      Object lita = ((LiteralExpr) arg).literal.getConstantValue();
      if ((lita instanceof IntLiteral) && (cv instanceof IntLiteral)) {
        long quo = ((IntLiteral) cv).getLongValue();
        long div = ((IntLiteral) lita).getLongValue();
        if (div != 0) {
          long value = quo / div;
          return genLiteral(value, type);
        }
      } else if ((lita instanceof FloatLiteral) && (cv instanceof FloatLiteral)) {
        double quo = ((FloatLiteral) cv).getDoubleValue();
        double div = ((FloatLiteral) lita).getDoubleValue();
        if (div != 0.0) {
          double value = quo / div;
          return new LiteralExpr(LiteralMap.put(value, type));
        }
      }
      return new DivisionExpr(type, this, arg);
    }

    int op = 0;
    if (arg instanceof AdditionExpr)
      op = 0;
    else if (arg instanceof SubtractionExpr)
      op = 1;
    else if (arg instanceof MultiplicationExpr)
      op = 2;
    else if (arg instanceof DivisionExpr)
      op = 3;
    else
      return new DivisionExpr(type, this, arg);

    BinaryExpr ae = (BinaryExpr) arg;
    Expr       la = ae.getLeftArg();
    Expr       ra = ae.getRightArg();

    long   ivalue = 0;
    double dvalue = 0.0;
    int    sw     = (la.isLiteralExpr() ? 2 : 0) + (ra.isLiteralExpr() ? 1 : 0);
    if (cv instanceof IntLiteral) {
      ivalue = ((IntLiteral) cv).getLongValue();
      if (ivalue == 0)
        return this;
    } else if (cv instanceof FloatLiteral) {
      dvalue = ((FloatLiteral) cv).getDoubleValue();
      if (dvalue == 0.0)
        return this;
      sw += 4;
    } else
      return new DivisionExpr(type, this, arg);

    switch (sw) {
    case 3: {
      Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
      Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
      if (litla2 instanceof IntLiteral) {
        if (litra2 instanceof IntLiteral) {
          long v1     = ((IntLiteral) litla2).getLongValue();
          long v2     = ((IntLiteral) litra2).getLongValue();
          switch (op) {
          case 0:
            v1 = (v1 + v2);
            break;
          case 1:
            v1 = (v1 - v2);
            break;
          case 2:
            v1 = (v1 * v2);
            break;
          case 3:
            if (v2 == 0)
              return new DivisionExpr(type, this, arg);
            v1 = (v1 / v2);
            break;
          }
          if (v1 != 0)
            return genLiteral(ivalue / v1, type);
        }
      }
      break;
    }
    case 7: {
      Object litla2 = ((LiteralExpr) la).literal.getConstantValue();
      Object litra2 = ((LiteralExpr) ra).literal.getConstantValue();
      if (litla2 instanceof FloatLiteral) {
        if (litra2 instanceof FloatLiteral) {
          double v1 = ((FloatLiteral) litla2).getDoubleValue();
          double v2 = ((FloatLiteral) litra2).getDoubleValue();
          switch (op) {
          case 0:
            v1 = (v1 + v2);
            break;
          case 1:
            v1 = (v1 - v2);
            break;
          case 2:
            v1 = (v1 * v2);
            break;
          case 3:
            if (v2 == 0.0)
              return new DivisionExpr(type, this, arg);
            v1 = (v1 / v2);
            break;
          }
          if (v1 != 0.0)
            return new LiteralExpr(LiteralMap.put(dvalue / v1, type));
        }
      }
      break;
    }
    case 1:
    case 5:
      if (type.isRealType()) {
        if ((op == 2) && type.isRealType()) { // k / (x * c) <=> (k / c) / x
          ae.setLeftArg(null);
          ae.setRightArg(null);
          return new DivisionExpr(type, this.divide(type, ra), la);
        } else if (op == 3) { // k / (x / c) <=> (k * c) / x
          ae.setLeftArg(null);
          ae.setRightArg(null);
          return new DivisionExpr(type, this.multiply(type, ra), la);
        }
      }
      break;
    case 2:
    case 6:
      if (type.isRealType()) {
        if (op == 2) { // k / (c * x) <=> (k / c) / x
          ae.setLeftArg(null);
          ae.setRightArg(null);
          return new DivisionExpr(type, this.divide(type, la), ra);
        } else if (op == 3) { // k / (c / x) <=> (k / c) * x
          ae.setLeftArg(null);
          ae.setRightArg(null);
          return new MultiplicationExpr(type, this.divide(type, la), ra);
        }
      }
      break;
    case 0:
    case 4:
      break;
    }

    return new DivisionExpr(type, this, arg);
  }

  /**
   * Return true if the value of this literal is known to be zero.
   */
  public boolean isZero()
  {
    return literal.isZero();
  }

  /**
   * Return true if the value of this literal is known to be one.
   */
  public boolean isOne()
  {
    return literal.isOne();
  }

  /**
   * Return true if this is a simple expression.  A simple expression
   * consists solely of local scalar variables, constants, and numeric
   * operations such as add, subtract, multiply, and divide.
   */
  public boolean isSimpleExpr()
  {
    return getType().isAtomicType();
  }

  /**
   * Return true if this is a literal expression.
   */
  public boolean isLiteralExpr()
  {
    return true;
  }

  /**
   *  Return true if the result of the expression is either true (1)
   *  or false (0).
   */
  public boolean hasTrueFalseResult()
  {
    Literal lit = literal.getConstantValue();

    if (lit instanceof BooleanLiteral)
      return true;

    if (lit instanceof IntLiteral) {
      long v = ((IntLiteral) lit).getLongValue();
      return (v == 1) || (v == 0);
    }

    return false;
  }
}
