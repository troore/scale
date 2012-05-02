package scale.score.trans;

import scale.common.*;
import scale.score.*;
import scale.score.expr.*;
import scale.score.chords.*;
import scale.score.pred.*;
import scale.clef.LiteralMap;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.expr.IntLiteral;
 
/**
 * This class removes useless copies from the CFG.
 * <p>
 * $Id: UselessCopy.java,v 1.17 2007-02-28 18:00:39 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <A href="http://ali-www.cs.umass.edu/">Scale Compiler Group</A>, <BR>
 * <A href="http://www.cs.umass.edu/">Department of Computer Science</A><BR>
 * <A href="http://www.umass.edu/">University of Massachusetts</A>, <BR>
 * Amherst MA. 01003, USA<BR>
 * All Rights Reserved.<BR>
 * <P>
 * A useless copy is simply the expression <code>var = var;</code>.
 * It is always valid to remove such occurrences.
 * <p>
 * Unlike most optimizations this optimization never increases
 * register pressure.  Consequently, there is never a case where a
 * {@link scale.score.trans#legal legal} transformation is not also
 * {@link scale.score.trans#legal beneficial}.
 */

public class UselessCopy extends Optimization
{
  /**
   * True if traces are to be performed.
   */
  public static boolean classTrace = false;
  /**
   * If true, use heuristics that prune the cases where the
   * optimization is applied.
   */
  public static boolean useHeuristics = true;

  private static int uselessCopyCFGNodeCount = 0; /* A count of A = A instances removed. */

  static
  {
    Statistics.register("scale.score.trans.UselessCopy", "uselessCopyCFGNodes");
  }

  /**
   * Return the number of useless copies eliminated.
   */
  public static int uselessCopyCFGNodes()
  {
    return uselessCopyCFGNodeCount;
  }

  public UselessCopy(Scribble scribble)
  {
    super(scribble, "_no");
  }

  /**
   * Remove useless copy statements.
   * Useless copy statements(CFG nodes) are of the form
   * <pre>
   *  X = X;
   * </pre>
   * These are created by going into and out of SSA form and by other
   * optimizations.
   */
  public void perform()
  {
    Stack<Chord> wl    = WorkArea.<Chord>getStack("perform UseessCopy");
    Chord        begin = scribble.getBegin();

    wl.push(begin);
    Chord.nextVisit();
    begin.setVisited();

    while (!wl.empty()) {
      Chord s = wl.pop();

      s.pushOutCfgEdges(wl);

      if (!s.isAssignChord())
        continue;

      ExprChord se = (ExprChord) s;
      Expr      le = se.getLValue();
      if (!(le instanceof LoadDeclAddressExpr))
        continue;

      Expr re = se.getRValue();
      if (!(re instanceof LoadDeclValueExpr))
        continue;

      Declaration ld = ((LoadDeclAddressExpr) le).getDecl();
      Declaration rd = ((LoadDeclValueExpr) re).getDecl();
      if (ld != rd)
        continue;

      s.removeFromCfg();
      uselessCopyCFGNodeCount++;
      dChanged = true;
    }

    WorkArea.<Chord>returnStack(wl);

    if (dChanged) {
      scribble.recomputeDominators(); // Chord info no longer valid
      scribble.recomputeRefs();       // Chord info no longer valid
    }
  }

  /**
   * Return whether this optimization requires that the CFG be in SSA form.
   * It returns either
   * <dl>
   * <dt><b>NO_SSA</b><dd>the CFG must not be in SSA form,
   * <dt>IN_SSA<dd>the CFG must be in SSA form,
   * <dt>VALID_SSA<dd>the CFG must be in valid SSA form, or
   * <dt>NA_SSA<dd>the CFG does not need to be in valid SSA form.
   * </dl>
   */
  public int requiresSSA()
  {
    return NO_SSA;
  }
}
