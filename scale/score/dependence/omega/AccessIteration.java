package scale.score.dependence.omega;

import scale.common.*;
import scale.score.*;
import scale.score.chords.LoopHeaderChord;
import scale.score.dependence.*;
import scale.score.dependence.omega.omegaLib.*;

import scale.score.expr.*;
import scale.score.chords.Chord;
import scale.clef.decl.VariableDecl;

/**
 * A class for determining data dependences using the Omega library.
 * <p>
 * $Id: AccessIteration.java,v 1.51 2007-10-17 13:45:07 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class is an interface that lets the user generate Presburger
 * Formulae that represent dependences from program source.   Patterned
 * after notation used in Hans Zima and Barbara Chapman, <i>Supercompilers
 * for Parallel and Vector Computers</i>, ACM Press, 1991.
 * <p>
 * <b>Improvement:</b><br>
 * We use the actual node (<i>Expr</i>) to get information about the
 * loops.  We don't actually need to do this since we already get the
 * loop information when the omega test object, <i>OmegaTest</i>, is
 * created.
 */
public final class AccessIteration 
{
  private static int formErrorCount = 0;
  private static int ubAffineCount = 0;
  private static int lbAffineCount = 0;

  private static final String[] stats = {"formError", "ubAffine", "lbAffine"};

  static
  {
    Statistics.register("scale.score.dependence.omega.AccessIteration", stats);
  }

  /**
   * Return the count of all the failues due to incorrect form.
   */
  public static int formError()
  {
    return formErrorCount;
  }

  /**
   * Return the count of all the failues due to loop upper bound.
   */
  public static int ubAffine()
  {
    return ubAffineCount;
  }

  /**
   * Return the count of all the failues due to loop lower bound.
   */
  public static int lbAffine()
  {
    return lbAffineCount;
  }

  private SubscriptExpr   access;        // A reference to the memory location that this access represents.
  private LoopHeaderChord loop;          // A reference to the loop containing this access.
  private LoopHeaderChord outermostLoop; // A reference to the outer most loop containing this loop.
  private Relation        relation;      // The data dependence relation.
  private Vector<VarDecl> subscripts;    // The Omega tuple data structure which represents the subscripts of the array.
  private int             at;            // The access type: inputTuple or outputTuple.
  private Scribble        scribble;

  /**
   * Create a reference to a single memory access at a particular loop
   * iteration.  The object is used by the Omega test for adding
   * constraints to the data dependence relation.
   */
  public AccessIteration()
  {
  }

  /**
   * @param arr an access to an array.
   * @param loop is the loop containing the array access
   * @param omegaLib is the Omega Library token
   * @param relation the relation we are creating.
   * @param at is the kind: <tt>inputTuple</tt> or <tt>outputTuple</tt>
   */
  public void initialize(SubscriptExpr   arr,
                         LoopHeaderChord loop,
                         OmegaLib        omegaLib,
                         Relation        relation,
                         int             at) throws scale.common.Exception
  {
    this.access   = arr;
    this.loop     = loop;
    this.scribble = loop.getScribble();
    this.relation = relation;
    this.at       = at;

    this.outermostLoop = loop.getTopLoop();

    switch (at) {
    case VarDecl.UNKNOWN_TUPLE:
      formErrorCount++;
      throw new scale.common.Exception("Unknown tuple " + at);
    
    case VarDecl.INPUT_TUPLE:
      this.subscripts = omegaLib.inputVars();
      if (relation.isSet()) {
        if (relation.numberSet() != loop.getNestedLevel()) {
          formErrorCount++;
          throw new scale.common.Exception("Invalid relation - number of sets " + relation);
        }
      } else if (relation.numberInput() != loop.getNestedLevel()) {
        formErrorCount++;
        throw new scale.common.Exception("Invalid relation - number of inputs " + relation);
      }
      break;
    case VarDecl.OUTPUT_TUPLE:
      this.subscripts = omegaLib.outputVars();
      if (relation.numberOutput() != loop.getNestedLevel()) {
        formErrorCount++;
        throw new scale.common.Exception("Invalid relation - number of outputs " + relation);
      }
      break;
    default:
      throw new scale.common.Exception("Unknown tuple " + at);
    }
    
    // Assign the loop index variables names to the relation/subscripts.

    setNames();
  }
  
