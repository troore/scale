package scale.clef.expr;

import java.util.AbstractCollection;
import java.math.*;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.type.*;

/**
 * A class which represents the address of a Declaration.
 * <p>
 * $Id: AddressLiteral.java,v 1.28 2007-08-28 17:58:20 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class AddressLiteral extends Literal 
{
  /**
   * The object associated with the AddressLiteral.  It can be either
   * a Declaration or a Literal.
   */
  private Object value;
  /**
   * The offset from the begiining of the declaration.
   */
  private long offset;

  /**
   * @param value is the {@link scale.clef.expr.Literal literal} whose
   * address is represented
   * @param offset is the offset from the start of the {@link
   * scale.clef.expr.Literal literal}
   */
  public AddressLiteral(Type type, Literal value, long offset)
  {
    super(type);
    this.value = value;
    this.offset = offset;
  }

  /**
   * @param decl is the {@link scale.clef.decl.Declaration
   * declaration} whose address is represented
   * @param offset is the offset from the start of the {@link
   * scale.clef.decl.Declaration declaration}
   */
  public AddressLiteral(Type type, Declaration decl, long offset)
  {
    super(type);
    this.value = decl;
    this.offset = offset;
    decl.setAddressTaken();
  }

  /**
   * @param decl is the {@link scale.clef.decl.Declaration
   * declaration} whose address is represented
   */
  public AddressLiteral(Type type, Declaration decl)
  {
    this(type, decl, 0);
  }

  private AddressLiteral(Type type, Object value, long offset)
  {
    super(type);
    this.value = value;
    this.offset = offset;
  }

  /**
   * Return a copy of the address literal with a different type.
   */
  public AddressLiteral copy(Type type)
  {
    return new AddressLiteral(type, value, offset);
  }

  /**
   * Return true if the two expressions are equivalent.
   */
  public boolean equivalent(Object exp)
  {
    if (!super.equivalent(exp))
      return false;
    AddressLiteral al = (AddressLiteral) exp;
    return (value == al.value) && (offset == al.offset);
  }

  /**
   * Return short description of current node.
   */
  public String getDisplayLabel()
  {
    StringBuffer buf = new StringBuffer("&");
    if (value instanceof Declaration) {
      Declaration decl = (Declaration) value;
      buf.append(decl.getName());
    } else {
      buf.append('&');
      buf.append(value);
    }
    buf.append('+');
    buf.append(offset);
    return buf.toString();
  }

  public String toStringSpecial()
  {
    StringBuffer buf = new StringBuffer(super.toStringSpecial());
    buf.append(' ');
    buf.append(value);
    buf.append('+');
    buf.append(offset);
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitAddressLiteral(this);
  }

  /**
   * Return the offset from the {@link scale.clef.decl.Declaration
   * declaration}.
   */
  public final long getOffset()
  {
    return offset;
  }

  /**
   * Return the {@link scale.clef.expr.Literal literal} whose address
   * is represented or null.
   */
  public final Literal getValue()
  {
    if (value instanceof Literal)
      return (Literal) value;

    return null;
  }

  /**
   * Return the {@link scale.clef.decl.Declaration declaration} whose
   * address is represented or null.
   */
  public final Declaration getDecl()
  {
    if (value instanceof Declaration)
      return (Declaration) value;

    return null;
  }

  /**
   * Get the string version of the address literal using C syntax.
   * For another language, use {@link #getValue getValue()} which
   * returns an object that can then be displayed using logic specific
   * to the target language.
   * @return a String already converted for display using C syntax.
   */
  public String getGenericValue()
  {
    if (value instanceof Declaration) {
      Declaration decl = (Declaration) value;
      StringBuffer buf = new StringBuffer("((void *) (((char *)&");
      buf.append(decl.getName());
      buf.append(")+");
      buf.append(offset);
      buf.append("))");
      return buf.toString();
    }

    StringBuffer buf = new StringBuffer("((void *) (((char *)&(");
    buf.append(value);
    buf.append("))+");
    buf.append(offset);
    buf.append("))");
    return buf.toString();
  }

  public void getDeclList(AbstractCollection<Declaration> varList)
  {
    if (value instanceof Declaration)
      varList.add((Declaration) value);
  }
}
