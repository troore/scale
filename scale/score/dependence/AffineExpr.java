package scale.score.dependence;

import scale.common.*;
import scale.score.*;
import scale.score.expr.Expr;
import scale.score.chords.LoopHeaderChord;

import scale.clef.decl.VariableDecl;

/**
 * A class to represent affine expressions used by the data dependence
 * tester.
 * <p>
 * $Id: AffineExpr.java,v 1.25 2007-05-03 18:00:57 burrill Exp $
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An expression is an affine function of variables
 * <code>X<sub>1</sub>, X<sub>2</sub>, ..., X<sub>n</sub></code> if it
 * can be expressed as <code>k + &Sigma;
 * a<sub>i</sub>X<sub>i</sub></code> where <code>k,
 * a<sub>i</sub></code> are constants. Note, we do not handle the
 * cases where <code>a<sub>i</sub></code> or <code>k</code> are loop
 * invariant expressions; they must be integer constants.
 */
public final class AffineExpr
{
  /**
   * A token used to represent the affine representation for
   * non-affine expressions.
   */
  public static final AffineExpr notAffine = new AffineExpr();

  /**
   * A token used to represent the affine representation for a Phi
   * functions during determination of its affine representation.
   */
  public static final AffineExpr marker = new AffineExpr();

  /**
   * The terms in the affine expression.
   */
  private int               length;       // The number of terms.
  private VariableDecl[]    vars;         // x
  private long[]            coefficients; // a
  private LoopHeaderChord[] loops;
  private long              constant;     // k

  /**
   * Create a new affine expression object with room for n terms.
   */
  private AffineExpr(int n)
  {
    constant = 0;
    length   = 0;

    if (n == 0)
      return;

    vars         = new VariableDecl[n];
    coefficients = new long[n];
    loops        = new LoopHeaderChord[n];
  }
  
  /**
   * Create a new affine expression.
   */
  public AffineExpr()
  {
    constant = 0;
    length   = 0;
  }

  /**
   * Create a new affine expression.
   */
  public AffineExpr(long coefficient)
  {
    constant = coefficient;
    length   = 0;
  }

  /**
   * Create a new affine expression with one term.
   */
  public AffineExpr(VariableDecl vd, LoopHeaderChord loop)
  {
    this(vd, 1, loop);
  }

  /**
   * Create a new affine expression with one term.
   */
  public AffineExpr(VariableDecl vd, long coefficient, LoopHeaderChord loop)
  {
    this(1);

    vars[0]         = vd;
    coefficients[0] = coefficient;
    loops[0]        = loop;
    length          = 1;
  }

  /**
   * Add a variable to the affine expression.
   * @param var the variable
   * @param loop is the loop in which the variable is defined
   */
  public void addTerm(VariableDecl var, LoopHeaderChord loop)
  {
    addTerm(var, 1, loop);
  }

  /**
   * Add a variable to the affine expression.
   * @param var the variable
   * @param coefficient is the coefficient of the variable
   * @param loop is the loop in which the variable is defined
   */
  public void addTerm(VariableDecl var, long coefficient, LoopHeaderChord loop)
  {
    for (int i = 0; i < length; i++)
      if (vars[i] == var) {
        coefficients[i] += coefficient;
        return;
      }

    addTermInt(var, coefficient, loop);
  }

  private void addTermInt(VariableDecl var, long coefficient, LoopHeaderChord loop)
  {
    if (vars == null) {
      vars         = new VariableDecl[1];
      coefficients = new long[1];
      loops        = new LoopHeaderChord[1];
    } else if (length >= vars.length) {
      VariableDecl[] na = new VariableDecl[length + 2];
      System.arraycopy(vars, 0, na, 0, length);
      vars = na;
      long[] nc = new long[length + 2];
      System.arraycopy(coefficients, 0, nc, 0, length);
      coefficients = nc;
      LoopHeaderChord[] nl = new LoopHeaderChord[length + 2];
      System.arraycopy(loops, 0, nl, 0, length);
      loops = nl;
    }

    vars[length] = var;
    coefficients[length] = coefficient;
    loops[length] = loop;
    length++;
  }