  /**
   * Add the constraints requiring that the loop subscripts for this
   * reference (the i in A[i]) are within the loop bounds.
   *
   * @param n Presburber formula which represents the dependence relation.
   */
  public void inBounds(FAnd n) throws java.lang.Exception
  {
    for (LoopHeaderChord lh = loop; lh != null; lh = lh.getParent()) { // Iterate over all nested loops.
      if (!lh.isTrueLoop())
        continue;

      int incr = (int) lh.getStepValue(); // Get the loop step.
      if (incr == 0) {
        formErrorCount++;
        throw new scale.common.Exception("Loop step value is not known: " + lh);
      }

      Expr       lb      = lh.getLowerBound();
      AffineExpr lbounds = outermostLoop.isAffine(lb);
      
      if (lbounds == null) {
        lbAffineCount++;
        throw new scale.common.Exception("Non-affine loop lower bound expression: " + lb);
      }

      Expr       ub      = lh.getUpperBound();
      AffineExpr ubounds = outermostLoop.isAffine(ub);

      if (ubounds == null) {
        ubAffineCount++;
        throw new scale.common.Exception("Non-affine loop upper bound expression: " + ub);
      }

      // If the loop step is not 1, then constrain the values that the index variable can assume.

      VarDecl index = subscripts.elementAt(lh.getNestedLevel()); // Get the loop index variable.

      if (incr != 1) {
        EQHandle step = n.addStride(incr, false);
        step.updateCoefficient(index, 1);
        addCoefficients(step, lbounds, -1, at);
      }

      incr = (incr >= 0 ? 1 : -1);

      GEQHandle lower = n.addGEQ(false);
      addCoefficients(lower, lbounds, -incr, at);
      lower.updateCoefficient(index, incr);

      GEQHandle upper = n.addGEQ(false);
      boolean   well  = addCoefficients(upper, ubounds, incr, at);
      upper.updateCoefficient(index, -incr);
    } 
  }

  /**
   * Add a constraint requiring that this memory location(A[i]) and 
   * B[j] are the same.  We add constaints to check if the subscripts 
   * are equal.
   *
   * @param n is the presburger formula representing the constraint
   * @param Bj is the other memory location (array subscript expression)
   */
  public void sameMemory(FAnd n, AccessIteration Bj) throws scale.common.Exception
  {
    SubscriptExpr ind1  = access;
    SubscriptExpr ind2  = Bj.access;
    int           ind1l = ind1.numSubscripts();
    int           ind2l = ind2.numSubscripts();

    if (ind1l != ind2l) {
      formErrorCount++;
      throw new scale.common.Exception("subscripts must have same number of elements");
    }

    for (int i = 0; i < ind1l; i++) {
      Expr sub1 = ind1.getSubscript(i);
      Expr sub2 = ind2.getSubscript(i);

      AffineExpr ae1 = outermostLoop.isAffine(sub1);
      AffineExpr ae2 = outermostLoop.isAffine(sub2);

      if ((ae1 != null) && (ae2 != null)) {
        EQHandle e = n.addEQ(false);
        addCoefficients(e, ae1, -1, VarDecl.INPUT_TUPLE);
        Bj.addCoefficients(e, ae2, 1, VarDecl.OUTPUT_TUPLE);
      } else {
        n.addUnknown();
      }
    }
  }

  /**
   * Add a constraint requiring that this memory location(A[i]) and 
   * B[j] are adjacent.  We add constaints to check if the subscripts 
   * are adjacent.
   *
   * @param n is the presburger formula representing the constraint
   * @param Bj is the other memory location
   */
  public void adjMemory(FAnd n, AccessIteration Bj, int blockSize) throws scale.common.Exception
  {
    SubscriptExpr ind1  = access;
    SubscriptExpr ind2  = Bj.access;
    int           ind1l = ind1.numSubscripts();
    int           ind2l = ind2.numSubscripts();

    if (ind1l != ind2l) {
      formErrorCount++;
      throw new scale.common.Exception("subscripts must have same number of elements");
    }

    for (int i = 0; i < ind1l; i++) {
      Expr sub1 = ind1.getSubscript(i);
      Expr sub2 = ind2.getSubscript(i);

      AffineExpr ae1  = outermostLoop.isAffine(sub1);
      AffineExpr ae2  = outermostLoop.isAffine(sub2);
      
      if ((ae1 == null) || (ae2 == null)) {
        n.addUnknown();
        continue;
      }

      if (i > 0) {
        EQHandle e = n.addEQ(false);
        addCoefficients(e, ae1, -1, VarDecl.INPUT_TUPLE);
        Bj.addCoefficients(e, ae2, 1, VarDecl.OUTPUT_TUPLE);
        continue;
      }

      // First index for C - last index for Fortran.

      FOr       f  = n.addOr();
      FAnd      a1 = f.addAnd();
      GEQHandle ge = a1.addGEQ(false);

      addCoefficients(ge, ae1, -1, VarDecl.INPUT_TUPLE);
      Bj.addCoefficients(ge, ae2, 1, VarDecl.OUTPUT_TUPLE);
      ge.updateConstant(blockSize); 

      FAnd      a2  = f.addAnd();
      GEQHandle ge1 = a2.addGEQ(false);

      addCoefficients(ge1, ae1, 1, VarDecl.INPUT_TUPLE);
      Bj.addCoefficients(ge1, ae2, -1, VarDecl.OUTPUT_TUPLE);
      ge1.updateConstant(blockSize); 
    }
  }

