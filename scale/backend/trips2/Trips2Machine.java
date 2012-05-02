package scale.backend.trips2;

import java.util.StringTokenizer;
import java.io.FileReader;
import java.io.BufferedReader;

import scale.common.*;
import scale.clef.type.Type;
import scale.clef.type.AggregateType;

/**
 * This is the base class for all Trips specific information.
 * <p>
 * $Id: Trips2Machine.java,v 1.49 2007-11-01 16:52:29 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * When an instance of this class is instantiated, it initializes a
 * set of (key, value) items that describe the particular Trips
 * architectural parameters to be used.  The Trips architectural
 * parameters are found by looking for the following files
 * <ol>
 * <li><code>./tcconfig.h</code>
 * <li><code>/projects/trips/toolchain/config/tcconfig.h</code>
 * </ol>
 * This is a file in the form of a C <code>.h</code> file.
 * <p>
 * If one of these files is not found, defaults are used.
 * Only lines from the description file of the form
 * <pre>
 * #define name integer
 * </pre>
 * are processed.  All other lines are ignored.
 * <p>
 */

public final class Trips2Machine extends Machine
{
  private String[] configKeys = {
    "ISA_VERSION",
    "TCC_VERSION",
    "TGS_VERSION",
    "TAS_VERSION",
    "TLD_VERSION",
    "TEM_VERSION",
    "TSIM_VERSION",
    "NUM_GPR_REGISTERS",
    "ABSOLUTE_ADDRESSING",
    "GRID_WIDTH",
    "GRID_HEIGHT",
    "PFRAMES_PER_AFRAME",
    "NUMBER_AFRAMES",
    "DATA_WORD_SIZE",
    "INSTRUCTION_WORD_SIZE",
    "ADDRESS_SPACE_SIZE",
    "CODE_SPACE_SIZE",
    "INSN_SEQUENCE_NUMBERS",
    "COMPACT_FORM",
    "EXTENDED_FORM",
    "INSNS_PER_ENTER",
    "INSNS_PER_ENTERA",
    "INSNS_PER_ENTERB",
    "IMMEDIATE_FIELD_NUMBER_BITS",
    "LOAD_STORE_OFFSET_NUMBER_BITS",
    "CONSTANT_FIELD_NUMBER_BITS",
    "BRANCH_OFFSET_NUMBER_BITS",
    "LSID_NUMBER_BITS",
    "EXIT_NUMBER_BITS",
    "GRID_REDUCTION_VALUE",
  };

  private int[] configValues = {  
     1,   1,   1,   1,   1,
     1,   1, 128,   1,   8,
     8,   2,   4,  64,  32,
    64,  32,   0,   0,   0,
     4,   4,   2,   9,   9,
    16,  20,   5,   3, 100,
  };

  /**
   * Version of TIL to be generated.
   */
  public static int tilVersion = 3;
  /**
   * Maximum size of an immediate field
   */
  public static int maxImmediate = 255;
  /**
   * Minimum size of an immediate field
   */
  public static int minImmediate = -256;
  /**
   * Maximum size of a unsigned Constant
   */
  public static int maxUnsignedConst = 65535;
  /**
   * Maximum size of a signed Constant
   */
  public static int maxSignedConst = 32767;
  /**
   * Minimum size of a signed Constatn
   */
  public static int minSignedConst = -32768;
  /**
   * Maximum number of load/store queue entries
   */
  public static int maxLSQEntries = 32;
  /**
   * Maximum number of instructions in a block.  This gets set below,
   * we don't set it here so we can change it from the command line if
   * necessary.
   */
  public static int maxBlockSize = -1;
  /**
   * Maximum number of branches in a block.
   */
  public static int maxBranches = 8;
  /**
   * Number of instructions required to expand an enter, entera, enterb.
   */
  public static int[] enterSizes = {4, 2, 2};
  /**
   * If true, use a library routine to do floating point division.
   */
  public static boolean softwareFDIV = false;
  /**
   * The different types of enter instructions.
   */
  public static final int ENTER  = 0;
  public static final int ENTERA = 1;
  public static final int ENTERB = 2;
  /**
   * If writes should be nullified.
   */
  public static boolean nullifyWrites = true;
  /**
   * Default path to the tcconfig file.
   */
  public static String tcconfigPath = ".";

  private static final int capabilities = HAS_II_CONDITIONAL_MOVE |
    HAS_IF_CONDITIONAL_MOVE |
    HAS_FI_CONDITIONAL_MOVE |
    HAS_FF_CONDITIONAL_MOVE |
    HAS_INT_FROM_FP_CMP |
    HAS_SIMPLE_FLOOR |
    HAS_NON_VOLATILE_FP_REGS |
    HAS_PREDICATION |
    HAS_WHILE_LOOP_UNROLL_BENEFIT |
    HAS_EXPENSIVE_SUBROUTINE_CALLS;

