package scale.clef2scribble;

import scale.common.*;
import scale.score.chords.*;
import scale.score.expr.Expr;

/**
 * This class holds a SESE region of basic blocks.
 * <p>
 * $Id: ExprTuple.java,v 1.15 2007-10-04 19:58:10 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Because the region is SESE, we can represent the basic blocks with
 * a single begin and end block.  In scribble, statements serve as
 * basic blocks.  This class also holds a reference to the variable
 * holding the result of the expression.
 */
public final class ExprTuple 
{
  // If one of these two is null, then they both should be.
  private Chord begin; /* First statement in the range */
  private Chord end;   /* Last statement in the range */
  private Expr  ref;   /* Variable holding the value of the expression. */

  /**
   * Record a range of Chords. Begin and end may be null or neither
   * arguments may be null.
   * @param exp the result of the last expression or null
   * @param begin first statement in the range
   * @param end last statement in the range
   */
  public ExprTuple(Expr exp, Chord begin, Chord end)
  {
    assert ((end == null) && (begin == null)) || ((end != null) && (begin != null)) :
      "Either both ends of the sequence must be known or neither.";

    this.begin = begin;
    this.end = end;
    this.ref = exp;
  }

  /**
   * Create a range of Chords. Both or neither arguments may be null.
   * @param begin first statement in the range
   * @param end last statement in the range
   */
  public ExprTuple(Chord begin, Chord end)
  {
    this(null, begin, end);
  }

  /**
   * Return the first Chord in the range.
   */
  public Chord getBegin()
  {
    return begin;
  }

  /**
   * Return the last Chord in the range.
   */
  public Chord getEnd()
  {
    return end;
  }

  /**
   * Append the range to this range.
   * @param sr is the range to be appended
   */
  public void concat(ExprTuple sr)
  { 
    if (sr.getBegin() != null) {
      if (begin != null) {
        end.linkTo(sr.getBegin());
        end = sr.getEnd();
      } else {  // We are empty, so range becomes second range.
        begin = sr.getBegin();
        end   = sr.getEnd();
      }
    }
    // Else there is nothing to do because second range is empty.
  }

  /**
   * Append the Chord to this range.
   * @param s is the Chord to be appended
   */
  public void append(Chord s)
  { 
    assert (s != null) : "Statement must exist.";

    if (begin != null)
      end.linkTo(s);
    else // Range is empty, so this statement becomes entire range.
      begin = s;

    end   = s;
  }

  public String toString()
  {
    StringBuffer s = new StringBuffer("(ExprTuple ref: ");
    s.append(ref);
    s.append(", beg: ");
    s.append(begin);
    if (begin != end) {
      Chord x = begin.getOutCfgEdge(0);
      while ((x != null) && (x != end)) {
        s.append(", ");
        s.append(x);
        x = x.getOutCfgEdge(0);
      }
      s.append(", end: ");
      s.append(end);
    }
    s.append(")");
    return s.toString();
  }

  /**
   * Return a copy of the last expression in this range of CFG nodes.
   */
  public Expr getRef()
  {
    if (ref == null)
      return null;

    return ref.conditionalCopy();
  }

  /**
   * Specify the last expression in this range of CFG nodes.
   */
  public void setRef(Expr ref)
  {
    this.ref = ref;
  }
}
