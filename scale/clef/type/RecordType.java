package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;
import scale.clef.decl.*;

/**
 * A class representing a record or structure type.
 * <p>
 * $Id: RecordType.java,v 1.47 2007-08-27 18:13:32 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Note that the declarations in a record must be fields (FieldDecl
 * nodes) and not routines.
 */

public class RecordType extends AggregateType 
{
  private static Vector<RecordType> types; // A list of all the unique record types.

  private RecordType registerType; // Type to use if this struct is in a register.

  /**
   * Re-use an existing instance of a particular record type.
   * If no equivalent record type exists, create a new one.
   * @param fields the field declarations
   */
  public static RecordType create(Vector<FieldDecl> fields)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        RecordType ta = types.elementAt(i);
        if (compareUnique(ta.getAgFields(), fields)) {
          return ta;
        }
      }
    }
    RecordType a = new RecordType(fields);
    if (types == null)
      types = new Vector<RecordType>(2);
    types.addElement(a);
    return a;
  }

  public final RecordType returnRecordType()
  {
    return this;
  }

  protected RecordType(Vector<FieldDecl> fields)
  {
    super(fields);
  }
  
  /**
   * Return the type to use if a variable of this type is in a
   * register.
   */
  public Type registerType()
  {
    if (registerType != null)
      return registerType;

    int l = numFields();
    if (l > 2)
      return this;

    Type    rt  = Machine.currentMachine.getRealCalcType();
    boolean flg = false;
    for (int i = 0; i < l; i++) {
      Type ft = getField(i).getCoreType();
      if (!ft.isRealType() || ft.isComplexType())
        return this;
      if (rt != ft)
        flg = true;
    }

    if (!flg)
      return this;

    Vector<FieldDecl> nf = new Vector<FieldDecl>(l);
    for (int i = 0; i < l; i++)
      nf.add(new FieldDecl(getField(i).getName(), rt));

    registerType =  create(nf);
    return registerType;
  }

  /**
   * Calculate how many addressable memory units are needed to
   * represent the type.
   * @param machine is the machine-specific data machine
   * @return the number of bytes required to represent this type
   */
  public long memorySize(Machine machine)
  {
    Vector<FieldDecl> fields      = getAgFields();

    long   fieldOffset = 0;
    int    bitOffset   = 0;
    int    l           = fields.size();
    int    as          = 1;
    int    mbfs        = machine.maxBitFieldSize();
    int    binc        = ((mbfs + 7) / 8);
    int    mask        = binc - 1;

    for (int i = 0; i < l; i++) {
      FieldDecl fd   = fields.elementAt(i);
      int       bits = fd.getBits();
      Type      ft   = fd.getType();

      long ts = ft.memorySize(machine);
      int  fa = fd.isPackedField(machine) ? 1 : ft.alignment(machine);

      if (bits == 0) {
        if (bitOffset > 0) {
          fieldOffset += (bitOffset + 7) / 8;
          bitOffset = 0;
        }

        if (fa > as)
          as = fa;

        long x = fd.getFieldOffset();
        if ((x == 0) && (i > 0)) {
          fieldOffset = Machine.alignTo(fieldOffset, fa);
          fd.setFieldOffset(fieldOffset);
        } else
          fieldOffset = x;

        bitOffset = 0;
        fieldOffset += ts;

      } else {
        if (fa > as)
          as = fa;

        if ((bitOffset + bits) > 32) {
          if (bits <= 32) {
            fieldOffset += 4;
            bitOffset = 0;
          } else if ((bits + bitOffset > 64)) {
            fieldOffset += 8;
            bitOffset = 0;
          }
        }

        long x = fd.getFieldOffset();
        int  y = fd.getBitOffset();
        if ((x == 0) && (y == 0)) {
          if (bits <= 32) {
            fd.setBitOffset(bitOffset & 0x7);
            fd.setFieldOffset((fieldOffset & 0xfffffffffffffffcL) + (bitOffset >> 3));
          } else {
            fd.setBitOffset(bitOffset & 0xf);
            fd.setFieldOffset((fieldOffset & 0xfffffffffffffff0L) + (bitOffset >> 4));
          }
        } else {
          bitOffset = y;
          fieldOffset = x;
        }

        bitOffset += bits;
      }
    }

    if (bitOffset > 0)
      fieldOffset += machine.addressableMemoryUnits(bitOffset);

    long ms = Machine.alignTo(fieldOffset, as);

    return ms;
  }

  public void visit(Predicate p)
  {
    p.visitRecordType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitRecordType(this);
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

    RecordType t2 = (RecordType) tc;
    return compareFields(t2, true, true, false);
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
