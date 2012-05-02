package scale.score.pred;

import java.util.Enumeration;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.clef.decl.Declaration;
import scale.clef.decl.VariableDecl;
import scale.clef.expr.Literal;
import scale.score.chords.*;

/**
 * This class scans a Scribble CFG looking for lexically identical
 * binary expressions.
 * <p>
 * $Id: ExpressionList.java,v 1.47 2007-10-04 19:58:34 burrill Exp $
 * <p>
 * Copyright 2008 by the
 *<a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class ExpressionList extends TraceChords
{
  public ExpressionList()
  {
    super();
  }

  private Table<Expr, Chord> exprOccurs = new Table<Expr, Chord>(); // list all lexical identical binary expressions and their occurences

  /**
   * Visit each statement and traverse the expression tree (if any)
   * connected to it.  The expression tree is traversed leaf-to-root
   * which is necessary for some algorithms such as alias analyses.
   * Only the low expression of a DualExpr is used.
   * <p>
   * Note - this logic assumes that the expression tree is a tree and
   * not a dag.
   * @see scale.score.expr.DualExpr
   */
  public void visitChord(Chord c)
  {
    thisChord = c;
  
    int l = c.numInDataEdges();
    for (int i = 0; i < l; i++) {
      Expr exp = c.getInDataEdge(i);
      while (exp instanceof DualExpr)
        exp = ((DualExpr) exp).getLow();
      wl.push(exp);
      rl.push(exp);
    }

    while (!rl.empty()) {
      Expr r = rl.pop();

      int ll = r.numInDataEdges();
      for (int i = 0; i < ll; i++) {
        Expr exp = r.getInDataEdge(i);
        while (exp instanceof DualExpr)
          exp = ((DualExpr) exp).getLow();
        wl.push(exp);
        rl.push(exp);
      }
    }

    while (!wl.empty()) {
      Expr exp = wl.pop();
      exp.visit(this);
    }

    // rl & wl are empty upon exit.

    visitNote(c);
  }

  public void visitBinaryExpr(BinaryExpr e)
  {
    Chord s = getChord();

    if (!s.isExprChord())
      return;
    
    Expr[] ops = e.getOperandArray();
    for (int i = 0; i < 2; i++) {
      Expr oprnd = ops[i];
      if (oprnd.isMemRefExpr()) {
        if (oprnd instanceof FieldExpr)
          return;
      
        if (oprnd instanceof LoadValueIndirectExpr)
          oprnd = oprnd.getOperand(0);

        if (!((oprnd instanceof LoadDeclValueExpr) ||
              (oprnd instanceof LoadDeclAddressExpr)))
          return;

        // Disable expressions with globals

        Declaration decl = ((LoadExpr) oprnd).getDecl();
        if (decl == null)
          return;
        if (decl.isGlobal())
          return;
        if (decl.addressTaken())
          return;
      } else if (oprnd.isLiteralExpr())
        continue;
      else
        return;
    }
    
//      if (e.isCommutative() && (ops[0].canonical() > ops[1].canonical())) {
//        e.swapOperands();
//        ops = e.getOperandArray();
//      }

    // Two expressions are lexically identical when they have the same
    // operator and operands. We need consider arithmatic features here,
    // e.g., a+b and b+a are lexical identical if they have the same types.

    boolean saveuo = LoadExpr.setUseOriginal(true);

    Enumeration<Expr> en = exprOccurs.keys();
    while (en.hasMoreElements()) {
      Expr le = en.nextElement();

      if (e.equivalent(le)) {
        e = (BinaryExpr) le;
        break;
      }
    }

    LoadExpr.setUseOriginal(saveuo);
    exprOccurs.add(e, s);
  }

  /**
   * Return an enumeration of all expressions that have lexically
   * equivalent binary expressions.
   */
  public Enumeration<Expr> allExprs()
  {
    return exprOccurs.keys();
  }

  /**
   * Return a set of all expressions lexically equivalent to the
   * original expression.
   */
  public HashSet<Chord> allOccurs(Expr original)
  {
    return exprOccurs.getRowSet(original);
  }

  /**
   * Return the number of different expressions.
   */
  public int numExpressions()
  {
    return exprOccurs.numRows();
  }
}