  /**
   * Assign the index loop variable names to the relation or subscripts
   * used in the <i>AccessIteration</i> class for each nested loop.
   */
  private void setNames() throws scale.common.Exception
  {
    for (LoopHeaderChord l = loop; l != null; l = l.getParent()) {
      if (l.getLoopIndexVar() == null) // Don't name the index if index variable is not available.
        continue;

      int    depth = l.getNestedLevel();
      String name  = l.getLoopIndexVar().getName();

      if (relation.isSet()) {
        relation.nameSetVar(depth, name);
        continue;
      }

      VarDecl var   = subscripts.elementAt(1);
      int     kind  = var.kind();

      if (kind == VarDecl.INPUT_VAR) {
        relation.nameInputVar(depth, name);
        continue;
      }

      if (kind == VarDecl.OUTPUT_VAR) {
        relation.nameOutputVar(depth, name);
        continue;
      }

      subscripts.elementAt(depth).nameVariable(name);
    }
  }

  /**
   * Add loop direction(sign) information to the dependence contraint.
   * @param c the Omega Test constraint structure.
   * @param ae the affine expression.
   * @param sign the sign, or loop, direction.
   * @return true if the affine expression contains the index term
   */
  private boolean addCoefficients(ConstraintHandle c,
                                  AffineExpr       ae,
                                  int              sign,
                                  int              of) throws scale.common.Exception
  {
    boolean hasIndex = false;

    // Add the constant term first.

    c.updateConstant((int) (sign * ae.getConstant()));

    VarDecl var    = null;
    int     cDepth = loop.getNestedLevel();

    // Then, update each of the terms.

    int vel = ae.numTerms();
    for (int i = 0; i < vel; i++) {
      VariableDecl indexVar  = ae.getVariable(i);
      String       name      = null;
      boolean      constant  = false;

      LoopHeaderChord tloop  = ae.getLoop(i);
      int             tDepth = 0;

      if (tloop != null)
        tDepth = tloop.getNestedLevel();

      if (indexVar != null) {
        name = indexVar.getName();
        InductionVar iv   = tloop.getInductionVar(indexVar);
        if (iv != null) { // If loop induction variable.
          if (iv.isPrimary()) // If primary induction variable.
            var = subscripts.elementAt(iv.getLoopHeader().getNestedLevel());
          else {
            if (iv.hasForward()) {
              AffineExpr ivfe = iv.getForwardExpr();
              if ((ivfe != ae) || (of != at))
                addCoefficients(c, ivfe, sign, at);
              continue;
            }
          }
          hasIndex = true;
        } else if (cDepth > tDepth) // Constant in this loop.
          constant = true;
      } else {
        constant = true;
        name = '(' + Long.toString(ae.getCoefficient(i)) + ')';
      }

      if (constant) {
        var = (VarDecl) relation.getVar(name);
        if (var == null) {
          FreeVarDecl n = new FreeVarDecl(name, 0);
          var = relation.getLocal(n);
          relation.putVar(name, var);
        }
      }

      if (var == null) {
        // - If indexVar is an induction variable for which forward substitution
        //   was not possible, or 
        // - If indexVar is not a constant, in formulas it will be represented as:
        //
        //    var_name(ivar1, ivar2, ...,ivarN)
        //
        // (e.g. mrsij#s_6(mi,mj,mr) (from olda.f))
        // where 
        //
        //  *  var_name is the (SSA) name of the induction variable.
        //  *  ivar1-ivarN are the loop (primary) induction variable names
        //     of the shared loop nest the load and the definitions of this
        //     variable is enclosed in. ivarN is the innermost loop
        //     induction variable name in the shared loop nest.
        //

        if (tDepth == 0) {
          var = (VarDecl) relation.getVar(name);
          
          if (var == null) {
            FreeVarDecl n = new FreeVarDecl(name, 0);
            var = relation.getLocal(n);
            relation.putVar(name, var);
          }
        } else {
          FreeVarDecl n = (FreeVarDecl) relation.getVar(name);
          
          if (n == null) {
            n = new FreeVarDecl(name, tDepth);
            relation.putVar(name, n);
          }
          
          String tuple = "I";
          if (of == VarDecl.OUTPUT_TUPLE)
            tuple = "O";
        
          name = name + tuple;
          var = (VarDecl) relation.getVar(name);
          if (var == null) {
            var = relation.getLocal(n, of);
            relation.putVar(name, var);
          }
        }
      }
      
      c.updateCoefficient(var, (int) (sign * ae.getCoefficient(i)));
    }

    return hasIndex;
  }
}