  /**
   * Create a Machine instance for an trips.
   */
  public Trips2Machine()
  {
    super(capabilities + (softwareFDIV ? HAS_NO_FP_DIVIDE : 0));
    
    readConfig();
    if (Debug.debug(1))
      outputConfig();

    maxLSQEntries = 1<<getConfigValue("LSID_NUMBER_BITS");   
    maxBranches   = 1<<getConfigValue("EXIT_NUMBER_BITS");
    
    // Set the max block size.
    
    int gw  = getConfigValue("GRID_WIDTH");
    int gh  = getConfigValue("GRID_HEIGHT");
    int pf  = getConfigValue("PFRAMES_PER_AFRAME");
    int grv = getConfigValue("GRID_REDUCTION_VALUE");
    
    // Set maxBlockSize if it hasn't already been set from the command line
    // with -f.
    if (-1 == maxBlockSize)
      maxBlockSize = (pf * gw * gh * grv) / 100;
    
    // The number of instructions an enter will be expanded into.
    
    enterSizes[ENTER]  = getConfigValue("INSNS_PER_ENTER");
    enterSizes[ENTERA] = getConfigValue("INSNS_PER_ENTERA");
    enterSizes[ENTERB] = getConfigValue("INSNS_PER_ENTERB");
  }

  /**
   * Return the name of the generic target architecture.
   */
  public String getGenericArchitectureName()
  {
    return "trips";
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
    if (extension.length() > 0) {
      if ("ptil".equals(extension))
        return "scale.backend.trips2.TripsPGenerator";
    }
   
    return "scale.backend.trips2.Trips2Generator";
  }

  protected void setup()
  {
    super.setup();

    // Specify the types that are different.

    scale.clef.type.PointerType.setMinBitSize(64);

    intCalcType      = scale.clef.type.SignedIntegerType.create(64);
    floatCalcType    = scale.clef.type.FloatType.create(64);
    signedLongType   = scale.clef.type.SignedIntegerType.create(64);
    unsignedLongType = scale.clef.type.UnsignedIntegerType.create(64);
    sizetType        = scale.clef.type.UnsignedIntegerType.create(64);
    ptrdifftType     = scale.clef.type.SignedIntegerType.create(64);

    cacheLineSize = 64;
  }

  /**
   * Return the name of the specific target architecture.
   */
  public String getArchitectureName()
  {
    return "trips2";
  }

  /**
   * Return the file extension to use for an assembler source file.
   */
  public String getAsmFileExtension()
  {
    return ".til";
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
    if ((value >= minImmediate) && (value <= maxImmediate))
      return 0;

    if ((value >= -0xffffL) && (value <= 0xfffffL))
      return 1;

    if ((value >= -0xffffffffL) && (value <= 0xfffffffffL))
      return 2;

    return 3;
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
    return executionCostEstimate(Double.doubleToLongBits(value));
  }

  /**
   * Return the maximum bit field size (64) in bits.
   * Also, bit fields can not be split over this boundary.
   */
  public int maxBitFieldSize()
  {
    return 64;
  }

  /**
   * Add the flags necessary for the C preprocessor.
   */
  public void addCPPFlags(Vector<String> v)
  {
    v.addElement("__trips__=1");
  }

  /**
   * Return the most general purpose alignment in memory units.
   */
  public int generalAlignment()
  {
    return 8;
  }

  /**
   * Return the alignment of things stored on the stack.
   */
  public final int stackAlignment(Type type)
  {
    return 8;
  }

  /**
   * Return true if the machine is little-endian.
   */
  public boolean littleEndian()
  {
    return false;
  }

  /**
   * Read in the Trips machine description.
   */
  private void readConfig()
  {
    FileReader fos = null;
    if (Debug.debug(1))
      System.out.println("Trips2Machine: tcconfig Path = " + tcconfigPath);

    try {
      boolean found = false;
      StringTokenizer fileLoc = new StringTokenizer(tcconfigPath, ":");
      while (fileLoc.hasMoreTokens()) {
        String file = fileLoc.nextToken() + "/tcconfig.h";
        try {
          fos = new FileReader(file);
        }catch(java.lang.Exception ex0) {
          continue;
        }
        found = true;
        if (Debug.debug(1))
          System.out.println("Trips2Machine: Using " + file);
        break;
      }
      if (!found) {
        Msg.reportWarning(Msg.MSG_Configuration_file_s_not_found, "tcconfig.h");
        return;
      }
    } catch(java.lang.Exception ex1) {
      Msg.reportWarning(Msg.MSG_s, "Could not parse tcconfigPath, using default values");
      return;
    }

    try {
      BufferedReader in = new BufferedReader(fos);
      while (in.ready()) {
        String line = in.readLine();
        if (line == null)
          break;
        try {
          StringTokenizer tok = new StringTokenizer(line, "\n\f\t\r ");
          int l = tok.countTokens();
          if (l != 3)
            continue;
          String def   = tok.nextToken();
          String key   = tok.nextToken();
          String value = tok.nextToken();
          if (!"#define".equals(def))
            continue;
          int    v     = Integer.parseInt(value);
          setConfigValue(key, v);
        } catch (java.lang.Exception ex2) {
        }
      }
    } catch (java.lang.Exception ex3) {
    }

    try {
      fos.close();
    } catch (java.lang.Exception ex4) {
    }
  }

