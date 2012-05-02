package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.FieldDecl;

/** 
 * A class representing a C union type.
 * <p>
 * $Id: UnionType.java,v 1.42 2007-08-27 18:13:33 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Note that the declarations in a union must be fields (FieldDecl
 * nodes) and not routines.
 */

public class UnionType extends RecordType 
{
  private static Vector<RecordType> types; // A list of all the unique union types.

  /**
   * Re-use an existing instance of a particular union type.
   * If no equivalent union type exists, create a new one.
   * @param fields the list of field declarations
   * @param dummy is a dummy parameter required because Java is unable
   * to disambiguate between static methods with the same name and
   * parameters in parent and child classes!
   */

  public static UnionType create(Vector<FieldDecl> fields, boolean dummy)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        UnionType ta = (UnionType) types.elementAt(i);
        if (compareUnique(ta.getAgFields(), fields)) {
          return ta;
        }
      }
    }
    UnionType a = new UnionType(fields);
    return a;
  }

  private UnionType(Vector<FieldDecl> fields)
  {
    super(fields);
    if (types == null)
      types = new Vector<RecordType>(2);
    types.addElement(this);
  }

  /**
   * Return true if type represents a union.
   */
  public final boolean isUnionType()
  {
    return true;
  }

  public final UnionType returnUnionType()
  {
    return this;
  }

  /**
   * Return the number of addressable memory units required to
   * represent this type.
   */
  public long memorySize(Machine machine)
  {
    long              size   = 0;
    Vector<FieldDecl> fields = getAgFields();
    int               l      = fields.size();

    for (int i = 0; i < l; i++) {
      FieldDecl fd = fields.elementAt(i);
      long      fs = fd.getCoreType().memorySize(machine);
      if (fs > size)
        size = fs;
    }

    return size;
  }

  public void visit(Predicate p)
  {
    p.visitUnionType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitUnionType(this);
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<RecordType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<RecordType>();
    return types.elements();
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