  /**
   * Add a constant value to the affine expression.
   * @param coefficient is the constant value
   */
  public void addTerm(long coefficient)
  {
    constant += coefficient;
  }

  /**
   * Return true if the affine expression contains the parameter term.
   */
  public boolean hasTerm(VariableDecl term)
  {
    for (int i = 0; i < length; i++) {
      if (vars[i] == term)
        return true;
    }

    return false;
  }

  /**
   * Return the index of the term if the affine expression contains a
   * reference to the variable and -1 otherwise.
   */
  public int getTermIndex(VariableDecl term)
  {
    for (int i = 0; i < length; i++) {
      if (vars[i] == term)
        return i;
    }

    return -1;
  }

  /**
   * Return the index of the term if the affine expression contains a
   * reference to the variable or a renamed version of the variable
   * and -1 otherwise.
   */
  public int getTermIndexOrig(VariableDecl term)
  {
    for (int i = 0; i < length; i++) {
      if (vars[i].getOriginal() == term.getOriginal())
        return i;
    }

    return -1;
  }

  /**
   * Return the constant term from the affine expression.
   */
  public long getConstant()
  {
    return constant;
  }

  /**
   * Set the constant term of the affine expression.
   */
  public void setConstant(long c)
  {
    constant = c;
  }

  /**
   * Set the coefficient of the specified term of the affine
   * expression.
   */
  public void setCoefficient(int term, long coefficient)
  {
    coefficients[term] = coefficient;
  }

  /**
   * Return the coefficient of the specified term of the affine
   * expression.
   */
  public long getCoefficient(int term)
  {
    return coefficients[term];
  }

  /**
   * Return true if one of the terms is a loop index variable
   */
  public boolean hasLoopIndex(LoopHeaderChord loop)
  {
    for (int i = 0; i < length; i++) {
      if (loop.isLoopIndex(vars[i]))
        return true;
    }
    return false;
  }

  /**
   * Return the number of terms in the affine expression.
   */
  public int numTerms()
  {
    return length;
  }

  /**
   * Return the variable of the specified term of the affine
   * expression.
   */
  public VariableDecl getVariable(int term)
  {
    return vars[term];
  }

  /**
   * Return the loop associated with the specified term of the affine
   * expression.
   */
  public LoopHeaderChord getLoop(int term)
  {
    return loops[term];
  }

  /**
   *  Add constant to the affine expression.
   */
  public void addConst(long constant)
  {
    this.constant += constant;
  }

  /**
   *  Multiply the affine expression by a constant.
   */
  public void multConst(long c)
  {
    for (int i = 0; i < length; i++)
      coefficients[i] *= c;
    constant *= c;
  }

  /**
   *  Merge two affine expressions. That, is add the terms of the
   *  argument expression into this one.
   */
  public void merge(AffineExpr ae)
  {
    int l = ae.length;
    for (int i = 0; i < l; i++)
      addTerm(ae.vars[i], ae.coefficients[i], ae.loops[i]);

    constant += ae.constant;
    simplify();
  }

  private void removeTerm(int j)
  {
    int k = j + 1;
    int l = length - k;

    System.arraycopy(vars,         k, vars,         j, l);
    System.arraycopy(coefficients, k, coefficients, j, l);
    System.arraycopy(loops,        k, loops,        j, l);

    length--;

    vars[length] = null;
    loops[length] = null;
    coefficients[length] = 0;
  }

  private void simplify()
  {
    int i = 0;
    while (i < length) {
      VariableDecl cref = vars[i];
      
      int j = i + 1;
      while (j < length) {
        VariableDecl ref = vars[j];

        if (cref == ref) { // if same variables (names)
          long coeff = coefficients[j];
          coefficients[i] += coeff;
          removeTerm(j);
        }
        j++;
      }

      if (coefficients[i] == 0)
        removeTerm(i);
      else
        i++;
    }
  }

