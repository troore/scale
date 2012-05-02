package scale.backend.x86;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.clef.decl.FieldDecl;
import scale.clef.decl.VariableDecl;
import scale.clef.decl.EquivalenceDecl;
import scale.backend.Generator;

/**
 * This is the base class for all X86 specific information.
 * <p>
 * $Id: X86Machine.java,v 1.1 2007-11-01 16:52:30 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class X86Machine extends Machine
{
  /**
   * Create a Machine instance for an x86.
   */
  public X86Machine()
  {
    super(0);

    scale.clef.type.PointerType.setMinBitSize(32);
  }

  /**
   * Return the name of the generic target architecture.
   */
  public String getGenericArchitectureName()
  {
    return "x86";
  }

  /**
   * Return the name of the specific target architecture.
   */
  public String getArchitectureName()
  {
    return "x86";
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
    return "scale.backend.x86.X86Generator";
  }

  /**
   * Return the integer value <tt>alignment</tt>that satisfies 
   * <code>(0 == address % alignment)</code>
   * for the data size specified.
   * @param dataSize is the size of the data in addressable units.
   */
  public int alignData(int dataSize)
  {
    if (dataSize <= 1)
      return 1;

    if (dataSize <= 2)
      return 2;

    if (dataSize <= 4)
      return 4;

    return 4;
  }

  /**
   * Return the number of addressable units required
   * @param bitSize is the number of bits required for the data
   */
  public int addressableMemoryUnits(int bitSize)
  {
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
    return 1;
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
    return 1;
  }

  /**
   * Add the flags necessary for the C preprocessor.
   */
  public void addCPPFlags(Vector<String> v)
  {
    v.addElement("__i386__=1");
  }

  /**
   * Return the most general purpose alignment in memory units.
   */
  public final int generalAlignment()
  {
    return 4;
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
    return true;
  }

  /**
   * Return the assembler command appropriate to the architecture.
   * @param backendFeatures specifies code generation flags such as -g
   */
  public String getAssemblerCommand(int backendFeatures)
  {
    return "as";
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
    return super.keepTypeInRegister(type, temporary) && (temporary || !type.getCoreType().isRealType());
  }

  /**
   * Set the type to be used for the C <code>long double</code> type.
   */
  protected void setLongDoubleType()
  {
    longDoubleType = scale.clef.type.FloatType.create(64);
  }
}
