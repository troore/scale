package scale.score.chords;

import scale.common.*;
import scale.score.expr.*;
import scale.score.Predicate;

/** 
 * This class represents exit statements.
 * <p>
 * $Id: ExitChord.java,v 1.20 2006-01-25 19:47:58 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Note, Exit statements may have other statements between them and the EndChord if the
 * user writes the original program that way.
 */

public class ExitChord extends ExprChord
{
  /**
   * @param next the next node in the CFG
   */
  public ExitChord(Chord next)
  {
    this(next, null);
  }

  /**
   * @param next the next node in the CFG
   * @param exitValue is the value passed to the exit
   */
  public ExitChord(Chord next, Expr exitValue)
  {
    super(exitValue, next);
  }

  public Chord copy()
  {
    Expr expc = getRValue();
    if (expc != null)
      expc = expc.copy();

    Chord ec = new ExitChord(getNextChord(), expc);
    ec.copySourceLine(this);
    return ec;
  }

  public void visit(Predicate p)
  {
    p.visitExitChord(this);
  }
}

