package scale.score.expr;

import scale.common.*;
import scale.clef.decl.FieldDecl;
import scale.clef.type.Type;
import scale.clef.type.PointerType;
import scale.score.Predicate;

import scale.score.Note;

/**
 * This is the base class for field reference operations.
 * <p>
 * $Id: FieldExpr.java,v 1.50 2007-01-04 17:26:10 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class FieldExpr extends UnaryExpr
{
  /**
   * Field specifies the field of the expression being loaded.
   */
  private FieldDecl field;

  /**
   * @param type is the result type of the field reference
   * @param structure is the address of the structure accessed
   * @param field specifies the field being referenced
   */
  public FieldExpr(Type type, Expr structure, FieldDecl field)
  {
    super(type, structure);
    this.field     = field;
  }

  /**
   * Return true if the expressions are equivalent.  This method
   * should be called by the equivalent() method of the derived
   * classes.
   */
  public boolean equivalent(Expr exp)
  {
    if (!super.equivalent(exp))
      return false;
    FieldExpr o = (FieldExpr) exp;
    return (field == o.field);
  }

  /**
   * Return the expression specifying the structure.
   */
  public final Expr getStructure() 
  {
    return getArg();
  }

  /**
   * Return the expression specifying the field.
   */
  public FieldDecl getField() 
  {
    return field;
  }

  /**
   * Return true if the expression loads a value from memory.
   */
  public boolean isMemRefExpr()
  {
    return true;
  }

  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer("->");
    if (field != null)
      buf.append(field.getName());
    return buf.toString();
  }

  /**
   * The given expression is defined if the FieldExpr expression is
   * defined and the given expression is the structure.
   * @param expr the expression representing the structure name
   * @return true if the structure name is defined.
   */
  public boolean isDefined(Expr expr)
  {
    return this.isDefined() && (getStructure() == expr);
  }

  /**
   * Return the structure associated with the field expression. We
   * use the structure name to represent the access of the structure
   * - instead of the field name.
   */
  public Expr getReference()
  {
    return getStructure().getReference();
  }

  /**
   * Return a unique value representing this particular expression.
   */
  public long canonical()
  {
    long canon = (getClass().getName().hashCode() << 1) ^ getType().hashCode();
    canon = (canon << 1) ^ getStructure().canonical();
    return (canon << 1) ^ field.hashCode();
  }

  /**
   * Return true if this expression is loop invariant.
   * @param loop is the loop
   */
  public boolean isLoopInvariant(scale.score.chords.LoopHeaderChord loop)
  {
    return false;
  }
}