  /**
   * Normalize all terms to var term+ var term +...+const term,
   * Var terms ordered by canonical values of index varibles
   */
  private void normalize()
  {
    if (length <= 1)
      return;

    boolean exchange = true;
    while (exchange) {
      exchange = false;
      for (int j = 0; j < length - 1; j++) {
        int k    = j + 1;
        int ind1 = vars[j].getNodeID();
        int ind2 = vars[k].getNodeID();
        if (ind1 > ind2) {
          VariableDecl tv = vars[j];
          vars[j] = vars[k];
          vars[k] = tv;
          long ct = coefficients[j];
          coefficients[j] = coefficients[k];
          coefficients[k] = ct;
          LoopHeaderChord lt = loops[j];
          loops[j] = loops[k];
          loops[k] = lt;
          exchange = true;
        }
      }
    }
  }

  /**
   * Return true if this affine expression is equivalent to the argument.
   * @param ae affine expression to compare
   */
  public boolean equivalent(AffineExpr ae) 
  {
    normalize();
    ae.normalize();

    if (length != ae.length)
      return false;

    if (constant != ae.constant)
      return false;

    for (int i = 0; i < length; i++) {
      if (vars[i] != ae.vars[i])
        return false;
      if (coefficients[i] != ae.coefficients[i])
        return false;
    }

    return true;
  }
 
  /**
   * Return true if this affine expression is possibly greater than
   * the argument.
   * @param ae affine expression to compare
   */
  public boolean possiblyGreater(AffineExpr ae) 
  {
    normalize();
    ae.normalize();
  
    if (length != ae.length)
      return true;

    for (int i = 0; i < length; i++) {
      if (vars[i] != ae.vars[i])
        return true;
      if (coefficients[i] != ae.coefficients[i])
        return true;
    }

    return (constant > ae.constant);
  }
 
  /**
   * Return true if this affine expression is definitely greater than
   * or equal to the argument.
   * @param ae affine expression to compare
   */
  public boolean greaterEqual(AffineExpr ae) 
  {
    normalize();
    ae.normalize();
 
    if (length != ae.length)
      return false;

    for (int i = 0; i < length; i++) {
      if (vars[i] != ae.vars[i])
        return false;
      if (coefficients[i] != ae.coefficients[i])
        return false;
    }

    return (constant >= ae.constant);
  }

  /**
   * Return true if this affine expression is definitely greater than
   * the argument.
   * @param ae affine expression to compare
   */
  public boolean greater(AffineExpr ae) 
  {
    normalize();
    ae.normalize();
 
    if (length != ae.length)
      return false;

    for (int i = 0; i < length; i++) {
      if (vars[i] != ae.vars[i])
        return false;
      if (coefficients[i] != ae.coefficients[i])
        return false;
    }

    return (constant > ae.constant);
  }

  /**
   * Return true if this affine expression is possibly less than the
   * argument.
   * @param ae affine expression to compare
   */
  public boolean possiblyLess(AffineExpr ae) 
  {
    normalize();
    ae.normalize();
  
    if (length != ae.length)
      return true;

    for (int i = 0; i < length; i++) {
      if (vars[i] != ae.vars[i])
        return true;
      if (coefficients[i] != ae.coefficients[i])
        return true;
    }


    return (constant < ae.constant);
  }

  /**
   * Return true if this affine expression is definitely less than or
   * equal the argument.
   * @param ae affine expression to compare
   */
  public boolean lessEqual(AffineExpr ae) 
  {
    normalize();
    ae.normalize();
  
    if(length != ae.length)
      return false;

    for (int i = 0; i < length; i++) {
      if (vars[i] != ae.vars[i])
        return false;
      if (coefficients[i] != ae.coefficients[i])
        return false;
    }

    return (constant <= ae.constant);
  }
 
