package scale.score.expr;

import scale.score.Predicate;
import scale.common.*;
import scale.clef.type.*;
import scale.score.Note;

/**
 * This class represents a ExprPhi operation in static single
 * assignment form.
 * <p>
 * $Id: ExprPhiExpr.java,v 1.37 2006-02-28 16:37:11 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * It is used by PRE.
 * @see scale.score.trans.PRE
 */
public class ExprPhiExpr extends PhiExpr 
{
  /**
   * The version number.
   */
  private int     version;
  /**
   * canBeAvail
   */
  private boolean canBeAvail  = false;
  /**
   * later
   */
  private boolean later       = false;
  /**
   * downSafe
   */
  private boolean downSafe    = false;
  /**
   * willBeAvail
   */
  private boolean willBeAvail = false;

  /**
   * All in-coming values should be of the same type.  The result
   * type is the same as that of the incoming data.
   * @param type of the result and each in-coming value
   * @param operands a vector in-coming values
   */
  public ExprPhiExpr(Type type, Vector<Expr> operands)
  {
    this(-1, type, operands);
  }

  public ExprPhiExpr(int ver)
  {
    this(ver, VoidType.type, new Vector<Expr>(0));
  }

  public ExprPhiExpr(int ver, Type type, Vector<Expr> operands)
  {
    super(type, operands);
    version = ver;
  }

  public void setVersion(int version)
  {
    this.version = version;
  }

  public int getVersion()
  {
    return version;
  }

  public boolean getCanBeAvail()
  {
    return canBeAvail;
  }

  public void setCanBeAvail(boolean canBeAvail)
  {
    this.canBeAvail = canBeAvail;
  }

  public boolean getLater()
  {
    return later;
  }

  public void setLater(boolean later)
  {
    this.later = later;
  }

  public boolean getDownSafe()
  {
    return downSafe;
  }

  public void setDownSafe(boolean downSafe)
  {
    this.downSafe = downSafe;
  }

  public boolean getWillBeAvail()
  {
    return willBeAvail;
  }

  public void setWillBeAvail(boolean willBeAvail)
  {
    this.willBeAvail = willBeAvail;
  }

  public Expr copy()
  {
    int          n    = numOperands();
    Vector<Expr> args = new Vector<Expr>(n);
    for (int i = 0; i < n; i++)
      args.addElement(getOperand(i).copy());

    return new ExprPhiExpr(getType(), args);
  }

  public void visit(Predicate p)
  {
    p.visitExprPhiExpr(this);
  }

  public String getDisplayLabel(Object extra)
  {
    return "ExprPhi";
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(" ");
    buf.append(getDisplayLabel());
    buf.append(" v:");
    buf.append(String.valueOf(version));
    buf.append(canBeAvail ? " a:t" : " a:f");
    buf.append(later ? " l:t" : " l:f");
    buf.append(downSafe ? " t" : " f");
    buf.append(willBeAvail ? " w:t" : " w:f");
    int n = numOperands();
    for (int i = 0; i < n; i++) {
      buf.append(' ');
      buf.append(getOperand(i));
    }
    return buf.toString();
  }
}
