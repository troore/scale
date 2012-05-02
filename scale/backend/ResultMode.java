package scale.backend;

/**
 * This enum specifies where and what the result is from compiling an
 * expression.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @see Generator
 */
public enum ResultMode
{
  /**
   * Indicates that the <code>resultReg</code> field contains
   * <b>no</b> value.  In this case
   * <code>resultRegAddressOffset</code> is meaningless.  In this case
   * <code>resultRegAddressAlignment</code> is meaningless.
   */
  NO_VALUE("has_no_value"),
  /**
   * Indicates that the <code>resultReg</code> field specifies the
   * register that contains the value.  In this case
   * <code>resultRegAddressOffset</code> is meaningless (i.e., 0).  In
   * this case <code>resultRegAddressAlignment</code> is meaningless.
   */
  NORMAL_VALUE("has_normal_value"),
  /**
   * Indicates that the <code>resultReg</code> field specifies the
   * register that contains the value which is a structure.  In this
   * case <code>resultRegAddressOffset</code> is the offset to the
   * specified field (for <code>LoadFieldAddressExpr</code>).  In
   * this case <code>resultRegAddressAlignment</code> is meaningless.
   */
  STRUCT_VALUE("has_struct_value"),
  /**
   * Indicates that the <code>resultReg</code> specifies the register
   * that contains the base address.  The address value is the sum of
   * the register value and the offset in
   * <code>resultRegAddressOffset</code>.  This mode is used only when
   * <code>resultRegAddressOffset</code> may be non-zero.  In this
   * case <code>resultRegAddressAlignment</code> is meaningless (for
   * now).
   */
  ADDRESS_VALUE("has_address_value"),
  /**
   * Indicates that the <code>resultReg</code> specifies the register
   * that when added to the offset in
   * <code>resultRegAddressOffset</code> is the address of the value.
   * In this case <code>resultRegAddressAlignment</code> specifies the
   * alignment of the address provided in <code>resultReg</code> only.
   */
  ADDRESS("has_address");

  private final String name;

  private ResultMode(String name)
  {
    this.name = name;
  }

  public String toString()
  {
    return name;
  }
}
