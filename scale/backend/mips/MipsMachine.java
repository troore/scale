package scale.backend.mips;

import scale.common.*;
import scale.clef.type.Type;

/**
 * This is the base class for all Mips specific information.
 * <p>
 * $Id: MipsMachine.java,v 1.20 2007-11-01 16:52:28 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class MipsMachine extends Machine
{
  /**
   * Create a Machine instance for an mips.
   */
  public MipsMachine()
  {
    super(0);

    scale.clef.type.PointerType.setMinBitSize(32);
  }

  /**
   * Return the name of the generic target architecture.
   */
  public String getGenericArchitectureName()
  {
    return "mips";
  }

   /**
   * Return the name of the specific target architecture.
   */
  public String getArchitectureName()
  {
    return "mipsM10000";
  }

  /**
   * Determine the architecture sub-type.
   * @param architecture specifies the target architecture
   * @param extension specifies an extension to the a target architecture
   * @return the name of the specific target architecture generator class.
   * @throws java.lang.Exception if the extension is not understood
   */
  public String determineArchitecture(String architecture,
                                      String extension) throws java.lang.Exception
  {
    return "scale.backend.mips.MipsGenerator";
  }

 /**
   * Return the integer value <tt>alignment</tt>that satisfies 
   * <code>(0 == address % alignment)</code>
   * for the data size specified.
   * @param dataSize is the size of the data in addressable units.
   */
  public int alignData(int dataSize)
  {
    // MIGHT NEED TO BE CHANGED FOR MIPS -JEFF
    if (dataSize <= 1)
      return 1;

    if (dataSize <= 2)
      return 2;

    if (dataSize <= 4)
      return 4;

    return 8;
  }

  /**
   * Return the number of addressable units required
   * @param bitSize is the number of bits required for the data
   */
  public int addressableMemoryUnits(int bitSize)
  {
    // MIGHT NEED TO BE CHANGED FOR MIPS -JEFF
    return ((bitSize + 7) / 8);
  }

  /**
   * Return an estimate of the execution cost to provide this value.
   * The cost should be zero if the value can be represented as a
   * hard-wired register or in the immediate field of an instruction.
   * The cost is relative to the cost of other operations; a good
   * metric is the number of cycles needed to generate the value or
   * load it from memory.
   */
  public int executionCostEstimate(long value)
  {
    int val = (int) value;
    if ((val == value) || (value == (value & 0xffffffff)))
      return 0;

    return 5;
  }

  /**
   * Return an estimate of the execution cost to provide this value.
   * The cost should be zero if the value can be represented as a
   * hard-wired register or in the immediate field of an instruction.
   * The cost is relative to the cost of other operations; a good
   * metric is the number of cycles needed to generate the value or
   * load it from memory.
   */
  public int executionCostEstimate(double value)
  {
    if (value == 0.0)
      return 0;

    return 5;
  }

  /**
   * Add the flags necessary for the C preprocessor.
   */
  public void addCPPFlags(Vector<String> v)
  {
    v.addElement("__mips=1");
    v.addElement("_LANGUAGE_C");
    v.addElement("_MIPS_ISA=4");
    v.addElement("_MIPS_SIM=2");
    v.addElement("_MIPS_FPSET=32");
    v.addElement("_MIPS_SZINT=32");
    v.addElement("_MIPS_SZLONG=32");
    v.addElement("_MIPS_SZPTR=32");
    v.addElement("_LONGLONG");
    v.addElement("_NO_ANSIMODE");
    v.addElement("_ABIN32=2");
    v.addElement("__EXTENSIONS__");
    v.addElement("_SGI_SOURCE");
  }

  /**
   * Return the most general purpose alignment in memory units.
   */
  public int generalAlignment()
  {
    // MIGHT NEED TO BE CHANGED FOR MIPS -JEFF
    return 8;
  }

  /**
   * Return the alignment of things stored on the stack.
   */
  public final int stackAlignment(Type type)
  {
    return 4;
  }

  /**
   * Return true if the machine is little-endian.
   */
  public boolean littleEndian()
  {
    // 50% CHANGE THAT THIS NEEDS TO BE CHANGED FOR MIPS -JEFF
    return false;
  }

  /**
   * Return true if a value of the type should be allocated to a register.  If it is volatile or not an
   * atomic type, it probably should not be kept in a register.
   * @param type is the type
   * @param temporary is true if the duration will be short
   */
  public boolean keepTypeInRegister(Type type, boolean temporary)
  {
    if (type.isAggregateType())
      return false;
    return super.keepTypeInRegister(type, temporary);
  }
}
