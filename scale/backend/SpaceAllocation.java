package scale.backend;

import scale.common.*;

/** 
 * This class represents statically allocated memory.
 * <p>
 * $Id: SpaceAllocation.java,v 1.27 2007-10-04 19:57:49 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Typically memory is statically allocated in different areas or
 * sections.  Different systems use different rules for what can be
 * allocated where.  One section might be for addresses and another
 * for floating point values.  There is typically one area for
 * un-initialized memory.  An instance of this class represents an
 * individually statically allocated chunk of memory.  This chunk is
 * allocated in an area and has a size.  It may also have a set of
 * values to be used to statically initialize it.
 */

public class SpaceAllocation
{
  /**
   * The data area is not initialized.
   */
  public static final int DAT_NONE = 0;
  /**
   * The data area initializer contains 8-bit integers.
   */
  public static final int DAT_BYTE = 1;
  /**
   * The data area initializer 16-bit integer.
   */
  public static final int DAT_SHORT = 2;
  /**
   * The data area initializer contains 32-bit integer values.
   */
  public static final int DAT_INT = 3;
  /**
   * The data area initializer contains 64-bit integer values.
   */
  public static final int DAT_LONG = 4;
  /**
   * The data area initializer contains 32-bit floating point values.
   */
  public static final int DAT_FLT = 5;
  /**
   * The data area initializer contains 64-bit floating point values.
   */
  public static final int DAT_DBL = 6;
  /**
   * The data area initializer contains address data.
   */
  public static final int DAT_ADDRESS = 7;
  /**
   * The data area initializer contains instructions.
   */
  public static final int DAT_TEXT = 8;
  /**
   * The data area initializer contains 64-bit floating point values.
   */
  public static final int DAT_LDBL = 9;

  /**
   * Map from data type to displayable name.
   */
  public static final String[] types = {"none", "byte",  "short", "int",
					"long", "float", "double", "address",
					"text", "ldouble"};

  /**
   * The memory has local visibility.
   */
  public static final byte DAV_LOCAL = 0;
  /**
   * The memory has global visibility.
   */
  public static final byte DAV_GLOBAL = 1;
  /**
   * The memory is in another module.
   */
  public static final byte DAV_EXTERN = 2;
  /**
   * map from visibility to displayable name.
   */
  public static final String[] visibilities = {"local", "global", "extern"};

  private String  name;       // Name of the data area.
  private Object  value;      // The object used to generate the initialization for the memory area.
  private Displacement disp;  // Null or displacement to area.
  private long    size;       // The size of the memory or data item in addressable units.
  private int     alignment;  // The memory boundary to align the data on.
  private int     reps;       // Number of times item is repeated.
  private byte    area;       // Section
  private byte    type;       // What the memory holds.
  private byte    flags;      // Local, global, read-only, etc.

  private static final int VIS_MASK  = 0x3;
  private static final int RDONLY    = 0x4;
  private static final int WEAK      = 0x8;

  /**
   * True if the area is weakly linked.
   * If weak is true and visibility == DAV_EXTERN, then
   * then value contains the aliaed name (String).
   */


  /**
   * Create an object to represent static allocation of memory.
   * @param name is the name of the allocated memory
   * @param area is the area (section) in which the memory is allocated
   * @param type specifies what the memory holds
   * @param readOnly is true if the area is read-only
   * @param size specifies the size of the memory in addressable units
   * @param value specifies the object used to generate the
   * initialization for the memory area
   */
  public SpaceAllocation(String  name,
                         int     area,
                         int     type,
                         boolean readOnly,
                         long    size,
                         Object  value)
  {
    this(name, area, type, readOnly, size, 1, value);
  }

  /**
   * Create an object to represent static allocation of memory.
   * @param name is the name of the allocated memory
   * @param area is the area (section) in which the memory is allocated
   * @param type specifies what the memory holds
   * @param readOnly is true if the area is read-only
   * @param size specifies the size of the memory or data item in
   * addressable units
   * @param reps specifies the number of times the value is repeated
   * @param value specifies the object used to generate the
   * initialization for the memory area
   */
  public SpaceAllocation(String  name,
                         int     area,
                         int     type,
                         boolean readOnly,
                         long    size,
                         int     reps,
                         Object  value)
  {
    this(name, area, type, readOnly, size, 1, 1, value);
  }

