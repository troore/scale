package scale.clef.type;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;

/**
 * An aggregate type contains a list of fields (either field
 * declarations or routine declarations).
 * <p>
 * $Id: AggregateType.java,v 1.72 2007-10-04 19:58:08 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public abstract class AggregateType extends CompositeType
{
  /**
   * The field declarations including methods.
   */
  private Vector<FieldDecl> fields;  
  /**
   * True if the fields should be compared in order.
   */
  private boolean ordered; 

  public AggregateType(Vector<FieldDecl> fields, boolean ordered)
  {
    setFields(fields);
    this.ordered = ordered;
  }

  public AggregateType(Vector<FieldDecl> fields)
  {
    this(fields, true);
  }

  /**
   * Return true if type represents an aggregate object.
   */
  public final boolean isAggregateType()
  {
    return true;
  }

  public final AggregateType returnAggregateType()
  {
    return this;
  }

  /**
   * Return the field with the specified name or <code>null</code>.
   */
  public final FieldDecl findField(String name)
  {
    int l = fields.size();
    for (int i = 0; i < l; i++) {
      FieldDecl fd = fields.get(i);;
      if (name.equals(fd.getName()))
        return fd;
    }
    return null;
  }

  /**
   * Return the number of fields in the aggragate type.
   */
  public final int numFields()
  {
    return fields.size();
  }

  /**
   * Return the specified field.
   */
  public final FieldDecl getField(int i)
  {
    return fields.elementAt(i);
  }

  /**
   * Return the field whose offset is specified.
   */
  public final FieldDecl getFieldFromOffset(long offset)
  {
    int l = fields.size();
    for (int i = 0; i < l; i++) {
      FieldDecl fd = fields.elementAt(i);
      if (fd.getFieldOffset() == offset)
        return fd;
    }
    return null;
  }

  /**
   * Return the index of the field in the aggregation or -1 if not found.
   */
  public int getFieldIndex(FieldDecl fd)
  {
    int l = fields.size();
    for (int i = 0; i < l; i++)
      if (fd == fields.elementAt(i))
        return i;
    return -1;
  }

  /**
   * Return the vector containing the fields.
   */
  public Vector<FieldDecl> getAgFields()
  {
    return fields;
  }

  /**
   * No fields in the structure.
   * @see #allFieldsType
   */
  public static final int FT_START = 0;
  /**
   * All fields are integer or address type.
   * @see #allFieldsType
   */
  public static final int FT_INT   = 1;
  /**
   * All fields are 32-bit floating point.
   * @see #allFieldsType
   */
  public static final int FT_F32   = 2;
  /**
   * All fields are 64-bit floating point.
   * @see #allFieldsType
   */
  public static final int FT_F64   = 3;
  /**
   * All fields are floating point.
   * @see #allFieldsType
   */
  public static final int FT_FP    = 4;
  public static final int FT_MIX   = 5;
  /**
   * There is a mixture of field types.
   * @see #allFieldsType
   */

  private static final byte[][] ftTrans = {
    {0, 1, 2, 3, 4, 5}, // FT_START
    {1, 1, 5, 5, 5, 5}, // FT_INT
    {2, 5, 2, 4, 4, 5}, // FT_F32
    {3, 5, 4, 3, 4, 5}, // FT_F64
    {4, 5, 4, 4, 4, 5}, // FT_FP
    {5, 5, 5, 5, 5, 5}, // FT_MIX
  };

  /**
   * Return the "type" of all the fields in a structure including
   * any sub-structures.  The returned "type" is one of
   * <ul>
   * <li>FT_START - no fields in the structure
   * <li>FT_INT - all fields are integer or address type
   * <li>FT_F32 - all fields are 32-bit floating point
   * <li>FT_F64 - all fields are 64-bit floating point
   * <li>FT_FP  - all fields are floating point
   * <li>FT_MIX - there is a mixture of field types.
   * </ul>
   */
  public final int allFieldsType()
  {
    int state = FT_START;
    int l     = fields.size();
    for (int i = 0; i < l; i++) {
      FieldDecl fd = fields.elementAt(i);
      Type      ft = fd.getCoreType();
      int       ns = FT_MIX;

      if (ft.isAggregateType())
        ns = ((AggregateType) ft).allFieldsType();
      else if (ft.isRealType()) {
        FloatType flt = (FloatType) ft;
        int       bs  = flt.bitSize();

        if (bs == 32)
          ns = FT_F32;
        else if (bs == 64)
          ns = FT_F64;
        else
          ns = FT_FP;
      } else if (ft.isCompositeType())
        return FT_MIX;
      else
        ns = FT_INT;

      state = ftTrans[state][ns];
    }
    return state;
  }

  /**
   * Return true if the fields are ordered.
   */
  public final boolean isOrdered()
  {
    return ordered;
  }

  /**
   * Calculate the alignment needed for this data type.
   */
  public int alignment(Machine machine)
  {
    int as = 1;
    int l  = fields.size();

    for (int i = 0; i < l; i++) {
      FieldDecl fd = fields.elementAt(i);
      Type      ft = fd.getCoreType();
      int       fa = ft.alignment(machine);

      if (fa > as)
        as = fa;
    }

    return as;
  }

  public void visit(Predicate p)
  {
    p.visitAggregateType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitAggregateType(this);
  }

  /**
   * Go back through and fix up the field declarations so that each
   * declaration has a pointer to this class instance.  This is a
   * chicken and egg problem because we want to add the class to the
   * method declarations before the class is created.  But, if we
   * create the class first, then the class doesn't have any
   * declarations to begin with and we have to add them later (along
   * with the base clases).
   * @param fs is the list of fields
   */
  protected final void setFields(Vector<FieldDecl> fs)
  {
    fields = fs;
    int l = fields.size();
    for (int i = 0; i < l; i++)
      fields.elementAt(i).setMyStruct(this);
  }

  /**
   * Return the specified AST child of this node.
   */
  public Node getChild(int i)
  {
    return (Node) fields.elementAt(i);
  }

  /**
   * Return the number of AST children of this node.
   */
  public int numChildren()
  {
    return fields.size();
  }

  /**
   * Return true if the two sets of fields are identical (==).
   */
  protected static boolean compareUnique(Vector<FieldDecl> fields1, Vector<FieldDecl> fields2)
  {
    int l = fields1.size();

    if (l != fields2.size())
      return false;

    for (int i = 0; i < l; i++) {
      if (fields1.elementAt(i) != fields2.elementAt(i))
        return false;
    }
    return true; // The fields have matched!
  }

  /**
   * Check the set of fields for equivalence.
   * @param fields is the list of fields to compare to this types fields
   * @param useFieldName is true to compare field names
   * @param useFieldType is true to compare field types
   * @param useMethod is true to compare method fields
   */
  public boolean compareFields(Vector<FieldDecl>  fields,
                               boolean useFieldName,
                               boolean useFieldType,
                               boolean useMethod)
  {
    return compareFields(this.fields,
                         fields,
                         ordered,
                         useFieldName,
                         useFieldType,
                         useMethod);
  }

  /**
   * Check the set of fields for equivalence.
   */
  public boolean compareFields(AggregateType t2,
                               boolean       useFieldName,
                               boolean       useFieldType,
                               boolean       useMethod)
  {
    return compareFields(this.fields,
                         t2.fields,
                         ordered,
                         useFieldName,
                         useFieldType,
                         useMethod);
  }

  /**
   * Check the set of fields for equivalence.
   */
  public boolean compareFields(AggregateType t2,
                               boolean       inOrder,
                               boolean       useFieldName,
                               boolean       useFieldType,
                               boolean       useMethod)
  {
    return compareFields(this.fields,
                         t2.fields,
                         inOrder,
                         useFieldName,
                         useFieldType,
                         useMethod);
  }

  /**
   * Check ordered set of fields for equivalence
   */
  private static boolean compareFields(Vector<FieldDecl>  fields1,
                                       Vector<FieldDecl>  fields2,
                                       boolean inOrder,
                                       boolean useFieldName,
                                       boolean useFieldType,
                                       boolean useMethod)
  {
    if (inOrder)
      return compareOrderedFields(fields1,
                                  fields2,
                                  useFieldName,
                                  useFieldType,
                                  useMethod);

    return compareUnorderedFields(fields1,
                                  fields2,
                                  useFieldName,
                                  useFieldType,
                                  useMethod);
  }

  private static boolean compareOrderedFields(Vector<FieldDecl> fields1,
                                              Vector<FieldDecl> fields2,
                                              boolean useFieldName,
                                              boolean useFieldType,
                                              boolean useMethod)
  {
    int l1 = fields1.size();
    int l2 = fields2.size();
    if (l1 != l2)
      return false;

    for (int i1 = 0; i1 < l1; i1++) {
      FieldDecl d1 = fields1.elementAt(i1);
      FieldDecl d2 = fields2.elementAt(i1);

      if (useFieldName) {
        String id1 = d1.getName();
        String id2 = d2.getName();
        if (!id1.equals(id2))
          return false;
      }
      
      if (useFieldType) {
        if (d1.getType() != d2.getType())
          return false;
      }
    }
    return true; // The fields have matched!
  }

  /**
   * Check unordered fields for equivalence
   */
  private static boolean compareUnorderedFields(Vector<FieldDecl> fields1,
                                                Vector<FieldDecl> fields2,
                                                boolean useRecordFieldName,
                                                boolean useRecordFieldType,
                                                boolean useMethods)
  {
    int l1 = fields1.size();
    int l2 = fields2.size();
    if (l1 != l2)
      return false;

    if (!useRecordFieldName) {
      throw new scale.common.NotImplementedError("Would this make sense?");
      // I don't know how to determine correspondences between
      // fields if they are both unordered and unnamed.  If this
      // turns out to be a valid case, then we would have to ensure
      // that no field is matched twice.
    }

    for (int i1 = 0; i1 < l1; i1++) {
      FieldDecl f1  = fields1.elementAt(i1);
      String    id1 = f1.getName();

      boolean haveMatch = false; // We have not yet found a match.
      // While we have more fields to test, and we haven't already
      // found the field.
      for (int i2 = 0; i2 < l2; i2++) {
        FieldDecl f2   = fields2.elementAt(i2);
        String    id2 = f2.getName();
        haveMatch = true;         // Assume it will match.
  
        if (useRecordFieldName) { // Compare the field names.
          if (id1.equals(id2)) {  // Do the field names match?
            haveMatch = true;     // Still have to check its value.
          } else {
            haveMatch = false;
            continue;             // Skip the comparison of field types.
          }
        }

        // "haveMatch" must be true at this point.
        if (useRecordFieldType) // Compare the field types.
            haveMatch = f1.getType().equivalent(f2.getType());
      }
      if (!haveMatch)
        return false;  // No corresponding field.
    }
    // All fields have found a match.
    return true;
  }
}
