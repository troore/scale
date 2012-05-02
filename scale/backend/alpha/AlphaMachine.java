package scale.backend.alpha;

import scale.common.*;
import scale.clef.type.*;
import scale.clef.decl.FieldDecl;
import scale.backend.Generator;

/**
 * This is the base class for all Alpha specific information.
 * <p>
 * $Id: AlphaMachine.java,v 1.40 2007-11-01 16:52:28 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class AlphaMachine extends Machine
{
  private static final int capabilities = HAS_II_CONDITIONAL_MOVE |
                                          HAS_FF_CONDITIONAL_MOVE |
                                          HAS_SIMPLE_FLOOR |
                                          HAS_NON_VOLATILE_FP_REGS;

  private boolean bwx; // True if the target hardware supports BWX extensions.
  private boolean fix; // True if the target hardware supports FIX extensions.
  private boolean cix; // True if the target hardware supports CIX extensions.
  private boolean mvi; // True if the target hardware supports MVI extensions.

  /**
   * Create a Machine instance for an alpha.
   */
  public AlphaMachine()
  {
    super(capabilities);
  }

  protected void setup()
  {
    super.setup();

    // Specify the types that are different.

    PointerType.setMinBitSize(64);

    intCalcType      = SignedIntegerType.create(64);
    floatCalcType    = FloatType.create(64);
    unsignedLongType = UnsignedIntegerType.create(64);
    signedLongType   = SignedIntegerType.create(64);
    sizetType        = UnsignedIntegerType.create(64);
    ptrdifftType     = SignedIntegerType.create(64);

    Vector<FieldDecl> fields = new Vector<FieldDecl>(2);

    fields.addElement(new FieldDecl("ptr", voidStarType, 0));
    fields.addElement(new FieldDecl("offset", signedLongType, 8));
    vaListType = RecordType.create(fields);

    cacheLineSize = 16;
  }

  /**
   * Return the name of the generic target architecture.
   */
  public String getGenericArchitectureName()
  {
    return "alpha";
  }

  /**
   * Return the name of the specific target architecture.
   */
  public String getArchitectureName()
  {
    return "alphaEV5";
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

    // Determine what additional Alpha features are available.

    int len   = extension.length();
    int index = 0;
    while (index < len) {
      int sl = extension.indexOf('/', index);
      if (sl < 0)
        sl = len;
      String opt = extension.substring(index, sl);
      if ("bwx".equals(opt))
        bwx = true;
      else if ("fix".equals(opt))
        fix = true;
      else if ("cix".equals(opt))
        cix = true;
      else if ("mvi".equals(opt))
        mvi = true;
      index = sl + 1;
    }

    return "scale.backend.alpha.AlphaGenerator";
  }

  /**
   * Return true if processor has the BWX extensions.
   */
  public final boolean hasBWX()
  {
    return bwx;
  }

  /**
   * Return true if processor has the FIX extensions.
   */
  public final boolean hasFIX()
  {
    return fix;
  }

  /**
   * Return true if processor has the CIX extensions.
   */
  public final boolean hasCIX()
  {
    return cix;
  }

  /**
   * Return true if processor has the MVI extensions.
   */
  public final boolean hasMVI()
  {
    return mvi;
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
   * Return true if it is faster to generate the integer value than to
   * load it.
   */
  public boolean simpleInteger(long value)
  {
    int val = (int) value;
    return (val == value);
  }

  private static final byte[] intCosts = {1, 1, 1, 2, 1, 2, 2, 3};

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
    if ((value > -256) && (value < 256))
      return 0;

    long    sign  = value >> 32;
    int     val   = (int) value;
    boolean upper = ((value & 0xffffffffL) == 0);
    if (!upper && (((sign == 0) && (val >= 0)) || ((sign == -1) && (val < 0)))) {
      int low   = (val & 0xffff);
      int tmp1  = val - ((low << 16) >> 16);
      int high  = (tmp1 >> 16) & 0xffff;
      int tmp2  = tmp1 - (((high << 16) >> 16) << 16);
      int extra = 0;

      if (tmp2 != 0) {
        extra = 0x4000;
        tmp1 -= 0x40000000;
        high = (tmp1 >> 16) & 0xffff;
      }

      long tot = (((((long) high) << 48) >> 32) +
                  ((((long) low) << 48) >> 48) +
                  ((((long) extra) << 48) >> 32));
      int which = 0;
      if (low != 0)
        which |= 1;
      if (extra != 0)
        which |= 2;
      if (high != 0)
        which |= 4;

      int cost = intCosts[which];
      if (tot != value)
        cost++;

       return cost;
    }

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

    if (value == 2.0)
      return 1;

    return 5;
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
    v.addElement("__alpha=1");
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
    return 8;
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
    boolean debug = ((backendFeatures & Generator.DEBUG) != 0);
    boolean is    = ((backendFeatures & Generator.NIS) == 0);
    boolean ln    = ((backendFeatures & Generator.LINENUM) != 0);

    StringBuffer buf = new StringBuffer("as ");
    if (debug)
      buf.append("-g ");
    else if (ln)
      buf.append("-g3 ");

    buf.append("-O");
    buf.append((!debug && is) ? '1' : '0');
    buf.append(' ');

    return buf.toString();
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
      if (ctype instanceof FortranCharType) {
        FortranCharType fct = (FortranCharType) type;
        return fct.getLength() <= 8;
      }
      return false;
    }

    long bs = ctype.memorySize(this);
    if (bs > 16)
      return false;

    int ft = agt.allFieldsType();

    return (ft == AggregateType.FT_INT) || (ft == AggregateType.FT_F64);
  }

  /**
   * Return the functional unit information.  This information is an
   * array where each element of the array is the number of
   * instructions that can be issued to the corresponding functional
   * unit.
   * @see scale.backend.Instruction#getFunctionalUnit
   */
  public byte[] getFunctionalUnitDescriptions()
  {
    return functionalUnits;
  }

  public static final int FU_NONE   = 0; // Markers, etc.
  public static final int FU_LDST   = 1; // Load/store
  public static final int FU_INTALU = 2; // Integer ALU
  public static final int FU_FPALU  = 3; // Floating point ALU
  public static final int FU_BRANCH = 4; // Branch

  private static final byte[] functionalUnits = {1, 4, 4, 2, 1};

}