  /**
   * Output the Trips machine description.
   */
  private void outputConfig() 
  {
    System.out.println("Trips2Machine configuration:");
    System.out.print("\tGRID_WIDTH=");
    System.out.println(getConfigValue("GRID_WIDTH"));
    System.out.print("\tGRID_HEIGHT=");
    System.out.println(getConfigValue("GRID_HEIGHT"));
    System.out.print("\tPFRAMES_PER_AFRAME=");
    System.out.println(getConfigValue("PFRAMES_PER_AFRAME"));
    System.out.print("\tNUM_GPR_REGISTERS=");
    System.out.println(getConfigValue("NUM_GPR_REGISTERS"));
    System.out.print("\tGRID_REDUCTION_VALUE=");
    System.out.println(getConfigValue("GRID_REDUCTION_VALUE"));
    System.out.print("\tINSNS_PER_ENTER=");
    System.out.println(getConfigValue("INSNS_PER_ENTER"));
    System.out.print("\tINSNS_PER_ENTERA=");
    System.out.println(getConfigValue("INSNS_PER_ENTERA"));
    System.out.print("\tINSNS_PER_ENTERB=");
    System.out.println(getConfigValue("INSNS_PER_ENTERB"));
    System.out.print("\tLOAD_STORE_OFFSET_NUMBER_BITS=");
    System.out.println(getConfigValue("LOAD_STORE_OFFSET_NUMBER_BITS"));
    System.out.print("\tCONSTANT_FIELD_NUMBER_BITS=");
    System.out.println(getConfigValue("CONSTANT_FIELD_NUMBER_BITS"));
    System.out.print("\tBRANCH_OFFSET_NUMBER_BITS=");
    System.out.println(getConfigValue("BRANCH_OFFSET_NUMBER_BITS"));
    System.out.print("\tTIL_VERSION=");
    System.out.println(tilVersion);
  }

  /**
   * Return the index of the key or -1.
   */
  private int getKeyIndex(String key)
  {
    for (int i = 0; i < configKeys.length; i++) {
      if (configKeys[i].equals(key))
        return i;
    }
    return -1;
  }

  /**
   * Return the intger value for the specified key.
   * @param key is the String that specifies the parameter and case sensitive
   */
  public int getConfigValue(String key)
  {
    int index = getKeyIndex(key);
    if (index < 0)
      return -1;
    return configValues[index];
  }

  /**
   * Specify the value for a key.
   */
  private void setConfigValue(String key, int value)
  {
    int index = getKeyIndex(key);
    if (index >= 0) {
      configValues[index] = value;
      return;
    }

    index = configKeys.length;
    String[] ns = new String[index + 1];
    System.arraycopy(configKeys, 0, ns, 0, index);
    configKeys = ns;

    int[] nv = new int[index + 1];
    System.arraycopy(configValues, 0, nv, 0, index);
    configKeys = ns;

    configKeys[index] = key;
    configValues[index] = value;
  }

  /**
   * Return true if the value is a valid value for an immediate field.
   */
  public static boolean isImmediate(long value)
  {
    if ((value >= minImmediate) && (value <= maxImmediate))
      return true;
    return false;
  }

  /**
   * Return true if a value of the type should be allocated to a
   * register.  If it is volatile or not an atomic type, it probably
   * should not be kept in a register.
   * @param type is the type
   * @param temporary is true if the duration will be short
   */
  public boolean keepTypeInRegister(Type type, boolean temporary)
  {
    if (type.isVolatile())
      return false;

    Type ctype = type.getCoreType();
    if (ctype.isAtomicType())
      return true;

    AggregateType agt = ctype.returnAggregateType();
    if (agt == null) {
      if (ctype instanceof scale.clef.type.FortranCharType) {
        scale.clef.type.FortranCharType fct = (scale.clef.type.FortranCharType) type;
        return fct.getLength() <= 8;
      }
      return false;
    }

    long bs = agt.memorySize(this);
    if (bs > 16)
      return false;

    int ft = agt.allFieldsType();

    return (ft != AggregateType.FT_MIX);
  }

  /**
   * Return the proper instruction count estimator for the target architecture.
   */
  public scale.backend.ICEstimator getInstructionCountEstimator()
  {
    return new TripsLoopICEstimator();
  }

  /**
   * For architectures that block instructions, this is the maximum
   * number of instructions that can be in a block.  For other
   * architectures, the value 0 is returned.
   */
  public int getMaxBlockSize()
  {
    return maxBlockSize;
  }
}
