package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;
import scale.clef.expr.*;

/**
 * This class represents a C style enumeration type.
 * <p>
 * $Id: EnumerationType.java,v 1.52 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class EnumerationType extends NumericType 
{
  private static Vector<EnumerationType> types; // A list of all the unique enumeration types.

  private Vector<EnumElementDecl> enumVals; // A vector of the enumerated values.

  /**
   * Re-use an existing instance of a particular enumeration type.
   * If no equivalent enumeration type exists, create a new one.
   * @param  enums is a vector of EnumElementDecl instances
   */

  public static EnumerationType create(Vector<EnumElementDecl> enums)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        EnumerationType ta = types.elementAt(i);
        if (ta.compareEnums(enums))
          return ta;
      }
    }

    EnumerationType a = new EnumerationType(enums);
    return a;
  }

  private EnumerationType(Vector<EnumElementDecl> enums)
  {
    enumVals = enums;
    if (types == null)
      types = new Vector<EnumerationType>(2);
    types.addElement(this);
  }

  /**
   * Return true if type represents an enumeration value.
   */
  public boolean isEnumerationType()
  {
    return true;
  }

  public final EnumerationType returnEnumerationType()
  {
    return this;
  }

  /**
   * Return true if the bases are the same.
   */
  public boolean compareEnums(EnumerationType et)
  {
    return compareEnums(et.enumVals);
  }

  /**
   * Return true if the elements of the list are "equal" to this
   * type's enumeration.
   */
  public boolean compareEnums(Vector<EnumElementDecl> enumVals)
  {
    int n = enumVals.size();

    if (n != this.enumVals.size())
      return false;

    for (int i = 0; i < n; i++) {
      if (!this.enumVals.elementAt(i).equals(enumVals.elementAt(i)))
        return false;
    }
    return true;
  }

  /**
   * Return the number of elements in this enum type.
   */
  public int getNumEnums()
  {
    if (enumVals == null)
      return 0;
    return enumVals.size();
  }

  /**
   * Return the specified enum element.
   */
  public EnumElementDecl getEnum(int i)
  {
    return enumVals.elementAt(i);
  }

  public void visit(Predicate p)
  {
    p.visitEnumerationType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitEnumerationType(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    return (Node) enumVals.elementAt(i);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    if (enumVals == null)
      return 0;
    return enumVals.size();
  }

  /**
   * Return true if the enums are the same even though the order of
   * elements may be different.
   * @param t2 is the other enum
   * @param name is true if the enum names should be compared
   * @param value is true if the enum values should be compared
   */
  public boolean compareUnorderedEnumerators(EnumerationType t2,
                                             boolean         name,
                                             boolean         value)
  {
    // The two enumerations do not have to be of the same length.

    int l1 = enumVals.size();
    int l2 = t2.enumVals.size();

    for (int i1 = 0; i1 < l1; i1++) {
      EnumElementDecl eed1      = enumVals.elementAt(i1);
      boolean         haveMatch = false;
      String          id1       = eed1.getName();
      Expression      ex1       = eed1.getValue();

      for (int i2 = 0; i2 < l2; i2++) {
        EnumElementDecl eed2 = t2.enumVals.elementAt(i2);
        if (name) {
          String id2 = eed2.getName();
          if (id1.equals(id2)) {
            haveMatch = true;
          } else {
            haveMatch = false;
            continue;
          }
        }
        if (value) {
          Expression ex2 = eed2.getValue();
          try {
            long v1 = Lattice.convertToLongValue(ex1.getConstantValue());
            long v2 = Lattice.convertToLongValue(ex2.getConstantValue());
            haveMatch = v1 == v2;
          } catch(scale.common.InvalidException e) {
            // We require a constant expr, so this means graph is invalid.
            e.handler();
            throw new scale.common.InternalError("type brand must be a constant expression");
          }
        }
      }
      if (!haveMatch)
         return false;
    }
    return true;
  }

  /**
   * Return true if the enums are the same and the order of elements
   * is the same.
   * @param t2 is the other enum
   * @param name is true if the enum names should be compared
   * @param value is true if the enum values should be compared
   */
  public boolean compareOrderedEnumerators(EnumerationType t2,
                                           boolean         name,
                                           boolean         value)
  {
    int size1 = getNumEnums();
    int size2 = t2.getNumEnums();
  
    if (size1 != size2)
      return false; // must be same length

    for (int i = 0; i < size1; i++) {
      EnumElementDecl eed1 = enumVals.elementAt(i);
      EnumElementDecl eed2 = t2.enumVals.elementAt(i);
      if (name) {
        if (!eed1.getName().equals(eed2.getName())) {
          return false;
        }
      }
      if (value) {
        Expression ex1 = eed1.getValue();
        Expression ex2 = eed2.getValue();
        try {
          long v1 = Lattice.convertToLongValue(ex1.getConstantValue());
          long v2 = Lattice.convertToLongValue(ex2.getConstantValue());
          if (v1 != v2)
            return false;
        } catch(scale.common.InvalidException e) {
          // We require a constant expr, so this means graph is invalid.
          e.handler();
          throw new scale.common.InternalError("type brand must be a constant expression");
        }
      }
    }
    return true;
  }

  /**
   * Return true if the types are equivalent.
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    if (tc == null)
      return false;

    if (tc.getClass() != getClass())
      return false;

    EnumerationType t2 = (EnumerationType) tc;
    return compareOrderedEnumerators(t2, true, true);
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<EnumerationType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<EnumerationType>();
    return types.elements();
  }

  public int bitSize()
  {
    return 32;
  }

  /**
   * Calculate how many addressable memory units are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of bytes required to represent this type
   */
  public long memorySize(Machine machine)
  {
    return machine.addressableMemoryUnits(32);
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