  /**
   * Return true if the difference of two affine expressions are
   * within in a range.
   * @param ae affine expression to compare
   */
  public boolean differenceWithin(AffineExpr ae, int offset) 
  {
    normalize();
    ae.normalize();
  
    if (length != ae.length)
      return false;

    for (int i = 0; i < length; i++) {
      if (vars[i] != ae.vars[i])
        return false;
      if (coefficients[i] != ae.coefficients[i])
        return false;
    }

    long diff = constant - ae.constant;
    return ((diff <= offset) && (diff >= -offset));
  }

  /**
   * Return true if this affine expression is constant.
   */
  public boolean isConst()
  {
    if (length <= 0)
      return true;

    for (int i = 0; i < length; i++)
      if (coefficients[i] != 0)
        return false;

    return true;
  }
    
  /**
   * Set the constant term to the smaller of this expression's
   * constant term and the specified expression's constant term.
   */
  public void lower(AffineExpr e)
  {
    if (isConst() && e.isConst() && (getConstant() > e.getConstant()))
      constant = e.constant;
  }

  /**
   * Set the constant term to the larger of this expression's constant
   * term and the specified expression's constant term.
   */
  public void upper(AffineExpr e)
  {
    if (isConst() && e.isConst() && (getConstant() < e.getConstant()))
      constant = e.constant;
  }

  /**
   * Set the constant term to the greatest common denominator of this
   * expression's constant term and the specified expression's
   * constant term.
   */
  public void gcd(AffineExpr e)
  {
    if (isConst() && e.isConst()) {
      long a = getConstant();
      long b = e.getConstant();

      if (a < 0)
        a = -a;
      if (b < 0)
        b = -b;

      if (a == 0)
        a = b;
      else if (b != 0) {  
        while(a != b) {
          if (a > b)
            a = a - b;
          else
            b = b - a;
        }      
      }      
      constant = a;
    }
  }

  /**
   * Create a copy of the AffineExpr.
   */
  public AffineExpr copy()
  {
    AffineExpr ae = new AffineExpr(length);
    
    for (int i = 0; i < length; i++) {
      ae.vars[i] = vars[i];
      ae.coefficients[i] = coefficients[i];
      ae.loops[i] = loops[i];
    }

    ae.constant = constant;

    return ae;
  }

  public String toString()
  {
    StringBuffer s = new StringBuffer("(");

    boolean firstTerm = true;

    for (int i = 0; i < length; i++) {
      long coeff = coefficients[i];

      if ((coeff >= 0)  && !firstTerm)
        s.append('+');

      s.append(coeff);
      s.append('*');
      s.append(vars[i].getName());

      firstTerm = false;
    }

    if ((constant >= 0) && !firstTerm)
      s.append('+');

    s.append(constant);

    s.append(')');
    return s.toString();
  }
    
  /**
   * Return this affine expression which is the addition of two affine
   * expressions.
   */
  public AffineExpr add(AffineExpr a2)
  {
    if (this == notAffine)
      return notAffine;
    if (a2 == notAffine)
      return notAffine;

    for (int i = 0; i < a2.length; i++) {
      VariableDecl cref  = a2.vars[i];
      boolean      flg   = true;

      for (int j = 0; j < this.length; j++) {
        VariableDecl ref = this.vars[j];

        if (cref == ref) {
          this.coefficients[j] += a2.coefficients[i];
          flg = false;
          break;
        }
      }
      if (flg)
        this.addTermInt(a2.vars[i], a2.coefficients[i], a2.loops[i]);
    }

    this.constant += a2.constant;

    return this;
  }