  /**
   * Create an object to represent static allocation of memory.
   * @param name is the name of the allocated memory
   * @param area is the area (section) in which the memory is allocated
   * @param type specifies what the memory holds
   * @param readOnly is true if the area is read-only
   * @param size specifies the size of the memory or data item in
   * addressable units
   * @param reps specifies the number of times the value is repeated
   * @param alignment specifies the memory boundary to align the data on
   * @param value specifies the object used to generate the
   * initialization for the memory area
   */
  public SpaceAllocation(String  name,
                         int     area,
                         int     type,
                         boolean readOnly,
                         long    size,
                         int     reps,
                         int     alignment,
                         Object  value)
  {
    this.area         = (byte) area;
    this.type         = (byte) type;
    this.name         = name;
    this.size         = size;
    this.reps         = reps;
    this.alignment    = alignment;
    this.value        = value;
    this.flags        = (byte) (DAV_LOCAL | (readOnly ? RDONLY : 0));
  }

  /**
   * Associate the displacement with this space allocation.
   */
  public void setDisplacement(Displacement disp)
  {
    this.disp = disp;
  }

  /**
   * Return the displacement associated with this space allocation or
   * <code>null</code>
   */
  public Displacement getDisplacement()
  {
    return disp;
  }

  /**
   * Return true if this space allocation has the specified attributes.
   * @param area specifies the section in which the loader should place the area
   * @param type specifies the type of data
   * @param size is the number of addressable units required
   * @param readOnly is true if the area is read-only
   */
  public boolean matches(int area, int type, long size, boolean readOnly)
  {
    return (area == this.area) &&
      (type == this.type) &&
      (size == this.size) &&
      (readOnly == ((flags & RDONLY) != 0));
  }

  /**
   * Return true if the memory is read-only.
   */
  public boolean readOnly()
  {
    return ((flags & RDONLY) != 0);
  }

  /**
   * Return true if the memory is initialized.
   */
  public boolean containsData()
  {
    return (value != null);
  }

  /**
   * Return true if the memory is initialized with address data.
   */
  public boolean containsAddress()
  {
    return (type == DAT_ADDRESS);
  }

  /**
   * Set the visibility of the memory area.
   */
  public void setVisibility(byte visibility)
  {
    assert ((visibility & VIS_MASK) == visibility) :
      "Invalid visibility: " + visibility;
    this.flags = (byte) (visibility & VIS_MASK);
  }

  /**
   * Return the visibility of the memory.
   */
  public int getVisibility()
  {
    return flags & VIS_MASK;
  }

  /**
   * Return true if the area is weakly linked.
   */
  public boolean isWeak()
  {
    return (flags & WEAK) != 0;
  }
  
  /**
   * Specify if the symbol is weakly linked..
   */
  public void setWeak(boolean weak)
  {
    if (weak)
      flags |= WEAK;
    else 
      flags &= ~WEAK;
  }

  /**
   * Return the initilizer.
   */
  public Object getValue()
  {
    return value;
  }

  /**
   * Set the initilizer
   */
  public void setValue(Object value)
  {
    this.value = value;
  }

  /**
   * Return the name of the memory area.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Return the index of the area in which this memory is allocated.
   */
  public int getArea()
  {
    return area;
  }

  /**
   * Return the size of the memory in bytes.
   */
  public long getSize()
  {
    return size;
  }

  /**
   * Return the number of times the value is repeated.
   */
  public int getReps()
  {
    return reps;
  }

  /**
   * Set the alignment boundary required.
   */
  protected void setAlignment(int alignment)
  {
    this.alignment = alignment;
  }

  /**
   * Return the alignment boundary required.
   */
  public int getAlignment()
  {
    return alignment;
  }

  /**
   * Return the type of data this memory contains.
   */
  public int getType()
  {
    return type;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(SA ");
    buf.append(name);
    buf.append(" area:");
    buf.append(area);
    buf.append(" type:");
    buf.append(types[type]);
    buf.append(' ');
    buf.append(visibilities[flags & VIS_MASK]);
    buf.append(" size:");
    buf.append(size);
    buf.append(" aln:");
    buf.append(alignment);
    buf.append(" reps:");
    buf.append(reps);
    if ((flags & RDONLY) != 0)
      buf.append(" readonly");
    if (value != null) {
      buf.append(" value:");
      buf.append(value);
    }
    buf.append(')');
   return buf.toString();
  }
}
