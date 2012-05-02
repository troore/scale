package scale.backend.sparc;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.decl.Declaration;
import scale.clef.decl.FieldDecl;
import scale.clef.decl.VariableDecl;
import scale.clef.decl.EquivalenceDecl;
import scale.backend.Generator;

/**
 * This is the base class for all Sparc specific information.
 * <p>
 * $Id: SparcMachine.java,v 1.29 2007-11-01 16:52:28 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class SparcMachine extends Machine
{
  /**
   * Specifies the Sparc V8 architecture and the 32-bit ABI.
   */
  public static final int V8 = 0;
  /**
   * Specifies the Sparc V9 architecture and the 32-bit ABI.
   */
  public static final int V8PLUS = 1;
  /**
   * Specifies the Sparc V9 architecture with the Visual Instruction Set and the 32-bit ABI.
   */
  public static final int V8PLUSA = 2;
  /**
   * Specifies the Sparc V9 architecture with the Visual Instruction Set (Version 2) and the 32-bit ABI.
   */
  public static final int V8PLUSB = 3;
  /**
   * Specifies the Sparc V9 architecture and the 64-bit ABI.
   */
  public static final int V9 = 4;
  /**
   * Specifies the Sparc V9 architecture with the Visual Instruction Set and the 64-bit ABI.
   */
  public static final int V9A = 5;
  /**
   * Specifies the Sparc V9 architecture with the Visual Instruction Set (Version 2) and the 64-bit ABI.
   */
  public static final int V9B = 8;

  private int    isa;
  private String isas = null;

  /**
   * Create a Machine instance for an sparc.
   */
  public SparcMachine()
  {
    super(0);

    scale.clef.type.PointerType.setMinBitSize(32);
  }

  protected void setup()
  {
    super.setup();

    // Specify the types that are different.

    wchartType = scale.clef.type.SignedIntegerType.create(32);
  }

  /**
   * Return the name of the generic target architecture.
   */
  public String getGenericArchitectureName()
  {
    return "sparc";
  }

  /**
   * Return the name of the specific target architecture.
   */
  public String getArchitectureName()
  {
    return "sparcV8";
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
    isas = "v8";
    isa = V8;

    if (extension.length() > 0) {
      isas = extension.toLowerCase();

      if ("v8".equals(extension))
        isa = V8;
      else if ("v8plus".equals(extension)) 
        isa = V8PLUS;
      else if ("v8plusa".equals(extension)) 
        isa = V8PLUSA;
      else if ("v8plusb".equals(extension))
        isa = V8PLUSB;
      else if ("v9".equals(extension))
        isa = V9;
      else if ("v9a".equals(extension)) 
        isa = V9A;
      else if ("v9b".equals(extension))
        isa = V9B;
      else
       throw new java.lang.Exception("Unknown architecture extension - " + extension);
    }

    return "scale.backend.sparc.SparcGenerator";
  }

  /**
   * Return the specific instruction set architecture.
   */
  public final int getISA()
  {
    return isa;
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

    return 8;
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
    if ((value >= -4096) && (value <= 4095))
      return 0;

    int val  = (int) value;
    if (val == value)
      return 2;

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
    return 5;
  }

  /**
   * Add the flags necessary for the C preprocessor.
   */
  public void addCPPFlags(Vector<String> v)
  {
    v.addElement("__sparc=1");
  }

  /**
   * Return the most general purpose alignment in memory units.
   */
  public final int generalAlignment()
  {
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
    return false;
  }

  /**
   * Return the assembler command appropriate to the architecture.
   * @param backendFeatures specifies code generation flags such as -g
   */
  public String getAssemblerCommand(int backendFeatures)
  {
    StringBuffer buf = new StringBuffer("as -xarch=");
    buf.append(isas);
    return buf.toString();
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