  /**
   * Return this affine expression which is the addition of two affine
   * expressions.
   */
  public static AffineExpr add(AffineExpr a1, AffineExpr a2)
  {
    if (a1 == null)
      return notAffine;
    if (a2 == null)
      return notAffine;

    AffineExpr ae = a1.copy();
    for (int i = 0; i < a2.length; i++) {
      VariableDecl cref  = a2.vars[i];
      ae.addTerm(cref, a2.coefficients[i], a2.loops[i]);
    }

    ae.constant += a2.constant;

    return ae;
  }

  /**
   * Return this affine expression which is the difference of two
   * affine expressions.
   */
  public AffineExpr subtract(AffineExpr a2)
  {
    if (this == notAffine)
      return notAffine;
    if (a2 == notAffine)
      return notAffine;

    for (int i = 0; i < a2.length; i++) {
      VariableDecl cref  = a2.vars[i];
      boolean      flg   = true;

      for (int j = 0; j < this.length; j++) {
        VariableDecl ref = this.vars[j];

        if (cref == ref) {
          this.coefficients[j] -= a2.coefficients[i];
          flg = false;
          break;
        }
      }
      if (flg)
        this.addTermInt(a2.vars[i], -a2.coefficients[i], a2.loops[i]);
    }

    this.constant -= a2.constant;

    return this;
  }

  /**
   * Return this affine expression which is the difference of two
   * affine expressions.
   */
  public static AffineExpr subtract(AffineExpr a1, AffineExpr a2)
  {
    if (a1 == null)
      return notAffine;
    if (a2 == null)
      return notAffine;

    AffineExpr ae = a1.copy();
    for (int i = 0; i < a2.length; i++) {
      VariableDecl cref  = a2.vars[i];
      ae.addTerm(cref, -a2.coefficients[i], a2.loops[i]);
    }

    ae.constant -= a2.constant;

    return ae;
  }

  /**
   * Return this affine expression which is the product of two affine
   * expressions.
   */
  public AffineExpr multiply(AffineExpr a2)
  {
    if (this == notAffine)
      return notAffine;
    if (a2 == notAffine)
      return notAffine;

    AffineExpr prod = null;
    long       mult = 0;
    if (!this.isConst()) {
      if (!a2.isConst())
        return notAffine;
      mult = a2.constant;
      prod = this;
    } else {
      prod = a2;
      mult = this.constant;
    }

    for (int i = 0; i < this.length; i++)
      prod.coefficients[i] *= mult;

    prod.constant *= mult;

    return prod;
  }

  /**
   * Return this affine expression which is the product of two
   * affine expressions.
   */
  public static AffineExpr multiply(AffineExpr a1, AffineExpr a2)
  {
    if (a1 == null)
      return notAffine;
    if (a2 == null)
      return notAffine;

    AffineExpr prod = null;
    long       mult = 0;
    if (!a1.isConst()) {
      if (!a2.isConst())
        return notAffine;
      mult = a2.constant;
      prod = a1.copy();
    } else {
      prod = a2.copy();
      mult = a1.constant;
    }

    for (int i = 0; i < prod.length; i++)
      prod.coefficients[i] *= mult;

    prod.constant *= mult;

    return prod;
  }

  /**
   * Return this affine expression which is the division of two affine
   * expressions.
   */
  public static AffineExpr divide(AffineExpr a1, AffineExpr a2)
  {
    if (a1 == null)
      return notAffine;
    if ((a2 == null) || !a2.isConst())
      return notAffine;

    AffineExpr quotient = a1.copy();
    long       divisor  = a2.constant;

    for (int i = 0; i < quotient.length; i++)
      quotient.coefficients[i] /= divisor;

    quotient.constant /= divisor;

    return quotient;
  }

  /**
   * Return this affine expression which is the division of two affine
   * expressions.
   */
  public static AffineExpr negate(AffineExpr a1)
  {
    if (a1 == null)
      return notAffine;

    AffineExpr neg = a1.copy();

    for (int i = 0; i < neg.length; i++)
      neg.coefficients[i] = - neg.coefficients[i];

    neg.constant = - neg.constant;

    return neg;
  }
}
